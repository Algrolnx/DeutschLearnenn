package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GenderDasContainer
import com.example.ui.theme.GenderDasNeuter
import com.example.ui.theme.GenderDasOnDark
import com.example.ui.theme.GenderDerContainer
import com.example.ui.theme.GenderDerMasculine
import com.example.ui.theme.GenderDerOnDark
import com.example.ui.theme.GenderDieContainer
import com.example.ui.theme.GenderDieFeminine
import com.example.ui.theme.GenderDieOnDark
import com.example.ui.theme.GenderNeutralOnDark

@Composable
fun GenderBadge(
    article: String,
    modifier: Modifier = Modifier
) {
    if (article.isBlank()) return

    val (bgColor, textColor) = when (article.lowercase().trim()) {
        "der" -> GenderDerContainer to GenderDerMasculine
        "die" -> GenderDieContainer to GenderDieFeminine
        "das" -> GenderDasContainer to GenderDasNeuter
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = article.lowercase(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getGenderColor(article: String): Color {
    return when (article.lowercase().trim()) {
        "der" -> GenderDerMasculine
        "die" -> GenderDieFeminine
        "das" -> GenderDasNeuter
        else -> Color(0xFF64748B)
    }
}

fun getGenderColorOnDark(article: String): Color {
    return when (article.lowercase().trim()) {
        "der" -> GenderDerOnDark
        "die" -> GenderDieOnDark
        "das" -> GenderDasOnDark
        else -> GenderNeutralOnDark
    }
}
