package com.sweak.qralarm.alarm.protection

import java.util.Locale

/** Only call with content from system UI. Never classify arbitrary app text as a power menu. */
internal object PowerMenuMatcher {
    fun isSystemUi(packageName: String): Boolean =
        packageName == "com.android.systemui" || packageName == "com.samsung.android.globalactions"

    fun matches(packageName: String, className: String, labels: List<String>): Boolean {
        if (!isSystemUi(packageName)) return false
        if (className.contains("globalactions", ignoreCase = true)) return true
        val normalized = labels.map { it.lowercase(Locale.ROOT).trim() }
        // Require both labels: the notification shade can itself contain a 'Power off' shortcut.
        return normalized.any { it in powerOffLabels } && normalized.any { it in restartLabels }
    }

    private val powerOffLabels = setOf("power off", "shut down", "إيقاف التشغيل", "ايقاف التشغيل")
    private val restartLabels = setOf("restart", "reboot", "إعادة التشغيل", "اعادة التشغيل")
}
