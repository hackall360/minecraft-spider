package com.heledron.spideranimation.utilities

import com.google.gson.Gson
import com.heledron.spideranimation.SpiderAnimationMod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.server.ServerLifecycleHooks

private fun String.parseJSONColors(): Map<Block, RGB> {
    val colorMap = mutableMapOf<Block, RGB>()

    @Suppress("UNCHECKED_CAST")
    val json = Gson().fromJson(this, Map::class.java) as Map<String, List<Double>>

    for ((key, value) in json) {
        val block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation("minecraft", key)).orElse(null) ?: continue
        colorMap[block] = RGB(value[0].toInt(), value[1].toInt(), value[2].toInt())
    }

    return colorMap
}

private val blocks: Map<Block, RGB> by lazy {
    val resourceManager = ServerLifecycleHooks.getCurrentServer().resourceManager
    val resource = resourceManager
        .getResource(ResourceLocation(SpiderAnimationMod.MOD_ID, "block_colors.json"))
        .orElseThrow { IllegalStateException("Failed to load block_colors.json") }

    resource.open().reader().use { it.readText().parseJSONColors() }
}

private val blocksWithBrightness: Map<RGB, Pair<BlockState, Int>> by lazy {
    mutableMapOf<RGB, Pair<BlockState, Int>>().apply {
        for (brightness in 15 downTo 0) {
            for ((block, color) in blocks) {
                val newColor = color.withBrightness(brightness)
                if (newColor in this) continue
                this[newColor] = block.defaultBlockState() to brightness
            }
        }
    }
}

fun getColorFromBlock(state: BlockState): RGB? {
    return blocks[state.block]
}

fun getColorFromBlock(state: BlockState, brightness: Int): RGB? {
    return getColorFromBlock(state)?.withBrightness(brightness)
}

class MatchInfo(
    val state: BlockState,
    val blockColor: RGB,
    val distance: Double,
    val brightness: Int,
)

fun getBestMatchFromColor(color: RGB, allowCustomBrightness: Boolean): MatchInfo {
    val map = if (allowCustomBrightness) blocksWithBrightness else blocksWithBrightness.filterValues { it.second == 15 }
    val bestMatch = map.minBy { it.key.distanceTo(color) }
    return MatchInfo(bestMatch.value.first, bestMatch.key, color.distanceTo(bestMatch.key), bestMatch.value.second)
}

