package com.uncraftbar.sandsoftime.init;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Forge 1.20.1 replacement for the 1.21 data component used by the NeoForge build.
 */
public final class StoredTime {
    private static final String TAG_STORED_TIME = "StoredTime";

    private StoredTime() {
    }

    public static double get(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getDouble(TAG_STORED_TIME) : 0.0D;
    }

    public static void set(ItemStack stack, double value) {
        stack.getOrCreateTag().putDouble(TAG_STORED_TIME, Math.max(0.0D, value));
    }
}
