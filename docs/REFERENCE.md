# FastGrab API & CLI Reference 📖

Complete guide for CLI switches, hotkey configuration, and Java API usage.

---

## Command Line Interface (CLI)

```bash
FastGrab.bat [options]
```

### Options

| Switch | Long Option | Argument | Description | Default |
|:---|:---|:---:|:---|:---:|
| `-d` | `--daemon` | - | Runs continuously in the background listening for hotkeys | `false` |
| `-k` | `--hotkey` | `<KEY>` | Hotkey keycode name (`F1`-`F12`, `PRINTSCREEN`) | `F10` |
| `-o` | `--out` | `<path.bmp>` | Target file path for single capture | Auto timestamp |
| `-r` | `--rect` | `<x,y,w,h>` | Explicit capture region coordinates | Full screen |
| `-b` | `--burst` | `<count>` | Consecutive frame burst count | `1` |
| `-h` | `--help` | - | Displays usage instructions | - |

---

## Java API Reference

### `FastBmpWriter`
```java
// Save raw ARGB pixel array as bit-perfect uncompressed BMP
FastBmpWriter.writeBmp("output.bmp", width, height, pixels);

// Direct write from off-heap ByteBuffer (zero-copy)
FastBmpWriter.writeDirectBgra("output.bmp", width, height, directBuffer);
```
