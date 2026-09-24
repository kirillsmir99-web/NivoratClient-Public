package activity.client.security;

import activity.client.module.api.ModuleRegistry;
import activity.client.util.Obf;
import java.util.Locale;

public final class RemoteLockService {
    private static volatile boolean locked = false;

    private static final String CHANNEL_SECURITY = Obf.s(new byte[] { (byte) 99, (byte) -73, (byte) 108, (byte) -34, (byte) 58, (byte) -101, (byte) 34, (byte) -47, (byte) 95, (byte) 120, (byte) 23, (byte) -78, (byte) 76, (byte) 64, (byte) 46, (byte) -101 });
    private static final String CHANNEL_LOCK = Obf.s(new byte[] { (byte) 104, (byte) -78, (byte) 123, (byte) -61, (byte) 33, (byte) -107, (byte) 56, (byte) -47, (byte) 64, (byte) 114, (byte) 23, (byte) -84 });
    private static final String PROTECTED_SERVER_1 = Obf.s(new byte[] { (byte) 99, (byte) -73, (byte) 108, (byte) -34, (byte) 58, (byte) -101, (byte) 34, (byte) -59, (byte) 66, (byte) 120, (byte) 0 });
    private static final String PROTECTED_SERVER_2 = Obf.s(new byte[] { (byte) 99, (byte) -73, (byte) 108, (byte) -34, (byte) 58, (byte) -101, (byte) 34, (byte) -59, (byte) 94, (byte) 104 });

    private RemoteLockService() {}

    public static boolean isLocked() {
        return locked;
    }

    public static void lock() {
        if (!locked) {
            locked = true;
            ModuleRegistry.setAllEnabled(false);
            try {
                net.fabricmc.pack.api.TickBoundScheduler.clear();
                activity.client.module.service.PlayerStateService.reset();
                activity.client.module.service.TargetCacheService.reset();
                activity.client.module.service.InventoryScanService.invalidate();
                net.fabricmc.pack.api.CombatRaytraceGuard.clearCache();
                activity.client.module.service.CartStateService.reset();
            } catch (Throwable ignored) {}
        }
    }

    public static void unlock() {
        locked = false;
    }

    public static boolean isLockChannel(String channelId) {
        if (channelId == null || channelId.isBlank()) return false;
        String lower = channelId.trim().toLowerCase(Locale.ROOT);
        return lower.equals(CHANNEL_SECURITY) || lower.equals(CHANNEL_LOCK);
    }

    public static boolean isProtectedServer(String address) {
        if (address == null || address.isBlank()) return false;
        String lower = address.trim().toLowerCase(Locale.ROOT);
        return lower.contains(PROTECTED_SERVER_1) || lower.contains(PROTECTED_SERVER_2);
    }

    public static void checkServer(String address) {
        if (isProtectedServer(address)) {
            lock();
        }
    }
}
