package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.VisionDatabase
import com.example.data.VisionRepository
import com.example.util.AiEngineType
import com.example.util.GeminiTtsManager
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    private val serviceScope =
        CoroutineScope(
            Dispatchers.Default + SupervisorJob()
        )

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var wakeWordEngine: WakeWordEngine? = null
    private var detectionJob: Job? = null

    private var overlayView: View? = null
    private var overlayWindowManager: WindowManager? = null

    private var voiceController: VisionVoiceController? = null
    private var ttsManager: GeminiTtsManager? = null

    private var voiceRepository: VisionRepository? = null
    private var voiceSessionId: Long? = null

    @Volatile
    private var running = false

    @Volatile
    private var restarting = false

    @Volatile
    private var voiceActive = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        ttsManager =
            GeminiTtsManager(this)

        voiceController =
            VisionVoiceController(
                context = this,
                ttsManager = ttsManager!!
            )

        val database =
            VisionDatabase.getDatabase(
                this,
                serviceScope
            )

        voiceRepository =
            VisionRepository(
                database.visionDao()
            )

        wakeWordEngine =
            createWakeWordEngine()
    }

    private fun createWakeWordEngine(): WakeWordEngine {

        return WakeWordEngine(
            context = this,

            models =
                listOf(
                    WakeWordModel(
                        name = "Hey Jarvis",
                        modelPath = "hey_jarvis_v0.1.onnx",
                        threshold = 0.10f
                    )
                ),

            detectionMode =
                DetectionMode.SINGLE_BEST,

            detectionCooldownMs =
                2000L
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        startForeground(
            NOTIFICATION_ID,
            buildNotification()
        )

        if (!running && !voiceActive) {
            startWakeWordDetection()
        }

        return START_STICKY
    }

    private fun startWakeWordDetection() {

        if (running || voiceActive) {
            return
        }

        val engine =
            wakeWordEngine
                ?: run {
                    wakeWordEngine =
                        createWakeWordEngine()

                    wakeWordEngine
                        ?: return
                }

        running = true
        restarting = false

        detectionJob?.cancel()

        detectionJob =
            serviceScope.launch {

                try {

                    engine.detections.collect { detection ->

                        if (
                            !running ||
                            voiceActive
                        ) {
                            return@collect
                        }

                        Log.d(
                            TAG,
                            "Wake word detected: " +
                                "${detection.model.name}, " +
                                "score=${detection.score}"
                        )

                        running = false

                        try {
                            engine.stop()
                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                "Engine stop warning",
                                e
                            )
                        }

                        try {
                            engine.release()
                        } catch (e: Exception) {
                            Log.w(
                                TAG,
                                "Engine release warning",
                                e
                            )
                        }

                        wakeWordEngine = null

                        delay(
                            MICROPHONE_HANDOFF_DELAY_MS
                        )

                        showVisionOverlay()

                        startVoiceController()
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Wake-word detection crashed",
                        e
                    )

                    running = false

                    scheduleRestart()
                }
            }

        try {

            engine.start()

            Log.d(
                TAG,
                "Wake-word detection started"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to start wake-word engine",
                e
            )

            running = false

            scheduleRestart()
        }
    }

    private fun showVisionOverlay() {

        mainHandler.post {

            try {

                if (
                    !Settings.canDrawOverlays(this)
                ) {

                    Log.w(
                        TAG,
                        "Overlay permission is not granted"
                    )

                    finishVoiceSession()

                    return@post
                }

                removeVisionOverlayNow()

                val windowManager =
                    getSystemService(
                        Context.WINDOW_SERVICE
                    ) as WindowManager

                val container =
                    FrameLayout(this)

                val hologram =
                    VisionOverlayView(this)

                container.addView(
                    hologram,
                    FrameLayout.LayoutParams(
                        dp(330),
                        dp(330),
                        Gravity.CENTER
                    )
                )

                val params =
                    WindowManager.LayoutParams(
                        dp(330),
                        dp(330),

                        WindowManager.LayoutParams
                            .TYPE_APPLICATION_OVERLAY,

                        WindowManager.LayoutParams
                            .FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams
                            .FLAG_NOT_TOUCH_MODAL,

                        PixelFormat.TRANSLUCENT
                    ).apply {

                        gravity =
                            Gravity.BOTTOM or
                                Gravity.CENTER_HORIZONTAL

                        y = dp(12)
                        x = 0
                        alpha = 1.0f
                    }

                windowManager.addView(
                    container,
                    params
                )

                overlayWindowManager =
                    windowManager

                overlayView =
                    container

                wakeScreenIfNeeded()

                Log.d(
                    TAG,
                    "Vision hologram overlay shown"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Unable to show Vision overlay",
                    e
                )

                removeVisionOverlayNow()

                finishVoiceSession()
            }
        }
    }

    private fun startVoiceController() {

        mainHandler.post {

            if (voiceActive) {
                return@post
            }

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                Log.w(
                    TAG,
                    "Microphone permission not granted"
                )

                finishVoiceSession()

                return@post
            }

            voiceActive = true

            voiceSessionId = null

            Log.d(
                TAG,
                "Starting VisionVoiceController"
            )

            voiceController?.start(

                onListening = {

                    Log.d(
                        TAG,
                        "Vision voice: LISTENING"
                    )
                },

                onThinking = {

                    Log.d(
                        TAG,
                        "Vision voice: THINKING"
                    )
                },

                onSpeaking = {

                    Log.d(
                        TAG,
                        "Vision voice: SPEAKING"
                    )
                },

                onConversation = { text ->

                    Log.d(
                        TAG,
                        "Conversation input: $text"
                    )

                    handleVoiceText(
                        text
                    )
                },

                onCommand = { command ->

                    Log.d(
                        TAG,
                        "Command input: $command"
                    )

                    /*
                     * For now the semantic ACTION is
                     * passed into the Vision brain.
                     *
                     * The real device-action executor
                     * will be connected in the next step.
                     */
                    handleVoiceText(
                        command
                    )
                },

                onDismiss = {

                    Log.d(
                        TAG,
                        "Voice session dismissed"
                    )

                    finishVoiceSession()
                }
            )
        }
    }

    private fun handleVoiceText(
        text: String
    ) {

        val cleanText =
            text.trim()

        if (
            cleanText.isBlank() ||
            !voiceActive
        ) {
            return
        }

        serviceScope.launch {

            try {

                val repository =
                    voiceRepository
                        ?: return@launch

                val sessionId =
                    voiceSessionId
                        ?: repository
                            .createNewSession(
                                title = "Voice Session"
                            )
                            .also {
                                voiceSessionId = it
                            }

                Log.d(
                    TAG,
                    "Sending voice input to Vision Core: $cleanText"
                )

                val result =
                    repository.sendMessage(
                        sessionId = sessionId,
                        userPrompt = cleanText,
                        engineType =
                            AiEngineType.VISION_CORE
                    )

                if (!voiceActive) {
                    return@launch
                }

                result
                    .onSuccess { message ->

                        val response =
                            message.content
                                .trim()

                        if (
                            response.isNotBlank() &&
                            voiceActive
                        ) {

                            mainHandler.post {

                                if (!voiceActive) {
                                    return@post
                                }

                                serviceScope.launch {

                                    speakVoiceResponse(
                                        response
                                    )
                                }
                            }

                        } else {

                            resumeVoiceListening()
                        }
                    }
                    .onFailure { error ->

                        Log.e(
                            TAG,
                            "Voice AI request failed",
                            error
                        )

                        resumeVoiceListening()
                    }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Voice text handling failed",
                    e
                )

                resumeVoiceListening()
            }
        }
    }

    private suspend fun speakVoiceResponse(
        response: String
    ) {

        if (!voiceActive) {
            return
        }

        val tts =
            ttsManager
                ?: return

        Log.d(
            TAG,
            "Vision response: $response"
        )

        tts.speak(
            response,
            -System.currentTimeMillis()
        )

        delay(
            RESPONSE_SPEAK_WAIT_MS
        )

        if (!voiceActive) {
            return
        }

        resumeVoiceListening()
    }

    private fun resumeVoiceListening() {

        mainHandler.post {

            if (!voiceActive) {
                return@post
            }

            Log.d(
                TAG,
                "Resuming voice listening"
            )

            voiceController?.resumeListening(
                VOICE_RESUME_DELAY_MS
            )
        }
    }

    private fun finishVoiceSession() {

        mainHandler.post {

            voiceActive = false
            voiceSessionId = null

            try {
                voiceController?.stop()
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Voice controller stop warning",
                    e
                )
            }

            removeVisionOverlayNow()

            if (!voiceActive) {
                restartWakeWordDetection()
            }
        }
    }

    private fun restartWakeWordDetection() {

        mainHandler.post {

            if (
                running ||
                voiceActive
            ) {
                return@post
            }

            Log.d(
                TAG,
                "Returning to passive wake-word listening"
            )

            wakeWordEngine?.let { engine ->

                try {
                    engine.stop()
                } catch (_: Exception) {
                }

                try {
                    engine.release()
                } catch (_: Exception) {
                }
            }

            wakeWordEngine =
                createWakeWordEngine()

            startWakeWordDetection()
        }
    }

    private fun removeVisionOverlayNow() {

        val view =
            overlayView

        val manager =
            overlayWindowManager

        overlayView = null
        overlayWindowManager = null

        if (
            view != null &&
            manager != null
        ) {

            try {

                manager.removeViewImmediate(
                    view
                )

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Overlay remove warning",
                    e
                )
            }
        }
    }

    private fun scheduleRestart() {

        if (
            restarting ||
            voiceActive
        ) {
            return
        }

        restarting = true

        serviceScope.launch {

            delay(
                RESTART_DELAY_MS
            )

            if (
                !running &&
                !voiceActive
            ) {

                try {
                    wakeWordEngine?.stop()
                } catch (_: Exception) {
                }

                try {
                    wakeWordEngine?.release()
                } catch (_: Exception) {
                }

                wakeWordEngine =
                    createWakeWordEngine()

                restarting = false

                startWakeWordDetection()

            } else {

                restarting = false
            }
        }
    }

    private fun wakeScreenIfNeeded() {

        try {

            val powerManager =
                getSystemService(
                    POWER_SERVICE
                ) as PowerManager

            if (!powerManager.isInteractive) {

                @Suppress("DEPRECATION")
                val wakeLock =
                    powerManager.newWakeLock(

                        PowerManager
                            .SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager
                            .ACQUIRE_CAUSES_WAKEUP,

                        "Vision::WakeWordScreen"
                    )

                wakeLock.acquire(
                    SCREEN_WAKE_DURATION_MS
                )

                Log.d(
                    TAG,
                    "Screen wake requested"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to wake screen",
                e
            )
        }
    }

    private fun createNotificationChannel() {

        if (
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Vision Wake Word",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }
    }

    private fun buildNotification(): Notification {

        return NotificationCompat
            .Builder(
                this,
                NOTIFICATION_CHANNEL_ID
            )
            .setContentTitle(
                "Vision is listening"
            )
            .setContentText(
                "Say \"Hey Jarvis\" to start talking"
            )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .setOngoing(true)
            .build()
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {

        Log.d(
            TAG,
            "App task removed — Wake Word service remains active"
        )

        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Wake Word service destroyed"
        )

        running = false
        restarting = false
        voiceActive = false
        voiceSessionId = null

        detectionJob?.cancel()
        detectionJob = null

        mainHandler.removeCallbacksAndMessages(
            null
        )

        try {
            voiceController?.destroy()
        } catch (_: Exception) {
        }

        voiceController = null

        try {
            ttsManager?.shutdown()
        } catch (_: Exception) {
        }

        ttsManager = null
        voiceRepository = null

        removeVisionOverlayNow()

        try {
            wakeWordEngine?.stop()
        } catch (_: Exception) {
        }

        try {
            wakeWordEngine?.release()
        } catch (_: Exception) {
        }

        wakeWordEngine = null

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    companion object {

        private const val TAG =
            "VisionWakeWord"

        private const val NOTIFICATION_ID =
            9143

        private const val NOTIFICATION_CHANNEL_ID =
            "vision_wake_word"

        private const val MICROPHONE_HANDOFF_DELAY_MS =
            500L

        private const val RESTART_DELAY_MS =
            2000L

        private const val SCREEN_WAKE_DURATION_MS =
            3000L

        private const val VOICE_RESUME_DELAY_MS =
            350L

        private const val RESPONSE_SPEAK_WAIT_MS =
            1200L
    }
}
