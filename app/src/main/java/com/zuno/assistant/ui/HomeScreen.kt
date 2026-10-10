package com.zuno.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zuno.assistant.driving.DrivingOverride
import com.zuno.assistant.voice.VoiceCommand
import com.zuno.assistant.voice.VoiceState

@Composable
fun HomeScreen(vm: ZunoViewModel) {
    val context = LocalContext.current
    val voice by vm.voiceState.collectAsStateWithLifecycle()
    val queueSize by vm.queueSize.collectAsStateWithLifecycle()
    val lastHeard by vm.lastHeard.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val detectorMessage by vm.detectorMessage.collectAsStateWithLifecycle()

    // Re-check permissions whenever the user comes back from a system settings screen.
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val notificationAccess = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val micGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val activityGranted = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        if (granted) vm.listen()
    }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick++
        if (granted) vm.setAutoDetect(true)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- status -----------------------------------------------------------------------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Voice: " + describe(voice), style = MaterialTheme.typography.titleMedium)
                Text("Queued messages: $queueSize")
                lastHeard?.let { Text("Last heard: $it") }
            }
        }

        // ---- big voice controls -----------------------------------------------------------
        Button(
            onClick = {
                if (micGranted) vm.listen() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier.fillMaxWidth().height(72.dp),
        ) { Text("Listen (voice command)") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.command(VoiceCommand.READ) }, Modifier.weight(1f)) { Text("Read") }
            OutlinedButton(onClick = { vm.command(VoiceCommand.REPEAT) }, Modifier.weight(1f)) { Text("Repeat") }
            OutlinedButton(onClick = { vm.command(VoiceCommand.SKIP) }, Modifier.weight(1f)) { Text("Skip") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.command(VoiceCommand.PAUSE) }, Modifier.weight(1f)) { Text("Pause") }
            OutlinedButton(onClick = { vm.command(VoiceCommand.RESUME) }, Modifier.weight(1f)) { Text("Resume") }
            OutlinedButton(onClick = { vm.command(VoiceCommand.STOP) }, Modifier.weight(1f)) { Text("Stop") }
        }
        OutlinedButton(
            onClick = { vm.speakTest("Hello from Zuno. 10:30, 2.5, 1,200.50") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Test speech (uses your rules)") }

        // ---- driving mode -----------------------------------------------------------------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val d = settings.driving
                Text(
                    "Driving mode: " + if (d.active) "ON" else "OFF",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Auto-detection is approximate and can be wrong. Manual On/Off always wins. " +
                        "Detected driving right now: ${if (d.detectedDriving) "yes" else "no"}.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DrivingOverride.values().forEach { o ->
                        FilterChip(
                            selected = d.override == o,
                            onClick = { vm.setOverride(o) },
                            label = { Text(o.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Read new messages aloud while driving", Modifier.weight(1f))
                    Switch(checked = settings.autoRead, onCheckedChange = { vm.setAutoRead(it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-detect driving", Modifier.weight(1f))
                    Switch(
                        checked = settings.autoDetect,
                        onCheckedChange = { on ->
                            if (!on) vm.setAutoDetect(false)
                            else if (activityGranted) vm.setAutoDetect(true)
                            else activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                    )
                }
                detectorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }

        // ---- setup & permissions ----------------------------------------------------------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Notification access: " + if (notificationAccess) "granted" else "not granted",
                )
                Text(
                    "Lets Zuno read the text WhatsApp shows in its notifications so it can read " +
                        "them aloud. It cannot see full chats, and nothing is stored.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Open notification access settings") }

                Text("Microphone: " + if (micGranted) "granted" else "asked when you tap Listen")
                Text("Activity recognition: " + if (activityGranted) "granted" else "asked when you enable auto-detect")
                Text(
                    "Some phones stop background apps to save battery. If messages stop being " +
                        "read, set Zuno to unrestricted battery usage.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }) { Text("Open battery settings") }
            }
        }
    }
}

private fun describe(s: VoiceState): String = when (s) {
    VoiceState.Idle -> "idle"
    VoiceState.Listening -> "listening…"
    VoiceState.Processing -> "processing…"
    is VoiceState.Speaking -> "speaking (${s.who})"
    VoiceState.Paused -> "paused"
    is VoiceState.Error -> "error – ${s.message}"
}
