# ADR-0011 - Soporte de Windows en el objetivo Desktop

## Estado

Aceptada — 2026-09-07

## Contexto

El objetivo `desktopApp` de citoVision es Kotlin/JVM y Compose Multiplatform, de modo que **el mismo
código fuente vale para macOS, Windows y Linux**. Sin embargo, el MVP declaraba soportado únicamente
macOS (DMG), y el `README` recogía Windows y Linux como *fuera del MVP*.

Esa exclusión no era una decisión de producto sino **el reconocimiento de un fallo sin diagnosticar**.
El 2026-07-20 se montó el empaquetado multiplataforma completo —iconos `.ico` y `.png`, bloques
`windows { }` y `linux { }` en `nativeDistributions` y el workflow manual `desktop-package.yml`, que
compila en runners de GitHub porque `jpackage` **solo empaqueta para el sistema en el que se ejecuta**—
y el ejecutable de Windows resultante **abría pero fallaba al entrar como invitado**, con un error de
dependencias que nunca llegó a capturarse. El 2026-07-22 se intentó retomarlo y se abandonó por tiempo:
no se consiguió ninguna traza.

Quedó en pie una hipótesis y un descarte:

- **Hipótesis:** que fuera el mismo `NoClassDefFoundError: sun/misc/Unsafe` que rompía el `.app` de
  macOS, causado por que `jpackage` construye con `jlink` un runtime recortado que deja fuera el módulo
  `jdk.unsupported`, donde vive esa clase que el protobuf de DataStore necesita al serializar
  preferencias.
- **Descarte:** esa hipótesis **no podía ser la causa**, porque `modules("jdk.unsupported")` está
  declarado en el bloque `nativeDistributions` **compartido**, no dentro de `macOS { }`, y el historial
  de Git confirma que el arreglo, el bloque `windows { }` y el workflow entraron los tres en el mismo
  commit (`a27a5fc`). Cualquier paquete producido por ese workflow ya llevaba el arreglo.

Es decir: se sabía qué **no** era, pero no qué era.

## Decisión

**Windows pasa a ser una plataforma soportada del objetivo Desktop, y se distribuye como ZIP del
*app-image*.** Linux sigue fuera: el workflow lo construye, pero no se ha verificado en una máquina
real.

### 1. Por qué se pudo cerrar: se midió antes de arreglar

El 2026-09-07 se verificó el paquete en una máquina Windows real, y el diagnóstico dio un resultado
inesperado: **el fallo de invitado ya no se reproduce**. Los dos meses de desarrollo transcurridos
—tema oscuro, migración de Room a v6, detalles de UX del análisis— lo resolvieron de forma colateral.
No se ha determinado cuál de esos cambios fue, y no se investiga: no hay defecto que corregir.

El método sí merece registrarse, porque es el mismo que resolvió los tres fallos del `.app` de macOS y
el ITMS-90208 de ADR-0007: **no proponer arreglos sobre hipótesis sin medir**. Durante dos meses la
causa «obvia» fue una que ya estaba descartada por el propio historial del repositorio.

### 2. El defecto real que sí apareció

Verificar en la máquina real destapó un fallo distinto y genuinamente específico de Windows: **las
imágenes de los análisis locales no se mostraban en la pantalla de Historial**, mientras que en
Pacientes sí se veían.

La causa estaba en cómo se construía el modelo que recibe Coil:

```kotlin
val model = if (imagePath.startsWith("http")) imagePath else "file://$imagePath"
```

En macOS, Linux, Android e iOS la ruta local empieza por `/`, así que la concatenación produce
`file:///Users/...` — tres barras, que es la forma correcta— **por casualidad**. En Windows la ruta es
`C:\Users\...`, y el resultado es `file://C:\Users\...`, que no es una URI válida: con dos barras, lo
que sigue al esquema se interpreta como *autoridad* (host), de modo que `C:` se lee como nombre de
servidor, y además la contrabarra no es un separador legal en una URI. Coil no puede resolverla y la
tarjeta cae al *placeholder* gris previsto por RN-5 (SPEC-0004).

La asimetría entre pantallas era la pista: en Pacientes las imágenes provienen de URLs de descarga de
Firebase Storage (SPEC-0005), entran por la rama `http` y nunca pasan por esa concatenación.

Se corrige con una extensión que normaliza los separadores y añade la tercera barra cuando la ruta no
empieza por `/`, dejando `file:///C:/Users/...`. En el resto de plataformas el resultado es idéntico al
anterior, así que no hay regresión.

### 3. Inferencia ONNX verificada

La dependencia `com.microsoft.onnxruntime:onnxruntime` (JVM, 1.22.0) incluye las bibliotecas nativas de
Windows, Linux y macOS dentro del propio `jar`, sin `cinterop` ni empaquetado manual —a diferencia de
iOS, donde hizo falta SPM y Swift (ADR-0007)—. Se confirmó ejecutando análisis reales sobre varias
imágenes en Windows: **los resultados coinciden con los de Android e iOS**.

Esto no contradice la limitación registrada en ADR-0007, que afecta a **casos frontera** en los que un
umbral de prioridad se cruza por diferencias numéricas mínimas entre dispositivos. La coincidencia
observada aquí es evidencia de consistencia, no una garantía de reproducibilidad exacta.

### 4. Formato de distribución: ZIP del *app-image*, no MSI

El workflow construye `createDistributable` y no `packageMsi` deliberadamente:

- El instalador MSI **exige WiX instalado en el runner**, con la configuración adicional que eso
  implica.
- El *app-image* es **una carpeta con el ejecutable dentro**: se descomprime y se ejecuta, **sin
  instalar nada y sin permisos de administrador**. Para probar la app en una máquina prestada —que es
  exactamente el escenario del tribunal— es la vía con menos fricción.
- El MSI **no evitaría el aviso de SmartScreen**, porque ese aviso depende de la firma del ejecutable,
  no del formato de entrega.

### 5. SmartScreen: el equivalente de Gatekeeper

El ejecutable no está firmado con un certificado de Windows, igual que el DMG de macOS no está
notarizado. La primera ejecución puede mostrar el aviso «Windows protegió su PC», con la opción de
bloquear la aplicación; se abre con **Más información → Ejecutar de todas formas**.

El aviso **no aparece siempre**: SmartScreen se dispara con los ficheros que llevan la marca de
*descargado de internet* (Mark-of-the-Web), de modo que desbloquear el ZIP antes de descomprimirlo
—clic derecho → Propiedades → Desbloquear— o transferirlo por USB lo evita. Igual que con el DMG, esto
se documenta en las instrucciones que acompañan a la entrega.

## Consecuencias

- El `README` pasa a declarar Windows como plataforma soportada, con su formato y su requisito mínimo.
- Los paquetes de Windows salen del workflow `desktop-package.yml`, que se lanza a mano desde la pestaña
  *Actions*. **Los artefactos caducan a los 90 días**, así que una entrega posterior a ese plazo exige
  relanzarlo.
- **Linux queda verificado solo sobre el papel.** El workflow produce su *app-image* y el arreglo de la
  URI le aplica igual —la lista `modules(...)` y el código son compartidos—, pero **nadie lo ha
  ejecutado**. No se declara soportado.
- Firmar el ejecutable de Windows eliminaría el aviso de SmartScreen, pero exige un certificado de
  firma de código comercial. Queda fuera del alcance del TFM, en coherencia con el DMG sin notarizar.

## Referencias

- [ADR-0003](0003-inferencia-on-device-onnx-runtime.md) — inferencia on-device con ONNX Runtime.
- [ADR-0007](0007-inferencia-onnx-ios-spm-swift.md) — ONNX en iOS, y la limitación de reproducibilidad
  entre dispositivos.
- [ADR-0010](0010-distribucion-ios-testflight.md) — distribución de iOS, y por qué cada plataforma
  necesita una vía distinta.
- [SPEC-0004](../specs/0004-historial-analisis.md) — historial local e imágenes en fichero (RN-4, RN-5).
- [SPEC-0005](../specs/0005-base-datos-remota-pacientes.md) — imágenes remotas en Firebase Storage.
- `desktopApp/build.gradle.kts` — bloque `nativeDistributions`, módulos de `jlink` e iconos por sistema.
- `.github/workflows/desktop-package.yml` — empaquetado en runners de Windows y Linux.
