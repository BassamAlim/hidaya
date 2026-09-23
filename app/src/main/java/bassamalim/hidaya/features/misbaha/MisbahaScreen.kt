package bassamalim.hidaya.features.misbaha

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums

private val CounterMaxSize = 320.dp
private val RingWidth = 10.dp

@Composable
fun MisbahaScreen(viewModel: MisbahaViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dims = MaterialTheme.dimensions
    val view = LocalView.current

    MyScaffold(title = stringResource(R.string.misbaha)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = dims.screenPaddingHorizontal,
                    vertical = dims.spaceXl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TargetSelector(
                selected = state.target,
                targetLabel = { target ->
                    if (target == null) "∞"
                    else translateNums(
                        string = target.toString(),
                        numeralsLanguage = state.numeralsLanguage
                    )
                },
                onSelect = viewModel::onTargetChange
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Counter(
                    countText = state.countText,
                    roundsText = state.roundsText,
                    progress = state.progress,
                    modifier = Modifier.size(min(min(maxWidth, maxHeight) * 0.9f, CounterMaxSize)),
                    onClick = {
                        val completedRound = viewModel.onIncrementClick()
                        // Respects the system touch-feedback setting
                        view.performHapticFeedback(
                            if (completedRound) HapticFeedbackConstants.LONG_PRESS
                            else HapticFeedbackConstants.KEYBOARD_TAP
                        )
                    }
                )
            }

            TextButton(onClick = viewModel::onResetClick) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(dims.iconSm + dims.spaceXs)
                )

                Spacer(Modifier.width(dims.spaceSm))

                Text(
                    text = stringResource(R.string.reset),
                    style = MaterialTheme.appTypography.button
                )
            }
        }
    }
}

@Composable
private fun TargetSelector(
    selected: Int?,
    targetLabel: (Int?) -> String,
    onSelect: (Int?) -> Unit
) {
    val targets = MisbahaViewModel.TARGETS

    SingleChoiceSegmentedButtonRow {
        targets.forEachIndexed { index, target ->
            SegmentedButton(
                selected = target == selected,
                onClick = { onSelect(target) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = targets.size)
            ) {
                Text(
                    text = targetLabel(target),
                    style = MaterialTheme.appTypography.button
                )
            }
        }
    }
}

@Composable
private fun Counter(
    countText: String,
    roundsText: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 150),
        label = "misbaha progress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = RingWidth,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )

        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(RingWidth * 2),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = MaterialTheme.dimensions.elevationMd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.appTypography.display.copy(fontSize = 80.sp)
                )

                // Kept even when empty so the count doesn't jump when the first round completes
                Text(
                    text = roundsText,
                    style = MaterialTheme.appTypography.title
                )
            }
        }
    }
}
