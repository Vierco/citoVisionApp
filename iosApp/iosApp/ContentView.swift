import SwiftUI
import UIKit
import shared

/// Puente UIKit → Compose: expone el `UIViewController` que crea la UI Compose compartida
/// (`MainViewController()` en `shared`) como una vista SwiftUI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ZStack(alignment: .bottom) {
            composeContent
            // La barra nativa va POR ENCIMA de Compose a propósito (ADR-0008): así tiene contenido real
            // debajo que desenfocar. Dentro de Compose, con UIKitView, quedaría detrás de su superficie.
            //
            // Ignora la región segura del teclado porque SwiftUI, por defecto, empuja sus vistas por
            // encima de él: la barra se despegaba del borde inferior y tapaba el campo de texto. Lo
            // nativo es que se quede abajo y el teclado la cubra.
            NativeTabBar()
                .ignoresSafeArea(.keyboard, edges: .bottom)
        }
    }

    private var composeContent: some View {
        ComposeView()
            // Se ignoran TODAS las regiones seguras (`.container` + `.keyboard`), no solo el teclado.
            // SwiftUI aplica por defecto los insets del dispositivo a un UIViewControllerRepresentable,
            // así que Compose recibía un lienzo recortado y el fondo blanco de la ventana asomaba como dos
            // bandas: bajo la Dynamic Island y sobre el indicador de inicio.
            //
            // Con la pantalla completa, quien aplica los insets es Compose, igual que en Android con
            // `enableEdgeToEdge()`: el degradado de `MainScreen` cubre todo y son sus barras superior e
            // inferior las que se separan solas de las del sistema.
            .ignoresSafeArea()
    }
}

