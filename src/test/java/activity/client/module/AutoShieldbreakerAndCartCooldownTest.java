package activity.client.module;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.config.preset.PresetSerializer;
import activity.client.gui.hud.ActivityHudOverlay;
import activity.client.module.api.BuiltinModules;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import activity.client.module.impl.combat.ShaderPassModule;
import activity.client.module.service.CartStateService;
import activity.client.module.setting.NumberSetting;
import dev.shader.ShaderPassConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoShieldbreakerAndCartCooldownTest {

    @BeforeAll
    static void initRegistry() {
        BuiltinModules.registerAll();
    }

    @BeforeEach
    void resetState() {
        ActivityConfigManager.resetDefaults();
    }

    @Test
    void testReactionDelaySettingProperties() {
        IModule mod = ModuleRegistry.get(ShaderPassModule.ID);
        assertNotNull(mod, "AutoShieldbreaker must be registered");

        NumberSetting reactionDelaySetting = (NumberSetting) mod.getSetting("reaction_delay");
        assertNotNull(reactionDelaySetting, "reaction_delay setting must exist");

        assertEquals(0.0, reactionDelaySetting.getMin(), 0.001);
        assertEquals(2.0, reactionDelaySetting.getMax(), 0.001);
        assertEquals(0.05, reactionDelaySetting.getStep(), 0.001);
        assertEquals(" s", reactionDelaySetting.getUnit());
        assertEquals(0.0, reactionDelaySetting.getDefaultValue(), 0.001);
        assertEquals(0.0, reactionDelaySetting.get(), 0.001);
    }

    @Test
    void testReactionDelaySyncAndClamp() {
        IModule mod = ModuleRegistry.get(ShaderPassModule.ID);
        assertNotNull(mod);

        NumberSetting reactionDelaySetting = (NumberSetting) mod.getSetting("reaction_delay");
        ActivityConfig config = ActivityConfigManager.getConfig();

        reactionDelaySetting.set(0.30);
        assertEquals(0.30, config.autoShieldbreakerReactionDelaySec, 0.001);
        assertEquals(0.30, ShaderPassConfig.reactionDelaySec, 0.001);

        config.autoShieldbreakerReactionDelaySec = 5.0;
        config.sanitize();
        assertEquals(2.0, config.autoShieldbreakerReactionDelaySec, 0.001);

        config.autoShieldbreakerReactionDelaySec = -1.0;
        config.sanitize();
        assertEquals(0.0, config.autoShieldbreakerReactionDelaySec, 0.001);
    }

    @Test
    void testPresetSerializerCopiesReactionDelay() {
        ActivityConfig src = new ActivityConfig();
        src.autoShieldbreakerReactionDelaySec = 0.45;

        ActivityConfig dst = new ActivityConfig();
        dst.autoShieldbreakerReactionDelaySec = 0.0;

        PresetSerializer.copySettings(src, dst);
        assertEquals(0.45, dst.autoShieldbreakerReactionDelaySec, 0.001);
    }

    @Test
    void testConfigEqualsAndHashCodeIncludesReactionDelay() {
        ActivityConfig c1 = new ActivityConfig();
        ActivityConfig c2 = new ActivityConfig();
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        c1.autoShieldbreakerReactionDelaySec = 0.75;
        assertNotEquals(c1, c2);
        assertNotEquals(c1.hashCode(), c2.hashCode());

        c2.autoShieldbreakerReactionDelaySec = 0.75;
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testCartStateServiceNullPlayerSafe() {
        assertEquals(-1, CartStateService.findHotbarCart(null));
        assertFalse(CartStateService.isCartOnCooldown(null));
    }

    @Test
    void testActivityHudOverlayConstants() {
        assertEquals("PulseHUD", ActivityHudOverlay.DEFAULT_TITLE);
    }
}
