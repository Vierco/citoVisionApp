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
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose gestiona sus propios insets/teclado.
    }
}

