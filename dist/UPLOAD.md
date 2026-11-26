# Datos para subir cada archivo: World Omens 2.1.0

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo, el mismo para todas las versiones.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldomens-fabric-26.3-2.1.0.jar` | World Omens 2.1.0 for Fabric 26.3 | `2.1.0+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.3/worldomens-neoforge-26.3-2.1.0.jar` | World Omens 2.1.0 for NeoForge 26.3 | `2.1.0+26.3-neoforge` | NeoForge | 26.3 | 25 | NeoForge 26.3.0.101+ |
| `26.3/worldomens-forge-26.3-2.1.0.jar` | World Omens 2.1.0 for Forge 26.3 | `2.1.0+26.3-forge` | Forge | 26.3 | 25 | Forge 66.1.2+ |
| `26.1.2/worldomens-fabric-26.1.2-2.1.0.jar` | World Omens 2.1.0 for Fabric 26.1.2 | `2.1.0+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldomens-neoforge-26.1.2-2.1.0.jar` | World Omens 2.1.0 for NeoForge 26.1.2 | `2.1.0+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldomens-forge-26.1.2-2.1.0.jar` | World Omens 2.1.0 for Forge 26.1.2 | `2.1.0+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldomens-fabric-1.21.11-2.1.0.jar` | World Omens 2.1.0 for Fabric 1.21.11 | `2.1.0+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldomens-neoforge-1.21.11-2.1.0.jar` | World Omens 2.1.0 for NeoForge 1.21.11 | `2.1.0+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldomens-forge-1.21.11-2.1.0.jar` | World Omens 2.1.0 for Forge 1.21.11 | `2.1.0+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldomens-fabric-1.18.2-2.1.0.jar` | World Omens 2.1.0 for Fabric 1.18.2 | `2.1.0+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldomens-forge-1.18.2-2.1.0.jar` | World Omens 2.1.0 for Forge 1.18.2 | `2.1.0+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldomens-fabric-1.16.5-2.1.0.jar` | World Omens 2.1.0 for Fabric 1.16.5 | `2.1.0+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldomens-forge-1.16.5-2.1.0.jar` | World Omens 2.1.0 for Forge 1.16.5 | `2.1.0+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog (todas las versiones)

Markdown para Modrinth:

```markdown
## World Omens 2.1.0

Three new events, presets to tune how much combat you want, and personal statistics, on all five Minecraft versions.

- **Piglin Warband** (Nether): three waves of piglins, brutes and hoglins, led by the Piglin Warchief. Golden armor calms regular piglins but not the brutes or the Warchief. Drops gold and sometimes the Warchief's Cleaver.
- **Tide of the Drowned** (overworld coast, at night): three waves of drowned come out of the water, led by The Tidecaller. Drops prismarine and sometimes the Tidecaller's Conch. Ends at dawn.
- **Fishing Festival** (overworld, daytime, near water): a calm event with Luck, experience and points for every catch.
- **Presets**: `calm`, `normal` or `epic` in `config/worldomens.json` or with `/worldomens preset`. Your own values are kept.
- `/worldomens stats` shows your events, points, seals and bosses defeated.
- The `/omens` alias is now registered (the 2.0.0 notes mentioned it but it was missing).
- Fixed: in the 1.18.2 and 1.16.5 resource packs the models of relics, creatures and spawn eggs did not apply.
- New advancements and Spanish and English texts for everything. Update the resource pack together with the mod.
```

En Fabric: `- Requires Fabric API.`

Texto plano para CurseForge:

```text
World Omens 2.1.0

Three new events, presets to tune how much combat you want, and personal statistics, on all five Minecraft versions.

- Piglin Warband (Nether): three waves of piglins, brutes and hoglins, led by the Piglin Warchief. Golden armor calms regular piglins but not the brutes or the Warchief. Drops gold and sometimes the Warchief's Cleaver.
- Tide of the Drowned (overworld coast, at night): three waves of drowned come out of the water, led by The Tidecaller. Drops prismarine and sometimes the Tidecaller's Conch. Ends at dawn.
- Fishing Festival (overworld, daytime, near water): a calm event with Luck, experience and points for every catch.
- Presets: calm, normal or epic in config/worldomens.json or with /worldomens preset. Your own values are kept.
- /worldomens stats shows your events, points, seals and bosses defeated.
- The /omens alias is now registered (the 2.0.0 notes mentioned it but it was missing).
- Fixed: in the 1.18.2 and 1.16.5 resource packs the models of relics, creatures and spawn eggs did not apply.
- New advancements and Spanish and English texts for everything. Update the resource pack together with the mod.
```

En Fabric: `- Requires Fabric API.`

## Resource packs

Los cinco `.zip` de 2.1.0 (`worldomens-2.1.0+<minecraft>-resourcepack.zip`) hay que subirlos de nuevo: traen los modelos, textos y logros nuevos. Los de 1.18.2 y 1.16.5 de 2.0.0 no aplicaban los modelos de reliquias y criaturas (sus números de modelo salían del id viejo): estos sí.

## Comprobado

- Pruebas del juego: 43 de 43 en 26.3 Fabric (38 anteriores y 5 nuevas), 42 de 42 en 1.21.11 y aprobadas en 26.1.2. Pruebas unitarias de los presets en las cuatro versiones modernas. 1.18.2 y 1.16.5 compilan y pasan la prueba de humo, pero no tienen pruebas del juego.
- Prueba de humo (arranca el servidor, invoca cada criatura, inicia y detiene cada evento incluidos los tres nuevos, y ejecuta `preset`, `stats` y el alias `omens`) aprobada en los 13 loaders. En Forge 26.3 el script marca un aviso de red del servidor (`LanServerPinger`) ajeno al mod: llegó a "done" igual.
- Los 13 jars y los 5 resource packs de 2.1.0 se regeneraron: ninguno conserva el nombre viejo, todos incluyen los tres eventos nuevos y los cinco SHA-1 coinciden.
- Modelos e iconos de las dos criaturas y las dos reliquias nuevas: provisionales, hechos por código, hasta que haya arte definitivo (prompts en `PLAN-2.1.0-prompts.md`, local).
- Sin verificar: servidores reales con cliente vanilla, un jugador conectado en cualquier loader, el aspecto en cliente de lo nuevo, el comportamiento real del Piglin Warband (Nether) y de la marea en costas reales, y la detección de capturas del festival con una caña de verdad (las pruebas simulan el anzuelo).
