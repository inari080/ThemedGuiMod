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

    @Setting(category = "Appearance", label = "Overlay position")
    public OverlayPosition overlayPosition = OverlayPosition.TOP_LEFT;

    @Setting(category = "Appearance", label = "Edit HUD position", tooltip = "Drag the overlay directly to where you want it")
    public Runnable editHudPosition = () -> {};

    @Setting(category = "Appearance", label = "UI Settings", tooltip = "Open the theme picker with live palette previews")
    public Runnable openUiSettings = () -> {};

    // === Auto Mining (Private Server Testing Only) ===
    @Setting(category = "Auto Mining", label = "Enable Auto Mining", tooltip = "WARNING: For private server testing only. Automatically breaks blocks in front of player.")
    public boolean autoMiningEnabled = false;

    @Setting(category = "Auto Mining", label = "Mining Mode", tooltip = "Simple: Mine blocks in front. Route: Follow coordinate list with AOTV.")
    public MiningMode miningMode = MiningMode.SIMPLE;

    @Setting(category = "Auto Mining", label = "Mining Delay (ms)", tooltip = "Delay between block breaks in milliseconds. Higher values = slower mining.", min = 50, max = 1000)
    public int miningDelay = 200;

    @Setting(category = "Auto Mining", label = "Mining Range", tooltip = "How far to mine (1 = directly in front, 2 = 2 blocks, etc.)", min = 1, max = 5)
    public int miningRange = 1;

    @Setting(category = "Auto Mining", label = "Auto Switch Tool", tooltip = "Automatically switch to best tool for the block")
    public boolean autoSwitchTool = true;

    @Setting(category = "Auto Mining", label = "Route Loop", tooltip = "Loop the mining route continuously")
    public boolean routeLoop = true;

    @Setting(category = "Auto Mining", label = "Macro Evasion", tooltip = "Add random delays and movements to evade macro detection")
    public boolean macroEvasion = true;

    public enum MiningMode { SIMPLE, ROUTE }
}