package activity.visualsmoke;

import activity.client.gui.ActivityScreen;
import activity.client.gui.navigation.PvpKit;
import activity.client.gui.tab.ThemesTab;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import java.io.File;

public final class VisualSmoke implements ClientModInitializer {
    static void writeResult(boolean success, String detail) {
        String path = System.getProperty("visual.smoke.result");
        if (path == null) return;
        try {java.nio.file.Files.writeString(java.nio.file.Path.of(path), (success ? "PASSED " : "FAILED ") + detail);}
        catch (java.io.IOException ex) {throw new IllegalStateException(ex);}
    }
    private int ticks;
    private int stage;
    private activity.client.gui.custom.api.ui.UI screen;
    private boolean worldStarted;
    private boolean previewHud;
    private boolean verticalHud=true;
    private java.util.List<String> originalPhrases;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void verifyNativeText(){
        var config=activity.client.config.ActivityConfigManager.getConfig();
        var manager=activity.client.gui.custom.api.modules.ModuleManager.get();
        var mace=manager.findByName("auto_mace");
        for(String lang:java.util.List.of("en","ru")){
            activity.client.gui.custom.api.localization.LocalizationManager.setLanguage(lang);
            for(var module:manager.getAll()){
                require(!module.getDisplayName().startsWith("activity."),"Untranslated module "+module.getName());
                for(var setting:module.getSettings()){
                    require(!setting.getDisplayName().startsWith("activity."),"Untranslated setting "+setting.getName());
                    if(setting instanceof activity.client.gui.custom.api.modules.settings.impl.SelectSetting select)for(String option:select.getOptions())require(!select.labelFor(option).contains("&"),"Missing ampersand glyph "+option);
                }
            }
        }
        activity.client.gui.custom.api.localization.LocalizationManager.setLanguage("en");
        require(mace.getSettings().getFirst().getDisplayName().equals("Source Weapon"),"Wrong EN setting label");
        System.out.println("VISUAL_SMOKE EN mace: "+mace.getDisplayName()+" / "+mace.getSettings().getFirst().getDisplayName());
        for(String q:java.util.List.of("автобулава","mace","булава задержка","sword and axe","ьфсу","automcae")){
            var result=activity.client.gui.custom.DetailedModuleSearch.search(q);
            require(result.stream().anyMatch(m->m.getName().equals("auto_mace")),"Search failed: "+q);
        }
        var sourceSetting=(activity.client.gui.custom.api.modules.settings.impl.SelectSetting)mace.getSettings().getFirst();
        require(sourceSetting.labelFor("sword_and_axe").equals("Sword and Axe"),"Wrong EN option");
        activity.client.gui.custom.api.localization.LocalizationManager.setLanguage("ru");
        require(sourceSetting.labelFor("sword_and_axe").equals("Меч и топор"),"Stale option after language switch");
        require(!mace.getSettings().getFirst().getDisplayName().equals("Weapon in hand"),"Stale setting label");
    }
    private static void verifyInventory(net.minecraft.client.MinecraftClient client) {
        if(client.player==null)return;
        var inventory=client.player.getInventory();
        var original=new java.util.ArrayList<net.minecraft.item.ItemStack>();
        for(int i=0;i<9;i++){original.add(inventory.getStack(i).copy());inventory.setStack(i,net.minecraft.item.ItemStack.EMPTY);}
        try {
            var items=java.util.List.of(net.minecraft.item.Items.DIAMOND_SWORD,net.minecraft.item.Items.DIAMOND_AXE,net.minecraft.item.Items.MACE,net.minecraft.item.Items.TRIDENT,net.minecraft.item.Items.TOTEM_OF_UNDYING,net.minecraft.item.Items.GLOWSTONE);
            for(int i=0;i<items.size();i++)inventory.setStack(i,new net.minecraft.item.ItemStack(items.get(i)));
            activity.client.module.service.InventoryScanService.invalidate();
            require(activity.client.module.service.InventoryScanService.findSwordSlot(client.player)==0,"Sword scan");
            require(activity.client.module.service.InventoryScanService.findAxeSlot(client.player)==1,"Axe scan");
            require(activity.client.module.service.InventoryScanService.findMaceSlot(client.player)==2,"Mace scan");
            require(activity.client.module.service.InventoryScanService.findSpearSlot(client.player)==3,"Spear scan");
            require(activity.client.module.service.InventoryScanService.findHotbarTotemSlot(client.player)==4,"Totem scan");
            require(activity.client.module.service.InventoryScanService.findGlowstoneSlot(client.player)==5,"Glowstone scan");
            inventory.setStack(0,net.minecraft.item.ItemStack.EMPTY);
            activity.client.module.service.InventoryScanService.invalidate();
            require(activity.client.module.service.InventoryScanService.findSwordSlot(client.player)==-1,"Stale sword scan");
            System.out.println("INVENTORY_SMOKE six item scans and mutation invalidation passed");
        } finally {
            for(int i=0;i<9;i++)inventory.setStack(i,original.get(i));
            activity.client.module.service.InventoryScanService.invalidate();
        }
    }
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((context,tick)->{
            if(!previewHud)return;
            var client=net.minecraft.client.MinecraftClient.getInstance();
            dev.hpreaper.HealthHudOverlay.renderPreview(context,client,12,12,dev.hpreaper.HealthHudOverlay.DisplayMode.OWN_TARGET_AND_DIFFERENCE);
            dev.carthud.CartHudOverlay.renderElement(context,client,12,44,12);
            activity.client.gui.hud.CooldownHudOverlay.renderCooldownList(context,client.textRenderer,activity.client.gui.hud.CooldownHudOverlay.getMockEntriesForPreview(),12,76,verticalHud);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (stage == 0) {
                    if (client.getOverlay() != null || (client.currentScreen == null && client.world == null)) return;
                    if (++ticks < (Boolean.getBoolean("visual.smoke.lowfps") ? 120 : 40)) return;
                    if (Boolean.getBoolean("visual.smoke.world") && client.world == null) {
                        if (!worldStarted) {
                            worldStarted = true;
                            client.options.getViewDistance().setValue(4);
                            client.options.getSimulationDistance().setValue(5);
                            var loader = client.createIntegratedServerLoader();
                            if (new File("saves/RenderSmokeWorld/level.dat").isFile()) loader.start("RenderSmokeWorld", () -> client.scheduleStop());
                            else loader.createAndStart("RenderSmokeWorld",net.minecraft.server.MinecraftServer.DEMO_LEVEL_INFO.withGameMode(net.minecraft.world.GameMode.CREATIVE),net.minecraft.world.gen.GeneratorOptions.DEMO_OPTIONS,net.minecraft.world.gen.WorldPresets::createDemoOptions,new TitleScreen());
                        }
                        return;
                    }
                    if(Boolean.getBoolean("visual.smoke.release")) {
                        try(var marker=VisualSmoke.class.getResourceAsStream("/cooldownhud-edition.txt")) {
                            require(marker!=null&&new String(marker.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim().equals("public"),"Expected PUBLIC artifact");
                        }
                    }
                    if(Boolean.getBoolean("visual.smoke.spoofer")){
                        require(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("clientspoofer"),"Nested ClientSpoofer did not load");
                        require("vanilla".equals(net.minecraft.client.ClientBrandRetriever.getClientModName()),"Brand was not spoofed");
                        System.out.println("CLIENTSPOOFER_SMOKE loaded 1.4.0; brand="+net.minecraft.client.ClientBrandRetriever.getClientModName());
                    }
                    if(Boolean.getBoolean("visual.smoke.lowfps"))client.options.getMaxFps().setValue(10);
                    if(Boolean.getBoolean("visual.smoke.revision")) {client.setScreen(null); stage=501; ticks=0; return;}
                    if(Boolean.getBoolean("visual.smoke.pearlCatch")) {client.setScreen(null); stage=502; ticks=0; return;}
                    if(Boolean.getBoolean("visual.smoke.mechanics")) {client.setScreen(null); stage=500; ticks=0; return;}
                    verifyInventory(client);verifyNativeText();screen = activity.client.gui.custom.api.ui.UI.INSTANCE;
                    client.setScreen(screen);
                    screen.setSelectedTab(PvpKit.ALL.tabIndex());
                    stage = Boolean.getBoolean("visual.smoke.tooltip") ? 21 : 1;
                    ticks = 0;
                    return;
                }
                if(stage==500) {MechanicsSmoke.tick(client);return;}
                if(stage==501) {RevisionSmoke.tick(client);return;}
                if(stage==502) {PearlCatchSmoke.tick(client);return;}
                if(stage==21||stage==22){
                    var field=screen.getClass().getDeclaredField("moduleList");field.setAccessible(true);
                    var renderer=(activity.client.gui.custom.api.ui.module.ModuleListRenderer)field.get(screen);
                    var modules=activity.client.gui.custom.api.modules.ModuleManager.get().getAll();
                    var ui=activity.client.gui.custom.api.ui.UI.INSTANCE;
                    float[] rect=renderer.cardRect(modules,stage==21?1:10,
                        activity.client.gui.custom.api.ui.UI.panelX()+activity.client.gui.custom.api.ui.UI.contentXOff(),
                        activity.client.gui.custom.api.ui.UI.panelY()+31f,
                        activity.client.gui.custom.api.ui.UI.PANEL_W-activity.client.gui.custom.api.ui.UI.contentInset());
                    if(rect!=null){
                        var window=client.getWindow();
                        float scale=activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace.designGuiScale();
                        double x=(rect[0]+rect[2]*.55f)*scale*window.getWidth()/window.getFramebufferWidth();
                        double y=(rect[1]+rect[3]*.55f)*scale*window.getHeight()/window.getFramebufferHeight();
                        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getHandle(),x,y);
                        for(String axis:java.util.List.of("x","y")){var coordinate=client.mouse.getClass().getDeclaredField(axis);coordinate.setAccessible(true);coordinate.setDouble(client.mouse,axis.equals("x")?x:y);}
                        if(ticks==119||ticks==39)System.out.println("TOOLTIP_POSITION rect="+java.util.Arrays.toString(rect)+" mouse="+activity.client.gui.custom.api.drags.Position.mouseX()+","+activity.client.gui.custom.api.drags.Position.mouseY()+" capture="+activity.client.gui.custom.api.ui.UI.guiCaptureActive());
                    }
                }
                if (++ticks < (Boolean.getBoolean("visual.smoke.lowfps") ? 120 : 40)) return;
                ticks = 0;
                File output = new File(System.getProperty("visual.smoke.output"));
                output.mkdirs();
                String[] names = {"", "01-all.png", "02-mace.png", "03-inspector.png", "04-themes.png", "05-nivora.png", "06-settings.png", "07-theme-editor.png", "08-theme-editor-open.png", "09-about.png", "10-clickgui.png", "11-search.png", "12-sidebar.png", "13-chat.png", "14-ru-mace.png", "15-en-mace.png", "16-native-gg-wheel.png", "17-native-huds.png", "18-cooldown-grid.png", "19-eight-phrase-wheel.png", "20-three-phrase-wheel.png", "21-tooltip-top-layer.png", "22-tooltip-edge.png"};
                if(stage==21||stage==22){var cached=activity.client.gui.custom.NativeTooltip.class.getDeclaredField("cachedDescription");cached.setAccessible(true);require(cached.get(null)!=null,"Tooltip did not draw for stage "+stage);System.out.println("TOOLTIP_RENDERED "+cached.get(null));}
                if (stage <= 22) ScreenshotRecorder.saveScreenshot(output, names[stage], client.getFramebuffer(), 1,
                    result -> System.out.println("VISUAL_SMOKE screenshot: " + result.getString()));
                switch (stage++) {
                    case 1 -> screen.selectCategoryFromWorkspace(activity.client.gui.custom.api.modules.Category.MACE);
                    case 2 -> screen.openModuleInspector("auto_mace");
                    case 3 -> { screen.getInspector().close(); screen.setSelectedTab(ThemesTab.TAB_INDEX); }
                    case 4 -> {
                        activity.client.gui.custom.CustomRender.theme(activity.client.gui.theme.ThemePreset.NIVORA);
                        activity.client.config.ActivityConfigManager.getConfig().guiTheme="nivora";
                        screen.setSelectedTab(PvpKit.ALL.tabIndex());
                    }
                    case 5 -> { activity.client.config.ActivityConfigManager.getConfig().guiTheme="client";screen.setSelectedTab(4); }
                    case 6 -> screen.setSelectedTab(16);
                    case 7 -> screen.getThemesRenderer().getEditor().openNew();
                    case 8 -> {screen.getThemesRenderer().getEditor().close();screen.setSelectedTab(5);}
                    case 9 -> {screen.setSelectedTab(4);screen.openModuleInspector("ClickGui");}
                    case 10 -> {screen.getInspector().close();screen.setSelectedTab(0);screen.setSearchText("mace");}
                    case 11 -> {screen.setSearchText("");screen.customSidebarW=38f;}
                    case 12 -> {screen.close();for(int i=0;i<5;i++)activity.client.gui.overlay.ClientNotification.show(net.minecraft.text.Text.literal("Notification duplicate check: this long message must wrap within a compact glass card and never cover the hotbar."));client.setScreen(new net.minecraft.client.gui.screen.ChatScreen("",false));client.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal("NC: проверка анимации чата"));}
                    case 13 -> {client.setScreen(screen);screen.customSidebarW=90f;screen.openModuleInspector("auto_mace");}
                    case 14 -> {activity.client.gui.custom.api.localization.LocalizationManager.setLanguage("en");}
                    case 15 -> {activity.client.gui.custom.CustomRender.theme(activity.client.gui.theme.ThemePreset.CLIENT);client.setScreen(new activity.client.module.impl.utility.gui.AudioWaveRadialScreen(null));}
                    case 16 -> {client.setScreen(null);previewHud=true;}
                    case 17 -> {verticalHud=false;client.options.getGuiScale().setValue(4);}
                    case 18 -> {previewHud=false;originalPhrases=java.util.List.copyOf(dev.audio.AudioSyncClient.CONFIG.phrases);dev.audio.AudioSyncClient.CONFIG.phrases=new java.util.ArrayList<>(java.util.List.of("GGWP","Good Fight","GG","WP","Well Played","Nice Match","GF","Thanks"));client.setScreen(new activity.client.module.impl.utility.gui.AudioWaveRadialScreen(null));}
                    case 19 -> {dev.audio.AudioSyncClient.CONFIG.phrases=new java.util.ArrayList<>(originalPhrases);client.setScreen(new activity.client.module.impl.utility.gui.AudioWaveRadialScreen(null));}
                    case 20 -> {System.out.println("VISUAL_SMOKE completed: hud layouts, viewport bounds, three/eight phrase wheels, live RU/EN, source="+activity.client.gui.custom.api.ui.UI.class.getProtectionDomain().getCodeSource().getLocation()+", world="+(client.world!=null)+", all, mace, original inspector, themes, theme change, settings, theme editor, about, clickgui, search, sidebar, chat");writeResult(true,"visual");screen.close();client.scheduleStop(); }
                    case 21 -> {}
                    case 22 -> {System.out.println("VISUAL_TOOLTIP_SMOKE completed: real card hover, foreground layer, viewport placement, new icons, source="+screen.getClass().getProtectionDomain().getCodeSource().getLocation());writeResult(true,"tooltip");screen.close();}
                    default -> client.scheduleStop();
                }
            } catch (Throwable error) {
                writeResult(false, error.toString());
                stage=10000;System.err.println("VISUAL_SMOKE FAILED");
                error.printStackTrace();
                client.scheduleStop();
            }
        });
    }
}
