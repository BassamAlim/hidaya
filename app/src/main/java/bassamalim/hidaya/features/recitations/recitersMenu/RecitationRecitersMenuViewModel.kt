package bassamalim.hidaya.features.recitations.recitersMenu

import android.app.Activity
import android.os.Build
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.helpers.PlayerConnection
import bassamalim.hidaya.core.helpers.playbackStatus
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo
import bassamalim.hidaya.features.recitations.player.RecitationPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(UnstableApi::class)
@HiltViewModel
class RecitationRecitersMenuViewModel @Inject constructor(
    private val domain: RecitationRecitersMenuDomain,
    private val navigator: Navigator
): ViewModel() {

    private lateinit var allRecitations: Flow<Map<Int, Recitation>>
    private lateinit var suraNames: List<String>
    private lateinit var narrationSelections: Flow<Map<String, Boolean>>
    var searchText by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow(RecitationRecitersMenuUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val language = domain.getLanguage()
            suraNames = domain.getSuraNames(language)
            allRecitations = domain.observeRecitersWithNarrations(language)
            narrationSelections = domain.getNarrationSelections(language)

            val lastPlayed = domain.getLastPlayed().first()
            val numeralsLanguage = domain.getNumeralsLanguage()
            _uiState.update { it.copy(
                isLoading = false,
                numeralsLanguage = numeralsLanguage,
                playbackRecitationInfo = lastPlayed?.let {
                    domain.getLastPlayedMedia(lastPlayed.mediaId)
                },
                isFiltered = narrationSelections.first().values.any { bool -> !bool }
            )}

            domain.cleanFiles()
        }
    }

    private var connection: PlayerConnection? = null

    fun onStart(activity: Activity) {
        Log.i(Globals.TAG, "in onStart of RecitationsRecitersViewModel")

        connection = PlayerConnection(
            context = activity.applicationContext,
            service = RecitationPlayerService::class.java,
            onChange = ::onPlayerChange
        )
        activity.volumeControlStream = AudioManager.STREAM_MUSIC

        domain.registerDownloadReceiver()
    }

    fun onStop() {
        Log.i(Globals.TAG, "in onStop of RecitationsRecitersViewModel")

        connection?.release()
        connection = null

        domain.unregisterDownloadReceiver()
    }

    fun onBackPressed() {
        val context = navigator.getContext()
        if ((context as Activity).isTaskRoot) {
            navigator.navigate(Screen.Main) {
                popUpTo(Screen.RecitationsRecitersMenu.route) {
                    inclusive = true
                }
            }
        }
        else (context as AppCompatActivity).onBackPressedDispatcher.onBackPressed()
    }

    fun onPlayPauseClick() {
        val controller = connection?.controller
        // Nothing loaded in the player (e.g. after the app restarted): pick up the last played
        if (controller?.currentMediaItem == null) onContinueListeningClick()
        else Util.handlePlayPauseButtonAction(controller)
    }

    fun onContinueListeningClick() {
        viewModelScope.launch {
            val lastPlayed = domain.getLastPlayed().first()
            if (lastPlayed == null) return@launch

            navigator.navigate(
                Screen.RecitationPlayer(
                    action = "continue",
                    mediaId = lastPlayed.mediaId.toString()
                )
            )
        }
    }

    fun onFilterClick() {
        navigator.navigate(Screen.RecitersMenuFilter)
    }

    fun onFavoriteClick(reciterId: Int, oldValue: Boolean) {
        viewModelScope.launch {
            domain.setFavorite(reciterId, !oldValue)
        }
    }

    fun onDownloadNarrationClick(
        reciterId: Int,
        narration: Recitation.Narration,
        suraString: String
    ) {
        viewModelScope.launch {
            if (allRecitations.first()[reciterId]!!.narrations[narration.id]!!.downloadState
                == DownloadState.NOT_DOWNLOADED) {
                domain.downloadNarration(
                    reciterId = reciterId,
                    narration = narration,
                    suraNames = suraNames,
                    suraString = suraString
                )
            }
            else domain.deleteNarration(reciterId, narration)
        }
    }

    fun onNarrationClick(reciterId: Int, narrationId: Int) {
        navigator.navigate(
            Screen.RecitationSurasMenu(
                reciterId = reciterId.toString(),
                narrationId = narrationId.toString()
            )
        )
    }

    // TODO: find a better fix for this
    fun onSearchTextChange(text: String) {
        searchText = text
        _uiState.update { it.copy(
            searchText = text
        )}
    }

    fun onReciterExpandToggle(reciterId: Int) {
        _uiState.update { state ->
            val newExpandedIds = if (state.expandedReciterIds.contains(reciterId)) {
                state.expandedReciterIds - reciterId
            } else {
                state.expandedReciterIds + reciterId
            }
            state.copy(expandedReciterIds = newExpandedIds)
        }
    }

    private fun onPlayerChange(controller: MediaController) {
        val metadata = controller.currentMediaItem?.mediaMetadata

        _uiState.update { it.copy(
            playbackState = controller.playbackStatus(),
            playbackRecitationInfo = metadata?.let { m ->
                RecitationInfo(
                    reciterName = m.artist?.toString() ?: "",
                    narrationName = m.albumTitle?.toString() ?: "",
                    suraName = m.title?.toString() ?: ""
                )
            } ?: it.playbackRecitationInfo
        )}
    }

    fun getItems(page: Int): Flow<List<Recitation>> {
        val menuType = MenuType.entries[page]

        return combine(allRecitations, narrationSelections) { allRecitations, narrationSelections ->
            val items = when (menuType) {
                MenuType.FAVORITES -> {
                    allRecitations.filter { recitation -> recitation.value.isFavoriteReciter }
                }
                MenuType.DOWNLOADED -> {
                    val hasDownloaded = allRecitations.filter { recitation ->
                        recitation.value.narrations.any { narration ->
                            narration.value.downloadState == DownloadState.DOWNLOADED
                        }
                    }
                    hasDownloaded.map { recitation ->
                        recitation.key to recitation.value.copy(
                            narrations = recitation.value.narrations.filter { narration ->
                                narration.value.downloadState == DownloadState.DOWNLOADED
                            }
                        )
                    }.toMap()
                }
                else -> allRecitations
            }

            val selectedItems = items.values.filter { recitation ->
                // Unknown narrations count as selected, like the repository's default
                recitation.narrations.any { narration ->
                    narrationSelections[narration.value.name] != false
                }
            }.map { recitation ->
                recitation.copy(
                    narrations = recitation.narrations.filter { narration ->
                        narrationSelections[narration.value.name] != false
                    }
                )
            }.filter { recitation -> recitation.narrations.isNotEmpty() }

            domain.getSearchResults(_uiState.value.searchText, selectedItems)
        }
    }

}