package dev.mace.prestige;

import activity.client.util.Obf;
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

    private float evaluateAimStep(MinecraftClient client) {
        if (client == null || client.options == null) return dev.mace.prestige.internal.MaceDomain.f(1116194464);
        double sens = client.options.getMouseSensitivity().getValue();
        double f = sens * dev.mace.prestige.internal.MaceDomain.d(4863161960105938732L) + dev.mace.prestige.internal.MaceDomain.d(4852565530073009964L);
        double gcd = f * f * f * dev.mace.prestige.internal.MaceDomain.d(4377040591055130412L) * dev.mace.prestige.internal.MaceDomain.d(4854154760065760287L);
        return (float) Math.max(dev.mace.prestige.internal.MaceDomain.d(4865129878033452033L), gcd);
    }

    private boolean isRealLookOnTarget(ClientPlayerEntity player, Entity target) {
        Vec3d eyePos = player.getEyePos();
        Vec3d rotVec = player.getRotationVector(player.getPitch(), player.getYaw());
        double reach = dev.mace.prestige.internal.MaceDomain.d(4364655692079861548L);
        Vec3d reachEnd = eyePos.add(rotVec.x * reach, rotVec.y * reach, rotVec.z * reach);
        Box box = target.getBoundingBox().expand(dev.mace.prestige.internal.MaceDomain.d(4839054730243544758L));
        return box.contains(eyePos) || box.raycast(eyePos, reachEnd).isPresent();
    }

    public void track(Entity target, ClientPlayerEntity player, MinecraftClient client) {
        if (player == null || target == null || !target.isAlive()) {
            decay(player, client);
            return;
        }

        if (player.age == lastUpdateTick) {
            return;
        }
        lastUpdateTick = player.age;
        targetEntityId = target.getId();

        float targetYaw;
        float targetPitch;

        float f90 = dev.mace.prestige.internal.MaceDomain.f(1042964282);
        if (isRealLookOnTarget(player, target)) {
            targetYaw = player.getYaw();
            targetPitch = MathHelper.clamp(player.getPitch(), -f90, f90);
        } else {
            Vec3d eyePos = player.getEyePos().add(player.getVelocity());
            Vec3d targetCenter = target.getBoundingBox().getCenter();
            double dx = targetCenter.x - eyePos.x;
            double dy = targetCenter.y - eyePos.y;
            double dz = targetCenter.z - eyePos.z;
            double distHoriz = Math.sqrt(dx * dx + dz * dz);
            if (distHoriz < 0.0001D) {
                targetYaw = player.getYaw();
                targetPitch = player.getPitch();
            } else {
                targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - dev.mace.prestige.internal.MaceDomain.d(4379996078310592300L));
                targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distHoriz)));
            }
        }

        if (Float.isNaN(targetYaw) || Float.isNaN(targetPitch) || Float.isInfinite(targetYaw) || Float.isInfinite(targetPitch)) {
            return;
        }
        targetPitch = MathHelper.clamp(targetPitch, -f90, f90);

        float gcd = evaluateAimStep(client);
        float startYaw = active ? currentYaw : player.getYaw();
        float startPitch = active ? currentPitch : MathHelper.clamp(player.getPitch(), -f90, f90);

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - startYaw);
        float stepYaw = Math.round(deltaYaw / gcd) * gcd;
        float stepPitch = quantizePitchStep(targetPitch - startPitch, startPitch, gcd, f90);

        currentYaw = MathHelper.wrapDegrees(startYaw + stepYaw);
        currentPitch = MathHelper.clamp(startPitch + stepPitch, -f90, f90);

        active = true;
        AsyncSilentRot.set(currentYaw, currentPitch, this);
    }

    private static float quantizePitchStep(float deltaPitch, float startPitch, float gcd, float f90) {
        int pitchSteps = Math.round(deltaPitch / gcd);
        int maxUp = (int) Math.floor((f90 - startPitch) / gcd);
        int maxDown = (int) Math.ceil((-f90 - startPitch) / gcd);
        pitchSteps = Math.max(maxDown, Math.min(maxUp, pitchSteps));
        return pitchSteps * gcd;
    }

    public void decay(ClientPlayerEntity player, MinecraftClient client) {
        if (!active || player == null) {
            stop();
            return;
        }

        if (player.age == lastUpdateTick) {
            return;
        }
        lastUpdateTick = player.age;

        float f90 = dev.mace.prestige.internal.MaceDomain.f(1042964282);
        float realYaw = player.getYaw();
        float realPitch = MathHelper.clamp(player.getPitch(), -f90, f90);
        float gcd = evaluateAimStep(client);

        float deltaYaw = MathHelper.wrapDegrees(realYaw - currentYaw);
        float deltaPitch = realPitch - currentPitch;

        if (Math.abs(deltaYaw) <= gcd && Math.abs(deltaPitch) <= gcd) {
            stop();
            return;
        }

        float stepYaw = Math.round(deltaYaw / gcd) * gcd;
        float stepPitch = quantizePitchStep(deltaPitch, currentPitch, gcd, f90);

        currentYaw = MathHelper.wrapDegrees(currentYaw + stepYaw);
        currentPitch = MathHelper.clamp(currentPitch + stepPitch, -f90, f90);

        AsyncSilentRot.set(currentYaw, currentPitch, this);
    }

    public void stop() {
        if (active) {
            active = false;
            targetEntityId = -1;
            lastUpdateTick = -1;
            AsyncSilentRot.stop(this);
        }
    }
}
