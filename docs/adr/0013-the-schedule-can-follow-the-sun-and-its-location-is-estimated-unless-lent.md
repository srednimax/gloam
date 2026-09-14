# The schedule can follow the sun, and its location is estimated unless the user lends one

_Written 2026-09-14, before any of it is built._

## Context

The request is a schedule that comes on at sunset and goes off at sunrise. Today's schedule is a
fixed pair of clock times (`shade/Schedule.kt`). Everything about it is derived from that pair and the
clock rather than stored (ADR-0003, third amendment), and the window's end *is* the deadline
(ADR-0012).

Sunset needs two things the fixed pair did not: a date, which the clock has, and a place, which Gloam
does not. The app declares no location permission and no `INTERNET`, and its Play data-safety answer is
*nothing collected*.

**How much the place matters**, worked out with NOAA's equations for 2026:

| | Winter solstice | Summer solstice |
| --- | --- | --- |
| Warsaw sunset | 15:25 | 21:01 |
| Warsaw sunrise | 07:43 | 04:14 |
| Szczecin's sunset, if it used Warsaw's | 19 min early | **33 min early** |
| Przemyśl's sunset, if it used Warsaw's | 5 min early | 21 min late |
| Kashgar, on Beijing time, if it used Shanghai's | | **3 h 25 min early** |

**And sometimes the sun gives no answer at all.** In Tromsø (69.6° N) there is no sunset around the June
solstice and no sunrise around the December one.

Android computes twilight for itself (Night Light's *sunset to sunrise*), but inside the system server,
with no public API an app can call.

## Decision

> **A schedule is either fixed times or sunset to sunrise. Sunset to sunrise uses a location the user
> lends if there is one, and otherwise estimates it from the time zone. It falls back to the fixed times
> for any night the sun cannot answer.**

**1. One window a night, whichever kind.** The night that opens at sunset on day *D* closes at sunrise
on *D + 1*. Sun events are computed as instants from a date and a location, so daylight saving never
touches them, unlike the fixed pair's wall-clock times. The fixed pair stays stored in both kinds:
switching back restores it, and it is the fallback below.

**2. The four questions keep their shape.** `contains`, `nextOn`, `windowEnd` and `windowStart` keep
their signatures and meanings. So `work/ScheduleAlarm.kt`'s hop chain, `ScheduleReceiver`, the
reconcile and `deadlineFor` do not change.

ADR-0012 applies unchanged:

- A shade the schedule starts ignores auto-off and comes down at sunrise.
- Enabling the schedule does not touch auto-off.
- Choosing an auto-off duration during a night can only bring that night's deadline forward.

**3. Sunset means what weather apps print:** the sun's upper edge at the horizon with standard
refraction, an altitude of −0.833°. It is computed with NOAA's solar equations, which NOAA gives as
accurate to about a minute between 72° S and 72° N. There is no library and no network.

**4. When the sun gives no answer, that night is the fixed pair.** This covers two cases, and it applies
night by night, not season by season:

- The sunset that would open the night, or the sunrise that would close it, does not exist.
- No location can be had at all, for example on a phone set to `UTC`.

**5. The location, in order of preference:**

| Source | Used when | Needs |
| --- | --- | --- |
| **Lent location** | One is stored, **and** it was read in the time zone the phone is in now | The user's grant, once |
| **Estimated location** | Otherwise: the principal location of the phone's time zone, from IANA's `zone.tab`, then `zone1970.tab`, then tzdata's links for old names | Nothing |
| The fixed pair | Neither exists | Nothing |

A script (`scripts/gen_zone_locations.py`) generates the estimate table from tzdata, which is public
domain. The table is committed and regenerated when tzdata adds zones. A zone missing from it falls
back to the fixed pair; it never fails.

**`zone.tab` comes first, not `zone1970.tab`**, which was found while building the table. Since 2022,
tzdata has merged zones that agree after 1970 into links. `zone1970.tab` lists only the zone that
survived a merge, so from it alone `Europe/Oslo` would be `Europe/Berlin`, and an Oslo phone would get a
summer sunset more than an hour early. `zone.tab` still records Oslo at Oslo.

**6. The ask is `ACCESS_COARSE_LOCATION` and nothing more.** Never fine location, never background.

- **When:** from the schedule screen, at the moment the user picks *sunset to sunrise*. Never at launch
  and never from a receiver, in the spirit of ADR-0003's notification ask.
- **Every answer leaves a working schedule.** *Don't allow* means the estimate. After Android stops
  showing the dialog, which it does after two refusals, the row that offered it opens the app's system
  settings instead.
- The screen always says which source is in use.

**7. The location is read only while Gloam is in front,** using `LocationManager.getCurrentLocation`
with no Play services dependency.

- It is read at the grant, and again whenever the app is opened while the grant still stands.
- An *Only this time* grant gives exactly one read, and storing that read is what makes it enough.
- A failed read (location switched off, no position found) keeps what is stored.
- Receivers never read a location; they use what is stored.
- **A location is never replaced while a window is open.** The consequences explain why.

**8. What is stored:** the lent location rounded to 0.1°, which is about 11 km and moves sunset by less
than a minute, together with the time zone it was read in. It lives in a DataStore file of its own,
excluded from Auto Backup.

**9. No offset and no "not before".** Dark outside is the reason to dim, so the shade comes on when the
sun is gone, and 15:25 in a Warsaw December is accepted. Anyone who wants a clock time has fixed times.

## Alternatives

**Ask for location, with no estimate.** A refusal would leave the feature dead, and the first thing a
dimmer did would be to ask where you are. The estimate is good enough in most time zones that asking can
be an improvement rather than a condition.

**Estimate only, never ask.** Up to 33 minutes wrong across Poland and 3 h 25 min in western China, with
nothing the user can do about it. An app that cannot be made right is worse than one that offers to ask
once.

**Precise location, or Play services' fused provider.** Neither buys accuracy anyone would notice:
sunset moves by under a minute per 10 km. The cost would be a Play services dependency in an app that
has none, and the stricter review Play gives fine location.

**Type in a city or coordinates.** A city picker is a list of place names in every shipped language,
which is a database by `CLAUDE.md`'s own test. Coordinates are a number nobody knows.

**Civil twilight instead of sunset.** It is darker (in Warsaw, 41 minutes after sunset in December and 50 in
June), but it is not the time weather apps print. A schedule screen saying *Sunset 21:50* would
disagree with every other source on the phone.

**An offset or a "not before" limit.** Declined for now. It is cheap to add later as one more setting if
testers ask, which is why it is not built in advance.

**Enabling the schedule sets auto-off to Never.** The expectation behind it, that the schedule takes
over, is already true for scheduled nights (ADR-0012). *Never* would leave a shade started by hand
during the day with no deadline, which is the failure ADR-0012 exists to prevent.

**No window during polar day or night.** A schedule that silently does nothing for two months reads as
broken. The fixed pair is a window the user chose.

**Let Auto Backup carry the lent location.** Simpler, and it needs no rules XML. But *kept on the phone*
would become *kept on the phone and in your Google backup*. What the exclusion costs on a restored phone
is the estimate until the next read, which is at most one evening's accuracy.

## Consequences

- **A location permission appears in the manifest** for the first time. `scripts/aab-permissions.py`'s
  allow-list gains it. The listing and privacy text say what it is used for. The data-safety answer is
  re-checked against Play's rule for data processed only on the device, rather than assumed.
- **A backup rules XML returns** after the manifest's comment said there was none. It excludes one
  settings file, not a database.
- **`windowStart` has to stay constant across a night.** It is the night's identity, and the reconcile
  compares its marker against it. A new location read at 23:00 would move that night's sunset, the
  marker would stop matching, and a shade the user stopped would come back. That is why a location is
  never replaced while a window is open, and `ScheduleTest`'s sweep gains a row that asserts it.
- **Travel is covered by the time-zone check.** A lent location from Warsaw is ignored in New York, so
  New York gets its own estimate instead of Warsaw's sunset on New York's clock. Travelling within one
  zone keeps the old lent location until the next read, which is at worst the error the estimate would
  have had.
- **Windows move every day and can be very short.** Near polar summer, sunset at 00:30 and sunrise at
  01:30 is a correct one-hour night. The sweep covers a year at 52° N and at 70° N, the two daylight
  saving days, a change of zone, and the switch between sun nights and fallback nights.
- **Every surface that reports the schedule changes.** The one-line summary (`rememberScheduleSummary`)
  can no longer print a fixed *22:00 to 07:00*. That means new strings, in English and Polish.
- **CONTEXT.md** amends *Schedule* and adds *Location*.
