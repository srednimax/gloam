# The panel is touchable, and its size is what keeps it from trapping the user

## Context

`CLAUDE.md` calls one property of the shade load-bearing: it carries `FLAG_NOT_TOUCHABLE` and
`FLAG_NOT_FOCUSABLE`, so every touch passes through to whatever is underneath. Without them the phone
appears frozen — a window over every other app that also eats every touch is indistinguishable from a
crash, and the way out is behind the thing you need to get out of.

Phase 3b adds a **second** overlay window, the **panel**, and it cannot have that property. A control
surface the user cannot touch is a picture of a control.

**The reason it is worth reversing a load-bearing rule is a pair of measurements, not a preference.**
At dim level 100 the shade transmits 0.24 of what is under it (`phase-3.md` R3, median of 17 640
bright-pixel samples; R6 read 0.2411 independently and agreed). Everything Gloam hosts in an Activity
— the full app, and the compact controls 3a shipped — is *below* that window by construction: every
Activity in Android sits under `TYPE_APPLICATION_OVERLAY`, and no flag, theme or `LayoutParams` field
moves it. So the app's own controls are at **≈1.59 nits** while the panel, being a sibling overlay
above the shade, is at the **6.64 nits** the backlight override left (R6: its palette comes back
byte-identical to `Color.kt`, where a shaded copy would read ≈`#3D2D1D`). One of those two surfaces
can be read at maximum dim and the other cannot.

`PLAN.md` gives the panel one reason to exist beyond that: the **live preview**, a slider that moves
the dim over the content the user is actually reading rather than over Gloam's own screen. That, too,
requires being above the shade and requires catching a finger.

So the question this ADR answers is not *should the panel be touchable* — it must be — but **what
stands in for the flag once it is gone.**

## Decision

**The panel drops `FLAG_NOT_TOUCHABLE` and keeps everything else**, and its **size** is the bound
that replaces the flag.

```kotlin
const val PANEL_WINDOW_FLAGS =
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
```

- **`FLAG_NOT_FOCUSABLE` stays.** *Touchable* and *focusable* are two properties and are routinely
  confused: touchable is whether touch events land on the window, focusable is whether it takes input
  focus and the key events and IME that come with it. A focusable panel would take the Back key away
  from the app underneath and could raise a keyboard for a field it does not have.
- **`FLAG_LAYOUT_NO_LIMITS` is absent**, unlike the shade's. The shade extends past the system bars
  because a bright strip across the top of a dimmed screen reads as a bug; the panel is sized to its
  content and has no business outside the display's bounds.

**The bound is four rules, and the first is the one a test can see:**

1. **The window is added with an explicit pixel width — never `MATCH_PARENT`, never `WRAP_CONTENT`.**
   `panelWidthPx(displayWidthPx)` takes 5% off each side, caps the result at `PANEL_MAX_WIDTH_PX`
   (1200 px) and floors it at 1, so the answer is strictly narrower than the display at every display
   size. Height is `WRAP_CONTENT` and gravity is `BOTTOM`, because a bounded height clips a control
   and an unreachable close button is the trap this whole ADR is about.
2. **It cannot outlive the shade.** `ShadeService` adds and removes both windows; `onDestroy` takes
   both down. There is no state in which the panel is up and the shade is not.
3. **It has three ways out, ordered so that each one covers the failure of the one above it**
   (`phase-3.md` §8): the close control drawn in the panel; a **30-second inactivity timeout**
   (`PANEL_IDLE_TIMEOUT_MS`) owned by the service and re-armed from `dispatchTouchEvent` on the host's
   root view, which is the only place that sees the touches a child consumes; and the shade coming
   down by any of its own routes, which takes the panel with it.
4. **The width is re-measured on a configuration change.** A window laid out from explicit pixels
   keeps those pixels across a rotation, which is a bug the size rule creates and does not catch.

**`panelWidthPx` is a pure function and `PanelWidthTest` sweeps it**, which is what puts it in
`PLAN.md` rule 3's short list beside the ramp: device behaviour is proven by measurement, and the
pure functions that compute or bound a *safety* value are proven by test. It is the fourth such test
in the roadmap.

**The panel asks for no brightness of its own.** `screenBrightness` stays `BRIGHTNESS_OVERRIDE_NONE`,
so by the rule ADR-0010's fourth amendment records, the shade below it keeps the override. One dim
level, one ramp, in a fixed order — a second window deriving a second brightness would be a second
ramp wearing a window's clothes.

## Alternatives

**Keep `FLAG_NOT_TOUCHABLE` and let the compact controls be the answer.** This is 3a, it shipped, and
it is not a substitute: it is an Activity, so it is under the shade at 1.59 nits (R3), and a
translucent activity in its own task does not reliably keep another app's screen visible behind it —
so the live preview would be judged against Gloam's own UI, which is dark where the content is not.
3a is the cheap door; it is not this.

**`WRAP_CONTENT` width with a `Modifier.widthIn(max = …)` in the composition.** This was the draft and
it is the wrong guarantee. The risk being bounded is a long translated label growing the window until
it covers the display, and `WRAP_CONTENT` hands exactly that decision to the content: the bound then
lives inside the composition, where no JVM test can see it and the window manager does not enforce
it. A width the window is *added with* cannot be exceeded by a translation at all. It is also the
better layout — `WRAP_CONTENT` sizes a slider to its intrinsic minimum, which for a panel that is
mostly sliders is a panel of stubs.

**A full-width window with only part of it touchable.** There is no app-level API for it: touch
regions on an overlay are a system-window facility, and the app-visible knob is the flag, which is
all-or-nothing. The size *is* the region.

**Dismiss on tap-outside** (`FLAG_WATCH_OUTSIDE_TOUCH`). Wrong gesture for this window specifically:
the panel sits over another app's UI, so a dismissal gesture that is also a tap on that app is a
gesture whose two effects the user cannot separate — they close the panel and follow a link.

**Arm the timeout from values written rather than touches.** It inverts the panel's own purpose:
judging a dim level means setting it and then *looking*, which writes nothing, so a user who drags
once and studies the result would lose the panel mid-judgement. Touch-armed also keeps the recovery
property that matters most — a panel drawn off-screen by a layout bug receives no touches either, so
it still dies on schedule.

**Let the panel arm its own timeout.** If the composition fails to draw, the close control is gone
and a timeout the composition owns is gone with it. The `delay` runs on the service's scope, on a
clock the panel cannot influence, for exactly that case.

## Consequences

- **A rectangle of the screen swallows touches while the panel is up**, over an app the user did not
  choose to interrupt. That is accepted, and it is bounded in two directions: never wider than 90% of
  the display, never longer than 30 seconds untouched. R7 read both halves on the phone — the panel's
  own slider moved, and a swipe beside the panel scrolled the app underneath.
- **A safety property is now a computation rather than a constant**, which is a genuinely weaker
  guarantee. `PanelWidthTest` is the compensation and it is a merge gate; a change to `panelWidthPx`
  that widens it past the display fails on the JVM rather than on somebody's phone.
- **The size rule creates a rotation bug and R8 found it.** Summoned in landscape and rotated to
  portrait, the panel kept 1200 px on a 1220 px display — 10 px of screen either side of a *touchable*
  window. `onConfigurationChanged` re-measures now (1098 → 1200 → 1098 across both turns). Any future
  `LayoutParams` value derived from the display inherits this hazard.
- **The panel's locale is stale until it closes.** The same handler deliberately does not re-read
  strings: it is wrong for at most the 30-second timeout and the next summon is correct, where the
  rotation case was a safety property rather than a cosmetic one.
- **Two overlay windows now depend on their ordering**, which nothing documents — insertion order is
  an expectation, not a guarantee. R1 read it and R4 read it again through the notification route. A
  ROM that reorders them puts the panel under the shade, at which point it is 3a with more machinery;
  a ROM that lets the topmost window take the override even when it declines one needs the field copy
  `phase-3.md` §6 keeps written down and unbuilt.
- **`CLAUDE.md`'s house rule is now two rules, not one.** Anyone reading `PANEL_WINDOW_FLAGS` next to
  `SHADE_WINDOW_FLAGS` sees one missing flag and no reason; this ADR is the one hop that explains it,
  and a `MATCH_PARENT` appearing in the panel's params is a bug rather than a simplification.
