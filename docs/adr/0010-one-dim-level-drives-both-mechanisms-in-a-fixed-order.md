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
