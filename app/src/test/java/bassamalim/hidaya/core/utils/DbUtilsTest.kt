package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.Globals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the bundled database gets rebuilt. The exception paths log, so they need a device. */
class DbUtilsTest {

    @Test
    fun `a newer bundled database is revived`() = runTest {
        assertTrue(
            DbUtils.shouldReviveDb(
                lastDbVersion = Globals.DB_VERSION - 1,
                test = { listOf("Al-Fatiha") },
                dispatcher = StandardTestDispatcher(testScheduler)
            )
        )
    }

    @Test
    fun `an empty database is revived`() = runTest {
        assertTrue(
            DbUtils.shouldReviveDb(
                lastDbVersion = Globals.DB_VERSION,
                test = { emptyList() },
                dispatcher = StandardTestDispatcher(testScheduler)
            )
        )
    }

    @Test
    fun `a current, readable database is kept`() = runTest {
        assertFalse(
            DbUtils.shouldReviveDb(
                lastDbVersion = Globals.DB_VERSION,
                test = { listOf("Al-Fatiha") },
                dispatcher = StandardTestDispatcher(testScheduler)
            )
        )
    }

}
