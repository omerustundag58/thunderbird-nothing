package net.thunderbird.components.ui.bolt.theme.default

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.doto_bold
import net.thunderbird.components.ui.bolt.resources.doto_medium
import net.thunderbird.components.ui.bolt.resources.doto_regular
import net.thunderbird.components.ui.bolt.resources.inter_bold
import net.thunderbird.components.ui.bolt.resources.inter_medium
import net.thunderbird.components.ui.bolt.resources.inter_regular
import net.thunderbird.components.ui.bolt.resources.inter_semibold
import net.thunderbird.components.ui.bolt.theme.ThemeTypography
import org.jetbrains.compose.resources.Font

/**
 * Nothing OS inspired typography.
 *
 * Doto (dot matrix) for display/headline/titleLarge, Inter for everything else.
 * Both fonts are licensed under the SIL Open Font License.
 */
@Composable
@Suppress("MagicNumber")
fun defaultTypography(): ThemeTypography {
    val doto = FontFamily(
        Font(resource = Res.font.doto_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.doto_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.doto_bold, weight = FontWeight.Bold),
    )
    val inter = FontFamily(
        Font(resource = Res.font.inter_regular, weight = FontWeight.Normal),
        Font(resource = Res.font.inter_medium, weight = FontWeight.Medium),
        Font(resource = Res.font.inter_semibold, weight = FontWeight.SemiBold),
        Font(resource = Res.font.inter_bold, weight = FontWeight.Bold),
    )

    return ThemeTypography(
        displayLarge = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.2).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = doto,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}
