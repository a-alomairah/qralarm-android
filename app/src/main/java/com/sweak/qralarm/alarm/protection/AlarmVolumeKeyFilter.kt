package com.sweak.qralarm.alarm.protection

import android.view.KeyEvent

/** Keep DOWN/repeat/UP paired even when an alarm starts or ends in the middle of a press. */
internal class AlarmVolumeKeyFilter {
    private val consumed = mutableSetOf<Int>()
    val hasPendingKeys: Boolean get() = consumed.isNotEmpty()

    fun handle(keyCode: Int, action: Int, enabled: Boolean, repeatCount: Int = 0): Boolean {
        if (action == KeyEvent.ACTION_UP) return consumed.remove(keyCode)
        if (action != KeyEvent.ACTION_DOWN) return false
        if (keyCode in consumed) return true
        if (repeatCount > 0) return false // Initial DOWN went to the system before activation.
        if (enabled && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)) {
            consumed.add(keyCode)
            return true
        }
        // KEYCODE_MUTE is microphone mute, not speaker mute. Never intercept it.
        return false
    }

    fun clear() = consumed.clear()
}
