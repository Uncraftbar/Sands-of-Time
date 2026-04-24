package com.uncraftbar.sandsoftime.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class ModConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ModConfig INSTANCE;

    // ── HUD ──
    public final ForgeConfigSpec.EnumValue<HudDisplayMode> storedTimeHudMode;

    // ── Block blacklist ──
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockBlacklist;

    // ── Entity blacklist ──
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> entityBlacklist;

    public enum HudDisplayMode {
        ON_USE,
        ALWAYS,
        OFF
    }

    ModConfig(ForgeConfigSpec.Builder builder) {
        builder.push("entropic_hourglass");

        builder.translation("config.sands_of_time.entropic_hourglass")
                .comment("Settings for the Entropic Hourglass relic.");

        // HUD mode
        storedTimeHudMode = builder
                .translation("config.sands_of_time.entropic_hourglass.stored_time_hud_mode")
                .comment(
                        "Controls when the stored time HUD is displayed.",
                        "ON_USE = shown briefly when stored time changes",
                        "ALWAYS = always visible while hourglass is in inventory",
                        "OFF = never shown"
                )
                .defineEnum("storedTimeHudMode", HudDisplayMode.ON_USE);

        // Block blacklist
        blockBlacklist = builder
                .translation("config.sands_of_time.entropic_hourglass.block_blacklist")
                .comment(
                        "List of block IDs that cannot be accelerated by Temporal Injection.",
                        "Example: [\"minecraft:spawner\", \"minecraft:end_portal_frame\"]"
                )
                .defineListAllowEmpty(
                        "blockBlacklist",
                        List.of(),
                        o -> o instanceof String
                );

        // Entity blacklist
        entityBlacklist = builder
                .translation("config.sands_of_time.entropic_hourglass.entity_blacklist")
                .comment(
                        "List of entity type IDs that cannot be overclocked by Bio-Overclock.",
                        "Example: [\"minecraft:warden\", \"minecraft:ender_dragon\"]"
                )
                .defineListAllowEmpty(
                        "entityBlacklist",
                        List.of(),
                        o -> o instanceof String
                );

        builder.pop();
    }

    static {
        var pair = new ForgeConfigSpec.Builder().configure(ModConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }
}
