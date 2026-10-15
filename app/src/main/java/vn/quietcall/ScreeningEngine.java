package vn.quietcall;

import java.text.Normalizer;
import java.util.*;

/** Pure Java policy. No network or Android calls on the screening path. */
public final class ScreeningEngine {
    public enum Action { ALLOW, BLOCK, SILENCE }
    public record Brand(String name, Set<String> numbers, boolean enabled, Action action) {}
    public record Decision(Action action, String reason) {}

    public static String normalizeName(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
            .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Canonicalize Vietnamese domestic numbers; preserve other country codes. */
    public static String normalizeNumber(String input) {
        if (input == null || !input.trim().matches("\\+?[0-9() .-]+")) return "";
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.startsWith("0") && digits.length() >= 10 && digits.length() <= 11)
            digits = "84" + digits.substring(1);
        return digits.length() >= 8 && digits.length() <= 15 ? digits : "";
    }

    public static Decision evaluate(String number, boolean protection,
            Set<String> allowed, List<Brand> brands) {
        return evaluate(number, protection, allowed, brands, Collections.emptyMap());
    }

    public static Decision evaluate(String number, boolean protection,
            Set<String> allowed, List<Brand> brands, Map<String, String> blacklist) {
        if (!protection) return new Decision(Action.ALLOW, "Bảo vệ đang tạm dừng");
        String key = normalizeNumber(number);
        if (key.isEmpty()) return new Decision(Action.ALLOW, "Không xác định được số gọi");
        if (allowed.contains(key)) return new Decision(Action.ALLOW, "Số luôn cho phép");
        if (blacklist.containsKey(key)) return new Decision(Action.BLOCK, "Báo cáo cá nhân: " + blacklist.get(key));
        Brand matched = null;
        for (Brand brand : brands) {
            if (!brand.enabled() || !brand.numbers().contains(key)) continue;
            // Blocking wins if imported datasets associate one number with multiple brands.
            if (brand.action() == Action.BLOCK) return new Decision(Action.BLOCK, brand.name());
            if (brand.action() == Action.SILENCE) matched = brand;
        }
        if (matched != null) return new Decision(Action.SILENCE, matched.name());
        return new Decision(Action.ALLOW, "Chưa có trong dữ liệu thương hiệu bị chặn");
    }

    private ScreeningEngine() {}
}
