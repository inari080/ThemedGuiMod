# Themed GUI Demo (Fabric / Minecraft 26.1.2)

A minimal, from-scratch Fabric mod that reproduces the *techniques* behind a
slick, animated Minecraft mod config screen - a theme switcher, smooth
inertia scrolling, and an eased open/close transition - without copying any
code from any other mod. Press **O** in-game to open it.

This is meant as a learning/starting-point template, not a finished mod:
the settings rows are placeholder labels, nothing is persisted to disk, and
there's no in-game functionality behind the toggles. Swap those out for your
own config system once you're happy with the look and feel.

## What's inside

| File | Purpose |
|---|---|
| `ui/UiTheme.java` | Enum of selectable themes (Dark / Light / Mint). |
| `ui/UiPalette.java` | One ARGB color bundle per theme (`UiPalette.forTheme(...)`). |
| `ui/SmoothScroll.java` | Turns mouse-wheel notches into inertia-smoothed scrolling (exponential decay, not a fixed-speed lerp). |
| `ui/ScreenTransition.java` | Ease-out-cubic slide + fade for opening/closing the screen. |
| `ui/ThemedConfigScreen.java` | Ties it all together: sidebar categories, a scrollable content list, and a theme-cycle button. |
| `ThemedGuiModClient.java` | Registers the "O" keybinding that opens the screen. |

## Requirements

- JDK 25 (Minecraft 26.1+ requires Java 25; the old Java 21 toolchain won't work)
- IntelliJ IDEA 2025.3+ if you use it (needed for full Java 25 support)
- Git (only if you want to diff against the official Fabric example mod)

## Building / running

```
./gradlew build          # produces build/libs/themedgui-1.0.0.jar
./gradlew runClient       # launches a dev Minecraft client with the mod loaded
```

(You'll need to generate the Gradle wrapper first if it isn't present -
`gradle wrapper` - or just open the folder in IntelliJ IDEA and let it import.)

## Version notes (please double-check before building)

Minecraft 26.1 was a large modding-facing update: the game is now shipped
**unobfuscated** with Mojang's own names, Yarn mappings are no longer used,
and screens render through a new `GuiGraphicsExtractor` + "extract render
state" pipeline instead of the old `GuiGraphics#render(...)` style. The
versions below were current as of mid-2026; **check
[fabricmc.net/develop](https://fabricmc.net/develop) for the latest numbers
before you build**, since these move fast:

- `minecraft_version = 26.1.2`
- `loader_version = 0.18.5`
- `loom_version = 1.15-SNAPSHOT` (a stable 1.15.x may exist by the time you read this - prefer that if so)
- `fabric_api_version = 0.149.1+26.1.2`

All of these live in `gradle.properties` so you only need to edit them in
one place.

## Where to go from here

- Replace `ROW_LABELS` in `ThemedConfigScreen` with a real settings model
  (an enum + a small `Config` class that (de)serializes to JSON is the usual
  approach), and wire up `mouseClicked` to actually flip values.
- Add more `UiTheme` entries - just add the enum constant and a matching
  branch in `UiPalette.forTheme(...)`.
- If you want the settings to persist across restarts, look at Minecraft's
  `Path` for the config directory (`FabricLoader.getInstance().getConfigDir()`)
  and serialize your config model there.
