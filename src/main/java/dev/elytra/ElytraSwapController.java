package dev.elytra;

import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.concurrent.ThreadLocalRandom;

public final class ElytraSwapController {
    public enum State {
        IDLE,
        PREPARE_INTERACT,
        PREPARE_RESTORE
    }

    private static ElytraSwapController INSTANCE;
    private State state = State.IDLE;
    private int originalSlot = -1;
    private int targetSlot = -1;
    private long actionDeadlineMs = 0L;
    private long lastTriggerMs = 0L;

    public ElytraSwapController() {
        INSTANCE = this;
    }

    public static ElytraSwapController getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ElytraSwapController();
        }
        return INSTANCE;
    }

    public State getState() {
        return state;
    }

    public void trigger(MinecraftClient client) {
        if (!ElytraSwapConfig.enabled || client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;
        if (!player.isAlive() || player.isSpectator() || client.currentScreen != null) return;

        long now = System.currentTimeMillis();
        if (now - lastTriggerMs < 120L) return;
        if (state != State.IDLE) {
            if (now > actionDeadlineMs + 1000L) {
                reset();
            } else {
                return;
            }
        }
        lastTriggerMs = now;

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        boolean wearingElytra = chest != null && chest.isOf(Items.ELYTRA);

        int slot;
        if (wearingElytra) {
            slot = findChestplateInHotbar(player);
        } else {
            slot = findElytraInHotbar(player);
        }

        if (slot < 0 || slot >= 9) {
            return;
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        originalSlot = currentSlot;
        targetSlot = slot;

        if (currentSlot == slot) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
            reset();
            return;
        }

        SafeSlotManager.selectSlot(client, slot);
        long delay = ElytraSwapConfig.randomDelay
                ? 35L + ThreadLocalRandom.current().nextLong(30L)
                : 40L;
        actionDeadlineMs = now + delay;
        state = State.PREPARE_INTERACT;
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE || client == null) return;
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null || !player.isAlive()) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        if (now < actionDeadlineMs) {
            return;
        }

        if (state == State.PREPARE_INTERACT) {
            if (player.getInventory().getSelectedSlot() != targetSlot) {
                SafeSlotManager.selectSlot(client, targetSlot);
            }
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);

            if (ElytraSwapConfig.autoRestore && originalSlot >= 0 && originalSlot < 9 && originalSlot != targetSlot) {
                long baseDelay = (long) Math.max(20.0, ElytraSwapConfig.restoreDelayMs);
                long restoreDelay = ElytraSwapConfig.randomDelay
                        ? baseDelay + ThreadLocalRandom.current().nextLong(30L) - 15L
                        : baseDelay;
                actionDeadlineMs = now + Math.max(25L, restoreDelay);
                state = State.PREPARE_RESTORE;
            } else {
                reset();
            }
        } else if (state == State.PREPARE_RESTORE) {
            if (originalSlot >= 0 && originalSlot < 9) {
                SafeSlotManager.restoreSlot(client, originalSlot);
            }
            reset();
        }
    }

    public void reset() {
        state = State.IDLE;
        originalSlot = -1;
        targetSlot = -1;
        actionDeadlineMs = 0L;
    }

    public static int findElytraInHotbar(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s != null && s.isOf(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }

    public static int findChestplateInHotbar(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (isChestplate(s)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isChestplate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.NETHERITE_CHESTPLATE)
                || stack.isOf(Items.DIAMOND_CHESTPLATE)
                || stack.isOf(Items.IRON_CHESTPLATE)
                || stack.isOf(Items.CHAINMAIL_CHESTPLATE)
                || stack.isOf(Items.GOLDEN_CHESTPLATE)
                || stack.isOf(Items.LEATHER_CHESTPLATE)) {
            return true;
        }
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }
}
