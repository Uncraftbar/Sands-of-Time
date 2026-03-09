package com.uncraftbar.sandsoftime;

import com.uncraftbar.sandsoftime.config.ModConfig;
import com.uncraftbar.sandsoftime.init.DataComponentRegistry;
import com.uncraftbar.sandsoftime.init.EntityRegistry;
import com.uncraftbar.sandsoftime.init.ItemRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SandsOfTime.MOD_ID)
public class SandsOfTime {
    public static final String MOD_ID = "sands_of_time";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SandsOfTime(IEventBus modEventBus, ModContainer container) {
        // Register config
        container.registerConfig(Type.COMMON, ModConfig.SPEC);

        // Register deferred registries
        ItemRegistry.register(modEventBus);
        DataComponentRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);

        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);

        // Client-only setup (all client class references are in ClientSetup to avoid server NoClassDefFoundError)
        if (FMLEnvironment.dist.isClient()) {
            com.uncraftbar.sandsoftime.client.ClientSetup.init(modEventBus, container);
        }

        // Server events
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Sands of Time initializing...");
    }

    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Sands of Time loaded on server.");
    }
}
