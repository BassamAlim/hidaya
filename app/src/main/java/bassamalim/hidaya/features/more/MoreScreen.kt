package bassamalim.hidaya.features.more

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

private val FeatureCardHeight = 140.dp
private val FeatureIconContainerSize = 48.dp

@Composable
fun MoreScreen(viewModel: MoreViewModel) {
    val context = LocalContext.current
    val dims = MaterialTheme.dimensions

    val featuresItems = listOfNotNull(
        FeatureItem(
            title = stringResource(R.string.hadeeth_books),
            drawableId = R.drawable.ic_books,
            onClick = viewModel::onBooksClick
        ),
        FeatureItem(
            title = stringResource(R.string.tv_channels),
            icon = Icons.Default.LiveTv,
            onClick = viewModel::onTvClick
        ),
        // The radio needs Android 8
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FeatureItem(
                title = stringResource(R.string.quran_radio),
                icon = Icons.Default.Radio,
                onClick = viewModel::onRadioClick
            )
        } else null,
        FeatureItem(
            title = stringResource(R.string.date_converter),
            icon = Icons.Default.CalendarMonth,
            onClick = viewModel::onDateConverterClick
        )
    )

    val gamesItems = listOf(
        FeatureItem(
            title = stringResource(R.string.quiz_title),
            icon = Icons.AutoMirrored.Default.FactCheck,
            onClick = viewModel::onQuizClick
        ),
        FeatureItem(
            title = stringResource(R.string.verse_guess_title),
            icon = Icons.Default.TravelExplore,
            onClick = viewModel::onVerseGuessClick
        )
    )

    val settingsItems = listOf(
        FeatureItem(
            title = stringResource(R.string.settings),
            icon = Icons.Default.Settings,
            onClick = viewModel::onSettingsClick
        ),
        FeatureItem(
            title = stringResource(R.string.contact),
            icon = Icons.Default.Mail,
            onClick = { viewModel.onContactClick(context) }
        ),
        FeatureItem(
            title = stringResource(R.string.share_app),
            icon = Icons.Default.Share,
            onClick = { viewModel.onShareClick(context) }
        ),
        FeatureItem(
            title = stringResource(R.string.about),
            icon = Icons.Default.Info,
            onClick = viewModel::onAboutClick
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(dims.spaceLg),
        verticalArrangement = Arrangement.spacedBy(dims.spaceXl)
    ) {
        FeatureSection(title = stringResource(R.string.features), items = featuresItems)

        FeatureSection(title = stringResource(R.string.games), items = gamesItems)

        FeatureSection(title = stringResource(R.string.app_and_support), items = settingsItems)
    }
}

@Composable
private fun FeatureSection(title: String, items: List<FeatureItem>) {
    val dims = MaterialTheme.dimensions
    Column(verticalArrangement = Arrangement.spacedBy(dims.spaceLg)) {
        MySectionHeader(title = title)

        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dims.spaceMd)
            ) {
                rowItems.forEach { item ->
                    FeatureCard(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions
    MyCard(
        modifier = modifier.height(FeatureCardHeight),
        onClick = item.onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dims.elevationSm,
            pressedElevation = dims.elevationLg,
            hoveredElevation = dims.elevationMd
        ),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(FeatureIconContainerSize)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(dims.radiusLg)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.icon != null -> {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(dims.iconMd),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    item.drawableId != null -> {
                        Icon(
                            painter = painterResource(item.drawableId),
                            contentDescription = item.title,
                            modifier = Modifier.size(dims.iconMd),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dims.spaceLg))

            Text(
                text = item.title,
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
