package activity.visualsmoke;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.fabricmc.pack.api.SafeSlotManager;
import net.fabricmc.pack.api.CombatLockManager;
import dev.pearl.ClickPearlConfig;
import activity.client.module.impl.combat.ClickPearlModule;

final class MechanicsSmoke {
    private static int ticks;
    private static ClickPearlModule pearl;
    private static final java.util.concurrent.atomic.AtomicBoolean serverSawPearl = new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.concurrent.atomic.AtomicBoolean serverSawCart = new java.util.concurrent.atomic.AtomicBoolean();
    private static net.minecraft.util.math.BlockPos support;
    private static activity.client.module.impl.defense.OcclusionCacheModule cart;

    static void tick(MinecraftClient client) {
        if (!client.player.isAlive()) { client.player.requestRespawn(); return; }
        ticks++;
        if (ticks == 1) {
            client.getServer().submit(() -> {
                client.getServer().getPlayerManager().getPlayer(client.player.getUuid()).changeGameMode(net.minecraft.world.GameMode.CREATIVE);
                var player = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                player.setInvulnerable(true);
                player.setNoGravity(true);
                player.requestTeleport(-42, 170, 70);
                for (var entity : client.getServer().getOverworld().iterateEntities()) if (entity instanceof net.minecraft.entity.mob.HostileEntity || entity instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity) entity.discard();
            }).join();
            client.player.setNoGravity(true);
            client.player.setPosition(-42, 170, 70);
            for (int slot = 0; slot < 9; slot++) setStack(client, slot, ItemStack.EMPTY);
            setStack(client, 40, ItemStack.EMPTY);
            for (var module : activity.client.module.api.ModuleRegistry.getAll()) module.setEnabled(false);
            pearl = (ClickPearlModule) activity.client.module.api.ModuleRegistry.get("click_pearl");
            pearl.setEnabled(true);
            ClickPearlConfig.mode = "legit";
            ClickPearlConfig.searchMode = "hotbar";
            ClickPearlConfig.switchBack = true;
            ClickPearlConfig.randomDelay = false;
            ClickPearlConfig.preferOffhand = false;
            ClickPearlConfig.checkCooldown = false;
            client.player.setPitch(-65f);
            setStack(client, 0, new ItemStack(Items.DIAMOND_SWORD));
            setStack(client, 2, new ItemStack(Items.ENDER_PEARL, 16));
            SafeSlotManager.selectSlot(client, 0);
        }
        if (ticks == 12) {
            pearl.getController().trigger(client);
            require(client.player.getInventory().getSelectedSlot() == 2, "Pearl hotbar selection");
        }
        if (ticks >= 13 && ticks <= 35) {
            var server = client.getServer();
            server.execute(() -> {
                for (var entity : server.getOverworld().iterateEntities()) {
                    if (entity instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity) serverSawPearl.set(true);
                }
            });
        }
        if (ticks == 40) {
            require(serverSawPearl.get(), "Integrated server did not receive pearl throw");
            require(client.player.getInventory().getSelectedSlot() == 0, "Dispatcher did not restore pearl slot");
            require(!CombatLockManager.isLocked(), "Pearl sequence retained combat lock");
            var server = client.getServer();
            server.execute(() -> {
                for (var entity : server.getOverworld().iterateEntities()) {
                    if (entity instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity) entity.discard();
                }
            });
            ClickPearlConfig.mode = "legit";
            pearl.getController().trigger(client);
            client.setScreen(new net.minecraft.client.gui.screen.ChatScreen("", false));
        }
        if (ticks == 44) {
            require(client.player.getInventory().getSelectedSlot() == 0, "GUI suspension did not restore slot");
            require(!CombatLockManager.isLocked(), "GUI suspension retained combat lock");
            client.setScreen(null);
            pearl.setEnabled(false);
        }
        if (ticks == 50) {
            support = client.player.getBlockPos().add(0, -1, 3);
            var server = client.getServer();
            server.execute(() -> {
                for (var entity : server.getOverworld().iterateEntities()) {
                    if (entity instanceof net.minecraft.entity.vehicle.TntMinecartEntity) entity.discard();
                }
                for (int x=-1; x<=1; x++) for (int z=-3; z<=1; z++) {
                    var base = support.add(x,0,z);
                    server.getOverworld().setBlockState(base, net.minecraft.block.Blocks.STONE.getDefaultState());
                    for (int i=1; i<=3; i++) server.getOverworld().setBlockState(base.up(i), net.minecraft.block.Blocks.AIR.getDefaultState());
                }
            });
            setStack(client, 0, new ItemStack(Items.BOW));
            setStack(client, 1, new ItemStack(Items.RAIL, 16));
            setStack(client, 2, new ItemStack(Items.TNT_MINECART));
            cart = (activity.client.module.impl.defense.OcclusionCacheModule) activity.client.module.api.ModuleRegistry.get("auto_cart");
            cart.setEnabled(true);
            dev.virion.arc.MorrowConfig.legitMode = false;
            dev.virion.arc.MorrowConfig.autoCamera = false;
            dev.virion.arc.MorrowConfig.cameraMode = "packet";
            dev.virion.arc.MorrowConfig.randomDelay = false;
            dev.virion.arc.MorrowConfig.placementChance = 100;
            dev.virion.arc.MorrowConfig.maxDistance = 4.5;
            dev.virion.arc.MorrowConfig.useMainhandCart = true;
            client.player.setYaw(0f);
            client.player.setPitch(30f);
        }
        if (ticks == 75) {
            client.crosshairTarget = new net.minecraft.util.hit.BlockHitResult(
                    net.minecraft.util.math.Vec3d.ofCenter(support).add(0, .5, 0),
                    net.minecraft.util.math.Direction.UP, support, false);
            try {
                var allowed = dev.virion.arc.VirionArcController.class.getDeclaredMethod("canPlaceRail", MinecraftClient.class, net.minecraft.util.math.BlockPos.class);
                allowed.setAccessible(true);
                System.out.println("CART_SMOKE_SETUP player="+client.player.getBlockPos()+" support="+support+" rail="+activity.client.module.service.CartStateService.findRailSlot(client.player)+" cart="+activity.client.module.service.CartStateService.findHotbarCart(client.player)+" canPlace="+allowed.invoke(cart.getController(),client,support.up())+" cooldown="+activity.client.module.service.CartStateService.isCartOnCooldown(client.player)+" locks="+CombatLockManager.getLockMask());
                var begin = dev.virion.arc.VirionArcController.class.getDeclaredMethod("beginPlacementSequence", MinecraftClient.class, int.class);
                begin.setAccessible(true);
                begin.invoke(cart.getController(), client, 20);
            } catch (ReflectiveOperationException ex) {throw new IllegalStateException(ex);}
            require(CombatLockManager.isLocked(CombatLockManager.CART_PLACEMENT), "Cart sequence did not start");
        }
        if (ticks >= 76 && ticks <= 115) {
            var server = client.getServer();
            server.execute(() -> {
                for (var entity : server.getOverworld().iterateEntities()) {
                    if (entity instanceof net.minecraft.entity.vehicle.TntMinecartEntity) serverSawCart.set(true);
                }
            });
        }
        if (ticks == 90 || ticks == 105) {
            System.out.println("CART_SMOKE_PROGRESS slot="+client.player.getInventory().getSelectedSlot()+" hand="+client.player.getMainHandStack()+" rail="+client.world.getBlockState(support.up())+" lock="+CombatLockManager.getLockMask());
        }
        if (ticks == 120) {
            require(serverSawCart.get(), "Integrated server did not receive cart placement");
            require(!CombatLockManager.isLocked(), "Cart sequence retained combat lock");
            require(client.player.getInventory().getSelectedSlot() == 0, "Cart sequence did not restore bow slot");
            cart.setEnabled(false);
            System.out.println("MECHANICS_SMOKE completed: server-observed pearl throw, tick dispatch, slot return, GUI cancellation, lock cleanup; source=" + pearl.getClass().getProtectionDomain().getCodeSource().getLocation());
            System.out.println("MECHANICS_CART_SMOKE completed: integrated-server TNT minecart, accepted rail/cart interactions, slot return, lock cleanup");
            for (int slot = 0; slot < 9; slot++) setStack(client, slot, ItemStack.EMPTY);
            setStack(client, 40, ItemStack.EMPTY);
            setStack(client, 0, new ItemStack(Items.DIAMOND_SWORD));
            setStack(client, 12, new ItemStack(Items.ENDER_PEARL, 16));
            ClickPearlConfig.searchMode = "inventory";
            ClickPearlConfig.targetHotbarSlot = 3;
            pearl.setEnabled(true);
            serverSawPearl.set(false);
            client.player.setPitch(-65f);
            client.getServer().execute(() -> {
                var serverPlayer = client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                serverPlayer.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
            });
        }
        if (ticks == 134) {
            ClickPearlConfig.searchMode = "inventory";
            ClickPearlConfig.returnPearl = true;
            ClickPearlConfig.targetHotbarSlot = 3;
            ClickPearlConfig.checkCooldown = false;
            ClickPearlConfig.preferOffhand = false;
            ClickPearlConfig.combatGuard = false;
            System.out.println("INVENTORY_PEARL_SETUP state=" + pearl.getController().getState() + " inv=" + dev.pearl.ClickPearlController.findInventoryItem(client.player, Items.ENDER_PEARL) + " hotbar=" + dev.pearl.ClickPearlController.findHotbarItem(client.player, Items.ENDER_PEARL) + " lock=" + CombatLockManager.getLockMask() + " selected=" + client.player.getInventory().getSelectedSlot());
            pearl.getController().trigger(client);
            require(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen, "Inventory was not physically opened");
        }
        if (ticks == 135) require(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen, "Inventory closed before an extraction tick");
        if (ticks >= 140 && ticks <= 159) client.getServer().execute(() -> {
            for (var entity : client.getServer().getOverworld().iterateEntities()) if (entity instanceof net.minecraft.entity.projectile.thrown.EnderPearlEntity) serverSawPearl.set(true);
        });
        if (ticks == 165) {
            require(serverSawPearl.get(), "Inventory PEARL was not received by the server");
            require(client.currentScreen == null, "Managed inventory remained open");
            require(client.player.getInventory().getSelectedSlot() == 0, "Inventory pearl did not restore selected slot");
            require(client.player.getInventory().getStack(12).isOf(Items.ENDER_PEARL), "Inventory pearl stack was not restored");
            require(client.player.getInventory().getStack(2).isEmpty(), "Hotbar was not restored");
            require(!CombatLockManager.isLocked(), "Inventory pearl retained lock");
            System.out.println("INVENTORY_PEARL_SMOKE passed: visible inventory, accepted swap/use/return, restored slots, no lock");
            setStack(client, 12, new ItemStack(Items.ENDER_PEARL, 16));
            setStack(client, 13, new ItemStack(Items.ENDER_PEARL, 8));
            setStack(client, 2, new ItemStack(Items.DIAMOND_AXE));
        }
        if (ticks == 175) {
            ClickPearlConfig.returnPearl = false;
            pearl.getController().trigger(client);
        }
        if (ticks == 200) {
            require(client.currentScreen == null, "Leave-in-hotbar inventory stayed open");
            require(client.player.getInventory().getStack(2).isOf(Items.ENDER_PEARL), "PEARL was not left in configured hotbar slot");
            require(client.player.getInventory().getStack(12).isOf(Items.DIAMOND_AXE), "Displaced item was lost");
            require(client.player.getInventory().getStack(13).getCount() == 8, "Second inventory stack was moved");
            require(!CombatLockManager.isLocked(), "Leave-in-hotbar retained lock");
            setStack(client, 12, new ItemStack(Items.ENDER_PEARL, 16));
            setStack(client, 2, ItemStack.EMPTY);
        }
        if (ticks == 210) {
            ClickPearlConfig.returnPearl = true;
            pearl.getController().trigger(client);
        }
        if (ticks == 213) client.setScreen(null);
        if (ticks == 235) {
            require(client.currentScreen == null, "Manual close reopened inventory");
            require(pearl.getController().getState() == dev.pearl.ClickPearlController.State.IDLE, "Manual close did not cancel");
            require(!CombatLockManager.isLocked(), "Manual close retained lock");
            setStack(client, 12, new ItemStack(Items.ENDER_PEARL, 16));
            setStack(client, 2, ItemStack.EMPTY);
        }
        if (ticks == 245) pearl.getController().trigger(client);
        if (ticks == 247) pearl.setEnabled(false);
        if (ticks == 268) {
            require(client.currentScreen == null, "Disable reopened inventory");
            require(!CombatLockManager.isLocked(), "Disable retained lock");
            pearl.setEnabled(true);
            ClickPearlConfig.searchMode = "inventory";
            ClickPearlConfig.checkCooldown = false;
            setStack(client, 2, new ItemStack(Items.ENDER_PEARL, 16));
            setStack(client, 12, new ItemStack(Items.ENDER_PEARL, 8));
        }
        if (ticks == 278) {
            System.out.println("HOTBAR_PRIORITY_SETUP hotbar=" + dev.pearl.ClickPearlController.findHotbarItem(client.player, Items.ENDER_PEARL) + " selected=" + client.player.getInventory().getSelectedSlot() + " state=" + pearl.getController().getState());
            pearl.getController().trigger(client);
            require(client.currentScreen == null, "Hotbar PEARL opened inventory despite priority");
        }
        if (ticks >= 279 && ticks <= 300 && client.currentScreen != null) System.out.println("HOTBAR_PRIORITY_SCREEN " + ticks + " screen=" + client.currentScreen + " state=" + pearl.getController().getState());
        if (ticks == 300) {
            require(client.currentScreen == null, "Hotbar operation screen=" + client.currentScreen + " state=" + pearl.getController().getState());
            require(client.player.getInventory().getStack(12).getCount() == 8, "Hotbar operation touched inventory stack");
            require(!CombatLockManager.isLocked(), "Hotbar priority retained lock");
            System.out.println("INVENTORY_LIFECYCLE_SMOKE passed: optional return, leave PEARL, displaced item, multiple stacks, manual cancellation, disable, hotbar priority");
            VisualSmoke.writeResult(true, "mechanics + inventory lifecycle regression");
            client.scheduleStop();
        }
    }

    private static void setStack(MinecraftClient client, int slot, ItemStack stack) {
        client.player.getInventory().setStack(slot, stack);
        var uuid = client.player.getUuid();
        client.getServer().execute(() -> {
            var player = client.getServer().getPlayerManager().getPlayer(uuid);
            player.getInventory().setStack(slot, stack.copy());
            player.playerScreenHandler.sendContentUpdates();
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
