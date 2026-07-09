package com.example.themedgui.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A tiny, self-contained toast queue: call show("Saved") whenever something
 * worth confirming happens, and call draw(...) once per frame from the
 * screen's extractRenderState. Each toast fades in, holds, then fades out on
 * its own; multiple toasts stack upward so a burst of quick saves doesn't
 * just overwrite one message.
 */
public class Toast {

    private static final long FADE_IN_MS = 120;
    private static final long HOLD_MS = 1100;
    private static final long FADE_OUT_MS = 260;
    private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
    private static final int MAX_STACK = 3;

    private record Entry(String text, long shownAt) {
        float progress() {
            long elapsed = System.currentTimeMillis() - shownAt;
            if (elapsed < FADE_IN_MS) {
                return elapsed / (float) FADE_IN_MS;
            }
            if (elapsed < FADE_IN_MS + HOLD_MS) {
                return 1f;
            }
            long fadeElapsed = elapsed - FADE_IN_MS - HOLD_MS;
            return 1f - Math.min(1f, fadeElapsed / (float) FADE_OUT_MS);
        }

        boolean expired() {
            return System.currentTimeMillis() - shownAt >= TOTAL_MS;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    public void show(String text) {
        entries.addLast(new Entry(text, System.currentTimeMillis()));
        while (entries.size() > MAX_STACK) {
            entries.removeFirst();
        }
    }

    /** Draws every active toast anchored to the bottom-right of the given rectangle (typically the panel bounds), and prunes expired ones. */
    public void draw(GuiGraphicsExtractor graphics, Font font, UiPalette palette, int anchorRight, int anchorBottom) {
        entries.removeIf(Entry::expired);

        int y = anchorBottom - 12;
        // Newest toast lowest, older ones stack upward above it.
        for (Entry entry : entries) {
            float t = entry.progress();
            // A slight upward drift as it fades in reads as "rising into view".
            int riseOffset = (int) ((1f - t) * 6);

            int textWidth = font.width(entry.text());
            int boxW = textWidth + 16;
            int boxH = font.lineHeight + 10;
            int x = anchorRight - boxW - 10;
            int boxY = y - boxH + riseOffset;

            graphics.fill(x, boxY, x + boxW, boxY + boxH, withAlpha(palette.tooltipBack(), t));
            graphics.outline(x, boxY, boxW, boxH, withAlpha(palette.accent(), t));
            graphics.text(font, entry.text(), x + 8, boxY + 5, withAlpha(palette.tooltipText(), t), false);

            y = boxY - 6;
        }
    }

    private static int withAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, Math.min(1f, factor)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }
}