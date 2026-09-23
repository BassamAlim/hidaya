package bassamalim.hidaya.features.quiz.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums

private val DotHeight = 6.dp

@Composable
fun QuizTestScreen(viewModel: QuizTestViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    val dims = MaterialTheme.dimensions

    MyScaffold(title = stringResource(R.string.quiz_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = dims.screenPaddingHorizontal)
        ) {
            ProgressDots(
                answered = state.answeredQuestions,
                currentIdx = state.questionIdx,
                onDotClick = viewModel::onQuestionClick
            )

            Text(
                text = stringResource(
                    R.string.question_progress,
                    state.titleQuestionNumber,
                    translateNums(
                        string = viewModel.totalQuestions.toString(),
                        numeralsLanguage = state.numeralsLanguage
                    )
                ),
                modifier = Modifier.padding(top = dims.spaceSm),
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = dims.spaceLg),
                verticalArrangement = Arrangement.spacedBy(dims.spaceMd)
            ) {
                Text(
                    text = state.question,
                    modifier = Modifier.padding(bottom = dims.spaceSm),
                    style = MaterialTheme.appTypography.h1
                )

                val letters = stringArrayResource(R.array.answer_letters)
                state.answers.forEachIndexed { index, answer ->
                    AnswerOption(
                        letter = letters.getOrElse(index) { "" },
                        text = answer,
                        isSelected = index == state.selection,
                        onClick = { viewModel.onAnswerSelected(index) }
                    )
                }
            }

            BottomBar(
                isLastQuestion = state.questionIdx == viewModel.totalQuestions - 1,
                isAllAnswered = state.allAnswered,
                isPreviousButtonEnabled = state.previousButtonEnabled,
                isNextButtonEnabled = state.nextButtonEnabled,
                onPreviousQuestionClick = viewModel::onPreviousQuestionClick,
                onNextQuestionClick = viewModel::onNextQuestionClick
            )
        }
    }
}

@Composable
private fun ProgressDots(answered: List<Boolean>, currentIdx: Int, onDotClick: (Int) -> Unit) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dims.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(dims.spaceXs)
    ) {
        answered.forEachIndexed { index, isAnswered ->
            val color by animateColorAsState(
                targetValue = when {
                    index == currentIdx -> MaterialTheme.colorScheme.primary
                    isAnswered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "progress dot"
            )

            // The dot itself is thin, so the tap area around it is taller
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDotClick(index) }
                    .padding(vertical = dims.spaceSm),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(DotHeight)
                        .background(color = color, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val containerColor by animateColorAsState(
        targetValue =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "answer container"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) dims.borderThick else dims.borderThin,
        label = "answer border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        color = containerColor,
        border = BorderStroke(
            width = borderWidth,
            color =
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dims.spaceLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dims.iconLg)
                    .background(
                        color =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold),
                    color =
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(dims.spaceMd))

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.appTypography.body,
                color =
                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BottomBar(
    isLastQuestion: Boolean,
    isAllAnswered: Boolean,
    isPreviousButtonEnabled: Boolean,
    isNextButtonEnabled: Boolean,
    onPreviousQuestionClick: () -> Unit,
    onNextQuestionClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onPreviousQuestionClick,
            enabled = isPreviousButtonEnabled
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null
            )

            Spacer(Modifier.width(dims.spaceXs))

            Text(
                text = stringResource(R.string.previous),
                style = MaterialTheme.appTypography.button
            )
        }

        Button(
            onClick = onNextQuestionClick,
            enabled = isNextButtonEnabled
        ) {
            Text(
                text = stringResource(
                    when {
                        !isLastQuestion -> R.string.next
                        isAllAnswered -> R.string.finish_quiz
                        else -> R.string.answer_all_questions
                    }
                ),
                style = MaterialTheme.appTypography.button
            )

            if (!isLastQuestion) {
                Spacer(Modifier.width(dims.spaceXs))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        }
    }
}
