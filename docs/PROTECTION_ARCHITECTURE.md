# Архитектура защиты release-сборки NivoratClient

## 1. Введение и цели

Клиент **NivoratClient** (Minecraft 1.21.11, Fabric Loader, Java 21, Fabric Loom 1.15.5) содержит уникальную интеллектуальную собственность (IP), включающую:
- Алгоритмы кинематики и адаптивного управления вагонетками (`dev.nivorat.arc.*`);
- Расчёт баллистических траекторий и упреждения (`dev.raycast.*`);
- Механики булавы и слэма (`dev.mace.prestige.*`);
- Гауссову рандомизацию таймингов и хуманизацию кликов (`net.fabricmc.pack.api.GaussianTimingEngine`);
- Арбитраж слотов инвентаря и защиту тотемов (`net.fabricmc.pack.api.*`, `dev.storage.*`);
- Векторный 2D GUI-движок, многопоточный MSDF-рендерер шрифтов и 163 GLSL-шейдера.

Данная архитектура реализует многоуровневую защиту от обратной разработки, декомпиляции и копирования алгоритмов сторонними клиентами, строго соблюдая приоритеты:
1. **Работоспособность и стабильность** (0 падений Fabric Loader и Minecraft).
2. **Совместимость** (Mixins, entrypoints, экосистема Nivorat, сторонние моды).
3. **Производительность** (0 деградации frametime/FPS на hot-path участках).
4. **Непреодолимость простого реверс-инжиниринга**.

---

## 2. Модель уровней защиты (Protection Tiers)

Кодовая база структурирована на 4 строгих уровня защиты:

```
┌────────────────────────────────────────────────────────────────────────┐
│ TIER 0: ABI / KEEP BOUNDARY                                            │
│ - Entrypoints: CooldownHudClient, ClientIntegrationProvider            │
│ - Все Mixin-классы, методы @Inject/@Redirect/@ModifyArg, аксессоры     │
│ - Поля Setting в классах интерфейса для VisualSettingsStore            │
│ - Сторонний JAR ClientSpoofer-1.21.11-1.4.0.jar (Vendor SHA-256)       │
│ - Пути ресурсов (assets/**)                                            │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│ TIER 1: LIGHT / PERFORMANCE CRITICAL                                   │
│ - Рендеринг: Render2D, MsdfFonts, GlyphAtlasPage, Kawase blur passes   │
│ - Высокочастотные циклы: ArcMotionProfile.forward()                    │
│ - Разрешено: безопасное переименование, удаление отладочных метаданных │
│ - ЗАПРЕЩЕНО: тяжёлый control-flow, дешифрование строк в циклах кадров  │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│ TIER 2: NORMAL LOGIC                                                   │
│ - Общие модули, сервисы состояния, настройки, окна интерфейса          │
│ - Стандартное переименование классов/методов/полей                     │
│ - Control-flow обфускация                                              │
│ - Шифрование строковых констант                                        │
│ - Удаление SourceFile и LocalVariableTable                             │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│ TIER 3: MAX IP (CRITICAL IP)                                           │
│ - dev.nivorat.arc.* (AutoCartController, ArcMotionProfile, и др.)      │
│ - dev.mace.prestige.*, dev.raycast.*, net.fabricmc.pack.api.*          │
│ - Максимальное агрессивное переименование                              │
│ - Глубокий Control Flow (расщепление базовых блоков, ложные переходы)  │
│ - Тотальное шифрование строк и диагностических сообщений               │
│ - Полная зачистка имен параметров и локальных переменных               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Сохранение Fabric Loader ABI и Mixins

### 3.1. Точки входа (Entrypoints)
Файл `fabric.mod.json` декларирует две точки входа:
1. `client`: `activity.client.CooldownHudClient`
2. `nivorat:settings_v1`: `activity.client.integration.ClientIntegrationProvider`

Имена этих классов и их открытые конструкторы зафиксированы в правилах исключений DashO (`excludelist`). Это гарантирует, что `FabricLoader.getInstance().getEntrypointContainers(...)` безошибочно инстанциирует клиент.

### 3.2. Mixin-безопасность
Проект использует 5 конфигурационных файлов Mixin:
- `activity.pipeline.mixins.json`
- `activity.audio.mixins.json`
- `activity.dev.mixins.json`
- `activity.cooldown.mixins.json`
- `activity.custom-render.mixins.json`

Все 19 классов миксинов и аксессоров исключены из переименования и деструктивной трансформации потока управления. Дескрипторы инжекций (`@Inject(method = "...", at = @At(...))`) остаются валидными в среде Intermediary mappings после прохождения задачи `remapJar`.

---

## 4. Решение проблемы сериализации Gson (@SerializedName)

### 4.1. Проблема
Ранее класс `ActivityConfig.java` и nested DTO опирались на сопоставление JSON-ключей с именами полей Java через reflection. При обфускации поле `autoCartCameraSmoothness` превратилось бы в `a`, вызвав полный сброс настроек пользователя и несовместимость с ранее сохранёнными пресетами.

### 4.2. Реализованное решение
На все 381 сохраняемое поле классов `ActivityConfig`, `Keybind`, `AudioSyncConfig`, `ModConfig` внедрена явная аннотация:
```java
@SerializedName("autoCartCameraSmoothness")
public double autoCartCameraSmoothness = 110.0;
```
Это позволило полностью отвязать внутренние Java-идентификаторы от внешнего JSON-контракта:
- Обфускатор волен менять имя поля в байткоде на любое однобуквенное обозначение.
- Gson сериализует и десериализует JSON строго по значению в `@SerializedName`.
- Сохранена 100% совместимость со всеми существующими файлами конфигураций и пресетов.

---

## 5. Конвейер защиты шейдеров GLSL (Shader Hardening Pipeline)

Все 163 GLSL-шейдера (`.fsh`, `.vsh`, `.glsl`) в каталоге `assets/nivorat/shaders/` обрабатываются специализированным процессором `GlslShaderHardener`:
1. **Удаление комментариев**: Вырезаются все однострочные (`//`) и многострочные (`/* ... */`) комментарии разработчиков.
2. **Сжатие пробелов**: Множественные пробелы и отступы сворачиваются в одиночные.
3. **Строгая изоляция директив препроцессора**: Директивы `#version`, `#moj_import`, `#define`, `#extension` гарантированно сохраняются на отдельных строках с символом перевода строки, что предотвращает ошибки компиляции в OpenGL драйвере.
4. **Неизменность сигнатур**: Внешние юниформы (`uniform`), сэмплеры (`sampler2D`), атрибуты вершин (`in`/`out`) не затрагиваются, так как к ним обращается Java-код через `GL20.glGetUniformLocation`.
5. **Разделение сред**: Исходные читаемые шейдеры остаются в `src/main/resources` для удобства разработки, а минифицированные генерируются в `build/generated/protected-resources/` и внедряются исключительно в release JAR.

---

## 6. Вложенный компонент ClientSpoofer

Third-party компонент `vendor/clientspoofer/ClientSpoofer-1.21.11-1.4.0.jar`:
- Располагается внутри JAR по пути `META-INF/jars/ClientSpoofer-1.21.11-1.4.0.jar`.
- Имеет эталонный SHA-256: `c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9`.
- Сопровождается лицензией `META-INF/licenses/ClientSpoofer-MIT.txt`.
- Полностью исключён из процесса обфускации и переупаковки; автоматический валидатор `JarSecurityVerifier` проверяет его хэш байт-в-байт.

---

## 7. График задач сборки (Gradle Task Graph)

```
compileJava -> processResources -> jar -> remapJar
                                            │
compileProtection ──────────────────────────┤
                                            │
prepareProtection (GLSL Hardening, Dox) ────┤
                                            │
                                    protectedJar (DashO)
                                            │
                                   verifyProtectedJar
                                            │
                                     protectedRelease
                                            │
                              [protectedVisualSmoke] (тесты в игре)
```
