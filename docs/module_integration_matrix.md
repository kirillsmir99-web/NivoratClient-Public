# Module Integration Matrix: Full Legacy Module Audit

Документ аудита и технического анализа 12 отдельных модификаций Minecraft 1.21.11 (Fabric Loader) для интеграции в единый монолитный клиент **NivoratClient**.

---

## 1. Детальный аудит 12 модификаций

---

### MODULE: AutoMace
- **SOURCE REPOSITORY**: `SingleMods/AutoMace/source`
- **VERSION**: `2.4.6`
- **ENTRYPOINT**: `net.redstone.optimizer.RedstoneOptimizerClient`
- **MAIN CLASSES**:
  - `net.redstone.optimizer.RedstoneOptimizerClient` (клиентский инициализатор)
  - `net.redstone.optimizer.config.RedstoneOptimizerConfig` (хранилище настроек)
  - `net.redstone.optimizer.engine.RedstoneTickEngine` (тик-движок боевой логики)
  - `net.redstone.optimizer.gui.RedstoneSettingsScreen` (автономный экран настроек)
  - `net.fabricmc.pack.api.MasterScreen` (устаревший общий хаб)
  - `net.fabricmc.pack.api.KeybindManager` (устаревший менеджер горячих клавиш)
  - `net.fabricmc.pack.api.SafeSlotManager` (менеджер слотов хотбара)
  - `net.fabricmc.pack.api.GaussianTimingEngine` (генератор случайных нормальных задержек)
- **CONFIG**:
  - Класс: `RedstoneOptimizerConfig`
  - Файлы на диске: `config/redstone_optimizer.properties` (основной), `config/impact_tweaks.properties` (легаси)
  - Формат: Java `Properties` (key=value)
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — активация модуля.
  2. `sourceMode` (`int`, default: `2` [MODE_SWORD_AND_AXE]) — выбор оружия для атаки:
     - `0`: `MODE_SWORD_ONLY` (только мечи)
     - `1`: `MODE_AXE_ONLY` (только топоры)
     - `2`: `MODE_SWORD_AND_AXE` (мечи и топоры)
  3. `enchantMode` (`int`, default: `0` [ENCHANT_SMART]) — логика выбора чар булавы:
     - `0`: `ENCHANT_SMART` (умный выбор по броне цели)
     - `1`: `ENCHANT_BREACH_ONLY` (только Пробивание)
     - `2`: `ENCHANT_DENSITY_ONLY` (только Плотность)
  4. `missBehavior` (`int`, default: `0` [MISS_SWORD_HIT]) — поведение при промахе:
     - `0`: `MISS_SWORD_HIT` (удар исходным оружием)
     - `1`: `MISS_EMPTY_SWAP` (свап без удара)
  5. `randomDelay` (`boolean`, default: `true`) — рандомизация задержки возврата оружия.
  6. `restoreDelayMs` (`int`, default: `90`, min: `10`, max: `500`, step: `5`, unit: `ms`) — базовая задержка возврата оружия.
  7. `randomMaxRestoreDelayMs` (`int`, default: `120`, min: `10`, max: `500`, step: `5`, unit: `ms`) — верхняя граница рандомной задержки.
  8. `legitMode` (`boolean`, default: `true`) — проверка античита (GrimAC Safe тайминги).
  9. `missChance` (`int`, default: `10`, min: `0`, max: `100`, step: `1`, unit: `%`) — симуляция промаха человека.
- **KEYBINDS**:
  - Менеджер: `KeybindManager` (глобальный GLFW хук)
  - Идентификатор: `"auto_mace"`
  - Дефолт: `-1` (не назначена, без модификаторов)
- **EVENTS**:
  - `net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT` (перехват атаки)
  - `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK` (такт симуляции свапа)
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/redstone_optimizer/lang/en_us.json`
  - `assets/redstone_optimizer/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `RedstoneSettingsScreen` (кастомный темный экран с вертикальными строками слайдеров `SliderWidget` и кнопок переключения).
- **RUNTIME FLOW**:
  1. Игрок бьет сущность (`AttackEntityCallback.EVENT`).
  2. Проверяется валидность цели (живой игрок/моб, не блокирован стеной).
  3. Проверяется соответствие активного оружия `sourceMode`.
  4. Происходит мгновенный расчет наилучшей булавы в хотбаре по чарам `Breach`/`Density`.
  5. Отправляется переключение слота на булаву (`selectSlot`), наносится удар.
  6. Запускается таймер задержки `GaussianTimingEngine`.
  7. По истечении задержки слот восстанавливается обратно в исходное оружие.
- **MIGRATION RISKS**:
  - **КРИТИЧЕСКИЙ РИСК**: Пакетная синхронизация слотов. Любое изменение таймингов возврата или рассинхронизация с сервером приводит к детектам античита (FastSwap / BadPackets).
  - Конфликт дублирующегося пакета `net.fabricmc.pack.api`.
- **TARGET NIVORAT CATEGORY**: `COMBAT`
- **TARGET MODULE ID**: `auto_mace`

---

### MODULE: AutoSpear
- **SOURCE REPOSITORY**: `SingleMods/AutoSpear/source`
- **VERSION**: `2.0.1`
- **ENTRYPOINT**: `dev.momentum.SpearSwapClient`
- **MAIN CLASSES**:
  - `dev.momentum.SpearSwapClient` (клиентский инициализатор)
  - `dev.momentum.SpearSwapController` (боевой контроллер выпада копьем)
  - `dev.momentum.SpearConfig` (конфигурация)
  - `dev.momentum.SpearScreen` (автономный GUI)
  - `net.fabricmc.pack.api.*` (дублирующийся общий фреймворк)
- **CONFIG**:
  - Класс: `SpearConfig`
  - Файл на диске: `config/momentum_tweaks.properties`
  - Формат: Java `Properties`
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение авто-копья.
  2. `securityMode` (`int`, default: `0` [MODE_LEGIT]) — профиль безопасности античита:
     - `0`: `MODE_LEGIT` (задержка >= 140 ms)
     - `1`: `MODE_SEMI_LEGIT` (задержка >= 70 ms)
     - `2`: `MODE_RAGE` (задержка 0 ms)
  3. `priorityMode` (`int`, default: `0` [PRIORITY_AUTO]) — приоритет выбора уровня чар Lunge:
     - `0`: `PRIORITY_AUTO` (авто-выбор наивысшего уровня)
     - `1`: `PRIORITY_LUNGE_1` (Lunge I)
     - `2`: `PRIORITY_LUNGE_2` (Lunge II)
     - `3`: `PRIORITY_LUNGE_3` (Lunge III)
     - `4`: `PRIORITY_RANDOM` (случайный уровень)
  4. `randomDelay` (`boolean`, default: `true`) — рандомизация тайминга выпада.
  5. `maxDelayMs` (`int`, default: `185`, min: `10`, max: `500`, step: `5`, unit: `ms`) — максимальная задержка возврата.
  6. `missChance` (`int`, default: `0`, min: `0`, max: `100`, step: `1`, unit: `%`) — процент намеренных пропусков.
- **KEYBINDS**:
  - Идентификатор: `"spear_swap"`
  - Дефолт: `GLFW.GLFW_KEY_TAB` (без модификаторов)
- **EVENTS**:
  - `AttackEntityCallback.EVENT`
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/momentumtweaks/lang/en_us.json`
  - `assets/momentumtweaks/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (~1.21.1), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `SpearScreen` (панель настроек со слайдерами задержки и шанса промаха).
- **RUNTIME FLOW**:
  1. Перехват удара через `AttackEntityCallback` или нажатие `TAB`.
  2. Проверка расстояния луча (`hasLineOfSight`) и валидности цели.
  3. Поиск копья в хотбаре с чарами Lunge в соответствии с `priorityMode`.
  4. Свап на копье, эмуляция рывка/удара.
  5. Задержка с учетом выбранного `securityMode`.
  6. Возврат оружия на исходный слот.
- **MIGRATION RISKS**:
  - Расчет дистанции атаки копьем и рейкаст прицеливания (`findTargetAlongRay`).
  - Коллизия пакета `net.fabricmc.pack.api`.
- **TARGET NIVORAT CATEGORY**: `COMBAT`
- **TARGET MODULE ID**: `auto_spear`

---

### MODULE: AutoShieldbreaker
- **SOURCE REPOSITORY**: `SingleMods/AutoShieldbreaker/source`
- **VERSION**: `1.4.2`
- **ENTRYPOINT**: `dev.nivora.NivoraClient`
- **MAIN CLASSES**:
  - `dev.nivora.NivoraClient` (инициализатор)
  - `dev.nivora.ShieldAxeController` (машина состояний пробития щита)
  - `dev.nivora.ShieldBreakerConfig` (конфигурация)
  - `dev.nivora.ShieldBreakerScreen` (GUI)
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `ShieldBreakerConfig`
  - Файл на диске: `config/autoshieldbreaker.properties`
  - Формат: Java `Properties`
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение авто-пробития.
  2. `mode` (`int`, default: `0` [MODE_FULL_AUTO]) — режим работы:
     - `0`: `MODE_FULL_AUTO` (полный автомат при обнаружении блока щитом)
     - `1`: `MODE_SEMI_AUTO` (полуавтомат, требует клика игрока)
  3. `legitMode` (`boolean`, default: `true`) — легитный режим (проверка поля зрения и поворота).
  4. `abortOnManualSwitch` (`boolean`, default: `true`) — прерывать последовательность, если игрок сам сменил слот.
  5. `chance` (`int`, default: `100`, min: `10`, max: `100`, step: `1`, unit: `%`) — шанс срабатывания.
  6. `triggerDistance` (`double`, default: `2.85`, min: `1.8`, max: `2.85`, step: `0.05`, unit: `bl`) — дистанция детекции блокирования.
  7. `switchDelayMs` (`int`, default: `50`, min: `30`, max: `250`, step: `5`, unit: `ms`) — задержка перед ударом топором.
  8. `randomDelay` (`boolean`, default: `true`) — рандомизация задержки удара и возврата.
  9. `randomMaxDelayMs` (`int`, default: `70`, min: `30`, max: `200`, step: `5`, unit: `ms`) — максимум рандомной задержки удара.
  10. `restoreDelayMs` (`int`, default: `50`, min: `30`, max: `250`, step: `5`, unit: `ms`) — задержка возврата оружия.
  11. `randomMaxRestoreDelayMs` (`int`, default: `65`, min: `30`, max: `200`, step: `5`, unit: `ms`) — максимум рандомного возврата.
  12. `cooldownTicks` (`int`, default: `4`, min: `2`, max: `20`, step: `1`, unit: `ticks`) — внутренний кулдаун между циклами.
- **KEYBINDS**:
  - Идентификатор: `"shield_breaker"`
  - Дефолт: `GLFW.GLFW_KEY_J` (ctrl=true, shift=true, alt=false)
- **EVENTS**:
  - `AttackEntityCallback.EVENT`
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/animationsmooth/lang/en_us.json`
  - `assets/animationsmooth/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `ShieldBreakerScreen` (полноразмерный экран с 6 слайдерами и 3 переключателями).
- **RUNTIME FLOW**:
  1. `ShieldAxeController.handleIdle` сканирует цели в радиусе `triggerDistance`.
  2. Проверяется статус блокирования цели: `isTargetShielding(target)`.
  3. Поиск топора в хотбаре (`findAxeHotbarSlot`).
  4. Перевод машины состояний: `IDLE -> SWAPPING -> AWAITING_HIT -> RESTORING -> COOLDOWN`.
  5. Нанесение сокрушительного удара, деактивация щита противника.
  6. Возврат прежнего слота оружия.
- **MIGRATION RISKS**:
  - **ВЫСОКИЙ РИСК**: Детекты античита (ShieldBreaker, FastSwap). Машина состояний контроллера должна оставаться строго инвариантной.
- **TARGET NIVORAT CATEGORY**: `COMBAT`
- **TARGET MODULE ID**: `auto_shieldbreaker`

---

### MODULE: AutoStunSlime (Target: AutoStunSlam)
- **SOURCE REPOSITORY**: `SingleMods/AutoStunSlime/source`
- **VERSION**: `1.1.2`
- **ENTRYPOINT**: `dev.sunder.SunderClient`
- **MAIN CLASSES**:
  - `dev.sunder.SunderClient` (клиентский загрузчик)
  - `dev.sunder.SunderController` (двухэтапный комбо-контроллер)
  - `dev.sunder.SunderConfig` (хранилище настроек)
  - `dev.sunder.SunderScreen` (GUI)
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `SunderConfig`
  - Файл на диске: `config/autostunslime.properties`
  - Формат: Java `Properties`
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение комбо.
  2. `mode` (`int`, default: `0` [MODE_FULL_AUTO]) — режим работы (`0 = FULL_AUTO`, `1 = SEMI_AUTO`).
  3. `legitMode` (`boolean`, default: `true`) — проверка видимости и вектора движения.
  4. `chance` (`int`, default: `75`, min: `10`, max: `100`, step: `1`, unit: `%`) — вероятность срабатывания.
  5. `triggerDistance` (`double`, default: `2.4`, min: `1.5`, max: `3.0`, step: `0.05`, unit: `bl`) — дистанция триггера.
  6. `airTimeSec` (`double`, default: `1.0`, min: `0.1`, max: `3.0`, step: `0.1`, unit: `s`) — требуемое время нахождения цели/игрока в воздухе.
  7. `randomDelay` (`boolean`, default: `true`) — рандомизация фаз задержки.
  8. `axeDelayMs` (`int`, default: `45`, min: `20`, max: `200`, step: `5`, unit: `ms`) — задержка удара топором.
  9. `randomMaxDelayMs` (`int`, default: `100`, min: `25`, max: `200`, step: `5`, unit: `ms`) — предел рандома топора.
  10. `maceDelayMs` (`int`, default: `45`, min: `20`, max: `200`, step: `5`, unit: `ms`) — задержка удара булавой.
  11. `randomMaxMaceDelayMs` (`int`, default: `55`, min: `20`, max: `100`, step: `5`, unit: `ms`) — предел рандома булавы.
  12. `restoreDelayMs` (`int`, default: `50`, min: `20`, max: `200`, step: `5`, unit: `ms`) — задержка возврата слота.
  13. `randomMaxRestoreDelayMs` (`int`, default: `50`, min: `20`, max: `100`, step: `5`, unit: `ms`) — предел рандома возврата.
  14. `cooldownTicks` (`int`, default: `15`, min: `5`, max: `40`, step: `1`, unit: `ticks`) — пауза после завершения связки.
- **KEYBINDS**:
  - Идентификатор: `"auto_stun"`
  - Дефолт: `GLFW.GLFW_KEY_M` (ctrl=true, shift=true, alt=false)
- **EVENTS**:
  - `AttackEntityCallback.EVENT`
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/crosshairplus/lang/en_us.json`
  - `assets/crosshairplus/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `SunderScreen` (экран с 7 слайдерами задержек и кнопками режимов).
- **RUNTIME FLOW**:
  1. `SunderController` отслеживает состояние падения (`isAirborneConditionMet`).
  2. Находит цель, находящуюся в воздухе либо падающую после прыжка.
  3. Проверяет наличие топора и булавы в хотбаре.
  4. Этап 1: Свап на топор -> удар (пробивает щит / оглушает).
  5. Этап 2: Свап на булаву -> сокрушительный удар в падении с критом.
  6. Этап 3: Восстановление первоначального оружия и запуск кулдауна.
- **MIGRATION RISKS**:
  - **НАИВЫСШИЙ РИСК**: Самая сложная многошаговая цепочка свапов в игре. Малейшая рассинхронизация ломает второй удар булавой.
  - Требование обратной совместимости алиаса `auto_stun_slime` -> `auto_stun_slam`.
- **TARGET NIVORAT CATEGORY**: `COMBAT`
- **TARGET MODULE ID**: `auto_stun_slam` (алиас: `auto_stun_slime`)

---

### MODULE: AutoTotem
- **SOURCE REPOSITORY**: `SingleMods/AutoTotem/source`
- **VERSION**: `1.0.3`
- **ENTRYPOINT**: `dev.autototem.AutoTotemClient`
- **MAIN CLASSES**:
  - `dev.autototem.AutoTotemClient`
  - `dev.autototem.AutoTotemController` (контроллер мониторинга здоровья и свопа левой руки)
  - `dev.autototem.AutoTotemConfig`
  - `dev.autototem.AutoTotemScreen`
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `AutoTotemConfig`
  - Файл на диске: `config/autototem.properties`
  - Формат: Java `Properties`
- **SETTINGS**:
  1. `triggerHearts` (`int`, default: `3`, min: `1`, max: `9`, step: `1`, unit: `hearts`) — порог сердец для взятия тотема (3 сердца = 6 HP).
  2. `restoreHearts` (`int`, default: `6`, min: `4`, max: `10`, step: `1`, unit: `hearts`) — порог восстановления здоровья для возврата исходного предмета (6 сердец = 12 HP).
  3. `chance` (`int`, default: `100`, min: `10`, max: `100`, step: `1`, unit: `%`) — вероятность активации.
  4. `returnItem` (`boolean`, default: `true`) — возвращать ли исходный предмет в левую руку.
  5. `returnOnPop` (`boolean`, default: `true`) — мгновенно брать следующий тотем или возвращать предмет после срабатывания тотема.
  6. `mode` (`int`, default: `1`) — режим работы:
     - `0`: `OFF`
     - `1`: `PREDICTIVE_REACTIVE` (умный мониторинг входящего урона)
     - `2`: `RAGE_IMMEDIATE` (мгновенный безоговорочный свап)
- **KEYBINDS**:
  - Идентификатор: `"autototem"`
  - Дефолт: `-1` (не назначена)
- **EVENTS**:
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/effectoptimizer/lang/en_us.json`
  - `assets/effectoptimizer/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `AutoTotemScreen` (слайдеры порогов сердец, кнопка пресетов 3/5/7 HP).
- **RUNTIME FLOW**:
  1. Каждый клиентский такт проверяется `player.getHealth()`.
  2. Если здоровье падает ниже `triggerHearts * 2`, сканируется инвентарь на наличие `Items.TOTEM_OF_UNDYING`.
  3. Сохраняется предмет во второй руке (`offhandSlot = 45`).
  4. Отправляется пакет взаимодействия со слотом инвентаря (`sendOffhandSwap`), тотем экипируется.
  5. Когда здоровье превышает `restoreHearts * 2` (или тотем сработал), второй предмет возвращается на место.
- **MIGRATION RISKS**:
  - **ВЫСОКИЙ РИСК**: Пакетные манипуляции с инвентарем (slot 45 offhand swap). Ошибки могут вызвать выброс предметов или кик за спам слотами.
- **TARGET NIVORAT CATEGORY**: `DEFENSE`
- **TARGET MODULE ID**: `auto_totem`

---

### MODULE: AutoCart
- **SOURCE REPOSITORY**: `SingleMods/AutoCart/source`
- **VERSION**: `2.1.2`
- **ENTRYPOINT**: `dev.virion.arc.VirionArcClient`
- **MAIN CLASSES**:
  - `dev.virion.arc.VirionArcClient`
  - `dev.virion.arc.VirionArcController` (баллистический и рейкаст-контроллер установки)
  - `dev.virion.arc.MorrowConfig`
  - `dev.virion.arc.MorrowScreen`
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `MorrowConfig`
  - Файл на диске: `config/autocart.properties`
  - Формат: Java `Properties`
- **SETTINGS**:
  1. `placementChance` (`int`, default: `100`, min: `10`, max: `100`, step: `1`, unit: `%`) — шанс установки.
  2. `legitMode` (`boolean`, default: `true`) — легитный режим (проверка прямой видимости и коллизий блоков).
  3. `randomDelay` (`boolean`, default: `true`) — рандомизация тайминга кликов.
  4. `maxDistance` (`double`, default: `4.4`, min: `1.5`, max: `4.5`, step: `0.1`, unit: `bl`) — максимальная дистанция установки.
  5. `allowSelfCart` (`boolean`, default: `false`) — разрешать ли установку под себя.
  6. `allowPitPlacement` (`boolean`, default: `true`) — разрешать установку в углубления и ямы.
  7. `minDelayMs` (`int`, default: `70`, min: `10`, max: `200`, step: `5`, unit: `ms`) — минимальная задержка между рельсой и вагонеткой.
  8. `maxDelayMs` (`int`, default: `110`, min: `10`, max: `300`, step: `5`, unit: `ms`) — максимальная задержка.
  9. `preset` (`int`, default: `1` [PRESET_MEDIUM]):
     - `0`: `PRESET_FAST` (минимальные задержки)
     - `1`: `PRESET_MEDIUM` (баланс надежности и скорости)
     - `2`: `PRESET_SAFE` (максимальная маскировка под человека)
- **KEYBINDS**:
  - Идентификатор: `"cart"`
  - Дефолт: `GLFW.GLFW_KEY_I` (ctrl=true, shift=true, alt=false)
- **EVENTS**:
  - `ClientTickEvents.END_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/physicstweaks/lang/en_us.json`
  - `assets/physicstweaks/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `MorrowScreen` (пресеты Fast/Medium/Safe, слайдеры дистанции и задержек).
- **RUNTIME FLOW**:
  1. Триггер по горячей клавише или отслеживанию натяжения лука (`trackBowRelease`).
  2. Расчет точки приземления / позиции противника через трассировку лучей `raycast`.
  3. Определение твердой опорной поверхности `findSolidGroundBelow`.
  4. Проверка возможности установки рельс `canPlaceRail`.
  5. Быстрый свап на рельсу -> установка `interactAtTop`.
  6. Свап на TNT-вагонетку -> постановка на рельсу `interactOnRail`.
  7. Очистка состояния сессии.
- **MIGRATION RISKS**:
  - **ВЫСОКИЙ РИСК**: Сложные пространственные расчеты векторов и углов установки. Нарушение порядка пакетов приводит к ошибкам сервера ("Block cannot be placed").
- **TARGET NIVORAT CATEGORY**: `DEFENSE`
- **TARGET MODULE ID**: `auto_cart`

---

### MODULE: AutoAnchor
- **SOURCE REPOSITORY**: `SingleMods/AutoAnchor/source`
- **VERSION**: `1.0.0`
- **ENTRYPOINT**: `dev.luminance.AnchorClient`
- **MAIN CLASSES**:
  - `dev.luminance.AnchorClient`
  - `dev.luminance.AnchorController` (контроллер зарядки и подрыва якоря)
  - `dev.luminance.AnchorConfig`
  - `dev.luminance.AnchorScreen`
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `AnchorConfig`
  - Файл на диске: `config/luminance_tweaks.properties`
  - Формат: key=value файл
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение авто-якоря.
  2. `autoExplode` (`boolean`, default: `false`) — автоматически взрывать якорь после зарядки.
  3. `autoReturn` (`boolean`, default: `true`) — возвращать исходный предмет после клика.
  4. `chargeDelayTicks` (`int`, default: `1`, min: `0`, max: `10`, step: `1`, unit: `ticks`) — пауза перед зарядкой светокамнем.
  5. `explodeDelayTicks` (`int`, default: `1`, min: `0`, max: `10`, step: `1`, unit: `ticks`) — пауза перед взрывом.
  6. `chance` (`int`, default: `85`, min: `10`, max: `100`, step: `1`, unit: `%`) — вероятность срабатывания.
  7. `legitMode` (`boolean`, default: `true`) — проверка луча взгляда игрока.
  8. `targetCharges` (`int`, default: `1`, min: `1`, max: `4`, step: `1`, unit: `charges`) — количество светокамней для зарядки.
  9. `preset` (`String`, default: `"BALANCED"`):
     - `"FAST"`: задержки 1 тик, шанс 100%
     - `"MEDIUM"` / `"BALANCED"`: задержки 1 тик, шанс 85%
     - `"SAFE"`: задержки 2 тика, шанс 70%
- **KEYBINDS**:
  - Идентификатор: `"auto_anchor"`
  - Дефолт: `-1` (не назначена)
- **EVENTS**:
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/luminancetweaks/lang/en_us.json`
  - `assets/luminancetweaks/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `AnchorScreen` (слайдеры тиков зарядки/детонации, выбор пресета).
- **RUNTIME FLOW**:
  1. Игрок смотрит на блок `RespawnAnchorBlock`.
  2. `AnchorController` проверяет дистанцию и прямую видимость `hasLineOfSight`.
  3. Поиск светокамня `findGlowstoneSlot`.
  4. Свап слота, отправка пакета ПКМ на якорь до достижения `targetCharges`.
  5. Если `autoExplode = true`, нахождение безопасного предмета детонации `resolveDetonateSlot`, повторный клик для взрыва.
  6. Возврат исходного предмета.
- **MIGRATION RISKS**:
  - **СРЕДНИЙ РИСК**: Случайный самоподрыв при неверной дистанции или сбое выбора предмета для клика.
- **TARGET NIVORAT CATEGORY**: `DEFENSE`
- **TARGET MODULE ID**: `auto_anchor`

---

### MODULE: CartRefill
- **SOURCE REPOSITORY**: `SingleMods/CartRefill/source`
- **VERSION**: `2.4.1`
- **ENTRYPOINT**: `dev.storage.CartRefillClient`
- **MAIN CLASSES**:
  - `dev.storage.CartRefillClient`
  - `dev.storage.CartRefillController` (контроллер пополнения хотбара)
  - `dev.storage.RefillConfig`
  - `dev.storage.RefillScreen`
  - `dev.storage.CartHudOverlay` (дублирующийся оверлей)
  - `dev.storage.CartHudEditorScreen` (дублирующийся редактор)
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `RefillConfig`
  - Файлы на диске: `config/storage_tweaks.properties`, `config/cart_hud.properties`
  - Формат: key=value
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение автопополнения.
  2. `refillDelayTicks` (`int`, default: `2`, min: `0`, max: `10`, step: `1`, unit: `ticks`) — задержка перекладывания вагонетки в хотбар.
  3. `chance` (`int`, default: `100`, min: `10`, max: `100`, step: `1`, unit: `%`) — шанс пополнения.
  4. `legitMode` (`boolean`, default: `true`) — легитный режим (пополнение только при открытом инвентаре/контейнере).
  5. `autoClose` (`boolean`, default: `true`) — автоматически закрывать контейнер после пополнения.
  6. `showHud` (`boolean`, default: `true`) — отображать встроенный индикатор количества вагонеток.
  7. `randomDelay` (`boolean`, default: `true`) — рандомизация кликов инвентаря.
  8. `customX` (`int`, default: `-1`) — позиция HUD по X (-1 = дефолт).
  9. `customY` (`int`, default: `-1`) — позиция HUD по Y (-1 = дефолт).
- **KEYBINDS**:
  - Идентификатор: `"cart_refill"`
  - Дефолт: `-1` (не назначена)
- **EVENTS**:
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/storagetweaks/lang/en_us.json`
  - `assets/storagetweaks/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `RefillScreen`, `CartHudEditorScreen`.
- **RUNTIME FLOW**:
  1. Контроллер ведет снимок инвентаря `updateSnapshot`.
  2. При обнаружении расхода вагонетки в хотбаре (`detectSpentCart`) планирует пополнение `scheduleRefill`.
  3. При открытии контейнера находит запасную вагонетку `findInventoryCart`.
  4. Совершает перемещение в целевой слот хотбара, при необходимости закрывает экран `processLegitClosing`.
- **MIGRATION RISKS**:
  - **КРИТИЧЕСКИЙ КОНФЛИКТ**: Полное дублирование классов HUD (`CartHudOverlay` и `CartHudEditorScreen`) с модом `CartHUD`. Требует четкого разделения: CartRefill отвечает только за логику пополнения, а CartHUD — за рендеринг и позиционирование оверлея.
- **TARGET NIVORAT CATEGORY**: `DEFENSE`
- **TARGET MODULE ID**: `cart_refill`

---

### MODULE: HPReaper
- **SOURCE REPOSITORY**: `SingleMods/HPReaper/source`
- **VERSION**: `1.2.0`
- **ENTRYPOINT**: `dev.hpreaper.VitalityClient`
- **MAIN CLASSES**:
  - `dev.hpreaper.VitalityClient`
  - `dev.hpreaper.VitalityConfig`
  - `dev.hpreaper.HealthHudOverlay` (рендерер современного HUD бара с анимацией)
  - `dev.hpreaper.HpHudEditorScreen` (интерактивный экран позиционирования с док-панелью)
  - Вспомогательный UI: `dev.hpreaper.ui.ActivityButton`, `ActivityColors`, `ActivityGuiRenderer`
- **CONFIG**:
  - Класс: `VitalityConfig`
  - Файл на диске: `config/hp_reaper.properties`
  - Формат: key=value
- **SETTINGS**:
  1. `ownHealthX` (`int`, default: `-1`) — позиция своего HP.
  2. `ownHealthY` (`int`, default: `-1`).
  3. `crosshairTargetX` (`int`, default: `-1`) — позиция цели под прицелом.
  4. `crosshairTargetY` (`int`, default: `-1`).
  5. `targetHealthX` (`int`, default: `-1`) — позиция последней атакованной цели.
  6. `targetHealthY` (`int`, default: `-1`).
  7. `diffX` (`int`, default: `-1`) — позиция разницы здоровья.
  8. `diffY` (`int`, default: `-1`).
  9. `displayMode` (`DisplayMode`, default: `OWN_HEALTH`):
     - `OWN_HEALTH`: только свое здоровье
     - `CROSSHAIR_AND_TARGET`: под прицелом и атакованная цель
     - `TARGET_HEALTH`: только цель
     - `DIFFERENCE`: разница между HP игрока и цели
     - `ALL`: полный блок информации
  10. `targetFilter` (`TargetFilter`, default: `ALL_ENTITIES`):
     - `PLAYERS_ONLY`: только игроки
     - `HOSTILE_ONLY`: только враждебные мобы
     - `ALL_ENTITIES`: любые живые сущности
- **KEYBINDS**:
  - `key.hpreaper.open_editor` -> `GLFW.GLFW_KEY_J` (категория: `hpreaper.controls`)
  - `key.hpreaper.cycle_mode` -> не назначена
  - В `KeybindManager`: `"hpreaper"` -> `GLFW.GLFW_KEY_H`
- **EVENTS**:
  - `AttackEntityCallback.EVENT` (запоминание атакованного оппонента)
  - `ClientTickEvents.END_CLIENT_TICK`
  - `ClientCommandRegistrationCallback.EVENT` (команда `/hpreaper`)
  - `HudRenderCallback.EVENT` (отрисовка плавающего оверлея)
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/hpreaper/lang/en_us.json`
  - `assets/hpreaper/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `HpHudEditorScreen` (интерактивное перетаскивание плашек мышью с док-панелью сброса и настройки фильтров).
- **RUNTIME FLOW**:
  1. `AttackEntityCallback` запоминает последний атакованный `LivingEntity`.
  2. Каждый кадр `HudRenderCallback` рассчитывает HP, золотые сердечки (Absorption) и процент оставшегося здоровья.
  3. Отрисовывается полупрозрачная плашка с размытием, иконками сердец и числовыми значениями.
- **MIGRATION RISKS**:
  - **НИЗКИЙ РИСК**: Модуль чисто визуальный, не вмешивается в сокеты, пакеты и боевые такты.
  - Коллизия горячей клавиши `GLFW_KEY_J` (в оригинале конфликтовала с AutoShieldbreaker).
- **TARGET NIVORAT CATEGORY**: `UTILITY`
- **TARGET MODULE ID**: `hp_reaper`

---

### MODULE: AutoTool
- **SOURCE REPOSITORY**: `SingleMods/AutoTool/source`
- **VERSION**: `1.0.0`
- **ENTRYPOINT**: `ru.elarion.autotool.AutoToolClient`
- **MAIN CLASSES**:
  - `ru.elarion.autotool.AutoToolClient`
  - `ru.elarion.autotool.AutoToolConfig`
  - `ru.elarion.autotool.AutoToolEngine` (алгоритм выбора инструмента и расчета урона/эффективности)
  - `ru.elarion.autotool.AutoToolScreen`
  - Миксины: `ru.elarion.autotool.mixin.ClientPlayerInteractionManagerMixin`, `ClientPlayerInteractionManagerAccessor`
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `AutoToolConfig`
  - Файл на диске: `config/autotool.json`
  - Формат: JSON (через Google `Gson`)
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение мода.
  2. `singleSlotMode` (`boolean`, default: `false`):
     - `false`: переключает активный слот хотбара (1-9) на нужный инструмент.
     - `true`: оставляет текущий слот, но подменяет в нем предмет через инвентарный клик.
  3. `legitMode` (`boolean`, default: `true`):
     - `true`: выбирает инструмент только из хотбара (GrimAC Safe).
     - `false`: может доставать инструмент из основного инвентаря / рюкзака.
  4. `combatGuard` (`boolean`, default: `true`) — блокировать автовыбор инструмента, если игрок наведен на другого игрока или ведет бой.
  5. `ignoreInstantBreak` (`boolean`, default: `true`) — не переключать инструмент для блоков, ломающихся мгновенно (факелы, трава).
  6. `lockWhileMining` (`boolean`, default: `true`) — фиксировать инструмент на все время непрерывного ломания блока.
  7. `durabilitySaver` (`boolean`, default: `true`) — защита от поломки инструмента.
  8. `durabilityThreshold` (`int`, default: `5`, min: `1`, max: `50`, step: `1`, unit: `durability`) — минимальный порог оставшейся прочности.
  9. `preferSilkTouch` (`boolean`, default: `false`) — отдавать предпочтение Шелковому касанию на ценных рудах.
  10. `restorePreviousItem` (`boolean`, default: `true`) — возвращать исходный предмет после завершения добычи.
- **KEYBINDS**:
  - `key.autotool.toggle` -> не назначена
  - `key.autotool.gui` -> `GLFW.GLFW_KEY_V` (категория: `autotool.controls`)
- **EVENTS**:
  - `AttackEntityCallback.EVENT` (детекция боя для CombatGuard)
  - `ClientTickEvents.START_CLIENT_TICK`
  - `ClientCommandRegistrationCallback.EVENT` (`/autotool`)
- **MIXINS**:
  - `activity.autotool.mixins.json`:
    - `ClientPlayerInteractionManagerMixin` -> инъекция в `attackBlock` и `updateBlockBreakingProgress`
    - `ClientPlayerInteractionManagerAccessor` -> доступ к приватным полям прогресса копания блока `currentBreakingProgress`
- **RESOURCES**:
  - `assets/autotool/lang/en_us.json`
  - `assets/autotool/lang/ru_ru.json`
  - `activity.autotool.mixins.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `AutoToolScreen` (кнопочный интерфейс переключения флагов).
- **RUNTIME FLOW**:
  1. Игрок зажимает ЛКМ на блоке.
  2. Миксин `ClientPlayerInteractionManagerMixin` перехватывает начало атаки блока.
  3. `AutoToolEngine` анализирует материал блока (камень, земля, дерево, паутина, руда).
  4. Сканирует доступные слоты на наличие кирок/топоров/лопат/ножниц/мечей, учитывая уровень материала и чары Efficiency / Silk Touch.
  5. Проверяет порог прочности `durabilityThreshold`.
  6. Переключает слот.
  7. При отпускании ЛКМ или разрушении блока возвращает первоначальный предмет в руку.
- **MIGRATION RISKS**:
  - **ВЫСОКИЙ РИСК**: Миксины в клиентский менеджер взаимодействия с миром (`ClientPlayerInteractionManager`). Необходимо обеспечить точное совпадение сигнатур Fabric Loom для Minecraft 1.21.11.
- **TARGET NIVORAT CATEGORY**: `UTILITY`
- **TARGET MODULE ID**: `auto_tool`

---

### MODULE: AutoGG
- **SOURCE REPOSITORY**: `SingleMods/AutoGG/source`
- **VERSION**: `1.0.2`
- **ENTRYPOINT**: `ru.elarion.autogg.AutoGGClient`
- **MAIN CLASSES**:
  - `ru.elarion.autogg.AutoGGClient`
  - `ru.elarion.autogg.AutoGGConfig`
  - `ru.elarion.autogg.AutoGGScreen` (радиальное колесо выбора фраз)
  - `ru.elarion.autogg.AutoGGEditorScreen` (текстовый редактор списка фраз)
  - Миксин: `ru.elarion.autogg.mixin.ClientPlayNetworkHandlerMixin`
  - `net.fabricmc.pack.api.*`
- **CONFIG**:
  - Класс: `AutoGGConfig`
  - Файл на диске: `config/autogg.json`
  - Формат: JSON (через `Gson`)
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение модуля.
  2. `sendOnKill` (`boolean`, default: `true`) — отправлять фразу при убийстве оппонента.
  3. `sendOnOwnDeath` (`boolean`, default: `false`) — отправлять фразу при собственной гибели.
  4. `randomOrder` (`boolean`, default: `false`) — выбирать случайную фразу из списка или текущую активную.
  5. `phrases` (`List<String>`, default: `["GG", "GGWP", "EZ"]`) — настраиваемый список текстовых сообщений.
  6. `selected` (`int`, default: `0`) — индекс текущей выбранной фразы.
- **KEYBINDS**:
  - `key.autogg.settings` -> `GLFW.GLFW_KEY_G` (категория: `autogg.controls`)
  - В `KeybindManager`: `"autogg"` -> `GLFW.GLFW_KEY_G`
- **EVENTS**:
  - `AttackEntityCallback.EVENT` (отслеживание последнего атакованного игрока)
  - `ClientTickEvents.START_CLIENT_TICK`
- **MIXINS**:
  - `activity.autogg.mixins.json`:
    - `ClientPlayNetworkHandlerMixin` -> инъекция в `onGameMessage` сетевого обработчика `ClientPlayNetworkHandler` для детекции сообщений смерти в чате.
- **RESOURCES**:
  - `assets/autogg/lang/en_us.json`
  - `assets/autogg/lang/ru_ru.json`
  - `activity.autogg.mixins.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `AutoGGScreen` (красивое интерактивное радиальное колесо выбора фразы по зажатию клавиши) и `AutoGGEditorScreen` (редактор списка).
- **RUNTIME FLOW**:
  1. При нанесении ударов по игрокам модуль запоминает ник последнего таргета.
  2. Миксин `ClientPlayNetworkHandlerMixin` анализирует входящие пакеты сообщений сервера.
  3. При обнаружении шаблона гибели с именем цели или игрока планирует отправку фразы.
  4. С задержкой 100-300 ms отправляет чат-пакет `client.player.networkHandler.sendChatMessage(msg)`.
- **MIGRATION RISKS**:
  - **СРЕДНИЙ РИСК**: Миксин сетевого пакета сообщений. Важно сохранять защиту от рекурсивной отправки и дублирования при серийных киллах.
- **TARGET NIVORAT CATEGORY**: `UTILITY`
- **TARGET MODULE ID**: `auto_gg`

---

### MODULE: CartHUD
- **SOURCE REPOSITORY**: `SingleMods/CartHUD/source`
- **VERSION**: `1.0.0`
- **ENTRYPOINT**: `dev.carthud.CartHudClient`
- **MAIN CLASSES**:
  - `dev.carthud.CartHudClient`
  - `dev.carthud.CartHudConfig`
  - `dev.carthud.CartHudOverlay` (рендеринг HUD индикатора)
  - `dev.carthud.CartHudEditorScreen` (редактор экранных координат)
- **CONFIG**:
  - Класс: `CartHudConfig`
  - Файл на диске: `config/cart_hud.properties`, также синхронизация с `storage_tweaks.properties`
  - Формат: key=value
- **SETTINGS**:
  1. `enabled` (`boolean`, default: `true`) — включение отображения счетчика.
  2. `customX` (`int`, default: `-1`) — позиция по X.
  3. `customY` (`int`, default: `-1`) — позиция по Y.
- **KEYBINDS**:
  - `key.carthud.open_editor` -> `GLFW.GLFW_KEY_H` (категория: `carthud.controls`)
- **EVENTS**:
  - `ClientTickEvents.END_CLIENT_TICK`
  - `ClientCommandRegistrationCallback.EVENT` (`/carthud`)
  - `HudRenderCallback.EVENT` (отрисовка HUD)
- **MIXINS**: Отсутствуют.
- **RESOURCES**:
  - `assets/carthud/lang/en_us.json`
  - `assets/carthud/lang/ru_ru.json`
- **DEPENDENCIES**: `fabricloader` (>=0.16.0), `minecraft` (=1.21.11), `java` (>=21), `fabric-api` (*).
- **CURRENT GUI**: `CartHudEditorScreen` (перетаскивание оверлея мышью с кнопкой центрирования/сброса).
- **RUNTIME FLOW**:
  1. Подсчитывает суммарное количество вагонеток (`Items.TNT_MINECART` и `Items.MINECART`) в хотбаре и основном инвентаре.
  2. В `HudRenderCallback` рисует элегантную плашку с текстурой вагонетки и цифрой запаса.
- **MIGRATION RISKS**:
  - **НИЗКИЙ РИСК**: Прямое дублирование с `CartRefill`. Решение — объединить рендеринг в один канонический модуль `cart_hud`, а логику дозарядки оставить в `cart_refill`.
- **TARGET NIVORAT CATEGORY**: `UTILITY`
- **TARGET MODULE ID**: `cart_hud`

---

## 2. Анализ конфликтов и коллизий (Conflict Analysis)

### 2.1 Коллизии классов (Duplicate Class Names)
Обнаружено **19 идентичных классов**, скопированных сразу в 10 из 12 модов:
- `net.fabricmc.pack.api.CombatLockManager` (10 модов)
- `net.fabricmc.pack.api.CombatRaytraceGuard` (10 модов)
- `net.fabricmc.pack.api.GaussianTimingEngine` (10 модов)
- `net.fabricmc.pack.api.Keybind` (10 модов)
- `net.fabricmc.pack.api.KeybindManager` (10 модов)
- `net.fabricmc.pack.api.MasterScreen` (10 модов)
- `net.fabricmc.pack.api.ModuleRegistry` (10 модов)
- `net.fabricmc.pack.api.PackModule` (10 модов)
- `net.fabricmc.pack.api.SafeSlotManager` (10 модов)
- `net.fabricmc.pack.api.TickBoundScheduler` (10 модов)
- `net.fabricmc.pack.api.setting.BooleanSetting` (10 модов)
- `net.fabricmc.pack.api.setting.ColorSetting` (10 модов)
- `net.fabricmc.pack.api.setting.DoubleSetting` (10 модов)
- `net.fabricmc.pack.api.setting.EnumSetting` (10 модов)
- `net.fabricmc.pack.api.setting.IntSetting` (10 модов)
- `net.fabricmc.pack.api.setting.KeybindSetting` (10 модов)
- `net.fabricmc.pack.api.setting.Setting` (10 модов)
- `net.fabricmc.pack.api.setting.StringSetting` (10 модов)
- `CartHudEditorScreen` и `CartHudOverlay` (2 мода: `dev.storage.*` и `dev.carthud.*`).

### 2.2 Коллизии пакетов (Package Collisions)
- Пакет `net.fabricmc.pack.api` и субпакет `net.fabricmc.pack.api.setting` присутствуют одновременно в:
  `AutoAnchor`, `AutoCart`, `AutoGG`, `AutoMace`, `AutoShieldbreaker`, `AutoSpear`, `AutoStunSlime`, `AutoTool`, `AutoTotem`, `CartRefill`.
- Все модули имеют уникальные собственные пакеты для своей специфической логики (`net.redstone.optimizer`, `dev.momentum`, `dev.nivora`, `dev.sunder`, `dev.autototem`, `dev.virion.arc`, `dev.luminance`, `dev.storage`, `dev.hpreaper`, `ru.elarion.autotool`, `ru.elarion.autogg`, `dev.carthud`).

### 2.3 Коллизии миксинов (Mixin Collisions)
- Имена конфигураций и классов миксинов строго изолированы и префиксированы (`activity.`):
  - `activity.autotool.mixins.json` -> пакет `activity.client.mixin.autotool` (`ActivityClientPlayerInteractionManagerMixin`, `ActivityClientPlayerInteractionManagerAccessor`)
  - `activity.autogg.mixins.json` -> пакет `activity.client.mixin.autogg` (`ActivityClientPlayNetworkHandlerMixin`)
- Благодаря префиксу конфигураций, уникальным пакетам/классам и уникальным префиксам методов инъекции (`activity$autotool$...`, `activity$autogg$...`) полностью исключаются любые конфликты на уровне Fabric Loader и SpongePowered Mixin даже при одновременном наличии автономных JAR модов в папке mods.

### 2.4 Коллизии файлов конфигурации (Config Collisions)
- `redstone_keys.properties` и `pvp_keys.properties` перезаписываются всеми 10 модами `KeybindManager`.
- `storage_tweaks.properties` и `cart_hud.properties` одновременно читаются и записываются и `CartRefill`, и `CartHUD`.

### 2.5 Коллизии горячих клавиш (Keybind Collisions)
В автономных модах присутствовали пересечения дефолтных клавиш:
- `GLFW_KEY_J`: Shieldbreaker (`KeybindManager`) vs HPReaper (`key.hpreaper.open_editor`)
- `GLFW_KEY_H`: CartHUD (`key.carthud.open_editor`) vs HPReaper (`KeybindManager`)
- `GLFW_KEY_G`: AutoGG (`key.autogg.settings`) vs AutoGG (`KeybindManager`)

### 2.6 Точки входа и события (Duplicate Entrypoints & Events)
Каждый отдельный мод в `fabric.mod.json` имел свою независимую точку входа:
- 12 независимых клиентов регистрировали собственные обработчики `ClientTickEvents.START_CLIENT_TICK` и `AttackEntityCallback.EVENT`.
- В монолитном клиенте эти обработчики должны централизованно агрегироваться в `activity.client.ActivityClient` для предотвращения накладных расходов и гонок тасок.

### 2.7 Зависимости (Dependencies)
- Все 12 модов используют стандартный стек: Fabric Loader >=0.16.0 (актуальный 0.19.3), Minecraft 1.21.11, Java 21, Fabric API *.
- Сторонних библиотечных зависимостей нет.

---

## 3. Что можно удалить после интеграции

1. Устаревший пакет `net.fabricmc.pack.api` и `net.fabricmc.pack.api.setting` со всеми дубликатами:
   - `MasterScreen.java`
   - `PackModule.java`
   - Локальные самодельные `Setting.java`
2. Автономные экраны настроек отдельных модов:
   - `RedstoneSettingsScreen.java`
   - `SpearScreen.java`
   - `ShieldBreakerScreen.java`
   - `SunderScreen.java`
   - `AutoTotemScreen.java`
   - `MorrowScreen.java`
   - `AnchorScreen.java`
   - `RefillScreen.java`
   - `AutoToolScreen.java`
   - `AutoGGEditorScreen.java`
   - `dev.storage.CartHudEditorScreen.java` (дубликат)
3. Избыточные точки входа `*Client.java` (их логика переходит в `IModule.onClientTick`, `IModule.onAttackEntity`).

---

## 4. Что нельзя менять из-за риска поломки runtime logic

1. **Боевые машины состояний и задержки**:
   - `ShieldAxeController`: состояние `IDLE -> SWAPPING -> AWAITING_HIT -> RESTORING` и кулдауны.
   - `SunderController`: фазы комбо `SWAPPING_AXE -> STRIKING_AXE -> SWAPPING_MACE -> STRIKING_MACE`.
   - `SpearSwapController`: задержки `securityMode` и проверка Lunge уровней.
   - `RedstoneOptimizerConfig`: выбор зачарований булавы `Breach`/`Density` по типу защиты цели.
2. **Инвентарные алгоритмы**:
   - `AutoTotemController`: логика поиска тотемов и пакеты свапа в слот 45 (offhand).
   - `AutoToolEngine`: алгоритм расчета скорости ломания блоков и проверка прочности `durabilityThreshold`.
   - `VirionArcController`: баллистический расчет траектории и проверка твердости опоры под рельсы.
   - `AnchorController`: цикл зарядки светокамнем и проверка безопасного предмета детонации.
3. **Миксины**:
   - `ClientPlayerInteractionManagerMixin` и `ClientPlayNetworkHandlerMixin` — точки инъекции должны оставаться неизменными для точности перехвата.
4. **Алиасы**:
   - Сохранение распознавания старого имени `auto_stun_slime` наряду с новым `auto_stun_slam`.

---

## 5. Итоговые показатели аудита

```
MODULES_FOUND: 12
SETTINGS_TOTAL: 68
MIXINS_FOUND: 3
DEPENDENCY_CONFLICTS: 0
CONFIG_CONFLICTS: 3
HIGH_RISK_MODULES: 5
RECOMMENDED_MIGRATION_ORDER:
  1. HPReaper (HUD/Utility, безопасный старт, визуальный слой)
  2. CartHUD (HUD/Utility, визуальный индикатор)
  3. AutoGG (Utility, чат-миксин)
  4. AutoTool (Utility, миксины взаимодействия с блоками)
  5. CartRefill (Defense, связка с CartHUD)
  6. AutoAnchor (Defense, взаимодействие с блоками якоря)
  7. AutoCart (Defense, сложная баллистика и постановка)
  8. AutoTotem (Defense, критическая инвентарная логика)
  9. AutoMace (Combat, свап булавы)
  10. AutoSpear (Combat, механика копья Lunge)
  11. AutoShieldbreaker (Combat, машина состояний топора)
  12. AutoStunSlam (Combat, наивысший приоритет и сложность двухфазного комбо)
```
