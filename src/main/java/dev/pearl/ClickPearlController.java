package dev.pearl;

import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Random;

public final class ClickPearlController {
    private static final Random RNG = new Random();

    public enum State {
        IDLE,
        THROWING,
        WAITING_RETURN
    }

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int selectedPearlSlot = -1;
    private int sourceInvSlot = -1;
    private int targetHotbarIndex = -1;
    private boolean isInventorySwapped = false;

    private long throwTimeMs = 0L;
    private long returnTimeMs = 0L;
    private long lastTriggerTimeMs = 0L;

    public void trigger(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (ClickPearlConfig.combatGuard && client.currentScreen != null) {
            return;
        }
        if (!client.player.isAlive()) {
            return;
        }
        if (state != State.IDLE) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerTimeMs < 150L) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (ClickPearlConfig.checkCooldown && player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            return;
        }

        lastTriggerTimeMs = now;

        if (ClickPearlConfig.preferOffhand && isOffhandItem(player, Items.ENDER_PEARL)) {
            ActionResult res = client.interactionManager.interactItem(player, Hand.OFF_HAND);
            if (ClickPearlConfig.swingHand && res.isAccepted()) {
                player.swingHand(Hand.OFF_HAND);
            }
            return;
        }

        int hotbarSlot = findHotbarItem(player, Items.ENDER_PEARL);
        if (hotbarSlot >= 0) {
            handleHotbarThrow(client, player, hotbarSlot, now);
            return;
        }

        if ("inventory".equalsIgnoreCase(ClickPearlConfig.searchMode)) {
            int invSlot = findInventoryItem(player, Items.ENDER_PEARL);
            if (invSlot >= 9) {
                handleInventoryThrow(client, player, invSlot, now);
            }
        }
    }

    private void handleHotbarThrow(MinecraftClient client, ClientPlayerEntity player, int hotbarSlot, long now) {
        originalSlot = player.getInventory().getSelectedSlot();
        selectedPearlSlot = hotbarSlot;

        String mode = ClickPearlConfig.mode != null ? ClickPearlConfig.mode : "fast";

        if ("fast".equalsIgnoreCase(mode)) {
            if (originalSlot != hotbarSlot) {
                SafeSlotManager.selectSlot(client, hotbarSlot);
            }
            ActionResult res = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            if (ClickPearlConfig.swingHand && res.isAccepted()) {
                player.swingHand(Hand.MAIN_HAND);
            }

            if (ClickPearlConfig.switchBack && originalSlot != hotbarSlot) {
                if (ClickPearlConfig.switchDelayMs <= 5.0) {
                    SafeSlotManager.selectSlot(client, originalSlot);
                    originalSlot = -1;
                    state = State.IDLE;
                } else {
                    long delay = (long) ClickPearlConfig.switchDelayMs;
                    if (ClickPearlConfig.randomDelay) {
                        delay += RNG.nextInt(20);
                    }
                    returnTimeMs = now + delay;
                    state = State.WAITING_RETURN;
                }
            } else {
                originalSlot = -1;
                state = State.IDLE;
            }
        } else {
            if (originalSlot != hotbarSlot) {
                SafeSlotManager.selectSlot(client, hotbarSlot);
            }
            long prepDelay = ClickPearlConfig.randomDelay ? 30L + RNG.nextInt(25) : 30L;
            throwTimeMs = now + prepDelay;
            state = State.THROWING;
        }
    }

    private void handleInventoryThrow(MinecraftClient client, ClientPlayerEntity player, int invSlot, long now) {
        originalSlot = player.getInventory().getSelectedSlot();
        sourceInvSlot = invSlot;
        targetHotbarIndex = ClickPearlConfig.getTargetHotbarIndex();
        isInventorySwapped = true;

        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                sourceInvSlot,
                targetHotbarIndex,
                SlotActionType.SWAP,
                player
        );
        selectedPearlSlot = targetHotbarIndex;
        SafeSlotManager.selectSlot(client, targetHotbarIndex);

        String mode = ClickPearlConfig.mode != null ? ClickPearlConfig.mode : "fast";

        if ("fast".equalsIgnoreCase(mode)) {
            ActionResult res = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            if (ClickPearlConfig.swingHand && res.isAccepted()) {
                player.swingHand(Hand.MAIN_HAND);
            }

            long delay = Math.max(30L, (long) ClickPearlConfig.switchDelayMs);
            if (ClickPearlConfig.randomDelay) {
                delay += RNG.nextInt(20);
            }
            returnTimeMs = now + delay;
            state = State.WAITING_RETURN;
        } else {
            long prepDelay = ClickPearlConfig.randomDelay ? 40L + RNG.nextInt(25) : 40L;
            throwTimeMs = now + prepDelay;
            state = State.THROWING;
        }
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) {
            return;
        }
        if (client == null || client.player == null || !client.player.isAlive()) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();

        if (state == State.THROWING) {
            if (now >= throwTimeMs) {
                ClientPlayerEntity player = client.player;
                ActionResult res = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                if (ClickPearlConfig.swingHand && res.isAccepted()) {
                    player.swingHand(Hand.MAIN_HAND);
                }

                if (ClickPearlConfig.switchBack || isInventorySwapped) {
                    long delay = Math.max(30L, (long) ClickPearlConfig.switchDelayMs);
                    if (ClickPearlConfig.randomDelay) {
                        delay += RNG.nextInt(20);
                    }
                    returnTimeMs = now + delay;
                    state = State.WAITING_RETURN;
                } else {
                    originalSlot = -1;
                    state = State.IDLE;
                }
            }
            return;
        }

        if (state == State.WAITING_RETURN) {
            if (now >= returnTimeMs) {
                if (isInventorySwapped && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
                    client.interactionManager.clickSlot(
                            client.player.playerScreenHandler.syncId,
                            sourceInvSlot,
                            targetHotbarIndex,
                            SlotActionType.SWAP,
                            client.player
                    );
                    isInventorySwapped = false;
                    sourceInvSlot = -1;
                    targetHotbarIndex = -1;
                }

                if (ClickPearlConfig.switchBack && originalSlot >= 0 && originalSlot < 9) {
                    SafeSlotManager.selectSlot(client, originalSlot);
                }
                originalSlot = -1;
                selectedPearlSlot = -1;
                state = State.IDLE;
            }
        }
    }

    public void reset() {
        if (isInventorySwapped && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null && client.interactionManager != null) {
                try {
                    client.interactionManager.clickSlot(
                            client.player.playerScreenHandler.syncId,
                            sourceInvSlot,
                            targetHotbarIndex,
                            SlotActionType.SWAP,
                            client.player
                    );
                } catch (Throwable ignored) {}
            }
            isInventorySwapped = false;
            sourceInvSlot = -1;
            targetHotbarIndex = -1;
        }

        if (originalSlot >= 0 && originalSlot < 9) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                SafeSlotManager.selectSlot(client, originalSlot);
            }
        }
        originalSlot = -1;
        selectedPearlSlot = -1;
        state = State.IDLE;
    }

    public State getState() {
        return state;
    }

    public static int findHotbarItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    public static int findInventoryItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return -1;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isOffhandItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return false;
        ItemStack offhand = player.getOffHandStack();
        return !offhand.isEmpty() && offhand.isOf(targetItem);
    }
}
