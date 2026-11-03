package com.drowz.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp as lerpColor
import com.drowz.app.ui.theme.DrowzColors.Sky
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Drowz celestial scene: a dusk-to-deep-night sky that darkens as [progress] goes
 * 0 → 1 — the same visual language as the landing page.
 *
 * The scene's geometry (stars, ridges, skyline) is generated once into a normalized
 * [SkyModel] and only *re-colored/re-positioned* per frame, so animation stays cheap.
 *
 * Star/window twinkle is driven by a **monotonic frame clock** (accumulated seconds), so it
 * never wraps — unlike a looping 0→2π transition, which popped every cycle. The clock only
 * ticks once [progress] is past [STAR_ONSET] (below which no star or window is visible), so
 * an idle Home screen — a bedside sleep app may sit here a long time — does zero continuous
 * redraw work. Everything else is a pure function of [progress].
 *
 * @param progress 0 = dusk (warm horizon, sun up), 1 = deep night (moon high, city dark).
 * @param seed fixes the skyline so it's stable across recompositions and config changes.
 */
@Composable
fun NightSky(
    progress: Float,
    modifier: Modifier = Modifier,
    seed: Long = 7L,
) {
    val model = remember(seed) { SkyModel.generate(Random(seed)) }
    val p = progress.coerceIn(0f, 1f)

    // Only run the frame loop while shimmer is actually visible. Accumulating elapsed time
    // (rather than resetting) means pausing/resuming never jumps the twinkle phase.
    val animate = p > STAR_ONSET
    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            timeSec += (now - last) / 1_000_000_000f
            last = now
        }
    }

    Canvas(modifier) { drawScene(model, p, timeSec) }
}

// --- scene model (normalized 0..1 coordinates; size-independent) ---

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)

private class Win(
    val dx: Float, val dy: Float, val on: Boolean,
    val phase: Float, val offAt: Float, var owl: Boolean,
)

private class Building(val x: Float, val w: Float, val h: Float, val windows: List<Win>)

private class SkyModel(
    val stars: List<Star>,
    val far: List<Offset>,
    val near: List<Offset>,
    val buildings: List<Building>,
) {
    companion object {
        private const val HORIZON = 0.72f

        fun generate(r: Random): SkyModel {
            val stars = List(90) {
                Star(
                    x = r.nextFloat(),
                    y = r.nextFloat() * HORIZON * 0.92f,
                    r = (0.4f + r.nextFloat() * 1.1f) * 0.0016f,
                    phase = r.nextFloat() * 6.28f,
                    speed = 0.5f + r.nextFloat(),
                )
            }

            val far = ridge(r, base = HORIZON + 0.004f, amp = 0.13f, step = 1f / 7f)
            val near = ridge(r, base = HORIZON + 0.035f, amp = 0.21f, step = 1f / 5f)

            val buildings = ArrayList<Building>()
            var x = 0f
            while (x < 1f) {
                val w = 0.045f + r.nextFloat() * 0.075f
                val h = 0.10f + r.nextFloat() * 0.28f
                val cols = (w / 0.022f).toInt().coerceAtLeast(1)
                val rows = (h / 0.05f).toInt().coerceAtLeast(1)
                val windows = ArrayList<Win>(cols * rows)
                for (row in 0 until rows) {
                    for (c in 0 until cols) {
                        windows += Win(
                            dx = 0.012f + c * 0.022f,
                            dy = 0.02f + row * 0.05f,
                            on = r.nextFloat() < 0.55f,
                            phase = r.nextFloat() * 6.28f,
                            offAt = 0.52f + r.nextFloat() * 0.40f,
                            owl = false,
                        )
                    }
                }
                buildings += Building(x, w, h, windows)
                x += w + 0.004f + r.nextFloat() * 0.012f
            }

            // Keep two windows burning all night — the last souls awake.
            val lit = buildings.flatMap { it.windows }.filter { it.on }
            if (lit.isNotEmpty()) {
                repeat(2) { lit[r.nextInt(lit.size)].owl = true }
            }

            return SkyModel(stars, far, near, buildings)
        }

        private fun ridge(r: Random, base: Float, amp: Float, step: Float): List<Offset> {
            val pts = ArrayList<Offset>()
            var x = -step
            while (x <= 1f + step) {
                pts += Offset(x, base - r.nextFloat() * amp)
                x += step
            }
            return pts
        }
    }
}

// --- drawing ---

private const val HORIZON = 0.72f

/** Below this progress, no star or window is visible — so the twinkle clock can stay idle. */
private const val STAR_ONSET = 0.10f

/** Angular twinkle rate (rad/s): 2π / 11s — preserves the original shimmer cadence. */
private const val TWINKLE_RATE = 0.5712f

private fun DrawScope.drawScene(model: SkyModel, p: Float, time: Float) {
    val horizonPx = size.height * HORIZON

    drawSky(p)
    drawAfterglow(p, horizonPx)
    drawStars(model, p, time)
    drawSun(p, horizonPx)
    drawMoon(p, horizonPx)
    drawRidge(model.far, offset = p * 0.07f, color = lerpColor(Sky.MountainFar, Sky.MountainFarNight, p))
    drawRidge(model.near, offset = p * 0.14f, color = lerpColor(Sky.MountainNear, Sky.MountainNearNight, p))
    drawCity(model, p, time, horizonPx)
}

private fun DrawScope.drawSky(p: Float) {
    val brush = Brush.verticalGradient(
        0.00f to lerpColor(Sky.DuskTop, Sky.NightTop, p),
        0.48f to lerpColor(Sky.DuskMid, Sky.NightMid, p),
        HORIZON to lerpColor(Sky.DuskHorizon, Sky.NightHorizon, p),
        1.00f to lerpColor(Sky.DuskLow, Sky.NightLow, p),
    )
    drawRect(brush = brush, size = size)
}

private fun DrawScope.drawAfterglow(p: Float, horizonPx: Float) {
    val alpha = (1f - p) * 0.55f
    if (alpha <= 0.02f) return
    val center = Offset(size.width * 0.30f, horizonPx)
    val radius = size.width * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(1f, 0.59f, 0.31f, alpha), Color(1f, 0.59f, 0.31f, 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawStars(model: SkyModel, p: Float, time: Float) {
    val base = ((p - 0.10f) / 0.55f).coerceIn(0f, 1f)
    if (base <= 0f) return
    for (s in model.stars) {
        val tw = 0.55f + 0.45f * sin(time * TWINKLE_RATE * s.speed + s.phase)
        drawCircle(
            color = Sky.Star,
            radius = s.r * size.width,
            center = Offset(s.x * size.width, s.y * size.height),
            alpha = (base * tw).coerceIn(0f, 1f),
        )
    }
}

private fun DrawScope.drawMoon(p: Float, horizonPx: Float) {
    val mp = ((p - 0.30f) / 0.70f).coerceIn(0f, 1f)
    if (mp <= 0f) return
    val cx = size.width * 0.72f
    val cy = lerp(horizonPx + size.height * 0.08f, horizonPx - size.height * 0.34f, mp)
    val r = size.height * 0.045f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Sky.Moon.copy(alpha = 0.5f * mp), Sky.Moon.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = r * 3.4f,
        ),
        radius = r * 3.4f,
        center = Offset(cx, cy),
    )
    drawCircle(color = Sky.Moon, radius = r, center = Offset(cx, cy), alpha = 0.4f + 0.6f * mp)
}

private fun DrawScope.drawSun(p: Float, horizonPx: Float) {
    val alpha = (1f - p / 0.5f).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return
    val sp = (p / 0.55f).coerceIn(0f, 1f)
    val cx = size.width * 0.30f
    val cy = lerp(horizonPx - size.height * 0.12f, horizonPx + size.height * 0.16f, sp)
    val r = size.height * 0.05f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(1f, 0.74f, 0.43f, 0.75f * alpha), Color(1f, 0.59f, 0.31f, 0f)),
            center = Offset(cx, cy),
            radius = r * 4f,
        ),
        radius = r * 4f,
        center = Offset(cx, cy),
    )
    drawCircle(color = Sky.DuskHorizon, radius = r, center = Offset(cx, cy), alpha = alpha)
}

private fun DrawScope.drawRidge(points: List<Offset>, offset: Float, color: Color) {
    if (points.isEmpty()) return
    val w = size.width
    val h = size.height
    val dy = offset * h
    val path = Path().apply {
        moveTo(points.first().x * w, points.first().y * h + dy)
        for (i in 1 until points.size) lineTo(points[i].x * w, points[i].y * h + dy)
        lineTo(points.last().x * w, h + 2f)
        lineTo(points.first().x * w, h + 2f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCity(model: SkyModel, p: Float, time: Float, horizonPx: Float) {
    val w = size.width
    val h = size.height
    val cityOffset = p * 0.22f * h
    val cityColor = lerpColor(Sky.CityDusk, Sky.CityNight, (p * 0.85f).coerceIn(0f, 1f))
    val litIn = ((p - 0.16f) / 0.26f).coerceIn(0f, 1f)
    val winW = (0.0038f * w).coerceAtLeast(2f)
    val winH = (0.008f * h).coerceAtLeast(3f)

    for (b in model.buildings) {
        val left = b.x * w
        val top = horizonPx - b.h * h + cityOffset
        drawRect(color = cityColor, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(b.w * w, h - top + 4f))

        if (litIn <= 0f) continue
        for (win in b.windows) {
            if (!win.on) continue
            var level = litIn
            if (!win.owl) level *= (1f - ((p - win.offAt) / 0.06f).coerceIn(0f, 1f))
            if (level <= 0.001f) continue
            val flicker = 0.62f + 0.38f * sin(time * TWINKLE_RATE * s(win) + win.phase)
            drawRect(
                color = Sky.Window,
                topLeft = Offset(left + win.dx * w, top + win.dy * h),
                size = androidx.compose.ui.geometry.Size(winW, winH),
                alpha = (level * flicker).coerceIn(0f, 1f),
            )
        }
    }
}

// Per-window animation speed, derived from its phase so windows shimmer out of sync.
private fun s(win: Win): Float = 0.4f + (win.phase % 1f)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
