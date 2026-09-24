@file:OptIn(ExperimentalFoundationApi::class)

package bassamalim.hidaya.features.hijriDatePicker

import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.features.hijriDatePicker.HijriDatePickerDomain
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class HijriDatePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domain: HijriDatePickerDomain,
    private val navigator: Navigator
): ViewModel() {

    private val initialDate = savedStateHandle.get<String>("initial_date")!!

    private lateinit var language: Language
    private lateinit var numeralsLanguage: Language
    private val monthsNames = domain.getMonthNames()
    private val weekDays = domain.getWeekDays()
    var initialPage by Delegates.notNull<Int>()
    val pageCount = { (domain.maxYear - domain.minYear + 1) * 12 }
    private lateinit var daysPagerState: PagerState
    private lateinit var coroutineScope: CoroutineScope
    private var displayedMonth by Delegates.notNull<Int>()  // 1-indexed
    private var displayedYear by Delegates.notNull<Int>()
    private lateinit var currentDaysGrid: List<List<Int>>

    private val _uiState = MutableStateFlow(HijriDatePickerUiState())
    val uiState = _uiState.onStart {
        initializeData()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = HijriDatePickerUiState()
    )

    private fun initializeData() {
        viewModelScope.launch {
            language = domain.getLanguage()
            numeralsLanguage = domain.getNumeralsLanguage()

            val date = initialDate.split("-")
            val year = date[0].toInt()
            val month = date[1].toInt()
            val day = date[2].toInt()

            initialPage = getPageNum(year, month)
            displayedYear = year
            displayedMonth = month
            domain.setSelectedDate(year, month, day)

            _uiState.update { it.copy(
                isLoading = false,
                mainText = getMainText(),
                displayedYearText = translateNums(
                    numeralsLanguage = numeralsLanguage,
                    string = displayedYear.toString()
                ),
                displayedMonthText = getDisplayedMonthString(displayedMonth),
                weekDaysAbb = domain.getWeekDaysAbb(language),
                yearSelectorItems = getYearSelectorItems(),
                selectedDay = translateNums(
                    string = day.toString(),
                    numeralsLanguage = numeralsLanguage
                )
            )}
        }
    }

    fun onStart(daysPagerState: PagerState, coroutineScope: CoroutineScope) {
        this.daysPagerState = daysPagerState
        this.coroutineScope = coroutineScope
    }

    fun onYearSelectorToggled() {
        _uiState.update { it.copy(
            selectorMode = when (it.selectorMode) {
                SelectorMode.DAY_MONTH -> SelectorMode.YEAR
                SelectorMode.YEAR -> SelectorMode.DAY_MONTH
            }
        )}
    }

    fun onYearSelected(year: String) {
        coroutineScope.launch {
            val newPageNum = getPageNum(year.toInt(), displayedMonth)

            _uiState.update { it.copy(
                selectorMode = SelectorMode.DAY_MONTH
            )}
            updateDisplayed(newPageNum)
            daysPagerState.scrollToPage(newPageNum)
        }
    }

    fun onPreviousMonthClick() {
        coroutineScope.launch {
            val newPageNum = daysPagerState.currentPage - 1

            updateDisplayed(newPageNum)
            daysPagerState.animateScrollToPage(daysPagerState.currentPage - 1)
        }
    }

    fun onNextMonthClick() {
        coroutineScope.launch {
            val newPageNum = daysPagerState.currentPage + 1

            updateDisplayed(newPageNum)
            daysPagerState.animateScrollToPage(newPageNum)
        }
    }

    fun onDaySelected(x: Int, y: Int) {
        val day = currentDaysGrid[y][x]

        domain.setSelectedDate(year = displayedYear, month = displayedMonth, day = day)
        _uiState.update { it.copy(
            mainText = getMainText(),
            selectedDay = translateNums(
                string = day.toString(),
                numeralsLanguage = numeralsLanguage
            )
        )}
    }

    fun onSelectClicked() {
        navigator.navigateBackWithResult(
            Bundle().apply {
                // A copy: the domain keeps reusing its instance for the next picking
                putSerializable("selected_date", domain.getSelectedDate().clone() as UmmalquraCalendar)
            }
        )
    }

    fun onCancelClicked() {
        navigator.navigateBackWithResult(null)
    }

    private fun updateDisplayed(pageNum: Int) {
        val (year, month) = getYearAndMonth(pageNum)
        displayedYear = year
        displayedMonth = month

        val selectedDate = domain.getSelectedDate()
        val isCurrent = year == selectedDate[Calendar.YEAR]
                && month == selectedDate[Calendar.MONTH] + 1

        _uiState.update { it.copy(
            displayedYearText = translateNums(
                numeralsLanguage = numeralsLanguage,
                string = year.toString()
            ),
            displayedMonthText = getDisplayedMonthString(month),
            isSelectedDayDisplayed = isCurrent
        )}
    }

    private fun getDisplayedMonthString(month: Int) =
        "${monthsNames[month-1]} " + translateNums(
            string = month.toString(),
            numeralsLanguage = numeralsLanguage
        )

    private fun getMainText(): String {
        val selectedDate = domain.getSelectedDate()
        return "${weekDays[selectedDate[Calendar.DAY_OF_WEEK] - 1]} " +
                "${
                    translateNums(
                        string = selectedDate[Calendar.DATE].toString(),
                        numeralsLanguage = numeralsLanguage
                    )
                } " +
                monthsNames[selectedDate[Calendar.MONTH]]
    }

    private fun getYearSelectorItems() =
        Array(domain.maxYear - domain.minYear + 1) { idx ->
            translateNums(
                string = (domain.minYear + idx).toString(),
                numeralsLanguage = numeralsLanguage
            )
        }.toList()

    fun onMonthPageChanged(pageNum: Int) {
        val isActive = pageNum == daysPagerState.currentPage
        if (isActive) updateDisplayed(pageNum)
    }

    fun getDaysGrid(page: Int): List<List<DayCell>> {
        val (year, month) = getYearAndMonth(page)
        val calendar = UmmalquraCalendar()
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month - 1

        val offset = calendar[Calendar.DAY_OF_WEEK]
        val grid = Array(5) { row ->  // 1 hijri month spans 5 weeks at most
            Array(7) { col ->  // 7 days a week
                val idx = row * 7 + col - offset
                if (row == 0 && col < offset || idx >= calendar.lengthOfMonth()) 0
                else idx + 1
            }.toList()
        }.toList()

        val currentDate = domain.getCurrentDate()
        val textGrid = grid.mapIndexed { i, row ->
            row.mapIndexed { j, cell ->
                DayCell(
                    dayText =
                        if (cell == 0) ""
                        else translateNums(
                            string = cell.toString(),
                            numeralsLanguage = numeralsLanguage
                        ),
                    isToday = year == currentDate[Calendar.YEAR]
                            && month == currentDate[Calendar.MONTH]+1
                            && cell == currentDate[Calendar.DATE],
                )
            }
        }

        if (page == daysPagerState.currentPage) currentDaysGrid = grid

        return textGrid
    }

    private fun getPageNum(year: Int, month: Int) = (year - domain.minYear) * 12 + (month-1)

    private fun getYearAndMonth(pageNum: Int): Pair<Int, Int> {
        val year = domain.minYear + pageNum / 12
        val month = pageNum % 12 + 1
        return Pair(year, month)
    }

}