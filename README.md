<p align="center">
  <img src="external/citoVision.png" alt="citoVision" width="300">
</p>

<h1 align="center">🔬 citoVision</h1>

<p align="center">
  <em>Cribado morfológico hematológico asistido por IA: detecta, segmenta y clasifica células
  sanguíneas para <strong>priorizar</strong> la revisión del profesional, nunca para sustituirla.</em>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose%20Multiplatform-1.10-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Plataformas" src="https://img.shields.io/badge/Plataformas-Android%20%7C%20macOS-3DDC84?logo=android&logoColor=white">
  <img alt="IA" src="https://img.shields.io/badge/IA-YOLO11s--seg%20%2B%20ONNX%20Runtime-00A3E0">
  <img alt="Licencia" src="https://img.shields.io/badge/Licencia-Propietaria-lightgrey">
  <img alt="Estado" src="https://img.shields.io/badge/Estado-MVP%201.0--beta-orange">
</p>

---

## Tabla de contenidos

- [¿Qué es citoVision?](#qué-es-citovision)
- [Qué necesidad cubre](#qué-necesidad-cubre)
- [Capturas](#capturas)
- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [El modelo de IA](#el-modelo-de-ia)
- [Datos: local y remoto](#datos-local-y-remoto)
- [Seguridad y privacidad](#seguridad-y-privacidad)
- [Flujos](#flujos)
- [Cómo compilar](#cómo-compilar)
- [Testing](#testing)
- [Desarrollo dirigido por especificaciones](#desarrollo-dirigido-por-especificaciones)
- [Estado del proyecto](#estado-del-proyecto)
- [Licencia y atribución](#licencia-y-atribución)

---

## ¿Qué es citoVision?

**citoVision** es una aplicación multiplataforma (Android y macOS) que analiza imágenes microscópicas de
frotis sanguíneo y, sobre cada muestra, **detecta y clasifica las células**, las **cuenta por tipo** y
calcula una **prioridad de revisión** según los hallazgos morfológicos encontrados. La inferencia se
ejecuta **en el propio dispositivo** (on-device), sin enviar la imagen a ningún servidor para analizarla.

Es el Trabajo Fin de Máster que desarrolla el **primer módulo** de una plataforma modular de análisis
microscópico asistido por IA. Está concebido como un **MVP** (producto mínimo viable) con un alcance
realista y técnicamente sólido.

> ⚠️ **citoVision no diagnostica.** Es un prototipo académico y experimental, no está clínicamente validado
> y no es un producto sanitario. Su función es **priorizar** qué muestras conviene revisar antes; la
> decisión clínica corresponde siempre al profesional.

## Qué necesidad cubre

La revisión morfológica de un frotis al microscopio es **manual, lenta y dependiente de la experiencia**
del observador. Cuando el volumen de muestras es alto, los casos que requieren atención urgente (por
ejemplo, la presencia de células inmaduras) pueden quedar en una cola indiferenciada.

citoVision aborda ese cuello de botella **ordenando la cola**: marca automáticamente las muestras con
hallazgos morfológicamente relevantes para que el profesional **empiece por lo importante**. No reemplaza
el criterio experto; reduce el tiempo hasta que ese criterio se aplica donde más importa.

## Capturas

> _Sustituir los marcadores por capturas reales de la aplicación._

<p align="center">
  <img src="external/screenshots/login.png" alt="Login" width="220" hspace="6">
  <img src="external/screenshots/analisis.png" alt="Análisis" width="220" hspace="6">
  <img src="external/screenshots/resultado.png" alt="Resultado del análisis" width="220" hspace="6">
</p>
<p align="center">
  <img src="external/screenshots/historial.png" alt="Historial" width="220" hspace="6">
  <img src="external/screenshots/pacientes.png" alt="Pacientes" width="220" hspace="6">
  <img src="external/screenshots/ajustes.png" alt="Ajustes" width="220" hspace="6">
</p>

## Funcionalidades

- **Splash** e identidad visual propia.
- **Acceso**: correo y contraseña, Google Sign-In (Android) y **modo invitado** local. Las cuentas **no se
  crean desde la aplicación**: las genera **Lovelaced**, desarrolladora de citoVision, a petición.
- **Navegación inferior** con tres pestañas: Análisis, Historial y Pacientes.
- **Análisis de una muestra**: selección de imagen, **inferencia on-device**, conteo y clasificación por
  tipo celular, y **badge de prioridad** (baja / media / alta) con aviso no diagnóstico.
- **Confianza por célula**: en el detalle, la confianza del modelo para cada célula detectada.
- **Historial local** de análisis con visor de imagen a pantalla completa.
- **Consulta por paciente**: listado filtrable de los códigos de paciente del usuario y sus análisis
  almacenados en la nube.
- **Ajustes**: identidad de la cuenta, tema claro/oscuro/sistema, envío de feedback, borrado de datos
  locales, información de licencia y atribuciones.

> 👤 **Acceso con cuenta.** Por seguridad, el registro está cerrado: los usuarios **no pueden crearse desde
> la aplicación**. Es **Lovelaced**, desarrolladora de citoVision, quien genera las cuentas a petición. Sin
> cuenta puedes usar la app en **modo invitado** (análisis e historial locales). Si te interesa probar
> citoVision con cuenta de usuario, escríbenos a **[correo@pendiente](mailto:correo@pendiente)**
> _(sustituir por el correo definitivo)._

## Arquitectura

Proyecto **Kotlin Multiplatform (KMP)** con la lógica compartida en el módulo `shared/`, siguiendo
**Clean Architecture + MVVM**. La lógica común vive primero en `commonMain`; solo se recurre a código
específico de plataforma cuando hay una dependencia real (SDK de Android, APIs de escritorio, motor de
inferencia nativo…).

```
shared/
├── presentation   → UI (Compose Multiplatform), ViewModels, estado y eventos
├── application    → casos de uso y puertos (contratos)
├── domain         → entidades, reglas de negocio, validaciones
├── infrastructure → implementaciones: red (Ktor), persistencia (Room), Firebase REST, inferencia (ONNX)
├── composition    → inyección de dependencias (Koin)
├── core / ui      → utilidades transversales y sistema de tema
```

| Plataforma | Estado en el MVP |
|------------|------------------|
| Android    | ✅ Soportada (APK/AAB firmados) |
| macOS (Desktop / JVM) | ✅ Soportada (DMG) |
| iOS        | ⏸️ Fuera del MVP |
| Windows / Linux | ⏸️ Fuera del MVP |

## Tecnologías

| Ámbito | Stack |
|--------|-------|
| Lenguaje / UI | Kotlin · Kotlin Multiplatform · Compose Multiplatform 1.10 · Material 3 |
| Inyección de dependencias | Koin 4.0 |
| Red | Ktor Client 3.0 · kotlinx.serialization |
| Persistencia local | Room 2.8 (Multiplatform) · DataStore |
| Imágenes | Coil 3 |
| Logging | Napier |
| IA (inferencia) | ONNX Runtime 1.22 (on-device, Android + Desktop) |
| IA (entrenamiento) | Python · Ultralytics YOLO11 · Transfer Learning (fine-tuning) |
| Backend | Firebase Authentication · Cloud Firestore · Firebase Storage (vía API REST) |
| Cobertura de tests | Kover |

<p align="center">
  <img src="external/img_002149.png" alt="Muestra 1" width="250" hspace="10">
  <img src="external/img_001304.png" alt="Muestra 2" width="250" hspace="10">
  <img src="external/img_001701.png" alt="Muestra 3" width="250" hspace="10">
</p>

## El modelo de IA

citoVision **no entrena desde cero**: parte de **YOLO11s-seg** y lo especializa mediante **Transfer
Learning (fine-tuning)** sobre el **UNIVALI Leukocyte Dataset**, reorganizado con un **reparto
estratificado** por clase. En la aplicación se ejecuta exportado a **ONNX** y corriendo **on-device** con
ONNX Runtime; se usa la rama de detección del modelo (las máscaras de segmentación no se explotan en el MVP).

El modelo reconoce **14 clases** (12 tipos celulares + 2 no celulares). Cada tipo aporta un **peso de
relevancia morfológica**: cuanto mayor es la presencia de células inmaduras o atípicas, mayor es la
prioridad de revisión asignada a la muestra.

| Clase | ¿Célula? | Relevancia |
|-------|:-------:|:----------:|
| Blasto | ✅ | ●●●●● |
| Promielocito | ✅ | ●●●● |
| Mielocito | ✅ | ●●● |
| Metamielocito | ✅ | ●●● |
| Linfocito atípico | ✅ | ●● |
| Basófilo | ✅ | ●● |
| Eritroblasto | ✅ | ●● |
| Neutrófilo en banda (cayado) | ✅ | ● |
| Linfocito | ✅ | — |
| Monocito | ✅ | — |
| Eosinófilo | ✅ | — |
| Neutrófilo segmentado | ✅ | — |
| Artefacto | ❌ | — |
| Restos celulares | ❌ | — |

Las cinco clases más críticas (blasto, promielocito, mielocito, metamielocito y linfocito atípico) se
evalúan con un **umbral de confianza rebajado** para no perder hallazgos débiles, que se presentan de forma
diferenciada y con un efecto **acotado** sobre la prioridad (nunca elevan por sí solos una muestra a
prioridad alta).

> 📄 **Informe de entrenamiento del modelo:** [entrenamiento del modelo](docs/entrenamiento-modelo.md)
> _(sustituir por el enlace definitivo al informe)._

## Datos: local y remoto

citoVision maneja **dos almacenes independientes**, por diseño:

- **Muestras locales** (Room): cada análisis realizado se guarda en el dispositivo (imagen, conteo,
  prioridad, fecha) y se muestra en el **Historial**. Funciona también en **modo invitado**, sin cuenta.
- **Análisis por paciente** (Firestore + Storage): con la sesión de una cuenta iniciada, los análisis se
  **sincronizan a la nube** asociados a un **código de paciente** seudonimizado. La pestaña **Pacientes**
  consulta ese almacén remoto, acotado a los datos del propio usuario.

Ambos son independientes: borrar una muestra del Historial local no afecta al análisis remoto, y viceversa.
La sincronización a la nube es **asíncrona y duradera** (patrón *outbox*): la muestra local se guarda al
instante y el envío remoto se reintenta hasta completarse.

## Seguridad y privacidad

- **Autorización en el servidor.** Las reglas de Firestore y Storage exigen sesión autenticada y **acotan
  cada dato a su propietario** (`ownerUid`); no es una restricción solo de interfaz.
- **ID token en cada petición.** Las llamadas REST a Firebase viajan con el token del usuario; el cliente
  sin credenciales queda reservado al inicio de sesión.
- **Inferencia on-device.** La imagen se analiza en el dispositivo; no se envía a un servicio externo para
  su análisis.
- **Sin secretos en el repositorio.** Las claves de configuración se aportan por build (`local.properties`
  / variables de entorno), fuera del control de versiones.

## Flujos

**Análisis de una muestra**

```
Imagen microscópica → Detección → Conteo → Clasificación → Prioridad → Resultado → Guardado (local + nube)
```

**Navegación**

```
Splash → Login → Pantalla principal
                 ├── Análisis
                 ├── Historial
                 └── Pacientes
```

## Cómo compilar

**Requisitos**

- JDK 17
- Android Studio (versión reciente) o el SDK de Android con `compileSdk 36`
- Para el empaquetado de macOS, un JDK con `jpackage` disponible

**Configuración local**

Crea un fichero `local.properties` en la raíz (no versionado) con, al menos:

```properties
sdk.dir=/ruta/al/Android/sdk
firebaseWebApiKey=TU_WEB_API_KEY   # Web API key de Firebase (pública, sin restricción de aplicación)
```

Para el login con Google en Android se necesita además el `google-services.json` del proyecto Firebase y,
para las builds de release firmadas, un `keystore.properties` propio (ambos fuera del repositorio).

**Comandos habituales**

```bash
# Comprobaciones y tests de la lógica compartida
./gradlew ktlintCheck :shared:allTests

# Android (debug)
./gradlew :androidApp:assembleDebug

# Desktop (ejecutar en local)
./gradlew :desktopApp:run

# Desktop (empaquetar DMG de macOS)
./gradlew :desktopApp:packageDmg
```

> Requisitos mínimos de ejecución: **Android 8.1 (API 27)** o superior · **macOS** (paquete DMG).

## Testing

El proyecto sigue una pirámide de test con el grueso en **tests unitarios** de dominio, casos de uso y
ViewModels, más tests de integración de repositorios y fuentes de datos (con `MockEngine` de Ktor y dobles
de persistencia). La cobertura se mide con **Kover**:

```bash
./gradlew :shared:koverLog          # resumen por consola
./gradlew :shared:koverHtmlReport   # informe navegable en shared/build/reports/kover/html/
```

La cobertura se valora **por capas**: la lógica de negocio y de presentación (dominio, casos de uso,
ViewModels) está ampliamente cubierta, mientras que la UI declarativa (Compose) no se cubre con tests
unitarios, como es habitual en este tipo de proyectos.

## Desarrollo dirigido por especificaciones

citoVision se ha construido con un enfoque **spec-driven**: cada funcionalidad partió de una
**especificación aprobada** antes de escribir código, y las decisiones técnicas relevantes quedaron
registradas como **ADR** (Architecture Decision Records) y **RFC**. Esta documentación vive en el propio
repositorio y es la fuente de verdad del proyecto — este README se ha redactado a partir de ella:

- `docs/specs/` — especificaciones funcionales (autenticación, imagen, historial, base remota, inferencia…)
- `docs/adr/` — decisiones de arquitectura (Firestore REST, auth Desktop, inferencia ONNX…)
- `docs/rfc/` — propuestas técnicas
- `AGENTS.md`, `ARCHITECTURE.md`, `DESIGN.md`, `RULES.md`, `TESTING.md`, `SECURITY_MOBILE.md` — normas
  transversales del proyecto

## Estado del proyecto

- [x] Definición del proyecto y del MVP
- [x] Arquitectura y configuración KMP
- [x] Autenticación (Firebase / Identity Toolkit REST)
- [x] Análisis de imagen e **inferencia on-device (ONNX)**
- [x] Modelo entrenado y validado para el MVP
- [x] Historial local (Room) y análisis por paciente (Firestore + Storage)
- [x] Reglas de seguridad cerradas (autorización en servidor)
- [x] Tema claro/oscuro y pulido de UI
- [x] Entregables firmados: **APK + AAB** (Android) y **DMG** (macOS)
- [x] Cobertura de tests con Kover
- [ ] Soporte de iOS
- [ ] Documentación final del TFM

## Licencia y atribución

**Desarrollado por [Lovelaced](https://github.com/Vierco/citoVision)** — Sergio Álvarez.

**citoVision** — Copyright © 2026 Sergio Álvarez. Todos los derechos reservados.

Software **propietario**. Es un prototipo académico y experimental: no está clínicamente validado, no es un
producto sanitario y no debe utilizarse para emitir diagnósticos ni sustituir el criterio de profesionales
sanitarios. Para cualquier uso, reutilización o modificación del código, contacta con el autor.

**Atribución del dataset** — citoVision utiliza el **UNIVALI Leukocyte Dataset**, disponible en
[Zenodo](https://zenodo.org/records/17743609) bajo licencia **Creative Commons Attribution 4.0 International
(CC BY 4.0)**. El conjunto de datos se reorganizó mediante un reparto estratificado y se empleó para ajustar
un modelo de segmentación. Los autores del dataset no respaldan ni certifican citoVision.
