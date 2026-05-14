package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.api.HealthApiService
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for server-reachability checks.
 *
 * This is the project's first Repository and serves as the canonical
 * pattern for all future data-layer classes.
 *
 * Pattern notes:
 *  - `@Singleton` so the whole app shares one instance.
 *  - Constructor-injected dependencies via Hilt @Inject — no service
 *    locators, no `new XxxUtil(this)` calls.
 *  - Coroutines + `withContext(ioDispatcher)` instead of RxJava
 *    Schedulers. Dispatchers are injected (not Dispatchers.IO directly)
 *    so unit tests can swap in TestDispatcher.
 *  - Returns `Result<T>` — Kotlin's built-in success/failure wrapper —
 *    instead of throwing. ViewModels can then `.fold { ... }` to map
 *    the result to UI state without try/catch boilerplate.
 *  - Public surface is plain suspend functions, not Flow. Use Flow
 *    when there is a real stream (subscriptions, repeated polling);
 *    one-shot fetches like this stay suspend for simplicity.
 *
 * Future Repository template:
 *
 *     @Singleton
 *     class FaultRepository @Inject constructor(
 *         private val api: FaultApiService,
 *         @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
 *     ) {
 *         suspend fun listFaults(): Result<List<Fault>> =
 *             withContext(ioDispatcher) {
 *                 runCatching { api.listFaults().requireBody() }
 *             }
 *     }
 */
@Singleton
class HealthRepository @Inject constructor(
    private val healthApi: HealthApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Tries to reach the given server URL.
     *
     * Success means the server responded with any HTTP status code
     * (including 4xx and 5xx) — i.e. the network and DNS are working.
     * Failure means an IO error, timeout, or unparseable response.
     *
     * Distinguishing "server returned 500" from "server is reachable"
     * is the caller's job — that lives at the ViewModel layer where
     * UI state is decided.
     */
    suspend fun ping(serverUrl: String): Result<PingResult> =
        withContext(ioDispatcher) {
            runCatching {
                val response = healthApi.ping(serverUrl)
                PingResult(
                    httpCode = response.code(),
                    isSuccessful = response.isSuccessful,
                )
            }
        }
}

/**
 * Outcome of a single ping attempt. Lives in the repository layer
 * (not the UI layer) because it is a primitive data fact, not a
 * presentation concept.
 */
data class PingResult(
    val httpCode: Int,
    val isSuccessful: Boolean,
)
