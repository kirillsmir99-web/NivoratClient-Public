package dev.nivorat.arc;

import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoCartController {
    private static final int TRAJECTORY_TICKS = dev.nivorat.arc.internal.ArcDomain.i(1245457229);
    private static final double ARROW_DRAG = dev.nivorat.arc.internal.ArcDomain.d(6080668990927871120L);
    private static final double ARROW_GRAVITY = dev.nivorat.arc.internal.ArcDomain.d(6063208178848744100L);
    private static final double HIT_FACE_INSET = dev.nivorat.arc.internal.ArcDomain.d(6059932834006716229L);
    private static final double ENTITY_COLLISION_MARGIN = dev.nivorat.arc.internal.ArcDomain.d(6074930507809777677L);
    private static final double REACH_SAFETY_MARGIN = dev.nivorat.arc.internal.ArcDomain.d(6074930507809777677L);
    private static final double MIN_LEGIT_SAFE_DISTANCE = dev.nivorat.arc.internal.ArcDomain.d(3138250481166211902L);
    private static final double MIN_NON_LEGIT_DISTANCE = dev.nivorat.arc.internal.ArcDomain.d(6083937707064518669L);

    private static final int MIN_BOW_DRAW_TICKS = dev.nivorat.arc.internal.ArcDomain.i(1245457182);
    private static final int MAX_BOW_DRAW_TICKS = dev.nivorat.arc.internal.ArcDomain.i(1245415709);
    private static final float MIN_PULL_PROGRESS = dev.nivorat.arc.internal.ArcDomain.f(1447133922);
    private static final float MAX_PULL_PROGRESS = dev.nivorat.arc.internal.ArcDomain.f(1410158127);
    private static final double MAX_ALLOWED_AIM_DEV_DOT = dev.nivorat.arc.internal.ArcDomain.d(6083604637466516286L);

    private static final String S_CART_ACTIVE = activity.client.util.Obf.s(new byte[]{(byte) 118, (byte) -83, (byte) 125, (byte) -126, (byte) 48, (byte) -107, (byte) 62, (byte) -97, (byte) 115, (byte) 109, (byte) 24, (byte) -90, (byte) 93, (byte) 76, (byte) 55, (byte) -121, (byte) -64, (byte) -24, (byte) -38, (byte) -27, (byte) -62, (byte) 19, (byte) 62, (byte) 6, (byte) -104});
    private static final long MIN_PLACEMENT_INTERVAL_MS = 200L;
    private static volatile boolean placementRunning = false;
    private static volatile long lastPlacementTime = 0L;
    private static volatile AutoCartController activeInstance;

    private final ArcCameraInterpolator cameraInterpolator = new ArcCameraInterpolator();
    private boolean enabled = true;
    private boolean wasUsingBow;
    private int bowDrawTicks;
    private int bowSlot = -1;
    private PlacementJob activeJob;
    private ClientPlayerEntity owner;
    private World ownerWorld;
    private final java.util.Set<java.util.UUID> knownArrows = new java.util.HashSet<>();
    private int pendingDrawTicks;
    private long shotDeadlineMs;
    private ClientPlayerEntity shotOwner;
    private World shotWorld;
    private int shotBowSlot = -1;
    private boolean macroDrawing;
    private int macroOriginalSlot = -1;
    private int macroBowSlot = -1;
    private int macroDrawTicks;
    private ClientPlayerEntity macroOwner;
    private World macroWorld;
    private boolean macroUseWasPressed;

    public AutoCartController() {
        activeInstance = this;
    }

    public static void onRender(MinecraftClient client) {
        ArcMotionProfile.trackNaturalMovement(client);
        AutoCartController inst = activeInstance;
        if (inst != null && inst.isEnabled()) {
            inst.cameraInterpolator.onRender(client);
        }
    }

    public void tick(MinecraftClient client) {
        activeInstance = this;
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            cancelMacro(client);
            clearPendingShot();
            resetSession(client);
            return;
        }

        if (!enabled || !client.player.isAlive() || client.currentScreen != null
                || (activeJob != null && (owner != client.player || ownerWorld != client.world))) {
            cancelMacro(client);
            clearPendingShot();
            cancelJob(client);
            resetBowTracking();
            return;
        }

        tickMacro(client);
        trackBowRelease(client);
        confirmReleasedArrow(client);
        runPlacement(client);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            cancelMacro(MinecraftClient.getInstance());
            clearPendingShot();
            cameraInterpolator.reset();
        }
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
            if (!wasUsingBow) {
                knownArrows.clear();
                for (var arrow : client.world.getEntitiesByClass(net.minecraft.entity.projectile.ArrowEntity.class,
                        client.player.getBoundingBox().expand(40), entity -> true)) knownArrows.add(arrow.getUuid());
            }
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

        if (activeJob == null) {
            beginPlacementSequence(client, releasedDrawTicks);
        }
        if (pendingDrawTicks <= 0 && activeJob == null) {
            pendingDrawTicks = releasedDrawTicks;
            shotDeadlineMs = System.currentTimeMillis() + 1500L;
            shotOwner = client.player;
            shotWorld = client.world;
            shotBowSlot = trackedBowSlot;
        }
    }

    private void confirmReleasedArrow(MinecraftClient client) {
        if (pendingDrawTicks <= 0) return;
        if (client.player != shotOwner || client.world != shotWorld
                || client.player.getInventory().getSelectedSlot() != shotBowSlot
                || System.currentTimeMillis() > shotDeadlineMs) {
            clearPendingShot();
            cancelMacro(client);
            return;
        }
        for (var arrow : client.world.getEntitiesByClass(net.minecraft.entity.projectile.ArrowEntity.class,
                client.player.getBoundingBox().expand(40), entity -> entity.getOwner() == client.player)) {
            if (knownArrows.contains(arrow.getUuid()) || arrow.getVelocity().lengthSquared() < 0.0001D) continue;
            int ticks = pendingDrawTicks;
            clearPendingShot();
            beginPlacementSequence(client, ticks);
            if (activeJob == null) cancelMacro(client);
            return;
        }
    }

    private void clearPendingShot() {
        pendingDrawTicks = 0;
        shotOwner = null;
        shotWorld = null;
        shotBowSlot = -1;
        knownArrows.clear();
    }

    public boolean startMacro(MinecraftClient client, int drawTicks) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null
                || client.currentScreen != null || !enabled || !client.player.isAlive()
                || client.player.isUsingItem() || client.player.isGliding()
                || activeJob != null || pendingDrawTicks > 0 || macroOriginalSlot >= 0
                || net.fabricmc.pack.api.CombatLockManager.isLocked()
                || activity.client.module.service.CartStateService.isCartOnCooldown(client.player)
                || findRailSlot(client.player) < 0
                || (findTntMinecartSlot(client.player) < 0 && !activity.client.module.service.CartStateService.isCartInOffhand(client.player))) return false;
        int bow = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BowItem && (client.player.getAbilities().creativeMode
                    || !client.player.getProjectileType(stack).isEmpty())) { bow = slot; break; }
        }
        if (bow < 0) return false;
        macroOriginalSlot = client.player.getInventory().getSelectedSlot();
        macroBowSlot = bow;
        macroOwner = client.player;
        macroWorld = client.world;
        int sampledDraw = (MorrowConfig.preset == MorrowConfig.PRESET_LEARNED)
            ? ArcMotionProfile.getInstance().sampleMacroDrawTicks(drawTicks)
            : drawTicks;
        macroDrawTicks = MathHelper.clamp(sampledDraw, 3, 20);
        macroUseWasPressed = client.options.useKey.isPressed();
        SafeSlotManager.selectSlot(client, bow);
        if (!client.interactionManager.interactItem(client.player, Hand.MAIN_HAND).isAccepted()) {
            cancelMacro(client);
            return false;
        }
        macroDrawing = true;
        client.options.useKey.setPressed(true);
        net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.CART_PLACEMENT, true);
        return true;
    }

    private void tickMacro(MinecraftClient client) {
        if (!macroDrawing) return;
        if (client.player != macroOwner || client.world != macroWorld
                || client.player.getInventory().getSelectedSlot() != macroBowSlot
                || !client.player.isUsingItem() || !(client.player.getActiveItem().getItem() instanceof BowItem)) {
            cancelMacro(client);
            resetBowTracking();
            return;
        }
        if (client.player.getItemUseTime() < macroDrawTicks) return;
        int drawnTicks = Math.max(bowDrawTicks, client.player.getItemUseTime());
        client.player.stopUsingItem();
        client.interactionManager.stopUsingItem(client.player);
        client.options.useKey.setPressed(false);
        macroDrawing = false;
        net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.CART_PLACEMENT, false);
        beginPlacementSequence(client, drawnTicks);
        if (activeJob == null) {
            cancelMacro(client);
        }
    }

    private void cancelMacro(MinecraftClient client) {
        boolean ownedMacro = macroOriginalSlot >= 0 || macroDrawing;
        if (ownedMacro && client != null) client.options.useKey.setPressed(macroUseWasPressed);
        if (macroDrawing && client != null && client.player == macroOwner && client.interactionManager != null
                && client.player.isUsingItem()) {
            client.player.stopUsingItem();
        }
        if (macroOriginalSlot >= 0 && client != null && client.player == macroOwner && client.world == macroWorld
                && client.player.getInventory().getSelectedSlot() == macroBowSlot) SafeSlotManager.restoreSlot(client, macroOriginalSlot);
        macroOriginalSlot = -1;
        macroBowSlot = -1;
        macroDrawing = false;
        macroOwner = null;
        macroWorld = null;
        if (ownedMacro) {
            net.fabricmc.pack.api.CombatLockManager.setLock(net.fabricmc.pack.api.CombatLockManager.CART_PLACEMENT, false);
        }
    }

    private void beginPlacementSequence(MinecraftClient client, int drawTicks) {
        if (placementRunning || activeJob != null || client.currentScreen != null
                || net.fabricmc.pack.api.CombatLockManager.isLocked()) {
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
        owner = client.player;
        ownerWorld = client.world;
        net.fabricmc.pack.api.CombatLockManager.setLock(S_CART_ACTIVE, true);
        lastPlacementTime = now;

        int originalSlot = macroOriginalSlot >= 0 ? macroOriginalSlot : client.player.getInventory().getSelectedSlot();
        macroOriginalSlot = -1;
        macroOwner = null;
        macroWorld = null;
        float originalPitch = client.player.getPitch();
        float originalYaw = client.player.getYaw();
        float pullProgress = BowItem.getPullProgress(drawTicks);

        activeJob = new PlacementJob(
            target,
            resolution.aimTarget,
            originalSlot,
            railSlot,
            minecartSlot,
            useMainHand,
            now,
            originalPitch,
            originalYaw,
            pullProgress,
            resolution.flightTicks
        );

        boolean isPacket = "packet".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean isOff = "off".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean alreadyAiming = isPlayerAimingAtTarget(client, target, resolution.aimTarget);
        if (MorrowConfig.autoCamera && !isPacket && !isOff && !alreadyAiming) {
            long cameraDuration = MorrowConfig.cameraSmoothnessMs;
            if (MorrowConfig.preset == MorrowConfig.PRESET_SAFE) {
                int flightMs = Math.max(1, resolution.flightTicks) * 50;
                int prepMs = MorrowConfig.minDelayMs * 2;
                long targetStartMs = now + flightMs - 60 - prepMs;
                if (targetStartMs > now) {
                    activeJob.scheduledTimeMs = targetStartMs;
                    long adaptiveCamMs = MathHelper.clamp(flightMs - 60, 140, 180);
                    cameraDuration = adaptiveCamMs;
                }
            }
            Vec3d aim = resolution.aimTarget != null ? resolution.aimTarget : new Vec3d(target.getX() + 0.5D, target.getY() + 0.62D, target.getZ() + 0.5D);
            startAimTowards(client, aim, cameraDuration);
        }
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
                        Vec3d cartUpper = new Vec3d(groundPos.getX() + 0.5D, groundPos.getY() + 0.62D, groundPos.getZ() + 0.5D);
                        return new TargetResolution(groundPos, cartUpper, estTicks);
                    }
                }
            } else if (client.crosshairTarget instanceof BlockHitResult crossHit && crossHit.getType() == HitResult.Type.BLOCK) {
                BlockPos crossTarget = resolveTargetFromHit(client, crossHit);
                if (crossTarget != null && canPlaceRail(client, crossTarget)) {
                    double power = BowItem.getPullProgress(drawTicks);
                    double speed = power * 3.0D;
                    double dist = Math.sqrt(client.player.getEyePos().squaredDistanceTo(crossHit.getPos()));
                    int estTicks = Math.max(2, (int) Math.round(dist / (speed > 0.1D ? speed : 0.6D)));
                    Vec3d cartUpper = new Vec3d(crossTarget.getX() + 0.5D, crossTarget.getY() + 0.62D, crossTarget.getZ() + 0.5D);
                    return new TargetResolution(crossTarget, cartUpper, estTicks);
                }
            }
        }

        boolean isSelfCartAim = MorrowConfig.allowSelfCart && client.player.getPitch() >= 82.0F;
        if (isSelfCartAim) {
            BlockPos feetGround = findSolidGroundBelow(client.world, client.player.getBlockPos(), 2);
            if (feetGround != null && canPlaceRail(client, feetGround)) {
                Vec3d cartUpper = new Vec3d(feetGround.getX() + 0.5D, feetGround.getY() + 0.62D, feetGround.getZ() + 0.5D);
                return new TargetResolution(feetGround, cartUpper, 2);
            }
        }

        if (MorrowConfig.autonomousPlacement) {
            TargetResolution enemyTarget = findClosestEnemyGroundTarget(client, drawTicks);
            if (enemyTarget != null) {
                return enemyTarget;
            }
        }

        return null;
    }

    private TargetResolution findClosestEnemyGroundTarget(MinecraftClient client, int drawTicks) {
        if (client == null || client.player == null || client.world == null) return null;
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Box searchBox = client.player.getBoundingBox().expand(MorrowConfig.maxDistance);
        List<Entity> candidates = client.world.getOtherEntities(client.player, searchBox,
            e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity));

        Entity bestEntity = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Entity e : candidates) {
            Vec3d entityPos = new Vec3d(e.getX(), e.getY() + e.getStandingEyeHeight(), e.getZ());
            Vec3d toEntity = entityPos.subtract(eyePos);
            double distSq = toEntity.lengthSquared();
            if (distSq > MorrowConfig.maxDistance * MorrowConfig.maxDistance) continue;
            Vec3d dir = toEntity.normalize();
            double dot = lookVec.x * dir.x + lookVec.z * dir.z;
            if (dot >= 0.20D && distSq < closestDistSq) {
                closestDistSq = distSq;
                bestEntity = e;
            }
        }

        if (bestEntity != null) {
            BlockPos ground = findSolidGroundBelow(client.world, bestEntity.getBlockPos(), 3);
            if (ground != null && canPlaceRail(client, ground)) {
                double power = BowItem.getPullProgress(drawTicks);
                double speed = power * 3.0D;
                double dist = Math.sqrt(closestDistSq);
                int estTicks = Math.max(2, (int) Math.round(dist / (speed > 0.1D ? speed : 0.6D)));
                Vec3d cartUpper = new Vec3d(ground.getX() + 0.5D, ground.getY() + 0.62D, ground.getZ() + 0.5D);
                return new TargetResolution(ground, cartUpper, estTicks);
            }
        }
        return null;
    }

    private TargetResolution predictReleasedArrow(MinecraftClient client, int drawTicks) {
        double power = BowItem.getPullProgress(drawTicks);
        Vec3d position = client.player.getEyePos().add(0.0D, dev.nivorat.arc.internal.ArcDomain.d(-3155660258378661212L), 0.0D);
        Vec3d velocity = client.player.getRotationVec(1.0F).multiply(power * dev.nivorat.arc.internal.ArcDomain.d(3135998681352526654L));
        Vec3d playerVel = client.player.getVelocity();
        if (playerVel != null) {
            velocity = velocity.add(playerVel.x, client.player.isOnGround() ? 0.0D : playerVel.y, playerVel.z);
        }

        TargetResolution bestInReachIntercept = null;
        double bestInterceptScore = Double.MAX_VALUE;

        for (int tick = 0; tick < TRAJECTORY_TICKS; tick++) {
            Vec3d nextPosition = position.add(velocity);

            double distFromPlayer = position.distanceTo(client.player.getEyePos());
            if (distFromPlayer >= dev.nivorat.arc.internal.ArcDomain.d(6088441306691889165L) && distFromPlayer <= getReachDistance(client)) {
                BlockPos floorBelow = findSolidGroundBelow(client.world, BlockPos.ofFloored(position.x, position.y, position.z), 3);
                if (floorBelow != null && canPlaceRail(client, floorBelow)) {
                    Vec3d cartUpper = new Vec3d(floorBelow.getX() + 0.5D, floorBelow.getY() + 0.62D, floorBelow.getZ() + 0.5D);
                    Box cartHitbox = new Box(
                        floorBelow.getX() + 0.02D, floorBelow.getY(), floorBelow.getZ() + 0.02D,
                        floorBelow.getX() + 0.98D, floorBelow.getY() + 0.85D, floorBelow.getZ() + 0.98D
                    ).expand(0.35D);
                    if (cartHitbox.raycast(position, nextPosition).isPresent() || cartHitbox.contains(nextPosition) || cartHitbox.contains(position)) {
                        double score = Math.abs(distFromPlayer - dev.nivorat.arc.internal.ArcDomain.d(3137124581259369278L));
                        if (score < bestInterceptScore) {
                            bestInterceptScore = score;
                            bestInReachIntercept = new TargetResolution(floorBelow, cartUpper, tick + 1);
                        }
                    }
                }
            }

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
                    Vec3d cartUpper = new Vec3d(groundCandidate.getX() + 0.5D, groundCandidate.getY() + 0.62D, groundCandidate.getZ() + 0.5D);
                    return new TargetResolution(groundCandidate, cartUpper, tick + 1);
                }
            }

            BlockHitResult hit = raycast(client, position, nextPosition);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos target = resolveTargetFromHit(client, hit);
                if (target != null && canPlaceRail(client, target)) {
                    Vec3d cartUpper = new Vec3d(target.getX() + 0.5D, target.getY() + 0.62D, target.getZ() + 0.5D);
                    return new TargetResolution(target, cartUpper, tick + 1);
                }
                break;
            }

            position = nextPosition;
            velocity = stepArrowPhysics(velocity);
        }

        if (bestInReachIntercept != null) {
            return bestInReachIntercept;
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
            cancelJob(client);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - activeJob.startTimeMs > 3000L) {
            cancelJob(client);
            return;
        }
        if (now < activeJob.scheduledTimeMs) {
            return;
        }

        if (activeJob.stage == Stage.RESTORE_SLOT) {
            finishJob(client);
            return;
        }

        boolean isPacket = "packet".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean isOff = "off".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean isAutonomous = MorrowConfig.autonomousPlacement;
        if (MorrowConfig.autoCamera && cameraInterpolator.isActive()) return;
        boolean canBypassAim = isPacket || isOff || isAutonomous;
        if (!canBypassAim && !isPlayerAimingAtTarget(client, activeJob.target, activeJob.aimTarget)) {
            if (MorrowConfig.autoCamera) {
                if (cameraInterpolator.isActive() || (now - activeJob.startTimeMs < 600L)) {
                    return;
                }
            } else if (MorrowConfig.legitMode && (now - activeJob.startTimeMs < 450L)) {
                return;
            }
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
                int railDelay = getDynamicPlacementDelay(activeJob);
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
                    if (!(client.player.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem blockItem)
                            || !(blockItem.getBlock() instanceof AbstractRailBlock)
                            || !interactAtTop(client, activeJob.target.down())) {
                        cancelJob(client);
                        return;
                    }
                }

                activeJob.stage = Stage.PLACE_CART;
                int delay = getDynamicPlacementDelay(activeJob);
                activeJob.scheduledTimeMs = now + delay;
            }
            case PLACE_CART -> {
                if (MorrowConfig.autoCamera && cameraInterpolator.isActive()) {
                    return;
                }
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
                Hand cartHand = activeJob.useMainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;
                if (!client.player.getStackInHand(cartHand).isOf(net.minecraft.item.Items.TNT_MINECART)) {
                    cancelJob(client);
                    return;
                }
                boolean railConfirmed = state.getBlock() instanceof AbstractRailBlock;
                if (!railConfirmed && activeJob.railRetries < 2) {
                    activeJob.railRetries++;
                    boolean placed = interactOnRail(client, activeJob.target, cartHand);
                    if (placed) {
                        activeJob.stage = Stage.RESTORE_SLOT;
                        int restoreDelay = getDynamicPlacementDelay(activeJob);
                        activeJob.scheduledTimeMs = now + restoreDelay;
                        return;
                    }
                    activeJob.scheduledTimeMs = now + (MorrowConfig.preset == MorrowConfig.PRESET_FAST ? 25 : 45);
                    return;
                }

                if (!interactOnRail(client, activeJob.target, cartHand)) {
                    cancelJob(client);
                    return;
                }
                activeJob.stage = Stage.RESTORE_SLOT;
                int restoreDelay = getDynamicPlacementDelay(activeJob);
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

            if (!isTargetInFront(client, target)) {
                return false;
            }
        }

        return checkHeightFilter(client, target)
            && hasLineOfSight(client, target)
            && withinReach(client, target);
    }

    private boolean hasBlockingVehicle(MinecraftClient client, BlockPos target) {
        Box box = new Box(target);
        return !client.world.getOtherEntities(client.player, box,
            entity -> entity.isAlive() && !entity.isSpectator()
                && (entity.getType().toString().contains("minecart"))).isEmpty();
    }

    private boolean isPlayerAimingAtTarget(MinecraftClient client, BlockPos target, Vec3d aimTarget) {
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
        Vec3d lookVec = client.player.getRotationVec(1.0F).normalize();
        double reach = client.player.getBlockInteractionRange() + 0.35D;
        Vec3d reachEnd = eyePos.add(lookVec.multiply(reach));
        Box targetBox = new Box(target);
        Box supportBox = new Box(target.down());

        boolean hitsBoxes = targetBox.expand(0.35D).raycast(eyePos, reachEnd).isPresent()
            || supportBox.expand(0.35D).raycast(eyePos, reachEnd).isPresent();

        if (hitsBoxes) {
            return true;
        }

        boolean isAssisted = "assisted".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean isPacket = "packet".equalsIgnoreCase(MorrowConfig.cameraMode);
        if (isPacket) {
            return true;
        }

        double minDot = isAssisted ? 0.82D : (MorrowConfig.autoCamera ? 0.88D : MAX_ALLOWED_AIM_DEV_DOT);

        if (aimTarget != null) {
            Vec3d toAim = aimTarget.subtract(eyePos).normalize();
            if (lookVec.dotProduct(toAim) >= minDot) {
                return true;
            }
        }

        Vec3d targetCenter = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        Vec3d toTarget = targetCenter.subtract(eyePos).normalize();
        if (lookVec.dotProduct(toTarget) >= minDot) {
            return true;
        }

        if (MorrowConfig.legitMode && !isAssisted && !MorrowConfig.autoCamera) {
            return false;
        }

        return false;
    }

    private boolean isTargetInFront(MinecraftClient client, BlockPos target) {
        if (client == null || client.player == null || target == null) return false;
        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetCenterH = new Vec3d(target.getX() + 0.5D, eyePos.y, target.getZ() + 0.5D);
        Vec3d toTargetH = targetCenterH.subtract(eyePos).normalize();
        Vec3d rotVec = client.player.getRotationVec(1.0F);
        Vec3d lookVecH = new Vec3d(rotVec.x, 0.0, rotVec.z).normalize();
        return lookVecH.dotProduct(toTargetH) >= 0.40D;
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

        Vec3d targetCenter = new Vec3d(target.getX() + 0.5D, target.getY() + 0.62D, target.getZ() + 0.5D);
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
            return 45;
        } else if (MorrowConfig.preset == MorrowConfig.PRESET_SAFE) {
            return 110;
        }
        return 65;
    }

    private int getRandomDelay() {
        int min = MorrowConfig.getMinDelayMs();
        int max = MorrowConfig.getMaxDelayMs();
        if (!MorrowConfig.randomDelay || min >= max) return Math.max(50, min);
        double sampled = (min + max) * 0.5 + ThreadLocalRandom.current().nextGaussian() * (max - min) / 6.0;
        return Math.max(50, (int) Math.round(MathHelper.clamp(sampled, min, max)));
    }

    private int getDynamicPlacementDelay(PlacementJob job) {
        if (job == null) return getRandomDelay();
        if (MorrowConfig.preset == MorrowConfig.PRESET_LEARNED) {
            ArcMotionProfile prof = ArcMotionProfile.getInstance();
            if (job.stage == Stage.PLACE_RAIL) {
                return sampleLearnedDelay(prof.getLearnedPlacementDelayRailMs(), prof);
            } else if (job.stage == Stage.PLACE_CART) {
                return sampleLearnedDelay(prof.getLearnedPlacementDelayCartMs(), prof);
            }
        }
        if (MorrowConfig.preset == MorrowConfig.PRESET_FAST) {
            int min = MorrowConfig.getMinDelayMs();
            int max = MorrowConfig.getMaxDelayMs();
            if (min >= max) return min;
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        float power = job.pullProgress;
        int flightTicks = Math.max(1, job.flightTicks);
        int scale = dev.nivorat.arc.internal.ArcDomain.i(1245457199);
        int flightTimeMs = flightTicks * scale;

        float pThresh = dev.nivorat.arc.internal.ArcDomain.f(1421770012);
        int timeThresh = dev.nivorat.arc.internal.ArcDomain.i(1245457297);
        if (power >= pThresh || flightTimeMs <= timeThresh) {
            int sub = dev.nivorat.arc.internal.ArcDomain.i(1245457170);
            int floorAlloc = dev.nivorat.arc.internal.ArcDomain.i(1245457161);
            int floorDelay = dev.nivorat.arc.internal.ArcDomain.i(1245457214);
            int allocated = Math.max(floorAlloc, (flightTimeMs - sub) / 2);
            int baseDelay = Math.min(allocated, MorrowConfig.getMinDelayMs());
            return Math.max(floorDelay, baseDelay);
        }

        return getRandomDelay();
    }

    private static Vec3d stepArrowPhysics(Vec3d currentVelocity) {
        return currentVelocity.multiply(ARROW_DRAG).add(0.0D, -ARROW_GRAVITY, 0.0D);
    }

    private static int sampleLearnedDelay(int mean, ArcMotionProfile profile) {
        if (!MorrowConfig.randomDelay) return Math.max(20, mean);
        double deviation = Math.max(2.0, (profile.getLearnedMaxDelayMs() - profile.getLearnedMinDelayMs()) / 6.0);
        double value = mean + ThreadLocalRandom.current().nextGaussian() * deviation;
        return (int) Math.round(MathHelper.clamp(value, Math.max(100, mean - deviation * 3), Math.min(300, mean + deviation * 3)));
    }

    private static float[] calculateLookAngles(Vec3d eyePos, Vec3d hitPos) {
        double dx = hitPos.x - eyePos.x;
        double dy = hitPos.y - eyePos.y;
        double dz = hitPos.z - eyePos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float bound = dev.nivorat.arc.internal.ArcDomain.f(691621423);
        float pitch = MathHelper.clamp((float) -Math.toDegrees(Math.atan2(dy, distXZ)), -bound, bound);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - bound;
        return new float[]{pitch, yaw};
    }

    private void startAimTowards(MinecraftClient client, Vec3d aimTarget, long durationMs) {
        if (client == null || client.player == null || aimTarget == null) return;
        Vec3d eyePos = client.player.getEyePos();
        float[] angles = calculateLookAngles(eyePos, aimTarget);
        float curPitch = client.player.getPitch();
        float curYaw = client.player.getYaw();
        float targetPitch = angles[0];
        float targetYaw = angles[1];

        boolean isAdaptive = MorrowConfig.adaptiveAim;
        if (isAdaptive) {
            ArcMotionProfile profile = ArcMotionProfile.getInstance();
            float learnedPitchDelta = profile.getLearnedActionPitchDelta();
            float maxPitchDelta = Math.min(Math.max(learnedPitchDelta, 1.5f), 6.0f);
            float desiredDeltaPitch = targetPitch - curPitch;
            targetPitch = curPitch + MathHelper.clamp(desiredDeltaPitch, -maxPitchDelta, maxPitchDelta);
        }

        float deltaPitch = Math.abs(targetPitch - curPitch);
        float deltaYaw = Math.abs(MathHelper.wrapDegrees(targetYaw - curYaw));
        if (deltaPitch < 2.0f && deltaYaw < 2.0f) {
            return;
        }
        long baseDuration = isAdaptive ? ArcMotionProfile.getInstance().getLearnedCameraSmoothness() : durationMs;
        long effDuration = Math.min(baseDuration, Math.max(45L, (long) (Math.sqrt(deltaPitch * deltaPitch + deltaYaw * deltaYaw) * 3.5)));
        cameraInterpolator.start(
            curPitch, targetPitch,
            curYaw, targetYaw,
            effDuration,
            MorrowConfig.cameraRandomness / 100.0f,
            MorrowConfig.cameraCurve / 100.0f,
            MorrowConfig.cameraMouseGcd
        );
    }

    private void syncLookForPlacement(MinecraftClient client, Vec3d hitPosition) {
        if (client == null || client.player == null) return;
        float[] angles = calculateLookAngles(client.player.getEyePos(), hitPosition);
        float targetPitch = angles[0];
        float targetYaw = angles[1];

        boolean isPacket = "packet".equalsIgnoreCase(MorrowConfig.cameraMode);
        boolean needsPacketLook = isPacket || (MorrowConfig.autonomousPlacement && !"auto".equalsIgnoreCase(MorrowConfig.cameraMode));
        if (needsPacketLook) {
            if (client.getNetworkHandler() != null) {
                double gcd = ArcCameraInterpolator.calculateMouseGcd(client);
                float curYaw = client.player.getYaw();
                float curPitch = client.player.getPitch();
                float deltaYaw = MathHelper.wrapDegrees(targetYaw - curYaw);
                float deltaPitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f) - curPitch;

                long stepsYaw = Math.round(deltaYaw / gcd);
                long stepsPitch = Math.round(deltaPitch / gcd);

                float quantYaw = (float) (curYaw + stepsYaw * gcd);
                float quantPitch = MathHelper.clamp((float) (curPitch + stepsPitch * gcd), -90.0f, 90.0f);

                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    quantYaw, quantPitch, client.player.isOnGround(), client.player.horizontalCollision
                ));
            }
        }
    }

    private boolean interactAtTop(MinecraftClient client, BlockPos blockPos) {
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d hitPosition = calculateExactFaceHit(client.player.getEyePos(), lookVec, blockPos);
        syncLookForPlacement(client, hitPosition);
        BlockHitResult result = new BlockHitResult(hitPosition, Direction.UP, blockPos, false);
        var action = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
        if (action.isAccepted()) client.player.swingHand(Hand.MAIN_HAND);
        return action.isAccepted();
    }

    private boolean interactOnRail(MinecraftClient client, BlockPos railPos, Hand hand) {
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d topPoint = calculateExactFaceHit(client.player.getEyePos(), lookVec, railPos.down());
        Vec3d hitPosition = new Vec3d(topPoint.x, railPos.getY() + 0.1D, topPoint.z);
        syncLookForPlacement(client, hitPosition);
        BlockHitResult result = new BlockHitResult(hitPosition, Direction.UP, railPos, false);
        var action = client.interactionManager.interactBlock(client.player, hand, result);
        if (action.isAccepted()) {
            client.player.swingHand(hand);
            activity.client.module.service.CartStateService.notifyCartPlaced(railPos);
        }
        return action.isAccepted();
    }

    private Vec3d calculateExactFaceHit(Vec3d eyePos, Vec3d lookVec, BlockPos supportPos) {
        double targetY = supportPos.getY() + 1.0D;
        double dy = lookVec.y;
        if (Math.abs(dy) > 1.0E-5) {
            double t = (targetY - eyePos.y) / dy;
            if (t > 0.0D) {
                double hitX = eyePos.x + lookVec.x * t;
                double hitZ = eyePos.z + lookVec.z * t;
                double minX = supportPos.getX() + HIT_FACE_INSET;
                double maxX = supportPos.getX() + 1.0D - HIT_FACE_INSET;
                double minZ = supportPos.getZ() + HIT_FACE_INSET;
                double maxZ = supportPos.getZ() + 1.0D - HIT_FACE_INSET;
                if (hitX >= minX && hitX <= maxX && hitZ >= minZ && hitZ <= maxZ) {
                    return new Vec3d(hitX, targetY, hitZ);
                }
            }
        }
        return nearestTopPoint(eyePos, supportPos);
    }

    private Vec3d nearestTopPoint(Vec3d origin, BlockPos support) {
        double x = MathHelper.clamp(origin.x, support.getX() + HIT_FACE_INSET, support.getX() + 1.0D - HIT_FACE_INSET);
        double z = MathHelper.clamp(origin.z, support.getZ() + HIT_FACE_INSET, support.getZ() + 1.0D - HIT_FACE_INSET);
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
            net.fabricmc.pack.api.CombatLockManager.setLock(S_CART_ACTIVE, false);
            return;
        }

        long now = System.currentTimeMillis();
        long heldTime = now - activeJob.lastSlotSwitchTimeMs;
        int minSafeHold = getMinSafeSlotHoldMs();

        if (heldTime < minSafeHold && client != null && client.player != null
            && client.player.getInventory().getSelectedSlot() != activeJob.originalSlot) {
            activeJob.stage = Stage.RESTORE_SLOT;
            activeJob.scheduledTimeMs = activeJob.lastSlotSwitchTimeMs + minSafeHold
                + ThreadLocalRandom.current().nextInt(8, 20);
            return;
        }

        placementRunning = false;
        net.fabricmc.pack.api.CombatLockManager.setLock(S_CART_ACTIVE, false);
        lastPlacementTime = now;
        if (client != null && client.player != null) {
            selectSlot(client, activeJob.originalSlot);
            if (MorrowConfig.autoCamera) {
                if (MorrowConfig.cameraReturn) {
                    cameraInterpolator.startReturn(
                        activeJob.originalPitch, activeJob.originalYaw,
                        MorrowConfig.cameraReturnSmoothnessMs,
                        MorrowConfig.cameraRandomness / 100.0f,
                        MorrowConfig.cameraCurve / 100.0f,
                        MorrowConfig.cameraMouseGcd
                    );
                } else {
                    cameraInterpolator.reset();
                }
            } else if ("packet".equalsIgnoreCase(MorrowConfig.cameraMode) || (MorrowConfig.autonomousPlacement && !"auto".equalsIgnoreCase(MorrowConfig.cameraMode))) {
                if (client.getNetworkHandler() != null) {
                    double gcd = ArcCameraInterpolator.calculateMouseGcd(client);
                    float curYaw = client.player.getYaw();
                    float curPitch = client.player.getPitch();
                    float deltaYaw = MathHelper.wrapDegrees(activeJob.originalYaw - curYaw);
                    float deltaPitch = MathHelper.clamp(activeJob.originalPitch, -90.0f, 90.0f) - curPitch;

                    long stepsYaw = Math.round(deltaYaw / gcd);
                    long stepsPitch = Math.round(deltaPitch / gcd);

                    float quantYaw = (float) (curYaw + stepsYaw * gcd);
                    float quantPitch = MathHelper.clamp((float) (curPitch + stepsPitch * gcd), -90.0f, 90.0f);

                    client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        quantYaw, quantPitch, client.player.isOnGround(), client.player.horizontalCollision
                    ));
                }
            }
        }
        activeJob = null;
    }

    private void cancelJob(MinecraftClient client) {
        cameraInterpolator.reset();
        if (activeJob != null && client != null && client.player == owner && client.world == ownerWorld) {
            int selected = client.player.getInventory().getSelectedSlot();
            if (selected == activeJob.railSlot || selected == activeJob.minecartSlot) {
                SafeSlotManager.restoreSlot(client, activeJob.originalSlot);
            }
        }
        activeJob = null;
        owner = null;
        ownerWorld = null;
        placementRunning = false;
        net.fabricmc.pack.api.CombatLockManager.setLock(S_CART_ACTIVE, false);
    }

    private void resetBowTracking() {
        wasUsingBow = false;
        bowDrawTicks = 0;
        bowSlot = -1;
    }

    public void resetSession(MinecraftClient client) {
        cancelMacro(client);
        clearPendingShot();
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
        private final Vec3d aimTarget;
        private final int flightTicks;

        private TargetResolution(BlockPos target, Vec3d aimTarget, int flightTicks) {
            this.target = target;
            this.aimTarget = aimTarget;
            this.flightTicks = flightTicks;
        }
    }

    private static final class PlacementJob {
        private final BlockPos target;
        private final Vec3d aimTarget;
        private final int originalSlot;
        private final int railSlot;
        private final int minecartSlot;
        private final boolean useMainHand;
        private Stage stage;
        private final long startTimeMs;
        private long scheduledTimeMs;
        private long lastSlotSwitchTimeMs;
        private int railRetries;
        private final float originalPitch;
        private final float originalYaw;
        private final float pullProgress;
        private final int flightTicks;

        private PlacementJob(BlockPos target, Vec3d aimTarget, int originalSlot, int railSlot, int minecartSlot, boolean useMainHand, long startTimeMs, float originalPitch, float originalYaw, float pullProgress, int flightTicks) {
            this.target = target;
            this.aimTarget = aimTarget;
            this.originalSlot = originalSlot;
            this.railSlot = railSlot;
            this.minecartSlot = minecartSlot;
            this.useMainHand = useMainHand;
            this.stage = Stage.SELECT_RAIL;
            this.startTimeMs = startTimeMs;
            this.scheduledTimeMs = startTimeMs;
            this.lastSlotSwitchTimeMs = startTimeMs;
            this.railRetries = 0;
            this.originalPitch = originalPitch;
            this.originalYaw = originalYaw;
            this.pullProgress = pullProgress;
            this.flightTicks = flightTicks;
        }
    }
}
