# Vocabulary

The words this app uses, and the ones it does not.

Gloam has one unusually sharp naming problem for an app of its size: two completely different
mechanisms both look like "brightness" to a user, and only one of them is ours. Android's backlight
has a floor; Gloam's whole reason to exist is the range below it, which it reaches by drawing a dark
overlay rather than by turning the backlight down further. A word that blurs the two produces code
where nobody can tell which mechanism a value refers to, so the distinction is enforced here.

**The rule this file exists to enforce: one concept, one word, everywhere.** The entity, the DAO, the
route key, the string resource and the sentence in the UI all use it. When the domain word and the
user-facing word genuinely differ, both are written here with the difference stated.

## The domain

| Term | Means | Not |
| --- | --- | --- |
| Dim level | The single number the user moves, 0-100. The one value the product is about. | Not "brightness" - it runs the opposite way, and brightness is the system's word |
| Shade | The **untouchable** overlay window Gloam draws above every other app. The mechanism that reaches below the floor. | Not "filter", "mask" or "layer" - and not the panel |
| Panel | The **touchable** overlay window carrying the controls, sized to its own content. A second window with the opposite safety rule. | Not "the overlay" - there are two |
| Floor | The dimmest the Android backlight goes on this device. What Gloam exists to get past. | Not "minimum brightness" in code - too easily read as our own minimum |
| Backlight | The system screen brightness. Android's value, which Gloam lowers to the floor as the first half of the dim level's ramp before the shade does the rest. | Never our own value |
| Backlight top | The backlight Gloam takes over from. Captured the moment the override is applied and held until it is released. **Not a device constant** - it is where the user left their own brightness, so it decides how much of the slider the backlight half is worth. | Not "maximum brightness" - it is the user's starting point, not the panel's ceiling |
| Warmth | How far the shade is tinted amber. It **adds** amber over an already-dark screen, shifting the average colour. A separate control from the dim level, but not an unbounded one: the two together decide whether anything underneath stays legible, so both are capped. | Not "night mode" - that name is already the app's own dark theme. Not "cutting blue" - an overlay composites source-over and can only add light, never subtract a channel from what is underneath |
| Running | The shade is on screen and the service is in the foreground. A live state. | Not "enabled", which reads as a settings toggle |
| Deadline | The one instant at which the shade next comes down, whoever set it. There is only ever one. | Not "auto-off" - that is one of the things that sets it. Not "timer" |
| Auto-off | The duration a hand-started shade lasts before it comes down on its own. One-shot - it never repeats - and choosing a duration re-sets the deadline from that moment, whether the shade is already up or not. | Not "timer" on its own - the schedule sets deadlines too |
| Schedule | The nightly window: on at one time, off at another. One pair, not two independent switches. | Not "alarm" - nothing rings, and Gloam claims no exact-alarm permission |
| Escape hatch | A surface that stops the shade and can be reached without seeing Gloam's own UI, which the shade may be covering. Not every control that stops the shade is one. | Not the app's own *Stop dimming* button, which sits under the shade at high dim levels |

## Words we deliberately avoid

- "Brightness" never refers to a Gloam value. It is Android's word for the backlight, the user
  already has a system slider called that, and reusing it makes every function signature ambiguous.
  Ours is the dim level, and it counts the other way: more dim level is less light.
- "Night mode" is the app's own light/dark theme and nothing else. `ThemeMode` and `NightMode.kt`
  already own it. The amber tint over other apps is warmth. These are separate settings that a user
  can set in opposite directions, so they cannot share a name.
- "Filter" is avoided entirely. Blue-light filter apps have made it mean warmth to some people and
  dimming to others, and Gloam does both as separate controls.
- "Off" is reserved for the shade not being drawn at all. A dim level of zero is still running - the
  service is alive, the shade is transparent - and the two states behave differently on a reboot.
  It follows that **auto-off cannot itself be switched "off"**: the choice that disables it is
  **Never**, because "auto-off: off" is a sentence meaning the shade stays on.
- "Overlay" on its own is avoided now that there are two of them. The shade passes every touch
  through; the panel catches them. A name that covers both hides the one property that keeps the
  user from being trapped, so say **shade** or **panel** and never "the overlay window".
- "Screen dimmer" is fine in the listing and the README, where it is what people search for. It is
  not a code word; nothing is named `Dimmer`.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppTheme`, `AppPreferences`, `AppContainer`,
`MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
