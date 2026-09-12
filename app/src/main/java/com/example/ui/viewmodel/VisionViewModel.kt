package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.VisionDatabase
import com.example.data.model.AiEngineType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryFact
import com.example.data.repository.VisionRepository
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

    private val database = VisionDatabase.getDatabase(application, viewModelScope)
    private val repository = VisionRepository(database.visionDao())
    val ttsManager = TtsManager(application)

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                MutableStateFlow(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _memorySearchQuery = MutableStateFlow("")
    val memorySearchQuery: StateFlow<String> = _memorySearchQuery.asStateFlow()

    val memories: StateFlow<List<MemoryFact>> = _memorySearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allMemories
            } else {
                repository.searchMemories(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeEngine = MutableStateFlow(AiEngineType.VISION_CORE)
    val activeEngine: StateFlow<AiEngineType> = _activeEngine.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allSessions.collect { sessionList ->
                if (_currentSessionId.value == null && sessionList.isNotEmpty()) {
                    _currentSessionId.value = sessionList.first().id
                    val activeEngineId = sessionList.first().activeEngineId
                    _activeEngine.value = repository.getEngineById(activeEngineId)
                }
            }
        }
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun createNewSession(title: String = "Neural Stream #${System.currentTimeMillis().toString().takeLast(4)}") {
        viewModelScope.launch {
            val newId = repository.createNewSession(title, _activeEngine.value.id)
            _currentSessionId.value = newId
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

    fun sendMessage(overridePrompt: String? = null) {
        val prompt = (overridePrompt ?: _userInput.value).trim()
        if (prompt.isBlank() || _isGenerating.value) return

        val currentSession = _currentSessionId.value

        viewModelScope.launch {
            val targetSessionId = if (currentSession == null) {
                val newId = repository.createNewSession(
                    title = prompt.take(30),
                    engineId = _activeEngine.value.id
                )
                _currentSessionId.value = newId
                newId
            } else {
                currentSession
            }

            _userInput.value = ""
            _isGenerating.value = true

            repository.sendMessage(
                sessionId = targetSessionId,
                userPrompt = prompt,
                engineType = _activeEngine.value,
                customApiKey = _customApiKey.value.takeIf { it.isNotBlank() },
                temperature = _temperature.value
            )

            _isGenerating.value = false
        }
    }

    fun addMemory(category: MemoryCategory, key: String, detail: String, importance: Int) {
        if (key.isBlank() || detail.isBlank()) return
        viewModelScope.launch {
            repository.addMemory(
                MemoryFact(
                    category = category.name,
                    keyName = key.trim(),
                    factDetail = detail.trim(),
                    importance = importance
                )
            )
            _toastMessage.value = "Memory saved to Vault"
        }
    }

    fun deleteMemory(memoryId: Long) {
        viewModelScope.launch {
            repository.deleteMemory(memoryId)
            _toastMessage.value = "Memory forgotten"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _currentSessionId.value = null
            createNewSession("Vision Core System")
            _toastMessage.value = "System data reset"
        }
    }

    private val _userEmail = MutableStateFlow(com.example.data.auth.AuthManager.currentUserEmail())
    val userEmail: StateFlow<String?> = _userEmail

    fun onSignInResult(result: Result<String>) {
        result.onSuccess { email ->
            _userEmail.value = email
            _toastMessage.value = "Signed in as $email"
        }.onFailure { e ->
            _toastMessage.value = "Sign-in failed: ${e.message}"
        }
    }

    fun signOut() {
        com.example.data.auth.AuthManager.signOut()
        _userEmail.value = null
        _toastMessage.value = "Signed out"
    }

    fun toggleSpeak(message: ChatMessage) {
        ttsManager.speak(message.content, message.id)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
