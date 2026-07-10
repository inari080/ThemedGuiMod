package com.example.themedgui.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Dedicated theme-picker screen with live palette previews. Opened from the
 * "UI Settings" action row in {@link ThemedConfigScreen}.
 */
public class UiSettingsScreen extends Screen {

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 90;
    private static final int CARD_GAP = 16;
    private static final int PANEL_PADDING = 24;

    private final Screen parent;
    private final ScreenTransition transition = new ScreenTransition();
    private final Anim hoverAnim = new Anim(16f);

    private UiTheme previewTheme;

    public UiSettingsScreen(Screen parent) {
        super(Component.literal("UI Settings"));
        this.parent = parent;
        this.previewTheme = UiSettings.INSTANCE.getTheme();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UiPalette palette = previewTheme.palette();
        float alpha = transition.currentAlpha();

        graphics.fill(0, 0, this.width, this.height, withAlpha(palette.backdrop(), alpha));
        AmbientBackdrop.drawBlobs(graphics, this.width, this.height, palette.accent(), alpha);

        List<UiTheme> themes = UiTheme.values();
        int panelW = themes.size() * CARD_WIDTH + (themes.size() - 1) * CARD_GAP + PANEL_PADDING * 2;
        int panelH = CARD_HEIGHT + 72;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        transition.push(graphics);
        {
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, withAlpha(palette.panel(), alpha));
            graphics.outline(panelX, panelY, panelW, panelH, withAlpha(palette.panelBorder(), alpha));
            AmbientBackdrop.drawPanelGlow(graphics, panelX, panelY, panelW, panelH, palette.accent(), alpha);

            graphics.fill(panelX, panelY, panelX + panelW, panelY + 28, withAlpha(palette.header(), alpha));
            graphics.text(this.font, this.title, panelX + 12, panelY + 10, withAlpha(palette.text(), alpha), false);
            graphics.fill(panelX, panelY + 27, panelX + panelW, panelY + 28, withAlpha(palette.line(), alpha));

            String subtitle = "Click a theme to apply it";
            graphics.text(this.font, subtitle, panelX + 12, panelY + 36, withAlpha(palette.mutedText(), alpha), false);

            UiTheme selected = UiSettings.INSTANCE.getTheme();
            int cardsY = panelY + 52;
            int cardsX = panelX + PANEL_PADDING;

            for (int i = 0; i < themes.size(); i++) {
                UiTheme theme = themes.get(i);
                int cardX = cardsX + i * (CARD_WIDTH + CARD_GAP);
                boolean isSelected = theme == selected;
                boolean isHovered = isInside(mouseX, mouseY, cardX, cardsY, CARD_WIDTH, CARD_HEIGHT);

                hoverAnim.setTarget(theme, isHovered ? 1f : 0f);
                float hoverT = hoverAnim.tick(theme);

                if (isSelected || hoverT > 0.01f) {
                    int pad = Math.round(hoverT * 2);
                    float rowAlpha = isSelected ? alpha : alpha * hoverT;
                    int rowColor = isSelected ? palette.selected() : palette.hover();
                    graphics.fill(cardX - pad, cardsY - pad, cardX + CARD_WIDTH + pad, cardsY + CARD_HEIGHT + pad,
                            withAlpha(rowColor, rowAlpha));
                    if (!isSelected && hoverT > 0.05f) {
                        graphics.outline(cardX - pad, cardsY - pad, CARD_WIDTH + pad * 2, CARD_HEIGHT + pad * 2,
                                withAlpha(palette.accent(), alpha * hoverT * 0.6f));
                    }
                }

                drawThemeCard(graphics, theme, cardX, cardsY, alpha, isSelected);
            }
        }
        transition.pop(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (transition.finishCloseIfReady() && this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void drawThemeCard(GuiGraphicsExtractor graphics, UiTheme theme, int x, int y, float alpha, boolean selected) {
        UiPalette cardPalette = theme.palette();

        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT - 18, withAlpha(cardPalette.paper(), alpha));
        graphics.fill(x, y, x + CARD_WIDTH, y + 14, withAlpha(cardPalette.header(), alpha));
        graphics.fill(x, y + 13, x + CARD_WIDTH, y + 14, withAlpha(cardPalette.line(), alpha));
        graphics.fill(x + 8, y + 22, x + CARD_WIDTH - 8, y + 30, withAlpha(cardPalette.accent(), alpha));
        graphics.fill(x + 8, y + 36, x + CARD_WIDTH - 40, y + 44, withAlpha(cardPalette.toggleOn(), alpha));
        graphics.fill(x + CARD_WIDTH - 36, y + 36, x + CARD_WIDTH - 8, y + 44, withAlpha(cardPalette.toggleOff(), alpha));
        graphics.fill(x + 8, y + 50, x + CARD_WIDTH - 8, y + 58, withAlpha(cardPalette.field(), alpha));

        if (selected) {
            graphics.outline(x, y, CARD_WIDTH, CARD_HEIGHT - 18, withAlpha(cardPalette.accent(), alpha));
        } else {
            graphics.outline(x, y, CARD_WIDTH, CARD_HEIGHT - 18, withAlpha(cardPalette.panelBorder(), alpha));
        }

        String name = theme.getDisplayName();
        int nameWidth = this.font.width(name);
        graphics.text(this.font, name, x + (CARD_WIDTH - nameWidth) / 2, y + CARD_HEIGHT - 12,
                withAlpha(cardPalette.text(), alpha), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        List<UiTheme> themes = UiTheme.values();
        int panelW = themes.size() * CARD_WIDTH + (themes.size() - 1) * CARD_GAP + PANEL_PADDING * 2;
        int panelH = CARD_HEIGHT + 72;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int cardsY = panelY + 52;
        int cardsX = panelX + PANEL_PADDING;

        for (int i = 0; i < themes.size(); i++) {
            UiTheme theme = themes.get(i);
            int cardX = cardsX + i * (CARD_WIDTH + CARD_GAP);
            if (isInside(mouseX, mouseY, cardX, cardsY, CARD_WIDTH, CARD_HEIGHT)) {
                UiSettings.INSTANCE.setTheme(theme);
                previewTheme = theme;
                if (parent instanceof ThemedConfigScreen configScreen) {
                    configScreen.setTheme(theme);
                    configScreen.notifySaved("Theme");
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (!transition.isClosing()) {
            transition.beginClose();
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private static int withAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0f, Math.min(1f, factor)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }
}