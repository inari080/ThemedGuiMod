package com.example.themedgui.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Small helper that gives a screen a "slide up + fade in" open animation and
 * a quicker "slide down + fade out" close animation, using an ease-out-cubic
 * curve so the motion decelerates naturally instead of stopping abruptly.
 *
 * Typical usage inside a Screen:
 *
 *   private final ScreenTransition transition = new ScreenTransition();
 *
 *   protected void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
 *       transition.push(graphics);
 *       // ... draw your panel, applying transition.currentAlpha() to colors ...
 *       transition.pop(graphics);
 *       if (transition.finishCloseIfReady()) {
 *           this.minecraft.setScreen(null);
 *       }
 *   }
 *
 *   public void onClose() {
 *       if (!transition.isClosing()) {
 *           transition.beginClose();
 *           return; // swallow the first close request so the animation can play
 *       }
 *       super.onClose();
 *   }
 */
public class ScreenTransition {

	private final long openDurationMs;
	private final long closeDurationMs;
	private final float offsetPixels;

	private final long openedAt = System.currentTimeMillis();
	private long closeStartedAt = -1L;
	private boolean closing = false;

	public ScreenTransition() {
		this(180L, 80L, 18f);
	}

	public ScreenTransition(long openDurationMs, long closeDurationMs, float offsetPixels) {
		this.openDurationMs = openDurationMs;
		this.closeDurationMs = closeDurationMs;
		this.offsetPixels = offsetPixels;
	}

	public boolean isClosing() {
		return closing;
	}

	public void beginClose() {
		if (!closing) {
			closing = true;
			closeStartedAt = System.currentTimeMillis();
		}
	}

	/** Returns true exactly once, when the close animation has finished playing. */
	public boolean finishCloseIfReady() {
		if (!closing) {
			return false;
		}
		return System.currentTimeMillis() - closeStartedAt >= closeDurationMs;
	}

	public void push(GuiGraphicsExtractor graphics) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(0f, currentOffset());
	}

	public void pop(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
	}

	public float currentAlpha() {
		return easeOutCubic(progress());
	}

	private float currentOffset() {
		return (1f - easeOutCubic(progress())) * offsetPixels;
	}

	private float progress() {
		long elapsed = closing
				? System.currentTimeMillis() - closeStartedAt
				: System.currentTimeMillis() - openedAt;
		long duration = closing ? closeDurationMs : openDurationMs;
		float raw = clamp((float) elapsed / duration, 0f, 1f);
		return closing ? 1f - raw : raw;
	}

	private static float easeOutCubic(float t) {
		float inv = 1f - t;
		return 1f - inv * inv * inv;
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
