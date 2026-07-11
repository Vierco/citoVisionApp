package dev.lovelace.citovision.infrastructure.remote

/**
 * Configuración de acceso remoto (SPEC-0005).
 *
 * La **Web API key** de Firebase se aporta como propiedad de Koin desde el entry point de cada plataforma
 * (Desktop y Android leen `firebaseWebApiKey` de `local.properties`, no versionado). Es la key sin
 * restricción de aplicación; la del `google-services.json` está restringida a la app Android y las
 * llamadas REST con Ktor la rechazarían.
 *
 * [FIREBASE_PROJECT_ID] y [FIREBASE_STORAGE_BUCKET] son **identificadores públicos** del proyecto (constan
 * en el `google-services.json` ya versionado), no secretos; se dejan como constantes. Si en el futuro
 * hubiera varios entornos, moverlos a build-config.
 */
const val FIREBASE_WEB_API_KEY_PROPERTY = "firebase_web_api_key"

const val FIREBASE_PROJECT_ID = "citovision-cf661"

const val FIREBASE_STORAGE_BUCKET = "citovision-cf661.firebasestorage.app"
