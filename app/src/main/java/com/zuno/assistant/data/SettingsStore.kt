package com.zuno.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zuno.assistant.driving.DrivingOverride
import com.zuno.assistant.driving.DrivingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zunoDataStore: DataStore<Preferences> by preferencesDataStore(name = "zuno_settings")

data class Settings(
    val driving: DrivingState = DrivingState(),
    /** While driving mode is active, speak new messages automatically. */
    val autoRead: Boolean = true,
    /** Driving auto-detection (Activity Recognition) requested by the user. */
    val autoDetect: Boolean = false,
)

/** Small key/value settings live in DataStore; the rules list (a real data set) lives in Room. */
class SettingsStore(private val context: Context) {
    private object Keys {
        val override = stringPreferencesKey("driving_override")
        val detected = booleanPreferencesKey("detected_driving")
        val autoRead = booleanPreferencesKey("auto_read")
        val autoDetect = booleanPreferencesKey("auto_detect")
    }

    val flow: Flow<Settings> = context.zunoDataStore.data.map { p ->
        val override = runCatching { DrivingOverride.valueOf(p[Keys.override] ?: "AUTO") }
            .getOrDefault(DrivingOverride.AUTO)
        Settings(
            driving = DrivingState(override, p[Keys.detected] ?: false),
            autoRead = p[Keys.autoRead] ?: true,
            autoDetect = p[Keys.autoDetect] ?: false,
        )
    }

    suspend fun setOverride(o: DrivingOverride) = context.zunoDataStore.edit { it[Keys.override] = o.name }
    suspend fun setDetected(driving: Boolean) = context.zunoDataStore.edit { it[Keys.detected] = driving }
    suspend fun setAutoRead(v: Boolean) = context.zunoDataStore.edit { it[Keys.autoRead] = v }
    suspend fun setAutoDetect(v: Boolean) = context.zunoDataStore.edit { it[Keys.autoDetect] = v }
}
