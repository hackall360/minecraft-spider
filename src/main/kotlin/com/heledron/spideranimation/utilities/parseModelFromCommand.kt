package com.heledron.spideranimation.utilities

import com.google.gson.Gson
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import org.joml.Matrix4f

fun parseModelFromCommand(command: String): DisplayModel {
    //summon block_display ~-0.5 ~ ~-0.5 {Passengers:[{id:"minecraft:block_display",block_state:{Name:"minecraft:smooth_quartz",Properties:{}},transformation:[0.1f,0f,0f,0.15f,0f,0.0427f,0.0288f,0.4922f,0f,-0.0022f,0.5492f,-0.8771f,0f,0f,0f,1f]}...

    val pieces = mutableListOf<BlockDisplayModelPiece>()

    var json = command.substring("summon block_display ~-0.5 ~ ~-0.5 :".length)

    // convert 1.0f -> 1.0
    json = json.replace(Regex("""(\d*\.*\d+)f"""), "$1")

    val parsed = Gson().fromJson(json, Map::class.java)

    @Suppress("UNCHECKED_CAST")
    for (passenger in parsed["Passengers"] as List<Map<*, *>>) {
        val blockDisplay = passenger["block_state"] as? Map<*, *> ?: throw IllegalArgumentException("Missing block_state")
        val blockName = blockDisplay["Name"] as? String ?: throw IllegalArgumentException("Missing block_state.Name")
        val blockProperties = blockDisplay["Properties"] as Map<*, *>
        val block = BuiltInRegistries.BLOCK.get(ResourceLocation(blockName))
            ?: throw IllegalArgumentException("Unknown block name: $blockName")
        var blockData: BlockState = block.defaultBlockState()
        for ((propName, propValue) in blockProperties) {
            val property = block.stateDefinition.getProperty(propName as String) ?: continue
            val parsed = property.getValue(propValue as String)
            if (parsed.isPresent) {
                @Suppress("UNCHECKED_CAST")
                blockData = blockData.setValue(property as Property<Comparable<Any>>, parsed.get() as Comparable<Any>)
            }
        }

        val transformation = passenger["transformation"] as? List<Float> ?: throw IllegalArgumentException("Missing transformation")
        val matrix = Matrix4f(
            transformation[0], transformation[4], transformation[8], transformation[12],
            transformation[1], transformation[5], transformation[9], transformation[13],
            transformation[2], transformation[6], transformation[10], transformation[14],
            transformation[3], transformation[7], transformation[11], transformation[15]
        )


        pieces += BlockDisplayModelPiece(
            block = blockData ?: throw IllegalArgumentException("Unknown block name: $blockName"),
            transform = matrix,
            tags = passenger["Tags"] as? List<String> ?: emptyList()
        )
    }

    return DisplayModel(pieces)
}