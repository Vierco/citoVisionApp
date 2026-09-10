package dev.lovelace.citovision.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Valor y callback de un campo de texto cuyo contenido **lo gobierna el ViewModel y puede volver
 * transformado**. Lo devuelve [rememberSanitizedField].
 */
class SanitizedFieldState(
    val value: TextFieldValue,
    val onValueChange: (TextFieldValue) -> Unit,
)

/**
 * Mantiene la posición del cursor cuando el texto da la vuelta por el ViewModel y **vuelve distinto**.
 *
 * La sobrecarga de `TextField` que recibe un `String` guarda la selección por su cuenta, y al recibir un
 * texto que no coincide con el que ella tenía la descoloca. Es justo lo que pasa aquí: se escribe un
 * carácter, el saneado lo quita o lo normaliza, y el valor que vuelve no es el que se tecleó, así que el
 * cursor salta —se vio en iOS al escribir el guion, que llegaba como un guion Unicode distinto—.
 *
 * La solución es no delegar la selección: se recuerda aquí y el `TextFieldValue` se construye en cada
 * composición a partir del texto del ViewModel, que es el que manda, con la selección **acotada** a su
 * longitud. Así:
 * - si el saneado no cambió nada, la selección del usuario se respeta tal cual, incluso editando en medio;
 * - si quitó un carácter, el cursor queda justo donde estaba antes de teclearlo, no al final ni al principio;
 * - si lo normalizó por otro, el cursor avanza con normalidad.
 */
@Composable
fun rememberSanitizedField(
    text: String,
    onTextChange: (String) -> Unit,
): SanitizedFieldState {
    var selection by remember { mutableStateOf(TextRange(text.length)) }
    val clamped =
        TextRange(
            selection.start.coerceIn(0, text.length),
            selection.end.coerceIn(0, text.length),
        )
    return SanitizedFieldState(
        value = TextFieldValue(text = text, selection = clamped),
        onValueChange = { new ->
            selection = new.selection
            onTextChange(new.text)
        },
    )
}
