# ADR-0010 - Distribución de iOS por TestFlight

## Estado

Aceptada — 2026-09-03

## Contexto

citoVision se entrega en tres plataformas, pero hasta ahora **solo dos podían instalarse**: Android como
APK y macOS como DMG, ambos descargables y ejecutables por cualquiera. **En iOS no había ninguna vía**:
quien quisiera probar la app tenía que clonar el repositorio y compilarla con Xcode en un Mac.

Para la defensa del TFM eso no sirve. El tribunal debe poder ejecutar la aplicación en un iPhone sin
disponer de un entorno de desarrollo.

La causa no es una carencia del proyecto, sino una diferencia real entre plataformas: **Android permite
instalar fuera de su tienda y iOS no**. Un iPhone solo ejecuta aplicaciones firmadas por un perfil que
lo autorice, y **las tres formas de conseguirlo exigen una membresía de pago** del Apple Developer
Program (99 USD/año):

| Vía | Alcance | ¿UDID por persona? | Instalación | Revisión |
|---|---|---|---|---|
| **Development** | 100 dispositivos/tipo/año | Sí | Xcode y cable | No |
| **Ad Hoc** | 100 dispositivos/tipo/año | Sí | `.ipa` por Configurator u OTA | No |
| **TestFlight** | 100 internos + 10 000 externos | **No** | App TestFlight, por enlace | Primera build |

Un Apple ID gratuito permite instalar en el dispositivo propio desde Xcode, pero **el perfil caduca a
los 7 días** y sigue exigiendo un Mac: como vía de distribución no existe.

## Decisión

**La distribución de iOS se hace por TestFlight, mediante un grupo de testers externos con enlace
público.** Android mantiene el APK de descarga directa.

### 1. Por qué TestFlight y no las otras dos

Development y Ad Hoc obligan a **registrar el UDID de cada dispositivo antes de compilar** y a
**regenerar la build cada vez que se suma alguien**. Aplicado a un tribunal, significa pedir a cada
miembro el identificador de su teléfono y rehacer el paquete por cada incorporación. TestFlight no
necesita nada de eso: se envía una invitación o un enlace, y la persona instala desde la app TestFlight.

### 2. Grupo externo, no interno

Los testers *internos* de TestFlight son **usuarios de App Store Connect**, es decir, personas con
acceso a la cuenta de desarrollador. Un grupo *externo* con enlace público consigue lo mismo sin
conceder ese acceso, al precio de pasar **Beta App Review una vez** (solo la primera build del grupo).

### 3. Android sigue sin tienda

No se abre cuenta de Google Play. Android **no necesita una tienda para distribuirse**, y el APK firmado
del entregable ya cumple esa función. La asimetría se documenta en el README en lugar de disimularla:
explicar por qué una plataforma necesita tienda y la otra no forma parte del contenido del trabajo.

### 4. Ajustes que la decisión obliga a hacer en el proyecto

Preparar la primera subida destapó tres defectos, ninguno visible mientras solo se compilaba para
simulador y dispositivo propio:

- **Las versiones estaban escritas a mano en `Info.plist`.** Con `GENERATE_INFOPLIST_FILE = NO` manda ese
  fichero, así que los ajustes `MARKETING_VERSION` y `CURRENT_PROJECT_VERSION` quedaban **inertes**. Como
  TestFlight rechaza toda build cuyo `CFBundleVersion` ya se haya subido, la segunda subida habría
  fallado sin causa aparente. Ahora el plist interpola ambos ajustes.
- **Faltaba `ITSAppUsesNonExemptEncryption`.** Sin ella, App Store Connect pregunta por el cumplimiento
  de exportación en cada subida. La app solo usa el HTTPS/TLS del sistema, que es criptografía exenta, y
  así se declara.
- **El `DEVELOPMENT_TEAM` estaba escrito a fuego en el `.pbxproj`** (el equipo personal que Xcode
  registró al probar en dispositivo), contradiciendo a `Config.xcconfig`, que documenta `TEAM_ID` como el
  lugar donde va. Pasa a `$(TEAM_ID)`, con el valor de la membresía de pago en el `.xcconfig`.

## Alternativas consideradas

1. **Ad Hoc con UDIDs.** Evita la revisión de Apple, pero exige recopilar identificadores y recompilar
   por cada tester. Descartada por inviable con un tribunal.
2. **Solo Development.** Igual de costosa en UDIDs y además necesita Xcode en el lado del instalador.
   Descartada.
3. **No matricularse y documentar que iOS se compila desde el repo.** Era la opción honesta mientras no
   hubiera cuenta, y se mantuvo abierta hasta hoy. Descartada porque deja a iOS sin forma de evaluarse.
4. **Google Play internal testing, para dar paridad a Android.** Son 25 USD por una vez y el tribunal
   instalaría igual en las dos plataformas. Descartada por ahora: añade una gestión más sin resolver
   ningún problema, ya que en Android el APK **sí** funciona. Puede añadirse después sin tocar nada de
   lo decidido aquí.
5. **Grupo interno de TestFlight en lugar de externo.** Se ahorra la Beta App Review, pero obliga a dar
   de alta a cada evaluador como usuario de App Store Connect. Descartada por dar acceso innecesario a
   la cuenta.

## Consecuencias

**Positivas**

- iOS pasa a ser instalable por terceros: el tribunal puede ejecutar la app en su propio iPhone.
- La instalación no requiere Mac, Xcode, cable ni identificadores de dispositivo.
- Al arreglar las versiones, el número de build vuelve a ser gobernable desde Xcode, que es lo que la
  configuración aparentaba y no hacía.

**Negativas / deuda**

- **Las builds de TestFlight caducan a los 90 días.** Si la defensa es posterior, hay que subir otra.
  Es el riesgo operativo más probable de esta decisión.
- **99 USD/año recurrentes.** Al vencer la membresía, la vía desaparece: los perfiles dejan de ser
  válidos y TestFlight deja de distribuir.
- **El requisito mínimo es iOS 18.6** (ADR-0008), lo que excluye iPhones que no pasen de iOS 17. Se
  asume y se documenta en el README; bajarlo es una tarea aparte y con riesgo.
- **Al matricularse como Individual, la app figura bajo el nombre legal del desarrollador.** Irrelevante
  para un reparto por invitación, pero conviene saberlo.
- **La revisión de Apple examina con lupa lo que parece software médico.** El encuadre **no diagnóstico**
  ya está decidido (SPEC-0006, ADR-0003); lo que hace falta es hacérselo explícito al revisor, de ahí el
  anexo de este documento.
- Las dos plataformas se instalan de forma distinta, y eso hay que explicárselo al usuario.

## Verificación

La ejecuta el desarrollador (AGENTS.md §16 y §22; certificados y *provisioning* son competencia suya):

1. Compilar en simulador y comprobar en el bundle generado que `CFBundleShortVersionString` vale
   `1.0.0` y `CFBundleVersion` vale `1`. Si salieran vacíos, la interpolación no se aplicó.
2. Con la membresía activa, poner el Team ID nuevo en `Config.xcconfig` y comprobar que Xcode firma sin
   avisos en *Signing & Capabilities*.
3. *Product → Archive* con destino **Any iOS Device (arm64)** → *Distribute App* → *TestFlight & App
   Store* → *Upload*.
4. Que App Store Connect **no** pregunte por cumplimiento de exportación: confirma la clave nueva.
5. Subir una segunda build con `CURRENT_PROJECT_VERSION = 2` y comprobar que TestFlight la acepta como
   distinta de la primera.
6. Instalar desde TestFlight en un iPhone que no sea el de desarrollo y validar el flujo completo:
   login, selección de imagen, análisis e historial.

> Si la validación de la subida se queja de *"unsupported architectures"*, el origen habitual es que se
> hayan colado *slices* de simulador en el framework dinámico de `shared`. Limpiar la carpeta de build y
> volver a archivar.

## Anexo — Textos para App Store Connect

**Descripción de la beta**

> citoVision es una herramienta de apoyo al cribado de muestras citológicas desarrollada como Trabajo Fin
> de Máster. Analiza imágenes de microscopía en el propio dispositivo y propone un orden de revisión.
> No emite diagnósticos.

**Qué probar**

> Iniciar sesión (cuenta de Google, email o modo invitado), seleccionar una imagen de muestra, ejecutar
> el análisis y consultar el resultado en el historial y por paciente. En Ajustes puede elegirse si las
> imágenes se buscan en la galería o en el gestor de archivos.

**Notas para el revisor** — el punto importante

> Esta aplicación es un proyecto académico (Trabajo Fin de Máster) y **NO es un producto sanitario ni una
> herramienta de diagnóstico**. No emite diagnósticos, no confirma ni descarta patologías y no sustituye
> el criterio de un profesional.
>
> Lo que hace es ordenar por prioridad de revisión un conjunto de muestras, a partir de la presencia
> relativa de tipos celulares detectados con un modelo de visión por computador que se ejecuta en el
> dispositivo. La salida es una sugerencia de orden de trabajo, no un resultado clínico. La interfaz lo
> indica de forma explícita.
>
> Las imágenes de muestra utilizadas proceden de un conjunto de datos público de uso académico.

**Credenciales para la revisión**

La app permite entrar en **modo invitado** sin credenciales, así que no hace falta facilitar una cuenta
de prueba. Conviene indicarlo, porque un revisor que se encuentre una pantalla de login sin acceso puede
rechazar la build por ese solo motivo.

## Referencias

- **ADR-0004** (arranque de iOS), **ADR-0006** (autenticación) y **ADR-0008** (barra nativa y el
  *deployment target* 18.6).
- **SPEC-0006** y **ADR-0003**: carácter no diagnóstico del análisis y política de priorización.
- TestFlight, límites y caducidad de builds:
  `https://developer.apple.com/help/app-store-connect/test-a-beta-version/overview-of-testflight`
- Registro de dispositivos (Ad Hoc y Development):
  `https://developer.apple.com/help/account/devices/devices-overview`
- Matriculación en el Apple Developer Program: `https://developer.apple.com/support/enrollment/`
- AGENTS.md §8 (ADR), §16 (compilación) y §22 (firma y *provisioning*).
