# One dim level drives both mechanisms, in a fixed order

## Context

Gloam subtracts light two ways, and `CONTEXT.md` has always insisted they are different things: the
**backlight** is Android's value, the **shade** is ours. Phase 1 has to decide whether the user sees
that distinction, because `CONTEXT.md` also says the dim level is *"the single number the user moves.
The one value the product is about."* Those two sentences are in tension the moment the backlight is
actually driven.

Measured on the phone (Redmi `amethyst`, HyperOS, 2026-08-30):

- `WindowManager.LayoutParams.screenBrightness` on the shade window takes the panel from **500 nits
  to 6.64 nits**, and costs **no permission at all**.
- The display's own floor is **2.0 nits**. The system reverts the override when the window goes away,
  so a ROM kill cannot strand anyone dark.
- **The user's own brightness slider goes inert while the override is live** — the stored value moves,
  the applied brightness does not, and whatever they set lands the moment the shade comes off.

So the two mechanisms are asymmetric in range (a factor of ~75 versus the last stretch below the
floor) and asymmetric in side effects (one makes a system control appear broken; the other does not).

## Decision

**One dim level, one ramp, in a fixed order.** The first stretch of the slider walks the backlight
down from wherever the user left it to `MIN_BACKLIGHT`; the remainder raises the shade from fully
transparent to `MAX_SHADE_ALPHA`.

**The backlight half gets a toggle, not a second slider** — *"also lower the screen brightness"*,
defaulting on — for the user who would rather keep their system slider live.

**`MIN_BACKLIGHT` is a safety constant, not a preference**, exactly like `MAX_SHADE_ALPHA` and for
the same reason read from the other end: `LayoutParams.screenBrightness` documents `0.0f` as
`BRIGHTNESS_OVERRIDE_OFF`, which turns the backlight *off*. A ramp that reaches zero hands the user a
black screen with a live touchscreen — the precise failure `MAX_SHADE_ALPHA` exists to prevent,
arrived at from the opposite direction.

**100 means "as dark as currently allowed", not a fixed quantity of light.** Phase 2's ultra dark
therefore extends the top of the same slider rather than adding a second control, and the number does
not jump when it is enabled.

## Alternatives

**Two controls — a dim level and a backlight slider.** The honest case for it is that these really
are different mechanisms with different failure modes, and `CONTEXT.md` keeps them apart everywhere
else. It loses because users arrive with **one** question, and the answer to "how dark?" should not
require understanding which of two subsystems is currently doing the work.

**Both mechanisms moving together across the whole range.** Rejected because sequencing buys
something concrete: at moderate dim levels the shade is fully transparent, so there is no overlay
over the user's content at all and colours are untouched.

**`Settings.System.SCREEN_BRIGHTNESS` via `WRITE_SETTINGS`.** Rejected on the measurement: it mutates
the phone's *saved* brightness, so a ROM kill strands the user at minimum brightness with no Gloam
alive to restore it. The window override has no such failure mode and needs no permission.

## Consequences

- `CONTEXT.md`'s definition of dim level survives unamended. The vocabulary keeps `backlight` as
  Android's value; it is now the first half of the ramp rather than a word nothing reads.
- **The user's brightness slider is deferred, not broken, while Gloam runs.** That is inherent to the
  mechanism rather than a defect introduced here, and it is a thing the UI has to say out loud.
- The shade window's `screenBrightness` is now load-bearing state, which raises a question Phase 3
  must answer before the panel ships: when a second window appears above the shade, **whose override
  applies?** Measured in Phase 1, not discovered in Phase 3.

Amendment, 2026-08-30: three things this decision left unassigned now have owners, settled in a
grilling pass over `docs/PLAN.md`.

**The safety invariant moves from a `View` to the composite.** `MAX_SHADE_ALPHA` and `MIN_BACKLIGHT`
were reasoned about above as bounds on a single value each, which was true while the shade was one
black `View`. Phase 1 makes it a `FrameLayout` with two children, and a bound on each child does not
bound the result: black at `MAX_SHADE_ALPHA` still leaves content faintly visible, and a heavy amber
wash over that remainder is a screen nothing underneath can be read through, reached with neither
child past its own cap. The escape hatches survive either way, since all three sit above
`TYPE_APPLICATION_OVERLAY`; what is lost is the user's ability to see their own screen well enough to
reach any of them, which is the whole thing the cap buys. So the warmth layer gets its own constant
beside the other two, and the invariant is stated over the composite. `CLAUDE.md`'s house rule and
`CONTEXT.md`'s claim that warmth is *independent* of dim level are amended with it — warmth is a
separate control, but not an unbounded one.

**"The UI has to say out loud" belongs to Phase 1**, the phase that causes it. A user whose
brightness slider stops applying will report that Gloam broke their phone, not that a window override
is live, and nothing today connects that symptom to the *also lower the screen brightness* toggle
that ends it. Whether Gloam should go further and watch `SCREEN_BRIGHTNESS` with a `ContentObserver`
stays open, in Phase 3a, answered by the closed testers rather than by one person in a room.

**The ramp is unit-tested.** It is a pure function with no Android in it, and its failure mode is the
one this ADR exists to prevent: `0.0f` is `BRIGHTNESS_OVERRIDE_OFF` at one end and a fully opaque
screen at the other. Across all 101 inputs the ramp must never emit a backlight below `MIN_BACKLIGHT`,
never a shade alpha above `MAX_SHADE_ALPHA`, and never a composite past the warmth bound. The plan's
old blanket claim that no risk in it was reachable by a unit test was wrong here, and wrong in the
two places that matter most.

Amendment, 2026-08-30 (second). **Three of this ADR's own numbers did not survive being re-read
against the phone** during the Phase 1 planning pass. The decision stands; its arithmetic does not.

**The backlight's range is not a property of the device.** This ADR called the asymmetry *"a factor
of ~75"*. `dumpsys display` publishes the panel's calibration — `mBacklight = [6.83661E-4, 0.499951,
0.99975586, 1.0]` against `mNits = [2.0, 500.0, 1200.0, 2000.0]` — so the ratio between the user's
brightness and the floor is **user state**, running from 1 (already at minimum) to 1000 (at full) on
this one panel. The *"500 nits"* above was the user's setting at the time of the reading, not the
panel's maximum, and *"6.64 nits"* was a test float rather than a floor: `mScreenBrightnessRangeMinimum`
is `6.83661E-4`, which the spline maps to exactly the 2.0-nit floor. The override can reach it.

The consequence is in [`phase-1.md`](../phase-1.md) §2 rather than here: the split between the two
stretches cannot be a constant. It is derived from the backlight top, so that a user already at
minimum brightness does not get a slider whose first two-thirds do nothing.

**`Settings.System.SCREEN_BRIGHTNESS` is not a usable reading of the user's brightness.** With the
screen on and the mode manual, this phone reports `screen_brightness = 255` while `mScreenBrightness`
is `6.83661E-4` — the panel at its 2.0-nit floor. The obvious `raw / 255` computes a top of `1.0f`,
which is 2000 nits, for a user sitting at 2.0. A thousandfold overestimate, in the direction that
brightens the screen. `screen_brightness_float` reads `null` here, so there is no free float read
either. The integer's scale is device-defined and the encoding is gamma; neither has a public API.

**This does not reverse the decision.** `WRITE_SETTINGS` would inherit the same scale problem *and*
keep the stranding failure it was rejected for, so the window override remains the right mechanism.
What it changes is the confidence: the backlight half now depends on an estimate that has to be
verified against the device before it ships, which is why Phase 1 carries an explicit checkpoint that
can veto it and close the phase with the shade-only ramp instead.

Amendment, 2026-08-31 (third). **The phase ran, and the checkpoint that could have vetoed the
backlight half did not.** The decision is unchanged and now shipped; what changes is that the second
amendment's central reading turned out to be an artifact, and that this ADR's one open question has
an answer.

**Checkpoint B passed, so the backlight half is in.** Computed `backlightTop` against the panel's own
`mScreenBrightness` at `screen_brightness` 10, 20, 64 and 128 as well as 255, mode manual: the decode
lands **1.2% to 5.0% under** the user's own float and never over it — which is the safe direction,
because reading low can only make Gloam start darker than the user was. With `screen_brightness_mode`
set to `1` deliberately, adaptive turns out to be an ordinary read: the framework stores its own
choice back into the same integer, and the decode was 3.8% under at 6.3 lux. **No `null` path is
needed on this device**, and the toggle-off case shares the branch with it rather than having one of
its own.

**The second amendment's `screen_brightness = 255` reading is withdrawn.** The panel was in its
*inactivity DIM policy* at the time, not at the user's setting — `mBrightnessReason=manual [ dim ]` -
so the 255 and the `6.83661E-4` were never two views of the same moment. On this phone raw 255 is 500
nits and the two never coincide. So `Settings.System.SCREEN_BRIGHTNESS` **is** a usable reading of
the user's brightness after all, within a few percent and in the conservative direction.
**And the lesson that outlives the correction: read a panel with the screen held awake**
(`settings put system screen_off_timeout 600000`, and put it back). Four of Phase 1's readings were
first taken inside the inactivity timeout, and every one of them looked exactly like *the setting did
nothing*.

**Nothing downstream of that withdrawal moves.** The ramp still derives the split between its two
stretches from `backlightTop` rather than from a constant, because that argument was never about the
integer being unreadable — it was about the *range* being user state, running from 1 to 1000 on one
panel, which the withdrawal does not touch. `MIN_BACKLIGHT` is `0.01f`, set by measurement rather
than by argument: **6.64 nits** on the development panel, read against the criterion that the
notification shade can be pulled down and *Stop* tapped in a dark room and a lit one.

**The open question in *Consequences* — "when a second window appears above the shade, whose override
applies?" — is measured.** From both sides, and it is the same rule: **the topmost window that asks
for a brightness gets it, and a window that is hidden asks for nothing.** The system's own surfaces
(notification shade, quick settings, volume dialog) read our `0.01` back unchanged. An ordinary app
window below ours is not consulted at all — MIUI's video player in swipe-to-dim asked for `1.0` and
then for `0.0078`, either side of ours, and neither reached the panel; it is the topmost value that
applies, not the lowest. The two exceptions are both the system taking the window away rather than
out-ranking it: the **keyguard** releases the override outright and returns it on unlock, and a
**runtime permission dialog** force-hides every non-system overlay, which hands the user's own
brightness back for exactly as long as the dialog is up.

**What that leaves for Phase 3b is narrower than it was, but it is not answered.** What was measured
is a requester *below* ours losing. A panel window of Gloam's own *above* the shade that sets no
brightness of its own should leave the shade the topmost requester by the same rule — but "should, by
the rule" is not a reading, and 3b owns taking it.

Amendment, 2026-09-03 (fourth). **The open question is closed by a reading, and the answer is the
same rule.** *Consequences* left one question for Phase 3 — *"when a second window appears above the
shade, whose override applies?"* — and the third amendment narrowed it to Gloam's own windows and
refused to answer it without a device. Phase 3's checkpoint A took that reading before a line of the
panel was written, because a no-go would have cut the phase in half.

**Go, and it is the outcome that needs nothing built** (`phase-3.md` R1, 2026-09-02). With the shade
live at dim 100 and a bare 200 dp square added above it as a second `TYPE_APPLICATION_OVERLAY`:

- `dumpsys window windows` prints the stack top-first and the two come back adjacent and the right
  way round — the square at `Window #8`, the shade at `#9`. **The later `addView` is on top**, which
  is what insertion order predicted and what nothing documents.
- With the square above and asking for no brightness of its own,
  `mWindowManagerBrightnessOverride=0.01` and the tag still Gloam's. Removing the square changed
  neither; stopping the shade released it to `NaN`.

So **the topmost window that *asks* owns the override, and one that declines is not consulted** —
between two windows of ours exactly as it held against everyone else's. The panel therefore keeps
`BRIGHTNESS_OVERRIDE_NONE` and the shade keeps the override, which is what makes the panel legible at
the 6.64 nits the shade left rather than at the 1.59 nits every Activity of ours sits at (R3, R6).
The field-copy fallback the phase had written down — the panel carrying the shade's own
`screenBrightness` on every update — is **not built**, and stays in `phase-3.md` §6 as the thing to
reach for if a ROM ever disagrees.

**Read this amendment with [ADR-0011](0011-the-panel-is-touchable-and-what-keeps-it-from-trapping-the-user.md).**
This one says the second window may exist without disturbing the ramp; that one says what stops it
trapping the user once it does.

**One thing R1 found that it was not looking for, and R13 named: `MAX_SHADE_ALPHA` is not the alpha
that lands.** Both of Gloam's overlay windows printed `alpha=0.8` in `mAttrs`, a `LayoutParams` field
this app never sets. It is **Android 12's untrusted-touch cap** — an app overlay that lets touches
pass through may not obscure what is under it by more than
`maximum_obscuring_opacity_for_touch`, unset on both the phone and the emulator and therefore at the
framework's default 0.8, which the window manager writes into the window's alpha. Proven by moving
it: at `0.5` the shade comes back `alpha=0.5` (`phase-3.md` R13). The **panel** carries no alpha at
all, because it catches touches rather than passing them.

**So this ADR's cap is not the binding one, and the error runs in the safe direction.** R3 and R6
measured 0.2393 and 0.2411 of the content still reaching the eye where the two invariants compute
0.05 — that is `1 − 0.8 × MAX_SHADE_ALPHA` = 0.24, and it is *lighter* than computed, which is the
right direction for the bound that keeps the way out visible and the wrong one for the thing the app
is for. Nothing here changes: the invariants still hold, the tests still keep them, and a cap that
binds tighter than ours cannot break a bound stated as a floor on transmitted light. **What it
changes is Phase 2b's ceiling** — ultra dark is defined as going past `MAX_SHADE_ALPHA`, and past a
point more alpha buys nothing at all while the platform pins the window at 0.8. That is in `DOD.md`
against 2b, priced before the phase opens rather than discovered inside it.
