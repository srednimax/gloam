# Definition of done — what is still open

The **live checklist**. Keep it short: when an item closes, tick it, write the *result* into whatever
long-form record you keep, and delete the detail from here. A session should be able to pick up the
work by reading this file alone.

Phases and sequence live in [`PLAN.md`](PLAN.md). This file is the worklist.

**Phase 1 is closed, and its record is [`phase-1.md`](phase-1.md)** — the entry gate, the ramp,
warmth and the readings, as five checkpoints. **A, B, C and D shipped on 2026-08-30, E and the
API-33 pass on 2026-08-31**: the dim level drives the backlight and then the shade, amber is tinted
over the top of it, `MIN_BACKLIGHT` is `0.01f` — **6.64 nits**, set by R2 — and the whole ramp is a
pure function proven by a table sweep rather than by a screen. B could have vetoed the backlight half
and did not; **ADR-0010's third amendment** says why, and withdraws the reading that had made a veto
look likely — the panel was in its own inactivity dimming, not at the user's setting.

**Two of Phase 1's readings were taken for later phases and are theirs to read**, both written up in
`phase-1.md`'s readings block:

- **2b.** The notification's *Stop* action works, but HyperOS hides it in the long-press overlay
  rather than the collapsed row — and a plain tap lands on the app's own Stop button, which at
  maximum dim sits *under the shade* at 0.33 nits. Recorded rather than worked around, because it is
  vendor behaviour (stock Android expands the top notification and shows the action) and copy naming
  a gesture would be wrong on most devices. It is what turns **2b's Quick Settings tile from a nicety
  into the one-gesture escape hatch**.
- **3b.** The brightness belongs to the **topmost window that asks for one**, measured from both
  sides: system surfaces and a video player in swipe-to-dim both failed to move ours, while the
  keyguard and any runtime permission dialog release it by hiding our window rather than by
  out-ranking it. **Read out and closed by Phase 3's checkpoint A** (2026-09-02): the rule holds
  between two windows of Gloam's own as well, so the panel declines a brightness and the shade keeps
  the override. ADR-0010's fourth amendment.

The one item below that the phase still owns — starting the tester recruitment — stays here, because
it is what the door is waiting on.

**Phase 2's code is done, and its record is [`phase-2.md`](phase-2.md)** — seven checkpoints,
**A-G, all shipped between 2026-09-01 and 2026-09-02**: the shade comes down on its own after a
deadline it re-reads from the wall clock rather than from a timer, comes back after a restart, says
what Xiaomi's autostart costs if it is denied, and has a Help and feedback screen to be complained
to. The scaffolding the door was waiting on is resolved and the emulator matrix has a real test in
it. **What is left of the phase is not code**: the tester recruitment and the listing, both below,
and both calendar rather than effort.

**Phase 3's code is done, and its record is [`phase-3.md`](phase-3.md)** — seven checkpoints,
**A–G, shipped between 2026-09-02 and 2026-09-03**. The go/no-go went first and it went: a second
overlay window of ours sits *above* the shade and the shade keeps its backlight override, which is
what made 3b buildable rather than a bet. The controls came out of the screen that owned them and now
have three hosts — the full screen, a **compact activity** reached from the notification or from the
icon when the preference says so, and the **panel**, a touchable window above the shade and the only
control surface in Gloam that is legible at maximum dim (1.59 nits under it against 6.64 above,
measured both ways). The notification now says when the user's own brightness slider is paused.
**What is left of the phase is not code**: the two rule-5 questions below, which outlive it.

**Phase 4 is planned and open, and its detail is [`phase-4.md`](phase-4.md)** — six checkpoints,
A–F, none started. **A is a gate and it runs before any of the phase's own code**: an inexact
`setAndAllowWhileIdle` alarm plus the battery-optimisation exemption, read in forced Doze against a
bare receiver under the debug seam, deciding whether this ROM will let the shade come up on its own
at all. If it says no, scheduled-*on* is not deliverable with the mechanism `PLAN.md` chose and the
phase closes narrower — §1 carries the three verdicts and what each one ships.
⚠️ **Run `python3 scripts/device-gate.py` before every reading in that phase.** The autostart
grant lapses on its own, and a Doze run against an unknown one proves nothing in either direction —
which here costs a night rather than a minute.

## The standing schema gate — parked, because there is no database

**Gloam has no database** (ADR-0007). It keeps a dim level and a shade-running flag in DataStore,
where an unrecognised key is ignored and a missing one falls back to the default declared beside it.
There is no version at which a stored file stops being readable, so nothing below applies today and
`scripts/schema-gate.py` reports *"no database, nothing to gate"* and passes.

**Everything below is kept word for word and comes back the day a feature adds a table.** That is
the point of parking it rather than deleting it: adding a table is then a deliberate act with this
checklist already attached, not a convenience someone reaches for on a Tuesday.

⚠️ **Its trigger has changed.** ADR-0001 was written against a "before 1.0 / from 1.0"
boundary, and this plan has no 1.0 — the app ships as 0.x deliberately (`PLAN.md`, rule 2), so that
boundary would never arrive. **Read every "from 1.0" below as "from the first build that reaches a
user's phone"**, which is the door at Phase 2. Recorded as a dated amendment on ADR-0001.

Whenever `APP_SCHEMA_VERSION` exists and changes, all five hold before the release goes out:

1. `MIGRATION_x_y` written **and registered in** `APP_MIGRATIONS` for every step. A migration Room
   never runs is not a migration, and nothing about the build complains.
2. The exported `app/schemas/*/N.json` committed — every later migration is transcribed from it,
   never from the entity classes.
3. `SchemaGateTest` extended, so the **launch gate** is proven to let the upgrade through. Every
   migration test opens the database directly and walks past that gate; this assertion is the only
   thing standing in front of it.
4. A migration test proving the **rows survive** — read values back, do not merely assert nothing
   threw. `runMigrationsAndValidate` compares *shapes* and is blind to a table emptied by a cascading
   `DROP`.
5. **An actual upgrade watched on a phone**: install the previous build, put real data in it, install
   the new one over the top, confirm the app opens and the data is there.
   `./gradlew assembleDebug -PreleaseShapedDebug` is how to do this without touching a Play install —
   it is minified *and* `BuildConfig.DEBUG == false`, so migrations are registered rather than the
   destructive fallback. ⚠️ **Both halves are load-bearing**: a plain debug build takes the
   fallback and proves the opposite of what you wanted.

`scripts/schema-gate.py` enforces 1–3 mechanically in CI. Items 4 and 5 are yours.

## Phase P — the pipeline. No code, do it now

Runs alongside whatever is being built. **All of it must land before the door** (Phase 2), because
`applicationId` is one of the four things the door freezes.

Developer identity verification is **already done** — the account exists and has a live app — so the
one step with an external queue is behind you.

- [x] **Create the upload keystore, outside the repo.** Done 2026-08-30, and proven rather than
      assumed: `bundleRelease` ran `signReleaseBundle`, and `keytool -printcert -jarfile` on the AAB
      returns the keystore's own SHA-256. Its path, alias, algorithm and fingerprint are recorded
      under *The upload key* in [`RELEASING.md`](RELEASING.md).
- [x] **Back the keystore up somewhere that is not this machine.** Done 2026-08-30, confirmed by
      the owner. The keystore *and* its password, which is in `local.properties` and nowhere else,
      are in a password manager. **Provenance, honestly:** this box rests on the owner's word rather
      than on a check this repo can run — nothing in the toolchain can see inside a password vault,
      which is why it is the one Phase P item that closed without machine evidence behind it.
      The shape that was used, and the reason for it: the key is 4312 bytes, so its base64 is 5752
      characters of **text** and goes in a secure note rather than a file attachment — no uploader to
      hang, and it survives a vault export, which attachments sometimes do not. Restore is
      `base64 -d`. The certificate SHA-256 is stored in the same note so a future restore proves
      itself: decode, `keytool -list`, and the fingerprint must be the one in `RELEASING.md`.
      ⚠️ **Re-verify this one occasionally**, because its failure mode is silent — some password
      managers clip long fields without warning, and a note holding 5,700 of 5,752 characters looks
      untouched right up until the day it is needed, which is also the day the app can never be
      updated again.
- [x] **Create the Play Console entry.** Done 2026-08-30. App content answered: no ads, no
      government/financial/health features, all functionality available without sign-in, content
      rating questionnaire all-No, target audience **18+**, category **Tools**. Internal testing
      track exists with the owner as its only tester.
- [x] **Bind the package name by uploading a build.** Done 2026-08-30. `io.github.srednimax.gloam`
      is now yours, and it was verified by round-trip rather than by reading the Console: the bundle
      went up to internal testing, came back down through Play onto the phone, and `dumpsys package`
      reads `versionCode=29 versionName=0.2.0 installerPackageName=com.android.vending` — the same
      artifact `aab-version.py` signed off, delivered by Play rather than sideloaded. The opt-in page
      named the package before the invite was accepted, which is where the string was checked.
      **Two things this also proved, neither of them a separate item any more:** the upload keystore
      is the one Play will expect for every future update, and the release build survives R8 and
      launches — it came up on its overlay-permission gate, not a crash.
      **And one it created:** the `applicationId` is frozen *and* married to that keystore. The
      backup item above stopped being hygiene the moment this box was ticked.
- [x] **Read back the app's access status.** Answered 2026-08-30, and the answer is the expensive
      one: **the 12-tester / 14-day closed test applies to this app.** It is not a one-time account
      unlock that the existing live app satisfied. So the plan's assumption held, the schedule stands
      as written, and **recruiting is the long pole** — see the item below.
- [x] **Answer the Console's *App content* questionnaire.** Complete 2026-08-30. Ads, government/
      financial/health, app access, content rating and target audience were answered when the entry
      was created (see above); **data safety was answered last: no data collected, no data stored,
      nothing shared.** It gates a *closed* release, not only production, which is why it sat in
      front of the door rather than in Phase 5.
      **That declaration is provable rather than promised**, and this is the reason to keep it that
      way: Gloam requests no `INTERNET` permission at all, so it has no route off the device, and
      `scripts/aab-permissions.py` asserts `INTERNET` and `AD_ID` *absent from the built artifact*
      rather than trusting the source. Any dependency that merges either one in turns a true data
      safety answer into a false one silently — the check is what catches that, so a red
      `aab-permissions.py` is a Play declaration problem before it is a build problem.
      **One nuance worth not re-litigating later:** Gloam uses platform Auto Backup, so a dim level
      can reach the user's own Google Drive. That is not developer collection — the platform moves
      it, the user controls it, and the developer never sees it — which is why the answer is still
      "no data collected". `docs/play-app-content.md`, where the previous app wrote these answers
      down as a set, still does not exist here; Phase 5 owes it.
- [x] **Set up the GitHub repository.** Done, verified 2026-08-30 by
      `python3 scripts/repo-setup.py --dry-run`, which reports the `main: require CI` ruleset, the
      rebase-only merge setting, Pages from `main/docs` and `RELEASE_PLEASE_TOKEN` all already in
      place. It is idempotent, so re-run it after any manual change in the GitHub UI rather than
      trusting this box.
- [x] **Set the five Play secrets and create the service account.** Complete 2026-08-30.
      `repo-setup.py --dry-run` reports all five set, plus `RELEASE_PLEASE_TOKEN`.
      The four upload secrets are **verified, not merely present**: the `upload.storePassword` in
      `local.properties` opens the keystore, `keytool -list` reports the single alias `upload`, and
      the certificate SHA-256 is the one `RELEASING.md` records. That check exists because GitHub
      never reads a secret back, so "set" is all the dry run can ever tell you.
      **`PLAY_SERVICE_ACCOUNT_JSON` is proven, and it took four attempts to prove it.** Nothing on
      this machine could ever have told you the JSON was valid or that Play had granted the right
      scope; **the first green run of `publish-play.yml` was the only possible proof**, and it
      arrived on 2026-08-31 - run `33412104006`, tag `v0.3.0`, **attempt 4**, job *Bundle, verify,
      upload to internal testing*, 2m53s, the AAB and its mapping on the internal track.
      **What the three red attempts were, and it was one cause rather than three:** neither of the
      failures this box braced for. The service account authenticated on the very first try and the
      bundle built, signed and verified; the Cloud project simply had
      `androidpublisher.googleapis.com` switched off - *"Google Play Android Developer API has not
      been used in project 118298064751 before or it is disabled"*. Enabling it in project
      `118298064751` was the whole fix, and the retries after it were its propagation delay.
      **The lesson is the trigger, not the API.** `workflow_dispatch` is what let four attempts cost
      four re-runs instead of four versions nobody wanted; reach for it before reaching for a tag.
      The two failures originally braced for are still the ones to expect next time: a 401 is usually
      Play-to-API propagation and is worth re-running first, and a 403 is a permission missing rather
      than a broken pipeline.
      **`UPLOAD_KEYSTORE_BASE64` is not a backup.** A GitHub secret is write-only; you cannot get the
      keystore out of it again. It is a copy CI can use, not one you could recover from. The backup
      item above is untouched by this and remains the sharpest thing in this file.
      Permissions granted, and the per-app convention behind them, are in `RELEASING.md` under
      *Creating the service account* — two boxes for the internal pipeline, and the two more that
      only the production workflow wants.

- [x] **Decide the `applicationId` deliberately.** Done: **`io.github.srednimax.gloam`**. Reverse-DNS
      on a namespace verifiably yours; Play has never checked domain ownership and package
      registration is keyed to the **signing key**, not a domain. Short generic names like
      `app.gloam` are the ones another developer could claim first. `namespace = "app.gloam"` is
      internal and never reaches Play.

## Before the door — Phase 2 freezes these

Cheap now, expensive or impossible once a build sits on twelve strangers' phones.

- [x] **Resolve three pieces of scaffolding that describe features the app does not have.** Done —
      Phase 2 checkpoints A and E, and looking for the three found two more (`phase-2.md` §7).
      `onboardingDone` is deleted, `project.py` no longer reports a `DATABASE_FILE`, and
      `work/BatteryExemption.kt` was **split rather than deleted**: the half the Settings row calls
      is now `work/Autostart.kt`, and what stays behind the old name is unambiguously Phase 4's.
      The two it found — AppCompat's below-13 locale backport and WorkManager itself — are the ticks
      below and ADR-0003's second amendment.
- [x] **Ask for `POST_NOTIFICATIONS`.** Done — Phase 1 checkpoint A. `DimScreen` fires the ask from
      the start button, **before the first `startShade()` and never after**, so it is only ever raised
      with no shade on screen: the system refuses touches on non-system overlay windows while a
      permission dialog is up, and nobody should assume `FLAG_NOT_TOUCHABLE` exempts ours.
      `setRunning(true)` and `startShade()` moved into the outcome callback together, in all three
      outcomes — the shade starts even on a refusal, and the app says what was given up instead of
      withholding the feature.
      **No DataStore key was added**, which was the point: "never asked" and "asked twice and
      refused" are indistinguishable from `shouldShowRequestPermissionRationale` and want identical
      behaviour anyway, so the launcher answers it. The second denial is spent only by a deliberate
      tap on a control that says *Allow notifications*.
      The warning is a **live read** — `notificationsAllowed() && channelCanAppear(Shade)`, both
      halves, re-read on every resume beside `canDrawShade()` — rather than a remembered outcome,
      because the fix for it is a settings screen the app hands the user off to. It self-clears when
      they fix it and appears if they revoke mid-session.
- [ ] **Recruit 12 closed testers — start in Phase 1, not when the build is ready.** Production
      access needs them opted in *continuously for 14 days*, and **this is now confirmed to apply to
      Gloam** (Phase P, 2026-08-30) rather than being an assumption. It is the longest lead item in
      the plan, it is calendar time rather than effort, and it depends on other people replying.
      **Recruit more than twelve.** The window wants twelve opted in *continuously*; one person
      uninstalling partway through is the failure this rule is shaped to produce.
- [ ] **Put auto-off's default to the testers** (`PLAN.md` rule 5). `Hours2` ships **provisional**:
      it is the longest value that is still obviously not "the next day", which is the failure
      auto-off is shaped to design out — but it is a taste argued in a room with one person in it.
      The twelve are the room. Recorded here rather than in `phase-2.md` so the question outlives
      the phase that raised it; the answer is a one-line change to `AutoOff.Default` or nothing.
- [ ] **Ultra dark has a ceiling the platform holds, and 2b has to price it before it starts.**
      *Found by Phase 3's R13, 2026-09-03.* An app overlay that **passes touches** may not obscure
      more than `maximum_obscuring_opacity_for_touch` — unset on the phone and on the API-33
      emulator, so both sit at the framework default of **0.8**, which the window manager writes
      straight into the shade's window alpha. Moving the global to `0.5` brings the shade back at
      `alpha=0.5`, so it is the platform's number and not ours. **2b is defined as going past
      `MAX_SHADE_ALPHA`**, and past a point more alpha buys nothing: the composite is multiplied by
      0.8 whatever the children do, which is why R3 and R6 measured 24% transmission where the
      invariants compute 5%. The shade cannot drop the flag to escape it — a touch-catching
      full-screen overlay is the trap the app exists not to be — so 2b's honest options are the
      backlight (already at `MIN_BACKLIGHT` there), or accepting the ceiling and saying so in the
      copy. Read `phase-3.md`'s R13 and ADR-0010's fourth amendment before designing the feature.
- [ ] **Put the launcher preference's default to the testers** (`PLAN.md` rule 5). The icon opens
      the **full app** unless the user says otherwise, and the compact controls are the setting. The
      draft had it the other way round — compact by default, with a setting that expands — and the
      inversion is an argument rather than a reading: a first launcher tap that lands on a dialog
      over another app is a bad first meeting with an app nobody has used yet, and the compact host
      already has two doors that do not involve the icon. The twelve are the room, because the
      argument is about the *second week* of use rather than the first. The answer is a one-line
      change to `AppPreferences.launcherCompact`'s default or nothing.
- [ ] **Put the brightness-slider question to the testers** (`PLAN.md` rule 5, the second of its two).
      Should the phone's own brightness slider mean *"give me more light"* while Gloam is dimming?
      Phase 3 ships the cheap half — the ongoing notification now says the slider is paused, on the one
      surface that can be read at 6.64 nits — and deliberately ships no `ContentObserver`.
      **The engineering objection is gone and the taste one is not.** `phase-3.md`'s R10 measured that
      the backlight override switches the framework's auto-brightness controller off entirely, so an
      observer listening while the shade is up cannot mistake an adaptive write for a user's drag, and
      the manual-mode-only limitation does not apply. What is left is that a *system* control would do
      something the system never promised, on a screen that does not name the app responsible. That is
      a taste argued in a room with one person in it, which is what the twelve are for. The answer is a
      small, already-measured piece of work or nothing.
- [ ] **Replace the placeholder mark.** `art/mark.py`, then `python3 art/make-launcher-icon.py` and
      `make-feature-graphic.py`. Record the provenance in `art/README.md` — where the art came from is
      the thing most likely to block a first upload, and it is discovered late.
- [ ] **Write the listing, and shoot the feature graphic and screenshots.** *Moved here from Phase 5
      on 2026-08-30.* Play will not let a **closed** test open without a complete store listing, and
      the closed test is what the 12-tester window runs on — so these three cannot be polish. The
      graphic comes off the item above (`make-feature-graphic.py` derives from `art/mark.py`), which
      is now a dependency of the door rather than a Phase 5 nicety. `docs/store-listing.md` — every
      heading in it is parsed by a script. Keep health claims out of the copy *and* the tags — "eye
      strain", "sleep", "blue light" — because App content was answered health-No.
      **Part-done 2026-08-30, and the remainder is named rather than implied:**
      - [x] English short (74/80) and full (2571/4000) descriptions.
      - [x] App name, both locales — `Gloam`, untranslated on purpose, and the file says why.
      - [x] Two phone screenshots, 1452×2582. **Placeholders taken by hand**, because
            `scripts/screenshots.py` still walks the template app's `[SCENES]`. They unblock an
            upload; they do not sell anything.
      - [x] Icon (512²) and feature graphic (1024×500) exist at the right sizes — but both derive
            from the **placeholder mark**, so the item above still owns them.
      - [ ] **Polish short and full descriptions.** Deliberately deferred, English first. Note the
            trap recorded in `store-listing.md`: `play-metadata.py` emits *zero-byte* pl-PL files
            rather than skipping the locale, which is harmless by hand and not harmless once the
            publish workflow runs.
      - [ ] Real screenshots off a real mark, and the `[SCENES]` rewrite behind them.
- [x] **The three emulator CI legs are vacuously green.** Closed — Phase 2 checkpoint F, both ways
      at once. The API 26 leg is cut (it could never have installed a `minSdk` 33 app), leaving
      33 + 36 on `aosp_atd`; and `app/src/androidTest` has its first test. `ShadeWindowTest` starts
      the real service and reads the real window out of `dumpsys window windows`, which is the only
      place the *effective* flags exist — a JVM test can only assert the constant we pass in.
      **Proven non-vacuous by mutation**: dropping `FLAG_NOT_TOUCHABLE` turns it red with the
      message it was written to print. Both legs green on PR #25, and green on the phone too
      (R10) once HyperOS let the instrumentation package install at all.
- [x] **Create the API-33 AVD** and run the end-of-phase pass on it (ADR-0008). Done 2026-08-31:
      `gloam-api33`, `system-images;android-33;google_apis;x86_64` on a Pixel 6 profile, created and
      booted headless entirely from the CLI — no Android Studio. R10 passed on it: the app launches,
      the shade window appears with every safety flag intact, and the permission flow works.
      **Two things it settled that had been guesses.** The emulator *does* honour the window
      brightness override, which `phase-1.md` §7 expected it not to — and it has no nits calibration
      at all (`mBacklight=null, mNits=null`, identity spline), so the float it applies means nothing
      photometric and **the phone stays the only place light is measured**. The readings block
      carries the numbers. Recreate with
      `emulator -avd gloam-api33 -no-window -gpu swiftshader_indirect`.
- [x] **Re-read ADR-0004 now `minSdk` is 33.** Done — Phase 2 checkpoint E, with the dated
      amendment on ADR-0004 rather than as a tidy-up. The `AppLocalesMetadataHolderService` entry is
      gone; AppCompat stays, because ADR-0006 needs `setDefaultNightMode` and the switcher is still
      `setApplicationLocales`. R12 read the phone afterwards: the framework holds `[pl]` across a
      force-stop, so nothing was propping the switcher up.
- [x] **Replace the placeholder domain.** Done: the `Item` domain and the database are gone
      entirely (ADR-0007), and `ui/dim/` is the app's one screen over `AppPreferences`.
- [x] **Choose the palette.** Done: dusk amber, warm taupe, twilight violet and warm grey, seeded in
      `scripts/gen_scheme.py`. All 22 contrast checks pass in both schemes. Re-read the stderr report
      if you touch a seed — and remember the icon's ground is a *second* copy of `mark.PRIMARY`,
      which `make-launcher-icon.py` now checks rather than trusting.

## Before the polish half — Phase 5

The listing, the feature graphic and the screenshots **used to be here** and moved to *Before the
door* on 2026-08-30: a closed test will not open without them, so they are schedule, not polish.

- [x] **Write the privacy policy** and confirm GitHub Pages is serving `docs/`. Done 2026-08-30:
      `docs/privacy-policy.md` is written, and since PR #6 merged
      <https://srednimax.github.io/gloam/privacy-policy.html> answers 200 — the URL the Console's
      *Set privacy policy* field wants. Play requires a *hosted* URL, and an offline app has no
      server of its own, so this one waits on a merge to `main` rather than on a commit.
      `docs/index.md` was the template's `<App name>` placeholder while Pages served it publicly;
      it was written in PR #7, so the site root is Gloam's own and the listing's Website field has
      somewhere real to point.

- [x] **Add yourself as the sole required reviewer on the `production` environment.** Done
      2026-08-30, and read back from the API rather than from the settings page: the environment
      exists, its only protection rule is `required_reviewers`, and the reviewer is `srednimax`.
      It had to be created — the repo carried only `github-pages`, and
      `publish-play-production.yml` declares `environment: production`, which GitHub would have
      created implicitly on first run **with no protection rules at all**. The workflow's promise
      that "the environment holds it until you approve" would have been false the one time it
      mattered.
      **`prevent_self_review` is `false`, and must stay that way.** You are both the only account
      that can dispatch that workflow and its only reviewer, so turning it on would deadlock the
      production path outright — nobody left who is allowed to approve. It reads as a security
      tightening and is, here, a lockout.

## Standing checks that never close

- [ ] **Every release: run the artifact checks on the built AAB**, not on the source.
      `aab-permissions.py` is the one that finds what a *dependency* merged into your manifest —
      a permission you never declared, or a `uses-feature` that quietly filters the app off devices.
      **All four pass on the 2026-09-02 bundle** — `versionCode` 84, `versionName` 0.4.0 — and this
      is the run that proved WorkManager's removal on the artifact rather than in the diff.
      **Six permissions now, and nothing is merged**: `WAKE_LOCK` and `ACCESS_NETWORK_STATE` left
      with WorkManager, and `RECEIVE_BOOT_COMPLETED` stayed but is now declared in the source by
      `shade/BootReceiver.kt` instead of inherited. The one entry that is not ours is AndroidX's
      signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. `aab-reflection.py` shows the same
      removal from the other side: three `androidx.startup` initializers, WorkManager's gone along
      with the `tools:node="remove"` surgery that used to hold it back.
      **The hazard is not gone with it, only this instance of it** — which is why the check is a
      standing one.
- [ ] **Every release: read the release notes gate's output** rather than trusting it passed.
      **This is not hypothetical here.** Release PR #12 (`chore(main): release 0.3.0`) opened on
      2026-08-30 and its CI went red at this gate and nowhere else — `versionName is 0.3.0, but the
      newest release notes are for 0.2.0` — which is the whole of what has kept 0.3.0 off Play. The
      0.3.0 notes are now written in `store-listing.md`, and the gate learned to accept notes that
      run **ahead** of `versionName` so they could be written on an ordinary branch rather than on
      release-please's own, which the action force-pushes over. `RELEASING.md` carries the reasoning.
      **The release PR still has to regenerate over the new `main` before it goes green** — that is
      the thing to check rather than assume.
- [ ] **Every dependency bump: re-run `licensee`.** The build fails on an unallowed licence, which is
      the point — but the fix is two things, the allowlist *and* the bundled text.
- [ ] **Test the release-shaped build on a device.** A missing R8 keep rule does not crash; it makes
      a feature silently stop working.
