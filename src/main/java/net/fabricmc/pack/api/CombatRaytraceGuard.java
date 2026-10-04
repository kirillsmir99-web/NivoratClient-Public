package net.fabricmc.pack.api;

import net.fabricmc.pack.api.internal.CombatDomain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class CombatRaytraceGuard {
    public static final double MAX_COMBAT_REACH = CombatDomain.d(1018250555469504350L) ;
    public static final double MAX_BLOCK_REACH = CombatDomain.d(1025485568484987795L) ;

    private static volatile boolean dispatcherManaged = false;
    private static long lastRaycastTick = -1L;
    private static final java.util.Map<Integer, Boolean> ENTITY_LOS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<BlockPos, Boolean> BLOCK_LOS_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private CombatRaytraceGuard() {}

    public static void markDispatcherManaged() {
        dispatcherManaged = true;
    }

    public static boolean isDispatcherManaged() {
        return dispatcherManaged;
    }

    public static void onTick(long tick) {
        if (tick != lastRaycastTick) {
            lastRaycastTick = tick;
            clearCache();
        }
    }

    public static void clearCache() {
        ENTITY_LOS_CACHE.clear();
        BLOCK_LOS_CACHE.clear();
    }

    public static int getEntityCacheSize() {
        return ENTITY_LOS_CACHE.size();
    }

    public static int getBlockCacheSize() {
        return BLOCK_LOS_CACHE.size();
    }

    public static boolean hasLineOfSight(ClientPlayerEntity player, Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == null || target == null || client.world == null) {
            return false;
        }
        if (!dispatcherManaged) {
            long currentTick = client.world.getTime();
            if (currentTick != lastRaycastTick) {
                lastRaycastTick = currentTick;
                clearCache();
            }
        }

        Boolean cached = ENTITY_LOS_CACHE.get(target.getId());
        if (cached != null) {
            return cached;
        }

        World world = client.world;
        Vec3d eyePos = player.getEyePos();
        Vec3d targetEye = target.getEyePos();

        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos,
                targetEye,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            ENTITY_LOS_CACHE.put(target.getId(), Boolean.TRUE);
            return true;
        }

        Vec3d center = target.getBoundingBox().getCenter();
        hit = world.raycast(new RaycastContext(
                eyePos,
                center,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        boolean result = hit == null || hit.getType() == HitResult.Type.MISS;
        ENTITY_LOS_CACHE.put(target.getId(), result);
        return result;
    }

    public static boolean hasLineOfSight(ClientPlayerEntity player, BlockPos target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == null || target == null || client.world == null) {
            return false;
        }
        if (!dispatcherManaged) {
            long currentTick = client.world.getTime();
            if (currentTick != lastRaycastTick) {
                lastRaycastTick = currentTick;
                clearCache();
            }
        }

        Boolean cached = BLOCK_LOS_CACHE.get(target);
        if (cached != null) {
            return cached;
        }

        World world = client.world;
        Vec3d eyePos = player.getEyePos();
        Vec3d center = new Vec3d(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);

        BlockHitResult hit = world.raycast(new RaycastContext(
                eyePos,
                center,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        boolean result;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            result = true;
        } else {
            result = hit.getBlockPos().equals(target);
        }
        BLOCK_LOS_CACHE.put(target, result);
        return result;
    }

    public static boolean canReachCombat(ClientPlayerEntity player, Entity target, double maxRange) {
        if (player == null || target == null) {
            return false;
        }
        double limit = Math.min(MAX_COMBAT_REACH, maxRange);
        Vec3d eyePos = player.getEyePos();
        Box box = target.getBoundingBox().expand(CombatDomain.d(8179122849764804292L) );
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d reachEnd = eyePos.add(lookVec.multiply(limit));

        var hitOpt = box.expand(CombatDomain.d(8183626449392174788L) ).raycast(eyePos, reachEnd);
        if (hitOpt.isPresent()) {
            return eyePos.squaredDistanceTo(hitOpt.get()) <= limit * limit && hasLineOfSight(player, target);
        }

        return false;
    }

    public static boolean canReachCombat(ClientPlayerEntity player, Entity target) {
        return canReachCombat(player, target, MAX_COMBAT_REACH);
    }

    public static boolean canReachBlock(ClientPlayerEntity player, BlockPos target, double maxRange) {
        if (player == null || target == null) {
            return false;
        }
        double limit = Math.min(MAX_BLOCK_REACH, maxRange);
        Vec3d eyePos = player.getEyePos();
        double offsetHalf = CombatDomain.d(8199803111265181534L) ;
        Vec3d center = new Vec3d(target.getX() + offsetHalf, target.getY() + offsetHalf, target.getZ() + offsetHalf);
        if (eyePos.squaredDistanceTo(center) > limit * limit) {
            return false;
        }
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d toTarget = center.subtract(eyePos).normalize();
        return lookVec.dotProduct(toTarget) >= CombatDomain.d(8210648047156397764L)  && hasLineOfSight(player, target);
    }

    public static boolean canReachBlock(ClientPlayerEntity player, BlockPos target) {
        return canReachBlock(player, target, MAX_BLOCK_REACH);
    }
}
