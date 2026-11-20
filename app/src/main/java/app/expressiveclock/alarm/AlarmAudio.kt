package app.expressiveclock.alarm

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.expressiveclock.R
import kotlinx.coroutines.*

/** Shared by preview and foreground ringing; never changes alarm volume or DND. */
internal class AlarmAudio(private val context: Context, private val looping: Boolean = true) {
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val audio = context.getSystemService(AudioManager::class.java)
    private var focus: AudioFocusRequest? = null
    var onFinished: (() -> Unit)? = null
    var onStarted: ((String) -> Unit)? = null
    var onFallback: (() -> Unit)? = null
    private val playback: SoundPlayback = SoundPlayback(object : SoundPlayback.Backend {
        override fun requestFocus(changed: (Boolean) -> Unit): Boolean {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener({ change ->
                    changed(change == AudioManager.AUDIOFOCUS_GAIN)
                }, Handler(Looper.getMainLooper())).build()
            focus = request
            val result = audio.requestAudioFocus(request)
            Log.i("AlarmAudio", "focus=$result")
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        override fun abandonFocus() { focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null }
        override fun create(source: String): SoundPlayback.Player {
            val media = MediaPlayer()
            media.setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
            media.setAudioAttributes(attributes)
            media.isLooping = looping
            val handler = Handler(Looper.getMainLooper())
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            var onFailure: (() -> Unit)? = null
            var timeout: Runnable? = null
            return object : SoundPlayback.Player {
                override fun prepare(ready: () -> Unit, failed: () -> Unit) {
                    onFailure = failed
                    timeout = Runnable { onFallback?.invoke(); failed() }
                    handler.postDelayed(timeout!!, 5_000)
                    media.setOnPreparedListener { timeout?.let(handler::removeCallbacks); ready() }
                    media.setOnErrorListener { _, _, _ -> onFallback?.invoke(); failed(); true }
                    if (!looping) media.setOnCompletionListener { stop(); onFinished?.invoke() }
                    scope.launch {
                        var descriptor: android.content.res.AssetFileDescriptor? = null
                        try {
                            // Providers (including cloud documents) must never block the UI/ringer thread.
                            withContext(Dispatchers.IO) {
                                descriptor = context.contentResolver.openAssetFileDescriptor(Uri.parse(source), "r")
                            }
                            val fd = checkNotNull(descriptor) { "Audio provider returned no descriptor" }
                            if (fd.declaredLength < 0) media.setDataSource(fd.fileDescriptor)
                            else media.setDataSource(fd.fileDescriptor, fd.startOffset, fd.declaredLength)
                            media.prepareAsync()
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) {
                            Log.w("AlarmAudio", "Audio source unavailable; trying fallback", error)
                            onFallback?.invoke()
                            failed()
                        } finally { runCatching { descriptor?.close() } }
                    }
                }
                override fun start() { runCatching { media.start(); Log.i("AlarmAudio", "Playback started on alarm stream"); onStarted?.invoke(source) }.onFailure { onFallback?.invoke(); onFailure?.invoke() } }
                override fun pause() { runCatching { media.pause() } }
                override fun setVolume(volume: Float) { runCatching { media.setVolume(volume, volume) } }
                override fun release() { timeout?.let(handler::removeCallbacks); scope.cancel(); media.release() }
            }
        }
        override fun exhausted() { Log.e("AlarmAudio", "No playable alarm source"); onFallback?.invoke() }
    })
    /**
     * `null` means "default alarm sound" (that is what the sound page stores for it). [SILENT][app.expressiveclock.data.SILENT]
     * and anything blank mean no sound at all: no player is created, and the fallback chain below
     * must never turn an explicit silence into the default ringtone.
     */
    fun play(uri: String?) {
        if (isSilentSound(uri)) { stop(); return }
        playback.play(listOfNotNull(uri,
            runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)?.toString() }.getOrNull(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString(),
            "android.resource://${context.packageName}/${R.raw.alarm_fallback}").distinct())
    }
    fun setVolume(volume: Float): Unit { playback.setVolume(volume) }
    fun pause(): Unit { playback.pause() }
    fun resume(): Unit { playback.resume() }
    fun stop(): Unit { playback.stop() }
}

/** True for an explicit "Silent" choice or an empty/blank URI; such a source must never start a player. */
internal fun isSilentSound(uri: String?): Boolean =
    uri != null && (uri == app.expressiveclock.data.SILENT || uri.isBlank() || uri.equals("null", ignoreCase = true))
