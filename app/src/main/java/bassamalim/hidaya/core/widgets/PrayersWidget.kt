package bassamalim.hidaya.core.widgets

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.material3.ColorProviders
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
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

private data class PrayerWidgetItem(
    val name: String,
    val timeString: String,
    val isNext: Boolean
)

class PrayersWidget(
    private val appSettingsRepository: AppSettingsRepository,
    private val prayersRepository: PrayersRepository,
    private val locationRepository: LocationRepository,
    private val dispatcher: CoroutineDispatcher
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val context = context.withAppLocale()  // app language, not the device's (pre-API 33)

        val items = withContext(dispatcher) {
            getPrayerItems(context)
        }

        provideContent {
            GlanceTheme(colors = ColorProviders(light = lightColorScheme, dark = darkColorScheme)) {
                WidgetContent(items, context)
            }
        }
    }

    @Composable
    private fun WidgetContent(items: List<PrayerWidgetItem>?, context: Context) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable {
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
        ) {
            if (items == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.error_fetching_data),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (item in items) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.name,
                                style = TextStyle(
                                    color = if (item.isNext) GlanceTheme.colors.primary
                                            else GlanceTheme.colors.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Text(
                                text = item.timeString,
                                style = TextStyle(
                                    color = if (item.isNext) GlanceTheme.colors.primary
                                            else GlanceTheme.colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (item.isNext) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun getPrayerItems(context: Context): List<PrayerWidgetItem>? {
        val location = locationRepository.getLocation().first<Location?>() ?: return null
        val prayerTimes = PrayerTimeUtils.getPrayerTimes(
            settings = prayersRepository.getPrayerTimesCalculatorSettings().first(),
            selectedTimeZoneId = locationRepository.getTimeZone(location.ids.cityId),
            location = location,
            calendar = Calendar.getInstance()
        )
        val prayerTimeStrings = PrayerTimeUtils.formatPrayerTimes(
            prayerTimes = prayerTimes,
            language = LangUtils.getAppLanguage(),
            numeralsLanguage = appSettingsRepository.getNumeralsLanguage().first(),
            timeFormat = appSettingsRepository.getTimeFormat().first()
        )

        val prayerNames = context.resources.getStringArray(R.array.prayer_names)
        val now = Calendar.getInstance()

        val nextPrayer = prayerTimes.entries.firstOrNull { (prayer, time) ->
            prayer != Prayer.SUNRISE && prayer != Prayer.SUNSET && time != null && time.after(now)
        }?.key

        // Glance has no layout direction: the launcher lays the Row out in the *device* direction.
        // Fajr should lead in the *app* direction, so reverse only when the two differ.
        val isAppRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val isHostRtl =
            Resources.getSystem().configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        return prayerTimes.keys
            .filter { it != Prayer.SUNRISE && it != Prayer.SUNSET }
            .let { if (isAppRtl != isHostRtl) it.reversed() else it }
            .map { prayer ->
                PrayerWidgetItem(
                    name = getPrayerName(prayer, prayerNames),
                    timeString = prayerTimeStrings[prayer] ?: "",
                    isNext = prayer == nextPrayer
                )
            }
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
