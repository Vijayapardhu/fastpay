"""
Generates Fastpay's launcher-icon assets from the master logo.

Run from the repo root:  python tools/generate_logo_assets.py

Why a script rather than hand-placed PNGs
----------------------------------------
The logo has to exist at three different sizes with three different
geometries (launcher foreground, themed-icon silhouette, in-app mark). Keeping
them as loose files means the next person who tweaks the master has no way to
know which are derived and which are authoritative. Regenerating is the only
way to guarantee they stay consistent.

Sizing rules (Android adaptive icons)
-------------------------------------
An adaptive icon is a 108x108dp canvas; the launcher masks it down to the
central 72x72dp. The mark therefore has to sit inside an inscribed circle of
72dp, not merely a 72x72 box - a circular mask would clip the corners of a
box-fitted wide mark. Because this mark is wider than it is tall (about
1.26:1), the diagonal, not the width, is the binding constraint, so the
fitter below sizes on the diagonal.
"""

import math
import os

from PIL import Image

SRC = os.environ.get(
    "FASTPAY_LOGO",
    os.path.join(os.path.expanduser("~"), "Downloads", "logo.png"),
)
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable-nodpi")

# Canvas of the adaptive foreground, in px. 432 == 108dp at xxxhdpi.
CANVAS = 512
# Guaranteed-visible diameter as a fraction of the canvas (72/108).
SAFE_FRACTION = 72.0 / 108.0
# A little breathing room inside the mask so the mark never touches the edge.
SAFE_SCALE = 0.96


def fit_into_safe_zone(mark: Image.Image, canvas: int, scale: float) -> Image.Image:
    """Centers `mark` on a transparent square canvas, sized to the safe zone."""
    canvas_img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))

    max_extent = canvas * SAFE_FRACTION * scale
    w, h = mark.size
    diagonal = math.hypot(w, h)
    if diagonal > max_extent:
        factor = max_extent / diagonal
        mark = mark.resize(
            (max(1, round(w * factor)), max(1, round(h * factor))),
            Image.LANCZOS,
        )

    canvas_img.alpha_composite(mark, ((canvas - mark.width) // 2, (canvas - mark.height) // 2))
    return canvas_img


def to_monochrome(mark: Image.Image) -> Image.Image:
    """Flattens the mark to a single-colour silhouette.

    Themed launcher icons recolour the drawable with a system tint, so only the
    shape survives - the orange-to-red gradient and the dark lower stroke would
    both be overwritten. Taking the alpha channel as a mask is therefore the
    faithful conversion, not a lossy approximation.
    """
    alpha = mark.getchannel("A")
    solid = Image.new("RGBA", mark.size, (0, 0, 0, 255))
    solid.putalpha(alpha)
    return solid


def main() -> None:
    if not os.path.isfile(SRC):
        raise SystemExit(f"master logo not found: {SRC}")

    master = Image.open(SRC).convert("RGBA")
    # Trust the alpha channel, not the canvas: the master carries generous
    # transparent padding that would otherwise shrink the mark needlessly.
    bbox = master.getbbox()
    if bbox is None:
        raise SystemExit("master logo is fully transparent")
    mark = master.crop(bbox)
    print(f"master {master.size} -> mark {mark.size} (aspect {mark.width / mark.height:.3f})")

    os.makedirs(OUT, exist_ok=True)
    outputs = {
        # Launcher foreground: gradient mark, safe-zone padded.
        "ic_fastpay_logo_foreground.webp": fit_into_safe_zone(mark, CANVAS, SAFE_SCALE),
        # Android 13+ themed icon: silhouette, same geometry so the two do not
        # jump size when the user switches their launcher to themed icons.
        "ic_fastpay_logo_mono.webp": fit_into_safe_zone(
            to_monochrome(mark), CANVAS, SAFE_SCALE
        ),
        # In-app mark (header tile, call overlay). Tight crop rather than
        # safe-zone padded, because there the image *is* the container.
        "ic_fastpay_logo_mark.webp": fit_into_safe_zone(mark, 256, 1.72),
    }

    for name, img in outputs.items():
        path = os.path.join(OUT, name)
        # lossless-ish: method=6 max effort, quality=90 keeps the gradient
        # banding-free. These are three small files; the saving matters more
        # than a few KB.
        img.save(path, "WEBP", quality=90, method=6)
        kb = os.path.getsize(path) / 1024
        print(f"  {name:34} {img.size[0]}x{img.size[1]}  {kb:6.1f} KB")


if __name__ == "__main__":
    main()