package com.kgps.result.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.R

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
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealDark,
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
    onErrorContainer = RoseDanger,
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color.White,
    inversePrimary = TealContainer
)

fun appComposeFontFamily(fontId: String): FontFamily = when (fontId) {
    "nikosh" -> FontFamily(Font(R.font.nikosh, FontWeight.Normal))
    "sutonny_mj" -> FontFamily(Font(R.font.sutonny_mj, FontWeight.Normal))
    "kalpurush" -> FontFamily(Font(R.font.kalpurush, FontWeight.Normal))
    "solaiman_lipi" -> FontFamily(Font(R.font.solaiman_lipi, FontWeight.Normal))
    "noto_serif" -> FontFamily(Font(R.font.noto_serif_bengali, FontWeight.Normal))
    "tiro_bangla" -> FontFamily(Font(R.font.tiro_bangla, FontWeight.Normal))
    "noto_sans" -> FontFamily(Font(R.font.noto_sans_bengali, FontWeight.Normal))
    "hind_siliguri" -> FontFamily(Font(R.font.hind_siliguri, FontWeight.Normal))
    "anek_bangla" -> FontFamily(Font(R.font.anek_bangla, FontWeight.Normal))
    "mina" -> FontFamily(Font(R.font.mina, FontWeight.Normal))
    "baloo_da_2" -> FontFamily(Font(R.font.baloo_da_2, FontWeight.Normal))
    else -> FontFamily.Default
}

private fun buildResultMakerTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}

private val ResultMakerShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun appFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Color.White,
    labelColor = SlateText,
    iconColor = SlateMuted,
    selectedContainerColor = TealPrimary,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White,
    selectedTrailingIconColor = Color.White,
    disabledContainerColor = SlateBg,
    disabledLabelColor = SlateMuted,
    disabledLeadingIconColor = SlateMuted,
    disabledTrailingIconColor = SlateMuted,
    disabledSelectedContainerColor = TealContainer
)

@Composable
fun appFilterChipBorder(
    selected: Boolean,
    enabled: Boolean = true
): BorderStroke = FilterChipDefaults.filterChipBorder(
    enabled = enabled,
    selected = selected,
    borderColor = SlateOutline,
    selectedBorderColor = TealPrimary,
    disabledBorderColor = SlateOutline,
    disabledSelectedBorderColor = TealPrimary.copy(alpha = 0.6f),
    borderWidth = 1.dp,
    selectedBorderWidth = 1.5.dp
)

@Composable
fun ResultMakerTheme(
    appFontId: String = "system",
    content: @Composable () -> Unit
) {
    val typography = remember(appFontId) {
        buildResultMakerTypography(appComposeFontFamily(appFontId))
    }
    MaterialTheme(
        colorScheme = ResultMakerColorScheme,
        typography = typography,
        shapes = ResultMakerShapes,
        content = content
    )
}
