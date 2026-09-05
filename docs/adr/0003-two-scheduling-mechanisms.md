# Two scheduling mechanisms, and only one of them is allowed to be precise

## Context

Android has two ways to make something happen later, and they are not interchangeable:

- **WorkManager** survives reboots and app updates, batches with other work, and is subject to Doze.
  It will run your job *eventually* — which for a daily nudge is exactly right, and for anything with
  a real deadline is not.
- **AlarmManager exact alarms** fire at the minute. They are lost on reboot, and since Android 14 at
  `targetSdk` 33+ the permission is **denied by default**.

Two more platform facts shape everything below:

- **`SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM` are not alternatives.** The second is auto-granted
  and cannot be revoked, which sounds like the better deal until you read Play's policy: it is
  permitted only for apps whose core function is an alarm clock or a calendar. Declaring it in
  anything else puts the whole listing at risk to save one settings screen.
- **Vendor ROMs add a second layer of policy.** On Xiaomi's HyperOS, without *autostart* granted the
  ROM does not start the process for a broadcast at all — not `BOOT_COMPLETED`, not
  `MY_PACKAGE_REPLACED`, not an explicit `am broadcast` to a manifest receiver. Observed: a 03:00
  exact alarm landing at 06:50 without autostart, and on time in deep Doze with it.

## Decision

**One daily WorkManager sweep drives everything that is not time-critical.** It asks "what is due
today?" — derived from the data every time — and re-enqueues itself for tomorrow.

**Exact alarms are reserved for the things whose lateness has consequences**, declared with
`SCHEDULE_EXACT_ALARM`, and **the app assumes nothing about the grant**: it reads
`canScheduleExactAlarms()`, says which mechanism is actually in play, and falls back to
`setAndAllowWhileIdle` — late but real.

**Nothing persists a schedule.** Due dates are derived; the OS schedule was only ever a cache of that
derivation. So there is nothing per-item to cancel, orphan, or double-enqueue.

**Notification permission is asked from the app's own screen, never from a bare system dialog on
launch.** Android permits two denials before the dialog stops appearing for good, and there is one
place in the app that spends them.

## Alternatives

**One scheduled work item per reminder.** Sounds simpler and is not: every item is something to
cancel when the reminder is edited, to re-enqueue on boot, and to reconcile after a restore. Getting
any of those wrong produces a notification that fires twice, or one that stops firing with nothing to
show for it. A sweep has none of those states.

**A periodic work request.** WorkManager's minimum period is 15 minutes with a flex window it
chooses, so "every day at 09:00" is not expressible. A one-time request that enqueues the next one is
exact to the minute and survives being run late.

**Exact alarms for everything.** Spends a restricted permission, a battery-optimisation exemption and
the user's patience on nudges that do not need any of it. If everything is urgent, nothing is.

## Consequences

- `BootReceiver` and `PackageReplacedReceiver` exist and do almost nothing, deliberately. The update
  receiver is not optional: a schema-bumping update leaves the database at the old version until
  something opens it, and every background entry point correctly declines to work over one.
- A notification channel's importance is set at creation and **can only be lowered afterwards, by the
  user**. Creating a channel quiet is a decision with no second attempt.
- Testing any of this on a vendor ROM requires reading the autostart state first. A run against an
  unknown one proves nothing either way — `scripts/device-gate.py` reads it.

Amendment, 2026-08-29: superseded in practice by ADR-0007. Gloam schedules nothing. WorkManager
remains a dependency for future use, but no worker, no alarm and no SCHEDULE_EXACT_ALARM permission
survive. The reasoning above is kept intact rather than rewritten, because it is still correct for
the app it was written about, and it is what comes back if a Gloam feature ever calls for it.

Amendment, 2026-09-02 (second). **WorkManager is gone**, and the "kept for future use" above no
longer names anything: Phase 4's schedule is designed around `setAndAllowWhileIdle` plus the
battery-optimisation exemption, and `PLAN.md`'s *Not in this plan* forecloses everything else that
would have wanted a worker. What it was costing was not nothing — the manifest merger was adding
three permissions to the built artifact that the source never declared (`WAKE_LOCK`,
`ACCESS_NETWORK_STATE` and `RECEIVE_BOOT_COMPLETED`), and `ACCESS_NETWORK_STATE` on an app that
declares no `INTERNET` and answered Play's data-safety questionnaire *nothing collected, nothing
shared* is a line a reviewer can ask about with no better answer than "a dependency we do not use".
The runtime dependency, `work-testing`, the `Configuration.Provider` on `MainApplication` and the
`androidx.startup` manifest surgery all left together. `RECEIVE_BOOT_COMPLETED` is now declared in
the source by the boot receiver that actually needs it, which is why the order mattered: it was
declared before the merge that had been supplying it was removed.

The same amendment corrects the last consequence above. **The autostart grant has no in-app read.**
It is not an appop and there is no API for it, so nothing in the app can tell a granted one from a
denied one — only `scripts/device-gate.py`, run by a developer on a host with `adb`. That is a
sharper statement than "requires reading the autostart state first": the app cannot warn about a
denial, so what it does instead is say what a denial costs and hand the user to the screen. Read
`PLAN.md` rule 4's "re-read, not re-asked, in Phase 4" as that host-side scrape rather than as
anything the app performs.

Amendment, 2026-09-05 (third), and the first one that adds rather than withdraws. **Gloam schedules
something again**, and the first amendment's "Gloam schedules nothing" is now false in a narrower way
than this ADR was written for. Phase 4 ships a nightly window: on at one time, off at another, one
pair of times and no more.

**What comes back is exactly the parts of the reasoning above that were right.** One derivation
rather than a table of occurrences, nothing persisted per-occurrence, nothing to cancel or orphan,
and an app that assumes nothing about the grant it was given. **What does not come back is the exact
alarm, the `SCHEDULE_EXACT_ALARM` declaration and the sweep.** Gloam has *one* alarm, it is inexact,
and what this ADR describes as the fallback is the whole mechanism — which is worth stating plainly,
because a reader arriving here from the manifest will find no exact-alarm permission and should not
go looking for the code that lost it.

**The gate, and its numbers** (`phase-4.md` section 1, all three cells taken on the phone under
`force-idle`, 2026-09-05). The question was whether an inexact alarm can start a foreground service
at 22:00 on a Xiaomi/HyperOS device, and the verdict was *(ii), build as planned*:

- **Exemption on, autostart on:** the alarm fired 90,045 ms late against a 90-second window and
  `startForegroundService` was **allowed**. This is the shipping configuration and it works.
- **Exemption off, autostart on:** the alarm still fired — and `startForegroundService` threw
  `ForegroundServiceStartNotAllowedException: mAllowStartForeground false`. So the
  battery-optimisation exemption licenses the **service start**, not the alarm. That is the sharpest
  of the three, because it decides the copy: the shade does not come up at the time the user picked,
  and the next process start inside the window raises it instead.
- **Exemption on, autostart off:** nothing. Fifteen minutes of silence with the alarm still listed in
  `dumpsys alarm`. **Armed and run are two different things**, which is what this ADR's consequences
  claimed and what that cell measures.

**One finding no verdict anticipated, and it shaped the code rather than the copy.** An inexact alarm
is given a window of **75% of its futurity, capped at an hour**, and this ROM delivered every cell
within 50 ms of the far end of it. A ten-hour arm therefore buys an hour of lateness in one go, which
is why `work/ScheduleAlarm.kt` arms a *chain of hops* toward the on-instant rather than arming the
on-instant itself. The bound is the design input: on a stock API-33 emulator (R8) delivery took 85%
and 99.99% of two windows, so the far end is one vendor's scheduler habit and not something to build
on, while the *width* is the platform's and is.

**And one that the platform owns rather than the vendor.** A force-stopped package is in the stopped
state, and the stopped state survives a reboot — so `BOOT_COMPLETED` is not delivered to it and the
reboot row of `phase-4.md` section 6 does not fire. Force-stop and reboot compose in the wrong
direction: what recovers the alarm after both is the next launch, which is the third recovery site
and costs no code. Measured 2026-09-05: `stopped=true` and zero alarms after the reboot, one alarm
and `stopped=false` a second after the launcher tap.
