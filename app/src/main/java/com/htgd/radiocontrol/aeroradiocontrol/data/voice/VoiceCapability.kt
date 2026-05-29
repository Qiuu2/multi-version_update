package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two seams the [VoiceTalkAdapter] depends on but that can't run on a plain JVM:
 * native-library capability detection and the RECORD_AUDIO runtime check.
 * Abstracting them (like AuthStore's KeyValueStore / IPC's SocketConnection) lets
 * the adapter's gate logic be unit-tested with fakes.
 */

/** Detects whether the AAR's voice native libs can load on this device/ABI. */
interface VoiceNativeProbe {
    /**
     * True iff a supported ABI is present AND both AAR libs load. Result is
     * cached by the implementation — loadLibrary is idempotent but not free.
     */
    fun nativeLibsLoadable(): Boolean
}

/** Reads the current RECORD_AUDIO grant state (re-checked per session). */
interface AudioPermissionChecker {
    fun isRecordAudioGranted(): Boolean
}

/**
 * Production [VoiceNativeProbe]: mirrors what `MediaCodec`'s static initializer
 * does (loadLibrary "mp3lame" then "audioplay"), guarded so a 64-bit-only or
 * x86_64 device (AAR ships no x86/x86_64 .so — SPIKE-AAR64) fails to false
 * instead of throwing UnsatisfiedLinkError up.
 */
@Singleton
class DefaultVoiceNativeProbe @Inject constructor() : VoiceNativeProbe {

    private val loadable: Boolean by lazy { probe() }

    override fun nativeLibsLoadable(): Boolean = loadable

    private fun probe(): Boolean {
        // No ARM ABI at all (e.g. a pure x86_64 emulator) → libs absent.
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        val hasArmAbi = abis.any { it == ABI_ARM64 || it == ABI_ARMV7 }
        if (!hasArmAbi) return false
        return try {
            // Order matches MediaCodec.static{}: audioplay's DT_NEEDED references
            // libmp3lame.so, so load mp3lame first.
            System.loadLibrary(LIB_MP3LAME)
            System.loadLibrary(LIB_AUDIOPLAY)
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        } catch (e: Throwable) {
            false
        }
    }

    companion object {
        const val ABI_ARM64 = "arm64-v8a"
        const val ABI_ARMV7 = "armeabi-v7a"
        const val LIB_MP3LAME = "mp3lame"
        const val LIB_AUDIOPLAY = "audioplay"
    }
}

/** Production [AudioPermissionChecker] over the app context. */
@Singleton
class DefaultAudioPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioPermissionChecker {
    override fun isRecordAudioGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
}
