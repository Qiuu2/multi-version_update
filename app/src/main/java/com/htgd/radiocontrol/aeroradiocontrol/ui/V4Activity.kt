package com.htgd.radiocontrol.aeroradiocontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold.AppNavGraph
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import com.htgd.radiocontrol.screanadaption.CancelAdapt
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the v4 Compose UI.
 *
 * Lives side-by-side with the legacy [com.htgd.radiocontrol.aeroradiocontrol.activity.SignActivity]
 * during the gradual migration. Manifest declares this with its own LAUNCHER
 * filter under a distinct app label ("AeroRadio v4") so QA can install both
 * UIs on the same device and toggle between them.
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
