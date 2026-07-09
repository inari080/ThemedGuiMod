package com.example.themedgui.client.ui;

/**
 * Purely decorative, cheap ambient motion for the screen's dimmed margin:
 * a few soft blobs of accent-tinted light drifting in slow independent
 * orbits, plus a gentle "breathing" glow just outside the panel border.
 * Everything here is a pure function of time (System.currentTimeMillis()),
 * so there's no per-frame allocation or state to manage - just call
 * draw(...) once per frame.
 *
 * Deliberately kept subtle: this only paints in the ~20px dimmed margin
 * around the panel and a thin ring just outside its border, so it never
 * competes with the actual settings text for legibility.
 */
public class AmbientBackdrop {

    private AmbientBackdrop() {}

    private static final int BLOB_COUNT = 4;

    /** Drifting soft blobs across the full screen, meant to peek through the dimmed area outside the panel. */
    public static void drawBlobs(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int screenW, int screenH, int accentColor, float intensity) {
        if (intensity <= 0f) return;
        double t = System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < BLOB_COUNT; i++) {
            // Each blob gets its own phase/radius/speed so they don't move in lockstep.
            double phase = i * (Math.PI * 2 / BLOB_COUNT);
            double speed = 0.15 + i * 0.05;
            double orbitX = 0.5 + 0.38 * Math.cos(t * speed + phase);
            double orbitY = 0.5 + 0.38 * Math.sin(t * speed * 0.8 + phase * 1.3);

            int cx = (int) (orbitX * screenW);
            int cy = (int) (orbitY * screenH);
            int size = (int) (Math.min(screenW, screenH) * 0.12);

            // Approximate a soft radial glow with 3 nested translucent squares (cheap, no shader needed).
            drawSoftSquare(graphics, cx, cy, size, accentColor, 0.05f * intensity);
            drawSoftSquare(graphics, cx, cy, (int) (size * 0.6), accentColor, 0.06f * intensity);
            drawSoftSquare(graphics, cx, cy, (int) (size * 0.3), accentColor, 0.07f * intensity);
        }
    }

    /** A slow pulsing outline just outside the panel border, like the panel is gently breathing. */
    public static void drawPanelGlow(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int panelX, int panelY, int panelW, int panelH, int accentColor, float alpha) {
        double t = System.currentTimeMillis() / 1000.0;
        float pulse = (float) (0.5 + 0.5 * Math.sin(t * 1.3));
        int ringAlpha = Math.round((40 + pulse * 50) * alpha);
        int color = (clampByte(ringAlpha) << 24) | (accentColor & 0x00FFFFFF);
        int pad = 2;
        graphics.outline(panelX - pad, panelY - pad, panelW + pad * 2, panelH + pad * 2, color);
    }

    private static void drawSoftSquare(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int cx, int cy, int size, int rgb, float alpha) {
        int a = clampByte(Math.round(255 * alpha));
        int color = (a << 24) | (rgb & 0x00FFFFFF);
        graphics.fill(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, color);
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }
}