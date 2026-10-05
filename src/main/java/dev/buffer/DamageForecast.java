package dev.buffer;

import activity.client.util.Obf;
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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;



public final class DamageForecast {

    public record PublishedIntent(String module, float selfDamage, long expirationTimeMs) {}

    private static final AtomicReference<PublishedIntent> ACTIVE_INTENT = new AtomicReference<>(null);
    private static final AtomicLong LAST_BURST_TIME = new AtomicLong(0L);
    private static volatile float lastCalculatedBurst = 0.0F;
    private static final Map<Integer, Long> ENTITY_FIRST_SEEN = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> ENTITY_JITTER = new ConcurrentHashMap<>();

    public static void trackEntity(Entity entity, long worldTime) {
        ENTITY_FIRST_SEEN.putIfAbsent(entity.getId(), worldTime);
    }

    public static void pruneDeadEntities(MinecraftClient client) {
        if (client.world == null) {
            ENTITY_FIRST_SEEN.clear();
            ENTITY_JITTER.clear();
            return;
        }
        ENTITY_FIRST_SEEN.keySet().removeIf(id -> client.world.getEntityById(id) == null);
        ENTITY_JITTER.keySet().removeIf(id -> client.world.getEntityById(id) == null);
    }

    private static boolean isEntityLive(Entity entity, long worldTime) {
        int minTicks = BufferPipelineConfig.livenessMinTicks;
        if (minTicks <= 0) return true;
        Long firstSeen = ENTITY_FIRST_SEEN.get(entity.getId());
        if (firstSeen == null) {
            ENTITY_FIRST_SEEN.put(entity.getId(), worldTime);
            int jitter = java.util.concurrent.ThreadLocalRandom.current().nextInt(3) - 1;
            ENTITY_JITTER.put(entity.getId(), Math.max(1, minTicks + jitter));
            return false;
        }
        int required = ENTITY_JITTER.getOrDefault(entity.getId(), minTicks);
        return (worldTime - firstSeen) >= required;
    }

    private static boolean isInFovCone(ClientPlayerEntity player, Vec3d threatPos) {
        double halfAngleDeg = BufferPipelineConfig.fovFilterDegrees / 2.0;
        if (halfAngleDeg >= 90.0) return true;
        double cosThreshold = Math.cos(Math.toRadians(halfAngleDeg));
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d toThreat = threatPos.subtract(new Vec3d(player.getX(), player.getEyeY(), player.getZ())).normalize();
        return look.dotProduct(toThreat) >= cosThreshold;
    }

    private DamageForecast() {}

    public static void publishIntent(String module, float selfDamage, int delayTicks) {
        long expire = System.currentTimeMillis() + Math.max(Obf.l(6520153561102040442L), delayTicks * Obf.l(6520153561102040364L) + Obf.l(6520153561102040456L));
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
        return System.currentTimeMillis() - LAST_BURST_TIME.get() < Obf.l(6520153561102040298L);
    }

    private static float calculateExplosionRisk(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box searchBox = player.getBoundingBox().expand(Obf.d(1900023293373332766L));
        float highestDamage = 0.0F;
        long worldTime = client.world.getTime();

        for (Entity entity : client.world.getOtherEntities(player, searchBox)) {
            if (!isEntityLive(entity, worldTime)) continue;

            float power = 0.0F;
            if (entity instanceof EndCrystalEntity) {
                if (!isInFovCone(player, new Vec3d(entity.getX(), entity.getY(), entity.getZ()))) continue;
                power = Obf.f(0x1ABC3D1E);
            } else if (entity instanceof TntEntity tnt) {
                int fuseThreshold = 6 + (java.util.concurrent.ThreadLocalRandom.current().nextInt(2));
                if (tnt.getFuse() <= fuseThreshold) {
                    if (!isInFovCone(player, new Vec3d(entity.getX(), entity.getY(), entity.getZ()))) continue;
                    power = Obf.f(0x1AFC3D1E);
                }
            } else if (entity instanceof CreeperEntity creeper) {
                if (creeper.getFuseSpeed() > 0) {
                    if (!isInFovCone(player, new Vec3d(entity.getX(), entity.getY(), entity.getZ()))) continue;
                    power = Obf.f(0x1A3C3D1E);
                }
            }

            if (power > 0.0F) {
                double dist = Math.sqrt(entity.squaredDistanceTo(pos));
                double maxDist = power * Obf.d(1908467542674652446L);
                if (dist <= maxDist) {
                    double impact = (Obf.d(7317290695146618142L) - dist / maxDist);
                    double raw = (impact * impact + impact) / Obf.d(1908467542674652446L) * Obf.d(1900586243326754078L) * maxDist + Obf.d(7317290695146618142L);
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
                            Vec3d anchorCenter = Vec3d.ofCenter(p);
                            if (!isInFovCone(player, anchorCenter)) continue;
                            double dist = Math.sqrt(p.getSquaredDistance(pos));
                            double maxDist = Obf.d(1898334443513068830L);
                            if (dist <= maxDist) {
                                double impact = (Obf.d(7317290695146618142L) - dist / maxDist);
                                double raw = (impact * impact + impact) / Obf.d(1908467542674652446L) * Obf.d(1900586243326754078L) * maxDist + Obf.d(7317290695146618142L);
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
        if (vy >= -Obf.d(0x65AF0E2D694F0E2DL)) {
            return 0.0F;
        }

        float projectedDistance = (float) player.fallDistance + (float) (-vy * Obf.d(0x1A743D1E5A7C3D1EL));
        float f3 = Obf.f(0x1A3C3D1E);
        if (projectedDistance <= f3) {
            return 0.0F;
        }
        return (projectedDistance - f3);
    }

    private static float calculateMaceRisk(MinecraftClient client, ClientPlayerEntity player) {
        Vec3d pos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = player.getBoundingBox().expand(Obf.d(1904526893000703262L));
        float highestMace = 0.0F;
        long worldTime = client.world.getTime();

        for (Entity e : client.world.getOtherEntities(player, box)) {
            if (e instanceof PlayerEntity enemy && enemy.isAlive()) {
                if (!isEntityLive(enemy, worldTime)) continue;
                if (!isInFovCone(player, new Vec3d(enemy.getX(), enemy.getY(), enemy.getZ()))) continue;

                if (enemy.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.MACE)) {
                    if (enemy.getY() > player.getY() && enemy.getVelocity().y < Obf.d(-1894427165225278332L)) {
                        float fall = (float) enemy.fallDistance;
                        float damage = Obf.f(448544030) + fall * Obf.f(440155422);
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
        Box box = player.getBoundingBox().expand(Obf.d(1899460343419911454L));
        float highestTrident = 0.0F;

        for (Entity e : client.world.getOtherEntities(player, box)) {
            if (e instanceof TridentEntity trident) {
                Vec3d vel = trident.getVelocity();
                if (vel.lengthSquared() > Obf.d(7328944871629497476L)) {
                    Vec3d tridentPos = new Vec3d(trident.getX(), trident.getY(), trident.getZ());
                    Vec3d toPlayer = pos.subtract(tridentPos).normalize();
                    double dot = vel.normalize().dotProduct(toPlayer);
                    if (dot > Obf.d(7322587107330821677L)) {
                        float reduced = reduceDamageByArmor(player, Obf.f(460078366));
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
        float factor = Obf.f(1711029534) - Math.min(Obf.f(467418398), Math.max(armor / Obf.f(450641182), armor - damage / (Obf.f(444349726) + toughness / Obf.f(452738334)))) / Obf.f(464796958);
        return Math.max(0.0F, damage * factor);
    }
}
