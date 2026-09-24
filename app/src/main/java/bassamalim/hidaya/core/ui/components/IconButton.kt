package bassamalim.hidaya.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.PlaybackStatus

@Composable
fun MyIconButton(
    iconId: Int,
    modifier: Modifier = Modifier,
    description: String = "",
    iconSize: Dp = 24.dp,
    iconColor: Color = LocalContentColor.current,
    enabled: Boolean = true,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onClick: () -> Unit
) {
    IconButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = description,
            modifier = Modifier
                .size(iconSize)
                .padding(innerPadding),
            tint = iconColor
        )
    }
}

@Composable
fun MyIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    description: String = "",
    iconColor: Color = LocalContentColor.current,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = { if (isEnabled) onClick() },
        modifier = modifier,
        enabled = isEnabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = description,
            modifier = iconModifier,
            tint = iconColor
        )
    }
}

@Composable
fun MyFilledTonalIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    description: String = "",
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = { if (isEnabled) onClick() },
        modifier = modifier,
        enabled = isEnabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = description,
            modifier = iconModifier
        )
    }
}

@Composable
fun MyBackButton(onClick: (() -> Unit)? = null) {
    val context = LocalContext.current

    MyIconButton(Icons.AutoMirrored.Default.ArrowBackIos) {
        if (onClick == null)
            (context as ComponentActivity).onBackPressedDispatcher.onBackPressed()
        else
            onClick()
    }
}

@Composable
fun MyFavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector =
                if (isFavorite) Icons.Filled.Star
                else Icons.Outlined.StarOutline,
            contentDescription = stringResource(R.string.favorites),
            modifier = Modifier.size(size),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MyDownloadButton(
    state: DownloadState,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (state == DownloadState.DOWNLOADING)
            MyCircularProgressIndicator()
        else {
            MyIconButton(
                imageVector =
                    if (state == DownloadState.DOWNLOADED) Icons.Default.DownloadDone
                    else Icons.Default.Download,
                description = stringResource(R.string.download_description),
                iconModifier = Modifier.size(iconSize),
                onClick = onClick
            )
        }
    }
}

@Composable
fun MyIconPlayerButton(
    state: PlaybackStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified,
    enabled: Boolean = true,
    filled: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    IconButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = state,
            label = "",
            transitionSpec = {
                scaleIn(animationSpec = tween(durationMillis = 200)) togetherWith
                        scaleOut(animationSpec = tween(durationMillis = 200))
            }
        ) { state ->
            if (state == PlaybackStatus.CONNECTING || state == PlaybackStatus.BUFFERING)
                MyCircularProgressIndicator()
            else {
                Icon(
                    imageVector =
                        if (filled) {
                            if (state == PlaybackStatus.PLAYING)
                                Icons.Default.PauseCircleFilled
                            else Icons.Default.PlayCircleFilled
                        }
                        else {
                            if (state == PlaybackStatus.PLAYING) Icons.Default.Pause
                            else Icons.Default.PlayArrow
                        },
                    contentDescription = stringResource(R.string.play_pause_btn_description),
                    modifier = Modifier.size(iconSize),
                    tint = tint
                )
            }
        }
    }
}

@Composable
fun MyCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    MyIconButton(
        imageVector = Icons.Default.Close,
        modifier = modifier,
        description = stringResource(R.string.close),
        onClick = onClose
    )
}