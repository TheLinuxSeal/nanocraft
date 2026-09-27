#!/usr/bin/env python3
"""
Builds block-style chest textures from the chest entity textures in a Minecraft client jar.

Vanilla draws chests as entities with one 64x64 texture, so there are no per-face block textures
to point a .def at. This cuts that texture into 16x16 face textures that match NanoCraft's chest
shape (14x14 wide, 14 tall, starting 1px in from each side):

    <name>_top  <name>_bottom  <name>_front  <name>_front_half  <name>_back  <name>_side

(<name>_front has the latch; <name>_front_half is for double chest halves, which have no own latch.)

Usage:  python3 tools/chest_textures.py path/to/client.jar [output dir]
Output defaults to src/resources/assets/texture/block. Needs Pillow (pip install pillow).
"""
import io
import os
import sys
import zipfile

from PIL import Image

# block texture name -> chest entity texture in the jar
CHESTS = {
    "chest": "normal",
    "trapped_chest": "trapped",
    "ender_chest": "ender",
    "copper_chest": "copper",
    "exposed_copper_chest": "copper_exposed",
    "weathered_copper_chest": "copper_weathered",
    "oxidized_copper_chest": "copper_oxidized",
}

# Regions of the 64x64 entity texture (x, y, width, height). The side strips are stored upside
# down (the entity model is drawn flipped), so they're flipped back when used.
LID_TOP = (28, 0, 14, 14)
BASE_BOTTOM = (14, 19, 14, 14)
LID_ROW, LID_H = 14, 5
BASE_ROW, BASE_H = 33, 10
SIDE_X = {"side": 0, "back": 14, "front": 42}
LATCH = (1, 1, 2, 4)


def crop(im, x, y, w, h):
    return im.crop((x, y, x + w, y + h))


def side(im, x0):
    """14x14 side view: lid (5 rows) over the base, which the lid overlaps by 1 row."""
    lid = crop(im, x0, LID_ROW, 14, LID_H).transpose(Image.FLIP_TOP_BOTTOM)
    base = crop(im, x0, BASE_ROW, 14, BASE_H).transpose(Image.FLIP_TOP_BOTTOM)
    out = Image.new("RGBA", (14, 14))
    out.paste(base.crop((0, 1, 14, BASE_H)), (0, LID_H))
    out.paste(lid, (0, 0))
    return out


def to_block(face, x, y):
    """Places a face into a 16x16 texture at (x, y), extending its edges into the unused border
    so edge sampling never hits transparent pixels (the solid pass discards those)."""
    out = Image.new("RGBA", (16, 16))
    w, h = face.size
    for ty in range(16):
        for tx in range(16):
            sx = min(max(tx - x, 0), w - 1)
            sy = min(max(ty - y, 0), h - 1)
            out.putpixel((tx, ty), face.getpixel((sx, sy)))
    return out


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    jar = sys.argv[1]
    out_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", "src", "resources", "assets", "texture", "block")
    os.makedirs(out_dir, exist_ok=True)

    with zipfile.ZipFile(jar) as z:
        for name, entity in CHESTS.items():
            im = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/entity/chest/{entity}.png"))).convert("RGBA")

            faces = {
                "top": to_block(crop(im, *LID_TOP), 1, 1),
                "bottom": to_block(crop(im, *BASE_BOTTOM), 1, 1),
                "back": to_block(side(im, SIDE_X["back"]), 1, 2),
                "side": to_block(side(im, SIDE_X["side"]), 1, 2),
                "front_half": to_block(side(im, SIDE_X["front"]), 1, 2),
            }
            # the latch sits in the middle of the front, block y 7..11 -> rows 3..6 of the side view
            front = side(im, SIDE_X["front"])
            front.paste(crop(im, *LATCH).transpose(Image.FLIP_TOP_BOTTOM), (6, 3))
            faces["front"] = to_block(front, 1, 2)

            for face, img in faces.items():
                img.save(os.path.join(out_dir, f"{name}_{face}.png"))
            print(f"wrote {name}_*.png")


if __name__ == "__main__":
    main()
