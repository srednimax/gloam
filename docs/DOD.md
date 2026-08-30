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

- [ ] **Create the upload keystore, outside the repo**, and put its four values in
      `local.properties`. Back it up somewhere that is not this machine: losing it means never being
      able to update the app on Play again. **First**, because package registration is keyed to the
      **signing key**.
- [ ] **Create the Play Console entry and register the package name.** All Play packages must be
      registered by **30 September 2026**, and under-50-install names are first-come, first-served —
      so this is also how `io.github.srednimax.gloam` is secured.
- [ ] **Read back the app's access status while you are in there**, and record the answer here.
      It settles the plan's biggest scheduling unknown: is the 12-tester / 14-day closed test
      **per app**, or a one-time account unlock the existing live app already satisfied?
      Until it is answered, assume it applies.
- [ ] **Set up the GitHub repository** — `python3 scripts/repo-setup.py` does the ruleset, the
      merge setting and Pages; the release-please PAT is the one step it cannot do, and without it
      no release PR is ever opened. *Setting up a new repository* in [`RELEASING.md`](RELEASING.md).
      **None of this is inherited from the template** — GitHub copies files, never settings — and
      the build stays green while it is all still undone.
- [ ] **Set the five Play secrets and create the service account** (`docs/RELEASING.md`) — the
      service account is the one step CI cannot do for itself.
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
      access needs them opted in *continuously for 14 days*. Potentially the longest lead item in the
      plan and no amount of effort shortens it. **This account was created in 2026**, so it is inside
      the post-Nov-2023 regime and no age or organisation exemption is available; what is unconfirmed
      is only whether the requirement is per-app — see the Phase P item above.
- [ ] **Replace the placeholder mark.** `art/mark.py`, then `python3 art/make-launcher-icon.py` and
      `make-feature-graphic.py`. Record the provenance in `art/README.md` — where the art came from is
      the thing most likely to block a first upload, and it is discovered late.
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

- [ ] **Write the listing.** `docs/store-listing.md` — every heading in it is parsed by a script.
- [ ] **Write the privacy policy** and confirm GitHub Pages is serving `docs/`. Play requires a
      *hosted* URL, and an offline app has no server of its own.

## Standing checks that never close

- [ ] **Every release: run the artifact checks on the built AAB**, not on the source.
      `aab-permissions.py` is the one that finds what a *dependency* merged into your manifest —
      a permission you never declared, or a `uses-feature` that quietly filters the app off devices.
- [ ] **Every release: read the release notes gate's output** rather than trusting it passed.
- [ ] **Every dependency bump: re-run `licensee`.** The build fails on an unallowed licence, which is
      the point — but the fix is two things, the allowlist *and* the bundled text.
- [ ] **Test the release-shaped build on a device.** A missing R8 keep rule does not crash; it makes
      a feature silently stop working.
