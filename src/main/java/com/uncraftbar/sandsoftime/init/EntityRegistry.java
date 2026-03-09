package com.uncraftbar.sandsoftime.init;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.entities.TimeAcceleratorEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SandsOfTime.MOD_ID);

    public static final Supplier<EntityType<TimeAcceleratorEntity>> TIME_ACCELERATOR =
            ENTITIES.register("time_accelerator", () ->
                    EntityType.Builder.<TimeAcceleratorEntity>of(TimeAcceleratorEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(5)
                            .fireImmune()
                            .build("time_accelerator"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
