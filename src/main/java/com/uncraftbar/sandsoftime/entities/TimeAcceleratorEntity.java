package com.uncraftbar.sandsoftime.entities;

import com.uncraftbar.sandsoftime.init.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameRules;

import java.util.Optional;
import java.util.UUID;

/**
 * Invisible entity that accelerates a block's tick rate or an entity's AI.
 * Placed by the Entropic Hourglass's Temporal Injection and Bio-Overclock abilities.
 */
public class TimeAcceleratorEntity extends Entity {

    private static final int MAX_SPEED = 512;

    // ── Synched Data ──
    private static final EntityDataAccessor<Integer> DATA_SPEED =
            SynchedEntityData.defineId(TimeAcceleratorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_END_TIME =
            SynchedEntityData.defineId(TimeAcceleratorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(TimeAcceleratorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET_UUID =
            SynchedEntityData.defineId(TimeAcceleratorEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // ── State ──
    private BlockPos targetBlockPos = BlockPos.ZERO;
    private UUID targetEntityUUID = null;
    private Entity cachedTarget = null;
    private boolean ticking = false; // Re-entrance guard

    public enum Mode {
        BLOCK(0), ENTITY(1);
        public final int id;
        Mode(int id) { this.id = id; }
        public static Mode fromId(int id) { return id == 1 ? ENTITY : BLOCK; }
    }

    // ── Constructors ──

    /** Deserialization constructor */
    public TimeAcceleratorEntity(EntityType<? extends TimeAcceleratorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Block mode constructor */
    public TimeAcceleratorEntity(Level level, BlockPos pos, int speed, int durationTicks) {
        this(EntityRegistry.TIME_ACCELERATOR.get(), level);
        this.targetBlockPos = pos;
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.entityData.set(DATA_SPEED, clampSpeed(speed));
        this.entityData.set(DATA_END_TIME, level.getGameTime() + durationTicks);
        this.entityData.set(DATA_MODE, Mode.BLOCK.id);
    }

    /** Entity mode constructor */
    public TimeAcceleratorEntity(Level level, Entity target, int speed, int durationTicks) {
        this(EntityRegistry.TIME_ACCELERATOR.get(), level);
        this.targetEntityUUID = target.getUUID();
        this.cachedTarget = target;
        this.setPos(target.getX(), target.getY(), target.getZ());
        this.entityData.set(DATA_SPEED, clampSpeed(speed));
        this.entityData.set(DATA_END_TIME, level.getGameTime() + durationTicks);
        this.entityData.set(DATA_MODE, Mode.ENTITY.id);
        this.entityData.set(DATA_TARGET_UUID, Optional.of(target.getUUID()));
    }

    // ── Data ──

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SPEED, 1);
        this.entityData.define(DATA_END_TIME, 0L);
        this.entityData.define(DATA_MODE, 0);
        this.entityData.define(DATA_TARGET_UUID, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_SPEED, clampSpeed(tag.getInt("Speed")));
        entityData.set(DATA_END_TIME, tag.getLong("EndTime"));
        entityData.set(DATA_MODE, tag.getInt("Mode"));
        if (tag.contains("TargetX")) {
            targetBlockPos = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        }
        if (tag.hasUUID("TargetUUID")) {
            targetEntityUUID = tag.getUUID("TargetUUID");
            entityData.set(DATA_TARGET_UUID, Optional.of(targetEntityUUID));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Speed", getSpeedMultiplier());
        tag.putLong("EndTime", entityData.get(DATA_END_TIME));
        tag.putInt("Mode", getMode().id);
        tag.putInt("TargetX", targetBlockPos.getX());
        tag.putInt("TargetY", targetBlockPos.getY());
        tag.putInt("TargetZ", targetBlockPos.getZ());
        if (targetEntityUUID != null) {
            tag.putUUID("TargetUUID", targetEntityUUID);
        }
    }

    // ── Tick ──

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        int remaining = getRemainingTicks();
        if (remaining <= 0) {
            discard();
            return;
        }

        int speed = getSpeedMultiplier();
        if (speed < 1) {
            setSpeedMultiplier(1);
            return;
        }

        if (getMode() == Mode.BLOCK) {
            tickBlock(speed);
        } else {
            tickEntity(speed);
        }
    }

private void tickBlock(int speed) {
        if (ticking) return;
        ticking = true;
        try {
            Level level = level();
            BlockPos pos = targetBlockPos;
            BlockState state = level.getBlockState(pos);

            // ── Block Entity Ticker ──
            BlockEntity be = level.getBlockEntity(pos);
            BlockEntityTicker<BlockEntity> ticker = null;
            if (be != null && state.getBlock() instanceof EntityBlock entityBlock) {
                @SuppressWarnings("unchecked")
                BlockEntityTicker<BlockEntity> t = (BlockEntityTicker<BlockEntity>)
                        entityBlock.getTicker(level, state, be.getType());
                ticker = t;
            }

            // ── Random Tick ──
            boolean randomlyTicking = state.isRandomlyTicking();

            if (ticker == null && !randomlyTicking) {
                discard();
                return;
            }

            // 1. Block Entity Logic (Deterministic)
            // Machine processing speed is linear, so we force ticks directly.
            // We run (speed - 1) because the world already ran the 1st tick.
            int extraTicks = speed - 1; 
            
            if (ticker != null && be != null) {
                for (int i = 0; i < extraTicks; i++) {
                    if (level.getBlockEntity(pos) != be) break;
                    BlockState freshState = level.getBlockState(pos);
                    try {
                        ticker.tick(level, pos, freshState, be);
                    } catch (Exception e) {
                        break;
                    }
                }
            }

            // 2. Random Tick Logic (Probabilistic)
            // FIX: We now simulate the *chance* of a tick, rather than forcing the result.
            if (randomlyTicking && level instanceof ServerLevel serverLevel) {
                RandomSource random = serverLevel.random;
                
                // Fetch the world's specific gamerule (Default is 3)
                int randomTickSpeed = serverLevel.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);

                // If gamerule is 0, random ticks are disabled globally, so we skip acceleration.
                if (randomTickSpeed > 0) {
                    // We loop 'speed' times to simulate 'speed' amount of game ticks passing.
                    // For each simulated tick, we roll the dice.
                    for (int i = 0; i < speed; i++) {
                        state = level.getBlockState(pos);
                        if (!state.isRandomlyTicking()) break;

                        // THE FIX:
                        // Vanilla logic is: select 'randomTickSpeed' blocks out of 4096 per chunk section.
                        // So the probability per tick is (randomTickSpeed / 4096).
                        if (random.nextInt(4096) < randomTickSpeed) {
                            try {
                                state.randomTick(serverLevel, pos, random);
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            ticking = false;
        }
    }

    private void tickEntity(int speed) {
        Entity target = resolveTargetEntity();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }

        // Follow target
        setPos(target.getX(), target.getY(), target.getZ());

        int extraTicks = speed - 1;
        for (int i = 0; i < extraTicks; i++) {
            if (!target.isAlive()) break;
            try {
                target.tick();
            } catch (Exception e) {
                break;
            }
        }
    }

    private Entity resolveTargetEntity() {
        if (cachedTarget != null && cachedTarget.isAlive()) return cachedTarget;
        if (targetEntityUUID == null) {
            Optional<UUID> opt = entityData.get(DATA_TARGET_UUID);
            if (opt.isPresent()) targetEntityUUID = opt.get();
            else return null;
        }
        if (level() instanceof ServerLevel serverLevel) {
            cachedTarget = serverLevel.getEntity(targetEntityUUID);
        }
        return cachedTarget;
    }

    // ── Getters / Setters ──

    public int getSpeedMultiplier() { return entityData.get(DATA_SPEED); }
    public void setSpeedMultiplier(int speed) { entityData.set(DATA_SPEED, clampSpeed(speed)); }

    /** Computes remaining ticks from the stored end time and current game time. */
    public int getRemainingTicks() {
        long endTime = entityData.get(DATA_END_TIME);
        long gameTime = level().getGameTime();
        return (int) Math.max(0, endTime - gameTime);
    }

    public Mode getMode() { return Mode.fromId(entityData.get(DATA_MODE)); }
    public BlockPos getTargetBlockPos() { return targetBlockPos; }

    public UUID getTargetEntityUUID() {
        if (targetEntityUUID != null) return targetEntityUUID;
        Optional<UUID> opt = entityData.get(DATA_TARGET_UUID);
        return opt.orElse(null);
    }

    /** Extends the accelerator's duration by the given number of ticks. */
    public void addRemainingTicks(int ticks) {
        long endTime = entityData.get(DATA_END_TIME);
        long newEndTime = endTime + ticks;
        // Sanity clamp to prevent overflow (max ~3.4 years at 20 tps)
        entityData.set(DATA_END_TIME, Math.min(newEndTime, level().getGameTime() + Integer.MAX_VALUE));
    }

    private static int clampSpeed(int speed) {
        return Math.max(1, Math.min(speed, MAX_SPEED));
    }

    // ── Rendering ──

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(0, 2, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096; // 64 blocks
    }
}
