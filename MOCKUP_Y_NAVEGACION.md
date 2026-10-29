# Bosquejo de la interfaz VialGuard

Diseño textual de las pantallas presentes en el avance. Es un bosquejo para revisar con el docente, no una captura de ejecución ni una validación recibida. Se mantiene la interfaz Compose y Material 3 del código.

## Acceso y primera apertura

```text
VialGuard IoT
Avance formativo VialGuard
Primera apertura: crea tu cuenta local...
[ Usuario de demostración: operador     ]
[ Contraseña: ••••••••••                ]
[ Crear cuenta local                   ]
Credenciales protegidas localmente.
Firebase queda pendiente de integración.
[Progreso durante la operación]
[Mensaje de validación o error]
```

Cuando ya existe una cuenta, el botón pasa a «Iniciar sesión». La contraseña se oculta. Durante la operación se deshabilita el botón y se muestra progreso. Las pantallas de consulta aparecen tras autenticar. Cinco fallos producen un bloqueo temporal de un minuto; la app informa del bloqueo al intentar acceder durante ese período.

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
          Inicio       Estado
```

La búsqueda encuentra coincidencias en nombre o ruta, sin distinguir mayúsculas. Cada tarjeta muestra nombre, tipo, ruta y estado de ejemplo. Tocar una tarjeta abre Detalle. Una búsqueda sin coincidencias muestra «No hay estructuras con ese filtro».

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
          Inicio       Estado
```

El filtro de tipo y la búsqueda se combinan. El resumen numérico corresponde al conjunto completo de ejemplos, no al número de resultados filtrados. «Todas» incluye también Acceso. No representa una conexión activa ni el estado medido de un puente real.

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
          Inicio       Estado
```

Las dos lecturas se muestran únicamente en el ejemplo Puente. Las otras tarjetas indican que el detalle es de ejemplo y la comunicación IoT está pendiente. Se puede volver con «Volver a Estado» o con las pestañas inferiores. La interfaz permite desplazamiento vertical para acceder a contenido que no quepa en la pantalla.

## Flujo y límites

Primera apertura → crear cuenta local → Inicio → Estado → Detalle → volver a Estado. En posteriores accesos: iniciar sesión → consultar. Cerrar sesión devuelve al acceso. La cuenta local se conserva; la sesión solo vive en memoria. No hay recuperación de contraseña en este avance.

La comunicación futura se explica en `EXPLICACION_PARA_LA_FORMATIVA.md`. No hay botones que envíen órdenes a dispositivos ni indicadores de enlace activo. Carga, pérdida de red y lecturas recibidas de dispositivos son estados de una integración posterior, no comportamientos implementados aquí.
