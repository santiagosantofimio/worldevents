# Datos para subir cada archivo: World Omens 1.1.0

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo: uno para 26.3, 26.1.2 y 1.21.11, y otro para 1.18.2 y 1.16.5.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldevents-fabric-26.3-1.1.0.jar` | World Omens 1.1.0 for Fabric 26.3 | `1.1.0+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-fabric-26.1.2-1.1.0.jar` | World Omens 1.1.0 for Fabric 26.1.2 | `1.1.0+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-neoforge-26.1.2-1.1.0.jar` | World Omens 1.1.0 for NeoForge 26.1.2 | `1.1.0+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldevents-forge-26.1.2-1.1.0.jar` | World Omens 1.1.0 for Forge 26.1.2 | `1.1.0+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldevents-fabric-1.21.11-1.1.0.jar` | World Omens 1.1.0 for Fabric 1.21.11 | `1.1.0+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldevents-neoforge-1.21.11-1.1.0.jar` | World Omens 1.1.0 for NeoForge 1.21.11 | `1.1.0+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldevents-forge-1.21.11-1.1.0.jar` | World Omens 1.1.0 for Forge 1.21.11 | `1.1.0+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldevents-fabric-1.18.2-1.1.0.jar` | World Omens 1.1.0 for Fabric 1.18.2 | `1.1.0+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldevents-forge-1.18.2-1.1.0.jar` | World Omens 1.1.0 for Forge 1.18.2 | `1.1.0+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldevents-fabric-1.16.5-1.1.0.jar` | World Omens 1.1.0 for Fabric 1.16.5 | `1.1.0+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldevents-forge-1.16.5-1.1.0.jar` | World Omens 1.1.0 for Forge 1.16.5 | `1.1.0+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog: 26.3, 26.1.2 y 1.21.11

Markdown, para Modrinth (en Fabric añade al final la línea de Fabric API):

```markdown
## World Omens 1.1.0

Two new events, convergences, and a new look for every creature and item.

- **Eclipse:** in the daytime the sky goes dark and monsters walk in the sun without burning. Phantoms dive at players, and the Eclipse Wraith (boss) blinds them with pulses of darkness. It can drop the Eclipse Shard: night vision, and every monster around glows.
- **Blizzard:** snow and wind in snowy biomes, with strays out in the storm. Players in the open freeze unless they shelter, stand by a fire or wear leather. The Frost Yeti (boss) chills everyone near it with its roar and can drop Frostfang, a sword whose hits chill and slow.
- **Convergence:** an event can come as a convergence (8% by default, `convergenceChance` in the config): half as long again, with double points and seals.
- Every creature has a new model in a vanilla style, and the items have new 16x16 icons. Update the resource pack to see them.
```

Solo en Fabric, al final: `- Requires Fabric API.`

En CurseForge (texto plano; en Fabric añade la última línea):

```text
World Omens 1.1.0

Two new events, convergences, and a new look for every creature and item.

- **Eclipse:** in the daytime the sky goes dark and monsters walk in the sun without burning. Phantoms dive at players, and the Eclipse Wraith (boss) blinds them with pulses of darkness. It can drop the Eclipse Shard: night vision, and every monster around glows.
- **Blizzard:** snow and wind in snowy biomes, with strays out in the storm. Players in the open freeze unless they shelter, stand by a fire or wear leather. The Frost Yeti (boss) chills everyone near it with its roar and can drop Frostfang, a sword whose hits chill and slow.
- **Convergence:** an event can come as a convergence (8% by default, `convergenceChance` in the config): half as long again, with double points and seals.
- Every creature has a new model in a vanilla style, and the items have new 16x16 icons. Update the resource pack to see them.
- Requires Fabric API.
```

## Changelog: 1.18.2 y 1.16.5

En 1.18.2 el Eclipse Shard es un fragmento de amatista y en 1.16.5 uno de prismarina (no hay fragmentos de eco). En 1.16.5 no existe la congelación: el frío de la ventisca ralentiza y hace un poco de daño.

Markdown, para Modrinth (en Fabric añade al final la línea de Fabric API):

```markdown
## World Omens 1.1.0

Two new events, convergences, and a new look for every creature and item.

- **Eclipse:** in the daytime the sky goes dark and monsters walk in the sun without burning. Phantoms dive at players, and the Eclipse Wraith (boss) blinds them with pulses of darkness. It can drop the Eclipse Shard (an amethyst (prismarine in 1.16.5) shard): night vision, and every monster around glows.
- **Blizzard:** snow and wind in snowy biomes, with strays out in the storm. Players in the open are chilled unless they shelter, stand by a fire or wear leather. The Frost Yeti (boss) chills everyone near it with its roar and can drop Frostfang, a sword whose hits chill and slow.
- **Convergence:** an event can come as a convergence (8% by default, `convergenceChance` in the config): half as long again, with double points and seals.
- Every creature has a new model in a vanilla style, and the items have new 16x16 icons. Update the resource pack to see them.
```

Solo en Fabric, al final: `- Requires Fabric API.`

En CurseForge (texto plano; en Fabric añade la última línea):

```text
World Omens 1.1.0

Two new events, convergences, and a new look for every creature and item.

- **Eclipse:** in the daytime the sky goes dark and monsters walk in the sun without burning. Phantoms dive at players, and the Eclipse Wraith (boss) blinds them with pulses of darkness. It can drop the Eclipse Shard (an amethyst (prismarine in 1.16.5) shard): night vision, and every monster around glows.
- **Blizzard:** snow and wind in snowy biomes, with strays out in the storm. Players in the open are chilled unless they shelter, stand by a fire or wear leather. The Frost Yeti (boss) chills everyone near it with its roar and can drop Frostfang, a sword whose hits chill and slow.
- **Convergence:** an event can come as a convergence (8% by default, `convergenceChance` in the config): half as long again, with double points and seals.
- Every creature has a new model in a vanilla style, and the items have new 16x16 icons. Update the resource pack to see them.
- Requires Fabric API.
```

## Resource packs (los .zip)

Los cinco cambian: traen los modelos nuevos de todas las criaturas, los iconos de 16x16 y las criaturas y los ítems nuevos. Hay que subirlos todos.

- **Modrinth:** como *Additional file* en cada versión del mod de la misma versión de Minecraft.
- **Proyecto aparte** de tipo *Resource Pack* (en CurseForge es la única opción):

| Archivo | Version name | Version number | Game version |
|---|---|---|---|
| `26.3/worldevents-1.1.0+26.3-resourcepack.zip` | World Omens Resource Pack 1.1.0 for 26.3 | `1.1.0+26.3` | 26.3 |
| `26.1.2/worldevents-1.1.0+26.1.2-resourcepack.zip` | World Omens Resource Pack 1.1.0 for 26.1.2 | `1.1.0+26.1.2` | 26.1.2 |
| `1.21.11/worldevents-1.1.0+1.21.11-resourcepack.zip` | World Omens Resource Pack 1.1.0 for 1.21.11 | `1.1.0+1.21.11` | 1.21.11 |
| `1.18.2/worldevents-1.1.0+1.18.2-resourcepack.zip` | World Omens Resource Pack 1.1.0 for 1.18.2 | `1.1.0+1.18.2` | 1.18.2 |
| `1.16.5/worldevents-1.1.0+1.16.5-resourcepack.zip` | World Omens Resource Pack 1.1.0 for 1.16.5 | `1.1.0+1.16.5` | 1.16.5 |

Changelog (cambia 26.3 por la versión de cada zip):

```markdown
## World Omens Resource Pack 1.1.0 for Minecraft 26.3

New models for every creature, new 16x16 icons, and the Eclipse Wraith, Frost Yeti, Eclipse Shard and Frostfang.
```

Para 1.18.2 y 1.16.5:

```markdown
## World Omens Resource Pack 1.1.0 for Minecraft 1.18.2

New models for every creature, new 16x16 icons, and the Eclipse Wraith, Frost Yeti, Eclipse Shard and Frostfang.
```

En CurseForge, el mismo texto sin la línea del título con `##`.

**Servidores que mandan el pack a los jugadores:** al cambiar el zip, hay que poner el nuevo SHA-1 (el contenido del `.sha1`) en `resource-pack-sha1` de `server.properties`. Los `.sha1` no se suben.

## Comprobado

- Game tests de Fabric en 26.3, 26.1.2 y 1.21.11: pasan los 36 (tres nuevos: convergencia, eclipse y ventisca). 1.18.2 y 1.16.5 no tienen game tests.
- Prueba de humo (`versions/smoketest`: invoca las 13 criaturas, mina con el pico estelar y arranca y para los 12 eventos) en un servidor dedicado de desarrollo (`runServer`, el mismo código que los jars, no los jars de esta carpeta en un servidor instalado), sin errores en el log:
  - 26.1.2: Fabric, NeoForge y Forge.
  - 1.21.11: Fabric, NeoForge y Forge.
  - 1.18.2: Fabric y Forge.
  - 1.16.5: Fabric y Forge.
- Alcance de la prueba de humo: arranca y para cada evento e invoca cada criatura; no usa los ítems ni mide el efecto de las habilidades. Eso solo lo cubren los game tests, así que en 1.18.2 y 1.16.5 (Eclipse Shard, Frostfang, frío de la ventisca) no está comprobado.
- No comprobado: 26.3 en un servidor dedicado (solo sus game tests); los jars de esta carpeta en servidores reales con un cliente vanilla; el aspecto de las criaturas y los ítems nuevos en un cliente.
