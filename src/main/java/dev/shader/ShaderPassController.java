package dev.shader;

import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
    private static final double MAX_COMBAT_REACH = 2.85D;

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
    private UUID trackingShieldTargetId = null;
    private long shieldSeenStartTimeMs = 0L;

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
                    stage = Stage.WAITING_RESTORE;
                    stageTicks = 0;
                    restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
                }
                return ActionResult.PASS;
            }

            return ActionResult.PASS;
        } catch (Throwable t) {
            return ActionResult.PASS;
        }
    }

    public void tick(MinecraftClient client) {
        try {
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
                if (stage == Stage.SWAPPED_TO_AXE) {
                    if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot) {
                        clearState();
                        cooldownTicks = Math.max(4, ShaderPassConfig.cooldownTicks);
                        return;
                    }
                } else if (stage == Stage.WAITING_RESTORE) {
                    if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot && currentSlot != initialSlot) {
                        clearState();
                        cooldownTicks = Math.max(4, ShaderPassConfig.cooldownTicks);
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

    private void handleIdle(MinecraftClient client) {
        if (cooldownTicks > 0 || client.currentScreen != null || client.player == null) {
            trackingShieldTargetId = null;
            shieldSeenStartTimeMs = 0L;
            return;
        }

        if (isPlayerBusy(client.player)) {
            trackingShieldTargetId = null;
            shieldSeenStartTimeMs = 0L;
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked()) {
            trackingShieldTargetId = null;
            shieldSeenStartTimeMs = 0L;
            return;
        }

        if (dev.particle.ParticlePhysicsConfig.enabled && client.player != null) {
            if (!client.player.isOnGround() || dev.particle.ParticlePhysicsConfig.airTimeSec <= 0.05D) {
                trackingShieldTargetId = null;
                shieldSeenStartTimeMs = 0L;
                return;
            }
        }

        int axeSlot = findAxeHotbarSlot(client.player);
        if (axeSlot < 0) {
            trackingShieldTargetId = null;
            shieldSeenStartTimeMs = 0L;
            return;
        }

        PlayerEntity target = findBlockingPlayer(client);
        if (target == null) {
            trackingShieldTargetId = null;
            shieldSeenStartTimeMs = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (trackingShieldTargetId == null || !trackingShieldTargetId.equals(target.getUuid())) {
            trackingShieldTargetId = target.getUuid();
            shieldSeenStartTimeMs = now;
        }

        long reactionDelayMs = (long) (ShaderPassConfig.reactionDelaySec * 1000.0);
        if (reactionDelayMs > 0 && (now - shieldSeenStartTimeMs) < reactionDelayMs) {
            return;
        }

        if (ShaderPassConfig.chance < 100) {
            if (ThreadLocalRandom.current().nextInt(100) >= ShaderPassConfig.chance) {
                cooldownTicks = 2;
                return;
            }
        }

        trigger(client, target, axeSlot);
    }

    private void trigger(MinecraftClient client, PlayerEntity target, int axeSlot) {
        owner = client.player;
        ownerWorld = client.world;
        initialSlot = client.player.getInventory().getSelectedSlot();
        targetId = target.getUuid();
        activeAxeSlot = axeSlot;
        net.fabricmc.pack.api.CombatLockManager.setLock(COMBO_LOCK_PROPERTY, true);

        if (initialSlot == axeSlot) {
            executeStrike(client);
            return;
        }

        selectSlot(client, axeSlot);
        stage = Stage.SWAPPED_TO_AXE;
        stageTicks = 0;
        targetDelayTicks = Math.max(1, getSwitchDelayTicks());
    }

    private void handleSwappedToAxe(MinecraftClient client) {
        stageTicks++;
        if (stageTicks >= targetDelayTicks) {
            executeStrike(client);
        }
    }

    private void executeStrike(MinecraftClient client) {
        PlayerEntity target = getTarget(client);
        if (target == null || !isValidTarget(client, target)) {
            restoreWeapon(client);
            finish(1);
            return;
        }

        if (client.player.getInventory().getSelectedSlot() != activeAxeSlot) {
            selectSlot(client, activeAxeSlot);
            return;
        }

        if (ShaderPassConfig.mode == ShaderPassConfig.MODE_SEMI_AUTO) {
            stage = Stage.SEMI_AWAIT_HIT;
            stageTicks = 0;
            return;
        }

        if (!client.player.getMainHandStack().isIn(ItemTags.AXES)) {
            restoreWeapon(client);
            finish(1);
            return;
        }
        try {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
        } catch (Throwable t) {
            restoreWeapon(client);
            finish(1);
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
            finish(ShaderPassConfig.cooldownTicks);
        }
    }

    private void handleSemiAwaitHit(MinecraftClient client) {
        stageTicks++;
        if (stageTicks > 25) {
            restoreWeapon(client);
            finish(ShaderPassConfig.cooldownTicks);
            return;
        }

        PlayerEntity target = getTarget(client);
        if (target == null || !target.isAlive() || !canReach(client, target, ShaderPassConfig.triggerDistance)) {
            restoreWeapon(client);
            finish(ShaderPassConfig.cooldownTicks);
        }
    }

    private int getSwitchDelayTicks() {
        return GaussianTimingEngine.sampleActionTicks(ShaderPassConfig.switchDelayMs, ShaderPassConfig.randomDelay);
    }

    private int getRestoreDelayTicks() {
        return GaussianTimingEngine.sampleActionTicks(ShaderPassConfig.restoreDelayMs, ShaderPassConfig.randomDelay);
    }

    private boolean isPlayerBusy(ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return true;
        }
        return activity.client.module.service.PlayerStateService.isBusy(player);
    }

    private PlayerEntity findBlockingPlayer(MinecraftClient client) {
        PlayerEntity crosshair = activity.client.module.service.TargetCacheService.getCrosshairPlayerTarget(client);
        if (crosshair != null && isValidTarget(client, crosshair)) {
            return crosshair;
        }

        if (client.targetedEntity instanceof PlayerEntity target) {
            if (isValidTarget(client, target)) {
                return target;
            }
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof PlayerEntity target) {
            if (isValidTarget(client, target)) {
                return target;
            }
        }

        return findTargetAlongRay(client);
    }

    private boolean isValidTarget(MinecraftClient client, PlayerEntity target) {
        if (target == null || target == client.player || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (!isTargetShielding(target)) {
            return false;
        }
        return canReach(client, target, ShaderPassConfig.triggerDistance);
    }

    private PlayerEntity findTargetAlongRay(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        double maxDist = Math.min(client.player.getEntityInteractionRange() - 0.05D, ShaderPassConfig.triggerDistance);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(maxDist));

        PlayerEntity best = null;
        double closestDistSq = Double.MAX_VALUE;

        for (PlayerEntity player : client.world.getPlayers()) {
            if (!isValidTarget(client, player)) {
                continue;
            }
            if (player.squaredDistanceTo(client.player) > (maxDist + 1.0) * (maxDist + 1.0)) {
                continue;
            }
            Box box = player.getBoundingBox();
            var hit = box.raycast(eyePos, reachEnd);
            if (hit.isPresent()) {
                double dSq = eyePos.squaredDistanceTo(hit.get());
                if (dSq <= maxDist * maxDist && dSq < closestDistSq) {
                    if (CombatRaytraceGuard.hasLineOfSight(client.player, player)) {
                        closestDistSq = dSq;
                        best = player;
                    }
                }
            }
        }
        return best;
    }

    private boolean canReach(MinecraftClient client, PlayerEntity target, double maxReach) {
        if (target == null || target == client.player || !target.isAlive() || client.player == null) {
            return false;
        }

        Vec3d eyePos = client.player.getEyePos();
        double cappedReach = Math.min(client.player.getEntityInteractionRange() - 0.05D, maxReach);

        if (!CombatRaytraceGuard.hasLineOfSight(client.player, target)) {
            return false;
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            return eyePos.squaredDistanceTo(ehr.getPos()) <= cappedReach * cappedReach;
        }

        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(cappedReach));
        Box box = target.getBoundingBox();
        var hit = box.raycast(eyePos, reachEnd);
        if (hit.isPresent()) {
            return eyePos.squaredDistanceTo(hit.get()) <= cappedReach * cappedReach;
        }

        return false;
    }

    private int findAxeHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = activity.client.module.service.InventoryScanService.findAxeSlot(player);
        if (cached >= 0) {
            ItemStack item = player.getInventory().getStack(cached);
            if (item.isIn(ItemTags.AXES) && (!item.isDamageable()
                    || item.getMaxDamage() - item.getDamage() >= MIN_AXE_DURABILITY)) return cached;
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
        trackingShieldTargetId = null;
        shieldSeenStartTimeMs = 0L;
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
