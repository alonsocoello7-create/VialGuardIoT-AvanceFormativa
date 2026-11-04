# Bosquejo de la interfaz VialGuard

Diseño textual de las pantallas de la app. Sirve para revisar con el docente. Las capturas reales están en `evidencias/` y no reemplazan una validación del docente. La interfaz usa Compose y Material 3.

## Acceso y primera apertura

```text
VialGuard IoT
Monitoreo vial entre dos Android
Primera apertura: crea tu cuenta local...
[ Usuario de demostración: operador     ]
[ Contraseña: ••••••••••                ]
[ Crear cuenta local                   ]
Credenciales protegidas localmente.
Firebase queda pendiente de integración.
[Progreso durante la operación]
[Mensaje de validación o error]
```

- **Cuenta existente:** el botón cambia a «Iniciar sesión».
- **Contraseña:** se muestra oculta.
- **Durante la operación:** el botón se deshabilita y aparece una barra de progreso.
- **Pantallas de consulta:** solo aparecen después de autenticar.
- **Bloqueo:** cinco fallos bloquean el acceso durante un minuto, y la app lo informa si se intenta entrar en ese período.

## Inicio

```text
VialGuard IoT
Sesión: operador        [Cerrar sesión]
Datos de demostración...
Monitoreo de estructuras viales
[ Buscar estructura                    ]
Ejemplos: 1 habilitado, 1 inspección,
          1 sin datos
[ Puente de demostración               ]
[ Carretera de demostración            ]
[ Acceso de demostración               ]
----------------------------------------
     Inicio       Estado       Enlace
```

- **Búsqueda:** encuentra coincidencias en el nombre o la ruta, sin distinguir mayúsculas.
- **Tarjetas:** cada una muestra nombre, tipo, ruta y estado de ejemplo. Tocarla abre Detalle.
- **Sin coincidencias:** se muestra «No hay estructuras con ese filtro».

## Estado

```text
Sesión: operador        [Cerrar sesión]
Datos de demostración...
Estado de estructuras
[ Buscar estructura                    ]
[Todas] [Puente] [Carretera]
Resumen global de los tres ejemplos
[ Tarjetas que cumplen búsqueda/filtro ]
----------------------------------------
     Inicio       Estado       Enlace
```

- **Filtros:** el filtro de tipo y la búsqueda se combinan. «Todas» incluye también Acceso.
- **Resumen:** las cifras corresponden a los tres ejemplos, no a los resultados filtrados.
- **Alcance:** las tarjetas de esta pantalla son ejemplos fijos. Las lecturas en vivo están en Enlace.

## Detalle

```text
Sesión: operador        [Cerrar sesión]
[Volver a Estado]
Puente de demostración
Maqueta del laboratorio
Estado de ejemplo: Habilitado
Distancia de ejemplo: 100 cm
Inclinación de ejemplo: 0 grados
Firebase: bibliotecas agregadas...
Estas lecturas son de demostración...
----------------------------------------
     Inicio       Estado       Enlace
```

- **Lecturas:** las dos lecturas solo aparecen en el ejemplo Puente. Las demás tarjetas indican que el detalle es de ejemplo.
- **Volver:** se puede regresar con «Volver a Estado» o con las pestañas inferiores.

## Enlace: elección del rol

```text
Enlace entre dos teléfonos
Conecta ambos a la misma red Wi-Fi...
[Monitor] [Estación]
Estado del enlace (p. ej. «Enlace detenido.»)
```

Mientras el enlace está activo, los chips de rol se deshabilitan.

## Enlace: monitor

```text
[ Iniciar receptor ]
-- después de iniciar --
Esperando estación en el puerto 8888.
┌ Datos para la estación ─────────────┐
│ IP: 192.168.x.x                      │
│ Puerto: 8888                         │
│ Código: ABCD-2345                    │
│ El código cambia cada vez que...     │
└──────────────────────────────────────┘
[ Detener receptor ]
┌ Puente de demostración ─────────────┐
│ Estado: Normal | Precaución | Alarma │
│ Distancia: 100.0 cm (referencia ...) │
│ Variación: 0.0 cm                    │
│ Inclinación: 0.0°                    │
│ Recuperación: N s   (si corresponde) │
│ Alarma reconocida... (si corresponde)│
│ Lecturas simuladas por la estación.  │
└──────────────────────────────────────┘
[ Reconocer alarma ] [ Recalibrar ]
Historial
hh:mm:ss · Estación vinculada.
hh:mm:ss · Estado Alarma: 79.9 cm, 0.0°
hh:mm:ss · Orden enviada por operador: ...
[Aviso: código rechazado, orden enviada...]
```

- **Lectura reciente:** el estado se muestra en grande, y Alarma en rojo.
- **Lectura antigua o enlace caído:** si la lectura tiene más de 5 s, o el enlace se cortó, se muestra «Última lectura guardada, hace N s. Estado actual desconocido.».
- **Reconocer alarma:** solo se habilita con una alarma activa que todavía no se ha reconocido. Reconocerla no la apaga.
- **Recalibrar:** se deshabilita durante una alarma.
- **Historial:** registra vinculaciones, cambios de estado, órdenes con el usuario que las envió, intentos rechazados y el agotamiento del código.

## Enlace: estación

```text
[ IP del monitor: 192.168.x.x[:puerto] ]
[ Código de vinculación              ]
[ Conectar ]          (o [ Desconectar ])
Sensores simulados
Distancia: 100 cm
[====●==========================] 2–200 cm
Inclinación: 0.0°
[●==============================] 0–30°
(○) Simular falla de sensor
Estado calculado: Normal
Distancia / Variación / Inclinación
[Aviso: orden recibida, código rechazado...]
```

- **Datos de conexión:** el código acepta minúsculas y guion. Si la IP no tiene puerto, se usa el 8888.
- **Referencia:** la posición de los deslizadores al conectar es la referencia inicial, como la calibración del ESP32.
- **Envío:** la estación manda una lectura por segundo y reconecta sola si se pierde el enlace.
- **Código rechazado:** si el monitor lo rechaza, la estación deja de intentar y muestra el aviso.

## Flujo y límites

- **Primera apertura:** crear cuenta local → Inicio → Estado → Detalle → volver a Estado.
- **Accesos posteriores:** iniciar sesión → consultar.
- **Enlace:**
  - Monitor: «Iniciar receptor».
  - Estación: escribir la IP y el código → «Conectar».
  - Después: lecturas, órdenes e historial.
- **Cerrar sesión:** devuelve al acceso y detiene el enlace.
- **Cuenta y sesión:** la cuenta local se conserva; la sesión solo vive en memoria.
- **Recuperación de contraseña:** no existe en este avance.
- **Lecturas simuladas:** las genera el teléfono estación con los deslizadores. El ESP32 todavía no transmite a la app.
- **Más detalles:** el protocolo, las reglas y los límites del enlace están en `claude_ENLACE_WIFI.md`.
