package com.example.themedgui.client.ui;

import com.example.themedgui.client.ThemedGuiModClient;
import com.example.themedgui.client.config.ThemedGuiConfig;
import com.example.themedgui.client.hud.OverlayPositionStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A dedicated full-screen edit mode for repositioning the HUD overlay by
 * dragging it directly where it will actually appear, instead of picking a
 * corner from a dropdown. Opened from the "Edit HUD position" action row in
 * ThemedConfigScreen; closing it (Done, or vanilla Escape) returns to the game.
 */
public class OverlayPositionScreen extends Screen {

    private static final int MARGIN = 10;

    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;
    private boolean dragging = false;
    private int grabOffsetX;
    private int grabOffsetY;

    public OverlayPositionScreen() {
        super(Component.literal("Move HUD Overlay"));
    }

    @Override
    protected void init() {
        ThemedGuiConfig config = ThemedGuiModClient.CONFIG;
        boxW = Math.max(1, (int) (100 * config.hudScale));
        boxH = Math.max(1, (int) (20 * config.hudScale));

        resetToStoredOrDefault();

        int buttonWidth = 90;
        this.addRenderableWidget(Button.builder(Component.literal("Reset position"), btn -> {
            OverlayPositionStore.reset();
            resetToStoredOrDefault();
        }).bounds(this.width / 2 - buttonWidth - 4, this.height - 46, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(null);
        }).bounds(this.width / 2 + 4, this.height - 46, buttonWidth, 20).build());
    }

    private void resetToStoredOrDefault() {
        ThemedGuiConfig config = ThemedGuiModClient.CONFIG;
        if (OverlayPositionStore.hasCustomPosition()) {
            boxX = OverlayPositionStore.x();
            boxY = OverlayPositionStore.y();
        } else {
            boxX = switch (config.overlayPosition) {
                case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
                case TOP_RIGHT, BOTTOM_RIGHT -> this.width - boxW - MARGIN;
            };
            boxY = switch (config.overlayPosition) {
                case TOP_LEFT, TOP_RIGHT -> MARGIN;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> this.height - boxH - MARGIN;
            };
        }
        clampBox();
    }

    private void clampBox() {
        boxX = Math.max(0, Math.min(this.width - boxW, boxX));
        boxY = Math.max(0, Math.min(this.height - boxH, boxY));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dim (rather than fully hide) the game behind so edit mode reads as a distinct state
        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        ThemedGuiConfig config = ThemedGuiModClient.CONFIG;
        int alpha = Math.max(0, Math.min(255, config.opacity));
        int color = (alpha << 24) | (config.overlayColor & 0x00FFFFFF);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, color);

        boolean hovered = isInside(mouseX, mouseY, boxX, boxY, boxW, boxH);
        graphics.outline(boxX, boxY, boxW, boxH, (dragging || hovered) ? 0xFFFFFFFF : 0x88FFFFFF);

        String hint = "Drag the box to move it";
        int hintWidth = this.font.width(hint);
        graphics.text(this.font, hint, (this.width - hintWidth) / 2, this.height - 66, 0xFFFFFFFF, true);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (isInside(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
            dragging = true;
            grabOffsetX = mouseX - boxX;
            grabOffsetY = mouseY - boxY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (dragging) {
            boxX = (int) event.x() - grabOffsetX;
            boxY = (int) event.y() - grabOffsetY;
            clampBox();
            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            OverlayPositionStore.save(boxX, boxY);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}