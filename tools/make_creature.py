#!/usr/bin/env python3

import argparse
import json
import math
import os
import random
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import import_model

SIZE = 16
BIG_SIZE = 32


def grid(size=SIZE):
    return np.zeros((size, size, size), bool), np.zeros((size, size, size, 3))


def sphere(filled, colours, centre, radius, colour_fn, squash=(1.0, 1.0, 1.0)):
    size = filled.shape[0]
    for x in range(size):
        for y in range(size):
            for z in range(size):
                d = math.dist(((x + 0.5 - centre[0]) / squash[0], (y + 0.5 - centre[1]) / squash[1], (z + 0.5 - centre[2]) / squash[2]), (0, 0, 0))
                if d <= radius:
                    filled[x, y, z] = True
                    colours[x, y, z] = colour_fn(x, y, z, d)


def box(filled, colours, lo, hi, colour_fn):
    for x in range(max(lo[0], 0), min(hi[0], filled.shape[0])):
        for y in range(max(lo[1], 0), min(hi[1], filled.shape[1])):
            for z in range(max(lo[2], 0), min(hi[2], filled.shape[2])):
                filled[x, y, z] = True
                colours[x, y, z] = colour_fn(x, y, z) if callable(colour_fn) else colour_fn


def front_face(filled):
    face = {}
    size = filled.shape[0]
    for x in range(size):
        for y in range(size):
            for z in range(size):
                if filled[x, y, z]:
                    face[(x, y)] = z
                    break
    return face


def paint_front(filled, colours, pixels, colour):
    face = front_face(filled)
    for (x, y) in pixels:
        if (x, y) in face:
            colours[x, y, face[(x, y)]] = colour
            if face[(x, y)] + 1 < filled.shape[2] and filled[x, y, face[(x, y)] + 1]:
                colours[x, y, face[(x, y)] + 1] = colour


def blood_moon_lord():
    rng = random.Random(7)
    filled, colours = grid()
    craters = [(rng.uniform(2, 14), rng.uniform(2, 14), rng.uniform(2, 14), rng.uniform(1.2, 2.4)) for _ in range(9)]

    def colour(x, y, z, d):
        base = np.array([150, 18, 22])
        shade = 1.0 - 0.25 * (1 - y / SIZE)
        for cx, cy, cz, r in craters:
            if math.dist((x, y, z), (cx, cy, cz)) < r:
                return base * 0.55 * shade
        return base * shade * rng.uniform(0.9, 1.08)

    sphere(filled, colours, (8, 8, 8), 7.6, colour)
    eyes = [(4, 9), (5, 9), (5, 10), (6, 9), (10, 9), (11, 9), (10, 10), (9, 9)]
    paint_front(filled, colours, eyes, np.array([255, 200, 40]))
    brows = [(3, 11), (4, 11), (5, 11), (6, 10), (12, 11), (11, 11), (10, 11), (9, 10)]
    paint_front(filled, colours, brows, np.array([40, 0, 4]))
    mouth = [(5, 5), (6, 6), (7, 5), (8, 6), (9, 5), (10, 6), (11, 5), (6, 5), (8, 5), (10, 5)]
    paint_front(filled, colours, mouth, np.array([20, 0, 2]))
    return filled, colours


def blood_eye():
    rng = random.Random(3)
    filled, colours = grid()

    def colour(x, y, z, d):
        white = np.array([232, 220, 214]) * rng.uniform(0.93, 1.0)
        if rng.random() < 0.08:
            return np.array([170, 20, 30])
        return white

    sphere(filled, colours, (8, 8, 8), 6.6, colour)
    iris = [(x, y) for x in range(16) for y in range(16) if math.dist((x + 0.5, y + 0.5), (8, 8)) <= 3.4]
    paint_front(filled, colours, iris, np.array([180, 10, 24]))
    ring = [(x, y) for x in range(16) for y in range(16) if 2.6 < math.dist((x + 0.5, y + 0.5), (8, 8)) <= 3.4]
    paint_front(filled, colours, ring, np.array([110, 0, 12]))
    pupil = [(7, y) for y in range(5, 11)] + [(8, y) for y in range(5, 11)]
    paint_front(filled, colours, pupil, np.array([10, 0, 0]))
    shine = [(9, 10), (10, 10), (10, 9)]
    paint_front(filled, colours, shine, np.array([255, 235, 235]))
    return filled, colours


def storm_spirit():
    rng = random.Random(11)
    filled, colours = grid()

    def colour(x, y, z, d):
        grey = 70 + 60 * (y / SIZE) + rng.uniform(-8, 8)
        return np.array([grey, grey, grey + 18])

    for centre, radius in (((8, 9, 8), 5.0), ((4.5, 8, 8), 3.6), ((11.5, 8, 8), 3.6), ((8, 11.5, 7), 3.4), ((6, 10, 10), 3.2), ((10, 10, 10), 3.2)):
        sphere(filled, colours, centre, radius, colour)
    eyes = [(6, 9), (6, 10), (10, 9), (10, 10)]
    paint_front(filled, colours, eyes, np.array([255, 245, 120]))
    bolt = [(8, 4), (7, 3), (8, 2), (7, 1), (8, 0)]
    for (x, y) in bolt:
        filled[x, y, 8] = True
        colours[x, y, 8] = np.array([255, 240, 90])
    return filled, colours


def harvest_spirit():
    rng = random.Random(5)
    filled, colours = grid()

    def straw(x, y, z, d):
        weave = (y + x // 2 + z // 2) % 3 == 0
        base = np.array([170, 128, 48]) if weave else np.array([218, 176, 74])
        return base * rng.uniform(0.92, 1.06)

    sphere(filled, colours, (8, 7, 8), 5.2, straw, squash=(1.0, 1.05, 1.0))
    for x, y in ((2, 7), (1, 8), (14, 7), (15, 8), (6, 1), (8, 1), (10, 1)):
        filled[x, y, 8] = True
        colours[x, y, 8] = np.array([200, 160, 64])
    for y in (12, 13, 14):
        filled[8, y, 8] = True
        colours[8, y, 8] = np.array([86, 146, 48])
    for x, y in ((7, 13), (6, 14), (5, 14), (9, 14), (10, 14), (11, 15)):
        filled[x, y, 8] = True
        colours[x, y, 8] = np.array([104, 176, 58])
    filled[8, 15, 8] = True
    colours[8, 15, 8] = np.array([236, 200, 90])
    paint_front(filled, colours, [(6, 8), (6, 9), (10, 8), (10, 9)], np.array([46, 30, 20]))
    paint_front(filled, colours, [(5, 7), (11, 7)], np.array([232, 132, 120]))
    paint_front(filled, colours, [(6, 6), (7, 5), (8, 5), (9, 5), (10, 6)], np.array([90, 50, 26]))
    return filled, colours


def nomad_merchant():
    rng = random.Random(9)
    filled, colours = grid(BIG_SIZE)
    indigo = np.array([62, 50, 116])

    def robe(x, y, z):
        return indigo * rng.uniform(0.88, 1.05)

    for y in range(0, 20):
        radius = 6.5 - y * 0.1
        for x in range(BIG_SIZE):
            for z in range(BIG_SIZE):
                if math.dist((x + 0.5, z + 0.5), (16, 14)) <= radius:
                    filled[x, y, z] = True
                    colours[x, y, z] = np.array([200, 160, 60]) if y < 2 else np.array([112, 72, 40]) if y == 10 else robe(x, y, z)
    sphere(filled, colours, (16, 19.5, 14), 6.0, lambda x, y, z, d: robe(x, y, z), squash=(1.0, 0.5, 0.8))
    box(filled, colours, (12, 13, 7), (21, 16, 10), robe)
    sphere(filled, colours, (16, 24, 13.5), 4.6, lambda x, y, z, d: indigo * 0.82 * rng.uniform(0.9, 1.05))
    for x, y, z in ((16, 29, 14), (16, 29, 15), (16, 30, 16)):
        filled[x, y, z] = True
        colours[x, y, z] = indigo * 0.8
    face = [(x, y) for x in range(BIG_SIZE) for y in range(BIG_SIZE) if math.dist((x + 0.5, y + 0.5), (16, 23.5)) <= 2.7]
    paint_front(filled, colours, face, np.array([16, 12, 22]))
    paint_front(filled, colours, [(14, 24), (15, 24), (17, 24), (18, 24)], np.array([255, 222, 96]))

    box(filled, colours, (9, 6, 18), (24, 15, 28), lambda x, y, z: np.array([92, 60, 34]) if x % 4 == 0 or y % 4 == 2 else np.array([128, 86, 48]))
    for x in range(9, 24):
        for y in range(15, 20):
            for z in range(18, 28):
                if math.dist((y + 0.5, z + 0.5), (17.5, 23)) <= 2.6:
                    filled[x, y, z] = True
                    colours[x, y, z] = np.array([214, 180, 70]) if x % 5 == 0 else np.array([168, 40, 40])
    sphere(filled, colours, (12.5, 22.5, 22.5), 2.8, lambda x, y, z, d: np.array([192, 112, 70]) * rng.uniform(0.9, 1.05))
    sphere(filled, colours, (19.5, 23, 22.5), 3.2, lambda x, y, z, d: np.array([170, 98, 62]) * rng.uniform(0.9, 1.05))
    for x, top in ((11, 30), (14, 31), (19, 29)):
        for y in range(25, top + 1):
            filled[x, y, 23] = True
            colours[x, y, 23] = np.array([110, 74, 40])
    for z in range(6, 19):
        filled[22, 29, z] = True
        colours[22, 29, z] = np.array([110, 74, 40])
    filled[22, 28, 6] = True
    colours[22, 28, 6] = np.array([60, 60, 60])
    box(filled, colours, (21, 24, 5), (25, 28, 9), np.array([44, 44, 48]))
    box(filled, colours, (22, 25, 5), (24, 27, 9), np.array([255, 206, 90]))
    return filled, colours


def horde_warlord():
    rng = random.Random(13)
    filled, colours = grid(BIG_SIZE)
    dark = np.array([42, 42, 50])
    leather = np.array([92, 60, 34])
    red = np.array([138, 22, 22])
    bone = np.array([226, 214, 186])

    def iron(x, y, z):
        return np.array([74, 74, 86]) * rng.uniform(0.88, 1.08)

    box(filled, colours, (8, 3, 20), (25, 25, 23), lambda x, y, z: np.array([112, 16, 22]) * rng.uniform(0.85, 1.05))
    for x in range(8, 25):
        for y in range(3, 3 + rng.randint(0, 3)):
            filled[x, y, 20:23] = False
    for lo_x in (9, 18):
        box(filled, colours, (lo_x, 0, 12), (lo_x + 5, 4, 19), dark)
        box(filled, colours, (lo_x, 4, 12), (lo_x + 5, 11, 19), iron)
        box(filled, colours, (lo_x, 6, 11), (lo_x + 5, 9, 12), iron)
    box(filled, colours, (8, 11, 11), (25, 13, 20), leather)
    box(filled, colours, (15, 11, 10), (18, 13, 11), red)
    box(filled, colours, (7, 13, 11), (26, 24, 20), iron)
    emblem = [(16, y) for y in range(15, 22)] + [(15, 21), (17, 21), (14, 20), (18, 20), (15, 17), (17, 17)]
    paint_front(filled, colours, emblem, red)
    for lo_x in (3, 25):
        box(filled, colours, (lo_x, 9, 13), (lo_x + 5, 15, 18), dark)
        box(filled, colours, (lo_x, 15, 12), (lo_x + 5, 21, 19), iron)
    for lo_x in (2, 23):
        box(filled, colours, (lo_x, 21, 10), (lo_x + 8, 26, 21), lambda x, y, z: np.array([58, 58, 68]) * rng.uniform(0.9, 1.06))
    for x, top in ((4, 28), (7, 29), (25, 29), (28, 28)):
        for y in range(26, top + 1):
            filled[x, y, 15] = True
            colours[x, y, 15] = bone
    box(filled, colours, (11, 24, 11), (22, 31, 20), lambda x, y, z: dark * rng.uniform(0.9, 1.1))
    paint_front(filled, colours, [(x, y) for x in range(12, 21) for y in (27, 28)], np.array([12, 8, 8]))
    paint_front(filled, colours, [(13, 27), (14, 27), (18, 27), (19, 27)], np.array([255, 96, 30]))
    for x, y in ((10, 27), (9, 28), (8, 29), (8, 30), (8, 31), (22, 27), (23, 28), (24, 29), (24, 30), (24, 31)):
        for z in (14, 15, 16):
            filled[x, y, z] = True
            colours[x, y, z] = bone * (0.85 if y < 29 else 1.0)
    return filled, colours


def spectral_captain():
    rng = random.Random(21)
    filled, colours = grid(BIG_SIZE)
    teal = np.array([44, 104, 118])
    gold = np.array([224, 180, 70])
    pale = np.array([196, 236, 232])
    navy = np.array([20, 34, 58])

    def coat(x, y, z):
        return teal * rng.uniform(0.85, 1.1)

    for y in range(2, 21):
        radius = 7.2 - (y - 2) * 0.13
        for x in range(BIG_SIZE):
            for z in range(BIG_SIZE):
                if math.dist((x + 0.5, z + 0.5), (16, 15)) > radius or (y < 6 and rng.random() < (6 - y) * 0.22):
                    continue
                filled[x, y, z] = True
                colours[x, y, z] = pale * 0.8 if y < 4 else coat(x, y, z)
    box(filled, colours, (15, 8, 8), (17, 20, 10), gold)
    box(filled, colours, (9, 18, 12), (23, 22, 19), coat)
    for lo_x in (7, 21):
        box(filled, colours, (lo_x, 21, 12), (lo_x + 4, 23, 18), gold)
        box(filled, colours, (lo_x, 9, 13), (lo_x + 4, 21, 17), coat)
    box(filled, colours, (12, 22, 11), (21, 29, 19), lambda x, y, z: pale * rng.uniform(0.92, 1.03))
    paint_front(filled, colours, [(13, 26), (14, 26), (13, 25), (14, 25), (18, 26), (19, 26), (18, 25), (19, 25)], np.array([14, 30, 40]))
    paint_front(filled, colours, [(14, 25), (18, 25)], np.array([120, 255, 240]))
    paint_front(filled, colours, [(x, 23) for x in range(14, 19) if x % 2 == 0], np.array([40, 70, 76]))
    box(filled, colours, (7, 29, 10), (26, 30, 21), navy)
    box(filled, colours, (11, 30, 12), (22, 32, 19), navy)
    box(filled, colours, (7, 29, 9), (26, 30, 10), gold)
    return filled, colours


def magma_colossus():
    rng = random.Random(33)
    filled, colours = grid(BIG_SIZE)
    bone = np.array([70, 40, 90])

    def rock(x, y, z):
        veins = abs(math.sin(x * 0.7) + math.sin(y * 0.6 + 1.0) + math.sin(z * 0.8 + 2.0))
        if veins < 0.28:
            return np.array([255, 150, 40]) if rng.random() < 0.6 else np.array([235, 80, 24])
        return np.array([44, 38, 42])

    sphere(filled, colours, (16, 16, 18), 9.0, lambda x, y, z, d: rock(x, y, z), squash=(1.2, 0.95, 1.3))
    box(filled, colours, (9, 7, 2), (24, 20, 12), rock)
    for lo_x in (5, 22):
        for lo_z in (8, 21):
            box(filled, colours, (lo_x, 0, lo_z), (lo_x + 6, 10, lo_z + 6), rock)
    for x, top in ((10, 27), (14, 29), (18, 29), (22, 27)):
        for y in range(23, top + 1):
            filled[x, y, 20] = True
            colours[x, y, 20] = np.array([255, 120, 30]) if y == top else np.array([50, 44, 48])
    for lo_x, step in ((9, -1), (23, 1)):
        for i in range(5):
            filled[lo_x + step * (i // 2), 8 + i, 1] = True
            colours[lo_x + step * (i // 2), 8 + i, 1] = bone
            filled[lo_x + step * (i // 2), 8 + i, 2] = True
            colours[lo_x + step * (i // 2), 8 + i, 2] = bone
    paint_front(filled, colours, [(12, 16), (13, 16), (19, 16), (20, 16)], np.array([255, 220, 90]))
    paint_front(filled, colours, [(x, 10) for x in range(12, 21)], np.array([235, 80, 24]))
    return filled, colours


def void_stalker():
    rng = random.Random(44)
    filled, colours = grid(BIG_SIZE)
    shadow = np.array([20, 12, 32])
    cloak = np.array([38, 20, 60])
    claw = np.array([170, 120, 220])

    box(filled, colours, (11, 8, 18), (22, 25, 20), lambda x, y, z: cloak)
    for x in range(11, 22):
        for y in range(8, 8 + rng.randint(0, 6)):
            filled[x, y, 18:20] = False
    box(filled, colours, (13, 12, 13), (20, 25, 18), shadow)
    for lo_x in (13, 17):
        box(filled, colours, (lo_x, 0, 14), (lo_x + 3, 12, 17), shadow)
    for lo_x in (9, 21):
        box(filled, colours, (lo_x, 8, 14), (lo_x + 3, 25, 17), shadow)
        box(filled, colours, (lo_x, 5, 14), (lo_x + 3, 8, 17), claw)
    box(filled, colours, (13, 25, 12), (20, 32, 18), shadow)
    paint_front(filled, colours, [(14, 28), (15, 28), (17, 28), (18, 28)], np.array([255, 80, 220]))
    paint_front(filled, colours, [(14, 27), (18, 27)], np.array([120, 30, 160]))
    return filled, colours


def piglin_warchief():
    rng = random.Random(41)
    filled, colours = grid(BIG_SIZE)
    skin = np.array([196, 120, 112])
    snout = np.array([170, 92, 90])
    gold = np.array([226, 178, 58])
    dark_gold = np.array([150, 108, 30])
    black = np.array([30, 28, 32])
    red = np.array([138, 24, 28])
    bone = np.array([232, 222, 196])

    def skin_fn(x, y, z):
        return skin * rng.uniform(0.9, 1.08)

    def gold_fn(x, y, z):
        return gold * rng.uniform(0.86, 1.08)

    box(filled, colours, (8, 6, 20), (24, 22, 22), lambda x, y, z: red * rng.uniform(0.85, 1.05))
    for lo_x in (10, 17):
        box(filled, colours, (lo_x, 0, 12), (lo_x + 5, 3, 19), black)
        box(filled, colours, (lo_x, 3, 12), (lo_x + 5, 11, 19), gold_fn)
    box(filled, colours, (8, 11, 11), (24, 13, 20), black)
    box(filled, colours, (7, 13, 10), (25, 23, 21), skin_fn)
    box(filled, colours, (9, 14, 9), (23, 22, 11), gold_fn)
    paint_front(filled, colours, [(16, y) for y in range(15, 21)] + [(15, 19), (17, 19)], black)
    for lo_x in (2, 25):
        box(filled, colours, (lo_x, 21, 9), (lo_x + 5, 25, 21), gold_fn)
        box(filled, colours, (lo_x, 9, 11), (lo_x + 5, 21, 19), skin_fn)
        box(filled, colours, (lo_x, 7, 10), (lo_x + 5, 11, 20), dark_gold)
    box(filled, colours, (10, 23, 9), (22, 30, 20), skin_fn)
    box(filled, colours, (13, 24, 5), (19, 28, 9), lambda x, y, z: snout * rng.uniform(0.92, 1.05))
    paint_front(filled, colours, [(14, 25), (18, 25)], black)
    paint_front(filled, colours, [(12, 27), (13, 27), (19, 27), (20, 27)], np.array([255, 190, 40]))
    for x in (12, 19):
        box(filled, colours, (x, 23, 4), (x + 1, 27, 7), bone)
    for lo_x in (6, 22):
        box(filled, colours, (lo_x, 25, 13), (lo_x + 4, 29, 17), snout)
    box(filled, colours, (10, 29, 9), (22, 31, 20), gold_fn)
    for x in (10, 14, 18, 21):
        filled[x, 31, 10] = True
        colours[x, 31, 10] = gold
    return filled, colours


def tidecaller():
    rng = random.Random(53)
    filled, colours = grid(BIG_SIZE)
    teal = np.array([40, 118, 120])
    deep = np.array([22, 62, 96])
    kelp = np.array([34, 96, 58])
    skin = np.array([112, 170, 150])
    coral = np.array([224, 112, 100])
    pearl = np.array([226, 232, 226])
    glow = np.array([120, 255, 240])

    for y in range(0, 20):
        radius = 8.0 - y * 0.2
        for x in range(BIG_SIZE):
            for z in range(BIG_SIZE):
                if math.dist((x + 0.5, z + 0.5), (16, 15)) <= radius and not (y < 5 and rng.random() < (5 - y) * 0.2):
                    filled[x, y, z] = True
                    colours[x, y, z] = (deep if y < 4 else teal) * rng.uniform(0.85, 1.1)
    for x in range(8, 24):
        for z in range(9, 22):
            if (x + z) % 4 == 0 and rng.random() < 0.6:
                for y in range(0, rng.randint(3, 9)):
                    filled[x, y, z] = True
                    colours[x, y, z] = kelp * rng.uniform(0.85, 1.1)
    box(filled, colours, (8, 19, 10), (24, 24, 21), lambda x, y, z: teal * rng.uniform(0.88, 1.08))
    for lo_x in (3, 24):
        box(filled, colours, (lo_x, 20, 10), (lo_x + 5, 24, 20), lambda x, y, z: kelp * rng.uniform(0.9, 1.1))
        box(filled, colours, (lo_x, 24, 11), (lo_x + 5, 31, 18), lambda x, y, z: skin * rng.uniform(0.92, 1.06))
    box(filled, colours, (11, 22, 10), (21, 29, 20), lambda x, y, z: skin * rng.uniform(0.92, 1.06))
    paint_front(filled, colours, [(13, 25), (14, 25), (18, 25), (19, 25)], glow)
    paint_front(filled, colours, [(15, 23), (16, 23), (17, 23)], np.array([30, 56, 56]))
    for x in (11, 14, 17, 20):
        box(filled, colours, (x, 29, 10), (x + 1, 31, 12), coral)
    box(filled, colours, (10, 29, 10), (22, 30, 20), lambda x, y, z: coral * rng.uniform(0.9, 1.08))
    filled[15:17, 30:32, 15:17] = True
    colours[15:17, 30:32, 15:17] = pearl
    for x in (9, 22):
        for y in range(17, 22):
            filled[x, y, 9] = True
            colours[x, y, 9] = kelp
    return filled, colours


BUILDERS = {
    "blood_moon_lord": blood_moon_lord,
    "blood_eye": blood_eye,
    "storm_spirit": storm_spirit,
    "harvest_spirit": harvest_spirit,
    "nomad_merchant": nomad_merchant,
    "horde_warlord": horde_warlord,
    "spectral_captain": spectral_captain,
    "magma_colossus": magma_colossus,
    "void_stalker": void_stalker,
    "piglin_warchief": piglin_warchief,
    "tidecaller": tidecaller,
}
BIG = {"nomad_merchant", "horde_warlord", "spectral_captain", "magma_colossus", "void_stalker", "piglin_warchief", "tidecaller"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("name", choices=sorted(BUILDERS))
    parser.add_argument("model")
    parser.add_argument("texture")
    parser.add_argument("--texture-id", required=True)
    parser.add_argument("--colors", type=int, default=24)
    parser.add_argument("--preview")
    args = parser.parse_args()

    filled, average = BUILDERS[args.name]()
    average = np.clip(average, 0, 255)
    palette, index = import_model.quantise(filled, average, args.colors)
    big = args.name in BIG
    origin = np.full(3, -8.0 if big else 0.0)
    model = import_model.build_model(index, import_model.solid_interior(filled), palette, origin, 1.0, args.texture_id, "part", big)
    with open(args.model, "w") as f:
        json.dump(model, f, separators=(",", ":"))
    import_model.palette_texture(palette).save(args.texture)
    print(f"{args.name}: {int(filled.sum())} voxels, {len(model['elements'])} boxes, {len(palette)} colours")
    if args.preview:
        import_model.preview(index, palette, args.preview)


if __name__ == "__main__":
    main()
