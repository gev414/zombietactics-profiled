package n643064.zombie_tactics.profile;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import n643064.zombie_tactics.Config;

public final class MiningProfileResolver
{
    private MiningProfileResolver()
    {
    }

    public static MiningProfile resolve(EntityType<?> entityType)
    {
        Holder<EntityType<?>> holder =
                BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entityType);

        MiningProfile configuredProfile =
                holder.getData(MiningProfileDataMaps.MINING_PROFILES);

        if (configuredProfile != null)
        {
            return configuredProfile;
        }

        return defaultProfile();
    }

    private static MiningProfile defaultProfile()
    {
        return new MiningProfile(
                Config.increment,
                Config.maxHardness,
                Config.hardnessMult,
                Config.dropBlocks
        );
    }
}