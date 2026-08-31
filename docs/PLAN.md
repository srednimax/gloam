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
- [x] **Phase 1** — The mechanism is complete
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
the *polish* half stays last, which is what "build first, polish later" has always meant here.

The split moved once, on 2026-08-30. **Listing copy, the feature graphic and the screenshots are not
polish** — Play will not open a *closed* test without a complete listing, and the closed test is what
the 12-tester window below runs on. They sit before the door with everything else rule 2 freezes.
What genuinely stays last is the privacy prose's final pass, the Support screen's remainder, and the
mark's refinement.

Two things push the door early, and they are not equally certain:

- **Closed testing needs 12 testers opted in continuously for 14 days** before production access
  exists. That is potentially the longest lead item in this document, it cannot be compressed by
  working harder, and it depends on other people replying. **Recruiting starts in Phase 1.**
  The rule applies to personal developer accounts registered after Nov 2023; **this account was
  created in 2026**, so no exemption is available on age, and an organisation account is not in play.
  **Confirmed 2026-08-30, in the Console, with the app entry created: the requirement applies to
  Gloam.** It is not a one-time account unlock that the existing live app satisfied, so the
  assumption this plan was built on was the right one and nothing about the sequence changes.
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

**Detail: [`phase-1.md`](phase-1.md).**

Gloam currently subtracts light one way, by drawing black over the screen. It should do it three
ways, and the other two are the difference between "another dimmer" and the thing the README
promises: the **backlight** walked down to its floor first, and **warmth** tinting the shade amber.
Both words have been in `CONTEXT.md` since the beginning and nothing reads either. With the backlight
at its floor *and* the shade at its cap, the app finally reaches the place it exists to reach.

Settled in ADR-0010 **and its three amendments**: one dim level, one ramp, in a fixed
order, with the backlight half a toggle rather than a second slider. The first amendment is the half
that matters to the schedule, because it hands this phase three things it is easy to read as somebody
else's job — the safety invariant moving from a `View` to the composite once there are two layers,
saying out loud that the override leaves the user's own brightness slider inert, and the ramp's
bounds test. The second records what the phone said when this phase was planned, and it is why the
phase carried **a checkpoint that could veto the backlight half**, because the estimate that half
starts from had to be verified against the phone before it shipped. **It passed** (checkpoint B,
2026-08-30), and the third amendment records why the second one's alarming reading was an artifact
of the screen's own inactivity dimming rather than a property of the device.

It opens with an **entry gate**: ask for `POST_NOTIFICATIONS` before anything else, because this
phase produces the darkest state the app will ever reach short of ultra dark and the ongoing
notification is the documented way out of it.

It runs as **five checkpoints, each its own merge**, so the entry gate lands ahead of the device work
it does not depend on. Two of its readings are taken for later phases' benefit — **Phase 3b's
go/no-go falls out of one this phase needs anyway** — and **recruiting the twelve testers starts
here** as insurance against the 14-day requirement.

## Phase 2 — Safe to hand over

**Detail: [`phase-2.md`](phase-2.md).**

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

The pipeline half is Phase P, and the listing, feature graphic and screenshots moved *before the
door* on 2026-08-30 (rule 1) because a closed test cannot open without them. What is left here is
genuinely last: the mark's refinement, the privacy prose's final pass, and the rest of the Support
screen — the rate-on-Play link, and:

- **The tip, which ships — but not inside the app.** See ADR-0009 **and its 2026-08-30 amendment**,
  which is the part that decides what Phase 5 builds. Play's §3.2 treats a tip where 100% reaches the
  creator and nothing unlocks as a **peer-to-peer payment** for which its billing system is not
  required, and §4's anti-steering rule carries an explicit exception for §3 — so on the policy text
  an in-app link is permitted. Enforcement has rejected apps for it anyway, StreetComplete included,
  and that rejection extended to a link to the *project home page* because that page carried donation
  information. So v1's Support screen carries **no tip link**, the tip lives on the repository and
  the Pages site, and nothing the app links to carries it either. No billing dependency, no Console
  product, and the promise in `CLAUDE.md` and `README.md` stands — it just is not collected from
  inside the app until the app is live and there is something to appeal from.

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
