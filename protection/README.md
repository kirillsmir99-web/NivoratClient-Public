# NivoratClient: Подсистема защиты релизных сборок (Protection Subsystem)

## 1. Архитектурный обзор

Подсистема `protection/` реализует многоуровневую защиту интеллектуальной собственности NivoratClient (Minecraft 1.21.11, Fabric Loader, Loom 1.15.5, Java 21) без ущерба для стабильности игры, FPS и совместимости с Mixin/Fabric Loader.

### Содержимое каталога `protection/`:
```text
protection/
├── dasho/
│   └── project.dox           <- Конфигурация PreEmptive DashO (4-уровневая модель Tier 0-3)
├── shaders/
│   └── GlslShaderHardener.java <- AST/Token-based минификатор 163 GLSL шейдеров
├── verifier/
│   └── JarSecurityVerifier.java <- Независимый ZIP-верификатор и сканер утечек
└── README.md                 <- Настоящее руководство разработчика
```

---

## 2. Сборка релиза одной командой

Для генерации полноценного защищенного релизного дистрибутива выполните команду:

```powershell
./gradlew protectedRelease
```

### Поведение сборочного пайплайна:
1. **Компиляция Java и ресурсов**: стандартный `compileJava`, `generateEditionResource`, `processResources`.
2. **Fabric Intermediary Remapping**: генерация базового мода через `remapJar`.
3. **Hardening шейдеров**: `GlslShaderHardener` сжимает 163 GLSL файла из `src/main/resources/assets/nivorat/shaders/` в `build/generated/protected-resources/assets/nivorat/shaders/`, вырезая все комментарии разработчиков.
4. **Обфускация PreEmptive DashO**:
   - Читает системную переменную `DASHO_HOME`.
   - Если переменная не задана или исполняемый файл DashO отсутствует, **сборка немедленно прерывается с кодом 1** с информативным сообщением (тихий откат на ProGuard запрещен).
   - Выполняет многоуровневое переименование, преобразование графа потока управления (Control Flow Flattening) и шифрование строк.
5. **Верификация структуры (`verifyProtectedJar`)**:
   - Проверяет целостность `fabric.mod.json`, точки входа клиента и экосистемы.
   - Проверяет наличие всех 5 Mixin JSON конфигов и 19 mixin-классов.
   - Проверяет неизменность стороннего мода `ClientSpoofer-1.21.11-1.4.0.jar` по хэшу `c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9`.
   - Проверяет отсутствие исходных кодов (`.java`), маппингов, секретов и отладочных дампов.

---

## 3. Режим тестирования без лицензии DashO (`testMode`)

Для отладки сборочного конвейера, запуска CI/CD без коммерческой лицензии и прогона визуальных тестов поддерживается параметр `-Pprotection.testMode=true`:

```powershell
./gradlew protectedRelease -Pprotection.testMode=true
```

В этом режиме артефакт пакуется со всеми защитными механизмами ресурсов (минифицированные шейдеры, верификация контрольных сумм, исключение отладочной информации), позволяя запустить верификатор и тест-раннер без коммерческого бинарника DashO.

---

## 4. Запуск визуального смоук-теста защищенного JAR

Чтобы запустить реальный клиент Minecraft с инжекцией именно **защищенного JAR** и выполнить все 20 тестов механик и интерфейса с сохранением скриншотов:

```powershell
./gradlew --init-script tools/protected-visual-smoke.init.gradle protectedVisualSmoke -Pprotection.testMode=true
```

Все 20 скриншотов сохраняются в каталоге `build/protected-visual-smoke/captures/screenshots/` (`01-all.png` ... `20-three-phrase-wheel.png`).

---

## 5. Переменные окружения и секреты (Production CI)

| Переменная | Назначение | Пример значения |
|---|---|---|
| `DASHO_HOME` | Путь к установке PreEmptive DashO | `C:\Program Files\PreEmptive Protection DashO 12.0` |
| `PROTECTION_KEYSTORE` | Путь к приватному хранилищу ключей подписи JAR | `/secrets/release.keystore` |
| `PROTECTION_KEYSTORE_PASSWORD` | Пароль от хранилища ключей | `${{ secrets.KS_PASS }}` |
| `PROTECTION_KEY_ALIAS` | Алиас ключа | `nivorat-release` |
| `PROTECTION_KEY_PASSWORD` | Пароль от ключа | `${{ secrets.KEY_PASS }}` |

> ⚠️ Пароли и секреты никогда не записываются в файлы проекта и не выводятся в логи Gradle.
