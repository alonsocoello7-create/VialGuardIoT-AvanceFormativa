package cl.inacap.vialguard.link

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** El otro extremo no demostró conocer el código de vinculación. */
class AuthenticationException(message: String) : Exception(message)

/** Un mensaje alterado, repetido o reordenado no supera el descifrado. */
class TamperedFrameException : IOException("Mensaje alterado o repetido.")

/**
 * Canal autenticado y cifrado sobre TCP.
 *
 * Saludo: cada extremo aporta un nonce y una clave ECDH P-256 efímera. Las claves de sesión salen del
 * secreto ECDH y del código de vinculación derivado con PBKDF2, y cada extremo prueba conocerlo con un
 * HMAC sobre todo el saludo. El código nunca viaja por la red.
 * Mensajes: AES-256-GCM con una clave por sentido y un contador como IV, lo que rechaza repeticiones.
 */
class SecureChannel private constructor(
    private val input: DataInputStream,
    private val output: DataOutputStream,
    private val sendKey: SecretKeySpec,
    private val receiveKey: SecretKeySpec
) {
    private var sendCounter = 0L
    private var receiveCounter = 0L

    @Synchronized
    fun send(message: LinkMessage) {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, sendKey, GCMParameterSpec(TAG_BITS, iv(sendCounter++)))
        val frame = cipher.doFinal(LinkCodec.encode(message))
        output.writeInt(frame.size)
        output.write(frame)
        output.flush()
    }

    /** Solo debe llamarse desde un único lector a la vez. */
    fun receive(): LinkMessage {
        val size = input.readInt()
        if (size !in TAG_BITS / 8..MAX_FRAME_BYTES) throw TamperedFrameException()
        val frame = ByteArray(size).also(input::readFully)
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, receiveKey, GCMParameterSpec(TAG_BITS, iv(receiveCounter++)))
        val plain = try { cipher.doFinal(frame) } catch (e: GeneralSecurityException) { throw TamperedFrameException() }
        return LinkCodec.decode(plain)
    }

    private fun iv(counter: Long) = ByteBuffer.allocate(IV_BYTES).putLong(IV_BYTES - Long.SIZE_BYTES, counter).array()

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val IV_BYTES = 12
        private const val MAX_FRAME_BYTES = 4096
        private const val NONCE_BYTES = 32
        private const val MAX_PUBLIC_KEY_BYTES = 256
        private const val PBKDF2_ITERATIONS = 200_000
        private val MAGIC = "VGL1".toByteArray(Charsets.US_ASCII)
        private const val HMAC = "HmacSHA256"

        /** Extremo que inicia la conexión: la estación. */
        fun connect(rawInput: InputStream, rawOutput: OutputStream, code: String): SecureChannel {
            val input = DataInputStream(rawInput)
            val output = DataOutputStream(rawOutput)
            val keys = newKeyPair()
            val clientNonce = randomNonce()
            val clientPublic = keys.public.encoded
            output.write(MAGIC)
            output.write(clientNonce)
            writeBlock(output, clientPublic)
            output.flush()

            val serverNonce = ByteArray(NONCE_BYTES).also(input::readFully)
            val serverPublic = readBlock(input)
            val serverProof = ByteArray(32).also(input::readFully)
            val session = deriveSession(code, keys, serverPublic, clientNonce, clientPublic, serverNonce, serverPublic)
            // Se verifica al monitor antes de revelar la prueba propia.
            if (!MessageDigest.isEqual(serverProof, session.serverProof)) {
                throw AuthenticationException("El código de vinculación no coincide.")
            }
            output.write(session.clientProof)
            output.flush()
            return SecureChannel(input, output, session.clientKey, session.serverKey)
        }

        /** Extremo que escucha: el monitor. */
        fun accept(rawInput: InputStream, rawOutput: OutputStream, code: String): SecureChannel {
            val input = DataInputStream(rawInput)
            val output = DataOutputStream(rawOutput)
            val magic = ByteArray(MAGIC.size).also(input::readFully)
            if (!magic.contentEquals(MAGIC)) throw IOException("Protocolo desconocido.")
            val clientNonce = ByteArray(NONCE_BYTES).also(input::readFully)
            val clientPublic = readBlock(input)

            val keys = newKeyPair()
            val serverNonce = randomNonce()
            val serverPublic = keys.public.encoded
            val session = deriveSession(code, keys, clientPublic, clientNonce, clientPublic, serverNonce, serverPublic)
            output.write(serverNonce)
            writeBlock(output, serverPublic)
            output.write(session.serverProof)
            output.flush()

            // Quien recibió la prueba del monitor y no responde también cuenta como intento fallido:
            // una estación con el código equivocado corta aquí, sin enviar nada más.
            val clientProof = try {
                ByteArray(32).also(input::readFully)
            } catch (e: IOException) {
                throw AuthenticationException("La estación abandonó la vinculación.")
            }
            if (!MessageDigest.isEqual(clientProof, session.clientProof)) {
                throw AuthenticationException("La estación no conoce el código de vinculación.")
            }
            return SecureChannel(input, output, session.serverKey, session.clientKey)
        }

        private class Session(
            val clientKey: SecretKeySpec, val serverKey: SecretKeySpec,
            val clientProof: ByteArray, val serverProof: ByteArray
        )

        private fun deriveSession(
            code: String, keys: java.security.KeyPair, peerPublic: ByteArray,
            clientNonce: ByteArray, clientPublic: ByteArray, serverNonce: ByteArray, serverPublic: ByteArray
        ): Session {
            val shared = try {
                val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublic))
                KeyAgreement.getInstance("ECDH").run {
                    init(keys.private)
                    doPhase(peer, true)
                    generateSecret()
                }
            } catch (e: GeneralSecurityException) {
                throw IOException("Clave pública no válida.")
            } catch (e: RuntimeException) {
                // Algunos proveedores informan una codificación inválida con excepciones no verificadas.
                throw IOException("Clave pública no válida.")
            }
            val transcript = MessageDigest.getInstance("SHA-256").run {
                update(MAGIC); update(clientNonce); update(clientPublic); update(serverNonce); update(serverPublic)
                digest()
            }
            val spec = PBEKeySpec(PairingCode.normalize(code).toCharArray(), clientNonce + serverNonce, PBKDF2_ITERATIONS, 256)
            val master = try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally { spec.clearPassword() }
            fun expand(label: String) = Mac.getInstance(HMAC).run {
                init(SecretKeySpec(master, HMAC))
                update(label.toByteArray(Charsets.US_ASCII)); update(shared); update(transcript)
                doFinal()
            }
            return Session(
                clientKey = SecretKeySpec(expand("client key"), "AES"),
                serverKey = SecretKeySpec(expand("server key"), "AES"),
                clientProof = expand("client proof"),
                serverProof = expand("server proof")
            )
        }

        private fun newKeyPair() = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }

        private fun randomNonce() = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }

        private fun writeBlock(output: DataOutputStream, bytes: ByteArray) {
            output.writeShort(bytes.size)
            output.write(bytes)
        }

        private fun readBlock(input: DataInputStream): ByteArray {
            val size = input.readUnsignedShort()
            if (size !in 1..MAX_PUBLIC_KEY_BYTES) throw IOException("Clave pública no válida.")
            return ByteArray(size).also(input::readFully)
        }
    }
}

object PairingCode {
    // Sin 0/O ni 1/I/L, para que se pueda dictar y copiar sin confusiones.
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 8

    fun generate(): String {
        val random = SecureRandom()
        return (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
    }

    fun normalize(code: String) = code.uppercase().filter { it.isLetterOrDigit() }

    fun isWellFormed(code: String) = normalize(code).let { c -> c.length == LENGTH && c.all { it in ALPHABET } }

    fun display(code: String) = "${code.take(LENGTH / 2)}-${code.drop(LENGTH / 2)}"
}
