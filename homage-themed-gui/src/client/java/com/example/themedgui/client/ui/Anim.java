package com.example.themedgui.client.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * A generic exponential-decay animator keyed by an arbitrary object (a
 * SettingNode, a row index, a String tag - anything that's a stable
 * identity across frames). Instead of one bespoke "current/target/lastTime"
 * trio per feature (like SmoothScroll had for the scrollbar), this is a
 * single reusable store: call setTarget(key, value) once per frame with
 * where you want that key's value to end up, then tick(key) to advance and
 * read the eased result.
 *
 * Typical uses in this mod: hover glow amount per row (target 1 while
 * hovered, 0 otherwise), a slider/toggle's animated ratio (target =
 * node.getRatio() / node.getBoolean() ? 1 : 0), and the category-switch
 * slide progress.
 */
public class Anim {

    private static final class State {
        float current;
        float target;
        long lastTimeMs = System.currentTimeMillis();
    }

    private final Map<Object, State> states = new HashMap<>();
    private final float speed;

    /** Higher speed = snappier (catches up to target faster). Try 8-20. */
    public Anim(float speed) {
        this.speed = speed;
    }

    public void setTarget(Object key, float target) {
        State state = states.computeIfAbsent(key, k -> {
            State s = new State();
            s.current = target;
            s.target = target;
            return s;
        });
        state.target = target;
    }

    /** Advances the animation for this key by however long it's been since the last tick, and returns the eased value. */
    public float tick(Object key) {
        State state = states.get(key);
        if (state == null) {
            return 0f;
        }
        long now = System.currentTimeMillis();
        float dt = clamp(now - state.lastTimeMs, 1L, 50L) / 1000f;
        state.lastTimeMs = now;

        float diff = state.target - state.current;
        if (Math.abs(diff) < 0.002f) {
            state.current = state.target;
        } else {
            state.current += diff * (1f - (float) Math.exp(-speed * dt));
        }
        return state.current;
    }

    /** Reads the current value without advancing time or requiring a prior setTarget this frame. */
    public float peek(Object key) {
        State state = states.get(key);
        return state == null ? 0f : state.current;
    }

    /** Snaps a key straight to a value with no easing (e.g. on screen init). */
    public void snapTo(Object key, float value) {
        State state = new State();
        state.current = value;
        state.target = value;
        states.put(key, state);
    }

    /** Drops stale keys (e.g. rows that scrolled out and won't be reused) so this doesn't grow forever. */
    public void retainOnly(Iterable<?> liveKeys) {
        java.util.Set<Object> keep = new java.util.HashSet<>();
        liveKeys.forEach(keep::add);
        states.keySet().retainAll(keep);
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}