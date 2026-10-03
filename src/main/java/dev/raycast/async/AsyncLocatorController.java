package dev.raycast.async;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.mixin.pipeline.PipelineInteractionManagerAccessor;
import activity.client.module.keybind.Keybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AsyncLocatorController {
    private static final AsyncLocatorController INSTANCE = new AsyncLocatorController();

    private final MinecraftClient client = MinecraftClient.getInstance();

    private int state = 0;
    private int savedSlot = -1;
    private boolean wasHeld = false;
    private boolean manualTriggered = false;
    private long lastTimeMs;
    private Vec3d pearlStartPos = Vec3d.ZERO;
    private Vec3d pearlInitialVel = Vec3d.ZERO;
    private int pearlThrowTick = -1;
    private int firstAimTick = -1;
    private int lastRotationTick = -1;
    private int silentThrowEndTick = -1;
    private int aimAttempts = 0;
    private int startTick = -1;
    private float pingMs = 50.0F;
    private float silentYaw = 0.0F;
    private float silentPitch = 0.0F;
    private float targetYaw = 0.0F;
    private float targetPitch = 0.0F;
    private boolean thrown = false;

    private AsyncLocatorController() {
    }

    public static AsyncLocatorController getInstance() {
        return INSTANCE;
    }

    public void trigger() {
        manualTriggered = true;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            AsyncSilentRot.stop(this);
            firstAimTick = -1;
            silentThrowEndTick = -1;
            return;
        }

        if (client.currentScreen != null) {
            if (state != 0) {
                reset();
            }
            resetSilentRot();
            return;
        }

        boolean held = isHeld(client) || manualTriggered;
        manualTriggered = false;

        if (state == 0 && held && !wasHeld) {
            start();
        }
        wasHeld = held;

        if (state != 0 && startTick >= 0 && client.player.age - startTick > 60) {
            reset();
        }

        if (silentThrowEndTick >= 0) {
            if (client.player.age < silentThrowEndTick) {
                applySilentRotation();
            } else {
                resetSilentRot();
            }
        }

        switch (state) {
            case 1 -> {
                int slot = findItemSlot(Items.ENDER_PEARL);
                if (slot < 0) {
                    reset();
                    break;
                }
                selectSlot(slot);
                state = 2;
                updateTime();
            }
            case 2 -> {
                if (silentThrowEndTick >= 0 || !hasDelayElapsed(0.0)) break;
                if (client.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    Vec3d vel = client.player.getVelocity();
                    pearlStartPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
                    pearlInitialVel = AsyncMath.getDirection(client.player.getYaw(), client.player.getPitch())
                            .multiply(1.5)
                            .add(vel.x, client.player.isOnGround() ? 0.0 : vel.y, vel.z);
                    pearlThrowTick = client.player.age;
                    state = 4;
                } else {
                    reset();
                }
                updateTime();
            }
            case 3 -> {
                if (!hasDelayElapsed(0.0)) break;
                state = 4;
                updateTime();
            }
            case 4 -> {
                int slot = findItemSlot(Items.WIND_CHARGE);
                if (slot < 0) {
                    reset();
                    break;
                }
                selectSlot(slot);
                state = 5;
                updateTime();
                aimAttempts = 0;
                thrown = false;
                if (computeTargetAngle()) {
                    stepRotation();
                }
            }
            case 5 -> aimAndThrow();
            case 6 -> {
                if (silentThrowEndTick >= 0 && client.player != null && client.player.age < silentThrowEndTick) {
                    break;
                }
                reset();
            }
        }
    }

    private void aimAndThrow() {
        if (thrown) return;
        if (client.player == null || client.interactionManager == null) return;
        if (client.player.getMainHandStack().getItem() != Items.WIND_CHARGE) return;
        if (!hasDelayElapsed(0.0)) return;

        aimAttempts++;
        if (computeTargetAngle()) {
            stepRotation();
            boolean tickPassed = firstAimTick >= 0 && client.player.age > firstAimTick;
            boolean angleClose = Math.abs(MathHelper.wrapDegrees(silentYaw - targetYaw)) <= 4.0F
                    && Math.abs(silentPitch - targetPitch) <= 4.0F;
            if (tickPassed && angleClose) {
                performThrow(true);
            }
            return;
        }

        if (aimAttempts > 20) {
            if (firstAimTick >= 0 && client.player.age > firstAimTick) {
                performThrow(true);
            } else if (firstAimTick < 0) {
                performThrow(false);
            }
        }
    }

    private void performThrow(boolean silent) {
        if (client.player == null || client.interactionManager == null) return;
        if (silent) {
            float realYaw = client.player.getYaw();
            float realPitch = client.player.getPitch();
            client.player.setYaw(silentYaw);
            client.player.setPitch(silentPitch);
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.setYaw(realYaw);
            client.player.setPitch(realPitch);
            silentThrowEndTick = client.player.age + 2;
        } else {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        }
        thrown = true;
        state = 6;
        updateTime();
    }

    private void start() {
        if (client == null || client.player == null) return;
        if (findItemSlot(Items.ENDER_PEARL) < 0 || findItemSlot(Items.WIND_CHARGE) < 0) return;
        savedSlot = client.player.getInventory().getSelectedSlot();
        pearlThrowTick = -1;
        thrown = false;
        startTick = client.player.age;
        state = 1;
        updateTime();
    }

    public void reset() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg != null && cfg.autoPearlCatchRestoreSlot && savedSlot >= 0 && client != null && client.player != null) {
            selectSlot(savedSlot);
        }
        state = 0;
        savedSlot = -1;
        aimAttempts = 0;
        thrown = false;
        pearlThrowTick = -1;
        startTick = -1;
        manualTriggered = false;
    }

    private void resetSilentRot() {
        AsyncSilentRot.stop(this);
        silentYaw = 0.0F;
        silentPitch = 0.0F;
        firstAimTick = -1;
        lastRotationTick = -1;
        silentThrowEndTick = -1;
    }

    private void selectSlot(int slot) {
        if (client == null || client.player == null || client.interactionManager == null) return;
        client.player.getInventory().setSelectedSlot(slot);
        if (client.interactionManager instanceof PipelineInteractionManagerAccessor accessor) {
            accessor.invokeSyncSelectedSlot();
        }
    }

    private int findItemSlot(Item item) {
        if (client == null || client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void updateTime() {
        lastTimeMs = System.nanoTime() / 1_000_000L;
    }

    private boolean hasDelayElapsed(double delayMs) {
        long now = System.nanoTime() / 1_000_000L;
        return (double) (now - lastTimeMs) >= delayMs;
    }

    public boolean isHeldOrActive() {
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg == null || !cfg.autoPearlCatchEnabled || !"semi_auto".equals(cfg.autoPearlCatchMode)) {
            return false;
        }
        return state != 0 || isHeld(client) || manualTriggered;
    }

    public boolean isActive() {
        return state != 0;
    }

    public int getState() {
        return state;
    }

    public boolean isHeld(MinecraftClient client) {
        if (client == null || client.getWindow() == null || client.currentScreen != null) return false;
        ActivityConfig cfg = ActivityConfigManager.getConfig();
        if (cfg == null || !cfg.autoPearlCatchEnabled || !"semi_auto".equals(cfg.autoPearlCatchMode)) return false;
        Keybind kb = cfg.autoPearlCatchThrowKeybind;
        if (kb == null || kb.isUnbound()) {
            kb = cfg.autoPearlCatchAsyncKeybind;
        }
        if (kb == null || kb.isUnbound()) return false;
        Window window = client.getWindow();
        if (window.getHandle() == 0L) return false;
        boolean ctrl = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);
        return kb.matchesWindow(window, ctrl, shift, alt);
    }

    private void stepRotation() {
        if (client == null || client.player == null) return;
        if (lastRotationTick == client.player.age) {
            applySilentRotation();
            return;
        }

        lastRotationTick = client.player.age;
        float gcd = getMouseGcd();
        int maxSteps = Math.max(1, (int) (60.0F / gcd));
        float curYaw = firstAimTick >= 0 ? silentYaw : client.player.getYaw();
        float curPitch = firstAimTick >= 0 ? silentPitch : MathHelper.clamp(client.player.getPitch(), -90.0F, 90.0F);

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - curYaw);
        int stepsYaw = Math.round(deltaYaw / gcd);
        if (stepsYaw > maxSteps) {
            stepsYaw = maxSteps;
        } else if (stepsYaw < -maxSteps) {
            stepsYaw = -maxSteps;
        }

        silentYaw = curYaw + (float) stepsYaw * gcd;

        float deltaPitch = targetPitch - curPitch;
        int stepsPitch = Math.round(deltaPitch / gcd);
        if (stepsPitch > maxSteps) {
            stepsPitch = maxSteps;
        } else if (stepsPitch < -maxSteps) {
            stepsPitch = -maxSteps;
        }

        int maxPitchSteps = (int) Math.floor((90.0F - curPitch) / gcd);
        int minPitchSteps = (int) Math.ceil((-90.0F - curPitch) / gcd);
        if (stepsPitch > maxPitchSteps) {
            stepsPitch = maxPitchSteps;
        }
        if (stepsPitch < minPitchSteps) {
            stepsPitch = minPitchSteps;
        }

        silentPitch = MathHelper.clamp(curPitch + (float) stepsPitch * gcd, -90.0F, 90.0F);
        if (firstAimTick < 0) {
            firstAimTick = client.player.age;
        }

        applySilentRotation();
    }

    private void applySilentRotation() {
        AsyncSilentRot.set(silentYaw, silentPitch, this);
    }

    private float getMouseGcd() {
        if (client == null || client.options == null) return 0.15F;
        double sens = client.options.getMouseSensitivity().getValue();
        double f = sens * 0.6 + 0.2;
        double gcd = f * f * f * 8.0;
        return (float) gcd * 0.15F;
    }

    private double getPingTicks() {
        try {
            ClientPlayNetworkHandler net = (client != null) ? client.getNetworkHandler() : null;
            if (net != null && client.player != null) {
                PlayerListEntry entry = net.getPlayerListEntry(client.player.getUuid());
                if (entry != null && entry.getLatency() > 0) {
                    pingMs = (float) Math.min(entry.getLatency(), 400);
                }
            }
        } catch (Throwable ignored) {
        }
        return (double) pingMs / 50.0;
    }

    private boolean computeTargetAngle() {
        if (client == null || client.world == null || pearlThrowTick < 0 || client.player == null) return false;

        double pingTicks = getPingTicks();
        Vec3d playerVel = client.player.getVelocity();
        double predictedVy = playerVel.y;
        if (!client.player.isGliding()) {
            int intTicks = (int) pingTicks;
            for (int i = 0; i < intTicks; i++) {
                predictedVy = (predictedVy - 0.08) * 0.98;
            }
            double frac = pingTicks - (double) intTicks;
            if (frac > 0.0) {
                predictedVy += ((predictedVy - 0.08) * 0.98 - predictedVy) * frac;
            }
        }

        Vec3d playerExtraVel = new Vec3d(playerVel.x, client.player.isOnGround() ? 0.0 : predictedVy, playerVel.z);
        Vec3d playerPredictedPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ())
                .add(playerVel.x * pingTicks, playerVel.y * pingTicks, playerVel.z * pingTicks);

        double pearlFlightTicks = (double) (client.player.age - pearlThrowTick) + pingTicks;
        if (pearlFlightTicks < 0.0) pearlFlightTicks = 0.0;

        Vec3d currentPearlPos = pearlStartPos;
        Vec3d currentPearlVel = pearlInitialVel;
        int pearlIntTicks = (int) pearlFlightTicks;

        for (int i = 0; i < pearlIntTicks; i++) {
            currentPearlVel = new Vec3d(currentPearlVel.x, currentPearlVel.y - 0.03, currentPearlVel.z).multiply(0.99);
            currentPearlPos = currentPearlPos.add(currentPearlVel);
        }

        double pearlFrac = pearlFlightTicks - (double) pearlIntTicks;
        if (pearlFrac > 0.0) {
            Vec3d nextVel = new Vec3d(currentPearlVel.x, currentPearlVel.y - 0.03, currentPearlVel.z).multiply(0.99);
            currentPearlPos = currentPearlPos.add(nextVel.multiply(pearlFrac));
        }

        double collisionTime = calculateCollisionTime(playerPredictedPos, playerExtraVel, currentPearlPos, currentPearlVel);
        if (collisionTime <= 0.0) {
            return false;
        }

        Vec3d targetPearlPos = simulatePearl(currentPearlPos, currentPearlVel, collisionTime);
        targetPearlPos = new Vec3d(targetPearlPos.x, targetPearlPos.y - 0.2, targetPearlPos.z);
        Vec3d delta = targetPearlPos.subtract(playerPredictedPos).subtract(playerExtraVel.multiply(collisionTime));
        if (delta.lengthSquared() < 1.0E-6) {
            return false;
        }

        AsyncRot rot = AsyncMath.getRotation(delta);
        targetYaw = (float) rot.yaw();
        targetPitch = MathHelper.clamp((float) rot.pitch(), -90.0F, 90.0F);
        return true;
    }

    private double calculateCollisionTime(Vec3d eye, Vec3d extra, Vec3d pos, Vec3d vel) {
        Vec3d curPos = pos;
        Vec3d curVel = vel;
        double prevDist = pos.subtract(eye).length();

        for (int tick = 1; tick <= 100; tick++) {
            curVel = new Vec3d(curVel.x, curVel.y - 0.03, curVel.z).multiply(0.99);
            curPos = curPos.add(curVel);
            double threshold = 1.5 * Math.max(0.0, (double) tick - 1.0);
            double dist = curPos.subtract(eye).subtract(extra.multiply((double) tick)).length() - threshold;
            if (dist <= 0.0) {
                double span = prevDist - dist;
                double frac = span <= 1.0E-9 ? 0.0 : prevDist / span;
                return (double) (tick - 1) + frac;
            }
            prevDist = dist;
        }
        return 0.0;
    }

    private Vec3d simulatePearl(Vec3d pos, Vec3d vel, double t) {
        int floorT = (int) Math.floor(t);
        Vec3d curPos = pos;
        Vec3d curVel = vel;

        for (int i = 0; i < floorT; i++) {
            curVel = new Vec3d(curVel.x, curVel.y - 0.03, curVel.z).multiply(0.99);
            curPos = curPos.add(curVel);
        }

        double frac = t - (double) floorT;
        if (frac > 0.0) {
            curVel = new Vec3d(curVel.x, curVel.y - 0.03, curVel.z).multiply(0.99);
            curPos = curPos.add(curVel.multiply(frac));
        }
        return curPos;
    }
}
