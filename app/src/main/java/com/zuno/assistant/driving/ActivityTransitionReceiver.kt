package com.zuno.assistant.driving

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.zuno.assistant.ZunoGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives IN_VEHICLE enter/exit events and records them. The manual override still wins. */
class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val last = result.transitionEvents.lastOrNull { it.activityType == DetectedActivity.IN_VEHICLE } ?: return
        val driving = last.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZunoGraph.settings.setDetected(driving)
            } finally {
                pending.finish()
            }
        }
    }
}
