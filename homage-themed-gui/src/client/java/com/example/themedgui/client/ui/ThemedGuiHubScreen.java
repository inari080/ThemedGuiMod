package com.example.themedgui.client.ui;

import com.example.themedgui.client.api.AddonManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ModMenu-style entry point: a list of registered mods on the left, opening
 * that mod's own {@link ThemedConfigScreen} on the right when clicked.
 * <p>
 * The list is {@code ownEntries} (mods the caller passes in directly, e.g.
 * ThemedGuiMod's own demo config) followed by everything discovered through
 * {@link AddonManager#getEntries()} (other mods' "themedgui:addon"
 * entrypoints).
 */
public class ThemedGuiHubScreen extends Screen {

    private static final int SIDEBAR_WIDTH = 120;
    private static final int ROW_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 28;

    private final List<AddonManager.AddonEntry> entries = new ArrayList<>();
    private final UiTheme theme = UiSettings.INSTANCE.getTheme();

    public ThemedGuiHubScreen(List<AddonManager.AddonEntry> ownEntries) {
        super(Component.literal("Mods"));
        entries.addAll(ownEntries);
        entries.addAll(AddonManager.getEntries());
    }

    private int panelX() {
        return 20;
    }

    private int panelY() {
        return 20;
    }

    private int listY() {
        return panelY() + HEADER_HEIGHT;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UiPalette palette = theme.palette();
        int panelX = panelX();
        int panelY = panelY();
        int panelW = this.width - 40;
        int panelH = this.height - 40;

        graphics.fill(0, 0, this.width, this.height, palette.backdrop());
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, palette.panel());
        graphics.outline(panelX, panelY, panelW, panelH, palette.panelBorder());

        graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER_HEIGHT, palette.header());
        graphics.text(this.font, "Mods", panelX + 10, panelY + 9, palette.text(), false);

        int listY = listY();
        int listH = panelH - HEADER_HEIGHT;

        if (entries.isEmpty()) {
            graphics.text(this.font, "No mods registered yet.", panelX + 10, listY + 10, palette.mutedText(), false);
        }

        for (int i = 0; i < entries.size(); i++) {
            AddonManager.AddonEntry entry = entries.get(i);
            int rowY = listY + i * ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT);
            if (hovered) {
                graphics.fill(panelX, rowY, panelX + SIDEBAR_WIDTH, rowY + ROW_HEIGHT, palette.hover());
            }
            graphics.text(this.font, entry.displayName(), panelX + 10, rowY + (ROW_HEIGHT - 8) / 2,
                    palette.text(), false);
        }

        graphics.outline(panelX + SIDEBAR_WIDTH, listY, 1, listH, palette.line());

        String hint = "Select a mod to edit its settings";
        int hintW = this.font.width(hint);
        graphics.text(this.font, hint, panelX + SIDEBAR_WIDTH + ((panelW - SIDEBAR_WIDTH) - hintW) / 2,
                listY + listH / 2, palette.mutedText(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int panelX = panelX();
        int listY = listY();

        for (int i = 0; i < entries.size(); i++) {
            int rowY = listY + i * ROW_HEIGHT;
            if (isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT)) {
                AddonManager.AddonEntry entry = entries.get(i);
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ThemedConfigScreen(this, entry.registry(),
                            new ScreenBranding(entry.displayName(), entry.icon())));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean isInside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
