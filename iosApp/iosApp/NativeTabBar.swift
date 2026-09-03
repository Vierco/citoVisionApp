import SwiftUI
import shared

/// Estado de la barra, alimentado desde Compose por `NativeTabBarBridge` (ADR-0008).
///
/// Compose es la fuente única de verdad: aquí no se decide nada, solo se refleja lo que llega y se avisa
/// de los toques.
final class TabBarModel: ObservableObject {
    static let shared = TabBarModel()

    @Published var isVisible = false
    @Published var labels: [String] = []
    @Published var selectedIndex = 0

    /// Se instala una vez al arrancar la app.
    func install() {
        NativeTabBarBridge.shared.onStateChanged = { [weak self] state in
            // El puente puede llegar desde un hilo de fondo; la UI se toca siempre en el principal.
            DispatchQueue.main.async {
                self?.isVisible = state.visible
                self?.labels = state.labels
                self?.selectedIndex = Int(state.selectedIndex)
            }
        }
    }

    func select(_ index: Int) {
        NativeTabBarBridge.shared.selectTab(index: Int32(index))
    }
}

/// Barra de pestañas flotante con aspecto nativo de iOS (ADR-0008).
///
/// Va **por encima** de la vista de Compose en el `ZStack` de `ContentView`, y esa posición es justo lo
/// que hace posible el efecto: debajo hay una `UIView` real con el contenido de la app, así que
/// `.glassEffect()` tiene algo que desenfocar. Dentro de Compose, con `UIKitView`, quedaría por debajo de
/// su superficie y el cristal no reflejaría nada.
struct NativeTabBar: View {
    @ObservedObject var model = TabBarModel.shared

    /// SF Symbols por pestaña, en el mismo orden que las etiquetas que envía Compose.
    /// Verificado contra el catálogo del sistema: **no existe ningún símbolo `microscope`**; `microbe`
    /// es el más cercano y encaja con el análisis celular.
    private let symbols = ["microbe", "list.bullet", "person.2"]

    /// Debe cuadrar con `BAR_HEIGHT` de `AppNavigationBar.ios.kt`, que reserva el hueco en Compose.
    private let barHeight: CGFloat = 64

    var body: some View {
        if model.isVisible {
            HStack(spacing: 4) {
                ForEach(Array(model.labels.enumerated()), id: \.offset) { index, label in
                    tabItem(index: index, label: label)
                }
            }
            .padding(.horizontal, 8)
            .frame(height: barHeight)
            .glassBackground()
            .padding(.horizontal, 24)
            .transition(.opacity)
        }
    }

    private func tabItem(index: Int, label: String) -> some View {
        let isSelected = index == model.selectedIndex
        return Button {
            model.select(index)
        } label: {
            VStack(spacing: 2) {
                Image(systemName: symbols[safe: index] ?? "circle")
                    .font(.system(size: 20, weight: .regular))
                Text(label)
                    .font(.caption2)
                    .lineLimit(1)
            }
            .foregroundStyle(isSelected ? Color.accentColor : Color.secondary)
            .frame(maxWidth: .infinity)
            // Mínimo táctil accesible (DESIGN.md §Touch Targets, AGENTS.md §15).
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

private extension View {
    /// Liquid Glass en iOS 26; por debajo, material translúcido sobre la misma cápsula. El
    /// *deployment target* es 18.6, así que la rama antigua es alcanzable.
    @ViewBuilder
    func glassBackground() -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular, in: .capsule)
        } else {
            self.background(.ultraThinMaterial, in: Capsule())
        }
    }
}

private extension Array {
    /// Acceso tolerante: si Compose enviara más etiquetas que símbolos, se degrada en vez de reventar.
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
