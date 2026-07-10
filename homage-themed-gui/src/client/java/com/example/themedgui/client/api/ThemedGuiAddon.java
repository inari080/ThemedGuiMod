package com.example.themedgui.client.api;

/**
 * Implement this and expose it as a "themedgui:addon" entrypoint in your own
 * mod's fabric.mod.json to plug your mod's settings into ThemedGuiMod's hub
 * screen (the mod list you see when pressing O).
 *
 * fabric.mod.json:
 * <pre>{@code
 * "entrypoints": {
 *   "themedgui:addon": [ "com.mymod.MyThemedGuiAddon" ]
 * },
 * "depends": { "themedgui": "*" }
 * }</pre>
 *
 * Implementation:
 * <pre>{@code
 * public class MyThemedGuiAddon implements ThemedGuiAddon {
 *     @Override
 *     public void register(AddonRegistration registration) {
 *         registration.registerMod("mymod", "My Mod", MyModConfig.INSTANCE,
 *                 Identifier.of("mymod", "textures/gui/logo.png"));
 *     }
 * }
 * }</pre>
 *
 * {@code MyModConfig.INSTANCE} is a plain object whose public fields are
 * annotated with {@link com.example.themedgui.client.config.Setting}, exactly
 * like the config class you'd pass to a standalone SettingRegistry.
 */
public interface ThemedGuiAddon {
    void register(AddonRegistration registration);
}
