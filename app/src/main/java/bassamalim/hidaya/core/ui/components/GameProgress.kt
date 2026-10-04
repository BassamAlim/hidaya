package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

/** A game stat: the value over its label. */
@Composable
fun MyStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val dims = MaterialTheme.dimensions
    val contentColor =
        if (isHighlighted) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface

    MyCard(
        modifier = modifier,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceMd)
    ) {
        Text(
            text = value,
            style = MaterialTheme.appTypography.headline,
            color = contentColor,
            maxLines = 1
        )

        Text(
            text = label,
            style = MaterialTheme.appTypography.caption,
            color = if (isHighlighted) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Opens the leaderboard; meant as a [MySectionHeader]'s trailing action. */
@Composable
fun LeaderboardButton(onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions
    val labelStyle = MaterialTheme.appTypography.label

    TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Leaderboard,
            contentDescription = null,
            modifier = alignIconWithText(labelStyle.fontSize).size(dims.iconSm)
        )

        Spacer(Modifier.width(dims.spaceXs))

        Text(
            text = stringResource(R.string.leaderboard),
            modifier = Modifier.alignByBaseline(),
            style = labelStyle
        )
    }
}
