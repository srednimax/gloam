# Phase 2 — Safe to hand over

**This is the door.** The detail for one phase, written when it opened. Sequence lives in
[`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md); the decisions this phase
implements or amends are [ADR-0003](adr/0003-two-scheduling-mechanisms.md) (the autostart
observation), [ADR-0004](adr/0004-english-base-with-an-in-app-language-switcher.md) (the below-13
backport, now dead weight) and [ADR-0007](adr/0007-gloam-stores-settings-not-records-so-it-has-no-database.md)
(what is left in the toolchain that still believes in a database).

**What closing this phase means.** Nothing in it is a feature in the sense Phase 1 was. Every item
answers one question — *must a stranger have this before they can be handed the app?* — and the test
of the phase is not that the app does more but that it stops doing three things silently: taking a
shade the user forgot about into the next morning, losing the shade on a restart with nothing said,
and failing an OEM permission the user was never told about. When it closes, twelve people who did
not read this repository can be given the build.

**It is also the last phase in which four things are free.** `applicationId`, `minSdk`, every
DataStore key that has been written, and the escape-hatch inventory (PLAN rule 2). Three of those are
settled; this phase writes the fourth down and adds the last two stored keys the app will get before
strangers hold it.

---

## 0. Two things blocking the door that are not code, and one of them is now closed

Both were calendar rather than effort, both were predicted by `DOD.md`, and the pipeline half is done.

**The publish pipeline is green, on its fourth attempt, since 2026-08-31.** `Publish to Play` first
ran against tag `v0.3.0` and went red — not on the credentials, which is the failure `DOD.md` braced
for, but one step further in:

```
Google Play Android Developer API has not been used in project 118298064751 before
or it is disabled.  Enable it by visiting
https://console.developers.google.com/apis/api/androidpublisher.googleapis.com/overview?project=118298064751
```

That was the good failure. The service account authenticated, the bundle built, was signed and was
verified; the Cloud project simply had `androidpublisher.googleapis.com` switched off. Enabling it in
project `118298064751` and re-running with `workflow_dispatch` closed it: **run `33412104006`,
attempt 4, which put 0.3.0 on the internal track in 2m53s.**

**The part worth carrying forward is the trigger rather than the API.** Four attempts cost four
re-runs and not one extra version, because `workflow_dispatch` is there for exactly this. A
propagation delay and a broken credential look identical from outside; what separates them is whether
re-running fixes it, which is only cheap to find out when retrying is free.

**So `PLAY_SERVICE_ACCOUNT_JSON` is proven and the pipeline is no longer what the door waits on.**
What it waits on is the item below.

**Recruiting the twelve testers has not started.** Phase 1 owned starting it (`phase-1.md` §10) and
`DOD.md` still carries the box unticked. Fourteen *continuous* days is the longest lead item in the
plan and it depends on other people replying. Recruit more than twelve. If this phase's code lands
before the recruiting does, the door is held open by the slower of the two and it will be this one.

---

## What is in, and what is deliberately not

**In:** the first run as one flow rather than three unrelated hand-offs; the denial state of every ask
that exists; the escape-hatch inventory, written down as a definition and as one predicate that
2b's gate will read; auto-off; reboot restore and the autostart hand-off; the Support screen's mail
route; the freeze list and the scaffolding; the emulator harnesses, which currently describe an app
that does not exist.

**Not in, and the phase that owns each:** ultra dark and the Quick Settings tile (2b), the floating
host and any `ContentObserver` on `SCREEN_BRIGHTNESS` (3a), the panel (3b), the schedule and the
battery-optimisation ask (4), the mark's refinement and the tip (5).

**The tempting one is the Quick Settings tile.** Phase 1's R8 found that HyperOS hides the
notification's *Stop* action behind a long-press, and this phase writes the escape-hatch inventory
down — so the tile looks like it belongs here. It does not: the tile is what makes *ultra dark* safe,
and ultra dark is the riskiest feature in the plan. Both ship into a running closed test where twelve
people can be watched, which is the whole argument for Phase 2b existing. What this phase owes the
tile is the definition it will be gated against, not the tile.

**One shape to keep, without building it.** The chips and the "turns off at" line stay on the dim
*screen* this phase, outside whatever later becomes `DimControls()` — **a deferral, not a verdict.**
Putting them beside the sliders now, on the assumption they travel together, is how the extraction
gets harder rather than easier; `DimScreen`'s own comment already says the same about the sliders,
that pulling out a composable with one caller is guesswork about the second caller's shape.

**Phase 3a decides whether the floating host carries the deadline, and the twelve can answer it**
(rule 5). The argument is live in both directions: the floating host is by construction the surface
reached *while the shade is up, in the dark, without opening the app*, which is exactly where "turns
off at 23:40" is worth reading and "give me two more hours" is worth tapping. On product grounds the
deadline has a better claim to that host than the warmth slider does. That is 3a's call on 3a's
evidence, not this phase's.

---

## Checkpoints

**The phase is one phase and seven merges.** Each leaves the app working, ships its copy complete in
both locales — `scripts/translation-gate.py` is a merge gate, so that is enforced rather than
remembered — and is a point at which the phase could stop without stranding anything.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | The first run, and the escape hatch named | `refactor:` | nothing |
| **B** | Auto-off | `chore:` + `test:`, then `feat:` | A (it reads the deadline the same way) |
| **C** | Reboot restore, and the autostart hand-off | `feat:`, then the readings | **B** — restore honours the deadline |
| **D** | Support: the mail hand-offs | `feat:` | nothing |
| **E** | The scaffolding and the freeze list | `chore:` ×3 | C (it makes half of `BatteryExemption.kt` live) |
| **F** | The emulator harnesses: real tests or none | `test:`, `ci:`, `chore:` | B, C, D — it walks their screens |
| **G** | The documents | `docs:` | everything above |

**No checkpoint here is a gate the way Phase 1's B was.** Nothing in this phase can be vetoed by a
measurement, because nothing in it is betting on a device behaviour that has not been read. C carries
the closest thing to one — if the ROM will not start the process for a boot broadcast even with
autostart granted, reboot restore ships as an honest "this phone will not let Gloam do this" rather
than as a feature — and that is a copy outcome, not a cut one.

---

## 1. The first run, and the four asks

By the end of this plan Gloam asks the user for four things. Three exist today; the fourth is Phase
4's and rule 4 forbids pulling it forward.

| Ask | Introduced | On a refusal, today | On a refusal, after this phase |
| --- | --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | Phase 0 | the explainer replaces the screen — a live read, self-clearing | unchanged, and it now names both asks at once |
| `POST_NOTIFICATIONS` | Phase 1 | the warning banner — a live read, with the right button per state | unchanged; §2 gives its predicate a name |
| Xiaomi autostart | **this phase** | **nothing. Reboot restore silently never fires** | a permanent row in Settings that says so, and says the app cannot check |
| Battery exemption | Phase 4 | not asked | not asked. The shape above is what it inherits |

Two of the three are already right, and they are right in the same way: **they are live reads of what
the system reports now, not memories of how an ask ended.** Phase 1 argued that at length for the
notification warning and the argument generalises — every one of these is a switch on a settings
screen the app deliberately hands the user off to, so every one of them can change while the app is
backgrounded and none of them may be cached across that trip.

### What is actually missing is the framing, not a state machine

`DimScreen` already does the right thing in each state and never says what the sequence *is*. A new
user meets two hand-offs inside twenty seconds — *Open settings* for the overlay permission, then a
system dialog for notifications the moment they tap Start — with nothing connecting them. That is
what twelve strangers judge, and it is fixable with copy and ordering rather than with a flow.

**The overlay explainer becomes the first-run explainer.** Same trigger — shown while `canDraw` is
false, which is a live read and therefore self-clearing and self-restoring — but it names both asks
up front and says which one comes when. After the grant it disappears for good, because the condition
that raised it is gone, and it comes back the day the user revokes the permission, which is correct.

### `onboardingDone` is deleted, and this is the last phase in which that is free

`AppPreferences` declares `ONBOARDING_DONE`, exposes `onboardingDone` and `setOnboardingDone`, and
**nothing in the repository calls either** — verified by grep across `app/src`, `scripts/` and the
documents; the only other hits are the three places in `PLAN.md`, `DOD.md` and `phase-1.md` that
carry it as an open question. So it has never been written, which means no phone holds it, which
means deleting it costs nothing at all today and costs a stranded key forever after the door.

**The case for keeping it would have been a once-only welcome screen, and this phase argues against
one on its own merits rather than on the key's.** Everything a first-run user needs to be told is
tied to a condition the app can read: the overlay permission is missing, the notification cannot
appear, this device does not report a usable backlight. A welcome screen would restate those in a
place that is unreachable the second time — including on the day the user revokes the permission and
needs the explanation again. A remembered "done" is the same stale-outcome mistake the notification
warning was rewritten to avoid, one screen further up.

**One place genuinely wants a memory and it is not this key.** ADR-0003's amendment says Xiaomi's
autostart is *offered once and claimed never*, and "once" implies remembering that you offered.
§4 declines to store that too, and says why.

### Copy

| Key | English | Checkpoint |
| --- | --- | --- |
| `dim_permission_title` | *unchanged* | — |
| `dim_permission_body` | **reworded**: The shade is a layer on top of whatever is on screen, so Android asks you to allow it explicitly. Gloam reads nothing about the apps underneath. When you start dimming it will also ask to show a notification — that notification is how you stop Gloam from anywhere. | A |

Two asks named in one paragraph, in the order they arrive, with the reason for the second stated as
what it buys the user rather than as a permission name.

**"When you start dimming", not "when you first start dimming".** The explainer's whole virtue is
that it is a live read and comes back the day the permission is revoked, and on that day the reader
is not a first-run user. One word, and it is true both times.

---

## 2. The escape-hatch inventory — the thing the door freezes

PLAN rule 2 lists four things the door freezes and this is the only one with no owner yet. It has to
be written down *here* rather than in 2b, because 2b gates ultra dark on it and **a gate whose
definition is written by the feature it guards is not a gate.**

### The inventory

A surface counts as an escape hatch only if all three hold. The three clauses are the definition;
the list is what currently satisfies it.

1. **It is drawn above `TYPE_APPLICATION_OVERLAY`**, so no dim level Gloam can reach touches it.
   Measured, both directions: Phase 1's R5 found the notification shade, quick settings and the volume
   dialog all keep our override rather than lifting it, and its R9 found an ordinary app's own
   `screenBrightness` never reaches the panel while ours is up. The system's surfaces out-rank us by
   *hiding* our window, not by out-bidding it.
2. **Reaching it needs no sight of Gloam's own UI.** This is the clause Phase 1's R8 forced. The app's own
   *Stop dimming* button sits **under** the shade at 0.33 nits at maximum dim, so it is not a hatch —
   it is a control that happens to also stop the thing obscuring it.
3. **Its liveness is readable by the app at the moment of the check**, or it is unconditionally
   present. A hatch whose state cannot be read cannot be gated on.

| Surface | Clause 3 | Owner |
| --- | --- | --- |
| The ongoing notification's **Stop** action | `notificationsAllowed() && channelCanAppear(Shade)` — both halves, live | Phase 1, named here |
| The **Quick Settings tile** | needs no permission and no channel; fails independently of the above | Phase 2b |
| The **power menu** | unconditional, and outside the app entirely | the platform |

**One line of that table is weaker than it looks and this phase makes it weaker still.** Clause 3
holds for the power menu, but only for what it actually delivers. It is an unconditional route to an
*undimmed screen*, not to a *control*: `BOOT_COMPLETED` arrives after the first unlock (§4), so the
bright moment the user passes through is the keyguard — before the receiver has run, before the
service is alive, and before any notification exists to act on. What a restart buys after this phase
is sight and a bounded head start, not a permanent undo.

So the honest reading of that row is **unconditional, and bounded in duration rather than
permanent**. **R1's "how long after unlock" is the number behind the bound**, which is a better job
for it than "does it work". §4 argues why it is still enough; 2b reads this paragraph, not the table
alone.

### The predicate

`DimScreen` already computes the notification half of this inline. It moves to one named function in
`shade/`, because two callers want the identical answer and a second copy is how they drift:

```kotlin
/** Whether the shade's escape hatch — the ongoing notification's Stop action — can actually appear. */
fun Context.escapeHatchLive(): Boolean =
    notificationsAllowed() && channelCanAppear(AppChannel.Shade)
```

The warning banner reads it to decide whether to appear at all (the enum keeps deciding *which*
button). Phase 2b's gate reads it, live and continuously, and adds the tile as an independent second
term — `escapeHatchLive() || tileAdded()` — rather than replacing it. **Nothing else reads it
today**, which is a statement about this phase rather than a restriction on later ones.

**2b will need a third caller, and it is worth saying here rather than discovering it there.** §4's
boot receiver restores a shade without consulting this predicate, deliberately — §4 says why. In
this phase that produces a shade with no Stop action over a screen at 0.33 nits: uncomfortable, and
survivable, because R2 calibrated `MIN_BACKLIGHT` so the app's own controls stay legible at maximum
dim in a dark room. Past `MAX_SHADE_ALPHA` it stops being survivable, and **2b's gate cannot see
it** — that gate lives in the UI and the restore path has none. So the gate 2b builds has to cover
the receiver too, and this is the paragraph that says so.

**It is a `Context` extension and not a `ViewModel` property**, for the reason `CLAUDE.md` gives: it
needs a `Context`, and a `ViewModel` holding one is a `ViewModel` outliving its scope.

---

## 3. Auto-off

Moved here from Phase 4 on 2026-08-30, and it is the cheap half of that phase: **a coroutine on the
scope the service already owns**, needing no permission at all. It is also the answer to the plan's
named fear — *"I hate when other apps have no auto disable and the next day I cannot see anything on
my phone"* — and testers keep this build for fourteen continuous days.

It has a second job this phase discovered rather than inherited. Phase 1's R8 found that on HyperOS the
notification's Stop action takes a long-press to reach, and the app's own button is under the shade.
**Auto-off is the hatch that needs no gesture at all**, and until 2b's tile ships it is the only one
that does not depend on the user finding something. That is not the reason it was moved forward, but
it is the reason it defaults on rather than to *Never*.

### The choice, and the vocabulary constraint

`CONTEXT.md` reserves *off* for the shade not being drawn, so **auto-off cannot itself be switched
"off"** — the choice that disables it is **Never**. That is not pedantry: "auto-off: off" is a
sentence meaning the shade stays on, and a settings row that can be read backwards is a settings row
somebody reads backwards.

A fixed set of five, so it is five settings and not a list (`CLAUDE.md`'s cardinality test):

```kotlin
/** How long a shade the user started by hand stays up. A fixed set, so it is a setting, not a list. */
enum class AutoOff(val minutes: Int) {
    Never(0),
    Minutes30(30),
    Hour1(60),
    Hours2(120),
    Hours4(240),
}
```

**Default `Hours2`.** The span the plan asks for is "an episode to a film", and the default has to
lose the fewest arguments in both directions: short enough that a phone dimmed and forgotten recovers
the same evening, long enough not to interrupt a film — which is the one session where a shade
vanishing mid-use is worse than a bright screen. Two hours is the longest value that is still
obviously *not* "the next day", which is the failure being designed out.

`Never` is offered rather than hidden, because a user who reads for six hours and keeps being
interrupted will otherwise conclude the app is broken. It is a choice they make, which is the same
standard `DEFAULT_DIM_LEVEL` and warmth are held to.

**And `Hours2` ships provisional, because `PLAN.md` rule 5 names this exact question as one of the
two the twelve testers exist to answer.** The paragraph above is the best argument available from a
room with one person in it, and it is an argument about a *taste* — while this is the phase that
puts the thing in front of the only people who can turn a taste into evidence. So rule 5 stands
unamended, `DOD.md` carries a line to put the question to them while they are still reachable and
still paying attention, and `Hours2` is what they are asked about rather than what was settled
without them.

**Defaulting *on* is a different question and is not up for a vote.** That is the safety argument two
paragraphs up — until 2b's tile ships, auto-off is the only hatch needing no gesture — and a
preference poll is the wrong instrument for a safety floor.

### The deadline is an absolute instant, and the pure part is tested

`shade/AutoOff.kt`, no Android imports, in the shape `ShadeRamp.kt` established:

```kotlin
/** The instant the shade should come down, or `null` for Never. */
fun deadlineFor(startedAt: Long, choice: AutoOff): Long?

/** Whether a stored deadline has already passed. `null` deadline is never due. */
fun isDue(now: Long, offAtMillis: Long?): Boolean
```

**Absolute rather than a countdown, and that is load-bearing rather than tidy.** PLAN's Phase 4 says
why in the sentence this phase inherits: a HyperOS kill plus a `START_STICKY` restart resumes the
right deadline instead of silently restarting the clock. A stored duration cannot do that — the
restarted service has no idea how long it was dead. Phase 4 adds the second writer to the same value
and nothing about this shape has to be reworked.

### Where it runs, and the clock it is not allowed to trust

A job on the service's own scope, **re-armed by `collectLatest` over `offAtMillis`**. The loop
cannot capture the deadline once, because it changes twice in this phase for reasons other than
starting: the chip rewrite from *now*, and §4's receiver restoring one at boot.

```kotlin
preferences.offAtMillis.collectLatest { offAt ->
    while (isActive) {
        val remaining = offAt - System.currentTimeMillis()
        if (remaining <= 0) {
            // Must finish even though this write cancels the very block making it.
            withContext(NonCancellable) { preferences.endShade() }
            stopSelf()
            return@collectLatest
        }
        delay(min(remaining, DEADLINE_RECHECK_MS))   // 60_000
    }
}
```

**`NonCancellable` is required rather than defensive, and without it the failure is intermittent.**
`endShade()` writes the very value this block is collecting: the write commits, the flow emits,
`collectLatest` cancels the block that is still running — and the cancellation lands *between*
`endShade()` returning and `stopSelf()`. The shade comes down, `shadeRunning` goes false, and the
service stays alive holding an ongoing notification over a screen it is no longer dimming. Putting
`stopSelf()` first does not help either: `onDestroy` calls `scope.cancel()`, which cancels the write
from the other side. Cancellation is only observed at a suspension point, so once the
`NonCancellable` block returns, the plain `stopSelf()` call after it always runs.

**The fire path logs one line at the moment it stops.** R7 reads the lateness off logcat timestamps
and has nothing else to read it from — a notification vanishing is visible but not timestamped.
Logging is not developer *surface*, so it lives in `main/` rather than behind the debug seam.

**The cap is the whole point and it is a platform fact rather than a style.** `delay` on Android's
main dispatcher is a `Handler.postDelayed`, which is scheduled against `SystemClock.uptimeMillis` —
**a clock that stops advancing while the device is in deep sleep.** A single two-hour `delay` on a
phone that slept for ninety minutes of it fires ninety minutes late. Re-reading the wall clock at
most a minute at a time bounds the error to a minute *of CPU-awake time*, which is the most any
mechanism without an alarm can promise.

What that costs while the phone is asleep is nothing: `postDelayed` sets no alarm and holds no
wakelock, so nothing wakes the device — the message is simply overdue when the device next wakes for
some other reason, and the loop notices immediately. What it costs while awake is one comparison a
minute. **This is why auto-off needs no permission and Phase 4's scheduled-*on* needs two**: taking
something down only has to be right the next time the user looks at the screen, and putting something
up has to be right at 22:00.

**Firing while the screen is off is correct, not a bug to guard against.** The shade matters at the
moment the user next looks; taking it down while they are not looking is precisely the behaviour that
makes the morning fine.

### The deadline needs a reader the service cannot provide

Three things touch `off_at_millis` in this phase — `beginShade` arms it, the loop above fires it,
§4's receiver checks it at boot — and there is a fourth case none of them covers. **HyperOS kills the
process and `START_STICKY` does not always bring it back** — and R8a, taken 2026-09-01, is stronger
than that sentence: on this phone it did not bring it back *at all*, in either attempt, so what
follows is the ordinary path rather than the unlucky one. It is the premise this codebase is built
on: `ShadeService`'s own notes say that promise "is worth less than the stored `shadeRunning`
preference". The window dies with the process, `shadeRunning` stays `true`, the deadline stays where
it was, and nothing is alive to notice it pass.

Open Gloam the next morning and the button says **Stop** over a screen with no shade on it, under a
line reading *"Turns off at 00:00"* — nine hours gone. That is the first time this app's two stored
values can disagree *with each other*, which is exactly what §6's `beginShade`/`endShade` pairing
exists to prevent. They are written together and drift apart anyway, because the clock moves and
nobody is watching it.

**The reader is one line in a block that already exists.** `DimScreen`'s `reread()` runs on every
resume beside `canDrawShade()`; it gains `if (shadeRunning && isDue(now, offAt)) endShade()`. That is
a comparison of two stored values against the wall clock — **not** a check of whether a process is
alive, which is the thing `CLAUDE.md` forbids. The stored intent still decides what should be on
screen; this only lets the user's own earlier instruction finish arriving. **R8b is the reading
behind it.**

### What the user sees

Under the Start/Stop button, above nothing:

- **A row of chips**, the same `FilterChip` idiom Settings uses for theme and language. Five values,
  `Never` first.
- **While running, one line: "Turns off at 23:40."** A clock time rather than a countdown, and that
  is a deliberate saving: a countdown has to tick, which means a `LaunchedEffect` re-composing the
  screen every minute for the life of the session, and it has to be formatted into words that plural
  correctly in every locale. A time formatted by `DateFormat.getTimeFormat(context)` is one string
  with one argument, is locale-correct for free, and does not change — so nothing ticks. Its one
  weakness is that four hours from 23:00 reads as "03:00" with no date; at a four-hour ceiling that
  is unambiguous enough in context, and it is recorded here so nobody re-discovers it as a bug.

Changing the chip while the shade is running rewrites the deadline from *now*, not from when the
shade started. That is the reading somebody expects from tapping "2 hours" while looking at a screen,
and it makes the control usable as "give me two more hours" without inventing a second one.

**Nothing else extends the deadline.** Gloam cannot observe a touch — the shade carries
`FLAG_NOT_TOUCHABLE` so that every touch passes through, which is exactly why `PLAN.md`'s *Not in
this plan* forecloses inactivity-based auto-off. Elapsed time is forced, not chosen, and the chips
are the only input.

---

## 4. Reboot restore, and the autostart hand-off

### What the platform allows, and it is more than expected

Three facts, in the order they were checked rather than assumed:

- **`BOOT_COMPLETED` is one of the documented exemptions from the background foreground-service start
  restriction**, along with `LOCKED_BOOT_COMPLETED` and **`ACTION_MY_PACKAGE_REPLACED`**. So starting
  the shade from a boot receiver is allowed by the platform.
- **Android 15's `BOOT_COMPLETED` blocklist does not include `specialUse`.** The blocked types are
  `camera`, `dataSync`, `mediaPlayback`, `mediaProjection` and `phoneCall`, plus `microphone` from
  Android 14. Ours is `specialUse`, which is not on that list — and there is a compatibility switch
  that forces the restriction on early, so this is verifiable rather than merely read:
  `am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS <pkg>` (R4).
- **`RECEIVE_BOOT_COMPLETED` is a normal permission** with no prompt and no dialog.

**`MY_PACKAGE_REPLACED` is added alongside `BOOT_COMPLETED`, and it is not a nicety here.** An app
update kills the service, and this build is about to be updated repeatedly on twelve strangers'
phones for fourteen days. Without it, every internal-track update silently stops their shade and the
first thing each tester learns is that Gloam turns itself off.

### `LOCKED_BOOT_COMPLETED` is not an option, and the reason is storage

Gloam is not direct-boot aware, so its DataStore lives in credential-encrypted storage and cannot be
read before the first unlock. `BOOT_COMPLETED` is delivered after that unlock; `LOCKED_BOOT_COMPLETED`
is not. A receiver on the earlier action could not read `shadeRunning` at all, which is the only thing
it exists to read. Not a preference — a consequence of where the file lives.

### The permission is declared rather than inherited

`RECEIVE_BOOT_COMPLETED` is already in the built artifact today, **merged in by WorkManager rather
than written by us** (`DOD.md`'s artifact-check item names it, with `WAKE_LOCK` and
`ACCESS_NETWORK_STATE`). A load-bearing permission arriving by transitive merge is one dependency bump
away from disappearing, and it would disappear silently — the receiver would simply stop being called.
It is declared in `AndroidManifest.xml`, with the comment saying which feature owns it. §7 then
removes the dependency that had been supplying it, which is the order these two have to happen in.

### What the receiver decides, and what it refuses to do

```
BootReceiver (BOOT_COMPLETED, MY_PACKAGE_REPLACED, exported=true, not directBootAware)
  goAsync()
  read shadeRunning and offAtMillis in ONE DataStore read
  if (!shadeRunning)                  -> nothing
  if (isDue(now, offAtMillis))        -> endShade(); nothing on screen
  if (!canDrawShade())                -> nothing, and shadeRunning is left alone
  else                                -> startForegroundService(...)
```

Three of those four branches are refusals and each one earns its place:

- **A deadline that passed while the phone was off means the shade was never coming back.** Auto-off
  and reboot restore meet here, and this is the only place they can: without the check, a phone
  switched off at 23:00 with a two-hour deadline restores a shade at 09:00 the next morning that the
  user had already asked to end. `endShade()` clears both keys together so the next launch reads a
  clean state rather than a stale one.
- **No overlay permission means no window**, and starting anyway would post a notification saying
  *Screen dimmed* over an undimmed screen. The service already guards `addShadeWindow` on
  `canDrawShade()`, so the failure is quiet rather than a crash — quiet and wrong is worse than not
  starting.
- **`shadeRunning` is not cleared on that refusal.** It is the user's intent and they did not change
  it; the app changing it for them is how a revoked permission turns into a lost setting. The next
  launch shows the explainer, which is the right place for that conversation.

**The notification is deliberately *not* a fourth refusal, and this is the place to say so.** A user
who denied `POST_NOTIFICATIONS` and started the shade anyway — which Phase 1 allows on purpose — gets
it restored at boot with no Stop action and no app on screen to carry the warning banner. Phase 1
settled the principle for the hand-started case: refusing to dim somebody's screen because they
refused a notification is the app deciding it knows better, and a reboot does not change who decided.
What the reboot *does* change is that nobody is watching, so the cost is stated rather than implied —
the hatches left are the power menu and the app's own controls at 0.33 nits, and R2 is the reading
that makes those legible rather than trapping. **Past `MAX_SHADE_ALPHA` they would not be**, which is
the gap §2 hands to 2b.

**One read, not two.** `AppPreferences` grows a `suspend fun shadeIntentNow()` returning both values
from a single `store.data.first()` — the same shape and the same justification as `themeModeNow()`,
which already exists for the one caller that must have an answer before anything else can happen. A
`goAsync()` receiver has roughly ten seconds and a DataStore read is milliseconds, so this is
comfortable rather than tight.

### The safety interaction this phase creates, and the measurement that answers it

The power menu is the third escape hatch and its only use is a restart. **This phase makes a restart
put the shade back**, which reads, stated plainly, like removing a hatch.

It survives that, and the argument is measured rather than reasoned. Phase 1's R5 found the
**keyguard releases the brightness override outright** — `NaN`, `reason=manual`, back to the user's
own setting — and returns it on unlock. The lock screen is also above `TYPE_APPLICATION_OVERLAY`, so
the shade does not cover it. **Every restart therefore ends on one full-brightness, undimmed screen
that the user must pass through.**

**Be precise about what that screen offers, because it reads as more than it is.** `BOOT_COMPLETED`
arrives *after* the first unlock — the same fact that rules out `LOCKED_BOOT_COMPLETED` above — so at
the keyguard the receiver has not run, the service is not alive, and there is no notification to act
on. What the bright keyguard gives is **sight, not a control**: the user sees a normal phone, unlocks,
and then has whatever head start the restore takes before the shade returns. So a restart is not a
permanent undo of a setting they never changed, and it is not an escape either — it is a bounded
window with the app's own controls at the end of it. **R1 measures that window**: "how long after
unlock" is the size of the head start rather than a curiosity. **This phase's R5 confirms the
keyguard half on a *restored* shade specifically**, because Phase 1's reading was taken on one started
by hand and a restored window is added by a service with no Activity behind it.

The bounded version is auto-off, which applies to a restored shade through the same stored deadline —
so the worst case is not "dimmed forever after a reboot", it is "dimmed for the remainder of what
they asked for".

### Autostart: offered, never claimed, and never re-read by the app

ADR-0003 observed it and this feature is the first one that needs it: **without autostart, HyperOS
does not start the process for a broadcast at all** — not `BOOT_COMPLETED`, not
`MY_PACKAGE_REPLACED`, not an explicit `am broadcast` to a manifest receiver. So on the one device in
the loop, reboot restore is a feature the ROM decides.

**A correction to PLAN rule 4's table, and it is worth making explicitly.** That table says Phase 4
*re-reads* the autostart grant. The app cannot. There is no appop — `AUTO_START` is not in
`cmd appops`' vocabulary — the setting has no public state, and the OEM screen's own `checked`
attribute lies on this phone: `device-gate.py` records that every row reports `checked=false`,
granted ones included, which is why it infers the state from the list heading and the position of the
app's name relative to the "aren't allowed" divider. **That is a host-side uiautomator scrape over
`adb`, available to the toolchain and never to the app.** So "re-read in Phase 4" means
`python3 scripts/device-gate.py`, run by a developer, and Phase 4's text should be amended to say so.

**So the hand-off is a permanent row in Settings, not a prompt, and no key remembers it.** Three
reasons, in the order they matter:

- **The app cannot know whether it worked**, and ADR-0003's amendment names the hazard precisely: a
  confirmation checkbox would have the app repeating the user's guess back to them as its own
  assurance. A row that is always there makes no claim at all.
- **A once-only prompt needs a stored "offered" flag** — a written DataStore key, frozen at the door,
  bought to answer a question a permanently visible row does not ask. §1 deleted one such key today;
  adding another in the same phase would be a poor trade.
- **The failure it explains is invisible and recurring.** A user whose shade did not come back after a
  restart needs the explanation on the day it happens, which is not the day they installed the app.

The row is **gated on `hasAutostartSettings()`**, which *is* a live read — the manifest's `<queries>`
entry for `com.miui.securitycenter` is what makes `resolveActivity` answer honestly — so it is absent
on every phone that has no such screen rather than being a dead button.

### Copy — sections 3 and 4

| Key | English | Checkpoint |
| --- | --- | --- |
| `dim_auto_off_label` | Turn off on its own | B |
| `dim_auto_off_never` | Never | B |
| `dim_auto_off_30m` | After 30 minutes | B |
| `dim_auto_off_1h` | After 1 hour | B |
| `dim_auto_off_2h` | After 2 hours | B |
| `dim_auto_off_4h` | After 4 hours | B |
| `dim_auto_off_hint` | Gloam takes the shade down by itself after this long, so a screen you dimmed and forgot about is not still dark tomorrow. | B |
| `dim_auto_off_at` | Turns off at %1$s | B |
| `settings_restart` | After a restart | C |
| `settings_restart_body` | Gloam puts the shade back when your phone restarts. This phone only lets an app run at startup if autostart is switched on for it, and Gloam has no way to check whether it is — so if the shade does not come back, this is the setting to look at. | C |
| `settings_restart_open` | Open autostart settings | C |

`settings_restart_body` is shown only where `hasAutostartSettings()` is true, so its second sentence
is never read by somebody whose phone has no such screen. Translate from English only, per
[`translator-brief.md`](translator-brief.md), once the wording is settled rather than twice.

---

## 5. Support: the mail hand-offs

Moved here from Phase 5's polish half by PLAN rule 5: **a tester with no route to report is a tester
whose fourteen days produce nothing.** The rate-on-Play link and the tip stay in Phase 5, where there
is a listing to rate and an audience to tip (ADR-0009 and its amendment — the tip does not go in the
app at all).

`ui/support/` already holds the licences half, so this is a screen joining a package rather than a
new one. **A `Support` detail screen pushed from Settings' *About* section**, beside the existing
licences row — not a third tab. The bottom bar is a switch between roots and support is not a root;
it is somewhere you go once, from the place you already go to look things up.

Licences stays where it is. Moving it under Support would be three levels deep for a screen nobody
reaches by accident, and the churn buys nothing.

### The `mailto:` trap, which is a device finding rather than a design

Close to a lift from Binky's `SupportHandoff.kt` — a design that already cost a device session:
**Gmail silently ignores `EXTRA_SUBJECT` for `ACTION_SENDTO`**, so the subject and the body travel
percent-encoded in the `mailto:` query string instead. Nothing about the intent fails; the mail just
opens with an empty subject, which is exactly the sort of thing discovered by a user rather than by a
build. R9 re-measures it on this phone rather than trusting the note.

The subject carries a filter tag — `Gloam #bug`, `Gloam #feature` — and is `translatable="false"`,
because it is a string the developer sorts on rather than one the user reads. The body's prompt is
translated, and a developer receiving a Polish bug report is the correct outcome of an app with a
Polish locale.

**The body is prefilled with what a bug report always needs and a reporter never has to hand:** app
version and version code, Android release and SDK level, device manufacturer and model. Composed at
the point of the hand-off, from `BuildConfig` and `Build`, with nothing read that the app does not
already know about itself. It is not a privacy question in the ordinary sense — Gloam declares no
`INTERNET` permission and has no route off the device — the mail is the *user's own*, composed in
their client, visible to them before they send it, and deletable line by line.

**Every launch is a `try`/`catch` around `ActivityNotFoundException`**, in the shape
`startActivitySafely` already provides, and a failure says so rather than doing nothing. The
manifest's `<queries>` already declares the `mailto:` intent, so a pre-check would answer honestly if
one is ever added; none is added here, because the launch itself is the check.

### The address

**`gloam.dimmer@gmail.com`** — a dedicated account, already created. Settled 2026-08-31, and it was
the one decision in this document that could not be made from inside the repository.

**Dedicated rather than a plus-alias on the personal account, and the reason is the freeze.** A
shipped address is permanent in a way the listing is not: a sideloaded APK has no forced update, so
whatever string is in the build keeps being mailed forever. `...+gloam@` cannot be re-pointed — it is
welded to one Gmail account for as long as any build survives — while a dedicated address forwards
anywhere and can be handed to another maintainer without handing over your own mail. The tag filter
works identically either way.

**The Play listing's contact field points at the same address.** It is the route a tester finds
first, and two routes is one more than anybody needs.

### Copy

| Key | English | Checkpoint |
| --- | --- | --- |
| `settings_support` | Help and feedback | D |
| `support_title` | Help and feedback | D |
| `support_report` | Report a problem | D |
| `support_report_hint` | Opens your email app with what Gloam knows about your phone already filled in. | D |
| `support_feature` | Suggest a feature | D |
| `support_feature_hint` | Every feature in Gloam is free and always will be, so the only question is whether it belongs. | D |
| `support_subject_bug` (`translatable="false"`) | Gloam #bug | D |
| `support_subject_feature` (`translatable="false"`) | Gloam #feature | D |
| `support_body_bug` | What happened, and what you expected instead: | D |
| `support_body_feature` | What would you like Gloam to do? | D |
| `support_no_mail_app` | No email app on this phone to open. | D |

---

## 6. Storage

Two keys added, one deleted, and every one of them is frozen the day a stranger's phone holds it.

| Key | Type | Default | Note |
| --- | --- | --- | --- |
| `auto_off_minutes` | `Int` | `120` | The `AutoOff` value, stored as its minutes. `0` is **Never** |
| `off_at_millis` | `Long` | `0` | The absolute instant the shade next comes down. `0` is *no deadline* |
| ~~`onboarding_done`~~ | — | — | **Deleted.** Never written by anything; §1 |

**`auto_off_minutes` stores minutes rather than the enum's name**, which is the opposite of what
`theme_mode` does, and the difference is which direction the value can move. A theme mode is a closed
set whose members will not change; a duration is a number this app might later offer more of, and an
`Int` lets a future build add "3 hours" without a stored `Hours3` that older builds cannot parse. It
is read through `AutoOff.entries.firstOrNull { it.minutes == stored } ?: AutoOff.Hours2` — an
unrecognised number falls back to the default rather than throwing, the same rule `themeMode`
already follows and for the same reason.

**`off_at_millis` is named for what it is rather than for who set it**, because Phase 4 adds the
second writer. It means *the instant the shade next comes down, whoever decided that* — not
"auto-off's deadline". A `Long` of `0` rather than a nullable, because DataStore has no null and a
sentinel that is also a valid instant would be 1970.

**Two keys, one transaction.** `shadeRunning` and `off_at_millis` are written together or not at
all, through two new `AppPreferences` methods that name events rather than fields:

```kotlin
suspend fun beginShade(offAtMillis: Long)   // running = true, deadline set — one edit {}
suspend fun endShade()                      // running = false, deadline cleared — one edit {}
```

`setShadeRunning` goes away with them. It is not a rename: a caller that can write *running* without
writing the deadline is a caller that can leave a stale deadline behind a fresh start, and reboot
restore reads exactly those two values together. `DataStore.edit` is transactional, which is what
makes this free rather than a lock.

**Two methods and not three: the chip-while-running rewrite is `beginShade` called with `running`
already `true`.** Writing the same `true` back costs nothing and keeps the invariant the pair exists
for — the deadline is never written without the flag beside it. Nothing grows a `setDeadline`; that
is precisely the third writer this shape is built to refuse.

**No migration, ever** — that is the point of the DataStore rule. A build that has never seen
`off_at_millis` reads the default declared beside it, and the deleted key is simply never read again.

---

## 7. The scaffolding, and the freeze list closed out

`DOD.md` names three pieces of the repository describing features the app does not have. This phase
resolves all three and finds two more.

**1. `work/BatteryExemption.kt` — split, not deleted.** After §4 the file is half live: the two
autostart functions and `startActivitySafely` are called by the Settings row; the battery-optimisation
half stays uncalled until Phase 4. A file whose name describes the dead half is exactly the ambiguity
`DOD.md` is complaining about. It becomes **`work/Autostart.kt`** (live, with `startActivitySafely`,
which both halves need) and **`work/BatteryExemption.kt`** (still Phase 4's, and now unambiguously so).

Its doc comments also carry three references to an app this is not — `openExactAlarmSettings`, "since
5a", "4g's overnight-Doze run" — inherited from the template. They go with the split.

**2. `onboardingDone` — deleted** (§1).

**3. `scripts/project.py` still reports `DATABASE_FILE gloam.db`.** It is not merely a stale line: it
is *used*, by `edge-to-edge.py`'s schema-mismatch helpers, which pull, patch a version byte into and
restore a database file that does not exist. Removing the constant means removing those helpers, which
is §8's work anyway — so the two land together rather than one leaving a broken import behind.

**4, found here: AppCompat's below-13 locale backport.** `DOD.md` carries "re-read ADR-0004 now
`minSdk` is 33" and the answer is short. The disabled `AppLocalesMetadataHolderService` manifest entry
exists so AppCompat can persist a language choice on devices below 13, and `minSdk` 33 means the app
ships to none of them — on 13+ the framework persists the choice and the entry is ignored. **Remove
the manifest entry; keep AppCompat**, which ADR-0006 still needs for `setDefaultNightMode` and
ADR-0004 still needs for the switcher itself. A dated amendment on ADR-0004, per that file's own rule
that a decision is amended rather than edited.

**5, found here: WorkManager has no caller and no future one in this plan.** ADR-0003's amendment kept
it "for future use". Since then Phase 4 has been designed around `setAndAllowWhileIdle` plus the
battery exemption and explicitly not around a worker, and `PLAN.md`'s *Not in this plan* forecloses
everything else that would want one. What it costs today is not nothing:

- **Three permissions merged into the built artifact** that the source never declares — `WAKE_LOCK`,
  `ACCESS_NETWORK_STATE` and `RECEIVE_BOOT_COMPLETED`. `ACCESS_NETWORK_STATE` on an app that declares
  no `INTERNET` and answered Play's data-safety questionnaire *nothing collected, nothing shared* is a
  line a reviewer can ask about, and the honest answer is "a dependency we do not use".
- The `Configuration.Provider` on `MainApplication` and the `tools:node="remove"` manifest surgery
  against `androidx.startup`, both of which exist only to control when WorkManager initialises.
- **Three more places the removal has to reach**, none of them the runtime dependency: the
  `androidTestImplementation(libs.androidx.work.testing)` line in `app/build.gradle.kts`, and both
  library entries plus the `workManager` version ref in `gradle/libs.versions.toml`. A left-behind
  test dependency is how a removed library walks back in on somebody's next `implementation` line.

**Recommendation: remove it**, as a second dated amendment on ADR-0003, and re-run
`scripts/aab-permissions.py` on the resulting bundle to watch the permission list actually shrink —
which is the artifact check earning its keep rather than passing. It is ADR-0003's decision to amend,
and the order matters: §4 declares `RECEIVE_BOOT_COMPLETED` **before** this removes the merge that had
been supplying it.

**What stays and is not scaffolding:** `Channels.kt`, `NotificationPermission.kt` and
`OverlayPermission.kt` are all live after Phase 1; `AppContainer` is deliberately near-empty and says
so in its own comment.

---

## 8. The emulator harnesses describe an app that does not exist

Two CI assets came over with the template and neither has been re-pointed at Gloam. `DOD.md` names
the first; the second is worse and is found here.

### The instrumented matrix is green with nothing behind it

`app/src/androidTest` contains **zero tests**, so *Instrumented tests (API 26 / 34 / 36)* pass by
having nothing to run — roughly fifteen minutes of CI per pull request buying a checkmark. The API 26
leg could not install this app even if there were something to run: `minSdk` is 33.

`DOD.md` frames this as "give the matrix real tests or cut it". **Give it one real test**, and it
should be the one thing a JVM test can never reach:

> The shade's window arrives at the window manager as `TYPE_APPLICATION_OVERLAY` carrying
> `FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE`.

That is the property `CLAUDE.md` calls load-bearing and the one whose failure is *the single worst
thing this app could ship* — a phone that appears frozen with the way out underneath the thing
freezing it. **Phase 1 enumerated those flags exactly once**, in its R10 on the AVD —
`NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS` intact. Its R7 on the phone read the
shade's window but recorded `mPolicyVisibility` and `sbrt=`, never the flag list. So **the phone has
never had this property read off it at all**, and the one reading that exists expires the moment
somebody edits `addShadeWindow`.

**Two layers, because they fail differently:**

- **A JVM test on a named constant.** The flag set moves out of the `LayoutParams` constructor into
  `const val SHADE_WINDOW_FLAGS` in `shade/`, and a unit test asserts both bits are in it. Compile-time
  constants are safe in a JVM test where method calls on `android.jar` are not — `phase-1.md`'s Kotlin
  notes already establish that — so this costs nothing and catches the likely edit.
- **One instrumented test that the window really arrives that way.** Grant the overlay permission with
  `appops set <pkg> SYSTEM_ALERT_WINDOW allow` through `UiAutomation.executeShellCommand`, start the
  service, and assert against `dumpsys window windows`. **Verify the grant with
  `Settings.canDrawOverlays()` and never by reading the appop back** — `appops get` reports last-use
  rather than the granted mode on the phone in this loop, which is a trap already recorded once.
  `POST_NOTIFICATIONS` comes from a `GrantPermissionRule`.

**The matrix becomes API 33 and 36** — the floor and the target, exactly the pair ADR-0008 already
justifies for the end-of-phase pass and for the `gloam-api33` AVD. Two legs with one real assertion
is a better trade than three with none.

**The case for dropping API 26 is not that it cannot install; it is that its stated purpose is
already gone.** `ci.yml` justifies that leg in its own comment — *"the pre-S path into system backup
settings, the pre-R agent branch and the pre-S theme, plus AppCompat's pre-13 locale backport
(ADR-0004) run only down here"*. The backup agent went with the database (ADR-0007), and §7 removes
the locale backport in this phase. That comment describes an app that no longer exists, which is the
same disease as `SCENES` one file over. So cutting the leg removes dead weight rather than trading
coverage for minutes — and minutes were never the argument, since the legs run in parallel.

Honest about the cost: parsing `dumpsys window windows` is version-shaped text, so the instrumented
half is the layer that will need attention when a platform changes its output. That is why the flag
set is *also* asserted on the JVM, where nothing can drift.

### The edge-to-edge matrix walks the template's screens

`scripts/edge-to-edge.py`'s `SCENES` table drives ~75 scenes per cell through `items`, `item-detail`,
`Add an item`, `Backup and restore` and a `schema-mismatch` screen reached by patching a version byte
into a database file. **None of those exist.** The job is nightly-only and not part of
`instrumented-gate`, which is why nobody has been stopped by it — but a nightly that cannot pass is
not coverage, it is a red light everyone learns to ignore.

**`SCENES` is rewritten against Gloam's screens, and the rewrite is already a door item.** `DOD.md`
lists "Real screenshots off a real mark, and the `[SCENES]` rewrite behind them" in front of the door,
because `screenshots.py` **imports** the table rather than copying it — the comment in that file says
so explicitly. So one rewrite serves the store screenshots and the edge-to-edge matrix, and it is the
same work either way.

**The table and the shipped screenshots are separable, and checkpoint F owes only the table.**
`SCENES` can be rewritten and the matrix made honest with the placeholder mark still in place; what
waits on §13.3 is the *pixels* the listing ships. F is therefore not blocked on the mark, and §13.4 is
not blocked on F beyond the table landing.

Gloam's whole surface is a much smaller table than the one it replaces: the dim screen at its top and
bottom, the dim screen showing the permission explainer, Settings at its top and bottom, Support,
Licences and the licence text. **No forms** — so the `imePadding` scenes that were the most valuable
part of the old table have nothing to point at. Record that as a loss rather than deleting them
silently: the day Gloam grows a text field, that scene comes back.

**API 26 comes out of this matrix too**, and this matrix goes to **the same 33 and 36** as the
instrumented one. Two matrices with two different floors is a difference nobody could explain later:
34 was never a floor of anything, and `ci.yml` calls that leg *"the long-standing leg, kept as-is"*,
which is inertia rather than a reason. All four configurations stay — orientation and navigation mode
are the axis that still varies, and they vary by device and by user setting rather than by platform
release, so breadth is worth keeping when the legs run in parallel anyway.

The comment at the top of the job — *"the prize is API 26-28"* — stops being true and has to be
rewritten rather than left to mislead: the prize is now the configuration grid, not the API floor. If
API 33's image turns out to lack the navbar overlays the way API 34's ATD does, `edge-to-edge.py`
already reports that itself rather than failing silently.

---

## 9. Documents this phase amends

- **`PLAN.md`** — Phase 2 gains a *Detail: `phase-2.md`* line. **Rule 4's table is corrected**: the
  app can never re-read the autostart grant, so Phase 4's "re-read" means `device-gate.py`, run by a
  developer (§4). **Rule 5 is left standing rather than amended**, and that is deliberate: it names
  auto-off's default as one of the two questions the twelve answer, and §3 ships `Hours2` as
  provisional rather than closing it.
- **`CONTEXT.md`** — **three rows**, and the earlier claim that the existing definition survived was
  simply wrong. *"One-shot, set when they start it"* is the clause a reader would rely on to predict
  what the chip does mid-session, and §3 makes it false. **Deadline** is added because §6 is careful
  that `off_at_millis` is named for *what it is* rather than for who set it, and Phase 4's
  *earlier-deadline-wins* is unnameable without the noun. The three clauses stay in §2 — they are a
  testable predicate, and `CONTEXT.md` is a glossary:

  | Term | Means | Not |
  | --- | --- | --- |
  | **Deadline** | The one instant at which the shade next comes down, whoever set it. There is only ever one. | Not "auto-off" — that is one of the things that sets it. Not "timer" |
  | **Auto-off** | The duration a hand-started shade lasts before it comes down on its own. One-shot — it never repeats — and choosing a duration re-sets the deadline from that moment, whether the shade is already up or not. | Not "timer" on its own — the schedule sets deadlines too |
  | **Escape hatch** | A surface that stops the shade and can be reached without seeing Gloam's own UI, which the shade may be covering. Not every control that stops the shade is one. | Not the app's own *Stop dimming* button, which sits under the shade at high dim levels |
- **`CLAUDE.md`** — one stale line. The layout block calls `app/src/debug/` "currently an empty seam",
  and it has held Phase 1's backlight sweep since that phase's checkpoint B. §10's deadline button
  adds to it again.
- **ADR-0003 — a second dated amendment**, if §7's recommendation is taken: WorkManager removed, the
  three merged permissions with it. It also records §4's correction — the autostart grant has no
  in-app read, only a host-side scrape.
- **ADR-0004 — a dated amendment**: the below-13 locale backport is removed now that `minSdk` is 33.
  AppCompat stays.
- **`DOD.md`** — tick items as they close: the three scaffolding pieces, the emulator CI legs, the
  ADR-0004 re-read. The recruiting and listing items stay open until they are actually done, and the
  standing checks never close. **One item *opens*:** put auto-off's default to the testers (rule 5,
  §3), recorded here rather than in this file so the question outlives the phase that raised it.
- **`docs/store-listing.md`** — release notes for whatever versions this phase cuts. `notes-gate.py`
  fails a release whose notes are behind it, and that is not hypothetical here; it already reddened
  0.3.0's release PR once.
- **`README.md`** — auto-off is the first user-visible behaviour that is not the ramp. One line.

---

## 10. Readings, and things that only look like readings

Rule 3: device behaviour is proven by measurement, with the command that read it. Everything here
needs the phone plugged in — `adb devices` first. **R-numbers restart at R1 for this phase**; Phase
1's R1-R10 are in `phase-1.md`.

**A reboot is not a step a script may take and then continue through.** The phone has a password and
`adb` cannot get past it — not `wm dismiss-keyguard`, not a swipe, not a power cycle;
`device-gate.py`'s `wait_for_unlock` exists for exactly this and stops to ask a person. So R1 and R2
need somebody at the phone, which is why R3 exists.

| # | Reading | Decides |
| --- | --- | --- |
| **R1** | **A real reboot with autostart granted**: does the shade come back, and how long after unlock? | reboot restore, end to end |
| **R2** | The same with autostart **revoked** — does the process start for the broadcast at all? | whether §4's Settings copy describes a real failure or a theoretical one |
| **R3** | ~~`am broadcast -a android.intent.action.BOOT_COMPLETED <pkg>` as a stand-in for a reboot~~ **Refused: `BOOT_COMPLETED` is a protected broadcast and uid 2000 is not exempt.** The stand-in is `adb install -r`, which makes the *system* send `MY_PACKAGE_REPLACED` — the same receiver, the same four branches | whether the cheap loop is trustworthy; ADR-0003 says the ROM gates this broadcast identically |
| **R4** | `am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS <pkg>`, then R3 | that Android 15's blocklist really does not reach `specialUse` |
| **R5** | After a restored shade: is the **keyguard** undimmed and at the user's own brightness? | §4's safety argument, which today leans on a Phase 1 reading taken on a hand-started shade |
| **R6** | An app **update** over a live shade — does `MY_PACKAGE_REPLACED` bring it back? | the case twelve testers meet repeatedly for fourteen days |
| **R7** | **Auto-off with the screen off.** *Arm 2-minute deadline*, screen off immediately, logcat timestamps on the stop | how late the uptime-clock `delay` really is (§3), and whether the 60-second cap is enough |
| **R8a** | Auto-off across a kill that **restarts** — `kill -9` via `run-as` | that the absolute deadline resumes across `START_STICKY` rather than restarting the clock . **Taken 2026-09-01, and the premise did not hold: this ROM restarts nothing.** See the readings block |
| **R8b** | Auto-off across a kill that **does not** — `am force-stop`, which cancels that restart | that the next foreground clears a deadline nothing was alive to fire (§3's resume reconcile) |
| **R9** | The `mailto:` hand-off on this phone: does the **subject** arrive? | the Gmail `EXTRA_SUBJECT` trap, re-measured rather than inherited |
| **R10** | The instrumented window-flags test on both emulator legs **and** on the phone | §8's assertion, and whether the split-APK workaround still works |
| **R11** | **API-33 AVD end-of-phase pass** (ADR-0008): launches, window appears, permission flow, boot restore via R3, auto-off fires | the phase's own closing gate |

**R7, R8a and R8b need a deadline shorter than thirty minutes, and nothing in the shipped app can
make one.** `AutoOff`'s smallest value is `Minutes30`. A debug-only entry in that enum is out: it
lives in `main/`, it drives the chip row, and its label would go through the translation gate — the
exact "compiled into the release AAB with its strings still in the gate" failure the source-set seam
exists to prevent. `adb` cannot do it either, because DataStore sits in credential-encrypted storage
and the live process holds its state in memory. So the debug section grows one hardcoded-English
button, **Arm 2-minute deadline**, calling `beginShade(now + 120_000)` — the same justification the
backlight sweep beside it already carries, that only the app can do this to itself.

**R8b is the reading the old R8 skipped.** It used to say `kill -9` and *not* `am force-stop`, on the
grounds that force-stop cancels the `START_STICKY` restart — but the restarting path is the one that
already works, since a restarted service re-reads the deadline in `onCreate`. The path that needed a
new reader is the one where nothing comes back (§3), so force-stop is the more interesting reading
rather than the avoided one.

### Derivations — arithmetic, and the test is the proof

| | Quantity | How |
| --- | --- | --- |
| D1 | The deadline for a given start and choice | `deadlineFor` — `AutoOffTest`, §11 |
| D2 | Whether a stored deadline has passed | `isDue`, same test. Nothing on a device can tell you this is right; the wrong answer is a shade restored eight hours late |

```bash
adb devices
python3 scripts/device-gate.py                       # autostart, battery, channel - read before any run
python3 scripts/device-gate.py --autostart off       # then on, for R2
adb install -r -t app/build/outputs/apk/debug/app-debug.apk   # the reboot stand-in that works
adb shell am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS <applicationId>
adb logcat -s ShadeService:* ActivityManager:* | grep -i 'gloam\|ForegroundServiceStartNotAllowed'
adb shell dumpsys window windows | grep -A6 gloam    # ty=, flags, sbrt=
adb shell dumpsys display | grep -E 'mScreenBrightness=|mWindowManagerBrightnessOverride'
```

**End of phase:** R11, on the API-33 AVD. Recreate with
`emulator -avd gloam-api33 -no-window -gpu swiftshader_indirect`. The emulator has no nits
calibration at all — Phase 1's R10 settled that — so nothing photometric is read there, and the phone
stays the only place light is measured.

---

## 11. Tests

`PLAN.md` rule 3 promises three tests across the whole remaining roadmap and reserves two of them for
Phase 4 — the schedule window crossing midnight, and earlier-deadline-wins. **Phase 2 does not spend
one of those**, and it should be honest that what it adds instead is smaller.

- **`AutoOffTest`** (JVM, no Android). `deadlineFor` over every `AutoOff` value including `Never`;
  `isDue` at, before and after the instant; a deadline already in the past reading as due rather than
  as absent; `0` reading as *no deadline* rather than as 1970. It is small, and it is the seam Phase 4
  writes its second writer into — which is the actual reason it exists now rather than then.
- **`ShadeWindowFlagsTest`** (JVM). `SHADE_WINDOW_FLAGS` carries `FLAG_NOT_TOUCHABLE` and
  `FLAG_NOT_FOCUSABLE`. One assertion over compile-time constants, guarding the property `CLAUDE.md`
  calls load-bearing (§8).
- **`ShadeWindowTest`** (instrumented, `app/src/androidTest`, **the first test in that directory**).
  The window reaches the window manager as `TYPE_APPLICATION_OVERLAY` with both flags intact (§8).
- **`TranslationTest`** covers this phase's strings continuously; the merge gate covers completeness.
  Neither needs anything added.

Everything else in this phase is device behaviour and belongs in §10.

---

## 12. The commit sequence

Conventional Commits, and each one leaves the app working. **`feat:` lines land in `CHANGELOG.md`
through release-please and `chore:` / `refactor:` / `test:` / `ci:` do not**, which is what decides
the type below rather than taste.

| Checkpoint | Commits |
| --- | --- |
| **A** | `refactor: name the escape hatch and drop the unused onboarding key` — `escapeHatchLive()`, the warning reading it, the key deleted, and `dim_permission_body` reworded in both locales. Nothing a release note would mention |
| **B** | `chore: add the auto-off deadline` + `test: bound the auto-off deadline` — the pure function and its table, wired to nothing. Then `feat: take the shade down on its own after a while` — the two keys, `beginShade`/`endShade`, the service's job, the resume reconcile, the chips and the "turns off at" line. Plus `chore: arm a short deadline from the debug section`, which is what R7 and R8 are read with |
| **C** | `feat: put the shade back after a restart` — the receiver, both actions, `RECEIVE_BOOT_COMPLETED` declared, the deadline check, and the autostart row in Settings. Then **the readings**, which are not a commit |
| **D** | `feat: add a Help and feedback screen` — the `Support` key, the screen, the two `mailto:` hand-offs and their prefilled body |
| **E** | `chore: split the autostart hand-off out of BatteryExemption` + `chore: stop reporting a database that does not exist` + `refactor: drop AppCompat's below-13 locale backport`, and if ADR-0003 is amended, `chore: remove WorkManager` — runtime *and* `work-testing`, both catalogue entries and the version ref |
| **F** | `test: assert the shade window's flags` + `ci: cut the API 26 legs and give the instrumented matrix something to run` + `chore: rewrite the edge-to-edge scenes against Gloam's screens` |
| **G** | `docs: ...` — §9's edits, ADR-0003's and ADR-0004's amendments, and this file's readings block filled in |

**The ramp precedent holds for auto-off.** The deadline function lands as `chore:` wired to nothing —
the cheapest possible place to get the arithmetic wrong — and the `feat:` is the commit that wires it
up and says what a user would notice.

**`chore: remove WorkManager` changes the built artifact's permission list**, which is visible on the
Play listing even though no code changed for the user. It is still `chore:`; the place to say it is
the release-notes prose in `store-listing.md`, which is written by hand anyway.

---

## 13. The door itself — the non-code items

Not build tasks, in this phase because the door does not open without them, and every one of them is
already a box in `DOD.md`. They run alongside A-G rather than after them.

1. ~~**Enable `androidpublisher.googleapis.com` and get a green `publish-play.yml`**~~ (§0).
   **Done 2026-08-31**, on the fourth attempt. It was the item nothing else in this list could be
   proven without, and it now has a run number behind it rather than a plan.
2. **Recruit more than twelve testers** (§0). The long pole. Calendar time, other people's replies.
3. **Replace the placeholder mark** — `art/mark.py`, then `make-launcher-icon.py` and
   `make-feature-graphic.py`, with the provenance recorded in `art/README.md`. The listing's graphic
   and icon both derive from it, so it blocks item 4.
4. **Finish the listing**: polish the English short and full descriptions, then real screenshots off
   the real mark. The `SCENES` rewrite in §8 is what shoots them. `play-metadata.py` emits
   **zero-byte** `pl-PL` files rather than skipping the locale — harmless by hand, not harmless once
   the publish workflow runs.
5. **Keep health claims out of the copy and the tags** — "eye strain", "sleep", "blue light" — because
   App content was answered health-No.

---

## Kotlin and Android notes for this phase

- **`delay` is not `setTimeout`.** On Android's main dispatcher it is a `Handler.postDelayed`, which
  is scheduled against a clock that **stops advancing in deep sleep**. A `setTimeout` on a server that
  gets suspended has the same hazard and you rarely meet it; here it is the normal case, because a
  dimmed phone is a phone about to be put down. §3's loop re-reads the wall clock instead of trusting
  the timer, which is the whole difference between a two-hour auto-off and a two-hours-of-uptime one.
- **A `BroadcastReceiver` is not an event listener with a lifetime.** `onReceive` runs on the main
  thread and the process may be killed the instant it returns, so a coroutine launched inside it is
  one nobody will be alive to collect. `goAsync()` is the platform's way of saying "hold the process
  for me", and it comes with a budget of roughly ten seconds and an obligation to call `finish()` on
  every path, the failing ones included.
- **`NonCancellable` is the one place you deliberately leave structured concurrency.** Cancelling a
  coroutine cancels everything it started, which is usually the point — but §3's fire path writes the
  value it is itself collecting, so the write cancels its own block halfway through. Wrapping just
  the write says "this much finishes regardless". JS has no analogue: a `Promise` cannot be cancelled
  out from under you, so there is nothing to opt out of. Note also that cancellation is only observed
  at a suspension point, which is why a plain call placed *after* the `NonCancellable` block still
  runs.
- **`collectLatest` is `switchMap`, not `forEach`.** A new emission cancels the block still running
  for the previous one. That is what re-arms §3's deadline job when the chip rewrites it, and it is
  also the mechanism that makes the trap above possible — the same property doing both jobs.
- **`DataStore.edit {}` is a transaction, not a batch.** Two keys written inside one `edit` are
  visible to a reader together or not at all — which is why §6 replaces `setShadeRunning` with
  `beginShade`/`endShade` rather than calling two setters in a row and hoping.
- **A manifest `<receiver>` is a declaration the OS reads before any of this app's code runs.** That
  is the half with no JS analogue: the system decides to start the process *because* of what the
  manifest says, so `exported="true"` on a receiver for a system broadcast is not a security slip —
  it is the only way the broadcast can arrive. It is also why a vendor ROM's autostart switch can veto
  the whole thing without any of our code getting a chance to notice.
- **An `enum class` with a constructor argument is a lookup table**, which is why `AutoOff` carries its
  own minutes rather than a `when` mapping the two. `AppChannel` and `TopLevelDestination` are the same
  shape; three is a convention.
- **`BuildConfig` is generated, not written**, so `BuildConfig.VERSION_NAME` in §5's mail body is the
  version the build actually is rather than a string somebody remembered to update.

---

## Readings block

Rule 3: filled in as the phase runs, from the device, with the command that read it. Not "it looked
right". Derivations are in §10 and are proven by `AutoOffTest`, not recorded here.

**Read with the screen held awake where brightness is involved.** `phase-1.md` recorded four readings
first taken against a screen inside its inactivity timeout, which pins the panel to its floor and
looks exactly like "the setting did nothing". `settings put system screen_off_timeout 600000` first,
and put it back after.

| # | Reading | Command | Result |
| --- | --- | --- | --- |
| R1 | Real reboot, autostart on | reboot, wait for the unlock, `dumpsys window windows` | **The shade comes back, 54.2 s after the unlock.** 2026-09-01 |
| R2 | Real reboot, autostart off | `device-gate.py --autostart off`, reboot | **Not taken as a reboot** — the update path answers the same question and did. See below. 2026-09-01 |
| R3 | `am broadcast` as a reboot stand-in | `am broadcast -a android.intent.action.BOOT_COMPLETED` | **Impossible on this device.** `SecurityException`, uid 2000. See below. 2026-09-01 |
| R4 | The Android 15 blocklist does not reach `specialUse` | `am compat enable FGS_BOOT_COMPLETED_RESTRICTIONS`, then reboot | **Confirmed, and it was already on** — `enableSinceTargetSdk=35`. Back 47.5 s after the unlock. 2026-09-01 |
| R5 | Keyguard undimmed after a restored shade | `dumpsys display` at the lock screen | **Override released: `NaN`, tag `null`, panel back at the user's own 0.137.** 2026-09-01 |
| R6 | Update over a live shade | `adb install -r`, then `dumpsys window windows` | **Restored, in a new process, both flags and `sbrt=0.01` intact.** 2026-09-01 |
| R6b | The deadline that passed while nothing was alive | arm 2 min, hold the process dead, `adb install -r` | **`endShade()`, nothing restored.** The refusal §4 leans on. 2026-09-01 |
| R7 | Auto-off with the screen off, and how late | *Arm 2-minute deadline*, `input keyevent SLEEP`, logcat timestamps | **61 ms late**, screen off throughout. 2026-09-01 |
| R8a | Auto-off across a kill that restarts | `run-as ... kill -9`, then watch the restart | **There was no restart.** See below. 2026-09-01 |
| R8b | Auto-off across a kill that does not | `am force-stop`, then open the app | **Cleared on the next foreground**, read off the stored file. 2026-09-01 |
| R9 | `mailto:` subject survives | tap *Report a problem*, read the compose screen, send it | **It arrives, and so does the mail.** `Gloam #bug` prefilled; delivered and forwarded on. 2026-09-01 |
| R10 | Window-flags test, both emulator legs and the phone | `connectedDebugAndroidTest`; on the phone, the split-APK workaround in `CLAUDE.md` | **Both emulator legs green, and red under mutation. The phone is still out of reach.** See below. 2026-09-02 |
| R11 | API-33 AVD end-of-phase pass | `emulator -avd gloam-api33 -no-window` | **The phase's four behaviours, on the floor** — launch, shade, deadline, and a real reboot that restored it. 2026-09-02 |
| R12 | The language switcher without AppCompat's backport | tap *Polski*, `cmd locale get-app-locales`, force-stop, relaunch | **The framework holds it: `[pl]`, and a cold start comes up Polish.** 2026-09-01 |

### R3 — the cheap loop this phone refuses, and what took its place

`am broadcast -a android.intent.action.BOOT_COMPLETED` does not run here:

```
java.lang.SecurityException: Permission Denial: not allowed to send broadcast
android.intent.action.BOOT_COMPLETED from pid=10615, uid=2000
```

`BOOT_COMPLETED` is a **protected broadcast** — only the system may send it — and on this build the
shell uid is not one of the exempt callers. That is the platform, not the ROM, and no flag turns it
off. So the loop §10 was built around never existed, and the phase's own command block said to run
something that cannot be run.

**`adb install -r` is the replacement, and it is a better one than it looks.** It makes the *system*
send `MY_PACKAGE_REPLACED` — a real protected broadcast, delivered to a real manifest receiver, into
a process the ROM had to agree to start — and the receiver takes the same four branches whichever
action it was handed. Everything below except R1, R2 and R4 was read through it, and each of those
three is a reboot precisely because nothing else produces `BOOT_COMPLETED`.

### R1 and R4 — it comes back, and the head start is about a minute

Two real reboots, autostart granted, with the shade up and a two-hour deadline on it beforehand:

```
09-01 19:41:10.881 I/GloamBoot(19927): android.intent.action.BOOT_COMPLETED: shade restored, deadline=1788291397797
09-01 19:46:03.985 I/GloamBoot(20105): android.intent.action.BOOT_COMPLETED: shade restored, deadline=1788291397797
```

**54.2 s and 47.5 s after the unlock**, measured from the moment `isKeyguardShowing` went false to
the moment `dumpsys window windows` listed the overlay. The window came back with
`ty=APPLICATION_OVERLAY`, `fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS` and
`sbrt=0.01` — every safety attribute, on a window added by a service with no Activity behind it.

**The deadline is the same integer in both lines, and it was set before the first reboot.** An
absolute instant survived two restarts without being recomputed, which is the property §3 chose it
for, read here rather than argued.

**R4 turned out to be already true rather than newly true.** `dumpsys platform_compat` reports
`FGS_BOOT_COMPLETED_RESTRICTIONS ... enableSinceTargetSdk=35`, and Gloam targets 36 — so Android
15's blocklist was **already in force during R1**, and forcing the override on only made it
unambiguous. It survived the reboot (`packageOverrides={…gloam.debug=true}` after boot), the shade
came back anyway, and `ActivityManager` logged the allowance rather than a refusal:

```
W/ActivityManager: Foreground service started from background can not have location/camera/
microphone access: service …/app.gloam.shade.ShadeService
```

That warning **is** the reading: it is what the platform says when it permits an FGS start from the
background and merely narrows it. `specialUse` is not on the blocklist, on the device, with the
restriction on.

**About a minute of undimmed phone is the honest number to carry into 2b.** §4 argues the power menu
survives as a hatch because a restart ends on a bright keyguard and then a bounded window before the
shade returns. The window is bounded and it is roughly fifty seconds, most of it the ROM's own
staggering of boot broadcasts rather than anything this app can shorten.

### R2 — the ROM's veto, read off the update path

Autostart revoked, shade running, process dead, then `adb install -r`: **the process was never
started at all.** No pid, no `GloamBoot` line, no window — only the package installer's own
`startProcess` entries in the log. Granted, the same sequence starts the process and restores the
shade every time.

So ADR-0003's observation holds on this build and on this Android version, and `settings_restart_body`
describes a failure that really happens rather than a theoretical one. It was not taken as a *reboot*
because a reboot would answer the same question at the cost of another unlock: what is being tested
is whether the ROM starts the process for a broadcast, and `MY_PACKAGE_REPLACED` is a broadcast.

**One thing that reading cost, and it is worth knowing before repeating it: revoking autostart
force-stops the app.** A package in the *stopped* state receives no manifest broadcast at all,
whatever autostart says, so a restore reading taken straight after a `--autostart off` is measuring
the stopped flag. `dumpsys package <pkg> | grep stopped=` before believing any of it.

### R5 — the keyguard releases the override, on a restored shade too

`dumpsys display`, shade up, screen unlocked:

```
mWindowManagerBrightnessOverride=0.01
mWindowManagerBrightnessOverrideTag=io.github.srednimax.gloam.debug
mScreenBrightness=0.0053381873
```

The same dump at the lock screen, seconds later:

```
mWindowManagerBrightnessOverride=NaN
mWindowManagerBrightnessOverrideTag=null
mScreenBrightness=0.13694584
```

Phase 1's R5 was taken on a shade started by hand; this one was taken on a shade a `BroadcastReceiver`
put up, and the answer is identical. `dumpsys window windows` also puts `NotificationShade`,
`StatusBar` and the rest of the system layer **above** our window in z-order. So the bright screen a
restart ends on is real, and it is what makes §4's safety argument survive the feature it is about.

### R6b — the refusal, and what it took to hold the process dead long enough to read it

The branch that matters most — a deadline that passed while nothing of ours was alive — is the one
this phone fought hardest to show:

- **`am force-stop` keeps the process dead and sets the *stopped* flag**, and no manifest receiver
  runs in that state. The install went through, nothing was logged, and the reading was about the
  flag rather than about the deadline.
- **`kill -9` leaves the flag alone, and with autostart granted this ROM restarts the service.** It
  came back, its own `awaitDeadline` loop fired, and the shade ended before the update ever arrived.
  A clean reading of §3 and no reading at all of §4.

Killing it every 1.5 s for the length of the deadline — five restarts — and then installing into the
gap produced the branch:

```
09-01 19:35:55.233 I/GloamBoot(7610): android.intent.action.MY_PACKAGE_REPLACED:
the deadline (1788284114261) passed while the phone was off
```

No window, and the stored intent cleared. **This also corrects R8a's premise.** That reading found
"this ROM restarts nothing" — but it was taken with autostart *revoked*. With autostart granted,
`START_STICKY` is honoured and the killed service comes straight back. Both halves of §3's argument
therefore matter on this device: the restart path resumes the absolute deadline, and the
no-restart path is what the resume reconcile is for.

### Two things the toolchain was reporting wrongly, both found here

Neither was a failure. Both were clean, confident, wrong answers, which is the failure mode this
repository keeps meeting in new places — and both are fixed in `scripts/device-gate.py`:

- **The autostart scrape looked for "Gloam Debug".** The debug source set names the app *Gloam
  debug*. No row ever matched, so the grant could not be set, and `autostart allowed: no` was printed
  whatever the truth was. The label now comes from `project.DEBUG_APP_NAME`, parsed out of the debug
  `strings.xml`.
- **`uiautomator dump` fails while the screen is moving and leaves the previous dump on disk.** The
  helper read that back as a fresh one, matched a row at coordinates from before the swipe, and
  tapped whatever had moved into that spot — on a 107-row list, that is a grant handed to a
  neighbouring app. It now deletes the file first and requires a `<hierarchy` back, and it scrolls
  with a slow 800 px swipe rather than a fling that carried the target through the screen between
  dumps.

### Two things about the permission hand-offs, recorded where §1 can find them

- **HyperOS ignores the `package:` scoping on `ACTION_MANAGE_OVERLAY_PERMISSION`.** The intent lands
  on the full *Display over other apps* list, not on Gloam's own row — so the user arrives at an
  alphabetical list of every app and has to find themselves in it. `OverlayPermission.kt`'s comment
  said the Uri is what makes it land on our row; that is the documented behaviour and not this
  phone's. Not fixed here — there is nothing to fix, the intent is the only one there is — but the
  copy that sends people there should not promise a screen with one switch on it.
- **`adb install -r` resets an `appops`-granted `SYSTEM_ALERT_WINDOW`.** A grant made with
  `appops set … allow` does not survive the next update, which turns every restore-after-update
  reading into a reading about the appop: the receiver ran, found no overlay permission, and refused
  — correctly, and about the wrong thing. Grant it through the system screen before taking these.
  The refusal branch was read twice by accident this way, which is at least one branch proven for
  free.

### R7 — 61 ms late, and what it does not prove

Armed 18:15:34.204 for a deadline 120 s out, `input keyevent SLEEP` immediately after, and the fire
line landed at 18:17:34.240: **61 ms after the deadline**, with `mWakefulness=Dozing` for the whole
two minutes. On the awake path the 60-second re-check costs essentially nothing in accuracy.

**It does not prove the case the cap exists for, and the reason is `adb` itself.** The phone was on
USB and charging throughout — `dumpsys deviceidle` read `mState=ACTIVE mCharging=true` — so it never
suspended, and `SystemClock.uptimeMillis` never stopped advancing. §3's argument is about deep sleep,
which is unreachable while the cable that carries the reading is plugged in. **The variant that would
close it**: arm, unplug, wait past the deadline, plug back in and read the buffer, which survives the
disconnection. Worth taking before the door, and it can only make the case for the cap stronger — a
single two-hour `delay` would come back *later* than a loop that re-reads the wall clock, never
earlier.

### R8a — HyperOS did not restart the service at all

The reading was written to ask whether an absolute deadline resumes across a `START_STICKY` restart.
**It could not be taken, because there was no restart**, and that answer is worth more than the one
it replaced.

`run-as ... kill -9` on the live process at 18:25:26, deadline 18:26:34. Two and a half minutes later
`pidof` was empty, `dumpsys activity services` held **zero** `ShadeService` records — not even a
pending restart — and the deadline had passed with nothing alive to notice. `ActivityManager` logged
the death (`Process ... has died: fg TOP`, `Cancel FGS notification`) and scheduled nothing. Repeated
once, with the same result.

**The absence of a "Scheduling restart of crashed service" line is not the evidence.** That string
appears **zero** times in this ROM's whole buffer for any package, so its absence proves nothing
here. The empty `pidof` and the zero service records are the reading.

So the premise `ShadeService`'s own notes already carry — that `START_STICKY` "is worth less than the
stored preference" — is not a hedge on this phone, it is the normal case. **What that promotes is
§3's resume reconcile**: it was written for the kill that does not come back, and on this ROM that is
*every* kill. It is not a corner case; it is the path.

### R8a's other half — the app can claim a shade that is not there

Killed at 18:33:07 with the deadline at **18:34:48, still in the future**, then reopened. The stored
values were untouched — `shade_running = true`, `off_at_millis = 1788280488482` — the reconcile
correctly did nothing, and the screen read **"Stop dimming"** over **"Turns off at 18:34"** with no
shade on screen and no service alive.

Every part of that is the design working as written: the stored intent is what the user asked for,
the live service is what the ROM allowed, and `CLAUDE.md` forbids inferring one from the other. But
the *screen* is now asserting a shade that does not exist, and this ROM produces that state on every
kill rather than rarely.

**This is checkpoint C's to answer, and it is a wider question than the one C was scoped to.** C
restores the shade on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`; nothing restores it on a plain
foreground, which is when the user actually notices. The options are C's to weigh — restore from the
Dim screen when the intent says running and no service is alive, or say so rather than silently
claiming otherwise — and checkpoint B deliberately does neither, because both change what "running"
means to Phase 2b's gate.

### R8b — proven off the stored file, not off the screen

`phase-1.md` learned to distrust a reading that looks like a pass; the DataStore equivalent of the
WAL trap is trusting a *screen* that renders a default while the file still holds the old value. So
this was read from the file on both sides of the event:

```bash
adb shell "run-as <pkg> cat files/datastore/app_preferences.preferences_pb"   # then decode
```

It is a protobuf `map<string, Value>`, and thirty lines of Python read it — worth keeping, because it
is the only way to see what the app actually stored rather than what it draws. Sequence, 2026-09-01:

| Time | Event | `shade_running` | `off_at_millis` |
| --- | --- | --- | --- |
| 18:29:37 | armed, 120 s out | `true` | `1788280297377` (18:31:37) |
| 18:29:43 | `am force-stop`; `pidof` empty, 0 services | `true` | `1788280297377` |
| 18:32:18 | 41 s past the deadline, nothing alive | `true` | `1788280297377` |
| 18:32:22 | app opened | **`false`** | **`0`** |

Both keys, together, on the next foreground — which is `endShade()` doing exactly what §6's pairing
was written for. `auto_off_minutes` never appears in the file at all, because nothing has written it:
the default lives in the read, which is the DataStore rule holding in practice.

### R9 — the subject arrives, and the note it re-measures stays worth having

Tapped from the phone, on the debug build, with the shade up:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n <applicationId>/app.gloam.MainActivity
adb exec-out screencap -p > mail.png    # Settings > Help and feedback > Report a problem
```

Two apps claim `mailto:` on this phone — Gmail and, unexpectedly, PayPal — so the hand-off lands on
the system chooser rather than straight in a composer. Worth knowing before writing copy that
promises "your email app": on a phone with more than one, the first thing the user sees is a
question. Nothing to fix; the chooser is the platform being correct.

Gmail then opened with **all three** fields filled: `gloam.dimmer@gmail.com`, subject `Gloam #bug`,
and the body

```
What happened, and what you expected instead:


---
Gloam debug 0.3.0 (66)
Android 16 (SDK 36)
Xiaomi 24115RA8EG
```

**The subject is the whole reading, and it passes** — which is a result about the query string rather
than about Gmail having been fixed. `EXTRA_SUBJECT` was never tried here; §5's design put the subject
in the URI precisely so that this reading could not fail, and the reading confirms the URI route
works rather than clearing the extra. The note it inherited from Binky stays in the file for the next
person who reaches for the obvious API.

Two smaller things it settled in passing: `app_name` reaches the body as **Gloam debug** on a
sideloaded build, which is the disambiguation §5 wanted and only a real hand-off could show; and the
version code is the commit count, so a report identifies the exact build without anybody typing it.

**And the mail was then sent by hand, which answered a question this reading was not built to
ask.** It arrived at `gloam.dimmer@gmail.com` **and forwarded to the developer's own account**, so
the route is proven end to end rather than to the composer and no further. That matters more than it
looks: §5 chose a dedicated address over a `…+gloam@` alias on the argument that a real mailbox
*forwards anywhere* and can be handed to another maintainer without handing over your own mail. The
forwarding was the load-bearing half of that argument and was, until now, an assumption. It is a
reading.

**One thing this did not settle.** `adb shell input tap` cannot open Gmail's compose overflow menu —
three attempts, no menu — so the composer was left by pressing Back rather than by *Discard*, and the
send above happened by hand at the phone. That is a fact about driving another app's UI over `adb`,
not about Gloam, and it is recorded so the next person taking this reading knows the tidy-up is
theirs.

### R12 — the locale backport was carrying nothing, read from the store that was

§7 removes AppCompat's `AppLocalesMetadataHolderService` entry on the argument that `minSdk` 33 means
the app ships to no device that needs it. That argument is about *other* devices, so what it owes the
phone is the other half: that nothing on a device the app does ship to was leaning on it.

Taken on the debug build with the entry gone. Tapping **Polski** in Settings, then:

```
$ adb shell cmd locale get-app-locales io.github.srednimax.gloam.debug
Locales for io.github.srednimax.gloam.debug for user 0 are [pl]
```

— the **framework's** per-app locale store, which is the one AppCompat delegates to on 13+, holding
the choice the switcher made. A force-stop and a cold launch then came up in Polish (`Poziom
przyciemnienia`, `Włącz przyciemnianie`), so the choice survives process death with nothing of
AppCompat's persisting it.

**What this does and does not prove.** It proves the switcher writes to the platform store and reads
back from it on API 36 — the removal is invisible to a user on any device this app reaches. It says
nothing about devices below 13, and cannot: there are none. That is the removal's whole argument
rather than a gap in the reading.

The phone was put back afterwards — `cmd locale set-app-locales … --locales ""`, confirmed empty, and
the app relaunched in English.

### R10 — the assertion is real on the emulators, and the phone is a ROM problem rather than a test problem

**Green on both legs of the new matrix**, on PR #25's run: API 33 in 3m 05s and API 36 in 2m 43s,
each of them installing the app and the instrumentation and running `ShadeWindowTest` against a live
service. Before that they were green in 3 seconds with nothing to run, which is the failure `DOD.md`
recorded rather than a pass.

**A green test that has never been red proves nothing**, so it was mutated: deleting
`FLAG_NOT_TOUCHABLE` from `SHADE_WINDOW_FLAGS` and re-running turned it red with the message it was
written to print — *the shade would swallow every touch and the phone would look frozen* — and the
flag went back. That is the reading; the green on its own is not.

**The phone half is not taken, and the reason is the one `CLAUDE.md` already documents.** HyperOS
refuses the *first* install of a brand-new package outright — `INSTALL_FAILED_USER_RESTRICTED`, with
no dialog to miss — and `…gloam.debug.test` is a brand-new package. Neither `adb install -r -t` nor
the split-APK workaround gets past it, because the workaround is for the *prompt*, and there is no
prompt. It needs a hand on the device: enable USB installation in developer options, or push the APK
to `/sdcard/Download/` and tap it in the file manager.

**What that leaves unread is narrower than "the phone".** The window's flags on this phone were read
directly, twice, in R1 and R6 — `fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS`
off `dumpsys` after a real reboot and after an update. What is missing is the *automated* check on
this device, not the fact it asserts.

### R11 — the whole phase, on the API level it is allowed to be the worst on

`minSdk` is 33, so the API-33 AVD is the floor: whatever works here works everywhere the app ships.
The pass walked the phase's four behaviours end to end on `gloam-api33`, on the build that closes it.

```
$ adb -s emulator-5554 shell dumpsys window windows | grep -A1 'ty=APPLICATION_OVERLAY'
ty=APPLICATION_OVERLAY fmt=TRANSLUCENT alpha=0.8 sbrt=0.026663352
fl=NOT_FOCUSABLE NOT_TOUCHABLE LAYOUT_IN_SCREEN LAYOUT_NO_LIMITS HARDWARE_ACCELERATED
```

- **It launches and it dims.** A tap on *Start dimming* puts the shade up with all four flags and a
  window brightness override on it — the emulator honours the override, as `DOD.md` recorded, and its
  float still means nothing photometric because the image has no nits calibration at all.
- **The deadline is on screen, not just in storage.** `Turns off at 8:26 AM`, two hours after the tap,
  from `AutoOff.Default` without anyone choosing it.
- **A real reboot restores it**, which is the one thing that could not be read from the phone without
  another unlock: `GloamBoot: android.intent.action.BOOT_COMPLETED: shade restored,
  deadline=1788330406044`, the same absolute instant as before the reboot, and the window back with
  every flag and the same `sbrt`. The emulator has no vendor autostart to veto it, so this is what the
  path does when the ROM stays out of the way — R1 and R2 are what it does when the ROM does not.

**It is a floor check, not a light measurement.** The phone stays the only place a nit is read.

---

## Done when

- A new user meets one explanation naming both permission hand-offs in the order they arrive, and
  every one of the three asks that exists says what was given up when the answer is no.
- The escape-hatch inventory is written down as three clauses and one predicate, and Phase 2b has a
  definition to gate against that it did not write itself — including the two things this phase makes
  weaker: the power menu is bounded by restore latency rather than permanent, and the restore path is
  a caller 2b's gate has to cover because it has no UI to live in.
- A shade started by hand comes down on its own, on an absolute deadline that survives a process kill
  — and is cleared on the next foreground when nothing was alive to fire it — and the screen says
  when, with **Never** available and chosen rather than defaulted into.
- The shade comes back after a restart and after an update; a deadline that passed while the phone was
  off means it does not; and where the ROM can veto that, the app says so without claiming to know
  whether the user fixed it.
- A tester who finds a bug has a route to report it that fills in the version and the device for them.
- `onboarding_done` is gone, `BatteryExemption.kt` describes only what it does, `project.py` no longer
  reports a database, and the below-13 locale backport is removed with ADR-0004 amended rather than
  edited.
- The instrumented matrix runs a test that would go red if the shade lost a flag, on the two API
  levels that can install the app, and the edge-to-edge scenes walk Gloam's screens.
- Every row in the readings block above has a result behind it.
- **And outside the code**: `publish-play.yml` has run green at least once, and more than twelve
  testers are opted in.

**When this phase closes the app is safe to hand over. Closed testing opens here.**
