# Comparación con Ejemplo02 del profesor

Se revisó estáticamente el código fuente de Ejemplo02.zip entregado por el estudiante, sin ejecutar ni modificar el ejemplo. La comparación corresponde a VialGuardIoT_AvanceFormativa, con seguridad local implementada, Firebase como bibliotecas y justificación, y Bluetooth/Wi-Fi como explicación.

## Conclusión

El enfoque de VialGuard es coherente con el ejercicio de clase: Kotlin, Compose, Material 3, estado de interfaz mediante ViewModel y datos de demostración. El ejemplo presenta expresamente una simulación local. Esto apoya usar una maqueta de datos para la formativa, pero no elimina los requisitos de la guía ni acredita validación del docente.

## Coincidencias y diferencias

| Aspecto | Ejemplo02 | Nuestro avance |
|---|---|---|
| Interfaz | Compose y Material 3 | Compose y Material 3 |
| Estado | DevicesViewModel y StateFlow | AvanceViewModel y StateFlow para acceso y mensajes |
| Datos | Device y SensorReading; repositorio simulado en desarrollo | Estructuras y lecturas estáticas de demostración, identificadas como ejemplos |
| Presentación de sensores | Distingue conectado, desconectado, sin lecturas y última lectura guardada | Explica que los datos no vienen todavía del ESP32. No simula una conexión activa |
| Actualización | Propone emisiones periódicas cada tres segundos, con partes incompletas | No hay actualización periódica ni telemetría real en este avance |
| Firebase | No aparece en las dependencias o fuentes revisadas | Authentication, Firestore y Realtime Database agregados como bibliotecas, sin conexión activa |
| Seguridad | No hay autenticación ni cifrado implementados en las fuentes revisadas | Cuenta local, PBKDF2 con sal, registro cifrado AES-GCM, Android Keystore y limitación de intentos |
| Evidencia de compilación | No se compiló en esta revisión | Nuestro APK ya compiló y su firma se verificó. Las pruebas en teléfono siguen pendientes |

## Qué podemos aprovechar

La separación `model`, `data` y `ui/viewModel` del ejemplo ayuda a explicar quién representa los datos, quién los proporciona y quién prepara la interfaz. Si el docente pide seguir esa estructura, podemos mover el modelo y los datos estáticos de VialGuard a archivos separados, sin agregar funciones ni conectividad. Ese cambio organizativo no se hizo durante esta revisión.

También conviene explicar que una lectura ausente o antigua no equivale a una medición normal. El ejemplo solo considera alerta actual cuando el dispositivo aparece conectado y tiene una lectura fuera de rango. VialGuard debe mantener la distinción entre estados de ejemplo y mediciones reales. No corresponde copiar los límites de temperatura y humedad al proyecto vial: nuestros sensores y magnitudes son otros.

La interfaz del ejemplo contempla carga, error y lista vacía. Nuestro avance ya muestra mensajes de autenticación y resultados vacíos de búsqueda. Los estados de carga y error de una conexión IoT se explicarán cuando exista esa conexión; no se agregarán para aparentar una integración terminada.

## Partes sin terminar en el ZIP recibido

- `DevicesRepository.kt` exige `observeDevices()`. `FakeDeviceRepository.kt` declara `ObserveDevices()` y no implementa el método con el nombre y override que exige la interfaz.
- En ese repositorio se asigna `null` a `simulatedSnapshot` y luego se intenta invocarlo como función.
- `SensorReading` declara `measureAtMills`, mientras que la pantalla utiliza `measuredAtMillis`.
- `MainActivity.kt` todavía muestra el saludo `Hello Android!`, sin conectar DevicesScreen y DevicesViewModel.

Son observaciones del código recibido, no un resultado de ejecutar Gradle. Por eso no se copia el ejemplo completo ni se afirma que esté listo para entregar.

## Cómo defender nuestro avance

Podemos explicar al docente: “Seguimos el enfoque de simulación local trabajado en clase. La app usa Kotlin, Compose y ViewModel. Mostramos datos de demostración identificados y un acceso local con medidas de seguridad. Agregamos las bibliotecas Firebase y justificamos su integración futura. Bluetooth y Wi-Fi se explican en esta etapa, sin afirmar que ya existe comunicación con los sensores”.

Antes de presentar, falta ejecutar el APK en un teléfono, verificar el acceso y sus controles y tomar capturas reales. La aceptación de la propuesta, las pruebas entre dos Android y la retroalimentación del docente no se dan por realizadas.

La conclusión es favorable al enfoque del avance, con esas comprobaciones pendientes. El ejemplo del profesor no demuestra que Firebase sea opcional para la entrega ni reemplaza las aclaraciones específicas del estudiante y del docente.
