package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import android.content.Context
import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TokenEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3CallbackAdapter
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.AuthResult
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginAuthenticator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan A [LoginAuthenticator]: logs in through the v3 stack (TASK-PA-01, D-14).
 *
 * Replaces the (dormant) Retrofit `RetrofitLoginAuthenticator` via @Binds. Calls
 * v3 `POST /api/authorizations` (form `username`/`userpwd`) through
 * [V3CallbackAdapter], parses the token envelope, and — per CTO D-14 — writes the
 * token into v3 [ServerToken] (which the v3 RequestManger reads for the
 * Authorization header on every subsequent call). This is what makes the v3
 * *Method / RequestManger requests the V3*Repository issues actually authenticated.
 *
 * Token storage (D-14): v3 ServerToken owns the live token; AuthStore's encrypted
 * persistence is NOT used under Plan A (AuthStore is demoted to a UI-validation
 * helper via ServerAddress.parse). The ViewModel still receives an [AuthResult]
 * to drive its success state.
 *
 * Sets the v3 base URL from the typed [ServerAddress] before the call, mirroring
 * v3 LoginActivity.prelogin (which builds "http://host:port/api"), so the v3
 * stack targets the right server from this point on. The write goes through the
 * [ServerConfig] seam (not a direct `Constant.serveraddress =`) — same pattern as
 * V3TerminalRepository — so this class is unit-testable without loading v3
 * Constant (Android static-init); v3 Constant itself is unchanged (Plan A red line).
 */
@Singleton
class V3LoginAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoginAuthenticator {

    override suspend fun authenticate(
        address: ServerAddress,
        account: String,
        password: String,
    ): Result<AuthResult> = withContext(ioDispatcher) {
        runCatching {
            // Point the v3 stack at this server (matches LoginActivity:495's
            // "http://host:port/api"); subsequent v3 calls read it back via the
            // ServerConfig seam (prod = Constant.serveraddress). F-2: write through
            // the seam, not a direct Constant assignment (keeps this unit-testable).
            serverConfig.setBaseUrl("http://${address.host}:${address.port}/api")

            // v3 login = POST form to /authorizations with username/userpwd.
            // MyRequestBuilder prepends Constant.serveraddress; no token needed yet.
            val request = MyRequestBuilder(context).apply {
                setUrl(Constant.getAuthorization)
                setBodyMap(hashMapOf("username" to account, "userpwd" to password))
                setNeedToken(false)
            }

            val json = adapter.post(request).getOrThrow()
            val token = gson.fromJson(json, TokenEnvelopeDto::class.java)
                ?.data?.firstOrNull()?.token?.takeIf { it.isNotBlank() }
                ?: throw IOException("Login response had no token")

            // D-14: store into v3 ServerToken so RequestManger attaches the
            // Authorization header (Bearer prefix) on subsequent calls.
            ServerToken.serverToken = Constant.token_tag + token

            AuthResult(jwt = token, refreshToken = null, account = null)
        }
    }
}
