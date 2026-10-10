package com.zuno.assistant.driving

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * Driving detection via Google Play services Activity Recognition (IN_VEHICLE transitions).
 * Approximate by nature: it can be wrong, can lag, and is unavailable on devices without Play
 * services. The user can always override it manually.
 */
class DrivingDetector(private val context: Context) {

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    fun start(onResult: (String) -> Unit = {}) {
        if (!hasPermission()) {
            onResult("Activity recognition permission is not granted.")
            return
        }
        val transitions = listOf(
            ActivityTransition.ACTIVITY_TRANSITION_ENTER,
            ActivityTransition.ACTIVITY_TRANSITION_EXIT,
        ).map {
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(it)
                .build()
        }
        try {
            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent())
                .addOnSuccessListener { onResult("Driving auto-detection is on (approximate).") }
                .addOnFailureListener { onResult("Could not start driving detection: ${it.message}") }
        } catch (e: SecurityException) {
            onResult("Permission denied for driving detection.")
        }
    }

    fun stop() {
        try {
            ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent())
        } catch (e: SecurityException) {
            // Nothing to remove without permission.
        }
    }
}
