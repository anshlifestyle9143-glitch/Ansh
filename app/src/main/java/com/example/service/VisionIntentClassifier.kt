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

VisionIntentClassifier

Natural-language semantic intent detection.

IMPORTANT:

This class does NOT use keyword matching.

Examples:

"Phone ki torch jala do"

"Light on karo"

"Andhera hai, light chalu kar do"

can all be understood as ACTION.

While:

"Kaise ho?"

"Aaj kya kar rahi ho?"

are understood as CONVERSATION.
*/
class VisionIntentClassifier {

companion object {
private const val TAG = "VisionIntentClassifier"

private const val MODEL = "gemini-3.5-flash-lite"  

 private const val ACTION = "ACTION"  
 private const val CONVERSATION = "CONVERSATION"  
 private const val QUESTION = "QUESTION"  
 private const val SEARCH = "SEARCH"  
 private const val UNCLEAR = "UNCLEAR"

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

/**

Converts natural language into a high-level intent.

The model is explicitly instructed to understand meaning,

not search for fixed keywords.
*/
suspend fun classify(
text: String
): IntentResult = withContext(Dispatchers.IO) {

val cleanText = text.trim()

if (cleanText.isBlank()) {
return@withContext IntentResult(
type = IntentType.UNCLEAR,
originalText = cleanText
)
}

val apiKey =
VisionRetrofitClient.getIntentApiKey()

/*

If the Gemini key is unavailable, do NOT guess

using keywords. Safely treat it as conversation

so we never accidentally execute a device action.
*/
if (
apiKey.isBlank() ||
apiKey == "MY_GEMINI_API_KEY"
) {
Log.w(
TAG,
"Gemini API key unavailable; safe conversation fallback"
)

return@withContext IntentResult(
type = IntentType.CONVERSATION,
originalText = cleanText
)
}


val instruction = """
You are Vision's semantic intent classifier.

Your job is to understand the MEANING and INTENT  
 of the user's complete natural-language request.  

 Do NOT use simple keyword matching.  

 Understand Hindi, Hinglish and English naturally.  

 Return exactly ONE of these labels:  

 ACTION  
 CONVERSATION  
 QUESTION  
 SEARCH  
 UNCLEAR  

 Meaning:  

 ACTION:  
 The user wants Vision to perform a real-world,  
 phone/device/app task.  

 Examples:  
 "Phone ki torch jala do"  
 "Light off kar do"  
 "Mummy ko call laga do"  
 "Kal subah 7 baje mujhe utha dena"  
 "YouTube par Arijit Singh ke gaane chalao"  

 CONVERSATION:  
 The user is casually talking to Vision.  
 No external/device task is being requested.  

 Examples:  
 "Kaise ho?"  
 "Kya kar rahi ho?"  
 "Mujhe tumse baat karni hai"  

 QUESTION:  
 The user is primarily asking for information,  
 explanation, calculation, knowledge or reasoning.  

 Examples:  
 "India ki capital kya hai?"  
 "Photosynthesis kya hota hai?"  
 "2 plus 2 kitna hota hai?"  

 SEARCH:  
 The user explicitly wants something searched,  
 looked up or found online.  

 Examples:  
 "Google par latest news search karo"  
 "Internet par iske baare mein dekho"  

 UNCLEAR:  
 The request cannot be reliably understood.  

 IMPORTANT:  
 A request can contain words associated with actions  
 while still being conversation or a question.  
 Decide from the complete meaning and context.  

 User text:  
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
text = instruction
)
)
)
),
generationConfig =
GeminiGenerationConfig(
temperature = 0.0f,
topP = 1.0f,
topK = 1,
maxOutputTokens = 10
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
         ?.uppercase()  
         ?: ""  

 val intent =  
     when {  
         raw.contains(ACTION) ->  
             IntentType.ACTION  

         raw.contains(SEARCH) ->  
             IntentType.SEARCH  

         raw.contains(QUESTION) ->  
             IntentType.QUESTION  

         raw.contains(CONVERSATION) ->  
             IntentType.CONVERSATION  

         else ->  
             IntentType.UNCLEAR  
     }  

 Log.d(  
     TAG,  
     "Semantic intent: $intent | text=[$cleanText]"  
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
  * Safety rule:  
  * Never execute a device action when  
  * semantic classification failed.  
  */  
 IntentResult(  
     type = IntentType.CONVERSATION,  
     originalText = cleanText  
 )

}
}
}
