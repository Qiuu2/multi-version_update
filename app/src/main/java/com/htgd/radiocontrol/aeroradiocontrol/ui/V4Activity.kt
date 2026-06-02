package com.htgd.radiocontrol.aeroradiocontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.StartupAuthDecider
import com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold.AppNavGraph
import com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold.AppRoutes
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import com.htgd.radiocontrol.screanadaption.CancelAdapt
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry point for the v4 Compose UI — the app's single MAIN/LAUNCHER activity
 * (consolidated from the legacy double-launcher, RISK-AUDIT-02 / D-2026-05-27-03).
 *
 * The legacy [com.htgd.radiocontrol.aeroradiocontrol.activity.SignActivity] still
 * exists for in-progress migration but is no longer a launcher; it is reached by
 * explicit Intent only. Hosts the v4 nav graph (login → main; splash is no
 * longer a routing decision — see below).
 *
 * Annotated with [AndroidEntryPoint] so Compose-side ViewModels can use Hilt
 * via `hiltViewModel()`.
 *
 * Implements [CancelAdapt] to opt out of the legacy AutoSize density rewrite:
 * AutoSize rescales density to the legacy landscape design baseline, which
 * shrinks the Compose UI on portrait phones. Compose screens use native dp.
 *
 * ★ NEXT-2 (2026-06-01) — atomic startup auth check:
 *   Before `setContent`, [StartupAuthDecider.resumeSessionIfValid] reads the L2
 *   four-tuple ({jwt, serverAddress, account, tokenExpiry}) synchronously and:
 *     - on valid: rehydrates `Constant.serveraddress` via ServerConfig and
 *       returns true → AppNavGraph starts on Main.
 *     - on invalid: atomically clears any L2 residue and returns false →
 *       AppNavGraph starts on Login (with L1 prefill if rememberMe).
 *   This fixes the 2026-06-01 kill-app BLOCKER (LoginRoute used to bounce to
 *   Main with `Constant.serveraddress=null`). See
 *   `.state/api-snapshots/auth-split-brain-rootcause.md` for the full audit.
 */
@AndroidEntryPoint
class V4Activity : ComponentActivity(), CancelAdapt {

    @Inject lateinit var startupAuthDecider: StartupAuthDecider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ★ NEXT-2: synchronous, before setContent — no race against Composables.
        val start = if (startupAuthDecider.resumeSessionIfValid()) {
            AppRoutes.Main
        } else {
            AppRoutes.Login
        }
        setContent {
            AeroTheme {
                AppNavGraph(startDestination = start)
            }
        }
    }
}
