package bassamalim.hidaya.features.verseGuess

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.features.verseGuess.map.VerseMap
import bassamalim.hidaya.features.verseGuess.map.locationText

private const val TOTAL_COUNT_UP_MILLIS = 900
/** A streak is worth showing from its second exact guess */
private const val MIN_SHOWN_STREAK = 2

@Composable
internal fun VerseGuessRound(
    state: VerseGuessUiState,
    readVerseProgress: () -> Float,
    onPlayPauseClick: () -> Unit,
    onReplayClick: () -> Unit,
    onClipVerseClick: (index: Int) -> Unit,
    onVerseSelect: (index: Int) -> Unit,
    onSelectClick: () -> Unit,
    onNextClick: () -> Unit,
    onOpenInQuranClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions
    val result = state.result

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceSm
            ),
            verticalArrangement = Arrangement.spacedBy(dims.spaceSm)
        ) {
            RoundHeader(state)

            RecitationCard(
                verses = state.clipVerses,
                playback = state.playback,
                readVerseProgress = readVerseProgress,
                reciterName = state.reciterNames.getOrElse(state.reciterId) { "" },
                suraName = result?.let { state.suraNames[state.items[it.clipStart].suraNum - 1] },
                isRevealed = result != null,
                onPlayPauseClick = onPlayPauseClick,
                onReplayClick = onReplayClick,
                onVerseClick = onClipVerseClick
            )
        }

        VerseMap(
            items = state.items,
            suraNames = state.suraNames,
            selectedIndex = state.selectedIndex,
            onSelect = onVerseSelect,
            answer = result?.let { it.clipStart until it.clipStart + CLIP_LENGTH },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        if (result == null)
            SelectBar(
                selectionText = state.selectedIndex?.let { state.items.getOrNull(it) }
                    ?.locationText(state.suraNames),
                onSelectClick = onSelectClick
            )
        else
            RevealPanel(
                state = state,
                result = result,
                onOpenInQuranClick = { onOpenInQuranClick(result.clipStart) },
                onNextClick = onNextClick
            )
    }
}

/** The round and this session's points, then the streak and the lifetime total. */
@Composable
private fun RoundHeader(state: VerseGuessUiState) {
    val dims = MaterialTheme.dimensions
    // Counts up when a round's points land
    val total by animateIntAsState(
        targetValue = state.progress.totalPoints.toInt(),
        animationSpec = tween(TOTAL_COUNT_UP_MILLIS),
        label = "total points"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.verse_guess_round_number,
                    formatNumber(state.sessionRounds, state.numeralsLanguage)
                ),
                style = MaterialTheme.appTypography.title
            )

            Text(
                text = stringResource(
                    R.string.verse_guess_session,
                    formatNumber(state.sessionPoints, state.numeralsLanguage)
                ),
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = state.progress.currentStreak >= MIN_SHOWN_STREAK,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            StreakChip(
                streak = state.progress.currentStreak,
                state = state,
                modifier = Modifier.padding(end = dims.spaceMd)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatNumber(total, state.numeralsLanguage),
                style = MaterialTheme.appTypography.headline,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.verse_guess_total_points),
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakChip(streak: Int, state: VerseGuessUiState, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.verse_guess_streak,
                streak,
                formatNumber(streak, state.numeralsLanguage)
            ),
            modifier = Modifier.padding(horizontal = dims.spaceMd, vertical = dims.spaceXs),
            style = MaterialTheme.appTypography.caption
        )
    }
}

@Composable
private fun SelectBar(selectionText: String?, onSelectClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = dims.elevationSm) {
        Row(
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceMd
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectionText ?: stringResource(R.string.verse_map_hint),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = dims.spaceMd),
                style = if (selectionText != null) MaterialTheme.appTypography.title
                    else MaterialTheme.appTypography.label,
                color = if (selectionText != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onSelectClick, enabled = selectionText != null) {
                Text(
                    text = stringResource(R.string.select),
                    style = MaterialTheme.appTypography.button
                )
            }
        }
    }
}

@Composable
private fun RevealPanel(
    state: VerseGuessUiState,
    result: RoundResult,
    onOpenInQuranClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = dims.elevationSm) {
        Column(
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceMd
            ),
            verticalArrangement = Arrangement.spacedBy(dims.spaceXs)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(
                        R.string.verse_guess_points,
                        formatNumber(result.points, state.numeralsLanguage)
                    ),
                    style = MaterialTheme.appTypography.display,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = distanceText(state, result),
                    modifier = Modifier.padding(start = dims.spaceMd, bottom = dims.spaceXs),
                    style = MaterialTheme.appTypography.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.verse_guess_answer, clipLocationText(state, result.clipStart)),
                style = MaterialTheme.appTypography.subtitle,
                color = MaterialTheme.colorScheme.tertiary
            )

            Text(
                text = stringResource(
                    R.string.verse_guess_your_guess,
                    state.items[result.guessIndex].locationText(state.suraNames)
                ),
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.padding(top = dims.spaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenInQuranClick) {
                    Text(
                        text = stringResource(R.string.open_in_quran),
                        style = MaterialTheme.appTypography.button
                    )
                }

                Box(Modifier.weight(1f))

                Button(onClick = onNextClick) {
                    Text(
                        text = stringResource(R.string.next_round),
                        style = MaterialTheme.appTypography.button
                    )
                }
            }
        }
    }
}
