package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

/**
 * WS message envelope — **DRAFT PLACEHOLDER, NOT FINALIZED** (ICD-BroadcastWS-v1
 * is still DRAFT; OPEN INQ-O-2).
 *
 * ⚠ Receipt boundary (TASK-AR-103): every field name, the `type` discriminator
 * values, the terminal-state literals, the timestamp unit, and whether there is
 * an outer envelope at all are **unverified vendor assumptions** carried over
 * from `icd-contracts.md` §6 DRAFT. This file deliberately does NOT wire up a
 * serializer (no Gson `@SerializedName`, no parse logic) — message *parsing* is
 * the part that waits on the O-2 receipt. These types exist only so the
 * connection-lifecycle skeleton ([RealtimeClient]) has a typed surface to expose
 * and so callers can compile against a stable shape.
 *
 * When O-2 lands: align field names / literals to the real protocol, add the
 * Gson mapping, and drive ICD-BroadcastWS-v1 DRAFT→LIVE + ICD_UPDATE (via PM,
 * per STD-ICD-WRITE). [Unknown] stays regardless — see below.
 *
 * The terminal-state literal ambiguity is the single biggest open question
 * (flagged in INQ-O-2 §A-4): Handoff §807 writes `online/offline/fault/playing/
 * paging` while data-integration's ICD-TerminalDto enumerates `online/offline/
 * paging/talking/casting/urgent/alarm`. We do NOT resolve it here — the raw
 * state string is carried as a String, mapping is deferred to the receipt +
 * cross-domain alignment with data-integration (via PM).
 */
sealed interface BroadcastWSMessage {

    /** Terminal state push. Field shapes are DRAFT (INQ-O-2 §A-4). */
    data class TerminalStateChange(
        val terminalId: String,
        /** Raw vendor state literal, unmapped (see class doc — fault/alarm ambiguity). */
        val rawState: String,
        /** epoch millis — unit is a DRAFT assumption (INQ-O-2 §A-4 Q17). */
        val timestamp: Long,
    ) : BroadcastWSMessage

    /** Task progress push. DRAFT (INQ-O-2 §A-4 Q16). */
    data class TaskProgress(
        val taskId: String,
        /** 0..100 — scale is a DRAFT assumption. */
        val progressPct: Int,
        val rawStatus: String,
    ) : BroadcastWSMessage

    /**
     * Per-terminal broadcast result (Handoff §891 requires it; ICD §6 DRAFT did
     * NOT define it — flagged as a gap in INQ-O-2 §A-15). Shape is a guess.
     */
    data class BroadcastResult(
        val terminalId: String,
        val success: Boolean,
        val failureReason: String?,
    ) : BroadcastWSMessage

    /**
     * Any frame we received but could not classify. R-003-style fallback: an
     * unknown message must never crash the client — it is surfaced as data, not
     * an exception. Carries the raw text for diagnostics / once-O-2-lands triage.
     */
    data class Unknown(val rawText: String) : BroadcastWSMessage
}
