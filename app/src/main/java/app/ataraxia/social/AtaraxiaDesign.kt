package app.ataraxia.social

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AtaraxiaTokens {
    val Night = Color(0xFF07110D)
    val Forest = Color(0xFF0D1B15)
    val Moss = Color(0xFF15271F)
    val Raised = Color(0xFF1C3027)
    val Sage = Color(0xFF8DD9A6)
    val SageSoft = Color(0xFFC2EBCF)
    val Ivory = Color(0xFFF5F1E8)
    val Mist = Color(0xFFA6B7AD)
    val Line = Color(0xFF2C4438)
    val Clay = Color(0xFFE7B69E)

    val SmallShape = RoundedCornerShape(14.dp)
    val MediumShape = RoundedCornerShape(22.dp)
    val LargeShape = RoundedCornerShape(30.dp)

    const val QuickMotion = 180
    const val GentleMotion = 260
}

private val AtaraxiaColors = darkColorScheme(
    primary = AtaraxiaTokens.Sage,
    onPrimary = AtaraxiaTokens.Night,
    primaryContainer = AtaraxiaTokens.Raised,
    onPrimaryContainer = AtaraxiaTokens.SageSoft,
    secondary = AtaraxiaTokens.Clay,
    background = AtaraxiaTokens.Night,
    onBackground = AtaraxiaTokens.Ivory,
    surface = AtaraxiaTokens.Forest,
    onSurface = AtaraxiaTokens.Ivory,
    surfaceVariant = AtaraxiaTokens.Moss,
    onSurfaceVariant = AtaraxiaTokens.Mist,
    outline = AtaraxiaTokens.Line
)

private val AtaraxiaType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 48.sp, lineHeight = 50.sp, letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 40.sp, lineHeight = 43.sp, letterSpacing = (-.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
        fontSize = 29.sp, lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 17.sp, lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.6.sp
    )
)

@Composable
internal fun AtaraxiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtaraxiaColors,
        typography = AtaraxiaType,
        shapes = androidx.compose.material3.Shapes(
            small = AtaraxiaTokens.SmallShape,
            medium = AtaraxiaTokens.MediumShape,
            large = AtaraxiaTokens.LargeShape
        ),
        content = content
    )
}