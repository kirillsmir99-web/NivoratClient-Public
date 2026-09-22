package activity.client.module.setting;

import net.minecraft.text.Text;

public class ActionSetting extends Setting<Runnable> {

    public ActionSetting(String id, Text name, Text description, SettingGroup group, Runnable action) {
        super(id, name, description, group, action);
    }

    public ActionSetting(String id, Text name, Text description, SettingSection section, Runnable action) {
        super(id, name, description, section, action);
    }

    @Override
    public Runnable get() {
        return getDefaultValue();
    }

    @Override
    public void set(Runnable value) {

    }

    public void execute() {
        Runnable action = getDefaultValue();
        if (action != null) {
            try {
                action.run();
            } catch (Throwable ignored) {}
        }
    }
}
