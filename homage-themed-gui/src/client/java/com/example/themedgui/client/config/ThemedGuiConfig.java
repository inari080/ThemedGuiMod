package com.example.themedgui.client.config;

/** Plain field holder — your feature code reads these fields directly. */
public class ThemedGuiConfig {

    public enum OverlayPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    @Setting(category = "General", label = "Enable overlay", tooltip = "Toggles the HUD overlay box on or off entirely")
    public boolean enableOverlay = true;

    @Setting(category = "General", label = "Show minimap", tooltip = "Reserved for a future minimap overlay")
    public boolean showMinimap = false;

    @Setting(category = "Appearance", label = "HUD scale", tooltip = "Scales the overlay box size, 0.5x - 2.0x", min = 0.5, max = 2.0)
    public float hudScale = 1.0f;

    @Setting(category = "Appearance", label = "Opacity", tooltip = "Overlay transparency, 0 = invisible, 255 = solid", min = 0, max = 255)
    public int opacity = 200;

    @Setting(category = "Appearance", label = "Overlay color", tooltip = "Pick a custom color for the HUD overlay box", color = true)
    public int overlayColor = 0x33AAFF;

    @Setting(category = "Appearance", label = "Overlay position", tooltip = "Snaps the overlay to one of the four screen corners")
    public OverlayPosition overlayPosition = OverlayPosition.TOP_LEFT;

    @Setting(category = "Appearance", label = "Edit HUD position", tooltip = "Drag the overlay directly to where you want it")
    public Runnable editHudPosition = () -> {};

    @Setting(category = "Appearance", label = "UI Settings", tooltip = "Open the theme picker with live palette previews")
    public Runnable openUiSettings = () -> {};
}