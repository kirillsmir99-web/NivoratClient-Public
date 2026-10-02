package dev.raycast.async;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AsyncMath {
    private AsyncMath() {
    }

    public static Vec3d getDirection(float yaw, float pitch) {
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        float yawRad = -yaw * (float) (Math.PI / 180.0);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        return new Vec3d((double) (sinYaw * cosPitch), (double) (-sinPitch), (double) (cosYaw * cosPitch));
    }

    public static AsyncRot getRotation(Vec3d delta) {
        double yaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()));
        return new AsyncRot(yaw, pitch);
    }
}
