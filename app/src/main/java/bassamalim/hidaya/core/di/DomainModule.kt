package bassamalim.hidaya.core.di

import android.app.Application
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.BooksRepository
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
import bassamalim.hidaya.core.helpers.Alarm
import bassamalim.hidaya.features.about.AboutDomain
import bassamalim.hidaya.features.books.bookChaptersMenu.BookChaptersDomain
import bassamalim.hidaya.features.books.bookReader.BookReaderDomain
import bassamalim.hidaya.features.books.bookSearcher.BookSearcherDomain
import bassamalim.hidaya.features.books.booksMenu.BooksMenuDomain
import bassamalim.hidaya.features.books.booksMenuFilter.BooksMenuFilterDomain
import bassamalim.hidaya.features.dateConverter.DateConverterDomain
import bassamalim.hidaya.features.dateEditor.DateEditorDomain
import bassamalim.hidaya.features.hijriDatePicker.HijriDatePickerDomain
import bassamalim.hidaya.features.home.HomeDomain
import bassamalim.hidaya.features.leaderboard.LeaderboardDomain
import bassamalim.hidaya.features.locationPicker.LocationPickerDomain
import bassamalim.hidaya.features.locator.LocatorDomain
import bassamalim.hidaya.features.main.MainDomain
import bassamalim.hidaya.features.onboarding.OnboardingDomain
import bassamalim.hidaya.features.prayers.board.PrayersBoardDomain
import bassamalim.hidaya.features.prayers.extraReminderSettings.PrayerExtraReminderSettingsDomain
import bassamalim.hidaya.features.prayers.notificationSettings.PrayerNotificationSettingsDomain
import bassamalim.hidaya.features.qibla.QiblaDomain
import bassamalim.hidaya.features.quiz.result.QuizResultDomain
import bassamalim.hidaya.features.quiz.test.QuizTestDomain
import bassamalim.hidaya.features.quran.reader.QuranReaderDomain
import bassamalim.hidaya.features.quran.settings.QuranSettingsDomain
import bassamalim.hidaya.features.quran.surasMenu.QuranSurasDomain
import bassamalim.hidaya.features.radio.RadioDomain
import bassamalim.hidaya.features.recitations.player.RecitationPlayerDomain
import bassamalim.hidaya.features.recitations.recitersMenu.RecitationRecitersMenuDomain
import bassamalim.hidaya.features.recitations.recitersMenuFilter.RecitersMenuFilterDomain
import bassamalim.hidaya.features.recitations.surasMenu.RecitationSurasMenuDomain
import bassamalim.hidaya.features.remembrances.reader.RemembranceReaderDomain
import bassamalim.hidaya.features.remembrances.remembrancesMenu.RemembrancesMenuDomain
import bassamalim.hidaya.features.settings.SettingsDomain
import bassamalim.hidaya.features.tv.TvDomain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides @Singleton
    fun provideAboutDomain(
        appStateRepository: AppStateRepository,
        booksRepository: BooksRepository,
        prayersRepository: PrayersRepository,
        quranRepository: QuranRepository
    ) = AboutDomain(appStateRepository, booksRepository, prayersRepository, quranRepository)

    @Provides @Singleton
    fun provideBookChaptersDomain(booksRepository: BooksRepository) =
        BookChaptersDomain(booksRepository)

    @Provides @Singleton
    fun provideBooksMenuFilterDomain(booksRepository: BooksRepository) =
        BooksMenuFilterDomain(booksRepository)

    @Provides @Singleton
    fun provideBookReaderDomain(booksRepository: BooksRepository) =
        BookReaderDomain(booksRepository)

    @Provides @Singleton
    fun provideBookSearcherDomain(booksRepository: BooksRepository) =
        BookSearcherDomain(booksRepository)

    @Provides @Singleton
    fun provideBooksMenuDomain(
        booksRepository: BooksRepository,
        analyticsRepository: AnalyticsRepository
    ) = BooksMenuDomain(booksRepository, analyticsRepository)

    @Provides @Singleton
    fun provideDateConverterDomain(
        appSettingsRepository: AppSettingsRepository,
        appStateRepository: AppStateRepository
    ) = DateConverterDomain(appSettingsRepository, appStateRepository)

    @Provides @Singleton
    fun provideDateEditorDomain(appSettingsRepository: AppSettingsRepository) =
        DateEditorDomain(appSettingsRepository)

    @Provides @Singleton
    fun provideHijriDatePickerDomain(
        appSettingsRepository: AppSettingsRepository,
        appStateRepository: AppStateRepository
    ) = HijriDatePickerDomain(appSettingsRepository, appStateRepository)

    @Provides @Singleton
    fun provideHomeDomain(
        app: Application,
        prayersRepository: PrayersRepository,
        locationRepository: LocationRepository,
        quranRepository: QuranRepository,
        userRepository: UserRepository,
        appSettingsRepository: AppSettingsRepository,
        analyticsRepository: AnalyticsRepository
    ) = HomeDomain(
        app,
        prayersRepository,
        locationRepository,
        quranRepository,
        userRepository,
        appSettingsRepository,
        analyticsRepository
    )

    @Provides @Singleton
    fun provideLeaderboardDomain(
        app: Application,
        userRepository: UserRepository,
        appSettingsRepository: AppSettingsRepository
    ) = LeaderboardDomain(app, userRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideLocationPickerDomain(locationRepository: LocationRepository) =
        LocationPickerDomain(locationRepository)

    @Provides @Singleton
    fun provideLocatorDomain(
        app: Application,
        locationRepository: LocationRepository
    ) = LocatorDomain(app, locationRepository)

    @Provides @Singleton
    fun provideMainDomain(
        appStateRepository: AppStateRepository,
        appSettingsRepository: AppSettingsRepository
    ) = MainDomain(appStateRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideOnboardingDomain(
        appStateRepository: AppStateRepository,
        appSettingsRepository: AppSettingsRepository,
        prayersRepository: PrayersRepository
    ) = OnboardingDomain(appStateRepository, appSettingsRepository, prayersRepository)

    @Provides @Singleton
    fun providePrayerReminderDomain(
        prayersRepository: PrayersRepository,
        notificationsRepository: NotificationsRepository,
        appSettingsRepository: AppSettingsRepository,
        alarm: Alarm
    ) = PrayerExtraReminderSettingsDomain(
        prayersRepository,
        notificationsRepository,
        appSettingsRepository,
        alarm
    )

    @Provides @Singleton
    fun providePrayersBoardDomain(
        prayersRepository: PrayersRepository,
        locationRepository: LocationRepository,
        notificationsRepository: NotificationsRepository,
        appStateRepository: AppStateRepository,
        appSettingsRepository: AppSettingsRepository,
        prayerTimesReportRepository: PrayerTimesReportRepository,
    ) = PrayersBoardDomain(
        prayersRepository,
        locationRepository,
        notificationsRepository,
        appStateRepository,
        appSettingsRepository,
        prayerTimesReportRepository,
    )

    @Provides @Singleton
    fun providePrayerSettingsDomain(
        prayersRepository: PrayersRepository,
        notificationsRepository: NotificationsRepository,
        appSettingsRepository: AppSettingsRepository,
        alarm: Alarm
    ) = PrayerNotificationSettingsDomain(
        prayersRepository,
        notificationsRepository,
        appSettingsRepository,
        alarm
    )

    @Provides @Singleton
    fun provideQiblaDomain(
        app: Application,
        locationRepository: LocationRepository,
        appSettingsRepository: AppSettingsRepository
    ) = QiblaDomain(app, locationRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideQuizResultDomain(
        appSettingsRepository: AppSettingsRepository
    ) = QuizResultDomain(appSettingsRepository)

    @Provides @Singleton
    fun provideQuizTestDomain(
        quizRepository: QuizRepository,
        appSettingsRepository: AppSettingsRepository
    ) = QuizTestDomain(quizRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideQuranMenu(
        quranRepository: QuranRepository,
        appSettingsRepository: AppSettingsRepository,
        analyticsRepository: AnalyticsRepository
    ) = QuranSurasDomain(quranRepository, appSettingsRepository, analyticsRepository)

    @Provides @Singleton
    fun provideQuranReaderDomain(
        app: Application,
        quranRepository: QuranRepository,
        appSettingsRepository: AppSettingsRepository,
        userRepository: UserRepository,
        analyticsRepository: AnalyticsRepository,
        @ApplicationScope appScope: CoroutineScope
    ) = QuranReaderDomain(
        app,
        quranRepository,
        appSettingsRepository,
        userRepository,
        analyticsRepository,
        appScope
    )

    @Provides @Singleton
    fun provideQuranSettings(
        quranRepository: QuranRepository,
        recitationsRepository: RecitationsRepository,
        appSettingsRepository: AppSettingsRepository
    ) = QuranSettingsDomain(quranRepository, recitationsRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideRadioDomain(
        liveContentRepository: LiveContentRepository
    ) = RadioDomain(liveContentRepository)

    @Provides @Singleton
    fun provideRecitationPlayerDomain(
        app: Application,
        recitationsRepository: RecitationsRepository,
        quranRepository: QuranRepository
    ) = RecitationPlayerDomain(app, recitationsRepository, quranRepository)

    @Provides @Singleton
    fun provideRecitationRecitersMenuDomain(
        app: Application,
        recitationsRepository: RecitationsRepository,
        quranRepository: QuranRepository,
        appSettingsRepository: AppSettingsRepository
    ) = RecitationRecitersMenuDomain(
        app,
        recitationsRepository,
        quranRepository,
        appSettingsRepository
    )

    @Provides @Singleton
    fun provideRecitationRecitersMenuFilterDomain(recitationsRepository: RecitationsRepository) =
        RecitersMenuFilterDomain(recitationsRepository)

    @Provides @Singleton
    fun provideRecitationSurasMenuDomain(
        app: Application,
        recitationsRepository: RecitationsRepository,
        quranRepository: QuranRepository,
        analyticsRepository: AnalyticsRepository
    ) = RecitationSurasMenuDomain(app, recitationsRepository, quranRepository, analyticsRepository)

    @Provides @Singleton
    fun provideRemembranceReaderDomain(
        remembrancesRepository: RemembrancesRepository,
        appSettingsRepository: AppSettingsRepository
    ) = RemembranceReaderDomain(remembrancesRepository, appSettingsRepository)

    @Provides @Singleton
    fun provideRemembrancesMenuDomain(
        remembrancesRepository: RemembrancesRepository,
        analyticsRepository: AnalyticsRepository
    ) = RemembrancesMenuDomain(remembrancesRepository, analyticsRepository)

    @Provides @Singleton
    fun provideSettingsDomain(
        appSettingsRepository: AppSettingsRepository,
        prayersRepository: PrayersRepository,
        notificationsRepository: NotificationsRepository,
        locationRepository: LocationRepository,
        alarm: Alarm
    ) = SettingsDomain(
        appSettingsRepository,
        prayersRepository,
        notificationsRepository,
        locationRepository,
        alarm
    )

    @Provides @Singleton
    fun provideTvDomain(
        liveContentRepository: LiveContentRepository,
        analyticsRepository: AnalyticsRepository
    ) = TvDomain(liveContentRepository, analyticsRepository)

}