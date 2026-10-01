# Native Kimiko visual port

The active screen is `activity.client.gui.custom.api.ui.UI.INSTANCE`, built from the original Kimiko UI source. The earlier replacement `KimikoScreen` and `KimikoInspector` have been removed. No legacy ActivityScreen pages are composed into the native screen.

## Scope

Original Render2D, glass/refraction, post-processing capture/composition, MSDF text/icons, search animation, movable/resizable sidebar, module cards, setting widgets, native inspector, language button, settings popup, opening/closing animations, full theme gallery/editor, local sounds, and native notifications are ported. Chat text-field and message animations use the donor implementation with Minecraft 1.21.11 named mappings.

The module adapter wraps the target ModuleRegistry. It preserves current gameplay modules and setting callbacks. VisualMaterial, ClickGui and ClientSounds are cosmetic settings shown under Settings. The second donor supplies the red NC logo, red default theme, and All/kit navigation. Configs is absent from navigation; local gameplay configuration persistence is retained. IRC authentication, heartbeat, peers, login screen and peer name rendering are removed. No donor gameplay modules or remote GUI sharing are loaded.

Visual settings: config/nc-visual.json. Custom themes: nc-visual/themes under the game directory. Current module configuration remains owned by ActivityConfigManager.

## Structure

- api/ui: original native screen, cards, inspector, popup, sidebar, theme editor.
- api/modules: adapters for the current registry and visual settings.
- api/localization: original visual translations and language switch.
- utils/render: original GPU renderer, fonts and post-processing.
- utils/sounds: original local sound resources and controls.
- mixin: Minecraft rendering and chat integration.
- assets/kimiko: original shaders, fonts, sounds and visual resources.

## Verification

713 JUnit tests passed, zero failures/errors. Isolated Minecraft 1.21.11 creative-world smoke completed: All, Mace, current AutoMace inspector, themes, Nivora palette, settings, full theme editor, About, ClickGui, search, collapsed sidebar, chat and closing animation. Captures: build/visual-smoke/captures/screenshots. Logs: build/native-visual-world-smoke.log and build/native-visual-build.log. Notification regression: five identical messages produce one bounded, wrapped card above the hotbar; notifications are hidden while native ClickGUI is open. Only the public edition is delivered. Visual-setting persistence is debounced and uses an atomic file replacement. This does not establish FPS measurements or exhaustive compatibility with third-party modpacks.

The donor trees were read only in this work. The second donor matches docs/visual-donor-hashes.json. Kimiko has concurrently changed since that historical snapshot; its observed current state is recorded separately in docs/visual-donor-observation-20261001.json. docs/custom-render-sources.json records source and target hashes; adaptation includes namespace changes, current-module bridges, and removal of donor network/gameplay dependencies.

## Installed artifact

Public PulseHUD.jar installed as VortexHUD.jar in the authorized ElyPrism instance 1.21.11 TEST V2 NC. Current SHA-256: FC1933C8475EB6870B06BC7CEB1CA80239026B1EAFED2CE2761D2D4FBA0E96E7. Previous JAR: backup/launcher-visual-polish-20261001/VortexHUD-before-polish.jar. PUBLIC only. The built JAR completed isolated game smoke; the full third-party modpack test stopped in Fabric runtime remapping before Minecraft loaded. Restart the user instance to load the update.

## 2026-10-01 visual polish

See [Visual polish analysis](VISUAL_POLISH_ANALYSIS.md) for live localization, search caching, tooltips, updated kit icons, the GG glass-sector fixes and the Kimiko list-based Cooldown HUD. The current PUBLIC artifact was loaded from its remapped archive in the isolated creative-world smoke (20 views); source classes were excluded from the launch classpath.
