package com.example.themedgui.client.api;

import net.minecraft.resources.Identifier;

/**
 * Passed into {@link ThemedGuiAddon#register}. Call {@link #registerMod} once
 * per settings page you want to appear in the hub's mod list (usually once).
 */
public interface AddonRegistration {

    /**
     * @param modId       your mod id; also used as the settings file name
     *                    ({@code config/<modId>.json})
     * @param displayName shown as the row label in the hub's mod list
     * @param config      a plain object with public
     *                    {@link com.example.themedgui.client.config.Setting}
     *                    -annotated fields, same shape as a standalone
     *                    SettingRegistry's config object
     * @param icon        optional 16x16 texture shown in that mod's screen
     *                    header once opened; pass {@code null} for none
     */
    void registerMod(String modId, String displayName, Object config, Identifier icon);
}
