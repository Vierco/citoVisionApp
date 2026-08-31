import Foundation
import OnnxRuntimeBindings
import shared

/// Ejecuta el modelo ONNX de citoVision en iOS (SPEC-0006, ADR-0007).
///
/// ONNX Runtime vive únicamente en este lado: el framework compartido no lo enlaza, igual que ocurre con
/// Google Sign-In (ADR-0006). Kotlin aporta la ruta del `.onnx` y el tensor NCHW ya preprocesado; aquí solo
/// se ejecuta y se devuelve la rama de detección en crudo. El decodificado YOLO, el NMS y la priorización
/// siguen en `commonMain`, compartidos con Android y Desktop.
enum OnnxRuntimeBridge {
    /// La sesión se crea una sola vez y se reutiliza entre análisis (SPEC-0006 RF-8).
    private static var env: ORTEnv?
    private static var session: ORTSession?
    private static var loadedModelPath: String?

    /// Kotlin invoca el puente desde un dispatcher de fondo; el cerrojo serializa creación y ejecución.
    private static let lock = NSLock()

    /// NCHW 1×3×640×640, según la metadata del modelo (SPEC-0006 RN-2).
    private static let inputShape: [NSNumber] = [1, 3, 640, 640]

    /// Instala el ejecutor en el puente de Kotlin. Se llama una vez al arrancar la app.
    static func install() {
        OnnxBridge.shared.runner = { modelPath, input in
            run(modelPath: modelPath, input: input)
        }
    }

    /// `input` se declara en Kotlin como `NSData`, pero Swift lo recibe ya puenteado a `Data`.
    private static func run(modelPath: String, input: Data) -> OnnxNativeResult {
        lock.lock()
        defer { lock.unlock() }
        do {
            let session = try cachedSession(forModelAt: modelPath)
            guard let inputName = try session.inputNames().first else {
                return failure("el modelo no declara ninguna entrada")
            }
            // ORTValue NO copia el búfer: `inputData` debe seguir vivo durante toda la ejecución.
            let inputData = NSMutableData(data: input)
            let tensor = try ORTValue(tensorData: inputData, elementType: .float, shape: inputShape)
            // El API es asimétrico: `outputNames()` devuelve un array, pero `run` espera un conjunto.
            let outputNames = try session.outputNames()
            let outputs = try session.run(
                withInputs: [inputName: tensor],
                outputNames: Set(outputNames),
                runOptions: nil
            )
            return try detectionBranch(of: outputs)
        } catch {
            return failure("\(error)")
        }
    }

    /// Un YOLO11-seg expone dos salidas: la rama de detección `[1, atributos, anclas]` (rango 3) y los
    /// prototipos de máscara `[1, 32, 160, 160]` (rango 4). SPEC-0006 usa solo la primera.
    ///
    /// Se distingue **por el rango**, no por el nombre ni por la posición: el resultado es un diccionario,
    /// que no tiene orden garantizado.
    private static func detectionBranch(of outputs: [String: ORTValue]) throws -> OnnxNativeResult {
        for value in outputs.values {
            let shape = try value.tensorTypeAndShapeInfo().shape
            guard shape.count == 3 else { continue }
            // `tensorData()` devuelve el búfer interno del ORTValue, que muere con él: se copia byte a byte
            // antes de entregárselo a Kotlin, que lo lee cuando esta función ya ha retornado.
            let raw = try value.tensorData()
            let data = Data(bytes: raw.bytes, count: raw.length)
            return OnnxNativeResult(output: data, attributes: Int32(truncating: shape[1]), error: nil)
        }
        return failure("el modelo no devolvió ninguna rama de detección de rango 3")
    }

    private static func cachedSession(forModelAt path: String) throws -> ORTSession {
        if let session, loadedModelPath == path {
            return session
        }
        let environment: ORTEnv
        if let env {
            environment = env
        } else {
            environment = try ORTEnv(loggingLevel: ORTLoggingLevel.warning)
            env = environment
        }
        // Solo CPU. El execution provider de CoreML es optimización futura (ADR-0007) y se activaría aquí,
        // sobre unas ORTSessionOptions, sin tocar Kotlin.
        let created = try ORTSession(env: environment, modelPath: path, sessionOptions: nil)
        session = created
        loadedModelPath = path
        return created
    }

    private static func failure(_ reason: String) -> OnnxNativeResult {
        OnnxNativeResult(output: nil, attributes: 0, error: reason)
    }
}
