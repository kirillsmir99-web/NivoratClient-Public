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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class AnchorController {
    private static final double REACH_SAFETY_MARGIN = 0.15D;
    private static final long MIN_CYCLE_INTERVAL_MS = 250L;

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

        long now = System.currentTimeMillis();

        switch (state) {
            case WAITING_CHARGE -> {
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
                state = State.SELECT_GLOW;
            }
            case SELECT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos)) {
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
                timer = 1;
                state = State.INTERACT_GLOW;
            }
            case INTERACT_GLOW -> {
                if (!isLookingAtAnchor(client, targetPos)) {
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
                BlockState preState = client.world.getBlockState(targetPos);
                lastObservedCharges = preState.isOf(Blocks.RESPAWN_ANCHOR)
                    ? preState.get(RespawnAnchorBlock.CHARGES)
                    : 0;

                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, lastHit);
                client.player.swingHand(Hand.MAIN_HAND);
                lastActionTime = now;
                state = State.WAITING_SERVER_CHARGE;
                timer = 8;
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
                        state = State.WAITING_CHARGE;
                        int baseDelay = Math.max(1, AnchorConfig.chargeDelayTicks);
                        timer = baseDelay;
                        nextActionTime = now + GaussianTimingEngine.getDelay(baseDelay * 50.0D, 20.0D, 50L, 200L);
                    } else if (AnchorConfig.autoExplode) {
                        state = State.WAITING_DETONATE;
                        int baseDelay = Math.max(2, AnchorConfig.explodeDelayTicks);
                        timer = baseDelay;
                        nextActionTime = now + GaussianTimingEngine.getDelay(baseDelay * 50.0D, 20.0D, 80L, 300L);
                    } else {
                        if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                            state = State.WAITING_RETURN;
                            timer = 2;
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
                state = State.SELECT_DETONATE;
            }
            case SELECT_DETONATE -> {
                if (!isLookingAtAnchor(client, targetPos)) {
                    cancelState(client);
                    return;
                }
                int detSlot = resolveDetonateSlot(client.player, originalSlot, glowSlot);
                SafeSlotManager.selectSlot(client, detSlot);
                timer = 1;
                state = State.INTERACT_DETONATE;
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
                BlockHitResult hitToUse = lastHit;
                if (hitToUse == null && targetPos != null) {
                    hitToUse = new BlockHitResult(
                        new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5),
                        net.minecraft.util.math.Direction.UP,
                        targetPos,
                        false
                    );
                }
                if (hitToUse != null) {
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitToUse);
                    client.player.swingHand(Hand.MAIN_HAND);
                    if (targetPos != null) {
                        activity.client.module.impl.utility.AutoGGKillTracker.recordExplosion(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5,
                            8.5
                        );
                    }
                }
                lastActionTime = now;
                if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    state = State.WAITING_RETURN;
                    timer = 2;
                } else {
                    reset(client);
                }
            }
            case WAITING_RETURN -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                state = State.RETURN_SLOT;
            }
            case RETURN_SLOT -> {
                if (AnchorConfig.autoReturn && originalSlot >= 0 && originalSlot < 9) {
                    SafeSlotManager.selectSlot(client, originalSlot);
                }
                reset(client);
            }
            case IDLE -> {
                if (now - lastActionTime < MIN_CYCLE_INTERVAL_MS) {
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
                        lastActionTime = now + 100L;
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
                    int foundGlow = findGlowstoneSlot(client.player);
                    if (foundGlow < 0) {
                        return;
                    }
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    int baseDelay = Math.max(1, AnchorConfig.chargeDelayTicks);
                    state = State.WAITING_CHARGE;
                    timer = baseDelay;
                    nextActionTime = now + GaussianTimingEngine.getDelay(baseDelay * 50.0D, 20.0D, 50L, 200L);
                } else if (AnchorConfig.autoExplode) {
                    originalSlot = client.player.getInventory().getSelectedSlot();
                    targetPos = pos;
                    lastHit = hit;
                    net.fabricmc.pack.api.CombatLockManager.setLock("pvp.anchor_active", true);

                    int baseDelay = Math.max(2, AnchorConfig.explodeDelayTicks);
                    state = State.WAITING_DETONATE;
                    timer = baseDelay;
                    nextActionTime = now + GaussianTimingEngine.getDelay(baseDelay * 50.0D, 20.0D, 80L, 300L);
                }
            }
        }
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
        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        if (origSlot >= 0 && origSlot < 9 && origSlot != gSlot) {
            ItemStack stack = player.getInventory().getStack(origSlot);
            if (isSafeDetonateItem(stack)) {
                return origSlot;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == gSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (isSafeDetonateItem(stack)) {
                return i;
            }
        }

        return origSlot >= 0 && origSlot < 9 ? origSlot : player.getInventory().getSelectedSlot();
    }

    private boolean isSafeDetonateItem(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.isOf(Items.GLOWSTONE)) return false;
        if (stack.isOf(Items.RESPAWN_ANCHOR)) return false;
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
        if (activity.client.module.service.PlayerStateService.isBusy(player)) {
            return true;
        }
        if (client.options.useKey.isPressed()) {
            return true;
        }
        return false;
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
