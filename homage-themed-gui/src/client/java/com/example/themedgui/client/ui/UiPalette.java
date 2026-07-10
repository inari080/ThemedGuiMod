package com.example.themedgui.client.ui;

/**
 * A bundle of ARGB colors for one theme. Keeping this as a plain data holder
 * (rather than scattering hex literals through the screen class) means
 * adding a new theme is just "add one more branch in forTheme(...)".
 *
 * All colors are 0xAARRGGBB. Since Minecraft 1.21.6, text rendering expects
 * an explicit alpha channel - a color with a 0x00 alpha will render fully
 * transparent, which is a common gotcha when porting old RGB-only code.
 *
 * Layered surfaces, darkest to lightest (dark themes) so nesting reads clearly:
 *   backdrop -> panel -> header/sidebar -> paper (scroll content) -> field (control chip)
 */
public record UiPalette(
		int backdrop,
		int panel,
		int panelBorder,
		int sidebar,
		int header,
		int paper,
		int field,
		int fieldText,
		int hover,
		int selected,
		int text,
		int mutedText,
		int line,
		int mutedLine,
		int accent,
		int accentSoft,
		int toggleOn,
		int toggleOff,
		int tooltipBack,
		int tooltipText
) {

	public static UiPalette forTheme(UiTheme theme) {
		return switch (theme) {
			case DARK -> new UiPalette(
					0xCC101014, // backdrop (semi-transparent dim)
					0xFF1B1B22, // panel
					0xFF2E2E3A, // panel border
					0xFF16161C, // sidebar
					0xFF1F1F28, // header bar
					0xFF20202A, // paper (scrollable content backdrop)
					0xFF272733, // field (chip behind sliders/toggles)
					0xFFC7C7D6, // field text (values on chips)
					0xFF2A2A34, // hover
					0xFF3A3A50, // selected
					0xFFEAEAF0, // text
					0xFF8C8C9A, // muted text
					0xFF2E2E3A, // line (structural divider)
					0xFF23232C, // muted line (row separator)
					0xFF7C9CFF, // accent
					0xFF3D4E80, // accent soft (tracks, glows)
					0xFF5CE08A, // toggle on
					0xFFD9525C, // toggle off (red, for clear on/off contrast)
					0xF20B0B10, // tooltip back
					0xFFEAEAF0  // tooltip text
			);
			case LIGHT -> new UiPalette(
					0xCCF2F2F5,
					0xFFFFFFFF,
					0xFFD9D9E0,
					0xFFECECF1,
					0xFFF5F5F8,
					0xFFF9F9FB,
					0xFFEFEFF3,
					0xFF3A3A46,
					0xFFE3E3EC,
					0xFFD6DBFF,
					0xFF202028,
					0xFF6B6B78,
					0xFFD9D9E0,
					0xFFE7E7ED,
					0xFF3F65E0,
					0xFFB9C6F5,
					0xFF2FAE64,
					0xFFE2626B,
					0xF2202028,
					0xFFF5F5F8
			);
			case MINT -> new UiPalette(
					0xCC0E1912,
					0xFF122019,
					0xFF1E3A2A,
					0xFF0E1912,
					0xFF152417,
					0xFF16261C,
					0xFF1C3025,
					0xFFCFEBD9,
					0xFF1B3324,
					0xFF215038,
					0xFFE7F5EC,
					0xFF7FAE93,
					0xFF1E3A2A,
					0xFF17281D,
					0xFF4CD98A,
					0xFF275C3F,
					0xFF4CD98A,
					0xFFC15C52,
					0xF20A140F,
					0xFFE7F5EC
			);
		};
	}
}