package fastscreencapture;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * High-speed uncompressed Windows BMP writer with zero intermediate JVM allocations.
 * Writes raw BGRA / RGBA buffers directly to disk with standard 54-byte BMP headers.
 */
public final class FastBmpWriter {

    private FastBmpWriter() {}

    /**
     * Writes raw 32-bit ARGB/BGRA pixels to a standard uncompressed Windows BMP file.
     * Compatible with Windows Photo Viewer, Photoshop, Paint, Chrome, etc.
     *
     * @param filePath target output path
     * @param width image width in pixels
     * @param height image height in pixels
     * @param pixelData int[] pixel array (ARGB format: 0xAARRGGBB)
     * @throws IOException on write error
     */
    public static void writeBmp(String filePath, int width, int height, int[] pixelData) throws IOException {
        int imageSize = width * height * 4;
        int fileSize = 54 + imageSize;

        ByteBuffer header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN);

        // --- BITMAPFILEHEADER (14 bytes) ---
        header.put((byte) 'B');
        header.put((byte) 'M');
        header.putInt(fileSize);       // Total file size
        header.putShort((short) 0);    // Reserved 1
        header.putShort((short) 0);    // Reserved 2
        header.putInt(54);             // Pixel data offset

        // --- BITMAPINFOHEADER (40 bytes) ---
        header.putInt(40);             // Header size
        header.putInt(width);          // Image width
        header.putInt(-height);        // Negative height = top-down DIB (no row flip needed!)
        header.putShort((short) 1);    // Color planes
        header.putShort((short) 32);   // Bits per pixel (32-bit BGRA)
        header.putInt(0);              // BI_RGB (uncompressed)
        header.putInt(imageSize);      // Image payload size
        header.putInt(2835);           // Horizontal resolution (72 DPI)
        header.putInt(2835);           // Vertical resolution (72 DPI)
        header.putInt(0);              // Colors in color table
        header.putInt(0);              // Important color count
        header.flip();

        // Convert ARGB to little-endian BGRA byte stream directly in byte buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(imageSize).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < pixelData.length; i++) {
            int p = pixelData[i];
            buffer.put((byte) (p & 0xFF));         // Blue
            buffer.put((byte) ((p >> 8) & 0xFF));  // Green
            buffer.put((byte) ((p >> 16) & 0xFF)); // Red
            buffer.put((byte) ((p >> 24) & 0xFF)); // Alpha
        }
        buffer.flip();

        try (FileOutputStream fos = new FileOutputStream(filePath);
             FileChannel channel = fos.getChannel()) {
            channel.write(header);
            channel.write(buffer);
        }
    }

    /**
     * Writes a direct ByteBuffer (BGRA from FastScreen direct JNI) straight to disk.
     */
    public static void writeDirectBgra(String filePath, int width, int height, ByteBuffer bgraDirectBuffer) throws IOException {
        int imageSize = width * height * 4;
        int fileSize = 54 + imageSize;

        ByteBuffer header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte) 'B');
        header.put((byte) 'M');
        header.putInt(fileSize);
        header.putShort((short) 0);
        header.putShort((short) 0);
        header.putInt(54);

        header.putInt(40);
        header.putInt(width);
        header.putInt(-height); // Top-down
        header.putShort((short) 1);
        header.putShort((short) 32);
        header.putInt(0);
        header.putInt(imageSize);
        header.putInt(2835);
        header.putInt(2835);
        header.putInt(0);
        header.putInt(0);
        header.flip();

        bgraDirectBuffer.rewind();
        try (FileOutputStream fos = new FileOutputStream(filePath);
             FileChannel channel = fos.getChannel()) {
            channel.write(header);
            channel.write(bgraDirectBuffer);
        }
    }
}
