package activity.client.gui;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.DetailedModuleSearch;
import activity.client.gui.custom.SettingsBridge;
import activity.client.gui.custom.VisualText;
import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.SettingGroup;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VisualPolishTest {
    @BeforeEach void prepare(){ActivityConfigManager.setConfig(new ActivityConfig());DetailedModuleSearch.invalidate();}
    private NivoratModule mace(){
        var source=new NivoratModule("auto_mace",Text.translatable("activity.module.auto_mace.name"),Text.translatable("activity.module.auto_mace.desc"),ModuleCategory.COMBAT) {
            {metadata=ModuleMetadata.builder("auto_mace").aliases("автобулава","automace","mace").build();}
        };
        source.registerSetting(new EnumSetting("source_mode",Text.translatable("activity.setting.combat.source_mode"),Text.translatable("activity.setting.combat.source_mode.desc"),SettingGroup.GENERAL,
            List.of("sword_and_axe","sword_only","axe_only"),"sword_and_axe",option->Text.translatable("activity.dropdown.source."+option),()->"sword_and_axe",value->{}));
        return source;
    }
    @Test void openWidgetsTranslateWithoutRebuildingOrChangingOptionIds(){
        var source=mace();var model=(activity.client.gui.custom.api.modules.settings.impl.SelectSetting)SettingsBridge.models(source).getFirst();
        ActivityConfigManager.getConfig().language="en";
        assertEquals("Sword and Axe",model.labelFor("sword_and_axe"));
        String en=model.getDisplayName();String enDescription=model.getDescription();
        ActivityConfigManager.getConfig().language="ru";
        assertEquals("Меч и топор",model.labelFor("sword_and_axe"));
        assertNotEquals(en,model.getDisplayName());assertNotEquals(enDescription,model.getDescription());
        assertEquals("sword_and_axe",model.getSelected());
    }
    @Test void bothLanguagesRemainSearchableWithAliasesSettingsWrongLayoutAndTypos(){
        ActivityConfigManager.getConfig().language="en";
        var wrapper=new activity.client.gui.custom.api.modules.Module(mace());var pool=List.of(wrapper);
        for(String query:List.of("автобулава","AutoMace","меч топор","sword axe","ьфсу","automcae","avtobulava"))
            assertEquals(pool,DetailedModuleSearch.search(pool,query),query);
        assertTrue(DetailedModuleSearch.search(pool,"nonexistent feature").isEmpty());
    }
    @Test void repeatedSearchUsesCachedResultsAndRegistryReplacementDoesNotLeaveGhosts(){
        var first=new activity.client.gui.custom.api.modules.Module(mace());var pool=List.of(first);
        var result=DetailedModuleSearch.search(pool,"mace");assertSame(result,DetailedModuleSearch.search(pool,"mace"));
        var replacement=new activity.client.gui.custom.api.modules.Module(mace());
        assertSame(replacement,DetailedModuleSearch.search(List.of(replacement),"mace").getFirst());
        assertTrue(DetailedModuleSearch.search(List.of(),"mace").isEmpty());
    }
    @Test void localizedTextPreservesArgumentsAndSiblingsWithoutMissingGlyphs(){
        assertEquals("Sword and Axe",VisualText.safe("Sword & Axe","en"));
        assertEquals("20.0 HP",VisualText.resolve(Text.literal("20.0").append(Text.literal(" HP")),"en"));
        assertEquals("Use the sword",VisualText.resolve(Text.translatableWithFallback("missing.key","Use the %s","sword"),"en"));
    }
    @Test void literalCooldownControlsHaveEnglishDescriptions(){
        var source=new activity.client.module.impl.utility.CooldownHudModule();
        assertEquals("Cooldown HUD",VisualText.moduleName(source,"en"));
        var setting=source.getSettings().getFirst();
        assertEquals("Vertical layout",VisualText.settingName(source,setting,"en"));
        assertEquals("Stack cooldown timers vertically.",VisualText.settingDescription(source,setting,"en"));
    }
    @Test void horizontalCooldownsWrapWithinTheViewport() {
        var layout=activity.client.gui.custom.hud.CooldownLayout.of(9,90,false,320);
        assertEquals(3,layout.columns());assertEquals(3,layout.rows());
        assertTrue(layout.width()<=320);
        var narrow=activity.client.gui.custom.hud.CooldownLayout.of(8,500,false,180);
        assertEquals(1,narrow.columns());assertTrue(narrow.width()<=180);
    }
    @Test void emptyCooldownListOnlyHasHeaderMetrics() {
        var layout=activity.client.gui.custom.hud.CooldownLayout.of(0,90,true,320);
        assertEquals(22,layout.height());assertEquals(0,layout.rows());
    }
    @Test void glassSectorHasNonzeroExtentForShaderCoordinatesAndBackdropCapture() {
        var sector=new activity.client.gui.custom.utils.render.render2d.radialglass.BuiltRadialGlass(
            100,100,56,148,1f,.8f,7,1,0xff202020,0xff111111,0,1,2,
            0xffffffff,.65f,false,.32f,.006f,3,0xffffffff,.1f,0);
        assertTrue(sector.extent()>=sector.outerRadius());
        assertEquals(56,sector.innerRadius());assertEquals(148,sector.outerRadius());
    }
    @Test void tooltipNearBottomRightFlipsAwayFromPointerAndStaysVisible() {
        var p=activity.client.gui.custom.TooltipPlacement.place(315,175,185,70,320,180);
        assertTrue(p.x()+p.width()<=312);assertTrue(p.y()+p.height()<=172);
        assertTrue(p.x()+p.width()<315);assertTrue(p.y()+p.height()<175);
    }
    @Test void tooltipNearTopLeftUsesFreeSpaceAndLimitsOversizedCards() {
        var p=activity.client.gui.custom.TooltipPlacement.place(1,1,185,70,320,180);
        assertEquals(15,p.x());assertEquals(15,p.y());
        var narrow=activity.client.gui.custom.TooltipPlacement.place(80,50,185,200,160,100);
        assertTrue(narrow.x()>=8&&narrow.y()>=8);assertTrue(narrow.x()+narrow.width()<=152);assertTrue(narrow.y()+narrow.height()<=92);
    }
}
