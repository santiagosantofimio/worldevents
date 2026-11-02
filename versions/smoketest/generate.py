"""Writes the smoke test datapack's functions. Run with: python3 generate.py [legacy]

"legacy" writes the item syntax of 1.16.5 and 1.18.2 (NBT tags instead of item components)."""
import json, os, sys

legacy = len(sys.argv) > 1 and sys.argv[1] == "legacy"
here = os.path.dirname(os.path.abspath(__file__))
folder = os.path.join(here, "datapack", "data", "smoke", "functions" if legacy else "function")
tags = os.path.join(here, "datapack", "data", "minecraft", "tags", "functions" if legacy else "function")
for old in ("function", "functions"):
    for base in (os.path.join(here, "datapack", "data", "smoke", old), os.path.join(here, "datapack", "data", "minecraft", "tags", old)):
        if os.path.isdir(base):
            for name in os.listdir(base):
                os.remove(os.path.join(base, name))
            os.rmdir(base)
os.makedirs(folder)
os.makedirs(tags)

kinds = ["blood_moon_lord", "blood_eye", "mimic_sack", "living_meteorite", "storm_spirit", "harvest_spirit",
         "nomad_merchant", "horde_warlord", "spectral_captain", "magma_colossus", "void_stalker"]
events = [("blood_moon", "overworld"), ("gold_rush", "overworld"), ("meteor_shower", "overworld"),
          ("thunderstorm", "overworld"), ("blessed_harvest", "overworld"), ("merchant_caravan", "overworld"),
          ("horde", "overworld"), ("treasure_hunt", "overworld"), ("eruption", "the_nether"), ("void_storm", "the_end")]
if legacy:
    pickaxe = 'minecraft:diamond_pickaxe{worldevents_item:"starfall_pickaxe"}'
else:
    pickaxe = 'minecraft:diamond_pickaxe[minecraft:custom_data={worldevents_item:"starfall_pickaxe"}]'

steps = []
steps.append(["say [smoke] start", "forceload add ~-32 ~-32 ~32 ~32", "worldevents list", "worldevents status",
              "time set night"]
             + [f"worldevents summon {k} ~{(i % 4) * 3} ~3 ~{(i // 4) * 3}" for i, k in enumerate(kinds)]
             # The starfall pickaxe smelts what it mines: checks the loot hook.
             + ["setblock ~ ~20 ~ minecraft:iron_ore", f"loot spawn ~ ~22 ~ mine ~ ~20 ~ {pickaxe}"])
steps.append(['execute if entity @e[type=item,nbt={Item:{id:"minecraft:iron_ingot"}}] run say [smoke] loot hook ok',
              "kill @e[type=!player]", "say [smoke] killed creatures"])
for event, dim in events:
    steps.append([f"say [smoke] start {event}", f"execute in minecraft:{dim} run worldevents start {event}"])
    steps.append([f"execute in minecraft:{dim} run worldevents status", f"execute in minecraft:{dim} run worldevents stop",
                  f"say [smoke] stopped {event}"])
steps.append(["worldevents reload", "say [smoke] done", "stop"])

for i, lines in enumerate(steps):
    lines = list(lines)
    if i + 1 < len(steps):
        lines.append(f"schedule function smoke:step{i + 1} 40t")
    with open(os.path.join(folder, f"step{i}.mcfunction"), "w") as f:
        f.write("\n".join(lines) + "\n")
with open(os.path.join(folder, "load.mcfunction"), "w") as f:
    f.write("schedule function smoke:step0 100t\n")
with open(os.path.join(tags, "load.json"), "w") as f:
    json.dump({"values": ["smoke:load"]}, f, indent=1)
