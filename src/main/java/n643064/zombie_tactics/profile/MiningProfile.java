package n643064.zombie_tactics.profile;

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