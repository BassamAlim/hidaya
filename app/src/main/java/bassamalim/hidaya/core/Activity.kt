package bassamalim.hidaya.core

import android.Manifest
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.enums.LocationType
import bassamalim.hidaya.core.enums.Theme
import bassamalim.hidaya.core.nav.Navigation
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.startup.DbRecoveryHelper
import bassamalim.hidaya.core.startup.LocationStartupHelper
import bassamalim.hidaya.core.startup.PermissionsHelper
import bassamalim.hidaya.core.startup.PostLaunchInitializer
import bassamalim.hidaya.core.startup.StartActionHandler
import bassamalim.hidaya.core.startup.ThemeApplier
import bassamalim.hidaya.core.ui.theme.AppTheme
import bassamalim.hidaya.core.utils.LangUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Activity : AppCompatActivity() {

    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var appStateRepository: AppStateRepository
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var dbRecoveryHelper: DbRecoveryHelper
    @Inject lateinit var startActionHandler: StartActionHandler
    @Inject lateinit var themeApplier: ThemeApplier
    @Inject lateinit var locationStartupHelper: LocationStartupHelper
    @Inject lateinit var postLaunchInitializer: PostLaunchInitializer
    @Inject lateinit var permissionsHelper: PermissionsHelper

    private var shouldOnboard = false
    private var startRoute: String? = null
    private lateinit var language: Language
    private lateinit var theme: Flow<Theme>
    private lateinit var initialTheme: Theme

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = setupSplashScreen()
        super.onCreate(savedInstanceState)
        LangUtils.saveAppLocale(this)  // so alarm/service processes can restore it

        lifecycleScope.launch {
            try {
                initializeApp(splashScreen = splashScreen, savedInstanceState = savedInstanceState)
            } catch (e: Exception) {
                Log.e(Globals.TAG, "Error during app initialization", e)
                // Fallback to basic app launch
                launchApp()
            }
        }
    }

    private fun setupSplashScreen() = installSplashScreen().apply {
        setKeepOnScreenCondition { true }
    }

    private suspend fun initializeApp(splashScreen: SplashScreen, savedInstanceState: Bundle?) {
        initializeTheme()
        requestNotificationPermission()
        splashScreen.setKeepOnScreenCondition { false }

        val isLaunch = savedInstanceState == null
        startRoute = intent.getStringExtra("start_route")

        if (isLaunch) dbRecoveryHelper.testAndReviveIfNeeded(this)

        initializeUserSettings()

        if (isLaunch) handleStartupFlow()
        else launchApp()
    }

    private suspend fun initializeTheme() {
        theme = appSettingsRepository.getTheme()
        initialTheme = theme.first()
        themeApplier.apply(this, initialTheme)
    }

    private suspend fun initializeUserSettings() {
        shouldOnboard = !appStateRepository.isOnboardingCompleted().first()
        language = LangUtils.getAppLanguage()
    }

    private suspend fun handleStartupFlow() {
        try {
            val result = startActionHandler.handle(this, intent)
            if (result.overrideRoute != null) {
                startRoute = result.overrideRoute
            }

            if (shouldOnboard) {
                launchApp()
                postLaunch()
            }
            else {
                getLocationAndLaunch()
            }
        } catch (e: Exception) {
            Log.e(Globals.TAG, "Error in startup flow", e)
            launchApp()
            postLaunch()
        }
    }

    private suspend fun getLocationAndLaunch() {
        val location = locationStartupHelper.getStoredLocationType()
        if (location != null && location == LocationType.AUTO) {
            if (locationStartupHelper.hasFineAndCoarsePermission(this)) locate()
            else {
                // Launchers must be registered on the Activity before STARTED.
                locationPermissionRequestLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
        else {
            launchApp()
            postLaunch()
        }
    }

    // Launchers must be registered on the Activity before STARTED.
    private val locationPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onLocationPermissionRequestResult(result)
    }

    // Launchers must be registered on the Activity before STARTED.
    private val notificationPermissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onNotificationPermissionRequestResult(result)
    }

    private fun onLocationPermissionRequestResult(result: Map<String, Boolean>) {
        // Skip if this is a callback for background location or usage stats
        if (result.keys.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ||
            result.keys.contains(Manifest.permission.PACKAGE_USAGE_STATS)) {
            return
        }

        val fineLoc = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLoc = result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLoc && coarseLoc) {
            locate()
            requestExtraPermissions()
        }
        else {
            Log.w(Globals.TAG, "Location permissions denied")
            launchApp()
            postLaunch()
        }
    }

    private fun onNotificationPermissionRequestResult(result: Map<String, Boolean>) {
        val notificationGranted = result[Manifest.permission.POST_NOTIFICATIONS] ?: false
        if (notificationGranted) {
            Log.d(Globals.TAG, "Notification permission granted")
        }
        else {
            Log.w(Globals.TAG, "Notification permission denied")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionRequestLauncher.launch(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    private fun requestExtraPermissions() {
        val permissions = permissionsHelper.extraPermissionsToRequest(this)

        if (permissions.isNotEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.choose_allow_all_the_time),
                Toast.LENGTH_LONG
            ).show()

            locationPermissionRequestLauncher.launch(permissions.toTypedArray())
        }

        permissionsHelper.openExactAlarmSettings(this)
    }

    private fun locate() {
        locationStartupHelper.fetchLastLocation(this) { location ->
            if (location != null) storeLocation(location)
            else Log.w(Globals.TAG, "Received null location, not storing")
            launchApp()
            postLaunch()
        }

        requestExtraPermissions()
    }

    private fun storeLocation(location: Location) {
        lifecycleScope.launch {
            locationStartupHelper.storeLocation(location)
        }
    }

    private fun launchApp() {
        setContent {
            val themeState by theme.collectAsState(
                initial = initialTheme,
                context = lifecycleScope.coroutineContext
            )

            themeApplier.apply(this, themeState)

            AppTheme(theme = themeState, direction = getDirection(language)) {
                Navigation(
                    navigator = navigator,
                    thenTo = startRoute,
                    shouldOnboard = shouldOnboard
                )
            }
        }
    }

    private fun getDirection(language: Language) = when (language) {
        Language.ARABIC -> LayoutDirection.Rtl
        Language.ENGLISH -> LayoutDirection.Ltr
    }

    private fun postLaunch() {
        lifecycleScope.launch {
            postLaunchInitializer.run(this@Activity)
        }
    }

}
