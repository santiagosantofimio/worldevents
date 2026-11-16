# World Omens (repo `worldevents`): guide and pending work

Server-side Minecraft mod (vanilla clients can join). Published as **World Omens** on Modrinth and CurseForge;
the internal id stays `worldevents` (command `/worldevents`, `config/worldevents.json`). The author speaks Spanish:
answer in Spanish. Work branch: `claude/focused-gates-zo6ikh`.

## Layout

- `src/`: Minecraft **26.3**, Fabric only (Loom 1.18.4, Java 25). This is the main source: new things go here first.
- `versions/<mc>/`: ports. `common/` holds the code and resources (a copy, adapted), plus one subproject per loader.
  - `26.1.2` and `1.21.11`: Fabric, NeoForge and Forge. Same code as 26.3 except `EntityType` instead of
    `EntityTypes` and `hurtMarked` instead of `syncVelocity` (velocity sent to the player's client).
  - `1.18.2` and `1.16.5`: Fabric and Forge (standalone Forge build in `forge/`, called by `forgeBuild`).
    Creatures are invisible armor stands with the model on the head (`tools/legacy_resources.py`
    `add_head_transforms`); items carry NBT plus numeric CustomModelData; no `syncVelocity` (use `hurtMarked`),
    `getTarget()` is null on brain-driven mobs (hoglin), 1.16.5 has no freezing (`ticksFrozen`), no echo shard,
    no darkness, and `yRot` is a field.
  - Ports use `Hooks` (platform-neutral events) instead of the Fabric API directly.
- `dist/<mc>/`: files ready to upload (`./gradlew dist` at the root for 26.3,
  `./gradlew -p versions/<mc> dist` for the rest). `dist/UPLOAD.md`: names, versions and changelogs (Markdown for
  Modrinth, plain text for CurseForge). `CHANGELOG.md`.
- `tools/creature_models.py`: generates every creature's model and texture (a few boxes, vanilla style).
  Run `python3 tools/creature_models.py <resources dir>` for each version; for 1.18.2 and 1.16.5 then apply
  `legacy_resources.add_head_transforms`.
- `tools/item_icons.py`: 16x16 icons. `tools/render_model.py`: preview of a model without the game.

## Build and test

- Gradle runs on **Java 25** (`JAVA_HOME=<jdk25>`); JDKs 8, 17 and 21 must be installed too.
- Server tests: `./gradlew runGameTest` (root) and `./gradlew -p versions/<v> :fabric:runGameTest`.
  Two old tests are flaky under load or timing: horde (chunk loading) and bloody tear (time of day on 26.1.2).
  They pass on a rerun.
- Client test with screenshots: `./gradlew runClientGameTest`. Headless in the cloud it was run with
  `xvfb-run` plus Mesa, and on 26.3 with `SDL_VIDEO_FORCE_EGL=1`. Screenshots land in
  `build/run/clientGameTest/screenshots`.
- Commits end with the session's `Co-Authored-By` attribution. Don't open a PR unless asked.

## Released status

- **1.0.1** (in `dist/`, maybe already uploaded): Captain's Cutlass lunge fix (the velocity never reached the
  client), Magma Colossus fixes (attacks, doesn't burn, gets out of blocks), 2D weapons in hand, name changed to
  World Omens. Everything verified on 10 real servers.

## Roadmap for 1.1 (the author asked for "do them all")

1. [done, commit 3450ba9] 11 creatures redone in vanilla style, in every version.
2. [done, commit 8998524] 16x16 icons in every version.
3. **[IN PROGRESS] Phase 3a: Convergence, Eclipse and Blizzard. Only in `src/` (26.3), not ported.**
   - Convergence: `EventManager.start(level, event, convergence)`, `convergenceChance` (0.08) in ModConfig;
     the event lasts x1.5 with double points and seals. The bar shows "Convergence: <event>".
   - Eclipse (`event/impl/EclipseEvent.java`, boss `mob/EclipseWraith.java` on a phantom base, item
     `ECLIPSE_SHARD` on an echo shard that reveals monsters). Daytime only, in the overworld.
   - Blizzard (`event/impl/BlizzardEvent.java`, boss `mob/FrostYeti.java` on a ravager base, item `FROSTFANG` on a
     diamond sword that chills). Freezing for exposed players in cold biomes.
   - Resources added (models, `assets/minecraft/items/*.json`, advancements, en/es translations in
     `assets/worldevents/lang` and `worldevents/lang`). Script used:
     `scratchpad/add_content.py` + `content_3a.json` (not in the repo; easy to redo by hand).
   - Tests in `WorldEventsGameTests` (convergence, eclipse, blizzard) and environments in
     `src/gametest/resources/data/worldevents-test/test_environment/`.
   - **Pending failure:** `blizzardYetiChillsAndFrostfangFreezes` fails with "no snow in the blizzard on tick 0":
     `setWeatherParameters` doesn't apply `isRaining()` on the same tick. Check the rain after 1-2 ticks
     (`runAfterDelay`) or check `level.getLevelData().isRaining()`. Then run all the tests.
   - Then: check the creatures in the client (`runClientGameTest`; add them to `showMob`/`showSummoned`).
     The yeti may float slightly (consider `.above(-0.15f)` on its Part). Port to 26.1.2 and 1.21.11 (Java
     code + resources + tests), then 1.18.2 and 1.16.5 (adapt: blindness instead of darkness, no freezing in
     1.16.5 so use slowness plus cold damage, swap the echo shard for another item in `ITEM_SWAPS` and
     `EventItems`).
4. Phase 3b: Village Siege, Tide of the Drowned (coasts), Piglin Warband (Nether). Each with a boss (model in
   `creature_models.py`), a reward item (icon in `item_icons.py`), advancements, translations and tests.
5. Phase 4, server-owner tools: real-time scheduled events, Discord announcements (config webhook), event voting
   (`/eventos votar`), config menu with chest inventories, difficulty scaling by players or world age.
6. Phase 5, progression: more things in the seal shop (chat titles, particle trails, armor trims) and a monthly
   ranking with prizes.
7. Phase 6, ports to **1.20.1** and **1.19.2** (Forge + Fabric), following the `versions/` layout.
8. Phase 7: Paper/Spigot plugin version.

When phase 3a is done: rebuild `dist/` with version **1.1.0** (bump `version` in `gradle.properties` and
`versions/*/gradle.properties`), update `CHANGELOG.md` and `dist/UPLOAD.md`, and check the jars on real servers.
The author prefers releasing in phases (1.1 early), not waiting for everything.
