# Iconos de la aplicación Desktop

`citovision-1024.png` es el **maestro**: 1024×1024 RGBA, con el arte ocupando el 80,5 % del
lienzo dentro de un squircle centrado, según la rejilla de iconos de macOS de Apple. Todos los
derivados de esta carpeta salen de él.

**El maestro, a su vez, se genera desde `citovision-isotipo.svg`**, que es la **fuente de verdad de toda
la marca** y el entregable original del diseñador. De él salen también el `ic_launcher_foreground.xml`
del icono adaptativo de Android y la capa del icono de iOS, así que **cualquier rediseño empieza aquí y
obliga a rehacer las tres plataformas a la vez**, o la marca queda desigual entre ellas.

Es un export de Illustrator con `viewBox` de 1300×1300 y solo formas de relleno plano (`path`,
`circle`, `ellipse`) agrupadas por clase CSS: sin degradados, sin filtros y sin trazados abiertos. Esa
simplicidad es lo que permite convertirlo a VectorDrawable de forma determinista.

Dos detalles del proceso que conviene no olvidar si hay que repetirlo:

- El rasterizador disponible en el equipo (`qlmanage`) **pinta los SVG sobre fondo blanco opaco** y no
  admite transparencia. Para el maestro, el alfa de fuera del squircle se calcula aparte a partir de su
  geometría; para la capa de iOS, que necesita transparencia real, se rasteriza dos veces —sobre blanco
  y sobre negro— y se despeja el alfa (`a = 1 − (Cw − Cb)`, `C = Cb / a`), que es exacto también en los
  bordes suavizados y no deja halo.
- El arte del logo **no está centrado** dentro de su `viewBox` ni lo llena: su caja real hay que medirla
  sobre un render antes de escalar y centrar, o el icono sale descuadrado.

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

### Paso 1: del SVG al maestro

No es una conversión directa, por dos razones. El rasterizador disponible (`qlmanage`) pinta sobre
fondo blanco opaco, y el arte del logo no está centrado dentro de su `viewBox` ni lo llena. El maestro
se compone así:

1. Medir la caja real del dibujo sobre un render, detectando los píxeles que no son blancos.
2. Escribir un SVG envolvente de 1024×1024 que pinte el squircle de fondo (824 px de lado, radio 185,
   blanco) y dentro el arte, escalado a que su dimensión mayor ocupe el 86 % del squircle y centrado
   por su caja, no por el `viewBox`.
3. Rasterizarlo con `qlmanage -t -s 1024`.
4. Recortar el alfa al squircle, calculando la cobertura de cada píxel desde la geometría del
   rectángulo redondeado (una transición de un píxel basta para que quede suavizado).

### Paso 2: del maestro a los derivados

```bash
cd desktopApp/icons
ISET=$(mktemp -d)/citovision.iconset && mkdir -p "$ISET"
for size in 16 32 128 256 512; do
  sips -z "$size" "$size" citovision-1024.png --out "$ISET/icon_${size}x${size}.png"
  sips -z $((size * 2)) $((size * 2)) citovision-1024.png --out "$ISET/icon_${size}x${size}@2x.png"
done
iconutil -c icns "$ISET" -o citovision.icns
sips -z 512 512 citovision-1024.png --out citovision.png
sips -z 512 512 citovision-1024.png --out ../src/desktopMain/resources/icons/citovision.png
```

`iconutil` es estricto con los nombres del `.iconset`: si alguno no encaja con el patrón
`icon_<w>x<h>[@2x].png` falla con un escueto «Failed to generate ICNS» sin decir cuál.

Y el `.ico` de Windows:

```bash
python3 tools/make_windows_icon.py
```

### Las otras dos plataformas

No salen del maestro, sino del SVG, y hay que rehacerlas en la misma tanda:

- **Android**, `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`: conversión a
  VectorDrawable conservando el sistema de coordenadas del SVG (`viewportWidth` 1300), con los
  `circle`/`ellipse` reescritos como dos arcos, que es la única forma que admite el formato. El grupo
  envolvente escala el arte al **63 %** y lo centra. Ese 63 % no es arbitrario: el punto pintado más
  lejano del logo queda a 629 unidades del centro de su caja, y con el 68 % del icono anterior se
  saldría de la zona segura de 66 dp.
- **iOS**, `iosApp/iosApp/AppIcon.icon/Assets/citovision.png`: 1024×1024 con transparencia real, que
  se obtiene rasterizando dos veces —sobre blanco y sobre negro— y despejando el alfa.

Los diez rásteres de respaldo de `mipmap-*dpi` sí salen del maestro (48, 72, 96, 144 y 192 px), en dos
familias: la cuadrada usa el maestro tal cual y la redonda una variante recortada en círculo. Con
`minSdk 27` el icono adaptativo manda siempre, así que son legado.

## Aviso sobre el arte en Windows y Linux

El maestro está dibujado con la convención de **macOS**: el arte ocupa el 80,5 % del lienzo y el resto son
márgenes transparentes, porque el sistema aplica su propia máscara de squircle. Windows y Linux **no
recortan ni enmascaran**: pintan el PNG tal cual. Reutilizar el mismo arte funciona, pero ahí el icono se ve
algo más pequeño que los de las apps nativas, con aire alrededor.

Si molesta, la solución no es recortar el maestro por script —el recorte descuadra la sombra— sino dibujar
una variante que llene el lienzo y usarla solo para `.ico` y el PNG de Linux.
