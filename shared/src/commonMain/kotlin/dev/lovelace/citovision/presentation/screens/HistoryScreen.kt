package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.history_empty_desc
import citovision.shared.generated.resources.history_empty_title
import dev.lovelace.citovision.presentation.components.AnalysisCard
import dev.lovelace.citovision.presentation.components.AnalysisDetailDialog
import org.jetbrains.compose.resources.stringResource

private data class AnalysisItem(
    val title: String,
    val date: String,
    val patient: String,
    val description: String,
    val cellCount: String,
)

@Composable
fun HistoryScreen() {
    var hasData by remember { mutableStateOf(false) }
    var selectedAnalysis by remember { mutableStateOf<AnalysisItem?>(null) }

    val tempData =
        listOf(
            AnalysisItem(
                title = "Análisis de Sangre - Muestra A",
                date = "01/11/2023",
                patient = "PAC-2023-001",
                description = "Detección de glóbulos blancos completada satisfactoriamente.",
                cellCount = "Leucocitos: 7.500/µL, Neutrófilos: 60%, Linfocitos: 30%",
            ),
            AnalysisItem(
                title = "Análisis de Sangre - Muestra B",
                date = "02/11/2023",
                patient = "PAC-2023-002",
                description = "Presencia de anomalías en el conteo de plaquetas detectada.",
                cellCount = "Plaquetas: 120.000/µL (Bajo), Hematíes: 4.8M/µL",
            ),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasData) {
            // Estado Vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 64.dp)
                            .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFFD1D5DB),
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(Res.string.history_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6F6F6F),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.history_empty_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF6F6F6F).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // Estado con Datos (Lista Temporal)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(tempData) { item ->
                    AnalysisCard(
                        title = item.title,
                        date = item.date,
                        patient = item.patient,
                        description = item.description,
                        image = ColorPainter(if (item.patient.endsWith("1")) Color.LightGray else Color.Gray),
                        onClick = { selectedAnalysis = item },
                    )
                }
            }
        }

        // Dialog de Detalle
        selectedAnalysis?.let { analysis ->
            AnalysisDetailDialog(
                title = analysis.title,
                patient = analysis.patient,
                date = analysis.date,
                cellCount = analysis.cellCount,
                onDismissRequest = { selectedAnalysis = null },
            )
        }

        // Interruptor temporal abajo a la derecha
        Switch(
            checked = hasData,
            onCheckedChange = { hasData = it },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        )
    }
}
