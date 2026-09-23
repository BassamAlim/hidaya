package bassamalim.hidaya.features.recitations.surasMenu

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.models.Recitation
import bassamalim.hidaya.core.models.ReciterSura
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.features.recitations.RecitationMediaId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecitationSurasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: RecitationSurasMenuDomain,
    private val navigator: Navigator
): ViewModel() {

    private val reciterId = savedStateHandle.get<Int>("reciter_id") ?: 0
    private val narrationId = savedStateHandle.get<Int>("narration_id") ?: 0

    private lateinit var language: Language
    private lateinit var narration: Recitation.Narration
    private lateinit var decoratedSuraNames: List<String>
    private lateinit var plainSuraNames: List<String>

    private val _uiState = MutableStateFlow(RecitationSurasUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = RecitationSurasUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            plainSuraNames = domain.getPlainSuraNames()
            decoratedSuraNames = domain.getDecoratedSuraNames(language)
            narration = domain.getNarration(reciterId, narrationId, language)

            _uiState.update { it.copy(
                isLoading = false,
                title = domain.getReciterName(reciterId, language)
            )}
        }
    }

    fun onStart() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(
                downloadStates = domain.getDownloadStates(reciterId, narrationId)
            )}
        }

        domain.registerDownloadReceiver(
            reciterId = reciterId,
            narrationId = narrationId,
            setAsDownloaded = { suraId ->
                _uiState.update { it.copy(
                    downloadStates = it.downloadStates.toMutableMap().apply {
                        this[suraId] = DownloadState.DOWNLOADED
                    }
                )}
            },
            setDownloadStates = { downloadStates ->
                _uiState.update { it.copy(
                    downloadStates = downloadStates
                )}
            }
        )
    }

    fun onStop() {
        domain.unregisterDownloadReceiver()
    }

    fun onBackPressed() {
        val context = navigator.getContext()

        if ((context as Activity).isTaskRoot) {
            navigator.navigate(Screen.RecitationsRecitersMenu) {
                popUpTo(
                    Screen.RecitationSurasMenu(
                        reciterId = reciterId.toString(),
                        narrationId = narrationId.toString()
                    ).route
                ) {
                    inclusive = true
                }
            }
        }
        else (context as ComponentActivity).onBackPressedDispatcher.onBackPressed()
    }

    fun getItems(page: Int): Flow<List<ReciterSura>> {
        val menuType = MenuType.entries[page]
        val availableSuar = narration.availableSuras

        return domain.observeSuras(reciterId, narrationId, language).map { suras ->
            val items = suras.filter { sura ->
                val suraExists = availableSuar.contains(sura.id+1)
                val isWanted = when (menuType) {
                    MenuType.FAVORITES -> sura.isFavorite
                    MenuType.DOWNLOADED ->
                        _uiState.value.downloadStates[sura.id] == DownloadState.DOWNLOADED
                    else -> true
                }

                suraExists && isWanted
            }

            domain.getSearchResults(query = _uiState.value.searchText, items = items)
        }
    }

    fun onSuraClick(suraId: Int) {
        val mediaId = RecitationMediaId.encode(reciterId, narrationId, suraId)

        navigator.navigate(Screen.RecitationPlayer(action = "start", mediaId = mediaId))

        domain.trackRecitationPlayed(
            reciterId = reciterId,
            narrationId = narrationId,
            suraName = plainSuraNames[suraId]
        )
    }

    fun onFavoriteClick(sura: ReciterSura) {
        viewModelScope.launch {
            domain.setFavoriteStatus(suraId = sura.id, value = !sura.isFavorite)
        }
    }

    fun onDownloadClick(sura: ReciterSura) {
        if (_uiState.value.downloadStates[sura.id] == DownloadState.NOT_DOWNLOADED) {
            _uiState.update { it.copy(
                downloadStates = it.downloadStates.toMutableMap().apply {
                    this[sura.id] = DownloadState.DOWNLOADING
                }
            )}

            domain.download(
                reciterId = reciterId,
                narrationId = narrationId,
                suraId = sura.id,
                suraSearchName = sura.searchName,
                server = narration.server
            )
        }
        else {
            _uiState.update { it.copy(
                downloadStates = it.downloadStates.toMutableMap().apply {
                    this[sura.id] = DownloadState.NOT_DOWNLOADED
                }
            )}

            domain.delete(reciterId = reciterId, narrationId = narrationId, suraId = sura.id)
        }
    }

    fun onSearchChange(text: String) {
        _uiState.update { it.copy(
            searchText = text
        )}
    }

}