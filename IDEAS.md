# Ideas que necesitan cliente

Ideas descartadas porque no funcionan con un cliente vanilla: el mod se instala solo en el servidor.
Se guardan por si algún día hay un mod de cliente.

- **Luna de sangre con cielo y luna rojos**: cambiar el color del cielo, la luna o la niebla
  requiere shaders, texturas o un mod de cliente. Ahora se usa el flag vanilla de la boss bar
  que oscurece el cielo (`darkenSky`) y una niebla de partículas rojas, que sí ve un cliente vanilla.
- **Criaturas con extremidades animadas** (andar, mover brazos). Hoy cada criatura es un modelo de una pieza
  que flota, salta o vuela con el mob invisible que lleva debajo. Animar patas y brazos exigiría varias
  entidades de visualización movidas desde el servidor en cada tick (como ModelEngine en Bukkit): mucho
  tráfico de red y frágil con lag.
