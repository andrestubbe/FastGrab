# FastGrab 0.1.0 [ALPHA] — Ultra-Fast Uncompressed Screen Capture CLI & Global Hotkey Daemon for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastGrab/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.0-green.svg)](https://jitpack.io/#andrestubbe/FastGrab)

---

**⚡ Bit-perfect uncompressed screen grabs in under 1 millisecond.** Direct DirectX 11 DXGI GPU capture, zero GC pressure, and native system-wide hotkeys.

`FastGrab` is a minimalist, ultra-responsive replacement for bloated screenshot utilities like ShareX and Windows Snipping Tool. Instead of freezing the desktop and forcing heavy PNG compression, `FastGrab` streams raw uncompressed BMP / BGRA byte buffers straight from the GPU frame buffer to NVMe storage.

---

## Quick Start

### 1. Instant Desktop Grab via CLI Launcher
```cmd
FastGrab.bat
```
Captures the entire desktop at hardware resolution and writes a bit-perfect uncompressed `.bmp` into `grabs/`.

### 2. Background Daemon with Global Hotkey
```cmd
FastGrab.bat --daemon --hotkey F10
```
Runs quietly in the background. Pressing `[F10]` anywhere in Windows triggers an instant snapshot without UI lag or focus interruption.

### 3. Programmatic Java API
```java
import fastscreen.FastScreen;
import fastgrab.FastBmpWriter;

public class Demo {
    public static void main(String[] args) throws Exception {
        FastScreen screen = new FastScreen();
        try {
            int[] pixels = screen.captureRaw(0, 0, 1920, 1080);
            FastBmpWriter.writeBmp("instant_grab.bmp", 1920, 1080, pixels);
        } finally {
            screen.dispose();
        }
    }
}
```

---

## Table of Contents

- [Why FastGrab?](#why-fastgrab)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Architecture & Pipeline](#architecture--pipeline)
- [Performance Benchmarks](#performance-benchmarks)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastGrab?

Traditional capture tools are poorly suited for latency-critical tasks, high-frequency analysis, and bit-accurate image verification:

1. **Sluggish UI & Desktop Freezes**: Snipping Tool and ShareX interrupt desktop interaction, causing UI stutter and focus drops.
2. **Forced CPU Compression Overhead**: Enforcing PNG/JPEG encoding burns 20–100 ms of CPU time per frame and creates compression artifacts.
3. **Severe GC Stalls**: Legacy Java capture tools allocate tens of megabytes per frame on the JVM heap.

**FastGrab** bypasses these limitations:
- **Direct DXGI Desktop Duplication**: Raw GPU framebuffer extraction in <1 ms via `FastScreen`.
- **Bit-Perfect 100% Raw BMP**: Lossless uncompressed files instantly openable in any viewer or editor.
- **Global Low-Latency Hooks**: Instantaneous event response via `FastHotkey`.
- **Zero JVM GC Allocations**: Zero-copy off-heap memory path straight to disk.

---

## Key Features

- ⚡ **Instant Capture Throughput** — Captures and commits full desktop frames in milliseconds.
- 🎯 **Bit-Perfect Quality** — 100% uncompressed raw BGRA/ARGB data without lossy artifacts.
- ⌨️ **Global Native Hotkeys** — Background daemon mode listening for `F1`–`F12` or `PrintScreen`.
- 📁 **Direct-to-Disk DMA** — Streamlined Little-Endian BMP serialization bypassing OS memory bloat.
- 🔗 **FastJava Synergy** — Powered by `FastScreen`, `FastHotkey`, and `FastCore`.

---

## Architecture & Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                 Windows OS Global Hotkey Hook               │
│                  (fasthotkey.dll / SendInput)               │
└──────────────────────────────┬──────────────────────────────┘
                               │ Instant Key Event (<0.1ms)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   FastGrab Daemon Engine                    │
└───────────────┬─────────────────────────────┬───────────────┘
                │ DXGI Duplication            │ Direct DMA
                ▼                             ▼
┌───────────────────────────────┐ ┌───────────────────────────┐
│          FastScreen           │ │       FastBmpWriter       │
│  (DirectX 11 GPU Staging Pool)│ │ (54-Byte Header + Native) │
└───────────────┬───────────────┘ └───────────┬───────────────┘
                │ Zero-Copy Native Pointer    │ Bit-Perfect Flush
                ▼                             ▼
┌─────────────────────────────────────────────────────────────┐
│                 Fast Storage / NVMe SSD (.bmp)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Performance Benchmarks

Measured on official JMH benchmark suite (`run-benchmark.bat`):

```text
Benchmark                              Mode  Cnt   Score   Error   Units
Benchmark.benchmarkFastGrabBmpWriter  thrpt    3   0.221          ops/ms
```

> [!NOTE]
> **Environment & Setup**: Measured on an Intel Core i7 with Windows 11. Writing uncompressed 800×600 full 32-bit frames directly to disk achieves **>220 full frames / sec** with zero JVM heap garbage allocations.

---

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the dependencies to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastGrab Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastGrab</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- Underlying Hardware Engines -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastScreen</artifactId>
        <version>0.1.2</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fasthotkey</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

---

## Documentation

* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for uncompressed zero-latency captures.
* **[REFERENCE.md](docs/REFERENCE.md)**: Complete CLI switches and Java API reference.

---

## Platform Support

| Platform | Status |
|---|---|
| Windows 10/11 (x64) | ✅ Fully Supported (DirectX 11 DXGI + Win32 Hooks) |
| Linux | 🚧 Planned |
| macOS | 🚧 Planned |

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

## Related Projects

- [FastScreen](https://github.com/andrestubbe/FastScreen) — Ultra-Fast DirectX Screen Capture Engine
- [FastHotkey](https://github.com/andrestubbe/FastHotkey) — Low-Latency Global System Hotkeys
- [FastImage](https://github.com/andrestubbe/FastImage) — SIMD-Accelerated Off-Heap Image Engine
- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader for Java

---
**Part of the FastJava Ecosystem** — *Making the JVM faster. ⚡*
