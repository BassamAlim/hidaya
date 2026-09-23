package bassamalim.hidaya.core.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.tajwal
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.TimeOfDay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClock(
    previousPrayerTime: TimeOfDay?,
    nextPrayerTime: TimeOfDay?,
    numeralsLanguage: Language,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(TimeOfDay.fromCalendar(Calendar.getInstance())) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = TimeOfDay.fromCalendar(Calendar.getInstance())
        }
    }

    Draw(
        currentTime = currentTime,
        previousPrayerTime = previousPrayerTime,
        nextPrayerTime = nextPrayerTime,
        modifier = modifier,
        numeralsLanguage = numeralsLanguage
    )
}

@Composable
private fun Draw(
    currentTime: TimeOfDay,
    previousPrayerTime: TimeOfDay?,
    nextPrayerTime: TimeOfDay?,
    modifier: Modifier,
    numeralsLanguage: Language
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    val teethColor = MaterialTheme.colorScheme.onSurface
    val numbersColor = MaterialTheme.colorScheme.onSurface
    val hoursHandColor = MaterialTheme.colorScheme.onSurface
    val minutesHandColor = MaterialTheme.colorScheme.onSurface
    val secondsHandColor = MaterialTheme.colorScheme.primary
    val pastArcColor = MaterialTheme.colorScheme.primaryContainer
    val remainingArcColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 6.dp,
        tonalElevation = 6.dp
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Canvas(Modifier.size(maxWidth)) {
                val center = size.maxDimension / 2f
                val fullRadius = size.maxDimension / 2f

                drawTeeth(
                    center = center,
                    fullRadius = fullRadius,
                    color = teethColor
                )

                drawNumbers(
                    context = context,
                    numeralsLanguage = numeralsLanguage,
                    center = center,
                    fullRadius = fullRadius,
                    textMeasurer = textMeasurer,
                    color = numbersColor
                )

                drawHands(
                    currentTime = currentTime,
                    center = center,
                    fullRadius = fullRadius,
                    hoursHandColor = hoursHandColor,
                    minutesHandColor = minutesHandColor,
                    secondsHandColor = secondsHandColor
                )

                drawArcs(
                    currentTime = currentTime,
                    previousPrayerTime = previousPrayerTime,
                    nextPrayerTime = nextPrayerTime,
                    center = center,
                    fullRadius = fullRadius,
                    pastArcColor = pastArcColor,
                    remainingArcColor = remainingArcColor
                )
            }
        }
    }
}

private fun DrawScope.drawTeeth(center: Float, fullRadius: Float, color: Color) {
    val radius = fullRadius * 0.93f

    for (i in 0..59) {
        val theta = i * 0.105f  // 0.105 is the distance between each two teeth spaces
        drawLine(
            start = Offset(
                x = center + radius * cos(theta),
                y = center + radius * sin(theta)
            ),
            end = Offset(
                x = center + (radius + 10) * cos(theta),
                y = center + (radius + 10) * sin(theta)
            ),
            strokeWidth = 5f,
            color = color
        )
    }
}

private fun DrawScope.drawNumbers(
    context: Context,
    numeralsLanguage: Language,
    center: Float,
    fullRadius: Float,
    textMeasurer: TextMeasurer,
    color: Color
) {
    val numerals = when (numeralsLanguage) {
        Language.ARABIC -> context.resources.getStringArray(R.array.numerals)
        Language.ENGLISH -> context.resources.getStringArray(R.array.numerals_en)
    }
    val radius = fullRadius * 0.82f
    // A fixed share of the radius in real pixels, so the numbers keep the same proportion to
    // the face on every screen density and system font size
    val textStyle = TextStyle(
        fontFamily = tajwal,
        fontSize = (radius * 0.2f).toSp(),
        color = color
    )

    for (number in numerals) {
        val angle = Math.PI / 6 * (number.toInt() - 3)
        val size = textMeasurer.measure(number, style = textStyle).size
        val numberWidth = size.width
        val numberHeight = size.height

        drawText(
            textMeasurer = textMeasurer,
            text = number,
            topLeft = Offset(
                x = (center + cos(angle) * radius - numberWidth / 2).toFloat(),
                y = (center + sin(angle) * radius - numberHeight / 2).toFloat()
            ),
            style = textStyle
        )
    }
}

private fun DrawScope.drawHands(
    currentTime: TimeOfDay,
    center: Float,
    fullRadius: Float,
    hoursHandColor: Color,
    minutesHandColor: Color,
    secondsHandColor: Color
) {
    drawHoursHand(
        hour = currentTime.hour,
        minute = currentTime.minute,
        center = center,
        fullRadius = fullRadius,
        color = hoursHandColor
    )

    drawMinutesHand(
        minute = currentTime.minute,
        center = center,
        fullRadius = fullRadius,
        color = minutesHandColor
    )

    drawSecondsHand(
        second = currentTime.second,
        center = center,
        fullRadius = fullRadius,
        color = secondsHandColor
    )
}

private fun DrawScope.drawHoursHand(
    hour: Int,
    minute: Int,
    center: Float,
    fullRadius: Float,
    color: Color
) {
    val angle = (Math.PI * (hour + minute / 60f) * 5f / 30f - Math.PI / 2f).toFloat()
    val radius = fullRadius * 0.5f

    drawLine(
        start = Offset(x = center, y = center),
        // extend the hand a bit beyond the center
//        start = Offset(x = (center - cos(angle) * 20), y = (center - sin(angle) * 20)),
        end = Offset(x = (center + cos(angle) * radius), y = (center + sin(angle) * radius)),
        strokeWidth = 11f,
        cap = StrokeCap.Round,
        color = color
    )
}

private fun DrawScope.drawMinutesHand(
    minute: Int,
    center: Float,
    fullRadius: Float,
    color: Color
) {
    val angle = (Math.PI * minute / 30f - Math.PI / 2f).toFloat()
    val radius = fullRadius * 0.68f

    drawLine(
        start = Offset(x = center, y = center),
        // extend the hand a bit beyond the center
//        start = Offset(x = (center - cos(angle) * 30), y = (center - sin(angle) * 30)),
        end = Offset(x = (center + cos(angle) * radius), y = (center + sin(angle) * radius)),
        strokeWidth = 9f,
        cap = StrokeCap.Round,
        color = color
    )
}

private fun DrawScope.drawSecondsHand(
    second: Int,
    center: Float,
    fullRadius: Float,
    color: Color
) {
    val angle = (Math.PI * second / 30f - Math.PI / 2f).toFloat()
    val radius = fullRadius * 0.68f

    drawLine(
//        start = Offset(x = center, y = center),
        // extend the hand a bit beyond the center
        start = Offset(x = (center - cos(angle) * 40), y = (center - sin(angle) * 40)),
        end = Offset(x = (center + cos(angle) * radius), y = (center + sin(angle) * radius)),
        strokeWidth = 6f,
        cap = StrokeCap.Round,
        color = color
    )
}

private fun DrawScope.drawArcs(
    currentTime: TimeOfDay,
    previousPrayerTime: TimeOfDay?,
    nextPrayerTime: TimeOfDay?,
    center: Float,
    fullRadius: Float,
    pastArcColor: Color,
    remainingArcColor: Color
) {
    val radius = fullRadius * 0.98f

    if (previousPrayerTime != null) {
        drawPassedArc(
            currentTime = currentTime,
            previousPrayerTime = previousPrayerTime,
            center = center,
            radius = radius,
            color = pastArcColor
        )
    }

    if (nextPrayerTime != null) {
        drawRemainingArc(
            currentTime = currentTime,
            nextPrayerTime = nextPrayerTime,
            center = center,
            radius = radius,
            color = remainingArcColor
        )
    }
}

private fun DrawScope.drawPassedArc(
    currentTime: TimeOfDay,
    previousPrayerTime: TimeOfDay,
    center: Float,
    radius: Float,
    color: Color
) {
    val previousPrayerTimeAngle = timeToDegrees(previousPrayerTime)
    val currentTimeAngle = timeToDegrees(currentTime)
    val sweepAngle = (currentTimeAngle - previousPrayerTimeAngle + 360) % 360

    drawArc(
        startAngle = previousPrayerTimeAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(x = center - radius, y = center - radius),
        size = Size(width = radius * 2, height = radius * 2),
        style = Stroke(width = 8f, cap = StrokeCap.Round),
        color = color
    )
}

private fun DrawScope.drawRemainingArc(
    currentTime: TimeOfDay,
    nextPrayerTime: TimeOfDay,
    center: Float,
    radius: Float,
    color: Color
) {
    val currentTimeAngle = timeToDegrees(currentTime)
    val nextPrayerTimeAngle = timeToDegrees(nextPrayerTime)
    val sweepAngle = (nextPrayerTimeAngle - currentTimeAngle + 360) % 360

    drawArc(
        startAngle = currentTimeAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(x = center - radius, y = center - radius),
        size = Size(width = radius * 2, height = radius * 2),
        style = Stroke(width = 8f, cap = StrokeCap.Round),
        color = color
    )
}

fun timeToDegrees(time: TimeOfDay): Float {
    return -90f + (time.hour % 12) * 30 + time.minute * 0.5f
}