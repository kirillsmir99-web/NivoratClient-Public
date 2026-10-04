# ==============================================================================
# ProGuard Configuration for NivoratClient (Minecraft Fabric 1.21.11, Java 21)
# ==============================================================================

-dontshrink
-dontoptimize
-repackageclasses 'activity.client.internal'

# Strip local variable names, parameter names, and original source filenames
-renamesourcefileattribute ""
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod

# Ignore unresolved library references from Minecraft / Fabric Loom runtime
-dontwarn **
-dontnote **
-ignorewarnings

# ------------------------------------------------------------------------------
# TIER 0: FABRIC ENTRYPOINTS & API CONTRACTS (Strict ABI Preservation)
# ------------------------------------------------------------------------------
-keep public class activity.client.MemoryLeakFixClient {
    public *;
}

-keep public class activity.client.CooldownHudClient {
    public *;
}

-keep class activity.client.util.ModRenderContext {
    public static *;
}

-keep class activity.client.util.PacketSanitizer {
    public static *;
}

-keep public class activity.client.integration.ClientIntegrationProvider {
    public *;
}

# ------------------------------------------------------------------------------
# TIER 0: MIXIN INJECTION CLASSES (All 5 Configs)
# ------------------------------------------------------------------------------
-keep class activity.client.mixin.pipeline.** {
    *;
}

-keep class activity.client.mixin.audio.** {
    *;
}

-keep class activity.client.mixin.dev.** {
    *;
}

-keep class activity.client.mixin.cooldown.** {
    *;
}

-keep class activity.client.gui.custom.mixin.** {
    *;
}

# ------------------------------------------------------------------------------
# TIER 0: REFLECTIVE SETTING FIELDS (VisualSettingsStore)
# ------------------------------------------------------------------------------
-keepclassmembers class activity.client.gui.custom.VisualMaterial {
    public activity.client.gui.custom.api.modules.settings.Setting *;
}

-keepclassmembers class activity.client.gui.custom.api.modules.impl.Interface.ClickGui {
    public activity.client.gui.custom.api.modules.settings.Setting *;
}

-keepclassmembers class activity.client.gui.custom.api.modules.impl.Utils.ClientSounds {
    public activity.client.gui.custom.api.modules.settings.Setting *;
}

-keepclassmembers class activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule {
    public activity.client.gui.custom.api.modules.settings.Setting *;
}

# ------------------------------------------------------------------------------
# TIER 0: PUBLIC ECOSYSTEM & MODULE APIs
# ------------------------------------------------------------------------------
-keep public interface activity.client.module.api.IModule {
    public *;
}

-keep public class activity.client.module.api.CooldownModule {
    public *;
}

-keep public class activity.client.module.api.NivoratModule {
    public *;
}

# ------------------------------------------------------------------------------
# TIER 0 / 1: GUI REFLECTIVE & SMOKE TEST ENTRYPOINTS
# ------------------------------------------------------------------------------
-keep class activity.client.gui.custom.VisualMaterial {
    public *;
}
-keep class activity.client.gui.custom.api.modules.impl.Interface.ClickGui {
    public *;
}
-keep class activity.client.gui.custom.api.modules.impl.Utils.ClientSounds {
    public *;
}
-keep class activity.client.gui.custom.api.modules.impl.Interface.WatermarkModule {
    public *;
}

-keep class activity.client.gui.custom.api.modules.** {
    public *;
}
-keep class activity.client.gui.custom.api.ui.inspector.** {
    public *;
    private *;
}
-keep class activity.client.gui.custom.api.ui.theme.** {
    public *;
}
-keep class activity.client.gui.custom.api.ui.module.** {
    public *;
}

-keep class activity.client.gui.ActivityScreen {
    public static void clearSession();
}
-keep class activity.client.gui.custom.api.ui.UI** {
    public static activity.client.gui.custom.api.ui.UI INSTANCE;
    public *;
    private *;
}
-keep class activity.client.gui.custom.PresetRenderer {
    public *;
}
-keep class activity.client.gui.custom.NativeCollectionScreen {
    public *;
}
-keep class activity.client.gui.custom.NativeBindAssignment {
    public *;
}
-keep class activity.client.gui.tab.ThemesTab {
    public *;
}
-keep class activity.client.gui.navigation.PvpKit {
    public *;
}
-keep class activity.client.gui.custom.CollectionDrawer {
    public *;
}
-keep class activity.client.gui.custom.CooldownSelections {
    public *;
}
-keep class activity.client.gui.custom.CustomRender {
    public *;
}
-keep class activity.client.gui.custom.DetailedModuleSearch {
    public *;
}
-keep class activity.client.gui.custom.NativeTooltip {
    public *;
    private *;
}
-keep class activity.client.gui.custom.VisualSettingsStore {
    public *;
}
-keep class activity.client.gui.custom.api.drags.Position {
    public *;
}
-keep class activity.client.gui.custom.api.localization.LocalizationManager {
    public *;
}
-keep class activity.client.gui.custom.api.modules.Category {
    public *;
}
-keep class activity.client.gui.custom.api.modules.ModuleManager {
    public *;
}
-keep class activity.client.gui.custom.api.ui.module.ModuleListRenderer {
    public *;
}
-keep class activity.client.gui.custom.api.ui.theme.Theme {
    public *;
}
-keep class activity.client.gui.custom.api.ui.theme.ThemePins {
    public *;
}
-keep class activity.client.gui.custom.hud.CooldownListRenderer {
    public *;
}
-keep class activity.client.gui.custom.utils.animations.Decelerate {
    public *;
}
-keep class activity.client.gui.custom.utils.animations.Direction {
    public *;
}
-keep class activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace {
    public *;
}
-keep class activity.client.gui.hud.CooldownHudOverlay {
    public *;
}
-keep class activity.client.gui.overlay.ClientNotification {
    public *;
}
-keep class activity.client.gui.theme.ThemePreset {
    public *;
}

-keep public class activity.client.module.service.InventoryScanService { public *; }
-keep public class activity.client.config.ActivityConfigManager { public *; }
-keep public class activity.client.config.ActivityConfig { public *; }
-keep public class activity.client.config.preset.** { public *; }
-keep class activity.client.module.impl.utility.gui.AudioWaveRadialScreen { public *; }
-keep class dev.audio.AudioSyncClient { public *; }
-keep class dev.audio.AudioSyncConfig { public *; }
-keep class dev.carthud.** { public *; }
-keep class dev.hpreaper.** { public *; }

# ------------------------------------------------------------------------------
# GSON SERIALIZATION SAFETY
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------------------
# TIER 2 & TIER 3: AGGRESSIVE OBFUSCATION
# All remaining classes (dev.nivorat.arc.*, dev.mace.*, dev.raycast.*,
# CapitulationManager, SafeSlotManager, combat modules, timing engines)
# will have classes, methods, and fields renamed to a, b, c, etc.
# ------------------------------------------------------------------------------
