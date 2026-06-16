package android.learn.telemetryapp.datastructures

class MonotonicMaxQueue {

   private data class Candidate(
      val sequenceId: Long,
      val value: Float
   )

   private val deque = ArrayDeque<Candidate>()

   fun clear() {
      deque.clear()
   }

   fun insert(
      sequenceId: Long,
      value: Float,
      currentBufferSize: Int
   ) {
      removeExpiredCandidates(
         currentSequenceId = sequenceId,
         currentBufferSize = currentBufferSize
      )

      while (
         deque.isNotEmpty() &&
         deque.last().value <= value
      ) {
         deque.removeLast()
      }

      deque.addLast(
         Candidate(
            sequenceId = sequenceId,
            value = value
         )
      )
   }

   fun max(
      currentSequenceId: Long,
      currentBufferSize: Int
   ): Float {
      removeExpiredCandidates(
         currentSequenceId = currentSequenceId,
         currentBufferSize = currentBufferSize
      )

      return deque.firstOrNull()?.value ?: 0f
   }

   private fun removeExpiredCandidates(
      currentSequenceId: Long,
      currentBufferSize: Int
   ) {
      val oldestValidSequence =
         maxOf(
            0L,
            currentSequenceId - currentBufferSize + 1
         )

      while (
         deque.isNotEmpty() &&
         deque.first().sequenceId < oldestValidSequence
      ) {
         deque.removeFirst()
      }
   }
}