package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
                             * Stop accepting another wake word
                             * while Vision is responding.
                             */
                            running = false

                            /*
                             * Release microphone completely
                             * before showing the overlay.
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
                             * Give Android time to release
                             * the microphone.
                             */
                            delay(
                                MICROPHONE_HANDOFF_DELAY_MS
                            )

                            showVisionOverlay()
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

    private fun showVisionOverlay() {

        mainHandler.post {

            try {

                /*
                 * Android requires the user to grant
                 * "Display over other apps" permission.
                 */
                if (!Settings.canDrawOverlays(this)) {

                    Log.w(
                        TAG,
                        "Overlay permission is not granted"
                    )

                    scheduleRestart()
                    return@post
                }

                /*
                 * Avoid duplicate overlay instances.
                 */
                removeVisionOverlay()

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

                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,

                        PixelFormat.TRANSLUCENT
                    ).apply {

                        gravity =
                            Gravity.BOTTOM or
                                Gravity.CENTER_HORIZONTAL

                        y =
                            dp(12)

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

                /*
                 * Step 5 test:
                 * keep the hologram visible for 5 seconds,
                 * then return to passive wake-word listening.
                 */
                mainHandler.postDelayed(
                    {
                        removeVisionOverlay()
                        scheduleRestart()
                    },
                    OVERLAY_DURATION_MS
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Unable to show Vision overlay",
                    e
                )

                removeVisionOverlay()
                scheduleRestart()
            }
        }
    }

    private fun removeVisionOverlay() {

        mainHandler.post {

            val view =
                overlayView

            val manager =
                overlayWindowManager

            if (
                view != null &&
                manager != null
            ) {

                try {

                    manager.removeView(
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

            overlayView = null
            overlayWindowManager = null
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

    private fun dp(value: Int): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onDestroy() {

        running = false
        restarting = false

        detectionJob?.cancel()
        detectionJob = null

        mainHandler.removeCallbacksAndMessages(
            null
        )

        removeVisionOverlay()

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

        private const val MICROPHONE_HANDOFF_DELAY_MS =
            500L

        private const val RESTART_DELAY_MS =
            2000L

        private const val SCREEN_WAKE_DURATION_MS =
            3000L

        private const val OVERLAY_DURATION_MS =
            5000L
    }
}
