# Resultados de pruebas de VialGuard

## Enlace entre dos Android (3 y 4 de noviembre de 2026)

Versión probada: `0.3-enlace` (versionCode 2, minSdk 26, targetSdk 34), APK de depuración con SHA-256 `f8b9b1c4b1d7dcff775441037f7a354237860c925eabe165f6e6983c220b9be8`.

### Entorno

- **Compilación:** JDK 17 (Temurin 17.0.20.1), Gradle 8.6, Android Gradle Plugin 8.4.1 y Kotlin 1.9.24.
- **Emuladores:** dos dispositivos virtuales oficiales con Android 14 (API 34, imagen Google APIs x86_64, pantalla de 720 × 1280 y 1,5 GB de RAM), sin ventana y con aceleración WHPX. `emulator-5554` hizo de monitor y `emulator-5556`, de estación.
- **Red entre emuladores:** dos emuladores no se ven entre sí. Se usó la redirección `adb -s emulator-5554 emu redir add tcp:5000:8888`, y la estación se conectó a `10.0.2.2:5000`. En dos teléfonos reales se usaría la IP que muestra el monitor.
- **Cuentas:** se crearon «monitor» y «estacion», con una contraseña de prueba que no está en el código ni en el repositorio.
- **Manejo de la app:** se usó adb (toques y texto) y se leyó la pantalla con uiautomator. Las capturas son `screencap` del propio emulador.

### Resultados

| Prueba | Resultado observado | Captura |
|---|---|---|
| Instalación limpia y cuentas | En ambos emuladores la app se instaló, se creó la cuenta local y se abrió la sesión. | — |
| Receptor del monitor | Mostró «Esperando estación en el puerto 8888.», las IP 10.0.2.15 y 10.0.2.17, el puerto 8888 y un código de 8 caracteres. | 14 |
| Vinculación | Con `10.0.2.2:5000` y el código escrito en minúsculas y con guion, la estación quedó «Conectada al monitor. Enviando lecturas.». El monitor mostró «Estación conectada.» y «Estado: Normal» con 100,0 cm. | 15, 16 |
| Precaución | Con 88,1 cm (variación de 11,9 cm), el monitor mostró «Estado: Precaución». | 17 |
| Alarma | Con 79,9 cm (variación de 20,1 cm), el monitor mostró «Estado: Alarma» en rojo, con «Reconocer alarma» habilitado y «Recalibrar» deshabilitado. | 18 |
| Reconocer alarma | La estación mostró «El monitor reconoció la alarma. La alarma sigue activa hasta recuperarse.». El monitor siguió en Alarma con «Alarma reconocida…», ambos botones quedaron deshabilitados y el historial registró «Orden enviada por monitor: Reconocer alarma.». | 19, 20 |
| Retención de 30 s | Al volver a 100 cm, el monitor siguió en Alarma con «Recuperación: 25 s» unos 5 s después. El historial registró la alarma a las 16:40:46 y el paso a Normal a las 16:41:57 (hora del emulador), unos 30 s después de la vuelta a 100 cm. | 21, 22 |
| Recalibrar sin alarma | En Normal el botón estaba habilitado, y la estación respondió «El monitor recalibró la referencia.». | — |
| Falla de sensor | Con «Simular falla de sensor» activado, la estación calculó Alarma y el monitor mostró «Estado: Alarma» y «Datos no válidos: falla del sensor.». | 23 |
| Corte del enlace | Con la estación en modo avión, el monitor mostró «Desconectado. Se perdió la conexión con la estación. Esperando reconexión.» y «Última lectura guardada, hace 13 s. Estado actual desconocido.». La estación mostró «Sin conexión con el monitor. Reintentando…». | 24, 25 |
| Reconexión | Al desactivar el modo avión, la estación volvió sola a «Conectada al monitor» en unos 10 s, y el monitor, a «Estación conectada.». | 26 |
| Cerrar sesión | Al cerrar sesión en la estación, el monitor mostró la última lectura como estado desconocido a los 4 s. Al volver a entrar, la estación indicaba «Enlace detenido.». Un intento de acceso con el usuario equivocado fue rechazado con «Usuario o contraseña incorrectos.». | 27 |
| Código incorrecto | La estación mostró «El monitor rechazó el código. Revísalo e intenta de nuevo.», y el monitor, «Se rechazó un intento con un código incorrecto.». | 28 |
| Cinco códigos incorrectos | Con un receptor recién iniciado, los cinco intentos fueron rechazados, de unos 5 s cada uno. Después del quinto, el monitor se detuvo con «Demasiados intentos con código incorrecto. Inicia el receptor para generar otro código.». | 29 |
| Historial | Registró la vinculación, los cambios de estado con sus valores, las órdenes con el usuario que las envió, los intentos rechazados y la detención del receptor. | 30 |
| Código nuevo | Al reiniciar el receptor apareció un código distinto del agotado. | — |
| Recorrido local | Estado con el filtro Puente, Detalle e Inicio con las pestañas Inicio, Estado y Enlace. | 31, 32, 33 |
| Fallas de la app | El registro de fallas (`logcat -b crash`) de ambos emuladores no mostró fallas de la app. | — |

### Defecto encontrado y corregido

En la primera pasada, el texto de los botones del enlace tomaba el color fijo de la tipografía del proyecto:

- «Reconocer alarma» quedaba con texto oscuro sobre fondo negro.
- «Recalibrar» deshabilitado se veía igual que habilitado.

En `LinkScreen.kt`, el texto de los botones ahora usa el color que define el propio botón. Después se repitió toda la prueba desde una instalación limpia; las capturas 14 a 33 corresponden a esa versión.

### Observaciones

- **Intentos de una prueba anterior:** el historial de la captura 30 incluye tres rechazos de una prueba previa, que se interrumpió. Esos intentos contaron para el código de entonces. Para la prueba de cinco intentos se reinició el receptor, lo que reinicia el contador con un código nuevo.
- **Tiempos:** los segundos indicados incluyen la demora de la herramienta que leía la pantalla.

### Alcance

- **Lo que se verificó:** los escenarios del enlace en dos emuladores de un mismo computador.
- **Diferencia con dos teléfonos:** la conexión pasó por una redirección de puerto y no por un punto de acceso Wi-Fi real, así que no se probaron el aislamiento entre clientes de una red real, el alcance ni el consumo.
- **Pendiente:** la prueba en dos teléfonos físicos en la misma red o en el hotspot de uno de ellos.
- **Pruebas unitarias:** las de seguridad del canal (mensaje alterado, repetido, código incorrecto y ausencia de texto en claro) están en `app/src/test`. No equivalen a una auditoría ni a una certificación ISO.

## Acceso y navegación (28 de octubre de 2026)

Pruebas manuales en el emulador oficial `VialGuard_API34`: Android 14, API 34, x86_64 y pantalla virtual de 720 × 1280. Corresponden a la versión formativa anterior, con minSdk 24. Se usó una cuenta local de prueba creada durante la revisión; la contraseña de prueba no está en el código ni en el APK.

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

Las capturas 01 a 13 de `evidencias/` son de esa versión. Las de navegación se tomaron durante la prueba de interfaz; después se corrigió la validación de campos vacíos y se repitieron las pruebas de acceso.

Esas pruebas no cubrieron:

- teléfono físico ni Android 7;
- transferencia o restauración de copias, pérdida de la clave ni registro corrupto;
- cambios del reloj;
- servicios Firebase.

No se acredita almacenamiento de claves respaldado por hardware ni certificación ISO.
