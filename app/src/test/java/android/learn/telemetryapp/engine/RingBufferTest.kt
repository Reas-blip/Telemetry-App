package android.learn.telemetryapp.engine

import android.learn.telemetryapp.datastructures.RingBuffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RingBufferTest {

   private lateinit var ringBuffer: RingBuffer

   @BeforeEach
   fun setUp() {
      // Initialize a clean buffer with capacity 3 before each test
      ringBuffer = RingBuffer(actualCapacity = 3)
   }

   @Test
   fun `test initial state is empty`() {
      assertEquals(0, ringBuffer.count)
      assertEquals(3, ringBuffer.usedCapacity)
      assertThrows<IllegalArgumentException> { ringBuffer[0] }
   }

   @Test
   fun `test insertion up to capacity`() {
      ringBuffer.insertNewValue(10.0f)
      ringBuffer.insertNewValue(20.0f)
      ringBuffer.insertNewValue(30.0f)

      assertEquals(3, ringBuffer.count)
      assertEquals(10.0f, ringBuffer[0])
      assertEquals(20.0f, ringBuffer[1])
      assertEquals(30.0f, ringBuffer[2])
   }

   @Test
   fun `test circular overwrite when capacity is exceeded`() {
      ringBuffer.insertNewValue(10.0f) // sequence 0
      ringBuffer.insertNewValue(20.0f) // sequence 1
      ringBuffer.insertNewValue(30.0f) // sequence 2

      // This should overwrite 10.0f (index 0 logically becomes 20.0f)
      ringBuffer.insertNewValue(40.0f) // sequence 3

      assertEquals(3, ringBuffer.count, "Size should remain locked at maximum capacity")
      assertEquals(20.0f, ringBuffer[0], "Oldest item should now be 20.0f")
      assertEquals(30.0f, ringBuffer[1])
      assertEquals(40.0f, ringBuffer[2], "Newest item should be 40.0f")
   }

   @Test
   fun `test indexing out of bounds throws exception`() {
      ringBuffer.insertNewValue(1.0f)

      assertThrows<IllegalArgumentException> { ringBuffer[-1] }
      assertThrows<IllegalArgumentException> { ringBuffer[1] }
   }

   @Test
   fun `test clear resets all states`() {
      ringBuffer.insertNewValue(1.0f)
      ringBuffer.insertNewValue(2.0f)
      ringBuffer.clear()

      assertEquals(0, ringBuffer.count)
      assertThrows<IllegalArgumentException> { ringBuffer[0] }
   }

   @Test
   fun `test forEachValues executes closure chronologically with correct sequences`() {
      ringBuffer.insertNewValue(10.0f) // seq 0
      ringBuffer.insertNewValue(20.0f) // seq 1
      ringBuffer.insertNewValue(30.0f) // seq 2
      ringBuffer.insertNewValue(40.0f) // seq 3 (overwrites 10.0f)

      val items = mutableListOf<Float>()
      val sequences = mutableListOf<Long>()
      val logicalIndices = mutableListOf<Int>()

      ringBuffer.forEachValues { index, value, sequenceId, _ ->
         logicalIndices.add(index)
         items.add(value)
         sequences.add(sequenceId)
      }

      // Logical index must always read linearly from 0 to 2
      assertEquals(listOf(0, 1, 2), logicalIndices)
      // Values are ordered from oldest remaining to newest
      assertEquals(listOf(20.0f, 30.0f, 40.0f), items)
      // Sequence IDs accurately preserved even across wrap-around overwrites
      assertEquals(listOf(1L, 2L, 3L), sequences)
   }

   @Test
   fun `test iterator yields items in chronological order`() {
      ringBuffer.insertNewValue(1.0f)
      ringBuffer.insertNewValue(2.0f)
      ringBuffer.insertNewValue(3.0f)
      ringBuffer.insertNewValue(4.0f) // Overwrites 1.0f

      val iterator = ringBuffer.getIterator()

      assertTrue(iterator.hasNext())
      assertEquals(2.0f, iterator.next())
      assertTrue(iterator.hasNext())
      assertEquals(3.0f, iterator.next())
      assertTrue(iterator.hasNext())
      assertEquals(4.0f, iterator.next())
      assertFalse(iterator.hasNext())

      assertThrows<NoSuchElementException> { iterator.next() }
   }

   @Test
   fun `test changeBufferSize expands capacity without destroying order`() {
      ringBuffer.insertNewValue(10.0f)
      ringBuffer.insertNewValue(20.0f)
      ringBuffer.insertNewValue(30.0f)
      ringBuffer.insertNewValue(40.0f) // Buffer state: [40, 20, 30], logical oldest is 20

      // Expand capacity from 3 to 5
      ringBuffer.changeBufferSize(5)

      assertEquals(5, ringBuffer.usedCapacity)
      assertEquals(3, ringBuffer.count, "Item count should remain the same after expanding")

      // Assert order is cleanly rewritten linearly to the fresh array block
      assertEquals(20.0f, ringBuffer[0])
      assertEquals(30.0f, ringBuffer[1])
      assertEquals(40.0f, ringBuffer[2])

      // Add more elements to verify new capacity constraints
      ringBuffer.insertNewValue(50.0f)
      ringBuffer.insertNewValue(60.0f)
      assertEquals(5, ringBuffer.count)
      assertEquals(60.0f, ringBuffer[4])
   }

   @Test
   fun `test changeBufferSize shrinks capacity dropping oldest elements`() {
      ringBuffer.insertNewValue(10.0f) // seq 0
      ringBuffer.insertNewValue(20.0f) // seq 1
      ringBuffer.insertNewValue(30.0f) // seq 2
      ringBuffer.insertNewValue(40.0f) // seq 3 (Overwrites 10.0f. Buffer: 20, 30, 40)

      // Shrink capacity down to 2 items max
      ringBuffer.changeBufferSize(2)

      assertEquals(2, ringBuffer.usedCapacity)
      assertEquals(2, ringBuffer.count, "Size should drop to fit new capacity max limit")

      // The oldest remaining item (20.0f) should be safely discarded to fit the layout
      assertEquals(30.0f, ringBuffer[0])
      assertEquals(40.0f, ringBuffer[1])
   }
}

class RingBufferEdgeTestCase {

   private lateinit var ringBuffer: RingBuffer

   @BeforeEach
   fun setUp() {
      // Start with a standard capacity of 4 for edge testing
      ringBuffer = RingBuffer(actualCapacity = 4)
   }

   @Test
   fun `edge case - changing buffer size to zero or negative throws exception`() {
      // Guard against internal array sizes of 0 or less which cause instant crashes on write
      assertThrows<IllegalArgumentException> {
         ringBuffer.changeBufferSize(0)
      }
      assertThrows<IllegalArgumentException> {
         ringBuffer.changeBufferSize(-5)
      }
   }

   @Test
   fun `edge case - changeBufferSize on completely empty buffer`() {
      // Test expanding an empty buffer
      ringBuffer.changeBufferSize(10)
      assertEquals(10, ringBuffer.usedCapacity)
      assertEquals(0, ringBuffer.count)

      // Test shrinking an empty buffer
      ringBuffer.changeBufferSize(2)
      assertEquals(2, ringBuffer.usedCapacity)
      assertEquals(0, ringBuffer.count)

      // Ensure it can still accept values perfectly after empty modifications
      ringBuffer.insertNewValue(99.9f)
      assertEquals(1, ringBuffer.count)
      assertEquals(99.9f, ringBuffer[0])
   }

   @Test
   fun `edge case - expanding buffer while completely full handles getBufferIndex math cleanly`() {
      // Fill the buffer to maximum capacity (4/4) so bufferFull becomes true
      ringBuffer.insertNewValue(10f) // index 0
      ringBuffer.insertNewValue(20f) // index 1
      ringBuffer.insertNewValue(30f) // index 2
      ringBuffer.insertNewValue(40f) // index 3

      // Force a wrap-around overwrite to shift the 'head' pointer away from 0
      ringBuffer.insertNewValue(50f) // Overwrites 10f. Layout: [50, 20, 30, 40], head = 1

      // Expand capacity from 4 to 8.
      // This explicitly catches the state mutation bug where getBufferIndex might mix up old vs new capacity.
      ringBuffer.changeBufferSize(8)

      assertEquals(8, ringBuffer.usedCapacity)
      assertEquals(4, ringBuffer.count, "Count must preserve the 4 elements")

      // Assert that the chronological order was safely extracted and flattened
      assertEquals(20f, ringBuffer[0])
      assertEquals(30f, ringBuffer[1])
      assertEquals(40f, ringBuffer[2])
      assertEquals(50f, ringBuffer[3])
   }

   @Test
   fun `edge case - shrinking buffer below current size drops oldest records correctly`() {
      ringBuffer.insertNewValue(10f)
      ringBuffer.insertNewValue(20f)
      ringBuffer.insertNewValue(30f)
      ringBuffer.insertNewValue(40f)

      // Wrap around once
      ringBuffer.insertNewValue(50f) // Buffer state: [50, 20, 30, 40], Oldest is 20f

      // Shrink capacity down to 2. It must drop the two oldest remaining elements (20f and 30f)
      ringBuffer.changeBufferSize(2)

      assertEquals(2, ringBuffer.usedCapacity)
      assertEquals(2, ringBuffer.count)

      // Validating remaining timeline matches newest telemetry packets
      assertEquals(40f, ringBuffer[0])
      assertEquals(50f, ringBuffer[1])
   }

   @Test
   fun `edge case - verifying sequence tracking continuity across clear steps`() {
      ringBuffer.insertNewValue(1.1f) // sequenceId = 0
      ringBuffer.insertNewValue(2.2f) // sequenceId = 1

      // Clear down the engine
      ringBuffer.clear()
      assertEquals(0, ringBuffer.count)

      // Re-inserting elements must safely start sequence counters back at 0 to avoid drift gaps
      ringBuffer.insertNewValue(3.3f)

      var parsedSequenceId = -1L
      ringBuffer.forEachValues { _, _, sequenceId, _ ->
         parsedSequenceId = sequenceId
      }

      assertEquals(0L, parsedSequenceId, "Sequence tracking must reset to 0L following a clear action")
   }
}


class RingBufferMaxIntegrationTest {

   private lateinit var ringBuffer: RingBuffer

   @BeforeEach
   fun setUp() {
      // Initialize with a small capacity to easily force wrap-around conditions
      ringBuffer = RingBuffer(actualCapacity = 4)
   }

   @Test
   fun testUpsizingBufferPreservesMaxAndTracksCorrectly() {
      // Fill a small capacity-3 buffer
      ringBuffer.insertNewValue(10f)
      ringBuffer.insertNewValue(50f) // Current Max
      ringBuffer.insertNewValue(20f)
      assertEquals(50f, ringBuffer.maxValue, 0.0f)

      // Expand capacity dynamically to 6
      ringBuffer.changeBufferSize(6)

      // Max should still be 50f
      assertEquals(50f, ringBuffer.maxValue, 0.0f)
      assertEquals(3, ringBuffer.count)

      // Insert new values into the newly expanded space
      ringBuffer.insertNewValue(15f)
      ringBuffer.insertNewValue(70f) // New Absolute Max
      ringBuffer.insertNewValue(50f)
      assertEquals(70f, ringBuffer.maxValue, 0.0f)
      ringBuffer.insertNewValue(100f)

      assertEquals(100f, ringBuffer.maxValue, 0.0f)
      assertEquals(6, ringBuffer.count)
   }
   @Test
   fun testInitialFillingTracksMaxCorrectly() {
      // Stream: [10.0, 45.5, 20.0]
      ringBuffer.insertNewValue(10.0f)
      assertEquals(10.0f, ringBuffer.maxValue, 0.0f)

      ringBuffer.insertNewValue(45.5f)
      assertEquals(45.5f, ringBuffer.maxValue, 0.0f)

      ringBuffer.insertNewValue(20.0f)
      // 45.5 should remain the max even though a smaller value was appended
      assertEquals(45.5f, ringBuffer.maxValue, 0.0f)
      assertEquals(3, ringBuffer.count)
   }

   @Test
   fun testMaxUpdatesWhenOldMaxIsOverwritten() {
      // Fill the buffer to its capacity of 4
      ringBuffer.insertNewValue(100f) // Index 0 (Oldest)
      ringBuffer.insertNewValue(50f)  // Index 1
      ringBuffer.insertNewValue(75f)  // Index 2
      ringBuffer.insertNewValue(30f)  // Index 3

      assertEquals(100f, ringBuffer.maxValue, 0.0f)

      // The next insert triggers an overwrite because count == usedCapacity (4)
      // 10.0f will overwrite 100f at physical index 0.
      // The new max within the active window should now drop to 75f.
      ringBuffer.insertNewValue(10f)

      assertEquals(75f, ringBuffer.maxValue, 0.0f)
   }

   @Test
   fun testContinuousOverwritesMaintainMax() {
      // Push a massive peak value early on
      ringBuffer.insertNewValue(500f) // Will be overwritten 1st
      ringBuffer.insertNewValue(20f)  // Will be overwritten 2nd
      ringBuffer.insertNewValue(10f)  // Will be overwritten 3rd
      ringBuffer.insertNewValue(5f)   // Will be overwritten 4th

      // Now entirely stream new values that shift the window completely past the 500f peak
      ringBuffer.insertNewValue(50f)  // Overwrites 500f -> Window max becomes 50f
      assertEquals(50f, ringBuffer.maxValue, 0.0f)

      ringBuffer.insertNewValue(60f)  // Overwrites 20f  -> Window max becomes 60f
      assertEquals(60f, ringBuffer.maxValue, 0.0f)

      ringBuffer.insertNewValue(55f)  // Overwrites 10f  -> Window max remains 60f
      assertEquals(60f, ringBuffer.maxValue, 0.0f)
   }

   @Test
   fun testChangeBufferSizeRecalculatesMaxCorrectly() {
      // Setup an initial stream in a capacity-4 buffer
      ringBuffer.insertNewValue(10f)
      ringBuffer.insertNewValue(80f) // This is our current max
      ringBuffer.insertNewValue(30f)
      ringBuffer.insertNewValue(40f)

      assertEquals(80f, ringBuffer.maxValue, 0.0f)

      // Downsize the buffer capacity to 2.
      // It must preserve only the 2 most recent elements: [30f, 40f]
      // The 80f element is discarded, so the new max must drop to 40f.
      ringBuffer.changeBufferSize(3)
      ringBuffer.insertNewValue(90f)

      assertEquals(3, ringBuffer.count)
      assertEquals(90f, ringBuffer.maxValue, 0.0f)
      assertEquals(30f, ringBuffer[0], 0.0f)
      assertEquals(40f, ringBuffer[1], 0.0f)
      assertEquals(90f, ringBuffer[2], 0.0f)
   }

   @Test
   fun testClearResetsAllStateIncludingMax() {
      ringBuffer.insertNewValue(150f)
      ringBuffer.insertNewValue(20f)

      ringBuffer.clear()

      assertEquals(0, ringBuffer.count)
      assertEquals(0f, ringBuffer.maxValue, 0.0f)
   }
}