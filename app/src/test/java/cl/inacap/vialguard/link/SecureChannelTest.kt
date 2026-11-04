package cl.inacap.vialguard.link

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class SecureChannelTest {
    private val code = "ABCD2345"
    private val pool = Executors.newCachedThreadPool()

    // Un extremo rechazado queda esperando datos que nunca llegarán; se interrumpe al terminar.
    @After fun closePool() { pool.shutdownNow() }

    /** Une dos extremos en memoria y registra lo que la estación envía, como lo vería alguien en la red. */
    private class Ends(val station: SecureChannel, val monitor: SecureChannel, val stationWire: ByteArrayOutputStream)

    private fun handshake(stationCode: String = code, monitorCode: String = code): Ends {
        val toMonitor = PipedInputStream(1 shl 16)
        val toStation = PipedInputStream(1 shl 16)
        val wire = ByteArrayOutputStream()
        val stationOut = object : PipedOutputStream(toMonitor) {
            override fun write(b: ByteArray, off: Int, len: Int) { wire.write(b, off, len); super.write(b, off, len) }
        }
        val monitorOut = PipedOutputStream(toStation)
        val monitor = pool.submit(Callable { SecureChannel.accept(toMonitor, monitorOut, monitorCode) })
        val station = pool.submit(Callable { SecureChannel.connect(toStation, stationOut, stationCode) })
        return Ends(unwrap { station.get() }, unwrap { monitor.get() }, wire)
    }

    private fun <T> unwrap(get: () -> T): T = try { get() } catch (e: java.util.concurrent.ExecutionException) { throw e.cause!! }

    private val reading = LinkMessage.Reading("Puente", 80.0, 1.5, 100.0, 20.0, AlarmState.ALARM, true, 0, false)

    @Test fun `con el mismo codigo los mensajes llegan en ambos sentidos`() {
        val pair = handshake()
        pair.station.send(reading)
        assertEquals(reading, pair.monitor.receive())
        pair.monitor.send(LinkMessage.Command(CommandKind.ACKNOWLEDGE_ALARM))
        assertEquals(LinkMessage.Command(CommandKind.ACKNOWLEDGE_ALARM), pair.station.receive())
    }

    @Test fun `el codigo acepta guiones y minusculas`() {
        val pair = handshake(stationCode = "abcd-2345")
        pair.station.send(LinkMessage.Heartbeat)
        assertEquals(LinkMessage.Heartbeat, pair.monitor.receive())
    }

    @Test fun `un codigo incorrecto se rechaza`() {
        assertThrows(AuthenticationException::class.java) { handshake(stationCode = "ZZZZ9999") }
    }

    @Test fun `ni el codigo ni los valores viajan en claro`() {
        val pair = handshake()
        pair.station.send(reading)
        pair.monitor.receive()
        val wire = String(pair.stationWire.toByteArray(), Charsets.ISO_8859_1)
        assertFalse(wire.contains(code))
        assertFalse(wire.contains("Puente"))
        assertFalse(wire.contains("distanceCm"))
    }

    @Test fun `un mensaje alterado se rechaza`() {
        val tampered = relay { frame -> frame.also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() } }
        assertThrows(TamperedFrameException::class.java) { tampered.receive() }
    }

    @Test fun `un mensaje repetido se rechaza`() {
        val replayed = relay { frame -> frame + frame }
        replayed.receive()
        assertThrows(TamperedFrameException::class.java) { replayed.receive() }
    }

    /** Pasa el tráfico de la estación por un intermediario que puede modificar el primer mensaje. */
    private fun relay(attack: (ByteArray) -> ByteArray): SecureChannel {
        val toMonitor = PipedInputStream(1 shl 16)
        val toStation = PipedInputStream(1 shl 16)
        val wireOut = PipedOutputStream(toMonitor)
        val captured = ByteArrayOutputStream()
        var intercept = false
        val stationOut = object : java.io.OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (intercept) captured.write(b, off, len) else { wireOut.write(b, off, len); wireOut.flush() }
            }
        }
        val monitorOut = PipedOutputStream(toStation)
        val monitor = pool.submit(Callable { SecureChannel.accept(toMonitor, monitorOut, code) })
        val station = pool.submit(Callable { SecureChannel.connect(toStation, stationOut, code) })
        val stationChannel = station.get()
        val monitorChannel = monitor.get()
        intercept = true
        stationChannel.send(reading)
        wireOut.write(attack(captured.toByteArray()))
        wireOut.flush()
        return monitorChannel
    }

    @Test fun `un marco con longitud absurda se rechaza sin reservar memoria`() {
        val pair = handshakeWithRawFrame { DataOutputStream(it).writeInt(Int.MAX_VALUE) }
        assertThrows(TamperedFrameException::class.java) { pair.receive() }
    }

    private fun handshakeWithRawFrame(writeFrame: (PipedOutputStream) -> Unit): SecureChannel {
        val toMonitor = PipedInputStream(1 shl 16)
        val toStation = PipedInputStream(1 shl 16)
        val stationOut = PipedOutputStream(toMonitor)
        val monitorOut = PipedOutputStream(toStation)
        val monitor = pool.submit(Callable { SecureChannel.accept(toMonitor, monitorOut, code) })
        val station = pool.submit(Callable { SecureChannel.connect(toStation, stationOut, code) })
        station.get()
        val channel = monitor.get()
        writeFrame(stationOut)
        stationOut.flush()
        return channel
    }

    @Test fun `un protocolo desconocido se rechaza`() {
        assertThrows(IOException::class.java) {
            SecureChannel.accept(ByteArrayInputStream("GET / HTTP/1.1\r\n".toByteArray()), ByteArrayOutputStream(), code)
        }
    }

    @Test fun `el codigo generado tiene formato valido y no se repite`() {
        val codes = (1..50).map { PairingCode.generate() }
        assertTrue(codes.all(PairingCode::isWellFormed))
        assertTrue(codes.toSet().size > 45)
        assertFalse(PairingCode.isWellFormed("ABC"))
        assertFalse(PairingCode.isWellFormed("ABCD0000"))
        assertEquals("ABCD-2345", PairingCode.display("ABCD2345"))
    }

    @Test fun `el codec rechaza valores fuera de rango y json invalido`() {
        val valid = String(LinkCodec.encode(reading), Charsets.UTF_8)
        assertEquals(reading, LinkCodec.decode(valid.toByteArray()))
        listOf(
            "no es json",
            "{}",
            """{"type":"otro"}""",
            """{"type":"command","kind":"BORRAR_TODO"}""",
            valid.replace("\"distanceCm\":80.0", "\"distanceCm\":-1"),
            valid.replace("\"distanceCm\":80.0", "\"distanceCm\":1e9"),
            valid.replace("\"state\":\"ALARM\"", "\"state\":\"OK\""),
            valid.replace("\"recoverySeconds\":0", "\"recoverySeconds\":9999")
        ).forEach { bad ->
            assertThrows(bad, InvalidMessageException::class.java) { LinkCodec.decode(bad.toByteArray()) }
        }
    }
}
