# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [2.1.0] - Unreleased

Three new events, presets to tune how much combat you want, and personal statistics, on all five Minecraft versions.

### Added

- **Piglin Warband** (`piglin_warband`, Nether): three waves of piglins, brutes and hoglins, led in the last wave by the
  **Piglin Warchief** (150-160 health, calls two brutes at half health). Golden armor calms regular piglins but not the
  brutes or the Warchief. Drops gold and, sometimes, the **Warchief's Cleaver** (use it for Absorption and Fire Resistance).
- **Tide of the Drowned** (`drowned_tide`, overworld coast at night): three waves of drowned come out of the water near
  players standing on a coast, led by **The Tidecaller** (pulls nearby players toward it with an undertow). Drops prismarine
  and, sometimes, the **Tidecaller's Conch** (use it for Conduit Power, Dolphin's Grace and Water Breathing). Ends at dawn.
- **Fishing Festival** (`fishing_festival`, overworld daytime near water): a calm event with Luck while it lasts, experience
  and points for every catch (up to a configurable limit per player) and an advancement at the goal.
- **Presets**: `preset` in `config/worldomens.json` (`calm`, `normal` or `epic`) and `/worldomens preset [name]` (operators change it).
  Calm makes events less frequent and favours peaceful ones; epic does the opposite. Your own values are kept.
- `/worldomens stats`: your events taken part in, points, seals and bosses defeated, saved across restarts.
- The `/omens` alias (the 2.0.0 notes promised it, but it was missing).
- The Nomad Merchant can now trade the two new relics for event seals. New advancements and Spanish and English texts for everything.

### Fixed

- In the 1.18.2 and 1.16.5 resource packs the models of relics, creatures and spawn eggs did not apply: their model numbers
  were still computed from the old mod id. The packs are regenerated with the right numbers.

### Changed

- Every comment was removed from the code and configuration files.

## [2.0.0] - Unreleased

The mod id, package and command now match the name: everything that was `worldevents` is `worldomens`.

### Changed

- **Breaking:** the mod id is `worldomens`. The command is `/worldomens` (the alias `/eventos` stays), the
  config file is `config/worldomens.json`, and data and resource pack namespaces use `worldomens:`.
- Jar files are named `worldomens-<loader>-<minecraft>-<version>.jar`.
- The resource pack must be updated together with the mod: the old pack uses the old namespace and will not apply.

### Migration

- On first start, `config/worldevents.json` is moved to `config/worldomens.json` with its values.
- Each dimension's saved event state (running event, planned event and cooldowns) is copied from the old data.
- Event relics and creature spawn eggs you already own still work.
- Not migrated: advancement progress (the advancements have new ids), creatures alive at the moment of the update, and
  the custom model of items you already own until they are given again.

## [1.2.0] - Unreleased

### Added

- **Firefly Night** (`firefly_night`, on all supported Minecraft versions): a five-minute
  nighttime search in the overworld, including Peaceful difficulty. Follow personal green
  lights on nearby open ground with direction, distance and progress hints in English or
  Spanish. Each find grants 5 XP and 2 event points; finding all five grants an advancement.
  Progress survives restarts, and each player has their own lights and reward limit.
  The event ends at dawn and never changes blocks or creates creatures. Vanilla nighttime
  dangers still apply. Config defaults: `lightsPerPlayer` 5 (limited to 1–20) and
  `experiencePerLight` 5 (limited to 0–100), weight 10, cooldown 4800 s, duration 300 s.
  Existing config files receive the new section automatically.
- Added `/worldevents help [id]` and `/eventos help [id]` in English and Spanish.
  With no id, it describes the active event in the current dimension. Players can use help without operator permissions. With an id, it
  explains that event’s objective, reward and conditions without changing world state.
- **Caravan contracts**: players near an active caravan merchant can deliver 48 wheat, 12 iron ingots or 64 rotten flesh for one event seal. `/worldevents contract [id]` and `/eventos encargo [id]` show or complete these deliveries without operator permissions. Each player can complete two per caravan; progress survives server restarts. Creative and spectator players cannot claim seals.
- `/worldevents ranking` and `/eventos ranking` show the current event’s top three scores and your own score and rank. Everyone can use them while an event is active.

- NeoForge and Forge builds for Minecraft 26.3, with the same content as the Fabric build.

### Fixed

- Players on 1.18.2 and 1.16.5 can now use read-only event help without operator permissions.

### Changed

- Meteor Shower shooting stars now start closer and above the player's forward view, then cross toward its center. The existing particle trail and sounds remain; the change makes stars easier to notice without a client mod.
- The Colossus Hammer keeps its dark rock and lava colors in a squarer 16×16 item icon.
- The Nomad Merchant, Harvest Spirit, Horde Warlord and Spectral Captain use their original creature models and textures again.

## [1.1.1] - 2026-11-19

The meteor shower is much easier to notice.

### Changed

- **Meteor Shower**: shooting stars are now thick, glowing lines with a white core and a flame around
  it, lower and closer to the players, twice as frequent and with a whoosh, instead of faint dots high in
  the sky. Meteors fall from higher up with a long fiery tail and a roar, and every impact brings a crack
  of thunder that carries far, more flames and a ring of smoke. Up to 4 meteors per player (was 3).
  The new defaults are `shootingStarInterval` 1, `impactChance` 0.03 and `impactsPerPlayer` 4. A
  `config/worldevents.json` that already exists keeps its old values: delete the `meteor_shower` section
  (or set those three keys) to get the new ones.

## [1.1.0] - 2026-11-16

Two new events, convergences, and a new look for every creature and item.

### Added

- **Eclipse** (`eclipse`, overworld, daytime): the sky goes dark for 5 minutes and the monsters it
  brings walk in the sun without burning. Phantoms dive at players, and after a minute the **Eclipse
  Wraith** (boss) arrives: it swoops at players, blinds those below with darkness pulses and calls
  more phantoms. It can drop the **Eclipse Shard**: right-click for night vision and every monster
  around glows for a while (60 s cooldown). When the sun comes back, what the eclipse brought burns
  again.
- **Blizzard** (`blizzard`, overworld, only when a player is in a snowy biome): snow and wind for
  5 minutes, with strays coming out of the storm. Players out in the open freeze little by little,
  unless they are under a roof, near a fire or wearing leather. After a minute the **Frost Yeti**
  (boss) arrives and its roar chills and slows everyone near it. It can drop **Frostfang**: a sword
  whose hits chill and slow. No block is touched.
- **Convergence**: an event can come as a convergence (8% by default, `convergenceChance` in the
  config; `0` turns it off). It lasts half as long again and gives double points and double seals. The
  boss bar says so.
- New advancements for the two bosses and the two new items, with translations in English and Spanish.

### Changed

- Every creature has a new model in a vanilla style, made of a few blocks with Minecraft-like
  textures, in every version of the mod. The Living Meteorite is unchanged.
- New 16x16 icons for the mod's items, with vanilla-style lighting.
- 1.18.2: the Eclipse Shard is an amethyst shard (there are no echo shards); 1.16.5: a prismarine
  shard. In 1.16.5 there is no freezing, so the blizzard's cold slows players and bites now and
  then instead.

## [1.0.1] - 2026-11-03

Fixes from players' feedback.

### Changed

- The mod is called **World Omens** in game too, as on its pages: in the mod list, the creative tab,
  the advancement tab and the resource pack (also in Spanish). Its id (`worldevents`), the
  `/worldevents` command and `config/worldevents.json` stay the same, so worlds and configs carry on.

### Fixed

- **Captain's Cutlass**: its lunge now moves the player. The push only happened on the server and
  never reached the player's game, so the lunge did nothing but start its cooldown. The same went
  for the Magma Colossus's slam and the eruption's geysers, which now also throw players.
  (26.3, 26.1.2 and 1.21.11; 1.18.2 and 1.16.5 were fine.)
- **Magma Colossus**: it attacks. It now always goes after the nearest player within 32 blocks,
  even one it can't see, and no longer calms down and lets go of its prey near nether portals,
  respawn anchors or warped fungus, as a vanilla hoglin does.
- **Magma Colossus**: it no longer burns or takes damage from fire and lava (hidden, permanent fire
  resistance; colossi from 1.0.0 get it too).
- **Magma Colossus**: it no longer spawns stuck half inside the ground on uneven terrain or inside a
  hill. Twice a hoglin's size, it now looks for a spot with room for it, up to 10 blocks away, and
  gets out by itself if it ever ends up inside blocks.
- **Magma Colossus** (1.18.2 and 1.16.5): its ground slam never went off; now it does.
- Relic weapons (Blood Scythe, Starfall Pickaxe, Warlord's Axe, Captain's Cutlass and Colossus
  Hammer) show their own sprite in hand, like vanilla tools, instead of 3D models that were too big
  and blocky in first person and odd in other players' hands. 1.18.2 and 1.16.5 already did.

## [1.0.0] - 2026-11-01

First public release.

### Added

- Event system: one event at a time per dimension. After a global cooldown, the next
  event is drawn by weight among those available in the dimension and waits for its
  moment (night, enough players) before starting. Per-event cooldowns stop repeats.
- Boss bar with the event name and time left, shown in each player's client language.
  Players who join or change dimension mid-event see it and get a chat notice.
- The active event, the planned next event and all cooldowns are saved with the world
  and survive restarts.
- `config/worldevents.json`: `enabled`, `weight`, `cooldown` and `duration` per event
  (times in seconds), plus each event's own values. Missing keys are added on load
  without touching existing ones; a broken file is never overwritten.
- `/worldevents` (alias `/eventos`, permission level 2): `list`, `start <id>`, `stop`,
  `status`, `reload`. `start` refuses events that cannot happen in the current dimension.
- Server-side translations for `en_us` and `es_es`; vanilla clients see translated text.
- **Blood Moon** (`blood_moon`): a night with extra hostile mobs around players. Hostile
  mobs get more health and damage, and drop double loot and XP when killed by a player.
  Ends at dawn, darkens the sky through the boss bar, adds a faint red mist and blocks
  sleeping (all configurable). No extra mobs inside spawn protection.
- **Gold Rush** (`gold_rush`): for 5 minutes, ores mined by players drop double, with a
  golden sparkle and a chime. Silk touch drops are not multiplied.
- **Meteor Shower** (`meteor_shower`): shooting stars across the night sky, and a few
  meteors that fall near players and leave a small crater lined with ores around a magma
  core. Craters only dig natural terrain and never touch blocks with a block entity,
  liquids or spawn protection. With block damage off, meteors only drop the ores.
- **Thunderstorm** (`thunderstorm`): rain and thunder, visual-only lightning around players,
  creepers charged by the storm and storm spirits. The sky clears when it ends.
- **Blessed Harvest** (`blessed_harvest`): for 5 minutes in the daytime, crops around players
  grow by themselves and ripe crops drop double. It only makes crops grow; no blocks are placed
  or broken.
- **Merchant Caravan** (`merchant_caravan`): for 10 minutes in the daytime, a nomad merchant and
  their llamas camp near players and sell the events' items and rare vanilla goods for emeralds.
- **Horde** (`horde`): three waves of monsters at night, each bigger than the last, the third led
  by a boss. No creepers, zombies never break doors, monsters only come out of the dark, and the
  wave number shows in the boss bar. Players who help beat it get XP; leftovers leave at dawn.
- **Creatures** with their own 3D models, made for vanilla clients: an invisible vanilla mob
  does the moving and fighting, and item displays riding it show the model from the resource
  pack (a floating block without it). The Blood Moon Lord (boss), the Blood Eye, the Mimic
  Sack (gold rush; one hit and it bursts into gold), the Living Meteorite (climbs out of
  craters), the Storm Spirit (calls lightning), the Harvest Spirit (friendly; follows a player
  and leaves golden seeds), the Nomad Merchant (trades like a vanilla merchant) and the Horde
  Warlord (boss; its war cry spurs the horde on).
- **Items**, built on vanilla items with their own name, description and effects: Meteorite
  Fragment, Lucky Nugget, Bloody Tear (summons a blood moon at night), Blood Scythe
  (lifesteal), Starfall Pickaxe (smelts ores as it mines), Bottled Lightning (calls a
  lightning bolt where you look), Golden Seed (ripens the crops around at once) and Warlord's
  Axe (war cry: Strength and Speed for nearby players, with a cooldown shown on the axe).
- An advancement tab for the events.
- `/worldevents give <players> <item> [count]`.
- A spawn egg for every creature (the vanilla egg of its base mob, which brings out the real
  creature) and `/worldevents summon <creature> [pos]`. Summoned creatures belong to no event:
  they stay until defeated, with their abilities, boss bar and loot.
- A creative tab with every item and egg, for games with the mod on the client (singleplayer).
  Vanilla clients don't see it; `/worldevents give` hands out the same items and eggs.
- Optional resource pack (`./gradlew resourcePack`) with 2D item icons, 3D models for the
  scythe and pickaxe in hand and for every creature, and the names of items, creatures and
  advancements in each player's language. Without it everything works with the vanilla look.
- Game tests (`./gradlew runGameTest`, also part of `./gradlew build`) that run the events,
  items and mobs on a real test server, and a client test (`./gradlew runClientGameTest`)
  that saves screenshots of the items and mobs.
- **Event ranking**: players score points during events (kills, ores, crops, trades, bosses).
  When an event ends, the best three are announced and everyone who took part earns
  **Event Seals**, with extra seals for the winner.
- **Treasure Hunt** (`treasure_hunt`): a chest full of loot appears near players, and the action
  bar tells each one how warm they are. The first to open it takes the prize, but the **Spectral
  Captain** (boss; calls cannon fire that never harms the terrain) rises to guard it and the chest
  stays shut until it falls. It may drop the **Captain's Cutlass**, which lunges forward.
- **Eruption** (`eruption`, Nether only): geysers of fire burst around players after a moment's
  warning and the **Magma Colossus** (boss; its slams burn and throw everything around it) comes up
  to hunt them. Nothing touches a block. Monsters killed meanwhile may leave a **Magma Heart**, and
  the colossus may drop the **Colossus Hammer**, a heavy mace that sets its target on fire.
- **Void Storm** (`void_storm`, End only): **Void Stalkers** blink behind their prey and blind it,
  and pulses of the void lift players straight up and let them float back down. Stalkers may drop
  the **Unstable Pearl**, which blinks the player forward.
- Nomad merchants now sell relics (the strongest event items) for event seals.
- Players get a warning a minute before a drawn event starts (`warningSeconds`). Events started
  by command skip it.
