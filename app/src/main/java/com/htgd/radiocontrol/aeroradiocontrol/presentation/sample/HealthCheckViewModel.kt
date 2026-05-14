package com.htgd.radiocontrol.aeroradiocontrol.presentation.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the server-reachability check.
 *
 * This is the project's first ViewModel and serves as the canonical
 * pattern for all future UI state holders.
 *
 * Pattern notes:
 *  - `@HiltViewModel` so the Activity / Fragment can use the
 *    `by viewModels()` delegate without writing a factory.
 *  - Constructor-injected dependencies — Hilt resolves the chain
 *    from this ViewModel down through Repository, Retrofit, OkHttp.
 *  - Exposes a single `StateFlow<UiState>` for the UI to collect.
 *    No LiveData, no callbacks, no public mutable fields.
 *  - The mutable backing flow is `private` so only the ViewModel
 *    can write state. The public flow is read-only (`StateFlow`).
 *  - Coroutines launched in `viewModelScope` are auto-cancelled when
 *    the ViewModel clears, so we cannot leak.
 *  - No reference to View, Context, or Activity. Tests instantiate
 *    this directly with a fake Repository — no Android framework
 *    needed in unit tests.
 *
 * Wiring at the Activity / Fragment side (for reference):
 *
 *     @AndroidEntryPoint
 *     class HealthCheckActivity : AppCompatActivity() {
 *         private val viewModel: HealthCheckViewModel by viewModels()
 *
 *         override fun onCreate(savedInstanceState: Bundle?) {
 *             super.onCreate(savedInstanceState)
 *             lifecycleScope.launch {
 *                 repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                     viewModel.uiState.collect { state -> render(state) }
 *                 }
 *             }
 *         }
 *     }
 *
 * That hookup is intentionally not done in this commit — Phase 0-3
 * is about establishing the data-layer + ViewModel skeleton. The
 * first real screen (push notification fault list) lands in Phase 1.
 */
@HiltViewModel
class HealthCheckViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthCheckUiState>(HealthCheckUiState.Idle)
    val uiState: StateFlow<HealthCheckUiState> = _uiState.asStateFlow()

    /**
     * Triggers a server reachability check.
     * UI state transitions: Idle/Reachable/Unreachable → Checking → terminal state.
     */
    fun checkServer(serverUrl: String) {
        viewModelScope.launch {
            _uiState.value = HealthCheckUiState.Checking
            _uiState.value = healthRepository.ping(serverUrl).fold(
                onSuccess = { pingResult ->
                    if (pingResult.isSuccessful) {
                        HealthCheckUiState.Reachable(pingResult.httpCode)
                    } else {
                        HealthCheckUiState.ServerError(pingResult.httpCode)
                    }
                },
                onFailure = { throwable ->
                    HealthCheckUiState.Unreachable(throwable.message ?: "Unknown error")
                },
            )
        }
    }

    /** Resets back to the empty state. Useful after the user dismisses an error. */
    fun reset() {
        _uiState.value = HealthCheckUiState.Idle
    }
}

/**
 * Exhaustive UI state for the health-check screen.
 *
 * Sealed class so the `when (state) { ... }` in the UI must handle
 * every case — the compiler enforces completeness, which prevents
 * the "I forgot to render the error case" class of bug.
 */
sealed class HealthCheckUiState {
    /** No check has been triggered yet. */
    object Idle : HealthCheckUiState()

    /** Request in flight. UI should show a spinner. */
    object Checking : HealthCheckUiState()

    /** Server answered with a 2xx HTTP code. */
    data class Reachable(val httpCode: Int) : HealthCheckUiState()

    /** Server answered, but with a non-2xx code. Distinguished from Unreachable
     *  because "server is up but rejecting us" is a different troubleshooting
     *  case from "can't reach the server at all". */
    data class ServerError(val httpCode: Int) : HealthCheckUiState()

    /** Network / DNS / timeout — server did not answer at all. */
    data class Unreachable(val reason: String) : HealthCheckUiState()
}
