package dev.autototem;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class AutoTotemController {
    private boolean enabled = true;
    private State state = State.IDLE;
    private int swappedHotbarSlot = -1;
    private int heldTotemHotbarSlot = -1;
    private int lastTotemHotbarSlot = -1;
    private int savedMainSlot = -1;
    private int lastNonTotemSlot = -1;
    private net.minecraft.item.Item lastNonTotemItem = null;
    private boolean userCancelled = false;
    private long userCancelledTime = 0L;
    private float lastHp = 20.0F;
    private boolean awaitingHealAfterPop = false;
    private long lastPopTime = 0L;
    private static final long POST_POP_GRACE_MS = 1200L;
    private int userOverrideCount = 0;
    private int lastControllerAssignedSlot = -1;
    private long reactionUntil = 0L;
    private int timer = 0;
    private boolean openedByRefill = false;
    private int refillTargetHotbarSlot = -1;
    private int refillInvSlot = -1;
    private long lastRefillTime = 0L;
    private static final long REFILL_COOLDOWN_MS = 400L;

    public AutoTotemController() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            clear();
            awaitingHealAfterPop = false;
            userCancelled = false;
            userOverrideCount = 0;
            lastPopTime = 0L;
        }
    }

    public void onTotemPop() {
        if (!enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || !client.player.isAlive()) return;

        long now = System.currentTimeMillis();
        lastPopTime = now;
        awaitingHealAfterPop = true;
        userOverrideCount = 0;
        userCancelled = false;

        savedMainSlot = resolveReturnSlot(client.player);

        if (AutoTotemConfig.mode == 3) {
            int crystalSlot = getDesignatedCrystalSlot(client.player);
            if (AutoTotemConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                startRefill(client, crystalSlot);
                return;
            }
            int returnSlot = resolveReturnSlot(client.player);
            if (returnSlot >= 0 && returnSlot < 9) {
                SafeSlotManager.selectSlot(client, returnSlot);
            }
            clear();
            awaitingHealAfterPop = true;
            return;
        }

        if (AutoTotemConfig.mode == 1) {
            int nextTotem = findHotbarTotem(client.player);
            if (nextTotem >= 0) {
                heldTotemHotbarSlot = nextTotem;
                lastTotemHotbarSlot = nextTotem;
                lastControllerAssignedSlot = nextTotem;
                userCancelled = false;
                net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);
                SafeSlotManager.selectSlot(client, nextTotem);
                state = State.HOLD_IN_HAND;
                return;
            }

            if (AutoTotemConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                int targetRefill = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                if (targetRefill >= 0) {
                    startRefill(client, targetRefill);
                    return;
                }
            }

            int returnSlot = resolveReturnSlot(client.player);
            if (returnSlot >= 0 && returnSlot < 9) {
                SafeSlotManager.selectSlot(client, returnSlot);
            }
            clear();
            awaitingHealAfterPop = true;
            return;
        }

        if (AutoTotemConfig.mode == 2) {
            float hp = client.player.getHealth();
            float triggerHp = (float) (AutoTotemConfig.triggerHearts * 2.0);
            int nextTotem = findHotbarTotem(client.player);
            if (nextTotem >= 0 && hp <= triggerHp) {
                swappedHotbarSlot = nextTotem;
                lastTotemHotbarSlot = nextTotem;
                SafeSlotManager.selectSlot(client, swappedHotbarSlot);
                timer = 1;
                state = State.SWAP_OFFHAND;
                return;
            }

            if (AutoTotemConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                int targetRefill = resolveRefillTargetSlot(client.player, swappedHotbarSlot);
                if (targetRefill >= 0) {
                    startRefill(client, targetRefill);
                    return;
                }
            }

            if (AutoTotemConfig.returnOnPop && swappedHotbarSlot >= 0) {
                state = State.RESTORE_SWAP_SELECT;
            } else {
                int returnSlot = resolveReturnSlot(client.player);
                if (returnSlot >= 0 && returnSlot < 9) {
                    SafeSlotManager.selectSlot(client, returnSlot);
                }
                clear();
                awaitingHealAfterPop = true;
            }
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

        if (client.player.getInventory() != null) {
            int cur = client.player.getInventory().getSelectedSlot();
            ItemStack curStack = client.player.getInventory().getStack(cur);
            if (!curStack.isEmpty() && !curStack.isOf(Items.TOTEM_OF_UNDYING)) {
                if (cur != heldTotemHotbarSlot && cur != swappedHotbarSlot) {
                    lastNonTotemSlot = cur;
                    lastNonTotemItem = curStack.getItem();
                }
            }
        }

        long now = System.currentTimeMillis();
        float hp = client.player.getHealth();
        float maxHp = client.player.getMaxHealth();
        float triggerHp = (float) (AutoTotemConfig.triggerHearts * 2.0);
        float restoreHp = (float) (AutoTotemConfig.restoreHearts * 2.0);

        boolean isHealed = (hp >= maxHp - 1.0F) || (restoreHp > triggerHp && hp >= restoreHp);
        float effectiveHp = hp + client.player.getAbsorptionAmount();
        if (awaitingHealAfterPop) {
            if (isHealed || effectiveHp >= restoreHp || now - lastPopTime > POST_POP_GRACE_MS) {
                awaitingHealAfterPop = false;
            }
        }
        if (isHealed || (userCancelled && (now - userCancelledTime > 1500L || hp < lastHp - 0.5F))) {
            userCancelled = false;
            userOverrideCount = 0;
            awaitingHealAfterPop = false;
        }
        lastHp = hp;

        switch (state) {
            case WAITING_REACTION -> {
                if (hp > triggerHp) {
                    clear();
                    return;
                }
                if (now >= reactionUntil) {
                    int totemSlot = findHotbarTotem(client.player);
                    if (totemSlot < 0) {
                        if (AutoTotemConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                && findInventoryTotem(client.player) >= 0) {
                            int targetRefill = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                            if (targetRefill >= 0) {
                                startRefill(client, targetRefill);
                            } else {
                                clear();
                            }
                        } else {
                            clear();
                        }
                        return;
                    }
                    int currentSlot = client.player.getInventory().getSelectedSlot();
                    if (!client.player.getInventory().getStack(currentSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                        savedMainSlot = currentSlot;
                    }
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);

                    swappedHotbarSlot = totemSlot;
                    lastTotemHotbarSlot = totemSlot;
                    SafeSlotManager.selectSlot(client, swappedHotbarSlot);
                    timer = 1;
                    state = State.SWAP_OFFHAND;
                }
            }

            case HOLD_IN_HAND -> {
                int targetSlot = heldTotemHotbarSlot >= 0 ? heldTotemHotbarSlot : (AutoTotemConfig.mode == 3 ? getDesignatedCrystalSlot(client.player) : findHotbarTotem(client.player));
                if (targetSlot < 0 || targetSlot >= 9) {
                    clear();
                    return;
                }

                ItemStack slotStack = client.player.getInventory().getStack(targetSlot);
                boolean slotHasTotem = slotStack.isOf(Items.TOTEM_OF_UNDYING);

                if (!slotHasTotem) {

                    int nextTotem = findHotbarTotem(client.player);
                    if (nextTotem >= 0 && hp <= triggerHp) {
                        heldTotemHotbarSlot = nextTotem;
                        lastTotemHotbarSlot = nextTotem;
                        lastControllerAssignedSlot = nextTotem;
                        userOverrideCount = 0;
                        userCancelled = false;
                        SafeSlotManager.selectSlot(client, nextTotem);
                        return;
                    }

                    userOverrideCount = 0;
                    if (AutoTotemConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                        startRefill(client, targetSlot);
                    } else {
                        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
                        int returnSlot = resolveReturnSlot(client.player);
                        if (returnSlot >= 0 && returnSlot < 9) {
                            SafeSlotManager.selectSlot(client, returnSlot);
                        }
                        clear();
                    }
                    return;
                }

                int currentSelected = client.player.getInventory().getSelectedSlot();
                if (currentSelected != targetSlot) {
                    userOverrideCount++;
                    if (userOverrideCount < 2) {

                        SafeSlotManager.selectSlot(client, targetSlot);
                        lastControllerAssignedSlot = targetSlot;
                    } else {

                        userCancelled = true;
                        userCancelledTime = now;
                        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
                        state = State.IDLE;
                        return;
                    }
                } else {
                    lastControllerAssignedSlot = targetSlot;
                }

                if (hp >= restoreHp) {
                    if (AutoTotemConfig.returnItem) {
                        int returnSlot = resolveReturnSlot(client.player);
                        if (returnSlot >= 0 && returnSlot < 9) {
                            SafeSlotManager.selectSlot(client, returnSlot);
                        }
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

                    if (AutoTotemConfig.returnOnPop && swappedHotbarSlot >= 0) {
                        state = State.RESTORE_SWAP_SELECT;
                    } else {

                        if (AutoTotemConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                            startRefill(client, swappedHotbarSlot >= 0 ? swappedHotbarSlot : 0);
                        } else {
                            clear();
                        }
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

            case REFILL_WAIT_OPEN -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (client.currentScreen != null && !(client.currentScreen instanceof InventoryScreen)) {
                    clear();
                    return;
                }
                if (client.currentScreen == null) {
                    client.setScreen(new InventoryScreen(client.player));
                    openedByRefill = true;
                } else {
                    openedByRefill = false;
                }
                long swapMs = GaussianTimingEngine.getDelay(120.0D, 25.0D, 60L, 250L);
                timer = Math.max(2, (int) Math.round(swapMs / 50.0D));
                state = State.REFILL_WAIT_SWAP;
            }

            case REFILL_WAIT_SWAP -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (openedByRefill && client.currentScreen == null) {
                    clear();
                    return;
                }
                int invTotem = findInventoryTotem(client.player);
                if (invTotem < 0) {
                    finishRefill(client);
                    return;
                }
                int targetHotbar = refillTargetHotbarSlot;
                if (targetHotbar < 0 || targetHotbar >= 9) {
                    targetHotbar = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                }
                if (targetHotbar < 0 || targetHotbar >= 9) {
                    finishRefill(client);
                    return;
                }

                ItemStack currentStack = client.player.getInventory().getStack(targetHotbar);
                if (currentStack.isOf(Items.TOTEM_OF_UNDYING)) {
                    finishRefill(client);
                    return;
                }
                if (!currentStack.isEmpty()) {
                    int empty = findEmptyHotbarSlot(client.player);
                    if (empty >= 0) {
                        targetHotbar = empty;
                    }

                }

                if (client.player.isSprinting()) {
                    client.player.setSprinting(false);
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                            client.player,
                            net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.STOP_SPRINTING
                        ));
                    }
                }

                client.interactionManager.clickSlot(
                    client.player.playerScreenHandler.syncId,
                    invTotem,
                    targetHotbar,
                    SlotActionType.SWAP,
                    client.player
                );
                lastTotemHotbarSlot = targetHotbar;
                long closeMs = GaussianTimingEngine.getDelay(90.0D, 20.0D, 40L, 200L);
                timer = Math.max(1, (int) Math.round(closeMs / 50.0D));
                state = State.REFILL_WAIT_CLOSE;
            }

            case REFILL_WAIT_CLOSE -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                finishRefill(client);
            }

            case IDLE -> {
                if (userCancelled) {
                    return;
                }
                if (awaitingHealAfterPop) {
                    float effHp = hp + client.player.getAbsorptionAmount();
                    if (now - lastPopTime < POST_POP_GRACE_MS && effHp > 2.0F) {
                        return;
                    }
                }
                if (hp <= triggerHp) {
                    if (AutoTotemConfig.mode == 3) {

                        int crystalSlot = getDesignatedCrystalSlot(client.player);
                        ItemStack slotStack = client.player.getInventory().getStack(crystalSlot);
                        if (slotStack.isOf(Items.TOTEM_OF_UNDYING)) {
                            if (AutoTotemConfig.chance < 100) {
                                int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                                if (roll >= AutoTotemConfig.chance) {
                                    return;
                                }
                            }
                            int currentSlot = client.player.getInventory().getSelectedSlot();
                            if (currentSlot != crystalSlot && !client.player.getInventory().getStack(currentSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                                savedMainSlot = currentSlot;
                            } else if (savedMainSlot < 0 || savedMainSlot == crystalSlot) {
                                savedMainSlot = resolveReturnSlot(client.player);
                            }
                            heldTotemHotbarSlot = crystalSlot;
                            lastTotemHotbarSlot = crystalSlot;
                            lastControllerAssignedSlot = crystalSlot;
                            userOverrideCount = 0;
                            net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);
                            SafeSlotManager.selectSlot(client, crystalSlot);
                            state = State.HOLD_IN_HAND;
                            return;
                        } else {

                            if (AutoTotemConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                    && findInventoryTotem(client.player) >= 0) {
                                startRefill(client, crystalSlot);
                            }
                            return;
                        }
                    } else if (AutoTotemConfig.mode == 1) {

                        if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            return;
                        }
                        int totemSlot = findHotbarTotem(client.player);
                        if (totemSlot >= 0) {
                            if (AutoTotemConfig.chance < 100) {
                                int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                                if (roll >= AutoTotemConfig.chance) {
                                    return;
                                }
                            }
                            int currentSlot = client.player.getInventory().getSelectedSlot();
                            if (!client.player.getInventory().getStack(currentSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                                savedMainSlot = currentSlot;
                            } else if (savedMainSlot < 0 || savedMainSlot == totemSlot) {
                                savedMainSlot = resolveReturnSlot(client.player);
                            }
                            heldTotemHotbarSlot = totemSlot;
                            lastTotemHotbarSlot = totemSlot;
                            lastControllerAssignedSlot = totemSlot;
                            userOverrideCount = 0;
                            net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);
                            SafeSlotManager.selectSlot(client, totemSlot);
                            state = State.HOLD_IN_HAND;
                            return;
                        } else {
                            if (AutoTotemConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                    && findInventoryTotem(client.player) >= 0) {
                                int targetRefill = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                                if (targetRefill >= 0) {
                                    startRefill(client, targetRefill);
                                }
                            }
                            return;
                        }
                    } else {

                        if (client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            return;
                        }
                        int totemSlot = findHotbarTotem(client.player);
                        if (totemSlot < 0) {
                            if (AutoTotemConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                    && findInventoryTotem(client.player) >= 0) {
                                int targetRefill = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                                if (targetRefill >= 0) {
                                    startRefill(client, targetRefill);
                                }
                            }
                            return;
                        }
                        if (AutoTotemConfig.chance < 100) {
                            int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                            if (roll >= AutoTotemConfig.chance) {
                                return;
                            }
                        }
                        reactionUntil = now + GaussianTimingEngine.getDelay(120.0D, 20.0D, 70L, 200L);
                        state = State.WAITING_REACTION;
                    }
                }
            }
        }
    }

    private void startRefill(MinecraftClient client, int targetHotbar) {
        refillTargetHotbarSlot = targetHotbar;
        timer = 1;
        state = State.REFILL_WAIT_OPEN;
    }

    private void finishRefill(MinecraftClient client) {
        if (openedByRefill && client.currentScreen instanceof InventoryScreen) {
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
            client.setScreen(null);
        }
        lastRefillTime = System.currentTimeMillis();
        openedByRefill = false;
        int refilledSlot = refillTargetHotbarSlot;
        refillTargetHotbarSlot = -1;
        refillInvSlot = -1;
        timer = 0;

        if (client.player != null && client.player.isAlive()) {
            float hp = client.player.getHealth();
            float triggerHp = (float) (AutoTotemConfig.triggerHearts * 2.0);

            if (AutoTotemConfig.mode == 3) {
                net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
                int returnSlot = resolveReturnSlot(client.player);
                if (returnSlot >= 0 && returnSlot < 9) {
                    SafeSlotManager.selectSlot(client, returnSlot);
                }
                clear();
                awaitingHealAfterPop = true;
                return;
            }

            if (hp <= triggerHp && !awaitingHealAfterPop) {
                if (AutoTotemConfig.mode == 1) {
                    int slotToHold = (refilledSlot >= 0 && refilledSlot < 9) ? refilledSlot : findHotbarTotem(client.player);
                    if (slotToHold >= 0) {
                        heldTotemHotbarSlot = slotToHold;
                        lastTotemHotbarSlot = slotToHold;
                        lastControllerAssignedSlot = slotToHold;
                        userOverrideCount = 0;
                        userCancelled = false;
                        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", true);
                        SafeSlotManager.selectSlot(client, slotToHold);
                        state = State.HOLD_IN_HAND;
                        return;
                    }
                } else if (AutoTotemConfig.mode == 2) {
                    int slotToSwap = (refilledSlot >= 0 && refilledSlot < 9) ? refilledSlot : findHotbarTotem(client.player);
                    if (slotToSwap >= 0) {
                        swappedHotbarSlot = slotToSwap;
                        lastTotemHotbarSlot = slotToSwap;
                        SafeSlotManager.selectSlot(client, slotToSwap);
                        timer = 1;
                        state = State.SWAP_OFFHAND;
                        return;
                    }
                }
            }

            int returnSlot = resolveReturnSlot(client.player);
            if (returnSlot >= 0 && returnSlot < 9) {
                SafeSlotManager.selectSlot(client, returnSlot);
            }
        }
        clear();
    }

    public int getDesignatedCrystalSlot(ClientPlayerEntity player) {
        if (AutoTotemConfig.refillSlot >= 0 && AutoTotemConfig.refillSlot < 9) {
            return AutoTotemConfig.refillSlot;
        }
        if (player != null) {
            ItemStack s8 = player.getInventory().getStack(8);
            if (s8.isOf(Items.TOTEM_OF_UNDYING) || s8.isEmpty()) {
                return 8;
            }
            for (int i = 8; i >= 0; i--) {
                if (player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    return i;
                }
            }
        }
        return 8;
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

        if (AutoTotemConfig.mode == 3) {
            int crystalSlot = getDesignatedCrystalSlot(player);
            if (crystalSlot >= 0 && player.getInventory().getStack(crystalSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                return crystalSlot;
            }
        }

        if (AutoTotemConfig.refillSlot >= 0 && AutoTotemConfig.refillSlot < 9) {
            if (player.getInventory().getStack(AutoTotemConfig.refillSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                return AutoTotemConfig.refillSlot;
            }
        }

        if (lastTotemHotbarSlot >= 0 && lastTotemHotbarSlot < 9) {
            if (player.getInventory().getStack(lastTotemHotbarSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                return lastTotemHotbarSlot;
            }
        }

        int cached = activity.client.module.service.InventoryScanService.findHotbarTotemSlot(player);
        if (cached >= 0 && player.getInventory().getStack(cached).isOf(Items.TOTEM_OF_UNDYING)) return cached;

        for (int s = 0; s < 9; s++) {
            if (player.getInventory().getStack(s).isOf(Items.TOTEM_OF_UNDYING)) {
                return s;
            }
        }
        return -1;
    }

    private int findInventoryTotem(ClientPlayerEntity player) {
        if (player == null) return -1;

        for (int s = 9; s < 36; s++) {
            if (player.getInventory().getStack(s).isOf(Items.TOTEM_OF_UNDYING)) {
                return s;
            }
        }
        return -1;
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

    private int resolveRefillTargetSlot(ClientPlayerEntity player, int preferredSlot) {
        if (player == null) return -1;

        if (AutoTotemConfig.mode == 3) {
            return getDesignatedCrystalSlot(player);
        }

        if (AutoTotemConfig.refillSlot >= 0 && AutoTotemConfig.refillSlot < 9) {
            int target = AutoTotemConfig.refillSlot;
            ItemStack stack = player.getInventory().getStack(target);
            if (stack.isEmpty() || stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return target;
            }
            int empty = findEmptyHotbarSlot(player);
            return empty >= 0 ? empty : target;
        }

        if (preferredSlot >= 0 && preferredSlot < 9) {
            return preferredSlot;
        }
        if (lastTotemHotbarSlot >= 0 && lastTotemHotbarSlot < 9) {
            return lastTotemHotbarSlot;
        }

        int empty = findEmptyHotbarSlot(player);
        if (empty >= 0) return empty;

        for (int i = 8; i >= 0; i--) {
            if (savedMainSlot >= 0 && i == savedMainSlot) continue;
            return i;
        }
        return 8;
    }

    private void clear() {
        state = State.IDLE;
        swappedHotbarSlot = -1;
        heldTotemHotbarSlot = -1;
        savedMainSlot = -1;
        lastControllerAssignedSlot = -1;
        reactionUntil = 0L;
        timer = 0;
        openedByRefill = false;
        refillTargetHotbarSlot = -1;
        refillInvSlot = -1;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.totem_active", false);
    }

    private int findPreferredWeaponSlot(ClientPlayerEntity player) {
        if (player == null) return 0;
        int sword = activity.client.module.service.InventoryScanService.findSwordSlot(player);
        if (sword >= 0 && sword < 9) return sword;
        int axe = activity.client.module.service.InventoryScanService.findAxeSlot(player);
        if (axe >= 0 && axe < 9) return axe;
        int mace = activity.client.module.service.InventoryScanService.findMaceSlot(player);
        if (mace >= 0 && mace < 9) return mace;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && !stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return 0;
    }

    public int resolveReturnSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return 0;

        int anchorSlot = dev.luminance.AnchorController.getLastAnchorOriginalSlot();
        if (anchorSlot >= 0 && anchorSlot < 9) {
            ItemStack s = player.getInventory().getStack(anchorSlot);
            if (!s.isEmpty() && (s.isOf(net.minecraft.block.Blocks.RESPAWN_ANCHOR.asItem()) || s.isOf(Items.END_CRYSTAL))) {
                return anchorSlot;
            }
        }

        if (lastNonTotemItem != null && lastNonTotemSlot >= 0 && lastNonTotemSlot < 9) {
            ItemStack s = player.getInventory().getStack(lastNonTotemSlot);
            if (!s.isEmpty() && s.isOf(lastNonTotemItem)) {
                return lastNonTotemSlot;
            }
        }

        int pssSlot = activity.client.module.service.PlayerStateService.getLastNonTotemSlot();
        net.minecraft.item.Item pssItem = activity.client.module.service.PlayerStateService.getLastNonTotemItem();
        if (pssItem != null && pssSlot >= 0 && pssSlot < 9) {
            ItemStack s = player.getInventory().getStack(pssSlot);
            if (!s.isEmpty() && s.isOf(pssItem)) {
                return pssSlot;
            }
        }

        if (lastNonTotemItem != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = player.getInventory().getStack(i);
                if (!s.isEmpty() && s.isOf(lastNonTotemItem)) {
                    return i;
                }
            }
        }

        if (savedMainSlot >= 0 && savedMainSlot < 9) {
            ItemStack s = player.getInventory().getStack(savedMainSlot);
            if (!s.isEmpty() && !s.isOf(Items.TOTEM_OF_UNDYING)) {
                return savedMainSlot;
            }
        }

        if (AutoTotemConfig.mode == 3) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = player.getInventory().getStack(i);
                if (!s.isEmpty() && (s.isOf(net.minecraft.block.Blocks.RESPAWN_ANCHOR.asItem()) || s.isOf(Items.END_CRYSTAL))) {
                    return i;
                }
            }
        }

        if (lastNonTotemSlot >= 0 && lastNonTotemSlot < 9) {
            ItemStack s = player.getInventory().getStack(lastNonTotemSlot);
            if (!s.isOf(Items.TOTEM_OF_UNDYING)) {
                return lastNonTotemSlot;
            }
        }

        return findPreferredWeaponSlot(player);
    }

    public void setLastNonTotemSlotForTest(int slot, net.minecraft.item.Item item) {
        this.lastNonTotemSlot = slot;
        this.lastNonTotemItem = item;
    }

    public int getLastNonTotemSlot() {
        return this.lastNonTotemSlot;
    }

    public net.minecraft.item.Item getLastNonTotemItem() {
        return this.lastNonTotemItem;
    }

    public void setSavedMainSlotForTest(int slot) {
        this.savedMainSlot = slot;
    }

    public int getSavedMainSlot() {
        return this.savedMainSlot;
    }

    public boolean isAwaitingHealAfterPop() {
        return this.awaitingHealAfterPop;
    }

    public void setAwaitingHealAfterPopForTest(boolean val) {
        this.awaitingHealAfterPop = val;
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
        RESTORE_SWAP_MAIN,
        REFILL_WAIT_OPEN,
        REFILL_WAIT_SWAP,
        REFILL_WAIT_CLOSE
    }
}
