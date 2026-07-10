package com.example.themedgui.client;

import com.example.themedgui.client.api.AddonManager;
import com.example.themedgui.client.config.SettingRegistry;
import com.example.themedgui.client.config.ThemedGuiConfig;
import com.example.themedgui.client.hud.OverlayPositionStore;
import com.example.themedgui.client.hud.ThemedHud;
import com.example.themedgui.client.ui.OverlayPositionScreen;
import com.example.themedgui.client.ui.ThemedConfigScreen;
import com.example.themedgui.client.ui.ThemedGuiHubScreen;
import com.example.themedgui.client.ui.UiSettings;
import com.example.themedgui.client.ui.UiSettingsScreen;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ThemedGuiModClient implements ClientModInitializer {

	public static final String MOD_ID = "themedgui";

	// 他のfeatureコードからはこのstaticフィールドを直接参照してON/OFFを見る
	public static final ThemedGuiConfig CONFIG = new ThemedGuiConfig();
	public static SettingRegistry REGISTRY;


	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "general")
	);

	private static KeyMapping openConfigKey;
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		REGISTRY = new SettingRegistry(MOD_ID, CONFIG);
		UiSettings.INSTANCE.ensureLoaded();
		OverlayPositionStore.init(MOD_ID);

		CONFIG.editHudPosition = () -> {
			Minecraft client = Minecraft.getInstance();
			client.setScreen(new OverlayPositionScreen());
		};
		CONFIG.openUiSettings = () -> {
			Minecraft client = Minecraft.getInstance();
			Screen parent = client.screen instanceof ThemedConfigScreen configScreen ? configScreen : null;
			client.setScreen(new UiSettingsScreen(parent));
		};

		openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.themedgui.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				CATEGORY
		));

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.themedgui.toggle_auto_mining",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null) {
					// "themedgui" itself is always the first row; other mods show up
					// automatically via their "themedgui:addon" entrypoint.
					var ownEntry = new AddonManager.AddonEntry(MOD_ID, "Themed GUI Demo", null, REGISTRY);
					client.setScreen(new ThemedGuiHubScreen(List.of(ownEntry)));
				}
			}
		});
	}
}