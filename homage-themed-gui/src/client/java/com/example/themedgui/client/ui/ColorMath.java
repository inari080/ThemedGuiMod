package com.example.themedgui.client.ui;

/** Plain HSV <-> RGB conversion, no external deps. Colors are packed 0xRRGGBB (no alpha). */
public final class ColorMath {

    private ColorMath() {}

    public static int hsvToRgb(float hue, float saturation, float value) {
        float h = ((hue % 360f) + 360f) % 360f;
        float c = value * saturation;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = value - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        int ri = clamp(Math.round((r + m) * 255));
        int gi = clamp(Math.round((g + m) * 255));
        int bi = clamp(Math.round((b + m) * 255));
        return (ri << 16) | (gi << 8) | bi;
    }

    /** Returns {hue (0-360), saturation (0-1), value (0-1)}. */
    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == r) {
            hue = 60 * (((g - b) / delta) % 6);
        } else if (max == g) {
            hue = 60 * (((b - r) / delta) + 2);
        } else {
            hue = 60 * (((r - g) / delta) + 4);
        }
        if (hue < 0) hue += 360;

        float saturation = max == 0 ? 0 : delta / max;
        return new float[]{hue, saturation, max};
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /** Parses "#RRGGBB" or "RRGGBB". Returns null if the text isn't a valid 6-digit hex color. */
    public static Integer parseHex(String text) {
        String cleaned = text.startsWith("#") ? text.substring(1) : text;
        if (cleaned.length() != 6) return null;
        try {
            return Integer.parseInt(cleaned, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}