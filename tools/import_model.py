#!/usr/bin/env python3
"""Turns a textured 3D model (GLB, for example from Meshy) into a Minecraft item model made of cubes.

Minecraft item models can only contain axis-aligned boxes, so the mesh is voxelised: its surface is sampled,
each voxel takes the colour of the texture there, and runs of voxels with the same colour are merged into boxes.
The colours go into a small palette texture.

Kinds (how the model is placed and displayed):
    tool   held like a vanilla sword or pickaxe: the long axis runs bottom-left to top-right, flat side facing
           the camera, using the vanilla handheld display transforms.
    head   worn on a mob's head like a carved pumpkin: one 16x16x16 box with a texture per face, front north.
    crown  sits on top of a mob's head: centred, starting just below the top of the head.
    held   a small object, for example in a fox's mouth (shown with the "ground" transform).
    part   a creature's body part shown by an item display: fills 16 units, centred, placed by the display.
           With --big it fills 32 units (-8 to 24) instead, for tall creatures: the display then needs half the
           scale, so the vanilla block shown without the resource pack is half as big (Creature.Part#big).

Usage:
    python3 tools/import_model.py INPUT.glb KIND MODEL.json TEXTURE.png --texture-id worldevents:item/name_3d
        [--resolution N] [--colors N] [--turn DEGREES] [--flip] [--big] [--dominant] [--preview PREVIEW.png]

Requires Pillow and numpy (dev tool only; the mod does not use them).
"""

import argparse
import io
import json
import math
import struct
from collections import deque

import numpy as np
from PIL import Image

COMPONENTS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}
DTYPES = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}

HANDHELD_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
}
BLOCK_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}
FACES = {  # name: (axis, direction)
    "east": (0, 1), "west": (0, -1), "up": (1, 1), "down": (1, -1), "south": (2, 1), "north": (2, -1),
}


def load_glb(path):
    """Triangles (positions, uvs, indices) of every mesh in the default scene, plus the base colour texture."""
    with open(path, "rb") as f:
        data = f.read()
    magic, _, length = struct.unpack_from("<4sII", data, 0)
    if magic != b"glTF":
        raise SystemExit(f"{path} is not a GLB file")
    offset, doc, binary = 12, None, b""
    while offset < length:
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        chunk = data[offset + 8:offset + 8 + chunk_length]
        offset += 8 + chunk_length
        if chunk_type == 0x4E4F534A:
            doc = json.loads(chunk)
        elif chunk_type == 0x004E4942:
            binary = chunk
    if doc.get("extensionsRequired"):
        raise SystemExit(f"Unsupported glTF extensions: {doc['extensionsRequired']}")

    def accessor(index):
        acc = doc["accessors"][index]
        view = doc["bufferViews"][acc["bufferView"]]
        comps = COMPONENTS[acc["type"]]
        dtype = np.dtype(DTYPES[acc["componentType"]])
        start = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
        stride = view.get("byteStride", comps * dtype.itemsize)
        raw = np.frombuffer(binary, dtype=np.uint8, count=stride * (acc["count"] - 1) + comps * dtype.itemsize, offset=start)
        rows = np.lib.stride_tricks.as_strided(raw, shape=(acc["count"], comps * dtype.itemsize), strides=(stride, 1))
        values = np.ascontiguousarray(rows).view(dtype).reshape(acc["count"], comps).astype(np.float64)
        if acc.get("normalized"):
            values /= np.iinfo(dtype).max
        return values

    def node_matrix(node):
        if "matrix" in node:
            return np.array(node["matrix"], dtype=np.float64).reshape(4, 4).T
        t = np.eye(4)
        if "translation" in node:
            t[:3, 3] = node["translation"]
        r = np.eye(4)
        if "rotation" in node:
            x, y, z, w = node["rotation"]
            r[:3, :3] = [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                         [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                         [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]
        s = np.eye(4)
        if "scale" in node:
            s[0, 0], s[1, 1], s[2, 2] = node["scale"]
        return t @ r @ s

    positions, uvs, indices, texture_index = [], [], [], None
    count = 0

    def visit(node_index, parent):
        nonlocal count, texture_index
        node = doc["nodes"][node_index]
        world = parent @ node_matrix(node)
        if "mesh" in node:
            for prim in doc["meshes"][node["mesh"]]["primitives"]:
                if prim.get("mode", 4) != 4:
                    continue
                pos = accessor(prim["attributes"]["POSITION"])
                pos = (np.c_[pos, np.ones(len(pos))] @ world.T)[:, :3]
                uv = accessor(prim["attributes"]["TEXCOORD_0"]) if "TEXCOORD_0" in prim["attributes"] else np.zeros((len(pos), 2))
                idx = accessor(prim["indices"]).astype(np.int64).reshape(-1, 3) if "indices" in prim else np.arange(len(pos)).reshape(-1, 3)
                positions.append(pos)
                uvs.append(uv)
                indices.append(idx + count)
                count += len(pos)
                material = doc["materials"][prim["material"]] if "material" in prim else {}
                base = material.get("pbrMetallicRoughness", {}).get("baseColorTexture")
                if base is not None and texture_index is None:
                    texture_index = base["index"]
        for child in node.get("children", []):
            visit(child, world)

    scene = doc["scenes"][doc.get("scene", 0)]
    for root in scene["nodes"]:
        visit(root, np.eye(4))

    texture = None
    if texture_index is not None:
        image = doc["images"][doc["textures"][texture_index]["source"]]
        view = doc["bufferViews"][image["bufferView"]]
        start = view.get("byteOffset", 0)
        texture = Image.open(io.BytesIO(binary[start:start + view["byteLength"]])).convert("RGB")
    return np.concatenate(positions), np.concatenate(uvs), np.concatenate(indices), texture


def sample_surface(positions, uvs, triangles, spacing):
    """Points spread over every triangle (at most `spacing` apart) and their texture coordinates."""
    v = positions[triangles]
    t = uvs[triangles]
    edges = np.max(np.linalg.norm(v - np.roll(v, 1, axis=1), axis=2), axis=1)
    steps = np.clip(np.ceil(edges / spacing), 1, 24).astype(int)
    points, coords = [], []
    for n in np.unique(steps):
        tris, tuv = v[steps == n], t[steps == n]
        if n == 1:
            # Triangles smaller than the spacing: their centre is enough (dense meshes have millions of them).
            weights = [(1 / 3, 1 / 3, 1 / 3)]
        else:
            weights = [(i / n, j / n, 1 - i / n - j / n) for i in range(n + 1) for j in range(n + 1 - i)]
        w = np.array(weights)
        points.append(np.einsum("wk,tkd->twd", w, tris).reshape(-1, 3))
        coords.append(np.einsum("wk,tkd->twd", w, tuv).reshape(-1, 2))
    return np.concatenate(points), np.concatenate(coords)


def rotation(axis, degrees):
    a = math.radians(degrees)
    c, s = math.cos(a), math.sin(a)
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def place(points, kind, turn, flip, big=False):
    """Rotates and scales the model into Minecraft model space for its kind. Returns points and voxel size."""
    p = points - (points.min(axis=0) + points.max(axis=0)) / 2
    if flip:
        p = p @ rotation("z", 180).T
    p = p @ rotation("y", turn).T
    if kind == "tool":
        # Long axis (Y) goes bottom-left to top-right, like the flat sprite of a vanilla sword or pickaxe.
        p = p @ rotation("z", -45).T
    # Centre again: rotating moves the middle of the bounding box.
    p = p - (p.min(axis=0) + p.max(axis=0)) / 2
    if kind == "tool":
        extent = (p.max(axis=0) - p.min(axis=0))[:2].max()
        p = p * (15.0 / extent)
        p = p + np.array([8.0, 8.0, 8.0])
    elif kind == "head":
        p = p * (16.0 / (p.max(axis=0) - p.min(axis=0)).max())
        p = p + np.array([8.0, 8.0, 8.0])
    elif kind == "crown":
        size = p.max(axis=0) - p.min(axis=0)
        p = p * (14.0 / max(size[0], size[2], size[1] * 1.4))
        p = p + np.array([8.0, 14.0 - p[:, 1].min(), 8.0])
    elif kind == "part":
        p = p * ((32.0 if big else 16.0) / (p.max(axis=0) - p.min(axis=0)).max())
        p = p + np.array([8.0, 8.0, 8.0])
    else:  # held
        p = p * (11.0 / (p.max(axis=0) - p.min(axis=0)).max())
        p = p + np.array([8.0, 8.0, 8.0])
    return p


def voxelise(points, coords, texture, resolution, low, high, dominant=False):
    """Texture colour per voxel on a grid of `resolution` voxels per 16 units, inside [low, high].

    The colour is the average of the surface samples in the voxel or, with `dominant`, the average of only the
    samples of its most common colour: cleaner on textures made of many small patches of different colours,
    where the plain average turns every voxel into a muddy mix.
    """
    voxel = 16.0 / resolution
    origin = np.floor(points.min(axis=0) / voxel) * voxel
    origin = np.maximum(origin, low)
    cells = np.floor((points - origin) / voxel).astype(int)
    shape = tuple(int(math.ceil((min(high, points[:, a].max()) - origin[a]) / voxel)) + 1 for a in range(3))
    keep = np.all((cells >= 0) & (cells < np.array(shape)), axis=1)
    cells, coords = cells[keep], coords[keep]
    tex = np.asarray(texture, dtype=np.float64)
    h, w = tex.shape[:2]
    px = np.clip((np.mod(coords[:, 0], 1.0) * w).astype(int), 0, w - 1)
    py = np.clip((np.mod(coords[:, 1], 1.0) * h).astype(int), 0, h - 1)
    colours = tex[py, px]
    flat = np.ravel_multi_index(cells.T, shape)
    if dominant:
        # Coarse colour bins (16 levels per channel); keep only each voxel's most common bin.
        bins = (colours[:, 0] // 16) * 256 + (colours[:, 1] // 16) * 16 + colours[:, 2] // 16
        keys = flat.astype(np.int64) * 4096 + bins.astype(np.int64)
        unique, inverse, counts_per_key = np.unique(keys, return_inverse=True, return_counts=True)
        voxel_of_key = unique // 4096
        order = np.lexsort((-counts_per_key, voxel_of_key))
        first = np.ones(len(order), bool)
        first[1:] = voxel_of_key[order][1:] != voxel_of_key[order][:-1]
        best_key = np.zeros(len(unique), bool)
        best_key[order[first]] = True
        keep = best_key[inverse]
        flat, colours = flat[keep], colours[keep]
    sums = np.zeros((np.prod(shape), 3))
    counts = np.zeros(np.prod(shape))
    np.add.at(sums, flat, colours)
    np.add.at(counts, flat, 1)
    filled = (counts > 0).reshape(shape)
    average = (sums / np.maximum(counts, 1)[:, None]).reshape(shape + (3,))
    return filled, average, origin, voxel


def solid_interior(filled):
    """Voxels the outside cannot reach count as solid, so the hollow inside of the shell adds no faces."""
    shape = filled.shape
    outside = np.zeros(shape, bool)
    queue = deque()
    for x in range(shape[0]):
        for y in range(shape[1]):
            for z in range(shape[2]):
                if (x in (0, shape[0] - 1) or y in (0, shape[1] - 1) or z in (0, shape[2] - 1)) and not filled[x, y, z]:
                    outside[x, y, z] = True
                    queue.append((x, y, z))
    while queue:
        x, y, z = queue.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nx, ny, nz = x + dx, y + dy, z + dz
            if 0 <= nx < shape[0] and 0 <= ny < shape[1] and 0 <= nz < shape[2] and not filled[nx, ny, nz] and not outside[nx, ny, nz]:
                outside[nx, ny, nz] = True
                queue.append((nx, ny, nz))
    return ~outside


def quantise(filled, average, colours):
    """Reduces the voxel colours to a small palette. Returns palette (list of RGB) and an index grid (-1 = empty)."""
    values = average[filled].astype(np.uint8)
    strip = Image.fromarray(values.reshape(1, -1, 3), "RGB")
    reduced = strip.quantize(colors=colours, method=Image.Quantize.MEDIANCUT)
    palette = np.array(reduced.getpalette()[:colours * 3]).reshape(-1, 3)
    index = np.full(filled.shape, -1, dtype=int)
    index[filled] = np.asarray(reduced).reshape(-1)
    used = sorted(set(index[filled].tolist()))
    remap = {old: new for new, old in enumerate(used)}
    index[filled] = [remap[i] for i in index[filled].tolist()]
    return [tuple(int(c) for c in palette[i]) for i in used], index


def boxes(index, solid):
    """Greedy merge of same-colour voxels into boxes."""
    shape = index.shape
    done = np.zeros(shape, bool)
    result = []
    for z in range(shape[2]):
        for y in range(shape[1]):
            for x in range(shape[0]):
                colour = index[x, y, z]
                if colour < 0 or done[x, y, z]:
                    continue
                x2 = x
                while x2 + 1 < shape[0] and index[x2 + 1, y, z] == colour and not done[x2 + 1, y, z]:
                    x2 += 1
                y2 = y
                while y2 + 1 < shape[1] and np.all(index[x:x2 + 1, y2 + 1, z] == colour) and not done[x:x2 + 1, y2 + 1, z].any():
                    y2 += 1
                z2 = z
                while z2 + 1 < shape[2] and np.all(index[x:x2 + 1, y:y2 + 1, z2 + 1] == colour) and not done[x:x2 + 1, y:y2 + 1, z2 + 1].any():
                    z2 += 1
                done[x:x2 + 1, y:y2 + 1, z:z2 + 1] = True
                result.append(((x, y, z), (x2 + 1, y2 + 1, z2 + 1), colour))
    return result


def face_hidden(solid, lo, hi, axis, direction):
    """True if every voxel touching this face of the box is solid."""
    plane = hi[axis] if direction > 0 else lo[axis] - 1
    if plane < 0 or plane >= solid.shape[axis]:
        return False
    region = [slice(lo[a], hi[a]) for a in range(3)]
    region[axis] = slice(plane, plane + 1)
    return bool(solid[tuple(region)].all())


def build_model(index, solid, palette, origin, voxel, texture_id, kind, big=False):
    elements = []
    for lo, hi, colour in boxes(index, solid):
        uv_x, uv_y = colour % 16, colour // 16
        uv = [uv_x + 0.25, uv_y + 0.25, uv_x + 0.75, uv_y + 0.75]
        faces = {}
        for name, (axis, direction) in FACES.items():
            if not face_hidden(solid, lo, hi, axis, direction):
                faces[name] = {"uv": uv, "texture": "#palette"}
        if faces:
            elements.append({
                "from": [round(float(origin[a] + lo[a] * voxel), 4) for a in range(3)],
                "to": [round(float(origin[a] + hi[a] * voxel), 4) for a in range(3)],
                "faces": faces,
            })
    display = HANDHELD_DISPLAY if kind == "tool" else BLOCK_DISPLAY if kind in ("head", "crown") else {
        # As an item (spawn eggs, advancement icons) a creature is turned like a block, so its face shows.
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    }
    if big:
        # Twice the size: half the scale wherever the model is shown as an item.
        display = {name: dict(t, scale=[v / 2 for v in t["scale"]]) for name, t in display.items()}
    return {
        "textures": {"palette": texture_id, "particle": texture_id},
        "elements": elements,
        "display": display,
    }


def build_cube_model(filled, average, origin, voxel, texture_id):
    """One block-sized box whose six faces show the model as seen from each side, like a vanilla block.

    Used for heads: a single box renders far cheaper than hundreds of voxels on every mob that wears it.
    Returns the model and its 64x64 texture (one 16x16 tile per face).
    """
    shape = filled.shape
    fallback = average[filled].mean(axis=0)

    def project(order):
        """order: (depth axis, depth reversed, row axis, row reversed, column axis, column reversed)."""
        depth_axis, depth_rev, row_axis, row_rev, col_axis, col_rev = order
        rows, cols = shape[row_axis], shape[col_axis]
        tile = np.zeros((rows, cols, 3))
        for r in range(rows):
            for c in range(cols):
                idx = [0, 0, 0]
                idx[row_axis] = rows - 1 - r if row_rev else r
                idx[col_axis] = cols - 1 - c if col_rev else c
                colour = fallback
                depths = range(shape[depth_axis] - 1, -1, -1) if depth_rev else range(shape[depth_axis])
                for d in depths:
                    idx[depth_axis] = d
                    if filled[tuple(idx)]:
                        colour = average[tuple(idx)]
                        break
                tile[r, c] = colour
        return Image.fromarray(tile.astype(np.uint8)).resize((16, 16), Image.NEAREST)

    # Each face as seen from outside: side faces with up = +Y, the top with north up.
    tiles = {
        "north": project((2, False, 1, True, 0, True)),
        "south": project((2, True, 1, True, 0, False)),
        "east": project((0, True, 1, True, 2, True)),
        "west": project((0, False, 1, True, 2, False)),
        "up": project((1, True, 2, False, 0, False)),
        "down": project((1, False, 2, True, 0, False)),
    }
    atlas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    faces = {}
    for i, (name, tile) in enumerate(tiles.items()):
        tx, ty = (i % 4) * 16, (i // 4) * 16
        atlas.paste(tile, (tx, ty))
        faces[name] = {"uv": [tx / 4, ty / 4, tx / 4 + 4, ty / 4 + 4], "texture": "#palette"}
    lo = np.argwhere(filled).min(axis=0)
    hi = np.argwhere(filled).max(axis=0) + 1
    element = {
        "from": [round(float(origin[a] + lo[a] * voxel), 4) for a in range(3)],
        "to": [round(float(origin[a] + hi[a] * voxel), 4) for a in range(3)],
        "faces": faces,
    }
    model = {"textures": {"palette": texture_id, "particle": texture_id}, "elements": [element], "display": BLOCK_DISPLAY}
    return model, atlas


def palette_texture(palette):
    """Each colour fills a 4x4 block of a 64x64 texture, so mipmaps do not blend neighbouring colours."""
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i, colour in enumerate(palette):
        bx, by = (i % 16) * 4, (i // 16) * 4
        for dx in range(4):
            for dy in range(4):
                image.putpixel((bx + dx, by + dy), colour + (255,))
    return image


def preview(index, palette, path):
    """Front (from the south), side (from the east) and top views of the voxels, side by side."""
    colours = np.array(palette + [(40, 40, 48)], dtype=np.uint8)

    def first_hit(grid):
        first = np.full(grid.shape[:2], -1)
        for depth in range(grid.shape[2]):
            layer = grid[..., depth]
            first = np.where((first < 0) & (layer >= 0), layer, first)
        return first

    front = first_hit(index[:, :, ::-1]).T[::-1]                    # rows: Y top first, columns: X
    side = first_hit(np.moveaxis(index, 0, -1)[:, :, ::-1])[::-1, ::-1]  # rows: Y top first, columns: Z from south to north
    top = first_hit(np.moveaxis(index, 1, -1)[:, :, ::-1]).T        # rows: Z from north, columns: X
    views = [Image.fromarray(np.ascontiguousarray(colours[np.where(v < 0, len(palette), v)])) for v in (front, side, top)]
    scale = 8
    width = sum(v.width for v in views) * scale + 16 * (len(views) + 1)
    height = max(v.height for v in views) * scale + 32
    sheet = Image.new("RGB", (width, height), (24, 24, 30))
    x = 16
    for v in views:
        sheet.paste(v.resize((v.width * scale, v.height * scale), Image.NEAREST), (x, 16))
        x += v.width * scale + 16
    sheet.save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("input")
    parser.add_argument("kind", choices=["tool", "head", "crown", "held", "part"])
    parser.add_argument("model")
    parser.add_argument("texture")
    parser.add_argument("--texture-id", required=True)
    parser.add_argument("--resolution", type=int, default=16, help="voxels per 16 model units")
    parser.add_argument("--colors", type=int, default=32)
    parser.add_argument("--turn", type=float, default=0.0, help="rotate around the vertical axis first, in degrees")
    parser.add_argument("--flip", action="store_true", help="turn the model upside down first")
    parser.add_argument("--big", action="store_true", help="part kind only: fill 32 units instead of 16")
    parser.add_argument("--dominant", action="store_true", help="colour each voxel by its most common texture colour")
    parser.add_argument("--preview")
    args = parser.parse_args()

    positions, uvs, triangles, texture = load_glb(args.input)
    if args.big and args.kind != "part":
        raise SystemExit("--big only works with the part kind")
    placed = place(positions, args.kind, args.turn, args.flip, args.big)
    voxel = 16.0 / args.resolution
    points, coords = sample_surface(placed, uvs, triangles, voxel / 2)
    filled, average, origin, voxel = voxelise(points, coords, texture, args.resolution, -16.0, 32.0, args.dominant)
    palette, index = quantise(filled, average, args.colors)
    if args.kind == "head":
        model, image = build_cube_model(filled, average, origin, voxel, args.texture_id)
    else:
        model = build_model(index, solid_interior(filled), palette, origin, voxel, args.texture_id, args.kind, args.big)
        image = palette_texture(palette)
    with open(args.model, "w") as f:
        json.dump(model, f, separators=(",", ":"))
    image.save(args.texture)
    print(f"{args.input.rsplit('/', 1)[-1]}: {len(triangles)} triangles -> {int(filled.sum())} voxels, "
          f"{len(model['elements'])} boxes, {len(palette)} colours")
    if args.preview:
        preview(index, palette, args.preview)


if __name__ == "__main__":
    main()
