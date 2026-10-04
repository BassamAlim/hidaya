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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    ranksMap: Map<RankType, List<RankItem>>,
    isLoadingItems: Map<RankType, Boolean>,
    loadMoreItems: (RankType) -> Unit,
    numeralsLanguage: Language
) {
    @Composable
    fun Page(rankBy: RankType, header: @Composable () -> Unit = {}) {
        MyColumn {
            header()

            UserRankCard(
                userId = userId,
                userRank = userRankMap[rankBy] ?: "--",
                userRankInt = userRankIntMap[rankBy] ?: -1
            )

            // Keyed so switching rankings on a page starts the new one from its top
            key(rankBy) {
                UsersList(
                    items = ranksMap[rankBy] ?: emptyList(),
                    rankType = rankBy,
                    listState = rememberLazyListState(),
                    loadMoreItems = { loadMoreItems(rankBy) },
                    isLoading = isLoadingItems[rankBy] ?: false,
                    numeralsLanguage = numeralsLanguage
                )
            }
        }
    }

    TabLayout(
        pageNames = listOf(
            stringResource(R.string.by_reading),
            stringResource(R.string.by_listening),
            stringResource(R.string.verse_guess_title),
            stringResource(R.string.quiz_title)
        )
    ) { page ->
        when (page) {
            0 -> Page(RankType.BY_READING)
            1 -> Page(RankType.BY_LISTENING)
            3 -> Page(RankType.BY_QUIZ_LEARNED)
            else -> {
                var rankBy by rememberSaveable { mutableStateOf(RankType.BY_VERSE_GUESS_POINTS) }

                Page(rankBy) {
                    VerseGuessRankSwitch(selected = rankBy, onSelect = { rankBy = it })
                }
            }
        }
    }
}

/** The where's-the-verse page ranks by lifetime points or by best streak. */
@Composable
private fun VerseGuessRankSwitch(selected: RankType, onSelect: (RankType) -> Unit) {
    val dims = MaterialTheme.dimensions
    val options = listOf(
        RankType.BY_VERSE_GUESS_POINTS to stringResource(R.string.verse_guess_rank_points),
        RankType.BY_VERSE_GUESS_STREAK to stringResource(R.string.verse_guess_rank_streak)
    )

    SingleChoiceSegmentedButtonRow(
        Modifier
            .fillMaxWidth()
            .padding(start = dims.spaceLg, end = dims.spaceLg, top = dims.spaceLg)
    ) {
        options.forEachIndexed { index, (rankType, label) ->
            SegmentedButton(
                selected = rankType == selected,
                onClick = { onSelect(rankType) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = label, style = MaterialTheme.appTypography.button)
            }
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
    items: List<RankItem>,
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
        key = { _, item -> item.userId },
        itemComponent = { _, item -> ItemCard(item, rankType, numeralsLanguage) }
    )
}

@Composable
private fun ItemCard(
    item: RankItem,
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
        RankBadge(
            rankText = translateNums(item.rank.toString(), numeralsLanguage),
            rank = item.rank
        )

        Text(
            text = "${stringResource(R.string.user)} ${item.userId}",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dims.spaceMd),
            style = MaterialTheme.appTypography.title.copy(
                fontWeight = if (item.rank <= 3) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = when (rankType) {
                RankType.BY_READING -> "${item.value} ${stringResource(R.string.pages)}"
                RankType.BY_LISTENING, RankType.BY_VERSE_GUESS_STREAK,
                RankType.BY_QUIZ_LEARNED -> item.value
                RankType.BY_VERSE_GUESS_POINTS ->
                    stringResource(R.string.verse_guess_score, item.value)
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

