package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.data.auth.AuthManager
import com.example.data.local.VisionDatabase
import com.example.data.model.AiEngineType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryFact
import com.example.data.repository.VisionRepository
import com.example.service.WakeWordService
import com.example.util.TtsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VisionViewModel(application: Application) : AndroidViewModel(application) {

    private val database =
        VisionDatabase.getDatabase(application, viewModelScope)

    private val repository =
        VisionRepository(database.visionDao(), application)

    val ttsManager = TtsManager(application)

    /*
     * ---------------------------------------------------------
     * PERSISTENT SETTINGS
     * ---------------------------------------------------------
     */

    private val preferences =
        application.getSharedPreferences(
            PREFS_NAME,
            Application.MODE_PRIVATE
        )

    /*
     * ---------------------------------------------------------
     * SESSIONS
     * ---------------------------------------------------------
     */

    val sessions: StateFlow<List<ChatSession>> =
        repository.allSessions
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    private val _currentSessionId =
        MutableStateFlow<Long?>(null)

    val currentSessionId: StateFlow<Long?> =
        _currentSessionId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> =
        _currentSessionId
            .flatMapLatest { sessionId ->

                if (sessionId != null) {
                    repository.getMessagesForSession(sessionId)
                } else {
                    MutableStateFlow(emptyList())
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /*
     * ---------------------------------------------------------
     * MEMORY
     * ---------------------------------------------------------
     */

    private val _memorySearchQuery =
        MutableStateFlow("")

    val memorySearchQuery: StateFlow<String> =
        _memorySearchQuery.asStateFlow()

    val memories: StateFlow<List<MemoryFact>> =
        _memorySearchQuery
            .flatMapLatest { query ->

                if (query.isBlank()) {
                    repository.allMemories
                } else {
                    repository.searchMemories(query)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    /*
     * ---------------------------------------------------------
     * AI ENGINE
     * ---------------------------------------------------------
     */

    private val _activeEngine =
        MutableStateFlow(AiEngineType.VISION_CORE)

    val activeEngine: StateFlow<AiEngineType> =
        _activeEngine.asStateFlow()

    private val _isGenerating =
        MutableStateFlow(false)

    val isGenerating: StateFlow<Boolean> =
        _isGenerating.asStateFlow()

    /*
     * ---------------------------------------------------------
     * USER INPUT
     * ---------------------------------------------------------
     */

    private val _userInput =
        MutableStateFlow("")

    val userInput: StateFlow<String> =
        _userInput.asStateFlow()

    private val _temperature =
        MutableStateFlow(0.7f)

    val temperature: StateFlow<Float> =
        _temperature.asStateFlow()

    private val _customApiKey =
        MutableStateFlow("")

    val customApiKey: StateFlow<String> =
        _customApiKey.asStateFlow()

    /*
     * ---------------------------------------------------------
     * TOAST
     * ---------------------------------------------------------
     */

    private val _toastMessage =
        MutableStateFlow<String?>(null)

    val toastMessage: StateFlow<String?> =
        _toastMessage.asStateFlow()

    /*
     * ---------------------------------------------------------
     * WAKE WORD
     *
     * IMPORTANT:
     * The value is restored from local preferences so the
     * user's ON/OFF choice survives app restarts.
     * ---------------------------------------------------------
     */

    private val _wakeWordEnabled =
        MutableStateFlow(
            preferences.getBoolean(
                PREF_WAKE_WORD_ENABLED,
                false
            )
        )

    val wakeWordEnabled: StateFlow<Boolean> =
        _wakeWordEnabled.asStateFlow()

    /*
     * ---------------------------------------------------------
     * USER ACCOUNT
     * ---------------------------------------------------------
     */

    private val _userEmail =
        MutableStateFlow(
            AuthManager.currentUserEmail()
        )

    val userEmail: StateFlow<String?> =
        _userEmail.asStateFlow()

    /*
     * ---------------------------------------------------------
     * INIT
     * ---------------------------------------------------------
     */

    init {

        /*
         * Restore the last active chat session.
         */
        viewModelScope.launch {

            repository.allSessions.collect { sessionList ->

                if (
                    _currentSessionId.value == null &&
                    sessionList.isNotEmpty()
                ) {

                    _currentSessionId.value =
                        sessionList.first().id

                    val activeEngineId =
                        sessionList.first().activeEngineId

                    _activeEngine.value =
                        repository.getEngineById(
                            activeEngineId
                        )
                }
            }
        }

        /*
         * Restore Wake Word service if the user previously
         * enabled it.
         *
         * This does NOT enable Wake Word for a new user.
         * Default remains OFF.
         */
        if (_wakeWordEnabled.value) {
            startWakeWordService()
        }
    }

    /*
     * ---------------------------------------------------------
     * SESSION FUNCTIONS
     * ---------------------------------------------------------
     */

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _userInput.value = ""
    }

    fun createNewSession(
        title: String =
            "Neural Stream #${
                System.currentTimeMillis()
                    .toString()
                    .takeLast(4)
            }"
    ) {

        viewModelScope.launch {

            val newId =
                repository.createNewSession(
                    title,
                    _activeEngine.value.id
                )

            _currentSessionId.value =
                newId
        }
    }

    fun deleteSession(sessionId: Long) {

        viewModelScope.launch {

            repository.deleteSession(sessionId)

            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * INPUT / SETTINGS
     * ---------------------------------------------------------
     */

    fun onUserInputChange(text: String) {
        _userInput.value = text
    }

    fun setMemorySearchQuery(query: String) {
        _memorySearchQuery.value = query
    }

    fun setTemperature(temp: Float) {
        _temperature.value = temp
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setEngine(engine: AiEngineType) {
        _activeEngine.value = engine
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    /*
     * ---------------------------------------------------------
     * CHAT
     * ---------------------------------------------------------
     */

    fun sendMessage(
        overridePrompt: String? = null,
        autoSpeak: Boolean = false
    ) {

        val prompt =
            (overridePrompt ?: _userInput.value)
                .trim()

        if (
            prompt.isBlank() ||
            _isGenerating.value
        ) {
            return
        }

        val currentSession =
            _currentSessionId.value

        viewModelScope.launch {

            val targetSessionId =
                if (currentSession == null) {

                    val newId =
                        repository.createNewSession(
                            title = prompt.take(30),
                            engineId = _activeEngine.value.id
                        )

                    _currentSessionId.value =
                        newId

                    newId

                } else {

                    currentSession
                }

            _userInput.value = ""

            _isGenerating.value = true

            val result =
                repository.sendMessage(
                    sessionId = targetSessionId,
                    userPrompt = prompt,
                    engineType = _activeEngine.value,
                    customApiKey =
                        _customApiKey.value
                            .takeIf { it.isNotBlank() },
                    temperature =
                        _temperature.value
                )

            _isGenerating.value = false

            if (autoSpeak) {

                result
                    .getOrNull()
                    ?.let { assistantMessage ->

                        ttsManager.speak(
                            assistantMessage.content,
                            assistantMessage.id
                        )
                    }
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * MEMORY FUNCTIONS
     * ---------------------------------------------------------
     */

    fun addMemory(
        category: MemoryCategory,
        key: String,
        detail: String,
        importance: Int
    ) {

        if (
            key.isBlank() ||
            detail.isBlank()
        ) {
            return
        }

        viewModelScope.launch {

            repository.addMemory(
                MemoryFact(
                    category = category.name,
                    keyName = key.trim(),
                    factDetail = detail.trim(),
                    importance = importance
                )
            )

            _toastMessage.value =
                "Memory saved to Vault"
        }
    }

    fun deleteMemory(memoryId: Long) {

        viewModelScope.launch {

            repository.deleteMemory(memoryId)

            _toastMessage.value =
                "Memory forgotten"
        }
    }

    fun clearAllData() {

        viewModelScope.launch {

            repository.clearAllData()

            _currentSessionId.value = null

            createNewSession(
                "Vision Core System"
            )

            _toastMessage.value =
                "System data reset"
        }
    }

    /*
     * ---------------------------------------------------------
     * AUTH
     * ---------------------------------------------------------
     */

    fun onSignInResult(
        result: Result<String>
    ) {

        result
            .onSuccess { email ->

                _userEmail.value = email

                _toastMessage.value =
                    "Signed in as $email"
            }
            .onFailure { e ->

                _toastMessage.value =
                    "Sign-in failed: ${e.message}"
            }
    }

    fun restoreFromCloud() {

        viewModelScope.launch {

            val count =
                repository.restoreFromCloud()

            _toastMessage.value =
                when {

                    count > 0 ->
                        "Restored $count messages from cloud"

                    count == 0 ->
                        "No cloud messages found"

                    else ->
                        "Restore failed — check your connection"
                }
        }
    }

    fun signOut() {

        AuthManager.signOut()

        _userEmail.value = null

        _toastMessage.value =
            "Signed out"
    }

    /*
     * ---------------------------------------------------------
     * TTS
     * ---------------------------------------------------------
     */

    fun toggleSpeak(message: ChatMessage) {
        ttsManager.speak(
            message.content,
            message.id
        )
    }

    /*
     * ---------------------------------------------------------
     * WAKE WORD CONTROL
     * ---------------------------------------------------------
     */

    fun setWakeWordEnabled(enabled: Boolean) {
        _wakeWordEnabled.value = enabled
        if (enabled) {
            requestBatteryOptimizationExemption()
            startWakeWordService()
        } else {
            stopWakeWordService()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val context = getApplication<Application>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // some OEMs block this intent; ignore silently
            }
        }
    }

    fun startWakeWordService() {

        val context =
            getApplication<Application>()

        try {

            ContextCompat.startForegroundService(
                context,
                Intent(
                    context,
                    WakeWordService::class.java
                )
            )

        } catch (e: Exception) {

            _toastMessage.value =
                "Unable to start Wake Word service"
        }
    }

    fun stopWakeWordService() {

        val context =
            getApplication<Application>()

        try {

            context.stopService(
                Intent(
                    context,
                    WakeWordService::class.java
                )
            )

        } catch (e: Exception) {

            _toastMessage.value =
                "Unable to stop Wake Word service"
        }
    }

    /*
     * ---------------------------------------------------------
     * VIEWMODEL CLEANUP
     * ---------------------------------------------------------
     */

    override fun onCleared() {

        /*
         * IMPORTANT:
         * Do NOT stop Wake Word service here.
         *
         * ViewModel can be recreated while the application is
         * still alive. Wake Word should continue independently.
         */

        ttsManager.shutdown()

        super.onCleared()
    }

    companion object {

        private const val PREFS_NAME =
            "vision_preferences"

        private const val PREF_WAKE_WORD_ENABLED =
            "wake_word_enabled"
    }
}
