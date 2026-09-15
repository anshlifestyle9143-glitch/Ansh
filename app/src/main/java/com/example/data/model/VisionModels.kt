package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AiEngineType(
    val id: String,
    val displayName: String,
    val description: String,
    val modelTag: String,
    val iconName: String,
    val badgeColorHex: Long
) {
    VISION_CORE(
        id = "vision_core",
        displayName = "Vision Core",
        description = "Ultra-fast neural latency. Optimized for daily workflows, conversation, and fast execution.",
        modelTag = "gemini-3.5-flash-lite",
        iconName = "Bolt",
        badgeColorHex = 0xFF6750A4
    ),
    VISION_NEURAL_PRO(
        id = "vision_neural_pro",
        displayName = "Neural Pro",
        description = "High-precision deep reasoning engine. Specialized for software engineering, STEM, and complex problem-solving.",
        modelTag = "gemini-3.1-pro-preview",
        iconName = "Psychology",
        badgeColorHex = 0xFF21005D
    ),
    VISION_CREATIVE(
        id = "vision_creative",
        displayName = "Creative Studio",
        description = "Unbounded synthesis & imagination. Optimized for design ideation, storytelling, and persona crafting.",
        modelTag = "gemini-3.5-flash-lite",
        iconName = "AutoAwesome",
        badgeColorHex = 0xFF9A4058
    ),
    VISION_OFFLINE(
        id = "vision_offline",
        displayName = "Offline Vault Engine",
        description = "Local on-device reasoning & local memory recall. Operates without requiring cloud network latency.",
        modelTag = "on-device-local",
        iconName = "Storage",
        badgeColorHex = 0xFF7E5700
    )
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeEngineId: String = AiEngineType.VISION_CORE.id
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // USER, ASSISTANT, SYSTEM
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val engineName: String = "Vision Core",
    val latencyMs: Long = 0,
    val isFavorite: Boolean = false
)

enum class MemoryCategory(val displayName: String, val colorHex: Long) {
    USER_PROFILE("Profile & Identity", 0xFF6750A4),
    PREFERENCE("Preferences", 0xFF4A4458),
    PROJECT("Projects & Goals", 0xFF386A20),
    KNOWLEDGE("Custom Knowledge", 0xFF7E5700),
    CREATOR("Creator Tribute", 0xFF9A4058)
}

@Entity(tableName = "memory_facts")
data class MemoryFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // USER_PROFILE, PREFERENCE, PROJECT, KNOWLEDGE, CREATOR
    val keyName: String,
    val factDetail: String,
    val importance: Int = 3, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
