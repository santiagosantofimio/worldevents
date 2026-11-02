execute if entity @e[type=item,nbt={Item:{id:"minecraft:iron_ingot"}}] run say [smoke] loot hook ok
kill @e[type=!player]
say [smoke] killed creatures
schedule function smoke:step2 40t
