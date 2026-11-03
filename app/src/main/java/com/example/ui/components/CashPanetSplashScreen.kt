package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

private val SplashEmeraldCenter = Color(0xFF046E3B)
private val SplashEmeraldBase = Color(0xFF026134)
private val SplashEmeraldEdge = Color(0xFF014524)
private val SplashCoinGold = Color(0xFFF3C62F)

@Composable
fun CashPanetSplashScreen(
  modifier: Modifier = Modifier,
) {
  var entered by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    entered = true
  }

  val logoScale by animateFloatAsState(
    targetValue = if (entered) 1f else 0.88f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow,
    ),
    label = "splash_logo_scale",
  )

  val contentAlpha by animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
    label = "splash_content_alpha",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(
            SplashEmeraldCenter,
            SplashEmeraldBase,
            SplashEmeraldEdge,
          )
        )
      )
      .testTag("cashpanet_splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(32.dp)
        .alpha(contentAlpha),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_cashpanet_splash_logo),
        contentDescription = stringResource(R.string.cd_splash_logo),
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .size(168.dp)
          .scale(logoScale)
          .clip(RoundedCornerShape(38.dp))
          .testTag("splash_logo_image"),
      )

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )

      Spacer(modifier = Modifier.height(24.dp))

      LinearProgressIndicator(
        modifier = Modifier
          .width(120.dp)
          .height(3.dp)
          .clip(RoundedCornerShape(50)),
        color = SplashCoinGold,
        trackColor = Color.White.copy(alpha = 0.2f),
        strokeCap = StrokeCap.Round,
      )
    }
  }
}
