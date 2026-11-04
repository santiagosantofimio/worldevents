"""Orthographic preview of a Minecraft item model (elements with per-face UVs, no element rotation).

    python3 tools/render_model.py MODEL.json TEXTURE.png OUT.png [yaw pitch scale]...

Model space: x east, y up, z south. Yaw turns the model around y (0: looking at the south face, i.e. from +z).
"""
import json
import math
import sys

import numpy as np
from PIL import Image

FACE_NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.6, 'east': 0.6}


def face_corners(name, f, t):
    """The face's corners in the order that matches its UV: (u0,v0) (u1,v0) (u1,v1) (u0,v1), as Minecraft maps them."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    if name == 'north':   # looking at -z face from outside: left is east (x1), right is west (x0)
        return [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)]
    if name == 'south':
        return [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
    if name == 'west':
        return [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]
    if name == 'east':
        return [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
    if name == 'up':
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    if name == 'down':
        return [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]
    raise ValueError(name)


def render(model, texture, yaw, pitch, scale, size=(640, 640)):
    tex = np.asarray(texture.convert('RGBA')).astype(float)
    th, tw = tex.shape[:2]
    W, H = size
    img = np.zeros((H, W, 4))
    img[..., :3] = (40, 44, 52)
    img[..., 3] = 255
    zbuf = np.full((H, W), -1e9)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def view(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        # Turn around y, then tilt around x (camera looks along -z of view space).
        x, z = x * cy + z * sy, -x * sy + z * cy
        y, z = y * cp - z * sp, y * sp + z * cp
        return x, y, z

    for el in model['elements']:
        f, t = el['from'], el['to']
        for name, face in el.get('faces', {}).items():
            n = FACE_NORMALS[name]
            nv = view((n[0] + 8, n[1] + 8, n[2] + 8))
            if nv[2] <= 1e-6:
                continue  # facing away
            u0, v0, u1, v1 = face['uv']
            corners = [view(c) for c in face_corners(name, f, t)]
            a, b, d = np.array(corners[0]), np.array(corners[1]), np.array(corners[3])
            # Sample the face densely: one sample per screen pixel or texel, whichever is finer.
            span = max(np.linalg.norm(b - a), np.linalg.norm(d - a)) * scale
            tex_span = max(abs(u1 - u0) * tw / 16, abs(v1 - v0) * th / 16)
            steps = int(max(span, tex_span) * 1.5) + 2
            s = (np.arange(steps) + 0.5) / steps
            S, T = np.meshgrid(s, s)
            P = a[None, None, :] + S[..., None] * (b - a)[None, None, :] + T[..., None] * (d - a)[None, None, :]
            U = u0 + (u1 - u0) * S
            V = v0 + (v1 - v0) * T
            tx = np.clip((U / 16 * tw).astype(int), 0, tw - 1)
            ty = np.clip((V / 16 * th).astype(int), 0, th - 1)
            col = tex[ty, tx]
            sx = (W / 2 + P[..., 0] * scale).astype(int)
            syy = (H / 2 - P[..., 1] * scale).astype(int)
            depth = P[..., 2]
            ok = (col[..., 3] > 0) & (sx >= 0) & (sx < W) & (syy >= 0) & (syy < H)
            shade = SHADE[name]
            for i, j in zip(*np.nonzero(ok)):
                X, Y = sx[i, j], syy[i, j]
                if depth[i, j] > zbuf[Y, X]:
                    zbuf[Y, X] = depth[i, j]
                    c = col[i, j]
                    img[Y, X, :3] = c[:3] * shade
    return Image.fromarray(img.astype(np.uint8))


def main():
    model = json.load(open(sys.argv[1]))
    texture = Image.open(sys.argv[2])
    views = [(30, 20, 14), (180, 10, 14), (90, 10, 14), (0, 10, 14)]
    if len(sys.argv) > 4:
        vals = [float(v) for v in sys.argv[4:]]
        views = [tuple(vals[i:i + 3]) for i in range(0, len(vals), 3)]
    tiles = [render(model, texture, *v) for v in views]
    W, H = tiles[0].size
    sheet = Image.new('RGB', (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    sheet.save(sys.argv[3])


if __name__ == '__main__':
    main()
