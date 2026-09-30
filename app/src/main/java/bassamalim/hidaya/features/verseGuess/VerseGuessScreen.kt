package bassamalim.hidaya.features.verseGuess

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VerseGuessScreen(viewModel: VerseGuessViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!

    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            // The activity is recreated on rotation, and the clip should play through it
            if (!activity.isChangingConfigurations) viewModel.onStop()
        }
    }

    BackHandler(enabled = state.phase != VerseGuessPhase.SETUP, onBack = viewModel::onBackPressed)

    if (state.isLoading) return

    MyScaffold(title = stringResource(R.string.verse_guess_title)) { padding ->
        val modifier = Modifier.padding(padding)

        when (state.phase) {
            VerseGuessPhase.SETUP -> VerseGuessSetup(
                state = state,
                onScopeChange = viewModel::onScopeChange,
                onReciterChange = viewModel::onReciterChange,
                onStartClick = viewModel::onStartClick,
                modifier = modifier
            )
            VerseGuessPhase.ROUND -> VerseGuessRound(
                state = state,
                readVerseProgress = viewModel::readVerseProgress,
                onPlayPauseClick = viewModel::onPlayPauseClick,
                onReplayClick = viewModel::onReplayClick,
                onClipVerseClick = viewModel::onClipVerseClick,
                onVerseSelect = viewModel::onVerseSelect,
                onSelectClick = viewModel::onSelectClick,
                onNextClick = viewModel::onNextClick,
                onOpenInQuranClick = viewModel::onOpenInQuranClick,
                modifier = modifier
            )
        }
    }
}

/** With thousands separators, in the user's numerals. */
internal fun formatNumber(value: Long, numeralsLanguage: Language) =
    translateNums(String.format(Locale.US, "%,d", value), numeralsLanguage)

internal fun formatNumber(value: Int, numeralsLanguage: Language) =
    formatNumber(value.toLong(), numeralsLanguage)

/** Sura, verse range and page of the clip starting at [clipStart]. */
@Composable
internal fun clipLocationText(state: VerseGuessUiState, clipStart: Int): String {
    val first = state.items[clipStart]
    val last = state.items[clipStart + CLIP_LENGTH - 1]
    return stringResource(
        R.string.verse_range_location,
        state.suraNames[first.suraNum - 1],
        first.verseNumText,
        last.verseNumText,
        first.pageNumText
    )
}

/** How far off the guess was: spot on, on one of the clip's pages, or so many pages away. */
@Composable
internal fun distanceText(state: VerseGuessUiState, result: RoundResult): String {
    val clipPages = (result.clipStart until result.clipStart + CLIP_LENGTH)
        .map { state.items[it].pageNum }

    return when {
        result.distancePages == 0.0 -> stringResource(R.string.verse_guess_exact)
        state.items[result.guessIndex].pageNum in clipPages ->
            stringResource(R.string.verse_guess_same_page)
        else -> {
            val pages = result.distancePages.roundToInt().coerceAtLeast(1)
            pluralStringResource(
                R.plurals.verse_guess_pages_away,
                pages,
                formatNumber(pages, state.numeralsLanguage)
            )
        }
    }
}
