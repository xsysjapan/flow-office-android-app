package jp.co.xsys.flowoffice.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ReaderPrimaryLight,
    onPrimary = ReaderOnPrimaryLight,
    primaryContainer = ReaderPrimaryContainerLight,
    onPrimaryContainer = ReaderOnPrimaryContainerLight,
    background = ReaderBackgroundLight,
    surface = ReaderSurfaceLight,
    onSurface = ReaderOnSurfaceLight,
    surfaceVariant = ReaderSurfaceVariantLight,
    outline = ReaderOutlineLight,
    error = ReaderErrorLight,
    secondary = ReaderSecondaryLight,
    onSecondary = ReaderOnSecondaryLight,
    secondaryContainer = ReaderSecondaryContainerLight,
    onSecondaryContainer = ReaderOnSecondaryContainerLight,
    tertiary = ReaderTertiaryLight,
    onTertiary = ReaderOnTertiaryLight,
    tertiaryContainer = ReaderTertiaryContainerLight,
    onTertiaryContainer = ReaderOnTertiaryContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = ReaderPrimaryDark,
    onPrimary = ReaderOnPrimaryDark,
    primaryContainer = ReaderPrimaryContainerDark,
    onPrimaryContainer = ReaderOnPrimaryContainerDark,
    background = ReaderBackgroundDark,
    surface = ReaderSurfaceDark,
    onSurface = ReaderOnSurfaceDark,
    surfaceVariant = ReaderSurfaceVariantDark,
    outline = ReaderOutlineDark,
    error = ReaderErrorDark,
    secondary = ReaderSecondaryDark,
    onSecondary = ReaderOnSecondaryDark,
    secondaryContainer = ReaderSecondaryContainerDark,
    onSecondaryContainer = ReaderOnSecondaryContainerDark,
    tertiary = ReaderTertiaryDark,
    onTertiary = ReaderOnTertiaryDark,
    tertiaryContainer = ReaderTertiaryContainerDark,
    onTertiaryContainer = ReaderOnTertiaryContainerDark,
)

@Composable
fun FlowOfficeReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ReaderTypography,
        content = content,
    )
}
