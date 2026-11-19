# Datos para subir cada archivo: World Omens 1.1.1

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo, el mismo para todas las versiones.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldevents-fabric-26.3-1.1.1.jar` | World Omens 1.1.1 for Fabric 26.3 | `1.1.1+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-fabric-26.1.2-1.1.1.jar` | World Omens 1.1.1 for Fabric 26.1.2 | `1.1.1+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-neoforge-26.1.2-1.1.1.jar` | World Omens 1.1.1 for NeoForge 26.1.2 | `1.1.1+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldevents-forge-26.1.2-1.1.1.jar` | World Omens 1.1.1 for Forge 26.1.2 | `1.1.1+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldevents-fabric-1.21.11-1.1.1.jar` | World Omens 1.1.1 for Fabric 1.21.11 | `1.1.1+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldevents-neoforge-1.21.11-1.1.1.jar` | World Omens 1.1.1 for NeoForge 1.21.11 | `1.1.1+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldevents-forge-1.21.11-1.1.1.jar` | World Omens 1.1.1 for Forge 1.21.11 | `1.1.1+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldevents-fabric-1.18.2-1.1.1.jar` | World Omens 1.1.1 for Fabric 1.18.2 | `1.1.1+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldevents-forge-1.18.2-1.1.1.jar` | World Omens 1.1.1 for Forge 1.18.2 | `1.1.1+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldevents-fabric-1.16.5-1.1.1.jar` | World Omens 1.1.1 for Fabric 1.16.5 | `1.1.1+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldevents-forge-1.16.5-1.1.1.jar` | World Omens 1.1.1 for Forge 1.16.5 | `1.1.1+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog (todas las versiones)

Es el mismo texto para 26.3, 26.1.2, 1.21.11, 1.18.2 y 1.16.5.

Markdown, para Modrinth (en Fabric añade al final la línea de Fabric API):

```markdown
## World Omens 1.1.1

The meteor shower is much easier to notice.

- Shooting stars are now thick, glowing lines, lower and closer, twice as frequent and with a whoosh. Meteors fall with a long fiery tail and a roar, and every impact brings a crack of thunder and a ring of smoke. Up to 4 meteors per player (was 3).
- A config/worldevents.json that already exists keeps its old meteor_shower values: delete that section to get the new defaults.
```

Solo en Fabric, al final: `- Requires Fabric API.`

En CurseForge (texto plano; en Fabric añade la última línea):

```text
World Omens 1.1.1

The meteor shower is much easier to notice.

- Shooting stars are now thick, glowing lines, lower and closer, twice as frequent and with a whoosh. Meteors fall with a long fiery tail and a roar, and every impact brings a crack of thunder and a ring of smoke. Up to 4 meteors per player (was 3).
- A config/worldevents.json that already exists keeps its old meteor_shower values: delete that section to get the new defaults.
- Requires Fabric API.
```

## Resource packs (los .zip)

Los packs de 1.1.1 son idénticos a los de 1.1.0 (solo cambia el número en el nombre): **no hace falta volver a subirlos**. Los de 1.1.0 siguen valiendo para esta versión del mod.

## Comprobado

- Game tests de Fabric en 26.3, 26.1.2 y 1.21.11: pasan los 36. 1.18.2 y 1.16.5 compilan; no tienen game tests.
- Test de cliente en 26.3 con capturas: la lluvia de meteoritos (acelerada) con las estrellas fugaces gruesas, un meteoro con su rastro y el aviso de impacto.
- Prueba de humo (arranca y para los 12 eventos e invoca las 13 criaturas) en un servidor dedicado de desarrollo con la 1.1.1: Fabric de 26.1.2, 1.21.11, 1.18.2 y 1.16.5, sin errores. NeoForge y Forge (que comparten el mismo código `common`) y 26.3 no se han repetido en servidor para la 1.1.1; sí pasan las game tests de 26.3.
- No comprobado: los jars de esta carpeta en servidores reales con un cliente vanilla; el aspecto de la lluvia en 26.1.2, 1.21.11, 1.18.2 y 1.16.5 (solo se vio en 26.3).
