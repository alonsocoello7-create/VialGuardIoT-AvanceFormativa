package cl.inacap.vialguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ButtonPrimaryDark,
    secondary = TextSecondary,
    background = BackgroundLight,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun VialGuardIoTTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
