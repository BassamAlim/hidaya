package bassamalim.hidaya

import android.app.Application
import bassamalim.hidaya.core.utils.LangUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        LangUtils.restoreAppLocale(this)
    }

}