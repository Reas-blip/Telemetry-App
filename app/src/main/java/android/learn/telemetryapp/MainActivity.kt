package android.learn.telemetryapp


import android.learn.telemetryapp.datastructures.RingBufferReader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                     telemetryViewModel = telemetryViewModel, modifier = Modifier.weight(1f)
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
   val frame = telemetryViewModel.frame.collectAsState()

   var selectedCpuId by remember { mutableStateOf(-1L) }
   var selectedNetworkId by remember { mutableStateOf(-1L) }

   Column(
      modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .3f)),
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      Text("CPU Utilization Matrix")
      LiquidGlassTelemetryChart(
         modifier = Modifier.weight(1f),
         getReader = { telemetryViewModel.getCpuReader() },
         getFrame = { frame.value },
         getSelectedSequenceId = { selectedCpuId },
         onSelectionChanged = { selectedCpuId = it },
         onSelectionCleared = { selectedCpuId = -1L },
      )
//
//      Text("Network Throughput Stream")
//      LiquidGlassTelemetryChart(
//         modifier = Modifier.weight(1f),
//         getReader = { telemetryViewModel.getNetworkReader() },
//         getFrame = { frame.value },
//         getSelectedSequenceId = { selectedNetworkId },
//         onSelectionChanged = { selectedNetworkId = it },
//         onSelectionCleared = { selectedNetworkId = -1L },
//         chartStyle = TelemetryChartStyle(
//            yGridLinesConfiguration = GridLinesConfiguration.FixedStep(
//               1000f
//            )
//         )
//      )
   }
}


@Composable
fun LiquidGlassTelemetryChart(
   getReader: () -> RingBufferReader,
   getFrame: () -> Int,
   getSelectedSequenceId: () -> Long,
   onSelectionChanged: (Long) -> Unit,
   chartStyle: TelemetryChartStyle = TelemetryChartStyle(),
   modifier: Modifier = Modifier,
   onSelectionCleared: () -> Unit,
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
               width = 1.dp, brush = Brush.linearGradient(
                  colors = listOf(
                     glassSpecularWhite.copy(alpha = 0.35f),
                     Color.Transparent,
                     glassNeonGreen.copy(alpha = 0.15f)
                  ),
                  start = Offset(0f, 0f),
                  end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
               ), shape = RoundedCornerShape(10.dp)
            )
      )

//       LAYER 3: Crystal Clear Core Content
      TelemetryChart(
         getReader = getReader,
         getFrame = getFrame,
         getSelectedSequenceId = getSelectedSequenceId,
         onSelectionChanged = onSelectionChanged,
         onSelectionCleared = onSelectionCleared,
         modifier = Modifier.fillMaxSize(),
         chartStyle = chartStyle
      )
   }
}





@Composable
fun TelemetryChart(
   getReader: () -> RingBufferReader,
   getFrame: () -> Int,
   getSelectedSequenceId: () -> Long,
   onSelectionChanged: (Long) -> Unit,
   onSelectionCleared: () -> Unit,
   modifier: Modifier = Modifier,
   chartStyle: TelemetryChartStyle = TelemetryChartStyle()
) {
   val linePath = remember { Path() }
   val fillPath = remember { Path() }
   val textMeasurer = rememberTextMeasurer()
   val yGridLinesConfiguration = chartStyle.yGridLinesConfiguration
   val xGridLinesConfiguration = chartStyle.xGridLinesConfiguration

   val currentReader by rememberUpdatedState(getReader())


   // 🔥 ANIMATION SLOT: Holds the animated peak across frames without triggering recomposition
   val scaleCache = remember { object { var smoothPeak = -1f } }

   // OPTIMIZATION: Eliminate runtime text measurement stalls inside the rendering frame
   val labelHeight = remember(chartStyle.textStyle) {
      textMeasurer.measure("0.0", chartStyle.textStyle).size.height.toFloat()
   }

   Spacer(
      modifier = modifier
         .padding(top = 6.dp)
         .fillMaxSize()
         .drawBehind {
            val frame = getFrame()
            val reader = currentReader
            val selectedSequenceId = getSelectedSequenceId()

            val paddingLeft = chartStyle.yAxisPadding.toPx()
            val paddingBottom = chartStyle.xAxisPadding.toPx()
            val topPaddingFactor = .01f

            // 🔥 ANIMATION SLOT: Extract the current running animated peak value
            var currentRenderPeak = scaleCache.smoothPeak
            var detectedRawPeak = 0f

            val chartWidth = size.width - paddingLeft
            val paddingTop = (size.height - paddingBottom) * topPaddingFactor
            val chartHeight = (size.height - paddingBottom) * (1 - topPaddingFactor)
            val chartHeightBase = chartHeight + paddingTop

            fillPath.rewind()
            linePath.rewind()

            var snappedX = -1f
            var snappedY = -1f
            var snappedTelemetryValue = 0f

            var lastXvalue = 0f
            var lastYvalue = 0f
            var dynamicScalePeak = 0f
            var scaleFactor = 1f

            var lastDrawnX = -1f
            val resolution = 1.5f
            // SINGLE COMPACT ITERATION PASS OVER MEMORY CONTRACT BOUNDS
            var isFirstPoint = true
            reader.forEachValues { index, value, sequenceId, currentMaxValue ->
               val currentMaxValueTarget = chartStyle.absoluteMaxValue ?: currentMaxValue
               detectedRawPeak = currentMaxValueTarget // Capture the true raw value

               // Fallback for the first frame pass initialization
               if (currentRenderPeak <= 0f) { currentRenderPeak = currentMaxValueTarget }

               // 🔥 ANIMATION SLOT: Scale using the smooth running peak instead of raw value
               scaleFactor = if (currentRenderPeak > 0f) value / currentRenderPeak else 1f

               val x = ((index / (chartStyle.windowSize - 1f)) * chartWidth) + paddingLeft

               // Coerce Y to prevent drawing lines past borders during massive spikes
               val y = (chartHeight * (1f - scaleFactor) + paddingTop).coerceIn(paddingTop, chartHeightBase)

               if (isFirstPoint) {
                  linePath.moveTo(x, y)
                  fillPath.moveTo(x, chartHeightBase)
                  fillPath.lineTo(x, y)
                  isFirstPoint = false

                  lastDrawnX = x
               } else if (x - lastDrawnX >= resolution) {
                  linePath.lineTo(x, y)
                  fillPath.lineTo(x, y)

                  lastDrawnX = x
                  lastXvalue = x
                  lastYvalue = y
               }


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
            var infoText: String? = null
            val tooltipLayout = if (selectedSequenceId != -1L && snappedX != -1f) {
               infoText = String.format("%.2f", snappedTelemetryValue)
               textMeasurer.measure(
                  text = infoText,
                  style = chartStyle.textStyle.copy(color = Color.Black, fontSize = 12.sp)
               )
            } else null

            when (yGridLinesConfiguration) {
               is GridLinesConfiguration.FixedStep -> {

                  val yGridStep = yGridLinesConfiguration.stepValue
                  DrawAutoScaleGrid(
                     currentRenderPeak,
                     textMeasurer,
                     chartStyle,
                     paddingTop,
                     paddingLeft,
                     labelHeight,
                     chartHeight,
                     chartWidth,
                     chartHeightBase,
                     yGridStep,
                     showLabel = yGridLinesConfiguration.showLabel
                  )
               }

               is GridLinesConfiguration.FixedCount -> {

                  drawGrid(
                     width = chartWidth,
                     height = chartHeightBase,
                     offsetX = paddingLeft,
                     xGridLines = yGridLinesConfiguration.lineCount,
                     yGridLines = yGridLinesConfiguration.lineCount,
                     dynamicScalePeak = currentRenderPeak,
                     textMeasurer = textMeasurer,
                     textStyle = chartStyle.textStyle,
                     paddingLeft = paddingLeft,
                     paddingTop = paddingTop,
                     labelHeight = labelHeight,
                     showLabel = yGridLinesConfiguration.showLabel
                  )
               }
            }
            // -----------------------------------------------------------------
            // EXECUTE STYLING GRAPHICS PIPELINE
            // -----------------------------------------------------------------
            if (!isFirstPoint) {
               drawPath(
                  path = fillPath, brush = Brush.verticalGradient(
                     chartStyle.fillGradientColors, endY = chartHeight
                  )
               )
               drawPath(
                  path = fillPath, brush = Brush.radialGradient(
                     colors = listOf(
                        chartStyle.traceColor.copy(alpha = 0.25f), Color.Transparent
                     ), center = Offset(lastXvalue, lastYvalue), radius = chartWidth
                  )
               )
               drawPath(
                  path = linePath,
                  color = chartStyle.traceColor,
                  style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
               )
            }

            // Interactive Tracking HUD Layer
            if (tooltipLayout != null && snappedX != -1f) {
               drawLine(
                  color = chartStyle.crosshairColor,
                  start = Offset(snappedX, 0f),
                  end = Offset(snappedX, chartHeightBase),
                  strokeWidth = 1.5f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
               )

               drawCircle(
                  color = chartStyle.anchorNodeColor,
                  radius = 6.dp.toPx(),
                  center = Offset(snappedX, snappedY)
               )
               drawCircle(
                  color = Color.White, radius = 3.dp.toPx(), center = Offset(snappedX, snappedY)
               )

               // Tooltip Rendering Boundary Box Math


               val padding = 8.dp.toPx()
               val tooltipWidth = tooltipLayout.size.width + (padding * 2)
               val tooltipHeight = tooltipLayout.size.height + (padding * 2)

               val tooltipX =
                  if (snappedX + tooltipWidth + 16f > paddingLeft + chartWidth) snappedX - tooltipWidth - 16f else snappedX + 16f
               val tooltipY = (snappedY - tooltipHeight - 16f).coerceIn(
                  16f, chartHeight - tooltipHeight - 16f
               )

               drawRect(
                  color = Color.White,
                  topLeft = Offset(tooltipX, tooltipY),
                  size = Size(tooltipWidth, tooltipHeight),
                  alpha = 0.95f
               )
               drawText(
                  textMeasurer = textMeasurer,
                  text = infoText ?: "NaN",
                  style = chartStyle.textStyle.copy(color = Color.Black, fontSize = 12.sp),
                  topLeft = Offset(tooltipX + padding, tooltipY + padding)
               )
            }
            // 🔥 ANIMATION SLOT: THE INTERPOLATION ENGINE
            // Run this at the end of every frame to update the cache for the next draw tick
            if (detectedRawPeak > 0f) {
               if (scaleCache.smoothPeak <= 0f) {
                  scaleCache.smoothPeak = detectedRawPeak
               } else {
                  // Fast tracking going up (0.25f), smooth glide going down (0.05f)
                  val lerpFactor = if (detectedRawPeak > scaleCache.smoothPeak) 0.25f else 0.05f
                  scaleCache.smoothPeak += (detectedRawPeak - scaleCache.smoothPeak) * lerpFactor
               }
            }
         }
         .pointerInput(Unit) {
            detectDragGestures(
//               onDragEnd = { onSelectionCleared() },
//               onDragCancel = { onSelectionCleared() },
               onDrag = { change, _ ->
                  val paddingLeftPx = chartStyle.yAxisPadding.toPx()
                  val chartActiveWidth = size.width - paddingLeftPx
                  val normalizedTouchX =
                     (change.position.x - paddingLeftPx).coerceIn(0f, chartActiveWidth)

                  val fraction = normalizedTouchX / chartActiveWidth
                  val targetIndex = (fraction * (chartStyle.windowSize - 1f)).toInt()

                  // Direct contract execution lookup loop
                  currentReader.forEachValues { index, _, sequenceId, _ ->
                     if (index == targetIndex) {
                        onSelectionChanged(sequenceId)
                     }
                  }
               })
         }
   )
}

private fun DrawScope.DrawAutoScaleGrid(
   dynamicScalePeak: Float,
   textMeasurer: TextMeasurer,
   chartStyle: TelemetryChartStyle,
   paddingTop: Float,
   paddingLeft: Float,
   labelHeight: Float,
   chartHeight: Float,
   chartWidth: Float,
   chartHeightBase: Float,
   yGridStep: Float,
   showLabel: Boolean,
) {
   var yAxisGridPoint =
      (Math.floor((dynamicScalePeak / yGridStep).toDouble()) * yGridStep).toFloat()
   while (yAxisGridPoint > 0) {
      val labelText = String.format("%.1f", yAxisGridPoint)
      val measuredText = textMeasurer.measure(labelText, chartStyle.textStyle)

      val lineY = chartHeight * (1 - (yAxisGridPoint / dynamicScalePeak)) + paddingTop
      val textY = lineY - (labelHeight / 2f)


      drawText(
         textMeasurer = textMeasurer,
         text = labelText,
         style = chartStyle.textStyle,
         topLeft = Offset(
            paddingLeft - measuredText.size.width - 8.dp.toPx(), textY.coerceAtLeast(0f)
         )
      )

      if(showLabel) {
         drawLine(
            color = chartStyle.textStyle.color.copy(alpha = 0.12f),
            start = Offset(paddingLeft, lineY),
            end = Offset(paddingLeft + chartWidth, lineY),
            strokeWidth = 1f
         )
      }

      yAxisGridPoint -= yGridStep
      if (yGridStep == 0f) break
   }

   // 2. Synchronized X-Axis Timeline Markers & Vertical Grid Lines
   val xAxisLabels = listOf("-200s", "-100s", "Now")
   xAxisLabels.forEachIndexed { i, labelText ->
      val measuredText = textMeasurer.measure(labelText, chartStyle.textStyle)
      val fractionX = i / 2f

      val lineX = paddingLeft + (chartWidth * fractionX)
      var textX = lineX - (measuredText.size.width / 2f)
      textX = if (textX > chartWidth) textX - measuredText.size.width / 2f else textX

      val textY = chartHeightBase + 6.dp.toPx()

      if (showLabel) {
         drawText(
            textMeasurer = textMeasurer,
            text = labelText,
            style = chartStyle.textStyle,
            topLeft = Offset(textX, textY)
         )
      }

      drawLine(
         color = chartStyle.textStyle.color.copy(alpha = 0.12f),
         start = Offset(lineX, 0f),
         end = Offset(lineX, chartHeightBase),
         strokeWidth = 1f
      )
   }
}


fun DrawScope.drawGrid(
   width: Float,
   height: Float,
   offsetX: Float,
   xGridLines: Int,
   yGridLines: Int,
   dynamicScalePeak: Float,
   textMeasurer: TextMeasurer,
   textStyle: TextStyle,
   paddingLeft: Float,
   paddingTop: Float,
   labelHeight: Float,
   showLabel: Boolean,
) {

   val stepY = height / yGridLines
   val stepX = width / xGridLines

   // horizontal
   for (i in 0..yGridLines) {
      val y = i * stepY

      val labelText = String.format("%.1f", y)
      val measuredText = textMeasurer.measure(labelText, textStyle)


      val textY = (height - ( y + (labelHeight / 2f)))

      drawLine(
         color = Color.Gray.copy(alpha = 0.2f),
         start = Offset(offsetX, y),
         end = Offset(width + offsetX, y),
         strokeWidth = 1f
      )
      if (showLabel) {
         drawText(
            textMeasurer = textMeasurer,
            text = labelText,
            style = textStyle,
            topLeft = Offset(
               paddingLeft - measuredText.size.width - 8.dp.toPx(), textY.coerceAtLeast(0f)
            )
         )
      }

   }

   // vertical
   for (i in 0..xGridLines) {
      val x = i * stepX
      drawLine(
         color = Color.Gray.copy(alpha = 0.2f),
         start = Offset(x, 0f),
         end = Offset(x, height),
         strokeWidth = 1f
      )
   }
}