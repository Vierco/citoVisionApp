package dev.lovelace.citovision.domain.settings

/**
 * Preferencia de tema elegida por el usuario en Ajustes (DESIGN.md "Dark Mode"). `SYSTEM` sigue el modo
 * del sistema; `LIGHT`/`DARK` lo fuerzan. Por defecto `SYSTEM`.
 */
enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
}
