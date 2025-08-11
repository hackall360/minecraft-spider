package com.heledron.spideranimation.spider.rendering

import com.heledron.spideranimation.spider.Spider
import com.heledron.spideranimation.utilities.*
import com.heledron.spideranimation.utilities.Brightness
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f

fun targetRenderEntity(
    level: Level,
    position: Vec3,
) = blockRenderEntity(
    level = level,
    position = position,
    init = {
        it.blockState = Blocks.REDSTONE_BLOCK.defaultBlockState()
        it.setTeleportDuration(1)
        it.setInterpolationDuration(1)
        it.setBrightness(Brightness(15, 15))
        it.transformation = centredTransform(.25f, .25f, .25f)
    }
)

fun spiderRenderEntities(spider: Spider): RenderEntityGroup {
    val group = RenderEntityGroup()

    val transform = Matrix4f().rotate(spider.orientation)
    group.add(spider.body, modelToRenderEntity(spider, spider.position, spider.options.bodyPlan.bodyModel, transform))

    for ((legIndex, leg) in spider.body.legs.withIndex()) {
        val chain = leg.chain

        val pivot = spider.gait.legChainPivotMode.get(spider)
        for ((segmentIndex, rotation) in chain.getRotations(pivot).withIndex()) {
            val segmentPlan = spider.options.bodyPlan.legs.getOrNull(legIndex)?.segments?.getOrNull(segmentIndex) ?: continue

            val parent = chain.segments.getOrNull(segmentIndex - 1)?.position ?: chain.root

            val segmentTransform = Matrix4f().rotate(rotation)
            group.add(legIndex to segmentIndex, modelToRenderEntity(spider, parent, segmentPlan.model, segmentTransform))
        }
    }

    return group
}

private fun modelToRenderEntity(
    spider: Spider,
    position: Vec3,
    model: DisplayModel,
    transformation: Matrix4f
): RenderEntityGroup {
    val group = RenderEntityGroup()

    for ((index, piece) in model.pieces.withIndex()) {
        group.add(index, modelPieceToRenderEntity(spider, position, piece, transformation))
    }

    return group
}

private fun modelPieceToRenderEntity(
    spider: Spider,
    position: Vec3,
    piece: BlockDisplayModelPiece,
    transformation: Matrix4f,
) = blockRenderEntity(
    level = spider.world,
    position = position,
    init = {
        it.setTeleportDuration(1)
        it.setInterpolationDuration(1)
    },
    update = {
        val transform = Matrix4f(transformation).mul(piece.transform)
        it.applyTransformationWithInterpolation(transform)

        val cloak = if (piece.tags.contains("cloak")) {
            val relative = transform.transform(Vector4f(.5f, .5f, .5f, 1f))
            val piecePosition = Vec3(
                position.x + relative.x.toDouble(),
                position.y + relative.y.toDouble(),
                position.z + relative.z.toDouble(),
            )

            spider.cloak.getPiece(piece, piecePosition, piece.block, piece.brightness)
        } else null

        if (cloak != null) {
            it.blockState = cloak.first
            it.setBrightness(cloak.second)
        } else {
            it.blockState = piece.block
            it.setBrightness(piece.brightness)
        }
    },
)
