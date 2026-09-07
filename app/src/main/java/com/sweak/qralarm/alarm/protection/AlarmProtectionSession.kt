package com.sweak.qralarm.alarm.protection

import com.sweak.qralarm.core.domain.alarm.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Ephemeral: never re-arm from a stale database isAlarmRunning flag after process death. */
internal object AlarmProtectionSession {
    data class Session(
        val alarmId: Long,
        val doNotLeave: Boolean,
        val powerOffGuard: Boolean,
        val blockVolumeDown: Boolean,
        val keepRingerOn: Boolean
    )

    private val mutableSession = MutableStateFlow<Session?>(null)
    val state = mutableSession.asStateFlow()

    fun start(alarm: Alarm) {
        mutableSession.value = Session(
            alarm.alarmId,
            alarm.isDoNotLeaveAlarmEnabled,
            alarm.isPowerOffGuardEnabled,
            alarm.isBlockVolumeDownEnabled,
            alarm.isKeepRingerOnEnabled
        )
    }

    fun stop(alarmId: Long? = null) {
        mutableSession.update { if (alarmId == null || it?.alarmId == alarmId) null else it }
    }
}
