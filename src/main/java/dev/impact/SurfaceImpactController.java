package dev.impact;

import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SurfaceImpactController {
    private static final long COOLDOWN_MS = 600L;
    private static final Random RNG = new Random();

    public enum DropItemType {
        WATER_BUCKET(Items.WATER_BUCKET, true, true),
        COD_BUCKET(Items.COD_BUCKET, true, true),
        SALMON_BUCKET(Items.SALMON_BUCKET, true, true),
        TROPICAL_FISH_BUCKET(Items.TROPICAL_FISH_BUCKET, true, true),
        PUFFERFISH_BUCKET(Items.PUFFERFISH_BUCKET, true, true),
        AXOLOTL_BUCKET(Items.AXOLOTL_BUCKET, true, true),
        TADPOLE_BUCKET(Items.TADPOLE_BUCKET, true, true),
        HAY_BLOCK(Items.HAY_BLOCK, false, false),
        SLIME_BLOCK(Items.SLIME_BLOCK, false, false),
        COBWEB(Items.COBWEB, false, false),
        POWDER_SNOW_BUCKET(Items.POWDER_SNOW_BUCKET, false, true),
        WIND_CHARGE(Items.WIND_CHARGE, false, false);

        public final Item item;
        public final boolean isWater;
        public final boolean isPickupable;

        DropItemType(Item item, boolean isWater, boolean isPickupable) {
            this.item = item;
            this.isWater = isWater;
            this.isPickupable = isPickupable;
        }

        public static DropItemType fromItem(Item item) {
            for (DropItemType t : values()) {
                if (t.item == item) return t;
            }
            return null;
        }
    }

    private enum State {
        IDLE,
        INV_OPENING,
        INV_SWAPPING,
        PREPARED,
        DROPPED,
        WAITING_PICKUP,
        PICKING_UP,
        SWITCHING_BACK,
        RESTORE_INV_OPENING,
        RESTORE_INV_SWAPPING
    }

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int selectedDropSlot = -1;
    private DropItemType activeDropType = null;
    private DropItemType placedDropType = null;
    private BlockPos placedBlockPos = null;

    private int sourceInvSlot = -1;
    private int targetHotbarIndex = -1;
    private boolean swappedFromInventory = false;

    private long actionTimeMs = 0L;
    private int dropTimer = 0;
    private int pickupRetryCounter = 0;
    private long lastDropTime = 0L;
    private long lastCombatWeaponTime = 0L;
    private long lastPearlTime = 0L;
    private long lastWindChargeUseTime = 0L;
    private static volatile long globalLastPearlTime = 0L;
    private ClientPlayerEntity sessionPlayer;
    private net.minecraft.world.World sessionWorld;
    private boolean cleanupPending;
    private boolean openedInventoryScreen;

    public static void recordPearlThrown() {
        globalLastPearlTime = System.currentTimeMillis();
    }

    public static long getGlobalLastPearlTime() {
        return globalLastPearlTime;
    }

    public void cleanup() { if (cleanupPending) reset(); }

    private void openInventoryScreen(MinecraftClient client, ClientPlayerEntity player) {
        if (client != null && player != null && client.currentScreen == null) {
            try {
                client.setScreen(new InventoryScreen(player));
                openedInventoryScreen = true;
            } catch (Throwable ignored) {}
        }
    }

    private void closeInventoryScreen(MinecraftClient client) {
        if (!openedInventoryScreen) {
            return;
        }
        openedInventoryScreen = false;
        if (client != null) {
            if (client.currentScreen instanceof InventoryScreen) {
                try {
                    client.currentScreen.close();
                } catch (Throwable ignored) {}
            }
            if (client.player != null) {
                try {
                    client.player.closeHandledScreen();
                } catch (Throwable ignored) {}
            }
            try {
                client.setScreen(null);
            } catch (Throwable ignored) {}
        }
    }

    public boolean ownsInventoryScreen(MinecraftClient client) {
        return openedInventoryScreen && state != State.IDLE && client != null && client.currentScreen instanceof InventoryScreen;
    }

    public boolean isBusy() {
        return state != State.IDLE || swappedFromInventory || cleanupPending;
    }

    public State getState() {
        return state;
    }

    public boolean isSwappedFromInventory() {
        return swappedFromInventory;
    }

    public void reset() {
        if (state == State.IDLE && !swappedFromInventory && !cleanupPending && !openedInventoryScreen && originalSlot < 0 && selectedDropSlot < 0) {
            sessionPlayer = null;
            sessionWorld = null;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean sameSession = client != null && client.player != null
                && client.player == sessionPlayer && client.world == sessionWorld;
        if (sameSession && openedInventoryScreen && client.currentScreen instanceof InventoryScreen) {
            closeInventoryScreen(client);
        }
        if (swappedFromInventory && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
            if (sameSession && client.player.currentScreenHandler != client.player.playerScreenHandler) {
                cleanupPending = true;
                return;
            }
            if (sameSession && client.interactionManager != null) {
                try {
                    openInventoryScreen(client, client.player);
                    client.interactionManager.clickSlot(
                            client.player.playerScreenHandler.syncId,
                            sourceInvSlot,
                            targetHotbarIndex,
                            SlotActionType.SWAP,
                            client.player
                    );
                    closeInventoryScreen(client);
                } catch (Throwable ignored) {}
            }
        }
        if (sameSession && originalSlot >= 0 && client.player.getInventory().getSelectedSlot() == selectedDropSlot) {
            net.fabricmc.pack.api.SafeSlotManager.restoreSlot(client, originalSlot);
        }
        cleanupPending = false;
        sessionPlayer = null;
        sessionWorld = null;
        state = State.IDLE;
        originalSlot = -1;
        selectedDropSlot = -1;
        activeDropType = null;
        placedDropType = null;
        placedBlockPos = null;
        sourceInvSlot = -1;
        targetHotbarIndex = -1;
        swappedFromInventory = false;
        openedInventoryScreen = false;
        actionTimeMs = 0L;
        dropTimer = 0;
        pickupRetryCounter = 0;
    }

    private static long getRandomMs(double mean, double stdDev, long min, long max) {
        if (!SurfaceImpactConfig.randomDelay) {
            return Math.max(min, Math.min(max, Math.round(mean)));
        }
        long delay = Math.round(mean + RNG.nextGaussian() * stdDev);
        return Math.max(min, Math.min(max, delay));
    }

    private static double getGaussianJitter(double stdDev, double min, double max) {
        if (!SurfaceImpactConfig.randomDelay) {
            return 0.0;
        }
        double val = RNG.nextGaussian() * stdDev;
        return Math.max(min, Math.min(max, val));
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            reset();
            return;
        }

        ClientPlayerEntity player = client.player;
        if (cleanupPending || (sessionPlayer != null && (sessionPlayer != player || sessionWorld != client.world))) {
            reset();
            return;
        }
        sessionPlayer = player;
        sessionWorld = client.world;
        if (!SurfaceImpactConfig.enabled || !player.isAlive()) {
            reset();
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            reset();
            return;
        }

        if (player.getAbilities().flying || player.isGliding()) {
            reset();
            return;
        }

        if (player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            reset();
            return;
        }

        if (player.isClimbing() || player.isInLava() || player.hasVehicle()) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();

        if (player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) {
            lastPearlTime = now;
        }

        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();

        if (main.isOf(Items.ENDER_PEARL) || off.isOf(Items.ENDER_PEARL)) {
            if (client.options != null && client.options.useKey.isPressed()) {
                lastPearlTime = now;
            }
        }

        if (main.isOf(Items.MACE) || off.isOf(Items.MACE) || main.isOf(Items.TRIDENT) || off.isOf(Items.TRIDENT)) {
            lastCombatWeaponTime = now;
        }

        if (player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.WIND_CHARGE))) {
            lastWindChargeUseTime = now;
        }

        if (main.isOf(Items.WIND_CHARGE) || off.isOf(Items.WIND_CHARGE)) {
            if (client.options != null && client.options.useKey.isPressed()) {
                lastWindChargeUseTime = now;
            }
        }

        try {
            List<AbstractWindChargeEntity> charges = client.world.getEntitiesByClass(
                    AbstractWindChargeEntity.class,
                    player.getBoundingBox().expand(8.0),
                    p -> p.getOwner() == player || p.getOwner() == null
            );
            if (!charges.isEmpty()) {
                lastWindChargeUseTime = now;
            }
        } catch (Throwable ignored) {}

        try {
            List<EnderPearlEntity> pearls = client.world.getEntitiesByClass(
                    EnderPearlEntity.class,
                    player.getBoundingBox().expand(64.0),
                    p -> p.getOwner() == player || p.getOwner() == null
            );
            if (!pearls.isEmpty()) {
                lastPearlTime = now;
            }
        } catch (Throwable ignored) {}

        if (state == State.IDLE && player.isTouchingWater()) {
            return;
        }

        switch (state) {
            case IDLE -> processIdle(client, player);
            case INV_OPENING -> processInvOpening(client, player);
            case INV_SWAPPING -> processInvSwapping(client, player);
            case PREPARED -> processPrepared(client, player);
            case DROPPED -> processDropped(client, player);
            case WAITING_PICKUP -> processWaitingPickup(client, player);
            case PICKING_UP -> processPickingUp(client, player);
            case SWITCHING_BACK -> processSwitchingBack(client, player);
            case RESTORE_INV_OPENING -> processRestoreInvOpening(client, player);
            case RESTORE_INV_SWAPPING -> processRestoreInvSwapping(client, player);
        }
    }

    private boolean isExcluded(ClientPlayerEntity player) {
        long now = System.currentTimeMillis();

        if (now - globalLastPearlTime < 2500L) {
            return true;
        }

        if (net.fabricmc.pack.api.CombatLockManager.isLocked()) {
            return true;
        }

        if (dev.pearl.ClickPearlController.getInstance().getState() != dev.pearl.ClickPearlController.State.IDLE) {
            return true;
        }

        if (now - lastWindChargeUseTime < 2000L) {
            return true;
        }

        if (SurfaceImpactConfig.combatGuard) {
            if (mainHoldingWeapon(player) && (now - lastCombatWeaponTime < 1500L)) {
                return true;
            }

            if (player.isUsingItem()) {
                ItemStack active = player.getActiveItem();
                if (active.isOf(Items.TRIDENT)) {
                    return true;
                }
            }
        }

        if (SurfaceImpactConfig.pearlGuard) {
            if (now - lastPearlTime < 3500L) {
                return true;
            }

            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();
            if (main.isOf(Items.ENDER_PEARL) || off.isOf(Items.ENDER_PEARL)) {
                return true;
            }
        }

        return false;
    }

    private boolean mainHoldingWeapon(ClientPlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        return main.isOf(Items.MACE) || main.isOf(Items.TRIDENT);
    }

    private boolean isItemEnabled(DropItemType type) {
        if (type == null) return false;
        if (type.isWater) return SurfaceImpactConfig.enableWater;
        return switch (type) {
            case WIND_CHARGE -> SurfaceImpactConfig.enableWindCharge;
            case HAY_BLOCK -> SurfaceImpactConfig.enableHayBlock;
            case SLIME_BLOCK -> SurfaceImpactConfig.enableSlimeBlock;
            case COBWEB -> SurfaceImpactConfig.enableCobweb;
            case POWDER_SNOW_BUCKET -> SurfaceImpactConfig.enablePowderSnow;
            default -> false;
        };
    }

    private boolean isSoftLandingBlock(World world, BlockPos groundPos, ClientPlayerEntity player) {
        if (world == null) return false;
        if (groundPos != null) {
            BlockState bs = world.getBlockState(groundPos);
            FluidState fs = world.getFluidState(groundPos);
            if (isSoftMaterial(bs, fs)) {
                return true;
            }
            BlockPos up = groundPos.up();
            BlockState upBs = world.getBlockState(up);
            FluidState upFs = world.getFluidState(up);
            if (isSoftMaterial(upBs, upFs)) {
                return true;
            }
        }
        if (player != null) {
            int px = (int) Math.floor(player.getX());
            int pz = (int) Math.floor(player.getZ());
            int startY = (int) Math.floor(player.getY());
            int endY = groundPos != null ? Math.max(groundPos.getY() - 1, startY - 14) : Math.max(0, startY - 14);
            for (int y = startY; y >= endY; y--) {
                BlockPos p = new BlockPos(px, y, pz);
                BlockState bs = world.getBlockState(p);
                FluidState fs = world.getFluidState(p);
                if (isSoftMaterial(bs, fs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSoftMaterial(BlockState bs, FluidState fs) {
        if (fs != null && (fs.isOf(Fluids.WATER) || fs.isOf(Fluids.FLOWING_WATER))) {
            return true;
        }
        if (bs == null) return false;
        return bs.isOf(Blocks.SLIME_BLOCK)
                || bs.isOf(Blocks.COBWEB)
                || bs.isOf(Blocks.POWDER_SNOW)
                || bs.isOf(Blocks.WATER)
                || bs.isOf(Blocks.HAY_BLOCK)
                || bs.isOf(Blocks.SWEET_BERRY_BUSH)
                || bs.isOf(Blocks.HONEY_BLOCK);
    }

    private void processIdle(MinecraftClient client, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now - lastDropTime < COOLDOWN_MS) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        if (player.isOnGround() || player.getVelocity().y >= -0.35) {
            return;
        }

        if (isExcluded(player)) {
            return;
        }

        String camMode = SurfaceImpactConfig.cameraMode;
        boolean autoCam = "auto".equalsIgnoreCase(camMode);
        boolean packetCam = "packet".equalsIgnoreCase(camMode);

        float pitch = player.getPitch();
        if (!autoCam && !packetCam && pitch < SurfaceImpactConfig.pitchThreshold) {
            return;
        }

        BlockHitResult groundHit = getGroundHitResult(client, player, SurfaceImpactConfig.mode == 1 ? 48.0 : 24.0);
        if (groundHit == null || groundHit.getSide() != Direction.UP || groundHit.getBlockPos().getY() >= player.getY()) {
            return;
        }

        if (isSoftLandingBlock(client.world, groundHit.getBlockPos(), player)) {
            return;
        }

        double landingSurfaceY = groundHit.getBlockPos().getY() + 1.0;
        double distToGround = player.getY() - landingSurfaceY;
        if (distToGround < 0.2) {
            return;
        }

        double estimatedTotalFall = player.fallDistance + distToGround;
        if (estimatedTotalFall < Math.max(3.5, (double) SurfaceImpactConfig.fallThreshold)) {
            return;
        }

        boolean inNether = SurfaceImpactConfig.netherAdapter && isNether(client.world);
        DropItemType[] priorities = getPriorities(inNether);

        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack heldStack = player.getMainHandStack();
        DropItemType heldType = DropItemType.fromItem(heldStack.getItem());
        if (heldType != null && isItemEnabled(heldType) && (!inNether || !heldType.isWater)) {
            if (originalSlot < 0) {
                originalSlot = currentSlot;
            }
            selectedDropSlot = currentSlot;
            activeDropType = heldType;
            swappedFromInventory = false;
            state = State.PREPARED;
            return;
        }

        double maxPrepDist = (priorities.length > 0 && priorities[0] == DropItemType.WIND_CHARGE) ? 8.5 : 6.0;
        int directHotbarSlot = findDropItemInHotbar(player, priorities);
        if (directHotbarSlot >= 0) {
            if (distToGround > maxPrepDist) {
                return;
            }
            ItemStack stack = player.getInventory().getStack(directHotbarSlot);
            DropItemType type = DropItemType.fromItem(stack.getItem());
            if (type != null && isItemEnabled(type)) {
                if (originalSlot < 0) {
                    originalSlot = currentSlot;
                }
                selectedDropSlot = directHotbarSlot;
                activeDropType = type;
                swappedFromInventory = false;
                SafeSlotManager.selectSlot(client, selectedDropSlot);
                state = State.PREPARED;
                actionTimeMs = now + getRandomMs(50, 10, 35, 75);
                return;
            }
        }

        if (SurfaceImpactConfig.mode == 1) {
            if (distToGround < 4.0) {
                return;
            }

            DropItemType selectedType = null;
            int selectedInvSlot = -1;
            for (DropItemType type : priorities) {
                if (!isItemEnabled(type)) continue;
                int invSlot = findSpecificItemInInventory(player, type.item);
                if (invSlot >= 0) {
                    selectedType = type;
                    selectedInvSlot = invSlot;
                    break;
                }
            }

            if (selectedType == null || selectedInvSlot < 0) {
                return;
            }

            int currentSelected = player.getInventory().getSelectedSlot();
            if (originalSlot < 0) {
                originalSlot = currentSelected;
            }
            sourceInvSlot = selectedInvSlot;
            targetHotbarIndex = currentSelected;
            activeDropType = selectedType;
            selectedDropSlot = currentSelected;
            swappedFromInventory = true;

            openInventoryScreen(client, player);
            state = State.INV_OPENING;
            actionTimeMs = now + getRandomMs(45, 10, 30, 70);
        }
    }

    private void processInvOpening(MinecraftClient client, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        double distToGround = getQuickDistToGround(client, player);
        boolean emergency = distToGround <= 2.8 || player.isOnGround();
        if (emergency || now >= actionTimeMs) {
            if (client.interactionManager != null && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
                try {
                    client.interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            sourceInvSlot,
                            targetHotbarIndex,
                            SlotActionType.SWAP,
                            player
                    );
                } catch (Throwable ignored) {}
            }
            SafeSlotManager.selectSlot(client, targetHotbarIndex);
            if (emergency) {
                closeInventoryScreen(client);
                state = State.PREPARED;
                actionTimeMs = now;
            } else {
                state = State.INV_SWAPPING;
                actionTimeMs = now + getRandomMs(40, 10, 25, 60);
            }
        }
    }

    private void processInvSwapping(MinecraftClient client, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        double distToGround = getQuickDistToGround(client, player);
        boolean emergency = distToGround <= 2.8 || player.isOnGround();
        if (emergency || now >= actionTimeMs) {
            closeInventoryScreen(client);
            state = State.PREPARED;
            actionTimeMs = now + getRandomMs(20, 5, 10, 40);
        }
    }

    private void processPrepared(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isOnGround() || player.getVelocity().y >= 0.2) {
            startSwitchBack();
            return;
        }

        if (selectedDropSlot < 0 || selectedDropSlot >= 9 || activeDropType == null) {
            reset();
            return;
        }

        if (player.getInventory().getSelectedSlot() != selectedDropSlot) {
            SafeSlotManager.selectSlot(client, selectedDropSlot);
            return;
        }

        ItemStack currentStack = player.getInventory().getStack(selectedDropSlot);
        if (!currentStack.isOf(activeDropType.item) && !player.getMainHandStack().isOf(activeDropType.item)) {
            reset();
            return;
        }

        double maxReach = Math.min(player.getBlockInteractionRange(), 4.2);
        BlockHitResult bhr = getGroundHitResult(client, player, maxReach);
        if (bhr == null || bhr.getSide() != Direction.UP) {
            return;
        }

        BlockPos hitPos = bhr.getBlockPos();
        if (hitPos.getY() >= player.getY()) {
            return;
        }

        if (isSoftLandingBlock(client.world, hitPos, player)) {
            startSwitchBack();
            return;
        }

        double dx = player.getX() - (hitPos.getX() + 0.5);
        double dz = player.getZ() - (hitPos.getZ() + 0.5);
        if (dx * dx + dz * dz > 25.0) {
            return;
        }

        double landingSurfaceY = hitPos.getY() + 1.0;
        double feetDist = player.getY() - landingSurfaceY;
        if (feetDist < 0.0) {
            return;
        }

        if ("auto".equalsIgnoreCase(SurfaceImpactConfig.cameraMode) && feetDist <= 4.5) {
            player.setPitch(Math.min(90.0F, player.getPitch() + (90.0F - player.getPitch()) * 0.75F));
        }

        if (activeDropType == DropItemType.WIND_CHARGE) {
            double speed = Math.abs(player.getVelocity().y);
            double heightJitter = getGaussianJitter(0.15, -0.25, 0.25);
            double triggerHeight = Math.max(3.8, Math.min(9.0, speed * 2.2 + 1.2 + heightJitter));
            if (feetDist <= triggerHeight) {
                executeWindChargeDrop(client, player, hitPos);
            }
            return;
        }

        double vy = player.getVelocity().y;
        double nextFeetDist = feetDist + vy;

        double heightJitter = getGaussianJitter(0.15, -0.25, 0.25);
        double triggerHeight = Math.max(1.7, Math.min(2.5, 2.1 + heightJitter));

        boolean shouldTrigger = (nextFeetDist <= 1.1) || (feetDist <= triggerHeight);
        if (!shouldTrigger) {
            return;
        }

        executeDrop(client, player, bhr, hitPos, feetDist);
    }

    private static void swingIfNeeded(ClientPlayerEntity player, Hand hand, ActionResult result) {
        if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
            player.swingHand(hand);
        }
    }

    private void executeWindChargeDrop(MinecraftClient client, ClientPlayerEntity player, BlockPos hitPos) {
        if ("packet".equalsIgnoreCase(SurfaceImpactConfig.cameraMode) && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), 90.0F, player.isOnGround(), false));
        }
        ActionResult res = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        if (res.isAccepted()) {
            swingIfNeeded(player, Hand.MAIN_HAND, res);
        }

        lastWindChargeUseTime = System.currentTimeMillis();
        placedBlockPos = hitPos.up();
        placedDropType = DropItemType.WIND_CHARGE;
        lastDropTime = System.currentTimeMillis();
        dropTimer = 0;
        state = State.DROPPED;
    }

    private BlockHitResult getGroundHitResult(MinecraftClient client, ClientPlayerEntity player, double maxDistance) {
        if (client.crosshairTarget instanceof BlockHitResult target && target.getType() == HitResult.Type.BLOCK) {
            if (target.getSide() == Direction.UP && target.getBlockPos().getY() < player.getY()) {
                double dist = player.getEyePos().distanceTo(target.getPos());
                if (dist <= maxDistance) {
                    return target;
                }
            }
        }

        HitResult rayResult = player.raycast(maxDistance, 1.0F, false);
        if (rayResult instanceof BlockHitResult rayBhr && rayBhr.getType() == HitResult.Type.BLOCK) {
            if (rayBhr.getSide() == Direction.UP && rayBhr.getBlockPos().getY() < player.getY()) {
                return rayBhr;
            }
        }

        Vec3d eye = player.getEyePos();
        Vec3d down = eye.add(0.0, -maxDistance, 0.0);
        BlockHitResult downHit = client.world.raycast(new RaycastContext(
                eye, down, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
        ));
        if (downHit != null && downHit.getType() == HitResult.Type.BLOCK && downHit.getSide() == Direction.UP && downHit.getBlockPos().getY() < player.getY()) {
            return downHit;
        }

        return null;
    }

    private void executeDrop(MinecraftClient client, ClientPlayerEntity player, BlockHitResult bhr, BlockPos hitPos, double feetDist) {
        if ("packet".equalsIgnoreCase(SurfaceImpactConfig.cameraMode) && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(), 90.0F, player.isOnGround(), false));
        }

        boolean placed = false;

        if (activeDropType != null && (activeDropType.isWater || activeDropType == DropItemType.POWDER_SNOW_BUCKET)) {
            ActionResult itemResult = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            if (itemResult.isAccepted()) {
                placed = true;
                swingIfNeeded(player, Hand.MAIN_HAND, itemResult);
            }
        } else {
            ActionResult blockResult = client.interactionManager.interactBlock(player, Hand.MAIN_HAND, bhr);
            if (blockResult.isAccepted()) {
                placed = true;
                swingIfNeeded(player, Hand.MAIN_HAND, blockResult);
            }
        }

        ItemStack held = player.getMainHandStack();
        if (placed || held.isOf(Items.BUCKET) || !held.isOf(activeDropType.item) || feetDist <= 0.8) {
            placedBlockPos = hitPos.up();
            placedDropType = activeDropType;
            lastDropTime = System.currentTimeMillis();
            dropTimer = 0;
            state = State.DROPPED;
        }
    }

    private void processDropped(MinecraftClient client, ClientPlayerEntity player) {
        dropTimer++;

        if (placedDropType == DropItemType.WIND_CHARGE) {
            if (dropTimer >= 3 && (player.getVelocity().y > 0.05 || player.isOnGround() || dropTimer > 25)) {
                startSwitchBack();
            }
            return;
        }

        boolean hasLanded = player.isOnGround() || player.isTouchingWater()
                || player.isSubmergedInWater() || player.isPartlyTouchingWater()
                || (placedBlockPos != null && player.getY() <= placedBlockPos.getY() + 0.35);

        if (!hasLanded) {
            if (dropTimer > 40) {
                startSwitchBack();
            }
            return;
        }

        dropTimer = 0;

        if (placedDropType == null || !placedDropType.isPickupable || !SurfaceImpactConfig.pickupWater) {
            startSwitchBack();
            return;
        }

        actionTimeMs = System.currentTimeMillis() + getRandomMs(SurfaceImpactConfig.pickupDelayMs, 15.0, 30L, 300L);
        state = State.WAITING_PICKUP;
    }

    private void processWaitingPickup(MinecraftClient client, ClientPlayerEntity player) {
        if (placedBlockPos != null && player.squaredDistanceTo(placedBlockPos.getX() + 0.5, placedBlockPos.getY(), placedBlockPos.getZ() + 0.5) > 64.0) {
            startSwitchBack();
            return;
        }

        if (System.currentTimeMillis() < actionTimeMs) {
            return;
        }

        pickupRetryCounter = 8;
        state = State.PICKING_UP;
        actionTimeMs = 0L;
    }

    private boolean isWaterSource(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        FluidState fs = world.getFluidState(pos);
        if (fs.isOf(Fluids.WATER) && fs.isStill()) {
            return true;
        }
        BlockState bs = world.getBlockState(pos);
        return bs.isOf(Blocks.WATER) && fs.isStill();
    }

    private void processPickingUp(MinecraftClient client, ClientPlayerEntity player) {
        if (System.currentTimeMillis() < actionTimeMs) {
            return;
        }

        if (client.world == null || placedBlockPos == null) {
            startSwitchBack();
            return;
        }

        ItemStack held = player.getMainHandStack();
        if ((placedDropType != null && placedDropType.isWater && held.isOf(Items.WATER_BUCKET))
                || (placedDropType == DropItemType.POWDER_SNOW_BUCKET && held.isOf(Items.POWDER_SNOW_BUCKET))) {
            startSwitchBack();
            return;
        }

        if (!held.isOf(Items.BUCKET)) {
            if (selectedDropSlot >= 0 && selectedDropSlot < 9 && player.getInventory().getStack(selectedDropSlot).isOf(Items.BUCKET)) {
                SafeSlotManager.selectSlot(client, selectedDropSlot);
            } else {
                int bucketSlot = findEmptyBucketInHotbar(player);
                if (bucketSlot >= 0) {
                    SafeSlotManager.selectSlot(client, bucketSlot);
                }
            }
        }

        ItemStack curHeld = player.getMainHandStack();
        if (!curHeld.isOf(Items.BUCKET)) {
            if (curHeld.isOf(Items.WATER_BUCKET) || curHeld.isOf(Items.POWDER_SNOW_BUCKET)) {
                startSwitchBack();
                return;
            }
            pickupRetryCounter--;
            actionTimeMs = System.currentTimeMillis() + 60L;
            return;
        }

        double maxReach = Math.min(player.getBlockInteractionRange(), 4.2);
        HitResult hit = player.raycast(maxReach, 1.0F, true);
        boolean picked = false;

        if (hit instanceof BlockHitResult fluidBhr && fluidBhr.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = fluidBhr.getBlockPos();
            if ((placedDropType != null && placedDropType.isWater && isWaterSource(client.world, targetPos))
                    || (placedDropType == DropItemType.POWDER_SNOW_BUCKET && client.world.getBlockState(targetPos).isOf(Blocks.POWDER_SNOW))
                    || targetPos.equals(placedBlockPos)) {
                ActionResult itemResult = client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                if (itemResult.isAccepted()) {
                    picked = true;
                    swingIfNeeded(player, Hand.MAIN_HAND, itemResult);
                }
            }
        }

        ItemStack afterHeld = player.getMainHandStack();
        if (picked || afterHeld.isOf(Items.WATER_BUCKET) || afterHeld.isOf(Items.POWDER_SNOW_BUCKET)
                || !isWaterSource(client.world, placedBlockPos)) {
            startSwitchBack();
            return;
        }

        pickupRetryCounter--;
        if (pickupRetryCounter <= 0) {
            startSwitchBack();
        } else {
            actionTimeMs = System.currentTimeMillis() + getRandomMs(70, 15, 50, 100);
        }
    }

    private void startSwitchBack() {
        if (SurfaceImpactConfig.switchBack && swappedFromInventory && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                openInventoryScreen(client, client.player);
            }
            state = State.RESTORE_INV_OPENING;
            actionTimeMs = System.currentTimeMillis() + getRandomMs(45, 10, 30, 70);
        } else if (SurfaceImpactConfig.switchBack && originalSlot >= 0 && originalSlot < 9) {
            state = State.SWITCHING_BACK;
            actionTimeMs = System.currentTimeMillis() + getRandomMs(SurfaceImpactConfig.switchDelayMs, 20.0, 30L, 300L);
        } else {
            reset();
        }
    }

    private void processRestoreInvOpening(MinecraftClient client, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now < actionTimeMs) return;
        if (client.interactionManager != null && sourceInvSlot >= 9 && targetHotbarIndex >= 0) {
            try {
                client.interactionManager.clickSlot(
                        player.playerScreenHandler.syncId,
                        sourceInvSlot,
                        targetHotbarIndex,
                        SlotActionType.SWAP,
                        player
                );
            } catch (Throwable ignored) {}
        }
        state = State.RESTORE_INV_SWAPPING;
        actionTimeMs = now + getRandomMs(40, 10, 25, 65);
    }

    private void processRestoreInvSwapping(MinecraftClient client, ClientPlayerEntity player) {
        long now = System.currentTimeMillis();
        if (now < actionTimeMs) return;
        closeInventoryScreen(client);
        swappedFromInventory = false;
        if (originalSlot >= 0 && originalSlot < 9) {
            SafeSlotManager.selectSlot(client, originalSlot);
        }
        reset();
    }

    private void processSwitchingBack(MinecraftClient client, ClientPlayerEntity player) {
        if (System.currentTimeMillis() < actionTimeMs) {
            return;
        }
        if (originalSlot >= 0 && originalSlot < 9) {
            SafeSlotManager.selectSlot(client, originalSlot);
        }
        reset();
    }

    private double getQuickDistToGround(MinecraftClient client, ClientPlayerEntity player) {
        if (client == null || client.world == null || player == null) return 999.0;
        BlockHitResult bhr = getGroundHitResult(client, player, 20.0);
        if (bhr != null && bhr.getSide() == Direction.UP && bhr.getBlockPos().getY() < player.getY()) {
            return player.getY() - (bhr.getBlockPos().getY() + 1.0);
        }
        return 999.0;
    }

    private int findDropItemInHotbar(ClientPlayerEntity player, DropItemType[] priorities) {
        for (DropItemType type : priorities) {
            if (!isItemEnabled(type)) continue;
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).isOf(type.item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findSpecificItemInInventory(ClientPlayerEntity player, Item item) {
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyBucketInHotbar(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isOf(Items.BUCKET)) {
                return i;
            }
        }
        return -1;
    }

    private DropItemType[] getPriorities(boolean inNether) {
        List<DropItemType> list = new ArrayList<>();
        if (inNether) {
            addIfEnabled(list, DropItemType.HAY_BLOCK);
            addIfEnabled(list, DropItemType.SLIME_BLOCK);
            addIfEnabled(list, DropItemType.COBWEB);
            addIfEnabled(list, DropItemType.POWDER_SNOW_BUCKET);
            addIfEnabled(list, DropItemType.WIND_CHARGE);
        } else {
            addIfEnabled(list, DropItemType.WATER_BUCKET);
            addIfEnabled(list, DropItemType.COD_BUCKET);
            addIfEnabled(list, DropItemType.SALMON_BUCKET);
            addIfEnabled(list, DropItemType.TROPICAL_FISH_BUCKET);
            addIfEnabled(list, DropItemType.PUFFERFISH_BUCKET);
            addIfEnabled(list, DropItemType.AXOLOTL_BUCKET);
            addIfEnabled(list, DropItemType.TADPOLE_BUCKET);
            addIfEnabled(list, DropItemType.HAY_BLOCK);
            addIfEnabled(list, DropItemType.SLIME_BLOCK);
            addIfEnabled(list, DropItemType.COBWEB);
            addIfEnabled(list, DropItemType.POWDER_SNOW_BUCKET);
            addIfEnabled(list, DropItemType.WIND_CHARGE);
        }
        return list.toArray(new DropItemType[0]);
    }

    private void addIfEnabled(List<DropItemType> list, DropItemType type) {
        if (isItemEnabled(type)) {
            list.add(type);
        }
    }

    private boolean isNether(World world) {
        return world != null && world.getRegistryKey() == World.NETHER;
    }
}
