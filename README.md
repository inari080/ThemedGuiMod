# ThemedGuiMod (Fabric / Minecraft 26.1.2)

A reusable, annotation-driven settings screen for Fabric mods: a theme
system, smooth inertia scrolling, an eased open/close transition, a live
color picker, and a reflection-based `@Setting` system that turns plain
config fields into a full UI automatically. Press **O** in-game to open it.

Started as a from-scratch homage to the look and feel of slicker mod config
screens (Joo0n Reimagined's, specifically); has since grown into something
meant to be **dropped into future mods**, not just this demo one.

## Screenshots
![Demo screenshot](![img.png](homage-themed-gui/imgfile/img.png)img.png)

![Demo screenshot](![img_1.png](homage-themed-gui/imgfile/img_1.png))
## Quick start: adding a setting

Add a field to your config class and annotate it - that's it, no screen code
to write:

```java
public class MyModConfig {
    @Setting(category = "General", label = "Enable feature", tooltip = "Turns the whole thing on or off")
    public boolean enabled = true;

    @Setting(category = "Appearance", label = "Scale", tooltip = "0.5x - 2.0x", min = 0.5, max = 2.0)
    public float scale = 1.0f;

    @Setting(category = "Appearance", label = "Overlay color", color = true)
    public int overlayColor = 0x33AAFF;

    @Setting(category = "Appearance", label = "Position")
    public MyEnum position = MyEnum.TOP_LEFT; // any enum field becomes a cycling ENUM row

    @Setting(category = "Advanced", label = "Reset stats", tooltip = "Clears saved stats")
    public Runnable resetStats = () -> { /* ... */ }; // Runnable fields become an ACTION row
}
```

Field type decides the row kind automatically:

| Field type | Row kind |
|---|---|
| `boolean` | TOGGLE |
| `int` / `float` | SLIDER (needs `min`/`max`) |
| `int` with `color = true` | COLOR (opens a live HSV picker) |
| any `enum` | ENUM (click to cycle values) |
| `Runnable` | ACTION (click to run) |

Then in your client entrypoint:

```java
public static final MyModConfig CONFIG = new MyModConfig();
public static SettingRegistry REGISTRY;

@Override
public void onInitializeClient() {
    REGISTRY = new SettingRegistry("mymod", CONFIG); // scans, loads mymod.json from the config dir
    // open the screen from a keybind, a button, wherever:
    client.setScreen(new ThemedConfigScreen(parent, REGISTRY,
            new ScreenBranding("My Mod", Identifier.of("mymod", "textures/gui/logo.png"))));
}
```

`SettingRegistry` handles reflection scanning, Gson persistence (load on
construct, `save()` whenever you want to flush), and search. `ThemedConfigScreen`
handles literally everything else - you never touch rendering or input code
for a new setting.

## Making it yours: customization points

This is the part that matters for reuse across mods - none of the following
require editing the library files themselves:

**Branding.** Pass a `ScreenBranding(title, logo)` to the `ThemedConfigScreen`
constructor. `logo` is an optional 16x16 `Identifier` texture shown in the
header; omit it (or use the 2-arg constructor) for text-only.

**Themes.** `UiTheme` is an open registry, not a closed enum - register your
own palette from your mod's init instead of editing `UiTheme`/`UiPalette`:

``` java
UiTheme.register("mymod:brand", "My Mod", new UiPalette(
        0xCC0A0E1A, // backdrop
        0xFF12141C, // panel
        // ... copy UiPalette.dark() as a starting point and tweak
));
```

It'll show up as another card in the built-in theme picker (Dark/Light/Mint
plus whatever you've registered) automatically.

**Icons.** Two independent hooks:
- Per-setting: `@Setting(..., icon = "mymod:textures/gui/icon_speed.png")`
- Per-category (sidebar tab): `registry.setCategoryIcon("Automation", Identifier.of("mymod", "textures/gui/cat_auto.png"))`

Both are optional; rows/tabs without an icon just render as before.

**Feature code reads the config directly** - `ThemedHud` in this repo is the
pattern to copy: check `MyModConfig` fields (or `SettingNode` getters if you
need something more dynamic) wherever you render/tick, no event bus or
observer wiring required. Call `registry.save()` after any programmatic
change.

## What's inside

| File | Purpose |
|---|---|
| `config/Setting.java` | The `@Setting` annotation - category, label, tooltip, min/max, color flag, icon path. |
| `config/SettingNode.java` | Reflection wrapper around one annotated field; typed getters/setters per row kind. |
| `config/SettingRegistry.java` | Scans a config object, groups nodes by category, handles Gson load/save, category icons. |
| `config/ThemedGuiConfig.java` | This demo's config holder - copy the pattern for a new mod's config class. |
| `ui/UiTheme.java` | **Open** theme registry (`register(id, name, palette)`), not a closed enum. |
| `ui/UiPalette.java` | ARGB color bundle; `dark()`/`light()`/`mint()` factories, or build your own. |
| `ui/ScreenBranding.java` | Title + optional logo passed into `ThemedConfigScreen`, instead of hardcoded per-mod. |
| `ui/ThemedConfigScreen.java` | The main screen: sidebar categories (with icons), scrollable settings list, search box, scrollbar, tooltips, theme button. |
| `ui/ColorPickerScreen.java` | Full HSV picker with a live hex field, opened from any COLOR row. |
| `ui/OverlayPositionScreen.java` | Drag-to-position sub-screen, opened from the HUD-position ACTION row. |
| `ui/UiSettingsScreen.java` | Theme picker with live palette-preview cards. |
| `ui/SmoothScroll.java` | Mouse-wheel notches → inertia-smoothed scroll (exponential decay). |
| `ui/ScreenTransition.java` | Ease-out-cubic slide + fade for opening/closing. |
| `ui/Anim.java` | Generic per-key eased value (hover glow, toggle/slider knob position, category slide). |
| `ui/Toast.java` | Small "X saved" notifications in the panel corner. |
| `ui/ColorMath.java` | RGB↔HSV conversion for the color picker. |
| `ui/AmbientBackdrop.java` | Drifting background light blobs + panel edge glow. |
| `hud/ThemedHud.java` | Example in-game HUD element driven entirely by config fields. |
| `hud/OverlayPositionStore.java` | Persists a free-dragged HUD position separately from the enum corner presets. |
| `ThemedGuiModClient.java` | Client entrypoint: builds the registry, registers the keybind and HUD element. |

## Requirements

- JDK 25 (Minecraft 26.1+ requires Java 25; the old Java 21 toolchain won't work)
- IntelliJ IDEA 2025.3+ if you use it (needed for full Java 25 support)

## Building / running

```
./gradlew build          # produces build/libs/themedgui-1.0.0.jar
./gradlew runClient       # launches a dev Minecraft client with the mod loaded
```

## Version notes (please double-check before building)

Minecraft 26.1 was a large modding-facing update: the game ships
**unobfuscated** with Mojang's own names, Yarn mappings are no longer used,
and screens render through a `GuiGraphicsExtractor` + "extract render state"
pipeline instead of the old `GuiGraphics#render(...)` style. Input events
also moved to record types (`MouseButtonEvent`, etc.) instead of raw
`(double, double, int)` parameters. The versions below were current as of
mid-2026; **check [fabricmc.net/develop](https://fabricmc.net/develop) for
the latest numbers before you build**, since these move fast:

- `minecraft_version = 26.1.2`
- `loader_version = 0.18.5`
- `loom_version = 1.15-SNAPSHOT`
- `fabric_api_version = 0.149.1+26.1.2`

All of these live in `gradle.properties`.

## Where to go from here

- **Porting to a new mod**: copy `config/` and `ui/` wholesale into the new
  project, write a config class in the same shape as `ThemedGuiConfig`, and
  register a `SettingRegistry` + open a `ThemedConfigScreen` from your
  keybind/button. No other file needs editing.
- **Shared library module**: if you're doing this across several mods, the
  next step is pulling `config/` + `ui/` into their own Gradle subproject
  (`:themedgui-core` or similar) that each mod depends on, instead of
  copy-pasting the folder each time.
- **TEXT setting kind**: strings aren't wired up yet (`SettingRegistry#scan`
  skips non-matching field types) - same pattern as the others: add a
  `Kind.TEXT`, an `EditBox`-backed row in `ThemedConfigScreen`, and Gson
  load/save branches.