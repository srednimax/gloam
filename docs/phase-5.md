# Phase 5 — Ship shape

**The last phase, and the only one whose deliverable is mostly prose that has to be true.** Every
phase before this one ended with something a device could be asked about: does the window carry the
flag, does the alarm fire, does the shade come down. This one ends with a store listing, a privacy
policy, a site and a declaration — four documents that compile no matter what they say.

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist lives in [`DOD.md`](DOD.md); the decision
this phase reopens is
[ADR-0009](adr/0009-the-tip-is-a-peer-to-peer-link-not-an-in-app-purchase.md), and §6 reopens it for
a reason its own amendment missed. The vocabulary is in [`CONTEXT.md`](../CONTEXT.md), which is owed
nothing: *tip*, *listing* and *declaration* are words about shipping the app rather than words the
app uses about itself, and none of them names a mechanism a reader could confuse with another.

**What closing this phase means.** Today Gloam is a finished app describing itself as a half-finished
one: the listing was written for the build at the door, the site says the app stores two settings, the
privacy policy counts five and lists four, one of which was deleted in Phase 2. After this phase every
document about the app is true of the app, the Support screen has its last row, and the thing is on
Play under its own listing rather than on an internal track under yours.

**`PLAN.md`'s Phase 5 paragraph is already partly wrong, and correcting it is the first thing this
file does.** It names three items: the mark's refinement, the privacy prose's final pass, and the rest
of the Support screen. **The mark shipped at the door** (`DOD.md`, 2026-09-05) along with the icon, the
feature graphic and three real screenshots, so one third of the paragraph is spent. What replaced it is
not smaller: §4's blank-locale landmine, §7's missing declaration file and §6's link are all things the
roadmap did not know were here, and two of them were found by reading the pipeline rather than by
planning.

---

## 0. What this phase inherits, and why nothing in it is caught by a compiler

Four things are done and this phase spends them rather than repeating them:

- **The identity.** `art/mark.py` is the moon, the launcher icon and the 1024×500 feature graphic are
  generated from it, and the graphic reads its three colours out of `theme/Color.kt` at render time —
  so a palette change reaches the listing art by re-running one script. Nothing in this phase touches
  any of it.
- **The English listing, and three real screenshots.** `docs/store-listing.md` holds the only copy of
  the text; `art/play-screenshots/` holds 1452×2582 captures taken by `scripts/screenshots.py` off
  Gloam's own `[SCENES]` walk rather than off a mock-up.
- **A hosted privacy policy that answers 200**, at the URL the Console already holds, served by Pages
  from `docs/`. It ships **without a release** — a merge to `main` is live within a minute, which
  makes it the only user-facing document in this repo with no version number in front of it and no
  gate behind it.
- **A production path that has never carried a description.** `publish-play.yml` uploads no metadata
  at all; `publish-play-production.yml` renders `fastlane/metadata/android` from `store-listing.md`
  and is, in its own comment, *"the first and only place the notes for this versionCode reach Play"*.
  Everything about the listing that has ever reached Play was pasted into the Console by hand.

**The asymmetry that shapes the phase is that nothing here fails loudly.** A wrong flag on the shade
window fails on a device the same evening. A wrong sentence in the privacy policy is served to
everyone, indefinitely, and the only reader who can catch it is a reviewer deciding whether to reject
the app — or a user who compares it against the permission list and concludes the developer is lying.
The repo's gates do not reach any of it:

| Gate | Governs | Does not govern |
| --- | --- | --- |
| `scripts/translation-gate.py` | app strings, completeness per locale | `store-listing.md`, which is why its Polish half can sit empty on a green build |
| `TranslationTest` | format arguments, plural categories, orphans | whether a translated sentence is *true* |
| `scripts/notes-gate.py` | the newest `### x.y.z` heading against `versionName` | the descriptions above it |
| `scripts/aab-permissions.py` | that no forbidden permission reached the artifact | that the policy describes the five that did |

**So this phase's substitute for a compiler is §1**: a validate-only promotion that makes Play read the
whole edit before anyone can publish it, taken first, before a word of new copy is written.

**And one cost is not symmetric with the rest of the plan.** Everywhere else a mistake costs an update.
Here it costs two things an update cannot buy back: Play does not allow release notes to be edited on a
live release — `RELEASING.md` records the day that was learned — and a rejection during the closed test
costs the 14-day window rather than a day, because the window is other people's calendar.

---

## What is in, and what is deliberately not

**In:** a validate-only promotion taken as a gate; the privacy policy rewritten against the thirteen
keys the app actually stores and the five permissions it actually declares; `docs/index.md` caught up
with three shipped phases; the Polish listing written and the script bug that would publish it as two
zero-byte strings fixed in front of it; the Support screen's rate-on-Play row; `docs/play-app-content.md`,
which `DOD.md` says this phase owes; the four answers the twelve testers owe, landed or struck; a
restore read off a device rather than off an ADR; the release-shaped build walked on the phone; and the
promotion itself.

**Not in, and each with a reason rather than a phase:**

- **A tip link, anywhere this phase can reach.** §6. Not deferred out of caution — deferred because
  the listing links to the repository, which is the page the tip was going to live on.
- **A version row and a source-code row in the app.** §5. The mail hand-off already carries the
  version, and the source row is the same link as §6's.
- **A screenshot of the shade down.** §8, which closes it as a refusal with a reading behind it rather
  than leaving it open.
- **Ultra dark and the Quick Settings tile** — Phase 2b, which is unstarted and which §11 prices
  against this phase rather than ignoring.
- **1.0.** `PLAN.md` rule 2: the app ships 0.x and nothing in this plan produces a 1.0. This phase cuts
  **0.7.0** and the version is not a milestone.
- **A third language.** `PLAN.md`: adding one is an opt-in that gets its own phase file on the day it
  happens.

**The tempting one is the in-app review API**, and it is out for a reason worth writing down because it
looks like a free upgrade. `com.google.android.play:review` gives a rating dialog that never leaves the
app — nicer than a hand-off — at the cost of a Google dependency in an app whose entire declaration is
that it has no route off the device. The privacy policy's strongest sentence is *"the app does not hold
Android's `INTERNET` permission, so it is not capable of sending anything anywhere"*, and
`aab-permissions.py` asserts it on the artifact. A dependency that merged `INTERNET` would make that
sentence false on the same day this phase promises it is true. **The refusal is not that the library
would definitely do that — it is that the claim is worth more than the dialog**, and a `market://`
intent costs one function and no dependency.

---

## Checkpoints

**Seven checkpoints. One of them waits on other people and the rest do not, which is what decides their
order** — everything that can be taken before the closed test ends is in front of F. Each leaves the app
working and ships its copy complete in both locales, which `scripts/translation-gate.py` enforces rather
than asks for.

| | Checkpoint | Merges | Depends on |
| --- | --- | --- | --- |
| **A** | **The gate** — a validate-only promotion carrying the listing, to find out what the pipeline actually sends | no commit at all: the verdict is a reading | a build on the internal track |
| **B** | The truth pass — the privacy policy, `docs/index.md`, `README.md`, and `docs/play-app-content.md` | `docs:` | A |
| **C** | The blank-locale refusal in `play-metadata.py`, then the Polish listing | `fix:` then `docs:` | A |
| **D** | The Support screen's last row | `feat:` | nothing |
| **E** | The readings — the release-shaped build, the restore, the update in place, the shade-down capture, the API-33 pass | `fix:` if any of them finds something | B, C, D |
| **F** | The four answers from the twelve — landed, or struck with the reason | `feat:` / `fix:`, or nothing | the 14 days |
| **G** | The documents, and the promotion | `docs:` | everything above |

**A is a gate, not a rehearsal, and it is the fourth one in this plan.** Phase 1's could veto the
backlight half, Phase 3's could veto the panel, Phase 4's could veto scheduled-on. This one cannot veto
a feature, because there is no feature — what it vetoes is **the assumption that the metadata path
works**, which every other checkpoint here is written on top of. It costs one workflow dispatch and one
approval click, it publishes nothing, and it is taken against the listing that exists today rather than
against the one C writes. Taking it after C would answer the same question a week later and would have
let a week of copy be written on a bet.

**B and C are separate because one of them has a script bug in front of it.** B is prose about the app
and can be written and merged the moment A reports. C's Polish copy is worth nothing until
`play-metadata.py` stops emitting empty descriptions as *values* — so C is a `fix:` and then a `docs:`,
in that order, and the order is the point rather than tidiness.

**D depends on nothing and could land first.** It is deliberately not first: it is the only checkpoint
in this phase that changes the app, and putting it after A means the one thing that needs a release to
reach anybody is not sitting in `main` while a gate is still open. It is also the phase's only `feat:`,
so it is what makes this release 0.7.0 rather than a `docs:`-only version release-please would not cut.

**E is where a phase that looks like documentation gets its device time back.** Four of its readings are
things no document can answer: whether R8 broke a feature nobody would notice breaking
(`-PreleaseShapedDebug`), what a restore onto a new phone actually does with `shade_running`, whether the
launcher entry moving in Phase 4 costs a pinned icon on the first real update, and whether a screenshot
can photograph the shade at all.

**F is the only checkpoint that can be closed by silence**, and §10 says what silence is allowed to mean
for each of its four questions — decided now rather than argued about in the week the answers do or do
not arrive.

**G is the promotion, and it is a checkpoint rather than an afterthought** because it is the only moment
in this plan where the thing being shipped is read by somebody who did not build it.

---

## 1. The gate: what does the pipeline actually send to Play?

### The question, in the form that can be measured

Everything about the listing that has reached Play so far was typed into the Console. The first time
`fastlane supply` sends a *listing* rather than a binary is the first time all of these matter at once:

- **The Polish descriptions are two zero-byte files**, not absent ones. `play-metadata.py` renders them
  because `store-listing.md` has the headings with empty fences under them, and `store-listing.md`
  already warns that *"an empty short description is not a no-op to Play, it is a value"*. **That
  warning is a prediction nobody has tested.** Play may reject the edit, may accept it and blank the
  locale, or may ignore an empty string.
- **The screenshots** are 1452×2582 and there are three of them. Play's minimum count and aspect bounds
  are documented and the padding script was written against them; whether the *set* validates as a set
  has never been checked by Play.
- **The release notes** must exist for every language the app ships, in 500 characters or fewer, and
  `play-whatsnew.py` enforces both locally. Play enforces its own.
- **The icon and feature graphic** are regenerated PNGs at 512² and 1024×500.
- **And the one nobody would think to ask:** does `supply` accept a promotion at all before production
  access exists?

### The apparatus, which already exists

No code. `publish-play-production.yml` takes `dry_run` and it defaults to on, its comment saying exactly
what the gate wants: *"Play validates the whole edit and then discards it. Nothing is published, nothing
is sent for review."* Target a closed track rather than production, so the run proves the metadata path
without depending on access the app does not have yet:

```bash
gh workflow run publish-play-production.yml \
  -f from_track=internal \
  -f track=alpha \
  -f update_listing=true \
  -f rollout=1.0 \
  -f dry_run=true
```

Then approve it — the `production` environment gates the job whichever track the dialog names, which is
the protection rule `DOD.md` read back from the API rather than from the settings page.

⚠ **`update_listing=true` is the whole point of the run.** With it false the workflow adds
`--skip_upload_metadata --skip_upload_images --skip_upload_screenshots` and validates a promotion
carrying release notes and nothing else — a green run answering none of the questions above. A gate
taken with the default inputs belongs in §15's list of things that only look like readings, and it is
the easiest mistake in this phase to make.

### The three verdicts, decided now rather than argued about later

1. **It validates.** The metadata path is proven, the zero-byte Polish is something Play tolerates, and
   C's script fix drops from *blocker* to *hygiene* — still done, because tolerating an empty string
   today is not a promise about tomorrow, but done in C's own time rather than in front of it.
2. **It rejects on the Polish blanks.** The prediction in `store-listing.md` was right, C's `fix:` lands
   before any Polish copy is written, and the phase gains a reading worth more than the fix: a landmine
   found by a dry run rather than by a live listing going half-blank.
3. **It rejects on something this section did not predict** — an image dimension, a notes locale, a field
   Play wants that the renderer does not write, or the promotion itself. This is the reason the gate is
   first. Whatever it is, it is cheaper here than in G, where the same rejection arrives with a real
   release attached to it.

**No verdict cuts anything.** Unlike Phases 1, 3 and 4, this gate cannot veto a feature — it can only
change the order of B and C and the size of the fix in front of them. That is a weaker gate and it is
still worth taking first, because every other checkpoint here assumes the pipeline works.

---

## 2. The privacy policy is wrong today, and that is the phase's first fact

`docs/privacy-policy.md` is dated 30 August 2026 and describes the app as it was at the door. Two phases
of features and one deletion later, its central section reads:

> Five settings, in the app's private storage on your own device, readable only by the app:
> — how dim you set the screen — whether the shade should be on — your theme choice, and whether to
> follow the system colours — whether you have seen the app's first-run screen

**Three things are wrong with that, and they are wrong in different ways.**

- **It says five and lists four.** That was already true on the day it was written.
- **The fourth is `onboardingDone`, which Phase 2 deleted.** The policy describes a stored value that
  does not exist, which is the one kind of error that makes a reader doubt the rest of the document.
- **The app stores thirteen keys**, and the eight the policy has never mentioned are the interesting
  ones: warmth, whether to lower the backlight, the auto-off choice, the deadline, what the launcher
  icon opens, and the three that make up the schedule plus the marker recording the night it last
  honoured.

The list, from `data/AppPreferences.kt`, so the rewrite is checked against the source rather than against
memory:

| Key | What it is, in the policy's own register |
| --- | --- |
| `theme_mode`, `material_you` | your theme choice, and whether to follow the system colours |
| `dim_level`, `warmth`, `lower_backlight` | how dim you set the screen, how warm, and whether Gloam may lower the backlight |
| `shade_running`, `off_at_millis` | whether the shade should be on, and when it is due to come down |
| `auto_off_minutes` | how long a shade you start by hand lasts |
| `launcher_compact` | whether the icon opens the small controls or the whole app |
| `schedule_enabled`, `schedule_on_minutes`, `schedule_off_minutes`, `schedule_honoured_at` | the nightly window, and the night it last acted on |

**Write it as groups, not as thirteen bullets.** The policy's job is to let a reader see that nothing
here is about *them* — no identifier, no history, no content of anything they looked at — and a
thirteen-row list buries that under detail. Six lines, ending in the sentence that already does the
work: *"None of it identifies you, and none of it leaves your phone by any route the app controls."*

### The permissions section is short by three, and one of them is a manifest entry a reviewer will find

*What the app can access, and why* names two: the overlay and notifications. The artifact declares five
and `aab-permissions.py` has counted six on every release since 0.4.0, the sixth being AndroidX's own
signature-level entry. The five:

| Declared | In the policy today |
| --- | --- |
| `SYSTEM_ALERT_WINDOW` | yes — *Display over other apps* |
| `POST_NOTIFICATIONS` | yes |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | no |
| `RECEIVE_BOOT_COMPLETED` | **no**, and this is the one that matters |

`RECEIVE_BOOT_COMPLETED` is the one a curious reader finds on the Play listing's permission list and
cannot map onto anything the policy says. It is also easy to explain and reassuring once explained:
*Gloam is told when the phone finishes starting, so that a shade that was up when you switched off is
there again when you switch on.* The two foreground-service entries are a sentence rather than a
section — they are what lets the shade keep running while you use other apps, and they are the reason
the notification exists.

**Neither addition weakens the data-safety declaration**, and saying so explicitly in
`docs/play-app-content.md` (§7) is what keeps the two documents from drifting: none of these five
permissions can read anything, and the app has no route off the device to send it if they could.

### The two asks that are not permissions

The battery-optimisation exemption (Phase 4) and Xiaomi's autostart (Phase 2) are settings hand-offs
rather than permissions, so they never appear on any list Play shows. They belong in the policy anyway —
not because they collect anything, but because the policy is the document that says *what this app asks
of your phone and why*, and a user who has been sent to two Settings screens and finds neither mentioned
is reading a document that does not match their experience. One short paragraph, and it is also the
honest place to repeat what `PLAN.md` rule 4 says three of the four asks do when denied: nothing
visible, until the feature quietly does not work.

### Two sentences that are still exactly right, and must survive the rewrite

- *"The app does not hold Android's `INTERNET` permission, so it is not capable of sending anything
  anywhere, and a check runs against every release build to confirm that permission has not appeared."*
  That is `aab-permissions.py`'s `FORBIDDEN` set, and it is the strongest sentence in the document
  precisely because it names a mechanism rather than an intention.
- The Auto Backup paragraph. Its *number* changes with the list above; its reasoning does not, and §9
  measures it.

**And one that has to change for a reason unrelated to accuracy:** *Last updated: 30 August 2026*. The
date is how a reader knows the document is maintained, and it is served the moment B merges.

---

## 3. The site describes an app three phases old

`docs/index.md` is the Website field on the listing and the root of the Pages site. Its own comment block
says:

> It describes the build at the door — Phase 2 — which is the first one strangers see: the dim level,
> warmth and auto-off. The panel, the schedule and ultra dark are later phases and are deliberately not
> mentioned. **If a phase slips a feature, this file is one of the things that has to move with it.**

Three phases shipped features and none of them moved it. **That is the argument for a checkpoint rather
than a note** — the instruction was written, was correct, and was not followed, three times running, by
the person who wrote it.

What is missing, in the order the *What it does* list would carry it:

- **The controls come to where you are.** The panel above the shade, reached from the notification; the
  compact controls from the icon. This is the one that changes what the site is *for* — the door build
  was an app you opened, and this one is an app you mostly do not.
- **It dims on a schedule.** One pair of times, every night, including a window across midnight.
- **The deadline sentence needs one clause.** *Turns itself off* is written as though auto-off were the
  only thing that sets a deadline. There is one deadline and the schedule sets it too.

And one line in *Your data stays on your device* is the same error as §2's: *"it stores a dim level and
whether the shade should be on, and nothing else"*. **Fix both files in the same checkpoint from the same
list**, which is why B is one checkpoint rather than two.

**What stays off it, unchanged:** health claims of any kind — App content was answered health-No and
Play's enforcement has treated a linked page as part of the listing — and, from §6, any mention of a tip.

**`README.md` is the third file in the same pass** and needs less: it is current on features through
Phase 4, and what it carries that is about to become false is *"Nothing is on Play yet"*. §6 decides
whether its tip paragraph changes.

---

## 4. The Polish listing, and the zero-byte landmine in front of it

`store-listing.md`'s Polish section is deliberately empty and the reason is the same one behind the
translation gate: **copy is translated once, after review, rather than against a draft and again
afterwards.** The English is reviewed and shipped, so the deferral has done its job and the Polish gets
written here.

**Written from the English only**, per [`translator-brief.md`](translator-brief.md), with two things that
are not translation:

- **The app name is not translated in any locale.** `Gloam` is a coined word; `values-pl` does not
  override `app_name` and the listing must not either. A Polish listing whose name field reads `Gloam`
  is correct rather than unfinished, and `store-listing.md` already says so in both sections.
- **The character counts in the headings are the only warning before the Console refuses a paste.**
  Polish runs longer than English for the same content — the 74/80 short description has six characters
  of headroom in English and will have none in Polish, so the Polish short description is a *rewrite to
  fit* rather than a translation. That is a note for the translator rather than a surprise for the
  reviewer.

### The landmine, and what the fix actually is

`play-metadata.py` writes `pl-PL/short_description.txt` and `pl-PL/full_description.txt` as zero-byte
files rather than skipping the locale, and reports `listings: 2` either way. Harmless while a human
pastes into the Console; not harmless the first time `update_listing=true` runs — which is §1's gate, and
after it, G.

**The fix is to write nothing rather than to write nothing-as-a-value.** `fastlane supply` sends the
fields it finds files for and leaves the rest of Play's listing untouched, so an *absent* file is the
correct representation of "we have no Polish short description" and an empty one is a request to make
Play's Polish short description empty. So:

- **Omit the file when the fenced block is blank**, and print a warning naming the locale and the field,
  so a blank never disappears silently.
- **Add `--strict`, which turns that warning into a non-zero exit**, and pass it in
  `publish-play-production.yml` only on the path where `update_listing` is true. The release-notes-only
  promotion runs on every release and must not start failing because a locale's *descriptions* are
  incomplete; the listing-carrying promotion is exactly where an incomplete locale should stop the run.

That split is the whole design, and it is the same shape as every other gate here: loud where it is about
to matter, quiet where it is not.

⚠ **The fix lands before the Polish copy, not after it.** Once the Polish is written the bug is
invisible — there are no blank fields left to mis-render — and it comes back the day a third language is
added, with nobody left who remembers why.

---

## 5. The Support screen's last row, and the three that are not built

`ui/support/SupportScreen.kt` is a list of rows, each of which builds an intent from constants and hands
it to the system, with one piece of state: whether the last hand-off found an app. `PLAN.md` says the
rate-on-Play link is Phase 5's, *"where there is a listing to rate"*, and that is now true.

### The row

- **It is not a third `SupportRequest`.** That enum carries *what the mail is made of* — a subject and a
  prompt — and rating is not mail. A third entry would give it a subject line it does not have and make
  `sendSupportMail` a function with a branch in it. The rate row is a second kind of row calling a second
  function, and the `for (request in SupportRequest.entries)` loop stays exactly what it is.
- **`market://details?id=` with an `https://play.google.com/store/apps/details?id=` fallback**, in that
  order: the scheme opens the Play app directly, and the fallback is for a phone with no Play app but a
  browser. `startActivitySafely` already returns whether anything opened, so the fallback is one `||` and
  the failure message is the existing pattern with a different string.
- **The package in that URL is a literal, not `BuildConfig.APPLICATION_ID`.** Debug builds carry
  `applicationIdSuffix = ".debug"` so they install beside a Play copy, and Play has never heard of
  `io.github.srednimax.gloam.debug`. **The id Play knows is a fact about the listing rather than about
  this build**, so it is written down once as a constant beside `SUPPORT_ADDRESS`, with that sentence as
  its comment. Deriving it by stripping the suffix would work and would encode the suffix in two places.
- **No `<queries>` entry is needed and one already exists.** The manifest declares the `market` scheme
  intent, added by the template with the reasoning still attached: package visibility never blocked
  `startActivity`, only `resolveActivity`, and the entries exist *"so one added later cannot silently
  lie"*. Nothing to add.
- **A second failure string.** `support_no_mail_app` says which app is missing, which is the useful half
  of the message; the rate row's failure is a different missing app and gets its own string rather than a
  shared vague one.

### The three that are not built, and why each refusal is its own reason

- **The tip.** ADR-0009's amendment: no payment link ships inside the app in v1. §6 goes further than
  that, and it is the phase's sharpest item.
- **A source-code row.** It is the same link as §6's and inherits the same problem. It is also the row
  with the best claim to exist — *"read it, verify it"* is in the README — which is exactly why it needs
  an explicit refusal rather than an omission.
- **A version row.** The only reason to show a version number is so it can be quoted back to you, and
  `supportMailBody` already puts `versionName (versionCode)`, the Android version and the device model
  into every report before the user types a word. A row duplicating that is a row that exists to look
  complete. **The one thing that would flip it:** a user wanting to check they are on the latest build
  without opening Play — at which point the answer is the Play row, which is already here.

---

## 6. The tip, and the link nobody counted

**ADR-0009's amendment moved the tip out of the app and onto two pages: the repository and the Pages
site. Both of those are linked from the listing, and the amendment's own governing finding is that a link
to a page carrying donation information is what got StreetComplete rejected.**

The links, read off the listing rather than assumed:

| Listing field | Points at | Was going to carry the tip |
| --- | --- | --- |
| Website | `https://srednimax.github.io/gloam/` — `docs/index.md` | yes, per ADR-0009's amendment |
| Full description, last line | `github.com/srednimax/gloam` — the README | yes, per ADR-0009's amendment |
| Privacy policy URL | `docs/privacy-policy.md` | no |

The amendment's third bullet reads *"Nothing the app links to may itself carry the tip"*, and it
constrains "the Support screen's source-code link" — a link that does not exist. **What it did not notice
is that the listing links to both pages anyway**, so removing the link from the app changed nothing about
what a reviewer following the listing finds. The precedent it is built on is exactly this shape:
StreetComplete's rejection reached a *project home page* the app pointed at.

### The decision

**No tip ships in Phase 5, on any surface this phase controls**, and ADR-0009 gets a **third amendment**
recording why: the second one solved the in-app link and left the two linked pages untouched, which is
the same exposure one level out. The promise in `CLAUDE.md` and `README.md` is unchanged and uncollected
— which is what the second amendment already decided, and this only makes its own conclusion reach the
pages it named.

The alternatives, kept rather than deleted, because the choice is reversible and someone will want to
re-take it:

- **Drop the GitHub line from the full description and keep the Website field tip-free**, putting the tip
  on the repository alone. It costs the listing's *"read it, verify it"* line, which is a real part of
  how a source-available app earns trust, and buys a tip nobody is going to send in the first month of a
  closed test. **It is the option to take later**, once there is an audience — which is precisely what
  ADR-0009's amendment says: revisit with something to lose and a track record to appeal from.
- **Ship the tip on both pages and accept the exposure.** The three §3.2 conditions genuinely hold here —
  one person, 100%, nothing unlocks — and the policy text permits it. It loses on the same ground as
  before: the distinction has to survive a reviewer skimming for "external payment link", and the appeal
  path is poor.

### One line of `README.md` is inside this decision

The README says: *"If the app earns it there is a one-off tip — it unlocks nothing, it just says
thanks."* No link, no method, no account named — a statement about pricing rather than a solicitation.
**Keep it.** It is the promise `CLAUDE.md` is built on, deleting it would make the app's own framing
inconsistent for no gain, and a sentence with nothing to click is not donation information in the sense
the precedent is about. **What must not appear there is a payment link, a QR code, or a platform name.**
If that reading turns out to be too fine, the fallback is one sentence rewritten, on a page no release
depends on.

### When the tip does ship, the wording is the whole defence

Recorded here so the next person does not re-derive it: **a tip to a person, not a donation to a
project**; 100% reaching the developer, which rules out any platform taking a cut and any tiered
membership; and visibly unlocking nothing. Those are ADR-0009's three conditions, and they are what
separate this from every rejection the amendment lists.

---

## 7. `docs/play-app-content.md` — the declaration, written down where it can be re-read

`DOD.md` names this file directly: *"`docs/play-app-content.md`, where the previous app wrote these
answers down as a set, still does not exist here; Phase 5 owes it."*

**The reason to have it is that Play asks again.** App content declarations are re-confirmed when the app
changes, when Play changes the questionnaire, and annually — and the answers are given in a web form that
shows you what you said but never why. A year from now, "target audience 18+" is a decision nobody can
reconstruct, and the cost of getting it wrong on a re-declaration is a suspended listing.

What it holds, one section each: the answer, the date it was given, and **the fact that would change it**.

| Declaration | Answer | What would change it |
| --- | --- | --- |
| Ads | none | any ad SDK, ever |
| Data safety | no data collected, no data stored, nothing shared | a network permission on the artifact — `aab-permissions.py` is the check that would catch it first |
| Government / financial / health | no to all | any health-flavoured copy or tag; this is also why the listing and the site avoid *eye strain*, *sleep* and *blue light* |
| App access | all functionality available without sign-in | there is no account and no plan for one |
| Content rating | all-No questionnaire, contact `gloam.dimmer@gmail.com` | user-generated content, or a link out to any |
| Target audience | 18+ | lowering it brings Families policy and its design requirements |
| Category | Tools | Personalisation is launchers, wallpapers and themes; this is not one |

**The nuance that must be in it verbatim**, because it is the one a future re-declaration is most likely
to get wrong: Gloam uses platform Auto Backup, so a dim level can reach the user's own Google Drive,
**and that is still "no data collected"** — the platform moves it, the user controls it, the developer
never sees it. `DOD.md` records that reasoning today, and `DOD.md` is a file whose whole premise is that
closed items get deleted from it.

**And it is where §2's rewrite is cross-checked.** The privacy policy and this declaration are two
descriptions of the same facts, given to two different audiences, and the only thing keeping them
consistent is that one file names the other.

---

## 8. The screenshot of the shade down, and why a screenshot cannot take it

`DOD.md` leaves this open and calls it *"Phase 5's call, not a blocker for the door"*. It is called here,
and the call is **no** — with a reading behind it rather than an argument.

**The mechanism is two halves and a screenshot can only see one.** The dim level drives the backlight down
to `MIN_BACKLIGHT` *first* and then draws the shade for the rest (ADR-0010). The backlight is the display
panel; it exists nowhere in the framebuffer. So a `screencap` of a fully dimmed screen shows the shade's
alpha over the content and **nothing at all of the 6.64 nits the backlight half bought** — which means
whatever it shows is wrong in the direction that matters: it makes the app look like it does less than it
does, in the one image whose job is to show what it does.

The other three obstacles are real and each is smaller than that one:

- **`[SCENES]` cannot reach it.** The walk drives Gloam's own UI by tapping; the shade belongs to a
  service and sits above every app, so no tap sequence inside the app produces the picture.
- **It needs a host app underneath**, and a third party's UI in a store screenshot is somebody else's
  trademark in your listing.
- **A composed before/after is not a screenshot**, and Play's listing rules are specifically about images
  presenting something other than the actual in-app experience.

**The reading that closes it** is one command rather than a session: start the shade at a high dim level,
`adb shell screencap`, and look at the file. It settles empirically whether an app overlay is even in a
SurfaceFlinger capture, which is worth knowing regardless — and either way the item closes, because a
capture that *does* show the shade still cannot show the backlight.

**What replaces it is copy, and it already exists.** The full description's job has always been to
describe in words the thing no picture holds; `README.md` makes the same argument and is where this
reasoning was first written down. The `DOD.md` box is ticked with *"refused, R5"* rather than left open
for a future session to rediscover.

---

## 9. The restore: the one write path with none of our code in it

ADR-0005's amendment says *"the platform's default Auto Backup covers the preferences file"*. The
manifest carries `android:allowBackup="true"` and there is no `data_extraction_rules.xml`, so that is the
default: **the whole DataStore file goes to the user's Drive and comes back on a new device, with the app
not running and no line of ours involved in the write.**

**Every other path that writes preferences is one of ours.** This one is not, and it is the only way the
app can start on a phone with settings it has never seen written.

The questions, and the reason each is worth an actual reading rather than an argument:

| Restored | The question |
| --- | --- |
| `shade_running = true` | Does anything raise the shade on a phone where `SYSTEM_ALERT_WINDOW` has never been granted? `BootReceiver` refuses without the overlay permission, so the expected answer is *no, twice over* — but this is the one path reaching that refusal without the user having ever met the app |
| `off_at_millis` | It is an absolute instant from **another phone's clock**, almost certainly in the past. A passed deadline is `BootReceiver`'s first refusal, so it should be inert. If the old phone's clock ran ahead, it is a deadline in the future that the user never set |
| `schedule_enabled = true` and its times | The alarm is armed by `MainApplication`'s collector at first launch, on a phone where neither the battery exemption nor autostart was ever granted. The exemption is a **live read**, so §7's banner should be honest immediately — that is the claim to check |
| `schedule_honoured_at` | A marker naming a night on a different device. Harmless if it is in the past, which it always will be |

### How it is read

```bash
adb shell bmgr enabled
adb shell bmgr backupnow app.gloam.debug      # or the release id, if the Play copy is installed
adb shell pm clear app.gloam.debug            # the closest thing to a new device that costs a minute
adb shell bmgr restore <token> app.gloam.debug
```

⚠ **`bmgr` may have no usable transport on this ROM**, in which case the honest loop is slower and is the
only one: uninstall, reinstall from the closed track with backup enabled, and read the preferences file.
Do not substitute *"the settings look right"* for reading the file — a `pm clear` and a restore that
silently did nothing produce the same screen.

### What the reading decides

**Nothing, if it goes as expected** — and that is the useful outcome, because ADR-0005's claim is
currently a sentence nobody has read off a device. It gets a **second amendment** either way, saying what
a restore actually does.

**If the shade comes up, or a deadline arrives from another phone's clock**, the fix is a
`data_extraction_rules.xml` excluding `shade_running` and `off_at_millis` from both `cloud-backup` and
`device-transfer`, and a line in the privacy policy saying which settings travel. Those two keys are
*live state* rather than settings — everything else in the file is a preference a user would want back on
a new phone, which is the feature. **That distinction is the thing to write down whatever the reading
says**, because it is the rule the next stored key gets judged against.

---

## 10. The four answers the twelve owe, and what silence means

`DOD.md` carries four questions deliberately not answered in a room with one person in it. All four are
one-line changes or nothing, all four outlive the phases that raised them, and **this is the last phase,
so it is where they land or where they are struck.**

| Question | Lands as | Raised by |
| --- | --- | --- |
| Auto-off's default — `Hours2` ships provisional | `AutoOff.Default` | Phase 2 |
| The two edges of an episode — a shade started inside a window, and a night spent | `nightSpentBy` | Phase 4 §3, ADR-0012 |
| What the launcher icon opens — `launcherCompact` defaults `true` | `AppPreferences.launcherCompact`'s default | Phase 4 D, R9 |
| Should the phone's own brightness slider mean *"more light"* | a `ContentObserver`, already measured as buildable | Phase 3, R10 |

**Silence is an answer for three of them and not for the fourth, and that is decided here rather than in
the week the replies do or do not arrive.**

- **Auto-off's default, the episode edges and the launcher default** all ship a value today. A tester who
  lives with a default for fourteen days and does not mention it has told you something — not loudly, but
  the alternative is holding a shipped default hostage to a reply that may never come. So: **no reply, no
  change**, and `DOD.md`'s box closes with the count of testers who did not raise it rather than with a
  tick and no evidence.
- **The launcher default is the exception, and `DOD.md` already says why in a warning.**
  `ControlsActivity.forwardIfUnusable()` sends the user to the full app whenever `canDrawShade()` or
  `escapeHatchLive()` is false — so **a tester who declined notifications never sees the default at
  all**, and their silence is about a build they are not running. The question can only be closed on
  testers who granted notifications, and if there are not enough of those it closes as *unanswered* with
  the reason, which is a different outcome from *answered by silence*.
- **The brightness-slider question is not a default and cannot be answered by silence.** Nobody
  spontaneously reports the absence of a feature they were never told about. It is asked directly or not
  at all, and if it is not asked it closes as **not built**, with R10's measurement kept — the
  engineering objection is gone, so the next person to pick it up starts from *"this works, is it
  wanted"* rather than from scratch.

**A change here is a `feat:` or a `fix:` in F**, and each one carries the tester's own words in the commit
body rather than a summary of them, because the whole point of asking was to get a sentence from outside
the room.

---

## 11. What Phase 2b re-opens, named now rather than argued about later

**Phase 2b is unstarted and this phase does not wait for it.** But 2b is the only unshipped feature left
in the plan, and this phase's output is a set of documents describing the feature set — so if 2b ships
after Phase 5 closes, something in here becomes false. Naming it now costs a paragraph; finding it later
costs a listing update and a policy edit nobody remembered to make.

| 2b ships | Re-opens |
| --- | --- |
| Ultra dark | `docs/index.md`'s *What it does* and its *What it cannot do* — which is where the platform's 0.8 obscuring ceiling belongs, honestly stated; the full description in **both** locales; and the privacy key list in §2 **if** it stores a preference, which it will |
| The Quick Settings tile | `docs/index.md`'s escape-hatch sentence and the full description's; **no** new key, **no** new permission, and **no** change to `play-app-content.md` |
| Either | the screenshots, if the Dim screen grows a control — `scripts/screenshots.py` re-runs the walk rather than re-shooting by hand, so this is a command rather than a session |

**B and C are the checkpoints that re-run**, which is one reason they are separate from D, E and F.
Nothing in D (the app's last row), E (the readings) or F (the testers' answers) is touched by 2b.

⚠ **And 2b has a ceiling in front of it that `DOD.md` already priced**: an overlay that passes touches may
not obscure more than `maximum_obscuring_opacity_for_touch`, which is 0.8 by framework default on both
the phone and the API-33 emulator, and the window manager writes it straight into the shade's alpha. If
2b's honest answer turns out to be *"the platform will not let this go further"*, then **nothing above
re-opens** and this phase's documents are final on the day they merge. That is worth knowing before
writing them, and it is not this phase's decision to take.

---

## 12. Storage

**Owed nothing, and this is the first phase where that is the whole section.**

No new preference key. The rate-on-Play row holds no state; §§2 and 3 are prose about keys that already
exist; §10's four answers are *defaults* changing rather than keys appearing. `AppPreferences` is
untouched by every checkpoint except possibly F, and F changes a default's value rather than adding a key
— which is free, because `CLAUDE.md`'s rule keeps the default *in the read* and there is nothing stored
to migrate.

**The one file that might appear is `res/xml/data_extraction_rules.xml`** (§9), and it is a manifest
attribute pointing at two exclusions rather than a schema. If §9's reading calls for it, it lands in E as
a `fix:`.

**Thirteen keys, no database, nothing to migrate, ever** — which is ADR-0007 still holding at the end of
the plan it was written for.

---

## 13. Copy

Three new strings, both locales, plus the `docs/` prose the gate does not cover.

| Key | English | Note |
| --- | --- | --- |
| `support_rate` | *Rate Gloam on Play* | Names the destination, because the row leaves the app |
| `support_rate_hint` | Says the app opens Play, and that it is optional and unlocks nothing | The same register as the two mail hints, which both say *what will happen if you tap this* |
| `support_no_play` | Says there is no Play app or browser on this phone to open | Beside `support_no_mail_app` rather than replacing it — which app is missing is the useful half |

**The hint is where the app's whole framing gets one sentence.** Every feature is free and always will be;
a rating costs nothing and unlocks nothing, and the row says so rather than implying a transaction. Keep
it out of the register of a nag: this is a row on a screen the user chose to open, not a dialog that
appeared on the fifth launch.

⚠ **The listing copy is not covered by `translation-gate.py`** — it governs app strings only. The three
strings above redden a branch if Polish is missing; §4's Polish descriptions do not. That is the asymmetry
to hold in mind for the whole of C.

---

## 14. Documents this phase amends

- **`ADR-0009`** — a **third amendment**, and §6 is why. The second amendment moved the tip out of the app
  and named the repository and the Pages site as its home, without noticing that the listing's Website
  field and the full description's last line point at exactly those two pages. The amendment records the
  exposure, the decision (no tip on any surface this phase controls), and the two alternatives kept rather
  than deleted — because this is a decision someone should re-take when the app is live, which is what the
  second amendment already said and could not act on.
- **`ADR-0005`** — a **second amendment**, from §9. Its first says the platform's default Auto Backup
  covers the preferences file; this one says what a restore actually *does*, read off a device, and
  records the settings-versus-live-state rule deciding what a `data_extraction_rules.xml` would exclude if
  one is ever needed.
- **`ADR-0010`, `ADR-0012`, `ADR-0003`** — **nothing**, checked rather than assumed. This phase adds no
  mechanism, changes no invariant and schedules nothing. If §10's answers move `AutoOff.Default` or
  `nightSpentBy`, ADR-0012 gains a line naming the value that changed and the testers who changed it — the
  *rule* is untouched, which is the property the ADR was written to have.
- **`PLAN.md`** — Phase 5's paragraph is rewritten rather than ticked. The mark's refinement is struck as
  done at the door; §4's landmine, §6's link and §7's declaration file are what replace it; and the status
  list gets its last box. Rule 3's test count is reconciled and reads **four, all shipped**: the ramp, the
  panel's width, the schedule window and the deadline. This phase adds none, and §16 says why that is a
  claim rather than an omission.
- **`DOD.md`** — the biggest edit in this list, and mostly deletions. *Before the polish half* becomes the
  record of what closed; the four rule-5 boxes close with their answers or with the reason they could not
  (§10); the shade-down screenshot box closes as a refusal with R5 behind it (§8); the Polish descriptions
  box closes with C. The **standing checks stay open forever** by construction, and the artifact check
  gets this release's line: 0.7.0 adds a row to a screen and must still report the same six permissions.
- **`README.md`** — *"Nothing is on Play yet"* becomes false in G; the tip paragraph stays exactly as it
  is, per §6.
- **`docs/index.md`, `docs/privacy-policy.md`, `docs/store-listing.md`** — §§2, 3 and 4, and they are the
  phase rather than a tidy-up.
- **`docs/play-app-content.md`** — **new**, §7, and `DOD.md` has been saying so since Phase P.
- **`CONTEXT.md`** — owed nothing, checked rather than assumed. Every word this phase uses that is not
  already in it — *listing*, *tip*, *declaration*, *promotion* — is a word about publishing the app rather
  than about what the app does, and none of them is at risk of being confused with a mechanism.
- **`CLAUDE.md`** — one line if §9's reading calls for a `data_extraction_rules.xml`, because "what
  travels in a backup" would then be a rule someone could break by adding a key. None otherwise.

---

## 15. Readings, and things that only look like readings

**Rule 3: device behaviour is proven by measurement.** This phase is the inverse of Phase 4 — its pure
functions are none, and its measurements are mostly not about the device at all. Two of them are about
Play, which is a third party answering questions no local check can.

| | Reading | Answers | How |
| --- | --- | --- | --- |
| **R1** | **The gate.** A validate-only promotion with `update_listing=true` | What does the pipeline actually send, and does Play accept a zero-byte description? | §1; `gh workflow run`, then the run log |
| **R2** | The release-shaped build on the phone — `assembleDebug -PreleaseShapedDebug`, then walk every feature | Did R8 break something that does not crash? The shade, the panel, the compact host, the schedule's alarm, the receivers named in the manifest, DataStore | The phone, by hand, feature by feature |
| **R3** | An update **in place** over the previous version, on the closed track | Phase 4's F3 consequence: the launcher entry moved to `ControlsActivity`, so does a pinned home-screen icon survive? | `publish-play-closed.yml`, then the home screen |
| **R4** | A restore onto a cleared install | §9's four rows — and whether ADR-0005's claim is true | `bmgr`, then read the preferences file |
| **R5** | `screencap` with the shade down at a high dim level | Is an app overlay even in the capture — and can any image carry the backlight half? | `adb shell screencap`, then look at the file |
| **R6** | The API-33 emulator pass (ADR-0008) | The phase, on the API level it is allowed to be worst on | `gloam-api33`, headless |
| **R7** | The four artifact checks on the promoted bundle | Six permissions, no reflection surprise, both locales present, the version fields agreeing | `aab-permissions.py`, `aab-reflection.py`, `aab-locale.py`, `aab-version.py` |
| **R8** | The listing as a reviewer sees it, in **both** locales, on a phone | Does the Polish listing render, and does the store page match the app it describes? | The closed-track listing page, phone language switched |

**Things that look like readings and are not:**

- **"The dry run passed."** `--validate_only` validates the *edit* — field lengths, image dimensions,
  locales present. It says nothing about the policy review that happens after publish, which is the one
  that can reject the app. R1 is a check on the pipeline, not a prediction about the reviewer.
- **"The privacy policy is live."** Pages answering 200 is what `DOD.md` already ticked at the door. A
  hosted document that is wrong is worse than one that is missing, because it is evidence.
- **"CI is green."** Nothing in `ci.yml` reads a sentence for truth. `spotlessCheck` formats,
  `TranslationTest` counts format arguments, `notes-gate.py` compares a heading to `versionName`. A
  privacy policy claiming the app stores five settings passes every one of them.
- **"The screenshots are the right size."** 1452×2582 is a fact about the file. Three pictures of Gloam's
  own settings screens is a fact about the listing, and it is the one §8 is about.
- **"The app has no `INTERNET` permission, so nothing about the user can leave the phone."** True of the
  app and *not* the whole answer: the mail hand-off composes the app version, the Android version and the
  device model into the user's own mail client. It is their mail, visible to them, editable line by line,
  sent by an app that is not this one — which is exactly how the policy has to say it, rather than by
  omitting it.
- **"`bmgr restore` returned success."** §9's warning: a restore that silently did nothing and a restore
  that worked leave the same screen. Read the preferences file.

⚠ **Before R2, R3 and R4: `python3 scripts/device-gate.py`.** The autostart grant lapses on its own, and
R2's walk includes the schedule — a feature that does nothing on a lapsed grant, for a reason having
nothing to do with R8.

⚠ **And R2 is the one that needs `adb install -r` remembered.** The overlay appop is revoked by a
reinstall on this ROM, so a release-shaped build that comes up on its permission gate has proven nothing
about R8; re-grant with `appops set --uid` and walk it again.

---

## 16. Tests

**This phase adds none, and that is a claim rather than an omission.**

`PLAN.md` rule 3 promised four pure-function tests across the whole roadmap, standing in front of the
properties `CLAUDE.md` calls load-bearing: **`ShadeRampTest`** (Phase 1), **`PanelWidthTest`** (Phase 3b),
**`ScheduleTest`** and **`DeadlineTest`** (Phase 4). All four shipped. **This phase introduces no function
that computes or bounds a safety value**, so it introduces no fifth — and the rule's count is closed
rather than extended.

What the existing gates cover here:

- **`TranslationTest`** covers §13's three strings continuously — none has a format argument or a plural,
  so nothing in it is new — and `scripts/translation-gate.py` covers completeness at the merge. That is
  the whole of the app-string story for this phase.
- **`play-metadata.py --strict`** (§4) is the phase's one piece of new machinery, and it is a **gate
  rather than a test**: it runs in a workflow against the document, in the shape every other check in
  `scripts/` takes. A unit test asserting that a Python function skips an empty string would be a test of
  the function; the thing worth knowing is whether the *workflow* refuses to publish an incomplete locale,
  and that is proven by running it.
- **No test for the rate-on-Play row.** A Robolectric shadow asserting `startActivity` was called with a
  `market://` URI is a test of the mock. Whether the Play app opens on the right listing is R8, on a
  phone, against a listing that exists.
- **No instrumented test.** This phase adds no window, and `ShadeWindowTest` and `PanelWindowTest` exist
  because a window's *effective* flags live only inside the window manager.

Everything else here is device behaviour or Play behaviour and belongs in §15.

---

## 17. The commit sequence

Conventional Commits, and each leaves the app working. **`feat:` lines land in `CHANGELOG.md` through
release-please and `docs:` / `fix:` / `chore:` do not**, which decides the types below rather than taste.
**The phase ships one `feat:`, so it cuts 0.7.0** — the smallest `feat:` in the plan against the largest
volume of work, because everything else here is prose.

| Checkpoint | Commits |
| --- | --- |
| **A** | No commit. The verdict is a reading, taken against the listing that already exists |
| **B** | `docs: say what the app stores, what it asks for, and what it does` — the privacy policy rewritten against the thirteen keys and the five permissions, `docs/index.md` caught up with the panel and the schedule, `README.md`'s Play line, and `docs/play-app-content.md` as a new file |
| **C** | `fix: skip a locale with no description instead of publishing an empty one` — `play-metadata.py`'s omission, its warning, `--strict`, and the workflow passing it on the listing path only. Then `docs: write the Polish listing` — the short and full descriptions, and their character counts in the headings |
| **D** | `feat: add a rate-on-Play row to Help and feedback` — the row, `rateOnPlay()` beside `sendSupportMail()`, the Play package constant, and the three strings in both locales |
| **E** | The readings. Expected to be no commit, and the two that would produce one are named: a `fix:` carrying `data_extraction_rules.xml` if R4 finds a restore that raises the shade, and a `fix:` for whatever R2 finds that R8 broke |
| **F** | Whatever the twelve say — up to three one-line changes, each a `feat:` or a `fix:` carrying the tester's own words in the body, or no commit and a `docs:` line in `DOD.md` saying so |
| **G** | `docs: ...` — §14's edits, ADR-0009's third amendment, ADR-0005's second, this file's readings block filled in, `PLAN.md`'s last box, and the 0.7.0 release notes the notes gate wants. Then the release, and then the promotion |

**The order that matters is the one inside C**, and it is the ramp precedent in a different medium: the
script deciding how a blank field reaches Play lands *before* there is copy to hide the blank field, which
is the cheapest possible moment to get it wrong.

**And G's last step is not a merge.** The promotion is a workflow dispatch with an approval click,
`update_listing=true` this time and `dry_run=false`, at a rollout fraction rather than at 1.0 — which is
what `publish-play-production.yml` defaults to, and what turns a bad listing into a small number of people
rather than all of them.

---

## Kotlin and Android notes for this phase

**Three things, and none of them is Kotlin.** That is itself the note: this is the phase where the work
stops being about the language and starts being about the two systems the app is handed to.

**Auto Backup writes your files with your process dead.** Everywhere else in this app a preference changes
because something of ours wrote it — a screen, a receiver, a service. A restore is the platform opening
your private storage on a phone your code has never run on and putting a file there, before first launch,
with no callback and no hook unless you write a `BackupAgent` (ADR-0005 removed ours). The JS analogue is
not `localStorage`; it is closer to another process writing your config file between deploys. **Which is
why §9 is a reading and not a paragraph.**

**`<queries>` is about *asking*, not about *doing*.** Since Android 11 an app can only see the packages it
declares an interest in — but package visibility has never blocked `startActivity`. It blocks
`resolveActivity`, `queryIntentActivities` and friends, which are the pre-checks. So the rate row needs no
manifest change to *work*; the entry the manifest already carries exists so that a pre-check added later
cannot answer "no Play app" on a phone that has one. **The pattern here is: try the launch, catch
`ActivityNotFoundException`, and tell the user what was missing** — which is what `startActivitySafely`
already is.

**A Play edit is a transaction, not a PUT.** `fastlane supply` opens an edit, stages every change into it
— the promotion, the release notes, the descriptions, the images — and commits it once. That is why
`--validate_only` is a real gate rather than a linter: Play validates the whole staged edit and then
discards it, so §1 exercises the same code path G will, minus the commit. It is also why `RELEASING.md`
insists on *one edit, one review*: splitting a listing change from the release it belongs to turns one
review wait into two.

---

## Readings block

*(Filled in as each is taken. A dash left here at the end of the phase is a checkpoint that did not
close — see* **Done when** *.)*

- **R1** — the gate. —
- **R2** — the release-shaped build. —
- **R3** — the update in place, and the pinned icon. —
- **R4** — the restore. —
- **R5** — the shade-down capture. —
- **R6** — the API-33 pass. —
- **R7** — the artifact checks on the promoted bundle. —
- **R8** — the listing as a reviewer sees it, both locales. —

---

## Done when

- **Every sentence in `docs/privacy-policy.md` is true of the shipped app**, checked against
  `AppPreferences`' thirteen keys and the manifest's five permissions rather than against memory —
  including the two the policy has never named, and the two settings hand-offs that are not permissions at
  all.
- **`docs/index.md` describes the app that exists**, with the panel, the compact controls and the schedule
  in it, and its own comment's instruction followed for the first time.
- The Polish listing is written, and **`play-metadata.py` refuses to publish a locale whose descriptions
  are blank** rather than sending two empty strings — proven by running it, on the path where it matters
  and not on the path where it would break every release.
- Help and feedback has its last row, it opens the right listing from a build whose own `applicationId` is
  not the one Play knows, and it says which app is missing when nothing opens.
- **`docs/play-app-content.md` exists**, carries every Console answer with the fact that would change it,
  and names the Auto Backup nuance verbatim.
- **No tip link exists on any surface this phase controls**, ADR-0009's third amendment says why the second
  one did not reach far enough, and the promise in `CLAUDE.md` and `README.md` is unchanged.
- The shade-down screenshot is **closed as a refusal with R5 behind it**, not left open for a later session
  to rediscover.
- A restore is a reading rather than a claim, ADR-0005 says what it does, and either nothing travels that
  should not or `data_extraction_rules.xml` says which two keys do not.
- The four rule-5 questions are each **answered, or closed with the reason they could not be** — and the
  launcher one is not closed by silence, because the guard means some testers were never running the
  default.
- `aab-permissions.py` reports the **same six permissions** on the 0.7.0 artifact as on 0.6.0, on the
  release that adds a row which opens another app.
- The release-shaped build was walked feature by feature on the phone, and the first update **in place**
  was taken on the closed track rather than assumed from an internal install.
- The readings block above has no dashes left in it.
- **And the app is on Play under its own listing, in a staged rollout**, described by four documents that
  agree with each other and with the build — which is the whole of what "ship shape" was ever going to
  mean.
