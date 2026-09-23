package bassamalim.hidaya.features.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.TimeOfDay
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import bassamalim.hidaya.core.utils.LangUtils.translateNums
import bassamalim.hidaya.core.utils.LangUtils.translateTimeNums
import bassamalim.hidaya.features.main.BottomNavItem
import bassamalim.hidaya.features.quran.reader.QuranTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.get

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val domain: HomeDomain,
    private val navigator: Navigator
): ViewModel() {

    private val prayerNames = domain.getPrayerNames()
    private var times: Map<Prayer, Calendar?> = emptyMap()
    private var formattedTimes: Map<Prayer, String> = emptyMap()
    private var yesterdayIshaa: Calendar? = null
    private var formattedYesterdayIshaa: String = ""
    private var tomorrowFajr: Calendar? = null
    private var formattedTomorrowFajr: String = ""
    private var timer: CountDownTimer? = null
    private var previousPrayer: Prayer? = null
    private var nextPrayer: Prayer? = null
    private var previousPrayerWasYesterday = false
    private var nextPrayerIsTomorrow = false
    private var werdPage: Int? = null
    private var shouldCount = false

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = combine(
        _uiState.asStateFlow(),
        domain.getNumeralsLanguage(),
        domain.getLocation(),
        domain.getWerdPage(),
        domain.isWerdDone()
    ) { state, numeralsLanguage, location, werdPage, isWerdDone ->
        this.werdPage = werdPage

        state.copy(
            werdPage = translateNums(
                string = werdPage.toString(),
                numeralsLanguage = numeralsLanguage
            ),
            numeralsLanguage = numeralsLanguage,
            isWerdDone = isWerdDone
        )
    }.combine(
        domain.getLocalRecord()
    ) { state, localRecord ->
        state.copy(
            quranRecord = translateNums(
                string = localRecord.quranPages.toString(),
                numeralsLanguage = state.numeralsLanguage
            ),
            recitationsRecord = formatRecitationsTime(
                millis = localRecord.recitationsTime,
                language = state.language,
                numeralsLanguage = state.numeralsLanguage
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = HomeUiState()
    )

    private suspend fun initializeData(activity: Activity) {
        val pendingPermissions = getPendingPermissions(activity)

        val location = domain.getLocation().first()
        if (location != null) {
            times = domain.getPrayerTimeMap(location)
            formattedTimes = domain.getStrPrayerTimeMap(location)
            yesterdayIshaa = domain.getYesterdayIshaa(location)
            formattedYesterdayIshaa = domain.getStrYesterdayIshaa(location)
            tomorrowFajr = domain.getTomorrowFajr(location)
            formattedTomorrowFajr = domain.getStrTomorrowFajr(location)
        }

        shouldCount = location != null && times.isNotEmpty()

        val leaderboardConnected = domain.syncRecords()

        _uiState.update { it.withPrayers().copy(
            isLoading = false,
            pendingPermissions = pendingPermissions,
            isLeaderboardEnabled = leaderboardConnected
        )}
    }

    fun onStart(activity: Activity) {
        viewModelScope.launch {
            initializeData(activity)

            if (shouldCount)
                count()
        }
    }

    fun onStop() {
        timer?.cancel()
    }

    fun onPermissionResult(activity: Activity) {
        _uiState.update { it.copy(
            pendingPermissions = getPendingPermissions(activity)
        )}
    }

    fun onPrayerCardClick(navController: NavHostController) {
        navController.navigate(BottomNavItem.PrayersBoard.route) {
            navController.graph.startDestinationRoute?.let { screenRoute ->
                popUpTo(screenRoute) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun onTodayWerdCardClick() {
        val werdPage = werdPage ?: return

        navigator.navigate(
            Screen.QuranReader(
                targetType = QuranTarget.PAGE.name,
                targetValue = werdPage.toString()
            )
        )

        domain.trackDailyWerdViewed()
    }

    fun onRemembranceClick() {
        // Ids 0 and 1 are the morning and evening remembrances (same ids the reminders open)
        val id = if (_uiState.value.isMorning) 0 else 1
        navigator.navigate(Screen.RemembranceReader(id.toString()))
    }

    fun onLeaderboardClick() {
        navigator.navigate(Screen.Leaderboard)
    }

    private fun HomeUiState.withPrayers(): HomeUiState {
        val previousPrayer = getPreviousPrayer()
        val nextPrayer = getNextPrayer()
        val now = System.currentTimeMillis()

        return copy(
            previousPrayerName = prayerNames[previousPrayer]!!,
            previousPrayerTimeText = translateNums(
                string = if (previousPrayerWasYesterday) formattedYesterdayIshaa
                else formattedTimes[previousPrayer]!!,
                numeralsLanguage = numeralsLanguage
            ),
            nextPrayerName = prayerNames[nextPrayer]!!,
            nextPrayerTimeText = translateNums(
                string = if (nextPrayerIsTomorrow) formattedTomorrowFajr
                else formattedTimes[nextPrayer]!!,
                numeralsLanguage = numeralsLanguage
            ),
            todayPrayers = listOf(
                Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHAA
            ).map { prayer ->
                TodayPrayer(
                    name = prayerNames[prayer]!!,
                    timeText = formattedTimes[prayer].orEmpty(),
                    status = when {
                        !nextPrayerIsTomorrow && prayer == nextPrayer -> TodayPrayer.Status.NEXT
                        (times[prayer]?.timeInMillis ?: Long.MAX_VALUE) < now ->
                            TodayPrayer.Status.PASSED
                        else -> TodayPrayer.Status.UPCOMING
                    }
                )
            },
            // Morning remembrances from Fajr until Asr, evening ones from Asr until the next Fajr
            isMorning = !nextPrayerIsTomorrow &&
                    nextPrayer in setOf(Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR)
        )
    }

    private fun getPreviousPrayer(): Prayer? {
        previousPrayer = domain.getPreviousPrayer(times)

        previousPrayerWasYesterday = false
        if (previousPrayer == null) {
            previousPrayerWasYesterday = true
            previousPrayer = Prayer.ISHAA
        }

        return previousPrayer
    }

    private fun getPendingPermissions(activity: Activity): List<PendingPermission> {
        val pendingPermissions = mutableListOf<PendingPermission>()

        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(activity.application, fineLocationPermission)
            != PackageManager.PERMISSION_GRANTED) {
            pendingPermissions.add(
                PendingPermission(
                    messageResId = R.string.pending_location_permission_message,
                    permission = fineLocationPermission
                )
            )
        }

        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(activity.application, coarseLocationPermission)
            != PackageManager.PERMISSION_GRANTED) {
            pendingPermissions.add(
                PendingPermission(
                    messageResId = R.string.pending_location_permission_message,
                    permission = coarseLocationPermission
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            if (ContextCompat
                .checkSelfPermission(activity.application, backgroundLocationPermission)
                != PackageManager.PERMISSION_GRANTED) {
                pendingPermissions.add(
                    PendingPermission(
                        messageResId = R.string.pending_background_location_permission_message,
                        permission = backgroundLocationPermission
                    )
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(activity.application, notificationPermission)
                != PackageManager.PERMISSION_GRANTED) {
                pendingPermissions.add(
                    PendingPermission(
                        messageResId = R.string.pending_notification_permission_message,
                        permission = notificationPermission
                    )
                )
            }
        }

        return pendingPermissions
    }

    private fun getNextPrayer(): Prayer? {
        nextPrayer = domain.getNextPrayer(times)

        nextPrayerIsTomorrow = false
        if (nextPrayer == null) {
            nextPrayerIsTomorrow = true
            nextPrayer = Prayer.FAJR
        }

        return nextPrayer
    }

    private fun recount() {
        viewModelScope.launch {
            val location = domain.getLocation().first()
            if (location != null) {
                times = domain.getPrayerTimeMap(location)
                formattedTimes = domain.getStrPrayerTimeMap(location)
                yesterdayIshaa = domain.getYesterdayIshaa(location)
                formattedYesterdayIshaa = domain.getStrYesterdayIshaa(location)
                tomorrowFajr = domain.getTomorrowFajr(location)
                formattedTomorrowFajr = domain.getStrTomorrowFajr(location)
            }

            shouldCount = location != null && times.isNotEmpty()

            _uiState.update { it.withPrayers() }

            count()
        }
    }

    private fun count() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }

        val till =
            if (nextPrayerIsTomorrow) tomorrowFajr?.timeInMillis ?: return
            else times[nextPrayer]?.timeInMillis ?: return
        timer = object : CountDownTimer(
            /* millisInFuture = */ till - System.currentTimeMillis(),
            /* countDownInterval = */ 1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val previousPrayerTime =
                    if (nextPrayer == Prayer.FAJR) yesterdayIshaa
                    else times[previousPrayer]
                val nextPrayerTime =
                    if (nextPrayerIsTomorrow) tomorrowFajr
                    else times[nextPrayer]

                val timeFromPreviousPrayer =
                    if (nextPrayer == Prayer.FAJR)
                        System.currentTimeMillis() - (previousPrayerTime?.timeInMillis ?: System.currentTimeMillis())
                    else
                        System.currentTimeMillis() - (times[previousPrayer]?.timeInMillis ?: System.currentTimeMillis())
                val timeFromPreviousPrayerHours = timeFromPreviousPrayer / (60 * 60 * 1000) % 24
                val timeFromPreviousPrayerMinutes = timeFromPreviousPrayer / (60 * 1000) % 60
                val timeFromPreviousPrayerSeconds = timeFromPreviousPrayer / 1000 % 60
                val timeFromPreviousPrayerHms = String.format(
                    Locale.US,
                    "%02d:%02d:%02d",
                    timeFromPreviousPrayerHours,
                    timeFromPreviousPrayerMinutes,
                    timeFromPreviousPrayerSeconds
                )

                val timeToNextPrayerHours = millisUntilFinished / (60 * 60 * 1000) % 24
                val timeToNextPrayerMinutes = millisUntilFinished / (60 * 1000) % 60
                val timeToNextPrayerSeconds = millisUntilFinished / 1000 % 60
                val timeToNextPrayerHms = String.format(
                    Locale.US,
                    "%02d:%02d:%02d",
                    timeToNextPrayerHours,
                    timeToNextPrayerMinutes,
                    timeToNextPrayerSeconds
                )

                viewModelScope.launch {
                    _uiState.update { it.copy(
                        passed = translateTimeNums(
                            string = timeFromPreviousPrayerHms,
                            language = it.language,
                            numeralsLanguage = it.numeralsLanguage
                        ),
                        remaining = translateTimeNums(
                            string = timeToNextPrayerHms,
                            language = it.language,
                            numeralsLanguage = it.numeralsLanguage
                        ),
                        previousPrayerTime = previousPrayerTime?.let { TimeOfDay.fromCalendar(it) },
                        nextPrayerTime = nextPrayerTime?.let { TimeOfDay.fromCalendar(it) }
                    )}
                }
            }

            override fun onFinish() {
                recount()
            }
        }.start()
    }

    private fun formatRecitationsTime(
        millis: Long,
        language: Language,
        numeralsLanguage: Language
    ): String {
        val hours = millis / (60 * 60 * 1000) % 24
        val minutes = millis / (60 * 1000) % 60
        val seconds = millis / 1000 % 60

        return translateTimeNums(
            string = String.format(
                Locale.US, "%02d:%02d:%02d",
                hours, minutes, seconds
            ),
            language = language,
            numeralsLanguage = numeralsLanguage
        )
    }

}