# FastScreenCapture API & CLI Reference 📖

Complete guide for CLI switches, hotkey configuration, and Java API usage.

---

## Command Line Interface (CLI)

```bash
FastScreenCapture.bat [options]
```

### Options

| Switch | Long Option | Argument | Description | Default |
|:---|:---|:---:|:---|:---:|
| - | `--record` | `[seconds]` | Lossless 60 FPS video recording streamed into FFmpeg pipe | `60` |
| - | `--fps` | `<fps>` | Video recording framerate target | `60` |
| `-d` | `--daemon` | - | Runs in background (`[F9]` Toggle Video, `[F10]` Screenshot) | `false` |
| `-k` | `--hotkey` | `<KEY>` | Screenshot hotkey (`F1`-`F12`, `PRINTSCREEN`) | `F10` |
| `-o` | `--out` | `<path>` | Output target (`.bmp` or `.mp4`) | Auto timestamp |
| `-r` | `--rect` | `<x,y,w,h>` | Explicit capture region coordinates | Full screen |
| `-b` | `--burst` | `<count>` | Consecutive frame burst count | `1` |
| `-h` | `--help` | - | Displays usage instructions | - |

---

## Hotkeys & Audio Tone Cues (Daemon Mode)

* **`[F9]`**: Start/Stop Video Recording (`grabs/video_*.mp4`)
  * 🔔 **High Tone (1200 Hz)**: Confirms recording start
  * 🔔 **Low Tone (450 Hz)**: Confirms recording stop & finalization
* **`[F10]`**: Single uncompressed bit-perfect snapshot (`grabs/grab_*.bmp`)

---

## Java API Reference

### `FastBmpWriter`
```java
// Save raw ARGB pixel array as bit-perfect uncompressed BMP
FastBmpWriter.writeBmp("output.bmp", width, height, pixels);

// Direct write from off-heap ByteBuffer (zero-copy)
FastBmpWriter.writeDirectBgra("output.bmp", width, height, directBuffer);
```
