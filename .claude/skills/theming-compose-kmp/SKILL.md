---
name: theming-compose-kmp
description: "Crear el sistema de tema por primera vez, crear un nuevo componente reutilizable de UI (botón, card, estado de pantalla), o decidir qué color/spacing/radio usar en una pantalla."
---

# Skill: theming-compose-kmp

## Objetivo

Definir cómo se aplica el sistema de diseño de `DESIGN.md` (colores claro/oscuro, tipografía Dongle, spacing,
elevation, corner radius, componentes reutilizables, accesibilidad) en Compose Multiplatform (Android, iOS,
Desktop), dentro de `ui/theme` y `presentation/components` (ver `ARCHITECTURE.md`).

Este Skill traduce las reglas de `DESIGN.md` a código Compose; no redefine los valores de diseño, que viven
únicamente en `DESIGN.md`. Si `DESIGN.md` cambia, los valores se actualizan en `Color.kt`/`Typography.kt`;
este Skill sigue aplicando igual.

**El sistema de tema ya existe y tiene modo oscuro.** Este Skill describe cómo está hecho para extenderlo
con coherencia, no para crearlo de nuevo.

---

## Modelo conceptual

```text
ui/theme/
  Color.kt                  ← tokens `val` de DESIGN.md: paleta clara y, con prefijo `dark`, la oscura
  Theme.kt                  ← AppExtendedColors + LocalAppColors + CitoVisionTheme(darkTheme)
  Typography.kt             ← getTypography(): Dongle (Regular/Bold/Light) en los 7 slots de DESIGN.md
  SystemBarsAppearance.kt   ← expect fun SystemBarsAppearance(darkTheme): iconos de barras (Android; no-op en iOS/Desktop)

presentation/components/
  Components.kt             ← AnalysisCard, AnalysisDetailDialog, PriorityBadge (+ privados: ShimmerBox, ImageFallback…)
  AppNavigationBar.kt       ← barra de pestañas (ShortNavigationBar en Android/Desktop; nativa en iOS, ADR-0008)
  ModalOverlay.kt           ← ModalOverlayEffect(): avisa a la barra nativa de iOS de que hay un diálogo abierto
  IconAlignment.kt          ← Modifier.dongleIconAlign(): compensa las métricas de Dongle junto a un icono
  SanitizedFieldState.kt    ← rememberSanitizedField(): campos cuyo texto sanea el ViewModel
```

`CitoVisionTheme` se aplica **una sola vez**, en `App.kt`, que resuelve el tema efectivo a partir de la
preferencia persistida (`ObserveThemePreferenceUseCase`: Claro / Oscuro / Seguir sistema) y llama antes a
`SystemBarsAppearance(darkTheme)`.

La carpeta `ui/customviews` que cita `ARCHITECTURE.md` **no existe**: los componentes reutilizables viven en
`presentation/components`.

---

## Cuándo usarlo

- Crear un nuevo componente reutilizable de UI (botón, card, estado de pantalla, diálogo).
- Decidir qué color/spacing/radio/estilo de texto usar en una pantalla nueva.
- Añadir o cambiar un token de color (implica **las dos paletas**, clara y oscura).
- Revisar si una pantalla respeta el sistema de diseño y se ve bien en oscuro.
- Implementar accesibilidad (contraste, escalado de fuente, screen readers) en un componente.

## Cuándo NO usarlo

- Para diseñar la navegación entre pantallas → usar `navigation-compose-kmp`.
- Para diseñar el ViewModel/`UiState` de una pantalla → usar `mvvm-compose-kmp`.
- Para gestionar recursos (imágenes, strings, fuentes) → usar `compose-resources-kmp`; este Skill asume que
  Dongle ya está en `composeResources/font` y solo define cómo se referencia desde `Typography.kt`.
- Para modificar valores de diseño (colores, spacing, tipografía) → esos cambios se hacen en `DESIGN.md`
  primero (fichero de reglas: necesita confirmación humana), nunca inventándolos en código.
- Para tocar la barra de pestañas nativa de iOS → ADR-0008 y `NativeTabBarBridge.kt`, no este Skill.

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
- compose-resources-kmp (fuente Dongle, iconos vectoriales)
```

Documento de referencia obligatoria (no es un Skill): `DESIGN.md`.

---

## Entradas necesarias

- `DESIGN.md` actualizado (paleta clara **y** oscura, tipografía, spacing, radios, elevation, componentes).
- Estado de la pantalla a construir (Loading/Success/Empty/Error, ver `DESIGN.md` "Screen States").
- Comprobación de si el componente a crear ya existe en `presentation/components` (regla explícita de
  `DESIGN.md`, instrucción 4: reutilizar antes de crear).

---

## Criterios arquitectónicos

- Todos los valores de diseño provienen de `DESIGN.md`; no se inventan valores ad-hoc en una pantalla.
- Material3 (`MaterialTheme`, `ColorScheme`, `Typography`) es la base. Lo que Material3 no cubre con un slot
  (`primaryPressed`, `secondaryPressed`, `errorPressed`, `success`, `warning`, `info`, `hint`,
  `secondaryDark`) se expone en `AppExtendedColors` vía `LocalAppColors`, y **tiene versión oscura**.
- **Modo oscuro habilitado y elegible por el usuario** (`DESIGN.md` "Dark Mode": `enabled: true`,
  opciones claro/oscuro/sistema, por defecto sistema). Un componente nuevo se da por terminado cuando se ha
  visto en los dos temas.
- `secondaryDark` **no deriva de `secondary`**: es el verde semántico de la prioridad BAJA y sigue verde
  aunque la marca sea índigo. El semáforo de prioridad (error / warning / secondaryDark) no se rebrandea.
- Los componentes reutilizables viven en `presentation/components`; antes de crear uno nuevo se comprueba
  si ya existe uno equivalente.
- La tipografía se limita a los slots que `Typography.kt` define con Dongle (`displayLarge`, `headlineLarge`,
  `headlineMedium`, `titleLarge`, `bodyLarge`, `bodyMedium`, `labelLarge`). **Cualquier otro slot
  (`titleMedium`, `bodySmall`, `labelMedium`…) cae al valor por defecto de Material3: fuente del sistema y
  tamaños de Material, no Dongle.** Si hace falta un tamaño nuevo, se propone en `DESIGN.md` y se añade a
  `Typography.kt`; no se usa un slot sin definir.

---

## Reglas

- `CitoVisionTheme` se aplica una única vez, en `App.kt`. Ninguna pantalla ni componente lo vuelve a envolver.
- Los colores de pantalla se leen de `MaterialTheme.colorScheme.*` o de `LocalAppColors.current.*`; nunca
  de un `Color(0x…)` suelto fuera de `ui/theme/Color.kt`. Hoy no hay ninguno fuera y así debe seguir.
- Al añadir un token de color: `DESIGN.md` (ambas paletas) → `Color.kt` (`nombre` y `darkNombre`) →
  `AppExtendedColors` + `LightExtendedColors` + `DarkExtendedColors` en `Theme.kt`. Un token sin versión
  oscura es un bug visible.
- Los slots de `ColorScheme` que `DESIGN.md` no define (`primaryContainer`, `surfaceVariant`…) se dejan en el
  valor por defecto de `lightColorScheme()`/`darkColorScheme()`; no se inventan.
- **`LocalContentColor` lo fija `CitoVisionTheme` a `onBackground`.** Sin eso, fuera de un `Surface` el
  contenido por defecto sería negro e invisible en oscuro. No usar `Surface` como envoltorio solo para
  arreglar colores; leer el token que toque.
- `drawBehind`, `Canvas` y otros lambdas de dibujo **no leen `MaterialTheme`**: los colores se capturan
  fuera, en la composición, y se pasan al lambda (ver `ShimmerBox` en `Components.kt`).
- Spacing, radios y elevation se escriben con los valores exactos de `DESIGN.md` (spacing 4/8/16/24/32/48/64
  dp; radios 12/16/20/28 dp; elevation 0/2/6/12 dp). No hay objetos de tokens (`AppSpacing`…): la
  convención del repo es el valor inline **que coincida con la tabla**. Un `13.dp` o un `10.dp` es un olor.
- Dongle tiene métricas verticales atípicas: un `Text` alineado con un `Icon` en una `Row` queda
  desplazado. Se corrige con `Modifier.dongleIconAlign()` sobre el icono, no con paddings a ojo.
- Todo componente táctil (botón, card pulsable, ítem de lista) respeta el `minimumTouchTarget` de 48dp.
- Pantallas completas: Safe Areas en iOS, edge-to-edge en Android (`AppNavigationBar.kt` da
  `appScaffoldContentInsets()` y `floatingNavigationBarPadding()` por plataforma).
- Los diálogos que se abren sobre la pantalla principal llaman a `ModalOverlayEffect()` para que la barra
  nativa de iOS se atenúe (ADR-0008).
- Un campo de texto cuyo valor sanea el ViewModel (código de paciente) usa `rememberSanitizedField`; la
  sobrecarga de `TextField` con `String` descoloca el cursor cuando el texto vuelve distinto.
- Cualquier combinación de color nueva (texto sobre fondo) cumple contraste WCAG AA **en las dos paletas**
  antes de usarse. En oscuro, un mismo color no puede cumplir AA a la vez como relleno con texto blanco y
  como texto sobre `#111318` (los rangos de luminancia no se solapan): la paleta oscura es un compromiso
  documentado en `Color.kt`, no un descuido. No "corregirla" sin leer ese comentario.

---

## Decisiones automáticas

```text
Si un valor de diseño (color, spacing, radio, tipografía) no está en DESIGN.md
    → no se inventa; se propone en DESIGN.md (con confirmación humana) antes de usarlo en código

Si ya existe un componente en presentation/components que cubre la necesidad visual
    → se reutiliza, no se crea uno nuevo

Si un color nuevo se añade a la paleta clara
    → se añade también a la oscura, en Color.kt y en los dos AppExtendedColors, en el mismo cambio

Si un componente necesita el estado "pressed" con color explícito
    → LocalAppColors.current.xPressed + interactionSource (collectIsPressedAsState); hoy ningún componente
      lo usa y se confía en el state layer de Material3, así que solo si DESIGN.md lo pide para ese componente

Si un texto necesita un tamaño que no está en los 7 slots de Typography.kt
    → no usar un slot sin definir (saldría en fuente del sistema); proponer el slot en DESIGN.md

Si un color se necesita dentro de drawBehind / Canvas
    → capturarlo en la composición y pasarlo al lambda
```

---

## Proceso recomendado

1. Leer `DESIGN.md` (paleta clara **y** oscura, tipografía, spacing, radios, componentes, Screen States).
2. Comprobar en `presentation/components` si ya existe un componente equivalente.
3. Elegir los tokens: `MaterialTheme.colorScheme.*` para los slots de Material3, `LocalAppColors.current.*`
   para los extendidos, slots de `Typography.kt` para el texto, valores de la tabla para dp.
4. Si falta un token, proponerlo en `DESIGN.md` y, tras confirmación, añadirlo en `Color.kt`/`Theme.kt`
   (ambas paletas) o `Typography.kt`.
5. Construir el componente stateless (estado por parámetros, acciones por callbacks), con `Modifier` como
   parámetro y una `@Preview` si es relevante.
6. Verificar en claro **y** oscuro, en Android e iOS (paridad visual, `DESIGN.md` instrucción 6).
7. Verificar accesibilidad: contraste, `contentDescription`, tamaños en `sp`, tamaño táctil.

---

## Checklist

- [ ] `CitoVisionTheme` sigue aplicándose una sola vez, en `App.kt`.
- [ ] Ningún `Color(0x…)` fuera de `ui/theme/Color.kt`.
- [ ] Todo token nuevo tiene versión clara y oscura, y está en `AppExtendedColors` si no tiene slot Material3.
- [ ] Solo se usan los 7 slots de `Typography.kt`.
- [ ] Los dp usados están en las tablas de spacing/radio/elevation de `DESIGN.md`.
- [ ] Se comprobó `presentation/components` antes de crear un componente nuevo.
- [ ] Los componentes táctiles respetan 48dp.
- [ ] Safe Areas en iOS y edge-to-edge en Android; `ModalOverlayEffect()` en diálogos sobre la pantalla principal.
- [ ] Visto en claro y en oscuro; contraste AA en ambos.

---

## Definition of Done

- La app se ve coherente en Android, iOS y Desktop, en claro y en oscuro, con los mismos tokens.
- Ningún color, spacing, radio o estilo de texto fuera de lo que define `DESIGN.md`.
- Los componentes reutilizables viven en `presentation/components` y se reutilizan en todas las pantallas.
- El contraste de las combinaciones de color usadas cumple WCAG AA en las dos paletas.

---

## Riesgos

- Añadir un color solo a la paleta clara: se ve bien en el dispositivo del desarrollador y mal en el de
  la mitad de los usuarios.
- Usar `titleMedium`/`bodySmall`/`labelMedium`: no fallan, pero salen en fuente del sistema, no en Dongle.
- Colores/spacing/radios fuera de tabla en pantallas individuales, perdiendo consistencia.
- Leer `MaterialTheme` dentro de `drawBehind`: no compila o dibuja con el color equivocado.
- Duplicar componentes visualmente equivalentes por no mirar `presentation/components`.
- "Arreglar" la paleta oscura para que cumpla AA en un papel y romperla en el otro.

---

## Anti-patrones

- `Color(0xFF4E54EA)` escrito en un Composable de pantalla en lugar de `MaterialTheme.colorScheme.primary`.
- Un `val darkNuevoColor` que falta en `DarkExtendedColors` (o al revés).
- `Modifier.padding(13.dp)` cuando la tabla dice 12 o 16.
- `Text(style = MaterialTheme.typography.titleMedium)`: slot sin definir en `Typography.kt`.
- `Surface { … }` envolviendo un trozo de pantalla solo para que los iconos dejen de salir negros.
- `if (isSystemInDarkTheme()) …` dentro de una pantalla: el tema efectivo lo decide `App.kt`, la pantalla
  solo lee tokens.
- Crear `SecondaryCard` cuando `AnalysisCard` ya cubre el caso.

---

## Comandos útiles

N/A — este Skill no depende de comandos CLI. Para comprobar que no hay colores sueltos:

```bash
grep -rn "Color(0x" shared/src/commonMain/kotlin | grep -v ui/theme
```

---

## Salida esperada

Un componente en `presentation/components` (o una pantalla en `presentation/screens`) que solo consume
tokens de `MaterialTheme`, `LocalAppColors` y los valores de tabla de `DESIGN.md`, verificado en claro y oscuro.
Si el cambio añade tokens, toca `DESIGN.md` (con confirmación), `Color.kt` y `Theme.kt`.

---

## Ejemplos

### Correcto — Tema (así está en `Theme.kt`)

```kotlin
data class AppExtendedColors(
    val primaryPressed: Color,
    val secondaryPressed: Color,
    val secondaryDark: Color,
    val hint: Color,
    val success: Color,
    val warning: Color,
    val errorPressed: Color,
    val info: Color,
)

val LocalAppColors = staticCompositionLocalOf { LightExtendedColors }

@Composable
fun CitoVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    MaterialTheme(colorScheme = colorScheme, typography = getTypography()) {
        CompositionLocalProvider(
            LocalAppColors provides extendedColors,
            LocalContentColor provides colorScheme.onBackground,
        ) {
            content()
        }
    }
}
```

### Correcto — Leer tokens en un componente (extracto de `PriorityBadge`)

```kotlin
val color =
    when (priority) {
        Priority.ALTA -> MaterialTheme.colorScheme.error
        Priority.MEDIA -> LocalAppColors.current.warning
        Priority.BAJA -> LocalAppColors.current.secondaryDark // verde semántico, no de marca
    }
Row(
    modifier =
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) { /* Icon + Text con tint/color = color */ }
```

### Correcto — Color dentro de un lambda de dibujo

```kotlin
val base = MaterialTheme.colorScheme.surface // capturado en la composición
Box(
    modifier
        .drawBehind { drawRect(base) }, // el lambda no puede leer MaterialTheme
)
```

### Correcto — Añadir un token nuevo (los tres sitios)

```kotlin
// Color.kt
val highlight = Color(0xFF…)
val darkHighlight = Color(0xFF…)

// Theme.kt
data class AppExtendedColors(/* … */, val highlight: Color)
private val LightExtendedColors = AppExtendedColors(/* … */, highlight = highlight)
private val DarkExtendedColors = AppExtendedColors(/* … */, highlight = darkHighlight)
```

### Incorrecto

```kotlin
// ❌ Color hardcodeado fuera de ui/theme
Text("Hola", color = Color(0xFF4E54EA))

// ❌ Token solo en claro: en oscuro DarkExtendedColors no compila o, si se copia el claro, no se lee
val highlight = Color(0xFF…) // sin darkHighlight

// ❌ Slot de Typography sin definir: sale en fuente del sistema
Text("Subtítulo", style = MaterialTheme.typography.titleMedium)

// ❌ Decidir el tema en una pantalla
MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme) { /* … */ }

// ❌ Leer el tema dentro del dibujo
Modifier.drawBehind { drawRect(MaterialTheme.colorScheme.surface) }
```

---

## Referencias

- DESIGN.md — colores (claro y oscuro), tipografía, spacing, shapes, elevation, componentes, accesibilidad
- ARCHITECTURE.md — sección UI (nota: cita `ui/customviews`; la carpeta real es `presentation/components`)
- ADR-0008 — barra de pestañas nativa en iOS (`ModalOverlayEffect`, `NativeTabBarBridge`)
- clean-architecture-kmp, mvvm-compose-kmp, compose-resources-kmp
