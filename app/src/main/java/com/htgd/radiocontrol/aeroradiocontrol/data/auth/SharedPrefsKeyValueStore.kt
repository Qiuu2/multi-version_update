package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import android.content.SharedPreferences

/**
 * [KeyValueStore] backed by any [SharedPreferences] — plain or encrypted.
 *
 * The store is storage-agnostic: DataModule passes an EncryptedSharedPreferences
 * instance for the secure store and an ordinary one for the plain store, and
 * this adapter treats both identically. Writes use `commit()` (synchronous) so
 * [put] returns only once the data is durable, satisfying the AuthStore
 * ordering contract ("publish to StateFlow only after the write commits").
 */
class SharedPrefsKeyValueStore(
    private val prefs: SharedPreferences,
) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun put(vararg entries: Pair<String, Any?>) {
        val editor = prefs.edit()
        for ((key, value) in entries) {
            when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Long -> editor.putLong(key, value)
                else -> error("Unsupported value type for '$key': ${value::class.java}")
            }
        }
        // commit() (not apply()): block until durable so the caller can publish
        // a StateFlow update that is consistent with persisted state.
        editor.commit()
    }

    override fun clear() {
        prefs.edit().clear().commit()
    }
}
