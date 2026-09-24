package bassamalim.hidaya.features.quran.reader

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.QuranViewType
import bassamalim.hidaya.core.models.Verse
import bassamalim.hidaya.core.ui.components.MyHorizontalDivider
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.theme.hafs_smart
import bassamalim.hidaya.core.ui.theme.uthmanic_hafs

@Composable
internal fun PageContent(
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

private fun getLineHeight(configuration: Configuration): Dp {
    val screenHeightPx = configuration.screenHeightDp.dp
    val topBarHeight = 36.dp
    val bottomBarHeight = 56.dp
    val availableHeight = screenHeightPx - topBarHeight - bottomBarHeight
    return availableHeight / 15f
}
