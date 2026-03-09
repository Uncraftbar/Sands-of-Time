package com.uncraftbar.sandsoftime.init;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.items.charm.EntropicHourglassItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, SandsOfTime.MOD_ID);

    public static final Supplier<EntropicHourglassItem> ENTROPIC_HOURGLASS =
            ITEMS.register("entropic_hourglass", EntropicHourglassItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
