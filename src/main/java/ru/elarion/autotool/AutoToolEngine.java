package ru.elarion.autotool;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Locale;

public final class AutoToolEngine {
    private static long lastSwitchTimeMs = 0L;
    private static long lastAttackEntityTimeMs = 0L;
    private static long lastMiningActivityMs = 0L;

    private static boolean isMiningSessionActive = false;
    private static boolean isCombatSessionActive = false;
    private static int originalHotbarSlot = -1;
    private static int expectedToolSlot = -1;
    private static int swappedFromContainerSlot = -1;
    private static long currentReturnDelayMs = 390L;

    private AutoToolEngine() {}

    private static void recalculateReturnDelay() {
        double g = java.util.concurrent.ThreadLocalRandom.current().nextGaussian();
        currentReturnDelayMs = Math.max(330L, Math.min(480L, Math.round(390.0 + g * 28.0)));
    }

    public static boolean isCombatSessionActive() {
        return isCombatSessionActive;
    }

    public static boolean isMiningSessionActive() {
        return isMiningSessionActive;
    }

    public static int getOriginalHotbarSlot() {
        return originalHotbarSlot;
    }

    public static int getExpectedToolSlot() {
        return expectedToolSlot;
    }

    public static void setCombatSessionActiveForTest(boolean active, int origSlot, int expSlot) {
        isCombatSessionActive = active;
        originalHotbarSlot = origSlot;
        expectedToolSlot = expSlot;
    }

    public static void setMiningSessionActiveForTest(boolean active, int origSlot, int expSlot) {
        isMiningSessionActive = active;
        originalHotbarSlot = origSlot;
        expectedToolSlot = expSlot;
    }

    public static void resetSession() {
        isMiningSessionActive = false;
        isCombatSessionActive = false;
        originalHotbarSlot = -1;
        expectedToolSlot = -1;
        swappedFromContainerSlot = -1;
        lastSwitchTimeMs = 0L;
        lastAttackEntityTimeMs = 0L;
        lastMiningActivityMs = 0L;
    }

    public static void onAttackEntity() {
        onAttackEntity(null);
    }

    public static void onAttackEntity(Entity entity) {
        long now = System.currentTimeMillis();
        lastAttackEntityTimeMs = now;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        AutoToolConfig config = AutoToolClient.CONFIG;
        if (!config.enabled || !config.weaponSwitch) return;
        if (client.player.isCreative() || client.player.isSpectator()) return;

        Entity target = entity;
        if (target == null) {
            target = client.targetedEntity;
            if (target == null && client.crosshairTarget instanceof EntityHitResult ehr) {
                target = ehr.getEntity();
            }
        }

        triggerWeaponSwitch(client, target, config, now);
    }

    private static void checkCombatTarget(MinecraftClient client, AutoToolConfig config, long now) {
        if (!config.enabled || !config.weaponSwitch) return;
        if (client.player == null || client.player.isCreative() || client.player.isSpectator()) return;

        Entity target = client.targetedEntity;
        if (target == null && client.crosshairTarget instanceof EntityHitResult ehr) {
            target = ehr.getEntity();
        }

        if (target instanceof LivingEntity le && le.isAlive()) {
            if (client.options.attackKey.isPressed() || now - lastAttackEntityTimeMs < 400L) {
                triggerWeaponSwitch(client, target, config, now);
            }
        }
    }

    public static void triggerWeaponSwitch(MinecraftClient client, Entity target, AutoToolConfig config, long now) {
        if (now - lastSwitchTimeMs < 50L) return;

        int bestSlot = findBestWeaponSlot(client, target, config);
        if (bestSlot < 0) {
            lastAttackEntityTimeMs = now;
            return;
        }

        ClientPlayerEntity player = client.player;
        int currentSlot = player.getInventory().getSelectedSlot();

        if (!isCombatSessionActive && !isMiningSessionActive) {
            originalHotbarSlot = currentSlot;
            isCombatSessionActive = true;
            recalculateReturnDelay();
        }
        lastAttackEntityTimeMs = now;

        if (bestSlot < 9) {
            net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, bestSlot);
            expectedToolSlot = bestSlot;
            lastSwitchTimeMs = now;
        }
    }

    private static boolean shouldSuppressForCombat(MinecraftClient client, AutoToolConfig config) {
        if (!config.combatGuard) return false;
        if (client.player == null) return false;
        if (net.fabricmc.pack.api.CombatLockManager.isLocked()) return true;

        if (client.targetedEntity != null) return true;
        if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() != null) return true;

        if (System.currentTimeMillis() - lastAttackEntityTimeMs < 450L) return true;

        if (client.player.isUsingItem()) return true;

        return false;
    }

    private static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> key, String namePattern) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (key != null && entry.getKey().matchesKey(key)) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if (namePattern != null && id.contains(namePattern)) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
    }

    private static int getEfficiencyLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.EFFICIENCY, "eff");
    }

    private static boolean hasSilkTouch(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.SILK_TOUCH, "silk") > 0;
    }

    private static int getFortuneLevel(ItemStack stack) {
        return getEnchantmentLevel(stack, Enchantments.FORTUNE, "fortune");
    }

    public static float getWeaponBaseDamage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        Item item = stack.getItem();

        if (item == Items.NETHERITE_SWORD) return 8.0f;
        if (item == Items.DIAMOND_SWORD) return 7.0f;
        if (item == Items.IRON_SWORD) return 6.0f;
        if (item == Items.STONE_SWORD) return 5.0f;
        if (item == Items.GOLDEN_SWORD || item == Items.WOODEN_SWORD) return 4.0f;
        if (stack.isIn(ItemTags.SWORDS)) return 5.0f;

        if (item == Items.NETHERITE_AXE) return 10.0f;
        if (item == Items.DIAMOND_AXE) return 9.0f;
        if (item == Items.IRON_AXE) return 9.0f;
        if (item == Items.STONE_AXE) return 9.0f;
        if (item == Items.GOLDEN_AXE || item == Items.WOODEN_AXE) return 7.0f;
        if (stack.isIn(ItemTags.AXES)) return 8.0f;

        if (item == Items.MACE) return 6.0f;
        if (item == Items.TRIDENT) return 9.0f;

        return 0.0f;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(Items.MACE) || stack.isOf(Items.TRIDENT);
    }

    public static float evaluateWeaponScore(ItemStack stack, Entity target, AutoToolConfig config) {
        return evaluateWeaponScore(stack, target, config, null);
    }

    public static float evaluateWeaponScore(ItemStack stack, Entity target, AutoToolConfig config, ClientPlayerEntity player) {
        if (stack == null || stack.isEmpty()) return -100.0f;
        if (!isWeapon(stack)) return -100.0f;

        if (config.durabilitySaver && stack.isDamageable()) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining <= config.durabilityThreshold) {
                return -1000.0f;
            }
        }

        float baseDmg = getWeaponBaseDamage(stack);
        float score = 0.0f;

        boolean isAxe = stack.isIn(ItemTags.AXES);
        boolean isSword = stack.isIn(ItemTags.SWORDS);
        boolean isMace = stack.isOf(Items.MACE);
        boolean isTrident = stack.isOf(Items.TRIDENT);

        if (isSword) {
            score = baseDmg * 2.0f; // Swords have high sustained DPS (1.6 attack speed)
        } else if (isAxe) {
            score = baseDmg * 1.5f; // High single-hit burst damage
            if (target instanceof PlayerEntity pe && pe.isBlocking()) {
                score += 60.0f; // Disables shield blocking!
            }
        } else if (isMace) {
            score = baseDmg * 1.6f;
            if (player != null && player.fallDistance > 1.5f) {
                score += 25.0f + player.fallDistance * 6.0f; // Massive falling smash attack
            }
            int density = getEnchantmentLevel(stack, Enchantments.DENSITY, "density");
            if (density > 0) {
                score += density * 3.0f;
            }
        } else if (isTrident) {
            score = baseDmg * 1.3f;
        }

        // Enchantments
        int sharpness = getEnchantmentLevel(stack, Enchantments.SHARPNESS, "sharpness");
        if (sharpness > 0) {
            score += (sharpness * 1.5f);
        }

        if (target instanceof LivingEntity le) {
            try {
                String typeName = le.getType().toString().toLowerCase(Locale.ROOT);
                boolean isUndead = typeName.contains("zombie") || typeName.contains("skeleton") ||
                                   typeName.contains("wither") || typeName.contains("phantom") ||
                                   typeName.contains("drowned") || typeName.contains("husk") ||
                                   typeName.contains("stray") || typeName.contains("zombified");
                if (isUndead) {
                    int smite = getEnchantmentLevel(stack, Enchantments.SMITE, "smite");
                    if (smite > 0) {
                        score += (smite * 3.0f);
                    }
                }
                boolean isArthropod = typeName.contains("spider") || typeName.contains("silverfish") ||
                                      typeName.contains("endermite") || typeName.contains("bee");
                if (isArthropod) {
                    int bane = getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS, "bane");
                    if (bane > 0) {
                        score += (bane * 3.0f);
                    }
                }
            } catch (Throwable ignored) {}
        }

        int fireAspect = getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT, "fire_aspect");
        if (fireAspect > 0) {
            score += (fireAspect * 1.5f);
        }

        int knockback = getEnchantmentLevel(stack, Enchantments.KNOCKBACK, "knockback");
        if (knockback > 0) {
            score += (knockback * 0.5f);
        }

        // Status effects (Strength / Weakness)
        if (player != null) {
            try {
                if (player.hasStatusEffect(StatusEffects.STRENGTH)) {
                    int strengthLvl = player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1;
                    score += (strengthLvl * 3.0f);
                }
                if (player.hasStatusEffect(StatusEffects.WEAKNESS)) {
                    score = Math.max(0.0f, score - 4.0f);
                }
            } catch (Throwable ignored) {}
        }

        return score;
    }

    public static int findBestWeaponSlot(MinecraftClient client, Entity target, AutoToolConfig config) {
        if (client == null || client.player == null) return -1;
        PlayerInventory inv = client.player.getInventory();

        int bestSlot = -1;
        float bestScore = -1.0F;

        int currentSlot = inv.getSelectedSlot();
        ItemStack currentStack = inv.getStack(currentSlot);
        float currentScore = evaluateWeaponScore(currentStack, target, config, client.player);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            float score = evaluateWeaponScore(stack, target, config, client.player);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (currentScore >= bestScore && currentScore > 0.0F) {
            return -1;
        }

        if (bestScore <= 0.0F) {
            if (config.durabilitySaver && currentStack.isDamageable()) {
                int remaining = currentStack.getMaxDamage() - currentStack.getDamage();
                if (remaining <= config.durabilityThreshold) {
                    for (int i = 0; i < 9; i++) {
                        if (inv.getStack(i).isEmpty()) {
                            return i;
                        }
                    }
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = inv.getStack(i);
                        if (!s.isEmpty() && !s.isDamageable()) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }

        return bestSlot;
    }

    private static boolean isSilkTouchMandatory(BlockState state) {
        if (state.isOf(Blocks.ENDER_CHEST)) return true;
        if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE)) return true;
        if (state.isOf(Blocks.GLASS) || state.isOf(Blocks.TINTED_GLASS) || state.isOf(Blocks.GLASS_PANE)) return true;
        if (state.isIn(BlockTags.IMPERMEABLE)) return true;
        if (state.isOf(Blocks.BEEHIVE) || state.isOf(Blocks.BEE_NEST)) return true;
        if (state.isOf(Blocks.TURTLE_EGG) || state.isOf(Blocks.SNIFFER_EGG)) return true;
        if (state.isOf(Blocks.SCULK) || state.isOf(Blocks.SCULK_CATALYST) || state.isOf(Blocks.SCULK_SHRIEKER) || state.isOf(Blocks.SCULK_SENSOR)) return true;
        if (state.isOf(Blocks.AMETHYST_CLUSTER) || state.isOf(Blocks.CAMPFIRE) || state.isOf(Blocks.SOUL_CAMPFIRE)) return true;
        return false;
    }

    private static boolean isStandardOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) ||
               state.isIn(BlockTags.IRON_ORES) ||
               state.isIn(BlockTags.COPPER_ORES) ||
               state.isIn(BlockTags.GOLD_ORES) ||
               state.isIn(BlockTags.REDSTONE_ORES) ||
               state.isIn(BlockTags.LAPIS_ORES) ||
               state.isIn(BlockTags.DIAMOND_ORES) ||
               state.isIn(BlockTags.EMERALD_ORES);
    }

    public static float evaluateToolScore(ItemStack stack, BlockState state, AutoToolConfig config) {
        return evaluateToolScore(stack, state, config, null);
    }

    public static float evaluateToolScore(ItemStack stack, BlockState state, AutoToolConfig config, ClientPlayerEntity player) {
        if (stack == null || stack.isEmpty()) return 0.0F;

        if (config.durabilitySaver && stack.isDamageable()) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining <= config.durabilityThreshold) {
                return -1000.0F;
            }
        }

        if (stack.isIn(ItemTags.SWORDS) || stack.isOf(Items.MACE) || stack.isOf(Items.TRIDENT)) {
            if (stack.isIn(ItemTags.SWORDS)) {
                if (state.isOf(Blocks.COBWEB)) {
                    return 20.0F;
                } else if (state.isOf(Blocks.BAMBOO) || state.isOf(Blocks.BAMBOO_SAPLING)) {
                    return 100.0F;
                }
            }
            return -100.0F;
        }

        if (stack.isOf(Items.SHEARS)) {
            if (state.isOf(Blocks.COBWEB)) return 70.0F;
            if (state.isIn(BlockTags.WOOL)) return 80.0F;
            if (state.isIn(BlockTags.LEAVES)) return 80.0F;
            if (state.isOf(Blocks.VINE) || state.isOf(Blocks.GLOW_LICHEN)) return 80.0F;
            if (state.isOf(Blocks.TRIPWIRE)) return 80.0F;
        }

        float speed = stack.getMiningSpeedMultiplier(state);
        boolean suitable = stack.isSuitableFor(state);

        float score = speed;

        if (speed > 1.0F) {
            int eff = getEfficiencyLevel(stack);
            if (eff > 0) {
                score += (eff * eff + 1);
            }
        }

        if (player != null) {
            try {
                if (player.hasStatusEffect(StatusEffects.HASTE)) {
                    int hasteLvl = player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1;
                    score *= (1.0f + hasteLvl * 0.2f);
                }
                if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                    int fatigueLvl = player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();
                    float factor = switch (fatigueLvl) {
                        case 0 -> 0.3f;
                        case 1 -> 0.09f;
                        case 2 -> 0.0027f;
                        default -> 0.00081f;
                    };
                    score *= factor;
                }
            } catch (Throwable ignored) {}
        }

        if (state.isToolRequired()) {
            if (suitable) {
                score += 2000.0F;
            } else {
                return -50.0F;
            }
        } else if (suitable && speed > 1.0F) {
            score += 100.0F;
        }

        boolean hasSilk = hasSilkTouch(stack);

        if (isSilkTouchMandatory(state)) {
            if (hasSilk) {
                score += 10000.0F;
            } else if (state.isOf(Blocks.ENDER_CHEST)) {
                score -= 500.0F;
            }
        } else if (isStandardOre(state)) {
            if (config.preferSilkTouch) {
                if (hasSilk) score += 3000.0F;
            } else {
                int fortune = getFortuneLevel(stack);
                if (fortune > 0) score += (fortune * 150.0F);
            }
        }

        return score;
    }

    public static int findBestSlot(MinecraftClient client, BlockState state, AutoToolConfig config) {
        if (client.player == null) return -1;
        PlayerInventory inv = client.player.getInventory();

        int bestSlot = -1;
        float bestScore = -1.0F;

        int currentSlot = inv.getSelectedSlot();
        ItemStack currentStack = inv.getStack(currentSlot);
        float currentScore = evaluateToolScore(currentStack, state, config, client.player);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            float score = evaluateToolScore(stack, state, config, client.player);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (currentScore >= bestScore && currentScore > 1.0F) {
            return -1;
        }

        if (bestScore <= 1.0F) {
            if (config.durabilitySaver && currentStack.isDamageable()) {
                int remaining = currentStack.getMaxDamage() - currentStack.getDamage();
                if (remaining <= config.durabilityThreshold) {
                    // 1. Try empty hotbar slot
                    for (int i = 0; i < 9; i++) {
                        if (inv.getStack(i).isEmpty()) {
                            return i;
                        }
                    }
                    // 2. Fallback to non-damageable item in hotbar (blocks, food, torches, etc.)
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = inv.getStack(i);
                        if (!s.isEmpty() && !s.isDamageable()) {
                            return i;
                        }
                    }
                    // 3. Fallback to any damageable item with safe durability
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = inv.getStack(i);
                        if (!s.isEmpty() && s.isDamageable()) {
                            int rem = s.getMaxDamage() - s.getDamage();
                            if (rem > config.durabilityThreshold) {
                                return i;
                            }
                        }
                    }
                }
            }
            if (!state.isToolRequired()) {
                return -1;
            }
        }

        return bestSlot;
    }

    public static void onAttackBlock(ClientPlayerInteractionManager im, BlockPos pos, Direction direction, boolean currentlyBreakingThis) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;

        AutoToolConfig config = AutoToolClient.CONFIG;
        if (!config.enabled) return;

        if (client.player.isCreative() || client.player.isSpectator()) return;

        GameMode gameMode = client.interactionManager.getCurrentGameMode();
        if (gameMode == null || client.player.isBlockBreakingRestricted(client.world, pos, gameMode)) {
            return;
        }

        if (shouldSuppressForCombat(client, config)) {
            return;
        }

        if (config.lockWhileMining && currentlyBreakingThis) {
            lastMiningActivityMs = System.currentTimeMillis();
            return;
        }

        World world = client.world;
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;

        float hardness = state.getHardness(world, pos);
        if (hardness < 0.0F) return;

        if (config.ignoreInstantBreak && hardness == 0.0F) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSwitchTimeMs < 50L) {
            return;
        }

        int bestSlot = findBestSlot(client, state, config);
        if (bestSlot < 0) {
            lastMiningActivityMs = now;
            return;
        }

        ClientPlayerEntity player = client.player;
        int currentSlot = player.getInventory().getSelectedSlot();

        if (!isMiningSessionActive && !isCombatSessionActive) {
            originalHotbarSlot = currentSlot;
            swappedFromContainerSlot = -1;
            isMiningSessionActive = true;
            recalculateReturnDelay();
        }
        lastMiningActivityMs = now;

        if (bestSlot < 9) {
            net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, bestSlot);
            expectedToolSlot = bestSlot;
            lastSwitchTimeMs = now;
        }
    }

    public static void onBlockBroken(BlockPos pos) {
        lastMiningActivityMs = System.currentTimeMillis();
    }

    public static void onStopMining() {
        lastMiningActivityMs = System.currentTimeMillis();
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        AutoToolConfig config = AutoToolClient.CONFIG;
        long now = System.currentTimeMillis();

        // 1. Combat target check
        if (config.enabled && config.weaponSwitch && !isMiningSessionActive) {
            checkCombatTarget(client, config, now);
        }

        // 2. Active combat session return
        if (isCombatSessionActive) {
            if (client.currentScreen != null || client.player.isDead() || client.player.getHealth() <= 0.0F || !config.enabled) {
                isCombatSessionActive = false;
                originalHotbarSlot = -1;
                expectedToolSlot = -1;
                return;
            }

            if (!config.restorePreviousItem) {
                isCombatSessionActive = false;
                originalHotbarSlot = -1;
                expectedToolSlot = -1;
                return;
            }

            ClientPlayerEntity player = client.player;
            if (!config.singleSlotMode) {
                int currentSlot = player.getInventory().getSelectedSlot();
                if (currentSlot != expectedToolSlot && currentSlot != originalHotbarSlot) {
                    isCombatSessionActive = false;
                    originalHotbarSlot = -1;
                    expectedToolSlot = -1;
                    return;
                }
            }

            boolean isAttacking = client.options.attackKey.isPressed();
            Entity target = client.targetedEntity;
            if (target == null && client.crosshairTarget instanceof EntityHitResult ehr) {
                target = ehr.getEntity();
            }
            boolean isTargetingLiving = (target instanceof LivingEntity le && le.isAlive());

            if (isAttacking && isTargetingLiving) {
                lastAttackEntityTimeMs = now;
                recalculateReturnDelay();
                return;
            }

            if (now - lastAttackEntityTimeMs >= currentReturnDelayMs) {
                if (originalHotbarSlot >= 0 && originalHotbarSlot < 9) {
                    if (player.getInventory().getSelectedSlot() == expectedToolSlot) {
                        net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, originalHotbarSlot);
                    }
                }
                isCombatSessionActive = false;
                originalHotbarSlot = -1;
                expectedToolSlot = -1;
            }
            return;
        }

        // 3. Active mining session return
        if (!isMiningSessionActive) {
            return;
        }

        if (client.currentScreen != null || client.player.isDead() || client.player.getHealth() <= 0.0F) {
            isMiningSessionActive = false;
            originalHotbarSlot = -1;
            expectedToolSlot = -1;
            swappedFromContainerSlot = -1;
            return;
        }

        if (!config.enabled || !config.restorePreviousItem) {
            isMiningSessionActive = false;
            originalHotbarSlot = -1;
            expectedToolSlot = -1;
            swappedFromContainerSlot = -1;
            return;
        }

        ClientPlayerEntity player = client.player;

        if (!config.singleSlotMode) {
            int currentSlot = player.getInventory().getSelectedSlot();
            if (currentSlot != expectedToolSlot && currentSlot != originalHotbarSlot) {
                isMiningSessionActive = false;
                originalHotbarSlot = -1;
                expectedToolSlot = -1;
                return;
            }
        }

        boolean isAttacking = client.options.attackKey.isPressed();
        boolean isBreaking = client.interactionManager.isBreakingBlock();

        if (isAttacking || isBreaking) {
            lastMiningActivityMs = now;
            recalculateReturnDelay();
            return;
        }

        if (now - lastMiningActivityMs >= currentReturnDelayMs) {
            if (originalHotbarSlot >= 0 && originalHotbarSlot < 9) {
                if (player.getInventory().getSelectedSlot() == expectedToolSlot) {
                    net.fabricmc.pack.api.SafeSlotManager.selectSlot(client, originalHotbarSlot);
                }
            }

            isMiningSessionActive = false;
            originalHotbarSlot = -1;
            expectedToolSlot = -1;
            swappedFromContainerSlot = -1;
        }
    }
}
