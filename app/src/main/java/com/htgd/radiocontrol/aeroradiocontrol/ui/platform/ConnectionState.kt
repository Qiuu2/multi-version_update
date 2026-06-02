package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

/**
 * The realtime channel's lifecycle state, surfaced to the UI via
 * [RealtimeClient.connectionState] and rendered by ConnectionBanner.
 *
 * This is the **receipt-independent** part of the WS work (TASK-AR-103): the
 * state machine and its transitions are final regardless of what the vendor WS
 * protocol turns out to be (OPEN INQ-O-2). What is parsed over the wire is not.
 *
 * Transitions (driven by [RealtimeClientImpl]):
 * ```
 *   DISCONNECTED ──connect()──▶ CONNECTING ──onOpen──▶ CONNECTED
 *        ▲                          │                     │
 *        │                          │ onFailure           │ onFailure / heartbeat timeout
 *        │ disconnect()             ▼                     ▼
 *        └────────────────────── ERROR ◀─────────────────┘
 *                                   │  schedule exponential-backoff reconnect
 *                                   ▼  AND start polling so data keeps flowing
 *                             POLLING_FALLBACK ──onOpen (reconnect)──▶ CONNECTED
 * ```
 *
 * [POLLING_FALLBACK] is a distinct state (not folded into [ERROR]) because the
 * UX differs: ERROR is "we have nothing", POLLING_FALLBACK is "realtime is down
 * but we are still refreshing on a timer" — the yellow "实时已断开，轮询中"
 * banner (Handoff §实时性). The reconnect timer keeps running underneath it.
 */
enum class ConnectionState {
    /** No connection and none being attempted (initial; after [RealtimeClient.disconnect]). */
    DISCONNECTED,

    /** A connect attempt is in flight (socket opening or reconnect scheduled→firing). */
    CONNECTING,

    /** WS open and heartbeating; realtime messages flow. */
    CONNECTED,

    /** WS is down but the 10s/30s polling fallback is supplying data; reconnect pending. */
    POLLING_FALLBACK,

    /** Connect failed and no data source is currently succeeding (e.g. polling also failing). */
    ERROR,
}
