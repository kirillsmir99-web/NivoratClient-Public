package dev.shader;

import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ShaderPassController {
    private static final int MIN_AXE_DURABILITY = 4;
    private static final String COMBO_LOCK_PROPERTY = "pvp.shield_combo_active";
    private static final int ABORT_COOLDOWN_TICKS = 8;
    private static final int POST_STRIKE_COOLDOWN_TICKS = 15;
    private static final int TARGET_REPEAT_SUPPRESS_TICKS = 30;
    private static final int MAX_SWAP_TICKS = 6;
    private static final int SEMI_AWAIT_TIMEOUT_TICKS = 25;
    private static final int MIN_SHIELD_WARMUP_TICKS = 5;
    private static final double FACING_MIN_DOT = -0.15D;

    private ClientPlayerEntity owner;
    private World ownerWorld;
    private boolean enabled = true;
    private Stage stage = Stage.IDLE;
    private UUID targetId;
    private int initialSlot = -1;
    private int activeAxeSlot = -1;
    private int stageTicks = 0;
    private int targetDelayTicks = 0;
    private int restoreDelayTicks = 0;
    private int cooldownTicks = 0;
    private long tickCounter = 0L;
    private UUID trackingShieldTargetId = null;
    private long shieldSeenStartTick = 0L;
    private UUID lastStruckTargetId = null;
    private long lastStrikeTick = 0L;

    public ShaderPassController() {}

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        if (!enabled) {
            clearState();
        }
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        try {
            if (world == null || !world.isClient() || !(entity instanceof PlayerEntity target)) {
                return ActionResult.PASS;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.player != player || !enabled) {
                return ActionResult.PASS;
            }

            if (stage == Stage.SEMI_AWAIT_HIT) {
                if (hand == Hand.MAIN_HAND && client.player.getInventory().getSelectedSlot() == activeAxeSlot
                        && (targetId == null || target.getUuid().equals(targetId))) {
                    lastStruckTargetId = target.getUuid();
                    lastStrikeTick = tickCounter;
                    stage = Stage.WAITING_RESTORE;
                    stageTicks = 0;
                    restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
                }
            }

            return ActionResult.PASS;
        } catch (Throwable t) {
            return ActionResult.PASS;
        }
    }

    public void tick(MinecraftClient client) {
        try {
            tickCounter++;

            if (client == null || client.player == null || client.world == null || client.interactionManager == null
                    || (stage != Stage.IDLE && (client.player != owner || client.world != ownerWorld))) {
                clearState();
                return;
            }

            if (!enabled || !client.player.isAlive() || client.currentScreen != null) {
                if (stage != Stage.IDLE) {
                    restoreWeapon(client);
                    clearState();
                }
                return;
            }

            if (cooldownTicks > 0) {
                cooldownTicks--;
            }

            if (ShaderPassConfig.abortOnManualSwitch && stage != Stage.IDLE) {
                int currentSlot = client.player.getInventory().getSelectedSlot();
                if (stage == Stage.SWAPPED_TO_AXE || stage == Stage.SEMI_AWAIT_HIT) {
                    if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot) {
                        finish(Math.max(ABORT_COOLDOWN_TICKS, ShaderPassConfig.cooldownTicks));
                        return;
                    }
                } else if (stage == Stage.WAITING_RESTORE) {
                    if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot && currentSlot != initialSlot) {
                        finish(Math.max(ABORT_COOLDOWN_TICKS, ShaderPassConfig.cooldownTicks));
                        return;
                    }
                }
            }

            switch (stage) {
                case IDLE -> handleIdle(client);
                case SWAPPED_TO_AXE -> handleSwappedToAxe(client);
                case WAITING_RESTORE -> handleWaitingRestore(client);
                case SEMI_AWAIT_HIT -> handleSemiAwaitHit(client);
            }
        } catch (Throwable t) {
            clearState();
        }
    }

    private void resetTracking() {
        trackingShieldTargetId = null;
        shieldSeenStartTick = 0L;
    }

    private void handleIdle(MinecraftClient client) {
        if (cooldownTicks > 0 || client.currentScreen != null || client.player == null) {
            resetTracking();
            return;
        }

        if (isLocalPlayerPrevented(client.player, client)) {
            resetTracking();
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked()) {
            resetTracking();
            return;
        }

        int axeSlot = findAxeHotbarSlot(client.player);
        if (axeSlot < 0) {
            resetTracking();
            return;
        }

        PlayerEntity target = findBlockingPlayer(client);
        if (target == null) {
            resetTracking();
            return;
        }

        if (lastStruckTargetId != null && lastStruckTargetId.equals(target.getUuid())) {
            if (tickCounter - lastStrikeTick < TARGET_REPEAT_SUPPRESS_TICKS) {
                return;
            }
            lastStruckTargetId = null;
        }

        if (trackingShieldTargetId == null || !trackingShieldTargetId.equals(target.getUuid())) {
            trackingShieldTargetId = target.getUuid();
            shieldSeenStartTick = tickCounter;
        }

        long ticksSeen = tickCounter - shieldSeenStartTick;
        int targetUseTime = target.getItemUseTime();
        if (targetUseTime < MIN_SHIELD_WARMUP_TICKS && ticksSeen < MIN_SHIELD_WARMUP_TICKS) {
            return;
        }

        long warmupTicks = Math.round(ShaderPassConfig.reactionDelaySec * 20.0D);
        if (warmupTicks > 0 && ticksSeen < warmupTicks) {
            return;
        }

        int chance = Math.max(0, Math.min(100, ShaderPassConfig.chance));
        if (chance < 100 && ThreadLocalRandom.current().nextInt(100) >= chance) {
            lastStruckTargetId = target.getUuid();
            lastStrikeTick = tickCounter;
            resetTracking();
            return;
        }

        trigger(client, target, axeSlot);
    }

    private boolean isLocalPlayerPrevented(ClientPlayerEntity player, MinecraftClient client) {
        if (player == null || !player.isAlive()) return true;
        if (client.currentScreen != null) return true;
        if (player.isBlocking() || player.isUsingItem()) return true;
        if (ShaderPassConfig.checkAirTime && isAirTimeExceeded()) return true;
        return player.isSpectator() || player.isSleeping() || player.hasVehicle() || player.isGliding();
    }

    private static boolean isAirTimeExceeded() {
        int airTicks = activity.client.module.service.PlayerStateService.getAirTicks();
        double maxTicks = ShaderPassConfig.maxAirTimeSec * 20.0D;
        if (airTicks > maxTicks) {
            return true;
        }
        long maxDurationMs = Math.round(ShaderPassConfig.maxAirTimeSec * 1000.0D);
        return activity.client.module.service.PlayerStateService.getAirDurationMs() > maxDurationMs;
    }

    private static boolean isShield(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (stack.getItem() instanceof ShieldItem || stack.isOf(Items.SHIELD));
    }

    private void trigger(MinecraftClient client, PlayerEntity target, int axeSlot) {
        owner = client.player;
        ownerWorld = client.world;
        initialSlot = client.player.getInventory().getSelectedSlot();
        targetId = target.getUuid();
        activeAxeSlot = axeSlot;
        net.fabricmc.pack.api.CombatLockManager.setLock(COMBO_LOCK_PROPERTY, true);

        if (ShaderPassConfig.mode == ShaderPassConfig.MODE_SEMI_AUTO) {
            if (initialSlot != axeSlot) {
                selectSlot(client, axeSlot);
            }
            stage = Stage.SEMI_AWAIT_HIT;
            stageTicks = 0;
            return;
        }

        if (initialSlot == axeSlot) {
            executeStrike(client);
            return;
        }

        selectSlot(client, axeSlot);
        stage = Stage.SWAPPED_TO_AXE;
        stageTicks = 0;
        targetDelayTicks = 1;
    }

    private void handleSwappedToAxe(MinecraftClient client) {
        stageTicks++;

        if (stageTicks > MAX_SWAP_TICKS) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (client.player.getInventory().getSelectedSlot() != activeAxeSlot) {
            selectSlot(client, activeAxeSlot);
            return;
        }

        if (stageTicks < targetDelayTicks) {
            return;
        }

        executeStrike(client);
    }

    private void handleSemiAwaitHit(MinecraftClient client) {
        stageTicks++;
        if (stageTicks > SEMI_AWAIT_TIMEOUT_TICKS) {
            restoreWeapon(client);
            finish(Math.max(POST_STRIKE_COOLDOWN_TICKS, ShaderPassConfig.cooldownTicks));
            return;
        }

        PlayerEntity target = getTarget(client);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (stageTicks > 12 && !isTargetShielding(target)) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (client.player.handSwinging && client.player.getInventory().getSelectedSlot() == activeAxeSlot) {
            lastStruckTargetId = target.getUuid();
            lastStrikeTick = tickCounter;
            stage = Stage.WAITING_RESTORE;
            stageTicks = 0;
            restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
        }
    }

    private void executeStrike(MinecraftClient client) {
        PlayerEntity target = getTarget(client);
        if (target == null || target == client.player || !target.isAlive() || target.isSpectator()
                || target.getEntityWorld() != client.world) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (!isTargetShielding(target)) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        long ticksSeen = tickCounter - shieldSeenStartTick;
        if (target.getItemUseTime() < MIN_SHIELD_WARMUP_TICKS && ticksSeen < MIN_SHIELD_WARMUP_TICKS) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (!CombatRaytraceGuard.hasLineOfSight(client.player, target)) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        Vec3d eyePos = client.player.getEyePos();
        Box box = target.getBoundingBox();
        double cx = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double cy = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
        double cz = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        double maxStrikeReach = Math.max(3.6D, ShaderPassConfig.triggerDistance + 0.5D);
        if (eyePos.squaredDistanceTo(cx, cy, cz) > maxStrikeReach * maxStrikeReach) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (client.player.getInventory().getSelectedSlot() != activeAxeSlot) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        if (!client.player.getMainHandStack().isIn(ItemTags.AXES)) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        try {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            lastStruckTargetId = target.getUuid();
            lastStrikeTick = tickCounter;
        } catch (Throwable t) {
            restoreWeapon(client);
            finish(ABORT_COOLDOWN_TICKS);
            return;
        }

        stage = Stage.WAITING_RESTORE;
        stageTicks = 0;
        restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
    }

    private void handleWaitingRestore(MinecraftClient client) {
        stageTicks++;
        if (stageTicks >= restoreDelayTicks) {
            restoreWeapon(client);
            finish(Math.max(POST_STRIKE_COOLDOWN_TICKS, ShaderPassConfig.cooldownTicks));
        }
    }

    private int getRestoreDelayTicks() {
        if (ShaderPassConfig.legitMode) {
            long delayMs = GaussianTimingEngine.getShieldBreakerRestoreDelay();
            return Math.max(2, (int) Math.round(delayMs / 50.0D));
        }
        int ticks = GaussianTimingEngine.sampleActionTicks(ShaderPassConfig.restoreDelayMs, ShaderPassConfig.randomDelay);
        return Math.max(1, ticks);
    }

    private PlayerEntity findBlockingPlayer(MinecraftClient client) {
        PlayerEntity crosshair = activity.client.module.service.TargetCacheService.getCrosshairPlayerTarget(client);
        if (crosshair != null && isValidTarget(client, crosshair)) {
            return crosshair;
        }
        if (client.targetedEntity instanceof PlayerEntity target && isValidTarget(client, target)) {
            return target;
        }
        if (client.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof PlayerEntity target
                && isValidTarget(client, target)) {
            return target;
        }
        return findTargetAlongRay(client);
    }

    private boolean isValidTarget(MinecraftClient client, PlayerEntity target) {
        if (target == null || client.player == null || target == client.player
                || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (!isTargetShielding(target)) {
            return false;
        }
        if (!isFacingUs(client.player, target, FACING_MIN_DOT)) {
            return false;
        }
        return canReach(client, target, ShaderPassConfig.triggerDistance);
    }

    private static boolean isFacingUs(PlayerEntity me, PlayerEntity target, double minDot) {
        Vec3d toMe = new Vec3d(me.getX() - target.getX(), 0.0, me.getZ() - target.getZ());
        if (toMe.lengthSquared() < 1.0E-4) {
            return true;
        }
        toMe = toMe.normalize();
        Vec3d headLook = target.getRotationVector(0.0F, target.getHeadYaw());
        if (headLook.dotProduct(toMe) > minDot) {
            return true;
        }
        Vec3d bodyLook = target.getRotationVector(0.0F, target.getYaw());
        return bodyLook.dotProduct(toMe) > minDot;
    }

    private PlayerEntity findTargetAlongRay(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        double maxDist = Math.max(3.2D, Math.max(client.player.getEntityInteractionRange(), ShaderPassConfig.triggerDistance));
        Vec3d reachEnd = eyePos.add(lookVec.multiply(maxDist + 0.2D));

        PlayerEntity best = null;
        double closestDistSq = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !isValidTarget(client, player)) {
                continue;
            }
            Box box = player.getBoundingBox();
            var hit = box.expand(0.35D).raycast(eyePos, reachEnd);
            double dSq;
            if (hit.isPresent()) {
                dSq = eyePos.squaredDistanceTo(hit.get());
            } else {
                Vec3d center = box.getCenter();
                Vec3d toCenter = center.subtract(eyePos).normalize();
                if (lookVec.dotProduct(toCenter) > 0.55D) {
                    dSq = eyePos.squaredDistanceTo(center);
                } else {
                    continue;
                }
            }
            if (dSq <= maxDist * maxDist && dSq < closestDistSq
                    && CombatRaytraceGuard.hasLineOfSight(client.player, player)) {
                closestDistSq = dSq;
                best = player;
            }
        }
        return best;
    }

    private boolean canReach(MinecraftClient client, PlayerEntity target, double maxReach) {
        if (target == null || target == client.player || !target.isAlive() || client.player == null) {
            return false;
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            return true;
        }
        if (client.targetedEntity == target) {
            return true;
        }

        Vec3d eyePos = client.player.getEyePos();
        double cappedReach = Math.max(3.2D, Math.max(client.player.getEntityInteractionRange(), maxReach));

        if (!CombatRaytraceGuard.hasLineOfSight(client.player, target)) {
            return false;
        }

        Box box = target.getBoundingBox();
        double cx = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
        double cy = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
        double cz = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));
        if (eyePos.squaredDistanceTo(cx, cy, cz) > cappedReach * cappedReach) {
            return false;
        }

        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(cappedReach + 0.2D));
        if (box.expand(0.35D).raycast(eyePos, reachEnd).isPresent()) {
            return true;
        }
        Vec3d toCenter = box.getCenter().subtract(eyePos).normalize();
        return lookVec.dotProduct(toCenter) > 0.55D;
    }

    private int findAxeHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = activity.client.module.service.InventoryScanService.findAxeSlot(player);
        if (cached >= 0 && cached < 9) {
            ItemStack item = player.getInventory().getStack(cached);
            if (item.isIn(ItemTags.AXES) && (!item.isDamageable()
                    || item.getMaxDamage() - item.getDamage() >= MIN_AXE_DURABILITY)) {
                return cached;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isIn(ItemTags.AXES)
                    && (!stack.isDamageable() || stack.getMaxDamage() - stack.getDamage() >= MIN_AXE_DURABILITY)) {
                return slot;
            }
        }
        return -1;
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client.player == null || slot < 0 || slot >= 9) {
            return;
        }
        SafeSlotManager.selectSlot(client, slot);
    }

    private void restoreWeapon(MinecraftClient client) {
        if (client != null && client.player == owner && client.world == ownerWorld && owner != null
                && initialSlot >= 0
                && owner.getInventory().getSelectedSlot() == activeAxeSlot) {
            SafeSlotManager.restoreSlot(client, initialSlot);
        }
    }

    private PlayerEntity getTarget(MinecraftClient client) {
        if (targetId == null || client.world == null) {
            return null;
        }
        return client.world.getPlayerByUuid(targetId);
    }

    private boolean isTargetShielding(PlayerEntity target) {
        return activity.client.module.service.TargetCacheService.isTargetShielding(target);
    }

    public void reset() {
        restoreWeapon(MinecraftClient.getInstance());
        clearState();
    }

    private void clearState() {
        stage = Stage.IDLE;
        owner = null;
        ownerWorld = null;
        targetId = null;
        initialSlot = -1;
        activeAxeSlot = -1;
        stageTicks = 0;
        targetDelayTicks = 0;
        restoreDelayTicks = 0;
        resetTracking();
        net.fabricmc.pack.api.CombatLockManager.setLock(COMBO_LOCK_PROPERTY, false);
    }

    private void finish(int cooldown) {
        clearState();
        cooldownTicks = Math.max(1, cooldown);
    }

    private enum Stage {
        IDLE,
        SWAPPED_TO_AXE,
        WAITING_RESTORE,
        SEMI_AWAIT_HIT
    }
}
