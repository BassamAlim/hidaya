package bassamalim.hidaya.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.Response
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val domain: LeaderboardDomain
): ViewModel() {

    lateinit var numeralsLanguage: Language

    /** (user id, value) pairs fetched so far, best first */
    private val rawRanks = mutableMapOf<RankType, List<Pair<Int, Long>>>()

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = LeaderboardUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            numeralsLanguage = domain.getNumeralsLanguage()

            val userRecord = domain.getUserRecord()?.data
            val ranks = domain.getRanks()

            if (userRecord == null || ranks == null || ranks.values.any { it is Response.Error<*> }) {
                _uiState.update { it.copy(
                    isLoading = false,
                    isError = true
                )}
                return@launch
            }

            rawRanks.clear()
            ranks.forEach { (rankType, response) -> rawRanks[rankType] = response.data!! }

            val userRankRaw = domain.getUserRanks(userRecord)

            _uiState.update { it.copy(
                isLoading = false,
                userId = translateNums(userRecord.userId.toString(), numeralsLanguage),
                userRanks = userRankRaw.mapValues { (_, rank) ->
                    translateNums(rank?.toString() ?: "--", numeralsLanguage)
                },
                userRankInts = userRankRaw.mapValues { (_, rank) -> rank ?: -1 },
                ranks = rawRanks.mapValues { (rankType, raw) -> toItems(rankType, raw) }
            )}
        }
    }

    fun loadMore(rankType: RankType) {
        if (_uiState.value.isLoadingItems[rankType] == true || !domain.hasMore(rankType)) return

        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoadingItems = it.isLoadingItems + (rankType to true)
            )}

            val newRanks = domain.getMoreRanks(rankType)?.data.orEmpty()
            // records can change between pages, so a user may come up twice
            val raw = (rawRanks[rankType].orEmpty() + newRanks).distinctBy { it.first }
            rawRanks[rankType] = raw

            _uiState.update { it.copy(
                isLoadingItems = it.isLoadingItems + (rankType to false),
                ranks = it.ranks + (rankType to toItems(rankType, raw))
            )}
        }
    }

    /** Competition ranking (1, 2, 2, 4), matching how the user's own rank is counted. */
    private fun toItems(rankType: RankType, raw: List<Pair<Int, Long>>): List<RankItem> {
        var rank = 0
        return raw.mapIndexed { i, (userId, value) ->
            if (i == 0 || value != raw[i - 1].second) rank = i + 1
            RankItem(
                userId = translateNums(userId.toString(), numeralsLanguage),
                value = when (rankType) {
                    RankType.BY_READING -> translateNums(value.toString(), numeralsLanguage)
                    RankType.BY_LISTENING -> formatRecitationsTime(value)
                },
                rank = rank
            )
        }
    }

    private fun formatRecitationsTime(millis: Long): String {
        val hours = millis / (60 * 60 * 1000)
        val minutes = millis / (60 * 1000) % 60
        val seconds = millis / 1000 % 60

        return translateNums(
            numeralsLanguage = numeralsLanguage,
            string = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        )
    }

}
