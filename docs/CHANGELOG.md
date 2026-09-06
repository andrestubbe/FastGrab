# Changelog 📝

All notable changes to **FastScreenCapture** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.1] - 2026-09-06

- **Native Mouse Cursor Capture (`--cursor`, `--mouse`, `-c`)**:
  - Direct Win32 hardware cursor extraction via `GetCursorInfo` + `GetIconInfo` + `DrawIconEx`.
  - Precise hotspot alignment (arrow tip, crosshair center, text I-beam) and real-time 32-bit alpha blending into uncompressed BMP screenshots and 60 FPS video recordings.
  - Zero performance overhead when disabled; sub-0.01 ms blending time when active.
- **Official 60 FPS Demonstration Video**:
  - Embedded YouTube demonstration (`https://youtu.be/CBbNffXXvVc`) showcasing lossless 60 FPS direct-pipe streaming and sub-millisecond DXGI frame acquisition.
  - Linked interactive banner and video documentation in `README.md`.
- **Packaging & Dependency Fixes**:
  - Explicitly declared foundation dependencies (`FastSIMD`, `FastMemory`, `FastPointer`) in Maven and Gradle documentation.

## [0.1.0] - 2026-09-06

### Added
- **Initial Release of FastScreenCapture**: Ultra-fast uncompressed screen capture CLI and global hotkey daemon for Java.
- **Lossless 60 FPS Video Recording via FFmpeg Pipe**:
  - Direct zero-copy memory stream from DXGI Desktop Duplication directly into an FFmpeg process stdin.
  - Zero disk I/O lag: eliminates generating gigabytes of intermediate files.
  - Generates web- and Windows Photos-compatible MP4 files with `-movflags +faststart`.
  - Toggle video recording on/off anytime with global hotkey `[F9]`.
- **Acoustic Status Feedback**:
  - Crisp high pitch (1200 Hz) on recording start.
  - Low pitch (450 Hz) on recording stop and file finalization.
- **Bit-Perfect Uncompressed BMP Engine (`FastBmpWriter`)**:
  - Direct sequential write of 54-byte BMP header + 32-bit BGRA pixel streams.
  - Top-down DIB layout (negative height in `BITMAPINFOHEADER`) eliminating buffer flipping and vertical inversion passes.
  - Zero JVM heap allocations via NIO direct channel writes.
- **FastScreen DXGI Direct Desktop Integration**:
  - Sub-millisecond desktop frame grab via DirectX 11 Desktop Duplication API.
  - Off-heap zero-copy pixel buffer streaming straight from native memory to disk.
- **Global Hotkey Daemon (`--daemon`, `-d`)**:
  - Background system-wide hotkey listener using `FastHotkey` (Win32 low-level hooks).
  - Customizable hotkey trigger (`--hotkey <KEY>`, default `F10` for screenshot, `F9` for video toggle).
  - Automatic timestamped output (`grabs/video_*.mp4`, `grabs/grab_*.bmp`).
- **CLI & Burst Capture**:
  - Continuous video streaming via `--record <seconds>` and `--fps <fps>`.
  - Region selection via `--rect x,y,w,h`.
  - Custom output file targets via `--out <path>`.
  - Multi-frame burst capture (`--burst <count>`).
- **Ready-to-Use Scripts & Benchmarks**:
  - `FastScreenCapture.bat` launch script with cached classpath and automatic UTF-8 (`chcp 65001`) console encoding.
  - Standardized JMH microbenchmark suite (`run-benchmark.bat`).
