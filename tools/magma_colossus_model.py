"""Builds a vanilla-style Magma Colossus: a few boxes with a 256x256 pixel texture (2 texels per model unit,
which at the creature's scale of 2 is vanilla's 16 texels per block). Front is north (-z).

    python3 tools/magma_colossus_model.py src/main/resources/assets/worldevents/models/item/magma_colossus.json src/main/resources/assets/worldevents/textures/item/magma_colossus.png
"""
import json
import random
import sys

from PIL import Image

TEX = 256
TPU = 2  # texels per model unit

ROCK = [(28, 25, 31), (36, 32, 39), (45, 40, 48), (55, 49, 58)]
ROCK_TOP = [(46, 41, 49), (57, 51, 60), (68, 61, 71)]
LAVA_EDGE = (150, 38, 8)
LAVA = (255, 112, 20)
LAVA_HOT = (255, 214, 70)

# name: from, to, kind
BOXES = [
    ('hind_leg_l', (2, -3, 8), (7, 8, 14), 'rock'),
    ('hind_leg_r', (9, -3, 8), (14, 8, 14), 'rock'),
    ('body', (1, 6, 1), (15, 15, 14), 'rock'),
    ('chest', (-1, 9, -5), (17, 21, 5), 'rock'),
    ('core', (5, 10, -5.5), (11, 14, -5), 'core'),
    ('head', (4.5, 14, -10), (11.5, 21, -4), 'head'),
    ('brow', (4, 19, -10.5), (12, 21.5, -6), 'rock'),
    ('arm_l', (-5, 1, -6), (0, 19, 0), 'rock'),
    ('arm_r', (16, 1, -6), (21, 19, 0), 'rock'),
    ('fist_l', (-6, -3, -8), (1, 3, 1), 'rock'),
    ('fist_r', (15, -3, -8), (22, 3, 1), 'rock'),
    ('shoulder_l', (-6, 17, -7), (2, 23, 2), 'rock'),
    ('shoulder_r', (14, 17, -7), (22, 23, 2), 'rock'),
    ('spike_1', (5, 21, -2), (10, 25, 2), 'spike'),
    ('spike_2', (7, 15, 4), (11, 19, 8), 'spike'),
    ('spike_3', (4, 15, 9), (8, 18, 13), 'spike'),
]

FACES = {
    # face: (width axis, height axis) sizes from a box size (dx, dy, dz)
    'north': lambda d: (d[0], d[1]), 'south': lambda d: (d[0], d[1]),
    'east': lambda d: (d[2], d[1]), 'west': lambda d: (d[2], d[1]),
    'up': lambda d: (d[0], d[2]), 'down': lambda d: (d[0], d[2]),
}


class Packer:
    def __init__(self):
        self.x = self.y = self.row = 0

    def place(self, w, h):
        if self.x + w > TEX:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > TEX:
            raise SystemExit('texture full')
        at = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return at


def rock_face(img, x0, y0, w, h, rng, top=False, cracks=1):
    pal = ROCK_TOP if top else ROCK
    for y in range(h):
        for x in range(w):
            r = rng.random()
            c = pal[1] if r < 0.55 else pal[0] if r < 0.75 else pal[2] if r < 0.95 else pal[-1]
            img.putpixel((x0 + x, y0 + y), c)
    # Darker rim gives the blocky outline vanilla mobs have.
    for x in range(w):
        img.putpixel((x0 + x, y0 + h - 1), pal[0])
    for y in range(h):
        img.putpixel((x0, y0 + y), pal[0])
    for _ in range(cracks):
        crack(img, x0, y0, w, h, rng)


def crack(img, x0, y0, w, h, rng):
    if w < 4 or h < 4:
        return
    vertical = rng.random() < 0.6 if h >= w else rng.random() < 0.3
    length = int((h if vertical else w) * rng.uniform(0.5, 0.95))
    x = rng.randrange(1, w - 1)
    y = rng.randrange(1, h - 1)
    if vertical:
        y = rng.randrange(0, max(1, h - length))
    else:
        x = rng.randrange(0, max(1, w - length))
    pts = []
    for _ in range(length):
        pts.append((x, y))
        if vertical:
            y += 1
            if rng.random() < 0.35:
                x = min(w - 2, max(1, x + rng.choice((-1, 1))))
        else:
            x += 1
            if rng.random() < 0.35:
                y = min(h - 2, max(1, y + rng.choice((-1, 1))))
        if not (0 <= x < w and 0 <= y < h):
            break
    for px, py in pts:
        for nx, ny in ((px + 1, py), (px - 1, py), (px, py + 1), (px, py - 1)):
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in pts:
                img.putpixel((x0 + nx, y0 + ny), LAVA_EDGE)
    for i, (px, py) in enumerate(pts):
        img.putpixel((x0 + px, y0 + py), LAVA_HOT if 0 < i < len(pts) - 1 and rng.random() < 0.25 else LAVA)


def core_face(img, x0, y0, w, h, rng):
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            d = max(abs(x - cx) / (w / 2), abs(y - cy) / (h / 2))
            if d > 0.85:
                c = LAVA_EDGE
            elif d > 0.55:
                c = LAVA if rng.random() < 0.8 else LAVA_HOT
            else:
                c = LAVA_HOT if rng.random() < 0.8 else (255, 245, 190)
            img.putpixel((x0 + x, y0 + y), c)


def head_front(img, x0, y0, w, h, rng):
    rock_face(img, x0, y0, w, h, rng, cracks=0)
    # Eyes: two glowing slits under the brow; a lava mouth below.
    ey = int(h * 0.35)
    for ex in (2, w - 6):
        for x in range(4):
            img.putpixel((x0 + ex + x, y0 + ey), LAVA_HOT)
            img.putpixel((x0 + ex + x, y0 + ey + 1), LAVA)
    my = int(h * 0.72)
    for x in range(3, w - 3):
        img.putpixel((x0 + x, y0 + my), LAVA if x % 3 else LAVA_HOT)
        img.putpixel((x0 + x, y0 + my + 1), LAVA_EDGE)


def spike_face(img, x0, y0, w, h, rng, top=False):
    rock_face(img, x0, y0, w, h, rng, top=top, cracks=0)
    # Glowing tips: lava seeps out of the top of each rock.
    for x in range(w):
        for y in range(min(2, h)):
            if not top:
                img.putpixel((x0 + x, y0 + y), LAVA if rng.random() < 0.7 else LAVA_HOT)
    if top:
        for y in range(h):
            for x in range(w):
                if 1 <= x < w - 1 and 1 <= y < h - 1:
                    img.putpixel((x0 + x, y0 + y), LAVA if rng.random() < 0.6 else LAVA_HOT)


def main():
    img = Image.new('RGBA', (TEX, TEX), (0, 0, 0, 0))
    packer = Packer()
    elements = []
    for name, f, t, kind in BOXES:
        size = tuple(b - a for a, b in zip(f, t))
        faces = {}
        for face, dims in FACES.items():
            w, h = (max(1, round(v * TPU)) for v in dims(size))
            x0, y0 = packer.place(w, h)
            rng = random.Random(name + face)
            top = face == 'up'
            if kind == 'core':
                core_face(img, x0, y0, w, h, rng)
            elif kind == 'head' and face == 'north':
                head_front(img, x0, y0, w, h, rng)
            elif kind == 'spike':
                spike_face(img, x0, y0, w, h, rng, top=top)
            else:
                area = w * h
                rock_face(img, x0, y0, w, h, rng, top=top, cracks=0 if face == 'down' else 1 + area // 500)
            faces[face] = {'uv': [x0 * 16 / TEX, y0 * 16 / TEX, (x0 + w) * 16 / TEX, (y0 + h) * 16 / TEX],
                           'texture': '#skin'}
        elements.append({'name': name, 'from': list(f), 'to': list(t), 'faces': faces})
    model = {
        'textures': {'skin': 'worldevents:item/magma_colossus', 'particle': 'worldevents:item/magma_colossus'},
        'elements': elements,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.3125, 0.3125, 0.3125]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [0.5, 0.5, 0.5]},
        },
    }
    json.dump(model, open(sys.argv[1], 'w'), indent='\t')
    img.save(sys.argv[2])
    print(len(elements), 'elements; texture rows used up to', packer.y + packer.row)


if __name__ == '__main__':
    main()
