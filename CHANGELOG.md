# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Event system: one random event at a time per dimension, with a global cooldown,
  per-event cooldowns and a weighted draw among the events whose conditions are met.
- Boss bar with the event name and time left, shown in each player's client language.
  Players who join or change dimension mid-event see it too.
- Active events and cooldowns are saved with the world and survive restarts.
- `config/worldevents.json`: `enabled`, `weight`, `cooldown` and `duration` per event
  (times in seconds), plus each event's own values. Missing keys are added on load
  without touching existing ones.
- `/worldevents` (alias `/eventos`, permission level 2): `list`, `start <id>`, `stop`,
  `status`, `reload`.
- Server-side translations for `en_us` and `es_es`; vanilla clients see translated text.
- **Blood Moon** (`blood_moon`): a night with extra hostile mobs around players. Hostile
  mobs get more health and damage, and drop double loot and XP when killed by a player.
  Ends at dawn, darkens the sky through the boss bar and blocks sleeping (configurable).
- **Gold Rush** (`gold_rush`): for 5 minutes, ores mined by players drop double.
  Silk touch drops are not multiplied.
