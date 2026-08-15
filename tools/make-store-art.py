#!/usr/bin/env python3
"""
Draws the Play Store artwork from the same numbers the app's icon uses.

Play needs a 512x512 icon and a 1024x500 feature graphic, and neither can be a
vector. Rather than exporting them by hand from a drawing program, where they
would immediately start drifting away from the real mark, both are drawn here
from the construction rules in DESIGN.md section 7:

  - the coin rests at the cradle's lowest point, touching it
  - the cradle is a stroke, the coin is a fill
  - the arc is shallow, its ends level with each other
  - nothing else is in frame, and it is flat

The coin's bottom edge and the cradle stroke's inner edge meet at the same
number. That is the rule, not a coincidence, and it is asserted below so this
script fails rather than quietly drawing a floating coin.

Run:  python3 tools/make-store-art.py
Out:  store/icon-512.png, store/feature-1024x500.png
"""

import os
from PIL import Image, ImageDraw, ImageFont

FIELD = (0x16, 0x12, 0x1C)
COPPER = (0xAE, 0x67, 0x38)
PAPER = (0xF2, 0xF0, 0xEC)

# Straight from ic_launcher_foreground.xml, on its 108 unit canvas.
CRADLE_LEFT_X, CRADLE_Y = 32.2, 55.3
CRADLE_RIGHT_X = 75.8
CRADLE_BOTTOM = 72.5
CRADLE_STROKE = 1.78
COIN_CX, COIN_CY, COIN_R = 54.0, 63.0, 8.58

# The construction rule, checked rather than trusted.
#
# The coin's bottom sits at 71.58 and the stroke's inner edge at 71.61, so they
# are 0.03 units apart on a 108 unit canvas: a seventh of a pixel at 512, which
# is below anything that can be drawn, let alone seen. The tolerance is stated
# in pixels at the largest size this is rendered, because "close enough" for a
# mark means "closer than one pixel can show".
GAP = abs((COIN_CY + COIN_R) - (CRADLE_BOTTOM - CRADLE_STROKE / 2))
assert GAP * (512 / 108.0) < 0.25, (
    f"the coin floats {GAP:.3f} units above the cradle, which is visible at 512"
)


def quadratic(p0, p1, p2, steps=200):
    """The two quadratics that make the cradle, flattened to points."""
    out = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        out.append(
            (
                u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
                u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1],
            )
        )
    return out


def cradle_path(steps=600):
    """The cradle's centerline, as points on the 108 unit canvas."""
    return (
        quadratic((CRADLE_LEFT_X, CRADLE_Y), (CRADLE_LEFT_X, CRADLE_BOTTOM), (COIN_CX, CRADLE_BOTTOM), steps)
        + quadratic((COIN_CX, CRADLE_BOTTOM), (CRADLE_RIGHT_X, CRADLE_BOTTOM), (CRADLE_RIGHT_X, CRADLE_Y), steps)
    )


# The mark's own bounding box, stroke included, on the 108 unit canvas. The
# icon's numbers place it inside the adaptive icon's safe zone, low and small,
# because a launcher mask can crop the edges. A store icon has no mask and no
# safe zone, so the mark is framed by its own extents instead of inheriting a
# position that exists for a different reason.
HALF = CRADLE_STROKE / 2
MARK_LEFT = CRADLE_LEFT_X - HALF
MARK_RIGHT = CRADLE_RIGHT_X + HALF
MARK_TOP = min(CRADLE_Y - HALF, COIN_CY - COIN_R)
MARK_BOTTOM = CRADLE_BOTTOM + HALF
MARK_W = MARK_RIGHT - MARK_LEFT
MARK_H = MARK_BOTTOM - MARK_TOP


def draw_mark(canvas, box_left, box_top, box_width, supersample=4):
    """
    Draws the mark onto `canvas`, its bounding box fitted to `box_width`.

    The stroke is stamped rather than drawn as a polyline. A polyline of six
    hundred short segments renders with a bumpy edge where the joins overlap,
    which on a mark of two shapes is the only thing anybody would see. Stamping
    a circle at every point gives a constant width and true round caps for free,
    which is what the vector's `strokeLineCap="round"` asks for anyway.
    """
    s = supersample
    scale = box_width / MARK_W
    size_w = int(round(box_width))
    size_h = int(round(MARK_H * scale))

    layer = Image.new("RGBA", (size_w * s, size_h * s), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    def px(x, y):
        return ((x - MARK_LEFT) * scale * s, (y - MARK_TOP) * scale * s)

    r = HALF * scale * s
    for x, y in cradle_path():
        cx, cy = px(x, y)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=COPPER)

    cx, cy = px(COIN_CX, COIN_CY)
    cr = COIN_R * scale * s
    d.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=COPPER)

    flat = layer.resize((size_w, size_h), Image.LANCZOS)
    canvas.paste(flat, (int(round(box_left)), int(round(box_top))), flat)
    return size_h


def store_icon(path, size=512):
    """
    512 square, and deliberately **not** rounded.

    Play applies its own mask. An icon that arrives pre-rounded gets rounded
    twice and ends up with a pale rim.
    """
    img = Image.new("RGB", (size, size), FIELD)
    # Just over half the width. Play shows this at many sizes and often behind a
    # circular mask, so the mark stays well inside the corners.
    box_width = size * 0.54
    height = MARK_H / MARK_W * box_width
    draw_mark(img, (size - box_width) / 2, (size - height) / 2, box_width)
    img.save(path)
    return path


def feature_graphic(path, width=1024, height=500):
    """
    The banner Play will not publish without.

    The mark, the name, and the line the app itself opens with. Nothing else: a
    feature graphic is shown at many sizes and is often cropped toward the
    middle, so anything clever in it is a thing that will be cut in half. For
    the same reason the whole lockup is centered as a group rather than set to
    the left, which is what makes a center crop still contain all of it.

    The tagline is set in the app's own italic serif, because that is the voice
    it is written in everywhere else.
    """
    img = Image.new("RGB", (width, height), FIELD)
    d = ImageDraw.Draw(img)

    sans = "app/src/main/res/font/instrument_sans_variable.ttf"
    serif = "app/src/main/res/font/instrument_serif_italic.ttf"
    fallbacks = (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf",
    )

    def face(preferred, points):
        for candidate in (preferred,) + fallbacks:
            if os.path.exists(candidate):
                return ImageFont.truetype(candidate, points)
        return ImageFont.load_default()

    name_font = face(sans, 96)
    tag_font = face(serif, 44)

    name = "Meedwell"
    tagline = "for people who buy their music"
    name_w = d.textlength(name, font=name_font)
    tag_w = d.textlength(tagline, font=tag_font)

    mark_width = 220
    mark_height = MARK_H / MARK_W * mark_width
    gap = 56
    text_w = max(name_w, tag_w)
    total = mark_width + gap + text_w

    left = (width - total) / 2
    draw_mark(img, left, (height - mark_height) / 2, mark_width)

    text_x = left + mark_width + gap
    d.text((text_x, height / 2 - 84), name, font=name_font, fill=PAPER)
    d.text((text_x + 4, height / 2 + 32), tagline, font=tag_font, fill=(0xA8, 0xA2, 0x9A))

    img.save(path)
    return path


if __name__ == "__main__":
    os.makedirs("store", exist_ok=True)
    print("wrote", store_icon("store/icon-512.png"))
    print("wrote", feature_graphic("store/feature-1024x500.png"))
