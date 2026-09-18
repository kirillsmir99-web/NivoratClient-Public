package activity.client.module.service;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.Locale;

/**
 * Shared per-tick inventory and hotbar scanning service.
 *
 * <p>Avoids redundant traversals of player inventory (0..8 hotbar or 0..45 total)
 * performed by combat and defense modules within the same client tick.
 */
public final class InventoryScanService {

    private static long lastTickId = -1L;

    private static int cachedSwordSlot = -2;
    private static int cachedAxeSlot = -2;
    private static int cachedMaceSlot = -2;
    private static int cachedSpearSlot = -2;
    private static int cachedTotemSlot = -2;
    private static int cachedHotbarTotemSlot = -2;
    private static int cachedGlowstoneSlot = -2;

    private InventoryScanService() {}

    public static void onTick(MinecraftClient client, long tickId) {
        if (tickId != lastTickId) {
            lastTickId = tickId;
            invalidate();
        }
    }

    public static void invalidate() {
        cachedSwordSlot = -2;
        cachedAxeSlot = -2;
        cachedMaceSlot = -2;
        cachedSpearSlot = -2;
        cachedTotemSlot = -2;
        cachedHotbarTotemSlot = -2;
        cachedGlowstoneSlot = -2;
    }

    public static int getCachedSwordSlot() { return cachedSwordSlot; }
    public static int getCachedAxeSlot() { return cachedAxeSlot; }
    public static int getCachedMaceSlot() { return cachedMaceSlot; }
    public static int getCachedSpearSlot() { return cachedSpearSlot; }
    public static int getCachedTotemSlot() { return cachedTotemSlot; }
    public static int getCachedHotbarTotemSlot() { return cachedHotbarTotemSlot; }
    public static int getCachedGlowstoneSlot() { return cachedGlowstoneSlot; }
    public static void setCachedSwordSlotForTest(int slot) { cachedSwordSlot = slot; }
    public static void setCachedAxeSlotForTest(int slot) { cachedAxeSlot = slot; }
    public static void setCachedTotemSlotForTest(int slot) { cachedTotemSlot = slot; }
    public static void setCachedHotbarTotemSlotForTest(int slot) { cachedHotbarTotemSlot = slot; }

    public static int findSwordSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedSwordSlot != -2) return cachedSwordSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                cachedSwordSlot = i;
                return i;
            }
        }
        cachedSwordSlot = -1;
        return -1;
    }

    public static int findAxeSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedAxeSlot != -2) return cachedAxeSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && (stack.isIn(ItemTags.AXES) || isAxeItem(stack))) {
                if (!stack.isDamageable() || stack.getMaxDamage() - stack.getDamage() >= 4) {
                    cachedAxeSlot = i;
                    return i;
                }
            }
        }
        cachedAxeSlot = -1;
        return -1;
    }

    public static int findMaceSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedMaceSlot != -2) return cachedMaceSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && (stack.isOf(Items.MACE) || isMaceItem(stack))) {
                cachedMaceSlot = i;
                return i;
            }
        }
        cachedMaceSlot = -1;
        return -1;
    }

    public static int findSpearSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedSpearSlot != -2) return cachedSpearSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && isSpearItem(stack)) {
                cachedSpearSlot = i;
                return i;
            }
        }
        cachedSpearSlot = -1;
        return -1;
    }

    public static int findHotbarTotemSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedHotbarTotemSlot != -2) return cachedHotbarTotemSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
                cachedHotbarTotemSlot = i;
                if (cachedTotemSlot == -2) {
                    cachedTotemSlot = i;
                }
                return i;
            }
        }
        cachedHotbarTotemSlot = -1;
        return -1;
    }

    public static int findTotemSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedTotemSlot != -2) return cachedTotemSlot;
        if (cachedHotbarTotemSlot >= 0) {
            cachedTotemSlot = cachedHotbarTotemSlot;
            return cachedTotemSlot;
        }

        int start = (cachedHotbarTotemSlot == -1) ? 9 : 0;
        for (int i = start; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
                cachedTotemSlot = i;
                if (i < 9 && cachedHotbarTotemSlot == -2) {
                    cachedHotbarTotemSlot = i;
                }
                return i;
            }
        }
        cachedTotemSlot = -1;
        if (cachedHotbarTotemSlot == -2) {
            cachedHotbarTotemSlot = -1;
        }
        return -1;
    }

    public static int findGlowstoneSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return -1;
        if (cachedGlowstoneSlot != -2) return cachedGlowstoneSlot;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.GLOWSTONE)) {
                cachedGlowstoneSlot = i;
                return i;
            }
        }
        cachedGlowstoneSlot = -1;
        return -1;
    }

    private static boolean isAxeItem(ItemStack stack) {
        String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return name.contains("axe") || name.contains("топор");
    }

    private static boolean isMaceItem(ItemStack stack) {
        String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return name.contains("mace") || name.contains("булава");
    }

    private static boolean isSpearItem(ItemStack stack) {
        if (stack.isOf(Items.TRIDENT)) return true;
        try {
            if (stack.contains(DataComponentTypes.KINETIC_WEAPON)) return true;
        } catch (Throwable ignored) {}
        String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return name.contains("spear") || name.contains("trident") || name.contains("копь");
    }
}
