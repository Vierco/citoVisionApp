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

    /**
     * Códigos de paciente con análisis del usuario, **sin duplicados y en orden ascendente**, para el
     * listado seleccionable de la pestaña Pacientes (RF-4b). Acotado al `ownerUid` como el resto (RN-3).
     */
    suspend fun queryPatientCodes(ownerUid: String): Result<List<String>, RemoteAnalysisError>

    /** Borra el documento remoto del análisis. No afecta a la BD local (RF-9). */
    suspend fun deleteAnalysis(analysisId: String): Result<Unit, RemoteAnalysisError>
}
