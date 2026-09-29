package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.core.ui.theme.tajwal

/**
 * Pass [minFontSize] for text in a narrow slot: it then shows at [fontSize] (which follows
 * the user's font size setting) and only shrinks, down to [minFontSize], if it would not
 * otherwise fit in [maxLines].
 */
@Composable
fun MyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily = tajwal,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minFontSize: TextUnit = TextUnit.Unspecified
) {
    if (minFontSize.isSpecified) {
        BasicText(
            text = text,
            modifier = modifier,
            style = TextStyle(
                fontFamily = fontFamily,
                color = color.takeOrElse { LocalContentColor.current },
                fontWeight = fontWeight,
                textAlign = textAlign,
                // Relative, so it follows whatever size the text is shrunk to
                lineHeight = 1.4.em,
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            ),
            overflow = TextOverflow.Ellipsis,
            softWrap = softWrap,
            maxLines = maxLines,
            autoSize = TextAutoSize.StepBased(
                // StepBased rejects a minimum above the maximum
                minFontSize = if (minFontSize > fontSize) fontSize else minFontSize,
                maxFontSize = fontSize,
                stepSize = 1.sp
            )
        )
        return
    }

    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        style = TextStyle(
            fontFamily = fontFamily,
            color = color,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = fontSize * 1.4,
            platformStyle = PlatformTextStyle(includeFontPadding = true)
        ),
        softWrap = softWrap,
        maxLines = maxLines
    )
}

@Composable
fun MyText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
    fontFamily: FontFamily = tajwal
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        style = TextStyle(
            fontFamily = fontFamily,
            color = textColor,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = fontSize * 1.4,
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )
    )
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MyText(
            message,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}