# ADR-0008 - Barra de navegación nativa en iOS: SwiftUI superpuesta sobre Compose

## Estado

Aceptada — 2026-08-31

## Contexto

La barra inferior de citoVision es hoy un `NavigationBar` de Material 3 idéntico en las tres plataformas.
En Android es lo correcto; en iOS resulta ajeno: iOS 26 usa una barra **flotante en forma de cápsula** con
*Liquid Glass* (translucidez que desenfoca el contenido vivo de debajo) e iconos SF Symbols. `DESIGN.md`
§Mobile Specific Rules → iOS pide *"mantener comportamiento nativo cuando sea posible"*.

En agosto de 2026 se anotó como plan montar una **`UITabBar` real con `UIKitView`**. **Ese plan no
funciona**, y conviene dejar escrito por qué:

> Al embeber una vista UIKit con `UIKitView`, Compose **abre un agujero en su propia superficie Skia** para
> dejar ver el contenido UIKit, que queda **por debajo** de lo que Compose dibuja.

Liquid Glass consiste precisamente en **desenfocar lo que hay detrás**. Una `UITabBar` colocada por debajo
de la superficie de Compose no tiene delante nada que muestrear: se vería una barra nativa **plana y
opaca**. Se pagaría todo el coste del interop —pipeline de iconos, accesibilidad, métricas— sin obtener el
efecto que motiva el cambio.

JetBrains documenta la alternativa oficial
([Liquid Glass in a Compose Multiplatform app](https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html)):
**invertir la jerarquía**, con un `TabView` de SwiftUI por fuera que hospeda un `ComposeUIViewController` por
pestaña. El sistema pinta la barra y el efecto sale gratis.

**Pero eso choca con la arquitectura de esta app.** Aquí Compose no posee solo las pestañas: posee el
**Splash**, la **puerta de autenticación**, el `AppNavHost` y la navegación a Ajustes. Adoptar el `TabView`
obligaría a que Swift decidiera si hay sesión, a partir la UI en tres `ComposeUIViewController` y a
re-cablear por puentes el comportamiento cruzado de **SPEC-0006 RF-5b** (al guardar, saltar a Historial y
destellar la card nueva), que se acaba de validar.

## Decisión

**La barra nativa se dibuja en SwiftUI, superpuesta *por encima* de Compose**, no dentro ni por fuera.

La clave física es la posición en la jerarquía: si la barra es hermana **superior** de la vista de Compose,
debajo de ella sí hay una `UIView` real con el contenido de la app, así que `.glassEffect()` tiene qué
desenfocar. Es lo que resuelve el problema del agujero en la superficie sin invertir la arquitectura.

```
ContentView (SwiftUI)
└── ZStack
    ├── ComposeView()      ← la app entera: splash, login, navegación, pantallas
    └── NativeTabBar()     ← SwiftUI + .glassEffect() + SF Symbols
```

### 1. Un `expect/actual` propio, sin dependencias

`AppNavigationBar` en `commonMain` (`presentation/components`), con `AppNavigationItem` (etiqueta +
`Painter`). Sigue **descartada** la librería `adaptive-nav-bar`: sin target Desktop, Kotlin 2.3.21 y
Navigation3/Material3 alpha.

- **Android y Desktop**: delegan en una implementación Material 3 compartida en `commonMain`, para no
  duplicar el código real en dos *source sets*. Usa **`ShortNavigationBar`**, la barra de **Material 3
  Expressive**, que ya viene en el artefacto que trae Compose Multiplatform 1.10.3
  (`org.jetbrains.compose.material3:material3:1.9.0`) y **no exige opt-in experimental**. Con los valores
  por defecto la diferencia visible frente al `NavigationBar` clásico es **solo el alto del contenedor**
  (64 dp frente a 80 dp): el indicador activo y la disposición del ítem son idénticos. Se adopta por ser el
  componente al que Material da continuidad y por dejar disponibles `arrangement` e `iconPosition`, hoy sin
  usar. Material mantiene además a propósito la navegación **anclada y a todo el ancho**, al contrario que
  iOS 26.
- **iOS**: no dibuja nada **ni reserva hueco**. La barra *flota*, así que el contenido debe llegar hasta el
  borde inferior y pasar **por debajo** del cristal —es lo que hace que el efecto tenga sentido, y el patrón
  de cualquier app de iOS. Dos piezas lo consiguen:
  - `appScaffoldContentInsets()` excluye en iOS el borde inferior del `Scaffold`, para que el *viewport*
    llegue abajo del todo.
  - `floatingNavigationBarPadding()` devuelve el alto que la barra superpone (`0` en Android y Desktop), y
    los contenidos desplazables lo suman a su `contentPadding` final para que su **último elemento** pueda
    despejarse de la barra.

### 2. El estado sigue siendo de Compose

`MainScreen` conserva `selectedTabIndex` como **fuente única de verdad**. El puente solo transporta:

- **Kotlin → Swift**: qué pestañas hay, cuál está activa y si la barra debe verse.
- **Swift → Kotlin**: qué pestaña ha tocado el usuario.

Así **RF-5b sigue funcionando sin tocarlo**: cambiar de pestaña tras guardar ya era escribir en
`selectedTabIndex`, y ese cambio ahora se propaga solo a la barra nativa.

La visibilidad se deduce del **ciclo de vida de la composición**: la barra se anuncia al entrar en
composición y se oculta al salir. Como `AppNavigationBar` solo se usa en `MainScreen`, desaparece sola en
Splash, Login y Ajustes, sin lógica adicional.

### 3. El estado cruza como una clase, no como parámetros sueltos

```kotlin
class NativeTabBarState(val visible: Boolean, val labels: List<String>, val selectedIndex: Int)
```

Misma razón que en ADR-0007: los primitivos de un **parámetro de lambda** se empaquetan en `KotlinBoolean` /
`KotlinInt`, mientras que los de un **constructor** cruzan como `Bool` / `Int32` limpios.

### 4. Iconos: SF Symbols elegidos en Swift, etiquetas desde Kotlin

Las etiquetas viajan ya traducidas desde Compose Resources (fuente única de las traducciones ES/EN); el
símbolo lo decide Swift, porque es una cuestión de diseño de iOS. Verificado contra el catálogo del sistema
(`CoreGlyphs.bundle`): **no existe ningún símbolo `microscope`**. Se usan `microbe`, `list.bullet` y
`person.2`, los tres confirmados.

### 5. Degradación por versión

`.glassEffect()` existe desde **iOS 26**. Por debajo —el *deployment target* es 18.6— se usa
`.ultraThinMaterial` sobre la misma cápsula: no es Liquid Glass, pero sí una barra flotante translúcida y
nativa de aspecto.

## Alternativas consideradas

1. **`UITabBar` con `UIKitView`** *(el plan anotado en agosto)*. Descartada por el agujero en la superficie
   Skia descrito arriba: coste alto de interop y barra plana, sin el efecto buscado.
2. **`TabView` de SwiftUI por fuera** *(la vía oficial de JetBrains)*. Es la de mayor fidelidad —incluye
   comportamientos del sistema como minimizarse al hacer scroll— y sería la elegida en una app cuya
   navegación viviera en Swift. Descartada **aquí** por su coste: mueve a Swift la decisión de sesión,
   parte la UI en tres view controllers y obliga a re-cablear RF-5b, Ajustes y el splash. Revisable si algún
   día la navegación se lleva a nativo.
3. **Barra Compose con estilo iOS** (cápsula flotante, métricas y tipografía de iOS). Cero interop y cero
   riesgo, pero la translucidez sería un color semitransparente, no un desenfoque real. Descartada por no
   cumplir el objetivo.

## Consecuencias

**Positivas**

- Aspecto nativo real en iOS, con Liquid Glass efectivo, sin tocar la arquitectura de navegación.
- **Android y Desktop no cambian de comportamiento**: su `actual` delega en el mismo Material 3 de hoy.
- `MainScreen` conserva su estado y RF-5b sigue intacto.
- Sin dependencias nuevas en ninguna plataforma.
- El `expect/actual` deja la puerta abierta a que Desktop tenga su propia barra el día que interese.

**Negativas / deuda**

- **Una pieza más de Swift** y un puente más que mantener, con su registro en `project.pbxproj`.
- **La altura de la barra está acordada en dos sitios** (la constante de Kotlin que reserva el hueco y el
  layout de SwiftUI). Si una cambia y la otra no, aparece un desajuste; queda documentado en ambos ficheros.
- **La barra flota por encima de todo lo que dibuje Compose**, incluidos sus diálogos: en Android un diálogo
  tapa la barra y en iOS pasaría al revés. Se resuelve con `ModalOverlayEffect()`, que cada diálogo invoca
  para declararse superficie modal; mientras haya alguno abierto, la barra se retira. **Es una convención a
  recordar**: un diálogo nuevo bajo las pestañas que no lo llame quedará tapado por la barra.
- No se heredan comportamientos del sistema que sí daría un `TabView` real (minimizar al hacer scroll).
- La barra de iOS no se puede probar desde `commonTest`: solo la lógica de selección, que sigue en Compose.

## Verificación

La ejecuta el desarrollador (AGENTS.md §16):

1. `./gradlew :shared:ktlintCheck` y `./gradlew :shared:allTests`.
2. **Android y Desktop sin regresión**: la barra se ve y se comporta exactamente igual que antes.
3. En iOS: la barra flotante aparece sobre el contenido, con los tres iconos y sus etiquetas traducidas.
4. Tocar cada pestaña cambia la pantalla, y la pestaña activa se marca.
5. **RF-5b**: guardar un análisis salta a Historial y **la barra refleja el cambio** sin tocarla.
6. La barra **no aparece** en Splash, Login ni Ajustes.
7. Con el diálogo de detalle abierto, comprobar si la barra lo tapa (ver deuda).
8. Tema claro y oscuro, y comprobar el aspecto en un dispositivo con iOS < 26 si hay uno a mano.

## Referencias

- **DESIGN.md** §Navigation y §Mobile Specific Rules → iOS ("mantener comportamiento nativo cuando sea
  posible").
- **ADR-0004** (bring-up iOS) y **ADR-0006** / **ADR-0007** (patrón de puente Kotlin↔Swift que aquí se
  reutiliza).
- **SPEC-0006** RF-5b (cambio de pestaña automático tras guardar).
- Liquid Glass en Compose Multiplatform: `https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html`
- Integración con UIKit (el agujero en la superficie):
  `https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html`
- AGENTS.md §2, §3, §5, §6, §8, §15.
