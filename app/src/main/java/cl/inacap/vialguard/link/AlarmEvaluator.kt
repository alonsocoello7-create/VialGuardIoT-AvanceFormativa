package cl.inacap.vialguard.link

import kotlin.math.abs
import kotlin.math.ceil

enum class AlarmState(val label: String) {
    NORMAL("Normal"), CAUTION("Precaución"), ALARM("Alarma")
}

data class Evaluation(
    val state: AlarmState,
    val deltaCm: Double,
    val deltaDegrees: Double,
    val recoverySeconds: Int,
    val acknowledged: Boolean
)

/**
 * Mismos umbrales que el firmware `VialGuard_ESP32_final/sketch.ino`: 15 cm o 5° activan la alarma
 * (paso cortado), el 70 % activa precaución y la alarma se retiene hasta 30 s de lecturas normales.
 * La vibración del firmware no se simula. Los umbrales son didácticos, no criterios de ingeniería.
 */
class AlarmEvaluator(private var baseCm: Double, private var baseDegrees: Double) {
    private var state = AlarmState.NORMAL
    private var normalSince: Long? = null
    private var acknowledged = false
    private var lastDeltaCm = 0.0
    private var lastDeltaDegrees = 0.0

    val currentBaseCm get() = baseCm

    @Synchronized
    fun update(valid: Boolean, distanceCm: Double, degrees: Double, nowMillis: Long): Evaluation {
        lastDeltaCm = if (valid) abs(distanceCm - baseCm) else 0.0
        lastDeltaDegrees = if (valid) abs(degrees - baseDegrees) else 0.0
        val danger = !valid || lastDeltaCm >= DISTANCE_LIMIT_CM || lastDeltaDegrees >= TILT_LIMIT_DEGREES
        val caution = lastDeltaCm >= CAUTION_RATIO * DISTANCE_LIMIT_CM ||
            lastDeltaDegrees >= CAUTION_RATIO * TILT_LIMIT_DEGREES
        when {
            danger -> { state = AlarmState.ALARM; normalSince = null }
            state == AlarmState.ALARM -> {
                val since = normalSince
                when {
                    caution -> normalSince = null
                    since == null -> normalSince = nowMillis
                    nowMillis - since >= RETENTION_MILLIS -> {
                        state = AlarmState.NORMAL; normalSince = null; acknowledged = false
                    }
                }
            }
            else -> state = if (caution) AlarmState.CAUTION else AlarmState.NORMAL
        }
        val remaining = normalSince?.let { ceil((RETENTION_MILLIS - (nowMillis - it)) / 1000.0).toInt() } ?: 0
        return Evaluation(state, lastDeltaCm, lastDeltaDegrees, remaining, acknowledged)
    }

    /** Reconocer deja constancia de que alguien vio la alarma. No la apaga ni acorta la retención. */
    @Synchronized
    fun acknowledge(): Boolean {
        if (state != AlarmState.ALARM) return false
        acknowledged = true
        return true
    }

    /** Una orden remota no puede borrar una alarma cambiando la referencia contra la que se compara. */
    @Synchronized
    fun recalibrate(distanceCm: Double, degrees: Double): Boolean {
        if (state == AlarmState.ALARM) return false
        baseCm = distanceCm
        baseDegrees = degrees
        return true
    }

    companion object {
        const val DISTANCE_LIMIT_CM = 15.0
        const val TILT_LIMIT_DEGREES = 5.0
        const val CAUTION_RATIO = 0.7
        const val RETENTION_MILLIS = 30_000L
    }
}
