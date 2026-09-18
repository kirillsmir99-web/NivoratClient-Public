package dev.autototem;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public final class AutoTotemController {
    private boolean enabled = true;
    private State state = State.IDLE;
    private int swappedHotbarSlot = -1;
    private int savedMainSlot = -1;
    private boolean userCancelled = false;
    private long reactionUntil = 0L;
    private int timer = 0;

    public AutoTotemController() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            clear();
        }
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            clear();
            return;
        }

        if (!enabled || !client.player.isAlive()) {
            return;
        }

        long now = System.currentTimeMillis();
        float hp = activity.client.module.service.PlayerStateService.getHealth();
        float triggerHp = AutoTotemConfig.triggerHearts * 2.0F;
        float restoreHp = AutoTotemConfig.restoreHearts * 2.0F;

        if (hp > triggerHp) {
            userCancelled = false;
        }

        switch (state) {
            case WAITING_REACTION -> {
                if (hp > triggerHp) {
                    clear();
                    return;
                }
                if (now >= reactionUntil) {
                    int totemSlot = findHotbarTotem(client.player);
                    if (totemSlot < 0) {
                        clear();
                        return;
                    }
                    savedMainSlot = client.player.getInventory().getSelectedSlot();
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);
                    if (AutoTotemConfig.mode == 1) {
                        SafeSlotManager.selectSlot(client, totemSlot);
                        state = State.HOLD_IN_HAND;
                    } else {
                        swappedHotbarSlot = totemSlot;
                        SafeSlotManager.selectSlot(client, swappedHotbarSlot);
                        timer = 1;
                        state = State.SWAP_OFFHAND;
                    }
                }
            }
            case HOLD_IN_HAND -> {
                boolean mainHasTotem = client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
                if (!mainHasTotem) {
                    int nextTotem = findHotbarTotem(client.player);
                    if (nextTotem >= 0 && hp <= triggerHp) {
                        SafeSlotManager.selectSlot(client, nextTotem);
                    } else {
                        if (AutoTotemConfig.returnOnPop && savedMainSlot >= 0 && savedMainSlot < 9) {
                            SafeSlotManager.selectSlot(client, savedMainSlot);
                        }
                        clear();
                    }
                    return;
                }

                if (hp >= restoreHp) {
                    if (AutoTotemConfig.returnItem && savedMainSlot >= 0 && savedMainSlot < 9) {
                        SafeSlotManager.selectSlot(client, savedMainSlot);
                    }
                    clear();
                }
            }
            case SELECT_TOTEM -> {
                if (swappedHotbarSlot < 0 || swappedHotbarSlot >= 9) {
                    clear();
                    return;
                }
                SafeSlotManager.selectSlot(client, swappedHotbarSlot);
                timer = 1;
                state = State.SWAP_OFFHAND;
            }
            case SWAP_OFFHAND -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                sendOffhandSwap(client);
                swapLocalHands(client.player);
                if (savedMainSlot >= 0 && savedMainSlot != swappedHotbarSlot) {
                    timer = 1;
                    state = State.RESTORE_MAIN_SLOT;
                } else {
                    state = State.ACTIVE;
                }
            }
            case RESTORE_MAIN_SLOT -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (savedMainSlot >= 0 && savedMainSlot < 9) {
                    SafeSlotManager.selectSlot(client, savedMainSlot);
                }
                state = State.ACTIVE;
            }
            case ACTIVE -> {
                boolean offhandHasTotem = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

                if (!offhandHasTotem) {
                    if (client.player.getOffHandStack().isEmpty()) {
                        if (AutoTotemConfig.returnOnPop && swappedHotbarSlot >= 0) {
                            state = State.RESTORE_SWAP_SELECT;
                        } else {
                            clear();
                        }
                    } else {
                        clear();
                        userCancelled = true;
                    }
                    return;
                }

                if (hp >= restoreHp) {
                    if (AutoTotemConfig.returnItem && swappedHotbarSlot >= 0) {
                        state = State.RESTORE_SWAP_SELECT;
                    } else {
                        clear();
                    }
                }
            }
            case RESTORE_SWAP_SELECT -> {
                if (swappedHotbarSlot >= 0 && swappedHotbarSlot < 9) {
                    SafeSlotManager.selectSlot(client, swappedHotbarSlot);
                    timer = 1;
                    state = State.RESTORE_SWAP_OFFHAND;
                } else {
                    clear();
                }
            }
            case RESTORE_SWAP_OFFHAND -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                sendOffhandSwap(client);
                swapLocalHands(client.player);
                if (savedMainSlot >= 0 && savedMainSlot != swappedHotbarSlot) {
                    timer = 1;
                    state = State.RESTORE_SWAP_MAIN;
                } else {
                    clear();
                }
            }
            case RESTORE_SWAP_MAIN -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (savedMainSlot >= 0 && savedMainSlot < 9) {
                    SafeSlotManager.selectSlot(client, savedMainSlot);
                }
                clear();
            }
            case IDLE -> {
                if (userCancelled) {
                    return;
                }
                if (hp <= triggerHp) {
                    if (AutoTotemConfig.mode == 1 && client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        return;
                    }
                    if (AutoTotemConfig.mode == 2 && client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        return;
                    }
                    int totemSlot = findHotbarTotem(client.player);
                    if (totemSlot < 0) {
                        return;
                    }
                    if (AutoTotemConfig.chance < 100) {
                        int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                        if (roll >= AutoTotemConfig.chance) {
                            return;
                        }
                    }
                    reactionUntil = now + GaussianTimingEngine.getDelay(150.0D, 20.0D, 90L, 240L);
                    state = State.WAITING_REACTION;
                }
            }
        }
    }

    private void sendOffhandSwap(MinecraftClient client) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
            ));
        }
    }

    private void swapLocalHands(ClientPlayerEntity player) {
        if (player == null) return;
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        player.setStackInHand(Hand.MAIN_HAND, off);
        player.setStackInHand(Hand.OFF_HAND, main);
    }

    private int findHotbarTotem(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = activity.client.module.service.InventoryScanService.findHotbarTotemSlot(player);
        if (cached >= 0) return cached;
        for (int s = 0; s < 9; s++) {
            if (player.getInventory().getStack(s).isOf(Items.TOTEM_OF_UNDYING)) {
                return s;
            }
        }
        return -1;
    }

    private void clear() {
        state = State.IDLE;
        swappedHotbarSlot = -1;
        savedMainSlot = -1;
        reactionUntil = 0L;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
    }

    private enum State {
        IDLE,
        WAITING_REACTION,
        HOLD_IN_HAND,
        SELECT_TOTEM,
        SWAP_OFFHAND,
        RESTORE_MAIN_SLOT,
        ACTIVE,
        RESTORE_SWAP_SELECT,
        RESTORE_SWAP_OFFHAND,
        RESTORE_SWAP_MAIN
    }
}
