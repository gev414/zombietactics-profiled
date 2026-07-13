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
        return target != null
                && zombie.distanceToSqr(target.getCenter()) <= 9;
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

        final BlockPos blockPos;
        final double distanceToTargetSquared;
        final MarkerEntity marker =
                zombie.zombieTactics$getTargetMarker();
        final LivingEntity livingTarget =
                zombie.getTarget();

        if (livingTarget != null) {
            blockPos = Util.off(
                    zombie.blockPosition(),
                    livingTarget.blockPosition()
            );

            distanceToTargetSquared =
                    zombie.distanceToSqr(livingTarget);
        } else if (marker != null) {
            blockPos = Util.off(
                    zombie.blockPosition(),
                    marker.blockPosition()
            );

            distanceToTargetSquared =
                    zombie.distanceToSqr(marker);
        } else {
            navigationBlockedSinceTick = -1;
            target = null;
            return false;
        }

        target = null;

        if (distanceToTargetSquared * 1.2
                >= zombie.distanceToSqr(blockPos.getCenter())
                && !scanColumn(blockPos.above())) {

            if (zombie.getNavigation().isStuck()
                    && !scanColumn(blockPos)) {

                for (byte[] offset : offsets) {
                    scanColumn(
                            zombie.blockPosition().offset(
                                    offset[0],
                                    offset[1],
                                    offset[2]
                            )
                    );
                }
            }
        }

        return target != null;
    }
}