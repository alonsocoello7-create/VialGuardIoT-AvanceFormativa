# Enlace Wi-Fi entre dos Android

La guía de la unidad 2 exige que la aplicación permita interactuar a dos dispositivos Android por Wi-Fi o Bluetooth. Esta versión agrega esa interacción en la pestaña **Enlace**:

- **Monitor:** un teléfono recibe las lecturas, muestra el estado de la estructura y envía órdenes.
- **Estación:** el otro teléfono simula los sensores de la maqueta (HC-SR04 y MPU-6050), calcula el estado y lo transmite.

El ESP32 todavía no participa. Las lecturas de la estación son simuladas y se rotulan así en ambas pantallas.

## Qué hace cada rol

| Monitor | Estación |
|---|---|
| Abre un receptor TCP en el puerto 8888 y muestra su IP y un código de vinculación de 8 caracteres | Se conecta con la IP del monitor y su código |
| Muestra estado, distancia, variación, inclinación, referencia, validez de los datos y la cuenta de recuperación | Tiene deslizadores de distancia (2–200 cm) e inclinación (0–30°) y un interruptor de falla de sensor |
| Envía órdenes: «Reconocer alarma» y «Recalibrar» | Evalúa el estado con los mismos umbrales del firmware de la maqueta |
| Distingue una lectura reciente de una lectura antigua o de un enlace caído | Envía una lectura por segundo y reconecta sola si se corta el enlace |
| Guarda en SQLite los cambios de estado, vinculaciones, rechazos y órdenes (hasta 500 registros) | Recibe las órdenes y responde con su efecto en la siguiente lectura |

Los umbrales son los mismos del firmware vigente de la maqueta, `VialGuard_ESP32_final/sketch.ino`:

- **Alarma:** la distancia varía 15 cm o la inclinación 5° respecto de la referencia, o los datos dejan de ser válidos.
- **Precaución:** la variación alcanza el 70 % de esos umbrales.
- **Retención:** la alarma se mantiene hasta acumular 30 s seguidos de lecturas normales. Una precaución reinicia la cuenta.

Son umbrales didácticos, no criterios de ingeniería estructural.

**Equivalencia con la maqueta:**

| App | Firmware |
|---|---|
| Normal | Paso habilitado |
| Precaución | Precaución |
| Alarma | Paso cortado |

El firmware además corta el paso por vibración (giro o sacudida del MPU-6050) y registra la causa del corte. La estación no simula vibración ni causas.

## Reglas de operación

- **Reconocer una alarma** deja constancia de que alguien la vio. No la apaga ni acorta los 30 s de retención.
- **Recalibrar** se rechaza mientras hay una alarma. Si una orden remota pudiera cambiar la referencia, podría borrar una alarma real. El monitor deshabilita el botón y la estación también rechaza la orden.
- **Falla de sensor:** los datos marcados como no válidos activan la alarma.
- **Lecturas antiguas:** una lectura de más de 5 s, o con el enlace caído, se muestra como «Última lectura guardada… Estado actual desconocido». Nunca se presenta como el estado actual.
- **Sesión:** el enlace solo existe con una sesión local iniciada. Cerrar sesión lo detiene.

## Por qué Wi-Fi local y no Firebase

| Opción | Distancia | Consumo | Estabilidad | Decisión |
|---|---|---|---|---|
| Wi-Fi local con TCP | Alcance del punto de acceso o del hotspot | El de la radio Wi-Fi del teléfono, que ya está encendida | Conexión persistente, detección de silencio a los 5 s y reconexión automática | Elegida |
| Firebase sobre Internet | Sin límite de distancia | Wi-Fi o datos móviles | Depende de Internet y del servicio | Descartada para esta entrega: la app no tiene `google-services.json`, el enlace local no necesita Firebase y la guía pide Wi-Fi o Bluetooth entre los teléfonos. La configuración de la consola creada en octubre no se volvió a verificar. |
| Bluetooth | Corto, según la versión y el hardware | Bajo, sobre todo con BLE | Requiere emparejamiento y permisos de ejecución en Android 12 o superior | Alternativa válida, con más permisos y pasos para la demostración |
| MQTT | Depende de la red | Bajo | Requiere un broker | Necesita un servidor adicional que no existe en el proyecto |

Las bibliotecas Firebase siguen en el proyecto para una etapa posterior, pero ninguna parte del enlace las usa.

## Protocolo

### 1. Vinculación

1. La estación envía `VGL1`, un nonce aleatorio de 32 bytes y una clave pública ECDH P-256 efímera.
2. El monitor responde con su propio nonce, su clave pública efímera y una prueba HMAC-SHA256.
3. La estación verifica la prueba del monitor antes de revelar la suya. Si no coincide, corta la conexión.
4. La estación envía su prueba HMAC-SHA256 y el monitor la verifica.

Las claves se obtienen así:

- **Clave maestra:** PBKDF2-HMAC-SHA256 del código, con los dos nonces como sal y 200 000 iteraciones.
- **Claves de sesión y pruebas:** HMAC de la clave maestra sobre una etiqueta, el secreto ECDH y el resumen SHA-256 de todo el saludo.

El código nunca viaja por la red.

### 2. Mensajes

- **Formato de cada marco:** 4 bytes de longitud, seguidos del contenido cifrado con AES-256-GCM.
- **Claves:** hay una por sentido.
- **IV:** un contador de 64 bits.
- **Rechazo:** un mensaje alterado, repetido o fuera de orden no supera el descifrado y cierra la conexión.
- **Tamaño máximo:** 4096 bytes.

El contenido es JSON y cada valor se valida aunque el mensaje esté autenticado:

| Mensaje | Dirección | Contenido |
|---|---|---|
| Lectura | Estación → monitor | Estación, distancia, inclinación, referencia, variación, estado, validez, segundos de recuperación, alarma reconocida |
| Orden | Monitor → estación | `ACKNOWLEDGE_ALARM` o `RECALIBRATE` |
| Latido | Monitor → estación | Respuesta a cada lectura, para que la estación detecte que el monitor dejó de escuchar |

### 3. Tiempos e intentos

| Parámetro | Valor |
|---|---|
| Período de lectura | 1 s |
| Silencio que cierra el enlace, en cualquier sentido | 5 s |
| Espera máxima para conectar | 4 s |
| Reintento de la estación | Cada 2 s |
| Estaciones simultáneas | Una |
| Intentos fallidos antes de detener el receptor | 5 durante la vigencia del código, aunque entre medio se conecte la estación legítima. Hay que iniciarlo de nuevo, lo que genera otro código |

### 4. Código de vinculación

- **Formato:** 8 caracteres de un alfabeto de 31 símbolos. Se excluyen 0, O, 1, I y L para evitar confusiones.
- **Combinaciones:** 31⁸ ≈ 8,5 × 10¹¹, unos 40 bits.
- **Vigencia:** se genera uno nuevo cada vez que se inicia el receptor.
- **Escritura:** se aceptan minúsculas y guion.

## Límites de seguridad

- **Sin certificados:** la confianza depende de copiar el código que muestra la pantalla del monitor.
- **Ataque activo:** un intermediario que se haga pasar por estación obtiene la prueba del monitor y podría intentar adivinar el código fuera de línea. PBKDF2 encarece cada intento y el código cambia en cada inicio del receptor, pero 40 bits no equivalen a la protección de certificados.
- **Observador pasivo:** quien solo escucha la red no puede probar códigos con lo que captura, porque las claves también dependen del secreto ECDH efímero.
- **Sin roles:** cualquier persona con sesión local en el monitor puede enviar órdenes. El historial registra qué usuario las envió.
- **Historial sin cifrar:** contiene estados y órdenes, no credenciales. Se excluye de las copias en nube y de las transferencias entre dispositivos.
- **Alcance:** son controles concretos del prototipo, no una certificación ISO/IEC 27400.

## Límites de uso

- **Primer plano:** el enlace funciona con la app en primer plano. No hay servicio en segundo plano y Android puede cerrar el proceso.
- **Misma red:** ambos teléfonos deben estar en la misma red. Muchas redes institucionales aíslan a los clientes entre sí; en ese caso conviene usar el hotspot de uno de los teléfonos o un router propio.
- **IP visible:** el monitor muestra las IP privadas de sus interfaces activas. Si aparece más de una, se usa la de la red compartida con la estación.
- **Android 8 o superior:** `minSdk` subió de 24 a 26. Ambos teléfonos deben derivar la clave con PBKDF2WithHmacSHA256, disponible desde Android 8.

## Demostración con dos teléfonos

1. **Instalar:** poner el APK en ambos teléfonos, crear una cuenta local en cada uno e iniciar sesión.
2. **Red:** conectar ambos a la misma red Wi-Fi o al hotspot de uno de ellos.
3. **Monitor:** en el teléfono A, abrir **Enlace → Monitor → Iniciar receptor** y anotar la IP y el código.
4. **Estación:** en el teléfono B, abrir **Enlace → Estación**, escribir la IP y el código del monitor y pulsar **Conectar**.
5. **Estados:** en B, mover la distancia de 100 a 88 cm (precaución) y luego a 80 cm (alarma). En A, comprobar los cambios.
6. **Órdenes:** en A, pulsar **Reconocer alarma**. Volver B a 100 cm y comprobar que la alarma sigue durante 30 s.
7. **Falla:** activar la falla de sensor en B y comprobar la alarma en A.
8. **Corte:** desconectar B o apagar su Wi-Fi. A debe mostrar la última lectura guardada como estado desconocido. Al volver la red, B reconecta sola.

## Prueba con dos emuladores en un computador

Dos emuladores no se ven entre sí directamente. Cada uno tiene su propia red virtual: las IP que muestra el monitor, como 10.0.2.15, solo existen dentro de ese emulador. Por eso se redirige un puerto del computador hacia el monitor:

1. **Monitor:** en el emulador A, iniciar el receptor.
2. **Redirección:** desde el computador, ejecutar `adb -s emulator-5554 emu redir add tcp:5000:8888`, ajustando el número del emulador A.
3. **Estación:** en el emulador B, escribir `10.0.2.2:5000` y el código del monitor. Desde un emulador, 10.0.2.2 es el computador.

Los resultados de esta prueba y sus capturas están en `RESULTADOS_PRUEBAS.md` y `evidencias/`.
