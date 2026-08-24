---
name: clean-architecture-kmp
description: "Diseñar la arquitectura de una nueva feature, decidir en qué capa (Presentation/Application/Domain/Infrastructure/Composition) vive una clase, o revisar si una PR respeta los límites entre capas."
---

# Skill: clean-architecture-kmp

## Objetivo

Definir las reglas de Clean Architecture que cualquier agente de IA debe seguir al crear, modificar o revisar un proyecto Kotlin Multiplatform.

Este Skill enseña únicamente Clean Architecture. Otros conceptos (MVVM, Repository Pattern, Result Pattern, DI, etc.) pertenecen a Skills independientes.

---

## Modelo conceptual

```text
                   PRESENTATION
      (Compose, UiState, UiEvent, ViewModel)
                         │
                         ▼
                   APPLICATION
      (Use Cases, Ports, Orquestación)
                         │
                         ▼
                      DOMAIN
      (Entities, Value Objects, Interfaces)
                         ▲
                         │
                  INFRASTRUCTURE
 (Repository Impl, Ktor, Room, DataStore, APIs...)
```

La capa **Application** representa la capa **Use Cases** de la Clean Architecture clásica.

---

## Cuándo usarlo

- Diseñar una nueva arquitectura.
- Añadir nuevas features.
- Refactorizar.
- Revisar Pull Requests.
- Decidir dónde pertenece una clase.

## Cuándo NO usarlo

- Para definir MVVM.
- Para explicar Dependency Injection.
- Para definir Repository Pattern.
- Para explicar Testing.

---

## Dependencias

- mvvm-compose-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
- testing-kmp

---

## Regla fundamental

Cada elemento del proyecto debe pertenecer a **una única capa**.

Si una clase parece pertenecer a varias capas, probablemente deba dividirse.

---

## Responsabilidades por capa

### Presentation

**Responsabilidad**

- Mostrar información.
- Gestionar el estado de la UI.
- Transformar acciones del usuario en eventos.

**Puede depender de**

- Application

**Nunca depende de**

- Infrastructure

---

### Application

**Responsabilidad**

- Orquestar casos de uso.
- Coordinar repositorios.
- Aplicar reglas de aplicación.

**Puede depender de**

- Domain

**Nunca depende de**

- Presentation
- Infrastructure (implementaciones)

---

### Domain

**Responsabilidad**

- Reglas de negocio.
- Entidades.
- Value Objects.
- Interfaces (Ports).

No depende de ninguna otra capa.

---

### Infrastructure

**Responsabilidad**

- Implementar repositorios.
- Acceso a red.
- Base de datos.
- Preferencias.
- APIs externas.

**Puede depender de**

- Domain

Nunca al revés.

---

### Composition

Responsabilidad

- Componer la aplicación.

- Resolver las dependencias entre capas.

- Conectar interfaces con implementaciones.

- Inicializar la aplicación.

Puede depender de

- Presentation

- Application

- Domain

- Infrastructure

Nunca contiene

- Reglas de negocio.

- Casos de uso.

- Lógica de UI.

- Implementaciones de negocio.

La capa Composition es la única autorizada para conocer simultáneamente todas las capas del sistema.

Su única responsabilidad es ensamblar la aplicación.

---

## Regla de dependencias

Las dependencias siempre apuntan hacia el centro.

Una capa nunca podrá importar implementaciones concretas de una capa exterior.

La comunicación entre capas se realiza mediante interfaces definidas en capas interiores.

---

## Ubicación de cada elemento

| Elemento | Capa |
|----------|------|
| Entity | Domain |
| Value Object | Domain |
| Repository Interface | Domain |
| Use Case | Application |
| Repository Implementation | Infrastructure |
| Remote DataSource | Infrastructure |
| Local DataSource | Infrastructure |
| DTO | Infrastructure |
| Mapper DTO ↔ Entity | Infrastructure |
| ViewModel | Presentation |
| UiState | Presentation |
| UiEvent | Presentation |
| Composable | Presentation |
| Navigation | Presentation |
| Koin Modules | Composition |

---

## Heurísticas

- Usa Retrofit → Infrastructure.
- Usa Ktor Client → Infrastructure.
- Usa SQLDelight/Room/DataStore → Infrastructure.
- Contiene `@Composable` → Presentation.
- Es un ViewModel → Presentation.
- Contiene reglas de negocio → Domain.
- Coordina varios repositorios → Application.
- Convierte DTO ↔ Entity → Infrastructure.
- Puede ejecutarse sin Android, iOS, red o base de datos → probablemente Domain.

---

## Árbol de decisión

```text
¿Representa una regla de negocio?
        │
        ├── Sí → Domain
        │
        └── No
             │
             ¿Orquesta un caso de uso?
                     │
                     ├── Sí → Application
                     │
                     └── No
                          │
                          ¿Accede a recursos externos?
                                   │
                                   ├── Sí → Infrastructure
                                   │
                                   └── No → Presentation
```

---

## Invariantes

- Ningún DTO abandona Infrastructure.
- Ningún Repository Implementation abandona Infrastructure.
- Ningún ViewModel conoce Retrofit.
- Ningún ViewModel conoce SQLDelight.
- Ninguna Entity conoce Compose.
- Ningún Use Case conoce Android.
- Ningún Use Case conoce Ktor.
- Ningún Mapper depende de Presentation.

---

## Checklist

- [ ] Todas las clases pertenecen a una única capa.
- [ ] Las dependencias apuntan hacia el centro.
- [ ] No existen imports ilegales.
- [ ] Las interfaces están en Domain.
- [ ] Las implementaciones están en Infrastructure.
- [ ] Presentation solo orquesta la UI.
- [ ] Domain permanece puro.

---

## Definition of Done

- La arquitectura respeta las dependencias definidas.
- Cada responsabilidad está en su capa.
- No existen violaciones arquitectónicas.
- Las reglas de negocio permanecen independientes de frameworks.

---

## Riesgos

- God ViewModels.
- Repositorios gigantes.
- Mezcla de responsabilidades.
- Acoplamiento a frameworks.
- Duplicación de lógica de negocio.

---

## Anti-patrones

- ViewModel llamando Retrofit.
- Compose accediendo a base de datos.
- DTOs fuera de Infrastructure.
- Reglas de negocio en Repository.
- Casos de uso con dependencias Android.
- Entities con anotaciones de persistencia.

---

## Ejemplos

### Correcto

```
Presentation
    ↓
Application
    ↓
Domain
    ↑
Infrastructure
```

### Incorrecto

```
Compose
    ↓
Retrofit
    ↓
DTO
```

---

## Referencias

- mvvm-compose-kmp
- repository-pattern
- result-pattern
- dependency-injection-koin
- testing-kmp
