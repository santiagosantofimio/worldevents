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

- Gradle 1.18.2 and 1.16.5 need a JDK 17 that Gradle may not detect on its own: pass
  `-Porg.gradle.java.installations.paths=<jdk17 home>` (and the same as env `ORG_GRADLE_PROJECT_org.gradle.java.installations.paths`
  for the nested Forge builds). The first build of a version downloads Minecraft: on a slow network the server jar
  can time out, so download it to `~/.gradle/caches/fabric-loom/<mc>/minecraft-server.jar` by hand.
- Gradle runs on **Java 25** (`JAVA_HOME=<jdk25>`); JDKs 8, 17 and 21 must be installed too.
- Server tests: `./gradlew runGameTest` (root) and `./gradlew -p versions/<v> :fabric:runGameTest`.
  Two old tests are flaky under load or timing: horde (chunk loading) and bloody tear (time of day on 26.1.2).
  They pass on a rerun.
- Client test with screenshots: `./gradlew runClientGameTest`. Headless in the cloud it was run with
  `xvfb-run` plus Mesa, and on 26.3 with `SDL_VIDEO_FORCE_EGL=1`. Screenshots land in
  `build/run/clientGameTest/screenshots`.
- Commits end with the session's `Co-Authored-By` attribution. Don't open a PR unless asked.

## Released status

- **1.0.1**: Captain's Cutlass lunge fix, Magma Colossus fixes, 2D weapons in hand, name changed to World Omens.
- **1.1.0** (in `dist/`, built, not uploaded yet): new creature models and 16x16 icons, Convergence, Eclipse and
  Blizzard in all five versions. Verified: 36 game tests on 26.3, 26.1.2 and 1.21.11, and the smoke test
  (`versions/smoketest/run.sh`) on every loader of 26.1.2, 1.21.11, 1.18.2 and 1.16.5. Smoke test also on 26.3. Seen in a client (26.3 only): the new creatures and the
  creative tab. Not verified: the jars on real servers with a vanilla client, a client on the other versions, the
  Eclipse Shard/Frostfang/cold on 1.18.2 and 1.16.5. Details in `dist/UPLOAD.md`.
- The Eclipse Wraith is a vex underneath, not a phantom: a phantom's glowing eyes show through the invisible mob.

## Roadmap for 1.1 (the author asked for "do them all")

1. [done, commit 3450ba9] 11 creatures redone in vanilla style, in every version.
2. [done, commit 8998524] 16x16 icons in every version.
3. [done, 1.1.0] Phase 3a: Convergence, Eclipse and Blizzard in every version (client shots of the Wraith and the
   Yeti checked on 26.3: fine). 1.18.2 uses an amethyst shard and 1.16.5 a prismarine shard for the Eclipse
   Shard, and 1.16.5 has no freezing (slowness and a little damage).
4. Phase 3b: Village Siege, Tide of the Drowned (coasts), Piglin Warband (Nether). Each with a boss (model in
   `creature_models.py`), a reward item (icon in `item_icons.py`), advancements, translations and tests.
5. Phase 4, server-owner tools: real-time scheduled events, Discord announcements (config webhook), event voting
   (`/eventos votar`), config menu with chest inventories, difficulty scaling by players or world age.
6. Phase 5, progression: more things in the seal shop (chat titles, particle trails, armor trims) and a monthly
   ranking with prizes.
7. Phase 6, ports to **1.20.1** and **1.19.2** (Forge + Fabric), following the `versions/` layout.
8. Phase 7: Paper/Spigot plugin version.

Next release steps (done for 1.1.0, repeat for later ones): rebuild `dist/` with the new version (bump `version` in `gradle.properties` and
`versions/*/gradle.properties`), update `CHANGELOG.md` and `dist/UPLOAD.md`, and check the jars on real servers.
The author prefers releasing in phases (1.1 early), not waiting for everything.
