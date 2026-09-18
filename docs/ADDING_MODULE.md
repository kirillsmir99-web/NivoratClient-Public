# Руководство по добавлению нового модуля в NivoratClient

Данный документ описывает процесс создания, настройки и регистрации нового модуля в **NivoratClient** с использованием **Nivorat Module SDK**.

---

## 1. Архитектурные принципы

Nivorat Module SDK построен на концепциях **Zero Boilerplate**, **Single Source of Truth** и **Automatic UI Binding**:

1. **Единый источник истины (Single Source of Truth)**:
   Объект `Setting<T>` хранит реальное runtime-значение. Никаких рассинхронизированных локальных копий в UI. Изменение в UI мгновенно передается в логику и конфиг.
2. **Автоматическая генерация GUI (Automatic UI Binding)**:
   Все зарегистрированные настройки модуля автоматически отрисовываются фабрикой `SettingComponentFactory` в карточке модуля без написания GUI-кода.
3. **Единый менеджер клавиш (`KeybindManager`)**:
   Основная клавиша (`module.getKeybind()`) и вторичные хоткеи (`KeybindSetting`) обрабатываются централизованно с защитой от двойного срабатывания (debouncing).
4. **Стандартизированные секции и единицы**:
   Настройки группируются по секциям `GENERAL`, `BEHAVIOR`, `ADVANCED`, `HUD`, а числовые параметры используют строгие `NumberUnit` (`ms`, `ticks`, `%`, `HP`, `blocks`, `CPS`).
5. **Автоматическая поисковая индексация и Quick Access**:
   Новые модули автоматически получают двуязычную поисковую индексацию (RU/EN, транслит, опечатки) и поддержку закрепления в контекстном меню быстрого доступа (Quick Access pinning) с нулевым объёмом дополнительного кода (zero boilerplate).

---

## 2. Быстрый старт: 4 простых шага

### Шаг 1: Создание класса модуля

Создайте класс модуля в подходящем пакете (`activity.client.module.impl.combat`, `defense` или `utility`), унаследовав его от `NivoratModule`:

```java
package activity.client.module.impl.combat;

import activity.client.module.api.ModuleCategory;
import activity.client.module.api.ModuleMetadata;
import activity.client.module.api.NivoratModule;
import activity.client.gui.icon.ActivityIcon;
import net.minecraft.text.Text;

public class MyNewModule extends NivoratModule {
    public static final String ID = "my_new_module";

    public MyNewModule() {
        super(
            ID,
            Text.literal("Мой Модуль"),
            Text.literal("Описание работы модуля"),
            ModuleCategory.COMBAT
        );

        // Метаданные (автор, версия, иконка, хоткей)
        this.metadata = ModuleMetadata.builder(ID)
                .displayName(name)
                .description(description)
                .category(category)
                .author("Nivorat")
                .version("1.0.0")
                .icon(ActivityIcon.COMBAT)
                .keybind(keybind)
                .build();
    }
}
```

---

### Шаг 2: Добавление настроек (Typed Settings)

В конструкторе модуля зарегистрируйте настройки с помощью вспомогательных методов:

```java
// 1. Секция GENERAL (Основные)
registerBoolean(
    "auto_swap",
    Text.literal("Автоматический свап"),
    Text.literal("Переключать оружие при атаке"),
    SettingSection.GENERAL,
    true,
    () -> isAutoSwap,
    val -> isAutoSwap = val
);

registerEnum(
    "mode",
    Text.literal("Режим"),
    Text.literal("Алгоритм выбора"),
    SettingSection.GENERAL,
    List.of("smart", "fast"),
    "smart",
    () -> currentMode,
    val -> currentMode = val
);

// 2. Секция BEHAVIOR (Поведение: задержки, дистанции, шансы)
registerNumber(
    "delay",
    Text.literal("Задержка возврата"),
    Text.literal("Таймаут в миллисекундах"),
    SettingSection.BEHAVIOR,
    20.0, 300.0, 5.0, NumberUnit.MS, true,
    90.0,
    () -> delayMs,
    val -> delayMs = val
);

registerDouble(
    "distance",
    Text.literal("Дистанция"),
    Text.literal("Дистанция срабатывания"),
    SettingSection.BEHAVIOR,
    1.5, 4.5, 0.1, NumberUnit.BLOCKS,
    3.0,
    () -> distance,
    val -> distance = val
);

// 3. Секция ADVANCED (Расширенные: legit-режимы, хоткеи, действия)
registerBoolean(
    "legit_mode",
    Text.literal("Легитный режим"),
    Text.literal("Рандомизация таймингов для обхода античитов"),
    SettingSection.ADVANCED,
    true,
    () -> legitMode,
    val -> legitMode = val
);

registerAction(
    "reset_btn",
    Text.literal("Сбросить"),
    Text.literal("Возврат к настройкам по умолчанию"),
    SettingSection.ADVANCED,
    this::reset
);
```

#### Доступные типы настроек и их сопоставление с GUI:

| Setting Type | GUI Компонент | Назначение |
|---|---|---|
| `BooleanSetting` | `ActivityToggle` | Логический переключатель (Вкл / Выкл) |
| `NumberSetting` / `DoubleSetting` | `ActivitySlider` | Слайдер с поддержкой шага, мин/макс и единиц измерения |
| `IntegerSetting` | `ActivitySlider` | Целочисленный слайдер с фиксацией шага |
| `EnumSetting` | `ActivityDropdown` | Выпадающий список строковых или локализованных опций |
| `KeybindSetting` | `ActivityKeybindButton` | Кнопка настройки хоткея (Ctrl, Shift, Alt, Мышь) |
| `StringSetting` | `ActivityTextField` | Текстовое поле ввода |
| `ActionSetting` | `ActivityButton` | Кнопка вызова действия (сброс, открытие редактора HUD) |

#### Поддерживаемые единицы измерения (`NumberUnit`):
- `NumberUnit.MS` — `ms` (миллисекунды, напр. `90 ms`)
- `NumberUnit.TICKS` — `ticks` (игровые тики, напр. `2 ticks`)
- `NumberUnit.PERCENT` — `%` (проценты, напр. `75%`)
- `NumberUnit.HP` — `HP` (сердца / единицы здоровья, напр. `6 HP`)
- `NumberUnit.BLOCKS` — `bl` (дистанция в блоках, напр. `3.5 bl`)
- `NumberUnit.CPS` — `CPS` (клики в секунду)

---

### Шаг 3: Реализация игровых хуков жизненного цикла

Переопределите только те методы, которые реально требуются вашему модулю:

```java
@Override
public void onEnable() {
    // Вызывается при включении модуля
}

@Override
public void onDisable() {
    // Вызывается при выключении модуля
}

@Override
public void onTick(MinecraftClient client) {
    if (!isEnabled() || client.player == null) return;
    // Периодическая логика каждый клиентский тик
}

@Override
public ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
    if (!isEnabled()) return ActionResult.PASS;
    // Перехват атаки по сущности (для боевых модулей AutoMace, AutoSpear и т.д.)
    return ActionResult.PASS;
}

@Override
public void onRenderHud(DrawContext context, RenderTickCounter tickCounter) {
    if (!isEnabled()) return;
    // Отрисовка элементов оверлея в игре (для HUD-модулей)
}
```

---

### Шаг 4: Точка расширения (Extension Point для сложных HUD)

Если модулю необходим специальный визуальный виджет или встроенный предпросмотр внутри карточки:

```java
@Override
public boolean hasCustomSection() {
    return true;
}

@Override
public int buildCustomSection(ActivityTab tab, ActivityScreen screen, ScrollContainer container,
                              int startX, int startY, int innerRowW) {
    ActivityLabel preview = new ActivityLabel(startX, startY + 3, Text.literal("HUD Preview"));
    tab.addControl(container, preview);
    return ActivityMetrics.CONTROL_HEIGHT + ActivityMetrics.ROW_SPACING;
}
```

---

### Шаг 5: Регистрация модуля

Для регистрации встроенного модуля добавьте его в `BuiltinModules.registerAll()`:

```java
ModuleRegistry.register(new MyNewModule());
```

Для динамической или плагинной регистрации модуль можно зарегистрировать в любой момент через:
```java
ModuleRegistry.register(moduleInstance);
```

> **Обратите внимание**: Зарегистрированный модуль мгновенно становится доступен во всех компонентах GUI. Фабрика `SearchController` автоматически индексирует его метаданные (название, описание, категорию, алиасы) для двуязычного поиска (русский/английский, транслит, нечёткие опечатки), а в дереве навигации и контекстном меню правого клика автоматически активируется закрепление в Quick Access (быстрый доступ) без единой строчки вспомогательного кода.

---

## 3. Образец кода

Полный эталонный пример реализации модуля со всеми типами настроек и хуков доступен в файле:
`src/main/java/activity/client/module/example/ExampleModule.java`
