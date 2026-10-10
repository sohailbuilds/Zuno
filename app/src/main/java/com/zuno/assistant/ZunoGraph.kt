package com.zuno.assistant

import android.app.Application
import androidx.room.Room
import com.zuno.assistant.data.AppDatabase
import com.zuno.assistant.data.RuleRepository
import com.zuno.assistant.data.Settings
import com.zuno.assistant.data.SettingsStore
import com.zuno.assistant.driving.DrivingDetector
import com.zuno.assistant.queue.MessageQueue
import com.zuno.assistant.rules.RuleMatcher
import com.zuno.assistant.rules.SpeechRule
import com.zuno.assistant.voice.VoiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Tiny service locator: one place that wires the app's long-lived objects. */
object ZunoGraph {
    private lateinit var app: Application

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var settings: SettingsStore
        private set
    lateinit var ruleRepository: RuleRepository
        private set
    lateinit var detector: DrivingDetector
        private set

    val matcher = RuleMatcher()
    val queue = MessageQueue()

    @Volatile var currentSettings: Settings = Settings()
        private set
    @Volatile var currentRules: List<SpeechRule> = emptyList()
        private set

    /** Created lazily so a plain receiver wake-up doesn't spin up text-to-speech. */
    val voice: VoiceController by lazy {
        VoiceController(
            context = app,
            queue = queue,
            matcher = matcher,
            rulesProvider = { currentRules },
            drivingActive = { currentSettings.driving.active },
            autoRead = { currentSettings.autoRead },
        )
    }

    fun init(application: Application) {
        app = application
        settings = SettingsStore(app)
        val db = Room.databaseBuilder(app, AppDatabase::class.java, "zuno.db").build()
        ruleRepository = RuleRepository(db.ruleDao())
        detector = DrivingDetector(app)

        appScope.launch { settings.flow.collect { currentSettings = it } }
        appScope.launch { ruleRepository.rules.collect { currentRules = it } }
        appScope.launch {
            if (settings.flow.first().autoDetect && detector.hasPermission()) detector.start()
        }
    }
}
