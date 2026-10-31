# World Events

*Nombre de trabajo.* · [English below](#english)

Eventos aleatorios que dan vida a tu servidor de Minecraft: lunas de sangre, fiebres del oro, lluvias de meteoritos, tormentas eléctricas, cosechas benditas, caravanas de mercaderes y hordas, con criaturas propias.

**Solo servidor.** Se instala únicamente en el servidor: los jugadores entran con Minecraft vanilla, sin instalar nada, y lo ven todo (boss bar, títulos, partículas y sonidos vanilla). También funciona en un jugador.

## Eventos

| Evento | Qué pasa |
|---|---|
| **Luna de sangre** | Una noche con más monstruos cerca de los jugadores. Los hostiles tienen ×1,5 de vida y ×1,3 de daño, y sueltan el doble de botín y de experiencia si los mata un jugador. El cielo se oscurece, aparece una niebla roja y no se puede dormir. Termina al amanecer. |
| **Fiebre del oro** | Durante 5 minutos, los minerales que pica un jugador sueltan el doble, con un destello dorado. Con toque de seda no se duplican. |
| **Lluvia de meteoritos** | Estrellas fugaces cruzan el cielo y algunos meteoritos caen cerca de los jugadores. Cada uno deja un cráter pequeño con minerales y un núcleo de magma. |
| **Tormenta eléctrica** | Lluvia y truenos, rayos cayendo alrededor de los jugadores (sin fuego ni daños), creepers que se cargan con la tormenta y espíritus de la tormenta que lanzan rayos. Al terminar, el cielo se despeja. |
| **Cosecha bendita** | Durante 5 minutos, de día, los cultivos alrededor de los jugadores crecen solos y los maduros sueltan el doble. Cada jugador recibe un espíritu de la cosecha que le sigue y le deja semillas doradas. |
| **Caravana de mercaderes** | Durante 10 minutos, de día, un mercader nómada acampa con sus llamas cerca de los jugadores y vende objetos de los eventos y otras rarezas a cambio de esmeraldas. Si los jugadores están lejos entre sí, puede llegar una segunda caravana. |
| **Horda** | De noche, tres oleadas de monstruos salen de la oscuridad a por los jugadores, cada una mayor que la anterior. El Caudillo de la horda encabeza la última. Quien ayuda a vencerla recibe experiencia; lo que quede de la horda se va al amanecer. |

Los eventos son raros a propósito: por defecto, como mucho uno cada tres días de juego en cada dimensión. Cada evento afecta solo a su dimensión, y quien entra a mitad de un evento ve la boss bar y recibe un aviso.

## Criaturas

Criaturas propias, con modelo 3D, que solo aparecen durante su evento:

| Criatura | Evento | Qué hace |
|---|---|---|
| **Señor de la luna de sangre** | Luna de sangre | Jefe: una luna roja con cara furiosa y corona, que flota tras los jugadores blandiendo la Guadaña de sangre y llama esbirros. Tiene su propia boss bar y un contorno brillante para encontrarlo de noche. Se va al amanecer. |
| **Ojo sangriento** | Luna de sangre | Un ojo gigante que atraviesa paredes y embiste a los jugadores. |
| **Saco mimético** | Fiebre del oro | Un saco de oro vivo que aparece cerca de quien mina y huye saltando. Golpéalo una vez y revienta en oro: pepitas, oro en bruto y una pepita de la suerte. Si nadie lo atrapa en un minuto, escapa. |
| **Meteorito vivo** | Lluvia de meteoritos | Una roca ardiente con ojos que a veces sale de un cráter, salta tras los jugadores y prende fuego a lo que golpea. Suelta fragmentos y, a veces, el Pico estelar. |
| **Espíritu de la tormenta** | Tormenta eléctrica | Una nube furiosa que atraviesa paredes y lanza rayos sobre los jugadores. Puede soltar un Rayo embotellado. |
| **Espíritu de la cosecha** | Cosecha bendita | Un duendecillo de paja trenzada, amistoso, que sigue a un jugador y le deja semillas doradas. No se le puede hacer daño. |
| **Mercader nómada** | Caravana de mercaderes | Un viajero encapuchado bajo una enorme mochila de mercancías, con un farol colgando. Haz clic derecho para comerciar. No se le puede hacer daño. |
| **Caudillo de la horda** | Horda | Jefe: un bruto con armadura oscura, yelmo de cuernos y capa de guerra que blande el Hacha del caudillo. Su grito de guerra da Fuerza y Velocidad a los monstruos de alrededor. Tiene su propia boss bar. |

Cómo funcionan sin mod en el cliente: un mob vanilla invisible pone el movimiento, el combate y la caja de golpes, y una entidad de visualización montada encima muestra el modelo del resource pack. Sin el pack se ve un bloque flotando con el nombre de la criatura (por ejemplo, un bloque de magma para el meteorito vivo).

### Invocar criaturas

Cada criatura tiene su **huevo generador**: el huevo vanilla de su mob base, que al usarlo sobre un bloque saca la criatura de verdad, a cualquier hora y sin su evento. También se pueden invocar con `/worldevents summon <criatura>`. Una criatura invocada no pertenece a ningún evento: se queda hasta que la derrotan, con sus habilidades, su barra de jefe y su botín. Al mercader y al espíritu de la cosecha, que no reciben daño, se les puede quitar golpeándolos en creativo.

Ids: `blood_moon_lord`, `blood_eye`, `mimic_sack`, `living_meteorite`, `storm_spirit`, `harvest_spirit`, `nomad_merchant`, `horde_warlord`. Los huevos se dan con `/worldevents give <jugadores> <criatura>_spawn_egg`.

### Pestaña de creativo

En un jugador (o si un jugador instala el mod en su cliente) aparece la pestaña **Eventos del mundo** en el inventario de creativo, con todos los ítems y los huevos. Un cliente vanilla no la ve, porque es parte de la interfaz del cliente; en un servidor, lo mismo se consigue con `/worldevents give` y `/worldevents summon`.

## Ítems

| Ítem | Cómo se consigue | Qué hace |
|---|---|---|
| **Fragmento de meteorito** | Impactos de meteorito y meteoritos vivos | Al consumirlo: prisa minera II y visión nocturna (3 min). |
| **Pepita de la suerte** | Rara al picar minerales en la fiebre del oro, y en el saco mimético | Al consumirla: suerte y prisa minera (5 min). |
| **Lágrima sangrienta** | Rara al matar monstruos en la luna de sangre, y del Señor | Úsala de noche para invocar una luna de sangre. |
| **Guadaña de sangre** | El Señor de la luna de sangre | Arma: más daño y más lenta que una espada de diamante, y te cura un 15 % del daño que haces. |
| **Pico estelar** | El meteorito vivo | Pico de diamante que funde los minerales al picarlos (hierro, oro y cobre salen en lingotes). |
| **Rayo embotellado** | El espíritu de la tormenta | Úsalo para que caiga un rayo donde miras (hasta 64 bloques). |
| **Semilla dorada** | El espíritu de la cosecha | Úsala sobre tus cultivos: todos los de alrededor (7×7) maduran de golpe, y los tallos dan su melón o calabaza. |
| **Hacha del caudillo** | El Caudillo de la horda | Arma: más daño que un hacha de diamante. Úsala para un grito de guerra: Fuerza y Velocidad para ti y los jugadores cercanos, y Debilidad y Lentitud para los monstruos de alrededor (cada 45 s). |

El mercader nómada vende algunos de estos ítems (fragmentos, pepitas, semillas, rayos embotellados, lágrimas) y otras rarezas vanilla.

Son ítems vanilla con nombre, descripción y efectos propios: funcionan en cualquier cliente. Con el resource pack opcional tienen además su propio aspecto: un icono 2D en el inventario y, las armas y herramientas, un modelo 3D en la mano.

## Logros

Una pestaña propia, «Eventos del mundo»: sobrevivir a una luna de sangre, derrotar al Señor, conseguir la guadaña, recoger un fragmento de meteorito, conseguir el Pico estelar, encontrar una pepita de la suerte, atrapar un saco mimético, derrotar a un espíritu de la tormenta, conseguir un rayo embotellado, conseguir una semilla dorada, comerciar con un mercader nómada, vencer a una horda, derrotar al Caudillo y conseguir su hacha.

## Sin destrozos

- Los cráteres solo excavan terreno natural (tierra, hierba, arena, grava, piedra…). Nunca tocan bloques de construcción, cofres ni otros bloques con contenido, líquidos ni la zona de protección del spawn.
- Con `"blockDamage": false` (o `"craters": false` en la lluvia de meteoritos), los meteoritos no cambian ningún bloque: solo sueltan los minerales.
- La horda no trae creepers, sus zombis no rompen puertas y sus monstruos solo salen de zonas oscuras, nunca dentro de una base iluminada.
- La cosecha bendita solo hace crecer cultivos: no coloca ni rompe bloques.
- Los eventos solo actúan en chunks ya cargados cerca de los jugadores y nunca fuerzan la carga de chunks.

## Instalación

1. Servidor con Fabric Loader y Fabric API para la versión de Minecraft del mod.
2. Copia el `.jar` en la carpeta `mods/` del servidor.
3. Arranca el servidor: se crea `config/worldevents.json`.

Los jugadores no necesitan instalar nada.

### Resource pack opcional

Da a los ítems y criaturas su aspecto propio (texturas, modelos 3D) y traduce sus nombres al idioma de cada jugador. Sin él todo funciona igual: los ítems se ven como su ítem vanilla de base y las criaturas como un bloque flotando con su nombre.

- **Servidor**: sube `worldevents-<versión>-resourcepack.zip` a una web con enlace directo de descarga y añade en `server.properties`:
  ```properties
  resource-pack=https://tu-web/worldevents-<versión>-resourcepack.zip
  resource-pack-sha1=<contenido del archivo .sha1>
  require-resource-pack=false
  ```
  Al entrar, a cada jugador se le pregunta si quiere descargarlo.
- **Un jugador**: no hace falta; el mod carga el aspecto él solo.

## Comandos

`/worldevents` (alias `/eventos`), para operadores (nivel de permiso 2):

| Comando | Qué hace |
|---|---|
| `/worldevents list` | Lista los eventos, su peso y si están en espera. |
| `/worldevents start <id>` | Empieza un evento ya en tu dimensión, sin esperas. |
| `/worldevents stop` | Termina el evento de tu dimensión. |
| `/worldevents status` | Estado de cada dimensión: evento activo, próximo evento o tiempo de espera. |
| `/worldevents reload` | Recarga la configuración sin reiniciar. |
| `/worldevents give <jugadores> <ítem> [cantidad]` | Da ítems del mod: `meteorite_fragment`, `lucky_nugget`, `bloody_tear`, `blood_scythe`, `starfall_pickaxe`, `bottled_lightning`, `golden_seed`, `warlord_axe`, y los huevos `<criatura>_spawn_egg`. |
| `/worldevents summon <criatura> [posición]` | Invoca una criatura donde estás (o en la posición), fuera de su evento. |

Ids: `blood_moon`, `gold_rush`, `meteor_shower`, `thunderstorm`, `blessed_harvest`, `merchant_caravan`, `horde`. Para probar la luna de sangre, la horda o los meteoritos, usa antes `/time set night`.

## Cómo se eligen los eventos

En cada dimensión, cuando termina la espera global, se sortea el próximo evento por peso entre los disponibles allí (activados y fuera de su propia espera). El evento elegido espera su momento: la luna de sangre, la horda y los meteoritos empiezan al caer la noche; la cosecha bendita y la caravana, por la mañana; la fiebre del oro y la tormenta, enseguida. Así el peso decide de verdad la frecuencia de cada evento.

## Configuración

`config/worldevents.json`. Los tiempos van en **segundos** (un día de Minecraft son 1200 s). Si faltan claves, se añaden al arrancar sin tocar las tuyas. Aplica los cambios con `/worldevents reload`.

**General**

| Clave | Por defecto | Qué hace |
|---|---|---|
| `globalCooldown` | `3600` | Espera mínima entre el final de un evento y el sorteo del siguiente, por dimensión. |
| `startChance` | `0.05` | Probabilidad, en cada revisión tras esa espera, de sortear el próximo evento. Da algo de azar al momento. |
| `checkIntervalTicks` | `200` | Cada cuántos ticks se revisa (20 ticks = 1 s). |
| `minPlayers` | `1` | Jugadores necesarios en la dimensión para que empiece un evento. |
| `blockDamage` | `true` | Interruptor general de cualquier cambio en bloques. |

**En todos los eventos:** `enabled`, `weight` (peso en el sorteo), `cooldown` (espera antes de que se repita) y `duration`.

**Luna de sangre** (`blood_moon`): `healthMultiplier` 1.5 · `damageMultiplier` 1.3 · `dropMultiplier` 2.0 · `xpMultiplier` 2.0 · `extraMobsPerPlayer` 6 · `maxExtraMobs` 40 · `spawnInterval` 5 · `spawnAttemptsPerPlayer` 4 · `allowSleeping` false · `darkenSky` true · `particles` true · `bloodyTearChance` 0.02 · `bloodyTearSummons` true · `lord` true · `lordDelay` 120 (segundos desde que empieza) · `lordHealth` 200 · `scytheChance` 0.35 · `bloodEyes` true · `bloodEyesPerPlayer` 2. La duración es un máximo: termina al amanecer.

**Fiebre del oro** (`gold_rush`): `dropMultiplier` 2.0 · `particles` true · `luckyNuggetChance` 0.03 · `mimicSackChance` 0.5 (probabilidad de saco mimético por jugador y fiebre) · `mimicSackLifetime` 60.

**Lluvia de meteoritos** (`meteor_shower`): `shootingStarInterval` 2 (segundos entre estrellas fugaces por jugador) · `impactChance` 0.04 (probabilidad de que una estrella sea un meteorito que cae) · `impactsPerPlayer` 3 · `craters` true · `craterRadius` 2.5 · `oreChance` 0.35 · `fragmentsPerImpact` 2 · `livingMeteoriteChance` 0.2 · `pickaxeChance` 0.15.

**Tormenta eléctrica** (`thunderstorm`): `strikeInterval` 3 (segundos entre rayos por jugador, de media) · `chargeCreepers` true · `spiritsPerPlayer` 1 · `maxSpirits` 6 · `bottledLightningChance` 0.5.

**Cosecha bendita** (`blessed_harvest`): `growthRadius` 12 · `growthAttemptsPerPlayer` 16 (columnas que se revisan por jugador y segundo) · `dropMultiplier` 2.0 · `spirits` true · `maxSpirits` 6 · `seedInterval` 90 · `seedsPerPlayer` 3.

**Caravana de mercaderes** (`merchant_caravan`): `maxCaravans` 2 · `llamas` 2 · `priceMultiplier` 1.0 (multiplica los precios en esmeraldas).

**Horda** (`horde`): `mobsPerPlayer` 4 (monstruos por jugador en la primera oleada) · `extraMobsPerWave` 2 · `maxMobs` 30 · `waveBreak` 10 (segundos entre oleadas) · `waveTimeout` 150 (tras este tiempo llega la siguiente oleada aunque queden monstruos) · `warlord` true · `warlordHealth` 150 · `axeChance` 0.35 · `rewardXp` 50. La duración es un máximo: termina al amanecer.

## Idiomas

Español e inglés. Cada jugador ve los textos en el idioma de su juego (si no está disponible, en inglés).

---

## English

Random world events for Minecraft servers (Fabric): blood moons, gold rushes, meteor showers, thunderstorms, blessed harvests, merchant caravans and hordes, with their own creatures.

**Server-side only.** Install it on the server; players join with vanilla Minecraft and see everything (boss bars, titles, vanilla particles and sounds). Singleplayer works too.

- **Blood Moon**: a night with extra hostile mobs around players. Hostiles get 1.5× health and 1.3× damage and drop double loot and XP when killed by a player. Darker sky, red mist, no sleeping. Ends at dawn.
- **Gold Rush**: for 5 minutes, ores mined by players drop double, with a golden sparkle. Silk touch is not doubled.
- **Meteor Shower**: shooting stars cross the sky and a few meteors fall near players, leaving a small crater lined with ores around a magma core.
- **Thunderstorm**: rain and thunder, lightning striking around players (no fire, no damage to blocks), creepers charged by the storm and storm spirits throwing lightning. The sky clears when it ends.
- **Blessed Harvest**: for 5 minutes in the daytime, crops around players grow by themselves and ripe crops drop double. Each player gets a harvest spirit that follows them and leaves golden seeds.
- **Merchant Caravan**: for 10 minutes in the daytime, a nomad merchant and their llamas camp near players, selling the events' items and other rare goods for emeralds.
- **Horde**: at night, three waves of monsters come out of the dark for the players, each bigger than the last, with the Horde Warlord leading the third. Whoever helps beat it gets XP; what is left of the horde leaves at dawn.

Craters only dig natural terrain (dirt, grass, sand, gravel, stone…), never building blocks, containers, liquids or spawn protection. Hordes bring no creepers, their zombies never break doors, and they only come out of dark places, never inside a lit base. Set `"blockDamage": false` (or `"craters": false`) and meteors only drop their ores. Events only act in loaded chunks near players and never force chunks to load. By default there is at most one event every three in-game days per dimension.

**Creatures**, with their own 3D models: the **Blood Moon Lord** (a crowned blood-red moon with an angry face, the boss of the blood moon), the **Blood Eye** (a giant eyeball that charges through walls), the **Mimic Sack** (a living sack of gold that hops away; one hit and it bursts into gold), the **Living Meteorite** (a burning rock with eyes that climbs out of craters), the **Storm Spirit** (a thundercloud that calls lightning down on players), the **Harvest Spirit** (a friendly sprite of woven straw that leaves golden seeds), the **Nomad Merchant** (a hooded traveller under a towering pack; right-click to trade) and the **Horde Warlord** (an armoured, horned brute with its own boss bar who leads the last wave of a horde). An invisible vanilla mob does the moving and fighting and an item display riding it shows the model from the resource pack; without the pack, players see a floating block with the creature's name.

**Items:** Meteorite Fragment (Haste II and Night Vision), Lucky Nugget (Luck and Haste), Bloody Tear (summons a blood moon at night), Blood Scythe (heavy weapon with lifesteal), Starfall Pickaxe (smelts ores as you mine), Bottled Lightning (calls a lightning bolt where you look), Golden Seed (ripens all the crops around at once) and Warlord's Axe (heavy axe whose war cry gives you and nearby players Strength and Speed). They are vanilla items with their own name, description and effects, so they work on any client. There is also an advancement tab for the events.

**Install:** Fabric Loader and Fabric API on the server, drop the `.jar` into `mods/`. Players install nothing.

**Optional resource pack:** gives the items and mobs their own textures and 3D models and translates their names. Host `worldevents-<version>-resourcepack.zip` anywhere with a direct download link and set `resource-pack` and `resource-pack-sha1` (from the `.sha1` file) in `server.properties`. Without it everything still works, with the look of the vanilla base items and mobs. In singleplayer the mod loads it by itself.

**Summoning:** every creature has a spawn egg (the vanilla egg of its base mob, which brings out the real creature when used on a block), and `/worldevents summon <creature> [pos]` summons one too. Summoned creatures belong to no event: they stay until defeated, with their abilities, boss bar and loot. With the mod on the client (singleplayer), a **World Events** creative tab holds every item and egg; vanilla clients don't see it, so on servers use `/worldevents give` (eggs are `<creature>_spawn_egg`) and `summon`.

**Commands** (`/worldevents`, alias `/eventos`, permission level 2): `list`, `start <id>`, `stop`, `status`, `reload`, `give <players> <item> [count]`, `summon <creature> [pos]`. Event ids: `blood_moon`, `gold_rush`, `meteor_shower`, `thunderstorm`, `blessed_harvest`, `merchant_caravan`, `horde`.

**Config:** `config/worldevents.json`, times in seconds; see the tables above for every key. Missing keys are added on startup without touching yours; `/worldevents reload` applies changes.

Texts are available in English and Spanish, picked from each player's game language.

## License

MIT
