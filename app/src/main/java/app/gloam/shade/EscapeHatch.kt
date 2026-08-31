package app.gloam.shade

import android.content.Context
import app.gloam.work.AppChannel
import app.gloam.work.channelCanAppear
import app.gloam.work.notificationsAllowed

/**
 * **Whether the shade's escape hatch — the ongoing notification's Stop action — can actually
 * appear.**
 *
 * An *escape hatch* is a surface that stops the shade and can be reached without seeing Gloam's own
 * UI, which the shade may be covering. The app's own *Stop dimming* button is not one: Phase 1's R8
 * found it sits **under** the shade at 0.33 nits at maximum dim, so it is a control that happens to
 * also remove the thing obscuring it. The full three-clause definition, and the inventory of what
 * satisfies it, is `docs/phase-2.md` §2 — this function is the third clause made executable, the one
 * that says a hatch's liveness has to be readable at the moment of the check.
 *
 * **Both halves, and either one alone is a false positive.** The app-wide permission can be on while
 * the channel is lowered to `IMPORTANCE_NONE`, which hides the notification just as completely; a
 * screen reading only the permission would report a working way out that the user cannot see.
 *
 * **Read live, never remembered.** Both halves are switches on settings screens the app deliberately
 * hands the user off to, so both can change while Gloam is in the background — the same argument the
 * dim screen already makes for `canDrawShade()`, and the reason there is no cached answer anywhere.
 *
 * A `Context` extension rather than a `ViewModel` property because it needs a `Context`, and a
 * `ViewModel` holding one is a `ViewModel` outliving its scope (CLAUDE.md).
 *
 * **One caller today, and that is a fact about this phase rather than a restriction.** Phase 2b gates
 * ultra dark on it and adds the Quick Settings tile as an independent second term —
 * `escapeHatchLive() || tileAdded()` — rather than replacing it. Writing the predicate here is what
 * keeps that gate from being defined by the feature it guards.
 */
fun Context.escapeHatchLive(): Boolean = notificationsAllowed() && channelCanAppear(AppChannel.Shade)
