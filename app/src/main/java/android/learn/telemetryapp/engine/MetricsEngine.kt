package android.learn.telemetryapp.engine

import android.learn.telemetryapp.TelemetryChart
import android.learn.telemetryapp.datastructures.ChartGeometry
import android.learn.telemetryapp.datastructures.RingBuffer
import android.learn.telemetryapp.datastructures.RingBufferReader
import android.learn.telemetryapp.datastructures.TelemetrySeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


class MetricsEngine @Inject constructor(
   private val cpuGenerator: CpuGenerator,
   private val memoryGenerator: MemoryGenerator,
   private val networkGenerator: NetworkGenerator,
) {
   private var frequency: Int = 120
   private var engineJob: Job? = null
   private var clockTime = (1000 / frequency).toLong()


   private val cpuSeries = TelemetrySeries()

   val cpuValues:  TelemetrySeries
      get() = cpuSeries
   val memoryValues:  TelemetrySeries
      get() = memorySeries

   private val memorySeries = TelemetrySeries()

   val networkValues:  TelemetrySeries
      get() = networkSeries
   private val networkSeries = TelemetrySeries()

   private var onUpdate: () -> Unit = {}

   fun setFrequency(newFrequency: Int) {
      synchronized(lock) {
         frequency = newFrequency
         clockTime = (1000 / newFrequency).toLong()
      }
   }

   fun setUpdateListener(listener: () -> Unit) {
      onUpdate = listener
   }

   private val lock = Any()
   private var lastUiUpdateTime = 0L
   private val uiUpdateIntervalMs = 16L // ~60 FPS update rate
   private fun insertValues(cpuValue: Float, memoryValue: Float, networkValue: Float) {
      synchronized(lock) {
         cpuSeries.push(cpuValue)
         memorySeries.push(memoryValue)
         networkSeries.push(networkValue)
      }
      // This is used to Notify ViewModel of state changes
      onUpdate.invoke()
   }

   fun applyCallback(noOfValues: Int, metricValues: RingBuffer, callback: (Float) -> Unit) {
      synchronized(lock) {
         metricValues.changeBufferSize(noOfValues)
         metricValues.forEachValues { index, metricValue, sequenceId, currentMax -> callback(metricValue) }
      }
   }

   fun startEngine(scope: CoroutineScope) {
      if (engineJob?.isActive == true) return // Already running

      engineJob = scope.launch(Dispatchers.IO) {
         insertGeneratedValues()
      }
   }
   fun clearBuffers() {
      cpuSeries.clear()
      memorySeries.clear()
      networkSeries.clear()
   }
   fun <T> withCpuBufferLock(block: () -> T): T {
      synchronized(lock) {
         return block()
      }
   }
   private suspend fun insertGeneratedValues() {
      while (engineJob?.isActive == true) {
         val cpuValue = cpuGenerator.next()
         val memoryValue = memoryGenerator.next()
         val networkValue = networkGenerator.next()

         insertValues(cpuValue, memoryValue, networkValue)

         // Read clockTime safely outside or inside lock
         val currentDelay = synchronized(lock) { clockTime }

         if (currentDelay <= 0L) {
            delay(1L)
         } else {
            delay(currentDelay)
         }
      }
   }

   fun stopEngine() {
      engineJob?.cancel()
   }


}