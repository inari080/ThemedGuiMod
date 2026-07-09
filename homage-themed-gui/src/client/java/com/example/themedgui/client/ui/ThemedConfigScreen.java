package com.example.themedgui.client.ui;

import com.example.themedgui.client.config.SettingNode;
import com.example.themedgui.client.config.SettingRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ThemedConfigScreen extends Screen {

	private static final int SIDEBAR_WIDTH = 90;
	private static final int ROW_HEIGHT = 22;
	private static final int HEADER_HEIGHT = 28;

	private final Screen parent;
	private final SettingRegistry registry;
	private final ScreenTransition transition = new ScreenTransition();
	private final SmoothScroll scroll = new SmoothScroll();

	private UiTheme theme = UiTheme.DARK;
	private int selectedCategory = 0;
	private Button themeButton;
	private int lastScrollY = 0;

	public ThemedConfigScreen(Screen parent, SettingRegistry registry) {
		super(Component.literal("Themed GUI Demo"));
		this.parent = parent;
		this.registry = registry;
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
		List<String> categories = registry.categories();
		List<SettingNode> rows = categories.isEmpty()
				? List.of() : registry.nodes(categories.get(selectedCategory));

		graphics.fill(0, 0, this.width, this.height, withAlpha(palette.backdrop(), transition.currentAlpha()));

		transition.push(graphics);
		{
			int panelX = 20;
			int panelY = 20;
			int panelW = this.width - 40;
			int panelH = this.height - 40;
			float alpha = transition.currentAlpha();

			graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, withAlpha(palette.panel(), alpha));
			graphics.outline(panelX, panelY, panelW, panelH, withAlpha(palette.panelBorder(), alpha));

			graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.sidebar(), alpha));
			graphics.text(this.font, this.title, panelX + 12, panelY + 10, withAlpha(palette.text(), alpha), false);

			int sidebarX = panelX;
			int sidebarY = panelY + HEADER_HEIGHT;
			int sidebarH = panelH - HEADER_HEIGHT;
			graphics.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.sidebar(), alpha));

			for (int i = 0; i < categories.size(); i++) {
				int rowY = sidebarY + 8 + i * ROW_HEIGHT;
				boolean isSelected = i == selectedCategory;
				boolean isHovered = !isSelected && isInside(mouseX, mouseY, sidebarX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT);
				int rowColor = isSelected ? palette.selected() : (isHovered ? palette.hover() : 0);
				if (rowColor != 0) {
					graphics.fill(sidebarX + 4, rowY, sidebarX + SIDEBAR_WIDTH - 4, rowY + ROW_HEIGHT - 2, withAlpha(rowColor, alpha));
				}
				int textColor = isSelected ? palette.text() : palette.mutedText();
				graphics.text(this.font, categories.get(i), sidebarX + 12, rowY + 6, withAlpha(textColor, alpha), false);
			}

			int contentX = panelX + SIDEBAR_WIDTH;
			int contentY = sidebarY;
			int contentW = panelW - SIDEBAR_WIDTH;
			int contentH = sidebarH;
			int maxScroll = Math.max(0, rows.size() * ROW_HEIGHT - contentH);

			graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
			int scrollY = scroll.tick(maxScroll);
			lastScrollY = scrollY;
			for (int i = 0; i < rows.size(); i++) {
				SettingNode node = rows.get(i);
				int rowY = contentY + 8 + i * ROW_HEIGHT - scrollY;
				if (rowY + ROW_HEIGHT < contentY || rowY > contentY + contentH) {
					continue;
				}
				boolean isHovered = isInside(mouseX, mouseY, contentX + 8, rowY, contentW - 16, ROW_HEIGHT - 4);
				if (isHovered) {
					graphics.fill(contentX + 8, rowY, contentX + contentW - 8, rowY + ROW_HEIGHT - 4, withAlpha(palette.hover(), alpha));
				}
				graphics.text(this.font, node.label(), contentX + 14, rowY + 6, withAlpha(palette.text(), alpha), false);

				if (node.kind() == SettingNode.Kind.TOGGLE) {
					boolean on = node.getBoolean();
					int pillX = contentX + contentW - 44;
					int pillColor = on ? palette.toggleOn() : palette.toggleOff();
					graphics.fill(pillX, rowY + 3, pillX + 28, rowY + 15, withAlpha(pillColor, alpha));
				} else if (node.kind() == SettingNode.Kind.SLIDER) {
					int barX = contentX + contentW - 84;
					int barW = 70;
					graphics.fill(barX, rowY + 8, barX + barW, rowY + 11, withAlpha(palette.toggleOff(), alpha));
					int fillW = (int) (barW * node.getRatio());
					graphics.fill(barX, rowY + 8, barX + fillW, rowY + 11, withAlpha(palette.toggleOn(), alpha));
				}
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
		List<String> categories = registry.categories();
		int rowCount = categories.isEmpty() ? 0 : registry.nodes(categories.get(selectedCategory)).size();
		int maxScroll = Math.max(0, rowCount * ROW_HEIGHT - (this.height - 40 - HEADER_HEIGHT));
		scroll.addDelta((int) (-verticalAmount * ROW_HEIGHT), maxScroll);
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int mouseX = (int) event.x();
		int mouseY = (int) event.y();
		List<String> categories = registry.categories();

		int panelX = 20;
		int sidebarY = 20 + HEADER_HEIGHT;
		for (int i = 0; i < categories.size(); i++) {
			int rowY = sidebarY + 8 + i * ROW_HEIGHT;
			if (isInside(mouseX, mouseY, panelX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT)) {
				selectedCategory = i;
				return true;
			}
		}

		if (!categories.isEmpty()) {
			List<SettingNode> rows = registry.nodes(categories.get(selectedCategory));
			int contentX = panelX + SIDEBAR_WIDTH;
			int contentY = sidebarY;
			int contentW = this.width - 40 - SIDEBAR_WIDTH;
			int scrollY = lastScrollY;

			for (int i = 0; i < rows.size(); i++) {
				int rowY = contentY + 8 + i * ROW_HEIGHT - scrollY;
				if (!isInside(mouseX, mouseY, contentX + 8, rowY, contentW - 16, ROW_HEIGHT - 4)) continue;

				SettingNode node = rows.get(i);
				if (node.kind() == SettingNode.Kind.TOGGLE) {
					node.toggle();
					registry.save();
					return true;
				}
				// SLIDER drag handling can be added in mouseDragged the same way,
				// using node.setFromRatio((mouseX - barX) / (double) barW).
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
		registry.save();
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
		int scaled = Math.round(a * clamp(factor, 0f, 1f));
		return (scaled << 24) | (argb & 0x00FFFFFF);
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}