package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for a zone (a named set of terminals), as the UI consumes it.
 *
 * Repository output type. v4 PRD defines a zone as a terminal collection, so
 * [terminals] is nested here (fe's choice — saves a combine in the ViewModel).
 *
 * Derived counts (per-zone online/fault, global fault) are intentionally NOT on
 * this model — the UI computes them from [terminals] at the presentation layer
 * (fe owns that). Keeping the domain model to raw facts means a count-rule change
 * doesn't ripple into the data layer.
 */
data class Zone(
    val id: String,
    val name: String,
    val description: String? = null,
    val terminals: List<Terminal> = emptyList(),
)
