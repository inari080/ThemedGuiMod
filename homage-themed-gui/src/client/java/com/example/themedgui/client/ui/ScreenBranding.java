package com.example.themedgui.client.ui;

import net.minecraft.resources.Identifier;

/**
 * What to show in the config screen's header: a title and an optional logo
 * icon. Pass your own when constructing {@link ThemedConfigScreen} instead of
 * editing that class per-mod.
 *
 * Example:
 *   new ThemedConfigScreen(parent, registry,
 *       new ScreenBranding("Lootrun Advisor", Identifier.of("lootrunadvisor", "textures/gui/logo.png")));
 */
public record ScreenBranding(String title, Identifier logo) {

    public ScreenBranding(String title) {
        this(title, null);
    }
}