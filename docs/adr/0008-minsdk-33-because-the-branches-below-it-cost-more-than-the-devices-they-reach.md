# `minSdk` is 33, because the branches below it cost more than the devices they reach

## Context

`minSdk` 26 was inherited from the template, not chosen for Gloam. It looked free. It is not, and the
reason is the test rig rather than the code.

There is **one** device in the loop — a Redmi `amethyst` on HyperOS, Android 16, **API 36** — and no
emulator on the machine at all. That device sits at the *top* of the supported range, so every
platform difference the app depends on lives on the unvalidated side of it:

- `ShadeService` branches three ways on `layoutInDisplayCutoutMode` — `ALWAYS` at 30+, `SHORT_EDGES`
  at 28+, nothing below. **The bottom branch has never executed anywhere.**
- `POST_NOTIFICATIONS` is a runtime ask on 33+ and install-granted below it. The shade's ongoing
  notification is the documented way out of a very dark screen, so the app's central safety property
  has two behaviours and the phone can only show one of them.
- `TYPE_APPLICATION_OVERLAY` on API 26–27 predates three rounds of overlay tightening.

Walking up the ladder, what each step actually deletes *from this app*:

| `minSdk` | What it removes | Devices reachable |
| --- | --- | --- |
| 26 | — | ~99% |
| 30 | The three-way cutout branch collapses to one line; `<queries>` behaves uniformly | ~85–90% |
| **33** | `POST_NOTIFICATIONS` becomes one code path; ADR-0004's below-13 locale backport becomes dead weight | ~70% |
| 34 | Foreground-service types uniform — already declared, so nothing | ~55% |
| 35, 36 | **Nothing this app uses** | ~24% |

Android 16 was at ~24% of devices in August 2026 (AppBrain; StatCounter agreed within a point).

## Decision

**`minSdk` 33.** The phone at API 36 stays the primary target and the only place nits are measured;
**one API-33 AVD** covers the rest of the range, as a "does it launch, does the window appear, does
the permission flow work" pass at the end of each phase rather than as a test suite.

## Alternatives

**Hold at 26.** Reaches ~99% of devices and keeps every branch. It loses because those branches are
not merely untested — they are *untestable* in this setup, and shipping a safety property whose
alternate path has never run once is worse than not reaching the phones it was for.

**`minSdk` 36.** Considered because it would collapse the matrix to the one device that exists. It
loses badly: it costs roughly three phones in four and buys **nothing over 33** that this app uses.
Paying 46 points of reach for zero simplification is the worst trade on the table.

**`minSdk` 30.** The honest runner-up — ~85–90% reach, and it does kill the cutout branch. It loses
because the branch that matters is the notification permission, and 30 leaves that one in place.

## Consequences

- Roughly 30% of Android devices cannot install Gloam. That is the price, stated plainly.
- **The direction is asymmetric and this is why the decision is made now:** raising `minSdk` after
  launch strands existing installs on the last build that fitted them; lowering it later is free. It
  is one of the few decisions here that is genuinely cheaper to make before users than with them.
- The `layoutInDisplayCutoutMode` branches below `R` are dead and are removed with this change.
- **ADR-0004's below-13 locale backport is now dead weight** — the disabled
  `AppLocalesMetadataHolderService` manifest entry exists only for devices this app no longer ships
  to. It is *not* removed here: that is ADR-0004's territory and deserves its own reading. Noted in
  `DOD.md` so it cannot be forgotten.
- AppCompat itself stays regardless: ADR-0006 needs `setDefaultNightMode` to move the *configuration*
  so the window background and the system bars agree.
