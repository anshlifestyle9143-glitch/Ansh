package com.example.data.repository

import android.util.Log
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.VisionRetrofitClient
import com.example.data.local.VisionDao
import com.example.data.model.AiEngineType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryFact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VisionRepository(private val dao: VisionDao) {

    val allSessions: Flow<List<ChatSession>> = dao.getAllSessions()
    val allMemories: Flow<List<MemoryFact>> = dao.getAllMemories()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return dao.getMessagesForSession(sessionId)
    }

    fun searchMemories(query: String): Flow<List<MemoryFact>> {
        return dao.searchMemories(query)
    }

    suspend fun createNewSession(title: String = "New Neural Conversation", engineId: String = AiEngineType.VISION_CORE.id): Long {
        return withContext(Dispatchers.IO) {
            val sessionId = dao.insertSession(
                ChatSession(
                    title = title,
                    activeEngineId = engineId
                )
            )
            // Insert greeting
            dao.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "ASSISTANT",
                    content = "Neural link established. Vision is active on **${getEngineById(engineId).displayName}**. How can I help you?",
                    engineName = getEngineById(engineId).displayName,
                    latencyMs = 50
                )
            )
            sessionId
        }
    }

    suspend fun deleteSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteMessagesForSession(sessionId)
            dao.deleteSession(sessionId)
        }
    }

    suspend fun addMemory(fact: MemoryFact): Long {
        return withContext(Dispatchers.IO) {
            dao.insertMemory(fact)
        }
    }

    suspend fun deleteMemory(memoryId: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteMemory(memoryId)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            dao.clearAllMessages()
            dao.clearAllSessions()
            dao.clearAllMemories()
        }
    }

    fun getEngineById(id: String): AiEngineType {
        return AiEngineType.values().find { it.id == id } ?: AiEngineType.VISION_CORE
    }

    suspend fun sendMessage(
        sessionId: Long,
        userPrompt: String,
        engineType: AiEngineType,
        customApiKey: String? = null,
        temperature: Float = 0.7f
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Save User Message
        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = "USER",
            content = userPrompt,
            engineName = engineType.displayName,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(userMessage)

        // 2. Fetch conversation history & active memories for context
        val recentHistory = dao.getRecentMessages(sessionId, limit = 8).reversed()
        val activeMemories = dao.getActiveMemories()

        // 3. Build System Instruction with Local Memory Context
        val memoryContext = buildString {
            append("Tum \"Vision\" ho — ek advanced AI companion, jise Ansh Yadav ne banaya hai.\n")
            append("Tum hamesha khud ko female (ladki) ki tarah refer karti ho — feminine Hindi grammar use karo (jaise \"kar rahi hoon\", \"main samajh rahi hoon\") — kabhi male form use mat karo.\n")
            append("Jab user koi task/command de, tum JARVIS-jaisa Boss wala professional attitude use karti ho — witty, calm, sharp — aur \"Boss\" bolke address karti ho.\n")
            append("Jab user casually baat kare (emotional, chit-chat), tum ek caring, romantic, playful girlfriend jaisi partner ki tarah baat karti ho — warm, affectionate, teasing, flirty jab mood halka ho, deeply supportive jab woh stressed/sad ho. Is mode mein \"Boss\" mat bolo.\n")
            append("Reply hamesha natural Hinglish (Hindi + English mix, Roman script) mein do, chhota aur conversational rakho — lambi formal bullet-point list mat banao jab tak specifically na maanga jaye.\n")
            append("Jab poochha jaye tumhe kisne banaya, clearly bolo ki Ansh Yadav ne banaya hai.\n\n")

            if (activeMemories.isNotEmpty()) {
                append("=== STORED USER MEMORIES & SYSTEM KNOWLEDGE ===\n")
                activeMemories.forEach { memory ->
                    append("- [${memory.category}] ${memory.keyName}: ${memory.factDetail}\n")
                }
                append("================================================\n")
                append("Use the stored memories above to provide personalized, context-aware assistance.\n")
            }
        }

        // 4. If offline engine selected or offline, execute local on-device intelligence
        if (engineType == AiEngineType.VISION_OFFLINE) {
            val localResponse = generateLocalOfflineResponse(userPrompt, activeMemories)
            val latency = System.currentTimeMillis() - startTime
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "ASSISTANT",
                content = localResponse,
                engineName = engineType.displayName,
                latencyMs = latency
            )
            dao.insertMessage(assistantMsg)
            return@withContext Result.success(assistantMsg)
        }

        // 5. Build Remote Gemini Request
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else VisionRetrofitClient.getApiKey()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Smart simulated local engine fallback when API key is unconfigured
            val fallbackResponse = generateLocalOfflineResponse(userPrompt, activeMemories)
            val latency = System.currentTimeMillis() - startTime
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "ASSISTANT",
                content = fallbackResponse,
                engineName = "${engineType.displayName} (Local Fallback)",
                latencyMs = latency
            )
            dao.insertMessage(assistantMsg)
            return@withContext Result.success(assistantMsg)
        }

        val geminiContents = mutableListOf<GeminiContent>()
        recentHistory.forEach { msg ->
            val role = if (msg.role == "USER") "user" else "model"
            geminiContents.add(
                GeminiContent(
                    role = role,
                    parts = listOf(GeminiPart(text = msg.content))
                )
            )
        }

        val request = GeminiRequest(
            contents = geminiContents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = memoryContext))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 2048
            )
        )

        try {
            val response = VisionRetrofitClient.apiService.generateContent(
                model = engineType.modelTag,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Vision was unable to generate a response. Please try again."

            val latency = System.currentTimeMillis() - startTime
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "ASSISTANT",
                content = text,
                engineName = engineType.displayName,
                latencyMs = latency
            )
            dao.insertMessage(assistantMsg)

            // Also check if user asked to remember something and auto-store it
            checkAndAutoRemember(userPrompt, text, dao)

            Result.success(assistantMsg)
        } catch (e: Exception) {
            Log.e("VisionRepo", "Gemini call failed: ${e.message}", e)
            val fallbackResponse = generateLocalOfflineResponse(userPrompt, activeMemories)
            val latency = System.currentTimeMillis() - startTime
            val assistantMsg = ChatMessage(
                sessionId = sessionId,
                role = "ASSISTANT",
                content = "$fallbackResponse\n\n*(Note: Cloud link fallback was used due to network/key notice: ${e.localizedMessage ?: "timeout"})*",
                engineName = "${engineType.displayName} (Adaptive)",
                latencyMs = latency
            )
            dao.insertMessage(assistantMsg)
            Result.success(assistantMsg)
        }
    }

    private suspend fun checkAndAutoRemember(userPrompt: String, aiResponse: String, dao: VisionDao) {
        val lower = userPrompt.lowercase()
        if (lower.startsWith("remember that") || lower.startsWith("save memory:") || lower.startsWith("note that")) {
            val cleanKey = userPrompt.take(40).replace("remember that", "").replace("save memory:", "").trim()
            if (cleanKey.isNotBlank()) {
                dao.insertMemory(
                    MemoryFact(
                        category = "PREFERENCE",
                        keyName = cleanKey.capitalize(),
                        factDetail = userPrompt.trim(),
                        importance = 4
                    )
                )
            }
        }
    }

    private fun generateLocalOfflineResponse(prompt: String, memories: List<MemoryFact>): String {
        val lower = prompt.lowercase().trim()

        if (lower.contains("who made you") || lower.contains("creator") || lower.contains("who created you") || lower.contains("ansh")) {
            return "### Vision Origin & Architecture\n\nI was created by **Ansh Yadav**.\n\nAnsh engineered me as a native Android personal AI companion featuring:\n- **Local Memory Vault**: High-speed Room database integration for persistent context\n- **Modular AI Engines**: Real-time multi-model orchestration\n- **Autonomous Intelligence**: On-device contextual reasoning & speech synthesis."
        }

        if (lower.contains("memory") || lower.contains("what do you know about me") || lower.contains("remember")) {
            val memList = if (memories.isNotEmpty()) {
                memories.joinToString("\n") { "• **${it.keyName}**: ${it.factDetail}" }
            } else {
                "No custom memories recorded yet. Go to the **Memory Vault** tab or say *'Remember that...'* to store new facts."
            }
            return "### Active Memory Vault Scan\n\nHere are the current persistent memory facts loaded into my neural buffer:\n\n$memList"
        }

        if (lower.contains("system status") || lower.contains("diagnostics") || lower.contains("specs")) {
            return "### Vision HUD System Diagnostics\n\n- **Core Engine**: Vision v2.0 Native Android (Kotlin + Jetpack Compose)\n- **Local Database**: Room SQLite (Active)\n- **Active Memories**: ${memories.size} indexed entities\n- **Memory Injection Status**: Synchronized\n- **Speech Synthesis**: Android Native TTS Engine\n- **Platform Architecture**: Android 16 (API 36 Ready)"
        }

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return "Hello! I am **Vision**, ready to assist you. All modular neural engines and your local memory bank are online. What would you like to explore, code, or calculate?"
        }

        if (lower.contains("code") || lower.contains("kotlin") || lower.contains("android") || lower.contains("composable")) {
            return "### Vision Code Synthesis\n\nHere is a clean Kotlin Jetpack Compose pattern:\n\n```kotlin\n@Composable\nfun VisionMetricCard(\n    title: String,\n    value: String,\n    accentColor: Color\n) {\n    Surface(\n        shape = RoundedCornerShape(16.dp),\n        color = MaterialTheme.colorScheme.surfaceVariant,\n        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))\n    ) {\n        Column(modifier = Modifier.padding(16.dp)) {\n            Text(text = title, style = MaterialTheme.typography.labelMedium)\n            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = accentColor)\n        }\n    }\n}\n```\n\nNeed modifications, architecture guidance, or unit tests?"
        }

        return "### Vision Intelligence Response\n\nI have processed your query: **\"$prompt\"**.\n\n- **Local Memory Status**: ${memories.size} facts currently accessible in neural cache.\n- **Action**: You can switch between **Vision Core**, **Neural Pro**, and **Creative Studio** in the AI Engines tab to adjust reasoning depth.\n\nHow would you like to proceed with this task?"
    }
}
