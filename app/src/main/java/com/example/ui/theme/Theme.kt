package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldPrimaryDark,
  onPrimary = OnEmeraldPrimaryDark,
  primaryContainer = EmeraldPrimaryContainerDark,
  onPrimaryContainer = OnEmeraldPrimaryContainerDark,
  secondary = GoldSecondaryDark,
  onSecondary = OnGoldSecondaryDark,
  secondaryContainer = GoldSecondaryContainerDark,
  onSecondaryContainer = OnGoldSecondaryContainerDark,
  tertiary = TerracottaTertiaryDark,
  onTertiary = OnTerracottaTertiaryDark,
  tertiaryContainer = TerracottaTertiaryContainerDark,
  onTertiaryContainer = OnTerracottaTertiaryContainerDark,
  background = ObsidianBackgroundDark,
  onBackground = OnObsidianBackgroundDark,
  surface = PineSurfaceDark,
  onSurface = OnPineSurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = OnSurfaceVariantDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
  error = ErrorDark,
  onError = Color(0xFF690005),
  errorContainer = ErrorContainerDark,
  onErrorContainer = OnErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimaryLight,
  onPrimary = OnEmeraldPrimaryLight,
  primaryContainer = EmeraldPrimaryContainerLight,
  onPrimaryContainer = OnEmeraldPrimaryContainerLight,
  secondary = GoldSecondaryLight,
  onSecondary = OnGoldSecondaryLight,
  secondaryContainer = GoldSecondaryContainerLight,
  onSecondaryContainer = OnGoldSecondaryContainerLight,
  tertiary = TerracottaTertiaryLight,
  onTertiary = OnTerracottaTertiaryLight,
  tertiaryContainer = TerracottaTertiaryContainerLight,
  onTertiaryContainer = OnTerracottaTertiaryContainerLight,
  background = AlabasterBackgroundLight,
  onBackground = OnAlabasterBackgroundLight,
  surface = PureSurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
  error = ErrorLight,
  onError = Color.White,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight,
)

val CashPanetShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CashPanetTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = CashPanetShapes,
    content = content,
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  CashPanetTheme(darkTheme = darkTheme, content = content)
}
