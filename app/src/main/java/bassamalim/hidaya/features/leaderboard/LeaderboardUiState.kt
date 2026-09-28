package bassamalim.hidaya.features.leaderboard

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val userId: String = "",
    val userRanks: Map<RankType, String> = emptyMap(),
    val userRankInts: Map<RankType, Int> = emptyMap(),
    val ranks: Map<RankType, List<RankItem>> = emptyMap(),
    val isLoadingItems: Map<RankType, Boolean> = mapOf(
        RankType.BY_READING to false,
        RankType.BY_LISTENING to false
    )
)

/** A row in a ranking, with display-ready (translated) [userId] and [value]. */
data class RankItem(
    val userId: String,
    val value: String,
    val rank: Int
)
