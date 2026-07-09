package com.example.themedgui.client;

import com.example.themedgui.client.config.SettingRegistry;
import com.example.themedgui.client.config.ThemedGuiConfig;
import com.example.themedgui.client.hud.OverlayPositionStore;
import com.example.themedgui.client.hud.ThemedHud;
import com.example.themedgui.client.ui.OverlayPositionScreen;
import com.example.themedgui.client.ui.ThemedConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

	@Override
	public void onInitializeClient() {
		REGISTRY = new SettingRegistry(MOD_ID, CONFIG);
		OverlayPositionStore.init(MOD_ID);
		CONFIG.editHudPosition = () -> {
			Minecraft client = Minecraft.getInstance();
			client.setScreen(new OverlayPositionScreen());
		};

		openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.themedgui.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ThemedConfigScreen(null, REGISTRY));
				}
			}
		});

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
				ThemedHud::extract
		);
	}
}