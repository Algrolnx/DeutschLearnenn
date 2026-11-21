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

internal fun DrawScope.drawConceptSceneVector(
    key: WordArtworkKey,
    semanticTint: Color? = null
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.52f
    val u = size.minDimension * 0.36f

    when (key) {
        WordArtworkKey.CALENDAR_TIME -> {
            drawRoundRect(Color.White, Offset(cx - u * 0.72f, cy - u * 0.64f), Size(u * 1.44f, u * 1.38f), CornerRadius(u * 0.18f, u * 0.18f))
            drawRoundRect(Color(0xFFEF5350), Offset(cx - u * 0.72f, cy - u * 0.64f), Size(u * 1.44f, u * 0.40f), CornerRadius(u * 0.18f, u * 0.18f))
            for (r in 0..1) {
                for (c in 0..2) {
                    drawRoundRect(
                        Color(0xFFB0BEC5),
                        Offset(cx - u * 0.48f + c * u * 0.36f, cy - u * 0.04f + r * u * 0.34f),
                        Size(u * 0.24f, u * 0.22f),
                        CornerRadius(u * 0.05f, u * 0.05f)
                    )
                }
            }
        }

        WordArtworkKey.WORK_BRIEFCASE -> {
            drawRoundRect(Color(0xFF5D4037), Offset(cx - u * 0.24f, cy - u * 0.74f), Size(u * 0.48f, u * 0.34f), CornerRadius(u * 0.08f, u * 0.08f), style = Stroke(width = u * 0.09f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFA1887F), Color(0xFF5D4037)), Offset(cx, cy - u * 0.5f), Offset(cx, cy + u * 0.7f)),
                topLeft = Offset(cx - u * 0.80f, cy - u * 0.46f),
                size = Size(u * 1.60f, u * 1.14f),
                cornerRadius = CornerRadius(u * 0.18f, u * 0.18f)
            )
            drawRoundRect(Color(0xFFFFD54F), Offset(cx - u * 0.14f, cy - u * 0.02f), Size(u * 0.28f, u * 0.22f), CornerRadius(u * 0.06f, u * 0.06f))
        }

        WordArtworkKey.CHAT_BUBBLES -> {
            drawRoundRect(Color(0xFF29B6F6), Offset(cx - u * 0.82f, cy - u * 0.62f), Size(u * 1.18f, u * 0.78f), CornerRadius(u * 0.24f, u * 0.24f))
            drawRoundRect(Color(0xFF66BB6A), Offset(cx - u * 0.28f, cy - u * 0.04f), Size(u * 1.12f, u * 0.72f), CornerRadius(u * 0.24f, u * 0.24f))
            listOf(-0.20f, 0.08f, 0.36f).forEach { dx ->
                drawCircle(Color.White, u * 0.07f, Offset(cx + (dx + 0.18f) * u, cy + u * 0.32f))
            }
        }

        WordArtworkKey.COMPASS -> {
            drawGlossySphere(Offset(cx, cy), u * 0.82f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.35f)
            drawCircle(Color(0xFF37474F), u * 0.78f, Offset(cx, cy), style = Stroke(width = u * 0.08f))
            val north = Path().apply {
                moveTo(cx, cy - u * 0.58f)
                lineTo(cx + u * 0.18f, cy)
                lineTo(cx - u * 0.18f, cy)
                close()
            }
            val south = Path().apply {
                moveTo(cx, cy + u * 0.58f)
                lineTo(cx + u * 0.18f, cy)
                lineTo(cx - u * 0.18f, cy)
                close()
            }
            drawPath(north, color = Color(0xFFE53935))
            drawPath(south, color = Color(0xFF1E88E5))
        }

        WordArtworkKey.SCALE_COMPARE -> {
            drawRoundRect(Color(0xFF546E7A), Offset(cx - u * 0.08f, cy - u * 0.62f), Size(u * 0.16f, u * 1.24f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF455A64), Offset(cx - u * 0.78f, cy - u * 0.52f), Size(u * 1.56f, u * 0.14f), CornerRadius(u * 0.06f, u * 0.06f))
            drawGlossySphere(Offset(cx - u * 0.62f, cy + u * 0.12f), u * 0.30f, Color(0xFFFFB74D), Color(0xFFF57C00))
            drawGlossySphere(Offset(cx + u * 0.62f, cy + u * 0.12f), u * 0.30f, Color(0xFF4FC3F7), Color(0xFF0288D1))
        }

        WordArtworkKey.SPEED_GAUGE -> {
            drawArc(Color(0xFF4FC3F7), 160f, 110f, false, Offset(cx - u * 0.78f, cy - u * 0.56f), Size(u * 1.56f, u * 1.56f), style = Stroke(width = u * 0.18f, cap = StrokeCap.Round))
            drawArc(Color(0xFFFF7043), 270f, 110f, false, Offset(cx - u * 0.78f, cy - u * 0.56f), Size(u * 1.56f, u * 1.56f), style = Stroke(width = u * 0.18f, cap = StrokeCap.Round))
            drawLine(Color(0xFF263238), Offset(cx, cy + u * 0.24f), Offset(cx + u * 0.44f, cy - u * 0.24f), u * 0.10f, StrokeCap.Round)
            drawCircle(Color(0xFF263238), u * 0.14f, Offset(cx, cy + u * 0.24f))
        }

        WordArtworkKey.IDEA_SPARK -> {
            drawGlossySphere(Offset(cx, cy - u * 0.14f), u * 0.56f, Color(0xFFFFF176), Color(0xFFFFA000), 0.6f)
            drawRoundRect(Color(0xFF78909C), Offset(cx - u * 0.22f, cy + u * 0.40f), Size(u * 0.44f, u * 0.34f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.TROPHY -> {
            val cup = Path().apply {
                moveTo(cx - u * 0.56f, cy - u * 0.70f)
                lineTo(cx + u * 0.56f, cy - u * 0.70f)
                cubicTo(cx + u * 0.50f, cy + u * 0.24f, cx - u * 0.50f, cy + u * 0.24f, cx - u * 0.56f, cy - u * 0.70f)
                close()
            }
            drawPath(cup, brush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFF57F17)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.3f)))
            drawRoundRect(Color(0xFFF57F17), Offset(cx - u * 0.12f, cy + u * 0.14f), Size(u * 0.24f, u * 0.44f), CornerRadius(0f, 0f))
            drawRoundRect(Color(0xFF795548), Offset(cx - u * 0.44f, cy + u * 0.54f), Size(u * 0.88f, u * 0.24f), CornerRadius(u * 0.08f, u * 0.08f))
        }

        WordArtworkKey.GLOBE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFF4FC3F7), Color(0xFF0277BD), 0.45f)
            drawOval(Color(0xFF66BB6A), Offset(cx - u * 0.48f, cy - u * 0.42f), Size(u * 0.62f, u * 0.54f))
            drawOval(Color(0xFF66BB6A), Offset(cx + u * 0.08f, cy + u * 0.04f), Size(u * 0.52f, u * 0.48f))
        }

        WordArtworkKey.CITY_SKYLINE -> {
            drawRoundRect(Color(0xFF78909C), Offset(cx - u * 0.82f, cy - u * 0.20f), Size(u * 0.46f, u * 0.96f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF546E7A), Offset(cx - u * 0.26f, cy - u * 0.74f), Size(u * 0.54f, u * 1.50f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF90A4AE), Offset(cx + u * 0.36f, cy - u * 0.06f), Size(u * 0.46f, u * 0.82f), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFFFFF59D), Offset(cx - u * 0.12f, cy - u * 0.50f), Size(u * 0.12f, u * 0.18f), CornerRadius(0f, 0f))
            drawRoundRect(Color(0xFFFFF59D), Offset(cx + u * 0.06f, cy - u * 0.50f), Size(u * 0.12f, u * 0.18f), CornerRadius(0f, 0f))
        }

        WordArtworkKey.SHOPPING_BAG_SCENE -> {
            drawArc(Color(0xFFEC407A), 180f, 180f, false, Offset(cx - u * 0.30f, cy - u * 0.82f), Size(u * 0.60f, u * 0.56f), style = Stroke(width = u * 0.09f))
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFF48FB1), Color(0xFFD81B60)), Offset(cx, cy - u * 0.4f), Offset(cx, cy + u * 0.8f)),
                topLeft = Offset(cx - u * 0.62f, cy - u * 0.44f),
                size = Size(u * 1.24f, u * 1.22f),
                cornerRadius = CornerRadius(u * 0.16f, u * 0.16f)
            )
        }

        WordArtworkKey.WRENCH_TOOLS -> {
            rotate(-40f, Offset(cx, cy)) {
                drawRoundRect(Color(0xFF78909C), Offset(cx - u * 0.16f, cy - u * 0.72f), Size(u * 0.32f, u * 1.44f), CornerRadius(u * 0.12f, u * 0.12f))
                drawCircle(Color(0xFF90A4AE), u * 0.34f, Offset(cx, cy - u * 0.64f))
                drawCircle(Color(0xFFECEFF1), u * 0.15f, Offset(cx, cy - u * 0.74f))
            }
        }

        WordArtworkKey.SMILE_EMOTION -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFFFFEE58), Color(0xFFFBC02D), 0.55f)
            drawCircle(Color(0xFF3E2723), u * 0.09f, Offset(cx - u * 0.28f, cy - u * 0.16f))
            drawCircle(Color(0xFF3E2723), u * 0.09f, Offset(cx + u * 0.28f, cy - u * 0.16f))
            drawArc(Color(0xFF3E2723), 15f, 150f, false, Offset(cx - u * 0.40f, cy - u * 0.14f), Size(u * 0.80f, u * 0.62f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
        }

        WordArtworkKey.SCHOOL_CAP -> {
            val diamond = Path().apply {
                moveTo(cx, cy - u * 0.74f)
                lineTo(cx + u * 0.92f, cy - u * 0.24f)
                lineTo(cx, cy + u * 0.22f)
                lineTo(cx - u * 0.92f, cy - u * 0.24f)
                close()
            }
            drawPath(diamond, color = Color(0xFF3949AB))
            drawRoundRect(Color(0xFF283593), Offset(cx - u * 0.48f, cy + u * 0.04f), Size(u * 0.96f, u * 0.46f), CornerRadius(u * 0.12f, u * 0.12f))
            drawLine(Color(0xFFFFCA28), Offset(cx + u * 0.68f, cy - u * 0.10f), Offset(cx + u * 0.68f, cy + u * 0.52f), u * 0.07f, StrokeCap.Round)
        }

        WordArtworkKey.FAMILY_GROUP -> {
            drawGlossySphere(Offset(cx - u * 0.42f, cy - u * 0.34f), u * 0.26f, Color(0xFF42A5F5), Color(0xFF1565C0))
            drawRoundRect(Color(0xFF1E88E5), Offset(cx - u * 0.70f, cy - u * 0.02f), Size(u * 0.56f, u * 0.72f), CornerRadius(u * 0.24f, u * 0.24f))
            drawGlossySphere(Offset(cx + u * 0.42f, cy - u * 0.30f), u * 0.24f, Color(0xFFFF80AB), Color(0xFFC2185B))
            drawRoundRect(Color(0xFFEC407A), Offset(cx + u * 0.14f, cy + u * 0.02f), Size(u * 0.56f, u * 0.68f), CornerRadius(u * 0.24f, u * 0.24f))
            drawGlossySphere(Offset(cx, cy - u * 0.04f), u * 0.20f, Color(0xFFFFCA28), Color(0xFFF57C00))
            drawRoundRect(Color(0xFFFFA726), Offset(cx - u * 0.22f, cy + u * 0.20f), Size(u * 0.44f, u * 0.52f), CornerRadius(u * 0.18f, u * 0.18f))
        }

        WordArtworkKey.RECIPE_PLATE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFFFFFFFF), Color(0xFFCFD8DC), 0.35f)
            drawCircle(Color(0xFF90A4AE), u * 0.54f, Offset(cx, cy), style = Stroke(width = u * 0.04f))
            drawGlossySphere(Offset(cx - u * 0.16f, cy - u * 0.06f), u * 0.24f, Color(0xFFFF7043), Color(0xFFD84315))
            drawGlossySphere(Offset(cx + u * 0.20f, cy + u * 0.12f), u * 0.20f, Color(0xFF66BB6A), Color(0xFF2E7D32))
        }

        WordArtworkKey.COLOR_PALETTE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.82f, Color(0xFFFFE0B2), Color(0xFFD7CCC8), 0.35f)
            drawCircle(Color(0xFF2B3142), u * 0.16f, Offset(cx + u * 0.36f, cy + u * 0.28f))
            drawGlossySphere(Offset(cx - u * 0.42f, cy - u * 0.18f), u * 0.18f, Color(0xFFFF5252), Color(0xFFC62828))
            drawGlossySphere(Offset(cx - u * 0.14f, cy - u * 0.48f), u * 0.18f, Color(0xFFFFCA28), Color(0xFFF57F17))
            drawGlossySphere(Offset(cx + u * 0.26f, cy - u * 0.42f), u * 0.18f, Color(0xFF66BB6A), Color(0xFF2E7D32))
            drawGlossySphere(Offset(cx + u * 0.46f, cy - u * 0.06f), u * 0.18f, Color(0xFF42A5F5), Color(0xFF1565C0))
            drawGlossySphere(Offset(cx - u * 0.28f, cy + u * 0.28f), u * 0.18f, Color(0xFFAB47BC), Color(0xFF6A1B9A))
        }

        WordArtworkKey.COLOR_DROP -> {
            val baseColor = semanticTint ?: Color(0xFF42A5F5)
            val darkColor = Color(
                red = (baseColor.red * 0.62f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.62f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.62f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val drop = Path().apply {
                moveTo(cx, cy - u * 0.86f)
                cubicTo(cx + u * 0.84f, cy + u * 0.04f, cx + u * 0.68f, cy + u * 0.82f, cx, cy + u * 0.82f)
                cubicTo(cx - u * 0.68f, cy + u * 0.82f, cx - u * 0.84f, cy + u * 0.04f, cx, cy - u * 0.86f)
                close()
            }
            drawPath(
                drop,
                brush = Brush.radialGradient(
                    colors = listOf(baseColor, darkColor),
                    center = Offset(cx - u * 0.18f, cy),
                    radius = u * 1.3f
                )
            )
            drawPath(
                drop,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(width = (u * 0.05f).coerceAtLeast(2f))
            )
            rotate(-24f, Offset(cx - u * 0.24f, cy + u * 0.10f)) {
                drawOval(
                    color = Color.White.copy(alpha = 0.65f),
                    topLeft = Offset(cx - u * 0.36f, cy - u * 0.06f),
                    size = Size(u * 0.18f, u * 0.34f)
                )
            }
        }

        WordArtworkKey.SHOPPING_CART -> {
            val stroke = Stroke(width = (u * 0.09f).coerceAtLeast(3f), cap = StrokeCap.Round)
            val basket = Path().apply {
                moveTo(cx - u * 0.82f, cy - u * 0.44f)
                lineTo(cx - u * 0.56f, cy - u * 0.44f)
                lineTo(cx - u * 0.36f, cy + u * 0.28f)
                lineTo(cx + u * 0.64f, cy + u * 0.28f)
                lineTo(cx + u * 0.80f, cy - u * 0.26f)
                lineTo(cx - u * 0.48f, cy - u * 0.26f)
            }
            drawPath(basket, color = Color(0xFF42A5F5), style = stroke)
            drawLine(Color(0xFF90A4AE), Offset(cx - u * 0.36f, cy + u * 0.28f), Offset(cx + u * 0.60f, cy + u * 0.28f), u * 0.09f, StrokeCap.Round)
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx - u * 0.22f, cy + u * 0.56f))
            drawCircle(Color(0xFF263238), u * 0.16f, Offset(cx + u * 0.48f, cy + u * 0.56f))
        }

        WordArtworkKey.HAMMER_TOOL -> {
            rotate(-38f, Offset(cx, cy)) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFA1887F), Color(0xFF5D4037)), Offset(cx, cy - u * 0.3f), Offset(cx, cy + u * 0.8f)),
                    topLeft = Offset(cx - u * 0.12f, cy - u * 0.34f),
                    size = Size(u * 0.24f, u * 1.18f),
                    cornerRadius = CornerRadius(u * 0.10f, u * 0.10f)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFCFD8DC), Color(0xFF546E7A)), Offset(cx - u * 0.6f, cy - u * 0.6f), Offset(cx + u * 0.6f, cy - u * 0.3f)),
                    topLeft = Offset(cx - u * 0.64f, cy - u * 0.72f),
                    size = Size(u * 1.28f, u * 0.42f),
                    cornerRadius = CornerRadius(u * 0.10f, u * 0.10f)
                )
            }
        }

        WordArtworkKey.SAD_EMOTION -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFF90CAF9), Color(0xFF1E88E5), 0.5f)
            drawCircle(Color(0xFF1A237E), u * 0.09f, Offset(cx - u * 0.28f, cy - u * 0.16f))
            drawCircle(Color(0xFF1A237E), u * 0.09f, Offset(cx + u * 0.28f, cy - u * 0.16f))
            drawArc(Color(0xFF1A237E), 205f, 130f, false, Offset(cx - u * 0.36f, cy + u * 0.16f), Size(u * 0.72f, u * 0.46f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
        }

        WordArtworkKey.ANGRY_EMOTION -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFFFF8A65), Color(0xFFC62828), 0.5f)
            drawLine(Color(0xFF3E2723), Offset(cx - u * 0.42f, cy - u * 0.32f), Offset(cx - u * 0.14f, cy - u * 0.18f), u * 0.09f, StrokeCap.Round)
            drawLine(Color(0xFF3E2723), Offset(cx + u * 0.42f, cy - u * 0.32f), Offset(cx + u * 0.14f, cy - u * 0.18f), u * 0.09f, StrokeCap.Round)
            drawCircle(Color(0xFF3E2723), u * 0.08f, Offset(cx - u * 0.26f, cy - u * 0.10f))
            drawCircle(Color(0xFF3E2723), u * 0.08f, Offset(cx + u * 0.26f, cy - u * 0.10f))
            drawArc(Color(0xFF3E2723), 200f, 140f, false, Offset(cx - u * 0.34f, cy + u * 0.20f), Size(u * 0.68f, u * 0.40f), style = Stroke(width = u * 0.09f, cap = StrokeCap.Round))
        }

        WordArtworkKey.ARROW_DIRECTION -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFF4FC3F7), Color(0xFF0277BD), 0.45f)
            val arrow = Path().apply {
                moveTo(cx - u * 0.44f, cy - u * 0.14f)
                lineTo(cx + u * 0.04f, cy - u * 0.14f)
                lineTo(cx + u * 0.04f, cy - u * 0.40f)
                lineTo(cx + u * 0.50f, cy)
                lineTo(cx + u * 0.04f, cy + u * 0.40f)
                lineTo(cx + u * 0.04f, cy + u * 0.14f)
                lineTo(cx - u * 0.44f, cy + u * 0.14f)
                close()
            }
            drawPath(arrow, color = Color.White)
        }

        WordArtworkKey.CHECK_BADGE -> {
            drawGlossySphere(Offset(cx, cy), u * 0.80f, Color(0xFF66BB6A), Color(0xFF2E7D32), 0.5f)
            val check = Path().apply {
                moveTo(cx - u * 0.36f, cy + u * 0.04f)
                lineTo(cx - u * 0.10f, cy + u * 0.30f)
                lineTo(cx + u * 0.40f, cy - u * 0.26f)
            }
            drawPath(check, color = Color.White, style = Stroke(width = u * 0.16f, cap = StrokeCap.Round))
        }

        WordArtworkKey.WARNING_SIGN -> {
            val tri = Path().apply {
                moveTo(cx, cy - u * 0.78f)
                lineTo(cx + u * 0.86f, cy + u * 0.68f)
                lineTo(cx - u * 0.86f, cy + u * 0.68f)
                close()
            }
            drawPath(tri, brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFF57F17)), Offset(cx, cy - u * 0.7f), Offset(cx, cy + u * 0.7f)))
            drawRoundRect(Color(0xFF263238), Offset(cx - u * 0.08f, cy - u * 0.30f), Size(u * 0.16f, u * 0.54f), CornerRadius(u * 0.08f, u * 0.08f))
            drawCircle(Color(0xFF263238), u * 0.10f, Offset(cx, cy + u * 0.42f))
        }

        WordArtworkKey.CROWN -> {
            val crown = Path().apply {
                moveTo(cx - u * 0.78f, cy + u * 0.48f)
                lineTo(cx - u * 0.88f, cy - u * 0.38f)
                lineTo(cx - u * 0.38f, cy + u * 0.04f)
                lineTo(cx, cy - u * 0.62f)
                lineTo(cx + u * 0.38f, cy + u * 0.04f)
                lineTo(cx + u * 0.88f, cy - u * 0.38f)
                lineTo(cx + u * 0.78f, cy + u * 0.48f)
                close()
            }
            drawPath(crown, brush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFF57F17)), Offset(cx, cy - u * 0.6f), Offset(cx, cy + u * 0.5f)))
            drawRoundRect(Color(0xFFD84315), Offset(cx - u * 0.78f, cy + u * 0.44f), Size(u * 1.56f, u * 0.20f), CornerRadius(u * 0.08f, u * 0.08f))
            drawCircle(Color(0xFFEF5350), u * 0.10f, Offset(cx, cy + u * 0.16f))
        }

        WordArtworkKey.DIAMOND -> {
            val top = Path().apply {
                moveTo(cx - u * 0.56f, cy - u * 0.54f)
                lineTo(cx + u * 0.56f, cy - u * 0.54f)
                lineTo(cx + u * 0.88f, cy - u * 0.10f)
                lineTo(cx - u * 0.88f, cy - u * 0.10f)
                close()
            }
            val bottom = Path().apply {
                moveTo(cx - u * 0.88f, cy - u * 0.10f)
                lineTo(cx + u * 0.88f, cy - u * 0.10f)
                lineTo(cx, cy + u * 0.78f)
                close()
            }
            drawPath(top, brush = Brush.linearGradient(listOf(Color(0xFFB3E5FC), Color(0xFF29B6F6)), Offset(cx, cy - u * 0.6f), Offset(cx, cy)))
            drawPath(bottom, brush = Brush.linearGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD)), Offset(cx, cy), Offset(cx, cy + u * 0.8f)))
        }

        else -> {
            drawGlossySphere(Offset(cx, cy), u * 0.74f, Color(0xFF81D4FA), Color(0xFF0288D1))
        }
    }
}
