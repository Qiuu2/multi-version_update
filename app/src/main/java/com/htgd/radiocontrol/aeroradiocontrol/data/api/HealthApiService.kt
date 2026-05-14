package com.htgd.radiocontrol.aeroradiocontrol.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Retrofit interface for server health / reachability checks.
 *
 * This is the project's first Retrofit @interface and serves as the
 * pattern reference for all future API services.
 *
 * Pattern notes:
 *  - `suspend` functions, not Call<T> — we use coroutines, not RxJava.
 *  - `Response<T>` wrapper exposes the HTTP status code so the
 *    Repository can distinguish 2xx (server reachable, app authenticated)
 *    from 4xx/5xx (server reachable but something is wrong).
 *  - `@Url` parameter accepts a full URL per call. This is the workaround
 *    for the dynamic baseUrl issue described in NetworkModule. Once we
 *    introduce a base-URL interceptor in Phase 1, future API services
 *    will use relative paths via @GET("/some/endpoint") instead.
 *
 * Future API services should follow this template but drop @Url:
 *
 *     interface FaultApiService {
 *         @GET("/fault/list")
 *         suspend fun listFaults(): Response<List<FaultDto>>
 *     }
 */
interface HealthApiService {

    /**
     * Pings the given absolute URL and returns the raw HTTP response.
     *
     * Repository wraps this into a typed Result<HealthStatus>.
     * Any URL is acceptable — the call succeeds if the server returns
     * any HTTP response at all; it fails if the network is unreachable
     * or the request times out.
     */
    @GET
    suspend fun ping(@Url url: String): Response<Unit>
}
