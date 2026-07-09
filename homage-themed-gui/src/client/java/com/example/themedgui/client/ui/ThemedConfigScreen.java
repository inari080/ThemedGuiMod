package com.example.themedgui.client.ui;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A from-scratch example config screen demonstrating three techniques as an
 * homage to a slicker-than-usual Minecraft mod config GUI:
 *
 *   1. A theme system (see {@link UiTheme} / {@link UiPalette}) - swapping the
 *      active palette instantly re-colors every element on screen.
 *   2. Inertia-based scrolling (see {@link SmoothScroll}) for the settings list.
 *   3. An eased open/close animation (see {@link ScreenTransition}).
 *
 * This intentionally has NO real settings wired up yet - the rows are just
 * labels so you can see the layout and animation. Swap `ROW_LABELS` for your
 * own data model (and add click handling in mouseClicked) once you're ready.
 *
 * MAPPINGS NOTE: written against Minecraft 26.1.2's official Mojang mappings
 * (unobfuscated). If a method below doesn't resolve for you, check:
 *  - https://docs.fabricmc.net/develop/rendering/gui/custom-screens
 *  - https://docs.fabricmc.net/develop/rendering/gui-graphics
 * as a few GUI method names shift between Minecraft versions.
 */
public class ThemedConfigScreen extends Screen {

	private static final List<String> CATEGORIES = List.of("General", "Appearance", "Overlays");
	private static final List<String> ROW_LABELS = List.of(
			"Example toggle A", "Example toggle B", "Example slider C",
			"Example toggle D", "Example toggle E", "Example slider F",
			"Example toggle G", "Example toggle H"
	);

	private static final int SIDEBAR_WIDTH = 90;
	private static final int ROW_HEIGHT = 22;
	private static final int HEADER_HEIGHT = 28;

	private final Screen parent;
	private final ScreenTransition transition = new ScreenTransition();
	private final SmoothScroll scroll = new SmoothScroll();

	private UiTheme theme = UiTheme.DARK;
	private int selectedCategory = 0;
	private Button themeButton;

	public ThemedConfigScreen(Screen parent) {
		super(Component.literal("Themed GUI Demo"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		themeButton = Button.builder(themeButtonLabel(), btn -> {
			theme = theme.next();
			btn.setMessage(themeButtonLabel());
		}).bounds(this.width - 116, 8, 108, 20).build();
		this.addRenderableWidget(themeButton);
	}

	private Component themeButtonLabel() {
		return Component.literal("Theme: " + theme.getDisplayName());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		UiPalette palette = UiPalette.forTheme(theme);

		// Dim the game behind the panel.
		graphics.fill(0, 0, this.width, this.height, withAlpha(palette.backdrop(), transition.currentAlpha()));

		transition.push(graphics);
		{
			int panelX = 20;
			int panelY = 20;
			int panelW = this.width - 40;
			int panelH = this.height - 40;
			float alpha = transition.currentAlpha();

			// Panel background + border.
			graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, withAlpha(palette.panel(), alpha));
			graphics.outline(panelX, panelY, panelW, panelH, withAlpha(palette.panelBorder(), alpha));

			// Header strip + title.
			graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.sidebar(), alpha));
			graphics.text(this.font, this.title, panelX + 12, panelY + 10, withAlpha(palette.text(), alpha), false);

			// Sidebar categories.
			int sidebarX = panelX;
			int sidebarY = panelY + HEADER_HEIGHT;
			int sidebarH = panelH - HEADER_HEIGHT;
			graphics.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.sidebar(), alpha));

			for (int i = 0; i < CATEGORIES.size(); i++) {
				int rowY = sidebarY + 8 + i * ROW_HEIGHT;
				boolean isSelected = i == selectedCategory;
				boolean isHovered = !isSelected && isInside(mouseX, mouseY, sidebarX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT);
				int rowColor = isSelected ? palette.selected() : (isHovered ? palette.hover() : 0);
				if (rowColor != 0) {
					graphics.fill(sidebarX + 4, rowY, sidebarX + SIDEBAR_WIDTH - 4, rowY + ROW_HEIGHT - 2, withAlpha(rowColor, alpha));
				}
				int textColor = isSelected ? palette.text() : palette.mutedText();
				graphics.text(this.font, CATEGORIES.get(i), sidebarX + 12, rowY + 6, withAlpha(textColor, alpha), false);
			}

			// Content area (scrollable list of placeholder rows).
			int contentX = panelX + SIDEBAR_WIDTH;
			int contentY = sidebarY;
			int contentW = panelW - SIDEBAR_WIDTH;
			int contentH = sidebarH;
			int maxScroll = Math.max(0, ROW_LABELS.size() * ROW_HEIGHT - contentH);

			graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
			int scrollY = scroll.tick(maxScroll);
			for (int i = 0; i < ROW_LABELS.size(); i++) {
				int rowY = contentY + 8 + i * ROW_HEIGHT - scrollY;
				if (rowY + ROW_HEIGHT < contentY || rowY > contentY + contentH) {
					continue; // cheap culling, no point drawing off-screen rows
				}
				boolean isHovered = isInside(mouseX, mouseY, contentX + 8, rowY, contentW - 16, ROW_HEIGHT - 4);
				if (isHovered) {
					graphics.fill(contentX + 8, rowY, contentX + contentW - 8, rowY + ROW_HEIGHT - 4, withAlpha(palette.hover(), alpha));
				}
				graphics.text(this.font, ROW_LABELS.get(i), contentX + 14, rowY + 6, withAlpha(palette.text(), alpha), false);

				// A little toggle pill on the right, just for visual flavor.
				boolean on = i % 2 == 0;
				int pillX = contentX + contentW - 44;
				int pillColor = on ? palette.toggleOn() : palette.toggleOff();
				graphics.fill(pillX, rowY + 3, pillX + 28, rowY + 15, withAlpha(pillColor, alpha));
			}
			graphics.disableScissor();
		}
		transition.pop(graphics);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (transition.finishCloseIfReady() && this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		int maxScroll = Math.max(0, ROW_LABELS.size() * ROW_HEIGHT - (this.height - 40 - HEADER_HEIGHT));
		scroll.addDelta((int) (-verticalAmount * ROW_HEIGHT), maxScroll);
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mouseX = (int) event.x();
		int mouseY = (int) event.y();
		int panelX = 20;
		int sidebarY = 20 + HEADER_HEIGHT;
		for (int i = 0; i < CATEGORIES.size(); i++) {
			int rowY = sidebarY + 8 + i * ROW_HEIGHT;
			if (isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT)) {
				selectedCategory = i;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClose() {
		if (!transition.isClosing()) {
			// Swallow the first ESC/close press so the fade-out can play;
			// extractRenderState() will call setScreen(parent) once it's done.
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

	/** Multiplies an ARGB color's alpha channel by `factor` (0..1). */
	private static int withAlpha(int argb, float factor) {
		int a = (argb >>> 24) & 0xFF;
		int scaled = Math.round(a * clamp(factor, 0f, 1f));
		return (scaled << 24) | (argb & 0x00FFFFFF);
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
