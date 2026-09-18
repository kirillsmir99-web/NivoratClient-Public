package activity.client.gui.builder;

import activity.client.gui.ActivityScreen;
import activity.client.gui.ModuleSettingsView;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.ActivityTab;
import activity.client.module.api.IModule;

/**
 * Universal Module Card Builder adhering to the Activity design system.
 * Delegates rendering directly to generic {@link ModuleSettingsView#buildCard}.
 */
public final class ModuleCardBuilder {

    private ModuleCardBuilder() {}

    /**
     * Builds and registers a complete UI card for the given module into the tab and container.
     *
     * @param tab       parent activity tab
     * @param screen    parent screen (for overlay and modal managers)
     * @param container scroll container receiving the widgets
     * @param module    module instance to render
     * @param cardX     X position of the card
     * @param cardY     Y position of the card
     * @param cardW     width of the card
     * @param innerRowW available width inside card padding
     * @return total calculated height of the card
     */
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
