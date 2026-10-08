package me.neko.nzhelper.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreCrypto {

    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "nzhelper_master_key_v1"
    private const val MAGIC = "K1:"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    enum class Failure {
        NOT_WRAPPED,
        MASTER_KEY_MISSING,
        MASTER_KEY_MISMATCH,
        MALFORMED,
        KEYSTORE_ERROR
    }

    sealed interface Unwrap {
        class Ok(val value: ByteArray) : Unwrap
        class Err(val failure: Failure, val cause: Throwable? = null) : Unwrap
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        val out = ByteArray(iv.size + ct.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(ct, 0, out, iv.size, ct.size)
        return MAGIC + Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun encryptString(plain: String): String =
        if (plain.isEmpty()) plain else encrypt(plain.toByteArray(Charsets.UTF_8))

    fun unwrap(value: String): Unwrap {
        if (!value.startsWith(MAGIC)) return Unwrap.Err(Failure.NOT_WRAPPED)

        val data = try {
            Base64.decode(value.substring(MAGIC.length), Base64.NO_WRAP)
        } catch (t: Throwable) {
            return Unwrap.Err(Failure.MALFORMED, t)
        }
        if (data.size <= IV_LEN) return Unwrap.Err(Failure.MALFORMED)

        val key = try {
            existingKey()
        } catch (t: Throwable) {
            return Unwrap.Err(Failure.KEYSTORE_ERROR, t)
        } ?: return Unwrap.Err(Failure.MASTER_KEY_MISSING)

        val iv = data.copyOfRange(0, IV_LEN)
        val ct = data.copyOfRange(IV_LEN, data.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            Unwrap.Ok(cipher.doFinal(ct))
        } catch (t: Throwable) {
            Unwrap.Err(Failure.MASTER_KEY_MISMATCH, t)
        }
    }

    fun decrypt(value: String): ByteArray? = (unwrap(value) as? Unwrap.Ok)?.value
}
