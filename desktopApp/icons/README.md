# Iconos de la aplicación Desktop

`citovision-1024.png` es la **fuente de verdad**: 1024×1024 RGBA, con el arte ocupando el 80,5 % del
lienzo dentro de un squircle centrado, según la rejilla de iconos de macOS de Apple. Todo lo demás se
deriva de él, así que cualquier rediseño empieza por sustituir este fichero.

Derivados en uso:

- `citovision.icns` — icono del `.app` y del `.dmg`, vía
  `compose.desktop.nativeDistributions.macOS.iconFile`. **También aplica al ejecutar con `run`**: el plugin
  lo pasa como `-Xdock:icon` al arrancar la JVM, así que en macOS el Dock ya sale de aquí.
- `citovision.ico` — icono del ejecutable en **Windows**. Contiene la misma imagen a 16, 24, 32, 48, 64, 128
  y 256 px, porque Windows elige el tamaño según dónde la pinte. Se genera con
  `python3 tools/make_windows_icon.py`.
- `citovision.png` (512×512) — icono en **Linux**, que es el único formato que acepta ahí `jpackage`.
- `../src/desktopMain/resources/icons/citovision.png` (512×512) — lo carga `Main.kt` para el parámetro
  `icon` de la ventana, que es lo que gobierna la barra de tareas en Windows y Linux. En macOS la llamada a
  `java.awt.Taskbar` que hace `Main.kt` es probablemente redundante, porque el `-Xdock:icon` del plugin ya
  ha fijado el icono del Dock antes.

Son dos ficheros distintos y hay que regenerar los dos: si solo cambias uno, verás el arte nuevo en unas
plataformas y el viejo en otras.

## El nombre va aparte del icono

El texto que enseña el Dock al pasar el puntero **no** viene de estos ficheros, y se fija por tres vías
independientes que no se cubren entre sí (si solo pones una, en el resto de sitios sale «java»):

| Dónde se ve | Qué lo fija |
| --- | --- |
| Dock con `./gradlew run` | `application.jvmArgs += "-Xdock:name=..."` |
| Dock del `.app` / `.dmg` | `macOS.dockName` (y `packageName` nombra el bundle) |
| Barra de menús de macOS | `apple.awt.application.name`, en `Main.kt` antes de que arranque AWT |

Configurar `-Xdock:name` sobre la task `run` desde fuera **no funciona**: el plugin hace `setJvmArgs(...)` y
reemplaza la lista entera, así que solo sobrevive lo que se declare en el bloque `application`.

## Regenerar tras cambiar el arte

```bash
cd desktopApp/icons
ISET=$(mktemp -d)/citovision.iconset && mkdir -p "$ISET"
for size in 16 32 128 256 512; do
  sips -z "$size" "$size" citovision-1024.png --out "$ISET/icon_${size}x${size}.png"
  sips -z $((size * 2)) $((size * 2)) citovision-1024.png --out "$ISET/icon_${size}x${size}@2x.png"
done
iconutil -c icns "$ISET" -o citovision.icns
sips -z 512 512 citovision-1024.png --out ../src/desktopMain/resources/icons/citovision.png
```

`iconutil` es estricto con los nombres del `.iconset`: si alguno no encaja con el patrón
`icon_<w>x<h>[@2x].png` falla con un escueto «Failed to generate ICNS» sin decir cuál.

Y el `.ico` de Windows:

```bash
python3 tools/make_windows_icon.py
```

## Aviso sobre el arte en Windows y Linux

El maestro está dibujado con la convención de **macOS**: el arte ocupa el 80,5 % del lienzo y el resto son
márgenes transparentes, porque el sistema aplica su propia máscara de squircle. Windows y Linux **no
recortan ni enmascaran**: pintan el PNG tal cual. Reutilizar el mismo arte funciona, pero ahí el icono se ve
algo más pequeño que los de las apps nativas, con aire alrededor.

Si molesta, la solución no es recortar el maestro por script —el recorte descuadra la sombra— sino dibujar
una variante que llene el lienzo y usarla solo para `.ico` y el PNG de Linux.
