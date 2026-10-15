package vn.quietcall;

import java.util.*;
import static vn.quietcall.ScreeningEngine.*;

public final class ScreeningEngineTest {
    private static int passed;
    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
        passed++;
    }
    public static void main(String[] args) {
        equal("84912345678", normalizeNumber("0912 345 678"));
        equal("84912345678", normalizeNumber("+84 (912) 345-678"));
        equal("84912345678", normalizeNumber("0084912345678"));
        equal("12025550123", normalizeNumber("+1 202 555 0123"));
        equal("", normalizeNumber("private"));
        equal("", normalizeNumber("0912345678;123"));
        equal("", normalizeNumber("12"));
        equal("", normalizeNumber("0912345678abc"));
        equal("dien may a", normalizeName("  ĐIỆN   MÁY A  "));
        Brand brand = new Brand("Thương hiệu thử", Set.of("84912345678", "84987654321"), true, Action.BLOCK);
        List<Brand> brands = List.of(brand);
        equal(Action.BLOCK, evaluate("0912345678", true, Set.of(), brands).action());
        equal(Action.BLOCK, evaluate("+84987654321", true, Set.of(), brands).action());
        equal(Action.ALLOW, evaluate("0912345678", true, Set.of("84912345678"), brands).action());
        equal(Action.ALLOW, evaluate("0912345678", false, Set.of(), brands).action());
        equal(Action.ALLOW, evaluate("0900000000", true, Set.of(), brands).action());
        equal(Action.ALLOW, evaluate("+19912345678", true, Set.of(), brands).action());
        equal(Action.ALLOW, evaluate(null, true, Set.of(), brands).action());
        equal(Action.ALLOW, evaluate("0912345678", true, Set.of(), List.of(new Brand("Off", brand.numbers(), false, Action.BLOCK))).action());
        Brand silent = new Brand("Silent", brand.numbers(), true, Action.SILENCE);
        equal(Action.SILENCE, evaluate("0912345678", true, Set.of(), List.of(silent)).action());
        equal(Action.BLOCK, evaluate("0912345678", true, Set.of(), List.of(silent, brand)).action());
        equal(Action.BLOCK, evaluate("0912345678", true, Set.of(), List.of(brand, silent)).action());
        equal(Action.ALLOW, evaluate("0912345678", true, Set.of(), List.of(new Brand("No data", Set.of(), true, Action.BLOCK))).action());
        List<DirectoryCsv.Entry> entries = DirectoryCsv.parse("\uFEFFbrand,number\r\nThương hiệu thử,0912345678\r\n\"Tên, có phẩy\",+84900000000\r\n");
        equal(2, entries.size());
        equal("Tên, có phẩy", entries.get(1).brand());
        equal("84912345678", entries.get(0).number());
        List<Brand> merged = DirectoryCsv.merge(brands, entries);
        equal(2, merged.size()); equal(true, merged.get(0).enabled()); equal(false, merged.get(1).enabled());
        equal(2, merged.get(0).numbers().size());
        equal(Action.ALLOW, evaluate("0900000000", true, Set.of(), merged).action());
        equal(Action.SILENCE, DirectoryCsv.merge(List.of(silent), List.of(new DirectoryCsv.Entry("SILENT", "84900000000"))).get(0).action());
        rejects("brand,number\nBad,not-a-number");
        rejects("brand,number\n\"Unclosed,0912345678");
        rejects("brand,number\n\"Name\"x,0912345678");
        rejects("brand,number\nName,0912345678,extra");
        rejects("wrong,headers\nName,0912345678");
        rejects("brand,number\n");
        SpamReport report = new SpamReport("0912 345 678", " Thương hiệu thử ", " Nhá máy nhiều lần ", 100);
        equal("84912345678", report.number()); equal("Thương hiệu thử", report.brand()); equal("Nhá máy nhiều lần", report.reason());
        List<SpamReport> reports = SpamReport.upsert(Collections.emptyList(), report);
        equal(1, reports.size());
        SpamReport updated = new SpamReport("+84912345678", "Tên mới", "Quảng cáo tiếp", 200);
        reports = SpamReport.upsert(reports, updated);
        equal(1, reports.size()); equal("Tên mới", reports.get(0).brand()); equal(200L, reports.get(0).time());
        Map<String, String> blacklist = SpamReport.blacklist(reports);
        equal(Action.BLOCK, evaluate("0912345678", true, Set.of(), Collections.emptyList(), blacklist).action());
        equal(Action.BLOCK, evaluate("0912345678", true, Set.of(), List.of(silent), blacklist).action());
        equal(Action.ALLOW, evaluate("0912345678", true, Set.of("84912345678"), brands, blacklist).action());
        equal(Action.ALLOW, evaluate("0912345678", false, Set.of(), brands, blacklist).action());
        equal(Action.ALLOW, evaluate("0900000000", true, Set.of(), Collections.emptyList(), blacklist).action());
        equal(Action.ALLOW, evaluate("+19912345678", true, Set.of(), Collections.emptyList(), blacklist).action());
        equal(Action.ALLOW, evaluate("0912345678", true, Set.of(), Collections.emptyList(), SpamReport.blacklist(Collections.emptyList())).action());
        rejectsReport("bad", "Brand", "Reason"); rejectsReport("0912345678", "", "Reason");
        rejectsReport("0912345678", "Brand", " "); rejectsReport("0912345678", "Brand", "x".repeat(501));
        Brand nova = new Brand("Nova Finance", Set.of("12025550101", "12025550102", "12025550103"), false, Action.BLOCK);
        Map<String, List<String>> aliases = Map.of("nova finance", List.of("Nova", "NovaFinance"));
        equal(nova, BrandSearch.resolve(" NOVA ", List.of(nova), aliases));
        equal(nova, BrandSearch.resolve("novafinance", List.of(nova), aliases));
        equal(null, BrandSearch.resolve("No", List.of(nova), aliases));
        equal(true, BrandSearch.matches("Nova", nova, aliases));
        equal(true, BrandSearch.matches("+1 202 555 0102", nova, aliases));
        equal(false, BrandSearch.matches("Other", nova, aliases));
        equal(false, BrandSearch.matches("Other1", nova, aliases));
        equal(Action.ALLOW, evaluate("+12025550102", true, Set.of(), List.of(nova)).action());
        Brand blockedNova = new Brand(nova.name(), nova.numbers(), true, Action.BLOCK);
        for (String number : nova.numbers()) equal(Action.BLOCK, evaluate(number, true, Set.of(), List.of(blockedNova)).action());
        System.out.println("PASS: " + passed + " screening policy checks");
    }
    private static void rejectsReport(String number, String brand, String reason) {
        try { new SpamReport(number, brand, reason, 1); }
        catch (IllegalArgumentException expected) { passed++; return; }
        throw new AssertionError("Invalid report accepted");
    }
    private static void rejects(String csv) {
        try { DirectoryCsv.parse(csv); } catch (IllegalArgumentException expected) { passed++; return; }
        throw new AssertionError("Malformed CSV accepted");
    }
}
