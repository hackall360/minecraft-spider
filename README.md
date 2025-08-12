# Spider
## Introduction
This mod was developed for a video series about procedural animations.
1. Procedural Walking Animation: https://youtu.be/Hc9x1e85L0w
2. Procedural Galloping Animation: https://youtu.be/r70xJytj0sw
3. Procedurally Animated Robots: https://youtu.be/PSnPOYeTW-0


This mod is very experimental and untested in multiplayer. Use at your own risk.

The Spider Animation mod runs entirely on the server. Clients can join with vanilla Minecraft and do not need to download anything.



## Installation
1. Download the JAR from the [releases page](https://github.com/TheCymaera/minecraft-spider/releases/).
2. Set up a [Forge](https://files.minecraftforge.net/) server for Minecraft **1.20.1** (instructions below).
3. Place the JAR in the server's `mods` folder.
4. Download the world folder from [Planet Minecraft](https://www.planetminecraft.com/project/spider-garden/).
5. Place the world folder in the server directory. Name it `world`.

## Running a Server
1. Download the Forge installer for Minecraft **1.20.1** and run it with the `--installServer` option.
2. Accept the EULA by changing `eula=false` to `eula=true` in the generated `eula.txt` file.
3. Run the server with a command similar to `java -Xmx4G -Xms4G -jar forge-1.20.1-47.3.0.jar nogui`.
4. Place the Spider Animation JAR in the `mods` folder if you have not already.
5. Join the server with `localhost` as the IP address.


## Commands
Autocomplete will show available options.

Get control items:
```
/items
```

Load preset:
```
/preset <name:string> <segment_length:double?> <segment_count:int?>
/preset hexbot 1 4
```

Load torso or leg model
```
/torso_model <name:string>
/leg_model <name:string>
```

Modify or get options
```
/options gait maxSpeed 3

/options gait maxSpeed
```

Scale the spider
```
/scale 2
```

Play splay animation (Spider must be spawned)
```
/splay
```

Change eye or blinking lights palette
```
/animated_palette eye cyan_blinking_lights
/animated_palette blinking_lights red_blinking_lights

# Custom palette (block_id, block_brightness, sky_brightness)+
/animated_palette eye custom minecraft:stone 15 15 minecraft:redstone_block 15 15
```

Fine-grained model modification
```
/modify_model <...selectors> <...operations>

# Selectors can be either a block id or a tag
# e.g. Select all cloaks in the torso
/modify_model cloak torso ...

# e.g. Select diamond blocks and netherite blocks
/modify_model minecraft:diamond_block or minecraft:netherite_block ...

# Set block
/modify_model cloak set_block minecraft:gray_concrete

# Set brightness
/modify_model cloak brightness 0 7

# Copy block from the world
/modify_model cloak copy_block ~ ~1 ~
```

Stealth Variant Example:
```
/torso_model stealth
/scale 1.3
/modify_model cloak set_block minecraft:gray_concrete
/modify_model cloak or minecraft:netherite_block or minecraft:cauldron or minecraft:anvil or minecraft:gray_shulker_box brightness 0 7

# You may need to pick a different brightness depending on your shaders
```

Copy block examples used in the video:
```
/modify_model cloak torso copy_block ~ ~1 ~
/modify_model cloak tibia copy_block ~ ~1 ~
/modify_model cloak tip copy_block ~ ~1 ~
```

## Development
1. Clone or download the repo.
2. Run `./gradlew build` to compile the mod. The resulting JAR will be in `build/libs`.
3. For convenience, set up a symlink and add the link to the server `mods` folder.
   - Windows: `mklink /D newFile.jar originalFile.jar`
   - Mac/Linux: `ln -s originalFile.jar newFile.jar`

## License
You may use the mod and source code for both commercial or non-commercial purposes.

Attribution is appreciated but not due.

Do not resell without making substantial changes.
