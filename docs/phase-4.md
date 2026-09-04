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
  second, and that is the whole of §3.**
- **A deadline loop that trusts the wall clock and not a timer.** `ShadeService.awaitDeadline` wakes
  at most every `DEADLINE_RECHECK_MS` (60 s) and re-reads `System.currentTimeMillis()`, because
  `delay` on the main dispatcher is a `Handler.postDelayed` scheduled against `uptimeMillis`, a clock
  that stops advancing in deep sleep. The scheduled *off* needs nothing from this phase: it is a
  deadline, and this loop already fires deadlines.
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
one stored deadline with a second writer and a rule for which one wins; an inexact alarm and the
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

**Six merges, and the first one is not a merge.** Each leaves the app working, ships its copy complete
in both locales — `scripts/translation-gate.py` is a merge gate, so that is enforced rather than
remembered — and is a point at which the phase could stop without stranding anything.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The gate** — will an inexact alarm start a foreground service on this ROM? | `chore:` debug apparatus, then no commit at all | phone attached, autostart read |
| **B** | The window and the deadline, as pure functions with their tests | `refactor:` + `test:` | nothing |
| **C** | Storage, the alarm, the receiver, and every place it is re-armed | `chore:` | B, and **A's verdict** |
| **D** | The schedule screen, the row that reaches it, the battery hand-off, and the launcher default | `feat:` | C |
| **E** | The readings — the overnight run, the matrix, the emulator pass | no commit | D |
| **F** | The documents | `docs:` | everything above |

**A is a gate, not a task, and it runs before a line of this phase's real code is written.** Phase 1
carried one that could veto the backlight half; Phase 3 carried one that could veto the panel; this
one can veto scheduled-*on*, which is the expensive three-quarters of the phase. It is taken against a
**bare** receiver and a bare alarm — no schedule, no preferences, no UI, no decision logic — because
the question is about the platform and the ROM and nothing else, and every line written before the
answer is a line written on a bet. §1 is the whole of it, including what happens on each of the
three verdicts.

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

**E can find things D cannot, and it is the one checkpoint that costs a night.** Auto-off's R7 read
its lateness off a two-minute deadline; scheduled-on's honest reading is a real 22:00 on a phone that
has been in Doze since 23:00 the night before, which is a wall-clock day per attempt.
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
`setAndAllowWhileIdle(RTC_WAKEUP, ...)`. Nothing reads a preference; nothing writes one. That is the
whole apparatus, and it commits to none of §2, §3, §4 or §9.

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
| **R2** | on | on | The shipping configuration. **If this does not fire, the feature does not exist** |
| **R3** | on | off | What a Xiaomi user who granted one and not the other actually gets |

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

### The three verdicts, decided now rather than argued about later

**(i) R2 fires within minutes.** Build as planned. The copy in §11 promises *within a few
minutes* rather than *at*, and R1 decides whether §7's warning is "the schedule may be late" or
"the schedule will not run".

**(ii) R2 fires and R3 does not.** Also build as planned, and the Xiaomi half of §7 becomes
load-bearing rather than a footnote: the schedule screen carries **both** hand-offs on a phone with an
autostart manager, and the copy says the ROM decides. This is the outcome ADR-0003 predicts.

**(iii) R2 does not fire.** Then scheduled-*on* is not deliverable on this ROM with the mechanism
`PLAN.md` chose, and the phase does not get to improvise. Three responses, in order:

- **Ship the off-half alone and say so.** The window's end becomes a deadline the user can set, which
  needs nothing new — §5's machinery is the existing loop. It is honest and it is barely a
  feature (see §0), so it ships *with* the reason written into the copy rather than as a silent
  half.
- **Reopen `SCHEDULE_EXACT_ALARM`, as a decision and not as a fix.** ADR-0003 rejects it on Play
  policy — a declaration form, and a dimmer that does not qualify as an alarm or clock app — and
  `PLAN.md` inherits that. Reversing it is a new ADR, a Console form and a listing risk on an app that
  has not shipped yet. **It is not a thing to reach for at the end of a long debugging session**,
  which is precisely why it is written down here, before that session.
- **Cut the phase**, record the reading that cut it in `PLAN.md`, and let the schedule be the thing
  the twelve are asked about rather than the thing that was quietly attempted.

Whichever it is, **A produces a reading and not a commit.** The `chore:` is the apparatus.

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
```

`java.time` is available unconditionally at `minSdk` 33 — it desugars from API 26 and Gloam ships to
nothing below 33 (ADR-0008) — so there is no `ThreeTenABP`, no desugaring flag and no `Calendar` in
this file.

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
- **A deadline that is silently shortened** — the user expects 07:00 and gets 23:00. Annoying, visible,
  self-correcting (the Start button is right there), and it errs toward light.

**Every rule below errs toward the second.** That is the same direction ADR-0010's two bounds err, the
same direction `BootReceiver`'s three refusals err, and the same direction the notification permission
went in Phase 2 — the app runs and says what was given up rather than withholding the feature, but
where the two failures are *asymmetric* it takes the visible one.

### The rule

> **`off_at_millis` is the earliest of the deadlines live at the moment it is written, and it is
> written at exactly two moments: the user's hand, and the schedule's on-instant.**

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
silently skipped every night they happened to be early.

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

**The gap this leaves, stated rather than hidden:** between a ROM kill and the user's next look at
Gloam's own screen, `shade_running` can say `true` with no shade and no service. That is Phase 2's gap
and this phase does not widen it, because the schedule writes the same key through the same pair.

---

## 6. Where the alarm is armed, and every way it is lost

An `AlarmManager` alarm is **not durable**. Five things destroy it and all five are ordinary:

| Lost by | Recovered at |
| --- | --- |
| Reboot | `BootReceiver`, on `BOOT_COMPLETED` — a job it does not have yet |
| App update | `BootReceiver`, on `MY_PACKAGE_REPLACED` — already in its filter |
| Force-stop (the user, or the ROM's task killer) | `MainApplication.onCreate`, next time anything launches |
| The user editing the schedule | The collector below, immediately |
| Firing | The receiver's own re-arm (§4) |

Which gives **three call sites** for five loss paths, and one of them carries two:

```kotlin
// MainApplication.onCreate, beside the launcherCompact collector that is already there.
preferences.schedule
    .distinctUntilChanged()
    .onEach { armScheduleAlarm(it) }
    .launchIn(applicationScope)
```

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
| **Battery optimisation** | **Yes** — `isIgnoringBatteryOptimizations`, no permission required | The truth, live |

So this ask gets the **notification-warning idiom** rather than the **autostart-row idiom**, and the
difference is not cosmetic. Phase 2 built both and the distinction is exactly readability: the
notification warning is a live read re-evaluated on every resume, appearing when the state is wrong and
self-clearing when the user fixes it; the autostart row is permanent, makes no claim, and remembers
nothing, because a row that claimed anything would be the app repeating the user's guess back to them
as its own assurance (ADR-0003's central hazard).

Concretely:

- A banner on the schedule screen, shown **only when the schedule is enabled and the exemption is
  missing**, with a *Battery settings* action calling `openBatteryOptimisationSettings()`.
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
before the reading. If the schedule works *without* the exemption and is merely late, the banner says
the schedule may be late. If it does not fire at all, the banner says it will not run. Writing the
second sentence when the first is true is crying wolf on a permission screen; writing the first when
the second is true is the app knowing something the user does not, which is rule 4's third clause and
the one `PLAN.md` says generates support mail. §11 carries both drafts and E picks.

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
- **The schedule screen points at the existing row rather than growing a second hand-off.** On a phone
  with an autostart manager, the schedule screen carries one line saying the ROM has the last word and
  where the switch is; the switch itself stays in Settings, where `hasAutostartSettings()` already
  gates it to phones that have one. Two hand-offs for one grant is two places to keep honest.

---

## 9. Storage

Three new keys, no migration, no database, and the whole of `PLAN.md`'s *"four values — on time, off
time, enabled, `off_at_millis`"* with the fourth already in the file.

```kotlin
val SCHEDULE_ENABLED     = booleanPreferencesKey("schedule_enabled")
val SCHEDULE_ON_MINUTES  = intPreferencesKey("schedule_on_minutes")
val SCHEDULE_OFF_MINUTES = intPreferencesKey("schedule_off_minutes")
```

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

**One `Flow` for the three keys, like `shadeIntent` and for a related reason.** A reader that could
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

**Setters: two, not three.** `setScheduleEnabled(Boolean)` and `setScheduleWindow(onAt, offAt)` —
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
| `schedule_battery_body_never` | Android will not let Gloam start itself while the schedule is on. Turn battery optimisation off for Gloam, or the shade will not come up on its own. |
| `schedule_battery_action` | Battery settings |
| `schedule_rom_note` | This phone can also stop apps from starting in the background. If the shade does not come up, check After a restart in Settings. |

**Two drafts for one string, and E picks.** `schedule_battery_body_late` and `_never` are the two
halves of §7's open question; exactly one ships and the other is deleted before the copy is
translated — which is the whole reason the reading is a checkpoint before the `feat:` rather than after
it.

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
- **`ADR-0012`** (new) — *One deadline, and the earliest promise owns it.* §3 is the decision and
  it is ADR-shaped by the template's own test: there was a plausible alternative (auto-off applies to
  every episode; or the schedule always wins), it is a safety-shaped invariant with a test in front of
  it, and it is exactly the kind of rule a later phase breaks by adding a third writer. The
  alternatives section is the point: *last writer wins* is what the code would do by accident.
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
| **R1** | Inexact alarm, **no** battery exemption, autostart on, forced Doze | Is the exemption load-bearing, or was the ROM the only gate? | §1's matrix; `logcat -s GloamGate:*` |
| **R2** | Inexact alarm, exemption on, autostart on, forced Doze | **The gate.** Does the shipping configuration fire, and how late? | §1's matrix |
| **R3** | Inexact alarm, exemption on, **autostart off** | What a user who granted one and not the other gets | §1's matrix |
| **R4** | The overnight run — a real 22:00 after a full day idle | The lateness that is not an artefact of `force-idle` | `scripts/doze-capture.sh` |
| **R5** | `dumpsys alarm` after each of §6's five loss paths | Is exactly one alarm armed, for the right instant, after reboot / update / force-stop / edit / fire? | `device-gate.py`, `dumpsys alarm` |
| **R6** | A scheduled shade adopted from a hand-started one | §3's second table row, on the phone: does 23:00 become 07:00 at 22:00? | Debug button + `logcat -s GloamSchedule:*` |
| **R7** | The window crossing midnight, on the phone rather than in the test | That the zone the device hands us is the zone the test assumed | Set the window five minutes out across a synthetic midnight |
| **R8** | The API-33 emulator pass (ADR-0008) | The phase, on the API level it is allowed to be worst on | `gloam-api33`, headless |
| **R9** | The launcher default at `true`, on a `pm clear`ed install | Where does a first launcher tap land, and is the bounce visible? | `am start -c LAUNCHER`, `dumpsys window`, `logcat` |

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
below rather than taste. **The phase ships one `feat:`, which is release 0.6.0** — the smallest
changelog of any phase and the largest gap between what a release note says and what was built.

| Checkpoint | Commits |
| --- | --- |
| **A** | `chore: add an alarm to the debug section` — the bare receiver, its debug manifest and the arming button §1 is read against. Then no commit at all: the verdict is a reading |
| **B** | `refactor: resolve the shade's deadline from every promise that is live` — `shade/Schedule.kt`, `ShadeStart`, the five-argument `deadlineFor`, `autoOffDeadline`'s rename, `shade/Deadlines.kt` and the four call sites. Plus `test: sweep the schedule window and the deadline that wins` — `ScheduleTest` and `DeadlineTest`. **No behaviour change**, and the regression rows are what say so |
| **C** | `chore: arm an alarm for the schedule that cannot be switched on yet` — the three keys, `AppPreferences.schedule` and its two setters, `work/ScheduleAlarm.kt`, `shade/ScheduleReceiver.kt`, its manifest entry, `BootReceiver`'s extra line and `MainApplication`'s second collector |
| **D** | `feat: dim on a nightly schedule` — the `Schedule` route and screen, the dim screen's summary row, the time pickers, the battery banner and its hand-off, the compact host's read-only schedule section, `launcherCompact`'s default, and the copy in both locales |
| **E** | The readings. Not a commit — and if R4 moves the banner's wording, a `fix:` carrying one string |
| **F** | `docs: ...` — §12's edits, ADR-0003's third amendment, ADR-0012, this file's readings block filled in, and the release notes the notes gate wants |

**The ramp precedent holds twice, as it did in Phase 3.** B lands the arithmetic and its sweep wired to
nothing, which is the cheapest possible place to get a deadline rule wrong; C lands a receiver that can
raise the shade on somebody's phone while no user can reach it; and D is the commit that puts a person
in front of both.

**If A is a no-go, the sequence is B, then a narrowed C and D carrying scheduled-*off* alone**, with
§1's third verdict written into `PLAN.md` and into ADR-0003's amendment by F. B does not change
at all in that world — the pure functions and both tests are the same, because `windowEnd` is what
scheduled-off is made of. That is a phase closing narrower, not a phase failing.

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

- **R1** — inexact alarm, no exemption, autostart on, forced Doze: -
- **R2** — **the gate**: -
- **R3** — exemption on, autostart off: -
- **R4** — the overnight run: -
- **R5** — one alarm armed after each of the five loss paths: -
- **R6** — the hand-started shade adopted at the on-instant: -
- **R7** — the midnight crossing, on the device: -
- **R8** — the API-33 emulator pass: -
- **R9** — the launcher default at `true`, first run: **taken 2026-09-04**, HyperOS, debug build. A
  `pm clear`ed install lands on `MainActivity`; overlay granted with notifications denied also lands
  on `MainActivity`; both granted lands on `ControlsActivity`. `ControlsActivity` is started and
  forwards back, and is given `STARTING_WINDOW_TYPE_NONE` because it is translucent and floating —
  so the bounce paints nothing and the flash shape iii was thought to cost does not exist.

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
