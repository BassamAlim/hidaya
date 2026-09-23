package bassamalim.hidaya.features.recitations.surasMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.models.ReciterSura
import bassamalim.hidaya.core.ui.components.CustomSearchBar
import bassamalim.hidaya.core.ui.components.MyClickableSurface
import bassamalim.hidaya.core.ui.components.MyDownloadButton
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.TabLayout
import kotlinx.coroutines.flow.Flow

@Composable
fun RecitationSurasMenuScreen(viewModel: RecitationSurasViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart()
        onDispose(viewModel::onStop)
    }

    MyScaffold(
        title = state.title,
        onBack = viewModel::onBackPressed
    ) { padding ->
        TabLayout(
            pageNames = listOf(
                stringResource(R.string.all),
                stringResource(R.string.favorites),
                stringResource(R.string.downloaded)
            ),
            modifier = Modifier.padding(padding),
            searchComponent = {
                CustomSearchBar(
                    query = state.searchText,
                    hint = stringResource(R.string.suras_search_hint),
                    modifier = Modifier.fillMaxWidth(),
                    onQueryChange = viewModel::onSearchChange
                )
            }
        ) { page ->
            Tab(
                surasFlow = viewModel.getItems(page),
                downloadStates = state.downloadStates,
                onSuraClick = viewModel::onSuraClick,
                onFavoriteClick = viewModel::onFavoriteClick,
                onDownloadClick = viewModel::onDownloadClick
            )
        }
    }
}

@Composable
private fun Tab(
    surasFlow: Flow<List<ReciterSura>>,
    downloadStates: Map<Int, DownloadState>,
    onSuraClick: (Int) -> Unit,
    onFavoriteClick: (ReciterSura) -> Unit,
    onDownloadClick: (ReciterSura) -> Unit
) {
    val suras by surasFlow.collectAsStateWithLifecycle(emptyList())

    MyLazyColumn(
        lazyList = {
            items(suras, key = { it.id }) { sura ->
                SuraCard(
                    sura = sura,
                    downloadState = downloadStates[sura.id] ?: DownloadState.NOT_DOWNLOADED,
                    onClick = onSuraClick,
                    onFavoriteClick = onFavoriteClick,
                    onDownloadClick = onDownloadClick
                )
            }
        }
    )
}

@Composable
private fun SuraCard(
    sura: ReciterSura,
    downloadState: DownloadState,
    onClick: (Int) -> Unit,
    onFavoriteClick: (ReciterSura) -> Unit,
    onDownloadClick: (ReciterSura) -> Unit
) {
    MyClickableSurface(
        modifier = Modifier.padding(2.dp),
        onClick = { onClick(sura.id) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MyText(
                text = sura.suraName,
                modifier = Modifier
                    .weight(1F)
                    .padding(top = 12.dp, bottom = 12.dp, start = 20.dp),
                textAlign = TextAlign.Start
            )

            MyFavoriteButton(
                isFavorite = sura.isFavorite,
                onClick = { onFavoriteClick(sura) }
            )

            MyDownloadButton(
                state = downloadState,
                iconSize = 28.dp,
                onClick = { onDownloadClick(sura) }
            )
        }
    }
}