# Ultra dark is cancelled: the platform's touch-obscuring clamp is the floor

## Context

[`PLAN.md`](../PLAN.md) gave **Phase 2b** one headline mechanism — *"Ultra dark, going past
`MAX_SHADE_ALPHA`"* — and described it as the riskiest feature in the document, which is why it was
put behind the safety door and into a running closed test rather than in front of one. Everything
around it was designed for that risk: a live escape-hatch gate re-read continuously rather than a
remembered grant, and a Quick Settings tile brought forward as the hatch that fails independently of
the notification.

Then the mechanism was measured, twice, and it is not worth having.

**The ceiling is the platform's, and it is not the app's alpha.** `phase-3.md`'s R13 (2026-09-03)
found `maximum_obscuring_opacity_for_touch` — unset on the dev phone and on the API-33 emulator, so
both sit at the framework default of **0.8**. An app overlay that **passes touches** may not obscure
more than that, and the window manager does not warn: it writes 0.8 straight into the shade's window
alpha. Moving the global to `0.5` brings the shade back at `alpha=0.5`, which is how it was confirmed
to be the platform's number rather than one of ours. This is why R3 and R6 measured **24%**
transmission at dim 100 where [ADR-0010](0010-one-dim-level-drives-both-mechanisms-in-a-fixed-order.md)'s
invariants compute 5%: past a point, more alpha buys nothing, because the composite is multiplied by
0.8 whatever its children do.

**Then the levers were priced** (2026-09-13, dev phone; the table is in [`DOD.md`](../DOD.md)). At dim
100, in stored values — in light, divide by roughly five (ADR-0010's seventh amendment):

| lever | reaching the eye |
| --- | --- |
| the old `MIN_BACKLIGHT` of `0.01f` | 1.59 nits, measured |
| **the backlight at the panel's floor** | **≈0.48 — about 3.3× darker** |
| the shade cap raised to the 0.8 clamp, on top | ≈0.40 — **17%** |
| Extra dim on top of that | ≈0.07, computed from `framework-res` |

The phase's real win is on the second row, it is worth 3.3×, and it is **not ultra dark** — it is the
backlight, taken the same day and outside the phase as ADR-0010's fifth amendment. What the phase was
actually defined as is the third row: 17%, for breaking both of `shadeValuesFor`'s bounds.

## Decision

**Ultra dark is not built. `MAX_SHADE_ALPHA` stands, and both of ADR-0010's invariants stand with
it.** Phase 2b ships what is left of itself: the **Quick Settings tile**, which was always the safety
equipment attached to the mechanism and survives the mechanism's cancellation because a second
escape hatch is worth having on its own terms.

Darkness past this point is the **user's** to reach, through a system setting Gloam does not own, and
the app's copy says the ceiling exists rather than promising past it.

**The `escapeHatchLive() || tileAdded()` gate is not built either.** It existed to guard ultra dark,
and there is nothing left to guard. That is fortunate rather than incidental — see *Consequences*.

## Alternatives

**Raise `MAX_SHADE_ALPHA` to the 0.8 clamp.** This is the feature as planned, and it is the third row
of the table: **17%**, ≈0.48 → ≈0.40 nits. It costs both of the bounds `CLAUDE.md` calls load-bearing
— `(1 - shadeAlpha) * (1 - warmthAlpha) ≥ 1 - MAX_SHADE_ALPHA` for the signal that survives, and
`MAX_WARMTH_ALPHA * relativeLuminance(warmthTint(c)) ≤ 1 - MAX_SHADE_ALPHA` for the light the tint
adds. Those two are what keep the composite legible at every warmth colour, and `ShadeRampTest` sweeps
every input against them. Seventeen percent is not a perceptible step; two broken invariants are a
screen nothing can be read through. **This is the one that lost, and it is the whole reason for this
ADR.**

**Drop `FLAG_NOT_TOUCHABLE` so the clamp does not apply.** The clamp exists precisely because the
window passes touches; a full-screen overlay that catches them instead is exempt. It is also the trap
the app exists not to be — the phone appears frozen and the way out is behind the thing you need to
get out of. Not a trade-off, a contradiction in terms.

**`FLAG_DIM_BEHIND`.** Its dim is genuinely not clamped (`a:0.95` in SurfaceFlinger). The dim layer
belongs to uid 1000, and the input dispatcher treats it as a blocking occluder, so every touch to
every other app is dropped at any `dimAmount`. Measured dead. (The system Settings app is uid 1000
and exempt, so it is the wrong place to test this — which is how it looked alive at first.)

**Stack a second full-screen shade window.** One uid's opacities add up: two windows at 0.8 read as
0.96 to the input dispatcher, so it is the same wall one step further along. The readings are in
`DimBehindWindow`'s KDoc in `src/debug/`.

**Hand off to Extra dim** (Reduce Bright Colors) with a *Darker still* row deep-linking
`android.settings.REDUCE_BRIGHT_COLORS_SETTINGS`. It is the only lever past the ceiling and it is
worth ≈0.07 nits — by far the largest number in the table. **It was built and then deleted**, because
on HyperOS there is no user-reachable way to turn Extra dim on, measured three ways:

- The ROM's Settings toggle is broken. Injected taps arrive at the activity — `ACTION_DOWN` and
  `ACTION_UP` both logged, no obscured-touch rejection — and `reduce_bright_colors_activated` stays
  `0`.
- `adb shell settings put secure reduce_bright_colors_activated 1` works and `dumpsys color_display`
  reports `Activated: true`. So the **mechanism** is fine and the ROM's Settings app is not.
- HyperOS's Control Center ignores the AOSP `sysui_qs_tiles` list, so the system Extra dim tile cannot
  be added that way either.

A button that opens a screen whose switch does nothing is worse than silence. It fails on the only
device in the loop, and the failure is invisible from inside the app — nothing readable tells Gloam
whether the hand-off landed. (It was briefly theorised that Gloam's own overlay was rejecting the
touch as obscured. It is not: the toggle fails identically with no shade and no overlay permission.
Not our bug, and worth writing down so nobody re-derives it.)

**Take the backlight to the panel's floor.** This one **won** — 3.3×, the largest real gain available
— and it is the reason the rest of the table is a rounding error. It is recorded as ADR-0010's fifth
amendment rather than here because it is a change to the ramp, not to the cap, and it shipped outside
this phase.

## Consequences

**Dim 100 is ≈0.48 nits in stored values, ≈0.09 in light, and that is the floor the app ships.** It is
about 3.3× darker than the app reached a week before this was written, and the honest framing is
headroom rather than a new maximum: the level someone actually reads at should now sit well below 100,
with the top of the range kept for the nights that need it.

**The remaining limit on darkness is not alpha, it is flicker.** The panel is PWM from somewhere
between 126 and 64 nits downwards, so the floor flickers and so does the user's own 10%. Settings has
a *Flicker* section that says so. Any future attempt to go darker runs into this before it runs into
anything Gloam controls.

**The escape-hatch gate is never built, and the `tileAdded()` half could not have been built safely
anyway.** There is no live read of "is my tile added" — `onTileAdded` / `onTileRemoved` remembered
into a DataStore key is the only route, and that is live state in a store Auto Backup carries whole to
the next phone, where the tile is absent and the key says otherwise. `shade_began_at` is what that
costs once. Cancelling ultra dark removes the only caller that needed it.

One caller would still have liked it: `ControlsActivity.forwardIfUnusable()` is now stricter than the
danger warrants, since the tile *is* a way out. It stays conservative on purpose.

**`PLAN.md`'s own framing of Phase 2b does not survive**, and neither does the reason 2b sits behind
the door. The tile is not risky — it needs no permission, cannot be silenced by a channel setting, and
its down path is unconditional. What remains true is the sequencing: it ships into a running closed
test, which costs nothing.

**The copy has a ceiling to describe.** "As dark as it goes" is now a statement about the panel and
the platform rather than about a feature, and the store listing must not imply otherwise.
