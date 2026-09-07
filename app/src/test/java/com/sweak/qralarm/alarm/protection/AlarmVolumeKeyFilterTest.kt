package com.sweak.qralarm.alarm.protection

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmVolumeKeyFilterTest {
    @Test fun blocksSpeakerMuteAndVolumeReductionOnly() {
        val filter = AlarmVolumeKeyFilter()
        for (key in listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)) {
            assertTrue(filter.handle(key, KeyEvent.ACTION_DOWN, true))
            assertTrue(filter.handle(key, KeyEvent.ACTION_UP, true))
        }
        for (key in listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_POWER)) {
            assertFalse(filter.handle(key, KeyEvent.ACTION_DOWN, true))
            assertFalse(filter.handle(key, KeyEvent.ACTION_UP, true))
        }
    }

    @Test fun doesNotConsumeOrphanUpWhenAlarmBeginsMidPress() {
        val filter = AlarmVolumeKeyFilter()
        assertFalse(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, false))
        assertFalse(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, true, 1))
        assertFalse(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, true))
    }

    @Test fun finishesCapturedPressWhenAlarmEndsMidPress() {
        val filter = AlarmVolumeKeyFilter()
        assertTrue(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, true))
        assertTrue(filter.hasPendingKeys)
        assertTrue(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, false))
        assertTrue(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, false))
        assertFalse(filter.hasPendingKeys)
        assertFalse(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, false))
        assertFalse(filter.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, false))
    }
}
