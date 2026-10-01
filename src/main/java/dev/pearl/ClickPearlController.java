package dev.pearl;

import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class ClickPearlController {
    public enum State { IDLE, THROWING, WAITING_RETURN }
    private State state = State.IDLE;
    private ClientPlayerEntity owner;
    private World world;
    private int originalSlot = -1;
    private int selectedPearlSlot = -1;
    private int sourceInvSlot = -1;
    private boolean inventorySwapped;
    private boolean cleanupPending;
    private ItemStack displacedStack;
    private long actionTimeMs;
    private long lastTriggerTimeMs;

    public void trigger(MinecraftClient client) {
        if (!usable(client) || !ClickPearlConfig.enabled || state != State.IDLE
                || CombatLockManager.isLocked()) return;
        ClientPlayerEntity player = client.player;
        if (ClickPearlConfig.combatGuard && player.isUsingItem()) return;
        long now = System.currentTimeMillis();
        if (now - lastTriggerTimeMs < 150L) return;
        if (ClickPearlConfig.checkCooldown && player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) return;
        if (ClickPearlConfig.preferOffhand && isOffhandItem(player, Items.ENDER_PEARL)) {
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
            }
            return;
        }
        owner = player;
        world = client.world;
        originalSlot = player.getInventory().getSelectedSlot();
        lastTriggerTimeMs = now;
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, true);
        try {
            if (inventory >= 9) {
                sourceInvSlot = inventory;
                hotbar = ClickPearlConfig.getTargetHotbarIndex();
                displacedStack = player.getInventory().getStack(hotbar).copy();
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, inventory, hotbar, SlotActionType.SWAP, player);
                inventorySwapped = true;
            }
            selectedPearlSlot = hotbar;
            SafeSlotManager.selectSlot(client, hotbar);
            if (player.getInventory().getSelectedSlot() != hotbar || !player.getMainHandStack().isOf(Items.ENDER_PEARL)) {
                reset();
                return;
            }
            if ("fast".equalsIgnoreCase(ClickPearlConfig.mode) && !inventorySwapped) {
                usePearl(client, Hand.MAIN_HAND);
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

    private static boolean usable(MinecraftClient client) {
        return client != null && client.player != null && client.world != null && client.interactionManager != null
                && client.currentScreen == null && client.player.isAlive() && !client.player.isSpectator()
                && client.player.currentScreenHandler == client.player.playerScreenHandler;
    }

    private boolean owns(MinecraftClient client) {
        return client != null && client.player == owner && client.world == world && owner != null;
    }

    private boolean usePearl(MinecraftClient client, Hand hand) {
        if (!usable(client) || !client.player.getStackInHand(hand).isOf(Items.ENDER_PEARL)) return false;
        ActionResult result = client.interactionManager.interactItem(client.player, hand);
        if (ClickPearlConfig.swingHand && result instanceof ActionResult.Success success
                && success.swingSource() == ActionResult.SwingSource.CLIENT) client.player.swingHand(hand);
        return result.isAccepted();
    }

    private void scheduleReturn(long now) {
        if (!ClickPearlConfig.switchBack && !inventorySwapped) { clear(); return; }
        state = State.WAITING_RETURN;
        actionTimeMs = now + 50L * GaussianTimingEngine.sampleActionTicks(ClickPearlConfig.switchDelayMs, ClickPearlConfig.randomDelay);
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        if (!usable(client)) { reset(); return; }
        if (cleanupPending) { cleanup(client); return; }
        if (owner.getInventory().getSelectedSlot() != selectedPearlSlot) { reset(); return; }
        long now = System.currentTimeMillis();
        if (now < actionTimeMs) return;
        if (state == State.THROWING) {
            usePearl(client, Hand.MAIN_HAND);
            scheduleReturn(now);
        } else if (state == State.WAITING_RETURN) {
            cleanupPending = true;
            cleanup(client);
        }
    }

    public void cleanup(MinecraftClient client) {
        if (!cleanupPending) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        if (client.interactionManager == null) return;
        if (inventorySwapped) {
            if (owner.currentScreenHandler != owner.playerScreenHandler) return;
            ItemStack source = owner.getInventory().getStack(sourceInvSlot);
            ItemStack target = owner.getInventory().getStack(selectedPearlSlot);
            if (ItemStack.areItemsAndComponentsEqual(source, displacedStack) && source.getCount() == displacedStack.getCount()
                    && (target.isEmpty() || target.isOf(Items.ENDER_PEARL))) {
                client.interactionManager.clickSlot(owner.playerScreenHandler.syncId, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
            }
        }
        if (ClickPearlConfig.switchBack && owner.getInventory().getSelectedSlot() == selectedPearlSlot) {
            SafeSlotManager.restoreSlot(client, originalSlot);
        }
        clear();
    }

    public void reset() {
        if (state == State.IDLE && owner == null) return;
        cleanupPending = true;
        state = State.WAITING_RETURN;
        cleanup(MinecraftClient.getInstance());
    }

    private void clear() {
        state = State.IDLE;
        owner = null;
        world = null;
        originalSlot = selectedPearlSlot = sourceInvSlot = -1;
        inventorySwapped = cleanupPending = false;
        displacedStack = null;
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
