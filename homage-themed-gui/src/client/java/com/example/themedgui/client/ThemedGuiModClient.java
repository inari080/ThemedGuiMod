package com.example.themedgui.client;

import com.example.themedgui.client.automining.AutoMiner;
import com.example.themedgui.client.automining.MiningRoute;
import com.example.themedgui.client.automining.RouteManager;
import com.example.themedgui.client.automining.RouteMiner;
import com.example.themedgui.client.config.SettingRegistry;
import com.example.themedgui.client.config.ThemedGuiConfig;
import com.example.themedgui.client.hud.OverlayPositionStore;
import com.example.themedgui.client.hud.ThemedHud;
import com.example.themedgui.client.ui.OverlayPositionScreen;
import com.example.themedgui.client.ui.ThemedConfigScreen;
import com.example.themedgui.client.ui.UiSettings;
import com.example.themedgui.client.ui.UiSettingsScreen;
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
	public static AutoMiner autoMiner;
	public static RouteMiner routeMiner;
	public static RouteManager routeManager;

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "general")
	);

	private static KeyMapping openConfigKey;
	private static KeyMapping toggleAutoMiningKey;

	@Override
	public void onInitializeClient() {
		REGISTRY = new SettingRegistry(MOD_ID, CONFIG);
		UiSettings.INSTANCE.ensureLoaded();
		OverlayPositionStore.init(MOD_ID);
		autoMiner = new AutoMiner();
		routeMiner = new RouteMiner();
		routeManager = new RouteManager();

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

		toggleAutoMiningKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.themedgui.toggle_auto_mining",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new ThemedConfigScreen(null, REGISTRY));
				}
			}

			while (toggleAutoMiningKey.consumeClick()) {
				CONFIG.autoMiningEnabled = !CONFIG.autoMiningEnabled;
				REGISTRY.save();

				if (CONFIG.autoMiningEnabled && CONFIG.miningMode == ThemedGuiConfig.MiningMode.ROUTE) {
					// Load sample route when enabling route mining
					MiningRoute sampleRoute = routeManager.getRoute("sample_route");
					if (sampleRoute != null) {
						sampleRoute.setLoop(CONFIG.routeLoop);
						routeMiner.setRoute(sampleRoute);
						routeMiner.setBaseDelay(CONFIG.miningDelay);
					}
				}

				client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
						"Auto Mining " + (CONFIG.autoMiningEnabled ? "enabled" : "disabled") +
								" (" + CONFIG.miningMode + ")"
				));
			}

			// Tick auto miners based on mode
			if (CONFIG.autoMiningEnabled) {
				if (CONFIG.miningMode == ThemedGuiConfig.MiningMode.SIMPLE) {
					autoMiner.tick();
				} else if (CONFIG.miningMode == ThemedGuiConfig.MiningMode.ROUTE) {
					routeMiner.tick();
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