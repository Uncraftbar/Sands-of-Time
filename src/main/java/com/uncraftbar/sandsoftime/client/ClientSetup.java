package com.uncraftbar.sandsoftime.client;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.init.EntityRegistry;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Client-only event handlers. Registered conditionally from {@link SandsOfTime}.
 */
public class ClientSetup {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::onRegisterEntityRenderers);
        modEventBus.addListener(ClientSetup::onRegisterGuiOverlays);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) ->
                        new net.minecraftforge.client.gui.ModListScreen(screen)));
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.TIME_ACCELERATOR.get(), TimeAcceleratorRenderer::new);
    }

    private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "acceleration_hud", (gui, graphics, partialTick, screenWidth, screenHeight) -> AccelerationHudOverlay.render(graphics, partialTick));
    }
}
