package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LeitnerBox1
import com.example.ui.theme.LeitnerBox2
import com.example.ui.theme.LeitnerBox3
import com.example.ui.theme.LeitnerBox4
import com.example.ui.theme.LeitnerBox5

@Composable
fun LeitnerBoxIndicator(
    boxNumber: Int,
    modifier: Modifier = Modifier
) {
    val activeColor = when (boxNumber) {
        1 -> LeitnerBox1
        2 -> LeitnerBox2
        3 -> LeitnerBox3
        4 -> LeitnerBox4
        5 -> LeitnerBox5
        else -> LeitnerBox1
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isFilled = i <= boxNumber
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isFilled) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
            )
        }
    }
}
