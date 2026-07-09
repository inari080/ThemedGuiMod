package com.example.themedgui.client.ui;

/**
 * Turns a stepped scroll input (mouse wheel notches) into a smoothly
 * animated value, using exponential-decay interpolation rather than a
 * fixed-speed lerp. That means the motion naturally slows down as it
 * approaches the target instead of moving at a constant speed and then
 * stopping abruptly.
 *
 * Usage:
 *   scroll.addDelta((int) (verticalAmount * 16), maxScroll); // in mouseScrolled
 *   int y = scroll.tick(maxScroll);                          // in render/extractRenderState
 */
public class SmoothScroll {

	/** How quickly current chases target. Higher = snappier, lower = floatier. */
	private static final float SPEED = 12.0f;

	private int target = 0;
	private float current = 0f;
	private long lastTimeMs = System.currentTimeMillis();

	public int getTarget() {
		return target;
	}

	public int getValue() {
		return Math.round(current);
	}

	public void jumpTo(int position) {
		this.target = position;
		this.current = position;
	}

	public void addDelta(int delta, int maxScroll) {
		this.target = clamp(target + delta, 0, maxScroll);
	}

	public void clampTo(int maxScroll) {
		this.target = clamp(target, 0, maxScroll);
		this.current = clamp(current, 0f, (float) maxScroll);
	}

	/** Call once per frame; returns the current (interpolated) scroll offset. */
	public int tick(int maxScroll) {
		target = clamp(target, 0, maxScroll);

		long now = System.currentTimeMillis();
		// Clamp the frame delta so a lag spike / tab-out doesn't cause a huge jump.
		float dt = clamp(now - lastTimeMs, 1L, 50L) / 1000f;
		lastTimeMs = now;

		float diff = target - current;
		if (Math.abs(diff) < 0.5f) {
			current = target;
		} else {
			current += diff * (1f - (float) Math.exp(-SPEED * dt));
		}
		return Math.round(current);
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	private static long clamp(long v, long min, long max) {
		return Math.max(min, Math.min(max, v));
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
