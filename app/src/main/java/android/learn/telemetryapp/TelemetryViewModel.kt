package android.learn.telemetryapp

import android.learn.telemetryapp.datastructures.RingBufferReader
import android.learn.telemetryapp.engine.MetricsEngine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TelemetryViewModel @Inject constructor(private val metricsEngine: MetricsEngine) :
   ViewModel() {

   private var _frame = MutableStateFlow(0)
   val frame = _frame.asStateFlow()



   init {

      metricsEngine.setUpdateListener { _frame.value++ }
//      metricsEngine.startEngine(viewModelScope)
   }


     fun testLoop(){
      viewModelScope.launch(Dispatchers.IO){
      while (true) {
         _frame.value++
      }

      }
   }
   fun <T> withBufferLock(block: () -> T): T {
      return metricsEngine.withCpuBufferLock(block)
   }

   fun getValueTimeStamp(valueIndex: Int) {
      withBufferLock {
         metricsEngine.cpuValues.ringBuffer[valueIndex]
      }
   }
   override fun onCleared() {
      metricsEngine.stopEngine()
      super.onCleared()
   }

   fun setFrequency(frequency: Int) {
      metricsEngine.setFrequency(frequency)
   }

   fun reset() = viewModelScope.launch(Dispatchers.IO) {
      metricsEngine.stopEngine()
      metricsEngine.clearBuffers()
   }
   fun pause() = viewModelScope.launch(Dispatchers.IO) {
      metricsEngine.stopEngine()
   }

   fun resume() = viewModelScope.launch(Dispatchers.IO) {
      metricsEngine.startEngine(viewModelScope)
   }

   fun getCpuReader(): RingBufferReader = metricsEngine.cpuValues.ringBuffer
   fun getNetworkReader(): RingBufferReader = metricsEngine.networkValues.ringBuffer
   fun getMemoryReader(): RingBufferReader = metricsEngine.memoryValues.ringBuffer

   fun forEachCpuValues(firstNumValues: Int, action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit) {
      metricsEngine.cpuValues.ringBuffer.forEachValues(action)
   }

   fun forEachMemoryValues(firstNumValues: Int, action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit) {
      metricsEngine.memoryValues.ringBuffer.forEachValues(action)
   }

   fun forEachNetworkValues(firstNumValues: Int, action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit) {
      metricsEngine.networkValues.ringBuffer.forEachValues(action)
   }
}