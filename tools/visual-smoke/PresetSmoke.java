package activity.visualsmoke;

import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.LocalPresets;
import activity.client.gui.custom.PresetRenderer;
import net.minecraft.client.MinecraftClient;
import java.util.EnumSet;
import java.util.Set;

final class PresetSmoke {
    private static int ticks;
    private static java.nio.file.Path saved;
    private static PresetRenderer screen;
    private static activity.client.config.ActivityConfig originalConfig;
    static void tick(MinecraftClient client) throws Exception {
        ticks++;
        if (ticks == 1) {
            activity.client.gui.custom.api.localization.LocalizationManager.setLanguage("ru");
            var original = ActivityConfigManager.getConfig().copy();
            originalConfig = original;
            saved = LocalPresets.create("Проверка " + System.currentTimeMillis(), LocalPresets.Template.CURRENT,
                    EnumSet.of(LocalPresets.Part.MODULES, LocalPresets.Part.SOUNDS), Set.of("auto_mace"));
            var preview = LocalPresets.read(saved);
            require(preview.sections().getAsJsonObject("modules").getAsJsonObject("modules").size() == 1, "Module selection not isolated");
            require(preview.sections().getAsJsonObject("modules").getAsJsonObject("modules").has("auto_mace"), "Selected module missing");
            var modified = original.copy(); modified.autoMaceRestoreDelayMs = original.autoMaceRestoreDelayMs + 50;
            modified.hpReaperDiffX = 75; modified.autoMaceKeybind.copyFrom(new activity.client.module.keybind.Keybind(89));
            ActivityConfigManager.setConfig(modified);
            LocalPresets.apply(preview);
            require(ActivityConfigManager.getConfig().autoMaceRestoreDelayMs == original.autoMaceRestoreDelayMs, "Selected module was not restored");
            require(ActivityConfigManager.getConfig().hpReaperDiffX == 75, "Unselected HUD changed");
            require(ActivityConfigManager.getConfig().autoMaceKeybind.getKeyCode() == 89, "Unselected bind changed");
            ActivityConfigManager.setConfig(original);
            LocalPresets.toggleFavorite(saved);
            require(LocalPresets.favorites().contains(saved.getFileName().toString()), "Favorite not persisted");
            var exported = LocalPresets.export(saved);
            var imported = LocalPresets.importFile(exported);
            require(LocalPresets.read(imported).sections().equals(preview.sections()), "File import/export lost data");
            LocalPresets.delete(imported);
            require(!java.nio.file.Files.exists(imported), "Delete did not remove list entry");
            var ui = activity.client.gui.custom.api.ui.UI.INSTANCE; client.setScreen(ui);
            ui.selectCategoryFromWorkspace(activity.client.gui.custom.api.modules.Category.PRESETS);
            screen = ui.getPresetRenderer();
            require(client.currentScreen == ui, "Presets replaced the main screen");
        }
        if (ticks == 30) {
            capture(client, "presets-list-ru.png");
            var ui = activity.client.gui.custom.api.ui.UI.INSTANCE;
            require(screen.click(ui.panelX() + ui.contentXOff() + 20, ui.panelY() + 45, 0), "Create button not clickable");
        }
        if (ticks == 35) {
            var ui = activity.client.gui.custom.api.ui.UI.INSTANCE;
            require(screen.click(ui.panelX() + ui.contentXOff() + 20, ui.panelY() + 62, 0), "Name field not clickable");
            client.keyboard.setClipboard("Проверка ввода");
            require(ui.keyPressed(new net.minecraft.client.input.KeyInput(86, 0, 2)), "Name input did not accept paste");
        }
        if (ticks == 45) { capture(client, "presets-create-ru.png"); screen.scroll(-20); }
        if (ticks == 50) {
            var ui = activity.client.gui.custom.api.ui.UI.INSTANCE;
            screen.click(ui.panelX() + ui.contentXOff() + 20, ui.panelY() + 226, 0);
        }
        if (ticks == 60) capture(client, "presets-modules-ru.png");
        if (ticks == 70) {
            var changed = originalConfig.copy(); changed.autoMaceRestoreDelayMs += 50; ActivityConfigManager.setConfig(changed);
            require(!LocalPresets.changes(LocalPresets.read(saved)).isEmpty(), "Preview failed to show changed setting");
        }
        if (ticks == 90) capture(client, "presets-preview-ru.png");
        if (ticks == 100) {
            var model = dev.nivorat.arc.ArcMotionProfile.getInstance().exportProfile();
            dev.nivorat.arc.ArcMotionProfile.validateSharedProfile(model);
            var bundle = LocalPresets.capture("Профиль и темы", LocalPresets.Template.CURRENT, EnumSet.of(LocalPresets.Part.CART_PROFILE, LocalPresets.Part.THEMES));
            require(LocalPresets.parse(bundle.toString()).sections().size() == 2, "Profile/theme bundle invalid");
            LocalPresets.toggleFavorite(saved); LocalPresets.delete(saved);
            ActivityConfigManager.setConfig(originalConfig);
            client.setScreen(activity.client.gui.custom.api.ui.UI.INSTANCE);
            activity.client.gui.custom.CollectionDrawer.open(activity.client.gui.custom.NativeCollectionScreen.Kind.GG);
            require(client.currentScreen == activity.client.gui.custom.api.ui.UI.INSTANCE, "GG drawer replaced the main screen");
        }
        if (ticks == 130) capture(client, "gg-left-drawer-ru.png");
        if (ticks == 140) {
            activity.client.gui.custom.CollectionDrawer.close();
            VisualSmoke.writeResult(true, "Local preset selection, partial apply, file round trip, favorites, delete, theme/profile validation; preset list, creation, preview and GG drawer keep the main screen");
            client.scheduleStop();
        }
    }
    private static void capture(MinecraftClient client, String name) {
        var directory = new java.io.File(System.getProperty("visual.smoke.output")); directory.mkdirs();
        net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(directory, name, client.getFramebuffer(), 1, text -> {});
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
