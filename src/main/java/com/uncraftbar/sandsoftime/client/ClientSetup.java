package com.uncraftbar.sandsoftime.client;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.init.EntityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Client-only event handlers. Registered conditionally from
 * {@link SandsOfTime} when running on the physical client.
 */
public class ClientSetup {

    public static void init(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(ClientSetup::onRegisterEntityRenderers);
        modEventBus.addListener(ClientSetup::onRegisterGuiLayers);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.TIME_ACCELERATOR.get(), TimeAcceleratorRenderer::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(SandsOfTime.MOD_ID, "acceleration_hud"),
                AccelerationHudOverlay::render);
    }
}
