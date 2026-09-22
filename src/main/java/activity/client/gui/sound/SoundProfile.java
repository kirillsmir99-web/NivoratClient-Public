package activity.client.gui.sound;

import net.minecraft.text.Text;

public enum SoundProfile {
    SERENE("serene", "activity.sound_profile.serene", "Nivorat Serene"),
    CLASSIC("classic", "activity.sound_profile.classic", "Nivorat Classic"),
    MINECRAFT("minecraft", "activity.sound_profile.minecraft", "Minecraft");

    private final String id;
    private final String translationKey;
    private final String displayName;

    SoundProfile(String id, String translationKey, String displayName) {
        this.id = id;
        this.translationKey = translationKey;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Text getDisplayText() {
        return Text.translatable(this.translationKey);
    }

    public static SoundProfile fromId(String id) {
        if (id == null || id.isBlank()) return SERENE;
        for (SoundProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id) || profile.name().equalsIgnoreCase(id)) {
                return profile;
            }
        }
        return SERENE;
    }
}
