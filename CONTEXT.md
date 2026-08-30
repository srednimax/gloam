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
| Warmth | How far the shade is tinted amber, cutting blue. Independent of dim level. | Not "night mode" - that name is already the app's own dark theme |
| Running | The shade is on screen and the service is in the foreground. A live state. | Not "enabled", which reads as a settings toggle |

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
- "Overlay" on its own is avoided now that there are two of them. The shade passes every touch
  through; the panel catches them. A name that covers both hides the one property that keeps the
  user from being trapped, so say **shade** or **panel** and never "the overlay window".
- "Screen dimmer" is fine in the listing and the README, where it is what people search for. It is
  not a code word; nothing is named `Dimmer`.

## Naming conventions in code

Some names in this codebase are deliberately generic — `AppDatabase`, `AppTheme`, `AppPreferences`,
`AppContainer`, `MainApplication`, `MainActivity`. They stay correct whatever the app is called, so
renaming the app is a package move rather than a repo-wide find-and-replace. Don't rename them to
match the product; name the *domain* types after the domain instead.
