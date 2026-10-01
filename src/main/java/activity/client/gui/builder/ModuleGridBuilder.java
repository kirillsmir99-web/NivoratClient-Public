package activity.client.gui.builder;

import activity.client.gui.ActivityScreen;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.tab.ActivityTab;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import java.util.List;

public final class ModuleGridBuilder {
    private ModuleGridBuilder() { }
    public static int build(ActivityTab tab, ActivityScreen screen, ScrollContainer container,
                            List<IModule> modules, int x, int y, int width) {
        boolean twoColumns = width >= ActivityMetrics.RESPONSIVE_TWO_COLUMN_BREAKPOINT;
        int gap = ActivityMetrics.COLUMN_GAP;
        int cardWidth = twoColumns ? (width - gap) / 2 : width;
        int leftY = y, rightY = y;
        for (int i = 0; i < modules.size(); i++) {
            boolean right = twoColumns && (i % 2 == 1);
            int h = ModuleCardBuilder.buildCard(tab, screen, container, modules.get(i),
                right ? x + cardWidth + gap : x, right ? rightY : leftY,
                cardWidth, cardWidth - ActivityMetrics.PADDING_PANEL * 2);
            if (right) rightY += h + 10;
            else leftY += h + 10;
        }
        return Math.max(leftY, rightY);
    }
}
