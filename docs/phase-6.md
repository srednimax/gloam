# Phase 6 — Seven more languages

**The phase [`PLAN.md`](PLAN.md) promised would exist the day a third language was added.** Its exact
words were *"Languages are not a phase of their own… Adding a third is an opt-in, and the day it
happens it gets a phase file."* This is that day, and it arrives with seven at once rather than one:
**Czech, German, Spanish, French, Italian, Brazilian Portuguese and Ukrainian** — the app in nine
languages, and the Play listing in nine as well.

Sequence lives in [`PLAN.md`](PLAN.md); the live worklist in [`DOD.md`](DOD.md); the decision this
phase makes is
[ADR-0014](adr/0014-a-language-ships-on-an-audit-not-a-native-read-through.md), and it **retracts a
promise [ADR-0004](adr/0004-english-base-with-an-in-app-language-switcher.md) made** rather than
adding a new rule. The words the copy has to use are [`CONTEXT.md`](../CONTEXT.md)'s, in nine
languages now instead of two, which is what §5 of
[`translator-brief.md`](translator-brief.md) is for.

**What this phase is actually about is not the translating.** Ninety-nine strings times seven is a
long afternoon, and nothing about it is interesting. The phase exists because of the sentence
standing in front of it: *a language ships on a native speaker's read-through*. Seven reviewers were
not available, and the honest options were to wait indefinitely or to work out which half of a
read-through needed a person. §2 is that argument; the rest is what it cost to make it true.

---

## 1. What the gate demanded, in its own words

Nothing here was guessed at. The two scripts named exactly what nine shipped languages owe, and both
were run before a word was written:

```
$ python3 scripts/notes-gate.py
0.8.0 has notes for 2 locales but the app ships 9.
Missing: cs-CZ, de-DE, es-ES, fr-FR, it-IT, pt-BR, uk.

$ python3 scripts/play-metadata.py --out … --strict
FAILED: cs-CZ is shipped in the app but has no listing
… seven times …
```

**The toolchain was already built for nine.** `play-whatsnew.py`'s `LOCALES` and
`play-metadata.py`'s `APP_TAG` have carried all nine language names and Play codes since before this
phase — so adding the locales to `locales_config.xml` turned two passing gates red without a line of
script changing. That is the pipeline working as designed: the app's `locales_config.xml` is the
single declaration of what ships, and the listing gates read it rather than a list of their own.

## 2. The read-through, and why it is gone

[ADR-0014](adr/0014-a-language-ships-on-an-audit-not-a-native-read-through.md) carries the decision
and the alternatives. The part worth repeating here is the split, because it is what made the phase
finishable: a read-through was doing **two** jobs, and only one of them needed a native speaker.

- **Fluency** — register, idiom, a compound broken the wrong way. Needs a speaker. Costs
  embarrassment. Cheap for any user of that language to report, and a one-line fix.
- **The three rules that outrank fluency** — the escape hatch stays findable and exclusive, no health
  claims, and never state what the app cannot know. Needs *the brief and the string*, not fluency.
  Costs, if broken: a person under a dark overlay who has been told there is another way out when
  there is not.

So the second half was done properly and the first half got a channel instead of a gate.
[`translator-brief.md`](translator-brief.md) §2 is the checklist, written in this phase because it
was a placeholder before it and an audit against a placeholder is theatre.

## 3. The audit, and what it found

**R1 — forbidden-claim scan, all 99 × 8.** Health and *filter* vocabulary in each target language
rather than in English, matched as substrings, case-insensitively.

> **Eighteen candidates. All eighteen morphological false positives.** Polish `oczy` inside
> *Samoczynne*, Czech `oči` inside *Samočinné*, Ukrainian `сон` inside *сонця* (sun) and `очі` inside
> *щоночі* (nightly), Polish `oko` inside *blokady* and *odblokowuje*. **Zero rule violations.**

The negative result is worth as much as the method, and it is why the method is written down: a
substring scan over an inflecting language is mostly noise, so a clean read is only believable
alongside the noise it produced. A scan that had returned zero candidates would have meant the
pattern list was wrong.

**R2 — escape-hatch audit, all nine locales.** Every locale's `dim_notification_warning_body` keeps
the exclusive *only* and names the current screen; every locale's warning title names the
notification's button; and the word it uses is the **same word** the notification's own action uses
(`Zastavit`, `Stopp`, `Detener`, `Arrêter`, `Ferma`, `Parar`, `Зупинити`, `Zatrzymaj`, `Stop`).

**R3 — the invariant from R2, turned into a test.** That pairing is exactly the failure no build could
see: both halves fluent, naming two different things. `TranslationTest` now asserts that a string
which quotes another by name still contains it, over two pairs —
`shade_notification_stop` inside `dim_notification_warning_title`, and `settings_restart` inside
`schedule_rom_note`. **It was confirmed to fail when deliberately mispaired**, because a test whose
body silently skips is worse than no test, and both of these guard resources that all nine locales
have.

**R4 — the report row.** *"Something read wrong?"* under the language picker, one tap to the existing
support hand-off, with a `Gloam #language` subject of its own so translation reports sort apart from
bugs. The resolved locale is now stamped into every report's fact block — read off
`resources.configuration`, not from `currentAppLanguage()`, which is `null` for "follow the phone" and
would have told a translation report nothing.

## 4. Device readings

Taken on the Xiaomi over USB, debug build, `cmd locale set-app-locales` per locale and
`uiautomator dump` rather than screenshots: the dump carries **rendered text and bounds**, which is
what actually answers "is anything clipped", where a screenshot needs a person to squint at it.

**R5 — nine locales, three screens each, 27 dumps. Zero clipped strings.** No ellipsis anywhere, and
every bottom-bar label renders in full. The widest are Polish *Przyciemnianie* at 267 px and Ukrainian
*Налаштування* at 269 px, against a 610 px half-screen — so the tab bar was never close, and German
`Einstellungen` (235 px) was not the risk it looked like.

**R6 — and the reading that justified the whole exercise: six of the nine languages were
unreachable.**

> `LanguageRow` was a plain `Row`. At two chips that is correct and looks correct; at ten the sweep
> found **only *System*, *English*, *Polski* and *Čeština* laid out, and the remaining six simply
> absent** — not off-screen-but-present, not laid out at all. Six languages translated, declared,
> gated, complete, and not selectable from the switcher that exists to select them.

It is a `FlowRow` now, with the comment `DimControls` already carried for the auto-off chips — *five
chips do not fit one line on a narrow screen in every language, and a clipped safety control is worse
than a wrapped one.* The language row simply never got the same treatment, because it had never
needed it.

**What no test could have caught it.** `AppLanguageTest` compares the enum to `locales_config.xml`
and passes; `TranslationTest` compares resources and passes; the translation gate counts strings and
passes. **Not one of them knows how wide a chip is.** This is the compile-and-render check earning
its place in [`translator-brief.md`](translator-brief.md) §8 — a staged draft has never met a layout,
and a shipped language that cannot be chosen is a shipped language nobody reads.

**R7 — the fix, confirmed.** All nine endonyms render across three rows, none past the screen edge,
with the report row and its hint below them:

```
y=829   System  English  Polski  Čeština
y=997   Deutsch  Español  Français  Italiano
y=1165  Português (Brasil)  Українська
y=1303  Liest sich etwas falsch?
```

**The phone was put back as it was found:** app locale override cleared, `SYSTEM_ALERT_WINDOW`
re-granted by uid after the reinstall revoked it, dumps deleted from `/sdcard`. The shade was never
started, so the dim level and the schedule are untouched.

## 5. A latent bug inherited and not shipped

`currentAppLanguage()` matched the platform's locale against each entry's **whole tag**. That was
sound while every entry named a bare language, and `pt-BR` is the first that does not:
`Locale.getLanguage()` reports plain `"pt"`, `"pt" != "pt-BR"`, and the chip the user had just tapped
would never have looked selected.

It matches on the **language subtag of both sides** now, which also covers the original case the old
comment described (the platform handing back `"en-GB"` for an entry that says `"en"`). The match is
sound only while no two entries share a subtag, so `AppLanguageTest` asserts that rather than trusting
it — a second Spanish or a second Portuguese would make it *ambiguous* instead of merely wrong, and
`firstOrNull` would silently pick whichever was declared first.

**The reference repository next door has the same bug, unfixed**, with the same nine-language enum.
It was found here by reading the matcher while adding the entry that breaks it, not by a test.

## 6. The listing, in nine languages

The app's languages and the store's listing are different surfaces, and this phase moved both —
**because `--strict` leaves no third option.** A shipped language with no listing section is refused
before supply starts, and a shipped language with a *blank* section is what Play itself refused in
Phase 5 R1. So nine app languages means nine listings, in full.

**The tight constraint is the full description, and it is tighter than it looks.** Play's limit is
4000 characters and the Romance languages run 12–20% longer than the English:

| | Draft | Shipped |
| --- | --- | --- |
| Czech | 3573 | 3573 |
| German | 4187 | **3996** |
| Spanish | 4073 | **3995** |
| French | 4344 | **3999** |
| Italian | 4036 | **3972** |
| Brazilian Portuguese | 4071 | **3998** |
| Ukrainian | 3922 | 3922 |

Five of seven drafts were over the limit and needed cutting. **Where the cuts came from is the part
worth recording:** enumerations collapsed to lists (*"over other apps, over the launcher, over your
browser"* → *"over everything: other apps, launcher, browser"*), and restatements compressed. **What
was never cut:** the touch-through paragraph, the backlight-floor limits, the secure-screen
explanation, and the closing health line. Those four are what the description exists to be honest
about; the rest is arrangement.

**Four languages now sit within five characters of the ceiling.** That is a standing cost this phase
hands forward, and it is recorded at the top of
[`store-listing.md`](store-listing.md): an English sentence added to the full description is a cut in
four other languages. The counts in the headings are **computed, not estimated** — they are the only
warning before the Console refuses a paste.

**Release notes: nine locales, longest 437/500.** 0.8.0's note was written in two locales in Phase 5
and is now written in nine, for a release that has not been promoted yet — which is the correct order,
since Play does not allow release notes to be edited on a live release.

## 7. Known gaps, deliberately

- **The screenshots are English-only** — three shots, one locale, and Play will show them under all
  nine listings. A German listing with an English screenshot is a quality gap rather than a falsehood,
  and `scripts/screenshots.py` already carries a locale tag in its filenames (`1_dim-en.png`), so the
  route to fixing it exists and costs a device run per language. Not taken here: the listing copy is
  what a reader searches and reads, and eight more device runs buy less than the afternoon they cost.
- **No native read-through, for seven of nine.** That is the decision, not the gap — but it is named
  here too, because a reader of this file should not have to find ADR-0014 to learn it.
- **Argument roles are still unchecked.** `TranslationTest` holds that the *same* format arguments
  appear in every locale; nothing holds that an argument still refers to the same thing. That was the
  read-through's half, it has no replacement, and the honest statement of it now sits in the test's own
  comment rather than in a promise.
