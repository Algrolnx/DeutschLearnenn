package com.example.ui.components.illustrations

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Shared studio vector drawing helpers producing vibrant glossy shaded flat-vector graphics
 * with luminous studio backdrops, soft drop shadows, layered gradients, and crisp specular highlights.
 */
internal fun DrawScope.drawVectorStage(accent: Color, isDark: Boolean) {
    val bgCenter = if (isDark) Color(0xFF2B3142) else Color.White
    val bgEdge = if (isDark) Color(0xFF191D28) else accent.copy(alpha = 0.08f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(bgCenter, bgEdge),
            center = Offset(size.width * 0.5f, size.height * 0.44f),
            radius = maxOf(size.width, size.height) * 0.72f
        )
    )
    // Soft grounded shadow oval beneath the centered illustration
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = if (isDark) 0.42f else 0.14f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.5f, size.height * 0.87f),
            radius = size.minDimension * 0.30f
        ),
        topLeft = Offset(size.width * 0.24f, size.height * 0.81f),
        size = Size(size.width * 0.52f, size.height * 0.11f)
    )
}

internal fun DrawScope.drawGlossySphere(
    center: Offset,
    radius: Float,
    lightColor: Color,
    darkColor: Color,
    highlightAlpha: Float = 0.74f
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lightColor, darkColor),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
            radius = radius * 1.48f
        ),
        radius = radius,
        center = center
    )
    rotate(degrees = -28f, pivot = Offset(center.x - radius * 0.35f, center.y - radius * 0.38f)) {
        drawOval(
            color = Color.White.copy(alpha = highlightAlpha),
            topLeft = Offset(center.x - radius * 0.52f, center.y - radius * 0.50f),
            size = Size(radius * 0.40f, radius * 0.19f)
        )
    }
}

internal fun DrawScope.drawStemAndLeaf(
    stemBase: Offset,
    stemTop: Offset,
    leafRight: Boolean = true,
    scale: Float = 1f,
    leafLight: Color = Color(0xFF7CC832),
    leafDark: Color = Color(0xFF3E8E22),
    stemColor: Color = Color(0xFF6B3E26)
) {
    val w = size.minDimension * scale
    val stemPath = Path().apply {
        moveTo(stemBase.x - w * 0.018f, stemBase.y)
        quadraticTo(
            (stemBase.x + stemTop.x) * 0.5f - w * 0.01f,
            (stemBase.y + stemTop.y) * 0.5f,
            stemTop.x - w * 0.028f,
            stemTop.y
        )
        lineTo(stemTop.x + w * 0.028f, stemTop.y - w * 0.006f)
        quadraticTo(
            (stemBase.x + stemTop.x) * 0.5f + w * 0.01f,
            (stemBase.y + stemTop.y) * 0.5f,
            stemBase.x + w * 0.018f,
            stemBase.y
        )
        close()
    }
    drawPath(stemPath, color = stemColor)

    val dir = if (leafRight) 1f else -1f
    val anchor = Offset(
        x = (stemBase.x + stemTop.x) * 0.52f,
        y = stemTop.y + (stemBase.y - stemTop.y) * 0.28f
    )
    val tip = Offset(anchor.x + dir * w * 0.25f, anchor.y - w * 0.10f)
    val leafPath = Path().apply {
        moveTo(anchor.x, anchor.y)
        cubicTo(
            anchor.x + dir * w * 0.07f, anchor.y - w * 0.14f,
            tip.x - dir * w * 0.06f, tip.y - w * 0.04f,
            tip.x, tip.y
        )
        cubicTo(
            tip.x - dir * w * 0.05f, tip.y + w * 0.10f,
            anchor.x + dir * w * 0.09f, anchor.y + w * 0.09f,
            anchor.x, anchor.y
        )
        close()
    }
    drawPath(
        path = leafPath,
        brush = Brush.linearGradient(
            colors = listOf(leafLight, leafDark),
            start = anchor,
            end = tip
        )
    )
    drawLine(
        color = Color(0xFFA7E363).copy(alpha = 0.72f),
        start = Offset(anchor.x + dir * w * 0.02f, anchor.y - w * 0.005f),
        end = Offset(tip.x - dir * w * 0.04f, tip.y + w * 0.008f),
        strokeWidth = (w * 0.012f).coerceAtLeast(1.2f),
        cap = StrokeCap.Round
    )
}

internal fun DrawScope.drawRoundStrokeOutline(
    path: Path,
    color: Color,
    strokeRatio: Float = 0.024f
) {
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = (size.minDimension * strokeRatio).coerceAtLeast(1.5f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
