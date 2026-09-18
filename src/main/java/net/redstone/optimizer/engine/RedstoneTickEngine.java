package net.redstone.optimizer.engine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.redstone.optimizer.config.RedstoneOptimizerConfig;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class RedstoneTickEngine {
    private static final Random RNG = new Random();
    private static Field lastSelectedSlotField;
    private static Field lastAttackedTicksField;
    public static volatile boolean maceActive = false;

    private static String dec(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private static final String S_MACE = dec("bWFjZQ==");
    private static final String S_BULAVA = dec("0LHRg9C70LDQstCw");
    private static final String S_BREACH = dec("YnJlYWNo");
    private static final String S_PROBITIE = dec("0L/RgNC+0LHQuNGC0LjQtQ==");
    private static final String S_DENSITY = dec("ZGVuc2l0eQ==");
    private static final String S_PLOTNOST = dec("0L/Qu9C+0YLQvdC+0YHRgtG8");
    private static final String S_WIND_BURST = dec("d2luZF9idXJzdA==");
    private static final String S_VETROVOY = dec("0LLQtdGC0YDQvtCy0L7QuQ==");
    private static final String S_SHARPNESS = dec("c2hhcnBuZXNz");
    private static final String S_OSTROTA = dec("0L7RgdGC0YDQvtGC0LA=");
    private static final String S_FIRE_ASPECT = dec("ZmlyZV9hc3BlY3Q=");
    private static final String S_ZAGOVOR = dec("0LfQsNCz0L7QstC+0YA=");
    private static final String S_LOOTING = dec("bG9vdGluZw==");
    private static final String S_DOBYCHA = dec("0LTQvtCx0YvRh9Cw");

    static {
        try {
            for (Field f : ClientPlayerInteractionManager.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if ("lastselectedslot".equals(name) || "field_3721".equals(name) || name.contains("lastselectedslot")) {
                        f.setAccessible(true);
                        lastSelectedSlotField = f;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            for (Field f : PlayerEntity.class.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if ("lastattackedticks".equals(name) || "field_6273".equals(name) || name.contains("lastattackedticks")) {
                        f.setAccessible(true);
                        lastAttackedTicksField = f;
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void ensureFullAttackCharge(ClientPlayerEntity player) {
        if (player == null) return;
        try {
            if (lastAttackedTicksField != null) {
                float needed = player.getAttackCooldownProgressPerTick();
                int minNeeded = (int) Math.ceil(needed) + 2;
                int current = lastAttackedTicksField.getInt(player);
                if (current < minNeeded) {
                    lastAttackedTicksField.setInt(player, minNeeded);
                }
            }
        } catch (Throwable ignored) {}
    }

    private State state = State.IDLE;
    private int initialSlot = -1;
    private int activeMaceSlot = -1;
    private int targetEntityId = -1;
    private int restoreTicksRemaining = 0;
    private int ticksSinceAttack = 0;
    private int cooldownTicks = 0;
    private long lastSwapTimeMs = 0L;
    private long lastBusyTimeMs = 0L;
    private int clientTick = 0;
    private int swapStartTick = -1;
    private int lastSlotChangeTick = -1;
    private long targetHoldDurationMs = 0L;
    private int airTicks = 0;
    private long lastOnGroundTimeMs = System.currentTimeMillis();

    private static long getHumanGaussianDelay(double mean, double stdDev, long min, long max) {
        double gaussian = RNG.nextGaussian();
        long delay = Math.round(mean + gaussian * stdDev);
        return Math.max(min, Math.min(max, delay));
    }

    public boolean isEnabled() {
        return RedstoneOptimizerConfig.enabled;
    }

    public void toggle() {
        RedstoneOptimizerConfig.enabled = !RedstoneOptimizerConfig.enabled;
        RedstoneOptimizerConfig.save();
        if (!RedstoneOptimizerConfig.enabled) {
            clearState();
        }
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!world.isClient() || !(entity instanceof LivingEntity target)) {
            return ActionResult.PASS;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player != player || !RedstoneOptimizerConfig.enabled) {
            return ActionResult.PASS;
        }

        if (state != State.IDLE || cooldownTicks > 0) {
            return ActionResult.PASS;
        }

        if (client.currentScreen != null) {
            return ActionResult.PASS;
        }

        ClientPlayerEntity clientPlayer = client.player;
        if (!clientPlayer.isAlive() || clientPlayer.isSpectator() || clientPlayer.hasVehicle() || clientPlayer.isGliding()) {
            return ActionResult.PASS;
        }

        if (isPlayerBusy(clientPlayer)) {
            return ActionResult.PASS;
        }

        if (hasActiveConflict()) {
            return ActionResult.PASS;
        }

        if (target.isBlocking() || activity.client.module.service.TargetCacheService.isTargetShielding(target)) {
            return ActionResult.PASS;
        }

        long now = System.currentTimeMillis();
        if (now - lastSwapTimeMs < 150L || (clientTick - lastSlotChangeTick < 2 && lastSlotChangeTick >= 0)) {
            return ActionResult.PASS;
        }

        if (!target.isAlive() || target.isSpectator() || target.isRemoved()) {
            return ActionResult.PASS;
        }

        if (!isVanillaTarget(client, target)) {
            return ActionResult.PASS;
        }

        double reach = getVanillaReach(clientPlayer);
        if (!canReach(clientPlayer, target, reach)) {
            return ActionResult.PASS;
        }

        if (RedstoneOptimizerConfig.legitMode && !hasLineOfSight(clientPlayer, target)) {
            return ActionResult.PASS;
        }

        int curSlot = clientPlayer.getInventory().getSelectedSlot();
        ItemStack heldStack = clientPlayer.getInventory().getStack(curSlot);

        if (!isAllowedSourceItem(heldStack)) {
            return ActionResult.PASS;
        }

        if (heldStack.isOf(Items.MACE)) {
            return ActionResult.PASS;
        }

        float attackCooldown = clientPlayer.getAttackCooldownProgress(0.0f);
        float requiredCharge = 0.90f;
        if (attackCooldown < requiredCharge) {
            return ActionResult.PASS;
        }

        if (clientPlayer.isOnGround() || clientPlayer.verticalCollision || clientPlayer.isTouchingWater() || clientPlayer.isClimbing() || clientPlayer.hasVehicle()) {
            airTicks = 0;
            lastOnGroundTimeMs = System.currentTimeMillis();
        }

        long airDurationMs = (!clientPlayer.isOnGround() && !clientPlayer.verticalCollision)
                ? (System.currentTimeMillis() - lastOnGroundTimeMs)
                : 0L;

        boolean isHighFallDensity = (!clientPlayer.isOnGround() && !clientPlayer.verticalCollision)
                && !clientPlayer.isTouchingWater()
                && !clientPlayer.isClimbing()
                && (airDurationMs >= 600L || airTicks >= 12);

        if (RedstoneOptimizerConfig.enchantMode == RedstoneOptimizerConfig.ENCHANT_DENSITY_ONLY && !isHighFallDensity) {
            return ActionResult.PASS;
        }

        int maceSlot = findBestMaceSlot(clientPlayer, curSlot, isHighFallDensity);
        if (maceSlot < 0 || maceSlot == curSlot) {
            return ActionResult.PASS;
        }

        if (RedstoneOptimizerConfig.missChance > 0 && ThreadLocalRandom.current().nextInt(100) < RedstoneOptimizerConfig.missChance) {
            if (RedstoneOptimizerConfig.missBehavior == RedstoneOptimizerConfig.MISS_EMPTY_SWAP) {
                performMaceMiss(client, clientPlayer, curSlot, maceSlot);
                return ActionResult.FAIL;
            } else {
                return ActionResult.PASS;
            }
        }

        performMaceSwap(client, clientPlayer, target, curSlot, maceSlot);
        return ActionResult.PASS;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            clearState();
            return;
        }

        clientTick++;

        ClientPlayerEntity player = client.player;
        if (player.isOnGround() || player.verticalCollision || player.isTouchingWater() || player.isClimbing() || player.hasVehicle()) {
            airTicks = 0;
            lastOnGroundTimeMs = System.currentTimeMillis();
        } else {
            airTicks++;
        }

        if (client.player.isUsingItem() || client.player.isBlocking()) {
            lastBusyTimeMs = System.currentTimeMillis();
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (state == State.IDLE) {
            return;
        }

        if (!client.player.isAlive() || client.currentScreen != null || isPlayerBusy(client.player)) {
            restoreSlot(client);
            clearState();
            return;
        }

        if (activeMaceSlot >= 0 && client.player.getInventory().getSelectedSlot() != activeMaceSlot) {
            clearState();
            return;
        }

        ticksSinceAttack++;
        if (ticksSinceAttack > 20) {
            restoreSlot(client);
            clearState();
            return;
        }

        if (state == State.WAITING_RESTORE) {
            long elapsedMs = System.currentTimeMillis() - lastSwapTimeMs;
            boolean timeExpired = elapsedMs >= targetHoldDurationMs;
            boolean minTicksPassed = (clientTick - swapStartTick) >= 2;
            boolean differentTick = (clientTick != lastSlotChangeTick);

            if (timeExpired && minTicksPassed && differentTick) {
                restoreSlot(client);
                clearState();
                cooldownTicks = 2;
            }
        }
    }

    private void performMaceMiss(MinecraftClient client, ClientPlayerEntity player, int curSlot, int maceSlot) {
        state = State.WAITING_RESTORE;
        initialSlot = curSlot;
        activeMaceSlot = maceSlot;
        targetEntityId = -1;

        int minDelay = Math.min(RedstoneOptimizerConfig.restoreDelayMs, RedstoneOptimizerConfig.randomMaxRestoreDelayMs);
        int maxDelay = Math.max(RedstoneOptimizerConfig.restoreDelayMs, RedstoneOptimizerConfig.randomMaxRestoreDelayMs);
        double mean = (minDelay + maxDelay) / 2.0;
        double stdDev = Math.max(5.0, (maxDelay - minDelay) / 4.0);
        targetHoldDurationMs = getHumanGaussianDelay(mean, stdDev, minDelay, maxDelay);

        swapStartTick = clientTick;
        ticksSinceAttack = 0;
        lastSwapTimeMs = System.currentTimeMillis();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.mace_active", true);
        maceActive = true;

        safeStopUsingItem(client);
        net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, maceSlot, clientTick);
        lastSlotChangeTick = clientTick;

        player.resetTicksSinceLastAttack();
    }

    private void performMaceSwap(MinecraftClient client, ClientPlayerEntity player, LivingEntity target, int curSlot, int maceSlot) {
        state = State.WAITING_RESTORE;
        initialSlot = curSlot;
        activeMaceSlot = maceSlot;
        targetEntityId = target.getId();

        int minDelay = Math.min(RedstoneOptimizerConfig.restoreDelayMs, RedstoneOptimizerConfig.randomMaxRestoreDelayMs);
        int maxDelay = Math.max(RedstoneOptimizerConfig.restoreDelayMs, RedstoneOptimizerConfig.randomMaxRestoreDelayMs);
        double mean = (minDelay + maxDelay) / 2.0;
        double stdDev = Math.max(5.0, (maxDelay - minDelay) / 4.0);
        targetHoldDurationMs = getHumanGaussianDelay(mean, stdDev, minDelay, maxDelay);

        swapStartTick = clientTick;
        ticksSinceAttack = 0;
        lastSwapTimeMs = System.currentTimeMillis();
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.mace_active", true);
        maceActive = true;

        safeStopUsingItem(client);
        net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, maceSlot, clientTick);
        lastSlotChangeTick = clientTick;

        ensureFullAttackCharge(player);
    }

    private boolean isAllowedSourceItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES);
    }

    private int findBestMaceSlot(ClientPlayerEntity player, int currentSlot, boolean isHighFallDensity) {
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 1) continue;

            int score = getMaceScore(stack, isHighFallDensity);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestScore <= 0) {
            return -1;
        }

        return bestSlot;
    }

    private int getMaceScore(ItemStack stack, boolean isHighFallDensity) {
        if (!isMace(stack)) return -1;

        int breachLevel = getBreachLevel(stack);
        int densityLevel = getDensityLevel(stack);
        int windBurstLevel = getWindBurstLevel(stack);

        int mode = RedstoneOptimizerConfig.enchantMode;

        // 1. BREACH ONLY mode
        if (mode == RedstoneOptimizerConfig.ENCHANT_BREACH_ONLY) {
            if (breachLevel <= 0) {
                return -1;
            }
            int score = 10000 + breachLevel * 2000 + densityLevel * 100 + windBurstLevel * 50;
            if (stack.isDamageable()) {
                score += (stack.getMaxDamage() - stack.getDamage()) / 10;
            }
            return score;
        }

        // 2. DENSITY ONLY mode
        if (mode == RedstoneOptimizerConfig.ENCHANT_DENSITY_ONLY) {
            // Must have Density and player must be falling for >= 0.6s
            if (densityLevel <= 0 || !isHighFallDensity) {
                return -1;
            }
            int score = 10000 + densityLevel * 2000 + breachLevel * 100 + windBurstLevel * 50;
            if (stack.isDamageable()) {
                score += (stack.getMaxDamage() - stack.getDamage()) / 10;
            }
            return score;
        }

        // 3. SMART mode:
        if (isHighFallDensity) {
            // High fall (>= 0.6s): Density has top priority for massive smash bonus
            if (densityLevel > 0) {
                int score = 10000 + densityLevel * 2000 + breachLevel * 100 + windBurstLevel * 50;
                if (stack.isDamageable()) {
                    score += (stack.getMaxDamage() - stack.getDamage()) / 10;
                }
                return score;
            } else if (breachLevel > 0) {
                int score = 5000 + breachLevel * 500 + windBurstLevel * 50;
                if (stack.isDamageable()) {
                    score += (stack.getMaxDamage() - stack.getDamage()) / 10;
                }
                return score;
            } else {
                return 2000;
            }
        } else {
            // Ordinary jump (< 0.6s) or on ground: Breach is mandatory!
            // If the mace only has Density (no Breach), do NOT use Density mace; attack with normal sword instead!
            if (breachLevel > 0) {
                int score = 10000 + breachLevel * 2000 + densityLevel * 100 + windBurstLevel * 50;
                if (stack.isDamageable()) {
                    score += (stack.getMaxDamage() - stack.getDamage()) / 10;
                }
                return score;
            } else {
                // No Breach in ordinary jump/ground -> return -1 so player hits with sword
                return -1;
            }
        }
    }

    private static boolean isMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.MACE)) return true;

        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(S_BULAVA) || name.contains(S_MACE)) {
                return true;
            }
        } catch (Exception ignored) {}

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains(S_BULAVA) || str.contains(S_MACE)) {
                    return true;
                }
            }
        }

        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyNbt();
            if (nbt.contains("id")) {
                String idStr = nbt.getString("id").orElse("").toLowerCase(Locale.ROOT);
                if (idStr.contains(S_MACE) || idStr.contains(S_BULAVA)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int getBreachLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.BREACH) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if ((id.contains(S_BREACH) || id.contains(S_PROBITIE)) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains(S_PROBITIE) || str.contains(S_BREACH)) {
                    if (str.contains("iv") || str.contains("4")) return 4;
                    if (str.contains("iii") || str.contains("3")) return 3;
                    if (str.contains("ii") || str.contains("2")) return 2;
                    return 1;
                }
            }
        }

        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(S_PROBITIE) || name.contains(S_BREACH)) {
                if (name.contains("iv") || name.contains("4")) return 4;
                if (name.contains("iii") || name.contains("3")) return 3;
                if (name.contains("ii") || name.contains("2")) return 2;
                return 1;
            }
        } catch (Exception ignored) {}

        return 0;
    }

    private static int getDensityLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.DENSITY) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if ((id.contains(S_DENSITY) || id.contains(S_PLOTNOST)) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains(S_PLOTNOST) || str.contains(S_DENSITY)) {
                    if (str.contains("v") || str.contains("5")) return 5;
                    if (str.contains("iv") || str.contains("4")) return 4;
                    if (str.contains("iii") || str.contains("3")) return 3;
                    if (str.contains("ii") || str.contains("2")) return 2;
                    return 1;
                }
            }
        }

        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(S_PLOTNOST) || name.contains(S_DENSITY)) {
                if (name.contains("v") || name.contains("5")) return 5;
                if (name.contains("iv") || name.contains("4")) return 4;
                if (name.contains("iii") || name.contains("3")) return 3;
                if (name.contains("ii") || name.contains("2")) return 2;
                return 1;
            }
        } catch (Exception ignored) {}

        return 0;
    }

    private static int getWindBurstLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.WIND_BURST) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if ((id.contains(S_WIND_BURST) || id.contains(S_VETROVOY)) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains(S_VETROVOY) || str.contains(S_WIND_BURST)) {
                    if (str.contains("iii") || str.contains("3")) return 3;
                    if (str.contains("ii") || str.contains("2")) return 2;
                    return 1;
                }
            }
        }

        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(S_VETROVOY) || name.contains(S_WIND_BURST)) {
                if (name.contains("iii") || name.contains("3")) return 3;
                if (name.contains("ii") || name.contains("2")) return 2;
                return 1;
            }
        } catch (Exception ignored) {}

        return 0;
    }

    private boolean isPlayerBusy(ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) return true;
        if (player.isUsingItem() || player.isBlocking()) {
            lastBusyTimeMs = System.currentTimeMillis();
            return true;
        }
        return System.currentTimeMillis() - lastBusyTimeMs < 120L;
    }

    private boolean hasActiveConflict() {
        return net.fabricmc.pack.api.CombatLockManager.hasConflictExcludingMace();
    }

    private static void safeStopUsingItem(MinecraftClient client) {
        if (client == null || client.player == null) return;
        if (client.player.isUsingItem()) {
            client.player.clearActiveItem();
        }
    }

    private static void syncSlotTracker(MinecraftClient client, int slot) {
        if (client == null || client.interactionManager == null) return;
        if (lastSelectedSlotField != null) {
            try {
                lastSelectedSlotField.setInt(client.interactionManager, slot);
                return;
            } catch (Throwable ignored) {}
        }
        for (Field f : ClientPlayerInteractionManager.class.getDeclaredFields()) {
            if (f.getType() == int.class) {
                try {
                    f.setAccessible(true);
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if ("lastselectedslot".equals(name) || "field_3721".equals(name) || name.contains("lastselectedslot")) {
                        lastSelectedSlotField = f;
                        f.setInt(client.interactionManager, slot);
                        return;
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void restoreSlot(MinecraftClient client) {
        if (client == null || client.player == null) return;
        safeStopUsingItem(client);
        int cur = client.player.getInventory().getSelectedSlot();
        if (cur == activeMaceSlot) {
            int targetRestore;
            if (initialSlot >= 0 && initialSlot < 9 && isAllowedSourceItem(client.player.getInventory().getStack(initialSlot))) {
                targetRestore = initialSlot;
            } else {
                targetRestore = findBestSwordSlot(client.player);
                if (targetRestore < 0) {
                    targetRestore = (initialSlot >= 0 && initialSlot < 9) ? initialSlot : cur;
                }
            }
            if (targetRestore >= 0 && targetRestore < 9 && targetRestore != cur) {
                net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, targetRestore, clientTick);
                lastSlotChangeTick = clientTick;
            }
        }
    }

    private int findBestSwordSlot(ClientPlayerEntity player) {
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!stack.isIn(ItemTags.SWORDS)) continue;
            if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 1) continue;

            int score = getSwordScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private int getSwordScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isIn(ItemTags.SWORDS)) return -1;

        int sharpness = getSharpnessLevel(stack);
        int tier = getSwordMaterialTier(stack);

        int score = sharpness * 1000;
        score += tier * 100;

        int fireAspect = getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT, S_FIRE_ASPECT, S_ZAGOVOR);
        score += fireAspect * 20;

        int looting = getEnchantmentLevel(stack, Enchantments.LOOTING, S_LOOTING, S_DOBYCHA);
        score += looting * 10;

        if (stack.isDamageable()) {
            score += (stack.getMaxDamage() - stack.getDamage()) / 100;
        }

        return score;
    }

    private static int getSwordMaterialTier(ItemStack stack) {
        if (stack.isOf(Items.NETHERITE_SWORD)) return 6;
        if (stack.isOf(Items.DIAMOND_SWORD)) return 5;
        if (stack.isOf(Items.IRON_SWORD)) return 4;
        if (stack.isOf(Items.GOLDEN_SWORD)) return 3;
        if (stack.isOf(Items.STONE_SWORD)) return 2;
        if (stack.isOf(Items.WOODEN_SWORD)) return 1;

        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        if (id.contains("netherite")) return 6;
        if (id.contains("diamond")) return 5;
        if (id.contains("iron")) return 4;
        return 3;
    }

    private static int getSharpnessLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.SHARPNESS) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if ((id.contains(S_SHARPNESS) || id.contains(S_OSTROTA)) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains(S_OSTROTA) || str.contains(S_SHARPNESS)) {
                    if (str.contains("v") || str.contains("5")) return 5;
                    if (str.contains("iv") || str.contains("4")) return 4;
                    if (str.contains("iii") || str.contains("3")) return 3;
                    if (str.contains("ii") || str.contains("2")) return 2;
                    return 1;
                }
            }
        }

        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains(S_OSTROTA) || name.contains(S_SHARPNESS)) {
                if (name.contains("v") || name.contains("5")) return 5;
                if (name.contains("iv") || name.contains("4")) return 4;
                if (name.contains("iii") || name.contains("3")) return 3;
                if (name.contains("ii") || name.contains("2")) return 2;
                return 1;
            }
        } catch (Exception ignored) {}

        return 0;
    }

    private static int getEnchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key, String idKeyword, String ruKeyword) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(key) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if ((id.contains(idKeyword) || id.contains(ruKeyword)) && entry.getIntValue() > 0) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
    }

    public void reset() {
        clearState();
    }

    private void clearState() {
        state = State.IDLE;
        initialSlot = -1;
        activeMaceSlot = -1;
        targetEntityId = -1;
        restoreTicksRemaining = 0;
        targetHoldDurationMs = 0L;
        swapStartTick = -1;
        ticksSinceAttack = 0;
        airTicks = 0;
        maceActive = false;
        net.fabricmc.pack.api.CombatLockManager.setLock("pvp.mace_active", false);
    }

    private static boolean isVanillaTarget(MinecraftClient client, Entity target) {
        if (client == null || target == null) return false;
        Entity crosshair = activity.client.module.service.TargetCacheService.getCrosshairTarget(client);
        if (crosshair == target) return true;
        if (client.targetedEntity == target) return true;
        if (client.crosshairTarget instanceof EntityHitResult ehr) {
            return ehr.getEntity() == target;
        }
        return false;
    }

    private static double getVanillaReach(ClientPlayerEntity player) {
        double reach = 3.0D;
        if (player != null) {
            try {
                double entityRange = player.getEntityInteractionRange();
                if (entityRange > 0.0D) {
                    reach = Math.max(3.0D, entityRange);
                }
            } catch (Throwable ignored) {}
        }
        return reach;
    }

    private static double getDistanceToBox(Vec3d pos, Box box) {
        double clampedX = Math.max(box.minX, Math.min(pos.x, box.maxX));
        double clampedY = Math.max(box.minY, Math.min(pos.y, box.maxY));
        double clampedZ = Math.max(box.minZ, Math.min(pos.z, box.maxZ));
        return pos.distanceTo(new Vec3d(clampedX, clampedY, clampedZ));
    }

    private static boolean canReach(ClientPlayerEntity player, Entity target, double maxReach) {
        if (target == null || !target.isAlive() || player == null) return false;
        Vec3d eyePos = player.getEyePos();
        Box box = target.getBoundingBox();
        double dist = getDistanceToBox(eyePos, box);
        return dist <= maxReach + 0.1D;
    }

    private static boolean hasLineOfSight(ClientPlayerEntity player, Entity target) {
        Vec3d eye = player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        World world = player.getEntityWorld();

        BlockHitResult hit = world.raycast(new RaycastContext(
                eye,
                targetEye,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        return hit.getType() == HitResult.Type.MISS || eye.squaredDistanceTo(hit.getPos()) >= eye.squaredDistanceTo(targetEye) - 0.2D;
    }

    private enum State {
        IDLE,
        WAITING_RESTORE
    }
}
