say [smoke] start
forceload add ~-32 ~-32 ~32 ~32
worldevents list
worldevents status
time set night
worldevents summon blood_moon_lord ~0 ~3 ~0
worldevents summon blood_eye ~3 ~3 ~0
worldevents summon mimic_sack ~6 ~3 ~0
worldevents summon living_meteorite ~9 ~3 ~0
worldevents summon storm_spirit ~0 ~3 ~3
worldevents summon harvest_spirit ~3 ~3 ~3
worldevents summon nomad_merchant ~6 ~3 ~3
worldevents summon horde_warlord ~9 ~3 ~3
worldevents summon spectral_captain ~0 ~3 ~6
worldevents summon magma_colossus ~3 ~3 ~6
worldevents summon void_stalker ~6 ~3 ~6
setblock ~ ~20 ~ minecraft:iron_ore
loot spawn ~ ~22 ~ mine ~ ~20 ~ minecraft:diamond_pickaxe{worldevents_item:"starfall_pickaxe"}
schedule function smoke:step1 40t
