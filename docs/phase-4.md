# Phase 4 — It turns itself on and off

**One phase, one file, and the smallest new surface of any phase since Phase 0.** Auto-off shipped in
Phase 2, so what is left is the half `PLAN.md` split off by cost: the part that needs new machinery,
*because nothing of ours is running to do it*. That sentence is the whole phase. Everything below is
either a consequence of it or a refusal that follows from it.

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md); the decision
this phase reopens is
[ADR-0003](adr/0003-two-scheduling-mechanisms.md) — written for an app that no longer exists, amended
twice to say so, and about to be right again in a narrower way than it was written. The vocabulary is
in [`CONTEXT.md`](../CONTEXT.md), which already names **schedule**, **deadline** and **auto-off**
separately and, unusually for a phase this size, is owed nothing new.

**What closing this phase means.** Today Gloam dims when you tell it to and stops on a deadline it set
when you told it to. After this phase it starts on its own at a time you picked, and stops at the
other end of the same window — which is the first thing this app has ever done with no user in the
room. That is also the whole of the risk: a feature that runs while nobody is watching fails while
nobody is watching, and on this phone the ROM is a second opinion about whether it runs at all.

**Phase 2b is not in this file and this phase does not wait for it.** Ultra dark and the Quick
Settings tile change how dark the shade goes and how it is escaped; they do not touch when it goes up.
The one place they meet is §7's honest state, which is a warning banner in the same idiom 2b's
gate will want, and §10 says where a second one would go.

---

## 0. What this phase inherits, and the asymmetry that shapes all of it

Three things are already built and this phase adds to each rather than replacing any:

- **One stored deadline, `off_at_millis`, with exactly one writer.** `AppPreferences.beginShade`
  writes it in the same transaction as `shade_running` — `ShadeIntent` exists so nothing can read one
  without the other — and `endShade` clears both. Auto-off is the writer. **This phase adds the
  others, and that is the whole of §3** — a second that resolves, and two more that may only bring
  the deadline forward.
- **A deadline loop that trusts the wall clock and not a timer.** `ShadeService.awaitDeadline` wakes
  at most every `DEADLINE_RECHECK_MS` (60 s) and re-reads `System.currentTimeMillis()`, because
  `delay` on the main dispatcher is a `Handler.postDelayed` scheduled against `uptimeMillis`, a clock
  that stops advancing in deep sleep. The scheduled *off* needs almost nothing from this phase: it is
  a deadline, and this loop already fires deadlines. **Almost** is §5 — the wall-clock re-read fixes
  the loop's *correctness* in deep sleep and says nothing about its *latency*, which auto-off could
  afford and an overnight window cannot.
- **A receiver that puts the shade back, with four branches of which three are refusals.**
  `shade/BootReceiver.kt` reads the stored intent at `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`,
  refuses on a passed deadline, refuses without the overlay permission, and leaves the stored intent
  alone on that refusal. §4's alarm receiver is the same shape with a different third refusal — an
  alarm can arrive after the window it was meant to open — and §6 gives this one a second job.

**The asymmetry is the design.** Phase 2 wrote the sentence and this phase is what it was predicting:

> Taking something down only has to be right the next time the user looks at the screen. Putting
> something up has to be right at 22:00.

Taking down needs no permission because **something of ours is already alive** — the foreground
service owning the shade is, by definition, running whenever there is a shade to take down. Putting
up has nothing alive at all: at 21:59 on a phone that has not seen Gloam since breakfast, the process
does not exist, and everything expensive in this phase is the cost of getting one instant of
execution at 22:00 out of a platform and a ROM that both have opinions about that.

**So the whole of scheduled-*on* is one alarm and one receiver, and the whole of scheduled-*off* is a
number written into a key that already exists.** The two halves of one feature have wildly different
costs, which by `PLAN.md`'s own rule would normally make them two phases. They are not, and the reason
is that the cheap half is *four lines inside the expensive half's plan* — there is no version of this
phase that ships the off-half alone as a coherent feature, because "Gloam turns the shade off at
07:00" describes something the user has to turn on by hand at 22:00 every night, which is auto-off
with extra steps. §1 is the exception: if the gate says no, that unshippable thing is the
fallback, and it is a fallback precisely because it is not a feature.

---

## What is in, and what is deliberately not

**In:** the schedule as a window with one pair of times; the pure functions that answer *is it inside*
and *when does it next open*, with midnight and daylight saving both swept rather than reasoned about;
one stored deadline with three more writers and an asymmetry deciding what each may do to it; an inexact alarm and the
receiver it wakes, with the battery-optimisation exemption that is the reason it is allowed to start a
foreground service at all; the hand-off for that exemption and the live read behind it; the autostart
grant re-read host-side rather than asked for again; a schedule screen and the one row that reaches
it; and — first, before any of it — the measurement that says whether this ROM will run an inexact
alarm's foreground-service start at all.

**Not in, and the phase that owns each:** ultra dark and the Quick Settings tile (2b), the mark, the
tip and the rate-on-Play link (5). Also not in, and owned by nobody because they are `PLAN.md`'s *Not
in this plan* list: per-day windows, more than one window, saved presets, per-app rules, a Tasker
intent API, and inactivity-based auto-off.

**The tempting one is per-day windows**, and it is worth being exact about why it is out, because the
usual reason does not apply. `CLAUDE.md`'s cardinality test says seven per-day windows would be
**fourteen settings, not a table** — so the storage argument that keeps presets out does not keep this
out. What keeps it out is that it is a different feature wearing this one's clothes: seven windows
need seven rows of UI, a copy-to-all-days affordance, a "weekdays/weekends" shorthand nobody agrees
on, and a `Schedule` type that is a list rather than a pair — at which point §2's two pure
functions become seven-way and §3's earlier-wins has seven candidates instead of two. It is not
foreclosed and it costs no migration when it comes; it is simply not this phase, and the sentence to
keep is that **the reason is size, not storage.**

**The second tempting one is a second alarm for the off-time.** §5 refuses it and gives the
reasoning there rather than here, because the refusal is only defensible once §4 exists.

---

## Checkpoints

**Seven checkpoints. One of them is not a merge, and the first one is not part of the schedule.**
Each leaves the app working, ships its copy complete
in both locales — `scripts/translation-gate.py` is a merge gate, so that is enforced rather than
remembered — and is a point at which the phase could stop without stranding anything.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **0** | The morning the shade did not lift — §5's screen-on receiver, and R10 | `fix:` | phone attached |
| **A** | **The gate** — will an inexact alarm start a foreground service on this ROM? | `chore:` debug apparatus, then no commit at all | phone attached, autostart read, **one night** |
| **B** | The window and the deadline as pure functions with their tests, and both call-site sweeps | `refactor:` + `test:` | nothing |
| **C** | Storage, the alarm, the receiver, and every place it is re-armed | `chore:` | B, and **A's verdict** |
| **D** | The schedule screen, the row that reaches it, the battery hand-off, and the launcher default | `feat:` | C |
| **E** | The readings — the second overnight run, the reconcile paths, the emulator pass | no commit | D |
| **F** | The documents | `docs:` | everything above |

**0 is not part of the schedule and that is why it goes first.** §5's screen-on receiver repairs a
defect that is in 0.5.0 today: any auto-off deadline expiring while the phone is asleep leaves the
shade up for as much as a minute after it is picked up. Held to the end of the phase, it is in the
build the twelve are testing — and they will report it as *"the shade didn't turn off"*, which is
**also the schedule's symptom from a completely different cause**. Shipping it first is what keeps the
closed test's bug reports from arriving pre-mixed. It depends on nothing here, R10 is minutes rather
than a night, and the phone is already attached for A.

**A is a gate, not a task, and it runs before a line of this phase's real code is written.** Phase 1
carried one that could veto the backlight half; Phase 3 carried one that could veto the panel. It is
taken against a **bare** receiver and a bare alarm — no schedule, no preferences, no UI, no decision
logic — because the question is about the platform and the ROM and nothing else, and every line
written before the answer is a line written on a bet. §1 is the whole of it, including what happens on
each of the three verdicts.

⚠️ **What A vetoes is one file, not three quarters of the phase, and that is a correction
to an earlier draft of this document.** §6's reconcile means a process that starts for any reason
inside the window raises the shade — so `work/ScheduleAlarm.kt` and one `<receiver>` are the only
things downstream of R2, and everything else (the window, the deadline rule, the marker, the screen,
the copy) is identical in both worlds. The gate keeps its position because it is cheap and because it
still decides §7's banner and §11's three drafts; it does not keep the stakes it was written with.

**A costs a night, and that is the change that makes it a gate rather than a rehearsal.** §13 says in
as many words that `force-idle` is a simulation, so a gate taken only under `force-idle` vetoes on a
reading this document has already disowned. R4 moves inside A: the same bare apparatus, one alarm
armed about ten hours out, a phone put down and left to reach natural Doze on its own. It is the
cheapest night in the phase, because nothing has been built yet for it to waste.

**B is deliberately in front of C and not beside it**, and it is the `ShadeRamp.kt` precedent for the
third time: the arithmetic lands with its sweep, wired to nothing, at the cheapest possible moment to
be wrong. It is also the only checkpoint that touches code the app is already running — the four
existing `deadlineFor` call sites — which is why it is a `refactor:` with no behaviour change and its
own regression rows proving exactly that.

**C ships dead and that is on purpose.** With `schedule_enabled` defaulting to false and no UI able to
turn it on, everything C lands is unreachable — the alarm is never armed, the receiver never fires.
That is the same shape as Phase 3's F `chore:`, and the reason is the same: a receiver that can raise
the shade over somebody's phone is worth putting in front of a reviewer before it is put in front of a
user.

**E can find things D cannot, and it is the *second* checkpoint that costs a night.** A now carries
the first, against the bare apparatus (R4). E's is the same shape against the real feature — a real
22:00 on a phone that has been in Doze since the morning, which is a wall-clock day per attempt — and
it still earns its night, because a bare receiver and a receiver that reads preferences, writes two
keys and starts a foreground service are not the same load on a ROM deciding whether to run either.
`scripts/doze-capture.sh` exists for exactly this and already dumps `dumpsys alarm`.

---

## 1. The gate: will an inexact alarm start a foreground service at 22:00?

### The question, in the form that can be measured

`PLAN.md` states the mechanism as settled: *"an **inexact `setAndAllowWhileIdle` alarm** plus the
battery-optimisation exemption — that exemption is on Android's own list for starting a foreground
service from the background."* Both halves of that are true of the platform as documented. Neither is
a statement about this phone.

Three separate things have to hold, and they fail in different places:

1. **The alarm fires while the device is in Doze**, and not at the next maintenance window hours
   later. `setAndAllowWhileIdle` is the documented Doze-capable inexact alarm and the platform
   rate-limits it to roughly **one fire per nine minutes per app** while idle. That rate limit is not
   a bug and it is the honest ceiling on the promise this feature makes: *within a few minutes of
   22:00*, never *at* 22:00.
2. **The receiver's `startForegroundService` is allowed.** Starting a foreground service from a
   background broadcast is refused by default with `ForegroundServiceStartNotAllowedException`. The
   exemption being relied on is the **power allowlist** — which is what a user-granted
   battery-optimisation exemption puts the app on — and unlike the temporary allowlist an *exact*
   alarm confers, it is not time-boxed. This is the half `work/BatteryExemption.kt` was written for
   and has never been called from.
3. **HyperOS starts the process at all.** ADR-0003's own observation is that without autostart the ROM
   does not start the process for a broadcast — *not `BOOT_COMPLETED`, not `MY_PACKAGE_REPLACED`, not
   an explicit `am broadcast`*. An alarm's `PendingIntent` is a broadcast. There is no reason to think
   this one is exempt and every reason to check.

Only the first is bounded by documentation. The second is documented and worth confirming because a
`ForegroundServiceStartNotAllowedException` at 22:00 on a stranger's phone is invisible. The third is
vendor behaviour and documentation says nothing at all.

### The apparatus, under the debug seam

A bare alarm and a bare receiver, in `app/src/debug/`, with **their own manifest** so the release
artifact never carries either:

```
app/src/debug/AndroidManifest.xml          <receiver android:name=".shade.GateReceiver"
                                            android:exported="false" />
app/src/debug/java/app/gloam/shade/GateReceiver.kt
```

The receiver does one thing — `Log.i(TAG, ...)` the instant it ran and the lateness, then
`startForegroundService(ShadeService)` inside a `runCatching` that logs the exception class rather
than crashing — and `DebugSettings.kt` gains one button that arms it *n* minutes out with
`setAndAllowWhileIdle(RTC_WAKEUP, ...)`. Nothing reads a preference; nothing writes one.

**One more log line than that, and it is §7's whole argument.** The button also logs
`isIgnoringBatteryOptimisations()` at the moment it arms, so every cell below records **three** things
rather than one: the state that was set, what the app believes about it, and whether the alarm fired.
§7 claims that read is the truth on this phone and nothing has ever checked it — a claim about
Android's allowlist is not a claim about HyperOS's own battery policy, and the two live on different
screens. It costs one line against apparatus already being built. That is the
whole apparatus, and it commits to none of §2, §3, §4 or §9.

⚠️ **The gate measures `…gloam.debug`, not the release id.** The power allowlist and HyperOS's
autostart list are both per-package, so R1 to R3 have to be granted against the debug package and the
verdict transfers on that basis. Worth one line here because the failure is silent and expensive: a
night spent against grants sitting on the release id reads exactly like a ROM that refused.

**The debug manifest is new and is the point of the file layout, not an accident of it.**
`CLAUDE.md`'s rule is that developer-only surface lives in `app/src/debug/` and never in `main/`
behind `BuildConfig.DEBUG`, because with `isMinifyEnabled = false` a statically-false branch is still
compiled into the AAB. A receiver is the case where that matters most: a manifest entry is read by the
OS before any code runs, so a `<receiver>` in `main/` is a live entry point in the shipped app that no
`if` can hide. `scripts/aab-permissions.py` and `aab-reflection.py` read the artifact and are the
check that it stayed out.

### The matrix, and the one thing that must be re-read first

⚠️ **Read autostart before every cell.** The grant lapses on its own — `device-gate.py`'s own
docstring records it granted in the morning and gone by that evening with neither `pm clear` nor a
reinstall responsible — so a run against an unknown autostart state proves nothing either way.

```bash
python3 scripts/device-gate.py                    # read it, every time
python3 scripts/device-gate.py --autostart on     # set it, then re-read
```

Four cells, of which three are worth taking:

| | Battery exemption | Autostart | What a fire proves |
| --- | --- | --- | --- |
| **R1** | off | on | Whether the exemption is load-bearing at all, or whether HyperOS was the only gate |
| **R2** | on | on | The shipping configuration, and how late it is |
| **R3** | on | off | What a Xiaomi user who granted one and not the other actually gets |

**Each cell records three things, not one:** the exemption state as set, `isIgnoringBatteryOptimisations()`
as the app reads it, and whether the alarm fired and how late. The middle column is §7's, and a
disagreement between the first two is a finding in its own right — it would mean the banner cannot
tell the truth on this ROM, which rewrites §7's table before §11's copy is written. **R2 no longer
carries "if this does not fire, the feature does not exist"**: §6's reconcile is what changed that,
and the third verdict below says what is actually lost.

Taken awake first (fast, cheap, proves the code path), then **in forced Doze**, which is the one that
matters:

```bash
adb shell dumpsys battery unplug         # Doze will not engage on a charging device
adb shell dumpsys deviceidle force-idle  # this is what removes the recent-use allowlist
# arm from the debug button, then:
adb shell dumpsys deviceidle step        # walk it, rather than waiting for the state machine
adb shell dumpsys alarm | grep -A3 gloam # what is armed, and for when
adb logcat -s GloamGate:*                # what ran, and how late
adb shell dumpsys battery reset          # ⚠️ always, or the phone thinks it is unplugged forever
```

**Read the lateness, not the fact.** A fire nine minutes late is the documented rate limit doing its
job and is a pass; a fire ninety minutes late is Doze holding the alarm to a maintenance window and is
a different feature than the one being planned.

### And then the same cell overnight, which is the reading the verdict is taken on

`force-idle` is a simulation, and §13 says so about itself. Stepping the state machine by hand skips
whatever the ROM does on its own between 23:00 and 07:00, which on this phone is the entire question.
So R2 is re-taken with **no `force-idle` at all**: arm the debug button about ten hours out, unplug,
put the phone down, and read the log in the morning.

```bash
python3 scripts/device-gate.py             # autostart, again, before putting it down
# arm from the debug button at ~10 h, unplug, and leave the phone alone until morning
adb logcat -d -s GloamGate:* | tail -5     # what ran, and how late
```

**One night, against the bare apparatus, before anything is built on the answer.** It is R4, moved
here from E, and the move is the point: a gate whose reading is a simulation is a rehearsal for a
gate. E keeps a second overnight run against the real receiver, because the bare one proves the ROM
will start *a* process and not that it will start this one.

### The three verdicts, decided now rather than argued about later

**(i) R2 fires within minutes.** Build as planned. The copy in §11 promises *within a few
minutes* rather than *at*, and R1 decides whether §7's warning is "the schedule may be late" or
"the schedule will not run".

**(ii) R2 fires and R3 does not.** Also build as planned, and the Xiaomi half of §7 becomes
load-bearing rather than a footnote: the schedule screen carries **both** hand-offs on a phone with an
autostart manager, and the copy says the ROM decides. This is the outcome ADR-0003 predicts.

**(iii) R2 does not fire.** Then scheduled-*on* is not deliverable on this ROM with the mechanism
`PLAN.md` chose, and the phase does not get to improvise. Three responses, in order:

- **Ship the schedule without the alarm**, which is a narrowing rather than a cut and is the
  response. §6's reconcile raises the shade whenever a process starts inside the window for any
  reason — a launch, a reboot, an update — so what is lost is not the feature but its on-*edge*: the
  shade comes up the next time you open Gloam inside your window, rather than at 22:00 with the phone
  in your pocket. That is honest copy a person can act on, and a great deal more than the off-half
  alone this section reached for in an earlier draft. Scheduled-*off* is untouched in that world,
  because §5's loop never depended on the alarm.
- **Reopen `SCHEDULE_EXACT_ALARM`, as a decision and not as a fix.** ADR-0003 rejects it on Play
  policy — a declaration form, and a dimmer that does not qualify as an alarm or clock app — and
  `PLAN.md` inherits that. Reversing it is a new ADR, a Console form and a listing risk on an app that
  has not shipped yet. **It is not a thing to reach for at the end of a long debugging session**,
  which is precisely why it is written down here, before that session.
- **Cut the phase**, record the reading that cut it in `PLAN.md`, and let the schedule be the thing
  the twelve are asked about rather than the thing that was quietly attempted.

Whichever it is, **A produces a reading and not a commit.** The `chore:` is the apparatus.

### The verdict, taken 2026-09-05: **(ii)**, and one finding neither verdict anticipated

**R2 fired and R3 did not**, which is the outcome ADR-0003 predicts and the second of the three
verdicts written above. Build as planned; the Xiaomi half of section 7 is load-bearing rather than a
footnote. The numbers are in the readings block, and the three cells say this between them:

- **The alarm fires in forced deep Doze** with the exemption and autostart both granted (R2), 90 s
  late.
- **The exemption is load-bearing, and R1 says exactly which half it buys.** Without it the alarm
  still *fires* - the broadcast is delivered, the process runs, the receiver's first log line lands -
  and then `startForegroundService` throws
  `ForegroundServiceStartNotAllowedException: ... mAllowStartForeground false`. So the power
  allowlist licenses the *service start* and not the *alarm*, which is the split this section's
  second question guessed at and now does not have to.
- **Autostart is the other gate and it is absolute.** R3 - exemption granted, autostart denied - did
  not fire at all in fifteen minutes, and was still armed in `dumpsys alarm` at the end. Armed and
  run remain ADR-0003's two different things.

⚠️ **The finding neither verdict anticipated: the lateness is the platform's batching
window, and it is much wider than the rate limit this section was written about.** Every cell fired
at `maxWhenElapsed` *exactly* - 90,017 ms, 90,045 ms and 90,049 ms late across awake,
Doze-with-exemption and Doze-without - and `dumpsys alarm` says why: an inexact alarm is given a
window of **75% of its futurity, clamped to one hour**, and this ROM delivers at the far end of it
rather than at the near one.

| Armed | `window=` | Delivered |
| --- | --- | --- |
| 2 minutes out | `+1m29s997ms` - 75% of 120 s | at `maxWhenElapsed`, 90 s late |
| 10 hours out | `+1h0m0s0ms` - the cap | *(R4 reads this one; the window allows up to an hour)* |

**That is a fact about `AlarmManager` rather than about HyperOS**, and it is worth being exact that
it was measured rather than read off documentation: the window is visible in `dumpsys alarm` the
instant an alarm is armed, so the ten-hour figure cost seconds rather than the night R4 still owes.

**What it does to section 4 is a design change, and section 4 carries it.** `armScheduleAlarm` as
specified arms `nextOn` directly, which for a schedule set the morning before is an alarm armed a
long way out - so the shade would come up somewhere in an hour-wide window after 22:00, and section
11's *within a few minutes* would be false copy. The chain that fixes it is in section 4; it is
cheap, it is inside the mechanism the plan already chose, and finding it before checkpoint C was
written is what the gate was for.

**One thing the forced-Doze cells could not test, and it is still R4's.** `am kill` refuses to kill
an app Android considers unsafe to kill, so the process was alive in every cell when its alarm
arrived - which means *whether this ROM starts a process for a broadcast* has not been asked yet.
Only a night against a process the ROM has already reaped can ask it.

---

## 2. The window — one pair of times, midnight, and daylight saving

### The shape

`shade/Schedule.kt`, no Android imports, in the shape `ShadeRamp.kt` and `AutoOff.kt` established:

```kotlin
/** The nightly window: on at one time, off at another. One pair, not two independent switches. */
data class Schedule(
    val enabled: Boolean,
    val onAt: LocalTime,
    val offAt: LocalTime,
)

/** Is the window open at [now]? Half-open: 22:00 is inside it, 07:00 is not. */
fun Schedule.contains(now: Long, zone: ZoneId): Boolean

/** The next instant **strictly after** [now] at which the window opens, or null if it never does. */
fun Schedule.nextOn(now: Long, zone: ZoneId): Long?

/** The instant the window containing [now] closes, or null when [now] is outside it. */
fun Schedule.windowEnd(now: Long, zone: ZoneId): Long?

/** The instant the window containing [now] opened, or null when [now] is outside it. */
fun Schedule.windowStart(now: Long, zone: ZoneId): Long?
```

`java.time` is available unconditionally at `minSdk` 33 — it desugars from API 26 and Gloam ships to
nothing below 33 (ADR-0008) — so there is no `ThreeTenABP`, no desugaring flag and no `Calendar` in
this file.

**`windowStart` is the fourth function and it exists for §6's reconcile**, which has to tell *this
window has not been acted on* from *this window was acted on and the user ended it*. The on-instant of
the window containing `now` is the identity of a night, and it is the value the marker holds. It is
the exact mirror of `windowEnd` — same derivation, same helper, same null outside the window — so it
costs the sweep in §14 one more column rather than a new argument.

**`ZoneId` is a parameter and not `ZoneId.systemDefault()`.** A pure function that reads the device's
zone is not a pure function, and the DST rows below are exactly the ones that cannot be written
against whatever zone the test machine happens to be in. The zone is resolved at the one impure
boundary, in §3's `beginShadeAt`.

### The comparison, which is rule 3's second test

Three cases, and the third is the one `PLAN.md` names:

| `onAt` vs `offAt` | Shape | Inside when |
| --- | --- | --- |
| `on < off` | Same day — 13:00 to 17:00 | `on <= t < off` |
| `on > off` | **Crosses midnight** — 22:00 to 07:00 | `t >= on` **or** `t < off` |
| `on == off` | Degenerate | **Never.** See below |

**Half-open, `[on, off)`, and it agrees with `isDue` by construction.** At exactly `onAt` the window is
open; at exactly `offAt` it is closed. `isDue` uses `>=` for the same reason — a reader that disagreed
with the loop by one millisecond would leave a deadline that is exactly now unfired until the next
re-check — and a window whose end did not agree with it would fire the deadline a minute after
`contains` had already gone false.

**`onAt == offAt` is an empty window, never a twenty-four-hour one.** Both readings are available and
only one of them is safe: "on at 22:00, off at 22:00" read as a full day is **a shade that never comes
down**, which is the single failure this app exists to design out, arrived at by a user who tapped the
same time twice. Read as empty it is a schedule that does nothing, which is visible, harmless and
obviously wrong to the person who set it. The pure function answers *never* — `contains` false,
`nextOn` null, `windowEnd` null — and §10's screen refuses to save it with a sentence saying
why, so the function's answer is a floor rather than the user's whole experience of the mistake.

`enabled == false` answers the same three ways. A disabled schedule is an empty one, so no caller
needs to check the flag before asking.

### Daylight saving, which nobody asks for and everybody has

The window is **wall-clock local time**: "on at 22:00" means 22:00 wherever and whenever you are, and
that is the only reading a person would accept from a nightly schedule. It follows that the instants
are derived from the zone every time and never cached, which is ADR-0003's own rule — *nothing
persists a schedule; due dates are derived* — arriving in the one Gloam feature that ever wanted it.

Both derivations go through one private helper, and its behaviour on the two irregular days is
`java.time`'s rather than ours:

```kotlin
/** The first instant strictly after [now] whose local time is [time]. */
private fun nextOccurrence(time: LocalTime, now: Long, zone: ZoneId): Long
```

- **Spring forward.** In `Europe/Warsaw` on the last Sunday in March, local times from 02:00 to 02:59
  do not exist. `LocalDate.atTime(t).atZone(zone)` resolves a gap **forwards by the size of the gap**,
  so a schedule set to come on at 02:30 comes on at 03:30 that one night. Deterministic, documented,
  and the alternative — throwing, or skipping the night — is worse in both directions.
- **Autumn back.** On the last Sunday in October, 02:30 happens twice. `atZone` resolves an overlap to
  the **earlier** offset, so the shade comes on at the first 02:30 and not the second. Also
  deterministic; also the answer a person would give.
- **The window's length changes by an hour on those two nights**, in both directions, and that is
  correct rather than a bug to compensate for: the user said *off at 07:00*, and 07:00 is when the
  clock says 07:00.

**What is not handled, deliberately: the phone changing time zone while an alarm is armed.** Android
broadcasts `ACTION_TIMEZONE_CHANGED` and Gloam does not listen for it. The cost is bounded and small —
**at most one transition at the wrong wall-clock time**, after which §6's re-arming corrects it
from the new zone. The cost of listening is not: a fourth manifest receiver, a fourth entry point the
ROM must be willing to start, on a feature whose reason to exist is a nightly routine at home.
Recorded here so nobody rediscovers it as a bug, and one line in §11's copy would be
over-explaining it.

### The property that stops the alarm from looping

**`nextOn` is strictly greater than `now`, always.** This is not tidiness: §6 re-arms the alarm
from inside the receiver the alarm just fired, so a `nextOn` that could answer *now* is an alarm that
re-arms itself for the instant it is already at — a broadcast loop on a user's phone, at 22:00, with
a foreground service start in it. The implementation walks candidate days (today, tomorrow, and one
more, because a DST gap can push today's candidate past tomorrow's naive one) and takes the first
strictly-future instant; §14 sweeps a whole year of minutes asserting it.

---

## 3. One deadline, and the earliest promise owns it

### The problem, stated exactly

`off_at_millis` is one value. After this phase it has two writers with different ideas about when the
shade should come down, and `PLAN.md` calls resolving them *"the exact failure being designed out"*.
It is worth naming what the failure actually is, because there are two candidates and only one of them
is a safety property:

- **A deadline that is silently lost** — the user is promised 23:00, something overwrites it with
  07:00, and the shade is up all night. This is the failure. It is the app's named fear, in
  `PLAN.md`'s own words: *"I hate when other apps have no auto disable and the next day I cannot see
  anything on my phone."*
  ⚠️ **The load-bearing word is *silently*, and it has to be earned rather than asserted.** The
  table below contains that exact sequence of writes — 23:00 replaced by 07:00, on the second row —
  and calls it correct. What separates the row from the failure is not the arithmetic, which is
  identical, but whether the person can see it happen: adoption is something they asked for when they
  enabled the schedule, and an extension they cannot observe is indistinguishable from a deadline
  that was lost. So the row carries an obligation, discharged below and in §11.
- **A deadline that is silently shortened** — the user expects 07:00 and gets 23:00. Annoying, visible,
  self-correcting (the Start button is right there), and it errs toward light.

**Every rule below errs toward the second.** That is the same direction ADR-0010's two bounds err, the
same direction `BootReceiver`'s three refusals err, and the same direction the notification permission
went in Phase 2 — the app runs and says what was given up rather than withholding the feature, but
where the two failures are *asymmetric* it takes the visible one.

### The rule

> **`off_at_millis` is the earliest of the deadlines live at the moment it is written. It is
> *resolved* at exactly two moments — the user's hand, and the schedule's on-instant — and every
> other writer may only bring it forward.**

**Three kinds of writer, and the asymmetry between them is the invariant.** The rule started as "two
writers, earliest wins", which was not enough: an edit to the schedule and a missed on-instant both
need to touch this key, and neither is a start.

| Writer | When | May it move the deadline *later*? |
| --- | --- | --- |
| The hand — Start, or a chip on a running shade | `beginShadeAt(ByHand)` | **Yes.** An explicit act, and the shade is going up now |
| The schedule's on-instant | `beginShadeAt(BySchedule)`, §4 | **Yes**, and only here. The adoption row |
| The schedule being edited under a live shade | §6's collector | **No.** `min(stored, windowEnd(now, zone))` |
| A window found already open | §6's reconcile | Resolves as a start; the marker stops it repeating |

So the safety property is *monotone except at a start*: outside the two resolving moments, the stored
deadline can only ever come forward, never go out. That is what §14 asserts, and it is a stronger
claim than the one this section made when it had only two writers to keep track of.

The candidates, and when each is live:

| Candidate | Live when | Value |
| --- | --- | --- |
| **Auto-off** | The episode is **hand-started** — Start, or a chip tap on a running shade | `now + choice.minutes`, null for `Never` |
| **The window's end** | The shade is up **inside** an enabled window | `windowEnd(now, zone)`, null outside it |

**Auto-off does not apply to a scheduled episode, and `CONTEXT.md` already decided that** — its
*Auto-off* row reads "the duration a **hand-started** shade lasts". It is not a re-reading for
convenience: auto-off exists because a shade somebody put up by hand can be forgotten, and a shade
that came up by itself at 22:00 with an off-time of 07:00 attached has not been forgotten by anybody.
Applying auto-off to it would mean a two-hour default silently truncating every scheduled night —
the schedule would appear broken in exactly the way that produces support mail.

**The window's end applies to a hand-started episode inside the window, and that is the row that earns
the rule.** Start the shade by hand at 06:00 with a four-hour auto-off and a 22:00-to-07:00 window: the
candidates are 10:00 and 07:00, and without this row the shade sits over the morning for three hours
after the user said *off at 07:00*. Earlier wins; it comes down at 07:00.

So, as a function — and it replaces Phase 2's two-argument one rather than joining it:

```kotlin
/** Why the shade is going up, which decides whether auto-off is one of the deadlines. */
enum class ShadeStart { ByHand, BySchedule }

fun deadlineFor(
    now: Long,
    zone: ZoneId,
    start: ShadeStart,
    autoOff: AutoOff,
    schedule: Schedule,
): Long?
```

⚠️ **Phase 2's `deadlineFor(startedAt, choice)` is renamed, not overloaded.** It survives as
`autoOffDeadline(startedAt, choice)` with its existing test rows intact, and it is `internal` to the
package. Leaving it as a two-argument overload beside the five-argument one is the single most likely
way this rule gets broken later: a new call site takes the shorter signature, compiles, passes every
test, and quietly ships a shade the schedule cannot bound. An overload is not a convenience here, it
is a trap with the same name as the safe thing.

### The four call sites, and where the composition actually happens

Four places call the old function today:

```
ui/dim/DimViewModel.kt:98    beginShade()          - the Start button
ui/dim/DimViewModel.kt:120   setAutoOff(choice)    - the chip, rewriting a live deadline
shade/ShadeService.kt:752    setPanelAutoOff()     - the same chip, from the panel
shade/ShadeService.kt:785    togglePanelRunning()  - Start, from the panel
```

All four now need the schedule and the zone as well as the auto-off choice, which is four copies of a
three-line read. So the composition moves behind one `suspend` extension, in `shade/Deadlines.kt`:

```kotlin
/** Read both settings, resolve every live deadline, and store the earliest with the flag. */
suspend fun AppPreferences.beginShadeAt(
    start: ShadeStart,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
)
```

**An extension in `shade/` rather than a method on `AppPreferences`.** `AppPreferences` is a store: its
job is keys, defaults and transactions, and the one piece of policy already in it — collapsing
`NO_DEADLINE` to null — is there because it is the boundary that owns the sentinel. Which deadline
wins is not storage policy, it is the shade's policy, and it belongs beside the pure function it
calls rather than in the file that decides what a preference file looks like. The default arguments
are what keep the callers to one line and the test to none.

All four call sites become `preferences.beginShadeAt(ShadeStart.ByHand)`. §4's receiver is the
one caller that passes `BySchedule`.

⚠️ **`setAutoOff`'s `running` guard has to survive the move, and it is not visible in the list
above.** `DimViewModel.setAutoOff` and `ShadeService.setPanelAutoOff` both write the deadline *only*
`if (preferences.shadeIntent.first().running)`, because `beginShade` writes `running = true` in the
same transaction — so a call site rewritten to a bare `beginShadeAt(ByHand)` turns the shade **on**
from a chip tap while it is off. The pair exists so a deadline is never written without the flag
beside it, and that is exactly what makes the naive rewrite compile and ship.

### What this rule does at each moment, as a table nobody has to re-derive

Window 22:00 to 07:00, auto-off two hours, and the shade going up:

| Moment | Start | Candidates | Stored | Reads as |
| --- | --- | --- | --- | --- |
| Hand start, 21:00 | ByHand | auto-off 23:00 | **23:00** | Outside the window; the schedule promises nothing yet |
| ...then 22:00 arrives | BySchedule | window end 07:00 | **07:00** | The schedule takes the episode over and pushes the deadline out |
| Hand start, 23:00 | ByHand | 01:00, 07:00 | **01:00** | Inside the window, but the hand was involved and auto-off is shorter |
| Hand start, 06:00, auto-off 4 h | ByHand | 10:00, 07:00 | **07:00** | The row that earns the rule |
| Scheduled start, 22:00 | BySchedule | 07:00 | **07:00** | Auto-off is not a candidate |
| Chip tap "30 min" at 23:00 on a scheduled shade | ByHand | 23:30, 07:00 | **23:30** | An explicit override, and it errs toward light |
| Hand start, 13:00, schedule enabled | ByHand | 15:00 | **15:00** | Outside the window; `windowEnd` is null, not tomorrow's 07:00 |
| Hand start, `Never`, schedule disabled | ByHand | none | **null** | The only unbounded shade this app can produce |

**The second row is the one that makes the schedule work, and it is why §4's receiver fires even
when the shade is already up.** Without it, a user who starts by hand at 21:00 gets their schedule
silently skipped every night they happened to be early — the shade is up, so the on-edge does
nothing, and auto-off takes it down at 23:00 in the middle of the reading the window exists for.

**It is also the one place in this app where a deadline moves outward, so it is the one place that has
to be visible.** The only surface showing `off_at_millis` today is `DimScreen`'s *"Turns off at
23:40"* line, and at 22:00 the user is in a reading app, not on that screen. So the obligation from
the failure definition above is discharged in the ongoing notification, which *is* on their screen:
**the deadline goes in `setSubText`** — *"Until 07:00"* — leaving `setContentText` to Phase 2's
backlight sentence, which would otherwise be displaced on every episode that has a deadline, which
with a two-hour auto-off default is nearly all of them. One string, one format argument, and the
subtext is absent rather than empty when there is no deadline. Undoing a Phase 2 decision as a side
effect of a Phase 4 edit is the kind of thing this repository writes ADRs to catch, so it is written
down here instead.

**The third row is the one somebody will complain about**, and it is a taste rather than a safety
property: inside your own reading window, tapping Start gives you two hours rather than the night.
The alternative — auto-off not applying inside the window — makes auto-off stop working on exactly
the nights the user is most likely to fall asleep with the phone. It errs toward light, it goes into
`DOD.md` as a rule-5 question for the twelve, and the answer is a one-line change to the `ByHand` row
or nothing.

**The last row is asserted, not tolerated.** `Never` plus no schedule is the one configuration where
nothing brings the shade down on its own, it is reachable by choice from the auto-off chips today, and
§14 asserts that it is the **only** one — because the useful thing about that assertion is what
it says about every other row.

---

## 4. Scheduled-on: the alarm, and the receiver it wakes

### The alarm

One alarm at a time, for the next transition, re-derived from the window every time anything happens.
ADR-0003's *"nothing persists a schedule; due dates are derived"* is the rule and it survives intact:
there is no per-night enqueue, nothing to cancel, nothing to orphan, nothing to double-fire, and no
state anywhere that a restore could disagree with. `work/ScheduleAlarm.kt`:

```kotlin
/** Arm the next scheduled-on, or cancel if the schedule is off. Idempotent; call it freely. */
fun Context.armScheduleAlarm(schedule: Schedule, now: Long = System.currentTimeMillis())

private fun Context.schedulePendingIntent(): PendingIntent =
    PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, ScheduleReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
```

⚠️ **`nextOn` is the target and not the arm, and checkpoint A's readings are why.** An inexact
alarm is delivered inside a window of **75% of its futurity, capped at one hour**, at the far end of
it - measured three times in section 1. So arming `nextOn` directly means a schedule set at breakfast
puts the shade up somewhere in the hour after 22:00, which is not a nightly reading window anybody
would accept and is not what section 11's copy says.

**The fix is a chain of hops rather than one arm**, and it needs nothing the mechanism does not
already have. Let `gap = nextOn - now`. An alarm armed for `now + x` is delivered no later than
`now + x + min(0.75x, 1h)`, so it cannot overshoot the on-instant as long as

```
x + min(0.75x, 1h) <= gap    ->    x = if (gap >= 2h20m) gap - 1h else gap / 1.75
```

Each hop lands strictly inside the last 43% of the remaining gap and never past it, so the gap
shrinks geometrically: a 24-hour gap closes to under five minutes in about five hops. Five wakeups a
night, rather than the one this section first assumed - and rather than the every-ten-minutes poll
that the obvious fix would have been.

**Every hop is already a branch this receiver has.** A hop fires *before* the window opens, so
`contains(now)` is false, the receiver's second refusal logs *outside the window* and declines, and
the re-arm at the bottom of the `when` arms the next hop. The chain is therefore a change to
`armScheduleAlarm` and one constant, with nothing new in the receiver at all - and section 14 gains
one property to sweep: **no hop overshoots**, for every gap and every schedule.

Four decisions inside those few lines, each of which has a wrong answer that compiles:

- **`setAndAllowWhileIdle`, not `setExact*` and not `setAlarmClock`.** Both exact forms need
  `SCHEDULE_EXACT_ALARM`, which needs a Play declaration form for an app that is not a clock or a
  calendar (ADR-0003). `USE_EXACT_ALARM` is worse — auto-granted, unrevokable, and restricted by
  policy to alarm-clock apps, so declaring it in a dimmer puts the listing at risk to save a settings
  screen.
- **`RTC_WAKEUP`, not `RTC`.** A non-waking alarm fires when the device next wakes — and *turning the
  screen on is a device wake*, which means the race is between the alarm's broadcast and the first
  frame the user sees. Losing that race is a bright flash followed by the shade, on a phone picked up
  in a dark room, which is the exact experience this app exists to prevent. The cost is one wake per
  night.
- **`FLAG_IMMUTABLE`**, required from API 31 and correct anyway: nothing outside this app has any
  business filling in fields on an intent that raises the shade.
- **`FLAG_UPDATE_CURRENT`**, which is what makes re-arming idempotent. The same `PendingIntent`
  identity (same request code, same component) means `set` replaces rather than stacks, so §6's
  re-arm sites cannot produce several alarms.

**No new permission.** `setAndAllowWhileIdle` needs none; the receiver is `exported="false"` and
reached only by this app's own `PendingIntent`; `RECEIVE_BOOT_COMPLETED` is already declared by
`BootReceiver`. The expensive half of the roadmap's last feature adds **zero** lines to the
`<uses-permission>` block, and `scripts/aab-permissions.py` is what proves that on the artifact rather
than in the diff. ⚠️ `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` stays undeclared — it is what would let
the app raise its own exemption dialog, Play restricts it to apps whose core function *is* the
exemption, and the check asserts it absent.

### The receiver, which is mostly refusals

`shade/ScheduleReceiver.kt`, beside `BootReceiver.kt` and in its shape — `goAsync()`, one DataStore
read, a `when` of branches most of which decline. It logs as `GloamSchedule`, for the same reason
`BootReceiver` logs as `GloamBoot`: `adb logcat -s GloamSchedule:*` is how E reads which branch was
taken, and the platform owns a `BootReceiver` tag of its own.

```kotlin
private suspend fun MainApplication.onScheduledOn() {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val schedule = preferences.schedule.first()
    when {
        !schedule.enabled -> Log.i(TAG, "the schedule is off; a stale alarm, nothing to do")

        !schedule.contains(now, zone) ->
            Log.i(TAG, "fired at $now, outside the window; too late to open it")

        !canDrawShade() -> Log.i(TAG, "no overlay permission, leaving the stored intent alone")

        else -> {
            preferences.beginShadeAt(ShadeStart.BySchedule, now, zone)
            preferences.setScheduleHonouredAt(schedule.windowStart(now, zone)) // this night, done
            startShade()
            Log.i(TAG, "scheduled shade up until ${schedule.windowEnd(now, zone)}")
        }
    }
    armScheduleAlarm(schedule, now) // ⚠️ outside the `when` - see below
}
```

**Three refusals and two notes, and every one of the five is a real state rather than defensive
noise:**

1. **The schedule is off.** An alarm survives the preference that armed it being switched off —
   §6 cancels, but a cancel racing a fire is a real ordering and cheap to make harmless.
2. **The alarm arrived after the window closed.** This is the one this phase adds that `BootReceiver`
   does not have, and it is the direct consequence of §1's rate limit: an inexact alarm *can* be
   minutes late, and a 22:00-to-22:05 window with an alarm delivered at 22:07 must not open a window
   that has already ended. Refusing is also what makes the *nine-minute* case safe without any special
   handling — the window is either still open or it is not.
3. **No overlay permission**, refused exactly as `BootReceiver` refuses it: starting anyway would post
   a *Screen dimmed* notification over an undimmed screen, and quiet and wrong is worse than not
   starting.
4. **`shade_running` is left alone on that refusal**, for `BootReceiver`'s reason: it is the user's
   intent, they did not change it, and an app that clears it for them is an app where a revoked
   permission silently becomes a lost setting.
5. **The notification permission is deliberately not a refusal.** Same as `BootReceiver`, same
   reasoning — refusing to dim somebody's screen because they refused a notification is the app
   deciding it knows better — and the honest cost is written down rather than guarded: the hatches
   left are the power menu and the app's own controls at about 1.59 nits (`phase-3.md` R3), plus, from
   2b, the tile. The window's own end bounds the whole thing, which is more than a hand-started shade
   gets.

**The shade already being up is not a refusal**, and that is §3's second table row: the alarm
fires, the branch runs, `beginShadeAt(BySchedule)` rewrites the deadline to the window's end, and
`startShade()` on a live service is a no-op `onStartCommand` that re-enters `START_STICKY`. That is
the whole of how a hand-started early shade gets adopted by the schedule.

**The re-arm is outside the `when` and that placement is load-bearing.** Every branch above, refusals
included, must leave tomorrow's alarm armed — a receiver that re-armed only on the success path would
switch the schedule off permanently the first time the user happened to revoke the overlay permission,
and it would do it silently. It is one line, it is at the bottom, and `finally`-shaped placement is
what it is for.

---

## 5. Scheduled-off needs no alarm, and that is a claim worth defending

The obvious symmetry is two alarms, one per edge. This phase ships one, and the reason is §0's
asymmetry rather than economy.

**At the window's end there is, by construction, a foreground service running.** The only way for the
shade to be up is for `ShadeService` to own its window; the only thing scheduled-off has to do is take
it down; and `ShadeService.awaitDeadline` already takes shades down at a stored instant, re-reading the
wall clock at most every minute because `postDelayed` is scheduled against a clock that stops in deep
sleep. The scheduled off-time is **written into `off_at_millis` as a deadline** by §3,
`collectLatest` re-arms the loop on that write, and the existing mechanism fires it. There is nothing
to build.

**What a second alarm would add is a fourth reader of a value that already has three**, and it would
add it for a case none of the three miss:

- The **loop** covers the shade being up with the process alive, which is the case.
- The **boot receiver** covers the phone being off across the deadline, and refuses to restore a shade
  whose deadline passed.
- The **screen's resume reconcile** — `DimScreen.endShadeIfDue()`, Phase 2's R8b — covers the one case
  the other two do not: HyperOS kills the process, `START_STICKY` does not bring it back (R8a: on this
  phone it did not bring it back *at all*, in either attempt), and nobody is alive to notice the
  deadline pass. **But in that case the window died with the process**, so there is no shade on screen
  to take down; what is wrong is a stored `shade_running` that says `true`, and a comparison of two
  stored values against the wall clock is exactly what fixes it at the next resume.

So an off-alarm would fire, find no shade, and write a preference that the next launch would have
written anyway. It would cost a second `PendingIntent` identity, a second thing for §6 to keep
armed across every site, a second thing to cancel, and a second entry point for the ROM to decline — in
exchange for correcting a stored boolean a few hours earlier than the screen that reads it.

### What the loop does not fix, which is not the same as what it gets wrong

**The wall-clock re-read fixes correctness and says nothing about latency, and the schedule is what
makes the difference matter.** `awaitDeadline` is `delay(min(remaining, DEADLINE_RECHECK_MS))` on
`Dispatchers.Main.immediate` — a `Handler.postDelayed` against `uptimeMillis`. §0 already names that
clock as one that stops in deep sleep and treats re-reading `System.currentTimeMillis()` as the
answer. It is the answer to the wrong half: re-reading the wall clock means the loop can never fire
early or drift, but **nothing in the process is scheduled at all while the CPU is asleep**, so the
deadline is not late by a wrong number — it is not evaluated.

> Window 22:00 to 07:00. The phone is in deep sleep from 02:00. At 07:00 nothing runs. The user picks
> the phone up at 08:14; the screen turning on wakes the device; the pending `postDelayed` still owes
> up to sixty seconds of *uptime*. For up to a minute, the first thing they see is the shade, at the
> dim level, with the backlight override still on.

That is `PLAN.md`'s named fear — *"the next day I cannot see anything on my phone"* — reached through
the feature built to prevent it. Phase 2 has the same loop and could afford it: a two-hour auto-off
usually expires while the user is awake and holding the phone. **The schedule is what makes the
overnight deadline the ordinary case rather than the unlucky one.** `DimScreen.endShadeIfDue()`
(Phase 2's R8b) does not cover it either — that reconciles on *Gloam's* screen, and at 08:14 the user
is opening something else.

**The fix is not §5's second alarm. It is `ACTION_SCREEN_ON`, registered at runtime.**

- It is a protected broadcast that **cannot** be declared in a manifest, so it must be
  `registerReceiver`'d — which means no manifest entry, no permission, and **no fourth entry point
  for the ROM to decline**. Every cost §5 refuses the off-alarm for, this avoids by construction.
- It exists only while `ShadeService` is alive, which is exactly when there is a shade to take down.
  Registered in `onCreate`, unregistered in `onDestroy`; all it does is wake the deadline loop for one
  more wall-clock comparison.
- It rides a wake that has already happened rather than causing one, so it costs no battery, and it
  catches the single instant at which the latency is observable — the screen coming on.

⚠️ **What does not work, recorded so it is not reached for later:** switching the `delay` to
`elapsedRealtime` changes nothing. The problem is that the process is not scheduled, not that the
clock is wrong. Shortening `DEADLINE_RECHECK_MS` changes nothing either, for the same reason, and
costs a wakeup a minute on an awake device to fix a case that only exists on a sleeping one.

**This one is independent of the schedule and repairs auto-off today**, which is why §15 lands it as
its own checkpoint — **0**, before the gate — rather than inside B or D. It is the phase's only
genuine `fix:`, the only line here that reaches a user who never opens the schedule screen, and the
reason it goes first is not tidiness: see the checkpoint table's note about what the twelve would
otherwise report.

**The gap this leaves, stated rather than hidden:** between a ROM kill and the user's next look at
Gloam's own screen, `shade_running` can say `true` with no shade and no service. That is Phase 2's gap
and this phase does not widen it, because the schedule writes the same key through the same pair.

---

## 6. Where the alarm is armed, and every way it is lost

An `AlarmManager` alarm is **not durable**. Five things destroy it and all five are ordinary:

| Lost by | The alarm is recovered at | And the night in progress? |
| --- | --- | --- |
| Reboot | `BootReceiver`, on `BOOT_COMPLETED` — a job it does not have yet | Reconciled |
| App update | `BootReceiver`, on `MY_PACKAGE_REPLACED` — already in its filter | Reconciled |
| Force-stop (the user, or the ROM's task killer) | `MainApplication.onCreate`, next time anything launches | Reconciled |
| The user editing the schedule | The collector below, immediately | Reconciled, and the deadline tightened |
| Firing | The receiver's own re-arm (§4) | It *is* the night |

**The third column is the correction, and without it this table answers the wrong question.** Every
recovery above arms `nextOn`, which is **strictly future** — so recovering the alarm after a
force-stop at 21:50 arms it for *tomorrow* 22:00, and tonight is skipped in silence while the screen
still says the schedule is on. Losing the alarm and losing the night are different failures, and on
HyperOS the second is the ordinary one: swiping Gloam out of recents force-stops it, and the whole
reason this phase is expensive is that this ROM kills things. A table that recovers only the alarm is
a table that answers *"is an alarm armed?"* when the user's question is *"did it come on?"*

Which gives **three call sites** for five loss paths, and one of them carries two:

```kotlin
// MainApplication.onCreate, beside the launcherCompact collector that is already there.
preferences.schedule
    .distinctUntilChanged()
    .onEach { schedule ->
        armScheduleAlarm(schedule)             // the alarm
        preferences.tightenToWindow(schedule)  // the deadline, forward only
        reconcileWindow(schedule)              // the night, if one is open and unspent
    }
    .launchIn(applicationScope)
```

**`tightenToWindow` is §3's third writer and it can only move the deadline forward.**

```kotlin
/** With a shade up, bring the stored deadline forward to the window's end if that is sooner. */
suspend fun AppPreferences.tightenToWindow(
    schedule: Schedule,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
)
```

Without it, editing the schedule under a live shade does nothing until the next on-instant: a
scheduled shade up at 22:30 with a deadline of 07:00, an off-time moved to 23:00, and the shade stays
up until morning against a promise the user has just withdrawn. `DeadlineTest` would pass, because the
pure function was never called. One `min` closes it, and the direction is what makes it safe in every
case at once — an off-time moved *later* leaves the stored value alone, and so does disabling the
schedule, because `windowEnd` is then null or further out. Both err toward light. It is idempotent, so
the collector firing on every process start costs nothing, and it preserves an explicit chip override:
`min(23:30, 07:00)` is still 23:30.

**`reconcileWindow` is what stops a lost alarm from costing the whole night.**

```kotlin
/** Raise the shade if a window is open, has not been acted on, and nothing is up. */
private suspend fun MainApplication.reconcileWindow(schedule: Schedule)
// enabled && contains(now) && honouredAt != windowStart(now) && !shadeIntent.running
//   -> beginShadeAt(BySchedule); setScheduleHonouredAt(windowStart(now)); startShade()
```

**The marker is the whole of why this is safe.** The naive version — *window open, shade down, raise
it* — breaks the Stop button: the user stops at 23:00 inside a 22:00-to-07:00 window and the next
process start puts the shade straight back. Comparing against the on-instant already acted on tells
*never opened this window* from *opened, and ended by the person*, so an episode that ends inside a
window spends that night and Stop stays Stop until morning.

⚠️ **"`endShade` writes the marker" is too blunt, and one of its four call sites is a bug.** The four
do not want the same answer:

| Call site | What ended the shade | Spends the night? |
| --- | --- | --- |
| `ui/dim/DimViewModel.kt:104` | The user pressed Stop | **Yes** |
| `shade/ShadeService.kt:396` | Stop, from the notification or the panel | **Yes** |
| `shade/ShadeService.kt:357` | `awaitDeadline` fired | **Yes** — and see below |
| `shade/BootReceiver.kt:124` | A deadline that passed while the phone was off | **No** |

The last row is the bug. Phone off at 21:00 with a hand-started shade and a passed deadline, booted at
23:00 inside a 22:00-to-07:00 window: `BootReceiver` calls `endShade` to clear the stale intent, a
blunt marker write spends tonight, the reconcile refuses, and **the schedule is skipped for a reason
that has nothing to do with the schedule** — on the one path in the app whose whole job is cleaning up
after the phone being off.

**So the marker needs §3's mirror, and §3's architectural ruling applies to it unchanged.** Why a
shade went *up* decides whether auto-off is a candidate, which is why `ShadeStart` exists; why it came
*down* decides whether the night is spent. §3 put that composition in `shade/` as `beginShadeAt`
rather than as a method on `AppPreferences`, because *"which deadline wins is not storage policy, it
is the shade's policy"* — and a schedule-aware marker write inside `AppPreferences.endShade` breaks
that rule in the same file that states it.

```kotlin
/** Why the shade came down, which decides whether the night is spent. */
enum class ShadeEnd { ByHand, ByDeadline, Reaped }

/** Clear the intent, and spend the night if this ending was one the person owns. */
suspend fun AppPreferences.endShadeAt(
    reason: ShadeEnd,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
)
```

`Reaped` is `BootReceiver`'s, and it is the only one that leaves the marker alone. The rename is the
same shape as `beginShadeAt`'s and lands in the same checkpoint, for the reason §15 gives: these four
call sites are the **Stop path**, which `CLAUDE.md` calls the escape hatch, and rewriting the escape
hatch inside a checkpoint whose headline says it is unreachable is the worst place to put it.

⚠️ **The `ByDeadline` row is not settled here, and it must not be settled twice.** A shade
hand-started at 23:00 with a two-hour auto-off ends at 01:00 *inside* the window: spending the night
keeps the schedule out of the way, not spending it means opening the phone at 01:05 puts the shade
back. That is §3's third table row — already a `DOD.md` rule-5 question, already *"the one somebody
will complain about"* — arriving at the other end of the episode. It goes to the twelve as one
question about both edges, not two.

It also carries §1's third verdict. A process that starts inside the window for any reason now raises
the shade, so if R2 says this ROM will not run the alarm, what is lost is the on-*edge* and not the
feature.

**One collector rather than a call in the ViewModel, and the reason is `CLAUDE.md`'s own rule.** A
`ViewModel` never holds a `Context`, and `AlarmManager` needs one — which by the same house rule would
make arming "the screen's job, because that is what has one", the way starting the service is. That
works and it is wrong here: the screen that wrote the preference can be gone before the write lands,
and an alarm armed by a composable is an alarm whose existence depends on somebody looking at it. The
thing whose lifetime actually matches an alarm's is the **process**, and `MainApplication` already owns
a scope for work that outlives any screen and already collects one preference into it.

It also makes the first emission do double duty: the collector fires on every process start, which is
the force-stop row above, so the third recovery site costs no code at all.

**`BootReceiver` gains one line and keeps its shape.** It currently decides about the shade and
finishes; it now also arms, and — like §4's receiver — **arms on every branch including the
refusals**, because a phone that rebooted without the overlay permission still has a schedule. Its four
existing branches are untouched; the arm goes after the `when`.

**Cancelling is the same call.** `armScheduleAlarm` on a disabled schedule cancels the `PendingIntent`
rather than arming it, so the collector needs no branch and there is no second entry point that can be
forgotten. A `nextOn` of null — a disabled schedule, or `onAt == offAt` — cancels for the same reason.

**How E reads whether any of this worked:** `dumpsys alarm` is the only honest answer, and
`scripts/device-gate.py` already reads pending alarms with the warning that matters — *read the count
and the instant, never the notification*, because a notification is downstream of an alarm that fired
and this asks what is armed.

---

## 7. The battery-optimisation exemption, and the one ask whose answer the app can read

### Why this ask, and why now

`PLAN.md` rule 4: *every ask waits for the moment the feature needing it is switched on, and the phase
that introduces an ask owns its hand-off, its re-read, and what the user sees when the answer is no.*
This is the fourth and last of the four asks in the plan, and the feature switching it on is the
schedule's toggle. `work/BatteryExemption.kt` has existed since Phase 2 with nothing calling it — the
file's own header says so, and says that is deliberate — and this is the phase that calls it.

### It is the only one of the four the app can actually read

Worth stating plainly, because it changes what the copy is allowed to claim:

| Ask | Readable in-app? | What the app can therefore say |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | Yes — `canDrawOverlays()` | The truth, live |
| `POST_NOTIFICATIONS` | Yes, **and the channel too** — Phase 2 checks both halves | The truth, live |
| Xiaomi autostart | **No.** No appop, no API, and the OEM screen reports `checked=false` on granted rows | Only what a denial costs |
| **Battery optimisation** | **Yes on AOSP** — `isIgnoringBatteryOptimizations`, no permission required. **On this ROM, unread** | The truth, live — *provisionally*, see below |

⚠️ **The fourth row is a claim about AOSP and an assumption about HyperOS, and this section's whole
argument rests on it.** The `PowerManager` call reads Android's own allowlist. HyperOS keeps a
*separate* per-app battery policy on its App info screen, and nothing here has established that the
two agree. Both divergences are available and the banner lies in a different direction in each: read
exempt while the ROM still restricts, and there is **no banner** over a schedule that never fires —
which is the outcome `PLAN.md` says generates support mail; granted the ROM's way while the AOSP flag
stays false, and there is a **permanent banner** about something already fixed, which is the crying
wolf this section refuses two paragraphs down. It is the same hazard that demotes autostart to *"only
what a denial costs"*, and the only thing exempting battery optimisation from it so far is that an API
exists.

So it gets a reading rather than an assertion. **R1, R2 and R3 each record three things instead of
one** — the state that was set, what `isIgnoringBatteryOptimisations()` believes, and whether the
alarm fired — which costs one `Log.i` in the debug button that arms them, against apparatus §1
already builds. If the three disagree, this row and the two idioms below it are rewritten before §11's
copy is written rather than after a tester finds it.

So this ask gets the **notification-warning idiom** rather than the **autostart-row idiom**, and the
difference is not cosmetic. Phase 2 built both and the distinction is exactly readability: the
notification warning is a live read re-evaluated on every resume, appearing when the state is wrong and
self-clearing when the user fixes it; the autostart row is permanent, makes no claim, and remembers
nothing, because a row that claimed anything would be the app repeating the user's guess back to them
as its own assurance (ADR-0003's central hazard).

Concretely:

- A banner on the schedule screen, shown **only when the schedule is enabled and the exemption is
  missing**, with a *Battery settings* action calling `openBatteryOptimisationSettings()`.
- **That hand-off gains a third destination, and the order is decided by the reading above.** Today it
  tries `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` and falls back to `ACTION_SETTINGS` — the file's
  own comment calls the first *"the whole list, with the user one search away from us"*, and the
  second is Settings' front door. So the copy names a specific control and the button lands the user
  in a haystack, which is a hand-off owned only in the sense that a button exists (rule 4).
  `ACTION_APPLICATION_DETAILS_SETTINGS` with `package:` data lands on **Gloam's own App info page**,
  needs no permission, carries no policy risk, and is one tap from battery on both AOSP and HyperOS.
  It joins the chain; whether it goes *first* depends on whether R1 to R3 show the ROM's own control
  is the switch that governs the alarm, because if it is, App info is not a convenience but the only
  correct destination.
- Re-read on `ON_RESUME`, in the same `DisposableEffect` shape `DimScreen` uses — because the fix for
  it is a settings screen the app hands the user off to, and they come back.
- **No DataStore key.** "Never asked" and "asked and declined" want identical behaviour and are both
  visible from the live read, which is the same argument that kept a key out of the notification ask.

### What it does *not* do

**It does not gate the toggle.** The schedule can be switched on without the exemption, and the banner
says what is at risk. That is the Phase 2 precedent — the shade starts even when the notification
permission is refused, because refusing to dim somebody's screen because they refused a notification is
the app deciding it knows better — and the honest move here is the same one: run, and say plainly what
was given up.

⚠️ **But the strength of the sentence depends on §1's R1**, and the copy cannot be written
before the reading. Writing a stronger sentence than the reading supports is crying wolf on a
permission screen; writing a weaker one is the app knowing something the user does not, which is rule
4's third clause and the one `PLAN.md` says generates support mail.

**There are three states, not two, and §6's reconcile is what added the third.** An earlier draft of
this section framed it as a choice between *may be late* and *will not run*, and the second of those
is now false in the world it was written for: without the alarm the shade still comes up, because a
process starting inside the window raises it. So the drafts are:

| Draft | True when | What it tells the user to do |
| --- | --- | --- |
| `_late` | The alarm fires, held back by the rate limit | Nothing urgent; the exemption tightens it |
| `_delegated` | The alarm does not fire; the reconcile does | **Open Gloam when you settle down** |
| `_never` | Neither fires — kept only if a reading finds it | Grant the exemption or the schedule is decorative |

**The middle one is the likely ship and it is the best of the three**, because unlike the other two it
names an action the user can take tonight without leaving the app. §11 carries all three; A's readings
pick one and the others are deleted before the copy is translated.

---

## 8. Autostart — re-read, not re-asked

`PLAN.md` rule 4 gives this phase the autostart grant with one word: **re-read**. It is not an ask
here; Phase 2 introduced the hand-off with reboot restore and owns the Settings row.

**And the re-read is not something the app performs.** ADR-0003's second amendment is explicit: the
grant is not an appop, there is no API, the OEM screen's own `checked` attribute reads false on granted
rows, and the only thing that can tell a granted state from a denied one is
`scripts/device-gate.py` scraping that screen over `adb` from a host. So "Phase 4 re-reads the grant"
means **a developer runs the script before every reading in §13**, and nothing in the shipped
app changes.

Two consequences that are this phase's rather than Phase 2's:

- **Every cell of §1's matrix and every reading in E is invalid without it**, because the grant
  lapses on its own. This is the phase's most easily-wasted night: an overnight Doze run against an
  autostart state nobody read proves nothing in either direction, and it costs a day to discover.
- **What a denial costs went down, and the copy has to follow.** ADR-0003's observation is that
  without autostart the ROM does not start the process for a broadcast — which kills the alarm and
  `BootReceiver` both. §6's reconcile needs neither: it runs when the *user* launches the app, which
  no autostart policy governs. So a phone with autostart denied now gets a schedule with a worse
  on-edge rather than no schedule, `schedule_rom_note` says that instead of what it used to say, and
  this is a change in consequence rather than in code — nothing in the shipped app reads the grant,
  before or after.
- **The schedule screen points at the existing row rather than growing a second hand-off.** On a phone
  with an autostart manager, the schedule screen carries one line saying the ROM has the last word and
  where the switch is; the switch itself stays in Settings, where `hasAutostartSettings()` already
  gates it to phones that have one. Two hand-offs for one grant is two places to keep honest.

---

## 9. Storage

Four new keys, no migration, no database. `PLAN.md` promised *"four values — on time, off time,
enabled, `off_at_millis`"* with the fourth already in the file; the count below is one more than that,
and the extra one is app state rather than a setting.

```kotlin
val SCHEDULE_ENABLED      = booleanPreferencesKey("schedule_enabled")
val SCHEDULE_ON_MINUTES   = intPreferencesKey("schedule_on_minutes")
val SCHEDULE_OFF_MINUTES  = intPreferencesKey("schedule_off_minutes")
val SCHEDULE_HONOURED_AT  = longPreferencesKey("schedule_honoured_at")
```

**`schedule_honoured_at` holds the on-instant of the night most recently acted on**, and it is what
makes §6's reconcile safe. Without it, *the window is open and the shade is down* is indistinguishable
from *the user pressed Stop ten minutes ago*, and a reconcile that cannot tell them apart is a Stop
button that undoes itself at the next process start. It is written by §4's receiver when it raises,
and by `endShadeAt` when an episode the person owns ends inside a window — not by `endShade`, whose
four call sites want three different answers (§6). Its default is `0L`, which is no window and
therefore reconcilable, which is the right answer on a fresh install.

⚠️ **It is deliberately *not* part of `Schedule`, and that placement is load-bearing rather than
tidy.** The reconcile writes it, and §6's collector is what triggers the reconcile — so folding it
into the `schedule` `Flow` means every write re-emits, `distinctUntilChanged` passes it through, and
`armScheduleAlarm` runs a second time. It terminates, because the second reconcile is a no-op, but it
doubles every arm. Worse, the reconcile also needs `shade_running`, which is not in `schedule` either:
combining the two flows to get it would re-arm the alarm on **every shade start and stop**. That is
precisely the noise this file's closing note warns about — *"the log fills with arms nobody asked for,
and §13's R5 reads that log"* — and R5 is the reading that asks whether exactly one alarm is armed.
So the collector stays on `schedule.distinctUntilChanged()` and owns **arming only**; the tighten and
the reconcile are `suspend` one-shots called from the same `onEach`, reading `shadeIntent` and the
marker with `.first()`. Nothing they write is in the flow that triggered them.

**Minutes since local midnight, as an `Int`.** The same shape and the same reasoning as
`auto_off_minutes`: storage holds a number and the domain holds a type. A `LocalTime` serialised as
`"22:00"` would be a string to parse on every read, with a locale-shaped hazard (`22:00` against
`10:00 PM`) in a file that must survive an in-app language change, and a parse failure with nothing
sensible to do about it. An `Int` in `0..1439` is coerced on read the way `dim_level` and `warmth` are,
cannot fail to parse, and compares directly — which is what §2's three cases are written in
terms of.

**Defaults: `false`, `22*60`, `7*60`.** Off, because a screen dimmer that starts dimming on a night the
user did not ask about is indistinguishable from a broken phone — the same argument that makes
`DEFAULT_DIM_LEVEL` modest and warmth zero. The two times are defaults for a *disabled* schedule, so
they cost nothing and exist only so the picker opens on something plausible rather than on midnight.
22:00 to 07:00 is `PLAN.md`'s own example and is the shape of the thing.

**One `Flow` for the three *settings* keys — not the marker — like `shadeIntent` and for a related
reason.** A reader that could
take them apart is a reader that can see the old on-time beside the new off-time — a torn window,
briefly inverted, at the exact moment §6's collector is arming an alarm from it. They are not
written in one transaction the way `beginShade`'s pair is (the picker writes one at a time), which
makes the single read *more* necessary rather than less:

```kotlin
val schedule: Flow<Schedule> =
    store.data.map { prefs ->
        Schedule(
            enabled = prefs[Keys.SCHEDULE_ENABLED] ?: false,
            onAt = timeOf(prefs[Keys.SCHEDULE_ON_MINUTES] ?: DEFAULT_SCHEDULE_ON_MINUTES),
            offAt = timeOf(prefs[Keys.SCHEDULE_OFF_MINUTES] ?: DEFAULT_SCHEDULE_OFF_MINUTES),
        )
    }
```

**Setters: three, and only two of them are the user's.** `setScheduleEnabled(Boolean)`,
`setScheduleWindow(onAt, offAt)`, and `setScheduleHonouredAt(Long?)`, which no screen ever calls —
it is written by §4's receiver, by §6's reconcile and by `endShade`, and it is the one preference in
this file that records what the *app* did rather than what the person asked for. The first two are
the ones the rule below is about:
the window written as a pair in one `edit`, because §2's three cases are properties of the
*pair* and a caller that can move one edge without the other is a caller that can transiently create
the degenerate `onAt == offAt` window while the user is halfway through a picker. The screen picks one
time at a time; the setter writes both.

**The door does not freeze these**, and it is worth saying why the rule does not apply.
`PLAN.md` rule 2 freezes every DataStore key **that has been written**, and these three are written
for the first time by a build that has not shipped. What the door froze was `onboardingDone` — a key
already on disk with nothing reading it — and that was resolved in Phase 2. New keys added after the
door are ordinary; renaming one *later*, once twelve phones hold it, is what is not.

---

## 10. What the user sees, and where it lives

### Where the schedule goes, and the two places it does not

**Not in `DimControls()`.** That composable has three hosts — the full screen, the compact activity and
the panel — and `phase-3.md` states the rule for it: it gets the dim level, warmth, the running
switch and the auto-off chips, and nothing else. Everything added to it must stay legible at 6.64 nits,
fit a window that must never be `MATCH_PARENT` (ADR-0011), and be operable by somebody who cannot see
the rest of the screen. Two time pickers are none of those things.

**Not a Settings section either.** Settings is where the app's *properties* live — its palette, its
language, which surface the icon opens. When the shade is on is not a property of the app, it is the
thing the app does, and it belongs beside the other control over when the shade ends.

**So: a summary row on the dim screen, under the auto-off chips, opening a screen of its own.**

```
+- Dim -----------------------------+    +- Schedule ------------------------+
|  [ Stop dimming ]                 |    |  Dim on a schedule         [ o ]  |
|  Dim level      ---------o--       |    |  Every night, on at one time      |
|  Warmth         --o--              |    |  and off at another.              |
|                                   |    |                                   |
|  Turn off on its own              |    |  Turns on             22:00    >  |
|  (Never)(30 min)(1 h)(2 h)(4 h)   |    |  Turns off            07:00    >  |
|  Turns off at 23:40               |    |  Overnight, into the next morning.|
|                                   |    |                                   |
|  Schedule    22:00 to 07:00    >  | -> |  (!) Battery optimisation is on   |
+-----------------------------------+    |      [ Battery settings ]         |
                                         +-----------------------------------+
```

A new `Schedule : NavKey` beside `Support` and `Licences` — *"a detail screen, not a tab: somewhere you
go once, rather than a place the bottom bar switches to."* The route exists in `NavigationKeys.kt`
with the others; `TopLevelDestination` is untouched.

**The summary row is one line and states the window rather than the setting.** *Schedule - 22:00 to
07:00* when enabled, *Schedule - Off* when not. A user who set a schedule three weeks ago and forgot
finds out on the screen they already open, which is the whole reason it is here and not two taps into
Settings.

**And when the exemption is missing, the row says so rather than stating the window flatly.** §7 puts
the banner on the schedule screen, which inverts this row's own argument: the user who forgot is by
definition not opening the schedule screen, so the honest state would live where they never go and the
unqualified promise where they always do. Phase 2 settled the pattern in the other direction —
*"No Stop button outside Gloam"* sits on `DimScreen`, not behind Settings — so a warning affordance on
the home screen is this app's established idiom rather than a new one.

**The compact host carries the same state in the one line it already has**, and it has to, because the
launcher default above makes it the screen most users land on and `phase-3.md` is explicit that it has
no explainer and no room for a banner. The read-only section says the schedule is at risk instead of
naming the window; the cog is still the way to the screen that can explain it. One string, no new
surface, and it is the only thing in that host that could carry the fact at all.

### The row's placement depends on a default this phase changes

**The paragraph above is only true while the launcher opens the full app**, and this phase flips that.
`AppPreferences.launcherCompact` shipped in Phase 3a defaulting to `false` — the icon opens the full
app and the compact host is the setting — and `DOD.md` holds the inversion open as a `PLAN.md` rule-5
question for the twelve. **It ships `true` from here**, which makes the compact host the screen the
user already opens and takes the summary row's own justification with it.

**The argument that kept it `false` does not survive the device** (R9). `phase-3.md` and `DOD.md` both
rest on one claim: that a first launcher tap landing on a dialog over another app is a bad first
meeting with an app nobody has used yet. It cannot happen. `ControlsActivity.forwardIfUnusable()`
returns to `MainActivity` whenever `canDrawShade()` or `escapeHatchLive()` is false, and on a genuine
first run both are — so the tap lands on the full app with its explainer, measured rather than
reasoned about. The same reading kills a cost the argument never named: a translucent, floating
activity is given **no starting window at all** (`STARTING_WINDOW_TYPE_NONE`), so the bounce paints
nothing and the flash shape iii was thought to buy the icon back with does not exist.

**What the reading found instead is a row nobody had written down.** `escapeHatchLive()` is in the
same guard, so the *notification* permission decides whether the launcher route works: deny it and
the icon opens the full app for good, with nothing on screen saying why. That is not a bug to fix
here — the compact host is not an escape hatch (`phase-2.md` §2), and sending somebody to a dialog
they cannot escape from is the worse failure. It has one consequence this phase owns: **`DOD.md`'s
rule-5 item is answerable only by testers who granted notifications**, and a tester who declined is
not using the default at all. Read as approval, their silence would be the wrong reading.

**So the compact host gets the schedule as a read-only section, not as a control.** A fourth
`CompactSection` beside `Timer` and `Warmth` — mutually exclusive with them, so it costs one icon in
the row and no height beyond a single open section — showing *22:00 to 07:00*, or *Off*, with the cog
as the way to change it. **Not the pickers, and the reason is the panel rather than the size.** A
Material 3 `TimePicker` inside a `Dialog` needs an Activity's window token and `PanelWindow` has no
Activity under it; `TimeInput`'s text fields need an IME, which `PANEL_WINDOW_FLAGS`'
`FLAG_NOT_FOCUSABLE` refuses by design and for the shade's own reason. An inline dial *would* work in
both hosts — but a dial dragged at 1.59 nits, to set a time that is set once and read often, is the
wrong trade, and the forgot-I-set-it case this row exists for is a **read**.

### The screen

- **The toggle**, with a hint saying what a window is: on at one time, off at another, every night.
- **Two rows**, *Turns on* and *Turns off*, each showing the time and opening a Material 3 `TimePicker`
  in a dialog. `is24Hour = DateFormat.is24HourFormat(context)` so the picker matches the phone rather
  than the locale — those disagree, and the phone is the one the user set.
- **A derived line under the pair**: *"Overnight - the shade comes down the next morning."* when the
  window crosses midnight, nothing when it does not. It is one `contains`-adjacent fact and it is the
  single most common way a person mis-reads a pair of times.
- **The degenerate window is refused at the picker**, not stored and explained afterwards: choosing an
  off-time equal to the on-time keeps the dialog open with one line of copy. §2's function still
  answers *never* for it, because a function that is total is what makes the screen's refusal a
  convenience rather than the only thing standing between the user and a shade that never lifts.
- **A short window is warned about and not refused**, which is the opposite treatment and follows from
  the same reasoning. §4's second refusal exists because an inexact alarm can be minutes late, and the
  rate limit is roughly nine minutes — so **a short window can be missed entirely**, every night,
  while the screen shows the schedule as on. §10 currently refuses only the exactly-equal case, and
  `onAt + 5 min` sails through. It gets a line rather than a refusal because the failure it produces
  is the *visible, harmless* one this section already says it prefers: a schedule that does nothing,
  not a shade that never lifts.
  **The threshold is measured rather than invented, and A moved where it comes from.** It is not the
  nine-minute rate limit this paragraph was written about — no cell ever met that limit. It is the
  **batching window**, which is 75% of the futurity of the *last hop the chain arms* (§4). With the
  chain closing to a final hop of a few minutes, the worst arrival is a few minutes past `onAt`, so
  **a window under about ten minutes is the zone to warn about**, and R4 is what confirms the final
  hop behaves overnight the way the three forced-Doze cells behaved. Worth noting against §13: R7 asks for a five-minute test window, which sits inside
  exactly this zone and should say so rather than be debugged as a mystery.
- **The battery banner** (§7), when enabled and unexempted.
- **One line about the ROM** on phones where `hasAutostartSettings()` is true, pointing at Settings.

**Times are formatted with `DateFormat.getTimeFormat(context)`**, the same call
`rememberTimeText(instant)` already makes for *"Turns off at 23:40"* — locale-correct for free, and one
formatter for both places. A `LocalTime` gets there by being put on today's date; the helper takes an
instant and stays as it is.

**No countdown, anywhere.** Phase 2 made that decision for the auto-off line — a countdown has to tick,
which means recomposing every minute for the life of the session, and it has to plural correctly in
every locale — and a *"starts in 3 hours"* line on this screen would be the same cost for less
information than the time itself.

---

## 11. Copy

English is written here; Polish lands in the same commit, translated from English only
(`docs/translator-brief.md`), because `scripts/translation-gate.py` is a merge gate and copy is
translated once rather than against draft wording and again after review.

| Key | English |
| --- | --- |
| `schedule_title` | Schedule |
| `dim_schedule_row` | Schedule |
| `dim_schedule_window` | %1$s to %2$s |
| `dim_schedule_off` | Off |
| `schedule_enable` | Dim on a schedule |
| `schedule_enable_hint` | Gloam puts the shade up and takes it down at the times below, every night. |
| `schedule_on_at` | Turns on |
| `schedule_off_at` | Turns off |
| `schedule_overnight` | Overnight - the shade comes down the next morning. |
| `schedule_same_time` | Pick two different times. A window that starts and ends at the same moment never opens. |
| `schedule_battery_title` | Battery optimisation is on for Gloam |
| `schedule_battery_body_late` | Android may hold the schedule back by a few minutes. Turn battery optimisation off for Gloam to keep it close to the time you picked. |
| `schedule_battery_body_delegated` | Android will not let Gloam start itself at the time you picked. The shade still comes up when you next open Gloam inside the window. Turn battery optimisation off for Gloam to have it start on its own. |
| `schedule_battery_body_never` | Android will not let Gloam start itself while the schedule is on. Turn battery optimisation off for Gloam, or the shade will not come up on its own. |
| `schedule_battery_action` | Battery settings |
| `schedule_short_window` | This window is very short. Android may start Gloam a few minutes late, so a window this short can be missed. |
| `shade_notification_until` | Until %1$s |
| `schedule_rom_note` | This phone can also stop apps from starting in the background. If the shade does not start on its own, it will still come up when you open Gloam — and After a restart in Settings is where you can change that. |
| `dim_schedule_at_risk` | Schedule - may not start on its own |

**Three drafts for one string, and A has picked: `_delegated`.** Taken 2026-09-05, and R1 is what
decides it — without the exemption the alarm *fires* and `startForegroundService` is refused, so the
shade does not come up at the time the user picked, and §6's reconcile raises it the next time they
open Gloam inside the window. That is precisely what `_delegated` says and precisely what the other
two get wrong: `_late` describes a delay when the truth is a refusal, and `_never` describes a dead
feature when the reconcile still works. **`_late` and `_never` are deleted before the copy is
translated**, which is the whole reason the readings were a checkpoint before the `feat:` rather than
after it.

**One word in `_delegated` earns its place from the same reading: *Android*, not *this phone*.** The
refusal is `ForegroundServiceStartNotAllowedException`, which is the platform's, and the sentence
would be wrong on a Pixel if it blamed the vendor. What the vendor owns is autostart, which is
`schedule_rom_note`'s subject and stays a separate string — R3 measured that denying autostart stops
the alarm *running at all*, which is a different failure from this one and wants different words.

**No health claims, in the copy or anywhere near it** — *eye strain*, *sleep*, *blue light* — because
App content was answered health-No and this is the phase most likely to reach for the word "night".
`docs/store-listing.md` carries the same warning for the listing.

**"Schedule", not "timer" or "alarm".** `CONTEXT.md` reserves both: auto-off is not a timer because the
schedule sets deadlines too, and nothing here is an alarm because nothing rings and the app claims no
exact-alarm permission — which after §4 is a statement about the manifest and not just about the
words.

---

## 12. Documents this phase amends

- **`ADR-0003`** — a **third amendment**, and the first one that adds rather than withdraws. Its two
  existing amendments say Gloam schedules nothing and that WorkManager is gone; this phase makes the
  first false again in a narrower way than the ADR was written for. What comes back is exactly the
  parts that were right: *one derivation, nothing persisted per-occurrence, nothing to cancel or
  orphan*, and *the app assumes nothing about the grant*. What does **not** come back is the exact
  alarm, the `SCHEDULE_EXACT_ALARM` declaration and the sweep — Gloam has one alarm, it is inexact, and
  the fallback the ADR describes as a fallback is the only mechanism. The amendment also records
  §1's verdict, because ADR-0003 is where a future reader will look for "can this app schedule
  anything".
- **`ADR-0012`** (new) — *One deadline, monotone except at a start.* §3 is the decision and it is
  ADR-shaped by the template's own test: there was a plausible alternative (auto-off applies to every
  episode; or the schedule always wins), it is a safety-shaped invariant with a test in front of it,
  and it is exactly the kind of rule a later phase breaks by adding another writer. The alternatives
  section is the point: *last writer wins* is what the code would do by accident.
  **Its statement is not the one this file first drafted.** "Two writers, earliest wins" did not
  survive: an edit under a live shade and a window found already open both have to touch the key and
  neither is a start, so the decision is now an asymmetry — the deadline is *resolved* at two moments
  and may only be brought *forward* at every other. That is stronger, more testable, and names the
  one deliberate exception rather than pretending there is none: adoption at the on-instant moves a
  deadline outward, which is why §3 pairs it with an obligation to make it visible.
- **`PLAN.md`** — Phase 4's paragraph currently says the schedule *uses* an inexact alarm plus the
  exemption. After §1 it says whether that worked, on this ROM, with the reading that says so.
  Rule 3's test count is reconciled: the rule promised the window and earlier-deadline-wins, and both
  are delivered — Phase 3 already amended the count once and this phase adds none. **Status: Phase 4
  ticked**, or struck with §1's verdict.
- **`DOD.md`** — the rule-5 question from §3's third table row goes in beside auto-off's default
  and the launcher preference's, phrased so it outlives this phase; §7's banner wording is
  closed by E; and the standing artifact check gets a note that this is the release where "no new
  permissions" is a claim worth reading off `aab-permissions.py` rather than off the diff. **The
  launcher preference's own rule-5 item is rewritten rather than ticked**: the default ships `true`
  (§10), the first-meeting argument that set it `false` is replaced with R9, and the item gains the
  caveat that only testers who granted notifications are using the default at all.
- **`phase-3.md`** — §2's launcher paragraph is corrected in place. It argues for `false` from a
  first-run hazard the forward already prevents and from a flash a floating activity never paints;
  R9 is the reading, and the correction is a note rather than a rewrite, in the idiom Phase 3 already
  used to withdraw R8's rotation explanation.
- **`CONTEXT.md`** — **owed nothing**, checked rather than assumed. *Schedule* already reads "the
  nightly window: on at one time, off at another. One pair, not two independent switches", which is
  §2. *Deadline* already reads "the one instant at which the shade next comes down, **whoever
  set it**. There is only ever one", which is §3 written before §3 existed. *Auto-off*
  already says **hand-started**, which is the sentence §3 leans on hardest. If a word is added
  it is because the build found one, not because a phase felt owed one.
- **`CLAUDE.md`** — one line if §1 goes badly, none if it goes well. The house rules describe the
  shade, the panel and the storage rule; a schedule that works adds no rule anyone can break by
  accident that ADR-0012 does not already carry.
- **`README.md` and `docs/store-listing.md`** — the schedule is the last user-visible feature in the
  plan and the full description does not mention it. The listing copy is `DOD.md`'s item, not this
  phase's, but the release notes for the version this phase cuts are a notes-gate requirement
  (`scripts/notes-gate.py`) and belong in F.

---

## 13. Readings, and things that only look like readings

**Rule 3: device behaviour is proven by measurement, and the pure functions that compute or bound
safety values are proven by test.** This phase is unusual in having both halves be large — §14
is two tests the roadmap has been promising since it was written, and the measurements below are the
only way to know whether any of it runs.

| | Reading | Answers | How |
| --- | --- | --- | --- |
| **R1** | Inexact alarm, **no** battery exemption, autostart on, forced Doze | Is the exemption load-bearing, or was the ROM the only gate? **And does the app's own read agree with the state that was set?** | §1's matrix; `logcat -s GloamGate:*` |
| **R2** | Inexact alarm, exemption on, autostart on, forced Doze | **The gate.** Does the shipping configuration fire, how late, and does the app read the exemption correctly? | §1's matrix |
| **R3** | Inexact alarm, exemption on, **autostart off** | What a user who granted one and not the other gets, and whether the reconcile covers them | §1's matrix |
| **R4** | The overnight run — natural Doze, no `force-idle`. **Taken twice: in A against the bare apparatus, in E against the real receiver** | The lateness that is not an artefact of `force-idle`, and the threshold §10's short-window warning is written from | `scripts/doze-capture.sh` |
| **R5** | `dumpsys alarm` after each of §6's five loss paths | Is exactly one alarm armed, for the right instant, after reboot / update / force-stop / edit / fire? | `device-gate.py`, `dumpsys alarm` |
| **R6** | A scheduled shade adopted from a hand-started one | §3's second table row, on the phone: does 23:00 become 07:00 at 22:00? | Debug button + `logcat -s GloamSchedule:*` |
| **R7** | The window crossing midnight, on the phone rather than in the test | That the zone the device hands us is the zone the test assumed | Set the window five minutes out across a synthetic midnight |
| **R8** | The API-33 emulator pass (ADR-0008) | The phase, on the API level it is allowed to be worst on | `gloam-api33`, headless |
| **R9** | The launcher default at `true`, on a `pm clear`ed install | Where does a first launcher tap land, and is the bounce visible? | `am start -c LAUNCHER`, `dumpsys window`, `logcat` |
| **R10** | Shade up, deadline passed, phone asleep, then the power button | How long until it comes down — the reading that proves §5's `ACTION_SCREEN_ON` did what the loop could not | `logcat`, a stopwatch, a deadline set an hour back |
| **R11** | Force-stop, reboot, and permission-granted, each mid-window | Does §6's reconcile raise the shade, and does Stop still stay stopped for the rest of the night? | `am force-stop`, `logcat -s GloamSchedule:*` |

**Things that look like readings and are not**, listed because the temptation is real and each one
would produce a green result meaning nothing:

- **"The notification appeared."** `device-gate.py`'s own warning: a notification is downstream of an
  alarm that fired, and reading it instead of `dumpsys alarm` answers a question about the past when
  the question is what is armed.
- **"The alarm is in `dumpsys alarm`, so it will fire."** Being armed and being *run by this ROM* are
  ADR-0003's two different things, and the whole of §1 is that they are measured separately.
- **"It fired while I was using the phone."** An app the user has just interacted with is on a
  temporary allowlist. Every cell of §1's matrix goes through `force-idle` for that reason, and
  R4 exists because `force-idle` is itself a simulation.
- **A green `connectedAndroidTest`.** It proves the app *schedules* an alarm. `device-gate.py`'s
  opening paragraph is about precisely this: it cannot prove the phone will run it.

⚠️ **Before every one of R1 to R7: `python3 scripts/device-gate.py`.** The autostart grant lapses on
its own (§8), and a run against an unknown one is a wasted night rather than a weak result.

---

## 14. Tests

`PLAN.md` rule 3 promised four pure-function tests across the roadmap. The ramp was Phase 1's and the
panel's width was Phase 3's; **both of the remaining two are this phase's**, and this phase adds
nothing the rule did not know about.

- **`ScheduleTest`** (JVM, no Android) — *the window*. `ShadeRampTest`'s idiom: sweep the input space
  rather than assert a handful of examples.
  - The three shapes as a table — same-day, crossing midnight, degenerate — at the edges that matter:
    one minute before `onAt`, exactly `onAt`, inside, exactly `offAt`, one minute after.
  - **A whole day at one-minute granularity**, for both a same-day and a midnight-crossing window,
    asserting the two invariants together: `nextOn(now) > now` **always** (the property that stops
    §6's re-arm from looping), and `windowEnd(now) != null` **exactly when** `contains(now)` — an
    equivalence, which is stronger than testing either side alone and is the one a future refactor of
    either function would break.
  - **A whole year in `Europe/Warsaw`**, so both DST transitions are inside the sweep rather than being
    two special-cased dates somebody has to remember to update. On top of it, the two named rows: an
    `onAt` inside the spring gap resolves forward by an hour, and an `onAt` inside the autumn overlap
    takes the earlier offset.
  - **`UTC` and a half-hour-offset zone** as controls, so a passing suite is not a suite that only
    works where the author lives.
  - **`windowStart` in the same sweep as `windowEnd`**, with the same equivalence — non-null exactly
    when `contains(now)` — and one property the reconcile depends on: `windowStart` is *constant
    across every minute of one window*, which is what makes it usable as a night's identity. A
    version that drifted by a minute would let §6 reconcile the same night repeatedly.
  - `onAt == offAt` answers *never* on all three functions, and `enabled == false` answers identically
    to it.
- **`DeadlineTest`** (JVM, no Android) — *earlier-deadline-wins*, and it is written as an invariant
  rather than as eight examples, because §3's table is what the invariant produces and not what
  it proves.
  - **The invariant, swept**: across every `AutoOff` by both `ShadeStart` values by a handful of windows
    by every minute of a day, the answer is (a) one of the live candidates or null, and (b) **never
    later than any live candidate**. Restated: the stored deadline can only ever be earlier than a
    promise, never later — which is §3's whole safety argument, and it is the direction the
    failure is asymmetric in.
  - **The only-null row, asserted as an only**: the answer is null **exactly when** `ByHand` meets
    `AutoOff.Never` with no live window. That there is exactly one unbounded configuration is the
    useful half; a test that merely checked this one case would pass on a build where every case was
    unbounded.
  - **`BySchedule` ignores auto-off**, including — especially — `AutoOff.Never`, where a naive `min`
    over "every candidate" would give the same answer for the wrong reason.
  - **The Phase 2 regression rows**: with the schedule disabled and `ByHand`, `deadlineFor` reproduces
    `autoOffDeadline` exactly, for all five choices. This is what makes B a `refactor:` with a straight
    face.
  - **The end-side rows, beside the start-side ones**: `endShadeAt` spends the night for `ByHand` and
    `ByDeadline` inside a window, leaves the marker untouched for `Reaped`, and leaves it untouched
    for every reason outside a window. The `Reaped` row is the one worth asserting rather than
    trusting — it is the only one whose failure is a night silently skipped by the receiver that
    exists to tidy up after a reboot.
  - **Monotonicity, which is the third writer's whole safety argument**: `tightenToWindow` applied to
    any stored deadline, against any schedule, at any minute, returns a value **less than or equal to
    the one it was given** — and equal whenever `windowEnd` is null. Stated as a property rather than
    as rows, because the thing being asserted is that no input exists which pushes a deadline out.
    §3's table is what the invariant produces; this is what proves it.
- **`AutoOffTest`** keeps every row it has. `autoOffDeadline` is the same function under a new name and
  `isDue`, `deadlineOrNull` and `NO_DEADLINE` are untouched — so the rename is a rename in the test too,
  and nothing about the sentinel's coverage moves.
- **`TranslationTest`** covers this phase's strings continuously and the merge gate covers
  completeness. `dim_schedule_window` is the phase's only two-argument string and is exactly what that
  test's format-argument check exists for. Nothing needs adding.
- **No test for `ScheduleReceiver` or `armScheduleAlarm`.** A Robolectric shadow asserting that
  `AlarmManager.setAndAllowWhileIdle` was called with the instant we computed is a test of the mock; the
  thing worth knowing is whether the ROM runs it, which is §13's R2 and R5 and is not reachable
  from a test at all.
- **No instrumented test.** `ShadeWindowTest` and `PanelWindowTest` exist because a window's *effective*
  flags and its ordering exist only inside the window manager. This phase adds no window.

Everything else in this phase is device behaviour and belongs in §13.

---

## 15. The commit sequence

Conventional Commits, and each one leaves the app working. **`feat:` lines land in `CHANGELOG.md`
through release-please and `chore:` / `refactor:` / `test:` do not**, which is what decides the type
below rather than taste. **The phase ships one `feat:` and one `fix:`, which is release 0.6.0** —
close to the smallest changelog of any phase, and the largest gap between what a release note says and
what was built. The `fix:` is the odd one out: §5's screen-on receiver is the only line of this phase
that reaches a user who never opens the schedule screen.

| Checkpoint | Commits |
| --- | --- |
| **0** | `fix: take the shade down when the screen comes on, not a minute later` — §5's runtime `ACTION_SCREEN_ON` receiver in `ShadeService`, plus R10 against it. Depends on nothing else in this phase and repairs a defect that is in 0.5.0 now |
| **A** | `chore: add an alarm to the debug section` — the bare receiver, its debug manifest and the arming button §1 is read against. Then no commit at all: the verdict is a reading |
| **B** | `refactor: resolve the shade's deadline from every promise that is live` — `shade/Schedule.kt` with all four functions, `ShadeStart`, `ShadeEnd`, the five-argument `deadlineFor`, `autoOffDeadline`'s rename, `shade/Deadlines.kt`, and **both** four-call-site sweeps: `beginShadeAt` and `endShadeAt`. Plus `test: sweep the schedule window and the deadline that wins` — `ScheduleTest` and `DeadlineTest`. **No behaviour change**, and the regression rows are what say so — including Stop, in all four of its paths |
| **C** | `chore: arm an alarm for the schedule that cannot be switched on yet` — the four keys, `AppPreferences.schedule` and its three setters, `work/ScheduleAlarm.kt`, `shade/ScheduleReceiver.kt`, its manifest entry, `BootReceiver`'s extra line, and `MainApplication`'s second collector with the tighten and the reconcile hanging off it. **C is dead again**, which it briefly was not: `ShadeEnd` and `endShadeAt` moved to B, so nothing C lands touches a path a user can reach with the schedule disabled |
| **D** | `feat: dim on a nightly schedule` — the `Schedule` route and screen, the dim screen's summary row, the time pickers, the battery banner and its hand-off, the compact host's read-only schedule section, `launcherCompact`'s default, and the copy in both locales |
| **E** | The readings. Not a commit — and if R4 moves the banner's wording, a `fix:` carrying one string |
| **F** | `docs: ...` — §12's edits, ADR-0003's third amendment, ADR-0012, this file's readings block filled in, and the release notes the notes gate wants |

**The ramp precedent holds twice, as it did in Phase 3.** B lands the arithmetic and its sweep wired to
nothing, which is the cheapest possible place to get a deadline rule wrong; C lands a receiver that can
raise the shade on somebody's phone while no user can reach it; and D is the commit that puts a person
in front of both.

**If A is a no-go, the sequence does not change and C loses two files.** `work/ScheduleAlarm.kt`,
`shade/ScheduleReceiver.kt` and its manifest entry are the whole of what R2 vetoes. B, the marker, the
reconcile, `MainApplication`'s collector, D's screen and every string are identical, because none of
them is downstream of the alarm. What changes is §11's copy — the schedule promises *the next time you
open Gloam* rather than *at the time you picked* — and §1's third verdict goes into `PLAN.md` and
ADR-0003's amendment by F. That is a phase closing narrower, and rather less narrow than this document
assumed before §6 had a reconcile in it.

---

## Kotlin and Android notes for this phase

Five things here have no JavaScript analogue, and two of them are the reason the phase is expensive.

**`AlarmManager` is not `setTimeout`.** A `setTimeout` lives inside your process and dies with it. An
alarm is a row in a system service's table, keyed to a `PendingIntent`, which the OS keeps *for* you
and delivers to a process it starts if none exists. That is the whole reason it is worth its cost —
and also why §6 exists: a table the OS owns is a table the OS clears on reboot, on app update,
and when the user force-stops you, and there is no callback telling you it happened.

**A `PendingIntent` is a capability, not a callback.** It is a token that says *"whoever holds this may
send this exact intent as this app"*. `FLAG_IMMUTABLE` is what stops the holder filling in the blanks;
the `(requestCode, component)` pair is its *identity*, which is why `FLAG_UPDATE_CURRENT` replaces
rather than stacks and why §6's three call sites cannot produce three alarms. The closest JS
shape is a signed URL rather than a function reference.

**`goAsync()` is the receiver's `await`, and it is a token rather than a promise.** A
`BroadcastReceiver` is instantiated, called on the main thread, and considered disposable the moment
`onReceive` returns — so a coroutine launched and left running is racing a process kill. `goAsync()`
hands back a token that keeps the process alive for roughly ten seconds, and
`finally { pending.finish() }` is what guarantees it is spent. `BootReceiver` already carries the
pattern and §4's receiver copies it rather than inventing a second one.

**`java.time` here is `Temporal`, not `Date`.** `LocalTime` is a wall-clock time with no date and no
zone — *"22:00"*, the thing the user set. `Instant` / epoch millis is a point on the timeline with no
opinion about clocks — the thing an alarm needs. `ZoneId` is the function between them, and it is
**not injective in either direction**: one local time can be zero instants (a DST gap) or two (an
overlap), which is why §2 goes through `atZone` and takes what it resolves to rather than doing
arithmetic on minutes. JS has one `Date` doing all three jobs and a `Temporal` proposal to fix exactly
this; Kotlin has had the fixed version since API 26.

**`distinctUntilChanged` on the schedule `Flow` is not an optimisation.** Without it, every unrelated
preference write re-emits and re-arms the alarm. Re-arming is idempotent, so nothing breaks — but the
log fills with arms nobody asked for, and §13's R5 reads that log. The same reasoning is already
written on `ShadeService`'s `combine`.

---

## Readings block

*Filled in as they are taken. A dash is a reading not yet taken; a phase does not close with one left.*

- **R0** — the awake cell, taken first because it is cheap and proves the code path: **taken
  2026-09-05**, HyperOS, `…gloam.debug`, exemption on, autostart off, screen on, device `ACTIVE`.
  Armed 09:50:30 for 09:52:30; **fired 90,017 ms late**, `startForegroundService: allowed`, and the
  app's own `exempt=true` agreed with the state set from the host. Not in the matrix as planned, and
  it earned its place twice over: the first attempt read the log 31 s after the target, found
  nothing, and force-stopped — cancelling the alarm it was measuring. **The lateness is the reading;
  a window shorter than the lateness answers a different question.**
- **R1** — inexact alarm, no exemption, autostart on, forced Doze: **taken 2026-09-05**. Armed
  09:32:51 for 09:34:50, device stepped to deep `IDLE`. **The alarm fired**, 90,049 ms late — and
  then `startForegroundService` threw
  `ForegroundServiceStartNotAllowedException: … mAllowStartForeground false`. So the exemption is
  load-bearing for the **service start** and buys nothing for the **alarm**, which is the sharpest
  single answer of the three cells: without it the schedule's receiver runs, decides to raise the
  shade, and cannot. `isIgnoringBatteryOptimisations()` read `false`, agreeing with the host.
- **R2** — **the gate**: **taken 2026-09-05, and it fired.** Exemption on, autostart on, device
  stepped to deep `IDLE`, armed 09:16:53 for 09:18:52. **Fired 90,045 ms late**,
  `startForegroundService: allowed`, `exempt=true`. The shipping configuration works on this ROM
  under forced Doze. Verdict **(ii)**.
- **R3** — exemption on, autostart off: **taken 2026-09-05, and it did not fire.** Armed 08:55:28
  for 08:57:27, device stepped to deep `IDLE`, watched for **fifteen minutes** — nothing in
  `GloamGate` at all, and the alarm still listed in `dumpsys alarm` at the end. Armed and run are
  two different things, measured. This is the cell that makes section 7's Xiaomi half load-bearing.
- **R4** — the overnight run, natural Doze. In **A** against the bare apparatus: - . In **E** against
  the real receiver: -
  **Half of what R4 was going to cost has already been paid without waiting**: the window an alarm
  is given is visible in `dumpsys alarm` the instant it is armed, and a ten-hour arm reads
  `window=+1h0m0s0ms` against a two-minute arm's `+1m29s997ms`. So the batching window is 75% of the
  futurity **capped at one hour**, and since every cell above was delivered at `maxWhenElapsed`
  exactly, an hour is what a single long arm would cost. What the night still owes is the part no
  dump can answer: whether this ROM starts a **dead** process for the broadcast — `am kill` refused
  to kill the app in every cell above, so the process was alive each time and question three has not
  been asked yet.
- **R5** — one alarm armed after each of the five loss paths: -
- **R6** — the hand-started shade adopted at the on-instant: -
- **R7** — the midnight crossing, on the device: -
- **R8** — the API-33 emulator pass: **taken 2026-09-05**, `gloam-api33` headless
  (`-no-window -gpu swiftshader_indirect`), 1080x2400, and in **Polish**, so section 11's copy was
  read in `pl` rather than in the language it was written in. The phase's behaviours, on the floor
  API:
  - **Both ends of the run, twice.** A window set 4m42s out armed the on-instant itself —
    `hopFor`'s `FINAL_HOP_MS` branch, since the gap was under five minutes — fired **212,073 ms
    after it** against a `window=+3m32s52ms`, wrote the off-instant as the deadline (`scheduled shade
    up until 14:35:00`) and took the shade down **26 ms** after it. A second window, run across
    midnight, fired 134,691 ms after 23:55:00 and came down **46 ms** after a deadline of
    **2026-09-06 00:05:00**. The crossing resolves onto the next day on a device, not only in section
    14's sweep, and the zone the device handed us was the zone the test assumed.
  - ⚠️ **The far end is this ROM's habit rather than the platform's promise, and that is a
    correction to what section 1 concluded.** Its three cells each landed within 50 ms of
    `maxWhenElapsed`, and `ScheduleAlarm.kt` records that as *"the window is not a distribution to be
    optimistic about; it is the lateness"*. On stock API 33 one fire took **99.99%** of its window and
    the other **85%** — 23 seconds early. Nothing downstream moves: every hop is chosen against the
    *bound*, and section 4 already says delivering early is harmless by construction, a hop that
    simply re-arms sooner. What changes is which sentence is load-bearing — the bound is the design
    input, and the far end is one vendor's scheduler rather than something to build on.
  - **Four of section 6's five loss paths leave exactly one alarm** — fire, edit, reboot and app
    update. The reboot armed **twice, 3 ms apart** (14:42:06.707 and .710) and `dumpsys alarm` still
    listed one: `FLAG_UPDATE_CURRENT` doing exactly what section 4's fourth decision claims, which is
    the thing R5 counts rather than assumes. **Force-stop is the fifth and it cancels**: zero alarms
    afterwards, `stopped=true`, and the on-instant passed with nothing on screen and nothing in the
    log.
  - **What pays for that path is the reconcile**, and it is section 7's banner promise proven rather
    than asserted: opening the app 77 seconds into an unspent window logged `reconciled: window open
    since 14:40:00 and unspent, shade up`, and raised it. **Stop stays stopped** across an `am kill`
    and a relaunch inside the same window — and a *later* window still opens, which is the half
    worth naming: a night stopped by hand is spent, the schedule is not.
  - **The battery banner is read on resume, not observed.** Revoking the exemption from the host with
    the schedule screen open changed nothing on screen; backgrounding and returning brought the banner
    up, and granting it again cleared it the same way. That is the "live read rather than a remembered
    outcome" the phase asks for — the platform offers no broadcast for this state — and it is
    recorded here so a later reader does not file the delay as staleness.
  - **Section 3's adoption row, exercised here ahead of R6's turn on the phone.** A shade hand-started
    at 15:05:14 under a 30-minute auto-off carried a deadline of 15:35:14. The on-instant arrived at
    15:13:13.611 — 15 ms past the `maxWhenElapsed` of a 193.6 s window — and logged `scheduled
    shade up until 15:45:00`: the deadline moved **outward**, which is the one exception ADR-0012
    names, and the ongoing notification's sub-text read **"Do 15:45"** at the same moment. That is the
    visibility obligation section 3 pairs with the exception, met rather than promised. R6 is still
    owed on the phone, where the vendor's notification is the half this cannot answer.
- **R9** — the launcher default at `true`, first run: **taken 2026-09-04**, HyperOS, debug build. A
  `pm clear`ed install lands on `MainActivity`; overlay granted with notifications denied also lands
  on `MainActivity`; both granted lands on `ControlsActivity`. `ControlsActivity` is started and
  forwards back, and is given `STARTING_WINDOW_TYPE_NONE` because it is translucent and floating —
  so the bounce paints nothing and the flash shape iii was thought to cost does not exist.
- **R10** — the screen-on latency, after §5's receiver: **taken 2026-09-05, and the receiver does
  what the loop could not.** Cable out, ten-minute deadline armed at 09:42:31.9 for 09:52:31.9,
  screen off by the power button at 09:44:15, phone left alone, power button again at 10:00:12.035.

  ```
  10:00:12.035  PowerManagerService: Waking up from Dozing (reason=WAKE_REASON_POWER_BUTTON)
  10:00:12.894  ShadeService: auto-off fired 461000ms after the deadline
  ```

  **859 ms from the wake to the shade coming down**, and the same log line proves the run's own
  premise: the deadline sat **461 seconds in the past, unevaluated**, which is only possible if the
  process was not being scheduled at all. Had the phone stayed awake the loop would have fired at
  09:52:31, as the void attempt below did, 6 ms late.

  **What it would have been without the receiver, arithmetically rather than by assertion**: the
  deadline was written at 09:42:31.9, so the loop's re-check boundaries are at 09:43:31.9 and
  09:44:31.9, and the phone suspended at 09:44:15 with **16.9 s of that chunk's uptime still owed**.
  So this run would have shown the shade for about seventeen seconds after the power button, and up
  to sixty in the unluckiest case. It answers the one thing about §5 that documentation could not:
  `RECEIVER_NOT_EXPORTED` does not stop the system delivering its own protected broadcast.

  **First attempt void, same day, and the reason is worth keeping.** With the cable *in* — screen
  off, `dumpsys battery unplug`, `status 1` — the phone spent **0.0 s of 427 s suspended**,
  `Total run time` moving 427.2 s of realtime against 427.2 s of uptime. `uptimeMillis` never
  stopped, the deadline was evaluated on time, and the defect cannot appear. **A tethered phone does
  not suspend**; the same device runs 11 h realtime against 2.5 h uptime on battery. That is why the
  debug section grew a ten-minute deadline — two minutes is not long enough to unplug and let the
  SoC settle — and why every sleep-shaped reading in this phase has to be taken untethered.
- **R11** — the reconcile paths, and Stop staying stopped: -

---

## Done when

- The shade comes up on its own at a time the user picked, on a phone that has been idle since the
  morning, and the reading that says so is a real 22:00 rather than a `force-idle` simulation.
- It comes down at the other end of the same window, through the deadline loop that already existed,
  with no second alarm and no fourth reader of `off_at_millis`.
- **`off_at_millis` is never later than any deadline the user has been promised** — swept across every
  auto-off choice, both start reasons, a day of minutes and a year of a DST zone, with the one
  unbounded configuration asserted as the only one.
- The window crossing midnight is a swept property rather than an argument, and the two days a year the
  clock jumps are inside the sweep rather than beside it.
- Exactly one alarm is armed after a reboot, an app update, a force-stop, an edit and a fire — read off
  `dumpsys alarm`, not off a notification.
- The battery-optimisation exemption is asked for at the moment the schedule is switched on and never
  before it, its state is a live read rather than a remembered outcome, and the banner says the true
  one of §11's two sentences because R1 and R4 decided which it is.
- The autostart grant was re-read before every reading above, host-side, and the phase's conclusions
  say which state each was taken in.
- `aab-permissions.py` reports the **same six permissions** on the artifact as the release before it.
- The readings block above has no dashes left in it.
- **And either:** the schedule ships whole, `PLAN.md`'s Phase 4 is ticked, and ADR-0003's third
  amendment says which of its own rejected fallbacks turned out to be the mechanism -
- **or:** §1's R2 vetoed scheduled-on before anything was built on the bet, the off-half shipped
  alone with the reason in its own copy, `PLAN.md`'s Phase 4 carries the reading that narrowed it, and
  this file says so here rather than leaving it to be argued about later.
