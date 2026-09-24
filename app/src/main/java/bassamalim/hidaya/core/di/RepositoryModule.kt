package bassamalim.hidaya.core.di

import android.app.Application
import android.content.Context
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.AppSettingsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.AppStatePreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.BooksPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.NotificationsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.PrayersPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.QuranPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.RecitationsPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.RemembrancePreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.UserPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.room.daos.BooksDao
import bassamalim.hidaya.core.data.dataSources.room.daos.CitiesDao
import bassamalim.hidaya.core.data.dataSources.room.daos.CountriesDao
import bassamalim.hidaya.core.data.dataSources.room.daos.QuizAnswersDao
import bassamalim.hidaya.core.data.dataSources.room.daos.QuizQuestionsDao
import bassamalim.hidaya.core.data.dataSources.room.daos.RecitationNarrationsDao
import bassamalim.hidaya.core.data.dataSources.room.daos.RemembranceCategoriesDao
import bassamalim.hidaya.core.data.dataSources.room.daos.RemembrancePassagesDao
import bassamalim.hidaya.core.data.dataSources.room.daos.RemembrancesDao
import bassamalim.hidaya.core.data.dataSources.room.daos.SuraRecitersDao
import bassamalim.hidaya.core.data.dataSources.room.daos.SurasDao
import bassamalim.hidaya.core.data.dataSources.room.daos.VerseRecitationsDao
import bassamalim.hidaya.core.data.dataSources.room.daos.VerseRecitersDao
import bassamalim.hidaya.core.data.dataSources.room.daos.VersesDao
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.LiveContentRepository
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.NotificationsRepository
import bassamalim.hidaya.core.data.repositories.PrayerTimesReportRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.data.repositories.QuizRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.RecitationsRepository
import bassamalim.hidaya.core.data.repositories.RemembrancesRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.logger.Logger
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideAppSettingsRepository(
        appSettingsPreferencesDataSource: AppSettingsPreferencesDataSource,
        @ApplicationScope scope: CoroutineScope
    ) = AppSettingsRepository(appSettingsPreferencesDataSource, scope)

    @Provides @Singleton
    fun provideAppStateRepository(
        app: Application,
        appStatePreferencesDataSource: AppStatePreferencesDataSource,
        @ApplicationScope scope: CoroutineScope
    ) = AppStateRepository(app, appStatePreferencesDataSource, scope)

    @Provides @Singleton
    fun provideBooksRepository(
        app: Application,
        booksDao: BooksDao,
        booksPreferencesDataSource: BooksPreferencesDataSource,
        appSettingsRepository: AppSettingsRepository,
        gson: Gson,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope
    ) = BooksRepository(
        app,
        booksDao,
        booksPreferencesDataSource,
        appSettingsRepository,
        gson,
        dispatcher,
        scope
    )

    @Provides @Singleton
    fun provideLiveContentRepository(
        remoteConfig: FirebaseRemoteConfig,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = LiveContentRepository(remoteConfig, dispatcher)

    @Provides @Singleton
    fun provideLocationRepository(
        userPreferencesDataSource: UserPreferencesDataSource,
        countriesDao: CountriesDao,
        citiesDao: CitiesDao,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope
    ) = LocationRepository(userPreferencesDataSource, countriesDao, citiesDao, dispatcher, scope)

    @Provides @Singleton
    fun provideNotificationsRepository(
        notificationsPreferencesDataSource: NotificationsPreferencesDataSource,
        @ApplicationScope scope: CoroutineScope
    ) = NotificationsRepository(notificationsPreferencesDataSource, scope)

    @Provides @Singleton
    fun providePrayersRepository(
        app: Application,
        prayersPreferencesDataSource: PrayersPreferencesDataSource,
        @ApplicationScope scope: CoroutineScope
    ) = PrayersRepository(app, prayersPreferencesDataSource, scope)

    @Provides @Singleton
    fun provideQuizRepository(
        quizQuestionsDao: QuizQuestionsDao,
        quizAnswersDao: QuizAnswersDao,
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ) = QuizRepository(quizQuestionsDao, quizAnswersDao, dispatcher)

    @Provides @Singleton
    fun provideQuranRepository(
        quranPreferencesDataSource: QuranPreferencesDataSource,
        surasDao: SurasDao,
        versesDao: VersesDao,
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ) = QuranRepository(quranPreferencesDataSource, surasDao, versesDao, dispatcher)

    @Provides @Singleton
    fun provideRecitationsRepository(
        app: Application,
        recitationsPreferencesDataSource: RecitationsPreferencesDataSource,
        suraRecitersDao: SuraRecitersDao,
        verseRecitationsDao: VerseRecitationsDao,
        verseRecitersDao: VerseRecitersDao,
        recitationNarrationsDao: RecitationNarrationsDao,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
        gson: Gson
    ) = RecitationsRepository(
        app,
        recitationsPreferencesDataSource,
        suraRecitersDao,
        verseRecitationsDao,
        verseRecitersDao,
        recitationNarrationsDao,
        dispatcher,
        scope,
        gson
    )

    @Provides @Singleton
    fun provideRemembrancesRepository(
        remembrancePreferencesDataSource: RemembrancePreferencesDataSource,
        remembranceCategoriesDao: RemembranceCategoriesDao,
        remembrancesDao: RemembrancesDao,
        remembrancePassagesDao: RemembrancePassagesDao,
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope
    ) = RemembrancesRepository(
        remembrancePreferencesDataSource,
        remembranceCategoriesDao,
        remembrancesDao,
        remembrancePassagesDao,
        dispatcher,
        scope
    )

    @Provides @Singleton
    fun providePrayerTimesReportRepository(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ) = PrayerTimesReportRepository(context, firestore)

    @Provides @Singleton
    fun provideUserRepository(
        app: Application,
        userPreferencesDataSource: UserPreferencesDataSource,
        firestore: FirebaseFirestore,
        @ApplicationScope scope: CoroutineScope
    ) = UserRepository(app, userPreferencesDataSource, firestore, scope)

    @Provides @Singleton
    fun provideFirebaseAnalyticsRepository(
        firebaseAnalytics: FirebaseAnalytics,
    ) = AnalyticsRepository(firebaseAnalytics)

}