package com.example.themedgui.client.hud;

import com.example.themedgui.client.ThemedGuiModClient;
import com.example.themedgui.client.config.ThemedGuiConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ThemedHud {

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        ThemedGuiConfig config = ThemedGuiModClient.CONFIG;
        if (!config.enableOverlay) return;

        float scale = config.hudScale;
        int alpha = Math.max(0, Math.min(255, config.opacity));
        int color = (alpha << 24) | 0x33AAFF;

        int w = (int) (100 * scale);
        int h = (int) (20 * scale);

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int margin = 10;

        int x = switch (config.overlayPosition) {
            case TOP_LEFT, BOTTOM_LEFT -> margin;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - w - margin;
        };
        int y = switch (config.overlayPosition) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - h - margin;
        };

        graphics.fill(x, y, x + w, y + h, color);
    }
}