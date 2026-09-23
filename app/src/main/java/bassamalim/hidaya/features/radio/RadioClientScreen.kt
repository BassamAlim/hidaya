package bassamalim.hidaya.features.radio

import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MultiDrawableImage
import bassamalim.hidaya.core.ui.components.MyCircularProgressIndicator
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

private val OrbitMaxSize = 280.dp
private val PlayButtonSize = 100.dp
private val WavesHeight = 80.dp
private val StatusDotSize = 8.dp

// Dot positions expressed as (angle°, distance-ratio of ring radius)
private val DOT_POSITIONS = listOf(
    18f to 0.74f,  145f to 0.88f,  220f to 0.76f,  310f to 0.91f,
    72f to 0.95f,  168f to 0.80f,  258f to 0.70f,  340f to 0.86f
)
private val DOT_RADII_DP = listOf(1.8f, 1.2f, 1.5f, 1.0f, 1.8f, 1.3f, 1.0f, 1.5f)

private enum class RadioStatus { LIVE, CONNECTING, PAUSED, ERROR }

private fun statusOf(playbackState: Int) = when (playbackState) {
    PlaybackStateCompat.STATE_PLAYING -> RadioStatus.LIVE
    PlaybackStateCompat.STATE_ERROR -> RadioStatus.ERROR
    PlaybackStateCompat.STATE_STOPPED,
    PlaybackStateCompat.STATE_PAUSED -> RadioStatus.PAUSED
    else -> RadioStatus.CONNECTING
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RadioClientScreen(viewModel: RadioClientViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(activity)
        onDispose { viewModel.onStop(activity) }
    }

    val dims = MaterialTheme.dimensions
    val status = statusOf(state.btnState)
    val clock = rememberPlaybackClock(isRunning = status == RadioStatus.LIVE)

    MyScaffold(title = stringResource(R.string.quran_radio)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = dims.spaceXxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.holy_quran_radio),
                modifier = Modifier.padding(horizontal = dims.spaceXxl),
                style = MaterialTheme.appTypography.display,
                textAlign = TextAlign.Center
            )

            StatusLine(
                status = status,
                modifier = Modifier.padding(
                    top = dims.spaceSm,
                    start = dims.spaceXxl,
                    end = dims.spaceXxl
                )
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OrbitalButton(
                    status = status,
                    playbackState = state.btnState,
                    clock = { clock.longValue },
                    modifier = Modifier.size(min(min(maxWidth, maxHeight), OrbitMaxSize)),
                    onClick = viewModel::onPlayPauseClick
                )
            }

            Waves(
                color = MaterialTheme.colorScheme.primary,
                clock = { clock.longValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WavesHeight)
                    // Waves only fully show while the stream is live
                    .alpha(if (status == RadioStatus.LIVE) 1f else 0.4f)
            )
        }
    }
}

/**
 * Milliseconds of playback so far. Only advances while [isRunning], so everything drawn from it
 * freezes in place when the radio stops instead of jumping back to the start.
 */
@Composable
private fun rememberPlaybackClock(isRunning: Boolean): MutableLongState {
    val elapsed = remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                elapsed.longValue += now - last
                last = now
            }
        }
    }
    return elapsed
}

/** Fraction (0..1) of the way through a cycle of [periodMillis] at time [millis]. */
private fun cycle(millis: Long, periodMillis: Int) =
    (millis % periodMillis).toFloat() / periodMillis

@Composable
private fun StatusLine(status: RadioStatus, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions
    val color = when (status) {
        RadioStatus.LIVE -> MaterialTheme.colorScheme.error
        RadioStatus.ERROR -> MaterialTheme.colorScheme.error
        RadioStatus.CONNECTING -> MaterialTheme.colorScheme.primary
        RadioStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (status != RadioStatus.ERROR) {
            // The classic "on air" dot: pulses while live, steady otherwise
            val pulse = if (status == RadioStatus.LIVE) {
                val transition = rememberInfiniteTransition(label = "live dot")
                val alpha by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "live dot alpha"
                )
                alpha
            } else 1f

            Box(
                Modifier
                    .size(StatusDotSize)
                    .alpha(pulse)
                    .background(color = color, shape = CircleShape)
            )

            Spacer(Modifier.width(dims.spaceSm))
        }

        Text(
            text = stringResource(
                when (status) {
                    RadioStatus.LIVE -> R.string.radio_live
                    RadioStatus.CONNECTING -> R.string.radio_connecting
                    RadioStatus.PAUSED -> R.string.radio_paused
                    RadioStatus.ERROR -> R.string.radio_error
                }
            ),
            style = MaterialTheme.appTypography.label,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OrbitalButton(
    status: RadioStatus,
    playbackState: Int,
    clock: () -> Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = MaterialTheme.colorScheme.primary

    // The whole orbit is the tap target, not just the button drawn in its middle
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = status != RadioStatus.CONNECTING, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val millis = clock()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringR = size.minDimension / 2f * 0.9f

            // ryRatio ~0.55-0.65 looks like a circle tilted ~50°, not a squashed oval
            listOf(
                Triple(cycle(millis, 9000) * 360f, 1.00f, 0.62f),
                Triple(360f - cycle(millis, 14000) * 360f + 60f, 0.96f, 0.55f),
                Triple(cycle(millis, 20000) * 360f + 30f, 0.92f, 0.60f)
            ).forEach { (rotation, rxRatio, ryRatio) ->
                withTransform({ rotate(rotation, Offset(cx, cy)) }) {
                    val rx = ringR * rxRatio
                    val ry = ringR * ryRatio
                    val top = Offset(cx - rx, cy - ry)
                    val sz = Size(rx * 2f, ry * 2f)
                    drawOval(
                        color = color.copy(alpha = 0.04f),
                        topLeft = top, size = sz,
                        style = Stroke(width = 5.dp.toPx())
                    )
                    drawOval(
                        color = color.copy(alpha = 0.18f),
                        topLeft = top, size = sz,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // Scattered particle dots in the orbital band
            DOT_POSITIONS.forEachIndexed { i, (angleDeg, distRatio) ->
                val rad = angleDeg * PI.toFloat() / 180f
                val dist = ringR * distRatio
                drawCircle(
                    color = color.copy(alpha = 0.22f),
                    radius = DOT_RADII_DP[i].dp.toPx(),
                    center = Offset(cx + cos(rad) * dist, cy + sin(rad) * dist)
                )
            }
        }

        AnimatedContent(
            targetState = status,
            label = "btn",
            transitionSpec = { scaleIn(tween(200)) togetherWith scaleOut(tween(200)) }
        ) { targetStatus ->
            if (targetStatus == RadioStatus.CONNECTING) {
                MyCircularProgressIndicator()
            }
            else {
                val isPlaying = playbackState == PlaybackStateCompat.STATE_PLAYING
                MultiDrawableImage(
                    drawables = listOf(
                        (if (isPlaying) R.drawable.ic_radio_pause_container
                        else R.drawable.ic_radio_play_container)
                                to MaterialTheme.colorScheme.primaryContainer,
                        (if (isPlaying) R.drawable.ic_radio_pause_primary
                        else R.drawable.ic_radio_play_primary)
                                to MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.size(PlayButtonSize),
                    innerModifier = Modifier.fillMaxSize(),
                    contentDescription = stringResource(R.string.play_pause_btn_description)
                )
            }
        }
    }
}

@Composable
private fun Waves(color: Color, clock: () -> Long, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val millis = clock()
        val cy = size.height / 2f
        val amp = size.height * 0.28f
        val freq = 2.5f

        drawSineLine(
            color.copy(alpha = 0.55f), cy, cycle(millis, 5000) * TWO_PI,
            amp, freq, 2.dp.toPx()
        )
        drawSineLine(
            color.copy(alpha = 0.35f), cy, TWO_PI - cycle(millis, 7000) * TWO_PI + 0.8f,
            amp * 0.8f, freq, 1.5.dp.toPx()
        )
        drawSineLine(
            color.copy(alpha = 0.25f), cy, cycle(millis, 9000) * TWO_PI + 1.6f,
            amp * 0.9f, freq, 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawSineLine(
    color: Color,
    yCenter: Float,
    phase: Float,
    amplitude: Float,
    waveCount: Float,
    strokeWidth: Float
) {
    val path = Path()
    val steps = 200

    path.moveTo(0f, yCenter + amplitude * sin(phase))
    for (i in 1..steps) {
        val x = size.width * i / steps
        val y = yCenter + amplitude * sin(waveCount * TWO_PI * i / steps + phase)
        path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}
