# Revisión final del avance

Se contrastaron el código, el manifiesto, Gradle y la documentación con la guía TI3042 y la guía Firebase entregadas. La revisión distingue el alcance acordado para este avance de los requisitos generales del proyecto completo.

## Correcciones realizadas

- Se eliminó del README la frase antigua que dejaba toda la seguridad pendiente. Los controles locales se implementaron y se probaron en un emulador Android 14.
- Se añadió `MOCKUP_Y_NAVEGACION.md` con pantallas, navegación, mensajes y límites de los datos de demostración.
- Se publicó el código en GitHub y se preparó `ENTREGA_AAI_VialGuard.txt`, que contiene el enlace público para cargarlo en AAI. La tarea todavía no se ha enviado a AAI.
- La primera ejecución de Android Lint no encontró errores, pero señaló que `allowBackup=false` no cubre por sí solo todas las transferencias entre dispositivos modernos. Se añadió `data_extraction_rules.xml` para excluir SharedPreferences, donde se guarda el registro cifrado, de las copias en nube y transferencias entre dispositivos. También se deshabilitó `fullBackupContent` para la configuración anterior.
- En la prueba visual se corrigió el texto oscuro sobre el botón oscuro de acceso; se asignó el color de contenido del botón explícitamente.
- En la prueba de acceso con contraseña vacía se detectó un error técnico del proveedor criptográfico. Ahora los campos vacíos se rechazan antes de derivar la clave y se muestra «Ingresa tu usuario y contraseña». Se repitió la prueba y se guardó la evidencia corregida.

## Comprobaciones y límites

El estado final de compilación, Android Lint, firma y hash se registra en `VERIFICACION.txt`. Las advertencias de versiones disponibles, organización del catálogo y recursos sin uso se conservan: no se realizó una actualización general de herramientas en el cierre de esta entrega. No implican que el APK haya sido probado en un teléfono.

Se instaló y ejecutó en el emulador oficial `VialGuard_API34`, Android 14 x86_64. Las pruebas funcionales y de seguridad y sus capturas reales se registran en `RESULTADOS_PRUEBAS.md`. No se probó en un teléfono físico ni se ejecutó una transferencia o restauración de copias entre dispositivos.

La revisión del código no identificó una contraseña predefinida, almacenamiento de contraseña en claro, clave AES fija, sockets ni llamadas de autenticación o bases de datos Firebase. El acceso local está separado de la futura conexión Firebase. Esto no constituye una auditoría exhaustiva ni certificación ISO.

## Pendientes antes de presentar

1. Subir `ENTREGA_AAI_VialGuard.txt` a la tarea de AAI antes del plazo. El repositorio público es https://github.com/alonsocoello7-create/VialGuardIoT-AvanceFormativa .
2. Repetir la demostración en el teléfono de clase; el emulador no verifica todos los fabricantes ni versiones de Android.
3. Presentar y validar con el docente el avance y su alcance reducido. La documentación general exige comunicación entre dos Android; aquí se mantiene explicada por la aclaración aportada por el estudiante, sin atribuir al docente una aprobación no registrada.

## Limitaciones conocidas del prototipo

- Una sola cuenta local, sin recuperación de contraseña ni roles. Si se pierde la contraseña no hay recuperación dentro de la app. Borrar sus datos elimina la cuenta local.
- El bloqueo usa el reloj del dispositivo; no es un mecanismo administrado por servidor.
- La sesión permanece en memoria hasta cerrar sesión o finalizar el proceso. No hay caducidad por inactividad ni bloqueo automático al pasar a segundo plano.
- Las lecturas son fijas de demostración. No hay sensores conectados, comunicación entre teléfonos ni sincronización en nube.
- Firebase incluye bibliotecas y explicación. No incluye `google-services.json` ni operaciones reales de sus servicios en la app. Por tanto, no completa toda la lista de configuración de la guía Firebase.
- Las medidas de seguridad y la interfaz todavía requieren validación en los equipos de la demostración.

El avance tiene código y documentación coherentes con el alcance acordado. No se declara que todos los criterios de la rúbrica estén cumplidos o demostrados.

Referencia de la corrección de copias: [documentación oficial de Android](https://developer.android.com/identity/data/autobackup).
