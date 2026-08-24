---
name: ktlint-kmp-code-style
description: "Reglas de formato que hay que seguir para pasar KtlintCheck al escribir código Kotlin/KMP y no generar cientos de puntos a corregir en cada PullRequest."
---

# Skill: ktlint-kmp-code-style

## Objetivo

Asegurar que cualquier código Kotlin/KMP generado, modificado o refactorizado respeta las reglas de formato exigidas por Ktlint en el proyecto.

Este Skill no define arquitectura, MVVM, Compose ni testing. Su única responsabilidad es evitar errores de estilo, formato e imports detectados por Ktlint.

---

## Modelo conceptual

```text
Código Kotlin / KMP
        │
        ▼
Formato consistente
        │
        ▼
Ktlint sin errores
        │
        ▼
Código aceptable para commit / PR
```

El agente debe escribir código que pase Ktlint desde el primer intento siempre que sea razonable.

---

## Cuándo usarlo

- Al crear código Kotlin nuevo.
- Al modificar ficheros `.kt`.
- Al refactorizar código existente.
- Antes de entregar cambios en un PR.
- Cuando aparezcan errores de Ktlint relacionados con formato, imports, nombres o saltos de línea.

## Cuándo NO usarlo

- Para decidir arquitectura.
- Para decidir patrones de diseño.
- Para definir reglas de negocio.
- Para justificar excepciones de estilo sin revisar `.editorconfig`.

---

## Dependencias

- clean-architecture-kmp
- mvvm-compose-kmp
- compose-ui-style
- testing-kmp

---

## Entradas necesarias

- Código Kotlin afectado.
- Salida de Ktlint, si existe.
- Fichero `.editorconfig`, si se necesita revisar o ajustar reglas.
- Convenciones propias del proyecto, si existen.

---

## Criterios arquitectónicos

Este Skill no decide arquitectura.

Solo puede afectar a:

- Formato.
- Imports.
- Nombres.
- Saltos de línea.
- Comas finales.
- Espaciado.
- Organización superficial del código.

No debe cambiar comportamiento funcional salvo que sea imprescindible para corregir un error no autocorregible.

---

## Reglas

### Imports

- No usar wildcard imports.
- Eliminar imports no usados.
- Ordenar imports en orden lexicográfico.
- No dejar líneas vacías innecesarias entre imports.
- Mantener `java`, `javax`, `kotlin` y aliases al final si la configuración de Ktlint lo exige.

Incorrecto:

```kotlin
import androidx.compose.*
import kotlin.collections.List
import dev.lovelace.foo.Bar
```

Correcto:

```kotlin
import dev.lovelace.foo.Bar
import androidx.compose.runtime.Composable
import kotlin.collections.List
```

---

### Comas finales

En llamadas, constructores, listas de parámetros y estructuras multilínea, añadir trailing comma cuando Ktlint lo exija.

Incorrecto:

```kotlin
TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold
)
```

Correcto:

```kotlin
TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
)
```

---

### Expresiones multilínea

Si una expresión ocupa varias líneas, debe empezar en una nueva línea cuando Ktlint lo exija.

Incorrecto:

```kotlin
val typography = Typography(bodyLarge = TextStyle(
    fontSize = 16.sp,
))
```

Correcto:

```kotlin
val typography = Typography(
    bodyLarge = TextStyle(
        fontSize = 16.sp,
    ),
)
```

---

### Parámetros multilínea

Cuando una función, constructor o llamada supere una línea, colocar cada parámetro en su propia línea si Ktlint lo exige.

Incorrecto:

```kotlin
fun AppTheme(

    content: @Composable () -> Unit

) {
}
```

Correcto:

```kotlin
fun AppTheme(
    content: @Composable () -> Unit,
) {
}
```

---

### Espaciado en paréntesis

No insertar líneas vacías ni espacios innecesarios después del paréntesis de apertura ni antes del paréntesis de cierre.

Incorrecto:

```kotlin
fun AppTheme(

    content: @Composable () -> Unit

) {
}
```

Correcto:

```kotlin
fun AppTheme(
    content: @Composable () -> Unit,
) {
}
```

---

### Cuerpos de clase

El cuerpo de una clase no debe empezar con una línea en blanco.

Incorrecto:

```kotlin
class SplashViewModel : ViewModel() {

    private val value = ""
}
```

Correcto:

```kotlin
class SplashViewModel : ViewModel() {
    private val value = ""
}
```

---

### Líneas en blanco

- No dejar líneas en blanco innecesarias.
- Separar declaraciones cuando Ktlint lo pida.
- No dejar bloques con espacios verticales arbitrarios.
- Mantener una línea en blanco entre declaraciones independientes si Ktlint lo exige.

Incorrecto:

```kotlin
sealed interface LoginUiEvent {
    data object Submit : LoginUiEvent
    data class EmailChanged(val value: String) : LoginUiEvent
}
```

Correcto:

```kotlin
sealed interface LoginUiEvent {
    data object Submit : LoginUiEvent

    data class EmailChanged(
        val value: String,
    ) : LoginUiEvent
}
```

---

### Nombres de funciones

Por defecto, las funciones Kotlin deben empezar por minúscula y usar camelCase.

Correcto:

```kotlin
fun loginUser() {
}
```

Incorrecto:

```kotlin
fun LoginUser() {
}
```

---

### Excepción para Composables

Las funciones Composable que representan UI pueden empezar por mayúscula siguiendo la convención habitual de Jetpack Compose.

Ejemplo válido:

```kotlin
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
) {
}
```

Si Ktlint marca como error una función Composable que debe empezar por mayúscula, no debe renombrarse automáticamente a minúscula si representa una pantalla o componente Compose.

En ese caso, revisar y ajustar `.editorconfig` para permitir funciones anotadas con `@Composable`.

Ejemplo orientativo:

```editorconfig
[*.{kt,kts}]
ktlint_function_naming_ignore_when_annotated_with = Composable
```

Si la clave exacta no existe en la versión de Ktlint usada por el proyecto, consultar la documentación de la versión instalada y aplicar la propiedad equivalente.

---

## Decisiones automáticas

Si aparece `Unused import`:

- Eliminar el import.

Si aparece `Wildcard import`:

- Sustituir por imports explícitos.

Si aparece `Imports must be ordered`:

- Reordenar imports lexicográficamente según Ktlint.

Si aparece `Missing trailing comma`:

- Añadir coma final.

Si aparece `A multiline expression should start on a new line`:

- Reestructurar la expresión para que empiece en línea nueva.

Si aparece `Parameter should start on a newline`:

- Pasar cada parámetro a su propia línea.

Si aparece `No whitespace expected between opening parenthesis and first parameter name`:

- Eliminar línea vacía o espacio tras `(`.

Si aparece `No whitespace expected between last parameter and closing parenthesis`:

- Eliminar línea vacía o espacio antes de `)`.

Si aparece `Class body should not start with blank line`:

- Eliminar línea en blanco inicial del cuerpo de la clase.

Si aparece `Needless blank line(s)`:

- Eliminar líneas en blanco sobrantes.

Si aparece `Expected a blank line for this declaration`:

- Añadir una línea en blanco entre declaraciones.

Si aparece `Function name should start with a lowercase letter` en una función `@Composable`:

- No renombrar automáticamente si es una pantalla, componente o pieza de UI.
- Revisar `.editorconfig`.
- Configurar una excepción para `@Composable` si procede.

---

## Proceso recomendado

1. Revisar la salida completa de Ktlint.
2. Agrupar errores por tipo.
3. Corregir primero errores autocorregibles:
   - imports
   - trailing commas
   - espacios
   - líneas en blanco
   - saltos de línea
4. Revisar después errores no autocorregibles:
   - nombres de funciones
   - wildcard imports
   - reglas que requieren criterio humano
5. Si el error afecta a nombres de Composables:
   - verificar si la función tiene `@Composable`
   - verificar si representa UI
   - no romper la convención Compose
   - ajustar `.editorconfig` si corresponde
6. Ejecutar Ktlint de nuevo.
7. Repetir hasta que no queden errores.

---

## Checklist

- [ ] No hay imports sin usar.
- [ ] No hay wildcard imports.
- [ ] Los imports están ordenados.
- [ ] Las llamadas multilínea tienen trailing comma cuando corresponde.
- [ ] Las expresiones multilínea empiezan en una nueva línea.
- [ ] Los parámetros multilínea están correctamente separados.
- [ ] No hay espacios o líneas vacías dentro de paréntesis.
- [ ] Ninguna clase empieza con línea en blanco.
- [ ] No hay líneas en blanco innecesarias.
- [ ] Las declaraciones están separadas cuando Ktlint lo exige.
- [ ] Las funciones no Composable usan camelCase empezando por minúscula.
- [ ] Las funciones Composable de UI mantienen PascalCase si corresponde.
- [ ] `.editorconfig` contempla la excepción de `@Composable` si Ktlint la necesita.

---

## Definition of Done

- El código Kotlin pasa `ktlintCheck`.
- No se han introducido cambios funcionales innecesarios.
- No se ha roto la convención de nombres de Compose.
- El `.editorconfig` refleja las excepciones necesarias del proyecto.
- El código queda preparado para commit o Pull Request.

---

## Riesgos

- Renombrar Composables a minúscula y romper convenciones de Compose.
- Corregir estilo modificando comportamiento.
- Aplicar reglas manualmente sin revisar `.editorconfig`.
- Introducir imports explícitos incorrectos al eliminar wildcard imports.
- Aceptar cientos de errores de Ktlint como “ruido” en vez de corregirlos.

---

## Anti-patrones

- Ignorar Ktlint porque “solo es formato”.
- Desactivar reglas globalmente sin justificación.
- Añadir `@Suppress` para evitar corregir formato.
- Dejar `MutableStateFlow`, `remember`, `Composable` o APIs de Compose mal importadas tras eliminar wildcard imports.
- Renombrar `LoginScreen` a `loginScreen` solo para satisfacer una regla mal configurada.
- Mezclar cambios de formato con refactors funcionales grandes.

---

## Comandos útiles

Ejecutar comprobación Ktlint:

```bash
./gradlew ktlintCheck
```

Ejecutar autocorrección si el proyecto la tiene habilitada:

```bash
./gradlew ktlintFormat
```

Ejecutar comprobación completa antes de commit:

```bash
./gradlew ktlintCheck test
```

Buscar wildcard imports:

```bash
grep -R "import .*\.\*" . --include="*.kt"
```

---

## Salida esperada

Cuando el agente aplique este Skill debe devolver:

- Resumen de tipos de errores corregidos.
- Ficheros modificados.
- Indicación de si queda algún error no autocorregible.
- Si se modifica `.editorconfig`, explicar exactamente qué regla se ha añadido o cambiado.
- Confirmación de que `ktlintCheck` pasa, si se ha podido ejecutar.

---

## Ejemplos

### Error: trailing comma

Entrada:

```kotlin
Text(
    text = title,
    style = MaterialTheme.typography.titleLarge
)
```

Salida:

```kotlin
Text(
    text = title,
    style = MaterialTheme.typography.titleLarge,
)
```

---

### Error: Composable con nombre en mayúscula

Entrada:

```kotlin
@Composable
fun LoginScreen() {
}
```

Acción correcta:

- Mantener `LoginScreen`.
- Revisar `.editorconfig`.
- Añadir excepción para `@Composable` si la versión de Ktlint lo permite.

Acción incorrecta:

```kotlin
@Composable
fun loginScreen() {
}
```

---

### Error: class body blank line

Entrada:

```kotlin
class SplashViewModel : ViewModel() {

    init {
    }
}
```

Salida:

```kotlin
class SplashViewModel : ViewModel() {
    init {
    }
}
```

---

## Referencias

- kotlin-code-style
- mvvm-compose-kmp
- compose-ui-style
- clean-architecture-kmp
