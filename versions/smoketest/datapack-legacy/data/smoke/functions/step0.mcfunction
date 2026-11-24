say [smoke] start
forceload add ~-32 ~-32 ~32 ~32
worldomens list
worldomens status
worldomens help
worldomens help firefly_night
worldomens help blood_moon
worldomens ranking
worldomens contract
time set night
worldomens summon blood_moon_lord ~0 ~3 ~0
worldomens summon blood_eye ~3 ~3 ~0
worldomens summon mimic_sack ~6 ~3 ~0
worldomens summon living_meteorite ~9 ~3 ~0
worldomens summon storm_spirit ~0 ~3 ~3
worldomens summon harvest_spirit ~3 ~3 ~3
worldomens summon nomad_merchant ~6 ~3 ~3
worldomens summon horde_warlord ~9 ~3 ~3
worldomens summon spectral_captain ~0 ~3 ~6
worldomens summon magma_colossus ~3 ~3 ~6
worldomens summon void_stalker ~6 ~3 ~6
worldomens summon eclipse_wraith ~9 ~3 ~6
worldomens summon frost_yeti ~0 ~3 ~9
setblock ~ ~20 ~ minecraft:iron_ore
loot spawn ~ ~22 ~ mine ~ ~20 ~ minecraft:diamond_pickaxe{worldomens_item:"starfall_pickaxe"}
schedule function smoke:step1 40t
