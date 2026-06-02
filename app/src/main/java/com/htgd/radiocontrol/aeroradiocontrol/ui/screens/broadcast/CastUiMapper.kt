package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media

/**
 * Domain → UI mapping for the 点播 (Cast) media picker, at the ViewModel boundary
 * (broadcast SD1). Mirrors TerminalStatusMapper / TaskUiMappers / ServiceUiMapper:
 * the screen sees only [MediaUi]; the data layer's [Media] never reaches a Composable.
 */

private const val NA = "--"

fun Media.toMediaUi(): MediaUi = MediaUi(
    id = id,
    name = name,
    duration = durationSeconds.toDurationLabel(),
)

/** Seconds → "mm:ss"; null/negative → blank-tolerant "--". */
private fun Int?.toDurationLabel(): String {
    if (this == null || this < 0) return NA
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}
