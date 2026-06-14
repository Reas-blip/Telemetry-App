package android.learn.telemetryapp

import android.R.attr.paddingLeft
import android.R.attr.x
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.popularmovies.ui.theme.Gradient2
import com.example.popularmovies.ui.theme.GraphAccent
import com.example.popularmovies.ui.theme.TelemetryAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
   val telemetryViewModel: TelemetryViewModel by viewModels()
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
//      enableEdgeToEdge()
      setContent {
         TelemetryAppTheme {
            val frame = telemetryViewModel.frame.collectAsState().value
            Scaffold() { innerPadding ->

               Column(
                  modifier = Modifier
                     .padding(innerPadding)
                     .fillMaxSize()
                     .background(MaterialTheme.colorScheme.surface),
                  verticalArrangement = Arrangement.SpaceEvenly,
                  horizontalAlignment = Alignment.CenterHorizontally
               ) {
//                  Row() {
                  Button(
                     onClick = { telemetryViewModel.resume() },
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                  ) { Text("Start Engine") }

                  Button(
                     onClick = { telemetryViewModel.pause() },
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                  ) { Text("Stop Engine") }

                  LiveDashboard(
                     telemetryViewModel = telemetryViewModel,
                     modifier = Modifier.weight(1f)
                  )

                  Button(
                     onClick = { telemetryViewModel.reset() },
                     modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                  ) { Text("Reset Engine") }
               }
//               }
            }
         }
      }

   }
}


@Composable
fun LiveDashboard(telemetryViewModel: TelemetryViewModel, modifier: Modifier) {
   val frame by telemetryViewModel.frame.collectAsState()

   var selectedCpuId by remember { mutableStateOf(-1L) }
   var selectedNetworkId by remember { mutableStateOf(-1L) }

   Column(
      modifier = modifier
         .background(MaterialTheme.colorScheme.surface.copy(alpha = .3f)),
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      Text("CPU Utilization Matrix")
      LiquidGlassTelemetryChart(
         modifier = Modifier.weight(1f),
         viewModel = telemetryViewModel,
         frame = frame,
         selectedSequenceId = selectedCpuId,
         onSelectionChanged = { selectedCpuId = it },
         onSelectionCleared = { selectedCpuId = -1L },
         // PASSING THE LAMBDA REFERENCE HERE:
         fetchDataValues = { size, action ->
            telemetryViewModel.forEachCpuValues(size, action)
         }
      )

      Text("Network Throughput Stream")
      LiquidGlassTelemetryChart(
         modifier = Modifier.weight(1f),
         viewModel = telemetryViewModel,
         frame = frame,
         selectedSequenceId = selectedNetworkId,
         onSelectionChanged = { selectedNetworkId = it },
         onSelectionCleared = { selectedNetworkId = -1L },
         // PASSING A DIFFERENT METHOD HOOK HERE:
         fetchDataValues = { size, action ->
            telemetryViewModel.forEachNetworkValues(size, action)
         }
      )
   }
}


@Composable
fun LiquidGlassTelemetryChart(
   viewModel: TelemetryViewModel,
   frame: Int,
   selectedSequenceId: Long,
   onSelectionChanged: (Long) -> Unit,
   onSelectionCleared: () -> Unit,
   fetchDataValues: (
      firstNumValues: Int,
      action: (index: Int, value: Float, sequenceId: Long) -> Unit
   ) -> Unit,
   modifier: Modifier = Modifier
) {
   val glassNeonGreen = Color(0xFF00FF88)
   val glassSpecularWhite = Color(0xFFE0FFFA)
   val glassPanelBackground = Color(0x22121212) // Slightly deepened for better dark-mode contrast

   Box(
      modifier = modifier
         .padding(16.dp)
         .fillMaxWidth()
         .clip(RoundedCornerShape(10.dp))
   ) {

      Box(
         modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(10.dp))
            .border(
               width = 1.dp,
               brush = Brush.linearGradient(
                  colors = listOf(
                     glassSpecularWhite.copy(alpha = 0.35f),
                     Color.Transparent,
                     glassNeonGreen.copy(alpha = 0.15f)
                  ),
                  start = Offset(0f, 0f),
                  end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
               ),
               shape = RoundedCornerShape(10.dp)
            )
      )

      // LAYER 3: Crystal Clear Core Content
      TelemetryChart(
         viewModel = viewModel,
         frame = frame,
         selectedSequenceId = selectedSequenceId,
         onSelectionChanged = onSelectionChanged,
         onSelectionCleared = onSelectionCleared,
         fetchDataValues = fetchDataValues,
         modifier = Modifier
            .fillMaxSize()
      )
   }
}


@Composable
fun TelemetryChart(
   viewModel: TelemetryViewModel,
   frame: Int,
   selectedSequenceId: Long,
   onSelectionChanged: (Long) -> Unit,
   onSelectionCleared: () -> Unit,
   fetchDataValues: (
      firstNumValues: Int,
      action: (index: Int, value: Float, sequenceId: Long) -> Unit
   ) -> Unit,
   modifier: Modifier = Modifier
) {

   val linePath = remember { Path() }
   val fillPath = remember { Path() }

   val haptic = LocalHapticFeedback.current
   val textMeasurer = rememberTextMeasurer()
   var lastSequenceId by remember { mutableLongStateOf(-1L) }
   val windowSize = 200
   val xAxisLabelPadding = 40.dp
   val yAxisLabelPadding = 50.dp
   Canvas(
      modifier = modifier
         .fillMaxSize()
         .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
         .pointerInput(Unit) {
            detectDragGestures(
               onDrag = { change, _ ->
                  val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                  val targetIndex = (fraction * (windowSize - 1f)).toInt()

                  viewModel.withBufferLock {
                     fetchDataValues(windowSize) { index, _, sequenceid ->
                        if (index == targetIndex) {
                           // lock onto this id! it will never change for this data point
                           onSelectionChanged(sequenceid)
                        }
                     }
                  }
               }
            )
         }
   ) {
      val paddingLeft = yAxisLabelPadding.toPx()
      val paddingBottom = xAxisLabelPadding.toPx()
      frame
      val chartWidth = size.width - paddingLeft
      val chartHeight = size.height - paddingBottom

      fillPath.reset()
      linePath.reset()

      drawGrid(width = chartWidth, height = chartHeight, offsetX = paddingLeft)
      // Local tracking structural stores to pass info out of the buffer lock safely
      var snappedX = -1f
      var snappedY = -1f
      var snappedTelemetryValue = 0f

      var lastXvalue = 0f
      var lastYvalue = 0f

      viewModel.withBufferLock {

         // -------------------------
         // STEP 1: min/max
         // -------------------------
         val minY = 0f
         var maxY = Float.MIN_VALUE

         fetchDataValues(windowSize) { index, v, sequenceId ->
            // minY = minOf(minY, v)

            maxY = maxOf(maxY, v)
         }
         val scale = maxY

         // -----------------------------------------------------------------
         // DRAW AXIS TEXT LABELS
         // -----------------------------------------------------------------
         val labelStyle = TextStyle(color = Color.Gray, fontSize = 10.sp)

         // 1. Y-Axis Scale Values (Paints Ceiling, Midpoint and Floor baselines)
         val yAxisPoints = listOf(scale, scale / 2f, 0f)
         yAxisPoints.forEachIndexed { i, value ->
            val labelText = String.format("%.1f", value*maxY)
            val measuredText = textMeasurer.measure(labelText, labelStyle)

            // Distribute line points top-down evenly
            val fractionY = i / 2f
            var textY = (chartHeight * fractionY) - (measuredText.size.height / 2f)
            textY = if (textY < 0) measuredText.size.height/2f  else textY

            drawText(
               textMeasurer = textMeasurer,
               text = labelText,
               style = labelStyle,
               topLeft = Offset(paddingLeft - measuredText.size.width - 8.dp.toPx(), textY)
            )
         }

         // 2. X-Axis Timeline Markers (Paints T-199 to T-0 current relative samples track)
         val xAxisLabels = listOf("-200s", "-100s", "Now")
         xAxisLabels.forEachIndexed { i, labelText ->
            val measuredText = textMeasurer.measure(labelText, labelStyle)
            val fractionX = i / 2f

            var textX = paddingLeft + (chartWidth * fractionX) - (measuredText.size.width / 2f)
            textX = if (textX > chartWidth) textX - measuredText.size.width/2f else textX
            val textY = chartHeight + 6.dp.toPx()

            drawText(
               textMeasurer = textMeasurer,
               text = labelText,
               style = labelStyle,
               topLeft = Offset(textX, textY)
            )
         }


         // -------------------------
         // STEP 2: RESET PATHS (IMPORTANT)
         // -------------------------

         var isFirstPoint = true
         // -------------------------
         // STEP 3: build line path
         // -------------------------
         fetchDataValues(windowSize) { index, value, sequenceId ->
            val raw = if (scale != 0f) value / scale else .001f
            val normalized = raw
            Log.d("VALUE", "$value")
            val x = ((index / (windowSize - 1f)) * chartWidth) + paddingLeft

            val y = chartHeight * (1 - normalized)


            if (isFirstPoint) {
               linePath.moveTo(x, y)

               fillPath.moveTo(x, chartHeight)
               // start fill path at baseline
               fillPath.lineTo(x, y)

               isFirstPoint = false
            } else {
               linePath.lineTo(x, y)
               fillPath.lineTo(x, y)
            }
            lastXvalue = x
            lastYvalue = y


            if (selectedSequenceId == sequenceId) {
               snappedTelemetryValue = value
               snappedX = x
               snappedY = y
            }

         }

         // close fill shape
         if (!isFirstPoint) {
            fillPath.lineTo(lastXvalue, chartHeight)
            fillPath.close()
         }


      }
      val lineColor = Color(156, 39, 176) // #9C27B0
      // -------------------------
      // STEP 4: FILL (glow area)
      // -------------------------


      drawPath(
         path = fillPath,
//         blendMode = BlendMode.Screen,
         brush = Brush.verticalGradient(listOf(GraphAccent, Gradient2)
         )
      )
      drawPath(
         path = fillPath,
         brush = Brush.radialGradient(
            colors = listOf(
               lineColor.copy(alpha = 0.35f),
               Color.Transparent
            ),
            center = Offset(lastXvalue, lastYvalue),
            radius = size.width *chartWidth
         )
      )

      // -------------------------
      // STEP 5: LINE (stroke)
      // -------------------------
      drawPath(
         path = linePath, color = lineColor, style = Stroke(width = 2f)
      )

      if (selectedSequenceId != -1L && snappedX != -1f) {

         // HAPTIC DEBOUNCING GUARDRAIL
         if (selectedSequenceId != lastSequenceId) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Micro tactical vibration
            lastSequenceId = selectedSequenceId
         }

         // Draw Magnetic Crosshair Line
         drawLine(
            color = Color.Magenta.copy(alpha = 0.6f),
            start = Offset(snappedX, 0f),
            end = Offset(snappedX, chartHeight),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(
               intervals = floatArrayOf(10f, 10f),
               phase = 0f
            )
         )

         // Draw Snap Anchor Node Dot
         drawCircle(
            color = Color.Green,
            radius = 6.dp.toPx(),
            center = Offset(snappedX, snappedY)
         )
         drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(snappedX, snappedY)
         )

         // Draw Dynamic Floating Tooltip Graphic Window
         val infoText = String.format("%.2f", snappedTelemetryValue)
         val textLayoutResult = textMeasurer.measure(
            text = infoText,
            style = TextStyle(color = Color.Black, fontSize = 12.sp)
         )

         val padding = 8.dp.toPx()
         val tooltipWidth = textLayoutResult.size.width + (padding * 2)
         val tooltipHeight = textLayoutResult.size.height + (padding * 2)

         // Dynamic tooltip positioning offset rules to prevent window clipping bounds failures
         val tooltipX = if (snappedX + tooltipWidth + 16f > chartWidth) {
            snappedX - tooltipWidth - 16f // Flip to the left side of cursor if close to right edge
         } else {
            snappedX + 16f
         }
         val tooltipY = (snappedY - tooltipHeight - 16f).coerceIn(16f, chartHeight - tooltipHeight - 16f)

         // Draw tooltip container background bubble
         drawRect(
            color = Color.White,
            topLeft = Offset(tooltipX, tooltipY),
            size = Size(tooltipWidth, tooltipHeight),
            alpha = 0.9f
         )

         // Draw metadata content text values inside container bounds
         drawText(
            textMeasurer = textMeasurer,
            text = infoText,
            style = TextStyle(color = Color.Black, fontSize = 12.sp),
            topLeft = Offset(tooltipX + padding, tooltipY + padding)
         )
      }
   }
}

fun DrawScope.drawGrid(width: Float, height: Float, offsetX: Float) {

   val gridLines = 6
   val stepY = height / gridLines
   val stepX = width / gridLines

   // horizontal
   for (i in 0..gridLines) {
      val y = i * stepY
      drawLine(
         color = Color.Gray.copy(alpha = 0.2f),
         start = Offset(offsetX, y),
         end = Offset(width + offsetX, y),
         strokeWidth = 1f
      )
   }

   // vertical
   for (i in 0..gridLines) {
      val x = i * stepX
      drawLine(
         color = Color.Gray.copy(alpha = 0.2f),
         start = Offset(x, 0f),
         end = Offset(x, height),
         strokeWidth = 1f
      )
   }
}