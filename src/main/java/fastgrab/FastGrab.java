package fastgrab;

import fastscreen.FastScreen;
import fasthotkey.FastHotkey;
import fasthotkey.KeyCodes;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FastGrab — Ultra-fast uncompressed screen capture CLI and global hotkey daemon.
 * 
 * Captures bit-perfect uncompressed frames in sub-millisecond time using DXGI
 * and writes directly to disk without Java heap GC churn or PNG compression lags.
 */
public class FastGrab {

    private static final String VERSION = "0.1.0";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");

    // Virtual key for PrintScreen in Windows Win32 API is 0x2C (VK_SNAPSHOT)
    private static final int VK_SNAPSHOT_CODE = 0x2C;

    public static void main(String[] args) {
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

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
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

        if (daemonMode) {
            runDaemon(hotkeyCode, hotkeyName);
        } else {
            runOneShot(outputFile, captureX, captureY, captureW, captureH, burstCount);
        }
    }

    private static void printBanner() {
        System.out.println("===============================================================");
        System.out.println("⚡ FastGrab v" + VERSION + " — Bit-Perfect Uncompressed Screen Grabber");
        System.out.println("   DirectX 11 DXGI Duplication | Direct-to-Disk | Zero GC");
        System.out.println("===============================================================");
    }

    private static void printHelp() {
        printBanner();
        System.out.println("Usage: fastgrab [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -d, --daemon            Run in background listening for global hotkey");
        System.out.println("  -k, --hotkey <KEY>      Hotkey in daemon mode: F1-F12, PRINTSCREEN (Default: F10)");
        System.out.println("  -o, --out <path.bmp>    Output file path (Default: grab_TIMESTAMP.bmp)");
        System.out.println("  -r, --rect <x,y,w,h>    Sub-region to capture (Default: full screen)");
        System.out.println("  -b, --burst <count>     Capture burst sequence of uncompressed frames");
        System.out.println("  -h, --help              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  fastgrab                               # Instant bit-perfect full screen grab");
        System.out.println("  fastgrab --daemon --hotkey F10         # Background daemon listening on F10");
        System.out.println("  fastgrab --rect 100,100,800,600        # Grab specific 800x600 region");
        System.out.println("  fastgrab --burst 30                    # Capture 30 uncompressed frames in a burst");
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
        AtomicBoolean isCapturing = new AtomicBoolean(false);

        System.out.printf("[DAEMON] Registering global hotkey [%s]...\n", hotkeyName);
        FastHotkey.register(1, 0, hotkeyCode, id -> {
            if (isCapturing.compareAndSet(false, true)) {
                try {
                    long t0 = System.nanoTime();
                    int[] pixels = finalScreen.captureRaw(0, 0, 0, 0);
                    long t1 = System.nanoTime();

                    if (pixels != null) {
                        String filename = "grabs/grab_" + DATE_FORMAT.format(new Date()) + ".bmp";
                        FastBmpWriter.writeBmp(filename, 1920, 1080, pixels);
                        long t2 = System.nanoTime();

                        int count = grabCount.incrementAndGet();
                        double totalMs = (t2 - t0) / 1_000_000.0;
                        long size = new File(filename).length();
                        System.out.printf("[HOTKEY #%d] Captured in %.2f ms -> %s (%,d KB)\n", count, totalMs, filename, size / 1024);
                    }
                } catch (Exception ex) {
                    System.err.println("[ERROR] Capture failed: " + ex.getMessage());
                } finally {
                    isCapturing.set(false);
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
        }, "FastGrab-HotkeyListener");
        hotkeyThread.setDaemon(true);
        hotkeyThread.start();

        System.out.println();
        System.out.printf("🟢 [DAEMON READY] Press [%s] anywhere in Windows for an instant uncompressed grab!\n", hotkeyName);
        System.out.println("   Images will be saved to: " + Paths.get("grabs").toAbsolutePath());
        System.out.println("   Press [ENTER] in this console to exit daemon mode.\n");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        System.out.println("[DAEMON] Shutting down...");
        try {
            FastHotkey.stop();
        } catch (Exception ignored) {}
        finalScreen.dispose();
        System.out.println("[DAEMON] Bye!");
    }
}
