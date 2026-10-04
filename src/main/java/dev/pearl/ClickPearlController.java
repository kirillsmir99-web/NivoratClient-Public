package dev.pearl;

import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class ClickPearlController {
    public enum State { IDLE, OPENING_INVENTORY, CLOSING_INVENTORY, THROWING, WAITING_RETURN, OPENING_RETURN, CLOSING_RETURN }
    private static ClickPearlController INSTANCE;
    private State state = State.IDLE;

    public ClickPearlController() {
        INSTANCE = this;
    }

    public static ClickPearlController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickPearlController();
        }
        return INSTANCE;
    }
    private ClientPlayerEntity owner;
    private World world;
    private int originalSlot = -1;
    private int selectedPearlSlot = -1;
    private int sourceInvSlot = -1;
    private boolean inventorySwapped;
    private boolean returnPearl;
    private boolean restoreSelected;
    private long deadlineMs;
    private long actionTimeMs;
    private long lastTriggerTimeMs;

    public boolean ownsInventoryScreen(MinecraftClient client) {
        return state != State.IDLE && client != null && client.currentScreen instanceof InventoryScreen;
    }

    public void trigger(MinecraftClient client) {
        if (!usable(client) || !ClickPearlConfig.enabled || state != State.IDLE
                || CombatLockManager.isLocked()) return;
        ClientPlayerEntity player = client.player;
        if (ClickPearlConfig.combatGuard && player.isUsingItem()) return;
        long now = System.currentTimeMillis();
        if (now - lastTriggerTimeMs < 150L) return;
        if (ClickPearlConfig.checkCooldown && player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) return;
        if (findHotbarItem(player, Items.ENDER_PEARL) < 0 && ClickPearlConfig.preferOffhand && isOffhandItem(player, Items.ENDER_PEARL)) {
            lastTriggerTimeMs = now;
            usePearl(client, Hand.OFF_HAND);
            return;
        }
        int hotbar = findHotbarItem(player, Items.ENDER_PEARL);
        int inventory = hotbar < 0 && "inventory".equalsIgnoreCase(ClickPearlConfig.searchMode)
                ? findInventoryItem(player, Items.ENDER_PEARL) : -1;
        if (hotbar < 0 && inventory < 0) {
            if (isOffhandItem(player, Items.ENDER_PEARL)) {
                lastTriggerTimeMs = now;
                usePearl(client, Hand.OFF_HAND);
            } else {
                activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "trigger", false, "pearl_not_found");
            }
            return;
        }
        owner = player;
        world = client.world;
        originalSlot = player.getInventory().getSelectedSlot();
        returnPearl = ClickPearlConfig.returnPearl;
        restoreSelected = ClickPearlConfig.switchBack;
        deadlineMs = now + 4000L;
        lastTriggerTimeMs = now;
        dev.impact.SurfaceImpactController.recordPearlThrown();
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, true);
        try {
            if (inventory >= 9) {
                sourceInvSlot = inventory;
                int targetHotbar = ClickPearlConfig.getTargetHotbarIndex();
                selectedPearlSlot = targetHotbar;
                inventorySwapped = true;
                client.setScreen(new InventoryScreen(player));
                state = State.OPENING_INVENTORY;
                actionTimeMs = now + (ClickPearlConfig.randomDelay ? 35L : 15L);
                return;
            }
            selectedPearlSlot = hotbar;
            SafeSlotManager.selectSlot(client, hotbar);
            if (player.getInventory().getSelectedSlot() != hotbar || !player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "select_slot", false, "expected=" + hotbar + " actual=" + player.getInventory().getSelectedSlot());
                reset();
                return;
            }
            if ("fast".equalsIgnoreCase(ClickPearlConfig.mode)) {
                if (!usePearl(client, Hand.MAIN_HAND)) {
                    reset();
                    return;
                }
                scheduleReturn(now);
            } else {
                state = State.THROWING;
                actionTimeMs = now + 50L * GaussianTimingEngine.sampleActionTicks(30.0D, ClickPearlConfig.randomDelay);
            }
        } catch (Throwable error) {
            activity.client.module.api.ModuleDiagnostics.report("click_pearl", "trigger", error);
            reset();
        }
    }

    private boolean usable(MinecraftClient client) {
        return client != null && client.player != null && client.world != null && client.interactionManager != null
                && (client.currentScreen == null || (client.currentScreen instanceof InventoryScreen && ownsInventoryScreen(client)))
                && client.player.isAlive() && !client.player.isSpectator()
                && client.player.currentScreenHandler == client.player.playerScreenHandler;
    }

    private boolean owns(MinecraftClient client) {
        return client != null && client.player == owner && client.world == world && owner != null;
    }

    private boolean usePearl(MinecraftClient client, Hand hand) {
        if (!usable(client) || !client.player.getStackInHand(hand).isOf(Items.ENDER_PEARL)) {
            activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "use_pearl", false, "pearl_not_in_hand");
            return false;
        }
        if (ClickPearlConfig.checkCooldown
                && client.player.getItemCooldownManager().isCoolingDown(client.player.getStackInHand(hand))) {
            activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "use_pearl", false, "pearl_on_cooldown");
            return false;
        }
        ActionResult result = client.interactionManager.interactItem(client.player, hand);
        dev.impact.SurfaceImpactController.recordPearlThrown();
        if (ClickPearlConfig.swingHand && result instanceof ActionResult.Success success
                && success.swingSource() == ActionResult.SwingSource.CLIENT) client.player.swingHand(hand);
        boolean accepted = result.isAccepted();
        activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "use_pearl", accepted, "hand=" + hand + " result=" + result);
        return accepted;
    }

    private void scheduleReturn(long now) {
        if (!restoreSelected && !(inventorySwapped && returnPearl)) { clear(); return; }
        if (inventorySwapped && returnPearl) {
            state = State.OPENING_RETURN;
            actionTimeMs = now + (long) Math.max(20.0, ClickPearlConfig.switchDelayMs);
            return;
        }
        state = State.WAITING_RETURN;
        actionTimeMs = now + 50L * GaussianTimingEngine.sampleActionTicks(ClickPearlConfig.switchDelayMs, ClickPearlConfig.randomDelay);
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        long now = System.currentTimeMillis();
        if (now >= deadlineMs) { reset(); return; }
        if (!usable(client)) { reset(); return; }
        if (now < actionTimeMs) return;

        if (state == State.OPENING_INVENTORY) {
            if (client.currentScreen == null) {
                client.setScreen(new InventoryScreen(owner));
            }
            client.interactionManager.clickSlot(0, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
            activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "swap_to_hotbar", true, "src=" + sourceInvSlot + " dst=" + selectedPearlSlot);
            state = State.CLOSING_INVENTORY;
            actionTimeMs = now + (ClickPearlConfig.randomDelay ? 30L : 15L);
            return;
        }

        if (state == State.CLOSING_INVENTORY) {
            closeInventoryScreen(client);
            SafeSlotManager.selectSlot(client, selectedPearlSlot);
            state = State.THROWING;
            actionTimeMs = now + (ClickPearlConfig.randomDelay ? 25L : 10L);
            return;
        }

        if (state == State.THROWING) {
            if (owner.getInventory().getSelectedSlot() != selectedPearlSlot) {
                SafeSlotManager.selectSlot(client, selectedPearlSlot);
            }
            if (!usePearl(client, Hand.MAIN_HAND)) {
                reset();
                return;
            }
            if (inventorySwapped && returnPearl) {
                state = State.OPENING_RETURN;
                actionTimeMs = now + (long) Math.max(20.0, ClickPearlConfig.switchDelayMs);
            } else {
                scheduleReturn(now);
            }
            return;
        }

        if (state == State.OPENING_RETURN) {
            client.setScreen(new InventoryScreen(owner));
            client.interactionManager.clickSlot(0, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
            activity.client.diagnostic.DiagnosticEngine.recordAction("click_pearl", "return_to_inventory", true, "src=" + sourceInvSlot + " dst=" + selectedPearlSlot);
            state = State.CLOSING_RETURN;
            actionTimeMs = now + (ClickPearlConfig.randomDelay ? 30L : 15L);
            return;
        }

        if (state == State.CLOSING_RETURN) {
            closeInventoryScreen(client);
            if (restoreSelected && owns(client)) {
                SafeSlotManager.restoreSlot(client, originalSlot);
            }
            clear();
            return;
        }

        if (state == State.WAITING_RETURN) {
            finish(client);
        }
    }

    public void cleanup(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        if (System.currentTimeMillis() >= deadlineMs) reset();
    }

    private void closeInventoryScreen(MinecraftClient client) {
        if (client != null && client.currentScreen != null) {
            try {
                client.currentScreen.close();
            } catch (Throwable ignored) {}
            client.setScreen(null);
        }
    }

    private void finish(MinecraftClient client) {
        if (client != null && owns(client)) {
            closeInventoryScreen(client);
            if (inventorySwapped && returnPearl && client.interactionManager != null && sourceInvSlot >= 9 && selectedPearlSlot >= 0) {
                client.setScreen(new InventoryScreen(owner));
                client.interactionManager.clickSlot(0, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
                closeInventoryScreen(client);
            }
            if (restoreSelected && owner.getInventory().getSelectedSlot() == selectedPearlSlot) {
                SafeSlotManager.restoreSlot(client, originalSlot);
            }
        }
        clear();
    }

    public void reset() {
        if (state == State.IDLE && owner == null) return;
        finish(MinecraftClient.getInstance());
    }

    private void clear() {
        state = State.IDLE;
        owner = null;
        world = null;
        originalSlot = selectedPearlSlot = sourceInvSlot = -1;
        inventorySwapped = returnPearl = restoreSelected = false;
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, false);
    }

    public State getState() { return state; }

    public static int findHotbarItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) return i;
        }
        return -1;
    }

    public static int findInventoryItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) return i;
        }
        return -1;
    }

    public static boolean isOffhandItem(ClientPlayerEntity player, Item targetItem) {
        return player != null && targetItem != null && player.getOffHandStack().isOf(targetItem);
    }
}
