#!/usr/bin/env python3
"""Turns a pixel-art sprite from an AI image tool into a Minecraft item texture.

The image must show a single sprite on a flat, solid background (pure green works best). The script finds the
sprite's pixel grid, takes one colour per cell, makes the background transparent and writes a PNG at the grid's
own resolution (16 or 32 pixels per side, the sprite centred). AI tools rarely draw exactly 16x16, so keeping
the grid they drew avoids blurring the art.

Usage:
    python3 tools/import_sprite.py INPUT OUTPUT [--grid PIXELS] [--preview PREVIEW.png]

If the grid it finds is wrong (the texture comes out bigger than the drawing, with doubled pixels), measure one
pixel of the drawing in the image and pass it with --grid.

Requires Pillow and numpy (dev tool only; the mod does not use them).
"""

import argparse

import numpy as np
from PIL import Image

BACKGROUND_TOLERANCE = 90.0


def background_colour(rgb):
    """Median colour of the four corners."""
    patch = 24
    corners = np.concatenate([
        rgb[:patch, :patch].reshape(-1, 3), rgb[:patch, -patch:].reshape(-1, 3),
        rgb[-patch:, :patch].reshape(-1, 3), rgb[-patch:, -patch:].reshape(-1, 3)])
    return np.median(corners, axis=0)


def edge_profile(rgb, axis):
    """How much the colour changes between neighbouring columns (axis=1) or rows (axis=0)."""
    diff = np.abs(np.diff(rgb.astype(np.int32), axis=axis)).sum(axis=2)
    return diff.mean(axis=0 if axis == 1 else 1)


def cell_size(profile):
    """Grid period: the first strong peak of the edge profile's autocorrelation."""
    centred = profile - profile.mean()
    lags = range(8, len(profile) // 4)
    scores = [float(np.dot(centred[:-lag], centred[lag:])) / (len(profile) - lag) for lag in lags]
    best = max(scores)
    for k in range(1, len(scores) - 1):
        if scores[k] >= scores[k - 1] and scores[k] >= scores[k + 1] and scores[k] >= 0.5 * best:
            return lags[k]
    return lags[int(np.argmax(scores))]


def grid_lines(profile, period):
    """Positions of the cell boundaries along one axis, snapped to the strongest nearby edges."""
    length = len(profile) + 1
    best_offset, best_score = 0, -1.0
    for offset in range(period):
        positions = np.arange(offset, len(profile), period)
        score = sum(profile[max(0, p - 2):p + 3].max() for p in positions)
        if score > best_score:
            best_offset, best_score = offset, score
    lines = []
    window = max(2, period // 5)
    for p in range(best_offset, len(profile), period):
        lo, hi = max(0, p - window), min(len(profile), p + window + 1)
        local = profile[lo:hi]
        snapped = lo + int(np.argmax(local)) if local.max() > profile.mean() * 2 else p
        lines.append(snapped + 1)
    if not lines or lines[0] > period // 2:
        lines.insert(0, max(0, (lines[0] if lines else period) - period))
    while lines[-1] + period // 2 < length:
        lines.append(min(length, lines[-1] + period))
    return lines


def sample(rgb, background, xs, ys):
    """One colour per cell (median of its centre), or transparent where the cell is mostly background."""
    rows = []
    for j in range(len(ys) - 1):
        row = []
        for i in range(len(xs) - 1):
            x0, x1, y0, y1 = xs[i], xs[i + 1], ys[j], ys[j + 1]
            mx, my = max(1, (x1 - x0) // 4), max(1, (y1 - y0) // 4)
            cell = rgb[y0 + my:y1 - my, x0 + mx:x1 - mx].reshape(-1, 3).astype(np.float64)
            if len(cell) == 0:
                row.append(None)
                continue
            is_bg = np.linalg.norm(cell - background, axis=1) < BACKGROUND_TOLERANCE
            if is_bg.mean() > 0.5:
                row.append(None)
            else:
                row.append(tuple(int(round(c)) for c in np.median(cell[~is_bg], axis=0)))
        rows.append(row)
    return rows


def to_texture(cells):
    """Crops to the sprite and centres it on a 16 or 32 pixel square canvas."""
    filled = [(j, i) for j, row in enumerate(cells) for i, c in enumerate(row) if c is not None]
    if not filled:
        raise SystemExit("No sprite found: is the background a flat, solid colour?")
    top, bottom = min(j for j, _ in filled), max(j for j, _ in filled)
    left, right = min(i for _, i in filled), max(i for _, i in filled)
    width, height = right - left + 1, bottom - top + 1
    size = 16 if max(width, height) <= 16 else 32 if max(width, height) <= 32 else 16 * -(-max(width, height) // 16)
    texture = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ox, oy = (size - width) // 2, (size - height) // 2
    for j in range(top, bottom + 1):
        for i in range(left, right + 1):
            colour = cells[j][i]
            if colour is not None:
                texture.putpixel((ox + i - left, oy + j - top), colour + (255,))
    return texture, width, height


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("input")
    parser.add_argument("output")
    parser.add_argument("--grid", type=int, help="size of one drawn pixel in the image, instead of finding it")
    parser.add_argument("--preview", help="also write the texture scaled up, for checking it by eye")
    args = parser.parse_args()

    rgb = np.asarray(Image.open(args.input).convert("RGB"))
    background = background_colour(rgb)
    period = args.grid or round((cell_size(edge_profile(rgb, 1)) + cell_size(edge_profile(rgb, 0))) / 2)
    xs = grid_lines(edge_profile(rgb, 1), period)
    ys = grid_lines(edge_profile(rgb, 0), period)
    texture, width, height = to_texture(sample(rgb, background, xs, ys))
    texture.save(args.output)
    print(f"{args.input}: grid {period}px, sprite {width}x{height} cells -> {texture.size[0]}x{texture.size[1]} {args.output}")
    if args.preview:
        scale = max(1, 512 // texture.size[0])
        preview = Image.new("RGBA", texture.size, (40, 40, 48, 255))
        preview.alpha_composite(texture)
        preview.resize((texture.size[0] * scale, texture.size[1] * scale), Image.NEAREST).save(args.preview)


if __name__ == "__main__":
    main()
