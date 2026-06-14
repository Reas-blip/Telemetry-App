package android.learn.telemetryapp.engine

import kotlin.random.Random

interface MetricGenerator {
   fun next(): Float
}

/**
 * BEHAVIOR: Highly erratic, rapid fluctuations (Jittery).
 * BOUNDS: Strictly bounded between 0.0% and 100.0%.
 */
object CpuGenerator : MetricGenerator {
   private var current = Random.nextDouble(20.0, 60.0).toFloat()

   override fun next(): Float {
      // High frequency random walk: jump up or down by up to 8%
      val jitter = (Random.nextFloat() * 16f) - 8f
      current += jitter

      // Occasional sudden jump or drop
      if (RandomSpike.shouldTrigger(chance = 0.05f)) {
         val macroShift = (Random.nextFloat() * 30f) - 15f
         current += macroShift
      }

      // Iron-clad ceiling and floor enforcement
      current = current.coerceIn(0f, 100f)
      return current
   }
}

/**
 * BEHAVIOR: Smooth, slow climbing trend punctuated by a sharp drop (GC events).
 * BOUNDS: 10.0% to 95.0%.
 */
object MemoryGenerator : MetricGenerator {
   private var current = Random.nextDouble(30.0, 40.0).toFloat()

   override fun next(): Float {
      // Steady background leakage accumulation
      val accumulation = Random.nextFloat() * 0.4f
      current += accumulation

      // Simulate a Garbage Collection flush event (sharp drop)
      if (RandomSpike.shouldTrigger(chance = 0.02f)) {
         val dropPercentage = Random.nextFloat() * 0.3f // Drops 0% to 30% of current usage
         current -= (current * dropPercentage)
      }

      current = current.coerceIn(10f, 95f)
      return current
   }
}

/**
 * BEHAVIOR: Flatline baseline with massive, decaying high-volume bursts.
 * BOUNDS: 0.0 to 7500.0 Requests/sec.
 */
object NetworkGenerator : MetricGenerator {
   private var burstTicksRemaining = 0
   private var currentBurstIntensity = 0f

   override fun next(): Float {
      // Check if we should trigger a new episodic transmission burst
      if (burstTicksRemaining <= 0 && RandomSpike.shouldTrigger(chance = 0.03f)) {
         burstTicksRemaining = Random.nextInt(10, 35) // Burst lasts 10 to 35 frames
         currentBurstIntensity = Random.nextInt(3500, 7500).toFloat()
      }

      val output = if (burstTicksRemaining > 0) {
         // Inside an active burst frame window
         burstTicksRemaining--

         // Apply exponential decay over the lifetime of the burst so it tapers off
         currentBurstIntensity *= 0.94f

         // Add high-frequency noise so the burst line looks realistic, not perfectly smooth
         val noise = Random.nextInt(-300, 300)
         (currentBurstIntensity + noise).coerceIn(0f, 7500f)
      } else {
         // Standard idle environment noise baseline
         Random.nextFloat() * 15f
      }

      return output
   }
}

/**
 * Pure mathematical utility helper for clean rate probabilities.
 */
private object RandomSpike {
   /**
    * @param chance A value between 0.0f (0%) and 1.0f (100%)
    */
   fun shouldTrigger(chance: Float): Boolean {
      return Random.nextFloat() < chance
   }
}
