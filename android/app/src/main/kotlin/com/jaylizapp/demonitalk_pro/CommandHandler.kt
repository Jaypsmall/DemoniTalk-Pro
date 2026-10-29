package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.util.Log
import java.util.regex.Pattern
import kotlin.math.abs

class CommandHandler(private val context: Context) {

    private var internalListener: ((String) -> Unit)? = null
    private val accessibilityController = AccessibilityController(context)
    fun setInternalListener(listener: (String) -> Unit) {
        internalListener = listener
    }

    /**
     * Normaliza el texto reconocido por voz:
     * - elimina tildes
     * - convierte a minúsculas
     * - elimina espacios sobrantes
     * - unifica palabras utilizadas como wake word
     *
     * IMPORTANTE:
     * Solo sustituye palabras completas.
     * Así "demoni" puede convertirse en "maquina",
     * pero "demonizar" NO se modifica.
     */
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

        // Keep speech command parsing in Kotlin. The Rust JNI path aborted the
        // speech-result coroutine on some devices after Vosk returned a transcript.
        var normalizedText = text.normalize()
        var wakeWordDetected = false

        Log.i("CommandHandler", "Texto original: '$text' -> normalizado: '$normalizedText'")

        val foundWakeWord = wakeWords.firstOrNull { wakeWord ->
            normalizedText == wakeWord || normalizedText.startsWith("$wakeWord ")
        }

        if (foundWakeWord != null) {
            wakeWordDetected = true
            normalizedText = normalizedText.removePrefix(foundWakeWord).trim()
        } else if (requireWakeWord) {
            Log.w("CommandHandler", "Comando ignorado: falta la palabra de activación; se recibió '$normalizedText'")
            return CommandResult.Ignored
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

        Log.i("CommandHandler", "Ejecutando comando individual: '$normalizedText'")

        val isFirstChatPhrase = normalizedText.contains("primer chat") ||
                normalizedText.contains("primera conversacion") ||
                normalizedText.contains("primer contacto") ||
                normalizedText.contains("primer mensaje") ||
                normalizedText.contains("primera charla")

        if (isFirstChatPhrase) {
            val mentionsWhatsapp = normalizedText.contains("whatsapp") ||
                    normalizedText.contains("wasap") ||
                    normalizedText.contains("wasat") ||
                    normalizedText.contains("guatsap") ||
                    normalizedText.contains("guasa")

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

        val canonicalText = canonicalCommandPhrase(normalizedText)
        val command = commands
            .sortedByDescending { it.trigger.normalize().length }
            .firstOrNull { voiceCommand ->
                val trigger = voiceCommand.trigger.normalize()
                val canonicalTrigger = canonicalCommandPhrase(trigger)
                normalizedText == trigger ||
                        containsCommandPhrase(normalizedText, trigger) ||
                        canonicalText == canonicalTrigger ||
                        containsCommandPhrase(canonicalText, canonicalTrigger) ||
                        isFuzzyMatch(normalizedText, trigger)
            }

        if (command != null) {
            Log.i(
                "CommandHandler",
                "Disparador '" + command.trigger + "' reconocido para '" + normalizedText +
                        "'; requiereRoot=" + command.isRoot
            )
            if (command.action.startsWith("internal_")) {
                internalListener?.invoke(command.action)
            } else {
                executeAsync(command.action, forceRoot = command.isRoot)
            }
            return true
        }

        Log.w(
            "CommandHandler",
            "No hubo comando coincidente para '" + normalizedText +
                    "'. Disparadores cargados: " + commands.joinToString { it.trigger }
        )
        return false
    }

    private fun canonicalCommandPhrase(text: String): String {
        var canonical = text.normalize()
            .replace(Regex("^(?:el|la|los|las|un|una)\\s+"), "")
        val ordinals = mapOf(
            "primer" to "1", "primero" to "1", "primera" to "1",
            "segundo" to "2", "segunda" to "2",
            "tercer" to "3", "tercero" to "3", "tercera" to "3",
            "cuarto" to "4", "cuarta" to "4",
            "quinto" to "5", "quinta" to "5",
            "sexto" to "6", "sexta" to "6",
            "septimo" to "7", "septima" to "7",
            "octavo" to "8", "octava" to "8",
            "noveno" to "9", "novena" to "9",
            "decimo" to "10", "decima" to "10"
        )
        for ((word, number) in ordinals) {
            canonical = canonical.replace(Regex("\\b" + Regex.escape(word) + "\\b"), number)
        }
        return canonical.trim()
    }

    private fun containsCommandPhrase(text: String, phrase: String): Boolean {
        if (phrase.isBlank()) return false
        val pattern = Regex("(?<![a-z0-9])" + Regex.escape(phrase) + "(?![a-z0-9])")
        return pattern.containsMatchIn(text)
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
            val cleanParts = parts.map { it.trim().replace(Regex("^[*;,\\s]+"), "").trim() }
                .filter { it.isNotEmpty() }
            for ((index, cleanPart) in cleanParts.withIndex()) {
                executeSingleCommand(cleanPart, commands)
                if (index < cleanParts.lastIndex) {
                    val delay = if (cleanPart.startsWith("open_app:") || cleanPart.startsWith("abre ")) 1000L else 350L
                    try { Thread.sleep(delay) } catch (_: InterruptedException) { Thread.currentThread().interrupt(); break }
                }
            }
        }.start()
    }

    private fun executeAsync(action: String, forceRoot: Boolean = false) {
        Log.i("CommandHandler", "Despachando acción de voz: '$action' (forceRoot=$forceRoot)")
        Thread {
            try {
                processAction(action, forceRoot)
            } catch (e: Exception) {
                Log.e("CommandHandler", "Error ejecutando acción '$action'", e)
            }
        }.start()
    }

    private fun processAction(action: String, forceRoot: Boolean = false) {
        val cleanAction = action.trim()
        if (cleanAction.isEmpty()) return

        Log.i("CommandHandler", "Procesando acción: '$cleanAction'")

        if ((cleanAction.contains(",") || cleanAction.contains(";")) && !cleanAction.startsWith("type:")) {
            val subActions = cleanAction.split(Regex("""[,;]""")).map { it.trim() }.filter { it.isNotEmpty() }
            if (subActions.size > 1) {
                Thread {
                    for ((index, sub) in subActions.withIndex()) {
                        processAction(sub, forceRoot)
                        if (index < subActions.lastIndex) {
                            try { Thread.sleep(350) } catch (_: InterruptedException) { Thread.currentThread().interrupt(); break }
                        }
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

        if (forceRoot && !ShellUtils.isRootAvailable()) {
            Log.e("CommandHandler", "La acción '$cleanAction' requiere Root, pero Root no está disponible")
            return
        }

        val allowRoot = forceRoot || mode == "HYBRID" || mode == "ROOT"
        val allowAccessibility = !forceRoot && (mode == "HYBRID" || mode == "ACCESSIBILITY")
        val accessibilityOnlyAction = cleanAction.startsWith("open_app:") ||
                cleanAction.startsWith("click_") ||
                cleanAction in setOf("show_grid", "show_numbers", "hide_overlays", "click_send")

        if (cleanAction.startsWith("type:")) {
            val textToType = cleanAction.removePrefix("type:").trim()
            if (textToType.isEmpty()) {
                Log.w("CommandHandler", "Se ignoró una acción type: sin contenido")
                return
            }

            var typedViaAccessibility = false
            if (allowAccessibility) {
                try {
                    typedViaAccessibility = accessibilityController.execute(cleanAction)
                } catch (e: Exception) {
                    Log.e("CommandHandler", "Error ejecutando typing via accessibility: ${e.message}")
                }
            }

            Log.i("CommandHandler", "Escritura por accesibilidad: $typedViaAccessibility")

            if (!typedViaAccessibility && allowRoot && ShellUtils.isRootAvailable()) {
                try {
                    val escapedText = textToType.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "%s")
                    ShellUtils.executeCommand("input text \"$escapedText\"")
                } catch (e: Exception) {
                    Log.e("CommandHandler", "Error ejecutando typing via shell: ${e.message}")
                }
            }
            return
        }

        var shellSuccess = false

        if (allowRoot && !accessibilityOnlyAction && ShellUtils.isRootAvailable()) {
            shellSuccess = try {
                when {
                    cleanAction == "global_back" -> ShellUtils.executeCommand("input keyevent 4")
                    cleanAction == "global_home" -> ShellUtils.executeCommand("input keyevent 3")
                    cleanAction == "global_recents" -> ShellUtils.executeCommand("input keyevent 187")
                    cleanAction.startsWith("open_app:") || cleanAction.startsWith("click_") || cleanAction == "show_grid" || cleanAction == "show_numbers" || cleanAction == "hide_overlays" || cleanAction == "click_send" -> false
                    else -> ShellUtils.executeCommand(cleanAction)
                }
            } catch (e: Exception) {
                false
            }
        }

        if (forceRoot) {
            Log.i("CommandHandler", "Resultado de la acción Root '$cleanAction': $shellSuccess")
        }

        if (!shellSuccess && allowAccessibility) {
            try {
                val accessibilitySuccess = accessibilityController.execute(cleanAction)
                Log.i("CommandHandler", "Acción '$cleanAction' por accesibilidad: $accessibilitySuccess")
            } catch (e: Exception) {
                Log.e("CommandHandler", "Error ejecutando AccessibilityService: $cleanAction", e)
            }
        } else if (!shellSuccess) {
            Log.w("CommandHandler", "Acción '$cleanAction' sin ejecutarse: modo '$mode', requiereRoot=$forceRoot")
        }
    }

    enum class CommandResult {
        Ignored,
        WakeWordOnly,
        Executed
    }
}
