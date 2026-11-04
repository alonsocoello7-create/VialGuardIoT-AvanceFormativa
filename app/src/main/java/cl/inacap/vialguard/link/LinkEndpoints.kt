package cl.inacap.vialguard.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

const val DEFAULT_LINK_PORT = 8888
private const val CONNECT_TIMEOUT_MILLIS = 4_000
private const val SILENCE_TIMEOUT_MILLIS = 5_000
private const val SEND_PERIOD_MILLIS = 1_000L
private const val RETRY_DELAY_MILLIS = 2_000L
private const val MAX_FAILED_ATTEMPTS = 5

/** Los sockets bloquean sin atender la cancelación: cerrarlos es lo que libera al hilo. */
private suspend fun <T> closingOnCancel(resource: Closeable, block: suspend CoroutineScope.() -> T): T = coroutineScope {
    val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { runCatching { resource.close() } }
    }
    try { block() } finally { watcher.cancel() }
}

sealed interface MonitorEvent {
    data class Listening(val port: Int) : MonitorEvent
    data object StationConnected : MonitorEvent
    data class StationLost(val reason: String) : MonitorEvent
    data object AttemptRejected : MonitorEvent
    data class Received(val reading: LinkMessage.Reading) : MonitorEvent
    /** Demasiados códigos erróneos: el código actual deja de servir y hay que generar otro. */
    data object CodeExhausted : MonitorEvent
}

/** Escucha a una estación por vez. Termina al cancelarse o al agotar los intentos del código. */
class MonitorEndpoint(private val code: String, private val onEvent: (MonitorEvent) -> Unit) {
    @Volatile private var channel: SecureChannel? = null

    /** @return false si no hay una estación conectada. */
    fun send(command: LinkMessage.Command): Boolean {
        val current = channel ?: return false
        return try { current.send(command); true } catch (e: IOException) { false }
    }

    suspend fun run(port: Int = DEFAULT_LINK_PORT): Unit = withContext(Dispatchers.IO) {
        ServerSocket(port).use { server ->
            closingOnCancel(server) {
                onEvent(MonitorEvent.Listening(server.localPort))
                var failures = 0
                while (isActive && failures < MAX_FAILED_ATTEMPTS) {
                    val socket = try { server.accept() } catch (e: IOException) { break }
                    closingOnCancel<Unit>(socket) {
                        try {
                            socket.soTimeout = SILENCE_TIMEOUT_MILLIS
                            // Los intentos fallidos se acumulan durante toda la vigencia del código, aunque
                            // entre medio se conecte la estación legítima.
                            val secure = SecureChannel.accept(socket.getInputStream(), socket.getOutputStream(), code)
                            channel = secure
                            onEvent(MonitorEvent.StationConnected)
                            while (true) {
                                val message = secure.receive()
                                if (message is LinkMessage.Reading) {
                                    onEvent(MonitorEvent.Received(message))
                                    secure.send(LinkMessage.Heartbeat)
                                }
                            }
                        } catch (e: AuthenticationException) {
                            failures++
                            onEvent(MonitorEvent.AttemptRejected)
                        } catch (e: InvalidMessageException) {
                            onEvent(MonitorEvent.StationLost(e.message.orEmpty()))
                        } catch (e: IOException) {
                            if (channel != null) onEvent(MonitorEvent.StationLost("Se perdió la conexión con la estación."))
                        } finally {
                            channel = null
                            runCatching { socket.close() }
                        }
                    }
                }
                if (failures >= MAX_FAILED_ATTEMPTS) onEvent(MonitorEvent.CodeExhausted)
            }
        }
    }
}

sealed interface StationEvent {
    data object Connecting : StationEvent
    data object Connected : StationEvent
    data class Lost(val reason: String) : StationEvent
    data object CodeRejected : StationEvent
    data class CommandReceived(val kind: CommandKind) : StationEvent
}

/** Se conecta al monitor y publica lecturas cada segundo, reconectando si se pierde el enlace. */
class StationEndpoint(
    private val host: String,
    private val port: Int,
    private val code: String,
    private val nextReading: () -> LinkMessage.Reading,
    private val onEvent: (StationEvent) -> Unit
) {
    suspend fun run(): Unit = withContext(Dispatchers.IO) {
        while (isActive) {
            onEvent(StationEvent.Connecting)
            try {
                session()
            } catch (e: AuthenticationException) {
                // Reintentar con el mismo código solo gastaría los intentos del monitor.
                onEvent(StationEvent.CodeRejected)
                return@withContext
            } catch (e: IOException) {
                onEvent(StationEvent.Lost("Sin conexión con el monitor. Reintentando…"))
            } catch (e: InvalidMessageException) {
                onEvent(StationEvent.Lost(e.message.orEmpty()))
            }
            delay(RETRY_DELAY_MILLIS)
        }
    }

    private suspend fun session() {
        Socket().use { socket ->
            closingOnCancel(socket) {
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS)
                socket.soTimeout = SILENCE_TIMEOUT_MILLIS
                val secure = SecureChannel.connect(socket.getInputStream(), socket.getOutputStream(), code)
                onEvent(StationEvent.Connected)
                launch {
                    while (true) {
                        val message = secure.receive()
                        if (message is LinkMessage.Command) onEvent(StationEvent.CommandReceived(message.kind))
                    }
                }
                while (true) {
                    secure.send(nextReading())
                    delay(SEND_PERIOD_MILLIS)
                }
            }
        }
    }
}
