package dev.lighting;

import dev.buffer.DamageForecast;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.fabricmc.pack.api.SlotArbiter;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.EnumSet;
import java.util.Locale;

public final class LightmapFilterController {
    private static final double REACH_SAFETY_MARGIN = 0.15D;

    private State state = State.IDLE;
    private SlotArbiter.Lease lease = null;
    private int originalSlot = -1;
    private int glowSlot = -1;
    private boolean glowInOffhand = false;
    private BlockPos targetPos = null;
    private BlockHitResult lastHit = null;
    private int timer = 0;
    private int lastObservedCharges = 0;
    private long lastActionTime = 0L;
    private long nextActionTime = 0L;
    private final java.util.Map<BlockPos, Long> anchorFirstSeen = new java.util.HashMap<>();

    private BlockPos trackedPosition = null;
    private int interactionCount = 0;
    private BlockPos pendingExplosionPos = null;
    private long pendingExplosionTime = 0L;

    private DoubleState doubleState = DoubleState.IDLE;
    private BlockPos doubleTargetPos = null;
    private BlockHitResult doubleLastHit = null;
    private int doubleOriginalSlot = -1;
    private int doubleGlowSlot = -1;
    private boolean doubleGlowInOffhand = false;
    private boolean doubleIsChain = false;
    private int doubleTimer = 0;
    private long doubleNextActionTime = 0L;
    private int doubleLastObservedCharges = 0;

    private static int lastAnchorOriginalSlot = -1;
    private static long lastAnchorOriginalTime = 0L;

    public static int getLastAnchorOriginalSlot() {
        if (System.currentTimeMillis() - lastAnchorOriginalTime < 5000L) {
            return lastAnchorOriginalSlot;
        }
        return -1;
    }

    public static void recordAnchorOriginalSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            lastAnchorOriginalSlot = slot;
            lastAnchorOriginalTime = System.currentTimeMillis();
        }
    }

    public int getOriginalSlot() {
        if (originalSlot >= 0 && originalSlot < 9) return originalSlot;
        if (doubleOriginalSlot >= 0 && doubleOriginalSlot < 9) return doubleOriginalSlot;
        return -1;
    }

    public LightmapFilterController() {
    }

    public boolean isEnabled() {
        return LightmapFilterConfig.enabled;
    }

    public void toggle() {
        LightmapFilterConfig.enabled = !LightmapFilterConfig.enabled;
        LightmapFilterConfig.save();
        if (!LightmapFilterConfig.enabled) {
            cancelState(MinecraftClient.getInstance());
        }
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            reset(client);
            return;
        }

        if (!LightmapFilterConfig.enabled || !client.player.isAlive()) {
            reset(client);
            return;
        }

        if ((state != State.IDLE || doubleState != DoubleState.IDLE) && lease != null && !lease.isActive()) {
            cancelState(client);
            return;
        }

        if ("double".equalsIgnoreCase(LightmapFilterConfig.mode)) {
            tickDoubleAnchor(client);
            return;
        }

        tickSmartAuto(client);
    }

    private void tickDoubleAnchor(MinecraftClient client) {
        if (client.currentScreen != null) {
            cancelDoubleState(client);
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            cancelDoubleState(client);
            return;
        }

        if (isBusy(client, player) || isShiftPressed(client)) {
            cancelDoubleState(client);
            return;
        }

        long now = System.currentTimeMillis();

        if (doubleState != DoubleState.IDLE && !isRightClickPressed(client)) {
            if (doubleState != DoubleState.WAITING_SERVER_CHARGE && doubleState != DoubleState.WAITING_DETONATE
                && doubleState != DoubleState.WAITING_CHAIN_SERVER_CHARGE && doubleState != DoubleState.WAITING_CHAIN_DETONATE) {
                cancelDoubleState(client);
                return;
            }
        }

        switch (doubleState) {
            case IDLE -> {
                if (!isRightClickPressed(client)) {
                    return;
                }
                if (lease == null && SlotArbiter.isResourceLocked(SlotArbiter.Resource.HOTBAR_SELECT, "auto_anchor")) {
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
                    return;
                }

                BlockPos hitPos = hit.getBlockPos();
                Vec3d eyePos = player.getEyePos();
                Vec3d hitVec = hit.getPos();
                double reach = Math.min(player.getBlockInteractionRange() - REACH_SAFETY_MARGIN, 4.20D);
                if (eyePos.squaredDistanceTo(hitVec) > reach * reach) {
                    return;
                }
                if (!hasLineOfSight(client, eyePos, hitVec, hitPos)) {
                    return;
                }

                BlockState hitState = client.world.getBlockState(hitPos);
                if (hitState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    if (lease == null || !lease.isActive()) {
                        lease = SlotArbiter.acquire("auto_anchor", SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), 50, false);
                        if (lease == null) {
                            return;
                        }
                    }
                    doubleOriginalSlot = player.getInventory().getSelectedSlot();
                    recordAnchorOriginalSlot(doubleOriginalSlot);
                    doubleTargetPos = hitPos;
                    doubleLastHit = hit;
                    doubleIsChain = false;
                    doubleGlowInOffhand = player.getOffHandStack().isOf(Items.GLOWSTONE);
                    doubleGlowSlot = -1;
                    if (!doubleGlowInOffhand) {
                        doubleGlowSlot = findGlowstoneSlot(player);
                    }

                    int charges = hitState.get(RespawnAnchorBlock.CHARGES);
                    if (charges <= 0) {
                        if (!doubleGlowInOffhand && doubleGlowSlot < 0) {
                            return;
                        }
                        if (!doubleGlowInOffhand && player.getInventory().getSelectedSlot() != doubleGlowSlot) {
                            selectSlot(client, doubleGlowSlot);
                        }
                        interactDoubleGlowstone(client, now);
                    } else if (LightmapFilterConfig.doubleAutoExplode) {
                        int detSlot = resolveDetonateSlot(player, doubleOriginalSlot, doubleGlowSlot);
                        if (detSlot >= 0 && player.getInventory().getSelectedSlot() != detSlot) {
                            selectSlot(client, detSlot);
                        }
                        interactDoubleDetonate(client, now);
                    }
                } else {
                    BlockPos placePos = hitState.isReplaceable() ? hitPos : hitPos.offset(hit.getSide());
                    BlockState placeState = client.world.getBlockState(placePos);
                    if (!placeState.isReplaceable()) {
                        return;
                    }

                    int anchorSlot = -1;
                    if (player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                        anchorSlot = player.getInventory().getSelectedSlot();
                    } else {
                        anchorSlot = findAnchorSlot(player);
                    }
                    if (anchorSlot < 0) {
                        return;
                    }

                    if (lease == null || !lease.isActive()) {
                        lease = SlotArbiter.acquire("auto_anchor", SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), 50, false);
                        if (lease == null) {
                            return;
                        }
                    }
                    doubleOriginalSlot = player.getInventory().getSelectedSlot();
                    recordAnchorOriginalSlot(doubleOriginalSlot);
                    doubleTargetPos = placePos;
                    doubleLastHit = hit;
                    doubleIsChain = false;
                    doubleGlowInOffhand = player.getOffHandStack().isOf(Items.GLOWSTONE);
                    doubleGlowSlot = -1;
                    if (!doubleGlowInOffhand) {
                        doubleGlowSlot = findGlowstoneSlot(player);
                    }

                    if (player.getInventory().getSelectedSlot() != anchorSlot) {
                        selectSlot(client, anchorSlot);
                    }

                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                    player.swingHand(Hand.MAIN_HAND);
                    doubleState = DoubleState.WAITING_PLACE;
                    doubleTimer = 10;
                    doubleNextActionTime = now + getDoubleActionDelay();
                }
            }
            case WAITING_PLACE -> {
                if (doubleTimer-- <= 0) {
                    cancelDoubleState(client);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    return;
                }
                int charges = targetState.get(RespawnAnchorBlock.CHARGES);
                if (charges <= 0) {
                    if (!doubleGlowInOffhand) {
                        doubleGlowSlot = findGlowstoneSlot(player);
                        if (doubleGlowSlot < 0) {
                            cancelDoubleState(client);
                            return;
                        }
                        if (player.getInventory().getSelectedSlot() != doubleGlowSlot) {
                            selectSlot(client, doubleGlowSlot);
                        }
                    }
                    interactDoubleGlowstone(client, now);
                } else if (LightmapFilterConfig.doubleAutoExplode) {
                    int detSlot = resolveDetonateSlot(player, doubleOriginalSlot, doubleGlowSlot);
                    if (detSlot >= 0 && player.getInventory().getSelectedSlot() != detSlot) {
                        selectSlot(client, detSlot);
                    }
                    interactDoubleDetonate(client, now);
                } else {
                    finishDoubleCycle(client, now);
                }
            }
            case WAITING_CHARGE -> {
                if (doubleTimer-- <= 0) {
                    cancelDoubleState(client);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                if (!doubleGlowInOffhand && player.getInventory().getSelectedSlot() != doubleGlowSlot) {
                    selectSlot(client, doubleGlowSlot);
                    return;
                }
                interactDoubleGlowstone(client, now);
            }
            case WAITING_SERVER_CHARGE -> {
                if (doubleTimer-- <= 0) {
                    cancelDoubleState(client);
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    cancelDoubleState(client);
                    return;
                }
                int curCharges = targetState.get(RespawnAnchorBlock.CHARGES);
                if (curCharges > doubleLastObservedCharges || curCharges >= 1) {
                    if (LightmapFilterConfig.doubleAutoExplode) {
                        int detSlot = resolveDetonateSlot(player, doubleOriginalSlot, doubleGlowSlot);
                        if (detSlot >= 0 && player.getInventory().getSelectedSlot() != detSlot) {
                            selectSlot(client, detSlot);
                        }
                        doubleState = DoubleState.WAITING_DETONATE;
                        doubleTimer = 10;
                        doubleNextActionTime = now + getDoubleActionDelay();
                    } else {
                        finishDoubleCycle(client, now);
                    }
                }
            }
            case WAITING_DETONATE -> {
                if (doubleTimer-- <= 0) {
                    cancelDoubleState(client);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR) || targetState.get(RespawnAnchorBlock.CHARGES) <= 0) {
                    cancelDoubleState(client);
                    return;
                }
                interactDoubleDetonate(client, now);
            }
            case WAITING_CHAIN_PLACE -> {
                if (doubleTimer-- <= 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                BlockState currentBlock = client.world.getBlockState(doubleTargetPos);
                if (!currentBlock.isReplaceable()) {
                    return;
                }
                int anchorSlot = findAnchorSlot(player);
                if (anchorSlot < 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                if (player.getInventory().getSelectedSlot() != anchorSlot) {
                    selectSlot(client, anchorSlot);
                }
                BlockPos supportPos = doubleTargetPos.down();
                BlockHitResult placeHit = new BlockHitResult(
                    new Vec3d(doubleTargetPos.getX() + 0.5, doubleTargetPos.getY(), doubleTargetPos.getZ() + 0.5),
                    net.minecraft.util.math.Direction.UP,
                    supportPos,
                    false
                );
                client.interactionManager.interactBlock(player, Hand.MAIN_HAND, placeHit);
                player.swingHand(Hand.MAIN_HAND);
                doubleState = DoubleState.WAITING_CHAIN_CHARGE;
                doubleTimer = 10;
                doubleNextActionTime = now + getDoubleActionDelay();
            }
            case WAITING_CHAIN_CHARGE -> {
                if (doubleTimer-- <= 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    return;
                }
                doubleGlowInOffhand = player.getOffHandStack().isOf(Items.GLOWSTONE);
                doubleGlowSlot = -1;
                if (!doubleGlowInOffhand) {
                    doubleGlowSlot = findGlowstoneSlot(player);
                    if (doubleGlowSlot < 0) {
                        finishDoubleCycle(client, now);
                        return;
                    }
                    if (player.getInventory().getSelectedSlot() != doubleGlowSlot) {
                        selectSlot(client, doubleGlowSlot);
                    }
                }
                interactDoubleGlowstone(client, now);
            }
            case WAITING_CHAIN_SERVER_CHARGE -> {
                if (doubleTimer-- <= 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    finishDoubleCycle(client, now);
                    return;
                }
                int curCharges = targetState.get(RespawnAnchorBlock.CHARGES);
                if (curCharges > doubleLastObservedCharges || curCharges >= 1) {
                    if (LightmapFilterConfig.doubleAutoExplode) {
                        int detSlot = resolveDetonateSlot(player, doubleOriginalSlot, doubleGlowSlot);
                        if (detSlot >= 0 && player.getInventory().getSelectedSlot() != detSlot) {
                            selectSlot(client, detSlot);
                        }
                        doubleState = DoubleState.WAITING_CHAIN_DETONATE;
                        doubleTimer = 10;
                        doubleNextActionTime = now + getDoubleActionDelay();
                    } else {
                        finishDoubleCycle(client, now);
                    }
                }
            }
            case WAITING_CHAIN_DETONATE -> {
                if (doubleTimer-- <= 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                if (now < doubleNextActionTime) {
                    return;
                }
                BlockState targetState = client.world.getBlockState(doubleTargetPos);
                if (!targetState.isOf(Blocks.RESPAWN_ANCHOR) || targetState.get(RespawnAnchorBlock.CHARGES) <= 0) {
                    finishDoubleCycle(client, now);
                    return;
                }
                interactDoubleDetonate(client, now);
            }
        }
    }

    private void tickSmartAuto(MinecraftClient client) {
        if (isShiftPressed(client)) {
            if (state == State.WAITING_CHARGE || state == State.SELECT_GLOW || state == State.INTERACT_GLOW) {
                cancelState(client);
                return;
            }
        }

        long now = System.currentTimeMillis();

        switch (state) {
            case WAITING_CHARGE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                if (isShiftPressed(client)) {
                    cancelState(client);
                    return;
                }
                if (timer > 0) {
                    timer--;
                }
                if (timer > 0 || now < nextActionTime) {
                    return;
                }
                if (!glowInOffhand) {
                    if (client.player.getInventory().getSelectedSlot() != glowSlot) {
                        selectSlot(client, glowSlot);
                    }
                }
                interactGlowstone(client, now);
            }
            case SELECT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos) || isShiftPressed(client)) {
                    cancelState(client);
                    return;
                }
                if (glowInOffhand) {
                    interactGlowstone(client, now);
                    return;
                }
                int foundGlow = findGlowstoneSlot(client.player);
                if (foundGlow < 0) {
                    cancelState(client);
                    return;
                }
                glowSlot = foundGlow;
                selectSlot(client, glowSlot);
                interactGlowstone(client, now);
            }
            case INTERACT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos) || isShiftPressed(client)) {
                    cancelState(client);
                    return;
                }
                if (!glowInOffhand && client.player.getInventory().getSelectedSlot() != glowSlot) {
                    selectSlot(client, glowSlot);
                    return;
                }
                if (timer > 0) {
                    timer--;
                }
                if (timer > 0 || now < nextActionTime) {
                    return;
                }
                interactGlowstone(client, now);
            }
            case WAITING_SERVER_CHARGE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                BlockState currentAnchorState = client.world.getBlockState(targetPos);
                if (!currentAnchorState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    cancelState(client);
                    return;
                }
                int curCharges = currentAnchorState.get(RespawnAnchorBlock.CHARGES);
                if (curCharges > lastObservedCharges || curCharges >= LightmapFilterConfig.targetCharges) {
                    if (curCharges < LightmapFilterConfig.targetCharges) {
                        if (isShiftPressed(client)) {
                            cancelState(client);
                            return;
                        }
                        state = State.WAITING_CHARGE;
                        timer = Math.max(1, LightmapFilterConfig.chargeDelayTicks);
                        nextActionTime = now + getActionDelay(LightmapFilterConfig.chargeDelayTicks);
                    } else if (LightmapFilterConfig.autoExplode) {
                        state = State.WAITING_DETONATE;
                        timer = Math.max(1, LightmapFilterConfig.explodeDelayTicks);
                        nextActionTime = now + getActionDelay(LightmapFilterConfig.explodeDelayTicks);
                    } else {
                        if (LightmapFilterConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                            state = State.WAITING_RETURN;
                            timer = 1;
                            nextActionTime = now + getActionDelay(0);
                        } else {
                            reset(client);
                        }
                    }
                    return;
                }
                if (timer > 0) {
                    timer--;
                    return;
                }
                cancelState(client);
            }
            case WAITING_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                if (timer > 0) {
                    timer--;
                }
                if (timer > 0 || now < nextActionTime) {
                    return;
                }
                int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                if (detSlot < 0) {
                    cancelState(client);
                    return;
                }
                if (client.player.getInventory().getSelectedSlot() != detSlot) {
                    selectSlot(client, detSlot);
                }
                interactDetonate(client, now);
            }
            case SELECT_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                if (detSlot < 0) {
                    cancelState(client);
                    return;
                }
                if (client.player.getInventory().getSelectedSlot() != detSlot) {
                    selectSlot(client, detSlot);
                }
                interactDetonate(client, now);
            }
            case INTERACT_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                if (timer > 0) {
                    timer--;
                }
                if (timer > 0 || now < nextActionTime) {
                    return;
                }
                interactDetonate(client, now);
            }
            case WAITING_RETURN -> {
                if (timer > 0) {
                    timer--;
                }
                if (timer > 0 || now < nextActionTime) {
                    return;
                }
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen) {
                    return;
                }
                if (LightmapFilterConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    selectSlot(client, originalSlot);
                }
                reset(client);
            }
            case RETURN_SLOT -> {
                if (LightmapFilterConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    selectSlot(client, originalSlot);
                }
                reset(client);
            }
            case IDLE -> {
                if (now - lastActionTime < getMinCycleIntervalMs()) {
                    return;
                }
                if (isBusy(client, client.player)) {
                    return;
                }
                if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
                    return;
                }
                BlockPos pos = hit.getBlockPos();
                if (pos.equals(pendingExplosionPos)) {
                    BlockState st = client.world.getBlockState(pos);
                    if (now - pendingExplosionTime < 800L && st.isOf(Blocks.RESPAWN_ANCHOR) && st.get(RespawnAnchorBlock.CHARGES) > 0) {
                        return;
                    }
                    pendingExplosionPos = null;
                }
                BlockState blockState = client.world.getBlockState(pos);
                if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
                    return;
                }

                anchorFirstSeen.entrySet().removeIf(e -> now - e.getValue() > 15000L);
                Long firstSeen = anchorFirstSeen.get(pos);
                if (firstSeen == null) {
                    anchorFirstSeen.put(pos, now);
                } else if (now - firstSeen > 5000L) {
                    return;
                }

                if (LightmapFilterConfig.chance < 100) {
                    int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                    if (roll >= LightmapFilterConfig.chance) {
                        lastActionTime = now + 50L;
                        return;
                    }
                }
                Vec3d eyePos = client.player.getEyePos();
                Vec3d hitPos = hit.getPos();
                double reach = Math.min(client.player.getBlockInteractionRange() - REACH_SAFETY_MARGIN, 4.20D);
                if (eyePos.squaredDistanceTo(hitPos) > reach * reach) {
                    return;
                }
                if (!hasLineOfSight(client, eyePos, hitPos, pos)) {
                    return;
                }

                int charges = blockState.get(RespawnAnchorBlock.CHARGES);
                if (charges < LightmapFilterConfig.targetCharges) {
                    if (isShiftPressed(client)) {
                        return;
                    }
                    if (lease == null && SlotArbiter.isResourceLocked(SlotArbiter.Resource.HOTBAR_SELECT, "auto_anchor")) {
                        return;
                    }
                    glowInOffhand = client.player.getOffHandStack().isOf(Items.GLOWSTONE);
                    glowSlot = -1;
                    if (!glowInOffhand) {
                        glowSlot = findGlowstoneSlot(client.player);
                        if (glowSlot < 0) {
                            return;
                        }
                    }
                    if (lease == null || !lease.isActive()) {
                        lease = SlotArbiter.acquire("auto_anchor", SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), 50, false);
                        if (lease == null) {
                            return;
                        }
                    }
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    recordAnchorOriginalSlot(originalSlot);
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    state = State.WAITING_CHARGE;
                    timer = Math.max(1, LightmapFilterConfig.chargeDelayTicks);
                    nextActionTime = now + getActionDelay(LightmapFilterConfig.chargeDelayTicks);
                } else if (LightmapFilterConfig.autoExplode) {
                    if (lease == null && SlotArbiter.isResourceLocked(SlotArbiter.Resource.HOTBAR_SELECT, "auto_anchor")) {
                        return;
                    }
                    if (lease == null || !lease.isActive()) {
                        lease = SlotArbiter.acquire("auto_anchor", SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), 50, false);
                        if (lease == null) {
                            return;
                        }
                    }
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    recordAnchorOriginalSlot(originalSlot);
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    state = State.WAITING_DETONATE;
                    timer = Math.max(1, LightmapFilterConfig.explodeDelayTicks);
                    nextActionTime = now + getActionDelay(LightmapFilterConfig.explodeDelayTicks);
                }
            }
        }
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client == null || client.player == null || slot < 0 || slot >= 9) return;
        if (lease != null && lease.isActive()) {
            SlotArbiter.selectSlot(client, lease, slot);
        } else {
            SafeSlotManager.selectSlot(client, slot);
        }
    }

    private void interactGlowstone(MinecraftClient client, long now) {
        if (targetPos == null || client.world == null || client.interactionManager == null) return;
        BlockState preState = client.world.getBlockState(targetPos);
        lastObservedCharges = preState.isOf(Blocks.RESPAWN_ANCHOR)
            ? preState.get(RespawnAnchorBlock.CHARGES)
            : 0;

        BlockHitResult hitToUse = lastHit;
        if (hitToUse == null) {
            hitToUse = new BlockHitResult(
                new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5),
                net.minecraft.util.math.Direction.UP,
                targetPos,
                false
            );
        }

        Hand handToUse = glowInOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        client.interactionManager.interactBlock(client.player, handToUse, hitToUse);
        client.player.swingHand(handToUse);
        lastActionTime = now;
        state = State.WAITING_SERVER_CHARGE;
        timer = 10;
    }

    private void interactDetonate(MinecraftClient client, long now) {
        if (targetPos == null || client.world == null || client.interactionManager == null) return;
        DamageForecast.publishIntent("auto_anchor", 12.0F, 5);
        BlockHitResult hitToUse = lastHit;
        if (hitToUse == null) {
            hitToUse = new BlockHitResult(
                new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5),
                net.minecraft.util.math.Direction.UP,
                targetPos,
                false
            );
        }
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitToUse);
        client.player.swingHand(Hand.MAIN_HAND);
        activity.client.module.impl.utility.AudioWaveTracker.recordExplosion(
            targetPos.getX() + 0.5,
            targetPos.getY() + 0.5,
            targetPos.getZ() + 0.5,
            8.5
        );
        pendingExplosionPos = targetPos;
        pendingExplosionTime = now;
        lastActionTime = now;
        if (LightmapFilterConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
            state = State.WAITING_RETURN;
            timer = 1;
            nextActionTime = now + getActionDelay(0);
        } else {
            reset(client);
        }
    }

    private boolean isShiftPressed(MinecraftClient client) {
        if (client == null || client.player == null) return false;
        return (client.options != null && client.options.sneakKey.isPressed()) || client.player.isSneaking();
    }

    private boolean isRightClickPressed(MinecraftClient client) {
        if (client == null) return false;
        if (client.getWindow() != null && client.getWindow().getHandle() != 0L) {
            try {
                if (GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return client.options != null && client.options.useKey != null && client.options.useKey.isPressed();
    }

    public long getMinCycleIntervalMs() {
        String p = LightmapFilterConfig.preset != null ? LightmapFilterConfig.preset.toUpperCase(Locale.ROOT) : "BALANCED";
        return switch (p) {
            case "FAST" -> GaussianTimingEngine.getDelay(45.0D, 5.0D, 35L, 55L);
            case "MEDIUM" -> GaussianTimingEngine.getDelay(77.0D, 7.0D, 65L, 90L);
            case "SAFE" -> GaussianTimingEngine.getDelay(140.0D, 10.0D, 120L, 160L);
            default -> GaussianTimingEngine.getDelay(85.0D, 8.0D, 70L, 100L);
        };
    }

    public long getActionDelay(int delayTicks) {
        String p = LightmapFilterConfig.preset != null ? LightmapFilterConfig.preset.toUpperCase(Locale.ROOT) : "BALANCED";
        long delayMs = switch (p) {
            case "FAST" -> GaussianTimingEngine.getDelay(45.0D, 5.0D, 35L, 55L);
            case "MEDIUM" -> GaussianTimingEngine.getDelay(77.0D, 7.0D, 65L, 90L);
            case "SAFE" -> GaussianTimingEngine.getDelay(140.0D, 10.0D, 120L, 160L);
            default -> GaussianTimingEngine.getDelay(85.0D, 8.0D, 70L, 100L);
        };
        if (delayTicks > 0) {
            delayMs = Math.max(delayMs, delayTicks * 50L + GaussianTimingEngine.getDelay(0.0D, 5.0D, -10L, 10L));
        }
        return delayMs;
    }

    private boolean isLookingAtAnchor(MinecraftClient client, BlockPos pos) {
        if (pos == null || client.player == null || client.world == null) {
            return false;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (!hit.getBlockPos().equals(pos)) {
            return false;
        }
        BlockState blockState = client.world.getBlockState(pos);
        if (!blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        Vec3d eyePos = client.player.getEyePos();
        Vec3d hitPos = hit.getPos();
        double reach = Math.min(client.player.getBlockInteractionRange() - REACH_SAFETY_MARGIN, 4.20D);
        if (eyePos.squaredDistanceTo(hitPos) > reach * reach) {
            return false;
        }
        if (!hasLineOfSight(client, eyePos, hitPos, pos)) {
            return false;
        }
        lastHit = hit;
        return true;
    }

    private boolean hasLineOfSight(MinecraftClient client, Vec3d eyePos, Vec3d hitPos, BlockPos blockPos) {
        BlockHitResult hit = client.world.raycast(new RaycastContext(
            eyePos,
            hitPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return hit.getBlockPos().equals(blockPos);
    }

    public int resolveDetonateSlot(ClientPlayerEntity player, int origSlot, int gSlot) {
        if (player == null || player.getInventory() == null) return 0;
        return resolveDetonateSlot(player.getInventory(), origSlot, gSlot);
    }

    public int resolveDetonateSlot(PlayerInventory inventory, int origSlot, int gSlot) {
        if (inventory == null) return 0;

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = inventory.getStack(i);
            if (isWeaponItem(stack)) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }

        if (origSlot >= 0 && origSlot < 9 && origSlot != gSlot) {
            ItemStack stack = inventory.getStack(origSlot);
            if (!stack.isOf(Items.RESPAWN_ANCHOR) && !stack.isOf(Items.GLOWSTONE) && isSafeDetonateItem(stack)) {
                return origSlot;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = inventory.getStack(i);
            if (!stack.isOf(Items.RESPAWN_ANCHOR) && !stack.isOf(Items.GLOWSTONE) && isSafeDetonateItem(stack)) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = inventory.getStack(i);
            if (!stack.isOf(Items.RESPAWN_ANCHOR) && !stack.isOf(Items.GLOWSTONE)) {
                return i;
            }
        }

        return -1;
    }

    public boolean isWeaponItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)) {
            return true;
        }
        if (stack.isOf(Items.MACE)) return true;
        return isWeaponName(stack.getItem().toString());
    }

    public static boolean isWeaponName(String name) {
        if (name == null || name.isEmpty()) return false;
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("sword") || n.contains("mace") || n.contains("меч") || n.contains("булава")) {
            return true;
        }
        if (n.contains("pickaxe") || n.contains("кирка")) {
            return false;
        }
        return n.endsWith("_axe") || n.equals("axe") || n.contains("топор")
            || (n.contains("axe") && !n.contains("pickaxe"));
    }

    public boolean isSafeDetonateItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        if (stack.isOf(Items.RESPAWN_ANCHOR)) return false;
        if (stack.isOf(Items.GLOWSTONE)) return false;
        if (stack.getItem() instanceof net.minecraft.item.BlockItem) return false;
        if (stack.getItem() instanceof ShieldItem) return false;
        if (stack.isOf(Items.BOW) || stack.isOf(Items.CROSSBOW) || stack.isOf(Items.TRIDENT)) return false;
        if (isActionItem(stack)) return false;
        return isSafeDetonateName(stack.getItem().toString());
    }

    public static boolean isSafeDetonateName(String name) {
        if (name == null || name.isEmpty()) return true;
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("anchor") || n.contains("якорь")) return false;
        if (n.contains("glowstone") || n.contains("светокамень")) return false;
        if (n.contains("shield") || n.contains("щит")) return false;
        if (n.contains("bow") || n.contains("лук") || n.contains("trident") || n.contains("трезубец")) return false;
        if (n.contains("block") || n.contains("obsidian") || n.contains("dirt") || n.contains("stone") || n.contains("tnt")) return false;
        return true;
    }

    private int findGlowstoneSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = activity.client.module.service.InventoryScanService.findGlowstoneSlot(player);
        if (cached >= 0) return cached;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBusy(MinecraftClient client, ClientPlayerEntity player) {
        if (player == null) return true;
        return activity.client.module.service.PlayerStateService.isBusy(player);
    }

    private static boolean isActionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShieldItem) return true;
        UseAction action = stack.getUseAction();
        return action != null && action != UseAction.NONE;
    }

    private void interactDoubleGlowstone(MinecraftClient client, long now) {
        if (doubleTargetPos == null || client.world == null || client.interactionManager == null) return;
        BlockState preState = client.world.getBlockState(doubleTargetPos);
        doubleLastObservedCharges = preState.isOf(Blocks.RESPAWN_ANCHOR)
            ? preState.get(RespawnAnchorBlock.CHARGES)
            : 0;

        BlockHitResult hitToUse = doubleLastHit;
        if (hitToUse == null || !hitToUse.getBlockPos().equals(doubleTargetPos)) {
            hitToUse = new BlockHitResult(
                new Vec3d(doubleTargetPos.getX() + 0.5, doubleTargetPos.getY() + 0.5, doubleTargetPos.getZ() + 0.5),
                net.minecraft.util.math.Direction.UP,
                doubleTargetPos,
                false
            );
        }

        Hand handToUse = doubleGlowInOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
        client.interactionManager.interactBlock(client.player, handToUse, hitToUse);
        client.player.swingHand(handToUse);

        if (doubleIsChain) {
            doubleState = DoubleState.WAITING_CHAIN_SERVER_CHARGE;
        } else {
            doubleState = DoubleState.WAITING_SERVER_CHARGE;
        }
        doubleTimer = 10;
        doubleNextActionTime = now + getDoubleActionDelay();
    }

    private void interactDoubleDetonate(MinecraftClient client, long now) {
        if (doubleTargetPos == null || client.world == null || client.interactionManager == null) return;
        DamageForecast.publishIntent("auto_anchor", 12.0F, 5);
        BlockHitResult hitToUse = doubleLastHit;
        if (hitToUse == null || !hitToUse.getBlockPos().equals(doubleTargetPos)) {
            hitToUse = new BlockHitResult(
                new Vec3d(doubleTargetPos.getX() + 0.5, doubleTargetPos.getY() + 0.5, doubleTargetPos.getZ() + 0.5),
                net.minecraft.util.math.Direction.UP,
                doubleTargetPos,
                false
            );
        }

        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitToUse);
        client.player.swingHand(Hand.MAIN_HAND);
        activity.client.module.impl.utility.AudioWaveTracker.recordExplosion(
            doubleTargetPos.getX() + 0.5,
            doubleTargetPos.getY() + 0.5,
            doubleTargetPos.getZ() + 0.5,
            8.5
        );

        if (!doubleIsChain && LightmapFilterConfig.doubleChain) {
            doubleIsChain = true;
            doubleState = DoubleState.WAITING_CHAIN_PLACE;
            doubleTimer = 10;
            doubleNextActionTime = now + getDoubleActionDelay();
        } else {
            finishDoubleCycle(client, now);
        }
    }

    private void finishDoubleCycle(MinecraftClient client, long now) {
        if (lease != null) {
            SlotArbiter.release(lease, LightmapFilterConfig.autoReturn);
            lease = null;
        } else if (LightmapFilterConfig.autoReturn && doubleOriginalSlot >= 0 && doubleOriginalSlot < 9) {
            if (client != null && client.player != null) {
                SafeSlotManager.selectSlot(client, doubleOriginalSlot);
            }
        }
        resetDoubleState();
        doubleNextActionTime = now + getDoubleActionDelay();
    }

    public void cancelDoubleState(MinecraftClient client) {
        if (lease != null) {
            SlotArbiter.release(lease, LightmapFilterConfig.autoReturn);
            lease = null;
        } else if (LightmapFilterConfig.autoReturn && doubleOriginalSlot >= 0 && doubleOriginalSlot < 9) {
            if (client != null && client.player != null) {
                SafeSlotManager.selectSlot(client, doubleOriginalSlot);
            }
        }
        resetDoubleState();
    }

    private void resetDoubleState() {
        doubleState = DoubleState.IDLE;
        doubleTargetPos = null;
        doubleLastHit = null;
        doubleOriginalSlot = -1;
        doubleGlowSlot = -1;
        doubleGlowInOffhand = false;
        doubleIsChain = false;
        doubleTimer = 0;
        doubleLastObservedCharges = 0;
    }

    public int findAnchorSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }
        return -1;
    }

    public long getDoubleActionDelay() {
        String p = LightmapFilterConfig.presetDouble != null ? LightmapFilterConfig.presetDouble.toLowerCase(Locale.ROOT) : "fast";
        return switch (p) {
            case "fast" -> GaussianTimingEngine.getDelay(40.0D, 6.0D, 30L, 55L);
            case "legit" -> GaussianTimingEngine.getDelay(75.0D, 12.0D, 60L, 100L);
            case "custom" -> {
                double base = Math.max(15.0, LightmapFilterConfig.doubleDelayTicks * 50.0);
                yield GaussianTimingEngine.getDelay(base, Math.max(5.0, base * 0.15), (long)Math.max(10.0, base * 0.7), (long)(base * 1.35));
            }
            default -> GaussianTimingEngine.getDelay(40.0D, 6.0D, 30L, 55L);
        };
    }

    public void cancelState(MinecraftClient client) {
        cancelDoubleState(client);
        if (lease != null) {
            SlotArbiter.release(lease, LightmapFilterConfig.autoReturn);
            lease = null;
        } else if (LightmapFilterConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
            SafeSlotManager.selectSlot(client, originalSlot);
        }
        reset(client);
    }

    public void reset(MinecraftClient client) {
        if (lease != null) {
            SlotArbiter.release(lease, false);
            lease = null;
        }
        resetDoubleState();
        state = State.IDLE;
        originalSlot = -1;
        glowSlot = -1;
        glowInOffhand = false;
        targetPos = null;
        lastHit = null;
        timer = 0;
        lastObservedCharges = 0;
        nextActionTime = 0L;
        trackedPosition = null;
        interactionCount = 0;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", false);
    }

    private enum DoubleState {
        IDLE,
        WAITING_PLACE,
        WAITING_CHARGE,
        WAITING_SERVER_CHARGE,
        WAITING_DETONATE,
        WAITING_CHAIN_PLACE,
        WAITING_CHAIN_CHARGE,
        WAITING_CHAIN_SERVER_CHARGE,
        WAITING_CHAIN_DETONATE
    }

    private enum State {
        IDLE,
        WAITING_CHARGE,
        SELECT_GLOW,
        INTERACT_GLOW,
        WAITING_SERVER_CHARGE,
        WAITING_DETONATE,
        SELECT_DETONATE,
        INTERACT_DETONATE,
        WAITING_RETURN,
        RETURN_SLOT
    }
}
