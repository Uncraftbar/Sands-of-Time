package com.uncraftbar.sandsoftime.client;

import com.uncraftbar.sandsoftime.config.ModConfig;
import com.uncraftbar.sandsoftime.entities.TimeAcceleratorEntity;
import com.uncraftbar.sandsoftime.init.StoredTime;
import com.uncraftbar.sandsoftime.init.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HUD overlay for the Entropic Hourglass.
 *
 * <p>Renders two independent elements:</p>
 * <ol>
 *   <li><b>Entity acceleration popup</b> — shows speed + remaining time when
 *       the player looks at an entity being accelerated by Bio-Overclock.</li>
 *   <li><b>Stored time display</b> — shows the hourglass's stored temporal
 *       energy. Display mode is configurable: ON_USE (default), ALWAYS, or OFF.</li>
 * </ol>
 */
public class AccelerationHudOverlay {

    private static final double LOOK_RANGE = 64.0;
    private static final int RECOMPUTE_INTERVAL_TICKS = 5;

    /** How long (in game ticks) the stored-time HUD stays visible in ON_USE mode. */
    private static final int ON_USE_DISPLAY_DURATION = 60; // 3 seconds

    // --- Entity acceleration cache ---
    private static TimeAcceleratorEntity cachedAccelerator = null;
    private static long lastComputeTick = -1;

    // --- Stored time on-use tracking ---
    private static double lastKnownStoredTime = -1;
    private static long lastTimeChangeTick = -1;

    /**
     * Implements {@code LayeredDraw.Layer#render}.
     */
    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        long currentTick = mc.level.getGameTime();

        // --- Part 1: Entity acceleration popup ---
        renderEntityAccelerationPopup(graphics, mc, player, partialTick, currentTick);

        // --- Part 2: Stored time display ---
        renderStoredTimeDisplay(graphics, mc, player, currentTick);
    }

    // ========== Entity Acceleration Popup ==========

    private static void renderEntityAccelerationPopup(GuiGraphics graphics, Minecraft mc, Player player,
                                                       float partialTick, long currentTick) {
        // Recompute the looked-at accelerator every N game ticks
        if (currentTick < lastComputeTick || currentTick - lastComputeTick >= RECOMPUTE_INTERVAL_TICKS) {
            lastComputeTick = currentTick;
            cachedAccelerator = computeLookedAtAccelerator(player, partialTick);
        }

        if (cachedAccelerator == null || cachedAccelerator.isRemoved()) {
            cachedAccelerator = null;
            return;
        }

        int speed = cachedAccelerator.getSpeedMultiplier();
        int remainingSec = Math.max(0, cachedAccelerator.getRemainingTicks() / 20);

        String text = "\u00A76\u23F1 x" + speed + " \u00A77| \u00A7f" + remainingSec + "s";

        int textWidth = mc.font.width(text);
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 2 - 35;

        graphics.fill(x - 4, y - 3, x + textWidth + 4, y + 12, 0x80000000);
        graphics.drawString(mc.font, text, x, y, 0xFFFFFF, false);
    }

    // ========== Stored Time Display ==========

    private static void renderStoredTimeDisplay(GuiGraphics graphics, Minecraft mc, Player player, long currentTick) {
        ModConfig.HudDisplayMode mode = ModConfig.INSTANCE.storedTimeHudMode.get();
        if (mode == ModConfig.HudDisplayMode.OFF) return;

        // Find the hourglass in player's curio slots or inventory
        ItemStack hourglassStack = findHourglass(player);
        if (hourglassStack.isEmpty()) {
            lastKnownStoredTime = -1;
            return;
        }

        double storedTime = StoredTime.get(hourglassStack);

        // ON_USE mode: detect when stored time DECREASES (ability was used) and show for a few seconds.
        // We only trigger on decreases to avoid firing during passive accumulation.
        if (mode == ModConfig.HudDisplayMode.ON_USE) {
            if (lastKnownStoredTime >= 0 && storedTime < lastKnownStoredTime - 0.01) {
                // Stored time decreased — ability was used, start display timer
                lastTimeChangeTick = currentTick;
            }
            lastKnownStoredTime = storedTime;

            // Only display if within the display window
            if (lastTimeChangeTick < 0 || currentTick - lastTimeChangeTick > ON_USE_DISPLAY_DURATION) {
                return;
            }
        } else {
            // ALWAYS mode — always display
            lastKnownStoredTime = storedTime;
        }

        // Render the stored time HUD element
        String formattedTime = formatTime(storedTime);
        String text = "\u00A76\u231B \u00A7eStored: \u00A7f" + formattedTime;

        int textWidth = mc.font.width(text);
        int screenHeight = graphics.guiHeight();

        // Position: bottom-left corner, above chat area
        int x = 6;
        int y = screenHeight - 58;

        // Semi-transparent background
        graphics.fill(x - 4, y - 3, x + textWidth + 4, y + 12, 0x80000000);

        // Text
        graphics.drawString(mc.font, text, x, y, 0xFFFFFF, false);
    }

    // ========== Utilities ==========

    /**
     * Performs the expensive raycast + entity scan to find the accelerator
     * attached to whatever entity the player is currently looking at.
     */
    private static TimeAcceleratorEntity computeLookedAtAccelerator(Player player, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 lookVec = player.getViewVector(partialTick);
        Vec3 endPos = eyePos.add(lookVec.scale(LOOK_RANGE));
        AABB searchBox = player.getBoundingBox().inflate(LOOK_RANGE);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                e -> !e.isSpectator() && e.isAlive() && e.isPickable()
                        && !(e instanceof TimeAcceleratorEntity),
                LOOK_RANGE * LOOK_RANGE);

        if (entityHit == null) return null;

        Entity target = entityHit.getEntity();

        List<TimeAcceleratorEntity> accelerators = mc.level.getEntitiesOfClass(
                TimeAcceleratorEntity.class,
                target.getBoundingBox().inflate(2.0),
                e -> e.getMode() == TimeAcceleratorEntity.Mode.ENTITY
                        && target.getUUID().equals(e.getTargetEntityUUID()));

        return accelerators.isEmpty() ? null : accelerators.get(0);
    }

    /** Find the Entropic Hourglass in the player's curio charm slot or main inventory. */
    private static ItemStack findHourglass(Player player) {
        // Check curios first
        AtomicReference<ItemStack> found = new AtomicReference<>(ItemStack.EMPTY);
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.findFirstCurio(ItemRegistry.ENTROPIC_HOURGLASS.get()).ifPresent(slotResult ->
                        found.set(slotResult.stack())));

        if (!found.get().isEmpty()) return found.get();

        // Fallback: check main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ItemRegistry.ENTROPIC_HOURGLASS.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Format seconds into human-readable time (e.g., "1h 23m 45s" or "45.2s"). */
    private static String formatTime(double totalSeconds) {
        if (totalSeconds < 60) {
            return String.format("%.1fs", totalSeconds);
        }
        int total = (int) Math.floor(totalSeconds);
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
        return String.format("%dm %ds", minutes, seconds);
    }
}
