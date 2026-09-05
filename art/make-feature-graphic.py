#!/usr/bin/env python3
"""Render the 1024x500 Play feature graphic to art/play-feature-graphic.png.

Original art only: the mark comes from `mark.py`, the same declaration the launcher icon is
generated from, so the two assets cannot drift into being different marks. The ground is
`feature-background.png`; its provenance is recorded in `art/README.md`. Text is *rendered* with
Noto Sans, which the OFL explicitly permits without any notice — what it restricts is
redistributing glyph outlines as art.

**Play crops this graphic.** The asset is 2.048:1 and Play drops it into a 16:9 box with
`object-fit: cover` on the search-results card, so a slice of each side never reaches a viewer.
The layout is therefore declared against the *cropped* frame rather than the canvas — see CROP —
and main() measures what it produced instead of trusting that the constants still hold.

    python3 art/make-feature-graphic.py
"""

import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

import mark

# The three strings this graphic is *about*. Edit them; everything else is layout.
APP_NAME = "Gloam"
TAGLINE = "Dim below Android's lowest brightness"
FEATURES = "No ads  ·  No account  ·  Offline  ·  Free"

ART = Path(__file__).parent
OUT = ART / "play-feature-graphic.png"
BACKGROUND = ART / "feature-background.png"
COLOR_KT = ART.parent / "app/src/main/java/app/gloam/theme/Color.kt"
W, H = 1024, 500
S = 4  # supersample factor; everything below is in final pixels, scaled by S at draw time

# What Play's 16:9 card takes off each side, derived from the two aspect ratios rather than
# measured once and pasted. The first cut of this file put the wordmark 74px in and the moon's
# edge at 973, which left 6px of visible left margin and sliced 17px off the moon.
#
# So MARGIN is the gap from the *canvas* edge, sized to leave a real margin inside the crop as
# well, and everything is positioned against it — including the mark, which is right-aligned to
# it rather than placed by eye so it cannot drift back over the line if the mark is ever redrawn
# a different width.
CROP = round(W * (1 - (16 / 9) / (W / H)) / 2)  # 68px lost from each side
VISIBLE_MARGIN = 40  # what a viewer of the cropped graphic should actually see
MARGIN = CROP + VISIBLE_MARGIN  # 108px from the canvas edge to any content, both sides

MOON_SIZE = 190
MOON_CY = 205
MIN_GAP = 24  # the copy must clear the moon by this much; main() checks it


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


NIGHT, = scheme("background")
SURFACE = mark.SURFACE  # the mark stays the launcher icon's cream, so the moon matches


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


def ground(size):
    """`feature-background.png`, cover-fitted to the canvas.

    Cover rather than a plain resize: the source is within a rounding error of 2.048:1 today, but
    a straight `resize()` would silently stretch a replacement that isn't, and a stretched sky is
    the kind of wrong that only looks like a slightly odd gradient.
    """
    if not BACKGROUND.exists():
        raise SystemExit(f"{BACKGROUND}: missing — see art/README.md for what this file is.")
    img = Image.open(BACKGROUND).convert("RGB")
    w, h = size
    k = max(w / img.width, h / img.height)
    scaled = img.resize((round(img.width * k), round(img.height * k)), Image.LANCZOS)
    left, top = (scaled.width - w) // 2, (scaled.height - h) // 2
    return scaled.crop((left, top, left + w, top + h))


def draw_mark(base, cx, cy, size):
    """The shared mark from `mark.py`, centred on (cx, cy) at `size` px across, in final pixels.

    Painted onto its own layer so the crescent can be punched to full transparency rather than
    filled with a guess at the ground colour — the background here is a photograph, so a painted
    crescent would match it nowhere at all. The launcher icon gets the same hole from the same
    declaration, by winding rather than by alpha. Here the hole earns its keep twice: the sky
    shows through the moon's dark side, which is what the sky does.
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
    base = ground((W, H)).convert("RGBA").resize((W * S, H * S), Image.BICUBIC)

    moon_left = W - MARGIN - MOON_SIZE
    draw_mark(base, cx=W - MARGIN - MOON_SIZE / 2, cy=MOON_CY, size=MOON_SIZE)

    d = ImageDraw.Draw(base)
    wordmark = ImageFont.truetype(str(FONT_BOLD), 104 * S)
    tagline = ImageFont.truetype(str(FONT_REG), 31 * S)
    features = ImageFont.truetype(str(FONT_REG), 25 * S)

    x = MARGIN * S
    d.text((x, 240 * S), APP_NAME, font=wordmark, fill=SURFACE, anchor="ls")
    d.text((x, 296 * S), TAGLINE, font=tagline, fill=TAGLINE_INK, anchor="ls")
    d.text((x, 372 * S), FEATURES, font=features, fill=SUBTLE, anchor="ls")

    out = base.resize((W, H), Image.LANCZOS).convert("RGB")  # Play rejects alpha
    out.save(OUT, "PNG", optimize=True)

    # Measured, not assumed. Both of these have been wrong in this file before.
    widest = max(d.textlength(t, font=f) / S for t, f in
                 ((APP_NAME, wordmark), (TAGLINE, tagline), (FEATURES, features)))
    right = MARGIN + widest
    gap = moon_left - right
    print(f"crop takes {CROP}px per side -> Play shows x {CROP}..{W - CROP}")
    print(f"copy spans x {MARGIN}..{right:.0f}, moon x {moon_left}..{W - MARGIN}"
          f" — {'inside the crop' if right <= W - MARGIN else 'CLIPPED by the 16:9 crop'}")
    print(f"copy-to-moon gap {gap:.0f}px vs {MIN_GAP}px minimum"
          f" — {'ok' if gap >= MIN_GAP else 'COLLIDES'}")
    print(f"{OUT}  {out.size[0]}x{out.size[1]}  {OUT.stat().st_size / 1024:.0f} KiB")


if __name__ == "__main__":
    main()
