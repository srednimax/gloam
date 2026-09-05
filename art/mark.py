"""The mark: the single definition of this app's identity.

A disc with a crescent bitten out of it — the moon, which is the hour Gloam is for. Both
generators import this file (`make-launcher-icon.py` emits it as `VectorDrawable` path data,
`make-feature-graphic.py` rasterises it with PIL), so the launcher icon, the notification icon
and the listing graphic cannot drift into being different marks.

Traced from `art/concept.png` by `trace-mark.py` on 2026-09-05; provenance is recorded in
`art/README.md`. To replace it: draw or generate a new concept image, run
`python3 art/trace-mark.py <image>`, and paste the curves it prints over MARK and CRESCENT
below. Nothing else in the pipeline changes.

**Use art you have the right to ship.** An icon traced from an emoji font, a stock icon or someone
else's mark is a licence obligation at best and a rejected Play upload at worst. Art you generated
from your own prompt, or drew, is clean.

The geometry is two subpaths: the shape, and the hole punched out of it. Coordinates are in
arbitrary units around an origin at the mark's centre; callers fit them to their own canvas with
`fit()`. y grows downward, as it does in both SVG and PIL.
"""

import re
from dataclasses import dataclass

# The identity's colours. The mark is near-white, so the ground carries all the colour.
#
# The ground used to be one flat `primary` darkened for contrast, and it read as mud: an amber hue
# at a dark *tone* is brown, and a large flat field of it is brown at its least flattering. The fix
# is not a different hue — the amber is the shade's own warmth and earns its place — but to stop
# spending it as a flat field. So the ground is the app's night with the amber banked low as
# *light*, which is the same horizon the feature graphic draws and the thing the app is actually
# for: a moon over the last of the light.
SURFACE = (0xFF, 0xF8, 0xEF)  # the light scheme's `surface` — the mark itself
GROUND_NIGHT = (0x15, 0x13, 0x0E)  # the dark scheme's `background`
GROUND_GLOW = (0xC8, 0x7A, 0x2E)  # `primary`, at the tone that reads as a light source

# Where the glow sits on the 108dp adaptive canvas, as fractions of it. The centre is *below* the
# bottom edge on purpose: only the top of the light is on the tile, which is what makes it a horizon
# rather than a lamp. Circular rather than elliptical, because a VectorDrawable radial gradient has
# one radius — see ground() for why that matters.
GLOW_AT = (0.5, 1.02)
GLOW_RADIUS = 0.85
GLOW_FALLOFF = 1.5  # >1 keeps the bright core small and the spill long
GLOW_STRENGTH = 0.95


def ground(offset):
    """The ground's colour at `offset` of the way from the glow's centre to its edge, as (r, g, b).

    **This is the single source both renderings read.** The launcher icon's back layer is a
    VectorDrawable radial gradient and the flat mipmaps are drawn by PIL, and those are two
    different renderers that would otherwise each need their own copy of the curve above. Instead
    the generator samples this function for the gradient's colour stops *and* calls it per pixel for
    the raster, so the vector and the bitmap cannot drift apart.
    """
    t = max(0.0, 1.0 - offset) ** GLOW_FALLOFF * GLOW_STRENGTH
    return tuple(round(n + (g - n) * t) for n, g in zip(GROUND_NIGHT, GROUND_GLOW))


@dataclass(frozen=True)
class Subpath:
    """One closed contour, as cubic path data.

    `d` is always wound CLOCKWISE on screen (positive shoelace area with y downward). A hole is
    stored the same way and reversed at read time by `nodes()`, so it is subtracted by the non-zero
    fill rule that both SVG and VectorDrawable use by default. That is what makes the hole a real
    hole rather than a shape painted in the background colour — and it is why it survives the
    monochrome layer, where Android tints everything one flat colour and a painted hole would vanish.
    """

    d: str
    hole: bool = False


# The moon's disc. One outer contour, near-circular but traced rather than drawn, so it keeps
# the concept's slight irregularity instead of becoming a perfect PIL ellipse.
MARK = (
    "M-14.37,-135.39"
        "C8.23,-136.22 30.81,-133.48 51.43,-123.55"
        "C67.41,-115.84 82.16,-105.6 93.96,-92.23"
        "C103.12,-81.85 110.68,-70.75 116.52,-58.19"
        "C140,-7.61 130.49,52.93 92.33,93.82"
        "C81.1,105.87 67.75,115.61 52.94,122.79"
        "C37.13,130.45 19.11,135.27 1.51,135.75"
        "C-15.99,136.22 -33.42,133.52 -49.92,127.4"
        "C-65.28,121.7 -79.23,112.98 -91.52,102.2"
        "C-100.74,94.11 -109.23,84.59 -115.72,74.16"
        "C-127.71,54.9 -135.74,32.67 -137.09,9.88"
        "C-140,-39.54 -116.96,-87.81 -75.22,-114.91"
        "C-56.61,-127 -36.14,-132.65 -14.37,-135.39"
        "Z"
)

# The night side, bitten out of the disc. A real hole in the path — see Subpath.
CRESCENT = (
    "M18.68,-81.64"
        "C6.91,-73.19 -2.2,-65.23 -9.19,-52.14"
        "C-27.48,-17.89 -13.58,23.7 18.15,44.18"
        "C29,51.18 41.68,54.44 54.45,55.46"
        "C65.25,56.31 61.19,52.73 64.48,56.01"
        "C56.95,63.93 49.56,69.69 39.32,74.47"
        "C29.12,79.25 18.04,81.71 6.8,82.14"
        "C-28.76,83.51 -60.97,60.73 -73.68,28.03"
        "C-78.36,16 -79.54,2.2 -78.22,-10.55"
        "C-77.17,-20.7 -73.83,-32.64 -68.35,-41.55"
        "C-56.7,-60.51 -39.43,-75.66 -17.4,-81.25"
        "C-11.84,-82.66 -6.09,-83.2 -0.4,-83.91"
        "C4.81,-84.55 9.17,-84.3 14.37,-83.59"
        "C18.33,-83.05 17.64,-83.75 18.68,-81.64"
        "Z"
)

PARTS = (
    Subpath(MARK),
    Subpath(CRESCENT, hole=True),
)

_NUM = re.compile(r"-?\d*\.?\d+(?:e-?\d+)?")


def _points(d):
    """Path data -> the flat control-point list: 1 start point plus 3 per cubic segment."""
    pts = []
    for chunk in re.finditer(r"[MC]([^MCZz]*)", d):
        nums = [float(v) for v in _NUM.findall(chunk.group(1))]
        pts += list(zip(nums[0::2], nums[1::2]))
    return pts


def nodes(e, tf=lambda p: p):
    """Control points, caller-transformed, wound for fill or hole."""
    pts = _points(e.d)
    if e.hole:
        pts.reverse()
    return [tf(p) for p in pts]


def path_data(e, tf=lambda p: p, precision=2):
    """SVG/VectorDrawable path data for one subpath."""
    n = nodes(e, tf)

    def f(p):
        return f"{round(p[0], precision):g},{round(p[1], precision):g}"

    out = [f"M{f(n[0])}"]
    for i in range(1, len(n), 3):
        out.append("C" + " ".join(f(n[i + j]) for j in range(3)))
    return "".join(out) + "Z"


def outline(e, tf=lambda p: p, steps=32):
    """The same subpath flattened to a polygon, for renderers that can't draw curves."""
    n = nodes(e, tf)
    pts = []
    for i in range(1, len(n), 3):
        (x0, y0), (x1, y1), (x2, y2), (x3, y3) = n[i - 1], n[i], n[i + 1], n[i + 2]
        for s in range(steps):
            t = s / steps
            u = 1 - t
            pts.append((
                u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3,
            ))
    return pts


def bounds(parts=PARTS):
    """Bounding box of the silhouette. Holes are ignored — a hole cannot extend the mark."""
    xs, ys = [], []
    for e in parts:
        if e.hole:
            continue
        for x, y in outline(e):
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(canvas, art_size, parts=PARTS):
    """A transform placing the mark centred on a square canvas at a given longest-side size.

    Returns a function mapping mark coordinates to canvas coordinates.
    """
    x0, y0, x1, y1 = bounds(parts)
    scale = art_size / max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return lambda p: (canvas / 2 + (p[0] - cx) * scale, canvas / 2 + (p[1] - cy) * scale)
