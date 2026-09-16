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
- **Done, 2026-09-13, without a light meter.** There was no second phone, so the question was
  settled from the phone's own readings: the screencap values, mostly `CLIENT` composition in
  SurfaceFlinger (so screencap shows what the panel receives), and the display's sRGB colour mode.
  The model includes the platform's 0.8 window-alpha clamp. The ramp now derives the alpha from
  light, dim 100 is unchanged at ≈0.09 nits, and the levels in between are evenly spaced. See
  ADR-0010's seventh amendment.

## 2. Flicker: find where this panel switches to PWM

- The Redmi Note 14 Pro+ uses DC dimming at high brightness and **1920 Hz PWM** at low brightness.
  Flicker is visible during saccades up to about 2 kHz in a dark room.
- Sweep the window brightness override while filming in slow motion, and note where banding starts.
- If Gloam's backlight floor sits inside the PWM range, add a line to the **lower backlight**
  toggle's description saying that turning it off avoids low-brightness flicker. Those are new
  strings, so they go through the translation gate.
- **Measured, 2026-09-14.** Filmed with a second phone (Xiaomi 14T Pro, Pro video mode, 1/8000 s and
  1/4000 s, dark room) while debug Settings → Developer → *Run sweep* ran. Slow motion was not needed:
  1920 Hz is far above any video frame rate, and a short shutter shows PWM as rolling-shutter stripes.
  The `override:` line is readable in the frames, so each step is certain. Both videos agree:

  | Override | Panel (R1 fit) | Stripes |
  |---|---|---|
  | 1.0 → 0.25 | 500 → 126 nits | none |
  | 0.125 → 0.0001 | 64 nits → floor | **yes** |
  | `MIN_BACKLIGHT` 6.84e-4 | 2.0 nits | **yes** |
  | released, at the user's own 10% | ≈50 nits | **yes** |

  **The panel switches to PWM somewhere between 126 and 64 nits.** The sweep has no step in between.
  The backlight floor is deep inside the PWM range, and so is an ordinary low system brightness.
- **So the line suggested above would be wrong.** Turning the toggle off avoids flicker only if the
  user's own brightness stays above that range. The shade alone divides light by ≈21.3 at dim 100, so
  a flicker-free page can get no darker than about 3–6 nits, against ≈0.09 nits with the backlight
  lowered. Any copy has to present it as a trade-off, without numbers, since the switch point differs
  between panels.
- **Copy done, 2026-09-14.** A *Flicker* section in Settings, next to *Lock screen*
  (`settings_flicker`, `settings_flicker_body`), in English and Polish. It states the trade-off and
  mentions a phone's own anti-flicker or DC dimming setting without promising anything. The
  development phone has none: its device features say `support_dc_backlight` and
  `support_low_flicker_backlight` are `false`, and `hide_flicker_backlight` is `true`. So how that
  setting behaves with a lowered backlight is untested.

## 3. Default warmth colour

Optional, and only after a test.

- In the mesopic range, deep red looks 17–28% darker than amber at equal luminance, and red text is
  the first to slow reading. Red's gain for night vision is small over a light page.
- Consider `DEFAULT_WARMTH_COLOR` 50 → about 25 in `data/AppPreferences.kt`, but only after an
  objective check. For example, a timed reading test on the phone at dim 90, at colour 0, 50 and 100.
- If it changes, add a note to ADR-0010's sixth amendment. *(It did not; the eighth amendment records why.)*
- **The test is built, 2026-09-13**: debug build, Settings → Developer → *Reading test (warmth
  colour)*. It puts a white page with one short sentence under the real shade (dim 90, warmth 100,
  lowered backlight), and you tap True or False. It compares colours 0, 25, 50 and 100 in 16 blocks
  in a mirrored order, takes about eight minutes, and puts your settings back afterwards. Run it at
  night in a dark room. It keeps one row per trial across sessions, so pool a few nights before
  deciding:
  ```bash
  adb shell run-as io.github.srednimax.gloam.debug cat files/reading-test.csv
  ```

- **Result, three nights pooled, 2026-09-16: no colour is distinguishable from 50, so the default
  stays.** 288 trials (3 x 96) on the Xiaomi at dim 90, warmth 100, 12sp, English, lowered backlight,
  same room each night. Accuracy is at ceiling everywhere; response time is the geometric mean of
  correct trials at `rt_ms >= 300`, ratio against colour 50 with a 95% bootstrap interval resampled
  within each night:

  | Colour | Accuracy | Geomean RT | Ratio vs 50 (95% CI) |
  | --- | ---: | ---: | ---: |
  | 0 (amber) | 97% | 2932 ms | 0.946 (0.839-1.066) |
  | 25 | 93% | 3112 ms | 1.004 (0.887-1.132) |
  | 50 (default) | 97% | 3100 ms | 1.000 |
  | 100 (deep red) | 99% | 3220 ms | 1.039 (0.925-1.163) |

  Every interval crosses 1.0, and the rule set before the test — move to 25 only if 25 is clearly
  faster — lands on 1.004. **`DEFAULT_WARMTH_COLOR` stays at 50.** ADR-0010's **eighth amendment**
  records the run and what it settles, and the constant's own KDoc says the photometry is now the
  whole argument for 50 because reading speed had no opinion.

  The nights contradict each other, which is the substance of the result rather than an excuse for it.
  Night 1 had deep red 22% slower; night 2 had amber 23% faster; night 3 had amber slowest and deep red
  fastest, all three intervals straddling 1.0. Two "significant" effects in opposite directions and
  then neither is what a colour with no real effect looks like at 96 trials split four ways. What 288
  trials can resolve on a colour-vs-50 contrast is about **11%** in reading time, so the literature's
  17-28% apparent-darkness gap between amber and deep red is not appearing as a reading cost over a
  white page at this size. A fourth night of the same shape would add about 8% resolution and is not
  worth running; the colour bar stays a matter of taste.

  Rows and per-night summaries are outside the repo, in `~/gloam-data/reading-test/`
  (`reading-test-2026-09-16.csv` is the phone's whole file, all three nights).

## 4. One line of copy for the lock screen

- The keyguard releases Gloam's override, so the screen jumps to the user's own brightness. That is
  the brightest moment in a night session, and it is platform behaviour Gloam can't change.
- Add one tip, in onboarding or settings: keep the system brightness low at night, because Gloam dims
  on top of it. This is strings only, and needs Polish too.
- **Done, 2026-09-13.** There is no onboarding, so it is a *Lock screen* section in Settings
  (`settings_lock_screen`, `settings_lock_screen_body`), in English and Polish.

## 5. Small cleanups left over from PR #58

Some comments still say "amber" where the tint is now a range from amber to deep red:

- the KDoc on `DimSettings` and `ShadeValues` in `shade/ShadeRamp.kt`
- the "Amber above black" comment in `shade/ShadeService.kt`

**Done, 2026-09-13**, along with the same wording in `ShadeRampTest`, `ColumnColors.kt` and
`CLAUDE.md`. ADR-0010 keeps its wording, because an ADR records what was decided at the time.

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
