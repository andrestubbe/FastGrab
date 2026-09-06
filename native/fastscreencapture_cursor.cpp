#define UNICODE
#define _UNICODE
#include <windows.h>
#include <jni.h>

/**
 * FastScreenCapture Native Cursor Helper
 * 
 * Extracts current cursor position, hotspot, dimensions, and rendered 32-bit BGRA bitmap
 * using Win32 GetCursorInfo, GetIconInfo, and DrawIconEx.
 */
extern "C" {

JNIEXPORT jintArray JNICALL Java_fastscreencapture_FastCursor_nativeCaptureCursor(
    JNIEnv* env, jclass cls, jintArray metaOut) {

    CURSORINFO ci = { sizeof(CURSORINFO) };
    if (!GetCursorInfo(&ci)) return nullptr;

    // Check if cursor is showing
    if (!(ci.flags & CURSOR_SHOWING)) return nullptr;
    if (!ci.hCursor) return nullptr;

    ICONINFO ii = { 0 };
    if (!GetIconInfo(ci.hCursor, &ii)) return nullptr;

    int cw = GetSystemMetrics(SM_CXCURSOR);
    int ch = GetSystemMetrics(SM_CYCURSOR);
    if (cw <= 0) cw = 32;
    if (ch <= 0) ch = 32;

    HDC hdcScreen = GetDC(NULL);
    HDC hdcMem = CreateCompatibleDC(hdcScreen);

    BITMAPINFO bmi = { 0 };
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = cw;
    bmi.bmiHeader.biHeight = -ch; // Top-down
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    void* pBits = nullptr;
    HBITMAP hbm = CreateDIBSection(hdcMem, &bmi, DIB_RGB_COLORS, &pBits, NULL, 0);
    HBITMAP hbmOld = (HBITMAP)SelectObject(hdcMem, hbm);

    memset(pBits, 0, cw * ch * 4);
    DrawIconEx(hdcMem, 0, 0, ci.hCursor, cw, ch, 0, NULL, DI_NORMAL);

    // metaOut: [posX, posY, hotspotX, hotspotY, width, height]
    jint meta[6] = {
        ci.ptScreenPos.x,
        ci.ptScreenPos.y,
        (jint)ii.xHotspot,
        (jint)ii.yHotspot,
        cw,
        ch
    };
    env->SetIntArrayRegion(metaOut, 0, 6, meta);

    jintArray pixelResult = env->NewIntArray(cw * ch);
    env->SetIntArrayRegion(pixelResult, 0, cw * ch, (const jint*)pBits);

    SelectObject(hdcMem, hbmOld);
    DeleteObject(hbm);
    DeleteDC(hdcMem);
    ReleaseDC(NULL, hdcScreen);

    if (ii.hbmColor) DeleteObject(ii.hbmColor);
    if (ii.hbmMask) DeleteObject(ii.hbmMask);

    return pixelResult;
}

}
