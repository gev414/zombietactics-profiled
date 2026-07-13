# ZombieTactics-Profiled

ZombieTactics-Profiled is a NeoForge 1.21.1 fork of ZombieTactics that adds data-driven, entity-specific block-mining profiles.

The original global mining configuration remains available as the default. Datapacks can override those settings for individual entity types without requiring additional Java code or hardcoded mod integrations.

## Mining profiles

Mining profiles control the following block-breaking properties:

| Field                 | Description                                                                          |
| --------------------- | ------------------------------------------------------------------------------------ |
| `increment`           | Mining progress added each tick. Higher values break blocks faster.                  |
| `max_hardness`        | Maximum block hardness that the entity is allowed to target.                         |
| `hardness_multiplier` | Multiplier applied to the block-breaking duration. Higher values make mining slower. |
| `drop_blocks`         | Whether destroyed blocks should drop their normal items.                             |

The approximate number of mining ticks is:

```text
block hardness Ã— hardness_multiplier Ã· increment
```

All numeric values must be finite and non-negative.

## Fallback behavior

When an entity type has an assigned mining profile, that profile is used by its mining goal.

When no profile is assigned, the entity falls back to the original global values from the ZombieTactics common configuration:

* `increment`
* `maxHardness`
* `hardnessMult`
* `dropBlocks`

This preserves the original behavior for existing installations and entities that are not configured through a datapack.

## Example datapack

A complete example datapack is available under:

```text
examples/zombie_tactics_profile_example/```

Its structure is:

```text
zombie_tactics_profile_example/
|-- pack.mcmeta
`-- data/
    `-- zombie_tactics/
        `-- data_maps/
            `-- entity_type/
                `-- mining_profiles.json
```

The mining profile Data Map must be located at:

```text
data/zombie_tactics/data_maps/entity_type/mining_profiles.json
```

Example:

```json
{
  "values": {
    "minecraft:zombie": {
      "increment": 0.1,
      "max_hardness": 12.0,
      "hardness_multiplier": 5.0,
      "drop_blocks": true
    },
    "minecraft:zombified_piglin": {
      "increment": 0.1,
      "max_hardness": 12.0,
      "hardness_multiplier": 5.0,
      "drop_blocks": true
    }
  }
}
```

The keys inside `values` are entity type IDs. Modded entity IDs can be used in the same way:

```json
{
  "values": {
    "biohazard:brute": {
      "increment": 0.5,
      "max_hardness": 50.0,
      "hardness_multiplier": 1.0,
      "drop_blocks": false
    }
  }
}
```

Assigning a profile configures an entity that already receives the ZombieTactics mining goal. It does not automatically add block-breaking AI to unrelated entity classes.

Zombie-derived modded entities can use their own profile when they inherit the relevant ZombieTactics goal registration. Entities that replace their goal registration without calling the inherited implementation may require explicit integration.

## Installing a datapack

Copy the complete datapack folder or ZIP archive into:

```text
<world>/datapacks/
```

Then reload the world's server resources:

```text
/reload
```

After reloading, spawn a new entity of the configured type and verify its mining behavior.

Mining profiles are selected when the entity's mining goal is created. Entities that already existed before `/reload` may retain their previous profile until they are recreated.

## Building

The project requires Java 21 and includes the Gradle wrapper.

Windows:

```powershell
.\gradlew.bat clean build
```

Linux or macOS:

```bash
./gradlew clean build
```

The compiled mod JAR is generated under:

```text
build/libs/
```

## Main implementation classes

```text
n643064.zombie_tactics.profile.MiningProfile
n643064.zombie_tactics.profile.MiningProfileDataMaps
n643064.zombie_tactics.profile.MiningProfileResolver
```

`MiningProfile` defines and validates the profile data.

`MiningProfileDataMaps` registers the `zombie_tactics:mining_profiles` Data Map for entity types.

`MiningProfileResolver` returns an entity-specific datapack profile or constructs a fallback profile from the original global configuration.
