package bassamalim.hidaya.features.verseGuess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import bassamalim.hidaya.R
import bassamalim.hidaya.core.models.VerseGuessStats
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
internal fun VerseGuessSetup(
    state: VerseGuessUiState,
    onScopeChange: (juz: Int) -> Unit,
    onReciterChange: (reciterId: Int) -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions
    val juzLabel = stringResource(R.string.juz)
    val scopeOptions = listOf(stringResource(R.string.whole_quran)) +
            state.juzNumTexts.map { "$juzLabel $it" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dims.screenPaddingHorizontal, vertical = dims.screenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(dims.spaceMd)
    ) {
        IntroCard()

        if (state.progress.roundsPlayed > 0) StatsGrid(state)
        else Text(
            text = stringResource(
                R.string.verse_guess_rules,
                formatNumber(MAX_ROUND_POINTS, state.numeralsLanguage)
            ),
            style = MaterialTheme.appTypography.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingRow(
            label = stringResource(R.string.verse_guess_scope),
            options = scopeOptions,
            selected = state.scopeJuz,
            onSelect = onScopeChange
        )

        SettingRow(
            label = stringResource(R.string.reciter),
            options = state.reciterNames,
            selected = state.reciterId,
            onSelect = onReciterChange
        )

        Spacer(Modifier.height(dims.spaceSm))

        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.start_game),
                style = MaterialTheme.appTypography.button
            )
        }
    }
}

@Composable
private fun IntroCard() {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    MyCard(
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(dims.spaceXl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dims.iconXl + dims.spaceLg)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(dims.radiusLg)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TravelExplore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dims.iconLg)
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = dims.spaceLg)
            ) {
                Text(
                    text = stringResource(R.string.verse_guess_title),
                    style = MaterialTheme.appTypography.display,
                    color = contentColor
                )

                Text(
                    text = stringResource(R.string.verse_guess_description),
                    style = MaterialTheme.appTypography.label,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/** The user's standing across every game, two tiles a row. */
@Composable
private fun StatsGrid(state: VerseGuessUiState) {
    val dims = MaterialTheme.dimensions
    val progress = state.progress
    val numerals = state.numeralsLanguage

    Column(verticalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
            StatTile(
                value = formatNumber(progress.totalPoints, numerals),
                label = stringResource(R.string.verse_guess_total_points),
                isHighlighted = true,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = formatNumber(progress.roundsPlayed, numerals),
                label = stringResource(R.string.verse_guess_rounds_played),
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
            StatTile(
                value = progress.recentAverage?.let { formatNumber(it, numerals) } ?: "–",
                label = stringResource(
                    R.string.verse_guess_recent_average,
                    formatNumber(VerseGuessStats.RECENT_ROUNDS, numerals)
                ),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = formatNumber(progress.bestStreak, numerals),
                label = stringResource(R.string.verse_guess_best_streak),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val dims = MaterialTheme.dimensions
    val contentColor =
        if (isHighlighted) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface

    MyCard(
        modifier = modifier,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceMd)
    ) {
        Text(
            text = value,
            style = MaterialTheme.appTypography.headline,
            color = contentColor,
            maxLines = 1
        )

        Text(
            text = label,
            style = MaterialTheme.appTypography.caption,
            color = if (isHighlighted) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A labeled card showing [options]`[selected]`, opening the rest in a menu. */
@Composable
private fun SettingRow(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val dims = MaterialTheme.dimensions
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        MyCard(
            onClick = { isExpanded = true },
            shape = RoundedCornerShape(dims.radiusLg),
            contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceMd)
        ) {
            Text(
                text = label,
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = options.getOrElse(selected) { "" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.appTypography.title
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.appTypography.body) },
                    onClick = {
                        isExpanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}
