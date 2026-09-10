package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.microsc
import citovision.shared.generated.resources.nav_analysis
import citovision.shared.generated.resources.nav_history
import citovision.shared.generated.resources.nav_patients
import citovision.shared.generated.resources.nav_settings
import dev.lovelace.citovision.presentation.components.AppNavigationBar
import dev.lovelace.citovision.presentation.components.AppNavigationItem
import dev.lovelace.citovision.presentation.components.appScaffoldContentInsets
import dev.lovelace.citovision.presentation.components.swipeBetweenTabsEnabled
import kotlinx.coroutines.launch
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
    // Un pager en lugar de un simple índice: en Android y Desktop permite además cambiar de pestaña
    // deslizando. `rememberPagerState` ya conserva la página al recrear el proceso.
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val scope = rememberCoroutineScope()
    val swipeEnabled = swipeBetweenTabsEnabled()
    val selectedTab = MainTab.entries[pagerState.currentPage]

    // Al tocar una pestaña se anima el paso de página donde el gesto existe; donde no (iOS), se salta en
    // seco, para que el cambio se vea igual que antes de haber pager, como corresponde a su barra nativa.
    fun goToTab(index: Int) {
        scope.launch {
            if (swipeEnabled) pagerState.animateScrollToPage(index) else pagerState.scrollToPage(index)
        }
    }

    // Al guardar una muestra con éxito se cambia a la pestaña Historial y se guarda el id de la nueva card
    // para que el Historial espere a que esté en la lista, haga scroll a ella y la destaque una vez. El
    // Historial lo consume (a null) al terminar.
    var pendingFlashAnalysisId by rememberSaveable { mutableStateOf<String?>(null) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawBehind {
                    // Fondo base del tema
                    drawRect(backgroundColor)

                    // Efecto de resplandor azul y morado vertical (mezclados en el centro)
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
            // En iOS estos insets excluyen el borde inferior, para que el contenido llegue hasta abajo y
            // pase por debajo de la barra flotante (ADR-0008). En Android y Desktop son los de siempre.
            contentWindowInsets = appScaffoldContentInsets(),
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
                // Cada plataforma dibuja la suya (ADR-0008): Material 3 en Android y Desktop, y en iOS una
                // barra flotante nativa que pinta SwiftUI por encima. El estado se queda aquí.
                AppNavigationBar(
                    items =
                        MainTab.entries.map { tab ->
                            AppNavigationItem(
                                label = stringResource(tab.labelRes),
                                icon =
                                    when (tab) {
                                        MainTab.ANALYSIS -> painterResource(Res.drawable.microsc)
                                        MainTab.HISTORY -> rememberVectorPainter(Icons.Default.List)
                                        MainTab.PATIENTS -> rememberVectorPainter(Icons.Default.Person)
                                    },
                            )
                        },
                    selectedIndex = pagerState.currentPage,
                    onSelect = { goToTab(it) },
                )
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                userScrollEnabled = swipeEnabled,
            ) { page ->
                when (MainTab.entries[page]) {
                    MainTab.ANALYSIS ->
                        AnalysisScreen(
                            onAnalysisSaved = { analysisId ->
                                goToTab(MainTab.HISTORY.ordinal)
                                pendingFlashAnalysisId = analysisId
                            },
                        )

                    MainTab.HISTORY ->
                        HistoryScreen(
                            flashAnalysisId = pendingFlashAnalysisId,
                            onFlashConsumed = { pendingFlashAnalysisId = null },
                        )

                    // Se le dice si es la pestaña asentada, y no si está compuesta: dentro de un pager las
                    // páginas vecinas se componen mientras se arrastra, y no queremos consultar el remoto
                    // por medio gesto ni por uno que se queda a medias.
                    MainTab.PATIENTS -> PatientsScreen(isCurrentTab = page == pagerState.settledPage)
                }
            }
        }
    }
}
