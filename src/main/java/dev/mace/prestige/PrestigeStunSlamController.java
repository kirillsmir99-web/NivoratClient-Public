package dev.mace.prestige;

import activity.client.module.service.InventoryScanService;
import activity.client.module.service.TargetCacheService;
import net.fabricmc.pack.api.CombatRaytraceGuard;
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
import net.minecraft.item.ShieldItem;
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

    private int originalSlot = -1;
    private boolean hasAttackedWithAxe = false;
    private boolean hasAttackedWithMace = false;
    private Entity currentTarget = null;
    private long scheduledMaceAttackTime = 0L;

    private PrestigeStunSlamController() {
    }

    public static PrestigeStunSlamController getInstance() {
        return INSTANCE;
    }

    public PrestigeStunSlamConfig getConfig() {
        return config;
    }

    public void tick(MinecraftClient client) {
        if (!config.enabled || client == null || client.player == null || client.world == null || client.currentScreen != null) {
            reset();
            return;
        }

        ClientPlayerEntity player = client.player;

        if (player.isOnGround()) {
            if (originalSlot != -1 && !config.stayOnMace) {
                SafeSlotManager.selectSlot(client, originalSlot);
            }
            originalSlot = -1;
            hasAttackedWithAxe = false;
            hasAttackedWithMace = false;
            currentTarget = null;
            scheduledMaceAttackTime = 0L;
            PrestigeSilentAim.getInstance().stop();
            return;
        }

        double fallDistance = player.fallDistance;
        double vy = player.getVelocity().y;
        boolean isFalling = vy < -0.05 && !player.isUsingItem();

        if (!isFalling || fallDistance < config.minFallDistance) {
            return;
        }

        int axeSlot = findAxeSlot(player);
        int maceSlot = selectBestMaceSlot(player, fallDistance);
        if (axeSlot == -1 || maceSlot == -1) {
            return;
        }

        currentTarget = findTarget(client, player);
        if (currentTarget == null) {
            PrestigeSilentAim.getInstance().stop();
            return;
        }

        if (config.silentAim) {
            PrestigeSilentAim.getInstance().track(currentTarget, player, client);
        }

        if (!canReach(player.getEyePos(), currentTarget.getBoundingBox(), config.triggerDistance)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!hasAttackedWithAxe) {
            if (originalSlot == -1) {
                originalSlot = player.getInventory().getSelectedSlot();
            }
            SafeSlotManager.selectSlot(client, axeSlot);
            if (client.interactionManager != null) {
                client.interactionManager.attackEntity(player, currentTarget);
                player.swingHand(Hand.MAIN_HAND);
            }
            hasAttackedWithAxe = true;

            SafeSlotManager.selectSlot(client, maceSlot);
            long jitter = config.randomizer ? (long) (random.nextDouble() * Math.max(1.0, config.randomJitterMs)) : 0L;
            scheduledMaceAttackTime = now + jitter;

            if (jitter <= 10L && client.interactionManager != null) {
                client.interactionManager.attackEntity(player, currentTarget);
                player.swingHand(Hand.MAIN_HAND);
                hasAttackedWithMace = true;
            }
            return;
        }

        if (!hasAttackedWithMace && now >= scheduledMaceAttackTime) {
            SafeSlotManager.selectSlot(client, maceSlot);
            if (client.interactionManager != null) {
                client.interactionManager.attackEntity(player, currentTarget);
                player.swingHand(Hand.MAIN_HAND);
            }
            hasAttackedWithMace = true;
        }
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!config.enabled || entity == null) {
            return ActionResult.PASS;
        }
        if (entity instanceof LivingEntity living && isTargetBlocking(living)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player == player) {
                tick(client);
            }
        }
        return ActionResult.PASS;
    }

    public void reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && originalSlot != -1 && !config.stayOnMace) {
            SafeSlotManager.selectSlot(client, originalSlot);
        }
        originalSlot = -1;
        hasAttackedWithAxe = false;
        hasAttackedWithMace = false;
        currentTarget = null;
        scheduledMaceAttackTime = 0L;
        PrestigeSilentAim.getInstance().stop();
    }

    private Entity findTarget(MinecraftClient client, ClientPlayerEntity player) {
        Entity crosshairTarget = client.targetedEntity;
        if (isValidTarget(crosshairTarget, player)) {
            return crosshairTarget;
        }

        if (client.crosshairTarget instanceof EntityHitResult hit) {
            Entity entity = hit.getEntity();
            if (isValidTarget(entity, player)) {
                return entity;
            }
        }

        LivingEntity cached = TargetCacheService.getCrosshairLivingTarget(client);
        if (isValidTarget(cached, player)) {
            return cached;
        }

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        double range = config.triggerDistance;
        Box scanBox = player.getBoundingBox().expand(range + 1.0);

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : client.world.getOtherEntities(player, scanBox, entity -> isValidTarget(entity, player))) {
            Box box = e.getBoundingBox().expand(0.1);
            var hitOpt = box.raycast(eye, eye.add(look.multiply(range + 1.0)));
            if (hitOpt.isPresent()) {
                double dist = eye.squaredDistanceTo(hitOpt.get());
                if (dist < bestDist && CombatRaytraceGuard.hasLineOfSight(player, e)) {
                    bestDist = dist;
                    best = e;
                }
            } else {
                double dist = e.squaredDistanceTo(eye);
                if (dist <= range * range && dist < bestDist && CombatRaytraceGuard.hasLineOfSight(player, e)) {
                    bestDist = dist;
                    best = e;
                }
            }
        }

        return best;
    }

    private boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (entity == null || entity == player) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;
        if (living instanceof ArmorStandEntity || living instanceof BatEntity) return false;
        if (entity.isSpectator()) return false;
        return isTargetBlocking(living);
    }

    private boolean isTargetBlocking(LivingEntity target) {
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

    private boolean canReach(Vec3d eyePos, Box box, double reach) {
        double dx = Math.max(box.minX - eyePos.x, Math.max(0.0, eyePos.x - box.maxX));
        double dy = Math.max(box.minY - eyePos.y, Math.max(0.0, eyePos.y - box.maxY));
        double dz = Math.max(box.minZ - eyePos.z, Math.max(0.0, eyePos.z - box.maxZ));
        return (dx * dx + dy * dy + dz * dz) <= (reach * reach);
    }

    private int findAxeSlot(ClientPlayerEntity player) {
        if (player == null) return -1;
        int cached = InventoryScanService.findAxeSlot(player);
        if (cached >= 0) return cached;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty() && stack.isIn(ItemTags.AXES)) {
                return i;
            }
        }
        return -1;
    }

    private int selectBestMaceSlot(ClientPlayerEntity player, double fallDistance) {
        if (player == null) return -1;
        int bestSlot = -1;
        int bestScore = -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack != null && !stack.isEmpty() && stack.isOf(Items.MACE)) {
                int score = 1;
                ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
                if (ench != null) {
                    for (var entry : ench.getEnchantmentEntries()) {
                        boolean isDensity = entry.getKey().matchesKey(Enchantments.DENSITY);
                        boolean isBreach = entry.getKey().matchesKey(Enchantments.BREACH);
                        int lvl = entry.getIntValue();

                        if ("density".equals(config.enchantMode)) {
                            if (isDensity) score += 2000 * lvl;
                            if (isBreach) score += 10 * lvl;
                        } else if ("breach".equals(config.enchantMode)) {
                            if (isBreach) score += 2000 * lvl;
                            if (isDensity) score += 10 * lvl;
                        } else {
                            if (isDensity) {
                                score += (fallDistance >= 1.5 ? 25 : 15) * lvl;
                            }
                            if (isBreach) {
                                score += (fallDistance < 1.5 ? 20 : 8) * lvl;
                            }
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
}
