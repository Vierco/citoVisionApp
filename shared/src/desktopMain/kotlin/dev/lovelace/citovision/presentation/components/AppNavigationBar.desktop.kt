package dev.lovelace.citovision.presentation.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Desktop conserva el `NavigationBar` de Material 3 (ADR-0008); no hay barra de sistema que imitar. */
@Composable
actual fun AppNavigationBar(
    items: List<AppNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    MaterialAppNavigationBar(items = items, selectedIndex = selectedIndex, onSelect = onSelect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun appScaffoldContentInsets(): WindowInsets = ScaffoldDefaults.contentWindowInsets

/** La barra ocupa su hueco en el `Scaffold`, así que no hay nada flotante que compensar. */
@Composable
actual fun floatingNavigationBarPadding(): Dp = 0.dp
