package activity.client.gui.custom;

import java.awt.Color;
import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.api.modules.Module;
import activity.client.gui.custom.api.modules.settings.impl.BooleanSetting;
import activity.client.gui.custom.api.modules.settings.impl.ColorSetting;
import activity.client.gui.custom.api.modules.settings.impl.ModeSetting;
import activity.client.gui.custom.api.modules.settings.impl.NumberSetting;
import activity.client.gui.custom.api.modules.settings.impl.SeparatorSetting;
import activity.client.gui.custom.api.ui.theme.ClientAccent;
import activity.client.gui.custom.api.ui.theme.ThemeManager;

public final class VisualMaterial extends Module {
    public static final String CLIENT_COLOR_PALETTE = "Палитра";
    public static final String CLIENT_COLOR_ACCENT = "Акцент";
    public static final String CLIENT_COLOR_SPECTRUM = "Спектр";


    public static final String CLIENT_COLOR_THEMES = CLIENT_COLOR_PALETTE;
    public static final String CLIENT_COLOR_CUSTOM = CLIENT_COLOR_ACCENT;
    public static final String CLIENT_COLOR_RAINBOW = CLIENT_COLOR_SPECTRUM;

    public static final String GRADIENT_LINE = "Линия";
    public static final String GRADIENT_FLOW = "Поток";
    public static final String GRADIENT_CORNERS = "Углы";


    public static final String GRADIENT_HORIZONTAL = GRADIENT_LINE;
    public static final String GRADIENT_BLOBS = GRADIENT_FLOW;
    public static final String GRADIENT_SQUARE = GRADIENT_CORNERS;

    public static final String STYLE_GLASS = "Стекло";
    public static final String STYLE_MOSAIC = "Мозаика";

    public static final String STYLE_MONOLITH = STYLE_GLASS;
    public static final String STYLE_SHARDS = STYLE_MOSAIC;

    public static final String DRAG_FREE = "Свободный";
    public static final String DRAG_GHOST = "Призрак";

    private static final float THEME_COLOR_ALPHA = 204.0f;
    private static final VisualMaterial instance = new VisualMaterial();
    private final int[] rainbowPalette = new int[9];

    private final SeparatorSetting rectLayout = this.register(new SeparatorSetting("Материал"));
    public final ModeSetting rectStyle = this.register(new ModeSetting("Стиль", "Стиль оформления материала интерфейса.", STYLE_SHARDS, STYLE_SHARDS));
    public final BooleanSetting activeModuleMosaic = this.register(new BooleanSetting("Мозаика активных модулей", "Отображать эффект мозаики на плашках активных модулей.", true));
    public final BooleanSetting moduleActiveMosaic = activeModuleMosaic;
    public final NumberSetting mosaicScale = this.register(new NumberSetting("Размер осколков", "Масштаб панелей: чем выше значение, тем крупнее осколки.", 1.0, 0.4, 2.5, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicMorph = this.register(new NumberSetting("Деформация", "Амплитуда медленного изменения формы осколков.", 0.4, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicSpeed = this.register(new NumberSetting("Скорость", "Скорость плавного движения осколков.", 0.7, 0.1, 3.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicSeam = this.register(new NumberSetting("Толщина швов", "Визуальная толщина стыков между осколками.", 0.04, 0.01, 0.15, 0.005).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicBevel = this.register(new NumberSetting("Фаска", "Тонкий стеклянный блик вдоль скоса граней осколков.", 0.35, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting mosaicCellGlow = this.register(new NumberSetting("Свечение швов", "Мягкое свечение вдоль стыков осколков.", 0.20, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));
    public final NumberSetting textReadability = this.register(new NumberSetting("Читаемость текста", "Увеличение контраста текста и спокойствия фона панелей.", 0.85, 0.0, 1.0, 0.05).visibleWhen(this::isMosaicStyle));

    public final BooleanSetting waveEdgeEnabled = this.register(new BooleanSetting("Живая кромка", "Мягкая органичная волнистая деформация по внешнему контуру стеклянных поверхностей.", true));
    public final NumberSetting waveEdgeIntensity = this.register(new NumberSetting("Интенсивность", "Изменяет выраженность эффекта.", 0.70, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeSize = this.register(new NumberSetting("Размер волны", "Изменяет высоту и размер выпуклостей.", 0.50, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeDensity = this.register(new NumberSetting("Плотность", "Изменяет количество волн по краю.", 0.50, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeMotion = this.register(new BooleanSetting("Движение", "Плавное перемещение волн вдоль контура.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final NumberSetting waveEdgeSpeed = this.register(new NumberSetting("Скорость", "Изменяет скорость движения текстуры.", 0.50, 0.0, 1.0, 0.05).visibleWhen(() -> this.waveEdgeEnabled.getValue() && this.waveEdgeMotion.getValue()));
    public final NumberSetting waveEdgeGlow = this.register(new NumberSetting("Свечение", "Изменяет яркость света на вершинах волн.", 0.35, 0.0, 1.0, 0.05).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeInterface = this.register(new BooleanSetting("Интерфейс", "Применение живой кромки к главным окнам и панелям интерфейса.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting waveEdgeHud = this.register(new BooleanSetting("HUD", "Применение живой кромки к виджетам HUD на экране.", true).visibleWhen(this.waveEdgeEnabled::getValue));
    public final BooleanSetting activeModuleWaveEdge = this.register(new BooleanSetting("Анимация активных модулей", "Использовать мягкую живую кромку как основной индикатор активного состояния модулей.", true));


    public final BooleanSetting liveEdgeEnabled = waveEdgeEnabled;
    public final NumberSetting liveEdgeIntensity = waveEdgeIntensity;
    public final NumberSetting liveEdgeSize = waveEdgeSize;
    public final NumberSetting liveEdgeDensity = waveEdgeDensity;
    public final BooleanSetting liveEdgeMotion = waveEdgeMotion;
    public final NumberSetting liveEdgeSpeed = waveEdgeSpeed;
    public final NumberSetting liveEdgeGlow = waveEdgeGlow;
    public final BooleanSetting liveEdgeInterface = waveEdgeInterface;
    public final BooleanSetting liveEdgeHud = waveEdgeHud;
    public final BooleanSetting moduleLiveEdge = activeModuleWaveEdge;

    public final NumberSetting rectCornerRadius = this.register(new NumberSetting("Скругление углов", "Радиус скругления углов общих панелей интерфейса.", 7.0, 2.0, 10.0, 1.0));

    private final SeparatorSetting rectBackdrop = this.register(new SeparatorSetting("Фон"));
    public final NumberSetting rectBackdropBlur = this.register(new NumberSetting("Размытие фона", "Радиус размытия фона за панелями интерфейса.", 18.0, 0.0, 64.0, 1.0));
    public final NumberSetting rectRefractionStrength = this.register(new NumberSetting("Преломление", "Насколько сильно материал искажает размытый фон.", 0.3, 0.0, 0.8, 0.01));

    private final SeparatorSetting rectColors = this.register(new SeparatorSetting("Цвета"));
    public final ModeSetting gradientStyle = this.register(new ModeSetting("Градиент", "Способ отрисовки градиента в материале.", GRADIENT_FLOW, GRADIENT_LINE, GRADIENT_FLOW, GRADIENT_CORNERS));
    public final ModeSetting clientColorMode = this.register(new ModeSetting("Цвет клиента", "Источник акцентного цвета клиента: палитра активной темы, акцентные цвета или динамический спектр.", CLIENT_COLOR_PALETTE, CLIENT_COLOR_PALETTE, CLIENT_COLOR_ACCENT, CLIENT_COLOR_SPECTRUM));
    public final NumberSetting rainbowSpeed = this.register(new NumberSetting("Скорость спектра", "Скорость прокрутки оттенков спектра.", 1.0, 0.1, 4.0, 0.1).visibleWhen(this::isRainbowClientColor));
    public final NumberSetting rainbowSpread = this.register(new NumberSetting("Разброс спектра", "Расстояние между оттенками градиента спектра.", 0.18, 0.02, 0.5, 0.01).visibleWhen(this::isRainbowClientColor));
    public final NumberSetting rainbowSaturation = this.register(new NumberSetting("Насыщенность спектра", "Насыщенность цвета спектра.", 0.85, 0.3, 1.0, 0.01).visibleWhen(this::isRainbowClientColor));
    public final ColorSetting rectColor = this.register(new ColorSetting("Цвет", "Основной оттенок материала и цвет грани.", new Color(-857872385, true)).visibleWhen(this::isCustomClientColor));
    public final BooleanSetting rectUseSecondColor = this.register(new BooleanSetting("Второй цвет", "Включает второй цвет для углового градиента.", false).visibleWhen(this::isCustomClientColor));
    public final ColorSetting rectSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет градиента материала.", new Color(-855690602, true)).visibleWhen(() -> this.isCustomClientColor() && this.rectUseSecondColor.getValue()));
    public final BooleanSetting rectColorMovement = this.register(new BooleanSetting("Движение цвета", "Анимирует двухцветный градиент вокруг углов панели.", false).visibleWhen(this::usesSecondClientColor));

    private final SeparatorSetting rectRefraction = this.register(new SeparatorSetting("Грань"));
    public final NumberSetting rectEdgeStrength = this.register(new NumberSetting("Сила грани", "Насколько сильно цвет грани подмешивается в материал.", 0.18, 0.0, 1.0, 0.01));
    public final NumberSetting rectEdgeSharpness = this.register(new NumberSetting("Резкость грани", "Чем выше значение, тем тоньше и резче блик.", 55.0, 2.0, 100.0, 1.0).visibleWhen(() -> this.rectEdgeStrength.getFloat() > 0.0f));

    private final SeparatorSetting rectGlowSeparator = this.register(new SeparatorSetting("Свечение"));
    public final BooleanSetting rectGlow = this.register(new BooleanSetting("Свечение", "Рисует мягкое свечение вокруг панелей интерфейса.", false));
    public final NumberSetting rectGlowIntensity = this.register(new NumberSetting("Яркость свечения", "Яркость свечения вокруг панелей.", 0.6, 0.0, 2.0, 0.05).visibleWhen(this.rectGlow::getValue));
    public final NumberSetting rectGlowRadius = this.register(new NumberSetting("Радиус свечения", "Насколько далеко расходится свечение.", 15.0, 15.0, 70.0, 1.0).visibleWhen(this.rectGlow::getValue));

    private final SeparatorSetting miscSeparator = this.register(new SeparatorSetting("Прочее"));
    public final BooleanSetting hudIcons = this.register(new BooleanSetting("Иконки", "Показывает иконку справа в заголовке элементов HUD, а название сдвигает влево.", false));
    public final ModeSetting dragStyle = this.register(new ModeSetting("Перетаскивание", "Свободный режим сразу двигает элемент, призрак сначала показывает проекцию на новом месте.", DRAG_GHOST, DRAG_FREE, DRAG_GHOST));
    public final BooleanSetting dragJitter = this.register(new BooleanSetting("Тряска при перетаскивании", "Легкое дрожание элемента во время перетаскивания. Выключите для статики.", true).visibleWhen(() -> this.dragStyle.is(DRAG_GHOST) || this.dragStyle.is("Проекция")));
    public final BooleanSetting dragWaves = this.register(new BooleanSetting("Волны при перетаскивании", "Показывает шейдерные волны позади зажатого элемента.", true).visibleWhen(() -> this.dragStyle.is(DRAG_GHOST) || this.dragStyle.is("Проекция")));
    public final BooleanSetting dragTilt = this.register(new BooleanSetting("Наклон при перетаскивании", "Плавно наклоняет элемент в сторону движения при перетаскивании.", true).visibleWhen(() -> this.dragStyle.is(DRAG_FREE) || this.dragStyle.is("Обычный")));

    public VisualMaterial() {
        super("Interface", "Общий визуальный стиль для всех элементов интерфейса.", Category.DISPLAY);

    }

    public static VisualMaterial getInstance() {
        return instance;
    }

    private static int lerpWhite(int n, float f) {
        float f2 = Math.max(0.0f, Math.min(1.0f, f));
        int n2 = Math.round(255.0f + (float)((n >> 16 & 0xFF) - 255) * f2);
        int n3 = Math.round(255.0f + (float)((n >> 8 & 0xFF) - 255) * f2);
        int n4 = Math.round(255.0f + (float)((n & 0xFF) - 255) * f2);
        return n2 << 16 | n3 << 8 | n4;
    }

    public int clientPrimaryColorOpaque() {
        return 0xFF000000 | this.clientPrimaryColor() & 0xFFFFFF;
    }

    public boolean usesSecondClientColor() {
        return this.isThemeClientColor() || this.isRainbowClientColor() || this.rectUseSecondColor.getValue();
    }

    public int clientSecondaryColorOpaque() {
        return 0xFF000000 | this.clientSecondaryColor() & 0xFFFFFF;
    }

    private int[] rainbowPalette() {
        float f = this.rainbowSaturation.getFloat();
        float f2 = Math.min(1.0f, this.rainbowSpread.getFloat() * 4.0f);
        float f3 = ClientAccent.rainbowBaseHue();
        int n = this.rainbowPalette.length;
        for (int i = 0; i < n; ++i) {
            float f4 = f3 + (float)i / (float)(n - 1) * f2;
            f4 -= (float)Math.floor(f4);
            int n2 = Color.HSBtoRGB(f4, 1.0f, 1.0f) & 0xFFFFFF;
            this.rainbowPalette[i] = VisualMaterial.lerpWhite(n2, f);
        }
        return this.rainbowPalette;
    }

    public boolean clientColorMovement() {
        return this.usesSecondClientColor() && this.rectColorMovement.getValue();
    }

    public int clientPrimaryColor() {
        return ClientAccent.gradientA(204.0f);
    }

    public boolean isThemeClientColor() {
        return ThemeManager.isLiveOverrideActive() || this.clientColorMode.is(CLIENT_COLOR_PALETTE) || this.clientColorMode.is("Темы");
    }

    public int gradientStyleId() {
        if (this.gradientStyle.is(GRADIENT_FLOW) || this.gradientStyle.is("Жидкие пятна")) {
            return 1;
        }
        return (this.gradientStyle.is(GRADIENT_CORNERS) || this.gradientStyle.is("По квадрату")) ? 2 : 0;
    }

    public int[] clientPalette() {
        int[] nArray;
        if (this.isRainbowClientColor()) {
            return this.rainbowPalette();
        }
        if (this.isThemeClientColor() && (nArray = ThemeManager.blendedPalette()) != null && nArray.length > 0) {
            return nArray;
        }
        if (this.rectUseSecondColor.getValue()) {
            return new int[]{this.rectColor.getColor() & 0xFFFFFF, this.rectSecondColor.getColor() & 0xFFFFFF};
        }
        return new int[]{this.rectColor.getColor() & 0xFFFFFF};
    }

    public boolean isRainbowClientColor() {
        return this.clientColorMode.is(CLIENT_COLOR_SPECTRUM) || this.clientColorMode.is("Радуга");
    }

    public int clientSecondaryColor() {
        return ClientAccent.gradientB(204.0f);
    }

    public boolean isCustomClientColor() {
        return this.clientColorMode.is(CLIENT_COLOR_ACCENT) || this.clientColorMode.is("Свой");
    }

    public boolean isMosaicStyle() {
        return true;
    }

    public float getStyleTransition() {
        return 1.0f;
    }

    public boolean isWaveEdgeActiveFor(boolean isHud) {
        if (!this.waveEdgeEnabled.getValue()) {
            return false;
        }
        return isHud ? this.waveEdgeHud.getValue() : this.waveEdgeInterface.getValue();
    }

    public boolean isLiveEdgeActiveFor(boolean isHud) {
        return this.isWaveEdgeActiveFor(isHud);
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }
}
