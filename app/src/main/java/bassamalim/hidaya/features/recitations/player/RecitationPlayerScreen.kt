package bassamalim.hidaya.features.recitations.player

import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.ui.components.MyDownloadButton
import bassamalim.hidaya.core.ui.components.MyIconButton
import bassamalim.hidaya.core.ui.components.MyIconPlayerButton
import bassamalim.hidaya.core.ui.components.MyProgressSlider
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlin.math.sqrt

private val CoverMaxSize = 320.dp
private val SkipButtonSize = 56.dp
private val PlayButtonSize = 80.dp
private const val STAR_TURN_MILLIS = 60_000

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecitationPlayerScreen(viewModel: RecitationPlayerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!

    if (state.isLoading) return

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(activity)
        onDispose { viewModel.onStop(activity) }
    }

    val dims = MaterialTheme.dimensions

    MyScaffold(
        title = stringResource(R.string.recitations),
        onBack = { viewModel.onBackPressed(activity) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = dims.spaceXl, vertical = dims.spaceLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The cover takes whatever height is left, so controls stay on screen in any size
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Cover(
                    suraName = state.suraName,
                    isPlaying = state.btnState == PlaybackStateCompat.STATE_PLAYING,
                    modifier = Modifier.size(min(min(maxWidth, maxHeight), CoverMaxSize))
                )
            }

            TrackInfo(reciterName = state.reciterName, narrationName = state.narrationName)

            ProgressSection(
                progress = viewModel.progress,
                progressText = state.progress,
                duration = viewModel.duration,
                durationText = state.duration,
                isEnabled = state.controlsEnabled,
                onSliderChange = viewModel::onSliderChange,
                onSliderChangeFinished = viewModel::onSliderChangeFinished
            )

            TransportControls(
                playbackState = state.btnState,
                isEnabled = state.controlsEnabled,
                onPreviousTrackClick = viewModel::onPreviousTrackClick,
                onPlayPauseClick = viewModel::onPlayPauseClick,
                onNextTrackClick = viewModel::onNextTrackClick
            )

            SecondaryControls(
                repeatMode = state.repeatMode,
                shuffleMode = state.shuffleMode,
                downloadState = state.downloadState,
                onRepeatClick = viewModel::onRepeatClick,
                onShuffleClick = viewModel::onShuffleClick,
                onDownloadClick = viewModel::onDownloadClick
            )
        }
    }
}

@Composable
private fun Cover(suraName: String, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    // Turns slowly while playing and holds its angle when paused
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rotation.animateTo(
                    targetValue = rotation.value + 360f,
                    animationSpec = tween(durationMillis = STAR_TURN_MILLIS, easing = LinearEasing)
                )
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dims.radiusLg * 2),
        color = Color.Transparent,
        contentColor = contentColor,
        shadowElevation = dims.elevationMd
    ) {
        Box(
            modifier = Modifier.background(
                // Both ends stay close to primaryContainer, which onPrimaryContainer text is
                // guaranteed to contrast with, so the name stays readable in every theme
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        lerp(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary,
                            0.15f
                        )
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            StarPattern(
                color = contentColor.copy(alpha = 0.12f),
                // Read in the draw phase so each frame only redraws, never recomposes
                rotation = { rotation.value },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier.padding(dims.spaceXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dims.spaceLg)
            ) {
                Text(
                    text = stringResource(R.string.sura),
                    style = MaterialTheme.appTypography.title,
                    color = contentColor.copy(alpha = 0.7f)
                )

                Text(
                    text = suraName,
                    // Extra line height so diacritics on the name don't collide with the label
                    style = MaterialTheme.appTypography.display.copy(
                        fontSize = 40.sp,
                        lineHeight = 56.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** An eight-pointed star (two overlapping squares) framed by two rings. */
@Composable
private fun StarPattern(color: Color, rotation: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.4f
        val side = radius * sqrt(2f)
        val stroke = Stroke(width = 2.dp.toPx())

        drawCircle(color = color, radius = radius * 1.1f, style = stroke)
        drawCircle(color = color, radius = radius * 0.62f, style = stroke)

        rotate(rotation()) {
            repeat(2) { i ->
                rotate(45f * i) {
                    drawRect(
                        color = color,
                        topLeft = center - Offset(side / 2, side / 2),
                        size = Size(side, side),
                        style = stroke
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackInfo(reciterName: String, narrationName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimensions.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = reciterName,
            style = MaterialTheme.appTypography.h1,
            textAlign = TextAlign.Center
        )

        Text(
            text = narrationName,
            style = MaterialTheme.appTypography.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressSection(
    progress: Long,
    progressText: String,
    duration: Long,
    durationText: String,
    isEnabled: Boolean,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit
) {
    Column(Modifier.padding(top = MaterialTheme.dimensions.spaceLg)) {
        MyProgressSlider(
            value = progress.toFloat(),
            valueRange = 0F..duration.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled,
            onValueChange = onSliderChange,
            onValueChangeFinished = onSliderChangeFinished
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = progressText,
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = durationText,
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportControls(
    playbackState: Int,
    isEnabled: Boolean,
    onPreviousTrackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextTrackClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dims.spaceXl, Alignment.CenterHorizontally)
    ) {
        MyIconButton(
            iconId = R.drawable.ic_skip_previous,
            description = stringResource(R.string.previous_track_btn_description),
            modifier = Modifier.size(SkipButtonSize),
            iconSize = dims.iconXl,
            iconColor = MaterialTheme.colorScheme.onSurface,
            enabled = isEnabled,
            onClick = onPreviousTrackClick
        )

        Surface(
            modifier = Modifier.size(PlayButtonSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            MyIconPlayerButton(
                state = playbackState,
                enabled = isEnabled,
                iconSize = dims.iconXl,
                filled = false,
                tint = MaterialTheme.colorScheme.onPrimary,
                onClick = onPlayPauseClick
            )
        }

        MyIconButton(
            iconId = R.drawable.ic_skip_next,
            description = stringResource(R.string.next_track_btn_description),
            modifier = Modifier.size(SkipButtonSize),
            iconSize = dims.iconXl,
            iconColor = MaterialTheme.colorScheme.onSurface,
            enabled = isEnabled,
            onClick = onNextTrackClick
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SecondaryControls(
    repeatMode: Int,
    shuffleMode: Int,
    downloadState: DownloadState,
    onRepeatClick: (Int) -> Unit,
    onShuffleClick: (Int) -> Unit,
    onDownloadClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimensions.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToggleButton(
            // Only single-track repeat is toggled here, so it gets the "repeat one" icon when on
            icon =
                if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                else Icons.Default.Repeat,
            description = stringResource(R.string.repeat_description),
            isActive = repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE,
            onClick = { onRepeatClick(repeatMode) }
        )

        MyDownloadButton(
            state = downloadState,
            onClick = onDownloadClick,
            iconSize = MaterialTheme.dimensions.iconMd
        )

        ToggleButton(
            icon = Icons.Default.Shuffle,
            description = stringResource(R.string.shuffle_description),
            isActive = shuffleMode == SHUFFLE_MODE_ALL,
            onClick = { onShuffleClick(shuffleMode) }
        )
    }
}

@Composable
private fun ToggleButton(
    icon: ImageVector,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint =
                if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
