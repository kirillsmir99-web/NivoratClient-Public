package dev.mace.prestige;

import net.fabricmc.pack.api.SafeSlotManager;
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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

public final class PrestigeStunSlamController {
    private static final PrestigeStunSlamController INSTANCE = new PrestigeStunSlamController();

    private final PrestigeStunSlamConfig config = new PrestigeStunSlamConfig();
    private final Random random = new Random();

    private int savedSlot = -1;
    private boolean isAirborne = false;
    private boolean hasAttacked = false;
    private boolean axeSwapped = false;
    private boolean macePending = false;
    private int macePendingTicks = 0;
    private boolean slotSwappedThisTick = false;
    private long lastAttackTime = 0L;
    private LivingEntity currentTarget = null;
    private Vec3d lastPlayerEyePos = null;

    private PrestigeStunSlamController() {
    }

    public static PrestigeStunSlamController getInstance() {
        return INSTANCE;
    }

    public PrestigeStunSlamConfig getConfig() {
        return config;
    }

    public void tick(MinecraftClient client) {
        slotSwappedThisTick = false;
        if (!config.enabled || client == null || client.player == null || client.world == null || client.currentScreen != null) {
            PrestigeSilentAim.getInstance().decay(client != null ? client.player : null, client);
            return;
        }

        ClientPlayerEntity player = client.player;
        lastPlayerEyePos = player.getEyePos();
        updateGroundState(client, player);

        double fallDist = player.fallDistance;
        double vy = player.getVelocity().y;
        boolean inAir = !player.isOnGround() && !player.isClimbing() && !player.isTouchingWater();
        boolean isFalling = inAir && (vy < -0.05 || fallDist > 0.0) && !player.isUsingItem();
        boolean conditionMet = isAirborneConditionMet(player);
        boolean elytraFall = isFalling && player.isGliding();
        boolean aimEnabled = config.silentAim;

        if (!hasAttacked && hasMaceInHotbar(player) && inAir) {
            currentTarget = aimEnabled ? findTarget(client, player, 20.0) : findTargetUnderCrosshair(client, player);
        }

        if (aimEnabled && currentTarget != null && inAir && !hasAttacked) {
            PrestigeSilentAim.getInstance().track(currentTarget, player, client);
        } else {
            PrestigeSilentAim.getInstance().decay(player, client);
        }

        if (macePending) {
            if (macePendingTicks > 0) {
                macePendingTicks--;
            }
            if (macePendingTicks <= 0) {
                if (currentTarget != null && isValidTarget(currentTarget, player) && canReach(player, currentTarget)) {
                    int maceSlot = selectBestMaceSlot(player, fallDist);
                    if (maceSlot != -1 && player.getInventory().getSelectedSlot() != maceSlot) {
                        selectSlot(client, maceSlot);
                    }
                    attackTarget(client, player, currentTarget);
                    hasAttacked = true;
                    lastAttackTime = System.currentTimeMillis();
                }
                macePending = false;
                macePendingTicks = 0;
            }
            return;
        }

        if (inAir && !hasAttacked && isFalling && (conditionMet || elytraFall)) {
            if (config.chance < 100.0) {
                if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 100.0 >= config.chance) {
                    return;
                }
            }
            executeAttack(client, player, fallDist);
        }
    }

    private boolean isAirborneConditionMet(ClientPlayerEntity player) {
        if (player == null) return false;
        if (player.isOnGround() || player.isClimbing() || player.isTouchingWater() || player.hasVehicle()) {
            return false;
        }

        int airTicks = activity.client.module.service.PlayerStateService.getAirTicks();
        double airSec = airTicks / 20.0D;
        double fallDist = player.fallDistance;

        String cond = config.airCondition;
        if ("time".equalsIgnoreCase(cond)) {
            return airSec >= config.airTimeSec;
        } else if ("both".equalsIgnoreCase(cond)) {
            return airSec >= config.airTimeSec && fallDist >= config.minFallDistance;
        } else if ("any".equalsIgnoreCase(cond)) {
            return airSec >= config.airTimeSec || fallDist >= config.minFallDistance;
        } else {
            return fallDist >= config.minFallDistance;
        }
    }

    private void executeAttack(MinecraftClient client, ClientPlayerEntity player, double fallDist) {
        if (player.isUsingItem() || player.isOnGround()) return;
        if (hasAttacked || macePending) return;

        if (currentTarget == null || !isValidTarget(currentTarget, player) || !canReach(player, currentTarget)) {
            return;
        }

        boolean isShielding = isHoldingShield(currentTarget);

        if (isShielding && !axeSwapped) {
            int axeSlot = findAxeSlot(player);
            if (axeSlot != -1) {
                if (savedSlot == -1) {
                    savedSlot = player.getInventory().getSelectedSlot();
                }

                selectSlot(client, axeSlot);
                attackTarget(client, player, currentTarget);
                axeSwapped = true;

                int maceSlot = selectBestMaceSlot(player, fallDist);
                if (maceSlot != -1) {
                    selectSlot(client, maceSlot);
                }
                if ("semi_auto".equalsIgnoreCase(config.mode)) {
                    hasAttacked = true;
                    macePending = false;
                    return;
                }
                macePending = true;
                macePendingTicks = (int) Math.max(1, Math.round(config.attackDelayMs / 50.0));
                return;
            }
        }

        if (!isHoldingMace(player)) {
            if (savedSlot == -1) {
                savedSlot = player.getInventory().getSelectedSlot();
            }
            int maceSlot = selectBestMaceSlot(player, fallDist);
            if (maceSlot != -1 && player.getInventory().getSelectedSlot() != maceSlot) {
                selectSlot(client, maceSlot);
            }
        }

        if ("semi_auto".equalsIgnoreCase(config.mode)) {
            hasAttacked = true;
            return;
        }

        if (isHoldingMace(player) && hasDelayElapsed()) {
            attackTarget(client, player, currentTarget);
            hasAttacked = true;
            lastAttackTime = System.currentTimeMillis();
        }
    }

    private boolean hasDelayElapsed() {
        long minDelay = (long) Math.max(0.0, config.attackDelayMs + (config.randomizer ? config.randomJitterMs : 0.0));
        return System.currentTimeMillis() - lastAttackTime >= minDelay;
    }

    private void updateGroundState(MinecraftClient client, ClientPlayerEntity player) {
        boolean onGround = player.isOnGround();
        if (onGround) {
            if (isAirborne) {
                isAirborne = false;
                hasAttacked = false;
                axeSwapped = false;
                macePending = false;
                macePendingTicks = 0;
            }
            if (savedSlot != -1 && !config.stayOnMace) {
                selectSlot(client, savedSlot);
                savedSlot = -1;
            }
        } else {
            double vy = player.getVelocity().y;
            if (vy > 0.1 && hasAttacked) {
                hasAttacked = false;
                axeSwapped = false;
                macePending = false;
                macePendingTicks = 0;
            }
            if (!isAirborne) {
                isAirborne = true;
                hasAttacked = false;
                axeSwapped = false;
                macePending = false;
                macePendingTicks = 0;
            }
        }
    }

    private boolean selectSlot(MinecraftClient client, int slot) {
        if (slot >= 0 && slot < 9 && client != null && client.player != null) {
            SafeSlotManager.selectSlot(client, slot);
            slotSwappedThisTick = true;
            return true;
        }
        return false;
    }

    private void attackTarget(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        if (client == null || client.interactionManager == null || player == null || target == null) return;
        PrestigeSilentAim aim = PrestigeSilentAim.getInstance();
        boolean hasSilent = aim.isActive();
        float realYaw = player.getYaw();
        float realPitch = player.getPitch();
        if (hasSilent) {
            player.setYaw(aim.getYaw());
            player.setPitch(aim.getPitch());
        }
        try {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        } finally {
            if (hasSilent) {
                player.setYaw(realYaw);
                player.setPitch(realPitch);
            }
        }
    }

    private boolean canReach(ClientPlayerEntity player, Entity target) {
        if (player == null || target == null || !target.isAlive()) return false;
        double reach = Math.min(3.5D, Math.max(3.0D, config.triggerDistance));
        Vec3d eyePos = lastPlayerEyePos != null ? lastPlayerEyePos : player.getEyePos();
        Box targetBox = target.getBoundingBox().expand(0.2D);
        if (targetBox.contains(eyePos)) {
            return true;
        }

        PrestigeSilentAim aim = PrestigeSilentAim.getInstance();
        boolean hasRot = config.silentAim && aim.isActive();
        float yaw = hasRot ? aim.getYaw() : player.getYaw();
        float pitch = hasRot ? aim.getPitch() : player.getPitch();
        Vec3d rotVec = player.getRotationVector(pitch, yaw);
        Vec3d reachVec = eyePos.add(rotVec.x * reach, rotVec.y * reach, rotVec.z * reach);
        if (targetBox.raycast(eyePos, reachVec).isPresent()) {
            return true;
        }

        if (config.silentAim) {
            double dx = Math.max(targetBox.minX - eyePos.x, Math.max(0.0, eyePos.x - targetBox.maxX));
            double dy = Math.max(targetBox.minY - eyePos.y, Math.max(0.0, eyePos.y - targetBox.maxY));
            double dz = Math.max(targetBox.minZ - eyePos.z, Math.max(0.0, eyePos.z - targetBox.maxZ));
            double distSq = dx * dx + dy * dy + dz * dz;
            return distSq <= reach * reach;
        }

        return false;
    }

    private LivingEntity findTargetUnderCrosshair(MinecraftClient client, ClientPlayerEntity player) {
        if (client == null || client.world == null || player == null) return null;
        if (client.targetedEntity instanceof LivingEntity living && isValidTarget(living, player)) {
            return living;
        }
        if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && isValidTarget(living, player)) {
            return living;
        }
        Vec3d eyePos = player.getEyePos();
        Vec3d look = player.getRotationVector();
        double reach = Math.min(3.5D, Math.max(3.0D, config.triggerDistance));
        Vec3d end = eyePos.add(look.x * reach, look.y * reach, look.z * reach);
        for (Entity e : client.world.getEntities()) {
            if (e instanceof LivingEntity living && e != player && isValidTarget(living, player)) {
                Box box = living.getBoundingBox().expand(0.2D);
                if (box.raycast(eyePos, end).isPresent()) {
                    return living;
                }
            }
        }
        return null;
    }

    private LivingEntity findTarget(MinecraftClient client, ClientPlayerEntity player, double range) {
        if (client == null || client.world == null || player == null) return null;
        LivingEntity best = null;
        double bestDistSq = range * range;
        double playerY = player.getY();

        for (Entity other : client.world.getEntities()) {
            if (other != player && isValidTarget(other, player) && !(other.getY() > playerY + 2.5)) {
                double distSq = other.squaredDistanceTo(player);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = (LivingEntity) other;
                }
            }
        }
        return best;
    }

    private LivingEntity findNearestTarget(MinecraftClient client, ClientPlayerEntity player) {
        if (client == null || client.world == null || player == null) return null;
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Entity other : client.world.getEntities()) {
            if (other != player && isValidTarget(other, player) && canReach(player, other)) {
                double distSq = other.squaredDistanceTo(player);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = (LivingEntity) other;
                }
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (entity == null || entity == player) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive() || living.isDead()) return false;
        if (entity.isSpectator()) return false;
        if (entity instanceof ArmorStandEntity || entity instanceof BatEntity) return false;
        return true;
    }

    public static boolean isHoldingShield(Entity entity) {
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return false;
        if (target.isBlocking()) return true;
        if (target.isUsingItem()) {
            ItemStack active = target.getActiveItem();
            if (active != null && !active.isEmpty() && active.isOf(Items.SHIELD)) {
                return true;
            }
        }
        ItemStack offhand = target.getOffHandStack();
        if (offhand != null && !offhand.isEmpty() && offhand.isOf(Items.SHIELD)) {
            if (target.isUsingItem() || target.isSneaking() || target.isBlocking()) {
                return true;
            }
        }
        ItemStack mainhand = target.getMainHandStack();
        if (mainhand != null && !mainhand.isEmpty() && mainhand.isOf(Items.SHIELD)) {
            if (target.isUsingItem() || target.isSneaking() || target.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    private boolean isHoldingMace(ClientPlayerEntity player) {
        return player != null && player.getMainHandStack().isOf(Items.MACE);
    }

    private int findFirstMace(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.isOf(Items.MACE)) {
                return i;
            }
        }
        return -1;
    }

    private int findDensityMace(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.isOf(Items.MACE)) {
                ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    for (var entry : ench.getEnchantmentEntries()) {
                        if (entry.getKey().matchesKey(Enchantments.DENSITY)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private int findBreachMace(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.isOf(Items.MACE)) {
                ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    for (var entry : ench.getEnchantmentEntries()) {
                        if (entry.getKey().matchesKey(Enchantments.BREACH)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private int selectBestMaceSlot(ClientPlayerEntity player, double fallDist) {
        boolean smart = "smart".equalsIgnoreCase(config.enchantMode);
        boolean breachOnly = "breach".equalsIgnoreCase(config.enchantMode);
        boolean densityOnly = "density".equalsIgnoreCase(config.enchantMode);

        if (densityOnly) {
            int slot = findDensityMace(player);
            return slot != -1 ? slot : findFirstMace(player);
        }
        if (breachOnly) {
            int slot = findBreachMace(player);
            return slot != -1 ? slot : findFirstMace(player);
        }

        int maceSlot = -1;
        if (smart) {
            maceSlot = fallDist >= 7.0 ? findDensityMace(player) : findBreachMace(player);
        }
        if (maceSlot == -1) {
            maceSlot = findBreachMace(player);
        }
        if (maceSlot == -1) {
            maceSlot = findFirstMace(player);
        }
        return maceSlot;
    }

    private int findAxeSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.isIn(ItemTags.AXES)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasMaceInHotbar(ClientPlayerEntity player) {
        return findFirstMace(player) != -1;
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        return ActionResult.PASS;
    }

    public void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && savedSlot != -1 && !config.stayOnMace) {
            selectSlot(client, savedSlot);
        }
        savedSlot = -1;
        isAirborne = false;
        hasAttacked = false;
        axeSwapped = false;
        macePending = false;
        macePendingTicks = 0;
        slotSwappedThisTick = false;
        currentTarget = null;
        lastPlayerEyePos = null;
        PrestigeSilentAim.getInstance().stop();
    }
}
