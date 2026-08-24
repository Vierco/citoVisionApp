---
name: testing-kmp
description: "Escribir o revisar los tests de cualquier capa, decidir qué mockear con Mokkery, o verificar el grafo de Koin antes de mergear."
---

# Skill: testing-kmp

## Objetivo

Definir cómo escribir, organizar y mockear los tests del proyecto Kotlin Multiplatform (Android, iOS, Desktop), aplicando las reglas ya fijadas en `TESTING.md` (framework `kotlin.test`, patrón Given/When/Then, mocking con **Mokkery**, pirámide de testing, cobertura mínima) a cada capa de `clean-architecture-kmp`.

Este Skill **no redefine** las reglas generales de `TESTING.md` (framework, pirámide, métricas de cobertura, proceso de propuesta de nuevas herramientas); las aplica y las traduce en qué y cómo testear en Domain, Application, Infrastructure, Presentation y Composition.

---

## Modelo conceptual

```text
test/
  feature/
    domain/          → Tests unitarios puros (sin mocks, sin I/O)
    application/      → Tests unitarios con mocks (Mokkery) del Port (Repository)
    infrastructure/    → Tests de integración (MockEngine de Ktor, Room en memoria/temporal)
    presentation/      → Tests del ViewModel con UseCase mockeado, observando el StateFlow
composition/
  di/
    → Test de Koin: checkModules()/verify()
```

Cada capa se testea con el doble adecuado a su posición en la pirámide de `TESTING.md` (60–70 % unitario, 20–30 % integración, 5–10 % E2E solo si se solicita expresamente).

---

## Cuándo usarlo

- Escribir los tests de una nueva clase o feature en cualquier capa.
- Decidir si un test es unitario o de integración.
- Decidir qué se mockea y qué no en cada capa.
- Revisar si una tarea de desarrollo puede considerarse terminada.
- Verificar el grafo de Koin antes de mergear.

## Cuándo NO usarlo

- Para definir el framework de testing, la pirámide o las métricas de cobertura → esas reglas viven en `TESTING.md`, no se redefinen aquí.
- Para diseñar las capas en sí → `clean-architecture-kmp`.
- Para diseñar el `UseCase`/`Repository`/`ViewModel` que se está testeando → su Skill correspondiente.
- Para tests End-to-End → solo se desarrollan si el desarrollador lo solicita expresamente (regla de `TESTING.md`); este Skill no los activa por iniciativa propia.

---

## Dependencias

```text
- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
- result-pattern
- repository-pattern
- room-multiplatform
- ktor-client
```

Documento de referencia obligatoria (no es un Skill): `TESTING.md`.

---

## Entradas necesarias

- Clase/feature a testear y la capa a la que pertenece.
- Dependencias que necesita (Ports, UseCases) para decidir si se mockean con Mokkery o se usa un Fake escrito a mano.
- Si el comportamiento a testear involucra `suspend`/`Flow` (requiere `kotlinx-coroutines-test`).
- El contrato de error ya definido (`DomainError`/`AppError`, ver `result-pattern`) para testear los casos de fallo.

---

## Criterios arquitectónicos

- La estructura de `test` replica exactamente la de `main` (regla de `TESTING.md`): cada clase tiene su test en la misma ruta relativa.
- Domain se testea sin mocks: las reglas de negocio son funciones puras, se les pasan datos y se comprueba el resultado.
- Application mockea los Ports (interfaces de Repository) con Mokkery; nunca testea contra una implementación real de Infrastructure.
- Infrastructure se testea con dobles de la API externa (`MockEngine` de Ktor) o base de datos en memoria/temporal (Room), no contra servicios reales.
- Presentation testea el ViewModel con un `UseCase` mockeado, observando los valores del `StateFlow<UiState>`; no renderiza Composables reales (eso sería un test de UI, fuera del alcance de este Skill salvo petición expresa).
- Una tarea no se considera terminada sin su batería de tests correspondiente (regla de `TESTING.md`).

---

## Reglas

- Todo test debe seguir el patrón Given/When/Then (regla de `TESTING.md`): mediante comentarios `// Given` `// When` `// Then`, y/o un nombre de función descriptivo entre backticks.
- Debe usarse `@BeforeTest`/`@AfterTest` de `kotlin.test` cuando el test necesite preparar o limpiar estado; nunca lógica de setup duplicada dentro de cada test.
- Las aserciones deben usar las funciones de `kotlin.test` (`assertEquals`, `assertTrue`, `assertFalse`, `assertFailsWith`, etc.); no debe introducirse otra librería de aserciones sin pasar por el proceso de propuesta de `TESTING.md`.
- El mocking debe hacerse con **Mokkery** (`mock<T> { }`, `every`/`everySuspend`, `verify`/`verifySuspend`); no debe usarse Mockito ni MockK en código de `commonTest` (no son multiplatform).
- Las funciones `suspend` y los `Flow` deben testearse con `kotlinx-coroutines-test` (`runTest`); nunca con `runBlocking` a secas en tests que dependan de un `TestDispatcher`.
- Domain no debe mockear nada: si una clase de Domain necesita un mock para ser testeada, probablemente tenga una dependencia que no debería tener (revisar `clean-architecture-kmp`).
- Application debe mockear únicamente los Ports (interfaces); nunca debe instanciarse una implementación real de Infrastructure en un test de Application.
- Infrastructure debe testear sus Repository/DataSource con `MockEngine` (Ktor) o una base de datos Room real en memoria/temporal; nunca contra la API o base de datos de producción.
- Presentation debe testear el ViewModel con un `UseCase` mockeado/fake; debe verificarse cada transición relevante del `UiState`, incluyendo los casos de error (`Result.Failure`).
- Antes de incorporar cualquier herramienta de testing adicional a las ya definidas (`kotlin.test`, Mokkery, `kotlinx-coroutines-test`), debe proponerse indicando qué problema resuelve, sus ventajas, su compatibilidad KMP y su impacto (regla de `TESTING.md`); no debe añadirse sin este paso.
- Los tests E2E no se desarrollan salvo petición expresa del desarrollador (regla de `TESTING.md`).

---

## Decisiones automáticas

```text
Si la clase a testear vive en Domain (Entity, Value Object, Domain Service)
    → test unitario puro, sin mocks, sin corrutinas salvo que el propio Domain las use

Si la clase a testear vive en Application (UseCase)
    → test unitario, mockeando el/los Port con Mokkery, usando runTest si el UseCase es suspend

Si la clase a testear vive en Infrastructure (Repository, DataSource)
    → test de integración: MockEngine para RemoteDataSource, Room en memoria/temporal para LocalDataSource

Si la clase a testear es un ViewModel (Presentation)
    → test unitario con el UseCase mockeado, verificando los valores del StateFlow<UiState> con runTest

Si la clase a testear es un módulo de Koin (Composition)
    → test de checkModules()/verify() (ver dependency-injection-koin), no un test unitario por dependencia

Si el comportamiento a testear depende del tiempo (delays, timeouts, retries)
    → usar un TestDispatcher de kotlinx-coroutines-test, nunca delays reales en el test
```

---

## Proceso recomendado

1. Ubicar la clase a testear y su capa.
2. Crear el fichero de test en la ruta equivalente dentro de `test/`, replicando la estructura de `main/`.
3. Escribir el test siguiendo Given/When/Then.
4. Decidir el doble de prueba según la capa (sin mock en Domain, Mokkery en Application/Presentation, `MockEngine`/Room en memoria en Infrastructure).
5. Si hay `suspend`/`Flow`, envolver el test en `runTest`.
6. Cubrir el caso de éxito y, si aplica, cada variante de `Result.Failure` (ver `result-pattern`).
7. Ejecutar la suite y revisar cobertura (`Statements ≥80%`, `Branches ≥80%`, `Functions` idealmente 100 %, según `TESTING.md`).
8. No marcar la tarea como terminada sin esta batería de tests.

---

## Checklist

- [ ] El test está en la ruta de `test/` equivalente a la de `main/`.
- [ ] El test sigue el patrón Given/When/Then.
- [ ] Se usan `@BeforeTest`/`@AfterTest` cuando aplica.
- [ ] Las aserciones son de `kotlin.test`.
- [ ] El mocking, si existe, usa Mokkery.
- [ ] Los `suspend`/`Flow` se testean con `runTest`.
- [ ] Domain no tiene mocks; Application solo mockea Ports; Infrastructure usa `MockEngine`/Room en memoria; Presentation mockea el `UseCase`.
- [ ] Se cubren el caso de éxito y los casos de `Failure` relevantes.
- [ ] No se ha añadido ninguna herramienta de testing nueva sin pasar por el proceso de propuesta de `TESTING.md`.

---

## Definition of Done

- Toda clase de negocio relevante tiene su test en la ruta equivalente de `test/`.
- Los tests son deterministas, rápidos y no dependen de red, tiempo real ni estado compartido entre tests.
- Se cubre el caso de éxito y los casos de error tipados (`DomainError`/`AppError`) de cada operación relevante.
- La cobertura del proyecto se mantiene dentro de los objetivos de `TESTING.md` (Statements ≥80 %, Branches ≥80 %, Functions 100 % cuando sea razonable).
- Ninguna tarea se considera cerrada sin su batería de tests correspondiente.

---

## Riesgos

- Tests no deterministas por depender de `delay()` real, red real o el reloj del sistema.
- Mockear Infrastructure en tests de Application (en lugar del Port), acoplando el test a una implementación concreta.
- Usar `runBlocking` en lugar de `runTest`, perdiendo el control del `TestDispatcher` y ralentizando la suite.
- Renderizar Composables reales en un test de ViewModel cuando bastaba con observar el `StateFlow`.
- Cobertura alta pero tests triviales (solo comprueban que no lanza excepción) que no detectan regresiones reales.

---

## Anti-patrones

- Usar Mockito o MockK en código de `commonTest` (no son multiplatform).
- Test de Application que instancia un `RepositoryImpl` real en lugar de mockear el Port.
- Test de Infrastructure que llama a la API real en lugar de usar `MockEngine`.
- Carpeta de test que no replica la estructura de `main` (clases de test "sueltas" fuera de su ruta equivalente).
- Añadir una librería de testing nueva (ej. Turbine, aserciones de Kotest) directamente al código sin pasar por el proceso de propuesta de `TESTING.md`.
- Tests E2E añadidos sin que el desarrollador los haya pedido expresamente.

---

## Comandos útiles

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("dev.mokkery") version "<version-compatible-con-kotlin>"
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<version>")
            implementation("dev.mokkery:mokkery-coroutines:<version>")
            implementation("io.ktor:ktor-client-mock:<version>") // MockEngine
        }
    }
}
```

```bash
# Ejecutar todos los tests multiplatform
./gradlew allTests

# Ejecutar solo los tests de commonTest en JVM
./gradlew jvmTest

# Ejecutar los tests de Android
./gradlew testDebugUnitTest
```

---

## Salida esperada

```text
test/
  feature/
    domain/
      ItemTest.kt
    application/
      GetItemsUseCaseTest.kt
    infrastructure/
      ItemRepositoryImplTest.kt
      KtorItemRemoteDataSourceTest.kt
    presentation/
      ItemViewModelTest.kt
composition/
  di/
    KoinGraphTest.kt
```

---

## Ejemplos

### Correcto — Domain (sin mocks)

```kotlin
class ItemTest {

    @Test
    fun `given a negative price when creating an Item then it throws`() {
        // Given
        val invalidPrice = -1.0

        // When / Then
        assertFailsWith<IllegalArgumentException> {
            Item(id = "1", title = "Book", price = invalidPrice)
        }
    }
}
```

### Correcto — Application (Mokkery, mock del Port)

```kotlin
class GetUserUseCaseTest {

    private val repository = mock<UserRepository>()
    private val useCase = GetUserUseCase(repository)

    @Test
    fun `given a valid id when invoking the use case then it returns the user`() = runTest {
        // Given
        val expected = User(id = "1", name = "Ada")
        everySuspend { repository.getUser("1") } returns Result.Success(expected)

        // When
        val result = useCase("1")

        // Then
        assertEquals(Result.Success(expected), result)
        verifySuspend { repository.getUser("1") }
    }

    @Test
    fun `given a blank id when invoking the use case then it returns InvalidInput`() = runTest {
        // Given / When
        val result = useCase("")

        // Then
        assertEquals(Result.Failure(AppError.InvalidInput), result)
    }
}
```

### Correcto — Infrastructure (MockEngine de Ktor)

```kotlin
class KtorItemRemoteDataSourceTest {

    @Test
    fun `given a successful response when fetching items then it returns the parsed list`() = runTest {
        // Given
        val engine = MockEngine { request ->
            respond(
                content = """[{"id":"1","title":"Book"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = createHttpClient(engine)
        val dataSource = KtorItemRemoteDataSource(client)

        // When
        val result = dataSource.fetchItems()

        // Then
        assertEquals(1, result.size)
        assertEquals("Book", result.first().title)
    }
}
```

### Correcto — Presentation (ViewModel con UseCase mockeado)

```kotlin
class LoginViewModelTest {

    private val loginUseCase = mock<LoginUseCase>()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        viewModel = LoginViewModel(loginUseCase)
    }

    @Test
    fun `given valid credentials when submitting then uiState reflects success`() = runTest {
        // Given
        everySuspend { loginUseCase("a@a.com", "1234") } returns Result.Success(Unit)

        // When
        viewModel.onEvent(LoginUiEvent.EmailChanged("a@a.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("1234"))
        viewModel.onEvent(LoginUiEvent.Submit)

        // Then
        assertEquals(true, viewModel.uiState.value.isLoginSuccessful)
    }
}
```

### Incorrecto

```kotlin
// ❌ Mockito en commonTest — no es multiplatform
import org.mockito.Mockito.mock

// ❌ Application testeado contra la implementación real de Infrastructure
val useCase = GetUserUseCase(UserRepositoryImpl(realApi, realDb)) // debería mockear el Port

// ❌ runBlocking en lugar de runTest — pierde el control del TestDispatcher
@Test
fun test() = runBlocking {
    // ...
}

// ❌ Test sin Given/When/Then ni nombre descriptivo
@Test
fun test1() {
    val r = useCase("1")
    assertTrue(r is Result.Success)
}
```

---

## Referencias

- TESTING.md — framework, pirámide de testing, cobertura, organización de carpetas
- clean-architecture-kmp
- mvvm-compose-kmp
- dependency-injection-koin
- result-pattern
- repository-pattern
- room-multiplatform
- ktor-client
