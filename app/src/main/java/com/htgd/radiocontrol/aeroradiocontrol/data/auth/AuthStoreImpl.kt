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
 * L2 tokens + expiry and a plain one for the L1 prefill fields + serverAddress.
 *
 * ★ NEXT-2 (2026-06-01) — v2.1 → v2.2: adds [tokenExpiry] + [rememberMe] flows
 * and [clearL2Atomically] / [clearL1Account] / [setRememberMe] methods. See
 * [AuthStore] KDoc for the L1/L2 layer model + atomic invariant.
 *
 * The storage backends are injected ([KeyValueStore]) so the refresh/concurrency
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
    // ★ NEXT-2: this construction-time read is what makes the L2 four-tuple
    // available to [StartupAuthDecider] BEFORE any Composable runs — the
    // decider reads `.value` synchronously, no flow collection required.
    private val _serverAddress = MutableStateFlow(loadServerAddress())
    override val serverAddress: StateFlow<ServerAddress?> = _serverAddress.asStateFlow()

    private val _jwt = MutableStateFlow(secure.getString(KEY_JWT))
    override val jwt: StateFlow<String?> = _jwt.asStateFlow()

    private val _refreshToken = MutableStateFlow(secure.getString(KEY_REFRESH))
    override val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    // ★ NEXT-2: 0L sentinel = absent (SharedPreferences cannot store nullable
    // primitives). Getter exposes nullable to keep the AuthStore contract clean.
    private val _tokenExpiry = MutableStateFlow(
        secure.getLong(KEY_TOKEN_EXPIRY, 0L).takeIf { it > 0L },
    )
    override val tokenExpiry: StateFlow<Long?> = _tokenExpiry.asStateFlow()

    private val _account = MutableStateFlow(plain.getString(KEY_ACCOUNT))
    override val account: StateFlow<String?> = _account.asStateFlow()

    // ★ NEXT-2: default false on fresh install (no L1 fields written yet).
    private val _rememberMe = MutableStateFlow(plain.getBoolean(KEY_REMEMBER_ME, false))
    override val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

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
        tokenExpiry: Long?,
        rememberMe: Boolean,
    ) {
        // Persist first, publish second. A concurrent interceptor read that
        // races this either sees the pre-login state or the fully-committed new
        // state — never a torn mix (RISK-AR-001).
        //
        // serverAddress (host/port) lives in the plain store. It participates
        // in the L2 four-tuple invariant (a session without an address is not
        // a session), but encrypting an LAN IP buys nothing and Encrypted
        // SharedPreferences is the slowest of the three operations.
        //
        // ★ NEXT-2: L1 fields (account + rememberMe) are written
        // UNCONDITIONALLY here because the toggle semantics are "remember the
        // L1 prefill iff rememberMe=true". The toggle is a SEPARATE write path
        // via [setRememberMe] / [clearL1Account]; saveLogin just records what
        // the user authenticated as. Toggling rememberMe off afterwards wipes
        // the prefill via [clearL1Account] in a single transaction.
        plain.put(
            KEY_HOST to address.host,
            KEY_PORT to address.port,
            KEY_ACCOUNT to account,
            KEY_REMEMBER_ME to rememberMe,
        )
        secure.put(
            KEY_JWT to jwt,
            KEY_REFRESH to refreshToken,
            KEY_TOKEN_EXPIRY to tokenExpiry,
        )
        _serverAddress.value = address
        _account.value = account
        _rememberMe.value = rememberMe
        _refreshToken.value = refreshToken
        _tokenExpiry.value = tokenExpiry
        _jwt.value = jwt
        _isLoggedIn.value = jwt.isNotBlank()
    }

    override suspend fun clearLogin() {
        // ★ NEXT-2: active-logout / 401 / expiry-detected path. Drops L2
        // secrets only (jwt + refresh + tokenExpiry). serverAddress (host/port)
        // and L1 prefill fields (account, rememberMe) are intentionally
        // preserved so the next LoginScreen pre-fills. The L2 four-tuple
        // invariant check in [StartupAuthDecider] still correctly returns
        // false here because `jwt` is null — atomic-invariant requires ALL
        // four, not just serverAddress alone.
        secure.put(
            KEY_JWT to null,
            KEY_REFRESH to null,
            KEY_TOKEN_EXPIRY to null,
        )
        _jwt.value = null
        _refreshToken.value = null
        _tokenExpiry.value = null
        _isLoggedIn.value = false
    }

    override suspend fun clearL2Atomically() {
        // ★ NEXT-2: STRONGER than [clearLogin] — wipes the L2 secrets AND
        // serverAddress in a single logical step. Invoked from
        // [StartupAuthDecider] when the four-tuple invariant fails (any field
        // missing/expired), so any partial residue across all four fields is
        // cleared and the next [StartupAuthDecider] call can't be tricked into
        // the same half-state by a leftover serverAddress.
        secure.put(
            KEY_JWT to null,
            KEY_REFRESH to null,
            KEY_TOKEN_EXPIRY to null,
        )
        plain.put(KEY_HOST to null, KEY_PORT to null)
        _jwt.value = null
        _refreshToken.value = null
        _tokenExpiry.value = null
        _serverAddress.value = null
        _isLoggedIn.value = false
    }

    override suspend fun clearL1Account() {
        // ★ NEXT-2: rememberMe toggle OFF. Wipe account + host + port + the
        // flag in one transaction. L2 untouched.
        plain.put(
            KEY_ACCOUNT to null,
            KEY_HOST to null,
            KEY_PORT to null,
            KEY_REMEMBER_ME to false,
        )
        _account.value = null
        _serverAddress.value = null
        _rememberMe.value = false
    }

    override suspend fun setRememberMe(enabled: Boolean) {
        // ★ NEXT-2: pure flag write; no credential side-effects. If the user
        // toggles ON before submitting, [saveLogin] will later persist L1; if
        // OFF, the UI usually calls setRememberMe(false) directly (this write
        // alone reflects intent until the next saveLogin).
        plain.put(KEY_REMEMBER_ME to enabled)
        _rememberMe.value = enabled
    }

    override suspend fun reset() {
        secure.clear()
        plain.clear()
        _jwt.value = null
        _refreshToken.value = null
        _tokenExpiry.value = null
        _serverAddress.value = null
        _account.value = null
        _rememberMe.value = false
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
        /** ★ NEXT-2: epoch-millis when JWT becomes invalid. 0 = absent. */
        const val KEY_TOKEN_EXPIRY = "token_expiry"

        // Plain store keys (non-secret, survive logout iff rememberMe).
        const val KEY_HOST = "server_host"
        const val KEY_PORT = "server_port"
        const val KEY_ACCOUNT = "account"
        /** ★ NEXT-2: L1 prefill flag. Default false on fresh install. */
        const val KEY_REMEMBER_ME = "remember_me"
    }
}
