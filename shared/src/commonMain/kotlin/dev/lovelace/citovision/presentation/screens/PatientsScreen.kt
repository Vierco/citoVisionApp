package dev.lovelace.citovision.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.common_back
import citovision.shared.generated.resources.common_close
import citovision.shared.generated.resources.login_error_password_chars
import citovision.shared.generated.resources.login_error_password_desc
import citovision.shared.generated.resources.patients_button_search
import citovision.shared.generated.resources.patients_close_file
import citovision.shared.generated.resources.patients_error_empty_desc
import citovision.shared.generated.resources.patients_error_empty_title
import citovision.shared.generated.resources.patients_id_name_label
import citovision.shared.generated.resources.patients_no_results_desc
import citovision.shared.generated.resources.patients_no_results_title
import citovision.shared.generated.resources.patients_search_desc_default
import citovision.shared.generated.resources.patients_search_placeholder
import citovision.shared.generated.resources.patients_search_title
import dev.lovelace.citovision.presentation.components.AnalysisCard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private data class PatientAnalysisItem(
    val title: String,
    val date: String,
    val patient: String,
    val description: String,
)

@Composable
fun PatientsScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var noResultsFound by remember { mutableStateOf(false) }
    var showEmptyQueryError by remember { mutableStateOf(false) }
    var showAlphanumericError by remember { mutableStateOf(false) }

    val alphanumericRegex = "^[A-Za-z0-9]*$".toRegex()

    val mockResults =
        listOf(
            PatientAnalysisItem(
                title = "Análisis de Sangre - Muestra A",
                date = "01/11/2023",
                patient = searchQuery,
                description = "Detección de glóbulos blancos completada.",
            ),
            PatientAnalysisItem(
                title = "Análisis de Sangre - Muestra B",
                date = "02/11/2023",
                patient = searchQuery,
                description = "Presencia de anomalías en el conteo de plaquetas.",
            ),
        )

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2000)
            isLoading = false
            if (searchQuery == "00000") {
                noResultsFound = true
            } else {
                showResults = true
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
    ) {
        // Título y Subtítulo/Nombre Paciente
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.patients_search_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF282828),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (showResults) searchQuery else stringResource(Res.string.patients_search_desc_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6F6F6F),
                    modifier = Modifier.weight(1f),
                )

                if (showResults) {
                    TextButton(onClick = {
                        showResults = false
                        isLoading = false
                    }) {
                        Text(
                            text = stringResource(Res.string.patients_close_file),
                            color = Color(0xFF2FA7F0),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2FA7F0))
            }
        } else if (noResultsFound) {
            // Vista de Sin Resultados
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .drawBehind {
                            val stroke =
                                Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                                )
                            drawRoundRect(
                                color = Color.LightGray,
                                style = stroke,
                                cornerRadius =
                                    androidx.compose.ui.geometry
                                        .CornerRadius(28.dp.toPx()),
                            )
                        }.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier =
                            Modifier
                                .size(100.dp)
                                .background(Color(0xFFF3F4F6), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF6B7280),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.patients_no_results_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF282828),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.patients_no_results_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6F6F6F),
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = {
                            noResultsFound = false
                            showResults = false
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(Res.string.common_back), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        } else if (showResults) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement =
                    androidx.compose.foundation.layout.Arrangement
                        .spacedBy(16.dp),
            ) {
                items(mockResults) { item ->
                    AnalysisCard(
                        title = item.title,
                        date = item.date,
                        patient = item.patient,
                        description = item.description,
                        imagePath = null,
                        onClick = { /* TODO */ },
                    )
                }
            }
        } else {
            // Card de Búsqueda
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.85f),
                    ),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(Res.string.patients_id_name_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF282828),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            if (alphanumericRegex.matches(it)) {
                                searchQuery = it
                            } else {
                                showAlphanumericError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.patients_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isLoading = true
                            } else {
                                showEmptyQueryError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2FA7F0)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.patients_button_search),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEmptyQueryError) {
        AlertDialog(
            onDismissRequest = { showEmptyQueryError = false },
            confirmButton = {
                OutlinedButton(onClick = { showEmptyQueryError = false }) {
                    Text(stringResource(Res.string.common_close), color = Color(0xFF2FA7F0))
                }
            },
            title = {
                Text(
                    stringResource(Res.string.patients_error_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF282828),
                )
            },
            text = {
                Text(
                    stringResource(Res.string.patients_error_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
        )
    }

    if (showAlphanumericError) {
        AlertDialog(
            onDismissRequest = { showAlphanumericError = false },
            confirmButton = {
                OutlinedButton(onClick = { showAlphanumericError = false }) {
                    Text(stringResource(Res.string.common_close), color = Color(0xFF2FA7F0))
                }
            },
            title = {
                Text(
                    stringResource(Res.string.login_error_password_chars),
                    color = Color(0xFF282828),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    stringResource(Res.string.login_error_password_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
        )
    }
}
