package android.learn.telemetryapp


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
import android.learn.telemetryapp.datastructures.RingBufferReader
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.round

class TelemetrySurfaceView(
   context: Context,
   private var chartStyle: TelemetryChartStyle
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

   private var renderThread: Thread? = null

   @Volatile
   private var isRunning = false

   private var getReader: (() -> RingBufferReader)? = null
   private var getFrame: (() -> Int)? = null
   private var getSelectedSequenceId: (() -> Long)? = null
   private var withBufferLock: (((() -> Unit)) -> Unit)? = null

   private val linePath = Path()
   private val fillPath = Path()
   private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
   private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

   private var textPaint = chartStyle.textStyle.toNativePaint(getDensity())
   private var tooltipPaint = Paint().apply {
      color = androidColor.argb(180, 20, 20, 25)
   }
   private val labelHeight = textPaint.fontMetrics.ascent.absoluteValue
   private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = chartStyle.crosshairColor.toArgb()
   }

   private var smoothPeak = -1f

   private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.5f
      color = androidColor.argb(35, 255, 255, 255)
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
         color = composeStyle.color.toArgb()
         with(density) {
            textSize = composeStyle.fontSize.toPx()
         }
         val isBold = composeStyle.fontWeight == FontWeight.Bold
         val isItalic = composeStyle.fontStyle == FontStyle.Italic
         style = Paint.Style.FILL
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
      linePaint.strokeWidth = 5f
      crosshairPaint.color = this@TelemetrySurfaceView.chartStyle.crosshairColor.toArgb()
      crosshairPaint.strokeWidth = 3f
      crosshairPaint.pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
      textPaint.color = chartStyle.textStyle.color.toArgb()
      textPaint.textSize = 28f
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
         } catch (e: InterruptedException) { /* No-op safety pacing */
         }
      }
   }

   override fun run() {
      while (isRunning) {
         val canvas = holder.lockCanvas() ?: continue
         try {
            withBufferLock?.invoke {
               drawTelemetryFrame(canvas)
            }
         } finally {
            holder.unlockCanvasAndPost(canvas)
         }
      }
   }

   private fun drawTelemetryFrame(canvas: Canvas) {
      val reader = getReader?.invoke() ?: return
      val selectedSequenceId = getSelectedSequenceId?.invoke() ?: -1L

      canvas.drawColor(Color.Transparent.toArgb(), PorterDuff.Mode.CLEAR)

      val paddingLeft = chartStyle.yAxisPadding.value * resources.displayMetrics.density
      val paddingBottom = chartStyle.xAxisPadding.value * resources.displayMetrics.density
      val topPaddingFactor = 0.02f

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
      val resolution = 1.2f

      val traceNativeColor = chartStyle.traceColor.toArgb()
      val startColor = androidColor.argb(
         (0.35f * 255).toInt(),
         androidColor.red(traceNativeColor),
         androidColor.green(traceNativeColor),
         androidColor.blue(traceNativeColor)
      )
      val endColor = androidColor.argb(
         0,
         androidColor.red(traceNativeColor),
         androidColor.green(traceNativeColor),
         androidColor.blue(traceNativeColor)
      )

      fillPaint.shader = LinearGradient(
         paddingLeft, paddingTop,
         paddingLeft, chartHeightBase,
         startColor,
         endColor,
         Shader.TileMode.CLAMP
      )

      reader.forEachValues { index, value, sequenceId, currentMaxValue ->

         val currentMaxValueTarget = if (chartStyle.autoScale) {
            chartStyle.absoluteMaxValue ?: currentMaxValue
         } else {
            currentMaxValue
         }


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
         fillPath.lineTo(if (lastXvalue != 0f) lastXvalue else paddingLeft, chartHeightBase)
         fillPath.close()

         canvas.drawPath(fillPath, fillPaint)
         canvas.drawPath(linePath, linePaint)
      }

      when (val config = chartStyle.yGridLinesConfiguration) {
         is FixedStep -> {
            drawFixedStepGridY(
               canvas = canvas, dynamicScalePeak = smoothPeak, paddingTop = paddingTop,
               paddingLeft = paddingLeft, labelHeight = labelHeight, chartHeight = chartHeight,
               chartWidth = chartWidth, yGridStep = config.stepValue, showLabel = config.showLabel,
               textPaint = textPaint, gridPaint = gridPaint, density = getDensity()
            )
         }

         is FixedCount -> {
            drawFixedCountGridY(
               canvas = canvas, width = chartWidth, height = chartHeight, paddingLeft = paddingLeft,
               paddingTop = paddingTop, lineCount = config.lineCount, dynamicScalePeak = smoothPeak,
               labelHeight = labelHeight, showLabel = config.showLabel, gridPaint = gridPaint,
               textPaint = textPaint, density = getDensity()
            )
            drawFixedCountGridX(
               canvas = canvas, width = chartWidth, heightBase = chartHeightBase,
               paddingLeft = paddingLeft, lineCount = config.lineCount, gridPaint = gridPaint
            )
         }
      }

      if (selectedSequenceId != -1L && snappedX != -1f) {
         canvas.drawLine(snappedX, 0f, snappedX, chartHeightBase, crosshairPaint)
         canvas.drawCircle(
            snappedX,
            snappedY,
            14f,
            Paint().apply { color = androidColor.WHITE; isAntiAlias = true })
         canvas.drawCircle(
            snappedX,
            snappedY,
            7f,
            Paint().apply { color = chartStyle.anchorNodeColor.toArgb(); isAntiAlias = true })

         val infoText = String.format("%.2f", snappedTelemetryValue)
         val textWidth = textPaint.measureText(infoText)

         with(getDensity()) {
            val padding = 8.dp.toPx()
            val tooltipWidth = textWidth + padding * 2
            val tooltipHeight = labelHeight + (padding * 2)

            val tooltipX =
               if (snappedX + tooltipWidth + 16f > paddingLeft + chartWidth) snappedX - tooltipWidth - 16f else snappedX + 16f
            val tooltipY =
               (snappedY - 16f).coerceIn(1f + paddingTop + tooltipHeight, chartHeightBase)
            val textRect = RectF().apply {
               set(
                  tooltipX,
                  tooltipY,
                  tooltipX + tooltipWidth,
                  tooltipY - tooltipHeight
               )
            }
            canvas.drawRoundRect(textRect, 5.dp.toPx(), 5.dp.toPx(), tooltipPaint)

            tooltipPaint.color = androidColor.argb(220, 25, 25, 30)
            canvas.drawRoundRect(textRect, 6.dp.toPx(), 6.dp.toPx(), tooltipPaint)

            val nativeTextPaint = Paint(textPaint).apply { color = androidColor.WHITE }
            canvas.drawText(
               infoText,
               Offset(tooltipX + padding, tooltipY - padding),
               nativeTextPaint
            )
         }
      }

      if (detectedRawPeak > 0f) {
         val lerpFactor = if (detectedRawPeak > smoothPeak) 0.25f else 0.05f
         smoothPeak += (detectedRawPeak - smoothPeak) * lerpFactor
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

      var yAxisGridPoint = ( round(dynamicScalePeak / yGridStep) * yGridStep)
      while (yAxisGridPoint >= 0) {
         val labelText = String.format("%.1f", yAxisGridPoint)
         val textWidth = textPaint.measureText(labelText)
         val lineY = chartHeight * (1 - (yAxisGridPoint / dynamicScalePeak)) + paddingTop
         val textY =
            if ((lineY - labelHeight - paddingTop) < 0) lineY + labelHeight else lineY + (labelHeight / 2f)

         with(density) {
            if (showLabel) {
               canvas.drawText(
                  text = labelText,
                  bottomLeft = Offset(paddingLeft - textWidth - 8.dp.toPx(), textY.coerceAtLeast(0f)),
                  textPaint
               )
            }
         }
         canvas.drawLine(
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
         val relativeY = (height - (y - paddingTop)) / height
         val dataValue = relativeY * dynamicScalePeak
         val labelText = String.format("%.1f", dataValue)
         val measuredTextWidth = textPaint.measureText(labelText)
         val textY =
            if ((y - labelHeight - paddingTop) < 0) y + labelHeight else y + (labelHeight / 2f)

         canvas.drawLine(
            start = Offset(paddingLeft, y),
            end = Offset(width + paddingLeft, y),
            paint = gridPaint
         )
         with(density) {
            if (showLabel) {
               canvas.drawText(
                  text = labelText,
                  bottomLeft = Offset(
                     paddingLeft - measuredTextWidth - 8.dp.toPx(),
                     textY.coerceAtLeast(0f)
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
         canvas.drawLine(start = Offset(x, 0f), end = Offset(x, heightBase), paint = gridPaint)
      }
   }
}

