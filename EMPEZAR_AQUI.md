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
5. Revisar `evidencias/` y `RESULTADOS_PRUEBAS.md`: contienen capturas reales del emulador y resultados registrados. Repetir la demostración en el teléfono de clase y revisar los ajustes con el docente.

## Qué leer para explicar el trabajo

- `EXPLICACION_PARA_LA_FORMATIVA.md`: problemática, herramientas, Firebase, conexiones y seguridad.
- `RUBRICA_Y_PRUEBAS.md`: correspondencia con los quince criterios y pruebas pendientes.
- `MOCKUP_Y_NAVEGACION.md`: bosquejo de pantallas, interacciones y recorrido de acceso.
- `COMPARACION_EJEMPLO_PROFESOR.md`: relación con el ejemplo de clase, sin copiar funciones adicionales.
- `VERIFICACION.txt`: comprobaciones realizadas sobre la compilación y el APK.
- `REVISION_FINAL.md`: correcciones, limitaciones conocidas y pendientes antes de presentar.
- `RESULTADOS_PRUEBAS.md` y `evidencias/`: pruebas y capturas reales en Android 14.

## Estado comprobado

El APK compiló correctamente, se verificó su firma y se ejecutó en un emulador oficial Android 14. Se probaron navegación y controles de seguridad local y se guardaron capturas reales. Los resultados y límites están en `RESULTADOS_PRUEBAS.md`. La prueba en el teléfono de clase y la aprobación del docente siguen pendientes. Esta entrega no acredita el cumplimiento final de toda la rúbrica.

El borrador separado llamado `VialGuardIoT_AvanceFirebase` corresponde a un alcance anterior y no es la versión de entrega. El proyecto original recibido se conserva sin cambios.

## Cómo pide entregar la guía

La guía TI3042 indica subir a AAI un archivo de texto con un enlace a GitHub que contenga el código. El proyecto se publicó como repositorio público, por elección del estudiante: https://github.com/alonsocoello7-create/VialGuardIoT-AvanceFormativa . El archivo `ENTREGA_AAI_VialGuard.txt`, junto al proyecto en el ZIP, contiene ese enlace y es el que se debe cargar en AAI. La carga en AAI debe realizarla el estudiante; no se ha presentado la tarea automáticamente. El repositorio excluye `.gradle`, `.idea`, `build` y `local.properties`.
