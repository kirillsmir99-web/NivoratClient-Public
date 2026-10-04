# NivoratClient: Деобфускация крэш-логов и стек-трейсов (Phase 16)

## 1. Введение

В защищенных релизах NivoratClient имена классов, методов, полей и номеров строк классов ядра обфусцируются с помощью ProGuard 7.9.1.
В случае возникновения непредвиденного исключения у пользователя сгенерированный крэш-репорт Minecraft (`crash-reports/crash-YYYY-MM-DD.txt`) или лог (`logs/latest.log`) будет содержать нечитаемый стек-трейс вида:

```text
java.lang.NullPointerException: Cannot invoke method "a()" on null object
    at activity.client.internal.aK.b(Unknown Source)
    at activity.client.internal.aG.a(Unknown Source)
    at activity.client.internal.p.onTick(Unknown Source)
    ...
```

Для того чтобы разработчик мог оперативно локализовать и исправить ошибку, релизный пайплайн NivoratClient сохраняет приватные файлы маппингов (Change Log / Renaming Map) на сборочном сервере.

> ⚠️ **КРИТИЧЕСКОЕ ПРАВИЛО БЕЗОПАСНОСТИ**:
> Файлы маппингов **НИКОГДА** не должны попадать внутрь распространяемого релизного JAR! 
> Автоматический верификатор `JarSecurityVerifier` завершает сборку с фатальной ошибкой, если в JAR обнаруживается файл `*map*.txt` или `*mapping*.txt`.

---

## 2. Структура хранения маппингов на стороне разработчика

При выполнении команды `./gradlew protectedRelease` создается каталог метаданных конкретного билда:

```text
build/protection/mappings/
├── release-mapping.txt     <- Основной файл соответствия символов (ProGuard format)
└── <version>-<build_id>/
    ├── mapping.txt         <- Копия соответствия символов для конкретного релиза
    └── build-info.json     <- Метаданные релиза (commit SHA, jar sha256)
```

Все каталоги маппингов в `build/protection/mappings/` занесены в `.gitignore` и архивируются исключительно в закрытое хранилище артефактов (Secure CI Artifact Storage / S3 / Private NAS).

---

## 3. Инструменты и способы деобфускации

### Способ 1: Использование утилиты ProGuard ReTrace (CLI)

ProGuard генерирует файлы сопоставления стандартного формата ProGuard. Вы можете использовать инструмент `retrace`:

```powershell
# Синтаксис через ProGuard ReTrace:
java -cp "build/protection/proguard/proguard.jar" proguard.retrace.ReTrace build/protection/mappings/release-mapping.txt user_crash.txt
```

### Способ 2: Встроенный деобфускатор IntelliJ IDEA

1. Откройте проект NivoratClient в IntelliJ IDEA.
2. В верхнем меню выберите **Analyze** → **Unscramble Stack Trace...**
3. В поле **Log/Stack Trace** вставьте полученный от пользователя текст ошибки.
4. Отметьте чекбокс **Use unscramble tool**.
5. Выберите тип деобфускатора (**ProGuard**).
6. В поле **Path to mapping file** укажите `build/protection/mappings/release-mapping.txt`.
7. Нажмите **OK**. Стек-трейс моментально преобразуется в исходные названия классов и методов с активными гиперссылками на строки в IDE!
