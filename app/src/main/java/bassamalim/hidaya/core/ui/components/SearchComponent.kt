package bassamalim.hidaya.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions

/**
 * A search field for filtering the list below it. It brings its own spacing (the screen gutter
 * at the sides), so callers only size it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.search),
    onSearch: (String) -> Unit = {}
) {
    val dims = MaterialTheme.dimensions

    SearchBar(
        inputField = {
            // Material's text fields default to the system font; the app's text is Tajawal
            ProvideTextStyle(MaterialTheme.appTypography.body) {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { SearchBarPlaceholder(hint) },
                    leadingIcon = { SearchBarLeadingIcon() },
                    trailingIcon = {
                        if (query.isNotEmpty()) SearchBarClearButton { onQueryChange("") }
                    }
                )
            }
        },
        expanded = false,
        onExpandedChange = {},
        modifier = modifier.padding(horizontal = dims.spaceLg, vertical = dims.spaceSm),
        shape = searchBarShape(),
        windowInsets = WindowInsets(0, 0, 0, 0),
        content = {}
    )
}

/* The parts below are shared with search bars that expand into results (the Quran one). */

@Composable
fun searchBarShape(): Shape = RoundedCornerShape(MaterialTheme.dimensions.radiusLg)

@Composable
fun SearchBarPlaceholder(hint: String) {
    Text(
        text = hint,
        style = MaterialTheme.appTypography.body,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SearchBarLeadingIcon() {
    Icon(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.search)
    )
}

@Composable
fun SearchBarClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.close)
        )
    }
}
