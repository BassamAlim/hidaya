package bassamalim.hidaya.features.home

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.AnalogClock
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.theme.Positive
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

// The clock is sized from the card width only, so changing text never resizes it
private val HeroTextMinWidth = 140.dp
private val HeroClockMinSize = 120.dp
private val HeroClockMaxSize = 200.dp

@Composable
fun HomeScreen(viewModel: HomeViewModel, bottomNavController: NavHostController) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current!!
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.onPermissionResult(activity) }
    )

    DisposableEffect(key1 = viewModel) {
        viewModel.onStart(activity = activity)
        onDispose { viewModel.onStop() }
    }

    if (state.isLoading) return

    val dims = MaterialTheme.dimensions
    val onPrayersClick = { viewModel.onPrayerCardClick(bottomNavController) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dims.screenPaddingHorizontal,
                vertical = dims.screenPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(dims.spaceLg)
    ) {
        if (state.pendingPermissions.isNotEmpty()) {
            PendingPermissionsCard(
                pendingPermissions = state.pendingPermissions,
                permissionLauncher = permissionLauncher
            )
        }

        NextPrayerCard(state = state, onClick = onPrayersClick)

        TodayPrayersCard(prayers = state.todayPrayers, onClick = onPrayersClick)

        TodayWerdCard(
            werdPage = state.werdPage,
            isWerdDone = state.isWerdDone,
            onClick = viewModel::onTodayWerdCardClick
        )

        RemembranceCard(isMorning = state.isMorning, onClick = viewModel::onRemembranceClick)

        ProgressSection(
            quranPagesRecord = state.quranRecord,
            recitationsRecord = state.recitationsRecord,
            isLeaderboardEnabled = state.isLeaderboardEnabled,
            onLeaderboardClick = viewModel::onLeaderboardClick
        )
    }
}

@Composable
private fun PendingPermissionsCard(
    pendingPermissions: List<PendingPermission>,
    permissionLauncher: ActivityResultLauncher<String>
) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

    MyCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceSm)
    ) {
        // Fine and coarse location share a message, so each message is shown once
        pendingPermissions.distinctBy { it.messageResId }.forEach { permission ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(dims.iconMd)
                )

                Text(
                    text = stringResource(permission.messageResId),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dims.spaceMd),
                    style = MaterialTheme.appTypography.label,
                    color = contentColor
                )

                TextButton(onClick = { permissionLauncher.launch(permission.permission) }) {
                    Text(
                        text = stringResource(R.string.give_permission),
                        style = MaterialTheme.appTypography.button
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerCard(state: HomeUiState, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    MyCard(
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceXl)
    ) {
        BoxWithConstraints {
            val clockSize = (maxWidth - HeroTextMinWidth)
                .coerceIn(HeroClockMinSize, HeroClockMaxSize)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.next_prayer),
                        style = MaterialTheme.appTypography.label,
                        color = contentColor.copy(alpha = 0.75f)
                    )

                    // Arabic dots rise above the text box at this size, into the label above
                    Text(
                        text = state.nextPrayerName,
                        modifier = Modifier.padding(top = dims.spaceSm),
                        style = MaterialTheme.appTypography.display.copy(fontSize = 32.sp),
                        color = contentColor
                    )

                    Text(
                        text = state.nextPrayerTimeText,
                        style = MaterialTheme.appTypography.title,
                        color = contentColor
                    )

                    Spacer(Modifier.height(dims.spaceLg))

                    Text(
                        text = String.format(stringResource(R.string.in_time), state.remaining),
                        style = MaterialTheme.appTypography.h1.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnalogClock(
                    previousPrayerTime = state.previousPrayerTime,
                    nextPrayerTime = state.nextPrayerTime,
                    numeralsLanguage = state.numeralsLanguage,
                    modifier = Modifier.size(clockSize)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = dims.spaceMd),
            color = contentColor.copy(alpha = 0.15f)
        )

        Text(
            text = "${state.previousPrayerName} · " +
                    String.format(stringResource(R.string.passed), state.passed),
            style = MaterialTheme.appTypography.label,
            color = contentColor.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun TodayPrayersCard(prayers: List<TodayPrayer>, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(onClick = onClick, shape = RoundedCornerShape(dims.radiusLg)) {
        Text(
            text = stringResource(R.string.today_prayers),
            style = MaterialTheme.appTypography.title,
            modifier = Modifier.padding(bottom = dims.spaceMd)
        )

        Column(verticalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
            prayers.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
                    row.forEach { prayer ->
                        PrayerCell(prayer = prayer, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerCell(prayer: TodayPrayer, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions
    val isNext = prayer.status == TodayPrayer.Status.NEXT

    Surface(
        modifier = modifier.alpha(if (prayer.status == TodayPrayer.Status.PASSED) 0.5f else 1f),
        shape = RoundedCornerShape(dims.radiusMd),
        color =
            if (isNext) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor =
            if (isNext) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(vertical = dims.spaceSm, horizontal = dims.spaceXs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prayer.name,
                style = MaterialTheme.appTypography.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                // Some prayers don't occur at high latitudes
                text = prayer.timeText.ifEmpty { "—" },
                style = MaterialTheme.appTypography.subtitle.copy(
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TodayWerdCard(werdPage: String, isWerdDone: Boolean, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(onClick = onClick, shape = RoundedCornerShape(dims.radiusLg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.AutoMirrored.Default.MenuBook)

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceMd)
            ) {
                Text(
                    text = stringResource(R.string.today_werd),
                    style = MaterialTheme.appTypography.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${stringResource(R.string.page)} $werdPage",
                    style = MaterialTheme.appTypography.headline
                )
            }

            if (isWerdDone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Positive,
                        modifier = Modifier.size(dims.iconMd)
                    )

                    Spacer(Modifier.width(dims.spaceXs))

                    Text(
                        text = stringResource(R.string.already_read_description),
                        style = MaterialTheme.appTypography.label,
                        color = Positive
                    )
                }
            }
            else {
                FilledTonalButton(onClick = onClick) {
                    Text(
                        text = stringResource(R.string.read),
                        style = MaterialTheme.appTypography.button
                    )
                }
            }
        }
    }
}

@Composable
private fun RemembranceCard(isMorning: Boolean, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(onClick = onClick, shape = RoundedCornerShape(dims.radiusLg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (isMorning) Icons.Default.WbSunny else Icons.Default.Bedtime)

            Text(
                text = stringResource(
                    if (isMorning) R.string.morning_remembrances
                    else R.string.evening_remembrances
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceMd),
                style = MaterialTheme.appTypography.headline
            )

            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressSection(
    quranPagesRecord: String,
    recitationsRecord: String,
    isLeaderboardEnabled: Boolean,
    onLeaderboardClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Column {
        MySectionHeader(
            title = stringResource(R.string.your_progress),
            trailing = if (isLeaderboardEnabled) {
                {
                    TextButton(onClick = onLeaderboardClick) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            modifier = Modifier.size(dims.iconSm)
                        )

                        Spacer(Modifier.width(dims.spaceXs))

                        Text(
                            text = stringResource(R.string.leaderboard),
                            style = MaterialTheme.appTypography.label
                        )
                    }
                }
            } else null
        )

        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dims.spaceMd)
        ) {
            StatTile(
                value = quranPagesRecord,
                label = stringResource(R.string.quran_pages_record_title),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            StatTile(
                value = recitationsRecord,
                label = stringResource(R.string.recitations_time_record_title),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    MyCard(modifier = modifier, shape = RoundedCornerShape(MaterialTheme.dimensions.radiusLg)) {
        Text(
            text = value,
            style = MaterialTheme.appTypography.h1.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )

        Text(
            text = label,
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    val dims = MaterialTheme.dimensions

    Box(
        modifier = Modifier
            .size(dims.iconXl + dims.spaceSm)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(dims.radiusMd)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(dims.iconMd)
        )
    }
}
