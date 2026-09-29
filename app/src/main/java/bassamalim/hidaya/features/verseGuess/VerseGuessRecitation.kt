package bassamalim.hidaya.features.verseGuess

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.enums.PlaybackStatus
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.drawEightPointStar
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.core.ui.theme.hafs_smart
import kotlin.math.roundToInt

private val VERSE_TEXT_SIZE = 22.sp
private val VERSE_LINE_HEIGHT = 44.sp
/** About three lines, so the map keeps most of the screen */
private val VERSE_TEXT_MAX_HEIGHT = 148.dp
/** Fades the text into the card where it scrolls under the edges */
private val VERSE_TEXT_FADE = 16.dp
private val SEGMENT_HEIGHT = 4.dp
private const val EMPHASIS_MILLIS = 350
private const val DIMMED_ALPHA = 0.35f
private const val MARKER_ID = "marker"
private const val VERSE_TAG = "verse"

/**
 * The round's recitation: the clip's verses in the mushaf's script, the one being recited
 * brought forward and the others dimmed, over a progress bar with a segment per verse, and the
 * playback controls. Tapping a verse plays from it.
 *
 * Until [isRevealed], each verse ends in a plain star instead of its numbered marker, and the
 * sura isn't named, since both would give the answer away.
 */
@Composable
internal fun RecitationCard(
    verses: List<ClipVerse>,
    playback: ClipPlayback,
    readVerseProgress: () -> Float,
    reciterName: String,
    suraName: String?,
    isRevealed: Boolean,
    onPlayPauseClick: () -> Unit,
    onReplayClick: () -> Unit,
    onVerseClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = MaterialTheme.dimensions
    var isTextShown by rememberSaveable { mutableStateOf(true) }
    // Nothing stands out before playback starts or after it ends
    val currentVerse = playback.verseIndex.takeIf {
        playback.status == PlaybackStatus.PLAYING ||
                playback.status == PlaybackStatus.PAUSED ||
                playback.status == PlaybackStatus.BUFFERING
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    MyCard(
        modifier = modifier,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        AnimatedVisibility(visible = isTextShown) {
            ClipText(
                verses = verses,
                currentVerse = currentVerse,
                showsNumbers = isRevealed,
                background = containerColor,
                onVerseClick = onVerseClick,
                modifier = Modifier.padding(
                    start = dims.spaceLg,
                    end = dims.spaceLg,
                    top = dims.spaceMd
                )
            )
        }

        ClipProgress(
            playback = playback,
            readVerseProgress = readVerseProgress,
            modifier = Modifier.padding(
                start = dims.spaceLg,
                end = dims.spaceLg,
                top = if (isTextShown) dims.spaceSm else dims.spaceMd
            )
        )

        Controls(
            playback = playback,
            reciterName = reciterName,
            suraName = suraName,
            isTextShown = isTextShown,
            onTextToggle = { isTextShown = !isTextShown },
            onPlayPauseClick = onPlayPauseClick,
            onReplayClick = onReplayClick
        )
    }
}

@Composable
private fun ClipText(
    verses: List<ClipVerse>,
    currentVerse: Int?,
    showsNumbers: Boolean,
    background: Color,
    onVerseClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // Dimmed colors are made opaque over the card: the font draws a word's letters as
    // overlapping glyphs, and a translucent color would darken where they overlap
    fun dimmed(color: Color) = color.copy(alpha = DIMMED_ALPHA).compositeOver(background)

    val textColors = verses.indices.map { i ->
        animateColorAsState(
            targetValue =
                if (currentVerse == null || i == currentVerse) colors.onSurface
                else dimmed(colors.onSurface),
            animationSpec = tween(EMPHASIS_MILLIS),
            label = "verse text"
        ).value
    }
    val markerColors = verses.indices.map { i ->
        animateColorAsState(
            targetValue =
                if (currentVerse == null || i == currentVerse) colors.primary
                else dimmed(colors.primary),
            animationSpec = tween(EMPHASIS_MILLIS),
            label = "verse marker"
        ).value
    }

    val verseStarts = IntArray(verses.size)
    val text = buildAnnotatedString {
        verses.forEachIndexed { i, verse ->
            val start = length
            verseStarts[i] = start
            withStyle(SpanStyle(color = textColors[i])) { append(verse.text) }
            append(' ')
            if (showsNumbers) withStyle(SpanStyle(color = markerColors[i])) { append(verse.marker) }
            else appendInlineContent("$MARKER_ID$i", alternateText = "۝")
            append(' ')
            addStringAnnotation(VERSE_TAG, "$i", start, length)
        }
    }
    val markers = verses.indices.associate { i ->
        "$MARKER_ID$i" to InlineTextContent(
            Placeholder(0.7.em, 0.7.em, PlaceholderVerticalAlign.TextCenter)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawEightPointStar(
                    color = markerColors[i],
                    center = center,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()

    // Keeps the recited verse in view, a line of context above it. Keyed on the verses too, so
    // it never reads the starts of an earlier clip, or of none while a round's text loads.
    LaunchedEffect(currentVerse, layout, verses) {
        val textLayout = layout ?: return@LaunchedEffect
        val verseStart = currentVerse?.let(verseStarts::getOrNull) ?: return@LaunchedEffect
        if (verseStart > textLayout.layoutInput.text.length) return@LaunchedEffect
        val line = textLayout.getLineForOffset(verseStart)
        val target = textLayout.getLineTop((line - 1).coerceAtLeast(0))
        scrollState.animateScrollTo(target.roundToInt())
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(max = VERSE_TEXT_MAX_HEIGHT)
            .fadingEdges(
                top = scrollState.value > 0,
                bottom = scrollState.value < scrollState.maxValue,
                length = with(density) { VERSE_TEXT_FADE.toPx() }
            )
            .verticalScroll(scrollState)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(text) {
                    detectTapGestures { position ->
                        val textLayout = layout ?: return@detectTapGestures
                        verseAt(text, textLayout, position)?.let(onVerseClick)
                    }
                },
            inlineContent = markers,
            onTextLayout = { layout = it },
            style = TextStyle(
                fontFamily = hafs_smart,
                fontSize = VERSE_TEXT_SIZE,
                lineHeight = VERSE_LINE_HEIGHT,
                textAlign = TextAlign.Center
            )
        )
    }
}

private fun verseAt(text: AnnotatedString, layout: TextLayoutResult, position: Offset): Int? {
    val offset = layout.getOffsetForPosition(position)
    return text.getStringAnnotations(VERSE_TAG, offset, offset).firstOrNull()?.item?.toInt()
}

/** Fades content out towards the edges it scrolls past. */
private fun Modifier.fadingEdges(top: Boolean, bottom: Boolean, length: Float) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        if (top) drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = length
            ),
            size = Size(size.width, length),
            blendMode = BlendMode.DstIn
        )
        if (bottom) drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - length,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - length),
            size = Size(size.width, length),
            blendMode = BlendMode.DstIn
        )
    }

/** A segment per verse: full for the ones heard, filling for the one being recited. */
@Composable
private fun ClipProgress(
    playback: ClipPlayback,
    readVerseProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val gap = MaterialTheme.dimensions.spaceXs

    // Polled every frame while playing, and read only when drawing, so it doesn't recompose
    val progress by produceState(readVerseProgress(), playback) {
        value = readVerseProgress()
        while (playback.status == PlaybackStatus.PLAYING) {
            withFrameNanos {}
            value = readVerseProgress()
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(SEGMENT_HEIGHT)
    ) {
        val gapPx = gap.toPx()
        val segmentWidth = (size.width - gapPx * (CLIP_LENGTH - 1)) / CLIP_LENGTH
        val radius = CornerRadius(size.height / 2)

        repeat(CLIP_LENGTH) { i ->
            val fill = when {
                i < playback.verseIndex -> 1f
                i == playback.verseIndex -> progress
                else -> 0f
            }
            // Segments run right to left, in reading order, and fill the same way
            val left = size.width - (i + 1) * segmentWidth - i * gapPx

            drawRoundRect(
                color = colors.surfaceVariant,
                topLeft = Offset(left, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = radius
            )
            if (fill > 0f) drawRoundRect(
                color = colors.primary,
                topLeft = Offset(left + segmentWidth * (1 - fill), 0f),
                size = Size(segmentWidth * fill, size.height),
                cornerRadius = radius
            )
        }
    }
}

@Composable
private fun Controls(
    playback: ClipPlayback,
    reciterName: String,
    suraName: String?,
    isTextShown: Boolean,
    onTextToggle: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onReplayClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions
    val isError = playback.status == PlaybackStatus.ERROR

    Row(
        modifier = Modifier.padding(
            start = dims.spaceLg,
            end = dims.spaceSm,
            top = dims.spaceXs,
            bottom = dims.spaceSm
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // The sura is named once revealed, above the reciter
            Text(
                text = when {
                    isError -> stringResource(R.string.verse_audio_error)
                    suraName != null -> suraName
                    else -> reciterName
                },
                style = MaterialTheme.appTypography.label,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    suraName != null -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = if (isError) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!isError && suraName != null)
                Text(
                    text = reciterName,
                    style = MaterialTheme.appTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
        }

        IconButton(onClick = onTextToggle) {
            Icon(
                imageVector =
                    if (isTextShown) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (isTextShown) R.string.hide_verse_text else R.string.show_verse_text
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onReplayClick) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = stringResource(R.string.replay),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FilledIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(dims.minTouchTarget)
        ) {
            when (playback.status) {
                PlaybackStatus.BUFFERING -> CircularProgressIndicator(
                    modifier = Modifier.size(dims.iconMd - dims.spaceXs),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = dims.borderThick
                )
                PlaybackStatus.PLAYING -> Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = stringResource(R.string.play_pause_btn_description)
                )
                else -> Icon(
                    imageVector = if (isError) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_pause_btn_description)
                )
            }
        }
    }
}
