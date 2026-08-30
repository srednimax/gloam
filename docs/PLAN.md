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
- [ ] **Phase 2** — You cannot get trapped
- [ ] **Phase 3** — Reach the controls from anywhere
- [ ] **Phase 4** — It turns itself off
- [ ] **Phase 5** — Ship shape

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

**Open, and the first thing to settle when this phase opens:** does one dim level drive both the
backlight and the alpha as a single continuous ramp, or is the backlight a separate control? The
argument for one is that users came here with one question. The argument for two is that they are
genuinely different mechanisms with different failure modes.

## Phase 2 — You cannot get trapped

The house rules already claim this property and it is only half true today, so it is a phase rather
than a checklist item. Safety comes before reach, because Phase 3 makes the app much easier to leave
running.

- **Ask for `POST_NOTIFICATIONS`.** `work/NotificationPermission.kt` exists and nothing calls it. On
  Android 13+ a denied permission means the foreground service shows *no* notification — so the
  documented way out of a very dark screen is missing exactly when it matters.
- **Ultra dark, gated on that ask having succeeded.** Going past `MAX_SHADE_ALPHA` is only defensible
  while the escape hatch provably exists. It is a real gate, not a warning dialog.
- **Reboot restore.** `RECEIVE_BOOT_COMPLETED` is a normal permission with no prompt, and Android
  15's BOOT_COMPLETED blocklist does not include `specialUse` — so reading `shadeRunning` at boot and
  putting the shade back is allowed.

Worth knowing throughout: the pull-down notification shade, the lock screen and system dialogs are
**above** `TYPE_APPLICATION_OVERLAY` and can never be dimmed. That is a platform ceiling, and it is
also the reason a guaranteed-bright escape hatch exists at all.

## Phase 3 — Reach the controls from anywhere

Decided in the brainstorm: the compact screen and the floating panel are **one `DimControls()`
composable rendered in two hosts**, not two features.

- **From the launcher** — a floating-window `MainActivity`. Small, dialog-shaped, switch and sliders
  only, with a setting that expands it to the full app.
- **From inside another app** — a second, *touchable* overlay window above the shade. The value is
  the live preview: the slider moves the dim over the real content instead of over Gloam's own
  screen. Its cost is that Compose has no Activity there, so the `ViewTree*Owner`s are attached by
  hand.
- **Summoned from the ongoing notification**, with a **Quick Settings tile** as a cheap second door.
  Both live in the one surface the shade cannot dim. **No always-visible floating handle in v1** — a
  dark-adapted eye does not want a bright dot in it, and it lands in every screenshot.

The panel needs its own safety rule, opposite to the shade's: it *is* touchable, so it blocks touches
underneath it. Size its window to the panel, never `MATCH_PARENT`, and dismiss on an explicit close
plus an inactivity timeout rather than tap-outside.

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

## Phase 5 — Ship shape

Mostly the *Before the first upload* list already in [`DOD.md`](DOD.md) — the mark, the listing, the
privacy policy, the keystore, the repository setup, the Play secrets, and the `applicationId`
decided deliberately. Two things belong here that are not on it yet:

- **A Support screen.** `ui/support/` already holds the licences half. The rest is a rate-on-Play
  link and `#bug` / `#feature` mail hand-offs, close to a lift from Binky's `SupportHandoff.kt` — a
  design that already cost a device session to get right (Gmail silently ignores `EXTRA_SUBJECT` for
  `ACTION_SENDTO`, so subject and body travel percent-encoded in the `mailto:` query string).
- **Resolve the tip.** `CLAUDE.md` and `README.md` both promise "an optional one-off tip that unlocks
  nothing." Binky investigated exactly that and withdrew it: Play's Payments policy exempts only
  *tax-exempt* donations, and §4 forbids leading users to any other payment method — StreetComplete,
  free and open source, was rejected under it. **Gloam is currently promising something it probably
  cannot ship.** Either write the ADR that removes the promise, or re-read the policy and prove it
  wrong; do not leave it in the file loaded every session.

Languages are not a phase of their own. `scripts/translation-gate.py` makes completeness a merge
gate, so English and Polish stay in step branch by branch. Adding a third is an opt-in, and the day
it happens it gets a phase file.
