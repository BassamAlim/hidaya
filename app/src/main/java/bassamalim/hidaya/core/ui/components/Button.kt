package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.ui.theme.tajwal

@Composable
fun MyButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = { if (enabled) onClick() },
        modifier = modifier.padding(3.dp),
        shape = CircleShape,
        colors = colors
    ) {
        MyText(text = text, fontSize = fontSize, fontWeight = fontWeight)
    }
}

@Composable
fun MyFilledTonalButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.padding(3.dp),
        shape = CircleShape,
        colors = colors,
        contentPadding = contentPadding
    ) {
        MyText(text = text, fontSize = fontSize, fontWeight = fontWeight)
    }
}

@Composable
fun MyOutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.padding(3.dp),
        shape = CircleShape,
        colors = colors
    ) {
        MyText(text = text, fontSize = fontSize, fontWeight = fontWeight)
    }
}

@Composable
fun MySquareButton(
    text: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(3.dp),
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    innerPadding: PaddingValues = PaddingValues(6.dp),
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.padding(padding),
        shape = RoundedCornerShape(MaterialTheme.dimensions.radiusLg),
        enabled = enabled
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()

            // Square buttons often sit several to a row, so large font settings shrink the
            // label to fit instead of breaking the row
            MyText(
                text = text,
                modifier = Modifier.padding(innerPadding),
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = textColor,
                maxLines = 2,
                minFontSize = 12.sp
            )
        }
    }
}

@Composable
fun MySquareButton(
    text: String,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    iconSize: Dp = Dp.Unspecified,
    iconTint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    MySquareButton(
        text = text,
        fontSize = 20.sp,
        modifier = modifier
            .size(180.dp)
            .padding(vertical = 7.dp, horizontal = 7.dp),
        icon = {
            Icon(
                imageVector = imageVector,
                contentDescription = text,
                modifier =
                    if (iconSize == Dp.Unspecified) Modifier
                    else Modifier.size(iconSize),
                tint = iconTint
            )
        },
        onClick = onClick
    )
}

@Composable
fun MyHorizontalButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    enabled: Boolean = true,
    middlePadding: PaddingValues = PaddingValues(6.dp),
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = { if (enabled) onClick() },
        modifier = modifier.padding(3.dp),
        shape = CircleShape,
        colors = colors
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (icon != null) {
                icon()

                Spacer(modifier = Modifier.padding(middlePadding))
            }

            MyText(text = text, fontSize = fontSize, fontWeight = fontWeight)
        }
    }
}

@Composable
fun MySquareButton(
    text: String,
    drawableId: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 70.dp,
    onClick: () -> Unit
) {
    MySquareButton(
        text = text,
        fontSize = 20.sp,
        modifier = modifier
            .size(180.dp)
            .padding(vertical = 7.dp, horizontal = 7.dp),
        icon = {
            Icon(
                painter = painterResource(drawableId),
                contentDescription = text,
                modifier =
                    if (iconSize == Dp.Unspecified) Modifier
                    else Modifier.size(iconSize),
                tint = tint
            )
        },
        onClick = onClick
    )
}

@Composable
fun MyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    textColor: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
    fontFamily: FontFamily = tajwal,
    textModifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = text,
            modifier = textModifier,
            fontSize = fontSize,
            style = TextStyle(
                fontFamily = fontFamily,
                color = textColor,
                fontWeight = fontWeight,
                textAlign = textAlign,
                lineHeight = fontSize * 1.4
            )
        )
    }
}