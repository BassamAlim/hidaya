package bassamalim.hidaya.features.verseGuess

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyDropDownMenu
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.features.verseGuess.map.VerseMap
import bassamalim.hidaya.features.verseGuess.map.locationText

@Composable
fun VerseGuessScreen(viewModel: VerseGuessViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    val dims = MaterialTheme.dimensions

    MyScaffold(title = stringResource(R.string.verse_guess_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dims.screenPaddingHorizontal),
                contentAlignment = Alignment.Center
            ) {
                ScopeMenu(
                    scopeJuz = state.scopeJuz,
                    juzNumTexts = state.juzNumTexts,
                    onChange = viewModel::onScopeChange
                )
            }

            VerseMap(
                items = state.items,
                suraNames = state.suraNames,
                selectedIndex = state.selectedIndex,
                onSelect = viewModel::onVerseSelect,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            SelectionBar(
                text = state.selectedIndex?.let { state.items.getOrNull(it) }
                    ?.locationText(state.suraNames)
                    ?: stringResource(R.string.verse_map_hint)
            )
        }
    }
}

@Composable
private fun ScopeMenu(scopeJuz: Int, juzNumTexts: List<String>, onChange: (Int) -> Unit) {
    val juzLabel = stringResource(R.string.juz)

    MyDropDownMenu(
        selection = scopeJuz,
        items = (0..VerseGuessViewModel.JUZ_COUNT).toList().toTypedArray(),
        entries = (listOf(stringResource(R.string.whole_quran)) +
                juzNumTexts.map { "$juzLabel $it" }).toTypedArray(),
        onChoice = onChange
    )
}

@Composable
private fun SelectionBar(text: String) {
    val dims = MaterialTheme.dimensions

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = dims.elevationSm
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceLg
            ),
            style = MaterialTheme.appTypography.title
        )
    }
}
