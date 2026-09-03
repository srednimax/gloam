# Phase 3 — The controls, and the panel

**Two phases, one file.** `PLAN.md` splits Phase 3 along cost — 3a is a theme and an activity, 3b is
a hand-built Compose host in a raw window with a documented kill condition — and that split is right
and stays. What is wrong is planning them apart: **they are the same composable in two hosts**, 3b's
go/no-go decides what three of 3a's own decisions are worth, and the one fact that decides how much
either host is worth is a fact about both. Splitting the file would mean writing that fact twice and
then keeping two copies of it honest.

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md); the decision
this phase closes out is
[ADR-0010](adr/0010-one-dim-level-drives-both-mechanisms-in-a-fixed-order.md) — its *Consequences*
section left one question open for Phase 3 and its third amendment narrowed it. The vocabulary this
phase needs is in [`CONTEXT.md`](../CONTEXT.md), which already names **shade** and **panel**
separately and is about to need a third word.

**What closing this phase means.** Today Gloam has one control surface — a full-screen app — and one
way to stop the shade from outside it. This phase asks whether the controls can be reached *while
the shade is up*, which is the only moment anybody wants them. 3a makes them cheaper to reach. 3b
decides whether they can be reached at all past a certain dim level, and 3b is the half that can be
cut by the platform rather than by us.

**Phase 2b is not in this file and this phase leans on it in one place.** The Quick Settings tile is
one of the two doors `PLAN.md` gives the compact controls, and 2b owns it. Nothing here blocks on it:
the tile's click action is one line pointing at the same activity, and §3 leaves the hole shaped so
that 2b fills it without reopening anything. If 2b has not shipped when this phase starts, 3a ships
with one door instead of two and says so.

---

## 0. What this phase inherits, and the one thing it cannot fix

**Everything 3a builds sits under the shade it controls.** That is the spine of this file and it is a
platform fact rather than a design choice, so it is worth being exact about where it comes from.

The shade is a `TYPE_APPLICATION_OVERLAY` window. Every Activity in Android is below that type, ours
included. Phase 1 measured the consequence from three directions and got the same answer each time:

- **Phase 1's R8.** At maximum dim, `MainActivity` — with its *Stop dimming* button on it — is under
  the shade at `MIN_BACKLIGHT` × `(1 − MAX_SHADE_ALPHA)` = **0.33 nits**, while SystemUI's
  notification row above the shade stays at the full 6.64.
- **Phase 1's R9.** An ordinary app window that sets its own `screenBrightness` is *not consulted*
  while ours is up. MIUI's video player asked for `1.0` and then for `0.0078`; neither reached the
  panel. **It is the topmost window's value that applies, not the lowest.**
- **Phase 1's R5.** The system's own surfaces — notification shade, quick settings, volume dialog —
  read our `0.01` back unchanged. They are legible because the *shade's alpha* is not over them, not
  because they out-bid the override.

Put together: **a floating Activity cannot make itself legible under the shade.** It cannot raise the
backlight, because it is below the topmost window that asked. It cannot escape the alpha, because the
alpha is a window above it. There is no flag, no theme and no `LayoutParams` field that changes
either. So the compact controls are worth building for *reach* — one tap instead of four, no task
switch, a small dark dialog instead of a full screen of app — and they are worth nothing at all for
*legibility*. Anyone who reads 3a as "now you can adjust the dim from anywhere" has read it wrong.

**That is also the strongest argument for 3b that exists, and it is not the one `PLAN.md` gives.**
`PLAN.md` justifies the panel on the live preview — the slider moving the dim over real content
rather than over Gloam's own screen — which is true and is a good reason. The better one is that the
panel is a second `TYPE_APPLICATION_OVERLAY` window, so it is drawn *above* the shade rather than
under it, at the same 6.64 nits the notification row gets. **The panel is the only control surface in
this plan that stays readable at maximum dim.** If 3b is cut, Gloam ships an app you can stop from
anywhere and adjust from nowhere.

**And that puts a real tension between 2b and 3a on the record.** Ultra dark pushes past
`MAX_SHADE_ALPHA`, which pushes the compact controls further under. The darker 2b lets the shade go,
the less any Activity-hosted control is worth, and only the panel gets that back. 2b does not have to
wait for 3b — its own gate is the escape hatch, which is about *stopping* — but the two decisions are
coupled and this paragraph is where that is written down.

**Amended by checkpoint C's readings, 2026-09-02. Every argument above survives; two of its numbers
do not.** R3 measured the shade's transmission at dim 100 rather than deriving it, and it comes back
**0.2393**, not the `1 − MAX_SHADE_ALPHA` = 0.05 this section computes from. The cause is R1's
`alpha=0.8`, now quantified: `1 − 0.8 × MAX_SHADE_ALPHA` = 0.24, which is the measurement to three
decimal places. So on this ROM:

- **The compact host under the shade is at ≈1.59 nits, not 0.33** — 6.64 × 0.2393, where the first
  bullet above expects 6.64 × 0.05. Phase 1's R8 read 0.33 by arithmetic and this reads the composite.
- **D1's 20× becomes ≈4.2×** — `1 / (1 − 0.8 × MAX_SHADE_ALPHA)`. The panel is still the only surface
  that stays at the full 6.64, so 3b's case holds; it is four times better than what is under the
  shade rather than twenty, and §12's D1 is arithmetic that R6 must now re-derive rather than confirm.

**Which way this cuts is worth saying plainly.** ADR-0010's two bounds are *over*-satisfied — the
screen is lighter than computed, so nothing about the escape hatch's visibility is at risk. What is
at risk is the thing the app is for: at maximum dim Gloam is letting through nearly five times the
light it believes it is. That is ADR-0010's business and R6's reading, not this checkpoint's, and it
is recorded here because §0 is the section whose numbers it changes.

---

## What is in, and what is deliberately not

**In:** `DimControls()` and `AutoOffControls()`, extracted now that there is a second caller; the
compact controls as a floating host, with the three candidate shapes costed and one of them chosen on
a reading; the routes into that host — the ongoing notification's content intent, the launcher behind
a preference, and the hole 2b's tile fills; the `SCREEN_BRIGHTNESS` `ContentObserver` question, put
to the twelve in its strongest form rather than its easiest one (rule 5); 3b's go/no-go taken as a
measurement **before any of this phase's code exists**; and, if it goes, the panel — a touchable
overlay window with the opposite safety rule from the shade, and the dismissal that keeps it from
being a trap.

**Not in, and the phase that owns each:** ultra dark and the Quick Settings tile (2b — see §3), the
schedule and the battery-optimisation ask (4), the mark, the tip and the rate-on-Play link (5). Also
not in, and owned by nobody because they are `PLAN.md`'s *Not in this plan* list: an always-visible
floating handle, per-app rules, a widget, a Tasker intent API.

**The tempting one is the panel's scope.** A touchable Compose window above every app is, once it
exists, the natural home for anything — the auto-off chips, the schedule, Settings itself. It gets
`DimControls()` and nothing else. Every widget added to it is a widget that has to stay legible at
6.64 nits, has to fit a window that must never be `MATCH_PARENT` (§6), and has to be dismissible by
somebody who cannot see the rest of the screen.

**One thing that looks like polish and is not.** The compact host must forward to the full app when
the overlay permission is missing or the escape-hatch warning is live. A first run cannot happen in a
dialog with two sliders on it: the explainer, the warning banner and both permission hand-offs live
in `DimScreen` and are the reason a stranger gets through the first two minutes. §2 makes that a
guard rather than a note.

---

## Checkpoints

**The phase is two phases and seven merges.** Each leaves the app working, ships its copy complete in
both locales — `scripts/translation-gate.py` is a merge gate, so that is enforced rather than
remembered — and is a point at which the phase could stop without stranding anything.

| | Checkpoint | Phase | Merges | Depends on |
| --- | --- | --- | --- | --- |
| **A** | **The go/no-go** | 3b | `chore:` debug button, then no commit at all | phone attached |
| **B** | `DimControls()` and `AutoOffControls()`, extracted | 3a | `refactor:` | nothing |
| **C** | The compact host, and the reading that picks its shape | 3a | `feat:`, then the readings | B |
| **D** | The routes in: notification, launcher preference, the tile's hole | 3a | `feat:` | C, and **A's verdict** |
| **E** | The brightness-slider question | 3a | `feat:` (the notification line) — the observer only if the twelve say so | C, and the twelve |
| **F** | The panel: window, host, controls, dismissal | 3b | `chore:` + `test:`, then `feat:` | **A's verdict** |
| **G** | The documents | both | `docs:` | everything above |

**A is taken, and it went: 2026-09-02, R1, outcome one — the panel sits above the shade and the
shade keeps its override.** F is in. What follows is written as planned rather than as a cut; §5
carries the numbers and the one thing the reading found that it was not looking for.

**All seven are in, 2026-09-02 to 2026-09-03, and none of them was cut.** A went on the 2nd; B, C and
F's two commits followed the same day; D, E and F's `feat:` on the 3rd, with three bugs found by
reading rather than by using — F1, F2 and the double navigation-bar inset — each fixed in its own
`fix:` and written into the readings block below. G is this file's own close-out and §15 lists what
it landed. **The phase ships five `feat:` commits, which is release 0.5.0**, and the two questions it
could not answer alone are in `DOD.md` with the twelve, not here.

**A is a gate, not a task, and it runs before a line of this phase is written.** Phase 1 carried one
that could veto the backlight half; this one can veto 3b outright. It is a reading taken against a
*bare* second window — no Compose, no controls, no lifecycle — because the question is about window
ordering and brightness ownership and nothing else, and every line of panel code written before the
answer is a line written on a bet.

**It used to sit fifth, and the reason given for that does not survive being checked.** The argument
was that the reading is cheapest to take while the phone is already set up for 3a. That is not a
dependency: the apparatus is a 200 dp rectangle added to `WindowManager` from `DebugSettings.kt` over
a live shade, and both halves of that ship today. What the delay bought instead was three decisions
made blind — **D's notification target**, which is a different `PendingIntent` entirely depending on
the verdict (§3); **E's weight**, which is the difference between a question worth spending the
twelve's attention on and one to drop (§4); and **B's recommendation on the deadline**, which is a
question about which surface actually gets used. If A says no, F is skipped, the phase closes with 3a
alone and §5 records the cut here rather than leaving it to be argued about later.

**E is the other unusual one: it can close with no code at all.** `PLAN.md` rule 5 names the
`ContentObserver` as one of two questions the twelve answer, and the honest outcomes are *ship the
observer*, *ship nothing*, and *ship the cheap half* — §4 recommends the third and builds the reading
that makes the first cheap if they ask for it.

---

## 1. `DimControls()` — the extraction, and what does not go in it

Phase 1 wrote the rule and Phase 2 restated it: **do not extract a composable with one caller,
because you are guessing at the second caller's shape** — but do not write it so that extracting it
later means unpicking it. `DimScreen.kt` has carried that comment since Phase 1 and it has held; the
slider block reaches for no `Context`, starts no service and assumes no `Scaffold`.

This is the phase with the second caller, so the guess is over.

**Two composables, not one, and the split is the answer to a question Phase 2 handed forward.**
`phase-2.md` left it open whether the deadline travels to the floating host and said the twelve could
answer it. Making the auto-off row its own composable turns that from an extraction decision into a
one-line decision *per host*, which is the shape that survives whichever way the twelve answer:

```kotlin
/** The dim level, the warmth and the backlight toggle. State in, callbacks out, no Context. */
@Composable
fun DimControls(
    dimLevel: Int,
    warmth: Int,
    lowerBacklight: Boolean,
    backlightAvailable: Boolean,
    running: Boolean,
    onDimLevel: (Int) -> Unit,
    onWarmth: (Int) -> Unit,
    onLowerBacklight: (Boolean) -> Unit,
    onToggleRunning: () -> Unit,
    modifier: Modifier = Modifier,
)

/** The chips and the "turns off at" line. Rendered by whichever host wants them. */
@Composable
fun AutoOffControls(
    autoOff: AutoOff,
    offAtMillis: Long?,
    running: Boolean,
    onAutoOff: (AutoOff) -> Unit,
    modifier: Modifier = Modifier,
)
```

**What stays in `DimScreen` and never travels:** the permission explainer, the notification warning
banner and its three-way `ShadeWarning` branch, the top bar, the `Scaffold` insets, the resume
re-read, and `endShadeIfDue()`. Every one of those either needs a `Context`, needs a lifecycle it can
observe, or is first-run framing that a dialog is the wrong place for. The compact host gets none of
it, which is exactly why §2 makes it forward to the full app rather than reimplement any of it.

**`backlightAvailable` is a parameter rather than a read**, for the same reason the rest are: it is
`readBacklightTop(context) != null`, it needs a `Context`, and both hosts already have one at the
callsite. Passing it in keeps the composable free of Android, which is what makes it renderable
inside a raw `WindowManager` window in §7 where there is no Activity to borrow one from.

**And the start/stop button travels, which is not obvious.** It is the one control that also needs
the service started, and starting the service is the *screen's* job (`CLAUDE.md`: a `ViewModel` never
holds a `Context`). So `onToggleRunning` is a callback, and each host wires it to its own
`beginShade()` / `startShade()` pair. The compact host's version is shorter than `DimScreen`'s
because the permission ask never fires from there — §2.

**Kotlin note for a JS reader:** this is props-down / events-up and nothing more. The reason it is
worth a section is not the pattern, it is Compose's version of the rule about where side effects may
live: a composable that takes only values and lambdas can be rendered by *any* host, and one that
calls `LocalContext.current` can only be rendered where that Local has been provided. In a raw window
it has not been, and the failure is a crash rather than a `undefined`.

**Recommendation on the deadline, and it goes to the twelve either way.** Ship `AutoOffControls()` in
the compact host. `phase-2.md`'s argument for it is the right one — the compact host is by
construction the surface reached while the shade is up, in the dark, without opening the app, which
is exactly where *"turns off at 23:40"* is worth reading and *"give me two more hours"* is worth
tapping — and the argument against it was that it costs surface in a small dialog, which is answered
by the dialog having nothing else in it. The counter-argument that survives is about the five chips
wrapping in a narrow floating window; that is a layout finding, and R5 is where it gets read rather
than guessed.

---

## 2. The compact controls — the host, and the three shapes it could have

`PLAN.md` describes this half as *"a theme, an activity flag and a preference: cheap, boring, known to
work"*, and two thirds of that survives contact with the code. The theme is cheap. The activity flags
are boring. **The preference is the part that is not known to work**, because of what it has to
change.

### The three shapes

| | Shape | What it costs |
| --- | --- | --- |
| **i** | One `MainActivity`, `setTheme()` in `onCreate` from a stored preference | The *starting window* is drawn from the **manifest** theme, before any code runs. One theme in the manifest means one of the two modes flashes the other's window frame on every launch |
| **ii** | Two activities, and an `<activity-alias>` the app enables and disables to move the launcher entry | `PackageManager.setComponentEnabledSetting` on a `LAUNCHER` component **removes the icon from the home screen** on many launchers, and some do not put it back. A preference that can lose the app's icon is not a preference |
| **iii** | Two activities. `MainActivity` keeps the launcher entry unconditionally and forwards to `ControlsActivity` when the preference says so | One frame of `MainActivity`'s starting window before the forward. No component surgery, no lost icon |

**Shape iii, and the reason is failure mode rather than elegance.** All three cost roughly the same
to write. Only one of them has a failure that the user cannot undo from inside the app: shape ii
loses the launcher icon, which is the one route back to the setting that lost it. Shape i's flash is
cosmetic and shape iii's is the same flash, bounded to the compact case only.

**The preference is inverted from the one `PLAN.md` describes, and this is the amendment.**
`PLAN.md` has the floating window as what the launcher opens, *"with a setting that expands it to the
full app"*. That default cannot survive a first run: a stranger's first launch has to be the full app
— the overlay explainer, the notification warning, the two hand-offs, all of which §1 keeps in
`DimScreen`. So the shipped shape is the other way round:

- **The launcher opens the full app.** Always, by default, and always on a first run regardless of
  the preference — see the guard below.
- **The notification and the tile open the compact controls.** Those are the two surfaces reachable
  *while the shade is up*, which is the case the compact host exists for.
- **One preference — *Open the compact controls from the launcher*, default off** — moves the
  launcher route to the compact host for the user who has decided that is what Gloam is for.

**This is a taste argued in a room with one person in it, which rule 5 says is the wrong room.** It
goes to the twelve beside auto-off's default (`DOD.md` already carries that one). The difference is
that this one is safe to ship wrong: the preference is off by default, so the argued-in-a-room
decision is the *lower*-risk of the two, and flipping the default later is a one-line change with no
stored state behind it.

### The guard, which is not polish

```kotlin
// ControlsActivity, before anything is composed.
if (!canDrawShade() || !escapeHatchLive()) { openFullApp(); finish(); return }
```

Both halves are live reads, on the same re-read-on-resume rule `DimScreen` already follows: a special
permission and a notification channel are both switches on settings screens this app hands the user
off to, and both can change while it is in the background. The compact host has no explainer, no
warning banner and no hand-off, so the honest thing for it to do when either is false is to be the
full app instead of a dialog with a broken start button on it.

**And the guard is what makes the forward's condition load-bearing rather than a detail.** Written
naively the two activities forward into each other: `MainActivity` reads the preference and forwards
to `ControlsActivity`, `ControlsActivity`'s *Open Gloam* button opens `MainActivity`, and the user
lands back where they started. The guard above is the worse half of it, because no user is in the
loop at all — revoke the overlay permission on the settings screen this app sent them to, tap the
icon with the preference on, and `MainActivity` forwards, the guard fires, `openFullApp()` forwards
again, and the two activities ping-pong unbounded.

**So the forward tests how `MainActivity` was entered, not merely what the preference says:**

```kotlin
// MainActivity.onCreate, before setContent.
if (launcherCompact && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) { openControls(); finish(); return }
```

A positive test for the one case the preference is actually about, rather than a suppression flag on
the routes out. A launcher tap always carries the category and `ControlsActivity`'s `startActivity`
never does, so neither the *Open Gloam* button nor the guard has to remember anything — and any later
entry into `MainActivity`, a deep link or a settings shortcut, is correct by default instead of
correct for as long as somebody remembers to opt out.

**`escapeHatchLive()` gets its second caller here**, which `phase-2.md` §2 predicted would come from
2b. It arrives from 3a instead and the predicate is unchanged, which is the point of having written
it as a function in `shade/` rather than inline in the screen that first needed it.

### The window

```xml
<style name="Theme.App.Controls" parent="Theme.AppCompat.DayNight.Dialog">
    <item name="android:windowIsFloating">true</item>
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:backgroundDimEnabled">false</item>
    <item name="android:windowNoTitle">true</item>
</style>
```

**The parent is not a style preference and is the same constraint `themes.xml` already documents.**
`AppCompatActivity` throws on a theme that does not descend from `Theme.AppCompat`, and
`ControlsActivity` has to be an `AppCompatActivity` for exactly the reason `MainActivity` is: it is
where `AppCompatDelegate` lives, which is what applies the in-app language (ADR-0004) and moves the
night mode (ADR-0006). A `Theme.MaterialComponents.*.Dialog` parent would pull in a second widget
library that renders nothing, and a bare `android:Theme.Dialog` would crash the activity.

**`backgroundDimEnabled` is `false` on purpose.** The platform's dialog dim is a scrim over
everything behind the window — which is a *second* dimming mechanism, drawn by the system, on top of
the two this app already has and outside every bound `ShadeRampTest` asserts. `CLAUDE.md`'s composite
rule is about the shade's two layers; a system scrim is a third that the rule cannot see. Off.

Manifest, on the activity:

```
android:theme="@style/Theme.App.Controls"
android:taskAffinity=""          the compact host is not the app's task
android:excludeFromRecents="true"  and it is not a thing to come back to
android:launchMode="singleTop"     a second summon reuses it rather than stacking
android:noHistory="true"           and it does not linger behind whatever comes next
```

None of those four is load-bearing for safety — they are all "this is a dialog, not a screen" — but
`noHistory` is the one worth naming, because without it a user who summons the controls from the
notification and then leaves finds them again the next time they switch to Gloam's task, over
whatever they were doing.

### What the phone said — R2, R3 and R5, 2026-09-02

Taken through a debug-only *Open compact controls* button rather than through `adb`, and the reason
is the first correction below. Screen held awake at `screen_off_timeout=600000` for the shaded half,
restored after.

**The theme floats it, and the shape is the one this section wanted.** `mAttrs` comes back
`(wrapxwrap) ty=BASE_APPLICATION fmt=TRANSLUCENT`, `Requested w=960 h=2440` — **320 × 813 dp** on a
407 × 904 dp display — in its own task (`taskId` 11553 against `MainActivity`'s 11552, which is
`taskAffinity=""` and `FLAG_ACTIVITY_NEW_TASK` doing what they were asked). Nothing behind it is
scrimmed: the Settings screen under the dialog reads at its own brightness, which is
`backgroundDimEnabled=false` holding.

**Two corrections to this file, both found by the phone rather than by review.**

- **§2's theme block is incomplete, and the missing item is visible.** With only
  `android:windowNoTitle` set, the window came up with a grey bar reading *"Gloam debug"* across the
  top of it. `AppCompatDelegate` reads AppCompat's own **unprefixed** `windowNoTitle` when it builds
  the sub-decor; the platform attribute is not the one it consults. `Theme.App.Controls` now carries
  both, plus `windowActionBar=false`, and the bar is gone.
- **§12's `am start -n …/app.gloam.ControlsActivity` cannot work**, and should not be made to. The
  activity is `exported="false"` — the only things that legitimately open it are this app's own
  notification and 2b's tile — so `am` refuses it with `Permission Denial: … not exported from uid
  10524`, because `adb` is uid 2000. The way in is a debug-only button, which is the same
  only-the-app-can-do-this-to-itself argument the backlight sweep and the two-minute deadline already
  carry. **Exporting the activity to make a command line work would be a real change**: a dialog any
  installed app could raise over the foreground app is a different thing from a dialog Gloam raises
  over its own shade.

**The window is at its ceiling in both locales, and R5 is what shows it.** 2440 px is exactly the
space between the status and navigation bars, so the dialog is floating but not short: English fits
with *Open Gloam* just visible at the bottom, and **Polish overflows by about one line** — the
backlight hint runs to six lines against English's five — so *Otwórz Gloam* sits below the fold until
the content is scrolled. It scrolls, nothing is clipped, and `FlowRow` wraps the five auto-off chips
to three rows in both languages with no truncation in either. **The way out being below the fold on
open is the finding**, and it is a layout question for this section rather than one for the twelve:
the deadline travelling is not what costs the room, the two explanatory hints are.

**R3, and it is the reading that matters.** Shade started *from the compact host* at dim 100, warmth
0, backlight on: the button flipped to *Stop dimming* and the deadline line appeared inside the
dialog, so the host's own start/stop and `AutoOffControls` are wired correctly. The panel went to
`nits= 6.642711` with `reason=override(io.github.srednimax.gloam.debug)` — `MIN_BACKLIGHT`, as
computed. What the shade then did to the dialog under it was measured rather than eyeballed: the same
window captured shaded and unshaded, 17 640 bright-pixel samples, **median transmission 0.2393**
(p10–p90 0.2362–0.2417) where `1 − MAX_SHADE_ALPHA` = 0.05 was expected and `1 − 0.8 ×
MAX_SHADE_ALPHA` = 0.24 was not. §0 carries the consequence; R6 still owns the constant.

**What this host is not:** an escape hatch. It fails clause 2 of `phase-2.md` §2's definition —
reaching it needs sight of Gloam's own UI, and §0 is why that is not fixable. `EscapeHatch.kt`'s
inventory does not gain a row, and this file is where somebody who assumed it would goes to find out
why.

---

## 3. The routes in: the notification, the launcher, and the tile's hole

Three doors, and only one of them is new machinery.

**The notification's content intent stops pointing at `MainActivity`, and where it goes instead is
A's verdict.** It is one `PendingIntent` target in `ShadeService.buildNotification()`. Phase 1's R8
found that on HyperOS a plain tap on the notification row follows the content intent — the *Stop*
action is behind a long-press and is not in the collapsed row at all — so today a plain tap lands on
the full app under the shade.

- **If A goes**, the tap summons the **panel**, and it launches no Activity at all:
  `PendingIntent.getService` at `ShadeService` with a `SHOW_PANEL` action. **This is the one door in
  this phase that is a legibility improvement rather than a reach one.** The panel is above the shade
  at 6.64 nits (§0), there is no task switch, and the notification exists only while the service runs
  — so the panel's one precondition, that there is a shade for it to belong to (§6, rule 2), holds by
  construction on this route and needs no check.
- **If A is a no-go**, it points at `ControlsActivity` — under the shade at 0.33 nits, **a reach
  improvement and not a legibility one** (§0), which is the honest way to describe it.

R4 reads the tap end to end at maximum dim either way, so which of those two it turned out to be is
on the record rather than assumed.

**The panel does not take the compact host's other doors.** The icon and 2b's tile both reach
`ControlsActivity` whether or not the panel exists, because both can be tapped with no shade running
— and a window that cannot exist without one can never be the surface that starts it. That is the
division the two hosts settle into: **below the shade is where the shade gets started, above it is
where a running shade gets adjusted.**

**Kotlin/Android note on changing it.** `PendingIntent`s are cached by requesting identity, and two
`Intent`s that are `filterEquals` — same action, data, type, component, categories — are the same
pending intent regardless of their extras. Both candidate targets change that identity, and
`getService` changes more than the component: it is a different *kind* of pending intent from
`getActivity`, so there is nothing stale to inherit. If a later change is extras-only, it needs
`FLAG_UPDATE_CURRENT` beside `FLAG_IMMUTABLE` or the old extras survive an app update, silently.

**The launcher route** is shape iii from §2, with the entry test the guard above adds:
`MainActivity.onCreate` reads the preference and forwards only on a launcher tap. It needs the value
*before* the first frame, which is a `suspend` read, which the app already does once —
`MainApplication` reads `themeModeNow()` before any Activity exists and hands it over as
`startupThemeMode`. **`launcherCompactNow()` joins it there**, a second one-shot accessor of exactly
the same shape inside the same `runBlocking`.

**And it is deliberately *not* modelled on `ShadeIntent`.** That type exists because `beginShade()`
and `endShade()` write `running` and `offAtMillis` in one transaction, so a reader that could take
them apart is a reader that can see a half-written intent. Nothing writes the theme mode and the
launcher preference together and neither bears on the other, so there is no torn read to prevent —
and no saving to chase either, because DataStore serves `store.data` from its in-memory cache after
the first collection, so the second `first()` is not a second disk read. Borrowing the combined-read
argument here would leave a later reader holding a rule that was never true.

**The tile is 2b's and the hole is one line.** `PLAN.md` gives the compact controls two doors and
this phase can only build one of them. A `TileService`'s click either sends the user to an activity
or toggles the shade; 2b's tile is safety equipment and its primary job is stopping the shade, so the
integration this phase asks for is small and stated here so 2b does not have to rediscover it:
**2b's tile, whatever else it does, launches `ControlsActivity` for its long-press / "open app"
route** rather than `MainActivity` — and that is the same line whichever way A went, because a tile
can be tapped with no shade running and the panel cannot answer that. If 2b ships first, that is the
line. If 3a ships first, 2b's default already points at `MainActivity` and moving it is a one-word
edit.

---

## 4. The brightness slider question, in its strongest form

ADR-0010's first amendment parked this in 3a and rule 5 names it as one of two questions the twelve
answer. The question as parked: *"whether Gloam should go further and watch `SCREEN_BRIGHTNESS` with
a `ContentObserver`, reinterpreting a change as 'I want more light' — or whether that is too
clever."* Before it goes to anybody it is worth stating in the form that is actually attractive,
because the easy form loses on its own and the hard form does not.

**The failure it addresses is real and Phase 1 only half-fixed it.** While the override is live the
user's own brightness slider moves and does nothing. Phase 1 ships copy saying so — `dim_backlight_hint`,
always visible, deliberately not a dialog. **The copy is in the app, and the app is under the shade
at 0.33 nits.** The person who most needs the explanation is the person who cannot read it. That is
not a criticism of Phase 1's choice; it is the same ceiling as §0, arriving in a third place.

**The weak form.** Observe `SCREEN_BRIGHTNESS`; on a change, release the override. It loses on the
noise floor. Phase 1's R4 measured the framework storing *its own* adaptive choice back into the same
integer — `raw=14` at 6.3 lux — so under adaptive brightness the observer fires because the room
changed, and Gloam would read a cloud passing as an instruction. Gating it on
`SCREEN_BRIGHTNESS_MODE == 0` makes it a manual-mode-only feature, which is a large fraction of users
excluded from a fix for a problem they still have.

**The strong form, and it is genuinely interesting.** The system brightness slider lives in the
control centre, which R5 measured as *above* the shade and legible at the full 6.64 nits. It is
therefore an always-reachable, always-readable control that exists on every phone and that Gloam did
not have to build. Reinterpreting it — brightness up meaning dim level down — does not add a control
surface, it **un-breaks one**, and it does it on precisely the surface §0 says we cannot otherwise
reach. If that works, it is a partial substitute for 3b, which is a fact 3b's go/no-go should know
about and §5 records.

**What is wrong with the strong form**, stated as plainly as its appeal:

- It still needs manual mode, for the reason Phase 1's R4 found. Nothing fixes that; the setting is
  the same integer.
- It makes a *system* control do something the system did not promise, on a screen where the user
  cannot see which app is responsible. A user who then stops the shade and finds their brightness
  somewhere they did not put it has been surprised by an app that was not on screen.
- It is unreadable from the outside: no reading can tell a user's drag from another app's write.
- And it is exactly what `PLAN.md` means by *too clever*, which is why rule 5 sent it to the twelve
  rather than settling it here.

**Recommendation: ship the cheap half in this phase, build the reading, ship the observer only if the
twelve ask for it.**

The cheap half is that the explanation moves to a surface that can be read: the ongoing notification
is drawn above the shade and it is where the user already looks. While the override is live it says
so, in one line, in both locales. That fixes the *"my phone is broken"* reading of the symptom
without reinterpreting anything, and it is true on every device and in both brightness modes.

**It is not, however, free, and an earlier draft of this section said it was.** The claim was that
the service already updates the notification's text on every value change it collects. It does not:
`buildNotification()` is called exactly once, from `onStartCommand`, into `startForeground`; there is
no `notify()` anywhere in `app/src/main/`; and the notification carries a `setContentTitle` with **no
content text at all**. `applyShadeValues` writes the layers' `alpha` and the window's
`screenBrightness` and never goes near the notification. So checkpoint E is three small things rather
than one:

1. `setContentText` on the builder, which is a line that does not exist today.
2. A `NotificationManager.notify(NOTIFICATION_ID, buildNotification())` path, because
   `startForeground` posts once and never again.
3. **A trigger — and this is the part that is a decision rather than plumbing.** The line is only
   *true* while the override is actually live: `lowerBacklight && dimLevel > 0 && backlightTop !=
   null`, which is the condition `applyShadeValues` already branches on. `lowerBacklight` defaults to
   `true`, so it usually holds — but not always, because the toggle can be off, and dim 0 with the
   service running is a legitimate state rather than an off one (`CONTEXT.md`).

**So the notification is re-posted on that boolean's transitions and on nothing else** — derive it in
the collector that is already there, `distinctUntilChanged()`, and `notify()` once each way. The
alternatives fail in both directions at once. `DimScreen`'s slider writes on every integer step, so
the settings flow emits up to a hundred times per drag and Android throttles sustained notification
updates: re-posting per value is a binder call per slider pixel *and* a good way for the update that
matters to be the one dropped. Posting the line unconditionally instead is worse still — it would
tell a user who turned the backlight toggle off that their brightness slider is paused, which is a
false explanation on the one surface this section chose *because* it can be read.

The reading is **R10**: how noisy is `SCREEN_BRIGHTNESS` under adaptive on this ROM, and does
HyperOS's control-centre slider write it at all? Both halves are cheap while the phone is already
attached for this phase, and together they decide whether the strong form is even available here. If
the twelve say yes, the observer is then a small, already-measured piece of work rather than an
investigation.

**R10 is taken, and it removed the objection this section leant on hardest** (2026-09-03). Both
halves came back yes-and-better: HyperOS's control-centre slider does write `SCREEN_BRIGHTNESS`, and
it writes it **once per gesture rather than per pixel** — so an observer would hear one event per
drag, not a hundred. The noise floor is the surprise. The argument above was that under adaptive the
framework keeps writing its own choice into the same integer, so the observer would read a cloud
passing as an instruction; that is true of the setting in general and **cannot happen while our
override is live**, because the override switches the framework's auto-brightness controller off
outright — `mState=AUTO_BRIGHTNESS_DISABLED` and the light sensor not sampled at all, against
`AUTO_BRIGHTNESS_ENABLED` at a 250 ms rate the moment the shade stops. An observer that listens only
while the shade is up therefore hears nothing it did not want to hear, **and the manual-mode gate
this section called "a large fraction of users excluded" is not needed at all.**

**The recommendation does not move, and it matters that it does not.** Two of the four objections
were never about noise: a *system* control would be doing something the system did not promise, on a
screen that does not say which app is responsible, and no reading can tell a user's drag from another
app's write. Those are the reasons rule 5 sent this to the twelve, and they are untouched. What R10
changes is the *shape of the question they get asked* — it is no longer "would you accept this on
manual brightness only, knowing it might misfire", it is "should your brightness slider mean
something else while Gloam is dimming", which is a taste question with the engineering objection
removed from underneath it. `DOD.md` carries it, beside auto-off's default, because it outlives this
phase the same way.

---

## 5. The go/no-go, restated in the terms ADR-0010 left it in

`PLAN.md` states the kill condition as *"if a window above the shade seizes the backlight override
and cannot be made to match it, live preview is broken by construction and this phase is cut rather
than attempted."* ADR-0010's third amendment narrowed it and then refused to close it:

> A panel window of Gloam's own *above* the shade that sets no brightness of its own should leave the
> shade the topmost requester by the same rule — but "should, by the rule" is not a reading, and 3b
> owns taking it.

So the gate has **two** questions, not one, and the second is the one nobody has written down yet.

**Question one: does our second window sit above the shade at all?** Both windows are
`TYPE_APPLICATION_OVERLAY` from the same uid. Within one window type, ordering is the window
manager's business and the expectation is insertion order — the later `addView` on top. That is an
expectation, not a documented guarantee, and a panel *below* the shade is a panel dimmed by the thing
it exists to control, which is 3a's problem again with more machinery. **Read it in `dumpsys window
windows`, which prints the stack in order.**

**Question two: with the panel above and declining a brightness, who owns the override?**
`BRIGHTNESS_OVERRIDE_NONE` is the `LayoutParams` default and means *not asking*, so by the rule Phase
1's R5 and R9 found — the topmost window that *asks* owns it — the shade should keep it. Three
outcomes and only one of them is a cut:

| Outcome | What it means | Verdict |
| --- | --- | --- |
| The shade keeps `0.01`; the panel is above it | The rule holds for our own windows too | **Go**, with nothing to build |
| The panel takes the override as *none*; the panel jumps back to the user's brightness | Topmost is consulted even when it declines | **Go**, and the panel carries the shade's own `screenBrightness` on every update — we own both windows, so matching is a field copy |
| The panel takes it and cannot hold our value | Something in the ROM overrides per-window brightness for a second overlay | **No-go.** F is skipped |
| The panel will not sit above the shade | Live preview is inverted | **No-go**, unless removing and re-adding the shade underneath is reliable — which is itself a reading, and a shade that is briefly absent is not a trade worth making |

**How it is read, and it is deliberately not the panel.** A `chore:`-committed button in
`app/src/debug/.../DebugSettings.kt` adds a bare `View` — a coloured rectangle, 200 dp square,
`TYPE_APPLICATION_OVERLAY`, no Compose, no lifecycle, no controls — above the live shade, and a second
button removes it. That is the whole apparatus. It is the same justification the backlight sweep and
the two-minute deadline button beside it already carry: only the app can do this to itself, and the
seam exists so that developer-only surface never reaches a release build.

**A's verdict, read on the phone 2026-09-02: go, and it is outcome one — nothing to build.** Taken
with the shade live at dim 100, `MIN_BACKLIGHT` applied, screen held awake:

- **Order.** `dumpsys window windows` prints the stack top-first, and the two come back adjacent and
  the right way round: `Window #8` is the 600 px square — `ty=APPLICATION_OVERLAY`, `gr=CENTER`, no
  `sbrt=` — and `Window #9` is the shade, `fillxfill` with `sbrt=0.01`. The later `addView` is on
  top, which is what insertion order predicted and what nothing documents.
- **The override.** With the square above it and asking for no brightness of its own,
  `mWindowManagerBrightnessOverride=0.01` and the tag still `io.github.srednimax.gloam.debug`.
  Removing the square changed neither; stopping the shade released it to `NaN` and the panel went
  back to the user's own brightness.

So the rule Phase 1's R5 and R9 measured against other apps holds between two windows of ours as
well: **the topmost window that *asks* owns the override, and one that declines is not consulted.**
The panel therefore keeps `BRIGHTNESS_OVERRIDE_NONE`, and §6's field-copy fallback is not built — it
stays written down as the thing to reach for if a future ROM disagrees.

**One thing R1 found that it was not looking for, and it is not settled.** Both of Gloam's overlay
windows print `alpha=0.8` in `mAttrs` — a `LayoutParams` field **this app never sets**, on the
shade's window as much as on the debug square. It is applied rather than merely printed: in the
`screencap` the square comes back `(205, 1, 205)` where magenta is `(255, 0, 255)`, which is 0.8 of
it, while every sample outside the square is under `18/255`. That is the eyes-on half of the
ordering answer — the square is plainly not dimmed by the shade — and it opens a question this
reading cannot close: what an 0.8 window alpha does to the **shade's own** composite, which
ADR-0010's cap computes on the assumption that the layer alphas we set are the layer alphas that
land. **R6 owns it**, panel and shade in one frame. Nothing here shows a cap exceeded, and the error
would run *lighter* than computed rather than darker — the safe direction for the bound that keeps
the way out visible, and the unwelcome one for the thing the app is for.

**And it needs nothing else in this phase, which is why it is checkpoint A.** `ShadeService` ships a
live shade today and the debug seam already carries surface of exactly this kind, so the reading is
takeable before the first line of 3a is written — and it has to be, because three of 3a's own
decisions are answers to it.

**If it is a no-go, what the phase closes as** — and, because this is checkpoint A, it is known
before anything has been built on the other answer. 3a alone, with §0's paragraph promoted from an
observation to the phase's result: Gloam's controls are reachable from three doors and legible from
none of them once the dim level is high, the notification and the tile remain the way out, and the
`ContentObserver` question in §4 stops being a nicety and becomes the only remaining route to
adjusting the dim without stopping it. `PLAN.md`'s Phase 3b entry is struck through with a date and
the reading that struck it, exactly as `DOD.md` records the API 26 emulator leg being cut.

---

## 6. The panel — the window, and the safety rule that is the opposite one

**`CONTEXT.md` already names it and already says why it is dangerous:** *"the **touchable** overlay
window carrying the controls, sized to its own content. A second window with the opposite safety
rule."* `CLAUDE.md`'s house rule — the one it calls load-bearing — is that the shade carries
`FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE` so every touch passes through, because without them the
phone appears frozen. **The panel deliberately drops the first of those two.** That is the single
riskiest line of code in this plan and everything below is about bounding it.

### The flags, and which one does what

```kotlin
const val PANEL_WINDOW_FLAGS =
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
```

- **`FLAG_NOT_TOUCHABLE` is absent, and that is the point.** The panel catches touches; that is what
  makes it a control rather than a picture of one.
- **`FLAG_NOT_FOCUSABLE` stays.** These two are routinely confused and they are not the same
  property: *touchable* is whether the window receives touch events, *focusable* is whether it takes
  input focus and the key events that come with it. A panel that took focus would take the Back key
  away from the app underneath and would raise the IME for a field it does not have.
- **`FLAG_LAYOUT_NO_LIMITS` is absent**, unlike the shade's. The shade needs to extend past the
  system bars because a bright strip across the top reads as a bug. The panel is sized to its
  content and has no business outside the display's own bounds.

### The rule that keeps it from being a trap

**A touchable window blocks every touch under it, so the bound is its size.** The shade's safety
comes from a flag; the panel's comes from its `LayoutParams`, which is a weaker kind of guarantee
because a flag is a constant and a size is a computation.

1. **Never `MATCH_PARENT`, and the width is a number rather than a wish.** Bottom-anchored with
   `Gravity.BOTTOM`; `WRAP_CONTENT` height, because a bounded height clips a control and an
   unreachable close button is the trap the rest of this section exists to prevent; and an **explicit
   pixel width** — `params.width = panelWidthPx(displayWidthPx)`, the display width less a margin,
   capped at a maximum.
   **`WRAP_CONTENT` width was the draft and it is the wrong guarantee.** The risk this rule names is
   a long translated label growing the window to the full display, and `WRAP_CONTENT` is precisely
   the value that hands that decision to the content: the bound would live in a
   `Modifier.widthIn(max = …)` inside the composition, which no test can see and which the window
   manager does not enforce. A width the window is *added with* cannot be exceeded by a translation
   at all; it is a pure function of one integer, so it sweeps on the JVM the way the ramp does
   (`PLAN.md` rule 3); and for a panel that is mostly sliders it is the better layout anyway, since
   `WRAP_CONTENT` would size a slider to its intrinsic minimum. §13 writes the sweep, and the
   instrumented test keeps the height half, where only a device has the real strings.
2. **It cannot outlive the shade.** The panel is added and removed by `ShadeService`, which already
   owns the window manager and the scope; `removeShadeWindow()` gains a `removePanelWindow()` beside
   it, and `onDestroy` takes both down. There is no state in which the panel is up and the shade is
   not.
3. **It dismisses on an explicit close and on an inactivity timeout**, per `PLAN.md`, and not on
   tap-outside. Tap-outside is the wrong gesture here for a reason that is specific to this window:
   the panel sits over another app's UI, and a dismissal gesture that is also a tap on that app is a
   gesture whose two effects the user cannot separate. §8 is the timeout.
4. **The close control is not optional.** With `FLAG_NOT_FOCUSABLE` the Back key never reaches the
   panel, so there is no system gesture that closes it. If the close button fails to draw, the
   timeout in §8 is the only way out — which is why the timeout is a service-side `delay` on a clock
   the panel's composition cannot influence, rather than something the panel arms for itself.

### The brightness it carries

Whatever §5 answers. Outcome one: nothing — the field stays at `BRIGHTNESS_OVERRIDE_NONE` and the
shade keeps the override. Outcome two: the panel's `LayoutParams.screenBrightness` is written from
the same `ShadeValues.backlight` the shade's is, in the same `applyBacklight` pass, so the two windows
can never disagree. **In neither case does the panel compute a brightness of its own.** One dim level,
one ramp, in a fixed order (ADR-0010) — a second window deriving a second brightness would be a
second ramp wearing a window's clothes.

---

## 7. Compose with no Activity under it

This is where 3b's cost actually is, and it is worth being concrete because "attach three owners by
hand" undersells how it fails: it does not fail as a crash you can read, it fails as **a window that
appears and never recomposes**, which looks like a bug in the sliders.

An Activity is not just a screen — it is the thing that provides everything a Compose hierarchy
assumes exists. `ComposeView` walks up the view tree looking for a `LifecycleOwner`, a
`SavedStateRegistryOwner` and (only if something calls `viewModel()`) a `ViewModelStoreOwner`. In a
window added straight to the `WindowManager` there is no Activity above it, so nothing has set them,
and they have to be built:

```kotlin
private class PanelHost(context: Context) : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    val view = ComposeView(context)

    init {
        savedStateController.performAttach()
        // **Before the lifecycle passes CREATED, or the restore throws.** There is nothing to
        // restore — the panel has no saved state — but the registry insists on being told so.
        savedStateController.performRestore(null)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun show() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }

    /** Terminal — the next summon builds a new host. See below. */
    fun destroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
}
```

**One host per summon, and that is not a tidiness preference.** `DESTROYED` is terminal:
`LifecycleRegistry` has no upward event out of it and throws when asked to move up, and
`SavedStateRegistryController.performRestore` runs once per controller, so the `init` block above
cannot be re-run either. A `show()` / `hide()` pair on one long-lived host therefore works exactly
once — and §8 gives the panel three ways to close, all of which the user then reopens from the
notification, so the *second* summon is the ordinary case rather than an edge one. So `ShadeService`
builds the host when it adds the window and drops the reference when it removes it, which is the same
thing §9 says about storage: **the panel is summoned, not remembered**, and no state survives between
summons to go stale.

**`RESUMED` is not a formality.** The `Recomposer` that Compose installs for a window is tied to that
lifecycle: below `STARTED` it stops applying recompositions. A host left at `CREATED` produces a
window that draws its first frame correctly and then never changes again — the slider will not move
under your finger, and nothing in logcat says why. **This is the single most likely way 3b goes
wrong**, and it is why R7 reads a slider actually moving rather than a window appearing.

**No `ViewModelStoreOwner`, and that falls out of §1 rather than being a saving.** The panel calls no
`viewModel()`, because `DimControls()` takes state and callbacks as parameters. The state comes from
the service collecting `AppPreferences` — which it already does, for the shade — and the callbacks
write back through the same object. So the third owner is not needed, and the panel has no
`ViewModel` to be scoped, cleared or leaked. Had `DimControls()` been written to read its own state,
this section would be three owners and a `ViewModelStore` whose clearing nobody owns.

**The theme.** `AppTheme` needs no Activity — it is a Compose function over `MaterialTheme` — but
`dynamicColor` (ADR-0006) resolves against the context it is given, and the service's context is not
configured by `AppCompatDelegate` the way an Activity's is. The panel takes the same
`AppTheme(themeMode, dynamicColor)` call with values read from `AppPreferences`, and **R6 is where the
panel's palette is confirmed to match the app's** rather than assumed to.

**Configuration changes.** A raw `WindowManager` view is not recreated on rotation the way an Activity
is; the service gets `onConfigurationChanged` and the view is re-laid-out under it. Compose reads
configuration through the context's resources, which do update — but a bottom-anchored `WRAP_CONTENT`
window whose content re-measures is exactly the case where a size bound gets exceeded quietly. **R8.**

---

## 8. Dismissal, and what makes the panel not a trap

Three ways out, and the ordering is deliberate — each one is there because the one above it can fail.

1. **The close control**, drawn in the panel. The ordinary way, and the only one the user thinks
   about.
2. **An inactivity timeout**, owned by the service. A `delay` on the service's scope, re-armed on
   **every touch the panel receives**, taking the panel down when it expires. It is the panel's
   version of auto-off and it is here for the same reason: a control surface that only closes when
   you successfully press a button on it is a control surface that traps you when the button does not
   draw.
   **Touches rather than values written, which is what the draft said and is the wrong trigger for
   this window.** `PLAN.md` gives the panel one reason to exist — the slider moving the dim over real
   content — and judging that means setting a value and then *looking*, which writes nothing. A user
   who drags once and studies the result would lose the panel mid-judgement, and a user who taps the
   switch and taps it back would not re-arm at all. The recovery property the timeout is actually for
   survives the wider trigger untouched: a panel drawn off-screen by a layout bug receives no touches
   either, so it still dies on schedule. The place to catch them is `dispatchTouchEvent` on the
   host's `ComposeView`, which sees every touch including the ones a child consumes — a Compose
   `pointerInput` would see those only on `PointerEventPass.Initial`.
3. **The shade coming down**, by any of its routes — the notification's *Stop*, 2b's tile, the
   deadline, the ROM. The panel is a child of the shade's lifetime, not a peer of it.

**The timeout is a constant, not a preference**, and it belongs in `shade/` beside the others for
`CLAUDE.md`'s reason: it is a safety floor rather than a taste. Start it at **30 seconds** of no
interaction, which is long enough to move two sliders and read the result and short enough that a
panel drawn off-screen by a layout bug is gone before the user concludes the phone is broken.

**Kotlin note, and it is the same shape Phase 2 already uses.** The re-arm is `collectLatest` over the
panel's own interaction events: a new emission cancels the wait still running for the previous one,
which is `switchMap` and not `forEach`. `ShadeService` already runs exactly this pattern for the
deadline, and the same trap applies — the write that takes the panel down must not be cancelled by
the collection it is itself feeding.

**One thing the timeout must not do: take the shade with it.** The panel closing is not the user
saying they are done reading. The two lifetimes point one way only.

---

## 9. Storage

**One new key, and it is the first key added after the door.** `PLAN.md` rule 2 freezes every
DataStore key *that has been written* at Phase 2, which does not forbid new ones — it means this key
becomes frozen the moment a build carrying it reaches a phone, and it is worth adding with that in
mind rather than discovering it later.

| Key | Type | Default | Read by |
| --- | --- | --- | --- |
| `launcher_compact` | boolean | `false` | `MainApplication` at startup, `MainActivity.onCreate` |

**Named for what it is rather than for what it does.** Not `compact_mode` — there is no mode; the
compact host is always available from the notification and the tile, and this key decides one route
only. A name that implies a mode is a name a later phase reads as permission to branch on it.

**Read as a `Flow` like everything else, and read once at startup like `themeMode`.** The `Flow` is
what the Settings screen collects; `launcherCompactNow()` is what `MainApplication` needs before any
Activity exists, for the same reason `themeModeNow()` exists — a value that arrives after the first
composition is a value that arrives a frame too late, and here that frame is a whole activity launch.
Two one-shot accessors rather than one combined read, and §3 says why this is not the case
`ShadeIntent` was written for.

**Nothing else is stored.** The panel is summoned, not remembered: no "panel was open" flag, no last
position, no size. A window that restores itself after a process death is a window that can come back
without the shade it belongs to, which §6's rule 2 exists to make impossible. The inactivity timeout
and the panel's size bound are constants in `shade/`, not preferences, for `CLAUDE.md`'s line between
the two.

**And if §4's observer ships**, it is a second key on the same terms — a boolean, defaulting off,
read as a `Flow` by the service. It is written here so that the twelve saying yes is a small change
rather than a design session.

---

## 10. Copy

Both locales, in the same merge, because the gate is a merge gate. English first, per
[`translator-brief.md`](translator-brief.md).

| Key | English | Why it is worded that way |
| --- | --- | --- |
| `settings_launcher_compact` | Open the small controls from the icon | Not "compact mode" — §9's reason. "The icon" rather than "the launcher", which is a developer's word |
| `settings_launcher_compact_hint` | Tapping Gloam opens just the sliders instead of the whole app. The notification always opens the small controls. | Says what the *other* routes do, because the preference only moves one of three |
| `controls_open_app` | Open Gloam | The compact host's one way out to the full app. It must exist, or the dialog is a dead end with no Settings behind it |
| `shade_notification_text_backlight` | Your brightness slider is paused while Gloam is dimming | §4's cheap half. **The one line of copy in this app that is read on a screen at 6.64 nits**, so it is short, and it names the symptom before the cause |
| `panel_close` | Close controls | Content description as well as a label — §6 rule 4 makes this the ordinary way out, so it has to be findable by a screen reader too |

**"Panel" is not a word the UI says.** It is a domain word for two windows that are not each other
(`CONTEXT.md`), and a user has no reason to learn it. On screen the panel is *the controls*, the same
as the compact host is; they are one composable in two windows and the user meets them as one thing.
That is the whole reason §11 adds a vocabulary row rather than assuming the existing two cover it.

**Nothing here names a gesture.** Phase 1's R8 recorded the reason: HyperOS puts the notification's
*Stop* behind a long-press and stock Android does not, so copy that says "long-press" is wrong on
most devices. The same rule applies to anything this phase might have been tempted to say about
reaching the tile.

---

## 11. Documents this phase amends

- **`PLAN.md`** — 3a and 3b each gain a *Detail: `phase-3.md`* line. **Rule 4's table is untouched**:
  this phase adds no ask, which is worth stating because it is the first phase since 0 that does not.
  **3a's paragraph is corrected in one place** — the launcher preference is inverted and the shape is
  iii rather than a re-themed `MainActivity` (§2). **3b's paragraph gains §5's two questions** in
  place of its one, and its result, whichever way it goes. **And rule 3's count goes from three tests
  to four**, which is a change to a standing rule rather than a phase note: `panelWidthPx` is a pure
  function bounding a safety value — the bound that stands in for `FLAG_NOT_TOUCHABLE` — so it falls
  in exactly the category rule 3 reserves, and a fourth test arriving quietly would make the rule
  untrue. That one edit is made *with* this plan rather than at G, because the rule is what justifies
  the test.
- **`CONTEXT.md`** — **one row, and it is written already** rather than owed, on the same footing as
  the **Panel** row, which has described a window that does not exist yet since Phase 0. The existing
  **Shade** and **Panel** rows are correct and stay; what was missing is the third surface, and three
  things that can all be called "the controls" is exactly the collision that file exists to prevent.

  **The axis the row names is position, not size**, and the draft of it got that wrong. The panel is
  small too, carries the same controls and has no top bar either; calling one of them *compact*
  implies the other is the big one. What separates them is which side of the shade each sits on, and
  that difference is total — 0.33 nits against 6.64, touch-through against touch-catching. The term
  itself stays, because the shipped copy says *the small controls* and `settings_launcher_compact` is
  already named for it, and because "host" is a developer's word by the same test §10 applies to "the
  launcher". What the row *asserts* is what changed. It also records that **both surfaces are called
  *the controls* on screen**: `CONTEXT.md`'s own rule for a domain word and a user-facing word that
  differ is that both are written down with the difference stated, and until now that fact lived only
  in §10 of this file, where nobody looking up "the controls" would find it.

- **ADR-0010 — a fourth dated amendment.** Its *Consequences* left one question open for Phase 3 —
  *"when a second window appears above the shade, whose override applies?"* — and its third amendment
  narrowed it to our own windows and refused to answer it without a reading. §5 takes that reading;
  this is where the answer lands, go or no-go.
- **A new ADR, if §5 says go: *The panel is touchable, and what keeps it from trapping the user*.**
  It is ADR material rather than a phase note because it deliberately reverses the property
  `CLAUDE.md` calls load-bearing, and because the bound that replaces `FLAG_NOT_TOUCHABLE` is a
  computed size rather than a constant. A future reader who finds a `MATCH_PARENT` in the panel's
  params should be able to find out in one hop why it is a bug rather than a simplification. **If §5
  says no-go, no ADR is written** — there is nothing to decide.
- **`CLAUDE.md`** — two edits. The shade's house rule describes one window and there are about to be
  two with opposite rules; it gains the sentence, and links the ADR above rather than restating it.
  The layout block gains `ui/dim/DimControls.kt` and `shade/PanelWindow.kt`, and
  **`ControlsActivity.kt` at the package root beside `MainActivity.kt`** rather than under `ui/` — it
  is a second host for the app shell, which is exactly what that block says lives at the root.
- **`DOD.md`** — **two items open and one closes.** Open: the launcher-preference default (§2) and
  the `ContentObserver` (§4), both rule-5 questions for the twelve, recorded there rather than here
  so they outlive the phase that raised them — the same treatment auto-off's default already gets.
  Closes: nothing this phase owns, because everything in `DOD.md` today is Phase 2's door.
- **`README.md`** — one line. The compact controls are the first thing a user meets that is not the
  dim screen.
- **`docs/store-listing.md`** — release notes for whatever versions this phase cuts. `notes-gate.py`
  fails a release whose notes are behind it, and it has already reddened one release PR.
- **`scripts/edge-to-edge.py`** — the compact host is a **floating** window, so the insets matrix's
  question does not apply to it in the same way; what does apply is that it must not be clipped by
  the navigation bar on a gesture-nav device. One scene, reached the way the driver reaches a detail
  screen. The panel is not in that harness at all — it is not an Activity, and the driver walks
  activities.

---

## 12. Readings, and things that only look like readings

Rule 3: device behaviour is proven by measurement, with the command that read it. Everything here
needs the phone plugged in — `adb devices` first. **R-numbers restart at R1 for this phase**; Phase
1's are in `phase-1.md`, Phase 2's in `phase-2.md`. **A bare R-number in this file is always this
file's own**, and an earlier phase's is written *"Phase 1's R8"* every time — `phase-2.md`'s
convention, and it earns its keep here because three of Phase 1's numbers collide with three of
these.

**Read with the screen held awake wherever brightness is involved.** `settings put system
screen_off_timeout 600000` first, and put it back after. Four of Phase 1's readings were first taken
inside the inactivity timeout, which pins the panel to its floor and looks exactly like *the setting
did nothing* — the correction is on ADR-0010's third amendment and it cost a withdrawn reading.

| # | Reading | Decides |
| --- | --- | --- |
| **R1** | **The go/no-go, both questions** (§5): is our bare second window above the shade in `dumpsys window windows`, and does `mWindowManagerBrightnessOverride` still read the shade's `0.01` | **checkpoint A's verdict.** Nothing else in this phase starts until this has a result |
| **R2** | The floating host's shape: does §2's theme produce a floating, correctly-sized window, and does shape iii's forward flash | **checkpoint C's shape**, and whether the fallback is needed |
| **R3** | **The compact host's legibility at dim 100**, under a live shade, dark room | §0's claim, re-read against the new host rather than inherited from Phase 1's R8 on `MainActivity` |
| **R4** | The notification's **plain tap** on HyperOS, at maximum dim, end to end | §3 — where the route now lands, and whether that is a reach improvement or a legibility one |
| **R5** | The compact host in both locales, and the five auto-off chips in a narrow floating window | §1's recommendation on whether the deadline travels, and whether Polish wraps it |
| **R6** | The panel drawn at dim 100: its own nits, the content's nits, and its palette against the app's | that the panel is the legible surface §0 says it is — the whole case for 3b |
| **R7** | The panel catches touches **and the shade still passes them**: a slider moving under a finger inside the panel, a tap landing in the app outside it | §6, and §7's `RESUMED` trap, which is invisible any other way |
| **R8** | The panel across a rotation and a locale change | §7's configuration note, and the size bound under a re-measure |
| **R9** | The panel's inactivity dismissal on a **touch**-idle timer, and that stopping the shade takes it down | §8's three routes out |
| **R10** | **`SCREEN_BRIGHTNESS` noise under adaptive**, and whether HyperOS's control-centre slider writes it | §4 — whether the strong form is even available on this ROM |
| **R11** | **API-33 AVD end-of-phase pass** (ADR-0008): launches, compact host appears, shade appears, panel appears if 3b went | the phase's own closing gate |
| **R12** | The notification line: that it appears and disappears with the override, that a slider drag posts nothing, and that the row is legible above the shade | §4's cheap half. **Added by checkpoint E rather than planned** — the trigger is the part of §4 that is a decision, and a decision about how often to call `notify()` is only readable from outside the app |

**R1 is the only reading in this file that another reading cannot be substituted for**, and it is
numbered first because it is now taken first. Everything else here confirms something already argued;
R1 decides whether half the phase exists. It is taken against the debug button in §5, not against
panel code.

**R3 and R6 are the same reading twice and that is the point.** One control surface under the shade,
one above it, same dim level, same room. The pair is the phase's result in two numbers, and if they
come back the same something is wrong with the second window rather than with the argument.

### Derivations — arithmetic, and the test is the proof

| | Quantity | How |
| --- | --- | --- |
| D1 | The ratio between the panel and the content under the shade | `1 / (1 − MAX_SHADE_ALPHA)` — the panel sits above the alpha and shares the backlight, so it is **20×** what the app under it gets, at every dim level. R6 confirms; nothing measures it |
| D2 | The compact host's nits at maximum dim | `MIN_BACKLIGHT` nits × `(1 − MAX_SHADE_ALPHA)` = 0.33 on the development panel. Already derived in Phase 1; restated because §0 argues from it |

```bash
adb devices
adb shell settings put system screen_off_timeout 600000   # and put it back after
adb shell dumpsys window windows | grep -E 'Window\{|ty=|fl=|sbrt='   # the stack, in order
adb shell dumpsys display | grep -E 'mScreenBrightness=|mWindowManagerBrightnessOverride'
adb shell settings get system screen_brightness_mode       # 1 = adaptive, for R10
adb shell screencap -p /sdcard/panel.png                   # R6's composite, no meter needed
adb shell am start -n <applicationId>/app.gloam.ControlsActivity
```

**End of phase:** R11, on the API-33 AVD. Recreate with `emulator -avd gloam-api33 -no-window -gpu
swiftshader_indirect`. The emulator has no nits calibration at all — Phase 1's R10 settled that — so
nothing photometric is read there, and the phone stays the only place light is measured.

---

## 13. Tests

`PLAN.md` rule 3 promised three tests across the whole remaining roadmap and both remaining ones
belong to Phase 4 — the schedule window crossing midnight, and earlier-deadline-wins. **Phase 3
spends neither of those, and adds one the rule did not know about** (§6, rule 1), which is why §11
amends the rule rather than letting a fourth test appear quietly. The rest of what it adds is small
and should be described as such.

- **`PanelWindowFlagsTest`** (JVM, no Android). The mirror of `ShadeWindowFlagsTest`, and it asserts a
  *difference* rather than a property: `PANEL_WINDOW_FLAGS` carries `FLAG_NOT_FOCUSABLE` and **does
  not** carry `FLAG_NOT_TOUCHABLE`. The same `const val` inlining trick is what lets it run on the JVM
  without `android.jar` throwing.
- **`PanelWidthTest`** (JVM, no Android), and **this is the one that costs `PLAN.md` rule 3 a fourth
  test** rather than being free. `panelWidthPx(displayWidthPx)` is swept the way `ShadeRampTest`
  sweeps the ramp: across every plausible display width the answer is strictly narrower than the
  display, never narrower than a usable slider, and never `MATCH_PARENT`'s sentinel. That bound is
  what stands in for `FLAG_NOT_TOUCHABLE` on a window that catches touches, which is exactly the
  shape rule 3 reserves a test for — a pure function that fails in the direction the app exists to
  prevent. **An assertion that the params say `WRAP_CONTENT` would not have been that**: `WRAP_CONTENT`
  is the permissive value, and such a test passes identically whether the real bound is present or
  missing.
- **`PanelWindowTest`** (instrumented, beside `ShadeWindowTest`). The panel window reaches the window
  manager as `TYPE_APPLICATION_OVERLAY`, above the shade, at a size smaller than the display. The
  ordering half is the one only a device can answer, and it is the same assertion R5 takes by hand —
  written down so that a ROM update turning it over shows up as a red leg rather than as a support
  mail.
- **`TranslationTest`** covers this phase's strings continuously and the merge gate covers
  completeness. Neither needs anything added.
- **No test for `DimControls()`.** It is a composable with no logic in it — the extraction moved zero
  decisions — and a Compose UI test asserting that a slider passed a lambda calls the lambda is a test
  of Compose.

Everything else in this phase is device behaviour and belongs in §12.

---

## 14. The commit sequence

Conventional Commits, and each one leaves the app working. **`feat:` lines land in `CHANGELOG.md`
through release-please and `chore:` / `refactor:` / `test:` do not**, which is what decides the type
below rather than taste.

| Checkpoint | Commits |
| --- | --- |
| **A** | `chore: add a second overlay window to the debug section` — the bare rectangle R1 is read against. Then no commit at all: the verdict is a reading |
| **B** | `refactor: pull the dim controls out of the screen that owned them` — `DimControls()` and `AutoOffControls()`, `DimScreen` rewritten to call them. No behaviour change, nothing a release note would mention |
| **C** | `feat: add the compact controls` — `ControlsActivity`, `Theme.App.Controls`, the manifest entry, the permission/hatch guard and the *Open Gloam* way out. Then **the readings**, which are not a commit |
| **D** | `feat: open the compact controls from the notification and the icon` — the content intent's new target, which A's verdict decides between the panel and the activity; `launcher_compact` and its Settings row; `MainApplication`'s second startup read; and `MainActivity`'s launcher-only forward |
| **E** | `feat: say in the notification that the brightness slider is paused` — the content text, the `notify()` path and the derived `distinctUntilChanged` boolean that triggers it (§4). The observer is a separate `feat:` **only if the twelve ask for it**, and it does not land in this phase without them |
| **F** | `chore: add the panel window and its host` + `test: bound the panel window` — the window, `PanelHost`, the flags and `panelWidthPx`, wired to nothing. Then `feat: put the controls in a window above the shade` — the composition, the service's ownership, the close control and the inactivity timeout, which land **together** because a panel that appears before it can be dismissed is the trap this phase exists to avoid shipping |
| **G** | `docs: ...` — §11's edits, ADR-0010's fourth amendment, the panel ADR if there is one, and this file's readings block filled in |

**The ramp precedent holds twice here.** F's `chore:` lands the window and its bound wired to nothing
— the cheapest possible place to get a size rule wrong — and the `feat:` is the commit that puts a
user in front of it. And A lands its measuring apparatus as a `chore:` under the debug seam, the same
way Phase 1's backlight sweep and Phase 2's two-minute deadline button did, because in all three cases
only the app can do the thing to itself.

**If A is a no-go, the sequence is B through E and then G**, with D's content intent pointing at
`ControlsActivity`, and G's `docs:` commit is the one that records the cut. That is a phase closing
narrower, not a phase failing.

---

## 15. What G landed, and where it differs from §11

§11 said what the phase owed the documents before any of it was written. This says what was written,
because the two differ in three places and a plan nobody checks against the result is a wish.

- **`PLAN.md`** — the *Detail* lines were already there. G made §11's two corrections: 3a's paragraph
  now says the host is its own Activity and the launcher preference is inverted, with the
  `<activity-alias>` alternative and why it lost; 3b's carries **both** go/no-go questions, the
  verdict, and the field copy that is therefore not built. Rule 3's fourth test landed with the plan
  rather than here, as §11 said it would. **Status: 3a and 3b ticked.**
- **`CONTEXT.md`** — nothing owed. The *Compact controls* row was written when the phase was planned,
  on the axis §11 argued for: position, not size.
- **ADR-0010, fourth amendment** — the open question from *Consequences* is closed: the topmost
  window that asks owns the override, between two windows of ours as well. It also carries R13, which
  §11 could not have predicted because R1 had not been taken.
- **ADR-0011** — written, because §5 said go. *The panel is touchable, and its size is what keeps it
  from trapping the user.* The alternatives section is the one that matters: `WRAP_CONTENT` with a
  `widthIn(max)` inside the composition is the draft it replaces, and the reason it lost is that no
  test can see it.
- **`CLAUDE.md`** — the shade's house rule gains the second window and a link rather than a
  restatement; the layout block gains `ControlsActivity.kt`, `shade/PanelWindow.kt` and
  `ui/dim/DimControls.kt`. **One thing §11 did not list**: the debug-seam line said *the backlight
  sweep and the two-minute deadline button* and there are five buttons now — a stale line in the file
  every session reads is worth more than its size.
- **`DOD.md`** — Phase 3's record paragraph, the launcher-preference default opened as a rule-5
  question for the twelve, Phase 1's 3b bullet marked answered, and **one item §11 did not predict**:
  R13's ceiling on ultra dark, against Phase 2b, which is the one place in this phase's findings that
  changes what a later phase can promise.
- **`README.md`** — one paragraph, and it says *the controls come to where you are*, which is the
  phase in one line.
- **`docs/store-listing.md`** — 0.5.0's note in both locales, at 474 and 479 of 500. `notes-gate.py`
  now reports it written and waiting ahead of `versionName`, which is the state it is designed to
  pass.
- **`scripts/edge-to-edge.py`** — one scene, `compact-controls`, in a family of its own. It is
  reached **through the debug section** rather than through the icon, and both halves of that are
  forced by the platform: `ControlsActivity` is `exported="false"` so `am start -n` cannot touch it,
  and the icon route needs `CATEGORY_LAUNCHER`, which the driver's explicit-component relaunch does
  not carry — which is also what stops the launcher preference leaking into every later scene. The
  panel is not in that harness and cannot be; `PanelWidthTest` is the trade.

**What G did not do.** The scene above is written but not walked — the matrix is an emulator job and
this phase's device work went to R11, which is a different question about the same build. The first
nightly that runs it is the one that proves the route, and a scene that cannot be reached fails
loudly rather than silently.

## Kotlin and Android notes for this phase

- **A theme is read by the OS before any of this app's code runs.** `windowIsFloating` is not a
  property you set on a window, it is an attribute the platform reads out of the theme when the
  window is created — which is why §2's shapes are about *which theme is in the manifest* rather than
  about a runtime call, and why the starting window can disagree with what `setTheme()` later asks
  for. There is no JS analogue: nothing in a browser decides your document's shape from a
  declaration it read before your script existed.
- **`ComposeView` needs owners an Activity would have provided.** React's root needs nothing but a
  DOM node; Compose's root needs a lifecycle (to know when to recompose), a saved-state registry (to
  survive configuration changes) and, if anything asks for one, a `ViewModelStore`. An Activity is
  those three things wearing a screen. §7 builds them because there is no Activity, and the failure
  when the lifecycle is left below `STARTED` is not an error — it is a window that draws once and then
  ignores every state change.
- **`SavedStateRegistryController.performRestore(null)` must run before the lifecycle passes
  `CREATED`.** There is nothing to restore; the registry insists on being told so, and the exception
  it throws otherwise names neither the panel nor the reason.
- **`Lifecycle.State.DESTROYED` is terminal, not "hidden".** `LifecycleRegistry` has no upward event
  out of it and throws when asked for one, and `SavedStateRegistryController.performRestore` runs once
  per controller. A lifecycle host is therefore a **one-shot object**, which is why §7 builds one per
  summon instead of keeping one and toggling it. The JS instinct — an object with `show()` and
  `hide()` you call as often as you like — is exactly what does not survive here.
- **An `Intent`'s categories say how an Activity was entered.** A launcher tap carries
  `CATEGORY_LAUNCHER`; a `startActivity` from inside the app carries none. §2's forward tests for it
  rather than passing a "don't bounce me" extra, and the difference is between a rule that is correct
  by default for every future caller and one that is correct for as long as each of them remembers.
- **`FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE` are two properties, not one emphatic one.** Touchable
  is whether touches land on the window; focusable is whether key events and the IME do. The shade
  wants neither, the panel wants touches and not focus, and a window that confuses them either
  swallows the Back key or is a picture of a slider.
- **`BRIGHTNESS_OVERRIDE_NONE` is `-1f`, and it means *not asking* rather than *asking for nothing*.**
  That distinction is the whole of §5's second question: a window that declines should leave the next
  one down owning the panel, and "should" is why it is a reading.
- **A `ContentObserver` is not `addEventListener`.** It fires on a URI and tells you nothing about who
  wrote or what changed — and under adaptive brightness the framework writes to the same key the user
  does (Phase 1's R4). An observer cannot tell them apart, which is §4's whole difficulty and not a
  detail of it.
- **`PendingIntent`s are cached by `filterEquals`, which ignores extras.** Two intents differing only
  in their extras are the same pending intent; changing the target component is a different one.
  §3's edit is safe for that reason and a later extras-only edit would not be.
- **`collectLatest` is `switchMap`, not `forEach`**, and §8's timeout is the third place in this
  codebase that leans on it. A new emission cancels the block still running for the previous one,
  which is what re-arms a deadline — and the same property is why a write made *from inside* the
  collection can cancel itself halfway, which is what `NonCancellable` is for in `ShadeService`.
- **A `Service` is not a thread and now owns two windows.** Every callback runs on the main thread;
  both windows are added, updated and removed there, and the panel's composition runs there too. That
  is fine and it is also why the panel's content stays small: a heavy recomposition is a dropped
  frame on the shade as well.

---

## Readings block

Rule 3: filled in as the phase runs, from the device, with the command that read it. Not "it looked
right". Derivations are in §12 and are arithmetic, not observations.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | **Go/no-go: order and override** | debug second window over a live shade at dim 100; `dumpsys window windows`, `dumpsys display`, `screencap` | **Go, outcome one** (2026-09-02). Square is `Window #8`, shade `Window #9`, printed top-first — the second window is above. `mWindowManagerBrightnessOverride=0.01`, `…OverrideTag=io.github.srednimax.gloam.debug`, unchanged with the square up and declining a brightness; `NaN` once the shade stopped. `screencap` centre `(205, 1, 205)`, everything outside it under `18/255`. **Both our windows report an `alpha=0.8` the app never set** — §5, and R6 owns it |
| R2 | The floating host's shape | debug button, then `dumpsys window windows` for its bounds | **Floats, 960 × 2440 px = 320 × 813 dp** (2026-09-02), `(wrapxwrap)`, own task, nothing scrimmed behind it. Two corrections: AppCompat needs the **unprefixed** `windowNoTitle` or the window wears a *"Gloam debug"* title bar, and `am start -n` cannot reach an `exported="false"` activity — §2 |
| R3 | The compact host at dim 100 | shade live at 100 from the host itself; `screencap` shaded vs unshaded, `dumpsys display` | Panel `nits= 6.642711`, `reason=override(…gloam.debug)`. **Measured transmission 0.2393** (median of 17 640 bright-pixel samples, p10–p90 0.2362–0.2417) against a computed 0.05 — so the host is at **≈1.59 nits, not D2's 0.33**. `1 − 0.8 × MAX_SHADE_ALPHA` = 0.24 is R1's `alpha=0.8`, quantified. §0 |
| R4 | Notification plain tap, at maximum dim | tap the row, then `dumpsys window windows`, `dumpsys activity activities`, `screencap` | **The panel, and no Activity at all** (2026-09-03). Shade at dim 100 with the launcher in front: the tap added a second overlay window *above* the shade — `(0,36)(1098xwrap) ty=APPLICATION_OVERLAY fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN`, printed top-first at `#7` over the shade at `#8` — while `topResumedActivity` stayed `com.miui.home/.launcher.Launcher` and `mWindowManagerBrightnessOverride` stayed `0.01`. **No task switch and no Activity started**, which is what `getService` bought. `screencap` reads the panel band at a median 244/255 against 28/255 for the shaded launcher above it — §3's legibility improvement, measured. The collapsed row still carries no *Stop* action (Phase 1's R8), so this tap is the whole of what the row does |
| R5 | Both locales in a narrow floating window | `cmd locale set-app-locales`, screenshot both | **Chips wrap to three rows in both, clipped in neither** (2026-09-02) — `FlowRow` holds at 320 dp. The window is at its 2440 px ceiling in both: English fits with *Open Gloam* visible, **Polish overflows by ≈one line** and puts *Otwórz Gloam* below the fold until scrolled. §2 |
| R6 | The panel at dim 100, and its palette | `screencap` with the panel open, both themes | **Measured transmission 0.2411** (2026-09-03, median of 112 786 bright-channel samples, p10–p90 0.2378–0.2417) — R3 read 0.2393 on the surface *under* the shade, so the pair agrees, and both land on `1 − 0.8 × MAX_SHADE_ALPHA` = 0.24. **The panel is not dimmed by the shade above which it sits**: its primary reads `#FDB978` in dark and `#86531C` in light, byte-identical to `Color.kt`, where a shaded copy would read ≈`#3D2D1D`. 1098 × 1620 px in both themes. Light theme puts a near-white card on a maximally dim screen — safe only because the backlight override holds the panel at ≈6 nits. §0 |
| F1 | **A summon against a stopped app raised the shade** | tap the debug summon with the shade off; `dumpsys window windows`, `dumpsys display` | **Bug, found and fixed** (2026-09-03). From no service and no windows, one summon produced *two* overlay windows — the panel and the shade — and took the backlight override to 0.01, with `shadeIntent` still saying stopped, so the controls read *Start dimming* over an already-dark screen. `startForegroundService` creates the service rather than asking it, `onCreate` raised the shade unconditionally, and `showPanel`'s `shadeView == null` guard could never fail because `onCreate` had just filled that field. The stored intent decides now. Checkpoint D inherits this: a `PendingIntent` outlives its process, so a stale notification tap takes the same path |
| R7 | Touches: in the panel, and through beside it | drag the panel's slider; tap the app underneath | **Both** (2026-09-03). Dragging the panel's dim slider took it 100% → 33% and the panel repainted the new figure — §7's `RESUMED` trap is not tripped. A swipe *above* the panel scrolled the app underneath (14.0% of the region above the panel changed, bbox `(48,0)–(996,570)`), so the shade beside the panel still passes touch through. **The brightness override did not move with the dim level**: it stayed `0.01` across the drag. The observation stands; the explanation first written beside it does not — see the correction under this table |
| R8 | Rotation and locale change with the panel up | `settings put system user_rotation 1`, `cmd locale …` | **The panel survives both, and the rotation needed a fix** (2026-09-03). `settings put system user_rotation` does nothing on HyperOS; `wm user-rotation lock 1` is the one that turns the display. A window laid out from explicit pixels keeps them across a rotation, so the panel wore the width of whichever orientation it was summoned in: landscape gives 1200 px (the cap biting, correctly) and rotating to portrait left 1200 px on a 1220 px display — 10 px of screen either side of a *touchable* window. `onConfigurationChanged` re-measures now, and it reads 1098 → 1200 → 1098 across both turns. **The locale is not re-read**: switched to English under an open panel, the panel stayed Polish. Left alone — it is stale for at most the 30 s idle timeout and the next summon is correct, where the rotation case was a safety property. Polish fits the panel in both orientations, nothing clipped |
| R9 | Inactivity dismissal, and death with the shade | open the panel, leave it untouched; then stop the shade | **Both** (2026-09-03). Untouched, the panel lived **30.1 s** (poll granularity 2 s) and logged `panel idle for 30000ms, taking it down`; the shade survived it, `mWindowManagerBrightnessOverride` still `0.01`. The panel's own *Stop dimming* took both windows down and released the override to `NaN` (§8, third route). So does a process kill: `am force-stop` with both up went 2 overlay windows → 0 with nothing orphaned — the Xiaomi case. HyperOS never renders the notification's *Stop* action into the shade, so that route is unread |
| F2 | **A launcher tap on an existing task never calls `onCreate`** | `am start` with `CATEGORY_LAUNCHER`, preference on, task alive; `logcat -s ActivityTaskManager` | **Bug, found and fixed** (2026-09-03). `moveTaskToFront … result code=2` (`START_TASK_TO_FRONT`) — the instance is resumed, so §3's forward in `onCreate` could not run and the icon preference honoured a cold start and was silently inert for as long as the task survived, which is days. `launchMode="singleTop"` plus `onNewIntent` is the fix: re-read, `result code=2` now ends at `topResumedActivity=…/app.gloam.ControlsActivity`, and the cold path (`result code=0`) does too. **The same trace caught the guard working**: with the overlay op reset by `adb install -r`, one tap went `MainActivity` → `ControlsActivity` → `MainActivity` and **stopped there** — §2's ping-pong argument confirmed on the phone rather than argued, and the category test is what stopped it |
| R10 | `SCREEN_BRIGHTNESS` under adaptive, and the control-centre slider | poll `settings get system screen_brightness` at 10 Hz through a control-centre drag; `dumpsys display`'s *Automatic Brightness Controller State* with the override live and released | **The slider writes it, and while our override is live nothing else can** (2026-09-03). The control-centre slider is not a decoration: one drag took the setting 255 → 13 and a second 13 → 30, with Gloam's override unmoved at `0.01` throughout — the drag that does nothing, measured. **It writes once per gesture, not per pixel**: across a 2.1 s drag sampled every 101 ms the value changed exactly once, to its final figure. And the noise objection turns out not to apply: with the override up the controller reads `mState=AUTO_BRIGHTNESS_DISABLED`, `mLightSensorEnabled=false`, `mCurrentLightSensorRate=-1`, `mAmbientLuxValid=false` — the sensor is not sampled at all — against `AUTO_BRIGHTNESS_ENABLED`, `true`, `250` and `mAmbientLux=12.02` the moment the shade stops. 600 samples over 61.7 s under adaptive with the shade up: **zero writes**. Releasing the override produced one immediately (30 → 21), which is R4's phenomenon, and then 311 samples over 49.7 s at a steady 12.02 lux with none. §4 |
| R12 | **The notification line, and what it costs to post** | `dumpsys notification --noredact`; `logcat -b events` for `notification_enqueue`; `screencap` with the row open | **Both directions, one post each, and legible** (2026-09-03). Shade at dim 100 with the backlight toggle on: `android.title=(Screen dimmed)`, `android.text=(Your brightness slider is paused while Gloam is dimming)`. Toggling the backlight off gave **one** `notification_enqueue` and `android.text=null`, and back on one more and the line again; the whole 40% → 100% slider drag gave **none**. Starting the shade is two posts — `startForeground` without the line, then the transition with it. `NotificationShade` is `Window #4` above Gloam's `#7`, printed top-first, and the override stays `0.01` tagged ours with the row open: the text renders at **175/255**, which is HyperOS's own notification-body grey untouched, against **59/255** for `dim_backlight_hint`'s screen behind it (cream background at R6's 0.24 transmission) and 22/255 for that screen's own text. Wraps to two lines in English and in Polish, clipped in neither |
| R11 | API-33 AVD end-of-phase pass | `emulator -avd gloam-api33 -no-window` | **The whole phase on the floor, and it holds** (2026-09-03). Panel above shade, `panelWidthPx` at 972 of 1080, touches caught and passed, both dismissals, the notification line. See below |
| R13 | **The `alpha=0.8` R1 could not explain** | `settings put global maximum_obscuring_opacity_for_touch 0.5`, re-raise the shade, `dumpsys window windows` | **It is the platform's untrusted-touch cap, and the shade wears it** (2026-09-03). The window came back `alpha=0.5`, tracking the setting. **Added by R11 rather than planned** — the emulator printed the same unexplained 0.8 as the phone, which is what made it testable |

### R11 — the phase on the API level it is allowed to be worst on

`minSdk` is 33, so `gloam-api33` is the floor: what works here works everywhere Gloam ships. The pass
walked the phase end to end on the build that closes it, on an AOSP image that shares no vendor code
with the development phone.

**The two windows, printed top-first, from one `dumpsys window windows`:**

```
Window #5 ... mAttrs={(0,31)(972xwrap) gr=BOTTOM CENTER_VERTICAL ty=APPLICATION_OVERLAY fmt=TRANSLUCENT
              fl=NOT_FOCUSABLE LAYOUT_IN_SCREEN HARDWARE_ACCELERATED
              frame=[54,1781][1026,2243]
Window #6 ... mAttrs={(0,0)(fillxfill) ty=APPLICATION_OVERLAY fmt=TRANSLUCENT alpha=0.8 sbrt=0.026663352
              fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS HARDWARE_ACCELERATED
Window #9 ... io.github.srednimax.gloam.debug/app.gloam.MainActivity   ty=BASE_APPLICATION
```

- **The ordering is not a HyperOS accident.** Panel above shade above Activity, on a second ROM. R1's
  reading was taken against one vendor image and the insertion-order expectation it confirmed is
  still undocumented; this is the second independent device that behaves as §5 predicted.
- **`panelWidthPx` on a display it has never seen.** 972 px of 1080 — 54 px each side, the 5% inset —
  and the frame ends at 2243 with the navigation bar starting at 2274, so the 31 px gap is
  `PANEL_BOTTOM_MARGIN_DP` at 420 dpi and the panel is clear of the bar without adding its inset by
  hand. The flags are `PANEL_WINDOW_FLAGS` exactly: `NOT_TOUCHABLE` and `LAYOUT_NO_LIMITS` absent,
  which is ADR-0011 read off a window rather than off a constant.
- **`sbrt=0.026663352` at dim 40** with `top=0.37775588` off the debug readout. `shadeValuesFor`
  computes `0.026663` for those inputs, so the ramp is not approximately right on this leg — it is
  the same arithmetic to seven digits, on a device whose `float range=[null, null]` sends it down the
  integer-decode path.

**Touch, both directions, on the surface that is the phase's one deliberate safety reversal.** A
drag inside the panel took the dim level to 98 and the shade's override from `0.026663352` to `0.01`,
and the full app read **98%** back on its own slider — one preference, three hosts. A tap on the
*Dim* tab's own coordinates, which the panel covers, **did nothing at all** while the panel was up
and switched tabs the moment it closed. That is the property and its control in two taps: the panel
eats what is under it, and nothing else does.

**Both dismissals, and the shade outlives one of them.** The close control took the panel down and
left the shade running. Left alone, the panel died at **28.7 s** against `PANEL_IDLE_TIMEOUT_MS`, with
a 2 s poll and a stopwatch started at the tap rather than at the window — 30 s within the granularity
of the reading. Stopping the shade from the compact controls took **everything** down: zero overlay
windows owned by the package, nothing orphaned.

**And the two hosts, in one frame each.** `ControlsActivity` comes up resumed in its own task
(`t44`, beside `MainActivity`'s `t43`) with `(wrapxwrap)` and `fmt=TRANSPARENT` — R2's floating window
on a second ROM — and the screenshot is §0's whole argument in one image: the compact host renders as
dark brown under the shade while the panel, taken seconds earlier, is cream. The notification carries
`android.title=(Screen dimmed)` and `android.text=(Your brightness slider is paused while Gloam is
dimming)`, so checkpoint E is on the floor too.

### R13 — the `alpha=0.8` has a name, and Phase 2b inherits it

R1 found both of Gloam's overlay windows printing `alpha=0.8` in `mAttrs`, a `LayoutParams` field
**this app never sets**, and left it open. R11 found the same 0.8 on an AOSP emulator, which rules out
a vendor and makes it testable: if the number comes from the platform, changing the platform's number
should change the window's.

```
$ adb shell settings put global maximum_obscuring_opacity_for_touch 0.5
$ # stop the shade, start it again
$ adb shell dumpsys window windows | grep -o 'alpha=[0-9.]*'
alpha=0.5
```

**It is Android 12's untrusted-touch rule.** An app overlay that lets touches pass through may not
obscure what is under it by more than `maximum_obscuring_opacity_for_touch` — unset on both the phone
and the emulator, so both get the framework default of **0.8** — and the window manager applies the
cap by writing it into the window's alpha. Three things line up behind it and none of them is an
argument: the setting drives the number, the **panel** prints no alpha at all because it is the
window that *catches* touches rather than passing them, and R3 and R6's measured transmissions of
0.2393 and 0.2411 are `1 − 0.8 × MAX_SHADE_ALPHA` = 0.24 to three decimals.

**So the shade's real ceiling is not `MAX_SHADE_ALPHA`.** Gloam's own two bounds still hold and are
still the ones the tests keep, but the platform is holding a third and tighter one on top of them:
the darkest a touch-passing overlay can make the screen is 0.8 of what the composite computes. The
cap and this app's house rule agree — the window that passes touches is not allowed to hide what is
under it, which is the same sentence from two directions — and the agreement is a coincidence of
purpose worth not relying on.

**Phase 2b is where this bites.** *Ultra dark* is defined as going past `MAX_SHADE_ALPHA`, and a
window alpha the platform pins at 0.8 is not moved by raising a child's alpha at all: past a point,
more alpha buys nothing and the phase's own feature has a ceiling nobody had priced. The escape
routes are the backlight (already at `MIN_BACKLIGHT` there), the setting itself (a global the app
cannot write), or accepting the ceiling and saying so. **Recorded in `DOD.md` against 2b** rather
than solved here — this phase found it, it is not this phase's to spend.

### R7's second half, corrected — the override that did not move is the ramp working

The row above first carried the explanation *"`lowerBacklight` pins the backlight to `MIN_BACKLIGHT`
rather than following the ramp"*. **The observation is right and that sentence is not.**
`lowerBacklight` is a preference — whether Gloam may take the backlight down at all — and nothing in
`applyShadeValues` pins anything: the value handed to the window is `maxOf(top × light,
MIN_BACKLIGHT)` out of `shadeValuesFor`, recomputed on every change.

**What decides whether the override moves under a drag is where the breakpoint is, and ADR-0010's
central argument is that the breakpoint is not a constant.** It is `ln(ratio) / ln(ratio × span)`,
and it travels with the user's own brightness. On the development phone, whose float range tops out
near `0.5`, that puts it at **dim 56 for a user at their maximum** and at **dim 21 for one sitting at
the `screen_brightness = 13` R10 measured on the same day**. Above the breakpoint the backlight is at
`MIN_BACKLIGHT` *by construction* and the shade supplies the remainder — so a drag from 100 to 33
never leaves the shade's stretch, and an override that does not move is the ramp doing exactly what
it says.

**Read rather than argued** (R11, 2026-09-03, API 33): with `top = 0.37776` the breakpoint is dim 55,
the shade's window carries `sbrt=0.026663352` at dim 40 — `shadeValuesFor`'s own arithmetic to seven
digits — and a drag on the panel's slider to 98 took it to exactly `0.01`. Same expression, a device
whose top is nowhere near the floor: the override moves while there is something left to spend and
pins when there is not.

**What R7 is actually missing is one integer.** It did not record `settings get system
screen_brightness` at the moment it was taken, and that is the number that decides which of the two
cases it was. **Any reading of the override records the setting beside it** — the same shape of
lesson as ADR-0010's *read a panel with the screen held awake*, and the same cost when it is skipped:
a reading that is true and an explanation that is invented to fit it.

---

## Done when

- `DimControls()` and `AutoOffControls()` take state and callbacks and know nothing about `Scaffold`,
  the service or a `Context`, and `DimScreen` renders them without having lost the explainer, the
  warning banner or the resume re-read.
- The compact controls open from the notification, open from the icon when the preference says so,
  and hand the user to the full app when the overlay permission or the escape hatch is not there.
- The notification says, on a surface that can actually be read at 6.64 nits, that the user's own
  brightness slider is paused — and the `ContentObserver` question is with the twelve rather than
  with one person in a room.
- §0's ceiling is a measured pair of numbers rather than an argument: R3 and R6, same dim level, same
  room, one surface under the shade and one above it.
- Tapping *Open Gloam* in the compact host reaches the full app and stays there, and a revoked overlay
  permission sends the user to the full app **once** rather than bouncing them between two
  activities (§2).
- The readings block above has no dashes left in it.
- **And either:** the panel is up, catching its own touches and passing every other one through, dying
  on its close control, on its touch-idle timeout and with the shade, with `PanelWidthTest` holding
  its size bound and an ADR saying why a touchable overlay is allowed to exist at all —
- **or:** R1 vetoed it before anything had been built on the bet, F was skipped, `PLAN.md`'s Phase 3b
  is struck through with the reading that struck it, and this file says so here rather than leaving it
  to be argued about later.
