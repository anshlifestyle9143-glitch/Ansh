package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WakeWordService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var recognizer: LiveSpeechRecognizer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var running = false

    private val wakeWords = listOf("hello vision", "hey vision", "vision")

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        recognizer = LiveSpeechRecognizer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!running) {
            running = true
            startCycle()
        }
        return START_STICKY
    }

    private fun requestDuckFocus(): Boolean {
        val am = audioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .build()
            am.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    private fun startCycle() {
        serviceScope.launch {
            while (running) {
                requestDuckFocus()

                var detected = false
                val cycleDone = CompletableDeferred<Unit>()

                recognizer?.start(
                    onPartial = {},
                    onFinal = { text ->
                        val lower = text.lowercase()
                        if (wakeWords.any { lower.contains(it) }) {
                            detected = true
                        }
                        if (!cycleDone.isCompleted) cycleDone.complete(Unit)
                    },
                    onListeningChange = {},
                    onError = {
                        if (!cycleDone.isCompleted) cycleDone.complete(Unit)
                    }
                )

                withTimeoutOrNull(6000) { cycleDone.await() }
                abandonFocus()

                if (detected) {
                    launchVoiceCall()
                    running = false
                    stopSelf()
                    break
                }

                delay(700)
            }
        }
    }

    private fun launchVoiceCall() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_VOICE_CALL, true)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val channelId = "vision_wake_word"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Vision Wake Word", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vision is listening")
            .setContentText("Say \"Hey Vision\" to start talking")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        recognizer?.destroy()
        abandonFocus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 4201
    }
}
