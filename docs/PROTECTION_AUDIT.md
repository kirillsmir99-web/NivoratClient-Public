# NivoratClient Protection Audit & Classification

## 1. Executive Summary

Настоящий технический аудит определяет ландшафт интеллектуальной собственности (IP), границы рантайма и профили защиты для клиента **NivoratClient** (Minecraft 1.21.11, Fabric Loader >=0.16.0, Java 21, Fabric Loom 1.15.5).

Главная цель защиты release-сборки: предотвратить декомпиляцию в читаемый исходный код, кражу уникальных алгоритмов авто-картинга, предсказания движений, расчёта баллистики, таймингов и кастомного графического движка, при этом сохранив 100% стабильность, Fabric ABI, обратную совместимость JSON-конфигураций и пресетов, а также стабильный фреймрейт (frametime / FPS).

---

## 2. Архитектурная классификация компонентов

Кодовая база клиента разделена на 8 фундаментальных категорий:

| Категория | Пакеты / Компоненты | Критичность | Допустимая трансформация |
|---|---|---|---|
| **CRITICAL_IP** | `dev.nivorat.arc.*`, `dev.mace.prestige.*`, `dev.raycast.*`, `net.fabricmc.pack.api.*`, `dev.storage.*`, combat modules | Максимальная (Tier 3) | Агрессивное переименование, глубокий Control Flow, шифрование строк, удаление метаданных, переупорядочивание инструкций (вне hot-loop методов) |
| **HIGH_IP** | `activity.client.module.api.*`, `activity.client.module.service.*`, `activity.client.gui.custom.*` (логика UI/state) | Высокая (Tier 2) | Переименование, стандартный Control Flow, шифрование строк, удаление SourceFile и отладочных таблиц |
| **PERFORMANCE_CRITICAL** | `activity.client.gui.custom.utils.render.**`, MSDF шейдерные диспетчеры, `ArcMotionProfile.forward()` | Критично к FPS (Tier 1) | Безопасное переименование, удаление метаданных. **Запрещены** тяжёлые диспетчеры control-flow и строковое дешифрование в циклах рендера |
| **FABRIC_BOUNDARY** | `CooldownHudClient`, `ClientIntegrationProvider`, Mixin классы и аксессоры во всех 5 json-файлах | ABI Boundary (Tier 0) | **KEEP**. Полное сохранение имён классов, методов и дескрипторов инжекторов. Сохранение LocalVariableTable при захвате переменных |
| **SERIALIZATION_BOUNDARY** | `ActivityConfig`, `Preset`, `MorrowConfig`, `CartHudConfig` и все сохраняемые поля | Совместимость JSON (Tier 0 -> Tier 2) | Фиксация схемы через `@SerializedName`. После аннотирования разрешено обфусцировать поля Java |
| **RESOURCE_BOUNDARY** | `assets/activity/**`, `assets/nivorat/**`, пути к текстурам, шейдерам, шрифтам, звукам | Resource Identity (Tier 0) | Сохранение строковых путей ресурсов в неизменном виде. Защита шейдеров на уровне кода GLSL (safe minification) |
| **PUBLIC_API** | `IModule`, `CooldownModule`, экосистемные интерфейсы интеграции | Экосистема (Tier 0) | Сохранение публичных сигнатур для внешней совместимости |
| **THIRD_PARTY** | `ClientSpoofer-1.21.11-1.4.0.jar` (nested jar), Fabric API, Minecraft runtime | Сторонний код (Tier 0) | Байт-в-байт сохранение. Исходный SHA-256 (`c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9`) |

---

## 3. Детальный аудит sensitive пакетов

### 3.1. `dev.nivorat.arc.*` (Arc & AutoCart Engine)
* **Классы**: `AutoCartController`, `ArcMotionProfile`, `ArcCameraInterpolator`, `ArcMotorAnalysisEngine`, `ArcMotorCalibrationService`, `ArcInputRecorder`, `ArcActionValidator`, `ArcMotorFrame`, `ArcCalibrationState`, `AutoCartLogger`, `MorrowConfig`.
* **Назначение**: Проприетарная система динамического управления вагонетками (AutoCart), калибровки моторики игрока, сглаживания движений мыши, сбора кинематических профилей и валидации действий перед отправкой пакетов в обход античитов (GrimAC / Vulcan).
* **Уровень защиты**: **TIER 3 (MAX IP)**.
* **Переименование имён**: Разрешено полное переименование классов, полей, методов, локальных переменных и параметров. Исключение: сохраняемые поля `MorrowConfig` обязаны иметь аннотацию `@SerializedName`.
* **Control-Flow**: Агрессивный control-flow на уровне контроллеров, валидаторов и сервисов калибровки. В hot-path методе `ArcMotionProfile.forward()` и математических циклах интерполятора применяется умеренный (normal) flow во избежание деградации тактовой производительности.
* **Шифрование строк**: Да, шифруются все диагностические сообщения, логи калибровки, математические маркеры.
* **Рефлексия / Сериализация**: Сериализация профилей калибровки защищается фиксированными именами JSON-полей.
* **Тесты валидации**: `AutoCartEnhancementTest`, `ArcMotionPersistenceTest`, `ArcMotorEngineTest`, `ArcRotationSamplingTest`, `AutoCartSmoke`.

### 3.2. `dev.mace.*` & `dev.mace.prestige.*`
* **Классы**: `PrestigeAutoMaceController`, `PrestigeSilentAim`, `PrestigeStunSlamController`, `PrestigeAutoMaceConfig`, `PrestigeStunSlamConfig`.
* **Назначение**: Алгоритмы точного расчёта момента удара булавой (AutoMace), компенсация высоты падения, невидимая доводка прицела (SilentAim) и оглушающий слэм.
* **Уровень защиты**: **TIER 3 (MAX IP)**.
* **Переименование имён**: Полное для внутренней логики. Конфигурационные DTO аннотируются `@SerializedName`.
* **Control-Flow**: Агрессивный на контроллерах.
* **Шифрование строк**: Да.
* **Тесты валидации**: `AutoMaceGuiBridgeTest`, `AutoStunSlamIntegrationTest`.

### 3.3. `dev.raycast.*` & `dev.raycast.async.*`
* **Классы**: `RaycastInterpolator`, `RaycastPredictorController`, `RaycastTrajectory`, `AsyncLocatorController`, `AsyncMath`, `AsyncRot`, `AsyncSilentRot`.
* **Назначение**: Асинхронный расчёт баллистических траекторий, предсказание положения хитбоксов с учётом пинга и ускорения, ротация камеры без смещения клиентского угла обзора.
* **Уровень защиты**: **TIER 3 (MAX IP)**.
* **Переименование имён**: Полное.
* **Control-Flow**: Агрессивный на контроллерах, умеренный на функциях тригонометрии (`AsyncMath`).
* **Шифрование строк**: Да.
* **Тесты валидации**: `RaycastPredictorModuleTest`, `CameraAndTimingEdgeCaseTest`, `PearlInterceptTest`.

### 3.4. `net.fabricmc.pack.api.*`
* **Классы**: `CombatLockManager`, `CombatRaytraceGuard`, `GaussianTimingEngine`, `SafeSlotManager`, `SlotArbiter`, `TickBoundScheduler`, `TotemGuard`.
* **Назначение**: Ядро боевой автоматизации: распределение Гаусса для интервалов кликов (humanizer), арбитраж слотов инвентаря, защита от сброса тотема, планировщик с привязкой к серверным тикам.
* **Уровень защиты**: **TIER 3 (MAX IP)**.
* **Переименование имён**: Полное.
* **Control-Flow**: Высокий уровень.
* **Шифрование строк**: Да.
* **Тесты валидации**: `ActionTimingRegressionTest`, `AutoTotemComprehensiveTest`, `AutoShieldbreakerAndCartCooldownTest`.

### 3.5. `dev.storage.CartRefillController`
* **Классы**: `CartRefillController`.
* **Назначение**: Управление быстрой подкачкой вагонеток и рельс из хотбара и инвентаря.
* **Уровень защиты**: **TIER 3 (MAX IP)**.
* **Тесты валидации**: `CartRefillControllerTest`.

### 3.6. `activity.client.gui.custom.*` & Рендеринг
* **Классы**: Custom UI framework, `Render2D`, MSDF font engine, `GlassRenderer`, Kawase blur pipeline, Glow/Vapour шейдеры.
* **Назначение**: Векторный 2D GUI движок Nivorat, субпиксельные шрифты, кастомные визуальные эффекты.
* **Уровень защиты**:
  - Логика окон, модулей, настроек, анимаций: **TIER 2 (NORMAL)**.
  - Отрисовка примитивов, батчинг, шейдерные биндинги, глифы (`MsdfFonts`, `GlyphAtlasPage`, `Render2D`): **TIER 1 (LIGHT)** — без тяжёлого control flow.
* **Тесты валидации**: `VisualPolishTest`, `MsdfFontOptimizationTest`, `GlassEffectTest`, `publicVisualSmoke`, `protectedVisualSmoke`.

---

## 4. Границы Fabric и Mixin (Tier 0 KEEP)

### 4.1. Entrypoints (`fabric.mod.json`)
1. `activity.client.CooldownHudClient` (`client`)
2. `activity.client.integration.ClientIntegrationProvider` (`nivorat:settings_v1`)

Имена этих классов и их конструкторы без параметров должны быть сохранены в первозданном виде.

### 4.2. Конфигурации Mixin
* `activity.pipeline.mixins.json`:
  - `PipelineInteractionManagerMixin`, `PipelineInteractionManagerAccessor`, `AsyncInputMoveAccessor`, `AsyncInteractMixin`, `AsyncKeyboardInputMixin`, `KeyBindingAccessor`, `AsyncPlayerMoveMixin`, `AsyncMinecraftClientMixin`.
* `activity.audio.mixins.json`:
  - `AudioNetworkHandlerMixin`, `ClientCommonNetworkHandlerMixin`.
* `activity.dev.mixins.json`:
  - `MixinTranslationStorage`.
* `activity.cooldown.mixins.json`:
  - `MixinItemCooldownManager`.
* `activity.custom-render.mixins.json`:
  - `GuiRendererMixin`, `GuiRenderStateMixin`, `accessor.GuiGraphicsExtractorAccessor`, `accessor.GuiRendererDrawAccessor`, `chatanim.ChatScreenMixin`, `chatanim.ChatComponentMixin`, `GameRendererMixin`.

Все перечисленные классы миксинов, их методы с аннотациями `@Inject`, `@Redirect`, `@ModifyArg`, `@ModifyVariable`, `@Shadow`, `@Accessor`, `@Invoker` обязаны оставаться непереименованными в Tier 0 во избежание сбоя сопоставления байткода Fabric/Mixin.

---

## 5. Граница сериализации (Gson Serialization Boundary)

Клиент активно использует Gson для хранения конфигурации (`ActivityConfig.java`), пресетов (`Preset.java`), настроек модулей (`Setting.java`) и калибровочных данных.

По умолчанию Gson сопоставляет ключи JSON с именами Java-полей через reflection. Если обфускатор переименует поле `autoCartCameraSmoothness` в поле `a`, загрузка существующих конфигов и пресетов завершится потерей настроек.

**Стратегия решения**:
1. Все поля, сериализуемые в JSON, помечаются аннотацией `@SerializedName("имяПоля")`.
2. После добавления аннотаций поле в байткоде может быть безопасно переименовано обфускатором, в то время как JSON контракт остаётся неизменным.
3. Сохраняются типы generic (`Signature` атрибуты) для классов, использующих `TypeToken`.

---

## 6. Защита ресурсов GLSL

Шейдеры в формате `.fsh`, `.vsh` и `.glsl` располагаются в `assets/nivorat/shaders/`.
* Внешние юниформы, сэмплеры, `in`/`out` переменные и пути include завязаны на вызовы Java `GL20.glGetUniformLocation` и структуры Minecraft ShaderProgram.
* Любое случайное переименование глобальных идентификаторов через Regex сломает компиляцию шейдеров в драйвере видеокарты.
* Реализуется safe pipeline минификации: удаление комментариев, нормализация пробелов, сохранение директив препроцессора (`#version`, `#moj_import`). Шейдеры исходного дерева сохраняются для разработки, а в релизный JAR упаковывается защищённая генерация.

---

## 7. Сторонний компонент `ClientSpoofer`

* Путь: `META-INF/jars/ClientSpoofer-1.21.11-1.4.0.jar`.
* Контрольная сумма SHA-256: `c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9`.
* Лицензия: `META-INF/licenses/ClientSpoofer-MIT.txt`.
* Данный вложенный JAR не подлежит обфускации или перепаковке и должен проверяться на совпадение хэша на этапе сборки и верификации release JAR.

---

## 8. Итоговая матрица уровней защиты

```
+---------------------------------------------------------------------------------------+
| TIER 0 (KEEP)            | Fabric Entrypoints, Mixin classes/accessors,               |
|                          | ClientSpoofer, Resource identifiers, Public API contracts  |
+---------------------------------------------------------------------------------------+
| TIER 1 (LIGHT)           | GUI Render Hot-Paths (Render2D, MsdfFonts, Blur passes),   |
|                          | High-frequency Math loops (ArcMotionProfile.forward)       |
+---------------------------------------------------------------------------------------+
| TIER 2 (NORMAL)          | Общие модули, сервисы, UI-логика, настройки, окна          |
+---------------------------------------------------------------------------------------+
| TIER 3 (MAX IP)          | Arc AutoCart, Kinematics, Ballistics, Raycast Predictor,   |
|                          | Gaussian Timing, SafeSlot, Prestige Mace, AntiCheat bypass |
+---------------------------------------------------------------------------------------+
```
