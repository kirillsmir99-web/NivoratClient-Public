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
    public enum State { IDLE, OPENING_INVENTORY, CLOSING_INVENTORY, THROWING, WAITING_RETURN, OPENING_RETURN, CLOSING_RETURN }
    private State state = State.IDLE;
    private ClientPlayerEntity owner;
    private World world;
    private int originalSlot = -1;
    private int selectedPearlSlot = -1;
    private int sourceInvSlot = -1;
    private boolean inventorySwapped;
    private boolean returnPearl;
    private boolean restoreSelected;
    private long deadlineMs;
    private ItemStack pearlStack;
    private ItemStack displacedStack;
    private long actionTimeMs;
    private long lastTriggerTimeMs;
    private net.minecraft.client.gui.screen.Screen inventoryScreen;


    public boolean ownsInventoryScreen(MinecraftClient client) {
        return inventoryScreen != null && owns(client) && client.currentScreen == inventoryScreen;
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
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, true);
        try {
            if (inventory >= 9) {
                sourceInvSlot = inventory;
                hotbar = ClickPearlConfig.getTargetHotbarIndex();
                displacedStack = player.getInventory().getStack(hotbar).copy();
                selectedPearlSlot = hotbar;
                pearlStack = player.getInventory().getStack(inventory).copy();
                openInventory(client, State.OPENING_INVENTORY);
                return;
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
        if (!restoreSelected && !(inventorySwapped && returnPearl)) { clear(); return; }
        state = State.WAITING_RETURN;
        actionTimeMs = now + 50L * GaussianTimingEngine.sampleActionTicks(ClickPearlConfig.switchDelayMs, ClickPearlConfig.randomDelay);
    }

    private void openInventory(MinecraftClient client, State next) {
        state = next;
        actionTimeMs = System.currentTimeMillis() + 100L;
        inventoryScreen = new net.minecraft.client.gui.screen.ingame.InventoryScreen(owner) {
            @Override public boolean shouldPause() { return false; }
        };
        client.setScreen(inventoryScreen);
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen) inventoryScreen = client.currentScreen;
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        long now = System.currentTimeMillis();
        if (now >= deadlineMs) { reset(); return; }
        if (inventoryScreen != null) {
            if (!ownsInventoryScreen(client) || client.interactionManager == null
                    || owner.currentScreenHandler != owner.playerScreenHandler
                    || !owner.playerScreenHandler.getCursorStack().isEmpty()) { reset(); return; }
            if (now < actionTimeMs) return;
            if (state == State.OPENING_INVENTORY) {
                ItemStack source = owner.getInventory().getStack(sourceInvSlot);
                ItemStack target = owner.getInventory().getStack(selectedPearlSlot);
                if (!ItemStack.areEqual(source, pearlStack) || !ItemStack.areEqual(target, displacedStack)) { reset(); return; }
                client.interactionManager.clickSlot(owner.playerScreenHandler.syncId, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
                inventorySwapped = true;
                state = State.CLOSING_INVENTORY;
                actionTimeMs = now + 100L;
            } else if (state == State.CLOSING_INVENTORY) {
                if (!owner.getInventory().getStack(selectedPearlSlot).isOf(Items.ENDER_PEARL)) { reset(); return; }
                inventoryScreen = null;
                client.setScreen(null);
                SafeSlotManager.selectSlot(client, selectedPearlSlot);
                state = State.THROWING;
                actionTimeMs = now + 50L;
            } else if (state == State.OPENING_RETURN) {
                ItemStack source = owner.getInventory().getStack(sourceInvSlot);
                ItemStack target = owner.getInventory().getStack(selectedPearlSlot);
                if (!ItemStack.areEqual(source, displacedStack)
                        || (!target.isEmpty() && !ItemStack.areItemsAndComponentsEqual(target, pearlStack))) { reset(); return; }
                client.interactionManager.clickSlot(owner.playerScreenHandler.syncId, sourceInvSlot, selectedPearlSlot, SlotActionType.SWAP, owner);
                state = State.CLOSING_RETURN;
                actionTimeMs = now + 100L;
            } else if (state == State.CLOSING_RETURN) {
                finish(client);
            }
            return;
        }
        if (!usable(client) || owner.getInventory().getSelectedSlot() != selectedPearlSlot) { reset(); return; }
        if (now < actionTimeMs) return;
        if (state == State.THROWING) {
            if (!usePearl(client, Hand.MAIN_HAND)) { reset(); return; }
            scheduleReturn(now);
        } else if (state == State.WAITING_RETURN) {
            if (inventorySwapped && returnPearl) openInventory(client, State.OPENING_RETURN);
            else finish(client);
        }
    }

    public void cleanup(MinecraftClient client) {
        if (state == State.IDLE) return;
        if (!owns(client) || !owner.isAlive()) { clear(); return; }
        if (System.currentTimeMillis() >= deadlineMs
                || inventoryScreen != null && !ownsInventoryScreen(client)) reset();
    }

    private void finish(MinecraftClient client) {
        if (restoreSelected && owns(client) && owner.getInventory().getSelectedSlot() == selectedPearlSlot) {
            SafeSlotManager.restoreSlot(client, originalSlot);
        }
        boolean close = ownsInventoryScreen(client);
        clear();
        if (close) client.setScreen(null);
    }

    public void reset() {
        if (state == State.IDLE && owner == null) return;
        finish(MinecraftClient.getInstance());
    }

    private void clear() {
        inventoryScreen = null;
        state = State.IDLE;
        owner = null;
        world = null;
        originalSlot = selectedPearlSlot = sourceInvSlot = -1;
        inventorySwapped = returnPearl = restoreSelected = false;
        displacedStack = pearlStack = null;
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
