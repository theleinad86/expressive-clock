package app.expressiveclock.ui.screens

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.expressiveclock.core.WorldCity
import app.expressiveclock.ui.rememberSystem24HourFormat
import app.expressiveclock.ui.MechanicalText
import app.expressiveclock.ui.WorldClockDigitalReadout
import app.expressiveclock.ui.theme.WorldClockType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

internal val ClockHandMotionSaver = androidx.compose.runtime.saveable.listSaver<ClockHandMotion, Any>(
    save = { listOf(it.zone.id, it.startedAt, it.durationMillis, it.hourCorrection, it.minuteCorrection) },
    restore = { ClockHandMotion(ZoneId.of(it[0] as String), it[1] as Long, it[2] as Long, it[3] as Float, it[4] as Float) },
)
internal val LocalClockHandMotion = staticCompositionLocalOf<MutableState<ClockHandMotion>> {
    error("Clock hand presentation must be provided by ClockApp")
}

/** Signed shortest path, including fractional-hour and DST offsets. */
internal fun shortestClockDelta(from: Float, to: Float): Float = ((from - to + 540f) % 360f + 360f) % 360f - 180f

internal fun clockAngles(millis: Long, zone: ZoneId): Pair<Float, Float> {
    val t = Instant.ofEpochMilli(millis).atZone(zone)
    val seconds = t.second + t.nano / 1_000_000_000f
    return ((t.hour % 12) * 30f + t.minute * .5f + seconds / 120f) to
        (t.minute * 6f + seconds / 10f)
}

/** Reserve real scaled line heights; keep the original 100dp at normal settings. */
@Composable
internal fun worldClockDetailsHeight(city: WorldCity, now: Long, width: Dp): Dp {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val description = city.timeAt(now).format(DateTimeFormatter.ofPattern("zzzz · EEE, d MMM", locale))
    val subtitle = measurer.measure(description, MaterialTheme.typography.bodySmall, maxLines = 2,
        constraints = Constraints(maxWidth = with(density) { width.roundToPx().coerceAtLeast(1) }))
    return maxOf(100.dp, with(density) { WorldClockType.city(24.sp).lineHeight.toDp() + subtitle.size.height.toDp() } + 56.dp)
}

@Composable
internal fun WorldClockHeader(city: WorldCity, now: Long, dialSize: Dp, collapse: Float, detailsHeight: Dp = 100.dp) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val local = city.timeAt(now)
    val use24h = rememberSystem24HourFormat()
    val primary = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurface
    val description = local.format(DateTimeFormatter.ofPattern("zzzz · EEE, d MMM", locale))
    val visibility = worldClockHeaderVisibility(collapse)
    val digitalAlpha = visibility.digital
    Box(Modifier.fillMaxWidth().height(detailsHeight + dialSize * (1f - collapse))) {
        Column(Modifier.fillMaxWidth().graphicsLayer { alpha = visibility.analogue }
                .then(if (visibility.analogue <= 0f) Modifier.clearAndSetSemantics {} else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally) {
                AnalogueWorldClock(city.zone, now, Modifier.size(dialSize * (1f - collapse)), active = visibility.analogue > 0f)
                MechanicalText(city.city, style = WorldClockType.city(24.sp), color = foreground,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    selectionKey = city.zone.id, active = visibility.analogue > 0f, progressiveBlur = true)

        }
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 8.dp + dialSize * .5f * (1f - collapse))
            .graphicsLayer { alpha = digitalAlpha }
            .then(if (digitalAlpha < .5f) Modifier.clearAndSetSemantics {} else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints {
                val size = minOf(52f, maxWidth.value / if (use24h) 5.2f else 6.6f).sp
                val hours = local.format(DateTimeFormatter.ofPattern(if (use24h) "HH" else "hh"))
                val remainder = local.format(DateTimeFormatter.ofPattern(":mm:ss"))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WorldClockDigitalReadout(hours + remainder, city.zone.id,
                        foreground, primary, modifier = Modifier.weight(1f))
                    if (!use24h) Text(local.format(DateTimeFormatter.ofPattern("a", locale)),
                        style = WorldClockType.secondary(14.sp), color = foreground)
                }

            }
        }
        WorldClockSubtitle(description, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}

/** Simultaneous opacity transitions keep a timezone line visible throughout selection changes. */
@Composable
internal fun WorldClockSubtitle(description: String, modifier: Modifier = Modifier) {
    Crossfade(
        targetState = description,
        modifier = modifier.fillMaxWidth(),
        animationSpec = if (ValueAnimator.areAnimatorsEnabled()) tween(240, easing = LinearEasing) else snap(),
        label = "worldTimezoneDissolve",
    ) { line ->
        Text(line, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            maxLines = 2, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun AnalogueWorldClock(zone: ZoneId, now: Long, modifier: Modifier, active: Boolean) {
    var motion by LocalClockHandMotion.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = LocalLifecycleOwner.current
    // Only the draw phase observes this frame state. City rows and text remain on a 1Hz ticker.
    val frameTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(owner, active) {
        if (active) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                if (ValueAnimator.areAnimatorsEnabled()) withFrameNanos { frameTime.longValue = System.currentTimeMillis() }
                else { frameTime.longValue = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) }
            }
        }
    }
    LaunchedEffect(zone) {
        if (motion.zone != zone) {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            // Sample the exact last displayed positions and publish the new target/corrections
            // together. Canvas never sees a new zone with the previous zone's corrections.
            motion = motion.retarget(zone, frameTime.longValue,
                if (ValueAnimator.areAnimatorsEnabled()) (680 * scale).toLong() else 0L)
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val face = if (dark) MaterialTheme.colorScheme.surfaceContainerLow
        else lerp(MaterialTheme.colorScheme.surface, Color.White, .72f)
    val measurer = rememberTextMeasurer()
    val numeralStyle = MaterialTheme.typography.titleMedium.copy(color = ink, fontWeight = FontWeight.Medium)
    val labels = remember(measurer, numeralStyle) { (1..12).map { measurer.measure(it.toString(), numeralStyle) } }
    val semanticTime = Instant.ofEpochMilli(now).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    Canvas(modifier.clockLighting(accent, dark).semantics { contentDescription = "Analogue clock, $semanticTime" }) {
        val r = size.minDimension / 2f - 4.dp.toPx()
        if (r <= 0f) return@Canvas
        fun point(degrees: Float, radius: Float): Offset {
            val a = (degrees - 90f) * PI.toFloat() / 180f
            return center + Offset(cos(a), sin(a)) * radius
        }
        drawCircle(face, r)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = if (dark) .015f else .22f), Color.Transparent),
            center - Offset(r * .30f, r * .45f), r * 1.5f), r)
        drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .12f else .95f),
            accent.copy(alpha = if (dark) .08f else .09f), Color.White.copy(alpha = if (dark) .04f else .65f))),
            r, style = Stroke(1.dp.toPx()))
        repeat(60) { i ->
            drawLine(if (i % 5 == 0) ink else muted.copy(alpha = .3f),
                point(i * 6f, r * .94f), point(i * 6f, r * if (i % 5 == 0) .88f else .91f),
                strokeWidth = (if (i % 5 == 0) 2f else 1f).dp.toPx(), cap = StrokeCap.Round)
        }
        labels.forEachIndexed { i, label ->
            val p = point((i + 1) * 30f, r * .76f)
            // Dial numerals are part of the face artwork: constrain their ink to the
            // dial radius when display/font enlargement leaves a smaller clock.
            val labelScale = minOf(1f, r * .18f / label.size.height.coerceAtLeast(1))
            scale(labelScale, pivot = p) {
                drawText(label, topLeft = p - Offset(label.size.width / 2f, label.size.height / 2f))
            }
        }
        val instant = frameTime.longValue
        val angles = motion.anglesAt(instant)
        drawLine(ink, point(angles.first + 180f, r * .08f),
            point(angles.first, r * .47f), 6.dp.toPx(), StrokeCap.Round)
        drawLine(ink, point(angles.second + 180f, r * .09f),
            point(angles.second, r * .68f), 4.dp.toPx(), StrokeCap.Round)
        val t = Instant.ofEpochMilli(instant).atZone(zone)
        val secondAngle = (t.second + t.nano / 1_000_000_000f) * 6f
        val end = point(secondAngle, r * .84f)
        val start = point(secondAngle + 180f, r * .14f)
        for (width in listOf(9f, 5f)) drawLine(accent.copy(alpha = .045f), start, end, width.dp.toPx(), StrokeCap.Round)
        drawLine(accent, start, end, 1.5.dp.toPx(), StrokeCap.Round)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .35f), Color.Transparent), center, 15.dp.toPx()), 15.dp.toPx())
        drawCircle(accent, 5.dp.toPx())
        drawCircle(ink, 2.dp.toPx())
    }
}
