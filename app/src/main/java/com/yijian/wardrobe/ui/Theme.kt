package com.yijian.wardrobe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Cream = Color(0xFFF7F1E8)
val WarmWhite = Color(0xFFFFFCF7)
val Rose = Color(0xFFA85D67)
val Forest = Color(0xFF314D43)
val Sage = Color(0xFF9DAF9E)
val Caramel = Color(0xFFB57A50)
val Mist = Color(0xFF7A7A75)
val SoftRed = Color(0xFFB95555)

private val Colors = lightColorScheme(
    primary = Rose, onPrimary = Color.White, secondary = Forest, onSecondary = Color.White,
    tertiary = Caramel, background = Cream, onBackground = Forest, surface = WarmWhite,
    onSurface = Forest, error = SoftRed, outline = Sage
)

@Composable fun YiJianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
            labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
        ), content = content
    )
}
