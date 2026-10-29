# Correspondencia del avance con la rúbrica

La guía general contempla una solución completa. La entrega formativa se acota conforme a la última aclaración del estudiante: avance local funcional, seguridad implementada, bibliotecas Firebase agregadas y conectividad explicada. Esto no acredita por sí solo el cumplimiento final de todos los criterios.

| Criterio | Evidencia del avance y pendiente |
|---|---|
| 2.1.1.1 | Problemática vial y sensores/ESP32/Android identificados en la explicación. |
| 2.1.1.2 | Login local, Inicio, Estado, Detalle, búsqueda y filtros. Funciones futuras separadas. |
| 2.1.1.3 | Kotlin, Android Studio, Compose, Material 3 y ViewModel justificados. |
| 2.1.1.4 | Propuesta documentada. Presentación y retroalimentación del docente pendientes. |
| 2.1.2.5 | Wi-Fi, Bluetooth y MQTT comparados por cobertura, consumo y estabilidad, sin implementarlos. |
| 2.1.2.6 | Bosquejo separado en MOCKUP_Y_NAVEGACION.md: acceso, Inicio, Estado, Detalle, interacciones y navegación. Capturas de ejecución y validación visual en Android pendientes. |
| 2.1.2.7 | Validación de UI y autenticación ante docente pendiente. |
| 2.1.3.8 | Proyecto Kotlin compilado con assembleDebug. APK generado. |
| 2.1.3.9 | Sin permisos de Bluetooth, localización ni exploración Wi-Fi innecesarios. INTERNET declarado para las bibliotecas Firebase. |
| 2.1.3.10 | Cuenta local con PBKDF2 y sal, registro AES-GCM, Android Keystore, control de sesión y bloqueo temporal. Nube y evaluación completa ISO pendientes. |
| 2.1.3.11 | Compilación comprobada. Pruebas de autenticación y seguridad en dispositivo pendientes. Comunicación IoT aún no implementada. |
| 2.1.4.12 | Demostración en clase y comunicación entre dos Android pendientes. No se presentan como realizadas. |
| 2.1.4.13 | Decisiones y límites explicados en el documento de la formativa. |
| 2.1.4.14 | Controles locales identificables en código; evidencia de su ejecución en dispositivo pendiente. |
| 2.1.4.15 | Preguntas, retroalimentación y ajustes finales con el docente pendientes. |

## Verificación realizada

La compilación `:app:assembleDebug` terminó con BUILD SUCCESSFUL. El aviso de versiones de XML del SDK no impidió la compilación. Se conserva Android Gradle Plugin 8.4.1 y Kotlin 1.9.24 del proyecto recibido, usando Gradle 8.6.

Compilar confirma que se pudo construir el APK; no sustituye la ejecución de la app ni valida por sí solo el comportamiento de Android Keystore. No se generaron capturas falsas ni se declaró una conexión Firebase activa.

## Pruebas para el teléfono

1. Instalar el APK de avance en un dispositivo de prueba. El applicationId coincide con el original; si Android informa conflicto de firma, no desinstalar una app con datos importantes sin conservarlos primero.
2. Crear una cuenta local con contraseña de al menos diez caracteres, letras y números. Confirmar que una contraseña corta no permita crearla.
3. Recorrer Inicio, Estado, búsqueda, filtros y Detalle.
4. Cerrar sesión y confirmar que las pantallas privadas vuelvan a requerir acceso.
5. Intentar una contraseña incorrecta y verificar rechazo. Probar la correcta.
6. Tras cinco intentos incorrectos, comprobar bloqueo por un minuto; después, volver a probar la contraseña correcta.
7. Cerrar y volver a abrir el proceso para comprobar que la cuenta se conserva y que no entra automáticamente sin autenticar.
8. Documentar el resultado real y tomar las capturas solicitadas por el docente. Si algo falla, registrar y corregirlo antes de la demostración.

No existe una contraseña predefinida y no se utiliza una cuenta de Firebase para este acceso local. Los estados y lecturas de estructuras son ejemplos académicos.
