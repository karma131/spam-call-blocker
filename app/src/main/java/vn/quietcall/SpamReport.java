package vn.quietcall;

import java.util.*;

/** A user's own assessment, never represented as a verified community report. */
public record SpamReport(String number, String brand, String reason, long time) {
    public SpamReport {
        number = ScreeningEngine.normalizeNumber(number);
        brand = brand == null ? "" : brand.trim();
        reason = reason == null ? "" : reason.trim();
        if (number.isEmpty()) throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        if (brand.isEmpty() || brand.length() > 80) throw new IllegalArgumentException("Tên thương hiệu cần từ 1 đến 80 ký tự");
        if (reason.isEmpty() || reason.length() > 500) throw new IllegalArgumentException("Lý do cần từ 1 đến 500 ký tự");
    }

    public static List<SpamReport> upsert(List<SpamReport> current, SpamReport report) {
        List<SpamReport> result = new ArrayList<>(); result.add(report);
        for (SpamReport prior : current) if (!prior.number().equals(report.number())) result.add(prior);
        return result;
    }
    public static Map<String, String> blacklist(List<SpamReport> reports) {
        Map<String, String> result = new LinkedHashMap<>();
        for (SpamReport report : reports) result.put(report.number(), report.brand());
        return result;
    }
}
