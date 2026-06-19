package android.learn.telemetryapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.liquidGlass(
   cornerRadius: Dp = 24.dp
): Modifier {
   val shape = RoundedCornerShape(cornerRadius)

   return this
      .clip(shape)
      // 1. The Glass Base: High transparency with a tint of white/silver
      .background(
         Brush.verticalGradient(
            colors = listOf(
               Color.White.copy(alpha = 0.25f),
               Color.White.copy(alpha = 0.05f)
            )
         )
      )
      // 2. The Liquid Glare: A sharp diagonal gloss overlay
      .background(
         Brush.linearGradient(
            colors = listOf(
               Color.White.copy(alpha = 0.15f),
               Color.Transparent,
               Color.White.copy(alpha = 0.05f)
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
         )
      )
      // 3. The Structural Edge: Dual-tone border mimicking light refraction
      .border(
         width = 1.dp,
         brush = Brush.verticalGradient(
            colors = listOf(
               Color.White.copy(alpha = 0.45f), // Bright reflection at the top
               Color.White.copy(alpha = 0.10f)  // Fades out at the bottom
            )
         ),
         shape = shape
      )
}
