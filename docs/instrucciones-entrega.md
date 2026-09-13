# Instrucciones de instalación — citoVision

Guía para instalar y ejecutar citoVision a partir de los paquetes de entrega. citoVision se distribuye para
**macOS** (`.dmg`), **Windows** (`.zip`), **Android** (`.apk`) e **iOS** (por TestFlight).

citoVision se distribuye **bajo solicitud**: si tienes este documento es porque has recibido el paquete de tu
plataforma. Para cualquier duda, o para pedir otra plataforma, escribe a
**[hola@citovision.app](mailto:hola@citovision.app)**.

> **Nota:** citoVision es un prototipo académico y experimental. Los paquetes **no están firmados con un
> certificado comercial de tienda**, por lo que el sistema operativo puede mostrar avisos de seguridad al ser
> la primera vez que se instala una app fuera de la App Store / Google Play / Microsoft Store. Los pasos
> siguientes explican cómo abrirla con normalidad.

---

## macOS (fichero `.dmg`)

**Requisitos:** un Mac con macOS reciente (Intel o Apple Silicon).

Al abrir la app por primera vez, macOS (Gatekeeper) mostrará un aviso del tipo *"no se puede abrir porque Apple
no puede comprobar si contiene software malicioso"* o *"está dañada"*. Es lo esperado en una app sin notarizar;
se resuelve en un minuto.

### Pasos

1. Haz doble clic en `citoVision.dmg` para montarlo.
2. **Arrastra `citoVision` a la carpeta `Aplicaciones`** (o al Escritorio).
3. Haz doble clic en `citoVision`. Saldrá el aviso: ciérralo.
4. Ve a **Ajustes del Sistema → Privacidad y seguridad**.
5. Baja hasta el mensaje sobre citoVision y pulsa **"Abrir de todos modos"**.
6. Confirma. La próxima vez abrirá con doble clic directo.

> En **macOS 14 (Sonoma) y anteriores** hay un atajo más corto: **clic derecho sobre la app → Abrir**. En
> **macOS 15 (Sequoia) y posteriores ese atajo ya no funciona** para aplicaciones sin notarizar — Apple lo
> retiró—, así que hay que usar "Abrir de todos modos" o el comando de la sección siguiente.

### Alternativa con Terminal (vale en todas las versiones)

1. Abre **Terminal** (Aplicaciones → Utilidades → Terminal).
2. Escribe lo siguiente, **con un espacio al final**, sin pulsar Enter todavía:
   ```
   xattr -cr 
   ```
3. **Arrastra el icono de `citoVision`** (el que copiaste a Aplicaciones) **sobre la ventana de Terminal**: la
   ruta se rellenará sola. Debería quedar algo como:
   ```
   xattr -cr /Applications/citoVision.app
   ```
4. Pulsa **Enter** y abre la app con doble clic.

---

## Windows (fichero `.zip`)

**Requisitos:** Windows 10 o superior, de 64 bits.

**No se instala.** Es una carpeta que se ejecuta tal cual: lleva su propio entorno de ejecución dentro, así que
no hace falta instalar Java ni permisos de administrador.

### Pasos

1. Antes de descomprimir: **clic derecho sobre el `.zip` → Propiedades** y, si aparece la casilla
   **"Desbloquear"**, márcala y acepta. Esto evita el aviso del paso 3.
2. Descomprime en una ruta corta, por ejemplo `C:\citoVision`.
3. Ejecuta **`citoVision.exe`**. Si aparece *"Windows protegió su PC"*, pulsa **Más información → Ejecutar de
   todas formas**.

> El aviso sale porque el ejecutable no está firmado con un certificado comercial, no porque haya ningún
> problema con la aplicación. El razonamiento completo está en
> [ADR-0011](adr/0011-soporte-windows-desktop.md).

---

## Android (fichero `.apk`)

**Requisitos:** Android 8.1 (API 27) o superior.

Como la app no se instala desde Google Play, hay que **permitir la instalación de orígenes desconocidos** una
sola vez.

### Pasos

1. Copia el fichero `.apk` al teléfono (por cable, correo, o descárgalo desde el enlace).
2. Abre el `.apk` con el explorador de archivos.
3. Android pedirá permiso para **instalar apps de esta fuente**: acéptalo (te llevará a los ajustes para
   activarlo; luego vuelve atrás).
4. Pulsa **Instalar**.
5. Abre **citoVision** desde el cajón de aplicaciones.

> Si aparece un aviso de Play Protect, elige **"Instalar de todos modos"**: es habitual en apps que no proceden
> de la tienda.

---

## iOS (por TestFlight)

**Requisitos:** un iPhone con iOS 18.6 o superior.

iOS no permite instalar aplicaciones fuera de su tienda: un iPhone solo ejecuta apps firmadas por un perfil que
lo autorice, así que **no existe un equivalente al APK suelto**. La versión de iOS se reparte por **TestFlight**,
el canal de betas de Apple. No hacen falta ni Mac ni Xcode.

### Pasos

1. Instala la app **TestFlight** desde la App Store.
2. Recibirás una **invitación por correo** en la dirección con la que solicitaste el acceso; ábrela desde el
   iPhone.
3. Acepta la invitación e instala citoVision desde TestFlight.

> Dos límites propios del canal de Apple: **las builds caducan a los 90 días** de publicarse, y el grupo de
> pruebas tiene un aforo limitado. Si el enlace dice que no hay plazas o que la build ha expirado, hay que
> pedir una nueva. El razonamiento completo y las alternativas descartadas están en
> [ADR-0010](adr/0010-distribucion-ios-testflight.md).

---

## Primer uso

- Puedes entrar en **modo invitado** para probar la aplicación sin cuenta.
- Iniciar sesión (con Google, o con correo y contraseña) añade la pantalla **Pacientes**, que asocia los
  análisis a un código de paciente y los sincroniza entre dispositivos. Las cuentas de correo y contraseña las
  gestiona la desarrolladora; si necesitas una, escribe al mismo correo.
- Para probarlo necesitas imágenes de frotis sanguíneo al microscopio, que no son fáciles de conseguir. Con
  el paquete se incluye un conjunto de muestras; si no lo tienes, pídelo al mismo correo.
- En **Ajustes → Origen de las imágenes** eliges si el selector abre la galería de fotos o el explorador de
  archivos. Si has descargado las muestras como ficheros, cambia esa opción a **Archivos** o el selector
  aparecerá vacío.
- Recuerda: citoVision **prioriza, no diagnostica**. No es un producto sanitario ni sustituye el criterio de un
  profesional.
