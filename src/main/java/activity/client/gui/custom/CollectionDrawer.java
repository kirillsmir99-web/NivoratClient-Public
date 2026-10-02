package activity.client.gui.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CollectionDrawer {
    private static NativeCollectionScreen screen;
    private CollectionDrawer() {}
    public static boolean isOpen() { return screen != null; }
    public static NativeCollectionScreen screen() { return screen; }
    public static float width() { return isOpen() ? 210 : 0; }
    public static void open(NativeCollectionScreen.Kind kind) {
        var client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof activity.client.gui.custom.api.ui.UI)) { client.setScreen(new NativeCollectionScreen(client.currentScreen, kind)); return; }
        activity.client.gui.custom.api.ui.UI.INSTANCE.prepareCollectionDrawer();
        screen = new NativeCollectionScreen(client.currentScreen, kind, true);
        screen.init(client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
    }
    public static void close() { if (screen == null) return; var old = screen; screen = null; old.persist(); }
    public static void render(DrawContext context, int x, int y, float delta) { if (screen != null) screen.render(context, x, y, delta); }
}
