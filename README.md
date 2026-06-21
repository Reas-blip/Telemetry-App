## 🏗️ Deep Tech Architecture & Optimizations

This dashboard is engineered from the ground up to handle high-frequency telemetry streams (e.g., 100Hz+) without dropping frames, triggering garbage collection stutters, or blocking the main thread.

### 1. Hardware-Accelerated Native Rendering
* **SurfaceView Integration:** Bypassed the standard Jetpack Compose `Canvas` for high-frequency updates. Implemented a custom `TelemetrySurfaceView` wrapped in an `AndroidView` to leverage a dedicated drawing thread and `PixelFormat.TRANSPARENT` hardware acceleration.
* **Gesture-Driven Interactivity:** Mapped custom pointer inputs directly to the SurfaceView's local coordinate space, allowing users to drag and scrub through real-time telemetry points natively.

### 2. Zero-Allocation State Management (Ring Buffers)
* **Primitive-Backed Data Structures:** Engineered a custom `RingBuffer` using contiguous `FloatArray` and `LongArray` blocks. This strictly prevents object allocation per frame, entirely eliminating GC (Garbage Collection) pauses during active data streaming.
* **Amortized Max-Tracking:** Integrated a `MonotonicMaxQueue` alongside the circular buffer to calculate the rolling maximum scale in amortized $O(1)$ time, avoiding expensive array traversals on every frame tick.

### 3. Lock-Free Concurrency & Emulation
* **Atomic State Engines:** Utilized Kotlin's `ExperimentalAtomicApi` (`AtomicArray`, `AtomicLong`) within the `MetricsEngine` to ensure lock-free concurrency. Telemetry producers write data asynchronously without blocking the UI's read cycles.
* **Deterministic Simulation:** Built a comprehensive telemetry generator engine that mimics real-world edge cases: CPU jitter, memory GC drop events, and exponential network burst decay.

### 4. Recomposition Scoping & Glassmorphism
* **Targeted Recomposition:** Utilized `rememberUpdatedState` and strictly scoped `LaunchedEffect` animations (e.g., the `RotatingExpandIcon`) to ensure that toggling configurations never triggers a recomposition of the underlying charting surface.
* **Render-Safe Blur:** Integrated Chris Banes' `Haze` library to create a dynamic liquid-glass UI layer, applying heavy blur radii and micro-texture noise without impacting the draw loop of the underlying real-time graph.


https://github.com/user-attachments/assets/86f66a79-efe9-4aac-8cd7-1e0e5bb7977d

