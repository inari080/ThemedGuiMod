package com.example.themedgui.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Compact HSV color picker: a saturation/value square for the current hue,
 * a hue strip, a hex text field, and a live preview swatch. Changes are
 * applied live via onConfirm as the user drags, so "Cancel" restores the
 * color that was active when the screen opened.
 */
public class ColorPickerScreen extends Screen {

    private static final int SV_SIZE = 120;
    private static final int SV_GRID = 24; // cells per axis; kept coarse since each cell is its own fill() call
    private static final int HUE_WIDTH = 16;
    private static final int GAP = 10;
    private static final int SWATCH_SIZE = 32;

    private final Screen parent;
    private final UiTheme theme;
    private final int originalRgb;
    private final Consumer<Integer> onConfirm;

    private float hue;
    private float saturation;
    private float value;

    private EditBox hexBox;
    private boolean suppressHexResponder = false;
    private boolean draggingSv = false;
    private boolean draggingHue = false;

    private int panelX, panelY, panelW, panelH;
    private int svX, svY, hueX, hueY;

    public ColorPickerScreen(Screen parent, UiTheme theme, int initialRgb, Consumer<Integer> onConfirm) {
        super(Component.literal("Choose Color"));
        this.parent = parent;
        this.theme = theme;
        this.originalRgb = initialRgb & 0xFFFFFF;
        this.onConfirm = onConfirm;
        float[] hsv = ColorMath.rgbToHsv(this.originalRgb);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    private int currentRgb() {
        return ColorMath.hsvToRgb(hue, saturation, value);
    }

    @Override
    protected void init() {
        panelW = GAP * 3 + SV_SIZE + HUE_WIDTH;
        panelH = GAP * 4 + SV_SIZE + 24 + SWATCH_SIZE;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        svX = panelX + GAP;
        svY = panelY + GAP + 20;
        hueX = svX + SV_SIZE + GAP;
        hueY = svY;

        hexBox = new EditBox(this.font, panelX + GAP, panelY + panelH - SWATCH_SIZE - GAP - 20,
                panelW - GAP * 2 - SWATCH_SIZE - GAP, 18, Component.literal("Hex"));
        hexBox.setValue(ColorMath.toHex(currentRgb()));
        hexBox.setResponder(text -> {
            if (suppressHexResponder) return;
            Integer parsed = ColorMath.parseHex(text);
            if (parsed != null) {
                float[] hsv = ColorMath.rgbToHsv(parsed);
                hue = hsv[0];
                saturation = hsv[1];
                value = hsv[2];
                onConfirm.accept(currentRgb());
            }
        });
        this.addRenderableWidget(hexBox);

        int buttonWidth = (panelW - GAP * 3) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            onConfirm.accept(originalRgb);
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(panelX + GAP, panelY + panelH - GAP - 20, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            if (parent instanceof ThemedConfigScreen configScreen) {
                configScreen.notifySaved("Color");
            }
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(panelX + GAP * 2 + buttonWidth, panelY + panelH - GAP - 20, buttonWidth, 20).build());
    }

    private void updateHexBoxText() {
        suppressHexResponder = true;
        hexBox.setValue(ColorMath.toHex(currentRgb()));
        suppressHexResponder = false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UiPalette palette = theme.palette();

        graphics.fill(0, 0, this.width, this.height, palette.backdrop());
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panel());
        graphics.outline(panelX, panelY, panelW, panelH, palette.panelBorder());
        graphics.text(this.font, this.title, panelX + GAP, panelY + GAP, palette.text(), false);

        // Saturation/value square for the current hue, drawn as a coarse grid
        int cell = SV_SIZE / SV_GRID;
        for (int gy = 0; gy < SV_GRID; gy++) {
            float v = 1f - (gy + 0.5f) / SV_GRID;
            for (int gx = 0; gx < SV_GRID; gx++) {
                float s = (gx + 0.5f) / SV_GRID;
                int rgb = ColorMath.hsvToRgb(hue, s, v);
                int x = svX + gx * cell;
                int y = svY + gy * cell;
                graphics.fill(x, y, x + cell + 1, y + cell + 1, 0xFF000000 | rgb);
            }
        }
        graphics.outline(svX, svY, SV_SIZE, SV_SIZE, palette.panelBorder());

        // Cursor over the SV square at the current saturation/value
        int cursorX = svX + Math.round(saturation * SV_SIZE);
        int cursorY = svY + Math.round((1 - value) * SV_SIZE);
        drawCursorRing(graphics, cursorX, cursorY);

        // Hue strip: 6 gradient bands approximating the full hue wheel
        int bands = 12;
        int bandH = SV_SIZE / bands;
        for (int i = 0; i < bands; i++) {
            float h = i * (360f / bands);
            int rgb = ColorMath.hsvToRgb(h, 1f, 1f);
            int y = hueY + i * bandH;
            graphics.fill(hueX, y, hueX + HUE_WIDTH, y + bandH + 1, 0xFF000000 | rgb);
        }
        graphics.outline(hueX, hueY, HUE_WIDTH, SV_SIZE, palette.panelBorder());
        int hueMarkerY = hueY + Math.round((hue / 360f) * SV_SIZE);
        graphics.fill(hueX - 2, hueMarkerY - 1, hueX + HUE_WIDTH + 2, hueMarkerY + 1, palette.text());

        // Live preview swatch
        int swatchX = panelX + panelW - GAP - SWATCH_SIZE;
        int swatchY = panelY + panelH - GAP - SWATCH_SIZE;
        graphics.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | currentRgb());
        graphics.outline(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, palette.panelBorder());

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawCursorRing(GuiGraphicsExtractor graphics, int cx, int cy) {
        int r = 4;
        graphics.fill(cx - r, cy - 1, cx + r, cy + 1, 0xFFFFFFFF);
        graphics.fill(cx - 1, cy - r, cx + 1, cy + r, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (isInside(mouseX, mouseY, svX, svY, SV_SIZE, SV_SIZE)) {
            draggingSv = true;
            updateSvFromMouse(mouseX, mouseY);
            return true;
        }
        if (isInside(mouseX, mouseY, hueX, hueY, HUE_WIDTH, SV_SIZE)) {
            draggingHue = true;
            updateHueFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (draggingSv) {
            updateSvFromMouse((int) event.x(), (int) event.y());
            return true;
        }
        if (draggingHue) {
            updateHueFromMouse((int) event.y());
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSv = false;
        draggingHue = false;
        return super.mouseReleased(event);
    }

    private void updateSvFromMouse(int mouseX, int mouseY) {
        saturation = clamp01((mouseX - svX) / (float) SV_SIZE);
        value = 1f - clamp01((mouseY - svY) / (float) SV_SIZE);
        onConfirm.accept(currentRgb());
        updateHexBoxText();
    }

    private void updateHueFromMouse(int mouseY) {
        hue = clamp01((mouseY - hueY) / (float) SV_SIZE) * 360f;
        onConfirm.accept(currentRgb());
        updateHexBoxText();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static boolean isInside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}