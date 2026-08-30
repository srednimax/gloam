---
layout: default
title: Privacy Policy
---

# Privacy Policy

_Last updated: 30 August 2026_

Gloam is an Android app that dims your screen below the darkest setting Android offers. This policy
describes what it does with information. It is short because the app does almost nothing.

## The app collects nothing

Gloam has no account, no sign-in, no server, and **no network access of any kind**. That is not a
promise about intent: the app does not hold Android's `INTERNET` permission, so it is not capable of
sending anything anywhere, and a check runs against every release build to confirm that permission
has not appeared.

There is no analytics, no crash reporting, no advertising, and no third-party SDK that phones home.

## What the app stores

Five settings, in the app's private storage on your own device, readable only by the app:

- how dim you set the screen
- whether the shade should be on
- your theme choice, and whether to follow the system colours
- whether you have seen the app's first-run screen

That is the whole list. None of it identifies you, and none of it leaves your phone by any route the
app controls.

## What the app can access, and why

- **Display over other apps.** This is the app itself. Gloam dims by drawing a dark layer above
  whatever you are using, which is the only way to go below the system's minimum brightness. The
  layer passes every touch straight through to the app underneath and reads nothing about it.
- **Notifications.** While the shade is on, Android requires a permanent notification, and Gloam
  wants one anyway: it is how you turn the dimming off from anywhere, including from a screen that
  has become too dark to read. It contains no information about you.

## The two things that happen outside the app

Neither is done by Gloam, but both are the honest answer to "where could my data be":

1. **Android's automatic backup.** If it is enabled on your phone, Android backs up app settings to
   your own Google Drive, in a space that does not count against your storage quota and that Google
   states apps cannot read. For Gloam that means the five settings above and nothing else. You can
   turn it off in your phone's settings.
2. **Google Play.** Installing and updating an app is a transaction between your phone and Google,
   with its own privacy policy. The developer receives aggregate, anonymous statistics from Play —
   install counts, crash rates, country-level breakdowns — and no information about individuals.

## Children

The app is not directed at children, and collects nothing from anyone.

## Deleting your data

Uninstalling Gloam removes everything it stored. There is nowhere else to delete it from, because
there is nowhere else it went.

## Changes

If this policy changes, the date at the top changes with it, and the full history of this file is
public in the app's repository.

## Contact

Questions about this policy can be sent to the developer email address listed on the app's Google
Play listing.
