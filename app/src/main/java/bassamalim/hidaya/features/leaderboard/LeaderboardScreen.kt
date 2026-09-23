package bassamalim.hidaya.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.ui.components.ErrorScreen
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.PaginatedLazyColumn
import bassamalim.hidaya.core.ui.components.TabLayout
import bassamalim.hidaya.core.ui.theme.Bronze
import bassamalim.hidaya.core.ui.theme.Gold
import bassamalim.hidaya.core.ui.theme.Silver
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import kotlinx.collections.immutable.toPersistentList

private val RankBadgeSize = 40.dp

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MyScaffold(title = stringResource(R.string.leaderboard)) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) LoadingScreen()
            else if (state.isError)
                ErrorScreen(message = stringResource(R.string.error_fetching_data))
            else UsersList(
                userId = state.userId,
                userRankMap = state.userRanks,
                userRankIntMap = state.userRankInts,
                ranksMap = state.ranks,
                isLoadingItems = state.isLoadingItems,
                loadMoreItems = viewModel::loadMore,
                numeralsLanguage = viewModel.numeralsLanguage
            )
        }
    }
}

@Composable
private fun UsersList(
    userId: String,
    userRankMap: Map<RankType, String>,
    userRankIntMap: Map<RankType, Int>,
    ranksMap: Map<RankType, List<Pair<String, String>>>,
    isLoadingItems: Map<RankType, Boolean>,
    loadMoreItems: (RankType) -> Unit,
    numeralsLanguage: Language
) {
    TabLayout(
        pageNames = listOf(
            stringResource(R.string.by_reading),
            stringResource(R.string.by_listening)
        )
    ) { page ->
        val rankBy = RankType.entries[page]
        val userRank = userRankMap[rankBy] ?: "--"
        val userRankInt = userRankIntMap[rankBy] ?: -1
        val ranks = ranksMap[rankBy] ?: emptyList()

        MyColumn {
            UserRankCard(userId = userId, userRank = userRank, userRankInt = userRankInt)

            UsersList(
                items = ranks,
                rankType = rankBy,
                listState = rememberLazyListState(),
                loadMoreItems = { loadMoreItems(rankBy) },
                isLoading = isLoadingItems[rankBy] ?: false,
                numeralsLanguage = numeralsLanguage
            )
        }
    }
}

@Composable
private fun UserRankCard(userId: String, userRank: String, userRankInt: Int) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    MyCard(
        modifier = Modifier.padding(dims.spaceLg),
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = contentColor
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankBadge(rankText = userRank, rank = userRankInt)

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceMd)
            ) {
                Text(
                    text = stringResource(R.string.your_position),
                    style = MaterialTheme.appTypography.label,
                    color = contentColor.copy(alpha = 0.75f)
                )

                Text(
                    text = "${stringResource(R.string.user)} $userId",
                    style = MaterialTheme.appTypography.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UsersList(
    items: List<Pair<String, String>>,
    rankType: RankType,
    listState: LazyListState,
    isLoading: Boolean,
    loadMoreItems: () -> Unit,
    numeralsLanguage: Language
) {
    PaginatedLazyColumn(
        items = items.toPersistentList(),
        loadMoreItems = loadMoreItems,
        listState = listState,
        isLoading = isLoading,
        key = { _, item -> item.first },
        itemComponent = { index, item -> ItemCard(item, index+1, rankType, numeralsLanguage) }
    )
}

@Composable
private fun ItemCard(
    item: Pair<String, String>,
    rank: Int,
    rankType: RankType,
    numeralsLanguage: Language
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spaceLg, vertical = dims.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(rankText = translateNums(rank.toString(), numeralsLanguage), rank = rank)

        Text(
            text = "${stringResource(R.string.user)} ${item.first}",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dims.spaceMd),
            style = MaterialTheme.appTypography.title.copy(
                fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = when (rankType) {
                RankType.BY_READING -> "${item.second} ${stringResource(R.string.pages)}"
                RankType.BY_LISTENING -> item.second
            },
            style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = dims.spaceLg),
        thickness = dims.dividerThickness,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * The rank in a round badge. Medal colors go on the badge rather than the text: gold text on
 * a light background is hard to read.
 */
@Composable
private fun RankBadge(rankText: String, rank: Int) {
    val medal = when (rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> null
    }

    Box(
        modifier = Modifier
            .size(RankBadgeSize)
            .background(
                color = medal?.copy(alpha = 0.25f)
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            )
            .then(
                if (medal == null) Modifier
                else Modifier.border(MaterialTheme.dimensions.borderThick, medal, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rankText,
            style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

