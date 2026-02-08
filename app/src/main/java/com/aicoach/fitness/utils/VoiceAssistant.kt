package com.aicoach.fitness.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceAssistant @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceAssistant"
        private const val UTTERANCE_ID = "ai_coach_utterance"
    }

    sealed class VoiceState {
        data object Idle : VoiceState()
        data object Listening : VoiceState()
        data object Processing : VoiceState()
        data object Speaking : VoiceState()
        data class Error(val message: String) : VoiceState()
    }

    sealed class RecognitionResult {
        data class Success(val text: String) : RecognitionResult()
        data class Partial(val text: String) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
    }

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false

    private var onRecognitionResult: ((RecognitionResult) -> Unit)? = null
    private var onSpeechComplete: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initializeTts()
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                isTtsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isTtsInitialized) {
                    textToSpeech?.setSpeechRate(0.9f)
                    textToSpeech?.setPitch(1.0f)
                    setupUtteranceListener()
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun setupUtteranceListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _voiceState.value = VoiceState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    _voiceState.value = VoiceState.Idle
                    onSpeechComplete?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    _voiceState.value = VoiceState.Error("Speech error")
                }
            }
        })
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized) {
            Log.e(TAG, "TTS not initialized")
            onComplete?.invoke()
            return
        }

        onSpeechComplete = onComplete
        _voiceState.value = VoiceState.Speaking

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    fun speakAndListen(
        prompt: String,
        onResult: (RecognitionResult) -> Unit
    ) {
        speak(prompt) {
            startListening(onResult)
        }
    }

    fun startListening(onResult: (RecognitionResult) -> Unit) {
        onRecognitionResult = onResult

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Prefer offline recognition
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            }

            _voiceState.value = VoiceState.Listening
            speechRecognizer?.startListening(intent)
        } else {
            onResult(RecognitionResult.Error("Speech recognition not available"))
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.Processing
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                Log.e(TAG, "Recognition error: $errorMessage")
                _voiceState.value = VoiceState.Error(errorMessage)
                onRecognitionResult?.invoke(RecognitionResult.Error(errorMessage))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()

                if (!text.isNullOrBlank()) {
                    _voiceState.value = VoiceState.Idle
                    onRecognitionResult?.invoke(RecognitionResult.Success(text))
                } else {
                    onRecognitionResult?.invoke(RecognitionResult.Error("No speech recognized"))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    onRecognitionResult?.invoke(RecognitionResult.Partial(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _voiceState.value = VoiceState.Idle
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _voiceState.value = VoiceState.Idle
    }

    fun shutdown() {
        stopListening()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    fun isTtsReady(): Boolean = isTtsInitialized

    // Voice command detection
    fun detectCommand(text: String): VoiceCommand {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("complete") ||
            lowerText.contains("done") ||
            lowerText.contains("finished") -> VoiceCommand.COMPLETE_TASK

            lowerText.contains("start") ||
            lowerText.contains("begin") -> VoiceCommand.START_WORKOUT

            lowerText.contains("stop") ||
            lowerText.contains("pause") -> VoiceCommand.STOP_WORKOUT

            lowerText.contains("skip") -> VoiceCommand.SKIP_EXERCISE

            lowerText.contains("repeat") ||
            lowerText.contains("again") -> VoiceCommand.REPEAT_INSTRUCTION

            lowerText.contains("help") -> VoiceCommand.HELP

            else -> VoiceCommand.UNKNOWN
        }
    }

    enum class VoiceCommand {
        COMPLETE_TASK,
        START_WORKOUT,
        STOP_WORKOUT,
        SKIP_EXERCISE,
        REPEAT_INSTRUCTION,
        HELP,
        UNKNOWN
    }
}
