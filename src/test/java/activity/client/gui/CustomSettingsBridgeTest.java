package activity.client.gui;

import activity.client.gui.custom.SettingsBridge;
import activity.client.module.api.NivoratModule;
import activity.client.module.api.ModuleCategory;
import activity.client.module.setting.*;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CustomSettingsBridgeTest {
    @Test void originalWidgetCallbacksChangeExistingSettingsOnly() {
        var module = new NivoratModule("bridge_test",Text.literal("Test"),Text.empty(),ModuleCategory.COMBAT) {};
        boolean[] flag={false};double[] value={2};int[] count={1};String[] mode={"a"},name={"before"};int[] actions={0};
        module.registerSetting(new BooleanSetting("flag",Text.literal("Flag"),Text.empty(),SettingGroup.GENERAL,false,()->flag[0],v->flag[0]=v));
        module.registerSetting(new NumberSetting("value",Text.literal("Value"),Text.empty(),SettingGroup.GENERAL,0,10,.5,"",false,2,()->value[0],v->value[0]=v));
        module.registerSetting(new IntegerSetting("count",Text.literal("Count"),Text.empty(),SettingGroup.GENERAL,1,5,1,NumberUnit.NONE,1,()->count[0],v->count[0]=v));
        module.registerSetting(new EnumSetting("mode",Text.literal("Mode"),Text.empty(),SettingGroup.GENERAL,List.of("a","b"),"a",()->mode[0],v->mode[0]=v));
        module.registerSetting(new StringSetting("name",Text.literal("Name"),Text.empty(),SettingGroup.GENERAL,"before",()->name[0],v->name[0]=v));
        module.registerSetting(new ActionSetting("action",Text.literal("Action"),Text.empty(),SettingGroup.GENERAL,()->actions[0]++));
        var models=SettingsBridge.models(module);
        assertEquals(module.getSettings().size(),models.size());
        ((activity.client.gui.custom.api.modules.settings.impl.BooleanSetting)models.get(0)).toggle();
        ((activity.client.gui.custom.api.modules.settings.impl.SliderSetting)models.get(1)).setValue(7.6f);
        ((activity.client.gui.custom.api.modules.settings.impl.SliderSetting)models.get(2)).setValue(99);
        ((activity.client.gui.custom.api.modules.settings.impl.SelectSetting)models.get(3)).setSelected("b");
        ((activity.client.gui.custom.api.modules.settings.impl.TextSetting)models.get(4)).setText("after");
        ((activity.client.gui.custom.api.modules.settings.impl.ButtonSetting)models.get(5)).click();
        assertTrue(flag[0]);assertEquals(7.5,value[0]);assertEquals(5,count[0]);assertEquals("b",mode[0]);assertEquals("after",name[0]);assertEquals(1,actions[0]);
    }
    @Test void bindsPreserveModifiersAndMouseEncoding() {
        var module=new NivoratModule("binding_test",Text.literal("Test"),Text.empty(),ModuleCategory.COMBAT) {};
        var key=new activity.client.module.keybind.Keybind(65,true,true,false);
        module.registerSetting(new KeybindSetting("key",Text.literal("Key"),Text.empty(),SettingGroup.GENERAL,key,()->key,value->key.copyFrom(value)));
        var model=(activity.client.gui.custom.api.modules.settings.impl.BindSetting)SettingsBridge.models(module).getFirst();
        model.setKey(66);assertEquals(66,key.getKeyCode());assertTrue(key.isCtrl());assertTrue(key.isShift());
        model.setKey(2);assertTrue(key.isMouseButton());assertEquals(2,key.getMouseButton());assertEquals(2,model.getKey());
        model.setKey(-1);assertTrue(key.isUnbound());
    }
    @Test void conditionalControlsRetainCurrentVisibility() {
        var module=new NivoratModule("visibility_test",Text.literal("Test"),Text.empty(),ModuleCategory.COMBAT) {};
        boolean[] visible={false};
        var source=new BooleanSetting("flag",Text.literal("Flag"),Text.empty(),SettingGroup.GENERAL,false,()->false,v->{});
        source.setVisibilityCondition(()->visible[0]);module.registerSetting(source);
        var model=SettingsBridge.models(module).getFirst();assertFalse(model.isVisible());visible[0]=true;assertTrue(model.isVisible());
    }
}
