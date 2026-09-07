package com.sweak.qralarm.alarm.protection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerMenuMatcherTest {
    @Test fun recognizesAndroidAndSamsungMenusInEnglishAndArabic() {
        assertTrue(PowerMenuMatcher.matches("com.android.systemui", "Dialog",
            listOf("Power off", "Restart")))
        assertTrue(PowerMenuMatcher.matches("com.samsung.android.globalactions", "Dialog",
            listOf("إيقاف التشغيل", "إعادة التشغيل")))
        assertTrue(PowerMenuMatcher.matches("com.android.systemui",
            "com.samsung.android.globalactions.presentation.view.SamsungGlobalActionsDialog",
            emptyList()))
    }

    @Test fun doesNotDismissLookalikeAppContentOrNotificationShade() {
        assertFalse(PowerMenuMatcher.matches("com.example.notes", "GlobalActionsDialog",
            listOf("Power off", "Restart")))
        assertFalse(PowerMenuMatcher.matches("com.android.systemui", "NotificationShade",
            listOf("Power off", "Bluetooth", "Internet")))
        assertFalse(PowerMenuMatcher.matches("com.android.systemui", "NotificationShade",
            listOf("Restart your phone to install update", "Power off")))
    }
}
