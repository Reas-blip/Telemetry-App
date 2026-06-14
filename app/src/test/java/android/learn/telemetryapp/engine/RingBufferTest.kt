
package android.learn.telemetryapp.engine

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RingBufferTest {

   @Test
   fun `test initial state`() {
      val ringBuffer = RingBuffer(capacity = 3)
      Assertions.assertEquals(0, ringBuffer.count, "Initial count should be 0")
   }

   @Test
   fun `test adding elements below capacity`() {
      val ringBuffer = RingBuffer(capacity = 3)

      ringBuffer.insertNewValue(1.0f)
      ringBuffer.insertNewValue(2.0f)

      Assertions.assertEquals(2, ringBuffer.count)
      Assertions.assertEquals(1.0f, ringBuffer[0])
      Assertions.assertEquals(2.0f, ringBuffer[1])
   }

   @Test
   fun `test out of bounds access throws exception`() {
      val ringBuffer = RingBuffer(capacity = 3)
      ringBuffer.insertNewValue(10.0f)

      // Accessing index equal to or greater than count should throw IllegalArgumentException due to require()
      assertThrows<IllegalArgumentException> {
         ringBuffer[1]
      }
      assertThrows<IllegalArgumentException> {
         ringBuffer[-1]
      }
   }

   @Test
   fun `test clear resets the buffer`() {
      val ringBuffer = RingBuffer(capacity = 3)
      ringBuffer.insertNewValue(5.0f)
      ringBuffer.insertNewValue( 6.0f )

      ringBuffer.clear()

      Assertions.assertEquals(0, ringBuffer.count)
      assertThrows<IllegalArgumentException> { ringBuffer[0] }
   }

   @Test
   fun `test buffer wrapping behavior when full`() {
      val ringBuffer = RingBuffer(capacity = 4)

      // Fill up to usedCapacity
      ringBuffer.insertNewValue(10f)
      ringBuffer.insertNewValue(14f)
      ringBuffer.insertNewValue(20f)
      ringBuffer.insertNewValue(30f)
      ringBuffer.insertNewValue(25f)
      ringBuffer.insertNewValue(36f)

      // Note: Based on current implementation, a 4th write is required to flip `bufferFull` to true
      // due to `bufferFull = bufferSize == usedCapacity` evaluating before updating count.
      ringBuffer.insertNewValue(40f)

      // Check if values overwrite and iterate correctly according to your index logic
      Assertions.assertEquals(25f, ringBuffer[1])
      Assertions.assertEquals(36f, ringBuffer[2])
      Assertions.assertEquals(40f, ringBuffer[3])
   }

   @Test
   fun `test iterator with fewer elements than requested`() {
      val ringBuffer = RingBuffer(capacity = 5)
      ringBuffer.insertNewValue(1.1f)
      ringBuffer.insertNewValue(2.2f)

      // Requesting 5 elements, but only 2 exist
      val iterator = ringBuffer.getIterator(5)

      Assertions.assertTrue(iterator.hasNext())
      Assertions.assertEquals(1.1f, iterator.next())
      Assertions.assertTrue(iterator.hasNext())
      Assertions.assertEquals(2.2f, iterator.next())
      Assertions.assertFalse(iterator.hasNext())

      assertThrows<NoSuchElementException> {
         iterator.next()
      }
   }

   @Test
   fun `test iterator limits to firstNumValues`() {
      val ringBuffer = RingBuffer(capacity = 5)
      ringBuffer.insertNewValue(10f)
      ringBuffer.insertNewValue(20f)
      ringBuffer.insertNewValue(30f)

      // Only request first 2 values
      val iterator = ringBuffer.getIterator(2)

      Assertions.assertTrue(iterator.hasNext())
      Assertions.assertEquals(10f, iterator.next())
      Assertions.assertTrue(iterator.hasNext())
      Assertions.assertEquals(20f, iterator.next())
      Assertions.assertFalse(iterator.hasNext())
   }
}