package cl.inacap.vialguard

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** Cuenta local del avance. No almacena la contraseña ni la transmite a Firebase. */
class LocalCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("vialguard_protected", Context.MODE_PRIVATE)
    private val alias = "vialguard_local_aes_v1"
    private val encoderFlags = Base64.NO_WRAP
    fun hasAccount() = preferences.contains("record")

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(alias)) return store.getKey(alias, null) as SecretKey
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    private fun protect(record: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.doFinal(record.toString().toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString("record", Base64.encodeToString(bytes, encoderFlags))
            .putString("iv", Base64.encodeToString(cipher.iv, encoderFlags)).commit())
    }

    private fun read(): JSONObject {
        val encrypted = requireNotNull(preferences.getString("record", null))
        val iv = Base64.decode(requireNotNull(preferences.getString("iv", null)), encoderFlags)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return JSONObject(String(cipher.doFinal(Base64.decode(encrypted, encoderFlags)), Charsets.UTF_8))
    }

    private fun derive(password: String, salt: ByteArray, algorithm: String): ByteArray {
        val chars = password.toCharArray()
        val spec = PBEKeySpec(chars, salt, 600_000, 256)
        return try { SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded }
        finally { spec.clearPassword(); chars.fill('\u0000') }
    }

    fun create(username: String, password: String): Result<String> = runCatching {
        check(!hasAccount()) { "Ya existe una cuenta local. Inicia sesión." }
        require(username.trim().length >= 3) { "Usa un usuario de al menos tres caracteres." }
        require(password.length >= 10 && password.any { it.isLetter() } && password.any { it.isDigit() }) {
            "Usa al menos 10 caracteres, incluyendo letras y números."
        }
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val algorithm = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val digest = derive(password, salt, algorithm)
        protect(JSONObject().put("username", username.trim()).put("salt", Base64.encodeToString(salt, encoderFlags))
            .put("digest", Base64.encodeToString(digest, encoderFlags)).put("algorithm", algorithm)
            .put("failures", 0).put("lockedUntil", 0L))
        username.trim()
    }

    fun login(username: String, password: String): Result<String> = runCatching {
        val record = read() // Un registro corrupto o una clave perdida impiden el acceso.
        check(System.currentTimeMillis() >= record.getLong("lockedUntil")) {
            "Acceso bloqueado temporalmente. Espera un minuto e intenta de nuevo."
        }
        val actual = derive(password, Base64.decode(record.getString("salt"), encoderFlags), record.getString("algorithm"))
        val expected = Base64.decode(record.getString("digest"), encoderFlags)
        val validPassword = MessageDigest.isEqual(actual, expected)
        val validUser = username.trim() == record.getString("username")
        if (!validPassword || !validUser) {
            val failures = record.getInt("failures") + 1
            record.put("failures", if (failures >= 5) 0 else failures)
            if (failures >= 5) record.put("lockedUntil", System.currentTimeMillis() + 60_000)
            protect(record)
            error("Usuario o contraseña incorrectos.")
        }
        record.put("failures", 0).put("lockedUntil", 0L)
        protect(record)
        record.getString("username")
    }
}
