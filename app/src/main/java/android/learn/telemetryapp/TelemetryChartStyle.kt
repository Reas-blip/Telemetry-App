
package android.learn.telemetryapp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TelemetryChartStyle(
   val traceColor: Color = Color(0xFF9C27B0),
   val fillGradientColors: List<Color> = listOf(Color(0xFF9C27B0).copy(alpha = 0.4f), Color.Transparent),
   val crosshairColor: Color = Color.Magenta.copy(alpha = 0.6f),
   val anchorNodeColor: Color = Color.Green,
   val textStyle: TextStyle = TextStyle(color = Color.Gray, fontSize = 10.sp),
   val yAxisPadding: Dp = 60.dp,
   val xAxisPadding: Dp = 40.dp,
   val windowSize: Int = 200,
   val absoluteMaxValue: Float? = null,
   val yGridLinesConfiguration: GridLinesConfiguration = GridLinesConfiguration.FixedCount(5),
   val xGridLinesConfiguration: GridLinesConfiguration = GridLinesConfiguration.FixedCount(5)
)

sealed interface GridLinesConfiguration {
   /**
    * Fixes the exact numerical distance between lines.
    * Example: A step of 20.0f draws lines at 0, 20, 40, 60...
    */
   data class FixedStep(val stepValue: Float, val showLabel: Boolean = true) : GridLinesConfiguration {
      init { require(stepValue > 0f) { "Step value must be greater than 0" } }
   }

   /**
    * Fixes the exact count of grid lines distributed across the viewport.
    * Example: A count of 5 splits the visible Y-range evenly into 4 zones.
    */
   data class FixedCount(val lineCount: Int, val showLabel: Boolean = true) : GridLinesConfiguration {
      init { require(lineCount >= 2) { "Must have at least 2 grid lines (min and max boundaries)" } }
   }
}
