package dev.lovelace.citovision

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.lovelace.citovision.composition.di.initKoin
import dev.lovelace.citovision.infrastructure.auth.GOOGLE_WEB_CLIENT_ID_PROPERTY
import dev.lovelace.citovision.infrastructure.platform.ActivityProvider
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

/**
 * Entry point de proceso en Android. Arranca Koin una única vez (RULES.md), registra el Context de
 * Android, aporta el Web client ID (server) de Google —generado por el plugin google-services en
 * [R.string.default_web_client_id]— y alimenta el [ActivityProvider] con la Activity visible para
 * que Credential Manager pueda renderizar el selector de cuentas (ver SPEC-0001, flujo Google).
 */
class CitoVisionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CitoVisionApplication)
            properties(
                mapOf(GOOGLE_WEB_CLIENT_ID_PROPERTY to getString(R.string.default_web_client_id)),
            )
        }
        trackVisibleActivity()
    }

    private fun trackVisibleActivity() {
        val activityProvider: ActivityProvider = get()
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    activityProvider.update(activity)
                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (activityProvider.current === activity) {
                        activityProvider.update(null)
                    }
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit
            },
        )
    }
}
