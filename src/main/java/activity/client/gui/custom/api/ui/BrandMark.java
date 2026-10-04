package activity.client.gui.custom.api.ui;

import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.color.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public final class BrandMark {
    private static final String LOGO = "activity:textures/gui/client_logo.png";
    private static final Identifier LOGO_ID = Identifier.of("activity", "textures/gui/client_logo.png");
    private static volatile boolean logoRegistered = false;

    private BrandMark() {}

    private static void ensureLoaded() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null) return;
        if (logoRegistered) {
            AbstractTexture existing = client.getTextureManager().getTexture(LOGO_ID);
            if (existing != null && existing.getGlTexture() != null) {
                return;
            }
            logoRegistered = false;
        }
        try {
            InputStream stream = BrandMark.class.getResourceAsStream("/assets/activity/textures/gui/client_logo.png");
            if (stream != null) {
                try (stream) {
                    NativeImage image = NativeImage.read(stream);
                    if (image != null) {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "activity_client_logo", image);
                        texture.upload();
                        client.getTextureManager().registerTexture(LOGO_ID, (AbstractTexture) texture);
                        logoRegistered = true;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void draw(float x, float y, float size, float alpha) {
        if (size <= 0 || alpha <= .005f) return;
        ensureLoaded();
        if (!Render2D.imageReady(LOGO)) return;
        Render2D.image(LOGO, x, y, size, size,
            ColorUtil.rgba(255, 255, 255, Math.round(255 * Math.max(0, Math.min(1, alpha)))));
    }
}
