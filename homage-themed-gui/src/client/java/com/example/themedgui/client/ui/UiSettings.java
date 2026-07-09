package com.example.themedgui.client.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists the config-screen color theme across restarts, separate from the
 * feature settings in {@link com.example.themedgui.client.config.ThemedGuiConfig}.
 * Mirrors the role of Jooon's {@code JooonUiSettings}.
 */
public final class UiSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("themedgui-ui.json");

    public static final UiSettings INSTANCE = new UiSettings();

    private UiTheme theme = UiTheme.DARK;
    private boolean loaded = false;

    private UiSettings() {
    }

    public synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            String json = Files.readString(FILE);
            Data data = GSON.fromJson(json, Data.class);
            if (data != null && data.theme != null) {
                try {
                    theme = UiTheme.valueOf(data.theme);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public UiTheme getTheme() {
        ensureLoaded();
        return theme;
    }

    public synchronized void setTheme(UiTheme theme) {
        ensureLoaded();
        this.theme = theme;
        save();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data data = new Data();
            data.theme = theme.name();
            Files.writeString(FILE, GSON.toJson(data));
        } catch (IOException ignored) {
        }
    }

    private static class Data {
        String theme;
    }
}