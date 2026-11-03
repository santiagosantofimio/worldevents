"""Converts the mod's resources (the 1.21.4+ formats) for Minecraft 1.18.2 or 1.16.5.

    python3 tools/legacy_resources.py <modern resources dir> <vanilla client jar> <output resources dir> <1.18|1.16>

The output folder is replaced. What changes:

- Item looks: 1.21.4+ picks models in assets/minecraft/items/*.json by a string custom_model_data. Old versions pick
  them with numeric CustomModelData "overrides" in the vanilla item model (assets/minecraft/models/item). The number
  is EventItems.modelNumber in Java: a hash of "worldevents:<id>", worked out the same way here. Tools and weapons
  use their 2D model everywhere: old versions cannot pick a model by where the item is shown.
- Creature models get a "head" transform: on old versions each creature part is worn on an armor stand's head
  (Creature.java), and the transform gives it the size the part has in Java (PART_SCALES below).
- Advancements: data/worldevents/advancements, with icons and item conditions in NBT, and plain English titles
  (old versions have no translatable text with a fallback, and vanilla clients do not have the mod's translations).
- Base items that do not exist in the old version are swapped (ITEM_SWAPS), as in EventItems.java.
- The creative tab's name gets the key old Fabric and Forge versions look for.
"""
import json
import os
import shutil
import sys
import zipfile

MOD_ID = "worldevents"

# How big each creature part is, in blocks per 16 model units: the Creature.Part scale in the mob classes.
PART_SCALES = {
    "blood_moon_lord": 1.7,
    "blood_moon_lord_crown": 1.0,
    "blood_eye": 1.0,
    "mimic_sack": 0.8,
    "living_meteorite": 1.1,
    "storm_spirit": 1.3,
    "harvest_spirit": 0.9,
    "nomad_merchant": 1.0,
    "horde_warlord": 1.45,
    "spectral_captain": 1.05,
    "magma_colossus": 2.0,
    "void_stalker": 1.05,
}
# A worn head item is drawn at 0.625 of its model's size.
HEAD_ITEM_SCALE = 0.625

ITEM_SWAPS = {
    "1.18": {
        "mace": "netherite_axe",
        "prize_pottery_sherd": "nautilus_shell",
        "allay_spawn_egg": "parrot_spawn_egg",
    },
    "1.16": {
        "mace": "netherite_axe",
        "prize_pottery_sherd": "nautilus_shell",
        "allay_spawn_egg": "parrot_spawn_egg",
        "raw_iron": "iron_nugget",
        "raw_gold_block": "gold_block",
    },
}


def java_hash(text):
    """Java's String.hashCode."""
    h = 0
    for ch in text:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h - (1 << 32) if h & 0x80000000 else h


def model_number(model_id):
    """EventItems.modelNumber: Math.floorMod(("worldevents:" + id).hashCode(), 9_000_000) + 1_000_000."""
    return java_hash(MOD_ID + ":" + model_id) % 9_000_000 + 1_000_000


def strip_namespace(ref):
    return ref.split(":", 1)[1] if ":" in ref else ref


def first_model(node):
    """The model a 1.21.4 item model definition shows; for a choice by display context, the GUI's."""
    if node.get("type") == "minecraft:model":
        return node["model"]
    if node.get("type") == "minecraft:select" and node.get("property") == "minecraft:display_context":
        for case in node["cases"]:
            whens = case["when"] if isinstance(case["when"], list) else [case["when"]]
            if "gui" in whens:
                return first_model(case["model"])
        return first_model(node["fallback"])
    raise ValueError("unsupported item model: " + json.dumps(node)[:200])


def convert_items(modern, client, out, version, swaps):
    """Vanilla item models with an override for each of the mod's looks of that item."""
    items_dir = os.path.join(modern, "assets", "minecraft", "items")
    overrides = {}
    numbers = {}
    for name in sorted(os.listdir(items_dir)):
        item = name[:-len(".json")]
        definition = json.load(open(os.path.join(items_dir, name)))["model"]
        assert definition.get("property") == "minecraft:custom_model_data", name
        legacy_item = swaps.get(item, item)
        for case in definition["cases"]:
            model_id = strip_namespace(case["when"])
            number = model_number(model_id)
            assert numbers.setdefault(number, model_id) == model_id, "two models share the number %d" % number
            overrides.setdefault(legacy_item, {})[number] = first_model(case["model"])
    jar = zipfile.ZipFile(client)
    for item, cases in overrides.items():
        try:
            vanilla = json.loads(jar.read("assets/minecraft/models/item/%s.json" % item))
        except KeyError:
            sys.exit("%s has no item %s: add it to ITEM_SWAPS" % (version, item))
        # Overrides are checked in order and the last one that matches wins: "custom_model_data": n matches n and up.
        vanilla["overrides"] = [{"predicate": {"custom_model_data": number}, "model": model}
                                for number, model in sorted(cases.items())]
        write_json(os.path.join(out, "assets", "minecraft", "models", "item", item + ".json"), vanilla)
    return len(overrides)


def add_head_transforms(out):
    """Creature models, as worn on an armor stand's head, at the part's size."""
    models = os.path.join(out, "assets", MOD_ID, "models", "item")
    for model_id, scale in PART_SCALES.items():
        path = os.path.join(models, model_id + ".json")
        model = json.load(open(path))
        size = round(scale / HEAD_ITEM_SCALE, 4)
        assert size <= 4.0, model_id + " is too big for a head transform"
        model.setdefault("display", {})["head"] = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [size, size, size]}
        with open(path, "w") as f:
            json.dump(model, f, separators=(",", ":"))


def icon(node, swaps):
    item = strip_namespace(node["id"])
    result = {"item": "minecraft:" + swaps.get(item, item)}
    strings = node.get("components", {}).get("minecraft:custom_model_data", {}).get("strings", [])
    if strings:
        result["nbt"] = "{CustomModelData:%d}" % model_number(strip_namespace(strings[0]))
    return result


def text(component):
    return {"text": component["fallback"]} if "fallback" in component else component


def item_predicate(predicate, version, swaps):
    item = strip_namespace(predicate["items"])
    item = "minecraft:" + swaps.get(item, item)
    result = {"items": [item]} if version == "1.18" else {"item": item}
    data = predicate.get("predicates", {}).get("minecraft:custom_data")
    if data:
        result["nbt"] = "{" + ",".join('%s:"%s"' % (key, value) for key, value in data.items()) + "}"
    return result


def convert_advancements(modern, out, version, swaps):
    source = os.path.join(modern, "data", MOD_ID, "advancement")
    target = os.path.join(out, "data", MOD_ID, "advancements")
    for name in sorted(os.listdir(source)):
        advancement = json.load(open(os.path.join(source, name)))
        display = advancement.get("display")
        if display:
            display["icon"] = icon(display["icon"], swaps)
            display["title"] = text(display["title"])
            display["description"] = text(display["description"])
            if "background" in display:
                display["background"] = display["background"].replace("minecraft:gui/", "minecraft:textures/gui/") + ".png"
        for criterion in advancement.get("criteria", {}).values():
            conditions = criterion.get("conditions", {})
            if "items" in conditions:
                conditions["items"] = [item_predicate(p, version, swaps) for p in conditions["items"]]
        write_json(os.path.join(target, name), advancement)
    return len(os.listdir(source))


def add_tab_name(out):
    """Old Fabric and Forge versions name the creative tab itemGroup.worldevents.items."""
    lang = os.path.join(out, "assets", MOD_ID, "lang")
    for name in os.listdir(lang):
        path = os.path.join(lang, name)
        table = json.load(open(path))
        result = {}
        for key, value in table.items():
            result[key] = value
            if key == "itemGroup." + MOD_ID:
                result["itemGroup.%s.items" % MOD_ID] = value
        write_json(path, result)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent="\t", ensure_ascii=False)
        f.write("\n")


def main():
    modern, client, out, version = sys.argv[1:5]
    swaps = ITEM_SWAPS[version]
    if os.path.exists(out):
        shutil.rmtree(out)
    shutil.copytree(modern, out, ignore=shutil.ignore_patterns("items", "advancement", "fabric.mod.json"))
    items = convert_items(modern, client, out, version, swaps)
    add_head_transforms(out)
    advancements = convert_advancements(modern, out, version, swaps)
    add_tab_name(out)
    print("%s: %d item models with overrides, %d creature head transforms, %d advancements"
          % (version, items, len(PART_SCALES), advancements))


if __name__ == "__main__":
    main()
