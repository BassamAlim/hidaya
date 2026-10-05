package bassamalim.hidaya.features.quran.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyIconButton
import bassamalim.hidaya.core.ui.components.MyIconPlayerButton
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.tutorial.TutorialOverlay
import bassamalim.hidaya.core.ui.components.tutorial.TutorialStep
import bassamalim.hidaya.core.ui.components.tutorial.rememberTutorialState
import bassamalim.hidaya.core.ui.components.tutorial.tutorialTarget
import bassamalim.hidaya.core.ui.theme.Bookmark1Color
import bassamalim.hidaya.core.ui.theme.Bookmark2Color
import bassamalim.hidaya.core.ui.theme.Bookmark3Color
import bassamalim.hidaya.core.ui.theme.Bookmark4Color
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.features.quran.surasMenu.BookmarkItem
import kotlinx.coroutines.delay

private const val BARS_AUTO_HIDE_MILLIS = 5000L

@Composable
fun QuranReaderScreen(viewModel: QuranReaderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!
    val configuration = LocalConfiguration.current

    if (state.isLoading) return LoadingScreen()

    val pagerState = rememberPagerState(
        initialPage = viewModel.pageNum - 1,
        pageCount = { Globals.NUM_OF_QURAN_PAGES }
    )

    DisposableEffect(key1 = viewModel) {
        onDispose { viewModel.onStop() }
    }

    LaunchedEffect(state.navigateToPage) {
        state.navigateToPage?.let { page ->
            pagerState.scrollToPage(page)
            viewModel.onNavigateToPageConsumed()
        }
    }

    EnforcePortrait(activity)

    if (state.keepScreenOn) {
        KeepScreenOn(activity)
    }

    var barsVisible by remember { mutableStateOf(false) }
    // Bumped by page taps and by any bar button, restarting the auto-hide countdown
    var lastInteraction by remember { mutableIntStateOf(0) }
    val onInteraction: () -> Unit = {
        barsVisible = true
        lastInteraction++
    }
    val isPlaying = state.playerState == PlaybackStatus.PLAYING

    LaunchedEffect(barsVisible, lastInteraction, isPlaying, state.isBookmarksSheetShown) {
        // Stays up while listening or picking a bookmark: that's when the controls are needed
        if (barsVisible && !isPlaying && !state.isBookmarksSheetShown) {
            delay(BARS_AUTO_HIDE_MILLIS)
            barsVisible = false
        }
    }

    val tutorialState = rememberTutorialState()
    val verseTip = stringResource(R.string.reader_tutorial_verse)
    val playTip = stringResource(R.string.reader_tutorial_play)
    val reciterTip = stringResource(R.string.reader_tutorial_reciter)
    LaunchedEffect(state.isTutorialActive) {
        if (state.isTutorialActive) {
            tutorialState.start(
                steps = listOf(
                    TutorialStep(text = verseTip, targetKey = "reader_content"),
                    TutorialStep(text = playTip, targetKey = "reader_play"),
                    TutorialStep(text = reciterTip)
                ),
                onFinished = viewModel::onTutorialFinished
            )
        }
    }

    // The reader draws straight on the window with no Surface to set a content color, so
    // text without an explicit color would default to black, unreadable in dark mode
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
            TopBar(
                suraName = state.suraName,
                pageNumText = state.pageNum,
                juzNumText = state.juzNum
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .tutorialTarget(tutorialState, "reader_content")
            ) {
                PageContent(
                    viewType = state.viewType,
                    fillPage = state.fillPage,
                    selectedVerse = state.selectedVerse,
                    trackedVerseId = state.trackedVerseId,
                    textSize = state.textSize.toInt(),
                    language = viewModel.language,
                    scrollTo = viewModel.scrollTo,
                    onScrolled = viewModel::onScrolled,
                    pagerState = pagerState,
                    scrollToVersePosition = state.scrollToVersePosition,
                    onScrollToVerseConsumed = viewModel::onScrollToVerseConsumed,
                    onPageChange = viewModel::onPageChange,
                    buildPage = viewModel.pageBuilder::buildPage,
                    buildListPage = viewModel.pageBuilder::buildListPage,
                    onSuraHeaderGloballyPositioned = viewModel::onSuraHeaderGloballyPositioned,
                    onVerseGloballyPositioned = viewModel::onVerseGloballyPositioned,
                    onVersePointerInput = viewModel::onVersePointerInput,
                    onContentTap = onInteraction,
                    configuration = configuration
                )
            }

            AnimatedBottomBar(
                visible = barsVisible || state.isTutorialActive,
                playerState = state.playerState,
                onBookmarksClick = {
                    onInteraction()
                    viewModel.onBookmarksClick()
                },
                onPreviousVerseClick = {
                    onInteraction()
                    viewModel.onPreviousVerseClick()
                },
                onPlayPauseClick = {
                    onInteraction()
                    viewModel.onPlayPauseClick(activity)
                },
                onNextVerseClick = {
                    onInteraction()
                    viewModel.onNextVerseClick()
                },
                onSettingsClick = viewModel::onSettingsClick,
                playButtonModifier = Modifier.tutorialTarget(tutorialState, "reader_play")
            )
            }

            TutorialOverlay(state = tutorialState)
        }
    }

    if (state.isBookmarksSheetShown) {
        BookmarksSheet(
            bookmarks = state.bookmarks,
            onBookmarkClick = viewModel::onBookmarkClick,
            onDismiss = viewModel::onBookmarksSheetDismiss
        )
    }
}

@Composable
private fun TopBar(suraName: String, pageNumText: String, juzNumText: String) {
    // Follows the user's font size; labels shrink to fit a narrow screen instead of clipping
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // A plain Row gets no content color from a Surface or app bar, so set it explicitly
        val color = MaterialTheme.colorScheme.onSurface

        // Sura name
        MyText(
            text = "${stringResource(R.string.sura)} $suraName",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start,
            maxLines = 1,
            minFontSize = 12.sp
        )

        // Page number
        MyText(
            text = "${stringResource(R.string.page)} $pageNumText",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            minFontSize = 12.sp
        )

        // Juz number
        MyText(
            text = "${stringResource(R.string.juz)} $juzNumText",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            minFontSize = 12.sp
        )
    }
}

@Composable
private fun AnimatedBottomBar(
    visible: Boolean,
    playerState: PlaybackStatus,
    onBookmarksClick: () -> Unit,
    onPreviousVerseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextVerseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    playButtonModifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        BottomBar(
            playerState = playerState,
            onBookmarksClick = onBookmarksClick,
            onPreviousVerseClick = onPreviousVerseClick,
            onPlayPauseClick = onPlayPauseClick,
            onNextVerseClick = onNextVerseClick,
            onSettingsClick = onSettingsClick,
            playButtonModifier = playButtonModifier
        )
    }
}

@Composable
private fun BottomBar(
    playerState: PlaybackStatus,
    onBookmarksClick: () -> Unit,
    onPreviousVerseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextVerseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    playButtonModifier: Modifier = Modifier
) {
    BottomAppBar(Modifier.height(56.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MyIconButton(
                imageVector = Icons.Default.Bookmarks,
                description = stringResource(R.string.bookmark_verse_button_description),
                iconModifier = Modifier
                    .size(36.dp)
                    .padding(2.dp),
                onClick = onBookmarksClick
            )

            Row {
                // Skip to previous button
                MyIconButton(
                    // The two glyphs mirror each other, so swapping them flips the icon in RTL
                    imageVector =
                        if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Default.SkipNext
                        else Icons.Default.SkipPrevious,
                    description = stringResource(R.string.rewind_btn_description),
                    iconModifier = Modifier.size(40.dp),
                    onClick = onPreviousVerseClick
                )

                // Play/Pause button
                Box (playButtonModifier.padding(horizontal = 4.dp)) {
                    MyIconPlayerButton(
                        state = playerState,
                        onClick = { onPlayPauseClick() },
                        iconSize = 40.dp,
                        filled = false
                    )
                }

                // Skip to next button
                MyIconButton(
                    imageVector =
                        if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Default.SkipPrevious
                        else Icons.Default.SkipNext,
                    description = stringResource(R.string.fast_forward_btn_description),
                    iconModifier = Modifier.size(40.dp),
                    onClick = onNextVerseClick
                )
            }

            // Preference button
            MyIconButton(
                imageVector = Icons.Default.DisplaySettings,
                description = stringResource(R.string.settings),
                iconModifier = Modifier
                    .size(40.dp)
                    .padding(2.dp),
                onClick = onSettingsClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarksSheet(
    bookmarks: List<BookmarkItem>,
    onBookmarkClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val colors = listOf(Bookmark1Color, Bookmark2Color, Bookmark3Color, Bookmark4Color)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.bookmarked_verses),
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceSm
            ),
            style = MaterialTheme.appTypography.headline
        )

        Text(
            text = stringResource(R.string.bookmarks_hint),
            modifier = Modifier.padding(horizontal = dims.screenPaddingHorizontal),
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (bookmarks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_bookmarked_page),
                modifier = Modifier.padding(
                    horizontal = dims.screenPaddingHorizontal,
                    vertical = dims.spaceLg
                ),
                style = MaterialTheme.appTypography.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        bookmarks.forEach { bookmark ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBookmarkClick(bookmark.verseId) }
                    .padding(
                        horizontal = dims.screenPaddingHorizontal,
                        vertical = dims.spaceMd
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = colors[bookmark.index]
                )

                Spacer(Modifier.width(dims.spaceMd))

                Text(
                    text = stringResource(
                        R.string.bookmark_label,
                        bookmark.suraName,
                        bookmark.verseNumText
                    ),
                    style = MaterialTheme.appTypography.title
                )
            }
        }

        Spacer(Modifier.height(dims.spaceLg))
    }
}

@Composable
private fun EnforcePortrait(activity: Activity) {
    DisposableEffect(Unit) {
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}

@Composable
private fun KeepScreenOn(activity: Activity) {
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
