,### Autenticación y autorización

- Nunca implementar autenticación únicamente en cliente.
- Todas las autorizaciones deberán validarse siempre en el backend.
- No asumir que un usuario autenticado está autorizado para acceder a cualquier recurso.
- Las operaciones críticas deberán requerir autenticación y autorización.

---

### Tokens y credenciales

- Access Tokens: vida corta.
- Refresh Tokens: almacenamiento seguro.
- Android: Android Keystore o almacenamiento cifrado aprobado.
- iOS: Keychain.
- KMP: interfaz común en `commonMain` e implementación segura mediante `actual` por plataforma.
- Eliminar credenciales y datos sensibles al cerrar sesión.
- Nunca persistir contraseñas de usuario.

---

### Persistencia

- Persistir únicamente la información estrictamente necesaria.
- Cifrar únicamente la información sensible.
- Evitar almacenar datos sensibles cuando puedan obtenerse nuevamente desde el backend.

---

### Networking

- Timeouts explícitos en todas las conexiones.
- Manejo de errores centralizado.
- No exponer stack traces en producción.
- No aceptar certificados autofirmados en producción.
- Proteger los backends frente a SSRF cuando reciban URLs externas.
- No utilizar endpoints de desarrollo en builds de producción.

---

### Logging

- El nivel DEBUG nunca deberá estar habilitado en producción.
- Sanitizar cualquier dato antes de escribirlo en logs.
- No registrar respuestas completas de APIs si contienen información sensible.

---

### Configuración

- Separar claramente la configuración de Development, Staging y Production.
- Todas las claves deberán obtenerse mediante variables de entorno o mecanismos seguros de configuración.
- No incluir secretos dentro del repositorio.

---

### Dependencias

- Utilizar únicamente librerías mantenidas y con soporte activo.
- Revisar periódicamente vulnerabilidades (CVE) de las dependencias.
- Mantener actualizadas las librerías relacionadas con seguridad.

---

### Gestión de errores

- No capturar excepciones genéricas (`Exception`) salvo en puntos de entrada claramente justificados.
- No ignorar excepciones silenciosamente.
- Toda excepción recuperable deberá transformarse en un `Result`/`Either` o en un error de dominio.
- Reservar las excepciones para errores realmente excepcionales o irrecuperables.

---

### IA y privacidad

- No enviar PII salvo aprobación explícita.
- Minimizar el contexto enviado al proveedor de IA.
- Nunca incluir secretos, credenciales o tokens dentro de prompts.
- Documentar el proveedor, región, política de retención y finalidad del tratamiento.
- Separar claramente:
  - prompts del sistema,
  - instrucciones del agente,
  - datos proporcionados por el usuario.
- Validar siempre la salida de un LLM antes de utilizarla para ejecutar acciones críticas.

---

### Revisión de seguridad

Antes de finalizar cualquier cambio que afecte a:

- autenticación,
- autorización,
- almacenamiento,
- networking,
- criptografía,
- persistencia,
- integración con IA,
- gestión de credenciales,

el agente deberá verificar que el cambio cumple las recomendaciones aplicables de:

- OWASP Mobile Top 10.
- OWASP MASVS.
- Las políticas de seguridad definidas en este proyecto.

### Fuentes y estándares de referencia

- OWASP MASVS: https://mas.owasp.org/MASVS/
- OWASP Mobile Top 10: https://owasp.org/www-project-mobile-top-10/
