<p align="center">
  <img src="external/citoVision.png" alt="citoVision" width="300">
</p>

<h1 align="center">citoVision</h1>

<p align="center">
  <em>Cribado morfológico hematológico asistido por IA: detecta, segmenta y clasifica células
  sanguíneas para <strong>priorizar</strong> la revisión del profesional.</em>
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose%20Multiplatform-1.10-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Plataformas" src="https://img.shields.io/badge/Plataformas-Android%20%7C%20macOS-3DDC84?logo=android&logoColor=white">
  <img alt="IA" src="https://img.shields.io/badge/IA-Detecci%C3%B3n%20celular%20on--device-00A3E0">
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
- [Probar citoVision](#probar-citovision)
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

**citoVision** es una aplicación multiplataforma (Android, macOS, iOS y Windows) que analiza imágenes microscópicas de
frotis sanguíneo y, sobre cada muestra, **detecta y clasifica las células**, las **cuenta por tipo** y
calcula una **prioridad de revisión** según los hallazgos morfológicos encontrados. La inferencia se
ejecuta **en el propio dispositivo** (on-device), sin enviar la imagen a ningún servidor para analizarla.

Es el Trabajo Fin de Máster que desarrolla el **primer módulo** de una plataforma modular de análisis
microscópico asistido por IA. Está concebido como un **MVP** (producto mínimo viable) con un alcance
realista y técnicamente sólido.

> ⚠️ **citoVision no diagnostica.** Es un prototipo académico y experimental, no está clínicamente validado
> y no es un producto sanitario. Su función es **priorizar** qué muestras conviene revisar antes; la
> decisión clínica corresponde siempre al profesional.

> 🔒 **Este repositorio no incluye el modelo de IA.** Se publica para mostrar el diseño y la implementación
> de la aplicación; los pesos del modelo y su proceso de entrenamiento no forman parte de él. Por eso el
> proyecto **compila pero no analiza**: la aplicación funcional se facilita [bajo solicitud](#probar-citovision).

## Qué necesidad cubre

La revisión morfológica de un frotis al microscopio es **manual, lenta y dependiente de la experiencia**
del observador. Cuando el volumen de muestras es alto, los casos que requieren atención urgente (por
ejemplo, la presencia de células inmaduras) pueden quedar en una cola indiferenciada.

citoVision aborda ese cuello de botella **ordenando la cola**: marca automáticamente las muestras con
hallazgos morfológicamente relevantes para que el profesional **empiece por lo importante**. No reemplaza
el criterio experto; reduce el tiempo hasta que ese criterio se aplica donde más importa.

## Capturas


<p align="center">
  <img src="external/login2.png" alt="Login" width="220" hspace="6">
  <img src="external/seleccion2.png" alt="Análisis" width="220" hspace="6">
  <img src="external/muestra2.png" alt="Resultado del análisis" width="220" hspace="6">
</p>
<p align="center">
  <img src="external/historial2.png" alt="Historial" width="220" hspace="6">
  <img src="external/pacientes2.png" alt="Pacientes" width="220" hspace="6">
  <img src="external/settings2.png" alt="Ajustes" width="220" hspace="6">
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
> citoVision con cuenta de usuario, escríbenos a **[hola@citovision.app](mailto:hola@citovision.app)**


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
| Windows (Desktop / JVM) | ✅ Soportada (ZIP ejecutable) |
| iOS        | ✅ Soportada (TestFlight) |
| Linux      | ⏸️ Fuera del MVP |

## Tecnologías

| Ámbito | Stack |
|--------|-------|
| Lenguaje / UI | Kotlin · Kotlin Multiplatform · Compose Multiplatform 1.10 · Material 3 |
| Inyección de dependencias | Koin 4.0 |
| Red | Ktor Client 3.0 · kotlinx.serialization |
| Persistencia local | Room 2.8 (Multiplatform) · DataStore |
| Imágenes | Coil 3 |
| Logging | Napier |
| IA (inferencia) | ONNX Runtime 1.22 (on-device, Android + iOS + Desktop) |
| Backend | Firebase Authentication · Cloud Firestore · Firebase Storage (vía API REST) |
| Cobertura de tests | Kover |

<p align="center">
  <img src="external/img_002149.jpg" alt="Muestra 1" width="250" hspace="10">
  <img src="external/img_001304.jpg" alt="Muestra 2" width="250" hspace="10">
  <img src="external/img_011642.jpg" alt="Muestra 3" width="250" hspace="10">
</p>



## Probar citoVision

citoVision se distribuye **bajo solicitud**. Los ejecutables llevan el modelo de IA dentro, así que no hay
descargas abiertas: escribe a **[hola@citovision.app](mailto:hola@citovision.app)** indicando la plataforma y
recibirás el paquete correspondiente junto con las instrucciones de instalación y un conjunto de imágenes de
frotis para probarlo. La versión actual es **1.0.0-beta**.

| Plataforma | Formato | Requisito mínimo |
|---|---|---|
| **Android** | APK | Android 8.1 (API 27) |
| **macOS** | DMG | macOS (Apple Silicon o Intel) |
| **Windows** | ZIP ejecutable (no se instala) | Windows 10 o superior (64 bits) |
| **iOS** | Invitación a **TestFlight** | iPhone con iOS 18.6 |

> ⚠️ **Android, macOS y Windows muestran un aviso de seguridad la primera vez.** Es lo habitual en software
> distribuido fuera de la App Store y de Google Play, y no indica ningún problema con la aplicación. Las
> [instrucciones de instalación](docs/instrucciones-entrega.md) explican cómo abrirla con normalidad en cada
> sistema.

> ℹ️ **Por qué iOS se reparte de otra forma.** Android, macOS y Windows permiten instalar software fuera de
> su tienda; iOS no: un iPhone solo ejecuta aplicaciones firmadas por un perfil que lo autorice, así que no
> existe un equivalente al APK suelto. La versión de iOS se distribuye por **TestFlight**, el canal de betas
> de Apple, mediante invitación al correo con el que se solicita. Ten en cuenta dos límites propios del canal:
> **las builds caducan a los 90 días** de publicarse, y el grupo de pruebas tiene un aforo fijado. El
> razonamiento completo y las alternativas descartadas están en
> [ADR-0010](docs/adr/0010-distribucion-ios-testflight.md); el de Windows, en
> [ADR-0011](docs/adr/0011-soporte-windows-desktop.md).

## El modelo de IA

citoVision incorpora un **modelo propio de detección y clasificación celular**, entrenado sobre el
**UNIVALI Leukocyte Dataset** y ejecutado **en el propio dispositivo**: la imagen no sale de él para ser
analizada.

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

> 🔒 **Ni el modelo ni su proceso de entrenamiento forman parte de este repositorio.** Sí está la
> integración del motor de inferencia en las cuatro plataformas y la lógica de priorización
> ([ADR-0003](docs/adr/0003-inferencia-on-device-onnx-runtime.md),
> [ADR-0007](docs/adr/0007-inferencia-onnx-ios-spm-swift.md),
> [SPEC-0006](docs/specs/0006-analisis-celular-modelo-onnx.md)).


## Datos: local y remoto

citoVision maneja **dos almacenes independientes**, por diseño:

- **Muestras locales**: cada análisis realizado se guarda en el dispositivo (imagen, conteo,
  prioridad, fecha) y se muestra en el **Historial**. Funciona también en **modo invitado**, sin cuenta.
- **Análisis por paciente**: con la sesión de una cuenta iniciada, los análisis se
  **sincronizan a la nube** asociados a un **código de paciente** seudonimizado. La pestaña **Pacientes**
  consulta ese almacén remoto, acotado a los datos del propio usuario.

Ambos son independientes: borrar una muestra del Historial local no afecta al análisis remoto, y viceversa.
La sincronización a la nube es **asíncrona y duradera**: la muestra local se guarda al instante y el envío
remoto se reintenta hasta completarse, aunque se pierda la conexión.

## Seguridad y privacidad

- **Autorización en el servidor.** El acceso a los datos exige sesión autenticada y **cada dato queda
  acotado a su propietario**; no es una restricción solo de interfaz.
- **Sesión verificada en cada petición.** Toda llamada al servidor viaja identificada con la sesión del
  usuario.
- **Inferencia on-device.** La imagen se analiza en el dispositivo; no se envía a un servicio externo para
  su análisis.
- **Sin secretos en el repositorio.** Las claves de configuración se aportan en el momento de compilar,
  fuera del control de versiones.

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

> 🔒 El proyecto compila sin el modelo, pero **la pantalla de Análisis no puede analizar**: el fichero
> `.onnx` que carga `CellDetectorModel.kt` no está en el repositorio. El resto de la aplicación (acceso,
> historial, pacientes, ajustes) funciona con normalidad. Consulta la [licencia](LICENSE) antes de compilar.

**Requisitos**

- JDK 17
- Android Studio (versión reciente) o el SDK de Android con `compileSdk 36`
- Para el empaquetado de macOS, un JDK con `jpackage` disponible
- Para iOS, un Mac con Xcode reciente

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

Los paquetes de **Windows** y **Linux** no pueden generarse desde macOS: `jpackage` solo empaqueta para el
sistema en el que se ejecuta. Se construyen con el workflow **Desktop package**, que se lanza a mano desde la
pestaña *Actions* del repositorio y deja el resultado como artefacto descargable.

**iOS** no se compila con Gradle: se abre `iosApp/iosApp.xcodeproj` en Xcode y se ejecuta desde ahí. El
framework `shared` lo genera el propio proyecto mediante una fase de compilación que invoca
`:shared:embedAndSignAppleFrameworkForXcode`, así que no hay que construirlo por separado. Para ejecutar
en un iPhone físico hace falta indicar el `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`; en el
simulador puede quedarse vacío.

> Requisitos mínimos de ejecución: **Android 8.1 (API 27)** o superior · **iOS 18.6** o superior ·
> **macOS** (paquete DMG).

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
- [x] Soporte de iOS
- [x] Distribución de iOS por TestFlight
- [x] Soporte Windows
- [ ] Selección de bloques de imágenes
- [x] Selección de imágenes de ubicaciones externas
- [x] Documentación final del TFM

## Licencia y atribución

Desarrollado por **Lovelaced** — Sergio Álvarez.

**citoVision** — Copyright © 2026 Sergio Álvarez. Todos los derechos reservados.

Software **propietario**: el código se publica para su lectura y evaluación, no para su uso, copia,
modificación o distribución. Los términos completos están en [`LICENSE`](LICENSE). Es un prototipo académico
y experimental: no está clínicamente validado, no es un producto sanitario y no debe utilizarse para emitir
diagnósticos ni sustituir el criterio de profesionales sanitarios. Para cualquier uso, reutilización o
modificación del código, escribe a **[hola@citovision.app](mailto:hola@citovision.app)**.

**Atribución del dataset** — citoVision utiliza el **UNIVALI Leukocyte Dataset**, disponible en
[Zenodo](https://zenodo.org/records/17743609) bajo licencia **Creative Commons Attribution 4.0 International
(CC BY 4.0)**. El conjunto de datos se reorganizó mediante un reparto estratificado y se empleó para ajustar
un modelo de segmentación. Las imágenes de frotis que aparecen en este README proceden de ese dataset y se
muestran **redimensionadas para su visualización**. Los autores del dataset no respaldan ni certifican
citoVision.
