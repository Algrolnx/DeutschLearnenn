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

internal fun DrawScope.drawTechLifestyleAndBodyVector(key: WordArtworkKey): Boolean {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.PHONE -> {
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.46f, cy - u * 0.88f), Size(u * 0.92f, u * 1.72f), CornerRadius(u * 0.16f, u * 0.16f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF1E88E5)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.6f)),
                topLeft = Offset(cx - u * 0.38f, cy - u * 0.72f),
                size = Size(u * 0.76f, u * 1.36f),
                cornerRadius = CornerRadius(u * 0.10f, u * 0.10f)
            )
            drawCircle(Color(0xFF90A4AE), u * 0.06f, Offset(cx, cy + u * 0.74f))
        }

        WordArtworkKey.LAPTOP, WordArtworkKey.TV -> {
            val isTv = key == WordArtworkKey.TV
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.80f, cy - u * 0.66f), Size(u * 1.60f, u * 1.06f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF1565C0)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.3f)),
                topLeft = Offset(cx - u * 0.70f, cy - u * 0.56f),
                size = Size(u * 1.40f, u * 0.86f),
                cornerRadius = CornerRadius(u * 0.08f, u * 0.08f)
            )
            if (isTv) {
                drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.10f, cy + u * 0.40f), Size(u * 0.20f, u * 0.24f), CornerRadius(0f, 0f))
                drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.44f, cy + u * 0.60f), Size(u * 0.88f, u * 0.14f), CornerRadius(u * 0.07f, u * 0.07f))
            } else {
                drawRoundRect(Color(0xFFB0BEC5), Offset(cx - u * 0.94f, cy + u * 0.40f), Size(u * 1.88f, u * 0.20f), CornerRadius(u * 0.10f, u * 0.10f))
            }
        }

        WordArtworkKey.CAMERA -> {
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.28f, cy - u * 0.66f), Size(u * 0.56f, u * 0.24f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.82f, cy - u * 0.48f), Size(u * 1.64f, u * 1.12f), CornerRadius(u * 0.18f, u * 0.18f))
            drawGlossySphere(Offset(cx, cy + u * 0.08f), u * 0.38f, Color(0xFF4FC3F7), Color(0xFF0D47A1), 0.55f)
            drawCircle(Color(0xFFB0BEC5), u * 0.38f, Offset(cx, cy + u * 0.08f), style = Stroke(width = u * 0.08f))
            drawCircle(Color(0xFFFF5252), u * 0.08f, Offset(cx + u * 0.56f, cy - u * 0.26f))
        }

        WordArtworkKey.HEADPHONES -> {
            drawArc(
                Color(0xFF37474F),
                180f,
                180f,
                false,
                Offset(cx - u * 0.68f, cy - u * 0.76f),
                Size(u * 1.36f, u * 1.32f),
                style = Stroke(width = u * 0.14f, cap = StrokeCap.Round)
            )
            drawRoundRect(Color(0xFF1E88E5), Offset(cx - u * 0.82f, cy - u * 0.10f), Size(u * 0.34f, u * 0.72f), CornerRadius(u * 0.16f, u * 0.16f))
            drawRoundRect(Color(0xFF1E88E5), Offset(cx + u * 0.48f, cy - u * 0.10f), Size(u * 0.34f, u * 0.72f), CornerRadius(u * 0.16f, u * 0.16f))
        }

        WordArtworkKey.BOOK -> {
            drawRoundRect(Color(0xFF1E88E5), Offset(cx - u * 0.68f, cy - u * 0.78f), Size(u * 1.36f, u * 1.56f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(Color(0xFF0D47A1), Offset(cx - u * 0.68f, cy - u * 0.78f), Size(u * 0.28f, u * 1.56f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(Color.White, Offset(cx - u * 0.22f, cy - u * 0.46f), Size(u * 0.72f, u * 0.26f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF90CAF9), Offset(cx - u * 0.22f, cy - u * 0.04f), Size(u * 0.56f, u * 0.14f), CornerRadius(u * 0.06f, u * 0.06f))
        }

        WordArtworkKey.PEN -> {
            rotate(-40f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFFFFCA28), Offset(cx - u * 0.70f, cy - u * 0.18f), Size(u * 1.24f, u * 0.36f), CornerRadius(u * 0.06f, u * 0.06f))
                drawRoundRect(Color(0xFFF48FB1), Offset(cx - u * 0.90f, cy - u * 0.18f), Size(u * 0.24f, u * 0.36f), CornerRadius(u * 0.08f, u * 0.08f))
                val tip = Path().apply {
                    moveTo(cx + u * 0.54f, cy - u * 0.18f)
                    lineTo(cx + u * 0.94f, cy)
                    lineTo(cx + u * 0.54f, cy + u * 0.18f)
                    close()
                }
                drawPath(tip, color = Color(0xFFFFE0B2))
            }
        }

        WordArtworkKey.BACKPACK -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFF7043), Color(0xFFD84315)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.64f, cy - u * 0.66f),
                size = Size(u * 1.28f, u * 1.46f),
                cornerRadius = CornerRadius(u * 0.34f, u * 0.34f)
            )
            drawRoundRect(Color(0xFFFFAB91), Offset(cx - u * 0.44f, cy + u * 0.06f), Size(u * 0.88f, u * 0.54f), CornerRadius(u * 0.14f, u * 0.14f))
        }

        WordArtworkKey.GLASSES -> {
            val frame = Color(0xFF263238)
            val lens = Color(0xFF81D4FA).copy(alpha = 0.55f)
            drawCircle(lens, u * 0.36f, Offset(cx - u * 0.44f, cy))
            drawCircle(lens, u * 0.36f, Offset(cx + u * 0.44f, cy))
            drawCircle(frame, u * 0.36f, Offset(cx - u * 0.44f, cy), style = Stroke(width = u * 0.10f))
            drawCircle(frame, u * 0.36f, Offset(cx + u * 0.44f, cy), style = Stroke(width = u * 0.10f))
            drawLine(frame, Offset(cx - u * 0.08f, cy), Offset(cx + u * 0.08f, cy), u * 0.10f, StrokeCap.Round)
        }

        WordArtworkKey.MONEY, WordArtworkKey.CREDIT_CARD -> {
            if (key == WordArtworkKey.MONEY) {
                drawRoundRect(Color(0xFF66BB6A), Offset(cx - u * 0.88f, cy - u * 0.44f), Size(u * 1.76f, u * 0.94f), CornerRadius(u * 0.14f, u * 0.14f))
                drawCircle(Color(0xFF2E7D32), u * 0.28f, Offset(cx, cy + u * 0.03f))
                drawGlossySphere(Offset(cx + u * 0.44f, cy + u * 0.36f), u * 0.32f, Color(0xFFFFEE58), Color(0xFFF57F17))
            } else {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF283593)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.5f)),
                    topLeft = Offset(cx - u * 0.88f, cy - u * 0.54f),
                    size = Size(u * 1.76f, u * 1.08f),
                    cornerRadius = CornerRadius(u * 0.16f, u * 0.16f)
                )
                drawRoundRect(Color(0xFFFFD54F), Offset(cx - u * 0.64f, cy - u * 0.18f), Size(u * 0.34f, u * 0.24f), CornerRadius(u * 0.06f, u * 0.06f))
            }
        }

        WordArtworkKey.SHIRT, WordArtworkKey.JACKET -> {
            val light = if (key == WordArtworkKey.JACKET) Color(0xFF5C6BC0) else Color(0xFF42A5F5)
            val dark = if (key == WordArtworkKey.JACKET) Color(0xFF283593) else Color(0xFF1565C0)
            val path = Path().apply {
                moveTo(cx - u * 0.34f, cy - u * 0.76f)
                lineTo(cx + u * 0.34f, cy - u * 0.76f)
                lineTo(cx + u * 0.92f, cy - u * 0.34f)
                lineTo(cx + u * 0.72f, cy + u * 0.06f)
                lineTo(cx + u * 0.48f, cy - u * 0.14f)
                lineTo(cx + u * 0.48f, cy + u * 0.80f)
                lineTo(cx - u * 0.48f, cy + u * 0.80f)
                lineTo(cx - u * 0.48f, cy - u * 0.14f)
                lineTo(cx - u * 0.72f, cy + u * 0.06f)
                lineTo(cx - u * 0.92f, cy - u * 0.34f)
                close()
            }
            drawPath(path, brush = Brush.linearGradient(listOf(light, dark), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.8f)))
        }

        WordArtworkKey.PANTS, WordArtworkKey.SOCK -> {
            if (key == WordArtworkKey.PANTS) {
                val pants = Path().apply {
                    moveTo(cx - u * 0.56f, cy - u * 0.74f)
                    lineTo(cx + u * 0.56f, cy - u * 0.74f)
                    lineTo(cx + u * 0.64f, cy + u * 0.80f)
                    lineTo(cx + u * 0.14f, cy + u * 0.80f)
                    lineTo(cx, cy - u * 0.08f)
                    lineTo(cx - u * 0.14f, cy + u * 0.80f)
                    lineTo(cx - u * 0.64f, cy + u * 0.80f)
                    close()
                }
                drawPath(pants, brush = Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1E3A8A)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.8f)))
            } else {
                drawRoundRect(Color(0xFFFF7043), Offset(cx - u * 0.32f, cy - u * 0.76f), Size(u * 0.54f, u * 1.12f), CornerRadius(u * 0.14f, u * 0.14f))
                drawRoundRect(Color(0xFFFF7043), Offset(cx - u * 0.32f, cy + u * 0.16f), Size(u * 0.96f, u * 0.46f), CornerRadius(u * 0.22f, u * 0.22f))
            }
        }

        WordArtworkKey.SHOE -> {
            val shoe = Path().apply {
                moveTo(cx - u * 0.76f, cy - u * 0.26f)
                lineTo(cx - u * 0.10f, cy - u * 0.26f)
                quadraticTo(cx + u * 0.28f, cy + u * 0.14f, cx + u * 0.82f, cy + u * 0.20f)
                lineTo(cx + u * 0.82f, cy + u * 0.54f)
                lineTo(cx - u * 0.76f, cy + u * 0.54f)
                close()
            }
            drawPath(shoe, brush = Brush.linearGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), Offset(cx, cy - u * 0.2f), Offset(cx, cy + u * 0.5f)))
            drawRoundRect(Color.White, Offset(cx - u * 0.78f, cy + u * 0.46f), Size(u * 1.64f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.DRESS -> {
            val dress = Path().apply {
                moveTo(cx - u * 0.34f, cy - u * 0.80f)
                lineTo(cx + u * 0.34f, cy - u * 0.80f)
                lineTo(cx + u * 0.24f, cy - u * 0.14f)
                lineTo(cx + u * 0.76f, cy + u * 0.80f)
                lineTo(cx - u * 0.76f, cy + u * 0.80f)
                lineTo(cx - u * 0.24f, cy - u * 0.14f)
                close()
            }
            drawPath(dress, brush = Brush.linearGradient(listOf(Color(0xFFFF80AB), Color(0xFFC2185B)), Offset(cx, cy - u * 0.8f), Offset(cx, cy + u * 0.8f)))
        }

        WordArtworkKey.HAT -> {
            drawRoundRect(Color(0xFF8D6E63), Offset(cx - u * 0.54f, cy - u * 0.52f), Size(u * 1.08f, u * 0.76f), CornerRadius(u * 0.28f, u * 0.28f))
            drawRoundRect(Color(0xFF3E2723), Offset(cx - u * 0.54f, cy + u * 0.02f), Size(u * 1.08f, u * 0.18f), CornerRadius(0f, 0f))
            drawRoundRect(Color(0xFFA1887F), Offset(cx - u * 0.90f, cy + u * 0.16f), Size(u * 1.80f, u * 0.24f), CornerRadius(u * 0.12f, u * 0.12f))
        }

        WordArtworkKey.HEART -> {
            val heart = Path().apply {
                moveTo(cx, cy + u * 0.82f)
                cubicTo(cx - u * 1.15f, cy + u * 0.12f, cx - u * 0.88f, cy - u * 0.88f, cx, cy - u * 0.32f)
                cubicTo(cx + u * 0.88f, cy - u * 0.88f, cx + u * 1.15f, cy + u * 0.12f, cx, cy + u * 0.82f)
                close()
            }
            drawPath(heart, brush = Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFC62828)), Offset(cx - u * 0.25f, cy - u * 0.25f), u * 1.4f))
        }

        WordArtworkKey.EYE -> {
            val eye = Path().apply {
                moveTo(cx - u * 0.88f, cy)
                quadraticTo(cx, cy - u * 0.72f, cx + u * 0.88f, cy)
                quadraticTo(cx, cy + u * 0.72f, cx - u * 0.88f, cy)
                close()
            }
            drawPath(eye, color = Color.White)
            drawGlossySphere(Offset(cx, cy), u * 0.34f, Color(0xFF42A5F5), Color(0xFF1565C0), 0.5f)
            drawCircle(Color(0xFF111827), u * 0.14f, Offset(cx, cy))
        }

        WordArtworkKey.HAND -> {
            drawRoundRect(Color(0xFFFFCC80), Offset(cx - u * 0.38f, cy - u * 0.12f), Size(u * 0.76f, u * 0.78f), CornerRadius(u * 0.28f, u * 0.28f))
            listOf(-0.28f, -0.09f, 0.10f, 0.28f).forEach { dx ->
                drawRoundRect(Color(0xFFFFB74D), Offset(cx + dx * u - u * 0.07f, cy - u * 0.74f), Size(u * 0.14f, u * 0.72f), CornerRadius(u * 0.07f, u * 0.07f))
            }
            drawRoundRect(Color(0xFFFFB74D), Offset(cx - u * 0.62f, cy + u * 0.08f), Size(u * 0.36f, u * 0.18f), CornerRadius(u * 0.09f, u * 0.09f))
        }

        WordArtworkKey.TOOTH -> {
            val tooth = Path().apply {
                moveTo(cx - u * 0.58f, cy - u * 0.64f)
                quadraticTo(cx, cy - u * 0.44f, cx + u * 0.58f, cy - u * 0.64f)
                cubicTo(cx + u * 0.74f, cy - u * 0.04f, cx + u * 0.56f, cy + u * 0.84f, cx + u * 0.24f, cy + u * 0.84f)
                lineTo(cx, cy + u * 0.34f)
                lineTo(cx - u * 0.24f, cy + u * 0.84f)
                cubicTo(cx - u * 0.56f, cy + u * 0.84f, cx - u * 0.74f, cy - u * 0.04f, cx - u * 0.58f, cy - u * 0.64f)
                close()
            }
            drawPath(tooth, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.8f)))
        }

        WordArtworkKey.PILL -> {
            rotate(-35f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFFFF5252), Offset(cx - u * 0.78f, cy - u * 0.32f), Size(u * 0.90f, u * 0.64f), CornerRadius(u * 0.32f, u * 0.32f))
                drawRoundRect(Color(0xFFFFFFFF), Offset(cx - u * 0.02f, cy - u * 0.32f), Size(u * 0.80f, u * 0.64f), CornerRadius(u * 0.32f, u * 0.32f))
            }
        }

        WordArtworkKey.STETHOSCOPE, WordArtworkKey.HOSPITAL -> {
            if (key == WordArtworkKey.HOSPITAL) {
                drawRoundRect(Color.White, Offset(cx - u * 0.68f, cy - u * 0.76f), Size(u * 1.36f, u * 1.52f), CornerRadius(u * 0.18f, u * 0.18f))
                drawRoundRect(Color(0xFFE53935), Offset(cx - u * 0.16f, cy - u * 0.44f), Size(u * 0.32f, u * 0.88f), CornerRadius(u * 0.08f, u * 0.08f))
                drawRoundRect(Color(0xFFE53935), Offset(cx - u * 0.44f, cy - u * 0.16f), Size(u * 0.88f, u * 0.32f), CornerRadius(u * 0.08f, u * 0.08f))
            } else {
                drawArc(Color(0xFF37474F), 0f, 180f, false, Offset(cx - u * 0.48f, cy - u * 0.78f), Size(u * 0.96f, u * 0.88f), style = Stroke(width = u * 0.10f, cap = StrokeCap.Round))
                drawLine(Color(0xFF37474F), Offset(cx, cy + u * 0.10f), Offset(cx, cy + u * 0.56f), u * 0.10f, StrokeCap.Round)
                drawGlossySphere(Offset(cx, cy + u * 0.60f), u * 0.26f, Color(0xFFB0BEC5), Color(0xFF546E7A))
            }
        }

        WordArtworkKey.GUITAR -> {
            rotate(-28f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFF5D4037), Offset(cx - u * 0.08f, cy - u * 0.98f), Size(u * 0.16f, u * 0.96f), CornerRadius(u * 0.06f, u * 0.06f))
                drawGlossySphere(Offset(cx, cy + u * 0.32f), u * 0.50f, Color(0xFFFFA726), Color(0xFFD84315))
                drawGlossySphere(Offset(cx, cy - u * 0.06f), u * 0.34f, Color(0xFFFFB74D), Color(0xFFE65100), 0.35f)
                drawCircle(Color(0xFF3E2723), u * 0.16f, Offset(cx, cy + u * 0.24f))
            }
        }

        WordArtworkKey.MUSIC_NOTE -> {
            drawGlossySphere(Offset(cx - u * 0.30f, cy + u * 0.42f), u * 0.28f, Color(0xFFAB47BC), Color(0xFF6A1B9A))
            drawGlossySphere(Offset(cx + u * 0.40f, cy + u * 0.30f), u * 0.28f, Color(0xFFAB47BC), Color(0xFF6A1B9A))
            drawLine(Color(0xFF6A1B9A), Offset(cx - u * 0.08f, cy + u * 0.42f), Offset(cx - u * 0.08f, cy - u * 0.62f), u * 0.10f, StrokeCap.Round)
            drawLine(Color(0xFF6A1B9A), Offset(cx + u * 0.62f, cy + u * 0.30f), Offset(cx + u * 0.62f, cy - u * 0.74f), u * 0.10f, StrokeCap.Round)
            drawLine(Color(0xFF6A1B9A), Offset(cx - u * 0.08f, cy - u * 0.62f), Offset(cx + u * 0.62f, cy - u * 0.74f), u * 0.16f, StrokeCap.Round)
        }

        WordArtworkKey.BALL -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFFFFFFFF), Color(0xFFB0BEC5), 0.55f)
            drawCircle(Color(0xFF263238), u * 0.26f, Offset(cx, cy))
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx - u * 0.52f, cy - u * 0.28f))
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx + u * 0.52f, cy - u * 0.28f))
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx - u * 0.36f, cy + u * 0.52f))
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx + u * 0.36f, cy + u * 0.52f))
        }

        WordArtworkKey.CONTROLLER -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF546E7A), Color(0xFF263238)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.5f)),
                topLeft = Offset(cx - u * 0.86f, cy - u * 0.34f),
                size = Size(u * 1.72f, u * 0.82f),
                cornerRadius = CornerRadius(u * 0.38f, u * 0.38f)
            )
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.82f, cy + u * 0.04f), Size(u * 0.48f, u * 0.56f), CornerRadius(u * 0.24f, u * 0.24f))
            drawRoundRect(Color(0xFF37474F), Offset(cx + u * 0.34f, cy + u * 0.04f), Size(u * 0.48f, u * 0.56f), CornerRadius(u * 0.24f, u * 0.24f))
            // D-pad
            drawRoundRect(Color(0xFFCFD8DC), Offset(cx - u * 0.54f, cy - u * 0.16f), Size(u * 0.14f, u * 0.38f), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(Color(0xFFCFD8DC), Offset(cx - u * 0.66f, cy - u * 0.04f), Size(u * 0.38f, u * 0.14f), CornerRadius(u * 0.04f, u * 0.04f))
            // Action buttons
            drawCircle(Color(0xFFEF5350), u * 0.09f, Offset(cx + u * 0.56f, cy - u * 0.08f))
            drawCircle(Color(0xFF66BB6A), u * 0.09f, Offset(cx + u * 0.38f, cy + u * 0.08f))
            drawCircle(Color(0xFF42A5F5), u * 0.09f, Offset(cx + u * 0.56f, cy + u * 0.22f))
            drawCircle(Color(0xFFFFCA28), u * 0.09f, Offset(cx + u * 0.72f, cy + u * 0.08f))
        }

        WordArtworkKey.KEYBOARD -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.4f)),
                topLeft = Offset(cx - u * 0.94f, cy - u * 0.42f),
                size = Size(u * 1.88f, u * 0.88f),
                cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
            )
            for (r in 0..1) {
                for (c in 0..5) {
                    drawRoundRect(
                        Color(0xFF455A64),
                        Offset(cx - u * 0.78f + c * u * 0.26f, cy - u * 0.26f + r * u * 0.24f),
                        Size(u * 0.20f, u * 0.17f),
                        CornerRadius(u * 0.04f, u * 0.04f)
                    )
                }
            }
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.48f, cy + u * 0.22f), Size(u * 0.96f, u * 0.14f), CornerRadius(u * 0.05f, u * 0.05f))
        }

        WordArtworkKey.MICROPHONE -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFCFD8DC), Color(0xFF607D8B)), Offset(cx - u * 0.2f, cy - u * 0.8f), Offset(cx + u * 0.2f, cy)),
                topLeft = Offset(cx - u * 0.26f, cy - u * 0.82f),
                size = Size(u * 0.52f, u * 0.84f),
                cornerRadius = CornerRadius(u * 0.26f, u * 0.26f)
            )
            for (i in -2..2) {
                drawLine(Color(0xFF37474F).copy(alpha = 0.45f), Offset(cx - u * 0.18f, cy - u * 0.42f + i * u * 0.10f), Offset(cx + u * 0.18f, cy - u * 0.42f + i * u * 0.10f), u * 0.03f, StrokeCap.Round)
            }
            drawArc(Color(0xFF455A64), 0f, 180f, false, Offset(cx - u * 0.44f, cy - u * 0.44f), Size(u * 0.88f, u * 0.72f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
            drawLine(Color(0xFF455A64), Offset(cx, cy + u * 0.28f), Offset(cx, cy + u * 0.68f), u * 0.10f, StrokeCap.Round)
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.40f, cy + u * 0.64f), Size(u * 0.80f, u * 0.16f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.ENVELOPE -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3F2FD)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.5f)),
                topLeft = Offset(cx - u * 0.88f, cy - u * 0.56f),
                size = Size(u * 1.76f, u * 1.12f),
                cornerRadius = CornerRadius(u * 0.16f, u * 0.16f)
            )
            val flap = Path().apply {
                moveTo(cx - u * 0.84f, cy - u * 0.50f)
                lineTo(cx, cy + u * 0.12f)
                lineTo(cx + u * 0.84f, cy - u * 0.50f)
            }
            drawPath(flap, color = Color(0xFF42A5F5), style = Stroke(width = u * 0.08f, cap = StrokeCap.Round))
            drawGlossySphere(Offset(cx, cy + u * 0.12f), u * 0.18f, Color(0xFFFF7043), Color(0xFFD84315))
        }

        WordArtworkKey.DOCUMENT_PAGE -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFECEFF1)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.62f, cy - u * 0.80f),
                size = Size(u * 1.24f, u * 1.60f),
                cornerRadius = CornerRadius(u * 0.14f, u * 0.14f)
            )
            drawRoundRect(Color(0xFF42A5F5), Offset(cx - u * 0.42f, cy - u * 0.54f), Size(u * 0.56f, u * 0.14f), CornerRadius(u * 0.06f, u * 0.06f))
            listOf(-0.24f, 0.02f, 0.28f, 0.52f).forEachIndexed { idx, dy ->
                val w = if (idx == 3) 0.54f else 0.84f
                drawRoundRect(Color(0xFF90A4AE), Offset(cx - u * 0.42f, cy + dy * u), Size(u * w, u * 0.10f), CornerRadius(u * 0.05f, u * 0.05f))
            }
        }

        WordArtworkKey.NEWSPAPER -> {
            drawRoundRect(Color(0xFFECEFF1), Offset(cx - u * 0.84f, cy - u * 0.66f), Size(u * 1.68f, u * 1.32f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(Color(0xFF37474F), Offset(cx - u * 0.68f, cy - u * 0.48f), Size(u * 1.36f, u * 0.24f), CornerRadius(u * 0.05f, u * 0.05f))
            drawRoundRect(Color(0xFF42A5F5), Offset(cx - u * 0.68f, cy - u * 0.12f), Size(u * 0.62f, u * 0.56f), CornerRadius(u * 0.06f, u * 0.06f))
            listOf(-0.10f, 0.10f, 0.30f).forEach { dy ->
                drawRoundRect(Color(0xFF90A4AE), Offset(cx + u * 0.06f, cy + dy * u), Size(u * 0.62f, u * 0.10f), CornerRadius(u * 0.04f, u * 0.04f))
            }
        }

        WordArtworkKey.WALLET -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF8D6E63), Color(0xFF4E342E)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.6f)),
                topLeft = Offset(cx - u * 0.82f, cy - u * 0.54f),
                size = Size(u * 1.64f, u * 1.12f),
                cornerRadius = CornerRadius(u * 0.20f, u * 0.20f)
            )
            drawRoundRect(Color(0xFF66BB6A), Offset(cx - u * 0.62f, cy - u * 0.72f), Size(u * 0.94f, u * 0.30f), CornerRadius(u * 0.08f, u * 0.08f))
            drawRoundRect(Color(0xFF6D4C41), Offset(cx + u * 0.24f, cy - u * 0.14f), Size(u * 0.64f, u * 0.40f), CornerRadius(u * 0.14f, u * 0.14f))
            drawCircle(Color(0xFFFFD54F), u * 0.10f, Offset(cx + u * 0.44f, cy + u * 0.06f))
        }

        WordArtworkKey.EAR -> {
            val earOuter = Path().apply {
                moveTo(cx - u * 0.18f, cy - u * 0.68f)
                cubicTo(cx + u * 0.58f, cy - u * 0.82f, cx + u * 0.70f, cy + u * 0.04f, cx + u * 0.24f, cy + u * 0.36f)
                cubicTo(cx + u * 0.08f, cy + u * 0.78f, cx - u * 0.38f, cy + u * 0.74f, cx - u * 0.30f, cy + u * 0.24f)
                cubicTo(cx - u * 0.46f, cy - u * 0.18f, cx - u * 0.42f, cy - u * 0.58f, cx - u * 0.18f, cy - u * 0.68f)
                close()
            }
            drawPath(earOuter, brush = Brush.linearGradient(listOf(Color(0xFFFFCC80), Color(0xFFFB8C00)), Offset(cx - u * 0.3f, cy - u * 0.6f), Offset(cx + u * 0.5f, cy + u * 0.6f)))
            drawArc(Color(0xFFE65100), -80f, 210f, false, Offset(cx - u * 0.16f, cy - u * 0.42f), Size(u * 0.54f, u * 0.66f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
        }

        WordArtworkKey.MOUTH -> {
            val upperLip = Path().apply {
                moveTo(cx - u * 0.82f, cy)
                quadraticTo(cx - u * 0.38f, cy - u * 0.44f, cx, cy - u * 0.22f)
                quadraticTo(cx + u * 0.38f, cy - u * 0.44f, cx + u * 0.82f, cy)
                quadraticTo(cx, cy + u * 0.14f, cx - u * 0.82f, cy)
                close()
            }
            val lowerLip = Path().apply {
                moveTo(cx - u * 0.82f, cy)
                quadraticTo(cx, cy + u * 0.62f, cx + u * 0.82f, cy)
                quadraticTo(cx, cy + u * 0.12f, cx - u * 0.82f, cy)
                close()
            }
            drawPath(upperLip, brush = Brush.linearGradient(listOf(Color(0xFFFF6B8B), Color(0xFFD81B60)), Offset(cx, cy - u * 0.4f), Offset(cx, cy)))
            drawPath(lowerLip, brush = Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFAD1457)), Offset(cx, cy), Offset(cx, cy + u * 0.6f)))
            drawRoundRect(Color.White, Offset(cx - u * 0.44f, cy - u * 0.06f), Size(u * 0.88f, u * 0.14f), CornerRadius(u * 0.06f, u * 0.06f))
        }

        WordArtworkKey.FOOT -> {
            val foot = Path().apply {
                moveTo(cx - u * 0.24f, cy - u * 0.62f)
                cubicTo(cx + u * 0.28f, cy - u * 0.64f, cx + u * 0.36f, cy + u * 0.08f, cx + u * 0.56f, cy + u * 0.34f)
                cubicTo(cx + u * 0.66f, cy + u * 0.72f, cx - u * 0.22f, cy + u * 0.78f, cx - u * 0.34f, cy + u * 0.40f)
                cubicTo(cx - u * 0.44f, cy - u * 0.10f, cx - u * 0.48f, cy - u * 0.56f, cx - u * 0.24f, cy - u * 0.62f)
                close()
            }
            drawPath(foot, brush = Brush.linearGradient(listOf(Color(0xFFFFCC80), Color(0xFFFB8C00)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.7f)))
            listOf(-0.26f to 0.14f, -0.06f to 0.11f, 0.12f to 0.10f, 0.28f to 0.09f).forEachIndexed { idx, (dx, r) ->
                drawCircle(Color(0xFFFFB74D), u * r, Offset(cx + dx * u, cy - u * (0.64f - idx * 0.05f)))
            }
        }

        WordArtworkKey.AMBULANCE -> {
            drawRoundRect(Color.White, Offset(cx - u * 0.88f, cy - u * 0.46f), Size(u * 1.46f, u * 0.94f), CornerRadius(u * 0.16f, u * 0.16f))
            drawRoundRect(Color(0xFFECEFF1), Offset(cx + u * 0.32f, cy - u * 0.20f), Size(u * 0.54f, u * 0.68f), CornerRadius(u * 0.14f, u * 0.14f))
            drawRoundRect(Color(0xFF4FC3F7), Offset(cx + u * 0.42f, cy - u * 0.10f), Size(u * 0.36f, u * 0.28f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFFEF5350), Offset(cx - u * 0.32f, cy - u * 0.26f), Size(u * 0.16f, u * 0.48f), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(Color(0xFFEF5350), Offset(cx - u * 0.48f, cy - u * 0.10f), Size(u * 0.48f, u * 0.16f), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(Color(0xFF29B6F6), Offset(cx - u * 0.24f, cy - u * 0.62f), Size(u * 0.30f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
            drawCircle(Color(0xFF263238), u * 0.22f, Offset(cx - u * 0.48f, cy + u * 0.50f))
            drawCircle(Color(0xFF263238), u * 0.22f, Offset(cx + u * 0.52f, cy + u * 0.50f))
            drawCircle(Color(0xFFCFD8DC), u * 0.10f, Offset(cx - u * 0.48f, cy + u * 0.50f))
            drawCircle(Color(0xFFCFD8DC), u * 0.10f, Offset(cx + u * 0.52f, cy + u * 0.50f))
        }

        WordArtworkKey.THERMOMETER -> {
            drawRoundRect(Color(0xFFECEFF1), Offset(cx - u * 0.20f, cy - u * 0.84f), Size(u * 0.40f, u * 1.24f), CornerRadius(u * 0.20f, u * 0.20f))
            drawGlossySphere(Offset(cx, cy + u * 0.48f), u * 0.34f, Color(0xFFFF5252), Color(0xFFC62828))
            drawRoundRect(Color(0xFFE53935), Offset(cx - u * 0.09f, cy - u * 0.48f), Size(u * 0.18f, u * 0.90f), CornerRadius(u * 0.09f, u * 0.09f))
            for (i in 0..3) {
                drawLine(Color(0xFF546E7A), Offset(cx + u * 0.24f, cy - u * 0.56f + i * u * 0.22f), Offset(cx + u * 0.40f, cy - u * 0.56f + i * u * 0.22f), u * 0.05f, StrokeCap.Round)
            }
        }

        WordArtworkKey.SECURITY_SHIELD -> {
            val shield = Path().apply {
                moveTo(cx, cy - u * 0.84f)
                lineTo(cx + u * 0.72f, cy - u * 0.56f)
                cubicTo(cx + u * 0.74f, cy + u * 0.26f, cx + u * 0.34f, cy + u * 0.68f, cx, cy + u * 0.84f)
                cubicTo(cx - u * 0.34f, cy + u * 0.68f, cx - u * 0.74f, cy + u * 0.26f, cx - u * 0.72f, cy - u * 0.56f)
                close()
            }
            drawPath(shield, brush = Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0)), Offset(cx, cy - u * 0.8f), Offset(cx, cy + u * 0.8f)))
            val check = Path().apply {
                moveTo(cx - u * 0.28f, cy + u * 0.02f)
                lineTo(cx - u * 0.06f, cy + u * 0.24f)
                lineTo(cx + u * 0.32f, cy - u * 0.22f)
            }
            drawPath(check, color = Color.White, style = Stroke(width = u * 0.12f, cap = StrokeCap.Round))
        }

        WordArtworkKey.PIANO -> {
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.88f, cy - u * 0.58f), Size(u * 1.76f, u * 1.16f), CornerRadius(u * 0.14f, u * 0.14f))
            for (i in 0..5) {
                drawRoundRect(
                    Color.White,
                    Offset(cx - u * 0.78f + i * u * 0.26f, cy - u * 0.42f),
                    Size(u * 0.22f, u * 0.88f),
                    CornerRadius(u * 0.04f, u * 0.04f)
                )
            }
            listOf(-0.62f, -0.36f, 0.16f, 0.42f).forEach { dx ->
                drawRoundRect(Color(0xFF111827), Offset(cx + dx * u, cy - u * 0.42f), Size(u * 0.16f, u * 0.52f), CornerRadius(u * 0.03f, u * 0.03f))
            }
        }

        WordArtworkKey.VIOLIN -> {
            rotate(-28f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFF4E342E), Offset(cx - u * 0.07f, cy - u * 0.96f), Size(u * 0.14f, u * 0.88f), CornerRadius(u * 0.05f, u * 0.05f))
                drawGlossySphere(Offset(cx, cy + u * 0.30f), u * 0.44f, Color(0xFFFF8A65), Color(0xFFBF360C))
                drawGlossySphere(Offset(cx, cy - u * 0.08f), u * 0.32f, Color(0xFFFFAB91), Color(0xFFD84315), 0.4f)
                drawRoundRect(Color(0xFF3E2723), Offset(cx - u * 0.12f, cy + u * 0.34f), Size(u * 0.24f, u * 0.30f), CornerRadius(u * 0.06f, u * 0.06f))
            }
        }

        WordArtworkKey.MEDAL -> {
            val ribbonLeft = Path().apply {
                moveTo(cx - u * 0.42f, cy - u * 0.84f)
                lineTo(cx - u * 0.14f, cy - u * 0.84f)
                lineTo(cx + u * 0.04f, cy - u * 0.14f)
                lineTo(cx - u * 0.22f, cy - u * 0.14f)
                close()
            }
            val ribbonRight = Path().apply {
                moveTo(cx + u * 0.42f, cy - u * 0.84f)
                lineTo(cx + u * 0.14f, cy - u * 0.84f)
                lineTo(cx - u * 0.04f, cy - u * 0.14f)
                lineTo(cx + u * 0.22f, cy - u * 0.14f)
                close()
            }
            drawPath(ribbonLeft, color = Color(0xFFEF5350))
            drawPath(ribbonRight, color = Color(0xFF1E88E5))
            drawGlossySphere(Offset(cx, cy + u * 0.24f), u * 0.54f, Color(0xFFFFEE58), Color(0xFFF57F17))
            drawCircle(Color(0xFFFFF9C4), u * 0.34f, Offset(cx, cy + u * 0.24f), style = Stroke(width = u * 0.06f))
        }

        else -> return false
    }
    return true
}
