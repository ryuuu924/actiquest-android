package com.actiquest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiquest.ui.theme.CobaltBlue
import com.actiquest.ui.theme.SlateSecondary
import com.actiquest.ui.theme.SurfaceElevated
import com.actiquest.ui.theme.TextMuted
import com.actiquest.ui.theme.TextSecondary
import com.actiquest.ui.theme.ThemeManager

@Composable
fun StatusCountBox(
    modifier: Modifier = Modifier,
    label: String,
    count: String,
    icon: ImageVector,
    color: Color
) {
    val isDark = ThemeManager.isDarkMode
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) SurfaceElevated.copy(alpha = 0.8f) else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = if (isDark) 0.35f else 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    count,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 10.sp,
                color = if (isDark) TextMuted else Color(0xFF475569),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color = CobaltBlue,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) activeColor else (if (isDark) SurfaceElevated else Color(0xFFF1F5F9)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else (if (isDark) SlateSecondary.copy(alpha = 0.3f) else Color(0xFFCBD5E1))
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else (if (isDark) TextSecondary else Color(0xFF0F172A)),
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
