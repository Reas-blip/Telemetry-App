package android.learn.telemetryapp.datastructures

class TelemetrySeries(
   capacity: Int = 200, private val absoluteMax: Float? = null
) {
   val ringBuffer = RingBuffer(capacity)

   val geometry = ChartGeometry(capacity)

   fun push(
      value: Float
   ) {
      ringBuffer.insertNewValue(value)
      val maxValue = absoluteMax ?: ringBuffer.maxValue
      val y = (1f - (value / maxValue).coerceIn(0f, 1f))

      geometry.push(y)
   }

   fun clear() {
      geometry.clear()
      ringBuffer.clear()
   }
}

class ChartGeometry(
   capacity: Int
) {
   var capacity = capacity
      private set

   private val yCoordinates = FloatArray(capacity)

   var head = 0
      private set

   var count = 0
      private set

   fun clear() {
      head = 0
      count = 0
      yCoordinates.fill(0f)

   }
   fun push(y: Float) {
      yCoordinates[head] = y

      head = (head + 1) % yCoordinates.size

      if (count < yCoordinates.size) {
         count++
      }
   }

   fun forEach(
      action: (index: Int, y: Float) -> Unit
   ) {
      val start = if (count == yCoordinates.size) head
      else 0

      for (i in 0 until count) {
         val actualIndex = (start + i) % yCoordinates.size

         action(i, yCoordinates[actualIndex])
      }
   }
}