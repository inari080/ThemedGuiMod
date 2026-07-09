package com.example.themedgui.client.config;

import java.lang.reflect.Field;

public class SettingNode {

    public enum Kind { TOGGLE, SLIDER, TEXT, ACTION }

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
}