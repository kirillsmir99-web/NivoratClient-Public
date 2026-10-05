package dev.mace.prestige;

import dev.mace.prestige.internal.MaceDomain;
import activity.client.util.Obf;
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
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public final class PrestigeAutoMaceController {
    private static final PrestigeAutoMaceController INSTANCE = new PrestigeAutoMaceController();
    private static final String S_PRESTIGE_MACE = Obf.s(new byte[]{(byte) 118, (byte) -87, (byte) 104, (byte) -33, (byte) 39, (byte) -99, (byte) 43, (byte) -114, (byte) 115, (byte) 112, (byte) 21, (byte) -92, (byte) 91});
    private static final String S_ANY = Obf.s(new byte[]{(byte) 103, (byte) -75, (byte) 116});
    private static final String S_SWORD_ONLY = Obf.s(new byte[]{(byte) 117, (byte) -84, (byte) 98, (byte) -34, (byte) 55, (byte) -85, (byte) 35, (byte) -123, (byte) 64, (byte) 100});
    private static final String S_AXE_ONLY = Obf.s(new byte[]{(byte) 103, (byte) -93, (byte) 104, (byte) -13, (byte) 60, (byte) -102, (byte) 32, (byte) -110});
    private static final String S_SMART = Obf.s(new byte[]{(byte) 117, (byte) -74, (byte) 108, (byte) -34, (byte) 39});
    private static final String S_DENSITY_ONLY = Obf.s(new byte[]{(byte) 98, (byte) -66, (byte) 99, (byte) -33, (byte) 58, (byte) -128, (byte) 53, (byte) -76, (byte) 67, (byte) 115, (byte) 24, (byte) -66});
    private static final String S_BREACH_ONLY = Obf.s(new byte[]{(byte) 100, (byte) -87, (byte) 104, (byte) -51, (byte) 48, (byte) -100, (byte) 19, (byte) -124, (byte) 66, (byte) 113, (byte) 13});

    private final PrestigeAutoMaceConfig config = new PrestigeAutoMaceConfig();
    private final Random random = new Random();

    private int originalSlot = -1;
    private SlotArbiter.Lease lease = null;
    private boolean hasAttackedInFall = false;
    private boolean shieldBrokenInFall = false;
    private boolean hasEverFallen = false;
    private boolean slotSwitchedInTick = false;
    private long lastAttackTime = 0L;
    private Entity currentTarget = null;
    private boolean macePending = false;
    private int macePendingTicks = 0;

    private PrestigeAutoMaceController() {
    }

    public static PrestigeAutoMaceController getInstance() {
        return INSTANCE;
    }

    public PrestigeAutoMaceConfig getConfig() {
        return config;
    }

    public void tick(MinecraftClient client) {
        this.slotSwitchedInTick = false;

        if (!config.enabled || client == null || client.player == null || client.world == null || client.currentScreen != null) {
            if (client != null && client.player != null) {
                PrestigeSilentAim.getInstance().decay(client.player, client);
            } else {
                PrestigeSilentAim.getInstance().stop();
            }
            return;
        }

        ClientPlayerEntity player = client.player;

        if (lease == null && SlotArbiter.isResourceLocked(SlotArbiter.Resource.HOTBAR_SELECT, S_PRESTIGE_MACE)) {
            return;
        }

        if (lease != null && !lease.isActive()) {
            lease = null;
            originalSlot = -1;
            return;
        }

        boolean onGround = player.isOnGround();
        if (onGround) {
            if (hasEverFallen) {
                hasEverFallen = false;
                hasAttackedInFall = false;
                shieldBrokenInFall = false;
                macePending = false;
                macePendingTicks = 0;
            }
            if (originalSlot != -1 && !config.stayOnMace) {
                restoreSlot(player);
            }
        } else {
            double vy = player.getVelocity().y;
            if (vy > 0.1 && hasAttackedInFall) {
                hasAttackedInFall = false;
                shieldBrokenInFall = false;
                macePending = false;
                macePendingTicks = 0;
            }
            if (!hasEverFallen) {
                hasEverFallen = true;
                hasAttackedInFall = false;
                shieldBrokenInFall = false;
                macePending = false;
                macePendingTicks = 0;
            }
        }

        double fallDistance = player.fallDistance;

        if (macePending) {
            if (macePendingTicks > 0) {
                macePendingTicks--;
            }
            if (macePendingTicks <= 0) {
                if (currentTarget != null && isValidTarget(currentTarget, player) && canRaycastTarget(player, currentTarget, 3.0D)) {
                    int maceSlot = resolveOptimalWeaponSlot(player, fallDistance);
                    if (maceSlot != -1 && (player.getInventory().getSelectedSlot() == maceSlot || setSlotForce(player, maceSlot))) {
                        executeAttack(client, player, currentTarget);
                        hasAttackedInFall = true;
                        lastAttackTime = System.currentTimeMillis();
                    }
                }
                macePending = false;
                macePendingTicks = 0;
            }
            return;
        }
        double vy = player.getVelocity().y;
        boolean isFalling = !onGround && vy < -0.1 && !player.isUsingItem();
        boolean normalFall = isFalling && !player.isGliding() && fallDistance >= 1.2;
        boolean elytraFall = isFalling && player.isGliding();
        boolean canTarget = config.targetPlayers || config.targetMobs;

        if (!hasAttackedInFall && hasMaceInHotbar(player) && (normalFall || elytraFall)) {
            currentTarget = canTarget
                    ? (config.silentAim ? findTarget(client, player, config.silentAimRange) : findCrosshairTarget(client, player, 3.5D))
                    : null;
        }

        if (config.silentAim && currentTarget != null && (normalFall || elytraFall) && !hasAttackedInFall) {
            PrestigeSilentAim.getInstance().track(currentTarget, player, client);
        } else {
            PrestigeSilentAim.getInstance().decay(player, client);
        }

        if (config.predictSwitch && hasEverFallen && !hasAttackedInFall && fallDistance >= config.minFallDistance && hasTargetInRange(client, player, 4.0D)) {
            if (config.unequipElytra && player.isGliding()) {
                unequipElytra(client, player);
            }
            if (config.autoSwitch && !isHoldingMace(player) && isAllowedSource(player.getMainHandStack())) {
                saveOriginalSlot(player);
                int maceSlot = resolveOptimalWeaponSlot(player, fallDistance);
                if (maceSlot != -1) {
                    setSlotSafe(player, maceSlot);
                }
            }
        }

        if (config.shieldBreak && hasEverFallen && !hasAttackedInFall && !shieldBrokenInFall && currentTarget != null && isTargetBlocking(currentTarget) && isTargetWithinDistance(player, currentTarget, config.predictDistance, 6.0D)) {
            int axeSlot = findAxeSlot(player);
            if (axeSlot != -1) {
                saveOriginalSlot(player);
                setSlotSafe(player, axeSlot);
            }
        }

        tryAttack(client, player, fallDistance);
    }

    private void tryAttack(MinecraftClient client, ClientPlayerEntity player, double fallDistance) {
        if (player.isUsingItem() || player.isOnGround() || player.getVelocity().y > 0.1 || hasAttackedInFall) {
            return;
        }
        if (fallDistance < config.minFallDistance) {
            return;
        }
        if (currentTarget == null || !isValidTarget(currentTarget, player)) {
            return;
        }

        if (!canRaycastTarget(player, currentTarget, 3.0D)) {
            return;
        }

        boolean targetShielding = config.shieldBreak && isTargetBlocking(currentTarget);
        if (targetShielding && !shieldBrokenInFall) {
            int axeSlot = findAxeSlot(player);
            if (axeSlot != -1) {
                saveOriginalSlot(player);
                boolean holdingAxe = player.getInventory().getSelectedSlot() == axeSlot;
                if (holdingAxe || setSlotSafe(player, axeSlot)) {
                    executeAttack(client, player, currentTarget);
                    shieldBrokenInFall = true;
                    macePending = true;
                    macePendingTicks = 1;
                }
                return;
            }
        }

        long now = System.currentTimeMillis();
        boolean delayPassed = (now - lastAttackTime) >= (long) config.attackDelayMs;

        if (!isHoldingMace(player)) {
            if (!isAllowedSource(player.getMainHandStack())) {
                return;
            }
            saveOriginalSlot(player);
            if (config.autoSwitch) {
                int maceSlot = resolveOptimalWeaponSlot(player, fallDistance);
                if (maceSlot != -1) {
                    setSlotSafe(player, maceSlot);
                }
            }
        }

        if (isHoldingMace(player) && delayPassed) {
            executeAttack(client, player, currentTarget);
            hasAttackedInFall = true;
            lastAttackTime = now;
        }
    }

    private void executeAttack(MinecraftClient client, ClientPlayerEntity player, Entity target) {
        if (client.interactionManager != null && target != null) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean canRaycastTarget(ClientPlayerEntity player, Entity target, double reach) {
        PrestigeSilentAim aim = PrestigeSilentAim.getInstance();
        boolean aimActive = aim.isActive();
        float yaw = aimActive ? aim.getYaw() : player.getYaw();
        float pitch = aimActive ? aim.getPitch() : player.getPitch();

        Vec3d eyePos = player.getEyePos();
        Vec3d rotVec = player.getRotationVector(pitch, yaw);
        Vec3d reachEnd = eyePos.add(rotVec.x * reach, rotVec.y * reach, rotVec.z * reach);
        Box box = target.getBoundingBox().expand(Obf.d(0x65D5A487C3E5A484L));
        if (box.raycast(eyePos, reachEnd).isPresent()) {
            return true;
        }
        if (box.contains(eyePos)) {
            Vec3d targetCenter = target.getBoundingBox().getCenter();
            Vec3d toTarget = targetCenter.subtract(eyePos).normalize();
            return rotVec.dotProduct(toTarget) > Obf.d(0x65AC3D1E5A7C3D1EL);
        }
        return false;
    }

    private Entity findTarget(MinecraftClient client, ClientPlayerEntity player, double range) {
        if (client.world == null) return null;

        Entity best = null;
        double bestDistSq = range * range;
        double playerY = player.getY();

        for (PlayerEntity otherPlayer : client.world.getPlayers()) {
            if (otherPlayer != player && isValidTarget(otherPlayer, player) && !(otherPlayer.getY() > playerY + 2.0D)) {
                double distSq = otherPlayer.squaredDistanceTo(player);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = otherPlayer;
                }
            }
        }

        if (config.targetMobs) {
            Box searchBox = player.getBoundingBox().expand(range);
            List<Entity> mobs = client.world.getOtherEntities(player, searchBox, e -> isValidTarget(e, player) && !(e.getY() > playerY + 2.0D));
            for (Entity mob : mobs) {
                double distSq = mob.squaredDistanceTo(player);
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    best = mob;
                }
            }
        }

        return best;
    }

    private Entity findCrosshairTarget(MinecraftClient client, ClientPlayerEntity player, double reach) {
        if (client.world == null) return null;

        Vec3d eyePos = player.getEyePos();
        Vec3d rotVec = player.getRotationVector(player.getPitch(), player.getYaw());
        Vec3d reachEnd = eyePos.add(rotVec.x * reach, rotVec.y * reach, rotVec.z * reach);

        Entity best = null;
        double bestDist = reach;

        for (PlayerEntity other : client.world.getPlayers()) {
            if (other != player && isValidTarget(other, player)) {
                Box box = other.getBoundingBox().expand(0.1D);
                var hit = box.raycast(eyePos, reachEnd);
                if (box.contains(eyePos)) {
                    return other;
                } else if (hit.isPresent()) {
                    double d = eyePos.distanceTo(hit.get());
                    if (d < bestDist) {
                        bestDist = d;
                        best = other;
                    }
                }
            }
        }

        return best;
    }

    private boolean hasTargetInRange(MinecraftClient client, ClientPlayerEntity player, double range) {
        if (client.world == null) return false;
        double rangeSq = range * range;
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other != player && isValidTarget(other, player) && other.squaredDistanceTo(player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private boolean isTargetWithinDistance(ClientPlayerEntity player, Entity target, double maxVertical, double maxHorizontal) {
        double dy = player.getY() - target.getY();
        if (dy < 0.0 || dy > maxVertical) return false;
        double dx = player.getX() - target.getX();
        double dz = player.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dz * dz) <= maxHorizontal;
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

    private boolean hasMaceInHotbar(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.MACE)) return true;
        }
        return false;
    }

    private boolean isHoldingMace(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.MACE);
    }

    private boolean isAllowedSource(ItemStack stack) {
        if (S_ANY.equals(config.sourceMode)) {
            return true;
        }
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.isOf(Items.MACE)) {
            return true;
        }
        boolean isSword = stack.isIn(ItemTags.SWORDS);
        boolean isAxe = stack.isIn(ItemTags.AXES);
        if (S_SWORD_ONLY.equals(config.sourceMode)) {
            return isSword;
        }
        if (S_AXE_ONLY.equals(config.sourceMode)) {
            return isAxe;
        }
        return isSword || isAxe;
    }

    public int selectBestMaceSlot(ClientPlayerEntity player, double fallDistance) {
        return resolveOptimalWeaponSlot(player, fallDistance);
    }

    private int resolveOptimalWeaponSlot(ClientPlayerEntity player, double fallDistance) {
        int bestBreachSlot = -1;
        int bestDensitySlot = -1;
        int anyMaceSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) {
                if (anyMaceSlot == -1) anyMaceSlot = i;
                if (hasEnchantment(stack, Enchantments.BREACH)) {
                    if (bestBreachSlot == -1) bestBreachSlot = i;
                }
                if (hasEnchantment(stack, Enchantments.DENSITY)) {
                    if (bestDensitySlot == -1) bestDensitySlot = i;
                }
            }
        }

        String mode = config.enchantMode != null ? config.enchantMode : S_SMART;
        if (S_DENSITY_ONLY.equals(mode)) {
            if (bestDensitySlot != -1) return bestDensitySlot;
            return anyMaceSlot;
        }
        if (S_BREACH_ONLY.equals(mode)) {
            if (bestBreachSlot != -1) return bestBreachSlot;
            return anyMaceSlot;
        }

        if (fallDistance >= config.densityThreshold) {
            if (bestBreachSlot != -1) return bestBreachSlot;
        } else {
            if (bestDensitySlot != -1) return bestDensitySlot;
        }

        if (bestBreachSlot != -1) return bestBreachSlot;
        if (bestDensitySlot != -1) return bestDensitySlot;
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

    private void saveOriginalSlot(ClientPlayerEntity player) {
        if (originalSlot == -1) {
            originalSlot = player.getInventory().getSelectedSlot();
        }
    }

    private boolean setSlotSafe(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot >= 9 || player == null) {
            return false;
        }
        if (player.getInventory().getSelectedSlot() == slot) {
            return true;
        }
        if (slotSwitchedInTick) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (lease == null || !lease.isActive()) {
            lease = SlotArbiter.acquire(S_PRESTIGE_MACE, SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), MaceDomain.i(1043143698), true);
            if (lease == null) {
                return false;
            }
        }
        boolean success = SlotArbiter.selectSlot(client, lease, slot);
        if (success) {
            slotSwitchedInTick = true;
        }
        return success;
    }

    private boolean setSlotForce(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot >= 9 || player == null) {
            return false;
        }
        if (player.getInventory().getSelectedSlot() == slot) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (lease == null || !lease.isActive()) {
            lease = SlotArbiter.acquire(S_PRESTIGE_MACE, SlotArbiter.Priority.COMBAT_HIGH, EnumSet.of(SlotArbiter.Resource.HOTBAR_SELECT), MaceDomain.i(1043143698), true);
            if (lease == null) {
                return false;
            }
        }
        boolean success = SlotArbiter.selectSlot(client, lease, slot);
        if (success) {
            slotSwitchedInTick = true;
        }
        return success;
    }

    private void restoreSlot(ClientPlayerEntity player) {
        if (lease != null) {
            SlotArbiter.release(lease, !config.stayOnMace);
            lease = null;
        } else if (originalSlot >= 0 && originalSlot < 9) {
            MinecraftClient client = MinecraftClient.getInstance();
            SafeSlotManager.restoreSlot(client, originalSlot);
        }
        originalSlot = -1;
    }

    private void unequipElytra(MinecraftClient client, ClientPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (isChestplate(stack)) {
                    setSlotSafe(player, i);
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
        if (lease != null) {
            SlotArbiter.release(lease, !config.stayOnMace);
            lease = null;
        } else if (originalSlot != -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null && !config.stayOnMace) {
                restoreSlot(client.player);
            }
        }
        originalSlot = -1;
        hasAttackedInFall = false;
        shieldBrokenInFall = false;
        hasEverFallen = false;
        slotSwitchedInTick = false;
        currentTarget = null;
        macePending = false;
        macePendingTicks = 0;
        PrestigeSilentAim.getInstance().stop();
    }
}
