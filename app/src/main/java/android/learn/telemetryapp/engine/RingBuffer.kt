package android.learn.telemetryapp.engine

interface RingBufferReader {
   val count: Int

   operator fun get(index: Int): Float

   fun getIterator(firstNumValues: Int): Iterator<Float>

   fun forEachValues(firstNumValues: Int, action: (index: Int, value: Float, sequenceId: Long) -> Unit)
}

/**
 * Backpressure strategy:
 * When usedCapacity is reached, new samples overwrite
 * the oldest samples.
 */
class RingBuffer(private val actualCapacity: Int = 1024) : RingBufferReader {
   private var bufferSize = 0
   private var head = 0
   private var bufferFull: Boolean = false
   private val buffer = FloatArray(actualCapacity)
   private val sequenceBuffer = LongArray(actualCapacity)

   var usedCapacity = actualCapacity
   private var absoluteSequenceCounter = 0L
   override val count: Int
      get() = bufferSize

   override operator fun get(index: Int): Float {
      require(index in 0 until bufferSize)
      val index = getBufferIndex(index)
      return buffer[index]
   }

   fun clear() {
      head = 0
      bufferSize = 0
      bufferFull = false
      absoluteSequenceCounter = 0
      buffer.fill(0f)
      sequenceBuffer.fill(0L)
   }

   fun insertNewValue(value: Float) {
      val bufferIndex = head

      bufferFull = bufferSize == usedCapacity
      buffer[bufferIndex] = value
      sequenceBuffer[bufferIndex] = absoluteSequenceCounter

      absoluteSequenceCounter++
      if (!bufferFull) bufferSize++

      head = (head + 1) % usedCapacity
   }

   private fun set(index: Int, value: Float) {
      val bufferIndex = getBufferIndex(index)
      bufferFull = bufferSize == usedCapacity
      buffer[bufferIndex] = value
      if (!bufferFull) bufferSize++
   }

   private fun getBufferIndex(currentIndex: Int): Int = when (bufferFull) {
      false -> { require(currentIndex <= bufferSize,
         { "The Index your are requesting does not exist yet!" })
         currentIndex}
      true -> {
         (head + currentIndex) % usedCapacity
      }
   }


   override fun forEachValues(
      firstNumValues: Int,
      action: (index: Int, value: Float, sequenceId: Long) -> Unit
   ) {
      usedCapacity = firstNumValues
      val maxAvailableElements = if (bufferFull) usedCapacity else bufferSize
      val totalElementsToIterate: Int = minOf(maxAvailableElements, firstNumValues)
      // This allows us to get the last `firstNumValues` from the buffer
      val start = bufferSize - totalElementsToIterate
      for (currentIndex in 0 until totalElementsToIterate) {
         // this helps to iterate over the ringbuffer in a linear order
         val bufferIndex = getBufferIndex(start + currentIndex)

         action(currentIndex, buffer[bufferIndex], sequenceBuffer[bufferIndex])
      }


   }

   override fun getIterator(firstNumValues: Int): Iterator<Float> {
      return object : Iterator<Float> {
         private var currentIndex = 0
         private val maxAvailableElements = if (bufferFull) usedCapacity else bufferSize
         private val totalElementsToIterate: Int = minOf(maxAvailableElements, firstNumValues)
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

class LongRingBuffer