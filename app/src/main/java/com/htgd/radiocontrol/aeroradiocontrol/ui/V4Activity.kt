package com.htgd.radiocontrol.aeroradiocontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold.AppNavGraph
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import com.htgd.radiocontrol.screanadaption.CancelAdapt
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the v4 Compose UI — the app's single MAIN/LAUNCHER activity
 * (consolidated from the legacy double-launcher, RISK-AUDIT-02 / D-2026-05-27-03).
 *
 * The legacy [com.htgd.radiocontrol.aeroradiocontrol.activity.SignActivity] still
 * exists for in-progress migration but is no longer a launcher; it is reached by
 * explicit Intent only. Hosts the v4 nav graph (splash → login → main).
 *
 * Annotated with [AndroidEntryPoint] so Compose-side ViewModels can use Hilt
 * via `hiltViewModel()`.
 *
 * Implements [CancelAdapt] to opt out of the legacy AutoSize density rewrite:
 * AutoSize rescales density to the legacy landscape design baseline, which
 * shrinks the Compose UI on portrait phones. Compose screens use native dp.
 */
@AndroidEntryPoint
class V4Activity : ComponentActivity(), CancelAdapt {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AeroTheme {
                AppNavGraph()
            }
        }
    }
}
