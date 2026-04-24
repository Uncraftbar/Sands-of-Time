package com.uncraftbar.sandsoftime.init;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.items.charm.EntropicHourglassItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, SandsOfTime.MOD_ID);

    public static final Supplier<EntropicHourglassItem> ENTROPIC_HOURGLASS =
            ITEMS.register("entropic_hourglass", EntropicHourglassItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
