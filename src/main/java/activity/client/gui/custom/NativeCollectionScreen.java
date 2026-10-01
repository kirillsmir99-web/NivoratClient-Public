package activity.client.gui.custom;

import activity.client.gui.custom.api.drags.Position;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import dev.audio.AudioSyncClient;
import dev.audio.AudioSyncConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public final class NativeCollectionScreen extends Screen {
    public enum Kind { GG, COOLDOWN }
    private record Choice(String key, String label) {}
    private final Screen parent;
    private final Kind kind;
    private final List<Choice> inventory = new ArrayList<>();
    private List<Choice> visible = List.of();
    private String query = "";
    private boolean typing = true;
    private int editing = -1;
    private int scroll;
    private float x() { return (Position.screenWidth() - 320) / 2; }
    private float y() { return Math.max(8, (Position.screenHeight() - 280) / 2); }
    private boolean ru() { return activity.client.i18n.LocalizationService.isRussianPreferred(); }

    public NativeCollectionScreen(Screen parent, Kind kind) {
        super(Text.literal(kind == Kind.GG ? "Auto GG" : "Cooldown"));
        this.parent = parent; this.kind = kind;
    }
    @Override protected void init() {
        if (kind == Kind.COOLDOWN) {
            inventory.clear();
            for (var item : Registries.ITEM) {
                if (item == net.minecraft.item.Items.AIR) continue;
                inventory.add(new Choice("id:" + Registries.ITEM.getId(item), item.getName().getString()));
            }
            if (client.player != null) for (int i = 0; i < 9; i++) {
                var stack = client.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.contains(DataComponentTypes.CUSTOM_NAME)) {
                    Choice choice = new Choice("name:" + stack.getName().getString(), stack.getName().getString());
                    if (!inventory.contains(choice)) inventory.add(0, choice);
                }
            }
            for (String key : CooldownSelections.values()) if (key.startsWith("name:") && inventory.stream().noneMatch(c -> c.key.equals(key)))
                inventory.add(0, new Choice(key, key.substring(5)));
        }
        refresh();
    }
    private void refresh() {
        if (kind == Kind.GG) visible = AudioSyncClient.CONFIG.phrases.stream().map(p -> new Choice(p, p)).toList();
        else {
            String search = CooldownSelections.normalize(query);
            var chosen = CooldownSelections.values();
            visible = inventory.stream().filter(c -> search.isEmpty() || CooldownSelections.normalize(c.label + " " + c.key).contains(search))
                    .sorted(java.util.Comparator.comparing((Choice c) -> !chosen.contains(c.key))).toList();
        }
        scroll = Math.clamp(scroll, 0, Math.max(0, visible.size() - 8));
    }
    private void saveGG() {
        var cfg = AudioSyncClient.CONFIG;
        cfg.selected = Math.clamp(cfg.selected, 0, cfg.phrases.size() - 1);
        cfg.favorites.removeIf(p -> !cfg.phrases.contains(p));
        cfg.save();
        activity.client.config.ActivityConfigManager.getConfig().autoGGPhrase = cfg.currentPhrase();
        activity.client.config.ActivityConfigManager.markDirty();
        refresh();
    }
    private void submit() {
        String phrase = query.trim();
        if (kind != Kind.GG || phrase.isEmpty()) return;
        var cfg = AudioSyncClient.CONFIG;
        if (editing >= 0 && editing < cfg.phrases.size()) {
            String previous = cfg.phrases.get(editing);
            if (cfg.phrases.contains(phrase) && !previous.equals(phrase)) return;
            cfg.phrases.set(editing, phrase);
            if (cfg.favorites.remove(previous)) cfg.favorites.add(phrase);
        } else {
            if (cfg.phrases.size() >= AudioSyncConfig.MAX_PHRASES || cfg.phrases.stream().anyMatch(p -> p.equalsIgnoreCase(phrase))) return;
            cfg.phrases.add(phrase);
        }
        query = ""; editing = -1; saveGG();
    }
    @Override public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) { close(); return true; }
        if (!typing) return false;
        if (input.key() == 257) { submit(); return true; }
        if (input.key() == 259 && !query.isEmpty()) { query = query.substring(0, query.offsetByCodePoints(query.length(), -1)); scroll = 0; refresh(); return true; }
        if ((input.modifiers() & 2) != 0 && input.key() == 86) {
            String clipboard = client.keyboard.getClipboard().replace('\n', ' ').replace('\r', ' ');
            query = (query + clipboard).substring(0, Math.min(64, query.length() + clipboard.length())); scroll = 0; refresh(); return true;
        }
        return true;
    }
    @Override public boolean charTyped(CharInput input) {
        if (!typing || query.length() >= 64 || Character.isISOControl(input.codepoint())) return false;
        query += new String(Character.toChars(input.codepoint())); scroll = 0; refresh(); return true;
    }
    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return true;
        float mx = Position.mouseX() - x(), my = Position.mouseY() - y();
        if (mx >= 288 && my >= 8 && my <= 30) { close(); return true; }
        typing = mx >= 12 && mx <= (kind == Kind.GG ? 252 : 308) && my >= 48 && my <= 72;
        if (kind == Kind.GG && mx >= 260 && mx <= 308 && my >= 48 && my <= 72) { submit(); return true; }
        if (my >= 92 && my < 252 && mx >= 12 && mx <= 308) {
            int index = scroll + (int) ((my - 92) / 20);
            if (index >= visible.size()) return true;
            var choice = visible.get(index);
            if (kind == Kind.COOLDOWN) { CooldownSelections.toggle(choice.key); refresh(); }
            else {
                var cfg = AudioSyncClient.CONFIG;
                if (mx >= 285 && cfg.phrases.size() > 1) { cfg.phrases.remove(index); editing = -1; query = ""; }
                else if (mx < 36) { if (!cfg.favorites.remove(choice.key)) cfg.favorites.add(choice.key); }
                else { editing = index; query = choice.label; typing = true; }
                saveGG();
            }
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.clamp(scroll - (int) Math.signum(vertical) * 3, 0, Math.max(0, visible.size() - 8)); return true;
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try (var frame = UnifiedHudRender.beginNative(context)) {
            float x = x(), y = y();
            CustomRender.panel(context, x, y, 320, 280, 10, 1);
            Fonts.MONTSERRAT_MEDIUM.draw(kind == Kind.GG ? "Auto GG" : "Cooldown", x + 12, y + 12, 10, -1);
            Fonts.NV.msdf(NvIcons.CLOSE, x + 296, y + 14, 8, 0xffccd4e3);
            Fonts.MONTSERRAT_MEDIUM.draw(kind == Kind.GG ? (ru() ? "Фразы • избранные отправляются по умолчанию" : "Phrases • favorites are sent by default")
                    : (ru() ? "Предметы и переименованные предметы хотбара" : "Items and renamed hotbar items"), x + 12, y + 31, 6, 0xffb7c0d1);
            Render2D.rect(x + 12, y + 48, kind == Kind.GG ? 240 : 296, 24, 5, 0xff181c28);
            Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(query.isEmpty() ? (kind == Kind.GG ? (ru() ? "Введите фразу…" : "Enter a phrase…") : (ru() ? "Название или ID предмета…" : "Item name or ID…")) : query, 220, 7), x + 20, y + 56, 7, query.isEmpty() ? 0xff929fb6 : -1);
            if (kind == Kind.GG) {
                Render2D.rect(x + 260, y + 48, 48, 24, 5, ClientAccent.accent(220));
                Fonts.NV.msdf(editing < 0 ? NvIcons.ADD : NvIcons.CHECK, x + 280, y + 56, 8, -1);
            }
            var selected = kind == Kind.COOLDOWN ? CooldownSelections.values() : AudioSyncClient.CONFIG.favorites;
            for (int row = 0; row < 8 && scroll + row < visible.size(); row++) {
                Choice choice = visible.get(scroll + row);
                float rowY = y + 92 + row * 20;
                boolean chosen = selected.contains(choice.key);
                Render2D.rect(x + 12, rowY, 296, 18, 4, chosen ? ClientAccent.accent(65) : 0x991b202c);
                Fonts.NV.msdf(chosen ? NvIcons.CHECK : NvIcons.PINNED, x + 20, rowY + 5, 7, chosen ? ClientAccent.accentBright(255) : 0xff7e8ca4);
                Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(choice.label, 240, 7), x + 38, rowY + 5, 7, 0xffe2e7f1);
                if (kind == Kind.GG) Fonts.NV.msdf(NvIcons.DELETE, x + 292, rowY + 5, 7, 0xffc4a6b4);
            }
            Fonts.MONTSERRAT_MEDIUM.draw(kind == Kind.GG ? AudioSyncClient.CONFIG.phrases.size() + " / " + AudioSyncConfig.MAX_PHRASES
                    : (ru() ? "Только реальные кулдауны игры или сервера" : "Only actual game or server cooldowns"), x + 12, y + 262, 6, 0xffa5aec0);
        }
    }
    @Override public void close() { if (kind == Kind.GG) saveGG(); client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
