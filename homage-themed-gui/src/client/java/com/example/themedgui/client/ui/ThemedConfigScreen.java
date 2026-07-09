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
	private static final int SLIDER_WIDTH = 70;
	private static final int SLIDER_RIGHT_MARGIN = 84;
	private static final int TOOLTIP_PADDING = 4;

	private final Screen parent;
	private final SettingRegistry registry;
	private final ScreenTransition transition = new ScreenTransition();
	private final SmoothScroll scroll = new SmoothScroll();

	private UiTheme theme = UiTheme.DARK;
	private int selectedCategory = 0;
	private Button themeButton;
	private int lastScrollY = 0;
	private SettingNode draggingSlider = null;
	private String hoveredTooltip = null;

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

		hoveredTooltip = null;

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

			// Header bar
			graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.header(), alpha));
			graphics.text(this.font, this.title, panelX + 12, panelY + 10, withAlpha(palette.text(), alpha), false);
			graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.line(), alpha));

			int sidebarX = panelX;
			int sidebarY = panelY + HEADER_HEIGHT;
			int sidebarH = panelH - HEADER_HEIGHT;
			graphics.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.sidebar(), alpha));
			// Divider between sidebar and content
			graphics.fill(sidebarX + SIDEBAR_WIDTH - 1, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.line(), alpha));

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

			// Paper backdrop for the scrollable content area, distinct from the panel itself
			graphics.fill(contentX, contentY, contentX + contentW, contentY + contentH, withAlpha(palette.paper(), alpha));

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
					if (!node.tooltip().isEmpty()) {
						hoveredTooltip = node.tooltip();
					}
				}
				graphics.text(this.font, node.label(), contentX + 14, rowY + 6, withAlpha(palette.text(), alpha), false);

				if (node.kind() == SettingNode.Kind.TOGGLE) {
					boolean on = node.getBoolean();
					int pillX = contentX + contentW - 44;
					int pillColor = on ? palette.toggleOn() : palette.toggleOff();
					graphics.fill(pillX, rowY + 3, pillX + 28, rowY + 15, withAlpha(pillColor, alpha));
				} else if (node.kind() == SettingNode.Kind.SLIDER) {
					int barX = contentX + contentW - SLIDER_RIGHT_MARGIN;
					// Track sits on a field chip so it reads as a distinct control surface
					graphics.fill(barX - 3, rowY + 5, barX + SLIDER_WIDTH + 3, rowY + 14, withAlpha(palette.field(), alpha));
					graphics.fill(barX, rowY + 8, barX + SLIDER_WIDTH, rowY + 11, withAlpha(palette.accentSoft(), alpha));
					int fillW = (int) (SLIDER_WIDTH * node.getRatio());
					graphics.fill(barX, rowY + 8, barX + fillW, rowY + 11, withAlpha(palette.toggleOn(), alpha));
					// value shown just left of the track, on field text color
					String valueLabel = formatSliderValue(node);
					int valueWidth = this.font.width(valueLabel);
					graphics.text(this.font, valueLabel, barX - 8 - valueWidth, rowY + 6, withAlpha(palette.fieldText(), alpha), false);
				} else if (node.kind() == SettingNode.Kind.ENUM) {
					String value = node.enumValueLabel();
					int textWidth = this.font.width(value);
					graphics.text(this.font, value, contentX + contentW - 12 - textWidth, rowY + 6,
							withAlpha(palette.mutedText(), alpha), false);
				}

				// Subtle row separator
				if (i < rows.size() - 1) {
					graphics.fill(contentX + 8, rowY + ROW_HEIGHT - 4, contentX + contentW - 8, rowY + ROW_HEIGHT - 3, withAlpha(palette.mutedLine(), alpha));
				}
			}
			graphics.disableScissor();
		}
		transition.pop(graphics);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (hoveredTooltip != null) {
			drawTooltip(graphics, palette, hoveredTooltip, mouseX, mouseY);
		}

		if (transition.finishCloseIfReady() && this.minecraft != null) {
			this.minecraft.setScreen(parent);
		}
	}

	private void drawTooltip(GuiGraphicsExtractor graphics, UiPalette palette, String text, int mouseX, int mouseY) {
		int textWidth = this.font.width(text);
		int boxW = textWidth + TOOLTIP_PADDING * 2;
		int boxH = this.font.lineHeight + TOOLTIP_PADDING * 2;
		int x = mouseX + 12;
		int y = mouseY - boxH - 6;
		if (x + boxW > this.width) x = this.width - boxW - 2;
		if (y < 0) y = mouseY + 16;

		graphics.fill(x, y, x + boxW, y + boxH, palette.tooltipBack());
		graphics.outline(x, y, boxW, boxH, palette.line());
		graphics.text(this.font, text, x + TOOLTIP_PADDING, y + TOOLTIP_PADDING, palette.tooltipText(), false);
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
				scroll.jumpTo(0);
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
				} else if (node.kind() == SettingNode.Kind.SLIDER) {
					int barX = contentX + contentW - SLIDER_RIGHT_MARGIN;
					draggingSlider = node;
					node.setFromRatio((mouseX - barX) / (double) SLIDER_WIDTH);
					return true;
				} else if (node.kind() == SettingNode.Kind.ENUM) {
					node.cycleEnum(1);
					registry.save();
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
		if (draggingSlider != null) {
			int panelX = 20;
			int contentX = panelX + SIDEBAR_WIDTH;
			int contentW = this.width - 40 - SIDEBAR_WIDTH;
			int barX = contentX + contentW - SLIDER_RIGHT_MARGIN;
			draggingSlider.setFromRatio((event.x() - barX) / (double) SLIDER_WIDTH);
			return true;
		}
		return super.mouseDragged(event, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingSlider != null) {
			draggingSlider = null;
			registry.save();
			return true;
		}
		return super.mouseReleased(event);
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

	/** Ints show as whole numbers; floats show one decimal place (e.g. HUD scale 0.5-2.0). */
	private static String formatSliderValue(SettingNode node) {
		double value = node.getNumber();
		boolean wholeRange = node.min() == Math.floor(node.min()) && node.max() == Math.floor(node.max()) && node.max() - node.min() > 2;
		return wholeRange ? String.valueOf(Math.round(value)) : String.format("%.1f", value);
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