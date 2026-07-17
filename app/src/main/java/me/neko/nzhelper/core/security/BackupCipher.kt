package me.neko.nzhelper.core.security

import android.content.Context
import androidx.core.content.edit
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCipher {

    private const val PREFS = "backup_pw_prefs"
    private const val KEY_PW = "backup_password"
    private const val MAGIC = "NZB1"
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 600_000
    private const val KEY_BITS = 256

    private fun getPassword(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PW, null)
        if (stored != null) {
            KeystoreCrypto.decryptString(stored).takeIf { it != stored }?.let { return it }
        }
        val generated = generateRandomPassword()
        prefs.edit { putString(KEY_PW, KeystoreCrypto.encryptString(generated)) }
        return generated
    }

    fun hasCustomPassword(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("custom_pw_set", false)

    fun getCustomPassword(context: Context): String? {
        if (!hasCustomPassword(context)) return null
        return getPassword(context)
    }

    fun setPassword(context: Context, password: String) {
        val final = password.ifEmpty { generateRandomPassword() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_PW, KeystoreCrypto.encryptString(final))
            putBoolean("custom_pw_set", password.isNotEmpty())
        }
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
        val password = getPassword(context)
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
        decryptWithPassword(getPassword(context), data)

    fun isNzFile(data: ByteArray): Boolean {
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        if (data.size < magic.size) return false
        for (i in magic.indices) {
            if (data[i] != magic[i]) return false
        }
        return true
    }
}
