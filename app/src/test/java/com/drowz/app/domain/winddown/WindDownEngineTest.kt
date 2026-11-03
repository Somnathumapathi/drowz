package com.drowz.app.domain.winddown

import com.drowz.app.domain.model.EffectSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wind-down is a pure function of (elapsed, settings), so we can pin its whole
 * behaviour with plain assertions — no Android, no clock, no mocks.
 */
class WindDownEngineTest {

    private val engine = WindDownEngine()
    private val allOn = EffectSettings(
        dimScreen = true, targetBrightness = 0.1f,
        darkOverlay = true, overlayDarkness = 0.6f,
        doNotDisturb = true, lowerVolume = true, targetVolume = 0.2f,
        soundscape = true, windDownMinutes = 20,
    )

    @Test
    fun `starts essentially untouched at t0`() {
        val s = engine.stateAt(0.0, allOn)
        assertEquals(1f, s.brightness, EPS)      // dim ramp begins at 0
        assertEquals(0f, s.overlay, EPS)         // overlay doesn't start until 60s
        assertEquals(1f, s.volume, EPS)          // volume ramp begins at 30s
        assertEquals(0f, s.soundscape, EPS)
        assertFalse(s.dndActive)
        assertEquals(WindDownPhase.Reminder, s.phase)
        assertEquals(0f, s.progress, EPS)
    }

    @Test
    fun `brightness reaches target at end of dim ramp`() {
        assertEquals(0.1f, engine.stateAt(60.0, allOn).brightness, EPS)
    }

    @Test
    fun `dnd switches on at 30s and holds`() {
        assertFalse(engine.stateAt(29.0, allOn).dndActive)
        assertTrue(engine.stateAt(30.0, allOn).dndActive)
        assertTrue(engine.stateAt(600.0, allOn).dndActive)
    }

    @Test
    fun `volume ramps between 30s and 60s`() {
        assertEquals(1f, engine.stateAt(30.0, allOn).volume, EPS)     // ramp start
        assertEquals(0.2f, engine.stateAt(60.0, allOn).volume, EPS)   // ramp end = target
        val mid = engine.stateAt(45.0, allOn).volume                 // halfway
        assertEquals(0.6f, mid, 0.02f)
    }

    @Test
    fun `soundscape fades in, holds, then fades to silence by session end`() {
        assertTrue(engine.stateAt(60.0, allOn).soundscape > 0.95f)    // faded in
        assertEquals(1f, engine.stateAt(600.0, allOn).soundscape, 0.01f) // mid-session hold
        assertEquals(0f, engine.stateAt(1200.0, allOn).soundscape, EPS)  // silent at end
        assertEquals(WindDownPhase.Asleep, engine.stateAt(1200.0, allOn).phase)
    }

    @Test
    fun `disabled effects emit neutral values`() {
        val off = EffectSettings(
            dimScreen = false, darkOverlay = false, doNotDisturb = false,
            lowerVolume = false, soundscape = false,
        )
        val s = engine.stateAt(120.0, off)
        assertEquals(1f, s.brightness, EPS)
        assertEquals(0f, s.overlay, EPS)
        assertEquals(1f, s.volume, EPS)
        assertEquals(0f, s.soundscape, EPS)
        assertFalse(s.dndActive)
    }

    @Test
    fun `progress reaches full night after the ramp and restore returns to awake`() {
        assertEquals(1f, engine.stateAt(80.0, allOn).progress, EPS)
        assertEquals(WindDownState.Awake, engine.restored())
    }

    private companion object {
        const val EPS = 0.001f
    }
}
