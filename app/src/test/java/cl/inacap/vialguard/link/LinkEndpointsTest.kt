package cl.inacap.vialguard.link

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** Monitor y estación reales conversando por TCP en la interfaz local. */
class LinkEndpointsTest {
    private val code = "ABCD2345"
    private val reading = LinkMessage.Reading("Puente", 80.0, 0.0, 100.0, 20.0, AlarmState.ALARM, true, 0, false)

    private fun freePort() = ServerSocket(0).use { it.localPort }

    @Test fun `la lectura llega al monitor y la orden vuelve a la estacion`() = runBlocking {
        withTimeout(30_000) {
            val port = freePort()
            val received = CompletableDeferred<LinkMessage.Reading>()
            val command = CompletableDeferred<CommandKind>()
            val listening = CompletableDeferred<Unit>()
            val monitor = MonitorEndpoint(code) { event ->
                if (event is MonitorEvent.Listening) listening.complete(Unit)
                if (event is MonitorEvent.Received) received.complete(event.reading)
            }
            val monitorJob = launch(Dispatchers.IO) { monitor.run(port) }
            listening.await()
            val station = StationEndpoint("127.0.0.1", port, code, { reading }) { event ->
                if (event is StationEvent.CommandReceived) command.complete(event.kind)
            }
            val stationJob = launch(Dispatchers.IO) { station.run() }

            assertEquals(reading, received.await())
            assertTrue(monitor.send(LinkMessage.Command(CommandKind.ACKNOWLEDGE_ALARM)))
            assertEquals(CommandKind.ACKNOWLEDGE_ALARM, command.await())

            stationJob.cancelAndJoin()
            monitorJob.cancelAndJoin()
        }
    }

    @Test fun `el monitor sigue escuchando y la estacion reconecta tras perder el enlace`() = runBlocking {
        withTimeout(30_000) {
            val port = freePort()
            val listening = CompletableDeferred<Unit>()
            val lost = CompletableDeferred<Unit>()
            val secondConnection = CompletableDeferred<Unit>()
            val connections = AtomicInteger()
            val firstConnection = CompletableDeferred<Unit>()
            val monitor = MonitorEndpoint(code) { event ->
                when (event) {
                    is MonitorEvent.Listening -> listening.complete(Unit)
                    MonitorEvent.StationConnected -> when (connections.incrementAndGet()) {
                        1 -> firstConnection.complete(Unit)
                        2 -> secondConnection.complete(Unit)
                    }
                    is MonitorEvent.StationLost -> lost.complete(Unit)
                    else -> Unit
                }
            }
            val monitorJob = launch(Dispatchers.IO) { monitor.run(port) }
            listening.await()
            val first = launch(Dispatchers.IO) { StationEndpoint("127.0.0.1", port, code, { reading }) {}.run() }
            firstConnection.await()
            first.cancelAndJoin()
            lost.await()
            val second = launch(Dispatchers.IO) { StationEndpoint("127.0.0.1", port, code, { reading }) {}.run() }
            secondConnection.await()
            second.cancelAndJoin()
            monitorJob.cancelAndJoin()
        }
    }

    @Test fun `cinco codigos incorrectos agotan el receptor`() = runBlocking {
        withTimeout(60_000) {
            val port = freePort()
            val listening = CompletableDeferred<Unit>()
            val events = CopyOnWriteArrayList<MonitorEvent>()
            val monitorJob = launch(Dispatchers.IO) {
                MonitorEndpoint(code) { event ->
                    events += event
                    if (event is MonitorEvent.Listening) listening.complete(Unit)
                }.run(port)
            }
            listening.await()
            repeat(5) {
                val rejected = CompletableDeferred<Unit>()
                val attempt = launch(Dispatchers.IO) {
                    StationEndpoint("127.0.0.1", port, "ZZZZ9999", { reading }) { event ->
                        if (event == StationEvent.CodeRejected) rejected.complete(Unit)
                    }.run()
                }
                rejected.await()
                attempt.join()
            }
            monitorJob.join()
            assertEquals(5, events.count { it == MonitorEvent.AttemptRejected })
            assertTrue(events.last() == MonitorEvent.CodeExhausted)
        }
    }

    @Test fun `una estacion con el codigo incorrecto no recibe ordenes ni entrega lecturas`() = runBlocking {
        withTimeout(30_000) {
            val port = freePort()
            val listening = CompletableDeferred<Unit>()
            val events = CopyOnWriteArrayList<MonitorEvent>()
            val monitor = MonitorEndpoint(code) { event ->
                events += event
                if (event is MonitorEvent.Listening) listening.complete(Unit)
            }
            val monitorJob = launch(Dispatchers.IO) { monitor.run(port) }
            listening.await()
            StationEndpoint("127.0.0.1", port, "ZZZZ9999", { reading }) {}.run()
            assertTrue(events.none { it is MonitorEvent.Received || it == MonitorEvent.StationConnected })
            assertEquals(false, monitor.send(LinkMessage.Command(CommandKind.RECALIBRATE)))
            monitorJob.cancelAndJoin()
        }
    }
}
