package bassamalim.hidaya.features.books.bookChaptersMenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.models.Book
import bassamalim.hidaya.core.ui.components.CustomSearchBar
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyClickableSurface
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.TabLayout
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlinx.coroutines.flow.Flow

@Composable
fun BookChaptersScreen(viewModel: BookChaptersViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    MyScaffold(title = state.title) { padding ->
        Column(Modifier.padding(padding)) {
            state.continueReading?.let {
                ContinueReadingCard(
                    chapterTitle = it.chapterTitle,
                    onClick = viewModel::onContinueReadingClick
                )
            }

            TabLayout(
                pageNames = listOf(
                    stringResource(R.string.all),
                    stringResource(R.string.favorites)
                ),
                modifier = Modifier.weight(1f),
                searchComponent = {
                    CustomSearchBar(
                        query = state.searchText,
                        modifier = Modifier.fillMaxWidth(),
                        hint = stringResource(R.string.search),
                        onQueryChange = viewModel::onSearchTextChange
                    )
                }
            ) { page ->
                Tab(
                    chaptersFlow = viewModel.getItems(page),
                    onItemClick = viewModel::onItemClick,
                    onFavClick = viewModel::onFavoriteClick
                )
            }
        }
    }
}

@Composable
private fun Tab(
    chaptersFlow: Flow<List<Book.Chapter>>,
    onItemClick: (Book.Chapter) -> Unit,
    onFavClick: (Int) -> Unit
) {
    val chapters by chaptersFlow.collectAsStateWithLifecycle(emptyList())

    MyLazyColumn(
        lazyList = {
            items(chapters, key = { it.id }) { chapter ->
                ItemContainer(chapter = chapter, onItemClick = onItemClick, onFavClick = onFavClick)
            }
        }
    )
}

@Composable
private fun ItemContainer(
    chapter: Book.Chapter,
    onItemClick: (Book.Chapter) -> Unit,
    onFavClick: (Int) -> Unit
) {
    MyClickableSurface(modifier = Modifier.padding(2.dp), onClick = { onItemClick(chapter) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp, start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MyText(
                text = chapter.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp, bottom = 12.dp, start = 20.dp),
                textAlign = TextAlign.Start
            )

            MyFavoriteButton(isFavorite = chapter.isFavorite, onClick = { onFavClick(chapter.id) })
        }
    }
}

@Composable
private fun ContinueReadingCard(chapterTitle: String, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = Modifier.padding(
            horizontal = dims.screenPaddingHorizontal,
            vertical = dims.spaceSm
        ),
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(dims.iconLg)
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceMd)
            ) {
                Text(
                    text = stringResource(R.string.continue_reading),
                    style = MaterialTheme.appTypography.label
                )

                Text(
                    text = chapterTitle,
                    style = MaterialTheme.appTypography.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}
