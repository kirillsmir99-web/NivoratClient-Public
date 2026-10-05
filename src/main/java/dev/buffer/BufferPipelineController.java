package dev.buffer;

import net.fabricmc.pack.api.CombatLockManager;
import activity.client.util.Obf;
import net.fabricmc.pack.api.SafeSlotManager;
import net.fabricmc.pack.api.SlotArbiter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.EnumSet;

public final class BufferPipelineController {
    public static float getEffectiveHealth(PlayerEntity player) {
        if (player == null) return 0.0F;
        return player.getHealth() + (BufferPipelineConfig.countAbsorption ? player.getAbsorptionAmount() : 0.0F);
    }

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
    private float lastHp = Obf.f(467418398);
    private boolean awaitingHealAfterPop = false;
    private long lastPopTime = 0L;
    private static final long POST_POP_GRACE_MS = 50L;
    private int userOverrideCount = 0;
    private int lastControllerAssignedSlot = -1;
    private int timer = 0;
    private boolean openedByRefill = false;
    private int refillTargetHotbarSlot = -1;
    private int refillInvSlot = -1;
    private long lastRefillTime = 0L;
    private static final long REFILL_COOLDOWN_MS = Obf.l(6520153561102040114L);

    private SlotArbiter.Lease activeLease = null;
    private int pendingOffhandTicks = 0;
    private long lastThreatTime = 0L;
    private int lastWarnedTotemCount = -1;
    private boolean chanceRolledForCurrentThreat = false;
    private boolean chancePassedForCurrentThreat = true;

    public BufferPipelineController() {
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

    public boolean isArmed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;
        if (client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (client.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) return true;
        return activeLease != null && activeLease.isActive();
    }

    public void onTotemPop() {
        onTotemPop(MinecraftClient.getInstance());
    }

    public void onTotemPop(MinecraftClient client) {
        if (!enabled) return;
        if (state == State.REFILL_WAIT_OPEN || state == State.REFILL_WAIT_SWAP || state == State.REFILL_WAIT_CLOSE) {
            return;
        }

        long now = System.currentTimeMillis();
        lastPopTime = now;
        lastThreatTime = now;
        awaitingHealAfterPop = false;
        pendingOffhandTicks = 0;
        userOverrideCount = 0;
        userCancelled = false;

        if (client == null || client.player == null || !client.player.isAlive()) {
            clear();
            return;
        }

        savedMainSlot = resolveReturnSlot(client.player);

        if (BufferPipelineConfig.mode == 2 || BufferPipelineConfig.alwaysOffhand) {
            triggerOffhandTotem(client);
            return;
        }

        if (BufferPipelineConfig.mode == 3) {
            int crystalSlot = getDesignatedCrystalSlot(client.player);
            if (BufferPipelineConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                startRefill(client, crystalSlot);
                return;
            }
            int returnSlot = resolveReturnSlot(client.player);
            if (returnSlot >= 0 && returnSlot < 9 && client.player.getInventory().getSelectedSlot() != returnSlot) {
                SafeSlotManager.selectSlot(client, returnSlot);
            }
            clear();
            awaitingHealAfterPop = false;
            return;
        }

        if (BufferPipelineConfig.mode == 1) {
            int nextTotem = findHotbarTotem(client.player);
            if (nextTotem >= 0) {
                heldTotemHotbarSlot = nextTotem;
                lastTotemHotbarSlot = nextTotem;
                lastControllerAssignedSlot = nextTotem;
                acquireTotemLease(EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT));
                if (activeLease != null) {
                    SlotArbiter.selectSlot(client, activeLease, nextTotem);
                } else {
                    SafeSlotManager.selectSlot(client, nextTotem);
                }
                state = State.HOLD_IN_HAND;
                return;
            }

            if (BufferPipelineConfig.autoRefill && findInventoryTotem(client.player) >= 0) {
                int targetRefill = resolveRefillTargetSlot(client.player, lastTotemHotbarSlot);
                if (targetRefill >= 0) {
                    startRefill(client, targetRefill);
                    return;
                }
            }

            int returnSlot = resolveReturnSlot(client.player);
            if (returnSlot >= 0 && returnSlot < 9 && client.player.getInventory().getSelectedSlot() != returnSlot) {
                SafeSlotManager.selectSlot(client, returnSlot);
            }
            clear();
            awaitingHealAfterPop = false;
        }
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            clear();
            return;
        }

        if (!enabled || !client.player.isAlive()) {
            clear();
            return;
        }

        ClientPlayerEntity player = client.player;

        if (player.getInventory() != null) {
            int cur = player.getInventory().getSelectedSlot();
            ItemStack curStack = player.getInventory().getStack(cur);
            if (!curStack.isEmpty() && !curStack.isOf(Items.TOTEM_OF_UNDYING)) {
                if (cur != heldTotemHotbarSlot && cur != swappedHotbarSlot) {
                    lastNonTotemSlot = cur;
                    lastNonTotemItem = curStack.getItem();
                }
            }
        }

        checkTotemQuantityNotification(player);

        long now = System.currentTimeMillis();
        float hp = getEffectiveHealth(player);
        float maxHp = player.getMaxHealth();
        float triggerHp = (float) (BufferPipelineConfig.triggerHearts * 2.0);
        float restoreHp = (float) (BufferPipelineConfig.restoreHearts * 2.0);
        float burstDamage = DamageForecast.calculateExpectedBurst(client, player);
        float projectedHp = hp - burstDamage;

        boolean isThreat = hp <= triggerHp || projectedHp <= triggerHp;

        if (isThreat) {
            lastThreatTime = now;
            if (!chanceRolledForCurrentThreat) {
                chanceRolledForCurrentThreat = true;
                if (BufferPipelineConfig.chance < 100) {
                    int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                    chancePassedForCurrentThreat = (roll < BufferPipelineConfig.chance);
                } else {
                    chancePassedForCurrentThreat = true;
                }
            }
        } else {
            chanceRolledForCurrentThreat = false;
            chancePassedForCurrentThreat = true;
        }

        boolean isHealed = (hp >= maxHp - Obf.f(1711029534)) || (restoreHp > triggerHp && hp >= restoreHp);
        if (awaitingHealAfterPop) {
            if (isHealed || now - lastPopTime > POST_POP_GRACE_MS || !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                awaitingHealAfterPop = false;
            }
        }

        if (isHealed || (userCancelled && (now - userCancelledTime > Obf.l(6520153561102039234L) || hp < lastHp - Obf.f(1702640926)))) {
            userCancelled = false;
            userOverrideCount = 0;
            awaitingHealAfterPop = false;
        }
        lastHp = hp;

        if (pendingOffhandTicks > 0) {
            pendingOffhandTicks--;
            if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                pendingOffhandTicks = 0;
            }
        }

        if (BufferPipelineConfig.ignoreWhenUsing && player.isUsingItem() && projectedHp > Obf.f(452738334)) {
            return;
        }

        switch (state) {
            case HOLD_IN_HAND -> {
                int targetSlot = heldTotemHotbarSlot >= 0 ? heldTotemHotbarSlot : findHotbarTotem(player);
                if (targetSlot < 0 || targetSlot >= 9) {
                    clear();
                    return;
                }

                ItemStack slotStack = player.getInventory().getStack(targetSlot);
                boolean slotHasTotem = slotStack.isOf(Items.TOTEM_OF_UNDYING);

                if (!slotHasTotem) {
                    int nextTotem = findHotbarTotem(player);
                    if (nextTotem >= 0 && isThreat) {
                        heldTotemHotbarSlot = nextTotem;
                        lastTotemHotbarSlot = nextTotem;
                        lastControllerAssignedSlot = nextTotem;
                        userOverrideCount = 0;
                        userCancelled = false;
                        if (activeLease != null) {
                            SlotArbiter.selectSlot(client, activeLease, nextTotem);
                        } else {
                            SafeSlotManager.selectSlot(client, nextTotem);
                        }
                        return;
                    }

                    userOverrideCount = 0;
                    if (BufferPipelineConfig.autoRefill && findInventoryTotem(player) >= 0) {
                        startRefill(client, targetSlot);
                    } else {
                        releaseTotemLease(true);
                        clear();
                    }
                    return;
                }

                int currentSelected = player.getInventory().getSelectedSlot();
                if (currentSelected != targetSlot) {
                    userOverrideCount++;
                    if (userOverrideCount < 2) {
                        if (activeLease != null) {
                            SlotArbiter.selectSlot(client, activeLease, targetSlot);
                        } else {
                            SafeSlotManager.selectSlot(client, targetSlot);
                        }
                        lastControllerAssignedSlot = targetSlot;
                    } else {
                        userCancelled = true;
                        userCancelledTime = now;
                        releaseTotemLease(false);
                        state = State.IDLE;
                        return;
                    }
                } else {
                    lastControllerAssignedSlot = targetSlot;
                }

                boolean canExit = (BufferPipelineConfig.restoreHearts > 0.0 && hp >= restoreHp)
                        || (BufferPipelineConfig.restoreHearts <= 0.0 && !isThreat && hp >= triggerHp + 2.0F);

                if (canExit && now - lastThreatTime > (long)(BufferPipelineConfig.swapBackDelay * 1000.0)) {
                    if (BufferPipelineConfig.returnItem) {
                        releaseTotemLease(true);
                    } else {
                        releaseTotemLease(false);
                    }
                    clear();
                }
            }

            case SWAP_OFFHAND -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    state = State.ACTIVE;
                    return;
                }
                triggerOffhandTotem(client);
                state = State.ACTIVE;
            }

            case ACTIVE -> {
                boolean offhandHasTotem = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

                if (!offhandHasTotem) {
                    if (BufferPipelineConfig.autoRefill && findInventoryTotem(player) >= 0) {
                        int targetRefill = resolveRefillTargetSlot(player, swappedHotbarSlot);
                        if (targetRefill >= 0) {
                            startRefill(client, targetRefill);
                            return;
                        }
                    }
                    releaseTotemLease(true);
                    clear();
                    awaitingHealAfterPop = false;
                    return;
                }

                if (!BufferPipelineConfig.alwaysOffhand) {
                    boolean canRestore = (BufferPipelineConfig.restoreHearts > 0.0 && hp >= restoreHp)
                            || (BufferPipelineConfig.restoreHearts <= 0.0 && !isThreat);
                    if (canRestore && now - lastThreatTime > (long)(BufferPipelineConfig.swapBackDelay * 1000.0)) {
                        releaseTotemLease(true);
                        clear();
                    }
                }
            }

            case REFILL_WAIT_OPEN -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                executeContainerRefill(client);
            }

            case REFILL_WAIT_SWAP -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                finishRefill(client);
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
                    float effHp = player.getHealth() + player.getAbsorptionAmount();
                    if (now - lastPopTime < POST_POP_GRACE_MS && effHp > 2.0F && player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        return;
                    }
                    if (now - lastPopTime >= POST_POP_GRACE_MS || !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        awaitingHealAfterPop = false;
                    }
                }

                if (BufferPipelineConfig.alwaysOffhand && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    triggerOffhandTotem(client);
                    return;
                }

                if (isThreat) {
                    if (!chancePassedForCurrentThreat) {
                        return;
                    }

                    if (BufferPipelineConfig.mode == 2 || BufferPipelineConfig.alwaysOffhand) {
                        triggerOffhandTotem(client);
                        return;
                    }

                    if (BufferPipelineConfig.mode == 3) {
                        int crystalSlot = getDesignatedCrystalSlot(player);
                        ItemStack slotStack = player.getInventory().getStack(crystalSlot);
                        if (slotStack.isOf(Items.TOTEM_OF_UNDYING)) {
                            int currentSlot = player.getInventory().getSelectedSlot();
                            if (currentSlot != crystalSlot && !player.getInventory().getStack(currentSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                                savedMainSlot = currentSlot;
                            } else if (savedMainSlot < 0 || savedMainSlot == crystalSlot) {
                                savedMainSlot = resolveReturnSlot(player);
                            }
                            heldTotemHotbarSlot = crystalSlot;
                            lastTotemHotbarSlot = crystalSlot;
                            lastControllerAssignedSlot = crystalSlot;
                            userOverrideCount = 0;
                            acquireTotemLease(EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT));
                            if (activeLease != null) {
                                SlotArbiter.selectSlot(client, activeLease, crystalSlot);
                            } else {
                                SafeSlotManager.selectSlot(client, crystalSlot);
                            }
                            state = State.HOLD_IN_HAND;
                            return;
                        } else {
                            if (BufferPipelineConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                    && findInventoryTotem(player) >= 0) {
                                startRefill(client, crystalSlot);
                            }
                            return;
                        }
                    } else {
                        if (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            return;
                        }
                        int totemSlot = findHotbarTotem(player);
                        if (totemSlot >= 0) {
                            int currentSlot = player.getInventory().getSelectedSlot();
                            if (!player.getInventory().getStack(currentSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                                savedMainSlot = currentSlot;
                            } else if (savedMainSlot < 0 || savedMainSlot == totemSlot) {
                                savedMainSlot = resolveReturnSlot(player);
                            }
                            heldTotemHotbarSlot = totemSlot;
                            lastTotemHotbarSlot = totemSlot;
                            lastControllerAssignedSlot = totemSlot;
                            userOverrideCount = 0;
                            acquireTotemLease(EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT));
                            if (activeLease != null) {
                                SlotArbiter.selectSlot(client, activeLease, totemSlot);
                            } else {
                                SafeSlotManager.selectSlot(client, totemSlot);
                            }
                            state = State.HOLD_IN_HAND;
                            return;
                        } else {
                            if (BufferPipelineConfig.autoRefill && now - lastRefillTime >= REFILL_COOLDOWN_MS
                                    && findInventoryTotem(player) >= 0) {
                                int targetRefill = resolveRefillTargetSlot(player, lastTotemHotbarSlot);
                                if (targetRefill >= 0) {
                                    startRefill(client, targetRefill);
                                }
                            }
                        }
                    }
                }
            }

            default -> {}
        }
    }

    private void triggerOffhandTotem(MinecraftClient client) {
        if (client == null || client.player == null || client.interactionManager == null) return;
        if (pendingOffhandTicks > 0) return;

        ClientPlayerEntity player = client.player;
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            state = State.ACTIVE;
            return;
        }

        int invTotemSlot = findBestTotemSlot(player, true);
        if (invTotemSlot < 0) {
            return;
        }

        acquireTotemLease(EnumSet.of(SlotArbiter.Resource.OFFHAND, SlotArbiter.Resource.INVENTORY_CLICKS));

        int screenSlot = (invTotemSlot >= 0 && invTotemSlot < 9) ? (36 + invTotemSlot) : invTotemSlot;

        boolean needsLegitGui = Obf.s(new byte[]{(byte) 106, (byte) -66, (byte) 106, (byte) -59, (byte) 39}).equals(BufferPipelineConfig.inventorySource) && invTotemSlot >= 9;
        if (needsLegitGui) {
            client.setScreen(new InventoryScreen(player));
        }

        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                screenSlot,
                40,
                SlotActionType.SWAP,
                player
        );

        if (needsLegitGui) {
            player.closeHandledScreen();
            client.setScreen(null);
        }

        pendingOffhandTicks = 4;
        state = State.ACTIVE;
    }

    private void executeContainerRefill(MinecraftClient client) {
        if (client == null || client.player == null || client.interactionManager == null) {
            finishRefill(client);
            return;
        }

        ClientPlayerEntity player = client.player;
        int invTotem = findInventoryTotem(player);
        if (invTotem < 0) {
            finishRefill(client);
            return;
        }

        int targetHotbar = refillTargetHotbarSlot;
        if (targetHotbar < 0 || targetHotbar >= 9) {
            targetHotbar = resolveRefillTargetSlot(player, lastTotemHotbarSlot);
        }
        if (targetHotbar < 0 || targetHotbar >= 9) {
            finishRefill(client);
            return;
        }

        ItemStack currentStack = player.getInventory().getStack(targetHotbar);
        if (currentStack.isOf(Items.TOTEM_OF_UNDYING)) {
            finishRefill(client);
            return;
        }

        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                invTotem,
                targetHotbar,
                SlotActionType.SWAP,
                player
        );
        lastTotemHotbarSlot = targetHotbar;
        timer = Obf.s(new byte[]{(byte) 106, (byte) -66, (byte) 106, (byte) -59, (byte) 39}).equals(BufferPipelineConfig.inventorySource) ? 2 : 1;
        state = State.REFILL_WAIT_CLOSE;
    }

    private void acquireTotemLease(EnumSet<SlotArbiter.Resource> resources) {
        CombatLockManager.setLock(Obf.s(new byte[]{(byte) 118, (byte) -83, (byte) 125, (byte) -126, (byte) 39, (byte) -101, (byte) 56, (byte) -114, (byte) 65, (byte) 66, (byte) 21, (byte) -92, (byte) 74, (byte) 64, (byte) 44, (byte) -121}), true);
        if (activeLease == null || !activeLease.isActive()) {
            activeLease = SlotArbiter.acquire(Obf.s(new byte[]{(byte) 118, (byte) -83, (byte) 125, (byte) -126, (byte) 39, (byte) -101, (byte) 56, (byte) -114, (byte) 65}), SlotArbiter.Priority.EMERGENCY, resources, Obf.i(1518091554), false);
        }
    }

    private void releaseTotemLease(boolean restore) {
        CombatLockManager.setLock(Obf.s(new byte[]{(byte) 118, (byte) -83, (byte) 125, (byte) -126, (byte) 39, (byte) -101, (byte) 56, (byte) -114, (byte) 65, (byte) 66, (byte) 21, (byte) -92, (byte) 74, (byte) 64, (byte) 44, (byte) -121}), false);
        if (activeLease != null) {
            SlotArbiter.release(activeLease, restore);
            activeLease = null;
        }
    }

    private void checkTotemQuantityNotification(ClientPlayerEntity player) {
        if (!BufferPipelineConfig.lowTotemNotify || player == null) return;
        int total = 0;
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) total++;
        if (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)) total++;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                total++;
            }
        }
        if ((total == 1 || total == 2) && total != lastWarnedTotemCount) {
            lastWarnedTotemCount = total;
            player.sendMessage(Text.translatable(Obf.s(new byte[]{(byte) 103, (byte) -72, (byte) 121, (byte) -59, (byte) 37, (byte) -99, (byte) 56, (byte) -110, (byte) 2, (byte) 105, (byte) 27, (byte) -77, (byte) 91, (byte) 68, (byte) 116, (byte) -114, (byte) -63, (byte) -21, (byte) -38, (byte) -13, (byte) -64, (byte) 21, (byte) 57, (byte) 25, (byte) -109, (byte) -78}), total), true);
        } else if (total > 2) {
            lastWarnedTotemCount = -1;
        }
    }

    private void startRefill(MinecraftClient client, int targetHotbar) {
        if (Obf.s(new byte[]{(byte) 110, (byte) -76, (byte) 121, (byte) -50, (byte) 50, (byte) -122}).equals(BufferPipelineConfig.inventorySource)) {
            return;
        }
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, true);
        refillTargetHotbarSlot = targetHotbar;
        if (Obf.s(new byte[]{(byte) 106, (byte) -66, (byte) 106, (byte) -59, (byte) 39}).equals(BufferPipelineConfig.inventorySource) && client != null && client.player != null) {
            client.setScreen(new InventoryScreen(client.player));
            openedByRefill = true;
            timer = 2;
        } else {
            openedByRefill = false;
            timer = 1;
        }
        state = State.REFILL_WAIT_OPEN;
    }

    private void finishRefill(MinecraftClient client) {
        CombatLockManager.setLock(CombatLockManager.INVENTORY_ACTION, false);
        if (client != null && openedByRefill && client.currentScreen instanceof InventoryScreen) {
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
        state = State.IDLE;
        awaitingHealAfterPop = false;
    }

    public int getDesignatedCrystalSlot(ClientPlayerEntity player) {
        if (BufferPipelineConfig.refillSlot >= 0 && BufferPipelineConfig.refillSlot < 9) {
            return BufferPipelineConfig.refillSlot;
        }
        if (player != null) {
            ItemStack s8 = player.getInventory().getStack(8);
            if (s8.isOf(Items.TOTEM_OF_UNDYING) || s8.isEmpty()) {
                return 8;
            }
            for (int i = 8; i >= 0; i--) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING) || stack.isEmpty()) {
                    return i;
                }
            }
        }
        return 8;
    }

    private static int getTotemCost(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) return 9999;
        int cost = 0;
        if (stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) cost += 100;
        if (stack.hasEnchantments()) cost += 200;
        if (stack.hasGlint()) cost += 50;
        return cost;
    }

    private int findBestTotemSlot(ClientPlayerEntity player, boolean includeHotbar) {
        if (player == null) return -1;
        int bestSlot = -1;
        int lowestCost = 99999;

        if (Obf.s(new byte[]{(byte) 110, (byte) -76, (byte) 121, (byte) -50, (byte) 50, (byte) -122}).equals(BufferPipelineConfig.inventorySource)) {
            if (!includeHotbar) {
                return -1;
            }
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    int cost = getTotemCost(stack);
                    if (cost < lowestCost) {
                        lowestCost = cost;
                        bestSlot = i;
                    }
                }
            }
            return bestSlot;
        }

        if (Obf.s(new byte[]{(byte) 106, (byte) -66, (byte) 106, (byte) -59, (byte) 39}).equals(BufferPipelineConfig.inventorySource) && includeHotbar) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    int cost = getTotemCost(stack);
                    if (cost < lowestCost) {
                        lowestCost = cost;
                        bestSlot = i;
                    }
                }
            }
            if (bestSlot >= 0) {
                return bestSlot;
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                int cost = getTotemCost(stack);
                if (cost < lowestCost) {
                    lowestCost = cost;
                    bestSlot = i;
                }
            }
        }

        if (includeHotbar && bestSlot < 0) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    int cost = getTotemCost(stack);
                    if (cost < lowestCost) {
                        lowestCost = cost;
                        bestSlot = i;
                    }
                }
            }
        }

        return bestSlot;
    }

    private int findHotbarTotem(ClientPlayerEntity player) {
        if (player == null) return -1;

        if (BufferPipelineConfig.mode == 3) {
            int crystalSlot = getDesignatedCrystalSlot(player);
            if (crystalSlot >= 0 && player.getInventory().getStack(crystalSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                return crystalSlot;
            }
        }

        if (BufferPipelineConfig.refillSlot >= 0 && BufferPipelineConfig.refillSlot < 9) {
            if (player.getInventory().getStack(BufferPipelineConfig.refillSlot).isOf(Items.TOTEM_OF_UNDYING)) {
                return BufferPipelineConfig.refillSlot;
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
        return findBestTotemSlot(player, false);
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
        if (BufferPipelineConfig.mode == 3) {
            return getDesignatedCrystalSlot(player);
        }

        if (BufferPipelineConfig.refillSlot >= 0 && BufferPipelineConfig.refillSlot < 9) {
            return BufferPipelineConfig.refillSlot;
        }

        if (preferredSlot >= 0 && preferredSlot < 9) {
            return preferredSlot;
        }
        if (lastTotemHotbarSlot >= 0 && lastTotemHotbarSlot < 9) {
            return lastTotemHotbarSlot;
        }

        if (player == null) return -1;

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
        timer = 0;
        if (openedByRefill) {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c != null && c.currentScreen instanceof InventoryScreen) {
                if (c.player != null) {
                    c.player.closeHandledScreen();
                }
                c.setScreen(null);
            }
            openedByRefill = false;
        }
        refillTargetHotbarSlot = -1;
        refillInvSlot = -1;
        pendingOffhandTicks = 0;
        releaseTotemLease(false);
    }

    public int resolveReturnSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) return 0;

        int arbiterSlot = SlotArbiter.getLastUserSelectedSlot();
        if (arbiterSlot >= 0 && arbiterSlot < 9) {
            ItemStack s = player.getInventory().getStack(arbiterSlot);
            if (!s.isEmpty() && !s.isOf(Items.TOTEM_OF_UNDYING)) {
                return arbiterSlot;
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

        if (savedMainSlot >= 0 && savedMainSlot < 9) {
            ItemStack s = player.getInventory().getStack(savedMainSlot);
            if (!s.isEmpty() && !s.isOf(Items.TOTEM_OF_UNDYING)) {
                return savedMainSlot;
            }
        }

        int sword = activity.client.module.service.InventoryScanService.findSwordSlot(player);
        if (sword >= 0 && sword < 9) return sword;
        int axe = activity.client.module.service.InventoryScanService.findAxeSlot(player);
        if (axe >= 0 && axe < 9) return axe;

        return 0;
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

    public State getState() {
        return this.state;
    }

    public void setStateForTest(State s) {
        this.state = s;
    }

    public int getRefillTargetHotbarSlot() {
        return this.refillTargetHotbarSlot;
    }

    public void setRefillTargetHotbarSlotForTest(int slot) {
        this.refillTargetHotbarSlot = slot;
    }

    public int getSwappedHotbarSlot() {
        return this.swappedHotbarSlot;
    }

    public void setSwappedHotbarSlotForTest(int slot) {
        this.swappedHotbarSlot = slot;
    }

    public int getHeldTotemHotbarSlot() {
        return this.heldTotemHotbarSlot;
    }

    public void setHeldTotemHotbarSlotForTest(int slot) {
        this.heldTotemHotbarSlot = slot;
    }

    public int getLastTotemHotbarSlot() {
        return this.lastTotemHotbarSlot;
    }

    public void setLastTotemHotbarSlotForTest(int slot) {
        this.lastTotemHotbarSlot = slot;
    }

    public int resolveRefillTargetSlotForTest(ClientPlayerEntity player, int preferredSlot) {
        return resolveRefillTargetSlot(player, preferredSlot);
    }

    public void startRefillForTest(MinecraftClient client, int targetHotbar) {
        startRefill(client, targetHotbar);
    }

    public void finishRefillForTest(MinecraftClient client) {
        finishRefill(client);
    }

    public enum State {
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
