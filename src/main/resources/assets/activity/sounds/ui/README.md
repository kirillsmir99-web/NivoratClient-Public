# Activity UI Custom Sound Assets Directory

This directory (`assets/activity/sounds/ui/`) is designated for optional custom OGG audio files.
When custom sounds are placed here, `SoundManager` can play these dedicated audio clips instead of procedural vanilla sound events.

## Audio Specifications
- **Format**: Ogg Vorbis (`.ogg`)
- **Sample Rate**: 44,100 Hz or 48,000 Hz
- **Channels**: Mono (recommended for GUI) or Stereo
- **Bit Depth**: 16-bit
- **Recommended Length**:
  - Micro-ticks (hover, slider): 20ms – 50ms
  - Clicks & toggles: 50ms – 120ms
  - Accordion & dropdowns: 100ms – 200ms
  - Chimes & Presets: 150ms – 300ms
  - Window open/close: 180ms – 350ms

## File Mapping
| Sound Event Identifier | File Name | Description |
|------------------------|-----------|-------------|
| `activity:ui.menu_open` | `menu_open.ogg` | Soft ascending opening chime/whoosh |
| `activity:ui.menu_close` | `menu_close.ogg` | Soft descending tone |
| `activity:ui.hover` | `hover.ogg` | Very subtle haptic micro-tick |
| `activity:ui.select` | `select.ogg` | Crisp selection click |
| `activity:ui.click` | `click.ogg` | Standard UI button click |
| `activity:ui.toggle_on` | `toggle_on.ogg` | High pitch switch click |
| `activity:ui.toggle_off` | `toggle_off.ogg` | Lower muted switch click |
| `activity:ui.dropdown_open` | `dropdown_open.ogg` | Soft expansion pop |
| `activity:ui.dropdown_close` | `dropdown_close.ogg` | Soft collapse |
| `activity:ui.category_expand` | `category_expand.ogg` | Accordion expand page turn |
| `activity:ui.category_collapse` | `category_collapse.ogg` | Accordion collapse page turn |
| `activity:ui.preset_save` | `preset_save.ogg` | Positive ascending confirmation chime |
| `activity:ui.preset_reset` | `preset_reset.ogg` | Low warning caution chord |
| `activity:ui.preset_apply` | `preset_apply.ogg` | Affirmative activation tone |
| `activity:ui.slider_tick` | `slider_tick.ogg` | Digital ratchet escapement tick |

## Fallback Mechanism
If custom `.ogg` files are not placed in this directory, `SoundManager` automatically falls back to procedurally tuned vanilla Minecraft `SoundEvents` (e.g. `BLOCK_NOTE_BLOCK_PLING`, `BLOCK_NOTE_BLOCK_BASS`, `ITEM_BOOK_PAGE_TURN`, and `UI_BUTTON_CLICK` with tuned pitch and volume), guaranteeing zero missing asset warnings and complete functional tactile audio out of the box.
