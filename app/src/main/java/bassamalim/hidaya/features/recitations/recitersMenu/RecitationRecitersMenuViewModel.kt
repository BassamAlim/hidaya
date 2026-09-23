package bassamalim.hidaya.features.recitations.recitersMenu

import android.app.Activity
import android.os.Build
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_NONE
import android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo
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

    fun onStart(activity: Activity) {
        Log.i(Globals.TAG, "in onStart of RecitationsRecitersViewModel")

        viewModelScope.launch {
            domain.connect(activity = activity, connectionCallbacks = connectionCallbacks)

            domain.registerDownloadReceiver()
        }
    }

    fun onStop() {
        Log.i(Globals.TAG, "in onStop of RecitationsRecitersViewModel")

        domain.stopMediaBrowser(controllerCallback)

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
        if (_uiState.value.playbackState == STATE_NONE)
            return

        if (domain.getState() == STATE_PLAYING) {
            domain.pause()
            _uiState.update { it.copy(
                playbackState = STATE_PAUSED
            )}
        }
        else {
            domain.resume()
            _uiState.update { it.copy(
                playbackState = STATE_PLAYING
            )}
        }
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

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.i(Globals.TAG, "onConnected in RecitationsPlayerViewModel")
            domain.initializeController(controllerCallback)

            updateMetadata(domain.getMetadata())
        }

        override fun onConnectionSuspended() {
            Log.e(Globals.TAG, "Connection suspended in RecitationsPlayerViewModel")
            _uiState.update { it.copy(
                playbackState = STATE_NONE
            )}
        }

        override fun onConnectionFailed() {
            Log.e(Globals.TAG, "Connection failed in RecitationsPlayerViewModel")
            _uiState.update { it.copy(
                playbackState = STATE_NONE
            )}
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            updatePlaybackState(state)
        }

        override fun onSessionDestroyed() {
            domain.disconnectMediaBrowser()
        }
    }

    private fun updateMetadata(metadata: MediaMetadataCompat) {
        if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS) == 0L)
            return

        _uiState.update { it.copy(
            playbackRecitationInfo = RecitationInfo(
                reciterName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "",
                narrationName = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM) ?: "",
                suraName = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: ""
            )
        )}
    }

    private fun updatePlaybackState(state: PlaybackStateCompat) {
        _uiState.update { it.copy(
            playbackState = state.state
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
                recitation.narrations.any { narration ->
                    narrationSelections[narration.value.name]!!
                }
            }.map { recitation ->
                recitation.copy(
                    narrations = recitation.narrations.filter { narration ->
                        narrationSelections[narration.value.name]!!
                    }
                )
            }.filter { recitation -> recitation.narrations.isNotEmpty() }

            domain.getSearchResults(_uiState.value.searchText, selectedItems)
        }
    }

}