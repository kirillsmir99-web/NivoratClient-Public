package activity.client.gui.tab;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Manages tab registry, switching, and configuration synchronization across the Activity GUI.
 */
public class TabManager {

    private final List<ActivityTab> tabs;
    private int selectedIndex = 0;

    public TabManager(List<ActivityTab> tabs) {
        this.tabs = tabs != null ? Collections.unmodifiableList(tabs) : Collections.emptyList();
    }

    public List<ActivityTab> getTabs() {
        return tabs;
    }

    public int getTabCount() {
        return tabs.size();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < this.tabs.size()) {
            this.selectedIndex = index;
        }
    }

    public ActivityTab getSelectedTab() {
        if (this.selectedIndex >= 0 && this.selectedIndex < this.tabs.size()) {
            return this.tabs.get(this.selectedIndex);
        }
        if (!this.tabs.isEmpty()) {
            return this.tabs.get(0);
        }
        throw new IllegalStateException("TabManager has no registered tabs");
    }

    @Nullable
    public ActivityTab getTab(int index) {
        if (index >= 0 && index < this.tabs.size()) {
            return this.tabs.get(index);
        }
        return null;
    }

    @Nullable
    public ActivityTab getTabById(String id) {
        for (ActivityTab tab : this.tabs) {
            if (tab.getId().equalsIgnoreCase(id)) {
                return tab;
            }
        }
        return null;
    }

    public int getTabIndexById(String id) {
        if (id == null) return -1;
        for (int i = 0; i < this.tabs.size(); i++) {
            if (this.tabs.get(i).getId().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    public void selectNext() {
        if (!this.tabs.isEmpty()) {
            this.selectedIndex = (this.selectedIndex + 1) % this.tabs.size();
        }
    }

    public void selectPrevious() {
        if (!this.tabs.isEmpty()) {
            this.selectedIndex = (this.selectedIndex - 1 + this.tabs.size()) % this.tabs.size();
        }
    }

    /**
     * Loads settings into all registered tabs from the provided configuration.
     */
    public void loadAllFromConfig(ActivityConfig config) {
        if (config == null) return;
        for (ActivityTab tab : this.tabs) {
            tab.loadFromConfig(config);
        }
    }

    /**
     * Collects and saves settings from all registered tabs into the provided configuration.
     */
    public void saveAllToConfig(ActivityConfig config) {
        if (config == null) return;
        for (ActivityTab tab : this.tabs) {
            tab.saveToConfig(config);
        }
    }

    /**
     * Resets persistent state on all registered tabs to factory defaults and persists to disk.
     */
    public void resetAllTabs() {
        for (ActivityTab tab : this.tabs) {
            tab.resetDefaults();
        }
        ActivityConfig config = ActivityConfigManager.getConfig();
        config.resetToDefaults();
        saveAllToConfig(config);
        ActivityConfigManager.save();
    }
}
