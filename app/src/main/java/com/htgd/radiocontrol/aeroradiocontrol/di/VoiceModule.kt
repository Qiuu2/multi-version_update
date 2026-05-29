package com.htgd.radiocontrol.aeroradiocontrol.di

import com.htgd.radiocontrol.aeroradiocontrol.data.voice.AudioPermissionChecker
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.DefaultAudioPermissionChecker
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.DefaultVoiceNativeProbe
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceNativeProbe
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceTalkAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceTalkAdapterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI for the voice intercom adapter (ICD-VoiceAAR-v1).
 *
 * Binds the adapter and its two seams (native-lib probe + RECORD_AUDIO checker)
 * to their production implementations. The seams exist so the adapter's
 * availability + permission gate is unit-testable with fakes (no device).
 *
 * Consumer: Frontend-Business broadcast Tab (intercom/paging modes), Phase 1.
 * Real end-to-end stays pending the R-001 real-device confirmation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    @Singleton
    abstract fun bindVoiceTalkAdapter(impl: VoiceTalkAdapterImpl): VoiceTalkAdapter

    @Binds
    @Singleton
    abstract fun bindVoiceNativeProbe(impl: DefaultVoiceNativeProbe): VoiceNativeProbe

    @Binds
    @Singleton
    abstract fun bindAudioPermissionChecker(impl: DefaultAudioPermissionChecker): AudioPermissionChecker
}
