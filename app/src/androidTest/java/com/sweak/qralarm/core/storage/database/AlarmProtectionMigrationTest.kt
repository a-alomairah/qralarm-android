package com.sweak.qralarm.core.storage.database

import androidx.room.Room
import com.sweak.qralarm.core.data.alarm.AlarmsRepositoryImpl
import kotlinx.coroutines.runBlocking
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlarmProtectionMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), QRAlarmDatabase::class.java
    )

    @Test fun migrationPreservesAlarmAndDefaultsProtectionsOff() {
        val name = "alarm-protections-migration"
        helper.createDatabase(name, 10).apply {
            execSQL("""
                INSERT INTO alarm (alarmId, alarmHourOfDay, alarmMinute, isAlarmEnabled,
                    isAlarmRunning, nextAlarmTimeInMillis, numberOfSnoozes, snoozeDurationInMinutes,
                    numberOfSnoozesLeft, isAlarmSnoozed, ringtone, areVibrationsEnabled,
                    isUsingCode, gentleWakeUpDurationInSeconds, temporaryMuteDurationInSeconds)
                VALUES (42, 7, 30, 1, 0, 2000000000000, 3, 5, 3, 0, 'GENTLE_GUITAR', 1, 1, 30, 15)
            """.trimIndent())
            close()
        }
        helper.runMigrationsAndValidate(name, 11, true, QRAlarmDatabase.MIGRATION_10_11).apply {
            query("SELECT * FROM alarm WHERE alarmId = 42").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(cursor.getColumnIndexOrThrow("alarmHourOfDay")))
                assertEquals(30, cursor.getInt(cursor.getColumnIndexOrThrow("alarmMinute")))
                for (field in listOf("isDoNotLeaveAlarmEnabled", "isPowerOffGuardEnabled",
                    "isBlockVolumeDownEnabled", "isKeepRingerOnEnabled")) {
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow(field)))
                }
            }
            close()
        }
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            QRAlarmDatabase::class.java, name
        ).addMigrations(QRAlarmDatabase.MIGRATION_10_11).build()
        try {
            runBlocking {
                val repository = AlarmsRepositoryImpl(database.alarmsDao(), database.codesDao())
                val original = checkNotNull(repository.getAlarm(42))
                // Exercise both entity mappings with all independent on/off combinations.
                for (mask in 0..15) {
                    val configured = original.copy(
                        isDoNotLeaveAlarmEnabled = (mask and 1) != 0,
                        isPowerOffGuardEnabled = (mask and 2) != 0,
                        isBlockVolumeDownEnabled = (mask and 4) != 0,
                        isKeepRingerOnEnabled = (mask and 8) != 0
                    )
                    repository.addOrEditAlarm(configured)
                    assertEquals(configured, repository.getAlarm(42))
                }
            }
        } finally {
            database.close()
        }
    }
}
