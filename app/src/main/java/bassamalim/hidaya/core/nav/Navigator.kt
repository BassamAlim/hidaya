package bassamalim.hidaya.core.nav

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.models.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val RESULT_KEY = "nav_result"
private const val DUPLICATE_WINDOW_MILLIS = 500L

class Navigator @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {

    private var navController: NavController? = null
    private var lastRoute: String? = null
    private var lastNavigationMillis = 0L

    // Called whenever an Activity's NavHost comes to the foreground, so the singleton always
    // targets the visible Activity (there can be several, e.g. one opened from a notification).
    fun setController(navController: NavController) {
        this.navController = navController
    }

    // Only clears if [navController] is still the current one; another Activity may own it now.
    fun clearController(navController: NavController) {
        if (this.navController === navController) this.navController = null
    }

    fun navigate(destination: Screen) {
        if (isDuplicate(destination.route)) return
        navController?.navigate(destination.route)

        analyticsRepository.trackEvent(AnalyticsEvent.ScreenView(destination.route))
    }

    fun navigate(destination: Screen, builder: NavOptionsBuilder.() -> Unit) {
        if (isDuplicate(destination.route)) return
        navController?.navigate(route = destination.route, builder = builder)

        analyticsRepository.trackEvent(AnalyticsEvent.ScreenView(destination.route))
    }

    /**
     * True for the second tap of a fast double tap on the same item, which would otherwise push
     * the same screen twice. Only exact repeats are dropped, so moving on to a different screen
     * right away is never blocked.
     */
    private fun isDuplicate(route: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val isDuplicate = route == lastRoute && now - lastNavigationMillis < DUPLICATE_WINDOW_MILLIS
        lastRoute = route
        lastNavigationMillis = now
        return isDuplicate
    }

    fun popBackStack() {
        navController?.popBackStack()
    }

    fun popBackStack(destination: Screen, inclusive: Boolean = false) {
        navController?.popBackStack(route = destination.route, inclusive = inclusive)
    }

    /** Hands [data] to the previous destination (read it with [SavedStateHandle.navResults]). */
    fun navigateBackWithResult(data: Bundle?) {
        val navController = navController ?: return
        if (data != null)
            navController.previousBackStackEntry?.savedStateHandle?.set(RESULT_KEY, data)
        navController.popBackStack()
    }

    fun getContext() = navController?.context

}

/** Results sent back to this destination via [Navigator.navigateBackWithResult]. */
fun SavedStateHandle.navResults(): Flow<Bundle> =
    getStateFlow<Bundle?>(RESULT_KEY, null)
        .filterNotNull()
        .onEach { remove<Bundle>(RESULT_KEY) }
