# The tip is a peer-to-peer link, not an in-app purchase

## Context

`CLAUDE.md` and `README.md` both promise the same thing: *"an optional one-off tip that unlocks
nothing."* An earlier reading of Play's Payments policy — carried over from another project, which
investigated it and withdrew its own tip — concluded that Play most likely forbids this, and
`docs/PLAN.md` recorded the app as *"currently promising something it probably cannot ship."*

Re-read against the live policy text, that conclusion is wrong, and the carve-out is not the
tax-exempt one:

> "In cases where 100% of the tip or contribution from a user goes to the creator and the payment
> does not grant access to any digital content or services (including stickers, badges, special
> emojis etc.), then we regard this as a peer-to-peer payment and use of Google Play's billing
> system is **not** required."

That is Gloam's promise restated in Google's own words. The §4 anti-steering rule the earlier
investigation ran into — *"apps may not lead users to a payment method other than Google Play's
billing system"* — carries explicit exceptions for §3, §8 and §9, and the peer-to-peer carve-out
**is** §3.2.

Separately, and deliberately **not** relied on here: since October 2025, extended by the March 2026
settlement, Google no longer requires Play Billing or forbids external payment links at all. That
change applies only to *apps serving users in the United States*, and Gloam's listing serves Poland,
so the carve-out above is what actually does the work.

## Decision

**The promise stays, and the tip ships as an external peer-to-peer link** on the Support screen
(Phase 5). No billing dependency, no Console product, no server, no account — consistent with every
other claim the app makes about itself.

Three conditions are load-bearing rather than incidental, because they are exactly what §3.2 tests:

- **100% reaches the developer.** Not a project fund, not a split.
- **Nothing unlocks.** No badge, no theme, no thanks-screen that non-tippers do not get.
- **It is a payment to a person**, and the wording says so. "Tip", not "donate to the project".

## Alternatives

**A Play Billing consumable.** Unambiguously permitted, and the safe answer if the reading above is
ever contested. It loses on cost rather than on policy: a billing dependency, a Console product,
Google's cut, and an *"in-app purchases"* badge on the listing of an app whose entire pitch is that
there are none.

**Drop the tip and delete the promise.** The cheapest option, and it was the earlier project's
answer. It loses because the promise turns out to be shippable as written, and removing it on a
misreading would have quietly made the app worse for no reason.

## Consequences

- This rests on a **reading of policy text, not on an approval**. If a reviewer disagrees, the fix is
  to remove one link in an update — no code and no feature depends on it, which is the reason to
  prefer the link over anything more entangled.
- The listing copy must not describe the tip as a donation to a project or cause. That wording, not
  the mechanism, is what would move it out of §3.2.
- The framing sentence in `CLAUDE.md` — every feature free, no paid branch to keep alive — stands
  unamended, and now has a policy reading behind it rather than an assumption.
