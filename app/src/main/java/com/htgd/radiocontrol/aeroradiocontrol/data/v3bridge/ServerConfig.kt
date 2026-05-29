package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant

/**
 * Seam for reading/writing the v3 server base URL, so data-layer code does not
 * touch `Constant` directly (Plan A / TASK-PA-01).
 *
 * Why this exists:
 *   v3's `Constant` is a Java class whose static initializer transitively touches
 *   Android framework — loading it on a plain JVM unit test throws
 *   `ExceptionInInitializerError`. Hiding the `Constant.serveraddress` access
 *   behind this interface lets the V3 Repository/Authenticator be unit-tested
 *   with an in-memory fake (the project idiom — no Robolectric — same as
 *   AuthStoreImplTest injects KeyValueStore fakes). Production reads/writes the
 *   real `Constant.serveraddress`; **v3 Constant itself is NOT modified** (Plan A
 *   red line — only the new-code access point is made injectable).
 */
interface ServerConfig {
    /** The current v3 base URL, e.g. "http://host:port/api" (Constant.serveraddress). */
    fun baseUrl(): String

    /** Sets the v3 base URL (mirrors v3 LoginActivity setting Constant.serveraddress). */
    fun setBaseUrl(url: String)
}

/** Production [ServerConfig] backed by v3 `Constant.serveraddress`. */
class ConstantServerConfig @javax.inject.Inject constructor() : ServerConfig {
    override fun baseUrl(): String = Constant.serveraddress
    override fun setBaseUrl(url: String) {
        Constant.serveraddress = url
    }
}
