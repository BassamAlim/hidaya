package bassamalim.hidaya.features.remembrances.remembrancesMenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.ui.components.CustomSearchBar
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun RemembrancesMenuScreen(viewModel: RemembrancesMenuViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    MyScaffold(
        title = when (state.menuType) {
            MenuType.FAVORITES -> stringResource(R.string.favorite_remembrances)
            MenuType.CUSTOM -> state.categoryTitle
            else -> stringResource(R.string.all_remembrances)
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            CustomSearchBar(
                query = state.searchText,
                modifier = Modifier.fillMaxWidth(),
                hint = stringResource(R.string.remembrances_search_hint),
                onQueryChange = viewModel::onSearchTextChange
            )

            RemembrancesList(
                remembrances = state.remembrances,
                onItemClick = viewModel::onItemClick,
                onFavoriteClick = viewModel::onFavoriteCLick
            )
        }
    }
}

@Composable
private fun RemembrancesList(
    remembrances: List<RemembrancesItem>,
    onItemClick: (RemembrancesItem) -> Unit,
    onFavoriteClick: (RemembrancesItem) -> Unit
) {
    val dims = MaterialTheme.dimensions

    // Empty favorites or a search with no results
    if (remembrances.isEmpty()) {
        Text(
            text = stringResource(R.string.no_matches),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.spaceXxl),
            style = MaterialTheme.appTypography.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        return
    }

    MyLazyColumn(
        lazyList = {
            items(remembrances, key = { it.id }) { remembrance ->
                RemembranceRow(
                    text = remembrance.name,
                    isFavorite = remembrance.isFavorite,
                    onClick = { onItemClick(remembrance) },
                    onFavoriteClick = { onFavoriteClick(remembrance) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dims.spaceLg),
                    thickness = dims.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    )
}

/** Flat row with a divider, matching the Quran suras and reciters lists. */
@Composable
private fun RemembranceRow(
    text: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = dims.spaceLg,
                end = dims.spaceXs,
                top = dims.spaceMd,
                bottom = dims.spaceMd
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.appTypography.title
        )

        MyFavoriteButton(isFavorite = isFavorite, onClick = onFavoriteClick, size = dims.iconMd)
    }
}
