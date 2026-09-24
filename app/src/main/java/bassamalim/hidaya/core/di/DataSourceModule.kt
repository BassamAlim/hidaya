package bassamalim.hidaya.core.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.room.Room
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.dataSources.preferences.PreferencesFileNames
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.AppSettingsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.AppStatePreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.BooksPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.NotificationsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.PrayersPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.QuranPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.RecitationsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.RemembrancePreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.UserPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppSettingsPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.AppStatePreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.BooksPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.NotificationsPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.PrayersPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.QuranPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.RecitationsPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.RemembrancesPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.objects.UserPreferences
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.AppSettingsPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.AppStatePreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.BooksPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.NotificationsPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.PrayersPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.QuranPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.RecitationsPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.RemembrancesPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.preferences.serializers.UserPreferencesSerializer
import bassamalim.hidaya.core.data.dataSources.room.AppDatabase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides @Singleton
    fun provideAppSettingsPreferencesDataSource(@ApplicationContext appContext: Context) =
        AppSettingsPreferencesDataSource(
            DataStoreFactory.create(
                serializer = AppSettingsPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { AppSettingsPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.APP_SETTINGS_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideAppStatePreferencesDataSource(@ApplicationContext appContext: Context) =
        AppStatePreferencesDataSource(
            DataStoreFactory.create(
                serializer = AppStatePreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { AppStatePreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.APP_STATE_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideBooksPreferencesDataSource(@ApplicationContext appContext: Context) =
        BooksPreferencesDataSource(
            DataStoreFactory.create(
                serializer = BooksPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { BooksPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.BOOKS_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideNotificationsPreferencesDataSource(@ApplicationContext appContext: Context) =
        NotificationsPreferencesDataSource(
            DataStoreFactory.create(
                serializer = NotificationsPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { NotificationsPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.NOTIFICATIONS_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun providePrayersPreferencesDataSource(@ApplicationContext appContext: Context) =
        PrayersPreferencesDataSource(
            DataStoreFactory.create(
                serializer = PrayersPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { PrayersPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.PRAYERS_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideQuranPreferencesDataSource(@ApplicationContext appContext: Context) =
        QuranPreferencesDataSource(
            DataStoreFactory.create(
                serializer = QuranPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { QuranPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.QURAN_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideRecitationsPreferencesDataSource(@ApplicationContext appContext: Context) =
        RecitationsPreferencesDataSource(
            DataStoreFactory.create(
                serializer = RecitationsPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { RecitationsPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.RECITATIONS_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideRemembrancesPreferencesDataSource(@ApplicationContext appContext: Context) =
        RemembrancePreferencesDataSource(
            DataStoreFactory.create(
                serializer = RemembrancesPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { RemembrancesPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.REMEMBRANCES_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideUserPreferencesDataSource(@ApplicationContext appContext: Context) =
        UserPreferencesDataSource(
            DataStoreFactory.create(
                serializer = UserPreferencesSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { UserPreferences() }
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    appContext.dataStoreFile(PreferencesFileNames.USER_PREFERENCES_NAME)
                }
            )
        )

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(
            context = context.applicationContext,
            klass = AppDatabase::class.java,
            name = Globals.DB_NAME
        ).createFromAsset("databases/HidayaDB.db").build()

    @Provides
    fun provideBooksDao(database: AppDatabase) = database.booksDao()

    @Provides
    fun provideCitiesDao(database: AppDatabase) = database.citiesDao()

    @Provides
    fun provideCountriesDao(database: AppDatabase) = database.countriesDao()

    @Provides
    fun provideQuizAnswersDao(database: AppDatabase) = database.quizAnswersDao()

    @Provides
    fun provideQuizQuestionsDao(database: AppDatabase) = database.quizQuestionsDao()

    @Provides
    fun provideRecitationRecitersDao(database: AppDatabase) = database.suraRecitersDao()

    @Provides
    fun provideRecitationNarrationsDao(database: AppDatabase) = database.recitationNarrationsDao()

    @Provides
    fun provideRemembranceCategoriesDao(database: AppDatabase) = database.remembranceCategoriesDao()

    @Provides
    fun provideRemembrancePassagesDao(database: AppDatabase) = database.remembrancePassagesDao()

    @Provides
    fun provideRemembrancesDao(database: AppDatabase) = database.remembrancesDao()

    @Provides
    fun provideSurasDao(database: AppDatabase) = database.surasDao()

    @Provides
    fun provideVerseRecitationsDao(database: AppDatabase) = database.verseRecitationsDao()

    @Provides
    fun provideVerseRecitersDao(database: AppDatabase) = database.verseRecitersDao()

    @Provides
    fun provideVersesDao(database: AppDatabase) = database.versesDao()

    @Provides @Singleton
    fun provideFirestore() = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideRemoteConfig() = FirebaseRemoteConfig.getInstance()

    @Provides @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context) =
        FirebaseAnalytics.getInstance(context)

}