package bassamalim.hidaya

import android.app.Application
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.widgets.refreshWidgets
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class App: Application() {

    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        LangUtils.restoreAppLocale(this)

        // Widgets show times in these formats; the app language is handled by the Activity
        combine(
            appSettingsRepository.getNumeralsLanguage(),
            appSettingsRepository.getTimeFormat(),
            ::Pair
        )
            .distinctUntilChanged()
            .drop(1)  // the current values, which the widgets already show
            .onEach { refreshWidgets(this) }
            .launchIn(appScope)
    }

}
