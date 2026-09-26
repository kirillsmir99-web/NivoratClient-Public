package dev.kinetictweaks.controller;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import dev.kinetictweaks.trajectory.PearlCatchTrajectory;
import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class PearlCatchController {
    private static final PearlCatchController INSTANCE = new PearlCatchController();

    public enum Mode {
        VERTICAL,
        HORIZONTAL
    }

    public enum State {
        IDLE,
        ROTATING_TO_PEARL,
        THROW_PEARL,
        WAIT_FOR_WIND,
        SELECT_WIND,
        THROW_WIND,
        POST_THROW_HOLD,
        ROTATING_BACK,
        RESTORE_SLOT
    }

    private State state = State.IDLE;
    private Mode currentMode = Mode.VERTICAL;
    private final CameraInterpolator cameraInterpolator = new CameraInterpolator();

    private int initialSlot = -1;
    private float initialPitch = 0.0f;
    private float initialYaw = 0.0f;
    private float pearlPitch = 0.0f;
    private float targetWindPitch = 0.0f;
    private float targetWindYaw = 0.0f;

    private boolean pearlInOffhand = false;
    private int pearlSlot = -1;
    private boolean windInOffhand = false;
    private int windSlot = -1;

    private int delayTicksRemaining = 0;
    private int activeEffectiveDelay = 2;
    private int holdTicksRemaining = 0;
    private long pearlThrowTimeMs = 0L;
    private long windThrowTimeMs = 0L;
    private long lastTriggerTime = 0L;
    private int stateLifetimeTicks = 0;
    public static final long COOLDOWN_MS = 0L;

    public static PearlCatchController getInstance() {
        return INSTANCE;
    }

    public PearlCatchController() {}

    public CameraInterpolator getCameraInterpolator() {
        return cameraInterpolator;
    }

    public int calculateEffectiveDelay(double rawThrowDelay) {
        return calculateEffectiveDelay(MinecraftClient.getInstance(), rawThrowDelay);
    }

    public int calculateEffectiveDelay(MinecraftClient client, double rawThrowDelay) {
        int baseDelay = Math.max(1, (int) Math.round(rawThrowDelay));
        int pingMs = client != null ? getPlayerLatency(client) : 0;
        if (pingMs > 90) {
            return Math.max(1, baseDelay - 1);
        }
        return baseDelay;
    }

    public void trigger(MinecraftClient client) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        Mode defaultMode = (config != null && "horizontal".equalsIgnoreCase(config.autoPearlCatchDirection))
                ? Mode.HORIZONTAL
                : Mode.VERTICAL;
        trigger(client, defaultMode);
    }

    public void trigger(MinecraftClient client, Mode mode) {
        if (CombatLockManager.isLocked(CombatLockManager.TOTEM)) {
            return;
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null || !config.autoPearlCatchEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        if (state != State.IDLE && (now - lastTriggerTime > 1200L || stateLifetimeTicks > 25)) {
            reset();
        }

        if (CombatLockManager.isLocked(CombatLockManager.PEARL_CATCH) && state == State.IDLE) {
            CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, false);
        }

        boolean isCleanupState = (state == State.POST_THROW_HOLD || state == State.ROTATING_BACK || state == State.RESTORE_SLOT);
        if (state != State.IDLE && !isCleanupState) {
            return;
        }

        if (now - lastTriggerTime < COOLDOWN_MS) {
            return;
        }
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (client.currentScreen != null || !client.player.isAlive()) {
            return;
        }

        if (client.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            return;
        }
        if (client.player.getItemCooldownManager().isCoolingDown(Items.WIND_CHARGE.getDefaultStack())) {
            return;
        }

        pearlInOffhand = isOffhandItem(client.player, Items.ENDER_PEARL);
        pearlSlot = pearlInOffhand ? -1 : findHotbarItem(client.player, Items.ENDER_PEARL);

        windInOffhand = isOffhandItem(client.player, Items.WIND_CHARGE);
        windSlot = windInOffhand ? -1 : findHotbarItem(client.player, Items.WIND_CHARGE);

        if ((!pearlInOffhand && pearlSlot < 0) || (!windInOffhand && windSlot < 0)) {
            return;
        }

        if (mode == Mode.VERTICAL && isCeilingBlocked(client, 10.0)) {

            mode = Mode.HORIZONTAL;
        }

        if (mode == Mode.HORIZONTAL && isForwardBlocked(client, -15.0f, client.player.getYaw(), 2.0)) {

            return;
        }

        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, true);
        lastTriggerTime = now;
        currentMode = mode;

        if (cameraInterpolator.isActive()) {
            cameraInterpolator.reset();
        }

        if (!isCleanupState || initialSlot < 0) {
            initialSlot = client.player.getInventory().getSelectedSlot();
        }
        initialPitch = client.player.getPitch();
        initialYaw = client.player.getYaw();

        holdTicksRemaining = 0;
        pearlThrowTimeMs = 0L;
        windThrowTimeMs = 0L;

        boolean fullAuto = "full_auto".equalsIgnoreCase(config.autoPearlCatchMode);
        boolean legit = config.autoPearlCatchLegitMode;
        int effectiveDelay = calculateEffectiveDelay(client, config.autoPearlCatchThrowDelay);
        this.activeEffectiveDelay = effectiveDelay;
        this.delayTicksRemaining = effectiveDelay;

        long rotDuration = Math.max(50L, (long) config.autoPearlCatchRotationTimeMs);

        if (!pearlInOffhand && pearlSlot >= 0 && pearlSlot < 9 && pearlSlot != initialSlot) {
            SafeSlotManager.selectSlot(client, pearlSlot);
        }

        if (!fullAuto) {
            pearlPitch = initialPitch;
            targetWindPitch = initialPitch;
            targetWindYaw = initialYaw;
            state = State.THROW_PEARL;
        } else {
            if (mode == Mode.VERTICAL) {
                pearlPitch = -89.5f;
                targetWindPitch = -89.5f;
                targetWindYaw = initialYaw;
                cameraInterpolator.start(initialPitch, pearlPitch, initialYaw, initialYaw, rotDuration, legit);
                state = State.ROTATING_TO_PEARL;
            } else {

                PearlCatchTrajectory.Solution sol = PearlCatchTrajectory.solve3D(
                        effectiveDelay,
                        initialYaw,
                        client.player.getVelocity(),
                        client.player.isOnGround(),
                        (float) config.autoPearlCatchHorizontalOffset
                );

                pearlPitch = sol.pearlPitch();
                targetWindPitch = sol.windPitch();
                targetWindYaw = sol.windYaw();

                cameraInterpolator.start(initialPitch, pearlPitch, initialYaw, initialYaw, Math.max(60L, rotDuration - 20L), legit);
                state = State.ROTATING_TO_PEARL;
            }
        }
    }

    public void onRender(MinecraftClient client) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        boolean fullAuto = config != null && "full_auto".equalsIgnoreCase(config.autoPearlCatchMode);
        if (fullAuto && cameraInterpolator.isActive()) {
            cameraInterpolator.onRender(client);
        }
    }

    public void onTick(MinecraftClient client) {
        if (state == State.IDLE) {
            stateLifetimeTicks = 0;
            return;
        }

        stateLifetimeTicks++;
        if (stateLifetimeTicks > 30) {
            reset();
            return;
        }

        if (client == null || client.player == null || client.world == null || client.interactionManager == null || !client.player.isAlive()) {
            reset();
            return;
        }
        if (client.currentScreen != null) {
            reset();
            return;
        }

        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null || !config.autoPearlCatchEnabled) {
            reset();
            return;
        }

        boolean fullAuto = "full_auto".equalsIgnoreCase(config.autoPearlCatchMode);
        boolean legit = config.autoPearlCatchLegitMode;

        if (state == State.ROTATING_TO_PEARL) {
            if (!cameraInterpolator.isActive() || stateLifetimeTicks >= 10) {
                if (cameraInterpolator.isActive()) {
                    cameraInterpolator.finalizeInterpolation(client, pearlPitch, initialYaw);
                }
                state = State.THROW_PEARL;
            }
            return;
        }

        if (state == State.THROW_PEARL) {
            if (!pearlInOffhand && pearlSlot >= 0 && pearlSlot < 9) {
                int cur = client.player.getInventory().getSelectedSlot();
                if (cur != pearlSlot) {
                    SafeSlotManager.selectSlot(client, pearlSlot);
                }
            }
            pearlThrowTimeMs = System.currentTimeMillis();

            Hand hand = pearlInOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            ActionResult res = client.interactionManager.interactItem(client.player, hand);
            swingIfNeeded(client.player, hand, res);

            if (!res.isAccepted()) {
                holdTicksRemaining = 2;
                state = State.POST_THROW_HOLD;
                return;
            }

            if (fullAuto && currentMode == Mode.HORIZONTAL) {
                long dipDuration = Math.max(40L, activeEffectiveDelay * 50L - 10L);
                cameraInterpolator.start(client.player.getPitch(), targetWindPitch, client.player.getYaw(), targetWindYaw, dipDuration, legit);
            }

            delayTicksRemaining = activeEffectiveDelay;

            if (activeEffectiveDelay == 1 && !windInOffhand && windSlot >= 0 && windSlot < 9) {
                int cur = client.player.getInventory().getSelectedSlot();
                if (cur != windSlot) {
                    SafeSlotManager.selectSlot(client, windSlot);
                }
            }

            state = State.WAIT_FOR_WIND;
            return;
        }

        if (state == State.WAIT_FOR_WIND) {
            delayTicksRemaining--;

            if (delayTicksRemaining <= 1 && !windInOffhand && windSlot >= 0 && windSlot < 9) {
                int cur = client.player.getInventory().getSelectedSlot();
                if (cur != windSlot) {
                    SafeSlotManager.selectSlot(client, windSlot);
                }
            }
            if (delayTicksRemaining <= 0) {
                state = State.THROW_WIND;
            } else {
                return;
            }
        }

        if (state == State.THROW_WIND) {

            if (fullAuto && currentMode == Mode.HORIZONTAL) {
                cameraInterpolator.finalizeInterpolation(client, targetWindPitch, targetWindYaw);
            }

            if (!windInOffhand && windSlot >= 0 && windSlot < 9) {
                int cur = client.player.getInventory().getSelectedSlot();
                if (cur != windSlot) {
                    SafeSlotManager.selectSlot(client, windSlot);
                }
            }

            Hand hand = windInOffhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            ActionResult res = client.interactionManager.interactItem(client.player, hand);
            swingIfNeeded(client.player, hand, res);

            holdTicksRemaining = 2;
            windThrowTimeMs = System.currentTimeMillis();
            state = State.POST_THROW_HOLD;
            return;
        }

        if (state == State.POST_THROW_HOLD) {
            holdTicksRemaining--;
            long elapsed = System.currentTimeMillis() - (windThrowTimeMs > 0 ? windThrowTimeMs : pearlThrowTimeMs);
            if (holdTicksRemaining <= 0 && elapsed >= 100L) {
                if (fullAuto && config.autoPearlCatchRestoreCamera) {
                    cameraInterpolator.start(client.player.getPitch(), initialPitch, client.player.getYaw(), initialYaw, 125L, legit);
                    state = State.ROTATING_BACK;
                } else if (config.autoPearlCatchRestoreSlot && initialSlot >= 0 && initialSlot < 9 && initialSlot != client.player.getInventory().getSelectedSlot()) {
                    state = State.RESTORE_SLOT;
                } else {
                    reset();
                }
            }
            return;
        }

        if (state == State.ROTATING_BACK) {
            if (!cameraInterpolator.isActive() || stateLifetimeTicks >= 25) {
                if (cameraInterpolator.isActive()) {
                    cameraInterpolator.finalizeInterpolation(client, initialPitch, initialYaw);
                }
                if (config.autoPearlCatchRestoreSlot && initialSlot >= 0 && initialSlot < 9 && initialSlot != client.player.getInventory().getSelectedSlot()) {
                    state = State.RESTORE_SLOT;
                } else {
                    reset();
                }
            }
            return;
        }

        if (state == State.RESTORE_SLOT) {
            if (initialSlot >= 0 && initialSlot < 9 && initialSlot != client.player.getInventory().getSelectedSlot()) {
                SafeSlotManager.selectSlot(client, initialSlot);
            }
            reset();
        }
    }

    private static void swingIfNeeded(ClientPlayerEntity player, Hand hand, ActionResult result) {
        if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
            player.swingHand(hand);
        }
    }

    public static boolean isCeilingBlocked(MinecraftClient client, double maxDistance) {
        if (client == null || client.player == null || client.world == null) return false;
        Vec3d start = client.player.getEyePos();
        Vec3d end = start.add(0.0, maxDistance, 0.0);
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
        return hit != null && hit.getType() == HitResult.Type.BLOCK;
    }

    public static boolean isForwardBlocked(MinecraftClient client, float pitch, float yaw, double distance) {
        if (client == null || client.player == null || client.world == null) return false;
        Vec3d start = client.player.getEyePos();
        Vec3d dir = PearlCatchTrajectory.getDirectionVector(pitch, yaw);
        Vec3d end = start.add(dir.multiply(distance));
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
        return hit != null && hit.getType() == HitResult.Type.BLOCK;
    }

    public static int getPlayerLatency(MinecraftClient client) {
        if (client == null || client.player == null) return 0;
        try {
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            if (handler == null) return 0;
            PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
            return entry != null ? Math.max(0, entry.getLatency()) : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int findHotbarItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isOffhandItem(ClientPlayerEntity player, Item targetItem) {
        if (player == null || targetItem == null) return false;
        ItemStack offhand = player.getOffHandStack();
        return !offhand.isEmpty() && offhand.isOf(targetItem);
    }

    public State getState() {
        return state;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public float getPearlPitch() {
        return pearlPitch;
    }

    public float getTargetWindPitch() {
        return targetWindPitch;
    }

    public float getTargetWindYaw() {
        return targetWindYaw;
    }

    public int getActiveEffectiveDelay() {
        return activeEffectiveDelay;
    }

    public int getInitialSlot() {
        return initialSlot;
    }

    public boolean canInterruptCurrentState() {
        return state == State.IDLE || state == State.POST_THROW_HOLD || state == State.ROTATING_BACK || state == State.RESTORE_SLOT;
    }

    public void setStateForTest(State state, int initialSlot) {
        this.state = state;
        this.initialSlot = initialSlot;
    }

    public void startThrowForTest(int currentSelectedSlot, boolean isCleanup) {
        if (cameraInterpolator.isActive()) {
            cameraInterpolator.reset();
        }
        if (!isCleanup || initialSlot < 0) {
            initialSlot = currentSelectedSlot;
        }
        this.state = State.THROW_PEARL;
    }

    public void reset() {
        state = State.IDLE;
        cameraInterpolator.reset();
        initialSlot = -1;
        delayTicksRemaining = 0;
        activeEffectiveDelay = 2;
        holdTicksRemaining = 0;
        pearlThrowTimeMs = 0L;
        windThrowTimeMs = 0L;
        stateLifetimeTicks = 0;
        CombatLockManager.setLock(CombatLockManager.PEARL_CATCH, false);
    }
}
