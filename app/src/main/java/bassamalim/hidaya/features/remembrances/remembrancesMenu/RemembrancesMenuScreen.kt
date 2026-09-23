package bassamalim.hidaya.features.remembrances.remembrancesMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.ui.components.CustomSearchBar
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyText

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
    MyLazyColumn(
        lazyList = {
            items(remembrances, key = { it.id }) { remembrance ->
                RemembranceItem(
                    text = remembrance.name,
                    iconButton = {
                        MyFavoriteButton(
                            isFavorite = remembrance.isFavorite,
                            onClick = { onFavoriteClick(remembrance) }
                        )
                    },
                    onClick = { onItemClick(remembrance) }
                )
            }
        }
    )
}

@Composable
fun RemembranceItem(
    text: String,
    modifier: Modifier = Modifier,
    iconButton: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MyText(
                text = text,
                modifier = Modifier
                    .weight(1F)
                    .padding(10.dp),
                fontSize = 18.sp,
                textAlign = TextAlign.Start
            )

            iconButton()
        }
    }
}