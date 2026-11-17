package com.kgps.result.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsHelper(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingSpeak: Pair<String, String>? = null

    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val bdResult = engine.setLanguage(Locale("bn", "BD"))
                if (bdResult == TextToSpeech.LANG_MISSING_DATA || bdResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale("bn"))
                }
                engine.setSpeechRate(0.95f)
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _speakingId.value = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_speakingId.value == utteranceId) {
                            _speakingId.value = null
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_speakingId.value == utteranceId) {
                            _speakingId.value = null
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (_speakingId.value == utteranceId) {
                            _speakingId.value = null
                        }
                    }
                })
                ready = true
                pendingSpeak?.let { (txt, id) ->
                    pendingSpeak = null
                    speakInternal(txt, id)
                }
            }
        }
    }

    fun speak(text: String, id: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (_speakingId.value == id) {
            stop()
            return
        }
        if (!ready) {
            pendingSpeak = clean to id
            _speakingId.value = id
            return
        }
        speakInternal(clean, id)
    }

    private fun speakInternal(text: String, id: String) {
        _speakingId.value = id
        val res = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (res != TextToSpeech.SUCCESS) {
            _speakingId.value = null
        }
    }

    fun stop() {
        tts?.stop()
        _speakingId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        _speakingId.value = null
    }
}
