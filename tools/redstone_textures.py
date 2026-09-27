#!/usr/bin/env python3
"""
Builds pre-colored redstone dust textures from the grey vanilla ones in a Minecraft client jar.

Vanilla tints redstone dust by its power level (0-15) at render time. NanoCraft doesn't tint, so this
writes one colored copy per power level, which vanilla.def refers to as <texture>_{power}:

    redstone_dust_dot_0 .. redstone_dust_dot_15
    redstone_dust_line0_0 .. redstone_dust_line0_15
    redstone_dust_line1_0 .. redstone_dust_line1_15

Usage:  python3 tools/redstone_textures.py path/to/client.jar [output dir]
Output defaults to src/resources/assets/texture/block. Needs Pillow (pip install pillow).
"""
import io
import os
import sys
import zipfile

from PIL import Image

TINTED = ["redstone_dust_dot", "redstone_dust_line0", "redstone_dust_line1"]


def power_color(power):
    """Vanilla's redstone wire color for a power level (RedStoneWireBlock)."""
    f = power / 15.0
    r = f * 0.6 + (0.4 if f > 0 else 0.3)
    g = min(max(f * f * 0.7 - 0.5, 0.0), 1.0)
    b = min(max(f * f * 0.6 - 0.7, 0.0), 1.0)
    return r, g, b


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    out_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", "src", "resources", "assets", "texture", "block")
    os.makedirs(out_dir, exist_ok=True)

    with zipfile.ZipFile(sys.argv[1]) as z:
        for name in TINTED:
            grey = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")
            for power in range(16):
                r, g, b = power_color(power)
                img = Image.new("RGBA", grey.size)
                img.putdata([(round(p[0] * r), round(p[1] * g), round(p[2] * b), p[3]) for p in grey.getdata()])
                img.save(os.path.join(out_dir, f"{name}_{power}.png"))
            print(f"wrote {name}_0..15.png")


if __name__ == "__main__":
    main()
