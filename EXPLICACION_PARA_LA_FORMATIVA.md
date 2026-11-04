# Explicación del avance y las conexiones

## Problema y dispositivos

VialGuard muestra en una aplicación Android información de una maqueta de monitoreo vial. La solución usa un HC-SR04 para medir distancia y un MPU-6050 para medir inclinación, ambos conectados al ESP32.

La guía pide que al menos dos dispositivos Android interactúen por Wi-Fi o Bluetooth. En esta versión lo hacen así:

- **Estación:** un teléfono simula los sensores de la maqueta y envía lecturas.
- **Monitor:** el otro teléfono las recibe y envía órdenes.

El ESP32 todavía no transmite a la app; las lecturas del enlace son simuladas.

Además, la aplicación permite crear una cuenta local protegida, iniciar sesión, consultar estructuras de ejemplo, buscar, filtrar y abrir su detalle. Las funciones de nube se documentan como trabajo posterior.

## Herramientas

- **Desarrollo:** Android Studio y Kotlin corresponden a la guía de evaluación. Jetpack Compose y Material 3 permiten construir la interfaz en Kotlin, y los ViewModel separan el estado de cada pantalla.
- **Enlace:** usa sockets TCP y la criptografía incluida en Android, sin bibliotecas de red adicionales.
- **Firebase:** la guía de Firebase usa un ejemplo PulseHome hecho con XML, mientras que VialGuard usa Compose. Por eso el registro de la app y las dependencias de Firebase se adaptaron al paquete `cl.inacap.vialguard`.

## Para qué se agregan las bibliotecas Firebase

| Biblioteca | Uso previsto |
|---|---|
| firebase-auth | Identificar al usuario mediante Firebase Authentication en una etapa posterior. El inicio de sesión local de este avance no la usa. |
| firebase-firestore | Almacenar y consultar documentos, por ejemplo datos de estructuras o perfiles. |
| firebase-database | Explorar la sincronización de lecturas con Realtime Database, siguiendo el ejemplo de la guía. |
| Firebase BoM | Mantener compatibles las versiones de las bibliotecas Firebase agregadas. |

Incluir ambas bibliotecas de bases de datos no obliga a duplicar los datos. Antes de programar la integración se definirá cuál almacena cada tipo de información. Esta entrega no implementa esa decisión ni sustituye al servidor Django previsto anteriormente. El enlace entre teléfonos no usa Firebase.

## Conexiones y comunicación

Los sensores se conectan por cables al ESP32: el HC-SR04 usa TRIG y ECHO, y el MPU-6050 usa I2C. Estos cables no llegan al teléfono.

**Comunicación implementada entre dos Android:**

1. **Red:** ambos teléfonos se conectan a la misma red Wi-Fi, que puede ser el hotspot de uno de ellos.
2. **Monitor:** abre un receptor TCP en el puerto 8888 y muestra su IP y un código de vinculación.
3. **Estación:** se conecta con esos datos y envía una lectura simulada por segundo.
4. **Respuesta:** el monitor responde a cada lectura con un latido y puede enviar las órdenes «Reconocer alarma» y «Recalibrar».

El enlace no necesita Internet. `claude_ENLACE_WIFI.md` detalla el protocolo, los tiempos y las reglas.

**Conceptos:**

- **Wi-Fi:** es el acceso a una red.
- **Bluetooth:** permite un enlace inalámbrico de proximidad.
- **Firebase:** es un servicio en la nube.
- **MQTT:** es un protocolo de mensajería que necesita una red y un broker.
- **Firebase Authentication:** identifica usuarios, pero por sí sola no comparte lecturas.

Más adelante, el origen simulado puede reemplazarse por el ESP32, que también tiene Wi-Fi.

| Alternativa | Distancia, consumo y estabilidad |
|---|---|
| Wi-Fi local con TCP (elegida) | **Distancia:** la cobertura del punto de acceso o del hotspot. Como referencia, un fabricante declara de 23 a 46 m en interiores a través de 3 o 4 muros para un punto de acceso de oficina (Datto). **Consumo:** usa la radio Wi-Fi del teléfono, y el enlace envía un mensaje pequeño por segundo en cada sentido. En el ESP32, transmitir por Wi-Fi consume de 180 a 240 mA y recibir, de 95 a 100 mA (Espressif). **Estabilidad:** TCP entrega los datos en orden; la app detecta 5 s de silencio y reintenta cada 2 s. |
| Wi-Fi e Internet con Firebase | **Distancia:** sin límite mientras ambos teléfonos tengan Internet. **Consumo:** el de Wi-Fi o de los datos móviles. **Estabilidad:** depende de Internet y del servicio. Queda como opción futura porque el proyecto no tiene `google-services.json`. |
| Bluetooth | **Distancia:** de menos de 1 m a más de 1 km, según PHY, potencia, sensibilidad, antena y obstáculos (Bluetooth SIG). **Consumo:** BLE está diseñado para consumir significativamente menos que Bluetooth clásico (Android Developers). En el ESP32, transmitir a 0 dBm consume 130 mA y recibir, de 95 a 100 mA (Espressif). **Estabilidad:** requiere emparejamiento y permisos en tiempo de ejecución desde Android 12. |
| MQTT sobre red IP | Mensajería mediante publicadores, suscriptores y un broker. Necesita una red y un servidor adicional, y no reemplaza a Wi-Fi. Se mantiene como alternativa de telemetría para el ESP32. |

**Por qué Wi-Fi local:**

- la guía pide interacción por Wi-Fi o Bluetooth entre dos Android;
- la demostración ocurre en una misma sala;
- no requiere Internet, servidor ni emparejamiento;
- la radio Wi-Fi ya está activa en los teléfonos.

**Sobre las cifras:** son valores típicos de sus fuentes, no mediciones de estos equipos. El consumo de los teléfonos no se midió.

**Permisos:**

- **Manifiesto de la app:** declara solo INTERNET, que el enlace usa para sus sockets. Es un permiso normal, que Android concede al instalar.
- **Bibliotecas:** las de Firebase y AndroidX agregan otros permisos al APK por fusión de manifiestos (ver README).
- **No se piden:** permisos de Bluetooth, ubicación ni exploración de redes, porque la app no busca redes ni dispositivos.

## Configuración de Firebase que se explicará

La guía indica estos pasos:

1. Crear el proyecto.
2. Registrar la app con su applicationId exacto.
3. Colocar `google-services.json` en `app`.
4. Declarar Google Services y las dependencias.
5. Habilitar los proveedores.
6. Configurar las bases y sus reglas.

Este avance solo muestra las bibliotecas agregadas y justifica esos pasos; no presenta la conexión como terminada.

Durante la preparación:

- se creó el proyecto `vialguard-ac2f1` en el plan Spark;
- se registró la app Android;
- se habilitó el acceso con correo y contraseña;
- se creó Firestore en nam5 con reglas privadas.

Esa configuración quedó adelantada, pero no demuestra que la app ya se conecte. No se creó Realtime Database ni se habilitó el acceso con Google.

## Seguridad implementada y explicación

**Cuenta y sesión:**

- **Autenticación:** es local, con un verificador PBKDF2 con sal aleatoria.
- **Registro:** se cifra con AES-GCM, con la clave administrada por Android Keystore.
- **Política:** exige una contraseña mínima y limita los intentos.
- **Acceso:** las pantallas y el enlace solo están disponibles con sesión, y la app permite cerrar sesión.
- **Copias:** se deshabilitan las copias de seguridad y el tráfico HTTP en texto claro.
- **Credenciales predefinidas:** no hay ninguna.

**Canal del enlace:**

- claves ECDH efímeras;
- código de vinculación derivado con PBKDF2;
- AES-256-GCM con contador, que rechaza mensajes alterados o repetidos;
- bloqueo tras cinco códigos erróneos.

**Reglas de las órdenes remotas:**

- Reconocer una alarma no la apaga.
- Recalibrar se rechaza durante una alarma.

Los detalles están en `LocalCredentialStore.kt`, `AvanceViewModel.kt`, `SecureChannel.kt` y `claude_ENLACE_WIFI.md`.

**Límites:**

- el canal no usa certificados;
- el código tiene unos 40 bits;
- no hay roles.

Para la futura nube se explicarán la autenticación gestionada, el transporte cifrado, la autorización por usuario y la validación en el servidor. No se declara certificación ni cumplimiento completo de ISO/IEC 27400.

## Qué mostrar al docente

1. **Propuesta:** presentar el problema y las pantallas.
2. **Bibliotecas y comunicación:** explicar las bibliotecas agregadas y por qué se eligió Wi-Fi local.
3. **Demostración:** con dos teléfonos en la misma red o en el hotspot de uno de ellos:
   - vinculación con el código;
   - lecturas en vivo;
   - Precaución y Alarma;
   - Reconocer y Recalibrar;
   - corte y reconexión.
4. **Preguntas:** responder qué partes todavía no están conectadas, como el ESP32 y Firebase.
5. **Retroalimentación:** registrar la del docente y hacer los ajustes que indique. La revisión y la aceptación del docente no se inventan.

Las pruebas realizadas, con sus condiciones y capturas, están en `RESULTADOS_PRUEBAS.md` y en `evidencias/`. La prueba en teléfonos físicos y la validación del docente siguen pendientes.

## Referencias

- TI3042_U2_ES_GUÍA.docx y Guia_Firebase_Android_PulseHome.docx, documentos entregados por el estudiante.
- [Configuración de Firebase en Android](https://firebase.google.com/docs/android/setup?hl=es)
- [Reglas de seguridad de Firebase](https://firebase.google.com/docs/database/security)
- [Permisos de Bluetooth en Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Alcance de Bluetooth, Bluetooth SIG](https://www.bluetooth.com/learn-about-bluetooth/key-attributes/range/)
- [Resumen de Bluetooth Low Energy, Android Developers](https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview)
- [Hoja de datos ESP32 v5.3, tabla 5-4, Espressif](https://documentation.espressif.com/esp32_datasheet_en.html)
- [Especificaciones del punto de acceso AP40, Datto](https://networkinghelp.datto.com/help/Content/2_AccessPoints/1-overview/KB115000617663.html)
- [MQTT](https://mqtt.org/)
- [ISO/IEC 27400](https://www.iso.org/standard/44373.html)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Criptografía en Android](https://developer.android.com/privacy-and-security/cryptography)
