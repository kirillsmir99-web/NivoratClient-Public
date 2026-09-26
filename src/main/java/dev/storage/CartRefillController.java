package dev.storage;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class CartRefillController {
    private static final long COOLDOWN_MS = 250L;

    public enum State {
        IDLE,
        WAITING_OPEN,
        WAITING_SWAP,
        WAITING_CLOSE
    }

    private State state = State.IDLE;
    private final boolean[] hadCartInHotbar = new boolean[9];
    private int targetSlot = -1;
    private int invCartSlot = -1;
    private int timer = 0;
    private boolean openedByRefill = false;
    private long lastRefillTime = 0L;

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
        invCartSlot = -1;
        timer = 0;
        openedByRefill = false;
        net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.INVENTORY_ACTION, false);
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

        if (client.currentScreen != null && (!openedByRefill || !(client.currentScreen instanceof InventoryScreen))) {
            updateSnapshot(client.player);
            return;
        }

        if (client.player.isUsingItem()) {
            return;
        }

        switch (state) {
            case IDLE -> detectSpentCart(client);
            case WAITING_OPEN -> processWaitingOpen(client);
            case WAITING_SWAP -> processWaitingSwap(client);
            case WAITING_CLOSE -> processWaitingClose(client);
        }
    }

    private void updateSnapshot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            hadCartInHotbar[i] = player.getInventory().getStack(i).isOf(Items.TNT_MINECART);
        }
        state = State.IDLE;
        targetSlot = -1;
        invCartSlot = -1;
        timer = 0;
        openedByRefill = false;
        net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.INVENTORY_ACTION, false);
    }

    private void detectSpentCart(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (client.currentScreen != null) return;

        if (!player.playerScreenHandler.getCursorStack().isEmpty()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            boolean hasCart = stack.isOf(Items.TNT_MINECART);
            if (hadCartInHotbar[i] && !hasCart) {
                hadCartInHotbar[i] = false;

                if (stack.isEmpty()) {
                    scheduleRefill(i, client);
                    return;
                }
            }
            hadCartInHotbar[i] = hasCart;
        }
    }

    private void scheduleRefill(int hotbarSlot, MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (client.currentScreen != null) return;

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

        if (net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.TOTEM)
                || net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.INVENTORY_ACTION)) {
            return;
        }

        int foundInvSlot = findInventoryCart(player);
        if (foundInvSlot < 0) {
            return;
        }

        targetSlot = hotbarSlot;
        invCartSlot = foundInvSlot;
        openedByRefill = false;
        timer = sampleOpenDelayTicks();
        state = State.WAITING_OPEN;
    }

    private void processWaitingOpen(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || !player.isAlive()) {
            reset();
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.TOTEM)) {
            reset();
            return;
        }

        if (client.currentScreen != null) {
            reset();
            return;
        }

        if (targetSlot < 0 || targetSlot >= 9 || !player.getInventory().getStack(targetSlot).isEmpty()) {
            reset();
            return;
        }

        net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.INVENTORY_ACTION, true);

        client.setScreen(new InventoryScreen(player));
        openedByRefill = true;

        timer = sampleSwapDelayTicks();
        state = State.WAITING_SWAP;
    }

    private void processWaitingSwap(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null || !player.isAlive()) {
            reset();
            return;
        }

        if (!openedByRefill || !(client.currentScreen instanceof InventoryScreen)) {
            reset();
            return;
        }

        if (targetSlot < 0 || targetSlot >= 9) {
            finishRefill(client);
            return;
        }

        ItemStack currentTargetStack = player.getInventory().getStack(targetSlot);
        if (currentTargetStack.isOf(Items.TNT_MINECART)) {
            hadCartInHotbar[targetSlot] = true;
            finishRefill(client);
            return;
        }

        if (!currentTargetStack.isEmpty()) {
            finishRefill(client);
            return;
        }

        if (!player.playerScreenHandler.getCursorStack().isEmpty()) {
            finishRefill(client);
            return;
        }

        int currentInvSlot = findInventoryCart(player);
        if (currentInvSlot < 0) {
            finishRefill(client);
            return;
        }

        if (player.isSprinting()) {
            player.setSprinting(false);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                    player,
                    net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING
                ));
            }
        }

        client.interactionManager.clickSlot(
            player.playerScreenHandler.syncId,
            currentInvSlot,
            targetSlot,
            SlotActionType.SWAP,
            player
        );
        hadCartInHotbar[targetSlot] = true;
        lastRefillTime = System.currentTimeMillis();
        activity.client.module.service.CartStateService.notifyCartRefilled(targetSlot);

        timer = sampleCloseDelayTicks();
        state = State.WAITING_CLOSE;
    }

    private void processWaitingClose(MinecraftClient client) {
        if (timer > 0) {
            timer--;
            return;
        }

        finishRefill(client);
    }

    void finishRefill(MinecraftClient client) {
        if (openedByRefill && RefillConfig.autoClose && client != null && client.currentScreen instanceof InventoryScreen) {
            if (!net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.TOTEM)) {
                if (client.player != null) {
                    client.player.closeHandledScreen();
                }
                client.setScreen(null);
            }
        }

        reset();
    }

    public boolean shouldCloseScreen(net.minecraft.client.gui.screen.Screen screen, boolean openedByRefill) {
        return openedByRefill && RefillConfig.autoClose && (screen instanceof InventoryScreen);
    }

    public boolean shouldCloseScreen(net.minecraft.client.gui.screen.Screen screen) {
        return shouldCloseScreen(screen, this.openedByRefill);
    }

    public int sampleOpenDelayTicks() {
        if (!RefillConfig.randomDelay) {
            return Math.max(1, Math.min(10, RefillConfig.refillDelayTicks));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillOpenDelayTicks();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = baseTicks * 50.0;
        double stdDevMs = spreadTicks * 22.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 35.0);
        long sampleMs = GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
        return Math.max(1, (int) Math.round(sampleMs / 50.0D));
    }

    public long sampleOpenDelayMs() {
        if (!RefillConfig.randomDelay) {
            return Math.max(GaussianTimingEngine.FAST_LEGIT_MIN_MS,
                Math.min(GaussianTimingEngine.FAST_LEGIT_MAX_MS, RefillConfig.refillDelayTicks * 50L));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillOpenDelayMs();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = baseTicks * 50.0;
        double stdDevMs = spreadTicks * 22.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 35.0);
        return GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
    }

    public int sampleSwapDelayTicks() {
        if (!RefillConfig.randomDelay) {
            return Math.max(1, Math.min(10, RefillConfig.refillDelayTicks));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillSwapDelayTicks();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = (baseTicks + 0.2) * 50.0;
        double stdDevMs = spreadTicks * 25.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 40.0);
        long sampleMs = GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
        return Math.max(1, (int) Math.round(sampleMs / 50.0D));
    }

    public long sampleSwapDelayMs() {
        if (!RefillConfig.randomDelay) {
            return Math.max(GaussianTimingEngine.FAST_LEGIT_MIN_MS,
                Math.min(GaussianTimingEngine.FAST_LEGIT_MAX_MS, (RefillConfig.refillDelayTicks + 1) * 50L));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillSwapDelayMs();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = (baseTicks + 0.2) * 50.0;
        double stdDevMs = spreadTicks * 25.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 40.0);
        return GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
    }

    public int sampleCloseDelayTicks() {
        if (!RefillConfig.randomDelay) {
            return Math.max(1, Math.min(10, RefillConfig.refillDelayTicks));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillCloseDelayTicks();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = (baseTicks - 0.2) * 50.0;
        double stdDevMs = spreadTicks * 20.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 30.0);
        long sampleMs = GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
        return Math.max(1, (int) Math.round(sampleMs / 50.0D));
    }

    public long sampleCloseDelayMs() {
        if (!RefillConfig.randomDelay) {
            return Math.max(GaussianTimingEngine.FAST_LEGIT_MIN_MS,
                Math.min(GaussianTimingEngine.FAST_LEGIT_MAX_MS, RefillConfig.refillDelayTicks * 50L));
        }
        if (RefillConfig.refillDelayTicks == 2 && Math.abs(RefillConfig.randomSpreadTicks - 1.0) < 0.01) {
            return GaussianTimingEngine.getFastLegitRefillCloseDelayMs();
        }
        double baseTicks = Math.max(1.0, RefillConfig.refillDelayTicks);
        double spreadTicks = Math.max(0.0, RefillConfig.randomSpreadTicks);
        double meanMs = (baseTicks - 0.2) * 50.0;
        double stdDevMs = spreadTicks * 20.0;
        long minMs = Math.max(40L, Math.round((baseTicks - spreadTicks) * 50.0));
        long maxMs = Math.round((baseTicks + spreadTicks) * 50.0 + 30.0);
        return GaussianTimingEngine.getDelay(meanMs, stdDevMs, minMs, maxMs);
    }

    public State getState() {
        return state;
    }

    public int getTimer() {
        return timer;
    }

    public boolean isOpenedByRefill() {
        return openedByRefill;
    }

    public int getTargetSlot() {
        return targetSlot;
    }

    public void setStateForTest(State state, int timer, boolean openedByRefill) {
        this.state = state;
        this.timer = timer;
        this.openedByRefill = openedByRefill;
    }

    private int findInventoryCart(ClientPlayerEntity player) {
        return activity.client.module.service.CartStateService.findInventoryCart(player);
    }

    private int findEmptyHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
