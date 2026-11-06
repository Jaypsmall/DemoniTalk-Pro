package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

class CommandHandler(private val context: Context) {

    private var internalListener: ((String) -> Unit)? = null
    private val accessibilityController = AccessibilityController(context)
    private val gson = Gson()

    private data class RustProcessResult(
        val status: String? = null,
        val actions: List<String>? = null,
        val wake_word_detected: Boolean? = null,
        val detected_wake_word: String? = null
    )

    fun setInternalListener(listener: (String) -> Unit) {
        internalListener = listener
    }

    private fun String.normalize(): String {
        return VoiceTextNormalizer.normalize(this)
    }

    private val wakeWords: List<String>
        get() = listOf("demoni", "demonio", "hermano", "amigo", "maquina")

    fun execute(
        text: String,
        commands: List<VoiceCommand>,
        requireWakeWord: Boolean = false,
        isPartial: Boolean = false
    ): CommandResult {

        if (NativeEngine.isAvailable()) {
            try {
                val commandsJson = gson.toJson(commands)
                val responseJson = NativeEngine.processCommand(text, commandsJson, requireWakeWord)
                if (responseJson.isNotEmpty()) {
                    val rustResult = gson.fromJson(responseJson, RustProcessResult::class.java)
                    val status = rustResult?.status ?: "IGNORED"
                    val actionsList = rustResult?.actions ?: emptyList()

                    when (status) {
                        "EXECUTIVE" -> {
                            if (actionsList.isNotEmpty()) {
                                if (actionsList.size == 1) {
                                    val act = actionsList[0]
                                    if (act.startsWith("internal_")) {
                                        internalListener?.invoke(act)
                                    } else {
                                        executeAsync(act)
                                    }
                                } else {
                                    Thread {
                                        for (act in actionsList) {
                                            if (act.startsWith("internal_")) {
                                                internalListener?.invoke(act)
                                            } else {
                                                processAction(act)
                                            }
                                            try { Thread.sleep(1500) } catch (_: Exception) {}
                                        }
                                    }.start()
                                }
                                return CommandResult.Executed
                            }
                        }
                        "WAKE_WORD_ONLY" -> return CommandResult.WakeWordOnly
                        "IGNORED" -> {}
                    }
                }
            } catch (e: Exception) {
                Log.w("CommandHandler", "Error con Rust NativeEngine, fallback a Kotlin: ${e.message}")
            }
        }

        // Fallback Kotlin Command Engine
        var normalizedText = text.normalize()
        var wakeWordDetected = false

        Log.d("CommandHandler", "Texto original: '$text' -> normalizado: '$normalizedText'")

        if (requireWakeWord) {
            val foundWakeWord = wakeWords.firstOrNull { wakeWord ->
                normalizedText == wakeWord || normalizedText.startsWith("$wakeWord ")
            }

            if (foundWakeWord != null) {
                wakeWordDetected = true
                normalizedText = normalizedText.removePrefix(foundWakeWord).trim()
            } else {
                return CommandResult.Ignored
            }
        }

        if (wakeWordDetected && normalizedText.isEmpty()) {
            return CommandResult.WakeWordOnly
        }

        val sequenceRegex = Regex(
            """\s*[*;,]\s*|\s+(?:luego|después|despues|entonces)\s+(?:el\s+|la\s+|los\s+|las\s+|un\s+|una\s+)?|\s+y\s+(?:el\s+|la\s+|los\s+|las\s+|un\s+|una\s+)?(?=abre|abrir|abreme|pulsa|clic|click|escribe|manda|enviar|pon|reproduce|vuelve|atras|inicio|casa|recientes|buscar|busca|primer|primera|chat|cerrar|activar|desactivar|activa|modo|silencio|detente|cuadricula|numeros|oculta|foco|camara|\*)"""
        )

        val parts = normalizedText
            .split(sequenceRegex)
            .map { it.trim().replace(Regex("^[*;,\\s]+"), "").trim() }
            .filter { it.isNotEmpty() }

        if (parts.size > 1) {
            executeSequence(parts, commands)
            return CommandResult.Executed
        }

        val executed = executeSingleCommand(normalizedText, commands)
        return if (executed) CommandResult.Executed else CommandResult.Ignored
    }

    fun executeSingleCommand(
        text: String,
        commands: List<VoiceCommand>
    ): Boolean {

        var normalizedText = text.normalize()
        normalizedText = normalizedText.replace(Regex("^[*;,\\s]+"), "").trim()

        if (normalizedText.isEmpty()) return false

        Log.d("CommandHandler", "Ejecutando comando individual: '$normalizedText'")

        val isFirstChatPhrase = normalizedText.contains("primer chat") ||
                normalizedText.contains("primera conversacion") ||
                normalizedText.contains("primer contacto") ||
                normalizedText.contains("primer mensaje") ||
                normalizedText.contains("primera charla")

        if (isFirstChatPhrase) {
            val mentionsWhatsapp = normalizedText.contains("whatsapp") ||
                    normalizedText.contains("wasap") ||
                    normalizedText.contains("wasat") ||
                    normalizedText.contains("guatsap")

            if (mentionsWhatsapp) {
                executeSequence(listOf("open_app:whatsapp", "click_first_chat"), commands)
                return true
            } else {
                executeAsync("click_first_chat")
                return true
            }
        }

        when {
            normalizedText.startsWith("escribe ") -> {
                val content = normalizedText.removePrefix("escribe ").trim()
                if (content.isNotEmpty()) {
                    executeAsync("type:$content")
                    return true
                }
            }

            normalizedText.startsWith("manda este mensaje ") -> {
                val content = normalizedText.removePrefix("manda este mensaje ").trim()
                if (content.isNotEmpty()) {
                    executeAsync("type:$content")
                    return true
                }
            }
        }

        fun extractCleanAppName(text: String): String {
            var name = text.trim()
            val connectors = listOf(" para ", " de ", " en ", " y ")
            for (conn in connectors) {
                if (name.contains(conn)) {
                    name = name.substringBefore(conn).trim()
                }
            }
            return name
        }

        when {
            normalizedText.startsWith("abre ") -> {
                val appName = extractCleanAppName(normalizedText.removePrefix("abre "))
                if (appName.isNotEmpty()) {
                    executeAsync("open_app:$appName")
                    return true
                }
            }

            normalizedText.startsWith("abreme ") -> {
                val appName = extractCleanAppName(normalizedText.removePrefix("abreme "))
                if (appName.isNotEmpty()) {
                    executeAsync("open_app:$appName")
                    return true
                }
            }

            normalizedText.startsWith("abrir ") -> {
                val appName = extractCleanAppName(normalizedText.removePrefix("abrir "))
                if (appName.isNotEmpty()) {
                    executeAsync("open_app:$appName")
                    return true
                }
            }

            normalizedText.startsWith("abrirme ") -> {
                val appName = extractCleanAppName(normalizedText.removePrefix("abrirme "))
                if (appName.isNotEmpty()) {
                    executeAsync("open_app:$appName")
                    return true
                }
            }
        }

        when (normalizedText) {
            "vuelve atras", "atras", "volver atras" -> {
                executeAsync("global_back")
                return true
            }

            "inicio", "vete a casa", "ir a casa", "casa" -> {
                executeAsync("global_home")
                return true
            }

            "recientes", "aplicaciones recientes" -> {
                executeAsync("global_recents")
                return true
            }

            "enviar", "manda el mensaje", "enviar mensaje" -> {
                executeAsync("click_send")
                return true
            }

            "primer chat", "primer mensaje", "primera conversacion", "primer contacto" -> {
                executeAsync("click_first_chat")
                return true
            }

            "cuadricula", "cuadricula de numeros", "muestra cuadricula" -> {
                executeAsync("show_grid")
                return true
            }

            "numeros", "muestra numeros", "mostrar numeros", "actualizar", "actualizar numeros", "refrescar", "refrescar numeros" -> {
                executeAsync("show_numbers")
                return true
            }

            "oculta todo", "limpia pantalla", "ocultar todo" -> {
                executeAsync("hide_overlays")
                return true
            }

            "silencio", "para de escuchar", "deja de escuchar", "deja de oir", "detente" -> {
                internalListener?.invoke("internal_stop")
                return true
            }

            "activa c4", "activa c 4", "activar c4", "activar c 4", "modo azul", "modo vigilancia", "activar vigilancia" -> {
                internalListener?.invoke("internal_mode_blue")
                return true
            }

            "activar escucha", "activa escucha", "modo verde", "modo continuo", "escucha continua" -> {
                internalListener?.invoke("internal_continuous_on")
                return true
            }

            "siguiente cancion", "siguiente", "proxima" -> {
                executeAsync("media_next")
                return true
            }

            "cancion anterior", "anterior" -> {
                executeAsync("media_previous")
                return true
            }

            "para la musica", "para", "pausa", "deten la musica" -> {
                executeAsync("media_pause")
                return true
            }

            "reanuda la musica", "continua", "play" -> {
                executeAsync("media_play")
                return true
            }
        }

        if (normalizedText.startsWith("reproduce ") || normalizedText.startsWith("pon ")) {
            val query = normalizedText
                .replaceFirst("reproduce ", "")
                .replaceFirst("pon ", "")
                .trim()

            if (query.isNotEmpty()) {
                executeAsync("play_music:$query")
                return true
            }
        }

        if (normalizedText.startsWith("busca ") || normalizedText.startsWith("buscar ")) {
            val query = normalizedText
                .replaceFirst("busca ", "")
                .replaceFirst("buscar ", "")
                .trim()

            if (query.isNotEmpty()) {
                executeAsync("search_web:$query")
                return true
            }
        }

        val normalizedNumbersText = normalizedText
            .replace(Regex("""\buno\b"""), "1")
            .replace(Regex("""\bdos\b"""), "2")
            .replace(Regex("""\btres\b"""), "3")
            .replace(Regex("""\bcuatro\b"""), "4")
            .replace(Regex("""\bcinco\b"""), "5")
            .replace(Regex("""\bseis\b"""), "6")
            .replace(Regex("""\bsiete\b"""), "7")
            .replace(Regex("""\bocho\b"""), "8")
            .replace(Regex("""\bnueve\b"""), "9")
            .replace(Regex("""\bdiez\b"""), "10")

        val numberPattern = Pattern.compile(
            """(?:pulsa|pulsar|clic|click|numero|el|toca|tocar|presiona|presionar|selecciona|seleccionar|opcion)\s+(?:el\s+)?(\d+)"""
        )
        val matcher = numberPattern.matcher(normalizedNumbersText)

        if (matcher.find()) {
            val num = matcher.group(1)
            if (!num.isNullOrEmpty()) {
                executeAsync("click_number:$num")
                return true
            }
        }

        if (normalizedNumbersText.matches(Regex("""^\d+$"""))) {
            executeAsync("click_number:$normalizedNumbersText")
            return true
        }

        if (normalizedText.startsWith("pulsa ") || normalizedText.startsWith("toca ") || normalizedText.startsWith("presiona ")) {
            val targetText = normalizedText
                .replaceFirst("pulsa ", "")
                .replaceFirst("toca ", "")
                .replaceFirst("presiona ", "")
                .trim()
            if (targetText.isNotEmpty()) {
                executeAsync("click_text:$targetText")
                return true
            }
        }

        val command = commands.firstOrNull { voiceCommand ->
            val trigger = voiceCommand.trigger.normalize()
            normalizedText == trigger ||
                    normalizedText.contains(trigger) ||
                    isFuzzyMatch(normalizedText, trigger)
        }

        if (command != null) {
            if (command.action.startsWith("internal_")) {
                internalListener?.invoke(command.action)
            } else {
                executeAsync(command.action)
            }
            return true
        }

        return false
    }

    private fun isFuzzyMatch(text: String, trigger: String): Boolean {
        if (text.length < 4 || trigger.length < 4) return false
        if (abs(text.length - trigger.length) > 5) return false

        val prefixLength = (trigger.length * 0.7).toInt().coerceAtLeast(1)
        val prefix = trigger.substring(0, prefixLength.coerceAtMost(trigger.length))
        return text.contains(prefix)
    }

    private fun executeSequence(parts: List<String>, commands: List<VoiceCommand>) {
        Thread {
            for (part in parts) {
                val cleanPart = part.trim().replace(Regex("^[*;,\\s]+"), "").trim()
                if (cleanPart.isNotEmpty()) {
                    executeSingleCommand(cleanPart, commands)
                    try { Thread.sleep(1800) } catch (_: Exception) {}
                }
            }
        }.start()
    }

    private fun executeAsync(action: String) {
        Thread {
            try {
                processAction(action)
            } catch (e: Exception) {
                Log.e("CommandHandler", "Error ejecutando acción '$action'", e)
            }
        }.start()
    }

    private fun processAction(action: String) {
        val cleanAction = action.trim()
        if (cleanAction.isEmpty()) return

        if ((cleanAction.contains(",") || cleanAction.contains(";")) && !cleanAction.startsWith("type:")) {
            val subActions = cleanAction.split(Regex("""[,;]""")).map { it.trim() }.filter { it.isNotEmpty() }
            if (subActions.size > 1) {
                Thread {
                    for (sub in subActions) {
                        processAction(sub)
                        try { Thread.sleep(1500) } catch (_: Exception) {}
                    }
                }.start()
                return
            }
        }

        if (cleanAction.startsWith("internal_")) {
            internalListener?.invoke(cleanAction)
            return
        }

        val repository = CommandRepository(context)
        val mode = repository.getExecutionMode()

        val allowRoot = (mode == "HYBRID" || mode == "ROOT")
        val allowAccessibility = (mode == "HYBRID" || mode == "ACCESSIBILITY")

        var shellSuccess = false

        if (allowRoot && ShellUtils.isRootAvailable()) {
            shellSuccess = try {
                when {
                    cleanAction == "global_back" -> ShellUtils.executeCommand("input keyevent 4")
                    cleanAction == "global_home" -> ShellUtils.executeCommand("input keyevent 3")
                    cleanAction == "global_recents" -> ShellUtils.executeCommand("input keyevent 187")
                    cleanAction.startsWith("type:") -> {
                        val textToType = cleanAction.removePrefix("type:")
                        val escapedText = textToType.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "%s")
                        ShellUtils.executeCommand("input text \"$escapedText\"")
                    }
                    cleanAction.startsWith("open_app:") || cleanAction.startsWith("click_") || cleanAction == "show_grid" || cleanAction == "show_numbers" || cleanAction == "hide_overlays" || cleanAction == "click_send" -> false
                    else -> ShellUtils.executeCommand(cleanAction)
                }
            } catch (e: Exception) {
                false
            }
        }

        if (!shellSuccess && allowAccessibility) {
            try {
                accessibilityController.execute(cleanAction)
            } catch (e: Exception) {
                Log.e("CommandHandler", "Error ejecutando AccessibilityService: $cleanAction", e)
            }
        }
    }

    enum class CommandResult {
        Ignored,
        WakeWordOnly,
        Executed
    }
}
