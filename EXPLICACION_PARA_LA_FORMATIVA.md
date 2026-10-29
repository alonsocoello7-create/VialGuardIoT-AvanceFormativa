# Explicación del avance y las conexiones

## Problema y dispositivos

VialGuard propone mostrar en una aplicación Android información de una maqueta de monitoreo vial. La solución prevista considera HC-SR04 para distancia y MPU-6050 para inclinación, conectados al ESP32. Dos teléfonos Android representarían la interacción entre emisor y monitor que solicita la guía. En este avance, los datos son ejemplos y el ESP32 aún no transmite a la app.

La aplicación permite crear una cuenta local protegida, iniciar sesión, consultar estructuras, buscar, filtrar y abrir su detalle. Las funciones de nube y comunicación se documentan como trabajo posterior.

## Herramientas

Android Studio y Kotlin corresponden a la guía de evaluación. Jetpack Compose y Material 3 permiten construir la interfaz en Kotlin. El ViewModel separa el estado de la pantalla. La guía Firebase usa un ejemplo PulseHome con XML, mientras que el proyecto VialGuard recibido usa Compose; el registro de la app y las dependencias Firebase se adaptan al paquete `cl.inacap.vialguard`.

## Para qué se agregan las bibliotecas

| Biblioteca | Uso previsto |
|---|---|
| firebase-auth | Identificar al usuario mediante Firebase Authentication en una etapa posterior. El login local de este avance todavía no la utiliza. |
| firebase-firestore | Almacenar y consultar documentos, por ejemplo datos de estructuras o perfiles. |
| firebase-database | Explorar la sincronización de lecturas con Realtime Database, siguiendo el ejemplo de la guía. |
| Firebase BoM | Mantener compatibles las versiones de las bibliotecas Firebase agregadas. |

Incluir ambas bibliotecas de bases de datos no obliga a duplicar datos en ellas. Antes de programar la integración se definirá cuál almacena cada tipo de información. No se implementa esa decisión ahora ni se sustituye en esta entrega el servidor Django previsto anteriormente.

## Cómo serían las conexiones

Primero, los sensores se conectan por cables al ESP32. El HC-SR04 utiliza TRIG/ECHO, y el MPU-6050 utiliza I2C. Estos cables no llegan al teléfono.

Para la demostración futura entre teléfonos, Android A publicaría un dato de prueba y Android B lo consultaría mediante Firebase, con el acceso que permitan las reglas configuradas. Ambos necesitan una conexión de red para usar la nube. Más adelante se podría reemplazar el origen de demostración por el ESP32.

Wi-Fi es el acceso a una red, Bluetooth permite un enlace inalámbrico de proximidad y Firebase es un servicio en la nube. MQTT es un protocolo de mensajería que necesita una red y un broker. Firebase Authentication identifica usuarios; por sí solo no comparte lecturas.

| Alternativa | Justificación para exponer |
|---|---|
| Wi-Fi y Firebase | Propuesta para compartir datos mediante Internet entre teléfonos que pueden estar en redes distintas. La estabilidad depende de la cobertura y de Internet. Para la maqueta alimentada por USB, el consumo no es la restricción principal. |
| Bluetooth | Alternativa para teléfonos cercanos y una demostración sin Internet. El alcance y consumo dependen de la versión, hardware y entorno. BLE puede ser apropiado si se prioriza consumo. |
| MQTT sobre red IP | Alternativa para telemetría mediante publicadores, suscriptores y broker. No reemplaza la conexión Wi-Fi ni se agrega a este avance. |

No se fijan distancias o consumos universales sin medir los equipos. En una integración futura se declararán los permisos que requieran las operaciones realmente implementadas. Para esta versión local no se solicitan permisos Bluetooth, ubicación ni exploración de redes. INTERNET queda declarado para la futura integración de las bibliotecas Firebase.

## Configuración de Firebase que se explicará

Los pasos de la guía son crear el proyecto, registrar la app con su applicationId exacto, colocar google-services.json en app, declarar Google Services y las dependencias, habilitar los proveedores y configurar las bases y sus reglas. Este avance solo necesita mostrar las bibliotecas agregadas y justificar esos pasos; no presenta la conexión como terminada.

Durante la preparación se creó el proyecto `vialguard-ac2f1` en Spark, se registró Android, se habilitó correo/contraseña y se creó Firestore en nam5 con reglas privadas. Esa configuración quedó adelantada y no es evidencia de que esta app ya se conecte. No se activa más configuración en esta entrega. No se creó Realtime Database ni se habilitó acceso con Google.

## Seguridad implementada y explicación

El avance implementa autenticación local, verificación PBKDF2 con sal aleatoria, cifrado AES-GCM del registro y gestión de la clave con Android Keystore. También exige una contraseña mínima, limita intentos, restringe las pantallas a una sesión autenticada, permite cerrar sesión y deshabilita copias de seguridad y tráfico HTTP en claro. No contiene una contraseña de acceso predefinida. Los detalles están en `LocalCredentialStore.kt` y `AvanceViewModel.kt`.

Las pruebas en dispositivo deben comprobar rechazo de credenciales incorrectas, bloqueo temporal y denegación de acceso sin sesión. Para la futura nube se explicarán autenticación gestionada, transporte cifrado, autorización por usuario y validación en servidor. Los controles locales ya escritos y las medidas de nube aún pendientes se distinguen expresamente. No se declara certificación ni cumplimiento completo de ISO/IEC 27400.

## Qué mostrar al docente

Presentar el problema y las pantallas del avance, explicar las bibliotecas agregadas y el flujo de comunicación previsto. Mostrar las funcionalidades locales ejecutándose y responder qué partes aún no están conectadas. Registrar la retroalimentación real del docente y realizar los ajustes que indique. La revisión y la aceptación del docente no se inventan.

La guía general pide interacción entre dos Android, pruebas de conectividad y evidencia de seguridad. El alcance acordado exige seguridad implementada y deja Bluetooth y Wi-Fi como explicación. La app se ejecutó en un emulador oficial Android 14; las capturas reales y las pruebas de acceso y navegación están en `evidencias/` y `RESULTADOS_PRUEBAS.md`. La prueba en el teléfono de clase y la conectividad entre Android siguen pendientes. La validación del docente no se declara realizada.

## Referencias

- TI3042_U2_EF_GUÍA.docx y Guia_Firebase_Android_PulseHome.docx, documentos entregados por el estudiante.
- [Configuración Firebase Android](https://firebase.google.com/docs/android/setup?hl=es).
- [Reglas de seguridad Firebase](https://firebase.google.com/docs/database/security).
- [Permisos Bluetooth Android](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions).
- [MQTT](https://mqtt.org/).
- [ISO/IEC 27400](https://www.iso.org/standard/44373.html).
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore).
- [Criptografía en Android](https://developer.android.com/privacy-and-security/cryptography).
