package com.sweak.qralarm.alarm.protection

/** Android-free policy so volume/ringer behavior can be tested with actual state changes. */
internal class AlarmAudioProtection(
    private val audio: AudioAccess,
    private val volumeFloor: Int?,
    private val keepRingerOn: Boolean
) {
    interface AudioAccess {
        var alarmVolume: Int
        var ringerMode: Int
        val canChangeRingerMode: Boolean
    }

    private val originalRingerMode = audio.ringerMode
    private var changedRingerMode = false
    private var stopped = false

    fun enforce() {
        if (stopped) return
        volumeFloor?.let { floor ->
            if (audio.alarmVolume < floor) audio.alarmVolume = floor
        }
        if (keepRingerOn && audio.canChangeRingerMode && audio.ringerMode != NORMAL_RINGER_MODE) {
            audio.ringerMode = NORMAL_RINGER_MODE
            changedRingerMode = changedRingerMode || audio.ringerMode == NORMAL_RINGER_MODE
        }
    }

    fun stop() {
        if (stopped) return
        stopped = true
        // Do not overwrite a new user mode after access has been revoked or a guard has stopped.
        if (changedRingerMode && audio.canChangeRingerMode &&
            audio.ringerMode == NORMAL_RINGER_MODE
        ) {
            audio.ringerMode = originalRingerMode
        }
    }

    companion object {
        const val NORMAL_RINGER_MODE = 2 // AudioManager.RINGER_MODE_NORMAL
    }
}
