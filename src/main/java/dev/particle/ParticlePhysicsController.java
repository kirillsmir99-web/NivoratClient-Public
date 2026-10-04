package dev.particle;

import net.fabricmc.pack.api.CombatRaytraceGuard;
import net.fabricmc.pack.api.SafeSlotManager;
import net.fabricmc.pack.api.SlotArbiter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.passive.BatEntity;
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

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ParticlePhysicsController {
    private static final int MIN_AXE_DURABILITY = 4;

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
    private int maceAttemptTicks = 0;
    private int cooldownTimer = 0;
    private int airTicks = 0;
    private int comboLifetimeTicks = 0;
    private SlotArbiter.Lease lease = null;

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
            if (world == null || !world.isClient() || !(entity instanceof LivingEntity target)) {
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
                        timer = 1;
                    } else {
                        int restoreTicks = getRestoreDelayTicks();
                        if (restoreTicks <= 0) {
                            stage = Stage.RESTORE_SLOT;
                            restoreInitialSlot(client);
                            finishCombo();
                        } else {
                            stage = Stage.WAITING_RESTORE;
                            timer = restoreTicks;
                        }
                    }
                }
                return ActionResult.PASS;
            }

            if (stage == Stage.SEMI_AWAIT_MACE_HIT || stage == Stage.WAITING_MACE_STRIKE) {
                if (isTarget && hand == Hand.MAIN_HAND && client.player.getInventory().getSelectedSlot() == maceSlot) {
                    if (ParticlePhysicsConfig.stayOnWeapon) {
                        finishCombo(1);
                    } else {
                        int restoreTicks = getRestoreDelayTicks();
                        if (restoreTicks <= 0) {
                            stage = Stage.RESTORE_SLOT;
                            restoreInitialSlot(client);
                            finishCombo();
                        } else {
                            stage = Stage.WAITING_RESTORE;
                            timer = restoreTicks;
                        }
                    }
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

        if (stage != Stage.IDLE && lease != null && !lease.isActive()) {
            clearState();
            return;
        }

        if (stage != Stage.IDLE) {
            comboLifetimeTicks++;
            int maxLifetime = (stage == Stage.SEMI_AWAIT_MACE_HIT) ? 80 : 40;
            if (comboLifetimeTicks > maxLifetime) {
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
                long axeDelay = getAxeDelayMs();
                if (axeDelay <= 15L) {
                    executeAutoStrikeAxe(client);
                } else {
                    stage = Stage.WAITING_AXE_STRIKE;
                    timer = (int) Math.max(1, axeDelay / 50L);
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
                    int restoreTicks = getRestoreDelayTicks();
                    if (restoreTicks <= 0) {
                        stage = Stage.RESTORE_SLOT;
                        restoreInitialSlot(client);
                        finishCombo();
                    } else {
                        stage = Stage.WAITING_RESTORE;
                        timer = restoreTicks;
                    }
                    return;
                }
                selectSlot(client, maceSlot);
                if (ParticlePhysicsConfig.mode == ParticlePhysicsConfig.MODE_SEMI_AUTO) {
                    stage = Stage.SEMI_AWAIT_MACE_HIT;
                    timer = 60;
                } else {
                    long maceDelay = getMaceDelayMs();
                    if (maceDelay <= 15L) {
                        executeAutoStrikeMace(client);
                    } else {
                        stage = Stage.WAITING_MACE_STRIKE;
                        timer = (int) Math.max(1, maceDelay / 50L);
                        maceAttemptTicks = 0;
                    }
                }
            }
            case SELECT_MACE -> {
                if (maceSlot < 0 || maceSlot >= 9) {
                    int restoreTicks = getRestoreDelayTicks();
                    if (restoreTicks <= 0) {
                        stage = Stage.RESTORE_SLOT;
                        restoreInitialSlot(client);
                        finishCombo();
                    } else {
                        stage = Stage.WAITING_RESTORE;
                        timer = restoreTicks;
                    }
                    return;
                }
                selectSlot(client, maceSlot);
                long maceDelay = getMaceDelayMs();
                if (maceDelay <= 15L) {
                    executeAutoStrikeMace(client);
                } else {
                    stage = Stage.WAITING_MACE_STRIKE;
                    timer = (int) Math.max(1, maceDelay / 50L);
                    maceAttemptTicks = 0;
                }
            }
            case WAITING_MACE_STRIKE -> {
                if (timer > 0) {
                    timer--;
                    if (timer > 0) return;
                }
                LivingEntity target = getTarget(client);
                double maxReach = Math.min(client.player.getEntityInteractionRange() + 0.35D, Math.max(3.35D, ParticlePhysicsConfig.triggerDistance + 0.45D));
                if (target != null && target.isAlive() && canReach(client, target, maxReach)) {
                    executeAutoStrikeMace(client);
                } else {
                    maceAttemptTicks++;
                    if (maceAttemptTicks < 6) {
                        return;
                    }
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
                if (--timer <= 0) {
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
                if (client.options.attackKey.isPressed()) {
                    executeAutoStrikeAxe(client);
                }
            }
            case SEMI_SELECT_MACE -> {
                if (timer > 0 && --timer > 0) return;
                selectSlot(client, maceSlot);
                stage = Stage.SEMI_AWAIT_MACE_HIT;
                timer = 60;
            }
            case SEMI_AWAIT_MACE_HIT -> {
                LivingEntity target = getTarget(client);
                if (--timer <= 0 || target == null || !target.isAlive()
                        || client.player.squaredDistanceTo(target) > 36.0D) {
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

        if (SlotArbiter.isResourceLocked(SlotArbiter.Resource.HOTBAR_SELECT, "auto_mace_combo")) {
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
        if (foundAxe < 0 || foundMace < 0) {
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
        lease = SlotArbiter.acquire("auto_mace_combo", SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), 40, false);
        if (lease == null) {
            return;
        }
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", true);

        selectSlot(client, axeSlot);
        long axeDelay = getAxeDelayMs();
        if (axeDelay <= 15L) {
            executeAutoStrikeAxe(client);
        } else {
            stage = Stage.WAITING_AXE_STRIKE;
            timer = (int) Math.max(1, axeDelay / 50L);
        }
    }

    private void executeAutoStrikeAxe(MinecraftClient client) {
        try {
            LivingEntity target = getTarget(client);
            if (target == null || !target.isAlive() || !canReach(client, target, ParticlePhysicsConfig.triggerDistance + 0.5D)) {
                restoreInitialSlot(client);
                finishCombo(1);
                return;
            }

            if (client.player.getInventory().getSelectedSlot() != axeSlot) {
                selectSlot(client, axeSlot);
            }

            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);

            if (maceSlot >= 0) {
                stage = Stage.WAITING_MACE_SWAP;
                timer = 1;
                maceAttemptTicks = 0;
            } else {
                int restoreTicks = getRestoreDelayTicks();
                if (restoreTicks <= 0) {
                    stage = Stage.RESTORE_SLOT;
                    restoreInitialSlot(client);
                    finishCombo();
                } else {
                    stage = Stage.WAITING_RESTORE;
                    timer = restoreTicks;
                }
            }
        } catch (Throwable t) {
            activity.client.module.api.ModuleDiagnostics.report("particle_physics", "executeAutoStrikeAxe", t);
            restoreInitialSlot(client);
            finishCombo(1);
        }
    }

    private void executeAutoStrikeMace(MinecraftClient client) {
        try {
            LivingEntity target = getTarget(client);
            double maxReach = Math.min(client.player.getEntityInteractionRange() + 0.25D, Math.max(3.25D, ParticlePhysicsConfig.triggerDistance + 0.35D));
            if (target != null && target.isAlive() && canReach(client, target, maxReach)) {
                if (client.player.getInventory().getSelectedSlot() != maceSlot) {
                    selectSlot(client, maceSlot);
                }
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);

                if (ParticlePhysicsConfig.stayOnWeapon) {
                    finishCombo(1);
                } else {
                    int restoreTicks = getRestoreDelayTicks();
                    if (restoreTicks <= 0) {
                        stage = Stage.RESTORE_SLOT;
                        restoreInitialSlot(client);
                        finishCombo();
                    } else {
                        stage = Stage.WAITING_RESTORE;
                        timer = restoreTicks;
                    }
                }
            } else {
                restoreInitialSlot(client);
                finishCombo(1);
            }
        } catch (Throwable t) {
            activity.client.module.api.ModuleDiagnostics.report("particle_physics", "executeAutoStrikeMace", t);
            restoreInitialSlot(client);
            finishCombo(1);
        }
    }

    private long getAxeDelayMs() {
        long base = ParticlePhysicsConfig.axeDelayMs;
        if (ParticlePhysicsConfig.randomDelay) {
            long jitter = (long) (ThreadLocalRandom.current().nextDouble() * Math.max(1.0D, ParticlePhysicsConfig.axeJitterMs));
            base += jitter;
        }
        return base;
    }

    private long getMaceDelayMs() {
        long base = ParticlePhysicsConfig.maceDelayMs;
        if (ParticlePhysicsConfig.randomDelay) {
            long jitter = (long) (ThreadLocalRandom.current().nextDouble() * Math.max(1.0D, ParticlePhysicsConfig.maceJitterMs));
            base += jitter;
        }
        return base;
    }

    private int getRestoreDelayTicks() {
        long delayMs = ParticlePhysicsConfig.restoreDelayMs;
        if (delayMs <= 15L) {
            return 0;
        }
        if (ParticlePhysicsConfig.randomDelay) {
            delayMs += (long) (ThreadLocalRandom.current().nextDouble() * 20.0D);
        }
        return Math.max(1, (int) Math.round(delayMs / 50.0D));
    }

    private boolean isAirborneConditionMet(ClientPlayerEntity player) {
        if (player == null) return false;
        if (player.isOnGround() || player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) {
            return false;
        }

        int currentAirTicks = Math.max(airTicks, activity.client.module.service.PlayerStateService.getAirTicks());
        double currentAirSec = currentAirTicks / 20.0D;
        double fallDist = player.fallDistance;

        String cond = ParticlePhysicsConfig.airCondition;
        if ("time".equalsIgnoreCase(cond)) {
            return currentAirSec >= ParticlePhysicsConfig.airTimeSec;
        } else if ("both".equalsIgnoreCase(cond)) {
            return currentAirSec >= ParticlePhysicsConfig.airTimeSec && fallDist >= ParticlePhysicsConfig.minFallDistance;
        } else if ("any".equalsIgnoreCase(cond)) {
            return currentAirSec >= ParticlePhysicsConfig.airTimeSec || fallDist >= ParticlePhysicsConfig.minFallDistance;
        } else {
            return fallDist >= ParticlePhysicsConfig.minFallDistance;
        }
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
        if (target instanceof ArmorStandEntity || target instanceof BatEntity) {
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
            Box box = entity.getBoundingBox().expand(0.1D);
            var hit = box.raycast(eyePos, reachEnd);
            if (hit.isPresent()) {
                double dSq = eyePos.squaredDistanceTo(hit.get());
                if (dSq <= maxDist * maxDist && dSq < closestDistSq) {
                    if (CombatRaytraceGuard.hasLineOfSight(client.player, entity)) {
                        closestDistSq = dSq;
                        best = entity;
                    }
                }
            } else {
                double dist = entity.squaredDistanceTo(eyePos);
                if (dist <= maxDist * maxDist && dist < closestDistSq) {
                    if (CombatRaytraceGuard.hasLineOfSight(client.player, entity)) {
                        closestDistSq = dist;
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
        double cappedReach = Math.min(client.player.getEntityInteractionRange() + 0.25D, maxReach);

        if (!CombatRaytraceGuard.hasLineOfSight(client.player, target)) {
            return false;
        }

        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target) {
            return eyePos.squaredDistanceTo(ehr.getPos()) <= (cappedReach + 0.5D) * (cappedReach + 0.5D);
        }

        Vec3d lookVec = client.player.getRotationVec(1.0F);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(cappedReach));
        Box box = target.getBoundingBox().expand(0.25D);
        var hit = box.raycast(eyePos, reachEnd);

        if (hit.isPresent()) {
            return eyePos.squaredDistanceTo(hit.get()) <= (cappedReach + 0.35D) * (cappedReach + 0.35D);
        }

        double dx = Math.max(box.minX - eyePos.x, Math.max(0.0D, eyePos.x - box.maxX));
        double dy = Math.max(box.minY - eyePos.y, Math.max(0.0D, eyePos.y - box.maxY));
        double dz = Math.max(box.minZ - eyePos.z, Math.max(0.0D, eyePos.z - box.maxZ));
        double distSq = dx * dx + dy * dy + dz * dz;

        return distSq <= cappedReach * cappedReach;
    }

    private int findMaceHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int bestSlot = -1;
        int bestScore = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.MACE)) {
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
        return stack != null && !stack.isEmpty() && (stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isOf(Items.TRIDENT)
                || stack.isOf(Items.MACE));
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client != null && client.player != null && slot >= 0 && slot < 9) {
            if (lease != null && lease.isActive()) {
                SlotArbiter.selectSlot(client, lease, slot);
            } else {
                SafeSlotManager.selectSlot(client, slot);
            }
        }
    }

    private void restoreInitialSlot(MinecraftClient client) {
        if (lease != null) {
            SlotArbiter.release(lease, !ParticlePhysicsConfig.stayOnWeapon);
            lease = null;
        } else if (client != null && client.player != null && initialSlot >= 0 && initialSlot < 9 && !ParticlePhysicsConfig.stayOnWeapon) {
            selectSlot(client, initialSlot);
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
        if (target == null || !target.isAlive()) return false;
        if (target.isBlocking()) return true;
        if (target.isUsingItem()) {
            ItemStack active = target.getActiveItem();
            if (active != null && !active.isEmpty() && (active.getItem() instanceof ShieldItem || active.isOf(Items.SHIELD))) {
                return true;
            }
        }
        ItemStack offhand = target.getOffHandStack();
        if (offhand != null && !offhand.isEmpty() && (offhand.getItem() instanceof ShieldItem || offhand.isOf(Items.SHIELD))) {
            if (target.isUsingItem() || target.isSneaking() || target.isBlocking()) {
                return true;
            }
        }
        ItemStack mainhand = target.getMainHandStack();
        if (mainhand != null && !mainhand.isEmpty() && (mainhand.getItem() instanceof ShieldItem || mainhand.isOf(Items.SHIELD))) {
            if (target.isUsingItem() || target.isSneaking() || target.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        restoreInitialSlot(MinecraftClient.getInstance());
        clearState();
    }

    private void clearState() {
        if (lease != null) {
            SlotArbiter.release(lease, false);
            lease = null;
        }
        stage = Stage.IDLE;
        owner = null;
        ownerWorld = null;
        targetId = null;
        targetEntityId = -1;
        initialSlot = -1;
        maceSlot = -1;
        axeSlot = -1;
        timer = 0;
        maceAttemptTicks = 0;
        comboLifetimeTicks = 0;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.sunder_active", false);
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
