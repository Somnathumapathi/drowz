package com.drowz.app.platform.effects

import android.content.Context
import android.media.AudioManager

/**
 * Lowers media / ring / notification volume — no special permission required. Captures each
 * stream so [restore] is exact, and only ever lowers below the captured level. The alarm
 * stream is deliberately untouched so the morning wake alarm stays audible.
 *
 * `setStreamVolume` can throw while DND is active on some devices, so each write is guarded.
 */
internal class VolumeController(
    private val context: Context,
    private val baseline: BaselineStore,
) {
    private val audio get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val lastWritten = HashMap<Int, Int>()

    val available: Boolean get() = true

    fun capture() {
        for (stream in STREAMS) baseline.setVolume(stream, audio.getStreamVolume(stream))
        lastWritten.clear()
    }

    fun apply(fraction: Float) {
        if (fraction >= 0.999f) return
        for (stream in STREAMS) {
            val max = audio.getStreamMaxVolume(stream)
            val captured = baseline.volume(stream) ?: audio.getStreamVolume(stream)
            val target = minOf((fraction * max).toInt(), captured).coerceAtLeast(0)
            if (lastWritten[stream] == target) continue
            runCatching { audio.setStreamVolume(stream, target, 0) }
            lastWritten[stream] = target
        }
    }

    fun restore() {
        for (stream in STREAMS) {
            baseline.volume(stream)?.let { v ->
                runCatching { audio.setStreamVolume(stream, v, 0) }
            }
        }
        lastWritten.clear()
    }

    private companion object {
        val STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
        )
    }
}
