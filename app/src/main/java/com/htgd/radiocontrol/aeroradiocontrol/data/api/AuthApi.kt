package com.htgd.radiocontrol.aeroradiocontrol.data.api

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TokenEnvelopeDto
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Auth endpoints.
 *
 * Login uses `@Url` (a full absolute URL), NOT the relative-path +
 * DynamicBaseUrlInterceptor scheme the other APIs use. Reason: at login time the
 * server address is not yet in AuthStore (the ViewModel saves it only AFTER a
 * successful login), so the interceptor — which reads AuthStore — has nothing to
 * resolve. The caller (RetrofitLoginAuthenticator) therefore builds the absolute
 * URL `http://{host}:{port}/api/authorizations` from the address the user typed
 * and passes it here. The placeholder host never appears, so the base-URL
 * interceptor leaves it untouched. (Matches ICD-NetworkModule-v1 §2's "login
 * special case" and the legacy LoginActivity flow.)
 *
 * Body is `@FormUrlEncoded` with fields `username` / `userpwd` per the legacy
 * read (LoginActivity → form map, not JSON). OPEN(INQ-O-1 D-2): confirm form vs
 * JSON and the exact field names; if JSON, this becomes `@Body LoginRequest`
 * (ICD_UPDATE).
 *
 * Returns `Response<T>` (not bare T) so the caller sees the HTTP status — the
 * authoritative success signal under the documented-assumption (HTTP 2xx ==
 * success; see ResponseSuccessPolicy, OPEN Q3). AuthInterceptor skips
 * `/authorizations`, so no stale Bearer header is attached to the login call.
 */
interface AuthApi {

    @FormUrlEncoded
    @POST
    suspend fun login(
        @Url url: String,
        @Field("username") username: String,
        @Field("userpwd") password: String,
    ): Response<TokenEnvelopeDto>
}
