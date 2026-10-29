package com.jaylizapp.demonitalk_pro

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

class SpeechRecognitionManager(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onSpeechResults: (String) -> Unit,
    private val onErrorRestart: () -> Unit,
    private val onErrorStopWaiting: () -> Unit,
    private val isModeActive: () -> Boolean,
    private val onPartialSpeechResults: ((String) -> Boolean)? = null
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val originalStreamVolumes = mutableMapOf<Int, Int>()
    private var audioMutedByRecognizer = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val streamsToMute = intArrayOf(
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_DTMF,
        AudioManager.STREAM_ACCESSIBILITY
    )

    var isListening = false
        private set

    fun muteAudio(mute: Boolean) {
        if (!mute) {
            restoreRecognitionAudio(force = false)
            return
        }

        try {
            if (!audioMutedByRecognizer) {
                for (stream in streamsToMute) {
                    try {
                        if (!originalStreamVolumes.containsKey(stream)) {
                            originalStreamVolumes[stream] = audioManager.getStreamVolume(stream)
                        }
                        audioManager.setStreamVolume(stream, 0, 0)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                        }
                    } catch (_: Exception) {}
                }
                audioMutedByRecognizer = true
            }
        } catch (e: Exception) {
            Log.d("SpeechRecManager", "Error silenciando pitido de reconocimiento: ${e.message}")
        }
    }

    private fun restoreRecognitionAudio(force: Boolean) {
        if (!force && isModeActive()) {
            return
        }

        try {
            for (stream in streamsToMute) {
                try {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
                } catch (_: Exception) {}
            }

            val volumesToRestore = originalStreamVolumes.toMap()
            originalStreamVolumes.clear()
            audioMutedByRecognizer = false

            mainHandler.postDelayed({
                val keys = volumesToRestore.keys.toList()
                var idx = 0
                while (idx < keys.size) {
                    val stream = keys[idx]
                    val vol = volumesToRestore[stream]
                    if (vol != null) {
                        try {
                            audioManager.setStreamVolume(stream, vol, 0)
                        } catch (_: Exception) {}
                    }
                    idx++
                }
            }, 500)
        } catch (e: Exception) {
            Log.d("SpeechRecManager", "Error restaurando audio de reconocimiento: ${e.message}")
        }
    }

    private var preferOnDevice = true
    private var pendingRestartRunnable: Runnable? = null

    private fun safeRestart(delayMs: Long = 400L) {
        val r = pendingRestartRunnable
        if (r != null) {
            mainHandler.removeCallbacks(r)
        }
        val runnable = Runnable {
            pendingRestartRunnable = null
            if (isModeActive()) {
                onErrorRestart()
            } else {
                onErrorStopWaiting()
            }
        }
        pendingRestartRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun setupSpeechRecognizer() {
        if (speechRecognizer != null) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechRecManager", "Speech Recognition no disponible en este dispositivo")
            return
        }

        try {
            if (
                preferOnDevice &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            ) {
                try {
                    speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (e: Exception) {
                    Log.w("SpeechRecManager", "OnDeviceSpeechRecognizer falló, usando estándar: ${e.message}")
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            speechRecognizer?.setRecognitionListener(speechListener)
        } catch (e: Exception) {
            Log.e("SpeechRecManager", "Error creando SpeechRecognizer: ${e.message}", e)
            speechRecognizer = null
        }
    }

    private val speechListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onReady()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            muteAudio(true)
        }

        @SuppressLint("SwitchIntDef")
        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Log.d("SpeechRecManager", "Tiempo de silencio o sin coincidencia (Error $error)")
                isListening = false
                if (isModeActive()) {
                    muteAudio(true)
                    safeRestart(300L)
                } else {
                    muteAudio(false)
                    onErrorStopWaiting()
                }
                return
            }

            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                Log.e("SpeechRecManager", "Sin permiso de micrófono RECORD_AUDIO. Deteniendo reinicios.")
                onErrorStopWaiting()
                return
            }

            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                12 -> "Idioma no soportado (Error 12)"
                else -> "Error desconocido: $error"
            }

            Log.e("SpeechRecManager", "Error de reconocimiento [$error]: $message")
            isListening = false
            if (isModeActive()) {
                muteAudio(true)
            } else {
                muteAudio(false)
            }

            if (error == 12) {
                preferOnDevice = false
            }

            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_SERVER ||
                error == 12
            ) {
                resetSpeechRecognizer()
            }

            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                if (ShellUtils.isRootAvailable()) {
                    Thread {
                        try {
                            val pkg = context.packageName
                            val uid = android.os.Process.myUid()
                            val userId = android.os.Process.myUserHandle().hashCode()

                            ShellUtils.executeCommand("cmd appops set --user $userId $pkg RECORD_AUDIO allow")
                            ShellUtils.executeCommand("cmd appops set $uid RECORD_AUDIO allow")
                            ShellUtils.executeCommand("appops set $uid RECORD_AUDIO allow")

                            ShellUtils.executeCommand("cmd appops set --user $userId $pkg PROJECT_MEDIA allow")
                            ShellUtils.executeCommand("cmd appops set $uid PROJECT_MEDIA allow")
                            ShellUtils.executeCommand("appops set $uid PROJECT_MEDIA allow")

                            ShellUtils.executeCommand("pm grant $pkg android.permission.RECORD_AUDIO")
                        } catch (e: Exception) {
                            Log.e("SpeechRecManager", "Error recuperación ROOT: ${e.message}")
                        }
                    }.start()
                }
            }

            if (isModeActive()) {
                safeRestart(500L)
            } else {
                onErrorStopWaiting()
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            if (isModeActive()) {
                muteAudio(true)
            } else {
                muteAudio(false)
            }

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull { it.isNotBlank() }?.trim()
            if (!text.isNullOrEmpty()) {
                Log.i("SpeechRecManager", "🎤 Texto reconocido: '$text'")
                onSpeechResults(text)
            } else {
                Log.w("SpeechRecManager", "⚠️ Se recibió un resultado de voz vacío; no se enviará como comando")
                if (isModeActive()) {
                    safeRestart(300L)
                } else {
                    onErrorStopWaiting()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull { it.isNotBlank() }?.trim()
            if (!text.isNullOrEmpty()) {
                onPartialSpeechResults?.invoke(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (isListening) return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SpeechRecManager", "No hay permiso RECORD_AUDIO concedido.")
            isListening = false
            onErrorStopWaiting()
            return
        }

        try {
            setupSpeechRecognizer()
            if (speechRecognizer == null) return

            try { speechRecognizer?.cancel() } catch (_: Exception) {}

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            muteAudio(true)
            speechRecognizer?.startListening(intent)
            isListening = true

        } catch (e: Exception) {
            Log.e("SpeechRecManager", "Error al iniciar captura de voz: ${e.message}", e)
            isListening = false
            if (isModeActive()) {
                muteAudio(true)
                safeRestart(500L)
            } else {
                muteAudio(false)
            }
        }
    }

    fun stopListening() {
        isListening = false
        val r = pendingRestartRunnable
        if (r != null) {
            mainHandler.removeCallbacks(r)
        }
        pendingRestartRunnable = null
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
        restoreRecognitionAudio(force = true)
    }

    fun resetSpeechRecognizer() {
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
        try { speechRecognizer?.destroy() } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }

    fun destroy() {
        stopListening()
        resetSpeechRecognizer()
        restoreRecognitionAudio(force = true)
        mainHandler.removeCallbacksAndMessages(null)
    }
}
