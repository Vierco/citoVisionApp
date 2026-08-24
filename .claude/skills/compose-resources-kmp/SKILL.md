---
name: compose-resources-kmp
description: "Añadir una imagen, icono o fuente compartida entre plataformas, o añadir/traducir un string de interfaz."
---

# Skill: compose-resources-kmp

## Objetivo

Definir cómo organizar y usar el sistema oficial de recursos de Compose Multiplatform (`composeResources`) — imágenes, fuentes y strings — en `commonMain`, generando el accesor type-safe `Res`, dentro de la carpeta `composeResources` ya prevista en `ARCHITECTURE.md`.

Este Skill no es una librería de terceros: es el sistema de recursos propio de Compose Multiplatform (`compose.components.resources`), estable desde la línea `1.6.x` del framework.

---

## Modelo conceptual

```text
shared/src/commonMain/composeResources/
        ├── drawable/   ← imágenes (PNG, JPEG, WebP, vector XML sin referencias a recursos Android)
        ├── font/        ← fuentes (.ttf/.otf)
        ├── values/        ← strings.xml (formato Android); values-es/, values-en/... para otros idiomas
        └── files/           ← otros ficheros (JSON, raw...)

        │  (build/sync de Gradle)
        ▼
Res (generado automáticamente)
        │
   ┌────┴──────┬────────────┐
   ▼            ▼             ▼
Res.drawable   Res.font     Res.string
   │            │              │
painterResource()  Font()   stringResource()
```

Un recurso nunca se referencia por path o string suelto: siempre a través del objeto `Res` generado por el compilador tras sincronizar el proyecto.

---

## Cuándo usarlo

- Añadir una imagen, icono o ilustración compartida entre Android, iOS y Desktop.
- Cargar la fuente Dongle (ver `theming-compose-kmp`/`DESIGN.md`).
- Añadir o traducir un string de interfaz.
- Decidir si un recurso va en `composeResources` o se gestiona de otra forma (recurso específico de una sola plataforma).

## Cuándo NO usarlo

- Para definir los valores de diseño en sí (qué tipografía, qué color) → ver `theming-compose-kmp`/`DESIGN.md`; este Skill solo resuelve cómo se cargan los ficheros físicos (ej. la fuente), no qué estilo se le aplica.
- Para configuración de build o secretos → ver `build-config-kmp`.
- Para recursos exclusivos de una sola plataforma (icono de notificación Android, asset de splash nativo de Xcode) → esos siguen viviendo en `androidMain/res` o en el proyecto Xcode, no en `composeResources`.

---

## Dependencias

```text
- clean-architecture-kmp
- theming-compose-kmp
```

---

## Entradas necesarias

- Lista de imágenes/fuentes/strings a añadir.
- Si el proyecto necesita más de un idioma (i18n) y cuáles.
- Formato de los ficheros de imagen disponibles (deben ser PNG/JPEG/WebP o vector XML sin referencias Android-only).

---

## Criterios arquitectónicos

- Todos los recursos compartidos entre Android/iOS/Desktop viven en `commonMain/composeResources`, nunca duplicados manualmente en cada `androidMain/res`/proyecto Xcode.
- El acceso a un recurso se hace siempre a través del objeto `Res` generado (`Res.drawable.x`, `Res.font.x`, `Res.string.x`), nunca por path/string suelto ni por una referencia a un recurso nativo de plataforma.
- Los nombres de recurso siguen la convención de Android (minúsculas, guion bajo, sin espacios ni mayúsculas), por ser el formato común a todo el sistema.
- Ningún string de interfaz se hardcodea directamente en un Composable; todo texto visible para el usuario pasa por `Res.string` + `stringResource()`.

---

## Reglas

- Debe declararse la dependencia `compose.components.resources` en `commonMain`.
- Las imágenes deben ubicarse en `composeResources/drawable`, en formato PNG/JPEG/WebP o vector XML sin referencias a recursos Android; nunca en otro directorio.
- Las fuentes deben ubicarse en `composeResources/font` y cargarse con `Font(Res.font.xxx, FontWeight.x)` dentro de un `FontFamily`; nunca mediante el antiguo patrón `expect/actual` manual por plataforma, sustituido por el sistema `Res` desde Compose Multiplatform 1.6.
- Los strings de UI deben ubicarse en `composeResources/values/strings.xml`, con el mismo formato que Android (`<string name="...">...</string>`), accedidos siempre con `stringResource(Res.string.xxx)`.
- Si el proyecto soporta varios idiomas, cada idioma adicional debe tener su propio `values-<código_idioma>/strings.xml` con exactamente las mismas claves que el `values/strings.xml` por defecto; ninguna clave puede faltar en un idioma soportado.
- Los nombres de recurso deben ser únicos, descriptivos y en `snake_case` minúsculas (ej. `ic_logo`, `dongle_regular`, `error_generic_message`).
- Tras añadir o modificar un recurso, debe sincronizarse/compilarse el proyecto para regenerar `Res` antes de referenciarlo en código.
- Los recursos verdaderamente específicos de una sola plataforma no deben forzarse dentro de `composeResources`; permanecen en su ubicación nativa.

---

## Decisiones automáticas

```text
Si el recurso (imagen, fuente, string) se usa en más de una plataforma
    → va en commonMain/composeResources

Si el recurso es exclusivo de una plataforma (icono de notificación, asset de splash nativo)
    → permanece en androidMain/res o en el proyecto Xcode, no en composeResources

Si un string es visible para el usuario en cualquier pantalla
    → va en values/strings.xml, nunca como literal en el Composable

Si el proyecto añade un nuevo idioma
    → se crea values-<código>/strings.xml replicando todas las claves existentes

Si una imagen no es PNG/JPEG/WebP ni vector XML compatible
    → debe convertirse antes de añadirse a drawable/, no se fuerza el formato original
```

---

## Proceso recomendado

1. Declarar `compose.components.resources` en `commonMain` si no está ya.
2. Ubicar el recurso en la subcarpeta correspondiente (`drawable`, `font`, `values`, `files`).
3. Sincronizar/compilar el proyecto para regenerar `Res`.
4. Referenciar el recurso desde el Composable con la función correspondiente (`painterResource`, `Font`, `stringResource`).
5. Si es un string, añadir la misma clave en todos los `values-<idioma>` ya soportados.
6. Revisar que el nombre del recurso siga la convención `snake_case`.

---

## Checklist

- [ ] El recurso está en `commonMain/composeResources`, en la subcarpeta correcta.
- [ ] El nombre del recurso está en `snake_case` minúsculas.
- [ ] Se accede vía `Res.xxx`, nunca por path/string suelto.
- [ ] Ningún string de UI está hardcodeado en un Composable.
- [ ] Si hay varios idiomas, todas las claves existen en todos los `values-<idioma>`.
- [ ] El proyecto se sincronizó/compiló tras añadir el recurso, antes de referenciarlo.

---

## Definition of Done

- Todos los recursos compartidos (imágenes, fuentes, strings) están en `composeResources` y se renderizan igual en Android, iOS y Desktop.
- No existe ningún string de UI hardcodeado fuera de `values/strings.xml`.
- La fuente Dongle (ver `theming-compose-kmp`) se carga correctamente en las tres plataformas desde `Res.font`.

---

## Riesgos

- Strings hardcodeados en Composables, dificultando una futura traducción.
- Claves de string presentes en un idioma pero ausentes en otro, provocando un fallback inconsistente.
- Imágenes en formatos no soportados (SVG sin convertir, vector XML con referencias Android) rompiendo la compilación en alguna plataforma.
- Referenciar un recurso antes de sincronizar el proyecto, generando un error de compilación confuso ("unresolved reference: Res").
- Duplicar manualmente un recurso en `androidMain/res` además de en `composeResources`, generando inconsistencia entre plataformas.

---

## Anti-patrones

- `Text("Bienvenido a citoVision")` con el string literal en lugar de `Text(stringResource(Res.string.welcome_message))`.
- `painterResource("drawable/logo.png")` accediendo por path en lugar de `Res.drawable.logo`.
- Cargar la fuente Dongle con el antiguo patrón `expect fun font(...)` manual en lugar de `Font(Res.font.dongle_regular, ...)`.
- `IcLogo.png`, `Ic Logo.png` — nombres de recurso con mayúsculas o espacios.
- Añadir `values-fr/strings.xml` con solo una parte de las claves del `values/strings.xml` por defecto.

---

## Comandos útiles

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
        }
    }
}
```

---

## Salida esperada

```text
shared/src/commonMain/composeResources/
  drawable/
    ic_logo.png
  font/
    dongle_regular.ttf
    dongle_bold.ttf
  values/
    strings.xml
  values-es/
    strings.xml
```

---

## Ejemplos

### Correcto — fuente Dongle (ver theming-compose-kmp)

```kotlin
// ui/theme/AppTypography.kt
val DongleFontFamily = FontFamily(
    Font(Res.font.dongle_regular, FontWeight.Normal),
    Font(Res.font.dongle_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = DongleFontFamily, fontSize = 40.sp),
    headlineLarge = TextStyle(fontFamily = DongleFontFamily, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = DongleFontFamily, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = DongleFontFamily, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = DongleFontFamily, fontSize = 18.sp),
    bodyMedium = TextStyle(fontFamily = DongleFontFamily, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = DongleFontFamily, fontSize = 16.sp)
)
```

### Correcto — strings con i18n

```xml
<!-- composeResources/values/strings.xml -->
<resources>
    <string name="welcome_message">Bienvenido a citoVision</string>
    <string name="error_generic_message">Ha ocurrido un error inesperado</string>
</resources>
```

```xml
<!-- composeResources/values-en/strings.xml -->
<resources>
    <string name="welcome_message">Welcome to citoVision</string>
    <string name="error_generic_message">An unexpected error occurred</string>
</resources>
```

### Correcto — uso en un Composable

```kotlin
@Composable
fun WelcomeScreen() {
    Column {
        Image(
            painter = painterResource(Res.drawable.ic_logo),
            contentDescription = null
        )
        Text(
            text = stringResource(Res.string.welcome_message),
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
```

### Incorrecto

```kotlin
// ❌ String hardcodeado
Text("Bienvenido a citoVision")

// ❌ Acceso por path suelto en lugar del objeto Res
Image(painter = painterResource("drawable/ic_logo.png"), contentDescription = null)

// ❌ Patrón antiguo expect/actual para fuentes (obsoleto desde Compose Multiplatform 1.6)
expect fun loadDongleFont(): FontFamily
```

---

## Referencias

- clean-architecture-kmp
- theming-compose-kmp
- ARCHITECTURE.md — sección `composeResources`
- Documentación oficial: https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html
