package com.heledron.spideranimation.spider.presets

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import com.heledron.spideranimation.utilities.Brightness

enum class AnimatedPalettes(val palette: List<Pair<BlockState, Brightness>>) {
    CYAN_EYES(arrayOf(
        * Array(3) { Blocks.CYAN_SHULKER_BOX },
        Blocks.CYAN_CONCRETE,
        Blocks.CYAN_CONCRETE_POWDER,

        Blocks.LIGHT_BLUE_SHULKER_BOX,
        Blocks.LIGHT_BLUE_CONCRETE,
        Blocks.LIGHT_BLUE_CONCRETE_POWDER,
    ).map { it.defaultBlockState() to Brightness(15,15) }),

    CYAN_BLINKING_LIGHTS(arrayOf(
        * Array(3) { Blocks.BLACK_SHULKER_BOX to Brightness(0,15) },
        * Array(3) { Blocks.VERDANT_FROGLIGHT to Brightness(15,15) },
        Blocks.LIGHT_BLUE_SHULKER_BOX to Brightness(15,15),
        Blocks.LIGHT_BLUE_CONCRETE to Brightness(15,15),
        Blocks.LIGHT_BLUE_CONCRETE_POWDER to Brightness(15,15),
    ).map { (block, brightness) -> block.defaultBlockState() to brightness }),


    RED_EYES(arrayOf(
        * Array(3) { Blocks.RED_SHULKER_BOX },
        Blocks.RED_CONCRETE,
        Blocks.RED_CONCRETE_POWDER,

        Blocks.FIRE_CORAL_BLOCK,
        Blocks.REDSTONE_BLOCK,
    ).map { it.defaultBlockState() to Brightness(15,15) }),

    RED_BLINKING_LIGHTS(arrayOf(
        * Array(3) { Blocks.BLACK_SHULKER_BOX to Brightness(0,15) },
        * Array(3) { Blocks.PEARLESCENT_FROGLIGHT to Brightness(15,15) },
        Blocks.RED_TERRACOTTA to Brightness(15,15),
        Blocks.REDSTONE_BLOCK to Brightness(15,15),
        Blocks.FIRE_CORAL_BLOCK to Brightness(15,15),
    ).map { (block, brightness) -> block.defaultBlockState() to brightness }),
}