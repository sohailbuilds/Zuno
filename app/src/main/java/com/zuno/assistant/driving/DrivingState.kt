package com.zuno.assistant.driving

/** AUTO follows the detector; ON/OFF are manual overrides that always win. */
enum class DrivingOverride { AUTO, ON, OFF }

data class DrivingState(
    val override: DrivingOverride = DrivingOverride.AUTO,
    /** Last result from Activity Recognition. Can be wrong (false positives/negatives). */
    val detectedDriving: Boolean = false,
) {
    val active: Boolean
        get() = when (override) {
            DrivingOverride.ON -> true
            DrivingOverride.OFF -> false
            DrivingOverride.AUTO -> detectedDriving
        }
}
