package android.learn.telemetryapp.engine

import android.learn.telemetryapp.datastructures.RingBuffer
import android.learn.telemetryapp.datastructures.RingBufferReader
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
   private var frequency: Int = 3000

   private var engineJob: Job? = null
   private var clockTime = (1000 / 30).toLong()


   private val cpuBuffer = RingBuffer()

   val cpuValues: RingBufferReader
      get() = cpuBuffer

   val memoryValues: RingBufferReader
      get() = memoryBuffer
   private val memoryBuffer = RingBuffer()

   val networkValues: RingBufferReader
      get() = networkBuffer
   private val networkBuffer = RingBuffer()

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

   private fun insertValues(cpuValue: Float, memoryValue: Float, networkValue: Float) {
      synchronized(lock) {
         cpuBuffer.insertNewValue(cpuValue)
         memoryBuffer.insertNewValue(memoryValue)
         networkBuffer.insertNewValue(networkValue)
         // This is used to Notify ViewModel of state changes
         onUpdate.invoke()
      }
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
      cpuBuffer.clear()
      memoryBuffer.clear()
      networkBuffer.clear()
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
         delay(currentDelay)
      }
   }

   fun stopEngine() {
      engineJob?.cancel()
   }


}