package dev.buffer;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class DamageForecast {

    public record PublishedIntent(String module, float selfDamage, long expirationTimeMs) {}

    private static final AtomicReference<PublishedIntent> ACTIVE_INTENT = new AtomicReference<>(null);
    private static final AtomicLong LAST_BURST_TIME = new AtomicLong(0L);
    private static volatile float lastCalculatedBurst = 0.0F;

    private DamageForecast() {}

    public static void publishIntent(String module, float selfDamage, int delayTicks) {
        long expire = System.currentTimeMillis() + Math.max(100L, delayTicks * 50L + 150L);
        ACTIVE_INTENT.set(new PublishedIntent(module, selfDamage, expire));
    }

    public static void clearIntent(String module) {
        PublishedIntent cur = ACTIVE_INTENT.get();
        if (cur != null && cur.module().equals(module)) {
            ACTIVE_INTENT.set(null);
        }
    }

    public static float calculateExpectedBurst(MinecraftClient client, ClientPlayerEntity player) {
        if (client == null || player == null || client.world == null || !player.isAlive()) {
            lastCalculatedBurst = 0.0F;
            return 0.0F;
        }

        float totalBurst = 0.0F;

        PublishedIntent intent = ACTIVE_INTENT.get();
        if (intent != null) {
            if (System.currentTimeMillis() <= intent.expirationTimeMs()) {
                totalBurst += intent.selfDamage();
            } else {
                ACTIVE_INTENT.set(null);
            }
        }

        if (BufferPipelineConfig.predictiveDamage) {
            if (BufferPipelineConfig.predictCrystals) {
                totalBurst += calculateExplosionRisk(client, player);
            }
            if (BufferPipelineConfig.predictFall) {
                totalBurst += calculateFallRisk(player);
            }
            if (BufferPipelineConfig.predictMace) {
                totalBurst += calculateMaceRisk(client, player);
            }
            if (BufferPipelineConfig.predictTrident) {
                totalBurst += calculateTridentRisk(client, player);
            }
        }

        lastCalculatedBurst = totalBurst;
        if (totalBurst > 0.0F) {
            LAST_BURST_TIME.set(System.currentTimeMillis());
        }
        return totalBurst;
    }

    public static float getLastCalculatedBurst() {
        return lastCalculatedBurst;
    }

    public static boolean isRecentBurstThreat() {
        return System.currentTimeMillis() - LAST_BURST_TIME.get() < 500L;
    }

    private static float calculateExplosionRisk(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box searchBox = player.getBoundingBox().expand(9.0D);
        float highestDamage = 0.0F;

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            float power = 0.0F;
            if (entity instanceof EndCrystalEntity) {
                power = 6.0F;
            } else if (entity instanceof TntEntity tnt) {
                if (tnt.getFuse() <= 6) {
                    power = 4.0F;
                }
            } else if (entity instanceof CreeperEntity creeper) {
                if (creeper.getFuseSpeed() > 0) {
                    power = 3.0F;
                }
            }

            if (power > 0.0F) {
                double dist = Math.sqrt(entity.squaredDistanceTo(pos));
                double maxDist = power * 2.0D;
                if (dist <= maxDist) {
                    double impact = (1.0D - dist / maxDist);
                    double raw = (impact * impact + impact) / 2.0D * 7.0D * maxDist + 1.0D;
                    float reduced = reduceDamageByArmor(player, (float) raw);
                    if (reduced > highestDamage) {
                        highestDamage = reduced;
                    }
                }
            }
        }

        BlockPos playerBlock = player.getBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = playerBlock.add(dx, dy, dz);
                    if (client.world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)) {
                        int charges = client.world.getBlockState(p).get(RespawnAnchorBlock.CHARGES);
                        if (charges > 0) {
                            double dist = Math.sqrt(p.getSquaredDistance(pos));
                            double maxDist = 10.0D;
                            if (dist <= maxDist) {
                                double impact = (1.0D - dist / maxDist);
                                double raw = (impact * impact + impact) / 2.0D * 7.0D * maxDist + 1.0D;
                                float reduced = reduceDamageByArmor(player, (float) raw);
                                if (reduced > highestDamage) {
                                    highestDamage = reduced;
                                }
                            }
                        }
                    }
                }
            }
        }

        return highestDamage;
    }

    private static float calculateFallRisk(ClientPlayerEntity player) {
        if (player.isOnGround() || player.getAbilities().flying || player.isGliding()) {
            return 0.0F;
        }
        double vy = player.getVelocity().y;
        if (vy >= -0.3D) {
            return 0.0F;
        }

        float projectedDistance = (float) player.fallDistance + (float) (-vy * 3.0D);
        if (projectedDistance <= 3.0F) {
            return 0.0F;
        }
        return (projectedDistance - 3.0F);
    }

    private static float calculateMaceRisk(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = player.getBoundingBox().expand(4.5D);
        float highestMace = 0.0F;

        for (Entity e : client.world.getOtherEntities(player, box)) {
            if (e instanceof PlayerEntity enemy && enemy.isAlive()) {
                if (enemy.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.MACE)) {
                    if (enemy.getY() > player.getY() && enemy.getVelocity().y < -0.2D) {
                        float fall = (float) enemy.fallDistance;
                        float damage = 6.0F + fall * 3.0F;
                        float reduced = reduceDamageByArmor(player, damage);
                        if (reduced > highestMace) {
                            highestMace = reduced;
                        }
                    }
                }
            }
        }
        return highestMace;
    }

    private static float calculateTridentRisk(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = player.getBoundingBox().expand(8.0D);
        float highestTrident = 0.0F;

        for (Entity e : client.world.getOtherEntities(player, box)) {
            if (e instanceof TridentEntity trident) {
                Vec3d vel = trident.getVelocity();
                if (vel.lengthSquared() > 0.2D) {
                    Vec3d tridentPos = new Vec3d(trident.getX(), trident.getY(), trident.getZ());
                    Vec3d toPlayer = pos.subtract(tridentPos).normalize();
                    double dot = vel.normalize().dotProduct(toPlayer);
                    if (dot > 0.6D) {
                        float reduced = reduceDamageByArmor(player, 9.0F);
                        if (reduced > highestTrident) {
                            highestTrident = reduced;
                        }
                    }
                }
            }
        }
        return highestTrident;
    }

    private static float reduceDamageByArmor(ClientPlayerEntity player, float damage) {
        float armor = player.getArmor();
        float toughness = 0.0F;
        float factor = 1.0F - Math.min(20.0F, Math.max(armor / 5.0F, armor - damage / (2.0F + toughness / 4.0F))) / 25.0F;
        return Math.max(0.0F, damage * factor);
    }
}
