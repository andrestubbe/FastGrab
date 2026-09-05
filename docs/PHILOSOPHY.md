# The Philosophy of FastGrab 💡

> [!IMPORTANT]
> **"Bit-perfekt. Unkomprimiert. Keine Latenz. Zero GC."**

FastGrab was born out of frustration with existing screenshot utilities (ShareX, Windows Snipping Tool). Modern screen grabbers stall the desktop, trigger CPU spikes by enforcing slow PNG compression, and degrade subtle color gradations.

FastGrab takes a fundamentally different engineering route: **Direct-to-Disk DMA with zero compression latency.**

---

## Core Tenets

### 1. Zero-Compression Instant Snapshots
Compression algorithms (DEFLATE, PNG, JPEG) add 20–100ms of CPU latency per capture and introduce compression artifacts or frame drops. FastGrab stores bit-perfect raw uncompressed BMP / BGRA byte streams directly, completing the entire capture and disk write cycle in milliseconds.

### 2. DXGI Desktop Duplication Pipeline
FastGrab taps directly into the Windows Desktop Window Manager (DWM) compositor via `FastScreen`. Frames are extracted straight from the GPU frame buffer without legacy GDI locks or AWT Event Queue hops.

### 3. Global Low-Latency Hotkeys
Via `FastHotkey`, FastGrab hooks into native OS input messages. Pressing your trigger key executes an immediate capture callback without context switching delays or foreground focus requirements.

### 4. Zero JVM Garbage
By combining direct native memory buffers with single-pass Little-Endian file writing, FastGrab produces 0 bytes of temporary garbage on the JVM heap.

---

**⚡ FastGrab — Instant uncompressed capture for the FastJava ecosystem.**
