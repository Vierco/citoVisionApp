# ADR-0007 - Inferencia ONNX en iOS: ONNX Runtime por SPM ejecutado desde Swift

## Estado

Aceptada — 2026-08-31 · con una **actualización del 2026-09-04** al final del documento

## Contexto

Las fases 1 (arranque, ADR-0004) y 2 (autenticación, ADR-0006) dejaron la app de iOS funcionando, pero la
**inferencia celular sigue en stub**. Son exactamente dos clases de `iosMain`:

| Clase | Hoy en iOS | Android | Desktop |
|---|---|---|---|
| `ImageDecoderImpl` | devuelve `null` → `ImageDecodeFailed` | `BitmapFactory` | `javax.imageio.ImageIO` |
| `OnnxRunnerImpl` | lanza `NotImplementedError` | `ai.onnxruntime` (Java) | `ai.onnxruntime` (Java) |

Todo lo demás **ya es común y está cableado**: el puerto `CellDetector`, el orquestador `CellDetectorImpl`,
el pre/postprocesado determinista (`YoloPreprocessor`, `YoloPostprocessor`, `ConfidencePolicy`,
`PriorityCalculator`), el modelo empaquetado en Compose Resources y el registro en Koin
(`PlatformModule.ios.kt` ya declara `ImageDecoder` y `OnnxRunner`). Cerrar esta fase **no toca `commonMain`
salvo de forma aditiva**, ni Android, ni Desktop.

**ADR-0003 anticipó el camino** como *"iOS (futuro) → C/Obj-C API vía `cinterop`"*, y SPEC-0006 lo dejó
fuera de alcance explícitamente. Este ADR concreta ese punto abierto, y **se aparta de la vía cinterop** por
las restricciones que se han acumulado desde entonces:

1. **CocoaPods está vetado** por el owner (Apple lo deprecia; decisión fijada en ADR-0006). La distribución
   principal de ONNX Runtime para iOS (`onnxruntime-c`, `onnxruntime-objc`) es CocoaPods.
2. **El framework de Kotlin es obligatoriamente dinámico** (ADR-0005: con Xcode 26, el estático rompe con
   `cannot link directly with 'SwiftUICore'`). Un framework dinámico **debe resolver todos sus símbolos al
   enlazarse él mismo**, así que un `cinterop` contra ONNX Runtime obligaría a que **Gradle** poseyera y
   enlazara el binario nativo, no Xcode.
3. **Existe paquete SPM oficial de Microsoft**
   ([`microsoft/onnxruntime-swift-package-manager`](https://github.com/microsoft/onnxruntime-swift-package-manager)),
   ORT **1.24.2**, iOS 15+, producto `onnxruntime` (módulo Swift `OnnxRuntimeBindings`). El target de la app
   ya está en `IPHONEOS_DEPLOYMENT_TARGET = 18.6`, así que cumple de sobra.

Hechos verificados del API Objective-C de ONNX Runtime, que condicionan el diseño:

- `ORTSession` **solo** tiene el inicializador `initWithEnv:modelPath:sessionOptions:error:` → **acepta una
  ruta de fichero, no bytes en memoria**. (El API en C sí tiene `CreateSessionFromArray`; el de ObjC no.)
- `ORTValue(tensorData:elementType:shape:)` toma un `NSMutableData` **del llamante, sin copiarlo**, y
  `tensorDataWithError:` devuelve el `NSMutableData` subyacente, también sin copia.

## Decisión

**Mismo patrón que ADR-0006: Kotlin no enlaza ningún SDK nativo.** ONNX Runtime entra por SPM en el target de
Xcode y se ejecuta desde Swift; Kotlin le pasa el tensor por un puente.

### 1. `ImageDecoderImpl` de iOS: CoreGraphics puro en Kotlin/Native

Sin puente y sin dependencias nuevas: `platform.CoreGraphics` y `platform.UIKit` ya vienen con
Kotlin/Native. Se decodifica con `UIImage`, se dibuja en un `CGBitmapContext` de 32 bits creado **sobre el
propio `IntArray`** de `RgbImage` (`usePinned`), con
`kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Little`. Esa combinación deja en memoria los bytes
`B,G,R,A` que, leídos como entero *little-endian*, son exactamente el `0xAARRGGBB` que espera
`YoloPreprocessor`: **cero bucles de conversión**. Bytes no decodificables → `null`, como en las otras
plataformas.

### 2. `OnnxRunnerImpl` de iOS: puente a Swift

- **ONNX Runtime 1.24.2 por SPM**, añadido **solo al target de Xcode** (producto `onnxruntime`).
- Un objeto `OnnxBridge` en `iosMain` expone un punto de registro que Swift rellena al arrancar (igual que
  `GoogleSignInBridge`). Si Swift no lo ha registrado, la inferencia **falla de forma controlada**
  (`InferenceError`), nunca por crash.
- La llamada es **síncrona**: `CellDetectorImpl` ya ejecuta todo en el `defaultDispatcher` inyectado
  (SPEC-0006 RNF), así que no hace falta continuación ni callback asíncrono.

**Los datos cruzan como `NSData`, no como `FloatArray`.** El tensor de entrada son 1×3×640×640 = **1 228 800
floats** y la salida ~420 000. Un `KotlinFloatArray` en Swift se recorre elemento a elemento por envío de
mensaje ObjC: serían **~1,6 millones de mensajes por análisis**. Con `NSData` es un bloque de memoria y una
copia. El resultado vuelve en una clase exportada pequeña:

```kotlin
class OnnxNativeResult(val output: NSData?, val attributes: Int, val error: String?)
```

Los `Int` de un constructor se exportan como `Int32` sin boxing (a diferencia de los parámetros de lambda,
que sí se boxean a `KotlinInt`), así que Swift la construye sin fricción.

### 3. El modelo viaja por ruta, no por bytes

Como `ORTSession` solo acepta `modelPath`, Kotlin resuelve la **ruta del `.onnx` dentro del bundle** con
`Res.getUri(...)` (en iOS, Compose Resources vive en
`NSBundle.mainBundle.resourcePath/compose-resources/`) y se la pasa a Swift. **Sin copiar 39 MB.**

Como red de seguridad, si esa ruta no existe en disco (cambio interno de Compose Resources, recurso dentro
del bundle del framework, URI escapada), se **extrae una sola vez** a `Caches` con el
`modelProvider: suspend () -> ByteArray` que la clase ya recibe hoy por Koin. Así la firma del constructor
**no cambia** y la línea de DI queda intacta.

### 4. Ciclo de vida y aceleración

- La `ORTSession` se crea **una vez** y se cachea en el lado Swift, cumpliendo **SPEC-0006 RF-8** ("el modelo
  se carga una vez y se reutiliza entre análisis").
- Se arranca **solo con CPU**. El *execution provider* de **CoreML** queda como optimización futura
  (SPEC-0006 lo declara optimización, no requisito) y se podrá activar sin tocar Kotlin.

### 5. Lo que NO cambia

Mismo `.onnx`, mismo pre/postprocesado de `commonMain`, misma política de umbrales por clase (RN-2), misma
priorización (RN-8/RN-9). **Android y Desktop no se tocan.** `commonMain` solo recibe una función aditiva
para exponer la URI del modelo, y así no duplicar el nombre del fichero.

## Alternativas consideradas

1. **`cinterop` contra el API C de ONNX Runtime** *(lo que anticipaba ADR-0003)*. Es la vía "de manual" en
   KMP y evitaría escribir Swift. **Descartada por coste y riesgo, no por imposibilidad:** al ser el
   framework dinámico, Gradle tendría que descargar, versionar y enlazar él mismo el `xcframework` (>100 MB)
   con `linkerOpts`, quedando fuera del gestor de paquetes de Xcode; y si algún día la app necesitase ORT
   también desde Swift, acabaría con el runtime duplicado. La alternativa de enlazar con
   `-undefined dynamic_lookup` se descarta por frágil y hostil a la App Store. Revisable si algún día se
   vuelve a un framework estático.
2. **Convertir el modelo a Core ML.** Descartada: rompe la decisión central de ADR-0003 (**un único
   artefacto `.onnx` para las tres plataformas**), obligaría a revalidar el pipeline contra la referencia de
   Ultralytics en una plataforma más y a mantener dos exports.
3. **KInference (ONNX en Kotlin puro).** Descartada: no cubre las operaciones de un YOLO11-seg y el
   rendimiento en Kotlin/Native no es comparable al runtime nativo.
4. **Inferencia en servidor solo para iOS.** Descartada: contradice ADR-0003 (on-device por decisión del
   owner) y rompe la garantía de privacidad de SPEC-0006 (*la imagen no sale del dispositivo*).
5. **Pasar el tensor como `FloatArray`/`KotlinFloatArray`.** Descartada por el coste de interop descrito
   arriba (~1,6 M de mensajes ObjC por análisis).

## Consecuencias

**Positivas**

- Cierra la paridad funcional de iOS con Android y Desktop: **el mismo modelo y el mismo pipeline en las tres
  plataformas**, que es justo lo que ADR-0003 buscaba.
- **Riesgo de enlazado nulo**: el framework de Kotlin sigue sin depender de binarios nativos, así que
  continúa siendo dinámico sin conflicto (ADR-0005).
- **Patrón ya probado en el proyecto**: es el mismo puente que Google Sign-In (ADR-0006), con sus trampas ya
  conocidas y documentadas.
- Sin CocoaPods, respetando la restricción del owner.
- El `ImageDecoder` no necesita puente: es Kotlin/Native puro y **testeable en `iosTest`** el día que exista.

**Negativas / deuda**

- **Una pieza más de Swift** que mantener y que hay que registrar en `project.pbxproj` (trampa conocida de la
  Fase 2: un `.swift` no registrado sencillamente no se compila).
- **`OnnxRunnerImpl` de iOS no es testeable sin el host Swift** (misma limitación que el lanzador de Google).
  Se compensa con que toda la lógica determinista ya está cubierta en `commonTest`.
- **+~15-25 MB al binario de iOS** por el runtime de ONNX, sumados a los 39 MB del modelo (AGENTS.md §16).
- **Dependencia de un detalle interno de Compose Resources** (la ruta del bundle) para evitar la copia del
  modelo; mitigado con el fallback de extracción.
- La primera inferencia carga 39 MB desde disco: hay que confirmar que el arranque del análisis no dispara
  aviso de memoria en dispositivo real.

## Verificación

La ejecuta el desarrollador (AGENTS.md §16: el agente no compila):

1. `./gradlew :shared:ktlintCheck` y `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`.
2. `./gradlew :shared:allTests` — sin regresión en `commonTest`.
3. En simulador: cargar imagen → "Iniciar Escáner" → estado de carga → análisis guardado con prioridad, y la
   navegación al Historial de RF-5b.
4. **Validación del pipeline contra la referencia de SPEC-0006** (mismos ficheros que se usaron en Android y
   Desktop; los números deben coincidir, porque el pre/postprocesado es el mismo código):
   - `img_001304.jpg` → normales: `Mielocito` 0.9728, `Neutrófilo segmentado` 0.8536; revisión:
     `Mielocito` 0.0907, `Promielocito` 0.0897.
   - `img_002149.jpg` → `Neutrófilo en banda` 91 %, `Eosinófilo` 96 %, `Metamielocito` 97 %,
     `Promielocito` 68 %.
5. Imagen sin células → popup informativo y **no** se persiste (RF-6/RN-7).
6. Segundo análisis seguido: **no** se recarga el modelo (RF-8) y el resultado es idéntico (RN-5).
7. **Sin regresión**: Android y Desktop siguen analizando igual.

## Limitación conocida: reproducibilidad entre dispositivos

*Medido el 2026-08-31, al ejecutar la verificación de este ADR.*

**Lo observado.** Con `img_001304.jpg`, **Android y el simulador de iOS reproducen la referencia de Ultralytics
exactamente**. En un **iPhone SE (2.ª generación)** el resultado es el mismo salvo por el `Promielocito` que
la referencia sitúa en **0.0897**: en el dispositivo sube lo justo para cruzar el umbral **0.10** de las
clases críticas (RN-2). No es una detección nueva, es la misma que pasa de `LOW_CONFIDENCE_REVIEW` a
`STANDARD`.

**La consecuencia no es el recuento, es la prioridad.** Al contar como detección confirmada, ese
`Promielocito` aporta su peso de RN-8:

| | Detecciones `STANDARD` | Puntuación RN-8 | Prioridad RN-9 |
|---|---|---|---|
| Android / simulador | Mielocito (+3), Neutrófilo segmentado (+0) | 3 | **MEDIA** |
| iPhone SE 2 | + Promielocito (+4) | 7 | **ALTA** |

**Causa.** No es un defecto del pipeline compartido: que Android y el simulador reproduzcan la referencia lo
valida (letterbox, decodificado, NMS y umbrales son el mismo Kotlin en las tres plataformas). El origen está
por debajo, y hay dos familias posibles:

1. **El decodificado de la imagen no es idéntico.** Los iPhone decodifican JPEG **por hardware**; el
   simulador usa la ruta software del Mac. Distinto *upsampling* de croma o redondeo del IDCT y los píxeles
   difieren en algún bit menos significativo.
2. **La aritmética del runtime no es idéntica.** ONNX Runtime selecciona *kernels* SIMD según las
   características de CPU detectadas en ejecución y reparte los GEMM entre hilos; el A13 y el M-series del
   host no producen el mismo orden de acumulación en punto flotante.

Se pueden separar con un diagnóstico barato: **comparar un hash de `RgbImage.pixels` ya decodificado** en
simulador y dispositivo. Hash igual → es el runtime; hash distinto → es el decodificador.

**Alcance real.** Solo afecta a detecciones situadas a **±0.01 de un umbral**. `img_001304.jpg` es casi el
peor caso posible: se eligió como referencia precisamente por tener detecciones pegadas al límite (0.0907 y
0.0897). Una muestra con confianzas altas es insensible a esto.

**No es un problema de iOS.** Es una propiedad de la inferencia on-device sobre hardware heterogéneo, y muy
probablemente se daría igual entre dos Android con SoC distintos; simplemente no se ha probado. Ha aflorado
aquí porque iOS ha dado el primer par simulador/dispositivo con CPU diferentes.

**Decisión del owner (2026-08-31): se acepta como limitación conocida y no se mitiga.** El motivo es que no
existe control sobre el universo de SoC en los que la app puede ejecutarse, y la limitación **deja de ser un
problema en cuanto se explica**. Explícitamente **no se tocan los umbrales de RN-2**: salieron de medir
*recall* sobre el split `valid`, y moverlos para esquivar un artefacto numérico cambiaría una sensibilidad
medida por otra sin medir.

**Lo que contiene el riesgo** es el propio encuadre del producto: la prioridad es una **sugerencia de orden
de revisión**, nunca una conclusión clínica, y el profesional humano está siempre en el bucle (SPEC-0006
RN-10, con el aviso de no-diagnóstico de RF-3c como requisito de UI). Una muestra que en un dispositivo sale
MEDIA y en otro ALTA se revisa igualmente; lo que cambia es su puesto en la cola.

**Palanca disponible si algún día hiciera falta**, y solo si el diagnóstico apunta al runtime: fijar
`setIntraOpNumThreads(1)` y bajar el nivel de optimización del grafo en las `ORTSessionOptions` elimina la
varianza por reparto entre hilos, a costa de velocidad. No elimina la debida a *kernels* distintos.

> **Nota sobre SPEC-0006 RN-5.** La regla dice *"El resultado es reproducible para la misma imagen y modelo
> (inferencia determinista)"*. A la luz de lo anterior, eso se cumple **dentro de un mismo dispositivo**,
> pero no necesariamente entre dispositivos distintos. La spec está aprobada y **no se modifica aquí**;
> queda señalado por si el owner quiere matizarla (AGENTS.md §0.4).

## Actualización — 2026-09-04: el paquete SPM impide subir la app a App Store Connect

El texto anterior se conserva tal como se escribió. La decisión **no cambia**: ONNX Runtime sigue entrando
por SPM y ejecutándose desde Swift. Lo que aparece aquí es un **coste de esa vía que no se había
manifestado**, porque hasta ahora nunca se había archivado la app para distribuirla (ADR-0010).

**Apple rechazó la primera build** enviada a TestFlight, ya procesada, con este error:

> ITMS-90208: Invalid Bundle - The bundle `citoVision.app/Frameworks/onnxruntime.framework` does not
> support the minimum OS Version specified in the Info.plist.

La causa se localizó **inspeccionando el `.xcarchive` y el artefacto del paquete**, no por conjetura:

| | `MinimumOSVersion` del `Info.plist` | binario (`minos`) | SDK |
|---|---|---|---|
| `onnxruntime.xcframework` original (SPM) | 15.1 | 15.1 | 18.2 |
| `onnxruntime.framework` dentro del archivo | 15.1 | **18.6** | **26.5** |

El `Info.plist` viaja **intacto** desde el paquete, mientras que el binario se produce con el
`IPHONEOS_DEPLOYMENT_TARGET` del proyecto. El framework acaba declarando que soporta iOS 15.1 cuando su
binario exige 18.6, y la validación de Apple rechaza esa incoherencia.

**Es un defecto conocido de la distribución por SPM de ONNX Runtime**, no del proyecto: issue
[microsoft/onnxruntime#27396](https://github.com/microsoft/onnxruntime/issues/27396), abierta y sin
corregir en el paquete. **No se reproduce integrando por CocoaPods**, que aquí no es una salida por el veto
de ADR-0006.

### Solución adoptada

Una fase *Run Script* en el target `iosApp`, **`Fix ONNX Runtime MinimumOSVersion`**, colocada **la última**,
que reescribe el `MinimumOSVersion` del framework con `${IPHONEOS_DEPLOYMENT_TARGET}` mediante `PlistBuddy`
y **vuelve a firmar el bundle**. Se ejecuta siempre (`alwaysOutOfDate`). Commit `2cf8344`.

Dos detalles que el *workaround* de la issue no menciona y sin los cuales no funciona:

1. **Hay que re-firmar el framework.** Modificar el `Info.plist` de un bundle ya firmado invalida su firma,
   lo que produciría un rechazo distinto y menos evidente que el original. De ahí el `codesign --force
   --preserve-metadata=identifier,entitlements,flags`.
2. **Este target no tiene fase «Embed Frameworks».** Los productos de SPM marcados *Embed & Sign* los copia
   Xcode por su cuenta, sin fase visible, así que **no había forma de saber sobre el papel** si el script
   correría antes o después de esa copia. Se comprobó sobre el archivo generado: `MinimumOSVersion` = 18.6 y
   `codesign --verify --deep --strict` válido en el framework y en el bundle completo.

### Consecuencias

- **El número de build de la entrega rechazada se pierde.** `CFBundleVersion` no puede reutilizarse aunque
  la build nunca llegue a TestFlight, así que la subida buena fue la `1.0.0 (2)`.
- **Procedimiento recomendado para futuras subidas:** archivar → inspeccionar el `.xcarchive` → **Validate
  App** en el Organizer, que valida contra Apple **sin consumir el número de build** → *Distribute*.
- **Aviso benigno permanente:** cada subida y cada validación informan de *«Upload Symbols Failed — the
  archive did not include a dSYM for onnxruntime.framework»*. El framework llega precompilado y sin dSYM.
  No impide la subida; solo implica trazas sin simbolizar dentro de ONNX Runtime.
- Si Microsoft corrige el paquete, esta fase puede retirarse. Mientras tanto es **imprescindible para
  distribuir**, no una optimización.

## Referencias

- **ADR-0003** (ONNX Runtime on-device): decisión que este ADR completa para iOS.
- **ADR-0010** (distribución por TestFlight): contexto en el que apareció el ITMS-90208.
- Defecto del paquete SPM: `https://github.com/microsoft/onnxruntime/issues/27396`.
- **SPEC-0006** (análisis celular): RF-1, RF-4, RF-6, RF-7, RF-8, RN-2, RN-5; casos de referencia.
- **ADR-0004** (bring-up iOS) y **ADR-0005** (toolchain y framework dinámico): origen de la restricción de
  enlazado.
- **ADR-0006** (auth iOS): precedente del patrón "SDK nativo solo en Swift + puente".
- ONNX Runtime SPM: `https://github.com/microsoft/onnxruntime-swift-package-manager` (1.24.2, iOS 15+).
- API Objective-C de ONNX Runtime: `ORTEnv`, `ORTSession`, `ORTValue`, `ORTSessionOptions`.
- AGENTS.md §2, §3, §8, §16.
