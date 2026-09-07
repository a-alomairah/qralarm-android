package com.sweak.qralarm.alarm.protection

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import com.sweak.qralarm.alarm.activity.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Opt-in assistance during ringing only. Does not inspect app content, log it, or send it anywhere. */
class AlarmProtectionAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restoreJob: Job? = null
    private var lastPowerDismissAt = 0L
    private val consumedKeys = mutableSetOf<Int>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            AlarmProtectionSession.state.collect { session ->
                restoreJob?.cancel()
                lastPowerDismissAt = 0L
                // No window events or key filtering while idle. Track paired key-up events if
                // dismissal occurred between DOWN and UP; release them before clearing flags.
                serviceInfo = serviceInfo.apply {
                    eventTypes = if (session?.doNotLeave == true || session?.powerOffGuard == true) {
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    } else 0
                    flags = if (session?.blockVolumeDown == true || consumedKeys.isNotEmpty()) {
                        flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                    } else flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val session = AlarmProtectionSession.state.value ?: return
        val foregroundPackage = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            foregroundPackage == packageName
        ) restoreJob?.cancel()
        if (foregroundPackage == packageName) return // Preserve scanner and emergency navigation.

        if (session.powerOffGuard && PowerMenuMatcher.isSystemUi(foregroundPackage)) {
            val labels = mutableListOf<String>()
            labels.addAll(event.text.map { it.toString() })
            // Content is traversed only for recognized system UI, and is never persisted.
            rootInActiveWindow?.let { root ->
                try {
                    if (PowerMenuMatcher.isSystemUi(root.packageName?.toString().orEmpty())) {
                        collectLabels(root, labels, intArrayOf(200))
                    }
                } finally {
                    @Suppress("DEPRECATION")
                    root.recycle()
                }
            }
            if (PowerMenuMatcher.matches(foregroundPackage,
                    event.className?.toString().orEmpty(), labels)
            ) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastPowerDismissAt >= 300) {
                    lastPowerDismissAt = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
                return
            }
        }
        // Only react to an actual window transition, not continuous content updates.
        if (session.doNotLeave && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            !isAllowedSystemWindow(foregroundPackage)
        ) {
            restoreJob = scope.launch {
                delay(500)
                if (AlarmProtectionSession.state.value != session) return@launch
                try {
                    startActivity(Intent(this@AlarmProtectionAccessibilityService,
                        AlarmActivity::class.java).apply {
                        putExtra(AlarmActivity.EXTRA_ALARM_ID, session.alarmId)
                        putExtra(AlarmActivity.EXTRA_LAUNCHED_FROM_MAIN_ACTIVITY, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                } catch (_: SecurityException) {
                    // OEM/OS restrictions can reject returning to the foreground. Audio continues.
                } catch (_: android.content.ActivityNotFoundException) {
                    // The service must not crash and interrupt another enabled accessibility tool.
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && consumedKeys.remove(event.keyCode)) return true
        val session = AlarmProtectionSession.state.value ?: return false
        if (!session.blockVolumeDown) return false
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE || event.keyCode == KeyEvent.KEYCODE_MUTE
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) consumedKeys.add(event.keyCode)
            return true
        }
        return false // Volume up and the power button retain their normal behavior.
    }

    private fun isAllowedSystemWindow(name: String): Boolean =
        name == "android" || PowerMenuMatcher.isSystemUi(name) ||
            name == "com.android.settings" || name.endsWith(".permissioncontroller") ||
            name.endsWith(".packageinstaller") || name == "com.android.phone" ||
            name == "com.android.server.telecom" || name.endsWith(".incallui") ||
            name.endsWith(".dialer") || name == "com.samsung.android.emergency"

    private fun collectLabels(node: AccessibilityNodeInfo, labels: MutableList<String>, budget: IntArray) {
        if (budget[0]-- <= 0) return
        node.text?.let { labels.add(it.toString()) }
        node.contentDescription?.let { labels.add(it.toString()) }
        for (index in 0 until node.childCount) {
            if (budget[0] <= 0) break
            val child = node.getChild(index) ?: continue
            try { collectLabels(child, labels, budget) }
            finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() { restoreJob?.cancel() }

    override fun onDestroy() {
        scope.cancel()
        consumedKeys.clear()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(AccessibilityManager::class.java)
            val component = ComponentName(context, AlarmProtectionAccessibilityService::class.java)
            return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { ComponentName(it.resolveInfo.serviceInfo.packageName,
                    it.resolveInfo.serviceInfo.name) == component }
        }
    }
}
