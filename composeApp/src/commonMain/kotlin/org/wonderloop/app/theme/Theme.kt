package org.wonderloop.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import wonderloop.composeapp.generated.resources.Res
import wonderloop.composeapp.generated.resources.fredoka_semibold
import wonderloop.composeapp.generated.resources.nunito_bold
import wonderloop.composeapp.generated.resources.nunito_extrabold

val CoralPrimary = Color(0xFFFF6B6B)
val CoralHero = Color(0xFFD9384A)
val SunSecondary = Color(0xFFFFD166)
val MintAccent = Color(0xFF06D6A0)
val CreamBackground = Color(0xFFFFFBF5)
val PaperCard = Color(0xFFFFFFFF)
val TextDark = Color(0xFF2B2340)
val TextMuted = Color(0xFF6B6480)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    secondary = SunSecondary,
    tertiary = MintAccent,
    background = CreamBackground,
    surface = PaperCard,
    onPrimary = Color.White,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

val LocalTitleFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }
val LocalUiFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun TitleFont(): FontFamily = LocalTitleFontFamily.current

@Composable
fun UiFont(): FontFamily = LocalUiFontFamily.current

@Composable
fun rememberFredokaFontFamily(): FontFamily {
    val fredoka600 = Font(Res.font.fredoka_semibold, FontWeight.Normal)
    val fredoka600Medium = Font(Res.font.fredoka_semibold, FontWeight.Medium)
    val fredoka600SemiBold = Font(Res.font.fredoka_semibold, FontWeight.SemiBold)
    val fredoka600Bold = Font(Res.font.fredoka_semibold, FontWeight.Bold)
    val fredoka600ExtraBold = Font(Res.font.fredoka_semibold, FontWeight.ExtraBold)
    val fredoka600Black = Font(Res.font.fredoka_semibold, FontWeight.Black)
    return FontFamily(
        fredoka600,
        fredoka600Medium,
        fredoka600SemiBold,
        fredoka600Bold,
        fredoka600ExtraBold,
        fredoka600Black
    )
}

@Composable
fun rememberNunitoFontFamily(): FontFamily {
    val nunito700Normal = Font(Res.font.nunito_bold, FontWeight.Normal)
    val nunito700Medium = Font(Res.font.nunito_bold, FontWeight.Medium)
    val nunito700SemiBold = Font(Res.font.nunito_bold, FontWeight.SemiBold)
    val nunito700Bold = Font(Res.font.nunito_bold, FontWeight.Bold)
    val nunito800ExtraBold = Font(Res.font.nunito_extrabold, FontWeight.ExtraBold)
    val nunito800Black = Font(Res.font.nunito_extrabold, FontWeight.Black)
    return FontFamily(
        nunito700Normal,
        nunito700Medium,
        nunito700SemiBold,
        nunito700Bold,
        nunito800ExtraBold,
        nunito800Black
    )
}

@Composable
fun WonderLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val fredokaFamily = rememberFredokaFontFamily()
    val nunitoFamily = rememberNunitoFontFamily()

    val base = Typography()
    val wonderLoopTypography = Typography(
        // Titles & Headings -> Fredoka 600
        displayLarge = base.displayLarge.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        displayMedium = base.displayMedium.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        displaySmall = base.displaySmall.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        // Buttons & Action Labels -> Fredoka 600
        labelLarge = base.labelLarge.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = fredokaFamily, fontWeight = FontWeight.SemiBold),
        // UI & Stories -> Nunito 700/800
        bodyLarge = base.bodyLarge.copy(fontFamily = nunitoFamily, fontWeight = FontWeight.Bold),
        bodyMedium = base.bodyMedium.copy(fontFamily = nunitoFamily, fontWeight = FontWeight.Bold),
        bodySmall = base.bodySmall.copy(fontFamily = nunitoFamily, fontWeight = FontWeight.Bold)
    )

    CompositionLocalProvider(
        LocalTitleFontFamily provides fredokaFamily,
        LocalUiFontFamily provides nunitoFamily
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = wonderLoopTypography,
            content = content
        )
    }
}
