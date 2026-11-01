# Kayn Combat Diagnostics 1.5.0

A **standalone combat diagnostics mod for Starsector 0.98a-RC8**. No other mods
or libraries required. Observe your ship or a selected ship, check weapon activity,
and save session reports. It does not change ship behaviour or weapon AI.

## Install

1. Download the **KaynCombatDiagnostics-1.5.0.zip** mod package from [Releases](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics/releases/latest).
2. Extract the `KaynCombatDiagnostics` folder into your `Starsector/mods` folder.
3. Check that `Starsector/mods/KaynCombatDiagnostics/mod_info.json` exists.
4. Enable **Kayn Combat Diagnostics** in the Starsector launcher and start the game.
5. Enter combat and press **Ctrl+F8** to begin.

Use the mod ZIP; GitHub's **Source code** downloads require a local build.

## Shortcuts

| Shortcut | Action |
| --- | --- |
| **Ctrl+F8** | Start a fresh measurement or stop and archive the current one. |
| **Ctrl+F9** | Pin your selected friendly/enemy ship; press again to follow your controlled ship. With no target, stay on your ship. |
| **Ctrl+F10** | Next weapon page (six weapons per page). |
| **Ctrl+F7** | Show or hide help while diagnostics are enabled. |
| **Ctrl+Up / Ctrl+Down** | Scroll the panel. |

Diagnostics start off each battle. Rebind shortcuts in the mod's
`data/config/settings.json`, then restart the game.

## Features

### Weapon activity

Shows firing activity, projectile and impact counts, plus beam firing time and hit frames. Counts cover the current measurement, not trigger pulls.

![Weapon activity](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-projectiles-impacts.gif)

### Shot directions

Keeps the last five observed projectile directions per weapon, fading from green to black. Lines stay at their original world position.

![Shot directions](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-shot-vectors.gif)

### Ship status and session timer

Displays hull, current/max flux, weapon count, recording status and elapsed simulation time.

![Ship status and session timer](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-hull-flux-timer.gif)

### Follow different ships

Automatically follows your controlled ship. Switching ships archives the previous measurement and resets counters.

![Follow different ships](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-subject-switching.gif)

### Pages, help and pause

Browse weapon pages, open help and scroll long readouts. Pausing freezes collection and the timer; the panel marks cached evidence as PAUSED.

![Pages, help and pause](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-pages-help-pause.gif)

### Pin a selected ship

Use Ctrl+F9 to keep observing a selected friendly or enemy ship, then return to your controlled ship. This original clip includes one blank capture frame.

![Pin a selected ship](https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/live-target-pin.gif)

### Coloured weapon indicators

Shows simultaneous observations such as obstruction, range, cooldown, ammo and flux.
These describe observed conditions; they do not reveal the AI's private reasons or
guarantee that a weapon will fire or hit.

<details>
<summary>Show indicator GIFs and additional display examples</summary>
<p>These 18 clips use artificial example text to demonstrate the real panel's appearance.</p>

<p><strong>Blocked</strong> — An intervening ship, shield or asteroid intersects the estimated firing line.<br>
<img alt="Display example: Blocked" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-blocked.gif"></p>

<p><strong>Distance</strong> — The observed target is outside the estimated weapon range.<br>
<img alt="Display example: Distance" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-distance.gif"></p>

<p><strong>Fluxing</strong> — The ship is venting or lacks reported flux headroom.<br>
<img alt="Display example: Fluxing" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-fluxing.gif"></p>

<p><strong>No target</strong> — No weapon target is known to the observer.<br>
<img alt="Display example: No target" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-notarget.gif"></p>

<p><strong>Damaged</strong> — The weapon reports being disabled.<br>
<img alt="Display example: Damaged" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-damaged.gif"></p>

<p><strong>Cooldown</strong> — The weapon reports a remaining cooldown.<br>
<img alt="Display example: Cooldown" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-cooldown.gif"></p>

<p><strong>No ammo</strong> — The weapon has no ammunition remaining.<br>
<img alt="Display example: No ammo" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-noammo.gif"></p>

<p><strong>Hold</strong> — The weapon reports a forced no-fire flag.<br>
<img alt="Display example: Hold" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-hold.gif"></p>

<p><strong>Arc</strong> — The observed aim point is outside the weapon arc.<br>
<img alt="Display example: Arc" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-arc.gif"></p>

<p><strong>Overload</strong> — The ship is overloaded.<br>
<img alt="Display example: Overload" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-overload.gif"></p>

<p><strong>Unknown</strong> — Required evidence is missing or invalid.<br>
<img alt="Display example: Unknown" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-unknown.gif"></p>

<p><strong>AI hold</strong> — The individual autofire request is false.<br>
<img alt="Display example: AI hold" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-aihold.gif"></p>

<p><strong>Beam counters</strong> — Shows beam firing time and frames with reported damage.<br>
<img alt="Display example: Beam counters" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-beam.gif"></p>

<p><strong>Projectile counters</strong> — Shows observed projectiles and impacts.<br>
<img alt="Display example: Projectile counters" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-projectiles.gif"></p>

<p><strong>Multiple restrictions</strong> — Displays several conditions together, even alongside firing activity.<br>
<img alt="Display example: Multiple restrictions" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-multiple.gif"></p>

<p><strong>Evidence freshness</strong> — Distinguishes live, paused, final and failed observations.<br>
<img alt="Display example: Evidence freshness" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-freshness.gif"></p>

<p><strong>Recording status</strong> — Shows recording status and file-write warnings.<br>
<img alt="Display example: Recording status" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-recording.gif"></p>

<p><strong>Help</strong> — Displays controls and brief recording explanations.<br>
<img alt="Display example: Help" src="https://raw.githubusercontent.com/MysticEntry-Nova64/KaynCombatDiagnostics/main/docs/media/display-help.gif"></p>

</details>

## Saved session data

Reports are saved automatically in your game installation's **`Starsector/saves/common/`** folder:

| File | Contents |
| --- | --- |
| `kayn_combat_diagnostics_live.tsv.data` | Latest snapshot, updated about once per simulation second while recording. |
| `kayn_combat_diagnostics_<session UUID>.tsv.data` | Archived measurement, saved when you stop, switch ships, lose the observed ship or combat ends. |

Reports include weapon activity, counters, target evidence and observed restrictions
for **all weapons**, regardless of the displayed page. Open these tab-separated text
files in a text editor or spreadsheet application. They do not change campaign saves.
Stop diagnostics with **Ctrl+F8** before leaving combat to request a final archive.

[Detailed usage and limitations](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics/blob/main/docs/reference.md) ·
[Build instructions](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics/blob/main/BUILD.md) ·
[Verification](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics/blob/main/TEST-RESULTS.md) ·
[MIT license](https://github.com/MysticEntry-Nova64/KaynCombatDiagnostics/blob/main/LICENSE)
