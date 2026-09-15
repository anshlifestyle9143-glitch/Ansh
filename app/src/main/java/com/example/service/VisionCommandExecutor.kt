package com.example.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiRequest
import com.example.data.api.VisionRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Executes commands that have already been classified as ACTION.
 *
 * IMPORTANT:
 * This executor does NOT use keyword matching.
 *
 * Example:
 * "Phone ki torch jala do"
 * "Light on kar do"
 * "Andhera hai, light chalu karo"
 * "Turn on the flashlight"
 *
 * All can semantically resolve to:
 * FLASHLIGHT -> ON
 */
class VisionCommandExecutor(
    private val context: Context
) {

    companion object {
        private const val TAG = "VisionCommandExecutor"
        private const val MODEL = "gemini-3.5-flash"

        private const val FLASHLIGHT = "FLASHLIGHT"
        private const val ON = "ON"
        private const val OFF = "OFF"
        private const val UNKNOWN = "UNKNOWN"
    }

    data class CommandResult(
        val success: Boolean,
        val action: String,
        val message: String
    )

    private data class ParsedCommand(
        val action: String,
        val state: String
    )

    /**
     * Execute a natural-language device command.
     *
     * Semantic understanding is handled by Gemini.
     * No fixed phrase/keyword matching is used.
     */
    suspend fun execute(text: String): CommandResult =
        withContext(Dispatchers.IO) {

            val cleanText = text.trim()

            if (cleanText.isBlank()) {
                return@withContext CommandResult(
                    success = false,
                    action = UNKNOWN,
                    message = "Command samajh nahi aayi."
                )
            }

            val parsed = parseCommand(cleanText)

            Log.d(
                TAG,
                "Parsed command: action=${parsed.action}, state=${parsed.state}"
            )

            when (parsed.action) {

                FLASHLIGHT -> {
                    when (parsed.state) {
                        ON -> setFlashlight(true)
                        OFF -> setFlashlight(false)

                        else -> CommandResult(
                            success = false,
                            action = FLASHLIGHT,
                            message = "Boss, flashlight on ya off karni hai?"
                        )
                    }
                }

                else -> {
                    CommandResult(
                        success = false,
                        action = UNKNOWN,
                        message = "Boss, ye command abhi execute nahi kar sakti."
                    )
                }
            }
        }

    /**
     * Converts natural language into a structured device command.
     *
     * This is semantic understanding, not keyword detection.
     */
    private suspend fun parseCommand(
        text: String
    ): ParsedCommand = withContext(Dispatchers.IO) {

        val apiKey = VisionRetrofitClient.getCommandApiKey()

        if (
            apiKey.isBlank() ||
            apiKey == "MY_GEMINI_API_KEY"
        ) {
            Log.w(
                TAG,
                "Gemini API key unavailable"
            )

            return@withContext ParsedCommand(
                action = UNKNOWN,
                state = UNKNOWN
            )
        }

        val instruction = """
            You are Vision's device-command parser.

            Understand the COMPLETE meaning of the user's request.

            Do NOT use simple keyword matching.
            Understand Hindi, Hinglish and English naturally.

            Your job is to determine whether the user wants
            a supported device action.

            Currently supported action:

            FLASHLIGHT

            FLASHLIGHT states:

            ON
            OFF

            Return ONLY valid JSON.

            Required format:

            {
              "action": "FLASHLIGHT",
              "state": "ON"
            }

            or:

            {
              "action": "FLASHLIGHT",
              "state": "OFF"
            }

            If the request is not a supported device action:

            {
              "action": "UNKNOWN",
              "state": "UNKNOWN"
            }

            Examples:

            "Phone ki torch jala do"
            -> {"action":"FLASHLIGHT","state":"ON"}

            "Light on kar do"
            -> {"action":"FLASHLIGHT","state":"ON"}

            "Andhera hai, light chalu kar do"
            -> {"action":"FLASHLIGHT","state":"ON"}

            "Turn on the flashlight"
            -> {"action":"FLASHLIGHT","state":"ON"}

            "Torch band kar do"
            -> {"action":"FLASHLIGHT","state":"OFF"}

            "Flashlight off karo"
            -> {"action":"FLASHLIGHT","state":"OFF"}

            "Turn off the phone flash"
            -> {"action":"FLASHLIGHT","state":"OFF"}

            "Mummy ko call laga do"
            -> {"action":"UNKNOWN","state":"UNKNOWN"}

            "YouTube par Arijit Singh ke gaane chalao"
            -> {"action":"UNKNOWN","state":"UNKNOWN"}

            "Kaise ho?"
            -> {"action":"UNKNOWN","state":"UNKNOWN"}

            User command:
            $text
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(
                            text = instruction
                        )
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.0f,
                topP = 1.0f,
                topK = 1,
                maxOutputTokens = 40
            )
        )

        try {

            val response =
                VisionRetrofitClient.apiService.generateContent(
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
                    ?: ""

            Log.d(
                TAG,
                "Raw command parser response: $raw"
            )

            parseJsonResponse(raw)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Command parsing failed",
                e
            )

            ParsedCommand(
                action = UNKNOWN,
                state = UNKNOWN
            )
        }
    }

    /**
     * Safely extracts structured command information.
     */
    private fun parseJsonResponse(
        raw: String
    ): ParsedCommand {

        return try {

            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)

            val action =
                json.optString(
                    "action",
                    UNKNOWN
                ).uppercase()

            val state =
                json.optString(
                    "state",
                    UNKNOWN
                ).uppercase()

            when {
                action == FLASHLIGHT &&
                    state == ON -> {
                    ParsedCommand(
                        action = FLASHLIGHT,
                        state = ON
                    )
                }

                action == FLASHLIGHT &&
                    state == OFF -> {
                    ParsedCommand(
                        action = FLASHLIGHT,
                        state = OFF
                    )
                }

                else -> {
                    ParsedCommand(
                        action = UNKNOWN,
                        state = UNKNOWN
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Invalid command JSON",
                e
            )

            ParsedCommand(
                action = UNKNOWN,
                state = UNKNOWN
            )
        }
    }

    /**
     * Controls the physical phone flashlight.
     */
    private fun setFlashlight(
        enabled: Boolean
    ): CommandResult {

        return try {

            val cameraManager =
                context.getSystemService(
                    Context.CAMERA_SERVICE
                ) as CameraManager

            val cameraId =
                cameraManager.cameraIdList.firstOrNull { id ->

                    val characteristics =
                        cameraManager.getCameraCharacteristics(id)

                    characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true
                }

            if (cameraId == null) {

                return CommandResult(
                    success = false,
                    action = FLASHLIGHT,
                    message = "Boss, is phone me flashlight available nahi hai."
                )
            }

            cameraManager.setTorchMode(
                cameraId,
                enabled
            )

            if (enabled) {

                CommandResult(
                    success = true,
                    action = FLASHLIGHT,
                    message = "Flashlight on kar di Boss."
                )

            } else {

                CommandResult(
                    success = true,
                    action = FLASHLIGHT,
                    message = "Flashlight off kar di Boss."
                )
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Flashlight permission/security error",
                e
            )

            CommandResult(
                success = false,
                action = FLASHLIGHT,
                message = "Boss, flashlight control karne ki permission nahi mili."
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Flashlight execution failed",
                e
            )

            CommandResult(
                success = false,
                action = FLASHLIGHT,
                message = "Boss, flashlight control nahi ho payi."
            )
        }
    }
}
