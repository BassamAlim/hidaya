package bassamalim.hidaya.features.verseGuess.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.roundToInt

private val LINE_HEIGHT = 24.dp
private val MIN_BOX_WIDTH = 18.dp
private val BOX_GAP = 3.dp
private val BOX_CORNER = 3.dp
private val PAGE_MARKER_WIDTH = 18.dp
private val SURA_HEADER_HEIGHT = 36.dp

/** Shorter than the system long press, so sliding starts without a noticeable wait */
private const val HOLD_TIMEOUT_MILLIS = 300L
/** How far the finger can stray outside the selected box before a neighbor takes over */
private val STICKY_SLACK_X = 3.dp
private val STICKY_SLACK_Y = 6.dp

/** Gap between the finger and its label */
private val LABEL_LIFT = 40.dp

private val AUTO_SCROLL_EDGE = 56.dp
private val AUTO_SCROLL_MAX_STEP = 16.dp

private val RAIL_WIDTH = 32.dp
/** From the rail's right edge */
private val RAIL_TRACK_INSET = 10.dp
private val RAIL_THUMB_WIDTH = 6.dp
private val RAIL_THUMB_HEIGHT = 28.dp
private val RAIL_TICK_LENGTH = 8.dp
private val RAIL_MIN_LABEL_SPACING = 12.dp
private val RAIL_MARKER_RADIUS = 5.dp

/** About one dp per four letters, so long verses read as long, but never too small to hit. */
private fun boxWidth(textLength: Int) = maxOf(MIN_BOX_WIDTH.value, textLength / 4f).dp

/** Where the rail thumb sits when row [rowIndex] is at the top of the map. */
private class JuzTick(val rowIndex: Int, val juzNumText: String)

/** A dot on the rail for a verse on row [rowIndex], placed like a [JuzTick]. */
private class RailMarker(val rowIndex: Int, val color: Color)

@Composable
fun VerseMapItem.locationText(suraNames: List<String>) =
    stringResource(R.string.verse_location, suraNames[suraNum - 1], verseNumText, pageNumText)

/**
 * The whole of [items] as boxes that flow like the mushaf's text, always right to left.
 * Tap a box to select it, or hold and slide to scan with a label naming the verse under the
 * finger; sliding near the top or bottom edge scrolls the map. The rail on the right edge
 * jumps anywhere, with a tick at every juz.
 *
 * Once [answer] is set, the map scrolls to it and highlights it, and selection is locked.
 */
@Composable
fun VerseMap(
    items: List<VerseMapItem>,
    suraNames: List<String>,
    selectedIndex: Int?,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    answer: IntRange? = null
) {
    val sidePadding = MaterialTheme.dimensions.spaceMd
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    // A new scope starts from its top
    val listState = remember(items) { LazyListState() }

    val baseViewConfiguration = LocalViewConfiguration.current
    val mapViewConfiguration = remember(baseViewConfiguration) {
        object : ViewConfiguration by baseViewConfiguration {
            override val longPressTimeoutMillis = HOLD_TIMEOUT_MILLIS
        }
    }

    BoxWithConstraints(modifier) {
        val railWidthPx = with(density) { RAIL_WIDTH.toPx() }
        // Lines run leftwards from the rail
        val lineEndPx = constraints.maxWidth - railWidthPx
        val lineWidthPx = lineEndPx - with(density) { sidePadding.toPx() }
        val heightPx = constraints.maxHeight.toFloat()

        val rows = remember(items, lineWidthPx) {
            with(density) {
                layoutVerseMap(
                    items = items,
                    lineWidth = lineWidthPx,
                    gap = BOX_GAP.toPx(),
                    markerWidth = PAGE_MARKER_WIDTH.toPx()
                ) { boxWidth(it.textLength).toPx() }
            }
        }
        val rowFirstVerses = remember(rows) { rows.firstVerseIndices() }
        val verseRows = remember(rows) { rows.verseRowIndices(items.size) }
        val juzTicks = remember(rowFirstVerses) {
            rowFirstVerses.withIndex()
                .distinctBy { (_, verseIndex) -> items[verseIndex].juzNum }
                .map { (rowIndex, verseIndex) -> JuzTick(rowIndex, items[verseIndex].juzNumText) }
        }
        // Rows past this can't reach the top, so the rail maps its length onto 0 to this
        val scrollRange by remember(rows) {
            derivedStateOf {
                (rows.size - listState.layoutInfo.visibleItemsInfo.size).coerceAtLeast(1)
            }
        }

        var slidePosition by remember { mutableStateOf<Offset?>(null) }
        var railFraction by remember { mutableStateOf<Float?>(null) }
        var railY by remember { mutableFloatStateOf(0f) }
        var railTargetRow by remember { mutableStateOf<Int?>(null) }
        var railJuz by remember { mutableIntStateOf(0) }
        val isLocked = answer != null
        val colors = MaterialTheme.colorScheme
        val railMarkers = listOfNotNull(
            answer?.let { RailMarker(verseRows[it.first], colors.tertiary) },
            selectedIndex?.let { RailMarker(verseRows[it], colors.primary) }
        )

        /** Whether [position] is still on verse [index]'s box, give or take the sticky slack. */
        fun isOnVerse(position: Offset, index: Int): Boolean {
            val rowIndex = verseRows[index]
            val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == rowIndex }
                ?: return false
            val box = (rows[rowIndex] as VerseMapRow.Line).box(index) ?: return false
            val slackX = with(density) { STICKY_SLACK_X.toPx() }
            val slackY = with(density) { STICKY_SLACK_Y.toPx() }
            return abs(lineEndPx - box.start - box.width / 2 - position.x) <= box.width / 2 + slackX
                    && position.y in row.offset - slackY..row.offset + row.size + slackY
        }

        fun verseAt(position: Offset): Int? {
            val row = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                position.y.toInt() in it.offset until it.offset + it.size
            } ?: return null
            val line = rows[row.index] as? VerseMapRow.Line ?: return null
            return line.nearestVerse(lineEndPx - position.x)
        }

        fun selectAt(position: Offset, sticky: Boolean, tick: Boolean = true) {
            val current = currentSelectedIndex
            if (sticky && current != null && isOnVerse(position, current)) return
            val index = verseAt(position) ?: return
            if (index == current) return
            if (tick) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            currentOnSelect(index)
        }

        fun onRailDrag(fraction: Float?, y: Float) {
            railFraction = fraction
            railY = y
            if (fraction == null || rows.isEmpty()) {
                railTargetRow = null
                return
            }
            val row = (fraction * scrollRange).roundToInt().coerceAtMost(rows.lastIndex)
            railTargetRow = row
            val juz = items[rowFirstVerses[row]].juzNum
            if (juz != railJuz) {
                railJuz = juz
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        LaunchedEffect(answer, rows) {
            val answerRow = verseRows[(answer ?: return@LaunchedEffect).first]
            // Leaves a third of the screen above the answer, for context
            val rowsAbove = listState.layoutInfo.visibleItemsInfo.size / 3
            listState.animateScrollToItem((answerRow - rowsAbove).coerceAtLeast(0))
        }

        LaunchedEffect(listState) {
            snapshotFlow { railTargetRow }
                .filterNotNull()
                .collectLatest { listState.scrollToItem(it) }
        }

        LaunchedEffect(slidePosition != null) {
            val edge = with(density) { AUTO_SCROLL_EDGE.toPx() }
            val maxStep = with(density) { AUTO_SCROLL_MAX_STEP.toPx() }
            while (true) {
                withFrameNanos {}
                val position = slidePosition ?: break
                val step = when {
                    position.y < edge -> -maxStep * (1 - position.y / edge)
                    position.y > heightPx - edge -> maxStep * (1 - (heightPx - position.y) / edge)
                    else -> 0f
                }.coerceIn(-maxStep, maxStep)
                if (step != 0f) {
                    listState.scrollBy(step)
                    selectAt(position, sticky = true)
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            val textMeasurer = rememberTextMeasurer(cacheSize = 64)

            CompositionLocalProvider(LocalViewConfiguration provides mapViewConfiguration) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(rows, isLocked) {
                            if (isLocked) return@pointerInput
                            detectSlide(
                                onStart = {
                                    // The hold's own buzz stands in for the selection tick
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    slidePosition = it
                                    selectAt(it, sticky = false, tick = false)
                                },
                                onSlide = {
                                    slidePosition = it
                                    selectAt(it, sticky = true)
                                },
                                onEnd = { slidePosition = null }
                            )
                        }
                        .pointerInput(rows, isLocked) {
                            if (isLocked) return@pointerInput
                            detectTapGestures { selectAt(it, sticky = false) }
                        }
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(count = rows.size, contentType = { rows[it]::class }) { i ->
                            when (val row = rows[i]) {
                                is VerseMapRow.SuraHeader -> SuraHeader(
                                    name = suraNames[row.suraNum - 1],
                                    modifier = Modifier.absolutePadding(
                                        left = sidePadding,
                                        right = RAIL_WIDTH
                                    )
                                )
                                is VerseMapRow.Line -> VerseMapLine(
                                    line = row,
                                    selectedIndex = selectedIndex,
                                    answer = answer,
                                    lineEndPx = lineEndPx,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }
                    }
                }
            }

            ScrollRail(
                listState = listState,
                scrollRange = scrollRange,
                juzTicks = juzTicks,
                markers = railMarkers,
                dragFraction = railFraction,
                onDrag = ::onRailDrag,
                modifier = Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .width(RAIL_WIDTH)
                    .fillMaxHeight()
            )

            val position = slidePosition
            val selected = selectedIndex?.let(items::getOrNull)
            if (position != null && selected != null) {
                FloatingLabel(text = selected.locationText(suraNames)) { label, container ->
                    // Centered above the finger, or below it when there's no room above
                    val above = position.y - LABEL_LIFT.toPx() - label.height
                    val y = if (above >= 0) above else position.y + LABEL_LIFT.toPx()
                    IntOffset(
                        x = (position.x - label.width / 2f).roundToInt()
                            .coerceIn(0, (container.width - label.width).coerceAtLeast(0)),
                        y = y.roundToInt()
                            .coerceIn(0, (container.height - label.height).coerceAtLeast(0))
                    )
                }
            }

            val targetRow = railTargetRow
            if (railFraction != null && targetRow != null) {
                val item = items[rowFirstVerses[targetRow]]
                FloatingLabel(
                    text = stringResource(
                        R.string.verse_map_scroll_position,
                        item.juzNumText,
                        suraNames[item.suraNum - 1]
                    )
                ) { label, container ->
                    // Beside the rail, level with the finger
                    IntOffset(
                        x = (container.width - railWidthPx).roundToInt() - label.width,
                        y = (railY - label.height / 2f).roundToInt()
                            .coerceIn(0, (container.height - label.height).coerceAtLeast(0))
                    )
                }
            }
        }
    }
}

/**
 * Hold, then slide. Once the hold is recognized, the gesture's events are consumed in the
 * initial pass, so the list under the finger doesn't scroll while sliding.
 */
private suspend fun PointerInputScope.detectSlide(
    onStart: (Offset) -> Unit,
    onSlide: (Offset) -> Unit,
    onEnd: () -> Unit
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val hold = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

    try {
        onStart(hold.position)
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                .firstOrNull { it.id == hold.id } ?: break
            change.consume()
            if (!change.pressed) break
            onSlide(change.position)
        }
    } finally {
        onEnd()
    }
}

/**
 * A track with a thumb for the scroll position and a tick at each juz. Touching anywhere on it
 * reports the fraction of the track under the finger, and null once the finger lifts.
 */
@Composable
private fun ScrollRail(
    listState: LazyListState,
    scrollRange: Int,
    juzTicks: List<JuzTick>,
    markers: List<RailMarker>,
    dragFraction: Float?,
    onDrag: (fraction: Float?, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val currentOnDrag by rememberUpdatedState(onDrag)
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)
    val labelStyle = TextStyle(
        fontFamily = MaterialTheme.appTypography.caption.fontFamily,
        fontSize = 8.sp,
        color = colors.onSurfaceVariant
    )

    Canvas(
        modifier
            // Keeps the back gesture from taking vertical drags that start on the edge
            .systemGestureExclusion()
            .pointerInput(Unit) {
                // The thumb's center travels between half a thumb from each end
                val inset = RAIL_THUMB_HEIGHT.toPx() / 2
                fun fractionAt(y: Float) =
                    ((y - inset) / (size.height - 2 * inset)).coerceIn(0f, 1f)

                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    try {
                        currentOnDrag(fractionAt(down.position.y), down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes
                                .firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            currentOnDrag(fractionAt(change.position.y), change.position.y)
                        }
                    } finally {
                        currentOnDrag(null, 0f)
                    }
                }
            }
    ) {
        val inset = RAIL_THUMB_HEIGHT.toPx() / 2
        val trackLength = size.height - 2 * inset
        val trackX = size.width - RAIL_TRACK_INSET.toPx()
        fun yOf(fraction: Float) = inset + fraction.coerceIn(0f, 1f) * trackLength

        drawLine(
            color = colors.outlineVariant,
            start = Offset(trackX, inset),
            end = Offset(trackX, inset + trackLength),
            strokeWidth = 2.dp.toPx()
        )

        var lastLabelY = Float.NEGATIVE_INFINITY
        juzTicks.forEach { tick ->
            val y = yOf(tick.rowIndex.toFloat() / scrollRange)
            val tickHalf = RAIL_TICK_LENGTH.toPx() / 2
            drawLine(
                color = colors.outline,
                start = Offset(trackX - tickHalf, y),
                end = Offset(trackX + tickHalf, y),
                strokeWidth = 1.dp.toPx()
            )
            if (y - lastLabelY >= RAIL_MIN_LABEL_SPACING.toPx()) {
                // Labels sit on the map side of the track
                val text = textMeasurer.measure(tick.juzNumText, labelStyle)
                drawText(
                    textLayoutResult = text,
                    topLeft = Offset(
                        trackX - tickHalf - 2.dp.toPx() - text.size.width,
                        y - text.size.height / 2f
                    )
                )
                lastLabelY = y
            }
        }

        // Read here so scrolling only redraws the rail
        val fraction = dragFraction ?: (listState.firstVisibleItemIndex.toFloat() / scrollRange)
        val thumbCenter = yOf(fraction)
        val thumbWidth = RAIL_THUMB_WIDTH.toPx() * if (dragFraction != null) 1.5f else 1f
        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(trackX - thumbWidth / 2, thumbCenter - inset),
            size = Size(thumbWidth, RAIL_THUMB_HEIGHT.toPx()),
            cornerRadius = CornerRadius(thumbWidth / 2)
        )

        markers.forEach { marker ->
            drawCircle(
                color = marker.color,
                radius = RAIL_MARKER_RADIUS.toPx(),
                center = Offset(trackX, yOf(marker.rowIndex.toFloat() / scrollRange))
            )
        }
    }
}

@Composable
private fun SuraHeader(name: String, modifier: Modifier = Modifier) {
    val dims = MaterialTheme.dimensions
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SURA_HEADER_HEIGHT)
            .padding(top = dims.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f), color = lineColor)
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = dims.spaceSm),
            style = MaterialTheme.appTypography.label,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(Modifier.weight(1f), color = lineColor)
    }
}

@Composable
private fun VerseMapLine(
    line: VerseMapRow.Line,
    selectedIndex: Int?,
    answer: IntRange?,
    lineEndPx: Float,
    textMeasurer: TextMeasurer
) {
    val colors = MaterialTheme.colorScheme
    val markerTextStyle = TextStyle(
        fontFamily = MaterialTheme.appTypography.caption.fontFamily,
        fontSize = 8.sp,
        color = colors.outline
    )

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(LINE_HEIGHT)
    ) {
        val verticalGap = BOX_GAP.toPx()
        line.cells.forEach { cell ->
            // Cells are measured from the right edge
            val left = lineEndPx - cell.start - cell.width

            when (cell) {
                is VerseMapCell.PageMarker -> {
                    val centerX = left + cell.width / 2
                    drawLine(
                        color = colors.outline,
                        start = Offset(centerX, verticalGap),
                        end = Offset(centerX, size.height * 0.45f),
                        strokeWidth = 1.dp.toPx()
                    )
                    val text = textMeasurer.measure(cell.pageNumText, markerTextStyle)
                    drawText(
                        textLayoutResult = text,
                        topLeft = Offset(centerX - text.size.width / 2f, size.height * 0.5f)
                    )
                }
                is VerseMapCell.VerseBox -> {
                    // Selected and answer boxes grow to the full line height
                    val color = when {
                        cell.index == selectedIndex -> colors.primary
                        answer != null && cell.index in answer -> colors.tertiary
                        else -> null
                    }
                    val inset = if (color != null) 0f else verticalGap
                    drawRoundRect(
                        color = color ?: colors.surfaceVariant,
                        topLeft = Offset(left, inset),
                        size = Size(cell.width, size.height - 2 * inset),
                        cornerRadius = CornerRadius(BOX_CORNER.toPx())
                    )
                }
            }
        }
    }
}

/** A small label over the map, positioned by [placement] from its own size and the map's. */
@Composable
private fun FloatingLabel(
    text: String,
    placement: Density.(label: IntSize, container: IntSize) -> IntOffset
) {
    val dims = MaterialTheme.dimensions
    val shape = RoundedCornerShape(dims.radiusMd)

    Text(
        text = text,
        style = MaterialTheme.appTypography.label,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier
            .layout { measurable, constraints ->
                val label = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val position = placement(
                    IntSize(label.width, label.height),
                    IntSize(constraints.maxWidth, constraints.maxHeight)
                )
                layout(constraints.maxWidth, constraints.maxHeight) { label.place(position) }
            }
            .shadow(dims.elevationSm, shape)
            .background(MaterialTheme.colorScheme.inverseSurface, shape)
            .padding(horizontal = dims.spaceSm, vertical = dims.spaceXs)
    )
}
