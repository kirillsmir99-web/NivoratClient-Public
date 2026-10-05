package activity.client.presence;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DevBadgeText {
    private static final Style BADGE_STYLE = Style.EMPTY.withFont(
        new StyleSpriteSource.Font(Identifier.of("activity", "dev_badge")));
    private static final Style SEPARATOR_STYLE = Style.EMPTY
        .withFont(StyleSpriteSource.DEFAULT)
        .withColor(0xC184FF);

    private DevBadgeText() {}

    public static Text prefix(Text original) {
        if (original == null) {
            return null;
        }
        if (original.getString().contains(NivoratDev.BADGE_GLYPH)) {
            return original;
        }
        MutableText root = Text.empty();
        root.append(Text.literal(NivoratDev.BADGE_GLYPH).setStyle(BADGE_STYLE));
        root.append(Text.literal(NivoratDev.BADGE_SEPARATOR).setStyle(SEPARATOR_STYLE));
        root.append(original.copy());
        return root;
    }
}
