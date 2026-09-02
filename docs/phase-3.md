# Phase 3 — The controls, and the panel

**Two phases, one file.** `PLAN.md` splits Phase 3 along cost — 3a is a theme and an activity, 3b is
a hand-built Compose host in a raw window with a documented kill condition — and that split is right
and stays. What is wrong is planning them apart: **they are the same composable in two hosts**, 3b's
go/no-go is a reading that is cheapest to take while the phone is already set up for 3a, and the one
fact that decides how much either host is worth is a fact about both. Splitting the file would mean
writing that fact twice and then keeping two copies of it honest.

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

- **R8.** At maximum dim, `MainActivity` — with its *Stop dimming* button on it — is under the shade
  at `MIN_BACKLIGHT` × `(1 − MAX_SHADE_ALPHA)` = **0.33 nits**, while SystemUI's notification row
  above the shade stays at the full 6.64.
- **R9.** An ordinary app window that sets its own `screenBrightness` is *not consulted* while ours
  is up. MIUI's video player asked for `1.0` and then for `0.0078`; neither reached the panel.
  **It is the topmost window's value that applies, not the lowest.**
- **R5.** The system's own surfaces — notification shade, quick settings, volume dialog — read our
  `0.01` back unchanged. They are legible because the *shade's alpha* is not over them, not because
  they out-bid the override.

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

---

## What is in, and what is deliberately not

**In:** `DimControls()` and `AutoOffControls()`, extracted now that there is a second caller; the
compact controls as a floating host, with the three candidate shapes costed and one of them chosen on
a reading; the routes into that host — the ongoing notification's content intent, the launcher behind
a preference, and the hole 2b's tile fills; the `SCREEN_BRIGHTNESS` `ContentObserver` question, put
to the twelve in its strongest form rather than its easiest one (rule 5); 3b's go/no-go taken as a
measurement before any panel code exists; and, if it goes, the panel — a touchable overlay window
with the opposite safety rule from the shade, and the dismissal that keeps it from being a trap.

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
| **A** | `DimControls()` and `AutoOffControls()`, extracted | 3a | `refactor:` | nothing |
| **B** | The compact host, and the reading that picks its shape | 3a | `feat:`, then the readings | A |
| **C** | The routes in: notification, launcher preference, the tile's hole | 3a | `feat:` | B |
| **D** | The brightness-slider question | 3a | `feat:` (the notification line) — the observer only if the twelve say so | B, and the twelve |
| **E** | **The go/no-go** | 3b | `chore:` debug button, then no commit at all | B, phone attached |
| **F** | The panel: window, host, controls, dismissal | 3b | `chore:` + `test:`, then `feat:` | **E's verdict** |
| **G** | The documents | both | `docs:` | everything above |

**E is a gate, not a task, and it is this phase's checkpoint B.** Phase 1 carried one that could veto
the backlight half; this one can veto 3b outright. It is a reading taken against a *bare* second
window — no Compose, no controls, no lifecycle — because the question is about window ordering and
brightness ownership and nothing else, and every line of panel code written before the answer is a
line written on a bet. If E says no, F is skipped, the phase closes with 3a alone and §5 records the
cut here rather than leaving it to be argued about later.

**D is the other unusual one: it can close with no code at all.** `PLAN.md` rule 5 names the
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
wrapping in a narrow floating window; that is a layout finding, and R4 is where it gets read rather
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

**What this host is not:** an escape hatch. It fails clause 2 of `phase-2.md` §2's definition —
reaching it needs sight of Gloam's own UI, and §0 is why that is not fixable. `EscapeHatch.kt`'s
inventory does not gain a row, and this file is where somebody who assumed it would goes to find out
why.

---

## 3. The routes in: the notification, the launcher, and the tile's hole

Three doors, and only one of them is new machinery.

**The notification's content intent moves from `MainActivity` to `ControlsActivity`.** It is one
`PendingIntent` target in `ShadeService.buildNotification()`. Phase 1's R8 found that on HyperOS a
plain tap on the notification row follows the content intent — the *Stop* action is behind a
long-press and is not in the collapsed row at all — so today a plain tap lands on the full app under
the shade. After this it lands on the compact host under the shade. **That is a reach improvement and
not a legibility one** (§0), and R3 reads it end to end at maximum dim so the difference is on the
record rather than assumed.

**Kotlin/Android note on changing it.** `PendingIntent`s are cached by requesting identity, and two
`Intent`s that are `filterEquals` — same action, data, type, component, categories — are the same
pending intent regardless of their extras. Changing the *component* changes `filterEquals`, so this
particular edit gets a fresh one for free. If a later change is extras-only, it needs
`FLAG_UPDATE_CURRENT` beside `FLAG_IMMUTABLE` or the old extras survive an app update, silently.

**The launcher route** is shape iii from §2: `MainActivity.onCreate` reads the preference and
forwards. It needs a value *before* the first frame, which is a `suspend` read, which the app already
does once — `MainApplication` reads `themeModeNow()` before any Activity exists and hands it over as
`startupThemeMode`. The compact preference joins it there. **One blocking read at startup, not two:**
`store.data.first()` reads the file once and both values come off the same snapshot, the same
argument `ShadeIntent` makes in `AppPreferences`.

**The tile is 2b's and the hole is one line.** `PLAN.md` gives the compact controls two doors and
this phase can only build one of them. A `TileService`'s click either sends the user to an activity
or toggles the shade; 2b's tile is safety equipment and its primary job is stopping the shade, so the
integration this phase asks for is small and stated here so 2b does not have to rediscover it:
**2b's tile, whatever else it does, launches `ControlsActivity` for its long-press / "open app"
route** rather than `MainActivity`. If 2b ships first, that is the line. If 3a ships first, 2b's
default already points at `MainActivity` and moving it is a one-word edit.

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

- It still needs manual mode, for R4's reason. Nothing fixes that; the setting is the same integer.
- It makes a *system* control do something the system did not promise, on a screen where the user
  cannot see which app is responsible. A user who then stops the shade and finds their brightness
  somewhere they did not put it has been surprised by an app that was not on screen.
- It is unreadable from the outside: no reading can tell a user's drag from another app's write.
- And it is exactly what `PLAN.md` means by *too clever*, which is why rule 5 sent it to the twelve
  rather than settling it here.

**Recommendation: ship the cheap half in this phase, build the reading, ship the observer only if the
twelve ask for it.**

The cheap half is that the explanation moves to a surface that can be read. The ongoing
notification's text is already updated by the service on every value change it collects, it is drawn
above the shade, and it is where the user already looks. While the override is live it says so, in
one line, in both locales. That fixes the *"my phone is broken"* reading of the symptom without
reinterpreting anything, costs one string and one `notify()`, and is true on every device and in both
brightness modes.

The reading is **R10**: how noisy is `SCREEN_BRIGHTNESS` under adaptive on this ROM, and does
HyperOS's control-centre slider write it at all? Both halves are cheap while the phone is already
attached for this phase, and together they decide whether the strong form is even available here. If
the twelve say yes, the observer is then a small, already-measured piece of work rather than an
investigation.

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
`BRIGHTNESS_OVERRIDE_NONE` is the `LayoutParams` default and means *not asking*, so by R5's and R9's
rule the shade — the topmost window that *does* ask — should keep it. Three outcomes and only one of
them is a cut:

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

**If it is a no-go, what the phase closes as.** 3a alone, with §0's paragraph promoted from an
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

1. **Never `MATCH_PARENT`, in either axis.** `WRAP_CONTENT` width and height, bottom-anchored with
   `Gravity.BOTTOM`, with a `Modifier.widthIn(max = …)` inside the composition so a long translated
   chip label cannot grow the window to the full display. This is the one thing in the panel that a
   JVM test can hold to account, and §13 writes it.
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
    fun hide() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
}
```

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
   every value the panel writes, taking the panel down when it expires. It is the panel's version of
   auto-off and it is here for the same reason: a control surface that only closes when you
   successfully press a button on it is a control surface that traps you when the button does not
   draw.
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
what the Settings screen collects; the `suspend` one-shot is what `MainApplication` needs before any
Activity exists, for the same reason `themeModeNow()` exists — a value that arrives after the first
composition is a value that arrives a frame too late, and here that frame is a whole activity launch.

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
  place of its one, and its result, whichever way it goes.
- **`CONTEXT.md`** — **one row**. The existing **Shade** and **Panel** rows are correct and stay; what
  is missing is the third surface, and three things that can all be called "the controls" is exactly
  the collision this file exists to prevent:

  | Term | Means | Not |
  | --- | --- | --- |
  | **Compact controls** | The small, dialog-shaped Activity carrying `DimControls()`. Reached from the notification, the tile, or the icon. **Below the shade, like every Activity** | Not the panel, which is an overlay window *above* the shade. Not "compact mode" — there is no mode |

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
1's are in `phase-1.md`, Phase 2's in `phase-2.md`.

**Read with the screen held awake wherever brightness is involved.** `settings put system
screen_off_timeout 600000` first, and put it back after. Four of Phase 1's readings were first taken
inside the inactivity timeout, which pins the panel to its floor and looks exactly like *the setting
did nothing* — the correction is on ADR-0010's third amendment and it cost a withdrawn reading.

| # | Reading | Decides |
| --- | --- | --- |
| **R1** | The floating host's shape: does §2's theme produce a floating, correctly-sized window, and does shape iii's forward flash | **checkpoint B's shape**, and whether the fallback is needed |
| **R2** | **The compact host's legibility at dim 100**, under a live shade, dark room | §0's claim, re-read against the new host rather than inherited from R8's `MainActivity` |
| **R3** | The notification's **plain tap** on HyperOS, at maximum dim, end to end | §3 — whether the route change is a reach improvement, honestly stated |
| **R4** | The compact host in both locales, and the five auto-off chips in a narrow floating window | §1's recommendation on whether the deadline travels, and whether Polish wraps it |
| **R5** | **The go/no-go, both questions** (§5): is our bare second window above the shade in `dumpsys window windows`, and does `mWindowManagerBrightnessOverride` still read the shade's `0.01` | **checkpoint E's verdict.** F does not start until this has a result |
| **R6** | The panel drawn at dim 100: its own nits, the content's nits, and its palette against the app's | that the panel is the legible surface §0 says it is — the whole case for 3b |
| **R7** | The panel catches touches **and the shade still passes them**: a slider moving under a finger inside the panel, a tap landing in the app outside it | §6, and §7's `RESUMED` trap, which is invisible any other way |
| **R8** | The panel across a rotation and a locale change | §7's configuration note, and the size bound under a re-measure |
| **R9** | The panel's inactivity dismissal, and that stopping the shade takes it down | §8's three routes out |
| **R10** | **`SCREEN_BRIGHTNESS` noise under adaptive**, and whether HyperOS's control-centre slider writes it | §4 — whether the strong form is even available on this ROM |
| **R11** | **API-33 AVD end-of-phase pass** (ADR-0008): launches, compact host appears, shade appears, panel appears if 3b went | the phase's own closing gate |

**R5 is the only reading in this file that another reading cannot be substituted for.** Everything
else here is confirmation of something already argued; R5 decides whether half the phase exists. It
comes before F and it is taken against the debug button in §5, not against panel code.

**R2 and R6 are the same reading twice and that is the point.** One control surface under the shade,
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

`PLAN.md` rule 3 promises three tests across the whole remaining roadmap and both remaining ones
belong to Phase 4 — the schedule window crossing midnight, and earlier-deadline-wins. **Phase 3
spends neither**, and what it adds instead is small and should be described as such.

- **`PanelWindowFlagsTest`** (JVM, no Android). The mirror of `ShadeWindowFlagsTest`, and it asserts a
  *difference* rather than a property: `PANEL_WINDOW_FLAGS` carries `FLAG_NOT_FOCUSABLE` and **does
  not** carry `FLAG_NOT_TOUCHABLE`, and the panel's `LayoutParams` are `WRAP_CONTENT` in both axes —
  never `MATCH_PARENT`. The same `const val` inlining trick is what lets it run on the JVM without
  `android.jar` throwing. **It is the only mechanical guard on §6's rule**, and it is worth having
  precisely because a size is a weaker guarantee than a flag.
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
| **A** | `refactor: pull the dim controls out of the screen that owned them` — `DimControls()` and `AutoOffControls()`, `DimScreen` rewritten to call them. No behaviour change, nothing a release note would mention |
| **B** | `feat: add the compact controls` — `ControlsActivity`, `Theme.App.Controls`, the manifest entry, the permission/hatch guard and the *Open Gloam* way out. Then **the readings**, which are not a commit |
| **C** | `feat: open the compact controls from the notification and the icon` — the content intent's new target, `launcher_compact` and its Settings row, `MainApplication`'s second startup read, `MainActivity`'s forward |
| **D** | `feat: say in the notification that the brightness slider is paused` — one string, one `notify()`. The observer is a separate `feat:` **only if the twelve ask for it**, and it does not land in this phase without them |
| **E** | `chore: add a second overlay window to the debug section` — the bare rectangle R5 is read against. Then no commit at all: the verdict is a reading |
| **F** | `chore: add the panel window and its host` + `test: bound the panel window` — the window, `PanelHost`, the flags and the size bound, wired to nothing. Then `feat: put the controls in a window above the shade` — the composition, the service's ownership, the close control and the inactivity timeout, which land **together** because a panel that appears before it can be dismissed is the trap this phase exists to avoid shipping |
| **G** | `docs: ...` — §11's edits, ADR-0010's fourth amendment, the panel ADR if there is one, and this file's readings block filled in |

**The ramp precedent holds twice here.** F's `chore:` lands the window and its bound wired to nothing
— the cheapest possible place to get a size rule wrong — and the `feat:` is the commit that puts a
user in front of it. And E lands its measuring apparatus as a `chore:` under the debug seam, the same
way Phase 1's backlight sweep and Phase 2's two-minute deadline button did, because in all three cases
only the app can do the thing to itself.

**If E is a no-go, the sequence ends at D and G**, and G's `docs:` commit is the one that records the
cut. That is a phase closing narrower, not a phase failing.

---

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
| R1 | The floating host's shape | launch it, `dumpsys window windows` for its bounds | — |
| R2 | The compact host at dim 100 | shade live at 100, open the host, `screencap` + `dumpsys display` | — |
| R3 | Notification plain tap, at maximum dim | tap the row by hand, then `dumpsys activity activities` | — |
| R4 | Both locales in a narrow floating window | `cmd locale set-app-locales`, screenshot both | — |
| R5 | **Go/no-go: order and override** | debug second window, `dumpsys window windows` + `dumpsys display` | — |
| R6 | The panel at dim 100, and its palette | `screencap` with the panel open, both themes | — |
| R7 | Touches: in the panel, and through beside it | drag the panel's slider; tap the app underneath | — |
| R8 | Rotation and locale change with the panel up | `settings put system user_rotation 1`, `cmd locale …` | — |
| R9 | Inactivity dismissal, and death with the shade | open the panel, wait; then stop the shade | — |
| R10 | `SCREEN_BRIGHTNESS` under adaptive, and the control-centre slider | observe the setting while the room changes and while dragging | — |
| R11 | API-33 AVD end-of-phase pass | `emulator -avd gloam-api33 -no-window` | — |

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
- §0's ceiling is a measured pair of numbers rather than an argument: R2 and R6, same dim level, same
  room, one surface under the shade and one above it.
- The readings block above has no dashes left in it.
- **And either:** the panel is up, catching its own touches and passing every other one through, dying
  on its close control, on its timeout and with the shade, with `PanelWindowFlagsTest` holding its
  size bound and an ADR saying why a touchable overlay is allowed to exist at all —
- **or:** R5 vetoed it, F was skipped, `PLAN.md`'s Phase 3b is struck through with the reading that
  struck it, and this file says so here rather than leaving it to be argued about later.
