package android.learn.telemetryapp

import android.learn.telemetryapp.datastructures.RingBufferReader
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
      action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit
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
      TelemetryChart1(
         reader = viewModel.getCpuReader(),
         frame = frame,
         selectedSequenceId = selectedSequenceId,
         onSelectionChanged = onSelectionChanged,
         onSelectionCleared = onSelectionCleared,
         modifier = Modifier
            .fillMaxSize()
      )
   }
}


@Composable
fun TelemetryChart1(
   reader: RingBufferReader,
   frame: Int,
   selectedSequenceId: Long,
   onSelectionChanged: (Long) -> Unit,
   onSelectionCleared: () -> Unit,
   modifier: Modifier = Modifier,
   style: TelemetryChartStyle = TelemetryChartStyle()
) {
   frame
   val linePath = remember { Path() }
   val fillPath = remember { Path() }
   val textMeasurer = rememberTextMeasurer()

   // OPTIMIZATION: Eliminate runtime text measurement stalls inside the rendering frame
   val labelHeight = remember(style.textStyle) {
      textMeasurer.measure("0.0", style.textStyle).size.height.toFloat()
   }

   Canvas(
      modifier = modifier
         .padding(top = 6.dp)
         .fillMaxSize()
         .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
         .pointerInput(reader, style) {
            detectDragGestures(
//               onDragEnd = { onSelectionCleared() },
//               onDragCancel = { onSelectionCleared() },
               onDrag = { change, _ ->
                  val paddingLeftPx = style.yAxisPadding.toPx()
                  val chartActiveWidth = size.width - paddingLeftPx
                  val normalizedTouchX = (change.position.x - paddingLeftPx).coerceIn(0f, chartActiveWidth)

                  val fraction = normalizedTouchX / chartActiveWidth
                  val targetIndex = (fraction * (style.windowSize - 1f)).toInt()

                  // Direct contract execution lookup loop
                  reader.forEachValues { index, _, sequenceId, _ ->
                     if (index == targetIndex) {
                        onSelectionChanged(sequenceId)
                     }
                  }
               }
            )
         }
   ) {
      val paddingLeft = style.yAxisPadding.toPx()
      val paddingBottom = style.xAxisPadding.toPx()

      val chartWidth = size.width - paddingLeft
      val topPaddingFactor = .01f
      val paddingTop = (size.height - paddingBottom) * topPaddingFactor
      val chartHeight = (size.height - paddingBottom) * (1 - topPaddingFactor)
      val chartHeightBase = chartHeight + paddingTop

      fillPath.reset()
      linePath.reset()

      var snappedX = -1f
      var snappedY = -1f
      var snappedTelemetryValue = 0f

      var lastXvalue = 0f
      var lastYvalue = 0f
      var dynamicScalePeak = 0f
      var scaleFactor = 1f

      // SINGLE COMPACT ITERATION PASS OVER MEMORY CONTRACT BOUNDS
      var isFirstPoint = true
      reader.forEachValues { index, value, sequenceId, currentMaxValue ->
         val currentMaxValue = style.absoluteMaxValue ?: currentMaxValue
         dynamicScalePeak = currentMaxValue
         scaleFactor = if (currentMaxValue > 0f) value / currentMaxValue else 1f

         val x = ((index / (style.windowSize - 1f)) * chartWidth) + paddingLeft
         val y = chartHeight * (1f -  scaleFactor) + paddingTop

         if (isFirstPoint) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, chartHeightBase)
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

      if (!isFirstPoint) {
         fillPath.lineTo(lastXvalue, chartHeightBase)
         fillPath.close()
      }

      // -----------------------------------------------------------------
      // SYNCHRONIZED VECTOR GRID LAYOUT GENERATION PIPELINE
      // -----------------------------------------------------------------
      val yGridStep = 20f
      var yAxisGridPoint = ( Math.floor(( dynamicScalePeak/yGridStep ).toDouble()) * yGridStep ).toFloat()
      while(yAxisGridPoint > 0) {
         val labelText = String.format("%.1f", yAxisGridPoint)
         val measuredText = textMeasurer.measure(labelText, style.textStyle)

         val lineY = chartHeight * (1 - (yAxisGridPoint/dynamicScalePeak)) + paddingTop
         val textY = lineY - (labelHeight / 2f)


         drawText(
            textMeasurer = textMeasurer,
            text = labelText,
            style = style.textStyle,
            topLeft = Offset(paddingLeft - measuredText.size.width - 8.dp.toPx(), textY.coerceAtLeast(0f))
         )

         drawLine(
            color = style.textStyle.color.copy(alpha = 0.12f),
            start = Offset(paddingLeft, lineY),
            end = Offset(paddingLeft + chartWidth, lineY),
            strokeWidth = 1f
         )

         yAxisGridPoint -= yGridStep
         if (yGridStep == 0f) break
      }

      // 2. Synchronized X-Axis Timeline Markers & Vertical Grid Lines
      val xAxisLabels = listOf("-200s", "-100s", "Now")
      xAxisLabels.forEachIndexed { i, labelText ->
         val measuredText = textMeasurer.measure(labelText, style.textStyle)
         val fractionX = i / 2f

         val lineX = paddingLeft + (chartWidth * fractionX)
         var textX = lineX - (measuredText.size.width / 2f)
         textX = if (textX > chartWidth) textX - measuredText.size.width/2f else textX

         val textY = chartHeightBase + 6.dp.toPx()

         drawText(
            textMeasurer = textMeasurer,
            text = labelText,
            style = style.textStyle,
            topLeft = Offset(textX, textY)
         )

         drawLine(
            color = style.textStyle.color.copy(alpha = 0.12f),
            start = Offset(lineX, 0f),
            end = Offset(lineX, chartHeightBase),
            strokeWidth = 1f
         )
      }

      // -----------------------------------------------------------------
      // EXECUTE STYLING GRAPHICS PIPELINE
      // -----------------------------------------------------------------
      drawPath(
         path = fillPath,
         brush = Brush.verticalGradient(style.fillGradientColors, endY = chartHeight)
      )
      drawPath(
         path = fillPath,
         brush = Brush.radialGradient(
            colors = listOf(style.traceColor.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(lastXvalue, lastYvalue),
            radius = chartWidth
         )
      )
      drawPath(
         path = linePath,
         color = style.traceColor,
         style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
      )

      // Interactive Tracking HUD Layer
      if (selectedSequenceId != -1L && snappedX != -1f) {
         drawLine(
            color = style.crosshairColor,
            start = Offset(snappedX, 0f),
            end = Offset(snappedX, chartHeightBase),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
         )

         drawCircle(color = style.anchorNodeColor, radius = 6.dp.toPx(), center = Offset(snappedX, snappedY))
         drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(snappedX, snappedY))

         // Tooltip Rendering Boundary Box Math
         val infoText = String.format("%.2f", snappedTelemetryValue)
         val textLayoutResult = textMeasurer.measure(infoText, style.textStyle.copy(color = Color.Black, fontSize = 12.sp))

         val padding = 8.dp.toPx()
         val tooltipWidth = textLayoutResult.size.width + (padding * 2)
         val tooltipHeight = textLayoutResult.size.height + (padding * 2)

         val tooltipX = if (snappedX + tooltipWidth + 16f > paddingLeft + chartWidth) snappedX - tooltipWidth - 16f else snappedX + 16f
         val tooltipY = (snappedY - tooltipHeight - 16f).coerceIn(16f, chartHeight - tooltipHeight - 16f)

         drawRect(color = Color.White, topLeft = Offset(tooltipX, tooltipY), size = Size(tooltipWidth, tooltipHeight), alpha = 0.95f)
         drawText(textMeasurer = textMeasurer, text = infoText, style = style.textStyle.copy(color = Color.Black, fontSize = 12.sp), topLeft = Offset(tooltipX + padding, tooltipY + padding))
      }
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
      action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit
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
                     fetchDataValues(windowSize) { index, _, sequenceId, _ ->
                        if (index == targetIndex) {
                           // lock onto this id! it will never change for this data point
                           onSelectionChanged(sequenceId)
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
         var scale = 0f
// -------------------------
         // STEP 3: build line path
         var isFirstPoint = true
         // -------------------------
         fetchDataValues(windowSize) { index, value, sequenceId, currentMaxValue ->
            scale = currentMaxValue
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
         val maxY = scale


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