# VialGuard IoT: avance con enlace entre dos Android

Esta versión conserva el recorrido local del avance formativo (acceso, Inicio, Estado, Detalle, búsqueda y filtros) y agrega la pestaña **Enlace**, en la que dos teléfonos Android se comunican por Wi-Fi local:

- **Monitor:** un teléfono recibe las lecturas, muestra el estado y envía órdenes.
- **Estación:** el otro teléfono simula los sensores de la maqueta y transmite.

Las lecturas son simuladas y el ESP32 todavía no transmite a la app. El acceso es local: la app no autentica contra Firebase.

## Enlace entre dos Android

- **Vinculación:** el monitor abre un receptor TCP en el puerto 8888 y muestra su IP y un código de vinculación de 8 caracteres. La estación se conecta con esos datos.
- **Envío:** la estación manda una lectura por segundo.
- **Estado:** se calcula con los mismos umbrales del firmware de la maqueta (`VialGuard_ESP32_final`):
  - alarma con 15 cm o 5°, o con datos no válidos;
  - precaución al 70 % de esos umbrales;
  - retención de la alarma durante 30 s.
- **Nombres de los estados:** la app dice Normal y Alarma donde la maqueta dice paso habilitado y paso cortado; Precaución se llama igual. El firmware además corta el paso por vibración y registra la causa; la estación no simula vibración ni causas.
- **Órdenes del monitor:** «Reconocer alarma», que no la apaga, y «Recalibrar», que se rechaza durante una alarma.
- **Historial:** el monitor guarda sus eventos en SQLite.
- **Lecturas antiguas:** una lectura de más de 5 s, o con el enlace caído, se presenta como estado desconocido.
- **Reconexión:** si se corta el enlace, la estación reconecta sola.
- **Sesión:** cerrar sesión detiene el enlace.

`claude_ENLACE_WIFI.md` explica el protocolo, por qué se eligió Wi-Fi local y no Firebase, Bluetooth ni MQTT, los límites de seguridad y de uso y cómo hacer la demostración con dos teléfonos o dos emuladores.

## Seguridad implementada

**Cuenta local:**

- **Contraseña:** no se guarda. Se deriva un verificador PBKDF2-HMAC-SHA256 con sal aleatoria y 600 000 iteraciones, y se compara en tiempo constante.
- **Registro cifrado:** el registro con el verificador y la sal se cifra con AES-GCM. La clave la crea y administra Android Keystore.
- **SharedPreferences:** contiene el registro cifrado y el IV; nunca la contraseña ni una clave escrita en el código.
- **Política de contraseña:** al menos diez caracteres, con letras y números.
- **Bloqueo:** cinco intentos incorrectos bloquean el acceso durante un minuto.
- **Sesión:**
  - vive solo en memoria;
  - «Cerrar sesión» la elimina;
  - reiniciar el proceso exige volver a entrar;
  - las pantallas de consulta y el enlace solo están disponibles con sesión.

**Canal del enlace:**

- **Vinculación:** usa claves ECDH P-256 efímeras. El código se deriva con PBKDF2-HMAC-SHA256 (200 000 iteraciones), cada extremo prueba conocerlo con HMAC-SHA256 y el código nunca viaja por la red.
- **Mensajes:** van cifrados con AES-256-GCM, con una clave por sentido y un contador como IV. Se rechazan los mensajes alterados, repetidos o reordenados.
- **Validación:** los valores recibidos se validan aunque el mensaje esté autenticado.
- **Intentos:** cinco códigos erróneos detienen el receptor, y hay que iniciarlo de nuevo para obtener otro código.

**Copias y red:**

- **Copias de seguridad:** se mantiene `allowBackup=false` y se fija `fullBackupContent=false`.
- **Android 12 o superior:** `data_extraction_rules.xml` excluye de las copias en nube y de las transferencias entre dispositivos tanto SharedPreferences como la base del historial.
- **Tráfico HTTP:** el tráfico en texto claro está deshabilitado.

**Permisos:**

- **Manifiesto de la app:** declara solo INTERNET, que el enlace usa para abrir sus sockets.
- **APK final:** por fusión de manifiestos incluye además ACCESS_NETWORK_STATE y READ_GSERVICES, que agregan las bibliotecas Firebase, y un permiso interno de AndroidX para receptores no exportados.
- **Sin otros permisos:** no se piden permisos de Bluetooth, ubicación ni exploración Wi-Fi.

Son controles concretos del prototipo, no una certificación de cumplimiento de ISO/IEC 27400. No hay roles: cualquier usuario con sesión en el monitor puede enviar órdenes, y el historial registra quién las envió. En `claude_ENLACE_WIFI.md` se explican los límites del canal: no hay certificados y el código tiene unos 40 bits.

## Firebase en esta entrega

- **Bibliotecas:** se agregaron las oficiales `firebase-auth`, `firebase-firestore` y `firebase-database`, coordinadas por Firebase BoM.
- **Complemento Google Services:** se aplica solo si existe un `app/google-services.json` real, así que la app compila y funciona sin ese archivo.
- **Uso:** agregar bibliotecas no demuestra una conexión. La app no inicia sesión en Firebase ni lee o escribe en sus bases, y el enlace entre teléfonos tampoco usa Firebase.
- **Configuración:** se explica en `EXPLICACION_PARA_LA_FORMATIVA.md`.

## Funciones que no incluye

- **Funciones de gestión:** no hay historial avanzado de estructuras, registro ni eliminación de incidentes, informes, reinicio de estructuras ni funciones administrativas.
- **ESP32:** no se sincroniza con el ESP32. Los indicadores de conexión del enlace se refieren al otro teléfono, no a sensores reales.

## Estructura

`MainActivity.kt` contiene la interfaz principal y las pestañas:

| Archivo | Contenido |
|---|---|
| `AvanceViewModel.kt` | Estado de acceso |
| `LocalCredentialStore.kt` | Protección de credenciales locales |
| `LinkViewModel.kt` y `LinkScreen.kt` | Pestaña Enlace |
| `cl.inacap.vialguard.link` | Canal cifrado (`SecureChannel`), mensajes (`LinkMessage`), monitor y estación (`LinkEndpoints`), reglas de alarma (`AlarmEvaluator`) e historial (`EventHistory`) |
| `app/src/test` | Pruebas unitarias de las reglas de alarma, del canal cifrado y del enlace por TCP local |

## Abrir y probar

1. **Proyecto:** abrir esta carpeta en Android Studio y sincronizar Gradle.
2. **Cuenta:** instalar la app en un teléfono con Android 8 o superior y, en la primera apertura, crear una cuenta local. El nombre sugerido es `operador`; la contraseña debe cumplir la política que se muestra y no hay una contraseña predefinida.
3. **Recorrido local:** pasar por Inicio, Estado y Detalle. Probar la búsqueda, los filtros y Cerrar sesión.
4. **Enlace:** seguir «Demostración con dos teléfonos» de `claude_ENLACE_WIFI.md`. Los teléfonos deben estar en la misma red; si el Wi-Fi del instituto aísla a los clientes, usar el hotspot de uno de ellos.
5. **Pruebas unitarias:** ejecutar `gradlew testDebugUnitTest`.
6. **Evidencias:** revisar las capturas de `evidencias/` y `RESULTADOS_PRUEBAS.md`, y repetir la demostración en los teléfonos de clase.

Si en la copia de trabajo existe `local.properties`, apunta al SDK del computador de preparación. No se versiona: Android Studio crea el suyo.

## Versiones

| Componente | Versión |
|---|---|
| Kotlin | 1.9.24 |
| Compose | La configuración recibida |
| Firebase BoM | 33.1.2 |
| Gradle (wrapper) | 8.6 |
| Android Gradle Plugin | 8.4.1 |
| compileSdk y targetSdk | 34 |
| minSdk | 26 (Android 8) |

`minSdk` pasó de 24 a 26 porque ambos teléfonos deben derivar las claves con PBKDF2WithHmacSHA256, disponible desde Android 8. No se afirma que sean las versiones más recientes.

## Alcance

La guía de la unidad pide que al menos dos dispositivos Android interactúen por Wi-Fi o Bluetooth. Esta versión implementa esa interacción por Wi-Fi local.

Faltan:

- probarla en teléfonos físicos;
- la demostración en clase;
- la validación del docente.

`RESULTADOS_PRUEBAS.md` registra qué se verificó y en qué condiciones.

Código publicado: https://github.com/alonsocoello7-create/VialGuardIoT-AvanceFormativa. Esta versión se subió el 4 de noviembre de 2026, y la versión de octubre sigue disponible en el historial del repositorio.
