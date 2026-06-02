package com.htgd.radiocontrol.aeroradiocontrol.data.auth

/**
 * Pluggable JWT-refresh mechanism.
 *
 * Why an abstraction instead of calling the auth API directly from AuthStore?
 *   The refresh mechanism is OPEN(INQ-O-1 D-1). The legacy stack had NO
 *   dedicated refresh endpoint — it re-POSTed /authorizations with stored
 *   credentials. The login response (TokenModel) does carry `refresh_expired_at`,
 *   hinting a real refresh token + endpoint may exist server-side, unused.
 *   Until the backend confirms, this seam lets AuthStore own the thread-safe
 *   orchestration (Mutex, double-check, clear-on-failure — all testable today)
 *   while the network call stays a one-line swap. It also avoids a dependency
 *   cycle: AuthInterceptor (AR-002) depends on AuthStore, so AuthStore must not
 *   depend on the auth API that the interceptor sits in front of.
 *
 * Implementations live close to the network layer (AR-002 / Phase 1) and are
 * provided via Hilt. The default ([UnsupportedTokenRefresher]) reports failure
 * so an unconfigured build degrades to "re-login required" rather than crashing.
 */
fun interface TokenRefresher {

    /**
     * Exchanges the current [refreshToken] for a new JWT.
     *
     * Called by [AuthStore.refresh] already inside the refresh mutex, so
     * implementations need not serialize themselves. Must NOT touch AuthStore
     * (no re-entrancy) — just perform the exchange and return the new JWT.
     *
     * @param refreshToken the stored refresh token, or null if none was issued.
     * @return [Result.success] with the new JWT, or [Result.failure] if refresh
     *   is unsupported, the token is rejected, or the network fails.
     */
    suspend fun refresh(refreshToken: String?): Result<String>
}

/**
 * Default refresher used until INQ-O-1 D-1 is answered: always fails.
 *
 * Effect: a 401 leads AuthStore to clear the session and the user re-logs in.
 * This is the safe documented-assumption fallback — no silent half-working
 * refresh. Replace via Hilt once the backend contract is known.
 */
class UnsupportedTokenRefresher @javax.inject.Inject constructor() : TokenRefresher {
    override suspend fun refresh(refreshToken: String?): Result<String> =
        Result.failure(
            UnsupportedOperationException(
                "Token refresh not configured (OPEN INQ-O-1 D-1). Re-login required.",
            ),
        )
}
