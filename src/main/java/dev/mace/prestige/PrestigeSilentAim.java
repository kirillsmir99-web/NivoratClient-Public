package dev.mace.prestige;

import dev.raycast.async.AsyncSilentRot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PrestigeSilentAim {
    private static final PrestigeSilentAim INSTANCE = new PrestigeSilentAim();

    private boolean active = false;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private int targetEntityId = -1;
    private int lastUpdateTick = -1;

    private PrestigeSilentAim() {
    }

    public static PrestigeSilentAim getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public float getYaw() {
        return currentYaw;
    }

    public float getPitch() {
        return currentPitch;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    private float getGcdStep(MinecraftClient client) {
        if (client == null || client.options == null) return 0.15f;
        double sens = client.options.getMouseSensitivity().getValue();
        double f = sens * 0.6 + 0.2;
        double gcd = f * f * f * 8.0 * 0.15;
        return (float) Math.max(0.001, gcd);
    }

    public void track(Entity target, ClientPlayerEntity player, MinecraftClient client) {
        if (player == null || target == null || !target.isAlive()) {
            stop();
            return;
        }

        targetEntityId = target.getId();
        float gcd = getGcdStep(client);

        Vec3d eyePos = player.getEyePos().add(player.getVelocity());
        Box box = target.getBoundingBox();
        Vec3d targetCenter = box.getCenter();
        if (PrestigeAutoMaceController.getInstance().getConfig().randomJitter) {
            double jx = Math.sin(System.currentTimeMillis() * 0.006) * 0.07;
            double jy = Math.cos(System.currentTimeMillis() * 0.008) * 0.05;
            targetCenter = targetCenter.add(jx, jy, jx * 0.5);
        }

        double dx = targetCenter.x - eyePos.x;
        double dy = targetCenter.y - eyePos.y;
        double dz = targetCenter.z - eyePos.z;
        double distHoriz = Math.sqrt(dx * dx + dz * dz);

        if (distHoriz < 0.0001D) {
            return;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distHoriz)));
        if (Float.isNaN(targetYaw) || Float.isNaN(targetPitch) || Float.isInfinite(targetYaw) || Float.isInfinite(targetPitch)) {
            return;
        }
        targetPitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f);

        float startYaw = active && !Float.isNaN(currentYaw) ? currentYaw : player.getYaw();
        float startPitch = active && !Float.isNaN(currentPitch) ? currentPitch : MathHelper.clamp(player.getPitch(), -90.0f, 90.0f);

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - startYaw);
        float stepYaw = Math.round(deltaYaw / gcd) * gcd;

        float deltaPitch = targetPitch - startPitch;
        int pitchSteps = Math.round(deltaPitch / gcd);
        int maxUp = (int) Math.floor((90.0f - startPitch) / gcd);
        int maxDown = (int) Math.ceil((-90.0f - startPitch) / gcd);
        pitchSteps = Math.max(maxDown, Math.min(maxUp, pitchSteps));

        float nextYaw = MathHelper.wrapDegrees(startYaw + stepYaw);
        float nextPitch = MathHelper.clamp(startPitch + pitchSteps * gcd, -90.0f, 90.0f);

        if (Float.isNaN(nextYaw) || Float.isNaN(nextPitch) || Float.isInfinite(nextYaw) || Float.isInfinite(nextPitch)) {
            return;
        }

        currentYaw = nextYaw;
        currentPitch = nextPitch;
        active = true;

        AsyncSilentRot.set(currentYaw, currentPitch, this);
    }

    public void stop() {
        if (active) {
            active = false;
            targetEntityId = -1;
            AsyncSilentRot.stop(this);
        }
    }
}
