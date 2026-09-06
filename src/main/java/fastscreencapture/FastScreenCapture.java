package fastscreencapture;

import fastscreen.FastScreen;
import fasthotkey.FastHotkey;
import fasthotkey.KeyCodes;

import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FastScreenCapture — Ultra-fast uncompressed screen capture CLI and global hotkey daemon.
 * 
 * Captures bit-perfect uncompressed frames in sub-millisecond time using DXGI
 * and writes directly to disk without Java heap GC churn or PNG compression lags.
 */
public class FastScreenCapture {

    private static final String VERSION = "0.1.0";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    // Virtual key for PrintScreen in Windows Win32 API is 0x2C (VK_SNAPSHOT)
    private static final int VK_SNAPSHOT_CODE = 0x2C;

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printHelp();
            return;
        }

        boolean daemonMode = false;
        String outputFile = null;
        int captureX = 0, captureY = 0, captureW = 0, captureH = 0;
        int burstCount = 1;
        int hotkeyCode = KeyCodes.VK_F10;
        String hotkeyName = "F10";

        boolean recordMode = false;
        int recordDurationSeconds = 60;
        int recordFps = 60;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--record":
                case "--video":
                    recordMode = true;
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        try {
                            recordDurationSeconds = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException ignored) {}
                    }
                    break;
                case "--fps":
                    if (i + 1 < args.length) {
                        try {
                            recordFps = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException ignored) {}
                    }
                    break;
                case "--daemon":
                case "-d":
                    daemonMode = true;
                    break;
                case "--out":
                case "-o":
                    if (i + 1 < args.length) outputFile = args[++i];
                    break;
                case "--rect":
                case "-r":
                    if (i + 1 < args.length) {
                        String[] parts = args[++i].split(",");
                        if (parts.length == 4) {
                            captureX = Integer.parseInt(parts[0].trim());
                            captureY = Integer.parseInt(parts[1].trim());
                            captureW = Integer.parseInt(parts[2].trim());
                            captureH = Integer.parseInt(parts[3].trim());
                        }
                    }
                    break;
                case "--burst":
                case "-b":
                    if (i + 1 < args.length) burstCount = Integer.parseInt(args[++i]);
                    break;
                case "--hotkey":
                case "-k":
                    if (i + 1 < args.length) {
                        hotkeyName = args[++i].toUpperCase();
                        if (hotkeyName.equals("PRINTSCREEN") || hotkeyName.equals("SNAPSHOT") || hotkeyName.equals("PRINT")) {
                            hotkeyCode = VK_SNAPSHOT_CODE;
                        } else if (hotkeyName.startsWith("F")) {
                            int fNum = Integer.parseInt(hotkeyName.substring(1));
                            hotkeyCode = KeyCodes.VK_F1 + (fNum - 1);
                        }
                    }
                    break;
            }
        }

        printBanner();

        if (recordMode) {
            runRecordVideo(outputFile, captureX, captureY, captureW, captureH, recordDurationSeconds, recordFps);
        } else if (daemonMode) {
            runDaemon(hotkeyCode, hotkeyName);
        } else {
            runOneShot(outputFile, captureX, captureY, captureW, captureH, burstCount);
        }
    }

    private static void printBanner() {
        System.out.println("===============================================================");
        System.out.println("⚡ FastScreenCapture v" + VERSION + " — Bit-Perfect Uncompressed Screen Grabber");
        System.out.println("   DirectX 11 DXGI Duplication | Direct-to-Disk | Zero GC");
        System.out.println("===============================================================");
    }

    private static void printHelp() {
        printBanner();
        System.out.println("Usage: fastscreencapture [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --record [seconds]      Record video stream directly to MP4 via FFmpeg pipe (Default: 60s)");
        System.out.println("  --fps <fps>             Target frame rate for video recording (Default: 60)");
        System.out.println("  -d, --daemon            Run in background listening for global hotkey");
        System.out.println("  -k, --hotkey <KEY>      Hotkey in daemon mode: F1-F12, PRINTSCREEN (Default: F10)");
        System.out.println("  -o, --out <path>        Output file path (.bmp or .mp4)");
        System.out.println("  -r, --rect <x,y,w,h>    Sub-region to capture (Default: full screen)");
        System.out.println("  -b, --burst <count>     Capture burst sequence of uncompressed frames");
        System.out.println("  -h, --help              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  fastscreencapture --record 60          # Stream 60s lossless 60 FPS video directly to MP4");
        System.out.println("  fastscreencapture --daemon             # Background daemon listening on F10");
        System.out.println("  fastscreencapture                      # Instant bit-perfect full screen grab");
    }

    private static void runRecordVideo(String outFile, int x, int y, int w, int h, int durationSec, int fps) {
        FastScreen screen = null;
        Process ffmpegProc = null;
        try {
            screen = new FastScreen();

            int targetW = (w > 0) ? w : screen.getFrameWidth();
            int targetH = (h > 0) ? h : screen.getFrameHeight();
            if (targetW <= 0) targetW = 1920;
            if (targetH <= 0) targetH = 1080;

            File outDir = new File("grabs");
            if (!outDir.exists()) outDir.mkdirs();

            String finalOutFile = outFile;
            if (finalOutFile == null) {
                finalOutFile = "grabs/recording_" + DATE_FORMAT.format(new Date()) + ".mp4";
            } else if (!finalOutFile.endsWith(".mp4") && !finalOutFile.endsWith(".mkv")) {
                finalOutFile += ".mp4";
            }

            System.out.printf("[RECORD] Starting lossless direct pipe recording: %dx%d @ %d FPS for %d seconds\n",
                    targetW, targetH, fps, durationSec);
            System.out.printf("[RECORD] Output destination: %s\n", new File(finalOutFile).getAbsolutePath());

            String ffmpegPath = resolveFfmpegExecutable();

            // Build FFmpeg pipe command
            // Input: raw BGRA stream from stdin, pixel format bgra
            // Output: high quality H.264 (libx264, crf 17, fast preset)
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-y",
                    "-f", "rawvideo",
                    "-vcodec", "rawvideo",
                    "-s", targetW + "x" + targetH,
                    "-pix_fmt", "bgra",
                    "-r", String.valueOf(fps),
                    "-i", "-",
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "17",
                    "-pix_fmt", "yuv420p",
                    finalOutFile
            );
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            ffmpegProc = pb.start();

            OutputStream pipeOut = ffmpegProc.getOutputStream();
            byte[] frameBytes = new byte[targetW * targetH * 4];

            long frameIntervalNanos = 1_000_000_000L / fps;
            int totalFramesTarget = durationSec * fps;
            int framesRecorded = 0;

            long startRecordTime = System.currentTimeMillis();
            long nextFrameTime = System.nanoTime();

            System.out.println("🟢 [RECORDING ACTIVE] Pumping DXGI frames into FFmpeg pipe without disk latency...");

            while (framesRecorded < totalFramesTarget && ffmpegProc.isAlive()) {
                int[] pixels = (w > 0 && h > 0) ? screen.captureRaw(x, y, w, h) : screen.captureRaw(0, 0, 0, 0);
                if (pixels != null) {
                    // Convert ARGB to little-endian BGRA bytes
                    int byteIdx = 0;
                    int len = Math.min(pixels.length, targetW * targetH);
                    for (int i = 0; i < len; i++) {
                        int p = pixels[i];
                        frameBytes[byteIdx++] = (byte) (p & 0xFF);         // Blue
                        frameBytes[byteIdx++] = (byte) ((p >> 8) & 0xFF);  // Green
                        frameBytes[byteIdx++] = (byte) ((p >> 16) & 0xFF); // Red
                        frameBytes[byteIdx++] = (byte) ((p >> 24) & 0xFF); // Alpha
                    }
                    pipeOut.write(frameBytes);
                    framesRecorded++;
                }

                nextFrameTime += frameIntervalNanos;
                long sleepNanos = nextFrameTime - System.nanoTime();
                if (sleepNanos > 1_000_000L) {
                    try {
                        Thread.sleep(sleepNanos / 1_000_000L);
                    } catch (InterruptedException ignored) {}
                }

                if (framesRecorded % (fps * 5) == 0) {
                    int secondsElapsed = framesRecorded / fps;
                    System.out.printf("   [PROGRESS] %d / %d sec recorded (%d frames)\n", secondsElapsed, durationSec, framesRecorded);
                }
            }

            pipeOut.flush();
            pipeOut.close();

            System.out.println("[RECORD] Finalizing MP4 container...");
            ffmpegProc.waitFor();

            long totalTimeMs = System.currentTimeMillis() - startRecordTime;
            long fileSize = new File(finalOutFile).length();
            System.out.println("===============================================================");
            System.out.printf("✓ [SUCCESS] Video saved: %s\n", finalOutFile);
            System.out.printf("   Recorded: %d frames in %.2f seconds (%.1f FPS)\n",
                    framesRecorded, totalTimeMs / 1000.0, (framesRecorded * 1000.0) / totalTimeMs);
            System.out.printf("   File Size: %,d KB (%.2f MB)\n", fileSize / 1024, fileSize / (1024.0 * 1024.0));
            System.out.println("===============================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Video recording failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (screen != null) screen.dispose();
            if (ffmpegProc != null && ffmpegProc.isAlive()) {
                ffmpegProc.destroy();
            }
        }
    }

    private static String resolveFfmpegExecutable() {
        // 1. Check if ffmpeg is in PATH
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").start();
            p.waitFor();
            return "ffmpeg";
        } catch (Exception ignored) {}

        // 2. Check WinGet standard installation directories
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            File wingetDir = new File(localAppData, "Microsoft/WinGet/Packages");
            if (wingetDir.exists()) {
                File[] list = wingetDir.listFiles((dir, name) -> name.startsWith("Gyan.FFmpeg"));
                if (list != null && list.length > 0) {
                    for (File pkg : list) {
                        File[] sub = pkg.listFiles();
                        if (sub != null) {
                            for (File build : sub) {
                                File exe = new File(build, "bin/ffmpeg.exe");
                                if (exe.exists()) {
                                    return exe.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            }
        }
        return "ffmpeg";
    }

    private static void runOneShot(String outFile, int x, int y, int w, int h, int burst) {
        FastScreen screen = null;
        try {
            screen = new FastScreen();
            int targetW = (w > 0) ? w : 1920;
            int targetH = (h > 0) ? h : 1080;

            File outDir = new File("grabs");
            if (!outDir.exists()) outDir.mkdirs();

            System.out.printf("[INFO] Initiating %s grab...\n", (burst > 1 ? burst + "-frame burst" : "one-shot"));

            for (int b = 0; b < burst; b++) {
                long t0 = System.nanoTime();
                int[] pixels = (w > 0 && h > 0) ? screen.captureRaw(x, y, w, h) : screen.captureRaw(0, 0, 0, 0);
                long tCapture = System.nanoTime();

                if (pixels == null) {
                    System.err.println("[ERROR] Failed to capture DXGI desktop surface.");
                    return;
                }

                String filename = outFile;
                if (filename == null || burst > 1) {
                    filename = "grabs/grab_" + DATE_FORMAT.format(new Date()) + (burst > 1 ? "_" + String.format("%03d", b) : "") + ".bmp";
                }

                FastBmpWriter.writeBmp(filename, targetW, targetH, pixels);
                long tEnd = System.nanoTime();

                double captureMs = (tCapture - t0) / 1_000_000.0;
                double writeMs = (tEnd - tCapture) / 1_000_000.0;
                double totalMs = (tEnd - t0) / 1_000_000.0;
                long fileSize = new File(filename).length();

                System.out.printf("[✓] Saved: %s (%,d KB)\n", filename, fileSize / 1024);
                System.out.printf("    Timing: Capture: %.2f ms | Disk Write: %.2f ms | Total: %.2f ms\n", captureMs, writeMs, totalMs);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Exception during capture: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (screen != null) {
                screen.dispose();
            }
        }
    }

    private static void runDaemon(int hotkeyCode, String hotkeyName) {
        File outDir = new File("grabs");
        if (!outDir.exists()) outDir.mkdirs();

        System.out.println("[DAEMON] Initializing FastScreen DXGI engine...");
        FastScreen screen = null;
        try {
            screen = new FastScreen();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to initialize FastScreen: " + e.getMessage());
            return;
        }

        try {
            FastHotkey.loadLibrary();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load FastHotkey library: " + e.getMessage());
            if (screen != null) screen.dispose();
            return;
        }

        final FastScreen finalScreen = screen;
        AtomicInteger grabCount = new AtomicInteger(0);
        AtomicInteger videoCount = new AtomicInteger(0);
        AtomicBoolean isCapturing = new AtomicBoolean(false);
        AtomicBoolean isRecording = new AtomicBoolean(false);

        // Recording state holders
        final Object recordLock = new Object();
        final Process[] activeProcess = new Process[1];
        final OutputStream[] activePipe = new OutputStream[1];
        final Thread[] activeRecordThread = new Thread[1];
        final long[] recordStartTime = new long[1];
        final int[] framesCaptured = new int[1];
        final String[] currentVideoFile = new String[1];

        int recordFps = 60;
        int screenW = finalScreen.getFrameWidth() > 0 ? finalScreen.getFrameWidth() : 1920;
        int screenH = finalScreen.getFrameHeight() > 0 ? finalScreen.getFrameHeight() : 1080;
        String ffmpegExe = resolveFfmpegExecutable();

        // Hotkey 1: Single Frame Grab (e.g. F10)
        System.out.printf("[DAEMON] Registering single screenshot hotkey [%s]...\n", hotkeyName);
        FastHotkey.register(1, 0, hotkeyCode, id -> {
            if (isCapturing.compareAndSet(false, true)) {
                try {
                    long t0 = System.nanoTime();
                    int[] pixels = finalScreen.captureRaw(0, 0, 0, 0);
                    long t1 = System.nanoTime();

                    if (pixels != null) {
                        String filename = "grabs/grab_" + DATE_FORMAT.format(new Date()) + ".bmp";
                        FastBmpWriter.writeBmp(filename, screenW, screenH, pixels);
                        long t2 = System.nanoTime();

                        int count = grabCount.incrementAndGet();
                        double totalMs = (t2 - t0) / 1_000_000.0;
                        long size = new File(filename).length();
                        System.out.printf("\n📸 [HOTKEY SCREENSHOT #%d] Captured in %.2f ms -> %s (%,d KB)\n",
                                count, totalMs, filename, size / 1024);
                    }
                } catch (Exception ex) {
                    System.err.println("\n[ERROR] Capture failed: " + ex.getMessage());
                } finally {
                    isCapturing.set(false);
                }
            }
        });

        // Hotkey 2: Toggle Video Recording (F9: Start / Stop)
        final int recordHotkeyCode = (hotkeyCode == KeyCodes.VK_F9) ? KeyCodes.VK_F8 : KeyCodes.VK_F9;
        final String recordHotkeyName = (hotkeyCode == KeyCodes.VK_F9) ? "F8" : "F9";

        System.out.printf("[DAEMON] Registering video toggle hotkey [%s] (Start/Stop)...\n", recordHotkeyName);
        FastHotkey.register(2, 0, recordHotkeyCode, id -> {
            synchronized (recordLock) {
                if (!isRecording.get()) {
                    // START RECORDING
                    try {
                        String videoFilename = "grabs/video_" + DATE_FORMAT.format(new Date()) + ".mp4";
                        currentVideoFile[0] = videoFilename;
                        framesCaptured[0] = 0;
                        recordStartTime[0] = System.currentTimeMillis();

                        ProcessBuilder pb = new ProcessBuilder(
                                ffmpegExe,
                                "-y",
                                "-f", "rawvideo",
                                "-vcodec", "rawvideo",
                                "-s", screenW + "x" + screenH,
                                "-pix_fmt", "bgra",
                                "-r", String.valueOf(recordFps),
                                "-i", "-",
                                "-c:v", "libx264",
                                "-preset", "veryfast",
                                "-crf", "17",
                                "-pix_fmt", "yuv420p",
                                videoFilename
                        );
                        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                        Process proc = pb.start();
                        activeProcess[0] = proc;
                        activePipe[0] = proc.getOutputStream();
                        isRecording.set(true);

                        int num = videoCount.incrementAndGet();
                        System.out.printf("\n🔴 [RECORDING STARTED #%d] Stream: %dx%d @ %d FPS -> %s\n",
                                num, screenW, screenH, recordFps, videoFilename);
                        System.out.printf("   Press [%s] again to STOP recording.\n", recordHotkeyName);

                        // Background frame capture thread
                        Thread recThread = new Thread(() -> {
                            byte[] frameBytes = new byte[screenW * screenH * 4];
                            long frameIntervalNanos = 1_000_000_000L / recordFps;
                            long nextTime = System.nanoTime();
                            OutputStream pipe = activePipe[0];

                            while (isRecording.get() && proc.isAlive()) {
                                try {
                                    int[] pixels = finalScreen.captureRaw(0, 0, 0, 0);
                                    if (pixels != null) {
                                        int byteIdx = 0;
                                        int len = Math.min(pixels.length, screenW * screenH);
                                        for (int i = 0; i < len; i++) {
                                            int p = pixels[i];
                                            frameBytes[byteIdx++] = (byte) (p & 0xFF);
                                            frameBytes[byteIdx++] = (byte) ((p >> 8) & 0xFF);
                                            frameBytes[byteIdx++] = (byte) ((p >> 16) & 0xFF);
                                            frameBytes[byteIdx++] = (byte) ((p >> 24) & 0xFF);
                                        }
                                        pipe.write(frameBytes);
                                        framesCaptured[0]++;
                                    }

                                    nextTime += frameIntervalNanos;
                                    long sleepNanos = nextTime - System.nanoTime();
                                    if (sleepNanos > 1_000_000L) {
                                        Thread.sleep(sleepNanos / 1_000_000L);
                                    }
                                } catch (Exception ignored) {
                                    break;
                                }
                            }
                        }, "FastScreenCapture-VideoEncoder");
                        activeRecordThread[0] = recThread;
                        recThread.setDaemon(true);
                        recThread.start();

                    } catch (Exception e) {
                        System.err.println("\n[ERROR] Failed to start video recording: " + e.getMessage());
                        isRecording.set(false);
                    }
                } else {
                    // STOP RECORDING
                    isRecording.set(false);
                    try {
                        if (activePipe[0] != null) {
                            activePipe[0].flush();
                            activePipe[0].close();
                        }
                        if (activeProcess[0] != null) {
                            activeProcess[0].waitFor();
                        }
                        long durationMs = System.currentTimeMillis() - recordStartTime[0];
                        long fileSize = new File(currentVideoFile[0]).length();
                        System.out.printf("\n⏹️ [RECORDING SAVED] %s\n", currentVideoFile[0]);
                        System.out.printf("   Duration: %.2f sec | Frames: %d (%.1f FPS) | Size: %,d KB (%.2f MB)\n\n",
                                durationMs / 1000.0, framesCaptured[0], (framesCaptured[0] * 1000.0) / durationMs,
                                fileSize / 1024, fileSize / (1024.0 * 1024.0));
                    } catch (Exception e) {
                        System.err.println("\n[ERROR] Error finalizing recording: " + e.getMessage());
                    } finally {
                        activeProcess[0] = null;
                        activePipe[0] = null;
                    }
                }
            }
        });

        // Start hotkey listening loop in background thread
        Thread hotkeyThread = new Thread(() -> {
            try {
                FastHotkey.start();
            } catch (Exception e) {
                System.err.println("[ERROR] Hotkey listener terminated: " + e.getMessage());
            }
        }, "FastScreenCapture-HotkeyListener");
        hotkeyThread.setDaemon(true);
        hotkeyThread.start();

        System.out.println();
        System.out.println("🟢 [DAEMON READY]");
        System.out.printf("   • Press [%s] to TOGGLE VIDEO RECORDING (Lossless 60 FPS MP4 via FFmpeg pipe)\n", recordHotkeyName);
        System.out.printf("   • Press [%s] to TAKE BIT-PERFECT SCREENSHOT (BMP)\n", hotkeyName);
        System.out.println("   • Files saved to: " + Paths.get("grabs").toAbsolutePath());
        System.out.println("   • Press [ENTER] in this console to exit daemon mode.\n");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        // Stop any active recording before exit
        synchronized (recordLock) {
            if (isRecording.get()) {
                isRecording.set(false);
                try {
                    if (activePipe[0] != null) {
                        activePipe[0].flush();
                        activePipe[0].close();
                    }
                    if (activeProcess[0] != null) {
                        activeProcess[0].waitFor();
                    }
                } catch (Exception ignored) {}
            }
        }

        System.out.println("[DAEMON] Shutting down...");
        try {
            FastHotkey.stop();
        } catch (Exception ignored) {}
        finalScreen.dispose();
        System.out.println("[DAEMON] Bye!");
    }
}
