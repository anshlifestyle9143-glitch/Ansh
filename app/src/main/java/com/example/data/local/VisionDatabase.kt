package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryFact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ChatSession::class, ChatMessage::class, MemoryFact::class],
    version = 1,
    exportSchema = false
)
abstract class VisionDatabase : RoomDatabase() {
    abstract fun visionDao(): VisionDao

    companion object {
        @Volatile
        private var INSTANCE: VisionDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): VisionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VisionDatabase::class.java,
                    "vision_assistant_database"
                )
                    .addCallback(VisionDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class VisionDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.visionDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: VisionDao) {
                // Default Session
                val sessionId = dao.insertSession(
                    ChatSession(
                        title = "Vision Neural Interface",
                        activeEngineId = "vision_core"
                    )
                )

                // Welcome message
                dao.insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "ASSISTANT",
                        content = "Greetings. I am **Vision**, your personal AI assistant created by **Ansh Yadav**.\n\nI am engineered with local memory capabilities, modular neural engine switching, and real-time reasoning. How may I assist your workflow today?",
                        engineName = "Vision Core",
                        latencyMs = 120
                    )
                )

                // Initial Memory Facts
                val initialMemories = listOf(
                    MemoryFact(
                        category = MemoryCategory.CREATOR.name,
                        keyName = "Creator & Architect",
                        factDetail = "Vision was conceived, designed, and developed by Ansh Yadav as an advanced native Android AI companion.",
                        importance = 5
                    ),
                    MemoryFact(
                        category = MemoryCategory.USER_PROFILE.name,
                        keyName = "Assistant Identity",
                        factDetail = "Vision is an intelligent, highly articulate, polite, precise, and proactive AI assistant with deep technical mastery.",
                        importance = 5
                    ),
                    MemoryFact(
                        category = MemoryCategory.PREFERENCE.name,
                        keyName = "Interaction Style",
                        factDetail = "Clear, concise, beautifully formatted markdown with high-fidelity code samples and structured bullet points.",
                        importance = 4
                    ),
                    MemoryFact(
                        category = MemoryCategory.KNOWLEDGE.name,
                        keyName = "Modular Architecture",
                        factDetail = "Equipped with Vision Core, Neural Pro, Creative Studio, and on-device Offline Vault engines.",
                        importance = 4
                    )
                )

                for (memory in initialMemories) {
                    dao.insertMemory(memory)
                }
            }
        }
    }
}
