package com.example.themedgui.client.ui;

import com.example.themedgui.client.config.SettingNode;
import com.example.themedgui.client.config.SettingRegistry;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ThemedConfigScreen extends Screen {

	private static final int SIDEBAR_WIDTH = 90;
	private static final int ROW_HEIGHT = 22;
	private static final int DESCRIPTION_LINE_HEIGHT = 10;
	private static final int HEADER_HEIGHT = 28;
	private static final int SLIDER_WIDTH = 70;
	private static final int TOOLTIP_PADDING = 4;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_GUTTER = 16; // right-side space reserved for the scrollbar
	private static final int SCROLLBAR_MIN_THUMB = 20;
	private static final int SEARCH_BOX_WIDTH = 150;
	private static final long CATEGORY_SLIDE_MS = 180;
	private static final int CATEGORY_SLIDE_DISTANCE = 22;

	private final Screen parent;
	private final SettingRegistry registry;
	private final ScreenTransition transition = new ScreenTransition();
	private final SmoothScroll scroll = new SmoothScroll();

	/** Hover glow amount, keyed by "cat:<index>" for sidebar rows or the SettingNode itself for content rows. */
	private final Anim hoverAnim = new Anim(16f);
	/** Eased toggle/slider visual position, keyed by SettingNode. */
	private final Anim controlAnim = new Anim(14f);
	private final Toast toasts = new Toast();

	private UiTheme theme = UiSettings.INSTANCE.getTheme();
	private int selectedCategory = 0;
	private long categorySwitchTime = System.currentTimeMillis();
	private int categorySlideDir = 1;

	private Button themeButton;
	private EditBox searchBox;
	private SettingNode draggingSlider = null;
	private boolean draggingScrollbar = false;
	private String hoveredTooltip = null;

	/** Precomputed once per frame/interaction so render and input handling never disagree on geometry. */
	private record Layout(int contentX, int contentY, int contentW, int contentH, List<SettingNode> rows, int maxScroll) {
		int rowRight() {
			return contentX + contentW - SCROLLBAR_GUTTER;
		}
	}

	private final ScreenBranding branding;

	public ThemedConfigScreen(Screen parent, SettingRegistry registry) {
		this(parent, registry, new ScreenBranding("Themed GUI Demo"));
	}

	public ThemedConfigScreen(Screen parent, SettingRegistry registry, ScreenBranding branding) {
		super(Component.literal(branding.title()));
		this.parent = parent;
		this.registry = registry;
		this.branding = branding;
	}

	@Override
	protected void init() {
		int panelRight = this.width - 20; // matches panelX(20) + panelW(width-40)
		int controlY = 20 + 4; // panelY(20) + 4px inset, keeps controls inside the HEADER_HEIGHT(28) stripe

		themeButton = Button.builder(themeButtonLabel(), btn -> {
			theme = theme.next();
			UiSettings.INSTANCE.setTheme(theme);
			btn.setMessage(themeButtonLabel());
			toasts.show("Theme saved");
		}).bounds(panelRight - 8 - 108, controlY, 108, 20).build();
		this.addRenderableWidget(themeButton);

		searchBox = new EditBox(this.font, panelRight - 8 - 108 - 8 - SEARCH_BOX_WIDTH, controlY, SEARCH_BOX_WIDTH, 20,
				Component.literal("Search"));
		searchBox.setSuggestion("Search settings...");
		searchBox.setResponder(value -> scroll.jumpTo(0));
		this.addRenderableWidget(searchBox);
	}

	private Component themeButtonLabel() {
		return Component.literal("Theme: " + theme.getDisplayName());
	}

	private boolean isSearching() {
		return searchBox != null && !searchBox.getValue().trim().isEmpty();
	}

	/** Called by ColorPickerScreen (and any future sub-screen) when it commits a change back to us. */
	public void notifySaved(String what) {
		toasts.show(what + " saved");
	}

	/** Called by UiSettingsScreen when the user picks a theme card. */
	public void setTheme(UiTheme theme) {
		this.theme = theme;
		if (themeButton != null) {
			themeButton.setMessage(themeButtonLabel());
		}
	}

	/** Rows to display: the selected category normally, or a flattened cross-category match list while searching. */
	private List<SettingNode> currentRows(List<String> categories) {
		if (!isSearching()) {
			return categories.isEmpty() ? List.of() : registry.nodes(categories.get(selectedCategory));
		}
		String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
		List<SettingNode> matches = new ArrayList<>();
		for (SettingNode node : registry.allNodes()) {
			if (node.label().toLowerCase(Locale.ROOT).contains(needle)
					|| node.category().toLowerCase(Locale.ROOT).contains(needle)) {
				matches.add(node);
			}
		}
		return matches;
	}

	private Layout layout() {
		List<String> categories = registry.categories();
		List<SettingNode> rows = currentRows(categories);
		int panelX = 20;
		int panelY = 20;
		int panelW = this.width - 40;
		int panelH = this.height - 40;
		int sidebarY = panelY + HEADER_HEIGHT;
		int sidebarH = panelH - HEADER_HEIGHT;
		int contentX = panelX + SIDEBAR_WIDTH;
		int contentY = sidebarY;
		int contentW = panelW - SIDEBAR_WIDTH;
		int contentH = sidebarH;
		int maxScroll = Math.max(0, totalRowsHeight(rows) - contentH);
		return new Layout(contentX, contentY, contentW, contentH, rows, maxScroll);
	}

	/** Returns {thumbY, thumbH}, or null if the list fits and no scrollbar should be drawn. */
	private int[] scrollbarThumb(Layout layout, int scrollY) {
		if (layout.maxScroll() <= 0) return null;
		int totalHeight = layout.contentH() + layout.maxScroll();
		int thumbH = Math.max(SCROLLBAR_MIN_THUMB, layout.contentH() * layout.contentH() / totalHeight);
		int available = layout.contentH() - thumbH;
		int thumbY = layout.contentY() + (available > 0 ? Math.round((float) scrollY * available / layout.maxScroll()) : 0);
		return new int[]{thumbY, thumbH};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		UiPalette palette = theme.palette();
		List<String> categories = registry.categories();
		Layout layout = layout();
		boolean searching = isSearching();

		hoveredTooltip = null;

		float transitionAlpha = transition.currentAlpha();
		graphics.fill(0, 0, this.width, this.height, withAlpha(palette.backdrop(), transitionAlpha));
		// Drifting ambient light, unaffected by the panel's own slide so it feels like it's behind everything.
		AmbientBackdrop.drawBlobs(graphics, this.width, this.height, palette.accent(), transitionAlpha);

		int panelX = 20;
		int panelY = 20;
		int panelW = this.width - 40;
		int panelH = this.height - 40;

		// Category-switch slide: content eases in from the direction we navigated, sidebar stays put.
		float catProgress = 1f;
		if (!searching) {
			float raw = clamp01((System.currentTimeMillis() - categorySwitchTime) / (float) CATEGORY_SLIDE_MS);
			catProgress = easeOutCubic(raw);
		}
		int slideOffsetX = searching ? 0 : Math.round((1f - catProgress) * categorySlideDir * CATEGORY_SLIDE_DISTANCE);
		float catAlpha = searching ? 1f : catProgress;

		transition.push(graphics);
		{
			float alpha = transitionAlpha;

			graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, withAlpha(palette.panel(), alpha));
			graphics.outline(panelX, panelY, panelW, panelH, withAlpha(palette.panelBorder(), alpha));
			AmbientBackdrop.drawPanelGlow(graphics, panelX, panelY, panelW, panelH, palette.accent(), alpha);

			// Header bar
			graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.header(), alpha));
			int titleX = panelX + 12;
			if (branding.logo() != null) {
				int logoSize = 16;
				int logoY = panelY + (HEADER_HEIGHT - logoSize) / 2;
				graphics.blit(RenderPipelines.GUI_TEXTURED, branding.logo(), titleX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
				titleX += logoSize + 6;
			}
			graphics.text(this.font, this.title, titleX, panelY + 10, withAlpha(palette.text(), alpha), false);
			graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelW, panelY + HEADER_HEIGHT, withAlpha(palette.line(), alpha));

			int sidebarX = panelX;
			int sidebarY = panelY + HEADER_HEIGHT;
			int sidebarH = panelH - HEADER_HEIGHT;
			graphics.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.sidebar(), alpha));
			// Divider between sidebar and content
			graphics.fill(sidebarX + SIDEBAR_WIDTH - 1, sidebarY, sidebarX + SIDEBAR_WIDTH, sidebarY + sidebarH, withAlpha(palette.line(), alpha));

			for (int i = 0; i < categories.size(); i++) {
				int rowY = sidebarY + 8 + i * ROW_HEIGHT;
				boolean isSelected = !searching && i == selectedCategory;
				boolean isHovered = !isSelected && isInside(mouseX, mouseY, sidebarX, rowY, SIDEBAR_WIDTH, ROW_HEIGHT);

				String hoverKey = "cat:" + i;
				hoverAnim.setTarget(hoverKey, isHovered ? 1f : 0f);
				float hoverT = hoverAnim.tick(hoverKey);

				if (isSelected || hoverT > 0.01f) {
					int pad = Math.round(hoverT * 2);
					float rowAlpha = isSelected ? alpha : alpha * hoverT;
					int rowColor = isSelected ? palette.selected() : palette.hover();
					graphics.fill(sidebarX + 4 - pad, rowY - pad, sidebarX + SIDEBAR_WIDTH - 4 + pad, rowY + ROW_HEIGHT - 2 + pad, withAlpha(rowColor, rowAlpha));
					if (!isSelected && hoverT > 0.05f) {
						graphics.outline(sidebarX + 4 - pad, rowY - pad, SIDEBAR_WIDTH - 8 + pad * 2, ROW_HEIGHT - 2 + pad * 2, withAlpha(palette.accent(), alpha * hoverT * 0.6f));
					}
				}
				int textColor = isSelected ? palette.text() : palette.mutedText();
				int labelX = sidebarX + 12;
				Identifier catIcon = registry.categoryIcon(categories.get(i));
				if (catIcon != null) {
					int iconSize = 12;
					graphics.blit(RenderPipelines.GUI_TEXTURED, catIcon, labelX, rowY + 5, 0, 0, iconSize, iconSize, iconSize, iconSize);
					labelX += iconSize + 4;
				}
				graphics.text(this.font, categories.get(i), labelX, rowY + 6, withAlpha(textColor, alpha), false);
			}

			int contentX = layout.contentX();
			int contentY = layout.contentY();
			int contentW = layout.contentW();
			int contentH = layout.contentH();
			int rowRight = layout.rowRight();
			List<SettingNode> rows = layout.rows();

			// Paper backdrop for the scrollable content area, distinct from the panel itself
			graphics.fill(contentX, contentY, contentX + contentW, contentY + contentH, withAlpha(palette.paper(), alpha));

			if (searching && rows.isEmpty()) {
				String message = "No settings found";
				graphics.text(this.font, message, contentX + 14, contentY + 10, withAlpha(palette.mutedText(), alpha), false);
			}

			graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
			int scrollY = scroll.tick(layout.maxScroll());
			for (int i = 0; i < rows.size(); i++) {
				SettingNode node = rows.get(i);
				int thisRowH = rowHeight(node);
				int baseRowY = contentY + 8 + rowOffset(rows, i) - scrollY;
				if (baseRowY + thisRowH < contentY || baseRowY > contentY + contentH) {
					continue;
				}
				int rowY = baseRowY;
				int rowX = slideOffsetX;
				float rowAlphaMul = alpha * catAlpha;

				// Hit-testing uses the un-slid position so clicks feel right even mid-animation.
				boolean isHovered = isInside(mouseX, mouseY, contentX + 8, rowY, rowRight - contentX - 8, thisRowH - 4);
				hoverAnim.setTarget(node, isHovered ? 1f : 0f);
				float hoverT = hoverAnim.tick(node);

				if (hoverT > 0.01f) {
					int pad = Math.round(hoverT * 2);
					graphics.fill(contentX + 8 - pad + rowX, rowY - pad, rowRight + pad + rowX, rowY + thisRowH - 4 + pad, withAlpha(palette.hover(), rowAlphaMul * hoverT));
					if (hoverT > 0.1f) {
						graphics.outline(contentX + 8 - pad + rowX, rowY - pad, rowRight - contentX - 8 + pad * 2, thisRowH - 4 + pad * 2, withAlpha(palette.accent(), rowAlphaMul * hoverT * 0.5f));
					}
				}
				if (isHovered && !node.tooltip().isEmpty()) {
					hoveredTooltip = node.tooltip();
				}

				String label = searching ? "[" + node.category() + "] " + node.label() : node.label();
				int labelX = contentX + 14 + rowX;
				if (node.icon() != null) {
					int iconSize = 12;
					graphics.blit(RenderPipelines.GUI_TEXTURED, node.icon(), labelX, rowY + 5, 0, 0, iconSize, iconSize, iconSize, iconSize);
					labelX += iconSize + 4;
				}
				graphics.text(this.font, label, labelX, rowY + 6, withAlpha(palette.text(), rowAlphaMul), false);
				if (!node.tooltip().isEmpty()) {
					graphics.text(this.font, node.tooltip(), contentX + 14 + rowX, rowY + 6 + this.font.lineHeight,
							withAlpha(palette.mutedText(), rowAlphaMul), false);
				}

				if (node.kind() == SettingNode.Kind.TOGGLE) {
					boolean on = node.getBoolean();
					controlAnim.setTarget(node, on ? 1f : 0f);
					float animT = controlAnim.tick(node);

					int trackX = rowRight - 34 + rowX;
					int trackY = rowY + 3;
					int trackW = 28;
					int trackH = 12;
					int trackColor = lerpArgb(palette.toggleOff(), palette.toggleOn(), animT);
					graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, withAlpha(trackColor, rowAlphaMul));
					graphics.outline(trackX, trackY, trackW, trackH, withAlpha(palette.line(), rowAlphaMul));

					int knobSize = trackH - 4;
					int knobTravel = trackW - knobSize - 4;
					int knobX = trackX + 2 + Math.round(animT * knobTravel);
					graphics.fill(knobX, trackY + 2, knobX + knobSize, trackY + trackH - 2, withAlpha(0xFFFFFFFF, rowAlphaMul));
				} else if (node.kind() == SettingNode.Kind.SLIDER) {
					boolean isDraggingThis = node == draggingSlider;
					controlAnim.setTarget(node, (float) node.getRatio());
					float ticked = controlAnim.tick(node);
					float animRatio = isDraggingThis ? (float) node.getRatio() : ticked;

					int barX = rowRight - SLIDER_WIDTH - 8 + rowX;
					// Track sits on a field chip so it reads as a distinct control surface
					graphics.fill(barX - 3, rowY + 5, barX + SLIDER_WIDTH + 3, rowY + 14, withAlpha(palette.field(), rowAlphaMul));
					int fillW = Math.round(SLIDER_WIDTH * animRatio);
					graphics.fill(barX, rowY + 8, barX + fillW, rowY + 11, withAlpha(palette.toggleOn(), rowAlphaMul));
					int knobX = barX + fillW;
					graphics.fill(knobX - 1, rowY + 4, knobX + 2, rowY + 15, withAlpha(0xFFFFFFFF, rowAlphaMul));
					// value shown just left of the track, on field text color
					String valueLabel = formatSliderValue(node);
					int valueWidth = this.font.width(valueLabel);
					graphics.text(this.font, valueLabel, barX - 8 - valueWidth, rowY + 6, withAlpha(palette.fieldText(), rowAlphaMul), false);
				} else if (node.kind() == SettingNode.Kind.ENUM) {
					String value = node.enumValueLabel();
					int textWidth = this.font.width(value);
					graphics.text(this.font, value, rowRight - 4 - textWidth + rowX, rowY + 6,
							withAlpha(palette.mutedText(), rowAlphaMul), false);
				} else if (node.kind() == SettingNode.Kind.ACTION) {
					String actionLabel = "Open";
					int chipW = this.font.width(actionLabel) + 12;
					int chipX = rowRight - chipW + rowX;
					graphics.fill(chipX, rowY + 2, rowRight + rowX, rowY + ROW_HEIGHT - 6, withAlpha(palette.accentSoft(), rowAlphaMul));
					graphics.outline(chipX, rowY + 2, chipW, ROW_HEIGHT - 8, withAlpha(palette.accent(), rowAlphaMul));
					graphics.text(this.font, actionLabel, chipX + 6, rowY + 6, withAlpha(palette.text(), rowAlphaMul), false);
				} else if (node.kind() == SettingNode.Kind.COLOR) {
					int swatchW = 28;
					int swatchX = rowRight - swatchW + rowX;
					graphics.fill(swatchX, rowY + 3, rowRight + rowX, rowY + 15, withAlpha(0xFF000000 | node.getColor(), rowAlphaMul));
					graphics.outline(swatchX, rowY + 3, swatchW, 12, withAlpha(palette.line(), rowAlphaMul));
				}

				// Subtle row separator
				if (i < rows.size() - 1) {
					graphics.fill(contentX + 8 + rowX, rowY + thisRowH - 4, rowRight + rowX, rowY + thisRowH - 3, withAlpha(palette.mutedLine(), rowAlphaMul));
				}
			}
			graphics.disableScissor();

			int[] thumb = scrollbarThumb(layout, scrollY);
			if (thumb != null) {
				int trackX = contentX + contentW - SCROLLBAR_WIDTH - 4;
				graphics.fill(trackX, contentY, trackX + SCROLLBAR_WIDTH, contentY + contentH, withAlpha(palette.field(), alpha));
				graphics.fill(trackX, thumb[0], trackX + SCROLLBAR_WIDTH, thumb[0] + thumb[1], withAlpha(palette.accent(), alpha));
			}
		}
		transition.pop(graphics);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		if (hoveredTooltip != null) {
			drawTooltip(graphics, palette, hoveredTooltip, mouseX, mouseY);
		}
		// Toasts are pinned to the panel corner but drawn outside the push/pop block, so they don't slide with it.
		toasts.draw(graphics, this.font, palette, panelX + panelW - 8, panelY + panelH - 8);

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
		Layout layout = layout();
		scroll.addDelta((int) (-verticalAmount * ROW_HEIGHT), layout.maxScroll());
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
				if (i != selectedCategory) {
					categorySlideDir = i > selectedCategory ? 1 : -1;
					categorySwitchTime = System.currentTimeMillis();
				}
				selectedCategory = i;
				if (isSearching()) {
					searchBox.setValue("");
				}
				scroll.jumpTo(0);
				return true;
			}
		}

		Layout layout = layout();
		int scrollY = scroll.getValue();

		int[] thumb = scrollbarThumb(layout, scrollY);
		if (thumb != null) {
			int trackX = layout.contentX() + layout.contentW() - SCROLLBAR_WIDTH - 4;
			if (isInside(mouseX, mouseY, trackX - 2, layout.contentY(), SCROLLBAR_WIDTH + 4, layout.contentH())) {
				draggingScrollbar = true;
				dragScrollbarTo(layout, thumb[1], mouseY);
				return true;
			}
		}

		List<SettingNode> rows = layout.rows();
		int contentX = layout.contentX();
		int contentY = layout.contentY();
		int rowRight = layout.rowRight();

		for (int i = 0; i < rows.size(); i++) {
			int rowY = contentY + 8 + rowOffset(rows, i) - scrollY;
			int thisRowH = rowHeight(rows.get(i));
			if (!isInside(mouseX, mouseY, contentX + 8, rowY, rowRight - contentX - 8, thisRowH - 4)) continue;

			SettingNode node = rows.get(i);
			if (node.kind() == SettingNode.Kind.TOGGLE) {
				node.toggle();
				registry.save();
				toasts.show(node.label() + " saved");
				return true;
			} else if (node.kind() == SettingNode.Kind.SLIDER) {
				int barX = rowRight - SLIDER_WIDTH - 8;
				draggingSlider = node;
				node.setFromRatio((mouseX - barX) / (double) SLIDER_WIDTH);
				return true;
			} else if (node.kind() == SettingNode.Kind.ENUM) {
				node.cycleEnum(1);
				registry.save();
				toasts.show(node.label() + " saved");
				return true;
			} else if (node.kind() == SettingNode.Kind.ACTION) {
				node.runAction();
				return true;
			} else if (node.kind() == SettingNode.Kind.COLOR) {
				if (this.minecraft != null) {
					this.minecraft.setScreen(new ColorPickerScreen(this, theme, node.getColor(), picked -> {
						node.setColor(picked);
						registry.save();
					}));
				}
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void dragScrollbarTo(Layout layout, int thumbH, int mouseY) {
		int available = layout.contentH() - thumbH;
		if (available <= 0 || layout.maxScroll() <= 0) return;
		double ratio = (mouseY - thumbH / 2.0 - layout.contentY()) / available;
		ratio = Math.max(0, Math.min(1, ratio));
		scroll.jumpTo((int) Math.round(ratio * layout.maxScroll()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
		if (draggingScrollbar) {
			Layout layout = layout();
			int[] thumb = scrollbarThumb(layout, scroll.getValue());
			int thumbH = thumb != null ? thumb[1] : SCROLLBAR_MIN_THUMB;
			dragScrollbarTo(layout, thumbH, (int) event.y());
			return true;
		}
		if (draggingSlider != null) {
			Layout layout = layout();
			int barX = layout.rowRight() - SLIDER_WIDTH - 8;
			draggingSlider.setFromRatio((event.x() - barX) / (double) SLIDER_WIDTH);
			return true;
		}
		return super.mouseDragged(event, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		if (draggingSlider != null) {
			toasts.show(draggingSlider.label() + " saved");
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

	/** Rows with a description get extra height for the always-visible caption line under the label. */
	private static int rowHeight(SettingNode node) {
		return node.tooltip().isEmpty() ? ROW_HEIGHT : ROW_HEIGHT + DESCRIPTION_LINE_HEIGHT;
	}

	/** Sum of row heights before index `upToExclusive`. Row counts are small so an O(n) pass per call is fine. */
	private static int rowOffset(List<SettingNode> rows, int upToExclusive) {
		int sum = 0;
		for (int i = 0; i < upToExclusive; i++) {
			sum += rowHeight(rows.get(i));
		}
		return sum;
	}

	private static int totalRowsHeight(List<SettingNode> rows) {
		return rowOffset(rows, rows.size());
	}

	private static int withAlpha(int argb, float factor) {
		int a = (argb >>> 24) & 0xFF;
		int scaled = Math.round(a * clamp(factor, 0f, 1f));
		return (scaled << 24) | (argb & 0x00FFFFFF);
	}

	/** Per-channel linear blend between two ARGB colors (alpha channel taken from `to`). */
	private static int lerpArgb(int from, int to, float t) {
		float clamped = clamp(t, 0f, 1f);
		int a = (to >>> 24) & 0xFF;
		int r = Math.round(lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, clamped));
		int g = Math.round(lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, clamped));
		int b = Math.round(lerp(from & 0xFF, to & 0xFF, clamped));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static float lerp(float from, float to, float t) {
		return from + (to - from) * t;
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static float easeOutCubic(float t) {
		float inv = 1f - t;
		return 1f - inv * inv * inv;
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}