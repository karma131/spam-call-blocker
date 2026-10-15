package vn.quietcall;

import android.app.*;
import android.app.role.RoleManager;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.*;
import org.json.*;
import static vn.quietcall.ScreeningEngine.*;

public final class MainActivity extends Activity {
    private static final int BLUE = 0xff3884f5, INK = 0xff192331, GRAY = 0xff7a8493,
        LINE = 0xffeef0f3, SOFT = 0xfff3f7fe;
    private static final int[] PASTELS = {0xffd8f7e7, 0xffe2e7ff, 0xffffe8d8, 0xfff8def0, 0xffd8f1f5, 0xffeef4d5};
    private RuleStore store;
    private LinearLayout page, subnav;
    private Dialog activeSheet;
    private int tab = 1, settingsPage;
    private boolean onlyBlocked;
    private String query = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store = new RuleStore(this);
        try { store.installBundledDirectory(this); }
        catch (RuntimeException e) { toast("Không nạp được danh mục. Các quy tắc của bạn vẫn được giữ."); }
        if (state != null) { tab = state.getInt("tab", 1); query = state.getString("query", ""); }
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("tab", tab); state.putString("query", query); super.onSaveInstanceState(state);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return view;
    }
    private void gap(LinearLayout parent, int height) { parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private void divider(LinearLayout parent) { View line = new View(this); line.setBackgroundColor(LINE); parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1))); }
    private void caption(String value) { TextView view = text(value, 12, GRAY, true); view.setPadding(0, dp(20), 0, dp(10)); page.addView(view); }
    private void title(String heading) {
        TextView name = text(heading, 26, INK, true); name.setPadding(0, dp(18), 0, dp(5)); page.addView(name);
        gap(page, 8);
    }
    private void button(LinearLayout parent, String label, boolean primary, Runnable action) {
        TextView view = text(label, 14, primary ? Color.WHITE : BLUE, true); view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(14), dp(16), dp(14)); view.setBackground(bg(primary ? BLUE : SOFT, 14)); view.setMinHeight(dp(48));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(8), 0, dp(4)); parent.addView(view, lp);
        view.setOnClickListener(v -> {
            if (activeSheet != null && activeSheet.isShowing() && activeSheet.getWindow() != null
                    && view.getRootView() == activeSheet.getWindow().getDecorView()) activeSheet.dismiss();
            action.run();
        }); view.setContentDescription(label);
    }
    private GlyphView icon(String name, int color, int size) {
        GlyphView view = new GlyphView(this, name, color); view.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size))); return view;
    }
    private TextView avatar(String name, int index, int size) {
        TextView view = text(name.trim().isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT), size / 2 - 2, 0xff677ca1, true);
        view.setGravity(Gravity.CENTER); view.setBackground(bg(PASTELS[Math.floorMod(index, PASTELS.length)], size));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size))); return view;
    }
    private LinearLayout body() { LinearLayout box = column(); box.setPadding(dp(22), dp(12), dp(22), dp(24)); return box; }
    private EditText input(LinearLayout parent, String hint, String value, boolean phone) {
        EditText edit = new EditText(this); edit.setTextSize(16); edit.setHint(hint); edit.setText(value); edit.setTextColor(INK);
        edit.setInputType(phone ? android.text.InputType.TYPE_CLASS_PHONE : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setPadding(dp(12), dp(14), dp(12), dp(14)); edit.setBackground(bg(0xfff4f6f9, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(8), 0, dp(8)); parent.addView(edit, lp); return edit;
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private String actionLabel(Action action) { return switch (action) { case BLOCK -> "Đã chặn"; case SILENCE -> "Tắt chuông"; case ALLOW -> "Cho phép"; }; }
    private boolean hasRole() {
        RoleManager role = getSystemService(RoleManager.class);
        return role != null && role.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) && role.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }
    private void activate() {
        RoleManager role = getSystemService(RoleManager.class);
        if (role == null || !role.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) { toast("Thiết bị không hỗ trợ lọc cuộc gọi."); return; }
        startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 10);
    }
    private void navigate(int destination) { tab = destination; query = ""; onlyBlocked = false; settingsPage = 0; render(); }

    private void render() {
        LinearLayout root = column(); root.setBackgroundColor(Color.WHITE);
        if (android.os.Build.VERSION.SDK_INT >= 30) root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()); v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        }); else root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        LinearLayout top = row(); top.setPadding(dp(18), dp(12), dp(18), dp(8));
        LinearLayout bar = row(); bar.setPadding(dp(7), dp(5), dp(3), dp(5));
        GradientDrawable border = bg(Color.WHITE, 32); border.setStroke(dp(1), 0xffe5e8ec); bar.setBackground(border);
        TextView mark = text("Q", 20, BLUE, true); mark.setGravity(Gravity.CENTER); mark.setBackground(bg(0xffe8f1ff, 30));
        bar.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38))); mark.setContentDescription("QuietCall");
        EditText search = new EditText(this); search.setTag("catalog-search"); search.setTextSize(14); search.setSingleLine(true);
        search.setHint("Tìm thương hiệu hoặc số"); search.setTextColor(INK); search.setHintTextColor(GRAY);
        search.setPadding(dp(10), 0, 0, 0); search.setBackgroundColor(Color.TRANSPARENT); search.setText(query); search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        bar.addView(search, new LinearLayout.LayoutParams(0, dp(44), 1));
        GlyphView menu = icon("more", INK, 44); menu.setContentDescription("Tùy chọn"); bar.addView(menu);
        menu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, menu);
            popup.getMenu().add("Chặn theo tên thương hiệu").setOnMenuItemClickListener(item -> { blockByNameDialog(query); return true; });
            popup.getMenu().add("Nhập dữ liệu CSV").setOnMenuItemClickListener(item -> { pickCsv(); return true; });
            popup.getMenu().add("Nguồn dữ liệu").setOnMenuItemClickListener(item -> { sources(); return true; }); popup.show();
        });
        top.addView(bar, new LinearLayout.LayoutParams(-1, -2)); root.addView(top); subnav = row(); root.addView(subnav); divider(root);
        FrameLayout frame = new FrameLayout(this); ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        page = column(); page.setPadding(dp(20), 0, dp(20), dp(90)); scroll.addView(page); frame.addView(scroll);
        GlyphView add = new GlyphView(this, "plus", Color.WHITE); add.setBackground(bg(BLUE, 40)); add.setElevation(dp(5));
        add.setVisibility(tab == 1 || tab == 2 ? View.VISIBLE : View.GONE);
        add.setContentDescription("Chặn theo tên hoặc báo cáo số mới"); add.setTag("add-rule");
        FrameLayout.LayoutParams fab = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.END); fab.setMargins(0, 0, dp(22), dp(18)); frame.addView(add, fab);
        add.setOnClickListener(v -> { if (tab == 3) reportDialog("", ""); else blockByNameDialog(query); });
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1)); divider(root);
        LinearLayout navigation = row(); String[] labels = {"Nhật ký", "Thương hiệu", "Chặn", "Báo cáo", "Cài đặt"};
        String[] glyphs = {"clock", "brands", "shield", "report", "settings"};
        for (int i = 0; i < labels.length; i++) {
            final int destination = i; LinearLayout item = column(); item.setGravity(Gravity.CENTER); item.setPadding(0, dp(8), 0, dp(10));
            GlyphView navIcon = icon(glyphs[i], tab == i ? BLUE : GRAY, 34);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
            iconParams.gravity = Gravity.CENTER_HORIZONTAL; item.addView(navIcon, iconParams);
            TextView label = text(labels[i], 10, tab == i ? BLUE : GRAY, tab == i);
            label.setSingleLine(true); label.setGravity(Gravity.CENTER); label.setIncludeFontPadding(false);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2)); item.setContentDescription(labels[i]); item.setTag("nav-" + i);
            navigation.addView(item, new LinearLayout.LayoutParams(0, dp(68), 1)); item.setOnClickListener(v -> navigate(destination));
        }
        root.addView(navigation); root.setFocusableInTouchMode(true); setContentView(root); root.requestFocus(); root.requestApplyInsets();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); renderPage(); }
            @Override public void afterTextChanged(Editable value) {}
        }); renderPage();
    }
    private void renderPage() {
        if (page == null) return; page.removeAllViews(); subnav.removeAllViews();
        String left = switch (tab) { case 0 -> "Gần đây"; case 1 -> "Danh mục"; case 2 -> "Thương hiệu"; case 3 -> "Báo cáo của bạn"; default -> "QuietCall"; };
        String right = switch (tab) { case 0, 1 -> "Đã chặn"; case 2 -> "Số cá nhân"; case 3 -> "Chặn trên máy này"; default -> "Phiên bản 1.2"; };
        for (int i = 0; i < 2; i++) {
            final boolean second = i == 1; LinearLayout entry = column(); entry.setGravity(Gravity.CENTER);
            TextView label = text(second ? right : left, 15, onlyBlocked == second ? BLUE : GRAY, onlyBlocked == second);
            label.setGravity(Gravity.CENTER); label.setPadding(0, dp(15), 0, dp(14)); entry.addView(label, new LinearLayout.LayoutParams(-1, -2));
            View underline = new View(this); underline.setBackgroundColor(onlyBlocked == second ? BLUE : Color.TRANSPARENT); entry.addView(underline, new LinearLayout.LayoutParams(-1, dp(3)));
            subnav.addView(entry, new LinearLayout.LayoutParams(0, -2, 1)); if (tab < 3) entry.setOnClickListener(v -> { onlyBlocked = second; renderPage(); });
        }
        switch (tab) { case 0 -> calls(); case 1 -> catalog(false); case 2 -> { if (onlyBlocked) reports(true); else catalog(true); } case 3 -> reports(false); default -> settings(); }
    }
    private void status() {
        LinearLayout box = row(); box.setPadding(dp(12), dp(11), dp(8), dp(11)); box.setBackground(bg(SOFT, 12)); boolean active = hasRole() && store.enabled();
        box.addView(icon(active ? "blocked" : "shield", BLUE, 28)); TextView label = text(active ? "Đang bảo vệ cuộc gọi" : "Chưa kích hoạt bảo vệ", 12, INK, true);
        box.addView(label, new LinearLayout.LayoutParams(0, -2, 1)); TextView action = text(active ? "Quản lý" : "Bật ngay", 12, BLUE, true);
        action.setPadding(dp(8), dp(6), dp(8), dp(6)); box.addView(action);
        action.setOnClickListener(v -> { if (!hasRole()) activate(); else if (!store.enabled()) { store.enabled(true); renderPage(); } else navigate(4); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(14), 0, dp(6)); page.addView(box, lp);
    }
    private void catalog(boolean blockedPage) {
        status();
        LinearLayout heading = row(); heading.setPadding(0, dp(14), 0, dp(5));
        heading.addView(text(blockedPage ? "Thương hiệu đã chặn" : "Thương hiệu", 20, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (!blockedPage) {
            TextView add = text("+ Chặn theo tên", 12, BLUE, true); add.setPadding(dp(8), dp(10), 0, dp(10));
            add.setOnClickListener(v -> blockByNameDialog(query)); heading.addView(add);
        }
        page.addView(heading);
        List<Brand> brands = new ArrayList<>(store.brands()); Map<String, List<String>> aliases = store.aliases();
        brands.sort(Comparator.comparing((Brand b) -> !store.brandMetadata(b.name()).optBoolean("demo")));
        List<Brand> visible = new ArrayList<>();
        for (Brand brand : brands) if ((!blockedPage && !onlyBlocked || brand.enabled()) && BrandSearch.matches(query, brand, aliases)) visible.add(brand);
        caption((blockedPage || onlyBlocked ? "ĐANG ÁP DỤNG" : "DANH MỤC THƯƠNG HIỆU") + "  ·  " + visible.size());
        if (visible.isEmpty()) empty("brands", "Không tìm thấy thương hiệu", query.isEmpty() ? "Chọn thương hiệu trong Danh mục để bắt đầu chặn." : "Thử tên khác hoặc thêm một thương hiệu mới.");
        for (int i = 0; i < visible.size(); i++) brandRow(visible.get(i), i);
    }
    private void brandRow(Brand brand, int index) {
        LinearLayout item = row(); item.setPadding(0, dp(14), 0, dp(14)); item.addView(avatar(brand.name(), index, 50));
        LinearLayout detail = column(); detail.setPadding(dp(14), 0, dp(6), 0);
        TextView name = text(brand.name(), 17, INK, true); name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END); detail.addView(name);
        JSONObject meta = store.brandMetadata(brand.name()); TextView summary = text(meta.optString("category", "Số đã được gắn nhãn"), 12, GRAY, false);
        summary.setSingleLine(true); summary.setEllipsize(TextUtils.TruncateAt.END); detail.addView(summary); gap(detail, 4);
        detail.addView(text(store.brandNumbers(brand).size() + " số" + (brand.enabled() ? "  ·  " + actionLabel(brand.action()) : ""), 11, brand.enabled() ? BLUE : GRAY, false));
        item.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); GlyphView action = icon(brand.enabled() ? "blocked" : "shield", brand.enabled() ? BLUE : 0xffb1bac6, 44);
        action.setBackground(bg(brand.enabled() ? SOFT : 0xfff6f7f9, 30)); action.setContentDescription((brand.enabled() ? "Quản lý " : "Chặn ") + brand.name()); item.addView(action);
        action.setOnClickListener(v -> { if (brand.enabled()) brandDetails(brand.name()); else { setBrandPolicy(brand.name(), true, Action.BLOCK); renderPage(); toast("Đã chặn toàn bộ số của " + brand.name()); } });
        item.setOnClickListener(v -> brandDetails(brand.name())); item.setTag("brand-" + normalizeName(brand.name())); page.addView(item); divider(page);
    }
    private void setBrandPolicy(String name, boolean enabled, Action action) {
        List<Brand> brands = store.brands();
        for (int i = 0; i < brands.size(); i++) {
            Brand brand = brands.get(i); if (!normalizeName(brand.name()).equals(normalizeName(name))) continue;
            brands.set(i, new Brand(brand.name(), brand.numbers(), enabled, action)); store.saveBrands(brands); return;
        }
    }
    private void sheet(String heading, LinearLayout content) {
        if (activeSheet != null && activeSheet.isShowing()) activeSheet.dismiss();
        Dialog dialog = new Dialog(this); activeSheet = dialog; LinearLayout shell = body(); shell.setBackground(bg(Color.WHITE, 24)); shell.addView(text(heading, 23, INK, true)); gap(shell, 12);
        shell.addView(content); button(shell, "Đóng", false, dialog::dismiss); ScrollView scroll = new ScrollView(this); scroll.addView(shell); dialog.setContentView(scroll);
        if (dialog.getWindow() != null) { dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); dialog.getWindow().setGravity(Gravity.BOTTOM); }
        dialog.show(); if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, Math.min(dp(600), (int) (getResources().getDisplayMetrics().heightPixels * .87)));
    }
    private void brandDetails(String name) {
        Brand brand = store.resolveBrand(name); if (brand == null) return; LinearLayout content = column(); JSONObject meta = store.brandMetadata(name);
        content.addView(text(meta.optString("category", "Thương hiệu của bạn"), 14, GRAY, false)); gap(content, 8);
        button(content, "Chặn tất cả " + store.brandNumbers(brand).size() + " số", true, () -> { setBrandPolicy(name, true, Action.BLOCK); renderPage(); toast("Đã chặn " + name); });
        button(content, "Chỉ tắt chuông nhóm này", false, () -> { setBrandPolicy(name, true, Action.SILENCE); renderPage(); toast("Đã đặt chế độ tắt chuông"); });
        button(content, "Bỏ chặn thương hiệu", false, () -> { setBrandPolicy(name, false, brand.action()); renderPage(); toast("Đã tắt quy tắc nhóm. Blacklist cá nhân vẫn áp dụng."); });
        button(content, "Báo cáo thêm số của thương hiệu", false, () -> reportDialog("", name)); gap(content, 16); content.addView(text("CÁC SỐ TRONG NHÓM", 12, GRAY, true));
        for (String number : store.brandNumbers(brand)) {
            LinearLayout line = row(); line.setPadding(0, dp(10), 0, dp(10)); line.addView(icon("phone", BLUE, 28));
            TextView label = text(displayNumber(number), 16, INK, false); label.setPadding(dp(8), 0, 0, 0); line.addView(label); content.addView(line);
        }
        button(content, "Sửa tên & danh sách số", false, () -> editBrandWithName(name, name));
        sheet(name, content);
    }
    private void blockByNameDialog(String initial) {
        LinearLayout box = body(); AutoCompleteTextView name = new AutoCompleteTextView(this); name.setTextSize(17); name.setHint("Ví dụ: Nova, SunTravel…");
        name.setText(initial); name.setSingleLine(true); name.setThreshold(1); name.setTag("block-brand-input");
        List<String> choices = new ArrayList<>(); for (Brand brand : store.brands()) choices.add(brand.name()); for (List<String> aliases : store.aliases().values()) choices.addAll(aliases);
        name.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, choices)); box.addView(name, new LinearLayout.LayoutParams(-1, dp(56)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Chặn theo tên thương hiệu").setView(box).setPositiveButton("Chặn thương hiệu", null)
            .setNeutralButton("Thêm tên mới", (d, w) -> editBrandWithName("", name.getText().toString())).setNegativeButton("Hủy", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Brand brand = store.resolveBrand(name.getText().toString()); if (brand == null) { name.setError("Chọn đúng tên trong gợi ý, hoặc dùng Thêm tên mới"); return; }
            if (store.brandNumbers(brand).isEmpty()) { name.setError("Tên này chưa có số. Hãy thêm số trong chi tiết thương hiệu."); return; }
            setBrandPolicy(brand.name(), true, Action.BLOCK); dialog.dismiss(); tab = 1; query = ""; onlyBlocked = true; render();
            toast("Đã chặn toàn bộ " + store.brandNumbers(brand).size() + " số của " + brand.name());
        })); dialog.show();
    }
    private void editBrandWithName(String originalName, String initialName) {
        Brand original = store.resolveBrand(originalName); LinearLayout box = body(); EditText name = input(box, "Tên thương hiệu", initialName, false);
        EditText numbers = input(box, "Các số, mỗi số một dòng", original == null ? "" : String.join("\n", original.numbers()), false);
        numbers.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE); numbers.setMinLines(3); numbers.setMaxLines(6);
        ScrollView scroll = new ScrollView(this); scroll.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(original == null ? "Thêm thương hiệu" : "Chỉnh sửa thương hiệu").setView(scroll).setPositiveButton("Lưu & chặn", null).setNegativeButton("Hủy", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String label = name.getText().toString().trim(); if (label.isEmpty() || label.length() > 80) { name.setError("Nhập tên từ 1 đến 80 ký tự"); return; }
            List<Brand> brands = store.brands(); int index = -1;
            for (int i = 0; i < brands.size(); i++) {
                if (original != null && normalizeName(brands.get(i).name()).equals(normalizeName(original.name()))) index = i;
                else if (normalizeName(brands.get(i).name()).equals(normalizeName(label))) { name.setError("Tên đã có trong danh mục"); return; }
            }
            Set<String> parsed = new LinkedHashSet<>(); for (String part : numbers.getText().toString().split("[,;\\n\\r]+")) {
                if (part.trim().isEmpty()) continue; String number = normalizeNumber(part.trim()); if (number.isEmpty()) { numbers.setError("Số không hợp lệ: " + part.trim()); return; } parsed.add(number);
            }
            if (parsed.isEmpty()) { numbers.setError("Nhập ít nhất một số thuộc thương hiệu"); return; }
            Brand updated = new Brand(label, parsed, true, Action.BLOCK); if (index < 0) brands.add(updated); else brands.set(index, updated);
            store.saveBrands(brands); dialog.dismiss(); renderPage(); toast("Đã lưu và chặn " + label);
        })); dialog.show();
    }
    private String displayNumber(String number) {
        if (number.startsWith("1202555") && number.length() == 11) return "+1 202 555 " + number.substring(7);
        if (number.startsWith("84") && number.length() >= 11) return "0" + number.substring(2); return number;
    }
    private String brandFor(String number) {
        String canonical = normalizeNumber(number); for (Brand brand : store.brands()) if (brand.numbers().contains(canonical)) return brand.name();
        for (SpamReport report : store.reports()) if (report.number().equals(canonical)) return report.brand(); return displayNumber(canonical.isEmpty() ? number : canonical);
    }
    private void calls() {
        status(); title("Nhật ký sàng lọc"); JSONArray history = store.history();
        int visible = 0; for (int i = 0; i < history.length(); i++) {
            JSONObject entry = history.optJSONObject(i); if (entry == null) continue;
            String number = entry.optString("number"), name = brandFor(number); Action action = Action.valueOf(entry.optString("action", "ALLOW"));
            if (onlyBlocked && action == Action.ALLOW || !normalizeName(name + " " + number).contains(normalizeName(query))) continue;
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(entry.optLong("time")));
            callRow(name, number, actionLabel(action) + " · " + time, visible++);
        }
        if (visible == 0) empty("clock", "Chưa có cuộc gọi phù hợp", "Thử đổi từ khóa hoặc xem tab Gần đây.");
    }
    private void callRow(String name, String number, String summary, int index) {
        LinearLayout item = row(); item.setPadding(0, dp(16), 0, dp(16)); item.addView(avatar(name, index, 52));
        LinearLayout detail = column(); detail.setPadding(dp(14), 0, dp(6), 0); TextView label = text(name, 17, INK, true);
        label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END); detail.addView(label); gap(detail, 5); detail.addView(text(summary, 12, GRAY, false));
        item.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); GlyphView action = icon("chevron", BLUE, 44); action.setBackground(bg(0xfff5f7fa, 30)); item.addView(action);
        item.setOnClickListener(v -> {
            LinearLayout content = column(); content.addView(text(displayNumber(number), 21, INK, true)); gap(content, 10); content.addView(text(summary, 14, GRAY, false));
            button(content, "Báo cáo & chặn số này", true, () -> reportDialog(number, store.resolveBrand(name) != null ? name : ""));
            button(content, "Luôn cho phép số này", false, () -> addException(number)); sheet(name, content);
        }); action.setContentDescription("Chi tiết cuộc gọi " + name); action.setOnClickListener(v -> item.performClick()); page.addView(item); divider(page);
    }
    private void empty(String glyph, String heading, String detail) {
        LinearLayout box = column(); box.setGravity(Gravity.CENTER); box.setPadding(dp(16), dp(30), dp(16), dp(26)); box.addView(icon(glyph, BLUE, 60)); gap(box, 14);
        TextView name = text(heading, 19, INK, true); name.setGravity(Gravity.CENTER); box.addView(name); page.addView(box);
    }
    private void reports(boolean blacklistPage) {
        title(blacklistPage ? "Số chặn cá nhân" : "Báo cáo số làm phiền");
        button(page, "+  Báo cáo số spam", true, () -> reportDialog("", "")); List<SpamReport> reports = store.reports(); caption("BLACKLIST CÁ NHÂN · " + reports.size()); int count = 0;
        for (SpamReport report : reports) {
            if (!normalizeName(report.brand() + " " + report.number()).contains(normalizeName(query))) continue;
            LinearLayout item = row(); item.setPadding(0, dp(15), 0, dp(15)); item.addView(avatar(report.brand(), count++, 48));
            LinearLayout detail = column(); detail.setPadding(dp(12), 0, 0, 0); detail.addView(text(report.brand(), 17, INK, true));
            detail.addView(text(displayNumber(report.number()), 14, GRAY, false)); gap(detail, 3); detail.addView(text(report.reason(), 12, GRAY, false));
            item.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); item.addView(icon("blocked", BLUE, 36)); page.addView(item); divider(page);
            item.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(displayNumber(report.number()))
                .setMessage(report.brand() + "\n" + report.reason() + "\n\nGỡ khỏi blacklist cá nhân? Quy tắc thương hiệu và số ngoại lệ vẫn được áp dụng.")
                .setPositiveButton("Gỡ báo cáo", (d, w) -> { store.removeReport(report.number()); renderPage(); })
                .setNeutralButton("Luôn cho phép", (d, w) -> addException(report.number())).setNegativeButton("Đóng", null).show());
        }
        if (count == 0) empty("report", "Chưa có báo cáo phù hợp", "Báo cáo từ Nhật ký hoặc chi tiết thương hiệu.");
    }
    private void reportDialog(String initialNumber, String initialBrand) {
        LinearLayout box = body(); EditText number = input(box, "Số điện thoại cần chặn", initialNumber, true);
        AutoCompleteTextView brand = new AutoCompleteTextView(this); brand.setHint("Thương hiệu hoặc tên tự xưng"); brand.setText(initialBrand); brand.setTextSize(16); brand.setThreshold(1);
        List<String> names = new ArrayList<>(); for (Brand b : store.brands()) names.add(b.name()); brand.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        box.addView(brand, new LinearLayout.LayoutParams(-1, dp(54))); EditText reason = input(box, "Lý do báo cáo", "Quảng cáo làm phiền", false);
        ScrollView scroll = new ScrollView(this); scroll.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Báo cáo số mới").setView(scroll).setPositiveButton("Báo cáo & chặn", null).setNegativeButton("Hủy", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String canonical = normalizeNumber(number.getText().toString()); if (canonical.isEmpty()) { number.setError("Nhập số hợp lệ"); return; }
            String label = brand.getText().toString().trim(); Brand known = store.resolveBrand(label); if (known != null) label = known.name(); if (label.isEmpty()) label = "Chưa xác định";
            if (label.length() > 80) { brand.setError("Tối đa 80 ký tự"); return; } String explanation = reason.getText().toString().trim();
            if (explanation.isEmpty() || explanation.length() > 500) { reason.setError("Nhập lý do từ 1 đến 500 ký tự"); return; }
            try { store.report(new SpamReport(canonical, label, explanation, System.currentTimeMillis())); } catch (IllegalArgumentException e) { number.setError(e.getMessage()); return; }
            dialog.dismiss(); tab = 3; query = ""; onlyBlocked = false; render(); toast(hasRole() && store.enabled() ? "Đã thêm số vào blacklist" : "Đã lưu. Bật bảo vệ để chặn cuộc gọi.");
        })); dialog.show();
    }
    private void settings() {
        if (settingsPage == 1) { exceptions(); return; } if (settingsPage == 2) { simulator(); return; }
        title("Cài đặt"); status(); Switch enabled = new Switch(this);
        enabled.setText("Bật bảo vệ cuộc gọi"); enabled.setTextColor(INK); enabled.setChecked(store.enabled()); enabled.setPadding(0, dp(18), 0, dp(18));
        enabled.setOnCheckedChangeListener((v, checked) -> { store.enabled(checked); renderPage(); }); page.addView(enabled); divider(page);
        settingRow("shield", "Vai trò Call Screening", hasRole() ? "QuietCall đã được cấp vai trò sàng lọc" : "Chọn QuietCall để sàng lọc cuộc gọi", this::activate);
        settingRow("check", "Số luôn cho phép", store.allowed().size() + " số ngoại lệ", () -> { settingsPage = 1; renderPage(); });
        settingRow("search", "Kiểm tra một số", "", () -> { settingsPage = 2; renderPage(); });
        settingRow("brands", "Nhập danh mục CSV", "", this::pickCsv);
        settingRow("report", "Nguồn dữ liệu", "", this::sources);
        settingRow("clock", "Xóa nhật ký cuộc gọi", "", () -> new AlertDialog.Builder(this).setMessage("Xóa toàn bộ nhật ký?")
            .setPositiveButton("Xóa", (d, w) -> { store.clearHistory(); toast("Đã xóa nhật ký"); }).setNegativeButton("Hủy", null).show());
        gap(page, 20); page.addView(text("QuietCall 1.2\n", 12, GRAY, false));
    }
    private void settingRow(String glyph, String heading, String subtitle, Runnable action) {
        LinearLayout item = row(); item.setPadding(0, dp(18), 0, dp(18)); item.addView(icon(glyph, BLUE, 36)); LinearLayout detail = column();
        detail.setPadding(dp(10), 0, 0, 0); detail.addView(text(heading, 16, INK, true));
        if (!subtitle.isEmpty()) { gap(detail, 4); detail.addView(text(subtitle, 12, GRAY, false)); }
        item.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); item.addView(icon("chevron", GRAY, 28)); item.setOnClickListener(v -> action.run()); page.addView(item); divider(page);
    }
    private void exceptions() {
        button(page, "‹  Cài đặt", false, () -> { settingsPage = 0; renderPage(); }); title("Luôn cho phép");
        button(page, "+  Thêm số ngoại lệ", true, () -> addException(""));
        for (String number : store.allowed()) settingRow("phone", displayNumber(number), "Chạm để bỏ khỏi ngoại lệ", () -> { Set<String> allowed = store.allowed(); allowed.remove(number); store.saveAllowed(allowed); renderPage(); });
    }
    private void addException(String initial) {
        LinearLayout box = body(); EditText number = input(box, "Số luôn cho phép", initial, true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Thêm ngoại lệ").setView(box).setPositiveButton("Cho phép", null).setNegativeButton("Hủy", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String canonical = normalizeNumber(number.getText().toString()); if (canonical.isEmpty()) { number.setError("Nhập số hợp lệ"); return; }
            Set<String> allowed = store.allowed(); allowed.add(canonical); store.saveAllowed(allowed); dialog.dismiss(); renderPage(); toast("Đã thêm số luôn cho phép");
        })); dialog.show();
    }
    private void simulator() {
        button(page, "‹  Cài đặt", false, () -> { settingsPage = 0; renderPage(); }); title("Kiểm tra quy tắc");
        EditText number = input(page, "Ví dụ: +1 202 555 0101", "", true); TextView result = text("", 18, INK, true); result.setPadding(0, dp(18), 0, dp(16));
        button(page, "Kiểm tra số này", true, () -> {
            if (normalizeNumber(number.getText().toString()).isEmpty()) { number.setError("Nhập số hợp lệ"); return; }
            Decision decision = evaluate(number.getText().toString(), store.enabled(), store.allowed(), store.brands(), store.blacklist());
            result.setText(actionLabel(decision.action()) + "\n" + decision.reason() + (!hasRole() ? "\n\nChưa cấp vai trò lọc cuộc gọi trên Android." : ""));
        }); page.addView(result); button(page, "Chạy thử Nova Finance", false, () -> {
            Brand nova = store.resolveBrand("Nova"); if (nova == null || nova.numbers().isEmpty()) return; String sample = nova.numbers().iterator().next(); number.setText("+" + sample);
            Decision decision = evaluate(sample, store.enabled(), store.allowed(), store.brands(), store.blacklist()); result.setText(actionLabel(decision.action()) + "\n" + decision.reason());
        });
    }
    private void sources() {
        LinearLayout content = column();
        JSONArray rows = store.bundledSources(); Set<String> shown = new HashSet<>(); for (int i = 0; i < rows.length(); i++) {
            JSONObject entry = rows.optJSONObject(i); if (entry == null) continue; String key = entry.optString("brand") + entry.optString("sourceUrl"); if (!shown.add(key)) continue;
            gap(content, 16); content.addView(text(entry.optString("brand"), 16, INK, true));
            String publisher = entry.optBoolean("demo") ? "QuietCall · dữ liệu thử nghiệm" : entry.optString("publisher");
            content.addView(text(publisher + " · " + entry.optString("publishedAt"), 12, GRAY, false));
            TextView link = text(entry.optString("sourceUrl"), 12, BLUE, false); android.text.util.Linkify.addLinks(link, android.text.util.Linkify.WEB_URLS); content.addView(link);
        } sheet("Về danh mục", content);
    }
    private void pickCsv() { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), 20); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data); if (request != 20 || result != RESULT_OK || data == null || data.getData() == null) return; android.net.Uri uri = data.getData();
        new Thread(() -> {
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new IllegalArgumentException("Không mở được tệp"); ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int length;
                while ((length = stream.read(buffer)) != -1) { if (bytes.size() + length > 1024 * 1024) throw new IllegalArgumentException("Tệp vượt quá 1 MB"); bytes.write(buffer, 0, length); }
                List<DirectoryCsv.Entry> entries = DirectoryCsv.parse(bytes.toString(StandardCharsets.UTF_8.name())); runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return; new AlertDialog.Builder(this).setTitle("Nhập " + entries.size() + " dòng dữ liệu?")
                        .setMessage("Ghép số vào thương hiệu trùng tên. Giữ quy tắc hiện có; thương hiệu mới mặc định chưa chặn.")
                        .setPositiveButton("Nhập", (d, w) -> { store.saveBrands(DirectoryCsv.merge(store.brands(), entries)); renderPage(); toast("Đã cập nhật danh mục"); }).setNegativeButton("Hủy", null).show();
                });
            } catch (Exception e) { runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) toast("Không nhập được: " + e.getMessage()); }); }
        }, "directory-import").start();
    }
}
