package bassamalim.hidaya.features.onboarding

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.HighLatitudesAdjustmentMethod
import bassamalim.hidaya.core.enums.PrayerTimeCalculationMethod
import bassamalim.hidaya.core.enums.PrayerTimeJuristicMethod
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.features.settings.AppearanceSettings
import bassamalim.hidaya.features.settings.MenuSetting
import bassamalim.hidaya.features.settings.SettingsSection

private val LogoSize = 96.dp

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!
    val dims = MaterialTheme.dimensions

    Scaffold(
        // Continue stays reachable at the bottom however long the settings run
        bottomBar = {
            Button(
                onClick = viewModel::onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = dims.spaceXl, vertical = dims.spaceLg)
            ) {
                Text(
                    text = stringResource(R.string.continue_action),
                    modifier = Modifier.padding(vertical = dims.spaceXs),
                    style = MaterialTheme.appTypography.button
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dims.screenPaddingHorizontal,
                    vertical = dims.spaceXl
                )
        ) {
            WelcomeHeader()

            SettingsSection(title = stringResource(R.string.appearance)) {
                AppearanceSettings(
                    selectedLanguage = state.language,
                    onLanguageChange = { language ->
                        viewModel.onLanguageChange(language, activity)
                    },
                    selectedNumeralsLanguage = state.numeralsLanguage,
                    onNumeralsLanguageChange = viewModel::onNumeralsLanguageChange,
                    selectedTimeFormat = state.timeFormat,
                    onTimeFormatChange = viewModel::onTimeFormatChange,
                    selectedTheme = state.theme,
                    onThemeChange = viewModel::onThemeChange,
                    numeralsLanguage = state.numeralsLanguage
                )
            }

            // These terms mean little to most people, so the hint reassures them the defaults
            // are a safe choice
            SettingsSection(
                title = stringResource(R.string.prayer_time_settings),
                description = stringResource(R.string.prayer_settings_hint)
            ) {
                MenuSetting(
                    selection = state.calculationMethod,
                    items = PrayerTimeCalculationMethod.entries.toTypedArray(),
                    entries = stringArrayResource(R.array.prayer_times_calc_method_entries),
                    title = stringResource(R.string.calculation_method_title),
                    onSelection = viewModel::onCalculationMethodChange
                )

                MenuSetting(
                    selection = state.juristicMethod,
                    items = PrayerTimeJuristicMethod.entries.toTypedArray(),
                    entries = stringArrayResource(R.array.juristic_method_entries),
                    title = stringResource(R.string.juristic_method_title),
                    onSelection = viewModel::onJuristicMethodChange
                )

                MenuSetting(
                    selection = state.highLatitudesAdjustment,
                    items = HighLatitudesAdjustmentMethod.entries.toTypedArray(),
                    entries = stringArrayResource(R.array.high_lat_adjustment_entries),
                    title = stringResource(R.string.high_lat_adjustment_title),
                    onSelection = viewModel::onHighLatitudesAdjustmentChange
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(LogoSize)
                .clip(RoundedCornerShape(dims.radiusLg * 1.5f))
                .background(colorResource(R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(LogoSize)
                    .padding(dims.spaceSm)
            )
        }

        Spacer(Modifier.height(dims.spaceLg))

        Text(
            text = stringResource(R.string.welcome_message),
            style = MaterialTheme.appTypography.display,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
