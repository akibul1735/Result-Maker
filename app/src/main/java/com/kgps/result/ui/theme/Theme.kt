package com.kgps.result.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TealPrimary = Color(0xFF0F766E)
val TealDark = Color(0xFF115E59)
val TealContainer = Color(0xFFCCFBF1)
val TealSoft = Color(0xFFF0FDFA)
val SlateBg = Color(0xFFF8FAFC)
val SlateSurfaceVariant = Color(0xFFF1F5F9)
val SlateOutline = Color(0xFFE2E8F0)
val SlateText = Color(0xFF0F172A)
val SlateMuted = Color(0xFF64748B)
val EmeraldGrade = Color(0xFF047857)
val AmberNotice = Color(0xFFB45309)
val AmberBg = Color(0xFFFFFBEB)
val AmberBorder = Color(0xFFFCD34D)
val RoseDanger = Color(0xFFBE123C)
val RoseBg = Color(0xFFFFF1F2)

private val ResultMakerColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDark,
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = SlateText,
    tertiary = AmberNotice,
    onTertiary = Color.White,
    tertiaryContainer = AmberBg,
    onTertiaryContainer = Color(0xFF78350F),
    background = SlateBg,
    onBackground = SlateText,
    surface = Color.White,
    onSurface = SlateText,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline,
    outlineVariant = Color(0xFFCBD5E1),
    error = RoseDanger,
    onError = Color.White,
    errorContainer = RoseBg,
    onErrorContainer = RoseDanger
)

private val ResultMakerTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = SlateText
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = SlateText
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = SlateText
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = SlateText
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = SlateText
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        color = SlateMuted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val ResultMakerShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun ResultMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ResultMakerColorScheme,
        typography = ResultMakerTypography,
        shapes = ResultMakerShapes,
        content = content
    )
}
