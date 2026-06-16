package android.learn.telemetryapp.datastructures


interface RingBufferReader {
   val count: Int

   operator fun get(index: Int): Float

   fun getIterator(): Iterator<Float>

   fun forEachValues(action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit)

   fun changeBufferSize(capacity: Int)
}

/**
 * Backpressure strategy:
 * When usedCapacity is reached, new samples overwrite
 * the oldest samples.
 */
class RingBuffer(private val actualCapacity: Int = 200) : RingBufferReader {
   private var bufferSize = 0

   private var _maxValue = 0f
   val maxValue: Float
      get() = _maxValue
   private var head = 0
   private var bufferFull: Boolean = false
   private var buffer = FloatArray(actualCapacity)
   private var sequenceBuffer = LongArray(actualCapacity)
   private var maxDeque = MonotonicMaxQueue()

   var usedCapacity = actualCapacity
   private var absoluteSequenceCounter = 0L
   override val count: Int
      get() = bufferSize

   override fun changeBufferSize(capacity: Int) {
      require(capacity > 0, { "capacity must be greater than 0" })
      if (capacity == usedCapacity) return
      _maxValue = 0f
      val newBuffer = FloatArray(capacity)

      val newSequenceBuffer = LongArray(capacity)

      val elementsToCopy = minOf(capacity, bufferSize)

      maxDeque.clear()
      for (index in 0 until elementsToCopy) {
         val oldBufferIndex = getBufferIndex(bufferSize - elementsToCopy + index)

         newBuffer[index] = buffer[oldBufferIndex]
         maxDeque.insert(sequenceBuffer[oldBufferIndex], buffer[oldBufferIndex], index + 1)
         newSequenceBuffer[index] = sequenceBuffer[oldBufferIndex]

      }
      println("change ${newBuffer.toList()}")

      bufferFull = elementsToCopy == capacity
      buffer = newBuffer
      sequenceBuffer = newSequenceBuffer
      usedCapacity = capacity
      head = 0
      bufferSize = elementsToCopy

      if (bufferSize > 0) {
         val latestSequence = newSequenceBuffer[bufferSize - 1]
         _maxValue = maxDeque.max(latestSequence, bufferSize)
      }

   }

   override operator fun get(index: Int): Float {
      require(index in 0 until bufferSize)
      println("buffi ${buffer[index]}")
      val index = getBufferIndex(index)
      println("index$index")
      return buffer[index]
   }

   fun clear() {
      head = 0
      bufferSize = 0
      bufferFull = false
      absoluteSequenceCounter = 0
      buffer.fill(0f)
      sequenceBuffer.fill(0L)
      _maxValue = 0f
      maxDeque.clear()
   }

   fun insertNewValue(value: Float) {
      bufferFull = bufferSize == usedCapacity

      if (bufferFull) {
         buffer[head] = value
         sequenceBuffer[head] = absoluteSequenceCounter
         head = (head + 1) % usedCapacity
      } else {
         val insertionIndex = (head + bufferSize) % usedCapacity
         buffer[insertionIndex] = value
         sequenceBuffer[insertionIndex] = absoluteSequenceCounter
         bufferSize++
      }

      maxDeque.insert(absoluteSequenceCounter, value, bufferSize)
      _maxValue = maxDeque.max(absoluteSequenceCounter, bufferSize)
      absoluteSequenceCounter++
   }

   // Function is only used during tests
   private fun set(index: Int, value: Float) {
      val bufferIndex = getBufferIndex(index)
      bufferFull = bufferSize == usedCapacity
      buffer[bufferIndex] = value
      if (!bufferFull) bufferSize++
   }

   private fun getBufferIndex(currentIndex: Int, capacity: Int = usedCapacity): Int {
      require(currentIndex in 0..<bufferSize, { "Invalid Index" })
      return (head + currentIndex) % capacity
   }


   override fun forEachValues(
      action: (index: Int, value: Float, sequenceId: Long, currentMaxValue: Float) -> Unit
   ) {
      val totalElementsToIterate: Int = if (bufferFull) usedCapacity else bufferSize
      val start = bufferSize - totalElementsToIterate
      for (currentIndex in 0 until totalElementsToIterate) {
         // this helps to iterate over the ringbuffer in a linear order
         val bufferIndex = getBufferIndex(start + currentIndex)

         action(currentIndex, buffer[bufferIndex], sequenceBuffer[bufferIndex], maxValue)
      }


   }

   override fun getIterator(): Iterator<Float> {
      return object : Iterator<Float> {
         private var currentIndex = 0
         private val totalElementsToIterate: Int = if (bufferFull) usedCapacity else bufferSize
         override fun next(): Float {
            if (!hasNext()) throw NoSuchElementException("End of Iterator Reached")

            val next = buffer[getBufferIndex(currentIndex)]
            currentIndex++
            return next
         }

         override fun hasNext(): Boolean {
            return currentIndex < totalElementsToIterate
         }

      }
   }
}
