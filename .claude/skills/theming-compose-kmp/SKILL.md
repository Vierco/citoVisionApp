---
name: theming-compose-kmp
description: "Crear el sistema de tema por primera vez, crear un nuevo componente reutilizable de UI (botón, card, estado de pantalla), o decidir qué color/spacing/radio usar en una pantalla."
---

# Skill: theming-compose-kmp

## Objetivo

Definir cómo implementar el sistema de diseño de `DESIGN.md` (colores, tipografía, spacing, elevation, corner radius, componentes reutilizables, accesibilidad) en Compose Multiplatform (Android, iOS, Desktop), dentro de `ui/theme` y `ui/customviews` (ver `ARCHITECTURE.md`).

Este Skill traduce las reglas de `DESIGN.md` a código Compose Multiplatform; no redefine los valores de diseño en sí — esos viven únicamente en `DESIGN.md`. Si `DESIGN.md` cambia, los valores se actualizan ahí; este Skill sigue aplicando igual.

---

## Modelo conceptual

```text
ui/
  theme/
    AppColors.kt          ← ColorScheme de Material3 mapeado desde DESIGN.md
    AppExtendedColors.kt    ← pressed + semánticos (success/warning/info) — sin slot en Material3
    AppTypography.kt         ← Typography (solo los slots definidos en DESIGN.md)
    AppShapes.kt               ← Shapes (small/medium/large/extraLarge)
    AppSpacing.kt                ← spacing (xs..xxxl) — no es parte de Material3
    AppElevation.kt                ← elevation (none/low/medium/high) — no es parte de Material3
    AppTheme.kt                      ← MaterialTheme + CompositionLocal con los tokens extendidos
  customviews/
    AppPrimaryButton.kt
    AppSecondaryButton.kt
    AppDestructiveButton.kt
    StandardCard.kt
    ErrorCard.kt
    ScreenState.kt                    ← Loading / Empty / Error / Success (DESIGN.md "Screen States")
```

`AppTheme` se aplica una sola vez en la raíz del árbol de Composables (`App()`) y envuelve `MaterialTheme`, añadiendo los tokens que Material3 no cubre de forma nativa.

---

## Cuándo usarlo

- Crear el sistema de tema por primera vez en el proyecto.
- Crear un nuevo componente reutilizable de UI (botón, card, estado de pantalla).
- Decidir qué color/spacing/radio usar en una pantalla nueva.
- Revisar si una pantalla respeta el sistema de diseño.
- Implementar accesibilidad (contraste, Dynamic Type, screen readers) en un componente.

## Cuándo NO usarlo

- Para diseñar la navegación entre pantallas → usar `navigation-compose-kmp`.
- Para diseñar el ViewModel/`UiState` de una pantalla → usar `mvvm-compose-kmp`.
- Para gestionar recursos (imágenes, strings, fuentes) como ficheros multiplataforma → corresponde a un Skill de recursos de Compose; este Skill asume que la fuente Dongle ya está disponible como recurso y solo define cómo se referencia desde `AppTypography`.
- Para modificar valores de diseño (colores, spacing, tipografía) → esos cambios se hacen en `DESIGN.md`, nunca inventándolos directamente en código.

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
```

Documento de referencia obligatoria (no es un Skill): `DESIGN.md`.

---

## Entradas necesarias

- `DESIGN.md` actualizado (colores, tipografía, spacing, shapes, elevation, componentes).
- Estado de la pantalla a construir (Loading/Success/Empty/Error, ver `DESIGN.md` "Screen States").
- Comprobación de si el componente a crear ya existe en `ui/customviews` (regla explícita de `DESIGN.md`, instrucción 4: reutilizar antes de crear).

---

## Criterios arquitectónicos

- Todos los valores de diseño (color, spacing, radio, tipografía) provienen única y exclusivamente de `DESIGN.md`; no se inventan valores ad-hoc en una pantalla.
- Material3 (`MaterialTheme`, `ColorScheme`, `Typography`, `Shapes`) se usa como base, pero `DESIGN.md` define varios conceptos que Material3 no tiene de forma nativa (spacing, elevation como tokens nombrados, colores "pressed" explícitos, colores semánticos success/warning/info): esos se exponen mediante un `AppTheme` propio que envuelve `MaterialTheme`.
- El Dark Mode está explícitamente deshabilitado en `DESIGN.md` (`enabled: false`); `AppTheme` no debe consultar `isSystemInDarkTheme()` ni cambiar de esquema de color según el sistema mientras esa bandera siga en `false`.
- Los componentes reutilizables (botones, cards, estados de pantalla) viven en `ui/customviews`; antes de crear un componente nuevo, debe comprobarse si ya existe uno equivalente.
- La tipografía usada en pantallas se limita a los slots de `Typography` definidos explícitamente en `DESIGN.md` (`displayLarge`, `headlineLarge`, `headlineMedium`, `titleLarge`, `bodyLarge`, `bodyMedium`, `labelLarge`); no se usan slots de Material3 que `DESIGN.md` no ha definido (ej. `titleMedium`, `bodySmall`).

---

## Reglas

- `AppTheme` debe aplicarse una única vez, en la raíz de la app (`App()`), envolviendo todo el contenido.
- Los colores deben mapearse desde `DESIGN.md` a un `ColorScheme` de Material3 (`primary`, `onPrimary`, `secondary`, `onSecondary`, `tertiary`, `onTertiary`, `background`, `onBackground`, `surface`, `onSurface`, `error`); los slots de `ColorScheme` que `DESIGN.md` no define (ej. `primaryContainer`, `surfaceVariant`) deben dejarse en el valor por defecto de Material3, no inventarse.
- Los colores "pressed" (`primaryPressed`, `secondaryPressed`, `errorPressed`) y los semánticos (`success`, `warning`, `info`) no tienen slot en `ColorScheme`; deben exponerse en un objeto propio (`AppExtendedColors`) accesible vía `AppTheme`, no forzarse dentro de los slots de Material3.
- El estado "pressed" de un botón debe aplicar el color `xPressed` correspondiente usando el `interactionSource` del componente (`collectIsPressedAsState()`), no depender únicamente del state-layer/opacity por defecto de Material3.
- `spacing` y `elevation` deben definirse como objetos propios (`AppSpacing`, `AppElevation`) con los valores exactos de `DESIGN.md`; no deben usarse valores de `dp` sueltos directamente en un Composable de pantalla.
- `shapes` debe mapearse a `Shapes` de Material3 usando `small`/`medium`/`large`/`extraLarge` de `DESIGN.md`; el slot `extraSmall` de Material3 (no definido en `DESIGN.md`) se deja en su valor por defecto.
- La fuente Dongle debe cargarse como recurso de Compose Multiplatform (`FontFamily` con sus variantes regular/bold) y aplicarse en `AppTypography`, nunca como una fuente del sistema por defecto.
- Todo componente táctil (botón, card pulsable, ítem de lista) debe respetar el `minimumTouchTarget` de 48dp definido en `DESIGN.md`.
- En iOS, los Composables de pantalla completa deben respetar las Safe Areas (`WindowInsets`/equivalente de Compose Multiplatform); en Android, la app debe soportar edge-to-edge.
- Antes de crear un componente nuevo en `ui/customviews`, debe comprobarse si ya existe uno que cubra la necesidad; dos componentes visualmente equivalentes para el mismo propósito es una violación de esta regla.
- Cualquier combinación de color nueva (texto sobre fondo) debe cumplir contraste WCAG AA antes de usarse; si no se puede verificar, no se introduce el color hasta confirmarlo.

---

## Decisiones automáticas

```text
Si un valor de diseño (color, spacing, radio, tipografía) no está definido en DESIGN.md
    → no se inventa; se usa el valor por defecto de Material3 o se pregunta antes de añadirlo a DESIGN.md

Si ya existe un componente en ui/customviews que cubre la necesidad visual
    → se reutiliza ese componente, no se crea uno nuevo

Si un componente necesita un color de estado "pressed" explícito (no solo opacity)
    → se resuelve con interactionSource + AppExtendedColors, no con el comportamiento por defecto de Material3

Si Dark Mode sigue en enabled: false en DESIGN.md
    → AppTheme usa siempre el único ColorScheme definido, sin consultar isSystemInDarkTheme()

Si DESIGN.md cambia enabled a true en el futuro
    → este Skill debe revisarse para soportar un segundo ColorScheme (dark) y la lógica de selección de tema
```

---

## Proceso recomendado

1. Mapear los colores de `DESIGN.md` a un `ColorScheme` de Material3 + un `AppExtendedColors` para los que no tienen slot.
2. Definir `AppTypography` cargando la fuente Dongle y mapeando únicamente los slots definidos en `DESIGN.md`.
3. Definir `AppShapes`, `AppSpacing` y `AppElevation` como objetos propios con los valores exactos de `DESIGN.md`.
4. Construir `AppTheme` envolviendo `MaterialTheme` y exponiendo los tokens extendidos vía `CompositionLocal`.
5. Aplicar `AppTheme` una sola vez en la raíz de la app.
6. Construir los componentes de `DESIGN.md` (botones, cards, estados de pantalla) en `ui/customviews`, usando solo los tokens de los pasos 1-3.
7. Antes de crear cualquier componente nuevo, revisar `ui/customviews` para comprobar si ya existe uno equivalente.
8. Verificar accesibilidad (contraste, `contentDescription`, soporte de Dynamic Type vía `sp`) en cada componente nuevo.

---

## Checklist

- [ ] `AppTheme` se aplica una sola vez, en la raíz de la app.
- [ ] Todos los colores usados en pantallas vienen de `MaterialTheme.colorScheme` o de `AppExtendedColors`, nunca de un `Color(0x...)` suelto.
- [ ] El spacing usado en pantallas viene de `AppSpacing`, nunca de un `.dp` suelto.
- [ ] La tipografía usada se limita a los slots definidos en `DESIGN.md`.
- [ ] Dark Mode no se consulta dinámicamente mientras `DESIGN.md` lo tenga deshabilitado.
- [ ] Se comprobó `ui/customviews` antes de crear un componente nuevo.
- [ ] Los componentes táctiles respetan el `minimumTouchTarget` de 48dp.
- [ ] Las pantallas completas respetan Safe Areas en iOS y edge-to-edge en Android.

---

## Definition of Done

- La app se ve visualmente idéntica en Android, iOS y Desktop usando los mismos tokens de `AppTheme`.
- Ningún color, spacing, radio o estilo de texto está hardcodeado fuera de `ui/theme`.
- Los componentes de `DESIGN.md` (botones, cards, estados de pantalla) existen en `ui/customviews` y se reutilizan en todas las pantallas.
- El contraste de las combinaciones de color usadas cumple WCAG AA.

---

## Riesgos

- Colores/spacing/radios hardcodeados en pantallas individuales, perdiendo consistencia y dificultando un futuro rediseño.
- Implementar dark mode dinámico antes de que `DESIGN.md` lo habilite explícitamente.
- Duplicar componentes visualmente equivalentes en `ui/customviews` por no comprobar los existentes antes de crear uno nuevo.
- Usar slots de `Typography`/`ColorScheme` de Material3 no definidos en `DESIGN.md`, generando inconsistencia con el resto de la app.
- Combinaciones de color con contraste insuficiente, afectando la accesibilidad.

---

## Anti-patrones

- `Color(0xFF2FA7F0)` escrito directamente en un Composable de pantalla en lugar de `MaterialTheme.colorScheme.primary`.
- `Modifier.padding(16.dp)` con un valor suelto en lugar de `AppSpacing.md`.
- Crear `SecondaryCard` cuando ya existe `StandardCard` cubriendo el mismo caso.
- `if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()` cuando `DESIGN.md` tiene Dark Mode deshabilitado.
- Usar `MaterialTheme.typography.titleMedium` cuando ese slot no está definido en `DESIGN.md`.

---

## Comandos útiles

N/A — este Skill no depende de comandos CLI específicos; la carga de la fuente Dongle como recurso depende del sistema de Compose Resources del proyecto.

---

## Salida esperada

```text
ui/
  theme/
    AppColors.kt
    AppExtendedColors.kt
    AppTypography.kt
    AppShapes.kt
    AppSpacing.kt
    AppElevation.kt
    AppTheme.kt
  customviews/
    AppPrimaryButton.kt
    AppSecondaryButton.kt
    AppDestructiveButton.kt
    StandardCard.kt
    ErrorCard.kt
    ScreenState.kt
```

---

## Ejemplos

### Correcto — Colores (DESIGN.md → ColorScheme + AppExtendedColors)

```kotlin
// ui/theme/AppColors.kt
val AppColorScheme = lightColorScheme(
    primary = Color(0xFF2FA7F0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2FD38A),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFA56AE3),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF282828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF6F6F6F),
    error = Color(0xFFF53A63),
    onError = Color(0xFFFFFFFF)
)

// ui/theme/AppExtendedColors.kt
data class AppExtendedColors(
    val primaryPressed: Color,
    val secondaryPressed: Color,
    val errorPressed: Color,
    val success: Color,
    val warning: Color,
    val info: Color
)

val DefaultExtendedColors = AppExtendedColors(
    primaryPressed = Color(0xFF227FC0),
    secondaryPressed = Color(0xFF1E9E67),
    errorPressed = Color(0xFFC71C43),
    success = Color(0xFF2FD38A),
    warning = Color(0xFFF59E3A),
    info = Color(0xFF2FA7F0)
)

val LocalAppExtendedColors = staticCompositionLocalOf { DefaultExtendedColors }
```

### Correcto — Spacing y Elevation

```kotlin
// ui/theme/AppSpacing.kt
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

// ui/theme/AppElevation.kt
object AppElevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 6.dp
    val high = 12.dp
}
```

### Correcto — AppTheme (envuelve MaterialTheme)

```kotlin
// ui/theme/AppTheme.kt
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppExtendedColors provides DefaultExtendedColors) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

object AppThemeTokens {
    val colors: AppExtendedColors
        @Composable get() = LocalAppExtendedColors.current
    val spacing get() = AppSpacing
    val elevation get() = AppElevation
}
```

### Correcto — Botón primario con color "pressed" explícito

```kotlin
// ui/customviews/AppPrimaryButton.kt
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = if (isPressed) {
        AppThemeTokens.colors.primaryPressed
    } else {
        MaterialTheme.colorScheme.primary
    }

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
```

### Correcto — Standard Card

```kotlin
// ui/customviews/StandardCard.kt
@Composable
fun StandardCard(
    title: String,
    body: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.high),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(AppSpacing.sm))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(AppSpacing.md))
            AppPrimaryButton(text = actionLabel, onClick = onActionClick)
        }
    }
}
```

### Incorrecto

```kotlin
// ❌ Color hardcodeado fuera de ui/theme
Text("Hola", color = Color(0xFF2FA7F0))

// ❌ Spacing suelto
Column(modifier = Modifier.padding(16.dp)) { /* ... */ }

// ❌ Dark mode dinámico cuando DESIGN.md lo tiene deshabilitado
MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else AppColorScheme) { /* ... */ }

// ❌ Slot de Typography no definido en DESIGN.md
Text("Subtítulo", style = MaterialTheme.typography.titleMedium)

// ❌ Componente duplicado
@Composable
fun SecondaryCard(/* idéntico a StandardCard */) { }
```

---

## Referencias

- DESIGN.md — colores, tipografía, spacing, shapes, elevation, componentes, accesibilidad
- clean-architecture-kmp
- mvvm-compose-kmp
- ARCHITECTURE.md — sección UI
