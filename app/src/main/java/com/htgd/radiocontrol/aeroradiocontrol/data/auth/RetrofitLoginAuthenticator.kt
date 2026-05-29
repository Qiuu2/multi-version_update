package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.api.AuthApi
import com.htgd.radiocontrol.aeroradiocontrol.data.network.DynamicBaseUrlInterceptor
import com.htgd.radiocontrol.aeroradiocontrol.data.network.ResponseSuccessPolicy
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.AuthResult
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginAuthenticator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real [LoginAuthenticator]: calls `POST /api/authorizations` and maps the
 * token envelope into an [AuthResult].
 *
 * This is data-integration's implementation of the feature-layer seam
 * (ICD-LoginAuthenticator-v1), bound in DataModule. It supersedes the placeholder
 * `UnconfiguredLoginAuthenticator` (fe-business removed its placeholder
 * LoginModule on AR-005 completion, handing the binding to the data layer).
 *
 * Why it builds an absolute URL itself:
 *   At login the server address is not yet in AuthStore (the ViewModel saves it
 *   only after success), so DynamicBaseUrlInterceptor cannot resolve it. We
 *   build `http://{host}:{port}/api/authorizations` from the [address] the user
 *   typed and pass it to [AuthApi.login] via @Url — bypassing the interceptor.
 *
 * Success / failure (documented-assumption, OPEN INQ-O-1 Q2/Q3):
 *   - HTTP success is decided by [ResponseSuccessPolicy] (default: 2xx).
 *   - The body envelope is `{ "data": [ TokenDto ] }`; we take data[0] and
 *     require a non-blank token. Empty data / blank token → failure (bad creds).
 *   - The password flows in but is NEVER persisted here (soul: Security First).
 */
@Singleton
class RetrofitLoginAuthenticator @Inject constructor(
    private val authApi: AuthApi,
    private val successPolicy: ResponseSuccessPolicy,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoginAuthenticator {

    override suspend fun authenticate(
        address: ServerAddress,
        account: String,
        password: String,
    ): Result<AuthResult> = withContext(ioDispatcher) {
        runCatching {
            val url = "http://${address.host}:${address.port}/" +
                "${DynamicBaseUrlInterceptor.API_BASE_SEGMENT}$LOGIN_PATH"

            val response = authApi.login(url = url, username = account, password = password)

            if (!successPolicy.isSuccess(response)) {
                throw IOException("Login failed: HTTP ${response.code()}")
            }

            val token = response.body()?.data?.firstOrNull()
                ?: throw IOException("Login response had no token data")
            val jwt = token.token?.takeIf { it.isNotBlank() }
                ?: throw IOException("Login response token was blank")

            AuthResult(
                jwt = jwt,
                // OPEN(INQ-O-1 D-1): backend refresh token field unknown; null
                // until confirmed. account null → ViewModel keeps what user typed.
                refreshToken = null,
                account = null,
            )
        }
    }

    companion object {
        private const val LOGIN_PATH = "/authorizations"
    }
}
