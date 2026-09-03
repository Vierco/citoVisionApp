import SwiftUI
import GoogleSignIn
import shared

@main
struct iOSApp: App {
    init() {
        // Arranque único de Koin + Napier en el lado Kotlin (equivalente a Application/Main).
        MainViewControllerKt.bootstrap()
        // Google Sign-In vive en Swift; Kotlin solo recibe el ID token por el puente (ADR-0006).
        GoogleSignInPresenter.install()
        // ONNX Runtime también vive en Swift; Kotlin le pasa el tensor por el puente (ADR-0007).
        OnnxRuntimeBridge.install()
        // La barra de pestañas la dibuja SwiftUI encima de Compose, que le publica su estado (ADR-0008).
        TabBarModel.shared.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Retorno del flujo de Google: la app se reabre por su esquema de URL.
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
