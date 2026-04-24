package com.uncraftbar.sandsoftime.items.charm;

import com.uncraftbar.sandsoftime.SandsOfTime;
import com.uncraftbar.sandsoftime.config.ModConfig;
import com.uncraftbar.sandsoftime.entities.TimeAcceleratorEntity;
import com.uncraftbar.sandsoftime.init.StoredTime;
import com.uncraftbar.sandsoftime.init.EntityRegistry;
import com.uncraftbar.sandsoftime.init.ItemRegistry;
import com.uncraftbar.sandsoftime.items.base.AddonRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastStage;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.TooltipData;
import it.hurts.sskirillss.relics.utils.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import top.theillusivec4.curios.api.SlotContext;

import java.util.*;

/**
 * Entropic Hourglass — a time-manipulation relic found in desert ruins.
 *
 * <p><b>Chrono-Accumulation</b> (passive, level 0): Passively collects stored time
 * while carried in any inventory or curio slot.</p>
 *
 * <p><b>Temporal Injection</b> (active, level 3): Accelerates a targeted block's
 * tick rate by injecting stored time. Cycling speed steps, proportional cost.</p>
 *
 * <p><b>Bio-Overclock</b> (active, level 8): Overclocks a living entity's
 * biological processes for 30 seconds. Extends on re-click.</p>
 */
public class EntropicHourglassItem extends AddonRelicItem {

    // ── Speed steps for block/entity acceleration ──
    private static final int[] SPEED_STEPS = {2, 4, 8, 16, 32, 64, 128, 256, 512};

    // ── Constants ──
    private static final int DURATION_TICKS = 600;          // 30 seconds
    private static final double COST_PER_LEVEL = 30.0D;     // seconds per speed level
    private static final double ACTIVE_RANGE = 64.0D;       // blocks
    private static final int ACCUMULATION_WRITE_INTERVAL = 20; // Write to component every 20 ticks
    private static final int MIN_CAST_INTERVAL_TICKS = 5;   // Minimal anti-spam (250ms) for active abilities

    /**
     * Guard against double accumulation when both {@code inventoryTick} and {@code curioTick}
     * fire for the same item in a Curios slot. Keyed by player UUID, value is the last game time
     * we accumulated at. Evicted periodically (every 200 ticks) to prevent memory leaks.
     */
    private static final HashMap<UUID, Long> lastAccumulationTick = new HashMap<>();

    /** Per-player last cast game time — minimal anti-spam for active abilities. */
    private static final HashMap<UUID, Long> lastCastTick = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════════════
    //  Relic Data
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        // ── Chrono-Accumulation (passive, level 0, max upgrade 5) ──
                        .ability(AbilityData.builder("chrono_accumulation")
                                .maxLevel(5)
                                .stat(StatData.builder("accumulation_rate")
                                        .initialValue(0.1D, 1.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> MathUtils.round(value, 2))
                                        .build())

                                .build())
                        // ── Temporal Injection (active, level 3, max upgrade 5) ──
                        .ability(AbilityData.builder("temporal_injection")
                                .requiredLevel(3)
                                .maxLevel(5)
                                .stat(StatData.builder("max_multiplier")
                                        .initialValue(4.0D, 256.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) Math.floor(value))
                                        .build())
                                .active(CastData.builder()
                                        .type(CastType.INSTANTANEOUS)
                                        .build())

                                .build())
                        // ── Bio-Overclock (active, level 8, max upgrade 7) ──
                        .ability(AbilityData.builder("bio_overclock")
                                .requiredLevel(8)
                                .maxLevel(7)
                                .stat(StatData.builder("potency")
                                        .initialValue(2.0D, 8.0D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.2D)
                                        .formatValue(value -> (int) Math.floor(value))
                                        .build())
                                .active(CastData.builder()
                                        .type(CastType.INSTANTANEOUS)
                                        .build())

                                .build())
                        .build())
                .leveling(LevelingData.builder()
                        .initialCost(50)
                        .maxLevel(15)
                        .step(50)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.DESERT)
                        .build())
                .style(StyleData.builder()
                        .tooltip(TooltipData.builder()
                                .build())
                        .build())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Tooltip
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        double stored = StoredTime.get(stack);
        tooltipComponents.add(Component.translatable("tooltip.sands_of_time.entropic_hourglass.stored_time",
                formatTime(stored)));
    }

    private String formatTime(double seconds) {
        if (seconds < 60) return String.format("%.1fs", seconds);
        if (seconds < 3600) return String.format("%.1fm", seconds / 60.0);
        if (seconds < 86400) return String.format("%.1fh", seconds / 3600.0);
        return String.format("%.1fd", seconds / 86400.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Tick Handlers (Accumulation)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof Player player) {
            accumulateTime(stack, player);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        super.curioTick(slotContext, stack);
        Level level = slotContext.entity().level();
        if (!level.isClientSide() && slotContext.entity() instanceof Player player) {
            accumulateTime(stack, player);
        }
    }

    /**
     * Accumulates stored time based on the chrono_accumulation stat.
     * Writes to the STORED_TIME data component every ACCUMULATION_WRITE_INTERVAL ticks.
     * Uses a UUID-keyed dedup guard to prevent double accumulation from Curios.
     */
    private void accumulateTime(ItemStack stack, Player player) {
        if (!this.canUseAbility(stack, "chrono_accumulation")) return;
        long gameTime = player.level().getGameTime();
        if (gameTime % ACCUMULATION_WRITE_INTERVAL != 0) return;

        // Dedup guard: prevent double accumulation from both inventoryTick and curioTick
        UUID playerId = player.getUUID();
        Long lastTick = lastAccumulationTick.get(playerId);
        if (lastTick != null && lastTick == gameTime) return;
        lastAccumulationTick.put(playerId, gameTime);

        // Periodic eviction: every 200 ticks, prune stale entries to prevent memory leaks
        if (gameTime % 200 == 0) {
            lastAccumulationTick.values().removeIf(tick -> (gameTime - tick) > ACCUMULATION_WRITE_INTERVAL * 2);
            lastCastTick.values().removeIf(tick -> (gameTime - tick) > MIN_CAST_INTERVAL_TICKS * 10);
        }

        double rate = this.getAbilityValue(stack, "chrono_accumulation", "accumulation_rate");
        double currentTime = StoredTime.get(stack);
        double increment = (rate / 20.0D) * ACCUMULATION_WRITE_INTERVAL;
        double newTime = currentTime + increment;
        StoredTime.set(stack, newTime);

        // Grant XP when stored time crosses a 1-minute (60s) threshold
        if ((int) (currentTime / 60.0) < (int) (newTime / 60.0)) {
            this.spreadExperience(player, stack, 1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Active Abilities
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void castActiveAbility(ItemStack stack, Player player, String ability, CastType type, CastStage stage) {
        Level level = player.level();
        if (level.isClientSide()) return;

        // Server-side unlock check — prevents spoofed packets from using locked abilities
        if (!this.canUseAbility(stack, ability)) return;

        // Minimal server-side anti-spam (250ms between casts) using monotonic game time
        UUID playerId = player.getUUID();
        long gameTime = level.getGameTime();
        Long lastTick = lastCastTick.get(playerId);
        if (lastTick != null && (gameTime - lastTick) < MIN_CAST_INTERVAL_TICKS) return;
        lastCastTick.put(playerId, gameTime);

        if (ability.equals("temporal_injection")) {
            handleTemporalInjection(stack, player, level);
        } else if (ability.equals("bio_overclock")) {
            handleBioOverclock(stack, player, level);
        }
    }

    // ─── Temporal Injection ──────────────────────────────────────────────

    private void handleTemporalInjection(ItemStack stack, Player player, Level level) {
        // Raycast for a block
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 reach = eye.add(look.scale(ACTIVE_RANGE));
        BlockHitResult hitResult = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.no_target_block"), true);
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        // Determine if the block is acceleratable:
        // 1) Block entity with a ticker (furnaces, hoppers, etc.)
        // 2) Randomly ticking block (crops, saplings, fire, etc.)
        boolean hasBlockEntityTicker = false;
        if (be != null && state.getBlock() instanceof EntityBlock entityBlock) {
            @SuppressWarnings("unchecked")
            BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>)
                    entityBlock.getTicker(level, state, be.getType());
            hasBlockEntityTicker = (ticker != null);
        }
        boolean hasRandomTick = state.isRandomlyTicking();

        if (!hasBlockEntityTicker && !hasRandomTick) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.not_tickable"), true);
            return;
        }

        // Check block blacklist
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (ModConfig.INSTANCE.blockBlacklist.get().contains(blockId.toString())) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.block_blacklisted"), true);
            return;
        }

        // Stat value
        double maxMult = this.getAbilityValue(stack, "temporal_injection", "max_multiplier");
        int maxMultInt = Math.max(1, (int) Math.floor(maxMult));

        // Find existing accelerators on this block
        List<TimeAcceleratorEntity> existingAccelerators = findBlockAccelerators(level, pos);

        int currentSpeed = 0;
        if (!existingAccelerators.isEmpty()) {
            // Use the first one as primary, remove duplicates
            TimeAcceleratorEntity primary = existingAccelerators.get(0);
            currentSpeed = primary.getSpeedMultiplier();

            for (int i = 1; i < existingAccelerators.size(); i++) {
                existingAccelerators.get(i).discard();
            }
        }

        // Don't allow downgrade
        if (currentSpeed > maxMultInt) {
            // Existing accelerator exceeds our max — can only refresh at its current speed
            int nextSpeed = currentSpeed;
            boolean isRefresh = true;

            double cost = COST_PER_LEVEL * nextSpeed;
            double storedTime = StoredTime.get(stack);

            if (storedTime < cost) {
                player.displayClientMessage(Component.translatable("message.sands_of_time.insufficient_time"), true);
                return;
            }

            TimeAcceleratorEntity primary = existingAccelerators.get(0);
            primary.addRemainingTicks(DURATION_TICKS);

            StoredTime.set(stack, storedTime - cost);
            int xpAward = Math.max(1, (int) (cost / 30.0D));
            this.spreadExperience(player, stack, xpAward);

            player.displayClientMessage(Component.translatable("message.sands_of_time.block_refreshed",
                    nextSpeed, formatTime(cost)), true);
            return;
        }

        // Build available steps for this player's max
        List<Integer> availableSteps = buildAvailableSteps(maxMultInt);

        // Determine next speed
        int nextSpeed;
        boolean isRefresh = false;
        if (currentSpeed == 0) {
            // Fresh application: start at lowest step
            nextSpeed = availableSteps.isEmpty() ? maxMultInt : availableSteps.get(0);
        } else {
            // Cycling: get next step
            nextSpeed = getNextStep(currentSpeed, availableSteps, maxMultInt);
            if (nextSpeed == currentSpeed) {
                isRefresh = true; // At max, refreshing
            }
        }

        // Cost calculation:
        // - Upgrading: pay only the difference (e.g., 2x→4x costs for 2 levels)
        // - Fresh application: pay full cost
        // - Refreshing at max: proportional cost (COST_PER_LEVEL × speed)
        double cost;
        if (isRefresh) {
            // Proportional refresh fee — prevents exploit at high speeds
            cost = COST_PER_LEVEL * nextSpeed;
        } else if (currentSpeed > 0) {
            // Upgrade: pay the difference
            cost = COST_PER_LEVEL * (nextSpeed - currentSpeed);
        } else {
            // Fresh: pay full cost
            cost = COST_PER_LEVEL * nextSpeed;
        }

        double storedTime = StoredTime.get(stack);
        if (storedTime < cost) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.insufficient_time"), true);
            return;
        }

        if (isRefresh && !existingAccelerators.isEmpty()) {
            // Refresh: extend timer
            TimeAcceleratorEntity primary = existingAccelerators.get(0);
            primary.addRemainingTicks(DURATION_TICKS);
        } else {
            // Remove old and spawn new
            for (TimeAcceleratorEntity old : existingAccelerators) {
                old.discard();
            }

            TimeAcceleratorEntity accelerator = new TimeAcceleratorEntity(
                    level, pos, nextSpeed, DURATION_TICKS);
            level.addFreshEntity(accelerator);
        }

        // Deduct cost
        StoredTime.set(stack, storedTime - cost);
        int xpAward = Math.max(1, (int) (cost / 30.0D));
        this.spreadExperience(player, stack, xpAward);

        if (isRefresh) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.block_refreshed",
                    nextSpeed, formatTime(cost)), true);
        } else {
            player.displayClientMessage(Component.translatable("message.sands_of_time.block_accelerated",
                    nextSpeed, formatTime(cost)), true);
        }
    }

    // ─── Bio-Overclock ───────────────────────────────────────────────────

    private void handleBioOverclock(ItemStack stack, Player player, Level level) {
        // Raycast for a living entity (non-player)
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 reach = eye.add(look.scale(ACTIVE_RANGE));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(ACTIVE_RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, reach, searchBox,
                e -> e instanceof LivingEntity && !(e instanceof Player),
                ACTIVE_RANGE * ACTIVE_RANGE);

        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.no_target_entity"), true);
            return;
        }

        // Line-of-sight check
        if (!player.hasLineOfSight(target)) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.no_line_of_sight"), true);
            return;
        }

        // Check entity blacklist
        ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (ModConfig.INSTANCE.entityBlacklist.get().contains(entityId.toString())) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.entity_blacklisted"), true);
            return;
        }

        // Stat value
        double potencyRaw = this.getAbilityValue(stack, "bio_overclock", "potency");
        int multiplier = Math.max(1, (int) Math.floor(potencyRaw));

        // Check for existing entity accelerators
        List<TimeAcceleratorEntity> existingEntityAccelerators = findEntityAccelerators(level, target);

        int currentEntitySpeed = 0;
        if (!existingEntityAccelerators.isEmpty()) {
            TimeAcceleratorEntity primary = existingEntityAccelerators.get(0);
            currentEntitySpeed = primary.getSpeedMultiplier();

            for (int i = 1; i < existingEntityAccelerators.size(); i++) {
                existingEntityAccelerators.get(i).discard();
            }
        }

        // Cost based on the effective speed (max of our multiplier and existing speed)
        // This prevents cheap refreshing of a stronger overclock placed by another player
        int effectiveSpeed = Math.max(multiplier, currentEntitySpeed);
        double cost = COST_PER_LEVEL * effectiveSpeed;
        double storedTime = StoredTime.get(stack);
        if (storedTime < cost) {
            player.displayClientMessage(Component.translatable("message.sands_of_time.insufficient_time"), true);
            return;
        }

        if (!existingEntityAccelerators.isEmpty()) {
            TimeAcceleratorEntity primary = existingEntityAccelerators.get(0);

            // Don't downgrade
            if (multiplier < currentEntitySpeed) {
                // Extend at existing speed
                primary.addRemainingTicks(DURATION_TICKS);
            } else {
                // Upgrade or extend
                primary.setSpeedMultiplier(multiplier);
                primary.addRemainingTicks(DURATION_TICKS);
            }
        } else {
            // Spawn new
            TimeAcceleratorEntity accelerator = new TimeAcceleratorEntity(
                    level, target, multiplier, DURATION_TICKS);
            level.addFreshEntity(accelerator);
        }

        // Deduct cost
        StoredTime.set(stack, storedTime - cost);
        int xpAward = Math.max(1, (int) (cost / 30.0D));
        this.spreadExperience(player, stack, xpAward);
        player.displayClientMessage(Component.translatable("message.sands_of_time.entity_overclocked",
                target.getDisplayName(), effectiveSpeed, formatTime(cost)), true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private List<Integer> buildAvailableSteps(int maxMultiplier) {
        List<Integer> steps = new ArrayList<>();
        for (int step : SPEED_STEPS) {
            if (step <= maxMultiplier) steps.add(step);
        }
        if (steps.isEmpty() || steps.get(steps.size() - 1) != maxMultiplier) {
            steps.add(maxMultiplier);
        }
        return steps;
    }

    private int getNextStep(int currentSpeed, List<Integer> availableSteps, int maxMultiplier) {
        for (int step : availableSteps) {
            if (step > currentSpeed) return step;
        }
        return maxMultiplier; // At max, return max (isRefresh)
    }

    private List<TimeAcceleratorEntity> findBlockAccelerators(Level level, BlockPos pos) {
        AABB search = new AABB(pos).inflate(0.5);
        return level.getEntitiesOfClass(TimeAcceleratorEntity.class, search, e ->
                e.getMode() == TimeAcceleratorEntity.Mode.BLOCK &&
                        e.getTargetBlockPos().equals(pos));
    }

    private List<TimeAcceleratorEntity> findEntityAccelerators(Level level, Entity target) {
        AABB search = target.getBoundingBox().inflate(2.0);
        return level.getEntitiesOfClass(TimeAcceleratorEntity.class, search, e ->
                e.getMode() == TimeAcceleratorEntity.Mode.ENTITY &&
                        target.getUUID().equals(e.getTargetEntityUUID()));
    }
}
