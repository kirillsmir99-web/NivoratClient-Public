package dev.virion.arc;

import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class VirionArcController {
    private static final int TRAJECTORY_TICKS = 45;
    private static final double ARROW_DRAG = 0.99D;
    private static final double ARROW_GRAVITY = 0.05D;
    private static final double HIT_FACE_INSET = 0.02D;
    private static final double ENTITY_COLLISION_MARGIN = 0.15D;
    private static final double REACH_SAFETY_MARGIN = 0.15D;
    private static final double MIN_LEGIT_SAFE_DISTANCE = 2.0D;
    private static final double MIN_NON_LEGIT_DISTANCE = 0.6D;

    private static final int MIN_BOW_DRAW_TICKS = 2;
    private static final int MAX_BOW_DRAW_TICKS = 72000;
    private static final float MIN_PULL_PROGRESS = 0.10F;
    private static final float MAX_PULL_PROGRESS = 1.0F;
    private static final double MAX_ALLOWED_AIM_DEV_DOT = 0.50D;

    private static final long MIN_PLACEMENT_INTERVAL_MS = 200L;
    private static volatile boolean placementRunning = false;
    private static volatile long lastPlacementTime = 0L;

    private boolean enabled = true;
    private boolean wasUsingBow;
    private boolean jobCreatedThisTick;
    private int bowDrawTicks;
    private int bowSlot = -1;
    private PlacementJob activeJob;

    public VirionArcController() {
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            resetSession(client);
            return;
        }

        if (!enabled || !client.player.isAlive()) {
            cancelJob(client);
            resetBowTracking();
            return;
        }

        trackBowRelease(client);
        runPlacement(client);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        var client = MinecraftClient.getInstance();
        if (client != null && client.player != null && !enabled) {
            cancelJob(client);
            resetBowTracking();
        }
    }

    private void trackBowRelease(MinecraftClient client) {
        if (client.player == null || client.world == null || client.player.isGliding() || client.currentScreen != null) {
            resetBowTracking();
            return;
        }

        boolean usingBow = client.player.isUsingItem()
            && client.player.getActiveItem().getItem() instanceof BowItem;

        int currentSlot = client.player.getInventory().getSelectedSlot();
        ItemStack currentItem = client.player.getInventory().getStack(currentSlot);

        if (usingBow) {
            wasUsingBow = true;
            bowSlot = currentSlot;
            bowDrawTicks = Math.max(bowDrawTicks, client.player.getItemUseTime());
            return;
        }

        if (!wasUsingBow) {
            return;
        }

        int releasedDrawTicks = bowDrawTicks;
        int trackedBowSlot = bowSlot;
        resetBowTracking();

        if (trackedBowSlot < 0 || currentSlot != trackedBowSlot || !(currentItem.getItem() instanceof BowItem)) {
            return;
        }

        boolean hasArrow = client.player.getAbilities().creativeMode
            || !client.player.getProjectileType(currentItem).isEmpty();
        if (!hasArrow) {
            return;
        }

        if (releasedDrawTicks < MIN_BOW_DRAW_TICKS || releasedDrawTicks > MAX_BOW_DRAW_TICKS) {
            return;
        }

        float pullProgress = BowItem.getPullProgress(releasedDrawTicks);
        if (pullProgress < MIN_PULL_PROGRESS || pullProgress > MAX_PULL_PROGRESS) {
            return;
        }

        beginPlacementSequence(client, releasedDrawTicks);
    }

    private void beginPlacementSequence(MinecraftClient client, int drawTicks) {
        if (placementRunning || activeJob != null || client.currentScreen != null) {
            return;
        }

        if (activity.client.module.service.CartStateService.isCartOnCooldown(client.player)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastPlacementTime < MIN_PLACEMENT_INTERVAL_MS) {
            return;
        }

        int railSlot = findRailSlot(client.player);
        int minecartSlot = findTntMinecartSlot(client.player);
        boolean cartInOffhand = activity.client.module.service.CartStateService.isCartInOffhand(client.player);

        boolean useMainHand;
        if (MorrowConfig.useMainhandCart) {
            if (minecartSlot >= 0) {
                useMainHand = true;
            } else if (cartInOffhand) {
                useMainHand = false;
            } else {
                return;
            }
        } else {
            if (cartInOffhand) {
                useMainHand = false;
            } else if (minecartSlot >= 0) {
                useMainHand = true;
            } else {
                return;
            }
        }

        if (railSlot < 0) {
            return;
        }

        TargetResolution resolution = resolvePlacementTarget(client, drawTicks);
        if (resolution == null || resolution.target == null || !canPlaceRail(client, resolution.target)) {
            return;
        }

        BlockPos target = resolution.target;
        BlockState targetState = client.world.getBlockState(target);
        if (targetState.getBlock() instanceof AbstractRailBlock) {
            return;
        }

        if (MorrowConfig.placementChance < 100
            && ThreadLocalRandom.current().nextInt(100) >= MorrowConfig.placementChance) {
            return;
        }

        placementRunning = true;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", true);
        lastPlacementTime = now;

        int originalSlot = client.player.getInventory().getSelectedSlot();

        activeJob = new PlacementJob(
            target,
            originalSlot,
            railSlot,
            minecartSlot,
            useMainHand,
            now
        );
    }

    private TargetResolution resolvePlacementTarget(MinecraftClient client, int drawTicks) {

        TargetResolution trajectoryResolution = predictReleasedArrow(client, drawTicks);
        if (trajectoryResolution != null && trajectoryResolution.target != null && canPlaceRail(client, trajectoryResolution.target)) {
            return trajectoryResolution;
        }

        if (client.crosshairTarget != null) {
            if (client.crosshairTarget instanceof EntityHitResult entityHit) {
                Entity entity = entityHit.getEntity();
                if (entity != null && entity.isAlive() && !entity.isSpectator()) {
                    BlockPos groundPos = findSolidGroundBelow(client.world, entity.getBlockPos(), 3);
                    if (groundPos != null && canPlaceRail(client, groundPos)) {
                        double power = BowItem.getPullProgress(drawTicks);
                        double speed = power * 3.0D;
                        double dist = Math.sqrt(client.player.squaredDistanceTo(entity));
                        int estTicks = Math.max(2, (int) Math.round(dist / (speed > 0.1D ? speed : 0.6D)));
                        return new TargetResolution(groundPos, estTicks);
                    }
                }
            } else if (client.crosshairTarget instanceof BlockHitResult crossHit && crossHit.getType() == HitResult.Type.BLOCK) {
                BlockPos crossTarget = resolveTargetFromHit(client, crossHit);
                if (crossTarget != null && canPlaceRail(client, crossTarget)) {
                    double power = BowItem.getPullProgress(drawTicks);
                    double speed = power * 3.0D;
                    double dist = Math.sqrt(client.player.getEyePos().squaredDistanceTo(crossHit.getPos()));
                    int estTicks = Math.max(2, (int) Math.round(dist / (speed > 0.1D ? speed : 0.6D)));
                    return new TargetResolution(crossTarget, estTicks);
                }
            }
        }

        boolean isSelfCartAim = MorrowConfig.allowSelfCart && client.player.getPitch() >= 82.0F;
        if (isSelfCartAim) {
            BlockPos feetGround = findSolidGroundBelow(client.world, client.player.getBlockPos(), 2);
            if (feetGround != null && canPlaceRail(client, feetGround)) {
                return new TargetResolution(feetGround, 2);
            }
        }

        return null;
    }

    private TargetResolution predictReleasedArrow(MinecraftClient client, int drawTicks) {
        double power = BowItem.getPullProgress(drawTicks);
        Vec3d position = client.player.getEyePos().add(0.0D, -0.1D, 0.0D);
        Vec3d velocity = client.player.getRotationVec(1.0F).multiply(power * 3.0D);
        Vec3d playerVel = client.player.getVelocity();
        if (playerVel != null) {
            velocity = velocity.add(playerVel.x, client.player.isOnGround() ? 0.0D : playerVel.y, playerVel.z);
        }

        for (int tick = 0; tick < TRAJECTORY_TICKS; tick++) {
            Vec3d nextPosition = position.add(velocity);

            Box swept = new Box(
                Math.min(position.x, nextPosition.x), Math.min(position.y, nextPosition.y), Math.min(position.z, nextPosition.z),
                Math.max(position.x, nextPosition.x), Math.max(position.y, nextPosition.y), Math.max(position.z, nextPosition.z)
            ).expand(ENTITY_COLLISION_MARGIN);

            List<Entity> candidates = client.world.getOtherEntities(client.player, swept,
                e -> e.isAlive() && !e.isSpectator() && e.canHit()
                    && (e instanceof LivingEntity
                        || e.getType().toString().contains("crystal")
                        || e.getType().toString().contains("minecart")));

            Entity hitEntity = null;
            double closestDistSq = Double.MAX_VALUE;
            for (Entity e : candidates) {
                Box eBox = e.getBoundingBox().expand(0.3D);
                var rayHit = eBox.raycast(position, nextPosition);
                if (rayHit.isPresent()) {
                    double d = position.squaredDistanceTo(rayHit.get());
                    if (d < closestDistSq) {
                        closestDistSq = d;
                        hitEntity = e;
                    }
                } else if (swept.intersects(eBox)) {
                    double d = position.squaredDistanceTo(e.getBoundingBox().getCenter());
                    if (d < closestDistSq) {
                        closestDistSq = d;
                        hitEntity = e;
                    }
                }
            }

            if (hitEntity != null) {
                BlockPos groundCandidate = findSolidGroundBelow(client.world, hitEntity.getBlockPos(), 3);
                if (groundCandidate != null && canPlaceRail(client, groundCandidate)) {
                    return new TargetResolution(groundCandidate, tick + 1);
                }
            }

            BlockHitResult hit = raycast(client, position, nextPosition);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos target = resolveTargetFromHit(client, hit);
                if (target != null && canPlaceRail(client, target)) {
                    return new TargetResolution(target, tick + 1);
                }

                break;
            }

            position = nextPosition;
            velocity = velocity.multiply(ARROW_DRAG).add(0.0D, -ARROW_GRAVITY, 0.0D);
        }

        return null;
    }

    private BlockPos findSolidGroundBelow(World world, BlockPos start, int maxDrop) {
        BlockPos current = start;
        for (int i = 0; i <= maxDrop; i++) {
            BlockPos support = current.down();
            BlockState state = world.getBlockState(support);
            if (!state.isAir() && state.isSideSolidFullSquare(world, support, Direction.UP)) {
                return current;
            }
            current = current.down();
        }
        return null;
    }

    private BlockHitResult raycast(MinecraftClient client, Vec3d start, Vec3d end) {
        return client.world.raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));
    }

    private BlockPos resolveTargetFromHit(MinecraftClient client, BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos hitBlock = hit.getBlockPos();
        BlockState hitState = client.world.getBlockState(hitBlock);

        if (hitState.getBlock() instanceof AbstractRailBlock) {
            return null;
        }

        if (hitState.isReplaceable()) {
            if (canPlaceRail(client, hitBlock)) {
                return hitBlock.toImmutable();
            }
        }

        Direction side = hit.getSide();

        if (side == Direction.UP) {
            BlockPos topCandidate = hitBlock.up();
            if (canPlaceRail(client, topCandidate)) {
                return topCandidate.toImmutable();
            }
        } else if (side.getAxis().isHorizontal()) {

            if (hit.getPos().y >= hitBlock.getY() + 0.45D) {
                BlockPos topCandidate = hitBlock.up();
                if (canPlaceRail(client, topCandidate)) {
                    return topCandidate.toImmutable();
                }
            }

            Vec3d backStep = hit.getPos().subtract(client.player.getRotationVec(1.0F).multiply(0.35D));
            BlockPos backPos = BlockPos.ofFloored(backStep.x, backStep.y, backStep.z);
            BlockPos groundInFront = findSolidGroundBelow(client.world, backPos, 2);
            if (groundInFront != null && canPlaceRail(client, groundInFront)) {
                return groundInFront.toImmutable();
            }

            BlockPos inFront = hitBlock.offset(side);
            if (canPlaceRail(client, inFront)) {
                return inFront.toImmutable();
            }

            BlockPos dropCandidate = inFront.down();
            if (canPlaceRail(client, dropCandidate)) {
                return dropCandidate.toImmutable();
            }

            if (hit.getPos().y < hitBlock.getY() + 0.45D) {
                BlockPos topCandidate = hitBlock.up();
                if (canPlaceRail(client, topCandidate)) {
                    return topCandidate.toImmutable();
                }
            }

            if (MorrowConfig.allowPitPlacement) {
                BlockPos pit2 = inFront.down(2);
                if (canPlaceRail(client, pit2)) {
                    return pit2.toImmutable();
                }
                BlockPos pit3 = inFront.down(3);
                if (canPlaceRail(client, pit3)) {
                    return pit3.toImmutable();
                }
            }
        } else if (side == Direction.DOWN) {
            BlockPos underCeiling = hitBlock.down();
            if (canPlaceRail(client, underCeiling)) {
                return underCeiling.toImmutable();
            }
        }

        return null;
    }

    private void runPlacement(MinecraftClient client) {
        if (activeJob == null) {
            return;
        }

        if (client.player == null || client.world == null || client.currentScreen != null || !client.player.isAlive()) {
            placementRunning = false;
            net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", false);
            activeJob = null;
            return;
        }

        long now = System.currentTimeMillis();
        if (now < activeJob.scheduledTimeMs) {
            return;
        }

        if (activeJob.stage == Stage.RESTORE_SLOT) {
            finishJob(client);
            return;
        }

        if (!isPlayerAimingAtTarget(client, activeJob.target)) {
            cancelJob(client);
            return;
        }

        switch (activeJob.stage) {
            case SELECT_RAIL -> {
                if (!withinReach(client, activeJob.target) || !hasLineOfSight(client, activeJob.target)) {
                    cancelJob(client);
                    return;
                }
                selectSlot(client, activeJob.railSlot);
                activeJob.lastSlotSwitchTimeMs = now;
                activeJob.stage = Stage.PLACE_RAIL;
                int railDelay = getRandomDelay();
                activeJob.scheduledTimeMs = now + railDelay;
            }
            case PLACE_RAIL -> {
                BlockPos support = activeJob.target.down();
                int heightDiff = activeJob.target.getY() - client.player.getBlockY();
                if (heightDiff >= 2 && client.player.isOnGround() && client.player.getEyePos().y < support.getY() + 0.90D) {
                    client.player.jump();
                }

                if (!withinReach(client, activeJob.target) || !hasLineOfSight(client, activeJob.target)) {
                    cancelJob(client);
                    return;
                }
                if (client.player.getInventory().getSelectedSlot() != activeJob.railSlot) {
                    selectSlot(client, activeJob.railSlot);
                    activeJob.lastSlotSwitchTimeMs = now;
                }

                if (!(client.world.getBlockState(activeJob.target).getBlock() instanceof AbstractRailBlock)) {
                    interactAtTop(client, activeJob.target.down());
                }

                if (activeJob.useMainHand) {
                    selectSlot(client, activeJob.minecartSlot);
                    activeJob.lastSlotSwitchTimeMs = now;
                }
                activeJob.stage = Stage.PLACE_CART;
                int delay = getRandomDelay();
                activeJob.scheduledTimeMs = now + delay;
            }
            case PLACE_CART -> {
                if (!withinReach(client, activeJob.target) || !hasLineOfSight(client, activeJob.target)) {
                    cancelJob(client);
                    return;
                }
                if (activity.client.module.service.CartStateService.isCartOnCooldown(client.player)) {
                    cancelJob(client);
                    return;
                }
                if (hasBlockingVehicle(client, activeJob.target)) {
                    finishJob(client);
                    return;
                }
                if (activeJob.useMainHand && client.player.getInventory().getSelectedSlot() != activeJob.minecartSlot) {
                    selectSlot(client, activeJob.minecartSlot);
                    activeJob.lastSlotSwitchTimeMs = now;
                }

                BlockState state = client.world.getBlockState(activeJob.target);
                if (!(state.getBlock() instanceof AbstractRailBlock)) {
                    if (activeJob.railRetries < 2) {
                        activeJob.railRetries++;
                        activeJob.scheduledTimeMs = now + 50;
                        return;
                    }
                    cancelJob(client);
                    return;
                }

                interactOnRail(client, activeJob.target, activeJob.useMainHand ? Hand.MAIN_HAND : Hand.OFF_HAND);
                activity.client.module.service.CartStateService.notifyCartPlaced(activeJob.target);
                activeJob.stage = Stage.RESTORE_SLOT;
                int restoreDelay = getRandomDelay();
                activeJob.scheduledTimeMs = now + restoreDelay;
            }
            case RESTORE_SLOT -> {
                finishJob(client);
            }
        }
    }

    private boolean canPlaceRail(MinecraftClient client, BlockPos target) {
        if (target == null || client == null || client.world == null || client.player == null) {
            return false;
        }

        BlockState state = client.world.getBlockState(target);
        if (state.getBlock() instanceof AbstractRailBlock || !state.isReplaceable()) {
            return false;
        }

        BlockPos supportPos = target.down();
        BlockState support = client.world.getBlockState(supportPos);

        boolean solidSupport = !support.isAir()
            && support.isSideSolidFullSquare(client.world, supportPos, Direction.UP);

        if (!solidSupport) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (hasBlockingVehicle(client, target)) {
            return false;
        }

        boolean isSelfCart = MorrowConfig.allowSelfCart && client.player.getPitch() >= 82.0F;

        if (!isSelfCart) {
            Box playerBox = client.player.getBoundingBox();
            Box targetBox = new Box(target);

            if (playerBox.expand(0.10D).intersects(targetBox)) {
                return false;
            }

            if (target.getY() >= client.player.getBlockY()) {
                double dx = (target.getX() + 0.5D) - client.player.getX();
                double dz = (target.getZ() + 0.5D) - client.player.getZ();
                double hDistSq = dx * dx + dz * dz;

                double minDist = MorrowConfig.legitMode ? 1.15D : 0.75D;
                if (hDistSq < minDist * minDist) {
                    return false;
                }
            }
        }

        return isTargetInFront(client, target)
            && checkHeightFilter(client, target)
            && hasLineOfSight(client, target)
            && withinReach(client, target);
    }

    private boolean hasBlockingVehicle(MinecraftClient client, BlockPos target) {
        Box box = new Box(target);
        return !client.world.getOtherEntities(client.player, box,
            entity -> entity.isAlive() && !entity.isSpectator()
                && (entity.getType().toString().contains("minecart"))).isEmpty();
    }

    private boolean isPlayerAimingAtTarget(MinecraftClient client, BlockPos target) {
        if (client == null || client.player == null || target == null) {
            return false;
        }

        boolean isSelfCart = MorrowConfig.allowSelfCart && client.player.getPitch() >= 82.0F;
        if (isSelfCart) {
            int dx = Math.abs(target.getX() - client.player.getBlockX());
            int dz = Math.abs(target.getZ() - client.player.getBlockZ());
            if (dx <= 1 && dz <= 1) {
                return true;
            }
        }

        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetCenter = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        Vec3d toTarget = targetCenter.subtract(eyePos).normalize();
        Vec3d lookVec = client.player.getRotationVec(1.0F).normalize();
        double dot = lookVec.dotProduct(toTarget);

        return dot >= MAX_ALLOWED_AIM_DEV_DOT;
    }

    private boolean isTargetInFront(MinecraftClient client, BlockPos target) {
        return isPlayerAimingAtTarget(client, target);
    }

    private boolean checkHeightFilter(MinecraftClient client, BlockPos target) {
        int playerY = client.player.getBlockY();
        int targetY = target.getY();
        int diff = targetY - playerY;

        if (MorrowConfig.legitMode) {
            if (diff > 0) {

                return diff <= 4;
            } else {

                int minDiff = MorrowConfig.allowPitPlacement ? -6 : -4;
                return diff >= minDiff;
            }
        } else {
            return diff <= 8 && diff >= -8;
        }
    }

    private boolean hasLineOfSight(MinecraftClient client, BlockPos target) {
        if (client == null || client.player == null || client.world == null || target == null) {
            return false;
        }

        Vec3d eyePos = client.player.getEyePos();
        BlockPos supportPos = target.down();

        Vec3d targetCenter = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        BlockHitResult hitTarget = client.world.raycast(new RaycastContext(
            eyePos,
            targetCenter,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));

        if (hitTarget == null || hitTarget.getType() == HitResult.Type.MISS) {
            return true;
        }

        if (hitTarget.getBlockPos().equals(target) || hitTarget.getBlockPos().equals(supportPos)) {
            return true;
        }

        Vec3d topPoint = nearestTopPoint(eyePos, supportPos);
        BlockHitResult hitSupport = client.world.raycast(new RaycastContext(
            eyePos,
            topPoint,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));

        if (hitSupport == null || hitSupport.getType() == HitResult.Type.MISS) {
            return true;
        }

        return hitSupport.getBlockPos().equals(target) || hitSupport.getBlockPos().equals(supportPos);
    }

    private double getReachDistance(MinecraftClient client) {
        double baseRange = client.player.getBlockInteractionRange();
        double reach = Math.min(baseRange - REACH_SAFETY_MARGIN, MorrowConfig.maxDistance);
        return Math.max(1.0D, reach);
    }

    private boolean withinReach(MinecraftClient client, BlockPos target) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d hitPosition = nearestTopPoint(eyePos, target.down());
        Vec3d targetCenter = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);

        double dSq = Math.min(eyePos.squaredDistanceTo(hitPosition), eyePos.squaredDistanceTo(targetCenter));
        double baseRange = client.player.getBlockInteractionRange();

        double max3DReach = Math.min(baseRange, MorrowConfig.maxDistance + 0.25D);
        if (dSq > max3DReach * max3DReach) {
            return false;
        }

        double dx = (target.getX() + 0.5D) - client.player.getX();
        double dz = (target.getZ() + 0.5D) - client.player.getZ();
        double hDistSq = dx * dx + dz * dz;
        double maxHDist = MorrowConfig.maxDistance + 0.25D;

        return hDistSq <= maxHDist * maxHDist;
    }

    private int getMinSafeSlotHoldMs() {
        if (MorrowConfig.preset == MorrowConfig.PRESET_FAST) {
            return 55;
        } else if (MorrowConfig.preset == MorrowConfig.PRESET_SAFE) {
            return 115;
        }
        return 75;
    }

    private int getRandomDelay() {
        int min = MorrowConfig.getMinDelayMs();
        int max = MorrowConfig.getMaxDelayMs();
        if (min >= max) return min;
        int base = ThreadLocalRandom.current().nextInt(min, max + 1);
        int jitter = ThreadLocalRandom.current().nextInt(-5, 6);
        return Math.max(35, base + jitter);
    }

    private void interactAtTop(MinecraftClient client, BlockPos blockPos) {
        Vec3d hitPosition = nearestTopPoint(client.player.getEyePos(), blockPos);
        BlockHitResult result = new BlockHitResult(hitPosition, Direction.UP, blockPos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private void interactOnRail(MinecraftClient client, BlockPos railPos, Hand hand) {
        Vec3d topPoint = nearestTopPoint(client.player.getEyePos(), railPos.down());
        Vec3d hitPosition = new Vec3d(topPoint.x, railPos.getY() + 0.1D, topPoint.z);
        BlockHitResult result = new BlockHitResult(hitPosition, Direction.UP, railPos, false);
        client.interactionManager.interactBlock(client.player, hand, result);
        client.player.swingHand(hand);
        activity.client.module.service.CartStateService.notifyCartPlaced(railPos);
    }

    private Vec3d nearestTopPoint(Vec3d origin, BlockPos support) {
        double x = Math.max(support.getX() + HIT_FACE_INSET,
            Math.min(origin.x, support.getX() + 1.0D - HIT_FACE_INSET));
        double z = Math.max(support.getZ() + HIT_FACE_INSET,
            Math.min(origin.z, support.getZ() + 1.0D - HIT_FACE_INSET));
        return new Vec3d(x, support.getY() + 1.0D, z);
    }

    private int findRailSlot(ClientPlayerEntity player) {
        return activity.client.module.service.CartStateService.findRailSlot(player);
    }

    private int findTntMinecartSlot(ClientPlayerEntity player) {
        return activity.client.module.service.CartStateService.findHotbarCart(player);
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client == null || client.player == null || slot < 0 || slot >= 9) {
            return;
        }
        SafeSlotManager.selectSlot(client, slot);
    }

    private void finishJob(MinecraftClient client) {
        if (activeJob == null) {
            placementRunning = false;
            net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", false);
            return;
        }

        long now = System.currentTimeMillis();
        long heldTime = now - activeJob.lastSlotSwitchTimeMs;
        int minSafeHold = getMinSafeSlotHoldMs();

        if (heldTime < minSafeHold && client != null && client.player != null
            && client.player.getInventory().getSelectedSlot() != activeJob.originalSlot) {
            activeJob.stage = Stage.RESTORE_SLOT;
            activeJob.scheduledTimeMs = activeJob.lastSlotSwitchTimeMs + minSafeHold
                + ThreadLocalRandom.current().nextInt(10, 30);
            return;
        }

        placementRunning = false;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.cart_placement_active", false);
        lastPlacementTime = now;
        if (client != null && client.player != null) {
            selectSlot(client, activeJob.originalSlot);
        }
        activeJob = null;
    }

    private void cancelJob(MinecraftClient client) {
        finishJob(client);
    }

    private void resetBowTracking() {
        wasUsingBow = false;
        bowDrawTicks = 0;
        bowSlot = -1;
    }

    private void resetSession(MinecraftClient client) {
        cancelJob(client);
        resetBowTracking();
    }

    private enum Stage {
        SELECT_RAIL,
        PLACE_RAIL,
        PLACE_CART,
        RESTORE_SLOT
    }

    private static final class TargetResolution {
        private final BlockPos target;
        private final int flightTicks;

        private TargetResolution(BlockPos target, int flightTicks) {
            this.target = target;
            this.flightTicks = flightTicks;
        }
    }

    private static final class PlacementJob {
        private final BlockPos target;
        private final int originalSlot;
        private final int railSlot;
        private final int minecartSlot;
        private final boolean useMainHand;
        private Stage stage;
        private final long startTimeMs;
        private long scheduledTimeMs;
        private long lastSlotSwitchTimeMs;
        private int railRetries;

        private PlacementJob(BlockPos target, int originalSlot, int railSlot, int minecartSlot, boolean useMainHand, long startTimeMs) {
            this.target = target;
            this.originalSlot = originalSlot;
            this.railSlot = railSlot;
            this.minecartSlot = minecartSlot;
            this.useMainHand = useMainHand;
            this.stage = Stage.SELECT_RAIL;
            this.startTimeMs = startTimeMs;
            this.scheduledTimeMs = startTimeMs;
            this.lastSlotSwitchTimeMs = startTimeMs;
            this.railRetries = 0;
        }
    }
}
