# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [0.2.0] - 2026-10-31

### Added

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

## [0.1.0] - 2026-10-31

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
