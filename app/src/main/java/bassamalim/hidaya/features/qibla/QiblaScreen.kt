package bassamalim.hidaya.features.qibla

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MultiDrawableImage
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyDialog
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.theme.Negative
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun QiblaScreen(viewModel: QiblaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose { viewModel.onStop() }
    }

    val view = LocalView.current
    LaunchedEffect(state.isOnPoint) {
        // Respects the system touch-feedback setting
        if (state.isOnPoint) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    MyScaffold(title = stringResource(R.string.qibla)) { padding ->
        val errorMessageResId = state.errorMessageResId
        if (errorMessageResId != null) {
            ErrorState(
                messageResId = errorMessageResId,
                modifier = Modifier.padding(padding),
                onSetLocationClick = viewModel::onSetLocationClick
            )
        }
        else {
            QiblaContent(
                state = state,
                modifier = Modifier.padding(padding),
                onAccuracyClick = viewModel::onAccuracyIndicatorClick
            )
        }

        CalibrationDialog(
            calibrationDialogShown = state.calibrationDialogShown,
            onCalibrationDialogDismiss = viewModel::onCalibrationDialogDismiss
        )
    }
}

@Composable
private fun QiblaContent(
    state: QiblaUiState,
    modifier: Modifier = Modifier,
    onAccuracyClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = dims.screenPaddingHorizontal, vertical = dims.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DirectionStatus(
            isOnPoint = state.isOnPoint,
            offsetDegrees = state.offsetDegrees,
            offsetText = state.offsetText
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            QiblaGraphics(
                compassAngle = state.compassAngle,
                qiblaAngle = state.qiblaAngle,
                isOnPoint = state.isOnPoint
            )
        }

        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dims.spaceMd)
        ) {
            InfoTile(
                label = stringResource(R.string.kaaba_distance),
                value = "${state.distanceToKaaba} ${stringResource(R.string.distance_unit)}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            AccuracyTile(
                accuracy = state.accuracy,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onAccuracyClick
            )
        }
    }
}

@Composable
private fun DirectionStatus(isOnPoint: Boolean, offsetDegrees: Int, offsetText: String) {
    val dims = MaterialTheme.dimensions
    val containerColor by animateColorAsState(
        targetValue =
            if (isOnPoint) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "direction status container"
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isOnPoint) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        label = "direction status content"
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dims.spaceXl, vertical = dims.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Physical left/right, so these icons are deliberately not auto-mirrored
            Icon(
                imageVector = when {
                    isOnPoint -> Icons.Default.CheckCircle
                    offsetDegrees > 0 -> Icons.Default.TurnRight
                    else -> Icons.Default.TurnLeft
                },
                contentDescription = null,
                modifier = Modifier.size(dims.iconMd)
            )

            Spacer(Modifier.width(dims.spaceSm))

            Text(
                text = when {
                    isOnPoint -> stringResource(R.string.facing_qibla)
                    offsetDegrees > 0 -> stringResource(R.string.turn_right, offsetText)
                    else -> stringResource(R.string.turn_left, offsetText)
                },
                style = MaterialTheme.appTypography.title
            )
        }
    }
}

@Composable
private fun QiblaGraphics(compassAngle: Float, qiblaAngle: Float, isOnPoint: Boolean) {
    val glowColor by animateColorAsState(
        targetValue =
            if (isOnPoint) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent,
        label = "compass glow"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .padding(10.dp)
                .background(color = glowColor, shape = CircleShape)
        )

        // compass image
        MultiDrawableImage(
            drawables = listOf(
                R.drawable.compass_primary to MaterialTheme.colorScheme.primary,
                R.drawable.compass_on_surface to MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .rotate(compassAngle)
                .padding(horizontal = 10.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.qibla_pointer),
            contentDescription = "",
            modifier = Modifier
                .rotate(qiblaAngle)
                .padding(bottom = 26.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        Image(
            painter = painterResource(id = R.drawable.ic_qibla_kaaba),
            contentDescription = ""
        )
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg)
    ) {
        Text(
            text = label,
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(dims.spaceXs))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(dims.spaceSm))
            }

            Text(text = value, style = MaterialTheme.appTypography.title)
        }
    }
}

@Composable
private fun AccuracyTile(accuracy: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // SensorManager accuracy: 0 unreliable, 1 low, 2 medium, 3 high
    val (valueResId, color) = when (accuracy) {
        0, 1 -> R.string.accuracy_low to MaterialTheme.colorScheme.error
        2 -> R.string.accuracy_moderate to MaterialTheme.colorScheme.tertiary
        else -> R.string.accuracy_good to MaterialTheme.colorScheme.primary
    }

    InfoTile(
        label = stringResource(R.string.accuracy_indicator_description),
        value = stringResource(valueResId),
        modifier = modifier,
        onClick = onClick,
        leading = {
            Box(
                Modifier
                    .size(MaterialTheme.dimensions.spaceMd)
                    .background(color = color, shape = CircleShape)
            )
        }
    )
}

@Composable
private fun ErrorState(
    messageResId: Int,
    modifier: Modifier = Modifier,
    onSetLocationClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val isLocationError = messageResId == R.string.location_permission_for_qibla

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dims.spaceXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isLocationError) Icons.Default.LocationOff else Icons.Default.SensorsOff,
            contentDescription = null,
            modifier = Modifier.size(dims.iconXl * 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(dims.spaceLg))

        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.appTypography.title,
            textAlign = TextAlign.Center
        )

        if (isLocationError) {
            Spacer(Modifier.height(dims.spaceXl))

            Button(onClick = onSetLocationClick) {
                Text(
                    text = stringResource(R.string.set_location),
                    style = MaterialTheme.appTypography.button
                )
            }
        }
    }
}

@Composable
private fun CalibrationDialog(
    calibrationDialogShown: Boolean,
    onCalibrationDialogDismiss: () -> Unit
) {
    val context = LocalContext.current

    MyDialog(
        shown = calibrationDialogShown,
        onDismiss = onCalibrationDialogDismiss,
    ) {
        Column(
            Modifier.padding(vertical = 20.dp, horizontal = 30.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = getImageRequest(context),
                    imageLoader = getImageLoader(context)
                ),
                contentDescription = stringResource(R.string.compass_calibration),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            MyText(
                text = stringResource(R.string.qibla_warning),
                color = Negative
            )
        }
    }
}

@Composable
private fun getImageLoader(context: Context) =
    ImageLoader.Builder(context).components {
        if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
        else add(GifDecoder.Factory())
    }.build()

@Composable
private fun getImageRequest(context: Context) =
    ImageRequest.Builder(context)
    .data(R.drawable.compass_calibration)
    .apply(block = { size(Size.ORIGINAL) }).build()
