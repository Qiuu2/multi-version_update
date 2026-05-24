package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * v4 corner radii — Handoff.html §02 · Radius.
 *
 * | token       | dp  | usage                              |
 * |-------------|-----|------------------------------------|
 * | rCard       | 12  | cards, list items                   |
 * | rTile       | 16  | terminal tiles                      |
 * | rChip       | 999 | pills, chips, buttons               |
 * | rSheet      | 24  | bottom sheets (top corners only)    |
 * | rInput      | 12  | text fields                         |
 *
 * Field type is [CornerBasedShape] (the Material 3 [Shapes] constructor's
 * required upper bound). All values are [RoundedCornerShape] which extends it.
 */
data class AeroShapes(
    val rCard: CornerBasedShape  = RoundedCornerShape(12.dp),
    val rTile: CornerBasedShape  = RoundedCornerShape(16.dp),
    val rChip: CornerBasedShape  = RoundedCornerShape(999.dp),
    val rInput: CornerBasedShape = RoundedCornerShape(12.dp),
    val rSheet: CornerBasedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)

internal fun aeroToMaterial3Shapes(s: AeroShapes): Shapes = Shapes(
    extraSmall = s.rCard,
    small      = s.rCard,
    medium     = s.rTile,
    large      = s.rSheet,
    extraLarge = s.rSheet,
)
