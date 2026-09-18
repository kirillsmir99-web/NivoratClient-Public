package dev.storage;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class CartRefillController {
    private static final long COOLDOWN_MS = 300L;

    private enum State {
        IDLE,
        WAITING_DELAY,
        LEGIT_OPENED,
        LEGIT_SWAPPED,
        LEGIT_CLOSING
    }

    private State state = State.IDLE;
    private final boolean[] hadCartInHotbar = new boolean[9];
    private int targetSlot = -1;
    private int delayTimer = 0;
    private int legitTimer = 0;
    private long lastRefillTime = 0L;
    private Screen activeRefillScreen = null;

    public CartRefillController() {
    }

    public boolean isEnabled() {
        return RefillConfig.enabled;
    }

    public void toggle() {
        RefillConfig.enabled = !RefillConfig.enabled;
        RefillConfig.save();
        if (!RefillConfig.enabled) {
            reset();
        }
    }

    public void reset() {
        state = State.IDLE;
        targetSlot = -1;
        delayTimer = 0;
        legitTimer = 0;
        activeRefillScreen = null;
        for (int i = 0; i < 9; i++) {
            hadCartInHotbar[i] = false;
        }
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            reset();
            return;
        }

        if (!RefillConfig.enabled || !client.player.isAlive()) {
            reset();
            return;
        }

        if (client.player.currentScreenHandler != client.player.playerScreenHandler) {
            updateSnapshot(client.player);
            return;
        }

        if (client.currentScreen != null && client.currentScreen != activeRefillScreen) {
            updateSnapshot(client.player);
            return;
        }

        switch (state) {
            case IDLE -> detectSpentCart(client.player);
            case WAITING_DELAY -> processWaitingDelay(client);
            case LEGIT_OPENED -> processLegitOpened(client);
            case LEGIT_SWAPPED -> processLegitSwapped(client);
            case LEGIT_CLOSING -> processLegitClosing(client);
        }
    }

    private void updateSnapshot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            hadCartInHotbar[i] = player.getInventory().getStack(i).isOf(Items.TNT_MINECART);
        }
        state = State.IDLE;
        targetSlot = -1;
        delayTimer = 0;
        legitTimer = 0;
        activeRefillScreen = null;
    }

    private void detectSpentCart(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            boolean hasCart = stack.isOf(Items.TNT_MINECART);
            if (hadCartInHotbar[i] && !hasCart) {
                hadCartInHotbar[i] = false;
                scheduleRefill(i, player);
                return;
            }
            hadCartInHotbar[i] = hasCart;
        }
    }

    private void scheduleRefill(int hotbarSlot, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        if (RefillConfig.chance < 100) {
            int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
            if (roll >= RefillConfig.chance) {
                lastRefillTime = now + 200L;
                return;
            }
        }
        if (now - lastRefillTime < COOLDOWN_MS) {
            return;
        }
        int invCartSlot = findInventoryCart(player);
        if (invCartSlot < 0) {
            return;
        }
        targetSlot = hotbarSlot;
        int baseDelay = Math.max(0, RefillConfig.refillDelayTicks);
        if (RefillConfig.randomDelay && baseDelay > 0) {
            long ms = GaussianTimingEngine.getDelay(baseDelay * 50.0, 25.0, 20L, 500L);
            delayTimer = Math.max(1, (int) Math.round(ms / 50.0));
        } else {
            delayTimer = baseDelay;
        }
        state = State.WAITING_DELAY;
    }

    private void processWaitingDelay(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            reset();
            return;
        }

        if (player.isUsingItem()) {
            return;
        }

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        if (targetSlot < 0 || targetSlot >= 9) {
            reset();
            return;
        }

        ItemStack currentTargetStack = player.getInventory().getStack(targetSlot);
        if (currentTargetStack.isOf(Items.TNT_MINECART)) {
            hadCartInHotbar[targetSlot] = true;
            reset();
            return;
        }

        int invCartSlot = findInventoryCart(player);
        if (invCartSlot < 0) {
            reset();
            return;
        }

        if (!RefillConfig.legitMode) {
            client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                invCartSlot,
                targetSlot,
                SlotActionType.SWAP,
                player
            );
            hadCartInHotbar[targetSlot] = true;
            lastRefillTime = System.currentTimeMillis();
            activity.client.module.service.CartStateService.notifyCartRefilled(targetSlot);
            reset();
            return;
        }

        activeRefillScreen = new InventoryScreen(player);
        client.setScreen(activeRefillScreen);
        legitTimer = 2;
        state = State.LEGIT_OPENED;
    }

    private void processLegitOpened(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            reset();
            return;
        }

        if (client.currentScreen != activeRefillScreen) {
            reset();
            return;
        }

        if (legitTimer > 0) {
            legitTimer--;
            return;
        }

        int invCartSlot = findInventoryCart(player);
        if (invCartSlot >= 0 && targetSlot >= 0 && targetSlot < 9) {
            client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                invCartSlot,
                targetSlot,
                SlotActionType.SWAP,
                player
            );
            hadCartInHotbar[targetSlot] = true;
            lastRefillTime = System.currentTimeMillis();
            activity.client.module.service.CartStateService.notifyCartRefilled(targetSlot);
        }

        legitTimer = 2;
        state = State.LEGIT_SWAPPED;
    }

    private void processLegitSwapped(MinecraftClient client) {
        if (legitTimer > 0) {
            legitTimer--;
            return;
        }

        if (RefillConfig.autoClose && client.currentScreen == activeRefillScreen && client.player != null) {
            legitTimer = 1;
            state = State.LEGIT_CLOSING;
        } else {
            reset();
        }
    }

    private void processLegitClosing(MinecraftClient client) {
        if (legitTimer > 0) {
            legitTimer--;
            return;
        }

        if (client.currentScreen == activeRefillScreen && client.player != null) {
            client.player.closeHandledScreen();
            client.setScreen(null);
        }
        reset();
    }

    private int findInventoryCart(ClientPlayerEntity player) {
        return activity.client.module.service.CartStateService.findInventoryCart(player);
    }
}
