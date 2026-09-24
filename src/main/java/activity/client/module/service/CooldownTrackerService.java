package activity.client.module.service;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownTrackerService {

    public static final class CooldownEntry {
        public final Item item;
        public final ItemStack iconStack;
        public final int totalTicks;
        public int remainingTicks;
        public final long startTimestampMs;

        public CooldownEntry(Item item, int totalTicks) {
            this.item = item;
            ItemStack stack = null;
            try {
                if (item != null) {
                    stack = new ItemStack(item);
                }
            } catch (Throwable ignored) {
            }
            this.iconStack = stack;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
            this.startTimestampMs = System.currentTimeMillis();
        }

        public float getRemainingSeconds() {
            return Math.max(0.0f, remainingTicks / 20.0f);
        }

        public String getFormattedRemaining() {
            float sec = getRemainingSeconds();
            if (sec <= 0.0f) {
                return "0.0s";
            }
            if (sec > 5.0f) {
                return ((int) Math.ceil(sec)) + "s";
            } else {
                return String.format(Locale.ROOT, "%.1fs", sec);
            }
        }
    }

    private static final Map<Object, CooldownEntry> ACTIVE_COOLDOWNS = new ConcurrentHashMap<>();
    private static volatile long lastTridentUseTimeMs = 0L;

    private CooldownTrackerService() {}

    public static void recordTridentUsed() {
        lastTridentUseTimeMs = System.currentTimeMillis();
    }

    public static boolean isTridentRecentlyUsed() {
        return (System.currentTimeMillis() - lastTridentUseTimeMs) < 3000L;
    }

    public static void resetTridentUsageForTest() {
        lastTridentUseTimeMs = 0L;
    }

    public static boolean isTridentItem(Item item) {
        if (item == null) return false;
        try {
            String tk = item.getTranslationKey();
            if (tk != null && tk.contains("trident")) return true;
        } catch (Throwable ignored) {}
        try {
            String s = item.toString();
            if (s != null && s.toLowerCase(Locale.ROOT).contains("trident")) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isAirItem(Item item) {
        if (item == null) return true;
        try {
            String tk = item.getTranslationKey();
            if (tk != null && (tk.endsWith(".air") || tk.equals("air"))) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean hasTridentInInventory(net.minecraft.client.network.ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && isTridentItem(s.getItem())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasItemInInventory(net.minecraft.client.network.ClientPlayerEntity player, Item item) {
        if (player == null || player.getInventory() == null || item == null) return false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && (s.isOf(item) || (isTridentItem(item) && isTridentItem(s.getItem())))) {
                return true;
            }
        }
        return false;
    }

    public static void onCooldownSet(Item item, int durationTicks) {
        if (isAirItem(item) || durationTicks <= 0) {
            return;
        }
        if (isTridentItem(item)) {
            if (!isTridentRecentlyUsed()) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null && !hasTridentInInventory(mc.player)) {
                return;
            }
        }
        registerCooldown(item, item, durationTicks);
    }

    public static void onCooldownSetForTest(String testKey, int durationTicks) {
        if (testKey == null || durationTicks <= 0) {
            return;
        }
        registerCooldown(testKey, null, durationTicks);
    }

    private static void registerCooldown(Object key, Item item, int durationTicks) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        double minSec = (config != null) ? config.cooldownHudMinDuration : 2.5;
        int minTicks = (int) Math.round(minSec * 20.0);

        if (durationTicks < minTicks) {
            return;
        }

        ACTIVE_COOLDOWNS.put(key, new CooldownEntry(item, durationTicks));
    }

    public static void onCooldownRemoved(Item item) {
        if (item != null) {
            ACTIVE_COOLDOWNS.remove(item);
        }
    }

    public static void tick(MinecraftClient client) {
        if (ACTIVE_COOLDOWNS.isEmpty()) {
            return;
        }

        boolean hasPlayer = client != null && client.player != null && client.player.getItemCooldownManager() != null;

        for (Map.Entry<Object, CooldownEntry> mapEntry : ACTIVE_COOLDOWNS.entrySet()) {
            CooldownEntry entry = mapEntry.getValue();
            entry.remainingTicks--;

            if (entry.remainingTicks <= 0) {
                ACTIVE_COOLDOWNS.remove(mapEntry.getKey());
                continue;
            }

            if (isTridentItem(entry.item)) {
                if (hasPlayer && !hasTridentInInventory(client.player)) {
                    ACTIVE_COOLDOWNS.remove(mapEntry.getKey());
                    continue;
                }
            }

            if (hasPlayer && entry.iconStack != null && System.currentTimeMillis() - entry.startTimestampMs > 250L && !client.player.getItemCooldownManager().isCoolingDown(entry.iconStack)) {
                ACTIVE_COOLDOWNS.remove(mapEntry.getKey());
            }
        }
    }

    public static List<CooldownEntry> getActiveEntries() {
        if (ACTIVE_COOLDOWNS.isEmpty()) {
            return List.of();
        }
        List<CooldownEntry> list = new ArrayList<>(ACTIVE_COOLDOWNS.values());
        list.sort(Comparator.comparingInt(a -> a.remainingTicks));
        return list;
    }

    public static void clear() {
        ACTIVE_COOLDOWNS.clear();
        lastTridentUseTimeMs = 0L;
    }
}
