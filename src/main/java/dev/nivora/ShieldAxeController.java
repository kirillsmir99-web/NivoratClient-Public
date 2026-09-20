package dev.nivora;

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

public final class ShieldAxeController {
    private static final int MIN_AXE_DURABILITY = 4;
    private static final String COMBO_LOCK_PROPERTY = "pvp.shield_combo_active";
    private static final double MAX_COMBAT_REACH = 2.85D;

    private boolean enabled = true;
    private Stage stage = Stage.IDLE;
    private UUID targetId;
    private int initialSlot = -1;
    private int activeAxeSlot = -1;
    private int stageTicks = 0;
    private int targetDelayTicks = 0;
    private int restoreDelayTicks = 0;
    private int cooldownTicks = 0;

    public ShieldAxeController() {}

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
        if (!world.isClient() || !(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player != player || !enabled) {
            return ActionResult.PASS;
        }

        if (stage == Stage.SEMI_AWAIT_HIT) {
            if (targetId == null || target.getUuid().equals(targetId)) {
                stage = Stage.WAITING_RESTORE;
                stageTicks = 0;
                restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
            }
            return ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            clearState();
            return;
        }

        if (!enabled || !client.player.isAlive()) {
            if (stage != Stage.IDLE) {
                restoreWeapon(client);
                clearState();
            }
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (ShieldBreakerConfig.abortOnManualSwitch && stage != Stage.IDLE) {
            int currentSlot = client.player.getInventory().getSelectedSlot();
            if (stage == Stage.SWAPPED_TO_AXE) {
                if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot) {
                    clearState();
                    cooldownTicks = Math.max(4, ShieldBreakerConfig.cooldownTicks);
                    return;
                }
            } else if (stage == Stage.WAITING_RESTORE) {
                if (activeAxeSlot >= 0 && currentSlot != activeAxeSlot && currentSlot != initialSlot) {
                    clearState();
                    cooldownTicks = Math.max(4, ShieldBreakerConfig.cooldownTicks);
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
    }

    private void handleIdle(MinecraftClient client) {
        if (cooldownTicks > 0 || client.currentScreen != null || client.player == null) {
            return;
        }

        if (isPlayerBusy(client.player)) {
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.SUNDER)) {
            return;
        }

        if (dev.sunder.SunderConfig.enabled && !client.player.isOnGround()) {
            return;
        }

        int axeSlot = findAxeHotbarSlot(client.player);
        if (axeSlot < 0) {
            return;
        }

        PlayerEntity target = findBlockingPlayer(client);
        if (target == null) {
            return;
        }

        if (ShieldBreakerConfig.chance < 100) {
            if (ThreadLocalRandom.current().nextInt(100) >= ShieldBreakerConfig.chance) {
                cooldownTicks = 2;
                return;
            }
        }

        trigger(client, target, axeSlot);
    }

    private void trigger(MinecraftClient client, PlayerEntity target, int axeSlot) {
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

        if (ShieldBreakerConfig.mode == ShieldBreakerConfig.MODE_SEMI_AUTO) {
            stage = Stage.SEMI_AWAIT_HIT;
            stageTicks = 0;
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);

        stage = Stage.WAITING_RESTORE;
        stageTicks = 0;
        restoreDelayTicks = Math.max(1, getRestoreDelayTicks());
    }

    private void handleWaitingRestore(MinecraftClient client) {
        stageTicks++;
        if (stageTicks >= restoreDelayTicks) {
            restoreWeapon(client);
            finish(ShieldBreakerConfig.cooldownTicks);
        }
    }

    private void handleSemiAwaitHit(MinecraftClient client) {
        stageTicks++;
        if (stageTicks > 25) {
            restoreWeapon(client);
            finish(ShieldBreakerConfig.cooldownTicks);
            return;
        }

        PlayerEntity target = getTarget(client);
        if (target == null || !target.isAlive() || !canReach(client, target, ShieldBreakerConfig.triggerDistance)) {
            restoreWeapon(client);
            finish(ShieldBreakerConfig.cooldownTicks);
        }
    }

    private int getSwitchDelayTicks() {
        if (ShieldBreakerConfig.randomDelay) {
            long randomized = GaussianTimingEngine.getShieldBreakerSwitchDelay();
            return Math.max(1, (int) Math.round(randomized / 50.0D));
        }
        return Math.max(1, (int) Math.round(ShieldBreakerConfig.switchDelayMs / 50.0D));
    }

    private int getRestoreDelayTicks() {
        if (ShieldBreakerConfig.randomDelay) {
            long randomized = GaussianTimingEngine.getShieldBreakerRestoreDelay();
            return Math.max(1, (int) Math.round(randomized / 50.0D));
        }
        return Math.max(1, (int) Math.round(ShieldBreakerConfig.restoreDelayMs / 50.0D));
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
        return canReach(client, target, ShieldBreakerConfig.triggerDistance);
    }

    private PlayerEntity findTargetAlongRay(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        double maxDist = Math.min(MAX_COMBAT_REACH, ShieldBreakerConfig.triggerDistance);
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
        double cappedReach = Math.min(MAX_COMBAT_REACH, maxReach);

        if (!CombatRaytraceGuard.hasLineOfSight(client.player, target)) {
            return false;
        }

        if (client.targetedEntity == target) {
            return eyePos.squaredDistanceTo(target.getEyePos()) <= cappedReach * cappedReach;
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            return eyePos.squaredDistanceTo(ehr.getPos()) <= cappedReach * cappedReach;
        }

        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(cappedReach));
        Box box = target.getBoundingBox().expand(0.1D);
        var hit = box.raycast(eyePos, reachEnd);
        if (hit.isPresent()) {
            return eyePos.squaredDistanceTo(hit.get()) <= cappedReach * cappedReach;
        }

        return false;
    }

    private int findAxeHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = activity.client.module.service.InventoryScanService.findAxeSlot(player);
        if (cached >= 0) return cached;
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
        if (client.player != null && initialSlot >= 0 && initialSlot < 9) {
            selectSlot(client, initialSlot);
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
        clearState();
    }

    private void clearState() {
        stage = Stage.IDLE;
        targetId = null;
        initialSlot = -1;
        activeAxeSlot = -1;
        stageTicks = 0;
        targetDelayTicks = 0;
        restoreDelayTicks = 0;
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
