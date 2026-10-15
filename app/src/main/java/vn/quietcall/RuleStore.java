package vn.quietcall;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static vn.quietcall.ScreeningEngine.*;

public final class RuleStore {
    private final SharedPreferences prefs;
    public RuleStore(Context context) { this(context, "quietcall"); }
    RuleStore(Context context, String preferenceName) { prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE); }
    public boolean enabled() { return prefs.getBoolean("enabled", true); }
    public void enabled(boolean value) { prefs.edit().putBoolean("enabled", value).apply(); }
    public List<Brand> brands() {
        List<Brand> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("brands", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                Set<String> numbers = new LinkedHashSet<>();
                JSONArray list = row.getJSONArray("numbers");
                for (int j = 0; j < list.length(); j++) numbers.add(list.getString(j));
                result.add(new Brand(row.getString("name"), numbers, row.getBoolean("enabled"), Action.valueOf(row.getString("action"))));
            }
        } catch (JSONException | IllegalArgumentException e) {
            throw new IllegalStateException("Không đọc được dữ liệu quy tắc", e);
        }
        return result;
    }
    public void saveBrands(List<Brand> brands) {
        prefs.edit().putString("brands", serializeBrands(brands).toString()).apply();
    }
    private JSONArray serializeBrands(List<Brand> brands) {
        JSONArray array = new JSONArray();
        try {
            for (Brand b : brands) array.put(new JSONObject().put("name", b.name())
                .put("numbers", new JSONArray(b.numbers())).put("enabled", b.enabled()).put("action", b.action().name()));
        } catch (JSONException e) { throw new IllegalStateException(e); }
        return array;
    }
    public Set<String> allowed() { return new LinkedHashSet<>(prefs.getStringSet("allowed", Collections.emptySet())); }
    public void saveAllowed(Set<String> numbers) { prefs.edit().putStringSet("allowed", new LinkedHashSet<>(numbers)).apply(); }
    public List<SpamReport> reports() {
        List<SpamReport> result = new ArrayList<>();
        try {
            JSONArray rows = new JSONArray(prefs.getString("reports", "[]"));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                result.add(new SpamReport(row.getString("number"), row.getString("brand"), row.getString("reason"), row.getLong("time")));
            }
        } catch (JSONException | IllegalArgumentException e) { throw new IllegalStateException("Không đọc được blacklist cá nhân", e); }
        return result;
    }
    public Map<String, String> blacklist() { return SpamReport.blacklist(reports()); }
    private void saveReports(List<SpamReport> reports) {
        JSONArray rows = new JSONArray();
        try {
            for (SpamReport r : reports) rows.put(new JSONObject().put("number", r.number()).put("brand", r.brand())
                .put("reason", r.reason()).put("time", r.time()));
        } catch (JSONException e) { throw new IllegalStateException(e); }
        prefs.edit().putString("reports", rows.toString()).apply();
    }
    public void report(SpamReport report) {
        if (allowed().contains(report.number())) throw new IllegalArgumentException("Số đang ở Ngoại lệ. Hãy gỡ ngoại lệ trước khi báo cáo.");
        saveReports(SpamReport.upsert(reports(), report));
    }
    public void removeReport(String number) {
        List<SpamReport> current = reports(); current.removeIf(report -> report.number().equals(number)); saveReports(current);
    }

    public JSONArray bundledSources() {
        try { return new JSONArray(prefs.getString("bundledSources", "[]")); }
        catch (JSONException e) { throw new IllegalStateException("Không đọc được nguồn danh mục", e); }
    }
    public Map<String, List<String>> aliases() {
        Map<String, List<String>> result = new LinkedHashMap<>(); JSONArray rows = bundledSources();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i); if (row == null) continue;
            String key = normalizeName(row.optString("brand")); JSONArray values = row.optJSONArray("aliases");
            if (values == null || result.containsKey(key)) continue;
            List<String> list = new ArrayList<>(); for (int j = 0; j < values.length(); j++) list.add(values.optString(j)); result.put(key, list);
        }
        return result;
    }
    public JSONObject brandMetadata(String name) {
        JSONArray rows = bundledSources();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && normalizeName(row.optString("brand")).equals(normalizeName(name))) return row;
        }
        return new JSONObject();
    }
    public Brand resolveBrand(String name) { return BrandSearch.resolve(name, brands(), aliases()); }
    public Set<String> brandNumbers(Brand brand) {
        Set<String> numbers = new LinkedHashSet<>(brand.numbers());
        for (SpamReport report : reports()) if (normalizeName(report.brand()).equals(normalizeName(brand.name()))) numbers.add(report.number());
        return numbers;
    }
    /** Install each reviewed dataset version once; never reset user's toggles on launch. */
    public void installBundledDirectory(Context context) {
        try (InputStream stream = context.getAssets().open("brand-directory.json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONObject document = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
            String version = document.getString("version");
            if (version.equals(prefs.getString("bundledVersion", ""))) return;
            JSONArray rows = document.getJSONArray("entries");
            List<DirectoryCsv.Entry> entries = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String number = normalizeNumber(row.getString("number")), brand = row.getString("brand").trim();
                if (number.isEmpty() || brand.isEmpty() || brand.length() > 80) throw new IllegalArgumentException("Dữ liệu đóng gói không hợp lệ");
                entries.add(new DirectoryCsv.Entry(brand, number));
            }
            Map<String, Boolean> defaults = new HashMap<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i); defaults.put(normalizeName(row.getString("brand")), row.optBoolean("enabledByDefault", true));
            }
            List<Brand> current = brands(); Set<String> existing = new HashSet<>();
            for (Brand brand : current) existing.add(normalizeName(brand.name()));
            List<Brand> merged = DirectoryCsv.merge(current, entries);
            for (int i = 0; i < merged.size(); i++) {
                Brand brand = merged.get(i);
                if (!existing.contains(normalizeName(brand.name()))) merged.set(i, new Brand(brand.name(), brand.numbers(), defaults.getOrDefault(normalizeName(brand.name()), false), Action.BLOCK));
            }
            // Publish rules, source metadata and version in one preference edit.
            prefs.edit().putString("brands", serializeBrands(merged).toString())
                .putString("bundledSources", rows.toString()).putString("bundledVersion", version).apply();
        } catch (IOException | JSONException e) { throw new IllegalStateException("Không nạp được danh mục kèm ứng dụng", e); }
    }
    public JSONArray history() {
        try { return new JSONArray(prefs.getString("history", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    public void clearHistory() { prefs.edit().remove("history").apply(); }
    public void log(String number, Decision decision) {
        JSONArray old = history(), next = new JSONArray();
        try {
            next.put(new JSONObject().put("number", number).put("action", decision.action().name())
                .put("reason", decision.reason()).put("time", System.currentTimeMillis()));
            for (int i = 0; i < Math.min(old.length(), 199); i++) next.put(old.get(i));
        } catch (JSONException e) { return; }
        prefs.edit().putString("history", next.toString()).apply();
    }
}
