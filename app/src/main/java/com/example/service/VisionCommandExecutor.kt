package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
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
 */
class VisionCommandExecutor(
    private val context: Context
) {

    companion object {
        private const val TAG = "VisionCommandExecutor"
        private const val MODEL = "gemini-3.5-flash-lite"

        private const val FLASHLIGHT = "FLASHLIGHT"
        private const val CALL = "CALL"
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
        val state: String = UNKNOWN,
        val target: String = ""
    )

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
                "Parsed command: action=${parsed.action}, state=${parsed.state}, target=${parsed.target}"
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

                CALL -> {
                    makeCall(parsed.target)
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

    private suspend fun parseCommand(
        text: String
    ): ParsedCommand = withContext(Dispatchers.IO) {

        val apiKey = VisionRetrofitClient.getCommandApiKey()

        if (
            apiKey.isBlank() ||
            apiKey == "MY_GEMINI_API_KEY"
        ) {
            Log.w(TAG, "Gemini API key unavailable")

            return@withContext ParsedCommand(action = UNKNOWN)
        }

        val instruction = """
            You are Vision's device-command parser.

            Understand the COMPLETE meaning of the user's request.

            Do NOT use simple keyword matching.
            Understand Hindi, Hinglish and English naturally.

            Currently supported actions:

            1) FLASHLIGHT
               states: ON, OFF

            2) CALL
               target: the contact's name exactly as the user said it
               (e.g. "Mummy", "Rahul", "Papa")

            Return ONLY valid JSON.

            For flashlight:
            {
              "action": "FLASHLIGHT",
              "state": "ON"
            }
            or
            {
              "action": "FLASHLIGHT",
              "state": "OFF"
            }

            For calling:
            {
              "action": "CALL",
              "target": "Mummy"
            }

            If the request is not a supported device action:
            {
              "action": "UNKNOWN"
            }

            Examples:

            "Phone ki torch jala do"
            -> {"action":"FLASHLIGHT","state":"ON"}

            "Torch band kar do"
            -> {"action":"FLASHLIGHT","state":"OFF"}

            "Mummy ko call laga do"
            -> {"action":"CALL","target":"Mummy"}

            "Rahul ko phone lagao"
            -> {"action":"CALL","target":"Rahul"}

            "Papa ko call karo"
            -> {"action":"CALL","target":"Papa"}

            "YouTube par Arijit Singh ke gaane chalao"
            -> {"action":"UNKNOWN"}

            "Kaise ho?"
            -> {"action":"UNKNOWN"}

            User command:
            $text
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = instruction))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.0f,
                topP = 1.0f,
                topK = 1,
                maxOutputTokens = 60
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
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?: ""

            Log.d(TAG, "Raw command parser response: $raw")

            parseJsonResponse(raw)

        } catch (e: Exception) {

            Log.e(TAG, "Command parsing failed", e)

            ParsedCommand(action = UNKNOWN)
        }
    }

    private fun parseJsonResponse(raw: String): ParsedCommand {

        return try {

            val cleaned = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleaned)

            val action =
                json.optString("action", UNKNOWN).uppercase()

            when (action) {

                FLASHLIGHT -> {
                    val state =
                        json.optString("state", UNKNOWN).uppercase()

                    if (state == ON || state == OFF) {
                        ParsedCommand(action = FLASHLIGHT, state = state)
                    } else {
                        ParsedCommand(action = UNKNOWN)
                    }
                }

                CALL -> {
                    val target = json.optString("target", "").trim()

                    if (target.isNotBlank()) {
                        ParsedCommand(action = CALL, target = target)
                    } else {
                        ParsedCommand(action = UNKNOWN)
                    }
                }

                else -> ParsedCommand(action = UNKNOWN)
            }

        } catch (e: Exception) {

            Log.e(TAG, "Invalid command JSON", e)

            ParsedCommand(action = UNKNOWN)
        }
    }

    private fun setFlashlight(enabled: Boolean): CommandResult {

        val hasPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return CommandResult(
                success = false,
                action = FLASHLIGHT,
                message = "Boss, camera permission nahi mili. App kholke pehle allow karo."
            )
        }

        return try {

            val cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraId =
                cameraManager.cameraIdList.firstOrNull { id ->

                    val characteristics = cameraManager.getCameraCharacteristics(id)

                    val hasFlash = characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true

                    val isBackFacing = characteristics.get(
                        CameraCharacteristics.LENS_FACING
                    ) == CameraCharacteristics.LENS_FACING_BACK

                    hasFlash && isBackFacing

                } ?: cameraManager.cameraIdList.firstOrNull { id ->

                    val characteristics = cameraManager.getCameraCharacteristics(id)

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

            try {
                cameraManager.setTorchMode(cameraId, enabled)
            } catch (e: Exception) {
                Log.w(TAG, "First torch attempt failed, retrying", e)
            }

            Thread.sleep(150)

            cameraManager.setTorchMode(cameraId, enabled)

            if (enabled) {
                CommandResult(success = true, action = FLASHLIGHT, message = "Flashlight on kar di Boss.")
            } else {
                CommandResult(success = true, action = FLASHLIGHT, message = "Flashlight off kar di Boss.")
            }

        } catch (e: SecurityException) {

            Log.e(TAG, "Flashlight permission/security error", e)

            CommandResult(
                success = false,
                action = FLASHLIGHT,
                message = "Boss, flashlight control karne ki permission nahi mili."
            )

        } catch (e: Exception) {

            Log.e(TAG, "Flashlight execution failed", e)

            CommandResult(
                success = false,
                action = FLASHLIGHT,
                message = "Boss, flashlight control nahi ho payi."
            )
        }
    }

    /**
     * Looks up the contact by name and places a direct call.
     */
    private fun makeCall(contactName: String): CommandResult {

        if (contactName.isBlank()) {
            return CommandResult(
                success = false,
                action = CALL,
                message = "Boss, kisko call karna hai, naam bataiye."
            )
        }

        val hasCallPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasContactsPermission =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission || !hasContactsPermission) {
            return CommandResult(
                success = false,
                action = CALL,
                message = "Boss, call aur contacts ki permission nahi mili. App kholke pehle allow karo."
            )
        }

        val phoneNumber = findContactNumber(contactName)

        if (phoneNumber == null) {
            return CommandResult(
                success = false,
                action = CALL,
                message = "Boss, \"$contactName\" naam ka contact nahi mila."
            )
        }

        return try {

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            CommandResult(
                success = true,
                action = CALL,
                message = "$contactName ko call laga rahi hoon, Boss."
            )

        } catch (e: SecurityException) {

            Log.e(TAG, "Call permission/security error", e)

            CommandResult(
                success = false,
                action = CALL,
                message = "Boss, call lagane ki permission nahi mili."
            )

        } catch (e: Exception) {

            Log.e(TAG, "Call failed", e)

            CommandResult(
                success = false,
                action = CALL,
                message = "Boss, call lagane me problem aa gayi."
            )
        }
    }

    private fun findContactNumber(name: String): String? {

        return try {

            val resolver = context.contentResolver

            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val selection =
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"

            val selectionArgs = arrayOf("%$name%")

            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val numberIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                    if (numberIndex >= 0) cursor.getString(numberIndex) else null

                } else {
                    null
                }
            }

        } catch (e: Exception) {

            Log.e(TAG, "Contact lookup failed", e)

            null
        }
    }
}
