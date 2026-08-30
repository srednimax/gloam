# Phase 1 — The mechanism is complete

The detail for one phase, written when it opened. Sequence lives in [`PLAN.md`](PLAN.md); the live
worklist lives in [`DOD.md`](DOD.md); the decision this phase implements is
[ADR-0010](adr/0010-one-dim-level-drives-both-mechanisms-in-a-fixed-order.md) **and both of its
amendments** — the first assigns owners, the second records what the phone said when this phase was
planned and the ADR's own numbers were re-read against it.

**What closing this phase means.** Gloam subtracts light three ways instead of one — the backlight
walked to its floor, the shade drawn over what is left, and the shade tinted amber — driven by the
single dim level the product is about. The app reaches the place it exists to reach, and it says out
loud the one thing it breaks on the way.

---

## What is in, and what is deliberately not

**In:** the notification-permission entry gate; the backlight override; the ramp as a pure, tested
function; the warmth layer and the composite bound; the copy that explains the inert brightness
slider; the readings this phase's constants depend on, and two taken for later phases' benefit.

**Not in, and the phase that owns each:** ultra dark and the Quick Settings tile (2b), auto-off and
first-run coherence and reboot restore (2), the panel (3b), a `ContentObserver` on `SCREEN_BRIGHTNESS`
(3a), the schedule (4). The one that will feel tempting is the panel: the moment the shade owns a
window brightness, putting a slider in a second window is *nearly* free. It is not — see 3b's
lifecycle cost, and the go/no-go that falls out of checkpoint B's readings.

**One shape to keep, without building it.** Phase 3a renders the same controls in a second host, so
the dim slider, the warmth slider and the backlight toggle should sit in a composable that takes its
state and callbacks as parameters and knows nothing about `Scaffold`, the top bar or the service.
Do not extract `DimControls()` yet — extracting a thing with one caller is guesswork — but do not
write it so that extracting it later means unpicking it.

---

## Checkpoints

**The phase is one phase and five merges.** Each checkpoint leaves the app working, ships its own
copy complete in both locales — `scripts/translation-gate.py` is a merge gate, so that is enforced
rather than remembered — and is a point at which the phase could stop without stranding anything.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | The entry gate | `feat:` | nothing — closes a pre-door `DOD.md` item on its own |
| **B** | The sweep, then the readings | `chore:`, then no commit at all | A landed, phone attached |
| **C** | The backlight joins the ramp | `chore:` ramp + test, then `feat:` | **B's verdict** |
| **D** | Warmth and the composite | `feat:` | C, or B's veto |
| **E** | The documents | `docs:` | everything above |

**B is a gate, not a task.** It can veto C. If the `backlightTop` verification fails — see §2 — the
backlight half does not ship, C is skipped, and D lands against the shade-only ramp, which is the
same function taking the same branch it already takes when the toggle is off. Nothing is stranded by
that outcome and no stored key changes; the phase closes narrower and says so.

---

## 1. Entry gate — `POST_NOTIFICATIONS`, before anything else

`work/NotificationPermission.kt` has existed since Phase 0 and nothing calls it. This phase is the
first one that makes that a safety problem rather than an untidiness: it produces the darkest state
the app will ever reach short of ultra dark, and the ongoing notification's **Stop** action is the
documented way out of it. On Android 13+ that notification does not exist until the permission is
granted, so the property `CLAUDE.md` claims — *the foreground notification is the escape hatch* — is
today only half true.

**Where the ask goes.** In `DimScreen`, fired from the start button, **before the first
`startShade()`** and never after. Not at first launch, per rule 4: the ask waits for the moment the
feature needing it is switched on.

**Why never after — this is a platform constraint, not a preference.** The system hides or refuses
touches on non-system overlay windows while a permission dialog is up, precisely so that an app
cannot draw something over the dialog and trick the user into tapping *Allow*. Our shade is
`FLAG_NOT_TOUCHABLE`, which may or may not exempt it — nobody should assume either way. So the rule
in code is: **the ask is only ever raised with no shade on screen.** If a later phase needs to re-ask
while the shade is up, it stops the shade first or routes to app settings instead. Read the actual
behaviour off the phone anyway (§7 R7) — the rule stands regardless, but the answer decides how much
Phase 2 has to work around.

### "The first" is answered without storing anything

Android permits two denials before the dialog stops appearing, so *who spends them* is a design
question and not a detail. `NotificationPermission.kt`'s own comment already leans on the fact that
`shouldShowRequestPermissionRationale` is exact **after** a denial; it is ambiguous only between
*never asked* and *spent*, and those two want identical behaviour — fire the launcher, which either
shows the dialog or returns `PermanentlyDenied` immediately. So:

| State | Start button | The warning's button |
| --- | --- | --- |
| Granted | starts, no ask | not shown |
| Never asked | **asks** (spends the first) | not shown yet |
| Rationale pending — one denial spent | starts, **no ask** | **asks** (spends the second) |
| Spent | starts, launcher returns instantly | opens settings |

The second denial is only ever spent by a deliberate tap on a control that says *Allow
notifications*. **No DataStore key is needed**, which matters more than it reads: rule 2 freezes
every written key at the door, and `DOD.md` is already carrying `onboardingDone` as the open example
of a key added to answer a question that had a stateless answer.

**The ask is asynchronous, so `startShade()` moves into the outcome callback — and `setRunning(true)`
moves with it.** Both, together, in all three outcomes. If the intent were written on the button
press and the user backgrounded the app mid-dialog, DataStore would say *running* with no service
alive, and Phase 2's reboot restore reads exactly that flag. Keeping them together also makes the
"no shade on screen during the dialog" rule true by construction rather than by convention.

### What happens on a refusal

**The shade still starts.** Refusing to dim someone's screen because they refused a notification is
the app deciding it knows better; the honest move is to run and to say what they have given up. The
gate that genuinely blocks a feature on a live escape hatch is Phase 2b's.

### The warning is a live read, not a remembered outcome

`DimScreen` already re-reads `canDrawShade()` on every `ON_RESUME`, with a comment saying why: it is
a special permission granted on a settings screen, and the user can revoke it there while the app is
backgrounded, so a cached `true` becomes a service that cannot add its window. `POST_NOTIFICATIONS`
has the identical property — and the `PermanentlyDenied` path *sends the user to that settings screen
deliberately*. An outcome-driven warning is therefore stale exactly when it matters: they grant it,
come back, and the banner still says they have no Stop button.

**So the warning is derived, on resume, from whether the notification can actually appear:**

```
areNotificationsEnabled()  &&  channel(Shade).importance != IMPORTANCE_NONE
```

Both halves, because the warning's claim is a safety claim. §1 of the old draft named the channel
case and handed it to Phase 2b — but 2b's gate *blocks* ultra dark, and this *informs*. A false
negative here is a user at maximum darkness with no notification and nothing on screen saying so.
`Channels.kt` and `openChannelNotificationSettings()` both already exist and are both uncalled, so
the read costs a few lines against code the repo is already carrying.

**Shown only while `running` is true**, so it never greets a first-launch user who has started
nothing. **The outcome enum stops deciding whether the warning appears** and only picks which button
it carries:

| Live state | Button |
| --- | --- |
| Permission off, rationale available | Allow notifications (re-ask) |
| Permission off, dialog spent | Open notification settings |
| Permission on, channel importance `NONE` | Open notification settings → `openChannelNotificationSettings()` |

It self-clears the moment the user fixes it, and it *appears* if they revoke it mid-session — a case
an outcome-driven banner would miss entirely.

Closing this checkpoint ticks **Ask for `POST_NOTIFICATIONS`** in `DOD.md` and removes
`work/NotificationPermission.kt` from that file's list of scaffolding describing features the app
does not have.

---

## 2. The ramp

One dim level, one ramp, in a fixed order (ADR-0010). A pure function, in `shade/ShadeRamp.kt`, with
no Android imports at all — that is what makes it testable on the JVM and what keeps rule 3's
promise that the safety values are proven by test rather than by looking at a screen.

```kotlin
/** What the user asked for. Read from DataStore, combined into one value. */
data class DimSettings(
    val dimLevel: Int,          // 0–100, the one value the product is about
    val warmth: Int,            // 0–100
    val lowerBacklight: Boolean,
)

/** What the shade's window and its two children carry. */
data class ShadeValues(
    val backlight: Float?,      // null = release the override; the user's own brightness applies
    val shadeAlpha: Float,
    val warmthAlpha: Float,
)

fun shadeValuesFor(settings: DimSettings, backlightTop: Float?): ShadeValues
```

Two triples, one the input to the function producing the other, both three fields, both in `shade/`.
`CONTEXT.md` exists because this app has exactly that hazard, and it already carries the distinction
that separates them: **dim level** is what the user asks for, **shade** is the mechanism that
delivers it. `backlight` belongs in `ShadeValues` rather than looking out of place there — the
override is an attribute of the shade's own window.

`backlightTop` is a parameter rather than something the function reads, which is the whole trick:
the messy, device-dependent question of *where the backlight starts* stays outside the pure
function, and the function stays exhaustively testable.

### One geometric ramp in total light, spending the backlight first

**There is no `BACKLIGHT_STRETCH` constant.** An earlier draft split the slider at a fixed
breakpoint derived from one measured ratio. The phone says that ratio is not a property of the
device at all — it is user state, and on this one panel it moves by three orders of magnitude:

| The user's brightness | nits | ratio to the floor | where the backlight runs out |
| --- | --- | --- | --- |
| `raw` 255 - their maximum | 500 | 250 | dim 65 |
| `raw` 128 | 251 | 125 | dim 62 |
| `raw` 39 - the device default | 76 | 38 | dim 55 |
| `raw` 20 | 39 | 19 | dim 50 |
| `raw` 10 - **their minimum** | 19 | 9.5 | dim 43 |

A fixed breakpoint at 65 would give the person already at their system minimum a slider whose first
65 points travel from 19 nits to 2.0 - and whose last 35 do everything else. That is precisely
Gloam's user: someone who reads in the dark and finds the lowest system setting still too bright,
and the one whose experience a fixed breakpoint degrades most.

**The measured spread is 43 to 65 rather than the 0 to 70 an earlier draft assumed**, because this
phone's slider stops at `raw` 10 and cannot reach the panel floor at all - so the *"65 points that do
nothing"* case needs a device whose configured minimum is far lower than this one's, which is a
per-device resource and so is a device somebody owns. The conclusion is unchanged and the reason is
now narrower: a fixed breakpoint is wrong by 22 points across one phone's own settings range.

So the ramp is stated once, over the quantity that matters, and the breakpoint falls out:

```
top       = backlightTop, when the override is in play
ratio     = top / MIN_BACKLIGHT                 // 1.0 when there is no backlight to spend
span      = 1 / (1 - MAX_SHADE_ALPHA)           // 20 at 0.95 — what the shade alone can take
t         = dimLevel / 100
light     = (ratio * span) ^ -t                 // fraction of `top`'s light still reaching the eye

backlight  = max(top * light, MIN_BACKLIGHT)
shadeAlpha = 1 - min(1f, light * ratio)
```

Read it as: **the total light falls at a constant ratio per point from 0 to 100, and the backlight is
spent before the shade is touched.** Below the breakpoint `light × ratio ≥ 1`, so `shadeAlpha` is 0
and there is no overlay over the user's content at all — the concrete thing ADR-0010 bought by
sequencing rather than blending. Above it the backlight is pinned at `MIN_BACKLIGHT` and the shade
supplies the remainder.

Four things this buys that the two-stretch version had to assert:

- **No dead zone, for any user.** The breakpoint is `log(ratio) / (log(ratio) + log(span))`, which is
  0 when there is no backlight range to spend.
- **The toggle-off branch disappears.** Toggle off, or `backlightTop == null`, is `ratio = 1`, which
  is `light = span^-t`, which is `shadeAlpha = 1 - (1 - MAX_SHADE_ALPHA)^t` — the shade-only ramp,
  from the same expression with no `if`.
- **Monotonicity and continuity are true by construction**, not by assertion. §8 still tests them,
  because a fencepost in `t` would break both.
- **The perceived rate never kinks**, because there is no seam to kink at.

Dim level 0 is the one special case: **the override is released** (`backlight = null`), not set to
`top`. `CONTEXT.md` already says a dim level of zero is still *running*; this is what that means
physically — the shade is transparent, the backlight is the user's own, and their slider works.

**The same dim level is not the same darkness under the two toggle settings**, and it is not the same
darkness for two users at different brightness. That is what *"100 means as dark as currently
allowed"* already committed to, said out loud rather than hidden.

### Why geometric, and the claim the phone withdrew

Perception of brightness is roughly logarithmic, so equal slider travel should buy an equal *ratio*
of light, not an equal subtraction of it. Linear alpha is visibly wrong here: half-way through the
shade stretch a linear ramp still transmits 52% of the content, so most of the travel is spent in
the barely-dimmed region and all the useful darkness is crammed into the last few points. Geometric
transmits 22% at the same place.

**An earlier draft claimed this was right whatever the panel's response curve turned out to be, on
the grounds that nits ∝ float^γ for any γ. The phone withdrew that.** `dumpsys display` publishes the
device's own calibration:

```
mBacklight = [6.83661E-4, 0.499951, 0.99975586, 1.0]
mNits      = [2.0,        500.0,    1200.0,     2000.0]
```

Below the knee at float 0.5 that is **affine**, not a power law — `nits ≈ 997.5 × float + 1.3` — and
there is a second, steeper segment above it. The 1.3 offset is a large share of the output near the
floor, which is exactly where this product lives, so geometric-in-float and geometric-in-nits diverge
most where it matters. The ramp works in float because float is all an app can set; **R1 measures how
far apart they are**, and the answer is a correction, not a reassurance.

### `MIN_BACKLIGHT` is a floor on the escape hatch, not on the driver

An earlier draft defined it as *the smallest float that still produces a lit panel*. That is a
display-driver question, and the property it is load-bearing for is an ambient-light one.

The reasoning that changed it: **the escape hatches sit above `TYPE_APPLICATION_OVERLAY`, so
`MAX_SHADE_ALPHA` never dimmed them.** It protects the user's ability to see their own content well
enough to navigate. The **backlight override dims everything, the escape hatch included** — and it
does so on a value the user can no longer change, because their slider is inert while it is live.
Phase 0 could not strand anyone this way: its darkest state was the user's own backlight × 0.05, and
their slider still worked.

So `MIN_BACKLIGHT` is the smallest float at which **the notification shade can be pulled down and
Stop tapped, in a dark room and under normal room light**, on both the phone and the API-33 AVD, with
margin. The driver's floor is a hard lower bound on that search, not the answer to it. R2 is the
reading; R5 decides what it is measured against, because if the system's own windows carry their own
brightness above ours, the escape hatch is lifted and the search is much easier.

It is a floor on our own output. The display driver clamps below it anyway, and that clamp is the
thing we must never be seen to be asking to go past — `screenBrightness = 0.0f` is documented as
`BRIGHTNESS_OVERRIDE_OFF`, the backlight *off*, over a live touchscreen.

### The constants

Four now, beside each other, all in `shade/`, all with the comment that says why each is not a
preference.

| Constant | Value | Why it is a safety constant |
| --- | --- | --- |
| `MAX_SHADE_ALPHA` | `0.95f` (settled, Phase 0) | 1.0 is a black rectangle with every way out behind it |
| `MIN_BACKLIGHT` | **from R2**, never `0.0f` | above |
| `MAX_WARMTH_ALPHA` | `0.5f` | §3 |
| `WARMTH_EASE_FROM` | `0.88f` | §3 |

And one colour, `SHADE_AMBER`, bounded by an assertion rather than by taste — §3.

### Where `backlightTop` comes from

The one genuinely device-shaped question in the phase, the reason the ramp does not read it, and —
after the section above — the thing that sets the shape of the whole slider rather than merely its
starting point. **This is what checkpoint B gates.**

`Settings.System.SCREEN_BRIGHTNESS` is readable with no permission — reading is free, only *writing*
needs `WRITE_SETTINGS`, which ADR-0010 rejected. What it gives back is awkward in three ways, none of
which has a public API to fix: the integer's maximum is device-defined rather than a documented 255,
the mapping from that integer to the window override's `0f … 1f` is gamma-encoded by the framework,
and when adaptive brightness is on the stored value may not be what is on screen at all.

**How badly, measured on this phone, with the screen on and the mode manual:**

```
screen_brightness (int)   = 255            <- the top of a 10 ... 255 scale
mScreenBrightness (float) = 0.499951       <- 500 nits, this panel's non-HBM maximum
screen_brightness_float   = null           <- no free float read; that path does not exist here
```

⚠ **An earlier draft of this section recorded `mScreenBrightness = 6.83661E-4` beside that same
integer and built its whole argument on the thousandfold contradiction. That reading was an
artifact.** A screen approaching its inactivity timeout enters the framework's DIM policy and is
pinned to the panel's floor whatever the setting says - nothing in the number gives it away, and the
only place it shows is `mBrightnessReason=manual [ dim ]`. The debug sweep holds
`FLAG_KEEP_SCREEN_ON` while it runs for exactly this reason. **It generalises past brightness: a
device reading taken while the screen is on its way out is a reading of the timeout.**

**What the phone does, once it is actually awake** (R1 and R3, section 7):

| | |
| --- | --- |
| The setting's integer scale | `[10, 255]` - `config_screenBrightnessSettingMinimum`/`Maximum` |
| Integer to applied float | **linear from 1**: `(raw - 1) / 254 * max`. Not gamma-encoded |
| The user's own reach | 19 nits at their minimum, 500 at their maximum |
| The window override's reach | **the same** - override `1.0` and setting `255` are the same 500 nits |
| Below the user's floor | the panel goes to **2.0 nits**, which their slider cannot ask for |

The last two rows are the whole answer. The framework normalises the setting and the override onto
one range before the panel sees either, so **`backlightTop` is a ratio, not a brightness** - what
fraction of their own maximum the user is at. Nothing has to be known about nits, gamma, or where
this panel's high-brightness knee sits, which is why the decode ended up three lines long.

**And the product's thesis, measured rather than asserted:** the system's slider stops at 19 nits and
the panel goes to 2.0. There is a factor of **9.5 in the dark** that Android will not hand the user
and Gloam can, before a shade is drawn over anything at all.

**So: read once, decode, verify, hold, and degrade safely.**

- Read the scale from `config_screenBrightnessSettingMaximum` via
  `Resources.getSystem().getIdentifier(...)`. That is a **resource lookup by name**, not a non-SDK
  Java member, so the hidden-API blocklist does not reach it - but the resource may be absent, in
  which case `getIdentifier` returns 0 and we know we failed.
- **Map linearly, and from 1 rather than from that configured minimum.** The framework's own
  conversion starts at `BRIGHTNESS_OFF + 1`, and the measurement agrees with it to a tenth of a
  percent across the range. Decoding from 10 instead is 48% low at the bottom of the slider - the
  safe direction, but it visibly halves the screen the instant the override engages.
- **Then trim 5% off the result**, and clamp into `0f ... 1f`. Untrimmed the decode is within 0.1% in
  the middle of the range and **3.7% high at the bottom** of it, which is the one direction that
  matters; 5% covers that with margin, and is under both the ramp's own first step and the threshold
  of what an eye can see.
- **On any doubt, `backlightTop` is `null`** and the backlight half does nothing for that session.
  Never fall back to `1.0f`: a fallback that brightens the screen is the mirror image of the failure
  the constants exist to prevent, and *"Gloam blasted my screen"* is the same support mail as
  *"Gloam blacked out my phone"*.
- Capture at the moment the override goes from released to applied — the level crosses 0, or the
  toggle is switched on — and hold until it is released again. Holding is what makes it correct: the
  stored setting still *moves* while the override is live, because the user's slider writes to it
  blind, so a re-read mid-override would pick up a value they set without being able to see it. It is
  also what makes the value re-readable in practice — a user who wants to change their brightness
  drags Gloam to 0, which releases the override and makes their own slider live again.

**Checkpoint B's verdict: passed on 2026-08-30, and C is cleared to spend the backlight.** The
debug build's own computed `backlightTop` was held on the window and compared against
`dumpsys display`'s `mScreenBrightness` at five user settings across the range, mode manual (R3):

| `screen_brightness` | the user's own float | holding the computed top | |
| --- | --- | --- | --- |
| 10 - their minimum | 0.0177148 | 0.017502489 | 1.2% under |
| 20 | 0.03739791 | 0.03614602 | 3.4% under |
| 64 | 0.1240036 | 0.118308894 | 4.6% under |
| 128 | 0.2499755 | 0.23781857 | 4.9% under |
| 255 - their maximum | 0.499951 | 0.47497055 | 5.0% under |

**Never above, at any setting.** The test was *"if the computed value ever exceeds the actual the
backlight half does not ship"*, and it does not exceed it - by construction rather than by luck,
which is what the 5% trim is for. The first attempt trimmed 3% and came out **0.5% over** at the
bottom of the slider; that is the measurement that set the constant, and it is why the constant is
5% rather than a rounder-sounding number.

**Adaptive brightness is resolved, and it is the good branch (R4).** Set `screen_brightness_mode` to
`1` and the framework writes its own automatic choice back into the same integer: at 6.3 lux it
picked 27.2 nits and stored `screen_brightness = 14`, which the app read and decoded to 3.8% under
what was on the panel. So adaptive needs no special case at all - the stored integer is a live
reading of what the framework last chose rather than a stale manual one, and the override replaces
the automatic strategy cleanly (`reason=override(...)`) and hands it back on release. **The blanket
*adaptive to null* rule is not needed**, which matters because adaptive is on by default on most
Android devices and that rule would have shipped this phase's headline mechanism to a minority.

One thing it does confirm: the *capture once and hold* rule above is load-bearing under adaptive for
a second reason. The framework keeps re-choosing as the light changes, so a re-read mid-override
would pick up a value chosen for a room, not for the user.

---
---

## 3. Warmth, and the invariant moving to the composite

The shade stops being one `View` and becomes a `FrameLayout` with two children — **still one
window**, so every safety flag, the type, the cutout mode and `stopWithTask` are untouched. That is
the reason to do it now rather than retrofit it: adding a child costs nothing, adding a window costs
Phase 3b.

### What an overlay can and cannot do — platform, not choice

A window composites over what is behind it with source-over and nothing else. **An overlay can only
add light; it cannot subtract a colour channel.** There is no blend mode an app can ask for against
content it does not own. So warmth works by adding amber to a screen that is already dark, shifting
the average colour — it does not filter blue out of the light underneath, and `CONTEXT.md`'s
"cutting blue" is wrong in the strict sense and moves with this phase (§6). `CONTEXT.md` was already
right to avoid the word *filter* entirely; this is why.

### The algebra, which is most of the safety argument

Two stacked translucent layers over content, black at alpha `a`, amber at alpha `w`:

```
content signal reaching the eye  =  (1 - a) * (1 - w)
```

That product is the reason the invariant had to move. Black at `0.95` and amber at `0.5` puts
neither child past its own cap and leaves **2.5%** of the content — half of what the black layer
alone was ever allowed to leave. The escape hatches survive regardless, since all three sit above
`TYPE_APPLICATION_OVERLAY`; what is lost is the user's ability to see their own screen well enough
to reach any of them, which is the whole thing the cap buys.

**The first invariant, stated over the composite:**

```
(1 - shadeAlpha) * (1 - warmthAlpha)  ≥  1 - MAX_SHADE_ALPHA
```

Read it as: *the composite may never take more signal than the black layer alone was allowed to
take.* It needs no new number justified from scratch — it re-uses the constant already argued for,
which is why this shape was chosen over inventing another one.

### The second invariant, which the first cannot see

Source-over is `w × amber + (1 - w) × content`. The amber does not only attenuate the content, it
lays **veiling light on top of it**, and how much depends entirely on the amber's own luminance —
the very thing the section above says is the whole mechanism. The signal bound cannot see that term
at all. A bright amber passes it and produces an unreadable screen:

| amber | relative luminance | `w × L` at w = 0.5 | signal : veil at `a` = 0.88 |
| --- | --- | --- | --- |
| `#FFB000` | 0.523 | 0.262 | **0.23 : 1** — four times more veil than content |
| `#7A3B00` | 0.073 | 0.036 | 1.7 : 1 |
| `#3D1A00` | 0.017 | 0.009 | 6.9 : 1 |

Every assertion in §8 is green on the first row. So the amber's luminance is not a taste note; it is
the other half of the argument, and it gets an invariant in the same idiom — reusing the constant
already argued for, inventing nothing:

```
MAX_WARMTH_ALPHA * relativeLuminance(SHADE_AMBER)  ≤  1 - MAX_SHADE_ALPHA
```

*The amber may never add more light than the black layer was allowed to leave.* At
`MAX_WARMTH_ALPHA = 0.5f` that caps the amber's relative luminance at `0.10`. It is a compile-time
assertion in `ShadeRampTest` — no device, no meter, no photometry — and it turns *"pick a dark
amber"* from advice into a constraint the build enforces.

**It also proves §3's exception to the house rule rather than asserting it.** The brand seed
`B0763C` (dusk amber, `gen_scheme.py`) has relative luminance **0.224**, so `0.5 × 0.224 = 0.112`
and it fails by more than twice. The shade's amber genuinely cannot come from `MaterialTheme`, and
now there is a number saying why rather than three sentences of reasoning.

### Warmth yields, and it yields gently

At `shadeAlpha = MAX_SHADE_ALPHA` the first invariant forces `warmthAlpha = 0`. Something has to
give at the very top, and it cannot be the dim level: that is the one value the product is about. So
the applied warmth is the user's warmth scaled by the headroom the dim level leaves:

```
headroom(a) = clamp01( (MAX_SHADE_ALPHA - a) / (MAX_SHADE_ALPHA - WARMTH_EASE_FROM) )
warmthAlpha = (warmth / 100) * MAX_WARMTH_ALPHA * headroom(shadeAlpha)
```

**The binding constraint is `(1 - WARMTH_EASE_FROM) × (1 - MAX_WARMTH_ALPHA) ≥ 1 - MAX_SHADE_ALPHA`.**
Sweeping the whole 101 × 101 grid, the worst composite is exactly `0.05`, at dim 100 / warmth 0 —
equality, which is why §8's float epsilon is load-bearing rather than defensive. With
`MAX_WARMTH_ALPHA = 0.5f` that constraint caps `WARMTH_EASE_FROM` at `0.90`.

**`WARMTH_EASE_FROM = 0.88f`**, which leaves margin (`0.06` against a `0.05` bound) and puts the
onset at **dim level ≈ 90 with the backlight toggle on, ≈ 71 with it off**. An earlier draft claimed
`0.8f` eased from ≈ 94; solving back through the shade curve, `0.8f` eases from 84 and 54, and ≈ 94
is not reachable by any legal value of the constant.

**The ease is defined over `shadeAlpha`, not over the dim level, and that is deliberate** — the
invariant is stated over `shadeAlpha`, so easing over anything else would mean re-proving the bound
per toggle state. The consequence is that the *dim level* at which warmth starts to yield depends on
the toggle and on the user's starting brightness. That is the same honesty §2 already owes about
"100 means as dark as currently allowed", and it is stated rather than smoothed over.

A hard clamp instead of an ease would put a visible hue cliff in the last two points, which reads as
a bug rather than as a limit. The ease is in the ramp, so the test covers it.

Halving the margin makes the amber's luminance bound above a gate on the constants rather than a
footnote, which is why it is asserted rather than measured.

### Layer order and the amber itself

**Amber above black.** The order is irrelevant to the signal term — `(1-a)(1-w)` either way — but it
decides the amber's own strength: below the black layer it is attenuated by `(1-a)` and warmth
becomes invisible exactly where a dark-adapted eye would notice it. Above, it arrives at full
strength. The reason that is safe here rather than a way to add light back at maximum dim is the
pair of bounds above: the veil is capped by the amber's luminance, and by the time the black layer
is heavy the amber has already eased away.

**The amber is a constant in `shade/`, not a `MaterialTheme` colour, and this is a deliberate
exception to the house rule.** Three reasons, all of which distinguish it from every other colour in
the app: the shade is not a surface, it is a physical quantity chosen for its effect on light; it
must not change when the user switches the app's own light/dark theme; and it is bounded by the
assertion above rather than by the palette's contrast checks.

---

## 4. What the user sees

The dim screen grows from one slider and a button to three controls and a button. No new screen, no
navigation change.

| Control | Notes |
| --- | --- |
| **Dim level** slider | Unchanged in shape. Its meaning changes: it now drives both mechanisms |
| **Warmth** slider | 0–100, default 0 |
| **Also lower the screen brightness** switch | Default on, per ADR-0010. Disabled with a reason when `backlightTop` is unavailable |
| **Start / Stop dimming** button | Unchanged, now behind the entry gate |

**Warmth defaults to 0.** A colour cast the user did not ask for is indistinguishable from a broken
screen, and it is the same argument that keeps `DEFAULT_DIM_LEVEL` modest. Warmth is worth having
because someone chooses it.

### The switch cannot promise what the device will not do

When `backlightTop` is `null` — the read fails, the decode is out of range, or B's verification
vetoed it — the ramp degrades cleanly to the shade-only case. The UI does not: the switch still says
*Also lower the screen brightness*, still defaults on, and `dim_backlight_hint` still promises the
user's brightness slider will stop working. **That is worse than saying nothing**, and it is the
mirror image of the hint's own justification — an explanation for a symptom that is not happening.

Whether `backlightTop` is obtainable is answerable by the screen, with no service and no override
applied: the resource resolves or it does not, the setting reads or it does not, the decoded float is
in range or it is not. All pure reads, re-read on resume beside `canDraw`. When it is false the
switch stays **visible and disabled** under a line saying so — visible rather than hidden, because it
explains why Gloam behaves differently here than on someone else's phone.

### Saying the inert brightness slider out loud

Rule 4's third clause, and this phase owns it because this phase causes it. While the override is
live the user's own brightness slider moves and does not apply; whatever they set lands the moment
the shade comes off. A user who meets that with no explanation reports that **Gloam broke their
phone**, and nothing today connects the symptom to the switch that ends it.

It goes as supporting text under the switch — always present, not a one-off dialog, because the
symptom recurs and a dismissed dialog is not there when it does. Draft copy, English, to be
translated in the checkpoint that lands it:

| Key | English | Checkpoint |
| --- | --- | --- |
| `dim_notification_warning_title` | No Stop button outside Gloam | A |
| `dim_notification_warning_body` | Gloam is allowed to dim your screen but not to show a notification, so the only way to stop it is this screen. | A |
| `dim_notification_warning_channel_body` | Gloam's notification is switched off, so the only way to stop dimming is this screen. | A |
| `dim_notification_warning_ask` | Allow notifications | A |
| `dim_notification_warning_settings` | Open notification settings | A |
| `dim_backlight_label` | Also lower the screen brightness | C |
| `dim_backlight_hint` | Takes your screen's own brightness down to its lowest before the shade starts, so Gloam has less light to cover. While it is on, your phone's brightness slider will not do anything until you stop dimming. | C |
| `dim_backlight_unavailable` | This phone does not report its brightness in a way Gloam can use, so Gloam dims by drawing over the screen only. | C |
| `dim_warmth_label` | Warmth | D |

Translate from English only, per [`translator-brief.md`](translator-brief.md), and once the wording
is settled rather than twice. Each checkpoint is a merge, so the gate enforces its own rows.

---

## 5. Storage

Two new keys in `AppPreferences`, each a key, a `Flow` reading it with its default, and a `suspend`
setter. No migration, ever — that is the point of the DataStore rule.

| Key | Type | Default | Note |
| --- | --- | --- | --- |
| `warmth` | `Int` | `0` | 0–100, coerced on read and on write like `dim_level` |
| `lower_backlight` | `Boolean` | `true` | ADR-0010's toggle |

**Both are written before the door**, which is the last moment naming them is free: rule 2 freezes
every DataStore key that has reached a real phone. Name them right now — `backlight` is Android's
value throughout `CONTEXT.md`, so `lower_backlight` says what it does without inventing a word.

Neither is affected by B's veto: `lower_backlight` is the user's preference, and a device that cannot
honour it is the disabled-switch case rather than a missing key.

The service currently collects `dimLevel` alone and now needs three values together. One `combine`,
one immutable `DimSettings`, one `distinctUntilChanged`, one apply — not three independent collectors
racing each other into `updateViewLayout`.

---

## 6. Documents this phase amends

- **`CLAUDE.md`** — the composite house rule is already there. It names the warmth bound without
  giving it a name; fill in `MAX_WARMTH_ALPHA` and `WARMTH_EASE_FROM` once they exist, and note that
  the bound is two invariants rather than one — the signal product *and* the amber's luminance.
- **`CONTEXT.md`** — two edits.
  - **Warmth** currently says the shade is tinted amber, *"cutting blue"*. An overlay cannot subtract
    a channel (§3). Say what it does: adds amber over an already-dark screen, shifting the average
    colour rather than removing blue from the light underneath.
  - **Backlight top** is a new entry, paired with the existing **Floor**: the backlight Gloam takes
    over from, captured at the moment the override is applied and held until it is released. It is
    not a device constant — it is user state, and after §2 it sets the shape of the whole slider.
- **ADR-0010 — a second dated amendment is owed**, per this file's own rule that a measurement
  contradicting the ADR gets an amendment rather than an edit. Three of its numbers did not survive
  the phone:
  - the *"factor of ~75"* is not a property of the device. The ratio is user state and reaches 1000
    on this panel.
  - *"500 nits"* was the user's setting at the time, not the panel's maximum. Float 1.0 is 2000 nits.
  - `Settings.System.SCREEN_BRIGHTNESS` reads **255 while the panel sits at its 2.0-nit floor**. The
    ADR did not know that when it chose the window override. It does not reverse the decision —
    `WRITE_SETTINGS` has the same scale problem *plus* the stranding failure the ADR rejected it for
    — but it is material to it, and it is what checkpoint B exists to gate.
- **`docs/index.md`** — says warmth tints the shade *"as far or as little as you like"*, which the
  composite bound makes untrue at the top of the dim range. One sentence. It is a public page that
  Play's enforcement treats as part of the listing, so it gets fixed rather than tolerated.
- **`DOD.md`** — tick the `POST_NOTIFICATIONS` item at checkpoint A and drop
  `NotificationPermission.kt` from the scaffolding list.

---

## 7. Readings, and things that only look like readings

Rule 3: device behaviour is proven by measurement, and the phase writes down what was read and the
command that read it. Everything here needs the phone plugged in — `adb devices` first.

**Rule 3 is about things read off a device, so the two kinds are kept apart.** An earlier draft
listed both under one heading, and at least one of its "measurements" was arithmetic wearing a
command's clothes.

### Readings — off the phone

| # | Reading | Decides |
| --- | --- | --- |
| **R1** | The backlight sweep: float → nits, resolved finely near the bottom | how far geometric-in-float diverges from geometric-in-nits (§2) |
| **R2** | **Escape-hatch legibility at maximum dim** — pull down the notification shade and tap Stop, in a dark room *and* under normal room light | `MIN_BACKLIGHT` |
| **R3** | Computed `backlightTop` vs `dumpsys` `mScreenBrightness`, at four settings, mode manual | **checkpoint B's verdict** |
| **R4** | The same, with `screen_brightness_mode` set to `1` deliberately | whether adaptive forces the `null` path |
| **R5** | Whose override applies when a system window is above the shade — **lock screen, notification shade, volume dialog** | what R2 is measured against; 3b's go/no-go as a corollary |
| **R6** | Is the override released on a ROM kill? `am force-stop` with the shade live | ADR-0010's rejection of `WRITE_SETTINGS` |
| **R7** | Does the permission dialog work with the shade up? | how much Phase 2 has to work around |
| **R8** | The notification and its Stop action appear, and stop the shade, **at maximum dim** | the escape hatch, end to end |
| **R9** | Override vs an app setting its own `screenBrightness` — a video player in swipe-to-dim | a stated use case; taken here because it is cheap here |
| **R10** | API-33 AVD: launches, window appears, permission flow works | ADR-0008's end-of-phase pass |

**R5 was two items in an earlier draft** — "whose override wins with a second window" filed as Phase
3b's go/no-go, and "does the override survive screen off → on" filed separately. They are the same
reading. The system puts windows above `TYPE_APPLICATION_OVERLAY` constantly without any panel
existing, and the lock screen is the one the user meets every time they pick up the phone. Its answer
cuts both ways, which is why it belongs to Phase 1's safety case rather than to 3b's feasibility: if
system windows **do** lift the brightness, the escape hatch is legible while the content underneath
is not, and R2 gets much easier; if they **do not**, `MIN_BACKLIGHT` carries the whole safety
argument and R2 has to be passed against a hostile ambient case.

R1 has a shortcut worth knowing: `dumpsys display` publishes `mBacklight`/`mNits` for the panel, so
the curve does not need a meter — the sweep confirms the override honours it rather than discovering
it. The window override is per-window and only this app can set it, so the sweep needs a control
inside the app: **a sweep row in `app/src/debug/.../DebugSettings.kt`**, which is exactly what that
seam exists for and is currently an empty no-op. It never reaches a release build.

### Derivations — arithmetic, and the test is the proof

Not readings. They are recorded here so nobody goes looking for a light meter, and each is asserted
rather than observed.

| | Quantity | How |
| --- | --- | --- |
| D1 | The composite at maximum dim | `MIN_BACKLIGHT` nits × `(1 - MAX_SHADE_ALPHA)`. Nothing in `dumpsys` can see a view's alpha |
| D2 | The light warmth adds | `MAX_WARMTH_ALPHA × relativeLuminance(SHADE_AMBER) ×` backlight nits — §3's second invariant, asserted at compile time |
| D3 | Where the backlight runs out for a given user | `log(ratio) / (log(ratio) + log(span))` — §2 |

```bash
adb devices
adb shell dumpsys display | grep -E 'mBacklight=|mNits=|mScreenBrightness='
adb shell dumpsys display | grep -A2 OverrideBrightnessStrategy   # override value + owning package
adb shell settings get system screen_brightness
adb shell settings get system screen_brightness_mode              # 1 = adaptive
```

**End of phase:** R10, on the API-33 AVD (ADR-0008). Expect the emulator not to honour
`screenBrightness` in nits at all — that is fine and worth recording as a result rather than as a
failure. The phone stays the only place light is measured.

---

## 8. Tests

Three tests across the whole remaining roadmap, and this phase introduces the first. `ShadeRampTest`,
JVM, no Android — a table sweep over every input rather than a handful of examples, because the
inputs are 101 × 101 × a few and the JVM does not care.

Across all dim levels, all warmth values, the toggle both ways, and `backlightTop` at `1.0f`, at
`MIN_BACKLIGHT`, below `MIN_BACKLIGHT` and `null`:

1. `backlight` is `null`, or in `MIN_BACKLIGHT … backlightTop`. **Never `0.0f`** —
   `BRIGHTNESS_OVERRIDE_OFF`, the failure the constant exists to prevent.
2. `backlight` is never greater than `backlightTop`. The ramp may not brighten.
3. `shadeAlpha` in `0f … MAX_SHADE_ALPHA`.
4. `warmthAlpha` in `0f … MAX_WARMTH_ALPHA`.
5. **The signal invariant:** `(1 - shadeAlpha) * (1 - warmthAlpha) ≥ 1 - MAX_SHADE_ALPHA`, within a
   float epsilon. The worst case over the grid is exactly equal, so the epsilon is load-bearing.
6. **The veil invariant:** `MAX_WARMTH_ALPHA * relativeLuminance(SHADE_AMBER) ≤ 1 - MAX_SHADE_ALPHA`.
   One assertion, no sweep — it is a property of the constants, and it is what stops a later amber
   change from passing every other assertion with an unreadable screen.
7. **Monotonic:** a higher dim level never emits more light — `backlight` non-increasing,
   `shadeAlpha` non-decreasing. True by construction in the new formulation, which is exactly why it
   is worth asserting: a fencepost in `t` would break it silently.
8. **Continuous where the backlight runs out**, for several ratios — no jump in either value. Also
   true by construction; it catches the fencepost, not a wrong breakpoint.
9. **The rate is constant end to end.** Total light falls by the same ratio per point on both sides of
   the point where the backlight runs out. This is the assertion that pins the ramp's shape, and it is
   the one the old fixed-breakpoint design could not make.
10. Dim level 0 releases the override and leaves `shadeAlpha` at 0, both toggle states.
11. `backlightTop == null` and toggle-off produce **identical** output. One branch, proven.

Everything else in this phase is device behaviour and belongs in §7. There is no instrumented test
here: `app/src/androidTest` is empty and the three emulator CI legs are vacuously green, which
`DOD.md` records as Phase 2's problem to either fix or cut. Phase 1 does not adopt it.

---

## 9. The commit sequence

Conventional Commits, and each one leaves the app working — `feat:` lines land in `CHANGELOG.md`
through release-please, so they are written for someone reading the release notes. **`chore:` and
`test:` do not**, which is what decides the type below rather than taste.

| Checkpoint | Commits |
| --- | --- |
| **A** | `feat: ask for notification permission before the first shade` — the gate, the stateless first-ask rule, the live-read warning and its four buttons, plus the strings in both locales |
| **B** | `chore: add a backlight sweep to the debug build` — developer surface, no release note. Then **the readings**, which are not a commit |
| **C** | `chore: add the shade ramp` + `test: bound the shade ramp across every dim level` — the pure function and its table sweep, wired to nothing. Then `feat: take the backlight down with the dim level` — the override on the window, the `lower_backlight` key, the toggle and its two strings. `MIN_BACKLIGHT` gets its measured value here |
| **D** | `feat: tint the shade amber` — the `FrameLayout`, the second child, the `warmth` key and slider, `SHADE_AMBER`, and both invariants added to the ramp test |
| **E** | `docs: ...` — §6's edits, ADR-0010's second amendment, and this file's readings block filled in |

**The ramp is `chore:`, not `feat:`.** It lands wired to nothing — the cheapest possible place to get
the maths wrong — and a `feat:` line would put "add the shade ramp" under **Features** in the release
notes of a version where nothing observable changed, plus a minor bump for it. The feature is the
commit that wires it up, and that one says what a user would notice.

The debug sweep row lands at B and stays; it is developer surface in `app/src/debug/` by the house
rule, never in `main/` behind `BuildConfig.DEBUG`.

---

## 10. Recruiting the twelve testers starts here

Not a build task, and it is in this phase because it is the longest lead item in the plan and it is
calendar time rather than effort. Production access needs twelve testers opted in **continuously for
fourteen days**, confirmed on 2026-08-30 to apply to this app.

**Recruit more than twelve** — fifteen or more. The window wants twelve opted in *continuously*, and
one person uninstalling in week two is the failure the surplus exists to absorb. What they need is
a Google account and the opt-in link from the Play Console's closed track; what they will judge, per
rule 5, is the first two minutes. Track the count in `DOD.md`, which already carries the item.

---

## Kotlin and Compose notes for this phase

- **`combine` of three `Flow`s** is `Promise.all` over streams that never finish: it emits a new
  tuple every time *any* input changes, so the service gets one `DimSettings` rather than three
  callbacks it has to reconcile. `distinctUntilChanged` after it is what stops an unrelated
  preference write from re-applying the window layout.
- **`Float?` rather than a `-1f` sentinel.** Android's own API uses `BRIGHTNESS_OVERRIDE_NONE = -1f`
  because Java had no other way to say "absent". Kotlin does, and `null` cannot be accidentally
  arithmetic'd into a ramp the way `-1f` can. Convert to the sentinel at the one place that talks to
  `LayoutParams`, exactly like a discriminated union collapsing to a wire format at the boundary.
- **A window attribute is not a view property.** `view.alpha = x` takes effect on its own;
  `params.screenBrightness = x` does nothing until `windowManager.updateViewLayout(view, params)`.
  Keep the `LayoutParams` instance the window was added with, mutate it, and update — do not build a
  fresh one, or every flag has to be re-stated correctly every time.
- **`data class` with a `copy()`** is object spread with the compiler checking the field names, which
  is why `DimSettings` is a data class and not three fields on the service.
- **A permission launcher is asynchronous, and that changes the call order** rather than adding a
  callback to the end of it. `startShade()` and `setRunning(true)` both move inside the outcome
  lambda — closer to `await` than to a fire-and-forget `.then()`, because everything after the ask
  depends on its answer.
- **Compile-time constants are safe in a JVM unit test**, method calls on `android.jar` are not —
  the stub throws *"not mocked"*. Keeping `ShadeRamp.kt` free of Android imports makes that a
  non-question rather than a thing to remember. `relativeLuminance` is arithmetic over three floats,
  so §8's veil assertion stays on the JVM too; do not reach for `ColorUtils`.

---

## Readings block

Rule 3: filled in as the phase runs, from the device, with the command that read it. Not "it looked
right". Derivations are in section 7 and are proven by `ShadeRampTest`, not recorded here.

**Read with the screen held awake.** Four of these were first taken against a screen inside its
inactivity timeout, which pins the panel to its floor under `mBrightnessReason=manual [ dim ]` and
looks exactly like "the setting did nothing". The debug sweep holds `FLAG_KEEP_SCREEN_ON`; taking
anything by hand wants `settings put system screen_off_timeout 600000` first, and putting it back
after.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | Nits per `screenBrightness` float, resolved near the bottom | debug sweep + `dumpsys display` | **`nits = 498.3 x override + 1.66`** across 16 steps, 1.0 down to 1E-4. Affine, not a power law; the override clamps to the panel floor at and below 6.83661E-4 (2.0 nits). See below for what it costs the ramp |
| R2 | **Stop reachable at maximum dim, dark room and lit room** -> `MIN_BACKLIGHT` | by hand, on the phone | - (checkpoint C: wants the override on the shade's own window) |
| R3 | **Computed `backlightTop` vs actual, four settings, manual mode** -> B's verdict | debug row + `dumpsys display` | **Passed.** 1.2% to 5.0% *under* the user's own float at `raw` 10/20/64/128/255; never over. Table in section 2 |
| R4 | The same with adaptive deliberately on | `settings put system screen_brightness_mode 1` | **Adaptive is an ordinary read.** The framework stores its own choice back into the same integer (`raw=14` at 6.3 lux, 27.2 nits); decoded 3.8% under. No `null` path needed |
| R5 | Whose override applies - lock screen, notification shade, volume dialog | `dumpsys display \| grep -A2 OverrideBrightnessStrategy` | - (checkpoint C) |
| R6 | Override released on a ROM kill | `am force-stop` with the shade live | **Released.** Holding 2.0 nits, `am force-stop` returned the panel to the user's own 250.7 nits and `reason=manual` within a second. Taken on the activity's window at B; re-confirm on the shade's at C |
| R7 | Permission dialog usable with the shade up | by hand, on the phone | - |
| R8 | Notification and Stop appear and work, at maximum dim | by hand, on the phone | - (checkpoint C) |
| R9 | Override vs an app setting its own `screenBrightness` | same, with a video player in swipe-to-dim | - |
| R10 | API-33 AVD: launches, window appears, permission flow works | `emulator` + by hand | - |

**What R1 costs the ramp, which is a correction and not a reassurance.** The offset is the whole
story: `nits = 498.3 x u + 1.66` means the float range's bottom decade buys almost nothing. Halving
the override from 0.002 to 0.001 is a 50% cut in float and a **19% cut in light** (2.66 nits to
2.16). So a ramp that is geometric in the float is *not* geometric in what the eye receives, and it
flattens exactly where this product lives. The honest ratio the backlight can spend at full
brightness is **250:1 in nits**, not the 1462:1 the floats suggest. Checkpoint C owns what to do
about it; the two constants that would fix it exactly (`498.3` and `1.66`) are this panel's and are
not readable from an app.

**Already read, 2026-08-30, on the phone** - the readings that reshaped this document, kept here
because sections 2 and 6 both argue from them:

| Reading | Value | Command |
| --- | --- | --- |
| The panel's float -> nits curve | `[6.83661E-4, 0.499951, 0.99975586, 1.0]` -> `[2.0, 500.0, 1200.0, 2000.0]` | `dumpsys display \| grep mBacklight` |
| The system's minimum float | `6.83661E-4` - exactly 2.0 nits | `dumpsys display \| grep RangeMinimum` |
| ~~`SCREEN_BRIGHTNESS` at that floor: **255**, with the panel at 2.0 nits~~ | **Withdrawn 2026-08-30.** The panel was in its inactivity DIM policy, not at the user's setting. `raw` 255 is 500 nits; the two never coincide on this phone | `settings get system screen_brightness` |
| The user's own floor | `raw` 10 -> **19 nits**, and their slider goes no lower | `settings put system screen_brightness 10` |
| The panel's non-HBM maximum | `0.499951` - 500 nits, which is where both `raw` 255 and override `1.0` land | `dumpsys display \| grep hbmMax` |
| `screen_brightness_float` | `null` - no free float read on this ROM | `settings get system screen_brightness_float` |

---

## Done when

- The entry gate asks once, spends the second denial only on a deliberate tap, and the notification
  with its working Stop action is on the phone.
- The warning is a live read of whether that notification can actually appear, and it clears itself
  when the user fixes it in settings.
- One dim level drives backlight then shade at a constant ratio per point, warmth tints it, and both
  invariants are asserted by a test that sweeps every input.
- The four constants and the amber have measured or asserted values and the comments that say why
  none of them is a preference.
- The switch says out loud what it does to the user's own brightness slider — or says why it cannot,
  on a device where `backlightTop` is not trustworthy — in both languages.
- The readings block above has no dashes left in it, including the two taken for Phase 3b's go/no-go
  rather than for this phase.
- **Or:** checkpoint B vetoed the backlight half, C was skipped, and the phase closed with that
  written down here rather than argued about later.
