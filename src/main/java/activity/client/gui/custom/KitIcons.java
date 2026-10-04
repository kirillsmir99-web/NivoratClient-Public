package activity.client.gui.custom;

import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public final class KitIcons {
    private static final String ATLAS = "activity:textures/gui/kit_icons.png";
    private static final Identifier ATLAS_ID = Identifier.of("activity", "textures/gui/kit_icons.png");
    private static final int TOTAL_SLOTS = 11;
    private static final float SLOT_STRIDE = 72.0f;
    private static final float CONTENT_SIZE = 64.0f;
    private static final float PAD = 4.0f;
    private static final float TOTAL_W = 792.0f;
    private static final float TOTAL_H = 72.0f;
    private static volatile boolean textureRegistered = false;

    private KitIcons() {}

    private static void ensureLoaded() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null) return;
        if (textureRegistered) {
            AbstractTexture existing = client.getTextureManager().getTexture(ATLAS_ID);
            if (existing != null && existing.getGlTexture() != null) {
                return;
            }
            textureRegistered = false;
        }
        try {
            InputStream stream = KitIcons.class.getResourceAsStream("/assets/activity/textures/gui/kit_icons.png");
            if (stream != null) {
                try (stream) {
                    NativeImage image = NativeImage.read(stream);
                    if (image != null) {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "activity_kit_icons", image);
                        texture.upload();
                        client.getTextureManager().registerTexture(ATLAS_ID, (AbstractTexture) texture);
                        textureRegistered = true;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void draw(Category category, float x, float y, float size, int color) {
        int slot = switch (category) {
            case VISUALS -> 0;
            case NPOT -> 1;
            case CRYSTAL -> 2;
            case UHC -> 3;
            case SMP -> 4;
            case MACE -> 5;
            case BEAST -> 6;
            case SWORD -> 7;
            case AXE -> 8;
            case DPOT -> 9;
            case CART -> 10;
            default -> -1;
        };
        if (slot < 0) {
            drawFallback(category, x, y, size, color);
            return;
        }
        ensureLoaded();
        if (!Render2D.imageReady(ATLAS)) {
            drawFallback(category, x, y, size, color);
            return;
        }
        int renderCol = (color & 0xFF000000) | 0x00FFFFFF;
        float u1 = ((float) slot * SLOT_STRIDE + PAD) / TOTAL_W;
        float u2 = ((float) slot * SLOT_STRIDE + PAD + CONTENT_SIZE) / TOTAL_W;
        float v1 = PAD / TOTAL_H;
        float v2 = (PAD + CONTENT_SIZE) / TOTAL_H;
        Render2D.imageUv(ATLAS, x, y, size, size, 0.0f, 0.0f, u1, v1, u2, v2, renderCol);
    }

    private static void drawFallback(Category category, float x, float y, float size, int color) {
        if (category == Category.PINNED) {
            Fonts.NV.msdf(NvIcons.PINNED, x, y, size, color);
        } else if (category == Category.THEMES) {
            Fonts.NV.msdf(NvIcons.THEMES, x, y, size, color);
        } else if (category == Category.PRESETS) {
            Fonts.NV.msdf(NvIcons.PRESETS, x, y, size, color);
        } else if (category == Category.DISPLAY) {
            Fonts.NV.msdf(NvIcons.SETTINGS, x, y, size, color);
        } else {
            Fonts.NV.msdf(NvIcons.forCategory(category), x, y, size, color);
        }
    }
}
