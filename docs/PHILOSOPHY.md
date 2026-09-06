# The Philosophy of FastScreenCapture 💡

> [!IMPORTANT]
> **"Bit-perfekt. Unkomprimiert. Keine Latenz. Zero GC."**

FastScreenCapture was born out of frustration with existing capture utilities (ShareX, Windows Snipping Tool) when recording delicate high-frequency benchmarks like FastAnimation. Modern screen grabbers stall the desktop, trigger CPU spikes by enforcing slow compression passes, and degrade subtle color gradations and fine lines with blur and moiré.

FastScreenCapture takes a fundamentally different engineering route: **Direct DXGI GPU extraction with zero-copy RAM streaming.**

---

## Core Tenets

### 1. Zero-Compression Instant Snapshots & Streaming
Compression algorithms (DEFLATE, PNG, heavy CPU x264 presets) add massive latency per frame and introduce compression artifacts or frame drops. FastScreenCapture either stores bit-perfect raw uncompressed BMP byte streams directly or pipes native frames straight to FFmpeg via direct memory pipes without intermediate disk churn.

### 2. DXGI Desktop Duplication Pipeline
FastScreenCapture taps directly into the Windows Desktop Window Manager (DWM) compositor via `FastScreen`. Frames are extracted straight from the GPU frame buffer without legacy GDI locks or AWT Event Queue hops.

### 3. Global Low-Latency Hotkeys
Via `FastHotkey`, FastScreenCapture hooks into native OS input messages. Pressing your trigger key executes an immediate capture callback without context switching delays or foreground focus requirements.

### 4. Zero JVM Garbage
By combining direct native memory buffers with single-pass Little-Endian byte streaming, FastScreenCapture produces 0 bytes of temporary garbage on the JVM heap during hot recording loops.

---

**⚡ FastScreenCapture — Instant uncompressed capture for the FastJava ecosystem.**
