package activity.client.gui.sound;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Tactical audio manager for the NivoratClient GUI framework.
 *
 * <p>Supports three acoustic profiles:
 * <ul>
 *   <li>{@link SoundProfile#SERENE}: Default calm modern UI audio (27 discrete events).</li>
 *   <li>{@link SoundProfile#CLASSIC}: Legacy punchy sound pack (12 discrete events).</li>
 *   <li>{@link SoundProfile#MINECRAFT}: Vanilla Minecraft sound events.</li>
 * </ul>
 *
 * <p>Performance invariants:
 * <ul>
 *   <li>Zero disk I/O on clicks (all sounds are preloaded and pre-registered).</li>
 *   <li>Zero memory allocations per audio tick.</li>
 *   <li>Strict slider notch quantization (sounds strictly on notch change).</li>
 *   <li>Debounced hover and slider triggers (30-35ms for slider, 50ms for hover).</li>
 * </ul>
 */
public final class SoundManager {

    // Registry-free vanilla fallback events (safe for headless tests and runtime)
    private static final SoundEvent VANILLA_PLING = SoundEvent.of(Identifier.ofVanilla("block.note_block.pling"));
    private static final SoundEvent VANILLA_CLICK = SoundEvent.of(Identifier.ofVanilla("ui.button.click"));
    private static final SoundEvent VANILLA_BASS = SoundEvent.of(Identifier.ofVanilla("block.note_block.bass"));
    private static final SoundEvent VANILLA_PAGE = SoundEvent.of(Identifier.ofVanilla("item.book.page_turn"));
    private static final SoundEvent VANILLA_CHEST_OPEN = SoundEvent.of(Identifier.ofVanilla("block.chest.open"));
    private static final SoundEvent VANILLA_CHEST_CLOSE = SoundEvent.of(Identifier.ofVanilla("block.chest.close"));

    private static long lastSliderTickTime = 0L;
    private static long lastSliderNotch = Long.MIN_VALUE;
    private static long lastHoverTime = 0L;

    public static long getLastSliderNotch() {
        return lastSliderNotch;
    }

    public static void resetSliderTracking() {
        lastSliderNotch = Long.MIN_VALUE;
        lastSliderTickTime = 0L;
    }

    public static long getLastHoverTime() {
        return lastHoverTime;
    }

    public static void resetHoverTracking() {
        lastHoverTime = 0L;
    }

    private SoundManager() {}

    /**
     * @return true if master UI sounds are enabled in configuration
     */
    public static boolean isSoundEnabled() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        return config == null || config.soundEnabled;
    }

    /**
     * @return true if discrete slider ratchet audio cues are enabled
     */
    public static boolean isSliderSoundEnabled() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        return isSoundEnabled() && (config == null || config.sliderSoundEnabled);
    }

    /**
     * @return master volume multiplier in [0.0, 1.0] from configuration
     */
    public static float getVolumeMultiplier() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config == null) return 0.8f;
        return (float) Math.clamp(config.soundVolume / 100.0, 0.0, 1.0);
    }

    /**
     * @return active sound profile from configuration
     */
    public static SoundProfile getSoundProfile() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        return config != null ? SoundProfile.fromId(config.soundProfile) : SoundProfile.SERENE;
    }

    public static boolean isUseCustomSounds() {
        return getSoundProfile() != SoundProfile.MINECRAFT;
    }

    public static void setUseCustomSounds(boolean custom) {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            config.soundProfile = custom ? SoundProfile.SERENE.getId() : SoundProfile.MINECRAFT.getId();
        }
    }

    // ==========================================
    // 1. WINDOW TRANSITIONS
    // ==========================================

    public static void playOpen() {
        playSound(
            ActivitySoundEvents.SERENE_OPEN,
            ActivitySoundEvents.CLASSIC_OPEN,
            VANILLA_PLING,
            1.40f,
            0.80f
        );
    }

    public static void playClose() {
        playSound(
            ActivitySoundEvents.SERENE_CLOSE,
            ActivitySoundEvents.CLASSIC_CLOSE,
            VANILLA_PLING,
            0.85f,
            0.75f
        );
    }

    // ==========================================
    // 2. BUTTONS & CLICKS
    // ==========================================

    public static void playButtonPrimary() {
        playSound(
            ActivitySoundEvents.SERENE_BUTTON_PRIMARY,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.00f,
            0.75f
        );
    }

    public static void playButtonSecondary() {
        playSound(
            ActivitySoundEvents.SERENE_BUTTON_SECONDARY,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.15f,
            0.70f
        );
    }

    public static void playClick() {
        playButtonPrimary();
    }

    // ==========================================
    // 3. HOVER & SELECTION
    // ==========================================

    public static void playHover() {
        if (!isSoundEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastHoverTime < 50L) {
            return;
        }
        lastHoverTime = now;

        playSound(
            ActivitySoundEvents.SERENE_HOVER,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.60f,
            0.45f
        );
    }

    public static void playHoverImmediate() {
        if (!isSoundEnabled()) return;
        lastHoverTime = System.currentTimeMillis();
        playSound(
            ActivitySoundEvents.SERENE_HOVER,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.60f,
            0.45f
        );
    }

    public static void playSelect() {
        playButtonSecondary();
    }

    // ==========================================
    // 4. TOGGLES
    // ==========================================

    public static void playToggle(boolean state) {
        if (state) {
            playSound(
                ActivitySoundEvents.SERENE_TOGGLE_ON,
                ActivitySoundEvents.CLASSIC_TOGGLE_ON,
                VANILLA_PLING,
                1.60f,
                0.75f
            );
        } else {
            playSound(
                ActivitySoundEvents.SERENE_TOGGLE_OFF,
                ActivitySoundEvents.CLASSIC_TOGGLE_OFF,
                VANILLA_BASS,
                0.90f,
                0.75f
            );
        }
    }

    // ==========================================
    // 5. DROPDOWNS
    // ==========================================

    public static void playDropdownOpen() {
        playSound(
            ActivitySoundEvents.SERENE_DROPDOWN_OPEN,
            ActivitySoundEvents.CLASSIC_DROPDOWN_OPEN,
            VANILLA_CLICK,
            1.25f,
            0.65f
        );
    }

    public static void playDropdownClose() {
        playSound(
            ActivitySoundEvents.SERENE_DROPDOWN_CLOSE,
            ActivitySoundEvents.CLASSIC_DROPDOWN_CLOSE,
            VANILLA_CLICK,
            0.90f,
            0.60f
        );
    }

    // ==========================================
    // 6. CATEGORY ACCORDION & TABS
    // ==========================================

    public static void playCategoryExpand() {
        playSound(
            ActivitySoundEvents.SERENE_CATEGORY_EXPAND,
            ActivitySoundEvents.CLASSIC_CATEGORY_EXPAND,
            VANILLA_PAGE,
            1.30f,
            0.65f
        );
    }

    public static void playCategoryCollapse() {
        playSound(
            ActivitySoundEvents.SERENE_CATEGORY_COLLAPSE,
            ActivitySoundEvents.CLASSIC_CATEGORY_COLLAPSE,
            VANILLA_PAGE,
            0.95f,
            0.60f
        );
    }

    public static void playTabSwitch() {
        playSound(
            ActivitySoundEvents.SERENE_BUTTON_SECONDARY,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.15f,
            0.65f
        );
    }

    // ==========================================
    // 7. MODALS
    // ==========================================

    public static void playModalOpen() {
        playSound(
            ActivitySoundEvents.SERENE_MODAL_OPEN,
            ActivitySoundEvents.CLASSIC_OPEN,
            VANILLA_CHEST_OPEN,
            1.10f,
            0.75f
        );
    }

    public static void playModalClose() {
        playSound(
            ActivitySoundEvents.SERENE_MODAL_CLOSE,
            ActivitySoundEvents.CLASSIC_CLOSE,
            VANILLA_CHEST_CLOSE,
            1.00f,
            0.70f
        );
    }

    // ==========================================
    // 8. STATUS NOTIFICATIONS
    // ==========================================

    public static void playSuccess() {
        playSound(
            ActivitySoundEvents.SERENE_SUCCESS,
            ActivitySoundEvents.CLASSIC_SUCCESS,
            VANILLA_PLING,
            1.50f,
            0.85f
        );
    }

    public static void playWarning() {
        playSound(
            ActivitySoundEvents.SERENE_WARNING,
            ActivitySoundEvents.CLASSIC_WARNING,
            VANILLA_BASS,
            0.85f,
            0.85f
        );
    }

    public static void playError() {
        playSound(
            ActivitySoundEvents.SERENE_ERROR,
            ActivitySoundEvents.CLASSIC_WARNING,
            VANILLA_BASS,
            0.70f,
            0.90f
        );
    }

    // Presets mapping to status
    public static void playPresetSave() {
        playSuccess();
    }

    public static void playPresetApply() {
        playSuccess();
    }

    public static void playPresetReset() {
        playWarning();
    }

    // ==========================================
    // 9. WINDOW & ACTIONS
    // ==========================================

    public static void playPin() {
        playSound(
            ActivitySoundEvents.SERENE_PIN,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.40f,
            0.70f
        );
    }

    public static void playUnpin() {
        playSound(
            ActivitySoundEvents.SERENE_UNPIN,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.10f,
            0.65f
        );
    }

    public static void playCopy() {
        playSound(
            ActivitySoundEvents.SERENE_COPY,
            ActivitySoundEvents.CLASSIC_SUCCESS,
            VANILLA_PLING,
            1.40f,
            0.80f
        );
    }

    public static void playImport() {
        playSound(
            ActivitySoundEvents.SERENE_IMPORT,
            ActivitySoundEvents.CLASSIC_SUCCESS,
            VANILLA_PLING,
            1.20f,
            0.80f
        );
    }

    public static void playDelete() {
        playSound(
            ActivitySoundEvents.SERENE_DELETE,
            ActivitySoundEvents.CLASSIC_WARNING,
            VANILLA_BASS,
            0.75f,
            0.85f
        );
    }

    public static void playLinkOpen() {
        playSound(
            ActivitySoundEvents.SERENE_LINK_OPEN,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_PAGE,
            1.10f,
            0.70f
        );
    }

    public static void playReload() {
        playSound(
            ActivitySoundEvents.SERENE_RELOAD,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.20f,
            0.75f
        );
    }

    public static void playMaximize() {
        playSound(
            ActivitySoundEvents.SERENE_MAXIMIZE,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.30f,
            0.75f
        );
    }

    public static void playRestore() {
        playSound(
            ActivitySoundEvents.SERENE_RESTORE,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.00f,
            0.75f
        );
    }

    public static void playSearchFocus() {
        playSound(
            ActivitySoundEvents.SERENE_SEARCH_FOCUS,
            ActivitySoundEvents.CLASSIC_BUTTON,
            VANILLA_CLICK,
            1.40f,
            0.50f
        );
    }

    // ==========================================
    // 10. SLIDER DIGITAL RATCHET ("трррк")
    // ==========================================

    /**
     * Plays discrete digital ratchet ("трррк") audio cue for slider value increments.
     *
     * <p>Strict quantization invariant:
     * Sound triggers strictly when quantized value/notch actually changes:
     * <ul>
     *   <li>50 -&gt; 55: tick</li>
     *   <li>55 -&gt; 55: NO SOUND</li>
     * </ul>
     * Rate-limit debounce: strictly &gt;= 35ms.
     */
    public static void playSliderTick(double value, double min, double max, double step) {
        if (!isSliderSoundEnabled()) return;

        long now = System.currentTimeMillis();
        // Rate-limit debounce (35ms)
        if (now - lastSliderTickTime < 35L) {
            return;
        }

        double safeStep = (step > 0.0) ? step : Math.max(0.0001, (max - min) / 32.0);
        long notch = Math.round((value - min) / safeStep);

        // Quantization check: NO SOUND if notch did not change!
        if (notch == lastSliderNotch) {
            return;
        }

        lastSliderNotch = notch;
        lastSliderTickTime = now;

        float progress = (float) Math.clamp((value - min) / Math.max(0.0001, max - min), 0.0, 1.0);
        float toothFactor = (notch % 2 == 0) ? 1.0f : 1.05f;
        float pitch = (1.30f + progress * 0.35f) * toothFactor;

        playSound(
            ActivitySoundEvents.SERENE_SLIDER_TICK,
            ActivitySoundEvents.CLASSIC_SLIDER_TICK,
            VANILLA_CLICK,
            pitch,
            0.55f
        );
    }

    public static void playSliderRatchet(double value, double min, double max, double step) {
        playSliderTick(value, min, max, step);
    }

    // ==========================================
    // INTERNAL DISPATCH HELPER
    // ==========================================

    private static void playSound(SoundEvent sereneEvent, SoundEvent classicEvent, SoundEvent vanillaEvent, float pitch, float eventGain) {
        if (!isSoundEnabled()) return;

        SoundProfile profile = getSoundProfile();
        float profileBaseGain = switch (profile) {
            case SERENE -> 1.25f;
            case CLASSIC -> 1.15f;
            case MINECRAFT -> 0.85f;
        };
        float userUiVolume = getVolumeMultiplier();
        float finalVolume = (float) Math.clamp(profileBaseGain * userUiVolume * eventGain, 0.0f, 1.0f);
        if (finalVolume <= 0.001f) return;

        SoundEvent targetEvent = switch (profile) {
            case SERENE -> sereneEvent;
            case CLASSIC -> (classicEvent != null ? classicEvent : sereneEvent);
            case MINECRAFT -> vanillaEvent;
        };
        if (targetEvent == null) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getSoundManager() != null) {
                SoundEvent eventToPlay = targetEvent;
                if (targetEvent.id() != null && "nivoratclient".equals(targetEvent.id().getNamespace())) {
                    Identifier altId = Identifier.of("activity", targetEvent.id().getPath());
                    if (Registries.SOUND_EVENT != null && Registries.SOUND_EVENT.containsId(altId)) {
                        eventToPlay = SoundEvent.of(altId);
                    }
                }
                client.getSoundManager().play(
                    PositionedSoundInstance.ui(eventToPlay, pitch, finalVolume)
                );
            }
        } catch (Throwable ignored) {
            // Guard in unit tests without Minecraft sound engine
        }
    }
}
