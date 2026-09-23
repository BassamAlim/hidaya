package bassamalim.hidaya.features.prayers.notificationSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.NotificationType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.ui.components.DialogDismissButton
import bassamalim.hidaya.core.ui.components.DialogTitle
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun PrayerNotificationSettingsDialog(viewModel: PrayerNotificationSettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = viewModel::onDismiss,
        // Choosing an option saves and closes, so the only button left is Cancel
        confirmButton = { DialogDismissButton { viewModel.onDismiss() } },
        title = {
            DialogTitle(String.format(stringResource(R.string.notification_type), state.prayerName))
        },
        text = { DialogContent(viewModel, state) }
    )
}

@Composable
private fun DialogContent(viewModel: PrayerNotificationSettingsViewModel, state: PrayerNotificationSettingsUiState) {
    NotificationTypesRadioGroup(
        prayer = state.prayer,
        selection = state.notificationType,
        onSelect = viewModel::onNotificationTypeChange
    )
}

@Composable
private fun NotificationTypesRadioGroup(
    prayer: Prayer,
    selection: NotificationType,
    onSelect: (NotificationType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Athan
        if (prayer != Prayer.SUNRISE) {
            NotificationTypeOption(
                name = stringResource(R.string.athan_speaker),
                icon = Icons.Default.Campaign,
                isSelected = selection == NotificationType.ATHAN,
                onSelection = { onSelect(NotificationType.ATHAN) }
            )
        }

        // Notification
        NotificationTypeOption(
            name = stringResource(R.string.enable_notification),
            icon = Icons.Default.Notifications,
            isSelected = selection == NotificationType.NOTIFICATION,
            onSelection = { onSelect(NotificationType.NOTIFICATION) }
        )

        // Silent Notification
        NotificationTypeOption(
            name = stringResource(R.string.silent_notification),
            icon = Icons.Default.NotificationsPaused,
            isSelected = selection == NotificationType.SILENT,
            onSelection = { onSelect(NotificationType.SILENT) }
        )

        // Off
        NotificationTypeOption(
            name = stringResource(R.string.disable_notification),
            icon = Icons.Default.NotificationsOff,
            isSelected = selection == NotificationType.OFF,
            onSelection = { onSelect(NotificationType.OFF) }
        )
    }
}

@Composable
private fun NotificationTypeOption(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .selectable(
                selected = isSelected,
                onClick = onSelection,
                role = Role.RadioButton
            )
            .padding(MaterialTheme.dimensions.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMd)
    ) {
        RadioButton(selected = isSelected, onClick = null)

        Icon(
            imageVector = icon,
            contentDescription = name,
            tint =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = name,
            style = MaterialTheme.appTypography.title,
            color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
        )
    }
}
