package net.fabricmc.pack.api;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

public final class CombatLockManager {
    public static final String SHIELD_COMBO   = "pvp.shield_combo_active";
    public static final String SUNDER         = "pvp.sunder_active";
    public static final String CART_PLACEMENT = "pvp.cart_placement_active";
    public static final String ANCHOR         = "pvp.anchor_active";
    public static final String MACE           = "pvp.mace_active";
    public static final String SPEAR          = "pvp.spear_active";
    public static final String TOTEM          = "pvp.totem_active";
    public static final String PEARL_CATCH    = "pvp.pearl_catch_active";
    public static final String INVENTORY_ACTION = "pvp.inventory_action_active";

    private static final int MASK_SHIELD_COMBO   = 1 << 0;
    private static final int MASK_SUNDER         = 1 << 1;
    private static final int MASK_CART_PLACEMENT = 1 << 2;
    private static final int MASK_ANCHOR         = 1 << 3;
    private static final int MASK_MACE           = 1 << 4;
    private static final int MASK_SPEAR          = 1 << 5;
    private static final int MASK_TOTEM          = 1 << 6;
    private static final int MASK_PEARL_CATCH    = 1 << 7;
    private static final int MASK_INVENTORY_ACTION = 1 << 8;

    private static final AtomicInteger LOCK_MASK = new AtomicInteger(0);

    private CombatLockManager() {}

    @Deprecated
    public static int getLockMask() {
        return LOCK_MASK.get();
    }

    @Deprecated
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
                || Boolean.getBoolean(TOTEM)
                || Boolean.getBoolean(PEARL_CATCH);
    }

    @Deprecated
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

    @Deprecated
    public static boolean hasConflictExcludingMace() {
        if ((LOCK_MASK.get() & ~(MASK_MACE | MASK_SHIELD_COMBO)) != 0) {
            return true;
        }
        return Boolean.getBoolean(SUNDER)
                || Boolean.getBoolean(CART_PLACEMENT)
                || Boolean.getBoolean(ANCHOR)
                || Boolean.getBoolean(SPEAR)
                || Boolean.getBoolean(TOTEM)
                || Boolean.getBoolean(PEARL_CATCH);
    }

    @Deprecated
    public static void setLock(String key, boolean active) {
        if (key == null) return;
        int bit = getBitForKey(key);
        if (active) {
            System.setProperty(key, "true");
            if (bit != 0) LOCK_MASK.updateAndGet(m -> m | bit);
            syncArbiterOnSet(key);
        } else {
            System.clearProperty(key);
            if (bit != 0) LOCK_MASK.updateAndGet(m -> m & ~bit);
            syncArbiterOnClear(key);
        }
    }

    private static void syncArbiterOnSet(String key) {
        SlotArbiter.Priority priority = SlotArbiter.Priority.COMBAT_NORMAL;
        EnumSet<SlotArbiter.Resource> res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT);
        boolean atomic = false;

        switch (key) {
            case TOTEM -> {
                priority = SlotArbiter.Priority.EMERGENCY;
                res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT, SlotArbiter.Resource.OFFHAND);
            }
            case INVENTORY_ACTION -> {
                priority = SlotArbiter.Priority.UTILITY;
                res = EnumSet.of(SlotArbiter.Resource.INVENTORY_CLICKS);
            }
            case SHIELD_COMBO -> {
                priority = SlotArbiter.Priority.COMBAT_HIGH;
                res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT);
            }
            case MACE, SUNDER -> {
                priority = SlotArbiter.Priority.COMBAT_HIGH;
                res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT);
                atomic = true;
            }
            case ANCHOR -> {
                priority = SlotArbiter.Priority.COMBAT_NORMAL;
                res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT, SlotArbiter.Resource.USE_ITEM);
            }
            case PEARL_CATCH, CART_PLACEMENT, SPEAR -> {
                priority = SlotArbiter.Priority.UTILITY;
                res = EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT);
            }
        }
        SlotArbiter.acquire(key, priority, res, 40, atomic);
    }

    private static void syncArbiterOnClear(String key) {
        SlotArbiter.Lease lease = SlotArbiter.getActiveLease();
        if (lease != null && key.equals(lease.getOwner())) {
            lease.close();
        }
    }

    @Deprecated
    public static void reset() {
        LOCK_MASK.set(0);
        System.clearProperty("pvp.shield_combo_active");
        System.clearProperty("pvp.sunder_active");
        System.clearProperty("pvp.cart_placement_active");
        System.clearProperty("pvp.anchor_active");
        System.clearProperty("pvp.mace_active");
        System.clearProperty("pvp.spear_active");
        System.clearProperty("pvp.totem_active");
        System.clearProperty("pvp.pearl_catch_active");
        System.clearProperty("pvp.inventory_action_active");
        SlotArbiter.reset();
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
            case "pvp.pearl_catch_active" -> MASK_PEARL_CATCH;
            case "pvp.inventory_action_active" -> MASK_INVENTORY_ACTION;
            default -> 0;
        };
    }
}
