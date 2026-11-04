package cl.inacap.vialguard.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEvaluatorTest {
    private val evaluator = AlarmEvaluator(100.0, 0.0)

    private fun state(distance: Double, degrees: Double = 0.0, now: Long = 0, valid: Boolean = true) =
        evaluator.update(valid, distance, degrees, now).state

    @Test fun `reposo es normal`() = assertEquals(AlarmState.NORMAL, state(100.0))

    @Test fun `el 70 por ciento del umbral es precaucion`() {
        assertEquals(AlarmState.NORMAL, state(90.0))
        assertEquals(AlarmState.CAUTION, state(89.0))
        assertEquals(AlarmState.CAUTION, state(100.0, 3.6))
        assertEquals(AlarmState.CAUTION, state(110.6))
    }

    @Test fun `el umbral activa la alarma en ambos sentidos y por inclinacion`() {
        assertEquals(AlarmState.ALARM, AlarmEvaluator(100.0, 0.0).update(true, 85.0, 0.0, 0).state)
        assertEquals(AlarmState.ALARM, AlarmEvaluator(100.0, 0.0).update(true, 115.0, 0.0, 0).state)
        assertEquals(AlarmState.ALARM, AlarmEvaluator(100.0, 0.0).update(true, 100.0, 5.0, 0).state)
    }

    @Test fun `datos invalidos activan la alarma`() = assertEquals(AlarmState.ALARM, state(100.0, valid = false))

    @Test fun `la alarma se retiene 30 segundos y luego se recupera`() {
        state(80.0, now = 0)
        assertEquals(AlarmState.ALARM, state(100.0, now = 1_000))
        assertEquals(30, evaluator.update(true, 100.0, 0.0, 1_000).recoverySeconds)
        assertEquals(AlarmState.ALARM, state(100.0, now = 30_999))
        assertEquals(AlarmState.NORMAL, state(100.0, now = 31_000))
    }

    @Test fun `una precaucion durante la recuperacion reinicia la cuenta`() {
        state(80.0, now = 0)
        state(100.0, now = 1_000)
        assertEquals(AlarmState.ALARM, state(89.0, now = 20_000))
        assertEquals(AlarmState.ALARM, state(100.0, now = 21_000))
        assertEquals(AlarmState.ALARM, state(100.0, now = 50_999))
        assertEquals(AlarmState.NORMAL, state(100.0, now = 51_000))
    }

    @Test fun `reconocer no apaga la alarma ni acorta la retencion`() {
        state(80.0, now = 0)
        assertTrue(evaluator.acknowledge())
        val after = evaluator.update(true, 80.0, 0.0, 1_000)
        assertEquals(AlarmState.ALARM, after.state)
        assertTrue(after.acknowledged)
    }

    @Test fun `el reconocimiento no se arrastra a la siguiente alarma`() {
        state(80.0, now = 0)
        evaluator.acknowledge()
        state(100.0, now = 1_000)
        state(100.0, now = 31_000)
        assertFalse(evaluator.update(true, 80.0, 0.0, 32_000).acknowledged)
    }

    @Test fun `no se reconoce una alarma inexistente`() {
        state(100.0)
        assertFalse(evaluator.acknowledge())
    }

    @Test fun `recalibrar no puede borrar una alarma activa`() {
        state(80.0, now = 0)
        assertFalse(evaluator.recalibrate(80.0, 0.0))
        assertEquals(100.0, evaluator.currentBaseCm, 0.0)
        assertEquals(AlarmState.ALARM, state(80.0, now = 1_000))
    }

    @Test fun `recalibrar sin alarma cambia la referencia`() {
        state(95.0)
        assertTrue(evaluator.recalibrate(95.0, 0.0))
        assertEquals(AlarmState.NORMAL, state(95.0))
    }
}
