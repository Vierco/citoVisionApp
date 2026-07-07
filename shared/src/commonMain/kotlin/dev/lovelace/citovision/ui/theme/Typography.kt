package dev.lovelace.citovision.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import citovision.shared.generated.resources.Res
import citovision.shared.generated.resources.Dongle_Bold
import citovision.shared.generated.resources.Dongle_Light
import citovision.shared.generated.resources.Dongle_Regular
import org.jetbrains.compose.resources.Font

@Composable
fun getTypography(): Typography {
    val dongleFontFamily = FontFamily(
        Font(Res.font.Dongle_Regular, FontWeight.Normal),
        Font(Res.font.Dongle_Bold, FontWeight.Bold),
        Font(Res.font.Dongle_Light, FontWeight.Light)
    )

    return Typography(
        displayLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 72.sp,
            lineHeight = 64.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 56.sp,
            lineHeight = 48.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 48.sp,
            lineHeight = 40.sp
        ),
        titleLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 32.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 28.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 24.sp
        ),
        labelLarge = TextStyle(
            fontFamily = dongleFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 24.sp
        )
    )
}
