package n643064.zombie_tactics;

import n643064.zombie_tactics.profile.MiningProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.Objects;

public class ZombieMineGoal<T extends Zombie & IMarkerFollower> extends Goal {

    private static final int MINING_START_DELAY_TICKS = 10;
    private static final double MAX_MINING_REACH_SQR = 16.0D;


    private int navigationBlockedSinceTick = -1;

    final T zombie;
    final Level level;
    private final MiningProfile profile;

    BlockPos target;
    double progress;
    double hardness = Double.MAX_VALUE;

    final byte[][] offsets = new byte[][]{
            {0, 0, 1},
            {0, 0, -1},
            {1, 0, 0},
            {-1, 0, 0},
            {0, -1, 0},
            {0, 2, 0},

            {1, 0, 1},
            {1, 0, -1},
            {-1, 0, 1},
            {-1, 0, -1}
    };

    public ZombieMineGoal(T zombie) {
        this(
                zombie,
                new MiningProfile(
                        Config.increment,
                        Config.maxHardness,
                        Config.hardnessMult,
                        Config.dropBlocks
                )
        );
    }

    public ZombieMineGoal(T zombie, MiningProfile profile) {
        this.zombie = Objects.requireNonNull(zombie, "zombie");
        this.level = this.zombie.level();
        this.profile = Objects.requireNonNull(profile, "profile");

        this.setFlags(EnumSet.of(
                Goal.Flag.MOVE,
                Goal.Flag.LOOK
        ));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        progress = 0;
        hardness = level.getBlockState(target)
                .getBlock()
                .defaultDestroyTime()
                * profile.hardnessMultiplier();
    }

    boolean scanColumn(BlockPos bp) {
        int diff = Integer.compare(zombie.getBlockY() - bp.getY(), 0);

        if (!checkBlock(bp.offset(0, diff, 0))) {
            if (!checkBlock(bp)) {
                return checkBlock(bp.offset(0, -diff, 0));
            }
        }

        return true;
    }

    private boolean scanTowardTarget(
            BlockPos zombiePos,
            BlockPos destinationPos
    ) {
        int deltaX =
                destinationPos.getX() - zombiePos.getX();
        int deltaY =
                destinationPos.getY() - zombiePos.getY();
        int deltaZ =
                destinationPos.getZ() - zombiePos.getZ();

        int stepX = Integer.compare(deltaX, 0);
        int stepY = Integer.compare(deltaY, 0);
        int stepZ = Integer.compare(deltaZ, 0);

        /*
         * When the target is diagonal, first try the two
         * cardinal side columns. Opening one of these creates
         * an actual space through which the zombie can advance.
         */
        if (stepX != 0 && stepZ != 0) {
            BlockPos xSide =
                    zombiePos.offset(stepX, 0, 0);

            BlockPos zSide =
                    zombiePos.offset(0, 0, stepZ);

            /*
             * Prefer the axis with the larger remaining distance.
             * If that side contains no mineable block, try the
             * other cardinal side.
             */
            if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
                if (scanColumn(xSide.above())) {
                    return true;
                }

                if (scanColumn(zSide.above())) {
                    return true;
                }
            } else {
                if (scanColumn(zSide.above())) {
                    return true;
                }

                if (scanColumn(xSide.above())) {
                    return true;
                }
            }
        }

        /*
         * Fall back to the direct step, including a diagonal
         * position when neither cardinal side contains a block.
         */
        BlockPos directStep =
                zombiePos.offset(stepX, stepY, stepZ);

        return scanColumn(directStep.above());
    }

    boolean checkBlock(BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        final Block block = state.getBlock();
        final float destroyTime = block.defaultDestroyTime();

        if (!block.isPossibleToRespawnInThis(state)
                && destroyTime >= 0
                && destroyTime <= profile.maxHardness()) {

            target = pos;
            return true;
        }

        return false;
    }

    @Override
    public void stop() {
        navigationBlockedSinceTick = -1;

        if (target != null) {
            zombie.level().destroyBlockProgress(
                    zombie.getId(),
                    target,
                    -1
            );

            target = null;
        }

        zombie.getNavigation().recomputePath();

        progress = 0;
        hardness = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }

        final double distanceToTargetSquared;
        final MarkerEntity marker =
                zombie.zombieTactics$getTargetMarker();
        final LivingEntity livingTarget =
                zombie.getTarget();

        if (livingTarget != null) {
            distanceToTargetSquared =
                    zombie.distanceToSqr(livingTarget);
        } else if (marker != null) {
            distanceToTargetSquared =
                    zombie.distanceToSqr(marker);
        } else {
            target = null;
            return;
        }

        if (level.getBlockState(target).isAir()
                || distanceToTargetSquared <= Config.minDist
                || distanceToTargetSquared > Config.maxDist) {

            target = null;
            return;
        }

        if (progress >= hardness) {
            level.destroyBlock(
                    target,
                    profile.dropBlocks(),
                    zombie
            );

            zombie.level().destroyBlockProgress(
                    zombie.getId(),
                    target,
                    -1
            );

            target = null;
        } else {
            level.destroyBlockProgress(
                    zombie.getId(),
                    target,
                    (int) ((progress / hardness) * 10)
            );

            zombie.stopInPlace();

            zombie.getLookControl().setLookAt(
                    target.getX(),
                    target.getY(),
                    target.getZ()
            );

            progress += profile.increment();
            zombie.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null
                || !zombie.isAlive()
                || zombie.isNoAi()) {
            return false;
        }

        if (level.getBlockState(target).isAir()) {
            return false;
        }

        return zombie.distanceToSqr(target.getCenter())
                <= MAX_MINING_REACH_SQR;
    }

    @Override
    public boolean canUse() {
        if (!zombie.isAlive() || zombie.isNoAi()) {
            navigationBlockedSinceTick = -1;
            target = null;
            return false;
        }

        boolean navigationBlocked =
                zombie.getNavigation().isStuck()
                        || zombie.getNavigation().isDone();

        if (!navigationBlocked) {
            navigationBlockedSinceTick = -1;
            target = null;
            return false;
        }

        if (navigationBlockedSinceTick < 0) {
            navigationBlockedSinceTick = zombie.tickCount;
            return false;
        }

        int blockedTicks =
                zombie.tickCount - navigationBlockedSinceTick;

        if (blockedTicks < MINING_START_DELAY_TICKS) {
            return false;
        }

        final BlockPos destinationPos;
        final double distanceToTargetSquared;

        final MarkerEntity marker =
                zombie.zombieTactics$getTargetMarker();

        final LivingEntity livingTarget =
                zombie.getTarget();

        if (livingTarget != null) {
            destinationPos =
                    livingTarget.blockPosition();

            distanceToTargetSquared =
                    zombie.distanceToSqr(livingTarget);
        } else if (marker != null) {
            destinationPos =
                    marker.blockPosition();

            distanceToTargetSquared =
                    zombie.distanceToSqr(marker);
        } else {
            navigationBlockedSinceTick = -1;
            target = null;
            return false;
        }

        BlockPos zombiePos =
                zombie.blockPosition();

        BlockPos directStep =
                Util.off(
                        zombiePos,
                        destinationPos
                );

        target = null;

        if (distanceToTargetSquared * 1.2
                >= zombie.distanceToSqr(directStep.getCenter())
                && !scanTowardTarget(zombiePos, destinationPos)) {

            if (zombie.getNavigation().isStuck()) {
                for (byte[] offset : offsets) {
                    boolean foundBlock =
                            scanColumn(
                                    zombiePos.offset(
                                            offset[0],
                                            offset[1],
                                            offset[2]
                                    )
                            );

                    if (foundBlock) {
                        break;
                    }
                }
            }
        }

        return target != null;
    }
}