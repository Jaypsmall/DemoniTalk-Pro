package com.jaylizapp.demonitalk_pro

import android.content.Context
import android.util.Log

class DualSpeechManager(
    private val context: Context,
    private val repository: CommandRepository,
    private val onReady: () -> Unit,
    private val onSpeechResults: (String) -> Unit,
    private val onErrorRestart: () -> Unit,
    private val isModeActive: () -> Boolean,
    private val onPartialSpeechResults: ((String) -> Boolean)? = null
) {
    companion object {
        private const val TAG = "DualSpeechManager"
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val voskManager = VoskSpeechManager(
        context = context,
        onReady = onReady,
        onSpeechResults = onSpeechResults,
        onErrorRestart = onErrorRestart,
        isModeActive = isModeActive,
        onPartialSpeechResults = onPartialSpeechResults
    )

    private val googleManager = SpeechRecognitionManager(
        context = context,
        onReady = onReady,
        onSpeechResults = onSpeechResults,
        onErrorRestart = onErrorRestart,
        onErrorStopWaiting = { },
        isModeActive = isModeActive,
        onPartialSpeechResults = onPartialSpeechResults
    )

    init {
        // Pre-inicializamos el modelo Vosk en segundo plano para que esté listo al instante
        try {
            voskManager.initModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-inicializando Vosk: ${e.message}")
        }
    }

    val isListening: Boolean
        get() = voskManager.isListening || googleManager.isListening

    fun startListening() {
        val engine = repository.getSpeechEngine()
        Log.i(TAG, "Iniciando escucha con motor seleccionado: $engine")

        try {
            voskManager.stopListening()
        } catch (_: Exception) {}
        try {
            googleManager.stopListening()
        } catch (_: Exception) {}

        mainHandler.postDelayed({
            try {
                if (engine == "GOOGLE") {
                    googleManager.startListening()
                } else {
                    voskManager.startListening()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando motor $engine: ${e.message}")
            }
        }, 150L)
    }

    fun stopListening() {
        try { voskManager.stopListening() } catch (_: Exception) {}
        try { googleManager.stopListening() } catch (_: Exception) {}
    }

    fun destroy() {
        try { voskManager.destroy() } catch (_: Exception) {}
        try { googleManager.destroy() } catch (_: Exception) {}
    }
}
