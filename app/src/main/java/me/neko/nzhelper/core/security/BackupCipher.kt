package me.neko.nzhelper.core.security

import android.content.Context
import androidx.core.content.edit
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupPasswordUnavailableException(message: String) : IllegalStateException(message)

object BackupCipher {

    private const val PREFS = "backup_pw_prefs"
    private const val KEY_PW = "backup_password"
    private const val KEY_CUSTOM = "custom_pw_set"
    private const val MAGIC = "NZB1"
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun customFlag(context: Context): Boolean = prefs(context).getBoolean(KEY_CUSTOM, false)

    private fun readStoredPassword(context: Context): String? {
        val stored = prefs(context).getString(KEY_PW, null) ?: return null
        return when (val result = KeystoreCrypto.unwrap(stored)) {
            is KeystoreCrypto.Unwrap.Ok -> String(result.value, Charsets.UTF_8)
            is KeystoreCrypto.Unwrap.Err -> null
        }
    }

    private fun requirePassword(context: Context): String {
        val stored = prefs(context).getString(KEY_PW, null)
        if (stored != null) {
            return when (val result = KeystoreCrypto.unwrap(stored)) {
                is KeystoreCrypto.Unwrap.Ok -> String(result.value, Charsets.UTF_8)
                is KeystoreCrypto.Unwrap.Err -> throw BackupPasswordUnavailableException(
                    "备份密码已丢失（${DbKeyProvider.description(result.failure)}），需要重新设置后才能导出或恢复备份"
                )
            }
        }
        val generated = generateRandomPassword()
        prefs(context).edit { putString(KEY_PW, KeystoreCrypto.encryptString(generated)) }
        return generated
    }

    fun hasCustomPassword(context: Context): Boolean =
        customFlag(context) && readStoredPassword(context) != null

    fun getCustomPassword(context: Context): String? =
        if (!customFlag(context)) null else readStoredPassword(context)

    fun setPassword(context: Context, password: String) {
        val final = password.ifEmpty { generateRandomPassword() }
        prefs(context).edit {
            putString(KEY_PW, KeystoreCrypto.encryptString(final))
            putBoolean(KEY_CUSTOM, password.isNotEmpty())
        }
    }

    fun resetPasswordIfUnreadable(context: Context) {
        val stored = prefs(context).getString(KEY_PW, null) ?: return
        if (KeystoreCrypto.unwrap(stored) is KeystoreCrypto.Unwrap.Ok) return
        setPassword(context, "")
    }

    private fun generateRandomPassword(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun encrypt(context: Context, plaintext: ByteArray): ByteArray {
        val password = requirePassword(context)
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        val out = ByteArray(magic.size + SALT_LEN + IV_LEN + ct.size)
        var off = 0
        System.arraycopy(magic, 0, out, off, magic.size); off += magic.size
        System.arraycopy(salt, 0, out, off, SALT_LEN); off += SALT_LEN
        System.arraycopy(iv, 0, out, off, IV_LEN); off += IV_LEN
        System.arraycopy(ct, 0, out, off, ct.size)
        return out
    }

    fun decryptWithPassword(password: String, data: ByteArray): ByteArray? {
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        if (data.size < magic.size + SALT_LEN + IV_LEN) return null
        for (i in magic.indices) {
            if (data[i] != magic[i]) return null
        }
        var off = magic.size
        val salt = data.copyOfRange(off, off + SALT_LEN); off += SALT_LEN
        val iv = data.copyOfRange(off, off + IV_LEN); off += IV_LEN
        val ct = data.copyOfRange(off, data.size)
        return try {
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(ct)
        } catch (_: Exception) {
            null
        }
    }

    fun decrypt(context: Context, data: ByteArray): ByteArray? =
        decryptWithPassword(requirePassword(context), data)

    fun isNzFile(data: ByteArray): Boolean {
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }
}
