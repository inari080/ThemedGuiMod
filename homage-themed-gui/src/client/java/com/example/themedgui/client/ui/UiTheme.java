package com.example.themedgui.client.ui;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * An open registry of selectable UI themes. DARK/LIGHT/MINT are pre-registered
 * as the built-ins, but any mod can add its own at client-init time:
 *
 *   UiTheme.register("myMod:brand", "My Mod", new UiPalette(...));
 *
 * No need to touch this file for a new theme - that's the whole point of
 * making this an open registry instead of a closed enum.
 */
public final class UiTheme {

	private static final LinkedHashMap<String, UiTheme> REGISTRY = new LinkedHashMap<>();

	public static final UiTheme DARK = register("dark", "Dark", UiPalette.dark());
	public static final UiTheme LIGHT = register("light", "Light", UiPalette.light());
	public static final UiTheme MINT = register("mint", "Mint", UiPalette.mint());

	private final String id;
	private final String displayName;
	private final UiPalette palette;

	private UiTheme(String id, String displayName, UiPalette palette) {
		this.id = id;
		this.displayName = displayName;
		this.palette = palette;
	}

	/**
	 * Registers a new theme (or replaces one with the same id). Call this once,
	 * during your mod's client init, before any config screen is opened.
	 */
	public static synchronized UiTheme register(String id, String displayName, UiPalette palette) {
		UiTheme theme = new UiTheme(id, displayName, palette);
		REGISTRY.put(id, theme);
		return theme;
	}

	/** All registered themes, built-in and custom, in registration order. */
	public static List<UiTheme> values() {
		return List.copyOf(REGISTRY.values());
	}

	/** Looks up a theme by its registry id, falling back to DARK if unknown (e.g. a theme from an unloaded mod). */
	public static UiTheme byId(String id) {
		return REGISTRY.getOrDefault(id, DARK);
	}

	public String id() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UiPalette palette() {
		return palette;
	}

	public UiTheme next() {
		List<UiTheme> all = values();
		int index = all.indexOf(this);
		return all.get((index + 1) % all.size());
	}
}