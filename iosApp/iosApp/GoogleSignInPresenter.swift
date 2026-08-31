import Foundation
import GoogleSignIn
import UIKit
import shared

/// Motivo de cancelación acordado con Kotlin (`GoogleSignInBridge.CANCELLED`). Debe coincidir literalmente.
private let cancelledReason = "cancelled"

/// Lanza el flujo nativo de Google Sign-In y devuelve el ID token a Kotlin (ADR-0006).
///
/// El SDK de Google vive únicamente en este lado: el framework compartido no lo enlaza. El `clientID` lo
/// toma el SDK de la clave `GIDClientID` del `Info.plist`, así que aquí no se configura nada.
enum GoogleSignInPresenter {
    /// Instala el lanzador en el puente de Kotlin. Se llama una vez al arrancar la app.
    static func install() {
        GoogleSignInBridge.shared.presenter = { onResult in
            // El callback viene de Kotlin y devuelve `KotlinUnit`, no `Void`: se envuelve para que el
            // resto del código Swift trabaje con closures normales.
            present { idToken, error in
                _ = onResult(idToken, error)
            }
        }
    }

    private static func present(onResult: @escaping (String?, String?) -> Void) {
        guard let root = rootViewController() else {
            onResult(nil, "no-root-view-controller")
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: root) { result, error in
            if let error = error as NSError? {
                let cancelled = error.code == GIDSignInError.canceled.rawValue
                onResult(nil, cancelled ? cancelledReason : "sign-in-error-\(error.code)")
                return
            }
            // Firebase canjea el *ID token* de Google (no el access token) en `accounts:signInWithIdp`.
            guard let idToken = result?.user.idToken?.tokenString else {
                onResult(nil, "missing-id-token")
                return
            }
            onResult(idToken, nil)
        }
    }

    /// El selector de Google necesita un controlador desde el que presentarse; la UI es Compose dentro de
    /// un único `UIViewController`, así que se toma el raíz de la ventana activa.
    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
    }
}
