package com.example.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class OvertimeAlertPlayer(private val context: Context) {

    fun triggerOvertimeAlert(playSound: Boolean, vibrate: Boolean) {
        if (playSound) {
            playGentleChime()
        }
        if (vibrate) {
            triggerHapticPulse()
        }
    }

    private fun playGentleChime() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = false
            }
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone.play()
        } catch (_: Exception) {
            // Safely ignore if audio hardware is unavailable (e.g. headless/test environments)
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerHapticPulse() {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 180, 100, 220)
                    val amplitudes = intArrayOf(0, 180, 0, 220)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(longArrayOf(0, 180, 100, 220), -1)
                }
            }
        } catch (_: Exception) {
            // Safely ignore if vibrator service is unavailable
        }
    }
}
