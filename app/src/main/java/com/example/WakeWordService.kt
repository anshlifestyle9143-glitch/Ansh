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

        createNotificationChannel()

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

        /*
         * IMPORTANT:
         *
         * START_STICKY tells Android that this service
         * should be recreated if Android kills it.
         */
        return START_STICKY
    }

    private fun startWakeWordDetection() {

        if (running) {
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
                         * while Vision is handling this event.
                         */
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

                        /*
                         * Give Android time to release
                         * the microphone before overlay work.
                         */
                        delay(
                            MICROPHONE_HANDOFF_DELAY_MS
                        )

                        showVisionOverlay()
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

                if (!Settings.canDrawOverlays(this)) {

                    Log.w(
                        TAG,
                        "Overlay permission is not granted"
                    )

                    scheduleRestart()

                    return@post
                }

                /*
                 * IMPORTANT:
                 *
                 * Remove the previous overlay SYNCHRONOUSLY.
                 *
                 * The old implementation posted the removal
                 * asynchronously, which could remove the NEW
                 * overlay immediately after it was added.
                 *
                 * That caused the very fast blink.
                 */
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

                /*
                 * Save the exact overlay references AFTER
                 * successful addView().
                 */
                overlayWindowManager =
                    windowManager

                overlayView =
                    container

                /*
                 * Wake screen if necessary.
                 */
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

                scheduleRestart()
            }
        }
    }

    /*
     * Public/internal overlay removal entry.
     *
     * This does NOT automatically remove the overlay.
     *
     * Future assistant controller can call this when the
     * complete command has finished.
     */
    private fun removeVisionOverlay() {

        mainHandler.post {

            removeVisionOverlayNow()
        }
    }

    /*
     * REAL synchronous removal.
     *
     * No nested Handler.post().
     *
     * This is the important fix for the blink problem.
     */
    private fun removeVisionOverlayNow() {

        val view =
            overlayView

        val manager =
            overlayWindowManager

        /*
         * Clear references BEFORE removing the view.
         *
         * This prevents duplicate removal attempts.
         */
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

        if (restarting) {
            return
        }

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

    private fun createNotificationChannel() {

        if (
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,

                    "Vision Wake Word",

                    NotificationManager
                        .IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            )
                .createNotificationChannel(
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

        /*
         * DO NOT stop the service here.
         *
         * The Activity can disappear from Recents while
         * this foreground service continues running.
         */
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

        detectionJob?.cancel()
        detectionJob = null

        mainHandler.removeCallbacksAndMessages(
            null
        )

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
    }
}
