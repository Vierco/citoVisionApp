package dev.lovelace.citovision.application.ports

import dev.lovelace.citovision.core.result.Result
import dev.lovelace.citovision.domain.entities.Analysis
import dev.lovelace.citovision.domain.errors.RemoteAnalysisError

/**
 * Consulta remota de análisis por paciente (SPEC-0005, pestaña Pacientes). Puerto de la capa Application;
 * lo implementa el DataSource de Firestore en Infrastructure. Resultados acotados al `ownerUid` (RN-3).
 */
interface RemotePatientAnalyses {
    suspend fun queryByPatient(
        ownerUid: String,
        patientCode: String,
    ): Result<List<Analysis>, RemoteAnalysisError>

    /** Borra el documento remoto del análisis. No afecta a la BD local (RF-9). */
    suspend fun deleteAnalysis(analysisId: String): Result<Unit, RemoteAnalysisError>
}
