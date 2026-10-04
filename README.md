# MemoryLeakFix 1.0.4

Клиентский мод для Minecraft **1.21.11**, Fabric, Java **21**.

Оптимизирует использование оперативной памяти, текстурных буферов и устраняет утечки ресурсов. Включает оптимизированный рендеринг GUI, звуковой движок и вспомогательные утилиты.

## Сборка

```powershell
git clone https://github.com/kirillsmir99-web/NivoratClient-Public.git
cd NivoratClient-Public
./gradlew.bat remapJar --no-daemon
```

Артефакт: `build/libs/MemoryLeakFix.jar`. Требуется Fabric API для Minecraft 1.21.11. Внутри JAR находится неизменённый ClientSpoofer 1.4.0 с MIT-лицензией.

При замене установленного клиента сохраните старый JAR и конфигурацию, затем оставьте одну версию клиента в `mods`. В каталог `mods` устанавливается итоговый remapped JAR.

## Проверки

[PUBLIC 1.0.3: цикл PEARL и редакторы HUD](docs/PUBLIC_1.0.3.md), [PUBLIC 1.0.2: бинды, PEARL, HP и списки](docs/PUBLIC_1.0.2.md), [разбор механик](docs/MECHANICS_AUDIT.md), [оптимизация](docs/OPTIMIZATION_REPORT.md), [перенос визуала](docs/VISUAL_PORT.md), [состав перенесённых исходников](docs/custom-render-sources.json).

JUnit проверяет конфигурации, переходы, расчёты, локализацию, GUI и обучение. Изолированный игровой сценарий проверяет PUBLIC JAR с копией набора модов: перл и вагонетка должны реально появиться на встроенном сервере, а слот и блокировки — восстановиться. Тестовый helper не включается в продукт.

```powershell
./gradlew.bat -I tools/public-visual-smoke.init.gradle publicVisualSmoke -PmechanicsSmoke --no-daemon
./gradlew.bat -I tools/public-visual-smoke.init.gradle publicVisualSmoke --no-daemon
```

Это ограниченные воспроизводимые проверки. Совместимость всех режимов с каждым сторонним сервером, универсальный FPS на слабых компьютерах и отсутствие античит-отклонений ими не подтверждаются.
