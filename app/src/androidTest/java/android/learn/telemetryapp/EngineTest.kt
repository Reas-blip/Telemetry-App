package android.learn.telemetryapp

import android.learn.telemetryapp.engine.CpuGenerator
import android.learn.telemetryapp.engine.MemoryGenerator
import android.learn.telemetryapp.engine.MetricGenerator
import android.learn.telemetryapp.engine.MetricsEngine
import android.learn.telemetryapp.engine.NetworkGenerator
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EngineMemoryTest {

   @Test
   fun profileEngine() = runTest {
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

      kotlinx.coroutines.delay(60_000)

      engine.stopEngine()
   }
}