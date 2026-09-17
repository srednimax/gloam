# Play Console — App content answers

The answers Gloam gave in the Console's **App content** pages, what each one rests on, and **the fact
that would change it**. The Console shows what was answered but never why, and Play asks again: when
the app changes, when the questionnaire changes, and once a year. A re-declaration made from memory
a year from now is how a true answer turns false.

Written 2026-09-17 by Phase 5 B (`docs/phase-5.md` §7), which `DOD.md` had said was owed since Phase P.
The answers themselves are older — most were given on 2026-08-30, when the Console entry was created
— and each section says which is which.

Two rules keep this file honest:

- **The privacy policy and the Data safety form describe the same facts to two audiences**, and Play
  cross-checks them. [`privacy-policy.md`](privacy-policy.md) and this file move in the same commit.
  A change to one without the other is the drift this file exists to stop.
- **A claim about permissions is read off the built artifact, never off the manifest.** A dependency
  can merge a permission the source never declared, and that is invisible from `AndroidManifest.xml`.
  `scripts/aab-permissions.py` is the reader, and it runs before every upload.

## What the artifact carries

Read at **versionCode 189, versionName 0.7.1**, by `publish-play.yml`'s verify step on 2026-09-16 (run
35108345068), before the upload it guards:

> 7 permissions, all accounted for; none of the 7 forbidden ones present; 0 `<uses-feature>` declared

| Permission | Whose | What it is for | Console consequence |
| --- | --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | ours | the shade and the panel are overlay windows | none: not on Play's declaration list. It is a Settings hand-off, not a dialog |
| `POST_NOTIFICATIONS` | ours | the ongoing notification, which is the escape hatch | none |
| `FOREGROUND_SERVICE` | ours | `ShadeService` | none on its own |
| `FOREGROUND_SERVICE_SPECIAL_USE` | ours | `ShadeService`'s type | **the foreground service declaration**, below |
| `RECEIVE_BOOT_COMPLETED` | ours | `BootReceiver` puts a shade back and re-arms the schedule | none: normal, install-time |
| `ACCESS_COARSE_LOCATION` | ours | the lent location, for sunset to sunrise (ADR-0013) | **Data safety** has to be re-reasoned for it, below. No declaration form: that is for background location, which is forbidden here |
| `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX | its own non-exported receivers, signature-level | none: not user-visible |

**Forbidden, and asserted absent**: `INTERNET`, `AD_ID`, `QUERY_ALL_PACKAGES`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`.
The first two are what the Data safety and advertising-ID answers rest on, and the last two are what
keeps the location ask the one ADR-0013 describes.

**Two asks are not permissions and appear on no Play list**: the battery-optimisation exemption and
Xiaomi's autostart. Both are hand-offs to Settings. They need no declaration, and the privacy policy
names them anyway because a user sent to two Settings screens should find both explained.

---

## Ads

**No.** Answered 2026-08-30.

Would change: any ad SDK, ever. `AD_ID` absent from the artifact is the check that would catch one
arriving through a dependency.

## App access

**All functionality is available without special access.** Answered 2026-08-30.

There is no account, no sign-in and no plan for either. Would change: a feature behind a login, which
this app has no server to offer.

## Content rating

**The IARC questionnaire, every question answered No.** Answered 2026-08-30. The rating contact is
`gloam.dimmer@gmail.com`, the same address as the listing's.

Would change: user-generated content, communication between users, or a link out to anything that
has either. The Support screen's mail hand-off is not user-to-user communication: it opens the user's
own mail app, addressed to the developer. Its rate row (0.8.0) opens the app's own Play listing, whose
reviews are Play's surface rather than the app's: nothing in Gloam shows, sends or relays them.

⚠ **Which IARC category was picked is in the Console but not here.** Copy it into this section at
the next re-declaration.

## Target audience and content

**18 and over.** Answered 2026-08-30.

Would change: lowering it. Any age band under 18 brings in the Families policy and its design and
SDK requirements, and nothing about a screen dimmer is aimed at children.

## Government, financial and health features

**No to all three.** Answered 2026-08-30.

Health is the one to guard. **No health claim appears in the listing, the site or the tags** — no
*eye strain*, no *sleep*, no *blue light* — because Play's enforcement has treated a linked page as
part of the listing. The copy says so outright instead: *"Gloam is a dimmer. It makes no claim about
your eyes, your sleep or your health."* The Settings row about flicker says that some people find it
tiring and offers a trade-off; it promises no benefit, and is kept that way for this reason.

Would change: copy or a tag that sells the app as good for you.

## Data safety

**No data collected. No data shared.** First answered 2026-08-30, when the app had two settings and two
permissions. **Re-reasoned 2026-09-17 against 0.7.1**, which added the location permission, and the
answer stands.

Play's definitions, quoted from its Data safety help page (support.google.com, answer 10787469), read
2026-09-17:

> "'Collect' means transmitting data from your app off a user's device."
>
> "User data accessed by your app that is only processed locally on the user's device and not sent
> off device does **not** need to be disclosed."

The answer rests on four facts, one per route data could take:

1. **The app cannot transmit anything.** `INTERNET` is absent from the artifact, and
   `aab-permissions.py` fails the upload if it ever appears. This one fact is most of the answer.
2. **The lent location is accessed and processed only on the device.** It is read with
   `ACCESS_COARSE_LOCATION`, rounded to 0.1°, stored in its own DataStore file, and used for nothing
   but sunset and sunrise. That is exactly the case Play's second sentence exempts. It is also kept
   out of Auto Backup and device transfer by `res/xml/data_extraction_rules.xml` (ADR-0013 §8), so the
   exemption does not depend on the argument in the next point.
3. **Auto Backup is still "no data collected".** Gloam uses platform Auto Backup, so its settings — the
   dim level, the warmth, the schedule and the rest, everything except the location — can reach the
   user's own Google Drive. The platform moves the data, the user controls it, and the developer never
   sees it. That is Android transmitting the user's backup, not the app transmitting to the developer.
4. **The support mail is still "no data collected", and the reason is the definition rather than an
   exemption.** *Help and feedback* composes a `mailto:` with the app version, the Android version and
   the device's maker and model under the user's message, and hands it to **another app**. The user
   reads it, can edit every line, and sends it from their own mail client, or does not. Gloam has
   transmitted nothing; it cannot. ⚠ **Do not write "Play exempts support flows" in a re-declaration.**
   The Data safety help page has no such exemption; it exempts on-device processing, end-to-end
   encryption and ephemeral processing, and says nothing about user-initiated hand-offs to other
   apps. The claim is true because of what "collect" means, and that is the reason to give.
   What the developer does with a mail that arrives is in the privacy policy: used to reply, deleted on
   request.
   The **rate row** (0.8.0) is the same argument with less in it: it hands Play, or a browser, a URL
   holding the app's own package name and nothing about the user.

Would change any of it:

- **`INTERNET` on the artifact**, from our code or a dependency. Every answer here becomes a question.
- **Sending the location anywhere**, including into a backup. A change that dropped the exclusion rule
  or moved the location into the main settings file would put a location in the user's Drive, which
  is still arguably not *collection*, but the policy says otherwise and the two would disagree.
- **Fine or background location.** Both are forbidden on the artifact. Background location would also
  bring Play's separate declaration form and its review.
- **Anything the app sends by itself**, such as crash reporting or analytics. There is none today.

## Advertising ID

**No.** The artifact carries no `com.google.android.gms.permission.AD_ID`, and `aab-permissions.py`
forbids it.

Answered in the Console — confirmed by the owner 2026-09-17 — on a date not copied here. Date it at
the next re-declaration.

## Foreground service permissions

**`FOREGROUND_SERVICE_SPECIAL_USE`, for a user-controlled dimming overlay.** The manifest's
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` carries the text Play reads:

> Draws a user-controlled dimming overlay above other apps, which must remain on screen while the
> user is in those apps.

What the Console answer rests on, in the terms the declaration asks for:

- **What the user sees:** a dark layer over the screen, which the user starts by hand or by a
  schedule they set, plus an ongoing notification for as long as it is up.
- **Why it cannot wait or be deferred:** the overlay *is* the feature, and the apps being dimmed are
  the ones the user is reading. A service that stopped when Gloam left the screen would undim the
  page the moment the user turned to it.
- **Why none of the named types fits:** none of `mediaPlayback`, `dataSync`, `location`, `health`
  and the rest describes drawing a window over other apps. `specialUse` exists for this case.

**Answered in the Console** — confirmed by the owner 2026-09-17. ⚠ Its exact wording and date are in
the Console but not here; copy them into this section at the next re-declaration, so the text Play
holds can be compared with the manifest's.

Would change: a second foreground service, or a different type on this one.

## Store settings (not App content, recorded here because it is asked for the same reason)

- **Category: Tools.** Not Personalisation, which is launchers, wallpapers and themes.
- **Tags:** none health-flavoured, for the reason under *Health*.
- **Contact email:** `gloam.dimmer@gmail.com`, public on the listing.
- **Privacy policy:** `https://srednimax.github.io/gloam/privacy-policy.html`, served from `docs/` by
  Pages. It updates on a merge to `main`, with no release, which is why it has to be right before the
  app it describes ships rather than after.

---

## When Play asks again

1. Run `python3 scripts/aab-permissions.py` against the newest bundle, and compare its table with the
   one above. A new row is a new question.
2. Read `docs/privacy-policy.md` against the answers here. If one has moved, both move, in one commit.
3. Copy the three values this file does not hold while the Console is open: the IARC category, the
   advertising-ID date, the foreground-service wording.
4. Re-date the sections you re-answered.
