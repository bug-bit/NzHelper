package me.neko.nzhelper.core.security

import android.content.Context
import androidx.core.content.edit
import java.security.SecureRandom

class DbKeyUnavailableException(
    val failure: KeystoreCrypto.Failure,
    message: String
) : IllegalStateException(message)

object DbKeyProvider {

    private const val PREFS = "db_key_prefs"
    private const val KEY_PASSPHRASE = "passphrase"
    private const val KEY_LEN = 32

    @Volatile
    private var cached: ByteArray? = null

    sealed interface State {
        object NotInitialized : State
        object Ready : State
        class Unusable(val failure: KeystoreCrypto.Failure) : State
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun state(context: Context): State {
        cached?.let { return State.Ready }
        val stored = prefs(context).getString(KEY_PASSPHRASE, null) ?: return State.NotInitialized
        return when (val result = KeystoreCrypto.unwrap(stored)) {
            is KeystoreCrypto.Unwrap.Ok -> State.Ready
            is KeystoreCrypto.Unwrap.Err -> State.Unusable(result.failure)
        }
    }

    fun get(context: Context): ByteArray {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val prefs = prefs(context)
            val stored = prefs.getString(KEY_PASSPHRASE, null)
            val passphrase = if (stored == null) {
                ByteArray(KEY_LEN).also { bytes ->
                    SecureRandom().nextBytes(bytes)
                    prefs.edit { putString(KEY_PASSPHRASE, KeystoreCrypto.encrypt(bytes)) }
                }
            } else {
                when (val result = KeystoreCrypto.unwrap(stored)) {
                    is KeystoreCrypto.Unwrap.Ok -> result.value
                    is KeystoreCrypto.Unwrap.Err -> throw DbKeyUnavailableException(
                        result.failure,
                        description(result.failure)
                    )
                }
            }
            cached = passphrase
            return passphrase
        }
    }

    fun clear(context: Context) {
        synchronized(this) {
            cached = null
            prefs(context).edit { remove(KEY_PASSPHRASE) }
        }
    }

    fun description(failure: KeystoreCrypto.Failure): String = when (failure) {
        KeystoreCrypto.Failure.NOT_WRAPPED -> "数据库密钥记录格式无法识别"
        KeystoreCrypto.Failure.MASTER_KEY_MISSING -> "系统 Keystore 中没有本机主密钥，无法解开数据库密钥"
        KeystoreCrypto.Failure.MASTER_KEY_MISMATCH -> "系统 Keystore 中的主密钥已变更，无法解开数据库密钥"
        KeystoreCrypto.Failure.MALFORMED -> "数据库密钥记录已损坏"
        KeystoreCrypto.Failure.KEYSTORE_ERROR -> "系统 Keystore 暂时不可用"
    }
}
