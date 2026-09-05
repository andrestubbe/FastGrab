# FastGrab Roadmap 🗺️

**Vision:** The highest-throughput, lowest-latency screen recorder and uncompressed frame capture CLI daemon for Java and Windows.

---

## 🟢 v0.1.0: Core Capture & Daemon Release (Completed)
- [x] **DXGI Direct Capture**: Sub-millisecond desktop ingestion via `FastScreen`.
- [x] **Zero-Copy Uncompressed BMP Engine**: `FastBmpWriter` top-down DIB direct-to-disk streaming.
- [x] **Global Hotkey Daemon**: System-wide low-latency hotkey trigger (`FastHotkey`).
- [x] **Burst Capture**: Consecutive multi-frame burst capture with sub-3ms latency per frame.
- [x] **CLI Options**: Full command-line interface with `--rect`, `--burst`, `--hotkey`, `--out`, `--daemon`.
- [x] **Standardized Microbenchmarks**: JMH benchmark suite measuring disk throughput (>220 FPS).

---

## 🟡 v0.2.0: Continuous 60+ FPS Video Streaming & Raw Stream Container
- [ ] **Continuous 60 FPS Recorder Mode**: `--record <seconds>` and `--fps <60>` CLI capture loop.
- [ ] **Sequential Frame Ring Buffer**: Asynchronous multi-threaded disk writer queue to eliminate write stalls.
- [ ] **FastRaw / `.fseq` Uncompressed Stream Container**: Single-file uncompressed raw video container for lossless playback.
- [ ] **Region Drag Selector**: Interactive lightweight desktop overlay for region coordinates.

---

## 🟠 v0.3.0: Audio Synchronization & Lossless Compression
- [ ] **WASAPI Audio Loopback**: Synchronized desktop audio capture via native WASAPI loopback buffer.
- [ ] **FastLZ4 / FastZstd Lossless Compression**: Optional real-time zero-copy compression pipeline for 3–5× disk space savings at >120 FPS.
- [ ] **FastFileFormat Video Converter**: Export `.raw`/`.fseq` streams into MP4 (H.264 / HEVC / AV1) with zero quality loss.

---

## 🔴 v1.0.0: Enterprise Production Hardening
- [ ] **Multi-Monitor Simultaneous Capture**: Discrete capture threads per physical display output.
- [ ] **Tray Icon & Notification Toast**: Windows system tray daemon integration with status controls.
- [ ] **Long-Run Soak & Stress Tests**: Continuous 24h streaming test suite with zero dropped frames or memory leaks.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. ⚡*
