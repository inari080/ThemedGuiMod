package com.example.themedgui.client.config;

import java.lang.reflect.Field;

public class SettingNode {

    public enum Kind { TOGGLE, SLIDER, TEXT, ACTION, ENUM, COLOR }

    private final Object holder;
    private final Field field;
    private final String category;
    private final String label;
    private final String tooltip;
    private final Kind kind;
    private final double min;
    private final double max;

    public SettingNode(Object holder, Field field, String category, String label,
                       String tooltip, Kind kind, double min, double max) {
        this.holder = holder;
        this.field = field;
        this.category = category;
        this.label = label;
        this.tooltip = tooltip;
        this.kind = kind;
        this.min = min;
        this.max = max;
        field.setAccessible(true);
    }

    public String category() { return category; }
    public String label() { return label; }
    public String tooltip() { return tooltip; }
    public Kind kind() { return kind; }

    // --- TOGGLE ---
    public boolean getBoolean() {
        try { return field.getBoolean(holder); } catch (IllegalAccessException e) { return false; }
    }

    public void toggle() {
        try { field.setBoolean(holder, !field.getBoolean(holder)); } catch (IllegalAccessException ignored) {}
    }

    // --- SLIDER (backed by float or int fields) ---
    public double getRatio() {
        double value = getNumber();
        return max > min ? (value - min) / (max - min) : 0;
    }

    public void setFromRatio(double ratio) {
        double clamped = Math.max(0, Math.min(1, ratio));
        double value = min + clamped * (max - min);
        try {
            if (field.getType() == int.class) {
                field.setInt(holder, (int) Math.round(value));
            } else {
                field.setFloat(holder, (float) value);
            }
        } catch (IllegalAccessException ignored) {}
    }

    public double getNumber() {
        try {
            return field.getType() == int.class ? field.getInt(holder) : field.getFloat(holder);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    public double min() { return min; }
    public double max() { return max; }

    // --- ENUM ---
    public Enum<?> getEnum() {
        try { return (Enum<?>) field.get(holder); } catch (IllegalAccessException e) { return null; }
    }

    /** Cycles to the next (or previous, with step = -1) constant of the field's enum type. */
    public void cycleEnum(int step) {
        try {
            Enum<?>[] constants = (Enum<?>[]) field.getType().getEnumConstants();
            Enum<?> current = (Enum<?>) field.get(holder);
            int next = Math.floorMod(current.ordinal() + step, constants.length);
            field.set(holder, constants[next]);
        } catch (IllegalAccessException ignored) {}
    }

    /** Used by SettingRegistry#load to restore a saved enum constant by name. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setEnumByName(String name) {
        try {
            Enum<?> value = Enum.valueOf((Class<? extends Enum>) field.getType(), name);
            field.set(holder, value);
        } catch (IllegalArgumentException | IllegalAccessException ignored) {
            // unknown/renamed constant in saved file — keep current value
        }
    }

    /** Human-readable label for the current enum value, e.g. HUD_SCALE -> "Hud scale". */
    public String enumValueLabel() {
        Enum<?> value = getEnum();
        if (value == null) return "";
        String raw = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    // --- ACTION (backed by a Runnable field, e.g. "Edit HUD position") ---
    public void runAction() {
        try {
            Object value = field.get(holder);
            if (value instanceof Runnable runnable) runnable.run();
        } catch (IllegalAccessException ignored) {}
    }

    // --- COLOR (backed by an int field holding a packed 0xRRGGBB value) ---
    public int getColor() {
        try { return field.getInt(holder); } catch (IllegalAccessException e) { return 0; }
    }

    public void setColor(int rgb) {
        try { field.setInt(holder, rgb); } catch (IllegalAccessException ignored) {}
    }
}