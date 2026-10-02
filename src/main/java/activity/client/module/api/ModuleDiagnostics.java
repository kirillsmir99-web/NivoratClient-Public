package activity.client.module.api;

import java.util.HashMap;
import java.util.Map;

public final class ModuleDiagnostics {
    private static final long INTERVAL_NS = 30_000_000_000L;
    private static final Map<String, Failure> failures = new HashMap<>();
    private static final class Failure { long last; int suppressed; boolean logged; }
    private ModuleDiagnostics() {}

    public static synchronized void report(String module, String phase, Throwable error) {
        String key = module + ":" + phase;
        Failure failure = failures.computeIfAbsent(key, unused -> new Failure());
        long now = System.nanoTime();
        if (failure.logged && now - failure.last < INTERVAL_NS) { failure.suppressed++; return; }
        activity.client.ActivityClient.LOGGER.error("Module {} failed in {} ({} repeated errors suppressed)", module, phase, failure.suppressed, error);
        activity.client.diagnostic.DiagnosticEngine.recordError(module, phase, error);
        failure.logged = true;
        failure.last = now;
        failure.suppressed = 0;
    }
}
