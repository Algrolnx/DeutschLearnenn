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

internal fun DrawScope.drawFoodProduceVector(key: WordArtworkKey): Boolean {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.APPLE -> {
            drawStemAndLeaf(
                stemBase = Offset(cx, cy - u * 0.72f),
                stemTop = Offset(cx - u * 0.04f, cy - u * 1.15f),
                leafRight = false,
                scale = 0.95f
            )
            val apple = Path().apply {
                moveTo(cx, cy - u * 0.72f)
                cubicTo(cx - u * 0.25f, cy - u * 0.96f, cx - u * 1.04f, cy - u * 0.78f, cx - u * 0.94f, cy - u * 0.02f)
                cubicTo(cx - u * 0.88f, cy + u * 0.62f, cx - u * 0.42f, cy + u * 0.98f, cx - u * 0.14f, cy + u * 0.92f)
                quadraticTo(cx, cy + u * 0.84f, cx + u * 0.14f, cy + u * 0.92f)
                cubicTo(cx + u * 0.42f, cy + u * 0.98f, cx + u * 0.88f, cy + u * 0.62f, cx + u * 0.94f, cy - u * 0.02f)
                cubicTo(cx + u * 1.04f, cy - u * 0.78f, cx + u * 0.25f, cy - u * 0.96f, cx, cy - u * 0.72f)
                close()
            }
            drawPath(
                apple,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF3333), Color(0xFFD90429), Color(0xFF9E001A)),
                    center = Offset(cx + u * 0.22f, cy - u * 0.22f),
                    radius = u * 1.45f
                )
            )
            // Specular highlight arc on upper right
            val shine = Path().apply {
                moveTo(cx + u * 0.46f, cy - u * 0.56f)
                cubicTo(cx + u * 0.70f, cy - u * 0.45f, cx + u * 0.78f, cy - u * 0.12f, cx + u * 0.72f, cy + u * 0.08f)
                cubicTo(cx + u * 0.71f, cy - u * 0.12f, cx + u * 0.60f, cy - u * 0.36f, cx + u * 0.46f, cy - u * 0.56f)
                close()
            }
            drawPath(shine, color = Color.White.copy(alpha = 0.78f))
            // Calyx notch
            drawLine(
                color = Color(0xFF4A2511),
                start = Offset(cx - u * 0.06f, cy + u * 0.90f),
                end = Offset(cx, cy + u * 0.99f),
                strokeWidth = u * 0.055f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF4A2511),
                start = Offset(cx + u * 0.06f, cy + u * 0.90f),
                end = Offset(cx, cy + u * 0.99f),
                strokeWidth = u * 0.055f,
                cap = StrokeCap.Round
            )
        }

        WordArtworkKey.BANANA -> {
            val backBanana = Path().apply {
                moveTo(cx - u * 0.85f, cy + u * 0.22f)
                cubicTo(cx - u * 0.35f, cy + u * 0.62f, cx + u * 0.58f, cy + u * 0.35f, cx + u * 0.76f, cy - u * 0.72f)
                lineTo(cx + u * 0.92f, cy - u * 0.62f)
                cubicTo(cx + u * 0.74f, cy + u * 0.68f, cx - u * 0.40f, cy + u * 0.98f, cx - u * 0.95f, cy + u * 0.38f)
                close()
            }
            drawPath(backBanana, color = Color(0xFFE0B400))

            val frontBanana = Path().apply {
                moveTo(cx - u * 0.94f, cy + u * 0.10f)
                cubicTo(cx - u * 0.34f, cy + u * 0.52f, cx + u * 0.50f, cy + u * 0.26f, cx + u * 0.70f, cy - u * 0.78f)
                lineTo(cx + u * 0.86f, cy - u * 0.70f)
                cubicTo(cx + u * 0.70f, cy + u * 0.54f, cx - u * 0.36f, cy + u * 0.88f, cx - u * 0.98f, cy + u * 0.25f)
                close()
            }
            drawPath(
                frontBanana,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFE6A100)),
                    start = Offset(cx - u * 0.5f, cy - u * 0.5f),
                    end = Offset(cx + u * 0.5f, cy + u * 0.7f)
                )
            )
            // Brown banana stem & tip
            drawCircle(color = Color(0xFF6D4C2D), radius = u * 0.08f, center = Offset(cx - u * 0.96f, cy + u * 0.18f))
            drawRoundRect(
                color = Color(0xFF79552D),
                topLeft = Offset(cx + u * 0.70f, cy - u * 0.90f),
                size = Size(u * 0.18f, u * 0.20f),
                cornerRadius = CornerRadius(u * 0.04f, u * 0.04f)
            )
        }

        WordArtworkKey.CHERRY -> {
            val topStem = Offset(cx, cy - u * 0.92f)
            val leftCenter = Offset(cx - u * 0.44f, cy + u * 0.36f)
            val rightCenter = Offset(cx + u * 0.42f, cy + u * 0.42f)
            val stemStroke = Stroke(width = (u * 0.08f).coerceAtLeast(3f), cap = StrokeCap.Round)
            val stemPath1 = Path().apply {
                moveTo(topStem.x, topStem.y)
                quadraticTo(cx - u * 0.28f, cy - u * 0.25f, leftCenter.x, leftCenter.y - u * 0.32f)
            }
            val stemPath2 = Path().apply {
                moveTo(topStem.x, topStem.y)
                quadraticTo(cx + u * 0.22f, cy - u * 0.20f, rightCenter.x, rightCenter.y - u * 0.32f)
            }
            drawPath(stemPath1, color = Color(0xFF4F8A27), style = stemStroke)
            drawPath(stemPath2, color = Color(0xFF4F8A27), style = stemStroke)
            drawStemAndLeaf(topStem, Offset(cx + u * 0.04f, cy - u * 1.05f), leafRight = true, scale = 0.85f)
            drawGlossySphere(leftCenter, u * 0.46f, Color(0xFFFF3B4E), Color(0xFFA4061E))
            drawGlossySphere(rightCenter, u * 0.46f, Color(0xFFFF2640), Color(0xFF8C0419))
        }

        WordArtworkKey.PEACH -> {
            drawStemAndLeaf(
                stemBase = Offset(cx, cy - u * 0.68f),
                stemTop = Offset(cx, cy - u * 1.02f),
                leafRight = true,
                scale = 0.95f
            )
            drawGlossySphere(
                center = Offset(cx, cy + u * 0.06f),
                radius = u * 0.84f,
                lightColor = Color(0xFFFFB76B),
                darkColor = Color(0xFFE6513C)
            )
            val cleft = Path().apply {
                moveTo(cx, cy - u * 0.72f)
                cubicTo(cx + u * 0.24f, cy - u * 0.15f, cx + u * 0.14f, cy + u * 0.48f, cx, cy + u * 0.86f)
            }
            drawPath(
                cleft,
                color = Color(0xFFB72D20).copy(alpha = 0.55f),
                style = Stroke(width = (u * 0.055f).coerceAtLeast(2.5f), cap = StrokeCap.Round)
            )
        }

        WordArtworkKey.PEAR -> {
            drawStemAndLeaf(
                stemBase = Offset(cx, cy - u * 0.80f),
                stemTop = Offset(cx + u * 0.04f, cy - u * 1.12f),
                leafRight = true,
                scale = 0.85f
            )
            val pear = Path().apply {
                moveTo(cx, cy - u * 0.82f)
                cubicTo(cx + u * 0.36f, cy - u * 0.82f, cx + u * 0.40f, cy - u * 0.20f, cx + u * 0.66f, cy + u * 0.12f)
                cubicTo(cx + u * 0.86f, cy + u * 0.42f, cx + u * 0.68f, cy + u * 0.94f, cx, cy + u * 0.94f)
                cubicTo(cx - u * 0.68f, cy + u * 0.94f, cx - u * 0.86f, cy + u * 0.42f, cx - u * 0.66f, cy + u * 0.12f)
                cubicTo(cx - u * 0.40f, cy - u * 0.20f, cx - u * 0.36f, cy - u * 0.82f, cx, cy - u * 0.82f)
                close()
            }
            drawPath(
                pear,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDCE775), Color(0xFF9CCC65), Color(0xFF689F38)),
                    center = Offset(cx - u * 0.2f, cy - u * 0.1f),
                    radius = u * 1.4f
                )
            )
            rotate(-22f, Offset(cx - u * 0.34f, cy + u * 0.25f)) {
                drawOval(
                    color = Color.White.copy(alpha = 0.62f),
                    topLeft = Offset(cx - u * 0.48f, cy + u * 0.08f),
                    size = Size(u * 0.22f, u * 0.38f)
                )
            }
        }

        WordArtworkKey.STRAWBERRY -> {
            val berry = Path().apply {
                moveTo(cx, cy - u * 0.68f)
                cubicTo(cx + u * 0.92f, cy - u * 0.68f, cx + u * 0.86f, cy + u * 0.18f, cx, cy + u * 0.96f)
                cubicTo(cx - u * 0.86f, cy + u * 0.18f, cx - u * 0.92f, cy - u * 0.68f, cx, cy - u * 0.68f)
                close()
            }
            drawPath(
                berry,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF4D5A), Color(0xFFD90429), Color(0xFF9B001C)),
                    center = Offset(cx - u * 0.22f, cy - u * 0.22f),
                    radius = u * 1.45f
                )
            )
            // Golden seeds
            val seedPts = listOf(
                Offset(-0.34f, -0.22f), Offset(0f, -0.28f), Offset(0.34f, -0.22f),
                Offset(-0.42f, 0.06f), Offset(-0.14f, 0.04f), Offset(0.16f, 0.04f), Offset(0.40f, 0.08f),
                Offset(-0.24f, 0.34f), Offset(0.02f, 0.38f), Offset(0.24f, 0.34f),
                Offset(0f, 0.64f)
            )
            seedPts.forEach { p ->
                drawOval(
                    color = Color(0xFFFFE082),
                    topLeft = Offset(cx + p.x * u - u * 0.03f, cy + p.y * u - u * 0.045f),
                    size = Size(u * 0.06f, u * 0.09f)
                )
            }
            // Calyx crown leaves
            val calyx = Path().apply {
                moveTo(cx - u * 0.56f, cy - u * 0.62f)
                lineTo(cx - u * 0.22f, cy - u * 0.50f)
                lineTo(cx - u * 0.34f, cy - u * 0.82f)
                lineTo(cx, cy - u * 0.58f)
                lineTo(cx + u * 0.34f, cy - u * 0.82f)
                lineTo(cx + u * 0.22f, cy - u * 0.50f)
                lineTo(cx + u * 0.56f, cy - u * 0.62f)
                lineTo(cx, cy - u * 0.34f)
                close()
            }
            drawPath(calyx, color = Color(0xFF4CAF50))
            drawLine(
                color = Color(0xFF2E7D32),
                start = Offset(cx, cy - u * 0.68f),
                end = Offset(cx, cy - u * 1.02f),
                strokeWidth = u * 0.08f,
                cap = StrokeCap.Round
            )
        }

        WordArtworkKey.BLUEBERRY, WordArtworkKey.CURRANT, WordArtworkKey.BERRY_MIX -> {
            val isRed = key == WordArtworkKey.CURRANT
            val c1Light = if (isRed) Color(0xFFFF5252) else Color(0xFF5C7CFA)
            val c1Dark = if (isRed) Color(0xFFB71C1C) else Color(0xFF2B3A8C)
            val c2Light = if (key == WordArtworkKey.BERRY_MIX) Color(0xFFFF4D6D) else c1Light
            val c2Dark = if (key == WordArtworkKey.BERRY_MIX) Color(0xFFA4133C) else c1Dark
            drawGlossySphere(Offset(cx - u * 0.36f, cy - u * 0.16f), u * 0.44f, c1Light, c1Dark)
            drawGlossySphere(Offset(cx + u * 0.36f, cy - u * 0.10f), u * 0.42f, c2Light, c2Dark)
            drawGlossySphere(Offset(cx, cy + u * 0.30f), u * 0.50f, c1Light, c1Dark)
            // Crown star on front berry
            drawCircle(color = Color(0xFF1A1E3F), radius = u * 0.11f, center = Offset(cx, cy - u * 0.08f))
            drawStemAndLeaf(Offset(cx, cy - u * 0.45f), Offset(cx, cy - u * 0.85f), leafRight = true, scale = 0.78f)
        }

        WordArtworkKey.RASPBERRY -> {
            val drupes = listOf(
                Offset(-0.28f, -0.45f), Offset(0f, -0.48f), Offset(0.28f, -0.45f),
                Offset(-0.42f, -0.15f), Offset(-0.14f, -0.18f), Offset(0.14f, -0.18f), Offset(0.42f, -0.15f),
                Offset(-0.36f, 0.18f), Offset(-0.10f, 0.16f), Offset(0.12f, 0.16f), Offset(0.36f, 0.18f),
                Offset(-0.18f, 0.46f), Offset(0.18f, 0.46f), Offset(0f, 0.66f)
            )
            drupes.forEach { d ->
                drawGlossySphere(
                    Offset(cx + d.x * u, cy + d.y * u),
                    u * 0.24f,
                    Color(0xFFFF4D79),
                    Color(0xFFC2185B),
                    highlightAlpha = 0.55f
                )
            }
            drawStemAndLeaf(Offset(cx, cy - u * 0.65f), Offset(cx, cy - u * 1.0f), leafRight = true, scale = 0.72f)
        }

        WordArtworkKey.LEMON, WordArtworkKey.LIME -> {
            val isLime = key == WordArtworkKey.LIME
            val cLight = if (isLime) Color(0xFFAEEA00) else Color(0xFFFFF176)
            val cDark = if (isLime) Color(0xFF558B2F) else Color(0xFFFBC02D)
            rotate(-22f, Offset(cx, cy)) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(cLight, cDark),
                        center = Offset(cx - u * 0.25f, cy - u * 0.25f),
                        radius = u * 1.25f
                    ),
                    topLeft = Offset(cx - u * 0.92f, cy - u * 0.62f),
                    size = Size(u * 1.84f, u * 1.24f)
                )
                drawCircle(color = cDark, radius = u * 0.16f, center = Offset(cx - u * 0.90f, cy))
                drawCircle(color = cDark, radius = u * 0.16f, center = Offset(cx + u * 0.90f, cy))
                drawOval(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(cx - u * 0.45f, cy - u * 0.44f),
                    size = Size(u * 0.55f, u * 0.18f)
                )
            }
            drawStemAndLeaf(Offset(cx + u * 0.28f, cy - u * 0.58f), Offset(cx + u * 0.35f, cy - u * 0.85f), leafRight = true, scale = 0.78f)
        }

        WordArtworkKey.ORANGE, WordArtworkKey.MANDARIN, WordArtworkKey.GRAPEFRUIT -> {
            val light = when (key) {
                WordArtworkKey.GRAPEFRUIT -> Color(0xFFFF8A65)
                WordArtworkKey.MANDARIN -> Color(0xFFFFB74D)
                else -> Color(0xFFFFA726)
            }
            val dark = when (key) {
                WordArtworkKey.GRAPEFRUIT -> Color(0xFFE64A19)
                WordArtworkKey.MANDARIN -> Color(0xFFF57C00)
                else -> Color(0xFFEF6C00)
            }
            drawStemAndLeaf(Offset(cx, cy - u * 0.78f), Offset(cx, cy - u * 1.05f), leafRight = true, scale = 0.88f)
            drawGlossySphere(Offset(cx, cy + u * 0.04f), u * 0.84f, light, dark)
        }

        WordArtworkKey.GRAPE -> {
            drawStemAndLeaf(Offset(cx, cy - u * 0.72f), Offset(cx, cy - u * 1.08f), leafRight = true, scale = 0.85f)
            val rows = listOf(
                listOf(-0.42f, -0.14f, 0.14f, 0.42f) to -0.34f,
                listOf(-0.28f, 0f, 0.28f) to -0.02f,
                listOf(-0.16f, 0.16f) to 0.30f,
                listOf(0f) to 0.62f
            )
            rows.forEach { (xs, yPos) ->
                xs.forEach { xPos ->
                    drawGlossySphere(
                        Offset(cx + xPos * u, cy + yPos * u),
                        u * 0.24f,
                        Color(0xFF9C4DCC),
                        Color(0xFF4A148C),
                        highlightAlpha = 0.60f
                    )
                }
            }
        }

        WordArtworkKey.PLUM, WordArtworkKey.FIG, WordArtworkKey.MANGO -> {
            val light = when (key) {
                WordArtworkKey.MANGO -> Color(0xFFFFCA28)
                WordArtworkKey.FIG -> Color(0xFF8E24AA)
                else -> Color(0xFFAB47BC)
            }
            val dark = when (key) {
                WordArtworkKey.MANGO -> Color(0xFFE65100)
                WordArtworkKey.FIG -> Color(0xFF4A148C)
                else -> Color(0xFF4A148C)
            }
            drawStemAndLeaf(Offset(cx, cy - u * 0.78f), Offset(cx, cy - u * 1.06f), leafRight = true, scale = 0.82f)
            drawOval(
                brush = Brush.radialGradient(listOf(light, dark), Offset(cx - u * 0.22f, cy - u * 0.22f), u * 1.3f),
                topLeft = Offset(cx - u * 0.70f, cy - u * 0.78f),
                size = Size(u * 1.40f, u * 1.62f)
            )
            rotate(-25f, Offset(cx - u * 0.28f, cy - u * 0.32f)) {
                drawOval(
                    color = Color.White.copy(alpha = 0.52f),
                    topLeft = Offset(cx - u * 0.42f, cy - u * 0.42f),
                    size = Size(u * 0.28f, u * 0.14f)
                )
            }
        }

        WordArtworkKey.WATERMELON -> {
            val rind = Path().apply {
                moveTo(cx - u * 0.92f, cy - u * 0.16f)
                quadraticTo(cx, cy + u * 1.24f, cx + u * 0.92f, cy - u * 0.16f)
                close()
            }
            drawPath(rind, color = Color(0xFF2E7D32))
            val pith = Path().apply {
                moveTo(cx - u * 0.84f, cy - u * 0.16f)
                quadraticTo(cx, cy + u * 1.06f, cx + u * 0.84f, cy - u * 0.16f)
                close()
            }
            drawPath(pith, color = Color(0xFFDCEDC8))
            val flesh = Path().apply {
                moveTo(cx - u * 0.76f, cy - u * 0.16f)
                quadraticTo(cx, cy + u * 0.92f, cx + u * 0.76f, cy - u * 0.16f)
                close()
            }
            drawPath(
                flesh,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFF5252), Color(0xFFD50000)),
                    Offset(cx, cy - u * 0.2f),
                    Offset(cx, cy + u * 0.8f)
                )
            )
            listOf(Offset(-0.34f, 0.05f), Offset(0f, 0.12f), Offset(0.34f, 0.05f), Offset(-0.18f, 0.34f), Offset(0.18f, 0.34f))
                .forEach { p ->
                    drawOval(
                        color = Color(0xFF212121),
                        topLeft = Offset(cx + p.x * u - u * 0.035f, cy + p.y * u - u * 0.055f),
                        size = Size(u * 0.07f, u * 0.11f)
                    )
                }
        }

        WordArtworkKey.MELON, WordArtworkKey.COCONUT, WordArtworkKey.POMEGRANATE, WordArtworkKey.NUT -> {
            val (light, dark) = when (key) {
                WordArtworkKey.MELON -> Color(0xFFFFE082) to Color(0xFFFBC02D)
                WordArtworkKey.COCONUT -> Color(0xFF8D6E63) to Color(0xFF4E342E)
                WordArtworkKey.POMEGRANATE -> Color(0xFFE53935) to Color(0xFF880E4F)
                else -> Color(0xFFD7A86E) to Color(0xFF795548)
            }
            drawGlossySphere(Offset(cx, cy + u * 0.05f), u * 0.80f, light, dark)
            if (key == WordArtworkKey.COCONUT) {
                drawCircle(color = Color(0xFF3E2723), radius = u * 0.09f, center = Offset(cx - u * 0.16f, cy - u * 0.32f))
                drawCircle(color = Color(0xFF3E2723), radius = u * 0.09f, center = Offset(cx + u * 0.14f, cy - u * 0.34f))
                drawCircle(color = Color(0xFF3E2723), radius = u * 0.09f, center = Offset(cx, cy - u * 0.14f))
            }
        }

        WordArtworkKey.PINEAPPLE -> {
            // Spiky green crown
            val crown = Path().apply {
                moveTo(cx - u * 0.36f, cy - u * 0.42f)
                lineTo(cx - u * 0.48f, cy - u * 0.96f)
                lineTo(cx - u * 0.14f, cy - u * 0.65f)
                lineTo(cx, cy - u * 1.15f)
                lineTo(cx + u * 0.14f, cy - u * 0.65f)
                lineTo(cx + u * 0.48f, cy - u * 0.96f)
                lineTo(cx + u * 0.36f, cy - u * 0.42f)
                close()
            }
            drawPath(crown, color = Color(0xFF43A047))
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57F17)), Offset(cx - u * 0.2f, cy), u),
                topLeft = Offset(cx - u * 0.56f, cy - u * 0.48f),
                size = Size(u * 1.12f, u * 1.38f)
            )
            // Criss-cross diamond texture
            val stroke = Stroke(width = (u * 0.035f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
            for (k in -1..1) {
                drawLine(Color(0xFFE65100).copy(alpha = 0.45f), Offset(cx - u * 0.38f, cy + k * u * 0.28f), Offset(cx + u * 0.38f, cy + (k + 1) * u * 0.28f), stroke.width)
                drawLine(Color(0xFFE65100).copy(alpha = 0.45f), Offset(cx + u * 0.38f, cy + k * u * 0.28f), Offset(cx - u * 0.38f, cy + (k + 1) * u * 0.28f), stroke.width)
            }
        }

        WordArtworkKey.KIWI -> {
            drawGlossySphere(Offset(cx, cy), u * 0.82f, Color(0xFF8D6E63), Color(0xFF5D4037), highlightAlpha = 0.15f)
            drawCircle(color = Color(0xFF8BC34A), radius = u * 0.68f, center = Offset(cx, cy))
            drawOval(color = Color(0xFFF1F8E9), topLeft = Offset(cx - u * 0.22f, cy - u * 0.28f), size = Size(u * 0.44f, u * 0.56f))
            for (a in 0 until 10) {
                val rad = a * (2.0 * PI / 10.0)
                drawCircle(
                    color = Color(0xFF212121),
                    radius = u * 0.04f,
                    center = Offset(cx + cos(rad).toFloat() * u * 0.38f, cy + sin(rad).toFloat() * u * 0.38f)
                )
            }
        }

        WordArtworkKey.AVOCADO -> {
            val outer = Path().apply {
                moveTo(cx, cy - u * 0.90f)
                cubicTo(cx + u * 0.44f, cy - u * 0.90f, cx + u * 0.72f, cy - u * 0.10f, cx + u * 0.68f, cy + u * 0.36f)
                cubicTo(cx + u * 0.64f, cy + u * 0.92f, cx - u * 0.64f, cy + u * 0.92f, cx - u * 0.68f, cy + u * 0.36f)
                cubicTo(cx - u * 0.72f, cy - u * 0.10f, cx - u * 0.44f, cy - u * 0.90f, cx, cy - u * 0.90f)
                close()
            }
            drawPath(outer, color = Color(0xFF2E7D32))
            val inner = Path().apply {
                moveTo(cx, cy - u * 0.76f)
                cubicTo(cx + u * 0.34f, cy - u * 0.76f, cx + u * 0.58f, cy - u * 0.06f, cx + u * 0.56f, cy + u * 0.34f)
                cubicTo(cx + u * 0.52f, cy + u * 0.78f, cx - u * 0.52f, cy + u * 0.78f, cx - u * 0.56f, cy + u * 0.34f)
                cubicTo(cx - u * 0.58f, cy - u * 0.06f, cx - u * 0.34f, cy - u * 0.76f, cx, cy - u * 0.76f)
                close()
            }
            drawPath(inner, color = Color(0xFFC5E1A5))
            drawGlossySphere(Offset(cx, cy + u * 0.28f), u * 0.30f, Color(0xFF8D6E63), Color(0xFF4E342E))
        }

        WordArtworkKey.FRUIT_SALAD -> {
            // Colorful bowl with mixed drawn fruit cubes/slices
            drawGlossySphere(Offset(cx - u * 0.38f, cy - u * 0.22f), u * 0.28f, Color(0xFFFF5252), Color(0xFFC62828))
            drawGlossySphere(Offset(cx + u * 0.35f, cy - u * 0.18f), u * 0.26f, Color(0xFFFFCA28), Color(0xFFF57C00))
            drawGlossySphere(Offset(cx, cy - u * 0.34f), u * 0.28f, Color(0xFF9CCC65), Color(0xFF558B2F))
            drawGlossySphere(Offset(cx - u * 0.18f, cy - u * 0.05f), u * 0.20f, Color(0xFF5C6BC0), Color(0xFF283593))
            drawGlossySphere(Offset(cx + u * 0.16f, cy - u * 0.04f), u * 0.22f, Color(0xFFFFEE58), Color(0xFFFBC02D))
            val bowl = Path().apply {
                moveTo(cx - u * 0.88f, cy - u * 0.02f)
                lineTo(cx + u * 0.88f, cy - u * 0.02f)
                cubicTo(cx + u * 0.84f, cy + u * 0.78f, cx - u * 0.84f, cy + u * 0.78f, cx - u * 0.88f, cy - u * 0.02f)
                close()
            }
            drawPath(
                bowl,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF4FC3F7), Color(0xFF0288D1)),
                    Offset(cx, cy),
                    Offset(cx, cy + u * 0.8f)
                )
            )
            drawRoundRect(
                color = Color(0xFFB3E5FC),
                topLeft = Offset(cx - u * 0.92f, cy - u * 0.08f),
                size = Size(u * 1.84f, u * 0.14f),
                cornerRadius = CornerRadius(u * 0.07f, u * 0.07f)
            )
        }

        WordArtworkKey.CUCUMBER -> {
            rotate(-28f, Offset(cx, cy)) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
                        Offset(cx, cy - u * 0.3f),
                        Offset(cx, cy + u * 0.3f)
                    ),
                    topLeft = Offset(cx - u * 0.95f, cy - u * 0.30f),
                    size = Size(u * 1.90f, u * 0.60f),
                    cornerRadius = CornerRadius(u * 0.30f, u * 0.30f)
                )
                drawLine(Color(0xFFA5D6A7).copy(alpha = 0.6f), Offset(cx - u * 0.65f, cy - u * 0.14f), Offset(cx + u * 0.55f, cy - u * 0.14f), u * 0.06f, StrokeCap.Round)
                listOf(-0.45f, -0.1f, 0.25f, 0.55f).forEach { x ->
                    drawCircle(Color(0xFF1B5E20), u * 0.04f, Offset(cx + x * u, cy + u * 0.08f))
                }
            }
        }

        WordArtworkKey.TOMATO -> {
            drawGlossySphere(Offset(cx, cy + u * 0.08f), u * 0.82f, Color(0xFFFF5252), Color(0xFFD50000))
            val star = Path().apply {
                moveTo(cx - u * 0.38f, cy - u * 0.68f)
                lineTo(cx - u * 0.12f, cy - u * 0.58f)
                lineTo(cx, cy - u * 0.94f)
                lineTo(cx + u * 0.12f, cy - u * 0.58f)
                lineTo(cx + u * 0.38f, cy - u * 0.68f)
                lineTo(cx, cy - u * 0.44f)
                close()
            }
            drawPath(star, color = Color(0xFF388E3C))
        }

        WordArtworkKey.ONION -> {
            val bulb = Path().apply {
                moveTo(cx, cy - u * 0.92f)
                lineTo(cx + u * 0.14f, cy - u * 0.58f)
                cubicTo(cx + u * 0.94f, cy - u * 0.28f, cx + u * 0.86f, cy + u * 0.84f, cx, cy + u * 0.84f)
                cubicTo(cx - u * 0.86f, cy + u * 0.84f, cx - u * 0.94f, cy - u * 0.28f, cx - u * 0.14f, cy - u * 0.58f)
                close()
            }
            drawPath(
                bulb,
                brush = Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFD4882A), Color(0xFF8D5524)), Offset(cx - u * 0.2f, cy), u * 1.3f)
            )
            drawArc(
                color = Color(0xFF8D5524).copy(alpha = 0.4f),
                startAngle = -70f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - u * 0.42f, cy - u * 0.55f),
                size = Size(u * 0.84f, u * 1.35f),
                style = Stroke(width = u * 0.04f)
            )
        }

        WordArtworkKey.GARLIC -> {
            drawOval(Color(0xFFE0D7C8), Offset(cx - u * 0.65f, cy - u * 0.48f), Size(u * 1.30f, u * 1.25f))
            drawGlossySphere(Offset(cx, cy + u * 0.12f), u * 0.48f, Color(0xFFFFFBF2), Color(0xFFE6DEC8), 0.5f)
            drawRoundRect(Color(0xFFD7CBB8), Offset(cx - u * 0.10f, cy - u * 0.85f), Size(u * 0.20f, u * 0.42f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.POTATO -> {
            rotate(-12f, Offset(cx, cy)) {
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFE0B478), Color(0xFFA67442)), Offset(cx - u * 0.2f, cy - u * 0.2f), u * 1.3f),
                    topLeft = Offset(cx - u * 0.85f, cy - u * 0.56f),
                    size = Size(u * 1.70f, u * 1.12f)
                )
                listOf(Offset(-0.35f, -0.15f), Offset(0.25f, 0.20f), Offset(-0.08f, 0.25f)).forEach { p ->
                    drawCircle(Color(0xFF795548).copy(alpha = 0.45f), u * 0.045f, Offset(cx + p.x * u, cy + p.y * u))
                }
            }
        }

        WordArtworkKey.CARROT -> {
            rotate(-35f, Offset(cx, cy)) {
                drawStemAndLeaf(Offset(cx + u * 0.62f, cy), Offset(cx + u * 1.05f, cy), leafRight = true, scale = 0.8f)
                val carrot = Path().apply {
                    moveTo(cx + u * 0.66f, cy - u * 0.34f)
                    cubicTo(cx + u * 0.78f, cy, cx + u * 0.78f, cy, cx + u * 0.66f, cy + u * 0.34f)
                    lineTo(cx - u * 0.90f, cy + u * 0.06f)
                    quadraticTo(cx - u * 0.98f, cy, cx - u * 0.90f, cy - u * 0.06f)
                    close()
                }
                drawPath(carrot, brush = Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFE65100)), Offset(cx, cy - u * 0.3f), Offset(cx, cy + u * 0.3f)))
            }
        }

        WordArtworkKey.PEPPER -> {
            drawRoundRect(
                brush = Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFC62828)), Offset(cx - u * 0.2f, cy - u * 0.2f), u * 1.3f),
                topLeft = Offset(cx - u * 0.68f, cy - u * 0.55f),
                size = Size(u * 1.36f, u * 1.38f),
                cornerRadius = CornerRadius(u * 0.45f, u * 0.45f)
            )
            drawRoundRect(Color(0xFF43A047), Offset(cx - u * 0.12f, cy - u * 0.92f), Size(u * 0.24f, u * 0.42f), CornerRadius(u * 0.1f, u * 0.1f))
        }

        WordArtworkKey.MUSHROOM -> {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFFFF8E1), Color(0xFFD7CCC8)), Offset(cx - u * 0.25f, cy), Offset(cx + u * 0.25f, cy)),
                topLeft = Offset(cx - u * 0.26f, cy - u * 0.05f),
                size = Size(u * 0.52f, u * 0.88f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            val cap = Path().apply {
                moveTo(cx - u * 0.85f, cy + u * 0.08f)
                cubicTo(cx - u * 0.88f, cy - u * 0.95f, cx + u * 0.88f, cy - u * 0.95f, cx + u * 0.85f, cy + u * 0.08f)
                close()
            }
            drawPath(cap, brush = Brush.radialGradient(listOf(Color(0xFFBCAAA4), Color(0xFF6D4C41)), Offset(cx - u * 0.2f, cy - u * 0.4f), u * 1.1f))
        }

        WordArtworkKey.LETTUCE, WordArtworkKey.BROCCOLI -> {
            val isBroccoli = key == WordArtworkKey.BROCCOLI
            if (isBroccoli) {
                drawRoundRect(Color(0xFF8BC34A), Offset(cx - u * 0.18f, cy + u * 0.05f), Size(u * 0.36f, u * 0.78f), CornerRadius(u * 0.14f, u * 0.14f))
            }
            drawGlossySphere(Offset(cx - u * 0.38f, cy - u * 0.14f), u * 0.44f, Color(0xFF66BB6A), Color(0xFF2E7D32), 0.35f)
            drawGlossySphere(Offset(cx + u * 0.38f, cy - u * 0.14f), u * 0.44f, Color(0xFF66BB6A), Color(0xFF2E7D32), 0.35f)
            drawGlossySphere(Offset(cx, cy - u * 0.38f), u * 0.48f, Color(0xFF81C784), Color(0xFF388E3C), 0.40f)
        }

        WordArtworkKey.CORN -> {
            drawOval(Color(0xFFFFD54F), Offset(cx - u * 0.34f, cy - u * 0.84f), Size(u * 0.68f, u * 1.48f))
            val huskL = Path().apply {
                moveTo(cx - u * 0.45f, cy - u * 0.12f)
                quadraticTo(cx - u * 0.68f, cy + u * 0.65f, cx, cy + u * 0.88f)
                lineTo(cx, cy + u * 0.28f)
                close()
            }
            val huskR = Path().apply {
                moveTo(cx + u * 0.45f, cy - u * 0.12f)
                quadraticTo(cx + u * 0.68f, cy + u * 0.65f, cx, cy + u * 0.88f)
                lineTo(cx, cy + u * 0.28f)
                close()
            }
            drawPath(huskL, color = Color(0xFF66BB6A))
            drawPath(huskR, color = Color(0xFF43A047))
        }

        WordArtworkKey.PUMPKIN -> {
            drawRoundRect(Color(0xFF4E342E), Offset(cx - u * 0.10f, cy - u * 0.92f), Size(u * 0.20f, u * 0.34f), CornerRadius(u * 0.06f, u * 0.06f))
            drawOval(Color(0xFFE65100), Offset(cx - u * 0.90f, cy - u * 0.56f), Size(u * 1.80f, u * 1.36f))
            drawOval(Color(0xFFF57C00), Offset(cx - u * 0.58f, cy - u * 0.58f), Size(u * 1.16f, u * 1.40f))
            drawOval(Color(0xFFFF9800), Offset(cx - u * 0.28f, cy - u * 0.60f), Size(u * 0.56f, u * 1.44f))
        }

        WordArtworkKey.EGGPLANT -> {
            rotate(-22f, Offset(cx, cy)) {
                val body = Path().apply {
                    moveTo(cx - u * 0.14f, cy - u * 0.72f)
                    cubicTo(cx + u * 0.34f, cy - u * 0.76f, cx + u * 0.62f, cy + u * 0.12f, cx + u * 0.44f, cy + u * 0.62f)
                    cubicTo(cx + u * 0.28f, cy + u * 0.96f, cx - u * 0.36f, cy + u * 0.94f, cx - u * 0.48f, cy + u * 0.56f)
                    cubicTo(cx - u * 0.62f, cy + u * 0.10f, cx - u * 0.46f, cy - u * 0.64f, cx - u * 0.14f, cy - u * 0.72f)
                    close()
                }
                drawPath(
                    body,
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF9C27B0), Color(0xFF5E1778), Color(0xFF320B45)),
                        center = Offset(cx - u * 0.18f, cy - u * 0.08f),
                        radius = u * 1.35f
                    )
                )
                // Glossy curved shine highlight
                val shine = Path().apply {
                    moveTo(cx - u * 0.28f, cy - u * 0.24f)
                    cubicTo(cx - u * 0.38f, cy + u * 0.08f, cx - u * 0.34f, cy + u * 0.42f, cx - u * 0.18f, cy + u * 0.58f)
                    cubicTo(cx - u * 0.26f, cy + u * 0.36f, cx - u * 0.24f, cy + u * 0.06f, cx - u * 0.28f, cy - u * 0.24f)
                    close()
                }
                drawPath(shine, color = Color.White.copy(alpha = 0.52f))
                // Green calyx crown & stem
                val calyx = Path().apply {
                    moveTo(cx - u * 0.42f, cy - u * 0.38f)
                    lineTo(cx - u * 0.18f, cy - u * 0.48f)
                    lineTo(cx, cy - u * 0.24f)
                    lineTo(cx + u * 0.18f, cy - u * 0.48f)
                    lineTo(cx + u * 0.40f, cy - u * 0.38f)
                    lineTo(cx + u * 0.14f, cy - u * 0.78f)
                    lineTo(cx - u * 0.18f, cy - u * 0.76f)
                    close()
                }
                drawPath(calyx, brush = Brush.linearGradient(listOf(Color(0xFF7CB342), Color(0xFF33691E)), Offset(cx, cy - u * 0.8f), Offset(cx, cy - u * 0.3f)))
                drawRoundRect(Color(0xFF558B2F), Offset(cx - u * 0.08f, cy - u * 1.04f), Size(u * 0.16f, u * 0.32f), CornerRadius(u * 0.08f, u * 0.08f))
            }
        }

        WordArtworkKey.CAULIFLOWER -> {
            // Crisp green outer cradle leaves
            drawOval(Color(0xFF43A047), Offset(cx - u * 0.92f, cy - u * 0.08f), Size(u * 1.84f, u * 0.90f))
            drawOval(Color(0xFF66BB6A), Offset(cx - u * 0.78f, cy + u * 0.08f), Size(u * 1.56f, u * 0.72f))
            // Creamy bumpy florets
            drawGlossySphere(Offset(cx - u * 0.38f, cy - u * 0.04f), u * 0.40f, Color(0xFFFFFFFF), Color(0xFFE6DFCF), 0.45f)
            drawGlossySphere(Offset(cx + u * 0.38f, cy - u * 0.04f), u * 0.40f, Color(0xFFFFFFFF), Color(0xFFE6DFCF), 0.45f)
            drawGlossySphere(Offset(cx - u * 0.22f, cy - u * 0.40f), u * 0.40f, Color(0xFFFFFFFF), Color(0xFFEBE4D4), 0.50f)
            drawGlossySphere(Offset(cx + u * 0.22f, cy - u * 0.40f), u * 0.40f, Color(0xFFFFFFFF), Color(0xFFEBE4D4), 0.50f)
            drawGlossySphere(Offset(cx, cy - u * 0.16f), u * 0.46f, Color(0xFFFFFFFF), Color(0xFFF0EAD8), 0.52f)
        }

        WordArtworkKey.PEA_POD -> {
            rotate(-24f, Offset(cx, cy)) {
                val pod = Path().apply {
                    moveTo(cx - u * 0.95f, cy - u * 0.18f)
                    cubicTo(cx - u * 0.35f, cy + u * 0.56f, cx + u * 0.35f, cy + u * 0.56f, cx + u * 0.95f, cy - u * 0.18f)
                    cubicTo(cx + u * 0.42f, cy + u * 0.08f, cx - u * 0.42f, cy + u * 0.08f, cx - u * 0.95f, cy - u * 0.18f)
                    close()
                }
                drawPath(pod, brush = Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)), Offset(cx, cy - u * 0.2f), Offset(cx, cy + u * 0.4f)))
                listOf(-0.46f, -0.16f, 0.16f, 0.46f).forEach { dx ->
                    drawGlossySphere(Offset(cx + dx * u, cy + u * 0.04f), u * 0.18f, Color(0xFFAEEA00), Color(0xFF558B2F))
                }
            }
        }

        WordArtworkKey.CHILI -> {
            rotate(-28f, Offset(cx, cy)) {
                val pepper = Path().apply {
                    moveTo(cx - u * 0.42f, cy - u * 0.46f)
                    cubicTo(cx - u * 0.02f, cy - u * 0.48f, cx + u * 0.54f, cy - u * 0.08f, cx + u * 0.88f, cy + u * 0.52f)
                    cubicTo(cx + u * 0.34f, cy + u * 0.42f, cx - u * 0.48f, cy + u * 0.18f, cx - u * 0.62f, cy - u * 0.22f)
                    close()
                }
                drawPath(pepper, brush = Brush.radialGradient(listOf(Color(0xFFFF3D00), Color(0xFFD50000), Color(0xFF8E0000)), Offset(cx - u * 0.2f, cy - u * 0.15f), u * 1.2f))
                drawRoundRect(Color(0xFF43A047), Offset(cx - u * 0.72f, cy - u * 0.58f), Size(u * 0.32f, u * 0.18f), CornerRadius(u * 0.08f, u * 0.08f))
            }
        }

        WordArtworkKey.RADISH -> {
            drawStemAndLeaf(Offset(cx, cy - u * 0.46f), Offset(cx - u * 0.08f, cy - u * 0.96f), leafRight = true, scale = 0.88f)
            drawStemAndLeaf(Offset(cx, cy - u * 0.46f), Offset(cx + u * 0.08f, cy - u * 0.96f), leafRight = false, scale = 0.80f)
            drawGlossySphere(Offset(cx, cy + u * 0.14f), u * 0.64f, Color(0xFFFF4081), Color(0xFFC51162))
            val tip = Path().apply {
                moveTo(cx - u * 0.22f, cy + u * 0.66f)
                lineTo(cx + u * 0.22f, cy + u * 0.66f)
                lineTo(cx, cy + u * 1.02f)
                close()
            }
            drawPath(tip, color = Color(0xFFFFF8E1))
        }

        WordArtworkKey.ASPARAGUS -> {
            listOf(-0.28f, 0f, 0.28f).forEachIndexed { idx, dx ->
                val topY = cy - u * (0.72f + if (idx == 1) 0.16f else 0f)
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF9CCC65), Color(0xFF558B2F)), Offset(cx + dx * u, topY), Offset(cx + dx * u, cy + u * 0.8f)),
                    topLeft = Offset(cx + dx * u - u * 0.11f, topY),
                    size = Size(u * 0.22f, u * 1.56f),
                    cornerRadius = CornerRadius(u * 0.10f, u * 0.10f)
                )
                val tip = Path().apply {
                    moveTo(cx + dx * u - u * 0.14f, topY + u * 0.18f)
                    lineTo(cx + dx * u, topY - u * 0.16f)
                    lineTo(cx + dx * u + u * 0.14f, topY + u * 0.18f)
                    close()
                }
                drawPath(tip, color = Color(0xFF33691E))
            }
        }

        WordArtworkKey.ZUCCHINI -> {
            rotate(-24f, Offset(cx, cy)) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF1B5E20)), Offset(cx, cy - u * 0.3f), Offset(cx, cy + u * 0.3f)),
                    topLeft = Offset(cx - u * 0.92f, cy - u * 0.28f),
                    size = Size(u * 1.84f, u * 0.56f),
                    cornerRadius = CornerRadius(u * 0.28f, u * 0.28f)
                )
                drawLine(Color(0xFFA5D6A7).copy(alpha = 0.55f), Offset(cx - u * 0.66f, cy - u * 0.12f), Offset(cx + u * 0.66f, cy - u * 0.12f), u * 0.05f, StrokeCap.Round)
                drawRoundRect(Color(0xFF689F38), Offset(cx + u * 0.78f, cy - u * 0.14f), Size(u * 0.22f, u * 0.28f), CornerRadius(u * 0.06f, u * 0.06f))
            }
        }

        else -> return false
    }
    return true
}
