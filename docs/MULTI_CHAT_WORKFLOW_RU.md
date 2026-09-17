# Параллельная работа в нескольких чатах Antigravity

Да, проект можно ускорять несколькими чатами, НО качество сохраняется только при разделении владения файлами.

## Рекомендуемая схема: 3 чата

### Чат A — MAIN INTEGRATOR
Владеет:
- главный Screen;
- навигация / sidebar / tabs;
- config;
- presets;
- dialogs;
- module integration;
- финальные merge/build.

НЕ отдавать эти файлы другим агентам на одновременную запись.

### Чат B — ASSETS & UI SYSTEMS
Владеет:
- `src/main/resources/assets/nivoratclient/**`
- sounds
- sounds.json
- icons
- icon atlas
- font JSON/resources definitions
- helper-классы только для sound/icon/font systems, если MAIN заранее выделил package ownership.

Не меняет главный Screen/Config/Module classes.

### Чат C — QA & PERFORMANCE
По умолчанию READ-ONLY.
Проверяет:
- build;
- GUI Scale 2/3/4;
- localization;
- animation consistency;
- allocations;
- text measurement churn;
- sound spam;
- click-through;
- regressions.

Он НЕ должен редактировать core-файлы.
Вместо этого пишет patch recommendations для MAIN.

## Если все чаты видят одну и ту же папку

Не разрешайте двум чатам одновременно писать один файл.

Лучший вариант — Git branches / worktrees:

- `main-ui`
- `assets-ui`
- `qa-review`

MAIN интегрирует изменения после завершения этапа.

Если worktrees не используются:
строго соблюдайте ownership выше.

## Handoff protocol

После этапа каждый чат создаёт отчёт:
- FILES_CHANGED
- FILES_ADDED
- API_CHANGED
- CONFIG_CHANGED
- ASSET_IDS_ADDED
- KNOWN_RISKS
- BUILD_STATUS

MAIN читает отчёт перед merge.
