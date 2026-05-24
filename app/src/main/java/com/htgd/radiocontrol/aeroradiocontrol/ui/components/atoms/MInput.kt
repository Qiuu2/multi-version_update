package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun MInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val borderColor = when {
        isError -> colors.danger
        else -> colors.outline
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        if (label != null) {
            Text(text = label, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AeroTheme.shapes.sm)
                .background(colors.surfaceVariant)
                .border(1.dp, borderColor, AeroTheme.shapes.sm)
                .padding(horizontal = spacing.base, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AeroTheme.typography.body,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = LocalTextStyle.current.merge(
                        AeroTheme.typography.body.copy(color = colors.onSurface),
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (trailingIcon != null) {
                Icon(
                    trailingIcon, null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .then(
                            if (onTrailingIconClick != null)
                                Modifier.clickable(onClick = onTrailingIconClick)
                            else Modifier
                        ),
                )
            }
        }
        if (isError && errorMessage != null) {
            Text(text = errorMessage, style = AeroTheme.typography.caption, color = colors.danger)
        }
    }
}
