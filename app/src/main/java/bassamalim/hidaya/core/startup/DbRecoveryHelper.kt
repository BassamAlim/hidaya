package bassamalim.hidaya.core.startup

import android.app.Activity
import android.util.Log
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.dataSources.room.daos.SurasDao
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.RemembrancesRepository
import bassamalim.hidaya.core.di.IoDispatcher
import bassamalim.hidaya.core.utils.ActivityUtils
import bassamalim.hidaya.core.utils.DbUtils
import bassamalim.hidaya.core.utils.report
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DbRecoveryHelper @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val quranRepository: QuranRepository,
    private val recitationsRepository: RecitationsRepository,
    private val remembrancesRepository: RemembrancesRepository,
    private val surasDao: SurasDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    suspend fun testAndReviveIfNeeded(activity: Activity) {
        try {
            val shouldReviveDb = DbUtils.shouldReviveDb(
                lastDbVersion = appStateRepository.getLastDbVersion().first(),
                test = surasDao::getPlainNamesAr,
                dispatcher = dispatcher
            )

            if (shouldReviveDb) {
                Log.d(Globals.TAG, "Database needs revival, resetting...")
                DbUtils.resetDB(activity)
                appStateRepository.setLastDbVersion(Globals.DB_VERSION)
                ActivityUtils.restartApplication(activity)
            }
            else {
                Log.d(Globals.TAG, "Database is up to date")
            }
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Error during database test", e)
        }
    }

    suspend fun restoreData() {
        try {
            DbUtils.restoreDbData(
                suraFavorites = quranRepository.getSuraFavoritesBackup().first(),
                setSuraFavorites = quranRepository::setSuraFavorites,
                reciterFavorites = recitationsRepository.getReciterFavoritesBackup().first(),
                setReciterFavorites = recitationsRepository::setReciterFavorites,
                remembranceFavorites = remembrancesRepository.getFavoritesBackup().first(),
                setRemembranceFavorites = remembrancesRepository::setFavorites,
            )
            Log.d(Globals.TAG, "Database data restored successfully")
        } catch (e: Exception) {
            e.report()
            Log.e(Globals.TAG, "Failed to restore database data", e)
        }
    }

}
