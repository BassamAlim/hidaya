package bassamalim.hidaya.features.tv

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyCircularProgressIndicator
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

private val ChannelLogoSize = 64.dp

@Composable
fun TvScreen(viewModel: TvViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!
    val snackbarHostState = remember { SnackbarHostState() }
    val playbackFailedMessage = stringResource(R.string.playback_failed)

    LaunchedEffect(state.isPlaybackFailed) {
        if (state.isPlaybackFailed) {
            snackbarHostState.showSnackbar(playbackFailedMessage)
            viewModel.onPlaybackFailedShown()
        }
    }

    LifecycleStartEffect(Unit) {
        viewModel.onStart()
        onStopOrDispose {
            // the activity is recreated on rotation, playback should continue through it
            if (!activity.isChangingConfigurations) viewModel.onStop()
        }
    }

    BackHandler(enabled = state.isFullscreen, onBack = viewModel::onBackPressedInFullscreen)

    FullscreenEffect(activity = activity, isFullscreen = state.isFullscreen)

    KeepScreenOn()

    if (state.isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(
                player = viewModel.player,
                isLoading = state.isLoading,
                isFullscreen = true,
                onFullscreenButtonClick = viewModel::onFullscreenButtonClick,
                modifier = Modifier
                    .fillMaxSize()
                    .displayCutoutPadding()
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    else {
        MyScaffold(
            title = stringResource(R.string.tv_channels),
            snackBarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val maxPlayerHeight = maxHeight * 0.6f

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VideoPlayer(
                        player = viewModel.player,
                        isLoading = state.isLoading,
                        isFullscreen = false,
                        onFullscreenButtonClick = viewModel::onFullscreenButtonClick,
                        modifier = Modifier
                            .heightIn(max = maxPlayerHeight)
                            .aspectRatio(16f / 9f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(MaterialTheme.dimensions.spaceLg),
                        horizontalArrangement =
                            Arrangement.spacedBy(MaterialTheme.dimensions.spaceMd)
                    ) {
                        ChannelCard(
                            text = stringResource(R.string.quran_channel),
                            painter = painterResource(R.mipmap.ic_quran_channel),
                            isSelected = state.selectedChannel == TvChannel.QURAN,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = viewModel::onQuranChannelClick
                        )

                        ChannelCard(
                            text = stringResource(R.string.sunnah_channel),
                            painter = painterResource(R.mipmap.ic_sunnah_channel),
                            isSelected = state.selectedChannel == TvChannel.SUNNAH,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = viewModel::onSunnahChannelClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    player: Player,
    isLoading: Boolean,
    isFullscreen: Boolean,
    onFullscreenButtonClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShowPreviousButton(false)
                    setShowNextButton(false)
                    setShowRewindButton(false)
                    setShowFastForwardButton(false)
                    setFullscreenButtonClickListener { onFullscreenButtonClick(it) }
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.setFullscreenButtonState(isFullscreen)
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) MyCircularProgressIndicator()
    }
}

/** A channel to switch to; the one playing is highlighted and marked live. */
@Composable
private fun ChannelCard(
    text: String,
    painter: Painter,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = modifier.then(
            if (isSelected) Modifier.border(
                width = dims.borderThick,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(dims.radiusLg)
            )
            else Modifier
        ),
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor =
                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ChannelLogoSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(Modifier.height(dims.spaceSm))

            Text(
                text = text,
                style = MaterialTheme.appTypography.title,
                textAlign = TextAlign.Center
            )

            // Kept in the layout when hidden so both cards stay the same height
            Row(
                modifier = Modifier
                    .padding(top = dims.spaceXs)
                    .alpha(if (isSelected) 1f else 0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(dims.spaceSm)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )

                Spacer(Modifier.width(dims.spaceXs))

                Text(
                    text = stringResource(R.string.radio_live),
                    style = MaterialTheme.appTypography.caption
                )
            }
        }
    }
}

@Composable
private fun FullscreenEffect(activity: Activity, isFullscreen: Boolean) {
    DisposableEffect(isFullscreen) {
        if (!isFullscreen) return@DisposableEffect onDispose {}

        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            // on rotation the recreated activity re-applies fullscreen from the ui state
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }
}
