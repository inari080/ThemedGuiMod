package com.example.themedgui.client.hud;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists a manually dragged HUD overlay position.
 *
 * Kept separate from SettingRegistry (which persists @Setting-annotated fields
 * by reflection) because a dragged pixel position isn't really a "setting" the
 * user picks from a row - it's saved state, closer to a window position than
 * a config value. Null until the user drags the overlay at least once; until
 * then ThemedHud falls back to the corner chosen in Appearance settings.
 */
public class OverlayPositionStore {

    private static final Gson GSON = new Gson();
    private static Path filePath;

    private static Integer x;
    private static Integer y;

    private record Data(int x, int y) {}

    public static void init(String modId) {
        filePath = FabricLoader.getInstance().getConfigDir().resolve(modId + "_hud_position.json");
        load();
    }

    private static void load() {
        if (filePath == null || !Files.exists(filePath)) return;
        try {
            Data data = GSON.fromJson(Files.readString(filePath), Data.class);
            if (data != null) {
                x = data.x();
                y = data.y();
            }
        } catch (IOException | JsonSyntaxException ignored) {
        }
    }

    public static void save(int newX, int newY) {
        x = newX;
        y = newY;
        if (filePath == null) return;
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, GSON.toJson(new Data(newX, newY)));
        } catch (IOException ignored) {
        }
    }

    public static void reset() {
        x = null;
        y = null;
        if (filePath == null) return;
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    public static boolean hasCustomPosition() {
        return x != null && y != null;
    }

    public static int x() { return x; }
    public static int y() { return y; }
}