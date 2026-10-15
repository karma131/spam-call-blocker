package vn.quietcall;

import java.util.*;
import static vn.quietcall.ScreeningEngine.*;

/** The name selects a known group; incoming calls still match complete phone numbers. */
public final class BrandSearch {
    public static Brand resolve(String query, List<Brand> brands, Map<String, List<String>> aliases) {
        String key = normalizeName(query);
        if (key.isEmpty()) return null;
        for (Brand brand : brands) {
            if (normalizeName(brand.name()).equals(key)) return brand;
            for (String alias : aliases.getOrDefault(normalizeName(brand.name()), Collections.emptyList()))
                if (normalizeName(alias).equals(key)) return brand;
        }
        return null;
    }
    public static boolean matches(String query, Brand brand, Map<String, List<String>> aliases) {
        String key = normalizeName(query);
        if (key.isEmpty() || normalizeName(brand.name()).contains(key)) return true;
        for (String alias : aliases.getOrDefault(normalizeName(brand.name()), Collections.emptyList()))
            if (normalizeName(alias).contains(key)) return true;
        if (!query.trim().matches("\\+?[0-9() .-]+")) return false;
        String digits = query.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return false;
        String canonical = normalizeNumber(query);
        for (String number : brand.numbers())
            if (number.contains(digits) || (!canonical.isEmpty() && number.equals(canonical))) return true;
        return false;
    }
    private BrandSearch() {}
}
