package com.htgd.radiocontrol.aeroradiocontrol.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold.AppNavGraph
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import dagger.hilt.android.AndroidEntryPoint

/** Entry point for the v4 Compose UI. Registered as a second LAUNCHER in the manifest. */
@AndroidEntryPoint
class V4Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AeroTheme {
                AppNavGraph()
            }
        }
    }
}
