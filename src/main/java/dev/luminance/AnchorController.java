package dev.luminance;

import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Locale;

public final class AnchorController {
    private static final double REACH_SAFETY_MARGIN = 0.15D;

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int glowSlot = -1;
    private BlockPos targetPos = null;
    private BlockHitResult lastHit = null;
    private int timer = 0;
    private int lastObservedCharges = 0;
    private long lastActionTime = 0L;
    private long nextActionTime = 0L;
    private final java.util.Map<BlockPos, Long> anchorFirstSeen = new java.util.HashMap<>();

    public AnchorController() {
    }

    public boolean isEnabled() {
        return AnchorConfig.enabled;
    }

    public void toggle() {
        AnchorConfig.enabled = !AnchorConfig.enabled;
        AnchorConfig.save();
        if (!AnchorConfig.enabled) {
            cancelState(MinecraftClient.getInstance());
        }
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            reset(client);
            return;
        }

        if (!AnchorConfig.enabled || !client.player.isAlive()) {
            reset(client);
            return;
        }

        // When Shift is held down, auto-refill of anchor with glowstone must NOT work
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
                    return;
                }
                if (now < nextActionTime) {
                    return;
                }
                if (client.player.getInventory().getSelectedSlot() != glowSlot) {
                    SafeSlotManager.selectSlot(client, glowSlot);
                }
                interactGlowstone(client, now);
            }
            case SELECT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos) || isShiftPressed(client)) {
                    cancelState(client);
                    return;
                }
                int foundGlow = findGlowstoneSlot(client.player);
                if (foundGlow < 0) {
                    cancelState(client);
                    return;
                }
                glowSlot = foundGlow;
                SafeSlotManager.selectSlot(client, glowSlot);
                interactGlowstone(client, now);
            }
            case INTERACT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos) || isShiftPressed(client)) {
                    cancelState(client);
                    return;
                }
                if (client.player.getInventory().getSelectedSlot() != glowSlot) {
                    SafeSlotManager.selectSlot(client, glowSlot);
                    return;
                }
                if (timer > 0) {
                    timer--;
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
                if (curCharges > lastObservedCharges || curCharges >= AnchorConfig.targetCharges) {
                    if (curCharges < AnchorConfig.targetCharges) {
                        // More charges needed
                        if (isShiftPressed(client)) {
                            cancelState(client);
                            return;
                        }
                        int baseDelay = Math.max(0, AnchorConfig.chargeDelayTicks);
                        if (baseDelay <= 0 || isFast()) {
                            if (client.player.getInventory().getSelectedSlot() != glowSlot) {
                                SafeSlotManager.selectSlot(client, glowSlot);
                            }
                            interactGlowstone(client, now);
                        } else {
                            state = State.WAITING_CHARGE;
                            timer = baseDelay;
                            nextActionTime = now + getJitterDelay(baseDelay);
                        }
                    } else if (AnchorConfig.autoExplode) {
                        int baseDelay = Math.max(0, AnchorConfig.explodeDelayTicks);
                        int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                        SafeSlotManager.selectSlot(client, detSlot);
                        if (baseDelay <= 0 || isFast()) {
                            interactDetonate(client, now);
                        } else {
                            state = State.WAITING_DETONATE;
                            timer = baseDelay;
                            nextActionTime = now + getJitterDelay(baseDelay);
                        }
                    } else {
                        if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                            state = State.WAITING_RETURN;
                            timer = isFast() ? 0 : 1;
                            if (timer == 0) {
                                SafeSlotManager.selectSlot(client, originalSlot);
                                reset(client);
                            }
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
                    return;
                }
                if (now < nextActionTime) {
                    return;
                }
                int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                if (client.player.getInventory().getSelectedSlot() != detSlot) {
                    SafeSlotManager.selectSlot(client, detSlot);
                }
                interactDetonate(client, now);
            }
            case SELECT_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                SafeSlotManager.selectSlot(client, detSlot);
                interactDetonate(client, now);
            }
            case INTERACT_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                if (timer > 0) {
                    timer--;
                    return;
                }
                interactDetonate(client, now);
            }
            case WAITING_RETURN -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    SafeSlotManager.selectSlot(client, originalSlot);
                }
                reset(client);
            }
            case RETURN_SLOT -> {
                if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    SafeSlotManager.selectSlot(client, originalSlot);
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

                if (AnchorConfig.chance < 100) {
                    int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
                    if (roll >= AnchorConfig.chance) {
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
                if (charges < AnchorConfig.targetCharges) {
                    // Refilling with glowstone: disabled when Shift is pressed
                    if (isShiftPressed(client)) {
                        return;
                    }
                    int foundGlow = findGlowstoneSlot(client.player);
                    if (foundGlow < 0) {
                        return;
                    }
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    glowSlot = foundGlow;
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    int baseDelay = Math.max(0, AnchorConfig.chargeDelayTicks);
                    SafeSlotManager.selectSlot(client, glowSlot);
                    if (baseDelay <= 0 || isFast()) {
                        interactGlowstone(client, now);
                    } else {
                        state = State.WAITING_CHARGE;
                        timer = baseDelay;
                        nextActionTime = now + getJitterDelay(baseDelay);
                    }
                } else if (AnchorConfig.autoExplode) {
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    int baseDelay = Math.max(0, AnchorConfig.explodeDelayTicks);
                    int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                    SafeSlotManager.selectSlot(client, detSlot);
                    if (baseDelay <= 0 || isFast()) {
                        interactDetonate(client, now);
                    } else {
                        state = State.WAITING_DETONATE;
                        timer = baseDelay;
                        nextActionTime = now + getJitterDelay(baseDelay);
                    }
                }
            }
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

        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitToUse);
        client.player.swingHand(Hand.MAIN_HAND);
        lastActionTime = now;
        state = State.WAITING_SERVER_CHARGE;
        timer = isFast() ? 8 : 6;
    }

    private void interactDetonate(MinecraftClient client, long now) {
        if (targetPos == null || client.world == null || client.interactionManager == null) return;
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
        activity.client.module.impl.utility.AutoGGKillTracker.recordExplosion(
            targetPos.getX() + 0.5,
            targetPos.getY() + 0.5,
            targetPos.getZ() + 0.5,
            8.5
        );
        lastActionTime = now;
        if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
            state = State.WAITING_RETURN;
            timer = isFast() ? 0 : 1;
            if (timer == 0) {
                SafeSlotManager.selectSlot(client, originalSlot);
                reset(client);
            }
        } else {
            reset(client);
        }
    }

    private boolean isShiftPressed(MinecraftClient client) {
        if (client == null || client.player == null) return false;
        return (client.options != null && client.options.sneakKey.isPressed()) || client.player.isSneaking();
    }

    private long getMinCycleIntervalMs() {
        if (isFast()) return 0L;
        String p = AnchorConfig.preset != null ? AnchorConfig.preset.toUpperCase(Locale.ROOT) : "BALANCED";
        return switch (p) {
            case "FAST" -> 0L;
            case "MEDIUM" -> 25L;
            case "BALANCED" -> 40L;
            case "SAFE" -> 60L;
            default -> 40L;
        };
    }

    private long getJitterDelay(int baseDelayTicks) {
        if (baseDelayTicks <= 0 || isFast()) return 0L;
        return GaussianTimingEngine.getDelay(baseDelayTicks * 18.0D, 4.0D, 5L, 40L);
    }

    private boolean isFast() {
        return "FAST".equalsIgnoreCase(AnchorConfig.preset)
            || (AnchorConfig.chargeDelayTicks <= 0 && AnchorConfig.explodeDelayTicks <= 0);
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

    private int resolveDetonateSlot(ClientPlayerEntity player, int origSlot, int gSlot) {
        // 1. If totem in hotbar: switch to totem to detonate holding totem!
        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        // 2. If offhand already has totem, player is protected: prefer detonate with anchor
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 9; i++) {
                if (i == gSlot) continue;
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(Items.RESPAWN_ANCHOR)) {
                    return i;
                }
            }
        }

        // 3. If no totem, detonate with anchor!
        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }

        // 4. Fallback: empty hand or safe item
        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }

        if (origSlot >= 0 && origSlot < 9 && origSlot != gSlot) {
            ItemStack stack = player.getInventory().getStack(origSlot);
            if (isSafeDetonateItem(stack) || stack.isOf(Items.RESPAWN_ANCHOR)) {
                return origSlot;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (isSafeDetonateItem(stack) || stack.isOf(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }

        return origSlot >= 0 && origSlot < 9 ? origSlot : player.getInventory().getSelectedSlot();
    }

    private boolean isSafeDetonateItem(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.isOf(Items.GLOWSTONE)) return false;
        if (stack.isOf(Items.RESPAWN_ANCHOR)) return true;
        if (stack.getItem() instanceof ShieldItem) return false;
        if (stack.isOf(Items.BOW) || stack.isOf(Items.CROSSBOW) || stack.isOf(Items.TRIDENT)) return false;
        return !isActionItem(stack);
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

    private void cancelState(MinecraftClient client) {
        if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
            SafeSlotManager.selectSlot(client, originalSlot);
        }
        reset(client);
    }

    private void reset(MinecraftClient client) {
        state = State.IDLE;
        originalSlot = -1;
        glowSlot = -1;
        targetPos = null;
        lastHit = null;
        timer = 0;
        lastObservedCharges = 0;
        nextActionTime = 0L;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", false);
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
