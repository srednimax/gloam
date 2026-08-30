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
- [ ] **Phase P** — The pipeline ← *parallel to everything; no code; starts now*
- [ ] **Phase 1** — The mechanism is complete
- [ ] **Phase 2** — Safe to hand over ← **the door: closed testing opens here**
- [ ] **Phase 2b** — As dark as it goes
- [ ] **Phase 3a** — The controls, from the launcher
- [ ] **Phase 3b** — The panel *(carries a go/no-go — see the phase)*
- [ ] **Phase 4** — It turns itself on and off
- [ ] **Phase 5** — Ship shape

**Every phase in this list is split along cost or along kind.** That is not a stylistic tic: each of
Phases 2, 3, 4 and 5 originally bundled a cheap item with an expensive one, or a feature with the
safety work it depends on, and in every case the bundle hid which half was blocking. The rule that
fell out of it is the one worth carrying forward — **when the two halves of a phase have wildly
different costs, they are two phases**, however neatly one sentence describes them both.

---

## Five rules that cut across every phase

### 1. The door is Phase 2, not Phase 5

**The first upload is gated on the app being safe to hand to a stranger, not on it being finished.**
Phase 5 is therefore split rather than deferred: the *pipeline* half is Phase P and happens now, and
the *polish* half — listing copy, feature graphic, privacy prose — stays last, which is what "build
first, polish later" has always meant here.

Two things push the door early, and they are not equally certain:

- **Closed testing needs 12 testers opted in continuously for 14 days** before production access
  exists. That is potentially the longest lead item in this document, it cannot be compressed by
  working harder, and it depends on other people replying. **Recruiting starts in Phase 1.**
  The rule applies to personal developer accounts registered after Nov 2023; **this account was
  created in 2026**, so no exemption is available on age, and an organisation account is not in play.
  What is *not* yet confirmed is whether the requirement is per-app or a one-time account unlock the
  existing live app already satisfied. **Phase P answers this**, because the Console shows the app's
  access status the moment the entry exists. Until it answers, assume the requirement applies —
  being wrong that way costs twelve recruiting conversations, and being wrong the other way costs
  fourteen days discovered at the worst possible moment.
- **Phases 3 and 4 both carry open questions that only users can answer.** Building them before
  anyone has used the app means answering those in a room with one person in it. This reason is
  independent of the calendar and survives whatever Phase P reports back.

### 2. What the door freezes

Phase 2 is a one-way door for more than testing. The moment a build sits on twelve strangers'
phones, four things stop being free to change:

| Frozen at the door | Why | State |
| --- | --- | --- |
| `applicationId` | Registration is keyed to the signing key and cannot be re-pointed | Ratified; **Phase P must land before the door** |
| `minSdk` | Raising it strands existing installs on the last build that fitted them (ADR-0008) | Settled at 33 |
| Every DataStore key **that has been written** | A key on a real phone cannot be renamed or removed without consequence | `onboardingDone` is the open one — Phase 2 |
| The escape-hatch inventory | Ultra dark is gated on it, and the gate has to mean the same thing forever | Phase 2 |

**Anything cheap now and expensive later is pulled in front of the door.** In practice that is
`DOD.md`'s scaffolding cleanup: four pieces of the repo describing features the app does not have,
one of which is a stored preference key.

**The version number is not one of these.** `bump-minor-pre-major` is on and the app ships as 0.x;
**1.0 is deliberately not a milestone in this plan** and nothing will produce it on its own. ADR-0001's
parked checklist, which was written against a "from 1.0" boundary, is therefore re-triggered on
**the first build that reaches a user's phone** — the door — because that boundary is one that will
actually arrive.

### 3. How a phase proves itself

Most of the risk in this plan is device behaviour rather than logic, and none of *that* is reachable
by a unit test. So:

- **Every `phase-N.md` ends with a measurement block**: what was read off the device, and the command
  that read it. Not "it looked right".
- **The phone is the only place nits are measured.** It is a Redmi `amethyst`, HyperOS, Android 16,
  **API 36** — the top of the supported range.
- **One API-33 AVD covers the rest** (ADR-0008). Not a test suite: a "does it launch, does the window
  appear, does the permission flow work" pass at the end of each phase.

**But "most" is not "all", and the exceptions are the safety constants.** Three things this plan
introduces are pure functions with no Android in them, and each one fails in exactly the direction
the app exists to prevent:

- **The ramp** (ADR-0010). Dim level 0–100 → a backlight value and a shade alpha. Reach `0.0f` and
  that is `BRIGHTNESS_OVERRIDE_OFF` — the backlight off, over a live touchscreen. Exceed the shade
  cap and the way out is behind the thing you need to get out of.
- **The schedule window** (Phase 4). On at 22:00, off at 07:00 *crosses midnight*. Get the comparison
  wrong and the shade never comes on, or never goes off.
- **Earlier-deadline-wins** (Phase 4). Resolving two deadlines against a clock, which the phase calls
  load-bearing because it is the exact failure being designed out.

So the rule has two clauses. **Device behaviour is proven by measurement; the pure functions that
compute or bound safety values are proven by test, and the phase that introduces one introduces its
test.** That is three tests across the whole remaining roadmap, not a testing culture — and it is
three tests standing in front of the two constants `CLAUDE.md` calls load-bearing. The top of the
ramp is not visible on a device until it strands somebody.

### 4. Ask for nothing before the feature that needs it

By the end of this plan Gloam asks the user for four things, and three of them are hand-offs to
settings screens whose result cannot be read back reliably — `appops` has already been caught lying
about `SYSTEM_ALERT_WINDOW` on the test phone. So the sequence is a rule, not a phase:

**Every ask waits for the moment the feature needing it is switched on. The phase that introduces an
ask owns its hand-off, its re-read, and what the user sees when the answer is no.**

| Ask | When | Owner |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | First run — there is no app without it | Phase 0 |
| `POST_NOTIFICATIONS` | Immediately after, **before the first `startShade()`** | Phase 1 |
| Xiaomi autostart | When reboot restore first matters | Phase 2 — **re-read**, not re-asked, in Phase 4 |
| Battery-optimisation exemption | Only when the user enables scheduled-on | Phase 4 |

**The third clause is the one that was missing, and it is the one that generates support mail.**
Three of these four fail *silently*: a denied notification permission leaves the shade running with
no Stop action, a denied autostart leaves reboot restore never firing, a denied battery exemption
leaves scheduled-on never firing. In each case the app knows and the user does not. A re-read tells
the app; nothing tells the person holding the phone.

**The same logic applies to side effects, not only to permissions.** Phase 1's backlight override
makes the user's own brightness slider go inert (ADR-0010) — a system control that appears broken,
caused by us. The phase that introduces the behaviour owns saying so.

### 5. Where the answers come from

Rule 1 leans on user feedback for two entire phases, so the channel carrying it is load-bearing and
is named here rather than assumed.

- **Phases 3 and 4's open questions are answered by asking the twelve testers directly.** They are
  preference questions, not bug reports — whether a `ContentObserver` reinterpretation is too clever,
  what auto-off should default to — and those need prompted answers from people you can reach, not
  unprompted mail from whoever bothers.
- **The in-app route exists from the door anyway.** The Support screen's `#bug` / `#feature` mail
  hand-offs move into Phase 2, because a tester with no route to report is a tester whose 14 days
  produce nothing. The rate-on-Play link and the tip stay in the polish half, where there is a
  listing to rate and an audience to tip.

---

## Phase 0 — Skeleton, palette, and a shade that works ✅

The template's data layer is gone (ADR-0007), the dusk palette is generated and passes its contrast
checks, and `shade/ShadeService.kt` draws a real overlay that survives leaving the app. One screen,
one slider, one switch, over DataStore. English and Polish.

This is where the app is today.

## Phase P — The pipeline

**No code, no phase file, and it runs alongside whatever else is being built.** It is a phase only so
that it has an owner and a number; it was previously four bullets inside Phase 5 and therefore
belonged to nobody.

Developer identity verification is **already done** — the account exists and has a live app — which
removes the one step with an external queue and the reason this was ever a schedule emergency. What
remains is short and entirely local:

1. **The upload keystore**, outside the repo, backed up somewhere that is not this machine.
   It comes first because registration is keyed to the **signing key**.
2. **The Play Console entry and the package registration** for `io.github.srednimax.gloam`.
   All Play packages must be registered by **30 September 2026**, and under-50-install names are
   first-come, first-served — so the argument for this `applicationId` (ADR ratified in `DOD.md`) is
   itself an argument for registering it early rather than defending it later.
3. **Read back the app's access status**, which is what settles rule 1's open question about whether
   the 12×14 closed test applies to this app or was a one-time account unlock.

The Play secrets, the service account and the repository setup are the same list `DOD.md` has always
carried; they belong here too, and none of them blocks Phase 1.

**This must complete before the door**, not merely "early" — `applicationId` is one of the four
things Phase 2 freezes.

## Phase 1 — The mechanism is complete

Gloam currently subtracts light one way, by drawing black over the screen. It should do it three
ways, and the other two are the difference between "another dimmer" and the thing the README
promises.

**Entry gate, before anything else in this phase: ask for `POST_NOTIFICATIONS`, and confirm on the
phone that the notification and its Stop action actually appear.** Phase 1 produces the darkest state
the app will ever reach short of ultra dark — backlight at its floor *and* the shade at its cap — and
the ongoing notification is the documented way out of it. On Android 13+ that notification does not
exist until this is granted, and today nothing calls `work/NotificationPermission.kt`. There is also
a platform reason to do it first: the system suppresses or blocks touches on non-system overlay
windows while a permission dialog is up, so the ask may simply not work from a screen sitting under
the shade. Measure that rather than assume it.

- **Backlight.** `CONTEXT.md` has had the word since the beginning and nothing reads it. Measured on
  the phone 2026-08-30: `WindowManager.LayoutParams.screenBrightness` on the shade window takes the
  panel from **500 nits to 6.64 nits**, the display floor is **2.0 nits**, and it costs **no
  permission** — the system reverts it when the window goes away, so a ROM kill cannot strand
  anyone. `WRITE_SETTINGS` is rejected on that comparison alone.
- **Warmth.** Also already in the vocabulary, also unbuilt. The shade stops being one `View` and
  becomes a `FrameLayout` with two — black at the dim level, amber at the warmth — still one window,
  so every safety *flag* is untouched. Deliberately done now rather than retrofitted.
- **The range.** With the backlight at its floor *and* the shade at its cap, the app finally reaches
  the place it exists to reach.

**Settled (ADR-0010): one dim level, one ramp, in a fixed order.** The first stretch of the slider
walks the backlight down to `MIN_BACKLIGHT`; the remainder raises the shade to `MAX_SHADE_ALPHA`. The
backlight half is a **toggle, not a second slider**, default on. `MIN_BACKLIGHT` is a safety
constant beside `MAX_SHADE_ALPHA` and for the mirror-image reason: `screenBrightness = 0.0f` is
documented as `BRIGHTNESS_OVERRIDE_OFF`, and a ramp reaching it hands the user a black screen with a
live touchscreen. **100 means "as dark as currently allowed"**, so Phase 2b's ultra dark extends the
same slider rather than adding another.

Three things this phase owns that are easy to read as somebody else's job:

- **The safety invariant moves from a view to the composite.** `MAX_SHADE_ALPHA` is a bound on one
  `View`'s alpha, and after warmth there are two. The cap can hold on both children while the
  composite defeats it: black at 0.95 leaves 5% of the content visible, and a heavy amber wash over
  that 5% is a screen where nothing underneath can be read — reached without either view exceeding
  its own cap. The escape hatches survive regardless, since they sit above
  `TYPE_APPLICATION_OVERLAY`; what is lost is the user's ability to see their own screen well enough
  to navigate to any of them, which is what the cap buys. **The phase that adds the second layer
  amends the invariant**, which means editing `CLAUDE.md`'s house rule and `CONTEXT.md`'s claim that
  warmth is *independent* of dim level, plus a third safety constant bounding the warmth layer.
  Recorded as a dated amendment on ADR-0010.
- **Saying the inert brightness slider out loud** (rule 4). ADR-0010 measured that the user's own
  brightness control moves but does not apply while the override is live, and assigned "the UI has to
  say this out loud" to nobody. It is this phase's, because this phase causes it. A user whose
  brightness slider stops working will report that Gloam broke their phone, and nothing currently
  connects that symptom to the *also lower the screen brightness* toggle that turns it off.
  Whether Gloam should go further and watch `SCREEN_BRIGHTNESS` with a `ContentObserver` stays open,
  in Phase 3.
- **The ramp's bounds test** (rule 3). Across all 101 inputs the ramp never emits a backlight below
  `MIN_BACKLIGHT`, never a shade alpha above `MAX_SHADE_ALPHA`, and never a composite past the new
  warmth bound.

**Two measurements to take here for later phases' benefit**, both off the same command —
`dumpsys display | grep -A2 OverrideBrightnessStrategy`, which prints
`mWindowManagerBrightnessOverride` and an `…OverrideTag` naming the package that owns it:

1. **When a second window appears above the shade, whose override applies?** This is Phase 3b's
   go/no-go: if a touchable window above the shade seizes the override and cannot be made to match
   it, the panel's live preview is broken by construction and the panel should be cut rather than
   built. Finding this out here costs ten minutes; finding it out in Phase 3 costs a design.
2. **Does the override hold against an app that sets its own `screenBrightness`?** Video players do
   exactly this — swipe-to-dim *is* that API — and watching video in the dark is a stated use case
   alongside reading. The measurement recorded on 2026-08-30 was taken against the shade alone.

**Recruiting the twelve testers starts here**, as insurance against Phase P reporting that the 14-day
requirement applies.

## Phase 2 — Safe to hand over

**This is the door.** Everything in it answers one question — *must a stranger have this before they
can be handed the app?* — and nothing in it is a feature. Ultra dark used to live here and does not
any more: it *depends* on the safety work, which is not the same as being part of it, and it is the
riskiest thing in the document. It should meet twelve people while you are watching, which is what
Phase 2b is for.

- **First run, owned end to end.** The ask-late rule deliberately scatters three asks across Phases
  0, 1 and 2, which is right and leaves nobody owning the coherence of a new user's first two
  minutes. That is exactly what twelve strangers judge at the door. This phase owns it, and owns the
  **denial state of all four asks** per rule 4 — three of which fail silently today.
- **`onboardingDone` is used or deleted.** It has sat in `AppPreferences` read by nothing since Phase
  0, because the key exists and the concept had no owner. It is also a *stored* preference key, so
  the door freezes it (rule 2) — this is the last phase in which the choice is free.
- **Auto-off**, moved here from Phase 4. Phase 4's own text separates its two halves by cost, and
  this is the cheap one: *"a coroutine on the scope the service already owns"*, needing **no
  permission at all**. It is also the answer to the plan's named fear — *"I hate when other apps have
  no auto disable and the next day I cannot see anything on my phone"* — and testers keep this build
  for fourteen continuous days. A phase about not trapping people that defers the safety net by two
  phases is not that phase. It costs no design debt to move: Phase 4's model is one stored
  `offAtMillis` with the earlier deadline winning, so shipping auto-off alone gives that key a single
  writer and Phase 4 adds the second with nothing to rework. Discrete durations spanning an episode
  to a film, defaulting on, with a **Never** option — `CONTEXT.md` reserves *off* for the shade not
  being drawn, so "auto-off: off" is a sentence this vocabulary cannot say.
- **The Support screen's mail hand-offs**, moved here from Phase 5's polish half (rule 5).
  `ui/support/` already holds the licences half. Close to a lift from Binky's `SupportHandoff.kt` —
  a design that already cost a device session to get right (Gmail silently ignores `EXTRA_SUBJECT`
  for `ACTION_SENDTO`, so subject and body travel percent-encoded in the `mailto:` query string).
- **Reboot restore, with its autostart ask.** `RECEIVE_BOOT_COMPLETED` is a normal permission with no
  prompt and Android 15's BOOT_COMPLETED blocklist does not include `specialUse`, so reading
  `shadeRunning` at boot and putting the shade back is allowed *by the platform*. It is not allowed
  by the ROM: ADR-0003 observed that without autostart HyperOS does not start the process for a
  broadcast at all — **not `BOOT_COMPLETED`**. So the autostart ask moves here and attaches to the
  feature that first needs it (rule 4); Phase 4 re-reads the grant rather than introducing it, which
  it had to do anyway since the grant lapses on its own. Without the move, this phase would ship a
  feature unmeasurable on the only device in the loop except by hand-granting a permission users have
  not been asked for — which would break rule 3 as well as rule 4.
- **The freeze list closed out** (rule 2), including `DOD.md`'s four pieces of scaffolding.

Worth knowing throughout: the pull-down notification shade, the lock screen and system dialogs are
**above** `TYPE_APPLICATION_OVERLAY` and can never be dimmed. That is a platform ceiling, and it is
also the reason a guaranteed-bright escape hatch exists at all.

**When this phase closes the app is safe to hand over. Closed testing opens here.**

## Phase 2b — As dark as it goes

Behind the door on purpose: the riskiest feature in the plan, shipped *into* a running closed test
rather than in front of it.

- **Ultra dark**, going past `MAX_SHADE_ALPHA`, **gated on a live hatch check re-read continuously**.
  Not on "the Phase 1 ask succeeded" — that is a historical grant, and a grant is not a permanent
  fact. `POST_NOTIFICATIONS` can be revoked later, and more quietly the user can disable the shade's
  *notification channel* without touching the permission at all, at which point the permission still
  reads as granted, no notification exists, and the gate would wave ultra dark through. It is a real
  gate, not a warning dialog, and it reads what is true now.
- **The Quick Settings tile**, which is safety equipment rather than reach and therefore travels with
  the feature that needs it. Work out the hatch inventory at high alpha and it is exactly three
  surfaces, all in the undimmable layer: **the ongoing notification's Stop action, a QS tile, and the
  power menu.** Nothing else. The QS gear icon is a trap — bright and tappable, but Settings opens
  *beneath* the shade and is invisible, so force-stop is not reachable. The tile needs no permission
  and cannot be silenced by a notification-channel setting, so it fails independently of the
  notification; that independence, not raw strength, is the argument for having both. `minSdk` 33 is
  also exactly the level at which the app can ask the system to add the tile rather than hoping the
  user finds it.

## Phase 3a — The controls, from the launcher

Decided in the brainstorm: the compact screen and the floating panel are **one `DimControls()`
composable rendered in two hosts**. True of the composable — and the reason 3a and 3b are separate
phases is that it is false of the hosts, where all the cost lives.

**A floating-window `MainActivity`.** Small, dialog-shaped, switch and sliders only, with a setting
that expands it to the full app. A theme, an activity flag and a preference: cheap, boring, known to
work.

**Summoned from the ongoing notification**, with the Quick Settings tile from Phase 2b as a second
door — both already living in the one surface the shade cannot dim. **No always-visible floating
handle in v1**: a dark-adapted eye does not want a bright dot in it, and it lands in every screenshot.

**Open, and answered by the testers (rule 5):** while the backlight override is live the user's own
brightness slider moves but does nothing — it applies on release instead (measured). Phase 1 already
owns *saying so*. What is still open is whether Gloam should go further and watch `SCREEN_BRIGHTNESS`
with a `ContentObserver`, reinterpreting a change as "I want more light" — or whether that is too
clever.

## Phase 3b — The panel

**The one item in this roadmap with a documented kill condition**, because it is the one item the
platform can kill rather than you.

The **panel** is a second, *touchable* overlay window above the shade — `CONTEXT.md` names it
separately because it is not the shade and its safety rule is the opposite one. Its value is the live
preview: the slider moves the dim over the real content instead of over Gloam's own screen. That
value is real and 3a does not substitute for it — a translucent activity launched into its own task
does not reliably keep another app's screen visible behind it, so what the user would be judging the
dim against is Gloam's own UI, which for video is a bad proxy since your UI is dark and the content
is not.

Its cost is the highest in the document. Compose has no Activity in a raw `WindowManager` window, so
`ViewTreeLifecycleOwner`, `ViewTreeViewModelStoreOwner` and `ViewTreeSavedStateRegistryOwner` are
attached by hand and the lifecycle has to actually reach RESUMED or nothing recomposes.

Its own safety rule is opposite to the shade's: it *is* touchable, so it blocks touches underneath
it. Size its window to the panel, never `MATCH_PARENT`, and dismiss on an explicit close plus an
inactivity timeout rather than tap-outside. Two more things it must carry: `FLAG_NOT_FOCUSABLE` (a
window can receive touches without taking keyboard focus — `FLAG_NOT_TOUCHABLE` is the one that
passes them through), and **the same `screenBrightness` value as the shade**, so that opening the
controls cannot snap the screen back to full brightness.

**Go/no-go, decided on Phase 1's measurement, not when this phase opens.** If a window above the
shade seizes the backlight override and cannot be made to match it, live preview is broken by
construction and this phase is cut rather than attempted.

## Phase 4 — It turns itself on and off

Auto-off shipped in Phase 2. What is left is the expensive half — the part needing new machinery,
because nothing of ours is running to do it.

- **The schedule is a window** — on at 22:00, off at 07:00. One pair, not two independent switches.
  The window **crosses midnight**, which is rule 3's second test.
- **One stored `offAtMillis`, and the earlier deadline wins** — rule 3's third test. Auto-off gave
  this key one writer in Phase 2; this phase adds the second. Storing an absolute instant rather than
  a countdown is load-bearing: a HyperOS kill plus a `START_STICKY` restart then resumes the right
  deadline instead of silently restarting the clock, which is the exact failure being designed out.
- **Scheduled-on** uses an **inexact `setAndAllowWhileIdle` alarm plus the battery-optimisation
  exemption** — that exemption is on Android's own list for starting a foreground service from the
  background, and `work/BatteryExemption.kt` already exists. **Not `SCHEDULE_EXACT_ALARM`**: it needs
  a Play declaration form and a dimmer does not qualify as an alarm or clock app.
- **Xiaomi autostart is re-read here, not asked for here** (rule 4). Phase 2 introduced the ask with
  reboot restore; this phase confirms the grant has not lapsed, because it does lapse on its own.

**No table.** The whole of this phase is four values — on time, off time, enabled, `offAtMillis` —
which is four DataStore keys with their defaults declared beside them. See *Not in this plan* for the
test that decides when that stops being true.

## Phase 5 — Ship shape

The pipeline half is Phase P. What is left is the polish half, and it is genuinely last: the mark,
the listing copy, the privacy policy, the screenshots. Plus the rest of the Support screen — the
rate-on-Play link, and:

- **The tip, which ships** — see ADR-0009. An earlier reading had this as probably unshippable; the
  live policy text says the opposite. Play's §3.2 treats a tip where 100% reaches the creator and
  nothing unlocks as a **peer-to-peer payment** for which its billing system is not required, and the
  §4 anti-steering rule carries an explicit exception for §3. So it is an external link on the
  Support screen, no billing dependency, and the promise in `CLAUDE.md` and `README.md` stands.

Languages are not a phase of their own. `scripts/translation-gate.py` makes completeness a merge
gate, so English and Polish stay in step branch by branch. Adding a third is an opt-in, and the day
it happens it gets a phase file.

**The version stays 0.x** (rule 2). There is no 1.0 in this plan and nothing will produce one.

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
- **An inactivity-based auto-off**, as distinct from the elapsed-time one in Phase 2. It is not a
  preference, it is foreclosed: the shade carries `FLAG_NOT_TOUCHABLE` so that every touch passes
  through, which means **Gloam can never observe a touch**. Detecting idleness would need an
  accessibility service, which is the previous bullet. Elapsed time is forced, not chosen.
- **A home-screen widget**, **a Tasker / intent API**, **colour filters beyond warmth**,
  **multi-display support**, and the **always-visible floating handle** (decided against in Phase 3a).

None of these is forbidden forever. Each one reopens ADR-0007 deliberately, with a migration story
attached — which is exactly what ADR-0007 says adding a table should cost.
