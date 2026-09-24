package bassamalim.hidaya.core.utils

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import bassamalim.hidaya.core.data.dataSources.room.AppDatabase
import bassamalim.hidaya.core.Globals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

object DbUtils {

    suspend fun shouldReviveDb(
        lastDbVersion: Int,
        test: () -> List<Any>,
        dispatcher: CoroutineDispatcher
    ): Boolean {
        if (Globals.DB_VERSION > lastDbVersion) return true

        return try {  // if there is a problem in the db it will cause an error
            withContext(dispatcher) {
                test().isEmpty()
            }
        } catch (e: IllegalStateException) {
            e.report()
            Log.e(Globals.TAG, "DB Error: ${e.message}")
            e.printStackTrace()
            true
        } catch (e: SQLiteException) {
            e.report()
            Log.e(Globals.TAG, "DB Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Closes the app's Room instance, then deletes the file. Room re-copies it from the asset
     * on next open, so callers must restart the process before touching the DB again.
     */
    fun resetDB(context: Context, database: AppDatabase) {
        database.close()
        context.deleteDatabase(Globals.DB_NAME)
        Log.i(Globals.TAG, "Database Deleted")
    }

    suspend fun restoreDbData(
        suraFavorites: Map<Int, Boolean>,
        setSuraFavorites: suspend (Map<Int, Boolean>) -> Unit,
        reciterFavorites: Map<Int, Boolean>,
        setReciterFavorites: suspend (Map<Int, Boolean>) -> Unit,
        remembranceFavorites: Map<Int, Boolean>,
        setRemembranceFavorites: suspend (Map<Int, Boolean>) -> Unit,
    ) {
        if (suraFavorites.isNotEmpty())
            setSuraFavorites(suraFavorites)

        if (reciterFavorites.isNotEmpty())
            setReciterFavorites(reciterFavorites)

        if (remembranceFavorites.isNotEmpty())
            setRemembranceFavorites(remembranceFavorites)
    }

}