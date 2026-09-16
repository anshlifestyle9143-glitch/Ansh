package com.example.service

import android.util.Log
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.VisionRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * VisionIntentClassifier
 *
 * Fast semantic intent detection.
 *
 * This class understands the complete meaning of the request.
 * It is NOT a simple keyword matcher.
 *
 * Supported:
 *
 * ACTION
 * CONVERSATION
 * QUESTION
 * SEARCH
 * UNCLEAR
 */
class VisionIntentClassifier {

    companion object {

        private const val TAG =
            "VisionIntentClassifier"

        /*
         * Lightweight Gemini model used only for
         * intent classification.
         */
        private const val MODEL =
            "gemini-3.5-flash-lite"

        private const val ACTION =
            "ACTION"

        private const val CONVERSATION =
            "CONVERSATION"

        private const val QUESTION =
            "QUESTION"

        private const val SEARCH =
            "SEARCH"

        private const val UNCLEAR =
            "UNCLEAR"
    }

    enum class IntentType {
        ACTION,
        CONVERSATION,
        QUESTION,
        SEARCH,
        UNCLEAR
    }

    data class IntentResult(
        val type: IntentType,
        val originalText: String
    )

    suspend fun classify(
        text: String
    ): IntentResult =
        withContext(Dispatchers.IO) {

            val cleanText =
                text.trim()

            if (cleanText.isBlank()) {

                return@withContext IntentResult(
                    type = IntentType.UNCLEAR,
                    originalText = cleanText
                )
            }

            val apiKey =
                VisionRetrofitClient
                    .getIntentApiKey()

            /*
             * No API key:
             *
             * Never guess an ACTION.
             */
            if (
                apiKey.isBlank() ||
                apiKey == "MY_GEMINI_API_KEY"
            ) {

                Log.w(
                    TAG,
                    "Intent API key unavailable"
                )

                return@withContext IntentResult(
                    type =
                        IntentType.CONVERSATION,
                    originalText =
                        cleanText
                )
            }

            /*
             * Compact semantic instruction.
             *
             * Keeping this prompt short reduces request
             * size and therefore reduces classification
             * latency.
             */
            val instruction = """
                Classify the user's COMPLETE meaning.
                Understand Hindi, Hinglish and English.
                Do NOT classify from isolated keywords.

                Return ONLY ONE label:
                ACTION
                CONVERSATION
                QUESTION
                SEARCH
                UNCLEAR

                ACTION = user wants a device, phone, app,
                communication, reminder, media or other real-world
                task performed.

                CONVERSATION = casual/social conversation with Vision.

                QUESTION = asks for knowledge, explanation,
                calculation, reasoning or information.

                SEARCH = explicitly asks to search/find/look up
                something online.

                UNCLEAR = meaning cannot be reliably determined.

                Examples:

                "Phone ki torch jala do" = ACTION
                "Andhera hai, light chalu kar do" = ACTION
                "Mummy ko call laga do" = ACTION
                "YouTube par Arijit Singh chalao" = ACTION

                "Kaise ho?" = CONVERSATION
                "Kya kar rahi ho?" = CONVERSATION
                "Mujhe tumse baat karni hai" = CONVERSATION

                "India ki capital kya hai?" = QUESTION
                "Photosynthesis kya hota hai?" = QUESTION
                "2 plus 2 kitna hai?" = QUESTION

                "Google par latest news search karo" = SEARCH
                "Internet par iske baare mein dekho" = SEARCH

                User:
                $cleanText
            """.trimIndent()

            val request =
                GeminiRequest(

                    contents =
                        listOf(
                            GeminiContent(
                                role = "user",
                                parts =
                                    listOf(
                                        GeminiPart(
                                            text =
                                                instruction
                                        )
                                    )
                            )
                        ),

                    generationConfig =
                        GeminiGenerationConfig(

                            /*
                             * Deterministic classification.
                             */
                            temperature = 0.0f,

                            /*
                             * No need for broad sampling.
                             */
                            topP = 1.0f,
                            topK = 1,

                            /*
                             * Output is only one word.
                             */
                            maxOutputTokens = 4
                        )
                )

            val startTime =
                System.currentTimeMillis()

            try {

                val response =
                    VisionRetrofitClient
                        .apiService
                        .generateContent(
                            model = MODEL,
                            apiKey = apiKey,
                            request = request
                        )

                val raw =
                    response
                        .candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?.trim()
                        ?.uppercase()
                        .orEmpty()

                /*
                 * Exact label matching is safer than
                 * contains().
                 *
                 * Example:
                 * "NOT_ACTION" should not become ACTION.
                 */
                val intent =
                    when (raw) {

                        ACTION ->
                            IntentType.ACTION

                        CONVERSATION ->
                            IntentType.CONVERSATION

                        QUESTION ->
                            IntentType.QUESTION

                        SEARCH ->
                            IntentType.SEARCH

                        UNCLEAR ->
                            IntentType.UNCLEAR

                        else -> {

                            /*
                             * Sometimes models return:
                             * "ACTION\n"
                             * or extra formatting.
                             *
                             * Take the first clean token.
                             */
                            val firstToken =
                                raw
                                    .replace(
                                        "`",
                                        ""
                                    )
                                    .replace(
                                        "*",
                                        ""
                                    )
                                    .split(
                                        Regex("\\s+")
                                    )
                                    .firstOrNull()
                                    .orEmpty()

                            when (firstToken) {

                                ACTION ->
                                    IntentType.ACTION

                                CONVERSATION ->
                                    IntentType.CONVERSATION

                                QUESTION ->
                                    IntentType.QUESTION

                                SEARCH ->
                                    IntentType.SEARCH

                                else ->
                                    IntentType.UNCLEAR
                            }
                        }
                    }

                val latency =
                    System.currentTimeMillis() -
                        startTime

                Log.d(
                    TAG,
                    "Intent=$intent | " +
                        "latency=${latency}ms | " +
                        "text=[$cleanText]"
                )

                IntentResult(
                    type = intent,
                    originalText = cleanText
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Intent classification failed",
                    e
                )

                /*
                 * Safety:
                 *
                 * Never execute an ACTION if the
                 * classifier fails.
                 */
                IntentResult(
                    type =
                        IntentType.CONVERSATION,
                    originalText =
                        cleanText
                )
            }
        }
}
