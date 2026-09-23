package bassamalim.hidaya.features.quiz.result

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MyLazyColumn
import bassamalim.hidaya.core.ui.components.MyScaffold
import bassamalim.hidaya.core.ui.theme.Negative
import bassamalim.hidaya.core.ui.theme.Positive
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.utils.LangUtils.translateNums

private val ScoreRingSize = 140.dp
private val ScoreRingWidth = 12.dp

@Composable
fun QuizResultScreen(viewModel: QuizResultViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) return

    val dims = MaterialTheme.dimensions
    val correctCount = state.questions.count { it.isAnsweredCorrectly() }

    MyScaffold(title = stringResource(R.string.quiz_result)) { padding ->
        MyLazyColumn(
            modifier = Modifier.padding(padding),
            lazyList = {
                item {
                    ScoreHeader(
                        scorePercent = state.scorePercent,
                        correctCount = correctCount,
                        wrongCount = state.questions.size - correctCount,
                        numeralsLanguage = state.numeralsLanguage
                    )
                }

                items(state.questions, key = { it.questionNum }) { question ->
                    QuestionCard(
                        question = question,
                        numeralsLanguage = state.numeralsLanguage,
                        modifier = Modifier.padding(
                            horizontal = dims.screenPaddingHorizontal,
                            vertical = dims.spaceSm
                        )
                    )
                }

                item { Spacer(Modifier.height(dims.spaceLg)) }
            }
        )
    }
}

private fun QuizResultQuestion.isAnsweredCorrectly() =
    chosenAnswerId == answers.indexOfFirst { it.isCorrect }

@Composable
private fun ScoreHeader(
    scorePercent: Int,
    correctCount: Int,
    wrongCount: Int,
    numeralsLanguage: Language
) {
    val dims = MaterialTheme.dimensions
    val scoreColor = when {
        scorePercent >= 80 -> Positive
        scorePercent >= 50 -> MaterialTheme.colorScheme.tertiary
        else -> Negative
    }

    // Starts at 0 and fills up to the score on first show
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "score_animation"
    )
    LaunchedEffect(scorePercent) {
        targetProgress = scorePercent / 100f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dims.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(ScoreRingSize),
                color = scoreColor,
                strokeWidth = ScoreRingWidth,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp
            )

            Text(
                text = translateNums(
                    string = "${(animatedProgress * 100).toInt()}%",
                    numeralsLanguage = numeralsLanguage
                ),
                style = MaterialTheme.appTypography.display.copy(fontSize = 36.sp),
                color = scoreColor
            )
        }

        Spacer(Modifier.height(dims.spaceLg))

        Row(horizontalArrangement = Arrangement.spacedBy(dims.spaceMd)) {
            StatTile(
                label = stringResource(R.string.correct),
                value = translateNums(correctCount.toString(), numeralsLanguage),
                color = Positive
            )

            StatTile(
                label = stringResource(R.string.wrong),
                value = translateNums(wrongCount.toString(), numeralsLanguage),
                color = Negative
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color) {
    val dims = MaterialTheme.dimensions

    Column(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(dims.radiusLg)
            )
            .padding(horizontal = dims.spaceXl, vertical = dims.spaceMd),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.appTypography.h1.copy(fontWeight = FontWeight.Bold),
            color = color
        )

        Text(
            text = label,
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionCard(
    question: QuizResultQuestion,
    numeralsLanguage: Language,
    modifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions
    val isCorrect = question.isAnsweredCorrectly()
    val statusColor = if (isCorrect) Positive else Negative

    MyCard(
        modifier = modifier,
        shape = RoundedCornerShape(dims.radiusLg),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dims.iconLg)
                    .background(color = statusColor.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = translateNums(question.questionNum.toString(), numeralsLanguage),
                    style = MaterialTheme.appTypography.label.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }

            Text(
                text = stringResource(R.string.question),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dims.spaceSm),
                style = MaterialTheme.appTypography.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                contentDescription = stringResource(
                    if (isCorrect) R.string.correct else R.string.wrong
                ),
                modifier = Modifier.size(dims.iconMd),
                tint = statusColor
            )
        }

        Text(
            text = question.questionText,
            modifier = Modifier.padding(vertical = dims.spaceMd),
            style = MaterialTheme.appTypography.title
        )

        Column(verticalArrangement = Arrangement.spacedBy(dims.spaceSm)) {
            question.answers.forEachIndexed { index, answer ->
                AnswerItem(
                    answerText = answer.text,
                    isCorrectAnswer = answer.isCorrect,
                    isChosenAnswer = index == question.chosenAnswerId
                )
            }
        }
    }
}

@Composable
private fun AnswerItem(answerText: String, isCorrectAnswer: Boolean, isChosenAnswer: Boolean) {
    val dims = MaterialTheme.dimensions
    val highlight = when {
        isCorrectAnswer -> Positive
        isChosenAnswer -> Negative
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = highlight?.copy(alpha = 0.12f)
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(dims.radiusMd)
            )
            .padding(horizontal = dims.spaceMd, vertical = dims.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = answerText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.appTypography.body,
            color = highlight ?: MaterialTheme.colorScheme.onSurface
        )

        if (highlight != null) {
            Icon(
                imageVector = if (isCorrectAnswer) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = dims.spaceSm)
                    .size(dims.iconSm + dims.spaceXs),
                tint = highlight
            )
        }
    }
}
