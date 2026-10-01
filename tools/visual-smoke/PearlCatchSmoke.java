package activity.visualsmoke;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import dev.raycast.RaycastPredictorController;

final class PearlCatchSmoke {
    private static int tick;
    private static int scenario;
    private static volatile EnderPearlEntity pearl;
    private static volatile WindChargeEntity wind;
    private static Vec3d previousRelative;
    private static volatile double minimum = Double.MAX_VALUE;
    private static volatile boolean redirected;
    private static activity.client.module.api.IModule module;

    static void tick(MinecraftClient client) {
        if (!client.player.isAlive()) { client.player.requestRespawn(); return; }
        tick++;
        int step = (tick - 1) % 90;
        if (tick == 1) {
            for (var mod : activity.client.module.api.ModuleRegistry.getAll()) mod.setEnabled(false);
            module = activity.client.module.api.ModuleRegistry.get("auto_pearl_catch");
            net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
                for (var entity : server.getOverworld().iterateEntities()) {
                    if (entity instanceof EnderPearlEntity value) pearl = value;
                    if (entity instanceof WindChargeEntity value) wind = value;
                }
                if (pearl != null && wind != null) {
                    Vec3d relative = pearl.getEntityPos().subtract(wind.getEntityPos());
                    double distance = relative.length();
                    if (previousRelative != null) {
                        Vec3d motion = relative.subtract(previousRelative);
                        double fraction = motion.lengthSquared() < 1e-8 ? 0 : Math.clamp(-previousRelative.dotProduct(motion) / motion.lengthSquared(), 0, 1);
                        distance = Math.min(distance, previousRelative.add(motion.multiply(fraction)).length());
                    }
                    minimum = Math.min(minimum, distance);
                    previousRelative = relative;
                    if (wind.isRemoved() && !pearl.isRemoved() && minimum < 1) redirected = true;
                }
            });
        }
        if (step == 0) {
            module.setEnabled(false);
            client.getServer().submit(() -> {
                for (var entity : client.getServer().getOverworld().iterateEntities()) if (entity instanceof EnderPearlEntity || entity instanceof WindChargeEntity) entity.discard();
                pearl = null; wind = null; previousRelative = null; minimum = Double.MAX_VALUE; redirected = false;
                var player = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);
                player.setNoGravity(true);
                player.requestTeleport(-42, 150, 70);
                player.getInventory().setStack(0, new ItemStack(Items.DIAMOND_SWORD));
                player.getInventory().setStack(1, new ItemStack(Items.ENDER_PEARL, 16));
                player.getInventory().setStack(2, new ItemStack(Items.WIND_CHARGE, 16));
                player.playerScreenHandler.sendContentUpdates();
                return true;
            }).join();
            client.player.setNoGravity(true);
            client.player.setPosition(-42, 150, 70);
            client.player.setYaw(0); client.player.setPitch(0);
            client.player.getInventory().setSelectedSlot(0);
        }
        if (step >= 5 && step <= 45) {
            client.player.setVelocity(scenario == 0 ? Vec3d.ZERO : new Vec3d(.28, scenario == 1 ? .2 : -.3, 0));
            client.player.setOnGround(false);
        }
        if (step == 15) {
            module.setEnabled(true);
            var cfg = activity.client.config.ActivityConfigManager.getConfig();
            cfg.autoPearlCatchEnabled = true; cfg.autoPearlCatchMode = "full_auto"; cfg.autoPearlCatchLegitMode = false;
            cfg.autoPearlCatchRandomDelay = false; cfg.autoPearlCatchThrowDelay = 2; cfg.autoPearlCatchRotationTimeMs = 50;
            cfg.autoPearlCatchRestoreSlot = true; cfg.autoPearlCatchRestoreCamera = true;
            System.out.println("PEARL_CATCH_SETUP alive=" + client.player.isAlive() + " screen=" + client.currentScreen + " pearl=" + RaycastPredictorController.findHotbarItem(client.player, Items.ENDER_PEARL) + " wind=" + RaycastPredictorController.findHotbarItem(client.player, Items.WIND_CHARGE) + " locks=" + net.fabricmc.pack.api.CombatLockManager.getLockMask());
            RaycastPredictorController.getInstance().trigger(client, scenario == 1 ? RaycastPredictorController.Mode.HORIZONTAL : RaycastPredictorController.Mode.VERTICAL);
            System.out.println("PEARL_CATCH_TRIGGER state=" + RaycastPredictorController.getInstance().getState());
        }
        if (step == 80) {
            System.out.println("PEARL_CATCH_SMOKE scenario=" + scenario + " pearl=" + (pearl != null) + " wind=" + (wind != null) + " closest=" + minimum + " redirected=" + redirected + " pearlRemoved=" + (pearl != null && pearl.isRemoved()) + " windRemoved=" + (wind != null && wind.isRemoved()) + " state=" + RaycastPredictorController.getInstance().getState());
            if (pearl == null || wind == null || minimum > .65) throw new IllegalStateException("Pearl/wind interception failed in scenario " + scenario);
            if (net.fabricmc.pack.api.CombatLockManager.isLocked()) throw new IllegalStateException("Pearl catch retained lock");
            scenario++;
            if (scenario == 3) { VisualSmoke.writeResult(true, "server pearl/wind trajectories: still, moving upwards, moving downwards"); client.scheduleStop(); }
        }
    }
}
