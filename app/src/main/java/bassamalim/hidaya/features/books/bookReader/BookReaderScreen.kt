package bassamalim.hidaya.features.books.bookReader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.models.BookContent
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyTopBar
import bassamalim.hidaya.core.ui.components.ReaderBottomBar
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// The stored text size is an offset over this base (kept from the original reader)
private const val BASE_TEXT_SIZE = 15
private const val LINE_HEIGHT_RATIO = 1.7f
private const val SAVE_POSITION_DELAY_MILLIS = 500L

// The list starts with the chapter title, so door i sits at list index i + 1
private const val HEADER_ITEMS = 1

@Composable
fun BookReaderScreen(viewModel: BookReaderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    // Screen-level so a jump started from the sheet survives the sheet closing
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex =
            if (state.initialDoorIndex == 0 && state.initialScrollOffset == 0) 0
            else state.initialDoorIndex + HEADER_ITEMS,
        initialFirstVisibleItemScrollOffset = state.initialScrollOffset
    )

    // Saves where the user is once scrolling settles, so reopening the book resumes here
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (index, offset) ->
                delay(SAVE_POSITION_DELAY_MILLIS)
                if (index < HEADER_ITEMS) viewModel.onReadingPositionChange(0, 0)
                else viewModel.onReadingPositionChange(index - HEADER_ITEMS, offset)
            }
    }

    MyScaffold(
        title = state.bookTitle,
        topBar = {
            Column {
                MyTopBar(
                    title = state.bookTitle,
                    actions = {
                        if (state.doors.size > 1) {
                            IconButton(onClick = viewModel::onDoorsClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.book_sections)
                                )
                            }
                        }
                    }
                )

                ReadingProgress(listState = listState)
            }
        },
        bottomBar = {
            ReaderBottomBar(textSize = state.textSize, onSeek = viewModel::onTextSizeChange)
        }
    ) { padding ->
        ChapterContent(
            state = state,
            listState = listState,
            modifier = Modifier.padding(padding),
            onPreviousChapterClick = viewModel::onPreviousChapterClick,
            onNextChapterClick = viewModel::onNextChapterClick
        )
    }

    if (state.isDoorsSheetShown) {
        DoorsSheet(
            doors = state.doors,
            onDoorClick = { index ->
                scope.launch { listState.scrollToItem(index + HEADER_ITEMS) }
                viewModel.onDoorsSheetDismiss()
            },
            onDismiss = viewModel::onDoorsSheetDismiss
        )
    }
}

@Composable
private fun ReadingProgress(listState: LazyListState) {
    val progress by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) 0f
            else listState.firstVisibleItemIndex.toFloat() / (total - 1)
        }
    }

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        drawStopIndicator = {}
    )
}

@Composable
private fun ChapterContent(
    state: BookReaderUiState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    onPreviousChapterClick: () -> Unit,
    onNextChapterClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val fontSizeSp = state.textSize + BASE_TEXT_SIZE

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            Text(
                text = state.chapterTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dims.screenPaddingHorizontal,
                        vertical = dims.spaceXl
                    ),
                style = MaterialTheme.appTypography.display,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        itemsIndexed(state.doors, key = { _, door -> door.id }) { index, door ->
            Door(door = door, fontSizeSp = fontSizeSp)

            if (index < state.doors.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = dims.spaceXxl,
                        vertical = dims.spaceLg
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        item {
            ChapterNavigation(
                previousChapterTitle = state.previousChapterTitle,
                nextChapterTitle = state.nextChapterTitle,
                onPreviousClick = onPreviousChapterClick,
                onNextClick = onNextChapterClick
            )
        }
    }
}

@Composable
private fun Door(door: BookContent.Chapter.Door, fontSizeSp: Float) {
    val dims = MaterialTheme.dimensions

    Column(Modifier.padding(horizontal = dims.screenPaddingHorizontal)) {
        if (door.title.isNotBlank()) {
            Text(
                text = door.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dims.spaceMd),
                style = MaterialTheme.appTypography.title.copy(
                    fontSize = (fontSizeSp * 1.1f).sp,
                    lineHeight = (fontSizeSp * 1.1f * LINE_HEIGHT_RATIO).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )
        }

        // Generous line height: long Arabic passages with diacritics need the room
        Text(
            text = door.text,
            style = MaterialTheme.appTypography.body.copy(
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * LINE_HEIGHT_RATIO).sp
            ),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun ChapterNavigation(
    previousChapterTitle: String?,
    nextChapterTitle: String?,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    if (previousChapterTitle == null && nextChapterTitle == null) return

    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenPaddingHorizontal, vertical = dims.spaceXxl),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceMd)
    ) {
        ChapterButton(
            label = stringResource(R.string.previous_section),
            title = previousChapterTitle,
            modifier = Modifier.weight(1f),
            onClick = onPreviousClick
        )

        ChapterButton(
            label = stringResource(R.string.next_section),
            title = nextChapterTitle,
            modifier = Modifier.weight(1f),
            onClick = onNextClick
        )
    }
}

@Composable
private fun ChapterButton(
    label: String,
    title: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = title != null
    ) {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.dimensions.spaceXs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.appTypography.button)

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.appTypography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoorsSheet(
    doors: List<BookContent.Chapter.Door>,
    onDoorClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.book_sections),
            modifier = Modifier.padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.spaceSm
            ),
            style = MaterialTheme.appTypography.headline
        )

        LazyColumn {
            itemsIndexed(doors, key = { _, door -> door.id }) { index, door ->
                Text(
                    text = door.title.ifBlank { "${index + 1}" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDoorClick(index) }
                        .padding(
                            horizontal = dims.screenPaddingHorizontal,
                            vertical = dims.spaceMd
                        ),
                    style = MaterialTheme.appTypography.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
