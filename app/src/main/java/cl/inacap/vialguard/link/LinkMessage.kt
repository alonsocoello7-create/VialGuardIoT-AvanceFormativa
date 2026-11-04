package cl.inacap.vialguard.link

import com.google.gson.JsonObject
import com.google.gson.JsonParser

class InvalidMessageException(message: String) : Exception(message)

enum class CommandKind { ACKNOWLEDGE_ALARM, RECALIBRATE }

/** Mensajes que viajan dentro del canal cifrado. */
sealed interface LinkMessage {
    data class Reading(
        val station: String,
        val distanceCm: Double,
        val tiltDegrees: Double,
        val baseCm: Double,
        val deltaCm: Double,
        val state: AlarmState,
        val dataValid: Boolean,
        val recoverySeconds: Int,
        val acknowledged: Boolean
    ) : LinkMessage

    data class Command(val kind: CommandKind) : LinkMessage

    /** El monitor responde a cada lectura para que la estación detecte si dejó de escucharla. */
    data object Heartbeat : LinkMessage
}

object LinkCodec {
    private const val MAX_STATION_LENGTH = 40
    private const val MAX_DISTANCE_CM = 1000.0
    private const val MAX_DEGREES = 180.0

    fun encode(message: LinkMessage): ByteArray {
        val json = JsonObject()
        when (message) {
            is LinkMessage.Reading -> {
                json.addProperty("type", "reading")
                json.addProperty("station", message.station)
                json.addProperty("distanceCm", message.distanceCm)
                json.addProperty("tiltDegrees", message.tiltDegrees)
                json.addProperty("baseCm", message.baseCm)
                json.addProperty("deltaCm", message.deltaCm)
                json.addProperty("state", message.state.name)
                json.addProperty("dataValid", message.dataValid)
                json.addProperty("recoverySeconds", message.recoverySeconds)
                json.addProperty("acknowledged", message.acknowledged)
            }
            is LinkMessage.Command -> {
                json.addProperty("type", "command")
                json.addProperty("kind", message.kind.name)
            }
            LinkMessage.Heartbeat -> json.addProperty("type", "heartbeat")
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    /** Autenticar un mensaje no garantiza que sus valores tengan sentido: se revisan igual. */
    fun decode(bytes: ByteArray): LinkMessage = try {
        val json = JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
        when (json.get("type").asString) {
            "reading" -> LinkMessage.Reading(
                station = json.get("station").asString.also {
                    check(it.isNotBlank() && it.length <= MAX_STATION_LENGTH) { "estación" }
                },
                distanceCm = json.bounded("distanceCm", MAX_DISTANCE_CM),
                tiltDegrees = json.bounded("tiltDegrees", MAX_DEGREES),
                baseCm = json.bounded("baseCm", MAX_DISTANCE_CM),
                deltaCm = json.bounded("deltaCm", MAX_DISTANCE_CM),
                state = AlarmState.valueOf(json.get("state").asString),
                dataValid = json.get("dataValid").asBoolean,
                recoverySeconds = json.get("recoverySeconds").asInt.also {
                    check(it in 0..(AlarmEvaluator.RETENTION_MILLIS / 1000)) { "recuperación" }
                },
                acknowledged = json.get("acknowledged").asBoolean
            )
            "command" -> LinkMessage.Command(CommandKind.valueOf(json.get("kind").asString))
            "heartbeat" -> LinkMessage.Heartbeat
            else -> error("tipo")
        }
    } catch (e: RuntimeException) {
        throw InvalidMessageException("Mensaje con formato o valores no válidos.")
    }

    private fun JsonObject.bounded(name: String, max: Double): Double {
        val value = get(name).asDouble
        check(value.isFinite() && value in 0.0..max) { name }
        return value
    }
}
