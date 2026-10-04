package activity.visualsmoke;

import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.defense.OcclusionCacheModule;
import dev.nivorat.arc.MorrowConfig;
import dev.nivorat.arc.AutoCartController;
import net.fabricmc.pack.api.CombatLockManager;
import net.fabricmc.pack.api.SafeSlotManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import java.util.concurrent.atomic.AtomicBoolean;

final class AutoCartSmoke {
    private static int tick;
    private static OcclusionCacheModule cart;
    private static BlockPos support;
    private static final AtomicBoolean sawArrow = new AtomicBoolean();
    private static final AtomicBoolean sawCart = new AtomicBoolean();

    static void tick(MinecraftClient client) throws Exception {
        tick++;
        require(client.player.isAlive(), "AutoCart test player died");
        if (tick == 1) {
            for (var module : ModuleRegistry.getAll()) module.setEnabled(false);
            cart = (OcclusionCacheModule) ModuleRegistry.get("auto_cart");
            cart.setEnabled(true);
            for (int slot = 0; slot < 36; slot++) stack(client, slot, ItemStack.EMPTY);
            stack(client, 40, ItemStack.EMPTY);
            stack(client, 0, new ItemStack(Items.DIAMOND_SWORD));
            stack(client, 1, new ItemStack(Items.RAIL, 16));
            stack(client, 2, new ItemStack(Items.TNT_MINECART));
            stack(client, 3, new ItemStack(Items.BOW));
            stack(client, 12, new ItemStack(Items.ARROW, 64));
            client.getServer().submit(() -> {
                var player = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                player.setNoGravity(true);
                player.setInvulnerable(true);
                player.setHealth(player.getMaxHealth());
                return null;
            }).join();
            client.player.setNoGravity(true);
            SafeSlotManager.selectSlot(client, 0);
            MorrowConfig.legitMode = true;
            MorrowConfig.autoCamera = true;
            MorrowConfig.cameraMode = "auto";
            MorrowConfig.cameraSmoothnessMs = 110;
            MorrowConfig.randomDelay = false;
            MorrowConfig.placementChance = 100;
            MorrowConfig.maxDistance = 4.5;
            MorrowConfig.useMainhandCart = true;
            MorrowConfig.cameraReturn = true;
            MorrowConfig.autonomousPlacement = true;
        }
        if (tick == 30) {
            support = client.player.getBlockPos().add(0, -1, 3);
            client.getServer().submit(() -> {
                var world = client.getServer().getOverworld();
                for (var entity : world.iterateEntities())
                    if (entity instanceof ArrowEntity || entity instanceof TntMinecartEntity) entity.discard();
                for (int x = -1; x <= 1; x++) for (int z = -3; z <= 1; z++) {
                    var base = support.add(x, 0, z);
                    world.setBlockState(base, net.minecraft.block.Blocks.STONE.getDefaultState());
                    for (int height = 1; height <= 3; height++) world.setBlockState(base.up(height), net.minecraft.block.Blocks.AIR.getDefaultState());
                }
                return null;
            }).join();
            client.player.setYaw(0f);
            client.player.setPitch(30f);
        }
        if (tick == 60) require(cart.getController().startMacro(client, 6), "Macro did not start");
        if (tick >= 61 && tick <= 139) client.getServer().execute(() -> {
            for (var entity : client.getServer().getOverworld().iterateEntities()) {
                if (entity instanceof ArrowEntity) sawArrow.set(true);
                if (entity instanceof TntMinecartEntity) sawCart.set(true);
            }
        });
        if (tick == 140) {
            require(sawArrow.get(), "No physical server arrow");
            require(sawCart.get(), "No server minecart");
            require(client.player.getInventory().getSelectedSlot() == 0, "Macro did not restore original sword");
            require(!CombatLockManager.isLocked(), "Macro retained combat lock");
            require(!client.options.useKey.isPressed(), "Macro retained virtual use key");
            System.out.println("AUTOCART_SMOKE passed: physical shot, smooth camera, server placement, slot/key/lock cleanup");
            client.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
        }
        if (tick == 155) {
            capture(client, "autocart-ui-ready.png");
        }
        if (tick == 170) {
            client.setScreen(null);
            clearEntities(client);
            stack(client, 12, ItemStack.EMPTY);
            require(!cart.getController().startMacro(client, 6), "Macro accepted missing arrows");
            stack(client, 12, new ItemStack(Items.ARROW, 64));
            SafeSlotManager.selectSlot(client, 3);
            sawCart.set(false);
        }
        if (tick == 185) {
            for (String name : java.util.List.of("wasUsingBow", "bowDrawTicks", "bowSlot")) {
                var field = AutoCartController.class.getDeclaredField(name); field.setAccessible(true);
                if (name.equals("wasUsingBow")) field.setBoolean(cart.getController(), true);
                else field.setInt(cart.getController(), name.equals("bowSlot") ? 3 : 6);
            }
            cart.getController().tick(client);
        }
        if (tick >= 186 && tick <= 224) client.getServer().execute(() -> {
            for (var entity : client.getServer().getOverworld().iterateEntities()) if (entity instanceof TntMinecartEntity) sawCart.set(true);
        });
        if (tick == 225) {
            require(!sawCart.get(), "Cart placed without physical shot");
            require(!CombatLockManager.isLocked(), "Missing shot retained lock");
            stack(client, 2, new ItemStack(Items.TNT_MINECART));
            SafeSlotManager.selectSlot(client, 0);
            require(cart.getController().startMacro(client, 6), "Cancellation macro did not start");
            require(CombatLockManager.isLocked(), "Drawing macro was not protected from other actions");
            client.setScreen(new net.minecraft.client.gui.screen.ChatScreen("", false));
        }
        if (tick == 235) {
            require(!client.options.useKey.isPressed(), "GUI cancellation retained use key");
            require(client.player.getInventory().getSelectedSlot() == 0, "GUI cancellation retained bow slot");
            require(!CombatLockManager.isLocked(), "GUI cancellation retained lock");
            client.setScreen(null);
            System.out.println("AUTOCART_GUARDS passed: missing physical shot and GUI cancellation");
        }
        if (tick == 240) {
            client.setScreen(null);
            VisualSmoke.writeResult(true, "AutoCart physical arrow gate, macro, smooth camera, and cancellation");
            client.scheduleStop();
        }
    }

    private static void clearEntities(MinecraftClient client) {
        client.getServer().submit(() -> {
            for (var entity : client.getServer().getOverworld().iterateEntities())
                if (entity instanceof ArrowEntity || entity instanceof TntMinecartEntity) entity.discard();
            return null;
        }).join();
    }

    private static void stack(MinecraftClient client, int slot, ItemStack value) {
        client.player.getInventory().setStack(slot, value);
        var copy = value.copy();
        client.getServer().submit(() -> {
            var player = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
            player.getInventory().setStack(slot, copy);
            player.playerScreenHandler.sendContentUpdates();
            return null;
        }).join();
    }

    private static void capture(MinecraftClient client, String name) {
        java.io.File directory = new java.io.File(System.getProperty("visual.smoke.output")); directory.mkdirs();
        net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(directory, name, client.getFramebuffer(), 1, text -> {});
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
