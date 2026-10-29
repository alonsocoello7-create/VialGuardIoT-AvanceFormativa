# VialGuard IoT avance formativo

Esta es la versión reducida para la formativa. Conserva un recorrido local de Login, Inicio, Estado y Detalle, búsqueda y filtro de estructuras. Las lecturas y los estados son ejemplos académicos. En la primera apertura se crea una cuenta local con una contraseña elegida por el usuario. No autentica contra Firebase.

## Seguridad implementada

La contraseña no se guarda. Se deriva un verificador PBKDF2 con sal aleatoria, 600 000 iteraciones y comparación de tiempo constante. En Android 8 o superior se utiliza HMAC-SHA256; en Android 7 se utiliza HMAC-SHA1 por compatibilidad de la plataforma. El registro con verificador y sal se cifra con AES-GCM, mediante una clave creada y administrada por Android Keystore. SharedPreferences contiene el registro cifrado y el IV, no la contraseña ni una clave AES escrita en el código.

La creación exige una contraseña de al menos diez caracteres con letras y números. Cinco intentos incorrectos producen un bloqueo local de un minuto, registrado dentro de los datos cifrados. Los errores al descifrar impiden el acceso. La sesión se conserva en memoria, Cerrar sesión la elimina y reiniciar el proceso requiere volver a entrar. Las pantallas de consulta solo se muestran después de autenticar. El manifiesto deshabilita las copias de seguridad y el tráfico HTTP en texto claro.

Estos son controles concretos del avance, no una certificación de cumplimiento completo de ISO/IEC 27400. No hay roles administrativos ni comunicación inalámbrica implementada en esta versión.

En Android 12 o superior, `data_extraction_rules.xml` excluye explícitamente SharedPreferences de las copias en nube y de las transferencias entre dispositivos. Se conserva `allowBackup=false` y se establece `fullBackupContent=false`. Estas reglas aún requieren comprobarse en dispositivo.

## Firebase en esta entrega

Se agregaron las bibliotecas oficiales `firebase-auth`, `firebase-firestore` y `firebase-database`, coordinadas por Firebase BoM. Se declaró el complemento Google Services en Gradle. Solo se aplica al módulo cuando existe un `app/google-services.json` real, por lo que el avance local no depende de tener ese archivo para arrancar.

Agregar bibliotecas no demuestra una conexión a Firebase. Este programa todavía no inicia sesión en Firebase ni escribe o recibe datos de sus bases. La configuración y el uso se explican en `EXPLICACION_PARA_LA_FORMATIVA.md`.

## Funciones retiradas del avance

No incluye historial avanzado, registro/eliminación de incidentes, reconocimiento de alertas, informes, reinicio de estructuras, sincronización HTTP con ESP32, sockets entre teléfonos ni funciones administrativas. Tampoco muestra un indicador que afirme conexión activa a sensores.

Se conservaron los colores y la tipografía del proyecto recibido. `MainActivity.kt` contiene la interfaz, `AvanceViewModel.kt` administra el estado de acceso y `LocalCredentialStore.kt` protege las credenciales locales. La aplicación original en la carpeta `lot (1)` no se modificó.

## Abrir y probar

1. Abrir esta carpeta en Android Studio y sincronizar Gradle.
2. Elegir un emulador o teléfono Android y ejecutar la app.
3. En la primera apertura, crear una cuenta local. El nombre sugerido es `operador`; la contraseña la elige el usuario y debe cumplir la política mostrada. No existe una contraseña predefinida.
4. Recorrer Inicio, Estado y Detalle. Probar la búsqueda, los filtros y Cerrar sesión.
5. Cerrar sesión y comprobar contraseña incorrecta y correcta. Probar cinco intentos incorrectos, esperar un minuto y verificar que permita reintentar. No usar datos ni contraseñas reales para la demostración académica.
6. Revisar las capturas reales del emulador en `evidencias/` y `RESULTADOS_PRUEBAS.md`. Repetir la demostración en el teléfono que se utilizará en clase.

El archivo `local.properties`, si existe en la copia de trabajo, corresponde al SDK instalado en el computador de preparación. No se incluye en el paquete compartible. Android Studio debe crear su propio archivo para el SDK del equipo que abra el proyecto.

## Versiones

Se conserva Kotlin 1.9.24 y la configuración Compose recibida. Se fija Firebase BoM 33.1.2 para esa base del proyecto, con bibliotecas principales y sin módulos KTX adicionales. El wrapper usa Gradle 8.6 para Android Gradle Plugin 8.4.1. No se afirma que sean las versiones más recientes.

## Alcance de la evaluación

La guía general contempla una aplicación con interacción entre dos Android. Para esta formativa, el alcance acordado incluye seguridad implementada, bibliotecas Firebase y justificación de su uso, y Bluetooth/Wi-Fi solo explicados. El avance se ejecutó y se probó en un emulador oficial Android 14, con capturas reales y resultados en `RESULTADOS_PRUEBAS.md`. Quedan pendientes la comprobación en el teléfono de clase, la presentación y la validación ante el docente. La comunicación real y los controles de seguridad de una futura integración en nube corresponden a una etapa posterior; no se declaran implementados ni demostrados.

Código publicado: https://github.com/alonsocoello7-create/VialGuardIoT-AvanceFormativa
