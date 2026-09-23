package bassamalim.hidaya.features.remembrances.categoriesMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.components.MyCard
import bassamalim.hidaya.core.ui.components.MySectionHeader
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

// Reader ids of the two most-used remembrances (the same ones the daily reminders open)
private const val MORNING_REMEMBRANCE_ID = 0
private const val EVENING_REMEMBRANCE_ID = 1

private data class CategoryItem(
    val id: Int,
    val titleRes: Int,
    val iconRes: Int? = null,
    val imageVector: ImageVector? = null
)

private val categoryItems = listOf(
    CategoryItem(0, R.string.day_and_night_remembrances, R.drawable.ic_day_and_night),
    CategoryItem(1, R.string.prayers_remembrances, R.drawable.ic_praying),
    CategoryItem(2, R.string.quran_remembrances, R.drawable.ic_quran),
    CategoryItem(3, R.string.actions_remembrances, R.drawable.ic_moving),
    CategoryItem(4, R.string.events_remembrances, R.drawable.ic_events),
    CategoryItem(5, R.string.emotion_remembrances, R.drawable.ic_emotions),
    CategoryItem(6, R.string.places_remembrances, imageVector = Icons.AutoMirrored.Default.Logout),
    CategoryItem(7, R.string.title_more, R.drawable.ic_duaa_light_hands)
)

@Composable
fun RemembranceCategoriesScreen(viewModel: RemembranceCategoriesViewModel) {
    val dims = MaterialTheme.dimensions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = dims.screenPaddingHorizontal,
            vertical = dims.screenPaddingVertical
        ),
        verticalArrangement = Arrangement.spacedBy(dims.spaceMd)
    ) {
        item {
            CardPair {
                QuickCard(
                    title = stringResource(R.string.morning_remembrances),
                    icon = Icons.Default.WbSunny,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.onRemembranceClick(MORNING_REMEMBRANCE_ID) }
                )

                QuickCard(
                    title = stringResource(R.string.evening_remembrances),
                    icon = Icons.Default.Bedtime,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.onRemembranceClick(EVENING_REMEMBRANCE_ID) }
                )
            }
        }

        item {
            CardPair {
                CompactCard(
                    title = stringResource(R.string.all_remembrances),
                    icon = Icons.Default.ViewModule,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = viewModel::onAllRemembrancesClick
                )

                CompactCard(
                    title = stringResource(R.string.favorite_remembrances),
                    icon = Icons.Default.Favorite,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = viewModel::onFavoriteRemembrancesClick
                )
            }
        }

        item {
            MySectionHeader(
                title = stringResource(R.string.categories),
                modifier = Modifier.padding(top = dims.spaceSm)
            )
        }

        items(items = categoryItems.chunked(2), key = { row -> row.first().id }) { rowItems ->
            CardPair {
                rowItems.forEach { item ->
                    CategoryCard(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { viewModel.onCategoryClick(categoryId = item.id) }
                    )
                }

                // Keeps a lone last card at half width
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Two equal-width, equal-height cards side by side. */
@Composable
private fun CardPair(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spaceMd),
        content = content
    )
}

@Composable
private fun QuickCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        IconBadge(
            painter = rememberVectorPainter(icon),
            background = MaterialTheme.colorScheme.primary,
            tint = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(Modifier.height(dims.spaceMd))

        Text(text = title, style = MaterialTheme.appTypography.title)
    }
}

@Composable
private fun CompactCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = dims.spaceLg, vertical = dims.spaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dims.iconMd)
            )

            Spacer(Modifier.width(dims.spaceSm))

            Text(
                text = title,
                style = MaterialTheme.appTypography.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryCard(item: CategoryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    MyCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(dims.radiusLg),
        contentPadding = PaddingValues(dims.spaceLg)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBadge(
                painter = item.iconRes?.let { painterResource(it) }
                    ?: rememberVectorPainter(item.imageVector!!),
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(dims.spaceMd))

            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.appTypography.label,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IconBadge(painter: Painter, background: Color, tint: Color) {
    val dims = MaterialTheme.dimensions

    Box(
        modifier = Modifier
            .size(dims.iconXl + dims.spaceMd)
            .background(color = background, shape = RoundedCornerShape(dims.radiusLg)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(dims.iconLg),
            tint = tint
        )
    }
}
