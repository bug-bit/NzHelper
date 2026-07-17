package me.neko.nzhelper.core.security

import android.content.Context
import androidx.core.content.edit
import java.security.SecureRandom

object DbKeyProvider {

    private const val PREFS = "db_key_prefs"
    private const val KEY_PASSPHRASE = "passphrase"
    private const val KEY_LEN = 32

    @Volatile
    private var cached: ByteArray? = null

    fun get(context: Context): ByteArray {
        cached?.let { return it }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        val pass = if (stored != null) {
            KeystoreCrypto.decrypt(stored)
        } else {
            null
        }
        val final = pass ?: run {
            val bytes = ByteArray(KEY_LEN)
            SecureRandom().nextBytes(bytes)
            bytes
        }
        if (stored == null) {
            prefs.edit { putString(KEY_PASSPHRASE, KeystoreCrypto.encrypt(final)) }
        }
        cached = final
        return final
    }
}
