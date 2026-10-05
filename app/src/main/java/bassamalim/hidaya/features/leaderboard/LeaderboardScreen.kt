package bassamalim.hidaya.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.ui.components.ErrorScreen
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.PaginatedLazyColumn
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

/** The tile for each ranking: a short name and an icon. */
private val RankType.titleRes get() = when (this) {
    RankType.BY_READING -> R.string.rank_reading
    RankType.BY_LISTENING -> R.string.rank_listening
    RankType.BY_VERSE_GUESS_POINTS -> R.string.verse_guess_title
    RankType.BY_QUIZ_LEARNED -> R.string.rank_quiz
}

private val RankType.icon get() = when (this) {
    RankType.BY_READING -> Icons.AutoMirrored.Default.MenuBook
    RankType.BY_LISTENING -> Icons.Default.Headphones
    RankType.BY_VERSE_GUESS_POINTS -> Icons.Default.TravelExplore
    RankType.BY_QUIZ_LEARNED -> Icons.AutoMirrored.Default.FactCheck
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
    var rankBy by rememberSaveable { mutableStateOf(RankType.BY_READING) }

    Column(Modifier.fillMaxSize()) {
        RankSelector(selected = rankBy, onSelect = { rankBy = it })

        // Says what the numbers below measure, which the tile name alone doesn't
        Text(
            text = stringResource(
                when (rankBy) {
                    RankType.BY_READING -> R.string.rank_by_reading_desc
                    RankType.BY_LISTENING -> R.string.rank_by_listening_desc
                    RankType.BY_VERSE_GUESS_POINTS -> R.string.rank_by_verse_guess_points_desc
                    RankType.BY_QUIZ_LEARNED -> R.string.rank_by_quiz_learned_desc
                }
            ),
            modifier = Modifier.padding(
                start = MaterialTheme.dimensions.spaceLg,
                end = MaterialTheme.dimensions.spaceLg,
                top = MaterialTheme.dimensions.spaceMd
            ),
            style = MaterialTheme.appTypography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        UserRankCard(
            userId = userId,
            userRank = userRankMap[rankBy] ?: "--",
            userRankInt = userRankIntMap[rankBy] ?: -1
        )

        // Keyed so switching rankings starts the new one from its top
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

/** Equal tiles, icon over name, so every ranking is visible and none scrolls out of view. */
@Composable
private fun RankSelector(selected: RankType, onSelect: (RankType) -> Unit) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(start = dims.spaceLg, end = dims.spaceLg, top = dims.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)
    ) {
        RankType.entries.forEach { rankType ->
            val isSelected = rankType == selected

            Surface(
                selected = isSelected,
                onClick = { onSelect(rankType) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(dims.radiusMd),
                color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor =
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = dims.spaceXs, vertical = dims.spaceSm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = rankType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(dims.iconMd)
                    )

                    Spacer(Modifier.height(dims.spaceXs))

                    Text(
                        text = stringResource(rankType.titleRes),
                        style = MaterialTheme.appTypography.label.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRankCard(userId: String, userRank: String, userRankInt: Int) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    MyCard(
        modifier = Modifier.padding(
            start = dims.spaceLg,
            end = dims.spaceLg,
            top = dims.spaceSm,
            bottom = dims.spaceLg
        ),
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
                RankType.BY_LISTENING, RankType.BY_QUIZ_LEARNED -> item.value
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

