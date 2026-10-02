package dev.mace.prestige;

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
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PrestigeAutoMaceController {
    private static final PrestigeAutoMaceController INSTANCE = new PrestigeAutoMaceController();

    private final PrestigeAutoMaceConfig config = new PrestigeAutoMaceConfig();
    private final Random random = new Random();

    private int originalSlot = -1;
    private boolean hasAttackedInFall = false;
    private long lastAttackTime = 0L;
    private Entity currentTarget = null;
    private long weaponSwitchTime = 0L;
    private long scheduledAttackTime = 0L;
    private long restorePendingTime = 0L;

    private PrestigeAutoMaceController() {
    }

    public static PrestigeAutoMaceController getInstance() {
        return INSTANCE;
    }

    public PrestigeAutoMaceConfig getConfig() {
        return config;
    }

    private long calculateDelay() {
        if (!config.humanMode) return 0L;
        double base = config.attackDelayMs;
        if (config.randomJitter) {
            double jitter = random.nextGaussian() * 20.0;
            base += jitter;
        }
        return Math.max(120L, Math.min(260L, Math.round(base)));
    }

    private boolean isCloseToLanding(ClientPlayerEntity player, Entity target) {
        if (player.fallDistance < config.minFallDistance) return false;
        double vy = player.getVelocity().y;
        if (vy > -0.15) return false;
        Vec3d eye = player.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        double distY = eye.y - targetCenter.y;
        return distY < 0.95;
    }

    public void tick(MinecraftClient client) {
        if (!config.enabled || client == null || client.player == null || client.world == null || client.currentScreen != null) {
            reset();
            return;
        }

        ClientPlayerEntity player = client.player;

        if (player.isOnGround()) {
            if (originalSlot != -1 && !config.stayOnMace) {
                restoreSlot(player);
            }
            hasAttackedInFall = false;
            currentTarget = null;
            weaponSwitchTime = 0L;
            scheduledAttackTime = 0L;
            restorePendingTime = 0L;
            PrestigeSilentAim.getInstance().stop();
            return;
        }

        if (restorePendingTime > 0L && System.currentTimeMillis() >= restorePendingTime) {
            restorePendingTime = 0L;
            if (!config.stayOnMace) {
                restoreSlot(player);
            }
        }

        double fallDistance = player.fallDistance;
        double vy = player.getVelocity().y;
        boolean isFallingDown = vy < -0.08 && !player.isUsingItem();
        boolean fallReady = isFallingDown && !player.isGliding() && fallDistance >= Math.min(1.2, config.minFallDistance);
        boolean elytraFalling = isFallingDown && player.isGliding();

        if (!fallReady && !elytraFalling) {
            return;
        }

        if (!hasMaceInHotbar(player)) {
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

        if (config.predictSwitch && fallDistance >= config.minFallDistance && distanceTo(player, currentTarget) <= 4.0) {
            if (config.unequipElytra && player.isGliding()) {
                unequipElytra(client, player);
            }
            if (config.autoSwitch && !isHoldingMace(player) && weaponSwitchTime == 0L) {
                saveOriginalSlot(player);
                int maceSlot = selectBestMaceSlot(player, fallDistance);
                if (maceSlot != -1) {
                    setSlot(player, maceSlot);
                    weaponSwitchTime = System.currentTimeMillis();
                    scheduledAttackTime = weaponSwitchTime + calculateDelay();
                }
            }
        }

        tryAttack(client, player, fallDistance);
    }

    private void tryAttack(MinecraftClient client, ClientPlayerEntity player, double fallDistance) {
        if (hasAttackedInFall) {
            return;
        }

        if (fallDistance < config.minFallDistance) {
            return;
        }

        if (currentTarget == null || !isValidTarget(currentTarget, player)) {
            return;
        }

        Box targetBox = currentTarget.getBoundingBox();
        Vec3d eyePos = player.getEyePos();

        double reach = Math.min(player.getEntityInteractionRange() - 0.05D, 2.95D);
        if (!canReach(eyePos, targetBox, reach)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (config.attackDelayMs > 0 && !config.humanMode && (now - lastAttackTime) < (long) config.attackDelayMs) {
            return;
        }

        if (config.autoSwitch && !isHoldingMace(player)) {
            saveOriginalSlot(player);
            int maceSlot = selectBestMaceSlot(player, fallDistance);
            if (maceSlot != -1) {
                setSlot(player, maceSlot);
                weaponSwitchTime = now;
                scheduledAttackTime = now + calculateDelay();
                return;
            }
        }

        if (isHoldingMace(player)) {
            if (now >= scheduledAttackTime || isCloseToLanding(player, currentTarget)) {
                executeAttack(client, player, currentTarget);
                hasAttackedInFall = true;
                lastAttackTime = now;
                if (!config.stayOnMace) {
                    restorePendingTime = now + (config.humanMode ? calculateDelay() : 0L);
                }
            }
        }
    }

    private void executeAttack(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        if (client.interactionManager != null && target != null) {
            PrestigeSilentAim.getInstance().stop();
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    private Entity findTarget(MinecraftClient client, ClientPlayerEntity player) {
        if (client.world == null) return null;

        Entity best = null;
        double bestDistSq = config.silentAimRange * config.silentAimRange;
        Vec3d eyePos = player.getEyePos();

        for (PlayerEntity otherPlayer : client.world.getPlayers()) {
            if (isValidTarget(otherPlayer, player)) {
                double distSq = otherPlayer.squaredDistanceTo(eyePos);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = otherPlayer;
                }
            }
        }

        if (config.targetMobs) {
            Box searchBox = player.getBoundingBox().expand(config.silentAimRange);
            List<Entity> mobs = client.world.getOtherEntities(player, searchBox, e -> isValidTarget(e, player));
            for (Entity mob : mobs) {
                double distSq = mob.squaredDistanceTo(eyePos);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = mob;
                }
            }
        }

        return best;
    }

    public boolean isValidTarget(Entity entity, ClientPlayerEntity player) {
        if (entity == null || entity == player || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (entity instanceof ArmorStandEntity || entity instanceof BatEntity) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return config.targetPlayers;
        }
        return config.targetMobs;
    }

    public static boolean isTargetBlocking(Entity entity) {
        if (entity instanceof LivingEntity living) {
            if (living.isBlocking()) return true;
            ItemStack activeStack = living.getActiveItem();
            return activeStack != null && activeStack.isOf(Items.SHIELD);
        }
        return false;
    }

    private boolean canReach(Vec3d eyePos, Box targetBox, double reach) {
        if (targetBox.contains(eyePos)) return true;
        Vec3d closest = new Vec3d(
                Math.max(targetBox.minX, Math.min(eyePos.x, targetBox.maxX)),
                Math.max(targetBox.minY, Math.min(eyePos.y, targetBox.maxY)),
                Math.max(targetBox.minZ, Math.min(eyePos.z, targetBox.maxZ))
        );
        return closest.squaredDistanceTo(eyePos) <= (reach * reach);
    }

    public static Box getEffectiveBox(Entity target, double factor) {
        Box box = target.getBoundingBox();
        if (factor <= 1.0) return box;
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double halfWidth = (box.maxX - box.minX) * 0.5 * factor;
        double halfLength = (box.maxZ - box.minZ) * 0.5 * factor;
        return new Box(centerX - halfWidth, box.minY, centerZ - halfLength, centerX + halfWidth, box.maxY, centerZ + halfLength);
    }

    private static double distanceTo(Entity a, Entity b) {
        return Math.sqrt(a.squaredDistanceTo(b));
    }

    private boolean hasMaceInHotbar(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.MACE)) return true;
        }
        return false;
    }

    private boolean isHoldingMace(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.MACE);
    }

    public int selectBestMaceSlot(ClientPlayerEntity player, double fallDistance) {
        int bestBreachSlot = -1;
        int bestBreachLevel = 0;
        int bestDensitySlot = -1;
        int bestDensityLevel = 0;
        int anyMaceSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) {
                if (anyMaceSlot == -1) anyMaceSlot = i;
                int bLevel = getEnchantmentLevel(stack, Enchantments.BREACH);
                if (bLevel > bestBreachLevel) {
                    bestBreachLevel = bLevel;
                    bestBreachSlot = i;
                }
                int dLevel = getEnchantmentLevel(stack, Enchantments.DENSITY);
                if (dLevel > bestDensityLevel) {
                    bestDensityLevel = dLevel;
                    bestDensitySlot = i;
                }
            }
        }

        String mode = config.enchantMode != null ? config.enchantMode : "smart";
        if ("density_only".equals(mode)) {
            if (bestDensitySlot != -1) return bestDensitySlot;
            return anyMaceSlot;
        }
        if ("breach_only".equals(mode)) {
            if (bestBreachSlot != -1) return bestBreachSlot;
            return anyMaceSlot;
        }

        if (fallDistance >= 2.5 && bestDensitySlot != -1) {
            return bestDensitySlot;
        }
        if (bestBreachSlot != -1) {
            return bestBreachSlot;
        }
        if (bestDensitySlot != -1) {
            return bestDensitySlot;
        }
        return anyMaceSlot;
    }

    public static int findAxeSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && (stack.isIn(ItemTags.AXES) || stack.getItem() instanceof AxeItem)) {
                return i;
            }
        }
        return -1;
    }

    public static int findSwordSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasEnchantment(ItemStack stack, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (stack == null || stack.isEmpty()) return false;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry != null && entry.getKey() != null && entry.getKey().matchesKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getEnchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry != null && entry.getKey() != null && entry.getKey().matchesKey(key)) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
    }

    private void saveOriginalSlot(ClientPlayerEntity player) {
        if (originalSlot == -1) {
            originalSlot = player.getInventory().getSelectedSlot();
        }
    }

    private void setSlot(ClientPlayerEntity player, int slot) {
        if (slot >= 0 && slot < 9 && player.getInventory().getSelectedSlot() != slot) {
            net.fabricmc.pack.api.SafeSlotManager.selectSlot(MinecraftClient.getInstance(), slot);
        }
    }

    private void restoreSlot(ClientPlayerEntity player) {
        if (originalSlot >= 0 && originalSlot < 9) {
            setSlot(player, originalSlot);
            originalSlot = -1;
        }
    }

    private void unequipElytra(MinecraftClient client, ClientPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (isChestplate(stack)) {
                    setSlot(player, i);
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                    }
                    break;
                }
            }
        }
    }

    private static boolean isChestplate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.NETHERITE_CHESTPLATE)
                || stack.isOf(Items.DIAMOND_CHESTPLATE)
                || stack.isOf(Items.IRON_CHESTPLATE)
                || stack.isOf(Items.CHAINMAIL_CHESTPLATE)
                || stack.isOf(Items.GOLDEN_CHESTPLATE)
                || stack.isOf(Items.LEATHER_CHESTPLATE)) {
            return true;
        }
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == net.minecraft.entity.EquipmentSlot.CHEST;
    }

    public void reset() {
        if (originalSlot != -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null && !config.stayOnMace) {
                restoreSlot(client.player);
            }
        }
        originalSlot = -1;
        hasAttackedInFall = false;
        currentTarget = null;
        weaponSwitchTime = 0L;
        scheduledAttackTime = 0L;
        restorePendingTime = 0L;
        PrestigeSilentAim.getInstance().stop();
    }
}
