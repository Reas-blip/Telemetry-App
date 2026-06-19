package android.learn.telemetryapp


import android.learn.telemetryapp.datastructures.RingBufferReader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.popularmovies.ui.theme.TelemetryAppTheme
import dagger.hilt.android.AndroidEntryPoint

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as androidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.learn.telemetryapp.GridLinesConfiguration.FixedCount
import android.learn.telemetryapp.GridLinesConfiguration.FixedStep
import android.util.Log.d
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.absoluteValue
import kotlin.math.floor

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
      modifier = modifier,
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
         withBufferLock = telemetryViewModel::withBufferLock
      )
//
      Text("Network Throughput Stream")
      LiquidGlassTelemetryChart(
         modifier = Modifier.weight(1f),
         getReader = { telemetryViewModel.getNetworkReader() },
         getFrame = { frame.value },
         getSelectedSequenceId = { selectedNetworkId },
         onSelectionChanged = { selectedNetworkId = it },
         onSelectionCleared = { selectedNetworkId = -1L },
         chartStyle = TelemetryChartStyle(
            yGridLinesConfiguration = FixedStep(
               1000f
            )
         ), withBufferLock = telemetryViewModel::withBufferLock
      )
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
   onSelectionCleared: () -> Unit, withBufferLock: (() -> Unit) -> Unit
) {
   val glassNeonGreen = Color(0xFF00FF88)
   val glassSpecularWhite = Color(0xFFE0FFFA)
   val glassPanelBackground = Color(0x22121212) // Slightly deepened for better dark-mode contrast

   Card(
      modifier = modifier
         .padding(16.dp)
         .fillMaxWidth(),
      elevation = CardDefaults.cardElevation(4.dp),
      colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)

   ) {

      //       LAYER 3: Crystal Clear Core Content
      TelemetryChart(
         getReader = getReader,
         getFrame = getFrame,
         getSelectedSequenceId = getSelectedSequenceId,
         onSelectionChanged = onSelectionChanged,
         onSelectionCleared = onSelectionCleared,
         modifier = Modifier
            .fillMaxSize(),
         chartStyle = chartStyle, withBufferLock = withBufferLock
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
   withBufferLock: (() -> Unit) -> Unit, // Pass engine wrapper: engine::withCpuBufferLock
   modifier: Modifier = Modifier,
   chartStyle: TelemetryChartStyle = TelemetryChartStyle()
) {
   // Keep lambda captures up to date across recompositions
   val currentReaderState = rememberUpdatedState(getReader)
   val currentFrameState = rememberUpdatedState(getFrame)
   val currentSelectedIdState = rememberUpdatedState(getSelectedSequenceId)
   val currentLockState = rememberUpdatedState(withBufferLock)
   val density = LocalDensity.current

   AndroidView(
      factory = { context ->
         TelemetrySurfaceView(context, chartStyle).apply {
            // Supply raw data evaluation methods directly to the engine thread
            setTrackers(
               getReader = { currentReaderState.value() },
               getFrame = { currentFrameState.value() },
               getSelectedSequenceId = { currentSelectedIdState.value() },
               withBufferLock = { block -> currentLockState.value.invoke(block) }

            )
            layoutParams = android.view.ViewGroup.LayoutParams(
               android.view.ViewGroup.LayoutParams.MATCH_PARENT,
               android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            setZOrderOnTop(true)
            holder.setFormat(android.graphics.PixelFormat.TRANSPARENT)


            setTextStyle(density = density)
         }
      },
      modifier = modifier
         .padding(top = 6.dp, end = 6.dp)
         .fillMaxSize()
         .pointerInput(chartStyle.windowSize) {
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

                  // Execute touch-to-id evaluation
                  currentReaderState.value().forEachValues { index, _, sequenceId, _ ->
                     if (index == targetIndex) {
                        onSelectionChanged(sequenceId)
                     }
                  }
               }
            )
         },
      update = { view ->
         view.updateStyles(chartStyle)
      }
   )
}

private class TelemetrySurfaceView(
   context: Context,
   private var chartStyle: TelemetryChartStyle
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

   private val clipPath = Path()
   private val clipRect = android.graphics.RectF()
   private val cardCornerRadius =
      12f * context.resources.displayMetrics.density // Match your Card corner radius (e.g., 12dp)
   private var renderThread: Thread? = null

   @Volatile
   private var isRunning = false
   private var density = context.resources.displayMetrics.density

   // Tracking hook lambdas matching original contracts
   private var getReader: (() -> RingBufferReader)? = null
   private var getFrame: (() -> Int)? = null
   private var getSelectedSequenceId: (() -> Long)? = null
   private var withBufferLock: (((() -> Unit)) -> Unit)? = null


   // Pre-allocated Android Framework Graphics components (Zero GC Churn)
   private val linePath = Path()
   private val fillPath = Path()
   private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
   private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
   }

   private var textPaint = chartStyle.textStyle.toNativePaint(getDensity())
   private var tooltipPaint = Paint().apply {
      color = android.graphics.Color.argb(150, 255, 255, 255)
   }
   private val labelHeight = textPaint.fontMetrics.ascent.absoluteValue
   private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = chartStyle.crosshairColor.toArgb()
   }

   private var smoothPeak = -1f

   private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = android.graphics.Color.argb(30, 255, 255, 255) // Thin subtle white lines
   }

   init {
      holder.addCallback(this)
      updatePaintConfigurations()
   }

   private fun getDensity(): Density = Density(
      density = context.resources.displayMetrics.density,
      fontScale = context.resources.configuration.fontScale
   )

   fun setTextStyle(density: Density) {
      textPaint = chartStyle.textStyle.toNativePaint(density)
   }

   fun TextStyle.toNativePaint(density: Density): Paint {
      val composeStyle = this
      return Paint(Paint.ANTI_ALIAS_FLAG).apply {
         isAntiAlias = true

         // 1. Convert Compose Color to Native ARGB Int
         color = composeStyle.color.toArgb()

         // 2. Convert Text Size (Sp) to Pixels using the current density
         with(density) {
            textSize = composeStyle.fontSize.toPx()
         }

         // 3. Handle Font Weight / Style mapping
         val isBold = composeStyle.fontWeight == FontWeight.Bold
         val isItalic = composeStyle.fontStyle == FontStyle.Italic

         style = Paint.Style.FILL

         // Create the native Typeface
         val styleFlag = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
         }
         typeface = Typeface.create(Typeface.DEFAULT, styleFlag)
      }
   }

   fun setTrackers(
      getReader: () -> RingBufferReader,
      getFrame: () -> Int,
      getSelectedSequenceId: () -> Long,
      withBufferLock: (() -> Unit) -> Unit
   ) {
      this.getReader = getReader
      this.getFrame = getFrame
      this.getSelectedSequenceId = getSelectedSequenceId
      this.withBufferLock = withBufferLock
   }

   fun updateStyles(newStyle: TelemetryChartStyle) {
      this.chartStyle = newStyle
      updatePaintConfigurations()
   }

   private fun updatePaintConfigurations() {
      linePaint.color = chartStyle.traceColor.toArgb()
      linePaint.strokeWidth = 4f
      crosshairPaint.color = this@TelemetrySurfaceView.chartStyle.crosshairColor.toArgb()
      crosshairPaint.strokeWidth = 3f
      crosshairPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
      textPaint.color = chartStyle.textStyle.color.toArgb()
      textPaint.textSize = 32f
   }

   override fun surfaceCreated(holder: SurfaceHolder) {
      isRunning = true
      renderThread = Thread(this, "TelemetrySurfaceThread").apply { start() }
   }

   override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

   override fun surfaceDestroyed(holder: SurfaceHolder) {
      isRunning = false
      var retry = true
      while (retry) {
         try {
            renderThread?.join()
            retry = false
         } catch (e: InterruptedException) { /* Keep loop pacing safe */
         }
      }
   }

   override fun run() {
      while (isRunning) {
         val canvas = holder.lockCanvas() ?: continue
         try {
            // Synchronize the frame drawing process with the buffer's writing thread
            withBufferLock?.invoke {
               drawTelemetryFrame(canvas)
            }
         } finally {
            holder.unlockCanvasAndPost(canvas)
         }

         // Match target frequency (e.g., ~120Hz display timing cap)
         try {
            Thread.sleep(8)
         } catch (e: Exception) {
         }
      }
   }

   private fun drawTelemetryFrame(canvas: Canvas) {
      val reader = getReader?.invoke() ?: return
      val selectedSequenceId = getSelectedSequenceId?.invoke() ?: -1L

      // Wipe previous graphics off the frame surface
      canvas.drawColor(Color.Transparent.toArgb(), PorterDuff.Mode.CLEAR)


      val paddingLeft = chartStyle.yAxisPadding.value * resources.displayMetrics.density
      val paddingBottom = chartStyle.xAxisPadding.value * resources.displayMetrics.density
      val topPaddingFactor = 0.01f

      val chartWidth = width - paddingLeft
      val paddingTop = (height - paddingBottom) * topPaddingFactor
      val chartHeight = (height - paddingBottom) * (1 - topPaddingFactor)
      val chartHeightBase = chartHeight + paddingTop

      linePath.rewind()
      fillPath.rewind()

      var snappedX = -1f
      var snappedY = -1f
      var snappedTelemetryValue = 0f
      var lastXvalue = 0f
      var lastYvalue = 0f
      var detectedRawPeak = 0f

      var isFirstPoint = true
      var lastDrawnX = -1f
      val resolution = 1.5f

      val startColor = android.graphics.Color.argb(
         (0.8f * 255).toInt(),
         126,
         19,
         156
      ) // #669C27B0 (40% Alpha Purple)
      val endColor = android.graphics.Color.argb((0.1f * 255).toInt(), 126, 19, 156)

      fillPaint.shader = LinearGradient(
         0f, paddingTop,         // Start at the top of the chart
         0f, chartHeightBase,    // End at the bottom baseline of the chart
         startColor,
         endColor,
         Shader.TileMode.CLAMP
      )

      // Safe to loop directly now; we are protected by withBufferLock inside run()
      reader.forEachValues { index, value, sequenceId, currentMaxValue ->
         val currentMaxValueTarget = chartStyle.absoluteMaxValue ?: currentMaxValue
         detectedRawPeak = currentMaxValueTarget

         if (smoothPeak <= 0f) smoothPeak = currentMaxValueTarget

         val scaleFactor = if (smoothPeak > 0f) value / smoothPeak else 1f
         val x = ((index / (chartStyle.windowSize - 1f)) * chartWidth) + paddingLeft
         val y =
            (chartHeight * (1f - scaleFactor) + paddingTop).coerceIn(paddingTop, chartHeightBase)

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

         canvas.drawPath(fillPath, fillPaint)
         canvas.drawPath(linePath, linePaint)
      }


      when (val config = chartStyle.yGridLinesConfiguration) {
         is FixedStep -> {
            drawFixedStepGridY(
               canvas = canvas,
               dynamicScalePeak = smoothPeak,
               paddingTop = paddingTop,
               paddingLeft = paddingLeft,
               labelHeight = labelHeight,
               chartHeight = chartHeight,
               chartWidth = chartWidth,
               yGridStep = config.stepValue,
               showLabel = config.showLabel,
               textPaint = textPaint,
               gridPaint = gridPaint, // Uses thin grid paint
               density = getDensity()
            )
         }

         is FixedCount -> {
            drawFixedCountGridY(
               canvas = canvas,
               width = chartWidth,
               height = chartHeight,
               paddingLeft = paddingLeft,
               paddingTop = paddingTop,
               lineCount = config.lineCount,
               dynamicScalePeak = smoothPeak,
               labelHeight = labelHeight,
               showLabel = config.showLabel,
               gridPaint = gridPaint,
               textPaint = textPaint,
               density = getDensity()
            )

            // Draw matching vertical grid cuts
            drawFixedCountGridX(
               canvas = canvas,
               width = chartWidth,
               heightBase = chartHeightBase,
               paddingLeft = paddingLeft,
               lineCount = config.lineCount,
               gridPaint = gridPaint
            )
         }
      }

      when (val config = chartStyle.xGridLinesConfiguration) {
         is FixedStep -> {

         }

         is FixedCount -> {
            // Draw matching vertical grid cuts
            drawFixedCountGridX(
               canvas = canvas,
               width = chartWidth,
               heightBase = chartHeightBase,
               paddingLeft = paddingLeft,
               lineCount = config.lineCount,
               gridPaint = gridPaint
            )
         }
      }

      // Render HUD Tracking Overlay Lines
      if (selectedSequenceId != -1L && snappedX != -1f) {
         canvas.drawLine(snappedX, 0f, snappedX, chartHeightBase, crosshairPaint)

         canvas.drawCircle(snappedX, snappedY, 12f, Paint().apply {
            color = androidColor.WHITE
         })

         canvas.drawCircle(snappedX, snappedY, 6f, Paint().apply {
            color = chartStyle.anchorNodeColor.toArgb()
         })

         val infoText = String.format("%.2f", snappedTelemetryValue)
         val textWidth = textPaint.measureText(infoText)

         with(getDensity()) {

            val padding = 8.dp.toPx()
            val tooltipWidth = textWidth + padding * 2
            val tooltipHeight = labelHeight + (padding * 2)

            val tooltipX =
               if (snappedX + tooltipWidth + 16f > paddingLeft + chartWidth) snappedX - tooltipWidth - 16f else snappedX + 16f
            val tooltipY = (snappedY - 16f).coerceIn(
               16f, chartHeight - tooltipHeight - 16f
            )
            val textRect = RectF().apply {
               set(
                  tooltipX,
                  tooltipY,
                  tooltipX + tooltipWidth,
                  tooltipY - tooltipHeight
               )
            }
            canvas.drawRoundRect(textRect, 5.dp.toPx(), 5.dp.toPx(), tooltipPaint)

            canvas.drawText(
               infoText,
               Offset(tooltipX + padding, tooltipY - padding),
               Paint(textPaint).apply { color = androidColor.BLACK })
         }

      }

      // Smooth Peak Lerp Animation Engine
      if (detectedRawPeak > 0f) {
         val lerpFactor = if (detectedRawPeak > smoothPeak) 0.25f else 0.05f
         smoothPeak += (detectedRawPeak - smoothPeak) * lerpFactor
      }
   }
}

private fun drawFixedStepGridY(

   canvas: Canvas,
   dynamicScalePeak: Float,
   paddingTop: Float,
   paddingLeft: Float,
   labelHeight: Float,
   chartHeight: Float,
   chartWidth: Float,
   yGridStep: Float,
   showLabel: Boolean,
   textPaint: Paint,
   gridPaint: Paint,
   density: Density
) {
   var yAxisGridPoint =
      (floor((dynamicScalePeak / yGridStep).toDouble()) * yGridStep).toFloat()
   while (yAxisGridPoint >= 0) {
      val labelText = String.format("%.1f", yAxisGridPoint)
      val textWidth = textPaint.measureText(labelText)

      val lineY = chartHeight * (1 - (yAxisGridPoint / dynamicScalePeak)) + paddingTop

      val textY = if ((lineY - labelHeight - paddingTop) < 0) lineY + labelHeight
      else lineY + (labelHeight / 2f)
      with(density) {
         if (showLabel) {
            canvas.drawText(
               text = labelText,
               bottomLeft = Offset(
                  paddingLeft - textWidth - 8.dp.toPx(),
                  textY.coerceAtLeast(0f)
               ),
               textPaint

            )
         }

      }

      canvas.drawLine(
//         color = chartStyle.textStyle.color.copy(alpha = 0.12f),
         start = Offset(paddingLeft, lineY),
         end = Offset(paddingLeft + chartWidth, lineY),
         paint = gridPaint
      )

      yAxisGridPoint -= yGridStep
      if (yGridStep == 0f) break
   }
}

internal fun Canvas.drawLine(start: Offset, end: Offset, paint: Paint) {
   this.drawLine(start.x, start.y, end.x, end.y, paint)
}

internal fun Canvas.drawText(text: String, bottomLeft: Offset, textPaint: Paint) {
   this.drawText(text, bottomLeft.x, bottomLeft.y, textPaint)
}

private fun drawFixedCountGridY(
   canvas: Canvas,
   width: Float,
   height: Float,
   paddingLeft: Float,
   paddingTop: Float,
   lineCount: Int,
   dynamicScalePeak: Float,
   labelHeight: Float,
   showLabel: Boolean,
   gridPaint: Paint,
   textPaint: Paint,
   density: Density
) {
   val stepY = height / (lineCount - 1)
   for (i in 0 until lineCount) {
      val y = paddingTop + (i * stepY)

      // Calculate data values from rendering pixel values
      val relativeY = (height - (y - paddingTop)) / height
      val dataValue = relativeY * dynamicScalePeak

      val labelText = String.format("%.1f", dataValue)
      val measuredTextWidth = textPaint.measureText(labelText)
      val textY = if ((y - labelHeight - paddingTop) < 0) y + labelHeight
      else y + (labelHeight / 2f)

      if (i == 0) d("ERRor", "y:$y, textY:$textY, labelHeight:$labelHeight")

      canvas.drawLine(
//         color = textStyle.color.copy(alpha = 0.12f)alpha,
         start = Offset(paddingLeft, y),
         end = Offset(width + paddingLeft, y),
//         strokeWidth = 12ff
         paint = gridPaint
      )
      with(density) {

         if (showLabel) {
            canvas.drawText(
               text = labelText,
               bottomLeft = Offset(
                  paddingLeft - measuredTextWidth - 8.dp.toPx(), textY.coerceAtLeast(0f)
               ),
               textPaint = textPaint
            )
         }
      }
   }
}


private fun drawFixedCountGridX(
   canvas: Canvas,
   width: Float,
   heightBase: Float,
   paddingLeft: Float,
   lineCount: Int,
   gridPaint: Paint
) {
   val stepX = width / (lineCount - 1)
   for (i in 0 until lineCount) {
      val x = paddingLeft + (i * stepX)

      canvas.drawLine(
//         color = textStyle.color.copy(alpha = 0.12f),
         start = Offset(x, 0f),
         end = Offset(x, heightBase),
//         strokeWidth = 1f
         paint = gridPaint
      )

      // Implement customizable dynamic text label metrics here if needed
   }
}