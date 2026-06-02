package com.htgd.radiocontrol.aeroradiocontrol.data.auth

/**
 * Minimal key-value persistence seam used by [AuthStoreImpl].
 *
 * Why this exists:
 *   AuthStore's interesting logic — the Mutex/double-check refresh and the
 *   "publish only after the write commits" ordering (RISK-AR-001) — must be
 *   unit-testable on a plain JVM. But EncryptedSharedPreferences needs an
 *   Android device or Robolectric. Hiding the storage behind this interface
 *   lets the concurrency test inject an in-memory fake while production binds
 *   the encrypted implementation. The store does not know or care whether its
 *   values are encrypted; AuthStoreImpl decides which store (secure vs. plain)
 *   holds which key.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getInt(key: String, default: Int): Int

    /** ★ NEXT-2: read a Long (for tokenExpiry epoch-millis). [SharedPrefsKeyValueStore.put]
     *  already accepts Long writes; this completes the read side. */
    fun getLong(key: String, default: Long): Long

    /** ★ NEXT-2: read a Boolean (for rememberMe). [SharedPrefsKeyValueStore.put]
     *  already accepts Boolean writes; this completes the read side. */
    fun getBoolean(key: String, default: Boolean): Boolean

    /**
     * Writes the given keys and blocks until they are durably committed.
     * Pass a null value to remove a key. AuthStore relies on this returning
     * only after the write is visible, so the subsequent StateFlow publish is
     * consistent with persisted state.
     */
    fun put(vararg entries: Pair<String, Any?>)

    /** Removes everything in this store. */
    fun clear()
}
