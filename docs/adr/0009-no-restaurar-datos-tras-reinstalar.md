# ADR-0009 - Una reinstalación no restaura datos de la instalación anterior

## Estado

Aceptada — 2026-09-02

## Contexto

Al reinstalar citoVision en iOS con la sesión iniciada, la app **volvía a entrar sola con el usuario
anterior**. Reproducido en un iPhone físico: desinstalar con sesión activa, reinstalar y aparecer ya
dentro. Cerrando sesión antes de desinstalar, en cambio, sí pedía credenciales.

Ese comportamiento no es un fallo de la app, sino de cómo funciona cada plataforma. El problema real es
que **son dos mecanismos distintos, y ninguno de los dos estaba decidido**: uno venía dado por iOS y el
otro por una plantilla de Android Studio sin revisar.

### iOS: el Keychain no pertenece al sandbox

Al borrar una app, iOS elimina su contenedor —`Documents`, `Library`, `tmp`, `NSUserDefaults`, la base
de Room, DataStore— pero **conserva sus ítems del Keychain**. Reinstalar con el mismo *bundle id*
devuelve el acceso a ellos. Apple llegó a cambiarlo en una beta de iOS 10.3 y revirtió el cambio antes
de publicarla, así que sigue vigente.

Como `KeychainTokenStore` guarda ahí la `StoredSession` (ADR-0006), la sesión sobrevivía a la
reinstalación. El ítem usa `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, así que **no viaja a
iCloud ni a los backups**: la exposición se limita al mismo teléfono, pero dentro de él persistía.

### Android: Auto Backup con las reglas de plantilla

En Android no sobrevive nada a la desinstalación; el vector es otro. El manifiesto declaraba
`android:allowBackup="true"` y los dos ficheros de reglas (`backup_rules.xml` y
`data_extraction_rules.xml`) estaban **tal cual los generó la plantilla, con todo el contenido
comentado**. Con esa configuración, Auto Backup considera elegible todo el directorio de datos: la base
de Room con códigos de paciente y análisis, las imágenes de muestra en ficheros y la sesión que
persiste el SDK de Firebase Auth. Eso se copia al Google Drive del usuario y se restaura al reinstalar
o al estrenar dispositivo.

Para datos clínicos, esto merece una decisión explícita. **SECURITY_MOBILE §Persistencia** pide
*"persistir únicamente la información estrictamente necesaria"* y *"evitar almacenar datos sensibles
cuando puedan obtenerse nuevamente desde el backend"*, y los análisis están en Firestore (SPEC-0005).

## Decisión

**Una instalación nueva empieza siempre limpia, en las dos plataformas.** No se hereda sesión ni datos
locales de una instalación anterior, ni siquiera en el mismo dispositivo.

### 1. iOS: purgar el Keychain en el primer arranque tras instalar

La detección se apoya en la asimetría que causa el problema: **el Keychain sobrevive a la
desinstalación y `NSUserDefaults` no.** Esa diferencia distingue con exactitud una instalación nueva de
un arranque normal, sin heurísticas.

`FreshInstallSessionGuard` (`iosMain/infrastructure/auth/`) comprueba una marca en `NSUserDefaults`; si
no está, purga el Keychain y la escribe. Es idempotente: a partir de la segunda vez no vuelve a tocar
nada.

Se invoca desde `bootstrap()` **antes de `initKoin`**. La posición no es casual: en cuanto existe el
grafo de dependencias, cualquier colaborador puede leer la sesión, así que la limpieza tiene que
ocurrir antes de que eso sea posible.

Para poder ejecutarse ahí, `KeychainTokenStore` expone `purge()`, un borrado **no suspendido** del que
cuelga también `clear()`. El Keychain es una API bloqueante y en ese borrado no había nada que
suspender, así que el `suspend` del contrato `TokenStore` no aportaba nada en este punto.

### 2. Android: no participar en ninguna copia ni transferencia

`android:allowBackup="false"`, más exclusiones completas en los dos ficheros de reglas. **Hicieron
falta las tres cosas**, porque dos detalles de la plataforma invalidan la solución evidente:

- Desde **API 31**, `allowBackup="false"` desactiva la copia en la nube pero **no la transferencia
  entre dispositivos**. Esta última solo se corta desde el bloque `<device-transfer>` de
  `data_extraction_rules.xml`.
- **Excluir `domain="root"` no arrastra `database` ni `sharedpref`**: son dominios propios y hay que
  declararlos por separado. Justamente la base de Room y las preferencias.

Por eso ambos ficheros declaran los **nueve dominios** (`root`, `file`, `database`, `sharedpref`,
`external` y sus cuatro variantes `device_*`), y `data_extraction_rules.xml` lo hace en sus dos
bloques. `backup_rules.xml` (API ≤ 30) queda inerte con `allowBackup="false"`, pero se mantiene escrito
para que la intención conste en los dos sitios y para que reactivar la copia algún día no exponga los
datos por omisión.

### 3. Lo que deliberadamente NO se hace

**No se toca el ítem del Keychain que gestiona el SDK de Google Sign-In.** Es suyo, y no permite entrar
por sí solo: la app nunca llama a `restorePreviousSignIn()` y siempre abre el selector de cuenta, así
que como mucho aparecerá la cuenta preseleccionada. Quien decide si hay sesión es `TokenStore`, y ese
queda limpio.

## Alternativas consideradas

1. **Dejarlo como estaba**, por ser el comportamiento nativo de iOS. Descartada: que la plataforma lo
   permita no lo convierte en deseable para datos clínicos, y quien reinstala una app espera empezar de
   cero.
2. **En Android, mantener `allowBackup="true"` y excluir solo lo sensible.** Conservaría la restauración
   de las preferencias de UI al cambiar de teléfono, pero obliga a acertar con la lista de exclusiones
   y a revisarla cada vez que se añada persistencia. Descartada por frágil: el fallo es silencioso y se
   descubre tarde.
3. **En iOS, marcar el ítem del Keychain como no persistente.** No existe tal atributo; la
   accesibilidad (`kSecAttrAccessible*`) gobierna cuándo se puede leer y si sale del dispositivo, no si
   sobrevive a la desinstalación.
4. **Usar el mismo centinela de "primera ejecución" en Android.** No funciona: si la copia restaura los
   datos, restaura también el centinela. En Android la solución tiene que estar en la configuración de
   la copia, no en el código.

## Consecuencias

**Positivas**

- Una reinstalación deja la app en estado inicial en las dos plataformas, sin sesión heredada.
- Los datos clínicos dejan de ser elegibles para la copia en Drive y para la transferencia entre
  dispositivos.
- Cumple SECURITY_MOBILE §Tokens y §Persistencia. Responde a dos entradas del OWASP Mobile Top 10
  (2024): *M9 Insecure Data Storage* por la sesión heredada, y *M8 Security Misconfiguration* por el
  Auto Backup sin revisar, del que `allowBackup="true"` es el ejemplo de manual.
- El arranque normal no cambia: **RF-8 (SPEC-0001) sigue intacto**, cerrar y abrir la app mantiene la
  sesión.

**Negativas / deuda**

- **Tras reinstalar o cambiar de teléfono, el Historial local arranca vacío.** Los análisis no se
  pierden —están en Firestore— pero se llega a ellos desde Pacientes; no se repueblan solos. Es la
  consecuencia buscada, no un efecto colateral.
- También se pierden las preferencias locales no sensibles (tema, código de paciente recordado). Se
  acepta por simplicidad frente a una lista de exclusiones que habría que mantener.
- **La marca de instalación es un contrato implícito**: si algún día se limpiara `NSUserDefaults` por
  completo, el guard purgaría el Keychain y cerraría la sesión del usuario. Queda documentado en el
  propio fichero.
- `FreshInstallSessionGuard` **no tiene test**: vive en `iosMain` y el *source set* `iosTest` todavía no
  existe en el proyecto. Es el mismo hueco ya conocido, no una excepción abierta aquí.

## Verificación

La ejecuta el desarrollador (AGENTS.md §16):

1. `./gradlew :shared:ktlintCheck` y `./gradlew :shared:allTests`.
2. **iOS**: iniciar sesión → desinstalar → reinstalar → debe **pedir credenciales**.
3. **iOS**: cerrar y abrir la app con sesión iniciada → **sigue dentro** (RF-8, sin regresión).
4. **Android**: iniciar sesión → desinstalar → reinstalar → debe pedir credenciales, y el Historial
   local aparece vacío.
5. **Android**: comprobar que el Historial y las preferencias funcionan con normalidad en uso corriente.
6. Que el manifiesto fusione sin conflicto en `android:allowBackup` (ninguna librería lo declaraba, así
   que no debería hacer falta `tools:replace`).

## Referencias

- **SECURITY_MOBILE** §Tokens y credenciales, §Persistencia.
- **ADR-0006** (`KeychainTokenStore` y la sesión persistida en iOS) y **ADR-0002** (Desktop, sesión en
  memoria: allí este problema no existe).
- **SPEC-0001** RF-8 (la sesión sobrevive al reinicio) y **SPEC-0005** (los análisis viven en Firestore).
- Auto Backup y reglas de extracción: `https://developer.android.com/identity/data/autobackup`
- OWASP Mobile Top 10 — M9 Insecure Data Storage: `https://owasp.org/www-project-mobile-top-10/`
- AGENTS.md §8, §11, §13.
