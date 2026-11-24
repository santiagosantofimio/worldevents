# Datos para subir cada archivo: World Omens 2.0.0

Valores para el formulario de Modrinth o CurseForge, en inglés como la página del mod. En todos:

- **Release channel / Release type:** Release
- **Environment:** server (required), client (optional). El mod va solo en el servidor; los clientes vanilla pueden entrar. En CurseForge, marca *Server* y *Client*.
- **Changelog:** el de abajo, el mismo para todas las versiones.

## Archivos del mod

| Archivo | Version name | Version number | Loader | Game version | Java | Dependencias |
|---|---|---|---|---|---|---|
| `26.3/worldomens-fabric-26.3-2.0.0.jar` | World Omens 2.0.0 for Fabric 26.3 | `2.0.0+26.3-fabric` | Fabric | 26.3 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.3/worldomens-neoforge-26.3-2.0.0.jar` | World Omens 2.0.0 for NeoForge 26.3 | `2.0.0+26.3-neoforge` | NeoForge | 26.3 | 25 | NeoForge 26.3.0.101+ |
| `26.3/worldomens-forge-26.3-2.0.0.jar` | World Omens 2.0.0 for Forge 26.3 | `2.0.0+26.3-forge` | Forge | 26.3 | 25 | Forge 66.1.2+ |
| `26.1.2/worldomens-fabric-26.1.2-2.0.0.jar` | World Omens 2.0.0 for Fabric 26.1.2 | `2.0.0+26.1.2-fabric` | Fabric | 26.1.2 | 25 | Fabric API (required), Fabric Loader 0.19.5+ |
| `26.1.2/worldomens-neoforge-26.1.2-2.0.0.jar` | World Omens 2.0.0 for NeoForge 26.1.2 | `2.0.0+26.1.2-neoforge` | NeoForge | 26.1.2 | 25 | NeoForge 26.1.2.126+ |
| `26.1.2/worldomens-forge-26.1.2-2.0.0.jar` | World Omens 2.0.0 for Forge 26.1.2 | `2.0.0+26.1.2-forge` | Forge | 26.1.2 | 25 | Forge 64.1.5+ |
| `1.21.11/worldomens-fabric-1.21.11-2.0.0.jar` | World Omens 2.0.0 for Fabric 1.21.11 | `2.0.0+1.21.11-fabric` | Fabric | 1.21.11 | 21 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.21.11/worldomens-neoforge-1.21.11-2.0.0.jar` | World Omens 2.0.0 for NeoForge 1.21.11 | `2.0.0+1.21.11-neoforge` | NeoForge | 1.21.11 | 21 | NeoForge 21.11.48+ |
| `1.21.11/worldomens-forge-1.21.11-2.0.0.jar` | World Omens 2.0.0 for Forge 1.21.11 | `2.0.0+1.21.11-forge` | Forge | 1.21.11 | 21 | Forge 61.2.4+ |
| `1.18.2/worldomens-fabric-1.18.2-2.0.0.jar` | World Omens 2.0.0 for Fabric 1.18.2 | `2.0.0+1.18.2-fabric` | Fabric | 1.18.2 | 17 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.18.2/worldomens-forge-1.18.2-2.0.0.jar` | World Omens 2.0.0 for Forge 1.18.2 | `2.0.0+1.18.2-forge` | Forge | 1.18.2 | 17 | Forge 40.3.12+ |
| `1.16.5/worldomens-fabric-1.16.5-2.0.0.jar` | World Omens 2.0.0 for Fabric 1.16.5 | `2.0.0+1.16.5-fabric` | Fabric | 1.16.5 | 8 | Fabric API (required), Fabric Loader 0.19.5+ |
| `1.16.5/worldomens-forge-1.16.5-2.0.0.jar` | World Omens 2.0.0 for Forge 1.16.5 | `2.0.0+1.16.5-forge` | Forge | 1.16.5 | 8 | Forge 36.2.42+ |

NeoForge no existe para 1.18.2 ni 1.16.5. Los jars de 1.16.5 funcionan con Java 8 (y con versiones más nuevas).

## Changelog (todas las versiones)

Markdown para Modrinth:

```markdown
## World Omens 2.0.0

The mod id, command and config now use the name World Omens. This is a breaking change, with automatic migration.

- The command is `/worldomens` (aliases `/omens` and `/eventos`). `/worldevents` no longer exists.
- The config is `config/worldomens.json`. Your old `config/worldevents.json` is moved and its values are kept.
- Saved event state (running event, planned event, cooldowns) is copied from the old data automatically.
- Update the resource pack together with the mod: the old pack uses the old namespace and will not apply.
- Relics and spawn eggs you already own keep working. Advancement progress starts over, because the advancements have new ids.
- Jar files are now named `worldomens-<loader>-<minecraft>-<version>.jar`.
- NeoForge and Forge builds for Minecraft 26.3.
```

En Fabric: `- Requires Fabric API.`

Texto plano para CurseForge:

```text
World Omens 2.0.0

The mod id, command and config now use the name World Omens. This is a breaking change, with automatic migration.

- The command is /worldomens (aliases /omens and /eventos). /worldevents no longer exists.
- The config is config/worldomens.json. Your old config/worldevents.json is moved and its values are kept.
- Saved event state (running event, planned event, cooldowns) is copied from the old data automatically.
- Update the resource pack together with the mod: the old pack uses the old namespace and will not apply.
- Relics and spawn eggs you already own keep working. Advancement progress starts over, because the advancements have new ids.
- Jar files are now named worldomens-<loader>-<minecraft>-<version>.jar.
- NeoForge and Forge builds for Minecraft 26.3.
```

En Fabric: `- Requires Fabric API.`

## Resource packs

Los cinco `.zip` de 2.0.0 (`worldomens-2.0.0+<minecraft>-resourcepack.zip`) hay que subirlos de nuevo: cambió el espacio de nombres, así que los de 1.2.0 no sirven con este mod.

## Comprobado

- Pruebas de servidor tras el renombrado: 38 de 38 en 26.3 (la primera pasada tuvo un fallo en la prueba frágil de la luna de sangre, que pasó al repetir), 37 de 37 en 26.1.2 y 1.21.11. Builds de 26.3, 1.18.2 y 1.16.5 aprobados.
- Prueba de humo (arranca el servidor, invoca cada criatura, inicia y detiene cada evento, incluidos Firefly Night y los comandos de ayuda) aprobada en los 13 loaders: Fabric, NeoForge y Forge de 26.3, 26.1.2 y 1.21.11, y Fabric y Forge de 1.18.2 y 1.16.5. En Forge 26.3 el script marca un aviso de red del servidor (`LanServerPinger`) ajeno al mod.
- Los 13 jars y los 5 resource packs de 2.0.0 se regeneraron: ningún archivo conserva el nombre viejo, todos incluyen Firefly Night y el id `worldomens`, y los cinco SHA-1 coinciden.
- Migración probada en 26.1.2 con un config y un estado guardado con los nombres viejos: el config pasó a `worldomens.json` conservando un valor personalizado y el estado copió un dato marcado.
- Sin verificar: los jars de `dist/` en servidores reales con cliente vanilla, un jugador conectado en cualquier loader, aspecto en cliente tras el renombrado, y la migración en 1.21.11, 1.18.2 y 1.16.5 (el código es el mismo, pero solo se ejecutó en 26.1.2).
