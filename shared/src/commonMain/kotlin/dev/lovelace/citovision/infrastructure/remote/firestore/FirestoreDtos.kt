package dev.lovelace.citovision.infrastructure.remote.firestore

import kotlinx.serialization.Serializable

/**
 * DTOs de la API REST de Cloud Firestore (formato "typed values"). Solo existen en Infrastructure
 * (RULES.md); no cruzan a Application/Domain. Los enteros viajan como String (`integerValue`).
 *
 * Los campos nulos se omiten al serializar (el cliente usa `explicitNulls = false`), de modo que cada
 * `FirestoreValue` lleva una sola clave de tipo, como exige Firestore.
 */

@Serializable
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val arrayValue: FirestoreArrayValue? = null,
    val mapValue: FirestoreMapValue? = null,
)

@Serializable
data class FirestoreArrayValue(
    val values: List<FirestoreValue> = emptyList(),
)

@Serializable
data class FirestoreMapValue(
    val fields: Map<String, FirestoreValue> = emptyMap(),
)

/** Documento Firestore. `name` lo rellena el servidor al leer; al escribir se omite (va en la URL). */
@Serializable
data class FirestoreDocument(
    val name: String? = null,
    val fields: Map<String, FirestoreValue> = emptyMap(),
)

// --- Consulta estructurada (:runQuery) ---

@Serializable
data class RunQueryRequest(
    val structuredQuery: StructuredQuery,
)

@Serializable
data class StructuredQuery(
    val from: List<CollectionSelector>,
    val where: Filter,
    /** Proyección opcional: limita los campos que devuelve el servidor. Se omite si es null. */
    val select: Projection? = null,
)

@Serializable
data class CollectionSelector(
    val collectionId: String,
)

/** Filtro de la consulta: o bien uno compuesto (AND de varios) o bien uno simple. Nunca los dos. */
@Serializable
data class Filter(
    val compositeFilter: CompositeFilter? = null,
    val fieldFilter: FieldFilter? = null,
)

@Serializable
data class Projection(
    val fields: List<FieldReference>,
)

@Serializable
data class CompositeFilter(
    val op: String,
    val filters: List<FieldFilterWrapper>,
)

@Serializable
data class FieldFilterWrapper(
    val fieldFilter: FieldFilter,
)

@Serializable
data class FieldFilter(
    val field: FieldReference,
    val op: String,
    val value: FirestoreValue,
)

@Serializable
data class FieldReference(
    val fieldPath: String,
)

/** Cada elemento de la respuesta de :runQuery; sin `document` cuando no hay resultados. */
@Serializable
data class RunQueryResponseItem(
    val document: FirestoreDocument? = null,
)
