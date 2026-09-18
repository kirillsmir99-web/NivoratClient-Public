package ru.elarion.autotool;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private static int originalHotbarSlot = -1;
    private static int expectedToolSlot = -1;
    private static int swappedFromContainerSlot = -1;
    private static long currentReturnDelayMs = 390L;

    private static void recalculateReturnDelay() {
        double g = java.util.concurrent.ThreadLocalRandom.current().nextGaussian();
        currentReturnDelayMs = Math.max(330L, Math.min(480L, Math.round(390.0 + g * 28.0)));
    }





    public static void onAttackEntity() {
        lastAttackEntityTimeMs = System.currentTimeMillis();
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

    private static int getEfficiencyLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.EFFICIENCY)) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if (id.contains("efficiency") || id.contains("eff")) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
    }

    private static boolean hasSilkTouch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.SILK_TOUCH)) {
                    return true;
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if (id.contains("silk_touch")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getFortuneLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.FORTUNE)) {
                    return entry.getIntValue();
                }
                String id = entry.getKey().getIdAsString().toLowerCase(Locale.ROOT);
                if (id.contains("fortune")) {
                    return entry.getIntValue();
                }
            }
        }
        return 0;
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
        float currentScore = evaluateToolScore(currentStack, state, config);

        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            float score = evaluateToolScore(stack, state, config);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }


        
        if (currentScore >= bestScore && currentScore > 1.0F) {
            return -1;
        }

        if (bestScore <= 1.0F && !state.isToolRequired()) {
            
            
            if (config.durabilitySaver && currentStack.isDamageable()) {
                int remaining = currentStack.getMaxDamage() - currentStack.getDamage();
                if (remaining <= config.durabilityThreshold) {
                    for (int i = 0; i < 9; i++) {
                        if (inv.getStack(i).isEmpty()) {
                            return i;
                        }
                    }
                }
            }
            return -1;
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

        
        if (!isMiningSessionActive) {
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
        if (!isMiningSessionActive || client.player == null || client.interactionManager == null) {
            return;
        }

        
        if (client.currentScreen != null || client.player.isDead() || client.player.getHealth() <= 0.0F) {
            isMiningSessionActive = false;
            originalHotbarSlot = -1;
            expectedToolSlot = -1;
            swappedFromContainerSlot = -1;
            return;
        }

        AutoToolConfig config = AutoToolClient.CONFIG;
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

        
        
        
        long now = System.currentTimeMillis();
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
