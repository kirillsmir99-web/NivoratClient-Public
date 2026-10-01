package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ModulePreviewCard;
import activity.client.gui.icon.ActivityIcon;
import activity.client.gui.layout.ScrollContainer;
import activity.client.gui.navigation.PvpKit;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.text.Text;

public final class PvpKitTab extends ActivityTab {
    private final PvpKit kit;
    private int section = 0;
    private static final String[] SECTIONS = {"all", "weapons", "defense", "hud", "utility"};

    public PvpKitTab(PvpKit kit) {
        super(kit.id(), Text.translatable("activity.tab." + kit.id()), ActivityIcon.COMBAT);
        this.kit = kit;
    }
    @Override public Text getSubtitle() { return Text.translatable("activity.kit.subtitle"); }
    @Override public void resetDefaults() { }
    @Override public void loadFromConfig(ActivityConfig config) { }
    @Override public void saveToConfig(ActivityConfig config) { }

    private boolean matchesSection(IModule m) {
        if (section == 0) return true;
        String id = m.getId();
        return switch (section) {
            case 1 -> m.getCategory() == activity.client.module.api.ModuleCategory.COMBAT;
            case 2 -> m.getCategory() == activity.client.module.api.ModuleCategory.DEFENSE;
            case 3 -> id.contains("hud") || id.equals("hp_reaper");
            default -> m.getCategory() == activity.client.module.api.ModuleCategory.UTILITY
                && !id.contains("hud") && !id.equals("hp_reaper");
        };
    }
    @Override
    public void buildTab(ActivityScreen screen, ScrollContainer container, int x, int y, int width) {
        clearComponents();
        if (kit == PvpKit.ALL) {
            int gap = 3;
            int buttonW = Math.max(20, (width - gap * 4) / 5);
            for (int i = 0; i < SECTIONS.length; i++) {
                final int selected = i;
                var b = new ActivityButton(x + i * (buttonW + gap), y, buttonW, 20,
                    Text.translatable("activity.section." + SECTIONS[i]),
                    section == i ? ActivityButton.Variant.PRIMARY : ActivityButton.Variant.SECONDARY,
                    ignored -> { section = selected; screen.reloadCurrentTab(); });
                addControl(container, b);
            }
            y += 28;
        }
        boolean twoColumns = width >= 300;
        int gap = 10;
        int cardW = twoColumns ? (width - gap) / 2 : width;
        int index = 0;
        for (IModule module : ModuleRegistry.getAll()) {
            if (!kit.matches(module) || !matchesSection(module)) continue;
            int col = twoColumns ? index % 2 : 0;
            int row = twoColumns ? index / 2 : index;
            var card = new ModulePreviewCard(screen, module, x + col * (cardW + gap),
                y + row * (ModulePreviewCard.HEIGHT + gap), cardW);
            container.addChild(card);
            addComponent(card);
            registerModuleCard(module.getId(), card);
            index++;
        }
        if (kit == PvpKit.ALL && (section == 0 || section == 3)) {
            endModuleSection();
            int row = twoColumns ? (index + 1) / 2 : index;
            var hud = new ActivityButton(x, y + row * (ModulePreviewCard.HEIGHT + gap), width, 24,
                Text.translatable("activity.card.utility.hud_activity"),
                ignored -> screen.navigateToModule("utility", "hud_activity"));
            addControl(container, hud);
        }
    }
}
