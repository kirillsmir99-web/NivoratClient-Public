package activity.client.module.service;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared per-tick combat target resolution and caching service.
 *
 * <p>Avoids duplicate raycast and component inspections when multiple combat
 * modules query the crosshair target and shield blocking state in the same tick.
 */
public final class TargetCacheService {

    private static long lastTickId = -1L;
    private static Entity cachedCrosshairEntity = null;
    private static boolean crosshairResolved = false;
    private static final Map<Integer, Boolean> SHIELD_BLOCKING_CACHE = new ConcurrentHashMap<>();

    private TargetCacheService() {}

    public static void onTick(MinecraftClient client, long tickId) {
        if (tickId != lastTickId) {
            lastTickId = tickId;
            reset();
            if (client != null) {
                resolveCrosshair(client);
            }
        }
    }

    private static void resolveCrosshair(MinecraftClient client) {
        crosshairResolved = true;
        if (client.targetedEntity != null) {
            cachedCrosshairEntity = client.targetedEntity;
            return;
        }
        if (client.crosshairTarget instanceof EntityHitResult ehr) {
            cachedCrosshairEntity = ehr.getEntity();
            return;
        }
        cachedCrosshairEntity = null;
    }

    public static Entity getCrosshairTarget(MinecraftClient client) {
        if (client == null) return null;
        if (crosshairResolved && (cachedCrosshairEntity == null || !cachedCrosshairEntity.isRemoved())) {
            return cachedCrosshairEntity;
        }
        resolveCrosshair(client);
        return cachedCrosshairEntity;
    }

    public static LivingEntity getCrosshairLivingTarget(MinecraftClient client) {
        Entity target = getCrosshairTarget(client);
        return (target instanceof LivingEntity living && living.isAlive()) ? living : null;
    }

    public static PlayerEntity getCrosshairPlayerTarget(MinecraftClient client) {
        Entity target = getCrosshairTarget(client);
        return (target instanceof PlayerEntity player && player.isAlive() && !player.isSpectator()) ? player : null;
    }

    public static boolean isTargetShielding(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        int id = entity.getId();
        Boolean cached = SHIELD_BLOCKING_CACHE.get(id);
        if (cached != null) return cached;

        boolean blocking = entity.isBlocking();
        if (!blocking && entity.isUsingItem()) {
            net.minecraft.item.ItemStack active = entity.getActiveItem();
            blocking = active != null && !active.isEmpty()
                    && (active.getItem() instanceof ShieldItem || active.isOf(Items.SHIELD));
        }
        SHIELD_BLOCKING_CACHE.put(id, blocking);
        return blocking;
    }

    public static int getShieldCacheSize() {
        return SHIELD_BLOCKING_CACHE.size();
    }

    public static void setCachedCrosshairEntityForTest(Entity entity) {
        cachedCrosshairEntity = entity;
        crosshairResolved = true;
    }

    public static void reset() {
        cachedCrosshairEntity = null;
        crosshairResolved = false;
        SHIELD_BLOCKING_CACHE.clear();
    }
}
