package android.learn.telemetryapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as androidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.learn.telemetryapp.GridLinesConfiguration.FixedCount
import android.learn.telemetryapp.GridLinesConfiguration.FixedStep
import android.learn.telemetryapp.datastructures.RingBufferReader
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.popularmovies.ui.theme.TelemetryAppTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.absoluteValue
import kotlin.math.round

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
   val telemetryViewModel: TelemetryViewModel by viewModels()
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         TelemetryAppTheme {
            var engineRunning by remember { mutableStateOf(true) }

            // Shared HazeState to bridge elements across the screen
            val dashboardHazeState = remember { HazeState() }

            // Dynamic telemetry gradient background
            val racingBackgroundGradient = Brush.linearGradient(
               colors = listOf(
                  Color(0xFF0D0F12), // Deep Space Charcoal
                  Color(0xFF141923), // Midnight Blue
                  Color(0xFF090A0C)  // Dark Horizon
               ),
               start = Offset(0f, 0f),
               end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            Box(
               modifier = Modifier
                  .fillMaxSize()
                  .background(racingBackgroundGradient)
                  .hazeSource(state = dashboardHazeState)
            ) {
               Scaffold(
                  modifier = Modifier.fillMaxSize(),
                  containerColor = Color.Transparent,
                  contentWindowInsets = WindowInsets.safeDrawing,
                  topBar = {
                     FloatingTopAppBar(
                        hazeState = dashboardHazeState,
                        engineRunning = engineRunning,
                        onStartEngine = {
                           engineRunning = true
                           telemetryViewModel.resume()
                        },
                        onStopEngine = {
                           engineRunning = false
                           telemetryViewModel.reset()
                        },
                        onPauseEngine = {
                           engineRunning = false
                           telemetryViewModel.pause()
                        },
                        containerColor = Color(0xA62A3250)
                     )
                  }
               ) { innerPadding ->
                  // The hazeSource captures the structural gradient underneath the surface
                  Box(
                     modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                  ) {
                     LiveDashboard(
                        hazeState = dashboardHazeState,
                        telemetryViewModel = telemetryViewModel,
                        modifier = Modifier.fillMaxSize()
                     )
                  }
               }
            }
         }
      }
   }
}

@Composable
fun LiveDashboard(
   hazeState: HazeState,
   telemetryViewModel: TelemetryViewModel,
   modifier: Modifier
) {
   val frame = telemetryViewModel.frame.collectAsState()

   var selectedNetworkId by remember { mutableStateOf(-1L) }

   Column(
      modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
   ) {
      // LEFT DECK: CPU Analytics

      val cpuAccentColor: Color = Color(0xFFA832D7)
      val cpuTitle = "CPU UTILIZATION MATRIX"
      val cpuCurrentValue = "LOAD: ACTIVE"
      TelemetryTile(
         cpuTitle,
         cpuCurrentValue,
         cpuAccentColor,
         hazeState,
         { telemetryViewModel.getCpuReader() },
         { frame.value },
         20f,
         100f,
         telemetryViewModel::withBufferLock,
         Modifier.weight(1f)
      )

      // RIGHT DECK: Network Analytics
      val networkAccentColor: Color = Color(0xFF00FF88)
      val networkTitle: String = "NETWORK THROUGHPUT STREAM"
      val networkCurrentValue: String = "I/O: LIVE"
      TelemetryTile(
         networkTitle,
         networkCurrentValue,
         networkAccentColor,
         hazeState,
         { telemetryViewModel.getNetworkReader() },
         { frame.value },
         1000f,
         7500f,
         telemetryViewModel::withBufferLock,
         Modifier.weight(1f)
      )
   }
}

@Composable
private fun TelemetryTile(
   title: String,
   currentValue: String,
   accentColor: Color,
   hazeState: HazeState,
   getReader: () -> RingBufferReader,
   getFrame: () -> Int,
   defaultStepValue: Float,
   asboluteMax: Float,
   withBufferLock: (() -> Unit) -> Unit,
   modifier: Modifier,
) {
   // REMOVED: var dropDownExpanded by remember { mutableStateOf(false) }

   var autoScaleChipSelected by remember { mutableStateOf(false) }
   var chartStyle by remember {
      mutableStateOf(
         TelemetryChartStyle(
            autoScale = autoScaleChipSelected,
            absoluteMaxValue = asboluteMax,
            traceColor = accentColor,
            yGridLinesConfiguration = FixedStep(defaultStepValue)
         )
      )
   }
   var selectedId by remember { mutableStateOf(-1L) }
   val xGridConfig = chartStyle.xGridLinesConfiguration
   val yGridConfig = chartStyle.yGridLinesConfiguration

   val xGridCount = if (xGridConfig is FixedCount) xGridConfig.lineCount else 0
   val yGridCount = if (yGridConfig is FixedCount) yGridConfig.lineCount else 0

   Column(
      modifier = modifier.fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
   ) {
      DashboardMetricHeader(
         title = title,
         currentValue = currentValue,
         accentColor = accentColor,
         // We no longer pass 'expanded' state down from here!
         autoScaleGraph = autoScaleChipSelected,
         onClickAutoScaleChip = {
            autoScaleChipSelected = !autoScaleChipSelected
            chartStyle = chartStyle.copy(autoScale = autoScaleChipSelected,
               yGridLinesConfiguration = FixedStep(defaultStepValue),)
         },
         xGridCount = xGridCount.toFloat(),
         onXGridCountChange = { xGridCount ->
            chartStyle = chartStyle.copy(xGridLinesConfiguration = FixedCount(xGridCount.toInt()))
         },
         yGridCount = yGridCount.toFloat(),
         onYGridCountChange = { yGridCount ->
            chartStyle = chartStyle.copy(yGridLinesConfiguration = FixedCount(yGridCount.toInt()))
         },
      )

      // NOW SAFE: This canvas chart will NEVER recompose when the dropdown toggles!
      LiquidGlassTelemetryChart(
         modifier = modifier,
         hazeState = hazeState,
         getReader = { getReader() },
         getFrame = { getFrame() },
         getSelectedSequenceId = { selectedId },
         onSelectionChanged = { selectedId = it },
         chartStyle = chartStyle,
         onSelectionCleared = { selectedId = -1L },
         withBufferLock = withBufferLock
      )
   }
}


@Composable
fun DashboardMetricHeader(
   title: String,
   currentValue: String,
   accentColor: Color,
   autoScaleGraph: Boolean,
   onClickAutoScaleChip: () -> Unit,
   xGridCount: Float,
   onXGridCountChange: (Float) -> Unit,
   yGridCount: Float,
   onYGridCountChange: (Float) -> Unit,
) {
   val metricHeaderColor: Color = Color(0x9C1A1E29)
   Card(
      modifier = Modifier
         .fillMaxWidth()
         .padding(horizontal = 16.dp),
      colors = CardDefaults.cardColors(containerColor = metricHeaderColor),
      shape = RoundedCornerShape(12.dp)
   ) {
      Row(
         modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
         horizontalArrangement = Arrangement.SpaceBetween,
         verticalAlignment = Alignment.CenterVertically
      ) {
         DropDown(
            metricHeaderColor,
            autoScaleGraph,
            onClickAutoScaleChip,
            xGridCount,
            onXGridCountChange,
            yGridCount,
            onYGridCountChange
         )
         Column {

            Text(
               text = title,
               style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.5.sp,
                  color = Color(0xFF9EACB8)
               )
            )
         }
         Box(
            modifier = Modifier
               .clip(RoundedCornerShape(6.dp))
               .background(accentColor.copy(alpha = 0.15f))
               .padding(horizontal = 8.dp, vertical = 4.dp)
         ) {
            Text(
               text = currentValue,
               style = MaterialTheme.typography.bodyMedium.copy(
                  color = accentColor,
                  fontWeight = FontWeight.Black,
                  fontStyle = FontStyle.Normal
               )
            )
         }
      }
   }
}

@Composable
private fun DropDown(
   metricHeaderColor: Color,
   autoScaleGraph: Boolean,
   onClickAutoScaleChip: () -> Unit,
   xGridCount: Float,
   onXGridCountChange: (Float) -> Unit,
   yGridCount: Float,
   onYGridCountChange: (Float) -> Unit
) {

   var expanded by remember { mutableStateOf(false) }
   Box(
      modifier = Modifier.background(Color.Transparent)
   ){
      RotatingExpandIcon(
        expanded = expanded,
        onClick = {expanded = !expanded}
    )
      // The floating dropdown menu attached right under the arrow icon
      DropdownMenu(

         expanded = expanded,
         onDismissRequest = { expanded = !expanded},
         modifier = Modifier
            .background(metricHeaderColor.copy(alpha = .7f)) // Matches deep dashboard background
            .padding(10.dp)
            .width(600.dp)
      ) {

         val xGridText = "x"
         val yGridText = "y"
         val containerColor:  Color = Color(0xFF00FF88)
         val chipSelectedColor: Color = Color(0xFF40736E)
         FilterChip(
            selected = autoScaleGraph,
            onClick = onClickAutoScaleChip,
            label = { Text("AutoScale") },
            colors = FilterChipDefaults.filterChipColors(
               selectedContainerColor = chipSelectedColor.copy(alpha = 0.2f),
               selectedLabelColor = chipSelectedColor,
               containerColor = containerColor.copy(alpha = .2f),
               labelColor = containerColor
            )
         )
         Column(modifier = Modifier.fillMaxWidth()) {
            Text("GridLines Count")
            GridCountSlider(xGridText, xGridCount, onXGridCountChange)
            GridCountSlider(yGridText, yGridCount, onYGridCountChange)
         }

      }

   }
}


@Composable
fun RotatingExpandIcon(
   expanded: Boolean,
   onClick: () -> Unit
) {
   // Moving this here limits the recomposition scope strictly to this small component

   val rotation = remember { Animatable(0f) }
   LaunchedEffect(expanded) {
      rotation.animateTo(
         targetValue = if (expanded) 90f else 0f,
         animationSpec = spring(stiffness = Spring.StiffnessLow)
      )
   }
   IconButton(onClick = onClick) {
      Icon(
         imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
         contentDescription = "Filter Options",
         tint = Color.White,
         modifier = Modifier.graphicsLayer {
            rotationZ = rotation.value
         }
      )
   }
}

@Composable
private fun GridCountSlider(
   gridText: String,
   GridCount: Float,
   onGridCountChange: (Float) -> Unit
) {
   Row(horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically)
   {
      Text(gridText, modifier = Modifier.padding(end = 10.dp))

      Slider(
         value = GridCount,
         onValueChange = onGridCountChange,
         valueRange = 4f..10f,
         steps = 6,
         colors = SliderDefaults.colors(
            thumbColor = Color(0xFF00FF88),
            activeTrackColor = Color(0xFF00FF88),
            inactiveTrackColor = Color(0x33FFFFFF)
         ),
      )
   }
}

@Composable
fun LiquidGlassTelemetryChart(
   modifier: Modifier = Modifier,
   hazeState: HazeState,
   getReader: () -> RingBufferReader,
   getFrame: () -> Int,
   getSelectedSequenceId: () -> Long,
   onSelectionChanged: (Long) -> Unit,
   chartStyle: TelemetryChartStyle = TelemetryChartStyle(),
   onSelectionCleared: () -> Unit,
   withBufferLock: (() -> Unit) -> Unit
) {
   Card(
      modifier = modifier
         .padding(horizontal = 16.dp, vertical = 6.dp)
         .fillMaxWidth()
         .hazeEffect(
            state = hazeState,
            style = HazeStyle(
               tint = HazeTint(Color(0x120A0E1A)), // Dark crystalline base tint
               blurRadius = 32.dp,                 // Heavy blur to showcase underlying gradient
               noiseFactor = 0.03f                  // Micro-texture for realism
            )
         ),
      elevation = CardDefaults.cardElevation(0.dp),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent), // Transparent lets haze shine through
      shape = RoundedCornerShape(18.dp)
   ) {
      TelemetryChart(
         getReader = getReader,
         getFrame = getFrame,
         getSelectedSequenceId = getSelectedSequenceId,
         onSelectionChanged = onSelectionChanged,
         onSelectionCleared = onSelectionCleared,
         modifier = Modifier.fillMaxSize(),
         chartStyle = chartStyle,
         withBufferLock = withBufferLock
      )
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingTopAppBar(
   hazeState: HazeState,
   engineRunning: Boolean,
   onStartEngine: () -> Unit,
   onStopEngine: () -> Unit,
   onPauseEngine: () -> Unit,
   containerColor: Color,
) {
   Surface(
      modifier = Modifier
         .padding(horizontal = 16.dp, vertical = 8.dp)
         .fillMaxWidth()
         .windowInsetsPadding(WindowInsets.statusBars)
         .hazeEffect(
            state = hazeState,
            style = HazeStyle(
               tint = HazeTint(Color(0x1A121622)),
               blurRadius = 24.dp
            )
         ),
      shape = RoundedCornerShape(24.dp),
      color = Color.Transparent,
      tonalElevation = 0.dp
   ) {
      CustomTopAppBar(
         modifier = Modifier,
         title = {
            Text(
               text = "TELEMETRY CONTROL DECK",
               style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 2.sp,
                  color = Color.White
               )
            )
         },
         navigationIcon = null,
         actions = {
            Box(
               modifier = Modifier
                  .padding(6.dp)
                  .fillMaxHeight()
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(color = if (engineRunning) Color(0x9C6A3FA9) else Color(0xB500B8F3))
                  .clickable {
                     if (engineRunning) onPauseEngine() else onStartEngine()
                  }
            ) {
               if (engineRunning) Icon(
                  Icons.Rounded.Pause,
                  contentDescription = "Pause Engine",
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(4.dp),
                  tint = Color.White
               )
               else Icon(
                  Icons.Rounded.PlayArrow,
                  contentDescription = "Start Engine",
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(4.dp),
                  tint = Color(0xFF090A0C)
               )
            }
            Box(
               modifier = Modifier
                  .padding(6.dp)
                  .fillMaxHeight()
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(color = Color(0xFFB61830))
                  .clickable {
                     onStopEngine()
                  }) {
               Icon(
                  Icons.Rounded.Stop,
                  contentDescription = "Stop Engine",
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(4.dp),
                  tint = Color.White
               )
            }
         },
         colors = CustomTopAppBarDefaults.colors(
            containerColor = containerColor,
            actionIconContentColor = Color.White
         )
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
   withBufferLock: (() -> Unit) -> Unit,
   modifier: Modifier = Modifier,
   chartStyle: TelemetryChartStyle = TelemetryChartStyle()
) {
   val currentReaderState = rememberUpdatedState(getReader)
   val currentFrameState = rememberUpdatedState(getFrame)
   val currentSelectedIdState = rememberUpdatedState(getSelectedSequenceId)
   val currentLockState = rememberUpdatedState(withBufferLock)
   val density = LocalDensity.current

   AndroidView(
      factory = { context ->
         TelemetrySurfaceView(context, chartStyle).apply {
            setTrackers(
               getReader = { currentReaderState.value() },
               getFrame = { currentFrameState.value() },
               getSelectedSequenceId = { currentSelectedIdState.value() },
               withBufferLock = { block -> currentLockState.value.invoke(block) }
            )
            layoutParams = ViewGroup.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.MATCH_PARENT
            )
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            setTextStyle(density = density)
         }
      },
      modifier = modifier
         .padding(top = 6.dp, end = 6.dp)
         .fillMaxSize()
         .pointerInput(chartStyle.windowSize) {
            detectDragGestures(
               onDragEnd = { onSelectionCleared() },
               onDragCancel = { onSelectionCleared() },
               onDrag = { change, _ ->
                  val paddingLeftPx = chartStyle.yAxisPadding.toPx()
                  val chartActiveWidth = size.width - paddingLeftPx
                  val normalizedTouchX =
                     (change.position.x - paddingLeftPx).coerceIn(0f, chartActiveWidth)
                  val fraction = normalizedTouchX / chartActiveWidth
                  val targetIndex = (fraction * (chartStyle.windowSize - 1f)).toInt()

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

