package bassamalim.hidaya.features.locator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyTopBar
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

// Taller than the default button: these are the screen's main decision
private val ActionButtonHeight = 56.dp

@Composable
fun LocatorScreen(viewModel: LocatorViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = stringResource(R.string.choose_allow_all_the_time)
    val requestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onLocationRequestResult(permissions)
    }

    LaunchedEffect(null) {
        viewModel.provide(requestLauncher, snackbarHostState, snackbarMessage)
    }

    val dims = MaterialTheme.dimensions

    Scaffold(
        // Reached from Prayers or Qibla it needs a way back; on first launch there's none
        topBar = {
            if (!state.isInitial) MyTopBar(title = stringResource(R.string.location_screen_title))
        },
        // The actions stay pinned at the bottom; only the explanation scrolls if space is short
        bottomBar = {
            Actions(
                isInitial = state.isInitial,
                onLocateClick = viewModel::onLocateClick,
                onChooseManuallyClick = viewModel::onSelectLocationClick,
                onDeclineClick = viewModel::onSkipLocationClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.spaceXl, vertical = dims.spaceXxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(dims.iconXl * 2)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(dims.iconXl),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(dims.spaceXl))

            Text(
                text = stringResource(R.string.location_title),
                style = MaterialTheme.appTypography.display,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(dims.spaceXl))

            Column(verticalArrangement = Arrangement.spacedBy(dims.spaceMd)) {
                Point(Icons.Default.AccessTime, stringResource(R.string.location_point_prayers))
                Point(Icons.Default.Explore, stringResource(R.string.location_point_qibla))
                Point(Icons.Default.Update, stringResource(R.string.location_point_background))
                Point(Icons.Default.Lock, stringResource(R.string.location_point_privacy))
            }
        }
    }
}

@Composable
private fun Actions(
    isInitial: Boolean,
    onLocateClick: () -> Unit,
    onChooseManuallyClick: () -> Unit,
    onDeclineClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = dims.spaceXl, vertical = dims.spaceLg),
        verticalArrangement = Arrangement.spacedBy(dims.spaceSm)
    ) {
        Button(
            onClick = onLocateClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ActionButtonHeight)
        ) {
            Text(
                text = stringResource(R.string.locate),
                modifier = Modifier.padding(vertical = dims.spaceXs),
                style = MaterialTheme.appTypography.title
            )
        }

        FilledTonalButton(
            onClick = onChooseManuallyClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ActionButtonHeight)
        ) {
            Text(
                text = stringResource(R.string.choose_manually),
                modifier = Modifier.padding(vertical = dims.spaceXs),
                style = MaterialTheme.appTypography.title
            )
        }

        if (isInitial) {
            // Kept deliberately quiet: declining is allowed but costs the main features
            TextButton(onClick = onDeclineClick, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.decline_location),
                        style = MaterialTheme.appTypography.button
                    )

                    Text(
                        text = stringResource(R.string.decline_location_hint),
                        style = MaterialTheme.appTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimensions.iconMd),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.width(MaterialTheme.dimensions.spaceMd))

        Text(text = text, style = MaterialTheme.appTypography.body)
    }
}
