package bassamalim.hidaya.features.remembrances.reader

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.ReaderBottomBar
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The stored text size is an offset over this base (kept from the original reader)
private const val BASE_TEXT_SIZE = 10
private const val LINE_HEIGHT_RATIO = 1.6f
// Long enough to see the passage turn "done" before the list moves on
private const val ADVANCE_DELAY_MILLIS = 400L

@Composable
fun RemembranceReaderScreen(viewModel: RemembranceReaderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val dims = MaterialTheme.dimensions

    MyScaffold(
        title = state.title,
        bottomBar = {
            ReaderBottomBar(textSize = state.textSize, onSeek = viewModel::onTextSizeSliderChange)
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceMd
            ),
            verticalArrangement = Arrangement.spacedBy(dims.spaceMd)
        ) {
            itemsIndexed(state.items, key = { _, passage -> passage.id }) { index, passage ->
                PassageCard(
                    passage = passage,
                    textSize = state.textSize,
                    onRepetitionClick = {
                        when (viewModel.onRepetitionClick(passage.id)) {
                            RepetitionResult.COUNTED ->
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            RepetitionResult.COMPLETED -> {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                // Brings up the next passage so the user can keep going
                                if (index < state.items.lastIndex) scope.launch {
                                    delay(ADVANCE_DELAY_MILLIS)
                                    listState.animateScrollToItem(index + 1)
                                }
                            }
                            RepetitionResult.IGNORED -> {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PassageCard(
    passage: RemembrancePassage,
    textSize: Float,
    onRepetitionClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val fontSize = textSize + BASE_TEXT_SIZE
    val detailsFontSize = fontSize - 6
    var isExpanded by remember { mutableStateOf(false) }

    MyCard(
        shape = RoundedCornerShape(dims.radiusLg),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dims.spaceMd)) {
            if (passage.isTitleAvailable) {
                Text(
                    text = passage.title!!,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.appTypography.title.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * LINE_HEIGHT_RATIO).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            // The words being recited: full contrast, since this is what the user reads most
            Text(
                text = passage.text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.appTypography.body.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * LINE_HEIGHT_RATIO).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (passage.isTranslationAvailable) {
                Text(
                    text = passage.translation!!,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.appTypography.body.copy(
                        fontSize = detailsFontSize.sp,
                        lineHeight = (detailsFontSize * LINE_HEIGHT_RATIO).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)
            ) {
                RepetitionButton(
                    passage = passage,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = onRepetitionClick
                )

                if (passage.isVirtueAvailable || passage.isReferenceAvailable) {
                    ExpandButton(
                        isExpanded = isExpanded,
                        modifier = Modifier.fillMaxHeight(),
                        onClick = { isExpanded = !isExpanded }
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
                    if (passage.isVirtueAvailable) {
                        Detail(
                            label = stringResource(R.string.virtue),
                            text = passage.virtue!!,
                            fontSize = detailsFontSize
                        )
                    }

                    if (passage.isReferenceAvailable) {
                        Detail(
                            label = stringResource(R.string.reference),
                            text = passage.reference!!,
                            fontSize = detailsFontSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepetitionButton(
    passage: RemembrancePassage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val isComplete = passage.isRepetitionComplete
    val containerColor by animateColorAsState(
        targetValue =
            if (isComplete) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
        label = "repetition container"
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isComplete) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "repetition content"
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        if (isComplete) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(dims.iconMd)
            )

            Spacer(Modifier.width(dims.spaceSm))
        }

        Text(
            text = passage.repetitionText,
            modifier = Modifier.padding(vertical = dims.spaceXs),
            style = MaterialTheme.appTypography.h1
        )
    }
}

@Composable
private fun ExpandButton(isExpanded: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "expand arrow")

    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(R.string.title_more),
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
private fun Detail(label: String, text: String, fontSize: Float) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.appTypography.label.copy(fontSize = fontSize.sp),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = text,
            style = MaterialTheme.appTypography.body.copy(
                fontSize = fontSize.sp,
                lineHeight = (fontSize * LINE_HEIGHT_RATIO).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
