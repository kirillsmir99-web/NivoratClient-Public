package activity.visualsmoke;

import activity.client.gui.custom.NativeBindAssignment;
import activity.client.gui.custom.NativeCollectionScreen;
import activity.client.gui.custom.api.ui.UI;
import activity.client.gui.custom.api.ui.settings.impl.BindSetting;
import activity.client.gui.custom.api.ui.settings.impl.SelectSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import java.util.List;
import java.io.File;

final class RevisionSmoke {
    private static int tick;
    private static BindSetting binding;
    private static boolean dropdown;
    private static List<?> widgets() throws Exception {
        var field = UI.INSTANCE.getInspector().getClass().getDeclaredField("widgets"); field.setAccessible(true);
        return (List<?>) field.get(UI.INSTANCE.getInspector());
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static void capture(MinecraftClient client, String name) {
        File output = new File(System.getProperty("visual.smoke.output")); output.mkdirs();
        net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(output, name, client.getFramebuffer(), 1, text -> System.out.println("REVISION_CAPTURE " + text.getString()));
    }
    static void tick(MinecraftClient client) throws Exception {
        if (!client.player.isAlive()) { client.player.requestRespawn(); return; }
        tick++;
        if (tick == 1) {
            client.getServer().submit(() -> {
                client.getServer().getPlayerManager().getPlayer(client.player.getUuid()).changeGameMode(net.minecraft.world.GameMode.CREATIVE);
                for (var entity : client.getServer().getOverworld().iterateEntities()) if (entity instanceof net.minecraft.entity.mob.HostileEntity) entity.discard();
                return true;
            }).join();
            for (var module : activity.client.module.api.ModuleRegistry.getAll()) module.setEnabled(false);
            client.setScreen(UI.INSTANCE); UI.INSTANCE.setSelectedTab(0); UI.INSTANCE.openModuleInspector("click_pearl");
        }
        if (tick == 30) {
            binding = (BindSetting) widgets().stream().filter(s -> s instanceof BindSetting).findFirst().orElseThrow();
            for (int key : new int[]{280, 341, 340, 71}) {
                binding.setListening(true); UI.INSTANCE.keyPressed(new KeyInput(key, 0, key == 340 ? 1 : key == 341 ? 2 : 0));
                require(!binding.isListening(), "Binding capture did not finish: " + key);
                if (NativeBindAssignment.isOpen()) NativeBindAssignment.confirm();
                require(binding.backend().source().get().getKeyCode() == key, "Wrong key after capture: " + key);
            }
            binding.setListening(true); UI.INSTANCE.mouseClicked(new Click(0, 0, new MouseInput(4, 0)), false);
            if (NativeBindAssignment.isOpen()) NativeBindAssignment.confirm();
            require(binding.backend().source().get().getMouseButton() == 4, "Side mouse button was not captured");
            var spear = (activity.client.module.setting.KeybindSetting) activity.client.module.api.ModuleRegistry.get("auto_spear").getSetting("trigger_keybind");
            spear.get().set(258, false, false, false);
            binding.setListening(true); UI.INSTANCE.keyPressed(new KeyInput(258, 0, 0));
            require(NativeBindAssignment.isOpen(), "Duplicate TAB did not request confirmation");
        }
        if (tick == 50) { capture(client, "revision-01-bind-conflict.png"); NativeBindAssignment.cancel(); UI.INSTANCE.openModuleInspector("auto_mace"); }
        if (tick == 75) {
            for (Object widget : widgets()) if (widget instanceof SelectSetting select) {
                var field = SelectSetting.class.getDeclaredField("backend"); field.setAccessible(true);
                var backend = (activity.client.gui.custom.api.modules.settings.impl.SelectSetting) field.get(select);
                if (backend.getOptions().size() < 2) continue;
                var open = SelectSetting.class.getDeclaredField("open"); open.setAccessible(true); open.setBoolean(select, true);
                var anim = SelectSetting.class.getDeclaredField("dropAnim"); anim.setAccessible(true);
                var animation = (activity.client.gui.custom.utils.animations.Decelerate) anim.get(select);
                animation.setDirection(activity.client.gui.custom.utils.animations.Direction.FORWARDS); animation.counter.setTime(System.currentTimeMillis() - 1000);
                dropdown = true; break;
            }
            require(dropdown, "No dropdown to verify");
        }
        if (tick == 100) {
            capture(client, "revision-02-dropdown.png");
            var cfg = activity.client.config.ActivityConfigManager.getConfig(); cfg.hpReaperShowArmor = true; cfg.hpReaperMode = "own_hp";
            activity.client.module.api.ModuleRegistry.get("hp_reaper").setEnabled(true);
            dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.OWN_HEALTH;
            client.player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            client.player.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            client.setScreen(new dev.hpreaper.HpHudEditorScreen(null));
        }
        if (tick == 125) { capture(client, "revision-03-hp-editor.png"); client.setScreen(new NativeCollectionScreen(null, NativeCollectionScreen.Kind.GG)); }
        if (tick == 150) {
            capture(client, "revision-04-gg-phrases.png");
            ItemStack stack = new ItemStack(Items.MACE);
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Меч дракона"));
            var group = net.minecraft.util.Identifier.of("server", "dragon_weapon");
            stack.set(DataComponentTypes.USE_COOLDOWN, new net.minecraft.component.type.UseCooldownComponent(4f, java.util.Optional.of(group)));
            client.player.getInventory().setStack(0, stack);
            if (!activity.client.gui.custom.CooldownSelections.values().contains("name:Меч дракона")) activity.client.gui.custom.CooldownSelections.toggle("name:Меч дракона");
            client.player.getItemCooldownManager().set(stack, 100);
            var entry = activity.client.module.service.CooldownTrackerService.getActiveEntries().stream().filter(e -> group.equals(e.key)).findFirst().orElseThrow();
            require(entry.iconStack.contains(DataComponentTypes.CUSTOM_NAME), "Renamed stack components were lost");
            require(activity.client.gui.custom.hud.CooldownListRenderer.itemName(entry).equals("Меч дракона"), "Renamed cooldown label was lost");
            client.setScreen(new NativeCollectionScreen(null, NativeCollectionScreen.Kind.COOLDOWN));
        }
        if (tick == 175) {
            capture(client, "revision-05-cooldown-items.png");
            var theme = activity.client.gui.custom.api.ui.theme.Theme.NIVORA;
            if (!activity.client.gui.custom.api.ui.theme.ThemePins.isPinned(theme)) activity.client.gui.custom.api.ui.theme.ThemePins.toggle(theme);
            client.setScreen(UI.INSTANCE); UI.INSTANCE.setSelectedTab(16);
            require(UI.INSTANCE.getThemesRenderer().getAllThemes().getFirst() == theme, "Pinned theme is not ordered first");
            activity.client.gui.custom.VisualSettingsStore.save();
        }
        if (tick == 200) {
            capture(client, "revision-06-pinned-themes.png");
            client.setScreen(null);
            client.player.setHealth(6f);
            client.getServer().execute(() -> client.getServer().getPlayerManager().getPlayer(client.player.getUuid()).setHealth(6f));
        }
        if (tick == 225) {
            capture(client, "revision-07-health-hearts.png");
            System.out.println("REVISION_SMOKE passed: live bind routing, special keys, side mouse, conflict confirmation, dropdown, custom HP editor, GG list, named cooldown groups, pinned theme");
            client.setScreen(new dev.hpreaper.HpHudEditorScreen(null));
            dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.OWN_HEALTH;
            dev.hpreaper.VitalityConfig.setModePos(dev.hpreaper.HealthHudOverlay.DisplayMode.OWN_HEALTH, 320, 190);
        }
        if (tick == 235) {
            var screen = client.currentScreen;
            float scale = activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace.guiIndependentScale();
            screen.mouseClicked(new Click(20 * scale, 30 * scale, new MouseInput(0, 0)), false);
            require(screen.isDragging(), "HUD editor heading did not enable mouse drag routing");
            screen.mouseDragged(new Click(215 * scale, 72 * scale, new MouseInput(0, 0)), 195 * scale, 42 * scale);
            screen.mouseReleased(new Click(215 * scale, 72 * scale, new MouseInput(0, 0)));
            screen.mouseClicked(new Click(325 * scale, 195 * scale, new MouseInput(0, 0)), false);
            require(screen.isDragging(), "HUD indicator did not enable mouse drag routing");
            screen.mouseDragged(new Click(355 * scale, 215 * scale, new MouseInput(0, 0)), 30 * scale, 20 * scale);
            screen.mouseReleased(new Click(355 * scale, 215 * scale, new MouseInput(0, 0)));
            require(dev.hpreaper.VitalityConfig.ownHealthX == 350 && dev.hpreaper.VitalityConfig.ownHealthY == 210, "HP drag did not update native coordinates");
        }
        if (tick == 240) capture(client, "revision-08-hp-compact-dragged.png");
        if (tick == 242) dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.CROSSHAIR_AND_TARGET;
        if (tick == 245) capture(client, "revision-09-hp-own-target.png");
        if (tick == 247) dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.TARGET_HEALTH;
        if (tick == 250) capture(client, "revision-10-hp-target-only.png");
        if (tick == 252) dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE;
        if (tick == 255) capture(client, "revision-11-hp-difference.png");
        if (tick == 257) dev.hpreaper.VitalityConfig.displayMode = dev.hpreaper.HealthHudOverlay.DisplayMode.DISABLED;
        if (tick == 260) {
            capture(client, "revision-12-hp-disabled.png");
            client.setScreen(new dev.carthud.CartHudEditorScreen(null));
            dev.carthud.CartHudConfig.customX = 315; dev.carthud.CartHudConfig.customY = 200;
        }
        if (tick == 265) {
            var screen = client.currentScreen;
            float scale = activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace.guiIndependentScale();
            screen.mouseClicked(new Click(320 * scale, 207 * scale, new MouseInput(0, 0)), false);
            require(screen.isDragging(), "Cart HUD did not enable mouse drag routing");
            screen.mouseDragged(new Click(350 * scale, 227 * scale, new MouseInput(0, 0)), 30 * scale, 20 * scale);
            screen.mouseReleased(new Click(350 * scale, 227 * scale, new MouseInput(0, 0)));
            require(dev.carthud.CartHudConfig.customX == 345 && dev.carthud.CartHudConfig.customY == 220, "Cart HUD drag coordinates were wrong");
        }
        if (tick == 275) {
            capture(client, "revision-13-cart-native-editor.png");
            System.out.println("HUD_EDITOR_SMOKE passed: native panel and HUD drag routing, HP modes, disabled preview, frameless Cart HUD");
            VisualSmoke.writeResult(true, "revision GUI + native draggable HUD editors + HP modes");
            client.scheduleStop();
        }
    }
}
