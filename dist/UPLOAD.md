# Datos para subir cada archivo

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el texto de abajo, según la versión.

## 26.1.2

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.1.2/worldevents-fabric-26.1.2-1.0.0.jar` | World Omens 1.0.0 for Fabric 26.1.2 | `1.0.0+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldevents-neoforge-26.1.2-1.0.0.jar` | World Omens 1.0.0 for NeoForge 26.1.2 | `1.0.0+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldevents-forge-26.1.2-1.0.0.jar` | World Omens 1.0.0 for Forge 26.1.2 | `1.0.0+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |

Changelog:

> First release for Minecraft 26.1.2. Same content as World Omens 1.0.0: ten random events, eleven creatures with four bosses, event relics, Event Seals and the advancement tab. Server-side: players can join with vanilla Minecraft.

## 1.21.11

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `1.21.11/worldevents-fabric-1.21.11-1.0.0.jar` | World Omens 1.0.0 for Fabric 1.21.11 | `1.0.0+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldevents-neoforge-1.21.11-1.0.0.jar` | World Omens 1.0.0 for NeoForge 1.21.11 | `1.0.0+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldevents-forge-1.21.11-1.0.0.jar` | World Omens 1.0.0 for Forge 1.21.11 | `1.0.0+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |

Changelog:

> First release for Minecraft 1.21.11. Same content as World Omens 1.0.0: ten random events, eleven creatures with four bosses, event relics, Event Seals and the advancement tab. Server-side: players can join with vanilla Minecraft.

## Resource packs

Súbelos como archivo adicional de cada versión del mod (en Modrinth, *Additional files*), o como proyecto aparte de tipo *Resource Pack*:

| Archivo | Nombre | Game version |
|---|---|---|
| `26.1.2/worldevents-1.0.0+26.1.2-resourcepack.zip` | World Omens Resource Pack 1.0.0 (26.1.2) | 26.1.2 |
| `1.21.11/worldevents-1.0.0+1.21.11-resourcepack.zip` | World Omens Resource Pack 1.0.0 (1.21.11) | 1.21.11 |

Descripción del resource pack:

> Optional resource pack for World Omens: gives the mod's items and creatures their own textures and 3D models, and their names in each player's language. Servers can send it to players through `resource-pack` in `server.properties` (its SHA-1 is in the `.sha1` file next to it).

## Comprobado

Los seis jars se han probado en servidores dedicados reales: criaturas, los diez eventos, el botín y la entrada de un cliente vanilla (que ve las criaturas, recibe ítems y no es expulsado). En Fabric, además, pasan los 31 game tests. No se ha probado el lado del cliente con el mod instalado (la pestaña de creativo).
