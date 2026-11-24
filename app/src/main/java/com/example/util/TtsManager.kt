package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class VoiceToneProfile(
    val titleUk: String,
    val pitch: Float,
    val normalRate: Float,
    val slowRate: Float
) {
    SOFT(
        titleUk = "Природний",
        pitch = 1.00f,
        normalRate = 0.94f,
        slowRate = 0.72f
    ),
    WARM(
        titleUk = "Оксамитовий",
        pitch = 0.96f,
        normalRate = 0.92f,
        slowRate = 0.70f
    ),
    CRISP(
        titleUk = "Виразний",
        pitch = 1.04f,
        normalRate = 0.98f,
        slowRate = 0.74f
    )
}

data class GermanVoiceOption(
    val name: String,
    val displayLabel: String,
    val isLocal: Boolean
)

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val prefs = context.applicationContext.getSharedPreferences("deutschlern_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 16
        })
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.applicationContext.cacheDir, "tts_hd").apply { mkdirs() }
    private val cachedAudioKeys = ConcurrentHashMap<String, Boolean>()

    private var tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (e: Throwable) {
        Log.w("TtsManager", "TextToSpeech unavailable on init: ${e.message}")
        null
    }
    private var mediaPlayer: MediaPlayer? = null
    private var speakJob: Job? = null

    private var isInitialized = false
    private var pendingSpeech: Pair<String, Boolean>? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isSlowMode = MutableStateFlow(prefs.getBoolean("tts_slow_mode", false))
    val isSlowMode: StateFlow<Boolean> = _isSlowMode.asStateFlow()

    private val _useHdVoice = MutableStateFlow(prefs.getBoolean("tts_use_hd_voice", true))
    val useHdVoice: StateFlow<Boolean> = _useHdVoice.asStateFlow()

    private val initialTone = VoiceToneProfile.entries.firstOrNull {
        it.name == prefs.getString("tts_voice_tone", VoiceToneProfile.SOFT.name)
    } ?: VoiceToneProfile.SOFT

    private val _voiceTone = MutableStateFlow(initialTone)
    val voiceTone: StateFlow<VoiceToneProfile> = _voiceTone.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<GermanVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<GermanVoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(prefs.getString("tts_selected_voice", null))
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private var germanVoicesRaw: List<Voice> = emptyList()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })

            val result = tts?.setLanguage(Locale.GERMANY)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.GERMAN)
            }

            discoverAndSelectBestGermanVoice()
            isInitialized = true

            pendingSpeech?.let { (text, slow) ->
                pendingSpeech = null
                speak(text, slow)
            }
        } else {
            Log.w("TtsManager", "Offline TextToSpeech initialization failed with status=$status")
        }
    }

    private fun discoverAndSelectBestGermanVoice() {
        try {
            val allVoices = tts?.voices ?: return
            val deVoices = allVoices
                .filter { it.locale?.language.equals("de", ignoreCase = true) }
                .sortedWith(
                    compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                        .thenByDescending { it.locale?.country.equals("DE", ignoreCase = true) }
                        .thenByDescending { scoreVoiceNaturalness(it.name) }
                )

            germanVoicesRaw = deVoices

            val options = deVoices.take(6).mapIndexed { index, voice ->
                val isLocal = !voice.isNetworkConnectionRequired
                val lower = voice.name.lowercase()
                val hint = when {
                    lower.contains("deb") || lower.contains("nfh") || lower.contains("sfb") ||
                        lower.contains("female") -> "Природний"
                    lower.contains("deg") || lower.contains("male") -> "Баритон"
                    else -> "Стандарт"
                }
                val qualityTag = if (isLocal) "Офлайн" else "Мережа"
                GermanVoiceOption(
                    name = voice.name,
                    displayLabel = "Голос ${index + 1} · $hint ($qualityTag)",
                    isLocal = isLocal
                )
            }
            _availableVoices.value = options

            val savedName = _selectedVoiceName.value
            val preferred = deVoices.firstOrNull { it.name == savedName } ?: deVoices.firstOrNull()
            if (preferred != null) {
                tts?.voice = preferred
                _selectedVoiceName.value = preferred.name
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Could not inspect TTS voices: ${e.message}")
        }
    }

    private fun scoreVoiceNaturalness(voiceName: String): Int {
        val lower = voiceName.lowercase()
        return when {
            lower.contains("wavenet") || lower.contains("neural") -> 80
            lower.contains("nfh") -> 55
            lower.contains("deb") -> 50
            lower.contains("sfb") -> 45
            lower.contains("local") -> 30
            else -> 10
        }
    }

    fun selectVoice(voiceName: String) {
        val match = germanVoicesRaw.firstOrNull { it.name == voiceName } ?: return
        try {
            tts?.voice = match
            _selectedVoiceName.value = match.name
            prefs.edit().putString("tts_selected_voice", match.name).apply()
        } catch (e: Exception) {
            Log.w("TtsManager", "Failed to set voice $voiceName: ${e.message}")
        }
    }

    fun setVoiceTone(tone: VoiceToneProfile) {
        _voiceTone.value = tone
        prefs.edit().putString("tts_voice_tone", tone.name).apply()
    }

    fun setHdVoiceEnabled(enabled: Boolean) {
        _useHdVoice.value = enabled
        prefs.edit().putBoolean("tts_use_hd_voice", enabled).apply()
    }

    fun toggleSlowMode() {
        val next = !_isSlowMode.value
        _isSlowMode.value = next
        prefs.edit().putBoolean("tts_slow_mode", next).apply()
    }

    fun setSlowMode(slow: Boolean) {
        _isSlowMode.value = slow
        prefs.edit().putBoolean("tts_slow_mode", slow).apply()
    }

    fun speak(text: String, slow: Boolean = _isSlowMode.value) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return

        stopPlaybackOnly()

        if (_useHdVoice.value) {
            speakJob = scope.launch {
                val cachedFile = resolveOrFetchHdAudio(cleaned)
                if (!isActive) return@launch
                if (cachedFile != null && playAudioFile(cachedFile, slow)) {
                    return@launch
                }
                speakOfflineFallback(cleaned, slow)
            }
        } else {
            speakOfflineFallback(cleaned, slow)
        }
    }

    fun isHdAudioCached(text: String): Boolean {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return true
        val key = sha256(cleaned.lowercase(Locale.GERMAN))
        if (cachedAudioKeys.containsKey(key)) return true
        val targetFile = File(cacheDir, "$key.mp3")
        val exists = targetFile.exists() && targetFile.length() > 512L
        if (exists) cachedAudioKeys[key] = true
        return exists
    }

    suspend fun prefetchHdAudio(text: String): Boolean {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return true
        if (isHdAudioCached(cleaned)) return true
        return resolveOrFetchHdAudio(cleaned) != null
    }

    private suspend fun executeHttpCall(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = httpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) {
                        cont.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private suspend fun resolveOrFetchHdAudio(text: String): File? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val key = sha256(text.lowercase(Locale.GERMAN))
        val targetFile = File(cacheDir, "$key.mp3")
        if (targetFile.exists() && targetFile.length() > 512L) {
            cachedAudioKeys[key] = true
            return@withContext targetFile
        }

        val tempFile = File(cacheDir, "$key.${Thread.currentThread().id}.tmp")
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=de&client=tw-ob&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .build()

            executeHttpCall(req).use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                if (bytes.size < 512) return@withContext null
                tempFile.writeBytes(bytes)
                if (targetFile.exists()) targetFile.delete()
                val saved = if (tempFile.renameTo(targetFile)) targetFile else tempFile
                cachedAudioKeys[key] = true
                saved
            }
        } catch (ce: CancellationException) {
            tempFile.delete()
            throw ce
        } catch (e: Exception) {
            tempFile.delete()
            Log.d("TtsManager", "HD stream unavailable, falling back to offline TTS: ${e.message}")
            null
        }
    }

    private fun playAudioFile(file: File, slow: Boolean): Boolean {
        return try {
            val mp = MediaPlayer()
            mediaPlayer = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val speed = if (slow) 0.78f else 1.0f
                try {
                    mp.playbackParams = PlaybackParams().setSpeed(speed)
                } catch (_: Exception) {
                    // Some emulators don't support PlaybackParams pitch/speed adjustment gracefully
                }
            }
            mp.setOnCompletionListener { player ->
                _isSpeaking.value = false
                if (mediaPlayer === player) mediaPlayer = null
                try {
                    player.release()
                } catch (_: Exception) {
                }
            }
            mp.setOnErrorListener { player, _, _ ->
                _isSpeaking.value = false
                if (mediaPlayer === player) mediaPlayer = null
                try {
                    player.release()
                } catch (_: Exception) {
                }
                true
            }
            _isSpeaking.value = true
            mp.start()
            true
        } catch (e: Exception) {
            Log.w("TtsManager", "MediaPlayer failed on cached audio: ${e.message}")
            releaseMediaPlayer()
            false
        }
    }

    private fun speakOfflineFallback(text: String, slow: Boolean) {
        if (!isInitialized) {
            pendingSpeech = text to slow
            return
        }
        val tone = _voiceTone.value
        val rate = if (slow) tone.slowRate else tone.normalRate
        tts?.setSpeechRate(rate)
        tts?.setPitch(tone.pitch)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_de_${System.currentTimeMillis()}")
    }

    private fun stopPlaybackOnly() {
        speakJob?.cancel()
        speakJob = null
        releaseMediaPlayer()
        tts?.stop()
        _isSpeaking.value = false
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: Exception) {
            }
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
    }

    fun stop() {
        stopPlaybackOnly()
    }

    fun shutdown() {
        stopPlaybackOnly()
        scope.cancel()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}
