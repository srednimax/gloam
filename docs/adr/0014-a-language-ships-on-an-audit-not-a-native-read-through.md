# A language ships on an audit and a report row, not on a native read-through

## Context

[ADR-0004](0004-english-base-with-an-in-app-language-switcher.md) made English the base, made
completeness a merge gate, and left one promise outside both: that a translation would be *read* by
somebody who speaks the language before it shipped. `scripts/translation-gate.py` states it in its
own docstring — *"A language ships on a native speaker's read-through, and no script stands in for
that"* — and points at a section of [`translator-brief.md`](../translator-brief.md) that was never
written.

[`PLAN.md`](../PLAN.md) then said languages are not a phase of their own, and that adding a third is
an opt-in that gets a phase file the day it happens. This is that day, and it arrives with seven
languages rather than one: Czech, German, Spanish, French, Italian, Brazilian Portuguese and
Ukrainian, the same set as the reference repository next door.

**Seven native reviewers were not available, and no plausible way of finding them was.** Recruiting
strangers to proofread a screen dimmer for free was considered and rejected as a plan rather than
overlooked. So the promise as written had exactly one outcome: seven finished, mechanically green
drafts sitting in `translations/` indefinitely, and seven markets reading an app in English while the
drafts rot against every English string added after them.

## Decision

**The seven languages ship without a native read-through.** What replaces it is not a weaker version
of the same check — it is the half of that check which does not need a native speaker, done
properly, plus a channel for the half that does.

## Why this is not simply lowering the bar

The read-through was doing two jobs at once, and they are not equally dangerous.

- **Fluency.** Wrong register, a stilted phrase, a compound a native speaker would break differently.
  The cost of getting this wrong is embarrassment; it is visible to every user of that language, it
  is cheap for any one of them to report, and nothing about it is irreversible — a string is a
  one-line edit and a release.
- **The three rules that outrank fluency.** They are written out as
  [`translator-brief.md`](../translator-brief.md) §2 and they are this app's, not craft advice:

  1. **The escape hatch stays findable.** `CLAUDE.md`'s first house rule is that the shade must never
     trap the user. Some of this copy is the only thing standing between a person and a screen they
     cannot read their way out of, and it makes a *safety claim* — that there is a Stop button
     outside the app, or that there is not. A translation that softens *"the only way to stop
     dimming is this screen"* into *"you can also stop it here"* is not a style problem.
  2. **No health claims.** Play's App content declaration was answered health-No. No *eye strain*,
     no *sleep*, no *blue light*, no *filter* — and the schedule and warmth copy are the two places
     a translator would most reasonably reach for them, because that is what apps of this shape
     usually say.
  3. **Never state what the app cannot know.** Gloam cannot read back the autostart grant or the
     battery exemption, so that copy names where to look and promises no outcome. A translation that
     tightens *"this is the setting to look at"* into *"turn this on and the shade will come back"*
     invents a guarantee the platform never gave.

The load-bearing observation is that **only the first job needs a native speaker.** The second is a
bounded checklist against a fixed list of resources: does this string still say the escape hatch is
the only one, does it claim something about the body, does it promise something the app cannot see.
Answering it needs the *brief* and the *string*, not fluency — and it was answerable, so it was
answered.

## What was actually done instead

Four things, all before the locales went into `locales_config.xml`, and all recorded in
[`phase-6.md`](../phase-6.md):

1. **A forbidden-claim scan** across all 99 × 8 resources, for the health and *filter* vocabulary of
   each language rather than of English. **Eighteen candidates, all eighteen morphological false
   positives** — Polish `oczy` inside *Samoczynne*, Czech `oči` inside *Samočinné*, Ukrainian `сон`
   inside *сонця* and `очі` inside *щоночі*. Zero rule violations, and the negative result is worth
   as much as the method: a substring scan over an inflecting language is mostly noise, and knowing
   that is what makes the clean read believable.
2. **An escape-hatch audit** over the copy rule 1 covers, in all eight: every locale's
   `dim_notification_warning_body` keeps the *only* and names the screen, and every locale's warning
   title names the notification's button using the **same word** the notification's action uses.
3. **Two cross-string invariants turned into a test.** The pairing in 2 is exactly the failure no
   build could see — both halves fluent, naming two different things — so
   `TranslationTest` now asserts that a string quoting another by name still contains it, for
   `shade_notification_stop` inside `dim_notification_warning_title` and `settings_restart` inside
   `schedule_rom_note`. It was confirmed to fail when deliberately mispaired, because a silently
   skipping test is worse than none.
4. **A report row under the language picker**, wired to the existing support hand-off with a
   `Gloam #language` subject of its own, and the resolved locale stamped into every report's fact
   block so nobody has to say which language they are reading. This is the substantive change: the
   read-through was a gate *before* shipping, and what replaces it is a channel *after*.

## Alternatives

- **Waiting.** Seven complete drafts unshipped for an unbounded time, for a review with no plausible
  source. Rejected; it is the status quo with extra steps.
- **Shipping a subset.** The languages differ in how far they are from English, not in review risk,
  and each shipped language pays the same completeness cost at the gate anyway. No principled line
  divides them.
- **Paid review.** Not rejected on principle. It stays available for any language that reports
  enough trouble to earn it, which is a better trigger than buying all seven up front on the guess
  that they need it.
- **A `translations-pending` allowlist**, so an English string could merge ahead of its translations.
  Rejected: the *translate once, not twice* argument for it assumed a round of review would reword
  the copy afterwards. Without that round the drafting is one pass, so the argument loses its
  premise.
- **Machine back-translation as the gate itself.** It cannot see register, so it is not a substitute
  for a person — only for the part of a person's job that was never about being a person. The
  mechanical part of it is items 1–3 above.

## Consequences

- **A language can now be wrong in the field, and that is accepted.** The owner of this repo accepts
  that users will report language bugs. The exposure is bounded by the audit: what can be wrong is
  register and idiom, not what the app claims about the user's body or about the way out of the
  shade.
- **The completeness gate now demands eight translations for every future English string**, with no
  review cycle behind it. That is the real ongoing cost of this decision and it falls on every
  branch that adds copy.
- **Fluency findings arrive as ordinary bug reports** and are fixed as ordinary strings. The one
  thing a report must not be allowed to do is talk the app back across the three rules — a
  plausible-sounding suggestion is still checked against the brief, because the reporter has the
  fluency and the brief has the reasoning.
- **Clipping is fixed by shrinking the label, never by shortening the string.** With no reviewer,
  prefer the fix that needs no vocabulary judgement. German and Ukrainian are the long ones here;
  `dim_column_floor` and the auto-off chips are where it will show first.
- **This changes ADR-0004's promise, not its rule.** Every user-visible string is still a resource in
  every locale, translated from English only, and `TranslationTest` still keeps them level. What is
  retracted is *read end to end by a native speaker before it ships* — replaced by *audited against
  the three rules before it ships, and reported on by the people who read it*. A tenth language takes
  the same path.
- **`translator-brief.md` §§1, 2 and 5 stop being placeholders.** The audit is answerable only
  against a written brief, so the brief is now part of the mechanism rather than a courtesy to a
  future translator: §2 is the checklist item 1 and 2 above were run against.
