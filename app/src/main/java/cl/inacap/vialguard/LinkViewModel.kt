package cl.inacap.vialguard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.vialguard.link.AlarmEvaluator
import cl.inacap.vialguard.link.CommandKind
import cl.inacap.vialguard.link.DEFAULT_LINK_PORT
import cl.inacap.vialguard.link.EventHistory
import cl.inacap.vialguard.link.HistoryEntry
import cl.inacap.vialguard.link.LinkMessage
import cl.inacap.vialguard.link.MonitorEndpoint
import cl.inacap.vialguard.link.MonitorEvent
import cl.inacap.vialguard.link.PairingCode
import cl.inacap.vialguard.link.StationEndpoint
import cl.inacap.vialguard.link.StationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

enum class LinkRole { MONITOR, STATION }

data class LinkState(
    val role: LinkRole = LinkRole.MONITOR,
    val running: Boolean = false,
    val connected: Boolean = false,
    val status: String = "Enlace detenido.",
    val notice: String = "",
    val addresses: List<String> = emptyList(),
    val port: Int = DEFAULT_LINK_PORT,
    val pairingCode: String = "",
    val reading: LinkMessage.Reading? = null,
    val readingAgeSeconds: Long? = null,
    val history: List<HistoryEntry> = emptyList(),
    val target: String = "",
    val codeInput: String = "",
    val distanceCm: Float = 100f,
    val tiltDegrees: Float = 0f,
    val sensorFailure: Boolean = false,
    val local: LinkMessage.Reading? = null
)

/** Coordina los dos roles del enlace Wi-Fi. La estación simula los sensores; aún no hay ESP32 conectado. */
class LinkViewModel(application: Application) : AndroidViewModel(application) {
    private val history = EventHistory(application)
    private val mutable = MutableStateFlow(LinkState())
    val state = mutable.asStateFlow()

    private var linkJob: Job? = null
    private var tickerJob: Job? = null
    @Volatile private var monitor: MonitorEndpoint? = null
    // Los sockets avisan desde otros hilos; un aviso tardío de un enlace ya detenido se descarta.
    @Volatile private var runId = 0
    @Volatile private var lastReadingAt = 0L
    private var operator = ""

    fun setOperator(user: String) { operator = user }
    fun role(value: LinkRole) { if (!mutable.value.running) mutable.update { it.copy(role = value, notice = "", status = "Enlace detenido.") } }
    fun target(value: String) { mutable.update { it.copy(target = value, notice = "") } }
    fun codeInput(value: String) { mutable.update { it.copy(codeInput = value, notice = "") } }
    fun distance(value: Float) { mutable.update { it.copy(distanceCm = value) } }
    fun tilt(value: Float) { mutable.update { it.copy(tiltDegrees = value) } }
    fun sensorFailure(value: Boolean) { mutable.update { it.copy(sensorFailure = value) } }

    fun startMonitor() {
        if (mutable.value.running) return
        val code = PairingCode.generate()
        lastReadingAt = 0L
        mutable.update {
            it.copy(running = true, connected = false, pairingCode = code, reading = null, readingAgeSeconds = null,
                addresses = localAddresses(), port = DEFAULT_LINK_PORT, notice = "", status = "Iniciando receptor…")
        }
        refreshHistory()
        val id = ++runId
        val endpoint = MonitorEndpoint(code) { if (runId == id) onMonitorEvent(it) }
        monitor = endpoint
        linkJob = viewModelScope.launch {
            try {
                endpoint.run(DEFAULT_LINK_PORT)
            } catch (e: IOException) {
                mutable.update { it.copy(notice = "No se pudo abrir el puerto $DEFAULT_LINK_PORT.") }
            }
            if (runId == id) finishRun()
        }
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (lastReadingAt != 0L) mutable.update {
                    it.copy(readingAgeSeconds = (System.currentTimeMillis() - lastReadingAt) / 1000)
                }
            }
        }
    }

    private fun onMonitorEvent(event: MonitorEvent) {
        when (event) {
            is MonitorEvent.Listening -> mutable.update { it.copy(status = "Esperando estación en el puerto ${event.port}.") }
            MonitorEvent.StationConnected -> {
                record("Estación vinculada.")
                mutable.update { it.copy(connected = true, notice = "", status = "Estación conectada.") }
            }
            is MonitorEvent.StationLost -> {
                record("Enlace perdido: ${event.reason}")
                mutable.update { it.copy(connected = false, status = "Desconectado. ${event.reason} Esperando reconexión.") }
            }
            MonitorEvent.AttemptRejected -> {
                record("Intento de vinculación rechazado.")
                mutable.update { it.copy(notice = "Se rechazó un intento con un código incorrecto.") }
            }
            is MonitorEvent.Received -> {
                val previous = mutable.value.reading
                if (previous?.state != event.reading.state) {
                    record("Estado ${event.reading.state.label}: ${"%.1f".format(event.reading.distanceCm)} cm, " +
                        "${"%.1f".format(event.reading.tiltDegrees)}°" + if (event.reading.dataValid) "" else ", datos no válidos")
                }
                lastReadingAt = System.currentTimeMillis()
                mutable.update { it.copy(reading = event.reading, readingAgeSeconds = 0) }
            }
            MonitorEvent.CodeExhausted -> {
                record("Receptor detenido por exceso de códigos incorrectos.")
                mutable.update { it.copy(notice = "Demasiados intentos con código incorrecto. Inicia el receptor para generar otro código.") }
            }
        }
    }

    fun sendCommand(kind: CommandKind) {
        val endpoint = monitor ?: return
        viewModelScope.launch {
            val sent = withContext(Dispatchers.IO) { endpoint.send(LinkMessage.Command(kind)) }
            val name = if (kind == CommandKind.ACKNOWLEDGE_ALARM) "Reconocer alarma" else "Recalibrar"
            if (sent) record("Orden enviada por $operator: $name.")
            mutable.update { it.copy(notice = if (sent) "Orden enviada: $name." else "No hay una estación conectada.") }
        }
    }

    fun startStation() {
        val s = mutable.value
        if (s.running) return
        val endpoint = parseTarget(s.target)
        if (endpoint == null) {
            mutable.update { it.copy(notice = "Escribe la IP del monitor, por ejemplo 192.168.1.20 o 192.168.1.20:8888.") }
            return
        }
        if (!PairingCode.isWellFormed(s.codeInput)) {
            mutable.update { it.copy(notice = "El código tiene 8 caracteres, como aparece en el monitor.") }
            return
        }
        // La referencia inicial es la posición de los controles al conectar, como la calibración del ESP32.
        val evaluator = AlarmEvaluator(s.distanceCm.toDouble(), s.tiltDegrees.toDouble())
        mutable.update { it.copy(running = true, connected = false, local = null, notice = "", status = "Conectando…") }
        val id = ++runId
        val station = StationEndpoint(endpoint.first, endpoint.second, s.codeInput, { nextReading(evaluator) }) {
            if (runId == id) onStationEvent(evaluator, it)
        }
        linkJob = viewModelScope.launch {
            station.run()
            if (runId == id) finishRun()
        }
    }

    private fun nextReading(active: AlarmEvaluator): LinkMessage.Reading {
        val s = mutable.value
        val distance = s.distanceCm.toDouble()
        val tilt = s.tiltDegrees.toDouble()
        val result = active.update(!s.sensorFailure, distance, tilt, System.currentTimeMillis())
        val reading = LinkMessage.Reading(
            station = "Puente de demostración",
            distanceCm = distance,
            tiltDegrees = tilt,
            baseCm = active.currentBaseCm,
            deltaCm = result.deltaCm,
            state = result.state,
            dataValid = !s.sensorFailure,
            recoverySeconds = result.recoverySeconds,
            acknowledged = result.acknowledged
        )
        mutable.update { it.copy(local = reading) }
        return reading
    }

    private fun onStationEvent(active: AlarmEvaluator, event: StationEvent) {
        when (event) {
            StationEvent.Connecting -> mutable.update { it.copy(status = "Conectando…", connected = false) }
            StationEvent.Connected -> mutable.update { it.copy(status = "Conectada al monitor. Enviando lecturas.", connected = true) }
            is StationEvent.Lost -> mutable.update { it.copy(status = event.reason, connected = false) }
            StationEvent.CodeRejected -> mutable.update { it.copy(notice = "El monitor rechazó el código. Revísalo e intenta de nuevo.") }
            is StationEvent.CommandReceived -> {
                val s = mutable.value
                val text = when (event.kind) {
                    CommandKind.ACKNOWLEDGE_ALARM ->
                        if (active.acknowledge()) "El monitor reconoció la alarma. La alarma sigue activa hasta recuperarse."
                        else "El monitor intentó reconocer una alarma, pero no hay ninguna activa."
                    CommandKind.RECALIBRATE ->
                        if (active.recalibrate(s.distanceCm.toDouble(), s.tiltDegrees.toDouble())) "El monitor recalibró la referencia."
                        else "Recalibración rechazada: hay una alarma activa."
                }
                mutable.update { it.copy(notice = text) }
            }
        }
    }

    fun stop() {
        runId++
        linkJob?.cancel()
        finishRun()
    }

    private fun finishRun() {
        tickerJob?.cancel()
        monitor = null
        mutable.update { it.copy(running = false, connected = false, status = "Enlace detenido.", readingAgeSeconds = null) }
    }

    private fun record(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            history.add(System.currentTimeMillis(), text)
            refreshHistoryNow()
        }
    }

    private fun refreshHistory() { viewModelScope.launch(Dispatchers.IO) { refreshHistoryNow() } }

    private fun refreshHistoryNow() {
        val entries = history.recent()
        mutable.update { it.copy(history = entries) }
    }

    private fun parseTarget(raw: String): Pair<String, Int>? {
        val parts = raw.trim().split(":")
        val host = parts[0]
        val port = if (parts.size == 2) parts[1].toIntOrNull() else if (parts.size == 1) DEFAULT_LINK_PORT else null
        return if (host.isNotEmpty() && port != null && port in 1..65535) host to port else null
    }

    private fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { it.isSiteLocalAddress }
            .mapNotNull { it.hostAddress }
    }.getOrDefault(emptyList())

    override fun onCleared() {
        history.close()
    }
}
