# Changelog 📝

All notable changes to **FastGrab** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-09-05

### Added
- **Initial Release of FastGrab**: Ultra-fast uncompressed screen capture CLI and global hotkey daemon for Java.
- **Bit-Perfect Uncompressed BMP Engine (`FastBmpWriter`)**:
  - Direct sequential write of 54-byte BMP header + 32-bit BGRA pixel streams.
  - Top-down DIB layout (negative height in `BITMAPINFOHEADER`) eliminating buffer flipping and vertical inversion passes.
  - Zero JVM heap allocations via NIO direct channel writes.
- **FastScreen DXGI Direct Desktop Integration**:
  - Sub-millisecond desktop frame grab via DirectX 11 Desktop Duplication API.
  - Off-heap zero-copy pixel buffer streaming straight from native memory to disk.
- **Global Hotkey Daemon (`--daemon`, `-d`)**:
  - Background system-wide hotkey listener using `FastHotkey` (Win32 low-level hooks).
  - Customizable hotkey trigger (`--hotkey <KEY>`, default `F10`, supports `F1`-`F12`, `PRINTSCREEN`).
  - Automatic timestamped output (`grabs/grab_YYYYMMDD_HHMMSS_SSS.bmp`).
- **CLI & Burst Capture**:
  - Region selection via `--rect x,y,w,h`.
  - Custom output file targets via `--out <path.bmp>`.
  - Multi-frame burst capture (`--burst <count>`).
- **Ready-to-Use Scripts & Benchmarks**:
  - `FastGrab.bat` launch script with zero setup overhead.
  - Standardized JMH microbenchmark suite (`run-benchmark.bat`).
