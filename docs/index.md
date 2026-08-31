---
layout: default
title: Gloam
---

# Gloam

<!--
    The site root, served by GitHub Pages from `docs/`. It exists for two reasons: Play's listing
    asks for a Website, and the privacy policy needs a hosted URL. Both come free from a public
    repo — no hosting, no domain, no bill.

    It is not the store listing (that is `store-listing.md`, which scripts parse) and it is not the
    README (that is for someone reading the code). Write it for someone who found the app in the
    store and wants to know what it is before installing.

    It describes the build at the door — Phase 2 — which is the first one strangers see: the dim
    level, warmth and auto-off. The panel, the schedule and ultra dark are later phases and are
    deliberately not mentioned. If a phase slips a feature, this file is one of the things that has
    to move with it.

    Keep health claims out of this page as well as the listing. App content was answered health-No,
    and Play's enforcement has treated a linked page as part of the listing.
-->

**A screen dimmer that goes below your phone's minimum brightness.** Free, ad-free, and entirely on
your own phone: no account, no sign-up, no server.

Android's brightness slider stops at a floor. On most phones that floor is still too bright to read
in a properly dark room. Gloam is the range underneath it.

## What it does

- **Dims past the floor.** One control, from *barely dimmed* to *very nearly dark*. Gloam first takes
  the backlight down to the lowest your phone allows, then draws a shade over the screen to go the
  rest of the way.
- **Warms the screen.** A separate control tints the shade amber. The tint eases off on its own as
  the dim level nears its darkest, so the two controls together always leave something readable
  underneath.
- **Turns itself off.** Every shade you start by hand gets a deadline, so you never unlock a phone
  you cannot read the next morning. You can set that to **Never**, but it is on by default.

## What it cannot do

Worth knowing before you install:

- **It does not make your screen emit less light than the backlight floor.** Nothing an app can
  install can do that — the floor belongs to the display driver. Gloam gets the backlight to that
  floor and then puts a dark layer in front of it. The result reads as far dimmer; the panel is still
  lit.
- **A few things draw above it.** System permission dialogs and some secure screens sit above every
  app's overlay by design, so they will appear at full brightness. That is Android protecting you
  from apps like this one, and it is working as intended.
- **The shade never takes your taps.** Every touch passes straight through to whatever is underneath,
  and there is always an ongoing notification with a Stop button. Both are deliberate: an overlay
  over every other app has to be impossible to get stuck behind.
- **Some phones stop it.** Aggressive battery managers — Xiaomi's especially — will kill the service
  on their own. If the shade vanishes without you touching it, that is your ROM, and the fix is in
  its battery settings rather than in Gloam.

Gloam is a dimmer. It makes no claim about your eyes, your sleep or your health.

## Your data stays on your device

There is no backend to send anything to. Gloam has no account, no analytics and no network
permission at all — it stores a dim level and whether the shade should be on, and nothing else.
Backups go to storage you choose. The details are in the

**[Privacy Policy](privacy-policy.md)**.

## Support

**gloam.dimmer@gmail.com** — an ordinary mailbox that a person reads.

## The source

Built in the open at **[github.com/srednimax/gloam](https://github.com/srednimax/gloam)**.
