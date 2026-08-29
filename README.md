# Gloam

Take the screen below the brightness Android is willing to give you. A dimming overlay for reading
in the dark, when the lowest system setting is still too bright.

**Every feature is free.** No ads, no server, no account, nothing behind a paywall. If the app
earns it there is a one-off tip — it unlocks nothing, it just says thanks.

## Status

Bootstrapped from [android-starter](https://github.com/srednimax/android-starter), and now a working
proof of concept: it dims the screen. A slider sets a dim level, a foreground service draws the shade
over every other app, and it survives you leaving Gloam — which is the whole point, since the apps
being dimmed are the ones you are reading.

The template's data layer is gone ([ADR-0007](docs/adr/0007-gloam-stores-settings-not-records-so-it-has-no-database.md)):
Gloam stores settings, not records, so there is no database. The release pipeline and quality gates
are real. Nothing is on Play yet, the mark is still a placeholder, and the feature set beyond
"it dims" is still being decided.

## Contributing

Gloam is source-available, not open source: read it, verify it, file issues. Pull requests are not
accepted — see [LICENSE](LICENSE).

## Build

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # build + install on the connected phone
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented Room/DAO tests — needs a device
./gradlew spotlessApply          # format (the CI gate is spotlessCheck)
python3 scripts/project.py       # what the toolchain thinks this app is called
```

Commit subjects are [Conventional Commits](https://www.conventionalcommits.org); release-please
derives the version and `CHANGELOG.md` from them. See [docs/RELEASING.md](docs/RELEASING.md).
