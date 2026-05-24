package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 input — Handoff.html §03 · Input.
 *
 * Filled style (light-gray fill), focused state adds primary 1.5dp border,
 * error state turns the border red and shows the error string below.
 *
 * For password fields, pass `isPassword = true` to apply [PasswordVisualTransformation].
 */
@Composable
fun MInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    val shape  = AeroTheme.shapes.rInput

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it, color = colors.ink3) } },
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = error != null,
            shape = shape,
            leadingIcon = leading,
            trailingIcon = trailing,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = colors.surface2,
                unfocusedContainerColor = colors.surface2,
                errorContainerColor     = colors.surface2,
                focusedBorderColor      = colors.primary,
                unfocusedBorderColor    = colors.lineStrong,
                errorBorderColor        = colors.statusFault,
                focusedLabelColor       = colors.primaryInk,
                unfocusedLabelColor     = colors.ink3,
                errorLabelColor         = colors.statusFault,
                cursorColor             = colors.primary,
                errorCursorColor        = colors.statusFault,
            ),
            modifier = Modifier,
        )
        if (error != null) {
            Text(
                text = error,
                style = AeroTheme.typography.bodySmall,
                color = colors.statusFault,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}
