package com.sweak.qralarm.alarm.protection

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmAudioProtectionTest {
    private class Audio : AlarmAudioProtection.AudioAccess {
        override var alarmVolume = 4
        override var ringerMode = 0
        override var canChangeRingerMode = true
    }

    @Test fun disabledProtectionsLeaveBothSettingsAlone() {
        val audio = Audio()
        val protection = AlarmAudioProtection(audio, null, false)
        audio.alarmVolume = 0
        protection.enforce()
        protection.stop()
        assertEquals(0, audio.alarmVolume)
        assertEquals(0, audio.ringerMode)
    }

    @Test fun volumeFloorBlocksReductionButAllowsAnIncreaseWithoutChangingRinger() {
        val audio = Audio()
        val protection = AlarmAudioProtection(audio, 4, false)
        audio.alarmVolume = 0
        protection.enforce()
        assertEquals(4, audio.alarmVolume)
        audio.alarmVolume = 7
        protection.enforce()
        assertEquals(7, audio.alarmVolume)
        assertEquals(0, audio.ringerMode)
    }

    @Test fun ringerOnlyProtectionRestoresSilentModeAndDoesNotTouchVolume() {
        val audio = Audio()
        val protection = AlarmAudioProtection(audio, null, true)
        audio.alarmVolume = 0
        protection.enforce()
        assertEquals(2, audio.ringerMode)
        assertEquals(0, audio.alarmVolume)
        protection.stop()
        assertEquals(0, audio.ringerMode)
        protection.enforce()
        assertEquals(0, audio.ringerMode)
    }

    @Test fun deniedOrRevokedPolicyAccessDoesNotChangeRinger() {
        val audio = Audio().apply { canChangeRingerMode = false }
        val protection = AlarmAudioProtection(audio, 4, true)
        protection.enforce()
        assertEquals(0, audio.ringerMode)
        audio.canChangeRingerMode = true
        protection.enforce()
        assertEquals(2, audio.ringerMode)
        audio.canChangeRingerMode = false
        audio.ringerMode = 1
        audio.alarmVolume = 0
        protection.enforce()
        assertEquals(1, audio.ringerMode)
        assertEquals(4, audio.alarmVolume)
        protection.stop()
        assertEquals(1, audio.ringerMode)
    }

    @Test fun stoppingTwiceOrEnforcingAfterStopDoesNotRestoreOverUserChanges() {
        val audio = Audio().apply { ringerMode = 1 }
        val protection = AlarmAudioProtection(audio, 4, true)
        protection.enforce()
        protection.stop()
        assertEquals(1, audio.ringerMode)
        audio.ringerMode = 0
        audio.alarmVolume = 0
        protection.stop()
        protection.enforce()
        assertEquals(0, audio.ringerMode)
        assertEquals(0, audio.alarmVolume)
    }
}
