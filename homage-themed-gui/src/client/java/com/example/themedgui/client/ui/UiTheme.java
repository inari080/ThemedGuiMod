package com.example.themedgui.client.ui;

/**
 * The set of selectable themes. Each theme just maps to a {@link UiPalette}
 * (see {@link UiPalette#forTheme(UiTheme)}). Add a new constant here and a
 * matching branch in that method to add another theme.
 */
public enum UiTheme {
	DARK("Dark"),
	LIGHT("Light"),
	MINT("Mint");

	private final String displayName;

	UiTheme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UiTheme next() {
		UiTheme[] values = values();
		return values[(this.ordinal() + 1) % values.length];
	}
}
