package app.expressiveclock.alarm

/** Lifecycle independent of MediaPlayer so late callbacks and focus races are testable. */
internal class SoundPlayback(private val backend: Backend) {
    interface Player {
        fun prepare(ready: () -> Unit, failed: () -> Unit)
        fun start()
        fun pause()
        fun release()
        /** Linear gain, 0..1. Optional so test doubles need not care. */
        fun setVolume(volume: Float) {}
    }
    interface Backend {
        fun requestFocus(changed: (Boolean) -> Unit): Boolean
        fun abandonFocus()
        fun create(source: String): Player
        fun exhausted()
    }
    private var generation = 0
    private var focusGeneration = 0
    private var paused = false
    private var player: Player? = null
    private var ready = false
    private var focused = false
    private var active = false
    private var volume = 1f

    /** Applies to the current player and to every fallback created after it. */
    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        player?.setVolume(volume)
    }

    fun play(sources: List<String>) {
        stop()
        active = true
        val token = generation
        requestFocus(token)
        fun attempt(index: Int) {
            if (token != generation || !active) return
            player?.release()
            player = null
            ready = false
            if (index == sources.size) { active = false; backend.abandonFocus(); backend.exhausted(); return }
            var candidate: Player? = null
            try {
                candidate = backend.create(sources[index])
                player = candidate
                candidate.setVolume(volume)
                val current = candidate
                current.prepare(ready = {
                    if (token == generation && player === current) {
                        ready = true
                        if (focused && !paused) current.start()
                    }
                }, failed = {
                    if (token == generation && player === current) attempt(index + 1)
                })
            } catch (_: Exception) {
                if (candidate != null && player !== candidate) candidate.release()
                attempt(index + 1)
            }
        }
        attempt(0)
    }

    private fun requestFocus(token: Int) {
        val focusToken = ++focusGeneration
        focused = backend.requestFocus { gained ->
            if (token == generation && focusToken == focusGeneration && active && !paused) {
                focused = gained
                player?.let { if (gained && ready) it.start() else if (ready) it.pause() }
            }
        }
    }

    /** Preview-only pause retains the prepared player and its position, but releases audio focus. */
    fun pause() {
        if (!active || paused) return
        paused = true
        focusGeneration++
        if (ready) player?.pause()
        focused = false
        backend.abandonFocus()
    }

    fun resume() {
        if (!active || !paused) return
        paused = false
        requestFocus(generation)
        if (focused && ready) player?.start()
    }

    fun stop() {
        generation++
        focusGeneration++
        paused = false
        active = false
        player?.release()
        player = null
        ready = false
        focused = false
        backend.abandonFocus()
    }
}
