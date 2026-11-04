# Correspondencia del avance con la rúbrica

La guía de la unidad pide una aplicación que permita interactuar al menos a dos dispositivos Android por Wi-Fi o Bluetooth. Esta versión incluye:

- acceso local y consulta de estructuras de ejemplo, con seguridad implementada;
- bibliotecas Firebase, que todavía no se usan;
- un enlace Wi-Fi entre dos Android, en el que un teléfono hace de monitor y el otro de estación.

La tabla resume la evidencia de cada criterio. No acredita por sí sola el cumplimiento final, que depende de la presentación y de la evaluación del docente.

| Criterio | Evidencia y pendientes |
|---|---|
| 2.1.1.1 | La problemática vial, los sensores, el ESP32 y los dos teléfonos están identificados en `EXPLICACION_PARA_LA_FORMATIVA.md`. |
| 2.1.1.2 | Funciones: acceso local, Inicio, Estado, Detalle, búsqueda, filtros y Enlace (monitor con órdenes e historial; estación con sensores simulados). Las funciones futuras se presentan aparte. |
| 2.1.1.3 | Se justifican Kotlin, Android Studio, Compose, Material 3, ViewModel y sockets TCP con la criptografía de Android. |
| 2.1.1.4 | La propuesta está documentada. Faltan la presentación y la retroalimentación del docente. |
| 2.1.2.5 | Se eligió Wi-Fi local frente a Firebase, Bluetooth y MQTT, comparando distancia, consumo y estabilidad con valores típicos y su fuente (`claude_ENLACE_WIFI.md` y `EXPLICACION_PARA_LA_FORMATIVA.md`). |
| 2.1.2.6 | Bosquejo con la pestaña Enlace en `MOCKUP_Y_NAVEGACION.md` y capturas reales de la versión con enlace en `evidencias/` (14 a 33). Falta la validación del docente. |
| 2.1.2.7 | Falta validar la interfaz y la autenticación ante el docente. |
| 2.1.3.8 | El proyecto Kotlin compila y aprueba 25 pruebas unitarias de las reglas de alarma, el canal cifrado y el enlace por TCP. |
| 2.1.3.9 | El manifiesto de la app declara solo INTERNET, que usa el enlace. No pide permisos de Bluetooth, ubicación ni exploración Wi-Fi. Las bibliotecas Firebase y AndroidX agregan otros permisos al APK por fusión de manifiestos; ver `README.md`. |
| 2.1.3.10 | Controles: cuenta local con PBKDF2 y sal, registro AES-GCM con Android Keystore, bloqueo temporal, y un canal del enlace con ECDH, PBKDF2 y AES-GCM que se bloquea tras 5 códigos erróneos. Falta una evaluación completa de ISO/IEC 27400. |
| 2.1.3.11 | `RESULTADOS_PRUEBAS.md` registra las pruebas de acceso (28 de octubre) y las del enlace en dos emuladores (3 y 4 de noviembre). Un defecto de contraste encontrado se corrigió y se volvió a probar. Faltan los teléfonos físicos. |
| 2.1.4.12 | Falta la demostración en clase con dos teléfonos. `claude_ENLACE_WIFI.md` tiene el guion. |
| 2.1.4.13 | Las decisiones y sus límites se explican en `EXPLICACION_PARA_LA_FORMATIVA.md` y `claude_ENLACE_WIFI.md`. |
| 2.1.4.14 | Hay controles en el código, pruebas unitarias del canal (mensaje alterado, repetido, código incorrecto y ausencia de texto en claro) y capturas de código rechazado y de receptor bloqueado. Falta la demostración ante el docente. |
| 2.1.4.15 | Faltan las preguntas, la retroalimentación y los ajustes finales con el docente. |

## Verificación realizada

Se compiló con JDK 17, Gradle 8.6 y Android Gradle Plugin 8.4.1.

- **Pruebas unitarias:** pasaron las 25.
- **APK:** se generó y se firmó con la clave de depuración.
- **Android Lint:** terminó sin errores.

`VERIFICACION.txt` tiene las cifras de la última compilación.

El enlace se probó con dos emuladores oficiales de Android 14. Los resultados, las capturas y los límites de esa prueba están en `RESULTADOS_PRUEBAS.md`.

Compilar y probar en emuladores no reemplaza la prueba en los teléfonos de clase, que se describe a continuación.

## Pruebas para los teléfonos

1. **Instalación:** instalar el APK en dos teléfonos con Android 8 o superior. Si el teléfono tiene otra app con el paquete `cl.inacap.vialguard` (por ejemplo, `VialGuard.apk` de la versión «lot»), Android rechazará la instalación por firma distinta. No desinstalar una app con datos importantes sin respaldarlos.
2. **Cuenta:** crear una cuenta local en cada teléfono. Comprobar que se rechaza una contraseña corta.
3. **Recorrido local:** pasar por Inicio, Estado, la búsqueda, los filtros y Detalle.
4. **Sesión:**
   1. Cerrar sesión y confirmar que las pantallas privadas vuelven a pedir acceso.
   2. Probar una contraseña incorrecta y luego la correcta.
   3. Con cinco intentos incorrectos, comprobar el bloqueo de un minuto.
5. **Red:** conectar ambos teléfonos a la misma red. Si el Wi-Fi del instituto aísla a los clientes, usar el hotspot de uno de ellos.
6. **Vincular:**
   1. En el teléfono A: Enlace → Monitor → Iniciar receptor.
   2. En el teléfono B: Enlace → Estación, escribir la IP y el código de A y pulsar Conectar.
7. **Estados y órdenes:**
   1. Mover la distancia a unos 88 cm para ver Precaución y a unos 80 cm para ver Alarma.
   2. Pulsar Reconocer y comprobar que la alarma sigue activa.
   3. Comprobar que Recalibrar está deshabilitado durante la alarma.
   4. Volver a 100 cm y esperar los 30 s.
8. **Falla:** activar «Simular falla de sensor» en B y comprobar la alarma en A.
9. **Corte:** apagar el Wi-Fi de B. Comprobar que A muestra «Última lectura guardada… Estado actual desconocido» y que B reconecta sola al volver la red.
10. **Códigos:**
    1. Probar un código incorrecto en B y comprobar el rechazo.
    2. Con cinco intentos incorrectos, comprobar que A detiene el receptor.
    3. Iniciar el receptor de nuevo y comprobar que muestra otro código.
11. **Registro:** anotar el resultado real y tomar las capturas que pida el docente. Si algo falla, registrarlo y corregirlo antes de la demostración.

No existe una contraseña predefinida y no se usa una cuenta de Firebase. Los estados de Inicio, Estado y Detalle son ejemplos; las lecturas del enlace las simula la estación.
