package vn.quietcall;

import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import java.util.*;
import static vn.quietcall.ScreeningEngine.*;

/** Device checks against isolated test preferences; never clears user rules. */
public final class StoreTestRunner extends Instrumentation {
    private int checks;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message); checks++;
    }
    @Override public void onStart() {
        Context context = getTargetContext();
        String key = "quietcall_test_" + UUID.randomUUID(); Bundle result = new Bundle();
        try {
            RuleStore store = new RuleStore(context, key);
            store.installBundledDirectory(context);
            check(store.brands().size() == 8, "Eight initial groups");
            check(store.bundledSources().length() == 24, "24 source records");
            check(store.brands().stream().mapToInt(b -> b.numbers().size()).sum() == 24, "24 unique numbers");
            Brand nova = store.resolveBrand("Nova");
            check(nova != null && !nova.enabled() && nova.numbers().size() == 3, "Demo group starts unblocked and resolves by alias");
            check(evaluate("02488860469", true, store.allowed(), store.brands(), store.blacklist()).action() == Action.BLOCK, "Bundled number blocks");
            List<Brand> brands = store.brands(); Brand first = brands.get(0);
            brands.set(0, new Brand(first.name(), first.numbers(), false, Action.SILENCE)); store.saveBrands(brands);
            store.installBundledDirectory(context);
            check(!store.brands().get(0).enabled() && store.brands().get(0).action() == Action.SILENCE, "Reopen preserves policy");
            store.saveBrands(Collections.emptyList()); store.installBundledDirectory(context);
            check(store.brands().isEmpty(), "Deleted groups are not restored on reopen");
            store.report(new SpamReport("0900000001", "Test only", "Nhá máy", 10));
            RuleStore reopened = new RuleStore(context, key);
            check(reopened.reports().size() == 1, "Report persists");
            check(evaluate("+84900000001", true, reopened.allowed(), reopened.brands(), reopened.blacklist()).action() == Action.BLOCK, "Persisted report blocks");
            reopened.report(new SpamReport("+84900000001", "Updated", "Quảng cáo", 20));
            check(reopened.reports().size() == 1 && reopened.reports().get(0).brand().equals("Updated"), "Repeated report updates");
            reopened.saveAllowed(new LinkedHashSet<>(Arrays.asList("84900000001")));
            check(evaluate("0900000001", true, reopened.allowed(), reopened.brands(), reopened.blacklist()).action() == Action.ALLOW, "Exception overrides report");
            boolean rejected = false;
            try { reopened.report(new SpamReport("0900000001", "Test", "Reason", 30)); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected && reopened.reports().get(0).time() == 20, "Exception conflict does not mutate report");
            reopened.saveAllowed(Collections.emptySet()); reopened.removeReport("84900000001");
            check(new RuleStore(context, key).blacklist().isEmpty(), "Removal persists");
            check(evaluate("0900000001", true, reopened.allowed(), reopened.brands(), reopened.blacklist()).action() == Action.ALLOW, "Removed report stops blocking");
            result.putString("stream", "PASS: " + checks + " Android storage and seed checks\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL after " + checks + " checks: " + e + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally { context.deleteSharedPreferences(key); }
    }
}
