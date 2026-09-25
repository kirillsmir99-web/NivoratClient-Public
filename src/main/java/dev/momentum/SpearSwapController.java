package dev.momentum;

import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.GaussianTimingEngine;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.KineticWeaponComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class SpearSwapController {

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int spearSlot = -1;
    private int lastNonSpearSlot = 0;
    private int targetEntityId = -1;
    private int targetDelayTicks = 1;
    private int restoreDelayTicks = 2;
    private int targetRestoreDelayMs = 185;
    private int cooldownTicks = 0;

    private long swapStartTick = 0L;
    private long strikeTick = 0L;
    private long lastSwapTimeMs = 0L;
    private long lastStrikeTimeMs = 0L;
    private long lastRestoreTimeMs = 0L;
    private long clientTickCount = 0L;
    private long lastRestoreTick = -100L;
    private long lastSlotChangeTick = -100L;

    public SpearSwapController() {}

    public boolean isEnabled() {
        return SpearConfig.enabled;
    }

    public void toggle() {
        SpearConfig.enabled = !SpearConfig.enabled;
        SpearConfig.save();
        if (!SpearConfig.enabled) {
            resetAll();
        }
    }

    public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        return ActionResult.PASS;
    }

    private boolean isCooldownActive(long now) {
        if (cooldownTicks > 0) {
            return true;
        }
        if (SpearConfig.securityMode == SpearConfig.MODE_LEGIT) {
            return (clientTickCount - lastRestoreTick < 2L);
        } else if (SpearConfig.securityMode == SpearConfig.MODE_SEMI_LEGIT) {
            return (clientTickCount - lastRestoreTick < 1L);
        }
        return false;
    }

    public void onTrigger(MinecraftClient client) {
        try {
            if (!SpearConfig.enabled) return;
            if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;
            if (client.currentScreen != null) return;
            if (!client.player.isAlive()) return;
            if (state != State.IDLE) return;
            if (isPlayerBusy(client, client.player)) return;

            long now = System.currentTimeMillis();
            if (isCooldownActive(now)) {
                return;
            }

            suppressInputs(client);

            int cur = client.player.getInventory().getSelectedSlot();
            if (cur >= 0 && cur < 9) {
                ItemStack stack = client.player.getInventory().getStack(cur);
                if (!stack.isEmpty() && !isSpear(stack)) {
                    originalSlot = cur;
                    lastNonSpearSlot = cur;
                }
            }

            handleTriggerStart(client, client.player);
        } catch (Throwable ignored) {
            resetAll();
        }
    }

    public void onKeyRelease(MinecraftClient client) {
    }

    public void tick(MinecraftClient client) {
        try {
            clientTickCount++;

            if (cooldownTicks > 0) {
                cooldownTicks--;
            }

            if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
                resetAll();
                return;
            }

            ClientPlayerEntity player = client.player;
            int currentSlot = player.getInventory().getSelectedSlot();
            if (currentSlot >= 0 && currentSlot < 9) {
                ItemStack currentStack = player.getInventory().getStack(currentSlot);
                if (!currentStack.isEmpty() && !isSpear(currentStack)) {
                    lastNonSpearSlot = currentSlot;
                }
            }

            if (!player.isAlive() || client.currentScreen != null) {
                if (state != State.IDLE) {
                    restoreSlot(client);
                }
                resetAll();
                return;
            }

            if (state == State.IDLE) {
                return;
            }

            if (spearSlot >= 0 && currentSlot != spearSlot) {
                clearState();
                return;
            }

            if (state == State.SWAPPED_TO_SPEAR) {
                suppressInputs(client);
                handleSwappedToSpear(client, player);
                return;
            }

            if (state == State.WAITING_RESTORE) {
                suppressInputs(client);
                handleWaitingRestore(client);
            }
        } catch (Throwable ignored) {
            resetAll();
        }
    }

    private void suppressInputs(MinecraftClient client) {
        if (client == null || client.options == null) return;
        try {
            if (client.options.playerListKey != null && client.options.playerListKey.isPressed()) {
                client.options.playerListKey.setPressed(false);
                while (client.options.playerListKey.wasPressed()) {}
            }
            if (client.options.swapHandsKey != null && client.options.swapHandsKey.isPressed()) {
                client.options.swapHandsKey.setPressed(false);
                while (client.options.swapHandsKey.wasPressed()) {}
            }
            if (client.options.hotbarKeys != null) {
                for (KeyBinding k : client.options.hotbarKeys) {
                    if (k != null) {
                        k.setPressed(false);
                        while (k.wasPressed()) {}
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void handleTriggerStart(MinecraftClient client, ClientPlayerEntity player) {
        if (state != State.IDLE) return;
        if (isPlayerBusy(client, player)) return;
        if (CombatLockManager.isLocked()) return;

        long now = System.currentTimeMillis();
        if (isCooldownActive(now)) return;

        if (player.getAttackCooldownProgress(0.0F) < 0.90F) {
            return;
        }

        int curSlot = player.getInventory().getSelectedSlot();
        ItemStack inHand = player.getInventory().getStack(curSlot);

        LivingEntity target = findTarget(client, inHand);
        targetEntityId = (target != null) ? target.getId() : -1;

        if (isSpear(inHand)) {
            originalSlot = findWeaponSlot(player);
            spearSlot = curSlot;
            startSpearActive(client, player, curSlot, false);
            return;
        }

        int targetSpearSlot = findSpearSlot(player, curSlot);
        if (targetSpearSlot < 0) {
            return;
        }

        ItemStack targetStack = player.getInventory().getStack(targetSpearSlot);
        if (targetStack.isEmpty() || !isSpear(targetStack)) {
            return;
        }

        if (originalSlot < 0 || originalSlot >= 9 || player.getInventory().getStack(originalSlot).isEmpty() || isSpear(player.getInventory().getStack(originalSlot))) {
            if (!inHand.isEmpty() && !isSpear(inHand)) {
                originalSlot = curSlot;
            } else {
                originalSlot = findWeaponSlot(player);
            }
        }

        spearSlot = targetSpearSlot;
        startSpearActive(client, player, targetSpearSlot, true);
    }

    private int calculateDelay() {
        int floor = SpearConfig.getMinFloor();
        if (!SpearConfig.randomDelay) {
            return Math.max(floor, Math.min(1000, SpearConfig.maxDelayMs));
        }
        int max = Math.max(floor + (floor > 0 ? 30 : 0), SpearConfig.maxDelayMs);
        if (max <= floor) return floor;
        return (int) GaussianTimingEngine.getDelay((floor + max) / 2.0D, 18.0D, floor, max);
    }

    private int getSwitchDelayTicks() {
        return 1;
    }

    private int getRestoreDelayTicks() {
        if (SpearConfig.securityMode == SpearConfig.MODE_RAGE) {
            return 1;
        }
        if (SpearConfig.securityMode == SpearConfig.MODE_SEMI_LEGIT) {
            return Math.max(1, (int) Math.round(targetRestoreDelayMs / 50.0D));
        }
        return Math.max(2, (int) Math.round(targetRestoreDelayMs / 50.0D));
    }

    private int getCooldownTicks() {
        if (SpearConfig.securityMode == SpearConfig.MODE_RAGE) {
            return 0;
        }
        if (SpearConfig.securityMode == SpearConfig.MODE_SEMI_LEGIT) {
            return 1;
        }
        if (SpearConfig.randomDelay) {
            return 2 + (ThreadLocalRandom.current().nextBoolean() ? 1 : 0);
        }
        return 2;
    }

    private void startSpearActive(MinecraftClient client, ClientPlayerEntity player, int slot, boolean doSelect) {
        if (slot < 0 || slot >= 9) {
            clearState();
            return;
        }
        ItemStack targetStack = player.getInventory().getStack(slot);
        if (targetStack.isEmpty() || !isSpear(targetStack)) {
            clearState();
            return;
        }

        lastSwapTimeMs = System.currentTimeMillis();
        swapStartTick = clientTickCount;
        CombatLockManager.setLock("pvp.spear_active", true);

        if (doSelect) {
            selectSlot(client, slot);
        } else {
            SafeSlotManager.setLastSelectedSlot(client.interactionManager, slot);
        }

        executeSpearStrike(client, player);

        state = State.WAITING_RESTORE;
        strikeTick = clientTickCount;
        lastStrikeTimeMs = System.currentTimeMillis();
        targetRestoreDelayMs = calculateDelay();
        restoreDelayTicks = getRestoreDelayTicks();
    }

    private void handleSwappedToSpear(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getInventory().getSelectedSlot() != spearSlot) {
            selectSlot(client, spearSlot);
            return;
        }

        long elapsedTicks = clientTickCount - swapStartTick;
        boolean differentTick = (clientTickCount != lastSlotChangeTick);

        if (elapsedTicks >= targetDelayTicks && differentTick) {
            executeSpearStrike(client, player);

            state = State.WAITING_RESTORE;
            strikeTick = clientTickCount;
            lastStrikeTimeMs = System.currentTimeMillis();
            restoreDelayTicks = getRestoreDelayTicks();
        }
    }

    private void executeSpearStrike(MinecraftClient client, ClientPlayerEntity player) {
        try {
            int curSlot = player.getInventory().getSelectedSlot();
            if (curSlot != spearSlot) {
                return;
            }

            ItemStack stack = player.getInventory().getStack(spearSlot);
            if (stack.isEmpty() || !isSpear(stack)) {
                return;
            }

            boolean isMiss = false;
            if (SpearConfig.missChance > 0 && ThreadLocalRandom.current().nextInt(100) < SpearConfig.missChance) {
                isMiss = true;
            }

            if (isMiss) {
                player.swingHand(Hand.MAIN_HAND);
                player.resetTicksSinceLastAttack();
                return;
            }

            LivingEntity target = null;
            if (targetEntityId >= 0 && client.world != null) {
                Entity entity = client.world.getEntityById(targetEntityId);
                if (entity instanceof LivingEntity living && isLivingTargetValid(player, living)) {
                    target = living;
                }
            }
            if (target == null) {
                target = findTarget(client, stack);
            }

            PiercingWeaponComponent piercing = stack.get(DataComponentTypes.PIERCING_WEAPON);
            if (piercing != null && !client.interactionManager.isFlyingLocked()) {
                client.interactionManager.attackWithPiercingWeapon(piercing);
                player.swingHand(Hand.MAIN_HAND);
            } else {
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(
                            net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STAB,
                            net.minecraft.util.math.BlockPos.ORIGIN,
                            net.minecraft.util.math.Direction.DOWN
                    ));
                }
                player.swingHand(Hand.MAIN_HAND);
                player.resetTicksSinceLastAttack();

                if (target != null) {
                    client.interactionManager.attackEntity(player, target);
                }
            }
            activity.client.module.service.CooldownTrackerService.recordTridentUsed();
        } catch (Throwable ignored) {
        }
    }

    private void handleWaitingRestore(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            clearState();
            return;
        }

        long elapsedMs = System.currentTimeMillis() - lastStrikeTimeMs;
        long elapsedTicks = clientTickCount - strikeTick;
        boolean differentTick = (clientTickCount != lastSlotChangeTick);

        boolean timeExpired = elapsedMs >= targetRestoreDelayMs;
        boolean ticksExpired = elapsedTicks >= restoreDelayTicks;

        if ((timeExpired && ticksExpired && differentTick) || elapsedTicks > 20) {
            restoreSlot(client);
            clearState();
            cooldownTicks = getCooldownTicks();
        }
    }

    private boolean isPlayerBusy(MinecraftClient client, ClientPlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return true;
        }
        return activity.client.module.service.PlayerStateService.isBusy(player);
    }

    private static boolean isFoodOrConsumable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShieldItem || stack.isOf(Items.SHIELD)) return true;
        if (stack.contains(DataComponentTypes.FOOD)) return true;
        if (stack.contains(DataComponentTypes.POTION_CONTENTS)) return true;
        if (stack.contains(DataComponentTypes.CONSUMABLE)) return true;
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) || stack.isOf(Items.POTION) || stack.isOf(Items.MILK_BUCKET)) return true;
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) return true;
        return false;
    }

    private LivingEntity findTarget(MinecraftClient client, ItemStack spearStack) {
        if (client == null || client.player == null || client.world == null) return null;
        ClientPlayerEntity player = client.player;

        LivingEntity living = activity.client.module.service.TargetCacheService.getCrosshairLivingTarget(client);
        if (living != null && isLivingTargetValid(player, living)) {
            return living;
        }

        return findTargetAlongRay(client, player, spearStack);
    }

    private LivingEntity findTargetAlongRay(MinecraftClient client, ClientPlayerEntity player, ItemStack spearStack) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);

        double maxReach = 3.0D;
        try {
            double entityRange = player.getEntityInteractionRange();
            if (entityRange > 0.0D) {
                maxReach = entityRange;
            }
        } catch (Throwable ignored) {}

        if (spearStack != null && !spearStack.isEmpty()) {
            try {
                AttackRangeComponent arc = spearStack.get(DataComponentTypes.ATTACK_RANGE);
                if (arc != null) {
                    maxReach = Math.max(maxReach, (double) arc.maxRange());
                } else if (isSpear(spearStack)) {
                    maxReach = Math.max(maxReach, 4.5D);
                }
            } catch (Throwable ignored) {}
        }

        if (SpearConfig.securityMode == SpearConfig.MODE_RAGE) {
            maxReach += 0.5D;
        }

        Vec3d reachEnd = eyePos.add(lookVec.multiply(maxReach));
        Box searchBox = player.getBoundingBox().expand(maxReach + 1.0D);
        List<Entity> candidates = client.world.getOtherEntities(player, searchBox, e -> e instanceof LivingEntity);

        LivingEntity bestTarget = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity e : candidates) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!isLivingTargetValid(player, living)) continue;

            Box box = living.getBoundingBox().expand(0.1D);
            var hit = box.raycast(eyePos, reachEnd);
            if (hit.isPresent()) {
                Vec3d hitPos = hit.get();
                double distSq = eyePos.squaredDistanceTo(hitPos);
                if (distSq <= maxReach * maxReach && distSq < closestDistSq) {
                    if (hasLineOfSight(player, hitPos)) {
                        closestDistSq = distSq;
                        bestTarget = living;
                    }
                }
            }
        }
        return bestTarget;
    }

    private static boolean isLivingTargetValid(ClientPlayerEntity player, LivingEntity target) {
        if (target == null || target == player) return false;
        if (!target.isAlive() || target.isSpectator() || target.isRemoved()) return false;
        return true;
    }

    private static boolean hasLineOfSight(ClientPlayerEntity player, Vec3d targetPos) {
        if (player == null || player.getEntityWorld() == null || targetPos == null) return false;
        Vec3d eye = player.getEyePos();
        World world = player.getEntityWorld();

        BlockHitResult hit = world.raycast(new RaycastContext(
                eye,
                targetPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return eye.squaredDistanceTo(hit.getPos()) >= eye.squaredDistanceTo(targetPos) - 0.15D;
    }

    private int findWeaponSlot(ClientPlayerEntity player) {
        if (lastNonSpearSlot >= 0 && lastNonSpearSlot < 9 && lastNonSpearSlot != spearSlot) {
            ItemStack stack = player.getInventory().getStack(lastNonSpearSlot);
            if (!stack.isEmpty() && !isSpear(stack) && isSwordOrAxe(stack)) {
                return lastNonSpearSlot;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i == spearSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!isSpear(stack) && isSwordOrAxe(stack)) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i == spearSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!isSpear(stack)) {
                return i;
            }
        }
        if (lastNonSpearSlot >= 0 && lastNonSpearSlot < 9 && lastNonSpearSlot != spearSlot) {
            return lastNonSpearSlot;
        }
        for (int i = 0; i < 9; i++) {
            if (i != spearSlot) return i;
        }
        return 0;
    }

    private static boolean isSwordOrAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            if (stack.isIn(ItemTags.SWORDS)) return true;
        } catch (Throwable ignored) {}
        try {
            if (stack.isIn(ItemTags.AXES)) return true;
        } catch (Throwable ignored) {}
        String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return name.contains("sword") || name.contains("axe") || name.contains("меч") || name.contains("топор");
    }

    private int findSpearSlot(ClientPlayerEntity player, int curSlot) {
        List<Integer> allSpearSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i == curSlot) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 1) continue;
            if (isSpear(stack)) {
                allSpearSlots.add(i);
            }
        }

        if (allSpearSlots.isEmpty()) {
            return -1;
        }

        if (allSpearSlots.size() == 1) {
            return allSpearSlots.get(0);
        }

        int mode = SpearConfig.priorityMode;

        if (mode >= SpearConfig.PRIORITY_LUNGE_1 && mode <= SpearConfig.PRIORITY_LUNGE_3) {
            int targetLunge = mode;
            List<Integer> matching = new ArrayList<>();
            for (int slot : allSpearSlots) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (getLungeLevel(stack) == targetLunge) {
                    matching.add(slot);
                }
            }
            if (!matching.isEmpty()) {
                return findClosestSlot(matching, curSlot);
            }
        } else if (mode == SpearConfig.PRIORITY_RANDOM) {
            int randomIndex = ThreadLocalRandom.current().nextInt(allSpearSlots.size());
            return allSpearSlots.get(randomIndex);
        }

        return findClosestSlot(allSpearSlots, curSlot);
    }

    private int findClosestSlot(List<Integer> slots, int curSlot) {
        int bestSlot = slots.get(0);
        int bestDist = Math.abs(curSlot - bestSlot);
        for (int i = 1; i < slots.size(); i++) {
            int slot = slots.get(i);
            int dist = Math.abs(curSlot - slot);
            if (dist < bestDist) {
                bestDist = dist;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static String getSafeEnchantmentId(net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment> entry) {
        if (entry == null) return "";
        try {
            if (entry.getKey().isPresent()) {
                return entry.getKey().get().getValue().toString().toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        try {
            return entry.getIdAsString().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {}
        return "";
    }

    private static int getLungeLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        try {
            ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (ench == null || ench.isEmpty()) {
                ench = EnchantmentHelper.getEnchantments(stack);
            }
            if (ench != null && !ench.isEmpty()) {
                for (var entry : ench.getEnchantmentEntries()) {
                    int lvl = entry.getIntValue();
                    String id = getSafeEnchantmentId(entry.getKey());
                    if (id.contains("lunge") || id.contains("рывок") || id.contains("выпад")) {
                        return lvl;
                    }
                }
            }
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text t : lore.lines()) {
                    String str = t.getString().toLowerCase(Locale.ROOT);
                    int parsed = parseLungeLevel(str);
                    if (parsed > 0) return parsed;
                }
            }
            try {
                String name = stack.getName().getString().toLowerCase(Locale.ROOT);
                int parsed = parseLungeLevel(name);
                if (parsed > 0) return parsed;
            } catch (Exception ignored) {}
            var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) {
                String nbt = customData.copyNbt().toString().toLowerCase(Locale.ROOT);
                int parsed = parseLungeLevel(nbt);
                if (parsed > 0) return parsed;
                if (nbt.contains("form_id:3") || nbt.contains("form_id: 3")) return 3;
                if (nbt.contains("form_id:2") || nbt.contains("form_id: 2")) return 2;
                if (nbt.contains("form_id:1") || nbt.contains("form_id: 1")) return 1;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private static int parseLungeLevel(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (s.contains("выпад iii") || s.contains("lunge iii") || s.contains("рывок iii")
                || s.contains("выпад 3") || s.contains("lunge 3") || s.contains("рывок 3")
                || s.contains("lunge_3") || s.contains("выпад: 3") || s.contains("выпад:3")
                || s.contains("выпад: iii") || s.contains("выпад:iii")) {
            return 3;
        }
        if (s.contains("выпад ii") || s.contains("lunge ii") || s.contains("рывок ii")
                || s.contains("выпад 2") || s.contains("lunge 2") || s.contains("рывок 2")
                || s.contains("lunge_2") || s.contains("выпад: 2") || s.contains("выпад:2")
                || s.contains("выпад: ii") || s.contains("выпад:ii")) {
            return 2;
        }
        if (s.contains("выпад i") || s.contains("lunge i") || s.contains("рывок i")
                || s.contains("выпад 1") || s.contains("lunge 1") || s.contains("рывок 1")
                || s.contains("lunge_1") || s.contains("выпад: 1") || s.contains("выпад:1")
                || s.contains("выпад: i") || s.contains("выпад:i")) {
            return 1;
        }
        return 0;
    }

    private static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            if (stack.isIn(ItemTags.SPEARS)) return true;
        } catch (Throwable ignored) {}
        try {
            if (stack.contains(DataComponentTypes.PIERCING_WEAPON)) return true;
        } catch (Throwable ignored) {}
        try {
            if (stack.contains(DataComponentTypes.KINETIC_WEAPON)) return true;
        } catch (Throwable ignored) {}
        if (stack.isOf(Items.TRIDENT)) return true;

        String itemStr = "";
        try {
            itemStr = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            itemStr = stack.getItem().toString().toLowerCase(Locale.ROOT);
        }

        if (itemStr.endsWith("_spear") || itemStr.equals("spear") || itemStr.contains("spear") || itemStr.equals("trident")) {
            return true;
        }
        if (getLungeLevel(stack) > 0) {
            return true;
        }
        try {
            String name = stack.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("spear") || name.contains("копь") || name.contains("пика")
                    || name.contains("дротик") || name.contains("трезубец") || name.contains("trident")) {
                return true;
            }
        } catch (Exception ignored) {}
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text t : lore.lines()) {
                String str = t.getString().toLowerCase(Locale.ROOT);
                if (str.contains("spear") || str.contains("копь") || str.contains("пика")
                        || str.contains("дротик") || str.contains("трезубец") || str.contains("trident")
                        || str.contains("рывок") || str.contains("выпад") || str.contains("lunge")
                        || str.contains("пробиван") || str.contains("pierc")) {
                    return true;
                }
            }
        }
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            String nbt = customData.copyNbt().toString().toLowerCase(Locale.ROOT);
            if (nbt.contains("spear") || nbt.contains("lunge") || nbt.contains("копь") || nbt.contains("form_id")) {
                return true;
            }
        }
        return false;
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (client == null || client.player == null || slot < 0 || slot >= 9) return;
        SafeSlotManager.selectSlot(client, slot, clientTickCount);
        lastSlotChangeTick = clientTickCount;
    }

    private void restoreSlot(MinecraftClient client) {
        if (client == null || client.player == null) return;
        int cur = client.player.getInventory().getSelectedSlot();
        int target = originalSlot;
        if (target < 0 || target >= 9
                || target == spearSlot
                || client.player.getInventory().getStack(target).isEmpty()
                || isSpear(client.player.getInventory().getStack(target))) {
            target = findWeaponSlot(client.player);
        }
        if (target >= 0 && target < 9) {
            if (cur != target) {
                SafeSlotManager.selectSlot(client, target, clientTickCount);
                lastSlotChangeTick = clientTickCount;
            }
        }
        lastRestoreTick = clientTickCount;
        lastRestoreTimeMs = System.currentTimeMillis();
    }

    private void clearState() {
        state = State.IDLE;
        spearSlot = -1;
        originalSlot = -1;
        targetEntityId = -1;
        targetRestoreDelayMs = 185;
        CombatLockManager.setLock("pvp.spear_active", false);
        System.clearProperty("pvp.spear_active");
    }

    private void resetAll() {
        clearState();
    }

    private enum State {
        IDLE,
        SWAPPED_TO_SPEAR,
        WAITING_RESTORE
    }
}
