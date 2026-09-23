package bassamalim.hidaya.core.utils

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Room
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

    fun resetDB(context: Context) {
        context.deleteDatabase(Globals.DB_NAME)
        Log.i(Globals.TAG, "Database Deleted")

        Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = Globals.DB_NAME
        ).createFromAsset("databases/HidayaDB.db").build()

        Log.i(Globals.TAG, "Database Revived")
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