package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Dvr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import kotlinx.coroutines.delay

/**
 * Splash — Handoff.html §05 spec-card #00.
 *
 * Behavior:
 *   - Minimum visible duration: 800ms (anti-flicker).
 *   - Maximum visible duration: 1500ms.
 *   - On exit, call [onFinish] — the host decides where to route based on
 *     token presence (login screen vs main scaffold).
 */
@Composable
fun SplashScreen(
    versionName: String = "v4.0",
    onFinish: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        delay(1200L) // sits between 800 and 1500
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Handoff:755「品牌渐变背景」— was Night (AI/call-screen gradient), fixed Q3 #2
            .background(AeroGradients.Primary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo mark
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(elevation = 24.dp, shape = AeroTheme.shapes.rTile, clip = false)
                    .clip(AeroTheme.shapes.rTile)
                    .background(AeroGradients.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Dvr,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text  = "AeroRadioControl",
                style = AeroTheme.typography.display.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.padding(top = 28.dp),
            )

            Text(
                text  = "校园广播控制中心",
                style = AeroTheme.typography.body.copy(color = Color.White.copy(alpha = 0.8f)),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            text  = versionName,
            style = AeroTheme.typography.kicker.copy(color = Color.White.copy(alpha = 0.6f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        )
    }
}
