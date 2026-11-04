package cl.inacap.vialguard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cl.inacap.vialguard.link.AlarmState
import cl.inacap.vialguard.link.CommandKind
import cl.inacap.vialguard.link.LinkMessage
import cl.inacap.vialguard.link.PairingCode
import java.text.DateFormat
import java.util.Date

private const val STALE_AFTER_SECONDS = 5L

/** La tipografía del proyecto fija el color del texto: dentro de un botón se usa el del botón, que se atenúa al deshabilitarlo. */
@Composable
private fun ButtonText(text: String) = Text(text, color = LocalContentColor.current)

@Composable
fun LinkScreen(vm: LinkViewModel) {
    val s by vm.state.collectAsState()
    Text("Enlace entre dos teléfonos", style = MaterialTheme.typography.headlineSmall)
    Text("Conecta ambos a la misma red Wi-Fi. Un teléfono actúa como monitor y el otro como estación.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = s.role == LinkRole.MONITOR, enabled = !s.running,
            onClick = { vm.role(LinkRole.MONITOR) }, label = { Text("Monitor") })
        FilterChip(selected = s.role == LinkRole.STATION, enabled = !s.running,
            onClick = { vm.role(LinkRole.STATION) }, label = { Text("Estación") })
    }
    Text(s.status, style = MaterialTheme.typography.titleSmall)
    if (s.role == LinkRole.MONITOR) MonitorPanel(s, vm) else StationPanel(s, vm)
    if (s.notice.isNotBlank()) Text(s.notice, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun MonitorPanel(s: LinkState, vm: LinkViewModel) {
    if (!s.running) {
        Button(onClick = vm::startMonitor) { ButtonText("Iniciar receptor") }
    } else {
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Datos para la estación", style = MaterialTheme.typography.titleMedium)
                Text(if (s.addresses.isEmpty()) "Sin dirección Wi-Fi. Revisa la conexión." else "IP: ${s.addresses.joinToString(" o ")}")
                Text("Puerto: ${s.port}")
                Text("Código: ${PairingCode.display(s.pairingCode)}", fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge)
                Text("El código cambia cada vez que inicias el receptor y no viaja por la red.")
            }
        }
        OutlinedButton(onClick = vm::stop) { ButtonText("Detener receptor") }
    }
    val reading = s.reading
    if (s.running) {
        if (reading == null) Text("Sin datos: todavía no llega ninguna lectura.")
        else ReadingCard(reading, s.readingAgeSeconds, s.connected)
    }
    if (s.running && s.connected && reading != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = reading.state == AlarmState.ALARM && !reading.acknowledged,
                onClick = { vm.sendCommand(CommandKind.ACKNOWLEDGE_ALARM) }) { ButtonText("Reconocer alarma") }
            OutlinedButton(enabled = reading.state != AlarmState.ALARM,
                onClick = { vm.sendCommand(CommandKind.RECALIBRATE) }) { ButtonText("Recalibrar") }
        }
    }
    if (s.history.isNotEmpty()) {
        Text("Historial", style = MaterialTheme.typography.titleMedium)
        val format = remember { DateFormat.getTimeInstance(DateFormat.MEDIUM) }
        s.history.forEach { Text("${format.format(Date(it.atMillis))} · ${it.text}") }
    }
}

@Composable
private fun ReadingCard(reading: LinkMessage.Reading, ageSeconds: Long?, connected: Boolean) {
    val age = ageSeconds ?: 0L
    val stale = !connected || age > STALE_AFTER_SECONDS
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(reading.station, style = MaterialTheme.typography.titleMedium)
            // Una lectura vieja no demuestra que la estructura siga igual: no se presenta como estado actual.
            if (stale) Text("Última lectura guardada, hace $age s. Estado actual desconocido.",
                color = MaterialTheme.colorScheme.error)
            else Text("Estado: ${reading.state.label}", style = MaterialTheme.typography.headlineSmall,
                color = if (reading.state == AlarmState.ALARM) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            ReadingLines(reading)
            if (reading.state == AlarmState.ALARM && reading.acknowledged) Text("Alarma reconocida. Sigue activa hasta recuperarse.")
            Text("Lecturas simuladas por la estación. No provienen de un sensor real.")
        }
    }
}

@Composable
private fun ReadingLines(reading: LinkMessage.Reading) {
    if (!reading.dataValid) {
        Text("Datos no válidos: falla del sensor.")
    } else {
        Text("Distancia: ${"%.1f".format(reading.distanceCm)} cm (referencia ${"%.1f".format(reading.baseCm)} cm)")
        Text("Variación: ${"%.1f".format(reading.deltaCm)} cm")
        Text("Inclinación: ${"%.1f".format(reading.tiltDegrees)}°")
    }
    if (reading.recoverySeconds > 0) Text("Recuperación: ${reading.recoverySeconds} s")
}

@Composable
private fun StationPanel(s: LinkState, vm: LinkViewModel) {
    if (!s.running) {
        OutlinedTextField(value = s.target, onValueChange = vm::target, label = { Text("IP del monitor") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = s.codeInput, onValueChange = vm::codeInput, label = { Text("Código de vinculación") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = vm::startStation) { ButtonText("Conectar") }
    } else {
        OutlinedButton(onClick = vm::stop) { ButtonText("Desconectar") }
    }
    Text("Sensores simulados", style = MaterialTheme.typography.titleMedium)
    Text("Distancia: ${"%.0f".format(s.distanceCm)} cm")
    Slider(value = s.distanceCm, onValueChange = vm::distance, valueRange = 2f..200f)
    Text("Inclinación: ${"%.1f".format(s.tiltDegrees)}°")
    Slider(value = s.tiltDegrees, onValueChange = vm::tilt, valueRange = 0f..30f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = s.sensorFailure, onCheckedChange = vm::sensorFailure)
        Text("Simular falla de sensor")
    }
    s.local?.let { Text("Estado calculado: ${it.state.label}", style = MaterialTheme.typography.titleMedium); ReadingLines(it) }
}
