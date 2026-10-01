# PulseHUD 1.0.1

Клиентский мод для Minecraft **1.21.11**, Fabric, Java **21**. Выпускается PUBLIC JAR; IRC, авторизация разработчика и сервер присутствия удалены.

Нативный визуальный слой перенесён из Kimiko: стеклянный рендер, темы и редактор, анимации, настройки ClickGUI, поиск, локализация RU/EN, иконки и звуки. Разделение по китам, красная палитра и логотип взяты из второго визуального проекта. Игровые модули принадлежат текущему клиенту.

## Модули

* Бой: AutoMace, AutoSpear, AutoShieldbreaker, AutoStunSlam, ClickPearl, AutoPearlCatch.
* Защита и взаимодействия: AutoTotem, AutoCart, AutoAnchor, CartRefill, WaterDrop.
* Утилиты и интерфейс: AutoTool, AutoGG, HPReaper, Cart HUD, Cooldown HUD и настройки клиента.

AutoCart использует баллистический расчёт места установки и обучаемый профиль моторики камеры/задержек. Профиль обучается на ручных действиях; пустая калибровка не считается успешной.

## Сборка

```powershell
git clone https://github.com/kirillsmir99-web/dorabotka-v2-versii.git
cd dorabotka-v2-versii
./gradlew.bat test remapJar --no-daemon
```

Артефакт: `build/libs/PulseHUD.jar`. Требуется Fabric API для Minecraft 1.21.11. Внутри JAR находится неизменённый ClientSpoofer 1.4.0 с MIT-лицензией и проверкой SHA-256; отдельная его копия не нужна.

При замене установленного клиента сохраните старый JAR и конфигурацию, затем оставьте одну версию клиента в `mods`. В каталог `mods` устанавливается итоговый remapped JAR.

## Проверки

[Разбор механик](docs/MECHANICS_AUDIT.md), [оптимизация](docs/OPTIMIZATION_REPORT.md), [перенос визуала](docs/VISUAL_PORT.md), [состав перенесённых исходников](docs/custom-render-sources.json).

JUnit проверяет конфигурации, переходы, расчёты, локализацию, GUI и обучение. Изолированный игровой сценарий проверяет PUBLIC JAR с копией набора модов: перл и вагонетка должны реально появиться на встроенном сервере, а слот и блокировки — восстановиться. Тестовый helper не включается в продукт.

```powershell
./gradlew.bat -I tools/public-visual-smoke.init.gradle publicVisualSmoke -PmechanicsSmoke --no-daemon
./gradlew.bat -I tools/public-visual-smoke.init.gradle publicVisualSmoke --no-daemon
```

Это ограниченные воспроизводимые проверки. Совместимость всех режимов с каждым сторонним сервером, универсальный FPS на слабых компьютерах и отсутствие античит-отклонений ими не подтверждаются.
