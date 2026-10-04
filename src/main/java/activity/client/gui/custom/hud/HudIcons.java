package activity.client.gui.custom.hud;

import activity.client.gui.custom.utils.render.render2d.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public final class HudIcons {
    private static final String ATLAS = "activity:textures/gui/hud_icons.png";
    private static final Identifier ATLAS_ID = Identifier.of("activity", "textures/gui/hud_icons.png");
    public static final int TIMER = 10;
    public static final int CHAT = 11;
    private static volatile boolean textureRegistered = false;

    private HudIcons() {}

    private static void ensureLoaded() {
        if (textureRegistered) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getTextureManager() != null) {
            try {
                InputStream stream = HudIcons.class.getResourceAsStream("/assets/activity/textures/gui/hud_icons.png");
                if (stream != null) {
                    try (stream) {
                        NativeImage image = NativeImage.read(stream);
                        if (image != null) {
                            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "activity_hud_icons", image);
                            texture.upload();
                            client.getTextureManager().registerTexture(ATLAS_ID, (AbstractTexture) texture);
                            textureRegistered = true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public static int slot(Item item) {
        if (item == Items.ENDER_PEARL) return 0;
        if (item == Items.TNT_MINECART || item == Items.TNT) return 1;
        if (item == Items.WIND_CHARGE) return 2;
        if (item == Items.BOW || item == Items.CROSSBOW) return 3;
        if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) return 4;
        if (item == Items.CHORUS_FRUIT) return 5;
        if (item == Items.COOKED_BEEF) return 6;
        if (item == Items.SHIELD) return 7;
        if (item == Items.MACE) return 8;
        if (item == Items.TRIDENT) return 9;
        return TIMER;
    }

    public static void draw(int slot, float x, float y, float size, int color) {
        ensureLoaded();
        if (!Render2D.imageReady(ATLAS)) return;
        Render2D.imageUv(ATLAS, x, y, size, size, 0f, 0f,
                slot / 12f, 0f, (slot + 1) / 12f, 1f, color);
    }
}
