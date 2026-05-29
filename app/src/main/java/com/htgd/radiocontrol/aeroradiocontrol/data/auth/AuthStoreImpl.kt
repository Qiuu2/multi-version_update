package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.di.PlainStore
import com.htgd.radiocontrol.aeroradiocontrol.di.SecureStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [AuthStore] backed by two key-value stores: an encrypted one for the
 * tokens and a plain one for the non-secret address/account.
 *
 * See [AuthStore] for the threading and security contracts this upholds. The
 * storage backends are injected ([KeyValueStore]) so the refresh/concurrency
 * logic is unit-testable without an Android device — production binds
 * EncryptedSharedPreferences + plain SharedPreferences via Hilt (DataModule).
 */
@Singleton
class AuthStoreImpl @Inject constructor(
    @SecureStore private val secure: KeyValueStore,
    @PlainStore private val plain: KeyValueStore,
    private val tokenRefresher: TokenRefresher,
) : AuthStore {

    // Initial values are read once at construction from persisted state, so a
    // process restart restores the session before any flow is collected.
    private val _serverAddress = MutableStateFlow(loadServerAddress())
    override val serverAddress: StateFlow<ServerAddress?> = _serverAddress.asStateFlow()

    private val _jwt = MutableStateFlow(secure.getString(KEY_JWT))
    override val jwt: StateFlow<String?> = _jwt.asStateFlow()

    private val _refreshToken = MutableStateFlow(secure.getString(KEY_REFRESH))
    override val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    private val _account = MutableStateFlow(plain.getString(KEY_ACCOUNT))
    override val account: StateFlow<String?> = _account.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(!_jwt.value.isNullOrBlank())
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Serializes refresh so a burst of 401s on the same token triggers exactly
    // one network call.
    private val refreshMutex = Mutex()

    override suspend fun saveLogin(
        address: ServerAddress,
        account: String,
        jwt: String,
        refreshToken: String?,
    ) {
        // Persist first, publish second. A concurrent interceptor read that
        // races this either sees the pre-login state or the fully-committed new
        // state — never a torn mix (RISK-AR-001).
        plain.put(
            KEY_HOST to address.host,
            KEY_PORT to address.port,
            KEY_ACCOUNT to account,
        )
        secure.put(
            KEY_JWT to jwt,
            KEY_REFRESH to refreshToken,
        )
        _serverAddress.value = address
        _account.value = account
        _refreshToken.value = refreshToken
        _jwt.value = jwt
        _isLoggedIn.value = jwt.isNotBlank()
    }

    override suspend fun clearLogin() {
        // Drop only the secrets; keep host/account to pre-fill the next login.
        secure.put(KEY_JWT to null, KEY_REFRESH to null)
        _jwt.value = null
        _refreshToken.value = null
        _isLoggedIn.value = false
    }

    override suspend fun reset() {
        secure.clear()
        plain.clear()
        _jwt.value = null
        _refreshToken.value = null
        _serverAddress.value = null
        _account.value = null
        _isLoggedIn.value = false
    }

    override suspend fun refresh(knownStaleJwt: String?): Result<String> =
        refreshMutex.withLock {
            // Timing-independent double-check: if the live token has already
            // moved past the one the caller saw rejected, a concurrent refresh
            // beat us to it — return the live token, no second network call.
            // (knownStaleJwt == null forces an unconditional refresh.)
            val current = _jwt.value
            if (knownStaleJwt != null && !current.isNullOrBlank() && current != knownStaleJwt) {
                return@withLock Result.success(current)
            }

            tokenRefresher.refresh(_refreshToken.value).fold(
                onSuccess = { newJwt ->
                    secure.put(KEY_JWT to newJwt)
                    _jwt.value = newJwt
                    _isLoggedIn.value = newJwt.isNotBlank()
                    Result.success(newJwt)
                },
                onFailure = { error ->
                    // Refresh failed → the session is dead. Clear it so the next
                    // read returns null and the UI routes to login.
                    clearLogin()
                    Result.failure(error)
                },
            )
        }

    private fun loadServerAddress(): ServerAddress? {
        val host = plain.getString(KEY_HOST) ?: return null
        val port = plain.getInt(KEY_PORT, 0).takeIf { it > 0 } ?: return null
        return ServerAddress(host, port)
    }

    companion object {
        // Secure store keys (EncryptedSharedPreferences).
        const val KEY_JWT = "jwt"
        const val KEY_REFRESH = "refresh_token"

        // Plain store keys (non-secret, survive logout).
        const val KEY_HOST = "server_host"
        const val KEY_PORT = "server_port"
        const val KEY_ACCOUNT = "account"
    }
}
