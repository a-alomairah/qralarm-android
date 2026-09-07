package com.sweak.qralarm.features.add_edit_alarm.destinations.special

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sweak.qralarm.R
import com.sweak.qralarm.alarm.protection.AlarmProtectionAccessibilityService
import com.sweak.qralarm.core.ui.compose_util.OnResume
import com.sweak.qralarm.features.add_edit_alarm.AddEditAlarmFlowState

@Composable
internal fun AlarmProtectionPermissions(state: AddEditAlarmFlowState) {
    val context = LocalContext.current
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    var accessibilityEnabled by remember {
        mutableStateOf(AlarmProtectionAccessibilityService.isEnabled(context))
    }
    var policyAccess by remember {
        mutableStateOf(notificationManager.isNotificationPolicyAccessGranted)
    }
    OnResume {
        accessibilityEnabled = AlarmProtectionAccessibilityService.isEnabled(context)
        policyAccess = notificationManager.isNotificationPolicyAccessGranted
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (state.isDoNotLeaveAlarmEnabled || state.isPowerOffGuardEnabled ||
            state.isBlockVolumeDownEnabled
        ) {
            Text(stringResource(if (accessibilityEnabled) {
                R.string.alarm_protection_accessibility_ready
            } else R.string.alarm_protection_accessibility_needed))
            Text(stringResource(R.string.alarm_protection_disclosure),
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                openSettings(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) { Text(stringResource(R.string.alarm_protection_open_accessibility)) }
            if (!accessibilityEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(stringResource(R.string.alarm_protection_restricted_settings),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.isKeepRingerOnEnabled) {
            Text(stringResource(if (policyAccess) R.string.alarm_protection_dnd_ready
                else R.string.alarm_protection_dnd_needed))
            TextButton(onClick = {
                openSettings(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }) { Text(stringResource(R.string.alarm_protection_open_dnd)) }
        }
        if (state.isBlockVolumeDownEnabled) {
            Text(stringResource(R.string.alarm_protection_volume_hint),
                style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.alarm_protection_limits),
            style = MaterialTheme.typography.bodySmall)
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.alarm_protection_samsung_title),
                        style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.alarm_protection_samsung_help))
                    TextButton(onClick = {
                        openSettings(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData("package:${context.packageName}".toUri()))
                    }) { Text(stringResource(R.string.alarm_protection_app_info)) }
                }
            }
        }
    }
}

private fun openSettings(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.alarm_protection_settings_unavailable,
            Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.alarm_protection_settings_unavailable,
            Toast.LENGTH_LONG).show()
    }
}
