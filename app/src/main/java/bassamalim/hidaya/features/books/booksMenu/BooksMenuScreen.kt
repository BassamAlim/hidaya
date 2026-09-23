package bassamalim.hidaya.features.books.booksMenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.MyDownloadButton
import bassamalim.hidaya.core.ui.components.MyFloatingActionButton
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyListItem
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.tutorial.TutorialOverlay
import bassamalim.hidaya.core.ui.components.tutorial.TutorialShape
import bassamalim.hidaya.core.ui.components.tutorial.TutorialStep
import bassamalim.hidaya.core.ui.components.tutorial.rememberTutorialState
import bassamalim.hidaya.core.ui.components.tutorial.tutorialTarget
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.FileUtils

// Height of a FAB (56dp) plus its 16dp margin, and a little breathing room
private val FabClearance = 88.dp

@Composable
fun BooksMenuScreen(viewModel: BooksMenuViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val tutorialState = rememberTutorialState()

    if (state.isLoading) return LoadingScreen()

    val searchTip = stringResource(R.string.books_tutorial_search)
    LaunchedEffect(state.isTutorialActive) {
        if (state.isTutorialActive) {
            tutorialState.start(
                steps = listOf(
                    TutorialStep(
                        text = searchTip,
                        targetKey = "books_search",
                        shape = TutorialShape.Circle
                    )
                ),
                onFinished = viewModel::onTutorialFinished
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MyScaffold(
            title = stringResource(R.string.hadeeth_books),
            floatingActionButton = {
                val noDownloadedBooksMessage = stringResource(R.string.no_downloaded_books)
                MyFloatingActionButton(
                    iconId = R.drawable.ic_quran_search,
//                imageVector = Icons.Default.FindInPage,
                    description = stringResource(R.string.search_in_books),
                    modifier = Modifier.tutorialTarget(tutorialState, "books_search"),
                    onClick = {
                        viewModel.onSearcherClick(
                            snackBarHostState = snackBarHostState,
                            message = noDownloadedBooksMessage
                        )
                    }
                )
            },
            snackBarHost = {
                SnackbarHost(hostState = snackBarHostState)
            }
        ) { padding ->
            // books list
            MyLazyColumn(
                modifier = Modifier.padding(padding),
                lazyList = {
                    items(state.books.toList(), key = { (id, _) -> id }) { (id, book) ->
                        BookRow(
                            id = id,
                            book = book,
                            onItemClick = viewModel::onItemClick,
                            onDownloadButtonClick = viewModel::onDownloadButtonClick
                        )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.spaceLg),
                        thickness = MaterialTheme.dimensions.dividerThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    }

                    // Keeps the last book clear of the search button floating over the list
                    item { Spacer(Modifier.height(FabClearance)) }
                }
            )

            if (state.shouldShowWait != 0)
                WaitMessage(state.shouldShowWait)
        }

        TutorialOverlay(state = tutorialState)
    }
}

@Composable
private fun BookRow(
    id: Int,
    book: Book,
    onItemClick: (Int, Book) -> Unit,
    onDownloadButtonClick: (Int, Book) -> Unit,
) {
    MyListItem(
        headline = book.title,
        onClick = { onItemClick(id, book) },
        trailing = {
            MyDownloadButton(
                state = book.downloadState,
                iconSize = MaterialTheme.dimensions.iconMd,
                onClick = { onDownloadButtonClick(id, book) }
            )
        }
    )
}

@Composable
private fun WaitMessage(shouldShow: Int) {
    val context = LocalContext.current
    LaunchedEffect(shouldShow) {
        FileUtils.showWaitMassage(context)
    }
}