package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryFact
import kotlinx.coroutines.flow.Flow

@Dao
interface VisionDao {

    // --- SESSIONS ---
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    // --- MESSAGES ---
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: Long, limit: Int): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET isFavorite = :favorite WHERE id = :messageId")
    suspend fun toggleFavorite(messageId: Long, favorite: Boolean)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    // --- MEMORY FACTS ---
    @Query("SELECT * FROM memory_facts ORDER BY importance DESC, createdAt DESC")
    fun getAllMemories(): Flow<List<MemoryFact>>

    @Query("SELECT * FROM memory_facts WHERE isActive = 1 ORDER BY importance DESC")
    suspend fun getActiveMemories(): List<MemoryFact>

    @Query("SELECT * FROM memory_facts WHERE keyName LIKE '%' || :query || '%' OR factDetail LIKE '%' || :query || '%'")
    fun searchMemories(query: String): Flow<List<MemoryFact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryFact): Long

    @Update
    suspend fun updateMemory(memory: MemoryFact)

    @Query("DELETE FROM memory_facts WHERE id = :memoryId")
    suspend fun deleteMemory(memoryId: Long)

    @Query("DELETE FROM memory_facts")
    suspend fun clearAllMemories()

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()
}
