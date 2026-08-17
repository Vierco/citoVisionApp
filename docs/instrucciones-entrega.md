# Instrucciones de instalación — citoVision

Guía para instalar y ejecutar citoVision a partir de los paquetes de entrega. citoVision se distribuye para
**macOS** (fichero `.dmg`) y **Android** (fichero `.apk`).

> **Nota:** citoVision es un prototipo académico y experimental. Los paquetes **no están firmados con un
> certificado comercial de tienda**, por lo que el sistema operativo puede mostrar avisos de seguridad al ser
> la primera vez que se instala una app fuera de la App Store / Google Play. Los pasos siguientes explican cómo
> abrirla con normalidad.

---

## macOS (fichero `.dmg`)

**Requisitos:** un Mac con macOS reciente (Intel o Apple Silicon).

Al abrir la app por primera vez, macOS (Gatekeeper) mostrará un aviso del tipo *"no se puede abrir porque Apple
no puede comprobar si contiene software malicioso"* o *"está dañada"*. Es lo esperado en una app sin notarizar;
se resuelve en un minuto.

### Pasos

1. Haz doble clic en `citoVision.dmg` para montarlo.
2. **Arrastra `citoVision` a la carpeta `Aplicaciones`** (o al Escritorio).
3. Abre **Terminal** (Aplicaciones → Utilidades → Terminal).
4. Escribe lo siguiente, **con un espacio al final**, sin pulsar Enter todavía:
   ```
   xattr -cr 
   ```
5. **Arrastra el icono de `citoVision`** (el que copiaste a Aplicaciones) **sobre la ventana de Terminal**: la
   ruta se rellenará sola. Debería quedar algo como:
   ```
   xattr -cr /Applications/citoVision.app
   ```
6. Pulsa **Enter**.
7. Haz **doble clic** en `citoVision`. Ya se abre con normalidad.

### Alternativa (sin Terminal)

1. Haz doble clic en `citoVision` (dará el aviso; ciérralo).
2. Ve a **Ajustes del Sistema → Privacidad y seguridad**.
3. Baja hasta el mensaje sobre citoVision y pulsa **"Abrir de todos modos"**.
4. Confirma. La próxima vez abrirá con doble clic directo.

> En **macOS Sequoia (15) y posteriores**, el antiguo truco de *clic derecho → Abrir* ya no funciona: usa el
> comando `xattr -cr` o la opción **"Abrir de todos modos"** de Privacidad y seguridad.

---

## Android (fichero `.apk`)

**Requisitos:** un dispositivo Android.

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

## Primer uso

- Puedes entrar en **modo invitado** para probar la aplicación sin cuenta.
- Las cuentas de usuario las gestiona **Lovelaced**; si necesitas una, contacta con el equipo.
- Recuerda: citoVision **prioriza, no diagnostica**. No es un producto sanitario ni sustituye el criterio de un
  profesional.
