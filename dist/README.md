# Archivos para publicar

Una carpeta por versión de Minecraft, con el jar de cada loader y el resource pack opcional. Se generan con:

```bash
./gradlew -p versions/<versión> dist
```

| Versión | Fabric | NeoForge | Forge | Java |
|---|---|---|---|---|
| 26.1.2 | Loader 0.19.5+ y Fabric API | 26.1.2.126+ | 64.1.5+ | 25 |
| 1.21.11 | Loader 0.19.5+ y Fabric API | 21.11.48+ | 61.2.4+ | 21 |
| 1.18.2 | Loader 0.19.5+ y Fabric API | (no existe) | 40.3.12+ | 17 |

- `worldevents-<loader>-<versión>-<versión del mod>.jar`: el mod. En Fabric hay que marcar **Fabric API** como dependencia; NeoForge y Forge no necesitan nada más.
- `worldevents-<versión del mod>+<versión>-resourcepack.zip`: el resource pack opcional (modelos y texturas), con su SHA-1 en el `.sha1` para `server.properties`.
- `description.md`: la descripción para la página del mod.
- En 1.18.2 el mod es el mismo, con algunas diferencias porque esa versión no tiene todo lo que usa el mod: están en `UPLOAD.md`.
- `UPLOAD.md`: nombre, número de versión, changelog y dependencias de cada archivo, para el formulario de subida.
