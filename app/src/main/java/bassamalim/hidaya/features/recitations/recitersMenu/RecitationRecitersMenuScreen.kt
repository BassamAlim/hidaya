package bassamalim.hidaya.features.recitations.recitersMenu

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.ui.components.CustomSearchBar
import bassamalim.hidaya.core.ui.components.MyDownloadButton
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyIconPlayerButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.TabLayout
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.quran.surasMenu.RecitationInfo
import kotlinx.coroutines.flow.Flow

private val AvatarSize = 40.dp
private val PlaybackBarHeight = 68.dp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecitationRecitersMenuScreen(viewModel: RecitationRecitersMenuViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!

    if (state.isLoading) return

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(activity)
        onDispose(viewModel::onStop)
    }

    MyScaffold(
        title = stringResource(R.string.recitations),
        onBack = viewModel::onBackPressed
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            TabLayout(
                pageNames = listOf(
                    stringResource(R.string.all),
                    stringResource(R.string.favorites),
                    stringResource(R.string.downloaded)
                ),
                searchComponent = {
                    SearchRow(
                        query = viewModel.searchText,
                        isFiltered = state.isFiltered,
                        onQueryChange = viewModel::onSearchTextChange,
                        onFilterClick = viewModel::onFilterClick
                    )
                }
            ) { page ->
                Tab(
                    itemsFlow = viewModel.getItems(page),
                    expandedReciterIds = state.expandedReciterIds,
                    numeralsLanguage = state.numeralsLanguage,
                    onReciterExpandToggle = viewModel::onReciterExpandToggle,
                    onFavoriteClick = viewModel::onFavoriteClick,
                    onNarrationClick = viewModel::onNarrationClick,
                    onDownloadNarrationClick = viewModel::onDownloadNarrationClick
                )
            }

            PlaybackBar(
                recitationInfo = state.playbackRecitationInfo,
                playbackState = state.playbackState,
                onContinueListeningClick = viewModel::onContinueListeningClick,
                onPlayPauseClick = viewModel::onPlayPauseClick
            )
        }
    }
}

@Composable
private fun SearchRow(
    query: String,
    isFiltered: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    // The icon button's own 12dp padding brings the icon onto the screen's 16dp gutter
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = MaterialTheme.dimensions.spaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomSearchBar(
            query = query,
            hint = stringResource(R.string.reciters_search_hint),
            modifier = Modifier.weight(1F),
            onQueryChange = onQueryChange
        )

        IconButton(onClick = onFilterClick) {
            BadgedBox(badge = { if (isFiltered) Badge() }) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = stringResource(R.string.filter_search_description),
                    tint =
                        if (isFiltered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Tab(
    itemsFlow: Flow<List<Recitation>>,
    expandedReciterIds: Set<Int>,
    numeralsLanguage: Language,
    onReciterExpandToggle: (Int) -> Unit,
    onFavoriteClick: (Int, Boolean) -> Unit,
    onNarrationClick: (Int, Int) -> Unit,
    onDownloadNarrationClick: (Int, Recitation.Narration, String) -> Unit
) {
    val items by itemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    if (items.isEmpty()) {
        EmptyState()
        return
    }

    MyLazyColumn(
        lazyList = {
            items(items = items, key = { it.reciterId }) { item ->
                Column(Modifier.animateItem()) {
                    ReciterItem(
                        reciter = item,
                        isExpanded = item.reciterId in expandedReciterIds,
                        numeralsLanguage = numeralsLanguage,
                        onExpandToggle = { onReciterExpandToggle(item.reciterId) },
                        onFavoriteClick = onFavoriteClick,
                        onNarrationClick = onNarrationClick,
                        onDownloadNarrationClick = onDownloadNarrationClick
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.spaceLg),
                        thickness = MaterialTheme.dimensions.dividerThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Room for the playback bar so it never covers the last reciter
            item {
                Spacer(Modifier.height(PlaybackBarHeight + MaterialTheme.dimensions.spaceXl))
            }
        }
    )
}

@Composable
private fun EmptyState() {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(dims.iconXl * 1.5f),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(Modifier.height(dims.spaceLg))

        Text(
            text = stringResource(R.string.no_recitations_found),
            style = MaterialTheme.appTypography.body,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ReciterItem(
    reciter: Recitation,
    isExpanded: Boolean,
    numeralsLanguage: Language,
    onExpandToggle: () -> Unit,
    onFavoriteClick: (Int, Boolean) -> Unit,
    onNarrationClick: (Int, Int) -> Unit,
    onDownloadNarrationClick: (Int, Recitation.Narration, String) -> Unit
) {
    val dims = MaterialTheme.dimensions
    val suraString = stringResource(R.string.sura)
    // Most reciters have a single narration, so it's opened and downloaded from the row itself
    val singleNarration = reciter.narrations.values.singleOrNull()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (singleNarration != null)
                        onNarrationClick(reciter.reciterId, singleNarration.id)
                    else onExpandToggle()
                }
                .padding(
                    start = dims.spaceLg,
                    end = dims.spaceXs,
                    top = dims.spaceMd,
                    bottom = dims.spaceMd
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(name = reciter.reciterName)

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceMd)
            ) {
                Text(
                    text = reciter.reciterName,
                    style = MaterialTheme.appTypography.title
                )

                Text(
                    text = singleNarration?.name ?: pluralStringResource(
                        R.plurals.narrations_count,
                        reciter.narrations.size,
                        translateNums(
                            string = reciter.narrations.size.toString(),
                            numeralsLanguage = numeralsLanguage
                        )
                    ),
                    style = MaterialTheme.appTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MyFavoriteButton(
                isFavorite = reciter.isFavoriteReciter,
                onClick = { onFavoriteClick(reciter.reciterId, reciter.isFavoriteReciter) },
                size = dims.iconMd
            )

            if (singleNarration != null) {
                DownloadButton(
                    narration = singleNarration,
                    onClick = {
                        onDownloadNarrationClick(reciter.reciterId, singleNarration, suraString)
                    }
                )
            }
            else {
                ExpandArrow(isExpanded = isExpanded)
            }
        }

        if (singleNarration == null) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(150)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
            ) {
                Column(Modifier.padding(bottom = dims.spaceSm)) {
                    reciter.narrations.values.forEach { narration ->
                        NarrationItem(
                            narration = narration,
                            numeralsLanguage = numeralsLanguage,
                            onClick = { onNarrationClick(reciter.reciterId, narration.id) },
                            onDownloadClick = {
                                onDownloadNarrationClick(reciter.reciterId, narration, suraString)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.toString().orEmpty(),
            style = MaterialTheme.appTypography.headline,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ExpandArrow(isExpanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "expandArrowRotation"
    )

    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = stringResource(R.string.expand),
        modifier = Modifier
            .padding(MaterialTheme.dimensions.spaceMd)
            .size(MaterialTheme.dimensions.iconMd)
            .rotate(rotation),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NarrationItem(
    narration: Recitation.Narration,
    numeralsLanguage: Language,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Lines the narration up under the reciter's name
            .padding(
                start = dims.spaceLg + AvatarSize + dims.spaceMd,
                end = dims.spaceXs,
                top = dims.spaceSm,
                bottom = dims.spaceSm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = narration.name, style = MaterialTheme.appTypography.body)

            Text(
                text = pluralStringResource(
                    R.plurals.suras_count,
                    narration.availableSuras.size,
                    translateNums(
                        string = narration.availableSuras.size.toString(),
                        numeralsLanguage = numeralsLanguage
                    )
                ),
                style = MaterialTheme.appTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DownloadButton(narration = narration, onClick = onDownloadClick)
    }
}

@Composable
private fun DownloadButton(narration: Recitation.Narration, onClick: () -> Unit) {
    AnimatedContent(
        targetState = narration.downloadState,
        label = "downloadButtonState",
        transitionSpec = {
            scaleIn(animationSpec = tween(200)) togetherWith scaleOut(animationSpec = tween(200))
        }
    ) { state ->
        MyDownloadButton(
            state = state,
            iconSize = MaterialTheme.dimensions.iconMd,
            onClick = onClick
        )
    }
}

@Composable
private fun BoxScope.PlaybackBar(
    recitationInfo: RecitationInfo?,
    playbackState: PlaybackStatus,
    onContinueListeningClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    AnimatedVisibility(
        visible = recitationInfo != null,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
    ) {
        if (recitationInfo == null) return@AnimatedVisibility

        Surface(
            onClick = onContinueListeningClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = dims.spaceMd, end = dims.spaceMd, bottom = dims.spaceMd)
                .height(PlaybackBarHeight),
            shape = RoundedCornerShape(dims.radiusLg),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = dims.elevationLg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = dims.spaceSm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    MyIconPlayerButton(
                        state = playbackState,
                        onClick = onPlayPauseClick,
                        iconSize = dims.iconLg,
                        filled = false,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = dims.spaceMd)
                ) {
                    Text(
                        text = "${stringResource(R.string.sura)} ${recitationInfo.suraName}",
                        style = MaterialTheme.appTypography.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${stringResource(R.string.for_reciter)} " +
                                recitationInfo.reciterName,
                        style = MaterialTheme.appTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.continue_listening),
                    modifier = Modifier.size(dims.iconMd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
