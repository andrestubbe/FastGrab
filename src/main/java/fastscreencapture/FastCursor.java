package fastscreencapture;

import fastcore.FastCore;

/**
 * FastCursor — High-speed native Windows cursor capture and alpha blender.
 *
 * Extracts the exact current cursor shape (arrow, hand, I-beam, etc.) and
 * alpha-blends it directly into a raw 32-bit ARGB/BGRA screen buffer.
 */
public final class FastCursor {

    private static boolean loaded = false;

    static {
        try {
            FastCore.loadLibrary("fastscreencapture_cursor", FastCursor.class);
            loaded = true;
        } catch (Throwable t) {
            try {
                System.loadLibrary("fastscreencapture_cursor");
                loaded = true;
            } catch (Throwable ignored) {
                loaded = false;
            }
        }
    }

    private FastCursor() {}

    public static boolean isAvailable() {
        return loaded;
    }

    /**
     * Native hook to capture current cursor shape and coordinates.
     *
     * @param metaOut int[6] receiving: [posX, posY, hotspotX, hotspotY, cursorWidth, cursorHeight]
     * @return int[] array of cursor pixels (32-bit ARGB/BGRA with alpha), or null if invisible/failed.
     */
    public static native int[] nativeCaptureCursor(int[] metaOut);

    /**
     * Overlays the current cursor directly onto a screen pixel buffer.
     *
     * @param screenPixels destination screen buffer (32-bit ARGB or BGRA)
     * @param screenW screen width
     * @param screenH screen height
     * @param originX screen left origin (e.g. 0 for full screen, or crop X)
     * @param originY screen top origin (e.g. 0 for full screen, or crop Y)
     * @param isBgra true if screen buffer is BGRA, false if ARGB
     */
    public static void blendCursor(int[] screenPixels, int screenW, int screenH, int originX, int originY, boolean isBgra) {
        if (!loaded || screenPixels == null) return;

        int[] meta = new int[6];
        int[] cursorPixels = nativeCaptureCursor(meta);
        if (cursorPixels == null) return;

        int posX = meta[0];
        int posY = meta[1];
        int hotX = meta[2];
        int hotY = meta[3];
        int curW = meta[4];
        int curH = meta[5];

        // Top-left destination corner where cursor should be drawn
        int drawX = (posX - hotX) - originX;
        int drawY = (posY - hotY) - originY;

        for (int cy = 0; cy < curH; cy++) {
            int targetY = drawY + cy;
            if (targetY < 0 || targetY >= screenH) continue;

            int screenRowStart = targetY * screenW;
            int cursorRowStart = cy * curW;

            for (int cx = 0; cx < curW; cx++) {
                int targetX = drawX + cx;
                if (targetX < 0 || targetX >= screenW) continue;

                int curPixel = cursorPixels[cursorRowStart + cx];
                int alpha = (curPixel >> 24) & 0xFF;

                if (alpha == 0) {
                    continue; // Fully transparent
                }

                int dstIdx = screenRowStart + targetX;
                if (alpha == 255) {
                    // Fully opaque replacement
                    screenPixels[dstIdx] = curPixel;
                } else {
                    // Standard Alpha Blending
                    int dstPixel = screenPixels[dstIdx];

                    int srcR, srcG, srcB;
                    int dstR, dstG, dstB;

                    if (isBgra) {
                        // BGRA format: B is lowest byte, R is high byte
                        srcB = curPixel & 0xFF;
                        srcG = (curPixel >> 8) & 0xFF;
                        srcR = (curPixel >> 16) & 0xFF;

                        dstB = dstPixel & 0xFF;
                        dstG = (dstPixel >> 8) & 0xFF;
                        dstR = (dstPixel >> 16) & 0xFF;

                        int outR = (srcR * alpha + dstR * (255 - alpha)) / 255;
                        int outG = (srcG * alpha + dstG * (255 - alpha)) / 255;
                        int outB = (srcB * alpha + dstB * (255 - alpha)) / 255;

                        screenPixels[dstIdx] = (0xFF << 24) | (outR << 16) | (outG << 8) | outB;
                    } else {
                        // ARGB format
                        srcR = (curPixel >> 16) & 0xFF;
                        srcG = (curPixel >> 8) & 0xFF;
                        srcB = curPixel & 0xFF;

                        dstR = (dstPixel >> 16) & 0xFF;
                        dstG = (dstPixel >> 8) & 0xFF;
                        dstB = dstPixel & 0xFF;

                        int outR = (srcR * alpha + dstR * (255 - alpha)) / 255;
                        int outG = (srcG * alpha + dstG * (255 - alpha)) / 255;
                        int outB = (srcB * alpha + dstB * (255 - alpha)) / 255;

                        screenPixels[dstIdx] = (0xFF << 24) | (outR << 16) | (outG << 8) | outB;
                    }
                }
            }
        }
    }
}
