# Resultados de pruebas del avance VialGuard

Pruebas manuales realizadas el 28 de octubre de 2026, fecha local de Chile, en el emulador oficial de Android `VialGuard_API34`: Android 14, API 34, x86_64 y pantalla virtual 720 × 1280. Se usó una cuenta local de prueba creada durante la revisión. La contraseña de prueba no está incorporada al código ni al APK.

| Prueba | Resultado observado |
|---|---|
| Instalación y arranque | APK instalado; MainActivity abrió correctamente. |
| Contraste del acceso | Se corrigió el texto del botón y se comprobó su legibilidad. |
| Contraseña corta al crear cuenta | Rechazada con la política de diez caracteres, letras y números. |
| Creación de cuenta local | Permitió acceder a Inicio. |
| Inicio de sesión con cuenta guardada | Contraseña correcta aceptada. |
| Cierre de sesión | Volvió al acceso; dejó de mostrar las pantallas privadas. |
| Reinicio del proceso | Conservó la cuenta y volvió a exigir autenticación. |
| Credenciales vacías | Se corrigió el error técnico inicial. La repetición muestra «Ingresa tu usuario y contraseña». |
| Contraseña incorrecta no vacía | Rechazada con «Usuario o contraseña incorrectos». |
| Cinco intentos incorrectos | Activaron el bloqueo; incluso la contraseña correcta fue rechazada durante el período de bloqueo. |
| Reintento después del bloqueo | Tras esperar más de un minuto desde el quinto fallo, la contraseña correcta volvió a permitir el acceso a Inicio. |
| Inicio y Estado | Ambas pestañas respondieron y las tarjetas fueron accesibles mediante desplazamiento. |
| Filtro Puente | Mostró solo el puente de demostración. |
| Detalle y regreso | Mostró 100 cm y 0 grados como ejemplos y permitió volver a Estado. |
| Búsqueda sin coincidencias | Mostró «No hay estructuras con ese filtro». |
| Almacenamiento local | SharedPreferences contiene `record` e `iv`; el usuario y la contraseña de prueba no aparecen en claro. El IV decodificado tiene doce bytes. No se publicaron el registro ni su verificador. |
| Registro AndroidRuntime | No se observaron excepciones fatales en la consulta del registro durante las pruebas realizadas. |

Las capturas reales están en `evidencias/`. Las de navegación se tomaron durante la prueba de interfaz; después se corrigió la validación de campos vacíos y se repitieron las pruebas de acceso. El flujo de navegación no se modificó con esa corrección.

## Alcance de la comprobación

Las pruebas verifican estos recorridos locales en un emulador, no todos los posibles fallos ni todos los dispositivos. No se probaron en teléfono físico, Android 7, transferencia/restauración de copias, pérdida de la clave, registro corrupto, cambios del reloj, comunicación IoT ni servicios Firebase. No se acredita almacenamiento de claves respaldado por hardware ni certificación ISO. La presentación y la validación del docente siguen pendientes.
