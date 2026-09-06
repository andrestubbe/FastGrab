# FastScreenCapture 0.1.0 [ALPHA] — Ultra-Fast Uncompressed Screen Capture CLI & Global Hotkey Daemon for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastScreenCapture/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.0-green.svg)](https://jitpack.io/#andrestubbe/FastScreenCapture)

---

**⚡ Bit-perfect uncompressed screen grabs and direct 60 FPS video recording.** Direct DirectX 11 DXGI GPU capture, zero GC pressure, direct RAM pipe to FFmpeg, and native system-wide hotkeys.

`FastScreenCapture` was born out of frustration while recording high-frequency 3D particle benchmarks for **FastAnimation**: standard tools like **ShareX** and the native **Windows 11 Snipping Tool** introduced heavy compression blur, frame drops, and severe moiré interference on fine particle lines.

Instead of burning CPU on heavy compression or writing 30 GB of uncompressed bitmaps to the SSD, `FastScreenCapture` extracts pristine frames straight from DXGI Desktop Duplication and streams raw BGRA buffers directly into an optimized FFmpeg pipe — keeping CPU usage near 0% with zero dropped frames.

---

## Quick Start

### 1. Background Daemon (Recommended)
```cmd
FastScreenCapture.bat --daemon
```
Runs quietly in the background without UI lag or focus interruption:
- **`[F9]`**: **Toggle 60 FPS Video Recording** (starts/stops lossless MP4 stream via FFmpeg pipe with instant `+faststart` playback).
- **`[F10]`**: **Instant Bit-Perfect Screenshot** (uncompressed 32-bit BMP straight to `grabs/`).
- **Acoustic feedback**: High tone (1200 Hz) confirms recording start; low tone (450 Hz) confirms recording stop.

### 2. Instant Desktop Grab via CLI Launcher
```cmd
FastScreenCapture.bat
```
Captures the entire desktop at hardware resolution and writes a bit-perfect uncompressed `.bmp` into `grabs/`.

### 3. Programmatic Video & Screenshot API
```cmd
FastScreenCapture.bat --record 60 --fps 60 --out fastanimation_demo.mp4
```

---

## Table of Contents

- [Why FastScreenCapture?](#why-FastScreenCapture)
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

## Why FastScreenCapture?

Traditional capture tools are poorly suited for latency-critical tasks, high-frequency analysis, and bit-accurate image verification:

1. **Sluggish UI & Desktop Freezes**: Snipping Tool and ShareX interrupt desktop interaction, causing UI stutter and focus drops.
2. **Forced CPU Compression Overhead**: Enforcing PNG/JPEG encoding burns 20–100 ms of CPU time per frame and creates compression artifacts.
3. **Severe GC Stalls**: Legacy Java capture tools allocate tens of megabytes per frame on the JVM heap.

**FastScreenCapture** bypasses these limitations:
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

## Real-World Use Cases

- ⚡ **Zero-Lag Screenshot Hotkey Daemon**: Replace bloated screenshot utilities (ShareX, Windows Snipping Tool) with a background process that commits full-resolution desktop captures in <6 ms without UI pauses or window flashes.
- 🎯 **Bit-Exact Visual Regression & UI Testing**: Capture pristine, 100% uncompressed frames during automated UI / rendering test suites (e.g., Selenium, JavaFX, OpenGL, Vulkan) where PNG compression artifacts or color subsampling would cause false positive diffs.
- 🏎️ **High-Speed Gameplay & Event Bursting**: Burst-capture 5–60 consecutive uncompressed frames (`--burst 10`) at full monitor refresh rates during rapid in-game action, physics anomalies, or micro-stutters.
- 🤖 **Dataset Generation for Vision & OCR Pipelines**: Rapidly collect hundreds of raw screen patches per second without GPU/CPU encoding bottlenecks for training OCR, layout parsing, or object detection models.
- 📊 **Financial & High-Frequency Trading Telemetry**: Archive pixel-accurate order book states, charts, and millisecond ticker snapshots without introducing thread latency to execution algorithms.

---

## Architecture & Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                 Windows OS Global Hotkey                    │
│                        (FastHotkey)                         │
└──────────────────────────────┬──────────────────────────────┘
                               │ Instant Key Event (<0.1ms)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   FastScreenCapture Daemon Engine                    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Request Frame (<1ms)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         FastScreen                          │
│          (DirectX 11 DXGI GPU Desktop Duplication)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ Zero-Copy Off-Heap BGRA Buffer
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       FastBmpWriter                         │
│               (54-Byte Top-Down DIB Header)                 │
└──────────────────────────────┬──────────────────────────────┘
                               │ Direct NIO Channel Write
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Fast Storage / NVMe SSD (.bmp)              │
└─────────────────────────────────────────────────────────────┘
```

---

## Performance Benchmarks

Measured on official JMH benchmark suite (`run-benchmark.bat`):

```text
Benchmark                              Mode  Cnt   Score   Error   Units
Benchmark.benchmarkFastScreenCaptureBmpWriter  thrpt    3   0.221          ops/ms
```

> [!NOTE]
> **Environment & Setup**: Measured on an 11th Gen Intel(R) Core(TM) i5-1135G7 @ 2.40GHz (4C/8T), Windows 11 Home, OpenJDK 21 LTS. Writing uncompressed 800×600 full 32-bit frames directly to disk achieves **>220 full frames / sec** with zero JVM heap garbage allocations.

---

## API Quick Reference

FastScreenCapture provides both high-speed Java methods and a standalone CLI tool:

### Java API (`FastBmpWriter`)

| Class | Method | Return Type | Description |
|:---|:---|:---:|:---|
| `FastBmpWriter` | `writeBmp(String path, int w, int h, int[] pixels)` | `void` | Writes 32-bit ARGB/BGRA pixel array directly to uncompressed Windows BMP file (top-down DIB). |
| `FastBmpWriter` | `writeDirectBgra(String path, int w, int h, ByteBuffer buffer)` | `void` | **Zero-Copy**: Streams direct off-heap native buffer straight to disk via NIO FileChannel. |

### CLI Options Reference

| Switch | Long Option | Argument | Description | Default |
|:---|:---|:---:|:---|:---:|
| - | `--record` | `[seconds]` | Direct lossless 60 FPS video recording via FFmpeg pipe | `60` |
| - | `--fps` | `<fps>` | Target recording frame rate | `60` |
| `-d` | `--daemon` | - | Runs continuously in background (hotkeys: `F9` Video, `F10` Screenshot) | `false` |
| `-k` | `--hotkey` | `<KEY>` | Screenshot hotkey in daemon mode (`F1`–`F12`, `PRINTSCREEN`) | `F10` |
| `-o` | `--out` | `<path>` | Custom output file path (`.bmp` or `.mp4`) | `grabs/grab_...` |
| `-r` | `--rect` | `<x,y,w,h>` | Explicit capture region coordinates | Full screen |
| `-b` | `--burst` | `<count>` | Consecutive frame burst count | `1` |
| `-h` | `--help` | - | Prints usage instructions and switches | - |

---

## FFmpeg Requirement & Quick Setup

To enable high-speed direct-pipe video recording without disk I/O bottlenecks, `FastScreenCapture` requires FFmpeg. FastScreenCapture will automatically detect FFmpeg in your system `PATH` or in standard WinGet directories.

Install FFmpeg in one command via Windows Package Manager:

```cmd
winget install Gyan.FFmpeg
```

Alternatively, download the official Windows build directly from [gyan.dev/ffmpeg/builds](https://www.gyan.dev/ffmpeg/builds/) (e.g. `ffmpeg-release-essentials.zip`) and add its `bin` folder to your system `PATH`.

---

## Installation

### Option 1: Maven (`pom.xml`)

Add the JitPack repository and the dependencies to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastScreenCapture Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastScreenCapture</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- Underlying Hardware Engines -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastScreen</artifactId>
        <version>0.1.4</version>
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

### Option 2: Gradle (`build.gradle`)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastScreenCapture:0.1.0'
    implementation 'com.github.andrestubbe:FastScreen:0.1.4'
    implementation 'com.github.andrestubbe:fasthotkey:0.1.0'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest pre-compiled JARs directly to add them to your project's classpath or use the CLI directly:

1. 📸 [**FastScreenCapture-0.1.0.jar**](https://github.com/andrestubbe/FastScreenCapture/releases/tag/0.1.0) (The CLI & Capture Engine)
2. 🖥️ [**FastScreen-0.1.4.jar**](https://github.com/andrestubbe/FastScreen/releases/tag/0.1.4) (DirectX 11 DXGI Desktop Capture)
3. ⌨️ [**FastHotkey-0.1.0.jar**](https://github.com/andrestubbe/FastHotkey/releases/tag/0.1.0) (Low-Latency Win32 Hotkeys)
4. ⚙️ [**FastCore-0.1.0.jar**](https://github.com/andrestubbe/FastCore/releases/tag/0.1.0) (Mandatory Native Library Loader)

> [!IMPORTANT]
> FastScreenCapture requires `FastScreen` (and its native DLL), `FastHotkey`, and `FastCore` on the classpath to run.

---

## Documentation

* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for uncompressed zero-latency captures.
* **[REFERENCE.md](docs/REFERENCE.md)**: Complete CLI switches and Java API reference.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Full release history and version notes.
* **[ROADMAP.md](docs/ROADMAP.md)**: Upcoming features, 60+ FPS video streaming, and ecosystem milestones.

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
- [FastCamera](https://github.com/andrestubbe/FastCamera) — Hardware-Accelerated Native Camera Capture & Streaming
- [FastHotkey](https://github.com/andrestubbe/FastHotkey) — Low-Latency Global System Hotkeys
- [FastImage](https://github.com/andrestubbe/FastImage) — SIMD-Accelerated Off-Heap Image Engine
- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader for Java

---
**Part of the FastJava Ecosystem** — *Making the JVM faster. ⚡*
