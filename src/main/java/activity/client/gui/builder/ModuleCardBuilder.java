package activity.client.gui.builder;

import activity.client.gui.ActivityScreen;
import activity.client.gui.ModuleSettingsView;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.ActivityTab;
import activity.client.module.api.IModule;

public final class ModuleCardBuilder {

    private ModuleCardBuilder() {}

    public static int buildCard(
            ActivityTab tab,
            ActivityScreen screen,
            ScrollContainer container,
            IModule module,
            int cardX,
            int cardY,
            int cardW,
            int innerRowW
    ) {
        return ModuleSettingsView.buildCard(tab, screen, container, module, cardX, cardY, cardW, innerRowW);
    }
}
