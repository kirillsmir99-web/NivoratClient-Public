package net.fabricmc.pack.api;

import java.util.concurrent.atomic.AtomicInteger;

public final class CombatLockManager {
    public static final String SHIELD_COMBO   = "pvp.shield_combo_active";
    public static final String SUNDER         = "pvp.sunder_active";
    public static final String CART_PLACEMENT = "pvp.cart_placement_active";
    public static final String ANCHOR         = "pvp.anchor_active";
    public static final String MACE           = "pvp.mace_active";
    public static final String SPEAR          = "pvp.spear_active";
    public static final String TOTEM          = "pvp.totem_active";

    private static final int MASK_SHIELD_COMBO   = 1 << 0;
    private static final int MASK_SUNDER         = 1 << 1;
    private static final int MASK_CART_PLACEMENT = 1 << 2;
    private static final int MASK_ANCHOR         = 1 << 3;
    private static final int MASK_MACE           = 1 << 4;
    private static final int MASK_SPEAR          = 1 << 5;
    private static final int MASK_TOTEM          = 1 << 6;

    private static final AtomicInteger LOCK_MASK = new AtomicInteger(0);

    private CombatLockManager() {}

    public static int getLockMask() {
        return LOCK_MASK.get();
    }

    public static boolean isLocked() {
        if (LOCK_MASK.get() != 0) {
            return true;
        }
        return Boolean.getBoolean(SHIELD_COMBO)
                || Boolean.getBoolean(SUNDER)
                || Boolean.getBoolean(CART_PLACEMENT)
                || Boolean.getBoolean(ANCHOR)
                || Boolean.getBoolean(MACE)
                || Boolean.getBoolean(SPEAR)
                || Boolean.getBoolean(TOTEM);
    }

    public static boolean isLocked(String key) {
        if (key == null) return false;
        int bit = getBitForKey(key);
        if (bit != 0) {
            if ((LOCK_MASK.get() & bit) != 0) {
                return true;
            }
        }
        return Boolean.getBoolean(key);
    }

    public static boolean hasConflictExcludingMace() {
        if ((LOCK_MASK.get() & ~(MASK_MACE | MASK_SHIELD_COMBO)) != 0) {
            return true;
        }
        return Boolean.getBoolean(SUNDER)
                || Boolean.getBoolean(CART_PLACEMENT)
                || Boolean.getBoolean(ANCHOR)
                || Boolean.getBoolean(SPEAR)
                || Boolean.getBoolean(TOTEM);
    }

    public static void setLock(String key, boolean active) {
        if (key == null) return;
        int bit = getBitForKey(key);
        if (active) {
            System.setProperty(key, "true");
            if (bit != 0) LOCK_MASK.updateAndGet(m -> m | bit);
        } else {
            System.clearProperty(key);
            if (bit != 0) LOCK_MASK.updateAndGet(m -> m & ~bit);
        }
    }

    public static void reset() {
        LOCK_MASK.set(0);
        System.clearProperty("pvp.shield_combo_active");
        System.clearProperty("pvp.sunder_active");
        System.clearProperty("pvp.cart_placement_active");
        System.clearProperty("pvp.anchor_active");
        System.clearProperty("pvp.mace_active");
        System.clearProperty("pvp.spear_active");
        System.clearProperty("pvp.totem_active");
    }

    private static int getBitForKey(String key) {
        return switch (key) {
            case "pvp.shield_combo_active" -> MASK_SHIELD_COMBO;
            case "pvp.sunder_active" -> MASK_SUNDER;
            case "pvp.cart_placement_active" -> MASK_CART_PLACEMENT;
            case "pvp.anchor_active" -> MASK_ANCHOR;
            case "pvp.mace_active" -> MASK_MACE;
            case "pvp.spear_active" -> MASK_SPEAR;
            case "pvp.totem_active" -> MASK_TOTEM;
            default -> 0;
        };
    }
}
