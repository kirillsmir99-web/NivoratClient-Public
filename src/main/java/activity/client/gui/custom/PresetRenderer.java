package activity.client.gui.custom;

import activity.client.config.preset.LocalPresets;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;
import activity.client.gui.custom.utils.sounds.Sounds;
import activity.client.module.api.IModule;
import activity.client.module.api.ModuleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import java.nio.file.Path;
import java.util.*;

public final class PresetRenderer {
    private enum View { LIST, CREATE, MODULES, PREVIEW }
    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean contains(float mx, float my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    }
    private View view = View.LIST;
    private final List<Hit> hits = new ArrayList<>();
    private final EnumSet<LocalPresets.Part> parts = EnumSet.complementOf(EnumSet.of(LocalPresets.Part.CART_PROFILE));
    private final List<IModule> modules = ModuleRegistry.getAll();
    private final Set<String> moduleIds = new HashSet<>();
    private List<LocalPresets.Entry> entries = List.of();
    private Set<String> favorites = Set.of();
    private LocalPresets.Template template = LocalPresets.Template.CURRENT;
    private LocalPresets.Preview preview;
    private List<String> changes = List.of();
    private Path importPath, deleting, previewPath;
    private String name = "", message = "";
    private boolean typing, drawingBody;
    private float x, y, width, height, alpha, scroll, contentHeight;
    private float clipTop, clipBottom;

    public PresetRenderer() { for (var module : modules) moduleIds.add(module.getId()); }
    private String tr(String ru, String en) { return activity.client.i18n.LocalizationService.isRussianPreferred() ? ru : en; }
    public void open() { view = View.LIST; scroll = 0; typing = false; hits.clear(); reload(); }
    private void reload() {
        run(() -> { entries = LocalPresets.list(); favorites = LocalPresets.favorites(); });
    }
    private interface Action { void execute() throws Exception; }
    private void run(Action action) {
        try { action.execute(); }
        catch (Exception error) { message = Objects.toString(error.getMessage(), tr("Не удалось выполнить действие", "Action failed")); }
    }
    private void navigate(View next) { view = next; scroll = 0; typing = false; hits.clear(); Sounds.play("select_category"); }
    public void showPreview(Path path, boolean importing) throws java.io.IOException {
        preview = LocalPresets.read(path); changes = LocalPresets.changes(preview);
        previewPath = path; importPath = importing ? path : null; deleting = null; navigate(View.PREVIEW);
    }
    private void back() {
        if (view == View.MODULES) navigate(View.CREATE);
        else { preview = null; importPath = null; deleting = null; previewPath = null; navigate(View.LIST); }
    }
    public boolean click(float mx, float my, int button) {
        if (mx < x || mx > x + width || my < y || my > y + height) return false;
        if (button != 0) return true;
        typing = false;
        for (Hit hit : List.copyOf(hits)) if (hit.contains(mx, my)) { hit.action.run(); return true; }
        return true;
    }
    public void scroll(double delta) { scroll = Math.clamp(scroll - (float)delta * 22, 0, Math.max(0, contentHeight - (clipBottom - clipTop))); }
    public boolean keyPressed(KeyInput key) {
        if (key.key() == 256 && view != View.LIST) { back(); return true; }
        if (!typing) return false;
        if (key.key() == 259 && !name.isEmpty()) name = name.substring(0, name.offsetByCodePoints(name.length(), -1));
        if ((key.modifiers() & 2) != 0 && key.key() == 86) {
            String pasted = MinecraftClient.getInstance().keyboard.getClipboard().replaceAll("[\\p{Cntrl}]", "");
            name = (name + pasted).codePoints().limit(32).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }
        if (key.key() == 257) create();
        return true;
    }
    public boolean charTyped(CharInput input) {
        if (!typing || name.codePointCount(0, name.length()) >= 32 || Character.isISOControl(input.codepoint())) return false;
        name += new String(Character.toChars(input.codepoint())); return true;
    }
    private void create() {
        run(() -> {
            LocalPresets.create(name, template, parts, moduleIds.size() == modules.size() ? null : moduleIds);
            name = ""; reload(); navigate(View.LIST); message = tr("Пресет сохранён", "Preset saved");
        });
    }
    private void apply() {
        run(() -> {
            if (deleting != null) { LocalPresets.delete(deleting); message = tr("Пресет удалён", "Preset deleted"); }
            else { if (importPath != null) LocalPresets.importFile(importPath); LocalPresets.apply(preview); message = tr("Пресет применён", "Preset applied"); }
            reload(); back();
        });
    }
    public void render(DrawContext context, float x, float y, float width, float height, float alpha) {
        this.x = x; this.y = y; this.width = width; this.height = height; this.alpha = alpha; hits.clear(); drawingBody = false;
        float inner = width - 16;
        text(view == View.LIST ? tr("Пресеты", "Presets") : view == View.CREATE ? tr("Новый пресет", "New preset") : view == View.MODULES ? tr("Выбор модулей", "Select modules") : tr("Предпросмотр", "Preview"), x + 8, y + 9, 9, -1, inner - 45);
        if (view != View.LIST) button(x + width - 45, y + 5, 37, tr("Назад", "Back"), this::back);
        float bodyTop = y + 32;
        if (view == View.LIST) {
            button(x + 8, bodyTop, inner / 2 - 3, tr("Создать пресет", "Create preset"), () -> navigate(View.CREATE));
            button(x + 11 + inner / 2, bodyTop, inner / 2 - 3, tr("Импорт из файла", "Import file"), () -> run(() -> {
                String path = MinecraftClient.getInstance().keyboard.getClipboard().trim();
                if (path.startsWith("\"") && path.endsWith("\"")) path = path.substring(1, path.length() - 1);
                showPreview(Path.of(path), true);
            }));
            bodyTop += 29;
        }
        clipTop = bodyTop; clipBottom = y + height - (view == View.LIST ? 23 : 52);
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (clipBottom - clipTop)));
        Render2D.pushScissor(context, x + 6, clipTop, width - 12, clipBottom - clipTop);
        float row = clipTop - scroll; drawingBody = true;
        if (view == View.LIST) {
            if (entries.isEmpty()) text(tr("Сохранённых пресетов пока нет", "No saved presets yet"), x + 12, row + 12, 7, 0xffbdc7d7, inner - 8);
            for (var entry : entries) {
                card(context, x + 8, row, inner, 43);
                text(entry.name(), x + 16, row + 8, 8, -1, inner - 72);
                text(tr("Просмотреть изменения", "Preview changes"), x + 16, row + 25, 6, 0xffbdc7d7, inner - 72);
                hit(x + 8, row, inner - 55, 43, () -> run(() -> showPreview(entry.path(), false)));
                icon(NvIcons.PINNED, x + width - 54, row + 11, favorites.contains(entry.path().getFileName().toString()) ? ClientAccent.accent(255) : 0xff8490a4,
                    () -> run(() -> { LocalPresets.toggleFavorite(entry.path()); reload(); Sounds.play("select_category"); }));
                icon(NvIcons.DELETE, x + width - 30, row + 11, 0xffdcb0b0, () -> run(() -> {
                    showPreview(entry.path(), false); deleting = entry.path();
                    changes = List.of(tr("Удалить «", "Delete ‘") + entry.name() + tr("»?", "’ ?"), tr("Файл сохранится в папке deleted.", "The file stays in the deleted folder."));
                }));
                row += 49;
            }
        } else if (view == View.CREATE) {
            text(tr("Название", "Name"), x + 10, row + 3, 7, -1, inner);
            Render2D.rect(x + 8, row + 16, inner, 23, 5, color(0x50323a48));
            text(name.isEmpty() ? tr("Введите название", "Enter a name") : name + (typing ? "|" : ""), x + 15, row + 24, 7, -1, inner - 14);
            hit(x + 8, row + 16, inner, 23, () -> typing = true); row += 49;
            text(tr("Шаблон", "Template"), x + 10, row + 3, 7, -1, inner);
            button(x + 8, row + 16, inner, template == LocalPresets.Template.CURRENT ? tr("Текущие настройки", "Current settings") : tr("Модули по умолчанию", "Default modules"), () -> { template = template == LocalPresets.Template.CURRENT ? LocalPresets.Template.DEFAULTS : LocalPresets.Template.CURRENT; Sounds.play("select_category"); }); row += 49;
            text(tr("Что сохранить", "Include"), x + 10, row + 3, 7, -1, inner); row += 19;
            String[] labels = activity.client.i18n.LocalizationService.isRussianPreferred() ? new String[]{"Модули", "Бинды", "HUD", "Оформление", "Меню", "Звуки", "Темы", "Профиль Автокарта"} : new String[]{"Modules", "Bindings", "HUD", "Appearance", "Menu", "Sounds", "Themes", "AutoCart profile"};
            for (int i = 0; i < labels.length; i++) {
                var part = LocalPresets.Part.values()[i];
                toggle(x + 8, row, inner, labels[i], parts.contains(part), () -> { if (!parts.remove(part)) parts.add(part); Sounds.play("select_category"); }); row += 26;
            }
            button(x + 8, row, inner, tr("Выбрать модули: ", "Select modules: ") + moduleIds.size() + " / " + modules.size(), () -> navigate(View.MODULES)); row += 28;
        } else if (view == View.MODULES) {
            button(x + 8, row, inner / 2 - 3, tr("Все", "All"), () -> { for (var module : modules) moduleIds.add(module.getId()); });
            button(x + 11 + inner / 2, row, inner / 2 - 3, tr("Ни одного", "None"), moduleIds::clear); row += 30;
            for (var module : modules) {
                toggle(x + 8, row, inner, VisualText.moduleName(module), moduleIds.contains(module.getId()), () -> { if (!moduleIds.remove(module.getId())) moduleIds.add(module.getId()); Sounds.play("select_category"); }); row += 26;
            }
        } else {
            text(preview.name(), x + 10, row + 4, 8, -1, inner - 4); row += 25;
            if (changes.isEmpty()) text(tr("Настройки уже совпадают", "Settings already match"), x + 10, row + 5, 7, 0xffbdc7d7, inner);
            for (String change : changes) {
                for (String line : wrap(change, inner - 16)) { text(line, x + 12, row + 5, 6.5f, 0xffd9dfea, inner - 8); row += 12; }
                row += 7;
            }
        }
        contentHeight = Math.max(0, row - (clipTop - scroll));
        Render2D.popScissor(context); drawingBody = false;
        if (contentHeight > clipBottom - clipTop) {
            float track = clipBottom - clipTop, thumb = Math.max(16, track * track / contentHeight);
            Render2D.rect(x + width - 4, clipTop + (track - thumb) * scroll / (contentHeight - track), 2, thumb, 1, color(ClientAccent.accent(150)));
        }
        if (view == View.CREATE) button(x + 8, y + height - 45, inner, tr("Сохранить пресет", "Save preset"), this::create);
        if (view == View.MODULES) button(x + 8, y + height - 45, inner, tr("Готово", "Done"), this::back);
        if (view == View.PREVIEW) {
            button(x + 8, y + height - 45, inner / 2 - 3, deleting != null ? tr("Удалить", "Delete") : tr("Применить", "Apply"), this::apply);
            button(x + 11 + inner / 2, y + height - 45, inner / 2 - 3, deleting != null ? tr("Отмена", "Cancel") : tr("Экспорт файла", "Export file"), deleting != null ? this::back : () -> run(() -> {
                Path saved = previewPath;
                if (saved == null) throw new java.io.IOException(tr("Файл пресета не найден", "Preset file not found"));
                Path exported = LocalPresets.export(saved); MinecraftClient.getInstance().keyboard.setClipboard(exported.toAbsolutePath().toString()); message = tr("Путь к файлу скопирован", "File path copied");
            }));
        }
        text(message, x + 9, y + height - 15, 6, 0xffbdc7d7, inner);
    }
    private List<String> wrap(String value, float width) {
        List<String> lines = new ArrayList<>(); String line = "";
        for (String word : value.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && Fonts.MONTSERRAT_MEDIUM.width(next, 6.5f) > width) { lines.add(line); line = word; } else line = next;
        }
        if (!line.isEmpty()) lines.add(line); return lines;
    }
    private int color(int value) { return ((int)((value >>> 24) * alpha) << 24) | (value & 0xffffff); }
    private void text(String value, float x, float y, float size, int color, float width) { Fonts.MONTSERRAT_MEDIUM.draw(CustomRender.fit(value, width, size), x, y, size, color(color)); }
    private void card(DrawContext context, float x, float y, float w, float h) { CustomRender.panel(context, x, y, w, h, 6, alpha); }
    private void hit(float x, float y, float w, float h, Runnable action) {
        float top = Math.max(y, clipTop), bottom = Math.min(y + h, clipBottom);
        if (bottom > top) hits.add(new Hit(x, top, w, bottom - top, action));
    }
    private void button(float x, float y, float w, String label, Runnable action) {
        Render2D.rect(x, y, w, 22, 5, color(ClientAccent.accentSoft(75))); text(label, x + 7, y + 7, 7, -1, w - 14);
        if (!drawingBody) hits.add(new Hit(x, y, w, 22, action)); else hit(x, y, w, 22, action);
    }
    private void icon(String glyph, float x, float y, int color, Runnable action) { Fonts.NV.msdf(glyph, x + 4, y + 4, 9, color(color)); hit(x, y, 21, 21, action); }
    private void toggle(float x, float y, float w, String label, boolean selected, Runnable action) {
        Render2D.rect(x, y, w, 22, 5, color(selected ? ClientAccent.accentSoft(95) : 0x40343a48));
        Render2D.outline(x + w - 18, y + 7, 8, 8, 2, .6f, color(selected ? ClientAccent.accentBright(255) : 0xff929dad));
        if (selected) Fonts.NV.msdf(NvIcons.CHECK, x + w - 17, y + 8, 6, color(-1));
        text(label, x + 7, y + 7, 7, -1, w - 32); hit(x, y, w, 22, action);
    }
}
