package dev.lovelace.citovision.infrastructure.inference

import dev.lovelace.citovision.application.ports.ImageDecoder
import dev.lovelace.citovision.domain.inference.RgbImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

/**
 * Decodifica imágenes en iOS con UIKit + CoreGraphics (SPEC-0006, ADR-0007). No necesita puente a Swift ni
 * dependencias nuevas: `platform.UIKit` y `platform.CoreGraphics` ya vienen con Kotlin/Native.
 *
 * El dibujo se hace **directamente sobre el `IntArray` de [RgbImage]** (`usePinned`), con el formato
 * `kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Little`: esa combinación deja en memoria los bytes
 * `B,G,R,A`, que leídos como entero *little-endian* son exactamente el `0xAARRGGBB` que espera
 * `YoloPreprocessor`. Así no hace falta ningún bucle de conversión sobre millones de píxeles.
 *
 * Como Android (`BitmapFactory`) y Desktop (`ImageIO`), **ignora la orientación EXIF**: se decodifica el
 * búfer tal cual, de forma que las tres plataformas alimentan el modelo con los mismos píxeles.
 */
@OptIn(ExperimentalForeignApi::class)
class ImageDecoderImpl : ImageDecoder {
    override fun decode(bytes: ByteArray): RgbImage? {
        if (bytes.isEmpty()) return null
        val data =
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
        val cgImage = UIImage.imageWithData(data)?.CGImage ?: return null
        val width = CGImageGetWidth(cgImage).toInt()
        val height = CGImageGetHeight(cgImage).toInt()
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        try {
            val drawn =
                pixels.usePinned { pinned ->
                    val context =
                        CGBitmapContextCreate(
                            data = pinned.addressOf(0),
                            width = width.toULong(),
                            height = height.toULong(),
                            bitsPerComponent = BITS_PER_COMPONENT.toULong(),
                            bytesPerRow = (width * BYTES_PER_PIXEL).toULong(),
                            space = colorSpace,
                            bitmapInfo = PIXEL_FORMAT,
                        ) ?: return@usePinned false
                    val frame = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
                    CGContextDrawImage(context, frame, cgImage)
                    CGContextRelease(context)
                    true
                }
            if (!drawn) return null
        } finally {
            CGColorSpaceRelease(colorSpace)
        }
        return RgbImage(width = width, height = height, pixels = pixels)
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
        const val BITS_PER_COMPONENT = 8

        /** ARGB empaquetado en 32 bits; el alfa se descarta al dibujar y el preprocesador lo ignora. */
        val PIXEL_FORMAT: UInt = CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst.value or kCGBitmapByteOrder32Little
    }
}
