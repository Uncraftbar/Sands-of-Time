package com.uncraftbar.sandsoftime.client.config;

import com.uncraftbar.sandsoftime.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Simple Forge Mods config screen for the common Entropic Hourglass options. */
public class SandsConfigScreen extends Screen {
    private static final int LABEL = 0xA0A0A0;
    private static final int TITLE = 0xFFFFFF;

    private final Screen parent;
    private EditBox blockBlacklist;
    private EditBox entityBlacklist;

    public SandsConfigScreen(Screen parent) {
        super(Component.translatable("config.sands_of_time.entropic_hourglass"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int y = Math.max(36, this.height / 2 - 82);

        addRenderableWidget(CycleButton.builder((ModConfig.HudDisplayMode mode) -> Component.literal(mode.name()))
                .withValues(ModConfig.HudDisplayMode.values())
                .withInitialValue(ModConfig.INSTANCE.storedTimeHudMode.get())
                .withTooltip(mode -> Tooltip.create(Component.translatable("config.sands_of_time.entropic_hourglass.stored_time_hud_mode.tooltip")))
                .create(center - 100, y, 200, 20,
                        Component.translatable("config.sands_of_time.entropic_hourglass.stored_time_hud_mode"),
                        (button, mode) -> ModConfig.INSTANCE.storedTimeHudMode.set(mode)));

        y += 44;
        blockBlacklist = new EditBox(this.font, center - 150, y, 300, 20,
                Component.translatable("config.sands_of_time.entropic_hourglass.block_blacklist"));
        blockBlacklist.setMaxLength(1024);
        blockBlacklist.setValue(join(ModConfig.INSTANCE.blockBlacklist.get()));
        blockBlacklist.setHint(Component.literal("minecraft:spawner, minecraft:end_portal_frame"));
        addRenderableWidget(blockBlacklist);

        y += 44;
        entityBlacklist = new EditBox(this.font, center - 150, y, 300, 20,
                Component.translatable("config.sands_of_time.entropic_hourglass.entity_blacklist"));
        entityBlacklist.setMaxLength(1024);
        entityBlacklist.setValue(join(ModConfig.INSTANCE.entityBlacklist.get()));
        entityBlacklist.setHint(Component.literal("minecraft:warden, minecraft:ender_dragon"));
        addRenderableWidget(entityBlacklist);

        y += 40;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
            save();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(center - 154, y, 150, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> {
            ModConfig.INSTANCE.storedTimeHudMode.set(ModConfig.HudDisplayMode.ON_USE);
            ModConfig.INSTANCE.blockBlacklist.set(List.of());
            ModConfig.INSTANCE.entityBlacklist.set(List.of());
            rebuildWidgets();
        }).bounds(center + 4, y, 150, 20).build());
    }

    @Override
    public void onClose() {
        save();
        Minecraft.getInstance().setScreen(parent);
    }

    private void save() {
        ModConfig.INSTANCE.blockBlacklist.set(parseList(blockBlacklist.getValue()));
        ModConfig.INSTANCE.entityBlacklist.set(parseList(entityBlacklist.getValue()));
        ModConfig.SPEC.save();
    }

    private static String join(List<? extends String> values) {
        return String.join(", ", values);
    }

    private static List<String> parseList(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, TITLE);

        int center = this.width / 2;
        int y = Math.max(36, this.height / 2 - 82);
        graphics.drawString(this.font, Component.translatable("config.sands_of_time.entropic_hourglass.stored_time_hud_mode.tooltip"), center - 180, y + 23, LABEL, false);

        y += 44;
        graphics.drawString(this.font, Component.translatable("config.sands_of_time.entropic_hourglass.block_blacklist"), center - 150, y - 11, TITLE, false);
        graphics.drawString(this.font, Component.translatable("config.sands_of_time.entropic_hourglass.block_blacklist.tooltip"), center - 180, y + 23, LABEL, false);

        y += 44;
        graphics.drawString(this.font, Component.translatable("config.sands_of_time.entropic_hourglass.entity_blacklist"), center - 150, y - 11, TITLE, false);
        graphics.drawString(this.font, Component.translatable("config.sands_of_time.entropic_hourglass.entity_blacklist.tooltip"), center - 180, y + 23, LABEL, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
