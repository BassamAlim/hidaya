package bassamalim.hidaya.features.prayers.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.ui.components.LoadingScreen
import bassamalim.hidaya.core.ui.components.tutorial.TutorialOverlay
import bassamalim.hidaya.core.ui.components.tutorial.TutorialShape
import bassamalim.hidaya.core.ui.components.tutorial.TutorialStep
import bassamalim.hidaya.core.ui.components.tutorial.rememberTutorialState
import bassamalim.hidaya.core.ui.components.tutorial.tutorialTarget
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import java.util.SortedMap

private val BoardMinHeight = 480.dp

@Composable
fun PrayersBoardScreen(viewModel: PrayersBoardViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.loading) return LoadingScreen()

    val tutorialState = rememberTutorialState()
    val locationTip = stringResource(R.string.prayers_tutorial_location)
    val settingsTip = stringResource(R.string.prayers_tutorial_settings)
    val reportTip = stringResource(R.string.prayers_tutorial_report)
    val notificationTip = stringResource(R.string.prayers_tutorial_notification)
    val reminderTip = stringResource(R.string.prayers_tutorial_reminder)
    LaunchedEffect(state.isTutorialActive) {
        if (state.isTutorialActive) {
            tutorialState.start(
                steps = listOf(
                    TutorialStep(text = locationTip, targetKey = "prayers_location"),
                    TutorialStep(
                        text = settingsTip,
                        targetKey = "prayers_settings",
                        shape = TutorialShape.Circle
                    ),
                    TutorialStep(
                        text = reportTip,
                        targetKey = "prayers_report",
                        shape = TutorialShape.Circle
                    ),
                    TutorialStep(
                        text = notificationTip,
                        targetKey = "prayers_notification",
                        shape = TutorialShape.Circle
                    ),
                    TutorialStep(text = reminderTip, targetKey = "prayers_reminder")
                ),
                onFinished = viewModel::onTutorialFinished
            )
        }
    }

    val dims = MaterialTheme.dimensions

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Fills the screen so the rows can share its height; below the minimum it scrolls
        // instead of squashing them (landscape, split screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .height(max(maxHeight, BoardMinHeight))
                .padding(
                    horizontal = dims.screenPaddingHorizontal,
                    vertical = dims.screenPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(dims.spaceMd)
        ) {
            Header(
                isLocationAvailable = state.locationAvailable,
                locationName = state.locationName,
                onLocatorClick = viewModel::onLocatorClick,
                onSettingsClick = viewModel::onTimeCalculationSettingsClick,
                onHelpClick = viewModel::onReportHelpClick,
                locationModifier = Modifier.tutorialTarget(tutorialState, "prayers_location"),
                settingsModifier = Modifier.tutorialTarget(tutorialState, "prayers_settings"),
                helpModifier = Modifier.tutorialTarget(tutorialState, "prayers_report")
            )

            DayNavigator(
                dateText = state.dateText,
                isToday = state.noDateOffset,
                onDateClick = viewModel::onDateClick,
                onPreviousDayClick = viewModel::onPreviousDayClick,
                onNextDayClick = viewModel::onNextDayClick
            )

            if (state.locationAvailable) {
                PrayersCard(
                    prayersData = state.prayersData,
                    modifier = Modifier.weight(1f),
                    onNotificationClick = viewModel::onPrayerCardClick,
                    onReminderClick = viewModel::onExtraReminderCardClick,
                    notificationTargetModifier =
                        Modifier.tutorialTarget(tutorialState, "prayers_notification"),
                    reminderTargetModifier =
                        Modifier.tutorialTarget(tutorialState, "prayers_reminder")
                )
            }
            else {
                NoLocationState(
                    modifier = Modifier.weight(1f),
                    onLocatorClick = viewModel::onLocatorClick
                )
            }
        }

        if (state.report.dialogShown) {
            PrayerTimesReportDialog(
                state = state,
                onDismiss = viewModel::onReportDismiss,
                onOpenCalculationSettings = viewModel::onTimeCalculationSettingsClick,
                onOpenLocator = viewModel::onLocatorClick,
                onNext = viewModel::onReportNext,
                onBack = viewModel::onReportBack,
                onTogglePrayer = viewModel::onReportTogglePrayer,
                onOpenCorrectTimePicker = viewModel::onCorrectTimePickerOpen,
                onCorrectTimePickerDismiss = viewModel::onCorrectTimePickerDismiss,
                onCorrectTimePickerConfirm = viewModel::onCorrectTimePickerConfirm,
                onNotesChange = viewModel::onReportNotesChange,
                onSubmit = viewModel::onReportSubmit
            )

            CorrectTimePickerHost(
                target = state.report.timePickerTarget,
                existing = state.report.correctTimes,
                onConfirm = viewModel::onCorrectTimePickerConfirm,
                onDismiss = viewModel::onCorrectTimePickerDismiss
            )
        }

        TutorialOverlay(state = tutorialState)
    }
}

@Composable
private fun Header(
    isLocationAvailable: Boolean,
    locationName: String,
    onLocatorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    locationModifier: Modifier = Modifier,
    settingsModifier: Modifier = Modifier,
    helpModifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Surface(
                onClick = onLocatorClick,
                modifier = locationModifier,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = dims.spaceMd,
                        end = dims.spaceSm,
                        top = dims.spaceSm,
                        bottom = dims.spaceSm
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.locate),
                        modifier = Modifier.size(dims.iconSm + dims.spaceXs)
                    )

                    Spacer(Modifier.width(dims.spaceXs))

                    Text(
                        text =
                            if (isLocationAvailable) locationName
                            else stringResource(R.string.click_to_locate),
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.appTypography.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(dims.iconMd)
                    )
                }
            }
        }

        IconButton(onClick = onSettingsClick, modifier = settingsModifier) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.prayer_time_settings)
            )
        }

        IconButton(onClick = onHelpClick, modifier = helpModifier) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(R.string.report_wrong_prayer_times)
            )
        }
    }
}

@Composable
private fun DayNavigator(
    dateText: String,
    isToday: Boolean,
    onDateClick: () -> Unit,
    onPreviousDayClick: () -> Unit,
    onNextDayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_day_button_description)
            )
        }

        // Tapping the date jumps back to today
        TextButton(onClick = onDateClick, modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (isToday) "${stringResource(R.string.today)} · $dateText"
                    else dateText,
                style = MaterialTheme.appTypography.title,
                color =
                    if (isToday) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        IconButton(onClick = onNextDayClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_day_button_description)
            )
        }
    }
}

@Composable
private fun PrayersCard(
    prayersData: SortedMap<Prayer, PrayerCardData>,
    modifier: Modifier = Modifier,
    onNotificationClick: (Prayer) -> Unit,
    onReminderClick: (Prayer) -> Unit,
    notificationTargetModifier: Modifier = Modifier,
    reminderTargetModifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions

    // A plain Card rather than MyCard: its content has to fill the height for the rows to share
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dims.elevationSm)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dims.spaceSm),
            verticalArrangement = Arrangement.spacedBy(dims.spaceXs)
        ) {
            prayersData.forEach { (prayer, data) ->
                // Spotlight the controls on the first prayer row only
                val isFirst = prayer == prayersData.firstKey()
                PrayerRow(
                    data = data,
                    modifier = Modifier.weight(1f),
                    onNotificationClick = { onNotificationClick(prayer) },
                    onReminderClick = { onReminderClick(prayer) },
                    notificationModifier =
                        if (isFirst) notificationTargetModifier else Modifier,
                    reminderModifier = if (isFirst) reminderTargetModifier else Modifier
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(
    data: PrayerCardData,
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit,
    onReminderClick: () -> Unit,
    notificationModifier: Modifier = Modifier,
    reminderModifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions
    val isNext = data.status == PrayerCardData.Status.NEXT
    val contentColor =
        if (isNext) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color =
                    if (isNext) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent,
                shape = RoundedCornerShape(dims.radiusMd)
            )
            .heightIn(min = dims.listItemHeight)
            .padding(start = dims.spaceMd, end = dims.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Only the text fades for passed prayers; the controls stay fully usable.
        // Full height since alpha's layer clips, and Tajawal's dots (e.g. ش) draw above the
        // text bounds
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .alpha(if (data.status == PrayerCardData.Status.PASSED) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.appTypography.h1.copy(
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )

            Text(
                text = data.time,
                // Takes the same share as the name and sits centered in it, between the
                // name and the controls
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.appTypography.h1.copy(
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                ),
                color = contentColor
            )
        }

        IconButton(onClick = onNotificationClick, modifier = notificationModifier) {
            Icon(
                imageVector = when (data.notificationType) {
                    NotificationType.ATHAN -> Icons.Default.Campaign
                    NotificationType.NOTIFICATION -> Icons.Default.Notifications
                    NotificationType.SILENT -> Icons.Default.NotificationsPaused
                    NotificationType.OFF -> Icons.Default.NotificationsOff
                },
                contentDescription = stringResource(R.string.notification_type),
                // Set explicitly (not IconButton's default) to match the reminder chip's icon
                modifier = Modifier.size(dims.iconMd),
                tint =
                    if (data.notificationType == NotificationType.OFF)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.width(dims.spaceXs))

        ReminderChip(
            isSpecified = data.isExtraReminderOffsetSpecified,
            offsetText = data.extraReminderOffset,
            onClick = onReminderClick,
            modifier = reminderModifier
        )
    }
}

@Composable
private fun ReminderChip(
    isSpecified: Boolean,
    offsetText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions

    Surface(
        onClick = onClick,
        // Same height as the notification IconButton beside it (and a full touch target)
        modifier = modifier.heightIn(min = dims.minTouchTarget),
        shape = CircleShape,
        color =
            if (isSpecified) MaterialTheme.colorScheme.secondaryContainer
            else Color.Transparent,
        contentColor =
            if (isSpecified) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = dims.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddAlert,
                contentDescription = stringResource(R.string.extra_notifications),
                modifier = Modifier.size(dims.iconMd)
            )

            // The offset gets its own space instead of being squeezed beside the icon
            if (isSpecified) {
                Spacer(Modifier.width(dims.spaceXs))

                Text(text = offsetText, style = MaterialTheme.appTypography.label)
            }
        }
    }
}

@Composable
private fun NoLocationState(modifier: Modifier = Modifier, onLocatorClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(dims.iconXl * 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(dims.spaceLg))

        Text(
            text = stringResource(R.string.prayers_no_location),
            style = MaterialTheme.appTypography.body,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(dims.spaceXl))

        Button(onClick = onLocatorClick) {
            Text(
                text = stringResource(R.string.set_location),
                style = MaterialTheme.appTypography.button
            )
        }
    }
}
