package com.gowtham.pocketaudiobook.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice

data class VoiceModel(
    val id: String,
    val displayName: String,
    val localeTag: String,
    val localeDisplayName: String,
    val isNetworkRequired: Boolean,
    val isInstalled: Boolean,
    val qualityLabel: String,
    val latencyLabel: String
) {
    companion object {
        fun fromAndroidVoice(voice: Voice): VoiceModel {
            val isNetwork = voice.isNetworkConnectionRequired
            val features = voice.features ?: emptySet()
            val isInstalled = !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            val locale = voice.locale

            val qualityStr = when (voice.quality) {
                Voice.QUALITY_VERY_HIGH, Voice.QUALITY_HIGH -> "High Quality"
                Voice.QUALITY_NORMAL -> "Standard Quality"
                Voice.QUALITY_LOW, Voice.QUALITY_VERY_LOW -> "Low Quality"
                else -> ""
            }

            val latencyStr = when (voice.latency) {
                Voice.LATENCY_VERY_LOW, Voice.LATENCY_LOW -> "Low Latency"
                Voice.LATENCY_NORMAL -> "Normal Latency"
                Voice.LATENCY_HIGH, Voice.LATENCY_VERY_HIGH -> "High Latency"
                else -> ""
            }

            return VoiceModel(
                id = voice.name,
                displayName = voice.name,
                localeTag = locale.toLanguageTag(),
                localeDisplayName = locale.displayName,
                isNetworkRequired = isNetwork,
                isInstalled = isInstalled,
                qualityLabel = qualityStr,
                latencyLabel = latencyStr
            )
        }
    }
}
