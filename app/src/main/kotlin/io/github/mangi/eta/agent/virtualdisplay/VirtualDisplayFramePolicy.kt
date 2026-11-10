package io.github.mangi.eta.agent.virtualdisplay

internal object VirtualDisplayFramePolicy {
    const val EXPANDED_INTERVAL_MS = 30L
    const val COLLAPSED_INTERVAL_MS = 160L

    fun decodeIntervalMs(highFrequencyPreview: Boolean): Long =
        if (highFrequencyPreview) EXPANDED_INTERVAL_MS else COLLAPSED_INTERVAL_MS

    fun shouldDecode(
        nowElapsedMs: Long,
        lastDecodedAtElapsedMs: Long,
        intervalMs: Long,
        hasDecodedFrame: Boolean,
    ): Boolean {
        if (!hasDecodedFrame) return true
        return nowElapsedMs - lastDecodedAtElapsedMs >= intervalMs.coerceAtLeast(0L)
    }
}
