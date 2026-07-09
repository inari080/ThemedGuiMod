package com.example.themedgui.client.config;

/** Plain field holder — your feature code reads these fields directly. */
public class ThemedGuiConfig {

    @Setting(category = "General", label = "Enable overlay")
    public boolean enableOverlay = true;

    @Setting(category = "General", label = "Show minimap")
    public boolean showMinimap = false;

    @Setting(category = "Appearance", label = "HUD scale", min = 0.5, max = 2.0)
    public float hudScale = 1.0f;

    @Setting(category = "Appearance", label = "Opacity", min = 0, max = 255)
    public int opacity = 200;
}