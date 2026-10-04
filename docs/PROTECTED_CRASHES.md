# NivoratClient: Деобфускация крэш-логов и стек-трейсов (Phase 16)

## 1. Введение

В защищенных релизах NivoratClient имена классов, методов, полей и номеров строк классов ядра обфусцируются с помощью PreEmptive DashO.
В случае возникновения непредвиденного исключения у пользователя сгенерированный крэш-репорт Minecraft (`crash-reports/crash-YYYY-MM-DD.txt`) или лог (`logs/latest.log`) будет содержать нечитаемый стек-трейс вида:

```text
java.lang.NullPointerException: Cannot invoke method "a()" on null object
    at dev.nivorat.arc.a.b(Unknown Source)
    at dev.nivorat.arc.c.a(Unknown Source)
    at activity.client.a.b.onTick(Unknown Source)
    ...
```

Для того чтобы разработчик мог оперативно локализовать и исправить ошибку, релизный пайплайн NivoratClient сохраняет приватные файлы маппингов (Change Log / Renaming Map) на сборочном сервере.

> ⚠️ **КРИТИЧЕСКОЕ ПРАВИЛО БЕЗОПАСНОСТИ**:
> Файлы маппингов **НИКОГДА** не должны попадать внутрь распространяемого релизного JAR! 
> Автоматический верификатор `JarSecurityVerifier` завершает сборку с фатальной ошибкой, если в JAR обнаруживается файл `*map*.txt`, `*mapping*.txt` или `.dox`.

---

## 2. Структура хранения маппингов на стороне разработчика

При выполнении команды `./gradlew protectedRelease` создается каталог метаданных конкретного билда:

```text
build/protection/mappings/<version>-<build_id>/
├── mapping.txt             <- Основной файл соответствия символов (ProGuard/DashO format)
├── build-info.json         <- Метаданные релиза (commit SHA, config hash, jar sha256)
└── report.html             <- Подробный отчет DashO о примененных трансформациях
```

Содержимое `build-info.json`:
```json
{
  "version": "1.0.3",
  "buildId": "release-20261004-114701",
  "commitSha": "3d5f992a1c...",
  "protectionProfile": "DashO-Tiered-v1.4",
  "jarSha256": "9b99e3c8690146872ed7ef0751cf02714818d5c41509f2f410b2f01f710c8524"
}
```

Все каталоги маппингов в `build/protection/mappings/` занесены в `.gitignore` и архивируются исключительно в закрытое хранилище артефактов (Secure CI Artifact Storage / S3 / Private NAS).

---

## 3. Инструменты и способы деобфускации

### Способ 1: Использование утилиты Retrace (Рекомендуемый CLI)

DashO генерирует файлы сопоставления, совместимые со стандартным форматом Retrace. Вы можете использовать `retrace` из поставки DashO или ProGuard:

```powershell
# Синтаксис:
java -jar "$env:DASHO_HOME/lib/retrace.jar" <путь_к_mapping.txt> <путь_к_файлу_с_крэшем.txt>

# Пример деобфускации краша пользователя:
java -jar "$env:DASHO_HOME/lib/retrace.jar" `
    build/protection/mappings/1.0.3/mapping.txt `
    C:\Users\Admin\Downloads\user_crash.txt
```

### Способ 2: Встроенный деобфускатор IntelliJ IDEA

1. Откройте проект NivoratClient в IntelliJ IDEA.
2. В верхнем меню выберите **Analyze** → **Unscramble Stack Trace...**
3. В поле **Log/Stack Trace** вставьте полученный от пользователя текст ошибки.
4. Отметьте чекбокс **Use unscramble tool**.
5. Выберите тип деобфускатора (например, **ProGuard / DashO Retrace**).
6. В поле **Path to mapping file** укажите `build/protection/mappings/<версия>/mapping.txt`.
7. Нажмите **OK**. Стек-трейс моментально преобразуется в исходные названия классов и методов с активными гиперссылками на строки в IDE!

---

## 4. Пример деобфускации (До и После)

#### Зашифрованный стек-трейс из логов игрока:
```text
java.lang.ArithmeticException: / by zero
    at dev.nivorat.arc.d.a(Unknown Source)
    at dev.nivorat.arc.a.a(Unknown Source)
    at dev.nivorat.arc.a.b(Unknown Source)
    at activity.client.module.a.b.a(Unknown Source)
    at net.minecraft.class_310.handler$zcc000$activity$client$mixin$onTick(MinecraftClient.java:1845)
```

#### Расшифрованный стек-трейс после Retrace:
```text
java.lang.ArithmeticException: / by zero
    at dev.nivorat.arc.ArcMotionProfile.calculateOptimalPlacement(ArcMotionProfile.java:142)
    at dev.nivorat.arc.AutoCartController.calculateTrajectory(AutoCartController.java:88)
    at dev.nivorat.arc.AutoCartController.onTick(AutoCartController.java:54)
    at activity.client.module.impl.movement.AutoCartModule.onClientTick(AutoCartModule.java:62)
    at net.minecraft.client.MinecraftClient.handler$zcc000$activity$client$mixin$onTick(MinecraftClient.java:1845)
```

> 💡 **Обратите внимание**: Имя Mixin-метода `handler$zcc000$activity$client$mixin$onTick` изначально читаемо, так как Mixin-классы отнесены к **Tier 0 (ABI KEEP)** и не подвергаются разрушающему переименованию. Ошибку в методе `ArcMotionProfile` теперь можно исправить за пару минут!

---

## 5. Чек-лист для релиза новой версии

Перед выпуском каждого публичного релиза для комьюнити:
1. Запустите сборку: `./gradlew protectedRelease`.
2. Убедитесь, что файл `build/protection/mappings/<version>/mapping.txt` успешно создан и сохранен в надежное место.
3. Зафиксируйте в релизной таблице SHA-256 хэш полученного `NivoratClient-Protected.jar`.
4. При обращении пользователей в техническую поддержку требуйте указать точную версию клиента и приложить файл `crash-reports/crash-*.txt`.
