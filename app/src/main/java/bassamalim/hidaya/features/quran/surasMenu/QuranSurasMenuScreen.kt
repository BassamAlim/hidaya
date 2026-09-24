package bassamalim.hidaya.features.quran.surasMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyFavoriteButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.SearchBarClearButton
import bassamalim.hidaya.core.ui.components.SearchBarLeadingIcon
import bassamalim.hidaya.core.ui.components.SearchBarPlaceholder
import bassamalim.hidaya.core.ui.components.TabLayout
import bassamalim.hidaya.core.ui.components.searchBarShape
import bassamalim.hidaya.core.ui.components.tutorial.TutorialOverlay
import bassamalim.hidaya.core.ui.components.tutorial.TutorialShape
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
import kotlinx.coroutines.flow.Flow

@Composable
fun QuranSurasMenuScreen(viewModel: QuranSurasViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return LoadingScreen()

    val tutorialState = rememberTutorialState()
    val bookmarksTip = stringResource(R.string.quran_suras_tutorial_bookmarks)
    val hasBookmarks = state.bookmarks.isNotEmpty()
    // The tip points at the bookmarks row, so it waits until there's a bookmark to point at
    LaunchedEffect(state.isTutorialActive, hasBookmarks) {
        if (state.isTutorialActive && hasBookmarks) {
            tutorialState.start(
                steps = listOf(
                    TutorialStep(
                        text = bookmarksTip,
                        targetKey = "quran_bookmarks_row",
                        shape = TutorialShape.RoundedRect
                    )
                ),
                onFinished = viewModel::onTutorialFinished
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyScaffold(
            title = "",
            topBar = {}  // override the default top bar
        ) { padding ->
            TabLayout(
                pageNames = listOf(
                    stringResource(R.string.all),
                    stringResource(R.string.favorites)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                searchComponent = {
                    QuranSearchBar(
                        searchSurasAndPages = viewModel::searchSurasAndPages,
                        searchVerses = viewModel::searchVerses,
                        onSuraClick = viewModel::onSuraClick,
                        onPageClick = viewModel::onPageClick,
                        onVerseClick = viewModel::onVerseClick
                    )

                    if (hasBookmarks) {
                        BookmarksRow(
                            bookmarks = state.bookmarks,
                            modifier = Modifier.tutorialTarget(tutorialState, "quran_bookmarks_row"),
                            onBookmarkClick = viewModel::onBookmarkClick
                        )
                    }
                }
            ) { page ->
                Tab(
                    surasFlow = viewModel.getItems(page),
                    onSuraClick = viewModel::onSuraClick,
                    onFavoriteClick = viewModel::onFavoriteClick
                )
            }
        }

        TutorialOverlay(state = tutorialState)
    }
}

@Composable
private fun BookmarksRow(
    bookmarks: List<BookmarkItem>,
    modifier: Modifier = Modifier,
    onBookmarkClick: (Int) -> Unit
) {
    val dims = MaterialTheme.dimensions
    val colors = listOf(Bookmark1Color, Bookmark2Color, Bookmark3Color, Bookmark4Color)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = dims.spaceSm, vertical = dims.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)
    ) {
        items(bookmarks, key = { it.index }) { bookmark ->
            AssistChip(
                onClick = { onBookmarkClick(bookmark.verseId) },
                label = {
                    Text(
                        text = stringResource(
                            R.string.bookmark_label,
                            bookmark.suraName,
                            bookmark.verseNumText
                        ),
                        style = MaterialTheme.appTypography.label
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = stringResource(R.string.bookmarked_verse),
                        tint = colors[bookmark.index],
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
private fun Tab(
    surasFlow: Flow<List<SuraItem>>,
    onSuraClick: (Int) -> Unit,
    onFavoriteClick: (Int, Boolean) -> Unit
) {
    val suras by surasFlow.collectAsStateWithLifecycle(emptyList())

    MyLazyColumn(
        lazyList = {
            items(suras, key = { it.id }) { item ->
                SuraRow(
                    sura = item,
                    onClick = { onSuraClick(item.id) },
                    onFavoriteClick = { onFavoriteClick(item.id, item.isFavorite) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.spaceLg),
                    thickness = MaterialTheme.dimensions.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    )
}

@Composable
private fun SuraRow(sura: SuraItem, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = dims.spaceLg,
                end = dims.spaceXs,
                top = dims.spaceMd,
                bottom = dims.spaceMd
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dims.iconXl)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(dims.radiusMd)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sura.numberText,
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = dims.spaceMd)
        ) {
            Text(
                text = "${stringResource(R.string.sura)} ${sura.name}",
                style = MaterialTheme.appTypography.title
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        if (sura.isMeccan) R.drawable.ic_kaaba else R.drawable.ic_madina
                    ),
                    contentDescription = stringResource(
                        if (sura.isMeccan) R.string.meccan else R.string.medinan
                    ),
                    modifier = Modifier.size(dims.iconSm - dims.spaceXs),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Wide enough that the icon and the page read as two separate facts
                Spacer(Modifier.width(dims.spaceMd))

                Text(
                    text = stringResource(R.string.start_page, sura.startPageText),
                    style = MaterialTheme.appTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MyFavoriteButton(
            isFavorite = sura.isFavorite,
            onClick = onFavoriteClick,
            size = dims.iconMd
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuranSearchBar(
    searchSurasAndPages: (String) -> List<SearchMatch>,
    searchVerses: (String, Color) -> List<VerseMatch>,
    onSuraClick: (Int) -> Unit,
    onPageClick: (String) -> Unit,
    onVerseClick: (Int) -> Unit
) {
    val state = rememberTextFieldState()
    var expanded by remember { mutableStateOf(false) }

    val dims = MaterialTheme.dimensions

    SearchBar(
        inputField = {
            // Material's text fields default to the system font; the app's text is Tajawal
            ProvideTextStyle(MaterialTheme.appTypography.body) {
                SearchBarDefaults.InputField(
                    state = state,
                    onSearch = {},
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = {
                        SearchBarPlaceholder(stringResource(R.string.quran_search_hint))
                    },
                    leadingIcon = { SearchBarLeadingIcon() },
                    trailingIcon = {
                        if (state.text.isNotEmpty()) SearchBarClearButton(state::clearText)
                    }
                )
            }
        },
        expanded = expanded,
        onExpandedChange = {},
        // Expanded, it fills the screen, so the spacing only applies collapsed
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (expanded) 0.dp else dims.spaceLg,
                vertical = if (expanded) 0.dp else dims.spaceSm
            ),
        shape = searchBarShape()
    ) {
        val suraAndPageMatches = searchSurasAndPages(state.text.toString())
        val highlightColor = MaterialTheme.colorScheme.primary
        val verseMatches = searchVerses(state.text.toString(), highlightColor)

        SearchBarContent(
            suraAndPageMatches = suraAndPageMatches,
            verseMatches = verseMatches,
            onSuraClick = onSuraClick,
            onPageClick = onPageClick,
            onVerseClick = onVerseClick
        )
    }
}

@Composable
private fun SearchBarContent(
    suraAndPageMatches: List<SearchMatch>,
    verseMatches: List<VerseMatch>,
    onSuraClick: (Int) -> Unit,
    onPageClick: (String) -> Unit,
    onVerseClick: (Int) -> Unit
) {
    if (suraAndPageMatches.isEmpty() && verseMatches.isEmpty()) {
        NoMatchesSection(title = stringResource(R.string.suras_and_pages))

        NoMatchesSection(title = stringResource(R.string.verses))
    }
    else {
        if (suraAndPageMatches.isNotEmpty()) {
            SuraAndPagesMatchesSection(
                matches = suraAndPageMatches,
                onSuraClick = onSuraClick,
                onPageClick = onPageClick
            )
        }

        if (verseMatches.isNotEmpty()) {
            VerseMatchesSection(verseMatches = verseMatches, onVerseClick = onVerseClick)
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    MyText(
        text = title,
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 17.dp),
        fontSize = 16.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun NoMatchesSection(title: String) {
    SearchSectionTitle(title = title)

    MyText(
        text = stringResource(R.string.no_matches),
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SuraAndPagesMatchesSection(
    matches: List<SearchMatch>,
    onSuraClick: (Int) -> Unit,
    onPageClick: (String) -> Unit
) {
    SearchSectionTitle(title = stringResource(R.string.suras_and_pages))

    MyLazyColumn(
        state = rememberLazyListState(),
        lazyList = {
            items(
                matches,
                key = { match ->
                    when (match) {
                        is SuraMatch -> "sura-${match.id}"
                        is PageMatch -> "page-${match.num}"
                        else -> match.hashCode()
                    }
                }
            ) { match ->
                when (match) {
                    is SuraMatch -> {
                        ListItem(
                            headlineContent = {
                                MyText(
                                    text = "${stringResource(R.string.sura)} " +
                                            match.decoratedName,
                                    modifier = Modifier.padding(
                                        top = 12.dp,
                                        bottom = 12.dp,
                                        start = 16.dp
                                    ),
                                    textAlign = TextAlign.Start
                                )
                            },
                            modifier = Modifier.clickable { onSuraClick(match.id) }
                        )
                    }
                    is PageMatch -> {
                        ListItem(
                            headlineContent = {
                                MyText(
                                    text = "${stringResource(R.string.page)} ${match.num}",
                                    modifier = Modifier.padding(
                                        top = 12.dp,
                                        bottom = 12.dp,
                                        start = 16.dp
                                    ),
                                    textAlign = TextAlign.Start
                                )
                            },
                            modifier = Modifier.clickable { onPageClick(match.num) },
                            supportingContent = {
                                MyText(
                                    text = match.suraName,
                                    modifier = Modifier.padding(
                                        top = 6.dp,
                                        bottom = 6.dp,
                                        start = 16.dp
                                    ),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(thickness = 0.3.dp)
            }
        }
    )
}

@Composable
private fun VerseMatchesSection(verseMatches: List<VerseMatch>, onVerseClick: (Int) -> Unit) {
    SearchSectionTitle(title = stringResource(R.string.verses))

    MyLazyColumn(
        state = rememberLazyListState(),
        lazyList = {
            items(verseMatches, key = { it.id }) { verse ->
                VerseMatchListItem(item = verse, onVerseClick = onVerseClick)

                HorizontalDivider(thickness = 0.3.dp)
            }
        }
    )
}

@Composable
private fun VerseMatchListItem(item: VerseMatch, onVerseClick: (Int) -> Unit) {
    ListItem(
        headlineContent = {
            MyText(
                text = "${stringResource(R.string.sura)} ${item.suraName}, " +
                        "${stringResource(R.string.verse_number)} ${item.verseNum}",
                modifier = Modifier.padding(top = 6.dp, start = 16.dp)
            )
        },
        modifier = Modifier.clickable { onVerseClick(item.id) },
        supportingContent = {
            MyText(
                text = item.text,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, start = 16.dp),
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
                fontFamily = hafs_smart
            )
        }
    )
}
