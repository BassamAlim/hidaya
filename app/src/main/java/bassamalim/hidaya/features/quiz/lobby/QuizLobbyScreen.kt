package bassamalim.hidaya.features.quiz.lobby

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

@Composable
fun QuizLobbyScreen(viewModel: QuizLobbyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    val dims = MaterialTheme.dimensions

    MyScaffold(title = stringResource(R.string.quiz_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dims.screenPaddingHorizontal,
                    vertical = dims.screenPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(dims.spaceSm)
        ) {
            QuickQuizCard(onStartClick = viewModel::onStartQuizClick)

            Spacer(Modifier.height(dims.spaceSm))

            MySectionHeader(title = stringResource(R.string.quiz_categories))

            state.quizCategories.forEach { category ->
                CategoryRow(name = category, onClick = { viewModel.onCategoryClick(category) })
            }
        }
    }
}

@Composable
private fun QuickQuizCard(onStartClick: () -> Unit) {
    val dims = MaterialTheme.dimensions
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    MyCard(
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(dims.spaceXl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dims.iconXl + dims.spaceLg)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(dims.radiusLg)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dims.iconLg)
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = dims.spaceLg)
            ) {
                Text(
                    text = stringResource(R.string.quick_quiz),
                    style = MaterialTheme.appTypography.display,
                    color = contentColor
                )

                Text(
                    text = stringResource(R.string.quick_quiz_description),
                    style = MaterialTheme.appTypography.label,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(Modifier.height(dims.spaceLg))

        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.start_quiz),
                style = MaterialTheme.appTypography.button
            )
        }
    }
}

@Composable
private fun CategoryRow(name: String, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceLg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.appTypography.title
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.start_quiz),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
