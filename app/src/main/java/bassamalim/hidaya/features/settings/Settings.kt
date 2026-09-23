package bassamalim.hidaya.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.components.MyTextButton
import bassamalim.hidaya.core.ui.components.MyValuedSlider
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun <V> MenuSetting(
    selection: V,
    items: Array<V>,
    entries: Array<String>,
    title: String,
    icon: Any? = null,
    onSelection: (V) -> Unit = {}
) {
    var isShown by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { isShown = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                if (icon is ImageVector) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier
                            .padding(end = MaterialTheme.dimensions.spaceLg)
                            .size(MaterialTheme.dimensions.iconMd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                else if (icon is Painter) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        modifier = Modifier
                            .padding(end = MaterialTheme.dimensions.spaceLg)
                            .size(MaterialTheme.dimensions.iconMd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                else if (icon is Int) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = title,
                        modifier = Modifier
                            .padding(end = MaterialTheme.dimensions.spaceLg)
                            .size(MaterialTheme.dimensions.iconMd),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column {
                PreferenceTitle(title)

                // The selection can be outside [items] before the real value loads (e.g. an id
                // of 0 while the options start at 1), so a missing match shows no summary
                SummaryText(entries.getOrElse(items.indexOf(selection)) { "" })
            }
        }

        if (isShown) {
            AlertDialog(
                onDismissRequest = { isShown = false },
                confirmButton = {},
                dismissButton = {
                    MyTextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { isShown = false }
                    )
                },
                icon = {
                    if (icon is ImageVector) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    else if (icon is Painter) {
                        Icon(
                            painter = icon,
                            contentDescription = title,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    else if (icon is Int) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = title,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                title = {
                    MyText(
                        text = title,
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        Modifier
                            .heightIn(1.dp, 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        entries.forEachIndexed { index, text ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .fillMaxWidth()
                                    .padding(6.dp)
                                    .clickable {
                                        onSelection(items[index])
                                        isShown = false
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = index == items.indexOf(selection),
                                    onClick = {
                                        onSelection(items[index])
                                        isShown = false
                                    }
                                )

                                MyText(text = text, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SwitchSetting(
    value: Boolean,
    title: String,
    summary: String? = null,
    padding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 16.dp),
    enabled: Boolean = true,
    onSwitch: (Boolean) -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSwitch(!value) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                Modifier.weight(1f)
            ) {
                PreferenceTitle(title, Modifier.padding(end = 40.dp))

                if (summary != null) {
                    SummaryText(summary)
                }
            }

            Switch(
                checked = value,
                onCheckedChange = { onSwitch(!value) },
                modifier = Modifier
                    .fillMaxHeight(0.2f)
                    .height(10.dp),
                enabled = enabled
            )
        }
    }
}

@Composable
fun SliderPref(
    value: Float,
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (String) -> String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit = {},
    onValueChangeFinished: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
    ) {
        PreferenceTitle(title)

        MyValuedSlider(
            value = value,
            valueRange = valueRange,
            valueFormatter = valueFormatter,
            enabled = enabled,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}

@Composable
fun CategoryTitle(title: String) {
    MyText(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp, bottom = 8.dp, start = 15.dp, end = 15.dp),
        fontSize = 16.sp,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun PreferenceTitle(title: String, modifier: Modifier = Modifier) {
    MyText(
        text = title,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SummaryText(text: String) {
    MyText(
        text = text,
        fontSize = 14.sp,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** A titled group of settings rows on a card, shared by Settings and onboarding. */
@Composable
fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    val dims = MaterialTheme.dimensions

    MySectionHeader(title = title, modifier = Modifier.padding(top = dims.spaceMd))

    if (description != null) {
        Text(
            text = description,
            modifier = Modifier.padding(
                start = dims.spaceLg,
                end = dims.spaceLg,
                bottom = dims.spaceSm
            ),
            style = MaterialTheme.appTypography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    MyCard(
        shape = RoundedCornerShape(dims.radiusLg),
        contentPadding = PaddingValues(vertical = dims.spaceXs)
    ) {
        content()
    }
}
