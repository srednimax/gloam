# Night-reading research: changes to apply

_Written 2026-09-13. A summary of what to change in the app._

The full literature review is **[The Night Reader's Evidence](https://claude.ai/code/artifact/dd89dd67-5275-4c7f-87cd-694f9f38498b)**.
It has all 36 references, the photometric model and the reasoning. The parts that matter for code are
**What this means for Gloam** and the **engineering finding** box. Every claim below has a numbered
reference at the bottom of the report.

The warmth colour work is in **PR #58**, which isn't merged yet, so build on it.

## 1. Measure how much light the shade really passes

Do this first, before changing the ramp.

- `shade/ShadeRamp.kt` assumes shade alpha removes that fraction of *light*: its KDoc says "alpha
  genuinely multiplies".
- The device screencaps show Android blending in **gamma-encoded bytes** instead. ADR-0010's own
  sixth-amendment figures (21% blue left at alpha 0.5) assume the same.
- If the panel composites the same way, an alpha of `a` passes about `(1 − a)^2.2` of the light.
  Dim 100 would then be about **0.09 nits rather than 0.48**, and the shade half of the slider kinks
  where it meets the backlight half.
- **How to check:** in a dark room, lay a second phone's ambient light sensor against the screen.
  Record relative readings at dim 60–100, with the lower-backlight toggle on and off.
- **If confirmed:** derive the shade alpha from the target light through the sRGB encode, not
  `1 - light`. Then re-check both invariants in `ShadeRampTest`, and update the dim-100 figure in
  ADR-0010 and `docs/DOD.md`.

## 2. Flicker: find where this panel switches to PWM

- The Redmi Note 14 Pro+ uses DC dimming at high brightness and **1920 Hz PWM** at low brightness.
  Flicker is visible during saccades up to about 2 kHz in a dark room.
- Sweep the window brightness override while filming in slow motion, and note where banding starts.
- If Gloam's backlight floor sits inside the PWM range, add a line to the **lower backlight**
  toggle's description saying that turning it off avoids low-brightness flicker. Those are new
  strings, so they go through the translation gate.

## 3. Default warmth colour

Optional, and only after a test.

- In the mesopic range, deep red looks 17–28% darker than amber at equal luminance, and red text is
  the first to slow reading. Red's gain for night vision is small over a light page.
- Consider `DEFAULT_WARMTH_COLOR` 50 → about 25 in `data/AppPreferences.kt`, but only after an
  objective check. For example, a timed reading test on the phone at dim 90, at colour 0, 50 and 100.
- If it changes, add a note to ADR-0010's sixth amendment.

## 4. One line of copy for the lock screen

- The keyguard releases Gloam's override, so the screen jumps to the user's own brightness. That is
  the brightest moment in a night session, and it is platform behaviour Gloam can't change.
- Add one tip, in onboarding or settings: keep the system brightness low at night, because Gloam dims
  on top of it. This is strings only, and needs Polish too.

## 5. Small cleanups left over from PR #58

Some comments still say "amber" where the tint is now a range from amber to deep red:

- the KDoc on `DimSettings` and `ShadeValues` in `shade/ShadeRamp.kt`
- the "Amber above black" comment in `shade/ShadeService.kt`

## Keep as is

- **No sleep or health claims** in the app or the Play listing. The evidence supports "comfortable
  in the dark", not "protects your sleep".
- **Warmth stays a comfort setting.** At equal melanopic dose it keeps only about 1.3× the light of a
  plain dim, and the colour position changes that by about 4%.
- **The auto-off timer and schedule.** Sleep displacement is the largest measured harm of phone use
  at night, so these are the features that best match the evidence.

## Don't do

- **Night Shift-style "blue light" marketing.** It isn't supported by the trials or the Cochrane review.
- **A greyscale mode.** Source-over can't desaturate; only the system's colour correction can.
