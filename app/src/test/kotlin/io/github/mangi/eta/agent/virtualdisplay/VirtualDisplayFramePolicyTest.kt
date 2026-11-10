package io.github.mangi.eta.agent.virtualdisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualDisplayFramePolicyTest {
    @Test
    fun firstFrameAlwaysDecodes() {
        assertTrue(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 0L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = VirtualDisplayFramePolicy.COLLAPSED_INTERVAL_MS,
                hasDecodedFrame = false,
            ),
        )
    }

    @Test
    fun collapsedCadenceRejectsFramesBeforeInterval() {
        assertFalse(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 159L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = VirtualDisplayFramePolicy.COLLAPSED_INTERVAL_MS,
                hasDecodedFrame = true,
            ),
        )
        assertTrue(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 160L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = VirtualDisplayFramePolicy.COLLAPSED_INTERVAL_MS,
                hasDecodedFrame = true,
            ),
        )
    }

    @Test
    fun expandedCadenceUsesThirtyMillis() {
        assertFalse(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 29L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = VirtualDisplayFramePolicy.EXPANDED_INTERVAL_MS,
                hasDecodedFrame = true,
            ),
        )
        assertTrue(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 30L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = VirtualDisplayFramePolicy.EXPANDED_INTERVAL_MS,
                hasDecodedFrame = true,
            ),
        )
    }

    @Test
    fun collapsedStreamDecodesSevenTimesPerSecondInsteadOfEveryFrame() {
        var now = 0L
        var lastDecodedAt = 0L
        var hasDecodedFrame = false
        var decodes = 0
        repeat(61) {
            if (
                VirtualDisplayFramePolicy.shouldDecode(
                    nowElapsedMs = now,
                    lastDecodedAtElapsedMs = lastDecodedAt,
                    intervalMs = VirtualDisplayFramePolicy.COLLAPSED_INTERVAL_MS,
                    hasDecodedFrame = hasDecodedFrame,
                )
            ) {
                decodes++
                lastDecodedAt = now
                hasDecodedFrame = true
            }
            now += 16L
        }
        assertEquals(7, decodes)
    }

    @Test
    fun intervalSelectionFollowsPreviewMode() {
        assertEquals(
            VirtualDisplayFramePolicy.COLLAPSED_INTERVAL_MS,
            VirtualDisplayFramePolicy.decodeIntervalMs(highFrequencyPreview = false),
        )
        assertEquals(
            VirtualDisplayFramePolicy.EXPANDED_INTERVAL_MS,
            VirtualDisplayFramePolicy.decodeIntervalMs(highFrequencyPreview = true),
        )
    }

    @Test
    fun nonPositiveIntervalDecodesEveryFrame() {
        assertTrue(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 0L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = 0L,
                hasDecodedFrame = true,
            ),
        )
        assertTrue(
            VirtualDisplayFramePolicy.shouldDecode(
                nowElapsedMs = 0L,
                lastDecodedAtElapsedMs = 0L,
                intervalMs = -1L,
                hasDecodedFrame = true,
            ),
        )
    }
}
