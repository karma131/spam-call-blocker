package vn.quietcall;

import android.telecom.Call;
import android.telecom.CallScreeningService;
import static vn.quietcall.ScreeningEngine.*;

public final class SpamScreeningService extends CallScreeningService {
    @Override public void onScreenCall(Call.Details details) {
        if (details.getCallDirection() != Call.Details.DIRECTION_INCOMING) return;
        String number = details.getHandle() == null ? "" : details.getHandle().getSchemeSpecificPart();
        Decision decision;
        RuleStore store = new RuleStore(this);
        try {
            decision = ScreeningEngine.evaluate(number, store.enabled(), store.allowed(), store.brands(), store.blacklist());
        } catch (RuntimeException e) {
            decision = new Decision(Action.ALLOW, "Lỗi đọc quy tắc: cho phép để tránh chặn nhầm");
        }
        CallResponse.Builder response = new CallResponse.Builder();
        if (decision.action() == Action.BLOCK) {
            response.setDisallowCall(true).setRejectCall(true).setSkipNotification(true);
        } else if (decision.action() == Action.SILENCE) {
            response.setSilenceCall(true);
        }
        // Respond before persisting history. Never wait for a network caller-ID lookup.
        respondToCall(details, response.build());
        try { store.log(number, decision); } catch (RuntimeException ignored) { /* Response already delivered. */ }
    }
}
