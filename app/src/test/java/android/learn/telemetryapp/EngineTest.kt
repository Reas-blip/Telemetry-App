package android.learn.telemetryapp

import android.learn.telemetryapp.engine.CpuGenerator
import android.learn.telemetryapp.engine.MemoryGenerator
import android.learn.telemetryapp.engine.MetricGenerator
import android.learn.telemetryapp.engine.MetricsEngine
import android.learn.telemetryapp.engine.NetworkGenerator
import io.ktor.utils.io.core.Memory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

   @Test
   fun engine_generates_values() = runTest {
      val cpuGenerator = mockk<CpuGenerator>()
      val memoryGenerator = mockk<MemoryGenerator>()
      val networkGenerator = mockk<NetworkGenerator>()

      every { cpuGenerator.next() } returns 10f
      every { memoryGenerator.next() } returns 20f
      every { networkGenerator.next() } returns 30f

      val engine = MetricsEngine(
         cpuGenerator,
         memoryGenerator,
         networkGenerator
      )

      engine.startEngine(this)

      advanceTimeBy(20000)

      engine.stopEngine()

      assertTrue(engine.valueIndex > 0)
   }
}