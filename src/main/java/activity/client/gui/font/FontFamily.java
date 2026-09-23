package activity.client.gui.font;

import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

public enum FontFamily {
    MINECRAFT("minecraft", "activity.font.minecraft", Identifier.ofVanilla("default"), StyleSpriteSource.DEFAULT),
    ONEST("onest", "activity.font.onest", Identifier.of("activity", "onest"), new StyleSpriteSource.Font(Identifier.of("activity", "onest"))),
    INTER("inter", "activity.font.inter", Identifier.of("activity", "inter"), new StyleSpriteSource.Font(Identifier.of("activity", "inter"))),
    MANROPE("manrope", "activity.font.manrope", Identifier.of("activity", "manrope"), new StyleSpriteSource.Font(Identifier.of("activity", "manrope"))),
    RUBIK("rubik", "activity.font.rubik", Identifier.of("activity", "rubik"), new StyleSpriteSource.Font(Identifier.of("activity", "rubik"))),

    DEFAULT("default", "activity.font.minecraft", Identifier.ofVanilla("default"), StyleSpriteSource.DEFAULT),
    RETRO_PIXEL("retro_pixel", "activity.font.retro_pixel", Identifier.of("activity", "retro_pixel"), new StyleSpriteSource.Font(Identifier.of("activity", "retro_pixel")));

    private final String id;
    private final String translationKey;
    private final Identifier fontId;
    private final StyleSpriteSource spriteSource;

    FontFamily(String id, String translationKey, Identifier fontId, StyleSpriteSource spriteSource) {
        this.id = id;
        this.translationKey = translationKey;
        this.fontId = fontId;
        this.spriteSource = spriteSource;
    }

    public String getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public Identifier getFontId() {
        return this.fontId;
    }

    public StyleSpriteSource getSpriteSource() {
        return this.spriteSource;
    }

    public static FontFamily fromId(String id) {
        if (id == null || id.isBlank()) {
            return MINECRAFT;
        }
        String normalized = id.trim().toLowerCase();
        if ("default".equals(normalized)) {
            return MINECRAFT;
        }
        for (FontFamily family : values()) {
            if (family.id.equals(normalized)) {
                return family;
            }
        }
        return MINECRAFT;
    }
}
