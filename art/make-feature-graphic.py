#!/usr/bin/env python3
"""Render the 1024x500 Play feature graphic to art/play-feature-graphic.png.

Original art only: the mark comes from `mark.py`, the same declaration the launcher
icon is generated from, so the two assets cannot drift into being different marks. Text
is *rendered* with Noto Sans, which the OFL explicitly permits without any notice — what
it restricts is redistributing glyph outlines as art.

The picture is the app's subject rather than a logo on a background: a moon over the last
of the light. Its three colours are **read out of the app's dark scheme** at run time (see
`scheme()`), so the graphic cannot end up in colours the app itself no longer uses.

    python3 art/make-feature-graphic.py
"""

import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import mark

# The three strings this graphic is *about*. Edit them; everything else is layout.
APP_NAME = "Gloam"
TAGLINE = "Dim below Android's lowest brightness"
FEATURES = "No ads  ·  No account  ·  Offline  ·  Free"

OUT = Path(__file__).parent / "play-feature-graphic.png"
COLOR_KT = Path(__file__).parent.parent / "app/src/main/java/app/gloam/theme/Color.kt"
W, H = 1024, 500
S = 4  # supersample factor; everything below is in final pixels, scaled by S at draw time


def scheme(*roles):
    """Colour roles read out of `theme/Color.kt`'s dark scheme, as (r, g, b).

    Pasting the values here would make this file a second copy of the palette, and
    `scripts/gen_scheme.py` regenerates the first one from seeds — so the copy would go stale
    silently, the way this graphic's text inks once did. Reading them means a regenerated
    palette reaches the listing art by re-running this script and nothing else.
    """
    body = COLOR_KT.read_text().split("darkColorScheme(", 1)[-1]
    out = []
    for role in roles:
        found = re.search(rf"\b{role} = Color\(0x[0-9A-Fa-f]{{2}}([0-9A-Fa-f]{{6}})\)", body)
        if not found:
            raise SystemExit(f"{COLOR_KT}: no `{role}` in the dark scheme — has it been renamed?")
        h = found.group(1)
        out.append(tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)))
    return out


# The night, the last light in it, and the cold edge at the top of the sky. The mark itself
# stays SURFACE, shared with the launcher icon, so the moon is the same cream in both.
NIGHT, GLOW, DUSK = scheme("background", "primary", "tertiary")
SURFACE = mark.SURFACE

# Where the light sits, in fractions of the canvas. Its centre is deliberately *below* the bottom
# edge: only the top of the glow is on the canvas, which is what makes it a horizon rather than a
# lamp hanging in the sky — and keeping it right of centre leaves the copy on unlit ground, where
# a light ink still has something to be light against.
GLOW_AT = (0.88, 0.98)
GLOW_SIZE = (0.72, 0.66)
GLOW_FALLOFF = 1.4  # >1 keeps the bright core small and the spill long

# Two steps down from SURFACE towards the night, so the three text weights read as one family
# against the ground. Tinted rather than grey: a neutral grey on a coloured ground looks like a
# mistake.
#
# Derived, not pasted. These were literals until the palette changed and they stayed blue on a
# ground that had gone amber — the exact failure the comment above describes, produced by the
# comment's own rule being written down instead of executed.
def _step(t):
    """SURFACE mixed `t` of the way towards NIGHT, componentwise."""
    return tuple(round(s + (g - s) * t) for s, g in zip(SURFACE, NIGHT))


TAGLINE_INK = _step(0.16)
SUBTLE = _step(0.34)

FONT_DIR = Path("/usr/share/fonts/truetype/noto")
FONT_BOLD = FONT_DIR / "NotoSans-Bold.ttf"
FONT_REG = FONT_DIR / "NotoSans-Regular.ttf"


def dusk(size):
    """The ground: night, with the day's last light banked low on the right.

    One pass per pixel at final size — the supersampling above is for the *edges* of the mark
    and the text, and a gradient has no edges to alias, so it is built once here and scaled up
    with everything else.
    """
    w, h = size
    img = Image.new("RGB", (w, h))
    px = img.load()
    gx, gy = GLOW_AT
    sx, sy = GLOW_SIZE
    for y in range(h):
        v = (y / h) ** 1.6  # the violet is thickest at the top of the sky
        for x in range(w):
            # Elliptical distance from the glow's centre, clamped to 0 at its edge.
            t = max(0.0, 1.0 - math.hypot((x / w - gx) / sx, (y / h - gy) / sy)) ** GLOW_FALLOFF
            lit = tuple(n + (g - n) * t for n, g in zip(NIGHT, GLOW))
            # A little of the cold end of the palette where the light does not reach.
            cold = 0.10 * (1 - v) * (1 - t)
            px[x, y] = tuple(round(c + (d - c) * cold) for c, d in zip(lit, DUSK))
    return img


def draw_mark(base, cx, cy, size):
    """The shared mark from `mark.py`, centred on (cx, cy) at `size` px across, in final pixels.

    Painted onto its own layer so the crescent can be punched to full transparency rather than
    filled with a guess at the ground colour — the background here is a gradient, so a
    painted crescent would only match at one height. The launcher icon gets the same hole from
    the same declaration, by winding rather than by alpha. Here the hole earns its keep twice:
    the sky shows through the moon's dark side, which is what the sky does.
    """
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x0, y0, x1, y1 = mark.bounds()
    k = size / max(x1 - x0, y1 - y0)

    def tf(p):
        return ((cx + (p[0] - (x0 + x1) / 2) * k) * S, (cy + (p[1] - (y0 + y1) / 2) * k) * S)

    for e in mark.PARTS:
        d.polygon(mark.outline(e, tf), fill=(0, 0, 0, 0) if e.hole else SURFACE + (255,))
    base.alpha_composite(layer)


def main():
    base = dusk((W, H)).convert("RGBA").resize((W * S, H * S), Image.BICUBIC)

    draw_mark(base, cx=858, cy=228, size=230)

    d = ImageDraw.Draw(base)
    wordmark = ImageFont.truetype(str(FONT_BOLD), 116 * S)
    tagline = ImageFont.truetype(str(FONT_REG), 34 * S)
    features = ImageFont.truetype(str(FONT_REG), 27 * S)

    x = 74 * S
    d.text((x, 232 * S), APP_NAME, font=wordmark, fill=SURFACE, anchor="ls")
    d.text((x, 292 * S), TAGLINE, font=tagline, fill=TAGLINE_INK, anchor="ls")
    d.text((x, 378 * S), FEATURES, font=features, fill=SUBTLE, anchor="ls")

    # Play rejects alpha, and the widest line decides whether the copy clears the mark.
    for label, font, text in (("tagline", tagline, TAGLINE), ("features", features, FEATURES)):
        width = d.textlength(text, font=font) / S
        print(f"  {label}: {width:.0f}px wide, ends at x={74 + width:.0f}")

    out = base.resize((W, H), Image.LANCZOS).convert("RGB")
    out.save(OUT, "PNG", optimize=True)
    print(f"{OUT}  {out.size[0]}x{out.size[1]}  {OUT.stat().st_size / 1024:.0f} KiB  mode={out.mode}")


if __name__ == "__main__":
    main()
