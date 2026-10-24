package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AiAssistant(context: Context) {

    private val tag = "AiAssistant"
    private val repository = CommandRepository(context)

    fun isAiEnabled(): Boolean {
        return repository.isAiEnabled()
    }

    fun isAiAvailable(): Boolean {
        return isAiEnabled()
    }

    fun askFreeAi(prompt: String): String {
        val finalPrompt = buildPrompt(prompt)
        Log.i(tag, "Voice request entered AI provider chain")

        // 0. Petición a Groq API si hay API key configurada (ultra rápido para voz)
        val groqKey = repository.getGroqApiKey().trim()
        if (groqKey.isEmpty()) {
            val quickWebAnswer = fetchDuckDuckGoAnswer(prompt)
            if (!quickWebAnswer.isNullOrBlank()) {
                Log.i(tag, "Answer received through DuckDuckGo")
                return quickWebAnswer
            }
            return "Configura una clave de Groq para usar la IA."
        }

        val groqRes = fetchGroq(finalPrompt, groqKey)
        if (!groqRes.isNullOrBlank()) {
            Log.i(tag, "AI answered through Groq")
            return groqRes
        }
        Log.e(tag, "Groq failed; skipping slow secondary providers")
        return "No pude conectar con Groq. Revisa la conexión y tu clave de API."

        // 1. Petición POST dinámica a la IA LLM (Pollinations)

        // 2. Petición GET dinámica a la IA LLM

        // 3. Búsqueda web dinámica instantánea
        Log.e(tag, "All AI providers and web fallback failed")
        return "No pude conectar con los servidores de la IA en este momento. Revisa tu conexión a internet."
    }

    private fun buildPrompt(prompt: String): String {
        val instructions = repository.getAiInstructions().trim()
        val memory = repository.getAiMemory().trim()
        return buildString {
            if (instructions.isNotEmpty()) {
                appendLine("Instrucciones personalizadas: $instructions")
            }
            if (memory.isNotEmpty()) {
                appendLine("Memoria recordada: $memory")
            }
            append(prompt.trim())
        }
    }

    internal fun fetchPollinationsPost(prompt: String): String? {
        return try {
            val url = URL("https://text.pollinations.ai/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connection.setRequestProperty("Accept", "text/plain, application/json")
            connection.connectTimeout = 4000
            connection.readTimeout = 6000
            connection.doOutput = true

            val systemContent = "Eres Demoni, un asistente de voz fluido en español. Responde directo y en máximo 2 frases cortas. NUNCA uses emojis."
            val gson = com.google.gson.Gson()
            val escapedSystem = gson.toJson(systemContent)
            val escapedUser = gson.toJson(prompt)

            val jsonInputString = """
                {
                  "messages": [
                    {"role": "system", "content": $escapedSystem},
                    {"role": "user", "content": $escapedUser}
                  ],
                  "model": "openai"
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonInputString.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(charset("utf-8")).use { it.readText() }.trim()
                if (responseText.isNotEmpty()) return cleanResponse(responseText)
            } else {
                Log.w(tag, "Pollinations POST returned HTTP ${connection.responseCode}")
            }
            null
        } catch (e: Exception) {
            Log.e("AiAssistant", "Error en IA Gratuita (POST): ${e.message}")
            null
        }
    }

    fun fetchPollinationsGet(prompt: String): String? {
        return try {
            val systemPrompt = "Eres Demoni, un asistente de voz en español. Responde corto en máximo 2 frases. SIN EMOJIS."
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val encodedSystem = URLEncoder.encode(systemPrompt, "UTF-8")

            val urlString = "https://text.pollinations.ai/$encodedPrompt?system=$encodedSystem&model=openai"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connection.setRequestProperty("Accept", "*/*")
            connection.connectTimeout = 4000
            connection.readTimeout = 6000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(charset("utf-8")).use { it.readText() }.trim()
                if (responseText.isNotEmpty()) return cleanResponse(responseText)
            } else {
                Log.w(tag, "Pollinations GET returned HTTP ${connection.responseCode}")
            }
            null
        } catch (e: Exception) {
            Log.e("AiAssistant", "Error en IA Gratuita (GET): ${e.message}")
            null
        }
    }

    private fun fetchDuckDuckGoAnswer(prompt: String): String? {
        return try {
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val urlString = "https://api.duckduckgo.com/?q=$encodedPrompt&format=json&no_html=1&skip_disambig=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1200
            connection.readTimeout = 1800

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader(charset("utf-8")).use { it.readText() }
                val jsonObj = com.google.gson.JsonParser.parseString(json).asJsonObject
                val abstractText = jsonObj.get("AbstractText")?.asString?.trim() ?: ""
                if (abstractText.isNotEmpty()) return cleanResponse(abstractText)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    internal fun fetchGroq(prompt: String, apiKey: String): String? {
        return try {
            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile) DemoniTalkPro")
            connection.connectTimeout = 2500
            connection.readTimeout = 4500
            connection.doOutput = true

            val systemContent = "Eres Demoni, un asistente de voz conciso en español. Responde en una o dos frases muy cortas para ser leídas por voz. NUNCA uses emojis."
            val gson = com.google.gson.Gson()
            val escapedSystem = gson.toJson(systemContent)
            val escapedUser = gson.toJson(prompt)

            val jsonInputString = """
                {
                  "model": "openai/gpt-oss-120b",
                  "messages": [
                    {"role": "system", "content": $escapedSystem},
                    {"role": "user", "content": $escapedUser}
                  ]
                }
            """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonInputString.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(charset("utf-8")).use { it.readText() }.trim()
                if (responseText.isNotEmpty()) {
                    val jsonObj = com.google.gson.JsonParser.parseString(responseText).asJsonObject
                    val choices = jsonObj.get("choices") as? com.google.gson.JsonArray
                    if (choices != null && choices.size() > 0) {
                        val element = choices.get(0)
                        val messageObj = element.asJsonObject.getAsJsonObject("message")
                        val content = messageObj.get("content")?.asString?.trim() ?: ""
                        if (content.isNotEmpty()) return cleanResponse(content)
                    }
                }
            } else {
                val errorBody = connection.errorStream
                    ?.bufferedReader(charset("utf-8"))
                    ?.use { it.readText() }
                    ?.take(1000)
                    .orEmpty()
                Log.e("AiAssistant", "Error Groq API HTTP code: ${connection.responseCode}; body: $errorBody")
            }
            null
        } catch (e: Exception) {
            Log.e("AiAssistant", "Error en Groq API: ${e.message}")
            null
        }
    }

    private fun cleanResponse(text: String): String {
        return text
            .replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u27BF]"""), "")
            .replace(Regex("""[🚀😈🤖⚡🔥✨👍]"""), "")
            .trim()
    }
}
