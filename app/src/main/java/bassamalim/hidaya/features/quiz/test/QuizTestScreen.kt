package bassamalim.hidaya.features.quiz.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.Negative
import bassamalim.hidaya.core.ui.theme.Positive
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums

/** A streak is worth showing from its second correct answer */
private const val MIN_SHOWN_STREAK = 2

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
            QuestionHeader(state)

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
                        status = when {
                            !state.isRevealed -> AnswerStatus.OPEN
                            index == state.correctIndex -> AnswerStatus.CORRECT
                            index == state.chosenIndex -> AnswerStatus.WRONG
                            else -> AnswerStatus.OTHER
                        },
                        onClick = { viewModel.onAnswerClick(index) }
                    )
                }

                if (state.isRevealed) state.description?.let { DescriptionCard(it) }
            }

            if (state.isRevealed)
                Button(
                    onClick = viewModel::onNextClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dims.spaceMd)
                ) {
                    Text(
                        text = stringResource(R.string.next_question),
                        style = MaterialTheme.appTypography.button
                    )
                }
        }
    }
}

/** The question's number in this session and how it's going, then the streak. */
@Composable
private fun QuestionHeader(state: QuizTestUiState) {
    val dims = MaterialTheme.dimensions
    // The question on screen counts once it's answered
    val questionNum = state.sessionAnswered + if (state.isRevealed) 0 else 1

    Row(
        modifier = Modifier.padding(top = dims.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.quiz_question_number,
                    format(questionNum, state.numeralsLanguage)
                ),
                style = MaterialTheme.appTypography.title,
                color = MaterialTheme.colorScheme.primary
            )

            if (state.sessionAnswered > 0)
                Text(
                    text = stringResource(
                        R.string.quiz_session,
                        format(state.sessionCorrect, state.numeralsLanguage),
                        format(state.sessionAnswered, state.numeralsLanguage)
                    ),
                    style = MaterialTheme.appTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        AnimatedVisibility(
            visible = state.currentStreak >= MIN_SHOWN_STREAK,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.verse_guess_streak,
                        state.currentStreak,
                        format(state.currentStreak, state.numeralsLanguage)
                    ),
                    modifier = Modifier.padding(horizontal = dims.spaceMd, vertical = dims.spaceXs),
                    style = MaterialTheme.appTypography.caption
                )
            }
        }
    }
}

private enum class AnswerStatus { OPEN, CORRECT, WRONG, OTHER }

@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    status: AnswerStatus,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val accent = when (status) {
        AnswerStatus.CORRECT -> Positive
        AnswerStatus.WRONG -> Negative
        else -> null
    }
    val containerColor by animateColorAsState(
        targetValue = accent?.copy(alpha = 0.12f)
            ?: MaterialTheme.colorScheme.surfaceContainerLow,
        label = "answer container"
    )
    val contentColor =
        if (status == AnswerStatus.OTHER) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        enabled = status == AnswerStatus.OPEN,
        shape = RoundedCornerShape(dims.radiusLg),
        color = containerColor,
        border = BorderStroke(
            width = if (accent != null) dims.borderThick else dims.borderThin,
            color = accent ?: MaterialTheme.colorScheme.outlineVariant
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
                        color = accent ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (status) {
                    AnswerStatus.CORRECT, AnswerStatus.WRONG -> Icon(
                        imageVector =
                            if (status == AnswerStatus.CORRECT) Icons.Default.Check
                            else Icons.Default.Close,
                        contentDescription = stringResource(
                            if (status == AnswerStatus.CORRECT) R.string.correct_answer
                            else R.string.wrong_answer
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(dims.iconSm)
                    )
                    else -> Text(
                        text = letter,
                        style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(dims.spaceMd))

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.appTypography.body,
                color = contentColor
            )
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    val dims = MaterialTheme.dimensions

    MyCard(
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        Text(
            text = stringResource(R.string.about_the_answer),
            style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = description,
            modifier = Modifier.padding(top = dims.spaceXs),
            style = MaterialTheme.appTypography.body
        )
    }
}

private fun format(value: Int, numeralsLanguage: Language) =
    translateNums(value.toString(), numeralsLanguage)
