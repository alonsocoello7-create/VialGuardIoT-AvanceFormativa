# VialGuard: app con enlace entre dos Android

Esta carpeta contiene el proyecto Android de la Unidad 2, versión `0.3-enlace`. El APK de prueba se genera en `app/build/outputs/apk/debug/app-debug.apk`; su huella está en `VERIFICACION.txt`.

## Qué incluye

- **Consulta local:** acceso local, Inicio, Estado, Detalle, búsqueda y filtros, con datos académicos de demostración.
- **Enlace Wi-Fi entre dos Android:**
  - un teléfono es el monitor: recibe lecturas, muestra el estado, envía órdenes y guarda un historial;
  - el otro es la estación: simula los sensores de la maqueta;
  - el canal va cifrado y exige un código de vinculación.
- **Seguridad de la cuenta:** verificador PBKDF2 con sal, registro cifrado con una clave de Android Keystore, bloqueo temporal y cierre de sesión, que también detiene el enlace.
- **Firebase:** las bibliotecas Authentication, Firestore y Realtime Database están agregadas y explicadas, pero la app todavía no las usa.
- **Comparación de tecnologías:** Wi-Fi, Bluetooth y MQTT se comparan con valores típicos y su fuente.
- **ESP32:** no transmite a la app; las lecturas del enlace son simuladas.

## Cómo empezar

1. **Revisar el código:** abrir esta carpeta en Android Studio y esperar la sincronización de Gradle.
2. **Probar la app:** instalar el APK en dos teléfonos de prueba con Android 8 o superior. No instalar en el mismo teléfono la app anterior `VialGuard.apk`, porque comparten paquete.
3. **Crear las cuentas:** en la primera apertura de cada teléfono, crear una cuenta local con una contraseña de al menos diez caracteres, con letras y números. No hay contraseña predefinida.
4. **Probar el enlace:** seguir «Demostración con dos teléfonos» de `ENLACE_WIFI.md` y después la lista de `RUBRICA_Y_PRUEBAS.md`.
5. **Revisar las evidencias:** `evidencias/` y `RESULTADOS_PRUEBAS.md` tienen las capturas y los resultados reales. Antes de la clase, repetir la demostración en los teléfonos que se usarán.

## Qué leer para explicar el trabajo

| Archivo | Contenido |
|---|---|
| `ENLACE_WIFI.md` | Protocolo del enlace, por qué Wi-Fi local, límites de seguridad y de uso, y demostración |
| `EXPLICACION_PARA_LA_FORMATIVA.md` | Problemática, herramientas, Firebase, conexiones y seguridad |
| `RUBRICA_Y_PRUEBAS.md` | Correspondencia con los quince criterios y pruebas para los teléfonos |
| `MOCKUP_Y_NAVEGACION.md` | Bosquejo de pantallas, incluida la pestaña Enlace |
| `RESULTADOS_PRUEBAS.md` y `evidencias/` | Pruebas y capturas reales, de octubre y de noviembre |
| `VERIFICACION.txt` | Compilación, pruebas unitarias, Lint y APK |
| `COMPARACION_EJEMPLO_PROFESOR.md` y `REVISION_FINAL.md` | Revisiones de la versión formativa de octubre; tienen una nota al inicio sobre lo que cambió |

## Estado comprobado

- **Compilación:** el proyecto compila, las 25 pruebas unitarias pasan, Lint no informa errores y la firma del APK está verificada.
- **Emuladores:** con dos emuladores oficiales de Android 14 se comprobaron:
  - la vinculación;
  - Normal, Precaución y Alarma;
  - Reconocer y Recalibrar;
  - la retención de 30 s;
  - la falla de sensor;
  - el corte y la reconexión;
  - el cierre de sesión;
  - el bloqueo tras cinco códigos erróneos.
- **Pendiente:** la prueba en teléfonos físicos y la aprobación del docente.

Esta entrega no acredita por sí sola el cumplimiento final de la rúbrica.

## Cómo pide entregar la guía

La guía TI3042 indica subir a AAI un archivo de texto con el enlace a un repositorio de GitHub que contenga el código.

El código está en el repositorio público https://github.com/alonsocoello7-create/VialGuardIoT-AvanceFormativa. Esta versión se subió el 4 de noviembre de 2026, y la de octubre sigue disponible en su historial. El archivo de texto para AAI debe contener ese enlace. La carga en AAI la hace el estudiante y todavía no se ha realizado.

El repositorio no debe incluir `.gradle`, `.idea`, `build` ni `local.properties`.
