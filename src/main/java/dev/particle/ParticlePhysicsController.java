package dev.particle;

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
import net.minecraft.entity.LivingEntity;
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

public final class ParticlePhysicsController {
    private static final int MIN_AXE_DURABILITY = 4;
    private static final double MAX_COMBAT_REACH = 4.2D;

    private ClientPlayerEntity owner;
    private World ownerWorld;
    private boolean enabled = true;
    private Stage stage = Stage.IDLE;
    private UUID targetId;
    private int targetEntityId = -1;
    private int initialSlot = -1;
    private int maceSlot = -1;
    private int axeSlot = -1;
    private int timer = 0;
    private int cooldownTimer = 0;
    private int airTicks = 0;
    private int comboLifetimeTicks = 0;

    public ParticlePhysicsController() {
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
        try {
            if (world == null || !world.isClient() || !(entity instanceof net.minecraft.entity.LivingEntity target)) {
                return ActionResult.PASS;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.player != player || !enabled) {
                return ActionResult.PASS;
            }

            boolean isTarget = (targetEntityId != -1 && target.getId() == targetEntityId)
                    || (targetId != null && target.getUuid().equals(targetId))
                    || targetId == null;

            if (stage == Stage.SEMI_AWAIT_AXE_HIT) {
                if (isTarget && hand == Hand.MAIN_HAND && client.player.getInventory().getSelectedSlot() == axeSlot) {
                    if (maceSlot >= 0) {
                        stage = Stage.SEMI_SELECT_MACE;
                        timer = getMaceDelayTicks();
                    } else {
                        stage = Stage.WAITING_RESTORE;
                        timer = Math.max(1, getRestoreDelayTicks());
                    }
                }
                return ActionResult.PASS;
            }

            if (stage == Stage.SEMI_AWAIT_MACE_HIT || stage == Stage.WAITING_MACE_STRIKE) {
                if (isTarget && hand == Hand.MAIN_HAND && client.player.getInventory().getSelectedSlot() == maceSlot) {
                    stage = Stage.WAITING_RESTORE;
                    timer = Math.max(1, getRestoreDelayTicks());
                }
                return ActionResult.PASS;
            }

            return ActionResult.PASS;
        } catch (Throwable ignored) {
            return ActionResult.PASS;
        }
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null
                || (stage != Stage.IDLE && (client.player != owner || client.world != ownerWorld))) {
            clearState();
            airTicks = 0;
            return;
        }

        if (client.player.isOnGround() || client.player.verticalCollision) {
            airTicks = 0;
        } else {
            airTicks = Math.max(airTicks + 1, activity.client.module.service.PlayerStateService.getAirTicks());
        }

        if (!enabled || !client.player.isAlive() || client.currentScreen != null) {
            if (stage != Stage.IDLE) {
                restoreInitialSlot(client);
                clearState();
            }
            return;
        }

        if (cooldownTimer > 0) {
            cooldownTimer--;
        }

        if (stage != Stage.IDLE) {
            comboLifetimeTicks++;
            if (comboLifetimeTicks > 40) {
                restoreInitialSlot(client);
                finishCombo(5);
                return;
            }
            int currentSlot = client.player.getInventory().getSelectedSlot();
            if (stage == Stage.WAITING_AXE_STRIKE || stage == Stage.SEMI_AWAIT_AXE_HIT) {
                if (axeSlot >= 0 && currentSlot != axeSlot && currentSlot != initialSlot) {
                    restoreInitialSlot(client);
                    finishCombo(3);
                    return;
                }
            } else if (stage == Stage.WAITING_MACE_STRIKE || stage == Stage.SEMI_AWAIT_MACE_HIT) {
                if (maceSlot >= 0 && currentSlot != maceSlot && currentSlot != initialSlot) {
                    restoreInitialSlot(client);
                    finishCombo(3);
                    return;
                }
            }
        }

        switch (stage) {
            case IDLE -> handleIdle(client);
            case SELECT_AXE -> {
                if (axeSlot < 0 || axeSlot >= 9) {
                    finishCombo(2);
                    return;
                }
                selectSlot(client, axeSlot);
                int axeDelay = getAxeDelayTicks();
                if (axeDelay <= 0) {
                    executeAutoStrikeAxe(client);
                } else {
                    stage = Stage.WAITING_AXE_STRIKE;
                    timer = axeDelay;
                }
            }
            case WAITING_AXE_STRIKE -> {
                if (timer > 0) {
                    timer--;
                    if (timer > 0) return;
                }
                executeAutoStrikeAxe(client);
            }
            case WAITING_MACE_SWAP -> {
                if (timer > 0) {
                    timer--;
                    if (timer > 0) return;
                }
                if (maceSlot < 0 || maceSlot >= 9) {
                    stage = Stage.WAITING_RESTORE;
                    timer = Math.max(1, getRestoreDelayTicks());
                    return;
                }
                selectSlot(client, maceSlot);
                int maceDelay = getMaceDelayTicks();
                if (maceDelay <= 0) {
                    executeAutoStrikeMace(client);
                } else {
                    stage = Stage.WAITING_MACE_STRIKE;
                    timer = maceDelay;
                }
            }
            case SELECT_MACE -> {
                if (maceSlot < 0 || maceSlot >= 9) {
                    stage = Stage.WAITING_RESTORE;
                    timer = Math.max(1, getRestoreDelayTicks());
                    return;
                }
                selectSlot(client, maceSlot);
                int maceDelay = getMaceDelayTicks();
                if (maceDelay <= 0) {
                    executeAutoStrikeMace(client);
                } else {
                    stage = Stage.WAITING_MACE_STRIKE;
                    timer = maceDelay;
                }
            }
            case WAITING_MACE_STRIKE -> {
                if (timer > 0 && --timer > 0) return;
                LivingEntity target = getTarget(client);
                double maxReach = Math.min(3.8D, Math.max(3.2D, ParticlePhysicsConfig.triggerDistance + 0.6D));
                if (target != null && target.isAlive() && canReach(client, target, maxReach)) {
                    executeAutoStrikeMace(client);
                } else if (timer <= 0) {
                    restoreInitialSlot(client);
                    finishCombo(1);
                }
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
                    finishCombo(2);
                    return;
                }
                LivingEntity target = getTarget(client);
                if (target == null || !target.isAlive() || !canReach(client, target, ParticlePhysicsConfig.triggerDistance)) {
                    restoreInitialSlot(client);
                    finishCombo(2);
                    return;
                }
            }
            case SEMI_SELECT_MACE -> {
                if (timer > 0 && --timer > 0) return;
                selectSlot(client, maceSlot);
                stage = Stage.SEMI_AWAIT_MACE_HIT;
                timer = 30;
            }
            case SEMI_AWAIT_MACE_HIT -> {
                LivingEntity target = getTarget(client);
                if (--timer <= 0 || target == null || !target.isAlive()
                        || !canReach(client, target, ParticlePhysicsConfig.triggerDistance)) {
                    restoreInitialSlot(client);
                    finishCombo(1);
                }
            }
            case WAITING_RESTORE -> {
                if (timer > 0) {
                    timer--;
                    if (timer > 0) return;
                }
                stage = Stage.RESTORE_SLOT;
                restoreInitialSlot(client);
                finishCombo();
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

        if (isPlayerBusy(client.player)) {
            return;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked()) {
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

        LivingEntity target = findBlockingTarget(client);
        if (target == null) {
            return;
        }

        if (ParticlePhysicsConfig.chance < 100) {
            if (ThreadLocalRandom.current().nextInt(100) >= ParticlePhysicsConfig.chance) {
                cooldownTimer = 2;
                return;
            }
        }

        trigger(client, target, foundAxe, foundMace);
    }

    private void trigger(MinecraftClient client, LivingEntity target, int foundAxe, int foundMace) {
        owner = client.player;
        ownerWorld = client.world;
        initialSlot = client.player.getInventory().getSelectedSlot();
        axeSlot = foundAxe;
        maceSlot = foundMace;
        targetEntityId = target.getId();
        targetId = target.getUuid();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", true);

        if (ParticlePhysicsConfig.mode == ParticlePhysicsConfig.MODE_SEMI_AUTO) {
            if (initialSlot != axeSlot) {
                selectSlot(client, axeSlot);
            }
            stage = Stage.SEMI_AWAIT_AXE_HIT;
            timer = 30;
            return;
        }

        if (initialSlot == axeSlot) {
            executeAutoStrikeAxe(client);
            return;
        }

        selectSlot(client, axeSlot);
        int axeDelay = getAxeDelayTicks();
        if (axeDelay <= 0) {
            executeAutoStrikeAxe(client);
        } else {
            stage = Stage.WAITING_AXE_STRIKE;
            timer = axeDelay;
        }
    }

    private void executeAutoStrikeAxe(MinecraftClient client) {
        try {
            LivingEntity target = getTarget(client);
            if (target == null || !target.isAlive() || !canReach(client, target, ParticlePhysicsConfig.triggerDistance)) {
                restoreInitialSlot(client);
                finishCombo(1);
                return;
            }

            if (client.player.getInventory().getSelectedSlot() != axeSlot) {
                selectSlot(client, axeSlot);
            }

            if (!client.player.getMainHandStack().isIn(ItemTags.AXES)) {
                restoreInitialSlot(client);
                finishCombo(1);
                return;
            }
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);

            if (maceSlot >= 0) {
                int maceDelay = getMaceDelayTicks();
                if (maceDelay <= 0 && ParticlePhysicsConfig.mode != ParticlePhysicsConfig.MODE_SEMI_AUTO) {
                    selectSlot(client, maceSlot);
                    executeAutoStrikeMace(client);
                } else {
                    stage = ParticlePhysicsConfig.mode == ParticlePhysicsConfig.MODE_SEMI_AUTO ? Stage.SEMI_SELECT_MACE : Stage.WAITING_MACE_SWAP;
                    timer = Math.max(1, maceDelay);
                }
            } else {
                stage = Stage.WAITING_RESTORE;
                timer = Math.max(1, getRestoreDelayTicks());
            }
        } catch (Throwable t) {
            restoreInitialSlot(client);
            finishCombo(1);
        }
    }

    private void executeAutoStrikeMace(MinecraftClient client) {
        try {
            LivingEntity target = getTarget(client);
            double maxReach = Math.min(3.8D, Math.max(3.2D, ParticlePhysicsConfig.triggerDistance + 0.6D));
            if (target != null && target.isAlive() && canReach(client, target, maxReach)) {
                if (client.player.getInventory().getSelectedSlot() != maceSlot) {
                    selectSlot(client, maceSlot);
                }
                if (!client.player.getMainHandStack().isOf(Items.MACE)) {
                    restoreInitialSlot(client);
                    finishCombo(1);
                    return;
                }
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);

                if (ParticlePhysicsConfig.mode == ParticlePhysicsConfig.MODE_SEMI_AUTO) {
                    restoreInitialSlot(client);
                    finishCombo(1);
                } else {
                    stage = Stage.WAITING_RESTORE;
                    timer = Math.max(1, getRestoreDelayTicks());
                }
            } else {
                restoreInitialSlot(client);
                finishCombo(1);
            }
        } catch (Throwable t) {
            restoreInitialSlot(client);
            finishCombo(1);
        }
    }

    private int getAxeDelayTicks() {
        return GaussianTimingEngine.sampleActionTicks(ParticlePhysicsConfig.axeDelayMs, ParticlePhysicsConfig.randomDelay);
    }

    private int getMaceDelayTicks() {
        return GaussianTimingEngine.sampleActionTicks(ParticlePhysicsConfig.maceDelayMs, ParticlePhysicsConfig.randomDelay);
    }

    private int getRestoreDelayTicks() {
        return GaussianTimingEngine.sampleActionTicks(ParticlePhysicsConfig.restoreDelayMs, ParticlePhysicsConfig.randomDelay);
    }

    private boolean isAirborneConditionMet(ClientPlayerEntity player) {
        if (player == null) return false;
        if (ParticlePhysicsConfig.airTimeSec <= 0.05D) {
            return !player.isTouchingWater() && !player.isClimbing() && !player.hasVehicle();
        }
        if (player.isOnGround() || player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) {
            return false;
        }
        int neededTicks = Math.max(1, (int) Math.round(ParticlePhysicsConfig.airTimeSec * 20.0D));
        int currentAirTicks = Math.max(airTicks, activity.client.module.service.PlayerStateService.getAirTicks());
        return currentAirTicks >= neededTicks;
    }

    private boolean isPlayerBusy(ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) return true;
        return activity.client.module.service.PlayerStateService.isBusy(player);
    }

    private LivingEntity findBlockingTarget(MinecraftClient client) {
        LivingEntity crosshair = activity.client.module.service.TargetCacheService.getCrosshairLivingTarget(client);
        if (crosshair != null && isValidTarget(client, crosshair)) {
            return crosshair;
        }

        if (client.targetedEntity instanceof LivingEntity target) {
            if (isValidTarget(client, target)) {
                return target;
            }
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity target) {
            if (isValidTarget(client, target)) {
                return target;
            }
        }

        return findTargetAlongRay(client);
    }

    private boolean isValidTarget(MinecraftClient client, LivingEntity target) {
        if (target == null || target == client.player || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (!isTargetShielding(target)) {
            return false;
        }
        return canReach(client, target, ParticlePhysicsConfig.triggerDistance);
    }

    private LivingEntity findTargetAlongRay(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVec(1.0F);
        double maxDist = Math.min(client.player.getEntityInteractionRange() - 0.05D, ParticlePhysicsConfig.triggerDistance);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(maxDist));

        LivingEntity best = null;
        double closestDistSq = Double.MAX_VALUE;

        Box searchBox = client.player.getBoundingBox().expand(maxDist + 1.0D);
        for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, searchBox, e -> isValidTarget(client, e))) {
            Box box = entity.getBoundingBox();
            var hit = box.raycast(eyePos, reachEnd);
            if (hit.isPresent()) {
                double dSq = eyePos.squaredDistanceTo(hit.get());
                if (dSq <= maxDist * maxDist && dSq < closestDistSq) {
                    if (CombatRaytraceGuard.hasLineOfSight(client.player, entity)) {
                        closestDistSq = dSq;
                        best = entity;
                    }
                }
            }
        }
        return best;
    }

    private boolean canReach(MinecraftClient client, LivingEntity target, double maxReach) {
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

        double dx = Math.max(box.minX - eyePos.x, Math.max(0.0, eyePos.x - box.maxX));
        double dy = Math.max(box.minY - eyePos.y, Math.max(0.0, eyePos.y - box.maxY));
        double dz = Math.max(box.minZ - eyePos.z, Math.max(0.0, eyePos.z - box.maxZ));
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= cappedReach * cappedReach;
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
                    boolean isDensity = entry.getKey().matchesKey(Enchantments.DENSITY);
                    boolean isBreach = entry.getKey().matchesKey(Enchantments.BREACH);
                    int lvl = entry.getIntValue();

                    if (ParticlePhysicsConfig.enchantPreference == ParticlePhysicsConfig.ENCHANT_DENSITY) {
                        if (isDensity) score += 2000 * lvl;
                        if (isBreach) score += 10 * lvl;
                    } else if (ParticlePhysicsConfig.enchantPreference == ParticlePhysicsConfig.ENCHANT_BREACH) {
                        if (isBreach) score += 2000 * lvl;
                        if (isDensity) score += 10 * lvl;
                    } else {
                        if (isDensity) {
                            score += (airTicks > 0 ? 25 : 15) * lvl;
                        }
                        if (isBreach) {
                            score += (airTicks == 0 ? 16 : 8) * lvl;
                        }
                    }
                    if (entry.getKey().matchesKey(Enchantments.WIND_BURST)) {
                        score += 5 * lvl;
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

    private boolean isHandledWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(Items.TRIDENT) || stack.isOf(Items.MACE);
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client.player == null || slot < 0 || slot >= 9) return;
        SafeSlotManager.selectSlot(client, slot);
    }

    private void restoreInitialSlot(MinecraftClient client) {
        if (client != null && client.player == owner && client.world == ownerWorld && owner != null
                && initialSlot >= 0 && initialSlot < 9) {
            int selected = owner.getInventory().getSelectedSlot();
            if (selected == axeSlot || selected == maceSlot) SafeSlotManager.restoreSlot(client, initialSlot);
        }
    }

    private LivingEntity getTarget(MinecraftClient client) {
        if (client.world == null) return null;
        if (targetEntityId != -1) {
            Entity e = client.world.getEntityById(targetEntityId);
            if (e instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        if (targetId != null) {
            PlayerEntity p = client.world.getPlayerByUuid(targetId);
            if (p != null && p.isAlive()) {
                return p;
            }
        }
        return null;
    }

    private boolean isTargetShielding(LivingEntity target) {
        return activity.client.module.service.TargetCacheService.isTargetShielding(target);
    }

    public void reset() {
        restoreInitialSlot(MinecraftClient.getInstance());
        clearState();
    }

    private void clearState() {
        stage = Stage.IDLE;
        owner = null;
        ownerWorld = null;
        targetId = null;
        targetEntityId = -1;
        initialSlot = -1;
        maceSlot = -1;
        axeSlot = -1;
        timer = 0;
        comboLifetimeTicks = 0;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", false);
        System.clearProperty("pvp.sunder_active");
    }

    private void finishCombo() {
        finishCombo(ParticlePhysicsConfig.cooldownTicks);
    }

    private void finishCombo(int cooldown) {
        clearState();
        cooldownTimer = Math.max(0, cooldown);
    }

    private enum Stage {
        IDLE,
        SELECT_AXE,
        WAITING_AXE_STRIKE,
        WAITING_MACE_SWAP,
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
