#!/usr/bin/env python3
import json
import os
import random
import shutil
import sys

from PIL import Image

STYLES = {
    'rock': ([(28, 25, 31), (36, 32, 39), (45, 40, 48), (55, 49, 58)], [0.2, 0.55, 0.2, 0.05]),
    'moon': ([(96, 8, 12), (128, 14, 18), (156, 24, 26), (184, 40, 36)], [0.15, 0.45, 0.3, 0.1]),
    'gold': ([(156, 104, 18), (204, 150, 30), (240, 196, 60), (255, 236, 140)], [0.15, 0.45, 0.3, 0.1]),
    'eyeball': ([(196, 182, 176), (222, 212, 206), (238, 232, 228), (250, 248, 246)], [0.15, 0.4, 0.35, 0.1]),
    'flesh': ([(110, 20, 28), (140, 34, 40), (168, 52, 56)], [0.3, 0.5, 0.2]),
    'burlap': ([(98, 66, 38), (122, 84, 50), (142, 100, 62), (160, 118, 76)], [0.15, 0.45, 0.3, 0.1]),
    'rope': ([(150, 120, 70), (184, 152, 96), (206, 178, 120)], [0.3, 0.5, 0.2]),
    'cloud': ([(92, 96, 108), (112, 116, 128), (134, 138, 150), (160, 164, 176)], [0.15, 0.4, 0.3, 0.15]),
    'cloud_dark': ([(52, 54, 64), (64, 66, 78), (76, 80, 92)], [0.3, 0.5, 0.2]),
    'bolt': ([(255, 214, 40), (255, 236, 110), (255, 252, 200)], [0.4, 0.4, 0.2]),
    'straw': ([(178, 136, 40), (206, 164, 56), (226, 190, 80), (240, 214, 120)], [0.2, 0.4, 0.3, 0.1]),
    'twine': ([(110, 76, 34), (134, 94, 44)], [0.5, 0.5]),
    'leaf': ([(58, 112, 30), (78, 142, 40), (106, 170, 56)], [0.3, 0.45, 0.25]),
    'robe': ([(26, 36, 92), (34, 48, 116), (44, 62, 138), (60, 80, 160)], [0.2, 0.45, 0.25, 0.1]),
    'hood': ([(22, 30, 78), (30, 42, 102), (40, 54, 124)], [0.3, 0.45, 0.25]),
    'skin': ([(150, 104, 72), (176, 126, 90), (196, 146, 108)], [0.25, 0.5, 0.25]),
    'leather': ([(78, 50, 30), (98, 64, 38), (118, 80, 48), (136, 96, 60)], [0.2, 0.45, 0.25, 0.1]),
    'bedroll': ([(120, 24, 28), (150, 34, 36), (176, 50, 46)], [0.3, 0.45, 0.25]),
    'clay': ([(140, 74, 44), (164, 92, 56), (186, 112, 70)], [0.3, 0.45, 0.25]),
    'armor': ([(30, 30, 36), (42, 42, 50), (56, 56, 66), (74, 74, 86)], [0.2, 0.45, 0.25, 0.1]),
    'armor_red': ([(90, 14, 18), (116, 22, 26), (140, 32, 34)], [0.3, 0.45, 0.25]),
    'bone': ([(176, 166, 140), (204, 194, 166), (226, 218, 194)], [0.3, 0.45, 0.25]),
    'coat': ([(14, 64, 70), (20, 82, 90), (28, 102, 110), (40, 122, 128)], [0.2, 0.45, 0.25, 0.1]),
    'ghost': ([(120, 214, 204), (150, 230, 220), (180, 242, 234), (214, 252, 246)], [0.2, 0.4, 0.3, 0.1]),
    'hat': ([(58, 38, 22), (74, 50, 30), (92, 64, 38)], [0.3, 0.45, 0.25]),
    'void': ([(16, 8, 26), (26, 14, 40), (38, 20, 56), (52, 28, 74)], [0.25, 0.4, 0.25, 0.1]),
    'claw': ([(196, 190, 210), (220, 216, 232), (240, 238, 248)], [0.3, 0.45, 0.25]),
    'core': ([(255, 112, 20), (255, 160, 40), (255, 214, 70)], [0.3, 0.3, 0.4]),
    'corona': ([(255, 150, 30), (255, 196, 60), (255, 232, 130), (255, 250, 210)], [0.25, 0.35, 0.3, 0.1]),
    'fur': ([(196, 206, 218), (214, 224, 234), (230, 238, 246), (244, 248, 252)], [0.2, 0.4, 0.3, 0.1]),
    'yeti_skin': ([(84, 118, 150), (100, 138, 172), (120, 158, 190)], [0.3, 0.45, 0.25]),
    'ice': ([(120, 190, 230), (150, 212, 244), (190, 234, 252), (230, 248, 255)], [0.2, 0.35, 0.3, 0.15]),
}

LAVA_EDGE = (150, 38, 8)
LAVA = (255, 112, 20)
LAVA_HOT = (255, 214, 70)
BLACK = (14, 10, 14)
WHITE = (250, 250, 250)


def pick(rng, style):
    colours, weights = STYLES[style]
    return rng.choices(colours, weights)[0]


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


class Face:

    def __init__(self, img, x0, y0, w, h, rng):
        self.img, self.x0, self.y0, self.w, self.h, self.rng = img, x0, y0, w, h, rng

    def put(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((self.x0 + x, self.y0 + y), c)

    def rect(self, x, y, w, h, c):
        for yy in range(int(y), int(y + h)):
            for xx in range(int(x), int(x + w)):
                self.put(xx, yy, c)


def pattern(face, style, side):
    rng, w, h = face.rng, face.w, face.h
    if style == 'rock':
        for _ in range(0 if side == 'down' else 1 + w * h // 500):
            crack(face)
    elif style == 'moon':
        for _ in range(max(1, w * h // 90)):
            cx, cy, r = rng.randrange(w), rng.randrange(h), rng.choice((1, 1, 2))
            for yy in range(-r, r + 1):
                for xx in range(-r, r + 1):
                    if xx * xx + yy * yy <= r * r:
                        face.put(cx + xx, cy + yy, (80, 6, 10))
            face.put(cx - 1, cy - r, (200, 60, 50))
    elif style == 'eyeball':
        for _ in range(max(1, w * h // 60)):
            vein(face, (176, 30, 34))
    elif style in ('burlap',):
        for y in range(0, h, 3):
            for x in range(w):
                if (x + y) % 4 == 0:
                    face.put(x, y, shade(STYLES[style][0][0], 0.9))
    elif style == 'straw':
        for x in range(0, w, 2):
            for y in range(h):
                if rng.random() < 0.35:
                    face.put(x, y, (150, 112, 30))
    elif style in ('robe', 'coat', 'hood') and side not in ('up', 'down'):
        for x in range(2, w - 1, 4):
            for y in range(h):
                face.put(x, y, shade(STYLES[style][0][0], 0.85))
    elif style in ('armor', 'armor_red'):
        for x in (1, w - 2):
            for y in (1, h - 2):
                face.put(x, y, (150, 150, 160))
        for x in range(w):
            face.put(x, 0, shade(STYLES[style][0][-1], 1.15))
    elif style == 'leather' and side not in ('up', 'down'):
        for y in range(3, h, 6):
            for x in range(w):
                face.put(x, y, (58, 36, 20))
    elif style == 'cloud':
        for _ in range(max(1, w * h // 40)):
            cx, cy = rng.randrange(w), rng.randrange(h)
            face.rect(cx, cy, 2, 1, (184, 188, 198))
    elif style == 'ghost':
        for y in range(h):
            for x in range(w):
                if rng.random() < 0.04:
                    face.put(x, y, WHITE)
    elif style == 'fur' and side not in ('up', 'down'):
        for x in range(0, w, 2):
            for y in range(h):
                if rng.random() < 0.3:
                    face.put(x, y, (176, 188, 204))
    elif style == 'ice':
        for i in range(min(w, h)):
            face.put(i, i, (250, 254, 255))
    elif style == 'void':
        for _ in range(max(1, w * h // 70)):
            face.put(rng.randrange(w), rng.randrange(h), (170, 90, 230))
    elif style == 'core':
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for y in range(h):
            for x in range(w):
                d = max(abs(x - cx) / (w / 2), abs(y - cy) / (h / 2))
                c = LAVA_EDGE if d > 0.85 else (LAVA if rng.random() < 0.8 else LAVA_HOT) if d > 0.55 else \
                    (LAVA_HOT if rng.random() < 0.8 else (255, 245, 190))
                face.put(x, y, c)


def crack(face):
    rng, w, h = face.rng, face.w, face.h
    if w < 4 or h < 4:
        return
    vertical = rng.random() < (0.6 if h >= w else 0.3)
    length = int((h if vertical else w) * rng.uniform(0.5, 0.95))
    x, y = rng.randrange(1, w - 1), rng.randrange(1, h - 1)
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
    for px, py in pts:
        for nx, ny in ((px + 1, py), (px - 1, py), (px, py + 1), (px, py - 1)):
            if (nx, ny) not in pts:
                face.put(nx, ny, LAVA_EDGE)
    for i, (px, py) in enumerate(pts):
        face.put(px, py, LAVA_HOT if 0 < i < len(pts) - 1 and rng.random() < 0.25 else LAVA)


def vein(face, colour):
    rng = face.rng
    x, y = rng.choice((0, face.w - 1)), rng.randrange(face.h)
    dx = 1 if x == 0 else -1
    for _ in range(rng.randrange(3, max(4, face.w // 2))):
        face.put(x, y, colour)
        x += dx
        if rng.random() < 0.5:
            y += rng.choice((-1, 1))


def deco(face, name):
    w, h, rng = face.w, face.h, face.rng
    mx = w / 2
    if name == 'colossus_face':
        ey = int(h * 0.35)
        for ex in (2, w - 6):
            face.rect(ex, ey, 4, 1, LAVA_HOT)
            face.rect(ex, ey + 1, 4, 1, LAVA)
        my = int(h * 0.72)
        for x in range(3, w - 3):
            face.put(x, my, LAVA if x % 3 else LAVA_HOT)
            face.put(x, my + 1, LAVA_EDGE)
    elif name == 'spike_top':
        for y in range(1, h - 1):
            for x in range(1, w - 1):
                face.put(x, y, LAVA if rng.random() < 0.6 else LAVA_HOT)
    elif name == 'spike_side':
        for x in range(w):
            for y in range(min(2, h)):
                face.put(x, y, LAVA if rng.random() < 0.7 else LAVA_HOT)
    elif name == 'angry_face':
        ey = int(h * 0.32)
        for i in range(5):
            face.put(mx - 7 + i, ey + i // 2, (255, 210, 60))
            face.put(mx - 7 + i, ey + 1 + i // 2, (255, 150, 30))
            face.put(mx + 6 - i, ey + i // 2, (255, 210, 60))
            face.put(mx + 6 - i, ey + 1 + i // 2, (255, 150, 30))
        my = int(h * 0.66)
        face.rect(mx - 6, my, 12, 3, (40, 2, 6))
        for x in range(int(mx - 6), int(mx + 6), 2):
            face.put(x, my, (236, 226, 200))
            face.put(x + 1, my + 2, (236, 226, 200))
    elif name == 'gems':
        for x in range(1, w - 1, 4):
            face.rect(x, max(0, h // 2 - 1), 2, 2, (200, 16, 30))
            face.put(x, max(0, h // 2 - 1), (255, 120, 120))
    elif name == 'iris':
        cx, cy = (w - 1) / 2, (h - 1) / 2
        r = min(w, h) * 0.38
        for y in range(h):
            for x in range(w):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d < r * 0.45:
                    face.put(x, y, BLACK)
                elif d < r * 0.8:
                    face.put(x, y, (190, 20, 26) if rng.random() < 0.7 else (230, 60, 50))
                elif d < r:
                    face.put(x, y, (120, 8, 14))
        face.rect(cx - r * 0.35, cy - r * 0.55, 2, 2, WHITE)
    elif name == 'mimic_face':
        ey = int(h * 0.28)
        for ex in (int(mx - 4), int(mx + 2)):
            face.rect(ex, ey, 2, 2, (255, 236, 90))
            face.put(ex + 1, ey + 1, BLACK)
        my = int(h * 0.55)
        face.rect(2, my, w - 4, 3, (30, 14, 8))
        for x in range(2, w - 2, 2):
            face.put(x, my, WHITE)
            face.put(x + 1, my + 2, WHITE)
    elif name == 'cloud_eyes':
        ey = int(h * 0.38)
        for ex in (int(mx - 5), int(mx + 2)):
            face.rect(ex, ey, 3, 2, WHITE)
            face.put(ex + (2 if ex < mx else 0), ey, (60, 64, 76))
        face.rect(mx - 3, ey + 4, 6, 1, (40, 42, 52))
    elif name == 'cute_face':
        ey = int(h * 0.4)
        for ex in (int(mx - 4), int(mx + 2)):
            face.rect(ex, ey, 2, 2, (40, 26, 10))
            face.put(ex + 1, ey, WHITE)
            face.put(ex - 1 if ex < mx else ex + 2, ey + 3, (230, 120, 90))
        face.rect(mx - 2, ey + 4, 4, 1, (110, 60, 20))
    elif name == 'merchant_face':
        face.rect(1, 1, w - 2, h - 2, (176, 126, 90))
        face.rect(2, 3, 1, 1, BLACK)
        face.rect(w - 3, 3, 1, 1, BLACK)
        face.rect(2, h - 3, w - 4, 2, (190, 190, 186))
        face.rect(mx - 1, 4, 2, 2, (150, 100, 70))
    elif name == 'visor':
        face.rect(1, int(h * 0.38), w - 2, 2, (10, 8, 10))
        face.put(mx - 2, int(h * 0.38), (255, 40, 30))
        face.put(mx + 1, int(h * 0.38), (255, 40, 30))
        face.rect(mx - 0.5, int(h * 0.38) + 2, 1, h, (20, 20, 24))
    elif name == 'emblem':
        cx, cy = int(mx), int(h * 0.4)
        for i in range(-3, 4):
            face.put(cx + i, cy + abs(i) // 2, (190, 26, 30))
            face.put(cx, cy + i, (190, 26, 30))
    elif name == 'buttons':
        for y in range(2, h - 1, 3):
            face.put(mx - 2, y, (240, 196, 60))
            face.put(mx + 1, y, (240, 196, 60))
        face.rect(mx - 0.5, 0, 1, h, (10, 44, 50))
    elif name == 'ghost_face':
        ey = int(h * 0.3)
        for ex in (int(mx - 4), int(mx + 1)):
            face.rect(ex, ey, 3, 3, (14, 60, 66))
            face.put(ex + 1, ey + 1, (230, 255, 250))
        face.rect(2, int(h * 0.65), w - 4, h, (90, 190, 182))
        face.rect(mx - 2, int(h * 0.62), 4, 1, (14, 60, 66))
    elif name == 'skull':
        cx = int(mx)
        face.rect(cx - 2, 1, 4, 3, WHITE)
        face.put(cx - 1, 2, BLACK)
        face.put(cx + 1, 2, BLACK)
        face.rect(cx - 1, 4, 2, 1, WHITE)
    elif name == 'trim':
        face.rect(0, 0, w, 1, (240, 196, 60))
    elif name == 'void_eyes':
        ey = int(h * 0.35)
        for ex in (int(mx - 3), int(mx + 2)):
            face.rect(ex, ey, 1, 3, (220, 140, 255))
            face.put(ex, ey + 1, WHITE)
    elif name == 'fade':
        for y in range(h):
            for x in range(w):
                if face.rng.random() < y / max(1, h) * 0.8:
                    face.put(x, y, (90, 180, 172))
    elif name == 'wraith_face':
        ey = int(h * 0.4)
        for i in range(4):
            face.put(mx - 6 + i, ey + i // 2, (240, 240, 255))
            face.put(mx + 5 - i, ey + i // 2, (240, 240, 255))
        face.put(mx - 4, ey + 2, (200, 140, 255))
        face.put(mx + 3, ey + 2, (200, 140, 255))
    elif name == 'yeti_face':
        ey = int(h * 0.3)
        for ex in (int(mx - 4), int(mx + 2)):
            face.rect(ex, ey, 2, 2, (20, 30, 50))
            face.put(ex + 1, ey, (150, 230, 255))
        my = int(h * 0.65)
        face.rect(mx - 4, my, 8, 2, (40, 20, 30))
        face.put(mx - 3, my + 2, WHITE)
        face.put(mx + 2, my + 2, WHITE)
        face.put(mx - 3, my + 3, WHITE)
        face.put(mx + 2, my + 3, WHITE)
    else:
        raise ValueError(name)


C = {}
CLASSIC = ('harvest_spirit', 'nomad_merchant', 'horde_warlord', 'spectral_captain')

C['magma_colossus'] = dict(tpu=2, boxes=[
    ((2, -3, 8), (7, 8, 14), 'rock', {}),
    ((9, -3, 8), (14, 8, 14), 'rock', {}),
    ((1, 6, 1), (15, 15, 14), 'rock', {}),
    ((-1, 9, -5), (17, 21, 5), 'rock', {}),
    ((5, 10, -5.5), (11, 14, -5), 'core', {}),
    ((4.5, 14, -10), (11.5, 21, -4), 'rock', {'north': 'colossus_face'}),
    ((4, 19, -10.5), (12, 21.5, -6), 'rock', {}),
    ((-5, 1, -6), (0, 19, 0), 'rock', {}),
    ((16, 1, -6), (21, 19, 0), 'rock', {}),
    ((-6, -3, -8), (1, 3, 1), 'rock', {}),
    ((15, -3, -8), (22, 3, 1), 'rock', {}),
    ((-6, 17, -7), (2, 23, 2), 'rock', {}),
    ((14, 17, -7), (22, 23, 2), 'rock', {}),
    ((5, 21, -2), (10, 25, 2), 'rock', {'up': 'spike_top', 'north': 'spike_side', 'south': 'spike_side', 'east': 'spike_side', 'west': 'spike_side'}),
    ((7, 15, 4), (11, 19, 8), 'rock', {'up': 'spike_top', 'north': 'spike_side', 'south': 'spike_side', 'east': 'spike_side', 'west': 'spike_side'}),
    ((4, 15, 9), (8, 18, 13), 'rock', {'up': 'spike_top', 'north': 'spike_side', 'south': 'spike_side', 'east': 'spike_side', 'west': 'spike_side'}),
])

C['blood_moon_lord'] = dict(tpu=2, boxes=[
    ((2, 2, 0), (14, 14, 16), 'moon', {'north': 'angry_face'}),
    ((0, 2, 2), (16, 14, 14), 'moon', {}),
    ((2, 0, 2), (14, 16, 14), 'moon', {}),
])

C['blood_moon_lord_crown'] = dict(tpu=2, boxes=[
    ((3, 0, 3), (13, 3, 13), 'gold', {'north': 'gems', 'south': 'gems', 'east': 'gems', 'west': 'gems'}),
    ((3, 3, 3), (5, 7, 5), 'gold', {}),
    ((11, 3, 3), (13, 7, 5), 'gold', {}),
    ((3, 3, 11), (5, 7, 13), 'gold', {}),
    ((11, 3, 11), (13, 7, 13), 'gold', {}),
    ((7, 3, 3), (9, 9, 5), 'gold', {'north': 'gems'}),
])

C['blood_eye'] = dict(tpu=2, boxes=[
    ((2, 2, 2), (14, 14, 14), 'eyeball', {'north': 'iris'}),
    ((3, 13, 1.5), (13, 14.5, 4), 'flesh', {}),
    ((6, 6, 14), (10, 10, 17), 'flesh', {}),
    ((7, 7, 17), (9, 9, 19), 'flesh', {}),
])

C['mimic_sack'] = dict(tpu=2, boxes=[
    ((3, 0, 3), (13, 10, 13), 'burlap', {'north': 'mimic_face'}),
    ((4, 10, 4), (12, 12, 12), 'burlap', {}),
    ((6, 12, 6), (10, 13.5, 10), 'rope', {}),
    ((5, 13.5, 5), (11, 15, 11), 'burlap', {}),
    ((6, 15, 6), (10, 16, 10), 'gold', {}),
])

C['storm_spirit'] = dict(tpu=2, boxes=[
    ((2, 6, 3), (14, 13, 13), 'cloud', {'north': 'cloud_eyes'}),
    ((0, 5, 5), (5, 11, 11), 'cloud', {}),
    ((11, 5, 5), (16, 12, 11), 'cloud', {}),
    ((5, 11, 4), (12, 15, 11), 'cloud', {}),
    ((3, 4, 4), (13, 6, 12), 'cloud_dark', {}),
    ((7, 1, 7), (9, 4, 9), 'bolt', {}),
    ((8, -2, 7), (10, 1, 9), 'bolt', {}),
    ((6, -5, 7), (8, -2, 9), 'bolt', {}),
])

C['void_stalker'] = dict(tpu=1, boxes=[
    ((5, -8, 7), (7, 4, 9), 'void', {}),
    ((9, -8, 7), (11, 4, 9), 'void', {}),
    ((4, 4, 6), (12, 15, 10), 'void', {}),
    ((5, 15, 5), (11, 22, 11), 'void', {'north': 'void_eyes'}),
    ((2, -2, 7), (4, 15, 9), 'void', {}),
    ((12, -2, 7), (14, 15, 9), 'void', {}),
    ((1.5, -5, 6.5), (4.5, -2, 9.5), 'claw', {}),
    ((11.5, -5, 6.5), (14.5, -2, 9.5), 'claw', {}),
])

C['eclipse_wraith'] = dict(tpu=2, boxes=[
    ((3, 3, 4), (13, 13, 12), 'void', {'north': 'wraith_face'}),
    ((4, 13, 5), (12, 15, 7), 'corona', {}),
    ((4, 1, 5), (12, 3, 7), 'corona', {}),
    ((1, 4, 5), (3, 12, 7), 'corona', {}),
    ((13, 4, 5), (15, 12, 7), 'corona', {}),
    ((2, 12, 5), (4, 14, 7), 'corona', {}),
    ((12, 12, 5), (14, 14, 7), 'corona', {}),
    ((2, 2, 5), (4, 4, 7), 'corona', {}),
    ((12, 2, 5), (14, 4, 7), 'corona', {}),
    ((-6, 7.5, 6), (3, 8.5, 13), 'void', {}),
    ((13, 7.5, 6), (22, 8.5, 13), 'void', {}),
    ((6, 6, 12), (10, 10, 17), 'void', {}),
])

C['frost_yeti'] = dict(tpu=2, boxes=[
    ((2, -3, 8), (7, 8, 14), 'fur', {}),
    ((9, -3, 8), (14, 8, 14), 'fur', {}),
    ((1, 6, 1), (15, 15, 14), 'fur', {}),
    ((-1, 9, -5), (17, 21, 5), 'fur', {}),
    ((4.5, 14, -10), (11.5, 21, -4), 'yeti_skin', {'north': 'yeti_face'}),
    ((4, 19, -10.5), (12, 21.5, -6), 'fur', {}),
    ((3, 20, -8), (5, 25, -6), 'ice', {}),
    ((11, 20, -8), (13, 25, -6), 'ice', {}),
    ((-5, 1, -6), (0, 19, 0), 'fur', {}),
    ((16, 1, -6), (21, 19, 0), 'fur', {}),
    ((-6, -3, -8), (1, 3, 1), 'yeti_skin', {}),
    ((15, -3, -8), (22, 3, 1), 'yeti_skin', {}),
    ((-6, 17, -7), (2, 23, 2), 'fur', {}),
    ((14, 17, -7), (22, 23, 2), 'fur', {}),
    ((5, 21, -2), (9, 24, 2), 'ice', {}),
    ((7, 15, 4), (10, 18, 7), 'ice', {}),
])

SIZES = {'north': lambda d: (d[0], d[1]), 'south': lambda d: (d[0], d[1]), 'east': lambda d: (d[2], d[1]),
         'west': lambda d: (d[2], d[1]), 'up': lambda d: (d[0], d[2]), 'down': lambda d: (d[0], d[2])}


def build(name, spec):
    tpu = spec['tpu']
    rects = []
    for i, (f, t, style, decos) in enumerate(spec['boxes']):
        size = [b - a for a, b in zip(f, t)]
        for face, dims in SIZES.items():
            w, h = (max(1, round(v * tpu)) for v in dims(size))
            rects.append((i, face, w, h))
    for tex in (32, 64, 128, 256, 512):
        x = y = row = 0
        places = {}
        ok = True
        for i, face, w, h in sorted(rects, key=lambda r: -r[3]):
            if x + w > tex:
                x, y, row = 0, y + row, 0
            if y + h > tex or w > tex:
                ok = False
                break
            places[(i, face)] = (x, y)
            x += w
            row = max(row, h)
        if ok:
            break
    img = Image.new('RGBA', (tex, tex), (0, 0, 0, 0))
    elements = []
    for i, (f, t, style, decos) in enumerate(spec['boxes']):
        faces = {}
        for (j, face, w, h) in [r for r in rects if r[0] == i]:
            x0, y0 = places[(i, face)]
            fc = Face(img, x0, y0, w, h, random.Random('%s/%d/%s' % (name, i, face)))
            for yy in range(h):
                for xx in range(w):
                    c = pick(fc.rng, style)
                    if face == 'up':
                        c = shade(c, 1.12)
                    fc.put(xx, yy, c)
            if style not in ('core', 'bolt', 'ghost'):
                dark = shade(STYLES[style][0][0], 0.85)
                for xx in range(w):
                    fc.put(xx, h - 1, dark)
                for yy in range(h):
                    fc.put(0, yy, dark)
            pattern(fc, style, face)
            if face in decos:
                deco(fc, decos[face])
            faces[face] = {'uv': [x0 * 16 / tex, y0 * 16 / tex, (x0 + w) * 16 / tex, (y0 + h) * 16 / tex], 'texture': '#skin'}
        elements.append({'from': list(f), 'to': list(t), 'faces': faces})
    model = {
        'textures': {'skin': 'worldomens:item/' + name, 'particle': 'worldomens:item/' + name},
        'elements': elements,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'rotation': [0, 180, 0], 'scale': [0.5, 0.5, 0.5]},
        },
    }
    return model, img


def main():
    args = sys.argv[1:]
    only = None
    if '--only' in args:
        k = args.index('--only')
        only = set(args[k + 1:])
        args = args[:k]
    out = args[0] if args else 'src/main/resources'
    for name in (*C, *CLASSIC):
        if only and name not in only:
            continue
        mdir = os.path.join(out, 'assets', 'worldomens', 'models', 'item')
        tdir = os.path.join(out, 'assets', 'worldomens', 'textures', 'item')
        os.makedirs(mdir, exist_ok=True)
        os.makedirs(tdir, exist_ok=True)
        if name in CLASSIC:
            archive = os.path.join(os.path.dirname(__file__), 'classic_creatures')
            shutil.copyfile(os.path.join(archive, name + '.json'), os.path.join(mdir, name + '.json'))
            shutil.copyfile(os.path.join(archive, name + '.png'), os.path.join(tdir, name + '.png'))
            print(name)
            continue
        model, img = build(name, C[name])
        with open(os.path.join(mdir, name + '.json'), 'w') as fh:
            json.dump(model, fh, indent='\t')
            fh.write('\n')
        img.save(os.path.join(tdir, name + '.png'))
        print('%-24s %2d boxes, %dx%d texture' % (name, len(model['elements']), img.width, img.height))


if __name__ == '__main__':
    main()
