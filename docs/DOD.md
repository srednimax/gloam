# Definition of done — what is still open

The **live checklist**. Keep it short: when an item closes, tick it, write the *result* into whatever
long-form record you keep, and delete the detail from here. A session should be able to pick up the
work by reading this file alone.

Phases and sequence live in [`PLAN.md`](PLAN.md). This file is the worklist.

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
- [ ] ⚠️ **Back the keystore up somewhere that is not this machine.** **Now the sharpest open
      item in this file:** as of 2026-08-30 a published package is bound to this key, so losing it
      stopped being theoretical. One copy exists. Losing it
      means never being able to update the app on Play again — recoverable only by resetting the
      upload key with Google, and only while the app still exists. The keystore *and* its password,
      which is in `local.properties` and nowhere else.
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
- [ ] **Answer the Console's *App content* questionnaire** — data safety, ads, target audience,
      content rating. It gates a *closed* release, not only production, so it sits in front of the
      door with everything else here. Gloam's answers are unusually short (no network, no account,
      no analytics) and `scripts/aab-permissions.py` is what keeps the artifact honest about them:
      `INTERNET` and `AD_ID` are asserted absent rather than assumed. `docs/play-app-content.md`,
      where the previous app wrote those answers down, does not exist here yet; Phase 5 writes it.
- [x] **Set up the GitHub repository.** Done, verified 2026-08-30 by
      `python3 scripts/repo-setup.py --dry-run`, which reports the `main: require CI` ruleset, the
      rebase-only merge setting, Pages from `main/docs` and `RELEASE_PLEASE_TOKEN` all already in
      place. It is idempotent, so re-run it after any manual change in the GitHub UI rather than
      trusting this box.
- [ ] **Set the five Play secrets and create the service account** (`docs/RELEASING.md`). The
      service account is the one step CI cannot do for itself; four of the five secrets are now just
      a copy out of `local.properties`, and `repo-setup.py --dry-run` lists which are still missing:
      ```bash
      base64 -w0 ~/.keystores/gloam-upload.jks | gh secret set UPLOAD_KEYSTORE_BASE64
      gh secret set UPLOAD_STORE_PASSWORD   # the upload.storePassword value
      gh secret set UPLOAD_KEY_ALIAS        # upload
      gh secret set UPLOAD_KEY_PASSWORD     # the same string (PKCS12 keeps one password)
      ```
      Wanted at the first upload, not before. The fifth, `PLAY_SERVICE_ACCOUNT_JSON`, has the
      Play-to-API propagation delay in front of it, so create the service account before you need it.
- [x] **Decide the `applicationId` deliberately.** Done: **`io.github.srednimax.gloam`**. Reverse-DNS
      on a namespace verifiably yours; Play has never checked domain ownership and package
      registration is keyed to the **signing key**, not a domain. Short generic names like
      `app.gloam` are the ones another developer could claim first. `namespace = "app.gloam"` is
      internal and never reaches Play.

## Before the door — Phase 2 freezes these

Cheap now, expensive or impossible once a build sits on twelve strangers' phones.

- [ ] **Resolve four pieces of scaffolding that describe features the app does not have.** A future
      reader cannot tell an unused defence from a live one:
      `work/NotificationPermission.kt` (uncalled — wired up by Phase 1's entry gate),
      `work/BatteryExemption.kt` (uncalled — Phase 4),
      **`onboardingDone` in `AppPreferences`** (read by nothing — use it in Phase 2's first-run flow
      or delete the key; it is a *stored* key, so this is the last phase in which the choice is
      free), and `scripts/project.py` still reporting `DATABASE_FILE gloam.db` after ADR-0007
      removed the database.
- [ ] **Ask for `POST_NOTIFICATIONS`.** `work/NotificationPermission.kt` exists and nothing calls it.
      The shade's ongoing notification is the documented way out of a very dark screen, and on
      Android 13+ it is invisible until this is granted — so the safety property the house rules
      claim is only half true today. Phase 1's entry gate.
- [ ] **Recruit 12 closed testers — start in Phase 1, not when the build is ready.** Production
      access needs them opted in *continuously for 14 days*, and **this is now confirmed to apply to
      Gloam** (Phase P, 2026-08-30) rather than being an assumption. It is the longest lead item in
      the plan, it is calendar time rather than effort, and it depends on other people replying.
      **Recruit more than twelve.** The window wants twelve opted in *continuously*; one person
      uninstalling partway through is the failure this rule is shaped to produce.
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
- [ ] **The three emulator CI legs are vacuously green.** `app/src/androidTest` contains **zero
      tests**, so *Instrumented tests (API 26 / 34 / 36)* pass by having nothing to run, and the API
      26 leg could not install this app even if it did — `minSdk` is 33. That is ~15 minutes of CI
      per PR buying a checkmark with nothing behind it, and worse, a green tick that would keep
      showing green if a real instrumented test were added and then broke the install. Phase 2 either
      gives the matrix real tests or cuts it; recorded now so the next reader does not trust it.
- [ ] **Create the API-33 AVD** and run the end-of-phase pass on it (ADR-0008). `sdkmanager`,
      `avdmanager` and `emulator` are all CLI — no Android Studio needed. The phone stays the only
      place nits are measured; the emulator is the only place the non-36 range runs at all.
- [ ] **Re-read ADR-0004 now `minSdk` is 33.** The below-13 locale backport — the disabled
      `AppLocalesMetadataHolderService` manifest entry — exists only for devices the app no longer
      ships to. Removing it is ADR-0004's decision to amend, not a tidy-up. AppCompat itself stays
      regardless: ADR-0006 needs `setDefaultNightMode`.
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
      `docs/index.md` is **still the template's `<App name>` placeholder** and Pages is serving it
      publicly, which is why the listing's Website field is blank.

## Standing checks that never close

- [ ] **Every release: run the artifact checks on the built AAB**, not on the source.
      `aab-permissions.py` is the one that finds what a *dependency* merged into your manifest —
      a permission you never declared, or a `uses-feature` that quietly filters the app off devices.
      All four pass on the 2026-08-30 bundle. **Three of Gloam's eight permissions are merged rather
      than written** — `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and `RECEIVE_BOOT_COMPLETED`, all
      WorkManager's — which is exactly the reading the source cannot give you.
- [ ] **Every release: read the release notes gate's output** rather than trusting it passed.
- [ ] **Every dependency bump: re-run `licensee`.** The build fails on an unallowed licence, which is
      the point — but the fix is two things, the allowlist *and* the bundled text.
- [ ] **Test the release-shaped build on a device.** A missing R8 keep rule does not crash; it makes
      a feature silently stop working.
