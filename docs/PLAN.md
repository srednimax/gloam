# Roadmap

Sequence and status only. The reasoning behind a decision lives in [`docs/adr/`](adr/); the words
live in [`CONTEXT.md`](../CONTEXT.md); commands, layout and house rules live in
[`CLAUDE.md`](../CLAUDE.md).

**This file is the record, not the worklist.** What is still open — the boxes to tick, with the
device state and the commands to read it — lives in [`DOD.md`](DOD.md), which stays short enough to
open every session. Read the phase you are in here; read `DOD.md` to know what to do next.

**A phase being planned or built gets its own file** — `phase-N.md` beside this one — so working on
it costs the phase rather than the whole history. This file stays deliberately thin: one paragraph
per phase, no task lists. The detail is written when the phase opens, not now.

## Status

- [x] **Phase 0** — Skeleton, palette, and a shade that works
- [ ] **Phase 1** — The mechanism is complete
- [ ] **Phase 2** — You cannot get trapped ← **the door: closed testing opens here**
- [ ] **Phase 3** — Reach the controls from anywhere
- [ ] **Phase 4** — It turns itself off
- [ ] **Phase 5** — Ship shape

---

## Three rules that cut across every phase

### The door is Phase 2, not Phase 5

**The first upload is gated on the app being safe, not on it being finished.** Phase 5 is therefore
split rather than deferred: the *pipeline* half — `applicationId`, keystore, service account,
developer identity verification, the Play entry — happens early, and the *polish* half — listing
copy, feature graphic, privacy prose — stays last, which is what "build first, polish later" has
always meant here.

The reason is that the remaining work is not all the same kind. Some of it a calendar controls:

- **Closed testing needs 12 testers opted in continuously for 14 days** before production access
  exists. That is the longest lead item in this document, it cannot be compressed by working harder,
  and it depends on other people replying. **Recruiting starts in Phase 1.** (A personal developer
  account registered after Nov 2023 is what this applies to; an organisation account is exempt.)
- **All Play packages must be registered by 30 September 2026**, and registration requires completed
  developer identity verification. Registration is keyed to the **signing key**, not to a domain —
  and under-50-install names are first-come, first-served, so registering early is also how the name
  is secured.

Phases 3 and 4 both carry open questions that only users can answer. Building them before anyone has
used the app means answering those in a room with one person in it.

### How a phase proves itself

Every risk in this plan is device behaviour, not logic — none of it is reachable by a unit test. So:

- **Every `phase-N.md` ends with a measurement block**: what was read off the device, and the command
  that read it. Not "it looked right".
- **The phone is the only place nits are measured.** It is a Redmi `amethyst`, HyperOS, Android 16,
  **API 36** — the top of the supported range.
- **One API-33 AVD covers the rest** (ADR-0008). Not a test suite: a "does it launch, does the window
  appear, does the permission flow work" pass at the end of each phase.

### Ask for nothing before the feature that needs it

By the end of this plan Gloam asks the user for four things, and three of them are hand-offs to
settings screens whose result cannot be read back reliably — `appops` has already been caught lying
about `SYSTEM_ALERT_WINDOW` on the test phone. So the sequence is a rule, not a phase:

**Every ask waits for the moment the feature needing it is switched on, and the phase that introduces
an ask owns its hand-off and its re-read.**

| Ask | When | Owner |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | First run — there is no app without it | Phase 0 |
| `POST_NOTIFICATIONS` | Immediately after, **before the first `startShade()`** | Phase 1 |
| Battery-optimisation exemption | Only when the user enables scheduled-on | Phase 4 |
| Xiaomi autostart | Only when the user enables scheduled-on | Phase 4 |

Nobody sees the last two unless they asked for the feature behind them.

---

## Phase 0 — Skeleton, palette, and a shade that works ✅

The template's data layer is gone (ADR-0007), the dusk palette is generated and passes its contrast
checks, and `shade/ShadeService.kt` draws a real overlay that survives leaving the app. One screen,
one slider, one switch, over DataStore. English and Polish.

This is where the app is today.

## Phase 1 — The mechanism is complete

Gloam currently subtracts light one way, by drawing black over the screen. It should do it three
ways, and the other two are the difference between "another dimmer" and the thing the README
promises.

**Entry gate, before anything else in this phase: ask for `POST_NOTIFICATIONS`, and confirm on the
phone that the notification and its Stop action actually appear.** This moved here out of Phase 2
deliberately. Phase 1 produces the darkest state the app will ever reach — backlight at its floor
*and* the shade at its cap — and the ongoing notification is the documented way out of it. On
Android 13+ that notification does not exist until this is granted, and today nothing calls
`work/NotificationPermission.kt`. There is also a platform reason to do it first: the system
suppresses or blocks touches on non-system overlay windows while a permission dialog is up, so the
ask may simply not work from a screen sitting under the shade. Measure that rather than assume it.

- **Backlight.** `CONTEXT.md` has had the word since the beginning and nothing reads it. Measured on
  the phone 2026-08-30: `WindowManager.LayoutParams.screenBrightness` on the shade window takes the
  panel from **500 nits to 6.64 nits**, the display floor is **2.0 nits**, and it costs **no
  permission** — the system reverts it when the window goes away, so a ROM kill cannot strand
  anyone. `WRITE_SETTINGS` is rejected on that comparison alone.
- **Warmth.** Also already in the vocabulary, also unbuilt. The shade stops being one `View` and
  becomes a `FrameLayout` with two — black at the dim level, amber at the warmth — still one window,
  so every safety flag is untouched. Deliberately done now rather than retrofitted.
- **The range.** With the backlight at its floor *and* the shade at its cap, the app finally reaches
  the place it exists to reach.

**Settled (ADR-0010): one dim level, one ramp, in a fixed order.** The first stretch of the slider
walks the backlight down to `MIN_BACKLIGHT`; the remainder raises the shade to `MAX_SHADE_ALPHA`. The
backlight half is a **toggle, not a second slider**, default on. `MIN_BACKLIGHT` is a safety
constant beside `MAX_SHADE_ALPHA` and for the mirror-image reason: `screenBrightness = 0.0f` is
documented as `BRIGHTNESS_OVERRIDE_OFF`, and a ramp reaching it hands the user a black screen with a
live touchscreen. **100 means "as dark as currently allowed"**, so Phase 2's ultra dark extends the
same slider rather than adding another.

**One measurement to take here for Phase 3's benefit:** the backlight override lives on a *window's*
`LayoutParams`. When a second window appears above the shade, whose override applies? Read it with
`dumpsys display | grep -A2 OverrideBrightnessStrategy`. Finding this out in Phase 1 costs ten
minutes; finding it out in Phase 3 costs a design.

## Phase 2 — You cannot get trapped

The house rules already claim this property and it is only half true today, so it is a phase rather
than a checklist item. Safety comes before reach, because Phase 3 makes the app much easier to leave
running.

- **Ultra dark, gated on the Phase 1 ask having succeeded.** Going past `MAX_SHADE_ALPHA` is only
  defensible while the escape hatch provably exists. It is a real gate, not a warning dialog.
- **Reboot restore.** `RECEIVE_BOOT_COMPLETED` is a normal permission with no prompt, and Android
  15's BOOT_COMPLETED blocklist does not include `specialUse` — so reading `shadeRunning` at boot and
  putting the shade back is allowed.

Worth knowing throughout: the pull-down notification shade, the lock screen and system dialogs are
**above** `TYPE_APPLICATION_OVERLAY` and can never be dimmed. That is a platform ceiling, and it is
also the reason a guaranteed-bright escape hatch exists at all.

**This is the door.** When this phase closes the app is safe and its mechanism is complete, which is
the earliest point a tester's time is worth asking for. Closed testing opens here.

## Phase 3 — Reach the controls from anywhere

Decided in the brainstorm: the compact screen and the floating panel are **one `DimControls()`
composable rendered in two hosts**, not two features.

- **From the launcher** — a floating-window `MainActivity`. Small, dialog-shaped, switch and sliders
  only, with a setting that expands it to the full app.
- **From inside another app** — the **panel**: a second, *touchable* overlay window above the shade
  (`CONTEXT.md` names it, because it is not the shade and its safety rule is the opposite one). The
  value is the live preview: the slider moves the dim over the real content instead of over Gloam's
  own screen. Its cost is that Compose has no Activity there, so the `ViewTree*Owner`s are attached
  by hand.
- **Summoned from the ongoing notification**, with a **Quick Settings tile** as a cheap second door.
  Both live in the one surface the shade cannot dim. **No always-visible floating handle in v1** — a
  dark-adapted eye does not want a bright dot in it, and it lands in every screenshot.

The panel needs its own safety rule, opposite to the shade's: it *is* touchable, so it blocks touches
underneath it. Size its window to the panel, never `MATCH_PARENT`, and dismiss on an explicit close
plus an inactivity timeout rather than tap-outside. Two more things it must carry, both following
from Phase 1's measurement: `FLAG_NOT_FOCUSABLE` (a window can receive touches without taking
keyboard focus — `FLAG_NOT_TOUCHABLE` is the one that passes them through), and **the same
`screenBrightness` value as the shade**, so that opening the controls cannot snap the screen back to
full brightness.

**Open:** while the backlight override is live the user's own brightness slider moves but does
nothing — it applies on release instead (measured). Does Gloam watch `SCREEN_BRIGHTNESS` with a
`ContentObserver` and reinterpret a change as "I want more light", or is that too clever?

## Phase 4 — It turns itself off

The named fear: *"I hate when other apps have no auto disable and the next day I cannot see anything
on my phone."* Both halves ship, and they are one model rather than three timers.

- **Auto-off** is a one-shot for a manual start. It needs **no permission at all**, because the thing
  being turned off is a foreground service that must be alive to be turned off — a coroutine on the
  scope the service already owns.
- **The schedule is a window** — on at 22:00, off at 07:00. One pair, not two independent switches.
- **One stored `offAtMillis`, and the earlier deadline wins.** Storing an absolute instant rather
  than a countdown is load-bearing: a HyperOS kill plus a `START_STICKY` restart then resumes the
  right deadline instead of silently restarting the clock, which is the exact failure being designed
  out.
- **Scheduled-*on* is the expensive half** and the only part needing new machinery: nothing of ours
  is running to do it. Use an **inexact `setAndAllowWhileIdle` alarm plus the battery-optimisation
  exemption** — that exemption is on Android's own list for starting a foreground service from the
  background, and `work/BatteryExemption.kt` already exists. **Not `SCHEDULE_EXACT_ALARM`**: it needs
  a Play declaration form and a dimmer does not qualify as an alarm or clock app. Xiaomi autostart
  sits on top of all of it and lapses on its own.

Auto-off should default **on**.

**No table.** The whole of this phase is four values — on time, off time, enabled, `offAtMillis` —
which is four DataStore keys with their defaults declared beside them. See *Not in this plan* for the
test that decides when that stops being true.

## Phase 5 — Ship shape

Split in two, per the first rule above.

**The pipeline half, early — before the closed test can start:**

- `applicationId` **ratified: `io.github.srednimax.gloam`** (`DOD.md`'s item closes). It is
  reverse-DNS on a namespace verifiably yours, Play has never checked domain ownership, and package
  registration is keyed to the signing key. It is also the safer of the two candidates: short generic
  names like `app.gloam` are the ones another developer could claim first. `namespace = "app.gloam"`
  is internal and never reaches Play.
- The upload keystore, the Play service account and secrets, the repository setup — all still as
  listed in [`DOD.md`](DOD.md).
- **Developer identity verification and package name registration**, which are new and have their own
  external lead time.

**The polish half, last:** the mark, the listing copy, the privacy policy, the screenshots. Plus:

- **A Support screen.** `ui/support/` already holds the licences half. The rest is a rate-on-Play
  link and `#bug` / `#feature` mail hand-offs, close to a lift from Binky's `SupportHandoff.kt` — a
  design that already cost a device session to get right (Gmail silently ignores `EXTRA_SUBJECT` for
  `ACTION_SENDTO`, so subject and body travel percent-encoded in the `mailto:` query string).
- **The tip, which ships** — see ADR-0009. An earlier reading had this as probably unshippable; the
  live policy text says the opposite. Play's §3.2 treats a tip where 100% reaches the creator and
  nothing unlocks as a **peer-to-peer payment** for which its billing system is not required, and the
  §4 anti-steering rule carries an explicit exception for §3. So it is an external link on the
  Support screen, no billing dependency, and the promise in `CLAUDE.md` and `README.md` stands.

Languages are not a phase of their own. `scripts/translation-gate.py` makes completeness a merge
gate, so English and Polish stay in step branch by branch. Adding a third is an opt-in, and the day
it happens it gets a phase file.

---

## Not in this plan

**No phase in this roadmap adds a database.** ADR-0007 removed Room and said the reasoning survives
in the documents; this is the boundary that keeps it removed.

The test that decides it is **cardinality, not subject matter**: a *fixed set* is not a *list*,
however many members it has. Four schedule values are settings. Seven per-day windows would be
fourteen settings — still DataStore, still nothing to migrate. What needs a table is the user
creating rows nobody knew about at build time.

Out, and what each would cost if it ever came in:

- **Saved presets** — a user-created list. Reactivates ADR-0001 in full: schema gate, exported
  schemas, migration tests, the device-upgrade ritual parked in `DOD.md`.
- **Per-app rules** — a table *and* knowing the foreground app, which means `PACKAGE_USAGE_STATS` or
  an accessibility service. The accessibility route is a well-known Play rejection risk for an app
  that is not an accessibility tool, and either way it is a fifth permission hand-off.
- **A home-screen widget**, **a Tasker / intent API**, **colour filters beyond warmth**,
  **multi-display support**, and the **always-visible floating handle** (decided against in Phase 3).

None of these is forbidden forever. Each one reopens ADR-0007 deliberately, with a migration story
attached — which is exactly what ADR-0007 says adding a table should cost.
