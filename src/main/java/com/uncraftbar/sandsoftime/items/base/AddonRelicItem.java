package com.uncraftbar.sandsoftime.items.base;

import com.uncraftbar.sandsoftime.SandsOfTime;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;

/**
 * Base class for all addon relic items. Overrides getConfigRoute() so that the
 * Relics config system uses this addon's mod ID as the config namespace instead
 * of the base "relics" mod ID.
 */
public abstract class AddonRelicItem extends RelicItem {

    public AddonRelicItem() {
        super();
    }

    public AddonRelicItem(Properties properties) {
        super(properties);
    }

    @Override
    public String getConfigRoute() {
        return SandsOfTime.MOD_ID;
    }
}
