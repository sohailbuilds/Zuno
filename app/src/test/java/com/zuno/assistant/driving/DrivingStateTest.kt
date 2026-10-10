package com.zuno.assistant.driving

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingStateTest {
    @Test fun autoFollowsDetector() {
        assertTrue(DrivingState(DrivingOverride.AUTO, detectedDriving = true).active)
        assertFalse(DrivingState(DrivingOverride.AUTO, detectedDriving = false).active)
    }

    @Test fun manualOffWinsOverFalseDetection() {
        assertFalse(DrivingState(DrivingOverride.OFF, detectedDriving = true).active)
    }

    @Test fun manualOnWorksWithoutDetection() {
        assertTrue(DrivingState(DrivingOverride.ON, detectedDriving = false).active)
    }
}
