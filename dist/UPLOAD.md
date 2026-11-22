# Datos para subir cada archivo: World Omens 1.2.0

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo, el mismo para todas las versiones.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldevents-fabric-26.3-1.2.0.jar` | World Omens 1.2.0 for Fabric 26.3 | `1.2.0+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-fabric-26.1.2-1.2.0.jar` | World Omens 1.2.0 for Fabric 26.1.2 | `1.2.0+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-neoforge-26.1.2-1.2.0.jar` | World Omens 1.2.0 for NeoForge 26.1.2 | `1.2.0+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldevents-forge-26.1.2-1.2.0.jar` | World Omens 1.2.0 for Forge 26.1.2 | `1.2.0+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldevents-fabric-1.21.11-1.2.0.jar` | World Omens 1.2.0 for Fabric 1.21.11 | `1.2.0+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldevents-neoforge-1.21.11-1.2.0.jar` | World Omens 1.2.0 for NeoForge 1.21.11 | `1.2.0+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldevents-forge-1.21.11-1.2.0.jar` | World Omens 1.2.0 for Forge 1.21.11 | `1.2.0+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldevents-fabric-1.18.2-1.2.0.jar` | World Omens 1.2.0 for Fabric 1.18.2 | `1.2.0+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldevents-forge-1.18.2-1.2.0.jar` | World Omens 1.2.0 for Forge 1.18.2 | `1.2.0+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldevents-fabric-1.16.5-1.2.0.jar` | World Omens 1.2.0 for Fabric 1.16.5 | `1.2.0+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldevents-forge-1.16.5-1.2.0.jar` | World Omens 1.2.0 for Forge 1.16.5 | `1.2.0+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog (todas las versiones)

Markdown para Modrinth:

```markdown
## World Omens 1.2.0

- Firefly Night: follow five personal lights in the overworld at night. Each light grants experience and event points; completing the route grants an advancement. Progress survives restarts.
- `/worldevents help [id]` and `/eventos help [id]` explain an event’s objective, reward and conditions in English or Spanish. Without an id, they describe the active event in the current dimension. Players can use help without operator permissions.
- Caravan contracts: near an active merchant, deliver wheat, iron ingots or rotten flesh for an event seal. Up to two deliveries per player per caravan; progress survives restarts. Use `/worldevents contract` or `/eventos encargo` to see recipes.
- `/worldevents ranking` and `/eventos ranking` show the current event's top three scores and your own score and rank.
- Meteor Shower shooting stars now cross closer to the player's forward view, making them easier to see.
- The Colossus Hammer has a squarer 2D icon with the same dark rock and lava colors.
- The Nomad Merchant's hood now frames a visible face and nose, making its silhouette easier to read.
- Firefly Night and help are available on all five supported Minecraft versions. Existing configuration files receive the new event settings.
```

En Fabric: `- Requires Fabric API.`

Texto plano para CurseForge:

```text
World Omens 1.2.0

- Firefly Night: follow five personal lights in the overworld at night. Each light grants experience and event points; completing the route grants an advancement. Progress survives restarts.
- /worldevents help [id] and /eventos help [id] explain an event's objective, reward and conditions in English or Spanish. Without an id, they describe the active event in the current dimension. Players can use help without operator permissions.
- Caravan contracts: near an active merchant, deliver wheat, iron ingots or rotten flesh for an event seal. Up to two deliveries per player per caravan; progress survives restarts. Use /worldevents contract or /eventos encargo to see recipes.
- /worldevents ranking and /eventos ranking show the current event's top three scores and your own score and rank.
- Meteor Shower shooting stars now cross closer to the player's forward view, making them easier to see.
- The Colossus Hammer has a squarer 2D icon with the same dark rock and lava colors.
- The Nomad Merchant's hood now frames a visible face and nose, making its silhouette easier to read.
- Firefly Night and help are available on all five supported Minecraft versions. Existing configuration files receive the new event settings.
```

En Fabric: `- Requires Fabric API.`

## Resource packs

Los `.zip` de 1.2.0 se generan para cada versión. El evento nuevo usa partículas vanilla y no necesita modelos ni texturas nuevas.

## Comprobado

- GameTests de Fabric: 38 en 26.3 y 37 en 26.1.2 y 1.21.11. Builds de 1.18.2 y 1.16.5 aprobados.
- Las pruebas en 26.3 verificaron consumo de materiales, dos sellos, límite por jugador y ranking vacío/con puntos. Los cinco builds y `dist` se repitieron tras estos cambios; los once jars incluyen los encargos y textos del ranking, y los cinco SHA-1 coinciden.
- Builds Forge de 1.18.2 y 1.16.5 aprobados.
- Pruebas de humo previas a los encargos en once loaders: iniciaron y detuvieron eventos sin errores del mod.
- Cliente 26.3: `runClientGameTest` aprobó y la captura `worldevents_firefly_night` mostró boss bar, pistas de dirección/distancia y partículas verdes visibles a 13 bloques.
- Cliente 26.3 tras el ajuste de meteoritos: `runClientGameTest` aprobó; cuatro capturas `worldevents_meteor_shower` muestran varias estelas cruzando la vista del jugador.
- Cliente 26.3 con el icono cuadrado del mazo: `runClientGameTest` aprobó y `worldevents_creative_tab` mostró el icono en el inventario. El primer arranque quedó detenido al cargar recursos; se cerró y el segundo terminó en verde.
- Cliente 26.3 con el mercader actualizado: `runClientGameTest` aprobó; `worldevents_creature_nomad_merchant` mostró ojos, barba y nariz desde el frente, sin llamas que tapen la vista.
- Sin verificar: los jars de `dist/` en servidores reales con cliente vanilla, aspecto en las otras versiones y balance prolongado.
