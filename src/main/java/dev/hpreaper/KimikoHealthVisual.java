package dev.hpreaper;

import activity.client.config.ActivityConfigManager;
import activity.client.gui.custom.UnifiedHudRender;
import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.render2d.Render2DCoordinateSpace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

public final class KimikoHealthVisual {
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static float visibility;
    private static long lastFrame;
    private KimikoHealthVisual() {}

    public static void armor(DrawContext context, LivingEntity entity, int x, int y) {
        if (entity == null || !ActivityConfigManager.getConfig().hpReaperShowArmor) return;
        int count = 0;
        for (var slot : ARMOR) if (!entity.getEquippedStack(slot).isEmpty()) count++;
        if (count == 0) return;
        UnifiedHudRender.card(x, y, 4 + count * 16, 20);
        int offset = 0;
        for (var slot : ARMOR) {
            var stack = entity.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            context.getMatrices().pushMatrix();
            Render2DCoordinateSpace.applyGuiScaleIndependence(context.getMatrices());
            context.getMatrices().translate(x + 2 + offset, y + 2);
            context.drawItem(stack, 0, 0);
            context.getMatrices().popMatrix();
            offset += 16;
        }
    }

    public static void lowHealth(DrawContext context, MinecraftClient client) {
        var cfg = ActivityConfigManager.getConfig();
        boolean show = client.player != null && cfg.hpReaperLowHealthHearts
                && client.player.getHealth() <= cfg.hpReaperLowHealthThreshold;
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? .05f : Math.min(.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        visibility += ((show ? 1f : 0f) - visibility) * (1f - (float) Math.exp(-dt * 15f));
        if (visibility < .005f || client.player == null) return;
        int health = (int) Math.ceil(client.player.getHealth());
        int max = Math.min(100, (int) Math.ceil(client.player.getMaxHealth()));
        int absorption = (int) Math.ceil(client.player.getAbsorptionAmount());
        int hearts = (max + 1) / 2;
        int total = hearts + Math.min(50, (absorption + 1) / 2);
        float x = (Position.screenWidth() - Math.min(10, total) * 8f) / 2f;
        float y = Position.screenHeight() * .65f;
        int color = client.player.hasStatusEffect(StatusEffects.POISON) ? 0xff84af34
                : client.player.hasStatusEffect(StatusEffects.WITHER) ? 0xff777184 : 0xffff536e;
        try (var frame = UnifiedHudRender.beginNative(context)) {
            for (int i = 0; i < total; i++) {
                float hx = x + i % 10 * 8f, hy = y + i / 10 * 10f;
                Fonts.HEART.msdf("A", hx - .5f, hy - .5f, 10f, (Math.round(210 * visibility) << 24) | 0x11131b);
                int units = i < hearts ? health - i * 2 : absorption - (i - hearts) * 2;
                if (units <= 0) continue;
                int fill = i < hearts ? color : 0xffffd76c;
                if (units == 1) activity.client.gui.custom.utils.render.render2d.Render2D.pushScissor(context, hx, hy, 4.5f, 9f);
                Fonts.HEART.msdf("A", hx, hy, 9f, (Math.round(255 * visibility) << 24) | (fill & 0xffffff));
                if (units == 1) activity.client.gui.custom.utils.render.render2d.Render2D.popScissor(context);
            }
        }
    }
}
