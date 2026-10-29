# Entrega del avance VialGuard

Esta carpeta es el proyecto que corresponde al alcance acordado para la formativa. El ZIP compartible contiene este proyecto y el APK de prueba `VialGuard_AvanceFormativa.apk`.

## Qué incluye

- Acceso local, Inicio, Estado, Detalle, búsqueda y filtros, con datos académicos de demostración.
- Seguridad local implementada: verificador de contraseña con sal, registro cifrado y clave administrada por Android Keystore, bloqueo temporal y cierre de sesión.
- Bibliotecas Firebase Authentication, Firestore y Realtime Database, con explicación de su configuración y finalidad. La app no se conecta todavía a Firebase.
- Explicación de Wi-Fi, Bluetooth y MQTT. No se implementa comunicación inalámbrica ni lectura real del ESP32.

## Cómo empezar

1. Descomprimir el ZIP antes de abrir el proyecto.
2. Para revisar el código, abrir la carpeta `VialGuardIoT_AvanceFormativa` en Android Studio y esperar la sincronización de Gradle. Para probar el APK ya construido, copiarlo a un teléfono de prueba e instalarlo.
3. En la primera apertura, crear una cuenta local. Pueden usar `operador` como nombre; elijan una contraseña de prueba de al menos diez caracteres con letras y números. No existe una contraseña predefinida.
4. Recorrer las pantallas, probar búsqueda y filtros y cerrar sesión. Seguir después la lista de pruebas de `RUBRICA_Y_PRUEBAS.md`.
5. Tomar capturas reales de acceso, Inicio, Estado y Detalle, y registrar los resultados reales de las pruebas de seguridad. Revisar los ajustes con el docente.

## Qué leer para explicar el trabajo

- `EXPLICACION_PARA_LA_FORMATIVA.md`: problemática, herramientas, Firebase, conexiones y seguridad.
- `RUBRICA_Y_PRUEBAS.md`: correspondencia con los quince criterios y pruebas pendientes.
- `MOCKUP_Y_NAVEGACION.md`: bosquejo de pantallas, interacciones y recorrido de acceso.
- `COMPARACION_EJEMPLO_PROFESOR.md`: relación con el ejemplo de clase, sin copiar funciones adicionales.
- `VERIFICACION.txt`: comprobaciones realizadas sobre la compilación y el APK.
- `REVISION_FINAL.md`: correcciones, limitaciones conocidas y pendientes antes de presentar.

## Estado comprobado

El APK compiló correctamente y se verificó su firma. Todavía no se ejecutó en un teléfono o emulador: la navegación y el comportamiento de la seguridad requieren esa comprobación. Las capturas y la aprobación del docente siguen pendientes. Esta entrega no acredita el cumplimiento final de toda la rúbrica.

El borrador separado llamado `VialGuardIoT_AvanceFirebase` corresponde a un alcance anterior y no es la versión de entrega. El proyecto original recibido se conserva sin cambios.

## Cómo pide entregar la guía

La guía TI3042 indica subir a AAI un archivo de texto con un enlace a GitHub que contenga el código. El ZIP sirve para compartir y abrir este proyecto, pero no sustituye ese enlace. Todavía no se publicó este proyecto ni se dispone de un enlace verificado. Antes de subir la tarea, publicar el código de esta carpeta en el repositorio del estudiante y comprobar el acceso que tendrá el docente. No subir `.gradle`, `.idea`, `build` ni `local.properties`; el proyecto incluye un `.gitignore`.
