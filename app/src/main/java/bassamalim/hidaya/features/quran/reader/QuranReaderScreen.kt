package bassamalim.hidaya.features.quran.reader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.support.v4.media.session.PlaybackStateCompat
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.QuranViewType
import bassamalim.hidaya.core.models.Verse
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyHorizontalDivider
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
import bassamalim.hidaya.core.ui.theme.hafs_smart
import bassamalim.hidaya.core.ui.theme.nsp
import bassamalim.hidaya.core.ui.theme.uthmanic_hafs
import bassamalim.hidaya.features.quran.surasMenu.BookmarkItem
import kotlinx.coroutines.delay

private const val BARS_AUTO_HIDE_MILLIS = 5000L

@Composable
fun QuranReaderScreen(viewModel: QuranReaderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current!!
    val configuration = LocalConfiguration.current

    if (state.isLoading) return LoadingScreen()

    val pagerState = rememberPagerState(
        initialPage = viewModel.pageNum - 1,
        pageCount = { Globals.NUM_OF_QURAN_PAGES }
    )

    DisposableEffect(key1 = viewModel) {
        onDispose { viewModel.onStop(activity) }
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
    val isPlaying = state.playerState == PlaybackStateCompat.STATE_PLAYING

    LaunchedEffect(barsVisible, lastInteraction, isPlaying, state.isBookmarksSheetShown) {
        // Stays up while listening or picking a bookmark: that's when the controls are needed
        if (barsVisible && !isPlaying && !state.isBookmarksSheetShown) {
            delay(BARS_AUTO_HIDE_MILLIS)
            barsVisible = false
        }
    }

    val featureNotFoundMessage = stringResource(R.string.feature_not_supported)

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
                buildPage = viewModel::buildPage,
                buildListPage = viewModel::buildListPage,
                onSuraHeaderGloballyPositioned = viewModel::onSuraHeaderGloballyPositioned,
                onVerseGloballyPositioned = viewModel::onVerseGloballyPositioned,
                onVersePointerInput = viewModel::onVersePointerInput,
                onContentTap = onInteraction,
                configuration = configuration
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
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
                viewModel.onPlayPauseClick(
                    activity = activity,
                    snackbarHostState = snackbarHostState,
                    message = featureNotFoundMessage
                )
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

    if (state.isBookmarksSheetShown) {
        BookmarksSheet(
            bookmarks = state.bookmarks,
            onBookmarkClick = viewModel::onBookmarkClick,
            onDismiss = viewModel::onBookmarksSheetDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(suraName: String, pageNumText: String, juzNumText: String) {
    CenterAlignedTopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        title = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sura name
                MyText(
                    text = "${stringResource(R.string.sura)} $suraName",
                    fontSize = 18.nsp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )

                // Page number
                MyText(
                    text = "${stringResource(R.string.page)} $pageNumText",
                    fontSize = 18.nsp,
                    fontWeight = FontWeight.Medium
                )

                // Juz number
                MyText(
                    text = "${stringResource(R.string.juz)} $juzNumText",
                    fontSize = 18.nsp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }
        }
    )
}

@Composable
private fun AnimatedBottomBar(
    visible: Boolean,
    playerState: Int,
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
    playerState: Int,
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
                    iconId = R.drawable.ic_skip_previous,
                    description = stringResource(R.string.rewind_btn_description),
                    iconSize = 40.dp,
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
                    iconId = R.drawable.ic_skip_next,
                    description = stringResource(R.string.fast_forward_btn_description),
                    iconSize = 40.dp,
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
private fun PageContent(
    viewType: QuranViewType,
    fillPage: Boolean,
    selectedVerse: Verse?,
    trackedVerseId: Int,
    textSize: Int,
    language: Language,
    pagerState: PagerState,
    scrollTo: Float,
    onScrolled: () -> Unit,
    scrollToVersePosition: Float?,
    onScrollToVerseConsumed: () -> Unit,
    onPageChange: (Int, Int) -> Unit,
    buildPage: (Int, Int?, Int, Color, Color, Color) -> List<Section>,
    buildListPage: (Int, Int?, Int, Color, Color, Color) -> List<Section>,
    onSuraHeaderGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit,
    onVerseGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit,
    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit,
    onContentTap: () -> Unit,
    configuration: Configuration
) {
    val lineHeight = remember { getLineHeight(configuration) }
    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val selectedVerseId = selectedVerse?.id

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { pageIdx ->
        val isCurrentPage = pageIdx == pagerState.currentPage
        val scrollState = rememberScrollState()

        LaunchedEffect(pagerState.currentPage) {
            onPageChange(pagerState.currentPage, pageIdx)
        }

        if (isCurrentPage) {
            LaunchedEffect(scrollToVersePosition) {
                scrollToVersePosition?.let { position ->
                    scrollState.animateScrollTo(position.toInt())
                    onScrollToVerseConsumed()
                }
            }
        }

        val columnModifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) { detectTapGestures { onContentTap() } }

        Column(
            modifier = columnModifier,
            verticalArrangement =
                if (pageIdx == 0 || pageIdx == 1) Arrangement.Top
                else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (viewType) {
                QuranViewType.PAGE -> {
                    val pageContent = remember(pageIdx, selectedVerseId, trackedVerseId, defaultColor, primaryColor, tertiaryColor) {
                        buildPage(pageIdx + 1, selectedVerseId, trackedVerseId, defaultColor, primaryColor, tertiaryColor)
                    }

                    PageItems(
                        fillPage = fillPage,
                        sections = pageContent,
                        isCurrentPage = isCurrentPage,
                        textSize = textSize,
                        lineHeight = lineHeight,
                        onVersePointerInput = onVersePointerInput,
                        onSuraHeaderGloballyPositioned = onSuraHeaderGloballyPositioned
                    )
                }
                QuranViewType.LIST -> {
                    val pageContent = remember(pageIdx, selectedVerseId, trackedVerseId, defaultColor, primaryColor, tertiaryColor) {
                        buildListPage(pageIdx + 1, selectedVerseId, trackedVerseId, defaultColor, primaryColor, tertiaryColor)
                    }

                    ListItems(
                        sections = pageContent,
                        isCurrentPage = isCurrentPage,
                        selectedVerse = selectedVerse,
                        trackedVerseId = trackedVerseId,
                        textSize = textSize,
                        language = language,
                        onSuraHeaderGloballyPositioned = onSuraHeaderGloballyPositioned,
                        onVerseGloballyPositioned = onVerseGloballyPositioned,
                        onVersePointerInput = onVersePointerInput
                    )
                }
            }

            if (isCurrentPage && scrollTo > 0f) {
                LaunchedEffect(scrollTo) {
                    scrollState.animateScrollTo(scrollTo.toInt())
                    onScrolled()
                }
            }
        }
    }
}

@Composable
private fun PageItems(
    fillPage: Boolean,
    sections: List<Section>,
    isCurrentPage: Boolean,
    textSize: Int,
    lineHeight: Dp,
    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit,
    onSuraHeaderGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit
) {
    for (section in sections) {
        when (section) {
            is SuraHeaderSection -> {
                SuraHeader(
                    suraNum = section.suraNum,
                    suraName = section.suraName,
                    isCurrentPage = isCurrentPage,
                    textSize = textSize,
                    onGloballyPositioned = onSuraHeaderGloballyPositioned
                )
            }
            is BasmalahSection -> {
                Basmalah(textSize)
            }
            is VersesSection -> {
                PageViewScreen(
                    annotatedString = section.annotatedString,
                    textSize = textSize,
                    onVersePointerInput = onVersePointerInput
                )
            }
        }

//        if (fillPage) {
//            when (section) {
//                is SuraHeaderSection -> {
//                    SuraHeader(
//                        suraNum = section.suraNum,
//                        suraName = section.suraName,
//                        isCurrentPage = isCurrentPage,
//                        textSize = textSize,
//                        height = lineHeight,
//                        onGloballyPositioned = onSuraHeaderGloballyPositioned
//                    )
//                }
//                is BasmalahSection -> {
//                    Basmalah(textSize = textSize, height = lineHeight)
//                }
//                is VersesSection -> {
//                    FilledPageViewScreen(
//                        annotatedString = section.annotatedString,
//                        numOfLines =
//                            if (section.suraNum == 1) section.numOfLines-1
//                            else section.numOfLines,
//                        lineHeight = lineHeight,
//                        onVersePointerInput = onVersePointerInput
//                    )
//                }
//            }
//        }
//        else {
//            when (section) {
//                is SuraHeaderSection -> {
//                    SuraHeader(
//                        suraNum = section.suraNum,
//                        suraName = section.suraName,
//                        isCurrentPage = isCurrentPage,
//                        textSize = textSize,
//                        onGloballyPositioned = onSuraHeaderGloballyPositioned
//                    )
//                }
//                is BasmalahSection -> {
//                    Basmalah(textSize)
//                }
//                is VersesSection -> {
//                    PageViewScreen(
//                        annotatedString = section.annotatedString,
//                        textSize = textSize,
//                        onVersePointerInput = onVersePointerInput
//                    )
//                }
//            }
//        }
    }
}

@Composable
private fun ListItems(
    sections: List<Section>,
    isCurrentPage: Boolean,
    selectedVerse: Verse?,
    trackedVerseId: Int,
    textSize: Int,
    language: Language,
    onSuraHeaderGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit,
    onVerseGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit,
    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit
) {
    for (section in sections) {
        when (section) {
            is SuraHeaderSection -> {
                SuraHeader(
                    suraNum = section.suraNum,
                    suraName = section.suraName,
                    isCurrentPage = isCurrentPage,
                    textSize = textSize,
                    onGloballyPositioned = onSuraHeaderGloballyPositioned
                )
            }
            is BasmalahSection -> {
                Basmalah(textSize)
            }
            is ListVerse -> {
                ListViewScreen(
                    verseId = section.id,
                    annotatedString = section.text,
                    isCurrentPage = isCurrentPage,
                    textSize = textSize,
                    selectedVerse = selectedVerse,
                    trackedVerseId = trackedVerseId,
                    onVerseGloballyPositioned = onVerseGloballyPositioned,
                    onVersePointerInput = onVersePointerInput
                )

                if (language != Language.ARABIC && !section.translation.isNullOrEmpty()) {
                    MyText(
                        text = section.translation,
                        modifier = Modifier.padding(6.dp),
                        fontSize = (textSize - 5).sp
                    )
                }

                if (section != sections.last()) {
                    MyHorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PageViewScreen(
    annotatedString: AnnotatedString,
    textSize: Int,
    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .pointerInput(Unit) {
                onVersePointerInput(this, layoutResult, annotatedString)
            },
        onTextLayout = { textLayoutResult ->
            layoutResult = textLayoutResult
        },
        style = TextStyle(
            fontFamily = hafs_smart,
            fontSize = textSize.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    )
}

//@Composable
//private fun FilledPageViewScreen(
//    annotatedString: AnnotatedString,
//    numOfLines: Int,
//    lineHeight: Dp,
//    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit
//) {
//    BoxWithConstraints(
//        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp).background(Color.Black)
//    ) {
//        val availableHeight = maxHeight
//        val usableHeight = availableHeight
//
//        val calculatedLineHeight = usableHeight / numOfLines * 0.95f
//
//        var fontSize by remember(annotatedString) { mutableStateOf(25.sp) }
//        var ready by remember(annotatedString) { mutableStateOf(false) }
//        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
//        var adjustmentCount by remember(annotatedString) { mutableIntStateOf(0) }
//        val t1 = remember { System.currentTimeMillis() }
//
//        Text(
//            text = annotatedString,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 6.dp, horizontal = 6.dp)
//                .pointerInput(annotatedString) {
//                    onVersePointerInput(this, layoutResult, annotatedString)
//                }
//                .drawWithContent {
//                    if (ready) {
//                        drawContent()
//                        println("Took ${System.currentTimeMillis() - t1} ms to draw content")
//                    }
//                },
//            style = TextStyle(
//                fontFamily = hafs_smart,
//                fontSize = fontSize,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                lineHeight = calculatedLineHeight.value.sp,
//                lineHeightStyle = LineHeightStyle(
//                    alignment = LineHeightStyle.Alignment.Center,
//                    trim = LineHeightStyle.Trim.None
//                ),
//                textAlign = TextAlign.Justify,
//                lineBreak = LineBreak(
//                    strategy = LineBreak.Strategy.Balanced,
//                    strictness = LineBreak.Strictness.Strict,
//                    wordBreak = LineBreak.WordBreak.Default
//                ),
//                platformStyle = PlatformTextStyle(includeFontPadding = false)
//            ),
//            overflow = TextOverflow.Visible,
//            onTextLayout = { textLayoutResult ->
//                layoutResult = textLayoutResult
//
//                if (!ready && adjustmentCount < 50) {
//                    val lineCount = textLayoutResult.lineCount
//
//                    when {
//                        lineCount > numOfLines -> {
//                            fontSize = (fontSize.value - 0.2f).sp
//                            adjustmentCount++
//                        }
//                        lineCount < numOfLines -> {
//                            fontSize = (fontSize.value + 0.2f).sp
//                            adjustmentCount++
//                        }
//                        else -> {
//                            ready = true
//                        }
//                    }
//                }
//            }
//        )
//    }
//}

@Composable
private fun ListViewScreen(
    verseId: Int,
    annotatedString: AnnotatedString,
    isCurrentPage: Boolean,
    selectedVerse: Verse?,
    trackedVerseId: Int,
    textSize: Int,
    onVerseGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit,
    onVersePointerInput: (PointerInputScope, TextLayoutResult?, AnnotatedString) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 6.dp)
            .onGloballyPositioned { layoutCoordinates ->
                onVerseGloballyPositioned(verseId, isCurrentPage, layoutCoordinates)
            }
            .pointerInput(Unit) {
                onVersePointerInput(this, layoutResult, annotatedString)
            },
        onTextLayout = { textLayoutResult ->
            layoutResult = textLayoutResult
        },
        style = TextStyle(
            fontFamily = hafs_smart,
            fontSize = textSize.sp,
            color =
                if (selectedVerse?.id == verseId) MaterialTheme.colorScheme.primary
                else if (trackedVerseId == verseId) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    )
}

@Composable
private fun SuraHeader(
    suraNum: Int,
    suraName: String,
    isCurrentPage: Boolean,
    textSize: Int,
    height: Dp? = null,
    onGloballyPositioned: (Int, Boolean, LayoutCoordinates) -> Unit
) {
// TODO: Implement revelation icon
//                        Icon(
//                            painter = painterResource(
//                                if (item.revelation == 0) R.drawable.ic_kaaba
//                                else R.drawable.ic_madina
//                            ),
//                            contentDescription = stringResource(R.string.revelation_view_description),
//                            modifier = Modifier.size(30.dp),
//                            tint = MaterialTheme.colorScheme.outlineVariant
//                        )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(height ?: (textSize * 1.6).dp)
            .onGloballyPositioned { layoutCoordinates ->
                onGloballyPositioned(suraNum, isCurrentPage, layoutCoordinates)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.sura_header),
            contentDescription = suraName,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp, horizontal = 5.dp),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )

        MyText(
            text = "${stringResource(R.string.sura)} $suraName",
            fontSize = (textSize * 0.9).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = uthmanic_hafs
        )
    }
}

@Composable
private fun Basmalah(textSize: Int, height: Dp? = null) {
    Image(
        painter = painterResource(R.drawable.basmala),
        contentDescription = stringResource(R.string.basmalah),
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(height ?: (textSize * 1.6).dp),
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
    )
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

private fun getLineHeight(configuration: Configuration): Dp {
    val screenHeightPx = configuration.screenHeightDp.dp
    val topBarHeight = 36.dp
    val bottomBarHeight = 56.dp
    val availableHeight = screenHeightPx - topBarHeight - bottomBarHeight
    return availableHeight / 15f
}