package activity.client.gui.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registry of all custom NivoratClient sound events.
 *
 * <p>Pre-registers all 27 Serene events, 12 Classic events, and legacy backward-compatible aliases.
 * All sound event instances are pre-allocated constants to prevent dynamic allocations in audio ticks.
 */
public final class ActivitySoundEvents {

    public static final String MOD_ID = "nivoratclient";
    public static final String ALT_MOD_ID = "activity";

    // ==========================================
    // 1. SERENE EVENTS (27 UI EVENTS)
    // ==========================================
    public static final Identifier SERENE_OPEN_ID = Identifier.of(MOD_ID, "ui.serene.open");
    public static final SoundEvent SERENE_OPEN = SoundEvent.of(SERENE_OPEN_ID);

    public static final Identifier SERENE_CLOSE_ID = Identifier.of(MOD_ID, "ui.serene.close");
    public static final SoundEvent SERENE_CLOSE = SoundEvent.of(SERENE_CLOSE_ID);

    public static final Identifier SERENE_BUTTON_PRIMARY_ID = Identifier.of(MOD_ID, "ui.serene.button_primary");
    public static final SoundEvent SERENE_BUTTON_PRIMARY = SoundEvent.of(SERENE_BUTTON_PRIMARY_ID);

    public static final Identifier SERENE_BUTTON_SECONDARY_ID = Identifier.of(MOD_ID, "ui.serene.button_secondary");
    public static final SoundEvent SERENE_BUTTON_SECONDARY = SoundEvent.of(SERENE_BUTTON_SECONDARY_ID);

    public static final Identifier SERENE_HOVER_ID = Identifier.of(MOD_ID, "ui.serene.hover");
    public static final SoundEvent SERENE_HOVER = SoundEvent.of(SERENE_HOVER_ID);

    public static final Identifier SERENE_TOGGLE_ON_ID = Identifier.of(MOD_ID, "ui.serene.toggle_on");
    public static final SoundEvent SERENE_TOGGLE_ON = SoundEvent.of(SERENE_TOGGLE_ON_ID);

    public static final Identifier SERENE_TOGGLE_OFF_ID = Identifier.of(MOD_ID, "ui.serene.toggle_off");
    public static final SoundEvent SERENE_TOGGLE_OFF = SoundEvent.of(SERENE_TOGGLE_OFF_ID);

    public static final Identifier SERENE_DROPDOWN_OPEN_ID = Identifier.of(MOD_ID, "ui.serene.dropdown_open");
    public static final SoundEvent SERENE_DROPDOWN_OPEN = SoundEvent.of(SERENE_DROPDOWN_OPEN_ID);

    public static final Identifier SERENE_DROPDOWN_CLOSE_ID = Identifier.of(MOD_ID, "ui.serene.dropdown_close");
    public static final SoundEvent SERENE_DROPDOWN_CLOSE = SoundEvent.of(SERENE_DROPDOWN_CLOSE_ID);

    public static final Identifier SERENE_CATEGORY_EXPAND_ID = Identifier.of(MOD_ID, "ui.serene.category_expand");
    public static final SoundEvent SERENE_CATEGORY_EXPAND = SoundEvent.of(SERENE_CATEGORY_EXPAND_ID);

    public static final Identifier SERENE_CATEGORY_COLLAPSE_ID = Identifier.of(MOD_ID, "ui.serene.category_collapse");
    public static final SoundEvent SERENE_CATEGORY_COLLAPSE = SoundEvent.of(SERENE_CATEGORY_COLLAPSE_ID);

    public static final Identifier SERENE_MODAL_OPEN_ID = Identifier.of(MOD_ID, "ui.serene.modal_open");
    public static final SoundEvent SERENE_MODAL_OPEN = SoundEvent.of(SERENE_MODAL_OPEN_ID);

    public static final Identifier SERENE_MODAL_CLOSE_ID = Identifier.of(MOD_ID, "ui.serene.modal_close");
    public static final SoundEvent SERENE_MODAL_CLOSE = SoundEvent.of(SERENE_MODAL_CLOSE_ID);

    public static final Identifier SERENE_SUCCESS_ID = Identifier.of(MOD_ID, "ui.serene.success");
    public static final SoundEvent SERENE_SUCCESS = SoundEvent.of(SERENE_SUCCESS_ID);

    public static final Identifier SERENE_WARNING_ID = Identifier.of(MOD_ID, "ui.serene.warning");
    public static final SoundEvent SERENE_WARNING = SoundEvent.of(SERENE_WARNING_ID);

    public static final Identifier SERENE_ERROR_ID = Identifier.of(MOD_ID, "ui.serene.error");
    public static final SoundEvent SERENE_ERROR = SoundEvent.of(SERENE_ERROR_ID);

    public static final Identifier SERENE_SLIDER_TICK_ID = Identifier.of(MOD_ID, "ui.serene.slider_tick");
    public static final SoundEvent SERENE_SLIDER_TICK = SoundEvent.of(SERENE_SLIDER_TICK_ID);

    public static final Identifier SERENE_PIN_ID = Identifier.of(MOD_ID, "ui.serene.pin");
    public static final SoundEvent SERENE_PIN = SoundEvent.of(SERENE_PIN_ID);

    public static final Identifier SERENE_UNPIN_ID = Identifier.of(MOD_ID, "ui.serene.unpin");
    public static final SoundEvent SERENE_UNPIN = SoundEvent.of(SERENE_UNPIN_ID);

    public static final Identifier SERENE_COPY_ID = Identifier.of(MOD_ID, "ui.serene.copy");
    public static final SoundEvent SERENE_COPY = SoundEvent.of(SERENE_COPY_ID);

    public static final Identifier SERENE_IMPORT_ID = Identifier.of(MOD_ID, "ui.serene.import");
    public static final SoundEvent SERENE_IMPORT = SoundEvent.of(SERENE_IMPORT_ID);

    public static final Identifier SERENE_DELETE_ID = Identifier.of(MOD_ID, "ui.serene.delete");
    public static final SoundEvent SERENE_DELETE = SoundEvent.of(SERENE_DELETE_ID);

    public static final Identifier SERENE_LINK_OPEN_ID = Identifier.of(MOD_ID, "ui.serene.link_open");
    public static final SoundEvent SERENE_LINK_OPEN = SoundEvent.of(SERENE_LINK_OPEN_ID);

    public static final Identifier SERENE_RELOAD_ID = Identifier.of(MOD_ID, "ui.serene.reload");
    public static final SoundEvent SERENE_RELOAD = SoundEvent.of(SERENE_RELOAD_ID);

    public static final Identifier SERENE_MAXIMIZE_ID = Identifier.of(MOD_ID, "ui.serene.maximize");
    public static final SoundEvent SERENE_MAXIMIZE = SoundEvent.of(SERENE_MAXIMIZE_ID);

    public static final Identifier SERENE_RESTORE_ID = Identifier.of(MOD_ID, "ui.serene.restore");
    public static final SoundEvent SERENE_RESTORE = SoundEvent.of(SERENE_RESTORE_ID);

    public static final Identifier SERENE_SEARCH_FOCUS_ID = Identifier.of(MOD_ID, "ui.serene.search_focus");
    public static final SoundEvent SERENE_SEARCH_FOCUS = SoundEvent.of(SERENE_SEARCH_FOCUS_ID);

    // ==========================================
    // 2. CLASSIC EVENTS (12 UI EVENTS)
    // ==========================================
    public static final Identifier CLASSIC_OPEN_ID = Identifier.of(MOD_ID, "ui.classic.open");
    public static final SoundEvent CLASSIC_OPEN = SoundEvent.of(CLASSIC_OPEN_ID);

    public static final Identifier CLASSIC_CLOSE_ID = Identifier.of(MOD_ID, "ui.classic.close");
    public static final SoundEvent CLASSIC_CLOSE = SoundEvent.of(CLASSIC_CLOSE_ID);

    public static final Identifier CLASSIC_BUTTON_ID = Identifier.of(MOD_ID, "ui.classic.button");
    public static final SoundEvent CLASSIC_BUTTON = SoundEvent.of(CLASSIC_BUTTON_ID);

    public static final Identifier CLASSIC_TOGGLE_ON_ID = Identifier.of(MOD_ID, "ui.classic.toggle_on");
    public static final SoundEvent CLASSIC_TOGGLE_ON = SoundEvent.of(CLASSIC_TOGGLE_ON_ID);

    public static final Identifier CLASSIC_TOGGLE_OFF_ID = Identifier.of(MOD_ID, "ui.classic.toggle_off");
    public static final SoundEvent CLASSIC_TOGGLE_OFF = SoundEvent.of(CLASSIC_TOGGLE_OFF_ID);

    public static final Identifier CLASSIC_DROPDOWN_OPEN_ID = Identifier.of(MOD_ID, "ui.classic.dropdown_open");
    public static final SoundEvent CLASSIC_DROPDOWN_OPEN = SoundEvent.of(CLASSIC_DROPDOWN_OPEN_ID);

    public static final Identifier CLASSIC_DROPDOWN_CLOSE_ID = Identifier.of(MOD_ID, "ui.classic.dropdown_close");
    public static final SoundEvent CLASSIC_DROPDOWN_CLOSE = SoundEvent.of(CLASSIC_DROPDOWN_CLOSE_ID);

    public static final Identifier CLASSIC_CATEGORY_EXPAND_ID = Identifier.of(MOD_ID, "ui.classic.category_expand");
    public static final SoundEvent CLASSIC_CATEGORY_EXPAND = SoundEvent.of(CLASSIC_CATEGORY_EXPAND_ID);

    public static final Identifier CLASSIC_CATEGORY_COLLAPSE_ID = Identifier.of(MOD_ID, "ui.classic.category_collapse");
    public static final SoundEvent CLASSIC_CATEGORY_COLLAPSE = SoundEvent.of(CLASSIC_CATEGORY_COLLAPSE_ID);

    public static final Identifier CLASSIC_SUCCESS_ID = Identifier.of(MOD_ID, "ui.classic.success");
    public static final SoundEvent CLASSIC_SUCCESS = SoundEvent.of(CLASSIC_SUCCESS_ID);

    public static final Identifier CLASSIC_WARNING_ID = Identifier.of(MOD_ID, "ui.classic.warning");
    public static final SoundEvent CLASSIC_WARNING = SoundEvent.of(CLASSIC_WARNING_ID);

    public static final Identifier CLASSIC_SLIDER_TICK_ID = Identifier.of(MOD_ID, "ui.classic.slider_tick");
    public static final SoundEvent CLASSIC_SLIDER_TICK = SoundEvent.of(CLASSIC_SLIDER_TICK_ID);

    // ==========================================
    // 3. LEGACY / COMPATIBILITY ALIASES
    // ==========================================
    public static final Identifier MENU_OPEN_ID = SERENE_OPEN_ID;
    public static final Identifier MENU_CLOSE_ID = SERENE_CLOSE_ID;
    public static final Identifier HOVER_ID = SERENE_HOVER_ID;
    public static final Identifier SELECT_ID = SERENE_BUTTON_PRIMARY_ID;
    public static final Identifier CLICK_ID = SERENE_BUTTON_PRIMARY_ID;
    public static final Identifier TOGGLE_ON_ID = SERENE_TOGGLE_ON_ID;
    public static final Identifier TOGGLE_OFF_ID = SERENE_TOGGLE_OFF_ID;
    public static final Identifier DROPDOWN_OPEN_ID = SERENE_DROPDOWN_OPEN_ID;
    public static final Identifier DROPDOWN_CLOSE_ID = SERENE_DROPDOWN_CLOSE_ID;
    public static final Identifier CATEGORY_EXPAND_ID = SERENE_CATEGORY_EXPAND_ID;
    public static final Identifier CATEGORY_COLLAPSE_ID = SERENE_CATEGORY_COLLAPSE_ID;
    public static final Identifier PRESET_SAVE_ID = SERENE_SUCCESS_ID;
    public static final Identifier PRESET_RESET_ID = SERENE_ERROR_ID;
    public static final Identifier PRESET_APPLY_ID = SERENE_SUCCESS_ID;
    public static final Identifier SLIDER_TICK_ID = SERENE_SLIDER_TICK_ID;

    public static final SoundEvent MENU_OPEN = SERENE_OPEN;
    public static final SoundEvent MENU_CLOSE = SERENE_CLOSE;
    public static final SoundEvent HOVER = SERENE_HOVER;
    public static final SoundEvent SELECT = SERENE_BUTTON_PRIMARY;
    public static final SoundEvent CLICK = SERENE_BUTTON_PRIMARY;
    public static final SoundEvent TOGGLE_ON = SERENE_TOGGLE_ON;
    public static final SoundEvent TOGGLE_OFF = SERENE_TOGGLE_OFF;
    public static final SoundEvent DROPDOWN_OPEN = SERENE_DROPDOWN_OPEN;
    public static final SoundEvent DROPDOWN_CLOSE = SERENE_DROPDOWN_CLOSE;
    public static final SoundEvent CATEGORY_EXPAND = SERENE_CATEGORY_EXPAND;
    public static final SoundEvent CATEGORY_COLLAPSE = SERENE_CATEGORY_COLLAPSE;
    public static final SoundEvent PRESET_SAVE = SERENE_SUCCESS;
    public static final SoundEvent PRESET_RESET = SERENE_ERROR;
    public static final SoundEvent PRESET_APPLY = SERENE_SUCCESS;
    public static final SoundEvent SLIDER_TICK = SERENE_SLIDER_TICK;

    private ActivitySoundEvents() {}

    /**
     * Registers all sound events into Minecraft's SoundEvent registry.
     */
    public static void register() {
        // Serene
        registerSafe(SERENE_OPEN_ID, SERENE_OPEN);
        registerSafe(SERENE_CLOSE_ID, SERENE_CLOSE);
        registerSafe(SERENE_BUTTON_PRIMARY_ID, SERENE_BUTTON_PRIMARY);
        registerSafe(SERENE_BUTTON_SECONDARY_ID, SERENE_BUTTON_SECONDARY);
        registerSafe(SERENE_HOVER_ID, SERENE_HOVER);
        registerSafe(SERENE_TOGGLE_ON_ID, SERENE_TOGGLE_ON);
        registerSafe(SERENE_TOGGLE_OFF_ID, SERENE_TOGGLE_OFF);
        registerSafe(SERENE_DROPDOWN_OPEN_ID, SERENE_DROPDOWN_OPEN);
        registerSafe(SERENE_DROPDOWN_CLOSE_ID, SERENE_DROPDOWN_CLOSE);
        registerSafe(SERENE_CATEGORY_EXPAND_ID, SERENE_CATEGORY_EXPAND);
        registerSafe(SERENE_CATEGORY_COLLAPSE_ID, SERENE_CATEGORY_COLLAPSE);
        registerSafe(SERENE_MODAL_OPEN_ID, SERENE_MODAL_OPEN);
        registerSafe(SERENE_MODAL_CLOSE_ID, SERENE_MODAL_CLOSE);
        registerSafe(SERENE_SUCCESS_ID, SERENE_SUCCESS);
        registerSafe(SERENE_WARNING_ID, SERENE_WARNING);
        registerSafe(SERENE_ERROR_ID, SERENE_ERROR);
        registerSafe(SERENE_SLIDER_TICK_ID, SERENE_SLIDER_TICK);
        registerSafe(SERENE_PIN_ID, SERENE_PIN);
        registerSafe(SERENE_UNPIN_ID, SERENE_UNPIN);
        registerSafe(SERENE_COPY_ID, SERENE_COPY);
        registerSafe(SERENE_IMPORT_ID, SERENE_IMPORT);
        registerSafe(SERENE_DELETE_ID, SERENE_DELETE);
        registerSafe(SERENE_LINK_OPEN_ID, SERENE_LINK_OPEN);
        registerSafe(SERENE_RELOAD_ID, SERENE_RELOAD);
        registerSafe(SERENE_MAXIMIZE_ID, SERENE_MAXIMIZE);
        registerSafe(SERENE_RESTORE_ID, SERENE_RESTORE);
        registerSafe(SERENE_SEARCH_FOCUS_ID, SERENE_SEARCH_FOCUS);

        // Classic
        registerSafe(CLASSIC_OPEN_ID, CLASSIC_OPEN);
        registerSafe(CLASSIC_CLOSE_ID, CLASSIC_CLOSE);
        registerSafe(CLASSIC_BUTTON_ID, CLASSIC_BUTTON);
        registerSafe(CLASSIC_TOGGLE_ON_ID, CLASSIC_TOGGLE_ON);
        registerSafe(CLASSIC_TOGGLE_OFF_ID, CLASSIC_TOGGLE_OFF);
        registerSafe(CLASSIC_DROPDOWN_OPEN_ID, CLASSIC_DROPDOWN_OPEN);
        registerSafe(CLASSIC_DROPDOWN_CLOSE_ID, CLASSIC_DROPDOWN_CLOSE);
        registerSafe(CLASSIC_CATEGORY_EXPAND_ID, CLASSIC_CATEGORY_EXPAND);
        registerSafe(CLASSIC_CATEGORY_COLLAPSE_ID, CLASSIC_CATEGORY_COLLAPSE);
        registerSafe(CLASSIC_SUCCESS_ID, CLASSIC_SUCCESS);
        registerSafe(CLASSIC_WARNING_ID, CLASSIC_WARNING);
        registerSafe(CLASSIC_SLIDER_TICK_ID, CLASSIC_SLIDER_TICK);

        // Base ui.* events from sounds.json
        registerSafe(Identifier.of(MOD_ID, "ui.open"), SoundEvent.of(Identifier.of(MOD_ID, "ui.open")));
        registerSafe(Identifier.of(MOD_ID, "ui.close"), SoundEvent.of(Identifier.of(MOD_ID, "ui.close")));
        registerSafe(Identifier.of(MOD_ID, "ui.button"), SoundEvent.of(Identifier.of(MOD_ID, "ui.button")));
        registerSafe(Identifier.of(MOD_ID, "ui.toggle_on"), SoundEvent.of(Identifier.of(MOD_ID, "ui.toggle_on")));
        registerSafe(Identifier.of(MOD_ID, "ui.toggle_off"), SoundEvent.of(Identifier.of(MOD_ID, "ui.toggle_off")));
        registerSafe(Identifier.of(MOD_ID, "ui.dropdown_open"), SoundEvent.of(Identifier.of(MOD_ID, "ui.dropdown_open")));
        registerSafe(Identifier.of(MOD_ID, "ui.dropdown_close"), SoundEvent.of(Identifier.of(MOD_ID, "ui.dropdown_close")));
        registerSafe(Identifier.of(MOD_ID, "ui.category_expand"), SoundEvent.of(Identifier.of(MOD_ID, "ui.category_expand")));
        registerSafe(Identifier.of(MOD_ID, "ui.category_collapse"), SoundEvent.of(Identifier.of(MOD_ID, "ui.category_collapse")));
        registerSafe(Identifier.of(MOD_ID, "ui.success"), SoundEvent.of(Identifier.of(MOD_ID, "ui.success")));
        registerSafe(Identifier.of(MOD_ID, "ui.warning"), SoundEvent.of(Identifier.of(MOD_ID, "ui.warning")));
        registerSafe(Identifier.of(MOD_ID, "ui.slider_tick"), SoundEvent.of(Identifier.of(MOD_ID, "ui.slider_tick")));
    }

    private static void registerSafe(Identifier id, SoundEvent event) {
        // Intentionally no-op to prevent polluting Minecraft's vanilla Registries.SOUND_EVENT.
        // Client UI sounds are played via PositionedSoundInstance.ui(SoundEvent.of(id), ...)
        // which resolves directly through assets/activity/sounds.json without global registry exposure.
    }
}
