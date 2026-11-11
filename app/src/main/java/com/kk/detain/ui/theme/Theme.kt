package com.kk.detain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Mist = Color(0xFFF5F6FB)
val Cloud = Color(0xFFFFFFFF)
val Ink = Color(0xFF262A36)
val InkSoft = Color(0xFF737A8C)
val InkFaint = Color(0xFFA6ACBC)
val Periwinkle = Color(0xFF7A86F8)
val PeriwinkleDeep = Color(0xFF5B67E6)
val PeriwinkleSoft = Color(0xFFE9EBFF)
val Mint = Color(0xFF3FAE92)
val MintSoft = Color(0xFFE0F6EE)
val Peach = Color(0xFFEA947C)
val PeachSoft = Color(0xFFFFEDE5)
val SandSoft = Color(0xFFFBF3E3)
val Rose = Color(0xFFE07A88)
val RoseSoft = Color(0xFFFCE8EB)
val Outline = Color(0xFFE4E7F0)
val OutlineFaint = Color(0xFFF0F2F8)

private val LightColors = lightColorScheme(
    primary = Periwinkle,
    onPrimary = Color.White,
    primaryContainer = PeriwinkleSoft,
    onPrimaryContainer = Color(0xFF2F3563),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = MintSoft,
    onSecondaryContainer = Color(0xFF16463A),
    tertiary = Peach,
    onTertiary = Color.White,
    tertiaryContainer = PeachSoft,
    onTertiaryContainer = Color(0xFF5C2C1F),
    background = Mist,
    onBackground = Ink,
    surface = Cloud,
    onSurface = Ink,
    surfaceVariant = OutlineFaint,
    onSurfaceVariant = InkSoft,
    surfaceContainer = Color(0xFFFAFBFE),
    surfaceContainerLow = Color(0xFFFCFDFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outline = Outline,
    outlineVariant = OutlineFaint,
    error = Rose,
    errorContainer = RoseSoft,
    onErrorContainer = Color(0xFF5C1F27),
    inverseSurface = Ink,
    inverseOnSurface = Color.White,
)

private val DetainTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 29.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val DetainShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun DetainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DetainTypography,
        shapes = DetainShapes,
        content = content,
    )
}
