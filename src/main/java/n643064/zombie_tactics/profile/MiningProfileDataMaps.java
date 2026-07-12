package n643064.zombie_tactics.profile;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import n643064.zombie_tactics.Main;

public final class MiningProfileDataMaps
{
    public static final DataMapType<EntityType<?>, MiningProfile> MINING_PROFILES =
            DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(
                            Main.MODID,
                            "mining_profiles"
                    ),
                    Registries.ENTITY_TYPE,
                    MiningProfile.CODEC
            ).build();

    private MiningProfileDataMaps()
    {
    }

    public static void register(RegisterDataMapTypesEvent event)
    {
        event.register(MINING_PROFILES);
    }
}
