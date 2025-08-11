package com.heledron.spideranimation.utilities

import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4f

data class Brightness(val block: Int, val sky: Int)

class BlockDisplayModelPiece (
    var block: BlockState,
    var transform: Matrix4f,
    var brightness: Brightness? = null,
    var tags: List<String> = emptyList(),
) {
    fun scale(scale: Float) {
        transform.set(Matrix4f().scale(scale).mul(transform))
    }

    fun scale(x: Float, y: Float, z: Float) {
        transform.set(Matrix4f().scale(x, y, z).mul(transform))
    }

    fun clone() = BlockDisplayModelPiece(
        block = block,
        transform = Matrix4f(transform),
        brightness = brightness?.let { Brightness(it.block, it.sky) },
        tags = tags,
    )
}

class DisplayModel(var pieces: List<BlockDisplayModelPiece>) {
    fun scale(scale: Float) = apply {
        pieces.forEach { it.scale(scale, scale, scale) }
    }

    fun scale(x: Float, y: Float, z: Float) = apply {
        pieces.forEach { it.scale(x, y, z) }
    }

    fun clone() = DisplayModel(pieces.map { it.clone() })

    companion object {
        fun empty() = DisplayModel(emptyList())
    }
}