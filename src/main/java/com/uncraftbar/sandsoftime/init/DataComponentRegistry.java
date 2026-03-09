package com.uncraftbar.sandsoftime.init;

import com.uncraftbar.sandsoftime.SandsOfTime;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.mojang.serialization.Codec;

import java.util.function.Supplier;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, SandsOfTime.MOD_ID);

    /**
     * Stored temporal energy in seconds. Persisted to NBT and synced to client.
     */
    public static final Supplier<DataComponentType<Double>> STORED_TIME =
            DATA_COMPONENTS.register("stored_time", () ->
                    DataComponentType.<Double>builder()
                            .persistent(Codec.DOUBLE)
                            .networkSynchronized(ByteBufCodecs.DOUBLE)
                            .build());

    /**
     * Sets default STORED_TIME = 0.0 on the Entropic Hourglass item.
     */
    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        event.modify(ItemRegistry.ENTROPIC_HOURGLASS.get(), builder ->
                builder.set(STORED_TIME.get(), 0.0D));
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        eventBus.addListener(DataComponentRegistry::modifyComponents);
    }
}
