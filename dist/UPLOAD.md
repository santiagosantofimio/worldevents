# Datos para subir cada archivo: World Omens 1.0.1

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo: uno para 26.3, 26.1.2 y 1.21.11, y otro para 1.18.2 y 1.16.5.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldevents-fabric-26.3-1.0.1.jar` | World Omens 1.0.1 for Fabric 26.3 | `1.0.1+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-fabric-26.1.2-1.0.1.jar` | World Omens 1.0.1 for Fabric 26.1.2 | `1.0.1+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-neoforge-26.1.2-1.0.1.jar` | World Omens 1.0.1 for NeoForge 26.1.2 | `1.0.1+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldevents-forge-26.1.2-1.0.1.jar` | World Omens 1.0.1 for Forge 26.1.2 | `1.0.1+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldevents-fabric-1.21.11-1.0.1.jar` | World Omens 1.0.1 for Fabric 1.21.11 | `1.0.1+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldevents-neoforge-1.21.11-1.0.1.jar` | World Omens 1.0.1 for NeoForge 1.21.11 | `1.0.1+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldevents-forge-1.21.11-1.0.1.jar` | World Omens 1.0.1 for Forge 1.21.11 | `1.0.1+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldevents-fabric-1.18.2-1.0.1.jar` | World Omens 1.0.1 for Fabric 1.18.2 | `1.0.1+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldevents-forge-1.18.2-1.0.1.jar` | World Omens 1.0.1 for Forge 1.18.2 | `1.0.1+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldevents-fabric-1.16.5-1.0.1.jar` | World Omens 1.0.1 for Fabric 1.16.5 | `1.0.1+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldevents-forge-1.16.5-1.0.1.jar` | World Omens 1.0.1 for Forge 1.16.5 | `1.0.1+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog: 26.3, 26.1.2 y 1.21.11

Markdown, para Modrinth (en Fabric añade al final la línea de Fabric API):

```markdown
## World Omens 1.0.1

Fixes from players' feedback.

- The Captain's Cutlass lunge now moves you. The Magma Colossus's slam and the eruption's geysers now throw players too.
- The Magma Colossus attacks: it always goes after the nearest player, even one it can't see, and no longer calms down near nether portals, respawn anchors or warped fungus.
- The Magma Colossus no longer burns or takes damage from fire and lava.
- The Magma Colossus no longer spawns stuck in the ground on uneven terrain, and gets itself out if it ever ends up inside blocks.
- Relic weapons show their own sprite in hand, like vanilla tools, instead of 3D models that were too big in first person. Update the resource pack to see it.
```

Solo en Fabric, al final: `- Requires Fabric API.`

En CurseForge (texto plano; en Fabric añade la última línea):

```text
World Omens 1.0.1

Fixes from players' feedback.

- The Captain's Cutlass lunge now moves you. The Magma Colossus's slam and the eruption's geysers now throw players too.
- The Magma Colossus attacks: it always goes after the nearest player, even one it can't see, and no longer calms down near nether portals, respawn anchors or warped fungus.
- The Magma Colossus no longer burns or takes damage from fire and lava.
- The Magma Colossus no longer spawns stuck in the ground on uneven terrain, and gets itself out if it ever ends up inside blocks.
- Relic weapons show their own sprite in hand, like vanilla tools, instead of 3D models that were too big in first person. Update the resource pack to see it.
- Requires Fabric API.
```

## Changelog: 1.18.2 y 1.16.5

En estas versiones el lunge del sable ya funcionaba y las armas ya se veían con su sprite en la mano.

Markdown, para Modrinth (en Fabric añade al final la línea de Fabric API):

```markdown
## World Omens 1.0.1

Fixes from players' feedback.

- The Magma Colossus attacks: it always goes after the nearest player, even one it can't see, and no longer calms down near nether portals, respawn anchors or warped fungus.
- The Magma Colossus's ground slam now works.
- The Magma Colossus no longer burns or takes damage from fire and lava.
- The Magma Colossus no longer spawns stuck in a hill when summoned there, and gets itself out if it ever ends up inside blocks.
```

Solo en Fabric, al final: `- Requires Fabric API.`

En CurseForge (texto plano; en Fabric añade la última línea):

```text
World Omens 1.0.1

Fixes from players' feedback.

- The Magma Colossus attacks: it always goes after the nearest player, even one it can't see, and no longer calms down near nether portals, respawn anchors or warped fungus.
- The Magma Colossus's ground slam now works.
- The Magma Colossus no longer burns or takes damage from fire and lava.
- The Magma Colossus no longer spawns stuck in a hill when summoned there, and gets itself out if it ever ends up inside blocks.
- Requires Fabric API.
```

## Resource packs (los .zip)

Los de 26.3, 26.1.2 y 1.21.11 cambian (las armas en la mano). Los de 1.18.2 y 1.16.5 se ven igual que los de 1.0.0: subirlos es opcional.

- **Modrinth:** como *Additional file* en cada versión del mod de la misma versión de Minecraft.
- **Proyecto aparte** de tipo *Resource Pack* (en CurseForge es la única opción):

| Archivo | Version name | Version number | Game version |
|---|---|---|---|
| `26.3/worldevents-1.0.1+26.3-resourcepack.zip` | World Omens Resource Pack 1.0.1 for 26.3 | `1.0.1+26.3` | 26.3 |
| `26.1.2/worldevents-1.0.1+26.1.2-resourcepack.zip` | World Omens Resource Pack 1.0.1 for 26.1.2 | `1.0.1+26.1.2` | 26.1.2 |
| `1.21.11/worldevents-1.0.1+1.21.11-resourcepack.zip` | World Omens Resource Pack 1.0.1 for 1.21.11 | `1.0.1+1.21.11` | 1.21.11 |
| `1.18.2/worldevents-1.0.1+1.18.2-resourcepack.zip` | World Omens Resource Pack 1.0.1 for 1.18.2 | `1.0.1+1.18.2` | 1.18.2 |
| `1.16.5/worldevents-1.0.1+1.16.5-resourcepack.zip` | World Omens Resource Pack 1.0.1 for 1.16.5 | `1.0.1+1.16.5` | 1.16.5 |

Changelog (cambia 26.3 por la versión de cada zip):

```markdown
## World Omens Resource Pack 1.0.1 for Minecraft 26.3

Relic weapons show their own sprite in hand, like vanilla tools, instead of 3D models.
```

Para 1.18.2 y 1.16.5:

```markdown
## World Omens Resource Pack 1.0.1 for Minecraft 1.18.2

Same looks as 1.0.0, for World Omens 1.0.1.
```

En CurseForge, el mismo texto sin la línea del título con `##`.

**Servidores que mandan el pack a los jugadores:** al cambiar el zip, hay que poner el nuevo SHA-1 (el contenido del `.sha1`) en `resource-pack-sha1` de `server.properties`. Los `.sha1` no se suben.

## Comprobado

- Game tests de Fabric en 26.3, 26.1.2 y 1.21.11: pasan los 33 (dos nuevos: el coloso persigue a un jugador tras un muro junto a hongos distorsionados sin soltarlo ni quemarse, y encuentra sitio o sale de los bloques).
- Los jars de esta carpeta, en servidores dedicados reales con un cliente vanilla: Fabric, NeoForge y Forge de 26.1.2 y 1.21.11, y Fabric y Forge de 1.18.2 y 1.16.5 (1.16.5 con Java 8). En todos, el lunge del sable empuja al cliente y no se repite durante la espera, y el coloso invocado dentro de una colina sale, ataca y mata al cliente, y metido en lava no pierde vida.
- Cliente real con el mod (26.3, 26.1.2 y 1.21.11, con renderizado por software): usar el sable mueve al jugador (con 1.0.0 no se movía), y capturas de las armas en la mano y de las criaturas. 26.3 no se ha probado en un servidor dedicado aparte: no hay cliente de pruebas para 26.3.
