# Estructura del proyecto

```text
/MyApp
├── androidApp/                           ← Módulo Gradle Android (entry point)
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/.../androidapp/
│   │       │   ├── MainActivity.kt
│   │       │   └── MyApplication.kt
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── iosApp/                               ← Proyecto Xcode (entry point iOS)
│   ├── iosApp.xcodeproj
│   └── iosApp/
│       └── iOSApp.swift
│
├── shared/                               ← Módulo KMP donde reside la arquitectura
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/.../shared/
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── screens/
│   │   │   │   │   ├── viewmodels/
│   │   │   │   │   ├── state/
│   │   │   │   │   ├── events/
│   │   │   │   │   ├── navigation/
│   │   │   │   │   └── components/
│   │   │   │   │
│   │   │   │   ├── domain/
│   │   │   │   │   ├── entities/
│   │   │   │   │   ├── valueobjects/
│   │   │   │   │   ├── services/
│   │   │   │   │   ├── events/
│   │   │   │   │   └── errors/
│   │   │   │   │
│   │   │   │   ├── application/
│   │   │   │   │   ├── usecases/
│   │   │   │   │   ├── ports/           ← Interfaces (`expect` si el contrato lo requiere)
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── errors/
│   │   │   │   │
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── persistence/     ← Room común: AppDatabase, @Entity, @Dao
│   │   │   │   │   ├── network/         ← Cliente Ktor común (sin engine)
│   │   │   │   │   ├── mappers/
│   │   │   │   │   ├── datasources/
│   │   │   │   │   └── repositories/    ← Implementación de los ports
│   │   │   │   │
│   │   │   │   ├── composition/
│   │   │   │   │   └── di/              ← Módulos comunes de DI (Koin/Kotlin-Inject)
│   │   │   │   │
│   │   │   │   ├── core/
│   │   │   │   │   ├── utils/
│   │   │   │   │   ├── result/
│   │   │   │   │   └── extensions/
│   │   │   │   │
│   │   │   │   └── ui/
│   │   │   │       ├── theme/
│   │   │   │       └── customviews/
│   │   │   │
│   │   │   └── composeResources/        ← Strings, imágenes y fuentes multiplataforma
│   │   │
│   │   ├── androidMain/
│   │   │   └── kotlin/.../shared/
│   │   │       ├── infrastructure/
│   │   │       │   ├── persistence/     ← `actual` Room Builder Android
│   │   │       │   └── network/         ← `actual` Engine OkHttp
│   │   │       └── composition/
│   │   │           └── di/              ← Proveedores Android (Context, etc.)
│   │   │
│   │   ├── iosMain/
│   │   │   └── kotlin/.../shared/
│   │   │       ├── infrastructure/
│   │   │       │   ├── persistence/     ← `actual` Room Builder iOS
│   │   │       │   └── network/         ← `actual` Engine Darwin
│   │   │       └── composition/
│   │   │           └── di/              ← Dependencias Apple (Keychain, etc.)
│   │   │
│   │   ├── commonTest/
│   │   ├── androidUnitTest/
│   │   └── iosTest/
│   │
│   └── build.gradle.kts
│
├── Doc/
│   ├── adr/                             ← Decisiones arquitectónicas globales
│   ├── app/
│   ├── getting-started/
│   └── features/
│       ├── feature1/
│       │   └── rfc/
│       └── feature2/
│           └── rfc/
│
└── settings.gradle.kts
```

## 3. Arquitectura objetivo

Toda nueva funcionalidad deberá respetar la arquitectura definida para el proyecto KMP.

### Presentation

Responsabilidad:

```text
presentation
  screens
  viewmodels
  state
  events
  navigation
  components
```

Contiene:

- Pantallas Compose.
- ViewModels.
- UiState y UiEvent.
- Navegación.
- Componentes reutilizables.
- Adaptación del estado de la aplicación a la UI.

No contiene:

- Lógica de negocio.
- Acceso directo a APIs.
- Acceso directo a bases de datos.

---

### Domain

Responsabilidad:

```text
domain
  entities
  valueobjects
  services
  events
  errors
```

Contiene:

- Reglas de negocio puras.
- Entidades.
- Value Objects.
- Domain Services.
- Eventos y errores de dominio.

El dominio:

- No conoce Android.
- No conoce iOS.
- No conoce Ktor.
- No conoce Room.
- No conoce librerías externas.

Debe ser completamente multiplataforma.

---

### Application

Responsabilidad:

```text
application
  usecases
  ports
  dto
  errors
```

Contiene:

- Casos de uso.
- Orquestación de la lógica de negocio.
- Interfaces (Ports).
- DTOs propios de aplicación.
- Errores de aplicación.

Los casos de uso únicamente dependen del dominio y de los puertos definidos por la aplicación.

---

### Infrastructure

Responsabilidad:

```text
infrastructure
  persistence
  network
  datasources
  repositories
  mappers
```

Contiene:

- Implementaciones de los Ports.
- Clientes Ktor.
- Room (AppDatabase, `@Entity`, `@Dao`).
- DataSources.
- Repositorios concretos.
- Mappers.
- Servicios externos.

Toda dependencia tecnológica vive aquí.

Las implementaciones específicas de plataforma deberán ubicarse en:

```text
androidMain/
iosMain/
```

mediante mecanismos `expect/actual` cuando sea necesario.

---

### Composition

Responsabilidad:

```text
composition
  di
```

Contiene:

- Configuración de Dependency Injection.
- Módulos Koin.
- Factories.
- Wiring de dependencias.

No contiene lógica de negocio.

---

### Core

Responsabilidad:

```text
core
  result
  utils
  extensions
```

Contiene elementos transversales compartidos por toda la aplicación:

- Result / Either.
- Utilidades.
- Extensions.
- Dispatchers.
- Clock.
- Logging abstraído.
- Helpers comunes.

No contiene lógica de negocio.

---

### UI

Responsabilidad:

```text
ui
  theme
  customviews
```

Contiene exclusivamente:

- Tema Compose.
- Tipografía.
- Colores.
- Componentes visuales reutilizables.
- Diseño compartido.

No contiene lógica.

---

## Dependencias permitidas

```text
presentation
    ↓
application
    ↓
domain

presentation
    ↓
domain

infrastructure
    ↓
application
    ↓
domain

composition
    ↓
presentation
    ↓
application
    ↓
domain
    ↓
infrastructure

core
↑ utilizado por todas las capas cuando corresponda
```

Notas:

- `presentation` nunca accede directamente a APIs ni persistencia.
- `application` nunca conoce implementaciones concretas.
- `domain` nunca depende de ninguna otra capa.
- `infrastructure` implementa únicamente los contratos definidos en `application`.

---

## Restricciones arquitectónicas

Está prohibido:

```text
domain
    → infrastructure

domain
    → presentation

application
    → presentation

application
    → androidMain

application
    → iosMain

commonMain
    → Android SDK

commonMain
    → UIKit / SwiftUI / Foundation específicos

presentation
    → infrastructure (acceso directo)

ViewModel
    → Retrofit

ViewModel
    → Room

Composable
    → Repository

Composable
    → UseCase que no pase por el ViewModel
```

Toda dependencia específica de plataforma deberá resolverse mediante:

- `expect/actual`
- Dependency Injection
- Ports definidos en `application`

Nunca mediante referencias directas desde `commonMain`.

Toda funcionalidad nueva deberá implementarse primero en commonMain siempre que sea posible.
Solo se usará androidMain, iosMain o desktopMain cuando exista una dependencia específica de plataforma.

## Diagrama de ejemplo de uso

UI Platform Layer
(Android Compose / iOS SwiftUI / Desktop Compose)
            │
            ▼
presentation
(Screens, ViewModels, UiState, UiEvent, navigation, components)
            │
            ▼
application
(UseCases + Ports + Application DTOs)
            │
            ▼
domain
(Entities, ValueObjects, Domain Services, Domain Events, Domain Errors)
            ▲
            │
infrastructure
(RepositoryImpl + DataSources + Mappers)
            │
     ┌──────┴────────┬────────────┐
     │               │            │
androidMain      iosMain     desktopMain
(actuals)        (actuals)   (actuals)
     │               │            │
Android SDK      Apple APIs   JVM/Desktop APIs
Room             Keychain     Room (JVM)
OkHttp           Darwin       CIO/OkHttp
DataStore        Native FS    Desktop FS


### Regal relacionada al diagrama de ejemplo de uso

commonMain contiene la lógica común.
androidMain, iosMain y desktopMain solo contienen implementaciones específicas de plataforma.

Ninguna capa de commonMain puede depender directamente de:
- Android SDK
- UIKit / SwiftUI / Foundation específicos
- APIs JVM/Desktop específicas

Toda dependencia específica de plataforma debe entrar mediante:
- expect/actual
- Ports definidos en application
- Dependency Injection desde composition/di

### Ejemplo de dirección de dependencias

presentation → application → domain

infrastructure → application/domain

composition → todas las capas para construir el grafo

domain → nada del resto

Reiteración: 

Toda funcionalidad nueva deberá implementarse primero en commonMain siempre que sea posible.
Solo se usará androidMain, iosMain o desktopMain cuando exista una dependencia específica de plataforma.

