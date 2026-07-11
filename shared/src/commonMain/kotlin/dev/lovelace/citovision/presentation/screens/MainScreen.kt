package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.microsc
import citovision.shared.generated.resources.nav_analysis
import citovision.shared.generated.resources.nav_history
import citovision.shared.generated.resources.nav_patients
import citovision.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class MainTab(
    val labelRes: StringResource,
) {
    ANALYSIS(Res.string.nav_analysis),
    HISTORY(Res.string.nav_history),
    PATIENTS(Res.string.nav_patients),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToSettings: () -> Unit) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = MainTab.entries[selectedTabIndex]

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo blanco base
                    drawRect(Color.White)

                    // Efecto de resplandor azul y morado vertical (mezclados en el centro)
                    val primaryColor = Color(0xFF2FA7F0)
                    val tertiaryColor = Color(0xFFA56AE3)

                    scale(scaleX = 2.2f, scaleY = 2.5f, pivot = center) {
                        // Resplandor Azul (Posicionado más arriba)
                        val blueCenter = Offset(center.x, center.y - size.height * 0.15f)
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.25f),
                                            Color.Transparent,
                                        ),
                                    center = blueCenter,
                                    radius = size.width * 0.45f,
                                ),
                            radius = size.width * 0.45f,
                            center = blueCenter,
                        )
                        // Resplandor Morado (Tertiary) (Posicionado más abajo)
                        val purpleCenter = Offset(center.x, center.y + size.height * 0.15f)
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            tertiaryColor.copy(alpha = 0.20f),
                                            Color.Transparent,
                                        ),
                                    center = purpleCenter,
                                    radius = size.width * 0.4f,
                                ),
                            radius = size.width * 0.4f,
                            center = purpleCenter,
                        )
                    }
                },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(selectedTab.labelRes)) },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(Res.string.nav_settings),
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.Transparent,
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTabIndex == tab.ordinal,
                            onClick = { selectedTabIndex = tab.ordinal },
                            icon = {
                                val painter =
                                    when (tab) {
                                        MainTab.ANALYSIS -> painterResource(Res.drawable.microsc)
                                        MainTab.HISTORY -> rememberVectorPainter(Icons.Default.List)
                                        MainTab.PATIENTS -> rememberVectorPainter(Icons.Default.Person)
                                    }
                                Icon(
                                    painter = painter,
                                    contentDescription = stringResource(tab.labelRes),
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    indicatorColor = Color(0xFF2FD38A), // Secondary
                                ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                when (selectedTab) {
                    MainTab.ANALYSIS -> AnalysisScreen()
                    MainTab.HISTORY -> HistoryScreen()
                    MainTab.PATIENTS -> PatientsScreen()
                }
            }
        }
    }
}
