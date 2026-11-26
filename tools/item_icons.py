#!/usr/bin/env python3
import math
import os
import sys

from PIL import Image

N = 16


class Sprite:
    def __init__(self):
        self.region = {}
        self.colour = {}

    def add(self, name, pixels):
        for p in pixels:
            if 0 <= p[0] < N and 0 <= p[1] < N:
                self.region[(int(p[0]), int(p[1]))] = name

    def paint(self, pixels, c):
        for p in pixels:
            if 0 <= p[0] < N and 0 <= p[1] < N:
                self.colour[(int(p[0]), int(p[1]))] = c

    def render(self, ramps, outline=True):
        img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
        for (x, y), name in self.region.items():
            dark, mid, light = ramps[name]
            up_left_out = self.region.get((x - 1, y)) != name or self.region.get((x, y - 1)) != name
            down_right_out = self.region.get((x + 1, y)) != name or self.region.get((x, y + 1)) != name
            c = light if up_left_out and not down_right_out else dark if down_right_out and not up_left_out else mid
            img.putpixel((x, y), c + (255,))
        if outline:
            for (x, y), name in list(self.region.items()):
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= nx < N and 0 <= ny < N and (nx, ny) not in self.region:
                        dark = ramps[name][0]
                        img.putpixel((nx, ny), tuple(int(v * 0.55) for v in dark) + (255,))
        for (x, y), c in self.colour.items():
            img.putpixel((x, y), c + (255,))
        return img


def disc(cx, cy, r):
    return [(x, y) for y in range(N) for x in range(N) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r]


def line(x0, y0, x1, y1):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    return [(round(x0 + (x1 - x0) * i / max(1, n - 1)), round(y0 + (y1 - y0) * i / max(1, n - 1))) for i in range(n)]


def thick(points, w=2):
    out = set()
    for x, y in points:
        for dx in range(w):
            out.add((x + dx, y))
    return out


def poly(pts):
    out = []
    for y in range(N):
        for x in range(N):
            px, py, inside = x + 0.5, y + 0.5, False
            for i in range(len(pts)):
                (x1, y1), (x2, y2) = pts[i], pts[i - 1]
                if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1:
                    inside = not inside
            if inside:
                out.append((x, y))
    return out


def handle(s, x1=10, y1=5):
    s.add('wood', line(1, 14, x1, y1))
    s.add('wood', [(x + 1, y) for x, y in line(1, 14, x1, y1)][:-1])


WOOD = ((58, 34, 20), (96, 62, 36), (128, 88, 52))
ICONS = {}


def icon(fn):
    ICONS[fn.__name__] = fn
    return fn


@icon
def blood_scythe():
    s = Sprite()
    s.add('wood', line(2, 14, 11, 5))
    s.add('wood', [(x + 1, y) for x, y in line(2, 14, 11, 5)][:-1])
    blade = set()
    for i in range(14):
        t = i / 13
        x = 12 - 11 * t
        y = 4 - 3.2 * math.sin(math.pi * t * 0.9) + 3.5 * t * t
        blade.update({(round(x), round(y)), (round(x), round(y) + 1)})
        if t < 0.6:
            blade.add((round(x), round(y) + 2))
    s.add('blade', blade)
    s.add('gold', [(11, 4), (12, 5), (11, 5)])
    s.paint([(round(12 - 11 * t), round(4 - 3.2 * math.sin(math.pi * t * 0.9) + 3.5 * t * t)) for t in (0.3, 0.45, 0.6)],
            (255, 160, 160))
    return s.render({'wood': ((44, 22, 22), (74, 38, 34), (104, 58, 50)), 'blade': ((120, 10, 20), (186, 24, 36), (232, 70, 70)),
                     'gold': ((150, 100, 20), (210, 160, 40), (250, 220, 110))})


@icon
def starfall_pickaxe():
    s = Sprite()
    handle(s, 10, 5)
    head = set()
    for i in range(15):
        t = (i - 7) / 7
        x = 10.5 + t * 4.6 + (1 - t * t) * 1.6
        y = 5.0 + t * 4.6 - (1 - t * t) * 1.6
        head.update({(round(x), round(y)), (round(x) - 1, round(y)), (round(x), round(y) + 1)})
    s.add('head', head)
    s.paint([(12, 3), (11, 3), (13, 3), (12, 2), (12, 4)], (255, 240, 150))
    s.paint([(12, 3)], (255, 255, 230))
    return s.render({'wood': WOOD, 'head': ((60, 26, 110), (100, 50, 170), (150, 100, 220))})


@icon
def warlord_axe():
    s = Sprite()
    handle(s, 11, 4)
    s.add('iron', poly([(8, 2), (12, 0.5), (15, 3.5), (15.5, 8), (12, 9), (10, 6)]))
    s.add('edge', [(15, 4), (15, 5), (15, 6), (15, 7), (14, 8), (14, 3)])
    s.add('iron', [(11, 3), (12, 2)])
    return s.render({'wood': ((40, 26, 20), (70, 46, 32), (98, 68, 46)), 'iron': ((36, 36, 44), (62, 62, 72), (98, 98, 110)),
                     'edge': ((140, 20, 24), (190, 34, 36), (230, 80, 70))})


@icon
def captains_cutlass():
    s = Sprite()
    s.add('grip', line(1, 14, 3, 12))
    s.add('grip', line(2, 14, 4, 12))
    s.add('gold', [(4, 11), (5, 12), (3, 10), (6, 13), (2, 11), (2, 12), (1, 13), (5, 10)])
    blade = set()
    for i in range(12):
        t = i / 11
        x = 5 + 9.5 * t
        y = 10 - 9 * t + math.sin(math.pi * t) * 1.6
        blade.update({(round(x), round(y)), (round(x) + 1, round(y)), (round(x), round(y) + 1)})
    s.add('steel', blade)
    s.paint([(round(5 + 9.5 * t) + 1, round(10 - 9 * t + math.sin(math.pi * t) * 1.6)) for t in (0.3, 0.5, 0.7)], (240, 252, 255))
    return s.render({'grip': ((40, 24, 16), (70, 44, 28), (96, 64, 40)), 'gold': ((150, 100, 20), (214, 164, 44), (252, 224, 120)),
                     'steel': ((90, 120, 130), (160, 196, 206), (214, 236, 242))})


@icon
def colossus_hammer():
    s = Sprite()
    handle(s, 8, 8)
    s.add('rock', poly([(6, 1), (12, 1), (15, 3), (15, 8), (12, 10), (5, 8), (5, 3)]))
    s.paint([(8, 3), (9, 3), (9, 4), (10, 4), (11, 4), (11, 5), (12, 5), (12, 6), (7, 6), (8, 6), (8, 7)], (255, 112, 20))
    s.paint([(10, 4), (11, 5)], (255, 214, 70))
    return s.render({'wood': ((40, 26, 20), (70, 46, 32), (98, 68, 46)), 'rock': ((22, 20, 26), (44, 40, 48), (70, 64, 74))})


@icon
def bloody_tear():
    s = Sprite()
    drop = [(x, y) for y in range(N) for x in range(N)
            if (y >= 8 and (x - 7.5) ** 2 + (y - 9.5) ** 2 <= 16) or (2 <= y < 8 and abs(x - 7.5) <= (y - 1.5) * 0.62)]
    s.add('blood', drop)
    s.paint([(6, 8), (6, 9), (5, 10)], (255, 150, 150))
    return s.render({'blood': ((96, 4, 14), (160, 14, 28), (210, 40, 50))})


@icon
def magma_heart():
    s = Sprite()
    rows = ['...xxx...xxx....', '..xxxxx.xxxxx...', '.xxxxxxxxxxxxx..', '.xxxxxxxxxxxxx..', '.xxxxxxxxxxxxx..',
            '.xxxxxxxxxxxxx..', '..xxxxxxxxxxx...', '...xxxxxxxxx....', '....xxxxxxx.....', '.....xxxxx......',
            '......xxx.......', '.......x........']
    heart = [(x, y + 2) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch == 'x']
    s.add('rock', heart)
    s.paint([(7, 4), (7, 5), (8, 6), (7, 7), (7, 8), (6, 9), (7, 10), (5, 6), (4, 6), (9, 7), (10, 7), (11, 6)], (255, 112, 20))
    s.paint([(7, 7), (8, 6)], (255, 214, 70))
    s.paint([(3, 4), (4, 4)], (120, 96, 90))
    return s.render({'rock': ((30, 22, 24), (56, 40, 40), (84, 62, 58))})


@icon
def unstable_pearl():
    s = Sprite()
    s.add('pearl', disc(7.5, 7.5, 6))
    swirl = [(round(7.5 + r * math.cos(a)), round(7.5 + r * math.sin(a))) for a, r in
             [(k * 0.55, 0.6 + k * 0.33) for k in range(14)]]
    s.paint(swirl, (90, 230, 210))
    s.paint([(7, 7), (8, 8)], (210, 255, 248))
    s.paint([(5, 4), (4, 5)], (230, 210, 255))
    return s.render({'pearl': ((40, 14, 80), (78, 36, 140), (120, 70, 190))})


@icon
def event_seal():
    s = Sprite()
    wax = [(x, y) for y in range(N) for x in range(N)
           if math.hypot(x - 7.5, y - 7.5) <= 6.3 + 0.7 * math.cos(6 * math.atan2(y - 7.5, x - 7.5))]
    s.add('wax', wax)
    star = [(7, 4), (8, 4), (7, 5), (8, 5), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (11, 7),
            (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (6, 9), (7, 9), (8, 9), (9, 9), (5, 10), (6, 10),
            (9, 10), (10, 10), (7, 6), (8, 6)]
    s.paint(star, (240, 196, 60))
    s.paint([(7, 6), (6, 7)], (255, 236, 150))
    return s.render({'wax': ((110, 12, 20), (170, 24, 34), (214, 60, 60))})


@icon
def golden_seed():
    s = Sprite()
    s.add('gold', [(x, y) for y in range(N) for x in range(N) if ((x - 7.5) / 4.6) ** 2 + ((y - 9.5) / 5.4) ** 2 <= 1])
    s.add('leaf', [(8, 3), (8, 2), (9, 2), (10, 1), (11, 1), (10, 2), (7, 2), (6, 1)])
    s.paint([(6, 7), (5, 8), (6, 8)], (255, 248, 200))
    return s.render({'gold': ((170, 110, 20), (226, 170, 40), (252, 222, 100)), 'leaf': ((40, 90, 24), (70, 136, 40), (110, 180, 60))})


@icon
def bottled_lightning():
    s = Sprite()
    s.add('glass', [(x, y) for y in range(N) for x in range(N) if (x - 7.5) ** 2 + (y - 10) ** 2 <= 25])
    s.add('glass', [(x, y) for y in range(3, 6) for x in range(6, 10)])
    s.add('cork', [(x, y) for y in range(1, 3) for x in range(6, 10)])
    bolt = [(9, 6), (8, 7), (7, 8), (8, 8), (9, 8), (8, 9), (7, 10), (6, 11), (7, 11), (8, 11), (7, 12), (6, 13)]
    inside = [(x, y) for y in range(7, 15) for x in range(4, 12) if (x - 7.5) ** 2 + (y - 10) ** 2 <= 12]
    s.paint(inside, (30, 40, 90))
    s.paint(bolt, (255, 230, 80))
    s.paint([(4, 8), (4, 9)], (230, 240, 255))
    return s.render({'glass': ((90, 110, 140), (150, 170, 200), (210, 225, 240)), 'cork': ((100, 66, 36), (140, 96, 56), (176, 128, 80))})


@icon
def meteorite_fragment():
    s = Sprite()
    s.add('rock', poly([(3, 6), (7, 2.5), (12, 3.5), (14, 8), (12.5, 13), (6, 14), (2.5, 10.5)]))
    s.paint([(6, 6), (7, 7), (8, 7), (9, 8), (10, 8), (7, 10), (6, 11), (11, 11)], (255, 120, 30))
    s.paint([(8, 7)], (255, 220, 90))
    s.paint([(10, 5), (5, 9), (11, 10)], (200, 190, 180))
    return s.render({'rock': ((34, 30, 34), (64, 58, 60), (96, 88, 88))})


@icon
def eclipse_shard():
    s = Sprite()
    s.add('shard', poly([(5, 14.5), (2.5, 9), (6, 3), (10.5, 1), (13.5, 5), (11, 11)]))
    s.paint([(8, 4), (7, 5), (9, 4)], (210, 170, 255))
    s.paint([(9, 7), (8, 8), (7, 9), (8, 9)], (255, 200, 80))
    s.paint([(8, 8)], (255, 240, 170))
    return s.render({'shard': ((22, 10, 40), (52, 24, 86), (96, 56, 150))})


@icon
def frostfang():
    s = Sprite()
    s.add('grip', line(1, 14, 3, 12))
    s.add('grip', line(2, 14, 4, 12))
    s.add('guard', [(2, 10), (3, 11), (5, 12), (6, 13), (4, 11), (5, 11)])
    blade = set()
    for x, y in line(5, 10, 14, 1):
        blade.update({(x, y), (x + 1, y), (x, y + 1)})
    s.add('ice', blade)
    s.paint([(7, 8), (9, 6), (11, 4), (13, 2)], (245, 252, 255))
    s.paint([(8, 9), (12, 5)], (90, 160, 210))
    return s.render({'grip': ((30, 40, 70), (50, 66, 108), (74, 94, 140)), 'guard': ((90, 150, 200), (140, 200, 240), (200, 236, 255)),
                     'ice': ((80, 150, 200), (150, 214, 244), (214, 244, 255))})


@icon
def warchiefs_cleaver():
    s = Sprite()
    handle(s, 11, 4)
    s.add('gold', poly([(8, 2), (12, 0.5), (15, 3.5), (15.5, 8), (12, 9), (10, 6)]))
    s.add('edge', [(15, 4), (15, 5), (15, 6), (15, 7), (14, 8), (14, 3)])
    s.add('gold', [(11, 3), (12, 2)])
    s.paint([(3, 12), (5, 10), (7, 8)], (150, 24, 28))
    s.paint([(10, 4), (11, 5)], (232, 222, 196))
    return s.render({'wood': ((26, 22, 24), (48, 40, 44), (76, 64, 68)), 'gold': ((150, 104, 24), (216, 164, 44), (252, 222, 110)),
                     'edge': ((190, 150, 40), (240, 200, 70), (255, 244, 170))})


@icon
def tidecallers_conch():
    s = Sprite()
    s.add('shell', disc(7.5, 8.0, 6))
    s.add('shell', [(12, 11), (13, 12), (13, 13), (12, 13), (14, 13)])
    swirl = [(round(7.5 + r * math.cos(a)), round(8.0 + r * math.sin(a)))
             for a, r in [(k * 0.5, 0.5 + k * 0.32) for k in range(16)]]
    s.paint(swirl, (226, 244, 238))
    s.paint([(7, 8), (8, 8), (8, 9)], (110, 250, 240))
    s.paint([(4, 4), (3, 5)], (255, 255, 255))
    s.paint([(11, 11), (12, 12)], (224, 150, 140))
    return s.render({'shell': ((30, 94, 104), (58, 156, 160), (118, 210, 200))})


def main():
    args = sys.argv[1:]
    preview = None
    if '--preview' in args:
        k = args.index('--preview')
        preview = args[k + 1]
        args = args[:k] + args[k + 2:]
    out = args[0] if args else 'src/main/resources/assets/worldomens/textures/item'
    os.makedirs(out, exist_ok=True)
    images = {}
    for name, fn in ICONS.items():
        images[name] = fn()
        images[name].save(os.path.join(out, name + '.png'))
    if preview:
        z = 10
        sheet = Image.new('RGBA', (len(images) * (N * z + 8), N * z), (44, 48, 56, 255))
        for i, (name, img) in enumerate(images.items()):
            sheet.paste(img.resize((N * z, N * z), Image.NEAREST), (i * (N * z + 8), 0), img.resize((N * z, N * z), Image.NEAREST))
        sheet.save(preview)
    print(len(images), 'icons')


if __name__ == '__main__':
    main()
