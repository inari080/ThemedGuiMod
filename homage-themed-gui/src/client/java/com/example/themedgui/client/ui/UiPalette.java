package com.example.themedgui.client.ui;

/**
 * A bundle of ARGB colors for one theme. Keeping this as a plain data holder
 * (rather than scattering hex literals through the screen class) means
 * adding a new theme is just "add one more branch in forTheme(...)".
 *
 * All colors are 0xAARRGGBB. Since Minecraft 1.21.6, text rendering expects
 * an explicit alpha channel - a color with a 0x00 alpha will render fully
 * transparent, which is a common gotcha when porting old RGB-only code.
 */
public record UiPalette(
		int backdrop,
		int panel,
		int panelBorder,
		int sidebar,
		int hover,
		int selected,
		int text,
		int mutedText,
		int accent,
		int toggleOn,
		int toggleOff
) {

	public static UiPalette forTheme(UiTheme theme) {
		return switch (theme) {
			case DARK -> new UiPalette(
					0xCC101014, // backdrop (semi-transparent dim)
					0xFF1B1B22, // panel
					0xFF2E2E3A, // panel border
					0xFF16161C, // sidebar
					0xFF2A2A34, // hover
					0xFF3A3A50, // selected
					0xFFEAEAF0, // text
					0xFF8C8C9A, // muted text
					0xFF7C9CFF, // accent
					0xFF5CE08A, // toggle on
					0xFF4A4A55  // toggle off
			);
			case LIGHT -> new UiPalette(
					0xCCF2F2F5,
					0xFFFFFFFF,
					0xFFD9D9E0,
					0xFFECECF1,
					0xFFE3E3EC,
					0xFFD6DBFF,
					0xFF202028,
					0xFF6B6B78,
					0xFF3F65E0,
					0xFF2FAE64,
					0xFFC7C7D1
			);
			case MINT -> new UiPalette(
					0xCC0E1912,
					0xFF122019,
					0xFF1E3A2A,
					0xFF0E1912,
					0xFF1B3324,
					0xFF215038,
					0xFFE7F5EC,
					0xFF7FAE93,
					0xFF4CD98A,
					0xFF4CD98A,
					0xFF32493C
			);
		};
	}
}
