package bassamalim.hidaya.core.nav

import android.os.Bundle
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Results handed back to the previous screen, e.g. a picked hijri date or city. */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorResultsTest {

    private val navController = mockk<NavController>(relaxed = true)
    private val navigator = Navigator(analyticsRepository = mockk(relaxed = true)).apply {
        setController(navController)
    }

    private fun bundleWith(key: String) = mockk<Bundle> {
        every { containsKey(any()) } answers { firstArg<String>() == key }
    }

    @Test
    fun `a result reaches the collector of its key, again and again`() = runTest {
        val received = mutableListOf<Bundle>()
        navigator.results("selected_date").onEach { received += it }
            .launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        val first = bundleWith("selected_date")
        val second = bundleWith("selected_date")
        navigator.navigateBackWithResult(first)
        navigator.navigateBackWithResult(second)

        assertEquals(listOf(first, second), received)
        verify(exactly = 2) { navController.popBackStack() }
    }

    @Test
    fun `a result for another key is not delivered`() = runTest {
        val received = mutableListOf<Bundle>()
        navigator.results("selected_date").onEach { received += it }
            .launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        navigator.navigateBackWithResult(bundleWith("city_id"))

        assertEquals(emptyList<Bundle>(), received)
    }

    @Test
    fun `going back without a result still goes back`() = runTest {
        navigator.navigateBackWithResult(null)

        verify { navController.popBackStack() }
    }

}
