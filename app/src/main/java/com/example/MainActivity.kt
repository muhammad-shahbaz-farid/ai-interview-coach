package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.InterviewRepository
import com.example.ui.InterviewViewModel
import com.example.ui.InterviewViewModelFactory
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningState by mutableStateOf(false)

    private var requestPermissionCallback: (() -> Unit)? = null

    private val viewModel: InterviewViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = InterviewRepository(database)
        InterviewViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Secure TextToSpeech initialization
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout(
                    viewModel = viewModel,
                    speakText = { text -> speak(text) },
                    startListening = { onResult, onError ->
                        startLiveListening(onResult, onError)
                    },
                    stopListening = { stopLiveListening() },
                    isListening = isListeningState
                )
            }
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "InterviewCoachTTS")
        }
    }

    private fun startLiveListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionCallback = { startLiveListening(onResult, onError) }
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            return
        }

        runOnUiThread {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningState = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListeningState = false
                    }
                    override fun onError(error: Int) {
                        isListeningState = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check Mic."
                            SpeechRecognizer.ERROR_CLIENT -> "Client device service error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission denied."
                            SpeechRecognizer.ERROR_NETWORK -> "Network connections failed."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak closer."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Recording timed out. Speak again."
                            else -> "Speech recognition engine is currently busy."
                        }
                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onResult(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                onError("Speech input system is unavailable on this simulator: ${e.localizedMessage}")
            }
        }
    }

    private fun stopLiveListening() {
        runOnUiThread {
            speechRecognizer?.stopListening()
            isListeningState = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionCallback?.invoke()
            }
            requestPermissionCallback = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.let {
            it.stop()
            it.shutdown()
        }
        speechRecognizer?.destroy()
    }
}
