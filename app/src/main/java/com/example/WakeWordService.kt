package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    private val serviceScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var wakeWordEngine: WakeWordEngine? = null
    private var detectionJob: Job? = null
    private var running = false

    override fun onCreate() {
        super.onCreate()

        wakeWordEngine = WakeWordEngine(
            context = this,
            models = listOf(
                WakeWordModel(
                    name = "Hey Jarvis",
                    modelPath = "hey_jarvis_v0.1.onnx",
                    threshold = 0.10f
                )
            ),
            detectionMode = DetectionMode.SINGLE_BEST,
            detectionCooldownMs = 2000L
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
            running = true
            startWakeWordDetection()
        }

        return START_STICKY
    }

    private fun startWakeWordDetection() {

        val engine = wakeWordEngine ?: return

        detectionJob?.cancel()

        detectionJob = serviceScope.launch {

            try {

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

                    running = false

                    engine.stop()

                    launchVoiceCall()

                    stopSelf()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Wake word detection stopped",
                    e
                )
            }
        }

        try {

            engine.start()

        } catch (e: IllegalStateException) {

            Log.e(
                TAG,
                "Unable to start wake word engine. " +
                        "Check RECORD_AUDIO permission.",
                e
            )

            running = false
            stopSelf()
        }
    }

    private fun launchVoiceCall() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

            putExtra(
                MainActivity.EXTRA_OPEN_VOICE_CALL,
                true
            )
        }

        startActivity(intent)
    }

    private fun buildNotification(): Notification {

        val channelId = "vision_wake_word"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Vision Wake Word",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(
            this,
            channelId
        )
            .setContentTitle("Vision is listening")
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

        detectionJob?.cancel()

        wakeWordEngine?.release()

        wakeWordEngine = null

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    companion object {

        private const val TAG =
            "VisionWakeWord"

        private const val NOTIFICATION_ID =
            4201
    }
}
