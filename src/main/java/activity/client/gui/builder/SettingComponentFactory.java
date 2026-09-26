package activity.client.gui.builder;

import activity.client.gui.ActivityScreen;
import activity.client.gui.component.ActivityButton;
import activity.client.gui.component.ActivityComponent;
import activity.client.gui.component.ActivityDropdown;
import activity.client.gui.component.ActivityKeybindButton;
import activity.client.gui.component.ActivityLabel;
import activity.client.gui.component.ActivitySlider;
import activity.client.gui.component.ActivityTextField;
import activity.client.gui.component.ActivityToggle;
import activity.client.gui.theme.ActivityMetrics;
import activity.client.module.setting.ActionSetting;
import activity.client.module.setting.BooleanSetting;
import activity.client.module.setting.EnumSetting;
import activity.client.module.setting.KeybindSetting;
import activity.client.module.setting.NumberSetting;
import activity.client.module.setting.Setting;
import activity.client.module.setting.StringSetting;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Consumer;

public final class SettingComponentFactory {

    private SettingComponentFactory() {}

    public record SettingRow(ActivityLabel label, ActivityComponent control) {}

    public static SettingRow createRow(Setting<?> setting, int startX, int rowY, int innerRowW,
                                       ActivityScreen screen, Runnable onModified) {
        return createRow(setting, startX, rowY, innerRowW,
                screen != null ? screen.getOverlayManager() : null,
                screen != null ? screen.getModalManager() : null,
                onModified);
    }

    public static SettingRow createRow(Setting<?> setting, int startX, int rowY, int innerRowW,
                                       activity.client.gui.overlay.OverlayManager overlayManager,
                                       activity.client.gui.modal.ModalManager modalManager,
                                       Runnable onModified) {
        return createRow(setting, startX, rowY, innerRowW, overlayManager, modalManager, onModified, null);
    }

    public static SettingRow createRow(Setting<?> setting, int startX, int rowY, int innerRowW,
                                       activity.client.gui.overlay.OverlayManager overlayManager,
                                       activity.client.gui.modal.ModalManager modalManager,
                                       Runnable onModified, Consumer<Runnable> onDispose) {
        if (setting == null) return null;

        int sliderW = Math.min(150, (int) (innerRowW * 0.55f));
        int dropdownW = Math.min(130, innerRowW / 2);
        int keybindBtnW = innerRowW < 200 ? 55 : (innerRowW < 240 ? 70 : 85);
        int tfW = Math.min(130, innerRowW / 2);
        int btnW = Math.min(130, innerRowW / 2);

        SettingRow row = null;
        if (setting instanceof BooleanSetting bs) {
            int toggleLabelMaxW = Math.max(20, innerRowW - ActivityMetrics.TOGGLE_WIDTH - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, bs.getName());
            lbl.setMaxWidth(toggleLabelMaxW);
            ActivityToggle toggle = createToggle(bs, startX + innerRowW - ActivityMetrics.TOGGLE_WIDTH, rowY, modalManager, onModified, onDispose);
            row = new SettingRow(lbl, toggle);

        } else if (setting instanceof NumberSetting ns) {
            int labelMaxW = Math.max(20, innerRowW - sliderW - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, ns.getName());
            lbl.setMaxWidth(labelMaxW);
            ActivitySlider slider = createSlider(ns, startX + innerRowW - sliderW, rowY, sliderW, ActivityMetrics.CONTROL_HEIGHT, onModified, onDispose);
            row = new SettingRow(lbl, slider);

        } else if (setting instanceof EnumSetting es) {
            int labelMaxW = Math.max(20, innerRowW - dropdownW - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, es.getName());
            lbl.setMaxWidth(labelMaxW);
            ActivityDropdown<String> dropdown = createDropdown(es, startX + innerRowW - dropdownW, rowY, dropdownW, ActivityMetrics.CONTROL_HEIGHT, overlayManager, onModified, onDispose);
            row = new SettingRow(lbl, dropdown);

        } else if (setting instanceof KeybindSetting ks) {
            int labelMaxW = Math.max(20, innerRowW - keybindBtnW - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, ks.getName());
            lbl.setMaxWidth(labelMaxW);
            ActivityKeybindButton btn = createKeybindButton(ks, startX + innerRowW - keybindBtnW, rowY, keybindBtnW, ActivityMetrics.CONTROL_HEIGHT, onModified);
            row = new SettingRow(lbl, btn);

        } else if (setting instanceof StringSetting ss) {
            int labelMaxW = Math.max(20, innerRowW - tfW - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, ss.getName());
            lbl.setMaxWidth(labelMaxW);
            ActivityTextField tf = createTextField(ss, startX + innerRowW - tfW, rowY, tfW, ActivityMetrics.CONTROL_HEIGHT, onModified);
            row = new SettingRow(lbl, tf);

        } else if (setting instanceof ActionSetting as) {
            int labelMaxW = Math.max(20, innerRowW - btnW - 4);
            ActivityLabel lbl = new ActivityLabel(startX, rowY + 3, as.getName());
            lbl.setMaxWidth(labelMaxW);
            ActivityButton btn = createActionButton(as, startX + innerRowW - btnW, rowY, btnW, ActivityMetrics.CONTROL_HEIGHT);
            row = new SettingRow(lbl, btn);
        }

        if (row != null && row.label() != null && setting.getDescription() != null && !setting.getDescription().getString().isEmpty()) {
            row.label().setTooltip(setting.getDescription());
        }

        return row;
    }

    public static ActivityToggle createToggle(BooleanSetting setting, int x, int y,
                                              ActivityScreen screen, Runnable onModified) {
        return createToggle(setting, x, y, screen != null ? screen.getModalManager() : null, onModified);
    }

    public static ActivityToggle createToggle(BooleanSetting setting, int x, int y,
                                              activity.client.gui.modal.ModalManager modalManager, Runnable onModified) {
        return createToggle(setting, x, y, modalManager, onModified, null);
    }

    private static ActivityToggle createToggle(BooleanSetting setting, int x, int y,
                                              activity.client.gui.modal.ModalManager modalManager, Runnable onModified,
                                              Consumer<Runnable> onDispose) {
        ActivityToggle toggle = new ActivityToggle(
                x, y,
                setting.get(),
                val -> {
                    setting.set(val);
                    if (onModified != null) onModified.run();
                }
        );
        Consumer<Boolean> listener = val -> {
            if (toggle.getState() != val) {
                toggle.setState(val);
            }
        };
        attachListener(setting, listener, onDispose);
        if (setting.getId().toLowerCase(Locale.ROOT).contains("legit") && modalManager != null) {
            toggle.setConfirmTurnOff(
                    Text.translatable("activity.modal.legit_off.title"),
                    Text.translatable("activity.modal.legit_off.desc"),
                    Text.translatable("activity.button.disable"),
                    modalManager
            );
        }
        return toggle;
    }

    public static ActivitySlider createSlider(NumberSetting setting, int x, int y, int width, int height,
                                              Runnable onModified) {
        return createSlider(setting, x, y, width, height, onModified, null);
    }

    private static ActivitySlider createSlider(NumberSetting setting, int x, int y, int width, int height,
                                              Runnable onModified, Consumer<Runnable> onDispose) {
        ActivitySlider slider = new ActivitySlider(
                x, y,
                width, height,
                setting.getMin(), setting.getMax(), setting.get(), setting.getStep(),
                Text.translatable("activity.setting.defense.delay_label"),
                val -> Text.literal(setting.formatValue(val)),
                val -> {
                    setting.set(val);
                    if (onModified != null) onModified.run();
                }
        );
        Consumer<Double> listener = val -> {
            if (Double.compare(slider.getValue(), val) != 0) {
                slider.setValueSilently(val);
            }
        };
        attachListener(setting, listener, onDispose);
        return slider;
    }

    public static ActivityDropdown<String> createDropdown(EnumSetting setting, int x, int y, int width, int height,
                                                         ActivityScreen screen, Runnable onModified) {
        return createDropdown(setting, x, y, width, height, screen != null ? screen.getOverlayManager() : null, onModified);
    }

    public static ActivityDropdown<String> createDropdown(EnumSetting setting, int x, int y, int width, int height,
                                                         activity.client.gui.overlay.OverlayManager overlayManager, Runnable onModified) {
        return createDropdown(setting, x, y, width, height, overlayManager, onModified, null);
    }

    private static ActivityDropdown<String> createDropdown(EnumSetting setting, int x, int y, int width, int height,
                                                         activity.client.gui.overlay.OverlayManager overlayManager,
                                                         Runnable onModified, Consumer<Runnable> onDispose) {
        ActivityDropdown<String> dropdown = new ActivityDropdown<>(
                x, y, width, height,
                overlayManager,
                setting.getOptions(), setting.get(),
                setting::getOptionName,
                val -> {
                    setting.set(val);
                    if (onModified != null) onModified.run();
                }
        );
        if (setting.getTooltipProvider() != null) {
            dropdown.setTooltipProvider(setting::getOptionTooltip);
        }
        dropdown.setTooltip(setting.getDescription());
        Consumer<String> listener = val -> {
            if (!java.util.Objects.equals(dropdown.getSelectedOption(), val)) {
                dropdown.setSelectedOptionSilently(val);
            }
        };
        attachListener(setting, listener, onDispose);
        return dropdown;
    }

    private static <T> void attachListener(Setting<T> setting, Consumer<T> listener, Consumer<Runnable> onDispose) {
        setting.addListener(listener);
        if (onDispose != null) onDispose.accept(() -> setting.removeListener(listener));
    }

    public static ActivityKeybindButton createKeybindButton(KeybindSetting setting, int x, int y, int width, int height,
                                                            Runnable onModified) {
        return new ActivityKeybindButton(
                x, y, width, height,
                setting.get(),
                kb -> {
                    setting.set(kb);
                    if (onModified != null) onModified.run();
                }
        );
    }

    public static ActivityTextField createTextField(StringSetting setting, int x, int y, int width, int height,
                                                    Runnable onModified) {
        ActivityTextField tf = new ActivityTextField(
                x, y, width, height,
                Text.translatable("activity.setting.utility.phrase_placeholder")
        );
        tf.setText(setting.get());
        tf.setOnChanged(val -> {
            setting.set(val);
            if (onModified != null) onModified.run();
        });
        return tf;
    }

    public static ActivityButton createActionButton(ActionSetting setting, int x, int y, int width, int height) {
        return new ActivityButton(
                x, y, width, height,
                setting.getName(),
                b -> setting.execute()
        );
    }
}
