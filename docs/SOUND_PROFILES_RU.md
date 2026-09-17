# Звуковые профили NivoratClient

В архиве теперь есть 3 режима:

1. `Minecraft`
   - текущие vanilla UI sounds;
   - никаких файлов в этом пакете не требуется.

2. `Nivorat Classic`
   - папка: `src/main/resources/assets/nivoratclient/sounds/ui/`
   - это исходный пакет из предыдущей версии.

3. `Nivorat Serene`
   - папка: `src/main/resources/assets/nivoratclient/sounds/ui_serene/`
   - новый, более тихий и расслабляющий набор.

## Nivorat Serene содержит

- open
- close
- button_primary
- button_secondary
- hover
- toggle_on
- toggle_off
- dropdown_open
- dropdown_close
- category_expand
- category_collapse
- modal_open
- modal_close
- success
- warning
- error
- slider_tick
- pin
- unpin
- copy
- import
- delete
- link_open
- reload
- maximize
- restore
- search_focus

## Обязательное правило slider

`slider_tick` запускается только при смене дискретного шага значения.
Например 50 → 55 = один tick.
Если курсор движется, но quantized value не изменился — звук не играть.
Дополнительно использовать rate-limit.

## Настройки

Пользователь должен видеть:

Звуковой профиль:
- Nivorat Serene
- Nivorat Classic
- Minecraft

Звуки интерфейса:
- Вкл / Выкл

Громкость:
- 0–100 %

Рекомендуемый default:
`Nivorat Serene`, 55–65%.
