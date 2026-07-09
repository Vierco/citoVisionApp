package dev.lovelace.citovision.infrastructure.platform

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Mantiene una referencia débil a la Activity visible actual.
 *
 * Credential Manager exige un Context de Activity para renderizar el selector de cuentas, pero el
 * flujo de Google se dispara desde el ViewModel (sin acceso a la UI). La Application alimenta este
 * proveedor mediante `ActivityLifecycleCallbacks`. La referencia débil evita fugas de memoria.
 */
class ActivityProvider {
    private var reference: WeakReference<Activity> = WeakReference(null)

    val current: Activity?
        get() = reference.get()

    fun update(activity: Activity?) {
        reference = WeakReference(activity)
    }
}
