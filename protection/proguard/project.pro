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
-keep public class activity.client.CooldownHudClient {
    public *;
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
# TIER 0 / 1: GUI & CONFIG RUNTIME ENTRYPOINTS
# ------------------------------------------------------------------------------
-keep public class activity.client.gui.** { public *; }
-keep public class activity.client.gui.custom.** { public *; }
-keep public class activity.client.module.impl.utility.gui.** { public *; }
-keep public class activity.client.module.service.InventoryScanService { public *; }
-keep public class activity.client.config.ActivityConfigManager { public *; }
-keep public class activity.client.config.ActivityConfig { public *; }
-keep public class activity.client.config.preset.** { public *; }
-keep public class dev.audio.** { public *; }
-keep public class dev.hpreaper.** { public *; }
-keep public class dev.carthud.** { public *; }

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
