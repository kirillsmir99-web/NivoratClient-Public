package activity.client.module.service;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CartStateService {

    public interface CartEventListener {
        default void onCartPlaced(BlockPos pos) {}
        default void onCartRefilled(int hotbarSlot) {}
    }

    private static final List<CartEventListener> LISTENERS = new CopyOnWriteArrayList<>();

    private static int cachedCartCount = 0;
    private static long lastCountWorldTime = -1L;

    private CartStateService() {}

    public static void addListener(CartEventListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(CartEventListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static void notifyCartPlaced(BlockPos pos) {
        invalidate();
        for (CartEventListener listener : LISTENERS) {
            try {
                listener.onCartPlaced(pos);
            } catch (Throwable ignored) {}
        }
    }

    public static void notifyCartRefilled(int hotbarSlot) {
        invalidate();
        for (CartEventListener listener : LISTENERS) {
            try {
                listener.onCartRefilled(hotbarSlot);
            } catch (Throwable ignored) {}
        }
    }

    public static int countCarts(ClientPlayerEntity player) {
        if (player == null) return 0;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            long worldTime = mc.world.getTime();
            if (worldTime != lastCountWorldTime) {
                lastCountWorldTime = worldTime;
                cachedCartCount = countCartsUncached(player);
            }
            return cachedCartCount;
        }
        return countCartsUncached(player);
    }

    public static int countCartsUncached(ClientPlayerEntity player) {
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TNT_MINECART)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int findInventoryCart(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TNT_MINECART)) {
                return i;
            }
        }
        return -1;
    }

    public static int findHotbarCart(ClientPlayerEntity player) {
        if (player == null) return -1;
        boolean hasCooldownManager = player.getItemCooldownManager() != null;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.TNT_MINECART)) {
                if (hasCooldownManager && player.getItemCooldownManager().isCoolingDown(stack)) {
                    continue;
                }
                return slot;
            }
        }
        return -1;
    }

    public static boolean isCartOnCooldown(ClientPlayerEntity player) {
        if (player == null || player.getItemCooldownManager() == null) {
            return false;
        }
        return player.getItemCooldownManager().isCoolingDown(Items.TNT_MINECART.getDefaultStack());
    }

    public static boolean isCartInOffhand(ClientPlayerEntity player) {
        if (player == null) return false;
        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isOf(Items.TNT_MINECART)) {
            return false;
        }
        if (player.getItemCooldownManager() != null && player.getItemCooldownManager().isCoolingDown(offhand)) {
            return false;
        }
        return true;
    }

    public static int findRailSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof AbstractRailBlock) {
                return slot;
            }
        }
        return -1;
    }

    public static void reset() {
        cachedCartCount = 0;
        lastCountWorldTime = -1L;
    }

    public static void invalidate() {
        reset();
    }
}
