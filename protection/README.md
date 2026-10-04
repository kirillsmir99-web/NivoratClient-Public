# NivoratClient: Подсистема защиты релизных сборок (Protection Subsystem)

## 1. Архитектурный обзор

Подсистема `protection/` реализует многоуровневую защиту интеллектуальной собственности NivoratClient (Minecraft 1.21.11, Fabric Loader, Loom 1.15.5, Java 21) без ущерба для стабильности игры, FPS и совместимости с Mixin/Fabric Loader. Защита построена на базе локального инструментария ProGuard 7.9.1, специализированных доменных энкодеров констант/строк и процессора минификации шейдеров.

### Содержимое каталога `protection/`:
```text
protection/
├── proguard/
│   └── project.pro              <- Конфигурация ProGuard 7.9.1 (4-уровневая модель Tier 0-3)
├── shaders/
│   └── GlslShaderHardener.java  <- Token-based минификатор 163 GLSL шейдеров
├── verifier/
│   ├── JarSecurityVerifier.java <- Независимый ZIP-верификатор и сканер утечек
│   └── FunctionSecurityVerifier.java <- Верификатор сокрытия функций и констант
└── README.md                    <- Настоящее руководство разработчика
```

---

## 2. Сборка защищённого релиза одной командой

Для генерации полноценного защищенного релизного дистрибутива выполните команду:

```powershell
./gradlew protectedRelease
```

### Поведение сборочного пайплайна:
1. **Компиляция Java и ресурсов**: стандартный `compileJava`, `generateEditionResource`, `processResources`.
2. **Fabric Intermediary Remapping**: генерация базового мода через `remapJar`.
3. **Hardening шейдеров**: `GlslShaderHardener` сжимает 163 GLSL файла из `src/main/resources/assets/nivorat/shaders/` в `build/generated/protected-resources/assets/nivorat/shaders/`, вырезая все комментарии разработчиков и нормализуя пробелы.
4. **Обфускация ProGuard 7.9.1 (`obfuscateJar`)**:
   - Выполняет многоуровневое переименование приватных классов, методов и полей в компактные символы (`a`, `b`, `c`...).
   - Переупаковывает все чувствительные внутренние классы в единый пакет `activity.client.internal`.
   - Вычищает таблицы отладки (`LocalVariableTable`, `LocalVariableTypeTable`, `SourceFile`).
   - Сохраняет публичные ABI-контракты Fabric, Mixin-классы и аннотации Gson `@SerializedName`.
5. **Финальная сборка артефакта (`protectedJar`)**:
   - Внедряет минифицированные шейдеры и водяной знак сборки (`protection/watermark.properties`).
6. **Верификация структуры (`verifyProtectedJar`, `verifyFunctionProtection`)**:
   - Проверяет целостность `fabric.mod.json`, точки входа клиента и экосистемы.
   - Проверяет наличие всех 5 Mixin JSON конфигов и 19 mixin-классов.
   - Проверяет неизменность стороннего мода `ClientSpoofer-1.21.11-1.4.0.jar` по эталонному хэшу `c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9`.
   - Проверяет отсутствие исходных кодов (`.java`), маппингов, секретов и отладочных дампов.

---

## 3. Запуск визуального смоук-теста защищенного JAR

Чтобы запустить реальный клиент Minecraft с инжекцией именно **защищенного JAR** и выполнить все 20 тестов механик и интерфейса с сохранением скриншотов:

```powershell
./gradlew --init-script tools/protected-visual-smoke.init.gradle protectedVisualSmoke
```

Все 20 скриншотов сохраняются в каталоге `build/protected-visual-smoke/captures/screenshots/` (`01-all.png` ... `20-three-phrase-wheel.png`).

---

## 4. Переменные окружения и подпись (Production CI)

| Переменная | Назначение | Пример значения |
|---|---|---|
| `PROTECTION_KEYSTORE` | Путь к приватному хранилищу ключей подписи JAR | `/secrets/release.keystore` |
| `PROTECTION_KEYSTORE_PASSWORD` | Пароль от хранилища ключей | `${{ secrets.KS_PASS }}` |
| `PROTECTION_KEY_ALIAS` | Алиас ключа | `nivorat-release` |
| `PROTECTION_KEY_PASSWORD` | Пароль от ключа | `${{ secrets.KEY_PASS }}` |

> ⚠️ Пароли и секреты никогда не записываются в файлы проекта и не выводятся в логи Gradle.
