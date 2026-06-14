package android.learn.telemetryapp.engine

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object GeneratorModule {
   @Provides
   fun getCpuGenerator(): CpuGenerator {
      return CpuGenerator
   }

   @Provides
   fun getMemoryGenerator(): MemoryGenerator {
      return MemoryGenerator
   }

   @Provides
   fun getNetworkGenerator(): NetworkGenerator {
      return NetworkGenerator
   }

}