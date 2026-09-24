package activity.client.presence;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DevBadgeText {
    private static final Style BADGE_STYLE = Style.EMPTY.withFont(
        new StyleSpriteSource.Font(Identifier.of("nivoratclient", "dev_badge")));

    private DevBadgeText() {}

    public static Text prefix(Text original) {
        if (original == null || original.getString().contains(NivoratDev.BADGE_GLYPH)) {
            return original;
        }
        MutableText badge = Text.literal(NivoratDev.BADGE_GLYPH).setStyle(BADGE_STYLE);
        badge.append(Text.literal(NivoratDev.BADGE_SEPARATOR).styled(style -> style.withColor(0xC184FF)));
        return badge.append(original);
    }
}
