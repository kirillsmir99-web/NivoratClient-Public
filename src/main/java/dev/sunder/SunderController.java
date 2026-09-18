package dev.sunder;

import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
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
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SunderController {
    private static final int MIN_AXE_DURABILITY = 4;
    private static final Map<UUID, Long> SHIELD_DISABLED_TARGETS = new ConcurrentHashMap<>();
    private static final double MAX_COMBAT_REACH = 2.85D;

    private boolean enabled = true;
    private Stage stage = Stage.IDLE;
    private UUID targetId;
    private int initialSlot = -1;
    private int maceSlot = -1;
    private int axeSlot = -1;
    private int timer = 0;
    private int cooldownTimer = 0;
    private int airTicks = 0;

    public SunderController() {
    }

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

        if (stage == Stage.SEMI_AWAIT_AXE_HIT) {
            if (targetId == null || target.getUuid().equals(targetId)) {
                markShieldDisabled(target.getUuid());
                stage = Stage.SEMI_SELECT_MACE;
                timer = 1;
            }
            return ActionResult.PASS;
        }

        if (stage == Stage.SEMI_AWAIT_MACE_HIT) {
            if (targetId == null || target.getUuid().equals(targetId)) {
                stage = Stage.WAITING_RESTORE;
                timer = Math.max(2, getRestoreDelayTicks());
            }
            return ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            clearState();
            airTicks = 0;
            return;
        }

        if (client.player.isOnGround() || client.player.isTouchingWater() || client.player.isClimbing() || client.player.hasVehicle()) {
            airTicks = 0;
        } else {
            airTicks = Math.max(airTicks + 1, activity.client.module.service.PlayerStateService.getAirTicks());
        }

        if (!enabled || !client.player.isAlive()) {
            if (stage != Stage.IDLE) {
                restoreInitialSlot(client);
                clearState();
            }
            return;
        }

        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        switch (stage) {
            case IDLE -> handleIdle(client);
            case SELECT_AXE -> {
                if (axeSlot < 0 || axeSlot >= 9) {
                    finishCombo();
                    return;
                }
                selectSlot(client, axeSlot);
                stage = Stage.WAITING_AXE_STRIKE;
                timer = Math.max(2, getAxeDelayTicks());
            }
            case WAITING_AXE_STRIKE -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                executeAutoStrikeAxe(client);
            }
            case SELECT_MACE -> {
                if (maceSlot < 0 || maceSlot >= 9) {
                    finishCombo();
                    return;
                }
                selectSlot(client, maceSlot);
                stage = Stage.WAITING_MACE_STRIKE;
                timer = Math.max(2, getMaceDelayTicks());
            }
            case WAITING_MACE_STRIKE -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                executeAutoStrikeMace(client);
            }
            case SEMI_SELECT_AXE -> {
                selectSlot(client, axeSlot);
                stage = Stage.SEMI_AWAIT_AXE_HIT;
                timer = 30;
            }
            case SEMI_AWAIT_AXE_HIT -> {
                if (timer > 0) {
                    timer--;
                } else {
                    restoreInitialSlot(client);
                    finishCombo();
                    return;
                }
                PlayerEntity target = getTarget(client);
                if (target == null || !target.isAlive() || !canReach(client, target, SunderConfig.triggerDistance)) {
                    restoreInitialSlot(client);
                    finishCombo();
                }
            }
            case SEMI_SELECT_MACE -> {
                selectSlot(client, maceSlot);
                stage = Stage.SEMI_AWAIT_MACE_HIT;
                timer = 30;
            }
            case SEMI_AWAIT_MACE_HIT -> {
                if (timer > 0) {
                    timer--;
                } else {
                    restoreInitialSlot(client);
                    finishCombo();
                    return;
                }
                PlayerEntity target = getTarget(client);
                if (target == null || !target.isAlive() || !canReach(client, target, SunderConfig.triggerDistance)) {
                    restoreInitialSlot(client);
                    finishCombo();
                }
            }
            case WAITING_RESTORE -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                stage = Stage.RESTORE_SLOT;
            }
            case RESTORE_SLOT -> {
                restoreInitialSlot(client);
                finishCombo();
            }
        }
    }

    private void handleIdle(MinecraftClient client) {
        if (cooldownTimer > 0 || client.currentScreen != null || client.player == null) {
            return;
        }

        if (isPlayerBusy(client, client.player)) {
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked(net.fabricmc.pack.api.CombatLockManager.SHIELD_COMBO)) {
            return;
        }

        if (!isAirborneConditionMet(client.player)) {
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        if (!isHandledWeapon(mainHand)) {
            return;
        }

        int foundMace = findMaceHotbarSlot(client.player);
        int foundAxe = findAxeHotbarSlot(client.player);
        if (foundAxe < 0) {
            return;
        }
        if (foundMace < 0 && SunderConfig.mode != SunderConfig.MODE_FULL_AUTO) {
            return;
        }

        PlayerEntity target = findBlockingPlayer(client);
        if (target == null) {
            return;
        }

        if (SunderConfig.chance < 100) {
            if (ThreadLocalRandom.current().nextInt(100) >= SunderConfig.chance) {
                cooldownTimer = 15;
                return;
            }
        }

        trigger(client, target, foundAxe, foundMace);
    }

    private void trigger(MinecraftClient client, PlayerEntity target, int foundAxe, int foundMace) {
        initialSlot = client.player.getInventory().getSelectedSlot();
        axeSlot = foundAxe;
        maceSlot = foundMace;
        targetId = target.getUuid();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", true);

        if (SunderConfig.mode == SunderConfig.MODE_SEMI_AUTO) {
            stage = Stage.SEMI_SELECT_AXE;
            timer = 1;
            return;
        }

        stage = Stage.SELECT_AXE;
        timer = 1;
    }

    private void executeAutoStrikeAxe(MinecraftClient client) {
        PlayerEntity target = getTarget(client);
        if (target == null || !target.isAlive() || !canReach(client, target, SunderConfig.triggerDistance)) {
            restoreInitialSlot(client);
            finishCombo();
            return;
        }

        if (client.player.getInventory().getSelectedSlot() != axeSlot) {
            selectSlot(client, axeSlot);
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        markShieldDisabled(targetId);

        if (maceSlot >= 0) {
            stage = Stage.SELECT_MACE;
            timer = 1;
        } else {
            stage = Stage.WAITING_RESTORE;
            timer = Math.max(2, getRestoreDelayTicks());
        }
    }

    private void executeAutoStrikeMace(MinecraftClient client) {
        PlayerEntity target = getTarget(client);
        if (target != null && target.isAlive() && canReach(client, target, SunderConfig.triggerDistance)) {
            if (client.player.getInventory().getSelectedSlot() != maceSlot) {
                selectSlot(client, maceSlot);
                return;
            }
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
        }

        stage = Stage.WAITING_RESTORE;
        timer = Math.max(2, getRestoreDelayTicks());
    }

    private int getAxeDelayTicks() {
        if (SunderConfig.randomDelay) {
            long randomized = GaussianTimingEngine.getDelay(140.0D, 28.0D, 80L, 240L);
            return Math.max(2, (int) Math.round(randomized / 50.0D));
        }
        return Math.max(2, (int) Math.round(SunderConfig.axeDelayMs / 50.0D));
    }

    private int getMaceDelayTicks() {
        if (SunderConfig.randomDelay) {
            long randomized = GaussianTimingEngine.getDelay(130.0D, 28.0D, 75L, 220L);
            return Math.max(2, (int) Math.round(randomized / 50.0D));
        }
        return Math.max(2, (int) Math.round(SunderConfig.maceDelayMs / 50.0D));
    }

    private int getRestoreDelayTicks() {
        if (SunderConfig.randomDelay) {
            long randomized = GaussianTimingEngine.getDelay(135.0D, 28.0D, 80L, 230L);
            return Math.max(2, (int) Math.round(randomized / 50.0D));
        }
        return Math.max(2, (int) Math.round(SunderConfig.restoreDelayMs / 50.0D));
    }

    private boolean isAirborneConditionMet(ClientPlayerEntity player) {
        if (SunderConfig.airTimeSec <= 0.001D) {
            return true;
        }
        if (player.isGliding()) {
            return true;
        }
        if (player.fallDistance > 0.6F) {
            return true;
        }
        int neededTicks = (int) Math.round(SunderConfig.airTimeSec * 20.0D);
        return airTicks >= neededTicks;
    }

    private boolean isPlayerBusy(MinecraftClient client, ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) return true;
        if (player.isUsingItem() || player.isBlocking()) return true;
        if (client.options.useKey.isPressed()) return true;
        return false;
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
        if (!isTargetShielding(target) || isShieldDisabled(target.getUuid())) {
            return false;
        }
        return canReach(client, target, SunderConfig.triggerDistance);
    }

    private PlayerEntity findTargetAlongRay(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        double maxDist = Math.min(MAX_COMBAT_REACH, SunderConfig.triggerDistance);
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
        Box box = target.getBoundingBox();
        var hit = box.raycast(eyePos, reachEnd);
        if (hit.isPresent()) {
            return eyePos.squaredDistanceTo(hit.get()) <= cappedReach * cappedReach;
        }

        return false;
    }

    private int findMaceHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int bestSlot = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(Items.MACE)) {
                continue;
            }
            int score = 1;
            ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (ench != null) {
                for (var entry : ench.getEnchantmentEntries()) {
                    if (entry.getKey().matchesKey(Enchantments.DENSITY)) {
                        score += (airTicks > 0 ? 15 : 4) * entry.getIntValue();
                    }
                    if (entry.getKey().matchesKey(Enchantments.BREACH)) {
                        score += (airTicks == 0 ? 15 : 4) * entry.getIntValue();
                    }
                    if (entry.getKey().matchesKey(Enchantments.WIND_BURST)) {
                        score += 5 * entry.getIntValue();
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
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

    private boolean isHandledWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isIn(ItemTags.SWORDS) || stack.isOf(Items.TRIDENT);
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client.player == null || slot < 0 || slot >= 9) return;
        SafeSlotManager.selectSlot(client, slot);
    }

    private void restoreInitialSlot(MinecraftClient client) {
        if (client.player != null && initialSlot >= 0 && initialSlot < 9) {
            selectSlot(client, initialSlot);
        }
    }

    private PlayerEntity getTarget(MinecraftClient client) {
        if (targetId == null || client.world == null) return null;
        return client.world.getPlayerByUuid(targetId);
    }

    private boolean isTargetShielding(PlayerEntity target) {
        return activity.client.module.service.TargetCacheService.isTargetShielding(target);
    }

    private void markShieldDisabled(UUID targetUuid) {
        if (targetUuid == null) return;
        SHIELD_DISABLED_TARGETS.put(targetUuid, System.currentTimeMillis() + 5000L);
    }

    private boolean isShieldDisabled(UUID targetUuid) {
        if (targetUuid == null) return false;
        Long expire = SHIELD_DISABLED_TARGETS.get(targetUuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            SHIELD_DISABLED_TARGETS.remove(targetUuid);
            return false;
        }
        return true;
    }

    public void reset() {
        clearState();
    }

    private void clearState() {
        stage = Stage.IDLE;
        targetId = null;
        initialSlot = -1;
        maceSlot = -1;
        axeSlot = -1;
        timer = 0;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", false);
    }

    private void finishCombo() {
        clearState();
        cooldownTimer = SunderConfig.cooldownTicks;
    }

    private enum Stage {
        IDLE,
        SELECT_AXE,
        WAITING_AXE_STRIKE,
        SELECT_MACE,
        WAITING_MACE_STRIKE,
        SEMI_SELECT_AXE,
        SEMI_AWAIT_AXE_HIT,
        SEMI_SELECT_MACE,
        SEMI_AWAIT_MACE_HIT,
        WAITING_RESTORE,
        RESTORE_SLOT
    }
}
