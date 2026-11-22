package com.kgps.result.ui.theme

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kgps.result.R
import com.kgps.result.splitMixedScriptRuns

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

private fun fontResIdFor(fontId: String): Int? = when (fontId) {
    "nikosh" -> R.font.nikosh
    "sutonny_mj" -> R.font.sutonny_mj
    "kalpurush" -> R.font.kalpurush
    "solaiman_lipi" -> R.font.solaiman_lipi
    "noto_serif" -> R.font.noto_serif_bengali
    "tiro_bangla" -> R.font.tiro_bangla
    "noto_sans" -> R.font.noto_sans_bengali
    "hind_siliguri" -> R.font.hind_siliguri
    "anek_bangla" -> R.font.anek_bangla
    "mina" -> R.font.mina
    "baloo_da_2" -> R.font.baloo_da_2
    "roboto" -> R.font.roboto
    "times_new_roman" -> R.font.times_new_roman
    "tinos" -> R.font.tinos
    "arimo" -> R.font.arimo
    "plus_jakarta_sans" -> R.font.plus_jakarta_sans
    "merriweather" -> R.font.merriweather
    else -> null
}

fun appComposeFontFamily(fontId: String): FontFamily =
    fontResIdFor(fontId)?.let { FontFamily(Font(it, FontWeight.Normal)) } ?: FontFamily.Default

fun appComposeFontFamily(
    bnFontId: String,
    enFontId: String,
    context: Context?
): FontFamily {
    val cleanBn = bnFontId.trim().ifBlank { "system" }
    val cleanEn = enFontId.trim().ifBlank { "system" }
    if (cleanBn == "system" && cleanEn == "system") return FontFamily.Default

    if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val enResId = fontResIdFor(if (cleanEn == "system") "roboto" else cleanEn)
            val bnResId = fontResIdFor(cleanBn)
            if (enResId != null) {
                val enSysFamily = android.graphics.fonts.FontFamily.Builder(
                    android.graphics.fonts.Font.Builder(context.resources, enResId).build()
                ).build()
                val builder = AndroidTypeface.CustomFallbackBuilder(enSysFamily)
                if (bnResId != null) {
                    val bnSysFamily = android.graphics.fonts.FontFamily.Builder(
                        android.graphics.fonts.Font.Builder(context.resources, bnResId).build()
                    ).build()
                    builder.addCustomFallback(bnSysFamily)
                }
                return FontFamily(androidx.compose.ui.text.font.Typeface(builder.build()))
            }
        } catch (_: Exception) {
            // Fall back below
        }
    }

    return when {
        cleanBn != "system" -> appComposeFontFamily(cleanBn)
        cleanEn != "system" -> appComposeFontFamily(cleanEn)
        else -> FontFamily.Default
    }
}

fun buildMixedScriptAnnotatedString(
    text: String,
    bnFamily: FontFamily,
    enFamily: FontFamily
): AnnotatedString = buildAnnotatedString {
    splitMixedScriptRuns(text).forEach { run ->
        withStyle(SpanStyle(fontFamily = if (run.isBengali) bnFamily else enFamily)) {
            append(run.text)
        }
    }
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
            fontSize = 21.sp,
            lineHeight = 26.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.5.sp,
            lineHeight = 22.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            lineHeight = 19.sp
        ),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.5.sp,
            lineHeight = 20.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 17.5.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 15.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 16.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            lineHeight = 13.sp
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
    appEnglishFontId: String = "system",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val typography = remember(appFontId, appEnglishFontId, context) {
        buildResultMakerTypography(appComposeFontFamily(appFontId, appEnglishFontId, context))
    }
    MaterialTheme(
        colorScheme = ResultMakerColorScheme,
        typography = typography,
        shapes = ResultMakerShapes,
        content = content
    )
}
