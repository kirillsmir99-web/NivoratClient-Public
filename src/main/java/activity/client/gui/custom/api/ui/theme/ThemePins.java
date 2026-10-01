package activity.client.gui.custom.api.ui.theme;

import activity.client.gui.custom.VisualSettingsStore;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ThemePins {
    private static final Set<String> IDS = new LinkedHashSet<>();
    private ThemePins() {}
    public static Set<String> ids() { return Set.copyOf(IDS); }
    public static void load(Iterable<String> ids) { IDS.clear(); for (String id : ids) if (id != null) IDS.add(id); }
    public static boolean isPinned(ITheme theme) { return IDS.contains(theme.id()); }
    public static void toggle(ITheme theme) {
        if (!IDS.remove(theme.id())) IDS.add(theme.id());
        VisualSettingsStore.markDirty();
    }
}
