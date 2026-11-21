package com.example.ui.components.illustrations

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawHomeAndTravelVector(key: WordArtworkKey): Boolean {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.CAR -> {
            val cabin = Path().apply {
                moveTo(cx - u * 0.58f, cy - u * 0.02f)
                lineTo(cx - u * 0.34f, cy - u * 0.46f)
                lineTo(cx + u * 0.36f, cy - u * 0.46f)
                lineTo(cx + u * 0.62f, cy - u * 0.02f)
                close()
            }
            drawPath(cabin, color = Color(0xFF4FC3F7))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFC62828)), Offset(cx, cy - u * 0.1f), Offset(cx, cy + u * 0.45f)),
                topLeft = Offset(cx - u * 0.92f, cy - u * 0.06f),
                size = Size(u * 1.84f, u * 0.54f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            drawCircle(Color(0xFF263238), u * 0.24f, Offset(cx - u * 0.52f, cy + u * 0.48f))
            drawCircle(Color(0xFFCFD8DC), u * 0.11f, Offset(cx - u * 0.52f, cy + u * 0.48f))
            drawCircle(Color(0xFF263238), u * 0.24f, Offset(cx + u * 0.52f, cy + u * 0.48f))
            drawCircle(Color(0xFFCFD8DC), u * 0.11f, Offset(cx + u * 0.52f, cy + u * 0.48f))
        }

        WordArtworkKey.BUS, WordArtworkKey.TRAIN -> {
            val isTrain = key == WordArtworkKey.TRAIN
            val bodyLight = if (isTrain) Color(0xFF42A5F5) else Color(0xFFFFCA28)
            val bodyDark = if (isTrain) Color(0xFF1565C0) else Color(0xFFF57C00)
            drawRoundRect(
                brush = Brush.linearGradient(listOf(bodyLight, bodyDark), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.6f)),
                topLeft = Offset(cx - u * 0.72f, cy - u * 0.74f),
                size = Size(u * 1.44f, u * 1.32f),
                cornerRadius = CornerRadius(u * 0.20f, u * 0.20f)
            )
            drawRoundRect(Color(0xFFE1F5FE), Offset(cx - u * 0.54f, cy - u * 0.54f), Size(u * 1.08f, u * 0.56f), CornerRadius(u * 0.10f, u * 0.10f))
            drawCircle(Color(0xFFFFF59D), u * 0.11f, Offset(cx - u * 0.42f, cy + u * 0.30f))
            drawCircle(Color(0xFFFFF59D), u * 0.11f, Offset(cx + u * 0.42f, cy + u * 0.30f))
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.58f, cy + u * 0.54f), Size(u * 0.24f, u * 0.20f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF263238), Offset(cx + u * 0.34f, cy + u * 0.54f), Size(u * 0.24f, u * 0.20f), CornerRadius(u * 0.06f, u * 0.06f))
        }

        WordArtworkKey.BICYCLE -> {
            val wheelStroke = Stroke(width = (u * 0.08f).coerceAtLeast(3f))
            drawCircle(Color(0xFF37474F), u * 0.34f, Offset(cx - u * 0.54f, cy + u * 0.30f), style = wheelStroke)
            drawCircle(Color(0xFF37474F), u * 0.34f, Offset(cx + u * 0.54f, cy + u * 0.30f), style = wheelStroke)
            val frameColor = Color(0xFFE53935)
            val frameWidth = (u * 0.07f).coerceAtLeast(2.5f)
            drawLine(frameColor, Offset(cx - u * 0.54f, cy + u * 0.30f), Offset(cx - u * 0.08f, cy + u * 0.30f), frameWidth, StrokeCap.Round)
            drawLine(frameColor, Offset(cx - u * 0.54f, cy + u * 0.30f), Offset(cx - u * 0.24f, cy - u * 0.22f), frameWidth, StrokeCap.Round)
            drawLine(frameColor, Offset(cx - u * 0.24f, cy - u * 0.22f), Offset(cx + u * 0.34f, cy - u * 0.22f), frameWidth, StrokeCap.Round)
            drawLine(frameColor, Offset(cx - u * 0.08f, cy + u * 0.30f), Offset(cx + u * 0.34f, cy - u * 0.22f), frameWidth, StrokeCap.Round)
            drawLine(frameColor, Offset(cx + u * 0.28f, cy - u * 0.42f), Offset(cx + u * 0.54f, cy + u * 0.30f), frameWidth, StrokeCap.Round)
            drawLine(Color(0xFF263238), Offset(cx - u * 0.36f, cy - u * 0.28f), Offset(cx - u * 0.10f, cy - u * 0.28f), frameWidth * 1.2f, StrokeCap.Round)
        }

        WordArtworkKey.PLANE -> {
            rotate(-24f, Offset(cx, cy)) {
                val wings = Path().apply {
                    moveTo(cx - u * 0.16f, cy - u * 0.16f)
                    lineTo(cx - u * 0.36f, cy - u * 0.92f)
                    lineTo(cx - u * 0.06f, cy - u * 0.92f)
                    lineTo(cx + u * 0.26f, cy - u * 0.14f)
                    close()
                }
                drawPath(wings, color = Color(0xFF1E88E5))
                val lowerWing = Path().apply {
                    moveTo(cx - u * 0.16f, cy + u * 0.16f)
                    lineTo(cx - u * 0.36f, cy + u * 0.92f)
                    lineTo(cx - u * 0.06f, cy + u * 0.92f)
                    lineTo(cx + u * 0.26f, cy + u * 0.14f)
                    close()
                }
                drawPath(lowerWing, color = Color(0xFF1565C0))
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC)), Offset(cx, cy - u * 0.2f), Offset(cx, cy + u * 0.2f)),
                    topLeft = Offset(cx - u * 0.86f, cy - u * 0.18f),
                    size = Size(u * 1.72f, u * 0.36f),
                    cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
                )
            }
        }

        WordArtworkKey.SHIP -> {
            val sail = Path().apply {
                moveTo(cx - u * 0.08f, cy - u * 0.88f)
                lineTo(cx + u * 0.58f, cy + u * 0.12f)
                lineTo(cx - u * 0.08f, cy + u * 0.12f)
                close()
            }
            drawPath(sail, color = Color(0xFFFFFFFF))
            drawLine(Color(0xFF5D4037), Offset(cx - u * 0.10f, cy - u * 0.88f), Offset(cx - u * 0.10f, cy + u * 0.24f), u * 0.07f, StrokeCap.Round)
            val hull = Path().apply {
                moveTo(cx - u * 0.88f, cy + u * 0.22f)
                lineTo(cx + u * 0.88f, cy + u * 0.22f)
                lineTo(cx + u * 0.62f, cy + u * 0.68f)
                lineTo(cx - u * 0.62f, cy + u * 0.68f)
                close()
            }
            drawPath(hull, brush = Brush.linearGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), Offset(cx, cy + u * 0.2f), Offset(cx, cy + u * 0.7f)))
            drawRoundRect(Color(0xFF0288D1), Offset(cx - u * 0.96f, cy + u * 0.64f), Size(u * 1.92f, u * 0.18f), CornerRadius(u * 0.09f, u * 0.09f))
        }

        WordArtworkKey.SUITCASE -> {
            drawRoundRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(cx - u * 0.24f, cy - u * 0.86f),
                size = Size(u * 0.48f, u * 0.36f),
                cornerRadius = CornerRadius(u * 0.10f, u * 0.10f),
                style = Stroke(width = u * 0.09f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF26A69A), Color(0xFF00695C)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.7f)),
                topLeft = Offset(cx - u * 0.76f, cy - u * 0.54f),
                size = Size(u * 1.52f, u * 1.24f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            drawRoundRect(Color(0xFF80CBC4), Offset(cx - u * 0.48f, cy - u * 0.54f), Size(u * 0.14f, u * 1.24f), CornerRadius(0f, 0f))
            drawRoundRect(Color(0xFF80CBC4), Offset(cx + u * 0.34f, cy - u * 0.54f), Size(u * 0.14f, u * 1.24f), CornerRadius(0f, 0f))
        }

        WordArtworkKey.PASSPORT -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF1E40AF), Color(0xFF0F172A)), Offset(cx, cy - u * 0.8f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.56f, cy - u * 0.78f),
                size = Size(u * 1.12f, u * 1.56f),
                cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
            )
            drawRoundRect(Color(0xFF172554), Offset(cx - u * 0.56f, cy - u * 0.78f), Size(u * 0.16f, u * 1.56f), CornerRadius(u * 0.08f, u * 0.08f))
            drawCircle(Color(0xFFFFD54F), u * 0.28f, Offset(cx + u * 0.04f, cy - u * 0.08f), style = Stroke(width = u * 0.055f))
            drawOval(Color(0xFFFFD54F), Offset(cx - u * 0.10f, cy - u * 0.36f), Size(u * 0.28f, u * 0.56f), style = Stroke(width = u * 0.04f))
            drawRoundRect(Color(0xFFFFD54F), Offset(cx - u * 0.26f, cy + u * 0.36f), Size(u * 0.60f, u * 0.10f), CornerRadius(u * 0.05f, u * 0.05f))
        }

        WordArtworkKey.TICKET -> {
            rotate(-12f, Offset(cx, cy)) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFE65100)), Offset(cx - u, cy - u * 0.5f), Offset(cx + u, cy + u * 0.5f)),
                    topLeft = Offset(cx - u * 0.86f, cy - u * 0.46f),
                    size = Size(u * 1.72f, u * 0.92f),
                    cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
                )
                drawRoundRect(Color.White, Offset(cx - u * 0.80f, cy - u * 0.40f), Size(u * 1.60f, u * 0.80f), CornerRadius(u * 0.10f, u * 0.10f))
                drawRoundRect(Color(0xFFFF7043), Offset(cx - u * 0.80f, cy - u * 0.40f), Size(u * 1.60f, u * 0.24f), CornerRadius(u * 0.10f, u * 0.10f))
                // Perforation stub line & barcode stripes
                for (i in 0..3) {
                    drawCircle(Color(0xFFCFD8DC), u * 0.04f, Offset(cx + u * 0.32f, cy - u * 0.08f + i * u * 0.14f))
                }
                drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.66f, cy - u * 0.04f), Size(u * 0.82f, u * 0.10f), CornerRadius(u * 0.04f, u * 0.04f))
                drawRoundRect(Color(0xFF90A4AE), Offset(cx - u * 0.66f, cy + u * 0.14f), Size(u * 0.56f, u * 0.10f), CornerRadius(u * 0.04f, u * 0.04f))
                listOf(0.44f, 0.54f, 0.62f, 0.70f).forEach { bx ->
                    drawRoundRect(Color(0xFF263238), Offset(cx + bx * u, cy - u * 0.06f), Size(u * 0.05f, u * 0.34f), CornerRadius(0f, 0f))
                }
            }
        }

        WordArtworkKey.MAP -> {
            val leftFold = Path().apply {
                moveTo(cx - u * 0.84f, cy - u * 0.56f)
                lineTo(cx - u * 0.28f, cy - u * 0.70f)
                lineTo(cx - u * 0.28f, cy + u * 0.64f)
                lineTo(cx - u * 0.84f, cy + u * 0.76f)
                close()
            }
            val midFold = Path().apply {
                moveTo(cx - u * 0.28f, cy - u * 0.70f)
                lineTo(cx + u * 0.28f, cy - u * 0.54f)
                lineTo(cx + u * 0.28f, cy + u * 0.76f)
                lineTo(cx - u * 0.28f, cy + u * 0.64f)
                close()
            }
            val rightFold = Path().apply {
                moveTo(cx + u * 0.28f, cy - u * 0.54f)
                lineTo(cx + u * 0.84f, cy - u * 0.70f)
                lineTo(cx + u * 0.84f, cy + u * 0.64f)
                lineTo(cx + u * 0.28f, cy + u * 0.76f)
                close()
            }
            drawPath(leftFold, color = Color(0xFF81C784))
            drawPath(midFold, color = Color(0xFF4CAF50))
            drawPath(rightFold, color = Color(0xFFA5D6A7))
            drawLine(Color(0xFFFFD54F), Offset(cx - u * 0.66f, cy + u * 0.28f), Offset(cx + u * 0.62f, cy - u * 0.16f), u * 0.08f, StrokeCap.Round)
            drawGlossySphere(Offset(cx + u * 0.20f, cy - u * 0.24f), u * 0.20f, Color(0xFFFF5252), Color(0xFFC62828))
        }

        WordArtworkKey.TRAFFIC_LIGHT -> {
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.36f, cy - u * 0.88f), Size(u * 0.72f, u * 1.72f), CornerRadius(u * 0.20f, u * 0.20f))
            drawGlossySphere(Offset(cx, cy - u * 0.48f), u * 0.20f, Color(0xFFFF5252), Color(0xFFC62828))
            drawGlossySphere(Offset(cx, cy), u * 0.20f, Color(0xFFFFEE58), Color(0xFFF9A825))
            drawGlossySphere(Offset(cx, cy + u * 0.48f), u * 0.20f, Color(0xFF69F0AE), Color(0xFF00C853))
        }

        WordArtworkKey.HOUSE -> {
            val roof = Path().apply {
                moveTo(cx, cy - u * 0.90f)
                lineTo(cx + u * 0.92f, cy - u * 0.12f)
                lineTo(cx - u * 0.92f, cy - u * 0.12f)
                close()
            }
            drawPath(roof, brush = Brush.linearGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), Offset(cx, cy - u), Offset(cx, cy)))
            drawRoundRect(Color(0xFFFFF8E1), Offset(cx - u * 0.68f, cy - u * 0.14f), Size(u * 1.36f, u * 0.96f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF8D6E63), Offset(cx - u * 0.18f, cy + u * 0.22f), Size(u * 0.36f, u * 0.60f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF81D4FA), Offset(cx - u * 0.52f, cy + u * 0.06f), Size(u * 0.24f, u * 0.24f), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(Color(0xFF81D4FA), Offset(cx + u * 0.28f, cy + u * 0.06f), Size(u * 0.24f, u * 0.24f), CornerRadius(u * 0.04f, u * 0.04f))
        }

        WordArtworkKey.KEY -> {
            rotate(-35f, Offset(cx, cy)) {
                drawCircle(Color(0xFFFFB300), u * 0.38f, Offset(cx - u * 0.46f, cy), style = Stroke(width = u * 0.14f))
                drawRoundRect(Color(0xFFFFCA28), Offset(cx - u * 0.12f, cy - u * 0.09f), Size(u * 0.98f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
                drawRoundRect(Color(0xFFFFB300), Offset(cx + u * 0.42f, cy + u * 0.04f), Size(u * 0.14f, u * 0.24f), CornerRadius(u * 0.04f, u * 0.04f))
                drawRoundRect(Color(0xFFFFB300), Offset(cx + u * 0.66f, cy + u * 0.04f), Size(u * 0.14f, u * 0.30f), CornerRadius(u * 0.04f, u * 0.04f))
            }
        }

        WordArtworkKey.BED, WordArtworkKey.SOFA -> {
            val isSofa = key == WordArtworkKey.SOFA
            val cLight = if (isSofa) Color(0xFF26A69A) else Color(0xFF5C6BC0)
            val cDark = if (isSofa) Color(0xFF00695C) else Color(0xFF283593)
            drawRoundRect(Color(0xFF6D4C41), Offset(cx - u * 0.88f, cy - u * 0.44f), Size(u * 1.76f, u * 0.88f), CornerRadius(u * 0.16f, u * 0.16f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(cLight, cDark), Offset(cx, cy - u * 0.1f), Offset(cx, cy + u * 0.5f)),
                topLeft = Offset(cx - u * 0.80f, cy - u * 0.08f),
                size = Size(u * 1.60f, u * 0.54f),
                cornerRadius = CornerRadius(u * 0.12f, u * 0.12f)
            )
            drawRoundRect(Color.White, Offset(cx - u * 0.66f, cy - u * 0.28f), Size(u * 0.52f, u * 0.22f), CornerRadius(u * 0.10f, u * 0.10f))
            if (isSofa) {
                drawRoundRect(Color.White, Offset(cx + u * 0.14f, cy - u * 0.28f), Size(u * 0.52f, u * 0.22f), CornerRadius(u * 0.10f, u * 0.10f))
            }
        }

        WordArtworkKey.CHAIR, WordArtworkKey.TABLE -> {
            val wood = Color(0xFF8D6E63)
            val woodDark = Color(0xFF5D4037)
            if (key == WordArtworkKey.CHAIR) {
                drawRoundRect(wood, Offset(cx - u * 0.42f, cy - u * 0.80f), Size(u * 0.84f, u * 0.72f), CornerRadius(u * 0.12f, u * 0.12f))
                drawRoundRect(woodDark, Offset(cx - u * 0.48f, cy - u * 0.04f), Size(u * 0.96f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
                drawRoundRect(woodDark, Offset(cx - u * 0.40f, cy + u * 0.12f), Size(u * 0.14f, u * 0.70f), CornerRadius(u * 0.04f, u * 0.04f))
                drawRoundRect(woodDark, Offset(cx + u * 0.26f, cy + u * 0.12f), Size(u * 0.14f, u * 0.70f), CornerRadius(u * 0.04f, u * 0.04f))
            } else {
                drawRoundRect(wood, Offset(cx - u * 0.90f, cy - u * 0.30f), Size(u * 1.80f, u * 0.22f), CornerRadius(u * 0.10f, u * 0.10f))
                drawRoundRect(woodDark, Offset(cx - u * 0.74f, cy - u * 0.10f), Size(u * 0.16f, u * 0.82f), CornerRadius(u * 0.04f, u * 0.04f))
                drawRoundRect(woodDark, Offset(cx + u * 0.58f, cy - u * 0.10f), Size(u * 0.16f, u * 0.82f), CornerRadius(u * 0.04f, u * 0.04f))
            }
        }

        WordArtworkKey.LAMP, WordArtworkKey.BULB -> {
            if (key == WordArtworkKey.LAMP) {
                val shade = Path().apply {
                    moveTo(cx - u * 0.42f, cy - u * 0.82f)
                    lineTo(cx + u * 0.42f, cy - u * 0.82f)
                    lineTo(cx + u * 0.72f, cy - u * 0.10f)
                    lineTo(cx - u * 0.72f, cy - u * 0.10f)
                    close()
                }
                drawPath(shade, brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57C00)), Offset(cx, cy - u * 0.8f), Offset(cx, cy)))
                drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.08f, cy - u * 0.10f), Size(u * 0.16f, u * 0.76f), CornerRadius(u * 0.06f, u * 0.06f))
                drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.44f, cy + u * 0.62f), Size(u * 0.88f, u * 0.18f), CornerRadius(u * 0.09f, u * 0.09f))
            } else {
                drawGlossySphere(Offset(cx, cy - u * 0.22f), u * 0.58f, Color(0xFFFFF59D), Color(0xFFFFB300), 0.65f)
                drawRoundRect(Color(0xFF78909C), Offset(cx - u * 0.24f, cy + u * 0.34f), Size(u * 0.48f, u * 0.36f), CornerRadius(u * 0.10f, u * 0.10f))
            }
        }

        WordArtworkKey.CLOCK -> {
            drawGlossySphere(Offset(cx, cy), u * 0.82f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.4f)
            drawCircle(Color(0xFF37474F), u * 0.76f, Offset(cx, cy), style = Stroke(width = u * 0.08f))
            drawLine(Color(0xFF263238), Offset(cx, cy), Offset(cx, cy - u * 0.48f), u * 0.08f, StrokeCap.Round)
            drawLine(Color(0xFFE53935), Offset(cx, cy), Offset(cx + u * 0.38f, cy + u * 0.18f), u * 0.06f, StrokeCap.Round)
            drawCircle(Color(0xFF263238), u * 0.09f, Offset(cx, cy))
        }

        WordArtworkKey.KNIFE_FORK -> {
            drawRoundRect(Color(0xFF90A4AE), Offset(cx - u * 0.40f, cy - u * 0.82f), Size(u * 0.14f, u * 1.62f), CornerRadius(u * 0.07f, u * 0.07f))
            drawRoundRect(Color(0xFFB0BEC5), Offset(cx - u * 0.54f, cy - u * 0.82f), Size(u * 0.42f, u * 0.48f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF90A4AE), Offset(cx + u * 0.26f, cy - u * 0.82f), Size(u * 0.18f, u * 1.62f), CornerRadius(u * 0.09f, u * 0.09f))
        }

        WordArtworkKey.CUP -> {
            drawCircle(Color(0xFF29B6F6), u * 0.26f, Offset(cx + u * 0.56f, cy + u * 0.06f), style = Stroke(width = u * 0.12f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.6f)),
                topLeft = Offset(cx - u * 0.58f, cy - u * 0.44f),
                size = Size(u * 1.16f, u * 1.08f),
                cornerRadius = CornerRadius(u * 0.24f, u * 0.24f)
            )
        }

        WordArtworkKey.UMBRELLA -> {
            val canopy = Path().apply {
                moveTo(cx - u * 0.90f, cy - u * 0.02f)
                cubicTo(cx - u * 0.86f, cy - u * 0.92f, cx + u * 0.86f, cy - u * 0.92f, cx + u * 0.90f, cy - u * 0.02f)
                close()
            }
            drawPath(canopy, brush = Brush.linearGradient(listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A)), Offset(cx, cy - u), Offset(cx, cy)))
            drawLine(Color(0xFF455A64), Offset(cx, cy - u * 0.02f), Offset(cx, cy + u * 0.72f), u * 0.08f, StrokeCap.Round)
            drawArc(Color(0xFF455A64), 0f, 180f, false, Offset(cx, cy + u * 0.54f), Size(u * 0.30f, u * 0.30f), style = Stroke(width = u * 0.08f, cap = StrokeCap.Round))
        }

        WordArtworkKey.LOCK -> {
            drawArc(
                Color(0xFF90A4AE),
                180f,
                180f,
                false,
                Offset(cx - u * 0.38f, cy - u * 0.86f),
                Size(u * 0.76f, u * 0.78f),
                style = Stroke(width = u * 0.14f, cap = StrokeCap.Round)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57C00)), Offset(cx, cy - u * 0.2f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.62f, cy - u * 0.22f),
                size = Size(u * 1.24f, u * 1.00f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            drawCircle(Color(0xFF3E2723), u * 0.14f, Offset(cx, cy + u * 0.26f))
        }

        WordArtworkKey.GIFT -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFC2185B)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.68f, cy - u * 0.32f),
                size = Size(u * 1.36f, u * 1.12f),
                cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
            )
            drawRoundRect(Color(0xFFF06292), Offset(cx - u * 0.76f, cy - u * 0.44f), Size(u * 1.52f, u * 0.28f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(Color(0xFFFFD54F), Offset(cx - u * 0.12f, cy - u * 0.44f), Size(u * 0.24f, u * 1.24f), CornerRadius(0f, 0f))
            drawCircle(Color(0xFFFFCA28), u * 0.22f, Offset(cx - u * 0.22f, cy - u * 0.64f))
            drawCircle(Color(0xFFFFCA28), u * 0.22f, Offset(cx + u * 0.22f, cy - u * 0.64f))
        }

        WordArtworkKey.DELAY_HOURGLASS -> {
            drawRoundRect(Color(0xFF6D4C41), Offset(cx - u * 0.48f, cy - u * 0.82f), Size(u * 0.96f, u * 0.16f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF6D4C41), Offset(cx - u * 0.48f, cy + u * 0.66f), Size(u * 0.96f, u * 0.16f), CornerRadius(u * 0.08f, u * 0.08f))
            val topBulb = Path().apply {
                moveTo(cx - u * 0.38f, cy - u * 0.66f)
                lineTo(cx + u * 0.38f, cy - u * 0.66f)
                lineTo(cx + u * 0.04f, cy)
                lineTo(cx - u * 0.04f, cy)
                close()
            }
            val botBulb = Path().apply {
                moveTo(cx - u * 0.04f, cy)
                lineTo(cx + u * 0.04f, cy)
                lineTo(cx + u * 0.38f, cy + u * 0.66f)
                lineTo(cx - u * 0.38f, cy + u * 0.66f)
                close()
            }
            drawPath(topBulb, color = Color(0xFFE3F2FD))
            drawPath(botBulb, color = Color(0xFFE3F2FD))
            val sandBot = Path().apply {
                moveTo(cx, cy + u * 0.24f)
                lineTo(cx + u * 0.32f, cy + u * 0.64f)
                lineTo(cx - u * 0.32f, cy + u * 0.64f)
                close()
            }
            drawPath(sandBot, color = Color(0xFFFFB300))
            // Urgent late clock badge in corner
            drawGlossySphere(Offset(cx + u * 0.44f, cy - u * 0.36f), u * 0.28f, Color(0xFFFF5252), Color(0xFFC62828))
            drawLine(Color.White, Offset(cx + u * 0.44f, cy - u * 0.36f), Offset(cx + u * 0.44f, cy - u * 0.52f), u * 0.05f, StrokeCap.Round)
            drawLine(Color.White, Offset(cx + u * 0.44f, cy - u * 0.36f), Offset(cx + u * 0.56f, cy - u * 0.30f), u * 0.05f, StrokeCap.Round)
        }

        WordArtworkKey.DEPARTURE_BOARD -> {
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.84f, cy - u * 0.64f), Size(u * 1.68f, u * 1.24f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(Color(0xFF1E88E5), Offset(cx - u * 0.84f, cy - u * 0.64f), Size(u * 1.68f, u * 0.26f), CornerRadius(u * 0.12f, u * 0.12f))
            listOf(-0.20f, 0.08f, 0.36f).forEachIndexed { idx, dy ->
                drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.68f, cy + dy * u), Size(u * 0.92f, u * 0.18f), CornerRadius(u * 0.04f, u * 0.04f))
                drawRoundRect(
                    if (idx == 0) Color(0xFF69F0AE) else Color(0xFFFFD54F),
                    Offset(cx + u * 0.32f, cy + dy * u),
                    Size(u * 0.38f, u * 0.18f),
                    CornerRadius(u * 0.04f, u * 0.04f)
                )
            }
        }

        WordArtworkKey.RAILWAY_TRACK -> {
            listOf(-0.42f, -0.14f, 0.14f, 0.42f).forEach { dy ->
                val halfW = (0.56f + dy * 0.48f) * u
                drawRoundRect(Color(0xFF8D6E63), Offset(cx - halfW, cy + dy * u), Size(halfW * 2f, u * 0.14f), CornerRadius(u * 0.04f, u * 0.04f))
            }
            drawLine(Color(0xFF78909C), Offset(cx - u * 0.26f, cy - u * 0.72f), Offset(cx - u * 0.68f, cy + u * 0.74f), u * 0.11f, StrokeCap.Round)
            drawLine(Color(0xFF78909C), Offset(cx + u * 0.26f, cy - u * 0.72f), Offset(cx + u * 0.68f, cy + u * 0.74f), u * 0.11f, StrokeCap.Round)
        }

        WordArtworkKey.AIRPORT_TERMINAL -> {
            drawRoundRect(Color(0xFFCFD8DC), Offset(cx - u * 0.76f, cy + u * 0.06f), Size(u * 1.12f, u * 0.66f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(Color(0xFF4FC3F7), Offset(cx - u * 0.64f, cy + u * 0.18f), Size(u * 0.88f, u * 0.30f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF90A4AE), Offset(cx + u * 0.46f, cy - u * 0.34f), Size(u * 0.22f, u * 1.06f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF37474F), Offset(cx + u * 0.30f, cy - u * 0.62f), Size(u * 0.54f, u * 0.30f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF29B6F6), Offset(cx + u * 0.34f, cy - u * 0.54f), Size(u * 0.46f, u * 0.14f), CornerRadius(u * 0.04f, u * 0.04f))
        }

        WordArtworkKey.TRAIN_STATION -> {
            val arch = Path().apply {
                moveTo(cx - u * 0.82f, cy + u * 0.66f)
                lineTo(cx - u * 0.82f, cy - u * 0.06f)
                cubicTo(cx - u * 0.82f, cy - u * 0.86f, cx + u * 0.82f, cy - u * 0.86f, cx + u * 0.82f, cy - u * 0.06f)
                lineTo(cx + u * 0.82f, cy + u * 0.66f)
                close()
            }
            drawPath(arch, brush = Brush.linearGradient(listOf(Color(0xFF90CAF9), Color(0xFF1976D2)), Offset(cx, cy - u * 0.8f), Offset(cx, cy + u * 0.6f)))
            drawRoundRect(Color(0xFFECEFF1), Offset(cx - u * 0.42f, cy - u * 0.04f), Size(u * 0.84f, u * 0.70f), CornerRadius(u * 0.14f, u * 0.14f))
            drawGlossySphere(Offset(cx, cy - u * 0.36f), u * 0.20f, Color.White, Color(0xFFCFD8DC))
        }

        WordArtworkKey.EXIT_DOOR -> {
            drawRoundRect(Color(0xFF2E7D32), Offset(cx - u * 0.52f, cy - u * 0.78f), Size(u * 0.84f, u * 1.54f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(Color(0xFF81C784), Offset(cx - u * 0.40f, cy - u * 0.66f), Size(u * 0.60f, u * 1.42f), CornerRadius(u * 0.06f, u * 0.06f))
            drawLine(Color(0xFF1B5E20), Offset(cx + u * 0.06f, cy), Offset(cx + u * 0.84f, cy), u * 0.14f, StrokeCap.Round)
            val arrow = Path().apply {
                moveTo(cx + u * 0.58f, cy - u * 0.26f)
                lineTo(cx + u * 0.92f, cy)
                lineTo(cx + u * 0.58f, cy + u * 0.26f)
                close()
            }
            drawPath(arrow, color = Color(0xFF1B5E20))
        }

        WordArtworkKey.MEGAPHONE -> {
            val cone = Path().apply {
                moveTo(cx - u * 0.56f, cy - u * 0.16f)
                lineTo(cx + u * 0.48f, cy - u * 0.62f)
                lineTo(cx + u * 0.48f, cy + u * 0.52f)
                lineTo(cx - u * 0.56f, cy + u * 0.12f)
                close()
            }
            drawPath(cone, brush = Brush.linearGradient(listOf(Color(0xFFFF7043), Color(0xFFD84315)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.5f)))
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.52f, cy + u * 0.08f), Size(u * 0.20f, u * 0.54f), CornerRadius(u * 0.08f, u * 0.08f))
            drawArc(Color(0xFFFFB300), -45f, 90f, false, Offset(cx + u * 0.46f, cy - u * 0.34f), Size(u * 0.38f, u * 0.56f), style = Stroke(width = u * 0.08f, cap = StrokeCap.Round))
        }

        WordArtworkKey.MOTORCYCLE -> {
            drawCircle(Color(0xFF263238), u * 0.32f, Offset(cx - u * 0.56f, cy + u * 0.34f), style = Stroke(width = u * 0.11f))
            drawCircle(Color(0xFF263238), u * 0.32f, Offset(cx + u * 0.56f, cy + u * 0.34f), style = Stroke(width = u * 0.11f))
            drawRoundRect(Color(0xFFE53935), Offset(cx - u * 0.46f, cy - u * 0.16f), Size(u * 0.92f, u * 0.38f), CornerRadius(u * 0.16f, u * 0.16f))
            drawLine(Color(0xFF90A4AE), Offset(cx + u * 0.24f, cy - u * 0.36f), Offset(cx + u * 0.56f, cy + u * 0.34f), u * 0.10f, StrokeCap.Round)
        }

        WordArtworkKey.HELICOPTER -> {
            drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.72f, cy - u * 0.54f), Size(u * 1.44f, u * 0.10f), CornerRadius(u * 0.05f, u * 0.05f))
            drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.06f, cy - u * 0.46f), Size(u * 0.12f, u * 0.24f), CornerRadius(0f, 0f))
            drawRoundRect(Color(0xFF0288D1), Offset(cx - u * 0.86f, cy - u * 0.06f), Size(u * 0.64f, u * 0.16f), CornerRadius(u * 0.06f, u * 0.06f))
            drawGlossySphere(Offset(cx + u * 0.12f, cy + u * 0.04f), u * 0.46f, Color(0xFF29B6F6), Color(0xFF01579B))
            drawRoundRect(Color(0xFF81D4FA), Offset(cx + u * 0.08f, cy - u * 0.24f), Size(u * 0.44f, u * 0.28f), CornerRadius(u * 0.10f, u * 0.10f))
        }

        WordArtworkKey.BOTTLE -> {
            drawRoundRect(Color(0xFFE53935), Offset(cx - u * 0.16f, cy - u * 0.88f), Size(u * 0.32f, u * 0.18f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF81D4FA), Offset(cx - u * 0.14f, cy - u * 0.72f), Size(u * 0.28f, u * 0.34f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)), Offset(cx - u * 0.3f, cy), Offset(cx + u * 0.3f, cy)),
                topLeft = Offset(cx - u * 0.38f, cy - u * 0.42f),
                size = Size(u * 0.76f, u * 1.22f),
                cornerRadius = CornerRadius(u * 0.20f, u * 0.20f)
            )
            drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(cx - u * 0.34f, cy), Size(u * 0.68f, u * 0.36f), CornerRadius(u * 0.06f, u * 0.06f))
        }

        WordArtworkKey.PLATE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.82f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.38f)
            drawCircle(Color(0xFF90A4AE), u * 0.56f, Offset(cx, cy), style = Stroke(width = u * 0.045f))
        }

        WordArtworkKey.POT_PAN -> {
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.90f, cy - u * 0.12f), Size(u * 1.80f, u * 0.22f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF90A4AE), Color(0xFF455A64)), Offset(cx - u * 0.6f, cy), Offset(cx + u * 0.6f, cy)),
                topLeft = Offset(cx - u * 0.64f, cy - u * 0.24f),
                size = Size(u * 1.28f, u * 0.94f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            drawRoundRect(Color(0xFF546E7A), Offset(cx - u * 0.68f, cy - u * 0.38f), Size(u * 1.36f, u * 0.18f), CornerRadius(u * 0.09f, u * 0.09f))
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.16f, cy - u * 0.56f), Size(u * 0.32f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.MIRROR -> {
            drawRoundRect(Color(0xFF8D6E63), Offset(cx - u * 0.58f, cy - u * 0.82f), Size(u * 1.16f, u * 1.62f), CornerRadius(u * 0.54f, u * 0.54f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFE1F5FE), Color(0xFF81D4FA)), Offset(cx - u * 0.4f, cy - u * 0.7f), Offset(cx + u * 0.4f, cy + u * 0.7f)),
                topLeft = Offset(cx - u * 0.46f, cy - u * 0.70f),
                size = Size(u * 0.92f, u * 1.38f),
                cornerRadius = CornerRadius(u * 0.44f, u * 0.44f)
            )
            drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - u * 0.24f, cy - u * 0.42f), Offset(cx + u * 0.24f, cy + u * 0.08f), u * 0.09f, StrokeCap.Round)
        }

        WordArtworkKey.FRIDGE -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5)), Offset(cx - u * 0.5f, cy), Offset(cx + u * 0.5f, cy)),
                topLeft = Offset(cx - u * 0.54f, cy - u * 0.82f),
                size = Size(u * 1.08f, u * 1.64f),
                cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
            )
            drawLine(Color(0xFF78909C), Offset(cx - u * 0.54f, cy - u * 0.22f), Offset(cx + u * 0.54f, cy - u * 0.22f), u * 0.05f)
            drawRoundRect(Color(0xFF607D8B), Offset(cx + u * 0.26f, cy - u * 0.58f), Size(u * 0.10f, u * 0.24f), CornerRadius(u * 0.05f, u * 0.05f))
            drawRoundRect(Color(0xFF607D8B), Offset(cx + u * 0.26f, cy - u * 0.06f), Size(u * 0.10f, u * 0.34f), CornerRadius(u * 0.05f, u * 0.05f))
        }

        WordArtworkKey.WARDROBE -> {
            drawRoundRect(Color(0xFF8D6E63), Offset(cx - u * 0.62f, cy - u * 0.80f), Size(u * 1.24f, u * 1.60f), CornerRadius(u * 0.10f, u * 0.10f))
            drawLine(Color(0xFF4E342E), Offset(cx, cy - u * 0.80f), Offset(cx, cy + u * 0.80f), u * 0.05f)
            drawCircle(Color(0xFFFFD54F), u * 0.07f, Offset(cx - u * 0.14f, cy))
            drawCircle(Color(0xFFFFD54F), u * 0.07f, Offset(cx + u * 0.14f, cy))
        }

        WordArtworkKey.ALARM_CLOCK -> {
            drawCircle(Color(0xFFE53935), u * 0.22f, Offset(cx - u * 0.56f, cy - u * 0.56f))
            drawCircle(Color(0xFFE53935), u * 0.22f, Offset(cx + u * 0.56f, cy - u * 0.56f))
            drawGlossySphere(Offset(cx, cy + u * 0.06f), u * 0.72f, Color.White, Color(0xFFCFD8DC), 0.4f)
            drawCircle(Color(0xFFE53935), u * 0.68f, Offset(cx, cy + u * 0.06f), style = Stroke(width = u * 0.09f))
            drawLine(Color(0xFF263238), Offset(cx, cy + u * 0.06f), Offset(cx, cy - u * 0.36f), u * 0.07f, StrokeCap.Round)
            drawLine(Color(0xFF263238), Offset(cx, cy + u * 0.06f), Offset(cx + u * 0.32f, cy + u * 0.22f), u * 0.06f, StrokeCap.Round)
        }

        WordArtworkKey.SCISSORS -> {
            drawLine(Color(0xFF90A4AE), Offset(cx - u * 0.26f, cy + u * 0.12f), Offset(cx + u * 0.64f, cy - u * 0.68f), u * 0.12f, StrokeCap.Round)
            drawLine(Color(0xFF78909C), Offset(cx + u * 0.26f, cy + u * 0.12f), Offset(cx - u * 0.64f, cy - u * 0.68f), u * 0.12f, StrokeCap.Round)
            drawCircle(Color(0xFFE53935), u * 0.26f, Offset(cx - u * 0.42f, cy + u * 0.44f), style = Stroke(width = u * 0.12f))
            drawCircle(Color(0xFFE53935), u * 0.26f, Offset(cx + u * 0.42f, cy + u * 0.44f), style = Stroke(width = u * 0.12f))
        }

        WordArtworkKey.CANDLE -> {
            drawRoundRect(Color(0xFFFFF8E1), Offset(cx - u * 0.26f, cy - u * 0.18f), Size(u * 0.52f, u * 0.96f), CornerRadius(u * 0.08f, u * 0.08f))
            val flame = Path().apply {
                moveTo(cx, cy - u * 0.88f)
                cubicTo(cx + u * 0.28f, cy - u * 0.52f, cx + u * 0.24f, cy - u * 0.24f, cx, cy - u * 0.24f)
                cubicTo(cx - u * 0.24f, cy - u * 0.24f, cx - u * 0.28f, cy - u * 0.52f, cx, cy - u * 0.88f)
                close()
            }
            drawPath(flame, brush = Brush.linearGradient(listOf(Color(0xFFFFF176), Color(0xFFFF6D00)), Offset(cx, cy - u * 0.9f), Offset(cx, cy - u * 0.2f)))
        }

        WordArtworkKey.BELL -> {
            val bell = Path().apply {
                moveTo(cx, cy - u * 0.72f)
                cubicTo(cx + u * 0.54f, cy - u * 0.72f, cx + u * 0.56f, cy + u * 0.18f, cx + u * 0.76f, cy + u * 0.48f)
                lineTo(cx - u * 0.76f, cy + u * 0.48f)
                cubicTo(cx - u * 0.56f, cy + u * 0.18f, cx - u * 0.54f, cy - u * 0.72f, cx, cy - u * 0.72f)
                close()
            }
            drawPath(bell, brush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFF57F17)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.5f)))
            drawCircle(Color(0xFFD97706), u * 0.16f, Offset(cx, cy + u * 0.60f))
        }

        else -> return false
    }
    return true
}
