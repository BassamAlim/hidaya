package bassamalim.hidaya.features.hijriDatePicker

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyDialog
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

// Laid out like Material 3's date picker, which only supports the Gregorian calendar
private val DayCellHeight = 44.dp
private val DaySize = 40.dp
private val WeekDaysRowHeight = 36.dp
private const val MAX_WEEKS_IN_MONTH = 6
// Same for both modes, so switching to the year list doesn't resize the dialog
private val SelectorHeight = WeekDaysRowHeight + DayCellHeight * MAX_WEEKS_IN_MONTH

@Composable
fun HijriDatePickerDialog(
    viewModel: HijriDatePickerViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    if (state.isLoading) return

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPage,
        pageCount = viewModel.pageCount
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(pagerState, coroutineScope)
        onDispose {}
    }

    MyDialog(
        shown = true,
        onDismiss = viewModel::onCancelClicked
    ) {
        Column {
            Header(selectedDate = state.mainText)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Controls(
                displayedMonth = state.displayedMonthText,
                displayedYear = state.displayedYearText,
                selectorMode = state.selectorMode,
                onYearSelectorToggled = viewModel::onYearSelectorToggled,
                onPreviousMonthClick = viewModel::onPreviousMonthClick,
                onNextMonthClick = viewModel::onNextMonthClick
            )

            Box(Modifier.height(SelectorHeight)) {
                when (state.selectorMode) {
                    SelectorMode.DAY_MONTH -> DaySelector(
                        weekDaysAbbreviations = state.weekDaysAbb,
                        pagerState = pagerState,
                        onMonthPageChanged = viewModel::onMonthPageChanged,
                        getDaysGrid = viewModel::getDaysGrid,
                        isSelectedDayDisplayed = state.isSelectedDayDisplayed,
                        selectedDay = state.selectedDay,
                        onDaySelected = viewModel::onDaySelected
                    )
                    SelectorMode.YEAR -> YearSelector(
                        selectedYear = state.displayedYearText,
                        yearOptions = state.yearSelectorItems,
                        onYearSelected = viewModel::onYearSelected
                    )
                }
            }

            Actions(
                onSelectClick = viewModel::onSelectClicked,
                onCancelClick = viewModel::onCancelClicked
            )
        }
    }
}

@Composable
private fun Header(selectedDate: String) {
    val dims = MaterialTheme.dimensions

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = dims.spaceXl,
                end = dims.spaceMd,
                top = dims.spaceLg,
                bottom = dims.spaceMd
            )
    ) {
        Text(
            text = stringResource(R.string.pick_hijri_date),
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(dims.spaceSm))

        MyText(
            text = selectedDate,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 28.sp,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            minFontSize = 16.sp
        )
    }
}

@Composable
private fun Controls(
    displayedMonth: String,
    displayedYear: String,
    selectorMode: SelectorMode,
    onYearSelectorToggled: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dims.minTouchTarget + dims.spaceSm)
            .padding(horizontal = dims.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month and year; opens the year list
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onYearSelectorToggled)
                .padding(
                    start = dims.spaceMd,
                    end = dims.spaceXs,
                    top = dims.spaceSm,
                    bottom = dims.spaceSm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$displayedMonth · $displayedYear",
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Icon(
                imageVector =
                    if (selectorMode == SelectorMode.YEAR) Icons.Default.ArrowDropUp
                    else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))

        // Month arrows only make sense over the days grid
        if (selectorMode == SelectorMode.DAY_MONTH) {
            IconButton(onClick = onPreviousMonthClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNextMonthClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DaySelector(
    weekDaysAbbreviations: List<String>,
    pagerState: PagerState,
    onMonthPageChanged: (Int) -> Unit,
    getDaysGrid: (Int) -> List<List<DayCell>>,
    isSelectedDayDisplayed: Boolean,
    selectedDay: String,
    onDaySelected: (Int, Int) -> Unit
) {
    val dims = MaterialTheme.dimensions

    Column(Modifier.padding(horizontal = dims.spaceMd)) {
        Row(Modifier.fillMaxWidth()) {
            weekDaysAbbreviations.forEach {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(WeekDaysRowHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.appTypography.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            onMonthPageChanged(page)

            DaysGrid(
                daysGrid = getDaysGrid(page),
                isSelectedDayDisplayed = isSelectedDayDisplayed,
                selectedDay = selectedDay,
                onDaySelected = onDaySelected
            )
        }
    }
}

@Composable
private fun DaysGrid(
    daysGrid: List<List<DayCell>>,
    isSelectedDayDisplayed: Boolean,
    selectedDay: String,
    onDaySelected: (Int, Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        daysGrid.forEachIndexed { y, row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEachIndexed { x, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(DayCellHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.dayText.isNotEmpty()) {
                            Day(
                                text = cell.dayText,
                                isSelected = isSelectedDayDisplayed && cell.dayText == selectedDay,
                                isToday = cell.isToday,
                                onClick = { onDaySelected(x, y) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Day(text: String, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(DaySize)
            .then(
                // Today is outlined, as in Material's picker; the selection fill replaces it
                if (isToday && !isSelected) Modifier.border(1.dp, colors.primary, CircleShape)
                else Modifier
            ),
        shape = CircleShape,
        color = if (isSelected) colors.primary else Color.Transparent,
        contentColor = when {
            isSelected -> colors.onPrimary
            isToday -> colors.primary
            else -> colors.onSurface
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.appTypography.body.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun YearSelector(
    selectedYear: String,
    yearOptions: List<String>,
    onYearSelected: (String) -> Unit
) {
    val dims = MaterialTheme.dimensions
    val colors = MaterialTheme.colorScheme
    // Opens with the selected year about two rows down, so earlier years show above it
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = (yearOptions.indexOf(selectedYear) - 6).coerceAtLeast(0)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dims.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dims.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)
    ) {
        items(yearOptions, key = { it }) { year ->
            val isSelected = year == selectedYear

            Surface(
                onClick = { onYearSelected(year) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dims.buttonHeight - dims.spaceSm),
                shape = CircleShape,
                color = if (isSelected) colors.primary else Color.Transparent,
                contentColor = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = year,
                        style = MaterialTheme.appTypography.body.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Actions(
    onSelectClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.spaceMd, vertical = dims.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceSm, Alignment.End)
    ) {
        TextButton(onClick = onCancelClick) {
            Text(
                text = stringResource(R.string.cancel),
                style = MaterialTheme.appTypography.button
            )
        }

        TextButton(onClick = onSelectClick) {
            Text(
                text = stringResource(R.string.select),
                style = MaterialTheme.appTypography.button
            )
        }
    }
}
