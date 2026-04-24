package com.uncraftbar.sandsoftime;

import com.uncraftbar.sandsoftime.config.ModConfig;
import com.uncraftbar.sandsoftime.init.EntityRegistry;
import com.uncraftbar.sandsoftime.init.ItemRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SandsOfTime.MOD_ID)
public class SandsOfTime {
    public static final String MOD_ID = "sands_of_time";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SandsOfTime() {
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        // Register config
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);

        // Register deferred registries
        ItemRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);

        // Register lifecycle events
        modEventBus.addListener(this::commonSetup);

        // Client-only setup (all client class references are in ClientSetup to avoid server NoClassDefFoundError)
        if (FMLEnvironment.dist.isClient()) {
            com.uncraftbar.sandsoftime.client.ClientSetup.init(modEventBus);
        }

        // Server events
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Sands of Time initializing...");
    }

    private void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Sands of Time loaded on server.");
    }
}
