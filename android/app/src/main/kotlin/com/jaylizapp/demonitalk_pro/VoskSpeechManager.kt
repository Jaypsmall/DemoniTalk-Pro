package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class VoskSpeechManager(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onSpeechResults: (String) -> Unit,
    private val onErrorRestart: () -> Unit,
    private val isModeActive: () -> Boolean,
    private val onErrorStopWaiting: () -> Unit = {},
    private val onPartialSpeechResults: ((String) -> Boolean)? = null
) : RecognitionListener {

    companion object {
        private const val TAG = "VoskSpeechManager"
        private const val MODEL_NAME = "model-es"
        private const val SAMPLE_RATE = 16000.0f
        // Pause after a Vosk segment before submitting the full phrase.
        private const val RESULT_SETTLE_MS = 1100L
    }

    private var voskModel: Model? = null
    private var speechService: SpeechService? = null
    private var activeRecognizer: Recognizer? = null
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingResultSegments = mutableListOf<String>()
    private var resultCommitRunnable: Runnable? = null

    @Volatile
    private var isInitializing = false

    @Volatile
    private var hasExecutedPartialInCurrentSession = false

    @Volatile
    private var listenRequested = false

    var isListening = false
        private set

    val isModelReady: Boolean
        get() = voskModel != null

    fun initModel(onComplete: ((Boolean) -> Unit)? = null) {
        if (voskModel != null) {
            onComplete?.invoke(true)
            return
        }
        if (isInitializing) return
        isInitializing = true

        Log.i(TAG, "Cargando modelo Vosk '$MODEL_NAME' para escucha sin internet...")
        StorageService.unpack(context, MODEL_NAME, MODEL_NAME,
            { model ->
                voskModel = model
                isInitializing = false
                Log.i(TAG, "¡Modelo Vosk '$MODEL_NAME' cargado con éxito!")
                onComplete?.invoke(true)
            },
            { exception ->
                isInitializing = false
                Log.e(TAG, "Error cargando modelo Vosk offline: ${exception.message}", exception)
                onComplete?.invoke(false)
            }
        )
    }

    fun startListening() {
        if (isListening) return
        listenRequested = true

        if (voskModel == null) {
            initModel { success ->
                if (!listenRequested) return@initModel
                if (success) {
                    startListeningInternal()
                } else if (isModeActive()) {
                    onErrorRestart()
                } else {
                    onErrorStopWaiting()
                }
            }
        } else {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (!listenRequested) return
        val model = voskModel ?: return
        try {
            stopSpeechService()

            val recognizer = Recognizer(model, SAMPLE_RATE)
            activeRecognizer = recognizer
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            hasExecutedPartialInCurrentSession = false

            isListening = true
            speechService?.startListening(this)
            onReady()
            Log.d(TAG, "Vosk Speech Service inició la escucha offline")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar Vosk SpeechService: ${e.message}", e)
            isListening = false
            if (isModeActive()) {
                onErrorRestart()
            } else {
                listenRequested = false
                onErrorStopWaiting()
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!isListening || hasExecutedPartialInCurrentSession || hypothesis.isNullOrEmpty()) return
        val text = parseJsonText(hypothesis, "partial")
        if (text.isNotEmpty()) {
            if (onPartialSpeechResults?.invoke(text) == true) {
                hasExecutedPartialInCurrentSession = true
                isListening = false
                stopListening()
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        if (!isListening) return
        if (hasExecutedPartialInCurrentSession) {
            hasExecutedPartialInCurrentSession = false
            return
        }
        // Keep the recorder active to collect a following speech segment.
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "text")
            if (text.isNotEmpty()) {
                Log.i(TAG, "Vosk reconoció: '$text'")
                pendingResultSegments.add(text)
                scheduleResultCommit()
                return
            }
        }
        Log.d(TAG, "Segmento Vosk vacio; se mantiene el microfono abierto")
    }

    override fun onFinalResult(hypothesis: String?) {
        if (!isListening) return
        if (hasExecutedPartialInCurrentSession) {
            hasExecutedPartialInCurrentSession = false
            return
        }
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "text")
            if (text.isNotEmpty()) {
                Log.i(TAG, "Resultado final Vosk: '$text'")
                pendingResultSegments.add(text)
            }
        }
        commitPendingResults()
    }

    override fun onError(exception: Exception?) {
        if (!isListening) return
        Log.e(TAG, "Error Vosk Speech: ${exception?.message}", exception)
        if (pendingResultSegments.isNotEmpty()) commitPendingResults() else handleNoSpeechResult()
    }

    override fun onTimeout() {
        if (!isListening) return
        Log.d(TAG, "Vosk Speech timeout")
        if (pendingResultSegments.isNotEmpty()) commitPendingResults() else handleNoSpeechResult()
    }

    private fun scheduleResultCommit() {
        resultCommitRunnable?.let(mainHandler::removeCallbacks)
        val runnable = Runnable { commitPendingResults() }
        resultCommitRunnable = runnable
        mainHandler.postDelayed(runnable, RESULT_SETTLE_MS)
    }

    private fun commitPendingResults() {
        if (!isListening) return
        resultCommitRunnable?.let(mainHandler::removeCallbacks)
        resultCommitRunnable = null

        val text = pendingResultSegments.joinToString(" ").trim()
        pendingResultSegments.clear()
        isListening = false
        stopSpeechService()

        if (text.isNotEmpty()) {
            Log.i(TAG, "Transcripcion Vosk consolidada: '$text'")
            onSpeechResults(text)
        } else {
            handleNoSpeechResult()
        }
    }

    private fun handleNoSpeechResult() {
        resultCommitRunnable?.let(mainHandler::removeCallbacks)
        resultCommitRunnable = null
        pendingResultSegments.clear()
        isListening = false
        if (isModeActive()) {
            onErrorRestart()
        } else {
            listenRequested = false
            onErrorStopWaiting()
        }
    }

    fun stopListening() {
        listenRequested = false
        stopSpeechService()
    }

    private fun stopSpeechService() {
        isListening = false
        resultCommitRunnable?.let(mainHandler::removeCallbacks)
        resultCommitRunnable = null
        pendingResultSegments.clear()
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (_: Exception) {
        }
        speechService = null
        try {
            activeRecognizer?.close()
        } catch (_: Exception) {
        }
        activeRecognizer = null
    }

    private fun parseJsonText(json: String?, key: String): String {
        if (json.isNullOrEmpty()) return ""
        return try {
            val jsonObj = gson.fromJson(json, JsonObject::class.java)
            jsonObj?.get(key)?.asString?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun destroy() {
        stopListening()
        try {
            voskModel?.close()
        } catch (_: Exception) {
        }
        voskModel = null
    }
}
