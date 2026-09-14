package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
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

    private var wakeWordEngine: WakeWordEngine? = null
    private var detectionJob: Job? = null

    @Volatile
    private var running = false

    @Volatile
    private var restarting = false

    override fun onCreate() {
        super.onCreate()

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
                        modelPath =
                            "hey_jarvis_v0.1.onnx",
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

        if (!running) {
            startWakeWordDetection()
        }

        return START_STICKY
    }

    private fun startWakeWordDetection() {

        if (running) return

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

                    launch {

                        engine.detections.collect { detection ->

                            if (!running) {
                                return@collect
                            }

                            Log.d(
                                TAG,
                                "Wake word detected: " +
                                    "${detection.model.name}, " +
                                    "score=${detection.score}"
                            )

                            /*
                             * Immediately stop accepting new
                             * wake-word detections.
                             */
                            running = false

                            /*
                             * IMPORTANT:
                             *
                             * Completely release the wake-word
                             * audio engine before starting the
                             * Android SpeechRecognizer.
                             *
                             * This prevents both recognizers from
                             * competing for RECORD_AUDIO.
                             */
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

                            /*
                             * Give Android a short amount of time
                             * to release the audio input path.
                             */
                            delay(
                                MICROPHONE_HANDOFF_DELAY_MS
                            )

                            launchVoiceCall()
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
    }

    private fun scheduleRestart() {

        if (restarting) return

        restarting = true

        serviceScope.launch {

            delay(
                RESTART_DELAY_MS
            )

            if (!running) {

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
            }
        }
    }

    private fun launchVoiceCall() {

        try {

            /*
             * Request the screen to wake before opening Vision.
             */
            wakeScreenIfNeeded()

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                ).apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )

                    putExtra(
                        MainActivity.EXTRA_OPEN_VOICE_CALL,
                        true
                    )
                }

            startActivity(intent)

            Log.d(
                TAG,
                "Voice activity launch requested"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to launch voice screen",
                e
            )

            /*
             * If Activity launch fails, restart wake-word
             * detection so Vision does not remain silent.
             */
            scheduleRestart()
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
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
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

    private fun buildNotification(): Notification {

        val channelId =
            "vision_wake_word"

        if (
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Vision Wake Word",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }

        return NotificationCompat.Builder(
            this,
            channelId
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

    override fun onDestroy() {

        running = false
        restarting = false

        detectionJob?.cancel()
        detectionJob = null

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

        /*
         * Delay between releasing the wake-word microphone
         * and starting the voice conversation.
         */
        private const val MICROPHONE_HANDOFF_DELAY_MS =
            500L

        private const val RESTART_DELAY_MS =
            2000L

        private const val SCREEN_WAKE_DURATION_MS =
            3000L
    }
}
