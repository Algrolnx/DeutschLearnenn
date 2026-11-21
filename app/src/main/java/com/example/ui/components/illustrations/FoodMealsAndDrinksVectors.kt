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

internal fun DrawScope.drawFoodMealsAndDrinksVector(key: WordArtworkKey): Boolean {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.BREAD, WordArtworkKey.ROLL -> {
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFFE0A15E), Color(0xFF9C5A25)), Offset(cx - u * 0.2f, cy - u * 0.2f), u * 1.3f),
                topLeft = Offset(cx - u * 0.90f, cy - u * 0.52f),
                size = Size(u * 1.80f, u * 1.06f)
            )
            listOf(-0.42f, 0f, 0.42f).forEach { x ->
                drawLine(Color(0xFFFFE0B2), Offset(cx + (x - 0.10f) * u, cy - u * 0.32f), Offset(cx + (x + 0.14f) * u, cy - u * 0.08f), u * 0.08f, StrokeCap.Round)
            }
        }

        WordArtworkKey.CROISSANT -> {
            val croissant = Path().apply {
                moveTo(cx - u * 0.95f, cy + u * 0.30f)
                cubicTo(cx - u * 0.72f, cy - u * 0.72f, cx + u * 0.72f, cy - u * 0.72f, cx + u * 0.95f, cy + u * 0.30f)
                cubicTo(cx + u * 0.45f, cy - u * 0.04f, cx - u * 0.45f, cy - u * 0.04f, cx - u * 0.95f, cy + u * 0.30f)
                close()
            }
            drawPath(croissant, brush = Brush.linearGradient(listOf(Color(0xFFFFCA28), Color(0xFFD97706)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.3f)))
        }

        WordArtworkKey.CHEESE -> {
            val wedge = Path().apply {
                moveTo(cx - u * 0.86f, cy + u * 0.45f)
                lineTo(cx + u * 0.86f, cy + u * 0.45f)
                lineTo(cx + u * 0.86f, cy - u * 0.30f)
                lineTo(cx - u * 0.20f, cy - u * 0.62f)
                close()
            }
            drawPath(wedge, brush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFFBC02D)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.5f)))
            drawCircle(Color(0xFFE6A100), u * 0.14f, Offset(cx - u * 0.15f, cy + u * 0.12f))
            drawCircle(Color(0xFFE6A100), u * 0.10f, Offset(cx + u * 0.38f, cy + u * 0.18f))
            drawCircle(Color(0xFFE6A100), u * 0.08f, Offset(cx + u * 0.18f, cy - u * 0.16f))
        }

        WordArtworkKey.BUTTER -> {
            drawRoundRect(Color(0xFFE2E8F0), Offset(cx - u * 0.95f, cy + u * 0.36f), Size(u * 1.90f, u * 0.24f), CornerRadius(u * 0.12f, u * 0.12f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFDD835)), Offset(cx, cy - u * 0.3f), Offset(cx, cy + u * 0.3f)),
                topLeft = Offset(cx - u * 0.68f, cy - u * 0.32f),
                size = Size(u * 1.36f, u * 0.68f),
                cornerRadius = CornerRadius(u * 0.12f, u * 0.12f)
            )
        }

        WordArtworkKey.EGG -> {
            drawOval(Color(0xFFFFFBF2), Offset(cx - u * 0.88f, cy - u * 0.55f), Size(u * 1.76f, u * 1.24f))
            drawGlossySphere(Offset(cx, cy + u * 0.08f), u * 0.38f, Color(0xFFFFCA28), Color(0xFFF57C00))
        }

        WordArtworkKey.MILK, WordArtworkKey.WATER, WordArtworkKey.JUICE -> {
            val isMilk = key == WordArtworkKey.MILK
            val liquidTop = when (key) {
                WordArtworkKey.MILK -> Color(0xFFFFFFFF)
                WordArtworkKey.JUICE -> Color(0xFFFFB74D)
                else -> Color(0xFF4FC3F7)
            }
            val liquidBot = when (key) {
                WordArtworkKey.MILK -> Color(0xFFE3F2FD)
                WordArtworkKey.JUICE -> Color(0xFFF57C00)
                else -> Color(0xFF0288D1)
            }
            val glass = Path().apply {
                moveTo(cx - u * 0.52f, cy - u * 0.72f)
                lineTo(cx + u * 0.52f, cy - u * 0.72f)
                lineTo(cx + u * 0.38f, cy + u * 0.80f)
                lineTo(cx - u * 0.38f, cy + u * 0.80f)
                close()
            }
            drawPath(glass, color = Color(0xFFB3E5FC).copy(alpha = 0.40f))
            val fill = Path().apply {
                moveTo(cx - u * 0.46f, cy - u * 0.24f)
                lineTo(cx + u * 0.46f, cy - u * 0.24f)
                lineTo(cx + u * 0.36f, cy + u * 0.75f)
                lineTo(cx - u * 0.36f, cy + u * 0.75f)
                close()
            }
            drawPath(fill, brush = Brush.linearGradient(listOf(liquidTop, liquidBot), Offset(cx, cy - u * 0.2f), Offset(cx, cy + u * 0.8f)))
            drawRoundStrokeOutline(glass, if (isMilk) Color(0xFF90CAF9) else Color(0xFF81D4FA), 0.02f)
        }

        WordArtworkKey.COFFEE, WordArtworkKey.TEA -> {
            val isTea = key == WordArtworkKey.TEA
            drawCircle(
                color = if (isTea) Color(0xFF66BB6A) else Color(0xFF8D6E63),
                radius = u * 0.28f,
                center = Offset(cx + u * 0.58f, cy + u * 0.08f),
                style = Stroke(width = u * 0.14f)
            )
            val cup = Path().apply {
                moveTo(cx - u * 0.62f, cy - u * 0.28f)
                lineTo(cx + u * 0.56f, cy - u * 0.28f)
                cubicTo(cx + u * 0.52f, cy + u * 0.76f, cx - u * 0.58f, cy + u * 0.76f, cx - u * 0.62f, cy - u * 0.28f)
                close()
            }
            drawPath(
                cup,
                brush = Brush.linearGradient(
                    if (isTea) listOf(Color(0xFFA5D6A7), Color(0xFF43A047)) else listOf(Color(0xFFBCAAA4), Color(0xFF5D4037)),
                    Offset(cx - u * 0.5f, cy),
                    Offset(cx + u * 0.5f, cy + u * 0.6f)
                )
            )
            // Steam wisps
            listOf(-0.22f, 0.14f).forEach { dx ->
                drawLine(Color(0xFFB0BEC5), Offset(cx + dx * u, cy - u * 0.44f), Offset(cx + (dx + 0.08f) * u, cy - u * 0.76f), u * 0.06f, StrokeCap.Round)
            }
        }

        WordArtworkKey.BEER -> {
            drawRoundRect(Color(0xFFFFB300), Offset(cx - u * 0.48f, cy - u * 0.40f), Size(u * 0.88f, u * 1.15f), CornerRadius(u * 0.14f, u * 0.14f))
            drawCircle(Color.White, u * 0.26f, Offset(cx - u * 0.24f, cy - u * 0.48f))
            drawCircle(Color.White, u * 0.28f, Offset(cx + u * 0.06f, cy - u * 0.52f))
            drawCircle(Color.White, u * 0.24f, Offset(cx + u * 0.30f, cy - u * 0.44f))
        }

        WordArtworkKey.WINE -> {
            val bowl = Path().apply {
                moveTo(cx - u * 0.44f, cy - u * 0.76f)
                lineTo(cx + u * 0.44f, cy - u * 0.76f)
                cubicTo(cx + u * 0.46f, cy + u * 0.12f, cx - u * 0.46f, cy + u * 0.12f, cx - u * 0.44f, cy - u * 0.76f)
                close()
            }
            drawPath(bowl, color = Color(0xFFE3F2FD).copy(alpha = 0.7f))
            val wine = Path().apply {
                moveTo(cx - u * 0.38f, cy - u * 0.36f)
                lineTo(cx + u * 0.38f, cy - u * 0.36f)
                cubicTo(cx + u * 0.36f, cy + u * 0.08f, cx - u * 0.36f, cy + u * 0.08f, cx - u * 0.38f, cy - u * 0.36f)
                close()
            }
            drawPath(wine, color = Color(0xFF9E0031))
            drawLine(Color(0xFF90A4AE), Offset(cx, cy + u * 0.02f), Offset(cx, cy + u * 0.78f), u * 0.08f, StrokeCap.Round)
            drawOval(Color(0xFF90A4AE), Offset(cx - u * 0.36f, cy + u * 0.74f), Size(u * 0.72f, u * 0.12f))
        }

        WordArtworkKey.CAKE -> {
            val slice = Path().apply {
                moveTo(cx - u * 0.76f, cy + u * 0.62f)
                lineTo(cx + u * 0.76f, cy + u * 0.62f)
                lineTo(cx + u * 0.76f, cy - u * 0.16f)
                lineTo(cx - u * 0.56f, cy - u * 0.52f)
                close()
            }
            drawPath(slice, color = Color(0xFFFFD54F))
            drawRoundRect(Color(0xFFF48FB1), Offset(cx - u * 0.64f, cy + u * 0.10f), Size(u * 1.40f, u * 0.14f), CornerRadius(u * 0.06f, u * 0.06f))
            drawGlossySphere(Offset(cx, cy - u * 0.58f), u * 0.22f, Color(0xFFFF5252), Color(0xFFC62828))
        }

        WordArtworkKey.ICECREAM -> {
            val cone = Path().apply {
                moveTo(cx - u * 0.44f, cy - u * 0.05f)
                lineTo(cx + u * 0.44f, cy - u * 0.05f)
                lineTo(cx, cy + u * 0.94f)
                close()
            }
            drawPath(cone, color = Color(0xFFE09F5A))
            drawGlossySphere(Offset(cx - u * 0.24f, cy - u * 0.24f), u * 0.36f, Color(0xFFFF80AB), Color(0xFFD81B60))
            drawGlossySphere(Offset(cx + u * 0.22f, cy - u * 0.22f), u * 0.34f, Color(0xFFFFF59D), Color(0xFFFBC02D))
            drawGlossySphere(Offset(cx, cy - u * 0.54f), u * 0.34f, Color(0xFF81D4FA), Color(0xFF0288D1))
        }

        WordArtworkKey.CHOCOLATE -> {
            drawRoundRect(Color(0xFF5D4037), Offset(cx - u * 0.60f, cy - u * 0.80f), Size(u * 1.20f, u * 1.56f), CornerRadius(u * 0.12f, u * 0.12f))
            drawRoundRect(Color(0xFFD32F2F), Offset(cx - u * 0.64f, cy + u * 0.05f), Size(u * 1.28f, u * 0.78f), CornerRadius(u * 0.10f, u * 0.10f))
            for (r in 0..1) {
                for (c in 0..1) {
                    drawRoundRect(
                        Color(0xFF8D6E63),
                        Offset(cx - u * 0.46f + c * u * 0.50f, cy - u * 0.66f + r * u * 0.34f),
                        Size(u * 0.40f, u * 0.26f),
                        CornerRadius(u * 0.05f, u * 0.05f)
                    )
                }
            }
        }

        WordArtworkKey.PIZZA -> {
            val crust = Path().apply {
                moveTo(cx - u * 0.72f, cy - u * 0.62f)
                quadraticTo(cx, cy - u * 0.86f, cx + u * 0.72f, cy - u * 0.62f)
                lineTo(cx, cy + u * 0.88f)
                close()
            }
            drawPath(crust, color = Color(0xFFD97706))
            val cheese = Path().apply {
                moveTo(cx - u * 0.60f, cy - u * 0.45f)
                quadraticTo(cx, cy - u * 0.66f, cx + u * 0.60f, cy - u * 0.45f)
                lineTo(cx, cy + u * 0.82f)
                close()
            }
            drawPath(cheese, color = Color(0xFFFFD54F))
            drawCircle(Color(0xFFD32F2F), u * 0.16f, Offset(cx - u * 0.20f, cy - u * 0.25f))
            drawCircle(Color(0xFFD32F2F), u * 0.15f, Offset(cx + u * 0.20f, cy - u * 0.18f))
            drawCircle(Color(0xFFD32F2F), u * 0.14f, Offset(cx, cy + u * 0.20f))
        }

        WordArtworkKey.SOUP -> {
            val bowl = Path().apply {
                moveTo(cx - u * 0.86f, cy - u * 0.06f)
                lineTo(cx + u * 0.86f, cy - u * 0.06f)
                cubicTo(cx + u * 0.82f, cy + u * 0.78f, cx - u * 0.82f, cy + u * 0.78f, cx - u * 0.86f, cy - u * 0.06f)
                close()
            }
            drawPath(bowl, color = Color(0xFFEF5350))
            drawOval(Color(0xFFFFB74D), Offset(cx - u * 0.80f, cy - u * 0.18f), Size(u * 1.60f, u * 0.26f))
            listOf(-0.25f, 0f, 0.25f).forEach { x ->
                drawLine(Color(0xFFB0BEC5), Offset(cx + x * u, cy - u * 0.34f), Offset(cx + x * u, cy - u * 0.72f), u * 0.06f, StrokeCap.Round)
            }
        }

        WordArtworkKey.FISH_FOOD -> {
            val tail = Path().apply {
                moveTo(cx + u * 0.45f, cy)
                lineTo(cx + u * 0.92f, cy - u * 0.40f)
                lineTo(cx + u * 0.92f, cy + u * 0.40f)
                close()
            }
            drawPath(tail, color = Color(0xFF0288D1))
            drawOval(
                brush = Brush.linearGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0277BD)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.4f)),
                topLeft = Offset(cx - u * 0.85f, cy - u * 0.42f),
                size = Size(u * 1.48f, u * 0.84f)
            )
            drawCircle(Color.White, u * 0.10f, Offset(cx - u * 0.50f, cy - u * 0.10f))
            drawCircle(Color(0xFF1A237E), u * 0.05f, Offset(cx - u * 0.52f, cy - u * 0.10f))
        }

        WordArtworkKey.MEAT, WordArtworkKey.SAUSAGE -> {
            val light = if (key == WordArtworkKey.MEAT) Color(0xFFE57373) else Color(0xFFFF8A65)
            val dark = if (key == WordArtworkKey.MEAT) Color(0xFFB71C1C) else Color(0xFFD84315)
            drawOval(
                brush = Brush.linearGradient(listOf(light, dark), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.4f)),
                topLeft = Offset(cx - u * 0.84f, cy - u * 0.44f),
                size = Size(u * 1.68f, u * 0.88f)
            )
            if (key == WordArtworkKey.MEAT) {
                drawCircle(Color.White, u * 0.18f, Offset(cx - u * 0.34f, cy - u * 0.02f))
            }
        }

        WordArtworkKey.PRETZEL -> {
            val stroke = Stroke(width = (u * 0.20f).coerceAtLeast(6f), cap = StrokeCap.Round)
            drawCircle(Color(0xFFD97706), u * 0.56f, Offset(cx, cy + u * 0.06f), style = stroke)
            drawLine(Color(0xFFD97706), Offset(cx - u * 0.44f, cy - u * 0.24f), Offset(cx + u * 0.38f, cy + u * 0.46f), stroke.width, StrokeCap.Round)
            drawLine(Color(0xFFD97706), Offset(cx + u * 0.44f, cy - u * 0.24f), Offset(cx - u * 0.38f, cy + u * 0.46f), stroke.width, StrokeCap.Round)
            listOf(Offset(-0.34f, -0.34f), Offset(0.32f, -0.32f), Offset(0f, 0.56f), Offset(-0.48f, 0.10f)).forEach { p ->
                drawCircle(Color.White, u * 0.045f, Offset(cx + p.x * u, cy + p.y * u))
            }
        }

        WordArtworkKey.DONUT -> {
            drawGlossySphere(Offset(cx, cy), u * 0.78f, Color(0xFFFFCC80), Color(0xFFD97706), 0.35f)
            drawGlossySphere(Offset(cx, cy - u * 0.04f), u * 0.70f, Color(0xFFFF80AB), Color(0xFFD81B60), 0.45f)
            drawCircle(Color.White.copy(alpha = 0.92f), u * 0.24f, Offset(cx, cy))
            listOf(Offset(-0.38f, -0.24f), Offset(0.34f, -0.28f), Offset(-0.40f, 0.26f), Offset(0.38f, 0.24f), Offset(0f, -0.48f))
                .forEachIndexed { idx, p ->
                    val c = if (idx % 2 == 0) Color(0xFFFFEE58) else Color(0xFF4FC3F7)
                    drawCircle(c, u * 0.06f, Offset(cx + p.x * u, cy + p.y * u))
                }
        }

        WordArtworkKey.MUFFIN -> {
            val base = Path().apply {
                moveTo(cx - u * 0.56f, cy + u * 0.04f)
                lineTo(cx + u * 0.56f, cy + u * 0.04f)
                lineTo(cx + u * 0.42f, cy + u * 0.78f)
                lineTo(cx - u * 0.42f, cy + u * 0.78f)
                close()
            }
            drawPath(base, color = Color(0xFF5C6BC0))
            drawGlossySphere(Offset(cx - u * 0.24f, cy - u * 0.14f), u * 0.38f, Color(0xFFFFCC80), Color(0xFFC67128), 0.35f)
            drawGlossySphere(Offset(cx + u * 0.24f, cy - u * 0.14f), u * 0.38f, Color(0xFFFFCC80), Color(0xFFC67128), 0.35f)
            drawGlossySphere(Offset(cx, cy - u * 0.36f), u * 0.44f, Color(0xFFFFE0B2), Color(0xFFD97706), 0.45f)
            drawGlossySphere(Offset(cx, cy - u * 0.72f), u * 0.16f, Color(0xFFFF5252), Color(0xFFC62828))
        }

        WordArtworkKey.HONEY -> {
            drawRoundRect(
                brush = Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57F17)), Offset(cx - u * 0.2f, cy), u * 1.2f),
                topLeft = Offset(cx - u * 0.62f, cy - u * 0.44f),
                size = Size(u * 1.24f, u * 1.18f),
                cornerRadius = CornerRadius(u * 0.34f, u * 0.34f)
            )
            drawRoundRect(Color(0xFF6D4C41), Offset(cx - u * 0.44f, cy - u * 0.72f), Size(u * 0.88f, u * 0.30f), CornerRadius(u * 0.10f, u * 0.10f))
            drawRoundRect(Color(0xFFFFF8E1), Offset(cx - u * 0.38f, cy - u * 0.04f), Size(u * 0.76f, u * 0.44f), CornerRadius(u * 0.10f, u * 0.10f))
        }

        WordArtworkKey.COOKIE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.78f, Color(0xFFE6AC63), Color(0xFF9C5A25), 0.25f)
            listOf(Offset(-0.34f, -0.24f), Offset(0.24f, -0.34f), Offset(0.02f, 0.02f), Offset(-0.28f, 0.32f), Offset(0.34f, 0.24f))
                .forEach { p ->
                    drawCircle(Color(0xFF4E342E), u * 0.11f, Offset(cx + p.x * u, cy + p.y * u))
                }
        }

        WordArtworkKey.CANDY -> {
            rotate(-28f, Offset(cx, cy)) {
                val wrapL = Path().apply {
                    moveTo(cx - u * 0.44f, cy)
                    lineTo(cx - u * 0.88f, cy - u * 0.34f)
                    lineTo(cx - u * 0.88f, cy + u * 0.34f)
                    close()
                }
                val wrapR = Path().apply {
                    moveTo(cx + u * 0.44f, cy)
                    lineTo(cx + u * 0.88f, cy - u * 0.34f)
                    lineTo(cx + u * 0.88f, cy + u * 0.34f)
                    close()
                }
                drawPath(wrapL, color = Color(0xFFFF4081))
                drawPath(wrapR, color = Color(0xFFFF4081))
                drawGlossySphere(Offset(cx, cy), u * 0.50f, Color(0xFFFF80AB), Color(0xFFD81B60))
            }
        }

        WordArtworkKey.CHICKEN_LEG -> {
            rotate(-30f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFFFFF8E1), Offset(cx + u * 0.08f, cy - u * 0.12f), Size(u * 0.72f, u * 0.24f), CornerRadius(u * 0.12f, u * 0.12f))
                drawCircle(Color(0xFFFFF8E1), u * 0.16f, Offset(cx + u * 0.78f, cy - u * 0.12f))
                drawCircle(Color(0xFFFFF8E1), u * 0.16f, Offset(cx + u * 0.78f, cy + u * 0.12f))
                drawGlossySphere(Offset(cx - u * 0.24f, cy), u * 0.52f, Color(0xFFFFAB40), Color(0xFFC65102), 0.35f)
            }
        }

        WordArtworkKey.SHRIMP -> {
            drawArc(
                brush = Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFFE64A19)), Offset(cx - u, cy), Offset(cx + u, cy)),
                startAngle = 160f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(cx - u * 0.68f, cy - u * 0.60f),
                size = Size(u * 1.36f, u * 1.24f),
                style = Stroke(width = u * 0.38f, cap = StrokeCap.Round)
            )
            drawCircle(Color(0xFF212121), u * 0.06f, Offset(cx - u * 0.44f, cy - u * 0.30f))
        }

        WordArtworkKey.NOODLES -> {
            val bowl = Path().apply {
                moveTo(cx - u * 0.86f, cy - u * 0.02f)
                lineTo(cx + u * 0.86f, cy - u * 0.02f)
                cubicTo(cx + u * 0.82f, cy + u * 0.78f, cx - u * 0.82f, cy + u * 0.78f, cx - u * 0.86f, cy - u * 0.02f)
                close()
            }
            drawPath(bowl, color = Color(0xFF1E88E5))
            listOf(-0.24f, -0.06f, 0.12f).forEach { dy ->
                drawArc(Color(0xFFFFD54F), 180f, 180f, false, Offset(cx - u * 0.66f, cy + (dy - 0.32f) * u), Size(u * 1.32f, u * 0.46f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
            }
            drawLine(Color(0xFF795548), Offset(cx - u * 0.30f, cy - u * 0.22f), Offset(cx + u * 0.88f, cy - u * 0.76f), u * 0.07f, StrokeCap.Round)
            drawLine(Color(0xFF795548), Offset(cx - u * 0.20f, cy - u * 0.10f), Offset(cx + u * 0.94f, cy - u * 0.56f), u * 0.07f, StrokeCap.Round)
        }

        else -> return false
    }
    return true
}
