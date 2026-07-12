package n643064.zombie_tactics.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Defines block-mining characteristics for a specific entity type.
 *
 * @param increment          mining progress added each tick
 * @param maxHardness        maximum block hardness the entity may target
 * @param hardnessMultiplier multiplier applied when calculating break duration
 * @param dropBlocks         whether destroyed blocks should drop their items
 */
public record MiningProfile(
        double increment,
        double maxHardness,
        double hardnessMultiplier,
        boolean dropBlocks
) {

    public static final Codec<MiningProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.DOUBLE
                            .fieldOf("increment")
                            .forGetter(MiningProfile::increment),
                    Codec.DOUBLE
                            .fieldOf("max_hardness")
                            .forGetter(MiningProfile::maxHardness),
                    Codec.DOUBLE
                            .fieldOf("hardness_multiplier")
                            .forGetter(MiningProfile::hardnessMultiplier),
                    Codec.BOOL
                            .fieldOf("drop_blocks")
                            .forGetter(MiningProfile::dropBlocks)
            ).apply(instance, MiningProfile::new));

    public MiningProfile {
        validateNonNegativeFinite("increment", increment);
        validateNonNegativeFinite("maxHardness", maxHardness);
        validateNonNegativeFinite(
                "hardnessMultiplier",
                hardnessMultiplier
        );
    }

    private static void validateNonNegativeFinite(
            String fieldName,
            double value
    ) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(
                    fieldName + " must be a finite, non-negative number"
            );
        }
    }
}