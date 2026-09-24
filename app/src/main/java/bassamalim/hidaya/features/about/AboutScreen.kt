package bassamalim.hidaya.features.about

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.models.Source
import bassamalim.hidaya.core.ui.components.MyListItem
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun AboutScreen(viewModel: AboutViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current!!

    MyScaffold(
        title = stringResource(R.string.about),
        snackBarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 5.dp)
        ) {
            ThankYouMessage(viewModel::onTitleClick)

            SourcesList(state.sources)

            HiddenArea(
                isDevModeOn = state.isDevModeEnabled,
                lastDailyUpdate = state.lastDailyUpdate,
                onRebuildDatabaseClick = { message ->
                    viewModel.onRebuildDatabaseClick(
                        activity = activity,
                        snackbarHostState = snackbarHostState,
                        message = message
                    )
                },
                onResetTutorials = { message ->
                    viewModel.onResetTutorials(
                        snackbarHostState = snackbarHostState,
                        message = message
                    )
                },
                onResetOnboarding = { message ->
                    viewModel.onResetOnboarding(
                        snackbarHostState = snackbarHostState,
                        message = message
                    )
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.ThankYouMessage(onTitleClick: () -> Unit) {
    MyText(
        text = stringResource(R.string.thanks),
        fontSize = 25.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(top = 15.dp, bottom = 20.dp)
            .align(Alignment.CenterHorizontally)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onTitleClick
            )
    )
}

@Composable
private fun ColumnScope.SourcesList(sources: List<Source>) {
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .weight(1F)
            .verticalScroll(rememberScrollState())
    ) {
        MySectionHeader(title = stringResource(R.string.sources))

        // Whole rows are tappable, not just the link word inside a sentence
        sources.forEach { source ->
            MyListItem(
                headline = source.title,
                supporting = source.sourceName,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = { uriHandler.openUri(source.url) }
            )

            if (source != sources.last()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.spaceLg),
                    thickness = MaterialTheme.dimensions.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.HiddenArea(
    isDevModeOn: Boolean,
    lastDailyUpdate: String,
    onRebuildDatabaseClick: (String) -> Unit,
    onResetTutorials: (String) -> Unit,
    onResetOnboarding: (String) -> Unit
) {
    AnimatedVisibility(visible = isDevModeOn, enter = expandVertically()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val databaseRebuiltMessage = stringResource(R.string.database_rebuilt)
            // rebuild database button
            FilledTonalButton(onClick = { onRebuildDatabaseClick(databaseRebuiltMessage) }) {
                MyText(stringResource(R.string.rebuild_database))
            }

            val tutorialsResetMessage = stringResource(R.string.tutorials_reset)
            // reset tutorials do not show again button
            FilledTonalButton(onClick = { onResetTutorials(tutorialsResetMessage) }) {
                MyText(stringResource(R.string.reset_tutorials))
            }

            val onboardingResetMessage = stringResource(R.string.onboarding_reset)
            FilledTonalButton(onClick = { onResetOnboarding(onboardingResetMessage) }) {
                MyText(stringResource(R.string.reset_onboarding))
            }

            // last daily update text
            MyText(
                text = lastDailyUpdate,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(10.dp)
            )
        }
    }
}