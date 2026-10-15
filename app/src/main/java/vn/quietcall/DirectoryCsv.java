package vn.quietcall;

import java.util.*;
import static vn.quietcall.ScreeningEngine.*;

/** Validates the entire file before applying any change. CSV has brand,number columns. */
public final class DirectoryCsv {
    public record Entry(String brand, String number) {}

    public static List<Entry> parse(String source) {
        if (source.startsWith("\uFEFF")) source = source.substring(1);
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>(); StringBuilder field = new StringBuilder();
        boolean quoted = false, closed = false;
        // Sentinel newline flushes the final record without allowing unclosed quotes.
        for (int i = 0; i <= source.length(); i++) {
            char c = i == source.length() ? '\n' : source.charAt(i);
            if (quoted) {
                if (i == source.length()) throw new IllegalArgumentException("Thiếu dấu đóng ngoặc kép trong CSV");
                if (c == '"') {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '"') { field.append('"'); i++; }
                    else { quoted = false; closed = true; }
                } else field.append(c);
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(field.toString()); field.setLength(0); closed = false;
                if (c != ',') {
                    if (row.size() != 1 || !row.get(0).trim().isEmpty()) rows.add(row);
                    row = new ArrayList<>();
                    if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                }
            } else if (c == '"') {
                if (field.length() != 0 || closed) throw new IllegalArgumentException("Ngoặc kép không hợp lệ");
                quoted = true;
            } else {
                if (closed) throw new IllegalArgumentException("Ký tự thừa sau ngoặc kép");
                field.append(c);
            }
        }
        if (rows.isEmpty() || !rows.get(0).equals(Arrays.asList("brand", "number")))
            throw new IllegalArgumentException("Dòng đầu phải là brand,number");
        if (rows.size() > 10001) throw new IllegalArgumentException("Tối đa 10.000 dòng dữ liệu");
        List<Entry> entries = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> cells = rows.get(i);
            if (cells.size() != 2) throw new IllegalArgumentException("Dòng " + (i + 1) + " phải có hai cột");
            String name = cells.get(0).trim(), number = normalizeNumber(cells.get(1).trim());
            if (name.isEmpty() || name.length() > 80 || number.isEmpty())
                throw new IllegalArgumentException("Tên hoặc số không hợp lệ ở dòng " + (i + 1));
            entries.add(new Entry(name, number));
        }
        if (entries.isEmpty()) throw new IllegalArgumentException("Tệp chưa có dữ liệu");
        return entries;
    }

    /** Existing policies survive updates; newly discovered brands start disabled. */
    public static List<Brand> merge(List<Brand> current, List<Entry> entries) {
        Map<String, Brand> map = new LinkedHashMap<>();
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (Brand brand : current) {
            String key = normalizeName(brand.name());
            map.put(key, brand); grouped.put(key, new LinkedHashSet<>(brand.numbers()));
        }
        for (Entry entry : entries) {
            String key = normalizeName(entry.brand()); Brand prior = map.get(key);
            if (prior == null) {
                map.put(key, new Brand(entry.brand(), Collections.emptySet(), false, Action.BLOCK));
                grouped.put(key, new LinkedHashSet<>());
            }
            grouped.get(key).add(entry.number());
        }
        List<Brand> result = new ArrayList<>();
        for (Map.Entry<String, Brand> entry : map.entrySet()) {
            Brand brand = entry.getValue();
            result.add(new Brand(brand.name(), grouped.get(entry.getKey()), brand.enabled(), brand.action()));
        }
        return result;
    }
    private DirectoryCsv() {}
}
