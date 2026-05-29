package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress

/**
 * Seam for the login network call (TASK-AR-005 · ICD-LoginAuthenticator-v1).
 *
 * Why a seam owned by the feature layer?
 *   AuthStore (data-integration) *persists* a session but does not *fetch* one —
 *   the `/authorizations` call is data-integration's Phase-1 work (AR-002), which
 *   does not exist yet. Rather than block AR-005 or reach into a non-existent API,
 *   [LoginViewModel] depends on this interface and data-integration provides the
 *   real implementation via a one-line Hilt swap once `/authorizations` lands —
 *   the same pattern data already uses for `TokenRefresher`.
 *
 * This is a cross-domain contract (defined here, implemented by data-integration),
 * registered in the ICD registry by PM — NOT a private handshake (§7.1). The
 * default binding ([UnconfiguredLoginAuthenticator]) fails cleanly so an
 * unconfigured build degrades to "登录暂不可用" instead of crashing.
 */
fun interface LoginAuthenticator {

    /**
     * Authenticates against [address] with [account] / [password].
     *
     * @return [Result.success] with an [AuthResult] carrying the issued token(s),
     *   or [Result.failure] on bad credentials / network / not-configured. The
     *   ViewModel maps failure into the form-level error banner; it does NOT
     *   persist anything — [AuthStore.saveLogin] is the ViewModel's job on success.
     */
    suspend fun authenticate(
        address: ServerAddress,
        account: String,
        password: String,
    ): Result<AuthResult>
}

/**
 * Result of a successful authentication. Fields map 1:1 onto
 * `AuthStore.saveLogin(address, account, jwt, refreshToken)` so there is no
 * impedance mismatch (PM constraint ①):
 *   - [jwt]          → saveLogin.jwt
 *   - [refreshToken] → saveLogin.refreshToken (nullable; backend may not issue one)
 *   - [account]      → saveLogin.account, when the server canonicalizes the
 *     login name; null means "use what the user typed". `address` is supplied by
 *     the ViewModel from the form, not echoed here.
 */
data class AuthResult(
    val jwt: String,
    val refreshToken: String? = null,
    val account: String? = null,
)

/**
 * Default [LoginAuthenticator] used until data-integration ships the real
 * `/authorizations` call (AR-002 / Phase 1). Always fails, so login is cleanly
 * unavailable rather than silently broken — mirrors `UnsupportedTokenRefresher`.
 */
class UnconfiguredLoginAuthenticator @javax.inject.Inject constructor() : LoginAuthenticator {
    override suspend fun authenticate(
        address: ServerAddress,
        account: String,
        password: String,
    ): Result<AuthResult> = Result.failure(
        UnsupportedOperationException(
            "Login not configured: /authorizations not wired yet (AR-002/Phase 1).",
        ),
    )
}
