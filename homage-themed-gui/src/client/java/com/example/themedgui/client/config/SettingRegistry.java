package com.example.themedgui.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Scans a config holder object for @Setting fields, groups them by category,
 * and loads/saves the plain field values as JSON next to the other configs.
 */
public class SettingRegistry {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Object holder;
    private final Path filePath;
    private final LinkedHashMap<String, List<SettingNode>> byCategory = new LinkedHashMap<>();

    public SettingRegistry(String modId, Object holder) {
        this.holder = holder;
        this.filePath = FabricLoader.getInstance().getConfigDir().resolve(modId + ".json");
        scan();
        load();
    }

    private void scan() {
        for (Field field : holder.getClass().getFields()) {
            Setting ann = field.getAnnotation(Setting.class);
            if (ann == null) continue;

            SettingNode.Kind kind;
            Class<?> type = field.getType();
            if (ann.sectionHeader()) {
                kind = SettingNode.Kind.SECTION_HEADER;
            } else if (type == boolean.class) {
                kind = SettingNode.Kind.TOGGLE;
            } else if (type == int.class && ann.color()) {
                kind = SettingNode.Kind.COLOR;
            } else if (type == int.class || type == float.class) {
                kind = SettingNode.Kind.SLIDER;
            } else if (type.isEnum()) {
                kind = SettingNode.Kind.ENUM;
            } else if (Runnable.class.isAssignableFrom(type)) {
                kind = SettingNode.Kind.ACTION;
            } else {
                continue; // TEXT can be added later the same way
            }

            SettingNode node = new SettingNode(holder, field, ann.category(), ann.label(),
                    ann.tooltip(), kind, ann.min(), ann.max());
            byCategory.computeIfAbsent(ann.category(), k -> new ArrayList<>()).add(node);
        }
    }

    public List<String> categories() {
        return new ArrayList<>(byCategory.keySet());
    }

    public List<SettingNode> nodes(String category) {
        return byCategory.getOrDefault(category, List.of());
    }

    /** Flattened view across every category, in scan order. Used by the settings-screen search box. */
    public List<SettingNode> allNodes() {
        List<SettingNode> all = new ArrayList<>();
        for (List<SettingNode> nodes : byCategory.values()) {
            all.addAll(nodes);
        }
        return all;
    }

    public void load() {
        if (!Files.exists(filePath)) return;
        try {
            String json = Files.readString(filePath);
            Map<?, ?> raw = GSON.fromJson(json, Map.class);
            if (raw == null) return;
            for (List<SettingNode> nodes : byCategory.values()) {
                for (SettingNode node : nodes) {
                    Object saved = raw.get(node.label());
                    if (saved == null) continue;
                    if (node.kind() == SettingNode.Kind.TOGGLE && saved instanceof Boolean b) {
                        if (b != node.getBoolean()) node.toggle();
                    } else if (node.kind() == SettingNode.Kind.SLIDER && saved instanceof Number n) {
                        double ratio = node.max() > node.min()
                                ? (n.doubleValue() - node.min()) / (node.max() - node.min()) : 0;
                        node.setFromRatio(ratio);
                    } else if (node.kind() == SettingNode.Kind.ENUM && saved instanceof String s) {
                        node.setEnumByName(s);
                    } else if (node.kind() == SettingNode.Kind.COLOR && saved instanceof Number n) {
                        node.setColor(n.intValue());
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void save() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (List<SettingNode> nodes : byCategory.values()) {
            for (SettingNode node : nodes) {
                Object value = switch (node.kind()) {
                    case TOGGLE -> node.getBoolean();
                    case SLIDER -> node.getNumber();
                    case ENUM -> node.getEnum() != null ? node.getEnum().name() : null;
                    case COLOR -> node.getColor();
                    default -> null;
                };
                if (value != null) out.put(node.label(), value);
            }
        }
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, GSON.toJson(out));
        } catch (IOException ignored) {
        }
    }
}