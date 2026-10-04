package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun TabLayout(
    pageNames: List<String>,
    modifier: Modifier = Modifier,
    searchComponent: @Composable () -> Unit = {},
    tabsContent: @Composable (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pageNames.size })

    Column(modifier) {
        Tabs(pagerState = pagerState, pageNames = pageNames)

        searchComponent()

        TabsContent(pagerState = pagerState, content = tabsContent)
    }
}

@Composable
fun Tabs(pagerState: PagerState, pageNames: List<String>) {
    val scope = rememberCoroutineScope()
    val tabs = @Composable {
        pageNames.forEachIndexed { index, _ ->
            Tab(
                selected = pagerState.currentPage == index,
                text = {
                    MyText(
                        text = pageNames[index],
                        fontSize = 18.sp,
                        color =
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }

    // Past three, tabs scroll rather than squeeze their names onto two lines
    if (pageNames.size > 3)
        PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 0.dp) { tabs() }
    else PrimaryTabRow(selectedTabIndex = pagerState.currentPage) { tabs() }
}

@Composable
fun ColumnScope.TabsContent(pagerState: PagerState, content: @Composable (Int) -> Unit) {
    HorizontalPager(
        state = pagerState,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.weight(1F)
    ) { page ->
        content(page)
    }
}