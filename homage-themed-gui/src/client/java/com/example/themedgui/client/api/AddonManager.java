package com.example.themedgui.client.api;

import com.example.themedgui.client.config.SettingRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers every mod that exposes a "themedgui:addon" entrypoint, calls its
 * {@link ThemedGuiAddon#register}, and turns each registered config object
 * into its own {@link SettingRegistry}. Populated lazily, once, on first use
 * (Fabric's entrypoint list is stable after mod init, so a single scan is
 * enough).
 */
public final class AddonManager {

    public static final String ENTRYPOINT_KEY = "themedgui:addon";

    private static List<AddonEntry> entries;

    private AddonManager() {
    }

    /** All addon-registered mod pages, in entrypoint discovery order. */
    public static synchronized List<AddonEntry> getEntries() {
        if (entries == null) {
            entries = scan();
        }
        return entries;
    }

    private static List<AddonEntry> scan() {
        List<AddonEntry> found = new ArrayList<>();
        AddonRegistration registration = (modId, displayName, config, icon) ->
                found.add(new AddonEntry(modId, displayName, icon, new SettingRegistry(modId, config)));

        for (EntrypointContainer<ThemedGuiAddon> container :
                FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT_KEY, ThemedGuiAddon.class)) {
            try {
                container.getEntrypoint().register(registration);
            } catch (Throwable t) {
                String source = container.getProvider().getMetadata().getId();
                System.err.println("[ThemedGuiMod] Addon '" + source + "' failed to register: " + t);
            }
        }
        return Collections.unmodifiableList(found);
    }

    /** One mod's entry in the hub list, plus the registry backing its settings page. */
    public record AddonEntry(String modId, String displayName, Identifier icon, SettingRegistry registry) {
    }
}
