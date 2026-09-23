package bassamalim.hidaya.core.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.material3.ColorProviders
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.ui.theme.darkColorScheme
import bassamalim.hidaya.core.ui.theme.lightColorScheme
import bassamalim.hidaya.core.data.repositories.LocationRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.Location
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import bassamalim.hidaya.core.utils.PrayerTimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

private data class NextPrayerData(
    val name: String,
    val timeString: String,
    val remainingText: String
)

class NextPrayerWidget(
    private val appSettingsRepository: AppSettingsRepository,
    private val prayersRepository: PrayersRepository,
    private val locationRepository: LocationRepository,
    private val dispatcher: CoroutineDispatcher
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val context = context.withAppLocale()  // app language, not the device's (pre-API 33)

        val data = withContext(dispatcher) {
            getNextPrayerData(context)
        }

        provideContent {
            GlanceTheme(colors = ColorProviders(light = lightColorScheme, dark = darkColorScheme)) {
                WidgetContent(data, context)
            }
        }
    }

    @Composable
    private fun WidgetContent(data: NextPrayerData?, context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .clickable {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
        ) {
            if (data == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.error_fetching_data),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header strip
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(GlanceTheme.colors.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.next_prayer),
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    // Prayer name and time
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = data.name,
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Text(
                            text = data.timeString,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        Text(
                            text = data.remainingText,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun getNextPrayerData(context: Context): NextPrayerData? {
        val location = locationRepository.getLocation().first<Location?>() ?: return null
        val settings = prayersRepository.getPrayerTimesCalculatorSettings().first()
        val timeZoneId = locationRepository.getTimeZone(location.ids.cityId)
        val language = LangUtils.getAppLanguage()
        val numeralsLanguage = appSettingsRepository.getNumeralsLanguage().first()
        val timeFormat = appSettingsRepository.getTimeFormat().first()
        val prayerNames = context.resources.getStringArray(R.array.prayer_names)
        val now = Calendar.getInstance()

        // Try today; if all prayers have passed, fall back to tomorrow's Fajr
        for (dayOffset in 0..1) {
            val calendar = Calendar.getInstance().apply {
                if (dayOffset > 0) add(Calendar.DAY_OF_YEAR, 1)
            }
            val prayerTimes = PrayerTimeUtils.getPrayerTimes(
                settings = settings,
                selectedTimeZoneId = timeZoneId,
                location = location,
                calendar = calendar
            )
            val nextEntry = prayerTimes.entries.firstOrNull { (prayer, time) ->
                prayer != Prayer.SUNRISE && prayer != Prayer.SUNSET && time != null && time.after(now)
            } ?: continue

            val prayerTimeStrings = PrayerTimeUtils.formatPrayerTimes(
                prayerTimes = prayerTimes,
                language = language,
                numeralsLanguage = numeralsLanguage,
                timeFormat = timeFormat
            )

            val remainingMillis = nextEntry.value!!.timeInMillis - now.timeInMillis
            val hours = (remainingMillis / (1000L * 60 * 60)).toInt()
            val minutes = ((remainingMillis % (1000L * 60 * 60)) / (1000L * 60)).toInt()
            val remainingFormatted = if (hours > 0) "%d:%02d".format(hours, minutes)
                                     else "%d min".format(minutes)

            return NextPrayerData(
                name = getPrayerName(nextEntry.key, prayerNames),
                timeString = prayerTimeStrings[nextEntry.key] ?: "",
                remainingText = context.getString(R.string.remaining, remainingFormatted)
            )
        }
        return null
    }

    private fun getPrayerName(prayer: Prayer, names: Array<String>) = when (prayer) {
        Prayer.FAJR -> names[0]
        Prayer.SUNRISE -> names[1]
        Prayer.DHUHR -> names[2]
        Prayer.ASR -> names[3]
        Prayer.MAGHRIB -> names[4]
        Prayer.ISHAA -> names[5]
        Prayer.SUNSET -> ""
    }

}
