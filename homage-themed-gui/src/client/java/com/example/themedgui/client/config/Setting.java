package com.example.themedgui.client.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public field in a config holder class as a user-visible setting.
 * The screen discovers these via reflection at startup (see SettingRegistry).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    String category();          // e.g. "General", "Appearance"
    String label();              // display label
    String tooltip() default ""; // shown on hover, optional
    double min() default 0;      // used for SLIDER kind (float/int fields)
    double max() default 1;
    boolean color() default false; // if true on an int field, shown as a color-picker row instead of a slider
    boolean sectionHeader() default false; // if true, renders as a section header within the category
}