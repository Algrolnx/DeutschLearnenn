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

internal fun DrawScope.drawNatureAndAnimalVector(key: WordArtworkKey): Boolean {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.DOG -> {
            // Friendly dog head with floppy ears & glossy nose
            drawOval(Color(0xFF795548), Offset(cx - u * 0.92f, cy - u * 0.36f), Size(u * 0.44f, u * 0.96f))
            drawOval(Color(0xFF795548), Offset(cx + u * 0.48f, cy - u * 0.36f), Size(u * 0.44f, u * 0.96f))
            drawGlossySphere(Offset(cx, cy), u * 0.72f, Color(0xFFD7A86E), Color(0xFF9C6635), 0.32f)
            drawOval(Color(0xFFFFF3E0), Offset(cx - u * 0.38f, cy + u * 0.04f), Size(u * 0.76f, u * 0.54f))
            drawCircle(Color(0xFF212121), u * 0.09f, Offset(cx - u * 0.28f, cy - u * 0.14f))
            drawCircle(Color(0xFF212121), u * 0.09f, Offset(cx + u * 0.28f, cy - u * 0.14f))
            drawCircle(Color.White, u * 0.03f, Offset(cx - u * 0.30f, cy - u * 0.17f))
            drawCircle(Color.White, u * 0.03f, Offset(cx + u * 0.26f, cy - u * 0.17f))
            drawOval(Color(0xFF262626), Offset(cx - u * 0.16f, cy + u * 0.06f), Size(u * 0.32f, u * 0.22f))
            drawRoundRect(Color(0xFFFF6B81), Offset(cx - u * 0.10f, cy + u * 0.36f), Size(u * 0.20f, u * 0.26f), CornerRadius(u * 0.10f, u * 0.10f))
        }

        WordArtworkKey.CAT -> {
            // Pointed ears + sleek feline face + whiskers
            val earL = Path().apply {
                moveTo(cx - u * 0.62f, cy - u * 0.16f)
                lineTo(cx - u * 0.68f, cy - u * 0.94f)
                lineTo(cx - u * 0.14f, cy - u * 0.56f)
                close()
            }
            val earR = Path().apply {
                moveTo(cx + u * 0.62f, cy - u * 0.16f)
                lineTo(cx + u * 0.68f, cy - u * 0.94f)
                lineTo(cx + u * 0.14f, cy - u * 0.56f)
                close()
            }
            drawPath(earL, color = Color(0xFFFF9800))
            drawPath(earR, color = Color(0xFFFF9800))
            drawGlossySphere(Offset(cx, cy + u * 0.06f), u * 0.72f, Color(0xFFFFCC80), Color(0xFFEF6C00), 0.35f)
            drawCircle(Color(0xFF1B5E20), u * 0.09f, Offset(cx - u * 0.26f, cy - u * 0.04f))
            drawCircle(Color(0xFF1B5E20), u * 0.09f, Offset(cx + u * 0.26f, cy - u * 0.04f))
            val nose = Path().apply {
                moveTo(cx - u * 0.10f, cy + u * 0.12f)
                lineTo(cx + u * 0.10f, cy + u * 0.12f)
                lineTo(cx, cy + u * 0.24f)
                close()
            }
            drawPath(nose, color = Color(0xFFF48FB1))
            listOf(-1f, 1f).forEach { dir ->
                drawLine(Color(0xFF4E342E), Offset(cx + dir * u * 0.36f, cy + u * 0.14f), Offset(cx + dir * u * 0.88f, cy + u * 0.04f), u * 0.03f, StrokeCap.Round)
                drawLine(Color(0xFF4E342E), Offset(cx + dir * u * 0.36f, cy + u * 0.26f), Offset(cx + dir * u * 0.88f, cy + u * 0.30f), u * 0.03f, StrokeCap.Round)
            }
        }

        WordArtworkKey.BIRD -> {
            // Glossy bluebird profile with yellow beak & wing
            val tail = Path().apply {
                moveTo(cx - u * 0.54f, cy + u * 0.10f)
                lineTo(cx - u * 0.98f, cy - u * 0.18f)
                lineTo(cx - u * 0.86f, cy + u * 0.26f)
                close()
            }
            drawPath(tail, color = Color(0xFF0277BD))
            drawGlossySphere(Offset(cx - u * 0.08f, cy + u * 0.18f), u * 0.56f, Color(0xFF4FC3F7), Color(0xFF0288D1), 0.45f)
            drawGlossySphere(Offset(cx + u * 0.34f, cy - u * 0.24f), u * 0.36f, Color(0xFF81D4FA), Color(0xFF0288D1), 0.5f)
            val beak = Path().apply {
                moveTo(cx + u * 0.64f, cy - u * 0.30f)
                lineTo(cx + u * 0.98f, cy - u * 0.18f)
                lineTo(cx + u * 0.64f, cy - u * 0.06f)
                close()
            }
            drawPath(beak, color = Color(0xFFFFB300))
            drawCircle(Color(0xFF1A237E), u * 0.065f, Offset(cx + u * 0.44f, cy - u * 0.28f))
            drawOval(Color(0xFF039BE5), Offset(cx - u * 0.42f, cy - u * 0.04f), Size(u * 0.58f, u * 0.34f))
        }

        WordArtworkKey.HORSE -> {
            // Crisp equestrian head silhouette with mane & bridle accent
            val mane = Path().apply {
                moveTo(cx - u * 0.16f, cy - u * 0.88f)
                quadraticTo(cx - u * 0.72f, cy - u * 0.36f, cx - u * 0.68f, cy + u * 0.68f)
                lineTo(cx - u * 0.28f, cy + u * 0.68f)
                close()
            }
            drawPath(mane, color = Color(0xFF3E2723))
            val head = Path().apply {
                moveTo(cx - u * 0.22f, cy - u * 0.86f)
                lineTo(cx + u * 0.02f, cy - u * 0.58f)
                lineTo(cx + u * 0.72f, cy + u * 0.02f)
                cubicTo(cx + u * 0.84f, cy + u * 0.28f, cx + u * 0.56f, cy + u * 0.44f, cx + u * 0.28f, cy + u * 0.28f)
                lineTo(cx - u * 0.06f, cy + u * 0.76f)
                lineTo(cx - u * 0.54f, cy + u * 0.76f)
                cubicTo(cx - u * 0.54f, cy + u * 0.10f, cx - u * 0.42f, cy - u * 0.50f, cx - u * 0.22f, cy - u * 0.86f)
                close()
            }
            drawPath(head, brush = Brush.linearGradient(listOf(Color(0xFFA16B47), Color(0xFF6D4C41)), Offset(cx, cy - u), Offset(cx, cy + u)))
            drawCircle(Color(0xFF1A1A1A), u * 0.06f, Offset(cx + u * 0.14f, cy - u * 0.26f))
        }

        WordArtworkKey.COW, WordArtworkKey.PIG -> {
            val isCow = key == WordArtworkKey.COW
            val light = if (isCow) Color(0xFFFFFFFF) else Color(0xFFFFB2C9)
            val dark = if (isCow) Color(0xFFDDE3EA) else Color(0xFFF06292)
            if (isCow) {
                drawOval(Color(0xFFD7CCC8), Offset(cx - u * 0.68f, cy - u * 0.82f), Size(u * 0.30f, u * 0.42f))
                drawOval(Color(0xFFD7CCC8), Offset(cx + u * 0.38f, cy - u * 0.82f), Size(u * 0.30f, u * 0.42f))
            } else {
                drawOval(Color(0xFFF48FB1), Offset(cx - u * 0.68f, cy - u * 0.72f), Size(u * 0.36f, u * 0.44f))
                drawOval(Color(0xFFF48FB1), Offset(cx + u * 0.32f, cy - u * 0.72f), Size(u * 0.36f, u * 0.44f))
            }
            drawGlossySphere(Offset(cx, cy + u * 0.04f), u * 0.72f, light, dark, 0.38f)
            if (isCow) {
                drawOval(Color(0xFF37474F), Offset(cx - u * 0.52f, cy - u * 0.48f), Size(u * 0.44f, u * 0.36f))
            }
            drawCircle(Color(0xFF212121), u * 0.075f, Offset(cx - u * 0.26f, cy - u * 0.16f))
            drawCircle(Color(0xFF212121), u * 0.075f, Offset(cx + u * 0.26f, cy - u * 0.16f))
            val snoutColor = if (isCow) Color(0xFFFFB2C9) else Color(0xFFEC407A)
            drawOval(snoutColor, Offset(cx - u * 0.40f, cy + u * 0.06f), Size(u * 0.80f, u * 0.46f))
            drawCircle(Color(0xFF880E4F), u * 0.06f, Offset(cx - u * 0.16f, cy + u * 0.28f))
            drawCircle(Color(0xFF880E4F), u * 0.06f, Offset(cx + u * 0.16f, cy + u * 0.28f))
        }

        WordArtworkKey.BUTTERFLY -> {
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFE65100)), Offset(cx - u * 0.4f, cy - u * 0.2f), u),
                topLeft = Offset(cx - u * 0.92f, cy - u * 0.68f),
                size = Size(u * 0.86f, u * 0.76f)
            )
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFE65100)), Offset(cx + u * 0.4f, cy - u * 0.2f), u),
                topLeft = Offset(cx + u * 0.06f, cy - u * 0.68f),
                size = Size(u * 0.86f, u * 0.76f)
            )
            drawOval(Color(0xFFFF8A65), Offset(cx - u * 0.74f, cy + u * 0.04f), Size(u * 0.68f, u * 0.64f))
            drawOval(Color(0xFFFF8A65), Offset(cx + u * 0.06f, cy + u * 0.04f), Size(u * 0.68f, u * 0.64f))
            drawRoundRect(Color(0xFF3E2723), Offset(cx - u * 0.10f, cy - u * 0.52f), Size(u * 0.20f, u * 1.12f), CornerRadius(u * 0.10f, u * 0.10f))
            drawLine(Color(0xFF3E2723), Offset(cx - u * 0.04f, cy - u * 0.50f), Offset(cx - u * 0.26f, cy - u * 0.82f), u * 0.045f, StrokeCap.Round)
            drawLine(Color(0xFF3E2723), Offset(cx + u * 0.04f, cy - u * 0.50f), Offset(cx + u * 0.26f, cy - u * 0.82f), u * 0.045f, StrokeCap.Round)
        }

        WordArtworkKey.BEE -> {
            drawOval(Color(0xFFE0F7FA), Offset(cx - u * 0.56f, cy - u * 0.84f), Size(u * 0.54f, u * 0.72f))
            drawOval(Color(0xFFE0F7FA), Offset(cx + u * 0.02f, cy - u * 0.84f), Size(u * 0.54f, u * 0.72f))
            drawOval(
                brush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFFBC02D)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.6f)),
                topLeft = Offset(cx - u * 0.78f, cy - u * 0.34f),
                size = Size(u * 1.56f, u * 0.96f)
            )
            drawRoundRect(Color(0xFF212121), Offset(cx - u * 0.22f, cy - u * 0.32f), Size(u * 0.18f, u * 0.92f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF212121), Offset(cx + u * 0.14f, cy - u * 0.30f), Size(u * 0.18f, u * 0.88f), CornerRadius(u * 0.06f, u * 0.06f))
            drawCircle(Color(0xFF212121), u * 0.06f, Offset(cx - u * 0.54f, cy + u * 0.06f))
        }

        WordArtworkKey.FROG, WordArtworkKey.RABBIT -> {
            val isFrog = key == WordArtworkKey.FROG
            if (isFrog) {
                drawGlossySphere(Offset(cx - u * 0.42f, cy - u * 0.44f), u * 0.28f, Color(0xFF81C784), Color(0xFF2E7D32))
                drawGlossySphere(Offset(cx + u * 0.42f, cy - u * 0.44f), u * 0.28f, Color(0xFF81C784), Color(0xFF2E7D32))
                drawCircle(Color.White, u * 0.15f, Offset(cx - u * 0.42f, cy - u * 0.46f))
                drawCircle(Color.White, u * 0.15f, Offset(cx + u * 0.42f, cy - u * 0.46f))
                drawCircle(Color(0xFF1B1B1B), u * 0.08f, Offset(cx - u * 0.42f, cy - u * 0.46f))
                drawCircle(Color(0xFF1B1B1B), u * 0.08f, Offset(cx + u * 0.42f, cy - u * 0.46f))
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)), Offset(cx, cy), u * 1.1f),
                    topLeft = Offset(cx - u * 0.84f, cy - u * 0.36f),
                    size = Size(u * 1.68f, u * 1.08f)
                )
            } else {
                drawOval(Color(0xFFF5F5F5), Offset(cx - u * 0.42f, cy - u * 1.06f), Size(u * 0.30f, u * 0.88f))
                drawOval(Color(0xFFF48FB1), Offset(cx - u * 0.34f, cy - u * 0.94f), Size(u * 0.14f, u * 0.62f))
                drawOval(Color(0xFFF5F5F5), Offset(cx + u * 0.12f, cy - u * 1.06f), Size(u * 0.30f, u * 0.88f))
                drawOval(Color(0xFFF48FB1), Offset(cx + u * 0.20f, cy - u * 0.94f), Size(u * 0.14f, u * 0.62f))
                drawGlossySphere(Offset(cx, cy + u * 0.12f), u * 0.68f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.4f)
                drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx - u * 0.24f, cy + u * 0.02f))
                drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx + u * 0.24f, cy + u * 0.02f))
                drawCircle(Color(0xFFF06292), u * 0.09f, Offset(cx, cy + u * 0.20f))
            }
        }

        WordArtworkKey.SUN -> {
            for (i in 0 until 12) {
                val rad = i * (2.0 * PI / 12.0)
                val sx = cx + cos(rad).toFloat() * u * 0.68f
                val sy = cy + sin(rad).toFloat() * u * 0.68f
                val ex = cx + cos(rad).toFloat() * u * 0.96f
                val ey = cy + sin(rad).toFloat() * u * 0.96f
                drawLine(Color(0xFFFFB300), Offset(sx, sy), Offset(ex, ey), u * 0.11f, StrokeCap.Round)
            }
            drawGlossySphere(Offset(cx, cy), u * 0.54f, Color(0xFFFFF176), Color(0xFFFB8C00), 0.60f)
        }

        WordArtworkKey.MOON -> {
            drawGlossySphere(Offset(cx, cy), u * 0.78f, Color(0xFFFFF59D), Color(0xFFFBC02D), 0.50f)
            drawCircle(Color(0xFFF57F17).copy(alpha = 0.25f), u * 0.16f, Offset(cx - u * 0.24f, cy - u * 0.18f))
            drawCircle(Color(0xFFF57F17).copy(alpha = 0.25f), u * 0.11f, Offset(cx + u * 0.26f, cy + u * 0.22f))
        }

        WordArtworkKey.STAR -> {
            val star = Path().apply {
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) u * 0.92f else u * 0.40f
                    val a = -PI / 2.0 + i * (PI / 5.0)
                    val px = cx + cos(a).toFloat() * r
                    val py = cy + sin(a).toFloat() * r
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(star, brush = Brush.radialGradient(listOf(Color(0xFFFFF176), Color(0xFFFFA000)), Offset(cx - u * 0.2f, cy - u * 0.2f), u * 1.1f))
        }

        WordArtworkKey.CLOUD, WordArtworkKey.RAIN, WordArtworkKey.SNOW -> {
            val isRain = key == WordArtworkKey.RAIN
            val isSnow = key == WordArtworkKey.SNOW
            val cloudTop = cy - u * 0.18f
            drawGlossySphere(Offset(cx - u * 0.42f, cloudTop + u * 0.12f), u * 0.38f, Color(0xFFFFFFFF), Color(0xFFB0BEC5), 0.35f)
            drawGlossySphere(Offset(cx + u * 0.42f, cloudTop + u * 0.14f), u * 0.36f, Color(0xFFFFFFFF), Color(0xFFB0BEC5), 0.35f)
            drawGlossySphere(Offset(cx, cloudTop - u * 0.10f), u * 0.48f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.45f)
            drawRoundRect(Color(0xFFCFD8DC), Offset(cx - u * 0.56f, cloudTop + u * 0.18f), Size(u * 1.12f, u * 0.30f), CornerRadius(u * 0.15f, u * 0.15f))
            if (isRain) {
                listOf(-0.36f, 0f, 0.36f).forEach { dx ->
                    drawLine(Color(0xFF29B6F6), Offset(cx + dx * u, cloudTop + u * 0.62f), Offset(cx + (dx - 0.12f) * u, cloudTop + u * 0.96f), u * 0.10f, StrokeCap.Round)
                }
            } else if (isSnow) {
                listOf(-0.36f, 0f, 0.36f).forEach { dx ->
                    drawCircle(Color(0xFF81D4FA), u * 0.10f, Offset(cx + dx * u, cloudTop + u * 0.78f))
                }
            }
        }

        WordArtworkKey.TREE -> {
            drawRoundRect(Color(0xFF795548), Offset(cx - u * 0.16f, cy + u * 0.12f), Size(u * 0.32f, u * 0.78f), CornerRadius(u * 0.08f, u * 0.08f))
            drawGlossySphere(Offset(cx - u * 0.34f, cy - u * 0.06f), u * 0.44f, Color(0xFF66BB6A), Color(0xFF2E7D32), 0.30f)
            drawGlossySphere(Offset(cx + u * 0.34f, cy - u * 0.06f), u * 0.44f, Color(0xFF66BB6A), Color(0xFF2E7D32), 0.30f)
            drawGlossySphere(Offset(cx, cy - u * 0.44f), u * 0.54f, Color(0xFF81C784), Color(0xFF388E3C), 0.38f)
        }

        WordArtworkKey.FLOWER -> {
            drawLine(Color(0xFF43A047), Offset(cx, cy + u * 0.05f), Offset(cx, cy + u * 0.94f), u * 0.10f, StrokeCap.Round)
            for (i in 0 until 6) {
                val a = i * (2.0 * PI / 6.0)
                val px = cx + cos(a).toFloat() * u * 0.44f
                val py = cy - u * 0.24f + sin(a).toFloat() * u * 0.44f
                drawGlossySphere(Offset(px, py), u * 0.28f, Color(0xFFFF80AB), Color(0xFFD81B60), 0.35f)
            }
            drawGlossySphere(Offset(cx, cy - u * 0.24f), u * 0.26f, Color(0xFFFFEE58), Color(0xFFFBC02D), 0.5f)
        }

        WordArtworkKey.MOUNTAIN -> {
            drawGlossySphere(Offset(cx + u * 0.56f, cy - u * 0.54f), u * 0.24f, Color(0xFFFFF176), Color(0xFFFFA000), 0.5f)
            val backPeak = Path().apply {
                moveTo(cx - u * 0.14f, cy + u * 0.62f)
                lineTo(cx + u * 0.44f, cy - u * 0.52f)
                lineTo(cx + u * 0.96f, cy + u * 0.62f)
                close()
            }
            drawPath(backPeak, brush = Brush.linearGradient(listOf(Color(0xFF78909C), Color(0xFF37474F)), Offset(cx + u * 0.4f, cy - u * 0.5f), Offset(cx + u * 0.4f, cy + u * 0.6f)))
            val frontPeak = Path().apply {
                moveTo(cx - u * 0.96f, cy + u * 0.62f)
                lineTo(cx - u * 0.16f, cy - u * 0.74f)
                lineTo(cx + u * 0.60f, cy + u * 0.62f)
                close()
            }
            drawPath(frontPeak, brush = Brush.linearGradient(listOf(Color(0xFF607D8B), Color(0xFF263238)), Offset(cx - u * 0.2f, cy - u * 0.7f), Offset(cx, cy + u * 0.6f)))
            // Sunlit left ridge + jagged snow cap
            val sunlitFace = Path().apply {
                moveTo(cx - u * 0.96f, cy + u * 0.62f)
                lineTo(cx - u * 0.16f, cy - u * 0.74f)
                lineTo(cx - u * 0.04f, cy + u * 0.62f)
                close()
            }
            drawPath(sunlitFace, color = Color(0xFF90A4AE).copy(alpha = 0.45f))
            val snowCap = Path().apply {
                moveTo(cx - u * 0.16f, cy - u * 0.74f)
                lineTo(cx + u * 0.14f, cy - u * 0.22f)
                lineTo(cx - u * 0.04f, cy - u * 0.30f)
                lineTo(cx - u * 0.20f, cy - u * 0.18f)
                lineTo(cx - u * 0.42f, cy - u * 0.26f)
                close()
            }
            drawPath(snowCap, color = Color.White)
            // Emerald foothill strip
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)), Offset(cx, cy + u * 0.4f), Offset(cx, cy + u * 0.75f)),
                topLeft = Offset(cx - u * 0.96f, cy + u * 0.48f),
                size = Size(u * 1.92f, u * 0.24f),
                cornerRadius = CornerRadius(u * 0.12f, u * 0.12f)
            )
        }

        WordArtworkKey.WAVE -> {
            val wave = Path().apply {
                moveTo(cx - u * 0.92f, cy + u * 0.46f)
                cubicTo(cx - u * 0.64f, cy - u * 0.72f, cx + u * 0.28f, cy - u * 0.82f, cx + u * 0.58f, cy - u * 0.24f)
                cubicTo(cx + u * 0.30f, cy - u * 0.38f, cx + u * 0.10f, cy - u * 0.06f, cx + u * 0.36f, cy + u * 0.14f)
                lineTo(cx + u * 0.92f, cy + u * 0.14f)
                lineTo(cx + u * 0.92f, cy + u * 0.64f)
                lineTo(cx - u * 0.92f, cy + u * 0.64f)
                close()
            }
            drawPath(wave, brush = Brush.linearGradient(listOf(Color(0xFF29B6F6), Color(0xFF01579B)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.6f)))
        }

        WordArtworkKey.FIRE -> {
            val outer = Path().apply {
                moveTo(cx, cy - u * 0.96f)
                cubicTo(cx + u * 0.76f, cy - u * 0.30f, cx + u * 0.86f, cy + u * 0.80f, cx, cy + u * 0.86f)
                cubicTo(cx - u * 0.86f, cy + u * 0.80f, cx - u * 0.76f, cy - u * 0.30f, cx, cy - u * 0.96f)
                close()
            }
            drawPath(outer, brush = Brush.linearGradient(listOf(Color(0xFFFF7043), Color(0xFFD50000)), Offset(cx, cy - u), Offset(cx, cy + u)))
            val inner = Path().apply {
                moveTo(cx, cy - u * 0.32f)
                cubicTo(cx + u * 0.42f, cy + u * 0.10f, cx + u * 0.44f, cy + u * 0.72f, cx, cy + u * 0.78f)
                cubicTo(cx - u * 0.44f, cy + u * 0.72f, cx - u * 0.42f, cy + u * 0.10f, cx, cy - u * 0.32f)
                close()
            }
            drawPath(inner, brush = Brush.linearGradient(listOf(Color(0xFFFFF176), Color(0xFFFF9100)), Offset(cx, cy - u * 0.3f), Offset(cx, cy + u * 0.8f)))
        }

        WordArtworkKey.SHEEP -> {
            listOf(Offset(-0.44f, 0f), Offset(0.44f, 0f), Offset(-0.28f, -0.34f), Offset(0.28f, -0.34f), Offset(0f, -0.44f), Offset(0f, 0.26f))
                .forEach { p ->
                    drawGlossySphere(Offset(cx + p.x * u, cy + p.y * u), u * 0.38f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.3f)
                }
            drawOval(Color(0xFF3E2723), Offset(cx - u * 0.30f, cy - u * 0.24f), Size(u * 0.60f, u * 0.70f))
            drawCircle(Color.White, u * 0.07f, Offset(cx - u * 0.14f, cy - u * 0.04f))
            drawCircle(Color.White, u * 0.07f, Offset(cx + u * 0.14f, cy - u * 0.04f))
        }

        WordArtworkKey.DUCK -> {
            drawGlossySphere(Offset(cx - u * 0.16f, cy + u * 0.22f), u * 0.56f, Color(0xFFFFEE58), Color(0xFFFBC02D))
            drawGlossySphere(Offset(cx + u * 0.34f, cy - u * 0.26f), u * 0.36f, Color(0xFFFFF176), Color(0xFFFBC02D))
            val bill = Path().apply {
                moveTo(cx + u * 0.62f, cy - u * 0.30f)
                lineTo(cx + u * 0.98f, cy - u * 0.20f)
                lineTo(cx + u * 0.62f, cy - u * 0.10f)
                close()
            }
            drawPath(bill, color = Color(0xFFFF7043))
            drawCircle(Color(0xFF212121), u * 0.06f, Offset(cx + u * 0.42f, cy - u * 0.32f))
        }

        WordArtworkKey.MOUSE -> {
            drawCircle(Color(0xFF90A4AE), u * 0.30f, Offset(cx - u * 0.44f, cy - u * 0.38f))
            drawCircle(Color(0xFFF48FB1), u * 0.18f, Offset(cx - u * 0.44f, cy - u * 0.38f))
            drawCircle(Color(0xFF90A4AE), u * 0.30f, Offset(cx + u * 0.44f, cy - u * 0.38f))
            drawCircle(Color(0xFFF48FB1), u * 0.18f, Offset(cx + u * 0.44f, cy - u * 0.38f))
            drawGlossySphere(Offset(cx, cy + u * 0.12f), u * 0.60f, Color(0xFFCFD8DC), Color(0xFF78909C), 0.35f)
            drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx - u * 0.20f, cy + u * 0.04f))
            drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx + u * 0.20f, cy + u * 0.04f))
            drawCircle(Color(0xFFF06292), u * 0.10f, Offset(cx, cy + u * 0.26f))
        }

        WordArtworkKey.TURTLE -> {
            val shell = Path().apply {
                moveTo(cx - u * 0.74f, cy + u * 0.34f)
                cubicTo(cx - u * 0.72f, cy - u * 0.56f, cx + u * 0.56f, cy - u * 0.56f, cx + u * 0.58f, cy + u * 0.34f)
                close()
            }
            drawPath(shell, brush = Brush.radialGradient(listOf(Color(0xFF66BB6A), Color(0xFF1B5E20)), Offset(cx - u * 0.1f, cy - u * 0.1f), u))
            drawGlossySphere(Offset(cx + u * 0.68f, cy + u * 0.10f), u * 0.26f, Color(0xFF9CCC65), Color(0xFF558B2F))
            drawRoundRect(Color(0xFF558B2F), Offset(cx - u * 0.56f, cy + u * 0.32f), Size(u * 0.24f, u * 0.36f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(Color(0xFF558B2F), Offset(cx + u * 0.18f, cy + u * 0.32f), Size(u * 0.24f, u * 0.36f), CornerRadius(u * 0.10f, u * 0.10f))
        }

        WordArtworkKey.PARROT -> {
            drawGlossySphere(Offset(cx - u * 0.08f, cy + u * 0.16f), u * 0.54f, Color(0xFFFF5252), Color(0xFFC62828))
            drawGlossySphere(Offset(cx + u * 0.24f, cy - u * 0.30f), u * 0.36f, Color(0xFFFF7043), Color(0xFFD84315))
            drawOval(Color(0xFF4CAF50), Offset(cx - u * 0.44f, cy - u * 0.04f), Size(u * 0.52f, u * 0.72f))
            drawCircle(Color(0xFFFFCA28), u * 0.20f, Offset(cx + u * 0.58f, cy - u * 0.24f))
            drawCircle(Color(0xFF212121), u * 0.06f, Offset(cx + u * 0.34f, cy - u * 0.34f))
        }

        WordArtworkKey.LION -> {
            drawGlossySphere(Offset(cx, cy), u * 0.84f, Color(0xFFE65100), Color(0xFF8D3300), 0.2f)
            drawGlossySphere(Offset(cx, cy + u * 0.04f), u * 0.56f, Color(0xFFFFCA28), Color(0xFFF57C00), 0.35f)
            drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx - u * 0.20f, cy - u * 0.06f))
            drawCircle(Color(0xFF212121), u * 0.07f, Offset(cx + u * 0.20f, cy - u * 0.06f))
            drawOval(Color(0xFF5D4037), Offset(cx - u * 0.12f, cy + u * 0.08f), Size(u * 0.24f, u * 0.16f))
        }

        WordArtworkKey.ELEPHANT -> {
            drawOval(Color(0xFF78909C), Offset(cx - u * 0.90f, cy - u * 0.54f), Size(u * 0.52f, u * 0.88f))
            drawOval(Color(0xFF78909C), Offset(cx + u * 0.38f, cy - u * 0.54f), Size(u * 0.52f, u * 0.88f))
            drawGlossySphere(Offset(cx, cy - u * 0.08f), u * 0.60f, Color(0xFFB0BEC5), Color(0xFF607D8B), 0.35f)
            drawRoundRect(Color(0xFF78909C), Offset(cx - u * 0.12f, cy + u * 0.18f), Size(u * 0.24f, u * 0.66f), CornerRadius(u * 0.12f, u * 0.12f))
            drawCircle(Color(0xFF212121), u * 0.065f, Offset(cx - u * 0.22f, cy - u * 0.16f))
            drawCircle(Color(0xFF212121), u * 0.065f, Offset(cx + u * 0.22f, cy - u * 0.16f))
        }

        WordArtworkKey.BEAR -> {
            drawCircle(Color(0xFF6D4C41), u * 0.26f, Offset(cx - u * 0.48f, cy - u * 0.50f))
            drawCircle(Color(0xFF6D4C41), u * 0.26f, Offset(cx + u * 0.48f, cy - u * 0.50f))
            drawGlossySphere(Offset(cx, cy + u * 0.04f), u * 0.70f, Color(0xFFA1887F), Color(0xFF5D4037), 0.3f)
            drawOval(Color(0xFFFFE0B2), Offset(cx - u * 0.30f, cy + u * 0.04f), Size(u * 0.60f, u * 0.46f))
            drawCircle(Color(0xFF212121), u * 0.075f, Offset(cx - u * 0.24f, cy - u * 0.12f))
            drawCircle(Color(0xFF212121), u * 0.075f, Offset(cx + u * 0.24f, cy - u * 0.12f))
            drawOval(Color(0xFF3E2723), Offset(cx - u * 0.13f, cy + u * 0.08f), Size(u * 0.26f, u * 0.18f))
        }

        WordArtworkKey.LIGHTNING -> {
            val bolt = Path().apply {
                moveTo(cx + u * 0.18f, cy - u * 0.90f)
                lineTo(cx - u * 0.42f, cy + u * 0.06f)
                lineTo(cx + u * 0.02f, cy + u * 0.06f)
                lineTo(cx - u * 0.22f, cy + u * 0.90f)
                lineTo(cx + u * 0.46f, cy - u * 0.12f)
                lineTo(cx + u * 0.02f, cy - u * 0.12f)
                close()
            }
            drawPath(bolt, brush = Brush.linearGradient(listOf(Color(0xFFFFF176), Color(0xFFFFA000)), Offset(cx, cy - u), Offset(cx, cy + u)))
        }

        WordArtworkKey.WIND_SWIRL -> {
            val stroke = Stroke(width = (u * 0.12f).coerceAtLeast(4f), cap = StrokeCap.Round)
            drawLine(Color(0xFF4FC3F7), Offset(cx - u * 0.78f, cy - u * 0.24f), Offset(cx + u * 0.34f, cy - u * 0.24f), stroke.width, StrokeCap.Round)
            drawArc(Color(0xFF4FC3F7), -90f, 220f, false, Offset(cx + u * 0.10f, cy - u * 0.54f), Size(u * 0.48f, u * 0.30f), style = stroke)
            drawLine(Color(0xFF29B6F6), Offset(cx - u * 0.62f, cy + u * 0.12f), Offset(cx + u * 0.68f, cy + u * 0.12f), stroke.width, StrokeCap.Round)
            drawLine(Color(0xFF81D4FA), Offset(cx - u * 0.44f, cy + u * 0.48f), Offset(cx + u * 0.26f, cy + u * 0.48f), stroke.width, StrokeCap.Round)
        }

        WordArtworkKey.DROPLET -> {
            val drop = Path().apply {
                moveTo(cx, cy - u * 0.88f)
                cubicTo(cx + u * 0.78f, cy + u * 0.06f, cx + u * 0.66f, cy + u * 0.86f, cx, cy + u * 0.86f)
                cubicTo(cx - u * 0.66f, cy + u * 0.86f, cx - u * 0.78f, cy + u * 0.06f, cx, cy - u * 0.88f)
                close()
            }
            drawPath(drop, brush = Brush.radialGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0277BD)), Offset(cx - u * 0.16f, cy + u * 0.1f), u * 1.2f))
        }

        else -> return false
    }
    return true
}
